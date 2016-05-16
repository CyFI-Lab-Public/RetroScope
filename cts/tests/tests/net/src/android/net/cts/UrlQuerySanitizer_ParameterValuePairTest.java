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

package android.net.cts;

import android.net.UrlQuerySanitizer;
import android.net.UrlQuerySanitizer.ParameterValuePair;
import android.test.AndroidTestCase;

public class UrlQuerySanitizer_ParameterValuePairTest extends AndroidTestCase {
    public void testConstructor() {
        final String parameter = "name";
        final String vaule = "Joe_user";

        UrlQuerySanitizer uqs = new UrlQuerySanitizer();
        ParameterValuePair parameterValuePair = uqs.new ParameterValuePair(parameter, vaule);
        assertEquals(parameter, parameterValuePair.mParameter);
        assertEquals(vaule, parameterValuePair.mValue);
    }
}
