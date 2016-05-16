/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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
package com.android.mail.preferences;

import android.content.Context;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Interface to allow migrating preferences from other projects into the UnifiedEmail code, so apps
 * can slowly move their preferences into the shared code.
 */
public abstract class BasePreferenceMigrator {
    /** If <code>true</code>, we have not attempted a migration since the app started. */
    private static final AtomicBoolean sMigrationNecessary = new AtomicBoolean(true);

    public final boolean performMigration(
            final Context context, final int oldVersion, final int newVersion) {
        // Ensure we only run this once
        if (sMigrationNecessary.getAndSet(false)) {
            migrate(context, oldVersion, newVersion);
            return true;
        }

        return false;
    }

    /**
     * Migrates preferences to UnifiedEmail.
     *
     * @param oldVersion The previous version of UnifiedEmail's preferences
     * @param newVersion The new version of UnifiedEmail's preferences
     */
    protected abstract void migrate(final Context context, int oldVersion, int newVersion);
}
