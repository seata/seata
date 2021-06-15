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
package io.seata.core.logger;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import io.seata.common.util.CollectionUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import org.slf4j.Logger;

import static io.seata.common.DefaultValues.DEFAULT_LOG_EXCEPTION_RATE;

/**
 * @author jsbxyyx
 */
public final class StackTraceLogger {

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    private static final String STACK_TRACE_LOGGER_PREFIX = "[stacktrace]";

    public static void info(Logger logger, Throwable cause, String format, Object[] args) {
        if (logger.isInfoEnabled()) {
            if (needToPrintStackTrace()) {
                logger.info(STACK_TRACE_LOGGER_PREFIX + format, buildNewArgs(args, cause));
            } else {
                logger.info(format, args);
            }
        }
    }

    public static void warn(Logger logger, Throwable cause, String format, Object[] args) {
        if (logger.isWarnEnabled()) {
            if (needToPrintStackTrace()) {
                logger.warn(STACK_TRACE_LOGGER_PREFIX + format, buildNewArgs(args, cause));
            } else {
                logger.warn(format, args);
            }
        }
    }

    public static void error(Logger logger, Throwable cause, String format, Object[] args) {
        if (logger.isErrorEnabled()) {
            if (needToPrintStackTrace()) {
                logger.error(STACK_TRACE_LOGGER_PREFIX + format, buildNewArgs(args, cause));
            } else {
                logger.error(format, args);
            }
        }
    }

    private static int getRate() {
        return CONFIG.getInt(ConfigurationKeys.TRANSACTION_LOG_EXCEPTION_RATE, DEFAULT_LOG_EXCEPTION_RATE);
    }

    private static boolean needToPrintStackTrace() {
        return needToPrintLog();
    }

    public static boolean needToPrintLog() {
        int rate = getRate();
        return ThreadLocalRandom.current().nextInt(rate) == 0;
    }

    private static Object[] buildNewArgs(Object[] args, Throwable cause) {
        if (CollectionUtils.isEmpty(args)) {
            return new Object[]{cause};
        } else {
            Object[] newArgs = Arrays.copyOf(args, args.length + 1);
            newArgs[args.length] = cause;
            return newArgs;
        }
    }
}
