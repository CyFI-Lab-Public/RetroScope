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
 * \file  phFriNfc_NdefReg.h
 * \brief NFC Ndef Registration / Listening.
 *
 * Project: NFC-FRI
 *
 * $Date: Fri Oct  5 10:10:07 2007 $
 * $Author: frq05303 $
 * $Revision: 1.1 $
 * $Aliases: NFC_FRI1.1_WK826_PREP1,NFC_FRI1.1_WK826_R1,NFC_FRI1.1_WK826_R2,NFC_FRI1.1_WK830_PREP1,NFC_FRI1.1_WK830_PREP2,NFC_FRI1.1_WK830_R5_1,NFC_FRI1.1_WK830_R5_2,NFC_FRI1.1_WK830_R5_3,NFC_FRI1.1_WK832_PREP1,NFC_FRI1.1_WK832_PRE2,NFC_FRI1.1_WK832_PREP2,NFC_FRI1.1_WK832_PREP3,NFC_FRI1.1_WK832_R5_1,NFC_FRI1.1_WK832_R6_1,NFC_FRI1.1_WK834_PREP1,NFC_FRI1.1_WK834_PREP2,NFC_FRI1.1_WK834_R7_1,NFC_FRI1.1_WK836_PREP1,NFC_FRI1.1_WK836_R8_1,NFC_FRI1.1_WK838_PREP1,NFC_FRI1.1_WK838_R9_PREP2,NFC_FRI1.1_WK838_R9_1,NFC_FRI1.1_WK840_R10_PREP1,NFC_FRI1.1_WK840_R10_1,NFC_FRI1.1_WK842_R11_PREP1,NFC_FRI1.1_WK842_R11_PREP2,NFC_FRI1.1_WK842_R11_1,NFC_FRI1.1_WK844_PREP1,NFC_FRI1.1_WK844_R12_1,NFC_FRI1.1_WK846_PREP1,NFC_FRI1.1_WK846_R13_1,NFC_FRI1.1_WK848_PREP1,NFC_FRI1.1_WK848_R14_1,NFC_FRI1.1_WK850_PACK1,NFC_FRI1.1_WK851_PREP1,NFC_FRI1.1_WK850_R15_1,NFC_FRI1.1_WK902_PREP1,NFC_FRI1.1_WK902_R16_1,NFC_FRI1.1_WK904_PREP1,NFC_FRI1.1_WK904_R17_1,NFC_FRI1.1_WK906_R18_1,NFC_FRI1.1_WK908_PREP1,NFC_FRI1.1_WK908_R19_1,NFC_FRI1.1_WK910_PREP1,NFC_FRI1.1_WK910_R20_1,NFC_FRI1.1_WK912_PREP1,NFC_FRI1.1_WK912_R21_1,NFC_FRI1.1_WK914_PREP1,NFC_FRI1.1_WK914_R22_1,NFC_FRI1.1_WK914_R22_2,NFC_FRI1.1_WK916_R23_1,NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $
 *
 */

#ifndef PHFRINFC_NDEFREG_H
#define PHFRINFC_NDEFREG_H

#ifndef PH_FRINFC_EXCLUDE_FROM_TESTFW /* */

/** 
 *  \name NDEF Registry and Listening
 *
 */
/*@{*/
#define PH_FRINFC_NDEFREG_FILEREVISION "$Revision: 1.1 $" /** \ingroup grp_file_attributes */
#define PH_FRINFC_NDEFREG_FILEALIASES  "$Aliases: NFC_FRI1.1_WK826_PREP1,NFC_FRI1.1_WK826_R1,NFC_FRI1.1_WK826_R2,NFC_FRI1.1_WK830_PREP1,NFC_FRI1.1_WK830_PREP2,NFC_FRI1.1_WK830_R5_1,NFC_FRI1.1_WK830_R5_2,NFC_FRI1.1_WK830_R5_3,NFC_FRI1.1_WK832_PREP1,NFC_FRI1.1_WK832_PRE2,NFC_FRI1.1_WK832_PREP2,NFC_FRI1.1_WK832_PREP3,NFC_FRI1.1_WK832_R5_1,NFC_FRI1.1_WK832_R6_1,NFC_FRI1.1_WK834_PREP1,NFC_FRI1.1_WK834_PREP2,NFC_FRI1.1_WK834_R7_1,NFC_FRI1.1_WK836_PREP1,NFC_FRI1.1_WK836_R8_1,NFC_FRI1.1_WK838_PREP1,NFC_FRI1.1_WK838_R9_PREP2,NFC_FRI1.1_WK838_R9_1,NFC_FRI1.1_WK840_R10_PREP1,NFC_FRI1.1_WK840_R10_1,NFC_FRI1.1_WK842_R11_PREP1,NFC_FRI1.1_WK842_R11_PREP2,NFC_FRI1.1_WK842_R11_1,NFC_FRI1.1_WK844_PREP1,NFC_FRI1.1_WK844_R12_1,NFC_FRI1.1_WK846_PREP1,NFC_FRI1.1_WK846_R13_1,NFC_FRI1.1_WK848_PREP1,NFC_FRI1.1_WK848_R14_1,NFC_FRI1.1_WK850_PACK1,NFC_FRI1.1_WK851_PREP1,NFC_FRI1.1_WK850_R15_1,NFC_FRI1.1_WK902_PREP1,NFC_FRI1.1_WK902_R16_1,NFC_FRI1.1_WK904_PREP1,NFC_FRI1.1_WK904_R17_1,NFC_FRI1.1_WK906_R18_1,NFC_FRI1.1_WK908_PREP1,NFC_FRI1.1_WK908_R19_1,NFC_FRI1.1_WK910_PREP1,NFC_FRI1.1_WK910_R20_1,NFC_FRI1.1_WK912_PREP1,NFC_FRI1.1_WK912_R21_1,NFC_FRI1.1_WK914_PREP1,NFC_FRI1.1_WK914_R22_1,NFC_FRI1.1_WK914_R22_2,NFC_FRI1.1_WK916_R23_1,NFC_FRI1.1_WK918_R24_1,NFC_FRI1.1_WK920_PREP1,NFC_FRI1.1_WK920_R25_1,NFC_FRI1.1_WK922_PREP1,NFC_FRI1.1_WK922_R26_1,NFC_FRI1.1_WK924_PREP1,NFC_FRI1.1_WK924_R27_1,NFC_FRI1.1_WK926_R28_1,NFC_FRI1.1_WK928_R29_1,NFC_FRI1.1_WK930_R30_1,NFC_FRI1.1_WK934_PREP_1,NFC_FRI1.1_WK934_R31_1,NFC_FRI1.1_WK941_PREP1,NFC_FRI1.1_WK941_PREP2,NFC_FRI1.1_WK941_1,NFC_FRI1.1_WK943_R32_1,NFC_FRI1.1_WK949_PREP1,NFC_FRI1.1_WK943_R32_10,NFC_FRI1.1_WK943_R32_13,NFC_FRI1.1_WK943_R32_14,NFC_FRI1.1_WK1007_R33_1,NFC_FRI1.1_WK1007_R33_4,NFC_FRI1.1_WK1017_PREP1,NFC_FRI1.1_WK1017_R34_1,NFC_FRI1.1_WK1017_R34_2,NFC_FRI1.1_WK1023_R35_1 $"      /** \ingroup grp_file_attributes */
/*@}*/
#include <phFriNfc_NdefRecord.h>

#endif /* Exclude from test fw */

/*
 * NDEF Registration And Listening - States of the Finite State machine
 *
 */
#define PH_FRINFC_NDEFREG_STATE_INIT        0       /**< \internal Init state. The start-up state */
#define PH_FRINFC_NDEFREG_STATE_DIS_PKT     1       /**< \internal Dispatch Packet is in progress */
#define PH_FRINFC_NDEFREG_STATE_DIS_RCD     2       /**< \internal Dispatch Record is in progress */

/*
 * NDEF Registration And Listening internal definitions
 */
#define PH_FRINFC_NDEFRECORD_TNF_MASK       0x07    /**< \internal */
#define PH_FRINFC_NDEFREG_CH_FLG_ARR_INDEX  50      /**< \internal */




/** \defgroup grp_fri_nfc_ndef_reg NDEF Registry
 *
 *  This component implements the NDEF Registration and Listening functionality.
 */
/*@{*/

/*
 * \name NDEF Registration And Listening callback and node definitions
 *
 *  \ref PH_FRINFC_NDEFREG_MAX_RTD_REGISTERED is the maximum number of RTDs
 *  that can be registered in a node.\n
 *  \ref PH_FRINFC_NDEFREG_MAX_RTD is the maximum number of Records that can
 *  be present in a single callback function.
 */
/*@{*/
#define PH_FRINFC_NDEFREG_MAX_RTD_REGISTERED    64    /**< Maximum number of RTDs per node */
#define PH_FRINFC_NDEFREG_MAX_RTD                8    /**< Maximum number of RTDs per callback function. */
/*@}*/

/**
 *  \brief NDEF Callback
 *
 * \copydoc page_reg
 *
 *  Upon reception of a NDEF record the component calls into the function registered as a listener
 *  for a NDEF type. The function must be compatible to the declaration of the pointer described in
 *  this section.
 *
 * \par Parameter
 *      The underlying type of the callback parameter (void pointer) is \ref phFriNfc_NdefReg_CbParam_t .
 *
 * \note On systems, requiring non-blocking operation, the user-defined callback function must not block,
 *       but just deliver the data and return immediately. In this case the CB must make a copy of the
 *       parameter structure (\ref phFriNfc_NdefReg_CbParam_t) and store it within the environment of the
 *       registered listener. A copy is needed because once the CB returns the values behind the parameter
 *       pointer become invalid. We observe the following rules:
 *       - This component does not rely on lower layers (e.g. HAL), therefore it doesn't need to handle
 *         completion routines.
 *       - This library gets a NDEF message and extracts the records using the NDEF Record (Tools) Library.
 *       - Alternatively, this component can process pre-extracted NDEF records.
 *       - This library only handles TOP level records, cascaded content is ignored.
 *       - Functions do not block: The \ref phFriNfc_NdefReg_Process "Process" function needs to be called
 *         periodically until completion.
 *       .
 */
typedef void(*pphFriNfc_NdefReg_Cb_t)(void*);


/**
 * \brief Callback Parameter. This parameter is provided to the CB function that serves
 *        as the notifier for services/applicatioon/software components registered for a specific
 *        NDEF Type.
 *
 * All information required to perform the \ref pphFriNfc_Cr_t "callback" operation is contained
 * within the structure. The members of the structure may only be read, changing them is not allowed.
 *
 *
 */
typedef struct phFriNfc_NdefReg_CbParam
{
    /**
     * Number of array Positions. Each array position carries data from a NDEF Record. The maximum
     * number is \ref PH_FRINFC_NDEFREG_MAX_RTD .
     */
    uint8_t                 Count;

    /**
     * The records that matched with the registred RTDs for this callback.
     * The number of records here will be equal to the first parameter Count.
     */
    phFriNfc_NdefRecord_t   Records[PH_FRINFC_NDEFREG_MAX_RTD];

    /** Indicates whether a record is chunked or not. */
    uint8_t                 Chunked[PH_FRINFC_NDEFREG_MAX_RTD];

    /** Pointer to the raw record. */
    uint8_t                 *RawRecord[PH_FRINFC_NDEFREG_MAX_RTD];

    /** Size of the raw record */
    uint32_t                RawRecordSize[PH_FRINFC_NDEFREG_MAX_RTD];

    /** Pointer for usage by the registering entity. The software component that registers for
        a specific RTD can specify this \b context pointer. With the help of the pointer
        the component is able to resolve its own address, context or object, respectively.\n
        \b Example: \ref grp_fri_nfc_ndef_reg "This SW component" is embedded into a C++ system
        that has one object registered for a certain RTD. \ref grp_fri_nfc_ndef_reg "This library"
        itself is written in C and therefore it requires a pure "C" callback that can be provided by
        C++ through a \b static member function. The registering C++ object will consequently set the
        \ref phFriNfc_NdefReg_CbParam_t::CbContext pointer to its \c this pointer. When the static
        member function of the C++ class is called it immediately knows the instance and can
        call into one of the C++ instance members again (\ref phFriNfc_NdefReg_CbParam_t::CbContext
        needs to be casted back to the original C++ class type).
    */
    void                    *CbContext;
} phFriNfc_NdefReg_CbParam_t;



/**
 * \brief Registration of a Callback - Parameter Structure
 *
 * This structure is used by the registering software. The registering listener has to \b initialise
 * \b all \b members of the structure that are \b not \b internal. Members for \b internal use \b must
 * \b not be set by the registering entity. Used by \ref phFriNfc_NdefReg_CbParam_t .
 *
 */
typedef struct phFriNfc_NdefReg_Cb
{
    /**
     * Number of array Positions. Each array position carries data specifying a RTD. The maximum number
     * is \ref PH_FRINFC_NDEFREG_MAX_RTD .
     *
     * \li Needs to be set by the registering entity.
     */
    uint8_t                     NumberOfRTDs;
    /**
     *  The Type Name Format, according to the NDEF specification, see the NDEF Record (Tools) component.
     *
     * \li Needs to be set by the registering entity.   
     */
    uint8_t                     Tnf[PH_FRINFC_NDEFREG_MAX_RTD_REGISTERED];

    /**
     * Array of pointers to the individual RTD buffers.
     *
     * \li Needs to be set by the registering entity.
     */
    uint8_t                     *NdefType[PH_FRINFC_NDEFREG_MAX_RTD_REGISTERED];

    /**
     * Array of length indicators of the RTD buffers.
     *
     * \li Needs to be set by the registering entity.
     */
    uint8_t                      NdeftypeLength[PH_FRINFC_NDEFREG_MAX_RTD_REGISTERED];

    /**
     * Function pointer to the C-style function within the registering entity.
     *
     * \li Needs to be set by the registering entity.
     */
    pphFriNfc_NdefReg_Cb_t       NdefCallback;

    /**
     * Context pointer of the registering entity (see \ref phFriNfc_NdefReg_CbParam_t).
     *
     * \li Needs to be set by the registering entity.
     */
    void                        *CbContext;

    /** \internal
     * This member is required by the library to link to the previous registered item. In case of the
     * first item this member is NULL.
     */
    struct phFriNfc_NdefReg_Cb  *Previous;
    /** \internal
     * This member is required by the library to link to the next registered item. In case of the
     * last item this member is NULL.
     */
    struct phFriNfc_NdefReg_Cb  *Next;
} phFriNfc_NdefReg_Cb_t;


/**
 *  \brief NFC NDEF Registry Compound
 *
 *  The NDEF Registry Compound. This is the context structure of the NDEF Registry and
 *  Listener.
 *
 */
typedef struct phFriNfc_NdefReg
{
    phFriNfc_NdefReg_Cb_t       *NdefTypeList;    /**< \internal List of Callback Structures (Listeners). */
    uint8_t                     *NdefData;        /**< \internal Data to process. */
    uint32_t                    NdefDataLength;   /**< \internal Length of the NDEF data. */
    uint8_t                     State;            /**< \internal The state of the library. */
    uint8_t                   **NdefTypes;        /**< \internal */

    phFriNfc_NdefRecord_t      *RecordsExtracted; /**< \internal */

    phFriNfc_NdefReg_CbParam_t  *CbParam;         /**< \internal */

    /*  Harsha: Fix for 0000252: [JF] Buffer overshoot in phFriNfc_NdefRecord_GetRecords */ 
    uint8_t                     *IsChunked;       /**< \internal Array of chunked flags */

    /*  Harsha: Fix for 0000252: [JF] Buffer overshoot 
        in phFriNfc_NdefRecord_GetRecords   */
    uint32_t                    NumberOfRecords;  /**< \internal Space available in NdefTypes
                                                                and IsChunked arrays */

    /*  Harsha: Fix for 0000243: [JF] phFriNfc_NdefReg_Process 
        won't parse correctly chunked records   */
    /*  Used to remember the last valid TNF */
    uint8_t                     validPreviousTnf; /**< \internal The last valid TNF that we had. */
 
    uint32_t                    NumberOfNdefTypes;/**< \internal */

    uint32_t                    RecordIndex;      /**< \internal */

    uint32_t                    RtdIndex;         /**< \internal */

    /*  This flag is used to remember whether we have found a 
        TNF which matches with the Registered RTD */
    uint8_t                     MainTnfFound;     /**< \internal */
    
    /*  This flag is used to tell whether the present record
        being processed is newly extracted */
    uint8_t                     newRecordextracted;/**< \internal */

}phFriNfc_NdefReg_t;


#ifndef PH_FRINFC_EXCLUDE_FROM_TESTFW /* */

/**
 * \brief Ndef Registry \b Reset function
 *
 * \copydoc page_reg
 * 
 * Resets the component instance to the initial state and lets the component forget about
 * the list of registered items. Does basic initialisation.
 *
 * \param[in] NdefReg Pointer to a valid or uninitialised instance of \ref phFriNfc_NdefReg_t .
 *
 * \param[in] NdefTypesarray Array of pointers to individual NDEF Types. Later used to store
 *            the NdefTypes
 *
 * \param[in] RecordsExtracted Pointer to an uninitialised instance of the NDEF Record structure
 *            that is later used to retrieve the record information.
 *
 * \param[in] CbParam Pointer to an un-initialised instance of \ref phFriNfc_NdefReg_CbParam_t
 *            structure, which is later used to store the callback parameters.
 *
 * \param[in] ChunkedRecordsarray Pointer to an array of bytes. Later used to store the
 *            Chunked record flags.
 *
 * \param[in] NumberOfRecords The number of members in the arrays NdefTypesarray and ChunkedRecordsarray.
 *
 * \retval NFCSTATUS_SUCCESS                The operation has been successful.
 * \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
 *
 * \note  This function has to be called at the beginning, after creating an instance of
 *        \ref phFriNfc_NdefReg_t .
 */
NFCSTATUS phFriNfc_NdefReg_Reset(phFriNfc_NdefReg_t          *NdefReg,
                                 uint8_t                    **NdefTypesarray,  
                                 phFriNfc_NdefRecord_t       *RecordsExtracted,
                                 phFriNfc_NdefReg_CbParam_t  *CbParam,
                                 uint8_t                     *ChunkedRecordsarray,  
                                 uint32_t                     NumberOfRecords);


/**
 * \brief Ndef Registry \b Add \b Callback function
 *
 * \copydoc page_reg 
 * 
 * Adds an NDEF type listener to the (internal) list of listeners:
 * The registering caller or embedding SW must create an instance of \ref phFriNfc_NdefReg_Cb_t and
 * hand the reference over to this function. The library does no allocation of memory.
 *
 * \param[in] NdefReg Pointer to an initialised instance of \ref phFriNfc_NdefReg_t that holds the
 *            context of the current component instance.
 *
 * \param[in] NdefCb Pointer to a caller-initialised structure describing the context of a Listener
 *            that requests its registration.
 *
 * \retval NFCSTATUS_SUCCESS                                        The operation has been successful.
 * \retval NFCSTATUS_INVALID_PARAMETER                              At least one parameter of the function 
 *                                                                  is invalid. or Number of RTDs in NdefCb 
 *                                                                  Structure is greater than 
 *                                                                  PH_FRINFC_NDEFREG_MAX_RTD_REGISTERED
 *
 * \note This function returns once the listener is registered successfully or an error occurs.
 */
NFCSTATUS phFriNfc_NdefReg_AddCb(phFriNfc_NdefReg_t     *NdefReg,
                                 phFriNfc_NdefReg_Cb_t  *NdefCb);


/**
 * \brief NDEF Registry \b Remove \b Callback function
 *
 * \copydoc page_reg
 * 
 * Removes a specific listener from the list: The element to remove is specified by its address.
 * As the library does no de-allocation the caller of this function  needs to take care of the
 * correct disposal of the removed \ref phFriNfc_NdefReg_Cb_t instance once this function returns
 * successfully.
 *
 * \param[in] NdefReg Pointer to an initialised instance of \ref phFriNfc_NdefReg_t that holds the
 *            context of this component.
 *
 * \param[in] NdefCb Pointer to a caller-initialised structure describing the context of a Listener
 *            that requests its un-registration.
 *
 * \retval NFCSTATUS_SUCCESS                The operation has been successful.
 * \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
 * \retval NFCSTATUS_NODE_NOT_FOUND         If the internal list is NULL or a list node is not found.
 *
 * \note This function returns once the listener is removed successfully or an error occurs.
 */
NFCSTATUS phFriNfc_NdefReg_RmCb(phFriNfc_NdefReg_t    *NdefReg,
                                phFriNfc_NdefReg_Cb_t *NdefCb);



/**
 * \brief NDEF Registry \b Dispatch \b Packet function
 *
 * \copydoc page_reg
 * 
 * The entry point for NDEF \b PACKETS retrieved from the Peer (Remote) Device:
 * The function performs a reset of the state. It starts the action (state machine). For actual
 * processing a periodic call of \ref phFriNfc_NdefReg_Process has to be done. This
 * function parses the Message, isolates the record, looks for a match with the registered
 * RTDs and if a match is found, it calls the related callback. This procedure is done for each
 * record in the Message
 *
 * \param[in] NdefReg Pointer to an initialised instance of \ref phFriNfc_NdefReg_t that holds the
 *            context of this component.
 *
 * \param[in] PacketData Pointer to a NDEF Packet that has been received.
 *
 * \param[in] PacketDataLength Length of the NDEF packet to process.
 *
 * \retval NFCSTATUS_SUCCESS                The operation has been successfully initiated.
 * \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
 *
 * \note This function returns once the operation is initiated or an error occurs.
 *
 */
NFCSTATUS phFriNfc_NdefReg_DispatchPacket(phFriNfc_NdefReg_t    *NdefReg,
                                          uint8_t               *PacketData,
                                          uint16_t               PacketDataLength);


/**
 * \brief NDEF Registry \b Dispatch \b Record function
 *
 * \copydoc page_reg
 * 
 * The entry point for NDEF \b RECORDS retrieved from the Peer (Remote) Device:
 * The function performs a reset of the state. It starts the action (state machine). For actual
 * processing a periodic call of \ref phFriNfc_NdefReg_Process has to be done. This
 * function compares the given record with the registered RTDs and if a match is found it calls
 * the related callback.
 *
 * \param[in] NdefReg Pointer to an initialised instance of \ref phFriNfc_NdefReg_t that holds the
 *            context of this component.
 *
 * \param[in] RecordsExtracted Pointer to a NDEF Record that has been received.
 *
 * \retval NFCSTATUS_SUCCESS                The operation has been successfully initiated.
 * \retval NFCSTATUS_INVALID_PARAMETER      At least one parameter of the function is invalid.
 *
 * \note This function returns once the process is initiated or an error occurs.
 */
NFCSTATUS phFriNfc_NdefReg_DispatchRecord(phFriNfc_NdefReg_t     *NdefReg,
                                          phFriNfc_NdefRecord_t  *RecordsExtracted);


/**
 * \brief NDEF Registry \b Process function
 *
 * \copydoc page_reg
 * 
 * Processing function, needed to avoid long blocking and to give control to other parts of the software
 * between the internal dispatching of data.
 * The function needs to be called during processing, within a message loop or a simple loop until its
 * return value tells that it has finished. No State reset is performed during operation.
 *
 * \param NdefReg Pointer to a valid instance of the \ref phFriNfc_NdefReg_t structure describing
 *                the component context.
 *
 * \param Status  Pointer to a variable receiving the final result of the NDEF data processing operation.
 *                There are the following values:
 *                \li NFCSTATUS_SUCCESS                   The operation has been successful.
 *                \li NFCSTATUS_INVALID_PARAMETER         At least one parameter of the function is invalid.
 *                \li NFCSTATUS_NODE_NOT_FOUND            If the List is NULL or Node is not found.
 *                \li NFCSTATUS_INVALID_DEVICE_REQUEST    State other than the specified in the File.
 *                
 * \retval        FALSE Processing has finished, no more function call is needed.
 * \retval        TRUE  Processing is ongoing, the function must be called again.
 */
uint8_t phFriNfc_NdefReg_Process(phFriNfc_NdefReg_t  *NdefReg,
                                 NFCSTATUS           *Status);

/*@}*/ /* defgroup */

#endif /* PH_FRINFC_EXCLUDE_FROM_TESTFW */

#endif /* PHFRINFCNDEFREG_H */

