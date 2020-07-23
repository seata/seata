/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.datasource.undo.parser;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import java.sql.Timestamp;

public class JacksonConfigurerAdapter implements SerializerConfigurerAdapter {

    @Override
    public String name() {
        return JacksonUndoLogParser.NAME;
    }

    public void config(SimpleModule module, ObjectMapper mapper) {
        JsonSerializer timestampSerializer = new JacksonUndoLogParser.TimestampSerializer();
        JsonDeserializer timestampDeserializer = new JacksonUndoLogParser.TimestampDeserializer();
        JsonSerializer blobSerializer = new JacksonUndoLogParser.BlobSerializer();
        JsonDeserializer blobDeserializer = new JacksonUndoLogParser.BlobDeserializer();
        JsonSerializer clobSerializer = new JacksonUndoLogParser.ClobSerializer();
        JsonDeserializer clobDeserializer = new JacksonUndoLogParser.ClobDeserializer();

        module.addSerializer(Timestamp.class, timestampSerializer);
        module.addDeserializer(Timestamp.class, timestampDeserializer);
        module.addSerializer(SerialBlob.class, blobSerializer);
        module.addDeserializer(SerialBlob.class, blobDeserializer);
        module.addSerializer(SerialClob.class, clobSerializer);
        module.addDeserializer(SerialClob.class, clobDeserializer);

        mapper.registerModule(module);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        mapper.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER);
    }
}
