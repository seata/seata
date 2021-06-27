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
package io.seata.rm.tcc;

import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.parameterfetcher.MockBooleanParameterFetcher;

/**
 * The type Tcc param.
 *
 * @author zhangsen
 */
public class TccParam {

    /**
     * The Num.
     */
    @BusinessActionContextParameter
    protected int num;

    /**
     * The Name, this field has no annotation
     */
    protected String name;

    /**
     * The Email.
     */
    @BusinessActionContextParameter(paramName = "email0")
    protected String email;

    /**
     * The Remark.
     */
    @BusinessActionContextParameter(paramName = "remark")
    protected String remark;

    /**
     * The flag, try to use the fetcher
     */
    @BusinessActionContextParameter(isParamInProperty = true, fetcher = MockBooleanParameterFetcher.class)
    protected Boolean flag;

    /**
     * Instantiates a new Tcc param.
     */
    public TccParam() {
    }

    /**
     * Instantiates a new Tcc param.
     *
     * @param num    the num
     * @param name   the name
     * @param email  the email
     * @param remark the remark
     * @param flag   the flag
     */
    public TccParam(int num, String name, String email, String remark, Boolean flag) {
        this.num = num;
        this.name = name;
        this.email = email;
        this.remark = remark;
        this.flag = flag;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Boolean getFlag() {
        return flag;
    }

    public void setFlag(Boolean flag) {
        this.flag = flag;
    }
}
