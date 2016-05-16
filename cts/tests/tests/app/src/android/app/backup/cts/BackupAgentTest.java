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

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.os.ParcelFileDescriptor;
import android.test.AndroidTestCase;

import java.io.IOException;

public class BackupAgentTest extends AndroidTestCase {

    public void testBackupAgent() {
        BackupAgent agent = new TestBackupAgent();
        agent.onCreate();
        agent.onDestroy();
    }

    class TestBackupAgent extends BackupAgent {

        @Override
        public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                ParcelFileDescriptor newState) throws IOException {
        }

        @Override
        public void onRestore(BackupDataInput data, int appVersionCode,
                ParcelFileDescriptor newState) throws IOException {
        }
    }
}
