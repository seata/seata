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
package io.seata.core.store.db;

import io.seata.common.exception.StoreException;
import io.seata.common.executor.Initialize;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.constants.DBType;

import javax.sql.DataSource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static io.seata.core.constants.DefaultValues.DEFAULT_DB_MAX_CONN;
import static io.seata.core.constants.DefaultValues.DEFAULT_DB_MAX_WAIT;
import static io.seata.core.constants.DefaultValues.DEFAULT_DB_MIN_CONN;

/**
 * The abstract datasource provider
 * 
 * @author zhangsen
 * @author will
 */
public abstract class AbstractDataSourceProvider implements DataSourceProvider, Initialize {

    private DataSource dataSource;

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    private final static String MYSQL_DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";

    private final static String MYSQL8_DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";

    private final static String MYSQL_DRIVER_FILE_PREFIX = "mysql-connector-java-";

    private final static Map<String, ClassLoader> MYSQL_DRIVER_LOADERS;

    static {
        MYSQL_DRIVER_LOADERS = createMysqlDriverClassLoaders();
    }

    @Override
    public void init() {
        this.dataSource = generate();
    }

    @Override
    public DataSource provide() {
        return this.dataSource;
    }

    /**
     * generate the datasource
     * @return datasource
     */
    public abstract DataSource generate();

    /**
     * Get db type db type.
     *
     * @return the db type
     */
    protected DBType getDBType() {
        return DBType.valueof(CONFIG.getConfig(ConfigurationKeys.STORE_DB_TYPE));
    }

    /**
     * get db driver class name
     *
     * @return the db driver class name
     */
    protected String getDriverClassName() {
        String driverClassName = CONFIG.getConfig(ConfigurationKeys.STORE_DB_DRIVER_CLASS_NAME);
        if (StringUtils.isBlank(driverClassName)) {
            throw new StoreException(
                String.format("the {%s} can't be empty", ConfigurationKeys.STORE_DB_DRIVER_CLASS_NAME));
        }
        return driverClassName;
    }

    /**
     * get db max wait
     *
     * @return the db max wait
     */
    protected Long getMaxWait() {
        Long maxWait = CONFIG.getLong(ConfigurationKeys.STORE_DB_MAX_WAIT, DEFAULT_DB_MAX_WAIT);
        return maxWait;
    }

    protected ClassLoader getDriverClassLoader() {
        return MYSQL_DRIVER_LOADERS.getOrDefault(getDriverClassName(), ClassLoader.getSystemClassLoader());
    }

    private static Map<String, ClassLoader> createMysqlDriverClassLoaders() {
        Map<String, ClassLoader> loaders = new HashMap<>();
        String cp = System.getProperty("java.class.path");
        if (cp == null || cp.isEmpty()) {
            return loaders;
        }
        Stream.of(cp.split(File.pathSeparator))
                .map(File::new)
                .filter(File::exists)
                .map(file -> file.isFile() ? file.getParentFile() : file)
                .filter(Objects::nonNull)
                .filter(File::isDirectory)
                .map(file -> new File(file, "jdbc"))
                .filter(File::exists)
                .filter(File::isDirectory)
                .distinct()
                .flatMap(file -> {
                    File[] files = file.listFiles((f, name) -> name.startsWith(MYSQL_DRIVER_FILE_PREFIX));
                    if (files != null) {
                        return Stream.of(files);
                    } else {
                        return Stream.of();
                    }
                })
                .forEach(file -> {
                    if (loaders.containsKey(MYSQL8_DRIVER_CLASS_NAME) && loaders.containsKey(MYSQL_DRIVER_CLASS_NAME)) {
                        return;
                    }
                    try {
                        URL url = file.toURI().toURL();
                        ClassLoader loader = new URLClassLoader(new URL[]{url}, ClassLoader.getSystemClassLoader());
                        try {
                            loader.loadClass(MYSQL8_DRIVER_CLASS_NAME);
                            loaders.putIfAbsent(MYSQL8_DRIVER_CLASS_NAME, loader);
                        } catch (ClassNotFoundException e) {
                            loaders.putIfAbsent(MYSQL_DRIVER_CLASS_NAME, loader);
                        }
                    } catch (MalformedURLException ignore) {
                    }
                });
        return loaders;
    }

    /**
     * Get url string.
     *
     * @return the string
     */
    protected String getUrl() {
        String url = CONFIG.getConfig(ConfigurationKeys.STORE_DB_URL);
        if (StringUtils.isBlank(url)) {
            throw new StoreException(String.format("the {%s} can't be empty", ConfigurationKeys.STORE_DB_URL));
        }
        return url;
    }

    /**
     * Get user string.
     *
     * @return the string
     */
    protected String getUser() {
        String user = CONFIG.getConfig(ConfigurationKeys.STORE_DB_USER);
        if (StringUtils.isBlank(user)) {
            throw new StoreException(String.format("the {%s} can't be empty", ConfigurationKeys.STORE_DB_USER));
        }
        return user;
    }

    /**
     * Get password string.
     *
     * @return the string
     */
    protected String getPassword() {
        String password = CONFIG.getConfig(ConfigurationKeys.STORE_DB_PASSWORD);
        return password;
    }

    /**
     * Get min conn int.
     *
     * @return the int
     */
    protected int getMinConn() {
        int minConn = CONFIG.getInt(ConfigurationKeys.STORE_DB_MIN_CONN, DEFAULT_DB_MIN_CONN);
        return minConn < 0 ? DEFAULT_DB_MIN_CONN : minConn;
    }

    /**
     * Get max conn int.
     *
     * @return the int
     */
    protected int getMaxConn() {
        int maxConn = CONFIG.getInt(ConfigurationKeys.STORE_DB_MAX_CONN, DEFAULT_DB_MAX_CONN);
        return maxConn < 0 ? DEFAULT_DB_MAX_CONN : maxConn;
    }

    /**
     * Get validation query string.
     *
     * @param dbType the db type
     * @return the string
     */
    protected String getValidationQuery(DBType dbType) {
        if (DBType.ORACLE.equals(dbType)) {
            return "select sysdate from dual";
        } else {
            return "select 1";
        }
    }
}
