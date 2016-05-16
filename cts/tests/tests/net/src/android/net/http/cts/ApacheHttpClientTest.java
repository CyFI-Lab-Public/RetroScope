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
package android.net.http.cts;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.test.AndroidTestCase;
import android.util.Log;
import android.webkit.cts.CtsTestServer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ApacheHttpClientTest extends AndroidTestCase {

    private static final String TAG = ApacheHttpClientTest.class.getSimpleName();

    private static final int NUM_DOWNLOADS = 20;

    private static final int SMALL_DOWNLOAD_SIZE = 100 * 1024;

    private CtsTestServer mWebServer;

    private WifiManager mWifiManager;

    private ConnectivityManager mConnectivityManager;

    private boolean mHasTelephony;

    private boolean mHasWifi;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mWebServer = new CtsTestServer(mContext);
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mConnectivityManager = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        PackageManager packageManager = mContext.getPackageManager();
        mHasTelephony = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        mHasWifi = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mWebServer.shutdown();
    }

    public void testExecute_withMobile() throws Exception {
        if (mHasTelephony) {
            disconnectWifiToConnectToMobile();
        }

        downloadMultipleFiles();

        if (mHasWifi) {
            connectToWifi();
        }
    }

    public void testExecute_withWifi() throws Exception {
        if (mHasWifi) {
            if (!mWifiManager.isWifiEnabled()) {
                connectToWifi();
            }
            downloadMultipleFiles();
        }
    }

    private void downloadMultipleFiles() throws ClientProtocolException, IOException {
        List<HttpResponse> responses = new ArrayList<HttpResponse>();
        for (int i = 0; i < NUM_DOWNLOADS; i++) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet request = new HttpGet(getSmallDownloadUrl(i).toString());
            HttpResponse response = httpClient.execute(request);
            responses.add(response);
        }

        for (int i = 0; i < NUM_DOWNLOADS; i++) {
            assertDownloadResponse("Download " + i, SMALL_DOWNLOAD_SIZE, responses.get(i));
        }
    }

    private Uri getSmallDownloadUrl(int index) {
        return Uri.parse(mWebServer.getTestDownloadUrl("cts-small-download-" + index,
                SMALL_DOWNLOAD_SIZE));
    }

    private void assertDownloadResponse(String message, int expectedNumBytes, HttpResponse response)
            throws IllegalStateException, IOException {
        byte[] buffer = new byte[4096];
        assertEquals(200, response.getStatusLine().getStatusCode());

        InputStream stream = response.getEntity().getContent();
        int numBytes = 0;
        while (true) {
            int bytesRead = stream.read(buffer);
            if (bytesRead < 0) {
                break;
            } else {
                numBytes += bytesRead;
            }
        }
        assertEquals(message, SMALL_DOWNLOAD_SIZE, numBytes);
    }

    private void connectToWifi() throws InterruptedException {
        if (!mWifiManager.isWifiEnabled()) {
            ConnectivityActionReceiver receiver =
                    new ConnectivityActionReceiver(ConnectivityManager.TYPE_WIFI, State.CONNECTED);
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(receiver, filter);

            assertTrue(mWifiManager.setWifiEnabled(true));
            assertTrue("Wifi must be configured to connect to an access point for this test.",
                    receiver.waitForStateChange());

            mContext.unregisterReceiver(receiver);
        }
    }

    private void disconnectWifiToConnectToMobile() throws InterruptedException {
        if (mHasWifi && mWifiManager.isWifiEnabled()) {
            ConnectivityActionReceiver connectMobileReceiver =
                    new ConnectivityActionReceiver(ConnectivityManager.TYPE_MOBILE,
                            State.CONNECTED);
            ConnectivityActionReceiver disconnectWifiReceiver =
                    new ConnectivityActionReceiver(ConnectivityManager.TYPE_WIFI,
                            State.DISCONNECTED);
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(connectMobileReceiver, filter);
            mContext.registerReceiver(disconnectWifiReceiver, filter);

            assertTrue(mWifiManager.setWifiEnabled(false));
            assertTrue(disconnectWifiReceiver.waitForStateChange());
            assertTrue(connectMobileReceiver.waitForStateChange());

            mContext.unregisterReceiver(connectMobileReceiver);
            mContext.unregisterReceiver(disconnectWifiReceiver);
        }
    }

    /** Receiver that captures the last connectivity change's network type and state. */
    private class ConnectivityActionReceiver extends BroadcastReceiver {

        private final CountDownLatch mReceiveLatch = new CountDownLatch(1);

        private final int mNetworkType;

        private final State mExpectedState;

        ConnectivityActionReceiver(int networkType, State expectedState) {
            mNetworkType = networkType;
            mExpectedState = expectedState;
        }

        public void onReceive(Context context, Intent intent) {
            NetworkInfo networkInfo = intent.getExtras()
                    .getParcelable(ConnectivityManager.EXTRA_NETWORK_INFO);
            int networkType = networkInfo.getType();
            State networkState = networkInfo.getState();
            Log.i(TAG, "Network type: " + networkType + " State: " + networkInfo.getState());
            if (networkType == mNetworkType && networkInfo.getState() == mExpectedState) {
                mReceiveLatch.countDown();
            }
        }

        public boolean waitForStateChange() throws InterruptedException {
            return mReceiveLatch.await(30, TimeUnit.SECONDS) || hasExpectedState();
        }

        private boolean hasExpectedState() {
            return mExpectedState == mConnectivityManager.getNetworkInfo(mNetworkType).getState();
        }
    }
}
