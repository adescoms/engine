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

package com.torodb.mongodb.commands.signatures.authentication;

import com.torodb.mongodb.commands.tools.EmptyCommandArgumentMarshaller;
import com.torodb.mongowp.bson.BsonDocument;
import com.torodb.mongowp.commands.MarshalException;
import com.torodb.mongowp.commands.impl.AbstractNotAliasableCommand;
import com.torodb.mongowp.commands.tools.Empty;
import com.torodb.mongowp.exceptions.BadValueException;
import com.torodb.mongowp.exceptions.FailedToParseException;
import com.torodb.mongowp.exceptions.MongoException;
import com.torodb.mongowp.exceptions.NoSuchKeyException;
import com.torodb.mongowp.exceptions.TypesMismatchException;
import com.torodb.mongowp.fields.StringField;
import com.torodb.mongowp.utils.BsonDocumentBuilder;
import com.torodb.mongowp.utils.BsonReaderTool;

/**
 *
 */
public class GetNonceCommand extends AbstractNotAliasableCommand<Empty, String> {

  public static final GetNonceCommand INSTANCE = new GetNonceCommand();
  private static final String COMMAND_NAME = "getnonce";
  private static final StringField NONCE_FIELD = new StringField("nonce");

  private GetNonceCommand() {
    super(COMMAND_NAME);
  }

  @Override
  public boolean isAdminOnly() {
    return true;
  }

  @Override
  public Class<? extends Empty> getArgClass() {
    return Empty.class;
  }

  @Override
  public Empty unmarshallArg(BsonDocument requestDoc) throws BadValueException,
      TypesMismatchException, NoSuchKeyException, FailedToParseException {
    return Empty.getInstance();
  }

  @Override
  public BsonDocument marshallArg(Empty request) throws MarshalException {
    return EmptyCommandArgumentMarshaller.marshallEmptyArgument(this);
  }

  @Override
  public Class<? extends String> getResultClass() {
    return String.class;
  }

  @Override
  public String unmarshallResult(BsonDocument resultDoc) throws BadValueException,
      TypesMismatchException, NoSuchKeyException, FailedToParseException, MongoException {
    return BsonReaderTool.getString(resultDoc, NONCE_FIELD);
  }

  @Override
  public BsonDocument marshallResult(String result) throws MarshalException {
    return new BsonDocumentBuilder()
        .append(NONCE_FIELD, result)
        .build();
  }

}
