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

package com.torodb.kvdocument.conversion.mongowp;

import com.torodb.kvdocument.conversion.mongowp.values.BsonKvString;
import com.torodb.kvdocument.values.KvBinary.KvBinarySubtype;
import com.torodb.kvdocument.values.KvBoolean;
import com.torodb.kvdocument.values.KvDecimal128;
import com.torodb.kvdocument.values.KvDeprecated;
import com.torodb.kvdocument.values.KvDocument;
import com.torodb.kvdocument.values.KvDouble;
import com.torodb.kvdocument.values.KvInteger;
import com.torodb.kvdocument.values.KvLong;
import com.torodb.kvdocument.values.KvMaxKey;
import com.torodb.kvdocument.values.KvMinKey;
import com.torodb.kvdocument.values.KvMongoDbPointer;
import com.torodb.kvdocument.values.KvMongoJavascript;
import com.torodb.kvdocument.values.KvMongoJavascriptWithScope;
import com.torodb.kvdocument.values.KvMongoObjectId;
import com.torodb.kvdocument.values.KvMongoRegex;
import com.torodb.kvdocument.values.KvNull;
import com.torodb.kvdocument.values.KvUndefined;
import com.torodb.kvdocument.values.KvValue;
import com.torodb.kvdocument.values.heap.ByteArrayKvMongoObjectId;
import com.torodb.kvdocument.values.heap.ByteSourceKvBinary;
import com.torodb.kvdocument.values.heap.DefaultKvMongoTimestamp;
import com.torodb.kvdocument.values.heap.InstantKvInstant;
import com.torodb.kvdocument.values.heap.LongKvInstant;
import com.torodb.mongowp.bson.BsonArray;
import com.torodb.mongowp.bson.BsonBinary;
import com.torodb.mongowp.bson.BsonBoolean;
import com.torodb.mongowp.bson.BsonDateTime;
import com.torodb.mongowp.bson.BsonDbPointer;
import com.torodb.mongowp.bson.BsonDecimal128;
import com.torodb.mongowp.bson.BsonDeprecated;
import com.torodb.mongowp.bson.BsonDocument;
import com.torodb.mongowp.bson.BsonDouble;
import com.torodb.mongowp.bson.BsonInt32;
import com.torodb.mongowp.bson.BsonInt64;
import com.torodb.mongowp.bson.BsonJavaScript;
import com.torodb.mongowp.bson.BsonJavaScriptWithScope;
import com.torodb.mongowp.bson.BsonMax;
import com.torodb.mongowp.bson.BsonMin;
import com.torodb.mongowp.bson.BsonNull;
import com.torodb.mongowp.bson.BsonObjectId;
import com.torodb.mongowp.bson.BsonRegex;
import com.torodb.mongowp.bson.BsonString;
import com.torodb.mongowp.bson.BsonTimestamp;
import com.torodb.mongowp.bson.BsonUndefined;
import com.torodb.mongowp.bson.BsonValue;
import com.torodb.mongowp.bson.BsonValueVisitor;
import com.torodb.mongowp.bson.impl.InstantBsonDateTime;

import java.util.function.Function;

public class FromBsonValueTranslator
    implements BsonValueVisitor<KvValue<?>, Void>, Function<BsonValue<?>, KvValue<?>> {

  private FromBsonValueTranslator() {}

  public static FromBsonValueTranslator getInstance() {
    return FromBsonValueTranslatorHolder.INSTANCE;
  }

  @Override
  public KvValue<?> apply(BsonValue<?> bsonValue) {
    return bsonValue.accept(this, null);
  }

  @Override
  public KvValue<?> visit(BsonArray value, Void arg) {
    return MongoWpConverter.toEagerArray(value);
  }

  @Override
  public KvValue<?> visit(BsonBinary value, Void arg) {
    KvBinarySubtype subtype;
    switch (value.getSubtype()) {
      case FUNCTION:
        subtype = KvBinarySubtype.MONGO_FUNCTION;
        break;
      case GENERIC:
        subtype = KvBinarySubtype.MONGO_GENERIC;
        break;
      case MD5:
        subtype = KvBinarySubtype.MONGO_MD5;
        break;
      case OLD_BINARY:
        subtype = KvBinarySubtype.MONGO_OLD_BINARY;
        break;
      case OLD_UUID:
        subtype = KvBinarySubtype.MONGO_OLD_UUID;
        break;
      case USER_DEFINED:
        subtype = KvBinarySubtype.MONGO_USER_DEFINED;
        break;
      case UUID:
        subtype = KvBinarySubtype.MONGO_UUID;
        break;
      default:
        subtype = KvBinarySubtype.UNDEFINED;
        break;
    }
    return new ByteSourceKvBinary(
        subtype, value.getNumericSubType(), value.getByteSource().getDelegate());
  }

  @Override
  public KvValue<?> visit(BsonDbPointer value, Void arg) {
    return KvMongoDbPointer.of(value.getNamespace(), (KvMongoObjectId) visit(value.getId(), arg));
  }

  @Override
  public KvValue<?> visit(BsonDateTime value, Void arg) {
    if (value instanceof InstantBsonDateTime) {
      return new InstantKvInstant(value.getValue());
    }
    return new LongKvInstant(value.getMillisFromUnix());
  }

  @Override
  public KvValue<?> visit(BsonDocument value, Void arg) {
    return MongoWpConverter.toEagerDocument(value);
  }

  @Override
  public KvValue<?> visit(BsonDouble value, Void arg) {
    return KvDouble.of(value.doubleValue());
  }

  @Override
  public KvValue<?> visit(BsonInt32 value, Void arg) {
    return KvInteger.of(value.intValue());
  }

  @Override
  public KvValue<?> visit(BsonInt64 value, Void arg) {
    return KvLong.of(value.longValue());
  }

  @Override
  public KvValue<?> visit(BsonBoolean value, Void arg) {
    if (value.getPrimitiveValue()) {
      return KvBoolean.TRUE;
    }
    return KvBoolean.FALSE;
  }

  @Override
  public KvValue<?> visit(BsonJavaScript value, Void arg) {
    return KvMongoJavascript.of(value.getValue());
  }

  @Override
  public KvValue<?> visit(BsonJavaScriptWithScope value, Void arg) {
    return KvMongoJavascriptWithScope.of(
        value.getJavaScript(), (KvDocument) visit(value.getScope(), arg));
  }

  @Override
  public KvValue<?> visit(BsonMax value, Void arg) {
    return KvMaxKey.getInstance();
  }

  @Override
  public KvValue<?> visit(BsonMin value, Void arg) {
    return KvMinKey.getInstance();
  }

  @Override
  public KvValue<?> visit(BsonNull value, Void arg) {
    return KvNull.getInstance();
  }

  @Override
  public KvValue<?> visit(BsonObjectId value, Void arg) {
    return new ByteArrayKvMongoObjectId(value.toByteArray());
  }

  @Override
  public KvValue<?> visit(BsonRegex value, Void arg) {

    return KvMongoRegex.of(value.getPattern(), value.getOptionsAsText());
  }

  @Override
  public KvValue<?> visit(BsonString value, Void arg) {
    return new BsonKvString(value);
  }

  @Override
  public KvValue<?> visit(BsonUndefined value, Void arg) {
    return KvUndefined.getInstance();
  }

  @Override
  public KvValue<?> visit(BsonTimestamp value, Void arg) {
    return new DefaultKvMongoTimestamp(value.getSecondsSinceEpoch(), value.getOrdinal());
  }

  @Override
  public KvValue<?> visit(BsonDeprecated value, Void arg) {
    return KvDeprecated.of(value.getValue());
  }

  @Override
  public KvValue<?> visit(BsonDecimal128 value, Void arg) {
    return KvDecimal128.of(value.getHigh(), value.getLow());
  }

  private static class FromBsonValueTranslatorHolder {
    private static final FromBsonValueTranslator INSTANCE = new FromBsonValueTranslator();
  }

  //@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD")
  private Object readResolve() {
    return FromBsonValueTranslator.getInstance();
  }
}
