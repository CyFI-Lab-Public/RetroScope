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
package com.android.mail;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

@SmallTest
public class EmailAddressTest extends AndroidTestCase {

    public void testNameRegex() {
        {
            EmailAddress email = EmailAddress.getEmailAddress("email@gmail.com");
            assertEquals("", email.getName());
        }

        {
            EmailAddress nameKnown = EmailAddress.getEmailAddress("john doe <coolguy@doe.com>");
            assertEquals("john doe", nameKnown.getName());
        }

        {
            EmailAddress withQuotes = EmailAddress
                    .getEmailAddress("\"john doe\" <coolguy@doe.com>");
            assertEquals("john doe", withQuotes.getName());
        }

        {
            EmailAddress noSpace = EmailAddress.getEmailAddress("john doe<coolguy@doe.com>");
            assertEquals("john doe", noSpace.getName());
        }

        {
            EmailAddress noSpaceWithQuotes = EmailAddress
                    .getEmailAddress("\"john doe\"<coolguy@doe.com>");
            assertEquals("john doe", noSpaceWithQuotes.getName());
        }

    }

    /**
     * Test the parsing of email addresses
     */
    public void testEmailAddressParsing() {
        EmailAddress address = EmailAddress.getEmailAddress("test name <test@localhost.com>");
        assertEquals("test name", address.getName());
        assertEquals("test@localhost.com", address.getAddress());

        address = EmailAddress.getEmailAddress("\"test name\" <test@localhost.com>");
        assertEquals("test name", address.getName());
        assertEquals("test@localhost.com", address.getAddress());

        address = EmailAddress.getEmailAddress("<test@localhost.com>");
        assertEquals("", address.getName());
        assertEquals("test@localhost.com", address.getAddress());

        address = EmailAddress.getEmailAddress("test@localhost.com");
        assertEquals("", address.getName());
        assertEquals("test@localhost.com", address.getAddress());

        address = EmailAddress.getEmailAddress("O'brian <test@localhost.com>");
        assertEquals("O'brian", address.getName());
        assertEquals("test@localhost.com", address.getAddress());

        address = EmailAddress.getEmailAddress("\"O'brian\" <test@localhost.com>");
        assertEquals("O'brian", address.getName());
        assertEquals("test@localhost.com", address.getAddress());

        address = EmailAddress.getEmailAddress("\"\\\"O'brian\\\"\" <test@localhost.com>");
        assertEquals("\"O'brian\"", address.getName());
        assertEquals("test@localhost.com", address.getAddress());


        // Ensure that white space is trimmed from the name

        // Strings that will match the regular expression
        address = EmailAddress.getEmailAddress("\" \" <test@localhost.com>");
        assertEquals("", address.getName());

        address = EmailAddress.getEmailAddress("\" test name \" <test@localhost.com>");
        assertEquals("test name", address.getName());

        // Strings that will fallthrough to the rfc822 tokenizer
        address = EmailAddress.getEmailAddress("\"\\\" O'brian \\\"\" <test@localhost.com>");
        assertEquals("\" O'brian \"", address.getName());

        address = EmailAddress.getEmailAddress("\" \\\"O'brian\\\" \" <test@localhost.com>");
        assertEquals("\"O'brian\"", address.getName());
    }
}
