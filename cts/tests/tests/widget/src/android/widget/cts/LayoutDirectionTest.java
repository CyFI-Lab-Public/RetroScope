/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.ViewGroup;
import android.widget.*;
import com.android.cts.stub.R;

import static android.view.View.LAYOUT_DIRECTION_LTR;
import static android.view.View.LAYOUT_DIRECTION_RTL;
import static android.view.View.LAYOUT_DIRECTION_INHERIT;
import static android.view.View.LAYOUT_DIRECTION_LOCALE;

public class LayoutDirectionTest extends ActivityInstrumentationTestCase2<LayoutDirectionStubActivity> {

    public LayoutDirectionTest() {
        super(LayoutDirectionStubActivity.class);
    }

    private void checkDefaultDirectionForOneLayoutWithCode(ViewGroup vg) {
        assertEquals(LAYOUT_DIRECTION_LTR, vg.getLayoutDirection());
    }

    @UiThreadTest
    public void testLayoutDirectionDefaults() {
        checkDefaultDirectionForOneLayoutWithCode(new LinearLayout(getActivity()));
        checkDefaultDirectionForOneLayoutWithCode(new FrameLayout(getActivity()));
        checkDefaultDirectionForOneLayoutWithCode(new TableLayout(getActivity()));
        checkDefaultDirectionForOneLayoutWithCode(new RelativeLayout(getActivity()));
        checkDefaultDirectionForOneLayoutWithCode(new GridLayout(getActivity()));
    }

    private void checkDirectionForOneLayoutWithCode(ViewGroup vg) {
        vg.setLayoutDirection(LAYOUT_DIRECTION_LTR);
        assertEquals(LAYOUT_DIRECTION_LTR, vg.getLayoutDirection());

        vg.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        assertEquals(LAYOUT_DIRECTION_RTL, vg.getLayoutDirection());

        vg.setLayoutDirection(LAYOUT_DIRECTION_LOCALE);
        // running with English locale
        assertEquals(LAYOUT_DIRECTION_LTR, vg.getLayoutDirection());

        vg.setLayoutDirection(LAYOUT_DIRECTION_INHERIT);
        // default is LTR
        assertEquals(LAYOUT_DIRECTION_LTR, vg.getLayoutDirection());
    }

    @UiThreadTest
    public void testDirectionForAllLayoutsWithCode() {
        checkDirectionForOneLayoutWithCode(new LinearLayout(getActivity()));
        checkDirectionForOneLayoutWithCode(new FrameLayout(getActivity()));
        checkDirectionForOneLayoutWithCode(new TableLayout(getActivity()));
        checkDirectionForOneLayoutWithCode(new RelativeLayout(getActivity()));
        checkDirectionForOneLayoutWithCode(new GridLayout(getActivity()));
    }

    private void checkDirectionInheritanceForOneLayoutWithCode(ViewGroup parent) {
        LinearLayout child = new LinearLayout(getActivity());
        child.setLayoutDirection(LAYOUT_DIRECTION_INHERIT);
        parent.addView(child);

        // Parent is LTR
        parent.setLayoutDirection(LAYOUT_DIRECTION_LTR);

        assertEquals(LAYOUT_DIRECTION_LTR, parent.getLayoutDirection());
        assertEquals(LAYOUT_DIRECTION_LTR, child.getLayoutDirection());

        // Parent is RTL
        parent.setLayoutDirection(LAYOUT_DIRECTION_RTL);

        assertEquals(LAYOUT_DIRECTION_RTL, parent.getLayoutDirection());
        assertEquals(LAYOUT_DIRECTION_RTL, child.getLayoutDirection());
    }

    @UiThreadTest
    public void testDirectionInheritanceForAllLayoutsWithCode() {
        checkDirectionInheritanceForOneLayoutWithCode(new LinearLayout(getActivity()));
        checkDirectionInheritanceForOneLayoutWithCode(new FrameLayout(getActivity()));
        checkDirectionInheritanceForOneLayoutWithCode(new TableLayout(getActivity()));
        checkDirectionInheritanceForOneLayoutWithCode(new RelativeLayout(getActivity()));
        checkDirectionInheritanceForOneLayoutWithCode(new GridLayout(getActivity()));
    }

    private void checkDirectionForOneLayoutFromXml(int parentId, int parentDir, int parentResDir,
                                                   int child1Id, int child1Dir, int child1ResDir,
                                                   int child2Id, int child2Dir, int child2ResDir,
                                                   int child3Id, int child3Dir, int child3ResDir,
                                                   int child4Id, int child4Dir, int child4ResDir) {
        ViewGroup ll = (ViewGroup) getActivity().findViewById(parentId);
        assertEquals(parentResDir, ll.getLayoutDirection());

        ViewGroup child1 = (ViewGroup) getActivity().findViewById(child1Id);
        assertEquals(child1ResDir, child1.getLayoutDirection());

        ViewGroup child2 = (ViewGroup) getActivity().findViewById(child2Id);
        assertEquals(child2ResDir, child2.getLayoutDirection());

        ViewGroup child3 = (ViewGroup) getActivity().findViewById(child3Id);
        assertEquals(child3ResDir, child3.getLayoutDirection());

        ViewGroup child4 = (ViewGroup) getActivity().findViewById(child4Id);
        assertEquals(child4ResDir, child4.getLayoutDirection());
    }

    @UiThreadTest
    public void testDirectionFromXml() {
        // We only test LinearLayout as the others would be the same (they extend ViewGroup / View)
        checkDirectionForOneLayoutFromXml(
                R.id.layout_linearlayout_ltr, LAYOUT_DIRECTION_LTR, LAYOUT_DIRECTION_LTR,
                R.id.layout_linearlayout_ltr_child_1, LAYOUT_DIRECTION_LTR, LAYOUT_DIRECTION_LTR,
                R.id.layout_linearlayout_ltr_child_2, LAYOUT_DIRECTION_RTL, LAYOUT_DIRECTION_RTL,
                // parent is LTR
                R.id.layout_linearlayout_ltr_child_3, LAYOUT_DIRECTION_INHERIT, LAYOUT_DIRECTION_LTR,
                // running with English locale
                R.id.layout_linearlayout_ltr_child_4, LAYOUT_DIRECTION_LOCALE, LAYOUT_DIRECTION_LTR);

        checkDirectionForOneLayoutFromXml(
                R.id.layout_linearlayout_rtl, LAYOUT_DIRECTION_RTL, LAYOUT_DIRECTION_RTL,
                R.id.layout_linearlayout_rtl_child_1, LAYOUT_DIRECTION_LTR, LAYOUT_DIRECTION_LTR,
                R.id.layout_linearlayout_rtl_child_2, LAYOUT_DIRECTION_RTL, LAYOUT_DIRECTION_RTL,
                // parent is RTL
                R.id.layout_linearlayout_rtl_child_3, LAYOUT_DIRECTION_INHERIT, LAYOUT_DIRECTION_RTL,
                // running with English locale
                R.id.layout_linearlayout_rtl_child_4, LAYOUT_DIRECTION_LOCALE, LAYOUT_DIRECTION_LTR);

        checkDirectionForOneLayoutFromXml(
                // default is LTR
                R.id.layout_linearlayout_inherit, LAYOUT_DIRECTION_INHERIT, LAYOUT_DIRECTION_LTR,
                R.id.layout_linearlayout_inherit_child_1, LAYOUT_DIRECTION_LTR, LAYOUT_DIRECTION_LTR,
                R.id.layout_linearlayout_inherit_child_2, LAYOUT_DIRECTION_RTL, LAYOUT_DIRECTION_RTL,
                // parent is LTR
                R.id.layout_linearlayout_inherit_child_3, LAYOUT_DIRECTION_INHERIT, LAYOUT_DIRECTION_LTR,
                // running with English locale
                R.id.layout_linearlayout_inherit_child_4, LAYOUT_DIRECTION_LOCALE, LAYOUT_DIRECTION_LTR);

        checkDirectionForOneLayoutFromXml(
                // running with English locale
                R.id.layout_linearlayout_locale, LAYOUT_DIRECTION_LOCALE, LAYOUT_DIRECTION_LTR,
                R.id.layout_linearlayout_locale_child_1, LAYOUT_DIRECTION_LTR, LAYOUT_DIRECTION_LTR,
                R.id.layout_linearlayout_locale_child_2, LAYOUT_DIRECTION_RTL, LAYOUT_DIRECTION_RTL,
                // parent is LTR
                R.id.layout_linearlayout_locale_child_3, LAYOUT_DIRECTION_INHERIT, LAYOUT_DIRECTION_LTR,
                // running with English locale
                R.id.layout_linearlayout_locale_child_4, LAYOUT_DIRECTION_LOCALE, LAYOUT_DIRECTION_LTR);
    }
}
