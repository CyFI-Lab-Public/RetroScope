/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.content.res.cts;

import java.util.Locale;

import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.Resources.NotFoundException;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.DisplayMetrics;
import android.util.Log;

import com.android.cts.stub.R;

public class ConfigTest extends AndroidTestCase {
    enum Properties {
        LANGUAGE,
        COUNTRY,
        MCC,
        MNC,
        TOUCHSCREEN,
        KEYBOARD,
        KEYBOARDHIDDEN,
        NAVIGATION,
        ORIENTATION,
        WIDTH,
        HEIGHT,
        DENSITY,
        SCREENLAYOUT,
        SWIDTH_DP,
        WIDTH_DP,
        HEIGHT_DP
    }

    private static void checkValue(final Resources res, final int resId,
            final String expectedValue) {
        try {
            final String actual = res.getString(resId);
            assertNotNull("Returned wrong configuration-based simple value: expected <nothing>, "
                    + "got '" + actual + "' from resource 0x" + Integer.toHexString(resId),
                    expectedValue);
            assertEquals("Returned wrong configuration-based simple value: expected '"
                    + expectedValue + "', got '" + actual + "' from resource 0x"
                    + Integer.toHexString(resId), expectedValue, actual);
        } catch (NotFoundException e) {
            assertNull("Resource not found for configuration-based simple value: expecting \""
                    + expectedValue + "\"", expectedValue);
        }
    }

    private static void checkValue(final Resources res, final int resId,
            final int[] styleable, final String[] expectedValues) {
        final Resources.Theme theme = res.newTheme();
        final TypedArray sa = theme.obtainStyledAttributes(resId, styleable);
        for (int i = 0; i < styleable.length; i++) {
            final String actual = sa.getString(i);
            assertEquals("Returned wrong configuration-based style value: expected '"
                    + expectedValues[i] + "', got '" + actual + "' from attr "
                    + i + " of resource 0x" + Integer.toHexString(resId),
                    actual, expectedValues[i]);
        }
        sa.recycle();
    }

    private class TotalConfig {
        final Configuration mConfig;
        final DisplayMetrics mMetrics;

        public TotalConfig() {
            mConfig = new Configuration();
            mMetrics = new DisplayMetrics();
            mConfig.locale = new Locale("++", "++");
        }

        public void setProperty(final Properties p, final int value) {
            switch(p) {
                case MCC:
                    mConfig.mcc = value;
                    break;
                case MNC:
                    mConfig.mnc = value;
                    break;
                case TOUCHSCREEN:
                    mConfig.touchscreen = value;
                    break;
                case KEYBOARD:
                    mConfig.keyboard = value;
                    break;
                case KEYBOARDHIDDEN:
                    mConfig.keyboardHidden = value;
                    break;
                case NAVIGATION:
                    mConfig.navigation = value;
                    break;
                case ORIENTATION:
                    mConfig.orientation = value;
                    break;
                case WIDTH:
                    mMetrics.widthPixels = value;
                    mMetrics.noncompatWidthPixels = value;
                    break;
                case HEIGHT:
                    mMetrics.heightPixels = value;
                    mMetrics.noncompatHeightPixels = value;
                    break;
                case DENSITY:
                    // this is the ratio from the standard
                    mMetrics.density = (((float)value)/((float)DisplayMetrics.DENSITY_DEFAULT));
                    mMetrics.noncompatDensity = mMetrics.density;
                    mConfig.densityDpi = value;
                    break;
                case SCREENLAYOUT:
                    mConfig.screenLayout = value;
                    break;
                case SWIDTH_DP:
                    mConfig.smallestScreenWidthDp = value;
                    break;
                case WIDTH_DP:
                    mConfig.screenWidthDp = value;
                    break;
                case HEIGHT_DP:
                    mConfig.screenHeightDp = value;
                    break;
                default:
                    assert(false);
                    break;
            }
        }

        public void setProperty(final Properties p, final String value) {
            switch(p) {
                case LANGUAGE:
                    final String oldCountry = mConfig.locale.getCountry();
                    mConfig.locale = new Locale(value, oldCountry);
                    break;
                case COUNTRY:
                    final String oldLanguage = mConfig.locale.getLanguage();
                    mConfig.locale = new Locale(oldLanguage, value);
                    break;
                default:
                    assert(false);
                    break;
            }
        }

        public Resources getResources() {
            final AssetManager assmgr = new AssetManager();
            assmgr.addAssetPath(mContext.getPackageResourcePath());
            return new Resources(assmgr, mMetrics, mConfig);
        }
    }

    public TotalConfig makeEmptyConfig() {
        return new TotalConfig();
    }

    public TotalConfig makeClassicConfig() {
        TotalConfig config = new TotalConfig();
        config.setProperty(Properties.LANGUAGE, "en");
        config.setProperty(Properties.COUNTRY, "US");
        config.setProperty(Properties.MCC, 310);
        config.setProperty(Properties.MNC, 001); // unused
        config.setProperty(Properties.TOUCHSCREEN, Configuration.TOUCHSCREEN_FINGER);
        config.setProperty(Properties.KEYBOARD, Configuration.KEYBOARD_QWERTY);
        config.setProperty(Properties.KEYBOARDHIDDEN, Configuration.KEYBOARDHIDDEN_YES);
        config.setProperty(Properties.NAVIGATION, Configuration.NAVIGATION_TRACKBALL);
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_PORTRAIT);
        config.setProperty(Properties.SWIDTH_DP, 320);
        config.setProperty(Properties.WIDTH_DP, 320);
        config.setProperty(Properties.HEIGHT_DP, 480);
        config.setProperty(Properties.DENSITY, 160);
        config.setProperty(Properties.WIDTH, 200);
        config.setProperty(Properties.HEIGHT, 320);
        return config;
    }

    private static void checkPair(Resources res, int[] notResIds,
            int simpleRes, String simpleString,
            int bagRes, String bagString) {
        boolean willHave = true;
        if (notResIds != null) {
            for (int i : notResIds) {
                if (i == simpleRes) {
                    willHave = false;
                    break;
                }
            }
        }
        checkValue(res, simpleRes, willHave ? simpleString : null);
        checkValue(res, bagRes, R.styleable.TestConfig,
                new String[]{willHave ? bagString : null});
    }

    @SmallTest
    public void testAllEmptyConfigs() {
        /**
         * Test a resource that contains a value for each possible single
         * configuration value.
         */
        TotalConfig config = makeEmptyConfig();
        Resources res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple default");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag default"});

        config = makeEmptyConfig();
        config.setProperty(Properties.LANGUAGE, "xx");
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple xx");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag xx"});

        config = makeEmptyConfig();
        config.setProperty(Properties.LANGUAGE, "xx");
        config.setProperty(Properties.COUNTRY, "YY");
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple xx-rYY");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag xx-rYY"});

        config = makeEmptyConfig();
        config.setProperty(Properties.MCC, 111);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple mcc111");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag mcc111"});

        config = makeEmptyConfig();
        config.setProperty(Properties.MNC, 222);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple mnc222");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag mnc222"});

        config = makeEmptyConfig();
        config.setProperty(Properties.TOUCHSCREEN, Configuration.TOUCHSCREEN_NOTOUCH);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple notouch");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag notouch"});

        config = makeEmptyConfig();
        config.setProperty(Properties.TOUCHSCREEN, Configuration.TOUCHSCREEN_STYLUS);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple stylus");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag stylus"});

        config = makeEmptyConfig();
        config.setProperty(Properties.KEYBOARD, Configuration.KEYBOARD_NOKEYS);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple nokeys");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag nokeys"});

        config = makeEmptyConfig();
        config.setProperty(Properties.KEYBOARD, Configuration.KEYBOARD_12KEY);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 12key");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 12key"});

        config = makeEmptyConfig();
        config.setProperty(Properties.KEYBOARDHIDDEN, Configuration.KEYBOARDHIDDEN_NO);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple keysexposed");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag keysexposed"});

        config = makeEmptyConfig();
        config.setProperty(Properties.NAVIGATION, Configuration.NAVIGATION_NONAV);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple nonav");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag nonav"});

        config = makeEmptyConfig();
        config.setProperty(Properties.NAVIGATION, Configuration.NAVIGATION_DPAD);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple dpad");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag dpad"});

        config = makeEmptyConfig();
        config.setProperty(Properties.NAVIGATION, Configuration.NAVIGATION_WHEEL);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple wheel");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag wheel"});

        config = makeEmptyConfig();
        config.setProperty(Properties.HEIGHT, 480);
        config.setProperty(Properties.WIDTH, 320);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 480x320");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 480x320"});

        config = makeEmptyConfig();
        config.setProperty(Properties.DENSITY, 240);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 240dpi");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 240dpi"});

        config = makeEmptyConfig();
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_LANDSCAPE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple landscape");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag landscape"});

        config = makeEmptyConfig();
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_SQUARE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple square");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag square"});

        config = makeEmptyConfig();
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_SMALL);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple small");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag small"});

        config = makeEmptyConfig();
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_NORMAL);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple normal");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag normal"});

        config = makeEmptyConfig();
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_LARGE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple large");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag large"});

        config = makeEmptyConfig();
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_XLARGE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple xlarge");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag xlarge"});

        config = makeEmptyConfig();
        config.setProperty(Properties.SWIDTH_DP, 600);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw600");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag sw600"});

        config = makeEmptyConfig();
        config.setProperty(Properties.SWIDTH_DP, 600);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw600");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag sw600"});

        config = makeEmptyConfig();
        config.setProperty(Properties.SWIDTH_DP, 720);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw720");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag sw720"});

        config = makeEmptyConfig();
        config.setProperty(Properties.WIDTH_DP, 600);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple w600");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag w600"});

        config = makeEmptyConfig();
        config.setProperty(Properties.WIDTH_DP, 720);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple w720");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag w720"});

        config = makeEmptyConfig();
        config.setProperty(Properties.HEIGHT_DP, 550);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple h550");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag h550"});

        config = makeEmptyConfig();
        config.setProperty(Properties.HEIGHT_DP, 670);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple h670");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag h670"});
    }

    @SmallTest
    public void testAllClassicConfigs() {
        /**
         * Test a resource that contains a value for each possible single
         * configuration value.
         */
        TotalConfig config = makeClassicConfig();
        Resources res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple default");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag default"});

        config = makeClassicConfig();
        config.setProperty(Properties.LANGUAGE, "xx");
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple xx");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag xx"});

        config = makeClassicConfig();
        config.setProperty(Properties.LANGUAGE, "xx");
        config.setProperty(Properties.COUNTRY, "YY");
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple xx-rYY");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag xx-rYY"});

        config = makeClassicConfig();
        config.setProperty(Properties.MCC, 111);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple mcc111");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag mcc111"});

        config = makeClassicConfig();
        config.setProperty(Properties.MNC, 222);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple mnc222");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag mnc222"});

        config = makeClassicConfig();
        config.setProperty(Properties.TOUCHSCREEN, Configuration.TOUCHSCREEN_NOTOUCH);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple notouch");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag notouch"});

        config = makeClassicConfig();
        config.setProperty(Properties.TOUCHSCREEN, Configuration.TOUCHSCREEN_STYLUS);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple stylus");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag stylus"});

        config = makeClassicConfig();
        config.setProperty(Properties.KEYBOARD, Configuration.KEYBOARD_NOKEYS);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple nokeys");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag nokeys"});

        config = makeClassicConfig();
        config.setProperty(Properties.KEYBOARD, Configuration.KEYBOARD_12KEY);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 12key 63x57");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 12key 63x57"});

        config = makeClassicConfig();
        config.setProperty(Properties.KEYBOARDHIDDEN, Configuration.KEYBOARDHIDDEN_NO);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple keysexposed");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag keysexposed"});

        config = makeClassicConfig();
        config.setProperty(Properties.NAVIGATION, Configuration.NAVIGATION_NONAV);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple nonav");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag nonav"});

        config = makeClassicConfig();
        config.setProperty(Properties.NAVIGATION, Configuration.NAVIGATION_DPAD);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple dpad 63x57");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag dpad 63x57"});

        config = makeClassicConfig();
        config.setProperty(Properties.NAVIGATION, Configuration.NAVIGATION_WHEEL);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple wheel");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag wheel"});

        config = makeClassicConfig();
        config.setProperty(Properties.HEIGHT, 480);
        config.setProperty(Properties.WIDTH, 320);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 480x320");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 480x320"});

        config = makeClassicConfig();
        config.setProperty(Properties.DENSITY, 240);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 240dpi");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 240dpi"});

        config = makeClassicConfig();
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_LANDSCAPE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple landscape");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag landscape"});

        config = makeClassicConfig();
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_SQUARE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple square");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag square"});

        config = makeClassicConfig();
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_SMALL);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple small");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag small"});

        config = makeClassicConfig();
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_NORMAL);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple normal");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag normal"});

        config = makeClassicConfig();
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_LARGE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple large");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag large"});

        config = makeClassicConfig();
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_XLARGE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple xlarge");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag xlarge"});

        config = makeClassicConfig();
        config.setProperty(Properties.SWIDTH_DP, 600);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw600");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag sw600"});

        config = makeClassicConfig();
        config.setProperty(Properties.SWIDTH_DP, 600);
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_LANDSCAPE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw600 land");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag sw600 land"});

        config = makeClassicConfig();
        config.setProperty(Properties.SWIDTH_DP, 720);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw720");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag sw720"});

        config = makeClassicConfig();
        config.setProperty(Properties.WIDTH_DP, 600);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple w600");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag w600"});

        config = makeClassicConfig();
        config.setProperty(Properties.WIDTH_DP, 720);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple w720");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag w720"});

        config = makeClassicConfig();
        config.setProperty(Properties.HEIGHT_DP, 550);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple h550");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag h550"});

        config = makeClassicConfig();
        config.setProperty(Properties.HEIGHT_DP, 670);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple h670");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag h670"});
    }
    
    @MediumTest
    public void testDensity() throws Exception {
        // have 32, 240 and the default 160 content.
        // rule is that closest wins, with down scaling (larger content)
        // being twice as nice as upscaling.
        // transition at H/2 * (-1 +/- sqrt(1+8L/H))
        // SO, X < 49 goes to 32
        // 49 >= X < 182 goes to 160
        // X >= 182 goes to 240
        TotalConfig config = makeClassicConfig();
        config.setProperty(Properties.DENSITY, 2);
        Resources res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 32dpi");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 32dpi"});

        config = makeClassicConfig();
        config.setProperty(Properties.DENSITY, 32);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 32dpi");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 32dpi"});

        config = makeClassicConfig();
        config.setProperty(Properties.DENSITY, 48);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 32dpi");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 32dpi"});

        config = makeClassicConfig();
        config.setProperty(Properties.DENSITY, 49);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple default");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag default"});

        config = makeClassicConfig();
        config.setProperty(Properties.DENSITY, 150);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple default");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag default"});

        config = makeClassicConfig();
        config.setProperty(Properties.DENSITY, 181);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple default");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag default"});

        config = makeClassicConfig();
        config.setProperty(Properties.DENSITY, 182);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 240dpi");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 240dpi"});

        config = makeClassicConfig();
        config.setProperty(Properties.DENSITY, 239);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 240dpi");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 240dpi"});

        config = makeClassicConfig();
        config.setProperty(Properties.DENSITY, 490);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 240dpi");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 240dpi"});
    }

    @MediumTest
    public void testScreenSize() throws Exception {
        // ensure that we fall back to the best available screen size
        // for a given configuration.
        TotalConfig config = makeClassicConfig();
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_SMALL);
        Resources res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple small");
        checkValue(res, R.configVarying.small, "small");
        checkValue(res, R.configVarying.normal, "default");
        checkValue(res, R.configVarying.large, "default");
        checkValue(res, R.configVarying.xlarge, "default");
        
        config = makeClassicConfig();
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_NORMAL);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple normal");
        checkValue(res, R.configVarying.small, "default");
        checkValue(res, R.configVarying.normal, "normal");
        checkValue(res, R.configVarying.large, "default");
        checkValue(res, R.configVarying.xlarge, "default");
        
        config = makeClassicConfig();
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_LARGE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple large");
        checkValue(res, R.configVarying.small, "default");
        checkValue(res, R.configVarying.normal, "normal");
        checkValue(res, R.configVarying.large, "large");
        checkValue(res, R.configVarying.xlarge, "default");
        
        config = makeClassicConfig();
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_XLARGE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple xlarge");
        checkValue(res, R.configVarying.small, "default");
        checkValue(res, R.configVarying.normal, "normal");
        checkValue(res, R.configVarying.large, "large");
        checkValue(res, R.configVarying.xlarge, "xlarge");
    }

    @MediumTest
    public void testNewScreenSize() throws Exception {
        // ensure that swNNNdp, wNNNdp, and hNNNdp are working correctly
        // for various common screen configurations.
        TotalConfig config = makeClassicConfig();
        config.setProperty(Properties.SWIDTH_DP, 589);
        config.setProperty(Properties.WIDTH_DP, 589);
        config.setProperty(Properties.HEIGHT_DP, 500);
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_LANDSCAPE);
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_LARGE);
        Resources res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple large");
        checkValue(res, R.configVarying.sw, "default");
        checkValue(res, R.configVarying.w, "default");
        checkValue(res, R.configVarying.h, "default");
        checkValue(res, R.configVarying.wh, "default");

        config = makeClassicConfig();
        config.setProperty(Properties.SWIDTH_DP, 590);
        config.setProperty(Properties.WIDTH_DP, 590);
        config.setProperty(Properties.HEIGHT_DP, 500);
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_LANDSCAPE);
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_LARGE);
        config.setProperty(Properties.DENSITY, DisplayMetrics.DENSITY_MEDIUM);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw590 mdpi");
        checkValue(res, R.configVarying.sw, "590 mdpi");

        config.setProperty(Properties.DENSITY, DisplayMetrics.DENSITY_HIGH);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw590 hdpi");
        checkValue(res, R.configVarying.sw, "590 hdpi");

        config.setProperty(Properties.DENSITY, DisplayMetrics.DENSITY_XHIGH);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw590 xhdpi");
        checkValue(res, R.configVarying.sw, "590 xhdpi");

        config.setProperty(Properties.SWIDTH_DP, 591);
        config.setProperty(Properties.WIDTH_DP, 591);
        config.setProperty(Properties.DENSITY, DisplayMetrics.DENSITY_MEDIUM);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw591");
        checkValue(res, R.configVarying.sw, "591");

        config.setProperty(Properties.DENSITY, DisplayMetrics.DENSITY_HIGH);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw591 hdpi");
        checkValue(res, R.configVarying.sw, "591 hdpi");

        config.setProperty(Properties.DENSITY, DisplayMetrics.DENSITY_XHIGH);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw591 hdpi");
        checkValue(res, R.configVarying.sw, "591 hdpi");

        config = makeClassicConfig();
        config.setProperty(Properties.SWIDTH_DP, 480);
        config.setProperty(Properties.WIDTH_DP, 800);
        config.setProperty(Properties.HEIGHT_DP, 480);
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_LANDSCAPE);
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_LARGE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple w720");
        checkValue(res, R.configVarying.sw, "default");
        checkValue(res, R.configVarying.w, "720");
        checkValue(res, R.configVarying.h, "default");
        checkValue(res, R.configVarying.wh, "600");

        config = makeClassicConfig();
        config.setProperty(Properties.SWIDTH_DP, 600);
        config.setProperty(Properties.WIDTH_DP, 1024);
        config.setProperty(Properties.HEIGHT_DP, 552);
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_LANDSCAPE);
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_LARGE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw600 land");
        checkValue(res, R.configVarying.sw, "600 land");
        checkValue(res, R.configVarying.w, "720");
        checkValue(res, R.configVarying.h, "550");
        checkValue(res, R.configVarying.wh, "600-550");

        config = makeClassicConfig();
        config.setProperty(Properties.SWIDTH_DP, 600);
        config.setProperty(Properties.WIDTH_DP, 600);
        config.setProperty(Properties.HEIGHT_DP, 974);
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_PORTRAIT);
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_LARGE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw600");
        checkValue(res, R.configVarying.sw, "600");
        checkValue(res, R.configVarying.w, "600");
        checkValue(res, R.configVarying.h, "670");
        checkValue(res, R.configVarying.wh, "600-550");

        config = makeClassicConfig();
        config.setProperty(Properties.SWIDTH_DP, 719);
        config.setProperty(Properties.WIDTH_DP, 1279);
        config.setProperty(Properties.HEIGHT_DP, 669);
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_LANDSCAPE);
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_LARGE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw600 land");
        checkValue(res, R.configVarying.sw, "600 land");
        checkValue(res, R.configVarying.w, "720");
        checkValue(res, R.configVarying.h, "550");
        checkValue(res, R.configVarying.wh, "600-550");

        config = makeClassicConfig();
        config.setProperty(Properties.SWIDTH_DP, 800);
        config.setProperty(Properties.WIDTH_DP, 1280);
        config.setProperty(Properties.HEIGHT_DP, 672);
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_LANDSCAPE);
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_XLARGE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw720");
        checkValue(res, R.configVarying.sw, "720");
        checkValue(res, R.configVarying.w, "720");
        checkValue(res, R.configVarying.h, "670");
        checkValue(res, R.configVarying.wh, "720-670");

        config = makeClassicConfig();
        config.setProperty(Properties.SWIDTH_DP, 800);
        config.setProperty(Properties.WIDTH_DP, 720);
        config.setProperty(Properties.HEIGHT_DP, 1230);
        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_PORTRAIT);
        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_XLARGE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw720");
        checkValue(res, R.configVarying.sw, "720");
        checkValue(res, R.configVarying.w, "720");
        checkValue(res, R.configVarying.h, "670");
        checkValue(res, R.configVarying.wh, "720-670");
    }

// TODO - add tests for special cases - ie, other key params seem ignored if 
// nokeys is set

    @MediumTest
    public void testPrecidence() {
        /**
         * Check for precidence of resources selected when there are multiple
         * options matching the current config.
         */
        TotalConfig config = makeEmptyConfig();
        config.setProperty(Properties.HEIGHT, 640);
        config.setProperty(Properties.WIDTH, 400);
        Resources res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 640x400");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 640x400"});

        config.setProperty(Properties.NAVIGATION, Configuration.NAVIGATION_NONAV);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple nonav");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag nonav"});

        config.setProperty(Properties.KEYBOARD, Configuration.KEYBOARD_NOKEYS);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple nokeys");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag nokeys"});

        config.setProperty(Properties.KEYBOARDHIDDEN, Configuration.KEYBOARDHIDDEN_NO);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple keysexposed");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag keysexposed"});

        config.setProperty(Properties.TOUCHSCREEN, Configuration.TOUCHSCREEN_NOTOUCH);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple notouch");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag notouch"});

        config.setProperty(Properties.DENSITY, 240);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 240dpi");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 240dpi"});

        config.setProperty(Properties.ORIENTATION, Configuration.ORIENTATION_LANDSCAPE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple landscape");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag landscape"});

        config.setProperty(Properties.SCREENLAYOUT, Configuration.SCREENLAYOUT_SIZE_XLARGE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple xlarge");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag xlarge"});

        config.setProperty(Properties.HEIGHT_DP, 670);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple h670");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag h670"});

        config.setProperty(Properties.WIDTH_DP, 720);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 720-670");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 720-670"});

        config.setProperty(Properties.SWIDTH_DP, 720);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple sw720");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag sw720"});

        config.setProperty(Properties.LANGUAGE, "xx");
        config.setProperty(Properties.COUNTRY, "YY");
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple xx-rYY");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag xx-rYY"});

        config.setProperty(Properties.MCC, 111);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple mcc111 xx-rYY");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag mcc111 xx-rYY"});

        config.setProperty(Properties.MNC, 222);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple mcc111 mnc222");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag mcc111 mnc222"});
    }

    @MediumTest
    public void testCombinations() {
        /**
         * Verify that in cases of ties, the specific ordering is followed
         */

        /**
         * Precidence order: mcc, mnc, locale, swdp, wdp, hdp, screenlayout-size,
         * screenlayout-long, orientation, density,
         * touchscreen, hidden, keyboard, navigation, width-height
         */

        /**
         * verify mcc trumps mnc.  Have 110-xx, 220-xx but no 110-220
         * so which is selected?  Should be mcc110-xx.
         */
        TotalConfig config = makeClassicConfig();
        config.setProperty(Properties.MCC, 110);
        config.setProperty(Properties.MNC, 220);
        config.setProperty(Properties.LANGUAGE, "xx");
        Resources res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple mcc110 xx");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag mcc110 xx"});

        /* full A + B + C doesn't exist.  Do we get A + C or B + C? 
         */
        config = makeClassicConfig();
        config.setProperty(Properties.MCC, 111);
        config.setProperty(Properties.MNC, 222);
        config.setProperty(Properties.LANGUAGE, "xx");
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple mcc111 mnc222");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag mcc111 mnc222"});

        config = makeClassicConfig();
        config.setProperty(Properties.MNC, 222);
        config.setProperty(Properties.LANGUAGE, "xx");
        config.setProperty(Properties.ORIENTATION, 
                Configuration.ORIENTATION_SQUARE);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple mnc222 xx");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag mnc222 xx"});

        config = makeClassicConfig();
        config.setProperty(Properties.LANGUAGE, "xx");
        config.setProperty(Properties.ORIENTATION, 
                Configuration.ORIENTATION_SQUARE);
        config.setProperty(Properties.DENSITY, 32);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple xx square");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag xx square"});

        /**
         * Verify that proper strings are found for multiple-selectivity case
         * (ie, a string set for locale and mcc is found only when both are
         * true).
         */
        config = makeClassicConfig();
        config.setProperty(Properties.LANGUAGE, "xx");
        config.setProperty(Properties.COUNTRY, "YY");
        config.setProperty(Properties.MCC, 111);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple mcc111 xx-rYY");
        checkValue(res, R.configVarying.bag, R.styleable.TestConfig,
                new String[] { "bag mcc111 xx-rYY" });

        config = makeClassicConfig();
        config.setProperty(Properties.LANGUAGE, "xx");
        config.setProperty(Properties.COUNTRY, "YY");
        config.setProperty(Properties.MCC, 333);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple xx-rYY");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[] { "bag xx-rYY" });

        config = makeClassicConfig();
        config.setProperty(Properties.MNC, 333);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple default");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag default"});

        config = makeClassicConfig();
        config.setProperty(Properties.ORIENTATION, 
                Configuration.ORIENTATION_SQUARE);
        config.setProperty(Properties.DENSITY, 32);
        config.setProperty(Properties.TOUCHSCREEN, 
                Configuration.TOUCHSCREEN_STYLUS);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple square 32dpi");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag square 32dpi"});

        config = makeClassicConfig();
        config.setProperty(Properties.DENSITY, 32);
        config.setProperty(Properties.TOUCHSCREEN, 
                Configuration.TOUCHSCREEN_STYLUS);
        config.setProperty(Properties.KEYBOARDHIDDEN, 
                Configuration.KEYBOARDHIDDEN_NO);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 32dpi stylus");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 32dpi stylus"});

        config = makeClassicConfig();
        config.setProperty(Properties.TOUCHSCREEN, 
                Configuration.TOUCHSCREEN_STYLUS);
        config.setProperty(Properties.KEYBOARDHIDDEN, 
                Configuration.KEYBOARDHIDDEN_NO);
        config.setProperty(Properties.KEYBOARD, Configuration.KEYBOARD_12KEY);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple stylus keysexposed");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag stylus keysexposed"});

        config = makeClassicConfig();
        config.setProperty(Properties.KEYBOARDHIDDEN, 
                Configuration.KEYBOARDHIDDEN_NO);
        config.setProperty(Properties.KEYBOARD, Configuration.KEYBOARD_12KEY);
        config.setProperty(Properties.NAVIGATION, 
                Configuration.NAVIGATION_DPAD);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple keysexposed 12key");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag keysexposed 12key"});

        config = makeClassicConfig();
        config.setProperty(Properties.KEYBOARD, Configuration.KEYBOARD_12KEY);
        config.setProperty(Properties.NAVIGATION, 
                Configuration.NAVIGATION_DPAD);
        config.setProperty(Properties.HEIGHT, 63);
        config.setProperty(Properties.WIDTH, 57);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple 12key dpad");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag 12key dpad"});

        config = makeClassicConfig();
        config.setProperty(Properties.NAVIGATION, 
                Configuration.NAVIGATION_DPAD);
        config.setProperty(Properties.HEIGHT, 640);
        config.setProperty(Properties.WIDTH, 400);
        res = config.getResources();
        checkValue(res, R.configVarying.simple, "simple dpad 63x57");
        checkValue(res, R.configVarying.bag,
                R.styleable.TestConfig, new String[]{"bag dpad 63x57"});
    }
    
    @MediumTest
    public void testVersions() {
        // Check that we get the most recent resources that are <= our
        // current version.  Note the special version adjustment, so that
        // during development the resource version is incremented to the
        // next one.
        int vers = android.os.Build.VERSION.SDK_INT;
        if (!"REL".equals(android.os.Build.VERSION.CODENAME)) {
            vers++;
        }
        String expected = "v" + vers + "cur";
        assertEquals(expected, mContext.getResources().getString(R.string.version_cur));
        assertEquals("base",  mContext.getResources().getString(R.string.version_old));
        assertEquals("v3",  mContext.getResources().getString(R.string.version_v3));
    }
}
