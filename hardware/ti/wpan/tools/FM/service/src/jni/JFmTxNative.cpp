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
#include "JFmTxNative.h"

#include <stdlib.h>

#define LOG_TAG "JFmTxNative"
#include <cutils/properties.h>

using namespace android;
static int radio_fd;
extern long jContext;

#undef VIDIOC_S_MODULATOR
#define VIDIOC_S_MODULATOR    1078220343

extern "C" {
#include <stdio.h>
#include <fcntl.h>
#include <asoundlib.h>
#include <linux/videodev.h>
#include <math.h>
#include <pthread.h>
#include <errno.h>
#include <string.h>

void nativeJFmTx_Callback(long context, int status,
                        int command, long value);
extern void MCP_HAL_LOG_EnableLogToAndroid(const char *app_name);

} //extern "C"

static jclass _sJClass;
static JavaVM *g_jVM = NULL;

static jmethodID _sMethodId_nativeCb_fmTxCmdEnable;
static jmethodID _sMethodId_nativeCb_fmTxCmdDisable;
static jmethodID _sMethodId_nativeCb_fmTxCmdDestroy;
static jmethodID _sMethodId_nativeCb_fmTxCmdTune;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetTunedFrequency;
static jmethodID _sMethodId_nativeCb_fmTxCmdStartTransmission;
static jmethodID _sMethodId_nativeCb_fmTxCmdStopTransmission;
static jmethodID _sMethodId_nativeCb_fmTxCmdEnableRds;
static jmethodID _sMethodId_nativeCb_fmTxCmdDisableRds;
static jmethodID _sMethodId_nativeCb_fmTxCmdSetRdsTextRtMsg;
static jmethodID _sMethodId_nativeCb_fmTxCmdSetRdsTextPsMsg;
/*
static jmethodID _sMethodId_nativeCb_fmTxCmdSetRdsTransmissionMode;
//  static jmethodID _sMethodId_nativeCb_fmTxCmdGetRdsTransmissionMode;
static jmethodID _sMethodId_nativeCb_fmTxCmdSetRdsTrafficCodes;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetRdsTrafficCodes;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetRdsTextPsMsg;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetRdsTextRtMsg;
static jmethodID _sMethodId_nativeCb_fmTxCmdWriteRdsRawData;
static jmethodID _sMethodId_nativeCb_fmTxCmdReadRdsRawData;
static jmethodID _sMethodId_nativeCb_fmTxCmdChangeAudioSource;
*/
static jmethodID _sMethodId_nativeCb_fmTxCmdSetInterruptMask;
static jmethodID _sMethodId_nativeCb_fmTxCmdSetMonoStereoMode;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetMonoStereoMode;
static jmethodID _sMethodId_nativeCb_fmTxCmdSetPowerLevel;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetPowerLevel;
static jmethodID _sMethodId_nativeCb_fmTxCmdSetMuteMode;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetMuteMode;
static jmethodID _sMethodId_nativeCb_fmTxCmdSetRdsAfCode;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetRdsAfCode;
static jmethodID _sMethodId_nativeCb_fmTxCmdSetRdsPiCode;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetRdsPiCode;
static jmethodID _sMethodId_nativeCb_fmTxCmdSetRdsPtyCode;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetRdsPtyCode;
static jmethodID _sMethodId_nativeCb_fmTxCmdSetRdsTextRepertoire;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetRdsTextRepertoire;
static jmethodID _sMethodId_nativeCb_fmTxCmdSetRdsPsDispalyMode;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetRdsPsDispalyMode;
static jmethodID _sMethodId_nativeCb_fmTxCmdSetRdsPsDisplaySpeed;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetRdsPsDisplaySpeed;
static jmethodID _sMethodId_nativeCb_fmTxCmdSetRdsTransmittedMask;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetRdsTransmittedMask;
static jmethodID _sMethodId_nativeCb_fmTxCmdSetRdsMusicSpeechFlag  ;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetRdsMusicSpeechFlag  ;
static jmethodID _sMethodId_nativeCb_fmTxCmdSetPreEmphasisFilter;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetPreEmphasisFilter;
static jmethodID _sMethodId_nativeCb_fmTxCmdSetRdsExtendedCountryCode;
static jmethodID _sMethodId_nativeCb_fmTxCmdGetRdsExtendedCountryCode;
static jmethodID _sMethodId_nativeCb_fmTxCmdChangeDigitalAudioConfiguration;



int fm_read_transmitter_capabilities(int radio_fd)
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

static int nativeJFmTx_Create(JNIEnv *env,jobject obj,jobject jContextValue)
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

   fmStatus = fm_read_transmitter_capabilities(radio_fd);
   if(fmStatus< 0)
   {
     close(radio_fd);
     return fmStatus;
   }

    V4L2_JBTL_LOGD("nativeJFmRx_create:Exiting Successfully");

    return fmStatus;
return 0;
}

static jint nativeJFmTx_Destroy(JNIEnv *env, jobject obj,jlong jContextValue)
{

return 0;

}



static int nativeJFmTx_Enable(JNIEnv *env, jobject obj, jlong jContextValue)
{
   int  status ;
   struct v4l2_modulator vm;

   V4L2_JBTL_LOGD("nativeJFmRx_enable(): Entered");

   jContext = jContextValue;
   vm.index = 0;

   status = ioctl(radio_fd, VIDIOC_S_MODULATOR, &vm);
   if(status < 0)
   {
     V4L2_JBTL_LOGD("Failed to Enable FM\n");
     return status;
   }

   V4L2_JBTL_LOGD("nativeJFmRx_enable: FM_RX_Enable() returned %d",(int)status);
   nativeJFmTx_Callback(jContext,status,FM_TX_CMD_ENABLE,status);
   V4L2_JBTL_LOGD("nativeJFmRx_enable(): Exit");

   return status;
}


static int nativeJFmTx_Disable(JNIEnv *env, jobject obj, jlong jContextValue)
{
    V4L2_JBTL_LOGD("nativeJFmTx_disable(): Entered");

   jContext = jContextValue;
    close(radio_fd);
    nativeJFmTx_Callback(jContext,0,FM_TX_CMD_DISABLE,0);

    V4L2_JBTL_LOGD("nativeJFmTx_disable(): Exit");;

    return FM_SUCCESS;
}


static int nativeJFmTx_Tune(JNIEnv *env, jobject obj,jlong jContextValue,jlong user_freq)
{
    struct v4l2_frequency vf;
    struct v4l2_tuner vt;
    int status, div;

    V4L2_JBTL_LOGD("nativeJFmRx_tune(): Entered");

    vf.tuner = 0;
    vf.frequency = rint(user_freq * 16 + 0.5);

    status = ioctl(radio_fd, VIDIOC_S_FREQUENCY, &vf);
    if(status < 0)
    {
        V4L2_JBTL_LOGD("Failed to tune to frequency %d\n",user_freq);
        return FM_FAILED;
    }
    V4L2_JBTL_LOGD("Tuned to frequency %2.1f MHz\n",user_freq);

   jContext = jContextValue;
nativeJFmTx_Callback(jContext,status,FM_TX_CMD_TUNE,user_freq);

    V4L2_JBTL_LOGD("nativeJFmRx_Tune(): Exit");
     return FM_PENDING;

}

static int nativeJFmTx_GetTunedFrequency(JNIEnv *env, jobject obj,jlong jContextValue)
{
return 0;

}
static int nativeJFmTx_StartTransmission(JNIEnv *env, jobject obj, jlong jContextValue)
{
   V4L2_JBTL_LOGD("nativeJFmRx_enable(): Init");
   jContext = jContextValue;
   nativeJFmTx_Callback(jContext,0,FM_TX_CMD_START_TRANSMISSION,0);
   V4L2_JBTL_LOGD("nativeJFmRx_enable(): Exit");

return 0;

}

static int nativeJFmTx_StopTransmission(JNIEnv *env, jobject obj, jlong jContextValue)
{
   V4L2_JBTL_LOGD("nativeJFmRx_enable(): Init");
   jContext = jContextValue;
   nativeJFmTx_Callback(jContext,0,FM_TX_CMD_STOP_TRANSMISSION,0);
   V4L2_JBTL_LOGD("nativeJFmRx_enable(): Exit");
return 0;

}


static int nativeJFmTx_EnableRds(JNIEnv *env, jobject obj, jlong jContextValue)
{
  struct v4l2_modulator vmod;
  int ret;

  V4L2_JBTL_LOGD("nativeJFmTx_EnableRds(): Init");
  vmod.index = 0;
  ret = ioctl(radio_fd, VIDIOC_G_MODULATOR, &vmod);
  if(ret < 0)
  {
      printf("Failed to get TX mode\n");
      return -1;
  }
  vmod.txsubchans = vmod.txsubchans | V4L2_TUNER_SUB_RDS;

  ret = ioctl(radio_fd, VIDIOC_S_MODULATOR, &vmod);
  if(ret < 0)
  {
      printf("Failed to set TX mode\n");
      return -1;
  }

   jContext = jContextValue;
  nativeJFmTx_Callback(jContext,0,FM_TX_CMD_ENABLE_RDS,0);

    V4L2_JBTL_LOGD("nativeJFmTx_EnableRds(): Exit");;

return 0;

}


static int nativeJFmTx_DisableRds(JNIEnv *env, jobject obj, jlong jContextValue)
{
  struct v4l2_modulator vmod;
  int ret;

  V4L2_JBTL_LOGD("nativeJFmTx_DisableRds(): Init");
  vmod.index = 0;
  ret = ioctl(radio_fd, VIDIOC_G_MODULATOR, &vmod);
  if(ret < 0)
  {
      printf("Failed to get TX mode\n");
      return -1;
  }
  vmod.txsubchans = vmod.txsubchans & ~V4L2_TUNER_SUB_RDS;

  ret = ioctl(radio_fd, VIDIOC_S_MODULATOR, &vmod);
  if(ret < 0)
  {
      printf("Failed to set TX mode\n");
      return -1;
  }

  jContext = jContextValue;
  nativeJFmTx_Callback(jContext,0,FM_TX_CMD_DISABLE_RDS,0);

    V4L2_JBTL_LOGD("nativeJFmTx_DisableRds(): Exit");;

return 0;
}

static int nativeJFmTx_SetRdsTransmissionMode(JNIEnv *env, jobject obj, jlong jContextValue,jint mode)
{

/*    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_SetRdsTransmissionMode(): Entered");

    FmTxStatus  status =FM_TX_SetRdsTransmissionMode(fmTxContext,(FmTxRdsTransmissionMode)mode);
    ALOGD("nativeJFmTx_SetRdsTransmissionMode: FM_TX_SetRdsTransmissionMode() returned %d",(int)status);
*/
    ALOGD("nativeJFmTx_SetRdsTransmissionMode(): Exit");
    return 0;
}

/*
static int nativeJFmTx_GetRdsTransmissionMode(JNIEnv *env, jobject obj, jlong jContextValue)
{

    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_GetRdsTransmissionMode(): Entered");

    FmTxStatus  status =FM_TX_GetRdsTransmissionMode(fmTxContext);
    ALOGD("nativeJFmTx_GetRdsTransmissionMode: FM_TX_GetRdsTransmissionMode() returned %d",(int)status);

    ALOGD("nativeJFmTx_GetRdsTransmissionMode(): Exit");
    return status;
}

*/
static int nativeJFmTx_SetRdsTextPsMsg(JNIEnv *env, jobject obj, jlong jContextValue,jstring psString,jint length)
{

/*    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    const char *psStr = (char*) env->GetStringUTFChars(psString, &iscopy);

    ALOGD("nativeJFmTx_SetRdsTextPsMsg(): Entered");
    ALOGD("nativeJFmTx_SetRdsTextPsMsg():--> psStr= %s",psStr);

    FmTxStatus  status =FM_TX_SetRdsTextPsMsg(fmTxContext,(const FMC_U8 *)psStr,(FMC_UINT)length);
    ALOGD("nativeJFmTx_SetRdsTextPsMsg: FM_TX_SetRdsTextPsMsg() returned %d",(int)status);
*/
    struct v4l2_ext_controls_kfmapp vec;
    struct v4l2_ext_control_kfmapp vctrls;
        jboolean iscopy;
    int res;
    char rds_text[100];

    vec.ctrl_class = V4L2_CTRL_CLASS_FM_TX;
    vec.count = 1;
    vctrls.id = V4L2_CID_RDS_TX_PS_NAME;
    vctrls.string = (char*) env->GetStringUTFChars(psString, &iscopy);
    vctrls.size = strlen(rds_text) + 1;
    vec.controls = &vctrls;

    ALOGE("Entered RDS  PS Name is - %s\n",vctrls.string);
    res = ioctl(radio_fd, VIDIOC_S_EXT_CTRLS, &vec);
    if(res < 0)
    {
        ALOGE("Failed to set FM Tx RDS Radio PS Name\n");
        return res;
    }

    ALOGD("nativeJFmTx_SetRdsTextPsMsg(): Exit");

    return res;
}
/*
static int nativeJFmTx_GetRdsTextPsMsg(JNIEnv *env, jobject obj, jlong jContextValue)
{

    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_GetRdsTextPsMsg(): Entered");

    FmTxStatus  status =FM_TX_GetRdsTextPsMsg(fmTxContext);
    ALOGD("nativeJFmTx_GetRdsTextPsMsg: FM_TX_GetRdsTextPsMsg() returned %d",(int)status);

    ALOGD("nativeJFmTx_GetRdsTextPsMsg(): Exit");

    return status;
}

static int nativeJFmTx_WriteRdsRawData(JNIEnv *env, jobject obj, jlong jContextValue,jstring msg,jint length)
{

    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_WriteRdsRawData(): Entered");

    jboolean iscopy;
    const char *rawData = (char*) env->GetStringUTFChars(msg, &iscopy);


    FmTxStatus  status =FM_TX_WriteRdsRawData(fmTxContext,(const FMC_U8 *)rawData,(FMC_UINT)length);
    ALOGD("nativeJFmTx_WriteRdsRawData: FM_TX_WriteRdsRawData() returned %d",(int)status);

    ALOGD("nativeJFmTx_WriteRdsRawData(): Exit");
    return status;
}


static int nativeJFmTx_ReadRdsRawData(JNIEnv *env, jobject obj, jlong jContextValue)
{

    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_ReadRdsRawData(): Entered");

    FmTxStatus  status =FM_TX_ReadRdsRawData(fmTxContext);
    ALOGD("nativeJFmTx_ReadRdsRawData: FM_TX_ReadRdsRawData() returned %d",(int)status);

    ALOGD("nativeJFmTx_ReadRdsRawData(): Exit");
    return status;
}
*/
static int nativeJFmTx_SetMuteMode(JNIEnv *env, jobject obj, jlong jContextValue,jint mode)
{

   struct v4l2_control vt;
   int status;

   ALOGD("nativeJFmTx_SetMuteMode(): Entered");
   vt.id = V4L2_CID_AUDIO_MUTE;

   if (mode == 0)
       vt.value = FM_MUTE_OFF;
   else
    vt.value = FM_MUTE_ON;

   status = ioctl(radio_fd,VIDIOC_S_CTRL,&vt);
   if(status < 0)
   {
     ALOGD("nativeJFmTx_SetMuteMode(): Faile returned %d\n", status);
     return status;
   }

    ALOGD("nativeJFmTx_SetMuteMode(): Exit");
    return status;
}

static int nativeJFmTx_GetMuteMode(JNIEnv *env, jobject obj, jlong jContextValue)
{
    ALOGD("nativeJFmTx_GetMuteMode(): Entered");

    ALOGD("nativeJFmTx_GetMuteMode(): Exit");
    return 0;
}
/*
static int nativeJFmTx_SetRdsPsDisplayMode(JNIEnv *env, jobject obj, jlong jContextValue, jint displayMode)
{

    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_SetRdsPsDisplayMode(): Entered");

    FmTxStatus  status =FM_TX_SetRdsPsDisplayMode(fmTxContext,(FmcRdsPsDisplayMode)displayMode);
    ALOGD("nativeJFmTx_SetRdsPsDisplayMode: FM_TX_SetRdsPsDisplayMode() returned %d",(int)status);

    ALOGD("nativeJFmTx_SetRdsPsDisplayMode(): Exit");
    return status;
}


static int nativeJFmTx_GetRdsPsDisplayMode(JNIEnv *env, jobject obj, jlong jContextValue)
{

    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_GetRdsPsDisplayMode(): Entered");

    FmTxStatus  status =FM_TX_GetRdsPsDisplayMode(fmTxContext);
    ALOGD("nativeJFmTx_GetRdsPsDisplayMode: FM_TX_GetRdsPsDisplayMode() returned %d",(int)status);

    ALOGD("nativeJFmTx_GetRdsPsDisplayMode(): Exit");
    return status;
}

*/
static int nativeJFmTx_SetRdsTextRtMsg(JNIEnv *env, jobject obj, jlong jContextValue, jint msgType,jstring msg,jint length)
{

 //   FmTxContext * fmTxContext = (FmTxContext *)jContextValue;

    jboolean iscopy;
    const char *rtMsg = (char*) env->GetStringUTFChars(msg, &iscopy);
    struct v4l2_ext_controls_kfmapp vec;
    struct v4l2_ext_control_kfmapp vctrls;
    int res;
    char rds_text[100];

    ALOGD("nativeJFmTx_SetRdsTextRtMsg(): Entered");


    vec.ctrl_class = V4L2_CTRL_CLASS_FM_TX;
    vec.count = 1;
    vctrls.id = V4L2_CID_RDS_TX_RADIO_TEXT;
    vctrls.string = (char*) env->GetStringUTFChars(msg, &iscopy);
    vctrls.size = strlen(rtMsg) + 1;
    vec.controls = &vctrls;

    ALOGD("nativeJFmTx_SetRdsTextRtMsg():--> RTMsg = %s",vctrls.string);
    res = ioctl(radio_fd, VIDIOC_S_EXT_CTRLS, &vec);
    if(res < 0)
    {
            ALOGE("Failed to set FM Tx RDS Radio text");
        return res;
    }

    //    FmTxStatus  status =FM_TX_SetRdsTextRtMsg(fmTxContext,(FmcRdsRtMsgType)msgType,(const FMC_U8 *)rtMsg,(FMC_UINT)length);
    //    ALOGD("nativeJFmTx_SetRdsTextRtMsg: FM_TX_SetRdsTextRtMsg() returned %d",(int)status);

    ALOGD("nativeJFmTx_SetRdsTextRtMsg(): Exit");
    return 0;
}

/*
static int nativeJFmTx_GetRdsTextRtMsg(JNIEnv *env, jobject obj, jlong jContextValue)
{

    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_GetRdsTextRtMsg(): Entered");
    FmTxStatus  status =FM_TX_GetRdsTextRtMsg(fmTxContext);
    ALOGD("nativeJFmTx_GetRdsTextRtMsg: FM_TX_SetRdsTextRtMsg() returned %d",(int)status);

    ALOGD("nativeJFmTx_GetRdsTextRtMsg(): Exit");
    return status;
}

*/
static int nativeJFmTx_SetRdsTransmittedGroupsMask(JNIEnv *env, jobject obj, jlong jContextValue, jlong fieldMask)
{

/*    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_SetRdsTransmittedGroupsMask(): Entered");

    FmTxStatus  status =FM_TX_SetRdsTransmittedGroupsMask(fmTxContext,(FmTxRdsTransmittedGroupsMask)fieldMask);
    ALOGD("nativeJFmTx_SetRdsTransmittedGroupsMask: FM_TX_SetRdsTransmittedGroupsMask() returned %d",(int)status);
*/
    ALOGD("nativeJFmTx_SetRdsTransmittedGroupsMask(): Exit");
    return 0;
}

/*
static int nativeJFmTx_GetRdsTransmittedGroupsMask(JNIEnv *env, jobject obj, jlong jContextValue)
{

    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_GetRdsTransmittedGroupsMask(): Entered");

    FmTxStatus  status =FM_TX_GetRdsTransmittedGroupsMask(fmTxContext);
    ALOGD("nativeJFmTx_GetRdsTransmittedGroupsMask: FM_TX_GetRdsTransmittedGroupsMask() returned %d",(int)status);

    ALOGD("nativeJFmTx_GetRdsTransmittedGroupsMask(): Exit");
    return status;
}

static int nativeJFmTx_SetRdsTrafficCodes(JNIEnv *env, jobject obj, jlong jContextValue, jint taCode,jint tpCode)
{

    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_SetRdsTrafficCodes(): Entered");

    FmTxStatus  status =FM_TX_SetRdsTrafficCodes(fmTxContext,(FmcRdsTaCode)taCode,(FmcRdsTpCode)tpCode);
    ALOGD("nativeJFmTx_SetRdsTrafficCodes: FM_TX_SetRdsTrafficCodes() returned %d",(int)status);

    ALOGD("nativeJFmTx_SetRdsTrafficCodes(): Exit");
    return status;
}

static int nativeJFmTx_GetRdsTrafficCodes(JNIEnv *env, jobject obj, jlong jContextValue)
{

    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_GetRdsTrafficCodes(): Entered");

    FmTxStatus  status =FM_TX_GetRdsTrafficCodes(fmTxContext);
    ALOGD("nativeJFmTx_GetRdsTrafficCodes: FM_TX_GetRdsTrafficCodes() returned %d",(int)status);

    ALOGD("nativeJFmTx_GetRdsTrafficCodes(): Exit");
    return status;
}

static int nativeJFmTx_SetRdsMusicSpeechFlag(JNIEnv *env, jobject obj, jlong jContextValue, jint musicSpeechFlag)
{

    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_SetRdsMusicSpeechFlag(): Entered");

    FmTxStatus  status =FM_TX_SetRdsMusicSpeechFlag(fmTxContext,(FmcRdsMusicSpeechFlag)musicSpeechFlag);
    ALOGD("nativeJFmTx_SetRdsMusicSpeechFlag: FM_TX_SetRdsMusicSpeechFlag() returned %d",(int)status);

    ALOGD("nativeJFmTx_SetRdsMusicSpeechFlag(): Exit");
    return status;
}


static int nativeJFmTx_GetRdsMusicSpeechFlag(JNIEnv *env, jobject obj, jlong jContextValue)
{

    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_GetRdsMusicSpeechFlag(): Entered");

    FmTxStatus  status =FM_TX_GetRdsMusicSpeechFlag(fmTxContext);
    ALOGD("nativeJFmTx_GetRdsMusicSpeechFlag: FM_TX_GetRdsMusicSpeechFlag() returned %d",(int)status);

    ALOGD("nativeJFmTx_GetRdsMusicSpeechFlag(): Exit");
    return status;
}

static int nativeJFmTx_SetRdsExtendedCountryCode(JNIEnv *env, jobject obj, jlong jContextValue, jint ecc)
{

    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_SetRdsExtendedCountryCode(): Entered");

    FmTxStatus  status =FM_TX_SetRdsECC(fmTxContext,(FmcRdsExtendedCountryCode)ecc);
    ALOGD("nativeJFmTx_SetRdsExtendedCountryCode: FM_TX_SetRdsECC() returned %d",(int)status);

    ALOGD("nativeJFmTx_SetRdsExtendedCountryCode(): Exit");
    return status;
}

static int nativeJFmTx_GetRdsExtendedCountryCode(JNIEnv *env, jobject obj, jlong jContextValue)
{

    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_GetRdsExtendedCountryCode(): Entered");

    FmTxStatus  status =FM_TX_GetRdsECC(fmTxContext);
    ALOGD("nativeJFmTx_GetRdsExtendedCountryCode: FM_TX_GetRdsECC() returned %d",(int)status);

    ALOGD("nativeJFmTx_GetRdsExtendedCountryCode(): Exit");
    return status;
}

static int nativeJFmTx_ChangeAudioSource(JNIEnv *env, jobject obj, jlong jContextValue,jint txSource,jint eSampleFreq)
{

    FmTxContext * fmTxContext = (FmTxContext *)jContextValue;
    ALOGD("nativeJFmTx_ChangeAudioSource(): Entered");

    ALOGD(" txSource = %d , Sampling frequency = %d ",(int) txSource, (int) eSampleFreq);
    FmTxStatus  status =FM_TX_ChangeAudioSource(fmTxContext,(FmTxAudioSource)txSource,(ECAL_SampleFrequency)eSampleFreq);
    ALOGD("nativeJFmTx_ChangeAudioSource: FM_TX_ChangeAudioSource() returned %d",(int)status);

    ALOGD("nativeJFmTx_ChangeAudioSource(): Exit");
    return status;
}
*/

static int nativeJFmTx_ChangeDigitalSourceConfiguration(JNIEnv *env, jobject obj, jlong jContextValue,jint eSampleFreq)
{
return 0;

}


static int nativeJFmTx_SetRdsTextRepertoire(JNIEnv *env, jobject obj, jlong jContextValue,jint repertoire)
{
return 0;

}


static int nativeJFmTx_GetRdsTextRepertoire(JNIEnv *env, jobject obj, jlong jContextValue,jint repertoire)
{
return 0;
}

static int nativeJFmTx_SetRdsPtyCode(JNIEnv *env, jobject obj, jlong jContextValue,jint ptyCode)
{
	int user_val;
	int res;
	struct v4l2_ext_controls_kfmapp vec;
	struct v4l2_ext_control_kfmapp vctrls;

	ALOGE("nativeJFmTx_SetRdsPtyCode(): Entered");

	vec.ctrl_class = V4L2_CTRL_CLASS_FM_TX;
	vec.count = 1;
	vctrls.id = V4L2_CID_RDS_TX_PTY;
	vctrls.value = ptyCode;
	vctrls.size = 0;
	vec.controls = &vctrls;

	res = ioctl(radio_fd, VIDIOC_S_EXT_CTRLS, &vec);
	if(res < 0)
	{
		ALOGE("Failed to set FM Tx RDS PTY\n");
		return res;
	}

	ALOGE("nativeJFmTx_SetRdsPtyCode(): Exit");

	return res;
}

static int nativeJFmTx_GetRdsPtyCode(JNIEnv *env, jobject obj, jlong jContextValue,jint ptyCode)
{
	return 0;
}

static int nativeJFmTx_SetRdsPiCode(JNIEnv *env, jobject obj, jlong jContextValue,jint piCode)
{
	struct v4l2_ext_controls_kfmapp vec;
	struct v4l2_ext_control_kfmapp vctrls;
	int res;

	ALOGD("nativeJFmTx_SetRdsPiCode(): Enter");

	vec.ctrl_class = V4L2_CTRL_CLASS_FM_TX;
	vec.count = 1;
	vctrls.id = V4L2_CID_RDS_TX_PI;
	vctrls.value = piCode;
	vctrls.size = 0;
	vec.controls = &vctrls;

	res = ioctl(radio_fd, VIDIOC_S_EXT_CTRLS, &vec);
	if(res < 0)
	{
		ALOGE("Failed to set FM Tx RDS PI Code\n");
		return res;
	}

	ALOGD("Setting FM Tx RDS PI Code is Succesful\n");

	return res;
}

static int nativeJFmTx_GetRdsPiCode(JNIEnv *env, jobject obj, jlong jContextValue)
{
return 0;
}
static int nativeJFmTx_SetRdsAfCode(JNIEnv *env, jobject obj, jlong jContextValue,jint afCode)
{
    int fd, res;
    char str[10];

    sprintf(str, "%d", afCode);

    ALOGD("nativeJFmTx_SetRdsAfCode(): Enter");

    fd = open(FMTX_RDS_AF_SYSFS_ENTRY, O_RDWR);
    if (fd < 0) {
        ALOGD("Can't open %s", FMTX_RDS_AF_SYSFS_ENTRY);
        return -1;
    }

    /* Need max 6 cahrs to set AF between 75000 KHz to 108000 KHz */
    res = write(fd, str, 6);
    if(res <= 0) {
        ALOGD("Failed to set FM TX RDS AF Frequency\n");
        goto exit;
    }

    ALOGD("FM RDS Alternate Frequency Set is succesfull\n");
exit:
    close(fd);
    return res;
}

static int nativeJFmTx_GetRdsAfCode(JNIEnv *env, jobject obj, jlong jContextValue)
{
    return 0;
}

static int nativeJFmTx_SetMonoStereoMode(JNIEnv *env, jobject obj, jlong jContextValue,jint monoStereoMode)
{
return 0;
}

static int nativeJFmTx_GetMonoStereoMode(JNIEnv *env, jobject obj, jlong jContextValue)
{
return 0;

}

static int nativeJFmTx_SetPowerLevel(JNIEnv *env, jobject obj, jlong jContextValue,jint powerLevel)
{
	struct v4l2_ext_controls_kfmapp vec;
	struct v4l2_ext_control_kfmapp vctrls;
	int res;

	ALOGD("nativeJFmTx_SetPowerLevel(): Enter and power level = %d\n",powerLevel);

	vec.ctrl_class = V4L2_CTRL_CLASS_FM_TX;
	vec.count = 1;
	vctrls.id = V4L2_CID_TUNE_POWER_LEVEL;
	vctrls.value = 122 - powerLevel;
	vctrls.size = 0;
	vec.controls = &vctrls;

	res = ioctl(radio_fd, VIDIOC_S_EXT_CTRLS, &vec);
	if(res < 0)
	{
		ALOGE("Failed to set FM Tx power level\n");
		return res;
	}

	ALOGE("Setting FM Tx Power level to ---> %d\n", 122 - vctrls.value);

	return res;
}

static int nativeJFmTx_GetPowerLevel(JNIEnv *env, jobject obj, jlong jContextValue)
{
return 0;

}

static int nativeJFmTx_SetPreEmphasisFilter(JNIEnv *env, jobject obj, jlong jContextValue,jint preEmpFilter)
{
return 0;
}


static int nativeJFmTx_GetPreEmphasisFilter(JNIEnv *env, jobject obj, jlong jContextValue)
{
return 0;

}

static int nativeJFmTx_SetRdsPsScrollSpeed(JNIEnv *env, jobject obj, jlong jContextValue,jint scrollSpeed)
{
return 0;

}

static int nativeJFmTx_GetRdsPsScrollSpeed(JNIEnv *env, jobject obj, jlong jContextValue)
{
return 0;

}
//################################################################################

//                                 SIGNALS

//###############################################################################

    void nativeJFmTx_Callback(long context, int status,
                        int command, long value)
    {

        V4L2_JBTL_LOGI("nativeJFmTx_Callback: Entered, ");

        JNIEnv* env = NULL;
        bool attachedThread = false;
        int jRet ;

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


        switch (command) {

        case FM_TX_CMD_ENABLE:
            V4L2_JBTL_LOGI("FM_TX_CMD_ENABLE:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdEnable,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_DISABLE:
            V4L2_JBTL_LOGI("FM_TX_CMD_DISABLE:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdDisable,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;
/*
        case FM_TX_CMD_SET_INTERRUPT_MASK:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_INTERRUPT_MASK:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetInterruptMask,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_GET_INTERRUPT_STATUS:
            V4L2_JBTL_LOGI("FM_TX_CMD_DISABLE:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetInterruptMask,
                                     (jlong)context,
                                     (jint)status,
                                     (jint)value);
            break;

            */
        case FM_TX_CMD_START_TRANSMISSION:
            V4L2_JBTL_LOGI("FM_TX_CMD_START_TRANSMISSION:Status: %d ",status);
//            lptUnavailResources = (jclass *)event->p.cmdDone.v.audioOperation.ptUnavailResources;
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdStartTransmission,
                                      (jlong)context,
                                      (jint)status);
            break;

        case FM_TX_CMD_STOP_TRANSMISSION:
            V4L2_JBTL_LOGI("FM_TX_CMD_STOP_TRANSMISSION:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdStopTransmission,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_TUNE:
            V4L2_JBTL_LOGI("FM_TX_CMD_TUNE:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdTune,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_GET_TUNED_FREQUENCY:
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_TUNED_FREQUENCY:Status: %d,Value: %d ",status,value);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetTunedFrequency,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;


        case FM_TX_CMD_SET_MONO_STEREO_MODE:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_MONO_STEREO_MODE:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetMonoStereoMode,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_GET_MONO_STEREO_MODE:
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_MONO_STEREO_MODE:Status: %d,Value: %d ",status,value);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetMonoStereoMode,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

/*
        case FM_TX_CMD_SET_POWER_LEVEL:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_POWER_LEVEL:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetPowerLevel,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_GET_POWER_LEVEL:
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_POWER_LEVEL:Status: %d,Value: %d ",status,value);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetPowerLevel,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;
*/
        case FM_TX_CMD_SET_MUTE_MODE:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_MUTE_MODE:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetMuteMode,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_GET_MUTE_MODE:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_MUTE_MODE:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetMuteMode,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_ENABLE_RDS:
            V4L2_JBTL_LOGI("FM_TX_CMD_ENABLE_RDS:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdEnableRds,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_DISABLE_RDS:
            V4L2_JBTL_LOGI("FM_TX_CMD_DISABLE_RDS:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdDisableRds,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_SET_RDS_TEXT_RT_MSG:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_RDS_TEXT_RT_MSG:Status: %d ",status);

	    env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetRdsTextRtMsg,
			    (jlong)context,
			    (jint)status,
			    (jint)value);

        break;

        case FM_TX_CMD_SET_RDS_TEXT_PS_MSG:
	V4L2_JBTL_LOGI("FM_TX_CMD_SET_RDS_TEXT_PS_MSG:Status: %d ",status);

	env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetRdsTextPsMsg,
			(jlong)context,
			(jint)status,
			(jint)value);

	break;

        case FM_TX_CMD_SET_RDS_PTY_CODE:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_RDS_PI_CODE:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetRdsPtyCode,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

/*
        case FM_TX_CMD_SET_RDS_TRANSMISSION_MODE:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_RDS_TRANSMISSION_MODE:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetRdsTransmissionMode,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_GET_RDS_TRANSMISSION_MODE
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_POWER_LEVEL:Status: %d,Value: %d ",status,value);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetRdsTransmissionMode,
                                     (jlong)context,
                                     (jint)status,
                                     (jint)value);
            break;
*/
        case FM_TX_CMD_SET_RDS_AF_CODE:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_RDS_AF_CODE:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetRdsAfCode,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;
/*
        case FM_TX_CMD_GET_RDS_AF_CODE:
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_POWER_LEVEL:Status: %d,Value: %d ",status,value);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetRdsAfCode,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_SET_RDS_PI_CODE:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_RDS_PI_CODE:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetRdsPiCode,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_GET_RDS_PI_CODE:
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_RDS_PI_CODE:Status: %d,Value: %d ",status,value);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetRdsPiCode,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_GET_RDS_PTY_CODE:
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_RDS_PTY_CODE:Status: %d,Value: %d ",status,value);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetRdsPtyCode,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_SET_RDS_TEXT_REPERTOIRE:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_RDS_TEXT_REPERTOIRE:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetRdsTextRepertoire,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_GET_RDS_TEXT_REPERTOIRE:
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_RDS_TEXT_REPERTOIRE:Status: %d,Value: %d ",status,value);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetRdsTextRepertoire,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_SET_RDS_PS_DISPLAY_MODE:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_RDS_PS_DISPLAY_MODE:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetRdsPsDispalyMode,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_GET_RDS_PS_DISPLAY_MODE:
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_RDS_PS_DISPLAY_MODE:Status: %d,Value: %d ",status,value);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetRdsPsDispalyMode,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_SET_RDS_PS_DISPLAY_SPEED:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_RDS_PS_DISPLAY_SPEED:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetRdsPsDisplaySpeed,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_GET_RDS_PS_DISPLAY_SPEED:
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_RDS_PS_DISPLAY_SPEED:Status: %d,Value: %d ",status,value);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetRdsPsDisplaySpeed,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_GET_RDS_TEXT_PS_MSG:
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_RDS_TEXT_PS_MSG:Status: %d ",status);

            jRadioTextMsg = env->NewByteArray(event->p.cmdDone.v.psMsg.len);

            if (jRadioTextMsg == NULL)
            {
                V4L2_JBTL_LOGE("FM_TX_CMD_GET_RDS_TEXT_PS_MSG: Failed converting elements");
                goto EXCEPTION;
            }

            env->SetByteArrayRegion(jRadioTextMsg,0,event->p.cmdDone.v.psMsg.len,(jbyte*)event->p.cmdDone.v.psMsg.msg);

            if (env->ExceptionOccurred())    {
                V4L2_JBTL_LOGE("FM_TX_CMD_GET_RDS_TEXT_PS_MSG: env->SetByteArrayRegion failed");
                goto EXCEPTION;
            }

            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetRdsTextPsMsg,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)event->p.cmdDone.v.psMsg.len,
                                      jRadioTextMsg);
            break;

        case FM_TX_CMD_GET_RDS_TEXT_RT_MSG:
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_RDS_TEXT_RT_MSG:Status: %d ",status);

            jRadioTextMsg = env->NewByteArray(event->p.cmdDone.v.rtMsg.len);

            if (jRadioTextMsg == NULL)
            {
                V4L2_JBTL_LOGE("FM_TX_CMD_GET_RDS_TEXT_RT_MSG: Failed converting elements");
                goto EXCEPTION;
            }

            env->SetByteArrayRegion(jRadioTextMsg,0,event->p.cmdDone.v.rtMsg.len,(jbyte*)event->p.cmdDone.v.rtMsg.msg);

            if (env->ExceptionOccurred())    {
                V4L2_JBTL_LOGE("FM_TX_CMD_GET_RDS_TEXT_RT_MSG: env->SetByteArrayRegion failed");
                goto EXCEPTION;
            }

            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetRdsTextRtMsg,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)event->p.cmdDone.v.rtMsg.rtMsgType,
                                      (jint)event->p.cmdDone.v.rtMsg.len,
                                      jRadioTextMsg);
            break;

        case FM_TX_CMD_SET_RDS_TRANSMITTED_MASK:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_RDS_TRANSMITTED_MASK:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetRdsTransmittedMask,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_GET_RDS_TRANSMITTED_MASK:
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_RDS_TRANSMITTED_MASK:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetRdsTransmittedMask,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_SET_RDS_TRAFFIC_CODES:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_RDS_TRAFFIC_CODES:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetRdsTrafficCodes,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)event->p.cmdDone.v.trafficCodes.taCode,
                                      (jint)event->p.cmdDone.v.trafficCodes.tpCode);
            break;

        case FM_TX_CMD_GET_RDS_TRAFFIC_CODES:
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_RDS_TRAFFIC_CODES:Status: %d , taCode: %d ,tpCode: %d ",status,event->p.cmdDone.v.trafficCodes.taCode,event->p.cmdDone.v.trafficCodes.tpCode);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetRdsTrafficCodes,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)event->p.cmdDone.v.trafficCodes.taCode,
                                      (jint)event->p.cmdDone.v.trafficCodes.tpCode);
            break;

        case FM_TX_CMD_SET_RDS_MUSIC_SPEECH_FLAG:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_RDS_MUSIC_SPEECH_FLAG:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetRdsMusicSpeechFlag,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_GET_RDS_MUSIC_SPEECH_FLAG:
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_RDS_MUSIC_SPEECH_FLAG:Status: %d,Value: %d ",status,value);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetRdsMusicSpeechFlag,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_SET_PRE_EMPHASIS_FILTER:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_PRE_EMPHASIS_FILTER:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetPreEmphasisFilter,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_GET_PRE_EMPHASIS_FILTER:
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_PRE_EMPHASIS_FILTER:Status: %d,Value: %d ",status,value);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetPreEmphasisFilter,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_SET_RDS_EXTENDED_COUNTRY_CODE:
            V4L2_JBTL_LOGI("FM_TX_CMD_SET_RDS_EXTENDED_COUNTRY_CODE:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdSetRdsExtendedCountryCode,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_GET_RDS_EXTENDED_COUNTRY_CODE:
            V4L2_JBTL_LOGI("FM_TX_CMD_GET_PRE_EMPHASIS_FILTER:Status: %d,Value: %d ",status,value);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdGetRdsExtendedCountryCode,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        case FM_TX_CMD_WRITE_RDS_RAW_DATA:
            V4L2_JBTL_LOGI("FM_TX_CMD_WRITE_RDS_RAW_DATA:Status: %d ",status);
            jRadioTextMsg = env->NewByteArray(event->p.cmdDone.v.rawRds.len);

            if (jRadioTextMsg == NULL)
            {
                V4L2_JBTL_LOGE("FM_TX_CMD_WRITE_RDS_RAW_DATA: Failed converting elements");
                goto EXCEPTION;
            }

            env->SetByteArrayRegion(jRadioTextMsg,0,event->p.cmdDone.v.rawRds.len,(jbyte*)event->p.cmdDone.v.rawRds.data);

            if (env->ExceptionOccurred())    {
                V4L2_JBTL_LOGE("FM_TX_CMD_WRITE_RDS_RAW_DATA: env->SetByteArrayRegion failed");
                goto EXCEPTION;
            }

            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdWriteRdsRawData,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)event->p.cmdDone.v.rawRds.len,
                                      jRadioTextMsg);
            break;

        case FM_TX_CMD_READ_RDS_RAW_DATA:
            V4L2_JBTL_LOGI("FM_TX_CMD_READ_RDS_RAW_DATA:Status: %d ",status);
            jRadioTextMsg = env->NewByteArray(event->p.cmdDone.v.rawRds.len);

            if (jRadioTextMsg == NULL)
            {
                V4L2_JBTL_LOGE("FM_TX_CMD_READ_RDS_RAW_DATA: Failed converting elements");
                goto EXCEPTION;
            }

            env->SetByteArrayRegion(jRadioTextMsg,0,event->p.cmdDone.v.rawRds.len,(jbyte*)event->p.cmdDone.v.rawRds.data);

            if (env->ExceptionOccurred())    {
                V4L2_JBTL_LOGE("FM_TX_CMD_READ_RDS_RAW_DATA: env->SetByteArrayRegion failed");
                goto EXCEPTION;
            }

            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdReadRdsRawData,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)event->p.cmdDone.v.rawRds.len,
                                      jRadioTextMsg);
            break;

        case FM_TX_CMD_CHANGE_AUDIO_SOURCE:
            V4L2_JBTL_LOGI("FM_TX_CMD_CHANGE_AUDIO_SOURCE:Status: %d ",status);
            lptUnavailResources = (jclass *)event->p.cmdDone.v.audioOperation.ptUnavailResources;
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdChangeAudioSource,
                                      (jlong)context,
                                      (jint)status,
                                      (jclass)lptUnavailResources);
            break;
*/
        case FM_TX_CMD_CHANGE_DIGITAL_AUDIO_CONFIGURATION:
            V4L2_JBTL_LOGI("FM_TX_CMD_CHANGE_AUDIO_SOURCE:Status: %d ",status);
            env->CallStaticVoidMethod(_sJClass,_sMethodId_nativeCb_fmTxCmdChangeDigitalAudioConfiguration,
                                      (jlong)context,
                                      (jint)status,
                                      (jint)value);
            break;

        default:
            V4L2_JBTL_LOGE("nativeJFmTx_Callback():EVENT cmdType-------------->default");
//            V4L2_JBTL_LOGE("unhandled fm cmdType %d", event->p.cmdDone.cmdType);
            break;
        } //end switch

        if (env->ExceptionOccurred())    {
            V4L2_JBTL_LOGE("nativeJFmTx_Callback:  ExceptionOccurred");
            goto EXCEPTION;
        }

/*Delete the local references
        if (jRadioTextMsg!= NULL)
            env->DeleteLocalRef(jRadioTextMsg);
*/
        V4L2_JBTL_LOGD("nativeJFmTx_Callback: Exiting, Calling DetachCurrentThread at the END");

//        g_jVM->DetachCurrentThread();

        return;

EXCEPTION:
        V4L2_JBTL_LOGE("nativeJFmTx_Callback: Exiting due to failure");
/*        if (jRadioTextMsg!= NULL)
            env->DeleteLocalRef(jRadioTextMsg);
*/
        if (env->ExceptionOccurred())    {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }

        g_jVM->DetachCurrentThread();

        return;

    }

/**********************************************************************
*                Callback registration

***********************************************************************/
#define VERIFY_METHOD_ID(methodId) \
        if (!_VerifyMethodId(methodId, #methodId)) { \
            V4L2_JBTL_LOGE("Error obtaining method id for %s", #methodId);    \
            return;     \
        }

static bool _VerifyMethodId(jmethodID methodId, const char *name)
{
    bool result = true;

    if (methodId == NULL)
    {
        V4L2_JBTL_LOGE("_VerifyMethodId: Failed getting method id of %s", name);
        result = false;
    }

    return result;
}



void nativeJFmTx_ClassInitNative(JNIEnv* env, jclass clazz){
    V4L2_JBTL_LOGD("nativeJFmTx_ClassInitNative: Entered");


    if (NULL == env)
    {
        V4L2_JBTL_LOGE("nativeJFmRx_ClassInitNative: NULL == env");
    }

    env->GetJavaVM(&g_jVM);

    /* Save class information in global reference in order to prevent class unloading */
    _sJClass = (jclass)env->NewGlobalRef(clazz);

    /* Initialize structure of RBTL callbacks */
    V4L2_JBTL_LOGD("nativeJFmTx_ClassInitNative: Initializing FMTX callback structure");


    V4L2_JBTL_LOGD("nativeJFmTx_ClassInitNative: Obtaining method IDs");


    _sMethodId_nativeCb_fmTxCmdEnable = env->GetStaticMethodID(clazz,
                                        "nativeCb_fmTxCmdEnable",
                                        "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdEnable);


    _sMethodId_nativeCb_fmTxCmdDisable = env->GetStaticMethodID(clazz,
                                         "nativeCb_fmTxCmdDisable",
                                         "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdDisable);

    _sMethodId_nativeCb_fmTxCmdDestroy = env->GetStaticMethodID(clazz,
                                         "nativeCb_fmTxCmdDestroy",
                                         "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdDestroy);


    _sMethodId_nativeCb_fmTxCmdTune = env->GetStaticMethodID(clazz,
                                      "nativeCb_fmTxCmdTune",
                                      "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdTune);


    _sMethodId_nativeCb_fmTxCmdGetTunedFrequency= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetTunedFrequency",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetTunedFrequency);



    _sMethodId_nativeCb_fmTxCmdStartTransmission = env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdStartTransmission",
            "(JI)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdStartTransmission);


    _sMethodId_nativeCb_fmTxCmdStopTransmission = env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdStopTransmission",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdStopTransmission);


    _sMethodId_nativeCb_fmTxCmdEnableRds = env->GetStaticMethodID(clazz,
                                           "nativeCb_fmTxCmdEnableRds",
                                           "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdEnableRds);


    _sMethodId_nativeCb_fmTxCmdDisableRds = env->GetStaticMethodID(clazz,
                                            "nativeCb_fmTxCmdDisableRds",
                                            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdDisableRds);

    _sMethodId_nativeCb_fmTxCmdSetRdsTextRtMsg= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetRdsTextRtMsg",
            "(JIII[B)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetRdsTextRtMsg);


/*
    _sMethodId_nativeCb_fmTxCmdSetRdsTransmissionMode = env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetRdsTransmissionMode",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetRdsTransmissionMode);
    _sMethodId_nativeCb_fmTxCmdGetRdsTransmissionMode = env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetRdsTransmissionMode",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetRdsTransmissionMode);

    _sMethodId_nativeCb_fmTxCmdGetRdsTrafficCodes= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetRdsTrafficCodes",
            "(JIII)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetRdsTrafficCodes);


    _sMethodId_nativeCb_fmTxCmdSetRdsTrafficCodes= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetRdsTrafficCodes",
            "(JIII)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetRdsTrafficCodes);


*/
    _sMethodId_nativeCb_fmTxCmdSetRdsTextPsMsg= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetRdsTextPsMsg",
            "(JII[B)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetRdsTextPsMsg);

/*
    _sMethodId_nativeCb_fmTxCmdGetRdsTextPsMsg= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetRdsTextPsMsg",
            "(JII[B)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetRdsTextPsMsg);


    _sMethodId_nativeCb_fmTxCmdGetRdsTextRtMsg= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetRdsTextRtMsg",
            "(JIII[B)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetRdsTextRtMsg);


    _sMethodId_nativeCb_fmTxCmdWriteRdsRawData= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdWriteRdsRawData",
            "(JII[B)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdWriteRdsRawData);


    _sMethodId_nativeCb_fmTxCmdReadRdsRawData= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdReadRdsRawData",
            "(JII[B)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdReadRdsRawData);

    */

    _sMethodId_nativeCb_fmTxCmdSetInterruptMask= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetInterruptMask",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetInterruptMask);

    /*
    _sMethodId_nativeCb_fmTxCmdGetInterruptMask= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetInterruptMask",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetInterruptMask);
    */


    _sMethodId_nativeCb_fmTxCmdSetMonoStereoMode= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetMonoStereoMode",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetMonoStereoMode);


    _sMethodId_nativeCb_fmTxCmdGetMonoStereoMode= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetMonoStereoMode",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetMonoStereoMode);



    _sMethodId_nativeCb_fmTxCmdSetPowerLevel= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetPowerLevel",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetPowerLevel);



    _sMethodId_nativeCb_fmTxCmdGetPowerLevel= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetPowerLevel",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetPowerLevel);


    _sMethodId_nativeCb_fmTxCmdSetMuteMode= env->GetStaticMethodID(clazz,
                                            "nativeCb_fmTxCmdSetMuteMode",
                                            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetMuteMode);


    _sMethodId_nativeCb_fmTxCmdGetMuteMode= env->GetStaticMethodID(clazz,
                                            "nativeCb_fmTxCmdGetMuteMode",
                                            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetMuteMode);


    _sMethodId_nativeCb_fmTxCmdSetRdsAfCode= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetRdsAfCode",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetRdsAfCode);



    _sMethodId_nativeCb_fmTxCmdGetRdsAfCode= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetRdsAfCode",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetRdsAfCode);



    _sMethodId_nativeCb_fmTxCmdSetRdsPiCode= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetRdsPiCode",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetRdsPiCode);


    _sMethodId_nativeCb_fmTxCmdGetRdsPiCode= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetRdsPiCode",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetRdsPiCode);


    _sMethodId_nativeCb_fmTxCmdSetRdsPtyCode= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetRdsPtyCode",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetRdsPtyCode);


    _sMethodId_nativeCb_fmTxCmdGetRdsPtyCode= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetRdsPtyCode",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetRdsPtyCode);



    _sMethodId_nativeCb_fmTxCmdSetRdsPsDispalyMode= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetRdsPsDispalyMode",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetRdsPsDispalyMode);



    _sMethodId_nativeCb_fmTxCmdGetRdsPsDispalyMode= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetRdsPsDispalyMode",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetRdsPsDispalyMode);



    _sMethodId_nativeCb_fmTxCmdSetRdsPsDisplaySpeed= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetRdsPsDisplaySpeed",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetRdsPsDisplaySpeed);


    _sMethodId_nativeCb_fmTxCmdGetRdsPsDisplaySpeed= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetRdsPsDisplaySpeed",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetRdsPsDisplaySpeed);


    _sMethodId_nativeCb_fmTxCmdSetRdsTransmittedMask= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetRdsTransmittedMask",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetRdsTransmittedMask);


    _sMethodId_nativeCb_fmTxCmdGetRdsTransmittedMask= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetRdsTransmittedMask",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetRdsTransmittedMask);


    _sMethodId_nativeCb_fmTxCmdSetRdsMusicSpeechFlag = env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetRdsMusicSpeechFlag",
            "(JIJ)V") ;
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetRdsMusicSpeechFlag);


    _sMethodId_nativeCb_fmTxCmdGetRdsMusicSpeechFlag = env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetRdsMusicSpeechFlag",
            "(JIJ)V") ;
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetRdsMusicSpeechFlag);


    _sMethodId_nativeCb_fmTxCmdSetPreEmphasisFilter= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetPreEmphasisFilter",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetPreEmphasisFilter);


    _sMethodId_nativeCb_fmTxCmdGetPreEmphasisFilter= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetPreEmphasisFilter",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetPreEmphasisFilter);



    _sMethodId_nativeCb_fmTxCmdSetRdsExtendedCountryCode= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetRdsExtendedCountryCode",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetRdsExtendedCountryCode);


    _sMethodId_nativeCb_fmTxCmdGetRdsExtendedCountryCode= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetRdsExtendedCountryCode",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetRdsExtendedCountryCode);


    _sMethodId_nativeCb_fmTxCmdChangeDigitalAudioConfiguration= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdChangeDigitalAudioConfiguration",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdChangeDigitalAudioConfiguration);

/*
    _sMethodId_nativeCb_fmTxCmdChangeAudioSource= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdChangeAudioSource",
            "(JILcom/ti/jfm/core/JFmCcmVacUnavailResourceList;)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdChangeAudioSource);
*/
    _sMethodId_nativeCb_fmTxCmdSetRdsTextRepertoire= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdSetRdsTextRepertoire",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdSetRdsTextRepertoire);


    _sMethodId_nativeCb_fmTxCmdGetRdsTextRepertoire= env->GetStaticMethodID(clazz,
            "nativeCb_fmTxCmdGetRdsTextRepertoire",
            "(JIJ)V");
    VERIFY_METHOD_ID(_sMethodId_nativeCb_fmTxCmdGetRdsTextRepertoire);

    V4L2_JBTL_LOGD("nativeJFmTx_ClassInitNative:Exiting");
}

  JNINativeMethod JFmTxNative_sMethods[] = {
    /* name, signature, funcPtr */
    {"nativeJFmTx_ClassInitNative", "()V", (void*)nativeJFmTx_ClassInitNative},
    {"nativeJFmTx_Create","(Lcom/ti/jfm/core/JFmContext;)I", (void*)nativeJFmTx_Create},
    {"nativeJFmTx_Destroy","(J)I", (void*)nativeJFmTx_Destroy},
    {"nativeJFmTx_Enable","(J)I", (void*)nativeJFmTx_Enable},
    {"nativeJFmTx_Disable","(J)I", (void*)nativeJFmTx_Disable},
    {"nativeJFmTx_Tune","(JJ)I", (void*)nativeJFmTx_Tune},
    {"nativeJFmTx_StopTransmission","(J)I", (void*)nativeJFmTx_StopTransmission},
    {"nativeJFmTx_StartTransmission","(J)I", (void*)nativeJFmTx_StartTransmission},
    {"nativeJFmTx_EnableRds","(J)I", (void*)nativeJFmTx_EnableRds},
    {"nativeJFmTx_DisableRds","(J)I", (void*)nativeJFmTx_DisableRds},
    {"nativeJFmTx_SetRdsTransmissionMode","(JI)I", (void*)nativeJFmTx_SetRdsTransmissionMode},
    {"nativeJFmTx_SetRdsTextPsMsg","(JLjava/lang/String;I)I", (void*)nativeJFmTx_SetRdsTextPsMsg},
/*
    {"nativeJFmTx_GetRdsTextPsMsg","(J)I", (void*)nativeJFmTx_GetRdsTextPsMsg},
    {"nativeJFmTx_WriteRdsRawData","(JLjava/lang/String;I)I", (void*)nativeJFmTx_WriteRdsRawData},
*/
    {"nativeJFmTx_SetMuteMode","(JI)I", (void*)nativeJFmTx_SetMuteMode},
    {"nativeJFmTx_GetMuteMode","(J)I", (void*)nativeJFmTx_GetMuteMode},
    {"nativeJFmTx_SetRdsTextRtMsg","(JILjava/lang/String;I)I", (void*)nativeJFmTx_SetRdsTextRtMsg},
    {"nativeJFmTx_SetRdsTransmittedGroupsMask","(JJ)I", (void*)nativeJFmTx_SetRdsTransmittedGroupsMask},
/*
    {"nativeJFmTx_SetRdsPsDisplayMode","(JI)I", (void*)nativeJFmTx_SetRdsPsDisplayMode},
    {"nativeJFmTx_GetRdsPsDisplayMode","(J)I", (void*)nativeJFmTx_GetRdsPsDisplayMode},
    {"nativeJFmTx_GetRdsTextRtMsg","(J)I", (void*)nativeJFmTx_GetRdsTextRtMsg},
    {"nativeJFmTx_GetRdsTransmittedGroupsMask","(J)I", (void*)nativeJFmTx_GetRdsTransmittedGroupsMask},
    {"nativeJFmTx_SetRdsTrafficCodes","(JII)I", (void*)nativeJFmTx_SetRdsTrafficCodes},
    {"nativeJFmTx_GetRdsTrafficCodes","(J)I", (void*)nativeJFmTx_GetRdsTrafficCodes},
    {"nativeJFmTx_SetRdsMusicSpeechFlag","(JI)I", (void*)nativeJFmTx_SetRdsMusicSpeechFlag},
    {"nativeJFmTx_GetRdsMusicSpeechFlag","(J)I", (void*)nativeJFmTx_GetRdsMusicSpeechFlag},
    {"nativeJFmTx_SetRdsExtendedCountryCode","(JI)I", (void*)nativeJFmTx_SetRdsExtendedCountryCode},
    {"nativeJFmTx_GetRdsExtendedCountryCode","(J)I", (void*)nativeJFmTx_GetRdsExtendedCountryCode},
    {"nativeJFmTx_ReadRdsRawData","(J)I", (void*)nativeJFmTx_ReadRdsRawData},
    {"nativeJFmTx_ChangeAudioSource","(JII)I", (void*)nativeJFmTx_ChangeAudioSource},
*/
    {"nativeJFmTx_ChangeDigitalSourceConfiguration","(JI)I", (void*)nativeJFmTx_ChangeDigitalSourceConfiguration},
    {"nativeJFmTx_SetRdsTextRepertoire","(JI)I", (void*)nativeJFmTx_SetRdsTextRepertoire},
    {"nativeJFmTx_GetRdsTextRepertoire","(J)I", (void*)nativeJFmTx_GetRdsTextRepertoire},
    {"nativeJFmTx_SetRdsPtyCode","(JI)I", (void*)nativeJFmTx_SetRdsPtyCode},
    {"nativeJFmTx_GetRdsPtyCode","(J)I", (void*)nativeJFmTx_GetRdsPtyCode},
    {"nativeJFmTx_SetRdsPiCode","(JI)I", (void*)nativeJFmTx_SetRdsPiCode},
    {"nativeJFmTx_GetRdsPiCode","(J)I", (void*)nativeJFmTx_GetRdsPiCode},
    {"nativeJFmTx_SetRdsAfCode","(JI)I", (void*)nativeJFmTx_SetRdsAfCode},
    {"nativeJFmTx_GetRdsAfCode","(J)I", (void*)nativeJFmTx_GetRdsAfCode},
    {"nativeJFmTx_SetMonoStereoMode","(JI)I", (void*)nativeJFmTx_SetMonoStereoMode},
    {"nativeJFmTx_GetMonoStereoMode","(J)I", (void*)nativeJFmTx_GetMonoStereoMode},
    {"nativeJFmTx_SetPowerLevel","(JI)I", (void*)nativeJFmTx_SetPowerLevel},
    {"nativeJFmTx_GetPowerLevel","(J)I", (void*)nativeJFmTx_GetPowerLevel},
    {"nativeJFmTx_SetPreEmphasisFilter","(JI)I", (void*)nativeJFmTx_SetPreEmphasisFilter},
    {"nativeJFmTx_GetPreEmphasisFilter","(J)I", (void*)nativeJFmTx_GetPreEmphasisFilter},
    {"nativeJFmTx_SetRdsPsScrollSpeed","(JI)I", (void*)nativeJFmTx_SetRdsPsScrollSpeed},
    {"nativeJFmTx_GetRdsPsScrollSpeed","(J)I", (void*)nativeJFmTx_GetRdsPsScrollSpeed}

};

/*
 * Register several native methods for one class.
 */

int getTxNativeSize()
{
    return NELEM(JFmTxNative_sMethods);
}

