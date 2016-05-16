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

package android.app.backup.cts;

import android.app.backup.BackupManager;
import android.app.backup.RestoreObserver;
import android.test.AndroidTestCase;

public class BackupManagerTest extends AndroidTestCase {

    public void testBackupManager() throws Exception {
        // Check that these don't crash as if they were called in an app...
        BackupManager backupManager = new BackupManager(mContext);
        backupManager.dataChanged();
        BackupManager.dataChanged("com.android.cts.stub");

        // Backup isn't expected to work in this test but check for obvious bugs...
        int result = backupManager.requestRestore(new RestoreObserver() {});
        assertTrue(result != 0);
    }
}
