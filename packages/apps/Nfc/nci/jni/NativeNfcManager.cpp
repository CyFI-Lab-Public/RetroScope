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

#include <semaphore.h>
#include <errno.h>
#include "OverrideLog.h"
#include "NfcJniUtil.h"
#include "NfcAdaptation.h"
#include "SyncEvent.h"
#include "PeerToPeer.h"
#include "SecureElement.h"
#include "RoutingManager.h"
#include "NfcTag.h"
#include "config.h"
#include "PowerSwitch.h"
#include "JavaClassConstants.h"
#include "Pn544Interop.h"
#include <ScopedLocalRef.h>
#include <ScopedUtfChars.h>
#include <ScopedPrimitiveArray.h>

extern "C"
{
    #include "nfa_api.h"
    #include "nfa_p2p_api.h"
    #include "rw_api.h"
    #include "nfa_ee_api.h"
    #include "nfc_brcm_defs.h"
    #include "ce_api.h"
}

extern UINT8 *p_nfa_dm_start_up_cfg;
extern const UINT8 nfca_version_string [];
extern const UINT8 nfa_version_string [];
extern tNFA_DM_DISC_FREQ_CFG* p_nfa_dm_rf_disc_freq_cfg; //defined in stack
namespace android
{
    extern bool gIsTagDeactivating;
    extern bool gIsSelectingRfInterface;
    extern void nativeNfcTag_doTransceiveStatus (uint8_t * buf, uint32_t buflen);
    extern void nativeNfcTag_notifyRfTimeout ();
    extern void nativeNfcTag_doConnectStatus (jboolean is_connect_ok);
    extern void nativeNfcTag_doDeactivateStatus (int status);
    extern void nativeNfcTag_doWriteStatus (jboolean is_write_ok);
    extern void nativeNfcTag_doCheckNdefResult (tNFA_STATUS status, uint32_t max_size, uint32_t current_size, uint8_t flags);
    extern void nativeNfcTag_doMakeReadonlyResult (tNFA_STATUS status);
    extern void nativeNfcTag_doPresenceCheckResult (tNFA_STATUS status);
    extern void nativeNfcTag_formatStatus (bool is_ok);
    extern void nativeNfcTag_resetPresenceCheck ();
    extern void nativeNfcTag_doReadCompleted (tNFA_STATUS status);
    extern void nativeNfcTag_abortWaits ();
    extern void nativeLlcpConnectionlessSocket_abortWait ();
    extern void nativeNfcTag_registerNdefTypeHandler ();
    extern void nativeLlcpConnectionlessSocket_receiveData (uint8_t* data, uint32_t len, uint32_t remote_sap);
}


/*****************************************************************************
**
** public variables and functions
**
*****************************************************************************/
bool                        gActivated = false;
SyncEvent                   gDeactivatedEvent;

namespace android
{
    jmethodID               gCachedNfcManagerNotifyNdefMessageListeners;
    jmethodID               gCachedNfcManagerNotifyTransactionListeners;
    jmethodID               gCachedNfcManagerNotifyLlcpLinkActivation;
    jmethodID               gCachedNfcManagerNotifyLlcpLinkDeactivated;
    jmethodID               gCachedNfcManagerNotifyLlcpFirstPacketReceived;
    jmethodID               gCachedNfcManagerNotifySeFieldActivated;
    jmethodID               gCachedNfcManagerNotifySeFieldDeactivated;
    jmethodID               gCachedNfcManagerNotifySeListenActivated;
    jmethodID               gCachedNfcManagerNotifySeListenDeactivated;
    jmethodID               gCachedNfcManagerNotifyHostEmuActivated;
    jmethodID               gCachedNfcManagerNotifyHostEmuData;
    jmethodID               gCachedNfcManagerNotifyHostEmuDeactivated;
    const char*             gNativeP2pDeviceClassName                 = "com/android/nfc/dhimpl/NativeP2pDevice";
    const char*             gNativeLlcpServiceSocketClassName         = "com/android/nfc/dhimpl/NativeLlcpServiceSocket";
    const char*             gNativeLlcpConnectionlessSocketClassName  = "com/android/nfc/dhimpl/NativeLlcpConnectionlessSocket";
    const char*             gNativeLlcpSocketClassName                = "com/android/nfc/dhimpl/NativeLlcpSocket";
    const char*             gNativeNfcTagClassName                    = "com/android/nfc/dhimpl/NativeNfcTag";
    const char*             gNativeNfcManagerClassName                = "com/android/nfc/dhimpl/NativeNfcManager";
    const char*             gNativeNfcSecureElementClassName          = "com/android/nfc/dhimpl/NativeNfcSecureElement";
    void                    doStartupConfig ();
    void                    startStopPolling (bool isStartPolling);
    void                    startRfDiscovery (bool isStart);
    void                    setUiccIdleTimeout (bool enable);
    void                    restartPollingWithTechMask(int mask);
}


/*****************************************************************************
**
** private variables and functions
**
*****************************************************************************/
namespace android
{
static jint                 sLastError = ERROR_BUFFER_TOO_SMALL;
static jmethodID            sCachedNfcManagerNotifySeApduReceived;
static jmethodID            sCachedNfcManagerNotifySeMifareAccess;
static jmethodID            sCachedNfcManagerNotifySeEmvCardRemoval;
static jmethodID            sCachedNfcManagerNotifyTargetDeselected;
static SyncEvent            sNfaEnableEvent;  //event for NFA_Enable()
static SyncEvent            sNfaDisableEvent;  //event for NFA_Disable()
static SyncEvent            sNfaEnableDisablePollingEvent;  //event for NFA_EnablePolling(), NFA_DisablePolling()
static SyncEvent            sNfaSetConfigEvent;  // event for Set_Config....
static SyncEvent            sNfaGetConfigEvent;  // event for Get_Config....
static bool                 sIsNfaEnabled = false;
static bool                 sDiscoveryEnabled = false;  //is polling for tag?
static bool                 sIsDisabling = false;
static bool                 sRfEnabled = false; // whether RF discovery is enabled
static bool                 sSeRfActive = false;  // whether RF with SE is likely active
static bool                 sReaderModeEnabled = false; // whether we're only reading tags, not allowing P2p/card emu
static bool                 sP2pActive = false; // whether p2p was last active
static bool                 sAbortConnlessWait = false;
static bool                 sIsSecElemSelected = false;  //has NFC service selected a sec elem
#define CONFIG_UPDATE_TECH_MASK     (1 << 1)
#define DEFAULT_TECH_MASK           (NFA_TECHNOLOGY_MASK_A \
                                     | NFA_TECHNOLOGY_MASK_B \
                                     | NFA_TECHNOLOGY_MASK_F \
                                     | NFA_TECHNOLOGY_MASK_ISO15693 \
                                     | NFA_TECHNOLOGY_MASK_B_PRIME \
                                     | NFA_TECHNOLOGY_MASK_A_ACTIVE \
                                     | NFA_TECHNOLOGY_MASK_F_ACTIVE \
                                     | NFA_TECHNOLOGY_MASK_KOVIO)
#define DEFAULT_DISCOVERY_DURATION       500
#define READER_MODE_DISCOVERY_DURATION   200

static void nfaConnectionCallback (UINT8 event, tNFA_CONN_EVT_DATA *eventData);
static void nfaDeviceManagementCallback (UINT8 event, tNFA_DM_CBACK_DATA *eventData);
static bool isPeerToPeer (tNFA_ACTIVATED& activated);
static bool isListenMode(tNFA_ACTIVATED& activated);

static UINT16 sCurrentConfigLen;
static UINT8 sConfig[256];
static UINT8 sLongGuardTime[] = { 0x00, 0x20 };
static UINT8 sDefaultGuardTime[] = { 0x00, 0x11 };

/////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////


/*******************************************************************************
**
** Function:        getNative
**
** Description:     Get native data
**
** Returns:         Native data structure.
**
*******************************************************************************/
nfc_jni_native_data *getNative (JNIEnv* e, jobject o)
{
    static struct nfc_jni_native_data *sCachedNat = NULL;
    if (e)
    {
        sCachedNat = nfc_jni_get_nat(e, o);
    }
    return sCachedNat;
}


/*******************************************************************************
**
** Function:        handleRfDiscoveryEvent
**
** Description:     Handle RF-discovery events from the stack.
**                  discoveredDevice: Discovered device.
**
** Returns:         None
**
*******************************************************************************/
static void handleRfDiscoveryEvent (tNFC_RESULT_DEVT* discoveredDevice)
{
    if (discoveredDevice->more)
    {
        //there is more discovery notification coming
        return;
    }

    bool isP2p = NfcTag::getInstance ().isP2pDiscovered ();
    if (!sReaderModeEnabled && isP2p)
    {
        //select the peer that supports P2P
        NfcTag::getInstance ().selectP2p();
    }
    else
    {
        //select the first of multiple tags that is discovered
        NfcTag::getInstance ().selectFirstTag();
    }
}


/*******************************************************************************
**
** Function:        nfaConnectionCallback
**
** Description:     Receive connection-related events from stack.
**                  connEvent: Event code.
**                  eventData: Event data.
**
** Returns:         None
**
*******************************************************************************/
static void nfaConnectionCallback (UINT8 connEvent, tNFA_CONN_EVT_DATA* eventData)
{
    tNFA_STATUS status = NFA_STATUS_FAILED;
    ALOGD("%s: event= %u", __FUNCTION__, connEvent);

    switch (connEvent)
    {
    case NFA_POLL_ENABLED_EVT: // whether polling successfully started
        {
            ALOGD("%s: NFA_POLL_ENABLED_EVT: status = %u", __FUNCTION__, eventData->status);

            SyncEventGuard guard (sNfaEnableDisablePollingEvent);
            sNfaEnableDisablePollingEvent.notifyOne ();
        }
        break;

    case NFA_POLL_DISABLED_EVT: // Listening/Polling stopped
        {
            ALOGD("%s: NFA_POLL_DISABLED_EVT: status = %u", __FUNCTION__, eventData->status);

            SyncEventGuard guard (sNfaEnableDisablePollingEvent);
            sNfaEnableDisablePollingEvent.notifyOne ();
        }
        break;

    case NFA_RF_DISCOVERY_STARTED_EVT: // RF Discovery started
        {
            ALOGD("%s: NFA_RF_DISCOVERY_STARTED_EVT: status = %u", __FUNCTION__, eventData->status);

            SyncEventGuard guard (sNfaEnableDisablePollingEvent);
            sNfaEnableDisablePollingEvent.notifyOne ();
        }
        break;

    case NFA_RF_DISCOVERY_STOPPED_EVT: // RF Discovery stopped event
        {
            ALOGD("%s: NFA_RF_DISCOVERY_STOPPED_EVT: status = %u", __FUNCTION__, eventData->status);

            SyncEventGuard guard (sNfaEnableDisablePollingEvent);
            sNfaEnableDisablePollingEvent.notifyOne ();
        }
        break;

    case NFA_DISC_RESULT_EVT: // NFC link/protocol discovery notificaiton
        status = eventData->disc_result.status;
        ALOGD("%s: NFA_DISC_RESULT_EVT: status = %d", __FUNCTION__, status);
        if (status != NFA_STATUS_OK)
        {
            ALOGE("%s: NFA_DISC_RESULT_EVT error: status = %d", __FUNCTION__, status);
        }
        else
        {
            NfcTag::getInstance().connectionEventHandler(connEvent, eventData);
            handleRfDiscoveryEvent(&eventData->disc_result.discovery_ntf);
        }
        break;

    case NFA_SELECT_RESULT_EVT: // NFC link/protocol discovery select response
        ALOGD("%s: NFA_SELECT_RESULT_EVT: status = %d, gIsSelectingRfInterface = %d, sIsDisabling=%d", __FUNCTION__, eventData->status, gIsSelectingRfInterface, sIsDisabling);

        if (sIsDisabling)
            break;

        if (eventData->status != NFA_STATUS_OK)
        {
            if (gIsSelectingRfInterface)
            {
                nativeNfcTag_doConnectStatus(false);
            }

            ALOGE("%s: NFA_SELECT_RESULT_EVT error: status = %d", __FUNCTION__, eventData->status);
            NFA_Deactivate (FALSE);
        }
        break;

    case NFA_DEACTIVATE_FAIL_EVT:
        ALOGD("%s: NFA_DEACTIVATE_FAIL_EVT: status = %d", __FUNCTION__, eventData->status);
        break;

    case NFA_ACTIVATED_EVT: // NFC link/protocol activated
        ALOGD("%s: NFA_ACTIVATED_EVT: gIsSelectingRfInterface=%d, sIsDisabling=%d", __FUNCTION__, gIsSelectingRfInterface, sIsDisabling);
        NfcTag::getInstance().setActive(true);
        if (sIsDisabling || !sIsNfaEnabled)
            break;
        gActivated = true;

        NfcTag::getInstance().setActivationState ();
        if (gIsSelectingRfInterface)
        {
            nativeNfcTag_doConnectStatus(true);
            break;
        }

        nativeNfcTag_resetPresenceCheck();
        if (isPeerToPeer(eventData->activated))
        {
            if (sReaderModeEnabled)
            {
                ALOGD("%s: ignoring peer target in reader mode.", __FUNCTION__);
                NFA_Deactivate (FALSE);
                break;
            }
            sP2pActive = true;
            ALOGD("%s: NFA_ACTIVATED_EVT; is p2p", __FUNCTION__);
            // Disable RF field events in case of p2p
            UINT8  nfa_disable_rf_events[] = { 0x00 };
            ALOGD ("%s: Disabling RF field events", __FUNCTION__);
            status = NFA_SetConfig(NCI_PARAM_ID_RF_FIELD_INFO, sizeof(nfa_disable_rf_events),
                    &nfa_disable_rf_events[0]);
            if (status == NFA_STATUS_OK) {
                ALOGD ("%s: Disabled RF field events", __FUNCTION__);
            } else {
                ALOGE ("%s: Failed to disable RF field events", __FUNCTION__);
            }
            // For the SE, consider the field to be on while p2p is active.
            SecureElement::getInstance().notifyRfFieldEvent (true);
        }
        else if (pn544InteropIsBusy() == false)
        {
            NfcTag::getInstance().connectionEventHandler (connEvent, eventData);

            // We know it is not activating for P2P.  If it activated in
            // listen mode then it is likely for an SE transaction.
            // Send the RF Event.
            if (isListenMode(eventData->activated))
            {
                sSeRfActive = true;
                SecureElement::getInstance().notifyListenModeState (true);
            }
        }
        break;

    case NFA_DEACTIVATED_EVT: // NFC link/protocol deactivated
        ALOGD("%s: NFA_DEACTIVATED_EVT   Type: %u, gIsTagDeactivating: %d", __FUNCTION__, eventData->deactivated.type,gIsTagDeactivating);
        NfcTag::getInstance().setDeactivationState (eventData->deactivated);
        if (eventData->deactivated.type != NFA_DEACTIVATE_TYPE_SLEEP)
        {
            {
                SyncEventGuard g (gDeactivatedEvent);
                gActivated = false; //guard this variable from multi-threaded access
                gDeactivatedEvent.notifyOne ();
            }
            nativeNfcTag_resetPresenceCheck();
            NfcTag::getInstance().connectionEventHandler (connEvent, eventData);
            nativeNfcTag_abortWaits();
            NfcTag::getInstance().abort ();
        }
        else if (gIsTagDeactivating)
        {
            NfcTag::getInstance().setActive(false);
            nativeNfcTag_doDeactivateStatus(0);
        }

        // If RF is activated for what we think is a Secure Element transaction
        // and it is deactivated to either IDLE or DISCOVERY mode, notify w/event.
        if ((eventData->deactivated.type == NFA_DEACTIVATE_TYPE_IDLE)
                || (eventData->deactivated.type == NFA_DEACTIVATE_TYPE_DISCOVERY))
        {
            if (sSeRfActive) {
                sSeRfActive = false;
                if (!sIsDisabling && sIsNfaEnabled)
                    SecureElement::getInstance().notifyListenModeState (false);
            } else if (sP2pActive) {
                sP2pActive = false;
                // Make sure RF field events are re-enabled
                ALOGD("%s: NFA_DEACTIVATED_EVT; is p2p", __FUNCTION__);
                // Disable RF field events in case of p2p
                UINT8  nfa_enable_rf_events[] = { 0x01 };

                if (!sIsDisabling && sIsNfaEnabled)
                {
                    ALOGD ("%s: Enabling RF field events", __FUNCTION__);
                    status = NFA_SetConfig(NCI_PARAM_ID_RF_FIELD_INFO, sizeof(nfa_enable_rf_events),
                            &nfa_enable_rf_events[0]);
                    if (status == NFA_STATUS_OK) {
                        ALOGD ("%s: Enabled RF field events", __FUNCTION__);
                    } else {
                        ALOGE ("%s: Failed to enable RF field events", __FUNCTION__);
                    }
                    // Consider the field to be off at this point
                    SecureElement::getInstance().notifyRfFieldEvent (false);
                }
            }
        }

        break;

    case NFA_TLV_DETECT_EVT: // TLV Detection complete
        status = eventData->tlv_detect.status;
        ALOGD("%s: NFA_TLV_DETECT_EVT: status = %d, protocol = %d, num_tlvs = %d, num_bytes = %d",
             __FUNCTION__, status, eventData->tlv_detect.protocol,
             eventData->tlv_detect.num_tlvs, eventData->tlv_detect.num_bytes);
        if (status != NFA_STATUS_OK)
        {
            ALOGE("%s: NFA_TLV_DETECT_EVT error: status = %d", __FUNCTION__, status);
        }
        break;

    case NFA_NDEF_DETECT_EVT: // NDEF Detection complete;
        //if status is failure, it means the tag does not contain any or valid NDEF data;
        //pass the failure status to the NFC Service;
        status = eventData->ndef_detect.status;
        ALOGD("%s: NFA_NDEF_DETECT_EVT: status = 0x%X, protocol = %u, "
             "max_size = %lu, cur_size = %lu, flags = 0x%X", __FUNCTION__,
             status,
             eventData->ndef_detect.protocol, eventData->ndef_detect.max_size,
             eventData->ndef_detect.cur_size, eventData->ndef_detect.flags);
        NfcTag::getInstance().connectionEventHandler (connEvent, eventData);
        nativeNfcTag_doCheckNdefResult(status,
            eventData->ndef_detect.max_size, eventData->ndef_detect.cur_size,
            eventData->ndef_detect.flags);
        break;

    case NFA_DATA_EVT: // Data message received (for non-NDEF reads)
        ALOGD("%s: NFA_DATA_EVT:  len = %d", __FUNCTION__, eventData->data.len);
        nativeNfcTag_doTransceiveStatus(eventData->data.p_data,eventData->data.len);
        break;
    case NFA_RW_INTF_ERROR_EVT:
        ALOGD("%s: NFC_RW_INTF_ERROR_EVT", __FUNCTION__);
        nativeNfcTag_notifyRfTimeout();
        break;
    case NFA_SELECT_CPLT_EVT: // Select completed
        status = eventData->status;
        ALOGD("%s: NFA_SELECT_CPLT_EVT: status = %d", __FUNCTION__, status);
        if (status != NFA_STATUS_OK)
        {
            ALOGE("%s: NFA_SELECT_CPLT_EVT error: status = %d", __FUNCTION__, status);
        }
        break;

    case NFA_READ_CPLT_EVT: // NDEF-read or tag-specific-read completed
        ALOGD("%s: NFA_READ_CPLT_EVT: status = 0x%X", __FUNCTION__, eventData->status);
        nativeNfcTag_doReadCompleted (eventData->status);
        NfcTag::getInstance().connectionEventHandler (connEvent, eventData);
        break;

    case NFA_WRITE_CPLT_EVT: // Write completed
        ALOGD("%s: NFA_WRITE_CPLT_EVT: status = %d", __FUNCTION__, eventData->status);
        nativeNfcTag_doWriteStatus (eventData->status == NFA_STATUS_OK);
        break;

    case NFA_SET_TAG_RO_EVT: // Tag set as Read only
        ALOGD("%s: NFA_SET_TAG_RO_EVT: status = %d", __FUNCTION__, eventData->status);
        nativeNfcTag_doMakeReadonlyResult(eventData->status);
        break;

    case NFA_CE_NDEF_WRITE_START_EVT: // NDEF write started
        ALOGD("%s: NFA_CE_NDEF_WRITE_START_EVT: status: %d", __FUNCTION__, eventData->status);

        if (eventData->status != NFA_STATUS_OK)
            ALOGE("%s: NFA_CE_NDEF_WRITE_START_EVT error: status = %d", __FUNCTION__, eventData->status);
        break;

    case NFA_CE_NDEF_WRITE_CPLT_EVT: // NDEF write completed
        ALOGD("%s: FA_CE_NDEF_WRITE_CPLT_EVT: len = %lu", __FUNCTION__, eventData->ndef_write_cplt.len);
        break;

    case NFA_LLCP_ACTIVATED_EVT: // LLCP link is activated
        ALOGD("%s: NFA_LLCP_ACTIVATED_EVT: is_initiator: %d  remote_wks: %d, remote_lsc: %d, remote_link_miu: %d, local_link_miu: %d",
             __FUNCTION__,
             eventData->llcp_activated.is_initiator,
             eventData->llcp_activated.remote_wks,
             eventData->llcp_activated.remote_lsc,
             eventData->llcp_activated.remote_link_miu,
             eventData->llcp_activated.local_link_miu);

        PeerToPeer::getInstance().llcpActivatedHandler (getNative(0, 0), eventData->llcp_activated);
        break;

    case NFA_LLCP_DEACTIVATED_EVT: // LLCP link is deactivated
        ALOGD("%s: NFA_LLCP_DEACTIVATED_EVT", __FUNCTION__);
        PeerToPeer::getInstance().llcpDeactivatedHandler (getNative(0, 0), eventData->llcp_deactivated);
        break;
    case NFA_LLCP_FIRST_PACKET_RECEIVED_EVT: // Received first packet over llcp
        ALOGD("%s: NFA_LLCP_FIRST_PACKET_RECEIVED_EVT", __FUNCTION__);
        PeerToPeer::getInstance().llcpFirstPacketHandler (getNative(0, 0));
        break;
    case NFA_PRESENCE_CHECK_EVT:
        ALOGD("%s: NFA_PRESENCE_CHECK_EVT", __FUNCTION__);
        nativeNfcTag_doPresenceCheckResult (eventData->status);
        break;
    case NFA_FORMAT_CPLT_EVT:
        ALOGD("%s: NFA_FORMAT_CPLT_EVT: status=0x%X", __FUNCTION__, eventData->status);
        nativeNfcTag_formatStatus (eventData->status == NFA_STATUS_OK);
        break;

    case NFA_I93_CMD_CPLT_EVT:
        ALOGD("%s: NFA_I93_CMD_CPLT_EVT: status=0x%X", __FUNCTION__, eventData->status);
        break;

    case NFA_CE_UICC_LISTEN_CONFIGURED_EVT :
        ALOGD("%s: NFA_CE_UICC_LISTEN_CONFIGURED_EVT : status=0x%X", __FUNCTION__, eventData->status);
        SecureElement::getInstance().connectionEventHandler (connEvent, eventData);
        break;

    case NFA_SET_P2P_LISTEN_TECH_EVT:
        ALOGD("%s: NFA_SET_P2P_LISTEN_TECH_EVT", __FUNCTION__);
        PeerToPeer::getInstance().connectionEventHandler (connEvent, eventData);
        break;

    default:
        ALOGE("%s: unknown event ????", __FUNCTION__);
        break;
    }
}


/*******************************************************************************
**
** Function:        nfcManager_initNativeStruc
**
** Description:     Initialize variables.
**                  e: JVM environment.
**                  o: Java object.
**
** Returns:         True if ok.
**
*******************************************************************************/
static jboolean nfcManager_initNativeStruc (JNIEnv* e, jobject o)
{
    ALOGD ("%s: enter", __FUNCTION__);

    nfc_jni_native_data* nat = (nfc_jni_native_data*)malloc(sizeof(struct nfc_jni_native_data));
    if (nat == NULL)
    {
        ALOGE ("%s: fail allocate native data", __FUNCTION__);
        return JNI_FALSE;
    }

    memset (nat, 0, sizeof(*nat));
    e->GetJavaVM(&(nat->vm));
    nat->env_version = e->GetVersion();
    nat->manager = e->NewGlobalRef(o);

    ScopedLocalRef<jclass> cls(e, e->GetObjectClass(o));
    jfieldID f = e->GetFieldID(cls.get(), "mNative", "I");
    e->SetIntField(o, f, (jint)nat);

    /* Initialize native cached references */
    gCachedNfcManagerNotifyNdefMessageListeners = e->GetMethodID(cls.get(),
            "notifyNdefMessageListeners", "(Lcom/android/nfc/dhimpl/NativeNfcTag;)V");
    gCachedNfcManagerNotifyTransactionListeners = e->GetMethodID(cls.get(),
            "notifyTransactionListeners", "([B)V");
    gCachedNfcManagerNotifyLlcpLinkActivation = e->GetMethodID(cls.get(),
            "notifyLlcpLinkActivation", "(Lcom/android/nfc/dhimpl/NativeP2pDevice;)V");
    gCachedNfcManagerNotifyLlcpLinkDeactivated = e->GetMethodID(cls.get(),
            "notifyLlcpLinkDeactivated", "(Lcom/android/nfc/dhimpl/NativeP2pDevice;)V");
    gCachedNfcManagerNotifyLlcpFirstPacketReceived = e->GetMethodID(cls.get(),
            "notifyLlcpLinkFirstPacketReceived", "(Lcom/android/nfc/dhimpl/NativeP2pDevice;)V");
    sCachedNfcManagerNotifyTargetDeselected = e->GetMethodID(cls.get(),
            "notifyTargetDeselected","()V");
    gCachedNfcManagerNotifySeFieldActivated = e->GetMethodID(cls.get(),
            "notifySeFieldActivated", "()V");
    gCachedNfcManagerNotifySeFieldDeactivated = e->GetMethodID(cls.get(),
            "notifySeFieldDeactivated", "()V");
    gCachedNfcManagerNotifySeListenActivated = e->GetMethodID(cls.get(),
            "notifySeListenActivated", "()V");
    gCachedNfcManagerNotifySeListenDeactivated = e->GetMethodID(cls.get(),
            "notifySeListenDeactivated", "()V");

    gCachedNfcManagerNotifyHostEmuActivated = e->GetMethodID(cls.get(),
            "notifyHostEmuActivated", "()V");

    gCachedNfcManagerNotifyHostEmuData = e->GetMethodID(cls.get(),
            "notifyHostEmuData", "([B)V");

    gCachedNfcManagerNotifyHostEmuDeactivated = e->GetMethodID(cls.get(),
            "notifyHostEmuDeactivated", "()V");

    sCachedNfcManagerNotifySeApduReceived = e->GetMethodID(cls.get(),
            "notifySeApduReceived", "([B)V");

    sCachedNfcManagerNotifySeMifareAccess = e->GetMethodID(cls.get(),
            "notifySeMifareAccess", "([B)V");

    sCachedNfcManagerNotifySeEmvCardRemoval =  e->GetMethodID(cls.get(),
            "notifySeEmvCardRemoval", "()V");

    if (nfc_jni_cache_object(e, gNativeNfcTagClassName, &(nat->cached_NfcTag)) == -1)
    {
        ALOGE ("%s: fail cache NativeNfcTag", __FUNCTION__);
        return JNI_FALSE;
    }

    if (nfc_jni_cache_object(e, gNativeP2pDeviceClassName, &(nat->cached_P2pDevice)) == -1)
    {
        ALOGE ("%s: fail cache NativeP2pDevice", __FUNCTION__);
        return JNI_FALSE;
    }

    ALOGD ("%s: exit", __FUNCTION__);
    return JNI_TRUE;
}


/*******************************************************************************
**
** Function:        nfaDeviceManagementCallback
**
** Description:     Receive device management events from stack.
**                  dmEvent: Device-management event ID.
**                  eventData: Data associated with event ID.
**
** Returns:         None
**
*******************************************************************************/
void nfaDeviceManagementCallback (UINT8 dmEvent, tNFA_DM_CBACK_DATA* eventData)
{
    ALOGD ("%s: enter; event=0x%X", __FUNCTION__, dmEvent);

    switch (dmEvent)
    {
    case NFA_DM_ENABLE_EVT: /* Result of NFA_Enable */
        {
            SyncEventGuard guard (sNfaEnableEvent);
            ALOGD ("%s: NFA_DM_ENABLE_EVT; status=0x%X",
                    __FUNCTION__, eventData->status);
            sIsNfaEnabled = eventData->status == NFA_STATUS_OK;
            sIsDisabling = false;
            sNfaEnableEvent.notifyOne ();
        }
        break;

    case NFA_DM_DISABLE_EVT: /* Result of NFA_Disable */
        {
            SyncEventGuard guard (sNfaDisableEvent);
            ALOGD ("%s: NFA_DM_DISABLE_EVT", __FUNCTION__);
            sIsNfaEnabled = false;
            sIsDisabling = false;
            sNfaDisableEvent.notifyOne ();
        }
        break;

    case NFA_DM_SET_CONFIG_EVT: //result of NFA_SetConfig
        ALOGD ("%s: NFA_DM_SET_CONFIG_EVT", __FUNCTION__);
        {
            SyncEventGuard guard (sNfaSetConfigEvent);
            sNfaSetConfigEvent.notifyOne();
        }
        break;

    case NFA_DM_GET_CONFIG_EVT: /* Result of NFA_GetConfig */
        ALOGD ("%s: NFA_DM_GET_CONFIG_EVT", __FUNCTION__);
        {
            SyncEventGuard guard (sNfaGetConfigEvent);
            if (eventData->status == NFA_STATUS_OK &&
                    eventData->get_config.tlv_size <= sizeof(sConfig))
            {
                sCurrentConfigLen = eventData->get_config.tlv_size;
                memcpy(sConfig, eventData->get_config.param_tlvs, eventData->get_config.tlv_size);
            }
            else
            {
                ALOGE("%s: NFA_DM_GET_CONFIG failed", __FUNCTION__);
                sCurrentConfigLen = 0;
            }
            sNfaGetConfigEvent.notifyOne();
        }
        break;

    case NFA_DM_RF_FIELD_EVT:
        ALOGD ("%s: NFA_DM_RF_FIELD_EVT; status=0x%X; field status=%u", __FUNCTION__,
              eventData->rf_field.status, eventData->rf_field.rf_field_status);
        if (sIsDisabling || !sIsNfaEnabled)
            break;

        if (!sP2pActive && eventData->rf_field.status == NFA_STATUS_OK)
            SecureElement::getInstance().notifyRfFieldEvent (
                    eventData->rf_field.rf_field_status == NFA_DM_RF_FIELD_ON);
        break;

    case NFA_DM_NFCC_TRANSPORT_ERR_EVT:
    case NFA_DM_NFCC_TIMEOUT_EVT:
        {
            if (dmEvent == NFA_DM_NFCC_TIMEOUT_EVT)
                ALOGE ("%s: NFA_DM_NFCC_TIMEOUT_EVT; abort", __FUNCTION__);
            else if (dmEvent == NFA_DM_NFCC_TRANSPORT_ERR_EVT)
                ALOGE ("%s: NFA_DM_NFCC_TRANSPORT_ERR_EVT; abort", __FUNCTION__);

            nativeNfcTag_abortWaits();
            NfcTag::getInstance().abort ();
            sAbortConnlessWait = true;
            nativeLlcpConnectionlessSocket_abortWait();
            {
                ALOGD ("%s: aborting  sNfaEnableDisablePollingEvent", __FUNCTION__);
                SyncEventGuard guard (sNfaEnableDisablePollingEvent);
                sNfaEnableDisablePollingEvent.notifyOne();
            }
            {
                ALOGD ("%s: aborting  sNfaEnableEvent", __FUNCTION__);
                SyncEventGuard guard (sNfaEnableEvent);
                sNfaEnableEvent.notifyOne();
            }
            {
                ALOGD ("%s: aborting  sNfaDisableEvent", __FUNCTION__);
                SyncEventGuard guard (sNfaDisableEvent);
                sNfaDisableEvent.notifyOne();
            }
            sDiscoveryEnabled = false;
            PowerSwitch::getInstance ().abort ();

            if (!sIsDisabling && sIsNfaEnabled)
            {
                NFA_Disable(FALSE);
                sIsDisabling = true;
            }
            else
            {
                sIsNfaEnabled = false;
                sIsDisabling = false;
            }
            PowerSwitch::getInstance ().initialize (PowerSwitch::UNKNOWN_LEVEL);
            ALOGE ("%s: crash NFC service", __FUNCTION__);
            //////////////////////////////////////////////
            //crash the NFC service process so it can restart automatically
            abort ();
            //////////////////////////////////////////////
        }
        break;

    case NFA_DM_PWR_MODE_CHANGE_EVT:
        PowerSwitch::getInstance ().deviceManagementCallback (dmEvent, eventData);
        break;

    default:
        ALOGD ("%s: unhandled event", __FUNCTION__);
        break;
    }
}

/*******************************************************************************
**
** Function:        nfcManager_sendRawFrame
**
** Description:     Send a raw frame.
**                  e: JVM environment.
**                  o: Java object.
**
** Returns:         True if ok.
**
*******************************************************************************/
static jboolean nfcManager_sendRawFrame (JNIEnv* e, jobject, jbyteArray data)
{
    ScopedByteArrayRO bytes(e, data);
    uint8_t* buf = const_cast<uint8_t*>(reinterpret_cast<const uint8_t*>(&bytes[0]));
    size_t bufLen = bytes.size();
    tNFA_STATUS status = NFA_SendRawFrame (buf, bufLen, 0);

    return (status == NFA_STATUS_OK);
}

/*******************************************************************************
**
** Function:        nfcManager_routeAid
**
** Description:     Route an AID to an EE
**                  e: JVM environment.
**                  o: Java object.
**
** Returns:         True if ok.
**
*******************************************************************************/
static jboolean nfcManager_routeAid (JNIEnv* e, jobject, jbyteArray aid, jint route)
{
    ScopedByteArrayRO bytes(e, aid);
    uint8_t* buf = const_cast<uint8_t*>(reinterpret_cast<const uint8_t*>(&bytes[0]));
    size_t bufLen = bytes.size();
    bool result = RoutingManager::getInstance().addAidRouting(buf, bufLen, route);
    return result;
}

/*******************************************************************************
**
** Function:        nfcManager_unrouteAid
**
** Description:     Remove a AID routing
**                  e: JVM environment.
**                  o: Java object.
**
** Returns:         True if ok.
**
*******************************************************************************/
static jboolean nfcManager_unrouteAid (JNIEnv* e, jobject, jbyteArray aid)
{
    ScopedByteArrayRO bytes(e, aid);
    uint8_t* buf = const_cast<uint8_t*>(reinterpret_cast<const uint8_t*>(&bytes[0]));
    size_t bufLen = bytes.size();
    bool result = RoutingManager::getInstance().removeAidRouting(buf, bufLen);
    return result;
}

/*******************************************************************************
**
** Function:        nfcManager_doInitialize
**
** Description:     Turn on NFC.
**                  e: JVM environment.
**                  o: Java object.
**
** Returns:         True if ok.
**
*******************************************************************************/
static jboolean nfcManager_doInitialize (JNIEnv* e, jobject o)
{
    ALOGD ("%s: enter; ver=%s nfa=%s NCI_VERSION=0x%02X",
        __FUNCTION__, nfca_version_string, nfa_version_string, NCI_VERSION);
    tNFA_STATUS stat = NFA_STATUS_OK;

    if (sIsNfaEnabled)
    {
        ALOGD ("%s: already enabled", __FUNCTION__);
        goto TheEnd;
    }

    PowerSwitch::getInstance ().initialize (PowerSwitch::FULL_POWER);

    {
        unsigned long num = 0;

        NfcAdaptation& theInstance = NfcAdaptation::GetInstance();
        theInstance.Initialize(); //start GKI, NCI task, NFC task

        {
            SyncEventGuard guard (sNfaEnableEvent);
            tHAL_NFC_ENTRY* halFuncEntries = theInstance.GetHalEntryFuncs ();

            NFA_Init (halFuncEntries);

            stat = NFA_Enable (nfaDeviceManagementCallback, nfaConnectionCallback);
            if (stat == NFA_STATUS_OK)
            {
                num = initializeGlobalAppLogLevel ();
                CE_SetTraceLevel (num);
                LLCP_SetTraceLevel (num);
                NFC_SetTraceLevel (num);
                RW_SetTraceLevel (num);
                NFA_SetTraceLevel (num);
                NFA_P2pSetTraceLevel (num);
                sNfaEnableEvent.wait(); //wait for NFA command to finish
            }
        }

        if (stat == NFA_STATUS_OK)
        {
            //sIsNfaEnabled indicates whether stack started successfully
            if (sIsNfaEnabled)
            {
                SecureElement::getInstance().initialize (getNative(e, o));
                RoutingManager::getInstance().initialize(getNative(e, o));
                nativeNfcTag_registerNdefTypeHandler ();
                NfcTag::getInstance().initialize (getNative(e, o));
                PeerToPeer::getInstance().initialize ();
                PeerToPeer::getInstance().handleNfcOnOff (true);

                /////////////////////////////////////////////////////////////////////////////////
                // Add extra configuration here (work-arounds, etc.)

                struct nfc_jni_native_data *nat = getNative(e, o);

                if ( nat )
                {
                    if (GetNumValue(NAME_POLLING_TECH_MASK, &num, sizeof(num)))
                        nat->tech_mask = num;
                    else
                        nat->tech_mask = DEFAULT_TECH_MASK;
                    ALOGD ("%s: tag polling tech mask=0x%X", __FUNCTION__, nat->tech_mask);
                }

                // if this value exists, set polling interval.
                if (GetNumValue(NAME_NFA_DM_DISC_DURATION_POLL, &num, sizeof(num)))
                    nat->discovery_duration = num;
                else
                    nat->discovery_duration = DEFAULT_DISCOVERY_DURATION;

                NFA_SetRfDiscoveryDuration(nat->discovery_duration);

                // Do custom NFCA startup configuration.
                doStartupConfig();
                goto TheEnd;
            }
        }

        ALOGE ("%s: fail nfa enable; error=0x%X", __FUNCTION__, stat);

        if (sIsNfaEnabled)
            stat = NFA_Disable (FALSE /* ungraceful */);

        theInstance.Finalize();
    }

TheEnd:
    if (sIsNfaEnabled)
        PowerSwitch::getInstance ().setLevel (PowerSwitch::LOW_POWER);
    ALOGD ("%s: exit", __FUNCTION__);
    return sIsNfaEnabled ? JNI_TRUE : JNI_FALSE;
}


/*******************************************************************************
**
** Function:        nfcManager_enableDiscovery
**
** Description:     Start polling and listening for devices.
**                  e: JVM environment.
**                  o: Java object.
**                  mode: Not used.
**
** Returns:         None
**
*******************************************************************************/
static void nfcManager_enableDiscovery (JNIEnv* e, jobject o)
{
    tNFA_TECHNOLOGY_MASK tech_mask = DEFAULT_TECH_MASK;
    struct nfc_jni_native_data *nat = getNative(e, o);

    if (nat)
        tech_mask = (tNFA_TECHNOLOGY_MASK)nat->tech_mask;

    ALOGD ("%s: enter; tech_mask = %02x", __FUNCTION__, tech_mask);

    if (sDiscoveryEnabled)
    {
        ALOGE ("%s: already polling", __FUNCTION__);
        return;
    }

    tNFA_STATUS stat = NFA_STATUS_OK;

    ALOGD ("%s: sIsSecElemSelected=%u", __FUNCTION__, sIsSecElemSelected);

    PowerSwitch::getInstance ().setLevel (PowerSwitch::FULL_POWER);

    if (sRfEnabled) {
        // Stop RF discovery to reconfigure
        startRfDiscovery(false);
    }

    {
        SyncEventGuard guard (sNfaEnableDisablePollingEvent);
        stat = NFA_EnablePolling (tech_mask);
        if (stat == NFA_STATUS_OK)
        {
            ALOGD ("%s: wait for enable event", __FUNCTION__);
            sDiscoveryEnabled = true;
            sNfaEnableDisablePollingEvent.wait (); //wait for NFA_POLL_ENABLED_EVT
            ALOGD ("%s: got enabled event", __FUNCTION__);
        }
        else
        {
            ALOGE ("%s: fail enable discovery; error=0x%X", __FUNCTION__, stat);
        }
    }

    // Start P2P listening if tag polling was enabled or the mask was 0.
    if (sDiscoveryEnabled || (tech_mask == 0))
    {
        ALOGD ("%s: Enable p2pListening", __FUNCTION__);
        PeerToPeer::getInstance().enableP2pListening (true);
    }

    // Actually start discovery.
    startRfDiscovery (true);

    PowerSwitch::getInstance ().setModeOn (PowerSwitch::DISCOVERY);

    ALOGD ("%s: exit", __FUNCTION__);
}


/*******************************************************************************
**
** Function:        nfcManager_disableDiscovery
**
** Description:     Stop polling and listening for devices.
**                  e: JVM environment.
**                  o: Java object.
**
** Returns:         None
**
*******************************************************************************/
void nfcManager_disableDiscovery (JNIEnv*, jobject)
{
    tNFA_STATUS status = NFA_STATUS_OK;
    ALOGD ("%s: enter;", __FUNCTION__);

    pn544InteropAbortNow ();
    if (sDiscoveryEnabled == false)
    {
        ALOGD ("%s: already disabled", __FUNCTION__);
        goto TheEnd;
    }

    // Stop RF Discovery.
    startRfDiscovery (false);

    if (sDiscoveryEnabled)
    {
        SyncEventGuard guard (sNfaEnableDisablePollingEvent);
        status = NFA_DisablePolling ();
        if (status == NFA_STATUS_OK)
        {
            sDiscoveryEnabled = false;
            sNfaEnableDisablePollingEvent.wait (); //wait for NFA_POLL_DISABLED_EVT
        }
        else
            ALOGE ("%s: Failed to disable polling; error=0x%X", __FUNCTION__, status);
    }

    PeerToPeer::getInstance().enableP2pListening (false);

    //if nothing is active after this, then tell the controller to power down
    if (! PowerSwitch::getInstance ().setModeOff (PowerSwitch::DISCOVERY))
        PowerSwitch::getInstance ().setLevel (PowerSwitch::LOW_POWER);

    // We may have had RF field notifications that did not cause
    // any activate/deactive events. For example, caused by wireless
    // charging orbs. Those may cause us to go to sleep while the last
    // field event was indicating a field. To prevent sticking in that
    // state, always reset the rf field status when we disable discovery.
    SecureElement::getInstance().resetRfFieldStatus();
TheEnd:
    ALOGD ("%s: exit", __FUNCTION__);
}

void enableDisableLongGuardTime (bool enable)
{
    // TODO
    // This is basically a work-around for an issue
    // in BCM20791B5: if a reader is configured as follows
    // 1) Only polls for NFC-A
    // 2) Cuts field between polls
    // 3) Has a short guard time (~5ms)
    // the BCM20791B5 doesn't wake up when such a reader
    // is polling it. Unfortunately the default reader
    // mode configuration on Android matches those
    // criteria. To avoid the issue, increase the guard
    // time when in reader mode.
    //
    // Proper fix is firmware patch for B5 controllers.
    SyncEventGuard guard(sNfaSetConfigEvent);
    tNFA_STATUS stat = NFA_SetConfig(NCI_PARAM_ID_T1T_RDR_ONLY, 2,
            enable ? sLongGuardTime : sDefaultGuardTime);
    if (stat == NFA_STATUS_OK)
        sNfaSetConfigEvent.wait ();
    else
        ALOGE("%s: Could not configure longer guard time", __FUNCTION__);
    return;
}

void enableDisableLptd (bool enable)
{
    // This method is *NOT* thread-safe. Right now
    // it is only called from the same thread so it's
    // not an issue.
    static bool sCheckedLptd = false;
    static bool sHasLptd = false;

    tNFA_STATUS stat = NFA_STATUS_OK;
    if (!sCheckedLptd)
    {
        sCheckedLptd = true;
        SyncEventGuard guard (sNfaGetConfigEvent);
        tNFA_PMID configParam[1] = {NCI_PARAM_ID_TAGSNIFF_CFG};
        stat = NFA_GetConfig(1, configParam);
        if (stat != NFA_STATUS_OK)
        {
            ALOGE("%s: NFA_GetConfig failed", __FUNCTION__);
            return;
        }
        sNfaGetConfigEvent.wait ();
        if (sCurrentConfigLen < 4 || sConfig[1] != NCI_PARAM_ID_TAGSNIFF_CFG) {
            ALOGE("%s: Config TLV length %d returned is too short", __FUNCTION__,
                    sCurrentConfigLen);
            return;
        }
        sHasLptd = true;
    }
    // Bail if we checked and didn't find any LPTD config before
    if (!sHasLptd) return;
    UINT8 enable_byte = enable ? 0x01 : 0x00;

    SyncEventGuard guard(sNfaSetConfigEvent);

    stat = NFA_SetConfig(NCI_PARAM_ID_TAGSNIFF_CFG, 1, &enable_byte);
    if (stat == NFA_STATUS_OK)
        sNfaSetConfigEvent.wait ();
    else
        ALOGE("%s: Could not configure LPTD feature", __FUNCTION__);
    return;
}

void setUiccIdleTimeout (bool enable)
{
    // This method is *NOT* thread-safe. Right now
    // it is only called from the same thread so it's
    // not an issue.
    tNFA_STATUS stat = NFA_STATUS_OK;
    UINT8 swp_cfg_byte0 = 0x00;
    {
        SyncEventGuard guard (sNfaGetConfigEvent);
        tNFA_PMID configParam[1] = {0xC2};
        stat = NFA_GetConfig(1, configParam);
        if (stat != NFA_STATUS_OK)
        {
            ALOGE("%s: NFA_GetConfig failed", __FUNCTION__);
            return;
        }
        sNfaGetConfigEvent.wait ();
        if (sCurrentConfigLen < 4 || sConfig[1] != 0xC2) {
            ALOGE("%s: Config TLV length %d returned is too short", __FUNCTION__,
                    sCurrentConfigLen);
            return;
        }
        swp_cfg_byte0 = sConfig[3];
    }
    SyncEventGuard guard(sNfaSetConfigEvent);
    if (enable)
        swp_cfg_byte0 |= 0x01;
    else
        swp_cfg_byte0 &= ~0x01;

    stat = NFA_SetConfig(0xC2, 1, &swp_cfg_byte0);
    if (stat == NFA_STATUS_OK)
        sNfaSetConfigEvent.wait ();
    else
        ALOGE("%s: Could not configure UICC idle timeout feature", __FUNCTION__);
    return;
}
/*******************************************************************************
**
** Function         nfc_jni_cache_object_local
**
** Description      Allocates a java object and calls it's constructor
**
** Returns          -1 on failure, 0 on success
**
*******************************************************************************/
static int nfc_jni_cache_object_local (JNIEnv *e, const char *className, jobject *cachedObj)
{
    ScopedLocalRef<jclass> cls(e, e->FindClass(className));
    if(cls.get() == NULL) {
        ALOGE ("%s: find class error", __FUNCTION__);
        return -1;
    }

    jmethodID ctor = e->GetMethodID(cls.get(), "<init>", "()V");
    jobject obj = e->NewObject(cls.get(), ctor);
    if (obj == NULL) {
       ALOGE ("%s: create object error", __FUNCTION__);
       return -1;
    }

    *cachedObj = obj;
    if (*cachedObj == NULL) {
        ALOGE ("%s: global ref error", __FUNCTION__);
        return -1;
    }
    return 0;
}


/*******************************************************************************
**
** Function:        nfcManager_doCreateLlcpServiceSocket
**
** Description:     Create a new LLCP server socket.
**                  e: JVM environment.
**                  o: Java object.
**                  nSap: Service access point.
**                  sn: Service name
**                  miu: Maximum information unit.
**                  rw: Receive window size.
**                  linearBufferLength: Max buffer size.
**
** Returns:         NativeLlcpServiceSocket Java object.
**
*******************************************************************************/
static jobject nfcManager_doCreateLlcpServiceSocket (JNIEnv* e, jobject, jint nSap, jstring sn, jint miu, jint rw, jint linearBufferLength)
{
    PeerToPeer::tJNI_HANDLE jniHandle = PeerToPeer::getInstance().getNewJniHandle ();

    ScopedUtfChars serviceName(e, sn);

    ALOGD ("%s: enter: sap=%i; name=%s; miu=%i; rw=%i; buffLen=%i", __FUNCTION__, nSap, serviceName.c_str(), miu, rw, linearBufferLength);

    /* Create new NativeLlcpServiceSocket object */
    jobject serviceSocket = NULL;
    if (nfc_jni_cache_object(e, gNativeLlcpServiceSocketClassName, &(serviceSocket)) == -1)
    {
        ALOGE ("%s: Llcp socket object creation error", __FUNCTION__);
        return NULL;
    }

    /* Get NativeLlcpServiceSocket class object */
    ScopedLocalRef<jclass> clsNativeLlcpServiceSocket(e, e->GetObjectClass(serviceSocket));
    if (e->ExceptionCheck())
    {
        e->ExceptionClear();
        ALOGE("%s: Llcp Socket get object class error", __FUNCTION__);
        return NULL;
    }

    if (!PeerToPeer::getInstance().registerServer (jniHandle, serviceName.c_str()))
    {
        ALOGE("%s: RegisterServer error", __FUNCTION__);
        return NULL;
    }

    jfieldID f;

    /* Set socket handle to be the same as the NfaHandle*/
    f = e->GetFieldID(clsNativeLlcpServiceSocket.get(), "mHandle", "I");
    e->SetIntField(serviceSocket, f, (jint) jniHandle);
    ALOGD ("%s: socket Handle = 0x%X", __FUNCTION__, jniHandle);

    /* Set socket linear buffer length */
    f = e->GetFieldID(clsNativeLlcpServiceSocket.get(), "mLocalLinearBufferLength", "I");
    e->SetIntField(serviceSocket, f,(jint)linearBufferLength);
    ALOGD ("%s: buffer length = %d", __FUNCTION__, linearBufferLength);

    /* Set socket MIU */
    f = e->GetFieldID(clsNativeLlcpServiceSocket.get(), "mLocalMiu", "I");
    e->SetIntField(serviceSocket, f,(jint)miu);
    ALOGD ("%s: MIU = %d", __FUNCTION__, miu);

    /* Set socket RW */
    f = e->GetFieldID(clsNativeLlcpServiceSocket.get(), "mLocalRw", "I");
    e->SetIntField(serviceSocket, f,(jint)rw);
    ALOGD ("%s:  RW = %d", __FUNCTION__, rw);

    sLastError = 0;
    ALOGD ("%s: exit", __FUNCTION__);
    return serviceSocket;
}


/*******************************************************************************
**
** Function:        nfcManager_doGetLastError
**
** Description:     Get the last error code.
**                  e: JVM environment.
**                  o: Java object.
**
** Returns:         Last error code.
**
*******************************************************************************/
static jint nfcManager_doGetLastError(JNIEnv*, jobject)
{
    ALOGD ("%s: last error=%i", __FUNCTION__, sLastError);
    return sLastError;
}


/*******************************************************************************
**
** Function:        nfcManager_doDeinitialize
**
** Description:     Turn off NFC.
**                  e: JVM environment.
**                  o: Java object.
**
** Returns:         True if ok.
**
*******************************************************************************/
static jboolean nfcManager_doDeinitialize (JNIEnv*, jobject)
{
    ALOGD ("%s: enter", __FUNCTION__);

    sIsDisabling = true;
    pn544InteropAbortNow ();
    SecureElement::getInstance().finalize ();
    PowerSwitch::getInstance ().initialize (PowerSwitch::UNKNOWN_LEVEL);

    if (sIsNfaEnabled)
    {
        SyncEventGuard guard (sNfaDisableEvent);
        tNFA_STATUS stat = NFA_Disable (TRUE /* graceful */);
        if (stat == NFA_STATUS_OK)
        {
            ALOGD ("%s: wait for completion", __FUNCTION__);
            sNfaDisableEvent.wait (); //wait for NFA command to finish
            PeerToPeer::getInstance ().handleNfcOnOff (false);
        }
        else
        {
            ALOGE ("%s: fail disable; error=0x%X", __FUNCTION__, stat);
        }
    }
    nativeNfcTag_abortWaits();
    NfcTag::getInstance().abort ();
    sAbortConnlessWait = true;
    nativeLlcpConnectionlessSocket_abortWait();
    sIsNfaEnabled = false;
    sDiscoveryEnabled = false;
    sIsDisabling = false;
    sIsSecElemSelected = false;
    gActivated = false;

    {
        //unblock NFA_EnablePolling() and NFA_DisablePolling()
        SyncEventGuard guard (sNfaEnableDisablePollingEvent);
        sNfaEnableDisablePollingEvent.notifyOne ();
    }

    NfcAdaptation& theInstance = NfcAdaptation::GetInstance();
    theInstance.Finalize();

    ALOGD ("%s: exit", __FUNCTION__);
    return JNI_TRUE;
}


/*******************************************************************************
**
** Function:        nfcManager_doCreateLlcpSocket
**
** Description:     Create a LLCP connection-oriented socket.
**                  e: JVM environment.
**                  o: Java object.
**                  nSap: Service access point.
**                  miu: Maximum information unit.
**                  rw: Receive window size.
**                  linearBufferLength: Max buffer size.
**
** Returns:         NativeLlcpSocket Java object.
**
*******************************************************************************/
static jobject nfcManager_doCreateLlcpSocket (JNIEnv* e, jobject, jint nSap, jint miu, jint rw, jint linearBufferLength)
{
    ALOGD ("%s: enter; sap=%d; miu=%d; rw=%d; buffer len=%d", __FUNCTION__, nSap, miu, rw, linearBufferLength);

    PeerToPeer::tJNI_HANDLE jniHandle = PeerToPeer::getInstance().getNewJniHandle ();
    PeerToPeer::getInstance().createClient (jniHandle, miu, rw);

    /* Create new NativeLlcpSocket object */
    jobject clientSocket = NULL;
    if (nfc_jni_cache_object_local(e, gNativeLlcpSocketClassName, &(clientSocket)) == -1)
    {
        ALOGE ("%s: fail Llcp socket creation", __FUNCTION__);
        return clientSocket;
    }

    /* Get NativeConnectionless class object */
    ScopedLocalRef<jclass> clsNativeLlcpSocket(e, e->GetObjectClass(clientSocket));
    if (e->ExceptionCheck())
    {
        e->ExceptionClear();
        ALOGE ("%s: fail get class object", __FUNCTION__);
        return clientSocket;
    }

    jfieldID f;

    /* Set socket SAP */
    f = e->GetFieldID (clsNativeLlcpSocket.get(), "mSap", "I");
    e->SetIntField (clientSocket, f, (jint) nSap);

    /* Set socket handle */
    f = e->GetFieldID (clsNativeLlcpSocket.get(), "mHandle", "I");
    e->SetIntField (clientSocket, f, (jint) jniHandle);

    /* Set socket MIU */
    f = e->GetFieldID (clsNativeLlcpSocket.get(), "mLocalMiu", "I");
    e->SetIntField (clientSocket, f, (jint) miu);

    /* Set socket RW */
    f = e->GetFieldID (clsNativeLlcpSocket.get(), "mLocalRw", "I");
    e->SetIntField (clientSocket, f, (jint) rw);

    ALOGD ("%s: exit", __FUNCTION__);
    return clientSocket;
}


/*******************************************************************************
**
** Function:        nfcManager_doCreateLlcpConnectionlessSocket
**
** Description:     Create a connection-less socket.
**                  e: JVM environment.
**                  o: Java object.
**                  nSap: Service access point.
**                  sn: Service name.
**
** Returns:         NativeLlcpConnectionlessSocket Java object.
**
*******************************************************************************/
static jobject nfcManager_doCreateLlcpConnectionlessSocket (JNIEnv *, jobject, jint nSap, jstring /*sn*/)
{
    ALOGD ("%s: nSap=0x%X", __FUNCTION__, nSap);
    return NULL;
}


/*******************************************************************************
**
** Function:        nfcManager_doGetSecureElementList
**
** Description:     Get a list of secure element handles.
**                  e: JVM environment.
**                  o: Java object.
**
** Returns:         List of secure element handles.
**
*******************************************************************************/
static jintArray nfcManager_doGetSecureElementList(JNIEnv* e, jobject)
{
    ALOGD ("%s", __FUNCTION__);
    return SecureElement::getInstance().getSecureElementIdList (e);
}

/*******************************************************************************
**
** Function:        nfcManager_enableRoutingToHost
**
** Description:     NFC controller starts routing data to host.
**                  e: JVM environment.
**                  o: Java object.
**
** Returns:         None
**
*******************************************************************************/
static void nfcManager_enableRoutingToHost(JNIEnv*, jobject)
{
    ALOGD ("%s: enter", __FUNCTION__);
    PowerSwitch::getInstance ().setLevel (PowerSwitch::FULL_POWER);
    PowerSwitch::getInstance ().setModeOn (PowerSwitch::HOST_ROUTING);
    if (sRfEnabled) {
        // Stop RF discovery to reconfigure
        startRfDiscovery(false);
    }
    RoutingManager::getInstance().commitRouting();
    startRfDiscovery(true);
    ALOGD ("%s: exit", __FUNCTION__);
}

/*******************************************************************************
**
** Function:        nfcManager_disableRoutingToHost
**
** Description:     NFC controller stops routing data to host.
**                  e: JVM environment.
**                  o: Java object.
**
** Returns:         None
**
*******************************************************************************/
static void nfcManager_disableRoutingToHost(JNIEnv*, jobject)
{
    ALOGD ("%s: enter", __FUNCTION__);
    bool rfWasEnabled = false;

    if (PowerSwitch::getInstance ().getLevel() == PowerSwitch::LOW_POWER)
    {
        ALOGD ("%s: no need to disable routing while power is OFF", __FUNCTION__);
        goto TheEnd;
    }

    if (sRfEnabled) {
        rfWasEnabled = true;
        // Stop RF discovery to reconfigure
        startRfDiscovery(false);
    }
    RoutingManager::getInstance().commitRouting();
    if (rfWasEnabled)
    {
        startRfDiscovery(true);
    }
    if (! PowerSwitch::getInstance ().setModeOff (PowerSwitch::HOST_ROUTING))
        PowerSwitch::getInstance ().setLevel (PowerSwitch::LOW_POWER);
TheEnd:
    ALOGD ("%s: exit", __FUNCTION__);
}

/*******************************************************************************
**
** Function:        nfcManager_doSelectSecureElement
**
** Description:     NFC controller starts routing data in listen mode.
**                  e: JVM environment.
**                  o: Java object.
**
** Returns:         None
**
*******************************************************************************/
static void nfcManager_doSelectSecureElement(JNIEnv*, jobject)
{
    ALOGD ("%s: enter", __FUNCTION__);
    bool stat = true;

    if (sIsSecElemSelected)
    {
        ALOGD ("%s: already selected", __FUNCTION__);
        goto TheEnd;
    }

    PowerSwitch::getInstance ().setLevel (PowerSwitch::FULL_POWER);

    if (sRfEnabled) {
        // Stop RF Discovery if we were polling
        startRfDiscovery (false);
    }


    stat = SecureElement::getInstance().activate (0xABCDEF);
    sIsSecElemSelected = true;

    startRfDiscovery (true);
    PowerSwitch::getInstance ().setModeOn (PowerSwitch::SE_ROUTING);
TheEnd:
    ALOGD ("%s: exit", __FUNCTION__);
}


/*******************************************************************************
**
** Function:        nfcManager_doDeselectSecureElement
**
** Description:     NFC controller stops routing data in listen mode.
**                  e: JVM environment.
**                  o: Java object.
**
** Returns:         None
**
*******************************************************************************/
static void nfcManager_doDeselectSecureElement(JNIEnv*, jobject)
{
    ALOGD ("%s: enter", __FUNCTION__);
    bool bRestartDiscovery = false;

    if (! sIsSecElemSelected)
    {
        ALOGE ("%s: already deselected", __FUNCTION__);
        goto TheEnd2;
    }

    if (PowerSwitch::getInstance ().getLevel() == PowerSwitch::LOW_POWER)
    {
        ALOGD ("%s: do not deselect while power is OFF", __FUNCTION__);
        sIsSecElemSelected = false;
        goto TheEnd;
    }

    if (sRfEnabled) {
        // Stop RF Discovery if we were polling
        startRfDiscovery (false);
        bRestartDiscovery = true;
    }

    //if controller is not routing to sec elems AND there is no pipe connected,
    //then turn off the sec elems
    if (SecureElement::getInstance().isBusy() == false)
        SecureElement::getInstance().deactivate (0xABCDEF);

    sIsSecElemSelected = false;
TheEnd:
    if (bRestartDiscovery)
        startRfDiscovery (true);

    //if nothing is active after this, then tell the controller to power down
    if (! PowerSwitch::getInstance ().setModeOff (PowerSwitch::SE_ROUTING))
        PowerSwitch::getInstance ().setLevel (PowerSwitch::LOW_POWER);

TheEnd2:
    ALOGD ("%s: exit", __FUNCTION__);
}


/*******************************************************************************
**
** Function:        isPeerToPeer
**
** Description:     Whether the activation data indicates the peer supports NFC-DEP.
**                  activated: Activation data.
**
** Returns:         True if the peer supports NFC-DEP.
**
*******************************************************************************/
static bool isPeerToPeer (tNFA_ACTIVATED& activated)
{
    return activated.activate_ntf.protocol == NFA_PROTOCOL_NFC_DEP;
}

/*******************************************************************************
**
** Function:        isListenMode
**
** Description:     Indicates whether the activation data indicates it is
**                  listen mode.
**
** Returns:         True if this listen mode.
**
*******************************************************************************/
static bool isListenMode(tNFA_ACTIVATED& activated)
{
    return ((NFC_DISCOVERY_TYPE_LISTEN_A == activated.activate_ntf.rf_tech_param.mode)
            || (NFC_DISCOVERY_TYPE_LISTEN_B == activated.activate_ntf.rf_tech_param.mode)
            || (NFC_DISCOVERY_TYPE_LISTEN_F == activated.activate_ntf.rf_tech_param.mode)
            || (NFC_DISCOVERY_TYPE_LISTEN_A_ACTIVE == activated.activate_ntf.rf_tech_param.mode)
            || (NFC_DISCOVERY_TYPE_LISTEN_F_ACTIVE == activated.activate_ntf.rf_tech_param.mode)
            || (NFC_DISCOVERY_TYPE_LISTEN_ISO15693 == activated.activate_ntf.rf_tech_param.mode)
            || (NFC_DISCOVERY_TYPE_LISTEN_B_PRIME == activated.activate_ntf.rf_tech_param.mode));
}

/*******************************************************************************
**
** Function:        nfcManager_doCheckLlcp
**
** Description:     Not used.
**
** Returns:         True
**
*******************************************************************************/
static jboolean nfcManager_doCheckLlcp(JNIEnv*, jobject)
{
    ALOGD("%s", __FUNCTION__);
    return JNI_TRUE;
}


/*******************************************************************************
**
** Function:        nfcManager_doActivateLlcp
**
** Description:     Not used.
**
** Returns:         True
**
*******************************************************************************/
static jboolean nfcManager_doActivateLlcp(JNIEnv*, jobject)
{
    ALOGD("%s", __FUNCTION__);
    return JNI_TRUE;
}


/*******************************************************************************
**
** Function:        nfcManager_doAbort
**
** Description:     Not used.
**
** Returns:         None
**
*******************************************************************************/
static void nfcManager_doAbort(JNIEnv*, jobject)
{
    ALOGE("%s: abort()", __FUNCTION__);
    abort();
}


/*******************************************************************************
**
** Function:        nfcManager_doDownload
**
** Description:     Download firmware patch files.  Do not turn on NFC.
**
** Returns:         True if ok.
**
*******************************************************************************/
static jboolean nfcManager_doDownload(JNIEnv*, jobject)
{
    ALOGD ("%s: enter", __FUNCTION__);
    NfcAdaptation& theInstance = NfcAdaptation::GetInstance();

    theInstance.Initialize(); //start GKI, NCI task, NFC task
    theInstance.DownloadFirmware ();
    theInstance.Finalize();
    ALOGD ("%s: exit", __FUNCTION__);
    return JNI_TRUE;
}


/*******************************************************************************
**
** Function:        nfcManager_doResetTimeouts
**
** Description:     Not used.
**
** Returns:         None
**
*******************************************************************************/
static void nfcManager_doResetTimeouts(JNIEnv*, jobject)
{
    ALOGD ("%s", __FUNCTION__);
    NfcTag::getInstance().resetAllTransceiveTimeouts ();
}


/*******************************************************************************
**
** Function:        nfcManager_doSetTimeout
**
** Description:     Set timeout value.
**                  e: JVM environment.
**                  o: Java object.
**                  tech: technology ID.
**                  timeout: Timeout value.
**
** Returns:         True if ok.
**
*******************************************************************************/
static bool nfcManager_doSetTimeout(JNIEnv*, jobject, jint tech, jint timeout)
{
    if (timeout <= 0)
    {
        ALOGE("%s: Timeout must be positive.",__FUNCTION__);
        return false;
    }
    ALOGD ("%s: tech=%d, timeout=%d", __FUNCTION__, tech, timeout);
    NfcTag::getInstance().setTransceiveTimeout (tech, timeout);
    return true;
}


/*******************************************************************************
**
** Function:        nfcManager_doGetTimeout
**
** Description:     Get timeout value.
**                  e: JVM environment.
**                  o: Java object.
**                  tech: technology ID.
**
** Returns:         Timeout value.
**
*******************************************************************************/
static jint nfcManager_doGetTimeout(JNIEnv*, jobject, jint tech)
{
    int timeout = NfcTag::getInstance().getTransceiveTimeout (tech);
    ALOGD ("%s: tech=%d, timeout=%d", __FUNCTION__, tech, timeout);
    return timeout;
}


/*******************************************************************************
**
** Function:        nfcManager_doDump
**
** Description:     Not used.
**                  e: JVM environment.
**                  o: Java object.
**
** Returns:         Text dump.
**
*******************************************************************************/
static jstring nfcManager_doDump(JNIEnv* e, jobject)
{
    char buffer[100];
    snprintf(buffer, sizeof(buffer), "libnfc llc error_count=%u", /*libnfc_llc_error_count*/ 0);
    return e->NewStringUTF(buffer);
}


/*******************************************************************************
**
** Function:        nfcManager_doSetP2pInitiatorModes
**
** Description:     Set P2P initiator's activation modes.
**                  e: JVM environment.
**                  o: Java object.
**                  modes: Active and/or passive modes.  The values are specified
**                          in external/libnfc-nxp/inc/phNfcTypes.h.  See
**                          enum phNfc_eP2PMode_t.
**
** Returns:         None.
**
*******************************************************************************/
static void nfcManager_doSetP2pInitiatorModes (JNIEnv *e, jobject o, jint modes)
{
    ALOGD ("%s: modes=0x%X", __FUNCTION__, modes);
    struct nfc_jni_native_data *nat = getNative(e, o);

    tNFA_TECHNOLOGY_MASK mask = 0;
    if (modes & 0x01) mask |= NFA_TECHNOLOGY_MASK_A;
    if (modes & 0x02) mask |= NFA_TECHNOLOGY_MASK_F;
    if (modes & 0x04) mask |= NFA_TECHNOLOGY_MASK_F;
    if (modes & 0x08) mask |= NFA_TECHNOLOGY_MASK_A_ACTIVE;
    if (modes & 0x10) mask |= NFA_TECHNOLOGY_MASK_F_ACTIVE;
    if (modes & 0x20) mask |= NFA_TECHNOLOGY_MASK_F_ACTIVE;
    nat->tech_mask = mask;
}


/*******************************************************************************
**
** Function:        nfcManager_doSetP2pTargetModes
**
** Description:     Set P2P target's activation modes.
**                  e: JVM environment.
**                  o: Java object.
**                  modes: Active and/or passive modes.
**
** Returns:         None.
**
*******************************************************************************/
static void nfcManager_doSetP2pTargetModes (JNIEnv*, jobject, jint modes)
{
    ALOGD ("%s: modes=0x%X", __FUNCTION__, modes);
    // Map in the right modes
    tNFA_TECHNOLOGY_MASK mask = 0;
    if (modes & 0x01) mask |= NFA_TECHNOLOGY_MASK_A;
    if (modes & 0x02) mask |= NFA_TECHNOLOGY_MASK_F;
    if (modes & 0x04) mask |= NFA_TECHNOLOGY_MASK_F;
    if (modes & 0x08) mask |= NFA_TECHNOLOGY_MASK_A_ACTIVE | NFA_TECHNOLOGY_MASK_F_ACTIVE;

    PeerToPeer::getInstance().setP2pListenMask(mask);
}

static void nfcManager_doEnableReaderMode (JNIEnv*, jobject, jint technologies)
{
    if (sDiscoveryEnabled) {
        sReaderModeEnabled = true;
        PeerToPeer::getInstance().enableP2pListening(false);
        NFA_PauseP2p();
        NFA_DisableListening();
        // Limit polling to these technologies
        int tech_mask = 0;
        if (technologies & 0x01)
           tech_mask |= NFA_TECHNOLOGY_MASK_A;
        if (technologies & 0x02)
           tech_mask |= NFA_TECHNOLOGY_MASK_B;
        if (technologies & 0x04)
           tech_mask |= NFA_TECHNOLOGY_MASK_F;
        if (technologies & 0x08)
           tech_mask |= NFA_TECHNOLOGY_MASK_ISO15693;
        if (technologies & 0x10)
           tech_mask |= NFA_TECHNOLOGY_MASK_KOVIO;

        enableDisableLptd(false);
        enableDisableLongGuardTime(true);
        NFA_SetRfDiscoveryDuration(READER_MODE_DISCOVERY_DURATION);
        restartPollingWithTechMask(tech_mask);
    }
}

static void nfcManager_doDisableReaderMode (JNIEnv* e, jobject o)
{
    struct nfc_jni_native_data *nat = getNative(e, o);
    if (sDiscoveryEnabled) {
        sReaderModeEnabled = false;
        NFA_ResumeP2p();
        PeerToPeer::getInstance().enableP2pListening(true);
        NFA_EnableListening();

        enableDisableLptd(true);
        enableDisableLongGuardTime(false);
        NFA_SetRfDiscoveryDuration(nat->discovery_duration);
        restartPollingWithTechMask(nat->tech_mask);
    }
}


/*****************************************************************************
**
** JNI functions for android-4.0.1_r1
**
*****************************************************************************/
static JNINativeMethod gMethods[] =
{
    {"doDownload", "()Z",
            (void *)nfcManager_doDownload},

    {"initializeNativeStructure", "()Z",
            (void*) nfcManager_initNativeStruc},

    {"doInitialize", "()Z",
            (void*) nfcManager_doInitialize},

    {"doDeinitialize", "()Z",
            (void*) nfcManager_doDeinitialize},

    {"sendRawFrame", "([B)Z",
            (void*) nfcManager_sendRawFrame},

    {"routeAid", "([BI)Z",
            (void*) nfcManager_routeAid},

    {"unrouteAid", "([B)Z",
            (void*) nfcManager_unrouteAid},

    {"enableDiscovery", "()V",
            (void*) nfcManager_enableDiscovery},

    {"enableRoutingToHost", "()V",
            (void*) nfcManager_enableRoutingToHost},

    {"disableRoutingToHost", "()V",
            (void*) nfcManager_disableRoutingToHost},

    {"doGetSecureElementList", "()[I",
            (void *)nfcManager_doGetSecureElementList},

    {"doSelectSecureElement", "()V",
            (void *)nfcManager_doSelectSecureElement},

    {"doDeselectSecureElement", "()V",
            (void *)nfcManager_doDeselectSecureElement},

    {"doCheckLlcp", "()Z",
            (void *)nfcManager_doCheckLlcp},

    {"doActivateLlcp", "()Z",
            (void *)nfcManager_doActivateLlcp},

    {"doCreateLlcpConnectionlessSocket", "(ILjava/lang/String;)Lcom/android/nfc/dhimpl/NativeLlcpConnectionlessSocket;",
            (void *)nfcManager_doCreateLlcpConnectionlessSocket},

    {"doCreateLlcpServiceSocket", "(ILjava/lang/String;III)Lcom/android/nfc/dhimpl/NativeLlcpServiceSocket;",
            (void*) nfcManager_doCreateLlcpServiceSocket},

    {"doCreateLlcpSocket", "(IIII)Lcom/android/nfc/dhimpl/NativeLlcpSocket;",
            (void*) nfcManager_doCreateLlcpSocket},

    {"doGetLastError", "()I",
            (void*) nfcManager_doGetLastError},

    {"disableDiscovery", "()V",
            (void*) nfcManager_disableDiscovery},

    {"doSetTimeout", "(II)Z",
            (void *)nfcManager_doSetTimeout},

    {"doGetTimeout", "(I)I",
            (void *)nfcManager_doGetTimeout},

    {"doResetTimeouts", "()V",
            (void *)nfcManager_doResetTimeouts},

    {"doAbort", "()V",
            (void *)nfcManager_doAbort},

    {"doSetP2pInitiatorModes", "(I)V",
            (void *)nfcManager_doSetP2pInitiatorModes},

    {"doSetP2pTargetModes", "(I)V",
            (void *)nfcManager_doSetP2pTargetModes},

    {"doEnableReaderMode", "(I)V",
            (void *)nfcManager_doEnableReaderMode},

    {"doDisableReaderMode", "()V",
            (void *)nfcManager_doDisableReaderMode},

    {"doDump", "()Ljava/lang/String;",
            (void *)nfcManager_doDump},
};


/*******************************************************************************
**
** Function:        register_com_android_nfc_NativeNfcManager
**
** Description:     Regisgter JNI functions with Java Virtual Machine.
**                  e: Environment of JVM.
**
** Returns:         Status of registration.
**
*******************************************************************************/
int register_com_android_nfc_NativeNfcManager (JNIEnv *e)
{
    ALOGD ("%s: enter", __FUNCTION__);
    PowerSwitch::getInstance ().initialize (PowerSwitch::UNKNOWN_LEVEL);
    ALOGD ("%s: exit", __FUNCTION__);
    return jniRegisterNativeMethods (e, gNativeNfcManagerClassName, gMethods, NELEM (gMethods));
}


/*******************************************************************************
**
** Function:        startRfDiscovery
**
** Description:     Ask stack to start polling and listening for devices.
**                  isStart: Whether to start.
**
** Returns:         None
**
*******************************************************************************/
void startRfDiscovery(bool isStart)
{
    tNFA_STATUS status = NFA_STATUS_FAILED;

    ALOGD ("%s: is start=%d", __FUNCTION__, isStart);
    SyncEventGuard guard (sNfaEnableDisablePollingEvent);
    status  = isStart ? NFA_StartRfDiscovery () : NFA_StopRfDiscovery ();
    if (status == NFA_STATUS_OK)
    {
        sNfaEnableDisablePollingEvent.wait (); //wait for NFA_RF_DISCOVERY_xxxx_EVT
        sRfEnabled = isStart;
    }
    else
    {
        ALOGE ("%s: Failed to start/stop RF discovery; error=0x%X", __FUNCTION__, status);
    }
}


/*******************************************************************************
**
** Function:        doStartupConfig
**
** Description:     Configure the NFC controller.
**
** Returns:         None
**
*******************************************************************************/
void doStartupConfig()
{
    struct nfc_jni_native_data *nat = getNative(0, 0);
    tNFA_STATUS stat = NFA_STATUS_FAILED;
    int actualLen = 0;

    // If polling for Active mode, set the ordering so that we choose Active over Passive mode first.
    if (nat && (nat->tech_mask & (NFA_TECHNOLOGY_MASK_A_ACTIVE | NFA_TECHNOLOGY_MASK_F_ACTIVE)))
    {
        UINT8  act_mode_order_param[] = { 0x01 };
        SyncEventGuard guard (sNfaSetConfigEvent);
        stat = NFA_SetConfig(NCI_PARAM_ID_ACT_ORDER, sizeof(act_mode_order_param), &act_mode_order_param[0]);
        if (stat == NFA_STATUS_OK)
            sNfaSetConfigEvent.wait ();
    }

    //configure RF polling frequency for each technology
    static tNFA_DM_DISC_FREQ_CFG nfa_dm_disc_freq_cfg;
    //values in the polling_frequency[] map to members of nfa_dm_disc_freq_cfg
    UINT8 polling_frequency [8] = {1, 1, 1, 1, 1, 1, 1, 1};
    actualLen = GetStrValue(NAME_POLL_FREQUENCY, (char*)polling_frequency, 8);
    if (actualLen == 8)
    {
        ALOGD ("%s: polling frequency", __FUNCTION__);
        memset (&nfa_dm_disc_freq_cfg, 0, sizeof(nfa_dm_disc_freq_cfg));
        nfa_dm_disc_freq_cfg.pa = polling_frequency [0];
        nfa_dm_disc_freq_cfg.pb = polling_frequency [1];
        nfa_dm_disc_freq_cfg.pf = polling_frequency [2];
        nfa_dm_disc_freq_cfg.pi93 = polling_frequency [3];
        nfa_dm_disc_freq_cfg.pbp = polling_frequency [4];
        nfa_dm_disc_freq_cfg.pk = polling_frequency [5];
        nfa_dm_disc_freq_cfg.paa = polling_frequency [6];
        nfa_dm_disc_freq_cfg.pfa = polling_frequency [7];
        p_nfa_dm_rf_disc_freq_cfg = &nfa_dm_disc_freq_cfg;
    }
}


/*******************************************************************************
**
** Function:        nfcManager_isNfcActive
**
** Description:     Used externaly to determine if NFC is active or not.
**
** Returns:         'true' if the NFC stack is running, else 'false'.
**
*******************************************************************************/
bool nfcManager_isNfcActive()
{
    return sIsNfaEnabled;
}

void restartPollingWithTechMask (int tech_mask)
{
    tNFA_STATUS stat = NFA_STATUS_FAILED;
    startRfDiscovery (false);
    {
        SyncEventGuard guard (sNfaEnableDisablePollingEvent);
        stat = NFA_DisablePolling ();
        if (stat == NFA_STATUS_OK)
        {
            ALOGD ("%s: wait for enable event", __FUNCTION__);
            sNfaEnableDisablePollingEvent.wait (); //wait for NFA_POLL_DISABLED_EVT
        }
        else
        {
            ALOGE ("%s: failed to disable polling; error=0x%X", __FUNCTION__, stat);
            goto TheEnd;
        }
        stat = NFA_EnablePolling (tech_mask);
        if (stat == NFA_STATUS_OK)
        {
            ALOGD ("%s: wait for enable event", __FUNCTION__);
            sNfaEnableDisablePollingEvent.wait (); //wait for NFA_POLL_ENABLED_EVT
        }
        else
            ALOGE ("%s: fail enable polling; error=0x%X", __FUNCTION__, stat);
    }
TheEnd:
    startRfDiscovery(true);
}

/*******************************************************************************
**
** Function:        startStopPolling
**
** Description:     Start or stop polling.
**                  isStartPolling: true to start polling; false to stop polling.
**
** Returns:         None.
**
*******************************************************************************/
void startStopPolling (bool isStartPolling)
{
    ALOGD ("%s: enter; isStart=%u", __FUNCTION__, isStartPolling);
    tNFA_STATUS stat = NFA_STATUS_FAILED;

    startRfDiscovery (false);
    if (isStartPolling)
    {
        tNFA_TECHNOLOGY_MASK tech_mask = DEFAULT_TECH_MASK;
        unsigned long num = 0;
        if (GetNumValue(NAME_POLLING_TECH_MASK, &num, sizeof(num)))
            tech_mask = num;

        SyncEventGuard guard (sNfaEnableDisablePollingEvent);
        ALOGD ("%s: enable polling", __FUNCTION__);
        stat = NFA_EnablePolling (tech_mask);
        if (stat == NFA_STATUS_OK)
        {
            ALOGD ("%s: wait for enable event", __FUNCTION__);
            sNfaEnableDisablePollingEvent.wait (); //wait for NFA_POLL_ENABLED_EVT
        }
        else
            ALOGE ("%s: fail enable polling; error=0x%X", __FUNCTION__, stat);
    }
    else
    {
        SyncEventGuard guard (sNfaEnableDisablePollingEvent);
        ALOGD ("%s: disable polling", __FUNCTION__);
        stat = NFA_DisablePolling ();
        if (stat == NFA_STATUS_OK)
        {
            sNfaEnableDisablePollingEvent.wait (); //wait for NFA_POLL_DISABLED_EVT
        }
        else
            ALOGE ("%s: fail disable polling; error=0x%X", __FUNCTION__, stat);
    }
    startRfDiscovery (true);
    ALOGD ("%s: exit", __FUNCTION__);
}


} /* namespace android */
