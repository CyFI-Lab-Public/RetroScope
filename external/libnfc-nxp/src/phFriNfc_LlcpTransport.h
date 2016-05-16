/*
 * Copyright (C) 2010 NXP Semiconductors
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

/**
 * \file  phFriNfc_LlcpTransport.h
 * \brief 
 *
 * Project: NFC-FRI
 *
 */       
 
#ifndef PHFRINFC_LLCP_TRANSPORT_H
#define PHFRINFC_LLCP_TRANSPORT_H
#include <phNfcHalTypes.h>
#include <phNfcLlcpTypes.h>
#include <phNfcTypes.h>
#include <phLibNfcStatus.h>
#include <phFriNfc_Llcp.h>
#include <phFriNfc_LlcpUtils.h>
#ifdef ANDROID
#include <string.h>
#include <pthread.h>
#endif


typedef uint32_t    phFriNfc_Socket_Handle;

/**
 * \ingroup grp_fri_nfc_llcp_mac
 * \brief Declaration of a TRANSPORT type
 */
struct phFriNfc_LlcpTransport;
typedef struct phFriNfc_LlcpTransport phFriNfc_LlcpTransport_t;

struct phFriNfc_LlcpTransport_Socket;
typedef struct phFriNfc_LlcpTransport_Socket phFriNfc_LlcpTransport_Socket_t;

struct phFriNfc_Llcp_CachedServiceName;
typedef struct phFriNfc_Llcp_CachedServiceName phFriNfc_Llcp_CachedServiceName_t;

/*========== ENUMERATES ===========*/

/* Enum reperesents the different LLCP Link status*/
typedef enum phFriNfc_LlcpTransportSocket_eSocketState
{
   phFriNfc_LlcpTransportSocket_eSocketDefault,
   phFriNfc_LlcpTransportSocket_eSocketCreated,
   phFriNfc_LlcpTransportSocket_eSocketBound,
   phFriNfc_LlcpTransportSocket_eSocketRegistered,
   phFriNfc_LlcpTransportSocket_eSocketConnected,
   phFriNfc_LlcpTransportSocket_eSocketConnecting,
   phFriNfc_LlcpTransportSocket_eSocketAccepted,
   phFriNfc_LlcpTransportSocket_eSocketDisconnected,
   phFriNfc_LlcpTransportSocket_eSocketDisconnecting,
   phFriNfc_LlcpTransportSocket_eSocketRejected,
}phFriNfc_LlcpTransportSocket_eSocketState_t;



/*========== CALLBACKS ===========*/

/**
*\ingroup grp_fri_nfc
*
* \brief LLCP socket error notification callback definition
*/
typedef void (*pphFriNfc_LlcpTransportSocketErrCb_t) ( void*      pContext,
                                                       uint8_t    nErrCode);


/**
*\ingroup grp_fri_nfc
*
* \brief LLCP socket listen callback definition
*/
typedef void (*pphFriNfc_LlcpTransportSocketListenCb_t) (void*                            pContext,
                                                         phFriNfc_LlcpTransport_Socket_t  *IncomingSocket);

/**
*\ingroup grp_fri_nfc
*
* \brief LLCP socket connect callback definition
*/
typedef void (*pphFriNfc_LlcpTransportSocketConnectCb_t)  ( void*        pContext,
                                                            uint8_t      nErrCode,
                                                            NFCSTATUS    status);

/**
*\ingroup grp_fri_nfc
*
* \brief LLCP socket disconnect callback definition
*/
typedef void (*pphFriNfc_LlcpTransportSocketDisconnectCb_t) (void*        pContext,
                                                             NFCSTATUS    status);

/**
*\ingroup grp_fri_nfc
*
* \brief LLCP socket accept callback definition
*/
typedef void (*pphFriNfc_LlcpTransportSocketAcceptCb_t) (void*        pContext,
                                                         NFCSTATUS    status);

/**
*\ingroup grp_fri_nfc
*
* \brief LLCP socket reject callback definition
*/
typedef void (*pphFriNfc_LlcpTransportSocketRejectCb_t) (void*        pContext,
                                                         NFCSTATUS    status);

/**
*\ingroup grp_fri_nfc
*
* \brief LLCP socket reception callback definition
*/
typedef void (*pphFriNfc_LlcpTransportSocketRecvCb_t) (void*     pContext,
                                                       NFCSTATUS status);

/**
*\ingroup grp_fri_nfc
*
* \brief LLCP socket reception with SSAP callback definition
*/
typedef void (*pphFriNfc_LlcpTransportSocketRecvFromCb_t) (void*       pContext,
                                                           uint8_t     ssap,
                                                           NFCSTATUS   status);

/**
*\ingroup grp_fri_nfc
*
* \brief LLCP socket emission callback definition
*/
typedef void (*pphFriNfc_LlcpTransportSocketSendCb_t) (void*        pContext,
                                                       NFCSTATUS    status);


/*========== STRUCTURES ===========*/
/**
 * \ingroup grp_fri_nfc_llcp_mac
 * \brief Declaration of a SOCKET type
 */
struct phFriNfc_LlcpTransport_Socket 
{
   phFriNfc_LlcpTransportSocket_eSocketState_t    eSocket_State;
   phFriNfc_LlcpTransport_eSocketType_t           eSocket_Type;
   phFriNfc_LlcpTransport_sSocketOptions_t        sSocketOption;
   pphFriNfc_LlcpTransportSocketErrCb_t           pSocketErrCb;

   /* Remote and local socket info */
   uint8_t                                        socket_sSap;
   uint8_t                                        socket_dSap;
   // TODO: copy service name (could be deallocated by upper layer)
   phNfc_sData_t                                  sServiceName;
   uint8_t                                        remoteRW;
   uint8_t                                        localRW;
   uint16_t                                       remoteMIU;
   uint16_t                                       localMIUX;
   uint8_t                                        index;

   /* SDP related fields */
   uint8_t                                       nTid;

   /* Information Flags */
   bool_t                                        bSocketRecvPending;
   bool_t                                        bSocketSendPending;
   bool_t                                        bSocketListenPending;
   bool_t                                        bSocketDiscPending;
   bool_t                                        bSocketConnectPending;
   bool_t                                        bSocketAcceptPending;
   bool_t                                        bSocketRRPending;
   bool_t                                        bSocketRNRPending;

   /* Buffers */
   phNfc_sData_t                                  sSocketSendBuffer;
   phNfc_sData_t                                  sSocketLinearBuffer;
   phNfc_sData_t*                                 sSocketRecvBuffer;
   uint32_t                                       *receivedLength;
   uint32_t                                       bufferLinearLength;
   uint32_t                                       bufferSendMaxLength;
   uint32_t                                       bufferRwMaxLength;
   bool_t                                         ReceiverBusyCondition;
   bool_t                                         RemoteBusyConditionInfo;
   UTIL_FIFO_BUFFER                               sCyclicFifoBuffer;
   uint32_t                                       indexRwRead;
   uint32_t                                       indexRwWrite;

   /* Construction Frame */
   phFriNfc_Llcp_sPacketHeader_t                  sLlcpHeader;
   phFriNfc_Llcp_sPacketSequence_t                sSequence;
   uint8_t                                        socket_VS;
   uint8_t                                        socket_VSA;
   uint8_t                                        socket_VR;
   uint8_t                                        socket_VRA;

   /* Callbacks */
   pphFriNfc_LlcpTransportSocketAcceptCb_t        pfSocketAccept_Cb;
   pphFriNfc_LlcpTransportSocketSendCb_t          pfSocketSend_Cb;
   pphFriNfc_LlcpTransportSocketRecvFromCb_t      pfSocketRecvFrom_Cb;
   pphFriNfc_LlcpTransportSocketRecvCb_t          pfSocketRecv_Cb;
   pphFriNfc_LlcpTransportSocketListenCb_t        pfSocketListen_Cb;
   pphFriNfc_LlcpTransportSocketConnectCb_t       pfSocketConnect_Cb;
   pphFriNfc_LlcpTransportSocketDisconnectCb_t    pfSocketDisconnect_Cb;

   /* Table of PHFRINFC_LLCP_RW_MAX Receive Windows Buffers */
   phNfc_sData_t                                  sSocketRwBufferTable[PHFRINFC_LLCP_RW_MAX];

   /* Pointer a the socket table */
   phFriNfc_LlcpTransport_t                       *psTransport;
   /* Context */
   void                                          *pListenContext;
   void                                          *pAcceptContext;
   void                                          *pRejectContext;
   void                                          *pConnectContext;
   void                                          *pDisconnectContext;
   void                                          *pSendContext;
   void                                          *pRecvContext;
   void                                          *pContext;
};

/**
 * \ingroup grp_fri_nfc_llcp_mac
 * \brief TODO
 */
struct phFriNfc_Llcp_CachedServiceName
{
   phNfc_sData_t                         sServiceName;
   uint8_t                               nSap;
};


/**
 * \ingroup grp_fri_nfc_llcp_mac
 * \brief Declaration of a TRANSPORT Type with a table of PHFRINFC_LLCP_NB_SOCKET_DEFAULT sockets
 *        and a pointer a Llcp layer
 */
struct phFriNfc_LlcpTransport 
{
   phFriNfc_LlcpTransport_Socket_t       pSocketTable[PHFRINFC_LLCP_NB_SOCKET_MAX];
   phFriNfc_Llcp_CachedServiceName_t     pCachedServiceNames[PHFRINFC_LLCP_SDP_ADVERTISED_NB];
   phFriNfc_Llcp_t                       *pLlcp;
   pthread_mutex_t                       mutex;
   bool_t                                bSendPending;
   bool_t                                bRecvPending;
   bool_t                                bDmPending;
   bool_t                                bFrmrPending;

   phFriNfc_Llcp_LinkSend_CB_t           pfLinkSendCb;
   void                                  *pLinkSendContext;

   uint8_t                               socketIndex;

   /**< Info field of pending FRMR packet*/
   uint8_t                               FrmrInfoBuffer[4];
   phFriNfc_Llcp_sPacketHeader_t         sLlcpHeader;
   phFriNfc_Llcp_sPacketSequence_t       sSequence;

  /**< Info field of pending DM packet*/
   phFriNfc_Llcp_sPacketHeader_t         sDmHeader;
   phNfc_sData_t                         sDmPayload;
   uint8_t                               DmInfoBuffer[3];

   uint8_t                               LinkStatusError;

   /**< Service discovery related infos */
   phNfc_sData_t                         *psDiscoveryServiceNameList;
   uint8_t                               *pnDiscoverySapList;
   uint8_t                               nDiscoveryListSize;
   uint8_t                               nDiscoveryReqOffset;
   uint8_t                               nDiscoveryResOffset;

   uint8_t                               nDiscoveryResTidList[PHFRINFC_LLCP_SNL_RESPONSE_MAX];
   uint8_t                               nDiscoveryResSapList[PHFRINFC_LLCP_SNL_RESPONSE_MAX];
   uint8_t                               nDiscoveryResListSize;

   uint8_t                               pDiscoveryBuffer[PHFRINFC_LLCP_MIU_DEFAULT];
   pphFriNfc_Cr_t                        pfDiscover_Cb;
   void                                  *pDiscoverContext;

};

/*
################################################################################
********************** TRANSPORT Interface Function Prototype  *****************
################################################################################
*/

bool_t testAndSetSendPending(phFriNfc_LlcpTransport_t* transport);

void clearSendPending(phFriNfc_LlcpTransport_t* transport);

 /**
* \ingroup grp_fri_nfc
* \brief <b>Create a socket on a LLCP-connected device</b>.
*
*/
NFCSTATUS phFriNfc_LlcpTransport_Reset (phFriNfc_LlcpTransport_t      *pLlcpSocketTable,
                                        phFriNfc_Llcp_t               *pLlcp);


/**
* \ingroup grp_fri_nfc
* \brief <b>Close all existing sockets</b>.
*
*/
NFCSTATUS phFriNfc_LlcpTransport_CloseAll (phFriNfc_LlcpTransport_t  *pLlcpSocketTable);


/**
* \ingroup grp_fri_nfc
* \brief <b>Used by transport layers to request a send on link layer</b>.
*
*/
NFCSTATUS phFriNfc_LlcpTransport_LinkSend( phFriNfc_LlcpTransport_t         *LlcpTransport,
                                           phFriNfc_Llcp_sPacketHeader_t    *psHeader,
                                           phFriNfc_Llcp_sPacketSequence_t  *psSequence,
                                           phNfc_sData_t                    *psInfo,
                                           phFriNfc_Llcp_LinkSend_CB_t      pfSend_CB,
                                           uint8_t                          socketIndex,
                                           void                             *pContext );


/**
* \ingroup grp_fri_nfc
* \brief <b>Used by transport layers to send a DM frame</b>.
*
*  This function is only used when the DM is not related to a DISC on a socket.
*/
NFCSTATUS phFriNfc_LlcpTransport_SendDisconnectMode(phFriNfc_LlcpTransport_t* psTransport,
                                                    uint8_t                   dsap,
                                                    uint8_t                   ssap,
                                                    uint8_t                   dmOpCode);

/**
* \ingroup grp_fri_nfc
* \brief <b>Used by transport layers to send a FRMR frame</b>.
*
*/
NFCSTATUS phFriNfc_LlcpTransport_SendFrameReject(phFriNfc_LlcpTransport_t           *psTransport,
                                                 uint8_t                            dsap,
                                                 uint8_t                            rejectedPTYPE,
                                                 uint8_t                            ssap,
                                                 phFriNfc_Llcp_sPacketSequence_t*   sLlcpSequence,
                                                 uint8_t                            WFlag,
                                                 uint8_t                            IFlag,
                                                 uint8_t                            RFlag,
                                                 uint8_t                            SFlag,
                                                 uint8_t                            vs,
                                                 uint8_t                            vsa,
                                                 uint8_t                            vr,
                                                 uint8_t                            vra);

/*!
* \ingroup grp_fri_nfc
* \brief <b>Discover remote services SAP using SDP protocol</b>.
 */
NFCSTATUS phFriNfc_LlcpTransport_DiscoverServices( phFriNfc_LlcpTransport_t  *pLlcpTransport,
                                                   phNfc_sData_t             *psServiceNameList,
                                                   uint8_t                   *pnSapList,
                                                   uint8_t                   nListSize,
                                                   pphFriNfc_Cr_t            pDiscover_Cb,
                                                   void                      *pContext );

/**
* \ingroup grp_lib_nfc
* \brief <b>Get the local options of a socket</b>.
*
* This function returns the local options (maximum packet size and receive window size) used
* for a given connection-oriented socket. This function shall not be used with connectionless
* sockets.
*
* \param[out] pLlcpSocket           A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  psLocalOptions        A pointer to be filled with the local options of the socket.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_SocketGetLocalOptions(phFriNfc_LlcpTransport_Socket_t  *pLlcpSocket,
                                                       phLibNfc_Llcp_sSocketOptions_t   *psLocalOptions);


/**
* \ingroup grp_lib_nfc
* \brief <b>Get the local options of a socket</b>.
*
* This function returns the remote options (maximum packet size and receive window size) used
* for a given connection-oriented socket. This function shall not be used with connectionless
* sockets.
*
* \param[out] pLlcpSocket           A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  psRemoteOptions       A pointer to be filled with the remote options of the socket.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_SocketGetRemoteOptions(phFriNfc_LlcpTransport_Socket_t*   pLlcpSocket,
                                                        phLibNfc_Llcp_sSocketOptions_t*    psRemoteOptions);


 /**
* \ingroup grp_fri_nfc
* \brief <b>Create a socket on a LLCP-connected device</b>.
*
* This function creates a socket for a given LLCP link. Sockets can be of two types : 
* connection-oriented and connectionless. If the socket is connection-oriented, the caller
* must provide a working buffer to the socket in order to handle incoming data. This buffer
* must be large enough to fit the receive window (RW * MIU), the remaining space being
* used as a linear buffer to store incoming data as a stream. Data will be readable later
* using the phLibNfc_LlcpTransport_Recv function.
* The options and working buffer are not required if the socket is used as a listening socket,
* since it cannot be directly used for communication.
*
* \param[in]  pLlcpSocketTable      A pointer to a table of PHFRINFC_LLCP_NB_SOCKET_DEFAULT sockets.
* \param[in]  eType                 The socket type.
* \param[in]  psOptions             The options to be used with the socket.
* \param[in]  psWorkingBuffer       A working buffer to be used by the library.
* \param[out] pLlcpSocket           A pointer to a socket pointer to be filled with a
                                    socket found on the socket table.
* \param[in]  pErr_Cb               The callback to be called each time the socket
*                                   is in error.
* \param[in]  pContext              Upper layer context to be returned in the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_BUFFER_TOO_SMALL         The working buffer is too small for the MIU and RW
*                                            declared in the options.
* \retval NFCSTATUS_INSUFFICIENT_RESOURCES   No more socket handle available.
* \retval NFCSTATUS_FAILED                   Operation failed.  
* */
NFCSTATUS phFriNfc_LlcpTransport_Socket(phFriNfc_LlcpTransport_t                           *pLlcpSocketTable,
                                        phFriNfc_LlcpTransport_eSocketType_t               eType,
                                        phFriNfc_LlcpTransport_sSocketOptions_t*           psOptions,
                                        phNfc_sData_t*                                     psWorkingBuffer,
                                        phFriNfc_LlcpTransport_Socket_t                    **pLlcpSocket,
                                        pphFriNfc_LlcpTransportSocketErrCb_t               pErr_Cb,
                                        void*                                              pContext);

/**
* \ingroup grp_fri_nfc
* \brief <b>Close a socket on a LLCP-connected device</b>.
*
* This function closes a LLCP socket previously created using phFriNfc_LlcpTransport_Socket.
* If the socket was connected, it is first disconnected, and then closed.
*
* \param[in]  pLlcpSocket                    A pointer to a phFriNfc_LlcpTransport_Socket_t.

* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_Close(phFriNfc_LlcpTransport_Socket_t*   pLlcpSocket);


/**
* \ingroup grp_fri_nfc
* \brief <b>Bind a socket to a local SAP</b>.
*
* This function binds the socket to a local Service Access Point.
*
* \param[out] pLlcpSocket           A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  pConfigInfo           A port number for a specific socket
* \param TODO
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_ALREADY_REGISTERED       The selected SAP is already bound to another
                                             socket.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_Bind(phFriNfc_LlcpTransport_Socket_t    *pLlcpSocket,
                                      uint8_t                            nSap,
                                      phNfc_sData_t                      *psServiceName);

/**
* \ingroup grp_fri_nfc
* \brief <b>Listen for incoming connection requests on a socket</b>.
*
* This function switches a socket into a listening state and registers a callback on
* incoming connection requests. In this state, the socket is not able to communicate
* directly. The listening state is only available for connection-oriented sockets
* which are still not connected. The socket keeps listening until it is closed, and
* thus can trigger several times the pListen_Cb callback.
*
*
* \param[in]  pLlcpSocket        A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  pListen_Cb         The callback to be called each time the
*                                socket receive a connection request.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state to switch
*                                            to listening state.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_Listen(phFriNfc_LlcpTransport_Socket_t*          pLlcpSocket,
                                        pphFriNfc_LlcpTransportSocketListenCb_t   pListen_Cb,
                                        void*                                     pContext);

/**
* \ingroup grp_fri_nfc
* \brief <b>Accept an incoming connection request for a socket</b>.
*
* This functions allows the client to accept an incoming connection request.
* It must be used with the socket provided within the listen callback. The socket
* is implicitly switched to the connected state when the function is called.
*
* \param[in]  pLlcpSocket           A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  psOptions             The options to be used with the socket.
* \param[in]  psWorkingBuffer       A working buffer to be used by the library.
* \param[in]  pErr_Cb               The callback to be called each time the accepted socket
*                                   is in error.
* \param[in]  pAccept_RspCb         The callback to be called when the Accept operation is completed
* \param[in]  pContext              Upper layer context to be returned in the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_BUFFER_TOO_SMALL         The working buffer is too small for the MIU and RW
*                                            declared in the options.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_Accept(phFriNfc_LlcpTransport_Socket_t*             pLlcpSocket,
                                        phFriNfc_LlcpTransport_sSocketOptions_t*     psOptions,
                                        phNfc_sData_t*                               psWorkingBuffer,
                                        pphFriNfc_LlcpTransportSocketErrCb_t         pErr_Cb,
                                        pphFriNfc_LlcpTransportSocketAcceptCb_t      pAccept_RspCb,
                                        void*                                        pContext);

 /**
* \ingroup grp_fri_nfc
* \brief <b>Reject an incoming connection request for a socket</b>.
*
* This functions allows the client to reject an incoming connection request.
* It must be used with the socket provided within the listen callback. The socket
* is implicitly closed when the function is called.
*
* \param[in]  pLlcpSocket           A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  pReject_RspCb         The callback to be called when the Reject operation is completed
* \param[in]  pContext              Upper layer context to be returned in the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_Reject( phFriNfc_LlcpTransport_Socket_t*           pLlcpSocket,
                                            pphFriNfc_LlcpTransportSocketRejectCb_t   pReject_RspCb,
                                            void                                      *pContext);
/**
* \ingroup grp_fri_nfc
* \brief <b>Try to establish connection with a socket on a remote SAP</b>.
*
* This function tries to connect to a given SAP on the remote peer. If the
* socket is not bound to a local SAP, it is implicitly bound to a free SAP.
*
* \param[in]  pLlcpSocket           A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  nSap               The destination SAP to connect to.
* \param[in]  pConnect_RspCb     The callback to be called when the connection
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Connection operation is in progress,
*                                            pConnect_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_Connect( phFriNfc_LlcpTransport_Socket_t*           pLlcpSocket,
                                          uint8_t                                    nSap,
                                          pphFriNfc_LlcpTransportSocketConnectCb_t   pConnect_RspCb,
                                          void*                                      pContext);

/**
* \ingroup grp_fri_nfc
* \brief <b>Try to establish connection with a socket on a remote service, given its URI</b>.
*
* This function tries to connect to a SAP designated by an URI. If the
* socket is not bound to a local SAP, it is implicitly bound to a free SAP.
*
* \param[in]  pLlcpSocket        A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  psUri              The URI corresponding to the destination SAP to connect to.
* \param[in]  pConnect_RspCb     The callback to be called when the connection
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Connection operation is in progress,
*                                            pConnect_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_ConnectByUri(phFriNfc_LlcpTransport_Socket_t*           pLlcpSocket,
                                              phNfc_sData_t*                            psUri,
                                              pphFriNfc_LlcpTransportSocketConnectCb_t   pConnect_RspCb,
                                              void*                                     pContext);

/**
* \ingroup grp_lib_nfc
* \brief <b>Disconnect a currently connected socket</b>.
*
* This function initiates the disconnection of a previously connected socket.
*
* \param[in]  pLlcpSocket        A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  pDisconnect_RspCb  The callback to be called when the 
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Disconnection operation is in progress,
*                                            pDisconnect_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_Disconnect(phFriNfc_LlcpTransport_Socket_t*           pLlcpSocket,
                                            pphLibNfc_LlcpSocketDisconnectCb_t         pDisconnect_RspCb,
                                            void*                                      pContext);

/**
* \ingroup grp_fri_nfc
* \brief <b>Send data on a socket</b>.
*
* This function is used to write data on a socket. This function
* can only be called on a connection-oriented socket which is already
* in a connected state.
* 
*
* \param[in]  hSocket            Socket handle obtained during socket creation.
* \param[in]  psBuffer           The buffer containing the data to send.
* \param[in]  pSend_RspCb        The callback to be called when the 
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Reception operation is in progress,
*                                            pSend_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_Send(phFriNfc_LlcpTransport_Socket_t*             pLlcpSocket,
                                      phNfc_sData_t*                               psBuffer,
                                      pphFriNfc_LlcpTransportSocketSendCb_t        pSend_RspCb,
                                      void*                                        pContext);

/**
* \ingroup grp_fri_nfc
* \brief <b>Read data on a socket</b>.
*
* This function is used to read data from a socket. It reads at most the
* size of the reception buffer, but can also return less bytes if less bytes
* are available. If no data is available, the function will be pending until
* more data comes, and the response will be sent by the callback. This function
* can only be called on a connection-oriented socket.
* 
*
* \param[in]  hSocket            Socket handle obtained during socket creation.
* \param[in]  psBuffer           The buffer receiving the data.
* \param[in]  pRecv_RspCb        The callback to be called when the 
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Reception operation is in progress,
*                                            pRecv_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_Recv( phFriNfc_LlcpTransport_Socket_t*             pLlcpSocket,
                                       phNfc_sData_t*                               psBuffer,
                                       pphFriNfc_LlcpTransportSocketRecvCb_t        pRecv_RspCb,
                                       void*                                        pContext);



 /**
* \ingroup grp_lib_nfc
* \brief <b>Read data on a socket and get the source SAP</b>.
*
* This function is the same as phLibNfc_Llcp_Recv, except that the callback includes
* the source SAP. This functions can only be called on a connectionless socket.
* 
*
* \param[in]  pLlcpSocket        A pointer to a LlcpSocket created.
* \param[in]  psBuffer           The buffer receiving the data.
* \param[in]  pRecv_RspCb        The callback to be called when the 
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Reception operation is in progress,
*                                            pRecv_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_RecvFrom( phFriNfc_LlcpTransport_Socket_t                   *pLlcpSocket,
                                           phNfc_sData_t*                                    psBuffer,
                                           pphFriNfc_LlcpTransportSocketRecvFromCb_t         pRecv_Cb,
                                           void                                              *pContext);

/**
* \ingroup grp_fri_nfc
* \brief <b>Send data on a socket to a given destination SAP</b>.
*
* This function is used to write data on a socket to a given destination SAP.
* This function can only be called on a connectionless socket.
* 
*
* \param[in]  pLlcpSocket        A pointer to a LlcpSocket created.
* \param[in]  nSap               The destination SAP.
* \param[in]  psBuffer           The buffer containing the data to send.
* \param[in]  pSend_RspCb        The callback to be called when the 
*                                operation is completed.
* \param[in]  pContext           Upper layer context to be returned in
*                                the callback.
*
* \retval NFCSTATUS_SUCCESS                  Operation successful.
* \retval NFCSTATUS_INVALID_PARAMETER        One or more of the supplied parameters
*                                            could not be properly interpreted.
* \retval NFCSTATUS_PENDING                  Reception operation is in progress,
*                                            pSend_RspCb will be called upon completion.
* \retval NFCSTATUS_INVALID_STATE            The socket is not in a valid state, or not of 
*                                            a valid type to perform the requsted operation.
* \retval NFCSTATUS_NOT_INITIALISED          Indicates stack is not yet initialized.
* \retval NFCSTATUS_SHUTDOWN                 Shutdown in progress.
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_SendTo( phFriNfc_LlcpTransport_Socket_t             *pLlcpSocket,
                                         uint8_t                                     nSap,
                                         phNfc_sData_t*                              psBuffer,
                                         pphFriNfc_LlcpTransportSocketSendCb_t       pSend_RspCb,
                                         void*                                       pContext);
#endif  /*  PHFRINFC_LLCP_TRANSPORT_H    */
