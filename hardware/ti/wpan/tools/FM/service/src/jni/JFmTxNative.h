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


#ifndef JFMTXNATIVE_H_
#define JFMTXNATIVE_H_

extern "C" {
#include "jni.h"
}



#define FM_SUCCESS 0
#define FM_FAILED 1
#define FM_PENDING 2
#define FM_ERR_THREAD_CREATION_FAILED 4


#define DEFAULT_RADIO_DEVICE    "/dev/radio0"

#define FMTX_RDS_AF_SYSFS_ENTRY    "/sys/class/video4linux/radio0/fmtx_rds_af"

#define DEFAULT_FM_ALSA_CARD    "hw:CARD=0"

#define CTL_INDEX_0                0

#define FM_MODE_SWITCH_CTL_NAME   "Mode Switch"
#define FM_MODE_OFF		   0
#define FM_MODE_TX		   1
#define FM_MODE_TX	           2

#define FM_BAND_SWITCH_CTL_NAME    "Region Switch"
#define FM_BAND_EUROPE_US          0
#define FM_BAND_JAPAN              1

#define FM_MUTE_ON                 0
#define FM_MUTE_OFF                1
#define FM_MUTE_ATTENUATE          2

#define FM_RF_DEPENDENT_MUTE_CTL_NAME     "RF Dependent Mute"
#define FM_TX_GET_RSSI_LVL_CTL_NAME 	  "RSSI Level"
#define FM_TX_RSSI_THRESHOLD_LVL_CTL_NAME "RSSI Threshold"
#define FM_STEREO_MONO_CTL_NAME	          "Stereo/Mono"
#define FM_TX_DEEMPHASIS_CTL_NAME    	  "De-emphasis Filter"
#define FM_RDS_SWITCH_CTL_NAME    	  "RDS Switch"
#define FM_RDS_DISABLE                    0
#define FM_RDS_ENABLE                     1
#define FM_TX_RDS_OPMODE_CTL_NAME	  "RDS Operation Mode"
#define FM_TX_AF_SWITCH_CTL_NAME	  "AF Switch"



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
 * FmTxEventType structure
 *
 */
typedef FMC_UINT FmTxEventType;
typedef FMC_UINT FmTxStatus;


/*-------------------------------------------------------------------------------
 * FmTxCmdType structure
 *
 */
typedef FMC_UINT FmTxCmdType;

#define FM_TX_CMD_ENABLE							((FmTxCmdType)0)	/* Enable command */
#define FM_TX_CMD_DISABLE							((FmTxCmdType)1)	/* Disable command */
#define FM_TX_CMD_SET_BAND						((FmTxCmdType)2)	/* Set Band command */
#define FM_TX_CMD_GET_BAND						((FmTxCmdType)3)	/* Get Band command */
#define FM_TX_CMD_SET_MONO_STEREO_MODE			((FmTxCmdType)4)	/* Set Mono/Stereo command */
#define FM_TX_CMD_GET_MONO_STEREO_MODE			((FmTxCmdType)5)	/* Get Mono/Stereo command */
#define FM_TX_CMD_SET_MUTE_MODE					((FmTxCmdType)6)	/* Set Mute mode command */
#define FM_TX_CMD_GET_MUTE_MODE					((FmTxCmdType)7)	/* Get Mute mode command */
#define FM_TX_CMD_SET_RF_DEPENDENT_MUTE_MODE	((FmTxCmdType)8)	/* Set RF-Dependent Mute Mode command */
#define FM_TX_CMD_GET_RF_DEPENDENT_MUTE_MODE	((FmTxCmdType)9)	/* Get RF-Dependent Mute Mode command */
#define FM_TX_CMD_SET_RSSI_THRESHOLD				((FmTxCmdType)10)	/* Set RSSI Threshold command */
#define FM_TX_CMD_GET_RSSI_THRESHOLD				((FmTxCmdType)11)	/* Get RSSI Threshold command */
#define FM_TX_CMD_SET_DEEMPHASIS_FILTER			((FmTxCmdType)12)	/* Set De-Emphassi Filter command */
#define FM_TX_CMD_GET_DEEMPHASIS_FILTER			((FmTxCmdType)13)	/* Get De-Emphassi Filter command */
#define FM_TX_CMD_SET_VOLUME						((FmTxCmdType)14)	/* Set Volume command */
#define FM_TX_CMD_GET_VOLUME						((FmTxCmdType)15)	/* Get Volume command */
#define FM_TX_CMD_TUNE								((FmTxCmdType)16)	/* Tune command */
#define FM_TX_CMD_GET_TUNED_FREQUENCY			((FmTxCmdType)17)	/* Get Tuned Frequency command */
#define FM_TX_CMD_SEEK								((FmTxCmdType)18)	/* Seek command */
#define FM_TX_CMD_STOP_SEEK						((FmTxCmdType)19)	/* Stop Seek command */
#define FM_TX_CMD_GET_RSSI						((FmTxCmdType)20)	/* Get RSSI command */
#define FM_TX_CMD_ENABLE_RDS						((FmTxCmdType)21)	/* Enable RDS command */
#define FM_TX_CMD_DISABLE_RDS						((FmTxCmdType)22)	/* Disable RDS command */
#define FM_TX_CMD_SET_RDS_SYSTEM					((FmTxCmdType)23)	/* Set RDS System command */
#define FM_TX_CMD_GET_RDS_SYSTEM					((FmTxCmdType)24)	/* Get RDS System command */
#define FM_TX_CMD_SET_RDS_GROUP_MASK			((FmTxCmdType)25)	/* Set RDS groups to be recieved */
#define FM_TX_CMD_GET_RDS_GROUP_MASK			((FmTxCmdType)26)	/*  Get RDS groups to be recieved*/
#define FM_TX_CMD_SET_RDS_AF_SWITCH_MODE		((FmTxCmdType)27)	/* Set AF Switch Mode command */
#define FM_TX_CMD_GET_RDS_AF_SWITCH_MODE		((FmTxCmdType)28)	/* Get AF Switch Mode command */
#define FM_TX_CMD_ENABLE_AUDIO				((FmTxCmdType)29)	/* Set Audio Routing command */
#define FM_TX_CMD_DISABLE_AUDIO 				((FmTxCmdType)30)	/* Get Audio Routing command */
#define FM_TX_CMD_DESTROY							((FmTxCmdType)31)	/* Destroy command */
#define FM_TX_CMD_CHANGE_AUDIO_TARGET					((FmTxCmdType)32)	/* Change the audio target*/
#define FM_TX_CMD_CHANGE_DIGITAL_AUDIO_CONFIGURATION	((FmTxCmdType)33)	/* Change the digital target configuration*/
#define FM_TX_INIT_ASYNC                              	((FmTxCmdType)34)	/* */
#define FM_TX_CMD_INIT                              	((FmTxCmdType)35)	/* */
#define FM_TX_CMD_DEINIT                              	((FmTxCmdType)36)	/* */
#define FM_TX_CMD_SET_CHANNEL_SPACING                              	((FmTxCmdType)37)	/* */
#define FM_TX_CMD_GET_CHANNEL_SPACING                              	((FmTxCmdType)38)	/* */
#define FM_TX_CMD_GET_FW_VERSION                            	((FmTxCmdType)39)	/*Gets the FW version */
#define FM_TX_CMD_IS_CHANNEL_VALID                            	((FmTxCmdType)40)	/*Verify that the tuned channel is valid*/
#define FM_TX_CMD_COMPLETE_SCAN                            	((FmTxCmdType)41)	/*Perfrom Complete Scan on the selected Band*/
#define FM_TX_CMD_COMPLETE_SCAN_PROGRESS                            	((FmTxCmdType)42)
#define FM_TX_CMD_STOP_COMPLETE_SCAN                            	((FmTxCmdType)43)
#define FM_TX_CMD_START_TRANSMISSION                            	((FmTxCmdType)44)
#define FM_TX_CMD_STOP_TRANSMISSION                            		((FmTxCmdType)45)
#define FM_TX_CMD_SET_RDS_TEXT_RT_MSG                            	((FmTxCmdType)46)
#define FM_TX_CMD_SET_RDS_TEXT_PS_MSG                            	((FmTxCmdType)47)
#define FM_TX_CMD_SET_RDS_PTY_CODE					((FmTxCmdType)48)
#define FM_TX_CMD_GET_RDS_PTY_CODE					((FmTxCmdType)49)
#define FM_TX_CMD_SET_RDS_AF_CODE                                       ((FmTxCmdType)50)

#define FM_TX_LAST_API_CMD                                              (FM_TX_CMD_SET_RDS_AF_CODE)
#define FM_TX_CMD_NONE					0xFFFFFFFF


namespace android {

extern JNIEnv *getJBtlEnv();
extern void setJBtlEnv(JNIEnv *env);

}

/* Following macros and structs are re-declared since android
   file system has old videodev2.h but kfmapp needs new K35 videodev2.h
   declarations, So need to remove these definitions once android headers
   move to K35 plus */

#define V4L2_CTRL_CLASS_FM_TX 0x009b0000        /* FM Modulator control class */
/* FM Modulator class control IDs */
#define V4L2_CID_FM_TX_CLASS_BASE               (V4L2_CTRL_CLASS_FM_TX | 0x900)
#define V4L2_CID_FM_TX_CLASS                    (V4L2_CTRL_CLASS_FM_TX | 1)

#define V4L2_CID_TUNE_PREEMPHASIS               (V4L2_CID_FM_TX_CLASS_BASE + 112)

#define V4L2_CID_RDS_TX_DEVIATION               (V4L2_CID_FM_TX_CLASS_BASE + 1)
#define V4L2_CID_RDS_TX_PI                      (V4L2_CID_FM_TX_CLASS_BASE + 2)
#define V4L2_CID_RDS_TX_PTY                     (V4L2_CID_FM_TX_CLASS_BASE + 3)
#define V4L2_CID_RDS_TX_PS_NAME                 (V4L2_CID_FM_TX_CLASS_BASE + 5)
#define V4L2_CID_RDS_TX_RADIO_TEXT              (V4L2_CID_FM_TX_CLASS_BASE + 6)

#define V4L2_CID_TUNE_POWER_LEVEL               (V4L2_CID_FM_TX_CLASS_BASE + 113)
#define V4L2_CID_TUNE_ANTENNA_CAPACITOR         (V4L2_CID_FM_TX_CLASS_BASE + 114)
#define V4L2_TUNER_SUB_RDS              0x0010


#undef VIDIOC_S_MODULATOR
#define VIDIOC_S_MODULATOR    1078220343

struct v4l2_ext_control_kfmapp {
	__u32 id;
	__u32 size;
	__u32 reserved2[1];
	union {
		__s32 value;
		__s64 value64;
		char *string;
	};
} __attribute__ ((packed));

struct v4l2_ext_controls_kfmapp {
	__u32 ctrl_class;
	__u32 count;
	__u32 error_idx;
	__u32 reserved[2];
	struct v4l2_ext_control_kfmapp *controls;
};
#endif /* JFMTXNATIVE_H_ */
