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

package com.android.cts.verifier;

import com.android.cts.verifier.backup.BackupTestActivity;

import android.app.backup.BackupAgentHelper;

public class VerifierBackupAgent extends BackupAgentHelper {

    @Override
    public void onCreate() {
        super.onCreate();
        addHelper("test-results", new TestResultsBackupHelper(this));
        addHelper("backup-test-prefs", BackupTestActivity.getSharedPreferencesBackupHelper(this));
        addHelper("backup-test-files", BackupTestActivity.getFileBackupHelper(this));
    }
}
