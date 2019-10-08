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
package io.seata.rm.saga.remoting.parser;

import io.seata.common.exception.FrameworkException;
import io.seata.common.util.ReflectionUtil;
import io.seata.rm.saga.api.LocalSAGA;
import io.seata.rm.saga.remoting.Protocols;
import io.seata.rm.saga.remoting.RemotingDesc;

import java.util.Set;

/**
 * local saga bean parsing
 *
 * @author zhangsen
 */
public class LocalSAGARemotingParser extends AbstractedRemotingParser {

    @Override
    public boolean isReference(Object bean, String beanName) {
        Class<?> classType = bean.getClass();
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(classType);
        for (Class<?> interClass : interfaceClasses) {
            if (interClass.isAnnotationPresent(LocalSAGA.class)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isService(Object bean, String beanName) {
        Class<?> classType = bean.getClass();
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(classType);
        for (Class<?> interClass : interfaceClasses) {
            if (interClass.isAnnotationPresent(LocalSAGA.class)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public RemotingDesc getServiceDesc(Object bean, String beanName) throws FrameworkException {
        if (!this.isRemoting(bean, beanName)) {
            return null;
        }
        RemotingDesc remotingDesc = new RemotingDesc();
        remotingDesc.setReference(true);
        remotingDesc.setProtocol(Protocols.IN_JVM);
        Class<?> classType = bean.getClass();
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(classType);
        for (Class<?> interClass : interfaceClasses) {
            if (interClass.isAnnotationPresent(LocalSAGA.class)) {
                remotingDesc.setInterfaceClassName(interClass.getName());
                remotingDesc.setInterfaceClass(interClass);
                remotingDesc.setTargetBean(bean);
                return remotingDesc;
            }
        }
        throw new FrameworkException("Couldn't parser any Remoting info");
    }

    @Override
    public short getProtocol() {
        return Protocols.IN_JVM;
    }
}
