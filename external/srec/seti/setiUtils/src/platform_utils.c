/*---------------------------------------------------------------------------*
 *  platform_utils.c  *
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



#ifndef NO_STDERR  
#include <stdio.h>
#endif

#include <string.h>
#include "pmemory.h"
#include "platform_utils.h"


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

char * safe_strtok(char *input_str, char *seps, int * token_len)
{
  int i, j, k, n, m;
  int sep_found;
  char *pos;

  m = strlen(seps);

  if (!m || !input_str)
    return NULL;

  n = strlen(input_str);
  pos = input_str;  

  for (i=0, sep_found = 0; i<n; i++) {
    for (j=0; j<m; j++) {
      if (*pos == seps[j]) {
        /* found seperator */
        sep_found++;
        break;
      }
    }
    if (sep_found == i) {
      /* found first non separator */ 
      break;      
    }
    pos++;
  }

  *token_len = 0;

  /* now find ending position of token */
  for (k=i; k<n; k++) {
    for (j=0; j<m; j++) {
      if (pos[k-i] == seps[j]) {
        /* first occurance equals separator*/
        return pos;
      }
    }
    (*token_len)++;
  }

  /* no more tokens */
  return pos;
}


/* C54 and WinCE does not have strdup or stricmp */
#if defined TI_DSP || defined UNDER_CE 
int stricmp(const char *str1, const char *str2)
{
	if(str1 == NULL || str2 == NULL){
#ifndef NO_STDERR      
		PLogError(L("stricmp: str1 or str2 is NULL\n"));
#endif		
        exit (1);
	}

	for (; *str1 != '\0' && *str2 != '\0' && tolower(*str1) == tolower(*str2);
		str1++, str2++)
		;
	if (*str1 == '\0')
		return *str2 == '\0'? 0 : -1;
	else if (*str2 == '\0')
		return 1;
	else
		return tolower(*str1) < tolower(*str2)? -1 : 1;
}

char * strdup(char *in_string)
{
	char * new_string = NULL;	

	if(in_string == NULL){
#ifndef NO_STDERR
		PLogError(L("strdup: input string is NULL\n"));
#endif		
        return NULL;
	}

	new_string = (char *)MALLOC(sizeof(char)*(strlen(in_string)+1), MTAG);
	if(!new_string)
		return NULL;
	
	strcpy(new_string, in_string);	
	return new_string;	
}
#endif

