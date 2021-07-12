package io.seata.spring.kt

import io.seata.core.context.RootContext
import io.seata.core.exception.TransactionException
import io.seata.core.model.GlobalStatus
import io.seata.core.model.TransactionManager
import io.seata.spring.annotation.GlobalTransactional
import io.seata.spring.kt.support.transactionScope
import io.seata.tm.TransactionManagerHolder
import io.seata.tm.api.GlobalTransactionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * @author sustly
 */
class TransactionScopeTest {

    companion object {
        private val DEFAULT_XID = "1234567890"
    }

    /**
     * Init.
     */
    init {
        TransactionManagerHolder.set(object : TransactionManager {
            @Throws(TransactionException::class)
            override fun begin(
                applicationId: String,
                transactionServiceGroup: String,
                name: String,
                timeout: Int
            ): String {
                return DEFAULT_XID
            }

            @Throws(TransactionException::class)
            override fun commit(xid: String): GlobalStatus {
                return GlobalStatus.Committed
            }

            @Throws(TransactionException::class)
            override fun rollback(xid: String): GlobalStatus {
                return GlobalStatus.Rollbacked
            }

            @Throws(TransactionException::class)
            override fun getStatus(xid: String): GlobalStatus {
                return GlobalStatus.Begin
            }

            @Throws(TransactionException::class)
            override fun globalReport(xid: String, globalStatus: GlobalStatus): GlobalStatus {
                return globalStatus
            }
        })
    }


    @Test
    @Throws(NoSuchMethodException::class)
    fun testGlobalTransactional() {
        RootContext.bind(DEFAULT_XID)
        val globalTransactionContext = GlobalTransactionContext.getCurrentOrCreate()
        globalTransactionContext.begin()
        println(RootContext.getXID())
        val mockClassAnnotation = MockMethodAnnotation()
        val xid = runBlocking {
            mockClassAnnotation.doBiz()
        }
        Assertions.assertNotNull(xid)
    }

    private open class MockMethodAnnotation {
        /**
         * use io coroutine
         */
        @GlobalTransactional(name = "doBiz")
        suspend fun doBiz(): String? = io {
            return@io RootContext.getXID()
        }

        suspend fun <T> io(block: suspend CoroutineScope.() -> T): T {
            return withContext(Dispatchers.IO, block)
        }
    }

    @Test
    @Throws(NoSuchMethodException::class)
    fun testTransactionalScope() {
        RootContext.bind(DEFAULT_XID)
        val globalTransactionContext = GlobalTransactionContext.getCurrentOrCreate()
        globalTransactionContext.begin()
        println(RootContext.getXID())
        val mockMethodScope = MockMethodScope()
        val xid = runBlocking {
            transactionScope {
                mockMethodScope.doBiz()
            }
        }
        Assertions.assertNotNull(xid)
    }

    private open class MockMethodScope {
        /**
         * use io coroutine
         */
        suspend fun doBiz(): String? = io {
            return@io RootContext.getXID()
        }

        suspend fun <T> io(block: suspend CoroutineScope.() -> T): T {
            return withContext(Dispatchers.IO, block)
        }
    }
}