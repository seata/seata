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
package io.seata.rm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.exception.FrameworkException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.CommitType;
import io.seata.core.model.Resource;
import io.seata.core.model.ResourceManager;

/**
 * default resource manager, adapt all resource managers
 *
 * @author zhangsen
 */
public class DefaultResourceManager implements ResourceManager {

    /**
     * all resource managers
     */
    protected static Map<BranchType, ResourceManager> resourceManagers
        = new ConcurrentHashMap<>();

    private DefaultResourceManager() {
        initResourceManagers();
    }

    /**
     * Get resource manager.
     *
     * @return the resource manager
     */
    public static DefaultResourceManager get() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * only for mock
     *
     * @param branchType  branchType
     * @param rm resource manager
     */
    public static void mockResourceManager(BranchType branchType, ResourceManager rm) {
        resourceManagers.put(branchType, rm);
    }

    protected void initResourceManagers() {
        //init all resource managers
        List<ResourceManager> allResourceManagers = EnhancedServiceLoader.loadAll(ResourceManager.class);
        if (CollectionUtils.isNotEmpty(allResourceManagers)) {
            for (ResourceManager rm : allResourceManagers) {
                resourceManagers.put(rm.getBranchType(), rm);
            }
        }
    }

    @Override
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId,
                                     String resourceId, String applicationData)
        throws TransactionException {
        return getResourceManager(branchType).branchCommit(branchType, xid, branchId, resourceId, applicationData);
    }

    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId,
                                       String resourceId, String applicationData)
        throws TransactionException {
        return getResourceManager(branchType).branchRollback(branchType, xid, branchId, resourceId, applicationData);
    }

    @Override
    public Long branchRegister(BranchType branchType, CommitType commitType, String resourceId,
                               String clientId, String xid, String applicationData, String lockKeys)
        throws TransactionException {
        return getResourceManager(branchType).branchRegister(branchType, commitType, resourceId, clientId, xid, applicationData,
            lockKeys);
    }

    @Override
    public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status,
                             String applicationData) throws TransactionException {
        getResourceManager(branchType).branchReport(branchType, xid, branchId, status, applicationData);
    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId,
                             String xid, String lockKeys) throws TransactionException {
        return getResourceManager(branchType).lockQuery(branchType, resourceId, xid, lockKeys);
    }

    @Override
    public void registerResource(Resource resource) {
        getResourceManager(resource.getBranchType()).registerResource(resource);
    }

    @Override
    public void unregisterResource(Resource resource) {
        getResourceManager(resource.getBranchType()).unregisterResource(resource);
    }

    @Override
    public Map<String, Resource> getManagedResources() {
        Map<String, Resource> allResource = new HashMap<>();
        for (ResourceManager rm : resourceManagers.values()) {
            Map<String, Resource> tempResources = rm.getManagedResources();
            if (tempResources != null) {
                allResource.putAll(tempResources);
            }
        }
        return allResource;
    }

    /**
     * get ResourceManager by Resource Type
     *
     * @param branchType branch type
     * @return resource manager
     */
    public ResourceManager getResourceManager(BranchType branchType) {
        ResourceManager rm = resourceManagers.get(branchType);
        if (rm == null) {
            throw new FrameworkException("No ResourceManager for BranchType:" + branchType.name());
        }
        return rm;
    }

    @Override
    public BranchType getBranchType() {
        throw new FrameworkException("DefaultResourceManager isn't a real ResourceManager");
    }

    private static class SingletonHolder {
        private static DefaultResourceManager INSTANCE = new DefaultResourceManager();
    }

}
