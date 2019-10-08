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
package io.seata.rm.saga.remoting;

/**
 * remoting bean info
 *
 * @author zhangsen
 */
public class RemotingDesc {

    /**
     * is referenc bean ?
     */
    private boolean isReference = false;

    /**
     * rpc target bean, the service bean has this property
     */
    private Object targetBean;

    /**
     * the saga interface tyep
     */
    private Class<?> interfaceClass;

    /**
     * interface class name
     */
    private String interfaceClassName;

    /**
     * rpc uniqueId: hsf、dubbo's version、sofa-rpc's uniqueId
     */
    private String uniqueId;

    /**
     * dubbo/hsf 's group
     */
    private String group;

    /**
     * protocol: sofa-rpc、dubbo、injvm etc.
     */
    private short protocol;

    /**
     * Gets target bean.
     *
     * @return the target bean
     */
    public Object getTargetBean() {
        return targetBean;
    }

    /**
     * Sets target bean.
     *
     * @param targetBean the target bean
     */
    public void setTargetBean(Object targetBean) {
        this.targetBean = targetBean;
    }

    /**
     * Gets interface class.
     *
     * @return the interface class
     */
    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

    /**
     * Sets interface class.
     *
     * @param interfaceClass the interface class
     */
    public void setInterfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    /**
     * Gets interface class name.
     *
     * @return the interface class name
     */
    public String getInterfaceClassName() {
        return interfaceClassName;
    }

    /**
     * Sets interface class name.
     *
     * @param interfaceClassName the interface class name
     */
    public void setInterfaceClassName(String interfaceClassName) {
        this.interfaceClassName = interfaceClassName;
    }

    /**
     * Gets unique id.
     *
     * @return the unique id
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Sets unique id.
     *
     * @param uniqueId the unique id
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * Gets group.
     *
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * Sets group.
     *
     * @param group the group
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Gets protocol.
     *
     * @return the protocol
     */
    public short getProtocol() {
        return protocol;
    }

    /**
     * Sets protocol.
     *
     * @param protocol the protocol
     */
    public void setProtocol(short protocol) {
        this.protocol = protocol;
    }

    /**
     * Is reference boolean.
     *
     * @return the boolean
     */
    public boolean isReference() {
        return isReference;
    }

    /**
     * Sets reference.
     *
     * @param reference the reference
     */
    public void setReference(boolean reference) {
        isReference = reference;
    }
}
