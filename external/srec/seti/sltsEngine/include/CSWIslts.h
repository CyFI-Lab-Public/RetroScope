/*---------------------------------------------------------------------------*
 *  CSWIslts.h  *
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



#ifndef _CSWISLTS_H__
#define _CSWISLTS_H__

extern "C" {
#include <SWIslts.h>
}

class CSWIslts{
public:
  CSWIslts();
  ~CSWIslts();
  SWIsltsResult init();
  SWIsltsResult term();
#ifdef USE_FSM_DICT
  SWIsltsResult open(const char *data_filename, const char *dictionary_filename);
#else
  SWIsltsResult open(const char *data_filename);  
#endif
  SWIsltsResult close();
  SWIsltsResult textToPhone(const char *text, 
                             char **output_phone_string,
                             int *output_phone_len,
                             int max_phone_len);
private:
  SWIsltsWrapperHand m_sltsWrapHand;
  SWIsltsHand m_sltsHand;
};

#endif  /* _SWI_SLTSWRAP_H__ */


