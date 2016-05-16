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
 * \file  phFriNfc_LlcpTransport_Connection.h
 * \brief 
 *
 * Project: NFC-FRI
 *
 */
#ifndef PHFRINFC_LLCP_TRANSPORT_CONNECTION_H
#define PHFRINFC_LLCP_TRANSPORT_CONNECTION_H
/*include files*/
#include <phNfcTypes.h>
#include <phNfcStatus.h>
#include <phFriNfc.h>

#include <phFriNfc_Llcp.h>

void Handle_ConnectionOriented_IncommingFrame(phFriNfc_LlcpTransport_t      *pLlcpTransport,
                                              phNfc_sData_t                 *psData,
                                              uint8_t                       dsap,
                                              uint8_t                       ptype,
                                              uint8_t                       ssap);

NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_HandlePendingOperations(phFriNfc_LlcpTransport_Socket_t *pSocket);

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
NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_SocketGetLocalOptions(phFriNfc_LlcpTransport_Socket_t  *pLlcpSocket,
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
NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_SocketGetRemoteOptions(phFriNfc_LlcpTransport_Socket_t*   pLlcpSocket,
                                                                           phLibNfc_Llcp_sSocketOptions_t*    psRemoteOptions);

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
NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_Close(phFriNfc_LlcpTransport_Socket_t*   pLlcpSocket);

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
NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_Listen(phFriNfc_LlcpTransport_Socket_t*          pLlcpSocket,
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
NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_Accept(phFriNfc_LlcpTransport_Socket_t*             pLlcpSocket,
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
NFCSTATUS phLibNfc_LlcpTransport_ConnectionOriented_Reject( phFriNfc_LlcpTransport_Socket_t*           pLlcpSocket,
                                                            pphFriNfc_LlcpTransportSocketRejectCb_t   pReject_RspCb,
                                                            void                                      *pContext);

/**
* \ingroup grp_fri_nfc
* \brief <b>Try to establish connection with a socket on a remote SAP</b>.
*
* This function tries to connect to a given SAP on the remote peer. If the
* socket is not bound to a local SAP, it is implicitly bound to a free SAP.
*
* \param[in]  pLlcpSocket        A pointer to a phFriNfc_LlcpTransport_Socket_t.
* \param[in]  nSap               The destination SAP to connect to.
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
* \retval NFCSTATUS_FAILED                   Operation failed.
*/
NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_Connect( phFriNfc_LlcpTransport_Socket_t*           pLlcpSocket,
                                                             uint8_t                                    nSap,
                                                             phNfc_sData_t*                             psUri,
                                                             pphFriNfc_LlcpTransportSocketConnectCb_t   pConnect_RspCb,
                                                             void*                                      pContext);

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
NFCSTATUS phLibNfc_LlcpTransport_ConnectionOriented_Disconnect(phFriNfc_LlcpTransport_Socket_t*           pLlcpSocket,
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
* \param[in]  pLlcpSocket        A pointer to a phFriNfc_LlcpTransport_Socket_t.
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
NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_Send(phFriNfc_LlcpTransport_Socket_t*             pLlcpSocket,
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
* \param[in]  pLlcpSocket        A pointer to a phFriNfc_LlcpTransport_Socket_t.
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
NFCSTATUS phFriNfc_LlcpTransport_ConnectionOriented_Recv( phFriNfc_LlcpTransport_Socket_t*             pLlcpSocket,
                                                          phNfc_sData_t*                               psBuffer,
                                                          pphFriNfc_LlcpTransportSocketRecvCb_t        pRecv_RspCb,
                                                          void*                                        pContext);
#endif /* PHFRINFC_LLCP_TRANSPORT_CONNECTION_H */
