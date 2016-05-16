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

package android.text.method.cts;


import android.test.ActivityInstrumentationTestCase2;
import android.text.method.SingleLineTransformationMethod;
import android.view.View;
import android.widget.EditText;

/**
 * Test {@link SingleLineTransformationMethod}.
 */
public class SingleLineTransformationMethodTest
        extends ActivityInstrumentationTestCase2<StubActivity> {
    public SingleLineTransformationMethodTest() {
        super("com.android.cts.stub", StubActivity.class);
    }

    public void testConstructor() {
        new SingleLineTransformationMethod();
    }

    public void testGetInstance() {
        SingleLineTransformationMethod method0 = SingleLineTransformationMethod.getInstance();
        assertNotNull(method0);

        SingleLineTransformationMethod method1 = SingleLineTransformationMethod.getInstance();
        assertSame(method0, method1);
    }

    public void testGetReplacement() {
        MySingleLineTranformationMethod method = new MySingleLineTranformationMethod();
        TextMethodUtils.assertEquals(new char[] { ' ', '\uFEFF' }, method.getReplacement());
        TextMethodUtils.assertEquals(new char[] { '\n', '\r' }, method.getOriginal());
    }

    public void testGetTransformation() {
        SingleLineTransformationMethod method = SingleLineTransformationMethod.getInstance();
        CharSequence result = method.getTransformation("hello\nworld\r", null);
        assertEquals("hello world\uFEFF", result.toString());

        EditText editText = new EditText(getActivity());
        editText.setText("hello\nworld\r");
        // TODO cannot get transformed text from the view
    }

    /**
     * The Class MySingleLineTranformationMethod.
     */
    private static class MySingleLineTranformationMethod extends SingleLineTransformationMethod {
        @Override
        protected char[] getOriginal() {
            return super.getOriginal();
        }

        @Override
        protected char[] getReplacement() {
            return super.getReplacement();
        }
    }
}
