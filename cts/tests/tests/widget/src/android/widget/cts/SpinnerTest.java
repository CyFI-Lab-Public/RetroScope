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

import com.android.cts.stub.R;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * Test {@link Spinner}.
 */
public class SpinnerTest extends ActivityInstrumentationTestCase2<RelativeLayoutStubActivity> {
    private Context mTargetContext;

    public SpinnerTest() {
        super("com.android.cts.stub", RelativeLayoutStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTargetContext = getInstrumentation().getTargetContext();
    }

    public void testConstructor() {
        new Spinner(mTargetContext);

        new Spinner(mTargetContext, null);

        new Spinner(mTargetContext, null, com.android.internal.R.attr.spinnerStyle);

        Spinner spinner = (Spinner) getActivity().findViewById(R.id.spinner1);
        assertEquals(mTargetContext.getString(R.string.text_view_hello), spinner.getPrompt());
    }

    @UiThreadTest
    public void testGetBaseline() {
        Spinner spinner = new Spinner(mTargetContext);

        assertEquals(-1, spinner.getBaseline());

        spinner = (Spinner) getActivity().findViewById(R.id.spinner1);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mTargetContext,
                com.android.cts.stub.R.array.string, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        assertTrue(spinner.getBaseline() > 0);
    }

    public void testSetOnItemClickListener() {
        Spinner spinner = new Spinner(mTargetContext);

        try {
            spinner.setOnItemClickListener(null);
            fail("Should throw RuntimeException");
        } catch (RuntimeException e) {
        }
    }

    @UiThreadTest
    public void testPerformClick() {
        final Spinner spinner = (Spinner) getActivity().findViewById(R.id.spinner1);

        assertTrue(spinner.performClick());

        // TODO: no description for the expected result for this method in its javadoc, issue?
        // Or do UI check?
    }

    public void testOnClick() {
        Spinner spinner = new Spinner(mTargetContext);
        // normal value
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        AlertDialog alertDialog = builder.show();
        assertTrue(alertDialog.isShowing());
        spinner.onClick(alertDialog, 10);
        assertEquals(10, spinner.getSelectedItemPosition());
        assertFalse(alertDialog.isShowing());

        // exceptional
        try {
            spinner.onClick(null, 10);
            fail("did not throw NullPointerException");
        } catch (NullPointerException e) {
        }

        Dialog dialog = new Dialog(getActivity());
        dialog.show();
        assertTrue(dialog.isShowing());
        spinner.onClick(dialog, -10);
        assertEquals(-10, spinner.getSelectedItemPosition());
        assertFalse(dialog.isShowing());
    }

    @UiThreadTest
    public void testAccessPrompt() {
        final String promptText = "prompt text";

        Spinner spinner = new Spinner(mTargetContext);

        spinner.setPrompt(promptText);
        assertEquals(promptText, spinner.getPrompt());

        spinner.setPrompt(null);
        assertNull(spinner.getPrompt());

        // TODO: find the dialog and get its title to assert whether setPrompt() takes effect?
    }

    public void testsetPromptId() {
        Spinner spinner = new Spinner(mTargetContext);

        spinner.setPromptId(R.string.hello_world);
        assertEquals(mTargetContext.getString(R.string.hello_world), spinner.getPrompt());

        try {
            spinner.setPromptId(-1);
            fail("Should throw NotFoundException");
        } catch (NotFoundException e) {
            // issue 1695243, not clear what is supposed to happen if promptId is exceptional.
        }

        try {
            spinner.setPromptId(Integer.MAX_VALUE);
            fail("Should throw NotFoundException");
        } catch (NotFoundException e) {
            // issue 1695243, not clear what is supposed to happen if promptId is exceptional.
        }

        // TODO: find the dialog and get its title to assert whether setPromptId() takes effect?
    }

    public void testOnLayout() {
        // onLayout() is implementation details, do NOT test
    }
}
