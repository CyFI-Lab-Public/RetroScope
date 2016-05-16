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

package android.graphics.drawable.cts;

import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Xml;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * The useful methods for graphics.drawable test.
 */
public class DrawableTestUtils {

    public static void skipCurrentTag(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        int type;
        while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
               && (type != XmlPullParser.END_TAG
                       || parser.getDepth() > outerDepth)) {
        }
    }
    
    /**
     * Retrieve an AttributeSet from a XML.
     *
     * @param parser the XmlPullParser to use for the xml parsing.
     * @param searchedNodeName the name of the target node.
     * @return the AttributeSet retrieved from specified node.
     * @throws IOException
     * @throws XmlPullParserException
     */
    public static AttributeSet getAttributeSet(XmlResourceParser parser, String searchedNodeName)
            throws XmlPullParserException, IOException {
        AttributeSet attrs = null;
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && type != XmlPullParser.START_TAG) {
        }
        String nodeName = parser.getName();
        if (!"alias".equals(nodeName)) {
            throw new RuntimeException();
        }
        int outerDepth = parser.getDepth();
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }
            nodeName = parser.getName();
            if (searchedNodeName.equals(nodeName)) {
                outerDepth = parser.getDepth();
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                        && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                    if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                        continue;
                    }
                    nodeName = parser.getName();
                    attrs = Xml.asAttributeSet(parser);
                    break;
                }
                break;
            } else {
                skipCurrentTag(parser);
            }
        }
        return attrs;
    }
}
