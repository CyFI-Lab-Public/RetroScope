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

package android.sax.cts;


import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.sax.Element;
import android.sax.ElementListener;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.sax.TextElementListener;
import android.test.AndroidTestCase;

import java.io.StringReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class ElementTest extends AndroidTestCase {
    private static final String ATOM_NAMESPACE = "http://www.w3.org/2005/Atom";
    private static final String FEED = "feed";
    private static final String XMLFILE = "<feed xmlns='http://www.w3.org/2005/Atom'>"
            + "<name><id>bob</id></name>"
            + "<entry1><id year=\"2009\">james</id></entry1>"
            + "<entry2 year=\"2000\"><id>jim</id></entry2>"
            + "<name><id>tom</id></name>"
            + "<name><id>brett</id></name></feed>";

    private static final String ID = "id";
    private static final String ENTRY1 = "entry1";
    private static final String ENTRY2 = "entry2";
    private static final String NAME = "name";

    private static final String Y2009 = "2009";
    private static final String Y2000 = "2000";
    private static final String JIM = "jim";

    private static final String[] NAMES = { "bob", "tom", "brett" };

    private int mNameIndex;
    private int mEntry1ListenersCalled;
    private int mEntry2ListenersCalled;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mNameIndex = 0;
        mEntry1ListenersCalled = 0;
        mEntry2ListenersCalled = 0;
    }

    public void testParse() throws Exception {
        RootElement root = new RootElement(ATOM_NAMESPACE, FEED);
        assertNotNull(root);
        Element name = root.getChild(ATOM_NAMESPACE, NAME);
        name.getChild(ATOM_NAMESPACE, ID).setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                assertEquals(NAMES[mNameIndex], body);
                mNameIndex++;
            }
        });

        Element entry1 = root.getChild(ATOM_NAMESPACE, ENTRY1);
        entry1.getChild(ATOM_NAMESPACE, ID).setElementListener(new ElementListener() {
            public void start(Attributes attributes) {
                assertEquals(Y2009, attributes.getValue(0));
                mEntry1ListenersCalled++;
            }

            public void end() {
                mEntry1ListenersCalled++;
            }
        });

        Element entry2 = root.requireChild(ATOM_NAMESPACE, ENTRY2);
        entry2.setStartElementListener(new StartElementListener() {
            public void start(Attributes attributes) {
                assertEquals(Y2000, attributes.getValue(0));
                mEntry2ListenersCalled++;
            }
        });
        entry2.setEndElementListener(new EndElementListener() {
            public void end() {
                mEntry2ListenersCalled++;
            }
        });
        entry2.getChild(ATOM_NAMESPACE, ID).setTextElementListener(new TextElementListener() {
            public void start(Attributes attributes) {
                mEntry2ListenersCalled++;
            }

            public void end(String body) {
                assertEquals(JIM, body);
                mEntry2ListenersCalled++;
            }
        });

        SAXParserFactory spfactory = SAXParserFactory.newInstance();
        spfactory.setValidating(false);

        SAXParser saxParser = spfactory.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setContentHandler(root.getContentHandler());

        InputSource source = new InputSource(new StringReader(XMLFILE));
        xmlReader.parse(source);

        assertEquals(NAMES.length, mNameIndex);
        assertEquals(2, mEntry1ListenersCalled);
        assertEquals(4, mEntry2ListenersCalled);
    }
}
