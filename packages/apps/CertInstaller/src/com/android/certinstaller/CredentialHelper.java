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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.security.Credentials;
import android.security.KeyChain;
import android.security.IKeyChainService;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.DEROctetString;
import com.android.org.bouncycastle.asn1.x509.BasicConstraints;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

/**
 * A helper class for accessing the raw data in the intent extra and handling
 * certificates.
 */
class CredentialHelper {
    private static final String DATA_KEY = "data";
    private static final String CERTS_KEY = "crts";

    private static final String TAG = "CredentialHelper";

    // keep raw data from intent's extra
    private HashMap<String, byte[]> mBundle = new HashMap<String, byte[]>();

    private String mName = "";
    private int mUid = -1;
    private PrivateKey mUserKey;
    private X509Certificate mUserCert;
    private List<X509Certificate> mCaCerts = new ArrayList<X509Certificate>();

    CredentialHelper() {
    }

    CredentialHelper(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        String name = bundle.getString(KeyChain.EXTRA_NAME);
        bundle.remove(KeyChain.EXTRA_NAME);
        if (name != null) {
            mName = name;
        }

        mUid = bundle.getInt(Credentials.EXTRA_INSTALL_AS_UID, -1);
        bundle.remove(Credentials.EXTRA_INSTALL_AS_UID);

        Log.d(TAG, "# extras: " + bundle.size());
        for (String key : bundle.keySet()) {
            byte[] bytes = bundle.getByteArray(key);
            Log.d(TAG, "   " + key + ": " + ((bytes == null) ? -1 : bytes.length));
            mBundle.put(key, bytes);
        }
        parseCert(getData(KeyChain.EXTRA_CERTIFICATE));
    }

    synchronized void onSaveStates(Bundle outStates) {
        try {
            outStates.putSerializable(DATA_KEY, mBundle);
            outStates.putString(KeyChain.EXTRA_NAME, mName);
            if (mUserKey != null) {
                outStates.putByteArray(Credentials.USER_PRIVATE_KEY,
                        mUserKey.getEncoded());
            }
            ArrayList<byte[]> certs = new ArrayList<byte[]>(mCaCerts.size() + 1);
            if (mUserCert != null) {
                certs.add(mUserCert.getEncoded());
            }
            for (X509Certificate cert : mCaCerts) {
                certs.add(cert.getEncoded());
            }
            outStates.putByteArray(CERTS_KEY, Util.toBytes(certs));
        } catch (CertificateEncodingException e) {
            throw new AssertionError(e);
        }
    }

    void onRestoreStates(Bundle savedStates) {
        mBundle = (HashMap) savedStates.getSerializable(DATA_KEY);
        mName = savedStates.getString(KeyChain.EXTRA_NAME);
        byte[] bytes = savedStates.getByteArray(Credentials.USER_PRIVATE_KEY);
        if (bytes != null) {
            setPrivateKey(bytes);
        }

        ArrayList<byte[]> certs = Util.fromBytes(savedStates.getByteArray(CERTS_KEY));
        for (byte[] cert : certs) {
            parseCert(cert);
        }
    }

    X509Certificate getUserCertificate() {
        return mUserCert;
    }

    private void parseCert(byte[] bytes) {
        if (bytes == null) {
            return;
        }

        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate)
                    certFactory.generateCertificate(
                            new ByteArrayInputStream(bytes));
            if (isCa(cert)) {
                Log.d(TAG, "got a CA cert");
                mCaCerts.add(cert);
            } else {
                Log.d(TAG, "got a user cert");
                mUserCert = cert;
            }
        } catch (CertificateException e) {
            Log.w(TAG, "parseCert(): " + e);
        }
    }

    private boolean isCa(X509Certificate cert) {
        try {
            // TODO: add a test about this
            byte[] asn1EncodedBytes = cert.getExtensionValue("2.5.29.19");
            if (asn1EncodedBytes == null) {
                return false;
            }
            DEROctetString derOctetString = (DEROctetString)
                    new ASN1InputStream(asn1EncodedBytes).readObject();
            byte[] octets = derOctetString.getOctets();
            ASN1Sequence sequence = (ASN1Sequence)
                    new ASN1InputStream(octets).readObject();
            return BasicConstraints.getInstance(sequence).isCA();
        } catch (IOException e) {
            return false;
        }
    }

    boolean hasPkcs12KeyStore() {
        return mBundle.containsKey(KeyChain.EXTRA_PKCS12);
    }

    boolean hasKeyPair() {
        return mBundle.containsKey(Credentials.EXTRA_PUBLIC_KEY)
                && mBundle.containsKey(Credentials.EXTRA_PRIVATE_KEY);
    }

    boolean hasUserCertificate() {
        return (mUserCert != null);
    }

    boolean hasCaCerts() {
        return !mCaCerts.isEmpty();
    }

    boolean hasAnyForSystemInstall() {
        return (mUserKey != null) || hasUserCertificate() || hasCaCerts();
    }

    void setPrivateKey(byte[] bytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            mUserKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        } catch (InvalidKeySpecException e) {
            throw new AssertionError(e);
        }
    }

    boolean containsAnyRawData() {
        return !mBundle.isEmpty();
    }

    byte[] getData(String key) {
        return mBundle.get(key);
    }

    void putPkcs12Data(byte[] data) {
        mBundle.put(KeyChain.EXTRA_PKCS12, data);
    }

    CharSequence getDescription(Context context) {
        // TODO: create more descriptive string
        StringBuilder sb = new StringBuilder();
        String newline = "<br>";
        if (mUserKey != null) {
            sb.append(context.getString(R.string.one_userkey)).append(newline);
        }
        if (mUserCert != null) {
            sb.append(context.getString(R.string.one_usercrt)).append(newline);
        }
        int n = mCaCerts.size();
        if (n > 0) {
            if (n == 1) {
                sb.append(context.getString(R.string.one_cacrt));
            } else {
                sb.append(context.getString(R.string.n_cacrts, n));
            }
        }
        return Html.fromHtml(sb.toString());
    }

    void setName(String name) {
        mName = name;
    }

    String getName() {
        return mName;
    }

    void setInstallAsUid(int uid) {
        mUid = uid;
    }

    boolean isInstallAsUidSet() {
        return mUid != -1;
    }

    Intent createSystemInstallIntent() {
        Intent intent = new Intent("com.android.credentials.INSTALL");
        // To prevent the private key from being sniffed, we explicitly spell
        // out the intent receiver class.
        intent.setClassName("com.android.settings", "com.android.settings.CredentialStorage");
        intent.putExtra(Credentials.EXTRA_INSTALL_AS_UID, mUid);
        try {
            if (mUserKey != null) {
                intent.putExtra(Credentials.EXTRA_USER_PRIVATE_KEY_NAME,
                        Credentials.USER_PRIVATE_KEY + mName);
                intent.putExtra(Credentials.EXTRA_USER_PRIVATE_KEY_DATA,
                        mUserKey.getEncoded());
            }
            if (mUserCert != null) {
                intent.putExtra(Credentials.EXTRA_USER_CERTIFICATE_NAME,
                        Credentials.USER_CERTIFICATE + mName);
                intent.putExtra(Credentials.EXTRA_USER_CERTIFICATE_DATA,
                        Credentials.convertToPem(mUserCert));
            }
            if (!mCaCerts.isEmpty()) {
                intent.putExtra(Credentials.EXTRA_CA_CERTIFICATES_NAME,
                        Credentials.CA_CERTIFICATE + mName);
                X509Certificate[] caCerts
                        = mCaCerts.toArray(new X509Certificate[mCaCerts.size()]);
                intent.putExtra(Credentials.EXTRA_CA_CERTIFICATES_DATA,
                        Credentials.convertToPem(caCerts));
            }
            return intent;
        } catch (IOException e) {
            throw new AssertionError(e);
        } catch (CertificateEncodingException e) {
            throw new AssertionError(e);
        }
    }

    boolean installCaCertsToKeyChain(IKeyChainService keyChainService) {
        for (X509Certificate caCert : mCaCerts) {
            byte[] bytes = null;
            try {
                bytes = caCert.getEncoded();
            } catch (CertificateEncodingException e) {
                throw new AssertionError(e);
            }
            if (bytes != null) {
                try {
                    keyChainService.installCaCertificate(bytes);
                } catch (RemoteException e) {
                    Log.w(TAG, "installCaCertsToKeyChain(): " + e);
                    return false;
                }
            }
        }
        return true;
    }

    boolean extractPkcs12(String password) {
        try {
            return extractPkcs12Internal(password);
        } catch (Exception e) {
            Log.w(TAG, "extractPkcs12(): " + e, e);
            return false;
        }
    }

    private boolean extractPkcs12Internal(String password)
            throws Exception {
        // TODO: add test about this
        java.security.KeyStore keystore = java.security.KeyStore.getInstance("PKCS12");
        PasswordProtection passwordProtection = new PasswordProtection(password.toCharArray());
        keystore.load(new ByteArrayInputStream(getData(KeyChain.EXTRA_PKCS12)),
                      passwordProtection.getPassword());

        Enumeration<String> aliases = keystore.aliases();
        if (!aliases.hasMoreElements()) {
            return false;
        }

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            KeyStore.Entry entry = keystore.getEntry(alias, passwordProtection);
            Log.d(TAG, "extracted alias = " + alias + ", entry=" + entry.getClass());

            if (entry instanceof PrivateKeyEntry) {
                if (TextUtils.isEmpty(mName)) {
                    mName = alias;
                }
                return installFrom((PrivateKeyEntry) entry);
            }
        }
        return true;
    }

    private synchronized boolean installFrom(PrivateKeyEntry entry) {
        mUserKey = entry.getPrivateKey();
        mUserCert = (X509Certificate) entry.getCertificate();

        Certificate[] certs = entry.getCertificateChain();
        Log.d(TAG, "# certs extracted = " + certs.length);
        mCaCerts = new ArrayList<X509Certificate>(certs.length);
        for (Certificate c : certs) {
            X509Certificate cert = (X509Certificate) c;
            if (isCa(cert)) {
                mCaCerts.add(cert);
            }
        }
        Log.d(TAG, "# ca certs extracted = " + mCaCerts.size());

        return true;
    }
}
