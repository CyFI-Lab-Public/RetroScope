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

import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.tests.getinfo.DeviceInfoConstants;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Unit tests for {@link DeviceInfoResult}
 */
public class DeviceInfoResultTest extends TestCase {

    private DeviceInfoResult mDeserializingInfo;

    @Override
    protected void setUp() throws Exception {
        mDeserializingInfo = new DeviceInfoResult() {
            // override parent to advance xml parser to correct tag
            @Override
            void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.getName().equals(TAG)) {
                        super.parse(parser);
                        return;
                    }
                    eventType = parser.next();
                }
                throw new XmlPullParserException(String.format("Could not find tag %s", TAG));
            }
        };
    }

    public void testFeatures() throws Exception {
        assertSerializeParse(DeviceInfoConstants.FEATURES, "");
        assertSerializeParse(DeviceInfoConstants.FEATURES,
                "android.hardware.audio.low_latency:sdk:false;");
        assertSerializeParse(DeviceInfoConstants.FEATURES, "android.hardware.audio.low_latency:"
                + "sdk:false;android.hardware.bluetooth:sdk:true;");
    }

    public void testProcesses() throws Exception {
        assertSerializeParse(DeviceInfoConstants.PROCESSES, "");
        assertSerializeParse(DeviceInfoConstants.PROCESSES, "ueventd:0;");
        assertSerializeParse(DeviceInfoConstants.PROCESSES, "ueventd:0;netd:0;");
    }

    public void testOpenGlTextureFormats() throws Exception {
        assertSerializeParse(DeviceInfoConstants.OPEN_GL_COMPRESSED_TEXTURE_FORMATS, "");
        assertSerializeParse(DeviceInfoConstants.OPEN_GL_COMPRESSED_TEXTURE_FORMATS, "texture1;");
        assertSerializeParse(DeviceInfoConstants.OPEN_GL_COMPRESSED_TEXTURE_FORMATS,
                "texture1;texture2;");
    }

    public void testSystemLibraries() throws Exception {
        assertSerializeParse(DeviceInfoConstants.SYS_LIBRARIES, "");
        assertSerializeParse(DeviceInfoConstants.SYS_LIBRARIES, "lib1;");
        assertSerializeParse(DeviceInfoConstants.SYS_LIBRARIES, "lib1;lib2;");
    }

    private void assertSerializeParse(String name, String value)
            throws IOException, ParseException {
        DeviceInfoResult serializedInfo = new DeviceInfoResult();
        addMetric(name, value, serializedInfo);
        String serializedOutput = serialize(serializedInfo);
        mDeserializingInfo.parse(new StringReader(serializedOutput));
        assertEquals(value, mDeserializingInfo.getMetrics().get(name));
    }

    /**
     * Test populating a combined metric like device serial
     */
    public void testPopulateMetrics_combinedSerial() throws Exception {
        DeviceInfoResult info = new DeviceInfoResult();
        // first add another metric to make hashmap non empty, so combined logic is triggered
        addMetric(DeviceInfoConstants.PROCESSES, "proc", info);
        addMetric(DeviceInfoConstants.SERIAL_NUMBER, "device1", info);
        // ensure the stored serial number equals the value that was just set
        assertEquals("device1", info.getMetrics().get(
                DeviceInfoConstants.SERIAL_NUMBER));
        // now add it again
        addMetric(DeviceInfoConstants.SERIAL_NUMBER, "device1", info);
        // should still equal same value
        assertEquals("device1", info.getMetrics().get(
                DeviceInfoConstants.SERIAL_NUMBER));
        // now store different serial, and expect csv
        addMetric(DeviceInfoConstants.SERIAL_NUMBER, "device2", info);
        assertEquals("device1,device2", info.getMetrics().get(
                DeviceInfoConstants.SERIAL_NUMBER));
    }

    /**
     * Test populating a verified-to-be-identical metric like DeviceInfoConstants.BUILD_FINGERPRINT
     */
    public void testPopulateMetrics_verify() throws Exception {
        DeviceInfoResult info = new DeviceInfoResult();
        addMetric(DeviceInfoConstants.BUILD_FINGERPRINT, "fingerprint1", info);
        // ensure the stored fingerprint equals the value that was just set
        assertEquals("fingerprint1", info.getMetrics().get(
                DeviceInfoConstants.BUILD_FINGERPRINT));
        // now add it again
        addMetric(DeviceInfoConstants.BUILD_FINGERPRINT, "fingerprint1", info);
        // should still equal same value
        assertEquals("fingerprint1", info.getMetrics().get(
                DeviceInfoConstants.BUILD_FINGERPRINT));
        // now store different serial, and expect error message
        addMetric(DeviceInfoConstants.BUILD_FINGERPRINT, "fingerprint2", info);
        assertTrue(info.getMetrics().get(
                DeviceInfoConstants.BUILD_FINGERPRINT).contains("ERROR"));
    }

    /**
     * Helper method to add given metric to the {@link DeviceInfoResult}
     */
    private void addMetric(String metricName, String metricValue, DeviceInfoResult serializedInfo) {
        Map<String, String> collectedMetrics = new HashMap<String, String>();
        collectedMetrics.put(metricName, metricValue);
        serializedInfo.populateMetrics(collectedMetrics);
    }

    /**
     * Helper method to serialize given object to XML
     */
    private String serialize(DeviceInfoResult serializedInfo)
            throws IOException {
        KXmlSerializer xmlSerializer = new KXmlSerializer();
        StringWriter serializedOutput = new StringWriter();
        xmlSerializer.setOutput(serializedOutput);
        serializedInfo.serialize(xmlSerializer);
        return serializedOutput.toString();
    }
}
