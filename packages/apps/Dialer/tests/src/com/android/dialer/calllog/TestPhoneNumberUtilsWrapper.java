/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.dialer.calllog;

import android.content.res.Resources;

/**
 * Modified version of {@link com.android.dialer.calllog.PhoneNumberHelper} to be used in tests
 * that allows injecting the voicemail number.
 */
public final class TestPhoneNumberUtilsWrapper extends PhoneNumberUtilsWrapper {
    private CharSequence mVoicemailNumber;

    public TestPhoneNumberUtilsWrapper(CharSequence voicemailNumber) {
        mVoicemailNumber = voicemailNumber;
    }

    @Override
    public boolean isVoicemailNumber(CharSequence number) {
        return mVoicemailNumber.equals(number);
    }
}
