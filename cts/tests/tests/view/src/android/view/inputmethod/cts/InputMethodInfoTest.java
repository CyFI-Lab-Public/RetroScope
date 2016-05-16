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

package android.view.inputmethod.cts;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Parcel;
import android.test.AndroidTestCase;
import android.util.Printer;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class InputMethodInfoTest extends AndroidTestCase {
    private InputMethodInfo mInputMethodInfo;
    private String mPackageName;
    private String mClassName;
    private CharSequence mLabel;
    private String mSettingsActivity;

    private int mSubtypeNameResId;
    private int mSubtypeIconResId;
    private String mSubtypeLocale;
    private String mSubtypeMode;
    private String mSubtypeExtraValue_key;
    private String mSubtypeExtraValue_value;
    private String mSubtypeExtraValue;
    private boolean mSubtypeIsAuxiliary;
    private boolean mSubtypeOverridesImplicitlyEnabledSubtype;
    private int mSubtypeId;
    private InputMethodSubtype mInputMethodSubtype;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPackageName = mContext.getPackageName();
        mClassName = InputMethodSettingsActivityStub.class.getName();
        mLabel = "test";
        mSettingsActivity = "android.view.inputmethod.cts.InputMethodSettingsActivityStub";
        mInputMethodInfo = new InputMethodInfo(mPackageName, mClassName, mLabel, mSettingsActivity);

        mSubtypeNameResId = 0;
        mSubtypeIconResId = 0;
        mSubtypeLocale = "en_US";
        mSubtypeMode = "keyboard";
        mSubtypeExtraValue_key = "key1";
        mSubtypeExtraValue_value = "value1";
        mSubtypeExtraValue = "tag," + mSubtypeExtraValue_key + "=" + mSubtypeExtraValue_value;
        mSubtypeIsAuxiliary = false;
        mSubtypeOverridesImplicitlyEnabledSubtype = false;
        mSubtypeId = 99;
        mInputMethodSubtype = new InputMethodSubtype(mSubtypeNameResId, mSubtypeIconResId,
                mSubtypeLocale, mSubtypeMode, mSubtypeExtraValue, mSubtypeIsAuxiliary,
                mSubtypeOverridesImplicitlyEnabledSubtype, mSubtypeId);
    }

    public void testInputMethodInfoProperties() throws XmlPullParserException, IOException {
        assertEquals(0, mInputMethodInfo.describeContents());
        assertNotNull(mInputMethodInfo.toString());

        assertInfo(mInputMethodInfo);
        assertEquals(0, mInputMethodInfo.getIsDefaultResourceId());

        Intent intent = new Intent(InputMethod.SERVICE_INTERFACE);
        intent.setClass(mContext, InputMethodSettingsActivityStub.class);
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> ris = pm.queryIntentServices(intent, PackageManager.GET_META_DATA);
        for (int i = 0; i < ris.size(); i++) {
            ResolveInfo resolveInfo = ris.get(i);
            mInputMethodInfo = new InputMethodInfo(mContext, resolveInfo);
            assertService(resolveInfo.serviceInfo, mInputMethodInfo.getServiceInfo());
            assertInfo(mInputMethodInfo);
        }
    }

    public void testInputMethodSubtypeProperties() {
        // TODO: Test InputMethodSubtype.getDisplayName()
        assertEquals(mSubtypeNameResId, mInputMethodSubtype.getNameResId());
        assertEquals(mSubtypeIconResId, mInputMethodSubtype.getIconResId());
        assertEquals(mSubtypeLocale, mInputMethodSubtype.getLocale());
        assertEquals(mSubtypeMode, mInputMethodSubtype.getMode());
        assertEquals(mSubtypeExtraValue, mInputMethodSubtype.getExtraValue());
        assertTrue(mInputMethodSubtype.containsExtraValueKey(mSubtypeExtraValue_key));
        assertEquals(mSubtypeExtraValue_value,
                mInputMethodSubtype.getExtraValueOf(mSubtypeExtraValue_key));
        assertEquals(mSubtypeIsAuxiliary, mInputMethodSubtype.isAuxiliary());
        assertEquals(mSubtypeOverridesImplicitlyEnabledSubtype,
                mInputMethodSubtype.overridesImplicitlyEnabledSubtype());
        assertEquals(mSubtypeId, mInputMethodSubtype.hashCode());
    }

    private void assertService(ServiceInfo expected, ServiceInfo actual) {
        assertEquals(expected.getIconResource(), actual.getIconResource());
        assertEquals(expected.labelRes, actual.labelRes);
        assertEquals(expected.nonLocalizedLabel, actual.nonLocalizedLabel);
        assertEquals(expected.icon, actual.icon);
        assertEquals(expected.permission, actual.permission);
    }

    private void assertInfo(InputMethodInfo info) {
        assertEquals(mPackageName, info.getPackageName());
        assertEquals(mSettingsActivity, info.getSettingsActivity());
        ComponentName component = info.getComponent();
        assertEquals(mClassName, component.getClassName());
        String expectedId = component.flattenToShortString();
        assertEquals(expectedId, info.getId());
        assertEquals(mClassName, info.getServiceName());
    }

    public void testDump() {
        MockPrinter printer = new MockPrinter();
        String prefix = "test";
        mInputMethodInfo.dump(printer, prefix);
    }

    public void testLoadIcon() {
        PackageManager pm = mContext.getPackageManager();
        assertNotNull(mInputMethodInfo.loadIcon(pm));
    }

    public void testEquals() {
        InputMethodInfo inputMethodInfo = new InputMethodInfo(mPackageName, mClassName, mLabel,
                mSettingsActivity);
        assertTrue(inputMethodInfo.equals(mInputMethodInfo));
    }

    public void testLoadLabel() {
        CharSequence expected = "test";
        PackageManager pm = mContext.getPackageManager();
        assertEquals(expected.toString(), mInputMethodInfo.loadLabel(pm).toString());
    }

    public void testInputMethodInfoWriteToParcel() {
        final Parcel p = Parcel.obtain();
        mInputMethodInfo.writeToParcel(p, 0);
        p.setDataPosition(0);
        final InputMethodInfo imi = InputMethodInfo.CREATOR.createFromParcel(p);

        assertEquals(mInputMethodInfo.getPackageName(), imi.getPackageName());
        assertEquals(mInputMethodInfo.getServiceName(), imi.getServiceName());
        assertEquals(mInputMethodInfo.getSettingsActivity(), imi.getSettingsActivity());
        assertEquals(mInputMethodInfo.getId(), imi.getId());
        assertEquals(mInputMethodInfo.getIsDefaultResourceId(), imi.getIsDefaultResourceId());
        assertService(mInputMethodInfo.getServiceInfo(), imi.getServiceInfo());
    }

    public void testInputMethodSubtypeWriteToParcel() {
        final Parcel p = Parcel.obtain();
        mInputMethodSubtype.writeToParcel(p, 0);
        p.setDataPosition(0);
        final InputMethodSubtype subtype = InputMethodSubtype.CREATOR.createFromParcel(p);

        assertEquals(mInputMethodSubtype.containsExtraValueKey(mSubtypeExtraValue_key),
                subtype.containsExtraValueKey(mSubtypeExtraValue_key));
        assertEquals(mInputMethodSubtype.getExtraValue(), subtype.getExtraValue());
        assertEquals(mInputMethodSubtype.getExtraValueOf(mSubtypeExtraValue_key),
                subtype.getExtraValueOf(mSubtypeExtraValue_key));
        assertEquals(mInputMethodSubtype.getIconResId(), subtype.getIconResId());
        assertEquals(mInputMethodSubtype.getLocale(), subtype.getLocale());
        assertEquals(mInputMethodSubtype.getMode(), subtype.getMode());
        assertEquals(mInputMethodSubtype.getNameResId(), subtype.getNameResId());
        assertEquals(mInputMethodSubtype.hashCode(), subtype.hashCode());
        assertEquals(mInputMethodSubtype.isAuxiliary(), subtype.isAuxiliary());
        assertEquals(mInputMethodSubtype.overridesImplicitlyEnabledSubtype(),
                subtype.overridesImplicitlyEnabledSubtype());
    }

    public void testInputMethodSubtypesOfSystemImes() {
        final InputMethodManager imm = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        final List<InputMethodInfo> imis = imm.getInputMethodList();
        final ArrayList<String> localeList = new ArrayList<String>(Arrays.asList(
                Resources.getSystem().getAssets().getLocales()));
        boolean foundEnabledSystemImeSubtypeWithValidLanguage = false;
        for (InputMethodInfo imi : imis) {
            if ((imi.getServiceInfo().applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                continue;
            }
            final int subtypeCount = imi.getSubtypeCount();
            // System IME must have one subtype at least.
            assertTrue(subtypeCount > 0);
            if (foundEnabledSystemImeSubtypeWithValidLanguage) {
                continue;
            }
            final List<InputMethodSubtype> enabledSubtypes =
                    imm.getEnabledInputMethodSubtypeList(imi, true);
            SUBTYPE_LOOP:
            for (InputMethodSubtype subtype : enabledSubtypes) {
                final String subtypeLocale = subtype.getLocale();
                if (subtypeLocale.length() < 2) {
                    continue;
                }
                // TODO: Detect language more strictly.
                final String subtypeLanguage = subtypeLocale.substring(0, 2);
                for (final String locale : localeList) {
                    if (locale.startsWith(subtypeLanguage)) {
                        foundEnabledSystemImeSubtypeWithValidLanguage = true;
                        break SUBTYPE_LOOP;
                    }
                }
            }
        }
        assertTrue(foundEnabledSystemImeSubtypeWithValidLanguage);
    }

    class MockPrinter implements Printer {
        @Override
        public void println(String x) {
        }
    }
}
