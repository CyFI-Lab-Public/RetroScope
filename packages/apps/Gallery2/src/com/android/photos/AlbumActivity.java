/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.photos;

import android.app.Activity;
import android.os.Bundle;

public class AlbumActivity extends Activity implements MultiChoiceManager.Provider {

    public static final String KEY_ALBUM_URI = AlbumFragment.KEY_ALBUM_URI;
    public static final String KEY_ALBUM_TITLE = AlbumFragment.KEY_ALBUM_TITLE;

    private MultiChoiceManager mMultiChoiceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle intentExtras = getIntent().getExtras();
        mMultiChoiceManager = new MultiChoiceManager(this);
        if (savedInstanceState == null) {
            AlbumFragment albumFragment = new AlbumFragment();
            mMultiChoiceManager.setDelegate(albumFragment);
            albumFragment.setArguments(intentExtras);
            getFragmentManager().beginTransaction().add(android.R.id.content,
                    albumFragment).commit();
        }
        getActionBar().setTitle(intentExtras.getString(KEY_ALBUM_TITLE));
    }

    @Override
    public MultiChoiceManager getMultiChoiceManager() {
        return mMultiChoiceManager;
    }
}
