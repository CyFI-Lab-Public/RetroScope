/*---------------------------------------------------------------------------*
 *  pmemory_ext.h  *
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



#ifndef _PORTMEMORY_EXT_H
#define _PORTMEMORY_EXT_H

#include <stddef.h>

#ifdef __cplusplus
extern "C"
{
#endif

  /* TODO - removed PortNewAndTrace and PortDeleteAndTrace * Ian 06-March-2002 */
  
  int  PortMemoryInit(void);
  void PortMemoryTerm(void);
  void* PortNew(size_t sizeInBytes);
  void PortDelete(void* objectPtr);
  void PortMemTrackDump(void);
  int   PortGetMaxMemUsed(void);
  int  PortMemGetPoolSize(void);
  void PortMemSetPoolSize(size_t sizeInBytes);
  
#ifdef __cplusplus
}
#endif

#endif /* _PORTMEMORY_H */
