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

import android.app.Dialog;
import android.holo.cts.LayoutModifier;
import android.view.View;
import android.view.ViewGroup;

/**
 * Wacky {@link LayoutModifier} that takes a {@link Dialog} and transplants into the
 * parent view group.
 */
public class DialogModifier extends AbstractLayoutModifier {

    private DialogBuilder mBuilder;

    public DialogModifier(DialogBuilder builder) {
        mBuilder = builder;
    }

    @Override
    public View modifyView(View view) {
        Dialog dialog = mBuilder.buildDialog(view);

        ViewGroup parent = (ViewGroup) view.getParent();
        parent.removeView(view); // remove the filler view

        // get the dialog as a view
        View newView = dialog.getWindow().getDecorView();

        // remove it from the dialog
        dialog.getWindow().getWindowManager().removeView(newView);

        // so we can add it to our ViewGroup
        parent.addView(newView);

        return newView;
    }
}
