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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ser.std.ArraySerializerBase;
import io.seata.common.Constants;
import io.seata.common.executor.Initialize;
import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.rm.datasource.undo.UndoLogParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialException;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;

/**
 * The type Json based undo log parser.
 *
 * @author jsbxyyx
 */
@LoadLevel(name = JacksonUndoLogParser.NAME)
public class JacksonUndoLogParser implements UndoLogParser, Initialize {

    public static final String NAME = "jackson";

    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonUndoLogParser.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private final SimpleModule module = new SimpleModule();

    @Override
    public void init() {
        JacksonConfigurerAdapter adapter = (JacksonConfigurerAdapter) CustomSerializerConfigurerAdapter.getConfig(NAME);
        if (null == adapter) {
            adapter = new JacksonConfigurerAdapter();
        }
        adapter.config(module, mapper);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] getDefaultContent() {
        return "{}".getBytes(Constants.DEFAULT_CHARSET);
    }

    @Override
    public byte[] encode(BranchUndoLog branchUndoLog) {
        try {
            return mapper.writeValueAsBytes(branchUndoLog);
        } catch (JsonProcessingException e) {
            LOGGER.error("json encode exception, {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public BranchUndoLog decode(byte[] bytes) {
        try {
            BranchUndoLog branchUndoLog;
            if (Arrays.equals(bytes, getDefaultContent())) {
                branchUndoLog = new BranchUndoLog();
            } else {
                branchUndoLog = mapper.readValue(bytes, BranchUndoLog.class);
            }
            return branchUndoLog;
        } catch (IOException e) {
            LOGGER.error("json decode exception, {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * if necessary
     * extend {@link ArraySerializerBase}
     */
    public static class TimestampSerializer extends JsonSerializer<Timestamp> {

        @Override
        public void serializeWithType(Timestamp timestamp, JsonGenerator gen, SerializerProvider serializers,
                                      TypeSerializer typeSerializer) throws IOException {
            WritableTypeId typeId = typeSerializer.writeTypePrefix(gen,
                    typeSerializer.typeId(timestamp, JsonToken.START_ARRAY));
            serialize(timestamp, gen, serializers);
            gen.writeTypeSuffix(typeId);
        }

        @Override
        public void serialize(Timestamp timestamp, JsonGenerator gen, SerializerProvider serializers) {
            try {
                gen.writeNumber(timestamp.getTime());
                gen.writeNumber(timestamp.getNanos());
            } catch (IOException e) {
                LOGGER.error("serialize java.sql.Timestamp error : {}", e.getMessage(), e);
            }

        }
    }

    /**
     * if necessary
     * extend {@link JsonNodeDeserializer}
     */
    public static class TimestampDeserializer extends JsonDeserializer<Timestamp> {

        @Override
        public Timestamp deserialize(JsonParser p, DeserializationContext ctxt) {
            if (p.isExpectedStartArrayToken()) {
                ArrayNode arrayNode;
                try {
                    arrayNode = p.getCodec().readTree(p);
                    Timestamp timestamp = new Timestamp(arrayNode.get(0).asLong());
                    timestamp.setNanos(arrayNode.get(1).asInt());
                    return timestamp;
                } catch (IOException e) {
                    LOGGER.error("deserialize java.sql.Timestamp error : {}", e.getMessage(), e);
                }
            }
            LOGGER.error("deserialize java.sql.Timestamp type error.");
            return null;
        }
    }

    /**
     * the class of serialize blob type
     */
    public static class BlobSerializer extends JsonSerializer<SerialBlob> {

        @Override
        public void serializeWithType(SerialBlob blob, JsonGenerator gen, SerializerProvider serializers,
                                      TypeSerializer typeSer) throws IOException {
            WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen,
                    typeSer.typeId(blob, JsonToken.VALUE_EMBEDDED_OBJECT));
            serialize(blob, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeIdDef);
        }

        @Override
        public void serialize(SerialBlob blob, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            try {
                gen.writeBinary(blob.getBytes(1, (int) blob.length()));
            } catch (SerialException e) {
                LOGGER.error("serialize java.sql.Blob error : {}", e.getMessage(), e);
            }
        }
    }

    /**
     * the class of deserialize blob type
     */
    public static class BlobDeserializer extends JsonDeserializer<SerialBlob> {

        @Override
        public SerialBlob deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            try {
                return new SerialBlob(p.getBinaryValue());
            } catch (SQLException e) {
                LOGGER.error("deserialize java.sql.Blob error : {}", e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * the class of serialize clob type
     */
    public static class ClobSerializer extends JsonSerializer<SerialClob> {

        @Override
        public void serializeWithType(SerialClob clob, JsonGenerator gen, SerializerProvider serializers,
                                      TypeSerializer typeSer) throws IOException {
            WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen,
                    typeSer.typeId(clob, JsonToken.VALUE_EMBEDDED_OBJECT));
            serialize(clob, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeIdDef);
        }

        @Override
        public void serialize(SerialClob clob, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            try {
                gen.writeString(clob.getCharacterStream(), (int) clob.length());
            } catch (SerialException e) {
                LOGGER.error("serialize java.sql.Blob error : {}", e.getMessage(), e);
            }
        }
    }

    public static class ClobDeserializer extends JsonDeserializer<SerialClob> {

        @Override
        public SerialClob deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            try {
                return new SerialClob(p.getValueAsString().toCharArray());

            } catch (SQLException e) {
                LOGGER.error("deserialize java.sql.Clob error : {}", e.getMessage(), e);
            }
            return null;
        }
    }

}
