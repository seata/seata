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
package io.seata.server.storage.mongo.session;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.alibaba.fastjson.JSON;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.QueryOperators;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import io.seata.common.exception.StoreException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;
import io.seata.server.storage.mongo.MongoPooledFactory;
import io.seata.server.store.AbstractTransactionStoreManager;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author funkye
 */
public class MongoTransactionStoreManager extends AbstractTransactionStoreManager implements TransactionStoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoTransactionStoreManager.class);

    private static volatile MongoTransactionStoreManager instance;

    /**
     * Get the instance.
     */
    public static MongoTransactionStoreManager getInstance() {
        if (null == instance) {
            synchronized (MongoTransactionStoreManager.class) {
                if (null == instance) {
                    instance = new MongoTransactionStoreManager();
                }
            }
        }
        return instance;
    }

    @Override
    public boolean writeSession(LogOperation logOperation, SessionStorable session) {
        if (LogOperation.GLOBAL_ADD.equals(logOperation)) {
            return insertGlobalTransactionDO(convertGlobalTransactionDO(session));
        } else if (LogOperation.GLOBAL_UPDATE.equals(logOperation)) {
            return updateGlobalTransactionDO(convertGlobalTransactionDO(session));
        } else if (LogOperation.GLOBAL_REMOVE.equals(logOperation)) {
            return deleteGlobalTransactionDO(convertGlobalTransactionDO(session));
        } else if (LogOperation.BRANCH_ADD.equals(logOperation)) {
            return insertBranchTransactionDO(convertBranchTransactionDO(session));
        } else if (LogOperation.BRANCH_UPDATE.equals(logOperation)) {
            return updateBranchTransactionDO(convertBranchTransactionDO(session));
        } else if (LogOperation.BRANCH_REMOVE.equals(logOperation)) {
            return deleteBranchTransactionDO(convertBranchTransactionDO(session));
        } else {
            throw new StoreException("Unknown LogOperation:" + logOperation.name());
        }
    }

    private boolean deleteBranchTransactionDO(BranchTransactionDO convertBranchTransactionDO) {
        try {
            MongoCollection<Document> collection = MongoPooledFactory.getBranchCollection();
            collection.deleteOne(new Document("branch_id", convertBranchTransactionDO.getBranchId()));
            return true;
        } catch (Exception e) {
            LOGGER.error("deleteBranchTransactionDO fail : {}", e.getMessage());
            return false;
        }
    }

    private boolean insertBranchTransactionDO(BranchTransactionDO convertBranchTransactionDO) {
        try {
            MongoCollection<Document> collection = MongoPooledFactory.getBranchCollection();
            collection.insertOne(convertDocumentByBranch(convertBranchTransactionDO));
            return true;
        } catch (Exception e) {
            LOGGER.error("insertBranchTransactionDO fail : {}", e.getMessage());
            return false;
        }
    }

    private boolean updateBranchTransactionDO(BranchTransactionDO convertBranchTransactionDO) {
        try {
            MongoCollection<Document> collection = MongoPooledFactory.getBranchCollection();
            collection.updateOne(new Document("branch_id", convertBranchTransactionDO.getBranchId()),
                new Document("$set", convertDocumentByBranch(convertBranchTransactionDO)));
            return true;
        } catch (Exception e) {
            LOGGER.error("updateBranchTransactionDO fail : {}", e.getMessage());
            return false;
        }
    }

    private boolean deleteGlobalTransactionDO(GlobalTransactionDO convertGlobalTransactionDO) {
        try {
            MongoCollection<Document> collection = MongoPooledFactory.getGlobalCollection();
            collection.deleteOne(new Document("xid", convertGlobalTransactionDO.getXid()));
            return true;
        } catch (Exception e) {
            LOGGER.error("deleteGlobalTransactionDO fail : {}", e.getMessage());
            return false;
        }
    }

    private boolean insertGlobalTransactionDO(GlobalTransactionDO convertGlobalTransactionDO) {
        try {
            MongoCollection<Document> collection = MongoPooledFactory.getGlobalCollection();
            collection.insertOne(convertDocumentByGlobal(convertGlobalTransactionDO));
            return true;
        } catch (Exception e) {
            LOGGER.error("insertGlobalTransactionDO fail : {}", e.getMessage());
            return false;
        }
    }

    private boolean updateGlobalTransactionDO(GlobalTransactionDO convertGlobalTransactionDO) {
        try {
            MongoCollection<Document> collection = MongoPooledFactory.getGlobalCollection();
            collection.updateOne(new Document("xid", convertGlobalTransactionDO.getXid()),
                new Document("$set", convertDocumentByGlobal(convertGlobalTransactionDO)));
            return true;
        } catch (Exception e) {
            LOGGER.error("updateGlobalTransactionDO fail : {}", e.getMessage());
            return false;
        }
    }

    /**
     * Read session global session.
     *
     * @param xid
     *            the xid
     * @param withBranchSessions
     *            the withBranchSessions
     * @return the global session
     */
    @Override
    public GlobalSession readSession(String xid, boolean withBranchSessions) {
        try {
            MongoCollection<Document> collection = MongoPooledFactory.getGlobalCollection();
            String json = convertJsonString(collection.find(new Document("xid", xid)));
            if (StringUtils.isBlank(json)) {
                return null;
            }
            GlobalTransactionDO globalTransactionDO = JSON.parseObject(json, GlobalTransactionDO.class);
            // branch transactions
            List<BranchTransactionDO> branchTransactionDOs = null;
            if (withBranchSessions) {
                MongoCollection<Document> branchCollection = MongoPooledFactory.getBranchCollection();
                FindIterable<Document> docs = branchCollection.find(new Document("xid", xid));
                branchTransactionDOs = new ArrayList<>();
                for (Document doc : docs) {
                    branchTransactionDOs.add(JSON.parseObject((String)doc.get("do"), BranchTransactionDO.class));
                }
            }
            return getGlobalSession(globalTransactionDO, branchTransactionDOs);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Read session global session.
     *
     * @param xid
     *            the xid
     * @return the global session
     */
    @Override
    public GlobalSession readSession(String xid) {
        return this.readSession(xid, true);
    }

    /**
     * Read session list.
     *
     * @param statuses
     *            the statuses
     * @return the list
     */
    public List<GlobalSession> readSession(GlobalStatus[] statuses) {
        BasicDBList values = new BasicDBList();
        for (int i = 0; i < statuses.length; i++) {
            values.add(new BasicDBObject("status", statuses[i].getCode()));
        }
        BasicDBObject queryCondition = new BasicDBObject();
        queryCondition.put(QueryOperators.OR, values);
        try {
            MongoCollection<Document> collection = MongoPooledFactory.getGlobalCollection();
            FindIterable<Document> docs = collection.find(queryCondition);
            List<GlobalTransactionDO> globalTransactionDOs = new ArrayList<>();
            for (Document doc : docs) {
                globalTransactionDOs.add(JSON.parseObject((String)doc.get("do"), GlobalTransactionDO.class));
            }
            if (globalTransactionDOs.size() > 0) {
                Set<String> xids =
                    globalTransactionDOs.stream().map(GlobalTransactionDO::getXid).collect(Collectors.toSet());
                List<BranchTransactionDO> branchTransactionDOs = new ArrayList<>();
                values = new BasicDBList();
                for (String xid : xids) {
                    values.add(new BasicDBObject("xid", xid));
                }
                queryCondition = new BasicDBObject();
                queryCondition.put(QueryOperators.OR, values);
                MongoCollection<Document> branchCollection = MongoPooledFactory.getBranchCollection();
                docs = branchCollection.find(queryCondition);
                for (Document doc : docs) {
                    branchTransactionDOs.add(JSON.parseObject((String)doc.get("do"), BranchTransactionDO.class));
                }
                if (branchTransactionDOs.size() > 0) {
                    Map<String, List<BranchTransactionDO>> branchTransactionDOsMap =
                        branchTransactionDOs.stream().collect(Collectors.groupingBy(BranchTransactionDO::getXid,
                            LinkedHashMap::new, Collectors.toList()));
                    return globalTransactionDOs.stream()
                        .map(globalTransactionDO -> getGlobalSession(globalTransactionDO,
                            branchTransactionDOsMap.get(globalTransactionDO.getXid())))
                        .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public List<GlobalSession> readSession(SessionCondition sessionCondition) {
        try {
            MongoCollection<Document> collection = MongoPooledFactory.getGlobalCollection();
            if (StringUtils.isNotBlank(sessionCondition.getXid())) {
                GlobalSession session = null;
                FindIterable<Document> docs = collection.find(new Document("xid", sessionCondition.getXid()));
                for (Document doc : docs) {
                    session = convertGlobalSession(JSON.parseObject((String)doc.get("do"), GlobalTransactionDO.class));
                    break;
                }
                if (null != session) {
                    List<GlobalSession> globalSessions = new ArrayList<>();
                    globalSessions.add(session);
                    return globalSessions;
                }
            } else if (sessionCondition.getTransactionId() != null) {
                FindIterable<Document> docs =
                    collection.find(new Document("transaction_id", sessionCondition.getTransactionId()));
                GlobalTransactionDO globalTransactionDO = null;
                for (Document doc : docs) {
                    globalTransactionDO = JSON.parseObject((String)doc.get("do"), GlobalTransactionDO.class);
                    break;
                }
                if (null != globalTransactionDO) {
                    List<BranchTransactionDO> branchTransactionDOs = new ArrayList<>();
                    MongoCollection<Document> branchCollection = MongoPooledFactory.getBranchCollection();
                    docs = branchCollection.find(new Document("xid", globalTransactionDO.getXid()));
                    for (Document doc : docs) {
                        branchTransactionDOs.add(JSON.parseObject((String)doc.get("do"), BranchTransactionDO.class));
                    }
                    if (null != branchTransactionDOs && branchTransactionDOs.size() > 0) {
                        GlobalSession globalSession = getGlobalSession(globalTransactionDO, branchTransactionDOs);
                        if (globalSession != null) {
                            List<GlobalSession> globalSessions = new ArrayList<>();
                            globalSessions.add(globalSession);
                            return globalSessions;
                        }
                    }
                }
            } else if (CollectionUtils.isNotEmpty(sessionCondition.getStatuses())) {
                return readSession(sessionCondition.getStatuses());
            }
        } catch (Exception e) {
        }
        return null;
    }

    private GlobalTransactionDO convertGlobalTransactionDO(SessionStorable session) {
        if (session == null || !(session instanceof GlobalSession)) {
            throw new IllegalArgumentException(
                "the parameter of SessionStorable is not available, SessionStorable:" + StringUtils.toString(session));
        }
        GlobalSession globalSession = (GlobalSession)session;

        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid(globalSession.getXid());
        globalTransactionDO.setStatus(globalSession.getStatus().getCode());
        globalTransactionDO.setApplicationId(globalSession.getApplicationId());
        globalTransactionDO.setBeginTime(globalSession.getBeginTime());
        globalTransactionDO.setTimeout(globalSession.getTimeout());
        globalTransactionDO.setTransactionId(globalSession.getTransactionId());
        globalTransactionDO.setTransactionName(globalSession.getTransactionName());
        globalTransactionDO.setTransactionServiceGroup(globalSession.getTransactionServiceGroup());
        globalTransactionDO.setApplicationData(globalSession.getApplicationData());
        return globalTransactionDO;
    }

    private BranchTransactionDO convertBranchTransactionDO(SessionStorable session) {
        if (session == null || !(session instanceof BranchSession)) {
            throw new IllegalArgumentException(
                "the parameter of SessionStorable is not available, SessionStorable:" + StringUtils.toString(session));
        }
        BranchSession branchSession = (BranchSession)session;

        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        branchTransactionDO.setXid(branchSession.getXid());
        branchTransactionDO.setBranchId(branchSession.getBranchId());
        branchTransactionDO.setBranchType(branchSession.getBranchType().name());
        branchTransactionDO.setClientId(branchSession.getClientId());
        branchTransactionDO.setResourceGroupId(branchSession.getResourceGroupId());
        branchTransactionDO.setTransactionId(branchSession.getTransactionId());
        branchTransactionDO.setApplicationData(branchSession.getApplicationData());
        branchTransactionDO.setResourceId(branchSession.getResourceId());
        branchTransactionDO.setStatus(branchSession.getStatus().getCode());
        return branchTransactionDO;
    }

    private GlobalSession convertGlobalSession(GlobalTransactionDO globalTransactionDO) {
        GlobalSession session =
            new GlobalSession(globalTransactionDO.getApplicationId(), globalTransactionDO.getTransactionServiceGroup(),
                globalTransactionDO.getTransactionName(), globalTransactionDO.getTimeout());
        session.setTransactionId(globalTransactionDO.getTransactionId());
        session.setXid(globalTransactionDO.getXid());
        session.setStatus(GlobalStatus.get(globalTransactionDO.getStatus()));
        session.setApplicationData(globalTransactionDO.getApplicationData());
        session.setBeginTime(globalTransactionDO.getBeginTime());
        return session;
    }

    private BranchSession convertBranchSession(BranchTransactionDO branchTransactionDO) {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(branchTransactionDO.getXid());
        branchSession.setTransactionId(branchTransactionDO.getTransactionId());
        branchSession.setApplicationData(branchTransactionDO.getApplicationData());
        branchSession.setBranchId(branchTransactionDO.getBranchId());
        branchSession.setBranchType(BranchType.valueOf(branchTransactionDO.getBranchType()));
        branchSession.setResourceId(branchTransactionDO.getResourceId());
        branchSession.setClientId(branchTransactionDO.getClientId());
        branchSession.setResourceGroupId(branchTransactionDO.getResourceGroupId());
        branchSession.setStatus(BranchStatus.get(branchTransactionDO.getStatus()));
        return branchSession;
    }

    private GlobalSession getGlobalSession(GlobalTransactionDO globalTransactionDO,
        List<BranchTransactionDO> branchTransactionDOs) {
        GlobalSession globalSession = convertGlobalSession(globalTransactionDO);
        // branch transactions
        if (branchTransactionDOs != null && branchTransactionDOs.size() > 0) {
            for (BranchTransactionDO branchTransactionDO : branchTransactionDOs) {
                globalSession.add(convertBranchSession(branchTransactionDO));
            }
        }
        return globalSession;
    }

    private Document convertDocumentByBranch(BranchTransactionDO branchTransactionDO) {
        String now = LocalDateTime.now().toString();
        Document doc = new Document();
        doc.put("branch_id", branchTransactionDO.getBranchId());
        doc.put("xid", branchTransactionDO.getXid());
        doc.put("transaction_id", branchTransactionDO.getTransactionId());
        doc.put("resource_group_id", branchTransactionDO.getResourceGroupId());
        doc.put("branch_type", branchTransactionDO.getBranchType());
        doc.put("status", branchTransactionDO.getStatus());
        doc.put("client_id", branchTransactionDO.getClientId());
        doc.put("application_data", branchTransactionDO.getApplicationData());
        doc.put("gmt_create", now);
        doc.put("gmt_modified", now);
        doc.put("do", JSON.toJSONString(branchTransactionDO));
        return doc;
    }

    private Document convertDocumentByGlobal(GlobalTransactionDO globalTransactionDO) {
        String now = LocalDateTime.now().toString();
        Document doc = new Document();
        doc.put("xid", globalTransactionDO.getXid());
        doc.put("transaction_id", globalTransactionDO.getTransactionId());
        doc.put("transaction_name", globalTransactionDO.getTransactionName());
        doc.put("transaction_service_group", globalTransactionDO.getTransactionServiceGroup());
        doc.put("timeout", globalTransactionDO.getTimeout());
        doc.put("begin_time", globalTransactionDO.getBeginTime());
        doc.put("status", globalTransactionDO.getStatus());
        doc.put("application_id", globalTransactionDO.getApplicationId());
        doc.put("application_data", globalTransactionDO.getApplicationData());
        doc.put("gmt_create", now);
        doc.put("gmt_modified", now);
        doc.put("do", JSON.toJSONString(globalTransactionDO));
        return doc;
    }

    private String convertJsonString(FindIterable<Document> docs) {
        for (Document doc : docs) {
            return String.valueOf(doc.get("do"));
        }
        return null;
    }

}
