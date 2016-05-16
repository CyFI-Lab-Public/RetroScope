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
 * \file  phFriNfc.h
 * \brief NFC FRI Main Header.
 *
 * Project: NFC-FRI
 *
 * $Date: Mon Dec 13 14:14:13 2010 $
 * $Author: ing02260 $
 * $Revision: 1.20 $
 * $Aliases:  $
 *
 */

#ifndef PHFRINFC_H /* */
#define PHFRINFC_H /* */
#include <phNfcTypes.h>
#include <phOsalNfc.h>

#define PH_HAL4_ENABLE

#ifdef PH_HAL4_ENABLE
    #include <phNfcConfig.h>
    #define LOCK_BITS_CHECK_ENABLE
#endif

#define FRINFC_READONLY_NDEF

#ifdef  DISABLE_MIFARE_MAPPING
#define PH_FRINFC_MAP_MIFAREUL_DISABLED
#define PH_FRINFC_MAP_MIFARESTD_DISABLED
#define PH_FRINFC_MAP_DESFIRE_DISABLED
#else
#define PH_NDEF_MIFARE_ULC
#endif

#ifdef DISABLE_FELICA_MAPPING
#define PH_FRINFC_MAP_FELICA_DISABLED
#endif

#ifdef DISABLE_JEWEL_MAPPING
#define PH_FRINFC_MAP_TOPAZ_DISABLED
#define PH_FRINFC_MAP_TOPAZ_DYNAMIC_DISABLED
#endif

#ifdef DISABLE_ISO15693_MAPPING
#define PH_FRINFC_MAP_ISO15693_DISABLED
#endif


#ifdef DISABLE_FORMAT
#define PH_FRINFC_FMT_DESFIRE_DISABLED
#define PH_FRINFC_FMT_MIFAREUL_DISABLED
#define PH_FRINFC_FMT_MIFARESTD_DISABLED
#define PH_FRINFC_FMT_ISO15693_DISABLED
#endif /* #ifdef DISABLE_FORMAT */

#define PH_FRINFC_FMT_TOPAZ_DISABLED

/*! 
 *  \name NFC FRI Main Header
 *
 * File: \ref phFriNfc.h
 *
 */
/*@{*/

#define PH_FRINFC_FILEREVISION "$Revision: 1.20 $"   /**< \ingroup grp_file_attributes */
#define PH_FRINFC_FILEALIASES  "$Aliases:  $"       /**< \ingroup grp_file_attributes */

/*@}*/


/*!
 *  \ingroup grp_fri_nfc_common
 *
 *  \brief \copydoc page_cb Completion Routine
 *
 * NFC-FRI components that work in an overlapped style need to provide a function that is compatible
 * to this definition.\n\n
 * It is \b mandatory to define such a routine for components that interact with other components up or
 * down the stack. Moreover, such components shall provide a function within their API to enable the
 * setting of the \b Completion \b Routine address and parameters.
 *
 * \par First Parameter: Context
 *      Set to the address of the called instance (component instance context structure). For instance,
 *      a component that needs to give control to a component up the stack needs to call the completion
 *      routine of the \b upper component. The value to assign to this parameter is the \b address of
 *      the context structure instance of the called component. Such a structure usually contains all
 *      variables, data or state information a component member needs for operation. The address of the
 *      upper instance must be known by the lower (completing) instance. The mechanism to ensure that this
 *      information is present involves the structure \ref phFriNfc_CplRt_t . See its documentation for
 *      further information.
 *
 * \par Second Parameter: Status Value
 *      The lower layer hands over the completion status via this parameter. The completion 
 *      routine that has been called needs to process the status in a way that is comparable to what
 *      a regular function return value would require.
 *
 * \note The prototype of the component's \b Process(ing) functions has to be compatible to this
 *       function pointer declaration for components interacting with others. In other cases, where
 *       there is no interaction or asynchronous processing the definition of the \b Process(ing)
 *       function can be arbitrary, if present at all.
 */

typedef void (*pphFriNfc_Cr_t)(void*, NFCSTATUS);


/*!
 * \ingroup grp_fri_nfc_common
 *
 * \brief Completion Routine structure
 *
 * This structure finds itself within each component that requires to report completion
 * to an upper (calling) component.\n\n
 * Depending on the actual implementation (static or dynamic completion information) the stack
 * initialisation \b or the calling component needs to inform the initialised \b or called component
 * about the completion path. This information is submitted via this structure.
 *
 */
typedef struct phFriNfc_CplRt
{
    pphFriNfc_Cr_t    CompletionRoutine; /*!< Address of the upper Layer's \b Process(ing) function to call upon completion.
                                          *   The stack initialiser (or depending on the implementation: the calling component) 
                                          *   needs to set this member to the address of the function that needs to be within
                                          *   the completion path: A calling component would give its own processing function 
                                          *   address to the lower layer.
                                          */
    void             *Context;           /*!< Instance address (context) parameter.
                                          *   The stack initialiser (or depending on the implementation: the calling component) 
                                          *   needs to set this member to the address of the component context structure instance
                                          *   within the completion path: A calling component would give its own instance address
                                          *   to the lower layer.
                                          */
} phFriNfc_CplRt_t;


#define NFCSTATUS_INVALID_DEVICE_REQUEST                        (0x10F5)


#endif /* __PHFRINFC_H__ */
