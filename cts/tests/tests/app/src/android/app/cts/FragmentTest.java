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
 * limitations under the License.
 */

package android.app.cts;

import android.app.Fragment;
import android.content.Context;
import android.test.AndroidTestCase;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;
import dalvik.annotation.ToBeFixed;

@TestTargetClass(Fragment.class)
public class FragmentTest extends AndroidTestCase {

    public static class TestFragment extends Fragment {
        public TestFragment() {}
    }

    public static class TestNotFragment {
        public TestNotFragment() {
            throw new IllegalStateException("Shouldn't call constructor");
        }
    }

    public void testInstantiateFragment() {
        assertNotNull(Fragment.instantiate(getContext(), TestFragment.class.getName()));
    }

    public void testInstantiateNonFragment() {
        try {
            Fragment.instantiate(getContext(), TestNotFragment.class.getName());
            fail();
        } catch (Exception e) {
            // Should get an exception and it shouldn't be an IllegalStateException
            assertFalse(e instanceof IllegalStateException);
        }
    }
}

