/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ide.eclipse.adt.internal.refactorings.core;

import com.android.resources.ResourceType;
import com.android.utils.Pair;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class RenameResourceXmlTextActionTest extends TestCase {
    public void test_Simple() throws Exception {
        checkWord("^foo", null);
        checkWord("'foo'^", null);
        checkWord("^@bogus", null);
        checkWord("@bo^gus", null);
        checkWord("bogus@^", null);
        checkWord("  @string/nam^e ", Pair.of(ResourceType.STRING, "name"));
        checkWord("@string/nam^e ", Pair.of(ResourceType.STRING, "name"));
        checkWord("\"^@string/name ", Pair.of(ResourceType.STRING, "name"));
        checkWord("^@string/name ", Pair.of(ResourceType.STRING, "name"));
        checkWord("\n^@string/name ", Pair.of(ResourceType.STRING, "name"));
        checkWord("\n^@string/name(", Pair.of(ResourceType.STRING, "name"));
        checkWord("\n^@string/name;", Pair.of(ResourceType.STRING, "name"));
        checkWord("\n^@string/name5", Pair.of(ResourceType.STRING, "name5"));
        checkWord("\n@string/name5^", Pair.of(ResourceType.STRING, "name5"));
        checkWord("\n@string/name5^(", Pair.of(ResourceType.STRING, "name5"));
        checkWord("\n@stri^ng/name5(", Pair.of(ResourceType.STRING, "name5"));
        checkWord("\n@string^/name5(", Pair.of(ResourceType.STRING, "name5"));
        checkWord("\n@string/^name5(", Pair.of(ResourceType.STRING, "name5"));
        checkWord("\n@string^name5(", null);
        checkWord("\n@strings^/name5(", null);
        checkWord("\n@+id/^myid(", Pair.of(ResourceType.ID, "myid"));
        checkWord("\n?a^ttr/foo\"", Pair.of(ResourceType.ATTR, "foo"));
        checkWord("\n?f^oo\"", Pair.of(ResourceType.ATTR, "foo"));
        checkWord("\n^?foo\"", Pair.of(ResourceType.ATTR, "foo"));
    }

    public void testClassNames() throws Exception {
        checkClassName("^foo", null);
        checkClassName("<^foo>", null);
        checkClassName("'foo.bar.Baz'^", null);
        checkClassName("<^foo.bar.Baz ", "foo.bar.Baz");
        checkClassName("<^foo.bar.Baz>", "foo.bar.Baz");
        checkClassName("<foo.^bar.Baz>", "foo.bar.Baz");
        checkClassName("<foo.bar.Baz^>", "foo.bar.Baz");
        checkClassName("<foo.bar.Baz^ >", "foo.bar.Baz");
        checkClassName("<foo.bar$Baz^ >", "foo.bar.Baz");
        checkClassName("</^foo.bar.Baz>", "foo.bar.Baz");
        checkClassName("</foo.^bar.Baz>", "foo.bar.Baz");

        checkClassName("\"^foo.bar.Baz\"", "foo.bar.Baz");
        checkClassName("\"foo.^bar.Baz\"", "foo.bar.Baz");
        checkClassName("\"foo.bar.Baz^\"", "foo.bar.Baz");
        checkClassName("\"foo.bar$Baz^\"", "foo.bar.Baz");

        checkClassName("<foo.^bar@Baz>", null);
    }

    private void checkClassName(String contents, String expectedClassName)
            throws Exception {
        int cursor = contents.indexOf('^');
        assertTrue("Must set cursor position with ^ in " + contents, cursor != -1);
        contents = contents.substring(0, cursor) + contents.substring(cursor + 1);
        assertEquals(-1, contents.indexOf('^'));
        assertEquals(-1, contents.indexOf('['));
        assertEquals(-1, contents.indexOf(']'));

        IDocument document = new Document();
        document.replace(0, 0, contents);
        String className =
                RenameResourceXmlTextAction.findClassName(document, null, cursor);
        assertEquals(expectedClassName, className);
    }

    private void checkWord(String contents, Pair<ResourceType, String> expectedResource)
            throws Exception {
        int cursor = contents.indexOf('^');
        assertTrue("Must set cursor position with ^ in " + contents, cursor != -1);
        contents = contents.substring(0, cursor) + contents.substring(cursor + 1);
        assertEquals(-1, contents.indexOf('^'));
        assertEquals(-1, contents.indexOf('['));
        assertEquals(-1, contents.indexOf(']'));

        IDocument document = new Document();
        document.replace(0, 0, contents);
        Pair<ResourceType, String> resource =
                RenameResourceXmlTextAction.findResource(document, cursor);
        assertEquals(expectedResource, resource);
    }
}
