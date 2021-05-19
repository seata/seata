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

import io.seata.common.util.StringUtils;

import java.time.Duration;

/**
 * duration utils
 *
 * @author wang.liang
 */
public final class DurationUtils {

    private DurationUtils() {
    }

    /**
     * duration string to duration
     *
     * @param durationStr
     * @return duration
     */
    public static Duration toDuration(String durationStr) {
        if (StringUtils.isBlank(durationStr)) {
            throw new IllegalArgumentException("Illegal time string: " + durationStr);
        }

        durationStr = durationStr.replaceAll("\\s", "");

        try {
            char unit = durationStr.charAt(durationStr.length() - 1);
            if (unit >= '0' && unit <= '9') {
                long millis = Long.valueOf(durationStr);
                return Duration.ofMillis(millis);
            } else if (unit == 's' || unit == 'S') {
                long seconds = Long.valueOf(durationStr.substring(0, durationStr.length() - 1));
                return Duration.ofSeconds(seconds);
            } else if (unit == 'm' || unit == 'M') {
                long minutes = Long.valueOf(durationStr.substring(0, durationStr.length() - 1));
                return Duration.ofMinutes(minutes);
            } else if (unit == 'h' || unit == 'H') {
                long hours = Long.valueOf(durationStr.substring(0, durationStr.length() - 1));
                return Duration.ofHours(hours);
            } else if (unit == 'd' || unit == 'D') {
                long days = Long.valueOf(durationStr.substring(0, durationStr.length() - 1));
                return Duration.ofDays(days);
            } else {
                //unknown unit
                throw new Exception();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal time string: " + durationStr);
        }
    }

    /**
     * milliseconds to duration string
     *
     * @param millis milliseconds
     * @return duration string
     */
    public static String millisToString(long millis) {
        if (millis % 1000 != 0) {
            return Long.toString(millis);
        } else {
            return secondsToString(millis / 1000);
        }
    }

    /**
     * seconds to duration string
     *
     * @param seconds seconds
     * @return duration string
     */
    public static String secondsToString(long seconds) {
        if (seconds % 60 != 0) {
            return seconds + "s";
        } else {
            return minutesToString(seconds / 60);
        }
    }

    /**
     * minutes to duration string
     *
     * @param minutes minutes
     * @return duration string
     */
    public static String minutesToString(long minutes) {
        if (minutes % 60 != 0) {
            return minutes + "m";
        } else {
            return hoursToString(minutes / 60);
        }
    }

    /**
     * hours to duration string
     *
     * @param hours hours
     * @return duration string
     */
    public static String hoursToString(long hours) {
        if (hours % 60 != 0) {
            return hours + "h";
        } else {
            return daysToString(hours / 24);
        }
    }

    /**
     * days to duration string
     *
     * @param days days
     * @return duration string
     */
    public static String daysToString(long days) {
        return days + "d";
    }

    /**
     * milliseconds to duration string
     *
     * @param millis milliseconds
     * @return duration string
     */
    public static String millisToString2(long millis) {
        if (millis % 1000 != 0) {
            return millis + " milliseconds";
        } else {
            return secondsToString2(millis / 1000);
        }
    }

    /**
     * seconds to duration string
     *
     * @param seconds seconds
     * @return duration string
     */
    public static String secondsToString2(long seconds) {
        if (seconds % 60 != 0) {
            return seconds + " seconds";
        } else {
            return minutesToString2(seconds / 60);
        }
    }

    /**
     * minutes to duration string
     *
     * @param minutes minutes
     * @return duration string
     */
    public static String minutesToString2(long minutes) {
        if (minutes % 60 != 0) {
            return minutes + " minutes";
        } else {
            return hoursToString2(minutes / 60);
        }
    }

    /**
     * hours to duration string
     *
     * @param hours hours
     * @return duration string
     */
    public static String hoursToString2(long hours) {
        if (hours % 60 != 0) {
            return hours + " hours";
        } else {
            return daysToString2(hours / 24);
        }
    }

    /**
     * days to duration string
     *
     * @param days days
     * @return duration string
     */
    public static String daysToString2(long days) {
        return days + " days";
    }
}
