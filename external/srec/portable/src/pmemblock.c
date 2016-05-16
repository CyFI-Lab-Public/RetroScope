/*---------------------------------------------------------------------------*
 *  pmemblock.c  *
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
#include "ptypes.h"

#if PORTABLE_MEM_MGR == PORTABLE_PSOS_BLOCK_SCHEME_MEM_MGR

#ifdef PSOSIM
#define PSOS
#endif

#ifdef PSOS
#include <stdlib.h>
#include <psos.h>
#endif

#ifdef __cplusplus
extern "C"
{
#endif

  /* Data *****************************************************************/
  
#define NUM_POOL_BINS 32
#define NUM_POOL_SLOTS  8
  
  typedef struct memory_pools
  {
    uint32   currentNumberOfPools;
    
    struct pool_info
    {
      unsigned long poolId;
      void*   pMemory;
      unsigned long size;
    }
    poolInfo[NUM_POOL_SLOTS];
    
  }
  MEMORY_POOL;
  
  static MEMORY_POOL memoryPool[NUM_POOL_BINS];
  
#define NUM_TRACKING_BINS NUM_POOL_BINS
  
  /* Object tracking variables */
  static struct tracking_struct
  {
    uint32 sCurrentAllocationSize;
    uint32 sMaximumAllocationSize;
    uint32 sTotalAllocationSize;
    
    uint32  sCurrentAllocRealSize;
    uint32  sMaximumAllocRealSize;
    uint32  sTotalAllocRealSize;
    
    uint32 sCurrentAllocationNumber;
    uint32 sMaximumAllocationNumber;
    uint32 sTotalAllocationNumber;
    
    uint32 sCurrentAllocationNumberArray[NUM_TRACKING_BINS];
    uint32 sMaximumAllocationNumberArray[NUM_TRACKING_BINS];
    uint32 sTotalAllocationNumberArray[NUM_TRACKING_BINS];
    
    uint32 sCurrentAllocationSizeArray[NUM_TRACKING_BINS];
    uint32 sMaximumAllocationSizeArray[NUM_TRACKING_BINS];
    uint32 sTotalAllocationSizeArray[NUM_TRACKING_BINS];
  }
  gMemoryTracking;
  
  
  /* Functions *********************************************************/
  
  static uint32 findBin(size_t size)
  {
    int i, bin;
    for (i = 0, bin = 1; i < NUM_TRACKING_BINS; i++, bin <<= 1)
    {
      if ((int)size <= bin)
        return i;
    }
    
    return 0;
  }
  
  
  static void MemoryTrackingInit(void)
  {
    int i;
    /* Initialization of object tracking variables */
    gMemoryTracking.sCurrentAllocationSize = 0;
    gMemoryTracking.sMaximumAllocationSize = 0;
    gMemoryTracking.sTotalAllocationSize = 0;
    
    gMemoryTracking.sCurrentAllocationNumber = 0;
    gMemoryTracking.sMaximumAllocationNumber = 0;
    gMemoryTracking.sTotalAllocationNumber = 0;
    
    gMemoryTracking.sCurrentAllocRealSize = 0;
    gMemoryTracking.sMaximumAllocRealSize = 0;
    gMemoryTracking.sTotalAllocRealSize = 0;
    
    for (i = 0; i < NUM_TRACKING_BINS; i++)
    {
      gMemoryTracking.sCurrentAllocationNumberArray[i] = 0;
      gMemoryTracking.sMaximumAllocationNumberArray[i] = 0;
      gMemoryTracking.sTotalAllocationNumberArray[i] = 0;
      
      gMemoryTracking.sCurrentAllocationSizeArray[i] = 0;
      gMemoryTracking.sMaximumAllocationSizeArray[i] = 0;
      gMemoryTracking.sTotalAllocationSizeArray[i] = 0;
    }
  }
  
  
  static void MemoryTrackingAdd(size_t size)
  {
    /* Memory tracking code */
    uint32 bin = findBin(size);
    uint32 binsize = 1 << bin;
    uint32 dummy;
    
    /* for breakpoint setting */
#ifdef PSOSIM
    if (bin == 0)
      dummy = 0;
    if (bin == 1)
      dummy = 0;
    if (bin == 2)
      dummy = 0;
    if (bin == 3)
      dummy = 0;
    if (bin == 4)
      dummy = 0;
    if (bin == 5)
      dummy = 0;
    if (bin == 6)
      dummy = 0;
    if (bin == 7)
      dummy = 0;
    if (bin == 8)
      dummy = 0;
    if (bin == 9)
      dummy = 0;
    if (bin == 10)
      dummy = 0;
    if (bin == 11)
      dummy = 0;
    if (bin == 12)
      dummy = 0;
    if (bin == 13)
      dummy = 0;
    if (bin == 14)
      dummy = 0;
    if (bin == 15)
      dummy = 0;
    if (bin == 16)
      dummy = 0;
    if (bin == 17)
      dummy = 0;
    if (bin == 18)
      dummy = 0;
    if (bin == 19)
      dummy = 0;
    if (bin == 20)
      dummy = 0;
    if (bin == 21)
      dummy = 0;
    if (bin >  21)
      dummy = 0;
#endif /* PSOSIM */
      
    gMemoryTracking.sCurrentAllocationSize += size;
    gMemoryTracking.sTotalAllocationSize += size;
    if (gMemoryTracking.sCurrentAllocationSize > gMemoryTracking.sMaximumAllocationSize)
      gMemoryTracking.sMaximumAllocationSize = gMemoryTracking.sCurrentAllocationSize;
      
    gMemoryTracking.sCurrentAllocRealSize += binsize;
    gMemoryTracking.sTotalAllocRealSize += binsize;
    if (gMemoryTracking.sCurrentAllocRealSize > gMemoryTracking.sMaximumAllocRealSize)
      gMemoryTracking.sMaximumAllocRealSize = gMemoryTracking.sCurrentAllocRealSize;
      
    gMemoryTracking.sCurrentAllocationNumber++;
    gMemoryTracking.sTotalAllocationNumber++;
    if (gMemoryTracking.sCurrentAllocationNumber > gMemoryTracking.sMaximumAllocationNumber)
      gMemoryTracking.sMaximumAllocationNumber = gMemoryTracking.sCurrentAllocationNumber;
      
    gMemoryTracking.sCurrentAllocationSizeArray[bin] += size;
    gMemoryTracking.sTotalAllocationSizeArray[bin] += size;
    if (gMemoryTracking.sCurrentAllocationSizeArray[bin] > gMemoryTracking.sMaximumAllocationSizeArray[bin])
      gMemoryTracking.sMaximumAllocationSizeArray[bin] = gMemoryTracking.sCurrentAllocationSizeArray[bin];
      
    gMemoryTracking.sCurrentAllocationNumberArray[bin]++;
    gMemoryTracking.sTotalAllocationNumberArray[bin]++;
    if (gMemoryTracking.sCurrentAllocationNumberArray[bin] > gMemoryTracking.sMaximumAllocationNumberArray[bin])
      gMemoryTracking.sMaximumAllocationNumberArray[bin] = gMemoryTracking.sCurrentAllocationNumberArray[bin];
  }
  
  
  static void MemoryTrackingDelete(unsigned long size)
  {
    /* Memory tracking code */
    uint32 bin = findBin(size);
    uint32 binsize = 1 << bin;
    
    gMemoryTracking.sCurrentAllocationSize -= size;
    gMemoryTracking.sCurrentAllocationNumber--;
    
    gMemoryTracking.sCurrentAllocationSizeArray[bin] -= size;
    gMemoryTracking.sCurrentAllocationNumberArray[bin]--;
    
    gMemoryTracking.sCurrentAllocRealSize -= binsize;
  }
  
  
  static void InitPools(void)
  {
    int i, j;
    for (i = 0; i < NUM_POOL_BINS; i++)
    {
      memoryPool[i].currentNumberOfPools = 0;
      
      for (j = 0; j < NUM_POOL_SLOTS; j++)
      {
        memoryPool[i].poolInfo[j].poolId = 0;
        memoryPool[i].poolInfo[j].pMemory = NULL;
        memoryPool[i].poolInfo[j].size = 0;
      }
    }
  }
  
  
  static void TermPools(void)
  {
    int i, j;
    /* For some reason, deleting the region then freeing the memory causes a failure */
    /* TODO: Figure out why??? */
    for (i = 1; i < NUM_POOL_BINS; i++)
    {
      for (j = 0; j < (int)memoryPool[i].currentNumberOfPools; j++)
      {
        if (memoryPool[i].poolInfo[j].pMemory != NULL)
        {
          unsigned long retval = pt_delete(memoryPool[i].poolInfo[j].poolId);
          PORT_ASSERT(retval == 0);
          
          PORT_ASSERT_GOOD_WRITE_POINTER(memoryPool[i].poolInfo[j].pMemory);
          free(memoryPool[i].poolInfo[j].pMemory);
          
          memoryPool[i].poolInfo[j].poolId = 0;
          memoryPool[i].poolInfo[j].pMemory = NULL;
          memoryPool[i].poolInfo[j].size = 0;
        }
      }
      
      memoryPool[i].currentNumberOfPools = 0;
    }
  }
  
  
#define PARTITION_CONTROL_BLOCK_SIZE 0x400
  
  static BOOL CreatePool(uint32 whichPool, uint32 poolSize)
  {
    static uint32 poolNumber = 0;
    
    void*   pMemory = NULL;
    unsigned long poolId, unused;
    
    uint32 currentNumberOfPools = memoryPool[whichPool].currentNumberOfPools;
    
    PORT_ASSERT((whichPool >= 0) && (whichPool < NUM_POOL_BINS));
    
    if (currentNumberOfPools == NUM_POOL_SLOTS)
      return FALSE;
      
      
    if (whichPool < 2)
    {
      /* Invalid partition size */
      return FALSE;
    }
    else
    {
      char name[5];
      unsigned long retval;
      
      pMemory = malloc(poolSize * (1 << whichPool) + PARTITION_CONTROL_BLOCK_SIZE);
      PORT_ASSERT_GOOD_WRITE_POINTER(pMemory);
      
      /* No memory protection */
      if (pMemory == NULL)
      {
        /* No memory left in system */
        return FALSE;
      }
      
      
      sprintf(name, "DP%02d", poolNumber);
      
      retval = pt_create(name, pMemory, 0, poolSize * (1 << whichPool) + PARTITION_CONTROL_BLOCK_SIZE,
                         1 << whichPool, PT_LOCAL | PT_DEL, &poolId, &unused);
      if (retval != 0)
      {
        /* Unable to create a pSOS partition */
        return FALSE;
      }
    }
    
    memoryPool[whichPool].poolInfo[currentNumberOfPools].poolId = poolId;
    memoryPool[whichPool].poolInfo[currentNumberOfPools].pMemory = pMemory;
    memoryPool[whichPool].poolInfo[currentNumberOfPools].size = poolSize;
    memoryPool[whichPool].currentNumberOfPools++;
    
    poolNumber++;
    
    return TRUE;
  }
  
  static BOOL AddPool(uint32 whichPool, uint32 poolSize)
  {
    if (memoryPool[whichPool].poolInfo[0].pMemory == NULL)
      return FALSE;
      
    return CreatePool(whichPool, poolSize);
  }
  
  static void* AllocateFromPsos(uint32 whichPool, uint32 poolIndex, uint32 size)
  {
    uint32 retval;
    void* pMemory;
    
    PORT_ASSERT(memoryPool[whichPool].poolInfo[poolIndex].poolId);
    
    retval = pt_getbuf(memoryPool[whichPool].poolInfo[poolIndex].poolId, &pMemory);
    
    /* If we got memory, then return */
    if (retval == 0)
    {
      PORT_ASSERT_GOOD_WRITE_POINTER(pMemory);
      *((unsigned long *)pMemory) = (whichPool << 27) + (poolIndex << 24) + size;
      return (unsigned long *)pMemory + 1;
    }
    else
      return NULL;
  }
  
  static void* SearchPoolsForMemory(uint32 whichPool, uint32 size)
  {
    void*   pMemory;
    uint32    poolIndex;
    /* Get memory from main region */
    if (whichPool == 0)
    {
      pMemory = malloc(size);
      
      /* No memory protection */
      if (pMemory == NULL)
      {
        /* No memory left in system */
        return NULL;
      }
      
      PORT_ASSERT_GOOD_WRITE_POINTER(pMemory);
      *((unsigned long *)pMemory) = (whichPool << 27) + size;
      return (unsigned long *)pMemory + 1;
    }
    
    /* Allocate memory from the first available bin (partition) */
    for (poolIndex = 0; poolIndex < memoryPool[whichPool].currentNumberOfPools; poolIndex++)
    {
      pMemory = AllocateFromPsos(whichPool, poolIndex, size);
      if (pMemory != NULL)
        return pMemory;
    }
    
    /* Made it here because we ran out of memory in the pool, so try to add more pools */
    if (AddPool(whichPool, memoryPool[whichPool].poolInfo[0].size >> 1) == FALSE)
    {
      /* All pools of this size have been consumed */
      return NULL;
    }
    
    /* Allocate memory from newly created pool */
    pMemory = AllocateFromPsos(whichPool, memoryPool[whichPool].currentNumberOfPools - 1, size);
    if (pMemory != NULL)
      return pMemory;
      
    /* If we can't allocate from the newly created pool, then we have problems */
    /* No memory protection */
    
    /* No memory left in system */
    return NULL;
  }
  
  void* PortMemBlockAllocateFromPool(uint32 size)
  {
    void*   pMemory = NULL;
    int    poolIndex;
    BOOL foundPool = FALSE;
    uint32 whichPool;
    
    PORT_ASSERT((size & 0xff000000) == 0);
    
    size += 4;
    whichPool = findBin(size); /* Add 4 so I can store info with data */
    MemoryTrackingAdd(size);
    
    /* If pool exists for the size needed, then use it, else find next largest pool */
    for (poolIndex = whichPool; poolIndex < 32; poolIndex++)
      if (memoryPool[poolIndex].poolInfo[0].pMemory != NULL)
      {
        foundPool = TRUE;
        whichPool = poolIndex;
        break;
      }
      
    /* If next largest pool doesn't exist, then use pool 0 (regions) */
    if (!foundPool)
      whichPool = 0;
      
    /* Allocate memory from the first available bin */
    pMemory = SearchPoolsForMemory(whichPool, size);
    PORT_ASSERT_GOOD_WRITE_POINTER(pMemory);
    return pMemory;
  }
  
  void PortMemBlockDeleteFromPool(void* pMemory)
  {
    unsigned long *pRealMemory = (unsigned long *)pMemory - 1;
    
    uint32 whichPool = (*pRealMemory >> 27) & 0x0000001f;
    uint32 whichBin = (*pRealMemory >> 24) & 0x00000007;
    
    PORT_ASSERT_GOOD_WRITE_POINTER(pMemory);
    MemoryTrackingDelete(*pRealMemory & 0x00ffffff);
    
    
    if (whichPool == 0)
    {
      free(pRealMemory);
    }
    else
    {
      uint32 retval = pt_retbuf(memoryPool[whichPool].poolInfo[whichBin].poolId, pRealMemory);
      PORT_ASSERT(retval == 0);
    }
  }
  
  /* PortMemGetPoolSize() : return size of portable memory pool, or 0 if
   * unknown.
   */
  int  PortMemBlockGetPoolSize(void)
  {
    return 0; /* TODO: Find size of pool: 4Mar02 */
  }
  
  /* PortMemBlockSetPoolSize() : set size of portable memory pool on PSOS.
   * This must be called before PortMemoryInit(), which is called by PortInit().
   */
  void PortMemBlockSetPoolSize(size_t sizeInBytes)
  {}
  
  int  PortMemBlockInit(void)
  {
    InitPools();
    CreatePool(findBin(1 <<  3),  3000);
    CreatePool(findBin(1 <<  4), 10000);
    CreatePool(findBin(1 <<  5),  8000);
    CreatePool(findBin(1 <<  6), 16000);
    CreatePool(findBin(1 <<  7),  5000);
    CreatePool(findBin(1 <<  8),  1000);
    CreatePool(findBin(1 <<  9),  2000);
    CreatePool(findBin(1 << 10),    50);
    CreatePool(findBin(1 << 11),    20);
    CreatePool(findBin(1 << 12),    24);
    CreatePool(findBin(1 << 13),    16);
    CreatePool(findBin(1 << 14),    10);
    CreatePool(findBin(1 << 15),    16);
    CreatePool(findBin(1 << 16),     4);
    CreatePool(findBin(1 << 18),     6);
    
    MemoryTrackingInit();
  }
  
  void PortMemBlockTerm(void)
  {
    TermPools();
  }
  
  void PortMemBlockTrackDump(void)
  {
    int i;
    
    printf("\nCurrent Memory Usage = %d\nMaximum Memory Usage = %d\nTotal Memory Allocation = %d\n\n",
           gMemoryTracking.sCurrentAllocationSize, gMemoryTracking.sMaximumAllocationSize, gMemoryTracking.sTotalAllocationSize);
           
    printf("\nCurrent Real Memory Usage = %d\nMaximum Real Memory Usage = %d\nTotal Real Memory Allocation = %d\n\n",
           gMemoryTracking.sCurrentAllocRealSize, gMemoryTracking.sMaximumAllocRealSize, gMemoryTracking.sTotalAllocRealSize);
           
    for (i = 0; i < NUM_TRACKING_BINS; i++)
      printf("Max size of 2^%2d byte objects = %d\n", i, gMemoryTracking.sMaximumAllocationSizeArray[i]);
      
    printf("\nCurrent Memory Objects = %d\nMaximum Memory Objects = %d\nTotal Memory Objects = %d\n\n",
           gMemoryTracking.sCurrentAllocationNumber, gMemoryTracking.sMaximumAllocationNumber, gMemoryTracking.sTotalAllocationNumber);
           
    for (i = 0; i < NUM_TRACKING_BINS; i++)
      printf("Max number for 2^%2d byte objects = %d\n", i, gMemoryTracking.sMaximumAllocationNumberArray[i]);
  }
  
  /* PortMemBlockGetMaxMemUsed() : return the maximum real memory allocated.
   * There is another function of the same name in pmalloc.c, for tracking
   * non-psos block memory.
   */
  int PortMemBlockGetMaxMemUsed(void)
  {
    return gMemoryTracking.sMaximumAllocRealSize;
  }
  
#ifdef __cplusplus
}
#endif

#endif /* PORTABLE_MEM_MGR == PORTABLE_PSOS_BLOCK_SCHEME_MEM_MGR */

