/*
 * Copyright (C) 2010 Google Inc.
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
 * limitations under the License
 */

package com.android.loaderapp;

import com.android.loaderapp.fragments.ContactFragment;
import com.android.loaderapp.fragments.ContactsListFragment;
import com.android.loaderapp.fragments.GroupsListFragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class HomeGroupsXLarge extends Activity implements ActionBar.Callback,
        ContactsListFragment.Controller, GroupsListFragment.Controller {
    private static final int ACTION_ID_SEARCH = 0;
    private static final int ACTION_ID_ADD = 1;

    private static final int MODE_GROUPS = 0;
    private static final int MODE_DETAILS = 1;

    int mMode;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.two_pane);

        GroupsListFragment groupsList = new GroupsListFragment();
        groupsList.setController(this);

        ContactsListFragment contactsList = new ContactsListFragment(
                ContactsListFragment.MODE_NULL);
        contactsList.setController(this);
 
        FragmentTransaction xact = openFragmentTransaction();
        xact.add(R.id.smallPane, groupsList);
        xact.add(R.id.largePane, contactsList);
        xact.commit();
        mMode = MODE_GROUPS;

        getActionBar().setCallback(this);
    }

    private ContactsListFragment getContactsList() {
        switch (mMode) {
            case MODE_GROUPS:
                return (ContactsListFragment) findFragmentById(R.id.largePane);
            case MODE_DETAILS:
                return (ContactsListFragment) findFragmentById(R.id.smallPane);
        }
        throw new IllegalStateException("unknown mode " + mMode);
    }

    public void onAllContactsSelected() {
        getContactsList().setMode(ContactsListFragment.MODE_VISIBLE);
    }

    public void onFavoritesSelected() {
        getContactsList().setMode(ContactsListFragment.MODE_STREQUENT);
    }

    public void onGroupSelected(String title) {
        getContactsList().setGroupMode(title);
    }

    public void onContactSelected(Uri contactUri) {
        if (contactUri == null) {
            return;
        }

        ContactFragment details = new ContactFragment(contactUri,
                new ContactFragment.DefaultController(this));
        FragmentTransaction xact = openFragmentTransaction();
        xact.addToBackStack(null);
        if (mMode == MODE_GROUPS) {
            mMode = MODE_DETAILS;
            // commit is actually async, so add in details at largePane, which will be correct
            // after swapPanes() does its thing.
            xact.remove(findFragmentById(R.id.smallPane));
            xact.add(R.id.largePane, details);
            xact.commit();
            swapPanes(); // swap the list to the small pane
        } else {
            xact.replace(R.id.largePane, details);
            xact.commit();
        }
    }

    private void swapPanes() {
        ViewGroup paneHost = (ViewGroup) findViewById(R.id.paneHost);

        View largePane = findViewById(R.id.largePane);
        ViewGroup.LayoutParams largeParams = largePane.getLayoutParams();
        int largeIndex = paneHost.indexOfChild(largePane);

        View smallPane = findViewById(R.id.smallPane);
        ViewGroup.LayoutParams smallParams = smallPane.getLayoutParams();
        int smallIndex = paneHost.indexOfChild(smallPane);

        paneHost.removeAllViews();

        largePane.setId(R.id.smallPane);
        largePane.setLayoutParams(smallParams);

        smallPane.setId(R.id.largePane);
        smallPane.setLayoutParams(largeParams);

        if (smallIndex < largeIndex) {
            // Small was before large so add them back in reverse order
            paneHost.addView(largePane);
            paneHost.addView(smallPane);
        } else {
            // Large was before small so add them back in reverse order
            paneHost.addView(smallPane);
            paneHost.addView(largePane);
        }
    }

    @Override
    public void onBackPressed() {
        if (!popBackStack(null)) {
            finish();
        } else {
            if (mMode == MODE_DETAILS) {
                swapPanes();
                mMode = MODE_GROUPS;
            }
        }
    }

    /** Implements ActionBar.Callback */
    public boolean onCreateActionMenu(Menu menu) {
        Resources resources = getResources();
        menu.add(0, ACTION_ID_SEARCH, 0, R.string.menu_search)
                .setIcon(resources.getDrawable(android.R.drawable.ic_menu_search));
        menu.add(0, ACTION_ID_ADD, 1, R.string.menu_newContact)
                .setIcon(resources.getDrawable(android.R.drawable.ic_menu_add));

        return true;
    }

    /** Implements ActionBar.Callback */
    public boolean onUpdateActionMenu(Menu menu) {
        return false;
    }

    /** Implements ActionBar.Callback */
    public boolean onActionItemClicked(MenuItem item) {
        switch (item.getItemId()) {
            case ACTION_ID_SEARCH: {
                startSearch(null, false, null, true);
                return true;
            }

            case ACTION_ID_ADD: {
                startActivity(new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI));
                return true;
            }
        }
        return false;
    }

    /** Implements ActionBar.Callback */
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        return false;
    }

    /** Implements ActionBar.Callback */
    public boolean onCreateContextMode(int modeId, Menu menu) {
        return false;
    }

    /** Implements ActionBar.Callback */
    public boolean onPrepareContextMode(int modeId, Menu menu) {
        return false;
    }

    /** Implements ActionBar.Callback */
    public boolean onContextItemClicked(int modeId, MenuItem item) {
        return false;
    }
}
