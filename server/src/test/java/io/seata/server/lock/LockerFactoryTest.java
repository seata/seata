package io.seata.server.lock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Chen Yang/CL10060-N/chen.yang@linecorp.com
 */
class LockerFactoryTest {

    /**
     * When using @GlobalLock, there isn't a branch session, so LockerFactory should get Locker without branch session.
     */
    @Test
    void getLockerWithoutBranchSession() {
        LockerFactory.get(null);
    }
}