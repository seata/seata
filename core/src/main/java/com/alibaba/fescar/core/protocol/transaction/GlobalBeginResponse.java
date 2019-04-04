/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.core.protocol.transaction;

import com.alibaba.fescar.core.protocol.AbstractMessage;
import com.alibaba.fescar.core.protocol.CodecHelper;
import com.alibaba.fescar.core.protocol.FragmentXID;
import java.nio.ByteBuffer;

/**
 * The type Global begin response.
 *
 * @author jimin.jm @alibaba-inc.com
 */
public class GlobalBeginResponse extends AbstractTransactionResponse {

    private static final long serialVersionUID = -5947172130577163908L;

    private FragmentXID xid;

    private String extraData;

    /**
     * Gets xid.
     *
     * @return the xid
     */
    public FragmentXID getXid() {
        return xid;
    }

    /**
     * Sets xid.
     *
     * @param xid the xid
     */
    public void setXid(FragmentXID xid) {
        this.xid = xid;
    }

    /**
     * Gets extra data.
     *
     * @return the extra data
     */
    public String getExtraData() {
        return extraData;
    }

    /**
     * Sets extra data.
     *
     * @param extraData the extra data
     */
    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    @Override
    public short getTypeCode() {
        return AbstractMessage.TYPE_GLOBAL_BEGIN_RESULT;
    }

    @Override
    protected void doEncode() {
        super.doEncode();

        byteBuffer.put(xid.toBytes());
        CodecHelper.write(byteBuffer, extraData);
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);

        xid = CodecHelper.readFragmentXID(byteBuffer);
        extraData = CodecHelper.readString(byteBuffer);
    }
}
