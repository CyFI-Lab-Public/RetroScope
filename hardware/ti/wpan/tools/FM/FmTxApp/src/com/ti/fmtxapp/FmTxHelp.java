/*
 *
 * Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ti.fmtxapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

public class FmTxHelp extends Activity implements View.OnKeyListener,
        View.OnClickListener {
    public static final String TAG = "FmTxHelp";

    /** Called when the activity is first created. */

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fmtxhelp);
        initControls();

    }

    private void initControls() {
        Button btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(this);

    }


    public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
        return false;
    }


    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
        case R.id.btnBack:
            finish();
            break;
        }
    }
}
