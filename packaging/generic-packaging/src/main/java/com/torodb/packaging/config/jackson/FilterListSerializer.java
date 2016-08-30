/*
 *     This file is part of ToroDB.
 *
 *     ToroDB is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ToroDB is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with ToroDB. If not, see <http://www.gnu.org/licenses/>.
 *
 *     Copyright (c) 2014, 8Kdata Technology
 *     
 */

package com.torodb.packaging.config.jackson;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.torodb.packaging.config.model.protocol.mongo.FilterList;
import com.torodb.packaging.config.util.DescriptionFactoryWrapper;

public class FilterListSerializer extends JsonSerializer<FilterList> {
	@Override
	public void serialize(FilterList value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {
		jgen.writeStartObject();

		serializeFields(value, jgen);

		jgen.writeEndObject();
	}

	private void serializeFields(FilterList value, JsonGenerator jgen) throws IOException {
		for (Map.Entry<String, List<String>> databaseEntry : value.entrySet()) {
            jgen.writeArrayFieldStart(databaseEntry.getKey());
            for (String collection : databaseEntry.getValue()) {
                jgen.writeString(collection);
            }
            jgen.writeEndArray();
		}
	}

	public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType type) throws JsonMappingException {
		if (!(visitor instanceof DescriptionFactoryWrapper)) {
			super.acceptJsonFormatVisitor(visitor, type);
		}
	}
}