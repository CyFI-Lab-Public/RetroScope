/*---------------------------------------------------------------------------*
 *  pmutex.h  *
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

#ifndef __PMUTEX_H
#define __PMUTEX_H

#if defined(__vxworks) && !defined(REAL_PTHREADS)
#undef  USE_THREAD
#define USE_THREAD
#undef  POSIX
#define POSIX
#endif



#include "ESR_ReturnCode.h"

#define SECOND2NSECOND	1000000000
#define SECOND2MSECOND	1000
#define MSECOND2NSECOND	1000000

#ifdef USE_THREAD

#ifdef _WIN32

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include "windows.h"

typedef HANDLE MUTEX;
typedef HANDLE EVENT;

#define createMutex(mutex, locked) \
  (*mutex = CreateMutex(NULL, locked, NULL)) == 0 ? ESR_MUTEX_CREATION_ERROR : ESR_SUCCESS

#define lockMutex(mutex) waitForHandle(mutex, INFINITE)
#define unlockMutex(mutex) (ReleaseMutex(*mutex) ? ESR_SUCCESS : ESR_FATAL_ERROR)
#define deleteMutex(mutex) ((void) CloseHandle(*mutex))
ESR_ReturnCode waitForHandle(HANDLE* handle, asr_uint32_t timeout);

#elif defined(POSIX)

#if defined(__vxworks) && !defined(REAL_PTHREADS)
#include "pthread_vx.h"
#else
#include <pthread.h>

#ifndef _POSIX_THREADS
#error "Thread is not defined!"
#endif
#endif /* _VX_WORKS_ */

typedef pthread_mutex_t MUTEX;
typedef pthread_cond_t EVENT;

ESR_ReturnCode createMutex_posix(MUTEX *mutex, ESR_BOOL locked);
ESR_ReturnCode deleteMutex_posix(MUTEX *mutex);

#define createMutex(mutex, locked) createMutex_posix(mutex, locked)
#define deleteMutex(mutex) deleteMutex_posix(mutex)
#define lockMutex(mutex) (pthread_mutex_lock(mutex) == 0 ? ESR_SUCCESS : ESR_FATAL_ERROR)
#define unlockMutex(mutex) (pthread_mutex_unlock(mutex) == 0 ? ESR_SUCCESS : ESR_FATAL_ERROR)

#else /* NON_POSIX */

#error Portable Synchronization not defined for this OS!

#endif /* _WIN32 */

#else /* USE_THREAD */

#define createMutex(mutex, locked) (ESR_SUCCESS)
#define deleteMutex(mutex) 
#define lockMutex(mutex) ((void) 0)
#define unlockMutex(mutex) ((void) 0)

#endif /* USE_THREAD */

#endif  
