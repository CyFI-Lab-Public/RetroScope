/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.calendarcommon2.cts;

import android.test.InstrumentationCtsTestRunner;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import com.android.calendarcommon2.RecurrenceSet;
import com.android.calendarcommon2.ICalendar;

import java.util.List;

public class Calendarcommon2Test extends InstrumentationTestCase {

    /**
     * Test to ensure that com.android.calendarcommon2 is not compiled and
     * included in the BOOTCLASSPATH. If it is, apps that include (via static
     * linking) a copy of com.android.calendarcommon2 will be using the copy in
     * BOOTCLASSPATH instead of the copy that is statically linked.
     */
    @MediumTest
    public void testStaticLinking() {
        RecurrenceSet recurSet = new RecurrenceSet(null, null, null, null);
        ICalendar.Component component = new ICalendar.Component("CTS", null);
        List<ICalendar.Property> properties =
            component.getProperties(RecurrenceSet.CTS_PROPERTY_NAME);
        assertTrue(properties == null);

        recurSet.addPropertiesForRuleStr(component, null, null);
            // The above method should be calling the stub RecurrenceSet built with
            // this test apk which has the hardcoded behavior of adding a property
            // with the name CTS_PROPERTY_NAME. The lines below test for that
            // behavior.
        properties = component.getProperties(RecurrenceSet.CTS_PROPERTY_NAME);
        assertTrue(properties.size() == 1);
    }
}
