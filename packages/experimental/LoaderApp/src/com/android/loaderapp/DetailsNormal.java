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

import android.app.Activity;
import android.os.Bundle;

public class DetailsNormal extends Activity {
    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.details_normal);

        ContactFragment frag = (ContactFragment) findFragmentById(R.id.details);
        frag.setController(new ContactFragment.DefaultController(this));
        frag.loadContact(getIntent().getData());
    }
}
