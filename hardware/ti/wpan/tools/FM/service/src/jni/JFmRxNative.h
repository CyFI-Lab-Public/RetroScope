/*
 *
 * Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef JFMRXNATIVE_H_
#define JFMRXNATIVE_H_

extern "C" {
#include "jni.h"
}



#define FM_SUCCESS 0
#define FM_FAILED 1
#define FM_PENDING 2
#define FM_ERR_THREAD_CREATION_FAILED 4


#define DEFAULT_RADIO_DEVICE    "/dev/radio0"
#define DEFAULT_FM_ALSA_CARD    "hw:CARD=0"
#define FM_BAND_SYSFS_ENTRY    "/sys/class/video4linux/radio0/fmrx_band"
#define FM_RDS_AF_SYSFS_ENTRY    "/sys/class/video4linux/radio0/fmrx_rds_af"
#define FM_RSSI_LVL_SYSFS_ENTRY    "/sys/class/video4linux/radio0/fmrx_rssi_lvl"

#define CTL_INDEX_0                0

#define FM_MODE_SWITCH_CTL_NAME   "Mode Switch"
#define FM_MODE_OFF		   0
#define FM_MODE_TX		   1
#define FM_MODE_RX	           2

#define FM_BAND_SWITCH_CTL_NAME    "Region Switch"
#define FM_BAND_EUROPE_US          0
#define FM_BAND_JAPAN              1


#define FM_RF_DEPENDENT_MUTE_CTL_NAME     "RF Dependent Mute"
#define FM_RX_GET_RSSI_LVL_CTL_NAME 	  "RSSI Level"
#define FM_RX_RSSI_THRESHOLD_LVL_CTL_NAME "RSSI Threshold"
#define FM_STEREO_MONO_CTL_NAME	          "Stereo/Mono"
#define FM_RX_DEEMPHASIS_CTL_NAME    	  "De-emphasis Filter"
#define FM_RDS_SWITCH_CTL_NAME    	  "RDS Switch"
#define FM_RDS_DISABLE                    0
#define FM_RDS_ENABLE                     1
#define FM_RX_RDS_OPMODE_CTL_NAME	  "RDS Operation Mode"
#define FM_RX_AF_SWITCH_CTL_NAME	  "AF Switch"



typedef unsigned long    FMC_U32;
typedef unsigned int     FMC_UINT;
typedef unsigned char    FMC_U8;

/*-------------------------------------------------------------------------------
 * FmcRdsRepertoire Type
 *
 * 	RDS Repertoire used for text data encoding and decoding
 */
typedef  FMC_UINT  FmcRdsRepertoire;

#define FMC_RDS_REPERTOIRE_G0_CODE_TABLE			((FmcRdsRepertoire)0)
#define FMC_RDS_REPERTOIRE_G1_CODE_TABLE			((FmcRdsRepertoire)1)
#define FMC_RDS_REPERTOIRE_G2_CODE_TABLE			((FmcRdsRepertoire)2)


#define RDS_BIT_0_TO_BIT_3		0x0f
#define RDS_BIT_4_TO_BIT_7		0xf0


/********************************************************************************
 *
 * Events sent to the application
 *
 *******************************************************************************/

/*-------------------------------------------------------------------------------
 * FmRxEventType structure
 *
 */
typedef FMC_UINT FmRxEventType;
typedef FMC_UINT FmRxStatus;


/*-------------------------------------------------------------------------------
 * FmRxCmdType structure
 *
 */
typedef FMC_UINT FmRxCmdType;

#define FM_RX_CMD_ENABLE							((FmRxCmdType)0)	/* Enable command */
#define FM_RX_CMD_DISABLE							((FmRxCmdType)1)	/* Disable command */
#define FM_RX_CMD_SET_BAND						((FmRxCmdType)2)	/* Set Band command */
#define FM_RX_CMD_GET_BAND						((FmRxCmdType)3)	/* Get Band command */
#define FM_RX_CMD_SET_MONO_STEREO_MODE			((FmRxCmdType)4)	/* Set Mono/Stereo command */
#define FM_RX_CMD_GET_MONO_STEREO_MODE			((FmRxCmdType)5)	/* Get Mono/Stereo command */
#define FM_RX_CMD_SET_MUTE_MODE					((FmRxCmdType)6)	/* Set Mute mode command */
#define FM_RX_CMD_GET_MUTE_MODE					((FmRxCmdType)7)	/* Get Mute mode command */
#define FM_RX_CMD_SET_RF_DEPENDENT_MUTE_MODE	((FmRxCmdType)8)	/* Set RF-Dependent Mute Mode command */
#define FM_RX_CMD_GET_RF_DEPENDENT_MUTE_MODE	((FmRxCmdType)9)	/* Get RF-Dependent Mute Mode command */
#define FM_RX_CMD_SET_RSSI_THRESHOLD				((FmRxCmdType)10)	/* Set RSSI Threshold command */
#define FM_RX_CMD_GET_RSSI_THRESHOLD				((FmRxCmdType)11)	/* Get RSSI Threshold command */
#define FM_RX_CMD_SET_DEEMPHASIS_FILTER			((FmRxCmdType)12)	/* Set De-Emphassi Filter command */
#define FM_RX_CMD_GET_DEEMPHASIS_FILTER			((FmRxCmdType)13)	/* Get De-Emphassi Filter command */
#define FM_RX_CMD_SET_VOLUME						((FmRxCmdType)14)	/* Set Volume command */
#define FM_RX_CMD_GET_VOLUME						((FmRxCmdType)15)	/* Get Volume command */
#define FM_RX_CMD_TUNE								((FmRxCmdType)16)	/* Tune command */
#define FM_RX_CMD_GET_TUNED_FREQUENCY			((FmRxCmdType)17)	/* Get Tuned Frequency command */
#define FM_RX_CMD_SEEK								((FmRxCmdType)18)	/* Seek command */
#define FM_RX_CMD_STOP_SEEK						((FmRxCmdType)19)	/* Stop Seek command */
#define FM_RX_CMD_GET_RSSI						((FmRxCmdType)20)	/* Get RSSI command */
#define FM_RX_CMD_ENABLE_RDS						((FmRxCmdType)21)	/* Enable RDS command */
#define FM_RX_CMD_DISABLE_RDS						((FmRxCmdType)22)	/* Disable RDS command */
#define FM_RX_CMD_SET_RDS_SYSTEM					((FmRxCmdType)23)	/* Set RDS System command */
#define FM_RX_CMD_GET_RDS_SYSTEM					((FmRxCmdType)24)	/* Get RDS System command */
#define FM_RX_CMD_SET_RDS_GROUP_MASK			((FmRxCmdType)25)	/* Set RDS groups to be recieved */
#define FM_RX_CMD_GET_RDS_GROUP_MASK			((FmRxCmdType)26)	/*  Get RDS groups to be recieved*/
#define FM_RX_CMD_SET_RDS_AF_SWITCH_MODE		((FmRxCmdType)27)	/* Set AF Switch Mode command */
#define FM_RX_CMD_GET_RDS_AF_SWITCH_MODE		((FmRxCmdType)28)	/* Get AF Switch Mode command */
#define FM_RX_CMD_ENABLE_AUDIO				((FmRxCmdType)29)	/* Set Audio Routing command */
#define FM_RX_CMD_DISABLE_AUDIO 				((FmRxCmdType)30)	/* Get Audio Routing command */
#define FM_RX_CMD_DESTROY							((FmRxCmdType)31)	/* Destroy command */
#define FM_RX_CMD_CHANGE_AUDIO_TARGET					((FmRxCmdType)32)	/* Change the audio target*/
#define FM_RX_CMD_CHANGE_DIGITAL_AUDIO_CONFIGURATION	((FmRxCmdType)33)	/* Change the digital target configuration*/
#define FM_RX_INIT_ASYNC                              	((FmRxCmdType)34)	/* */
#define FM_RX_CMD_INIT                              	((FmRxCmdType)35)	/* */
#define FM_RX_CMD_DEINIT                              	((FmRxCmdType)36)	/* */
#define FM_RX_CMD_SET_CHANNEL_SPACING                              	((FmRxCmdType)37)	/* */
#define FM_RX_CMD_GET_CHANNEL_SPACING                              	((FmRxCmdType)38)	/* */
#define FM_RX_CMD_GET_FW_VERSION                            	((FmRxCmdType)39)	/*Gets the FW version */
#define FM_RX_CMD_IS_CHANNEL_VALID                            	((FmRxCmdType)40)	/*Verify that the tuned channel is valid*/
#define FM_RX_CMD_COMPLETE_SCAN                            	((FmRxCmdType)41)	/*Perfrom Complete Scan on the selected Band*/
#define FM_RX_CMD_COMPLETE_SCAN_PROGRESS                            	((FmRxCmdType)42)
#define FM_RX_CMD_STOP_COMPLETE_SCAN                            	((FmRxCmdType)43)
#define FM_RX_LAST_API_CMD						(FM_RX_CMD_STOP_COMPLETE_SCAN)
#define FM_RX_CMD_NONE					0xFFFFFFFF


namespace android {

extern JNIEnv *getJBtlEnv();
extern void setJBtlEnv(JNIEnv *env);

}

/* Presently android videodev2.h is from k2.6.35 so remove below struct when
 * android header files moves to k3.0+ kernel header
 * */
struct ti_v4l2_hw_freq_seek {
    __u32                 tuner;
    enum v4l2_tuner_type  type;
    __u32                 seek_upward;
    __u32                 wrap_around;
    __u32                 spacing;
    __u32                 reserved[7];
};

#define V4L2_TUNER_SUB_RDS              0x0010
#endif /* JFMRXNATIVE_H_ */
