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
package io.seata.core.store.redis;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;

/**
 * @author funkye
 */
public abstract class AbstractJedisPooledProvider implements JedisPooledProvider {

    protected static final String HOST = "127.0.0.1";

    protected static final int PORT = 6379;

    protected static final int MINCONN = 1;

    protected static final int MAXCONN = 10;

    protected static final int DATABASE = 0;

    protected static final int MAXTOTAL = 100;

    protected static final int SENTINEL_HOST_NUMBER = 3;

    protected static final int TIMEOUT = 60000;

    protected static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();

}
