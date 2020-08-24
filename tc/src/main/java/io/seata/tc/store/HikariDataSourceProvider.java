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
package io.seata.tc.store;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.seata.common.loader.LoadLevel;
import io.seata.core.store.db.AbstractDataSourceProvider;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * The hikari datasource provider
 * @author diguage
 * @author will
 */
@LoadLevel(name = "hikari")
public class HikariDataSourceProvider extends AbstractDataSourceProvider {

    @Override
    public DataSource generate() {
        Properties properties = new Properties();
        properties.setProperty("dataSource.cachePrepStmts", "true");
        properties.setProperty("dataSource.prepStmtCacheSize", "250");
        properties.setProperty("dataSource.prepStmtCacheSqlLimit", "2048");
        properties.setProperty("dataSource.useServerPrepStmts", "true");
        properties.setProperty("dataSource.useLocalSessionState", "true");
        properties.setProperty("dataSource.rewriteBatchedStatements", "true");
        properties.setProperty("dataSource.cacheResultSetMetadata", "true");
        properties.setProperty("dataSource.cacheServerConfiguration", "true");
        properties.setProperty("dataSource.elideSetAutoCommits", "true");
        properties.setProperty("dataSource.maintainTimeStats", "false");

        HikariConfig config = new HikariConfig(properties);
        config.setDriverClassName(getDriverClassName());
        config.setJdbcUrl(getUrl());
        config.setUsername(getUser());
        config.setPassword(getPassword());
        config.setMaximumPoolSize(getMaxConn());
        config.setMinimumIdle(getMinConn());
        config.setAutoCommit(true);
        config.setConnectionTimeout(getMaxWait());
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }
}
