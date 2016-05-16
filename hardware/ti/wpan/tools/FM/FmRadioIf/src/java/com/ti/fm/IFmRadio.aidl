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

package com.ti.fm;

/**
 * System private API for FM Receiver service
 *
 * {@hide}
 */
interface IFmRadio {
    boolean rxEnable();
    boolean rxDisable();
    boolean rxIsEnabled() ;
    boolean rxIsFMPaused();
    int rxGetFMState();
     boolean resumeFm();
    boolean rxSetBand(int band);
    boolean rxSetBand_nb(int band);
    int rxGetBand();
    boolean rxGetBand_nb();
    boolean rxSetChannelSpacing(int channelSpace);
    boolean rxSetChannelSpacing_nb(int channelSpace);
    int rxGetChannelSpacing();
    boolean rxGetChannelSpacing_nb();
    boolean rxSetMonoStereoMode(int mode);
    boolean rxSetMonoStereoMode_nb(int mode);
    int rxGetMonoStereoMode();
    boolean  rxGetMonoStereoMode_nb();
    boolean rxSetMuteMode(int muteMode);
    boolean rxSetMuteMode_nb(int muteMode);
    int rxGetMuteMode();
     boolean rxGetMuteMode_nb();
    boolean rxSetRfDependentMuteMode(int rfMuteMode);
    boolean rxSetRfDependentMuteMode_nb(int rfMuteMode);
    int rxGetRfDependentMuteMode();
    boolean rxGetRfDependentMuteMode_nb();
    boolean rxSetRssiThreshold(int threshhold);
    boolean rxSetRssiThreshold_nb(int threshhold);
    int rxGetRssiThreshold();
    boolean rxGetRssiThreshold_nb();
    boolean rxSetDeEmphasisFilter(int filter);
    boolean rxSetDeEmphasisFilter_nb(int filter);
    int rxGetDeEmphasisFilter();
     boolean rxGetDeEmphasisFilter_nb();
    boolean rxSetVolume(int volume);
    int rxGetVolume();
     boolean rxGetVolume_nb();
    boolean rxTune_nb(int freq);
    int rxGetTunedFrequency();
    boolean rxGetTunedFrequency_nb();
    boolean rxSeek_nb(int direction);
    boolean rxStopSeek();
    boolean rxStopSeek_nb();
    int rxGetRssi();
    boolean rxGetRssi_nb();
    int rxGetRdsSystem();
    boolean rxGetRdsSystem_nb();
    boolean rxSetRdsSystem(int system);
    boolean rxSetRdsSystem_nb(int system);
    boolean rxEnableRds();
    boolean rxEnableRds_nb();
    boolean rxDisableRds();
    boolean rxDisableRds_nb();
    boolean rxSetRdsGroupMask(int mask);
    boolean rxSetRdsGroupMask_nb(int mask);
    long rxGetRdsGroupMask();
    boolean rxGetRdsGroupMask_nb();
    boolean rxSetRdsAfSwitchMode(int mode);
    boolean rxSetRdsAfSwitchMode_nb(int mode);
    int rxGetRdsAfSwitchMode();
    boolean rxGetRdsAfSwitchMode_nb();
    boolean rxChangeAudioTarget(int mask,int digitalConfig);
    boolean rxChangeDigitalTargetConfiguration(int digitalConfig);
    boolean rxEnableAudioRouting();
    boolean rxDisableAudioRouting();
    boolean rxIsValidChannel();
    boolean rxCompleteScan_nb();
    double rxGetFwVersion();
    int rxGetCompleteScanProgress();
    boolean rxGetCompleteScanProgress_nb();
    int rxStopCompleteScan();
    boolean rxStopCompleteScan_nb();


/*  FM TX */

    boolean txEnable();
    boolean txDisable();
    boolean txStartTransmission();
    boolean txStopTransmission();
    boolean txTune(long freq);

    boolean txSetPowerLevel(int powerLevel);

    boolean txEnableRds();
    boolean txDisableRds();
    boolean txSetRdsTransmissionMode(int mode);

    boolean txSetRdsTextPsMsg(String psString);

    boolean txWriteRdsRawData(String rawData);
    boolean txSetMonoStereoMode(int mode);

    boolean txSetPreEmphasisFilter(int preEmpFilter);

    boolean txSetMuteMode(int muteMode);

    boolean txSetRdsAfCode(int afCode);

    boolean txSetRdsPiCode(int piCode);

    boolean txSetRdsPtyCode(int ptyCode);

    boolean txSetRdsTextRepertoire(int repertoire);

    boolean txSetRdsPsDisplayMode(int dispalyMode);

    boolean txSetRdsPsScrollSpeed(int scrollSpeed);

    boolean txSetRdsTextRtMsg(int msgType, String msg, int msgLength);

    boolean txSetRdsTransmittedGroupsMask(long rdsTrasmittedGrpMask);

    boolean txSetRdsTrafficCodes(int taCode, int tpCode);

    boolean txSetRdsMusicSpeechFlag(int musicSpeechFlag);

    boolean txSetRdsECC(int ecc);

    boolean txChangeAudioSource(int audioSrc,int ecalSampleFreq);
    boolean txChangeDigitalSourceConfiguration(int ecalSampleFreq);
    int txGetFMState();

}
