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

package com.android.cts.apicoverage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Class that outputs an HTML report of the {@link ApiCoverage} collected. It is the XML report
 * transformed into HTML.
 */
class HtmlReport {

    public static void printHtmlReport(final List<File> testApks, final ApiCoverage apiCoverage,
            final String packageFilter, final String reportTitle, final OutputStream out)
                throws IOException, TransformerException {
        final PipedOutputStream xmlOut = new PipedOutputStream();
        final PipedInputStream xmlIn = new PipedInputStream(xmlOut);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                XmlReport.printXmlReport(testApks, apiCoverage, packageFilter, reportTitle, xmlOut);

                // Close the output stream to avoid "Write dead end" errors.
                try {
                    xmlOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();

        InputStream xsl = CtsApiCoverage.class.getResourceAsStream("/api-coverage.xsl");
        StreamSource xslSource = new StreamSource(xsl);
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(xslSource);

        StreamSource xmlSource = new StreamSource(xmlIn);
        StreamResult result = new StreamResult(out);
        transformer.transform(xmlSource, result);
    }
}
