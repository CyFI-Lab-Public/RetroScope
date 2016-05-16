/*
 *
 * Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ti.fmtxapp;

public interface FmTxAppConstants {

    /********************************************
     * Message Code
     ********************************************/
    public static final int EVENT_FM_TX_ENABLED = 1;
    public static final int EVENT_FM_TX_DISABLED = 2;
    public static final int EVENT_FM_TX_TUNE = 3;
    public static final int EVENT_FM_TX_STARTTRANSMISSION = 4;
    public static final int EVENT_FM_TX_STOPTRANSMISSION = 5;
    public static final int EVENT_FM_TX_DESTROY = 6;
    public static final int EVENT_FM_TX_ENABLE_RDS = 7;
    public static final int EVENT_FM_TX_DISABLE_RDS = 8;
    public static final int EVENT_FM_TX_SET_PS_DISPLAY_MODE = 9 ;
    public static final int EVENT_FM_TX_SET_RDS_MUSIC_SPEECH_FLAG = 10;
    public static final int EVENT_FM_TX_SET_TRANSMISSION_MODE = 11;
    public static final int EVENT_FM_TX_SET_RDS_TEXT_REPERTOIRE = 12;
    public static final int EVENT_FM_TX_SET_MONO_STEREO_MODE = 13;
    public static final int EVENT_FM_TX_SET_POWER_LEVEL = 14;
    public static final int EVENT_FM_TX_SET_RDS_TEXT_PS_MSG = 15;
    public static final int EVENT_FM_TX_SET_MUTE_MODE = 16;
    public static final int EVENT_FM_TX_SET_RDS_TX_GRP_MASK_RT = 17;
    public static final int EVENT_FM_TX_SET_RDS_TX_GRP_MASK_PS = 18;
    /********************************************
     *Fm Radio State
     ********************************************/
    public static final int STATE_ENABLED = 0;
    public static final int STATE_DISABLED = 1;
    public static final int STATE_ENABLING = 2;
    public static final int STATE_DISABLING = 3;
    public static final int STATE_PAUSE = 4;
    public static final int STATE_RESUME = 5;
    public static final int STATE_DEFAULT = 6;


    /********************************************
     *Main Screen Preference save keys
     ********************************************/
    public static final String FMENABLED = "FMENABLED";
    public static final String RDSENABLED = "RDSENABLED";
    public static final String FREQUENCY_STRING = "FREQUENCY_STRING";
    public static final String DEFAULT_FREQ = "90000";
    public static final String FMTXSTATE = "FMTXSTATE";



    /* Actvity result index */
    public static final int ACTIVITY_CONFIG = 1;
    public static final int ACTIVITY_ADVANCED = 2;

    /* Power range */
    public static final int POWER_MIN = 0;
    public static final int POWER_MAX = 31;
    public static final int PICODE_MIN = 0;
    public static final int PICODE_MAX = 65535;
    public static final int AFCODE_MIN = 75000;
    public static final int AFCODE_MAX = 108000;
    public static final int PTY_MIN = 0;
    public static final int PTY_MAX = 31;
    public static final int ECC_MIN = 0;
    public static final int ECC_MAX = 65535;

    public static final long RDS_RADIO_TRANSMITTED_GRP_PS_MASK = 1;
    public static final long RDS_RADIO_TRANSMITTED_GRP_RT_MASK = 2;

    public static final int RDS_TEXT_TYPE_RT_AUTO = 2;
    public static final int RDS_TEXT_TYPE_RT_A = 3;
    public static final int RDS_TEXT_TYPE_RT_B = 4;


    /* Config Preference save keys */
    public static final String DISPLAY_MODE = "DISPLAY_MODE";
    public static final String TX_MODE = "TX_MODE";
    public static final String FREQUENCY = "FREQUENCY";
    public static final String REPERTOIRE = "REPERTOIRE";
    public static final String MUSIC_SPEECH = "MUSIC_SPEECH";
    public static final String ECC = "ECC";
    public static final String ECC_STRING = "ECC_STRING";
    public static final String PS_STRING = "PS_STRING";
    public static final String RT_STRING = "RT_STRING";
    public static final String PTY_STRING = "PTY_STRING";
    public static final String PTY = "PTY";
    public static final String POWER = "POWER";
    public static final String POWER_STRING = "POWER_STRING";
    public static final String DEF_POWER_STRING = "4";
    public static final String MONO_STEREO  = "MONO_STEREO";
    public static final String AFCODE_STRING = "AFCODE_STRING";
    public static final String PICODE_STRING = "PICODE_STRING";
    public static final String AF_CODE = "AF_CODE";
    public static final String PI_CODE = "PI_CODE";
    public static final String EMP_FILTER = "EMP_FILTER";
    public static final String MUTE = "MUTE";

    public static final String PSENABLED = "PSENABLED";
    public static final String RTENABLED = "RTENABLED";
    public static final String ECCENABLED = "ECCENABLED";


       /* Default Preference values */
    public static final int INITIAL_VAL = -1;
    public static final int DEFAULT_POWER = 4;
    public static final int DEFAULT_PTY = 0;
    public static final int DEFAULT_TXMODE = 1;
    public static final int DEFAULT_MUSICSPEECH = 0;
    public static final int DEFAULT_ECC = 0;
    public static final int DEFAULT_REPERTOIRE = 0;
    public static final int DEFAULT_DISPLAYMODE = 0;
    public static final int DEFAULT_MONOSTEREO = 1;
    public static final String DEF_PS_STRING = "TI Radio PS";
    public static final String DEF_RT_STRING = "TI Radio RT";
    public static final String DEF_PTY_STRING = "0";
    public static final String DEF_ECC_STRING = "0";
    public static final int DEFAULT_AFCODE = 108000;
    public static final int DEFAULT_PICODE = 0;
    public static final int  DEFAULT_EMPFILTER = 0;
    public static final String DEFAULT_AFCODE_STRING = "108000";
    public static final String DEFAULT_PICODE_STRING = "0";
    public static final int DEF_FREQ = 90000;
    public static final int EVENT_GET_FREQUENCY = 40;


    /* Activity Intenets */
    public static final String INTENT_RDS_CONFIG = "android.intent.action.RDS_CONFIG";
    public static final String INTENT_PRESET = "android.intent.action.PRESET";
    public static final String INTENT_RXHELP = "android.intent.action.START_RXHELP";
    public static final String INTENT_RXTUNE = "android.intent.action.START_RXFREQ";


    public static final float APP_FM_FIRST_FREQ_US_EUROPE_KHZ = (float)87.5;
    public static final float APP_FM_LAST_FREQ_US_EUROPE_KHZ = (float)108.0;
}
