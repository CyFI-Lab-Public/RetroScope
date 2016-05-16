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

package android.view.cts;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.Resources.Theme;
import android.test.AndroidTestCase;
import android.view.ContextThemeWrapper;

import com.android.cts.stub.R;


public class ContextThemeWrapperTest extends AndroidTestCase {
    private static final int SYSTEM_DEFAULT_THEME = 0;

    private static class MocContextThemeWrapper extends ContextThemeWrapper {
        public boolean isOnApplyThemeResourceCalled;
        public MocContextThemeWrapper(Context base, int themeres) {
            super(base, themeres);
        }

        @Override
        protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
            isOnApplyThemeResourceCalled = true;
            super.onApplyThemeResource(theme, resid, first);
        }
    }

    public void testConstructor() {
        // new the ContextThemeWrapper instance
        new ContextThemeWrapper();

        // new the ContextThemeWrapper instance
        new ContextThemeWrapper(getContext(), R.style.TextAppearance);
    }

    public void testAccessTheme() {
        Context context = getContext();
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(
                context, SYSTEM_DEFAULT_THEME);
        // set Theme to TextAppearance
        contextThemeWrapper.setTheme(R.style.TextAppearance);
        TypedArray ta =
            contextThemeWrapper.getTheme().obtainStyledAttributes(R.styleable.TextAppearance);

        // assert theme style of TextAppearance
        assertEqualsTextAppearanceStyle(ta);
    }

    public void testGetSystemService() {
        // new the ContextThemeWrapper instance
        Context context = getContext();
        int themeres = R.style.TextAppearance;
        MocContextThemeWrapper contextThemeWrapper = new MocContextThemeWrapper(context, themeres);
        contextThemeWrapper.getTheme();
        assertTrue(contextThemeWrapper.isOnApplyThemeResourceCalled);
        // All service get from contextThemeWrapper just the same as this context get,
        // except Context.LAYOUT_INFLATER_SERVICE.
        assertEquals(context.getSystemService(Context.ACTIVITY_SERVICE),
                contextThemeWrapper.getSystemService(Context.ACTIVITY_SERVICE));
        assertNotSame(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE),
                contextThemeWrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
    }

    public void testAttachBaseContext() {
        assertTrue((new ContextThemeWrapper() {
            public boolean test() {
                // Set two different context to ContextThemeWrapper
                // it should throw a exception when set it at second time.
                // As ContextThemeWrapper is a context, we will attachBaseContext to
                // two different ContextThemeWrapper instances.
                try {
                    attachBaseContext(new ContextThemeWrapper(getContext(),
                            R.style.TextAppearance));
                } catch(IllegalStateException e) {
                    fail("test attachBaseContext fail");
                }

                try {
                    attachBaseContext(new ContextThemeWrapper());
                    fail("test attachBaseContext fail");
                } catch(IllegalStateException e) {
                    // expected
                }
                return true;
            }
        }).test());
    }

    private void assertEqualsTextAppearanceStyle(TypedArray ta) {
        final int defValue = -1;
        // get Theme and assert
        Resources.Theme expected = getContext().getResources().newTheme();
        expected.setTo(getContext().getTheme());
        expected.applyStyle(R.style.TextAppearance, true);
        TypedArray expectedTa = expected.obtainStyledAttributes(R.styleable.TextAppearance);
        assertEquals(expectedTa.getIndexCount(), ta.getIndexCount());
        assertEquals(expectedTa.getColor(R.styleable.TextAppearance_textColor, defValue),
                ta.getColor(R.styleable.TextAppearance_textColor, defValue));
        assertEquals(expectedTa.getColor(R.styleable.TextAppearance_textColorHint, defValue),
                ta.getColor(R.styleable.TextAppearance_textColorHint, defValue));
        assertEquals(expectedTa.getColor(R.styleable.TextAppearance_textColorLink, defValue),
                ta.getColor(R.styleable.TextAppearance_textColorLink, defValue));
        assertEquals(expectedTa.getColor(R.styleable.TextAppearance_textColorHighlight, defValue),
                ta.getColor(R.styleable.TextAppearance_textColorHighlight, defValue));
        assertEquals(expectedTa.getDimension(R.styleable.TextAppearance_textSize, defValue),
                ta.getDimension(R.styleable.TextAppearance_textSize, defValue));
        assertEquals(expectedTa.getInt(R.styleable.TextAppearance_textStyle, defValue),
                ta.getInt(R.styleable.TextAppearance_textStyle, defValue));
    }
}
