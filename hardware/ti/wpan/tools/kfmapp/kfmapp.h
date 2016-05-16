/*
 *  TI FM kernel driver's sample application.
 *
 *  Copyright (C) 2010 Texas Instruments
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

#ifndef _KFMAPP_H
#define _KFMAPP_H

#define DEFAULT_RADIO_DEVICE    "/dev/radio0"

#define FMRX_BAND_SYSFS_ENTRY    "/sys/class/video4linux/radio0/fmrx_band"
#define FMRX_RDS_AF_SYSFS_ENTRY    "/sys/class/video4linux/radio0/fmrx_rds_af"
#define FMRX_RSSI_LVL_SYSFS_ENTRY    "/sys/class/video4linux/radio0/fmrx_rssi_lvl"

#define FMTX_RDS_AF_SYSFS_ENTRY    "/sys/class/video4linux/radio0/fmtx_rds_af"

#define CTL_INDEX_0                0
#define CTL_INDEX_1                1

#define FMAPP_BATCH                0
#define FMAPP_INTERACTIVE          1

#define FM_MUTE_OFF                0
#define FM_MUTE_ON                 1

#define FM_SEARCH_DIRECTION_DOWN   0
#define FM_SEARCH_DIRECTION_UP     1

#define FM_MODE_SWITCH_CTL_NAME   "Mode Switch"
#define FM_MODE_OFF              0
#define FM_MODE_TX               1
#define FM_MODE_RX               2

#define FM_BAND_SWITCH_CTL_NAME    "Region Switch"
#define FM_BAND_EUROPE_US          0
#define FM_BAND_JAPAN              1

#define FM_RF_DEPENDENT_MUTE_CTL_NAME     "RF Dependent Mute"
#define FM_RX_RF_DEPENDENT_MUTE_ON        1
#define FM_RX_RF_DEPENDENT_MUTE_OFF       0

#define FM_RX_GET_RSSI_LVL_CTL_NAME       "RSSI Level"
#define FM_RX_RSSI_THRESHOLD_LVL_CTL_NAME "RSSI Threshold"

#define FM_STEREO_MONO_CTL_NAME              "Stereo/Mono"
#define FM_STEREO_MODE                    0
#define FM_MONO_MODE                      1

#define FM_RX_DEEMPHASIS_CTL_NAME          "De-emphasis Filter"
#define FM_RX_EMPHASIS_FILTER_50_USEC     0
#define FM_RX_EMPHASIS_FILTER_75_USEC     1

#define FM_RDS_SWITCH_CTL_NAME          "RDS Switch"
#define FM_RDS_DISABLE                    0
#define FM_RDS_ENABLE                     1

#define FM_RX_RDS_OPMODE_CTL_NAME      "RDS Operation Mode"
#define FM_RDS_SYSTEM_RDS                  0
#define FM_RDS_SYSTEM_RBDS                 1

#define FM_RX_AF_SWITCH_CTL_NAME      "AF Switch"
#define FM_RX_RDS_AF_SWITCH_MODE_ON        1
#define FM_RX_RDS_AF_SWITCH_MODE_OFF       0

/* Auto scan info */
#define  FMAPP_ASCAN_SIGNAL_THRESHOLD_PER  50 /* 50 % */
#define  FMAPP_ASCAN_NO_OF_SIGNAL_SAMPLE   3  /* 3 Samples */

#define  FMAPP_AF_MAX_FREQ_RANGE	6

#define V4L2_CID_CHANNEL_SPACING (V4L2_CID_PRIVATE_BASE + 0)

struct tx_rds {
        unsigned char   text_type;
        unsigned char   text[25];
        unsigned int    af_freq;
};
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

/* Following macros and structs are re-declared since android
file system has old videodev2.h but kfmapp needs new K35 videodev2.h
declarations, So need to remove these definitions once android headers
move to K35 plus */
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

#endif

