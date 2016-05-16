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

import android.nfc.Tag;

/** Tag tester that writes data to the tag and returns a way to confirm a successful write. */
public interface TagTester {

    /** @return true if the tag is testable by this {@link TagTester} */
    boolean isTestableTag(Tag tag);

    /** Writes some data to the tag and returns a {@link TagVerifier} to confirm it. */
    TagVerifier writeTag(Tag tag) throws Exception;
}
