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
package io.seata.server.storage.mongo.lock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import io.seata.common.util.CollectionUtils;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.core.store.LockDO;
import io.seata.server.storage.mongo.MongoPooledFactory;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author funkye
 */
public class MongoLocker extends AbstractLocker {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoLocker.class);

    public MongoLocker() {}

    @Override
    public boolean acquireLock(List<RowLock> rowLocks) {
        if (CollectionUtils.isEmpty(rowLocks)) {
            // no lock
            return true;
        }
        MongoCollection<Document> collection = MongoPooledFactory.getLockCollection();
        List<Document> list = convertDocumentByRowKey(convertToLockDO(rowLocks));
        try {
            String xid = rowLocks.get(0).getXid();
            for (Document document : list) {
                FindIterable<Document> documents = collection.find(document);
                for (Document doc : documents) {
                    Object orgXid = doc.get("xid");
                    if (orgXid != null && !String.valueOf(orgXid).equalsIgnoreCase(xid)) {
                        return false;
                    } else {
                        String rawLock = String.valueOf(doc.get("row_key"));
                        for (RowLock rowLock : rowLocks) {
                            if (rawLock.equals(rowLock.getRowKey())) {
                                rowLocks.remove(rowLock);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("isLockable error, locks:{}", CollectionUtils.toString(rowLocks), e);
        }
        List<Document> successList = new ArrayList<>();
        List<Document> docs = convertDocument(convertToLockDO(rowLocks));
        for (Document doc : docs) {
            try {
                collection.insertOne(doc);
                successList.add(doc);
            } catch (Exception e) {
                LOGGER.error("acquireLock fail:{}", e.getMessage());
                for (Document document : successList) {
                    collection.deleteOne(document);
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean releaseLock(List<RowLock> rowLocks) {
        MongoCollection<Document> collection = MongoPooledFactory.getLockCollection();
        List<Document> docs = convertDocumentByRowKey(convertToLockDO(rowLocks));
        try {
            for (Document document : docs) {
                collection.deleteOne(document);
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("unLock error, locks:{}", CollectionUtils.toString(rowLocks), e);
            return false;
        }
    }

    @Override
    public boolean releaseLock(String xid, Long branchId) {
        try {
            MongoCollection<Document> collection = MongoPooledFactory.getLockCollection();
            collection.deleteOne(new Document("xid", xid).append("branch_id", branchId));
            return true;
        } catch (Exception t) {
            LOGGER.error("unLock by branchId error, xid {}, branchId:{}", xid, branchId, t);
            return false;
        }
    }

    @Override
    public boolean releaseLock(String xid, List<Long> branchIds) {
        if (CollectionUtils.isEmpty(branchIds)) {
            // no lock
            return true;
        }
        try {
            MongoCollection<Document> collection = MongoPooledFactory.getLockCollection();
            List<Document> docs = convertDocumentByXid(xid, branchIds);
            for (Document document : docs) {
                collection.deleteOne(document);
            }
            return true;
        } catch (Exception t) {
            LOGGER.error("unLock by branchIds error, xid {}, branchIds:{}", xid, CollectionUtils.toString(branchIds),
                t);
            return false;
        }
    }

    @Override
    public boolean isLockable(List<RowLock> rowLocks) {
        MongoCollection<Document> collection = MongoPooledFactory.getLockCollection();
        List<Document> docs = convertDocumentByRowKey(convertToLockDO(rowLocks));
        try {
            for (Document document : docs) {
                FindIterable<Document> documents = collection.find(document);
                for (Document doc : documents) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("isLockable error, locks:{}", CollectionUtils.toString(rowLocks), e);
            return false;
        }
    }

    private List<Document> convertDocument(List<LockDO> locks) {
        String now = LocalDateTime.now().toString();
        List<Document> docs = new ArrayList<>();
        for (LockDO lock : locks) {
            Document doc = new Document();
            doc.put("row_key", lock.getRowKey());
            doc.put("transaction_id", lock.getTransactionId());
            doc.put("xid", lock.getXid());
            doc.put("pk", lock.getPk());
            doc.put("branch_id", lock.getBranchId());
            doc.put("resource_id", lock.getResourceId());
            doc.put("table_name", lock.getTableName());
            doc.put("gmt_create", now);
            doc.put("gmt_modified", now);
            docs.add(doc);
        }
        return docs;
    }

    private List<Document> convertDocumentByRowKey(List<LockDO> locks) {
        List<Document> docs = new ArrayList<>();
        for (LockDO lock : locks) {
            docs.add(convertDocumentByRowKey(lock));
        }
        return docs;
    }

    private Document convertDocumentByRowKey(LockDO lock) {
        Document doc = new Document();
        doc.put("row_key", lock.getRowKey());
        return doc;
    }

    private List<Document> convertDocumentByXid(String xid, List<Long> branchIds) {
        List<Document> docs = new ArrayList<>();
        for (Long branchId : branchIds) {
            Document doc = new Document();
            doc.put("xid", xid);
            doc.put("branch_id", branchId);
            docs.add(doc);
        }
        return docs;
    }
}
