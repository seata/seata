package io.seata.spring.kt.support

import io.seata.core.context.RootContext
import io.seata.tm.api.GlobalTransactionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * seata support kotlin coroutine
 *
 * if you use kotlin coroutine, GlobalTransactional can not help you, so you can use:
 * 
 * suspend fun test() = transactionScope {
 *  service1.XXX()
 *  service2.XXX()
 * }
 * 
 * @author sustly
 */
suspend fun <T> transactionScope(block: suspend CoroutineScope.() -> T): T {
    return if (coroutineContext[TransactionCoroutineContext] != null) {
        coroutineScope(block)
    } else {
        val globalTransactionContext = GlobalTransactionContext.getCurrentOrCreate()
        try {
            globalTransactionContext.begin()
            withContext(TransactionCoroutineContext()) {
                block()
            }.also {
                globalTransactionContext.commit()
                RootContext.unbind()
            }
        } catch (e: Exception) {
            globalTransactionContext.rollback()
            RootContext.unbind()
            throw e
        }
    }
}
