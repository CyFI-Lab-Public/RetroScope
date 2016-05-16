/*---------------------------------------------------------------------------*
 *  memmove.h  *
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

#ifndef _inl_memmove_
#define _inl_memmove_

#if USE_SYSTEM_MEMMOVE
#define MEMMOVE(S,D,N,Z) memmove(S,D,(N)*(Z))
#else
#define MEMMOVE memmove_inline

static void *memmove_inline(void *dest, const void *src, size_t count,
                            size_t size);

static void *memmove_inline(void *dest, const void *src, size_t count,
                            size_t size)
/*
**  A more efficient implementation of memmove
*/
{
  ASSERT(dest);
  ASSERT(src);

  while (count-- > 0)
  {
    memcpy(dest, src, size);
    dest = (void *)(((char *)dest) + size);
    src = (void *)(((char *)src) + size);
  }
  return (dest);  /* TODO: fix return */
}
#endif
#endif
