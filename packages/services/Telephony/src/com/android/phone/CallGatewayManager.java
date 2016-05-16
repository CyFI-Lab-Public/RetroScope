/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.phone;

import android.content.Intent;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.Connection;
import com.google.android.collect.Maps;

import java.util.HashMap;

/**
 * This class manages gateway information for outgoing calls. When calls are made, they may contain
 * gateway information for services which route phone calls through their own service/numbers.
 * The data consists of a number to call and the package name of the service. This data is used in
 * two ways:<br/>
 * 1. Call the appropriate routing number<br/>
 * 2. Display information about the routing to the user<br/>
 *
 * <p>When an outgoing call is finally placed in PhoneUtils.placeCall, it uses this class to get the
 * proper number to dial. It also saves an association between the connection object and the gateway
 * data into this class.  This association is later used in CallModeler when building Call objects
 * to send to the UI which require the gateway data to show an alert to users.
 */
public class CallGatewayManager {
    private static final String LOG_TAG = CallGatewayManager.class.getSimpleName();

    /**
     * Intent extra to specify the package name of the gateway
     * provider.  Used to get the name displayed in the in-call screen
     * during the call setup. The value is a string.
     */
    // TODO: This extra is currently set by the gateway application as
    // a temporary measure. Ultimately, the framework will securely
    // set it.
    /* package */ static final String EXTRA_GATEWAY_PROVIDER_PACKAGE =
            "com.android.phone.extra.GATEWAY_PROVIDER_PACKAGE";

    /**
     * Intent extra to specify the URI of the provider to place the
     * call. The value is a string. It holds the gateway address
     * (phone gateway URL should start with the 'tel:' scheme) that
     * will actually be contacted to call the number passed in the
     * intent URL or in the EXTRA_PHONE_NUMBER extra.
     */
    // TODO: Should the value be a Uri (Parcelable)? Need to make sure
    // MMI code '#' don't get confused as URI fragments.
    /* package */ static final String EXTRA_GATEWAY_URI =
            "com.android.phone.extra.GATEWAY_URI";

    public static final RawGatewayInfo EMPTY_INFO = new RawGatewayInfo(null, null, null);

    private final HashMap<Connection, RawGatewayInfo> mMap = Maps.newHashMap();

    public CallGatewayManager() {
    }

    /**
     * Static method returns an object containing the gateway data stored in the extras of the
     * Intent parameter.  If no such data exists, returns a Null-Object RawGatewayInfo.
     * @param intent The intent from which to read gateway data.
     * @return A populated or empty RawGatewayInfo object.
     */
    public static RawGatewayInfo getRawGatewayInfo(Intent intent, String number) {
        if (hasPhoneProviderExtras(intent)) {
            return new RawGatewayInfo(intent.getStringExtra(EXTRA_GATEWAY_PROVIDER_PACKAGE),
                    getProviderGatewayUri(intent), number);
        }
        return EMPTY_INFO;
    }

    /**
     * This function sets the current mapping from connection to gatewayInfo so that CallModeler
     * can request this data when creating Call objects.
     * @param connection The connection object for the placed outgoing call.
     * @param gatewayInfo Gateway info gathered using getRawGatewayInfo.
     */
    public void setGatewayInfoForConnection(Connection connection, RawGatewayInfo gatewayInfo) {
        if (!gatewayInfo.isEmpty()) {
            mMap.put(connection, gatewayInfo);
        } else {
            mMap.remove(connection);
        }
    }

    /**
     * Clears the gateway information previously stored via setGatewayInfoForConnection.
     */
    public void clearGatewayData(Connection connection) {
        setGatewayInfoForConnection(connection, EMPTY_INFO);
    }

    /**
     * If the parameter matches the connection object we previously saved through
     * setGatewayInfoForConnection, return the associated raw gateway info data. If not, then
     * return an empty raw gateway info.
     */
    public RawGatewayInfo getGatewayInfo(Connection connection) {
        final RawGatewayInfo info = mMap.get(connection);
        if (info != null) {
            return info;
        }

        return EMPTY_INFO;
    }

    /**
     * Check if all the provider's info is present in the intent.
     * @param intent Expected to have the provider's extra.
     * @return true if the intent has all the extras to build the
     * in-call screen's provider info overlay.
     */
    public static boolean hasPhoneProviderExtras(Intent intent) {
        if (null == intent) {
            return false;
        }
        final String name = intent.getStringExtra(EXTRA_GATEWAY_PROVIDER_PACKAGE);
        final String gatewayUri = intent.getStringExtra(EXTRA_GATEWAY_URI);

        return !TextUtils.isEmpty(name) && !TextUtils.isEmpty(gatewayUri);
    }

    /**
     * Copy all the expected extras set when a 3rd party provider is
     * used from the source intent to the destination one.  Checks all
     * the required extras are present, if any is missing, none will
     * be copied.
     * @param src Intent which may contain the provider's extras.
     * @param dst Intent where a copy of the extras will be added if applicable.
     */
    public static void checkAndCopyPhoneProviderExtras(Intent src, Intent dst) {
        if (!hasPhoneProviderExtras(src)) {
            Log.d(LOG_TAG, "checkAndCopyPhoneProviderExtras: some or all extras are missing.");
            return;
        }

        dst.putExtra(EXTRA_GATEWAY_PROVIDER_PACKAGE,
                     src.getStringExtra(EXTRA_GATEWAY_PROVIDER_PACKAGE));
        dst.putExtra(EXTRA_GATEWAY_URI,
                     src.getStringExtra(EXTRA_GATEWAY_URI));
    }

    /**
     * Return the gateway uri from the intent.
     * @param intent With the gateway uri extra.
     * @return The gateway URI or null if not found.
     */
    public static Uri getProviderGatewayUri(Intent intent) {
        final String uri = intent.getStringExtra(EXTRA_GATEWAY_URI);
        return TextUtils.isEmpty(uri) ? null : Uri.parse(uri);
    }

    /**
     * Return a formatted version of the uri's scheme specific
     * part. E.g for 'tel:12345678', return '1-234-5678'.
     * @param uri A 'tel:' URI with the gateway phone number.
     * @return the provider's address (from the gateway uri) formatted
     * for user display. null if uri was null or its scheme was not 'tel:'.
     */
    public static String formatProviderUri(Uri uri) {
        if (uri != null) {
            if (Constants.SCHEME_TEL.equals(uri.getScheme())) {
                return PhoneNumberUtils.formatNumber(uri.getSchemeSpecificPart());
            } else {
                return uri.toString();
            }
        }
        return null;
    }

    public static class RawGatewayInfo {
        public String packageName;
        public Uri gatewayUri;
        public String trueNumber;

        public RawGatewayInfo(String packageName, Uri gatewayUri,
                String trueNumber) {
            this.packageName = packageName;
            this.gatewayUri = gatewayUri;
            this.trueNumber = trueNumber;
        }

        public String getFormattedGatewayNumber() {
            return formatProviderUri(gatewayUri);
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(packageName) || gatewayUri == null;
        }
    }
}
