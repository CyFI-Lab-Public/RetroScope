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

package android.drm.cts.configs;

import java.util.HashMap;

import android.drm.cts.Config;

public class PassthruConfig implements Config {
    private static PassthruConfig sInstance = new PassthruConfig();
    private PassthruConfig() {}
    public static PassthruConfig getInstance() {
        return sInstance;
    }
    public String getPluginName() {
        return "Passthru";
    }
    public String getMimeType() {
        return "application/vnd.passthru.drm";
    }
    public String getAccountId() {
        return "01234567";
    }
    public String getRightsPath() {
        return "/sdcard/dummy_passthru_rights.xml";
    }
    public String getContentPath() {
        return "/sdcard/dummy_passthru_content.passthru";
    }
    public HashMap<String, String> getInfoOfRegistration() {
        return sInfoOfRegistration;
    }
    public HashMap<String, String> getInfoOfUnregistration() {
        return sInfoOfUnregistration;
    }
    public HashMap<String, String> getInfoOfRightsAcquisition(){
        return sInfoOfRightsAcquisition;
    }

    private static HashMap<String, String> sInfoOfRegistration = new HashMap<String, String>();
    private static HashMap<String, String> sInfoOfUnregistration = new HashMap<String, String>();
    private static HashMap<String, String> sInfoOfRightsAcquisition = new HashMap<String, String>();

    static {
        sInfoOfRegistration.put("Foo-1", "foo-1");

        sInfoOfUnregistration.put("Foo-2", "foo-2");

        sInfoOfRightsAcquisition.put("Foo-3", "foo-3");
    }
}
