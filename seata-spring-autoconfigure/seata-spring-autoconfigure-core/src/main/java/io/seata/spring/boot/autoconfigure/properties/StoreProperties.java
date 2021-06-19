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
package io.seata.spring.boot.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_SESSION_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_LOCK_PREFIX;

/**
 * @author spilledyear@outlook.com
 */
@Component
@ConfigurationProperties(prefix = STORE_PREFIX)
public class StoreProperties {
    /**
     * file, db, redis
     */
    private String mode = "file";

    private String publicKey = "";

    public String getMode() {
        return mode;
    }

    public StoreProperties setMode(String mode) {
        this.mode = mode;
        return this;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public StoreProperties setPublicKey(String publicKey) {
        this.publicKey = publicKey;
        return this;
    }


    @Component
    @ConfigurationProperties(prefix = STORE_SESSION_PREFIX)
    public static class Session {
        private String mode;

        public String getMode() {
            return mode;
        }

        public StoreProperties.Session setMode(String mode) {
            this.mode = mode;
            return this;
        }
    }


    @Component
    @ConfigurationProperties(prefix = STORE_LOCK_PREFIX)
    public static class Lock {
        private String mode;

        public String getMode() {
            return mode;
        }

        public StoreProperties.Lock setMode(String mode) {
            this.mode = mode;
            return this;
        }
    }
}
