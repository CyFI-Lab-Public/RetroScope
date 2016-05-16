/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.common;

import android.test.suitebuilder.annotation.SmallTest;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class Rfc822ValidatorTest extends TestCase {

    @SmallTest
    public void testEmailValidator() {
        Rfc822Validator validator = new Rfc822Validator("gmail.com");
        String[] validEmails = new String[] {
            "a@b.com", "a@b.fr", "a+b@c.com", "a@b.info", "john@example.com", "john@example.fr",
            "john@corp.example.com",
        };

        for (String email : validEmails) {
            assertTrue(email + " should be a valid email address", validator.isValid(email));
        }

        String[] invalidEmails = new String[] {
            "a", "a@b", "a b", "a@b.12", "john@example..com", "johnexample.com", "john.example.com"
        };

        for (String email : invalidEmails) {
            assertFalse(email + " should not be a valid email address", validator.isValid(email));
        }

        Map<String, String> fixes = new HashMap<String, String>();
        fixes.put("a", "<a@gmail.com>");
        fixes.put("a b", "<ab@gmail.com>");
        fixes.put("a@b", "<a@b>");
        fixes.put("()~><@not.work", "");

        for (Map.Entry<String, String> e : fixes.entrySet()) {
            assertEquals(e.getValue(), validator.fixText(e.getKey()).toString());
        }
    }

    @SmallTest
    public void testEmailValidatorNullDomain() {
        Rfc822Validator validator = new Rfc822Validator(null);

        Map<String, String> fixes = new HashMap<String, String>();
        fixes.put("a", "<a>");
        fixes.put("a b", "<a b>");
        fixes.put("a@b", "<a@b>");
        fixes.put("a@b.com", "<a@b.com>"); // this one is correct

        for (Map.Entry<String, String> e : fixes.entrySet()) {
            assertEquals(e.getValue(), validator.fixText(e.getKey()).toString());
        }
    }

    @SmallTest
    public void testEmailValidatorRemoveInvalid() {
        Rfc822Validator validator = new Rfc822Validator("google.com");
        validator.setRemoveInvalid(true);

        Map<String, String> fixes = new HashMap<String, String>();
        fixes.put("a", "");
        fixes.put("a b", "");
        fixes.put("a@b", "");
        fixes.put("a@b.com", "<a@b.com>"); // this one is correct

        for (Map.Entry<String, String> e : fixes.entrySet()) {
            assertEquals(e.getValue(), validator.fixText(e.getKey()).toString());
        }
    }
}
