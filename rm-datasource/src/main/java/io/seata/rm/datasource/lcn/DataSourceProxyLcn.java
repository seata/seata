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

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.util.JdbcUtils;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * The type Data source proxy lcn.
 *
 * @author funkye
 */
public class DataSourceProxyLcn extends AbstractDataSourceProxyLcn implements Resource {

    protected static final String DEFAULT_RESOURCE_GROUP_ID = "DEFAULT_LCN";
    private String resourceGroupId;
    private String jdbcUrl;

    private String dbType;

    private String userName;

    /**
     * Instantiates a new Abstract data source proxy lcn.
     *
     * @param targetDataSource
     *            the target data source
     */
    public DataSourceProxyLcn(DataSource targetDataSource) {
        super(targetDataSource);
        init(targetDataSource, resourceGroupId);
    }

    private void init(DataSource dataSource, String resourceGroupId) {
        if (StringUtils.isBlank(resourceGroupId)) {
            this.resourceGroupId = DEFAULT_RESOURCE_GROUP_ID;
        } else {
            this.resourceGroupId = resourceGroupId;
        }
        try (Connection connection = dataSource.getConnection()) {
            jdbcUrl = connection.getMetaData().getURL();
            dbType = JdbcUtils.getDbType(jdbcUrl);
            if (JdbcConstants.ORACLE.equals(dbType)) {
                userName = connection.getMetaData().getUserName();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("can not init dataSource", e);
        }
        DefaultResourceManager.get().registerResource(this);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection targetConnection = targetDataSource.getConnection();
        return getConnection(targetConnection);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection targetConnection = targetDataSource.getConnection(username, password);
        return getConnection(targetConnection);
    }

    public Connection getConnection(Connection connection) {
        if (RootContext.inGlobalTransaction()) {
            ConnectionProxyLcn connectionProxyLcn = new ConnectionProxyLcn(connection, this, RootContext.getXID());
            return connectionProxyLcn;
        } else {
            return connection;
        }
    }

    @Override
    public String getResourceGroupId() {
        return DEFAULT_RESOURCE_GROUP_ID;
    }

    @Override
    public String getResourceId() {
        if (JdbcConstants.POSTGRESQL.equals(dbType)) {
            return getPGResourceId();
        } else if (JdbcConstants.ORACLE.equals(dbType) && userName != null) {
            return getDefaultResourceId() + "/" + userName;
        } else {
            return getDefaultResourceId();
        }
    }

    /**
     * get the default resource id
     * 
     * @return resource id
     */
    private String getDefaultResourceId() {
        if (jdbcUrl.contains("?")) {
            return jdbcUrl.substring(0, jdbcUrl.indexOf('?'));
        } else {
            return jdbcUrl;
        }
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.LCN;
    }

    /**
     * prevent pg sql url like jdbc:postgresql://127.0.0.1:5432/seata?currentSchema=public
     * jdbc:postgresql://127.0.0.1:5432/seata?currentSchema=seata cause the duplicated resourceId it will cause the
     * problem like 1.get file lock fail 2.error table meta cache
     * 
     * @return resourceId
     */
    private String getPGResourceId() {
        if (jdbcUrl.contains("?")) {
            StringBuilder jdbcUrlBuilder = new StringBuilder();
            jdbcUrlBuilder.append(jdbcUrl.substring(0, jdbcUrl.indexOf('?')));
            StringBuilder paramsBuilder = new StringBuilder();
            String paramUrl = jdbcUrl.substring(jdbcUrl.indexOf('?') + 1, jdbcUrl.length());
            String[] urlParams = paramUrl.split("&");
            for (String urlParam : urlParams) {
                if (urlParam.contains("currentSchema")) {
                    paramsBuilder.append(urlParam);
                    break;
                }
            }

            if (paramsBuilder.length() > 0) {
                jdbcUrlBuilder.append("?");
                jdbcUrlBuilder.append(paramsBuilder);
            }
            return jdbcUrlBuilder.toString();
        } else {
            return jdbcUrl;
        }
    }

}
