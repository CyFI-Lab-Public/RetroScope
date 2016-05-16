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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.security.Credentials;
import android.security.KeyChain;
import android.security.KeyChain.KeyChainConnection;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Installs certificates to the system keystore.
 */
public class CertInstaller extends Activity {
    private static final String TAG = "CertInstaller";

    private static final int STATE_INIT = 1;
    private static final int STATE_RUNNING = 2;
    private static final int STATE_PAUSED = 3;

    private static final int NAME_CREDENTIAL_DIALOG = 1;
    private static final int PKCS12_PASSWORD_DIALOG = 2;
    private static final int PROGRESS_BAR_DIALOG = 3;

    private static final int REQUEST_SYSTEM_INSTALL_CODE = 1;

    // key to states Bundle
    private static final String NEXT_ACTION_KEY = "na";

    // key to KeyStore
    private static final String PKEY_MAP_KEY = "PKEY_MAP";

    // Values for usage type spinner
    private static final int USAGE_TYPE_SYSTEM = 0;
    private static final int USAGE_TYPE_WIFI = 1;

    private final KeyStore mKeyStore = KeyStore.getInstance();
    private final ViewHelper mView = new ViewHelper();

    private int mState;
    private CredentialHelper mCredentials;
    private MyAction mNextAction;

    private CredentialHelper createCredentialHelper(Intent intent) {
        try {
            return new CredentialHelper(intent);
        } catch (Throwable t) {
            Log.w(TAG, "createCredentialHelper", t);
            toastErrorAndFinish(R.string.invalid_cert);
            return new CredentialHelper();
        }
    }

    @Override
    protected void onCreate(Bundle savedStates) {
        super.onCreate(savedStates);

        mCredentials = createCredentialHelper(getIntent());

        mState = (savedStates == null) ? STATE_INIT : STATE_RUNNING;

        if (mState == STATE_INIT) {
            if (!mCredentials.containsAnyRawData()) {
                toastErrorAndFinish(R.string.no_cert_to_saved);
                finish();
            } else if (mCredentials.hasPkcs12KeyStore()) {
                showDialog(PKCS12_PASSWORD_DIALOG);
            } else {
                MyAction action = new InstallOthersAction();
                if (needsKeyStoreAccess()) {
                    sendUnlockKeyStoreIntent();
                    mNextAction = action;
                } else {
                    action.run(this);
                }
            }
        } else {
            mCredentials.onRestoreStates(savedStates);
            mNextAction = (MyAction)
                    savedStates.getSerializable(NEXT_ACTION_KEY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mState == STATE_INIT) {
            mState = STATE_RUNNING;
        } else {
            if (mNextAction != null) {
                mNextAction.run(this);
            }
        }
    }

    private boolean needsKeyStoreAccess() {
        return ((mCredentials.hasKeyPair() || mCredentials.hasUserCertificate())
                && !mKeyStore.isUnlocked());
    }

    @Override
    protected void onPause() {
        super.onPause();
        mState = STATE_PAUSED;
    }

    @Override
    protected void onSaveInstanceState(Bundle outStates) {
        super.onSaveInstanceState(outStates);
        mCredentials.onSaveStates(outStates);
        if (mNextAction != null) {
            outStates.putSerializable(NEXT_ACTION_KEY, mNextAction);
        }
    }

    @Override
    protected Dialog onCreateDialog (int dialogId) {
        switch (dialogId) {
            case PKCS12_PASSWORD_DIALOG:
                return createPkcs12PasswordDialog();

            case NAME_CREDENTIAL_DIALOG:
                return createNameCredentialDialog();

            case PROGRESS_BAR_DIALOG:
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setMessage(getString(R.string.extracting_pkcs12));
                dialog.setIndeterminate(true);
                dialog.setCancelable(false);
                return dialog;

            default:
                return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SYSTEM_INSTALL_CODE) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "credential is added: " + mCredentials.getName());
                Toast.makeText(this, getString(R.string.cert_is_added,
                        mCredentials.getName()), Toast.LENGTH_LONG).show();

                if (mCredentials.hasCaCerts()) {
                    // more work to do, don't finish just yet
                    new InstallCaCertsToKeyChainTask().execute();
                    return;
                }
                setResult(RESULT_OK);
            } else {
                Log.d(TAG, "credential not saved, err: " + resultCode);
                toastErrorAndFinish(R.string.cert_not_saved);
            }
        } else {
            Log.w(TAG, "unknown request code: " + requestCode);
        }
        finish();
    }

    private class InstallCaCertsToKeyChainTask extends AsyncTask<Void, Void, Boolean> {

        @Override protected Boolean doInBackground(Void... unused) {
            try {
                KeyChainConnection keyChainConnection = KeyChain.bind(CertInstaller.this);
                try {
                    return mCredentials.installCaCertsToKeyChain(keyChainConnection.getService());
                } finally {
                    keyChainConnection.close();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override protected void onPostExecute(Boolean success) {
            if (success) {
                setResult(RESULT_OK);
            }
            finish();
        }
    }

    void installOthers() {
        if (mCredentials.hasKeyPair()) {
            saveKeyPair();
            finish();
        } else {
            X509Certificate cert = mCredentials.getUserCertificate();
            if (cert != null) {
                // find matched private key
                String key = Util.toMd5(cert.getPublicKey().getEncoded());
                Map<String, byte[]> map = getPkeyMap();
                byte[] privatekey = map.get(key);
                if (privatekey != null) {
                    Log.d(TAG, "found matched key: " + privatekey);
                    map.remove(key);
                    savePkeyMap(map);

                    mCredentials.setPrivateKey(privatekey);
                } else {
                    Log.d(TAG, "didn't find matched private key: " + key);
                }
            }
            nameCredential();
        }
    }

    private void sendUnlockKeyStoreIntent() {
        Credentials.getInstance().unlock(this);
    }

    private void nameCredential() {
        if (!mCredentials.hasAnyForSystemInstall()) {
            toastErrorAndFinish(R.string.no_cert_to_saved);
        } else {
            showDialog(NAME_CREDENTIAL_DIALOG);
        }
    }

    private void saveKeyPair() {
        byte[] privatekey = mCredentials.getData(Credentials.EXTRA_PRIVATE_KEY);
        String key = Util.toMd5(mCredentials.getData(Credentials.EXTRA_PUBLIC_KEY));
        Map<String, byte[]> map = getPkeyMap();
        map.put(key, privatekey);
        savePkeyMap(map);
        Log.d(TAG, "save privatekey: " + key + " --> #keys:" + map.size());
    }

    private void savePkeyMap(Map<String, byte[]> map) {
        if (map.isEmpty()) {
            if (!mKeyStore.delete(PKEY_MAP_KEY)) {
                Log.w(TAG, "savePkeyMap(): failed to delete pkey map");
            }
            return;
        }
        byte[] bytes = Util.toBytes(map);
        if (!mKeyStore.put(PKEY_MAP_KEY, bytes, KeyStore.UID_SELF, KeyStore.FLAG_ENCRYPTED)) {
            Log.w(TAG, "savePkeyMap(): failed to write pkey map");
        }
    }

    private Map<String, byte[]> getPkeyMap() {
        byte[] bytes = mKeyStore.get(PKEY_MAP_KEY);
        if (bytes != null) {
            Map<String, byte[]> map =
                    (Map<String, byte[]>) Util.fromBytes(bytes);
            if (map != null) return map;
        }
        return new MyMap();
    }

    void extractPkcs12InBackground(final String password) {
        // show progress bar and extract certs in a background thread
        showDialog(PROGRESS_BAR_DIALOG);

        new AsyncTask<Void,Void,Boolean>() {
            @Override protected Boolean doInBackground(Void... unused) {
                return mCredentials.extractPkcs12(password);
            }
            @Override protected void onPostExecute(Boolean success) {
                MyAction action = new OnExtractionDoneAction(success);
                if (mState == STATE_PAUSED) {
                    // activity is paused; run it in next onResume()
                    mNextAction = action;
                } else {
                    action.run(CertInstaller.this);
                }
            }
        }.execute();
    }

    void onExtractionDone(boolean success) {
        mNextAction = null;
        removeDialog(PROGRESS_BAR_DIALOG);
        if (success) {
            removeDialog(PKCS12_PASSWORD_DIALOG);
            nameCredential();
        } else {
            mView.setText(R.id.credential_password, "");
            mView.showError(R.string.password_error);
            showDialog(PKCS12_PASSWORD_DIALOG);
        }
    }

    private Dialog createPkcs12PasswordDialog() {
        View view = View.inflate(this, R.layout.password_dialog, null);
        mView.setView(view);
        if (mView.getHasEmptyError()) {
            mView.showError(R.string.password_empty_error);
            mView.setHasEmptyError(false);
        }

        String title = mCredentials.getName();
        title = TextUtils.isEmpty(title)
                ? getString(R.string.pkcs12_password_dialog_title)
                : getString(R.string.pkcs12_file_password_dialog_title, title);
        Dialog d = new AlertDialog.Builder(this)
                .setView(view)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String password = mView.getText(R.id.credential_password);
                        mNextAction = new Pkcs12ExtractAction(password);
                        mNextAction.run(CertInstaller.this);
                     }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        toastErrorAndFinish(R.string.cert_not_saved);
                    }
                })
                .create();
        d.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override public void onCancel(DialogInterface dialog) {
                toastErrorAndFinish(R.string.cert_not_saved);
            }
        });
        return d;
    }

    private Dialog createNameCredentialDialog() {
        ViewGroup view = (ViewGroup) View.inflate(this, R.layout.name_credential_dialog, null);
        mView.setView(view);
        if (mView.getHasEmptyError()) {
            mView.showError(R.string.name_empty_error);
            mView.setHasEmptyError(false);
        }
        mView.setText(R.id.credential_info, mCredentials.getDescription(this).toString());
        final EditText nameInput = (EditText) view.findViewById(R.id.credential_name);
        if (mCredentials.isInstallAsUidSet()) {
            view.findViewById(R.id.credential_usage_group).setVisibility(View.GONE);
        } else {
            final Spinner usageSpinner = (Spinner) view.findViewById(R.id.credential_usage);

            usageSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    switch ((int) id) {
                        case USAGE_TYPE_SYSTEM:
                            mCredentials.setInstallAsUid(KeyStore.UID_SELF);
                            break;
                        case USAGE_TYPE_WIFI:
                            mCredentials.setInstallAsUid(Process.WIFI_UID);
                            break;
                        default:
                            Log.w(TAG, "Unknown selection for scope: " + id);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
        nameInput.setText(getDefaultName());
        nameInput.selectAll();
        Dialog d = new AlertDialog.Builder(this)
                .setView(view)
                .setTitle(R.string.name_credential_dialog_title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String name = mView.getText(R.id.credential_name);
                        if (TextUtils.isEmpty(name)) {
                            mView.setHasEmptyError(true);
                            removeDialog(NAME_CREDENTIAL_DIALOG);
                            showDialog(NAME_CREDENTIAL_DIALOG);
                        } else {
                            removeDialog(NAME_CREDENTIAL_DIALOG);
                            mCredentials.setName(name);

                            // install everything to system keystore
                            try {
                                startActivityForResult(
                                        mCredentials.createSystemInstallIntent(),
                                        REQUEST_SYSTEM_INSTALL_CODE);
                            } catch (ActivityNotFoundException e) {
                                Log.w(TAG, "systemInstall(): " + e);
                                toastErrorAndFinish(R.string.cert_not_saved);
                            }
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        toastErrorAndFinish(R.string.cert_not_saved);
                    }
                })
                .create();
        d.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override public void onCancel(DialogInterface dialog) {
                toastErrorAndFinish(R.string.cert_not_saved);
            }
        });
        return d;
    }

    private String getDefaultName() {
        String name = mCredentials.getName();
        if (TextUtils.isEmpty(name)) {
            return null;
        } else {
            // remove the extension from the file name
            int index = name.lastIndexOf(".");
            if (index > 0) name = name.substring(0, index);
            return name;
        }
    }

    private void toastErrorAndFinish(int msgId) {
        Toast.makeText(this, msgId, Toast.LENGTH_SHORT).show();
        finish();
    }

    private static class MyMap extends LinkedHashMap<String, byte[]>
            implements Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            // Note: one key takes about 1300 bytes in the keystore, so be
            // cautious about allowing more outstanding keys in the map that
            // may go beyond keystore's max length for one entry.
            return (size() > 3);
        }
    }

    private interface MyAction extends Serializable {
        void run(CertInstaller host);
    }

    private static class Pkcs12ExtractAction implements MyAction {
        private final String mPassword;
        private transient boolean hasRun;

        Pkcs12ExtractAction(String password) {
            mPassword = password;
        }

        public void run(CertInstaller host) {
            if (hasRun) {
                return;
            }
            hasRun = true;
            host.extractPkcs12InBackground(mPassword);
        }
    }

    private static class InstallOthersAction implements MyAction {
        public void run(CertInstaller host) {
            host.mNextAction = null;
            host.installOthers();
        }
    }

    private static class OnExtractionDoneAction implements MyAction {
        private final boolean mSuccess;

        OnExtractionDoneAction(boolean success) {
            mSuccess = success;
        }

        public void run(CertInstaller host) {
            host.onExtractionDone(mSuccess);
        }
    }
}
