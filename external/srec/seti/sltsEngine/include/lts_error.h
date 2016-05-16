/*---------------------------------------------------------------------------*
 *  lts_error.h  *
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



#ifndef _LTS_ERROR_H__
#define _LTS_ERROR_H__


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

#endif /* _LTS_ERROR_H__ */
