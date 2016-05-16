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

package com.android.mail.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Application;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

// Should not rely on any mail-specific packages.

/**
 * {@link RestrictedActivity} gives access to a subset of {@link Activity} methods. These methods
 * match the signatures from {@link Activity}.
 */
public interface RestrictedActivity {
    /*
     * All methods are from android.app.Activity, and the doc strings need to point to the
     * underlying methods.
     */

    /**
     * @see android.app.Activity#findViewById(int)
     */
    View findViewById(int id);

    /**
     * @see android.app.Activity#finish()
     */
    void finish();

    /**
     * @see android.app.Activity#getActionBar()
     */
    ActionBar getActionBar();

    /**
     * @see android.app.Activity#getApplication()
     */
    Application getApplication();

    /**
     * @see android.app.Activity#getComponentName()
     */
    ComponentName getComponentName();

    /**
     * @see android.app.Activity#getContentResolver()
     */
    ContentResolver getContentResolver();

    /**
     * @see android.app.Activity#getFragmentManager()
     */
    FragmentManager getFragmentManager();

    /**
     * @see android.app.Activity#getIntent()
     */
    Intent getIntent();

    /**
     * @see android.app.Activity#getLoaderManager()
     */
    LoaderManager getLoaderManager();

    /**
     * @see android.app.Activity#getMenuInflater()
     */
    MenuInflater getMenuInflater();

    /**
     * @see android.app.Activity#getWindow()
     */
    Window getWindow();

    /**
     * @see android.app.Activity#invalidateOptionsMenu()
     */
    void invalidateOptionsMenu();

    /**
     * @see android.app.Activity#isChangingConfigurations()
     */
    boolean isChangingConfigurations();

    /**
     * @see android.app.Activity#isFinishing()
     */
    boolean isFinishing();

    /**
     * @see android.app.Activity#onBackPressed()
     */
    void onBackPressed();

    /**
     * @see android.app.Activity#setContentView(int)
     */
    void setContentView(int layoutResId);

    /**
     * @see android.app.Activity#setDefaultKeyMode(int)
     */
    void setDefaultKeyMode(int mode);

    /**
     * @see android.app.Activity#setResult(int, Intent)
     */
    void setResult(int resultCode, Intent data);

    /**
     * @see android.app.Activity#setTitle(CharSequence)
     */
    void setTitle(CharSequence title);

    /**
     * @see android.app.Activity#showDialog(int)
     */
    void showDialog(int id);

    /**
     * @see android.app.Activity#startActionMode(android.view.ActionMode.Callback)
     */
    ActionMode startActionMode(ActionMode.Callback callback);

    /**
     * @see android.app.Activity#startActivityForResult(Intent, int)
     */
    void startActivityForResult(Intent intent, int requestCode);

    /**
     * @see android.app.Activity#startActivityForResult(Intent, int)
     */
    void startActivity(Intent intent);

    /**
     * @see android.app.Activity#startSearch(String, boolean, Bundle, boolean)
     */
    void startSearch(String initialQuery, boolean selectInitialQuery,
            Bundle appSearchData, boolean globalSearch);

    /**
     * @see android.app.Activity#getApplicationContext()
     */
    Context getApplicationContext();

    /**
     * Returns the context associated with the activity. This is different from the value returned
     * by {@link #getApplicationContext()}, which is the single context of the root activity. Some
     * components (dialogs) require the context of the activity. When implementing this, you can
     * return this, since each activity is also a context.
     * @return the context associated with this activity.
     */
    Context getActivityContext();

    /**
     * @see Activity#onOptionsItemSelected(MenuItem)
     */
    boolean onOptionsItemSelected(MenuItem item);

    /**
     * @see Activity#hasWindowFocus()
     */
    public boolean hasWindowFocus();

    void setPendingToastOperation(ToastBarOperation op);

    ToastBarOperation getPendingToastOperation();
}
