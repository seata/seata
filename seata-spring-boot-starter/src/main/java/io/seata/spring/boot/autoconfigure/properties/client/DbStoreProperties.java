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
package io.seata.spring.boot.autoconfigure.properties.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.core.constants.DefaultValues.DEFAULT_DB_DATASOURCE;
import static io.seata.core.constants.DefaultValues.DEFAULT_DB_MAX_CONN;
import static io.seata.core.constants.DefaultValues.DEFAULT_DB_MAX_WAIT;
import static io.seata.core.constants.DefaultValues.DEFAULT_DB_MIN_CONN;
import static io.seata.core.constants.DefaultValues.DEFAULT_LOCK_DB_TABLE;
import static io.seata.core.constants.DefaultValues.DEFAULT_STORE_DB_BRANCH_TABLE;
import static io.seata.core.constants.DefaultValues.DEFAULT_STORE_DB_GLOBAL_TABLE;
import static io.seata.spring.boot.autoconfigure.StarterConstants.DB_STORE_PREFIX;

/**
 * @author wang.liang
 */
@Component
@ConfigurationProperties(prefix = DB_STORE_PREFIX)
public class DbStoreProperties {
    /**
     * the datasource: druid|dbcp|hikari
     */
    private String datasource = DEFAULT_DB_DATASOURCE;

    private String dbType;
    private String driverClassName;
    private String url;
    private String user;
    private String password;

    private int maxConn = DEFAULT_DB_MAX_CONN;
    private int minConn = DEFAULT_DB_MIN_CONN;
    private long maxWait = DEFAULT_DB_MAX_WAIT;

    private String globalTable = DEFAULT_STORE_DB_GLOBAL_TABLE;
    private String branchTable = DEFAULT_STORE_DB_BRANCH_TABLE;
    private String lockTable = DEFAULT_LOCK_DB_TABLE;

    public String getDatasource() {
        return datasource;
    }

    public DbStoreProperties setDatasource(String datasource) {
        this.datasource = datasource;
        return this;
    }

    public String getDbType() {
        return dbType;
    }

    public DbStoreProperties setDbType(String dbType) {
        this.dbType = dbType;
        return this;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public DbStoreProperties setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public DbStoreProperties setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getUser() {
        return user;
    }

    public DbStoreProperties setUser(String user) {
        this.user = user;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public DbStoreProperties setPassword(String password) {
        this.password = password;
        return this;
    }

    public int getMinConn() {
        return minConn;
    }

    public DbStoreProperties setMinConn(int minConn) {
        this.minConn = minConn;
        return this;
    }

    public int getMaxConn() {
        return maxConn;
    }

    public DbStoreProperties setMaxConn(int maxConn) {
        this.maxConn = maxConn;
        return this;
    }

    public long getMaxWait() {
        return maxWait;
    }

    public DbStoreProperties setMaxWait(long maxWait) {
        this.maxWait = maxWait;
        return this;
    }

    public String getGlobalTable() {
        return globalTable;
    }

    public DbStoreProperties setGlobalTable(String globalTable) {
        this.globalTable = globalTable;
        return this;
    }

    public String getBranchTable() {
        return branchTable;
    }

    public DbStoreProperties setBranchTable(String branchTable) {
        this.branchTable = branchTable;
        return this;
    }

    public String getLockTable() {
        return lockTable;
    }

    public DbStoreProperties setLockTable(String lockTable) {
        this.lockTable = lockTable;
        return this;
    }
}
