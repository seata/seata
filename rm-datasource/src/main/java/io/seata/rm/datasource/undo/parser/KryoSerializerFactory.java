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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author jsbxyyx
 */
public class KryoSerializerFactory implements KryoFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(KryoSerializerFactory.class);

    private static final KryoSerializerFactory FACTORY = new KryoSerializerFactory();

    private KryoPool pool = new KryoPool.Builder(this).softReferences().build();

    private KryoSerializerFactory() {
    }

    public static KryoSerializerFactory getInstance() {
        return FACTORY;
    }

    public KryoSerializer get() {
        return new KryoSerializer(pool.borrow());
    }

    public void returnKryo(KryoSerializer kryoSerializer) {
        if (kryoSerializer == null) {
            throw new IllegalArgumentException("kryoSerializer is null");
        }
        pool.release(kryoSerializer.getKryo());
    }

    @Override
    public Kryo create() {
        Kryo kryo = new Kryo();
        KryoConfigurerAdapter adapter = (KryoConfigurerAdapter) CustomSerializerConfigurerAdapter.getConfig(KryoUndoLogParser.NAME);
        if (null == adapter) {
            adapter = new KryoConfigurerAdapter();
        }
        adapter.config(kryo);
        return kryo;
    }

    public static class BlobSerializer extends Serializer<Blob> {

        @Override
        public void write(Kryo kryo, Output output, Blob object) {
            try {
                byte[] bytes = object.getBytes(1L, (int) object.length());
                output.writeInt(bytes.length, true);
                output.write(bytes);
            } catch (SQLException e) {
                LOGGER.error("kryo write java.sql.Blob error: {}", e.getMessage(), e);
            }
        }

        @Override
        public Blob read(Kryo kryo, Input input, Class<Blob> type) {
            int length = input.readInt(true);
            byte[] bytes = input.readBytes(length);
            try {
                return new SerialBlob(bytes);
            } catch (SQLException e) {
                LOGGER.error("kryo read java.sql.Blob error: {}", e.getMessage(), e);
            }
            return null;
        }

    }

    public static class ClobSerializer extends Serializer<Clob> {

        @Override
        public void write(Kryo kryo, Output output, Clob object) {
            try {
                String s = object.getSubString(1, (int) object.length());
                output.writeString(s);
            } catch (SQLException e) {
                LOGGER.error("kryo write java.sql.Clob error: {}", e.getMessage(), e);
            }
        }

        @Override
        public Clob read(Kryo kryo, Input input, Class<Clob> type) {
            try {
                String s = input.readString();
                return new SerialClob(s.toCharArray());
            } catch (SQLException e) {
                LOGGER.error("kryo read java.sql.Clob error: {}", e.getMessage(), e);
            }
            return null;
        }

    }

    public static class TimestampSerializer extends Serializer<Timestamp> {

        @Override
        public void write(Kryo kryo, Output output, Timestamp object) {
            output.writeLong(object.getTime());
            output.writeInt(object.getNanos());
        }

        @Override
        public Timestamp read(Kryo kryo, Input input, Class<Timestamp> type) {
            Timestamp timestamp = new Timestamp(input.readLong());
            timestamp.setNanos(input.readInt());
            return timestamp;
        }
    }
}