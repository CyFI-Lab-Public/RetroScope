/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	    http://www.apache.org/licenses/LICENSE-2.0
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

public class HomeXLarge extends Activity implements ContactsListFragment.Controller,
        ActionBar.Callback {
    private static final int ACTION_ID_SEARCH = 0;
    private static final int ACTION_ID_ADD = 1;

    ContactsListFragment mList;
    ContactFragment mDetails;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.home_xlarge);

        mList = new ContactsListFragment();
        mList.setController(this);
        mDetails = new ContactFragment(null, new ContactFragment.DefaultController(this));
        FragmentTransaction transaction = openFragmentTransaction();
        transaction.add(R.id.contacts_list, mList);
        transaction.add(R.id.contact_details, mDetails);
        transaction.commit();

        getActionBar().setCallback(this);

        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            mDetails.loadContact(intent.getData());
        }
    }

    public void onAction(int id) {
        switch (id) {
            case ACTION_ID_SEARCH:
                startSearch(null, false, null, true);
                break;

            case ACTION_ID_ADD:
                startActivity(new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI));
                break;
        }
    }

    public void onContactSelected(Uri contactUri) {
        // The user clicked on an item in the left side pane, start loading the data for it
        mDetails.loadContact(contactUri);
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
