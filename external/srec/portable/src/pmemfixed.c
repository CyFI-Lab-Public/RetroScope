/*---------------------------------------------------------------------------*
 *  pmemfixed.c  *
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
#include "plog.h"

#undef malloc
#undef calloc
#undef realloc
#undef free


#ifdef PORTABLE_FIXED_SIZE_MEM_BLOCK_SCHEME

/*
    How does the Fixed Size Memory Block Manager Work?
    The memory manager manages an unlimited number of pools, each containing a linked list
    of free memory blocks of a fixed size.  The memory pools are ordered in increasing block
    size, eg. pool # 0 contains 4 byte memory blocks, pool # 1 contains 8, etc.  Each memory
    block consists of a header and body.  The header (which is currently 8 bytes long) is used
    to store the address of the next free memory block in the linked list, and to store the
    memory block's pool ID (this is used by the free function to determine which pool the block
    originated from).  The body is simply the usable memory.  Whenever the application requests
    memory of a given size, the memory manager selects the appropriate memory pool which contain
    blocks large enough to satisfy the request.  The memory manager removes a block from the
    linked list and returns the address of the memory block body.  If there are no blocks
    available in the pool, then more blocks are created (if there is memory available); the
    number created is configurable.  If it is not possible to create more blocks, then the
    memory manager searches the  remaining pools in the sequence until it finds a free block or
    it runs out of pools (in this case it will return a null pointer to the calling code).

    How is the memory space allocated to the fixed block pools?
    At start-up the memory manager requests one large memory block from the system (the size is
    defined by #define MEM_SIZE).  This memory is used to a) create the fixed size memory pools
    (each contain the initial number defined in the code) and b) to create extra memory blocks
    each time a particular pool has been exhausted (the number created is configurable for each
    memory pool also).  Once all of this memory has been used up it is also possible to make
    further requests to the system for more memory (to create more fixed memory blocks); this
    feature is switched on using the compilation flag ALLOW_ADDITIONAL_SYS_MEM_ALLOCS.  Note
    that once memory blocks have been added to a memory pool they cannot be removed and reused
    in another, eg a 0.5 MByte memory block could not be removed from its 0.5 Mbyte pool in
    order to create smaller 4 byte blocks in the 4byte block pool.

    How is the large memory block from the system allocated?
    It can be allocated in one of three ways depending on compile time definitions.  If you define
    STATIC_MEMORY_POOL, it's allocated as a static array.  If you define RTXC_PARTITION_MEMORY,
    it's allocated from the HEAP_MAP memory partition.  If you don't define anything, it's allocated
    using the system malloc call.  Of course, RTXC_PARTITION should only be defined on when you building
    for the RTXC operating system.

    If STATIC_MEMORY_POOL or RTXC_PARTITION is defined, you cannot define ALLOW_ADDITIONAL_SYS_MEM_ALLOCS.

    Key Points:
    1. Configurable memory block sizes (not restricted to power of 2 sizes).
    2. Best fit algorith.
    3. Dynamically increases the pool sizes (from an initial number).
    4. Can limit the total heap size.
    5. Configurable initial pool sizes.
    6. Allow additional system memory allocations in order to increase the pool sizes when the
       'heap' limit has been reached.
    7. Doesn't support block consolidation, and reuse across pools.

*/

/*************************** Header Files *************************************/

#ifdef RTXC_PARTITION_MEMORY
#include <rtxcapi.h>
/* TODO - When rtxcgen is run, it will create this header file that should contain
 * identifiers for various memory partitions that we will be using.  For now, in order
 * to get a compile, define a partition identifier.
 */
#define HEAP_MAP 1

#endif

#ifdef __cplusplus
extern "C"
{
#endif



  /*************************** Macros Definitions *******************************/
  /* All of these should be defined on the command line
   * #define MEM_MGR_STATS
   * #define ALLOW_ADDITIONAL_SYS_MEM_ALLOCS
   * #define ALLOW_POOL_GROWTHS
   * #define MEM_SIZE
   */
  
  /*
  #if (defined(STATIC_MEMORY_POOL) || defined(RTXC_PARTITION_MEMORY)) && defined(ALLOW_ADDITIONAL_SYS_MEM_ALLOCS)
  #error Can't allocate additional memory blocks from the system.
  #endif
  */
  /* TODO: Need to figure out a good size for this. */
  /* This had better be the same as the block in HEAP_MAP as defined when building RTXC. */
  
#ifndef MEM_SIZE
  /* If not defined on the command line, use a default value of 10 megabytes for the heap. */
#define MEM_SIZE       (10 * 1024 * 1024)      /* 10 MBytes */
#endif
  
#define MEM_BLOCK_HDR           8   /* (bytes): 16 bit Pool ID, 16 bit signature, 32 bit next block pointer */
#define MEM_POOL_ID_OFFSET      0
#define NEXT_BLOCK_PTR_OFFSET   1   /* (no. of 4 byte words) */
#define MEM_BLOCK_HDR_OFFSET    2   /* (no. of 4 byte words) */
  
#define MEM_POOL_ID_MASK        0x000000FF
#define MEM_REQ_SIZE_MASK       0xFFFFFF00
#define MEM_REQ_SIZE_BIT_SHIFT  8
  
  
  
  /*************************** Type Defs ****************************************/
  
  typedef struct
  {
    unsigned int accReqSize;
    unsigned int maxReqSize;
    unsigned int allocated;
    unsigned int accAllocated;
    unsigned int accFreed;
    unsigned int max_alloc;
  }
  MEM_STATS;
  
  
  
  /*************************** Global Variables *********************************/
  
  int memPoolsInitialised = 0;
  
  unsigned int memBlockSize[]    = {     4,    8,    12,    16,    20,    24,    28,    32,    36,    40,    44,    48,    52,    56,    60,    64,  128,   256,   512,  1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288 };
  /*unsigned int memBlockNum[]     = {   400, 1600, 17000,  8192, 13440,   512,   384,  4352,   900,  7000,   256,  2048,  1024,   128,   128,   256, 6000,  2500,   380,   170,   85,   40,   30,   120,    40,     2,      1,      2,      2 };*/
  unsigned int memBlockNum[]     = {     0,    0,     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,    0,     0,     0,     0,    0,    0,    0,     0,     0,     0,      0,      0,      0 };
  unsigned int memBlkGrowthNum[] = {   128,  128,   128,   128,   128,   128,   128,   128,   128,   128,   128,   128,   128,   128,   128,   128,  128,   128,     1,     1,    1,    1,    1,     1,     1,     1,      1,      1,      1 };
  
#define NUM_OF_POOLS        (sizeof( memBlockSize ) / sizeof( unsigned int ))
  
  static unsigned int memBlkGrowths[NUM_OF_POOLS];
  
#ifdef STATIC_MEMORY_POOL
  static char pHeap[MEM_SIZE];
#else
  static char *pHeap;
#endif
  static char* pReservedHeapMem;
  static char* pMemPools[NUM_OF_POOLS];
  
  static unsigned int initialHeapSize = MEM_SIZE;
  static unsigned int usedHeapSize = 0;
  static unsigned int reservedHeapSize = 0;
  static unsigned int totalSystemAllocMem = 0;
  static unsigned int numOfSystemAllocs = 0;
  
  static MEM_STATS memStats[NUM_OF_POOLS];
  static unsigned int allocatedMem = 0;
  static unsigned int maxAllocMem = 0;
  
  
  
  /*************************** Function Prototypes ******************************/
  
  void initAllMemPools(void);
  char* initMemPool(int poolId, int memBlockSize, int numOfMemBlocks, char** startAddress);
  void* myMalloc(size_t size);
  void myFree(void* ptr);
  void displayMemStats(void);
  void increaseMemPoolSize(unsigned int poolId);
  
  
  
  /*************************** Function Definitions *****************************/
  
  /*******************************************************************************
   *
   * Function:    PortMallocInit
   *
   * Args:        void
   *
   * Returns:     void
   *
   * Description: API function which initialises the fixed size memory pools.  Can
   *              be called multiple times in a session, but is only effective the
   *              first time it is called.
   *
   *******************************************************************************/
  
  void PortMallocInit(void)
  {
    if (0 == memPoolsInitialised)
    {
      initAllMemPools();
    }
  }
  
  
  
  
  
  int PortMallocGetMaxMemUsed(void)
  {
    return (int)maxAllocMem;
  }
  
  
  
  /*******************************************************************************
   *
   * Function:    PortMallocSetPoolSize
   *
   * Args:        Pool size (size_t)
   *
   * Returns:     void
   *
   * Description: API function used to set the initial heap size. Note this can be
   *              called at any time, but is only effective if the memory manager
   *              has not already been initialised.
   *
   *******************************************************************************/
  
  void PortMallocSetPoolSize(size_t size)
  {
#if !defined(STATIC_MEMORY_POOL) && !defined(RTXC_PARTITION_MEMORY)
    if (!memPoolsInitialised)
    {
      initialHeapSize = (unsigned int)size;
    }
#else
    (void)size;
#endif
  }
  
  
  
  /*******************************************************************************
   *
   * Function:    PortMallocGetPoolSize
   *
   * Args:        void
   *
   * Returns:     Pool Size (int)
   *
   * Description: API function to return the initial heap size.
   *
   *******************************************************************************/
  
  int PortMallocGetPoolSize(void)
  {
    return (int)initialHeapSize;
  }
  
  
  
  /*******************************************************************************
   *
   * Function:    initAllMemPools
   *
   * Args:        void
   *
   * Returns:     void
   *
   * Description: Internal function which is used to initialise all of the
   *              memory pools. Note it can be called many times but is only
   *              effective the first time it is called.
   *
   *******************************************************************************/
  
  void initAllMemPools(void)
  {
    char *availableMemStartAddress;
    
    if (0 == memPoolsInitialised)
    {
      int ii;
      
      /* Calculate the required heap size */
      for (ii = 0; ii < NUM_OF_POOLS; ii++)
      {
        usedHeapSize += (memBlockSize[ii] + MEM_BLOCK_HDR) * memBlockNum[ii];
      }
      
      if (initialHeapSize < usedHeapSize)
      {
        /* Insuffucient heap memory; abort initialisation */
        return;
      }
      
      
#if defined(STATIC_MEMORY_POOL)
      /* pHead has already been allocated, statically.  Don't need to do anything. */
#elif defined(RTXC_PARTITION_MEMORY)
      /* Grab the one and only block in HEAP_MAP. */
      pHeap = KS_alloc(HEAP_MAP);
      /* MEM_SIZE has better equal the size of HEAP_MAP's block. */
      PORT_ASSERT(MEM_SIZE == KS_inqmap(HEAP_MAP));
#else
      /* Use the system malloc for heap allocation. */
      
      pHeap = (char*)malloc(initialHeapSize);
#endif
      if (0 == pHeap)
      {
        /* Unable to get memory for heap; abort initialisation */
        return;
      }
      
      totalSystemAllocMem = initialHeapSize;
      numOfSystemAllocs++;
      reservedHeapSize = initialHeapSize - usedHeapSize;
      
      /* Initialise each memory pool */
      availableMemStartAddress = pHeap;
      
      for (ii = 0; ii < NUM_OF_POOLS; ii++)
      {
        pMemPools[ii] = 0;
        
        if (0 != memBlockNum[ii])
        {
          pMemPools[ii] = initMemPool(ii, memBlockSize[ii] + MEM_BLOCK_HDR, memBlockNum[ii], &availableMemStartAddress);
        }
      }
      
      pReservedHeapMem = availableMemStartAddress;
      
      memPoolsInitialised = 1;
    }
  }
  
  
  
  /*******************************************************************************
   *
   * Function:    initMemPool
   *
   * Args:        Pool ID (int), Memory Block Size (int), Number of Memory Blocks
   *              (int), Heap Memory Start Address (char**)
   *
   * Returns:     Memory Pool Start Address (char*)
   *
   * Description: Internal function used to fill a specified memory pool with a
   *              specified number of memory blocks of a specified size.  The heap
   *              memory start address is adjusted to point to the next available
   *              memory following the newly created pool.
   *
   *******************************************************************************/
  
  char* initMemPool(int poolId, int memBlockSize, int numOfMemBlocks, char** startAddress)
  {
    char* pPrevMemBlock = 0;
    char* pCurrMemBlock = 0;
    char* pStartMemPool = 0;
    int ii;
    
    for (ii = 0; ii < numOfMemBlocks; ii++)
    {
      pCurrMemBlock = &((*startAddress)[ii*memBlockSize]);
      
      *((unsigned int*)pCurrMemBlock) = poolId;
      
      if (0 != pPrevMemBlock)
      {
        ((unsigned int*)pPrevMemBlock)[NEXT_BLOCK_PTR_OFFSET] = (unsigned int)pCurrMemBlock;
      }
      
      pPrevMemBlock = pCurrMemBlock;
    }
    
    ((unsigned int*)pPrevMemBlock)[NEXT_BLOCK_PTR_OFFSET] = 0;
    
    pStartMemPool = *startAddress;
    
    *startAddress = (*startAddress) + (ii * memBlockSize);
    
    return pStartMemPool;
  }
  
  
  
  /*******************************************************************************
   *
   * Function:    PortMalloc
   *
   * Args:        Size (size_t)
   *
   * Returns:     Pointer to memory block (void*)
   *
   * Description: API function which is used by the application to request memory.
   *              A null pointer is returned if the memory manager is unable to
   *              satisfy the request.
   *
   *******************************************************************************/
  
  void* PortMalloc(size_t size)
  {
    int poolId;
    char *pMemBlock;
    int ii;
    
    /* Make sure the memory manager has been initialised */
    if (0 == memPoolsInitialised)
    {
      initAllMemPools();
    }
    
    poolId = NUM_OF_POOLS;
    pMemBlock = 0;
    
    /* Find the best fit memory block */
    for (ii = 0; ii < NUM_OF_POOLS; ii++)
    {
      if (memBlockSize[ii] >= size)
      {
        poolId = ii;
        
        break;
      }
    }
    
    /* Ensure that the requested size is not larger than the largest block size */
    if (NUM_OF_POOLS > poolId)
    {
      /* Search the selected memory pool for a memory block; if there are none
         then try to create some more blocks.  If this is not possible then
         search the next largest memory block pool.  Repeat until either a block
         is found, or there are no pools left */
      for (ii = poolId; ii < NUM_OF_POOLS; ii++)
      {
#ifdef ALLOW_POOL_GROWTHS
        /* If there are no blocks left, try to create some more */
        if (0 == pMemPools[ii])
        {
          increaseMemPoolSize(ii);
        }
#endif /* ALLOW_POOL_GROWTHS */
        
        if (0 != pMemPools[ii])
        {
          /* Remove the memory block from the pool linked-list */
          pMemBlock = pMemPools[ii];
          
          pMemPools[ii] = (char*)(((unsigned int*)pMemBlock)[NEXT_BLOCK_PTR_OFFSET]);
          
#ifdef MEM_MGR_STATS
          /* Record the requested size in the memory block header - this is used
             by PortFree to determine how much requested memory has been free'd */
          *((unsigned int*)pMemBlock) = ii | (size << MEM_REQ_SIZE_BIT_SHIFT);
#endif /* MEM_MGR_STATS */
          
          /* Adjust the memory block pointer to point to the useful portion of the
             memory block, ie beyond the header */
          pMemBlock = pMemBlock + MEM_BLOCK_HDR;
          
#ifdef MEM_MGR_STATS
          /* Update the memory statistics */
          allocatedMem += size;
          
          if (allocatedMem > maxAllocMem)
          {
            maxAllocMem = allocatedMem;
          }
          
          memStats[ii].accReqSize += size;
          memStats[ii].accAllocated++;
          memStats[ii].allocated++;
          
          if (memStats[ii].maxReqSize < size)
          {
            memStats[ii].maxReqSize = size;
          }
          
          if (memStats[ii].allocated > memStats[ii].max_alloc)
          {
            memStats[ii].max_alloc = memStats[ii].allocated;
          }
#endif /* MEM_MGR_STATS */
          break;
        }
      }
    }
    
    return (void*)pMemBlock;
  }
  
  
#ifdef ALLOW_POOL_GROWTHS
  /*******************************************************************************
   *
   * Function:    increaseMemPoolSize
   *
   * Args:        Pool ID (unsigned int)
   *
   * Returns:     void
   *
   * Description: Increases the number of blocks in a given pool by the number
   *              specified in the array memBlkGrowthNum if there is memory
   *              available.  Memory is allocated from the heap reserve if
   *              availabe, else it is requested from the system (if the
   *              compilation flag ALLOW_ADDITIONAL_SYS_MEM_ALLOCS is defined. If
   *              there is insufficient memory then the operation is aborted
   *              without notification to the calling code.
   *
   *******************************************************************************/
  
  void increaseMemPoolSize(unsigned int poolId)
  {
    unsigned int requiredMemSize = memBlkGrowthNum[poolId] * (memBlockSize[poolId] + MEM_BLOCK_HDR);
    
    /* See if there is enough heap reserve memory */
    if (requiredMemSize <= reservedHeapSize)
    {
      /* We're in luck; there's enough space */
      pMemPools[poolId] = initMemPool(poolId, memBlockSize[poolId] + MEM_BLOCK_HDR, memBlkGrowthNum[poolId], &pReservedHeapMem);
      
      memBlockNum[poolId] += memBlkGrowthNum[poolId];
      
      reservedHeapSize -= requiredMemSize;
      usedHeapSize += requiredMemSize;
      
#ifdef MEM_MGR_STATS
      memBlkGrowths[poolId]++;
#endif /* MEM_MGR_STATS */
    }
#ifdef ALLOW_ADDITIONAL_SYS_MEM_ALLOCS
    else
    {
      /* There's not enough memory in the heap reserve, so request it from the system */
      char* pStartAddress = (char*)malloc(requiredMemSize);
      
      if (0 != pStartAddress)
      {
        /* The system has allocated some memory, so let's make some more blocks */
        pMemPools[poolId] = initMemPool(poolId, memBlockSize[poolId] + MEM_BLOCK_HDR, memBlkGrowthNum[poolId], &pStartAddress);
        
        memBlockNum[poolId] += memBlkGrowthNum[poolId];
        
        totalSystemAllocMem += requiredMemSize;
        numOfSystemAllocs++;
        
#ifdef MEM_MGR_STATS
        memBlkGrowths[poolId]++;
#endif /* MEM_MGR_STATS */
      }
    }
#endif /* ALLOW_ADDITIONAL_SYS_MEM_ALLOCS */
  }
#endif /* ALLOW_POOL_GROWTHS */
  
  
  
  /*******************************************************************************
   *
   * Function:    PortFree
   *
   * Args:        Memory Block Pointer (void*)
   *
   * Returns:     void
   *
   * Description: API function used by the application code to return a memory
   *              block to the appropriate pool.  Note that this function is not
   *              able to handle null or stale memory block pointers; calling this
   *              function under these conditions will result in unpredictable
   *              behavior.
   *
   *******************************************************************************/
  
  void PortFree(void* pMem)
  {
    unsigned int tmpVal;
    unsigned char poolId;
    char* pCurrentHead;
#ifdef MEM_MGR_STATS
    unsigned int reqMemSize;
#endif
    
    /* What is the memory block pool id ? */
    tmpVal = ((unsigned int*)pMem)[-MEM_BLOCK_HDR_OFFSET+MEM_POOL_ID_OFFSET];
    poolId = tmpVal & MEM_POOL_ID_MASK;
    
    /* Add the memory block to the appropriate pool */
    pCurrentHead = pMemPools[poolId];
    ((unsigned int*)pMem)[-MEM_BLOCK_HDR_OFFSET+NEXT_BLOCK_PTR_OFFSET] = (unsigned int)pCurrentHead;
    pMemPools[poolId] = (char*) & (((unsigned int*)pMem)[-MEM_BLOCK_HDR_OFFSET]);
    
#ifdef MEM_MGR_STATS
    /* What was the requested memory size ? */
    reqMemSize = tmpVal >> MEM_REQ_SIZE_BIT_SHIFT;
    
    allocatedMem -= reqMemSize;
    
    PORT_ASSERT(allocatedMem >= 0);
    
    memStats[poolId].accFreed++;
    memStats[poolId].allocated--;
#endif /* MEM_MGR_STATS */
  }
  
  
  
  /*******************************************************************************
   *
   * Function:    displayMemStats
   *
   * Args:        void
   *
   * Returns:     void
   *
   * Description: API function used to display the overall memory and individual
   *              memory pool statistics to standard output.
   *
   *******************************************************************************/
  
  void displayMemStats(void)
  {
    unsigned int totBNum = 0;
    unsigned int totGrowths = 0;
    unsigned int totAlloc = 0;
    unsigned int totAccAlloc = 0;
    unsigned int totAccFreed = 0;
    unsigned int totMaxAlloc = 0;
    unsigned int totMemWithOH = 0;
    unsigned int totMem = 0;
    unsigned int bytesAllocWithOH = 0;
    unsigned int bytesAlloc = 0;
    unsigned int maxBytesAllocWithOH = 0;
    unsigned int maxBytesAlloc = 0;
    unsigned int ii;
    
    printf("\nPool ID   BlkSz AvReqSz MaxReqSz   NumBlk  Growths    Alloc AccAlloc AccFreed MaxAlloc Alloc(b)  MaxA(b)\n");
    printf("--------------------------------------------------------------------------------------------------------\n");
    
    for (ii = 0; ii < NUM_OF_POOLS; ii++)
    {
      unsigned int avReqSize = 0;
      
      if (0 != memStats[ii].accAllocated)
      {
        avReqSize = memStats[ii].accReqSize / memStats[ii].accAllocated;
      }
      
      printf("   %4i  %6i  %6i   %6i  %7i  %7i  %7i  %7i  %7i  %7i %8i %8i\n", ii, memBlockSize[ii], avReqSize, memStats[ii].maxReqSize, memBlockNum[ii], memBlkGrowths[ii], memStats[ii].allocated, memStats[ii].accAllocated, memStats[ii].accFreed, memStats[ii].max_alloc, (memBlockSize[ii]*memStats[ii].allocated), (memBlockSize[ii]*memStats[ii].max_alloc));
      
      totBNum += memBlockNum[ii];
      totGrowths += memBlkGrowths[ii];
      totAlloc += memStats[ii].allocated;
      totAccAlloc += memStats[ii].accAllocated;
      totAccFreed += memStats[ii].accFreed;
      totMaxAlloc += memStats[ii].max_alloc;
      
      totMemWithOH += (memBlockSize[ii] + MEM_BLOCK_HDR) * memBlockNum[ii];
      totMem += memBlockSize[ii] * memBlockNum[ii];
      bytesAllocWithOH += memStats[ii].allocated * (memBlockSize[ii] + MEM_BLOCK_HDR);
      bytesAlloc += memStats[ii].allocated * memBlockSize[ii];
      maxBytesAllocWithOH += memStats[ii].max_alloc * (memBlockSize[ii] + MEM_BLOCK_HDR);
      maxBytesAlloc += memStats[ii].max_alloc * memBlockSize[ii];
    }
    
    printf("--------------------------------------------------------------------------------------------------------\n");
    printf("Total                             %7i  %7i  %7i  %7i  %7i  %7i %8i %8i\n\n", totBNum, totGrowths, totAlloc, totAccAlloc, totAccFreed, totMaxAlloc, bytesAlloc, maxBytesAlloc);
    printf("Total Memory     %9i bytes\n", totMemWithOH);
    printf("Total Memory     %9i bytes (without overhead)\n", totMem);
    printf("Allocated Memory %9i bytes\n", bytesAllocWithOH);
    printf("Allocated Memory %9i bytes (without overhead)\n", bytesAlloc);
    printf("Max Alloc Memory %9i bytes\n", maxBytesAllocWithOH);
    printf("Max Alloc Memory %9i bytes (without overhead)\n", maxBytesAlloc);
    printf("\nReq Alloc Memory %9i bytes\n", allocatedMem);
    printf("Max Rq Alloc Mem %9i bytes\n\n", maxAllocMem);
    
    printf("Used Heap Size   %9i bytes\n", usedHeapSize);
    printf("Reserved Heap    %9i bytes\n", reservedHeapSize);
    printf("Total Sys Alloc  %9i bytes\n", totalSystemAllocMem);
    printf("Num of Sys Alloc %9i\n", numOfSystemAllocs);
    
    printf("\n");
  }
  
  
  
#ifdef __cplusplus
} /* end extern "C" */
#endif


#endif /* FIXED_SIZE_MEM_BLOCK_SCHEME */
