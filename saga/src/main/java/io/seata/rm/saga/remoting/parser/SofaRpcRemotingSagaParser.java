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
import io.seata.rm.saga.remoting.Protocols;
import io.seata.rm.saga.remoting.RemotingDesc;

/**
 * sofa-rpc remoting bean parsing
 *
 * @author zhangsen
 */
public class SofaRpcRemotingSagaParser extends AbstractedRemotingParser {

    /**
     * is reference bean ?
     *
     * @param bean
     * @param beanName
     * @return
     * @throws FrameworkException
     */
    @Override
    public boolean isReference(Object bean, String beanName)
        throws FrameworkException {
        String beanClassName = bean.getClass().getName();
        return "com.alipay.sofa.runtime.spring.factory.ReferenceFactoryBean".equals(beanClassName);
    }

    /**
     * is service bean ?
     *
     * @param bean
     * @param beanName
     * @return
     * @throws FrameworkException
     */
    @Override
    public boolean isService(Object bean, String beanName) throws FrameworkException {
        String beanClassName = bean.getClass().getName();
        return "com.alipay.sofa.runtime.spring.factory.ServiceFactoryBean".equals(beanClassName);
    }

    @Override
    public RemotingDesc getServiceDesc(Object bean, String beanName) throws FrameworkException {
        if (!this.isRemoting(bean, beanName)) {
            return null;
        }
        try {
            RemotingDesc serviceBeanDesc = new RemotingDesc();
            Class<?> interfaceClass = (Class<?>)ReflectionUtil.invokeMethod(bean, "getInterfaceClass");
            String interfaceClassName = (String)ReflectionUtil.getFieldValue(bean, "interfaceType");
            String uniqueId = (String)ReflectionUtil.getFieldValue(bean, "uniqueId");
            serviceBeanDesc.setInterfaceClass(interfaceClass);
            serviceBeanDesc.setInterfaceClassName(interfaceClassName);
            serviceBeanDesc.setUniqueId(uniqueId);
            serviceBeanDesc.setProtocol(Protocols.SOFA_RPC);
            if (isService(bean, beanName)) {
                Object targetBean = ReflectionUtil.getFieldValue(bean, "ref");
                serviceBeanDesc.setTargetBean(targetBean);
            }
            return serviceBeanDesc;
        } catch (Throwable t) {
            throw new FrameworkException(t);
        }
    }

    @Override
    public short getProtocol() {
        return Protocols.SOFA_RPC;
    }
}
