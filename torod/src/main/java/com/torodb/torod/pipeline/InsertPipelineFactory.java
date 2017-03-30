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

package com.torodb.torod.pipeline;

import com.google.common.util.concurrent.Service;
import com.torodb.core.backend.WriteBackendTransaction;
import com.torodb.core.d2r.D2RTranslatorFactory;
import com.torodb.core.transaction.metainf.MetaDatabase;
import com.torodb.core.transaction.metainf.MutableMetaCollection;

/**
 *
 */
public interface InsertPipelineFactory extends Service {

  /**
   *
   * @param translatorFactory
   * @param metaDb
   * @param mutableMetaCollection
   * @param backendConnection
   * @return
   */
  public InsertPipeline createInsertPipeline(
      D2RTranslatorFactory translatorFactory,
      MetaDatabase metaDb,
      MutableMetaCollection mutableMetaCollection,
      WriteBackendTransaction backendConnection,
      boolean concurrent);

}