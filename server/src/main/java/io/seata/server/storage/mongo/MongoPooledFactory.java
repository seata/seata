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
package io.seata.server.storage.mongo;

import java.util.ArrayList;
import java.util.List;

import io.seata.core.constants.ConfigurationKeys;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;

/**
 * @author funkye
 */
public class MongoPooledFactory {

    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(MongoPooledFactory.class);

    private static volatile MongoClient mongoClient = null;

    private static final String DEFAULT_BRANCH_TABLE_NAME = "branch_table";

    private static final String DEFAULT_GLOBAL_TABLE_NAME = "global_table";

    private static final String DEFAULT_LOCK_TABLE_NAME = "lock_table";

    private static String DEFAULT_DB_NAME = "seata";

    private static final String HOST = "127.0.0.1";

    private static final int PORT = 27017;

    private static final String USERNAME = "mongo";

    private static final String PASSWORD = "mongo";

    private static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();

    /**
     * get the mongo db pool instance (singleton)
     *
     * @return mongoClient
     */
    public static MongoClient getMongoPoolInstance(MongoClient... client) {
        if (mongoClient == null) {
            synchronized (MongoPooledFactory.class) {
                if (mongoClient == null) {
                    if (client != null && client.length > 0) {
                        mongoClient = client[0];
                    } else {
                        MongoClientOptions.Builder build = new MongoClientOptions.Builder();
                        build.connectionsPerHost(50);
                        build.maxWaitTime(1000 * 60 * 2);
                        build.connectTimeout(1000 * 60 * 1);
                        build.threadsAllowedToBlockForConnectionMultiplier(50);
                        MongoClientOptions mongoClientOptions = build.build();
                        ServerAddress serverAddress =
                            new ServerAddress(CONFIGURATION.getConfig(ConfigurationKeys.STORE_MONGO_HOST, HOST),
                                CONFIGURATION.getInt(ConfigurationKeys.STORE_MONGO_PORT, PORT));
                        List<ServerAddress> address = new ArrayList<ServerAddress>();
                        address.add(serverAddress);
                        DEFAULT_DB_NAME =
                            CONFIGURATION.getConfig(ConfigurationKeys.STORE_MONGO_DATA_BASE_NAME, DEFAULT_DB_NAME);
                        MongoCredential credential = MongoCredential.createScramSha1Credential(
                            CONFIGURATION.getConfig(ConfigurationKeys.STORE_MONGO_USERNAME, USERNAME), DEFAULT_DB_NAME,
                            CONFIGURATION.getConfig(ConfigurationKeys.STORE_MONGO_PASSWORD, PASSWORD).toCharArray());
                        mongoClient = new MongoClient(address, credential, mongoClientOptions);
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("initialization of the build mongo db connection pool is complete");
                        }
                    }
                }
            }
        }
        return mongoClient;
    }

    public static MongoCollection<Document> getLockCollection() {
        return getMongoPoolInstance().getDatabase(DEFAULT_DB_NAME).getCollection(DEFAULT_LOCK_TABLE_NAME);
    }

    public static MongoCollection<Document> getGlobalCollection() {
        return getMongoPoolInstance().getDatabase(DEFAULT_DB_NAME).getCollection(DEFAULT_GLOBAL_TABLE_NAME);
    }

    public static MongoCollection<Document> getBranchCollection() {
        return getMongoPoolInstance().getDatabase(DEFAULT_DB_NAME).getCollection(DEFAULT_BRANCH_TABLE_NAME);
    }

}
