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

package com.android.cts.tradefed.result;

import com.android.tradefed.util.xml.AbstractXmlParser.ParseException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.Reader;

/**
 * Helper abstract class for XmlPullParser
 *
 * TODO: move to com.android.tradefed.util.xml
 */
public abstract class AbstractXmlPullParser {

    /**
     * Parse the summary data from the given input data.
     *
     * @param xmlReader the input XML
     * @throws ParseException if failed to parse the summary data.
     */
    public void parse(Reader xmlReader) throws ParseException {
        try {
            XmlPullParserFactory fact = org.xmlpull.v1.XmlPullParserFactory.newInstance();
            XmlPullParser parser = fact.newPullParser();
            parser.setInput (xmlReader);
            parse(parser);
        } catch (XmlPullParserException e) {
           throw new ParseException(e);
        } catch (IOException e) {
            throw new ParseException(e);
        }
    }

    abstract void parse(XmlPullParser parser) throws XmlPullParserException, IOException;

    /**
     * Parse an integer value from an XML attribute
     *
     * @param parser the {@link XmlPullParser}
     * @param name the attribute name
     * @return the parsed value or 0 if it could not be parsed
     */
    protected int parseIntAttr(XmlPullParser parser, String name) {
        try {
            String value = parser.getAttributeValue(null, name);
            if (value != null) {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            // ignore
        }
        return 0;
    }

    /**
     * Parse a boolean attribute value
     */
    protected boolean parseBooleanAttr(XmlPullParser parser, String name) {
        String stringValue = parser.getAttributeValue(null, name);
        return stringValue != null &&
                Boolean.parseBoolean(stringValue);
    }

    /**
     * Helper method for retrieving attribute value with given name
     *
     * @param parser the XmlPullParser
     * @param name the attribute name
     * @return the attribute value
     */
    protected String getAttribute(XmlPullParser parser, String name) {
        return parser.getAttributeValue(null, name);
    }
}
