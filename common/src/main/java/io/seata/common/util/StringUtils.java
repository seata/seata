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
    public static String toString(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj.getClass().isPrimitive()) {
            return String.valueOf(obj);
        }
        if (obj instanceof String) {
            return (String)obj;
        }
        if (obj instanceof Number || obj instanceof Character || obj instanceof Boolean) {
            return String.valueOf(obj);
        }
        if (obj instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(obj);
        }
        if (obj instanceof Collection) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            if (!((Collection)obj).isEmpty()) {
                for (Object o : (Collection)obj) {
                    sb.append(toString(o)).append(",");
                }
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj instanceof Map) {
            Map<Object, Object> map = (Map)obj;
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            if (!map.isEmpty()) {
                map.forEach((key, value) -> {
                    sb.append(toString(key)).append("->")
                        .append(toString(value)).append(",");
                });
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append("}");
            return sb.toString();
        }
        StringBuilder sb = new StringBuilder();
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            sb.append(field.getName());
            sb.append("=");
            try {
                Object f = field.get(obj);
                if (f.getClass() == obj.getClass()) {
                    sb.append(f.toString());
                } else {
                    sb.append(toString(f));
                }
            } catch (Exception e) {
            }
            sb.append(";");
        }
        return sb.toString();
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
