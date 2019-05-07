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
package io.seata.core.domain;

import io.seata.core.model.GlobalOperation;
import io.seata.core.model.GlobalStatus;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.UntypedStateMachineBuilder;

/**
 * State Graph Helper
 *
 * @author leizhiyuan
 */
public class StateMachineHelper {

    public static GlobalSessionStatusStateMachine buildGlobalStatusMachine(GlobalStatus globalStatus) {
        final UntypedStateMachineBuilder builder = StateMachineBuilderFactory.create(
            GlobalSessionStatusStateMachine.class);

        builder.transitions().from(GlobalStatus.UnKnown).toAmong(GlobalStatus.Begin).onEach(GlobalOperation.BEGIN);
        builder.transitions().from(GlobalStatus.Begin).toAmong(GlobalStatus.Begin).onEach(GlobalOperation.BEGIN);
        builder.transitions().from(GlobalStatus.Begin).toAmong(GlobalStatus.Committing).onEach(GlobalOperation.COMMIT);
        builder.transitions().from(GlobalStatus.Begin).toAmong(GlobalStatus.TimeoutRollbacking).onEach(
            GlobalOperation.TIMEOUT);
        builder.transitions().from(GlobalStatus.Begin).toAmong(GlobalStatus.Rollbacking).onEach(
            GlobalOperation.ROLLBACK);
        builder.transitions().from(GlobalStatus.Committing).toAmong(GlobalStatus.AsyncCommitting).onEach(
            GlobalOperation.ASYNC_COMMIT);

        builder.transitions().from(GlobalStatus.AsyncCommitting).toAmong(GlobalStatus.CommitRetrying).onEach(
            GlobalOperation.RETRY_COMMIT);
        builder.transitions().from(GlobalStatus.AsyncCommitting).toAmong(GlobalStatus.CommitFailed).onEach(
            GlobalOperation.END_COMMIT_FAIL);
        builder.transitions().from(GlobalStatus.AsyncCommitting).toAmong(GlobalStatus.Committed).onEach(
            GlobalOperation.END_COMMIT_SUCCESS);

        builder.transitions().from(GlobalStatus.Committing).toAmong(GlobalStatus.CommitRetrying).onEach(
            GlobalOperation.RETRY_COMMIT);
        builder.transitions().from(GlobalStatus.Committing).toAmong(GlobalStatus.CommitFailed).onEach(
            GlobalOperation.END_COMMIT_FAIL);
        builder.transitions().from(GlobalStatus.Committing).toAmong(GlobalStatus.Committed).onEach(
            GlobalOperation.END_COMMIT_SUCCESS);

        builder.transitions().from(GlobalStatus.CommitRetrying).toAmong(GlobalStatus.CommitRetrying).onEach(
            GlobalOperation.RETRY_COMMIT);
        builder.transitions().from(GlobalStatus.CommitRetrying).toAmong(GlobalStatus.CommitFailed).onEach(
            GlobalOperation.END_COMMIT_FAIL);
        builder.transitions().from(GlobalStatus.CommitRetrying).toAmong(GlobalStatus.Committed).onEach(
            GlobalOperation.END_COMMIT_SUCCESS);

        builder.transitions().from(GlobalStatus.Rollbacking).toAmong(
            GlobalStatus.RollbackRetrying).onEach(GlobalOperation.RETRY_ROLLBACK);
        builder.transitions().from(GlobalStatus.Rollbacking).toAmong(
            GlobalStatus.Rollbacked).onEach(GlobalOperation.END_ROLLBACK_SUCCESS);
        builder.transitions().from(GlobalStatus.Rollbacking).toAmong(
            GlobalStatus.RollbackFailed).onEach(GlobalOperation.END_ROLLBACK_FAIL);

        builder.transitions().from(GlobalStatus.RollbackRetrying).toAmong(
            GlobalStatus.RollbackRetrying).onEach(GlobalOperation.RETRY_ROLLBACK);
        builder.transitions().from(GlobalStatus.RollbackRetrying).toAmong(
            GlobalStatus.Rollbacked).onEach(GlobalOperation.END_ROLLBACK_SUCCESS);
        builder.transitions().from(GlobalStatus.RollbackRetrying).toAmong(
            GlobalStatus.RollbackFailed).onEach(GlobalOperation.END_ROLLBACK_FAIL);

        builder.transitions().from(GlobalStatus.TimeoutRollbacking).toAmong(
            GlobalStatus.RollbackRetrying).onEach(GlobalOperation.RETRY_ROLLBACK);
        builder.transitions().from(GlobalStatus.TimeoutRollbacking).toAmong(
            GlobalStatus.Rollbacked).onEach(GlobalOperation.END_ROLLBACK_SUCCESS);
        builder.transitions().from(GlobalStatus.TimeoutRollbacking).toAmong(
            GlobalStatus.RollbackFailed).onEach(GlobalOperation.END_ROLLBACK_FAIL);

        builder.transitions().from(GlobalStatus.TimeoutRollbackRetrying).toAmong(
            GlobalStatus.TimeoutRollbackRetrying).onEach(GlobalOperation.RETRY_ROLLBACK);
        builder.transitions().from(GlobalStatus.TimeoutRollbackRetrying).toAmong(GlobalStatus.TimeoutRollbacked)
            .onEach(GlobalOperation.END_ROLLBACK_SUCCESS);
        builder.transitions().from(GlobalStatus.TimeoutRollbackRetrying).toAmong(GlobalStatus.TimeoutRollbackFailed)
            .onEach(GlobalOperation.END_ROLLBACK_FAIL);

        builder.transitions().from(GlobalStatus.TimeoutRollbacked).toAmong(
            GlobalStatus.TimeoutRollbackRetrying).onEach(GlobalOperation.RETRY_ROLLBACK);
        builder.transitions().from(GlobalStatus.TimeoutRollbacked).toAmong(
            GlobalStatus.TimeoutRollbacked).onEach(GlobalOperation.END_ROLLBACK_SUCCESS);
        builder.transitions().from(GlobalStatus.TimeoutRollbacked).toAmong(GlobalStatus.TimeoutRollbackFailed).onEach(
            GlobalOperation.END_ROLLBACK_FAIL);

        builder.transitions().from(GlobalStatus.TimeoutRollbackFailed).toAmong(
            GlobalStatus.TimeoutRollbackRetrying).onEach(GlobalOperation.RETRY_ROLLBACK);
        builder.transitions().from(GlobalStatus.TimeoutRollbackFailed).toAmong(GlobalStatus.TimeoutRollbacked).onEach(
            GlobalOperation.END_ROLLBACK_SUCCESS);
        builder.transitions().from(GlobalStatus.TimeoutRollbackFailed).toAmong(GlobalStatus.TimeoutRollbackFailed)
            .onEach(GlobalOperation.END_ROLLBACK_FAIL);

        builder.transitions().from(GlobalStatus.Committed).toAmong(
            GlobalStatus.Finished).onEach(GlobalOperation.FINISH);
        builder.transitions().from(GlobalStatus.CommitFailed).toAmong(
            GlobalStatus.Finished).onEach(GlobalOperation.FINISH);
        builder.transitions().from(GlobalStatus.TimeoutRollbacked).toAmong(
            GlobalStatus.Finished).onEach(GlobalOperation.FINISH);
        builder.transitions().from(GlobalStatus.Rollbacked).toAmong(
            GlobalStatus.Finished).onEach(GlobalOperation.FINISH);
        builder.transitions().from(GlobalStatus.TimeoutRollbackFailed).toAmong(
            GlobalStatus.Finished).onEach(GlobalOperation.FINISH);
        builder.transitions().from(GlobalStatus.RollbackFailed).toAmong(
            GlobalStatus.Finished).onEach(GlobalOperation.FINISH);
        GlobalSessionStatusStateMachine machine = builder.newAnyStateMachine(globalStatus);
        machine.start();
        return machine;
    }
}
