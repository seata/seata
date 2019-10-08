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
package io.seata.rm.saga.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import io.seata.common.Constants;

/**
 * The type Business activity context.
 *
 * @author zhangsen
 */
public class BusinessActivityContext implements Serializable {

    /**  */
    private static final long serialVersionUID = 6539226288677737992L;

    private Map<String, Object> context = new HashMap<String, Object>();

    /**
     * Instantiates a new Business activity context.
     */
    public BusinessActivityContext() {
    }

    /**
     * Instantiates a new Business activity context.
     *
     * @param context the context
     */
    public BusinessActivityContext(Map<String, Object> context) {
        this.context = context;
    }

    /**
     * Add context.
     *
     * @param key   the key
     * @param value the value
     */
    public void addContext(String key, Object value) {
        context.put(key, value);
    }

    /**
     * Fetch start time long.
     *
     * @return the long
     */
    public Long fetchStartTime() {
        return (Long)context.get(Constants.START_TIME);
    }

    /**
     * Get context object.
     *
     * @param key the key
     * @return the object
     */
    public Object getContext(String key) {
        return context.get(key);
    }

    /**
     * Gets context.
     *
     * @return the context
     */
    public Map<String, Object> getContext() {
        return context;
    }

    /**
     * Sets context.
     *
     * @param context the context
     */
    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    @Override
    public String toString() {
        return context.toString();
    }

}
