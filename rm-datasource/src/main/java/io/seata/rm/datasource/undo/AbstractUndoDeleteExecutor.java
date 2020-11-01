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
package io.seata.rm.datasource.undo;

import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.util.JdbcConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * @author chd
 */
public abstract class AbstractUndoDeleteExecutor extends AbstractUndoExecutor {

    private static final int INSERT_BATCH_NUM = 500;

    /**
     * Instantiates a new Abstract undo executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public AbstractUndoDeleteExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    public void executeOn(Connection conn) throws SQLException {
        if (IS_UNDO_DATA_VALIDATION_ENABLE && !dataValidationAndGoOn(conn)) {
            return;
        }

        try {
            TableRecords undoRows = getUndoRows();

            List<List<Row>> rowsDouble = CollectionUtils.cutData(undoRows.getRows(), INSERT_BATCH_NUM);
            PreparedStatement undoPstCache = null;
            for (List<Row> rows : rowsDouble) {
                PreparedStatement undoPst = null;
                if (null != undoPstCache && rows.size() == INSERT_BATCH_NUM) {
                    undoPst = undoPstCache;
                } else {
                    undoPst = conn.prepareStatement(generateInsertSql(rows));
                    undoPstCache = undoPst;
                }

                int undoIndex = 0;
                for (Row undoRow : rows) {
                    for (Field field : undoRow.nonPrimaryKeys()) {
                        undoPst.setObject(++ undoIndex, field.getValue(), field.getType());
                    }
                    for (Field field : getOrderedPkList(getUndoRows(), undoRow, JdbcConstants.MYSQL)) {
                        undoPst.setObject(++ undoIndex, field.getValue(), field.getType());
                    }
                }
                undoPst.executeUpdate();
            }

        } catch (Exception ex) {
            if (ex instanceof SQLException) {
                throw (SQLException) ex;
            } else {
                throw new SQLException(ex);
            }
        }
    }

    /**
     * Undo delete.
     *
     * Notice: PK is at last one.
     * @see AbstractUndoExecutor#undoPrepare
     *
     * @return sql
     */
    protected abstract String generateInsertSql(List<Row> rows);


    @Override
    protected TableRecords getUndoRows() {
        return sqlUndoLog.getBeforeImage();
    }
}
