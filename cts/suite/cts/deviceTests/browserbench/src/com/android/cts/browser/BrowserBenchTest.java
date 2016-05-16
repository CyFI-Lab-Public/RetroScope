/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.cts.browser;

import android.content.Intent;
import android.cts.util.WatchDog;
import android.net.Uri;
import android.provider.Browser;
import android.util.Log;
import android.webkit.cts.CtsTestServer;

import android.cts.util.CtsAndroidTestCase;
import com.android.cts.util.ResultType;
import com.android.cts.util.ResultUnit;
import com.android.cts.util.Stat;
import com.android.cts.util.TimeoutReq;

import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
/**
 * Browser benchmarking.
 * It launches an activity with URL and wait for POST from the client.
 */
public class BrowserBenchTest extends CtsAndroidTestCase {
    private static final String TAG = BrowserBenchTest.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final String OCTANE_START_FILE = "octane/index.html";
    private static final String ROBOHORNET_START_FILE = "robohornet/robohornet.html";
    private static final String HOST_COMPLETION_BROADCAST = "com.android.cts.browser.completion";
    // time-out for watch-dog. POST should happen within this time.
    private static long BROWSER_POST_TIMEOUT_IN_MS = 10 * 60 * 1000L;
    // watch-dog will time-out first. So make it long enough.
    private static long BROWSER_COMPLETION_TIMEOUT_IN_MS = 60 * 60 * 1000L;
    private static final String HTTP_USER_AGENT = "User-Agent";
    private CtsTestServer mWebServer;
    // used for final score
    private ResultType mTypeNonFinal = ResultType.NEUTRAL;
    private ResultUnit mUnitNonFinal = ResultUnit.NONE;
    // used for all other scores
    private ResultType mTypeFinal = ResultType.NEUTRAL;
    private ResultUnit mUnitFinal = ResultUnit.SCORE;
    private WatchDog mWatchDog;
    private CountDownLatch mLatch;
    // can be changed by each test before starting
    private volatile int mNumberRepeat;
    /** tells how many tests have run up to now */
    private volatile int mRunIndex;
    /** stores results for each runs. last entry will be the final score. */
    private LinkedHashMap<String, double[]> mResultsMap;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mWebServer = new CtsTestServer(getContext()) {
            @Override
            protected HttpResponse onPost(HttpRequest request) throws Exception {
                // post uri will look like "cts_report.html?final=1&score=10.1&message=hello"
                RequestLine requestLine = request.getRequestLine();
                String uriString = URLDecoder.decode(requestLine.getUri(), "UTF-8");
                if (DEBUG) {
                    Log.i(TAG, "uri:" + uriString);
                }
                String resultRe =
                        ".*cts_report.html\\?final=([\\d])&score=([\\d]+\\.?[\\d]*)&message=([\\w][\\w ]*)";
                Pattern resultPattern = Pattern.compile(resultRe);
                Matcher matchResult = resultPattern.matcher(uriString);
                if (matchResult.find()) {
                    int isFinal = Integer.parseInt(matchResult.group(1));
                    double score = Double.parseDouble(matchResult.group(2));
                    String message = matchResult.group(3);
                    Log.i(TAG, message + ":" + score);
                    if (!mResultsMap.containsKey(message)) {
                        mResultsMap.put(message, new double[mNumberRepeat]);
                    }
                    double[] scores = mResultsMap.get(message);
                    scores[mRunIndex] = score;
                    if (isFinal == 1) {
                        String userAgent = request.getFirstHeader(HTTP_USER_AGENT).getValue();
                        getReportLog().printValue(HTTP_USER_AGENT + "=" + userAgent, 0,
                                ResultType.NEUTRAL, ResultUnit.NONE);
                        mLatch.countDown();
                    }
                    mWatchDog.reset();
                }
                return null; // default response is OK as it will be ignored by client anyway.
            }
        };
        mResultsMap = new LinkedHashMap<String, double[]>();
        mWatchDog = new WatchDog(BROWSER_POST_TIMEOUT_IN_MS);
        mWatchDog.start();
    }

    @Override
    protected void tearDown() throws Exception {
        mWatchDog.stop();
        mWebServer.shutdown();
        mWebServer = null;
        mResultsMap = null;
        super.tearDown();
    }

    @TimeoutReq(minutes = 60)
    public void testOctane() throws InterruptedException {
        String url = mWebServer.getAssetUrl(OCTANE_START_FILE) + "?auto=1";
        final int kRepeat = 5;
        doTest(url, ResultType.LOWER_BETTER, ResultUnit.MS,
                ResultType.HIGHER_BETTER, ResultUnit.SCORE, kRepeat);
    }

    private void doTest(String url, ResultType typeNonFinal, ResultUnit unitNonFinal,
            ResultType typeFinal, ResultUnit unitFinal, int numberRepeat)
                    throws InterruptedException {
        mTypeNonFinal = typeNonFinal;
        mUnitNonFinal = unitNonFinal;
        mTypeFinal = typeFinal;
        mUnitFinal = unitFinal;
        mNumberRepeat = numberRepeat;
        Uri uri = Uri.parse(url);
        for (mRunIndex = 0; mRunIndex < numberRepeat; mRunIndex++) {
            Log.i(TAG, mRunIndex + "-th round");
            mLatch = new CountDownLatch(1);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // force using only one window or tab
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, getContext().getPackageName());
            getContext().startActivity(intent);
            boolean ok = mLatch.await(BROWSER_COMPLETION_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);
            assertTrue("timed-out", ok);
        }
        // it is somewhat awkward to handle the last one specially with Map
        int numberEntries = mResultsMap.size();
        int numberToProcess = 1;
        for (Map.Entry<String, double[]> entry : mResultsMap.entrySet()) {
            String message = entry.getKey();
            double[] scores = entry.getValue();
            if (numberToProcess == numberEntries) { // final score
                // store the whole results first
                getReportLog().printArray(message, scores, mTypeFinal, mUnitFinal);
                getReportLog().printSummary(message, Stat.getAverage(scores), mTypeFinal,
                        mUnitFinal);
            } else { // interim results
                getReportLog().printArray(message, scores, mTypeNonFinal, mUnitNonFinal);
            }
            numberToProcess++;
        }
    }
}
