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
package io.seata.core.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import io.seata.common.loader.LoadLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * The type transmittable Thread local context core.
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.3.0
 */
@LoadLevel(name = "TransmittableThreadLocalContextCore", order = Integer.MIN_VALUE + 2)
public class TransmittableThreadLocalContextCore implements ContextCore {

    private TransmittableThreadLocal<Map<String, String>> ttlThreadLocal = new TransmittableThreadLocal<Map<String, String>>() {
        @Override
        protected Map<String, String> initialValue() {
            return new HashMap<>();
        }
    };

    @Override
    public String put(String key, String value) {
        return ttlThreadLocal.get().put(key, value);
    }

    @Override
    public String get(String key) {
        return ttlThreadLocal.get().get(key);
    }

    @Override
    public String remove(String key) {
        return ttlThreadLocal.get().remove(key);
    }

    @Override
    public Map<String, String> entries() {
        return ttlThreadLocal.get();
    }
}
