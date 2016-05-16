/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.widget.cts;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.cts.stub.R;

/**
 * A minimal application for AdapterView test.
 */
public class AdapterViewStubActivity extends Activity {
    private ListView mView;

    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mView = new ListView(this);
        mView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        setContentView(mView);
    }

    public ListView getListView() {
        return mView;
    }

    public ArrayAdapter<String> getArrayAdapter() {
        final List<String> list = new ArrayList<String>();
        for (int i = 0; i < 4; i++) {
            list.add("test:" + i);
        }
        return new ArrayAdapter<String>(this, R.layout.adapterview_layout, list);
    }
}
