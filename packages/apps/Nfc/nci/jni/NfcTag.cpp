/*
 * Copyright (C) 2012 The Android Open Source Project
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

/*
 *  Tag-reading, tag-writing operations.
 */
#include "OverrideLog.h"
#include "NfcTag.h"
#include "JavaClassConstants.h"
#include <ScopedLocalRef.h>
#include <ScopedPrimitiveArray.h>

extern "C"
{
    #include "rw_int.h"
}


/*******************************************************************************
**
** Function:        NfcTag
**
** Description:     Initialize member variables.
**
** Returns:         None
**
*******************************************************************************/
NfcTag::NfcTag ()
:   mNumTechList (0),
    mTechnologyTimeoutsTable (MAX_NUM_TECHNOLOGY),
    mNativeData (NULL),
    mIsActivated (false),
    mActivationState (Idle),
    mProtocol(NFC_PROTOCOL_UNKNOWN),
    mtT1tMaxMessageSize (0),
    mReadCompletedStatus (NFA_STATUS_OK),
    mLastKovioUidLen (0),
    mNdefDetectionTimedOut (false)
{
    memset (mTechList, 0, sizeof(mTechList));
    memset (mTechHandles, 0, sizeof(mTechHandles));
    memset (mTechLibNfcTypes, 0, sizeof(mTechLibNfcTypes));
    memset (mTechParams, 0, sizeof(mTechParams));
    memset(mLastKovioUid, 0, NFC_KOVIO_MAX_LEN);
}


/*******************************************************************************
**
** Function:        getInstance
**
** Description:     Get a reference to the singleton NfcTag object.
**
** Returns:         Reference to NfcTag object.
**
*******************************************************************************/
NfcTag& NfcTag::getInstance ()
{
    static NfcTag tag;
    return tag;
}


/*******************************************************************************
**
** Function:        initialize
**
** Description:     Reset member variables.
**                  native: Native data.
**
** Returns:         None
**
*******************************************************************************/
void NfcTag::initialize (nfc_jni_native_data* native)
{
    mNativeData = native;
    mIsActivated = false;
    mActivationState = Idle;
    mProtocol = NFC_PROTOCOL_UNKNOWN;
    mNumTechList = 0;
    mtT1tMaxMessageSize = 0;
    mReadCompletedStatus = NFA_STATUS_OK;
    resetTechnologies ();
}


/*******************************************************************************
**
** Function:        abort
**
** Description:     Unblock all operations.
**
** Returns:         None
**
*******************************************************************************/
void NfcTag::abort ()
{
    SyncEventGuard g (mReadCompleteEvent);
    mReadCompleteEvent.notifyOne ();
}


/*******************************************************************************
**
** Function:        getActivationState
**
** Description:     What is the current state: Idle, Sleep, or Activated.
**
** Returns:         Idle, Sleep, or Activated.
**
*******************************************************************************/
NfcTag::ActivationState NfcTag::getActivationState ()
{
    return mActivationState;
}


/*******************************************************************************
**
** Function:        setDeactivationState
**
** Description:     Set the current state: Idle or Sleep.
**                  deactivated: state of deactivation.
**
** Returns:         None.
**
*******************************************************************************/
void NfcTag::setDeactivationState (tNFA_DEACTIVATED& deactivated)
{
    static const char fn [] = "NfcTag::setDeactivationState";
    mActivationState = Idle;
    mNdefDetectionTimedOut = false;
    if (deactivated.type == NFA_DEACTIVATE_TYPE_SLEEP)
        mActivationState = Sleep;
    ALOGD ("%s: state=%u", fn, mActivationState);
}


/*******************************************************************************
**
** Function:        setActivationState
**
** Description:     Set the current state to Active.
**
** Returns:         None.
**
*******************************************************************************/
void NfcTag::setActivationState ()
{
    static const char fn [] = "NfcTag::setActivationState";
    mNdefDetectionTimedOut = false;
    mActivationState = Active;
    ALOGD ("%s: state=%u", fn, mActivationState);
}

/*******************************************************************************
**
** Function:        isActivated
**
** Description:     Is tag activated?
**
** Returns:         True if tag is activated.
**
*******************************************************************************/
bool NfcTag::isActivated ()
{
    return mIsActivated;
}


/*******************************************************************************
**
** Function:        getProtocol
**
** Description:     Get the protocol of the current tag.
**
** Returns:         Protocol number.
**
*******************************************************************************/
tNFC_PROTOCOL NfcTag::getProtocol()
{
    return mProtocol;
}

/*******************************************************************************
**
** Function         TimeDiff
**
** Description      Computes time difference in milliseconds.
**
** Returns          Time difference in milliseconds
**
*******************************************************************************/
UINT32 TimeDiff(timespec start, timespec end)
{
    timespec temp;
    if ((end.tv_nsec-start.tv_nsec)<0)
    {
        temp.tv_sec = end.tv_sec-start.tv_sec-1;
        temp.tv_nsec = 1000000000+end.tv_nsec-start.tv_nsec;
    }
    else
    {
        temp.tv_sec = end.tv_sec-start.tv_sec;
        temp.tv_nsec = end.tv_nsec-start.tv_nsec;
    }

    return (temp.tv_sec * 1000) + (temp.tv_nsec / 1000000);
}

/*******************************************************************************
**
** Function:        IsSameKovio
**
** Description:     Checks if tag activate is the same (UID) Kovio tag previously
**                  activated.  This is needed due to a problem with some Kovio
**                  tags re-activating multiple times.
**                  activationData: data from activation.
**
** Returns:         true if the activation is from the same tag previously
**                  activated, false otherwise
**
*******************************************************************************/
bool NfcTag::IsSameKovio(tNFA_ACTIVATED& activationData)
{
    static const char fn [] = "NfcTag::IsSameKovio";
    ALOGD ("%s: enter", fn);
    tNFC_ACTIVATE_DEVT& rfDetail = activationData.activate_ntf;

    if (rfDetail.protocol != NFC_PROTOCOL_KOVIO)
        return false;

    memcpy (&(mTechParams[0]), &(rfDetail.rf_tech_param), sizeof(rfDetail.rf_tech_param));
    if (mTechParams [0].mode != NFC_DISCOVERY_TYPE_POLL_KOVIO)
        return false;

    struct timespec now;
    clock_gettime(CLOCK_REALTIME, &now);

    bool rVal = false;
    if (mTechParams[0].param.pk.uid_len == mLastKovioUidLen)
    {
        if (memcmp(mLastKovioUid, &mTechParams [0].param.pk.uid, mTechParams[0].param.pk.uid_len) == 0)
        {
            //same tag
            if (TimeDiff(mLastKovioTime, now) < 500)
            {
                // same tag within 500 ms, ignore activation
                rVal = true;
            }
        }
    }

    // save Kovio tag info
    if (!rVal)
    {
        if ((mLastKovioUidLen = mTechParams[0].param.pk.uid_len) > NFC_KOVIO_MAX_LEN)
            mLastKovioUidLen = NFC_KOVIO_MAX_LEN;
        memcpy(mLastKovioUid, mTechParams[0].param.pk.uid, mLastKovioUidLen);
    }
    mLastKovioTime = now;
    ALOGD ("%s: exit, is same Kovio=%d", fn, rVal);
    return rVal;
}

/*******************************************************************************
**
** Function:        discoverTechnologies
**
** Description:     Discover the technologies that NFC service needs by interpreting
**                  the data structures from the stack.
**                  activationData: data from activation.
**
** Returns:         None
**
*******************************************************************************/
void NfcTag::discoverTechnologies (tNFA_ACTIVATED& activationData)
{
    static const char fn [] = "NfcTag::discoverTechnologies (activation)";
    ALOGD ("%s: enter", fn);
    tNFC_ACTIVATE_DEVT& rfDetail = activationData.activate_ntf;

    mNumTechList = 0;
    mTechHandles [mNumTechList] = rfDetail.rf_disc_id;
    mTechLibNfcTypes [mNumTechList] = rfDetail.protocol;

    //save the stack's data structure for interpretation later
    memcpy (&(mTechParams[mNumTechList]), &(rfDetail.rf_tech_param), sizeof(rfDetail.rf_tech_param));

    switch (rfDetail.protocol)
    {
    case NFC_PROTOCOL_T1T:
        mTechList [mNumTechList] = TARGET_TYPE_ISO14443_3A; //is TagTechnology.NFC_A by Java API
        break;

    case NFC_PROTOCOL_T2T:
        mTechList [mNumTechList] = TARGET_TYPE_ISO14443_3A;  //is TagTechnology.NFC_A by Java API
        // could be MifFare UL or Classic or Kovio
        {
            // need to look at first byte of uid to find manuf.
            tNFC_RF_TECH_PARAMS tech_params;
            memcpy (&tech_params, &(rfDetail.rf_tech_param), sizeof(rfDetail.rf_tech_param));

            if ((tech_params.param.pa.nfcid1[0] == 0x04 && rfDetail.rf_tech_param.param.pa.sel_rsp == 0) ||
                rfDetail.rf_tech_param.param.pa.sel_rsp == 0x18 ||
                rfDetail.rf_tech_param.param.pa.sel_rsp == 0x08)
            {
                if (rfDetail.rf_tech_param.param.pa.sel_rsp == 0)
                {
                    mNumTechList++;
                    mTechHandles [mNumTechList] = rfDetail.rf_disc_id;
                    mTechLibNfcTypes [mNumTechList] = rfDetail.protocol;
                    //save the stack's data structure for interpretation later
                    memcpy (&(mTechParams[mNumTechList]), &(rfDetail.rf_tech_param), sizeof(rfDetail.rf_tech_param));
                    mTechList [mNumTechList] = TARGET_TYPE_MIFARE_UL; //is TagTechnology.MIFARE_ULTRALIGHT by Java API
                }
            }
        }
        break;

    case NFC_PROTOCOL_T3T:
        mTechList [mNumTechList] = TARGET_TYPE_FELICA;
        break;

    case NFC_PROTOCOL_ISO_DEP: //type-4 tag uses technology ISO-DEP and technology A or B
        mTechList [mNumTechList] = TARGET_TYPE_ISO14443_4; //is TagTechnology.ISO_DEP by Java API
        if ( (rfDetail.rf_tech_param.mode == NFC_DISCOVERY_TYPE_POLL_A) ||
                (rfDetail.rf_tech_param.mode == NFC_DISCOVERY_TYPE_POLL_A_ACTIVE) ||
                (rfDetail.rf_tech_param.mode == NFC_DISCOVERY_TYPE_LISTEN_A) ||
                (rfDetail.rf_tech_param.mode == NFC_DISCOVERY_TYPE_LISTEN_A_ACTIVE) )
        {
            mNumTechList++;
            mTechHandles [mNumTechList] = rfDetail.rf_disc_id;
            mTechLibNfcTypes [mNumTechList] = rfDetail.protocol;
            mTechList [mNumTechList] = TARGET_TYPE_ISO14443_3A; //is TagTechnology.NFC_A by Java API
            //save the stack's data structure for interpretation later
            memcpy (&(mTechParams[mNumTechList]), &(rfDetail.rf_tech_param), sizeof(rfDetail.rf_tech_param));
        }
        else if ( (rfDetail.rf_tech_param.mode == NFC_DISCOVERY_TYPE_POLL_B) ||
                (rfDetail.rf_tech_param.mode == NFC_DISCOVERY_TYPE_POLL_B_PRIME) ||
                (rfDetail.rf_tech_param.mode == NFC_DISCOVERY_TYPE_LISTEN_B) ||
                (rfDetail.rf_tech_param.mode == NFC_DISCOVERY_TYPE_LISTEN_B_PRIME) )
        {
            mNumTechList++;
            mTechHandles [mNumTechList] = rfDetail.rf_disc_id;
            mTechLibNfcTypes [mNumTechList] = rfDetail.protocol;
            mTechList [mNumTechList] = TARGET_TYPE_ISO14443_3B; //is TagTechnology.NFC_B by Java API
            //save the stack's data structure for interpretation later
            memcpy (&(mTechParams[mNumTechList]), &(rfDetail.rf_tech_param), sizeof(rfDetail.rf_tech_param));
        }
        break;

    case NFC_PROTOCOL_15693: //is TagTechnology.NFC_V by Java API
        mTechList [mNumTechList] = TARGET_TYPE_ISO15693;
        break;

    case NFC_PROTOCOL_KOVIO:
        ALOGD ("%s: Kovio", fn);
        mTechList [mNumTechList] = TARGET_TYPE_KOVIO_BARCODE;
        break;
    default:
        ALOGE ("%s: unknown protocol ????", fn);
        mTechList [mNumTechList] = TARGET_TYPE_UNKNOWN;
        break;
    }

    mNumTechList++;
    for (int i=0; i < mNumTechList; i++)
    {
        ALOGD ("%s: index=%d; tech=%d; handle=%d; nfc type=%d", fn,
                i, mTechList[i], mTechHandles[i], mTechLibNfcTypes[i]);
    }
    ALOGD ("%s: exit", fn);
}


/*******************************************************************************
**
** Function:        discoverTechnologies
**
** Description:     Discover the technologies that NFC service needs by interpreting
**                  the data structures from the stack.
**                  discoveryData: data from discovery events(s).
**
** Returns:         None
**
*******************************************************************************/
void NfcTag::discoverTechnologies (tNFA_DISC_RESULT& discoveryData)
{
    static const char fn [] = "NfcTag::discoverTechnologies (discovery)";
    tNFC_RESULT_DEVT& discovery_ntf = discoveryData.discovery_ntf;

    ALOGD ("%s: enter: rf disc. id=%u; protocol=%u, mNumTechList=%u", fn, discovery_ntf.rf_disc_id, discovery_ntf.protocol, mNumTechList);
    if (mNumTechList >= MAX_NUM_TECHNOLOGY)
    {
        ALOGE ("%s: exceed max=%d", fn, MAX_NUM_TECHNOLOGY);
        goto TheEnd;
    }
    mTechHandles [mNumTechList] = discovery_ntf.rf_disc_id;
    mTechLibNfcTypes [mNumTechList] = discovery_ntf.protocol;

    //save the stack's data structure for interpretation later
    memcpy (&(mTechParams[mNumTechList]), &(discovery_ntf.rf_tech_param), sizeof(discovery_ntf.rf_tech_param));

    switch (discovery_ntf.protocol)
    {
    case NFC_PROTOCOL_T1T:
        mTechList [mNumTechList] = TARGET_TYPE_ISO14443_3A; //is TagTechnology.NFC_A by Java API
        break;

    case NFC_PROTOCOL_T2T:
        mTechList [mNumTechList] = TARGET_TYPE_ISO14443_3A;  //is TagTechnology.NFC_A by Java API
        //type-2 tags are identitical to Mifare Ultralight, so Ultralight is also discovered
        if ((discovery_ntf.rf_tech_param.param.pa.sel_rsp == 0) &&
                (mNumTechList < (MAX_NUM_TECHNOLOGY-1)))
        {
            // mifare Ultralight
            mNumTechList++;
            mTechHandles [mNumTechList] = discovery_ntf.rf_disc_id;
            mTechLibNfcTypes [mNumTechList] = discovery_ntf.protocol;
            mTechList [mNumTechList] = TARGET_TYPE_MIFARE_UL; //is TagTechnology.MIFARE_ULTRALIGHT by Java API
        }

        //save the stack's data structure for interpretation later
        memcpy (&(mTechParams[mNumTechList]), &(discovery_ntf.rf_tech_param), sizeof(discovery_ntf.rf_tech_param));
        break;

    case NFC_PROTOCOL_T3T:
        mTechList [mNumTechList] = TARGET_TYPE_FELICA;
        break;

    case NFC_PROTOCOL_ISO_DEP: //type-4 tag uses technology ISO-DEP and technology A or B
        mTechList [mNumTechList] = TARGET_TYPE_ISO14443_4; //is TagTechnology.ISO_DEP by Java API
        if ( (discovery_ntf.rf_tech_param.mode == NFC_DISCOVERY_TYPE_POLL_A) ||
                (discovery_ntf.rf_tech_param.mode == NFC_DISCOVERY_TYPE_POLL_A_ACTIVE) ||
                (discovery_ntf.rf_tech_param.mode == NFC_DISCOVERY_TYPE_LISTEN_A) ||
                (discovery_ntf.rf_tech_param.mode == NFC_DISCOVERY_TYPE_LISTEN_A_ACTIVE) )
        {
            if (mNumTechList < (MAX_NUM_TECHNOLOGY-1))
            {
                mNumTechList++;
                mTechHandles [mNumTechList] = discovery_ntf.rf_disc_id;
                mTechLibNfcTypes [mNumTechList] = discovery_ntf.protocol;
                mTechList [mNumTechList] = TARGET_TYPE_ISO14443_3A; //is TagTechnology.NFC_A by Java API
                //save the stack's data structure for interpretation later
                memcpy (&(mTechParams[mNumTechList]), &(discovery_ntf.rf_tech_param), sizeof(discovery_ntf.rf_tech_param));
            }
        }
        else if ( (discovery_ntf.rf_tech_param.mode == NFC_DISCOVERY_TYPE_POLL_B) ||
                (discovery_ntf.rf_tech_param.mode == NFC_DISCOVERY_TYPE_POLL_B_PRIME) ||
                (discovery_ntf.rf_tech_param.mode == NFC_DISCOVERY_TYPE_LISTEN_B) ||
                (discovery_ntf.rf_tech_param.mode == NFC_DISCOVERY_TYPE_LISTEN_B_PRIME) )
        {
            if (mNumTechList < (MAX_NUM_TECHNOLOGY-1))
            {
                mNumTechList++;
                mTechHandles [mNumTechList] = discovery_ntf.rf_disc_id;
                mTechLibNfcTypes [mNumTechList] = discovery_ntf.protocol;
                mTechList [mNumTechList] = TARGET_TYPE_ISO14443_3B; //is TagTechnology.NFC_B by Java API
                //save the stack's data structure for interpretation later
                memcpy (&(mTechParams[mNumTechList]), &(discovery_ntf.rf_tech_param), sizeof(discovery_ntf.rf_tech_param));
            }
        }
        break;

    case NFC_PROTOCOL_15693: //is TagTechnology.NFC_V by Java API
        mTechList [mNumTechList] = TARGET_TYPE_ISO15693;
        break;
    default:
        ALOGE ("%s: unknown protocol ????", fn);
        mTechList [mNumTechList] = TARGET_TYPE_UNKNOWN;
        break;
    }

    mNumTechList++;
    if (discovery_ntf.more == FALSE)
    {
        for (int i=0; i < mNumTechList; i++)
        {
            ALOGD ("%s: index=%d; tech=%d; handle=%d; nfc type=%d", fn,
                    i, mTechList[i], mTechHandles[i], mTechLibNfcTypes[i]);
        }
    }

TheEnd:
    ALOGD ("%s: exit", fn);
}


/*******************************************************************************
**
** Function:        createNativeNfcTag
**
** Description:     Create a brand new Java NativeNfcTag object;
**                  fill the objects's member variables with data;
**                  notify NFC service;
**                  activationData: data from activation.
**
** Returns:         None
**
*******************************************************************************/
void NfcTag::createNativeNfcTag (tNFA_ACTIVATED& activationData)
{
    static const char fn [] = "NfcTag::createNativeNfcTag";
    ALOGD ("%s: enter", fn);

    JNIEnv* e = NULL;
    ScopedAttach attach(mNativeData->vm, &e);
    if (e == NULL)
    {
        ALOGE("%s: jni env is null", fn);
        return;
    }

    ScopedLocalRef<jclass> tag_cls(e, e->GetObjectClass(mNativeData->cached_NfcTag));
    if (e->ExceptionCheck())
    {
        e->ExceptionClear();
        ALOGE("%s: failed to get class", fn);
        return;
    }

    //create a new Java NativeNfcTag object
    jmethodID ctor = e->GetMethodID(tag_cls.get(), "<init>", "()V");
    ScopedLocalRef<jobject> tag(e, e->NewObject(tag_cls.get(), ctor));

    //fill NativeNfcTag's mProtocols, mTechList, mTechHandles, mTechLibNfcTypes
    fillNativeNfcTagMembers1(e, tag_cls.get(), tag.get());

    //fill NativeNfcTag's members: mHandle, mConnectedTechnology
    fillNativeNfcTagMembers2(e, tag_cls.get(), tag.get(), activationData);

    //fill NativeNfcTag's members: mTechPollBytes
    fillNativeNfcTagMembers3(e, tag_cls.get(), tag.get(), activationData);

    //fill NativeNfcTag's members: mTechActBytes
    fillNativeNfcTagMembers4(e, tag_cls.get(), tag.get(), activationData);

    //fill NativeNfcTag's members: mUid
    fillNativeNfcTagMembers5(e, tag_cls.get(), tag.get(), activationData);

    if (mNativeData->tag != NULL)
    {
        e->DeleteGlobalRef(mNativeData->tag);
    }
    mNativeData->tag = e->NewGlobalRef(tag.get());

    //notify NFC service about this new tag
    ALOGD ("%s: try notify nfc service", fn);
    e->CallVoidMethod(mNativeData->manager, android::gCachedNfcManagerNotifyNdefMessageListeners, tag.get());
    if (e->ExceptionCheck())
    {
        e->ExceptionClear();
        ALOGE ("%s: fail notify nfc service", fn);
    }

    ALOGD ("%s: exit", fn);
}


/*******************************************************************************
**
** Function:        fillNativeNfcTagMembers1
**
** Description:     Fill NativeNfcTag's members: mProtocols, mTechList, mTechHandles, mTechLibNfcTypes.
**                  e: JVM environment.
**                  tag_cls: Java NativeNfcTag class.
**                  tag: Java NativeNfcTag object.
**
** Returns:         None
**
*******************************************************************************/
void NfcTag::fillNativeNfcTagMembers1 (JNIEnv* e, jclass tag_cls, jobject tag)
{
    static const char fn [] = "NfcTag::fillNativeNfcTagMembers1";
    ALOGD ("%s", fn);

    //create objects that represent NativeNfcTag's member variables
    ScopedLocalRef<jintArray> techList(e, e->NewIntArray(mNumTechList));
    ScopedLocalRef<jintArray> handleList(e, e->NewIntArray(mNumTechList));
    ScopedLocalRef<jintArray> typeList(e, e->NewIntArray(mNumTechList));

    {
        ScopedIntArrayRW technologies(e, techList.get());
        ScopedIntArrayRW handles(e, handleList.get());
        ScopedIntArrayRW types(e, typeList.get());
        for (int i = 0; i < mNumTechList; i++) {
            mNativeData->tProtocols [i] = mTechLibNfcTypes [i];
            mNativeData->handles [i] = mTechHandles [i];
            technologies [i] = mTechList [i];
            handles [i]      = mTechHandles [i];
            types [i]        = mTechLibNfcTypes [i];
        }
    }

    jfieldID f = NULL;

    f = e->GetFieldID(tag_cls, "mTechList", "[I");
    e->SetObjectField(tag, f, techList.get());

    f = e->GetFieldID(tag_cls, "mTechHandles", "[I");
    e->SetObjectField(tag, f, handleList.get());

    f = e->GetFieldID(tag_cls, "mTechLibNfcTypes", "[I");
    e->SetObjectField(tag, f, typeList.get());
}


/*******************************************************************************
**
** Function:        fillNativeNfcTagMembers2
**
** Description:     Fill NativeNfcTag's members: mConnectedTechIndex or mConnectedTechnology.
**                  The original Google's implementation is in set_target_pollBytes(
**                  in com_android_nfc_NativeNfcTag.cpp;
**                  e: JVM environment.
**                  tag_cls: Java NativeNfcTag class.
**                  tag: Java NativeNfcTag object.
**                  activationData: data from activation.
**
** Returns:         None
**
*******************************************************************************/
//fill NativeNfcTag's members: mHandle, mConnectedTechnology
void NfcTag::fillNativeNfcTagMembers2 (JNIEnv* e, jclass tag_cls, jobject tag, tNFA_ACTIVATED& /*activationData*/)
{
    static const char fn [] = "NfcTag::fillNativeNfcTagMembers2";
    ALOGD ("%s", fn);
    jfieldID f = e->GetFieldID(tag_cls, "mConnectedTechIndex", "I");
    e->SetIntField(tag, f, (jint) 0);
}


/*******************************************************************************
**
** Function:        fillNativeNfcTagMembers3
**
** Description:     Fill NativeNfcTag's members: mTechPollBytes.
**                  The original Google's implementation is in set_target_pollBytes(
**                  in com_android_nfc_NativeNfcTag.cpp;
**                  e: JVM environment.
**                  tag_cls: Java NativeNfcTag class.
**                  tag: Java NativeNfcTag object.
**                  activationData: data from activation.
**
** Returns:         None
**
*******************************************************************************/
void NfcTag::fillNativeNfcTagMembers3 (JNIEnv* e, jclass tag_cls, jobject tag, tNFA_ACTIVATED& activationData)
{
    static const char fn [] = "NfcTag::fillNativeNfcTagMembers3";
    ScopedLocalRef<jbyteArray> pollBytes(e, e->NewByteArray(0));
    ScopedLocalRef<jclass> byteArrayClass(e, e->GetObjectClass(pollBytes.get()));
    ScopedLocalRef<jobjectArray> techPollBytes(e, e->NewObjectArray(mNumTechList, byteArrayClass.get(), 0));
    int len = 0;

    for (int i = 0; i < mNumTechList; i++)
    {
        ALOGD ("%s: index=%d; rf tech params mode=%u", fn, i, mTechParams [i].mode);
        switch (mTechParams [i].mode)
        {
        case NFC_DISCOVERY_TYPE_POLL_A:
        case NFC_DISCOVERY_TYPE_POLL_A_ACTIVE:
        case NFC_DISCOVERY_TYPE_LISTEN_A:
        case NFC_DISCOVERY_TYPE_LISTEN_A_ACTIVE:
            ALOGD ("%s: tech A", fn);
            pollBytes.reset(e->NewByteArray(2));
            e->SetByteArrayRegion(pollBytes.get(), 0, 2, (jbyte*) mTechParams [i].param.pa.sens_res);
            break;

        case NFC_DISCOVERY_TYPE_POLL_B:
        case NFC_DISCOVERY_TYPE_POLL_B_PRIME:
        case NFC_DISCOVERY_TYPE_LISTEN_B:
        case NFC_DISCOVERY_TYPE_LISTEN_B_PRIME:
            if (mTechList [i] == TARGET_TYPE_ISO14443_3B) //is TagTechnology.NFC_B by Java API
            {
                /*****************
                see NFC Forum Digital Protocol specification; section 5.6.2;
                in SENSB_RES response, byte 6 through 9 is Application Data, byte 10-12 or 13 is Protocol Info;
                used by public API: NfcB.getApplicationData(), NfcB.getProtocolInfo();
                *****************/
                ALOGD ("%s: tech B; TARGET_TYPE_ISO14443_3B", fn);
                len = mTechParams [i].param.pb.sensb_res_len;
                len = len - 4; //subtract 4 bytes for NFCID0 at byte 2 through 5
                pollBytes.reset(e->NewByteArray(len));
                e->SetByteArrayRegion(pollBytes.get(), 0, len, (jbyte*) (mTechParams [i].param.pb.sensb_res+4));
            }
            else
            {
                pollBytes.reset(e->NewByteArray(0));
            }
            break;

        case NFC_DISCOVERY_TYPE_POLL_F:
        case NFC_DISCOVERY_TYPE_POLL_F_ACTIVE:
        case NFC_DISCOVERY_TYPE_LISTEN_F:
        case NFC_DISCOVERY_TYPE_LISTEN_F_ACTIVE:
            {
                /****************
                see NFC Forum Type 3 Tag Operation Specification; sections 2.3.2, 2.3.1.2;
                see NFC Forum Digital Protocol Specification; sections 6.6.2;
                PMm: manufacture parameter; 8 bytes;
                System Code: 2 bytes;
                ****************/
                ALOGD ("%s: tech F", fn);
                UINT8 result [10]; //return result to NFC service
                memset (result, 0, sizeof(result));
                len =  10;

                /****
                for (int ii = 0; ii < mTechParams [i].param.pf.sensf_res_len; ii++)
                {
                    ALOGD ("%s: tech F, sendf_res[%d]=%d (0x%x)",
                          fn, ii, mTechParams [i].param.pf.sensf_res[ii],mTechParams [i].param.pf.sensf_res[ii]);
                }
                ***/
                memcpy (result, mTechParams [i].param.pf.sensf_res + 8, 8); //copy PMm
                if (activationData.params.t3t.num_system_codes > 0) //copy the first System Code
                {
                    UINT16 systemCode = *(activationData.params.t3t.p_system_codes);
                    result [8] = (UINT8) (systemCode >> 8);
                    result [9] = (UINT8) systemCode;
                    ALOGD ("%s: tech F; sys code=0x%X 0x%X", fn, result [8], result [9]);
                }
                pollBytes.reset(e->NewByteArray(len));
                e->SetByteArrayRegion(pollBytes.get(), 0, len, (jbyte*) result);
            }
            break;

        case NFC_DISCOVERY_TYPE_POLL_ISO15693:
        case NFC_DISCOVERY_TYPE_LISTEN_ISO15693:
            {
                ALOGD ("%s: tech iso 15693", fn);
                //iso 15693 response flags: 1 octet
                //iso 15693 Data Structure Format Identifier (DSF ID): 1 octet
                //used by public API: NfcV.getDsfId(), NfcV.getResponseFlags();
                uint8_t data [2]= {activationData.params.i93.afi, activationData.params.i93.dsfid};
                pollBytes.reset(e->NewByteArray(2));
                e->SetByteArrayRegion(pollBytes.get(), 0, 2, (jbyte *) data);
            }
            break;

        default:
            ALOGE ("%s: tech unknown ????", fn);
            pollBytes.reset(e->NewByteArray(0));
            break;
        } //switch: every type of technology
        e->SetObjectArrayElement(techPollBytes.get(), i, pollBytes.get());
    } //for: every technology in the array
    jfieldID f = e->GetFieldID(tag_cls, "mTechPollBytes", "[[B");
    e->SetObjectField(tag, f, techPollBytes.get());
}


/*******************************************************************************
**
** Function:        fillNativeNfcTagMembers4
**
** Description:     Fill NativeNfcTag's members: mTechActBytes.
**                  The original Google's implementation is in set_target_activationBytes()
**                  in com_android_nfc_NativeNfcTag.cpp;
**                  e: JVM environment.
**                  tag_cls: Java NativeNfcTag class.
**                  tag: Java NativeNfcTag object.
**                  activationData: data from activation.
**
** Returns:         None
**
*******************************************************************************/
void NfcTag::fillNativeNfcTagMembers4 (JNIEnv* e, jclass tag_cls, jobject tag, tNFA_ACTIVATED& activationData)
{
    static const char fn [] = "NfcTag::fillNativeNfcTagMembers4";
    ScopedLocalRef<jbyteArray> actBytes(e, e->NewByteArray(0));
    ScopedLocalRef<jclass> byteArrayClass(e, e->GetObjectClass(actBytes.get()));
    ScopedLocalRef<jobjectArray> techActBytes(e, e->NewObjectArray(mNumTechList, byteArrayClass.get(), 0));

    for (int i = 0; i < mNumTechList; i++)
    {
        ALOGD ("%s: index=%d", fn, i);
        switch (mTechLibNfcTypes[i])
        {
        case NFC_PROTOCOL_T1T:
        case NFC_PROTOCOL_T2T:
            {
                if (mTechLibNfcTypes[i] == NFC_PROTOCOL_T1T)
                    ALOGD ("%s: T1T; tech A", fn);
                else if (mTechLibNfcTypes[i] == NFC_PROTOCOL_T2T)
                    ALOGD ("%s: T2T; tech A", fn);
                actBytes.reset(e->NewByteArray(1));
                e->SetByteArrayRegion(actBytes.get(), 0, 1, (jbyte*) &mTechParams [i].param.pa.sel_rsp);
            }
            break;

        case NFC_PROTOCOL_T3T: //felica
            {
                ALOGD ("%s: T3T; felica; tech F", fn);
                //really, there is no data
                actBytes.reset(e->NewByteArray(0));
            }
            break;

        case NFC_PROTOCOL_ISO_DEP: //t4t
            {
                if (mTechList [i] == TARGET_TYPE_ISO14443_4) //is TagTechnology.ISO_DEP by Java API
                {
                    if ( (mTechParams[i].mode == NFC_DISCOVERY_TYPE_POLL_A) ||
                            (mTechParams[i].mode == NFC_DISCOVERY_TYPE_POLL_A_ACTIVE) ||
                            (mTechParams[i].mode == NFC_DISCOVERY_TYPE_LISTEN_A) ||
                            (mTechParams[i].mode == NFC_DISCOVERY_TYPE_LISTEN_A_ACTIVE) )
                    {
                        //see NFC Forum Digital Protocol specification, section 11.6.2, "RATS Response"; search for "historical bytes";
                        //copy historical bytes into Java object;
                        //the public API, IsoDep.getHistoricalBytes(), returns this data;
                        if (activationData.activate_ntf.intf_param.type == NFC_INTERFACE_ISO_DEP)
                        {
                            tNFC_INTF_PA_ISO_DEP& pa_iso = activationData.activate_ntf.intf_param.intf_param.pa_iso;
                            ALOGD ("%s: T4T; ISO_DEP for tech A; copy historical bytes; len=%u", fn, pa_iso.his_byte_len);
                            actBytes.reset(e->NewByteArray(pa_iso.his_byte_len));
                            if (pa_iso.his_byte_len > 0)
                                e->SetByteArrayRegion(actBytes.get(), 0, pa_iso.his_byte_len, (jbyte*) (pa_iso.his_byte));
                        }
                        else
                        {
                            ALOGE ("%s: T4T; ISO_DEP for tech A; wrong interface=%u", fn, activationData.activate_ntf.intf_param.type);
                            actBytes.reset(e->NewByteArray(0));
                        }
                    }
                    else if ( (mTechParams[i].mode == NFC_DISCOVERY_TYPE_POLL_B) ||
                            (mTechParams[i].mode == NFC_DISCOVERY_TYPE_POLL_B_PRIME) ||
                            (mTechParams[i].mode == NFC_DISCOVERY_TYPE_LISTEN_B) ||
                            (mTechParams[i].mode == NFC_DISCOVERY_TYPE_LISTEN_B_PRIME) )
                    {
                        //see NFC Forum Digital Protocol specification, section 12.6.2, "ATTRIB Response";
                        //copy higher-layer response bytes into Java object;
                        //the public API, IsoDep.getHiLayerResponse(), returns this data;
                        if (activationData.activate_ntf.intf_param.type == NFC_INTERFACE_ISO_DEP)
                        {
                            tNFC_INTF_PB_ISO_DEP& pb_iso = activationData.activate_ntf.intf_param.intf_param.pb_iso;
                            ALOGD ("%s: T4T; ISO_DEP for tech B; copy response bytes; len=%u", fn, pb_iso.hi_info_len);
                            actBytes.reset(e->NewByteArray(pb_iso.hi_info_len));
                            if (pb_iso.hi_info_len > 0)
                                e->SetByteArrayRegion(actBytes.get(), 0, pb_iso.hi_info_len, (jbyte*) (pb_iso.hi_info));
                        }
                        else
                        {
                            ALOGE ("%s: T4T; ISO_DEP for tech B; wrong interface=%u", fn, activationData.activate_ntf.intf_param.type);
                            actBytes.reset(e->NewByteArray(0));
                        }
                    }
                }
                else if (mTechList [i] == TARGET_TYPE_ISO14443_3A) //is TagTechnology.NFC_A by Java API
                {
                    ALOGD ("%s: T4T; tech A", fn);
                    actBytes.reset(e->NewByteArray(1));
                    e->SetByteArrayRegion(actBytes.get(), 0, 1, (jbyte*) &mTechParams [i].param.pa.sel_rsp);
                }
                else
                {
                    actBytes.reset(e->NewByteArray(0));
                }
            } //case NFC_PROTOCOL_ISO_DEP: //t4t
            break;

        case NFC_PROTOCOL_15693:
            {
                ALOGD ("%s: tech iso 15693", fn);
                //iso 15693 response flags: 1 octet
                //iso 15693 Data Structure Format Identifier (DSF ID): 1 octet
                //used by public API: NfcV.getDsfId(), NfcV.getResponseFlags();
                uint8_t data [2]= {activationData.params.i93.afi, activationData.params.i93.dsfid};
                actBytes.reset(e->NewByteArray(2));
                e->SetByteArrayRegion(actBytes.get(), 0, 2, (jbyte *) data);
            }
            break;

        default:
            ALOGD ("%s: tech unknown ????", fn);
            actBytes.reset(e->NewByteArray(0));
            break;
        }//switch
        e->SetObjectArrayElement(techActBytes.get(), i, actBytes.get());
    } //for: every technology in the array
    jfieldID f = e->GetFieldID (tag_cls, "mTechActBytes", "[[B");
    e->SetObjectField(tag, f, techActBytes.get());
}


/*******************************************************************************
**
** Function:        fillNativeNfcTagMembers5
**
** Description:     Fill NativeNfcTag's members: mUid.
**                  The original Google's implementation is in nfc_jni_Discovery_notification_callback()
**                  in com_android_nfc_NativeNfcManager.cpp;
**                  e: JVM environment.
**                  tag_cls: Java NativeNfcTag class.
**                  tag: Java NativeNfcTag object.
**                  activationData: data from activation.
**
** Returns:         None
**
*******************************************************************************/
void NfcTag::fillNativeNfcTagMembers5 (JNIEnv* e, jclass tag_cls, jobject tag, tNFA_ACTIVATED& activationData)
{
    static const char fn [] = "NfcTag::fillNativeNfcTagMembers5";
    int len = 0;
    ScopedLocalRef<jbyteArray> uid(e, NULL);

    switch (mTechParams [0].mode)
    {
    case NFC_DISCOVERY_TYPE_POLL_KOVIO:
        ALOGD ("%s: Kovio", fn);
        len = mTechParams [0].param.pk.uid_len;
        uid.reset(e->NewByteArray(len));
        e->SetByteArrayRegion(uid.get(), 0, len,
                (jbyte*) &mTechParams [0].param.pk.uid);
        break;

    case NFC_DISCOVERY_TYPE_POLL_A:
    case NFC_DISCOVERY_TYPE_POLL_A_ACTIVE:
    case NFC_DISCOVERY_TYPE_LISTEN_A:
    case NFC_DISCOVERY_TYPE_LISTEN_A_ACTIVE:
        ALOGD ("%s: tech A", fn);
        len = mTechParams [0].param.pa.nfcid1_len;
        uid.reset(e->NewByteArray(len));
        e->SetByteArrayRegion(uid.get(), 0, len,
                (jbyte*) &mTechParams [0].param.pa.nfcid1);
        break;

    case NFC_DISCOVERY_TYPE_POLL_B:
    case NFC_DISCOVERY_TYPE_POLL_B_PRIME:
    case NFC_DISCOVERY_TYPE_LISTEN_B:
    case NFC_DISCOVERY_TYPE_LISTEN_B_PRIME:
        ALOGD ("%s: tech B", fn);
        uid.reset(e->NewByteArray(NFC_NFCID0_MAX_LEN));
        e->SetByteArrayRegion(uid.get(), 0, NFC_NFCID0_MAX_LEN,
                (jbyte*) &mTechParams [0].param.pb.nfcid0);
        break;

    case NFC_DISCOVERY_TYPE_POLL_F:
    case NFC_DISCOVERY_TYPE_POLL_F_ACTIVE:
    case NFC_DISCOVERY_TYPE_LISTEN_F:
    case NFC_DISCOVERY_TYPE_LISTEN_F_ACTIVE:
        uid.reset(e->NewByteArray(NFC_NFCID2_LEN));
        e->SetByteArrayRegion(uid.get(), 0, NFC_NFCID2_LEN,
                (jbyte*) &mTechParams [0].param.pf.nfcid2);
        ALOGD ("%s: tech F", fn);
        break;

    case NFC_DISCOVERY_TYPE_POLL_ISO15693:
    case NFC_DISCOVERY_TYPE_LISTEN_ISO15693:
        {
            ALOGD ("%s: tech iso 15693", fn);
            jbyte data [I93_UID_BYTE_LEN];  //8 bytes
            for (int i=0; i<I93_UID_BYTE_LEN; ++i) //reverse the ID
                data[i] = activationData.params.i93.uid [I93_UID_BYTE_LEN - i - 1];
            uid.reset(e->NewByteArray(I93_UID_BYTE_LEN));
            e->SetByteArrayRegion(uid.get(), 0, I93_UID_BYTE_LEN, data);
        }
        break;

    default:
        ALOGE ("%s: tech unknown ????", fn);
        uid.reset(e->NewByteArray(0));
        break;
    }
    jfieldID f = e->GetFieldID(tag_cls, "mUid", "[B");
    e->SetObjectField(tag, f, uid.get());
}


/*******************************************************************************
**
** Function:        isP2pDiscovered
**
** Description:     Does the peer support P2P?
**
** Returns:         True if the peer supports P2P.
**
*******************************************************************************/
bool NfcTag::isP2pDiscovered ()
{
    static const char fn [] = "NfcTag::isP2pDiscovered";
    bool retval = false;

    for (int i = 0; i < mNumTechList; i++)
    {
        if (mTechLibNfcTypes[i] == NFA_PROTOCOL_NFC_DEP)
        {
            //if remote device supports P2P
            ALOGD ("%s: discovered P2P", fn);
            retval = true;
            break;
        }
    }
    ALOGD ("%s: return=%u", fn, retval);
    return retval;
}


/*******************************************************************************
**
** Function:        selectP2p
**
** Description:     Select the preferred P2P technology if there is a choice.
**
** Returns:         None
**
*******************************************************************************/
void NfcTag::selectP2p()
{
    static const char fn [] = "NfcTag::selectP2p";
    UINT8 rfDiscoveryId = 0;

    for (int i = 0; i < mNumTechList; i++)
    {
        //if remote device does not support P2P, just skip it
        if (mTechLibNfcTypes[i] != NFA_PROTOCOL_NFC_DEP)
            continue;

        //if remote device supports tech F;
        //tech F is preferred because it is faster than tech A
        if ( (mTechParams[i].mode == NFC_DISCOVERY_TYPE_POLL_F) ||
             (mTechParams[i].mode == NFC_DISCOVERY_TYPE_POLL_F_ACTIVE) )
        {
            rfDiscoveryId = mTechHandles[i];
            break; //no need to search further
        }
        else if ( (mTechParams[i].mode == NFC_DISCOVERY_TYPE_POLL_A) ||
                (mTechParams[i].mode == NFC_DISCOVERY_TYPE_POLL_A_ACTIVE) )
        {
            //only choose tech A if tech F is unavailable
            if (rfDiscoveryId == 0)
                rfDiscoveryId = mTechHandles[i];
        }
    }

    if (rfDiscoveryId > 0)
    {
        ALOGD ("%s: select P2P; target rf discov id=0x%X", fn, rfDiscoveryId);
        tNFA_STATUS stat = NFA_Select (rfDiscoveryId, NFA_PROTOCOL_NFC_DEP, NFA_INTERFACE_NFC_DEP);
        if (stat != NFA_STATUS_OK)
            ALOGE ("%s: fail select P2P; error=0x%X", fn, stat);
    }
    else
        ALOGE ("%s: cannot find P2P", fn);
    resetTechnologies ();
}


/*******************************************************************************
**
** Function:        resetTechnologies
**
** Description:     Clear all data related to the technology, protocol of the tag.
**
** Returns:         None
**
*******************************************************************************/
void NfcTag::resetTechnologies ()
{
    static const char fn [] = "NfcTag::resetTechnologies";
    ALOGD ("%s", fn);
   	mNumTechList = 0;
    memset (mTechList, 0, sizeof(mTechList));
    memset (mTechHandles, 0, sizeof(mTechHandles));
    memset (mTechLibNfcTypes, 0, sizeof(mTechLibNfcTypes));
    memset (mTechParams, 0, sizeof(mTechParams));
    resetAllTransceiveTimeouts ();
}


/*******************************************************************************
**
** Function:        selectFirstTag
**
** Description:     When multiple tags are discovered, just select the first one to activate.
**
** Returns:         None
**
*******************************************************************************/
void NfcTag::selectFirstTag ()
{
    static const char fn [] = "NfcTag::selectFirstTag";
    int foundIdx = -1;
    tNFA_INTF_TYPE rf_intf = NFA_INTERFACE_FRAME;

    for (int i = 0; i < mNumTechList; i++)
    {
        ALOGD ("%s: nfa target idx=%d h=0x%X; protocol=0x%X",
                fn, i, mTechHandles [i], mTechLibNfcTypes [i]);
        if (mTechLibNfcTypes[i] != NFA_PROTOCOL_NFC_DEP)
        {
            foundIdx = i;
            break;
        }
    }

    if (foundIdx != -1)
    {
        if (mTechLibNfcTypes [foundIdx] == NFA_PROTOCOL_ISO_DEP)
        {
            rf_intf = NFA_INTERFACE_ISO_DEP;
        }
        else
            rf_intf = NFA_INTERFACE_FRAME;

        tNFA_STATUS stat = NFA_Select (mTechHandles [foundIdx], mTechLibNfcTypes [foundIdx], rf_intf);
        if (stat != NFA_STATUS_OK)
            ALOGE ("%s: fail select; error=0x%X", fn, stat);
    }
    else
        ALOGE ("%s: only found NFC-DEP technology.", fn);
}


/*******************************************************************************
**
** Function:        getT1tMaxMessageSize
**
** Description:     Get the maximum size (octet) that a T1T can store.
**
** Returns:         Maximum size in octets.
**
*******************************************************************************/
int NfcTag::getT1tMaxMessageSize ()
{
    static const char fn [] = "NfcTag::getT1tMaxMessageSize";

    if (mProtocol != NFC_PROTOCOL_T1T)
    {
        ALOGE ("%s: wrong protocol %u", fn, mProtocol);
        return 0;
    }
    return mtT1tMaxMessageSize;
}


/*******************************************************************************
**
** Function:        calculateT1tMaxMessageSize
**
** Description:     Calculate type-1 tag's max message size based on header ROM bytes.
**                  activate: reference to activation data.
**
** Returns:         None
**
*******************************************************************************/
void NfcTag::calculateT1tMaxMessageSize (tNFA_ACTIVATED& activate)
{
    static const char fn [] = "NfcTag::calculateT1tMaxMessageSize";

    //make sure the tag is type-1
    if (activate.activate_ntf.protocol != NFC_PROTOCOL_T1T)
    {
        mtT1tMaxMessageSize = 0;
        return;
    }

    //examine the first byte of header ROM bytes
    switch (activate.params.t1t.hr[0])
    {
    case RW_T1T_IS_TOPAZ96:
        mtT1tMaxMessageSize = 90;
        break;
    case RW_T1T_IS_TOPAZ512:
        mtT1tMaxMessageSize = 462;
        break;
    default:
        ALOGE ("%s: unknown T1T HR0=%u", fn, activate.params.t1t.hr[0]);
        mtT1tMaxMessageSize = 0;
        break;
    }
}


/*******************************************************************************
**
** Function:        isMifareUltralight
**
** Description:     Whether the currently activated tag is Mifare Ultralight.
**
** Returns:         True if tag is Mifare Ultralight.
**
*******************************************************************************/
bool NfcTag::isMifareUltralight ()
{
    static const char fn [] = "NfcTag::isMifareUltralight";
    bool retval = false;

    for (int i =0; i < mNumTechList; i++)
    {
        if ( (mTechParams[i].mode == NFC_DISCOVERY_TYPE_POLL_A) ||
             (mTechParams[i].mode == NFC_DISCOVERY_TYPE_LISTEN_A) ||
             (mTechParams[i].mode == NFC_DISCOVERY_TYPE_LISTEN_A_ACTIVE) )
        {
            //see NFC Digital Protocol, section 4.6.3 (SENS_RES); section 4.8.2 (SEL_RES).
            //see Mifare Type Identification Procedure, section 5.1 (ATQA), 5.2 (SAK).
            if ( (mTechParams[i].param.pa.sens_res[0] == 0x44) &&
                 (mTechParams[i].param.pa.sens_res[1] == 0) )
            {
                // SyncEventGuard g (mReadCompleteEvent);
                // mReadCompletedStatus = NFA_STATUS_BUSY;
                // ALOGD ("%s: read block 0x10", fn);
                // tNFA_STATUS stat = NFA_RwT2tRead (0x10);
                // if (stat == NFA_STATUS_OK)
                    // mReadCompleteEvent.wait ();
                //
                // //if read-completion status is failure, then the tag is
                // //definitely Mifare Ultralight;
                // //if read-completion status is OK, then the tag is
                // //definitely Mifare Ultralight C;
                // retval = (mReadCompletedStatus == NFA_STATUS_FAILED);
                retval = true;
            }
            break;
        }
    }
    ALOGD ("%s: return=%u", fn, retval);
    return retval;
}


/*******************************************************************************
**
** Function:        isT2tNackResponse
**
** Description:     Whether the response is a T2T NACK response.
**                  See NFC Digital Protocol Technical Specification (2010-11-17).
**                  Chapter 9 (Type 2 Tag Platform), section 9.6 (READ).
**                  response: buffer contains T2T response.
**                  responseLen: length of the response.
**
** Returns:         True if the response is NACK
**
*******************************************************************************/
bool NfcTag::isT2tNackResponse (const UINT8* response, UINT32 responseLen)
{
    static const char fn [] = "NfcTag::isT2tNackResponse";
    bool isNack = false;

    if (responseLen == 1)
    {
        if (response[0] == 0xA)
            isNack = false; //an ACK response, so definitely not a NACK
        else
            isNack = true; //assume every value is a NACK
    }
    ALOGD ("%s: return %u", fn, isNack);
    return isNack;
}


/*******************************************************************************
**
** Function:        isNdefDetectionTimedOut
**
** Description:     Whether NDEF-detection algorithm timed out.
**
** Returns:         True if NDEF-detection algorithm timed out.
**
*******************************************************************************/
bool NfcTag::isNdefDetectionTimedOut ()
{
    return mNdefDetectionTimedOut;
}


/*******************************************************************************
**
** Function:        connectionEventHandler
**
** Description:     Handle connection-related events.
**                  event: event code.
**                  data: pointer to event data.
**
** Returns:         None
**
*******************************************************************************/
void NfcTag::connectionEventHandler (UINT8 event, tNFA_CONN_EVT_DATA* data)
{
    static const char fn [] = "NfcTag::connectionEventHandler";

    switch (event)
    {
    case NFA_DISC_RESULT_EVT:
        {
            tNFA_DISC_RESULT& disc_result = data->disc_result;
            if (disc_result.status == NFA_STATUS_OK)
            {
                discoverTechnologies (disc_result);
            }
        }
        break;

    case NFA_ACTIVATED_EVT:
        // Only do tag detection if we are polling and it is not 'EE Direct RF' activation
        // (which may happen when we are activated as a tag).
        if (data->activated.activate_ntf.rf_tech_param.mode < NCI_DISCOVERY_TYPE_LISTEN_A
            && data->activated.activate_ntf.intf_param.type != NFC_INTERFACE_EE_DIRECT_RF)
        {
            tNFA_ACTIVATED& activated = data->activated;
            if (IsSameKovio(activated))
                break;
            mIsActivated = true;
            mProtocol = activated.activate_ntf.protocol;
            calculateT1tMaxMessageSize (activated);
            discoverTechnologies (activated);
            createNativeNfcTag (activated);
        }
        break;

    case NFA_DEACTIVATED_EVT:
        mIsActivated = false;
        mProtocol = NFC_PROTOCOL_UNKNOWN;
        resetTechnologies ();
        break;

    case NFA_READ_CPLT_EVT:
        {
            SyncEventGuard g (mReadCompleteEvent);
            mReadCompletedStatus = data->status;
            mReadCompleteEvent.notifyOne ();
        }
        break;

    case NFA_NDEF_DETECT_EVT:
        {
            tNFA_NDEF_DETECT& ndef_detect = data->ndef_detect;
            mNdefDetectionTimedOut = ndef_detect.status == NFA_STATUS_TIMEOUT;
            if (mNdefDetectionTimedOut)
                ALOGE ("%s: NDEF detection timed out", fn);
        }
    }
}

/*******************************************************************************
**
** Function         setActive
**
** Description      Sets the active state for the object
**
** Returns          None.
**
*******************************************************************************/
void NfcTag::setActive(bool active)
{
    mIsActivated = active;
}


/*******************************************************************************
**
** Function:        resetAllTransceiveTimeouts
**
** Description:     Reset all timeouts for all technologies to default values.
**
** Returns:         none
**
*******************************************************************************/
void NfcTag::resetAllTransceiveTimeouts ()
{
    mTechnologyTimeoutsTable [TARGET_TYPE_ISO14443_3A] = 618; //NfcA
    mTechnologyTimeoutsTable [TARGET_TYPE_ISO14443_3B] = 1000; //NfcB
    mTechnologyTimeoutsTable [TARGET_TYPE_ISO14443_4] = 309; //ISO-DEP
    mTechnologyTimeoutsTable [TARGET_TYPE_FELICA] = 255; //Felica
    mTechnologyTimeoutsTable [TARGET_TYPE_ISO15693] = 1000;//NfcV
    mTechnologyTimeoutsTable [TARGET_TYPE_NDEF] = 1000;
    mTechnologyTimeoutsTable [TARGET_TYPE_NDEF_FORMATABLE] = 1000;
    mTechnologyTimeoutsTable [TARGET_TYPE_MIFARE_CLASSIC] = 618; //MifareClassic
    mTechnologyTimeoutsTable [TARGET_TYPE_MIFARE_UL] = 618; //MifareUltralight
    mTechnologyTimeoutsTable [TARGET_TYPE_KOVIO_BARCODE] = 1000; //NfcBarcode
}


/*******************************************************************************
**
** Function:        getTransceiveTimeout
**
** Description:     Get the timeout value for one technology.
**                  techId: one of the values in TARGET_TYPE_* defined in NfcJniUtil.h
**
** Returns:         Timeout value in millisecond.
**
*******************************************************************************/
int NfcTag::getTransceiveTimeout (int techId)
{
    static const char fn [] = "NfcTag::getTransceiveTimeout";
    int retval = 1000;
    if ((techId >= 0) && (techId < (int) mTechnologyTimeoutsTable.size()))
        retval = mTechnologyTimeoutsTable [techId];
    else
        ALOGE ("%s: invalid tech=%d", fn, techId);
    return retval;
}


/*******************************************************************************
**
** Function:        setTransceiveTimeout
**
** Description:     Set the timeout value for one technology.
**                  techId: one of the values in TARGET_TYPE_* defined in NfcJniUtil.h
**                  timeout: timeout value in millisecond.
**
** Returns:         Timeout value.
**
*******************************************************************************/
void setTransceiveTimeout (int techId, int timeout);
void NfcTag::setTransceiveTimeout (int techId, int timeout)
{
    static const char fn [] = "NfcTag::setTransceiveTimeout";
    if ((techId >= 0) && (techId < (int) mTechnologyTimeoutsTable.size()))
        mTechnologyTimeoutsTable [techId] = timeout;
    else
        ALOGE ("%s: invalid tech=%d", fn, techId);
}
