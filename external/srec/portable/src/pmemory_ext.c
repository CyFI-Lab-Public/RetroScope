/*---------------------------------------------------------------------------*
 *  pmemory_ext.c  *
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



#include "pmemory.h"
#include "ptrd.h"
#include "pmutex.h"
#include "passert.h"
#include "pmemory_ext.h"
#include "pmalloc.h"

#ifdef __cplusplus
extern "C"
{
#endif

#if defined(USE_THREAD) && defined(USE_DINKUM_LIB_DIRECT)
  static MUTEX memextMutex;
#endif
  
#ifdef RTXC
  void* operator new(size_t size)
  {
    return (PortNew(size));
  }
  void  operator delete(void* ptr)
  {
    PortDelete(ptr);
  }
#endif
  
#if defined(PORTABLE_DINKUM_MEM_MGR) || defined(PORTABLE_FIXED_SIZE_MEM_BLOCK_SCHEME)
  
  /* to assist with leak checking */
static int portNewCount = 0;
static int portDeleteCount = 0;

  /* enable writing and checking of guard words if debugging is enabled */
#ifdef _DEBUG
  /* crash on Xanavi's board with this option on, do not know why */
  /* #define DBG_GUARD_WORDS */
#endif /* _DEBUG */
  
  /* ************************************************************************************
   * PORTABLE_PSOS_BLOCK_SCHEME_MEM_MGR || PORTABLE_DINKUM_MEM_MGR || PORTABLE_FIXED_SIZE_MEM_BLOCK_SCHEME
   * ************************************************************************************/
  
  /* data ******************************************************************************/
  
  static BOOL  gMemoryInitted = FALSE; /* TODO: Temporary fix to PortTerm failure */
  
#define MEM_MGR_GetPoolSize()     PortMallocGetPoolSize()
#define MEM_MGR_SetPoolSize(sizeInBytes)  PortMallocSetPoolSize(sizeInBytes)
#define MEM_MGR_Init()       PortMallocInit()
#define MEM_MGR_Term()              PortMallocTerm()
#define MEM_MGR_Allocate(sizeInBytes)   PortMalloc(sizeInBytes)
#define MEM_MGR_Free(objectPtr)     PortFree(objectPtr)
#define MEM_MGR_Dump()
#define MEM_MGR_GetMaxMemUsed()     PortMallocGetMaxMemUsed()
  
  /* guard word data ********************************************************/
  
#ifdef DBG_GUARD_WORDS
#define GUARD_BEGIN  0xbbbbbbbb
#define GUARD_END    0xeeeeeeee
  
#define GUARD_OFF_REQ_SIZE   0
#define GUARD_OFF_START    sizeof(unsigned int)
#define GUARD_OFF_PTR    (sizeof(unsigned int) + sizeof(unsigned int))
#define GUARD_EXTRA     (sizeof(unsigned int) + sizeof(unsigned int) + sizeof(unsigned int))
#define GUARD_OFF_END(allocSize)    ((allocSize) - sizeof(unsigned int))
#define GUARD_ALLOC_SIZE(reqSize)   ((reqSize)+GUARD_EXTRA)
  
#define GUARD_PTR_FIELD(ptr,off) (unsigned int *)((char *)(ptr) + (off))
#define GUARD_ALLOC_PTR(ptr)  (void*) ((char *)(ptr) - GUARD_OFF_PTR)
#endif
  
  /* scan guard words data **************************************************/
  
  /* maintain a static list of allocated blocks (didn't want to perform any dynamic allocation).
   * This list can be scanned by PortMemScan() to determine if any allocated blocks
   * have overwritten their guard words.
   * Calling PortDelete() will check guard words upon de-allocation, but many
   * allocated blocks are only freed at program termination, which sometimes doesn't happen.
   *
   * This software is enabled separately with DBG_SCAN_GUARD_WORDS, because the performance
   * overhead is severe.
   */
#ifdef DBG_SCAN_GUARD_WORDS
#define MAX_ALLOCATED_BLOCKS  80000
  static void *allocArray[MAX_ALLOCATED_BLOCKS+1];
  static int allocArrayCount = 0;
  
  void AddToAllocList(void *memPtr);
  void RemoveFromAllocList(void *memPtr);
  
#define ADD_TO_ALLOC_LIST(ptr)   AddToAllocList(ptr)
#define REMOVE_FROM_ALLOC_LIST(ptr)  RemoveFromAllocList(ptr)
  
#else
#define ADD_TO_ALLOC_LIST(ptr)
#define REMOVE_FROM_ALLOC_LIST(ptr)
#endif
  
  /* Guard Functions ********************************************************/
  
#ifdef DBG_SCAN_GUARD_WORDS
  /* AddToAllocList() : maintain an array of allocated blocks that can be
   * used by PortMemScan() to check for overwritten guard words.
   */
  void AddToAllocList(void *memPtr)
  {
    allocArray[allocArrayCount] = memPtr;
    allocArrayCount++;
    if (allocArrayCount >= MAX_ALLOCATED_BLOCKS)
    {
      char buf[256];
      sprintf(buf, "AddToAllocList ERROR : MAX_ALLOCATED_BLOCKS is too small (%d)", allocArrayCount);
      PORT_INTERNAL_ERROR(buf);
    }
  }
  
  /* RemoveFromAllocList() : maintain an array of allocated blocks that can be
   * used by PortMemScan() to check for overwritten guard words.
   */
  void RemoveFromAllocList(void *memPtr)
  {
    int i;               /* loop index */
    int j;               /* loop index */
    int inList = FALSE;  /* TRUE when found in list */
    
    for (i = 0; i < allocArrayCount; i++)
    {
      if (allocArray[i] == memPtr)
      {
        inList = TRUE;
        break;
      }
    }
    PORT_ASSERT(inList == TRUE);  /* MUST be in list */
    /* remove by sliding down all following entries */
    for (j = i + 1; j < allocArrayCount; j++)
      allocArray[j-1] = allocArray[j];
    allocArrayCount--;
    allocArray[allocArrayCount] = NULL; /* clear out end of list */
  }
  
  /* PortMemScan() : scan the array of allocated blocks, confirming that no
   * allocated block has overwritten its guard words.
   */
  void PortMemScan(void)
  {
    int          i;
    
    PortCriticalSectionEnter(&PortMemoryCriticalSection);
    
    /* scan the allocated memory list */
    for (i = 0; i < allocArrayCount; i++)
    {
      /* verify that guard words have not been corrupted */
      void   *memPtr   = allocArray[i];
      void         *allocPtr      = GUARD_ALLOC_PTR(memPtr);
      unsigned int *requestedSizePtr  = GUARD_PTR_FIELD(allocPtr, GUARD_OFF_REQ_SIZE);
      unsigned int *guardStartPtr     = GUARD_PTR_FIELD(allocPtr, GUARD_OFF_START);
      unsigned int *guardEndPtr       = GUARD_PTR_FIELD(allocPtr, GUARD_OFF_END(GUARD_ALLOC_SIZE(*requestedSizePtr)));
      
      if ((*guardStartPtr) != GUARD_BEGIN)
      {
        PLogError("PortMemScan : corrupted start guard from block 0x%08x \n", (int)memPtr);
      }
      if ((*guardEndPtr)   != GUARD_END)
      {
        PLogError("PortMemScan : corrupted end guard from block 0x%08x \n", (int)memPtr);
      }
    }
    
    PortCriticalSectionLeave(&PortMemoryCriticalSection);
  }
#endif /* DBG_SCAN_GUARD_WORDS */
  
  /* Port Memory Functions ******************************************************/
  
  /* PortMemGetPoolSize() : return size of portable memory pool, or 0 if
   * unknown.
   */
  int  PortMemGetPoolSize(void)
  {
    return MEM_MGR_GetPoolSize();
  }
  
  /* PortMemSetPoolSize() : set size of portable memory pool on PSOS.
   * This must be called before PortMemoryInit(), which is called by PortInit().
   */
  void PortMemSetPoolSize(size_t sizeInBytes)
  {
    MEM_MGR_SetPoolSize(sizeInBytes);
  }
  
  /* PortMemoryInit() :
   */
  
  int  PortMemoryInit(void)
  {
#if defined(USE_THREAD) && defined(USE_DINKUM_LIB_DIRECT)
    if (createMutex(&memextMutex) == ESR_SUCCESS)
#endif
    {
      if (!gMemoryInitted)
      {
        MEM_MGR_Init();
        gMemoryInitted = TRUE;
      }
    }
    
    return gMemoryInitted;
  }
  
  /* PortMemoryTerm() :
   */
  
  void  PortMemoryTerm(void)
  {
    /* TODO: MEM_PSOS_BLOCK_SCHEME
     * Figure out why free memory causes rn#0 is get messed up! */
    MEM_MGR_Term();
#if defined(USE_THREAD) && defined(USE_DINKUM_LIB_DIRECT)
    deleteMutex(&memextMutex);
#endif
    gMemoryInitted = FALSE;
  }
  
  /* PortNew() :
   */
  
  void* PortNew(size_t sizeInBytes)
  {
    if (gMemoryInitted)
    {
      void *pMemory = NULL;
      
#if defined(USE_THREAD) && defined(USE_DINKUM_LIB_DIRECT)
      lockMutex(&memextMutex);
#endif
      portNewCount++;
      
#ifdef DBG_GUARD_WORDS
      sizeInBytes += GUARD_EXTRA; /* space for: requestedSize,guardStart,guardEnd */
#endif
      
      pMemory = MEM_MGR_Allocate(sizeInBytes);
      
#ifdef DBG_GUARD_WORDS
      if (NULL != pMemory)
      {
        /* at the beginning of the buffer, store the requested size and a guard word.
         * Store another guard word at the end of the buffer.
         */
        /* set guard words at either end of allocated buffer; will be checked at delete time */
        unsigned int * requestedSizePtr  = GUARD_PTR_FIELD(pMemory, GUARD_OFF_REQ_SIZE);
        unsigned int * guardStartPtr     = GUARD_PTR_FIELD(pMemory, GUARD_OFF_START);
        unsigned int * guardEndPtr       = GUARD_PTR_FIELD(pMemory, GUARD_OFF_END(sizeInBytes));
        
        *requestedSizePtr = sizeInBytes - GUARD_EXTRA;
        *guardStartPtr    = GUARD_BEGIN;
        *guardEndPtr      = GUARD_END;
        pMemory     = (void *) GUARD_PTR_FIELD(pMemory, GUARD_OFF_PTR);
        ADD_TO_ALLOC_LIST(pMemory);
      }
#endif /* DBG_GUARD_WORDS */
#if defined(USE_THREAD) && defined(USE_DINKUM_LIB_DIRECT)
      unlockMutex(&memextMutex);
#endif
      return pMemory;
    }
#ifdef PSOSIM
    /* PSOSIM's license manager calls new() before PSOS is running */
    else
    {
      return(malloc(sizeInBytes));
    }
#else  /* PSOSIM */
    /* Memory allocator not initialized when request for memory was made */
    passert(FALSE && "Call PortInit() before calling any portable functions\r\n");
    return NULL;
#endif /* PSOSIM */
  }
  
  void PortDelete(void* objectPtr)
  {
#if defined(USE_THREAD) && defined(USE_DINKUM_LIB_DIRECT)
    lockMutex(&memextMutex);
#endif
    portDeleteCount++;
    
#ifdef DBG_GUARD_WORDS
    {
      /* verify that guard words have not been corrupted */
      void *allocPtr     = GUARD_ALLOC_PTR(objectPtr);
      unsigned int *requestedSizePtr  = GUARD_PTR_FIELD(allocPtr, GUARD_OFF_REQ_SIZE);
      unsigned int *guardStartPtr     = GUARD_PTR_FIELD(allocPtr, GUARD_OFF_START);
      unsigned int *guardEndPtr       = GUARD_PTR_FIELD(allocPtr, GUARD_OFF_END(GUARD_ALLOC_SIZE(*requestedSizePtr)));
      
      passert((*guardStartPtr) == GUARD_BEGIN);
      passert((*guardEndPtr)   == GUARD_END);
      REMOVE_FROM_ALLOC_LIST(allocPtr);
      objectPtr = allocPtr;
    }
#endif
    
    MEM_MGR_Free(objectPtr);
#if defined(USE_THREAD) && defined(USE_DINKUM_LIB_DIRECT)
    unlockMutex(&memextMutex);
#endif
  }
  
  void PortMemTrackDump(void)
  {
    MEM_MGR_Dump();
  }
  
  /* PortGetMaxMemUsed() : return the maximum real memory allocated.
   * There is another function of the same name in pmalloc.c, for tracking
   * non-psos block memory. It uses #ifndef MEM_PSOS_BLOCK_SCHEME to enable.
   */
  int PortGetMaxMemUsed(void)
  {
    return MEM_MGR_GetMaxMemUsed();
  }
  
  /* PortMemCntReset() : reset the New/Delete count.
   * This is useful for checking that each new has a corresponding delete once
   * the system gets into a steady state.
   */
  void PortMemCntReset()
  {
    portNewCount = 0;
    portDeleteCount = 0;
  }
  
  
  /* PortMemGetCount() : return the accumulated new & delete counts */
  void PortMemGetCount(int *newCount, int *deleteCount)
  {
    *newCount    = portNewCount;
    *deleteCount = portDeleteCount;
  }
  
#endif /* (==PORTABLE_PSOS_BLOCK_SCHEME_MEM_MGR) || (==PORTABLE_DINKUM_MEM_MGR) || (==PORTABLE_FIXED_SIZE_MEM_BLOCK_SCHEME) */
  
#ifdef __cplusplus
}
#endif
