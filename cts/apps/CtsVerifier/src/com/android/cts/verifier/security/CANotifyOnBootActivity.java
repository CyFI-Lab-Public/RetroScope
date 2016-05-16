package com.android.cts.verifier.security;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CANotifyOnBootActivity extends PassFailButtons.Activity {

    private static final String TAG = CANotifyOnBootActivity.class.getSimpleName();
    private static final String CERT_ASSET_NAME = "myCA.cer";
    private File certStagingFile = new File("/sdcard/", CERT_ASSET_NAME);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = getLayoutInflater().inflate(R.layout.ca_boot_notify, null);
        Button checkCredsButton = (Button) view.findViewById(R.id.check_creds);
        Button installButton = (Button) view.findViewById(R.id.install);
        checkCredsButton.setOnClickListener(new OpenTrustedCredentials());
        installButton.setOnClickListener(new InstallCert());

        setContentView(view);

        setPassFailButtonClickListeners();
        setInfoResources(R.string.caboot_test, R.string.caboot_info, -1);

        getPassButton().setEnabled(true);
    }

    class OpenTrustedCredentials implements OnClickListener {
        @Override
        public void onClick(View v) {
            try {
                startActivity(new Intent("com.android.settings.TRUSTED_CREDENTIALS_USER"));
            } catch (ActivityNotFoundException e) {
                // do nothing
            }
        }
    }

    class InstallCert implements OnClickListener {
        @Override
        public void onClick(View v) {
            InputStream is = null;
            FileOutputStream os = null;
            try {
                try {
                    is = getAssets().open(CERT_ASSET_NAME);
                    os = new FileOutputStream(certStagingFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        os.write(buffer, 0, length);
                    }
                } finally {
                    if (is != null) is.close();
                    if (os != null) os.close();
                    certStagingFile.setReadable(true, false);
                }
            } catch (IOException ioe) {
                Log.w(TAG, "Problem moving cert file to /sdcard/", ioe);
                return;
            }

            try {
                startActivity(new Intent("android.credentials.INSTALL"));
            } catch (ActivityNotFoundException e) {
                // do nothing
            }
        }
    }


}
