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

import io.seata.common.loader.LoadLevel;
import io.seata.core.store.db.AbstractDataSourceProvider;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;

/**
 * The dbcp datasource provider
 * @author zhangsen
 * @author ggndnn
 * @author will
 */
@LoadLevel(name = "dbcp")
public class DbcpDataSourceProvider extends AbstractDataSourceProvider {

    @Override
    public DataSource generate() {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName(getDriverClassName());
        // DriverClassLoader works if upgrade commons-dbcp to at least 1.3.1.
        // https://issues.apache.org/jira/browse/DBCP-333
        ds.setDriverClassLoader(getDriverClassLoader());
        ds.setUrl(getUrl());
        ds.setUsername(getUser());

        ds.setPassword(getPassword());
        ds.setInitialSize(getMinConn());
        ds.setMaxTotal(getMaxConn());
        ds.setMinIdle(getMinConn());
        ds.setMaxIdle(getMinConn());
        ds.setMaxWaitMillis(getMaxWait());
        ds.setTimeBetweenEvictionRunsMillis(120000);
        ds.setNumTestsPerEvictionRun(1);
        ds.setTestWhileIdle(true);
        ds.setValidationQuery(getValidationQuery(getDBType()));
        ds.setConnectionProperties("useUnicode=yes;characterEncoding=utf8;socketTimeout=5000;connectTimeout=500");
        return ds;
    }
}
