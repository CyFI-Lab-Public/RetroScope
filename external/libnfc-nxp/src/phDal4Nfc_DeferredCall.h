#ifndef PHDAL4NFC_DEFERREDCALL_H
#define PHDAL4NFC_DEFERREDCALL_H

#ifdef PH_NFC_CUSTOMINTEGRATION
#include <phNfcCustomInt.h>
#else

#ifdef _DAL_4_NFC_C
#define _ext_
#else
#define _ext_ extern
#endif

typedef pphLibNfc_DeferredCallback_t pphDal4Nfc_Deferred_Call_t;

typedef phLibNfc_DeferredCall_t phDal4Nfc_DeferredCall_Msg_t;

#ifndef WIN32

#ifdef USE_MQ_MESSAGE_QUEUE
#include <mqueue.h>
#define MQ_NAME_IDENTIFIER  "/nfc_queue"

_ext_ const struct mq_attr MQ_QUEUE_ATTRIBUTES
#ifdef _DAL_4_NFC_C
                                          = { 0,                               /* flags */
                                             10,                              /* max number of messages on queue */
                                             sizeof(phDal4Nfc_DeferredCall_Msg_t),   /* max message size in bytes */
                                             0                                /* number of messages currently in the queue */
                                           }
#endif
;
#endif

#endif

void phDal4Nfc_DeferredCall(pphDal4Nfc_Deferred_Call_t func, void *param);
#endif
#endif


