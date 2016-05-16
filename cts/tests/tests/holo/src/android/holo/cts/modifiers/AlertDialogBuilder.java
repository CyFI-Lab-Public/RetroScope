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

package android.holo.cts.modifiers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.view.View;

public class AlertDialogBuilder implements DialogBuilder {

    public static final int ONE_BUTTON = 0;

    public static final int TWO_BUTTONS = 1;

    public static final int THREE_BUTTONS = 2;

    public static final int LIST = 3;

    public static final int SINGLE_CHOICE = 4;

    public static final int MULTI_CHOICE = 5;

    private int mDialogType;

    private static final CharSequence[] ITEMS = {"Red", "Green", "Blue"};

    public AlertDialogBuilder(int dialogType) {
        mDialogType = dialogType;
    }

    @Override
    public Dialog buildDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());

        switch (mDialogType) {
            case ONE_BUTTON:
                builder.setTitle("Exit Dialog")
                        .setMessage("Are you sure you want to exit?")
                        .setPositiveButton("Exit", null);
                break;
            case TWO_BUTTONS:
                builder.setMessage("Are you sure you want to exit?")
                        .setPositiveButton("Exit", null)
                        .setNegativeButton("Cancel", null);
                break;
            case THREE_BUTTONS:
                builder.setMessage("Are you sure you want to exit?")
                        .setPositiveButton("Exit", null)
                        .setNeutralButton("Neutral", null)
                        .setNegativeButton("Cancel", null);
                break;
            case LIST:
                builder.setTitle("Pick a Color")
                        .setItems(ITEMS, null);
                break;
            case SINGLE_CHOICE:
                builder.setTitle("Pick a Color")
                        .setSingleChoiceItems(ITEMS, 1, null);
                break;
            case MULTI_CHOICE:
                builder.setTitle("Pick a Color")
                        .setMultiChoiceItems(ITEMS, new boolean[]{false, true, false}, null);
                break;
        }

        return builder.show();
    }
}