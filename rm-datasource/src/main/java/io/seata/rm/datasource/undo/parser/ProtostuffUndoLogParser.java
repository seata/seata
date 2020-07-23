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

import io.protostuff.Input;
import io.protostuff.LinkedBuffer;
import io.protostuff.Output;
import io.protostuff.Pipe;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.WireFormat.FieldType;
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.Delegate;
import io.protostuff.runtime.RuntimeEnv;
import io.protostuff.runtime.RuntimeSchema;
import io.seata.common.executor.Initialize;
import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.rm.datasource.undo.UndoLogParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;

/**
 * The type protostuff based undo log parser.
 *
 * @author Geng Zhang
 */
@LoadLevel(name = ProtostuffUndoLogParser.NAME)
public class ProtostuffUndoLogParser implements UndoLogParser, Initialize {

    public static final String NAME = "protostuff";

    private final DefaultIdStrategy idStrategy = (DefaultIdStrategy) RuntimeEnv.ID_STRATEGY;
    private Schema<BranchUndoLog> schema;

    @Override
    public void init() {
        ProtostuffConfigurerAdapter adapter = (ProtostuffConfigurerAdapter) CustomSerializerConfigurerAdapter.getConfig(NAME);
        if (null == adapter) {
            adapter = new ProtostuffConfigurerAdapter();
        }
        adapter.config(idStrategy);
        schema = RuntimeSchema.getSchema(BranchUndoLog.class, idStrategy);
    }

    @Override
    public String getName() {
        return ProtostuffUndoLogParser.NAME;
    }

    @Override
    public byte[] getDefaultContent() {
        return encode(new BranchUndoLog());
    }

    @Override
    public byte[] encode(BranchUndoLog branchUndoLog) {
        // Re-use (manage) this buffer to avoid allocating on every serialization
        LinkedBuffer buffer = LinkedBuffer.allocate(512);
        // ser
        try {
            return ProtostuffIOUtil.toByteArray(branchUndoLog, schema, buffer);
        } finally {
            buffer.clear();
        }
    }

    @Override
    public BranchUndoLog decode(byte[] bytes) {
        if (bytes.length == 0) {
            return new BranchUndoLog();
        }
        BranchUndoLog fooParsed = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(bytes, fooParsed, schema);
        return fooParsed;
    }

    /**
     * Delegate for java.sql.Timestamp
     *
     * @author zhangsen
     */
    public static class TimestampDelegate implements Delegate<java.sql.Timestamp> {

        @Override
        public FieldType getFieldType() {
            return FieldType.BYTES;
        }

        @Override
        public Class<?> typeClass() {
            return java.sql.Timestamp.class;
        }

        @Override
        public java.sql.Timestamp readFrom(Input input) throws IOException {
            ByteBuffer buffer = input.readByteBuffer();
            long time = buffer.getLong();
            int nanos = buffer.getInt();
            buffer.flip();
            java.sql.Timestamp timestamp = new Timestamp(time);
            timestamp.setNanos(nanos);
            return timestamp;
        }

        @Override
        public void writeTo(Output output, int number, java.sql.Timestamp value, boolean repeated) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(12);
            buffer.putLong(value.getTime());
            buffer.putInt(value.getNanos());
            buffer.flip();
            output.writeBytes(number, buffer, repeated);
        }

        @Override
        public void transfer(Pipe pipe, Input input, Output output, int number, boolean repeated) throws IOException {
            output.writeBytes(number, input.readByteBuffer(), repeated);
        }
    }

    /**
     * Delegate for java.sql.Date
     *
     * @author zhangsen
     */
    public static class SqlDateDelegate implements Delegate<java.sql.Date> {

        @Override
        public FieldType getFieldType() {
            return FieldType.FIXED64;
        }

        @Override
        public Class<?> typeClass() {
            return java.sql.Date.class;
        }

        @Override
        public java.sql.Date readFrom(Input input) throws IOException {
            return new java.sql.Date(input.readFixed64());
        }

        @Override
        public void transfer(Pipe pipe, Input input, Output output, int number, boolean repeated) throws IOException {
            output.writeFixed64(number, input.readFixed64(), repeated);
        }

        @Override
        public void writeTo(Output output, int number, java.sql.Date value, boolean repeated) throws IOException {
            output.writeFixed64(number, value.getTime(), repeated);
        }
    }

    /**
     * Delegate for java.sql.Time
     *
     * @author zhangsen
     */
    public static class TimeDelegate implements Delegate<java.sql.Time> {

        @Override
        public FieldType getFieldType() {
            return FieldType.FIXED64;
        }

        @Override
        public Class<?> typeClass() {
            return java.sql.Time.class;
        }

        @Override
        public java.sql.Time readFrom(Input input) throws IOException {
            return new java.sql.Time(input.readFixed64());
        }

        @Override
        public void transfer(Pipe pipe, Input input, Output output, int number, boolean repeated) throws IOException {
            output.writeFixed64(number, input.readFixed64(), repeated);
        }

        @Override
        public void writeTo(Output output, int number, java.sql.Time value, boolean repeated) throws IOException {
            output.writeFixed64(number, value.getTime(), repeated);
        }
    }

    /**
     * Delegate for java.util.Date
     *
     * @author zhangsen
     */
    public static class DateDelegate implements Delegate<java.util.Date> {

        @Override
        public FieldType getFieldType() {
            return FieldType.FIXED64;
        }

        @Override
        public Class<?> typeClass() {
            return java.util.Date.class;
        }

        @Override
        public java.util.Date readFrom(Input input) throws IOException {
            return new java.util.Date(input.readFixed64());
        }

        @Override
        public void transfer(Pipe pipe, Input input, Output output, int number, boolean repeated) throws IOException {
            output.writeFixed64(number, input.readFixed64(), repeated);
        }

        @Override
        public void writeTo(Output output, int number, java.util.Date value, boolean repeated) throws IOException {
            output.writeFixed64(number, value.getTime(), repeated);
        }
    }
}
