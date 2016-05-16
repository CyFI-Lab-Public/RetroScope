/*---------------------------------------------------------------------------*
 *  SWIslts.h  *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                               *
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the 'License');          *
 *  you may not use this file except in compliance with the License.         *
 *                                                                           *
 *  You may obtain a copy of the License at                                  *
 *      http://www.apache.org/licenses/LICENSE-2.0                           *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an 'AS IS' BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * 
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *---------------------------------------------------------------------------*/



#ifndef _SWI_SLTS_H__
#define _SWI_SLTS_H__

#ifdef SWISLTS_USE_STATIC_API
  #define SWISLTS_FNDECLARE
#else
  #ifdef WIN32
    #ifdef _IN_SWI_SLTS__
      #define SWISLTS_FNDECLARE __declspec(dllexport)
    #else
      #define SWISLTS_FNDECLARE __declspec(dllimport)
    #endif
  #else
    #define SWISLTS_FNDECLARE
  #endif
#endif

typedef void * SWIsltsHand;

typedef enum {
  SWIsltsSuccess = 1,
  SWIsltsErrAllocResource = 2,
  SWIsltsInvalidParam = 3,
  SWIsltsMaxInputExceeded = 4,
  SWIsltsEmptyPhoneString = 5,
  SWIsltsInternalErr = 6,
  SSWIsltsUnknownInputChar = 7,
  SWIsltsFileOpenErr = 8,
  SWIsltsFileReadErr = 9,
  SWIsltsFileWriteErr = 10
}SWIsltsResult;

typedef struct SWIsltsTranscription {
  void  * pBuffer;
  size_t  nSizeOfBuffer;
} SWIsltsTranscription;

typedef struct SWIsltsWrapper {
  SWIsltsResult (*init) (void);
  SWIsltsResult (*term) (void);
  SWIsltsResult (*open) (SWIsltsHand *phLts, 
                         const char *data_filename);  
  SWIsltsResult (*close) (SWIsltsHand hLts);
  SWIsltsResult (*textToPhone) (SWIsltsHand hLts,
                                const char *text, 
                                char **output_phone_string,
                                int *output_phone_len,
                                int max_phone_len);
} SWIsltsWrapper;

typedef SWIsltsWrapper * SWIsltsWrapperHand;

SWISLTS_FNDECLARE SWIsltsResult SWIsltsGetWrapper(SWIsltsWrapperHand *phLtsWrap);
SWISLTS_FNDECLARE SWIsltsResult SWIsltsReleaseWrapper(SWIsltsWrapperHand hLtsWrap);

typedef SWIsltsResult (*pSWIsltsReleaseWrapper)(SWIsltsWrapperHand);              
typedef SWIsltsResult (*pSWIsltsGetWrapper)(SWIsltsWrapperHand *);

SWISLTS_FNDECLARE SWIsltsResult SWIsltsInit(void);
SWISLTS_FNDECLARE SWIsltsResult SWIsltsTerm(void);

SWISLTS_FNDECLARE SWIsltsResult SWIsltsOpen(SWIsltsHand *phLts, 
                                            const char *data_filename);
SWISLTS_FNDECLARE SWIsltsResult SWIsltsClose(SWIsltsHand hLts);

SWISLTS_FNDECLARE SWIsltsResult SWIsltsTextToPhone(SWIsltsHand hLts, 
                                                   const char *text, 
                                                   char **output_phone_string,
                                                   int *output_phone_len,
                                                   int max_phone_len);

SWISLTS_FNDECLARE SWIsltsResult SWIsltsG2PGetWordTranscriptions(SWIsltsHand hLts, 
                                                               const char *text,
                                                               SWIsltsTranscription **ppTranscriptions,
                                                               int *pnNbrOfTranscriptions);

SWISLTS_FNDECLARE SWIsltsResult SWIsltsG2PFreeWordTranscriptions(SWIsltsHand hLts, 
                                                                 SWIsltsTranscription *pTranscriptions);
#endif  /* _SWI_SLTS_H__ */


