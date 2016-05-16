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
 * \file phDalNfc.c
 * \brief DAL Implementation for linux
 *
 * Project: Trusted NFC Linux Lignt
 *
 * $Date: 07 aug 2009
 * $Author: Jonathan roux
 * $Revision: 1.0 $
 *
 */

#define _DAL_4_NFC_C

#include <unistd.h>
#include <pthread.h>
#include <stdlib.h>
#ifdef ANDROID
#include <linux/ipc.h>
#include <cutils/log.h>
#include <cutils/properties.h> // for property_get
#else
#include <sys/msg.h>
#endif
#include <semaphore.h>
#include <phDal4Nfc.h>
#include <phOsalNfc.h>
#include <phNfcStatus.h>
#include <phDal4Nfc_DeferredCall.h>
#include <phDal4Nfc_debug.h>
#include <phDal4Nfc_uart.h>
#include <phDal4Nfc_i2c.h>
#include <phDal4Nfc_link.h>
#include <phDal4Nfc_messageQueueLib.h>
#include <hardware/hardware.h>
#include <hardware/nfc.h>


/*-----------------------------------------------------------------------------------
                                       TYPES
------------------------------------------------------------------------------------*/
/*structure holds members related for both read and write operations*/
typedef struct Dal_RdWr_st
{
    /* Read members */
    pthread_t             nReadThread;             /* Read thread Hanlde */
    uint8_t *             pReadBuffer;             /* Read local buffer */
    int                   nNbOfBytesToRead;        /* Number of bytes to read */
    int                   nNbOfBytesRead;          /* Number of read bytes */
    char                  nReadBusy;               /* Read state machine */
    char                  nReadThreadAlive;        /* Read state machine */
    char                  nWaitingOnRead;          /* Read state machine */

    /* Read wait members */
    uint8_t *             pReadWaitBuffer;         /* Read wait local Buffer */
    int                   nNbOfBytesToReadWait;    /* Number of bytes to read */
    int                   nNbOfBytesReadWait;      /* Number of read bytes */
    char                  nReadWaitBusy;           /* Read state machine */
    char                  nWaitingOnReadWait;      /* Read state machine */
    char                  nCancelReadWait;         /* Read state machine */

    /* Write members */
    pthread_t             nWriteThread;            /* Write thread Hanlde */
    uint8_t *             pWriteBuffer;            /* Write local buffer */
    uint8_t *             pTempWriteBuffer;        /* Temp Write local buffer */
    int                   nNbOfBytesToWrite;       /* Number of bytes to write */
    int                   nNbOfBytesWritten;       /* Number of bytes written */
    char                  nWaitingOnWrite;         /* Write state machine */
    char                  nWriteThreadAlive;       /* Write state machine */
    char                  nWriteBusy;              /* Write state machine */
} phDal4Nfc_RdWr_t;

typedef void   (*pphDal4Nfc_DeferFuncPointer_t) (void * );
typedef void * (*pphDal4Nfc_thread_handler_t)   (void * pParam);


/*-----------------------------------------------------------------------------------
                                      VARIABLES
------------------------------------------------------------------------------------*/
static phDal4Nfc_RdWr_t               gReadWriteContext;
static phDal4Nfc_SContext_t           gDalContext;
static pphDal4Nfc_SContext_t          pgDalContext;
static phHal_sHwReference_t   *       pgDalHwContext;
static sem_t                          nfc_read_sem;
static int                            low_level_traces;
#ifdef USE_MQ_MESSAGE_QUEUE
static phDal4Nfc_DeferredCall_Msg_t   nDeferedMessage;
static mqd_t                          nDeferedCallMessageQueueId;

#else
int                            nDeferedCallMessageQueueId = 0;
#endif
static phDal4Nfc_link_cbk_interface_t gLinkFunc;
/*-----------------------------------------------------------------------------------
                                     PROTOTYPES
------------------------------------------------------------------------------------*/
static void      phDal4Nfc_DeferredCb     (void  *params);
static NFCSTATUS phDal4Nfc_StartThreads   (void);
static void      phDal4Nfc_FillMsg        (phDal4Nfc_Message_t *pDalMsg, phOsalNfc_Message_t *pOsalMsg);

/*-----------------------------------------------------------------------------------
                                DAL API IMPLEMENTATION
------------------------------------------------------------------------------------*/

static void refresh_low_level_traces() {
#ifdef LOW_LEVEL_TRACES
    low_level_traces = 1;
    return;
#else

#ifdef ANDROID
    char value[PROPERTY_VALUE_MAX];

    property_get("ro.debuggable", value, "");
    if (!value[0] || !atoi(value)) {
        low_level_traces = 0;  // user build, do not allow debug
        return;
    }

    property_get("debug.nfc.LOW_LEVEL_TRACES", value, "0");
    if (value[0]) {
        low_level_traces = atoi(value);
        return;
    }
#endif
    low_level_traces = 0;
#endif
}

/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_Register

PURPOSE:  DAL register function.

-----------------------------------------------------------------------------*/
NFCSTATUS phDal4Nfc_Register( phNfcIF_sReference_t  *psRefer,
                              phNfcIF_sCallBack_t if_cb, void *psIFConf )
{
    NFCSTATUS               result = NFCSTATUS_SUCCESS;

    if ((NULL != psRefer) &&
        (NULL != psRefer->plower_if) &&
        (NULL != if_cb.receive_complete) &&
        (NULL != if_cb.send_complete)
        )
    {
        /* Register the LLC functions to the upper layer */
        psRefer->plower_if->init           = phDal4Nfc_Init;
        psRefer->plower_if->release        = phDal4Nfc_Shutdown;
        psRefer->plower_if->send           = phDal4Nfc_Write;
        psRefer->plower_if->receive        = phDal4Nfc_Read;
        psRefer->plower_if->receive_wait   = phDal4Nfc_ReadWait;
        psRefer->plower_if->transact_abort = phDal4Nfc_ReadWaitCancel;
        psRefer->plower_if->unregister     = phDal4Nfc_Unregister;


        if (NULL != pgDalContext)
        {
            /* Copy the DAL context to the upper layer */
            psRefer->plower_if->pcontext = pgDalContext;
            /* Register the callback function from the upper layer */
            pgDalContext->cb_if.receive_complete = if_cb.receive_complete;
            pgDalContext->cb_if.send_complete = if_cb.send_complete;
            pgDalContext->cb_if.notify = if_cb.notify;
            /* Get the upper layer context */
            pgDalContext->cb_if.pif_ctxt = if_cb.pif_ctxt;
            /* Update the error state */
            result = NFCSTATUS_SUCCESS;
        }
        else
        {
            result = PHNFCSTVAL(CID_NFC_DAL, NFCSTATUS_NOT_INITIALISED);
        }
    }
    else /*Input parameters invalid*/
    {
        result = PHNFCSTVAL(CID_NFC_DAL, NFCSTATUS_INVALID_PARAMETER);
    }
    return result;
}

/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_Unregister

PURPOSE:  DAL unregister function.

-----------------------------------------------------------------------------*/
NFCSTATUS phDal4Nfc_Unregister(void *pContext, void *pHwRef )
{
    NFCSTATUS               result = NFCSTATUS_SUCCESS;

    if ((NULL == pContext) && (NULL == pHwRef))
    {
        result = PHNFCSTVAL(CID_NFC_DAL, NFCSTATUS_INVALID_PARAMETER);
    }
    else
    {
        if (NULL != pgDalContext)
        {
            /* Register the callback function from the upper layer */
            pgDalContext->cb_if.receive_complete = NULL;
            pgDalContext->cb_if.send_complete = NULL ;
            pgDalContext->cb_if.notify = NULL ;
            /* Get the upper layer context */
            pgDalContext->cb_if.pif_ctxt =  NULL ;
            
        }
        else
        {
            result = PHNFCSTVAL(CID_NFC_DAL, NFCSTATUS_NOT_INITIALISED);
        }
    }
    return result;
}

/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_Init

PURPOSE:  DAL Init function.

-----------------------------------------------------------------------------*/
NFCSTATUS phDal4Nfc_Init(void *pContext, void *pHwRef )
{
    NFCSTATUS        result = NFCSTATUS_SUCCESS;

    refresh_low_level_traces();

    if ((NULL != pContext) && (NULL != pHwRef))
    {
        pContext  = pgDalContext;
        pgDalHwContext = (phHal_sHwReference_t *)pHwRef;

        if ( gDalContext.hw_valid == TRUE )
        {
            /* The link has been opened from the application interface */
            gLinkFunc.open_from_handle(pgDalHwContext);

            if (!gLinkFunc.is_opened())
            {
                result = PHNFCSTVAL(CID_NFC_DAL, NFCSTATUS_INVALID_DEVICE);
            }
            else
            {
                /* Clear link buffers */
                gLinkFunc.flush();
            }
        }
        else
        {
            static phDal4Nfc_sConfig_t hw_config;
            hw_config.deviceNode = NULL;
            result = phDal4Nfc_Config(&hw_config, pHwRef );
        }
    }
    else /*Input parametrs invalid*/
    {
        result = NFCSTATUS_INVALID_PARAMETER;
    }

    return result;
}

/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_Shutdown

PURPOSE:  DAL Shutdown function.

-----------------------------------------------------------------------------*/

NFCSTATUS phDal4Nfc_Shutdown( void *pContext, void *pHwRef)
{
   NFCSTATUS result = NFCSTATUS_SUCCESS;
   void * pThreadReturn;

//   if (pContext == NULL)
//      return NFCSTATUS_INVALID_PARAMETER;

   if (gDalContext.hw_valid == TRUE)
   {
      /* Flush the link */
      gLinkFunc.flush();

      /* Close the message queue */
#ifdef USE_MQ_MESSAGE_QUEUE
       mq_close(nDeferedCallMessageQueueId);
#endif

   }

   return result;
}

NFCSTATUS phDal4Nfc_ConfigRelease(void *pHwRef)
{

   NFCSTATUS result = NFCSTATUS_SUCCESS;
   void * pThreadReturn;
   
   DAL_PRINT("phDal4Nfc_ConfigRelease ");

   if (gDalContext.hw_valid == TRUE)
   {
       /* Signal the read and write threads to exit.  NOTE: there
          actually is no write thread!  :)  */
       DAL_PRINT("Stop Reader Thread");
       gReadWriteContext.nReadThreadAlive = 0;
       gReadWriteContext.nWriteThreadAlive = 0;

       /* Wake up the read thread so it can exit */
       DAL_PRINT("Release Read Semaphore");
       sem_post(&nfc_read_sem);

       DAL_DEBUG("phDal4Nfc_ConfigRelease - doing pthread_join(%d)",
                 gReadWriteContext.nReadThread);
       if (pthread_join(gReadWriteContext.nReadThread,  &pThreadReturn) != 0)
       {
           result = PHNFCSTVAL(CID_NFC_DAL, NFCSTATUS_FAILED);
           DAL_PRINT("phDal4Nfc_ConfigRelease  KO");
       }

      /* Close the message queue */
#ifdef USE_MQ_MESSAGE_QUEUE
       mq_close(nDeferedCallMessageQueueId);
#endif

       /* Shutdown NFC Chip */
       phDal4Nfc_Reset(0);

      /* Close the link */
      gLinkFunc.close();

      if (gDalContext.pDev != NULL) {
          nfc_pn544_close(gDalContext.pDev);
      }
      /* Reset the Read Writer context to NULL */
      memset((void *)&gReadWriteContext,0,sizeof(gReadWriteContext));
      /* Reset the DAL context values to NULL */
      memset((void *)&gDalContext,0,sizeof(gDalContext));
   }

   gDalContext.hw_valid = FALSE;
   
   DAL_DEBUG("phDal4Nfc_ConfigRelease(): %04x\n", result);


   return result;
}

/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_Write

PURPOSE:  DAL Write function.

-----------------------------------------------------------------------------*/

NFCSTATUS phDal4Nfc_Write( void *pContext, void *pHwRef,uint8_t *pBuffer, uint16_t length)
{
    NFCSTATUS result = NFCSTATUS_SUCCESS;
    static int       MsgType= PHDAL4NFC_WRITE_MESSAGE;
    int *            pmsgType=&MsgType;
    phDal4Nfc_Message_t      sMsg;
    phOsalNfc_Message_t      OsalMsg;

    if ((NULL != pContext) && (NULL != pHwRef)&&
        (NULL != pBuffer) && (0 != length))
    {
        if( gDalContext.hw_valid== TRUE)
        {
            if((!gReadWriteContext.nWriteBusy)&&
                (!gReadWriteContext.nWaitingOnWrite))
            {
		DAL_PRINT("phDal4Nfc_Write() : Temporary buffer !! \n");
		gReadWriteContext.pTempWriteBuffer = (uint8_t*)malloc(length * sizeof(uint8_t));
		/* Make a copy of the passed arguments */
		memcpy(gReadWriteContext.pTempWriteBuffer,pBuffer,length);
                DAL_DEBUG("phDal4Nfc_Write(): %d\n", length);
                gReadWriteContext.pWriteBuffer = gReadWriteContext.pTempWriteBuffer;
                gReadWriteContext.nNbOfBytesToWrite  = length;
                /* Change the write state so that thread can take over the write */
                gReadWriteContext.nWriteBusy = TRUE;
                /* Just set variable here. This is the trigger for the Write thread */
                gReadWriteContext.nWaitingOnWrite = TRUE;
                /* Update the error state */
                result = NFCSTATUS_PENDING;
                /* Send Message and perform physical write in the DefferedCallback */
                /* read completed immediately */
		sMsg.eMsgType= PHDAL4NFC_WRITE_MESSAGE;
		/* Update the state */
		phDal4Nfc_FillMsg(&sMsg,&OsalMsg);
		phDal4Nfc_DeferredCall((pphDal4Nfc_DeferFuncPointer_t)phDal4Nfc_DeferredCb,(void *)pmsgType);
		memset(&sMsg,0,sizeof(phDal4Nfc_Message_t));
		memset(&OsalMsg,0,sizeof(phOsalNfc_Message_t));
            }
            else
            {
                /* Driver is BUSY with previous Write */
                DAL_PRINT("phDal4Nfc_Write() : Busy \n");
                result = PHNFCSTVAL(CID_NFC_DAL, NFCSTATUS_BUSY) ;
            }
        }
        else
        {
            /* TBD :Additional error code : NOT_INITIALISED */
            result = PHNFCSTVAL(CID_NFC_DAL, NFCSTATUS_INVALID_DEVICE);
        }

    }/*end if-Input parametrs valid-check*/
    else
    {
        result = NFCSTATUS_INVALID_PARAMETER;
    }
    return result;
}

/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_Read

PURPOSE:  DAL Read  function.

-----------------------------------------------------------------------------*/

NFCSTATUS phDal4Nfc_Read( void *pContext, void *pHwRef,uint8_t *pBuffer, uint16_t length)
{
    NFCSTATUS result = NFCSTATUS_SUCCESS;

    if ((NULL != pContext) && (NULL != pHwRef)&&
        (NULL != pBuffer) && (0 != length))
    {
        if ( gDalContext.hw_valid== TRUE)
        {
            if((!gReadWriteContext.nReadBusy)&&
                (!gReadWriteContext.nWaitingOnRead))
            {
                DAL_DEBUG("*****DAl Read called  length : %d\n", length);

                /* Make a copy of the passed arguments */
                gReadWriteContext.pReadBuffer = pBuffer;
                gReadWriteContext.nNbOfBytesToRead  = length;
                /* Change the Read state so that thread can take over the read */
                gReadWriteContext.nReadBusy = TRUE;
                /* Just set variable here. This is the trigger for the Reader thread */
                gReadWriteContext.nWaitingOnRead = TRUE;
                /* Update the return state */
                result = NFCSTATUS_PENDING;
                /* unlock reader thread */
                sem_post(&nfc_read_sem);
            }
            else
            {
                /* Driver is BUSY with prev Read */
                DAL_PRINT("DAL BUSY\n");
                /* Make a copy of the passed arguments */
                gReadWriteContext.pReadBuffer = pBuffer;
                gReadWriteContext.nNbOfBytesToRead  = length;
                result = NFCSTATUS_PENDING;
            }
        }
        else
        {
            /* TBD :Additional error code : NOT_INITIALISED */
            result = PHNFCSTVAL(CID_NFC_DAL, NFCSTATUS_INVALID_DEVICE);
        }
    }/*end if-Input parametrs valid-check*/
    else
    {
        result = NFCSTATUS_INVALID_PARAMETER;
    }
    DAL_DEBUG("*****DAl Read called  result : %x\n", result);
    return result;
}


/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_ReadWait

PURPOSE:  DAL Read wait function.

-----------------------------------------------------------------------------*/

NFCSTATUS phDal4Nfc_ReadWait(void *pContext, void *pHwRef,uint8_t *pBuffer, uint16_t length)
{
   /* not used */
   DAL_PRINT("phDal4Nfc_ReadWait"); 
   return 0;
}
/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_ReadWaitCancel

PURPOSE: Cancel the Read wait function.

-----------------------------------------------------------------------------*/

NFCSTATUS phDal4Nfc_ReadWaitCancel( void *pContext, void *pHwRef)
{
   DAL_PRINT("phDal4Nfc_ReadWaitCancel");

   /* unlock read semaphore */
   sem_post(&nfc_read_sem);

   return 0;
}

/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_Config

PURPOSE: Configure the serial port.

-----------------------------------------------------------------------------*/
NFCSTATUS phDal4Nfc_Config(pphDal4Nfc_sConfig_t config,void **phwref)
{
   NFCSTATUS                       retstatus = NFCSTATUS_SUCCESS;
   const hw_module_t* hw_module;
   nfc_pn544_device_t* pn544_dev;
   uint8_t num_eeprom_settings;
   uint8_t* eeprom_settings;
   int ret;

   /* Retrieve the hw module from the Android NFC HAL */
   ret = hw_get_module(NFC_HARDWARE_MODULE_ID, &hw_module);
   if (ret) {
       ALOGE("hw_get_module() failed");
       return NFCSTATUS_FAILED;
   }
   ret = nfc_pn544_open(hw_module, &pn544_dev);
   if (ret) {
       ALOGE("Could not open pn544 hw_module");
       return NFCSTATUS_FAILED;
   }
   config->deviceNode = pn544_dev->device_node;
   if (config->deviceNode == NULL) {
       ALOGE("deviceNode NULL");
       return NFCSTATUS_FAILED;
   }

   DAL_PRINT("phDal4Nfc_Config");

   if ((config == NULL) || (phwref == NULL))
      return NFCSTATUS_INVALID_PARAMETER;

   /* Register the link callbacks */
   memset(&gLinkFunc, 0, sizeof(phDal4Nfc_link_cbk_interface_t));
   switch(pn544_dev->linktype)
   {
      case PN544_LINK_TYPE_UART:
      case PN544_LINK_TYPE_USB:
      {
	 DAL_PRINT("UART link Config");
         /* Uart link interface */
         gLinkFunc.init               = phDal4Nfc_uart_initialize;
         gLinkFunc.open_from_handle   = phDal4Nfc_uart_set_open_from_handle;
         gLinkFunc.is_opened          = phDal4Nfc_uart_is_opened;
         gLinkFunc.flush              = phDal4Nfc_uart_flush;
         gLinkFunc.close              = phDal4Nfc_uart_close;
         gLinkFunc.open_and_configure = phDal4Nfc_uart_open_and_configure;
         gLinkFunc.read               = phDal4Nfc_uart_read;
         gLinkFunc.write              = phDal4Nfc_uart_write;
         gLinkFunc.reset              = phDal4Nfc_uart_reset;
      }
      break;

      case PN544_LINK_TYPE_I2C:
      {
	 DAL_PRINT("I2C link Config");
         /* i2c link interface */
         gLinkFunc.init               = phDal4Nfc_i2c_initialize;
         gLinkFunc.open_from_handle   = phDal4Nfc_i2c_set_open_from_handle;
         gLinkFunc.is_opened          = phDal4Nfc_i2c_is_opened;
         gLinkFunc.flush              = phDal4Nfc_i2c_flush;
         gLinkFunc.close              = phDal4Nfc_i2c_close;
         gLinkFunc.open_and_configure = phDal4Nfc_i2c_open_and_configure;
         gLinkFunc.read               = phDal4Nfc_i2c_read;
         gLinkFunc.write              = phDal4Nfc_i2c_write;
         gLinkFunc.reset              = phDal4Nfc_i2c_reset;
         break;
      }

      default:
      {
         /* Shound not happen : Bad parameter */
         return PHNFCSTVAL(CID_NFC_DAL, NFCSTATUS_INVALID_PARAMETER);
      }
   }

   gLinkFunc.init(); /* So that link interface can initialize its internal state */
   retstatus = gLinkFunc.open_and_configure(config, phwref);
   if (retstatus != NFCSTATUS_SUCCESS)
      return retstatus;

   /* Iniatilize the DAL context */
   (void)memset(&gDalContext,0,sizeof(phDal4Nfc_SContext_t));
   pgDalContext = &gDalContext;
   
   /* Reset the Reader Thread values to NULL */
   memset((void *)&gReadWriteContext,0,sizeof(gReadWriteContext));
   gReadWriteContext.nReadThreadAlive     = TRUE;
   gReadWriteContext.nWriteBusy = FALSE;
   gReadWriteContext.nWaitingOnWrite = FALSE;
   
   /* Prepare the message queue for the defered calls */
#ifdef USE_MQ_MESSAGE_QUEUE
   nDeferedCallMessageQueueId = mq_open(MQ_NAME_IDENTIFIER, O_CREAT|O_RDWR, 0666, &MQ_QUEUE_ATTRIBUTES);
#else
   nDeferedCallMessageQueueId = config->nClientId;
#endif

   gDalContext.pDev = pn544_dev;

   /* Start Read and Write Threads */
   if(NFCSTATUS_SUCCESS != phDal4Nfc_StartThreads())
   {
      return PHNFCSTVAL(CID_NFC_DAL, NFCSTATUS_FAILED);
   }

   gDalContext.hw_valid = TRUE;
   phDal4Nfc_Reset(1);
   phDal4Nfc_Reset(0);
   phDal4Nfc_Reset(1);

   return NFCSTATUS_SUCCESS;
}

/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_Reset

PURPOSE: Reset the PN544, using the VEN pin

-----------------------------------------------------------------------------*/
NFCSTATUS phDal4Nfc_Reset(long level)
{
   NFCSTATUS	retstatus = NFCSTATUS_SUCCESS;

   DAL_DEBUG("phDal4Nfc_Reset: VEN to %ld",level);

   retstatus = gLinkFunc.reset(level);

   return retstatus;
}

/*-----------------------------------------------------------------------------

FUNCTION: phDal4Nfc_Download

PURPOSE: Put the PN544 in download mode, using the GPIO4 pin

-----------------------------------------------------------------------------*/
NFCSTATUS phDal4Nfc_Download()
{
   NFCSTATUS	retstatus = NFCSTATUS_SUCCESS;

   DAL_DEBUG("phDal4Nfc_Download: GPIO4 to %d",1);

   usleep(10000);
   retstatus = phDal4Nfc_Reset(2);

   return retstatus;
}



/*-----------------------------------------------------------------------------------
                                DAL INTERNAL IMPLEMENTATION
------------------------------------------------------------------------------------*/



/**
 * \ingroup grp_nfc_dal
 *
 * \brief DAL Reader thread handler
 * This function manages the reads from the link interface. The reads are done from
 * this thread to create the asynchronous mecanism. When calling the synchronous
 * function phDal4Nfc_Read, the nWaitingOnRead mutex is unlocked and the read
 * can be done. Then a client callback is called to send the result.
 *
 * \param[in]       pArg     A custom argument that can be passed to the thread (not used)
 *
 * \retval TRUE                                 Thread exiting.
 */

int phDal4Nfc_ReaderThread(void * pArg)
{
    char      retvalue;
    NFCSTATUS result = NFCSTATUS_SUCCESS;
    uint8_t   retry_cnt=0;
    void *    memsetRet;

    static int       MsgType= PHDAL4NFC_READ_MESSAGE;
    int *     pmsgType=&MsgType;

    phDal4Nfc_Message_t      sMsg;
    phOsalNfc_Message_t      OsalMsg ;
    int i;
    int i2c_error_count;
    int i2c_workaround;
    int i2c_device_address = 0x57;
    if (gDalContext.pDev != NULL) {
        i2c_workaround = gDalContext.pDev->enable_i2c_workaround;
        if (gDalContext.pDev->i2c_device_address) {
            i2c_device_address = gDalContext.pDev->i2c_device_address;
            if (i2c_workaround && i2c_device_address < 32)
            {
                ALOGE("i2c_device_address not set to valid value");
                return NFCSTATUS_FAILED;
            }
        }
    } else {
        ALOGE("gDalContext.pDev is not set");
        return NFCSTATUS_FAILED;
    }

    pthread_setname_np(pthread_self(), "reader");

    /* Create the overlapped event. Must be closed before exiting
    to avoid a handle leak. This event is used READ API and the Reader thread*/

    DAL_PRINT("RX Thread \n");
    DAL_DEBUG("\nRX Thread nReadThreadAlive = %d",gReadWriteContext.nReadThreadAlive);
    DAL_DEBUG("\nRX Thread nWaitingOnRead = %d",gReadWriteContext.nWaitingOnRead);
    while(gReadWriteContext.nReadThreadAlive) /* Thread Loop */
    {
        /* Check for the read request from user */
	DAL_PRINT("RX Thread Sem Lock\n");
        sem_wait(&nfc_read_sem);
        DAL_PRINT("RX Thread Sem UnLock\n");

        if (!gReadWriteContext.nReadThreadAlive)
        {
            /* got the signal that we should exit.  NOTE: we don't
               attempt to read below, since the read may block */
            break;
        }

        /* Issue read operation.*/

    i2c_error_count = 0;
retry:
	gReadWriteContext.nNbOfBytesRead=0;
	DAL_DEBUG("RX Thread *New *** *****Request Length = %d",gReadWriteContext.nNbOfBytesToRead);
	memsetRet=memset(gReadWriteContext.pReadBuffer,0,gReadWriteContext.nNbOfBytesToRead);

	/* Wait for IRQ !!!  */
    gReadWriteContext.nNbOfBytesRead = gLinkFunc.read(gReadWriteContext.pReadBuffer, gReadWriteContext.nNbOfBytesToRead);

    /* A read value equal to the i2c_device_address indicates a HW I2C error at I2C address i2c_device_address
     * (pn544). There should not be false positives because a read of length 1
     * must be a HCI length read, and a length of i2c_device_address is impossible (max is 33).
     */
    if (i2c_workaround && gReadWriteContext.nNbOfBytesToRead == 1 &&
            gReadWriteContext.pReadBuffer[0] == i2c_device_address)
    {
        i2c_error_count++;
        DAL_DEBUG("RX Thread Read 0x%02x  ", i2c_device_address);
        DAL_DEBUG("%d times\n", i2c_error_count);

        if (i2c_error_count < 5) {
            usleep(2000);
            goto retry;
        }
        DAL_PRINT("RX Thread NOTHING TO READ, RECOVER");
        phOsalNfc_RaiseException(phOsalNfc_e_UnrecovFirmwareErr,1);
    }
    else
    {
        i2c_error_count = 0;

        if (low_level_traces)
        {
             phOsalNfc_PrintData("RECV", (uint16_t)gReadWriteContext.nNbOfBytesRead,
                    gReadWriteContext.pReadBuffer, low_level_traces);
        }
        DAL_DEBUG("RX Thread Read ok. nbToRead=%d\n", gReadWriteContext.nNbOfBytesToRead);
        DAL_DEBUG("RX Thread NbReallyRead=%d\n", gReadWriteContext.nNbOfBytesRead);
/*      DAL_PRINT("RX Thread ReadBuff[]={ ");
        for (i = 0; i < gReadWriteContext.nNbOfBytesRead; i++)
        {
          DAL_DEBUG("RX Thread 0x%x ", gReadWriteContext.pReadBuffer[i]);
        }
        DAL_PRINT("RX Thread }\n"); */

        /* read completed immediately */
        sMsg.eMsgType= PHDAL4NFC_READ_MESSAGE;
        /* Update the state */
        phDal4Nfc_FillMsg(&sMsg,&OsalMsg);
        phDal4Nfc_DeferredCall((pphDal4Nfc_DeferFuncPointer_t)phDal4Nfc_DeferredCb,(void *)pmsgType);
        memsetRet=memset(&sMsg,0,sizeof(phDal4Nfc_Message_t));
        memsetRet=memset(&OsalMsg,0,sizeof(phOsalNfc_Message_t));
    }

    } /* End of thread Loop*/

    DAL_PRINT("RX Thread  exiting");

    return TRUE;
}



/**
 * \ingroup grp_nfc_dal
 *
 * \brief DAL Start threads function
 * This function is called from phDal4Nfc_Config and is responsible of creating the
 * reader thread.
 *
 * \retval NFCSTATUS_SUCCESS                    If success.
 * \retval NFCSTATUS_FAILED                     Can not create thread or retreive its attributes
 */
NFCSTATUS phDal4Nfc_StartThreads(void)
{
    pthread_attr_t nReadThreadAttributes;
    pthread_attr_t nWriteThreadAttributes;
    int ret;

    if(sem_init(&nfc_read_sem, 0, 0) == -1)
    {
      DAL_PRINT("NFC Init Semaphore creation Error");
      return -1;
    }

    ret = pthread_create(&gReadWriteContext.nReadThread, NULL,  (pphDal4Nfc_thread_handler_t)phDal4Nfc_ReaderThread,  (void*) "dal_read_thread");
    if(ret != 0)
        return(PHNFCSTVAL(CID_NFC_DAL, NFCSTATUS_FAILED));

    return NFCSTATUS_SUCCESS;
}

/**
 * \ingroup grp_nfc_dal
 *
 * \brief DAL fill message function
 * Internal messages management. This function fills message structure
 * depending on message types.
 *
 * \param[in, out]       pDalMsg     DAL message to fill
 * \param[in, out]       pOsalMsg    OSAL message to fill
 *
 */
void phDal4Nfc_FillMsg(phDal4Nfc_Message_t *pDalMsg,phOsalNfc_Message_t *pOsalMsg)
{
  if(NULL != pgDalHwContext)
  {
    if(pDalMsg->eMsgType == PHDAL4NFC_WRITE_MESSAGE)
    {
        pDalMsg->transactInfo.length  = (uint8_t)gReadWriteContext.nNbOfBytesWritten;
        pDalMsg->transactInfo.buffer = NULL;
        pDalMsg->transactInfo.status  = NFCSTATUS_SUCCESS;
        pDalMsg->pHwRef  = pgDalHwContext;
        pDalMsg->writeCbPtr = pgDalContext->cb_if.send_complete;
        pOsalMsg->eMsgType = PH_DAL4NFC_MESSAGE_BASE;
        pOsalMsg->pMsgData = pDalMsg;
        return;
    }
    else if(pDalMsg->eMsgType == PHDAL4NFC_READ_MESSAGE)
    {
        pDalMsg->transactInfo.length  = (uint8_t)gReadWriteContext.nNbOfBytesRead;
        pDalMsg->transactInfo.buffer = gReadWriteContext.pReadBuffer;
        pDalMsg->pContext= pgDalContext->cb_if.pif_ctxt;
    }
    else
    {
        pDalMsg->transactInfo.length  = (uint8_t)gReadWriteContext.nNbOfBytesReadWait;
        pDalMsg->transactInfo.buffer = gReadWriteContext.pReadWaitBuffer;
        pDalMsg->pContext= pgDalContext;
    }
    pDalMsg->transactInfo.status  = NFCSTATUS_SUCCESS;
    pDalMsg->pHwRef  = pgDalHwContext;
    pDalMsg->readCbPtr = pgDalContext->cb_if.receive_complete;
    /*map to OSAL msg format*/
    pOsalMsg->eMsgType = PH_DAL4NFC_MESSAGE_BASE;
    pOsalMsg->pMsgData = pDalMsg;
  }

}

/**
 * \ingroup grp_nfc_dal
 *
 * \brief DAL deferred callback function
 * Generic handler function called by a client thread when reading a message from the queue.
 * Will function will directly call the client function (same context). See phDal4Nfc_DeferredCall
 *
  * \param[in]       params    Parameter that will be passed to the client function.
 *
 */
void phDal4Nfc_DeferredCb (void  *params)
{
    int*    pParam=NULL;
    int     i;
    phNfc_sTransactionInfo_t TransactionInfo;

    pParam=(int*)params;

    switch(*pParam)
    {
        case PHDAL4NFC_READ_MESSAGE:
            DAL_PRINT(" Dal deferred read called \n");
            TransactionInfo.buffer=gReadWriteContext.pReadBuffer;
            TransactionInfo.length=(uint16_t)gReadWriteContext.nNbOfBytesRead;
            if (gReadWriteContext.nNbOfBytesRead == gReadWriteContext.nNbOfBytesToRead) {
                TransactionInfo.status=NFCSTATUS_SUCCESS;
            } else {
                TransactionInfo.status=NFCSTATUS_READ_FAILED;
            }
            gReadWriteContext.nReadBusy = FALSE;


            /*  Reset flag so that another opertion can be issued.*/
            gReadWriteContext.nWaitingOnRead = FALSE;
            if ((NULL != pgDalContext) && (NULL != pgDalContext->cb_if.receive_complete))
            {
                pgDalContext->cb_if.receive_complete(pgDalContext->cb_if.pif_ctxt,
                                                        pgDalHwContext,&TransactionInfo);
            }
            
            break;
        case PHDAL4NFC_WRITE_MESSAGE:
            DAL_PRINT(" Dal deferred write called \n");

            if(low_level_traces)
            {
                phOsalNfc_PrintData("SEND", (uint16_t)gReadWriteContext.nNbOfBytesToWrite,
                        gReadWriteContext.pWriteBuffer, low_level_traces);
            }

            /* DAL_DEBUG("dalMsg->transactInfo.length : %d\n", dalMsg->transactInfo.length); */
            /* Make a Physical WRITE */
            /* NOTE: need to usleep(3000) here if the write is for SWP */
            usleep(500);  /* NXP advise 500us sleep required between I2C writes */
            gReadWriteContext.nNbOfBytesWritten = gLinkFunc.write(gReadWriteContext.pWriteBuffer, gReadWriteContext.nNbOfBytesToWrite);
            if (gReadWriteContext.nNbOfBytesWritten != gReadWriteContext.nNbOfBytesToWrite)
            {
                /* controller may be in standby. do it again! */ 
                usleep(10000); /* wait 10 ms */
                gReadWriteContext.nNbOfBytesWritten = gLinkFunc.write(gReadWriteContext.pWriteBuffer, gReadWriteContext.nNbOfBytesToWrite);
            }
            if (gReadWriteContext.nNbOfBytesWritten != gReadWriteContext.nNbOfBytesToWrite)
            {
                /* Report write failure or timeout */
                DAL_PRINT(" Physical Write Error !!! \n");
                TransactionInfo.length=(uint16_t)gReadWriteContext.nNbOfBytesWritten;
                TransactionInfo.status = PHNFCSTVAL(CID_NFC_DAL, NFCSTATUS_BOARD_COMMUNICATION_ERROR);
            }
            else
            {
                DAL_PRINT(" Physical Write Success \n"); 
	        TransactionInfo.length=(uint16_t)gReadWriteContext.nNbOfBytesWritten;
	        TransactionInfo.status=NFCSTATUS_SUCCESS;
/*              DAL_PRINT("WriteBuff[]={ ");
                for (i = 0; i < gReadWriteContext.nNbOfBytesWritten; i++)
                {
                  DAL_DEBUG("0x%x ", gReadWriteContext.pWriteBuffer[i]);
                }
                DAL_PRINT("}\n"); */

		// Free TempWriteBuffer 
		if(gReadWriteContext.pTempWriteBuffer != NULL)
		{
		    free(gReadWriteContext.pTempWriteBuffer);
		}
            }
            /* Reset Write context */
            gReadWriteContext.nWriteBusy = FALSE;
            gReadWriteContext.nWaitingOnWrite = FALSE;
            
            /* call LLC callback */
            if ((NULL != pgDalContext) && (NULL != pgDalContext->cb_if.send_complete))
            {
                pgDalContext->cb_if.send_complete(pgDalContext->cb_if.pif_ctxt,
                                                    pgDalHwContext,&TransactionInfo);
            }
            break;
        default:
            break;
    }
}

/**
 * \ingroup grp_nfc_dal
 *
 * \brief DAL deferred call function
 * This function will enable to call the callback client asyncronously and in the client context.
 * It will post a message in a queue that will be processed by a client thread.
 *
 * \param[in]       func     The function to call when message is read from the queue
 * \param[in]       param    Parameter that will be passed to the 'func' function.
 *
 */
void phDal4Nfc_DeferredCall(pphDal4Nfc_DeferFuncPointer_t func, void *param)
{
    int                retvalue = 0;
    phDal4Nfc_Message_Wrapper_t nDeferedMessageWrapper;
    phDal4Nfc_DeferredCall_Msg_t *pDeferedMessage;
    static phDal4Nfc_DeferredCall_Msg_t nDeferedMessageRead;
    static phDal4Nfc_DeferredCall_Msg_t nDeferedMessageWrite;

#ifdef USE_MQ_MESSAGE_QUEUE
    nDeferedMessage.eMsgType = PH_DAL4NFC_MESSAGE_BASE;
    nDeferedMessage.def_call = func;
    nDeferedMessage.params   = param;
    retvalue = (int)mq_send(nDeferedCallMessageQueueId, (char *)&nDeferedMessage, sizeof(phDal4Nfc_DeferredCall_Msg_t), 0);
#else
    if(PHDAL4NFC_READ_MESSAGE==(* (int*)param))
    {
        pDeferedMessage = &nDeferedMessageRead;
    }
    else
    {
        pDeferedMessage = &nDeferedMessageWrite;
    }
    nDeferedMessageWrapper.mtype = 1;
    nDeferedMessageWrapper.msg.eMsgType = PH_DAL4NFC_MESSAGE_BASE;
    pDeferedMessage->pCallback = func;
    pDeferedMessage->pParameter = param;
    nDeferedMessageWrapper.msg.pMsgData = pDeferedMessage;
    nDeferedMessageWrapper.msg.Size = sizeof(phDal4Nfc_DeferredCall_Msg_t);
    retvalue = phDal4Nfc_msgsnd(nDeferedCallMessageQueueId, (struct msgbuf *)&nDeferedMessageWrapper, sizeof(phLibNfc_Message_t), 0);
#endif

}

#undef _DAL_4_NFC_C
