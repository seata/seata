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
package io.seata.core.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * A unit test for {@link DurationUtils}
 *
 * @author wang.liang
 */
public class DurationUtilsTest {

    @Test
    public void testToDuration() {
        Duration duration = DurationUtils.toDuration("1");
        Assertions.assertEquals(duration.toMillis(), 1);

        duration = DurationUtils.toDuration("2s");
        Assertions.assertEquals(duration.toMillis(), 2 * 1000);

        duration = DurationUtils.toDuration("3m");
        Assertions.assertEquals(duration.toMillis(), 3 * 60 * 1000);

        duration = DurationUtils.toDuration("4h");
        Assertions.assertEquals(duration.toMillis(), 4 * 60 * 60 * 1000);

        duration = DurationUtils.toDuration("5d");
        Assertions.assertEquals(duration.toMillis(), 5 * 24 * 60 * 60 * 1000);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DurationUtils.toDuration(null);
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DurationUtils.toDuration("   ");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DurationUtils.toDuration("1x");
        });
    }

    @Test
    public void testMillisToString() {
        Assertions.assertEquals(DurationUtils.millisToString(1L), "1");
        Assertions.assertEquals(DurationUtils.millisToString(22L * 1000), "22s");
        Assertions.assertEquals(DurationUtils.millisToString(333L * 60 * 1000), "333m");
        Assertions.assertEquals(DurationUtils.millisToString(4444L * 60 * 60 * 1000), "4444h");
        Assertions.assertEquals(DurationUtils.millisToString(55555L * 24 * 60 * 60 * 1000), "55555d");
    }

    @Test
    public void testSecondsToString() {
        Assertions.assertEquals(DurationUtils.secondsToString(22), "22s");
        Assertions.assertEquals(DurationUtils.secondsToString(333L * 60), "333m");
        Assertions.assertEquals(DurationUtils.secondsToString(4444L * 60 * 60), "4444h");
        Assertions.assertEquals(DurationUtils.secondsToString(55555L * 24 * 60 * 60), "55555d");
    }

    @Test
    public void testMinutesToString() {
        Assertions.assertEquals(DurationUtils.minutesToString(333), "333m");
        Assertions.assertEquals(DurationUtils.minutesToString(4444L * 60), "4444h");
        Assertions.assertEquals(DurationUtils.minutesToString(55555L * 24 * 60), "55555d");
    }

    @Test
    public void testHoursToString() {
        Assertions.assertEquals(DurationUtils.hoursToString(4444), "4444h");
        Assertions.assertEquals(DurationUtils.hoursToString(55555L * 24), "55555d");
    }

    @Test
    public void testDaysToString() {
        Assertions.assertEquals(DurationUtils.daysToString(55555), "55555d");
    }
}
