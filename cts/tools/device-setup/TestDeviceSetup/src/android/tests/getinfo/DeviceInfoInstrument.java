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

package android.tests.getinfo;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.UserManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class DeviceInfoInstrument extends Instrumentation implements DeviceInfoConstants {

    private static final String TAG = "DeviceInfoInstrument";

    private static Bundle mResults = new Bundle();

    public DeviceInfoInstrument() {
        super();
    }

    @Override
    public void onCreate(Bundle arguments) {
        start();
    }

    @Override
    public void onStart() {
        addResult(BUILD_ID, Build.ID);
        addResult(PRODUCT_NAME, Build.PRODUCT);
        addResult(BUILD_DEVICE, Build.DEVICE);
        addResult(BUILD_BOARD, Build.BOARD);
        addResult(BUILD_MANUFACTURER, Build.MANUFACTURER);
        addResult(BUILD_BRAND, Build.BRAND);
        addResult(BUILD_MODEL, Build.MODEL);
        addResult(BUILD_TYPE, Build.TYPE);
        addResult(BUILD_FINGERPRINT, Build.FINGERPRINT);
        addResult(BUILD_ABI, Build.CPU_ABI);
        addResult(BUILD_ABI2, Build.CPU_ABI2);
        addResult(SERIAL_NUMBER, Build.SERIAL);

        addResult(VERSION_RELEASE, Build.VERSION.RELEASE);
        addResult(VERSION_SDK, Build.VERSION.SDK);

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getContext().getSystemService(
                Context.WINDOW_SERVICE);
        Display d = wm.getDefaultDisplay();
        d.getRealMetrics(metrics);
        addResult(RESOLUTION, String.format("%sx%s", metrics.widthPixels, metrics.heightPixels));
        addResult(SCREEN_DENSITY, metrics.density);
        addResult(SCREEN_X_DENSITY, metrics.xdpi);
        addResult(SCREEN_Y_DENSITY, metrics.ydpi);

        String screenDensityBucket = getScreenDensityBucket(metrics);
        addResult(SCREEN_DENSITY_BUCKET, screenDensityBucket);

        String screenSize = getScreenSize();
        addResult(SCREEN_SIZE, screenSize);

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(this.getContext(), DeviceInfoActivity.class);

        DeviceInfoActivity activity = (DeviceInfoActivity) startActivitySync(intent);
        waitForIdleSync();
        activity.waitForAcitityToFinish();

        TelephonyManager tm = (TelephonyManager) getContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        // network
        String network = tm.getNetworkOperatorName();
        addResult(NETWORK, network.trim());

        // imei
        String imei = tm.getDeviceId();
        addResult(IMEI, imei);

        // imsi
        String imsi = tm.getSubscriberId();
        addResult(IMSI, imsi);

        // phone number
        String phoneNumber = tm.getLine1Number();
        addResult(PHONE_NUMBER, phoneNumber);

        // features
        String features = getFeatures();
        addResult(FEATURES, features);

        // processes
        String processes = getProcesses();
        addResult(PROCESSES, processes);

        // OpenGL ES version
        String openGlEsVersion = getOpenGlEsVersion();
        addResult(OPEN_GL_ES_VERSION, openGlEsVersion);

        // partitions
        String partitions = getPartitions();
        addResult(PARTITIONS, partitions);

        // System libraries
        String sysLibraries = getSystemLibraries();
        addResult(SYS_LIBRARIES, sysLibraries);

        // Storage devices
        addResult(STORAGE_DEVICES, getStorageDevices());

        // Multi-user support
        addResult(MULTI_USER, getMultiUserInfo());

        finish(Activity.RESULT_OK, mResults);
    }

    /**
     * Add string result.
     *
     * @param key the string of the key name.
     * @param value string value.
     */
    static void addResult(final String key, final String value){
        mResults.putString(key, value);
    }

    /**
     * Add integer result.
     *
     * @param key the string of the key name.
     * @param value integer value.
     */
    private void addResult(final String key, final int value){
        mResults.putInt(key, value);
    }

    /**
     * Add float result.
     *
     * @param key the string of the key name.
     * @param value float value.
     */
    private void addResult(final String key, final float value){
        mResults.putFloat(key, value);
    }

    private String getScreenSize() {
        Configuration config = getContext().getResources().getConfiguration();
        int screenLayout = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        String screenSize = String.format("0x%x", screenLayout);
        switch (screenLayout) {
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                screenSize = "small";
                break;

            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                screenSize = "normal";
                break;

            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                screenSize = "large";
                break;

            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                screenSize = "xlarge";
                break;

            case Configuration.SCREENLAYOUT_SIZE_UNDEFINED:
                screenSize = "undefined";
                break;
        }
        return screenSize;
    }

    private String getScreenDensityBucket(DisplayMetrics metrics) {
        switch (metrics.densityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                return "ldpi";

            case DisplayMetrics.DENSITY_MEDIUM:
                return "mdpi";

            case DisplayMetrics.DENSITY_TV:
                return "tvdpi";

            case DisplayMetrics.DENSITY_HIGH:
                return "hdpi";

            case DisplayMetrics.DENSITY_XHIGH:
                return "xdpi";

            default:
                return "" + metrics.densityDpi;
        }
    }

    /**
     * Return a summary of the device's feature as a semi-colon-delimited list of colon separated
     * name and availability pairs like "feature1:sdk:true;feature2:sdk:false;feature3:other:true;".
     */
    private String getFeatures() {
        StringBuilder features = new StringBuilder();

        try {
            Set<String> checkedFeatures = new HashSet<String>();

            PackageManager packageManager = getContext().getPackageManager();
            for (String featureName : getPackageManagerFeatures()) {
                checkedFeatures.add(featureName);
                boolean hasFeature = packageManager.hasSystemFeature(featureName);
                addFeature(features, featureName, "sdk", hasFeature);
            }

            FeatureInfo[] featureInfos = packageManager.getSystemAvailableFeatures();
            if (featureInfos != null) {
                for (FeatureInfo featureInfo : featureInfos) {
                    if (featureInfo.name != null && !checkedFeatures.contains(featureInfo.name)) {
                        addFeature(features, featureInfo.name, "other", true);
                    }
                }
            }
        } catch (Exception exception) {
            Log.e(TAG, "Error getting features: " + exception.getMessage(), exception);
        }

        return features.toString();
    }

    private static void addFeature(StringBuilder features, String name, String type,
            boolean available) {
        features.append(name).append(':').append(type).append(':').append(available).append(';');
    }

    /**
     * Use reflection to get the features defined by the SDK. If there are features that do not fit
     * the convention of starting with "FEATURE_" then they will still be shown under the
     * "Other Features" section.
     *
     * @return list of feature names from sdk
     */
    private List<String> getPackageManagerFeatures() {
        try {
            List<String> features = new ArrayList<String>();
            Field[] fields = PackageManager.class.getFields();
            for (Field field : fields) {
                if (field.getName().startsWith("FEATURE_")) {
                    String feature = (String) field.get(null);
                    features.add(feature);
                }
            }
            return features;
        } catch (IllegalAccessException illegalAccess) {
            throw new RuntimeException(illegalAccess);
        }
    }

    /**
     * Return a semi-colon-delimited list of the root processes that were running on the phone
     * or an error message.
     */
    private static String getProcesses() {
        StringBuilder builder = new StringBuilder();

        try {
            String[] rootProcesses = RootProcessScanner.getRootProcesses();
            for (String rootProcess : rootProcesses) {
                builder.append(rootProcess).append(':').append(0).append(';');
            }
        } catch (Exception exception) {
            Log.e(TAG, "Error getting processes: " + exception.getMessage(), exception);
            builder.append(exception.getMessage());
        }

        return builder.toString();
    }

    /** @return a string containing the Open GL ES version number or an error message */
    private String getOpenGlEsVersion() {
        PackageManager packageManager = getContext().getPackageManager();
        FeatureInfo[] featureInfos = packageManager.getSystemAvailableFeatures();
        if (featureInfos != null && featureInfos.length > 0) {
            for (FeatureInfo featureInfo : featureInfos) {
                // Null feature name means this feature is the open gl es version feature.
                if (featureInfo.name == null) {
                    return featureInfo.getGlEsVersion();
                }
            }
        }
        return "No feature for Open GL ES version.";
    }

    private String getPartitions() {
        try {
            StringBuilder builder = new StringBuilder();
            Process df = new ProcessBuilder("df").start();
            Scanner scanner = new Scanner(df.getInputStream());
            try {
                while (scanner.hasNextLine()) {
                    builder.append(scanner.nextLine()).append(';');
                }
                return builder.toString();
            } finally {
                scanner.close();
            }
        } catch (IOException e) {
            return "Not able to run df for partition information.";
        }
    }

    private String getSystemLibraries() {
        PackageManager pm = getContext().getPackageManager();
        String list[] = pm.getSystemSharedLibraryNames();

        StringBuilder builder = new StringBuilder();
        for (String lib : list) {
            builder.append(lib);
            builder.append(";");
        }

        return builder.toString();
    }

    private String getStorageDevices() {
        int count = 0;
        count = Math.max(count, getContext().getExternalCacheDirs().length);
        count = Math.max(count, getContext().getExternalFilesDirs(null).length);
        count = Math.max(
                count, getContext().getExternalFilesDirs(Environment.DIRECTORY_PICTURES).length);
        count = Math.max(count, getContext().getObbDirs().length);

        final String result;
        if (Environment.isExternalStorageEmulated()) {
            if (count == 1) {
                return "1 emulated";
            } else {
                return "1 emulated, " + (count - 1) + " physical media";
            }
        } else {
            return count + " physical media";
        }
    }

    private String getMultiUserInfo() {
        try {
            final Method method = UserManager.class.getMethod("getMaxSupportedUsers");
            final Integer maxUsers = (Integer) method.invoke(null);
            if (maxUsers == 1) {
                return "single user";
            } else {
                return maxUsers + " users supported";
            }
        } catch (ClassCastException e) {
        } catch (NoSuchMethodException e) {
        } catch (InvocationTargetException e) {
        } catch (IllegalAccessException e) {
        }

        return "unknown";
    }
}
