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


/*!
* =============================================================================
* \file  phNfcInterface.h
* \brief Generic Interface Layer Function Definitions.
*
* Project: NFC-FRI-1.1
*
* $Date: Thu Feb 11 19:01:36 2010 $
* $Author: ing04880 $
* $Revision: 1.42 $
* $Aliases: NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
*
* =============================================================================
*/

#ifndef PHNFCINTERFACE_H /* */
#define PHNFCINTERFACE_H /* */

/**
*  \name NFC Inteface
*
* File: \ref phNfcInterface.h
*
*/

/*@{*/
#define PHNFCINTERFACE_FILEREVISION "$Revision: 1.42 $" /**< \ingroup grp_file_attributes */
#define PHNFCINTERFACE_FILEALIASES  "$Aliases: NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"   /**< \ingroup grp_file_attributes */
/*@}*/

/*
################################################################################
***************************** Header File Inclusion ****************************
################################################################################
*/

#include <phNfcTypes.h>
#include <phNfcHalTypes.h>


/*
################################################################################
****************************** Macro Definitions *******************************
################################################################################
*/

#define NFC_FSM_IN_PROGRESS 0x01U
#define NFC_FSM_COMPLETE    0x00U

#define NFC_FSM_CURRENT     0x00U
#define NFC_FSM_NEXT        0x01U

/* NFC Notification Types */

#define NFC_NOTIFY_INIT_COMPLETED       0x01
#define NFC_NOTIFY_INIT_FAILED          0xF1

#define NFC_NOTIFY_DEINIT_COMPLETED     0x02
#define NFC_NOTIFY_DEINIT_FAILED        0xF2

#define	NFC_NOTIFY_EVENT				0x70

#define NFC_NOTIFY_DEVICE_ACTIVATED     0x82
#define NFC_NOTIFY_DEVICE_DEACTIVATED   0x83

#define NFC_NOTIFY_SEND_COMPLETED       0x03
#define NFC_NOTIFY_SEND_ERROR           0xF3

#define NFC_NOTIFY_RECV_COMPLETED       0x04
#define NFC_NOTIFY_RECV_ERROR           0xF4
#define	NFC_NOTIFY_RECV_EVENT			0x74
#define NFC_NOTIFY_RECV_CANCELLED       0x34

#define NFC_NOTIFY_TRANSCEIVE_COMPLETED 0x05
#define NFC_NOTIFY_TRANSCEIVE_ERROR     0xF5

#define NFC_NOTIFY_POLL_ENABLED         0x06
#define NFC_NOTIFY_POLL_RESTARTED		0x16
#define NFC_NOTIFY_POLL_DISABLED        0x26
#define NFC_NOTIFY_CONFIG_SUCCESS		0x36
#define NFC_NOTIFY_CONFIG_ERROR			0xF6

#define NFC_NOTIFY_TARGET_DISCOVERED    0x10
#define NFC_NOTIFY_DISCOVERY_ERROR      0xFA
#define NFC_NOTIFY_TARGET_RELEASED      0x11
#define NFC_NOTIFY_TARGET_CONNECTED     0x12
#define NFC_NOTIFY_TARGET_PRESENT       0x13
#define NFC_NOTIFY_TARGET_REACTIVATED   0x14
#define NFC_NOTIFY_CONNECT_FAILED       0xFC
#define NFC_NOTIFY_TARGET_DISCONNECTED  0x15
#define NFC_NOTIFY_DISCONNECT_FAILED    0xFD

#define	NFC_NOTIFY_TRANSACTION			0x07

#define	NFC_NOTIFY_RESULT				0x08

#define NFC_NOTIFY_DEVICE_ERROR         0xFEU
#define NFC_NOTIFY_ERROR                0xFFU


#define BYTE_SIZE                       0x08U
#define BYTE_MASK                       0xFFU
/* HCI GET and SET BITS Macros */
#define MASK_BITS8(p,l) \
    ( ( (((uint8_t)(p))+((uint8_t)(l)))<=BYTE_SIZE )? \
    (~(0xFFU<<((p)+(l))) & (0xFFU<<(p))):(0U) )
#ifdef MASK_BITS
#define GET_BITS8(num,p,l) \
    ( ((((uint8_t)(p))+((uint8_t)(l)))<=BYTE_SIZE)? \
    (((num)& (MASK_BITS8(p,l)))>>(p)):(0U) )
#else
#define GET_BITS8(num,p,l) \
    ( ((((p)+(l))<=BYTE_SIZE))? \
    (((num)>>(p))& (~(0xFFU<<(l)))):(0U) )
#endif
#define SET_BITS8(num,p,l,val) \
    (  ((((uint8_t)(p))+((uint8_t)(l)))<=BYTE_SIZE)? \
    (((num)& (~MASK_BITS8(p,l)))|((val)<<(p))):(0U))

/*
################################################################################
******************** Enumeration and Structure Definition **********************
################################################################################
*/


enum phNfcIF_eExecution{
    NFC_EXEC_NORMAL                   = 0x00, /**<  Normal Execution Sequence */
    NFC_EXEC_CALLBACK                 = 0x01, /**<  Callback Execution Sequence */
    NFC_EXEC_UNKNOWN                  = 0xFF /**<  Callback Execution Sequence */
};


typedef enum phNfc_eModeType{
	MODE_ON		= 0x00U,       /**<  Switches the particular feature ON*/
	MODE_OFF				   /**<  Switches the particular feature OFF*/
}phNfc_eModeType_t;

/**
 * State Structure to hold the State Information
 *
 * This structure holds the state Information of a specified 
 * Layer .
 * 
 */

typedef struct phNfc_sState
{
    uint8_t     cur_state;
    uint8_t     transition;
    uint8_t     next_state;
    /* uint8_t      event; */

}phNfc_sState_t;



/**
 * Transaction Completion Information Structure 
 *
 * This structure holds the completion callback information of the 
 * transaction passed from the lower layer to the upper layer 
 * along with the completion callback.
 */

typedef struct phNfc_sTransactionInfo
{
	/* Returns the status of the Transaction Completion routine */
    NFCSTATUS           status;
	/* Indicates the Type of the Transaction */
	uint8_t				type;
	/* To contain more Transaction Notification specific info */
	void                *info;
	/* The data response from the Transaction */
    uint8_t             *buffer;
	/* The size of the data response from the Transaction */
    uint16_t             length;
}phNfc_sTransactionInfo_t;

/**
 * Notification Information Structure 
 *
 * This structure holds the notification callback information passed from 
 * the lower layer to the upper layer along with the notification callback.
 */

typedef struct phNfc_sCompletionInfo
{
    /* Returns the status of the completion routine */
    NFCSTATUS           status;

	/* Indicates the Type of the Information
	 * associated with the completion
	 */
	uint8_t				type;

	/* To contain more completion specific info */
    void                *info;

}phNfc_sCompletionInfo_t;


/**
 *  Notification Information 
 * 
 */
typedef struct phNfc_sNotificationInfo
{
	/* Returns the status of the Notification routine */
	NFCSTATUS						status;
	/* Indicates the Type of the Notification */
	phHal_eNotificationType_t		type;
	/* To contain more Notification specific info */
	void							*info;

}phNfc_sNotificationInfo_t;


/*
################################################################################
********************* Callback Function Type Definition ************************
################################################################################
*/

/**
* Interface Notification Callback
*
* This callback notifies the occurrance of an event in the Lower Interface.
*
* \param [in] pContext  Context for the Callback Function
* \param [in] pHwRef    Pointer to the Hardware Reference
* \param [in] type      Type of the Notification sent
* \param [out] pInfo    Pointer to the Transaction Information Structure
*                       which contains the Status of the operation, data
*                       obtained or sent and size of the data sent or received
*/

typedef void (*pphNfcIF_Notification_CB_t) (
                                                void *pContext,
                                                void *pHwRef,
                                                uint8_t type,
                                                void *pInfo
                                             );

/**
* asynchronous Interface Transaction Completion callback
*
* This callback signals the completion of the asynchronous send or receive
* operation. The number of bytes sent or recieved is returned back.
*
* \param [in] pContext  Context for the Callback Function
* \param [in] pHwRef    Pointer to the Hardware Reference
* \param [out] pInfo    Pointer to the Transaction Information Structure
*                       which contains the Status of the operation, data
*                       obtained or sent and size of the data sent or received
*/

typedef void (*pphNfcIF_Transact_Completion_CB_t) (
                                                    void *pContext,
                                                    void *pHwRef,
                                                    phNfc_sTransactionInfo_t *pInfo
                                                );

/*
################################################################################
********************** Generic Interface Function Prototype ********************
################################################################################
*/

/**
 * Generic NFC Interface Function Type .
 *
 * \param [in] pContext     Context pointer for the Generic Interface.
 * \param [in] pHwRef       pointer for the device interface link information.
 */

typedef NFCSTATUS (*pphNfcIF_Interface_t) (
                                                    void *pContext,
                                                    void *pHwRef
                                          );
/**
 * Data Transaction between the lower layer interface
 *
 * Sends or Receives the given amount of data to the lower layer. 
 * The call returns immediately and the registered callback is 
 * called when all data has been written.
 * <p>
 *
 * @note If the interface is not initialized the function does nothing.
 *
 * \param [in] pContext     Context pointer for sending the data.
 * \param [in] pHwRef       pointer for the device interface link information.
 * \param[in]  data         pointer to data buffer containing the data to be sent or
 *                          to be received. The data pointer is valid at least until
 *                          the registered callback is called.
 * \param[in] length        length of the data to be sent or to be received.
 */

typedef NFCSTATUS (*pphNfcIF_Transact_t) (
                                                void *pContext,
                                                void *pHwRef,
                                                uint8_t *data,
                                                uint16_t length
                                        );


/**
 * Generic Interface structure with the Lower Layer
 *
 * This structure holds the context and function pointers of all functions
 * required to interface with the Lower Layers.
 */

typedef struct phNfc_sLowerIF
{
    void                        *pcontext;
    pphNfcIF_Interface_t        init;
    pphNfcIF_Interface_t        release;
    pphNfcIF_Transact_t         send;
    pphNfcIF_Transact_t         receive;
    pphNfcIF_Transact_t         receive_wait;
    pphNfcIF_Interface_t        transact_abort;
    pphNfcIF_Interface_t        unregister;
} phNfc_sLowerIF_t,*pphNfc_sLowerIF_t;


/**
 * Generic Callback interface structure for the Lower layer.
 *
 * This structure holds the callback function pointers of the functions that
 * performs the completion of a particular operation. These functions are used
 * by the Lower Layer interface to convey the completion of an operation.
 */

typedef struct phNfcIF_sCallBack
{
	/**<Holds context info to be sent to lower layer*/
    void                                        *pif_ctxt;
	/**<Callback notifies occurrance of event in Lower Interface*/
    pphNfcIF_Notification_CB_t                  notify;
	/**<asynchronous Interface Transaction Completion callback*/
    pphNfcIF_Transact_Completion_CB_t           send_complete;
    pphNfcIF_Transact_Completion_CB_t           receive_complete;

} phNfcIF_sCallBack_t ,*pphNfcIF_sCallBack_t ;


/**
 * Interface Reference structure.
 *
 * This structure holds the reference parameters, callback function pointers and
 * lower interface functions .
 */

typedef struct phNfcIF_sReference
{
	/**<Generic Interface structure with the Lower Layer*/	
    phNfc_sLowerIF_t                        *plower_if;
	/**<pointer to the data to be sent*/
    uint8_t                                 *tx_data;
	/**<pointer to the data to be received*/
    uint8_t                                 *rx_data;
}phNfcIF_sReference_t, *pphNfcIF_sReference_t;


/*
################################################################################
********************** Register Function Type Definition ***********************
################################################################################
*/

/**
* Registers the interface functions and passes the callback functions to the
* lower layer.
*
* This function passes the callback functions of the interface to the
* lower interface and the lower interface registers its functions.
*/

typedef NFCSTATUS ( *pphNfcIF_Register_t) (
                                phNfcIF_sReference_t        *psReference,
                                phNfcIF_sCallBack_t         if_callback,
                                void                        *psIFConfig
                                );


/**
 * Layer Specific Configuration structure.
 *
 * This structure holds the Lower Layer Name and the registry function for registering
 * the lower layer interface functions .
 */


typedef struct phNfcLayer_sCfg
{
    uint8_t                 layer_index;
    uint8_t                 *layer_name;
    pphNfcIF_Register_t     layer_registry;
    struct phNfcLayer_sCfg  *layer_next;
}phNfcLayer_sCfg_t, *pphNfcLayer_sCfg_t;


#endif /* PHNFCINTERFACE_H */

