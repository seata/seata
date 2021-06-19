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
package io.seata.serializer.hessian;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import io.seata.common.loader.LoadLevel;
import io.seata.core.serializer.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author XinWang
 */
@LoadLevel(name = "HESSIAN")
public class HessianSerializer implements Serializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HessianSerializer.class);

    @Override
    public <T> byte[] serialize(T t) {
        byte[] stream = null;
        SerializerFactory hessian = HessianSerializerFactory.getInstance();
        try {
            com.caucho.hessian.io.Serializer serializer = hessian.getSerializer(t.getClass());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Hessian2Output output = new Hessian2Output(baos);
            serializer.writeObject(t, output);
            output.close();
            stream = baos.toByteArray();
        } catch (IOException e) {
            LOGGER.error("Hessian encode error:{}", e.getMessage(), e);
        }
        return stream;
    }

    @Override
    public <T> T deserialize(byte[] bytes) {
        T obj = null;
        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes);) {
            Hessian2Input input = new Hessian2Input(is);
            obj = (T) input.readObject();
            input.close();
        } catch (IOException e) {
            LOGGER.error("Hessian decode error:{}", e.getMessage(), e);
        }
        return obj;
    }
}
