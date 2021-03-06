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

package com.torodb.backend.mysql.tables.records;

import com.torodb.backend.converters.TableRefConverter;
import com.torodb.backend.mysql.tables.MySqlMetaDocPartIndexColumnTable;
import com.torodb.backend.tables.records.MetaDocPartIndexColumnRecord;
import com.torodb.core.TableRef;
import com.torodb.core.TableRefFactory;
import com.torodb.core.transaction.metainf.FieldIndexOrdering;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonReader;

public class MySqlMetaDocPartIndexColumnRecord extends MetaDocPartIndexColumnRecord<String> {

  private static final long serialVersionUID = 5447263400567262902L;

  /**
   * Create a detached MetaFieldRecord
   */
  public MySqlMetaDocPartIndexColumnRecord() {
    super(MySqlMetaDocPartIndexColumnTable.DOC_PART_INDEX_COLUMN);
  }

  /**
   * Create a detached, initialised MetaFieldRecord
   */
  public MySqlMetaDocPartIndexColumnRecord(String database, String indexIdentifier,
      Integer position, String collection, String tableRef, String identifier,
      FieldIndexOrdering fieldIndexOrdering) {
    super(MySqlMetaDocPartIndexColumnTable.DOC_PART_INDEX_COLUMN);

    values(database, indexIdentifier, position, collection, tableRef, identifier,
        fieldIndexOrdering);
  }

  @Override
  public MetaDocPartIndexColumnRecord<String> values(String database, String indexIdentifier,
      Integer position, String collection, String tableRef, String indentifier,
      FieldIndexOrdering fieldIndexOrdering) {
    setDatabase(database);
    setIndexIdentifier(indexIdentifier);
    setPosition(position);
    setCollection(collection);
    setTableRef(tableRef);
    setIdentifier(indentifier);
    setOrdering(fieldIndexOrdering);
    return this;
  }

  @Override
  protected String toTableRefType(TableRef tableRef) {
    return TableRefConverter.toJsonArray(tableRef).toString();
  }

  @Override
  public TableRef getTableRefValue(TableRefFactory tableRefFactory) {
    final JsonReader reader = Json.createReader(new StringReader(getTableRef()));
    return TableRefConverter.fromJsonArray(tableRefFactory, reader.readArray());
  }
}
