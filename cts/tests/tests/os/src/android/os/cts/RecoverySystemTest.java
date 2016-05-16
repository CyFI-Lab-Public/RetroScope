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

package android.os.cts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.RecoverySystem;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import android.util.Log;

public class RecoverySystemTest extends AndroidTestCase {
    private static final String TAG = "RecoverySystemTest";

    private AssetManager mAssets;

    @Override
    protected void setUp() throws Exception {
        Log.v(TAG, "setup");
        super.setUp();
        mAssets = mContext.getAssets();
    }

    /** Write the given asset to a file of the same name and return a File. */
    private File getAsset(String name) throws Exception {
        FileOutputStream fos = mContext.openFileOutput(name, 0);
        InputStream is = mAssets.open(name);
        byte[] b = new byte[4096];
        int read;
        while ((read = is.read(b)) != -1) {
            fos.write(b, 0, read);
        }
        is.close();
        fos.close();
        return mContext.getFileStreamPath(name);
    }

    @MediumTest
    public void testVerify() throws Exception {
        File otacerts = getAsset("otacerts.zip");
        File packageFile;

        // This is the only package for which verification should succeed.
        Log.v(TAG, "testing otasigned.zip");
        packageFile = getAsset("otasigned.zip");
        RecoverySystem.verifyPackage(packageFile, null, otacerts);
        packageFile.delete();

        expectVerifyFail("alter-footer.zip", otacerts);
        expectVerifyFail("alter-metadata.zip", otacerts);
        expectVerifyFail("fake-eocd.zip", otacerts);
        expectVerifyFail("jarsigned.zip", otacerts);
        expectVerifyFail("random.zip", otacerts);
        expectVerifyFail("unsigned.zip", otacerts);

        otacerts.delete();
    }

    /**
     * Try verifying the given file against the given otacerts,
     * expecting verification to fail.
     */
    private void expectVerifyFail(String name, File otacerts)
        throws Exception {
        Log.v(TAG, "testing " + name);
        File packageFile = getAsset(name);
        try {
            RecoverySystem.verifyPackage(packageFile, null, otacerts);
            fail("verification of " + name + " succeeded when it shouldn't have");
        } catch (GeneralSecurityException e) {
            // expected
        }
        packageFile.delete();
    }
}
