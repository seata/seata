package io.seata.spring.kt.support

import io.seata.core.context.RootContext
import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * TransactionCoroutineContext
 * 
 * @author sustly
 */
class TransactionCoroutineContext(private val xid: String? = RootContext.getXID()) :
    AbstractCoroutineContextElement(TransactionCoroutineContext),
    ThreadContextElement<String?> {

    companion object : CoroutineContext.Key<TransactionCoroutineContext>

    override fun restoreThreadContext(context: CoroutineContext, oldState: String?) {
        if (oldState != xid && oldState != null) {
            RootContext.bind(oldState)
        } else {
            RootContext.unbind()
        }
    }

    override fun updateThreadContext(context: CoroutineContext): String? {
        return RootContext.getXID().apply {
            if (xid != null) RootContext.bind(xid)
        }
    }
}
