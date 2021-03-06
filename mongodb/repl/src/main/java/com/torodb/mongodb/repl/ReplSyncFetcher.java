/*
 * ToroDB
 * Copyright © 2014 8Kdata Technology (www.8kdata.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.torodb.mongodb.repl;

import com.google.common.net.HostAndPort;
import com.torodb.core.annotations.TorodbRunnableService;
import com.torodb.core.logging.LoggerFactory;
import com.torodb.core.services.RunnableTorodbService;
import com.torodb.core.supervision.Supervisor;
import com.torodb.mongodb.repl.exceptions.NoSyncSourceFoundException;
import com.torodb.mongowp.OpTime;
import com.torodb.mongowp.client.core.UnreachableMongoServerException;
import com.torodb.mongowp.commands.oplog.OplogOperation;
import com.torodb.mongowp.commands.pojos.MongoCursor;
import com.torodb.mongowp.commands.pojos.MongoCursor.Batch;
import com.torodb.mongowp.exceptions.MongoException;
import com.torodb.mongowp.exceptions.OplogOperationUnsupported;
import com.torodb.mongowp.exceptions.OplogStartMissingException;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ThreadFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
class ReplSyncFetcher extends RunnableTorodbService {

  private static final int MIN_BATCH_SIZE = 5;
  private static final long SLEEP_TO_BATCH_MILLIS = 2;

  private final Logger logger;
  private final SyncServiceView callback;
  private final OplogReaderProvider readerProvider;
  private final SyncSourceProvider syncSourceProvider;
  private final ReplMetrics metrics;
  private long opsReadCounter = 0;

  private long lastFetchedHash;
  private OpTime lastFetchedOpTime;
  private volatile Thread runThread;

  ReplSyncFetcher(
      @TorodbRunnableService ThreadFactory threadFactory,
      Supervisor supervisor,
      @Nonnull SyncServiceView callback,
      @Nonnull SyncSourceProvider syncSourceProvider,
      @Nonnull OplogReaderProvider readerProvider,
      long lastAppliedHash,
      OpTime lastAppliedOpTime,
      ReplMetrics metrics,
      LoggerFactory loggerFactory) {
    super(supervisor, threadFactory);
    this.logger = loggerFactory.apply(this.getClass());
    this.callback = callback;
    this.readerProvider = readerProvider;
    this.lastFetchedHash = 0;
    this.lastFetchedOpTime = null;
    this.syncSourceProvider = syncSourceProvider;

    this.lastFetchedHash = lastAppliedHash;
    this.lastFetchedOpTime = lastAppliedOpTime;

    this.metrics = metrics;
  }

  @Override
  protected String serviceName() {
    return "ToroDB Sync Fetcher";
  }

  /**
   *
   * @return an approximation to the number of operations that has been fetched
   */
  public long getOpsReadCounter() {
    return opsReadCounter;
  }

  @Override
  protected void triggerShutdown() {
    if (runThread != null) {
      runThread.interrupt();
    }
  }

  @Override
  public void runProtected() {
    runThread = Thread.currentThread();
    boolean rollbackNeeded = false;
    try {
      OplogReader oplogReader = null;
      while (!rollbackNeeded && isRunning()) {
        try {
          if (callback.shouldPause()) {
            callback.awaitUntilUnpaused();
          }

          callback.awaitUntilAllFetchedAreApplied();

          HostAndPort syncSource = null;
          try {
            syncSource = syncSourceProvider.newSyncSource(lastFetchedOpTime);
            oplogReader = readerProvider.newReader(syncSource);
          } catch (NoSyncSourceFoundException ex) {
            logger.warn("There is no source to sync from");
            Thread.sleep(1000);
            continue;
          } catch (UnreachableMongoServerException ex) {
            assert syncSource != null;
            logger.warn("It was impossible to reach the sync source " + syncSource);
            Thread.sleep(1000);
            continue;
          }

          rollbackNeeded = fetch(oplogReader);
        } catch (InterruptedException ex) {
          logger.info("Interrupted fetch process", ex);
        } catch (RestartFetchException ex) {
          logger.info("Restarting fetch process", ex);
        } catch (Throwable ex) {
          throw new StopFetchException(ex);
        } finally {
          if (oplogReader != null) {
            oplogReader.close();
          }
        }
      }
      if (rollbackNeeded) {
        logger.info("Requesting rollback");
        callback.rollback(oplogReader);
      } else {
        logger.info(serviceName() + " ending by external request");
        callback.fetchFinished();
      }
    } catch (StopFetchException ex) {
      logger.info(serviceName() + " stopped by self request");
      callback.fetchAborted(ex);
    }
    logger.info(serviceName() + " stopped");
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  public boolean fetchIterationCanContinue() {
    return isRunning() && !callback.shouldPause();
  }

  /**
   *
   * @param reader
   * @return true iff rollback is needed
   * @throws com.torodb.torod.mongodb.repl.ReplSyncFetcher.StopFetchException
   * @throws com.torodb.torod.mongodb.repl.ReplSyncFetcher.RestartFetchException
   */
  private boolean fetch(OplogReader reader) throws StopFetchException, RestartFetchException {

    try {

      MongoCursor<OplogOperation> cursor = reader.queryGte(lastFetchedOpTime);
      Batch<OplogOperation> batch = cursor.fetchBatch();
      postBatchChecks(reader, cursor, batch);

      try {
        if (isRollbackNeeded(reader, batch, lastFetchedOpTime, lastFetchedHash)) {
          return true;
        }

        while (fetchIterationCanContinue()) {
          if (!batch.hasNext()) {
            preBatchChecks(batch);
            batch = cursor.fetchBatch();
            postBatchChecks(reader, cursor, batch);
            continue;
          }

          if (batch.hasNext()) {
            OplogOperation nextOp = batch.next();
            assert nextOp != null;
            boolean delivered = false;
            while (!delivered) {
              try {
                logger.debug("Delivered op: {}", nextOp);
                callback.deliver(nextOp);
                delivered = true;
                opsReadCounter++;
              } catch (InterruptedException ex) {
                logger.warn(serviceName() + " interrupted while a "
                    + "message was being to deliver. Retrying", ex);
              }
            }

            lastFetchedHash = nextOp.getHash();
            lastFetchedOpTime = nextOp.getOpTime();
            metrics.getLastOpTimeFetched().setValue(lastFetchedOpTime.toString());
          }
        }
      } finally {
        cursor.close();
      }

    } catch (MongoException ex) {
      throw new RestartFetchException();
    }
    return false;
  }

  /**
   * @param oldBatch
   */
  private void preBatchChecks(Batch<OplogOperation> oldBatch) {
    int batchSize = oldBatch.getBatchSize();
    if (batchSize > 0 && batchSize < MIN_BATCH_SIZE) {
      long currentTime = System.currentTimeMillis();
      long elapsedTime = currentTime - oldBatch.getFetchTime();
      if (elapsedTime < SLEEP_TO_BATCH_MILLIS) {
        try {
          logger.debug("Batch size is very small. Waiting {} millis for more...",
              SLEEP_TO_BATCH_MILLIS);
          Thread.sleep(SLEEP_TO_BATCH_MILLIS);
        } catch (InterruptedException ex) {
          Thread.interrupted();
        }
      }
    }
  }

  /**
   *
   * @param cursor
   * @throws InterruptedException
   */
  private void postBatchChecks(OplogReader reader, MongoCursor<OplogOperation> cursor,
      Batch<OplogOperation> newBatch)
      throws RestartFetchException {
    if (newBatch == null) {
      throw new RestartFetchException();
    }
    infrequentChecks(reader);

    if (!newBatch.hasNext()) {
      if (cursor.hasNext()) {
        throw new RestartFetchException();
      }
    }
    //TODO: log stats
  }

  private void infrequentChecks(OplogReader reader) throws RestartFetchException {
    if (syncSourceProvider.shouldChangeSyncSource()) {
      logger.info("A better sync source has been detected");
      throw new RestartFetchException();
    }
  }

  private boolean isRollbackNeeded(
      OplogReader reader,
      Batch<OplogOperation> batch,
      OpTime lastFetchedOpTime,
      long lastFetchedHash) throws StopFetchException {
    if (!batch.hasNext()) {
      try {
        /*
         * our last query return an empty set. But we can still detect a rollback if the last
         * operation stored on the sync source is before our last optime fetched
         */
        OplogOperation lastOp = reader.getLastOp();

        if (lastOp.getOpTime().compareTo(lastFetchedOpTime) < 0) {
          logger.info("We are ahead of the sync source. Rolling back");
          return true;
        }
      } catch (OplogStartMissingException ex) {
        logger.error("Sync source contais no operation on his oplog!");
        throw new StopFetchException();
      } catch (OplogOperationUnsupported ex) {
        logger.error("Sync source contais an invalid operation!");
        throw new StopFetchException(ex);
      } catch (MongoException ex) {
        logger.error("Unknown error while trying to fetch last remote operation", ex);
        throw new StopFetchException(ex);
      }
    } else {
      OplogOperation firstOp = batch.next();
      assert firstOp != null;
      if (firstOp.getHash() != lastFetchedHash
          || !firstOp.getOpTime().equals(lastFetchedOpTime)) {

        logger.info(
            "Rolling back: Our last fetched = [{}, {}]. Source = [{}, {}]",
            lastFetchedOpTime,
            lastFetchedHash,
            firstOp.getOpTime(),
            firstOp.getHash()
        );

        return true;
      }
    }
    return false;
  }

  private static class RestartFetchException extends Exception {

    private static final long serialVersionUID = 1L;
  }

  private static class StopFetchException extends Exception {

    private static final long serialVersionUID = 1L;

    public StopFetchException() {
    }

    public StopFetchException(Throwable cause) {
      super(cause);
    }
  }

  public static interface SyncServiceView {

    void deliver(@Nonnull OplogOperation oplogOp) throws InterruptedException;

    void rollback(OplogReader reader);

    void awaitUntilUnpaused() throws InterruptedException;

    boolean shouldPause();

    public void awaitUntilAllFetchedAreApplied();

    public void fetchFinished();

    public void fetchAborted(Throwable ex);
  }
}
