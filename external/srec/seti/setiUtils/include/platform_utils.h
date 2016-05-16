/*---------------------------------------------------------------------------*
 *  platform_utils.h  *
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



#ifndef _PLATFORM_UTILS_H_
#define _PLATFORM_UTILS_H_

/* 
  safe_strtok()
  Use this in place of regular strtok. which is dangerous because it modifies
  a global variable.  This function does not.
  
  Returns the position in NULL terminated input_str where seps are found, 
  and the length of the token
  Seps contains a NULL terminated string of separators
  Returns NULL if error
  
  If no more tokens left, token_len is 0 
*/
char * safe_strtok(char *input_str, char *seps, int * token_len);

/* C54 and WinCE does not have strdup */
#if defined TI_DSP || defined UNDER_CE || defined __CC_ARM
char * strdup(char *in_string);
#endif

#endif /* _PLATFORM_UTILS_H_ */
