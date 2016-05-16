/*---------------------------------------------------------------------------*
 *  pendian.c  *
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






#include "pendian.h"

void swap_byte_order(void *buffer, size_t count, size_t itemSize)
{
  char *data = (char *) buffer;
  register char *p, *q, c;
  
  /* Process every item */
  while (count > 0)
  {
    p = data;
    q = data + itemSize - 1;
    
    while (p < q)
    {
      c = *p;
      *p++ = *q;
      *q-- = c;
    }
    
    /* Prepare for next pass */
    data += itemSize;
    count--;
  }
}
