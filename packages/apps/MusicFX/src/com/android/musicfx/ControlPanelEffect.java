/*
 * Copyright (C) 2010-2011 The Android Open Source Project
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

package com.android.musicfx;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Common class defines constants to be used by the control panels.
 */
public class ControlPanelEffect {

    private final static String TAG = "MusicFXControlPanelEffect";

    /**
     * Audio session priority
     */
    private static final int PRIORITY = 0;

    /**
     * The control mode specifies if control panel updates effects and preferences or only
     * preferences.
     */
    static enum ControlMode {
        /**
         * Control panel updates effects and preferences. Applicable when audio session is delivered
         * by user.
         */
        CONTROL_EFFECTS,
        /**
         * Control panel only updates preferences. Applicable when there was no audio or invalid
         * session provided by user.
         */
        CONTROL_PREFERENCES
    }

    static enum Key {
        global_enabled, virt_enabled, virt_strength_supported, virt_strength, virt_type, bb_enabled,
        bb_strength, te_enabled, te_strength, avl_enabled, lm_enabled, lm_strength, eq_enabled,
        eq_num_bands, eq_level_range, eq_center_freq, eq_band_level, eq_num_presets, eq_preset_name,
        eq_preset_user_band_level, eq_preset_user_band_level_default,
        eq_preset_opensl_es_band_level, eq_preset_ci_extreme_band_level, eq_current_preset,
        pr_enabled, pr_current_preset
    }

    // Effect/audio session Mappings
    /**
     * Hashmap initial capacity
     */
    private static final int HASHMAP_INITIAL_CAPACITY = 16;
    /**
     * Hashmap load factor
     */
    private static final float HASHMAP_LOAD_FACTOR = 0.75f;
    /**
     * ConcurrentHashMap concurrency level
     */
    private static final int HASHMAP_CONCURRENCY_LEVEL = 2;

    /**
     * Map containing the Virtualizer audio session, effect mappings.
     */
    private static final ConcurrentHashMap<Integer, Virtualizer> mVirtualizerInstances = new ConcurrentHashMap<Integer, Virtualizer>(
            HASHMAP_INITIAL_CAPACITY, HASHMAP_LOAD_FACTOR, HASHMAP_CONCURRENCY_LEVEL);
    /**
     * Map containing the BB audio session, effect mappings.
     */
    private static final ConcurrentHashMap<Integer, BassBoost> mBassBoostInstances = new ConcurrentHashMap<Integer, BassBoost>(
            HASHMAP_INITIAL_CAPACITY, HASHMAP_LOAD_FACTOR, HASHMAP_CONCURRENCY_LEVEL);
    /**
     * Map containing the EQ audio session, effect mappings.
     */
    private static final ConcurrentHashMap<Integer, Equalizer> mEQInstances = new ConcurrentHashMap<Integer, Equalizer>(
            HASHMAP_INITIAL_CAPACITY, HASHMAP_LOAD_FACTOR, HASHMAP_CONCURRENCY_LEVEL);
    /**
     * Map containing the PR audio session, effect mappings.
     */
    private static final ConcurrentHashMap<Integer, PresetReverb> mPresetReverbInstances = new ConcurrentHashMap<Integer, PresetReverb>(
            HASHMAP_INITIAL_CAPACITY, HASHMAP_LOAD_FACTOR, HASHMAP_CONCURRENCY_LEVEL);

    /**
     * Map containing the package name, audio session mappings.
     */
    private static final ConcurrentHashMap<String, Integer> mPackageSessions = new ConcurrentHashMap<String, Integer>(
            HASHMAP_INITIAL_CAPACITY, HASHMAP_LOAD_FACTOR, HASHMAP_CONCURRENCY_LEVEL);

    // Defaults
    final static boolean GLOBAL_ENABLED_DEFAULT = false;
    private final static boolean VIRTUALIZER_ENABLED_DEFAULT = true;
    private final static int VIRTUALIZER_STRENGTH_DEFAULT = 1000;
    private final static boolean BASS_BOOST_ENABLED_DEFAULT = true;
    private final static int BASS_BOOST_STRENGTH_DEFAULT = 667;
    private final static boolean PRESET_REVERB_ENABLED_DEFAULT = false;
    private final static int PRESET_REVERB_CURRENT_PRESET_DEFAULT = 0; // None

    // EQ defaults
    private final static boolean EQUALIZER_ENABLED_DEFAULT = true;
    private final static String EQUALIZER_PRESET_NAME_DEFAULT = "Preset";
    private final static short EQUALIZER_NUMBER_BANDS_DEFAULT = 5;
    private final static short EQUALIZER_NUMBER_PRESETS_DEFAULT = 0;
    private final static short[] EQUALIZER_BAND_LEVEL_RANGE_DEFAULT = { -1500, 1500 };
    private final static int[] EQUALIZER_CENTER_FREQ_DEFAULT = { 60000, 230000, 910000, 3600000,
            14000000 };
    private final static short[] EQUALIZER_PRESET_CIEXTREME_BAND_LEVEL = { 0, 800, 400, 100, 1000 };
    private final static short[] EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT = { 0, 0, 0, 0, 0 };
    private final static short[][] EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT = new short[EQUALIZER_NUMBER_PRESETS_DEFAULT][EQUALIZER_NUMBER_BANDS_DEFAULT];

    // EQ effect properties which are invariable over all EQ effects sessions
    private static short[] mEQBandLevelRange = EQUALIZER_BAND_LEVEL_RANGE_DEFAULT;
    private static short mEQNumBands = EQUALIZER_NUMBER_BANDS_DEFAULT;
    private static int[] mEQCenterFreq = EQUALIZER_CENTER_FREQ_DEFAULT;
    private static short mEQNumPresets = EQUALIZER_NUMBER_PRESETS_DEFAULT;
    private static short[][] mEQPresetOpenSLESBandLevel = EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT;
    private static String[] mEQPresetNames;
    private static boolean mIsEQInitialized = false;
    private final static Object mEQInitLock = new Object();

    /**
     * Default int argument used in methods to see that the arg is a dummy. Used for method
     * overloading.
     */
    private final static int DUMMY_ARGUMENT = -1;

    /**
     * Inits effects preferences for the given context and package name in the control panel. If
     * preferences for the given package name don't exist, they are created and initialized.
     *
     * @param context
     * @param packageName
     * @param audioSession
     *            System wide unique audio session identifier.
     */
    public static void initEffectsPreferences(final Context context, final String packageName,
            final int audioSession) {
        final SharedPreferences prefs = context.getSharedPreferences(packageName,
                Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();
        final ControlMode controlMode = getControlMode(audioSession);

        // init preferences
        try {
            // init global on/off switch
            final boolean isGlobalEnabled = prefs.getBoolean(Key.global_enabled.toString(),
                    GLOBAL_ENABLED_DEFAULT);
            editor.putBoolean(Key.global_enabled.toString(), isGlobalEnabled);
            Log.v(TAG, "isGlobalEnabled = " + isGlobalEnabled);

            // Virtualizer
            final boolean isVIEnabled = prefs.getBoolean(Key.virt_enabled.toString(),
                    VIRTUALIZER_ENABLED_DEFAULT);
            final Virtualizer virt = new Virtualizer(0, audioSession);
            final int vIStrength = prefs.getInt(Key.virt_strength.toString(),
                    virt.getRoundedStrength());
            virt.release();
            editor.putBoolean(Key.virt_enabled.toString(), isVIEnabled);
            editor.putInt(Key.virt_strength.toString(), vIStrength);
            {
                final MediaPlayer mediaPlayer = new MediaPlayer();
                final int session = mediaPlayer.getAudioSessionId();
                Virtualizer virtualizerEffect = null;
                try {
                    virtualizerEffect = new Virtualizer(PRIORITY, session);
                    editor.putBoolean(Key.virt_strength_supported.toString(),
                            virtualizerEffect.getStrengthSupported());
                } finally {
                    if (virtualizerEffect != null) {
                        Log.d(TAG, "Releasing dummy Virtualizer effect");
                        virtualizerEffect.release();
                    }
                    mediaPlayer.release();
                }
            }

            // BassBoost
            final boolean isBBEnabled = prefs.getBoolean(Key.bb_enabled.toString(),
                    BASS_BOOST_ENABLED_DEFAULT);
            final int bBStrength = prefs.getInt(Key.bb_strength.toString(),
                    BASS_BOOST_STRENGTH_DEFAULT);
            editor.putBoolean(Key.bb_enabled.toString(), isBBEnabled);
            editor.putInt(Key.bb_strength.toString(), bBStrength);

            // Equalizer
            synchronized (mEQInitLock) {
                // If EQ is not initialized already create "dummy" audio session created by
                // MediaPlayer and create effect on it to retrieve the invariable EQ properties
                if (!mIsEQInitialized) {
                    final MediaPlayer mediaPlayer = new MediaPlayer();
                    final int session = mediaPlayer.getAudioSessionId();
                    Equalizer equalizerEffect = null;
                    try {
                        Log.d(TAG, "Creating dummy EQ effect on session " + session);
                        equalizerEffect = new Equalizer(PRIORITY, session);

                        mEQBandLevelRange = equalizerEffect.getBandLevelRange();
                        mEQNumBands = equalizerEffect.getNumberOfBands();
                        mEQCenterFreq = new int[mEQNumBands];
                        for (short band = 0; band < mEQNumBands; band++) {
                            mEQCenterFreq[band] = equalizerEffect.getCenterFreq(band);
                        }
                        mEQNumPresets = equalizerEffect.getNumberOfPresets();
                        mEQPresetNames = new String[mEQNumPresets];
                        mEQPresetOpenSLESBandLevel = new short[mEQNumPresets][mEQNumBands];
                        for (short preset = 0; preset < mEQNumPresets; preset++) {
                            mEQPresetNames[preset] = equalizerEffect.getPresetName(preset);
                            equalizerEffect.usePreset(preset);
                            for (short band = 0; band < mEQNumBands; band++) {
                                mEQPresetOpenSLESBandLevel[preset][band] = equalizerEffect
                                        .getBandLevel(band);
                            }
                        }

                        mIsEQInitialized = true;
                    } catch (final IllegalStateException e) {
                        Log.e(TAG, "Equalizer: " + e);
                    } catch (final IllegalArgumentException e) {
                        Log.e(TAG, "Equalizer: " + e);
                    } catch (final UnsupportedOperationException e) {
                        Log.e(TAG, "Equalizer: " + e);
                    } catch (final RuntimeException e) {
                        Log.e(TAG, "Equalizer: " + e);
                    } finally {
                        if (equalizerEffect != null) {
                            Log.d(TAG, "Releasing dummy EQ effect");
                            equalizerEffect.release();
                        }
                        mediaPlayer.release();

                        // When there was a failure set some good defaults
                        if (!mIsEQInitialized) {
                            mEQPresetOpenSLESBandLevel = new short[mEQNumPresets][mEQNumBands];
                            for (short preset = 0; preset < mEQNumPresets; preset++) {
                                // Init preset names to a dummy name
                                mEQPresetNames[preset] = prefs.getString(
                                        Key.eq_preset_name.toString() + preset,
                                        EQUALIZER_PRESET_NAME_DEFAULT + preset);
                                if (preset < EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT.length) {
                                    mEQPresetOpenSLESBandLevel[preset] = Arrays.copyOf(
                                            EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT[preset],
                                            mEQNumBands);
                                }
                            }
                        }
                    }
                }
                editor.putInt(Key.eq_level_range.toString() + 0, mEQBandLevelRange[0]);
                editor.putInt(Key.eq_level_range.toString() + 1, mEQBandLevelRange[1]);
                editor.putInt(Key.eq_num_bands.toString(), mEQNumBands);
                editor.putInt(Key.eq_num_presets.toString(), mEQNumPresets);
                // Resetting the EQ arrays depending on the real # bands with defaults if
                // band < default size else 0 by copying default arrays over new ones
                final short[] eQPresetCIExtremeBandLevel = Arrays.copyOf(
                        EQUALIZER_PRESET_CIEXTREME_BAND_LEVEL, mEQNumBands);
                final short[] eQPresetUserBandLevelDefault = Arrays.copyOf(
                        EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, mEQNumBands);
                // If no preset prefs set use CI EXTREME (= numPresets)
                final short eQPreset = (short) prefs.getInt(Key.eq_current_preset.toString(),
                        mEQNumPresets);
                editor.putInt(Key.eq_current_preset.toString(), eQPreset);
                final short[] bandLevel = new short[mEQNumBands];
                for (short band = 0; band < mEQNumBands; band++) {
                    if (controlMode == ControlMode.CONTROL_PREFERENCES) {
                        if (eQPreset < mEQNumPresets) {
                            // OpenSL ES effect presets
                            bandLevel[band] = mEQPresetOpenSLESBandLevel[eQPreset][band];
                        } else if (eQPreset == mEQNumPresets) {
                            // CI EXTREME
                            bandLevel[band] = eQPresetCIExtremeBandLevel[band];
                        } else {
                            // User
                            bandLevel[band] = (short) prefs.getInt(
                                    Key.eq_preset_user_band_level.toString() + band,
                                    eQPresetUserBandLevelDefault[band]);
                        }
                        editor.putInt(Key.eq_band_level.toString() + band, bandLevel[band]);
                    }
                    editor.putInt(Key.eq_center_freq.toString() + band, mEQCenterFreq[band]);
                    editor.putInt(Key.eq_preset_ci_extreme_band_level.toString() + band,
                            eQPresetCIExtremeBandLevel[band]);
                    editor.putInt(Key.eq_preset_user_band_level_default.toString() + band,
                            eQPresetUserBandLevelDefault[band]);
                }
                for (short preset = 0; preset < mEQNumPresets; preset++) {
                    editor.putString(Key.eq_preset_name.toString() + preset, mEQPresetNames[preset]);
                    for (short band = 0; band < mEQNumBands; band++) {
                        editor.putInt(Key.eq_preset_opensl_es_band_level.toString() + preset + "_"
                                + band, mEQPresetOpenSLESBandLevel[preset][band]);
                    }
                }
            }
            final boolean isEQEnabled = prefs.getBoolean(Key.eq_enabled.toString(),
                    EQUALIZER_ENABLED_DEFAULT);
            editor.putBoolean(Key.eq_enabled.toString(), isEQEnabled);

            // Preset reverb
            final boolean isEnabledPR = prefs.getBoolean(Key.pr_enabled.toString(),
                    PRESET_REVERB_ENABLED_DEFAULT);
            final short presetPR = (short) prefs.getInt(Key.pr_current_preset.toString(),
                    PRESET_REVERB_CURRENT_PRESET_DEFAULT);
            editor.putBoolean(Key.pr_enabled.toString(), isEnabledPR);
            editor.putInt(Key.pr_current_preset.toString(), presetPR);

            editor.commit();
        } catch (final RuntimeException e) {
            Log.e(TAG, "initEffectsPreferences: processingEnabled: " + e);
        }
    }

    /**
     * Gets the effect control mode based on the given audio session in the control panel. Control
     * mode defines if the control panel is controlling effects and/or preferences
     *
     * @param audioSession
     *            System wide unique audio session identifier.
     * @return effect control mode
     */
    public static ControlMode getControlMode(final int audioSession) {
        if (audioSession == AudioEffect.ERROR_BAD_VALUE) {
            return ControlMode.CONTROL_PREFERENCES;
        }
        return ControlMode.CONTROL_EFFECTS;
    }

    /**
     * Sets boolean parameter to value for given key
     *
     * @param context
     * @param packageName
     * @param audioSession
     *            System wide unique audio session identifier.
     * @param key
     * @param value
     */
    public static void setParameterBoolean(final Context context, final String packageName,
            final int audioSession, final Key key, final boolean value) {
        try {
            final SharedPreferences prefs = context.getSharedPreferences(packageName,
                    Context.MODE_PRIVATE);
            final ControlMode controlMode = getControlMode(audioSession);
            boolean enabled = value;

            // Global on/off
            if (key == Key.global_enabled) {
                boolean processingEnabled = false;
                if (value == true) {
                    // enable all with respect to preferences
                    if (controlMode == ControlMode.CONTROL_EFFECTS) {
                        final Virtualizer virtualizerEffect = getVirtualizerEffect(audioSession);
                        if (virtualizerEffect != null) {
                            virtualizerEffect.setEnabled(prefs.getBoolean(
                                    Key.virt_enabled.toString(), VIRTUALIZER_ENABLED_DEFAULT));
                            int defaultstrength = virtualizerEffect.getRoundedStrength();
                            final int vIStrength = prefs.getInt(Key.virt_strength.toString(),
                                    defaultstrength);
                            setParameterInt(context, packageName,
                                    audioSession, Key.virt_strength, vIStrength);
                        }
                        final BassBoost bassBoostEffect = getBassBoostEffect(audioSession);
                        if (bassBoostEffect != null) {
                            bassBoostEffect.setEnabled(prefs.getBoolean(Key.bb_enabled.toString(),
                                    BASS_BOOST_ENABLED_DEFAULT));
                            final int bBStrength = prefs.getInt(Key.bb_strength.toString(),
                                    BASS_BOOST_STRENGTH_DEFAULT);
                            setParameterInt(context, packageName,
                                    audioSession, Key.bb_strength, bBStrength);
                        }
                        final Equalizer equalizerEffect = getEqualizerEffect(audioSession);
                        if (equalizerEffect != null) {
                            equalizerEffect.setEnabled(prefs.getBoolean(Key.eq_enabled.toString(),
                                    EQUALIZER_ENABLED_DEFAULT));
                            final int[] bandLevels = getParameterIntArray(context,
                                    packageName, audioSession, Key.eq_band_level);
                            final int len = bandLevels.length;
                            for (short band = 0; band < len; band++) {
                                final int level = bandLevels[band];
                                setParameterInt(context, packageName,
                                        audioSession, Key.eq_band_level, level, band);
                            }
                        }
                        // XXX: Preset Reverb not used for the moment, so commented out the effect
                        // creation to not use MIPS
                        // final PresetReverb presetReverbEffect =
                        // getPresetReverbEffect(audioSession);
                        // if (presetReverbEffect != null) {
                        // presetReverbEffect.setEnabled(prefs.getBoolean(
                        // Key.pr_enabled.toString(), PRESET_REVERB_ENABLED_DEFAULT));
                        // }
                    }

                    processingEnabled = true;
                    Log.v(TAG, "processingEnabled=" + processingEnabled);

                } else {
                    // disable all
                    if (controlMode == ControlMode.CONTROL_EFFECTS) {
                        final Virtualizer virtualizerEffect = getVirtualizerEffectNoCreate(audioSession);
                        if (virtualizerEffect != null) {
                            mVirtualizerInstances.remove(audioSession, virtualizerEffect);
                            virtualizerEffect.setEnabled(false);
                            virtualizerEffect.release();
                        }
                        final BassBoost bassBoostEffect = getBassBoostEffectNoCreate(audioSession);
                        if (bassBoostEffect != null) {
                            mBassBoostInstances.remove(audioSession, bassBoostEffect);
                            bassBoostEffect.setEnabled(false);
                            bassBoostEffect.release();
                        }
                        final Equalizer equalizerEffect = getEqualizerEffectNoCreate(audioSession);
                        if (equalizerEffect != null) {
                            mEQInstances.remove(audioSession, equalizerEffect);
                            equalizerEffect.setEnabled(false);
                            equalizerEffect.release();
                        }
                        // XXX: Preset Reverb not used for the moment, so commented out the effect
                        // creation to not use MIPS
                        // final PresetReverb presetReverbEffect =
                        // getPresetReverbEffect(audioSession);
                        // if (presetReverbEffect != null) {
                        // presetReverbEffect.setEnabled(false);
                        // }
                    }

                    processingEnabled = false;
                    Log.v(TAG, "processingEnabled=" + processingEnabled);
                }
                enabled = processingEnabled;
            } else if (controlMode == ControlMode.CONTROL_EFFECTS) {
                final boolean isGlobalEnabled = prefs.getBoolean(Key.global_enabled.toString(),
                        GLOBAL_ENABLED_DEFAULT);
                if (isGlobalEnabled == true) {
                    // Set effect parameters
                    switch (key) {

                    case global_enabled:
                        // Global, already handled, to get out error free
                        break;

                    // Virtualizer
                    case virt_enabled:
                        final Virtualizer virtualizerEffect = getVirtualizerEffect(audioSession);
                        if (virtualizerEffect != null) {
                            virtualizerEffect.setEnabled(value);
                            enabled = virtualizerEffect.getEnabled();
                        }
                        break;

                    // BassBoost
                    case bb_enabled:
                        final BassBoost bassBoostEffect = getBassBoostEffect(audioSession);
                        if (bassBoostEffect != null) {
                            bassBoostEffect.setEnabled(value);
                            enabled = bassBoostEffect.getEnabled();
                        }
                        break;

                    // Equalizer
                    case eq_enabled:
                        final Equalizer equalizerEffect = getEqualizerEffect(audioSession);
                        if (equalizerEffect != null) {
                            equalizerEffect.setEnabled(value);
                            enabled = equalizerEffect.getEnabled();
                        }
                        break;

                    // PresetReverb
                    case pr_enabled:
                        // XXX: Preset Reverb not used for the moment, so commented out the effect
                        // creation to not use MIPS
                        // final PresetReverb presetReverbEffect =
                        // getPresetReverbEffect(audioSession);
                        // if (presetReverbEffect != null) {
                        // presetReverbEffect.setEnabled(value);
                        // enabled = presetReverbEffect.getEnabled();
                        // }
                        break;

                    default:
                        Log.e(TAG, "Unknown/unsupported key " + key);
                        return;
                    }
                }

            }

            // Set preferences
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(key.toString(), enabled);
            editor.commit();

        } catch (final RuntimeException e) {
            Log.e(TAG, "setParameterBoolean: " + key + "; " + value + "; " + e);
        }
    }

    /**
     * Gets boolean parameter for given key
     *
     * @param context
     * @param packageName
     * @param audioSession
     *            System wide unique audio session identifier.
     * @param key
     * @return parameter value
     */
    public static Boolean getParameterBoolean(final Context context, final String packageName,
            final int audioSession, final Key key) {
        final SharedPreferences prefs = context.getSharedPreferences(packageName,
                Context.MODE_PRIVATE);
        boolean value = false;

        try {
            value = prefs.getBoolean(key.toString(), value);
        } catch (final RuntimeException e) {
            Log.e(TAG, "getParameterBoolean: " + key + "; " + value + "; " + e);
        }

        return value;

    }

    /**
     * Sets int parameter for given key and value arg0, arg1
     *
     * @param context
     * @param packageName
     * @param audioSession
     *            System wide unique audio session identifier.
     * @param key
     * @param arg0
     * @param arg1
     */
    public static void setParameterInt(final Context context, final String packageName,
            final int audioSession, final Key key, final int arg0, final int arg1) {
        String strKey = key.toString();
        int value = arg0;

        try {
            final SharedPreferences prefs = context.getSharedPreferences(packageName,
                    Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = prefs.edit();
            final ControlMode controlMode = getControlMode(audioSession);

            // Set effect parameters
            if (controlMode == ControlMode.CONTROL_EFFECTS) {

                switch (key) {

                // Virtualizer
                case virt_strength: {
                    final Virtualizer virtualizerEffect = getVirtualizerEffect(audioSession);
                    if (virtualizerEffect != null) {
                        virtualizerEffect.setStrength((short) value);
                        value = virtualizerEffect.getRoundedStrength();
                    }
                    break;
                }
                    // BassBoost
                case bb_strength: {
                    final BassBoost bassBoostEffect = getBassBoostEffect(audioSession);
                    if (bassBoostEffect != null) {
                        bassBoostEffect.setStrength((short) value);
                        value = bassBoostEffect.getRoundedStrength();
                    }
                    break;
                }
                    // Equalizer
                case eq_band_level: {
                    if (arg1 == DUMMY_ARGUMENT) {
                        throw new IllegalArgumentException("Dummy arg passed.");
                    }
                    final short band = (short) arg1;
                    strKey = strKey + band;
                    final Equalizer equalizerEffect = getEqualizerEffect(audioSession);
                    if (equalizerEffect != null) {
                        equalizerEffect.setBandLevel(band, (short) value);
                        value = equalizerEffect.getBandLevel(band);
                        // save band level in User preset
                        editor.putInt(Key.eq_preset_user_band_level.toString() + band, value);
                    }
                    break;
                }
                case eq_current_preset: {
                    final Equalizer equalizerEffect = getEqualizerEffect(audioSession);
                    if (equalizerEffect != null) {
                        final short preset = (short) value;
                        final int numBands = prefs.getInt(Key.eq_num_bands.toString(),
                                EQUALIZER_NUMBER_BANDS_DEFAULT);
                        final int numPresets = prefs.getInt(Key.eq_num_presets.toString(),
                                EQUALIZER_NUMBER_PRESETS_DEFAULT);

                        if (preset < numPresets) {
                            // OpenSL ES EQ Effect presets
                            equalizerEffect.usePreset(preset);
                            value = equalizerEffect.getCurrentPreset();
                        } else {
                            final short[] eQPresetCIExtremeBandLevelDefault = Arrays.copyOf(
                                    EQUALIZER_PRESET_CIEXTREME_BAND_LEVEL, numBands);
                            final short[] eQPresetUserBandLevelDefault = Arrays.copyOf(
                                    EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, numBands);
                            // Set the band levels manually for custom presets
                            for (short band = 0; band < numBands; band++) {
                                short bandLevel = 0;
                                if (preset == numPresets) {
                                    // CI EXTREME
                                    bandLevel = (short) prefs.getInt(
                                            Key.eq_preset_ci_extreme_band_level.toString() + band,
                                            eQPresetCIExtremeBandLevelDefault[band]);
                                } else {
                                    // User
                                    bandLevel = (short) prefs.getInt(
                                            Key.eq_preset_user_band_level.toString() + band,
                                            eQPresetUserBandLevelDefault[band]);
                                }
                                equalizerEffect.setBandLevel(band, bandLevel);
                            }
                        }

                        // update band levels
                        for (short band = 0; band < numBands; band++) {
                            final short level = equalizerEffect.getBandLevel(band);
                            editor.putInt(Key.eq_band_level.toString() + band, level);
                        }
                    }
                    break;
                }
                case eq_preset_user_band_level:
                    // Fall through
                case eq_preset_user_band_level_default:
                    // Fall through
                case eq_preset_ci_extreme_band_level: {
                    if (arg1 == DUMMY_ARGUMENT) {
                        throw new IllegalArgumentException("Dummy arg passed.");
                    }
                    final short band = (short) arg1;
                    strKey = strKey + band;
                    break;
                }
                case pr_current_preset:
                    // XXX: Preset Reverb not used for the moment, so commented out the effect
                    // creation to not use MIPS
                    // final PresetReverb presetReverbEffect = getPresetReverbEffect(audioSession);
                    // if (presetReverbEffect != null) {
                    // presetReverbEffect.setPreset((short) value);
                    // value = presetReverbEffect.getPreset();
                    // }
                    break;
                default:
                    Log.e(TAG, "setParameterInt: Unknown/unsupported key " + key);
                    return;
                }
            } else {
                switch (key) {
                // Virtualizer
                case virt_strength:
                    // Do nothing
                    break;
                case virt_type:
                    // Do nothing
                    break;

                // BassBoost
                case bb_strength:
                    // Do nothing
                    break;

                // Equalizer
                case eq_band_level: {
                    if (arg1 == DUMMY_ARGUMENT) {
                        throw new IllegalArgumentException("Dummy arg passed.");
                    }
                    final short band = (short) arg1;
                    strKey = strKey + band;

                    editor.putInt(Key.eq_preset_user_band_level.toString() + band, value);
                    break;
                }
                case eq_current_preset: {
                    final short preset = (short) value;
                    final int numBands = prefs.getInt(Key.eq_num_bands.toString(),
                            EQUALIZER_NUMBER_BANDS_DEFAULT);
                    final int numPresets = prefs.getInt(Key.eq_num_presets.toString(),
                            EQUALIZER_NUMBER_PRESETS_DEFAULT);

                    final short[][] eQPresetOpenSLESBandLevelDefault = Arrays.copyOf(
                            EQUALIZER_PRESET_OPENSL_ES_BAND_LEVEL_DEFAULT, numBands);
                    final short[] eQPresetCIExtremeBandLevelDefault = Arrays.copyOf(
                            EQUALIZER_PRESET_CIEXTREME_BAND_LEVEL, numBands);
                    final short[] eQPresetUserBandLevelDefault = Arrays.copyOf(
                            EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, numBands);
                    for (short band = 0; band < numBands; band++) {
                        short bandLevel = 0;
                        if (preset < numPresets) {
                            // OpenSL ES EQ Effect presets
                            bandLevel = (short) prefs.getInt(
                                    Key.eq_preset_opensl_es_band_level.toString() + preset + "_"
                                            + band, eQPresetOpenSLESBandLevelDefault[preset][band]);
                        } else if (preset == numPresets) {
                            // CI EXTREME
                            bandLevel = (short) prefs.getInt(
                                    Key.eq_preset_ci_extreme_band_level.toString() + band,
                                    eQPresetCIExtremeBandLevelDefault[band]);
                        } else {
                            // User
                            bandLevel = (short) prefs.getInt(
                                    Key.eq_preset_user_band_level.toString() + band,
                                    eQPresetUserBandLevelDefault[band]);
                        }
                        editor.putInt(Key.eq_band_level.toString() + band, bandLevel);
                    }
                    break;
                }
                case eq_preset_user_band_level:
                    // Fall through
                case eq_preset_user_band_level_default:
                    // Fall through
                case eq_preset_ci_extreme_band_level: {
                    if (arg1 == DUMMY_ARGUMENT) {
                        throw new IllegalArgumentException("Dummy arg passed.");
                    }
                    final short band = (short) arg1;
                    strKey = strKey + band;
                    break;
                }
                case pr_current_preset:
                    // Do nothing
                    break;
                default:
                    Log.e(TAG, "setParameterInt: Unknown/unsupported key " + key);
                    return;
                }
            }

            // Set preferences
            editor.putInt(strKey, value);
            editor.apply();

        } catch (final RuntimeException e) {
            Log.e(TAG, "setParameterInt: " + key + "; " + arg0 + "; " + arg1 + "; " + e);
        }

    }

    /**
     * Sets int parameter for given key and value arg
     *
     * @param context
     * @param packageName
     * @param audioSession
     *            System wide unique audio session identifier.
     * @param key
     * @param arg
     */
    public static void setParameterInt(final Context context, final String packageName,
            final int audioSession, final Key key, final int arg) {
        setParameterInt(context, packageName, audioSession, key, arg, DUMMY_ARGUMENT);
    }

    /**
     * Gets int parameter given key
     *
     * @param context
     * @param packageName
     * @param audioSession
     *            System wide unique audio session identifier.
     * @param key
     * @return parameter value
     */
    public static int getParameterInt(final Context context, final String packageName,
            final int audioSession, final String key) {
        int value = 0;

        try {
            final SharedPreferences prefs = context.getSharedPreferences(packageName,
                    Context.MODE_PRIVATE);
            value = prefs.getInt(key, value);
        } catch (final RuntimeException e) {
            Log.e(TAG, "getParameterInt: " + key + "; " + e);
        }

        return value;
    }

    /**
     * Gets int parameter given key
     *
     * @param context
     * @param packageName
     * @param audioSession
     *            System wide unique audio session identifier.
     * @param key
     * @return parameter value
     */
    public static int getParameterInt(final Context context, final String packageName,
            final int audioSession, final Key key) {
        return getParameterInt(context, packageName, audioSession, key.toString());
    }

    /**
     * Gets int parameter given key and arg
     *
     * @param context
     * @param packageName
     * @param audioSession
     *            System wide unique audio session identifier.
     * @param audioSession
     * @param key
     * @param arg
     * @return parameter value
     */
    public static int getParameterInt(final Context context, final String packageName,
            final int audioSession, final Key key, final int arg) {
        return getParameterInt(context, packageName, audioSession, key.toString() + arg);
    }

    /**
     * Gets int parameter given key, arg0 and arg1
     *
     * @param context
     * @param packageName
     * @param audioSession
     *            System wide unique audio session identifier.
     * @param audioSession
     * @param key
     * @param arg0
     * @param arg1
     * @return parameter value
     */
    public static int getParameterInt(final Context context, final String packageName,
            final int audioSession, final Key key, final int arg0, final int arg1) {
        return getParameterInt(context, packageName, audioSession, key.toString() + arg0 + "_"
                + arg1);
    }

    /**
     * Gets integer array parameter given key. Returns null if not found.
     *
     * @param context
     * @param packageName
     * @param audioSession
     *            System wide unique audio session identifier.
     * @param key
     * @return parameter value array
     */
    public static int[] getParameterIntArray(final Context context, final String packageName,
            final int audioSession, final Key key) {
        final SharedPreferences prefs = context.getSharedPreferences(packageName,
                Context.MODE_PRIVATE);

        int[] intArray = null;
        try {
            // Get effect parameters
            switch (key) {
            case eq_level_range: {
                intArray = new int[2];
                break;
            }
            case eq_center_freq:
                // Fall through
            case eq_band_level:
                // Fall through
            case eq_preset_user_band_level:
                // Fall through
            case eq_preset_user_band_level_default:
                // Fall through
            case eq_preset_ci_extreme_band_level: {
                final int numBands = prefs.getInt(Key.eq_num_bands.toString(), 0);
                intArray = new int[numBands];
                break;
            }
            default:
                Log.e(TAG, "getParameterIntArray: Unknown/unsupported key " + key);
                return null;
            }

            for (int i = 0; i < intArray.length; i++) {
                intArray[i] = prefs.getInt(key.toString() + i, 0);
            }

        } catch (final RuntimeException e) {
            Log.e(TAG, "getParameterIntArray: " + key + "; " + e);
        }

        return intArray;
    }

    /**
     * Gets string parameter given key. Returns empty string if not found.
     *
     * @param context
     * @param packageName
     * @param audioSession
     *            System wide unique audio session identifier.
     * @param key
     * @return parameter value
     */
    public static String getParameterString(final Context context, final String packageName,
            final int audioSession, final String key) {
        String value = "";
        try {
            final SharedPreferences prefs = context.getSharedPreferences(packageName,
                    Context.MODE_PRIVATE);

            // Get effect parameters
            value = prefs.getString(key, value);

        } catch (final RuntimeException e) {
            Log.e(TAG, "getParameterString: " + key + "; " + e);
        }

        return value;
    }

    /**
     * Gets string parameter given key.
     *
     * @param context
     * @param packageName
     * @param audioSession
     *            System wide unique audio session identifier.
     * @param key
     * @return parameter value
     */
    public static String getParameterString(final Context context, final String packageName,
            final int audioSession, final Key key) {
        return getParameterString(context, packageName, audioSession, key.toString());
    }

    /**
     * Gets string parameter given key and arg.
     *
     * @param context
     * @param packageName
     * @param audioSession
     *            System wide unique audio session identifier.
     * @param args
     * @return parameter value
     */
    public static String getParameterString(final Context context, final String packageName,
            final int audioSession, final Key key, final int arg) {
        return getParameterString(context, packageName, audioSession, key.toString() + arg);
    }

    /**
     * Opens/initializes the effects session for the given audio session with preferences linked to
     * the given package name and context.
     *
     * @param context
     * @param packageName
     * @param audioSession
     *            System wide unique audio session identifier.
     */
    public static void openSession(final Context context, final String packageName,
            final int audioSession) {
        Log.v(TAG, "openSession(" + context + ", " + packageName + ", " + audioSession + ")");
        final String methodTag = "openSession: ";

        // init preferences
        final SharedPreferences prefs = context.getSharedPreferences(packageName,
                Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();

        final boolean isGlobalEnabled = prefs.getBoolean(Key.global_enabled.toString(),
                GLOBAL_ENABLED_DEFAULT);
        editor.putBoolean(Key.global_enabled.toString(), isGlobalEnabled);

        if (!isGlobalEnabled) {
            return;
        }

        // Manage audioSession information

        // Retrieve AudioSession Id from map
        boolean isExistingAudioSession = false;

        try {
            final Integer currentAudioSession = mPackageSessions.putIfAbsent(packageName,
                    audioSession);
            if (currentAudioSession != null) {
                // Compare with passed argument
                if (currentAudioSession == audioSession) {
                    // FIXME: Normally, we should exit the function here
                    // BUT: we have to take care of the virtualizer because of
                    // a bug in the Android Effects Framework
                    // editor.commit();
                    // return;
                    isExistingAudioSession = true;
                } else {
                    closeSession(context, packageName, currentAudioSession);
                }
            }
        } catch (final NullPointerException e) {
            Log.e(TAG, methodTag + e);
            editor.commit();
            return;
        }

        // Because the audioSession is new, get effects & settings from shared preferences

        // Virtualizer
        // create effect
        final Virtualizer virtualizerEffect = getVirtualizerEffect(audioSession);
        {
            final String errorTag = methodTag + "Virtualizer error: ";

            try {
                // read parameters
                final boolean isEnabled = prefs.getBoolean(Key.virt_enabled.toString(),
                        VIRTUALIZER_ENABLED_DEFAULT);
                int defaultstrength = isExistingAudioSession ? VIRTUALIZER_STRENGTH_DEFAULT :
                    virtualizerEffect.getRoundedStrength();
                final int strength = prefs.getInt(Key.virt_strength.toString(), defaultstrength);
                // init settings
                Virtualizer.Settings settings = new Virtualizer.Settings("Virtualizer;strength="
                        + strength);

                virtualizerEffect.setProperties(settings);

                // set parameters
                if (isGlobalEnabled == true) {
                    virtualizerEffect.setEnabled(isEnabled);
                } else {
                    virtualizerEffect.setEnabled(false);
                }

                // get parameters
                settings = virtualizerEffect.getProperties();
                Log.v(TAG, "Parameters: " + settings.toString() + ";enabled=" + isEnabled);

                // update preferences
                editor.putBoolean(Key.virt_enabled.toString(), isEnabled);
                editor.putInt(Key.virt_strength.toString(), settings.strength);
            } catch (final RuntimeException e) {
                Log.e(TAG, errorTag + e);
            }
        }

        // In case of an existing audio session
        // Exit after the virtualizer has been re-enabled

        if (isExistingAudioSession) {
            editor.apply();
            return;
        }

        // BassBoost
        // create effect
        final BassBoost bassBoostEffect = getBassBoostEffect(audioSession);
        {
            final String errorTag = methodTag + "BassBoost error: ";

            try {
                // read parameters
                final boolean isEnabled = prefs.getBoolean(Key.bb_enabled.toString(),
                        BASS_BOOST_ENABLED_DEFAULT);
                final int strength = prefs.getInt(Key.bb_strength.toString(),
                        BASS_BOOST_STRENGTH_DEFAULT);

                // init settings
                BassBoost.Settings settings = new BassBoost.Settings("BassBoost;strength="
                        + strength);

                bassBoostEffect.setProperties(settings);

                // set parameters
                if (isGlobalEnabled == true) {
                    bassBoostEffect.setEnabled(isEnabled);
                } else {
                    bassBoostEffect.setEnabled(false);
                }

                // get parameters
                settings = bassBoostEffect.getProperties();
                Log.v(TAG, "Parameters: " + settings.toString() + ";enabled=" + isEnabled);

                // update preferences
                editor.putBoolean(Key.bb_enabled.toString(), isEnabled);
                editor.putInt(Key.bb_strength.toString(), settings.strength);
            } catch (final RuntimeException e) {
                Log.e(TAG, errorTag + e);
            }
        }

        // Equalizer
        // create effect
        final Equalizer equalizerEffect = getEqualizerEffect(audioSession);
        {
            final String errorTag = methodTag + "Equalizer error: ";

            try {
                final short eQNumBands;
                final short[] bandLevel;
                final int[] eQCenterFreq;
                final short eQNumPresets;
                final String[] eQPresetNames;
                short eQPreset;
                synchronized (mEQInitLock) {
                    // read parameters
                    mEQBandLevelRange = equalizerEffect.getBandLevelRange();
                    mEQNumBands = equalizerEffect.getNumberOfBands();
                    mEQCenterFreq = new int[mEQNumBands];
                    mEQNumPresets = equalizerEffect.getNumberOfPresets();
                    mEQPresetNames = new String[mEQNumPresets];

                    for (short preset = 0; preset < mEQNumPresets; preset++) {
                        mEQPresetNames[preset] = equalizerEffect.getPresetName(preset);
                        editor.putString(Key.eq_preset_name.toString() + preset,
                                mEQPresetNames[preset]);
                    }

                    editor.putInt(Key.eq_level_range.toString() + 0, mEQBandLevelRange[0]);
                    editor.putInt(Key.eq_level_range.toString() + 1, mEQBandLevelRange[1]);
                    editor.putInt(Key.eq_num_bands.toString(), mEQNumBands);
                    editor.putInt(Key.eq_num_presets.toString(), mEQNumPresets);
                    // Resetting the EQ arrays depending on the real # bands with defaults if band <
                    // default size else 0 by copying default arrays over new ones
                    final short[] eQPresetCIExtremeBandLevel = Arrays.copyOf(
                            EQUALIZER_PRESET_CIEXTREME_BAND_LEVEL, mEQNumBands);
                    final short[] eQPresetUserBandLevelDefault = Arrays.copyOf(
                            EQUALIZER_PRESET_USER_BAND_LEVEL_DEFAULT, mEQNumBands);
                    // If no preset prefs set use CI EXTREME (= numPresets)
                    eQPreset = (short) prefs
                            .getInt(Key.eq_current_preset.toString(), mEQNumPresets);
                    if (eQPreset < mEQNumPresets) {
                        // OpenSL ES effect presets
                        equalizerEffect.usePreset(eQPreset);
                        eQPreset = equalizerEffect.getCurrentPreset();
                    } else {
                        for (short band = 0; band < mEQNumBands; band++) {
                            short level = 0;
                            if (eQPreset == mEQNumPresets) {
                                // CI EXTREME
                                level = eQPresetCIExtremeBandLevel[band];
                            } else {
                                // User
                                level = (short) prefs.getInt(
                                        Key.eq_preset_user_band_level.toString() + band,
                                        eQPresetUserBandLevelDefault[band]);
                            }
                            equalizerEffect.setBandLevel(band, level);
                        }
                    }
                    editor.putInt(Key.eq_current_preset.toString(), eQPreset);

                    bandLevel = new short[mEQNumBands];
                    for (short band = 0; band < mEQNumBands; band++) {
                        mEQCenterFreq[band] = equalizerEffect.getCenterFreq(band);
                        bandLevel[band] = equalizerEffect.getBandLevel(band);

                        editor.putInt(Key.eq_band_level.toString() + band, bandLevel[band]);
                        editor.putInt(Key.eq_center_freq.toString() + band, mEQCenterFreq[band]);
                        editor.putInt(Key.eq_preset_ci_extreme_band_level.toString() + band,
                                eQPresetCIExtremeBandLevel[band]);
                        editor.putInt(Key.eq_preset_user_band_level_default.toString() + band,
                                eQPresetUserBandLevelDefault[band]);
                    }

                    eQNumBands = mEQNumBands;
                    eQCenterFreq = mEQCenterFreq;
                    eQNumPresets = mEQNumPresets;
                    eQPresetNames = mEQPresetNames;
                }

                final boolean isEnabled = prefs.getBoolean(Key.eq_enabled.toString(),
                        EQUALIZER_ENABLED_DEFAULT);
                editor.putBoolean(Key.eq_enabled.toString(), isEnabled);
                if (isGlobalEnabled == true) {
                    equalizerEffect.setEnabled(isEnabled);
                } else {
                    equalizerEffect.setEnabled(false);
                }

                // dump
                Log.v(TAG, "Parameters: Equalizer");
                Log.v(TAG, "bands=" + eQNumBands);
                String str = "levels=";
                for (short band = 0; band < eQNumBands; band++) {
                    str = str + bandLevel[band] + "; ";
                }
                Log.v(TAG, str);
                str = "center=";
                for (short band = 0; band < eQNumBands; band++) {
                    str = str + eQCenterFreq[band] + "; ";
                }
                Log.v(TAG, str);
                str = "presets=";
                for (short preset = 0; preset < eQNumPresets; preset++) {
                    str = str + eQPresetNames[preset] + "; ";
                }
                Log.v(TAG, str);
                Log.v(TAG, "current=" + eQPreset);
            } catch (final RuntimeException e) {
                Log.e(TAG, errorTag + e);
            }
        }

        // XXX: Preset Reverb not used for the moment, so commented out the effect creation to not
        // use MIPS left in the code for (future) reference.
        // Preset reverb
        // create effect
        // final PresetReverb presetReverbEffect = getPresetReverbEffect(audioSession);
        // {
        // final String errorTag = methodTag + "PresetReverb error: ";
        //
        // try {
        // // read parameters
        // final boolean isEnabled = prefs.getBoolean(Key.pr_enabled.toString(),
        // PRESET_REVERB_ENABLED_DEFAULT);
        // final short preset = (short) prefs.getInt(Key.pr_current_preset.toString(),
        // PRESET_REVERB_CURRENT_PRESET_DEFAULT);
        //
        // // init settings
        // PresetReverb.Settings settings = new PresetReverb.Settings("PresetReverb;preset="
        // + preset);
        //
        // // read/update preferences
        // presetReverbEffect.setProperties(settings);
        //
        // // set parameters
        // if (isGlobalEnabled == true) {
        // presetReverbEffect.setEnabled(isEnabled);
        // } else {
        // presetReverbEffect.setEnabled(false);
        // }
        //
        // // get parameters
        // settings = presetReverbEffect.getProperties();
        // Log.v(TAG, "Parameters: " + settings.toString() + ";enabled=" + isEnabled);
        //
        // // update preferences
        // editor.putBoolean(Key.pr_enabled.toString(), isEnabled);
        // editor.putInt(Key.pr_current_preset.toString(), settings.preset);
        // } catch (final RuntimeException e) {
        // Log.e(TAG, errorTag + e);
        // }
        // }
        editor.commit();
    }

    /**
     * Closes the audio session (release effects) for the given session
     *
     * @param context
     * @param packageName
     * @param audioSession
     *            System wide unique audio session identifier.
     */
    public static void closeSession(final Context context, final String packageName,
            final int audioSession) {
        Log.v(TAG, "closeSession(" + context + ", " + packageName + ", " + audioSession + ")");

        // PresetReverb
        final PresetReverb presetReverb = mPresetReverbInstances.remove(audioSession);
        if (presetReverb != null) {
            presetReverb.release();
        }
        // Equalizer
        final Equalizer equalizer = mEQInstances.remove(audioSession);
        if (equalizer != null) {
            equalizer.release();
        }
        // BassBoost
        final BassBoost bassBoost = mBassBoostInstances.remove(audioSession);
        if (bassBoost != null) {
            bassBoost.release();
        }
        // Virtualizer
        final Virtualizer virtualizer = mVirtualizerInstances.remove(audioSession);
        if (virtualizer != null) {
            virtualizer.release();
        }

        mPackageSessions.remove(packageName);
    }

    /**
     * Enables or disables all effects (global enable/disable) for a given context, package name and
     * audio session. It sets/inits the control mode and preferences and then sets the global
     * enabled parameter.
     *
     * @param context
     * @param packageName
     * @param audioSession
     *            System wide unique audio session identifier.
     * @param enabled
     */
    public static void setEnabledAll(final Context context, final String packageName,
            final int audioSession, final boolean enabled) {
        initEffectsPreferences(context, packageName, audioSession);
        setParameterBoolean(context, packageName, audioSession, Key.global_enabled, enabled);
    }

    /**
     * Gets the virtualizer effect for the given audio session. If the effect on the session doesn't
     * exist yet, create it and add to collection.
     *
     * @param audioSession
     *            System wide unique audio session identifier.
     * @return virtualizerEffect
     */
    private static Virtualizer getVirtualizerEffectNoCreate(final int audioSession) {
        return mVirtualizerInstances.get(audioSession);
    }
    private static Virtualizer getVirtualizerEffect(final int audioSession) {
        Virtualizer virtualizerEffect = getVirtualizerEffectNoCreate(audioSession);
        if (virtualizerEffect == null) {
            try {
                final Virtualizer newVirtualizerEffect = new Virtualizer(PRIORITY, audioSession);
                virtualizerEffect = mVirtualizerInstances.putIfAbsent(audioSession,
                        newVirtualizerEffect);
                if (virtualizerEffect == null) {
                    // put succeeded, use new value
                    virtualizerEffect = newVirtualizerEffect;
                }
            } catch (final IllegalArgumentException e) {
                Log.e(TAG, "Virtualizer: " + e);
            } catch (final UnsupportedOperationException e) {
                Log.e(TAG, "Virtualizer: " + e);
            } catch (final RuntimeException e) {
                Log.e(TAG, "Virtualizer: " + e);
            }
        }
        return virtualizerEffect;
    }

    /**
     * Gets the bass boost effect for the given audio session. If the effect on the session doesn't
     * exist yet, create it and add to collection.
     *
     * @param audioSession
     *            System wide unique audio session identifier.
     * @return bassBoostEffect
     */
    private static BassBoost getBassBoostEffectNoCreate(final int audioSession) {
        return mBassBoostInstances.get(audioSession);
    }
    private static BassBoost getBassBoostEffect(final int audioSession) {

        BassBoost bassBoostEffect = getBassBoostEffectNoCreate(audioSession);
        if (bassBoostEffect == null) {
            try {
                final BassBoost newBassBoostEffect = new BassBoost(PRIORITY, audioSession);
                bassBoostEffect = mBassBoostInstances.putIfAbsent(audioSession, newBassBoostEffect);
                if (bassBoostEffect == null) {
                    // put succeeded, use new value
                    bassBoostEffect = newBassBoostEffect;
                }
            } catch (final IllegalArgumentException e) {
                Log.e(TAG, "BassBoost: " + e);
            } catch (final UnsupportedOperationException e) {
                Log.e(TAG, "BassBoost: " + e);
            } catch (final RuntimeException e) {
                Log.e(TAG, "BassBoost: " + e);
            }
        }
        return bassBoostEffect;
    }

    /**
     * Gets the equalizer effect for the given audio session. If the effect on the session doesn't
     * exist yet, create it and add to collection.
     *
     * @param audioSession
     *            System wide unique audio session identifier.
     * @return equalizerEffect
     */
    private static Equalizer getEqualizerEffectNoCreate(final int audioSession) {
        return mEQInstances.get(audioSession);
    }
    private static Equalizer getEqualizerEffect(final int audioSession) {
        Equalizer equalizerEffect = getEqualizerEffectNoCreate(audioSession);
        if (equalizerEffect == null) {
            try {
                final Equalizer newEqualizerEffect = new Equalizer(PRIORITY, audioSession);
                equalizerEffect = mEQInstances.putIfAbsent(audioSession, newEqualizerEffect);
                if (equalizerEffect == null) {
                    // put succeeded, use new value
                    equalizerEffect = newEqualizerEffect;
                }
            } catch (final IllegalArgumentException e) {
                Log.e(TAG, "Equalizer: " + e);
            } catch (final UnsupportedOperationException e) {
                Log.e(TAG, "Equalizer: " + e);
            } catch (final RuntimeException e) {
                Log.e(TAG, "Equalizer: " + e);
            }
        }
        return equalizerEffect;
    }

    // XXX: Preset Reverb not used for the moment, so commented out the effect creation to not
    // use MIPS
    // /**
    // * Gets the preset reverb effect for the given audio session. If the effect on the session
    // * doesn't exist yet, create it and add to collection.
    // *
    // * @param audioSession
    // * System wide unique audio session identifier.
    // * @return presetReverbEffect
    // */
    // private static PresetReverb getPresetReverbEffect(final int audioSession) {
    // PresetReverb presetReverbEffect = mPresetReverbInstances.get(audioSession);
    // if (presetReverbEffect == null) {
    // try {
    // final PresetReverb newPresetReverbEffect = new PresetReverb(PRIORITY, audioSession);
    // presetReverbEffect = mPresetReverbInstances.putIfAbsent(audioSession,
    // newPresetReverbEffect);
    // if (presetReverbEffect == null) {
    // // put succeeded, use new value
    // presetReverbEffect = newPresetReverbEffect;
    // }
    // } catch (final IllegalArgumentException e) {
    // Log.e(TAG, "PresetReverb: " + e);
    // } catch (final UnsupportedOperationException e) {
    // Log.e(TAG, "PresetReverb: " + e);
    // } catch (final RuntimeException e) {
    // Log.e(TAG, "PresetReverb: " + e);
    // }
    // }
    // return presetReverbEffect;
    // }
}
