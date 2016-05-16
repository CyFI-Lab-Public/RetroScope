/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.cts.verifier.nfc.tech;

import android.nfc.FormatException;
import android.nfc.Tag;

import java.io.IOException;

/** Tag verifier for checking that the {@link Tag} has some expected value. */
public interface TagVerifier {

    /** @return true if the tag has the expected value */
    Result verifyTag(Tag tag) throws FormatException, IOException;

    /** Class with info necessary to show the user what was written and read from a tag. */
    public static class Result {

        private final CharSequence mExpectedContent;

        private final CharSequence mActualContent;

        private final boolean mIsMatch;

        public Result(CharSequence expectedContent, CharSequence actualContent, boolean isMatch) {
            this.mExpectedContent = expectedContent;
            this.mActualContent = actualContent;
            this.mIsMatch = isMatch;
        }

        /** @return {@link CharSequence} representation of the data written to the tag */
        public CharSequence getExpectedContent() {
            return mExpectedContent;
        }

        /** @return {@link CharSequence} representation of the data read back from the tag */
        public CharSequence getActualContent() {
            return mActualContent;
        }

        /** @return whether or not the expected content matched the actual content of the tag */
        public boolean isMatch() {
            return mIsMatch;
        }
    }
}
