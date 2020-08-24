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
package io.seata.tc.store.db;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.core.store.db.DataSourceProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

/**
 * @author: will
 */
public class AbstractDataSourceProviderTest {

    private final String dbcpDatasourceType = "dbcp";

    private final String druidDatasourceType = "druid";

    private final String hikariDatasourceType = "hikari";

    @Test
    public void testDbcpDataSourceProvider() {
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbcpDatasourceType).provide();
        Assertions.assertNotNull(dataSource);
    }

    @Test
    public void testDruidDataSourceProvider() {
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, druidDatasourceType).provide();
        Assertions.assertNotNull(dataSource);
    }

    @Test
    public void testHikariDataSourceProvider() {
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, hikariDatasourceType).provide();
        Assertions.assertNotNull(dataSource);
    }
}
