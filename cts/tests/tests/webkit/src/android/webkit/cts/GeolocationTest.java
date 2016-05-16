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

package android.webkit.cts;

import android.content.Context;
import android.cts.util.PollingCheck;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.cts.WebViewOnUiThread.WaitForLoadedClient;
import android.webkit.cts.WebViewOnUiThread.WaitForProgressClient;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Callable;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;

public class GeolocationTest extends ActivityInstrumentationTestCase2<WebViewStubActivity> {

    // TODO Write additional tests to cover:
    // - test that the errors are correct
    // - test that use of gps and network location is correct

    // The URLs does not matter since the tests will intercept the load, but it has to be a real
    // url, and different domains.
    private static final String URL_1 = "http://www.example.com";
    private static final String URL_2 = "http://www.example.org";

    private static final String JS_INTERFACE_NAME = "Android";
    private static final int POLLING_TIMEOUT = 60 * 1000;
    private static final int LOCATION_THREAD_UPDATE_WAIT_MS = 250;

    // static HTML page always injected instead of the url loaded
    private static final String RAW_HTML =
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "  <head>\n" +
            "    <title>Geolocation</title>\n" +
            "    <script>\n" +
            "      function gotPos(position) {\n" +
            "        " + JS_INTERFACE_NAME + ".gotLocation();\n" +
            "      }\n" +
            "      function initiate_getCurrentPosition() {\n" +
            "        navigator.geolocation.getCurrentPosition(\n" +
            "            gotPos,\n" +
            "            handle_errors,\n" +
            "            {maximumAge:1000});\n" +
            "      }\n" +
            "      function handle_errors(error) {\n" +
            "        switch(error.code) {\n" +
            "          case error.PERMISSION_DENIED:\n" +
            "            " + JS_INTERFACE_NAME + ".errorDenied(); break;\n" +
            "          case error.POSITION_UNAVAILABLE:\n" +
            "            " + JS_INTERFACE_NAME + ".errorUnavailable(); break;\n" +
            "          case error.TIMEOUT:\n" +
            "            " + JS_INTERFACE_NAME + ".errorTimeout(); break;\n" +
            "          default: break;\n" +
            "        }\n" +
            "      }\n" +
            "    </script>\n" +
            "  </head>\n" +
            "  <body onload=\"initiate_getCurrentPosition();\">\n" +
            "  </body>\n" +
            "</html>";

    private JavascriptStatusReceiver mJavascriptStatusReceiver;
    private LocationManager mLocationManager;
    private WebViewOnUiThread mOnUiThread;
    private Thread mLocationUpdateThread;
    private volatile boolean mLocationUpdateThreadExitRequested;
    private List<String> mProviders;

    public GeolocationTest() throws Exception {
        super("com.android.cts.stub", WebViewStubActivity.class);
    }

    // Both this test and WebViewOnUiThread need to override some of the methods on WebViewClient,
    // so this test sublclasses the WebViewClient from WebViewOnUiThread
    private static class InterceptClient extends WaitForLoadedClient {

        public InterceptClient(WebViewOnUiThread webViewOnUiThread) throws Exception {
            super(webViewOnUiThread);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            // Intercept all page loads with the same geolocation enabled page
            try {
                return new WebResourceResponse("text/html", "utf-8",
                    new ByteArrayInputStream(RAW_HTML.getBytes("UTF-8")));
            } catch(java.io.UnsupportedEncodingException e) {
                return null;
            }
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Set up a WebView with JavaScript and Geolocation enabled
        final String GEO_DIR = "geo_test";
        mOnUiThread = new WebViewOnUiThread(this, getActivity().getWebView());
        mOnUiThread.getSettings().setJavaScriptEnabled(true);
        mOnUiThread.getSettings().setGeolocationEnabled(true);
        mOnUiThread.getSettings().setGeolocationDatabasePath(
                getActivity().getApplicationContext().getDir(GEO_DIR, 0).getPath());

        // Add a JsInterface to report back to the test when a location is received
        mJavascriptStatusReceiver = new JavascriptStatusReceiver();
        mOnUiThread.addJavascriptInterface(mJavascriptStatusReceiver, JS_INTERFACE_NAME);

        // Always intercept all loads with the same geolocation test page
        mOnUiThread.setWebViewClient(new InterceptClient(mOnUiThread));
        // Clear all permissions before each test
        GeolocationPermissions.getInstance().clearAll();
        // Cache this mostly because the lookup is two lines of code
        mLocationManager = (LocationManager)getActivity().getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
        // Add a test provider before each test to inject a location
        mProviders = mLocationManager.getAllProviders();
        for (String provider : mProviders) {
            // Can't mock passive provider.
            if (provider.equals(LocationManager.PASSIVE_PROVIDER)) {
                mProviders.remove(provider);
                break;
            }
        }
        addTestProviders();
    }

    @Override
    protected void tearDown() throws Exception {
        stopUpdateLocationThread();
        // Remove the test provider after each test
        for (String provider : mProviders) {
            try {
                // Work around b/11446702 by clearing the test provider before removing it
                mLocationManager.clearTestProviderEnabled(provider);
                mLocationManager.removeTestProvider(provider);
            } catch (IllegalArgumentException e) {} // Not much to do about this
        }
        mOnUiThread.cleanUp();
        // This will null all member and static variables
        super.tearDown();
    }

    private void addTestProviders() {
        for (String providerName : mProviders) {
            LocationProvider provider = mLocationManager.getProvider(providerName);
            mLocationManager.addTestProvider(provider.getName(),
                    provider.requiresNetwork(), //requiresNetwork,
                    provider.requiresSatellite(), // requiresSatellite,
                    provider.requiresCell(),  // requiresCell,
                    provider.hasMonetaryCost(), // hasMonetaryCost,
                    provider.supportsAltitude(), // supportsAltitude,
                    provider.supportsSpeed(), // supportsSpeed,
                    provider.supportsBearing(), // supportsBearing,
                    provider.getPowerRequirement(), // powerRequirement
                    provider.getAccuracy()); // accuracy
            mLocationManager.setTestProviderEnabled(provider.getName(), true);
        }
    }

    private void startUpdateLocationThread() {
        // Only start the thread once
        if (mLocationUpdateThread == null) {
            mLocationUpdateThreadExitRequested = false;
            mLocationUpdateThread = new Thread() {
                @Override
                public void run() {
                    while (!mLocationUpdateThreadExitRequested) {
                        try {
                            Thread.sleep(LOCATION_THREAD_UPDATE_WAIT_MS);
                        } catch(Exception e) {
                            // Do nothing, an extra update is no problem
                        }
                        updateLocation();
                    }
                }
            };
            mLocationUpdateThread.start();
        }
    }

    private void stopUpdateLocationThread() {
        // Only stop the thread if it was started
        if (mLocationUpdateThread != null) {
            mLocationUpdateThreadExitRequested = true;
            try {
                mLocationUpdateThread.join();
            } catch (InterruptedException e) {
                // Do nothing
            }
            mLocationUpdateThread = null;
        }
    }

    // Update location with a fixed latitude and longtitude, sets the time to the current time.
    private void updateLocation() {
        for (int i = 0; i < mProviders.size(); i++) {
            Location location = new Location(mProviders.get(i));
            location.setLatitude(40);
            location.setLongitude(40);
            location.setAccuracy(1.0f);
            location.setTime(java.lang.System.currentTimeMillis());
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            mLocationManager.setTestProviderLocation(mProviders.get(i), location);
        }
    }

    // Need to set the location just after loading the url. Setting it after each load instead of
    // using a maximum age.
    private void loadUrlAndUpdateLocation(String url) {
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        startUpdateLocationThread();
    }

    // WebChromeClient that accepts each location for one load. WebChromeClient is used in
    // WebViewOnUiThread to detect when the page is loaded, so subclassing the one used there.
    private static class TestSimpleGeolocationRequestWebChromeClient
                extends WaitForProgressClient {
        private boolean mReceivedRequest = false;
        private final boolean mAccept;
        private final boolean mRetain;

        public TestSimpleGeolocationRequestWebChromeClient(
                WebViewOnUiThread webViewOnUiThread, boolean accept, boolean retain) {
            super(webViewOnUiThread);
            this.mAccept = accept;
            this.mRetain = retain;
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(
                String origin, GeolocationPermissions.Callback callback) {
            mReceivedRequest = true;
            callback.invoke(origin, mAccept, mRetain);
        }
    }

    // Test loading a page and accepting the domain for one load
    public void testSimpleGeolocationRequestAcceptOnce() throws Exception {
        final TestSimpleGeolocationRequestWebChromeClient chromeClientAcceptOnce =
                new TestSimpleGeolocationRequestWebChromeClient(mOnUiThread, true, false);
        mOnUiThread.setWebChromeClient(chromeClientAcceptOnce);
        loadUrlAndUpdateLocation(URL_1);
        Callable<Boolean> receivedRequest = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return chromeClientAcceptOnce.mReceivedRequest;
            }
        };
        PollingCheck.check("Geolocation prompt not called", POLLING_TIMEOUT, receivedRequest);
        Callable<Boolean> receivedLocation = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return mJavascriptStatusReceiver.mHasPosition;
            }
        };
        PollingCheck.check("JS didn't get position", POLLING_TIMEOUT, receivedLocation);
        chromeClientAcceptOnce.mReceivedRequest = false;
        // Load URL again, should receive callback again
        loadUrlAndUpdateLocation(URL_1);
        PollingCheck.check("Geolocation prompt not called", POLLING_TIMEOUT, receivedRequest);
        PollingCheck.check("JS didn't get position", POLLING_TIMEOUT, receivedLocation);
    }

    private static class OriginCheck extends PollingCheck implements
            android.webkit.ValueCallback<Set<String>> {

        private boolean mReceived = false;
        private final Set<String> mExpectedValue;
        private Set<String> mReceivedValue = null;

        public OriginCheck(Set<String> val) {
            mExpectedValue = val;
        }

        @Override
        protected boolean check() {
            if (!mReceived) return false;
            if (mExpectedValue.equals(mReceivedValue)) return true;
            if (mExpectedValue.size() != mReceivedValue.size()) return false;
            // Origins can have different strings even if they represent the same origin,
            // for example http://www.example.com is the same origin as http://www.example.com/
            // and they are both valid representations
            for (String origin : mReceivedValue) {
                if (mExpectedValue.contains(origin)) continue;
                if (origin.endsWith("/")) {
                    if (mExpectedValue.contains(origin.substring(0, origin.length() - 1))) {
                        continue;
                    }
                } else {
                    if (mExpectedValue.contains(origin + "/")) continue;
                }
                return false;
            }
            return true;
        }
        @Override
        public void onReceiveValue(Set<String> value) {
            mReceived = true;
            mReceivedValue = value;
        }
    }

    // Class that waits and checks for a particular value being received
    private static class BooleanCheck extends PollingCheck implements
            android.webkit.ValueCallback<Boolean> {

        private boolean mReceived = false;
        private final boolean mExpectedValue;
        private boolean mReceivedValue;

        public BooleanCheck(boolean val) {
            mExpectedValue = val;
        }

        @Override
        protected boolean check() {
            return mReceived && mReceivedValue == mExpectedValue;
        }

        @Override
        public void onReceiveValue(Boolean value) {
            mReceived = true;
            mReceivedValue = value;
        }
    }

    // Test loading a page and retaining the domain forever
    public void testSimpleGeolocationRequestAcceptAlways() throws Exception {
        final TestSimpleGeolocationRequestWebChromeClient chromeClientAcceptAlways =
                new TestSimpleGeolocationRequestWebChromeClient(mOnUiThread, true, true);
        mOnUiThread.setWebChromeClient(chromeClientAcceptAlways);
        // Load url once, and the callback should accept the domain for all future loads
        loadUrlAndUpdateLocation(URL_1);
        Callable<Boolean> receivedRequest = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return chromeClientAcceptAlways.mReceivedRequest;
            }
        };
        PollingCheck.check("Geolocation prompt not called", POLLING_TIMEOUT, receivedRequest);
        Callable<Boolean> receivedLocation = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return mJavascriptStatusReceiver.mHasPosition;
            }
        };
        PollingCheck.check("JS didn't get position", POLLING_TIMEOUT, receivedLocation);
        chromeClientAcceptAlways.mReceivedRequest = false;
        mJavascriptStatusReceiver.clearState();
        // Load the same URL again
        loadUrlAndUpdateLocation(URL_1);
        PollingCheck.check("JS didn't get position", POLLING_TIMEOUT, receivedLocation);
        // Assert prompt for geolocation permission is not called the second time
        assertFalse(chromeClientAcceptAlways.mReceivedRequest);
        // Check that the permission is in GeolocationPermissions
        BooleanCheck trueCheck = new BooleanCheck(true);
        GeolocationPermissions.getInstance().getAllowed(URL_1, trueCheck);
        trueCheck.run();
        Set<String> acceptedOrigins = new TreeSet<String>();
        acceptedOrigins.add(URL_1);
        OriginCheck originCheck = new OriginCheck(acceptedOrigins);
        GeolocationPermissions.getInstance().getOrigins(originCheck);
        originCheck.run();

        // URL_2 should get a prompt
        chromeClientAcceptAlways.mReceivedRequest = false;
        loadUrlAndUpdateLocation(URL_2);
        // Checking the callback for geolocation permission prompt is called
        PollingCheck.check("Geolocation prompt not called", POLLING_TIMEOUT, receivedRequest);
        PollingCheck.check("JS didn't get position", POLLING_TIMEOUT, receivedLocation);
        acceptedOrigins.add(URL_2);
        originCheck = new OriginCheck(acceptedOrigins);
        GeolocationPermissions.getInstance().getOrigins(originCheck);
        originCheck.run();
        // Remove a domain manually that was added by the callback
        GeolocationPermissions.getInstance().clear(URL_1);
        acceptedOrigins.remove(URL_1);
        originCheck = new OriginCheck(acceptedOrigins);
        GeolocationPermissions.getInstance().getOrigins(originCheck);
        originCheck.run();
    }

    // Test the GeolocationPermissions API
    public void testGeolocationPermissions() {
        Set<String> acceptedOrigins = new TreeSet<String>();
        BooleanCheck falseCheck = new BooleanCheck(false);
        GeolocationPermissions.getInstance().getAllowed(URL_2, falseCheck);
        falseCheck.run();
        OriginCheck originCheck = new OriginCheck(acceptedOrigins);
        GeolocationPermissions.getInstance().getOrigins(originCheck);
        originCheck.run();

        // Remove a domain that has not been allowed
        GeolocationPermissions.getInstance().clear(URL_2);
        acceptedOrigins.remove(URL_2);
        originCheck = new OriginCheck(acceptedOrigins);
        GeolocationPermissions.getInstance().getOrigins(originCheck);
        originCheck.run();

        // Add a domain
        acceptedOrigins.add(URL_2);
        GeolocationPermissions.getInstance().allow(URL_2);
        originCheck = new OriginCheck(acceptedOrigins);
        GeolocationPermissions.getInstance().getOrigins(originCheck);
        originCheck.run();
        BooleanCheck trueCheck = new BooleanCheck(true);
        GeolocationPermissions.getInstance().getAllowed(URL_2, trueCheck);
        trueCheck.run();

        // Add a domain
        acceptedOrigins.add(URL_1);
        GeolocationPermissions.getInstance().allow(URL_1);
        originCheck = new OriginCheck(acceptedOrigins);
        GeolocationPermissions.getInstance().getOrigins(originCheck);
        originCheck.run();

        // Remove a domain that has been allowed
        GeolocationPermissions.getInstance().clear(URL_2);
        acceptedOrigins.remove(URL_2);
        originCheck = new OriginCheck(acceptedOrigins);
        GeolocationPermissions.getInstance().getOrigins(originCheck);
        originCheck.run();
        falseCheck = new BooleanCheck(false);
        GeolocationPermissions.getInstance().getAllowed(URL_2, falseCheck);
        falseCheck.run();

        // Try to clear all domains
        GeolocationPermissions.getInstance().clearAll();
        acceptedOrigins.clear();
        originCheck = new OriginCheck(acceptedOrigins);
        GeolocationPermissions.getInstance().getOrigins(originCheck);
        originCheck.run();

        // Add a domain
        acceptedOrigins.add(URL_1);
        GeolocationPermissions.getInstance().allow(URL_1);
        originCheck = new OriginCheck(acceptedOrigins);
        GeolocationPermissions.getInstance().getOrigins(originCheck);
        originCheck.run();
    }

    // Test loading pages and checks rejecting once and recjecting the domain forever
    public void testSimpleGeolocationRequestReject() throws Exception {
        final TestSimpleGeolocationRequestWebChromeClient chromeClientRejectOnce =
                new TestSimpleGeolocationRequestWebChromeClient(mOnUiThread, false, false);
        mOnUiThread.setWebChromeClient(chromeClientRejectOnce);
        // Load url once, and the callback should reject it once
        mOnUiThread.loadUrlAndWaitForCompletion(URL_1);
        Callable<Boolean> receivedRequest = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return chromeClientRejectOnce.mReceivedRequest;
            }
        };
        PollingCheck.check("Geolocation prompt not called", POLLING_TIMEOUT, receivedRequest);
        Callable<Boolean> locationDenied = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return mJavascriptStatusReceiver.mDenied;
            }
        };
        PollingCheck.check("JS got position", POLLING_TIMEOUT, locationDenied);
        // Same result should happen on next run
        chromeClientRejectOnce.mReceivedRequest = false;
        mOnUiThread.loadUrlAndWaitForCompletion(URL_1);
        PollingCheck.check("Geolocation prompt not called", POLLING_TIMEOUT, receivedRequest);
        PollingCheck.check("JS got position", POLLING_TIMEOUT, locationDenied);

        // Try to reject forever
        final TestSimpleGeolocationRequestWebChromeClient chromeClientRejectAlways =
            new TestSimpleGeolocationRequestWebChromeClient(mOnUiThread, false, true);
        mOnUiThread.setWebChromeClient(chromeClientRejectAlways);
        mOnUiThread.loadUrlAndWaitForCompletion(URL_2);
        PollingCheck.check("Geolocation prompt not called", POLLING_TIMEOUT, receivedRequest);
        PollingCheck.check("JS didn't get position", POLLING_TIMEOUT, locationDenied);
        // second load should now not get a prompt
        chromeClientRejectAlways.mReceivedRequest = false;
        mOnUiThread.loadUrlAndWaitForCompletion(URL_2);
        PollingCheck.check("JS didn't get position", POLLING_TIMEOUT, locationDenied);
        PollingCheck.check("Geolocation prompt not called", POLLING_TIMEOUT, receivedRequest);

        // Test if it gets added to origins
        Set<String> acceptedOrigins = new TreeSet<String>();
        acceptedOrigins.add(URL_2);
        OriginCheck domainCheck = new OriginCheck(acceptedOrigins);
        GeolocationPermissions.getInstance().getOrigins(domainCheck);
        domainCheck.run();
        // And now check that getAllowed returns false
        BooleanCheck falseCheck = new BooleanCheck(false);
        GeolocationPermissions.getInstance().getAllowed(URL_1, falseCheck);
        falseCheck.run();
    }

    // Object added to the page via AddJavascriptInterface() that is used by the test Javascript to
    // notify back to Java when a location or error is received.
    public final static class JavascriptStatusReceiver {
        public volatile boolean mHasPosition = false;
        public volatile boolean mDenied = false;
        public volatile boolean mUnavailable = false;
        public volatile boolean mTimeout = false;

        public void clearState() {
            mHasPosition = false;
            mDenied = false;
            mUnavailable = false;
            mTimeout = false;
        }

        @JavascriptInterface
        public void errorDenied() {
            mDenied = true;
        }

        @JavascriptInterface
        public void errorUnavailable() {
            mUnavailable = true;
        }

        @JavascriptInterface
        public void errorTimeout() {
            mTimeout = true;
        }

        @JavascriptInterface
        public void gotLocation() {
            mHasPosition = true;
        }
    }
}
