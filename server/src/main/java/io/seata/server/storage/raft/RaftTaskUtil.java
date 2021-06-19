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
package io.seata.server.storage.raft;

import java.nio.ByteBuffer;
import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.entity.Task;
import io.seata.core.raft.RaftServerFactory;
import io.seata.serializer.kryo.KryoInnerSerializer;
import io.seata.serializer.kryo.KryoSerializerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author funkye
 */
public class RaftTaskUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftTaskUtil.class);

    public static void createTask(Object data) {
        createTask(null, data);
    }

    public static void createTask(Closure done, Object data) {
        final Task task = new Task();
        KryoInnerSerializer kryo = KryoSerializerFactory.getInstance().get();
        try {
            task.setData(ByteBuffer.wrap(kryo.serialize(data)));
        } finally {
            KryoSerializerFactory.getInstance().returnKryo(kryo);
        }
        task.setDone(done == null ? status -> {
        } : done);
        RaftServerFactory.getInstance().getRaftServer().getNode().apply(task);
    }

}
