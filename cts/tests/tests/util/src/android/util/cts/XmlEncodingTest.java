/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.util.cts;

import android.util.Xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Vector;

import junit.framework.TestCase;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;


/**
 * TestCases for android.util.Xml.Encoding.
 */
//FIXME: This is a duplicated testcase. Need to improve the coverage tool in future.
public class XmlEncodingTest extends TestCase {

    private static final String STR_ISO_8859_1 = "ISO-8859-1";
    private static final String STR_US_ASCII = "US-ASCII";
    private static final String STR_UTF_16 = "UTF-16";
    private static final String STR_UTF_8 = "UTF-8";
    private static final String STR_END_DOCUMENT = "endDocument";
    private static final String STR_1_NUMBER = "1";
    private static final String STR_0_NUMBER = "0";
    private static final String STR_EMPTY = "";
    private static final String STR_END_ELEMENT = "endElement";
    private static final String STR_ABC = "abc";
    private static final String ATT_NAME = "name";
    private static final String TAG_SON = "Son";
    private static final String TAG_TEST = "Test";
    private static final String STR_START_ELEMENT = "startElement:";
    private static final String STR_START_DOCUMENT = "startDocument";
    private static final String STR_SET_DOCUMENT_LOCATOR = "setDocumentLocator:Locator[publicId: "
               + "null, systemId: null, line: 1, column: 0]";

    final String sourceStr = "<" + TAG_TEST + "><" + TAG_SON + " " + ATT_NAME + "=\"" + STR_ABC
            + "\"/></" + TAG_TEST + ">";

    // used for ContentHandler to pause XML.
    private static final String STR_START_PREFIX_MAPPING_TAG = "startPrefixMapping:";
    private static final String STR_SKIPPED_ENTITY_TAG = "skippedEntity:";
    private static final String STR_SET_DOCUMENT_LOCATOR_TAG = "setDocumentLocator:";
    private static final String STR_PROCESSING_INSTRUCTION_TAG = "processingInstruction:";
    private static final String STR_IGNORABLE_WHITESPACE_TAG = "ignorableWhitespace:";
    private static final String STR_END_PREFIX_MAPPING_TAG = "endPrefixMapping:";
    private static final String STR_LENGTH_TAG = "length:";
    private static final String STR_START_TAG = "start:";
    private static final String STR_CHARACTERS_TAG = "characters:";

    public void testValueOf() {

        // test US-ASCII
        DefaultContentHandler dc = new DefaultContentHandler();
        try {
            Xml.parse(new ByteArrayInputStream(sourceStr.getBytes(STR_US_ASCII)),
                            Xml.Encoding.US_ASCII, dc);
            assertEquals(STR_SET_DOCUMENT_LOCATOR, dc.mVec.elementAt(0));
            assertEquals(STR_START_DOCUMENT, dc.mVec.elementAt(1));
            assertEquals(STR_START_ELEMENT, dc.mVec.elementAt(2));
            assertEquals(TAG_TEST, dc.mVec.elementAt(3));
            assertEqualsOrEmpty(TAG_TEST, dc.mVec.elementAt(4));
            assertEquals(STR_0_NUMBER, dc.mVec.elementAt(5));
            assertEquals(STR_START_ELEMENT, dc.mVec.elementAt(6));
            assertEquals(TAG_SON, dc.mVec.elementAt(7));
            assertEqualsOrEmpty(TAG_SON, dc.mVec.elementAt(8));
            assertEquals(STR_1_NUMBER, dc.mVec.elementAt(9));
            assertEquals(ATT_NAME, dc.mVec.elementAt(10));
            assertEquals(STR_ABC, dc.mVec.elementAt(11));
            assertEquals(STR_END_ELEMENT, dc.mVec.elementAt(12));
            assertEquals(STR_EMPTY, dc.mVec.elementAt(13));
            assertEquals(TAG_SON, dc.mVec.elementAt(14));
            assertEqualsOrEmpty(TAG_SON, dc.mVec.elementAt(15));
            assertEquals(STR_END_ELEMENT, dc.mVec.elementAt(16));
            assertEquals(STR_EMPTY, dc.mVec.elementAt(17));
            assertEquals(TAG_TEST, dc.mVec.elementAt(18));
            assertEqualsOrEmpty(TAG_TEST, dc.mVec.elementAt(19));
            assertEquals(STR_END_DOCUMENT, dc.mVec.elementAt(20));
        } catch (SAXException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        }

        // test UTF-8
        dc = new DefaultContentHandler();
        try {
            Xml.parse(new ByteArrayInputStream(sourceStr.getBytes(STR_UTF_8)),
                            Xml.Encoding.UTF_8, dc);
            assertEquals(STR_SET_DOCUMENT_LOCATOR, dc.mVec.elementAt(0));
            assertEquals(STR_START_DOCUMENT, dc.mVec.elementAt(1));
            assertEquals(STR_START_ELEMENT, dc.mVec.elementAt(2));
            assertEquals(TAG_TEST, dc.mVec.elementAt(3));
            assertEqualsOrEmpty(TAG_TEST, dc.mVec.elementAt(4));
            assertEquals(STR_0_NUMBER, dc.mVec.elementAt(5));
            assertEquals(STR_START_ELEMENT, dc.mVec.elementAt(6));
            assertEquals(TAG_SON, dc.mVec.elementAt(7));
            assertEqualsOrEmpty(TAG_SON, dc.mVec.elementAt(8));
            assertEquals(STR_1_NUMBER, dc.mVec.elementAt(9));
            assertEquals(ATT_NAME, dc.mVec.elementAt(10));
            assertEquals(STR_ABC, dc.mVec.elementAt(11));
            assertEquals(STR_END_ELEMENT, dc.mVec.elementAt(12));
            assertEquals(STR_EMPTY, dc.mVec.elementAt(13));
            assertEquals(TAG_SON, dc.mVec.elementAt(14));
            assertEqualsOrEmpty(TAG_SON, dc.mVec.elementAt(15));
            assertEquals(STR_END_ELEMENT, dc.mVec.elementAt(16));
            assertEquals(STR_EMPTY, dc.mVec.elementAt(17));
            assertEquals(TAG_TEST, dc.mVec.elementAt(18));
            assertEqualsOrEmpty(TAG_TEST, dc.mVec.elementAt(19));
            assertEquals(STR_END_DOCUMENT, dc.mVec.elementAt(20));
        } catch (SAXException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        }

        // test UTF-16
        dc = new DefaultContentHandler();
        try {
            Xml.parse(new ByteArrayInputStream(sourceStr.getBytes(STR_UTF_16)),
                            Xml.Encoding.UTF_16, dc);
            assertEquals(STR_SET_DOCUMENT_LOCATOR, dc.mVec.elementAt(0));
            assertEquals(STR_START_DOCUMENT, dc.mVec.elementAt(1));
            assertEquals(STR_START_ELEMENT, dc.mVec.elementAt(2));
            assertEquals(TAG_TEST, dc.mVec.elementAt(3));
            assertEqualsOrEmpty(TAG_TEST, dc.mVec.elementAt(4));
            assertEquals(STR_0_NUMBER, dc.mVec.elementAt(5));
            assertEquals(STR_START_ELEMENT, dc.mVec.elementAt(6));
            assertEquals(TAG_SON, dc.mVec.elementAt(7));
            assertEqualsOrEmpty(TAG_SON, dc.mVec.elementAt(8));
            assertEquals(STR_1_NUMBER, dc.mVec.elementAt(9));
            assertEquals(ATT_NAME, dc.mVec.elementAt(10));
            assertEquals(STR_ABC, dc.mVec.elementAt(11));
            assertEquals(STR_END_ELEMENT, dc.mVec.elementAt(12));
            assertEquals(STR_EMPTY, dc.mVec.elementAt(13));
            assertEquals(TAG_SON, dc.mVec.elementAt(14));
            assertEqualsOrEmpty(TAG_SON, dc.mVec.elementAt(15));
            assertEquals(STR_END_ELEMENT, dc.mVec.elementAt(16));
            assertEquals(STR_EMPTY, dc.mVec.elementAt(17));
            assertEquals(TAG_TEST, dc.mVec.elementAt(18));
            assertEqualsOrEmpty(TAG_TEST, dc.mVec.elementAt(19));
            assertEquals(STR_END_DOCUMENT, dc.mVec.elementAt(20));
        } catch (SAXException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        }

        // test ISO-8859-1
        dc = new DefaultContentHandler();
        try {
            Xml.parse(new ByteArrayInputStream(sourceStr.getBytes(STR_ISO_8859_1)),
                            Xml.Encoding.ISO_8859_1, dc);
            assertEquals(STR_SET_DOCUMENT_LOCATOR, dc.mVec.elementAt(0));
            assertEquals(STR_START_DOCUMENT, dc.mVec.elementAt(1));
            assertEquals(STR_START_ELEMENT, dc.mVec.elementAt(2));
            assertEquals(TAG_TEST, dc.mVec.elementAt(3));
            assertEqualsOrEmpty(TAG_TEST, dc.mVec.elementAt(4));
            assertEquals(STR_0_NUMBER, dc.mVec.elementAt(5));
            assertEquals(STR_START_ELEMENT, dc.mVec.elementAt(6));
            assertEquals(TAG_SON, dc.mVec.elementAt(7));
            assertEqualsOrEmpty(TAG_SON, dc.mVec.elementAt(8));
            assertEquals(STR_1_NUMBER, dc.mVec.elementAt(9));
            assertEquals(ATT_NAME, dc.mVec.elementAt(10));
            assertEquals(STR_ABC, dc.mVec.elementAt(11));
            assertEquals(STR_END_ELEMENT, dc.mVec.elementAt(12));
            assertEquals(STR_EMPTY, dc.mVec.elementAt(13));
            assertEquals(TAG_SON, dc.mVec.elementAt(14));
            assertEqualsOrEmpty(TAG_SON, dc.mVec.elementAt(15));
            assertEquals(STR_END_ELEMENT, dc.mVec.elementAt(16));
            assertEquals(STR_EMPTY, dc.mVec.elementAt(17));
            assertEquals(TAG_TEST, dc.mVec.elementAt(18));
            assertEqualsOrEmpty(TAG_TEST, dc.mVec.elementAt(19));
            assertEquals(STR_END_DOCUMENT, dc.mVec.elementAt(20));
        } catch (SAXException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    class DefaultContentHandler implements ContentHandler {

        public Vector<String> mVec = new Vector<String>();

        public void characters(char[] ch, int start, int length) throws SAXException {
            mVec.add(STR_CHARACTERS_TAG + new String(ch));
            mVec.add(STR_START_TAG + start);
            mVec.add(STR_LENGTH_TAG + length);
        }

        public void endDocument() throws SAXException {
            mVec.add(STR_END_DOCUMENT);
        }

        public void endElement(String uri, String localName, String name) throws SAXException {
            mVec.add(STR_END_ELEMENT);
            mVec.add(uri);
            mVec.add(localName);
            mVec.add(name);
        }

        public void endPrefixMapping(String prefix) throws SAXException {
            mVec.add(STR_END_PREFIX_MAPPING_TAG + prefix);
        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            mVec.add(STR_IGNORABLE_WHITESPACE_TAG + new String(ch));
            mVec.add(STR_START_TAG + start);
            mVec.add(STR_LENGTH_TAG + length);
        }

        public void processingInstruction(String target, String data) throws SAXException {
            mVec.add(STR_PROCESSING_INSTRUCTION_TAG + target);
            mVec.add(data);
        }

        public void setDocumentLocator(Locator locator) {
            mVec.add(STR_SET_DOCUMENT_LOCATOR_TAG + locator);
        }

        public void skippedEntity(String name) throws SAXException {
            mVec.add(STR_SKIPPED_ENTITY_TAG + name);
        }

        public void startDocument() throws SAXException {
            mVec.add(STR_START_DOCUMENT);
        }

        public void startElement(String uri, String localName, String name, Attributes atts)
                throws SAXException {
            mVec.add(STR_START_ELEMENT + uri);
            mVec.add(localName);
            mVec.add(name);
            mVec.add(atts.getLength() + STR_EMPTY);
            for (int i = 0; i < atts.getLength(); i++) {
                mVec.add(atts.getLocalName(i));
                mVec.add(atts.getValue(i));
            }
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            mVec.add(STR_START_PREFIX_MAPPING_TAG + prefix);
            mVec.add(uri);
        }

    }

    private void assertEqualsOrEmpty(String expected, String actual) {
        if (actual.length() != 0) {
            assertEquals(expected, actual);
        }
    }
}
