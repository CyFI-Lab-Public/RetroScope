/*
 * Copyright (C) 2009 The Android Open Source Project
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

// Need to be in this package to access package methods.
package com.android.phone;
import android.content.Context;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;
import com.android.internal.telephony.CallerInfo;
import com.android.phone.PhoneUtils;
import static com.android.internal.telephony.PhoneConstants.PRESENTATION_ALLOWED;
import static com.android.internal.telephony.PhoneConstants.PRESENTATION_PAYPHONE;
import static com.android.internal.telephony.PhoneConstants.PRESENTATION_RESTRICTED;
import static com.android.internal.telephony.PhoneConstants.PRESENTATION_UNKNOWN;

// Test suite for the Caller Name Presentation (CNAP) handling.
// See AndroidManifest.xml how to run these tests.
public class CnapTest extends AndroidTestCase {
    private static final String TAG = "CnapTest";
    private Context mContext;
    private CallerInfo mCallerInfo;
    // TODO: This string should be loaded from the phone package and
    // not hardcoded.
    private String mUnknown = "Unknown";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mCallerInfo = new CallerInfo();
    }

    // Checks the cnap 'ABSENT NUMBER' is mapped to the unknown presentation.
    @SmallTest
    public void testAbsentNumberIsMappedToUnknown() throws Exception {
        String num = modifyForSpecialCnapCases("ABSENT NUMBER", PRESENTATION_ALLOWED);
        assertIsUnknown(num);
    }

    // HELPERS

    /**
     * Checks the number and CallerInfo structure indicate the number
     * is unknown.
     */
    private void assertIsUnknown(String number) {
        assertEquals(mUnknown, number);
        assertEquals(PRESENTATION_UNKNOWN, mCallerInfo.numberPresentation);
        // TODO: cnapName and name presentation should be set to
        // unknown. At least I cannot see why it shouldn't be the case
        // assertEquals(mUnknown, mCallerInfo.cnapName);
        // assertEquals(PRESENTATION_UNKNOWN, mCallerInfo.namePresentation);
    }

    /**
     * Shorthand for PhoneUtils.modifyForSpecialCnapCases(mContext, mCallerInfo, ...)
     */
    private String modifyForSpecialCnapCases(String number, int presentation) {
        return PhoneUtils.modifyForSpecialCnapCases(
            mContext, mCallerInfo, number, presentation);
    }
}
