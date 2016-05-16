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

package android.content.pm.cts;


import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Parcel;
import android.test.AndroidTestCase;

public class ConfigurationInfoTest extends AndroidTestCase {
    public void testConfigPreferences() throws NameNotFoundException {
        PackageManager pm = getContext().getPackageManager();

        // Test constructors
        new ConfigurationInfo();
        PackageInfo pkgInfo = pm.getPackageInfo(getContext().getPackageName(),
                PackageManager.GET_CONFIGURATIONS);
        ConfigurationInfo[] configInfoArray = pkgInfo.configPreferences;
        assertTrue(configInfoArray.length > 0);
        ConfigurationInfo configInfo = configInfoArray[0];
        ConfigurationInfo infoFromExisted = new ConfigurationInfo(configInfo);
        checkInfoSame(configInfo, infoFromExisted);

        // Test toString, describeContents
        assertEquals(0, configInfo.describeContents());
        assertNotNull(configInfo.toString());

        // Test writeToParcel
        Parcel p = Parcel.obtain();
        configInfo.writeToParcel(p, 0);
        p.setDataPosition(0);
        ConfigurationInfo infoFromParcel = ConfigurationInfo.CREATOR.createFromParcel(p);
        checkInfoSame(configInfo, infoFromParcel);
        p.recycle();
    }

    private void checkInfoSame(ConfigurationInfo expected, ConfigurationInfo actual) {
        assertEquals(expected.reqKeyboardType, actual.reqKeyboardType);
        assertEquals(expected.reqTouchScreen, actual.reqTouchScreen);
        assertEquals(expected.reqInputFeatures, actual.reqInputFeatures);
        assertEquals(expected.reqNavigation, actual.reqNavigation);
    }
}
