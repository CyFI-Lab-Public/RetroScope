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

import com.android.loaderapp.fragments.ContactsListFragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class HomeNormal extends Activity implements ContactsListFragment.Controller {
    static final int LOADER_LIST = 1;

    ContactsListFragment mFragment;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.home_normal);

        mFragment = (ContactsListFragment) findFragmentById(R.id.list);
        mFragment.setController(this);
    }

    public void onContactSelected(Uri contactUri) {
        // The user clicked on an item in the the list, start an activity to view it
        if (contactUri != null) {
            Intent intent = new Intent(this, DetailsNormal.class);
            intent.setData(contactUri);
            startActivity(intent);
        }
    }
}
