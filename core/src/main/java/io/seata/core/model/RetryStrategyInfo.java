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
package io.seata.core.model;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.StringUtils;

import java.time.Duration;

/**
 * Retry strategy info
 *
 * @author wang.liang
 */
public class RetryStrategyInfo implements RetryStrategy {

    //region constants

    public static final String STABLE_MODE = "S";
    public static final String INCREMENTAL_MODE = "I";
    public static final String PLAN_MODE = "P";

    //endregion


    //region fields

    // S=Stable, I=Incremental, P=Plan
    private String mode;

    //the fields for the mode S and I
    private Integer retryInterval; //unit is Seconds

    //the fields for the mode I
    private Integer maxRetryInterval; //unit is Seconds

    //the fields for the mode P
    private int[] retryIntervalPlan; //unit is Seconds


    //limit fields
    private Integer maxRetryCount;
    private Integer retryExpire; //unit is Seconds

    //endregion


    //region constructor

    public RetryStrategyInfo() {
    }

    // mode S
    public RetryStrategyInfo(int retryInterval) {
        this.mode = STABLE_MODE;
        this.retryInterval = retryInterval;
    }

    // mode S
    public RetryStrategyInfo(int retryInterval,
                             Integer maxRetryCount, Integer retryExpire) {
        this(retryInterval);
        this.maxRetryCount = maxRetryCount;
        this.retryExpire = retryExpire;
    }

    // mode I
    public RetryStrategyInfo(int retryInterval, Integer maxRetryInterval) {
        this.mode = INCREMENTAL_MODE;
        this.retryInterval = retryInterval;
        this.maxRetryInterval = maxRetryInterval;
    }

    // mode I
    public RetryStrategyInfo(int retryInterval, Integer maxRetryInterval,
                             Integer maxRetryCount, Integer retryExpire) {
        this(retryInterval, maxRetryInterval);
        this.maxRetryCount = maxRetryCount;
        this.retryExpire = retryExpire;
    }

    // mode P
    public RetryStrategyInfo(int[] retryIntervalPlan) {
        this.mode = PLAN_MODE;
        this.retryIntervalPlan = retryIntervalPlan;
    }

    // mode P
    public RetryStrategyInfo(int[] retryIntervalPlan,
                             Integer maxRetryCount, Integer retryExpire) {
        this(retryIntervalPlan);
        this.maxRetryCount = maxRetryCount;
        this.retryExpire = retryExpire;
    }

    public RetryStrategyInfo(String retryStrategyStr) {
        this.valueOf(retryStrategyStr);
    }

    //endregion


    //region RetryStrategy API

    /**
     * is expired
     *
     * @param globalTransactionBeginTime the global transaction begin time
     * @return the boolean
     */
    @Override
    public boolean isExpired(long globalTransactionBeginTime) {
        if (this.retryExpire != null && this.retryExpire > 0) {
            return System.currentTimeMillis() > globalTransactionBeginTime + this.retryExpire * 1000;
        } else {
            return false;
        }
    }

    /**
     * is reached max retry count
     *
     * @param branchRetryCount
     * @return the boolean
     */
    @Override
    public boolean isReachedMaxRetryCount(int branchRetryCount) {
        if (this.maxRetryCount != null && this.maxRetryCount > 0) {
            return branchRetryCount >= this.maxRetryCount;
        } else {
            return false;
        }
    }

    /**
     * next retry interval
     *
     * @param branchRetryCount the count of the branch retries so far
     * @return the retry interval, the unit is milliseconds
     */
    @Override
    public long nextRetryInterval(int branchRetryCount) {
        if (mode == null) {
            return 0L;
        }

        if (branchRetryCount <= 0) {
            throw new IllegalArgumentException("branchRetryCount must be greater than 0");
        }

        int interval;
        if (STABLE_MODE.equalsIgnoreCase(mode) && retryInterval != null && retryInterval > 0) {
            interval = retryInterval;
        } else if (INCREMENTAL_MODE.equalsIgnoreCase(mode) && retryInterval != null && retryInterval > 0) {
            interval = retryInterval * branchRetryCount;
            if (maxRetryInterval != null && maxRetryInterval > 0 && interval > maxRetryInterval) {
                interval = maxRetryInterval;
            }
        } else if (PLAN_MODE.equalsIgnoreCase(mode) && retryIntervalPlan != null && retryIntervalPlan.length > 0) {
            if (branchRetryCount >= retryIntervalPlan.length) {
                interval = retryIntervalPlan[retryIntervalPlan.length - 1];
            } else {
                interval = retryIntervalPlan[branchRetryCount - 1];
            }
        } else {
            return 0L;
        }

        return 1000 * interval;
    }

    //endregion


    //region data convert

    @Override
    public String toString() {
        boolean isNoMode = StringUtils.isBlank(mode) || mode.length() > 1 || !(STABLE_MODE + INCREMENTAL_MODE + PLAN_MODE).contains(mode.toUpperCase());
        boolean isNoMaxRetryCount = maxRetryCount == null || maxRetryCount <= 0;
        boolean isNoRetryExpire = retryExpire == null || retryExpire <= 0;
        if (isNoMode && isNoMaxRetryCount && isNoRetryExpire) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // mode
        if (StringUtils.isBlank(mode) || mode.length() > 1 || !(STABLE_MODE + INCREMENTAL_MODE + PLAN_MODE).contains(mode.toUpperCase())) {
            sb.append("-");
        } else {
            sb.append(mode.toUpperCase()).append(",");

            if (STABLE_MODE.equalsIgnoreCase(mode) || INCREMENTAL_MODE.equalsIgnoreCase(mode)) {
                // S or I: retry interval
                sb.append(DurationUtils.secondsToString(retryInterval));
                if (INCREMENTAL_MODE.equalsIgnoreCase(mode) && maxRetryInterval != null && maxRetryInterval > 0) {
                    // I: max retry count
                    sb.append(",").append(DurationUtils.secondsToString(maxRetryInterval));
                }
            } else {
                // P: retry interval plan
                for (int i = 0, l = retryIntervalPlan.length; i < l; ++i) {
                    sb.append(DurationUtils.secondsToString(retryIntervalPlan[i]));
                    if (i < l - 1) {
                        sb.append(",");
                    }
                }
            }
        }

        if (!isNoMaxRetryCount || !isNoRetryExpire) {
            //On the left is the mode data.
            //On the right is the limit data
            sb.append("|");

            //max retry count
            sb.append(maxRetryCount == null || maxRetryCount <= 0 ? "-" : maxRetryCount).append(",");

            //expire
            sb.append(retryExpire == null || retryExpire <= 0 ? "-" : DurationUtils.secondsToString(retryExpire));
        }

        return sb.toString();
    }

    public void valueOf(String retryStrategyStr) {
        if (StringUtils.isBlank(retryStrategyStr)) {
            return;
        }

        //temporary variable
        String mode;
        Integer retryInterval = null; //unit is Seconds
        Integer maxRetryInterval = null; //unit is Seconds
        int[] retryIntervalPlan = null; //unit is Seconds
        Integer maxRetryCount = null;
        Integer retryExpire = null; //unit is Seconds

        try {
            String[] arr = retryStrategyStr.replaceAll("[-\\s]", "").split("\\|");
            String[] arr1 = arr[0].split(",");
            String[] arr2 = arr.length > 1 ? arr[1].split(",") : null;

            //mode fields
            mode = arr1[0].toUpperCase();
            if (mode == null || mode.isEmpty() || mode.length() > 1 || !(STABLE_MODE + INCREMENTAL_MODE + PLAN_MODE).contains(mode.toUpperCase())) {
                throw new Exception("Illegal mode: " + mode);
            }
            if (STABLE_MODE.equalsIgnoreCase(mode) || INCREMENTAL_MODE.equalsIgnoreCase(mode)) {
                // retry interval
                retryInterval = this.toSeconds(arr1[1]);
                if (retryInterval == null || retryInterval <= 0) {
                    throw new Exception("Illegal retryInterval: " + retryInterval);
                }
                if (INCREMENTAL_MODE.equalsIgnoreCase(mode) && arr1.length > 2) {
                    // max retry interval
                    maxRetryInterval = this.toSeconds(arr1[2]);
                }
            } else {
                // retry interval plan
                Integer interval;
                retryIntervalPlan = new int[arr1.length - 1];
                for (int i = 1; i < arr1.length; ++i) {
                    interval = this.toSeconds(arr1[i]);
                    if (interval == null) {
                        throw new Exception("Illegal retryIntervalPlan[" + i + "]: " + interval);
                    }
                    retryIntervalPlan[i - 1] = interval;
                }
            }

            // limit fields
            if (arr2 != null && arr2.length > 0) {
                // max retry count
                if (StringUtils.isNotEmpty(arr2[0])) {
                    maxRetryCount = Integer.valueOf(arr2[0]);
                    if (maxRetryCount <= 0) {
                        maxRetryCount = null;
                    }
                }
                // retry expire
                if (arr2.length > 1) {
                    retryExpire = this.toSeconds(arr2[1]);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal string of the retry strategy: " + retryStrategyStr, e);
        }

        this.mode = mode.toUpperCase();
        this.retryInterval = retryInterval;
        this.maxRetryInterval = maxRetryInterval;
        this.retryIntervalPlan = retryIntervalPlan;
        this.maxRetryCount = maxRetryCount;
        this.retryExpire = retryExpire;
    }

    private Integer toSeconds(String durationStr) {
        Integer seconds = null;
        if (StringUtils.isNotEmpty(durationStr)) {
            char unit = durationStr.charAt(durationStr.length() - 1);
            if (unit >= '1' && unit <= '9') {
                //Too small a unit causes the generated string to be too long. Therefore, milliseconds are not recommended.
                throw new NotSupportYetException("Not support the time unit: millisecond. It causes the generated string to be too long.");
            }

            Duration duration = DurationUtils.toDuration(durationStr);
            seconds = (int) duration.getSeconds();
            if (seconds <= 0) {
                seconds = null;
            }
        }
        return seconds;
    }

    //endregion


    //region is empty

    /**
     * is empty instance
     *
     * @return the boolean
     */
    public boolean isEmpty() {
        return (mode == null || mode.length() == 0)
                && (retryExpire == null || retryExpire <= 0)
                && (maxRetryCount == null || maxRetryCount <= 0)
                && (retryInterval == null || retryInterval <= 0)
                && (maxRetryInterval == null || maxRetryInterval <= 0)
                && (retryIntervalPlan == null || retryIntervalPlan.length == 0);
    }

    //endregion


    //region Gets and Sets

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Integer getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Integer retryInterval) {
        this.retryInterval = retryInterval;
    }

    public Integer getMaxRetryInterval() {
        return maxRetryInterval;
    }

    public void setMaxRetryInterval(Integer maxRetryInterval) {
        this.maxRetryInterval = maxRetryInterval;
    }

    public int[] getRetryIntervalPlan() {
        return retryIntervalPlan;
    }

    public void setRetryIntervalPlan(int[] retryIntervalPlan) {
        this.retryIntervalPlan = retryIntervalPlan;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public Integer getRetryExpire() {
        return retryExpire;
    }

    public void setRetryExpire(Integer retryExpire) {
        this.retryExpire = retryExpire;
    }

    //endregion
}
