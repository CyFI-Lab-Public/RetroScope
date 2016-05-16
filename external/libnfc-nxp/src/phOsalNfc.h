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

/*
 * \file  phOsalNfc.h
 * \brief OSAL Implementation.
 *
 * Project: NFC-FRI 1.1
 * $Date: Fri Jun 26 14:41:31 2009 $
 * $Author: ing04880 $
 * $Revision: 1.21 $
 * $Aliases: NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */
#ifndef PHOSALNFC_H
#define PHOSALNFC_H


/** \defgroup grp_osal_nfc OSAL Component
 *
 *\note: API listed here encompasses Operating System Abstraction Layer interfaces required to be mapped to underlying OS platforms.
 *
 */
#include <phNfcTypes.h>

#ifdef PH_NFC_CUSTOMINTEGRATION
#include <phNfcCustomInt.h>
#else
#include <memory.h>

/**< OSAL Message Type */
#ifdef WIN32
//#define PH_OSALNFC_MESSAGE_BASE  (WM_USER+0x3FF)
#define PH_OSALNFC_MESSAGE_BASE  PH_LIBNFC_MESSAGE_BASE
#endif

/*!
 * \ingroup grp_osal_nfc
 *
 * OSAL Message structure contains message specific details like
 * message type, message specific data block details, etc.
 */
//typedef struct phOsalNfc_Message
//{
//    uint32_t eMsgType;/**< Type of the message to be posted*/
//    void   * pMsgData;/**< Pointer to message specific data block in case any*/
//    uint16_t Size;/**< Size of the datablock*/
//} phOsalNfc_Message_t,*pphOsalNfc_Message_t;
typedef phLibNfc_Message_t phOsalNfc_Message_t;
typedef pphLibNfc_Message_t pphOsalNfc_Message_t;

/*!
 * \ingroup grp_osal_nfc
 *
 * Enum definition contains  supported exception types
  */
typedef enum
{
    phOsalNfc_e_NoMemory,						/**<Memory allocation failed */
    phOsalNfc_e_PrecondFailed,					/**<precondition wasn't met */
    phOsalNfc_e_InternalErr,					/**<Unrecoverable error */
    phOsalNfc_e_UnrecovFirmwareErr,				/**<Unrecoverable firmware error */
    phOsalNfc_e_DALerror,						/**<Unrecoverable DAL error */
    phOsalNfc_e_Noerror							/**<No errortype */
} phOsalNfc_ExceptionType_t ;

/*!
 * \ingroup grp_osal_nfc
 *
 * OSAL Exception structure containing exception type and reason.
 */
typedef struct phOsalNfc_Exception
{
   phOsalNfc_ExceptionType_t eExceptionType;
   uint16_t reason;
} phOsalNfc_Exception_t;

#ifdef ANDROID
extern phOsalNfc_Exception_t phOsalNfc_Exception;
#endif

/* OsalNfc Status Type */

/** \ingroup grp_retval1
    A new semaphore could not be created due to
    a system error. */
#define NFCSTATUS_SEMAPHORE_CREATION_ERROR                      (0x1010)

/** \ingroup grp_retval1
    The given semaphore could not be released due to
    a system error or invalid handle. */
#define NFCSTATUS_SEMAPHORE_PRODUCE_ERROR                       (0x1011) 

/** \ingroup grp_retval11
    The given semaphore could not be consumed due to a
    system error or invalid handle. */
#define NFCSTATUS_SEMAPHORE_CONSUME_ERROR                       (0x1012) 


/*!
 * \ingroup grp_osal_nfc
 * \brief Raises exception
 *
 * The program jumps out of the current execution flow, i.e. this function
 * doesn't return.  The given exception contains information on what has
 * happened and how severe the error is.  @warning This function should only be
 * used for exceptional error situations where there is no means to recover.
 *
 * \param[in] eExceptiontype exception type.
 * \param[in] reason additional reason value that gives a vendor specific reason
 * code.
 *
 * \retval  None
 */
void phOsalNfc_RaiseException(phOsalNfc_ExceptionType_t  eExceptiontype,
                              uint16_t                   reason);

/*!
 * \ingroup grp_osal_nfc
 * \brief Output debug trace
 *
 * Outputs trace log of requested size as string
 *
 * \param[in] data Data block.
 * \param[in] size buffer size of the data block.
 *
 * \retval None
 */
void phOsalNfc_DbgTrace(uint8_t data[], uint32_t size);


/*!
 * \ingroup grp_osal_nfc
 * \brief Print string
 *
 * Outputs given string to debug port.
 *
 * \param[in] pString pointer to buffer content to be displayed.
 *
 * \retval None
 */
void phOsalNfc_DbgString(const char *pString);

/*!
 * \ingroup grp_osal_nfc
 * \brief Print data buffer
 *
 * Outputs given string to debug port.
 *
 * \param[in] pString pointer to string to be displayed.
 * \param[in] length number of bytes to be displayed.
 * \param[in] pBuffer pointer to data bytes to be displayed.
 *
 * \retval None
 */
void phOsalNfc_PrintData(const char *pString, uint32_t length, uint8_t *pBuffer,
        int verbosity);

/*!
 * \ingroup grp_osal_nfc
 * \brief Allocates some memory
 *
 * \param[in] Size   Size, in uint8_t, to be allocated
 *
 * \retval NON-NULL value:  The memory was successfully allocated ; the return value points to the allocated memory location
 * \retval NULL:            The operation was not successful, certainly because of insufficient resources.
 *
 */
extern void * phOsalNfc_GetMemory(uint32_t Size);


/*!
 * \ingroup grp_osal_nfc
 * \brief This API allows to free already allocated memory.
 * \param[in] pMem  Pointer to the memory block to deallocated
 * \retval None
 */
void   phOsalNfc_FreeMemory(void * pMem);

/*!
 * \ingroup grp_osal_nfc
 * \brief Compares the values stored in the source memory with the 
 * values stored in the destination memory.
 *
 * \param[in] src   Pointer to the Source Memory
 * \param[in] dest  Pointer to the Destination Memory
 * \param[in] n     Number of bytes to be compared.
 *
 * \retval Zero value:        The comparison was successful,
                    Both the memory areas contain the identical values.
 * \retval Non-Zero Value:    The comparison failed, both the memory
 *                  areas are non-identical.
 *
 */
int phOsalNfc_MemCompare(void *src, void *dest, unsigned int n);

#endif
#endif /*  PHOSALNFC_H  */
