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
package io.seata.spring.schema;

import java.lang.reflect.Method;
import java.util.Set;

import io.netty.util.internal.ConcurrentSet;

/**
 * The type seata target holder
 *
 * @author xingfudeshi@gmail.com
 */
public enum SeataTargetHolder {
    /**
     * instance
     */
    INSTANCE;
    private Set<SeataTarget> seataTargets;

    SeataTargetHolder() {
        this.seataTargets = new ConcurrentSet<>();
    }

    /**
     * add
     *
     * @param seataTarget
     * @return boolean
     * @author xingfudeshi@gmail.com
     */
    public boolean add(SeataTarget seataTarget) {
        return this.seataTargets.add(seataTarget);
    }

    /**
     * contains
     *
     * @param seataTarget
     * @return boolean
     * @author xingfudeshi@gmail.com
     */
    public boolean contains(SeataTarget seataTarget) {
        return this.seataTargets.contains(seataTarget);
    }

    /**
     * remove
     *
     * @param seataTarget
     * @return boolean
     * @author xingfudeshi@gmail.com
     */
    public boolean remove(SeataTarget seataTarget) {
        return this.seataTargets.remove(seataTarget);
    }

    /**
     * find
     *
     * @param targetType
     * @param targetName
     * @return io.seata.spring.schema.SeataTarget
     * @author xingfudeshi@gmail.com
     */
    public SeataTarget find(SeataTargetType targetType, String targetName) {
        return this.seataTargets
            .stream()
            .filter(seataTarget -> seataTarget.getTargetType().equals(targetType) && seataTarget.getTargetName().equals(targetName))
            .findAny().orElse(null);
    }

    /**
     * try find
     *
     * @param clasz
     * @param method
     * @return io.seata.spring.schema.SeataTarget
     * @author xingfudeshi@gmail.com
     */
    public SeataTarget tryFind(Class<?> clasz, Method method) {
        SeataTarget seataTarget = null;
        if (clasz != null) {
            seataTarget = SeataTargetHolder.INSTANCE.find(SeataTargetType.CLASS, clasz.getName());
        }
        if (method != null && seataTarget == null) {
            seataTarget = SeataTargetHolder.INSTANCE.find(SeataTargetType.METHOD, method.getName());
        }
        return seataTarget;
    }


}
