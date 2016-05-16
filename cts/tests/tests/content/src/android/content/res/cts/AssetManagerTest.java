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
package android.content.res.cts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.test.AndroidTestCase;
import android.util.TypedValue;

import com.android.cts.stub.R;
import com.android.internal.util.XmlUtils;


public class AssetManagerTest extends AndroidTestCase{
    private AssetManager mAssets;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mAssets = mContext.getAssets();
    }

    public void testAssetOperations() throws IOException, XmlPullParserException {
        final Resources res = getContext().getResources();
        final TypedValue value = new TypedValue();
        res.getValue(R.raw.text, value, true);
        final String fileName = "text.txt";
        InputStream inputStream = mAssets.open(fileName);
        assertNotNull(inputStream);
        final String expect = "OneTwoThreeFourFiveSixSevenEightNineTen";
        assertContextEquals(expect, inputStream);
        inputStream = mAssets.open(fileName, AssetManager.ACCESS_BUFFER);
        assertNotNull(inputStream);
        assertContextEquals(expect, inputStream);

        AssetFileDescriptor assetFileDes = mAssets.openFd(fileName);
        assertNotNull(assetFileDes);
        assertContextEquals(expect, assetFileDes.createInputStream());
        assetFileDes = mAssets.openNonAssetFd(value.string.toString());
        assertNotNull(assetFileDes);
        assertContextEquals(expect, assetFileDes.createInputStream());
        assetFileDes = mAssets.openNonAssetFd(value.assetCookie, value.string.toString());
        assertNotNull(assetFileDes);
        assertContextEquals(expect, assetFileDes.createInputStream());

        XmlResourceParser parser = mAssets.openXmlResourceParser("AndroidManifest.xml");
        assertNotNull(parser);
        XmlUtils.beginDocument(parser, "manifest");
        parser = mAssets.openXmlResourceParser(0, "AndroidManifest.xml");
        assertNotNull(parser);
        beginDocument(parser, "manifest");

        String[] files = mAssets.list("");
        boolean result = false;
        for (int i = 0; i < files.length; i++) {
            if (files[i].equals(fileName)) {
                result = true;
                break;
            }
        }
        assertTrue(result);

        try {
            mAssets.open("notExistFile.txt", AssetManager.ACCESS_BUFFER);
            fail("test open(String, int) failed");
        } catch (IOException e) {
            // expected
        }

        try {
            mAssets.openFd("notExistFile.txt");
            fail("test openFd(String) failed");
        } catch (IOException e) {
            // expected
        }

        try {
            mAssets.openNonAssetFd(0, "notExistFile.txt");
            fail("test openNonAssetFd(int, String) failed");
        } catch (IOException e) {
            // expected
        }

        try {
            mAssets.openXmlResourceParser(0, "notExistFile.txt");
            fail("test openXmlResourceParser(int, String) failed");
        } catch (IOException e) {
            // expected
        }

        assertNotNull(mAssets.getLocales());

    }

    private void assertContextEquals(final String expect, final InputStream inputStream)
            throws IOException {
        final BufferedReader bf = new BufferedReader(new InputStreamReader(inputStream));
        final String result = bf.readLine();
        inputStream.close();
        assertNotNull(result);
        assertEquals(expect, result);
    }

    private void beginDocument(final XmlPullParser parser,final  String firstElementName)
            throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG) {
        }
        if (type != XmlPullParser.START_TAG) {
            fail("No start tag found");
        }
        assertEquals(firstElementName, parser.getName());
    }

}
