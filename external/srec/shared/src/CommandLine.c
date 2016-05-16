/*---------------------------------------------------------------------------*
 *  CommandLine.c  *
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

#include "ESR_CommandLine.h"

ESR_ReturnCode ESR_CommandLineGetValue(int argc, const LCHAR * argv[],
                                       LCHAR* key, LCHAR* value, size_t* len)
{
  const LCHAR* lastKeyFound = NULL;
  
  while (--argc > 0 && **++argv)
  {
    if (**argv != '-')
    {
      /* got value */
      if (key == NULL)
      {
        /* but we don't have any key to associate it with */
        continue;
      }
      else
      {
        if (lastKeyFound != NULL && LSTRCMP(lastKeyFound, key) == 0)
        {
          if (LSTRLEN(*argv) + 1 > *len)
          {
            *len = LSTRLEN(*argv) + 1;
            return ESR_BUFFER_OVERFLOW;
          }
          *len = LSTRLEN(*argv) + 1;
          LSTRCPY(value, *argv);
          return ESR_SUCCESS;
        }
      }
    }
    else
    {
      /* got key */
      if (lastKeyFound != NULL && LSTRCMP(lastKeyFound, key) == 0)
      {
        *len = 1;
        LSTRCPY(value, L(""));
        return ESR_SUCCESS;
      }
      lastKeyFound = *argv + 1;
    }
  }
  
  /* Handle case where last argument is a key with no value */
  if (lastKeyFound != NULL && LSTRCMP(lastKeyFound, key) == 0)
  {
    *len = 1;
    LSTRCPY(value, L(""));
    return ESR_SUCCESS;
  }
  return ESR_NO_MATCH_ERROR;
}
