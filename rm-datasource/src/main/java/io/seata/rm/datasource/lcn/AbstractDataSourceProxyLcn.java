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
package io.seata.rm.datasource.lcn;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * The type Abstract data source proxy lcn.
 *
 * @author funkye
 */
public abstract class AbstractDataSourceProxyLcn implements DataSource {

    /**
     * The Target data source.
     */
    protected DataSource targetDataSource;

    /**
     * Instantiates a new Abstract data source proxy lcn.
     *
     * @param targetDataSource
     *            the target data source
     */
    public AbstractDataSourceProxyLcn(DataSource targetDataSource) {
        this.targetDataSource = targetDataSource;
    }

    /**
     * Gets target data source.
     *
     * @return the target data source
     */
    public DataSource getTargetDataSource() {
        return targetDataSource;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return targetDataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return targetDataSource.isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return targetDataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        targetDataSource.setLogWriter(out);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return targetDataSource.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        targetDataSource.setLoginTimeout(seconds);
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return targetDataSource.getParentLogger();
    }
}
