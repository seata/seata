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
package io.seata.compressor.bzip2;

import io.seata.common.util.IOUtil;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * the BZIP2 Util
 *
 * @author ph3636
 */
public class BZip2Util {

    private static final int BUFFER_SIZE = 8192;

    public static byte[] compress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        ByteArrayOutputStream bos = null;
        CBZip2OutputStream bzip2;
        try {
            bos = new ByteArrayOutputStream();
            bzip2 = new CBZip2OutputStream(bos);
            bzip2.write(bytes);
            bzip2.flush();
            bzip2.close();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("bzip2 compress error", e);
        } finally {
            IOUtil.close(bos);
        }
    }

    public static byte[] decompress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        ByteArrayOutputStream out = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        CBZip2InputStream bzip2;
        try {
            out = new ByteArrayOutputStream();
            bzip2 = new CBZip2InputStream(bis);
            byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            while ((n = bzip2.read(buffer)) > -1) {
                out.write(buffer, 0, n);
            }
            bzip2.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("bzip2 decompress error", e);
        } finally {
            IOUtil.close(out, bis);
        }
    }
}
