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
package io.seata.rm.datasource.undo.mysql;

import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.KeywordChecker;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.rm.datasource.undo.UndoExecutorGroup;
import io.seata.rm.datasource.undo.mysql.keyword.MySQLKeywordChecker;

/**
 *
 * @author: Zhibei Hao丶
 * @date: 2019/8/15 10:57
 * @version: V1.0
 */
public class MySQLUndoExecutorGroup implements UndoExecutorGroup {
  private final String MYSQL = "mysql";

  @Override
  public AbstractUndoExecutor getInsertExecutor(SQLUndoLog sqlUndoLog) {
    return new MySQLUndoInsertExecutor(sqlUndoLog);
  }

  @Override
  public AbstractUndoExecutor getUpdateExecutor(SQLUndoLog sqlUndoLog) {
    return new MySQLUndoUpdateExecutor(sqlUndoLog);
  }

  @Override
  public AbstractUndoExecutor getDeleteExecutor(SQLUndoLog sqlUndoLog) {
    return new MySQLUndoDeleteExecutor(sqlUndoLog);
  }


  @Override
  public String getDbType() {
    return MYSQL;
  }
}
