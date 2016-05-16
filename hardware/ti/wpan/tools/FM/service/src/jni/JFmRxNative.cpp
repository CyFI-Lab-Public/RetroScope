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

#include "android_runtime/AndroidRuntime.h"
#include "jni.h"
#include "JNIHelp.h"
#include "v4l2_JbtlLog.h"


#define LOG_TAG "JFmRxNative"
#include <cutils/properties.h>

using namespace android;

extern "C" {
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <asoundlib.h>
#include <linux/videodev.h>
#include <math.h>
#include <pthread.h>
#include <errno.h>
#include <string.h>
#include <poll.h>

#include "JFmRxNative.h"

/*Callback for FM commands*/
void nativeJFmRx_Callback(long context, int status,
            int command, long value);

 /*Callback for FM  PS*/


   void nativeJFmRx_PS_Callback(long context,int status, int freq,
               int len,unsigned char * name,
               int repertoire) ;

/*Callback for FM  Radio Text*/

void nativeJFmRx_RadioText_Callback(int status, bool resetDisplay,
            unsigned char * msg, int len, int startIndex,
            int repertoire) ;
} //extern "C"

static JavaVM *g_jVM = NULL;
static jclass _sJClass;

typedef pthread_t       THREAD_HANDLE;
THREAD_HANDLE   p_threadHandle;         /* Thread Handle for RDS data  */
static bool isThreadCreated = false;
static int radio_fd;
//snd_ctl_t *fm_snd_ctrl;
long jContext;
volatile bool g_stopCommListener = false;

static int chanl_spacing=200000;

/* Complete parsing of the RDS data has not been implemented yet
Commented the FM RX RDS callbacks functionality start*/

#if 0
static jmethodID _sMethodId_nativeCb_fmRxRawRDS;
static jmethodID _sMethodId_nativeCb_fmRxPiCodeChanged;
static jmethodID _sMethodId_nativeCb_fmRxPtyCodeChanged;
static jmethodID _sMethodId_nativeCb_fmRxMonoStereoModeChanged;
static jmethodID _sMethodId_nativeCb_fmRxAudioPathChanged;
static jmethodID _sMethodId_nativeCb_fmRxAfSwitchFreqFailed;
static jmethodID _sMethodId_nativeCb_fmRxAfSwitchStart;
static jmethodID _sMethodId_nativeCb_fmRxAfSwitchComplete;
static jmethodID _sMethodId_nativeCb_fmRxAfListChanged;
static jmethodID _sMethodId_nativeCb_fmRxCompleteScanDone;
#endif

/*Commented the FM RX RDS callbacks functionality end*/


static jmethodID _sMethodId_nativeCb_fmRxPsChanged;
static jmethodID _sMethodId_nativeCb_fmRxRadioText;
static jmethodID _sMethodId_nativeCb_fmRxCmdEnable;
static jmethodID _sMethodId_nativeCb_fmRxCmdDisable;

static jmethodID _sMethodId_nativeCb_fmRxCmdEnableAudio;
static jmethodID _sMethodId_nativeCb_fmRxCmdChangeAudioTarget;
static jmethodID _sMethodId_nativeCb_fmRxCmdSetBand;
static jmethodID _sMethodId_nativeCb_fmRxCmdGetBand;
static jmethodID _sMethodId_nativeCb_fmRxCmdSetMonoStereoMode;
static jmethodID _sMethodId_nativeCb_fmRxCmdGetMonoStereoMode;
static jmethodID _sMethodId_nativeCb_fmRxCmdGetMuteMode;
static jmethodID _sMethodId_nativeCb_fmRxCmdSetMuteMode;
static jmethodID _sMethodId_nativeCb_fmRxCmdSetRfDependentMuteMode;
static jmethodID _sMethodId_nativeCb_fmRxCmdGetRfDependentMuteMode;
static jmethodID _sMethodId_nativeCb_fmRxCmdSetRssiThreshhold;
static jmethodID _sMethodId_nativeCb_fmRxCmdGetRssiThreshhold;
static jmethodID _sMethodId_nativeCb_fmRxCmdSetDeemphasisFilter;
static jmethodID _sMethodId_nativeCb_fmRxCmdGetDeemphasisFilter;
static jmethodID _sMethodId_nativeCb_fmRxCmdSetVolume;
static jmethodID _sMethodId_nativeCb_fmRxCmdGetVolume;
static jmethodID _sMethodId_nativeCb_fmRxCmdGetChannelSpacing;
static jmethodID _sMethodId_nativeCb_fmRxCmdSetChannelSpacing;
static jmethodID _sMethodId_nativeCb_fmRxCmdTune;
static jmethodID _sMethodId_nativeCb_fmRxCmdGetTunedFrequency;
static jmethodID _sMethodId_nativeCb_fmRxCmdSeek;
static jmethodID _sMethodId_nativeCb_fmRxCmdStopSeek;
static jmethodID _sMethodId_nativeCb_fmRxCmdGetRssi;
static jmethodID _sMethodId_nativeCb_fmRxCmdEnableRds;
static jmethodID _sMethodId_nativeCb_fmRxCmdDisableRds;
static jmethodID _sMethodId_nativeCb_fmRxCmdGetRdsSystem;
static jmethodID _sMethodId_nativeCb_fmRxCmdSetRdsSystem;
static jmethodID _sMethodId_nativeCb_fmRxCmdSetRdsGroupMask;
static jmethodID _sMethodId_nativeCb_fmRxCmdGetRdsGroupMask;
static jmethodID _sMethodId_nativeCb_fmRxCmdSetRdsAfSwitchMode;
static jmethodID _sMethodId_nativeCb_fmRxCmdGetRdsAfSwitchMode;
static jmethodID _sMethodId_nativeCb_fmRxCmdDisableAudio;
static jmethodID _sMethodId_nativeCb_fmRxCmdDestroy;
static jmethodID _sMethodId_nativeCb_fmRxCmdChangeDigitalAudioConfiguration;
static jmethodID _sMethodId_nativeCb_fmRxCmdGetFwVersion;
static jmethodID _sMethodId_nativeCb_fmRxCmdIsValidChannel;
static jmethodID _sMethodId_nativeCb_fmRxCmdGetCompleteScanProgress;
static jmethodID _sMethodId_nativeCb_fmRxCmdStopCompleteScan;





int rdsParseFunc_updateRepertoire(int byte1,int byte2)
{

    int repertoire1,repertoire2;
    int repertoire3,repertoire4;
    int repertoire;

    /*replace to nibble high/low*/
    repertoire1 =  (FMC_U8)(byte1&RDS_BIT_0_TO_BIT_3);
    repertoire2 =  (FMC_U8)((byte1&RDS_BIT_4_TO_BIT_7)>>4);
    repertoire3 =  (FMC_U8)(byte2&RDS_BIT_0_TO_BIT_3);
    repertoire4 =  (FMC_U8)((byte2&RDS_BIT_4_TO_BIT_7)>>4);

    if((repertoire2==0)&&(repertoire1==15)&&(repertoire4==0)&&(repertoire3==15))
    {
        repertoire = FMC_RDS_REPERTOIRE_G0_CODE_TABLE;

    }
    else if((repertoire2==0)&&(repertoire1==14)&&(repertoire4==0)&&(repertoire3==14))
    {
        repertoire = FMC_RDS_REPERTOIRE_G1_CODE_TABLE;

    }
    else if ((repertoire2==1)&&(repertoire1==11)&&(repertoire4==6)&&(repertoire3==14))
    {
        repertoire = FMC_RDS_REPERTOIRE_G2_CODE_TABLE;

    }

V4L2_JBTL_LOGD(" rdsParseFunc_updateRepertoire repertoire%d\n",repertoire);
    return repertoire;
}


void rds_decode(int blkno, int byte1, int byte2)
{
    static unsigned char rds_psn[9];
    static unsigned  char rds_txt[65];
    static int  rds_pty,ms_code;
    static int group,spare,blkc_byte1,blkc_byte2;
    int len;
    bool resetDisplay =false;
    int status = 0,startIndex=0,repertoire,freq;

    switch (blkno) {
    case 0: /* Block A */
        V4L2_JBTL_LOGD("block A - id=%d\n",(byte1 << 8) | byte2);
    break;
    case 1: /* Block B */
    V4L2_JBTL_LOGD("block B - group=%d%c tp=%d pty=%d spare=%d\n",
            (byte1 >> 4) & 0x0f,
            ((byte1 >> 3) & 0x01) + 'A',
            (byte1 >> 2) & 0x01,
            ((byte1 << 3) & 0x18) | ((byte2 >> 5) & 0x07),
            byte2 & 0x1f);
    group = (byte1 >> 3) & 0x1f;
    spare = byte2 & 0x1f;
    rds_pty = ((byte1 << 3) & 0x18) | ((byte2 >> 5) & 0x07);
        ms_code = (byte2 >> 3)& 0x1;

    break;
    case 2: /* Block C */
        V4L2_JBTL_LOGD("block C - 0x%02x 0x%02x\n",byte1,byte2);
    blkc_byte1 = byte1;
    blkc_byte2 = byte2;
    break;
    case 3 : /* Block D */
    V4L2_JBTL_LOGD("block D - 0x%02x 0x%02x\n",byte1,byte2);
    switch (group) {
    case 0: /* Group 0A */
        rds_psn[2*(spare & 0x03)+0] = byte1;
        rds_psn[2*(spare & 0x03)+1] = byte2;
        if ((spare & 0x03) == 0x03)
        {
            V4L2_JBTL_LOGD("PSN: %s, PTY: %d, MS: %s\n",rds_psn,
                            rds_pty,ms_code?"Music":"Speech");

        len = strlen((const char *)rds_psn);
 V4L2_JBTL_LOGD("PS len %d",len);
        nativeJFmRx_PS_Callback(jContext,status,freq,len,rds_psn,repertoire);
        }

        break;
    case 4: /* Group 2A */

        repertoire = rdsParseFunc_updateRepertoire(byte1,byte2);

        repertoire =0;

 V4L2_JBTL_LOGD("Radio repertoire: %d\n",repertoire);
        rds_txt[4*(spare & 0x0f)+0] = blkc_byte1;
        rds_txt[4*(spare & 0x0f)+1] = blkc_byte2;
        rds_txt[4*(spare & 0x0f)+2] = byte1;
        rds_txt[4*(spare & 0x0f)+3] = byte2;
            /* Display radio text once we get 16 characters */
        if (spare > 16)
            {
            len =strlen((const char *)rds_txt);

 V4L2_JBTL_LOGD("RDS len %d",len);
            V4L2_JBTL_LOGD("Radio Text: %s\n",rds_txt);

            nativeJFmRx_RadioText_Callback(status, resetDisplay,
            rds_txt, len, startIndex,repertoire) ;
            }
        break;
         }
         V4L2_JBTL_LOGD("----------------------------------------\n");
         break;
     default:
         V4L2_JBTL_LOGD("unknown block [%d]\n",blkno);
    }
}

/**
 * Function:        entryFunctionForRdsThread
 * Brief:           Creates a thread for waiting on responses from RDS .
 * Description:
 */

void *entryFunctionForRdsThread(void *data)
{
  unsigned char buf[600];
  int radio_fd;
  int ret,index;
  struct pollfd pfd;

  radio_fd = (int)data;

  V4L2_JBTL_LOGD(" entryFunctionForRdsThread: Entering.g_stopCommListener %d \n",g_stopCommListener);

  while(!g_stopCommListener)
  {

  V4L2_JBTL_LOGD("RDS thread running..\n");

  while(1){
      memset(&pfd, 0, sizeof(pfd));
      pfd.fd = radio_fd;
      pfd.events = POLLIN;
      ret = poll(&pfd, 1, 10);
      if (ret == 0){
          /* Break the poll after RDS data available */
          break;
      }
  }

    ret = read(radio_fd,buf,500);
    if(ret < 0)
    {  V4L2_JBTL_LOGD("NO RDS data to read..\n");
    return NULL;
    }

    else if( ret > 0)
    {

    V4L2_JBTL_LOGD(" RDS data to read is available..\n");
       for(index=0;index<ret;index+=3)
         rds_decode(buf[index+2] & 0x7,buf[index+1],buf[index]);
    }
  }

  V4L2_JBTL_LOGD("RDS thread exiting..\n");
  return NULL;
}

int fm_read_tuner_capabilities(int radio_fd)
{
  struct v4l2_capability cap;
  int res;

  res = ioctl(radio_fd,VIDIOC_QUERYCAP,&cap);
  if(res < 0)
  {
    V4L2_JBTL_LOGD("Failed to read %s capabilities\n",DEFAULT_RADIO_DEVICE);
    return FM_FAILED;
  }
  if((cap.capabilities & V4L2_CAP_RADIO) == 0)
  {
    V4L2_JBTL_LOGD("%s is not radio devcie",DEFAULT_RADIO_DEVICE);
    return FM_FAILED;
  }
  V4L2_JBTL_LOGD("\n***%s Info ****\n",DEFAULT_RADIO_DEVICE);
  V4L2_JBTL_LOGD("Driver       : %s\n",cap.driver);
  V4L2_JBTL_LOGD("Card         : %s\n",cap.card);
  V4L2_JBTL_LOGD("Bus          : %s\n",cap.bus_info);
  V4L2_JBTL_LOGD("Capabilities : 0x%x\n",cap.capabilities);

  return FM_SUCCESS;
}


static int nativeJFmRx_Create(JNIEnv *env,jobject obj,jobject jContextValue)
{

    int fmStatus ;

   V4L2_JBTL_LOGD("Java_JFmRx_nativeJFmRx_Create(): Entered");

   radio_fd = open(DEFAULT_RADIO_DEVICE, O_RDWR);
   if(radio_fd < 0)
   {
       V4L2_JBTL_LOGD("Unable to open %s ..\n",DEFAULT_RADIO_DEVICE);
      jniThrowIOException(env, errno);
       return FM_FAILED;
   }

   fmStatus = fm_read_tuner_capabilities(radio_fd);
   if(fmStatus< 0)
   {
     close(radio_fd);
     return fmStatus;
   }

    V4L2_JBTL_LOGD("nativeJFmRx_create:Exiting Successfully");

    return fmStatus;
}



static int nativeJFmRx_Destroy(JNIEnv *env, jobject obj,jlong jContextValue)
{

    V4L2_JBTL_LOGD("nativeJFmRx_destroy(): Entered");

    V4L2_JBTL_LOGD("nativeJFmRx_destroy(): Exit");
    return FM_SUCCESS;
}



static int nativeJFmRx_Enable(JNIEnv *env, jobject obj, jlong jContextValue)
{

   int  status ;
   struct v4l2_tuner vtun;

   V4L2_JBTL_LOGD("nativeJFmRx_enable(): Entered");

   jContext = jContextValue;

   vtun.index = 0;
   vtun.audmode = V4L2_TUNER_MODE_STEREO;
   vtun.rxsubchans = V4L2_TUNER_SUB_RDS;

   status = ioctl(radio_fd, VIDIOC_S_TUNER, &vtun);
   if(status < 0)
   {
     V4L2_JBTL_LOGD("Failed to Enable FM\n");
     return status;
   }

   V4L2_JBTL_LOGD("nativeJFmRx_enable: FM_RX_Enable() returned %d",(int)status);
   nativeJFmRx_Callback(jContext,status,FM_RX_CMD_ENABLE,status);
   V4L2_JBTL_LOGD("nativeJFmRx_enable(): Exit");
    return status;
}



static int nativeJFmRx_Disable(JNIEnv *env, jobject obj, jlong jContextValue)
{
    V4L2_JBTL_LOGD("nativeJFmRx_disable(): Entered");

    // Terminate RDS thread
    g_stopCommListener = true;
    isThreadCreated = false;

    close(radio_fd);
    nativeJFmRx_Callback(jContext,0,FM_RX_CMD_DISABLE,0);

    V4L2_JBTL_LOGD("nativeJFmRx_disable(): Exit");;
    return FM_SUCCESS;
}



static int nativeJFmRx_SetBand(JNIEnv *env, jobject obj,jlong jContextValue, jint jFmBand)
{
   int status=0;
   static unsigned char last_band = FM_BAND_EUROPE_US;
   char curr_band;
   int fd, res;

   switch(jFmBand) {
       case 1:
           curr_band = '1';
           break;
       case 0:
       default:
           curr_band = '0';
           break;
   }

   V4L2_JBTL_LOGD("nativeJFmRx_setBand(): EnteredjFmBand  %d",jFmBand);
   V4L2_JBTL_LOGD("nativeJFmRx_setBand(): curr_band %d last_band %d",curr_band,last_band);

   fd = open(FM_BAND_SYSFS_ENTRY, O_RDWR);
   if (fd < 0) {
       V4L2_JBTL_LOGD("Can't open %s", FM_BAND_SYSFS_ENTRY);
       return FM_FAILED;
   }

   res = write(fd, &curr_band, sizeof(char));
   if(res <= 0){
       V4L2_JBTL_LOGD("Failed to set FM Band\n");
       return FM_FAILED;
   }

   nativeJFmRx_Callback(jContext,status,FM_RX_CMD_SET_BAND,status);

   V4L2_JBTL_LOGD("nativeJFmRx_setBand(): Exit");
   return FM_PENDING;
}


static int nativeJFmRx_GetBand(JNIEnv *env, jobject obj,jlong jContextValue)
{

    int status = 0;
    unsigned char curr_band;

nativeJFmRx_Callback(jContext,status,FM_RX_CMD_GET_BAND,curr_band);

    V4L2_JBTL_LOGD("nativeJFmRx_getBand(): Exit");

     return FM_PENDING;
}


static int nativeJFmRx_Tune(JNIEnv *env, jobject obj,jlong jContextValue,jint user_freq)
{
    struct v4l2_frequency vf;
    struct v4l2_tuner vt;
    int status, div;

    V4L2_JBTL_LOGD("nativeJFmRx_tune(): Entered");

    vt.index = 0;
    status = ioctl(radio_fd, VIDIOC_G_TUNER, &vt);
        if(status < 0)
        {
                V4L2_JBTL_LOGD("Failed to get tuner capabilities\n");
                return FM_FAILED;
        }

    vf.tuner = 0;
    vf.frequency = rint(user_freq * 16 + 0.5);

    div = (vt.capability & V4L2_TUNER_CAP_LOW) ? 1000 : 1;
    if (div == 1)
        vf.frequency /=1000;

    status = ioctl(radio_fd, VIDIOC_S_FREQUENCY, &vf);
    if(status < 0)
    {
        V4L2_JBTL_LOGD("Failed to tune to frequency %d\n",user_freq);
        return FM_FAILED;
    }
    V4L2_JBTL_LOGD("Tuned to frequency %2.1f MHz\n",user_freq);

nativeJFmRx_Callback(jContext,status,FM_RX_CMD_TUNE,user_freq);

    V4L2_JBTL_LOGD("nativeJFmRx_Tune(): Exit");
     return FM_PENDING;



}


static int nativeJFmRx_GetTunedFrequency(JNIEnv *env, jobject obj,jlong jContextValue)
{
   struct v4l2_frequency vf;
   int status;
   V4L2_JBTL_LOGD("nativeJFmRx_getTunedFrequency(): Entered");

   status = ioctl(radio_fd, VIDIOC_G_FREQUENCY,&vf);
   if(status < 0)
   {
     V4L2_JBTL_LOGD("Failed to read current frequency\n");
     return FM_FAILED;
   }

   V4L2_JBTL_LOGD("Tuned to frequency %2.1f MHz \n",(float)vf.frequency/1000);
nativeJFmRx_Callback(jContext,status,FM_RX_CMD_GET_TUNED_FREQUENCY,vf.frequency);

    V4L2_JBTL_LOGD("nativeJFmRx_getTunedFrequency(): Exit");
     return FM_PENDING;

}



static int nativeJFmRx_SetMonoStereoMode(JNIEnv *env, jobject obj,jlong jContextValue,jint jFmMode)
{

     struct v4l2_tuner vt;
     int status;
     V4L2_JBTL_LOGD("nativeJFmRx_SetMonoStereoMode(): Entered");

    vt.index = 0;
    vt.audmode = jFmMode;

    status = ioctl(radio_fd, VIDIOC_S_TUNER, &vt);
    if (status < 0){
       V4L2_JBTL_LOGD("Failed to set stereo/mono mode\n");
       return FM_FAILED;
    }

    V4L2_JBTL_LOGD("Set to %d Mode\n",jFmMode);
    nativeJFmRx_Callback(jContext,status, FM_RX_CMD_SET_MONO_STEREO_MODE,status);

    V4L2_JBTL_LOGD("nativeJFmRx_SetMonoStereoMode(): Exit");
    return FM_PENDING;
}



static int nativeJFmRx_GetMonoStereoMode(JNIEnv *env, jobject obj,jlong jContextValue)
{
    struct v4l2_tuner vt;
    int status;
    unsigned char mode;

    V4L2_JBTL_LOGD("nativeJFmRx_GetMonoStereoMode(): Entered");

    vt.index = 0;
    status = ioctl(radio_fd, VIDIOC_G_TUNER, &vt);
    if (status < 0){
       V4L2_JBTL_LOGD("Failed to get stereo/mono mode\n");
       return FM_FAILED;
    }
    mode = vt.audmode;

    V4L2_JBTL_LOGD("%d mode\n",mode);
nativeJFmRx_Callback(jContext,status, FM_RX_CMD_GET_MONO_STEREO_MODE,mode);

    V4L2_JBTL_LOGD("nativeJFmRx_GetMonoStereoMode(): Exit");
    return FM_PENDING    ;
}



static int nativeJFmRx_SetMuteMode(JNIEnv *env, jobject obj,jlong jContextValue,jint jFmMuteMode)
{

      struct v4l2_control vctrl;
   int status;

    V4L2_JBTL_LOGD("nativeJFmRx_setMuteMode(): Entered");

   vctrl.id = V4L2_CID_AUDIO_MUTE;
   vctrl.value = !jFmMuteMode; /* To Do:: Mapping in future for V4L2*/
   status = ioctl(radio_fd,VIDIOC_S_CTRL,&vctrl);
   if(status < 0)
   {
     V4L2_JBTL_LOGD("Failed to set mute mode\n");
     return FM_FAILED;
   }

nativeJFmRx_Callback(jContext,status, FM_RX_CMD_SET_MUTE_MODE,status);
    V4L2_JBTL_LOGD("nativeJFmRx_setMuteMode(): Exit");
      return FM_PENDING;


}


static int nativeJFmRx_GetMuteMode(JNIEnv *env, jobject obj,jlong jContextValue)
{
    struct v4l2_control vctrl;
    int status;
    V4L2_JBTL_LOGD("nativeJFmRx_getMuteMode(): Entered");
    vctrl.id = V4L2_CID_AUDIO_MUTE;
    status = ioctl(radio_fd,VIDIOC_G_CTRL,&vctrl);
    if(status < 0)
    {
      V4L2_JBTL_LOGD("Failed to get mute mode\n");
      return FM_FAILED;
    }

    V4L2_JBTL_LOGD("%d\n",vctrl.value);
nativeJFmRx_Callback(jContext,status, FM_RX_CMD_GET_MUTE_MODE,vctrl.value);

    V4L2_JBTL_LOGD("nativeJFmRx_getMuteMode(): Exit");
       return FM_PENDING;
}


static int nativeJFmRx_SetRssiThreshold(JNIEnv *env, jobject obj,jlong jContextValue,jint jFmRssi)
{

    int status;
    char rssi_lvl[10];
    int fd, res;

    V4L2_JBTL_LOGD("nativeJFmRx_setRssiThreshold(): Entered");

    sprintf(rssi_lvl,"%d",jFmRssi);

    V4L2_JBTL_LOGD("nativeJFmRx_setRssiThreshold(): val = %s", rssi_lvl);
    ;
    fd = open(FM_RSSI_LVL_SYSFS_ENTRY, O_RDWR);
    if (fd < 0) {
        V4L2_JBTL_LOGD("Can't open %s", FM_RSSI_LVL_SYSFS_ENTRY);
        return FM_FAILED;
    }

    res = write(fd, &rssi_lvl, sizeof(char));
    if(res <= 0){
        V4L2_JBTL_LOGD("Failed to set FM RSSI level\n");
        return FM_FAILED;
    }

    V4L2_JBTL_LOGD("Setting rssi to %d\n",jFmRssi);

    nativeJFmRx_Callback(jContext,status, FM_RX_CMD_SET_RSSI_THRESHOLD,status);
    V4L2_JBTL_LOGD("nativeJFmRx_setRssiThreshold(): Exit");

    return FM_PENDING;
}

static int nativeJFmRx_GetRssiThreshold(JNIEnv *env, jobject obj,jlong jContextValue)
{

  short rssi_threshold;
   int status;
   V4L2_JBTL_LOGD("nativeJFmRx_getRssiThreshold(): Entered");

   status = 0;

   V4L2_JBTL_LOGD("RSSI threshold set to %d\n",rssi_threshold);
nativeJFmRx_Callback(jContext,status, FM_RX_CMD_GET_RSSI_THRESHOLD,rssi_threshold);
    V4L2_JBTL_LOGD("nativeJFmRx_getRssiThreshold(): Exit");
     return FM_PENDING;
}

static int nativeJFmRx_GetRssi(JNIEnv *env, jobject obj,jlong jContextValue)
{
    int status;
    short curr_rssi_lvl;

    V4L2_JBTL_LOGD("nativeJFmRx_getRssi(): Entered");

    status = 0;

    V4L2_JBTL_LOGD("RSSI level is %d\n",curr_rssi_lvl);

nativeJFmRx_Callback(jContext,status, FM_RX_CMD_GET_RSSI,curr_rssi_lvl);
        V4L2_JBTL_LOGD("nativeJFmRx_getRssi(): Exit");
     return FM_PENDING;;

}

static int nativeJFmRx_SetVolume(JNIEnv *env, jobject obj,jlong jContextValue,jint jFmVolume)
{
   struct v4l2_control vctrl;
   int status;

   V4L2_JBTL_LOGD("nativeJFmRx_SetVolume(): Entered");

   vctrl.id = V4L2_CID_AUDIO_VOLUME;
   vctrl.value = jFmVolume;

   status = ioctl(radio_fd,VIDIOC_S_CTRL,&vctrl);
   if(status < 0)
   {
     V4L2_JBTL_LOGD("nativeJFmRx_SetVolume():Failed to set volume\n");
     return status;
   }
   V4L2_JBTL_LOGD("nativeJFmRx_SetVolume():Setting volume to %d \n",jFmVolume);

nativeJFmRx_Callback(jContext,status, FM_RX_CMD_SET_VOLUME,status);

    V4L2_JBTL_LOGD("nativeJFmRx_SetVolume(): Exit");
     return FM_PENDING;


}

static int nativeJFmRx_GetVolume(JNIEnv *env, jobject obj,jlong jContextValue)
{
    struct v4l2_control vctrl;
    int status;

    V4L2_JBTL_LOGD("nativeJFmRx_getVolume(): Entered");


   vctrl.id = V4L2_CID_AUDIO_VOLUME;
   status = ioctl(radio_fd,VIDIOC_G_CTRL,&vctrl);
   if(status < 0)
   {
     V4L2_JBTL_LOGD("Failed to get volume\n");
     return status;
   }

nativeJFmRx_Callback(jContext,status, FM_RX_CMD_GET_VOLUME,vctrl.value);
    V4L2_JBTL_LOGD("nativeJFmRx_getVolume(): Exit");
       return FM_PENDING;
}

static int nativeJFmRx_SetChannelSpacing(JNIEnv *env, jobject obj,jlong jContextValue,jint jFmChannelSpacing)
{

    int status = 0;
    ALOGD("nativeJFmRx_SetChannelSpacing(): Entered");

    chanl_spacing = jFmChannelSpacing * 50000;

    nativeJFmRx_Callback(jContext,status, FM_RX_CMD_SET_CHANNEL_SPACING,status);

    ALOGD("nativeJFmRx_SetChannelSpacing(): Exit");
    return FM_PENDING;

}

static int nativeJFmRx_GetChannelSpacing(JNIEnv *env, jobject obj,jlong jContextValue)
{

    int status =0;
    ALOGD("nativeJFmRx_GetChannelSpacing(): Entered");

      ALOGD("nativeJFmRx_GetChannelSpacing(): Exit");
    nativeJFmRx_Callback(jContext,status, FM_RX_CMD_GET_CHANNEL_SPACING,status);
     return FM_PENDING;
}

static jint nativeJFmRx_SetDeEmphasisFilter(JNIEnv *env, jobject obj,jlong jContextValue,jint jFmEmphasisFilter)
{

   int status;
    V4L2_JBTL_LOGD("nativeJFmRx_SetDeEmphasisFilter(): Entered");

    V4L2_JBTL_LOGD("1. nativeJFmRx_EnableRDS\n");
    status = 0;

   V4L2_JBTL_LOGD("Set to De-emphasis %d mode\n",jFmEmphasisFilter);
nativeJFmRx_Callback(jContext,status, FM_RX_CMD_SET_DEEMPHASIS_FILTER,status);
    V4L2_JBTL_LOGD("nativeJFmRx_SetDeEmphasisFilter(): Exit");
    V4L2_JBTL_LOGD("2. nativeJFmRx_EnableRDS\n");
     return FM_PENDING;
}


static int nativeJFmRx_GetDeEmphasisFilter(JNIEnv *env, jobject obj,jlong jContextValue)
{

    int status;
    unsigned char mode;

    V4L2_JBTL_LOGD("nativeJFmRx_GetDeEmphasisFilter(): Entered");

    V4L2_JBTL_LOGD("1. nativeJFmRx_EnableRDS\n");
    mode = 0;

    V4L2_JBTL_LOGD("De-emphasis filter %d\n",mode);
nativeJFmRx_Callback(jContext,status, FM_RX_CMD_GET_DEEMPHASIS_FILTER,mode);

    V4L2_JBTL_LOGD("nativeJFmRx_GetDeEmphasisFilter(): Exit");
    V4L2_JBTL_LOGD("2. nativeJFmRx_EnableRDS\n");
     return FM_PENDING;
}



static int nativeJFmRx_Seek(JNIEnv *env, jobject obj,jlong jContextValue,jint jdirection)
{

     struct ti_v4l2_hw_freq_seek frq_seek;
    struct v4l2_frequency vf;
        struct v4l2_tuner vt;
    int status, div;

    V4L2_JBTL_LOGD("nativeJFmRx_Seek(): Entered");
    V4L2_JBTL_LOGD("Seeking %s.. and channel spacing is %d\n",jdirection?"up":"down", chanl_spacing);
    frq_seek.seek_upward = jdirection;
        frq_seek.type = (v4l2_tuner_type)1;
        frq_seek.spacing = chanl_spacing;
        frq_seek.wrap_around = 0;

    errno = 0;
    status = ioctl(radio_fd,VIDIOC_S_HW_FREQ_SEEK,&frq_seek);
    if(errno == EAGAIN)
    {
      V4L2_JBTL_LOGD("Band limit reached\n");
    }
    else if(status <0)
    {
      V4L2_JBTL_LOGD("Seek operation failed\n");
      return status;
    }

    V4L2_JBTL_LOGD("nativeJFmRx_tune(): Entered");

        vt.index = 0;
        status = ioctl(radio_fd, VIDIOC_G_TUNER, &vt);
        if(status < 0)
        {
                V4L2_JBTL_LOGD("Failed to get tuner capabilities\n");
                return FM_FAILED;
        }

        div = (vt.capability & V4L2_TUNER_CAP_LOW) ? 1000 : 1;

     status = ioctl(radio_fd, VIDIOC_G_FREQUENCY,&vf);
     if(status < 0)
     {
       V4L2_JBTL_LOGD("Failed to read current frequency\n");
       return status;
     }

     V4L2_JBTL_LOGD("Tuned to frequency %3.2f MHz \n",vf.frequency / (16.0 * div));

    nativeJFmRx_Callback(jContext,status, FM_RX_CMD_SEEK,vf.frequency * 1000 / (16 * div));

        V4L2_JBTL_LOGD("nativeJFmRx_Seek(): Exit");
         return FM_PENDING;

}


static int nativeJFmRx_StopSeek(JNIEnv *env, jobject obj,jlong jContextValue)
{
int status =0;
    V4L2_JBTL_LOGD("nativeJFmRx_StopSeek(): Entered");
 nativeJFmRx_Callback(jContext,status, FM_RX_CMD_STOP_SEEK,status);
    V4L2_JBTL_LOGD("nativeJFmRx_StopSeek(): Exit");
         return FM_PENDING;
}

static int nativeJFmRx_EnableRDS(JNIEnv *env, jobject obj,jlong jContextValue)
{

    int status;
    unsigned char rds_mode = FM_RDS_ENABLE;
    struct v4l2_tuner vt;

    V4L2_JBTL_LOGD("nativeJFmRx_enableRDS(): Entered");

    V4L2_JBTL_LOGD("1. nativeJFmRx_EnableRDS\n");
    vt.index = 0;
    status = ioctl(radio_fd, VIDIOC_G_TUNER, &vt);
    if(status < 0)
    {
        V4L2_JBTL_LOGD("Failed to get tuner attributes\n");
        return status;
    }

    V4L2_JBTL_LOGD("2. nativeJFmRx_EnableRDS\n");

        if ((vt.rxsubchans & V4L2_TUNER_SUB_RDS) != 1)
                vt.rxsubchans |= V4L2_TUNER_SUB_RDS;

    status = ioctl(radio_fd, VIDIOC_S_TUNER, &vt);
    if(status < 0)
    {
        V4L2_JBTL_LOGD("Failed to set RDS on/off status\n");
        return status;
    }

    V4L2_JBTL_LOGD("3. nativeJFmRx_EnableRDS\n");
    if(isThreadCreated == false)
    {

        V4L2_JBTL_LOGD(" nativeJFmRx_EnableRDS: creating thread !!! \n");
        g_stopCommListener = false;
        /* Create rds receive thread once */
        status = pthread_create(&p_threadHandle,   /* Thread Handle. */
                    NULL,                               /* Default Atributes. */
                    entryFunctionForRdsThread,            /* Entry Function. */
                    (void *)radio_fd);           /* Parameters. */
        if (status < 0)
        {
            V4L2_JBTL_LOGD(" nativeJFmRx_EnableRDS: Thread Creation FAILED !!! \n");
            return FM_ERR_THREAD_CREATION_FAILED;
        }

        isThreadCreated = true;
    }
    else
        V4L2_JBTL_LOGD("RDS thread already created\n");

    V4L2_JBTL_LOGD("4. nativeJFmRx_EnableRDS\n");
    V4L2_JBTL_LOGD("RDS %d\n",rds_mode);

    nativeJFmRx_Callback(jContext,status, FM_RX_CMD_ENABLE_RDS,status);
    V4L2_JBTL_LOGD("nativeJFmRx_enableRDS(): Exit");
    return FM_PENDING;
}

static int nativeJFmRx_DisableRDS(JNIEnv *env, jobject obj,jlong jContextValue)
{

    int status;
    unsigned char rds_mode = FM_RDS_DISABLE;
    struct v4l2_tuner vt;

        V4L2_JBTL_LOGD("1. nativeJFmRx_DisableRDS\n");
        vt.index = 0;
        status = ioctl(radio_fd, VIDIOC_G_TUNER, &vt);
        if(status < 0)
        {
                V4L2_JBTL_LOGD("Failed to get tuner attributes\n");
                return status;
        }

    if(vt.rxsubchans & V4L2_TUNER_SUB_RDS)
        vt.rxsubchans &= ~V4L2_TUNER_SUB_RDS;

        V4L2_JBTL_LOGD("2. nativeJFmRx_DisableRDS and vt.rxsubchans = %d\n", vt.rxsubchans);

        status = ioctl(radio_fd, VIDIOC_S_TUNER, &vt);
        if(status < 0)
        {
                V4L2_JBTL_LOGD("Failed to set RDS on/off status\n");
                return status;
        }

nativeJFmRx_Callback(jContext,status, FM_RX_CMD_DISABLE_RDS,status);

    V4L2_JBTL_LOGD("nativeJFmRx_DisableRDS(): Exit");
     return FM_PENDING;
}

static int nativeJFmRx_EnableAudioRouting(JNIEnv *env, jobject obj,jlong jContextValue)
{
   int status = 0 ;
    V4L2_JBTL_LOGD("nativeJFmRx_enableAudioRouting(): Entered");

   nativeJFmRx_Callback(jContext,status, FM_RX_CMD_ENABLE_AUDIO,status);

    V4L2_JBTL_LOGD("nativeJFmRx_enableAudioRouting(): Exit");
     return FM_PENDING;
}

static int  nativeJFmRx_DisableAudioRouting(JNIEnv *env, jobject obj,jlong jContextValue)
{
   int status = 0 ;
    V4L2_JBTL_LOGD("nativeJFmRx_disableAudioRouting(): Entered");

   nativeJFmRx_Callback(jContext,status, FM_RX_CMD_ENABLE_AUDIO,status);

    V4L2_JBTL_LOGD("nativeJFmRx_disableAudioRouting(): Exit");
     return FM_PENDING;
}

static int nativeJFmRx_SetRdsAfSwitchMode(JNIEnv *env, jobject obj,jlong jContextValue,jint jRdsAfSwitchMode)
{

    int status;
    char af_switch;
    int fd, res;

    V4L2_JBTL_LOGD("nativeJFmRx_setRdsAfSwitchMode(): Entered");

    switch(jRdsAfSwitchMode) {
        case 1:
            af_switch = '1';
            break;
        case 0:
        default:
            af_switch = '0';
            break;
    }

    fd = open(FM_RDS_AF_SYSFS_ENTRY, O_RDWR);
    if (fd < 0) {
        V4L2_JBTL_LOGD("Can't open %s", FM_RDS_AF_SYSFS_ENTRY);
        return FM_FAILED;
    }

    res = write(fd, &af_switch, sizeof(char));
    if(res <= 0){
        V4L2_JBTL_LOGD("Failed to set FM AF Switch\n");
        return FM_FAILED;
    }


    V4L2_JBTL_LOGD("AF Switch %d ",jRdsAfSwitchMode);

    nativeJFmRx_Callback(jContext,status, FM_RX_CMD_SET_RDS_AF_SWITCH_MODE,status);
    V4L2_JBTL_LOGD("nativeJFmRx_setRdsAfSwitchMode(): Exit");
    return FM_PENDING;

}

static int nativeJFmRx_GetRdsAfSwitchMode(JNIEnv *env, jobject obj,jlong jContextValue)
{

    int status;
     unsigned char af_mode;


    V4L2_JBTL_LOGD("nativeJFmRx_getRdsAfSwitchMode(): Entered");

    status = 0;

nativeJFmRx_Callback(jContext,status, FM_RX_CMD_GET_RDS_AF_SWITCH_MODE,af_mode);
    V4L2_JBTL_LOGD("nativeJFmRx_getRdsAfSwitchMode(): Exit");
     return FM_PENDING;
}

static int   nativeJFmRx_ChangeAudioTarget (JNIEnv *env, jobject obj,jlong jContextValue, jint jFmRxAudioTargetMask, jint digitalConfig)
{

    V4L2_JBTL_LOGD("nativeJFmRx_ChangeAudioTarget(): Entered");
//nativeJFmRx_Callback(jContext,status, FM_RX_CMD_CHANGE_AUDIO_TARGET,status);
      V4L2_JBTL_LOGD("nativeJFmRx_ChangeAudioTarget(): Exit");
         return FM_PENDING;

}


static int    nativeJFmRx_ChangeDigitalTargetConfiguration(JNIEnv *env, jobject obj,jlong jContextValue,jint digitalConfig)
{

    V4L2_JBTL_LOGD("nativeJFmRx_ChangeDigitalTargetConfiguration(): Entered");

 //nativeJFmRx_Callback(jContext,status, FM_RX_CMD_CHANGE_DIGITAL_AUDIO_CONFIGURATION,status);

    V4L2_JBTL_LOGD("nativeJFmRx_ChangeDigitalTargetConfiguration(): Exit");
     return FM_PENDING;

}


static int   nativeJFmRx_SetRfDependentMuteMode(JNIEnv *env, jobject obj,jlong jContextValue, jint rf_mute)
{

      int status;
        V4L2_JBTL_LOGD("nativeJFmRx_SetRfDependentMuteMode(): Entered");

    status = 0;

nativeJFmRx_Callback(jContext,status, FM_RX_CMD_SET_RF_DEPENDENT_MUTE_MODE,status);
    V4L2_JBTL_LOGD("nativeJFmRx_SetRfDependentMuteMode(): Exit");
     return FM_PENDING;



}


static int    nativeJFmRx_GetRfDependentMute(JNIEnv *env, jobject obj,jlong jContextValue)
{

    int status;
     unsigned char rf_mute;
    V4L2_JBTL_LOGD(" nativeJFmRx_GetRfDependentMute(): Entered");

    status = 0;

nativeJFmRx_Callback(jContext,status, FM_RX_CMD_GET_RF_DEPENDENT_MUTE_MODE,rf_mute);
    V4L2_JBTL_LOGD(" nativeJFmRx_GetRfDependentMute(): Exit");
     return FM_PENDING;

}


static int    nativeJFmRx_SetRdsSystem(JNIEnv *env, jobject obj,jlong jContextValue, jint rdsSystem)
{
    int status;

     V4L2_JBTL_LOGD(" nativeJFmRx_SetRdsSystem(): Entered");

    V4L2_JBTL_LOGD("entered to ELSE\n");
    status = 0;

    V4L2_JBTL_LOGD("Set to %d\n",rdsSystem);
    nativeJFmRx_Callback(jContext,status,FM_RX_CMD_SET_RDS_SYSTEM,status);
    V4L2_JBTL_LOGD(" nativeJFmRx_SetRdsSystem(): Exit");
     return FM_PENDING;



}


static  int   nativeJFmRx_GetRdsSystem(JNIEnv *env, jobject obj,jlong jContextValue)
{

    int status;
     unsigned char mode;

    V4L2_JBTL_LOGD("nativeJFmRx_GetRdsSystem(): Entered");

    status = 0;

nativeJFmRx_Callback(jContext,status, FM_RX_CMD_GET_RDS_SYSTEM,mode);
    V4L2_JBTL_LOGD(" nativeJFmRx_GetRdsSystem(): Exit");
     return FM_PENDING;

}


static int   nativeJFmRx_SetRdsGroupMask(JNIEnv *env, jobject obj,jlong jContextValue, jlong groupMask)
{
int status =0;
    V4L2_JBTL_LOGD("nativeJFmRx_SetRdsGroupMask(): Entered");


nativeJFmRx_Callback(jContext,status, FM_RX_CMD_SET_RDS_GROUP_MASK,status);
    V4L2_JBTL_LOGD(" nativeJFmRx_SetRdsGroupMask(): Exit");
     return FM_PENDING;

}

static int   nativeJFmRx_GetRdsGroupMask(JNIEnv *env, jobject obj,jlong jContextValue)
{
int status =0;
    V4L2_JBTL_LOGD("nativeJFmRx_GetRdsGroupMask(): Entered");


nativeJFmRx_Callback(jContext,status, FM_RX_CMD_GET_RDS_GROUP_MASK,status);
    V4L2_JBTL_LOGD(" nativeJFmRx_GetRdsGroupMask(): Exit");
     return FM_PENDING;

}

static int nativeJFmRx_CompleteScan(JNIEnv *env, jobject obj, jlong jContextValue)
{

int status =0;
    ALOGD("nativeJFmRx_CompleteScan(): Entered");

    //nativeJFmRx_Callback(jContext,status, FM_RX_CMD_COMPLETE_SCAN,status);
    ALOGD("nativeJFmRx_CompleteScan(): Exit");
     return FM_PENDING;
}

static int nativeJFmRx_GetCompleteScanProgress(JNIEnv *env, jobject obj, jlong jContextValue)
{
int status =0;
    ALOGD("nativeJFmRx_GetCompleteScanProgress(): Entered");
  //nativeJFmRx_Callback(jContext,status, FM_RX_CMD_COMPLETE_SCAN_PROGRESS,status);

    ALOGD("nativeJFmRx_GetCompleteScanProgress(): Exit");
     return FM_PENDING;
}

static int nativeJFmRx_StopCompleteScan(JNIEnv *env, jobject obj, jlong jContextValue)
{

    ALOGD("nativeJFmRx_StopCompleteScan(): Entered");

 //nativeJFmRx_Callback(jContext,status, FM_RX_CMD_STOP_COMPLETE_SCAN,status);
    ALOGD("nativeJFmRx_StopCompleteScan(): Exit");
     return FM_PENDING;
}

static int nativeJFmRx_IsValidChannel(JNIEnv *env, jobject obj, jlong jContextValue)
{


    ALOGD("nativeJFmRx_IsValidChannel(): Entered");
//nativeJFmRx_Callback(jContext,status, FM_RX_CMD_IS_CHANNEL_VALID ,status);
    ALOGD("nativeJFmRx_IsValidChannel(): Exit");
     return FM_PENDING;
}


static int nativeJFmRx_GetFwVersion(JNIEnv *env, jobject obj, jlong jContextValue)
{

    ALOGD("nativeJFmRx_GetFwVersion(): Entered");
//nativeJFmRx_Callback(jContext,status, FM_RX_CMD_GET_FW_VERSION,status);
    ALOGD("nativeJFmRx_GetFwVersion(): Exit");
     return FM_PENDING;
}


//################################################################################

//                                 SIGNALS

//###############################################################################

extern "C"
{

void nativeJFmRx_RadioText_Callback(int status, bool resetDisplay,
            unsigned char * msg, int len, int startIndex,
            int repertoire)
{
    ALOGE("nativeJFmRx_RadioText_Callback: Entering");

ALOGE("nativeJFmRx_RadioText_Callback: msg %s",msg);
    JNIEnv* env = NULL;
        bool attachedThread = false;
    int jRet ;
    jbyteArray jRadioTxtMsg = NULL;

 /* check whether the current thread is attached to a virtual machine instance,
   if no only then try to attach to the current thread. */
        jRet = g_jVM->GetEnv((void **)&env,JNI_VERSION_1_4);

       if(jRet < 0)
       {
           ALOGE("failed to get JNI env,assuming native thread");
           jRet = g_jVM->AttachCurrentThread((&env), NULL);

           if(jRet != JNI_OK)
           {
               ALOGE("failed to atatch to current thread %d",jRet);
               return ;
           }

           attachedThread = true;
       }




       if(env == NULL) {
               ALOGI("%s: Entered, env is null", __func__);
           } else {
               ALOGD("%s: jEnv %p", __func__, (void *)env);
           }


V4L2_JBTL_LOGD("nativeJFmRx_Callback():EVENT --------------->FM_RX_EVENT_RADIO_TEXT");
        jRadioTxtMsg = env->NewByteArray(len);
        if (jRadioTxtMsg == NULL) {
            ALOGE("%s: Failed converting elements", __func__);
            goto CLEANUP;
        }

        env->SetByteArrayRegion(jRadioTxtMsg,
                0,
                len,
                (jbyte*)msg);

        if (env->ExceptionOccurred()) {
            ALOGE("%s: Calling nativeCb_fmRxRadioText failed",
                 __func__);
            goto CLEANUP;
        }

        env->CallStaticVoidMethod(_sJClass,
                _sMethodId_nativeCb_fmRxRadioText,(jlong)jContext,
                (jint)status,
                (jboolean)resetDisplay,
                jRadioTxtMsg,
                (jint)len,
                (jint)startIndex,
                (jint)repertoire);

    if (env->ExceptionOccurred()) {
            ALOGE("nativeJFmRx_RadioText_Callback:  ExceptionOccurred");
            goto CLEANUP;
        }

if(jRadioTxtMsg!= NULL)
        env->DeleteLocalRef(jRadioTxtMsg);

        if(attachedThread == true)
              g_jVM->DetachCurrentThread();

return ;

    CLEANUP:
ALOGE("nativeJFmRx_RadioText_Callback: Exiting due to failure");

if(jRadioTxtMsg!= NULL)
        env->DeleteLocalRef(jRadioTxtMsg);
    if (env->ExceptionOccurred())    {
    env->ExceptionDescribe();
    env->ExceptionClear();
    }

        if(attachedThread == true)
      g_jVM->DetachCurrentThread();
return ;
}





   void nativeJFmRx_PS_Callback(long context,int status, int freq,
               int len,unsigned char * name,
               int repertoire)

   {
       ALOGE("nativeJFmRx_PS_Callback: Exiting due to failure");
       JNIEnv* env = NULL;
           bool attachedThread = false;
       int jRet ;
       jbyteArray jNameString = NULL;
 int frequency =0;

   /* check whether the current thread is attached to a virtual machine instance,
   if no only then try to attach to the current thread. */
        jRet = g_jVM->GetEnv((void **)&env,JNI_VERSION_1_4);

       if(jRet < 0)
       {
           ALOGE("failed to get JNI env,assuming native thread");
           jRet = g_jVM->AttachCurrentThread((&env), NULL);

           if(jRet != JNI_OK)
           {
               ALOGE("failed to atatch to current thread %d",jRet);
               return ;
           }

           attachedThread = true;
       }




       if(env == NULL) {
               ALOGI("%s: Entered, env is null", __func__);
           } else {
               ALOGD("%s: jEnv %p", __func__, (void *)env);
           }

              V4L2_JBTL_LOGD("nativeJFmRx_PS_Callback():EVENT --------------->FM_RX_EVENT_PS_CHANGED len %d",len);


             jNameString = env->NewByteArray(len);

             if (jNameString == NULL)
             {
                 V4L2_JBTL_LOGD("nativeJFmRx_PS_Callback: Failed converting elements");
                 goto CLEANUP;
             }

             env->SetByteArrayRegion(jNameString,0,len,(jbyte*)name);

             if (env->ExceptionOccurred())      {
                 V4L2_JBTL_LOGD("nativeJFmRx_PS_Callback: Calling Java nativeCb_fmRxRadioText failed");
                 goto CLEANUP;
             }

             env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxPsChanged,(jlong)context,
                                       (jint)status,
                                       (jint)frequency,
                                       jNameString,
                                       (jint)repertoire);


       if (env->ExceptionOccurred()) {
               ALOGE("nativeJFmRx_PS_Callback:    ExceptionOccurred");
               goto CLEANUP;
           }

   if(jNameString!= NULL)
           env->DeleteLocalRef(jNameString);

           if(attachedThread == true)
                 g_jVM->DetachCurrentThread();

   return ;

       CLEANUP:
   ALOGE("nativeJFmRx_PS_Callback: Exiting due to failure");

   if(jNameString!= NULL)
           env->DeleteLocalRef(jNameString);
       if (env->ExceptionOccurred())    {
       env->ExceptionDescribe();
       env->ExceptionClear();
       }

           if(attachedThread == true)
         g_jVM->DetachCurrentThread();
   return ;
   }


    void nativeJFmRx_Callback(long context, int status,
            int command, long value)
    {

        V4L2_JBTL_LOGI("nativeJFmRx_Callback: Entered, ");

    JNIEnv* env = NULL;
    bool attachedThread = false;
    int jRet ;

/* check whether the current thread is attached to a virtual machine instance,
if no only then try to attach to the current thread. */

    jRet = g_jVM->GetEnv((void **)&env,JNI_VERSION_1_4);

    if(jRet < 0)
    {
         V4L2_JBTL_LOGI("failed to get JNI env,assuming native thread");
        jRet = g_jVM->AttachCurrentThread((&env), NULL);

        if(jRet != JNI_OK)
        {
             V4L2_JBTL_LOGI("failed to atatch to current thread %d",jRet);
            return ;
        }

        attachedThread = true;
    }

    if(env == NULL)
    {
         V4L2_JBTL_LOGD("nativeJFmRx_Callback: Entered, env is null");
    }


           switch (command)
    {

            case FM_RX_CMD_ENABLE:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdEnable,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_DISABLE:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdDisable,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_SET_BAND:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdSetBand,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_GET_BAND:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdGetBand,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_SET_MONO_STEREO_MODE:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdSetMonoStereoMode,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_GET_MONO_STEREO_MODE:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdGetMonoStereoMode,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_SET_MUTE_MODE:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdSetMuteMode,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_GET_MUTE_MODE:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdGetMuteMode,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_SET_RF_DEPENDENT_MUTE_MODE:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdSetRfDependentMuteMode,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_GET_RF_DEPENDENT_MUTE_MODE:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdGetRfDependentMuteMode,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_SET_RSSI_THRESHOLD:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdSetRssiThreshhold,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_GET_RSSI_THRESHOLD:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdGetRssiThreshhold,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_SET_DEEMPHASIS_FILTER:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdSetDeemphasisFilter,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_GET_DEEMPHASIS_FILTER:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdGetDeemphasisFilter,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_SET_VOLUME:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdSetVolume,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_GET_VOLUME:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdGetVolume,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_TUNE:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdTune,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_GET_TUNED_FREQUENCY:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdGetTunedFrequency,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_SEEK:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdSeek,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_STOP_SEEK:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdStopSeek,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_GET_RSSI:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdGetRssi,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_ENABLE_RDS:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdEnableRds,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_DISABLE_RDS:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdDisableRds,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_SET_RDS_SYSTEM:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdSetRdsSystem,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_GET_RDS_SYSTEM:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdGetRdsSystem,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_SET_RDS_GROUP_MASK:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdSetRdsGroupMask,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_GET_RDS_GROUP_MASK:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdGetRdsGroupMask,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_SET_RDS_AF_SWITCH_MODE:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdSetRdsAfSwitchMode,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_GET_RDS_AF_SWITCH_MODE:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdGetRdsAfSwitchMode,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_ENABLE_AUDIO:
        V4L2_JBTL_LOGD("nativeJFmRx_Callback: at FM_RX_CMD_ENABLE_AUDIO step 1");
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdEnableAudio,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
        V4L2_JBTL_LOGD("nativeJFmRx_Callback: at FM_RX_CMD_ENABLE_AUDIO step 2");
                break;

            case FM_RX_CMD_DISABLE_AUDIO:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdDisableAudio, (jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_CHANGE_AUDIO_TARGET:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdChangeAudioTarget,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_CHANGE_DIGITAL_AUDIO_CONFIGURATION:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdChangeDigitalAudioConfiguration,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_SET_CHANNEL_SPACING:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdSetChannelSpacing,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_GET_CHANNEL_SPACING:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdGetChannelSpacing,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_GET_FW_VERSION:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdGetFwVersion,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_IS_CHANNEL_VALID:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdIsValidChannel,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_COMPLETE_SCAN_PROGRESS:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdGetCompleteScanProgress,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            case FM_RX_CMD_STOP_COMPLETE_SCAN:
                env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmRxCmdStopCompleteScan,(jlong)context,
                                          (jint)status,
                                          (jint)command,
                                          (jlong)value);
                break;

            default:
                V4L2_JBTL_LOGD("nativeJFmRx_Callback:FM_RX_EVENT_CMD_DONE,unhendeld event");
                break;
            }

        if (env->ExceptionOccurred())    {
            V4L2_JBTL_LOGD("nativeJFmRx_Callback:  ExceptionOccurred");
            goto EXCEPTION;
        }

        V4L2_JBTL_LOGD("nativeJFmRx_Callback: Exiting, Calling DetachCurrentThread at the END");
     if(attachedThread == true)
        g_jVM->DetachCurrentThread();

        return;

EXCEPTION:

        /*Delete Jni Local refrencece */
        V4L2_JBTL_LOGD("nativeJFmRx_Callback: Exiting due to failure");
        if (env->ExceptionOccurred())    {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
 if(attachedThread == true)
        g_jVM->DetachCurrentThread();

        return;

    }


} //extern c


/**********************************************************************
*                Callback registration

***********************************************************************/
#define VERIFY_METHOD_ID(methodId) \
        if (!_VerifyMethodId(methodId, #methodId)) { \
            V4L2_JBTL_LOGD("Error obtaining method id for %s", #methodId);    \
            return;     \
        }

static bool _VerifyMethodId(jmethodID methodId, const char *name)
{
    bool result = true;

    if (methodId == NULL)
    {
        V4L2_JBTL_LOGD("_VerifyMethodId: Failed getting method id of %s", name);
        result = false;
    }

    return result;
}



void nativeJFmRx_ClassInitNative(JNIEnv* env, jclass clazz){
    V4L2_JBTL_LOGD("nativeJFmRx_ClassInitNative: Entered");

    if (NULL == env)
    {
        V4L2_JBTL_LOGD("nativeJFmRx_ClassInitNative: NULL == env");
    }

    env->GetJavaVM(&g_jVM);



    /* Save class information in global reference in order to prevent class unloading */
    _sJClass = (jclass)env->NewGlobalRef(clazz);


    V4L2_JBTL_LOGI("nativeJFmRx_ClassInitNative: Obtaining method IDs");

    _sMethodId_nativeCb_fmRxRadioText  = env->GetStaticMethodID(clazz,
                                         "nativeCb_fmRxRadioText",
                                         "(JIZ[BIII)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxRadioText);


    _sMethodId_nativeCb_fmRxPsChanged  = env->GetStaticMethodID(clazz,
                                         "nativeCb_fmRxPsChanged",
                                         "(JII[BI)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxPsChanged);

    /* Complete parsing of the RDS data has not been implemented yet
    Commented the FM RX RDS callbacks functionality start*/

    #if 0
    _sMethodId_nativeCb_fmRxRawRDS = env->GetStaticMethodID(clazz,
                                     "nativeCb_fmRxRawRDS",
                                     "(JII[B)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxRawRDS);


    _sMethodId_nativeCb_fmRxPiCodeChanged  = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxPiCodeChanged",
            "(JII)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxPiCodeChanged);


    _sMethodId_nativeCb_fmRxPtyCodeChanged  = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxPtyCodeChanged",
            "(JII)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxPtyCodeChanged);




    _sMethodId_nativeCb_fmRxMonoStereoModeChanged  = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxMonoStereoModeChanged",
            "(JII)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxMonoStereoModeChanged);


    _sMethodId_nativeCb_fmRxAudioPathChanged  = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxAudioPathChanged",
            "(JI)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxAudioPathChanged);


    _sMethodId_nativeCb_fmRxAfSwitchFreqFailed  = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxAfSwitchFreqFailed",
            "(JIIII)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxAfSwitchFreqFailed);


    _sMethodId_nativeCb_fmRxAfSwitchStart  = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxAfSwitchStart",
            "(JIIII)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxAfSwitchStart);


    _sMethodId_nativeCb_fmRxAfSwitchComplete  = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxAfSwitchComplete",
            "(JIIII)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxAfSwitchComplete);


    _sMethodId_nativeCb_fmRxAfListChanged  = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxAfListChanged",
            "(JII[BI)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxAfListChanged);

#endif
    /*Commented the FM RX RDS callbacks functionality end*/


    _sMethodId_nativeCb_fmRxCmdEnable = env->GetStaticMethodID(clazz,
                                        "nativeCb_fmRxCmdEnable",
                                        "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdEnable);


    _sMethodId_nativeCb_fmRxCmdDisable = env->GetStaticMethodID(clazz,
                                         "nativeCb_fmRxCmdDisable",
                                         "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdDisable);

    _sMethodId_nativeCb_fmRxCmdEnableAudio = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdEnableAudio",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdEnableAudio);



    _sMethodId_nativeCb_fmRxCmdChangeAudioTarget = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdChangeAudioTarget",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdChangeAudioTarget);


    _sMethodId_nativeCb_fmRxCmdSetBand = env->GetStaticMethodID(clazz,
                                         "nativeCb_fmRxCmdSetBand",
                                         "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdSetBand);

    _sMethodId_nativeCb_fmRxCmdGetBand = env->GetStaticMethodID(clazz,
                                         "nativeCb_fmRxCmdGetBand",
                                         "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdGetBand);



    _sMethodId_nativeCb_fmRxCmdSetMonoStereoMode = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdSetMonoStereoMode",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdSetMonoStereoMode);



    _sMethodId_nativeCb_fmRxCmdGetMonoStereoMode = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdGetMonoStereoMode",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdGetMonoStereoMode);



    _sMethodId_nativeCb_fmRxCmdGetMuteMode = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdGetMuteMode",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdGetMuteMode);



    _sMethodId_nativeCb_fmRxCmdSetMuteMode = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdSetMuteMode",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdSetMuteMode);



    _sMethodId_nativeCb_fmRxCmdSetRfDependentMuteMode = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdSetRfDependentMuteMode",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdSetRfDependentMuteMode);



    _sMethodId_nativeCb_fmRxCmdGetRfDependentMuteMode = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdGetRfDependentMuteMode",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdGetRfDependentMuteMode);



    _sMethodId_nativeCb_fmRxCmdSetRssiThreshhold = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdSetRssiThreshhold",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdSetRssiThreshhold);



    _sMethodId_nativeCb_fmRxCmdGetRssiThreshhold = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdGetRssiThreshhold",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdGetRssiThreshhold);



    _sMethodId_nativeCb_fmRxCmdSetDeemphasisFilter = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdSetDeemphasisFilter",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdSetDeemphasisFilter);


    _sMethodId_nativeCb_fmRxCmdGetDeemphasisFilter = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdGetDeemphasisFilter",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdGetDeemphasisFilter);



    _sMethodId_nativeCb_fmRxCmdSetVolume = env->GetStaticMethodID(clazz,
                                           "nativeCb_fmRxCmdSetVolume",
                                           "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdSetVolume);



    _sMethodId_nativeCb_fmRxCmdGetVolume = env->GetStaticMethodID(clazz,
                                           "nativeCb_fmRxCmdGetVolume",
                                           "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdGetVolume);

    _sMethodId_nativeCb_fmRxCmdSetChannelSpacing = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdSetChannelSpacing",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdSetChannelSpacing);



    _sMethodId_nativeCb_fmRxCmdGetChannelSpacing = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdGetChannelSpacing",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdGetChannelSpacing);



    _sMethodId_nativeCb_fmRxCmdTune = env->GetStaticMethodID(clazz,
                                      "nativeCb_fmRxCmdTune",
                                      "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdTune);


    _sMethodId_nativeCb_fmRxCmdGetTunedFrequency = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdGetTunedFrequency",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdGetTunedFrequency);


    _sMethodId_nativeCb_fmRxCmdSeek = env->GetStaticMethodID(clazz,
                                      "nativeCb_fmRxCmdSeek",
                                      "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdSeek);



    _sMethodId_nativeCb_fmRxCmdStopSeek = env->GetStaticMethodID(clazz,
                                          "nativeCb_fmRxCmdStopSeek",
                                          "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdStopSeek);


    _sMethodId_nativeCb_fmRxCmdGetRssi = env->GetStaticMethodID(clazz,
                                         "nativeCb_fmRxCmdGetRssi",
                                         "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdGetRssi);


    _sMethodId_nativeCb_fmRxCmdEnableRds = env->GetStaticMethodID(clazz,
                                           "nativeCb_fmRxCmdEnableRds",
                                           "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdEnableRds);


    _sMethodId_nativeCb_fmRxCmdDisableRds = env->GetStaticMethodID(clazz,
                                            "nativeCb_fmRxCmdDisableRds",
                                            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdDisableRds);


    _sMethodId_nativeCb_fmRxCmdGetRdsSystem = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdGetRdsSystem",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdGetRdsSystem);



    _sMethodId_nativeCb_fmRxCmdSetRdsSystem = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdSetRdsSystem",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdSetRdsSystem);


    _sMethodId_nativeCb_fmRxCmdSetRdsGroupMask = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdSetRdsGroupMask",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdSetRdsGroupMask);


    _sMethodId_nativeCb_fmRxCmdGetRdsGroupMask = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdGetRdsGroupMask",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdGetRdsGroupMask);


    _sMethodId_nativeCb_fmRxCmdSetRdsAfSwitchMode = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdSetRdsAfSwitchMode",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdSetRdsAfSwitchMode);


    _sMethodId_nativeCb_fmRxCmdGetRdsAfSwitchMode = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdGetRdsAfSwitchMode",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdGetRdsAfSwitchMode);


    _sMethodId_nativeCb_fmRxCmdDisableAudio = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdDisableAudio",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdDisableAudio);

    _sMethodId_nativeCb_fmRxCmdDestroy = env->GetStaticMethodID(clazz,
                                         "nativeCb_fmRxCmdDestroy",
                                         "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdDestroy);


    _sMethodId_nativeCb_fmRxCmdChangeDigitalAudioConfiguration= env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdChangeDigitalAudioConfiguration",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdChangeDigitalAudioConfiguration);

    /* Complete scan in V4l2 FM driver is not implemented yet
    Commented the FM RX Completescan functionality start*/

    /*_sMethodId_nativeCb_fmRxCompleteScanDone  = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCompleteScanDone",
            "(JII[I)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCompleteScanDone);*/

    /*Commented the FM RX Completescan functionality end*/


    _sMethodId_nativeCb_fmRxCmdGetFwVersion = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdGetFwVersion",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdGetFwVersion);

    _sMethodId_nativeCb_fmRxCmdIsValidChannel = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdIsValidChannel",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdIsValidChannel);


    _sMethodId_nativeCb_fmRxCmdGetCompleteScanProgress = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdGetCompleteScanProgress",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdGetCompleteScanProgress);

    _sMethodId_nativeCb_fmRxCmdStopCompleteScan = env->GetStaticMethodID(clazz,
            "nativeCb_fmRxCmdStopCompleteScan",
            "(JIIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmRxCmdStopCompleteScan);

    V4L2_JBTL_LOGD("nativeJFmRx_ClassInitNative:Exiting");
}

static JNINativeMethod JFmRxNative_sMethods[] = {
    /* name, signature, funcPtr */
    {"nativeJFmRx_ClassInitNative", "()V", (void*)nativeJFmRx_ClassInitNative},
    {"nativeJFmRx_Create", "(Lcom/ti/jfm/core/JFmContext;)I", (void*)nativeJFmRx_Create},
    {"nativeJFmRx_Destroy", "(J)I", (void*)nativeJFmRx_Destroy},
    {"nativeJFmRx_Enable", "(J)I", (void*)nativeJFmRx_Enable},
    {"nativeJFmRx_Disable", "(J)I", (void*)nativeJFmRx_Disable},
    {"nativeJFmRx_SetBand","(JI)I", (void*)nativeJFmRx_SetBand},
    {"nativeJFmRx_GetBand","(J)I", (void*)nativeJFmRx_GetBand},
    {"nativeJFmRx_Tune","(JI)I", (void*)nativeJFmRx_Tune},
    {"nativeJFmRx_GetTunedFrequency","(J)I", (void*)nativeJFmRx_GetTunedFrequency},
    {"nativeJFmRx_SetMonoStereoMode","(JI)I", (void*)nativeJFmRx_SetMonoStereoMode},
    {"nativeJFmRx_GetMonoStereoMode","(J)I", (void*)nativeJFmRx_GetMonoStereoMode},
    {"nativeJFmRx_SetMuteMode","(JI)I", (void*)nativeJFmRx_SetMuteMode},
    {"nativeJFmRx_GetMuteMode","(J)I", (void*)nativeJFmRx_GetMuteMode},
    {"nativeJFmRx_SetRssiThreshold","(JI)I", (void*)nativeJFmRx_SetRssiThreshold},
    {"nativeJFmRx_GetRssiThreshold","(J)I", (void*)nativeJFmRx_GetRssiThreshold},
    {"nativeJFmRx_GetRssi","(J)I", (void*)nativeJFmRx_GetRssi},
    {"nativeJFmRx_SetVolume","(JI)I", (void*)nativeJFmRx_SetVolume},
    {"nativeJFmRx_GetVolume","(J)I", (void*)nativeJFmRx_GetVolume},
    {"nativeJFmRx_SetChannelSpacing","(JI)I", (void*)nativeJFmRx_SetChannelSpacing},
    {"nativeJFmRx_GetChannelSpacing","(J)I", (void*)nativeJFmRx_GetChannelSpacing},
    {"nativeJFmRx_SetDeEmphasisFilter","(JI)I", (void*)nativeJFmRx_SetDeEmphasisFilter},
    {"nativeJFmRx_GetDeEmphasisFilter","(J)I", (void*)nativeJFmRx_GetDeEmphasisFilter},
    {"nativeJFmRx_Seek","(JI)I", (void*)nativeJFmRx_Seek},
    {"nativeJFmRx_StopSeek","(J)I", (void*)nativeJFmRx_StopSeek},
    {"nativeJFmRx_EnableRDS","(J)I", (void*)nativeJFmRx_EnableRDS},
    {"nativeJFmRx_DisableRDS","(J)I", (void*)nativeJFmRx_DisableRDS},
    {"nativeJFmRx_EnableAudioRouting","(J)I", (void*)nativeJFmRx_EnableAudioRouting},
    {"nativeJFmRx_DisableAudioRouting","(J)I", (void*)nativeJFmRx_DisableAudioRouting},
    {"nativeJFmRx_SetRdsAfSwitchMode","(JI)I", (void*)nativeJFmRx_SetRdsAfSwitchMode},
    {"nativeJFmRx_GetRdsAfSwitchMode","(J)I", (void*)nativeJFmRx_GetRdsAfSwitchMode},
    {"nativeJFmRx_ChangeAudioTarget","(JII)I",(void*)nativeJFmRx_ChangeAudioTarget},
    {"nativeJFmRx_ChangeDigitalTargetConfiguration","(JI)I",(void*)nativeJFmRx_ChangeDigitalTargetConfiguration},
    {"nativeJFmRx_SetRfDependentMuteMode","(JI)I",(void*)nativeJFmRx_SetRfDependentMuteMode},
    {"nativeJFmRx_GetRfDependentMute","(J)I",(void*)nativeJFmRx_GetRfDependentMute},
    {"nativeJFmRx_SetRdsSystem","(JI)I",(void*)nativeJFmRx_SetRdsSystem},
    {"nativeJFmRx_GetRdsSystem","(J)I",(void*)nativeJFmRx_GetRdsSystem},
    {"nativeJFmRx_SetRdsGroupMask","(JJ)I",(void*)nativeJFmRx_SetRdsGroupMask},
    {"nativeJFmRx_GetRdsGroupMask","(J)I",(void*)nativeJFmRx_GetRdsGroupMask},
    {"nativeJFmRx_CompleteScan","(J)I",(void*)nativeJFmRx_CompleteScan},
    {"nativeJFmRx_IsValidChannel","(J)I",(void*)nativeJFmRx_IsValidChannel},
    {"nativeJFmRx_GetFwVersion","(J)I",(void*)nativeJFmRx_GetFwVersion},
    {"nativeJFmRx_GetCompleteScanProgress","(J)I",(void*)nativeJFmRx_GetCompleteScanProgress},
    {"nativeJFmRx_StopCompleteScan","(J)I",(void*)nativeJFmRx_StopCompleteScan},

};

/**********************************/


/*
 * Register several native methods for one class.
 */
static int registerNatives(JNIEnv* env, const char* className,
               JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
         V4L2_JBTL_LOGD("Can not find class %s\n", className);
        return JNI_FALSE;
    }

    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        V4L2_JBTL_LOGD("Can not RegisterNatives\n");
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*Commented the FM TX functionality start*/

extern JNINativeMethod JFmTxNative_sMethods[];
extern int getTxNativeSize();

/*Commented the FM TX functionality end*/

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    V4L2_JBTL_LOGD("OnLoad");

    if (vm->GetEnv((void**)&env, JNI_VERSION_1_4) != JNI_OK) {
        goto bail;
    }

    if (!registerNatives(env,
                 "com/ti/jfm/core/JFmRx",
                         JFmRxNative_sMethods,
                 NELEM(JFmRxNative_sMethods))) {
        goto bail;
    }


    if (!registerNatives(env,
                 "com/ti/jfm/core/JFmTx",
                         JFmTxNative_sMethods,
                 getTxNativeSize())) {
        goto bail;
    }


    env->GetJavaVM(&g_jVM);

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}


