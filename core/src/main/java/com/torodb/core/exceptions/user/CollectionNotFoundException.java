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

package com.torodb.core.exceptions.user;

public class CollectionNotFoundException extends UserException {

  private static final long serialVersionUID = 1L;

  private final String database;
  private final String collection;

  public CollectionNotFoundException(String database, String collection) {
    super();
    this.database = database;
    this.collection = collection;
  }

  public String getDatabase() {
    return database;
  }

  public String getCollection() {
    return collection;
  }

  @Override
  public <R, A> R accept(UserExceptionVisitor<R, A> visitor, A arg) {
    return visitor.visit(this, arg);
  }

}
