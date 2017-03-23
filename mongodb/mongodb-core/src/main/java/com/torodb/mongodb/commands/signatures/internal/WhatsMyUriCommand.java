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

package com.torodb.mongodb.commands.signatures.internal;

import com.torodb.mongodb.commands.signatures.internal.WhatsMyUriCommand.WhatsMyUriReply;
import com.torodb.mongowp.bson.BsonDocument;
import com.torodb.mongowp.commands.impl.AbstractNotAliasableCommand;
import com.torodb.mongowp.commands.tools.Empty;
import com.torodb.mongowp.exceptions.BadValueException;
import com.torodb.mongowp.exceptions.NoSuchKeyException;
import com.torodb.mongowp.exceptions.TypesMismatchException;
import com.torodb.mongowp.fields.StringField;
import com.torodb.mongowp.utils.BsonDocumentBuilder;

public class WhatsMyUriCommand extends AbstractNotAliasableCommand<Empty, WhatsMyUriReply> {

  public static final WhatsMyUriCommand INSTANCE = new WhatsMyUriCommand();

  private WhatsMyUriCommand() {
    super("whatsmyuri");
  }

  @Override
  public Class<? extends Empty> getArgClass() {
    return Empty.class;
  }

  @Override
  public Empty unmarshallArg(BsonDocument requestDoc)
      throws TypesMismatchException, NoSuchKeyException, BadValueException {
    return Empty.getInstance();
  }

  @Override
  public BsonDocument marshallArg(Empty request) {
    throw new UnsupportedOperationException("Not supported yet."); //TODO
  }

  @Override
  public Class<? extends WhatsMyUriReply> getResultClass() {
    return WhatsMyUriReply.class;
  }

  @Override
  public BsonDocument marshallResult(WhatsMyUriReply reply) {

    return new BsonDocumentBuilder(2)
        .append(WhatsMyUriReply.YOU_FIELD, reply.getHost() + ":" + reply.getPort())
        .build();
  }

  @Override
  public WhatsMyUriReply unmarshallResult(BsonDocument resultDoc) {
    throw new UnsupportedOperationException("Not supported yet."); //TODO
  }

  public static class WhatsMyUriReply {

    private static final StringField YOU_FIELD = new StringField("you");

    private final String host;
    private final int port;

    public WhatsMyUriReply(String host, int port) {
      this.host = host;
      this.port = port;
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }
  }

}
