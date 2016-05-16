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

* \file  phDal4Nfc.h
* \brief Common DAL for the upper layer.
*
* Project: NFC-FRI-1.1
*
* $Date: Tue Nov 10 13:56:45 2009 $
* $Author: ing07299 $
* $Revision: 1.38 $
* $Aliases: NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
*
*/

#ifndef PHDAL4NFC_H
#define PHDAL4NFC_H

/**
*  \name DAl4 NFC
*
* File: \ref phDal4Nfc.h
*
*/
/*@{*/
#define PH_DAL4NFC_FILEREVISION "$Revision: 1.38 $" /**< \ingroup grp_file_attributes */
#define PH_DAL4NFC_FILEALIASES    "$Aliases: NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"
 /**< \ingroup grp_file_attributes */
/*@}*/
/*************************** Includes *******************************/
/** \defgroup grp_nfc_dal DAL Component
 *
 *
 *
 */
#include <hardware/nfc.h>
/**< Basic type definitions */
#include <phNfcTypes.h>
/**< Generic Interface Layer Function Definitions */
#include <phNfcInterface.h>
/*********************** End of includes ****************************/

/***************************** Macros *******************************/
 /**< Used for messaging by DAL as well as Upper Layers */
#define PH_DAL4NFC_MESSAGE_BASE  PH_LIBNFC_DEFERREDCALL_MSG

/************************ End of macros *****************************/


/********************* Structures and enums *************************/

/**
 * \ingroup grp_nfc_dal
 *
 * DAL context : This contains the information of the upper layer callback
 * and hardware reference.
 */
typedef struct phDal4Nfc_SContext
{
	phNfcIF_sCallBack_t		cb_if;		/**<Callback info registered by upper layer*/
	volatile uint8_t		hw_valid;	/**<Flag - shows Hardware present or not */
	void					*pHwRef;	/**<Hardware Reference*/
	nfc_pn544_device_t		*pDev;		/**<Android HAL reference*/
}phDal4Nfc_SContext_t,*pphDal4Nfc_SContext_t;

/**
 * \ingroup grp_nfc_dal
 *
 * DAL enum for Messages : This contains the enums used for
 * posting messages to the application.
 */
typedef enum phDal4Nfc_Messages_en
{
    /**<Read message type used to post Read DAL Message to dispatch routine.Dispatch routine
	calls read callback registered by upper layer */
    PHDAL4NFC_READ_MESSAGE = 0,
    /**<Readwait message type used to post Read wait DAL Message to dispatch routine.Dispatch routine
	calls read wait callback registered by upper layer */
	PHDAL4NFC_READWAIT_MESSAGE,
	/**<Write message type used to post write DAL Message to dispatch routine.Dispatch routine
	calls write wait callback registered by upper layer */
    PHDAL4NFC_WRITE_MESSAGE,
	/**<Notify message type used to post Notify DAL Message to dispatch routine.Dispatch routine
	calls notify callback registered by upper layer */
    PHDAL4NFC_NOTIFY_MESSAGE
}phDal4Nfc_Messages_en_t;

/**
 * \ingroup grp_nfc_dal
 *
 * \brief DAL Message structure definition.This structure contains
 *
 * This structure contains details like  message type,read ,read wait and write callbacks
 * reference details as registered by upper layer.
 */
typedef struct phDal4Nfc_Message
{
    /**<Refenrece to context.Context can be DAL context  */
	void                                *pContext;
    /**<Reference to hardware reference strucutre */
	void                                *pHwRef;
	/**<DAL message of type \ref phDal4Nfc_Messages_en_t*/
    phDal4Nfc_Messages_en_t             eMsgType;
	/**<Transaction specific information of type \ref phNfc_sTransactionInfo_t*/
    phNfc_sTransactionInfo_t            transactInfo;
	/**<Reference to read callback,registered by upper layer.This is of type \ref pphNfcIF_Transact_Completion_CB_t*/
    pphNfcIF_Transact_Completion_CB_t   readCbPtr;
	/**<Reference to write callback ,registered by upper layer.This is of type \ref pphNfcIF_Transact_Completion_CB_t*/
    pphNfcIF_Transact_Completion_CB_t   writeCbPtr;
} phDal4Nfc_Message_t,*pphDal4Nfc_Message_t;

typedef phLibNfc_sConfig_t phDal4Nfc_sConfig_t;
typedef phLibNfc_sConfig_t *pphDal4Nfc_sConfig_t;

/****************** End of structures and enums *********************/

/******************** Function declarations *************************/

/**
 * \ingroup grp_nfc_dal
 *
 *\brief Allows to register upper layer with DAL layer.
 * This API allows upper layer to register with DAL layer.As part of registration
 *<br>1.Exports DAL interfaces and DAL layer context to upper layer.
 *Exported DAL interfaces are :
 *<br><br>.phDal4Nfc_Shutdown
 *<br><br>.phDal4Nfc_Write
 *<br><br>.phDal4Nfc_Read
 *<br><br>.phDal4Nfc_ReadWait
 *<br><br>.phDal4Nfc_ReadWaitCancel
 *<br><br>phDal4Nfc_Unregister
 *<br><br>.Registeres upper layer callbacks and upper layer context with DAL layer.
 *For details refer to \ref phNfcIF_sReference_t.
 *Registration details are valid unless upper layer calls \ref phDal4Nfc_Unregister()
 or \ref phDal4Nfc_Shutdown called.

 * \param[in,out] psRefer   holds  DAL exported interface references once registration
 *							sucessful.This also contains transmit and receive buffer
 *							references.
 *
 * \param[in]     if_cb		Contains upper layer callback reference details, which are used
 *							by DAL layer during callback notification.
 *							These callbacks gets registered with DAL layer.
 *

 * \param[out]	 psIFConf  Currently this parameter not used.This  parameter to be other than NULL.
 *
 *
 * \retval NFCSTATUS_SUCCESS             Operation is successful.
 * \retval NFCSTATUS_INVALID_PARAMETER   At least one parameter of the function is invalid.
 *
 *\msc
 *ClientApp,UpperLayer,phDal4Nfc;
 *ClientApp=>phDal4Nfc [label="phDal4Nfc_Config()",URL="\ref phDal4Nfc_Config"];
 *ClientApp<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *UpperLayer=>phDal4Nfc [label="phDal4Nfc_Register()",URL="\ref phDal4Nfc_Register"];
 *UpperLayer<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *\endmsc
 */

 extern
 NFCSTATUS
 phDal4Nfc_Register(
                 phNfcIF_sReference_t   *psRefer,
                 phNfcIF_sCallBack_t    if_cb,
                 void *psIFConf
                 );


/**
 * \ingroup grp_nfc_dal
 *
 * \brief  Allows upper layer to unregister with DAL layer.
 * This interface allows to unregister upper layer callback interfaces with DAL layer.
 * \note: Once this this API is called DAL layer stops notifying upper layer callbacks in case
 * any events reported within DAL.
 *
 * \param[in]  pContext        DAL context is provided by the upper layer.
 *                             The DAL context earlier was given to the
 *                             upper layer through the \ref
 *                             \e phDal4Nfc_Register() function.
 * \param[in]  pHwRef		   for future use .Currently this parameter is not used.
 *                             This needs to be other than NULL.
 *
 * \retval NFCSTATUS_SUCCESS             Unregistration successful.
 * \retval NFCSTATUS_INVALID_PARAMETER   At least one parameter of the function is invalid.
 *
 *\msc
 *ClientApp,UpperLayer,phDal4Nfc;
 *ClientApp=>phDal4Nfc [label="phDal4Nfc_Config()",URL="\ref phDal4Nfc_Config"];
 *ClientApp<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *UpperLayer=>phDal4Nfc [label="phDal4Nfc_Register()",URL="\ref phDal4Nfc_Register"];
 *UpperLayer<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *--- [label="Upper layer can issue Unregistration later"];
 *UpperLayer=>phDal4Nfc [label="phDal4Nfc_Unregister()",URL="\ref phDal4Nfc_Unregister"];
 *UpperLayer<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *\endmsc
 */


 extern
 NFCSTATUS
 phDal4Nfc_Unregister(
                   void   *pContext,
                   void   *pHwRef
                   );


/**

 * \ingroup grp_nfc_dal
 *
 *\brief This interface allows to initialize DAL layer.
 * This API implements initialization of DAL layer. This includes :
 *
 * <br><br>. Initialize parameters for HW Interface.
 *<br><br>. Initializing read and writer threads.
 *<br><br>. Initializing read and write task specific events and event specific configurations.
 *<br><br>. Initializing DAL layer specific details.
 *
 * \param[in]  pContext        DAL context provided by the upper layer.
 *                             The DAL context will be exported to the
 *							   upper layer via upper layer registration interface.
 * \param[in]  pHwRef          information of the hardware
 *
 * \retval NFCSTATUS_SUCCESS            DAL initialization successful.
 * \retval NFCSTATUS_INVALID_DEVICE     The device is not enumerated or the
 *                                      Hardware Reference points to a device
 *										which does not exist. Alternatively,
 *                                      also already open devices produce this
 *                                      error.
 * \retval NFCSTATUS_INVALID_PARAMETER  At least one parameter of the function
 *                                      is invalid.
 *
 *\msc
 *ClientApp,UpperLayer,phDal4Nfc;
 *ClientApp=>phDal4Nfc [label="phDal4Nfc_Config()",URL="\ref phDal4Nfc_Config"];
 *ClientApp<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *UpperLayer=>phDal4Nfc [label="phDal4Nfc_Register()",URL="\ref phDal4Nfc_Register"];
 *UpperLayer<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *UpperLayer=>phDal4Nfc [label="phDal4Nfc_Init()",URL="\ref	phDal4Nfc_Init"];
 *UpperLayer<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *--- [label="DAL intialised ,read for read /write operations"];
 *\endmsc
 */

 extern
 NFCSTATUS
 phDal4Nfc_Init(
             void  *pContext,
             void  *pHwRef
             );


/**
 * \ingroup grp_nfc_dal
 *
 * \brief This API implements Deintialisation of DAL layer.
 *
 *This API implements Deintialisation of DAL layer. It includes :
 *<br><br>.Releases all the resources.( context,memory resources,read/write buffers).
 *<br><br>.closes COMxx port which is used during DAL session.
 *<br><br>.Terminates Reader and writer tasks.
 *
 * \param[in]  pContext        DAL context is provided by the upper layer.
 *                             The DAL context earlier was given to the
 *                             upper layer through the
 *                             \ref \e phDal4Nfc_Register() function
 * \param[in]  pHwRef           hardware reference context.
 *
 * \retval NFCSTATUS_SUCCESS            DAL shutdown successful
 * \retval NFCSTATUS_FAILED             DAL shutdown failed(example.unable to
 *                                      suspend thread, close HW Interface etc.)
 * \retval NFCSTATUS_INVALID_PARAMETER  At least one parameter of the function
 *                                      is invalid.
 *
 *\msc
 *ClientApp,UpperLayer,phDal4Nfc;
 *ClientApp=>phDal4Nfc [label="phDal4Nfc_Config()",URL="\ref phDal4Nfc_Config"];
 *ClientApp<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *UpperLayer=>phDal4Nfc [label="phDal4Nfc_Register()",URL="\ref phDal4Nfc_Register"];
 *UpperLayer<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *UpperLayer=>phDal4Nfc [label="phDal4Nfc_Init()",URL="\ref	phDal4Nfc_Init"];
 *UpperLayer<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *--- [label="Perform read write operation"];
 *--- [label="DAL can be shutdown during upper layer deinit sequence"];
 *UpperLayer=>phDal4Nfc [label="phDal4Nfc_Shutdown()",URL="\ref phDal4Nfc_Shutdown"];
 *ClientApp<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *\endmsc
 */

 extern
 NFCSTATUS
 phDal4Nfc_Shutdown(
                 void *pContext,
                 void *pHwRef
                 );

/**
 * \ingroup grp_nfc_dal
 *
 * \brief Allows to write data block to HW Interface.
 *
 * This asynchronous function writes the given block of data to the driver.
 * This interface enables writer thread in case their is no write requests pending and returns
 * sucessfully.Once writer thread completes write operation, it notifies upper layer using callback
 * mechanism .
 * \note writer thread notifies upper layer callback notified using  windows messaging  mechanism
 * under deferred call context.
 *
 *
 * \param[in]  pContext        DAL context is provided by the upper layer.
 *                             The DAL
 *                             context earlier was given to the upper layer
 *                             through the \ref \e phDal4Nfc_Register() function
 * \param[in]  pHwRef          information of the hardware.
 * \param[in]  pBuffer         The information given by the upper layer to
 *                             send it to the lower layer
 * \param[in]  length          The length of pLlc_Buf, that needs to be sent
 *                             to the lower layer is given by the upper layer
 *
 * \retval NFCSTATUS_PENDING                If the command is yet to be process.
 * \retval NFCSTATUS_BUSY                   BUSY with previous write operation
 * \retval NFCSTATUS_INVALID_DEVICE         The device has not been opened or has
 *                                          been disconnected meanwhile
 * \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function
 *                                          is invalid.
 *
 *\msc
 *ClientApp,UpperLayer,phDal4Nfc;
*--- [label="Configure,intialise DAL layer and Register with DAL "];
 *UpperLayer=>phDal4Nfc	[label="phDal4Nfc_Write()",URL="\ref  phDal4Nfc_Write()"];
 *UpperLayer<<phDal4Nfc	[label="NFCSTATUS_PENDING"];
 *--- [label="DAL posts write message to main thread under deferred call context"];
 *phDal4Nfc=>phDal4Nfc [label="phDal4Nfc_DeferredCall()",URL="\ref	phDal4Nfc_DeferredCall()"];
 *ClientApp=>phDal4Nfc	[label="phDal4Nfc_DeferredCb()",URL="\ref  phDal4Nfc_DeferredCb()"];
 *phDal4Nfc=>UpperLayer	[label="send_complete",URL="\ref phDal4Nfc_DeferredCb()"];
 *ClientApp<<phDal4Nfc	[label="NFCSTATUS_SUCCESS"];
 *\endmsc
 */
 extern
 NFCSTATUS
 phDal4Nfc_Write(
             void *pContext,
             void *pHwRef,
             uint8_t *pBuffer,
             uint16_t length
             );


/**
 * \ingroup grp_nfc_dal
 *
 * \brief Allows to Read data block from HW Interface.
 *
 * This asynchronous function reads the data from the driver in which length
 * and the required buffer are sent by upper layer. This interface enables
 * reader thread in case there is no read requests pending and returns sucessfully.
 * Once read operation is complete, it notifies  to upper layer through callback 
 * registered in the \b \e phDal4Nfc_Register() function.
 *
 *
 * \param[in]  pContext        DAL context is provided by the upper layer.
 *                             The DAL context earlier was given to the
 *                             upper layer through the
 *                             \b \e phDal4Nfc_Register() function
 * \param[in]  pHwRef          Information of the hardware
 * \param[in]  pBuffer         The information given by the upper layer to
 *                             receive data from the lower layer
 * \param[in]  length          The length of pBuffer given by the upper layer
 *
 * \retval NFCSTATUS_PENDING                If the command is yet to be processed.
 * \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function
 *                                          is invalid.
 * \retval NFCSTATUS_INVALID_DEVICE         The device has not been opened or has
 *                                          been disconnected meanwhile
 *
 *\msc
 *ClientApp,UpperLayer,phDal4Nfc;
 *--- [label="Configure,intialise DAL layer and Register with DAL "];
 *UpperLayer=>phDal4Nfc	[label="phDal4Nfc_Write()",URL="\ref phDal4Nfc_Write()"];
 *UpperLayer<<phDal4Nfc	[label="NFCSTATUS_PENDING"];
 *--- [label="DAL posts write message to main thread under deferred call context"];
 *phDal4Nfc=>phDal4Nfc [label="phDal4Nfc_DeferredCall()",URL="\ref	phDal4Nfc_DeferredCall()"];
 *ClientApp=>phDal4Nfc	[label="phDal4Nfc_DeferredCb()",URL="\ref  phDal4Nfc_DeferredCb()"];
 *phDal4Nfc=>UpperLayer	[label="send_complete",URL="\ref phDal4Nfc_DeferredCb()"];
 *ClientApp<<phDal4Nfc	[label="NFCSTATUS_SUCCESS"];
 *--- [label="upper layer can issue read request"];
 *UpperLayer=>phDal4Nfc	[label="phDal4Nfc_Read()",URL="\ref phDal4Nfc_Read()"];
 *UpperLayer<<phDal4Nfc	[label="NFCSTATUS_PENDING"];
 *--- [label="DAL posts read message to main thread under deferred call context"];
 *phDal4Nfc=>phDal4Nfc [label="phDal4Nfc_DeferredCall()",URL="\ref	phDal4Nfc_DeferredCall()"];
 *ClientApp=>phDal4Nfc	[label="phDal4Nfc_DeferredCb()",URL="\ref  phDal4Nfc_DeferredCb()"];
 *phDal4Nfc=>UpperLayer	[label="receive_complete",URL="\ref  phDal4Nfc_DeferredCb()"];
 *ClientApp<<phDal4Nfc	[label="NFCSTATUS_SUCCESS"];

 *\endmsc
 */

 extern
 NFCSTATUS
 phDal4Nfc_Read(
            void *pContext,
            void *pHwRef,
            uint8_t *pBuffer,
            uint16_t length
            );

/**
 * \ingroup grp_nfc_dal
 *
 * \brief Allows to wait before reading data block from HW Interface.
 *
 * This asynchronous function waits before reading the data from the
 * driver in which length  and the required buffer are sent by upper layer.  
 * This interface  enables reader thread  to wait for predefined time period
 * (predefined time period is configurable by the DAL implementer) to complete  
 * read request.Once read operation is complete, it notifies to upper layer 
 * through callback registered in the \b \e phDal4Nfc_Register()
 * function. Read request is expected to get complete within this time.
 *
 * \param[in]  pContext        DAL context is provided by the upper layer.
 *                             The DAL context earlier was given to the
 *							   upper layer through the
 *                             \b \e phDal4Nfc_Register() function
 * \param[in]  pHwRef          Information of the hardware
 * \param[in]  pBuffer         The information given by the upper layer to
 *                             receive data from the lower layer
 * \param[in]  length          The length of pBuffer given by the upper layer
 *
 * \retval NFCSTATUS_SUCCESS                DAL receive successful
 * \retval NFCSTATUS_BUSY                   BUSY with previous receive operation
 * \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the
 *                                          function is invalid.
 * \retval NFCSTATUS_INVALID_DEVICE         The device has not been opened or has
 *                                          been disconnected meanwhile
 *
 *
 *\msc
 *ClientApp,UpperLayer,phDal4Nfc;
 *--- [label="Configure,intialise DAL layer and Register with DAL "];
 *UpperLayer=>phDal4Nfc	[label="phDal4Nfc_Write()",URL="\ref  phDal4Nfc_Write()"];
 *UpperLayer<<phDal4Nfc	[label="NFCSTATUS_PENDING"];
 *--- [label="DAL posts write message to main thread under deferred call context"];
 *phDal4Nfc=>phDal4Nfc [label="phDal4Nfc_DeferredCall()",URL="\ref	phDal4Nfc_DeferredCall()"];
 *ClientApp=>phDal4Nfc	[label="phDal4Nfc_DeferredCb()",URL="\ref  phDal4Nfc_DeferredCb()"];
 *phDal4Nfc=>UpperLayer	[label="send_complete",URL="\ref phDal4Nfc_DeferredCb()"];
 *ClientApp<<phDal4Nfc	[label="NFCSTATUS_SUCCESS"];
 *--- [label="upper layer can issue read wait request "];
 *UpperLayer=>phDal4Nfc	[label="phDal4Nfc_ReadWait()",URL="\ref phDal4Nfc_ReadWait()"];
 *UpperLayer<<phDal4Nfc	[label="NFCSTATUS_PENDING"];
 *--- [label="DAL posts read wait message to main thread under deferred call context"];
 *phDal4Nfc=>phDal4Nfc [label="phDal4Nfc_DeferredCall()",URL="\ref	phDal4Nfc_DeferredCall()"];
 *ClientApp=>phDal4Nfc	[label="phDal4Nfc_DeferredCb()",URL="\ref  phDal4Nfc_DeferredCb()"];
 *phDal4Nfc=>UpperLayer	[label="receive_complete",URL="\ref  phDal4Nfc_DeferredCb()"];
 *ClientApp<<phDal4Nfc	[label="NFCSTATUS_SUCCESS"];
 *
 *\endmsc
 */


 extern
 NFCSTATUS
 phDal4Nfc_ReadWait(
                void *pContext,
                void *pHwRef,
                uint8_t *pBuffer,
                uint16_t length
                );


/**
 * \ingroup grp_nfc_dal
 *
 *\brief  Aborts read wait opertaion.
 *
 * This asynchronous function issues cancellation of the outstanding 
 * \b \e phDal4Nfc_ReadWait request.If upper layer wants to cancel the 
 * ongoing read wait operation this function is used. e.g. to abort the
 * transactions.
 *
 * \param[in]  pContext        DAL context is provided by the upper layer.
 *                             The DAL context earlier was given to the
 *                             upper layer through the
 *                             \b \e phDal4Nfc_Register() function
 * \param[in]  pHwRef          Information of the hardware
 *
 * \retval NFCSTATUS_SUCCESS                    DAL receive successful
 * \retval NFCSTATUS_INVALID_PARAMETER          At least one parameter of the
 *                                              function is invalid.
 * \retval NFCSTATUS_INVALID_DEVICE             The device has not been opened
 *                                              or has been disconnected meanwhile
 * \retval NFCSTATUS_BOARD_COMMUNICATION_ERROR  A board communication error occurred
                                                (e.g. configuration went wrong).
 *
 *
 *\msc
 *ClientApp,UpperLayer,phDal4Nfc;
 *--- [label="Configure,intialise DAL layer and Register with DAL "];
 *UpperLayer=>phDal4Nfc	[label="phDal4Nfc_Write()",URL="\ref  phDal4Nfc_Write()"];
 *UpperLayer<<phDal4Nfc	[label="NFCSTATUS_PENDING"];
 *--- [label="DAL posts write message to main thread under deferred call context"];
 *phDal4Nfc=>phDal4Nfc [label="phDal4Nfc_DeferredCall()",URL="\ref	phDal4Nfc_DeferredCall()"];
 *ClientApp=>phDal4Nfc	[label="phDal4Nfc_DeferredCb()",URL="\ref Call phDal4Nfc_DeferredCb()"];
 *phDal4Nfc=>UpperLayer	[label="send_complete",URL="\ref phDal4Nfc_DeferredCb()"];
 *ClientApp<<phDal4Nfc	[label="NFCSTATUS_SUCCESS"];
 *--- [label="upper layer can issue read wait request "];
 *UpperLayer=>phDal4Nfc	[label="phDal4Nfc_ReadWait()",URL="\ref phDal4Nfc_ReadWait()"];
 *UpperLayer<<phDal4Nfc	[label="NFCSTATUS_PENDING"];
 *--- [label="Issue Read wait cancel request here"];
 *UpperLayer=>phDal4Nfc	[label="phDal4Nfc_ReadWaitCancel()",URL="\ref phDal4Nfc_ReadWaitCancel()"];
 **UpperLayer<<phDal4Nfc	[label="NFCSTATUS_SUCCESS"];
 *\endmsc
 */
 extern
 NFCSTATUS
 phDal4Nfc_ReadWaitCancel(
                        void *pContext,
                        void *pHwRef
                        );

/**
 * \ingroup grp_nfc_dal
 *
 * \brief DAL config function
 * This synchronous function configures the given HW Interface and
 * sends the HANDLE to the caller.
 *
 * \param[in]       config     DAL configuration details as provided
 *                             by the upper layer. 
 * \param[in,out]   phwref     pointer to which valid Handle to DAL
 *                             interface is assigned.
 *
 * \retval NFCSTATUS_SUCCESS                    Configuration happened successfully.
 * \retval NFCSTATUS_INVALID_PARAMETER          At least one parameter of the function
 *                                              is invalid.
 * \retval NFCSTATUS_FAILED                     Configuration failed(example.unable to
 *                                              open HW Interface).
 * \retval NFCSTATUS_INVALID_DEVICE             The device has not been opened or
 *                                              has been disconnected meanwhile
 * \retval NFCSTATUS_BOARD_COMMUNICATION_ERROR  A board communication error occurred
                                                (e.g. configuration went wrong).
 *\msc
 *ClientApp,UpperLayer,phDal4Nfc;
 *ClientApp=>phDal4Nfc [label="phDal4Nfc_Config()",URL="\ref phDal4Nfc_Config"];
 *ClientApp<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *\endmsc
 */
 extern
 NFCSTATUS
 phDal4Nfc_Config (
                pphDal4Nfc_sConfig_t config,
                void **phwref
                );

 /**
 * \ingroup grp_nfc_dal
 *
 * \brief Release configuration for the given HW Interface.
 *
 * \copydoc page_reg Release all the variables of the DAL component, that has been 
 *      initialised in \b phDal4Nfc_Config function (Synchronous function).
 *
 * \param[in] pHwRef            Link information of the hardware
 *
 * \retval NFCSTATUS_SUCCESS            DAL Configuration Released successfully.
 * \retval NFCSTATUS_FAILED             Configuration release failed(example: Unable to close Com port).
 *
 *\msc
 *ClientApp,UpperLayer,phDal4Nfc;
 *ClientApp=>phDal4Nfc [label="phDal4Nfc_Config()",URL="\ref phDal4Nfc_Config"];
 *ClientApp<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *UpperLayer=>phDal4Nfc [label="phDal4Nfc_Register()",URL="\ref phDal4Nfc_Register"];
 *UpperLayer<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *UpperLayer=>phDal4Nfc [label="phDal4Nfc_Init()",URL="\ref	phDal4Nfc_Init"];
 *UpperLayer<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *--- [label="Perform read write operation"];
 *--- [label="DAL resources can be released during upper layer deinit sequence"];
 *UpperLayer=>phDal4Nfc [label="phDal4Nfc_Shutdown()",URL="\ref	phDal4Nfc_Shutdown"];
 *UpperLayer<<phDal4Nfc [label="NFCSTATUS_SUCCESS"];
 *ClientApp=>phDal4Nfc [label="phDal4Nfc_ConfigRelease()",URL="\ref phDal4Nfc_ConfigRelease"];
 *ClientApp<<phDal4Nfc [label="NFCSTATUS_SUCCESS"]; 
 *\endmsc
 */
extern 
NFCSTATUS 
phDal4Nfc_ConfigRelease(
    void        *pHwRef);

extern 
NFCSTATUS 
phDal4Nfc_Reset(long level);

extern 
NFCSTATUS 
phDal4Nfc_Download();

/******************** Function declarations *************************/

#endif /* PHDALNFC_H */

/****************************************** END OF FILE ***************************************************/

