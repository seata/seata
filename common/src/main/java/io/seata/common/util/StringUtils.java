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
package io.seata.common.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import io.seata.common.Constants;
import io.seata.common.exception.ShouldNeverHappenException;

/**
 * The type String utils.
 *
 * @author slievrly
 * @author Geng Zhang
 */
public class StringUtils {

    private StringUtils() {
    }

    /**
     * empty string
     */
    public static final String EMPTY = "";

    /**
     * Space string
     */
    public static final String SPACE = " ";

    /**
     * Is empty boolean.
     *
     * @param str the str
     * @return the boolean
     */
    public static boolean isNullOrEmpty(String str) {
        return (str == null) || (str.isEmpty());
    }

    /**
     * Is blank string ?
     *
     * @param str the str
     * @return boolean boolean
     */
    public static boolean isBlank(String str) {
        int length;

        if ((str == null) || ((length = str.length()) == 0)) {
            return true;
        }
        for (int i = 0; i < length; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Is Not blank string ?
     *
     * @param str the str
     * @return boolean boolean
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * Equals boolean.
     *
     * @param a the a
     * @param b the b
     * @return boolean
     */
    public static boolean equals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    /**
     * Equals ignore case boolean.
     *
     * @param a the a
     * @param b the b
     * @return the boolean
     */
    public static boolean equalsIgnoreCase(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equalsIgnoreCase(b);
    }

    /**
     * Input stream 2 string string.
     *
     * @param is the is
     * @return the string
     */
    public static String inputStream2String(InputStream is) {
        if (is == null) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int i;
            while ((i = is.read()) != -1) {
                baos.write(i);
            }
            return baos.toString(Constants.DEFAULT_CHARSET_NAME);
        } catch (Exception e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    /**
     * Input stream to byte array
     *
     * @param is the is
     * @return the byte array
     */
    public static byte[] inputStream2Bytes(InputStream is) {
        if (is == null) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int i;
            while ((i = is.read()) != -1) {
                baos.write(i);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    /**
     * Object.toString()
     *
     * @param obj the obj
     * @return string string
     */
    @SuppressWarnings("deprecation")
    public static String toString(final Object obj) {
        if (obj == null) {
            return "null";
        }

        //region Convert simple types to String directly

        if (obj instanceof CharSequence || obj instanceof Number || obj instanceof Boolean || obj instanceof Character) {
            return obj.toString();
        }
        if (obj instanceof Date) {
            Date date = (Date)obj;
            long time = date.getTime();
            String dateFormat;
            if (date.getHours() == 0 && date.getMinutes() == 0 && date.getSeconds() == 0 && time % 1000 == 0) {
                dateFormat = "yyyy-MM-dd";
            } else if (time % (60 * 1000) == 0) {
                dateFormat = "yyyy-MM-dd HH:mm";
            } else if (time % 1000 == 0) {
                dateFormat = "yyyy-MM-dd HH:mm:ss";
            } else {
                dateFormat = "yyyy-MM-dd HH:mm:ss.SSS";
            }
            return new SimpleDateFormat(dateFormat).format(obj);
        }
        if (obj instanceof Enum) {
            return obj.getClass().getSimpleName() + "." + ((Enum)obj).name();
        }

        //endregion

        //region Convert the Collection and Map

        if (obj instanceof Collection) {
            Collection<?> col = (Collection<?>)obj;
            return CollectionUtils.toString(col);
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>)obj;
            return CollectionUtils.toString(map);
        }

        //endregion

        return CycleDependencyHandler.wrap(obj, o -> {
            StringBuilder sb = new StringBuilder(32);
            sb.append(obj.getClass().getSimpleName()).append("(");
            final int initialLength = sb.length();

            // Gets all fields, excluding static or synthetic fields
            Field[] fields = ReflectionUtil.getAllFields(obj.getClass());
            for (Field field : fields) {
                field.setAccessible(true);

                if (sb.length() > initialLength) {
                    sb.append(", ");
                }
                sb.append(field.getName());
                sb.append("=");
                try {
                    Object f = field.get(obj);
                    if (f == obj) {
                        sb.append("(this ").append(f.getClass().getSimpleName()).append(")");
                    } else {
                        sb.append(toString(f));
                    }
                } catch (Exception ignore) {
                }
            }

            sb.append(")");
            return sb.toString();
        });
    }

    /**
     * Trim string to null if empty("").
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed String
     */
    public static String trimToNull(final String str) {
        final String ts = trim(str);
        return isEmpty(ts) ? null : ts;
    }

    /**
     * Trim string, or null if string is null.
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed string, {@code null} if null String input
     */
    public static String trim(final String str) {
        return str == null ? null : str.trim();
    }

    /**
     * Checks if a CharSequence is empty ("") or null.
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * Checks if a CharSequence is not empty ("") and not null.
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }
}
