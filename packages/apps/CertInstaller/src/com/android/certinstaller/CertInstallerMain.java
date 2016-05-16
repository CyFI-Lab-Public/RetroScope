/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.certinstaller;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.provider.DocumentsContract;
import android.security.Credentials;
import android.security.KeyChain;
import android.util.Log;
import android.widget.Toast;

import libcore.io.IoUtils;
import libcore.io.Streams;

import java.io.IOException;
import java.io.InputStream;

/**
 * The main class for installing certificates to the system keystore. It reacts
 * to the public {@link Credentials#INSTALL_ACTION} intent.
 */
public class CertInstallerMain extends PreferenceActivity {
    private static final String TAG = "CertInstaller";

    private static final int REQUEST_INSTALL = 1;
    private static final int REQUEST_OPEN_DOCUMENT = 2;

    private static final String[] ACCEPT_MIME_TYPES = {
            "application/x-pkcs12",
            "application/x-x509-ca-cert",
            "application/x-x509-user-cert",
            "application/x-x509-server-cert",
            "application/x-pem-file",
            "application/pkix-cert"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Credentials.INSTALL_ACTION.equals(action)
                || Credentials.INSTALL_AS_USER_ACTION.equals(action)) {
            Bundle bundle = intent.getExtras();

            /*
             * There is a special INSTALL_AS_USER action that this activity is
             * aliased to, but you have to have a permission to call it. If the
             * caller got here any other way, remove the extra that we allow in
             * that INSTALL_AS_USER path.
             */
            if (bundle != null && !Credentials.INSTALL_AS_USER_ACTION.equals(action)) {
                bundle.remove(Credentials.EXTRA_INSTALL_AS_UID);
            }

            // If bundle is empty of any actual credentials, ask user to open.
            // Otherwise, pass extras to CertInstaller to install those credentials.
            // Either way, we use KeyChain.EXTRA_NAME as the default name if available.
            if (bundle == null
                    || bundle.isEmpty()
                    || (bundle.size() == 1
                        && (bundle.containsKey(KeyChain.EXTRA_NAME)
                            || bundle.containsKey(Credentials.EXTRA_INSTALL_AS_UID)))) {
                final Intent openIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                openIntent.setType("*/*");
                openIntent.putExtra(Intent.EXTRA_MIME_TYPES, ACCEPT_MIME_TYPES);
                openIntent.putExtra(DocumentsContract.EXTRA_SHOW_ADVANCED, true);
                startActivityForResult(openIntent, REQUEST_OPEN_DOCUMENT);
            } else {
                final Intent installIntent = new Intent(this, CertInstaller.class);
                installIntent.putExtras(intent);
                startActivityForResult(installIntent, REQUEST_INSTALL);
            }
        } else if (Intent.ACTION_VIEW.equals(action)) {
            startInstallActivity(intent.getType(), intent.getData());
        }
    }

    private void startInstallActivity(String mimeType, Uri uri) {
        if (mimeType == null) {
            mimeType = getContentResolver().getType(uri);
        }

        InputStream in = null;
        try {
            in = getContentResolver().openInputStream(uri);

            final byte[] raw = Streams.readFully(in);
            startInstallActivity(mimeType, raw);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read certificate: " + e);
            Toast.makeText(this, R.string.cert_read_error, Toast.LENGTH_LONG).show();
        } finally {
            IoUtils.closeQuietly(in);
        }
    }

    private void startInstallActivity(String mimeType, byte[] value) {
        Intent intent = new Intent(this, CertInstaller.class);
        if ("application/x-pkcs12".equals(mimeType)) {
            intent.putExtra(KeyChain.EXTRA_PKCS12, value);
        } else if ("application/x-x509-ca-cert".equals(mimeType)
                || "application/x-x509-user-cert".equals(mimeType)
                || "application/x-x509-server-cert".equals(mimeType)
                || "application/x-pem-file".equals(mimeType)
                || "application/pkix-cert".equals(mimeType)) {
            intent.putExtra(KeyChain.EXTRA_CERTIFICATE, value);
        } else {
            throw new IllegalArgumentException("Unknown MIME type: " + mimeType);
        }

        startActivityForResult(intent, REQUEST_INSTALL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OPEN_DOCUMENT) {
            if (resultCode == RESULT_OK) {
                startInstallActivity(null, data.getData());
            } else {
                finish();
            }
        } else if (requestCode == REQUEST_INSTALL) {
            setResult(resultCode);
            finish();
        } else {
            Log.w(TAG, "unknown request code: " + requestCode);
        }
    }
}
