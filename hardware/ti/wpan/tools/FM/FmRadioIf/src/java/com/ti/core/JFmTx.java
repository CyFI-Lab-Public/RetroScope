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

package com.ti.jfm.core;

import java.util.HashMap; //import com.ti.jbtl.core.*;
import com.ti.jfm.core.*;

/** Class for providing connection with JFmTxNative.cpp module */

public class JFmTx {

    private static final String TAG = "JFmTx";

    private ICallback callback = null;

    private static JFmContext txContext;

    /** Hash table to store JFmContext handles based upon JFmTx. */
    private static HashMap<JFmContext, JFmTx> mTxContextsTable = new HashMap<JFmContext, JFmTx>();

    /** Events path */

static {
        try {
          //  JFmLog.i(TAG, "Loading libfmradio.so");
          //  System.loadLibrary("fmradio");
            nativeJFmTx_ClassInitNative();
        }
        catch (UnsatisfiedLinkError ule) {
            JFmLog.e(TAG, "WARNING: Could not load libfmradio.so");
        }
        catch (Exception e) {
            JFmLog.e("JFmRx", "Exception during NativeJFmRx_ClassInitNative ("
                    + e.toString() + ")");
        }
    }


    public interface ICallback {

       /* Fm TX call backs */
       void fmTxCmdEnable(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdDisable(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdDestroy(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdTune(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdGetTunedFrequency(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdStartTransmission(JFmTx context, JFmTxStatus status/*
                                                          * ,
                                                          * JFmCcmVacUnavailResourceList
                                                          * ccmVacUnavailResourceList
                                                          */);

       void fmTxCmdStopTransmission(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdSetPowerLevel(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdGetPowerLevel(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdEnableRds(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdDisableRds(JFmTx context, JFmTxStatus status, long value);

       // void fmTxCmdDone(JFmTx context, JFmTxStatus status, long value);
       void fmTxCmdSetRdsTransmissionMode(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdGetRdsTransmissionMode(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdSetMonoStereoMode(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdGetMonoStereoMode(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdSetPreEmphasisFilter(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdGetPreEmphasisFilter(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdSetMuteMode(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdGetMuteMode(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdSetRdsAfCode(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdGetRdsAfCode(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdSetRdsPiCode(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdGetRdsPiCode(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdSetRdsPtyCode(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdGetRdsPtyCode(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdSetRdsTextRepertoire(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdGetRdsTextRepertoire(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdSetRdsPsDisplayMode(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdGetRdsPsDisplayMode(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdSetRdsPsScrollSpeed(JFmTx context, JFmTxStatus status, long value);

       // void fmTxCmdGetRdsPsScrollSpeed(JFmTx context, JFmTxStatus status,
       // long value);
       void fmTxCmdSetRdsTextRtMsg(JFmTx context, JFmTxStatus status, int msgType, int msgLen,
             byte[] msg);

       void fmTxCmdGetRdsTextRtMsg(JFmTx context, JFmTxStatus status, int msgType, int msgLen,
             byte[] msg);

       void fmTxCmdSetRdsTransmittedMask(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdGetRdsTransmittedMask(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdSetRdsTextPsMsg(JFmTx context, JFmTxStatus status, int msgLen, byte[] msg);

       void fmTxCmdGetRdsTextPsMsg(JFmTx context, JFmTxStatus status, int msgLen, byte[] msg);

       void fmTxCmdSetRdsTrafficCodes(JFmTx context, JFmTxStatus status, int taCode, int tpCode);

       void fmTxCmdGetRdsTrafficCodes(JFmTx context, JFmTxStatus status, int taCode, int tpCode);

       void fmTxCmdSetRdsMusicSpeechFlag(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdGetRdsMusicSpeechFlag(JFmTx context, JFmTxStatus status, long value);

       // void fmTxCmdSetRdsECC(JFmTx context, JFmTxStatus status, long value);
       void fmTxCmdChangeAudioSource(JFmTx context, JFmTxStatus status,
             JFmCcmVacUnavailResourceList ccmVacUnavailResourceList);

       // void fmTxCmdChangeDigitalSourceConfiguration(JFmTx context,
       // JFmTxStatus status, JFmCcmVacUnavailResourceList
       // ccmVacUnavailResourceList);
       void fmTxCmdSetInterruptMask(JFmTx context, JFmTxStatus status, long value);

       void fmTxCmdWriteRdsRawData(JFmTx contextValue, JFmTxStatus status, int len, byte[] msg);

       void fmTxCmdReadRdsRawData(JFmTx contextValue, JFmTxStatus status, int len, byte[] msg);

       void fmTxCmdSetRdsPsDisplaySpeed(JFmTx contextValue, JFmTxStatus status, long value);

       void fmTxCmdGetRdsPsDisplaySpeed(JFmTx contextValue, JFmTxStatus status, long value);

       void fmTxCmdSetRdsExtendedCountryCode(JFmTx contextValue, JFmTxStatus status, long value);

       void fmTxCmdGetRdsExtendedCountryCode(JFmTx contextValue, JFmTxStatus status, long value);

       void fmTxCmdChangeDigitalAudioConfiguration(JFmTx contextValue, JFmTxStatus status,
             long value);

       void fmTxCmdSetRdsPsDispalyMode(JFmTx contextValue, JFmTxStatus status, long value);

       void fmTxCmdGetRdsPsDispalyMode(JFmTx contextValue, JFmTxStatus status, long value);

    }

    /**
    * Datatype Classes
    */

    public static class JFmTxRdsPiCode {
       private int value = 0;

       public JFmTxRdsPiCode(int val) {
          this.value = val;
       }

       public int getValue() {
          return value;
       }
    }

    public static enum JFmTxEcalResource implements IJFmEnum<Integer> {

       CAL_RESOURCE_I2SH(0x00), CAL_RESOURCE_PCMH(0x01), CAL_RESOURCE_PCMT_1(0x02), CAL_RESOURCE_PCMT_2(
             0x03), CAL_RESOURCE_PCMT_3(0x04), CAL_RESOURCE_PCMT_4(0x05), CAL_RESOURCE_PCMT_5(
             0x06), CAL_RESOURCE_PCMT_6(0x07), CAL_RESOURCE_FM_ANALOG(0x08), CAL_RESOURCE_LAST_EL_RESOURCE(
             0x08), CAL_RESOURCE_PCMIF(0x09), CAL_RESOURCE_FMIF(0x0A), CAL_RESOURCE_CORTEX(0x0B), CAL_RESOURCE_FM_CORE(
             0x0C), CAL_RESOURCE_MAX_NUM(0x0D), CAL_RESOURCE_INVALID(0x0E);

       private final int ecalResource;

       private JFmTxEcalResource(int ecalResource) {
          this.ecalResource = ecalResource;
       }

       public Integer getValue() {
          return ecalResource;
       }
    }

    public static enum JFmTxEcalOperation implements IJFmEnum<Integer> {

       CAL_OPERATION_FM_TX(0x00), CAL_OPERATION_FM_RX(0x01), CAL_OPERATION_A3DP(0x02), CAL_OPERATION_BT_VOICE(
             0x03), CAL_OPERATION_WBS(0x04), CAL_OPERATION_AWBS(0x05), CAL_OPERATION_FM_RX_OVER_SCO(
             0x06), CAL_OPERATION_FM_RX_OVER_A3DP(0x07), CAL_OPERATION_MAX_NUM(0x08), CAL_OPERATION_INVALID(
             0x09);

       private final int ecalOperation;

       private JFmTxEcalOperation(int ecalOperation) {
          this.ecalOperation = ecalOperation;
       }

       public Integer getValue() {
          return ecalOperation;
       }
    }

    public static enum JFmTxEcalSampleFrequency implements IJFmEnum<Integer> {
       CAL_SAMPLE_FREQ_8000(0x00), CAL_SAMPLE_FREQ_11025(0x01), CAL_SAMPLE_FREQ_12000(0x02), CAL_SAMPLE_FREQ_16000(
             0x03), CAL_SAMPLE_FREQ_22050(0x04), CAL_SAMPLE_FREQ_24000(0x05), CAL_SAMPLE_FREQ_32000(
             0x06), CAL_SAMPLE_FREQ_44100(0x07), CAL_SAMPLE_FREQ_48000(0x08);

       private final int ecalSampleFreq;

       private JFmTxEcalSampleFrequency(int ecalSampleFreq) {
          this.ecalSampleFreq = ecalSampleFreq;
       }

       public Integer getValue() {
          return ecalSampleFreq;
       }
    }

    public static enum JFmTxRdsSystem implements IJFmEnum<Integer> {
       FM_RDS_SYSTEM_RDS(0x00), FM_RDS_SYSTEM_RBDS(0x01);

       private final int rdsSystem;

       private JFmTxRdsSystem(int rdsSystem) {
          this.rdsSystem = rdsSystem;
       }

       public Integer getValue() {
          return rdsSystem;
       }
    }

    public static enum JFmTxAudioRouteMask implements IJFmEnum<Integer> {

       FMC_AUDIO_ROUTE_MASK_I2S(0x00000001), FMC_AUDIO_ROUTE_MASK_ANALOG(0x00000002), FMC_AUDIO_ROUTE_MASK_NONE(
             0x00000000), FMC_AUDIO_ROUTE_MASK_ALL(0x00000003);

       private final int audioRouteMask;

       private JFmTxAudioRouteMask(int audioRouteMask) {
          this.audioRouteMask = audioRouteMask;
       }

       public Integer getValue() {
          return audioRouteMask;
       }
    }

    public static class JFmTxPowerLevel {

       private int jFmTxPowerLevel;

       public JFmTxPowerLevel(int jFmTxPowerLevel) {
          this.jFmTxPowerLevel = jFmTxPowerLevel;
       }

       public int getvalue() {
          return jFmTxPowerLevel;
       }

       public void setvalue(int jFmTxPowerLevel) {
          this.jFmTxPowerLevel = jFmTxPowerLevel;
       }
    }

    public static class JFmTxFreq {

       private long value = 0;

       public JFmTxFreq(long value) {
          this.value = value;
       }

       public long getValue() {
          return value;
       }

       public void setValue(long value) {
          this.value = value;
       }
    }

    public static enum JFmTxMuteMode implements IJFmEnum<Integer> {
       FMC_MUTE(0x00), FMC_NOT_MUTE(0x01), FMC_ATTENUATE(0x02);

       private final int muteMode;

       private JFmTxMuteMode(int muteMode) {
          this.muteMode = muteMode;
       }

       public Integer getValue() {
          return muteMode;
       }
    }

    /*
    * public static enum JFmTxAudioTargetMask implements IJFmEnum<Integer> {
    * FM_RX_TARGET_MASK_INVALID(0), FM_RX_TARGET_MASK_I2SH(1),
    * FM_RX_TARGET_MASK_PCMH(2), FM_RX_TARGET_MASK_FM_OVER_SCO(4),
    * FM_RX_TARGET_MASK_FM_OVER_A3DP(8), FM_RX_TARGET_MASK_FM_ANALOG(16);
    * private final int audioTargetMask; private JFmTxAudioTargetMask(int
    * audioTargetMask) { this.audioTargetMask = audioTargetMask; } public
    * Integer getValue() { return audioTargetMask; } } public static enum
    * JFmTxRfDependentMuteMode implements IJFmEnum<Integer> {
    * FM_RX_RF_DEPENDENT_MUTE_ON(0x01), FM_RX_RF_DEPENDENT_MUTE_OFF(0x00);
    * private final int rfDependentMuteMode; private
    * JFmTxRfDependentMuteMode(int rfDependentMuteMode) {
    * this.rfDependentMuteMode = rfDependentMuteMode; } public Integer
    * getValue() { return rfDependentMuteMode; } }
    */

    public static enum JFmTxMonoStereoMode implements IJFmEnum<Integer> {
       FM_TX_MONO(0x00), FM_TX_STEREO(0x01);

       private final int monoStereoModer;

       private JFmTxMonoStereoMode(int monoStereoModer) {
          this.monoStereoModer = monoStereoModer;
       }

       public Integer getValue() {
          return monoStereoModer;
       }
    }

    public static enum JFmTxEmphasisFilter implements IJFmEnum<Integer> {
       FM_RX_EMPHASIS_FILTER_NONE(0x00), FM_RX_EMPHASIS_FILTER_50_USEC(0x01), FM_RX_EMPHASIS_FILTER_75_USEC(
             0x02);

       private final int emphasisFilter;

       private JFmTxEmphasisFilter(int emphasisFilter) {
          this.emphasisFilter = emphasisFilter;
       }

       public Integer getValue() {
          return emphasisFilter;
       }
    }

    public static enum JFmTxRepertoire implements IJFmEnum<Integer> {
       FMC_RDS_REPERTOIRE_G0_CODE_TABLE(0x00), FMC_RDS_REPERTOIRE_G1_CODE_TABLE(0x01), FMC_RDS_REPERTOIRE_G2_CODE_TABLE(
             0x02);

       private final int repertoire;

       private JFmTxRepertoire(int repertoire) {
          this.repertoire = repertoire;
       }

       public Integer getValue() {
          return repertoire;
       }
    }

    public static enum JFmTxRdsTransmissionMode implements IJFmEnum<Integer> {
       RDS_TRANSMISSION_MANUAL(0x00), RDS_TRANSMISSION_AUTOMATIC(0x01);

       private final int rdsTransmissionMode;

       private JFmTxRdsTransmissionMode(int rdsTransmissionMode) {
          this.rdsTransmissionMode = rdsTransmissionMode;
       }

       public Integer getValue() {
          return rdsTransmissionMode;
       }
    }

    public static class JFmTxRdsPtyCode {

       private int rdsPtyCode = 0;

       public JFmTxRdsPtyCode(int rdsPtyCode) {
          this.rdsPtyCode = rdsPtyCode;
       }

       public int getValue() {
          return rdsPtyCode;
       }

       public void setValue(int rdsPtyCode) {
          this.rdsPtyCode = rdsPtyCode;
       }
    }

    public static enum JFmMusicSpeechFlag implements IJFmEnum<Integer> {
       FMC_RDS_SPEECH(0x00), FMC_RDS_MUSIC(0x01);

       private final int musicSpeechFlag;

       private JFmMusicSpeechFlag(int musicSpeechFlag) {
          this.musicSpeechFlag = musicSpeechFlag;
       }

       public Integer getValue() {
          return musicSpeechFlag;
       }
    }

    public static enum JFmTaCode implements IJFmEnum<Integer> {
       FMC_RDS_TA_OFF(0x00), FMC_RDS_TA_ON(0x01);

       private final int taCode;

       private JFmTaCode(int taCode) {
          this.taCode = taCode;
       }

       public Integer getValue() {
          return taCode;
       }
    }

    public static enum JFmTpCode implements IJFmEnum<Integer> {
       FMC_RDS_TP_OFF(0x00), FMC_RDS_TP_ON(0x01);

       private final int tpCode;

       private JFmTpCode(int tpCode) {
          this.tpCode = tpCode;
       }

       public Integer getValue() {
          return tpCode;
       }
    }

    public static enum JFmRdsRtMsgType implements IJFmEnum<Integer> {
       FMC_RDS_TEXT_TYPE_PS(0x01), FMC_RDS_TEXT_TYPE_AUTO(0x02), FMC_RDS_TEXT_TYPE_A(0x03), FMC_RDS_TEXT_TYPE_B(
             0x04);

       private final int msgType;

       private JFmRdsRtMsgType(int msgType) {
          this.msgType = msgType;
       }

       public Integer getValue() {
          return msgType;
       }
    }

    public static enum JFmRdsPsDisplayMode implements IJFmEnum<Integer> {
       FMC_RDS_PS_DISPLAY_MODE_STATIC(0x00), FMC_RDS_PS_DISPLAY_MODE_SCROLL(0x01);

       private final int displayMode;

       private JFmRdsPsDisplayMode(int displayMode) {
          this.displayMode = displayMode;
       }

       public Integer getValue() {
          return displayMode;
       }
    }

    /*******************************************************************************
    * Class Methods
    *******************************************************************************/

    /* Fm Tx */

    public JFmTxStatus txCreate(ICallback callback) {
       JFmTxStatus jFmTxStatus;

       JFmLog.i(TAG, " nativeJFmTx_create()-Entered ");

       try {

          txContext = new JFmContext();

          JFmLog.d(TAG, "Calling nativeJFmTx_Create");
          int fmStatus = nativeJFmTx_Create(txContext);
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, fmStatus);
          JFmLog.d(TAG, "After nativeJFmTx_Create, status = " + jFmTxStatus.toString()
                + ", txContext = " + txContext.getValue());

          /**
           *Record the caller's callback and returned native context for
           * nativeJFmTx_create
           */

          this.callback = callback;
          mTxContextsTable.put(txContext, this);

       } catch (Exception e) {
          JFmLog.e(TAG, "create: exception during nativeJFmTx_create (" + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }

       return jFmTxStatus;
    }

    public JFmTxStatus txDestroy() {
       JFmLog.i(TAG, "txDestroy: entered");
       JFmTxStatus jFmTxStatus;

       try {
          int fmStatus = nativeJFmTx_Destroy(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, fmStatus);
          JFmLog.d(TAG, "After nativeJFmTx_Destroy, status = " + jFmTxStatus.toString());

          /*
           * Remove a pair of JFmContext-JFmTx related to the destroyed
           * context from the HashMap
           */

          mTxContextsTable.remove(txContext);

       } catch (Exception e) {
          JFmLog.e(TAG, "txDestroy: exception during nativeJFmTx_Destroy (" + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }

       JFmLog.d(TAG, "txDestroy: exiting");

       return jFmTxStatus;

    }

    public JFmTxStatus txEnable() {
       JFmLog.d(TAG, "txEnable: entered");
       JFmTxStatus jFmTxStatus;

       try {
          int fmStatus = nativeJFmTx_Enable(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, fmStatus);
          JFmLog.d(TAG, "After nativeJFmTx_Enable, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txEnable: exception during nativeJFmTx_Enable (" + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txEnable: exiting");

       return jFmTxStatus;

    }

    public JFmTxStatus txDisable() {
       JFmLog.d(TAG, "txDisable: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_Disable(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_Disable, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txDisable: exception during nativeJFmTx_Disable (" + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txDisable: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txTune(JFmTxFreq jfmTxFreq) {

       JFmLog.d(TAG, "txTune: entered");

       JFmTxStatus jFmTxStatus;

       try {

          int status = nativeJFmTx_Tune(txContext.getValue(), jfmTxFreq.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_Tune, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txTune: exception during nativeJFmTx_Tune (" + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txTune: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetTunedFrequency() {

       JFmLog.d(TAG, "txGetTunedFrequency: entered");

       JFmTxStatus jFmTxStatus;

       try {

          int status = nativeJFmTx_GetTunedFrequency(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_Tune, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txGetTunedFrequency: exception during nativeJFmTx_Tune (" + e.toString()
                + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetTunedFrequency: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txStartTransmission() {
       JFmLog.d(TAG, "txStartTransmission: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_StartTransmission(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog
                .d(TAG, "After nativeJFmTx_StartTransmission, status = "
                       + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "disable: exception during nativeJFmTx_StartTransmission ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txStartTransmission: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txStopTransmission() {
       JFmLog.d(TAG, "txStopTransmission: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_StopTransmission(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_StopTransmission, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txStopTransmission: exception during nativeJFmTx_StopTransmission ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txStopTransmission: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetPowerLevel(JFmTxPowerLevel jFmTxPowerLevel) {
       JFmLog.d(TAG, "txSetPowerLevel: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_SetPowerLevel(txContext.getValue(), jFmTxPowerLevel.getvalue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmnativeJFmTx_SetPowerLevel, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txSetPowerLevel: exception during nativeJFmTx_SetPowerLevel ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetPowerLevel: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetPowerLevel() {
       JFmLog.d(TAG, "txGetPowerLevel: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_GetPowerLevel(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmnativeJFmTx_GetPowerLevel, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txGetPowerLevel: exception during nativeJFmnativeJFmTx_GetPowerLevel ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetPowerLevel: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txEnableRds() {
       JFmLog.d(TAG, "txEnableRds: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_EnableRds(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_EnableRds, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txEnableRds: exception during nativeJFmTx_EnableRds (" + e.toString()
                + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txEnableRds: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txDisableRds() {
       JFmLog.d(TAG, "txDisableRds: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_DisableRds(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_DisableRds, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txDisableRds: exception during nativeJFmTx_DisableRds (" + e.toString()
                + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txDisableRds: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetRdsTransmissionMode(JFmTxRdsTransmissionMode mode) {
       JFmLog.d(TAG, "txSetRdsTransmissionMode: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_SetRdsTransmissionMode(txContext.getValue(), mode.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_SetRdsTransmissionMode, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG,
                "txSetRdsTransmissionMode: exception during nativeJFmTx_SetRdsTransmissionMode ("
                       + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetRdsTransmissionMode: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetRdsTransmissionMode() {
       JFmLog.d(TAG, "txGetRdsTransmissionMode: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_GetRdsTransmissionMode(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_GetRdsTransmissionMode, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG,
                "txGetRdsTransmissionMode: exception during nativeJFmTx_GetRdsTransmissionMode ("
                       + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetRdsTransmissionMode: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetRdsTextPsMsg(String psString, int length) {
       JFmLog.d(TAG, "txSetRdsTextPsMsg: entered");
       JFmLog.d(TAG, "txSetRdsTextPsMsg psString => " + psString + " length = " + length);
       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_SetRdsTextPsMsg(txContext.getValue(), psString, length);
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_SetRdsTextPsMsg, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txSetRdsTextPsMsg: exception during nativeJFmTx_SetRdsTextPsMsg ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetRdsTextPsMsg: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetRdsTextPsMsg() {
       JFmLog.d(TAG, "txGetRdsTextPsMsg: entered");
       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_GetRdsTextPsMsg(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_GetRdsTextPsMsg, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txSetRdsTextPsMsg: exception during nativeJFmTx_GetRdsTextPsMsg ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetRdsTextPsMsg: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txWriteRdsRawData(String msg, int length) {
       JFmLog.d(TAG, "txWriteRdsRawData: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_WriteRdsRawData(txContext.getValue(), msg, length);
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_WriteRdsRawData, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txWriteRdsRawData: exception during nativeJFmTx_WriteRdsRawData ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txWriteRdsRawData: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txChangeDigitalSourceConfiguration(int ecalSampleFreq) {
       JFmLog.d(TAG, "txChangeDigitalSourceConfiguration: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_ChangeDigitalSourceConfiguration(txContext.getValue(),
                ecalSampleFreq);
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_ChangeDigitalSourceConfiguration, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog
                .e(
                       TAG,
                       "txChangeDigitalSourceConfiguration: exception during nativeJFmTx_ChangeDigitalSourceConfiguration ("
                             + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txChangeDigitalSourceConfiguration: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txChangeAudioSource(JFmTxEcalResource audioSrc,
          JFmTxEcalSampleFrequency ecalSampleFreq) {
       JFmLog.d(TAG, "txChangeAudioSource: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_ChangeAudioSource(txContext.getValue(), audioSrc.getValue(),
                ecalSampleFreq.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog
                .d(TAG, "After nativeJFmTx_ChangeAudioSource, status = "
                       + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txChangeAudioSource: exception during nativeJFmTx_ChangeAudioSource ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txChangeAudioSource: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetRdsECC(int ecc) {
       JFmLog.d(TAG, "txSetRdsECC: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_SetRdsExtendedCountryCode(txContext.getValue(), ecc);
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_SetRdsECC, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txSetRdsECC: exception during nativeJFmTx_SetRdsECC (" + e.toString()
                + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetRdsECC: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetRdsECC() {
       JFmLog.d(TAG, "txGetRdsECC: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_GetRdsExtendedCountryCode(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_GetRdsECC, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txGetRdsECC: exception during nativeJFmTx_GetRdsECC (" + e.toString()
                + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetRdsECC: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetMonoStereoMode(JFmTxMonoStereoMode mode) {
       JFmLog.d(TAG, "txSetMonoStereoMode: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_SetMonoStereoMode(txContext.getValue(), mode.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog
                .d(TAG, "After nativeJFmTx_SetMonoStereoMode, status = "
                       + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txSetMonoStereoMode: exception during nativeJFmTx_SetMonoStereoMode ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetMonoStereoMode: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetMonoStereoMode() {
       JFmLog.d(TAG, "txGetMonoStereoMode: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_GetMonoStereoMode(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog
                .d(TAG, "After nativeJFmTx_GetMonoStereoMode, status = "
                       + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txSetRdsECC: exception during nativeJFmTx_GetMonoStereoMode ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetMonoStereoMode: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetPreEmphasisFilter(JFmTxEmphasisFilter preEmpFilter) {
       JFmLog.d(TAG, "txSetPreEmphasisFilter: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_SetPreEmphasisFilter(txContext.getValue(), preEmpFilter
                .getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_SetPreEmphasisFilter, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG,
                "txSetPreEmphasisFilter: exception during nativeJFmTx_SetPreEmphasisFilter ("
                       + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetPreEmphasisFilter: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetPreEmphasisFilter() {
       JFmLog.d(TAG, "txGetPreEmphasisFilter: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_GetPreEmphasisFilter(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_GetPreEmphasisFilter, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG,
                "txSetPreEmphasisFilter: exception during nativeJFmTx_GetPreEmphasisFilter ("
                       + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetPreEmphasisFilter: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetMuteMode(JFmTxMuteMode muteMode) {
       JFmLog.d(TAG, "txSetMuteMode: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_SetMuteMode(txContext.getValue(), muteMode.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_SetMuteMode, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txSetMuteMode: exception during nativeJFmTx_SetMuteMode ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetMuteMode: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetMuteMode() {
       JFmLog.d(TAG, "txGetMuteMode: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_GetMuteMode(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_GetMuteMode, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txGetMuteMode: exception during nativeJFmTx_GetMuteMode ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetMuteMode: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetRdsAfCode(int afCode) {
       JFmLog.d(TAG, "txSetRdsAfCode: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_SetRdsAfCode(txContext.getValue(), afCode);
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_SetRdsAfCode, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txSetRdsAfCode: exception during nativeJFmTx_SetRdsAfCode ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetRdsAfCode: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetRdsAfCode() {
       JFmLog.d(TAG, "txGetRdsAfCode: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_GetRdsAfCode(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_GetRdsAfCode, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txGetRdsAfCode: exception during nativeJFmTx_GetRdsAfCode ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetRdsAfCode: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetRdsPiCode(JFmTxRdsPiCode piCode) {
       JFmLog.d(TAG, "txSetRdsPiCode: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_SetRdsPiCode(txContext.getValue(), piCode.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_SetRdsPiCode, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txSetRdsPiCode: exception during nativeJFmTx_SetRdsPiCode ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetRdsPiCode: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetRdsPiCode() {
       JFmLog.d(TAG, "txGetRdsPiCode: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_GetRdsPiCode(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_SetRdsPiCode, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txGetRdsPiCode: exception during nativeJFmTx_SetRdsPiCode ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetRdsPiCode: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetRdsPtyCode(JFmTxRdsPtyCode ptyCode) {
       JFmLog.d(TAG, "txSetRdsPtyCode: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_SetRdsPtyCode(txContext.getValue(), ptyCode.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_SetRdsPtyCode, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txSetRdsPtyCode: exception during nativeJFmTx_SetRdsPtyCode("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetRdsPtyCode: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetRdsPtyCode() {
       JFmLog.d(TAG, "txGetRdsPtyCode: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_GetRdsPtyCode(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_GetRdsPtyCode, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txGetRdsPtyCode: exception during nativeJFmTx_GetRdsPtyCode("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetRdsPtyCode: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetRdsTextRepertoire(JFmTxRepertoire repertoire) {
       JFmLog.d(TAG, "txSetRdsTextRepertoire: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_SetRdsTextRepertoire(txContext.getValue(), repertoire
                .getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_SetRdsTextRepertoire, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG,
                "txSetRdsTextRepertoire: exception during nativeJFmTx_SetRdsTextRepertoire ("
                       + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetRdsTextRepertoire: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetRdsTextRepertoire() {
       JFmLog.d(TAG, "txGetRdsTextRepertoire: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_GetRdsTextRepertoire(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_GetRdsTextRepertoire, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG,
                "txGetRdsTextRepertoire: exception during nativeJFmTx_GetRdsTextRepertoire ("
                       + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetRdsTextRepertoire: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetRdsPsDisplayMode(JFmRdsPsDisplayMode dispalyMode) {
       JFmLog.d(TAG, "txSetRdsPsDisplayMode: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_SetRdsPsDisplayMode(txContext.getValue(), dispalyMode
                .getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_SetRdsECC, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txSetRdsECC: exception during nativeJFmTx_SetRdsECC (" + e.toString()
                + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetRdsECC: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetRdsPsDisplayMode() {
       JFmLog.d(TAG, "txGetRdsPsDisplayMode: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_GetRdsPsDisplayMode(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_GetRdsPsDisplayMode, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txGetRdsECC: exception during nativeJFmTx_GetRdsPsDisplayMode ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetRdsECC: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetRdsPsScrollSpeed(int scrollSpeed) {
       JFmLog.d(TAG, "txSetRdsPsScrollSpeed: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_SetRdsPsScrollSpeed(txContext.getValue(), scrollSpeed);
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_SetRdsPsScrollSpeed, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG,
                "txSetRdsPsScrollSpeed: exception during nativeJFmTx_SetRdsPsScrollSpeed ("
                       + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetRdsPsScrollSpeed: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetRdsPsScrollSpeed() {
       JFmLog.d(TAG, "txGetRdsPsScrollSpeed: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_GetRdsPsScrollSpeed(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_GetRdsPsScrollSpeed, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG,
                "txGetRdsPsScrollSpeed: exception during nativeJFmTx_GetRdsPsScrollSpeed ("
                       + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetRdsPsScrollSpeed: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetRdsTextRtMsg(JFmRdsRtMsgType msgType, String msg, int msgLength) {
       JFmLog.d(TAG, "txSetRdsTextRtMsg: entered");
       JFmLog.d(TAG, "txSetRdsTextRtMsg msg = " + msg + " msgLength = " + msgLength);
       JFmTxStatus jFmTxStatus;
       // byte[] message = msg.getBytes();
       try {
          int status = nativeJFmTx_SetRdsTextRtMsg(txContext.getValue(), msgType.getValue(), msg,
                msgLength);
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_SetRdsTextRtMsg, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txSetRdsTextRtMsg: exception during nativeJFmTx_SetRdsECC ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetRdsTextRtMsg: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetRdsTextRtMsg() {
       JFmLog.d(TAG, "txGetRdsTextRtMsg: entered");

       JFmTxStatus jFmTxStatus;
       // byte[] message = msg.getBytes();
       try {
          int status = nativeJFmTx_GetRdsTextRtMsg(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_GetRdsTextRtMsg, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txGetRdsTextRtMsg: exception during nativeJFmTx_GetRdsECC ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetRdsTextRtMsg: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetRdsTransmittedGroupsMask(long rdsTrasmittedGrpMask) {
       JFmLog.d(TAG, "txSetRdsTransmittedGroupsMask: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_SetRdsTransmittedGroupsMask(txContext.getValue(),
                rdsTrasmittedGrpMask);
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_SetRdsTransmittedGroupsMask, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG,
                "txSetRdsTransmittedGroupsMask: exception during nativeJFmTx_SetRdsTransmittedGroupsMask ("
                       + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetRdsTransmittedGroupsMask: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetRdsTransmittedGroupsMask() {
       JFmLog.d(TAG, "txGetRdsTransmittedGroupsMask: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_GetRdsTransmittedGroupsMask(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_GetRdsTransmittedGroupsMask, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG,
                "txGetRdsTransmittedGroupsMask: exception during nativeJFmTx_getRdsTransmittedGroupsMask ("
                       + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetRdsTransmittedGroupsMask: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetRdsTrafficCodes(JFmTaCode taCode, JFmTpCode tpCode) {
       JFmLog.d(TAG, "txSetRdsTransmittedGroupsMask: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_SetRdsTrafficCodes(txContext.getValue(), taCode.getValue(),
                tpCode.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_SetRdsTrafficCodes, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG,
                "txSetRdsTransmittedGroupsMask: exception during nativeJFmTx_SetRdsTrafficCodes ("
                       + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetRdsTransmittedGroupsMask: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetRdsTrafficCodes() {
       JFmLog.d(TAG, "txGetRdsTrafficCodes: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_GetRdsTrafficCodes(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_GetRdsTrafficCodes, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txGetRdsTrafficCodes: exception during nativeJFmTx_GetRdsTrafficCodes ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetRdsTrafficCodes: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txSetRdsMusicSpeechFlag(JFmMusicSpeechFlag musicSpeechFlag) {
       JFmLog.d(TAG, "txSetRdsMusicSpeechFlag: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_SetRdsMusicSpeechFlag(txContext.getValue(), musicSpeechFlag
                .getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_SetRdsECC, status = " + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG, "txSetRdsMusicSpeechFlag: exception during nativeJFmTx_SetRdsECC ("
                + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txSetRdsMusicSpeechFlag: exiting");
       return jFmTxStatus;
    }

    public JFmTxStatus txGetRdsMusicSpeechFlag() {
       JFmLog.d(TAG, "txGetRdsMusicSpeechFlag: entered");

       JFmTxStatus jFmTxStatus;

       try {
          int status = nativeJFmTx_GetRdsMusicSpeechFlag(txContext.getValue());
          jFmTxStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);
          JFmLog.d(TAG, "After nativeJFmTx_GetRdsMusicSpeechFlag, status = "
                + jFmTxStatus.toString());
       } catch (Exception e) {
          JFmLog.e(TAG,
                "txGetRdsMusicSpeechFlag: exception during nativeJFmTx_GetRdsMusicSpeechFlag ("
                       + e.toString() + ")");
          jFmTxStatus = JFmTxStatus.FAILED;
       }
       JFmLog.d(TAG, "txGetRdsMusicSpeechFlag: exiting");
       return jFmTxStatus;
    }

    /*--------------------------------------------------------------------------
    *        NATIVE PART
    *------------------------------------------------------------------------*/

    /* RBTL Calls */

    /* FM TX */
    private static native void nativeJFmTx_ClassInitNative();

    private static native int nativeJFmTx_Create(JFmContext contextValue);

    private static native int nativeJFmTx_Enable(long contextValue);

    private static native int nativeJFmTx_Tune(long contextValue, long jFmTxFreq);

    private static native int nativeJFmTx_GetTunedFrequency(long contextValue);

    private static native int nativeJFmTx_StartTransmission(long contextValue);

    private static native int nativeJFmTx_Disable(long contextValue);

    private static native int nativeJFmTx_Destroy(long contextValue);

    private static native int nativeJFmTx_StopTransmission(long contextValue);

    private static native int nativeJFmTx_SetPowerLevel(long contextValue, int powerLevel);

    private static native int nativeJFmTx_GetPowerLevel(long contextValue);

    private static native int nativeJFmTx_EnableRds(long contextValue);

    private static native int nativeJFmTx_DisableRds(long contextValue);

    private static native int nativeJFmTx_SetRdsTransmissionMode(long contextValue, int mode);

    private static native int nativeJFmTx_GetRdsTransmissionMode(long contextValue);

    private static native int nativeJFmTx_SetRdsTextPsMsg(long contextValue,
          java.lang.String psString, int length);

    private static native int nativeJFmTx_GetRdsTextPsMsg(long contextValue);

    private static native int nativeJFmTx_WriteRdsRawData(long contextValue, java.lang.String msg,
          int length);

    private static native int nativeJFmTx_ChangeDigitalSourceConfiguration(long contextValue,
          int ecalSampleFreq);

    private static native int nativeJFmTx_ChangeAudioSource(long contextValue, int audioSrc,
          int ecalSampleFreq);

    private static native int nativeJFmTx_SetRdsExtendedCountryCode(long contextValue, int ecc);

    private static native int nativeJFmTx_GetRdsExtendedCountryCode(long contextValue);

    private static native int nativeJFmTx_SetRdsMusicSpeechFlag(long contextValue,
          int musicSpeechFlag);

    private static native int nativeJFmTx_GetRdsMusicSpeechFlag(long contextValue);

    private static native int nativeJFmTx_SetRdsTrafficCodes(long contextValue, int taCode,
          int tpCode);

    private static native int nativeJFmTx_GetRdsTrafficCodes(long contextValue);

    private static native int nativeJFmTx_SetRdsTransmittedGroupsMask(long contextValue, long taCode);

    private static native int nativeJFmTx_GetRdsTransmittedGroupsMask(long contextValue);

    private static native int nativeJFmTx_SetRdsTextRtMsg(long contextValue, int msgType,
          java.lang.String msg, int msgLength);

    private static native int nativeJFmTx_GetRdsTextRtMsg(long contextValue);

    private static native int nativeJFmTx_SetRdsPsScrollSpeed(long contextValue, int scrollSpeed);

    private static native int nativeJFmTx_GetRdsPsScrollSpeed(long contextValue);

    private static native int nativeJFmTx_SetMuteMode(long contextValue, int muteMode);

    private static native int nativeJFmTx_GetMuteMode(long contextValue);

    private static native int nativeJFmTx_SetRdsAfCode(long contextValue, int afCode);

    private static native int nativeJFmTx_GetRdsAfCode(long contextValue);

    private static native int nativeJFmTx_SetRdsPiCode(long contextValue, int piCode);

    private static native int nativeJFmTx_GetRdsPiCode(long contextValue);

    private static native int nativeJFmTx_SetRdsPtyCode(long contextValue, int ptyCode);

    private static native int nativeJFmTx_GetRdsPtyCode(long contextValue);

    private static native int nativeJFmTx_SetRdsPsDisplayMode(long contextValue, int displayMode);

    private static native int nativeJFmTx_GetRdsPsDisplayMode(long contextValue);

    private static native int nativeJFmTx_SetRdsTextRepertoire(long contextValue, int reprtoire);

    private static native int nativeJFmTx_GetRdsTextRepertoire(long contextValue);

    private static native int nativeJFmTx_SetPreEmphasisFilter(long contextValue, int preEmpFilter);

    private static native int nativeJFmTx_GetPreEmphasisFilter(long contextValue);

    private static native int nativeJFmTx_GetMonoStereoMode(long contextValue);

    private static native int nativeJFmTx_SetMonoStereoMode(long contextValue, int mode);

    private static native int nativeJFmTx_ReadRdsRawData(long contextValue);

    /*---------------------------------------------------------------------------
    * -------------------------------- NATIVE PART--------------------------------
    * ---------------------------------------------------------------------------
    */

    /*---------------------------------------------------------------------------
    * ---------------------------------------------- Callbacks from the-------------
    * JFmTxNative.cpp module -----------------------------------------------------
    */

    /********* FM TX ***********/

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdEnable(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdEnable: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdEnable: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdEnable: calling callback");

          callback.fmTxCmdEnable(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdDisable(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdDisable: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdDisable: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdDisable: calling callback");

          callback.fmTxCmdDisable(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdDestroy(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdDestroy: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdDestroy: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdDestroy: calling callback");

          callback.fmTxCmdDestroy(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdTune(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdTune: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdTune: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdTune: calling callback");

          callback.fmTxCmdTune(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetTunedFrequency(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetTunedFrequency: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetTunedFrequency: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetTunedFrequency: calling callback");

          callback.fmTxCmdGetTunedFrequency(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdStopTransmission(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdStopTransmission: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdStopTransmission: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdStopTransmission: calling callback");

          callback.fmTxCmdStopTransmission(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdStartTransmission(long contextValue, int status/*
                                                                      * ,
                                                                      * JFmCcmVacUnavailResourceList
                                                                      * ccmVacUnavailResourceList
                                                                      */) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdStartTransmission: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdStartTransmission: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdStartTransmission: calling callback");

          callback.fmTxCmdStartTransmission(mJFmTx, txStatus/*
                                                   * ,
                                                   * ccmVacUnavailResourceList
                                                   */);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdEnableRds(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdEnableRds: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdEnableRds: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdEnableRds: calling callback");

          callback.fmTxCmdEnableRds(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdDisableRds(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdDisableRds: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdDisableRds: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdDisableRds: calling callback");

          callback.fmTxCmdDisableRds(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetRdsTransmissionMode(long contextValue, int status,
          long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTransmissionMode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTransmissionMode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTransmissionMode: calling callback");

          callback.fmTxCmdSetRdsTransmissionMode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetRdsTransmissionMode(long contextValue, int status,
          long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTransmissionMode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTransmissionMode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTransmissionMode: calling callback");

          callback.fmTxCmdGetRdsTransmissionMode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetMonoStereoMode(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetMonoStereoMode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetMonoStereoMode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetMonoStereoMode: calling callback");

          callback.fmTxCmdSetMonoStereoMode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetMonoStereoMode(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetMonoStereoMode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetMonoStereoMode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetMonoStereoMode: calling callback");

          callback.fmTxCmdGetMonoStereoMode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetPreEmphasisFilter(long contextValue, int status,
          long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetPreEmphasisFilter: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetPreEmphasisFilter: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetPreEmphasisFilter: calling callback");

          callback.fmTxCmdSetPreEmphasisFilter(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetPreEmphasisFilter(long contextValue, int status,
          long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetPreEmphasisFilter: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetPreEmphasisFilter: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetPreEmphasisFilter: calling callback");

          callback.fmTxCmdGetPreEmphasisFilter(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetMuteMode(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetMuteMode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetMuteMode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetMuteMode: calling callback");

          callback.fmTxCmdSetMuteMode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetMuteMode(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetMuteMode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetMuteMode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetMuteMode: calling callback");

          callback.fmTxCmdGetMuteMode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetRdsAfCode(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsAfCode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsAfCode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsAfCode: calling callback");

          callback.fmTxCmdSetMuteMode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetRdsAfCode(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsAfCode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsAfCode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsAfCode: calling callback");

          callback.fmTxCmdGetRdsAfCode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetRdsPiCode(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsPiCode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsPiCode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsPiCode: calling callback");

          callback.fmTxCmdSetRdsPiCode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetRdsPiCode(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsPiCode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsPiCode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsPiCode: calling callback");

          callback.fmTxCmdGetRdsPiCode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetRdsPtyCode(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsPtyCode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsPtyCode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsPtyCode: calling callback");

          callback.fmTxCmdSetRdsPtyCode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetRdsPtyCode(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsPtyCode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsPtyCode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsPtyCode: calling callback");

          callback.fmTxCmdGetRdsPtyCode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetRdsTextRepertoire(long contextValue, int status,
          long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTextRepertoire: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTextRepertoire: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTextRepertoire: calling callback");

          callback.fmTxCmdSetRdsTextRepertoire(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetRdsTextRepertoire(long contextValue, int status,
          long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTextRepertoire: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTextRepertoire: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTextRepertoire: calling callback");

          callback.fmTxCmdGetRdsTextRepertoire(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetRdsPsDisplayMode(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsPsDisplayMode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsPsDisplayMode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsPsDisplayMode: calling callback");

          callback.fmTxCmdSetRdsPsDisplayMode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetRdsPsDisplayMode(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsPsDisplayMode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsPsDisplayMode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsPsDisplayMode: calling callback");

          callback.fmTxCmdGetRdsPsDisplayMode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetRdsPsDisplaySpeed(long contextValue, int status,
          long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsPsDisplaySpeed: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsPsDisplaySpeed: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsPsDisplaySpeed: calling callback");

          callback.fmTxCmdSetRdsPsDisplaySpeed(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetRdsPsDisplaySpeed(long contextValue, int status,
          long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsPsDisplaySpeed: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsPsDisplaySpeed: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsPsDisplaySpeed: calling callback");

          callback.fmTxCmdGetRdsPsDisplaySpeed(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetRdsTransmittedMask(long contextValue, int status,
          long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTransmittedMask: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTransmittedMask: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTransmittedMask: calling callback");

          callback.fmTxCmdSetRdsTransmittedMask(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetRdsTransmittedMask(long contextValue, int status,
          long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTransmittedMask: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTransmittedMask: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTransmittedMask: calling callback");

          callback.fmTxCmdGetRdsTransmittedMask(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetRdsMusicSpeechFlag(long contextValue, int status,
          long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsMusicSpeechFlag: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsMusicSpeechFlag: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsMusicSpeechFlag: calling callback");

          callback.fmTxCmdSetRdsMusicSpeechFlag(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetRdsMusicSpeechFlag(long contextValue, int status,
          long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsMusicSpeechFlag: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsMusicSpeechFlag: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsMusicSpeechFlag: calling callback");

          callback.fmTxCmdGetRdsMusicSpeechFlag(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdChangeAudioSource(long contextValue, int status,
          JFmCcmVacUnavailResourceList ccmVacUnavailResourceList) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdChangeAudioSource: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdChangeAudioSource: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdChangeAudioSource: calling callback");

          callback.fmTxCmdChangeAudioSource(mJFmTx, txStatus, ccmVacUnavailResourceList);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetRdsTrafficCodes(long contextValue, int status,
          int taCode, int tpCode) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTrafficCodes: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTrafficCodes: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTrafficCodes: calling callback");

          callback.fmTxCmdSetRdsTrafficCodes(mJFmTx, txStatus, taCode, tpCode);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetRdsTrafficCodes(long contextValue, int status,
          int taCode, int tpCode) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTrafficCodes: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTrafficCodes: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTrafficCodes: calling callback");

          callback.fmTxCmdGetRdsTrafficCodes(mJFmTx, txStatus, taCode, tpCode);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetRdsTextPsMsg(long contextValue, int status, int msgLen,
          byte[] msg) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTextPsMsg: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTextPsMsg: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTextPsMsg: calling callback");

          callback.fmTxCmdSetRdsTextPsMsg(mJFmTx, txStatus, msgLen, msg);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetRdsTextPsMsg(long contextValue, int status, int msgLen,
          byte[] msg) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTextPsMsg: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTextPsMsg: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTextPsMsg: calling callback");

          callback.fmTxCmdGetRdsTextPsMsg(mJFmTx, txStatus, msgLen, msg);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetRdsTextRtMsg(long contextValue, int status, int msgType,
          int msgLen, byte[] msg) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTextRtMsg: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTextRtMsg: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsTextRtMsg: calling callback");

          callback.fmTxCmdSetRdsTextRtMsg(mJFmTx, txStatus, msgType, msgLen, msg);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetRdsTextRtMsg(long contextValue, int status, int msgType,
          int msgLen, byte[] msg) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTextRtMsg: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTextRtMsg: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsTextRtMsg: calling callback");

          callback.fmTxCmdGetRdsTextRtMsg(mJFmTx, txStatus, msgType, msgLen, msg);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetInterruptMask(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetInterruptMask: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetInterruptMask: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetInterruptMask: calling callback");

          callback.fmTxCmdSetInterruptMask(mJFmTx, txStatus, value);

       }

    }

    /*
    * @SuppressWarnings("unused") public static void
    * nativeCb_fmTxCmdGetInterruptMask(long contextValue, int status, long
    * value) { JFmLog.d(TAG, "nativeCb_fmTxCmdGetInterruptMask: entered");
    * JFmTx mJFmTx = getJFmTx(contextValue); if (mJFmTx != null) { ICallback
    * callback = mJFmTx.callback; JFmLog.d(TAG,
    * "nativeCb_fmTxCmdGetInterruptMask: converting callback args");
    * JFmLog.d(TAG, "nativeCb_fmTxCmdGetInterruptMask: calling callback");
    * callback.fmTxCmdGetInterruptMask(mJFmTx, status, value); } }
    */
    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetRdsExtendedCountryCode(long contextValue, int status,
          long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsExtendedCountryCode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsExtendedCountryCode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsExtendedCountryCode: calling callback");

          callback.fmTxCmdSetRdsExtendedCountryCode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetRdsExtendedCountryCode(long contextValue, int status,
          long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsExtendedCountryCode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsExtendedCountryCode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsExtendedCountryCode: calling callback");

          callback.fmTxCmdGetRdsExtendedCountryCode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdChangeDigitalAudioConfiguration(long contextValue,
          int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdChangeDigitalAudioConfiguration: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG,
                "nativeCb_fmTxCmdChangeDigitalAudioConfiguration: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdChangeDigitalAudioConfiguration: calling callback");

          callback.fmTxCmdChangeDigitalAudioConfiguration(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdWriteRdsRawData(long contextValue, int status, int len,
          byte[] msg) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdWriteRdsRawData: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdWriteRdsRawData: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdWriteRdsRawData: calling callback");

          callback.fmTxCmdWriteRdsRawData(mJFmTx, txStatus, len, msg);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdReadRdsRawData(long contextValue, int status, int len,
          byte[] msg) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdReadRdsRawData: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdReadRdsRawData: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdReadRdsRawData: calling callback");

          callback.fmTxCmdReadRdsRawData(mJFmTx, txStatus, len, msg);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetPowerLevel(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetPowerLevel: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetPowerLevel: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetPowerLevel: calling callback");

          callback.fmTxCmdSetPowerLevel(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetPowerLevel(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetPowerLevel: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetPowerLevel: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetPowerLevel: calling callback");

          callback.fmTxCmdGetPowerLevel(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdSetRdsPsDispalyMode(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsPsDispalyMode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsPsDispalyMode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdSetRdsPsDispalyMode: calling callback");

          callback.fmTxCmdSetRdsPsDispalyMode(mJFmTx, txStatus, value);

       }

    }

    @SuppressWarnings("unused")
    public static void nativeCb_fmTxCmdGetRdsPsDispalyMode(long contextValue, int status, long value) {

       JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsPsDispalyMode: entered");

       JFmTx mJFmTx = getJFmTx(contextValue);

       if (mJFmTx != null) {

          ICallback callback = mJFmTx.callback;

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsPsDispalyMode: converting callback args");

          JFmTxStatus txStatus = JFmUtils.getEnumConst(JFmTxStatus.class, status);

          JFmLog.d(TAG, "nativeCb_fmTxCmdGetRdsPsDispalyMode: calling callback");

          callback.fmTxCmdGetRdsPsDispalyMode(mJFmTx, txStatus, value);

       }

    }

    /******************************************************************************************
    * Private utility functions
    *******************************************************************************************/

    private static JFmTx getJFmTx(long contextValue) {
       JFmTx jFmTx;

       JFmContext profileContext = new JFmContext(contextValue);

       if (mTxContextsTable.containsKey(profileContext)) {
          jFmTx = mTxContextsTable.get(profileContext);
       } else {
          JFmLog.e(TAG, "JFmTx: Failed mapping context " + contextValue + " to a callback");
          jFmTx = null;
       }

       return jFmTx;
    }

} /* End class */
