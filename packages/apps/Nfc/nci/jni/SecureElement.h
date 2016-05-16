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
 *  Communicate with secure elements that are attached to the NFC
 *  controller.
 */
#pragma once
#include "SyncEvent.h"
#include "DataQueue.h"
#include "NfcJniUtil.h"
#include "RouteDataSet.h"
extern "C"
{
    #include "nfa_ee_api.h"
    #include "nfa_hci_api.h"
    #include "nfa_hci_defs.h"
    #include "nfa_ce_api.h"
}


class SecureElement
{
public:
    tNFA_HANDLE  mActiveEeHandle;


    /*******************************************************************************
    **
    ** Function:        getInstance
    **
    ** Description:     Get the SecureElement singleton object.
    **
    ** Returns:         SecureElement object.
    **
    *******************************************************************************/
    static SecureElement& getInstance ();


    /*******************************************************************************
    **
    ** Function:        initialize
    **
    ** Description:     Initialize all member variables.
    **                  native: Native data.
    **
    ** Returns:         True if ok.
    **
    *******************************************************************************/
    bool initialize (nfc_jni_native_data* native);


    /*******************************************************************************
    **
    ** Function:        finalize
    **
    ** Description:     Release all resources.
    **
    ** Returns:         None
    **
    *******************************************************************************/
    void finalize ();


    /*******************************************************************************
    **
    ** Function:        getSecureElementIdList
    **
    ** Description:     Get a list of ID's of all secure elements.
    **                  e: Java Virtual Machine.
    **
    ** Returns:         List of ID's.
    **
    *******************************************************************************/
    jintArray getSecureElementIdList (JNIEnv* e);


    /*******************************************************************************
    **
    ** Function:        activate
    **
    ** Description:     Turn on the secure element.
    **                  seID: ID of secure element.
    **
    ** Returns:         True if ok.
    **
    *******************************************************************************/
    bool activate (jint seID);


    /*******************************************************************************
    **
    ** Function:        deactivate
    **
    ** Description:     Turn off the secure element.
    **                  seID: ID of secure element.
    **
    ** Returns:         True if ok.
    **
    *******************************************************************************/
    bool deactivate (jint seID);


    /*******************************************************************************
    **
    ** Function:        connectEE
    **
    ** Description:     Connect to the execution environment.
    **
    ** Returns:         True if ok.
    **
    *******************************************************************************/
    bool connectEE ();


    /*******************************************************************************
    **
    ** Function:        disconnectEE
    **
    ** Description:     Disconnect from the execution environment.
    **                  seID: ID of secure element.
    **
    ** Returns:         True if ok.
    **
    *******************************************************************************/
    bool disconnectEE (jint seID);


    /*******************************************************************************
    **
    ** Function:        transceive
    **
    ** Description:     Send data to the secure element; read it's response.
    **                  xmitBuffer: Data to transmit.
    **                  xmitBufferSize: Length of data.
    **                  recvBuffer: Buffer to receive response.
    **                  recvBufferMaxSize: Maximum size of buffer.
    **                  recvBufferActualSize: Actual length of response.
    **                  timeoutMillisec: timeout in millisecond
    **
    ** Returns:         True if ok.
    **
    *******************************************************************************/
    bool transceive (UINT8* xmitBuffer, INT32 xmitBufferSize, UINT8* recvBuffer,
                     INT32 recvBufferMaxSize, INT32& recvBufferActualSize, INT32 timeoutMillisec);

    void notifyModeSet (tNFA_HANDLE eeHandle, bool success);

    /*******************************************************************************
    **
    ** Function:        notifyListenModeState
    **
    ** Description:     Notify the NFC service about whether the SE was activated
    **                  in listen mode.
    **                  isActive: Whether the secure element is activated.
    **
    ** Returns:         None
    **
    *******************************************************************************/
    void notifyListenModeState (bool isActivated);

    /*******************************************************************************
    **
    ** Function:        notifyRfFieldEvent
    **
    ** Description:     Notify the NFC service about RF field events from the stack.
    **                  isActive: Whether any secure element is activated.
    **
    ** Returns:         None
    **
    *******************************************************************************/
    void notifyRfFieldEvent (bool isActive);


    /*******************************************************************************
    **
    ** Function:        resetRfFieldStatus ();
    **
    ** Description:     Resets the field status.
    **
    ** Returns:         None
    **
    *******************************************************************************/
    void resetRfFieldStatus ();

    /*******************************************************************************
    **
    ** Function:        storeUiccInfo
    **
    ** Description:     Store a copy of the execution environment information from the stack.
    **                  info: execution environment information.
    **
    ** Returns:         None
    **
    *******************************************************************************/
    void storeUiccInfo (tNFA_EE_DISCOVER_REQ& info);


    /*******************************************************************************
    **
    ** Function:        getUiccId
    **
    ** Description:     Get the ID of the secure element.
    **                  eeHandle: Handle to the secure element.
    **                  uid: Array to receive the ID.
    **
    ** Returns:         True if ok.
    **
    *******************************************************************************/
    bool getUiccId (tNFA_HANDLE eeHandle, jbyteArray& uid);


    /*******************************************************************************
    **
    ** Function:        getTechnologyList
    **
    ** Description:     Get all the technologies supported by a secure element.
    **                  eeHandle: Handle of secure element.
    **                  techList: List to receive the technologies.
    **
    ** Returns:         True if ok.
    **
    *******************************************************************************/
    bool getTechnologyList (tNFA_HANDLE eeHandle, jintArray& techList);


    /*******************************************************************************
    **
    ** Function:        notifyTransactionListenersOfAid
    **
    ** Description:     Notify the NFC service about a transaction event from secure element.
    **                  aid: Buffer contains application ID.
    **                  aidLen: Length of application ID.
    **
    ** Returns:         None
    **
    *******************************************************************************/
    void notifyTransactionListenersOfAid (const UINT8* aid, UINT8 aidLen);


    /*******************************************************************************
    **
    ** Function:        notifyTransactionListenersOfTlv
    **
    ** Description:     Notify the NFC service about a transaction event from secure element.
    **                  The type-length-value contains AID and parameter.
    **                  tlv: type-length-value encoded in Basic Encoding Rule.
    **                  tlvLen: Length tlv.
    **
    ** Returns:         None
    **
    *******************************************************************************/
    void notifyTransactionListenersOfTlv (const UINT8* tlv, UINT8 tlvLen);


    /*******************************************************************************
    **
    ** Function:        connectionEventHandler
    **
    ** Description:     Receive card-emulation related events from stack.
    **                  event: Event code.
    **                  eventData: Event data.
    **
    ** Returns:         None
    **
    *******************************************************************************/
    void connectionEventHandler (UINT8 event, tNFA_CONN_EVT_DATA* eventData);


    /*******************************************************************************
    **
    ** Function:        applyRoutes
    **
    ** Description:     Read route data from XML and apply them again
    **                  to every secure element.
    **
    ** Returns:         None
    **
    *******************************************************************************/
    void applyRoutes ();


    /*******************************************************************************
    **
    ** Function:        setActiveSeOverride
    **
    ** Description:     Specify which secure element to turn on.
    **                  activeSeOverride: ID of secure element
    **
    ** Returns:         None
    **
    *******************************************************************************/
    void setActiveSeOverride (UINT8 activeSeOverride);


    /*******************************************************************************
    **
    ** Function:        isBusy
    **
    ** Description:     Whether NFC controller is routing listen-mode events or a pipe is connected.
    **
    ** Returns:         True if either case is true.
    **
    *******************************************************************************/
    bool isBusy ();


    /*******************************************************************************
    **
    ** Function         getActualNumEe
    **
    ** Description      Returns number of secure elements we know about.
    **
    ** Returns          Number of secure elements we know about.
    **
    *******************************************************************************/
    UINT8 getActualNumEe();


    /*******************************************************************************
    **
    ** Function         getSeVerInfo
    **
    ** Description      Gets version information and id for a secure element.  The
    **                  seIndex parmeter is the zero based index of the secure
    **                  element to get verion info for.  The version infommation
    **                  is returned as a string int the verInfo parameter.
    **
    ** Returns          ture on success, false on failure
    **
    *******************************************************************************/
    bool getSeVerInfo(int seIndex, char * verInfo, int verInfoSz, UINT8 * seid);


    /*******************************************************************************
    **
    ** Function:        isActivatedInListenMode
    **
    ** Description:     Can be used to determine if the SE is activated in listen mode
    **
    ** Returns:         True if the SE is activated in listen mode
    **
    *******************************************************************************/
    bool isActivatedInListenMode();

    /*******************************************************************************
    **
    ** Function:        isRfFieldOn
    **
    ** Description:     Can be used to determine if the SE is in an RF field
    **
    ** Returns:         True if the SE is activated in an RF field
    **
    *******************************************************************************/
    bool isRfFieldOn();

private:
    static const unsigned int MAX_RESPONSE_SIZE = 1024;
    enum RouteSelection {NoRoute, DefaultRoute, SecElemRoute};
    static const int MAX_NUM_EE = 5;    //max number of EE's
    static const UINT8 STATIC_PIPE_0x70 = 0x70; //Broadcom's proprietary static pipe
    static const UINT8 STATIC_PIPE_0x71 = 0x71; //Broadcom's proprietary static pipe
    static const UINT8 EVT_SEND_DATA = 0x10;    //see specification ETSI TS 102 622 v9.0.0 (Host Controller Interface); section 9.3.3.3
    static const tNFA_HANDLE EE_HANDLE_0xF3 = 0x4F3; //handle to secure element in slot 0
    static const tNFA_HANDLE EE_HANDLE_0xF4 = 0x4F4; //handle to secure element in slot 1
    static SecureElement sSecElem;
    static const char* APP_NAME;

    UINT8           mDestinationGate;       //destination gate of the UICC
    tNFA_HANDLE     mNfaHciHandle;          //NFA handle to NFA's HCI component
    nfc_jni_native_data* mNativeData;
    bool    mIsInit;                // whether EE is initialized
    UINT8   mActualNumEe;           // actual number of EE's reported by the stack
    UINT8   mNumEePresent;          // actual number of usable EE's
    bool    mbNewEE;
    UINT8   mNewPipeId;
    UINT8   mNewSourceGate;
    UINT16  mActiveSeOverride;      // active "enable" seid, 0 means activate all SEs
    tNFA_STATUS mCommandStatus;     //completion status of the last command
    bool    mIsPiping;              //is a pipe connected to the controller?
    RouteSelection mCurrentRouteSelection;
    int     mActualResponseSize;         //number of bytes in the response received from secure element
    bool    mUseOberthurWarmReset;  //whether to use warm-reset command
    bool    mActivatedInListenMode; // whether we're activated in listen mode
    UINT8   mOberthurWarmResetCommand; //warm-reset command byte
    tNFA_EE_INFO mEeInfo [MAX_NUM_EE];  //actual size stored in mActualNumEe
    tNFA_EE_DISCOVER_REQ mUiccInfo;
    tNFA_HCI_GET_GATE_PIPE_LIST mHciCfg;
    SyncEvent       mEeRegisterEvent;
    SyncEvent       mHciRegisterEvent;
    SyncEvent       mEeSetModeEvent;
    SyncEvent       mPipeListEvent;
    SyncEvent       mCreatePipeEvent;
    SyncEvent       mPipeOpenedEvent;
    SyncEvent       mAllocateGateEvent;
    SyncEvent       mDeallocateGateEvent;
    SyncEvent       mRoutingEvent;
    SyncEvent       mUiccInfoEvent;
    SyncEvent       mUiccListenEvent;
    SyncEvent       mAidAddRemoveEvent;
    SyncEvent       mTransceiveEvent;
    SyncEvent       mVerInfoEvent;
    SyncEvent       mRegistryEvent;
    UINT8           mVerInfo [3];
    UINT8           mResponseData [MAX_RESPONSE_SIZE];
    RouteDataSet    mRouteDataSet; //routing data
    std::vector<std::string> mUsedAids; //AID's that are used in current routes
    UINT8           mAidForEmptySelect[NCI_MAX_AID_LEN+1];
    Mutex           mMutex; // protects fields below
    bool            mRfFieldIsOn; // last known RF field state
    struct timespec mLastRfFieldToggle; // last time RF field went off
    /*******************************************************************************
    **
    ** Function:        SecureElement
    **
    ** Description:     Initialize member variables.
    **
    ** Returns:         None
    **
    *******************************************************************************/
    SecureElement ();


    /*******************************************************************************
    **
    ** Function:        ~SecureElement
    **
    ** Description:     Release all resources.
    **
    ** Returns:         None
    **
    *******************************************************************************/
    ~SecureElement ();


    /*******************************************************************************
    **
    ** Function:        nfaEeCallback
    **
    ** Description:     Receive execution environment-related events from stack.
    **                  event: Event code.
    **                  eventData: Event data.
    **
    ** Returns:         None
    **
    *******************************************************************************/
    static void nfaEeCallback (tNFA_EE_EVT event, tNFA_EE_CBACK_DATA* eventData);


    /*******************************************************************************
    **
    ** Function:        nfaHciCallback
    **
    ** Description:     Receive Host Controller Interface-related events from stack.
    **                  event: Event code.
    **                  eventData: Event data.
    **
    ** Returns:         None
    **
    *******************************************************************************/
    static void nfaHciCallback (tNFA_HCI_EVT event, tNFA_HCI_EVT_DATA* eventData);


    /*******************************************************************************
    **
    ** Function:        findEeByHandle
    **
    ** Description:     Find information about an execution environment.
    **                  eeHandle: Handle to execution environment.
    **
    ** Returns:         Information about an execution environment.
    **
    *******************************************************************************/
    tNFA_EE_INFO *findEeByHandle (tNFA_HANDLE eeHandle);


    /*******************************************************************************
    **
    ** Function:        findUiccByHandle
    **
    ** Description:     Find information about an execution environment.
    **                  eeHandle: Handle of the execution environment.
    **
    ** Returns:         Information about the execution environment.
    **
    *******************************************************************************/
    tNFA_EE_DISCOVER_INFO *findUiccByHandle (tNFA_HANDLE eeHandle);


    /*******************************************************************************
    **
    ** Function:        getDefaultEeHandle
    **
    ** Description:     Get the handle to the execution environment.
    **
    ** Returns:         Handle to the execution environment.
    **
    *******************************************************************************/
    tNFA_HANDLE getDefaultEeHandle ();


    /*******************************************************************************
    **
    ** Function:        adjustRoutes
    **
    ** Description:     Adjust routes in the controller's listen-mode routing table.
    **                  selection: which set of routes to configure the controller.
    **
    ** Returns:         None
    **
    *******************************************************************************/
    void adjustRoutes (RouteSelection selection);


    /*******************************************************************************
    **
    ** Function:        adjustProtocolRoutes
    **
    ** Description:     Adjust default routing based on protocol in NFC listen mode.
    **                  isRouteToEe: Whether routing to EE (true) or host (false).
    **
    ** Returns:         None
    **
    *******************************************************************************/
    void adjustProtocolRoutes (RouteDataSet::Database* db, RouteSelection routeSelection);


    /*******************************************************************************
    **
    ** Function:        adjustTechnologyRoutes
    **
    ** Description:     Adjust default routing based on technology in NFC listen mode.
    **                  isRouteToEe: Whether routing to EE (true) or host (false).
    **
    ** Returns:         None
    **
    *******************************************************************************/
    void adjustTechnologyRoutes (RouteDataSet::Database* db, RouteSelection routeSelection);


    /*******************************************************************************
    **
    ** Function:        getEeInfo
    **
    ** Description:     Get latest information about execution environments from stack.
    **
    ** Returns:         True if at least 1 EE is available.
    **
    *******************************************************************************/
    bool getEeInfo ();

    /*******************************************************************************
    **
    ** Function:        eeStatusToString
    **
    ** Description:     Convert status code to status text.
    **                  status: Status code
    **
    ** Returns:         None
    **
    *******************************************************************************/
    static const char* eeStatusToString (UINT8 status);


    /*******************************************************************************
    **
    ** Function:        encodeAid
    **
    ** Description:     Encode AID in type-length-value using Basic Encoding Rule.
    **                  tlv: Buffer to store TLV.
    **                  tlvMaxLen: TLV buffer's maximum length.
    **                  tlvActualLen: TLV buffer's actual length.
    **                  aid: Buffer of Application ID.
    **                  aidLen: Aid buffer's actual length.
    **
    ** Returns:         True if ok.
    **
    *******************************************************************************/
    bool encodeAid (UINT8* tlv, UINT16 tlvMaxLen, UINT16& tlvActualLen, const UINT8* aid, UINT8 aidLen);
};

