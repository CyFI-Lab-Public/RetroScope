/*---------------------------------------------------------------------------*
 *  pmemory.c  *
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




#include "passert.h"
#include "pcrc.h"
#include "pmemory.h"
#include "PFileSystem.h"
#include "PStackTrace.h"
#include "passert.h"
#include "pmemory_ext.h"
#include "pmutex.h"

#ifndef USE_STDLIB_MALLOC

#undef malloc
#undef calloc
#undef realloc
#undef free

static unsigned int gNbInit = 0;
static PFile* gFile = NULL;
static ESR_BOOL isLogEnabled = ESR_TRUE;

#ifdef PMEM_MAP_TRACE
static asr_uint32_t gMaxAlloc = -1;
static asr_uint32_t gCurAlloc = -1;
#endif

#if defined(PORTABLE_DINKUM_MEM_MGR) || defined(PORTABLE_FIXED_SIZE_MEM_BLOCK_SCHEME)
static size_t   gMemPoolSize = (3*1024*1024); /* default value: 3M */
#endif

#ifdef USE_THREAD
static MUTEX memMutex;
#endif

#define MAX_MEM_TAG 256

/* Only PMEM_MAP_TRACE has been defined, could do other memory logging/debugging */
#ifdef PMEM_MAP_TRACE

#define PMEM_STACKTRACE 0
/* If enabled, logs individual memory allocation, reallocation, free operations */
#define PMEM_LOG_LOWLEVEL 0
#elif defined(WIN32)
#pragma message("No PMEM_MAP_TRACE")
#endif

typedef struct MemoryData_t
{
#ifdef PMEM_MAP_TRACE
  int index;
#endif
  size_t size;
#if PMEM_STACKTRACE
  /**
   * Stacktrace of where the memory was allocated from.
   */
  const LCHAR* stackTrace;
  /**
   * Pointer to next memory allocation associated with the same tag.
   */
  struct MemoryData_t* next;
  /**
   * Pointer to last memory allocation associated with the same tag.
   */
  struct MemoryData_t* last;
#endif
}
MemoryData;

#ifdef PMEM_MAP_TRACE
typedef struct MemMapEntry_t
{
  /**
   * Memory tag/ID associated with allocation.
   */
  const LCHAR* tag;
  asr_uint32_t curAlloc;
  asr_uint32_t maxAlloc;
  unsigned int crc;
  /**
   * First memory allocation associated with this tag.
   * Memory that has been deallocated will not show up on this list.
   */
  MemoryData* first;
  /**
   * Last memory allocation associated with this tag.
   * Memory that has been deallocated will not show up on this list.
   */
  MemoryData* last;
}
MemMapEntry;

static MemMapEntry gMemoryMap[MAX_MEM_TAG];
#endif

#if defined(PORTABLE_DINKUM_MEM_MGR) || defined(PORTABLE_FIXED_SIZE_MEM_BLOCK_SCHEME)
extern ESR_ReturnCode memory_pool_creation_status; /* Verify that memory pool actually was created */
#define malloc PortNew
#define free PortDelete
#endif


#if PMEM_STACKTRACE
static ESR_ReturnCode getStackTrace(LCHAR* stackTrace, size_t* len)
{
  ESR_BOOL isInit;
  ESR_ReturnCode rc;

  rc = PStackTraceIsInitialized(&isInit);
  if (rc == ESR_SUCCESS && isInit)
  {
    LCHAR* index;
    size_t bufferLen = *len;
    size_t i;

    rc = PStackTraceGetValue(stackTrace, &bufferLen);
    if (rc == ESR_SUCCESS)
    {
      for (i = 0; i < 2; ++i)
      {
        rc = PStackTracePopLevel(stackTrace);
        if (rc != ESR_SUCCESS)
        {
          pfprintf(PSTDERR, "[%s:%d] PStackTracePopLevel failed with %s\n", __FILE__, __LINE__, ESR_rc2str(rc));
          goto CLEANUP;
        }
      }
      index = stackTrace;
      while (index)
      {
        index = LSTRSTR(index, L(" at\n"));
        if (index != NULL)
          *(index + 3) = L(' ');
      }
    }
    else if (rc == ESR_NOT_SUPPORTED)
      LSTRCPY(stackTrace, L(""));
    else if (rc != ESR_SUCCESS)
    {
      pfprintf(PSTDERR, "[%s:%d] PStackTraceGetValue failed with %s\n", __FILE__, __LINE__, ESR_rc2str(rc));
      goto CLEANUP;
    }
  }
  else
    LSTRCPY(stackTrace, L("(null)"));
  *len = LSTRLEN(stackTrace);
  return ESR_SUCCESS;
CLEANUP:
  return rc;
}
#endif /* PMEM_STACKTRACE */

#ifdef PMEM_MAP_TRACE
static int getIndex(const LCHAR *key)
{
  unsigned int crc = ~pcrcComputeString(key);
  int initialIdx = (int)(crc % MAX_MEM_TAG);
  int idx = initialIdx;

  for (;;)
  {
    if (gMemoryMap[idx].tag == NULL)
    {
      /* found an empty slot, use it. */
      gMemoryMap[idx].tag = key;
      gMemoryMap[idx].curAlloc = 0;
      gMemoryMap[idx].maxAlloc = 0;
      gMemoryMap[idx].crc = crc;
      gMemoryMap[idx].first = NULL;
      gMemoryMap[idx].last = NULL;
#if PMEM_LOG_LOWLEVEL
      if (gFile != NULL)
        pfprintf(gFile, L("pmem|newtag|%s|%d|\n"), key, idx);
#endif
      return idx;
    }

    if (gMemoryMap[idx].crc == crc &&
        LSTRCMP(gMemoryMap[idx].tag, key) == 0)
    {
      /* found a matching slot, return it */
      return idx;
    }

    if (++idx == MAX_MEM_TAG)
    {
      /* Look at next slot and wrap around. */
      idx = 0;
    }
    if (idx == initialIdx)
      return -1;
  }
}
#endif

/* Not thread-safe. But do not expect user calls this function on different threads simultaneously */
ESR_ReturnCode PMemorySetPoolSize(size_t size)
{
#if defined(PORTABLE_DINKUM_MEM_MGR) || defined(PORTABLE_FIXED_SIZE_MEM_BLOCK_SCHEME)
  if (gNbInit > 0)
    return ESR_INVALID_STATE;

  gMemPoolSize = size;
  return ESR_SUCCESS;
#else
  return ESR_NOT_SUPPORTED;
#endif
}

ESR_ReturnCode PMemoryGetPoolSize(size_t *size)
{
#if defined(PORTABLE_DINKUM_MEM_MGR) || defined(PORTABLE_FIXED_SIZE_MEM_BLOCK_SCHEME)
  *size = gMemPoolSize;
  return ESR_SUCCESS;
#else
  return ESR_NOT_SUPPORTED;
#endif
}

/* it is not thread safe: hard to protect the createMutex()
  * could fix it by using static mutex initialization in some OS,
  * but does not work with our own pthread implementation for vxworks
  * SUPPOSE the user just calls this function once
  */
ESR_ReturnCode PMemInit(void)
{
  ESR_ReturnCode init_status;

  if (gNbInit > 0)
    return ESR_INVALID_STATE;

  init_status = createMutex(&memMutex, ESR_FALSE);

  if (init_status == ESR_SUCCESS)
  {
    ++gNbInit;
#ifdef PMEM_MAP_TRACE
    memset(gMemoryMap, 0, sizeof(gMemoryMap));
#endif
#if defined(PORTABLE_DINKUM_MEM_MGR) || defined(PORTABLE_FIXED_SIZE_MEM_BLOCK_SCHEME)
    PortMemSetPoolSize(gMemPoolSize);
    PortMemoryInit();
    /* There is no friggin' way to pass the status of the memory initialization, because of the damn macros and all the other crap */
    /* So I am checking the value of an external variable, this sucks, but I can't ignore something this important */
    if (memory_pool_creation_status == ESR_SUCCESS)
    {
      /* Reset this because with all the layers of crap, I can't guarantee we'll get to the bottom layer on a re-init */
      memory_pool_creation_status = ESR_FATAL_ERROR;
    }
    else
    {
      pfprintf(PSTDERR, L("ESR_INVALID_STATE: Memory Pool Could Not Be Created\n"));
      PortMemoryTerm();
      unlockMutex(&memMutex);
      deleteMutex(&memMutex);
      init_status = ESR_INVALID_STATE;
    }
#endif
  }
  else
  {
    deleteMutex(&memMutex);
  }

#ifdef PMEM_MAP_TRACE
  // Initialize global static variables
  gCurAlloc = 0;
  gMaxAlloc = 0;
#endif

  return (init_status);
}

/* it is not thread safe: hard to protect the deleteMutex()
  * could fix it by using static mutex initialization in some OS,
  * but does not work with our own pthread implementation for vxworks
  * SUPPOSE the user just calls this function once
  */
ESR_ReturnCode PMemShutdown(void)
{
#ifdef PMEM_MAP_TRACE
  size_t i;
#endif

  if (gNbInit == 0)
    return ESR_INVALID_STATE;
  if (gNbInit == 1)
  {
#ifdef PMEM_MAP_TRACE
    for (i = 0; i < MAX_MEM_TAG; ++i)
    {
      free((LCHAR*) gMemoryMap[i].tag);
      gMemoryMap[i].tag = NULL;
    }
#endif
#if defined(PORTABLE_DINKUM_MEM_MGR) || defined(PORTABLE_FIXED_SIZE_MEM_BLOCK_SCHEME)
    PortMemoryTerm();
#endif
    deleteMutex(&memMutex);
  }
  gNbInit--;

  return ESR_SUCCESS;
}

ESR_ReturnCode PMemSetLogFile(PFile* file)
{
  if (gNbInit == 0)
    return ESR_INVALID_STATE;

  lockMutex(&memMutex);
  gFile = file;
  unlockMutex(&memMutex);

  return ESR_SUCCESS;
}

ESR_ReturnCode PMemDumpLogFile(void)
{
  ESR_ReturnCode rc;

  if (gNbInit == 0)
    return ESR_INVALID_STATE;

  lockMutex(&memMutex);
  if (gFile != NULL)
  {
    /* Hide gFile from memory report */
/*    CHK(rc, gFile->hideMemoryAllocation(gFile));*/

    rc = PMemReport(gFile);
    if (rc != ESR_SUCCESS)
    {
      pfprintf(PSTDERR, L("%s: PMemDumpLogFile() at %s:%d"), ESR_rc2str(rc), __FILE__, __LINE__);
      goto CLEANUP;
    }
    if (gFile != PSTDIN && gFile != PSTDOUT && gFile != PSTDERR)
    {
/*      rc = gFile->destroy(gFile);
      if (rc != ESR_SUCCESS)
      {
        pfprintf(PSTDERR, L("%s: PMemDumpLogFile() at %s:%d"), ESR_rc2str(rc), __FILE__, __LINE__);
        goto CLEANUP;
      }*/
      pfclose ( gFile );
    }
    gFile = NULL;
  }
  unlockMutex(&memMutex);
  return ESR_SUCCESS;
CLEANUP:
  unlockMutex(&memMutex);
  return rc;
}

ESR_ReturnCode PMemSetLogEnabled(ESR_BOOL value)
{
  lockMutex(&memMutex);
  isLogEnabled = value;
  unlockMutex(&memMutex);

  return ESR_SUCCESS;
}

ESR_ReturnCode PMemLogFree(void* ptr)
{
  MemoryData* data;
#ifdef PMEM_MAP_TRACE
  MemMapEntry* e;
#endif
#if PMEM_STACKTRACE && PMEM_LOG_LOWLEVEL
  ESR_ReturnCode rc;
#endif

  if (ptr == NULL || gNbInit == 0)
    return ESR_SUCCESS;

  lockMutex(&memMutex);

  data = (MemoryData*)(((unsigned char*) ptr) - sizeof(MemoryData));
#ifdef PMEM_MAP_TRACE
  e = gMemoryMap + data->index;
  passert(data->index >= 0 && data->index <= MAX_MEM_TAG);
  if (isLogEnabled)
  {
    passert(e->curAlloc >= data->size);
    e->curAlloc -= data->size;

    passert(gCurAlloc >= data->size);
    gCurAlloc -= data->size;

    data->size = 0;
  }
#if PMEM_STACKTRACE
  if (e->first != NULL && e->first == data)
    e->first = data->next;
  if (e->last != NULL && e->last == data)
    e->last = data->last;
  if (data->last != NULL)
    data->last->next = data->next;
  if (data->next != NULL)
  {
    data->next->last = data->last;
    data->next = NULL;
  }
  data->last = NULL;
#endif
#if PMEM_LOG_LOWLEVEL
  if (gFile != NULL && isLogEnabled)
  {
#if PMEM_STACKTRACE
    LCHAR stackTrace[P_MAX_STACKTRACE];
    size_t len = P_MAX_STACKTRACE;

    rc = getStackTrace(stackTrace, &len);
    if (rc != ESR_SUCCESS)
    {
      pfprintf(PSTDERR, "[%s:%d] getStackTrace failed with %s\n", __FILE__, __LINE__, ESR_rc2str(rc));
      goto CLEANUP;
    }
    pfprintf(gFile, L("pmem|free|%s|%s|%d|0x%x|%s|\n"), e->tag, data->stackTrace, data->size, ptr, stackTrace);
#else
    pfprintf(gFile, L("pmem|free|%s|%d|0x%x\n"), e->tag, data->size, ptr);
#endif /* PMEM_STACKTRACE */
  }
#endif /* PMEM_LOG_LOWLEVEL */
#endif /* PMEM_MAP_TRACE */

  unlockMutex(&memMutex);
  return ESR_SUCCESS;
#if PMEM_STACKTRACE && PMEM_LOG_LOWLEVEL
CLEANUP:
  unlockMutex(&memMutex);
  return rc;
#endif
}

ESR_ReturnCode PMemReport(PFile* file)
{
#define TAG_SIZE 52
#ifdef PMEM_MAP_TRACE
  asr_uint32_t totalAlloc = 0;
  size_t i;
  MemMapEntry* e;
  unsigned int crc;
  LCHAR truncatedTag[TAG_SIZE];
  size_t len;
  LCHAR TAG_PREFIX[] = L("...");
  const size_t TAG_PREFIX_SIZE = LSTRLEN(TAG_PREFIX);
  const size_t countToCopy = (TAG_SIZE - 1) - TAG_PREFIX_SIZE;
#endif
#if PMEM_STACKTRACE
  MemoryData* data;
#endif

  if (gNbInit == 0)
    return ESR_INVALID_STATE;
  if (file == NULL)
  {
    file = gFile;
    if (file == NULL)
      return ESR_SUCCESS;
  }

  lockMutex(&memMutex);
#ifdef PMEM_MAP_TRACE
  if (gFile != NULL)
  {
    for (i = 0, e = gMemoryMap; i < MAX_MEM_TAG; ++i, ++e)
    {
      if (e->tag == NULL)
        continue;
      crc = ~pcrcComputeString(e->tag);
      if (crc != e->crc)
        pfprintf(gFile, L("pmem|-|0|corrupt|%d|\n"), i);
    }
  }

  pfprintf(file, L("%-52s %10s %15s\n"), L("Memory tag"), L("Cur. Alloc"), L("Max. Alloc"));

  for (i = 0, e = gMemoryMap; i < MAX_MEM_TAG; ++i, ++e)
  {
    if (e->tag == NULL)
      continue;
    crc = ~pcrcComputeString(e->tag);
    if (crc != e->crc)
      pfprintf(file, L("**********%04d********** %38u %15u\n"), i, e->curAlloc, e->maxAlloc);
    else
    {
      len = LSTRLEN(e->tag);

      if (len > TAG_SIZE - 1)
      {
        LSTRCPY(truncatedTag, TAG_PREFIX);
        LSTRCPY(truncatedTag + TAG_PREFIX_SIZE, e->tag + (len - countToCopy));
        passert(LSTRLEN(truncatedTag) == TAG_SIZE - 1);
      }
      else
        LSTRCPY(truncatedTag, e->tag);
      pfprintf(file, L("%-52s %10u %15u\n"), truncatedTag, e->curAlloc, e->maxAlloc);
    }
#if PMEM_STACKTRACE
    data = gMemoryMap[i].first;
    while (data)
    {
      if (data->size != 0 && data->stackTrace != NULL)
      {
        LCHAR stackTrace[P_MAX_STACKTRACE];
        LCHAR* index;

        LSTRCPY(stackTrace, data->stackTrace);
        index = stackTrace;
        while (index)
        {
          index = LSTRSTR(index, L(" at "));
          if (index != NULL)
            *(index + 3) = L('\n');
        }
        pfprintf(file, L("StackTrace:\n%s\n\n"), stackTrace);
      }
      data = data->next;
    }
#endif
    passert(e->curAlloc >= 0);
    totalAlloc += e->curAlloc;
  }
  pfprintf(file, L("%-52s %10u %15u\n"), L("Total"), totalAlloc, gMaxAlloc);
  passert(totalAlloc == gCurAlloc);
#else
  /* not support */
#endif /* PMEM_MAP_TRACE */
  unlockMutex(&memMutex);

  return ESR_SUCCESS;
}
/*
DESCRIPTION
  The functionality described on this reference page is aligned with the ISO C standard. Any conflict between the requirements described here and the ISO C standard is unintentional. This volume of IEEE Std 1003.1-2001 defers to the ISO C standard.
The malloc() function shall allocate unused space for an object whose size in bytes is specified by size and whose value is unspecified.

The order and contiguity of storage allocated by successive calls to malloc() is unspecified. The pointer returned if the allocation succeeds shall be suitably aligned so that it may be assigned to a pointer to any type of object and then used to access such an object in the space allocated (until the space is explicitly freed or reallocated). Each such allocation shall yield a pointer to an object disjoint from any other object. The pointer returned points to the start (lowest byte address) of the allocated space. If the space cannot be allocated, a null pointer shall be returned. If the size of the space requested is 0, the behavior is implementation-defined: the value returned shall be either a null pointer or a unique pointer.

RETURN VALUE
Upon successful completion with size not equal to 0, malloc() shall return a pointer to the allocated space. If size is 0, either a null pointer or a unique pointer that can be successfully passed to free() shall be returned. Otherwise, it shall return a null pointer  and set errno to indicate the error.
*/
#ifdef PMEM_MAP_TRACE
void *pmalloc(size_t nbBytes, const LCHAR* tag, const LCHAR* file, int line)
#else
void *pmalloc(size_t nbBytes)
#endif
{
  MemoryData* data;
  void* result = NULL;
  size_t actualSize;
#ifdef PMEM_MAP_TRACE
  int idx;
  MemMapEntry* e;
#endif
#if PMEM_STACKTRACE
  size_t stackTraceSize = P_MAX_STACKTRACE;
  LCHAR* stackTrace;
  ESR_BOOL isInit;
  ESR_ReturnCode rc;
#endif

  if (gNbInit == 0)
    return NULL;

  lockMutex(&memMutex);

#ifdef PMEM_MAP_TRACE
  if (tag == NULL)
    tag = file;
  passert(tag != NULL);

  idx = getIndex(tag);
  if (idx == -1)
  {
    pfprintf(PSTDERR, L("ESR_INVALID_STATE: pmalloc() ran out of slots"));
    goto CLEANUP;
  }
  if (gMemoryMap[idx].tag == tag)
  {
    /* This is a new key, allocate memory for it */
    gMemoryMap[idx].tag = malloc(sizeof(LCHAR) * (LSTRLEN(tag) + 1));
    if (gMemoryMap[idx].tag == NULL)
      goto CLEANUP;
    LSTRCPY((LCHAR*) gMemoryMap[idx].tag, tag);
  }
#endif
  actualSize = sizeof(MemoryData) + nbBytes;

  data = (MemoryData *) malloc(actualSize);
  if (data == NULL)
  {
    /*
     * printf("no space when alloc %d from file %s line %d\nmem usage: %d\n",
     * nbBytes, file, line, PortMallocGetMaxMemUsed());
     */
    goto CLEANUP;
  }

#ifdef PMEM_MAP_TRACE
  data->index = idx;
#if PMEM_STACKTRACE
  rc = PStackTraceIsInitialized(&isInit);
  if (rc != ESR_SUCCESS)
    goto CLEANUP;
  if (isInit)
  {
    stackTrace = malloc(sizeof(LCHAR) * (stackTraceSize + 1));
    if (stackTrace == NULL)
      goto CLEANUP;
    rc = getStackTrace(stackTrace, &stackTraceSize);
    if (rc != ESR_SUCCESS)
      goto CLEANUP;
    /* Shrink stackTrace buffer */
    passert(LSTRLEN(stackTrace) < P_MAX_STACKTRACE);
    data->stackTrace = realloc(stackTrace, sizeof(LCHAR) * (LSTRLEN(stackTrace) + 1));
    if (data->stackTrace == NULL)
    {
      free(stackTrace);
      goto CLEANUP;
    }
  }
  else
    data->stackTrace = NULL;
#endif

  e = gMemoryMap + idx;

#if PMEM_STACKTRACE
  if (e->last != NULL)
    e->last->next = data;
  data->last = e->last;
  data->next = NULL;
  e->last = data;
  if (e->first == NULL)
    e->first = data;
#endif
#endif

  if (isLogEnabled)
  {
    data->size = actualSize;
#ifdef PMEM_MAP_TRACE
    e->curAlloc += actualSize;
    if (e->maxAlloc < e->curAlloc)
      e->maxAlloc = e->curAlloc;

    gCurAlloc += actualSize;
    if (gMaxAlloc < gCurAlloc)
      gMaxAlloc = gCurAlloc;
#endif
  }
  else
    data->size = 0;

  result = (void*)(data + 1);

#if PMEM_LOG_LOWLEVEL
  if (gFile != NULL && isLogEnabled)

    if (gFile != NULL)
    {
#if PMEM_STACKTRACE
      pfprintf(gFile, L("pmem|alloc|%s|%d|0x%x|%s|\n"), tag, actualSize, result, data->stackTrace);
#else
      pfprintf(gFile, L("pmem|alloc|%s|%d|0x%x|\n"), tag, actualSize, result);
#endif /* PMEM_STACKTRACE */
    }
#endif /* PMEM_LOG_LOWLEVEL */

CLEANUP:
  unlockMutex(&memMutex);
  return result;
}

#ifdef PMEM_MAP_TRACE
void *pcalloc(size_t nbItems, size_t itemSize, const LCHAR* tag, const LCHAR* file, int line)
#else
void *pcalloc(size_t nbItems, size_t itemSize)
#endif
{
  void* result = NULL;

  if (gNbInit == 1)
  {
#ifdef PMEM_MAP_TRACE
    result = (MemoryData *)pmalloc(nbItems * itemSize, tag, file, line);
#else
    result = (MemoryData *)pmalloc(nbItems * itemSize);
#endif
    if (result != NULL)
      memset(result, 0, nbItems * itemSize);
  }
  return (result);
}

/*
DESCRIPTION
The realloc() function changes the size of the memory object pointed to by ptr to the size specified by size. The contents of the object will remain unchanged up to the lesser of the new and old sizes. If the new size of the memory object would require movement of the object, the space for the previous instantiation of the object is freed. If the new size is larger, the contents of the newly allocated portion of the object are unspecified. If size is 0 and ptr is not a null pointer, the object pointed to is freed. If the space cannot be allocated, the object remains unchanged.
If ptr is a null pointer, realloc() behaves like malloc() for the specified size.

If ptr does not match a pointer returned earlier by calloc(), malloc() or realloc() or if the space has previously been deallocated by a call to free() or realloc(), the behaviour is undefined.

The order and contiguity of storage allocated by successive calls to realloc() is unspecified. The pointer returned if the allocation succeeds is suitably aligned so that it may be assigned to a pointer to any type of object and then used to access such an object in the space allocated (until the space is explicitly freed or reallocated). Each such allocation will yield a pointer to an object disjoint from any other object. The pointer returned points to the start (lowest byte address) of the allocated space. If the space cannot be allocated, a null pointer is returned.

 RETURN VALUE
Upon successful completion with a size not equal to 0, realloc() returns a pointer to the (possibly moved) allocated space. If size is 0, either a null pointer or a unique pointer that can be successfully passed to free() is returned. If there is not enough available memory, realloc() returns a null pointer
*/
#ifdef PMEM_MAP_TRACE
void *prealloc(void *ptr, size_t newSize, const LCHAR *file, int line)
#else
void *prealloc(void *ptr, size_t newSize)
#endif
{
  MemoryData* oldData;
  MemoryData* newData;
  void *result = NULL;
  size_t actualSize;
#ifdef PMEM_MAP_TRACE
  MemMapEntry* e;
#endif
  size_t oldSize;
#if PMEM_STACKTRACE
  const LCHAR* oldStackTrace;
  MemoryData* oldNext;
  MemoryData* oldLast;
#endif
  ESR_BOOL bMalloc = ESR_FALSE;

  if (gNbInit == 0)
    return NULL;

  if (newSize == 0 && ptr != NULL)
  {
#ifdef PMEM_MAP_TRACE
    pfree(ptr, file, line);
#else
    pfree(ptr);
#endif
    return NULL;
  }
  else if (ptr == NULL)
  {
#ifdef PMEM_MAP_TRACE
    return pmalloc(newSize, NULL, file, line);
#else
    return pmalloc(newSize);
#endif
  }

  lockMutex(&memMutex);

  oldData = (MemoryData *)(((unsigned char *) ptr) - sizeof(MemoryData));
  oldSize = oldData->size;
  passert(oldSize >= 0);
#if PMEM_STACKTRACE
  oldStackTrace = oldData->stackTrace;
  oldNext = oldData->next;
  oldLast = oldData->last;
#endif
#ifdef PMEM_MAP_TRACE
  e = gMemoryMap + oldData->index;
#endif

  actualSize = newSize + sizeof(MemoryData);
  if (oldSize != actualSize)
  {
#if defined(PORTABLE_DINKUM_MEM_MGR) || defined(PORTABLE_FIXED_SIZE_MEM_BLOCK_SCHEME)
    newData = (MemoryData *) PortNew(actualSize);
    if (newData == NULL)
    {
      pfprintf(PSTDERR, L("OUT_OF_MEMORY: prealloc() failed at %s:%d"), __FILE__, __LINE__);
      return NULL;
    }
    bMalloc = ESR_TRUE;
    if (oldSize >= actualSize)
    {
      memcpy(newData, oldData, actualSize);
    }
    else
    {
      memcpy(newData, oldData, oldSize);
    }
    PortDelete(oldData);
#else
    newData = (MemoryData *) realloc(oldData, actualSize);
    bMalloc = ESR_TRUE;
#endif
  }
  else /* No change */
  {
    newData = oldData;
  }

#ifdef PMEM_MAP_TRACE
  if (newData != NULL && bMalloc)
  {
    if (isLogEnabled)
    {
      e->curAlloc += actualSize - oldSize;
      if (e->maxAlloc < e->curAlloc)
        e->maxAlloc = e->curAlloc;

      gCurAlloc += actualSize - oldSize;
      if (gMaxAlloc < gCurAlloc)
        gMaxAlloc = gCurAlloc;
    }

#if PMEM_STACKTRACE
    newData->stackTrace = oldStackTrace;
    newData->next = oldNext;
    newData->last = oldLast;
    if (newData->last != NULL)
      newData->last->next = newData;
    if (newData->next != NULL)
      newData->next->last = newData;
    if (e->first == oldData)
      e->first = newData;
    if (e->last == oldData)
      e->last = newData;
#endif
  }
#endif

  if (newData != NULL)
  {
    newData->size = actualSize;
    result = (void*)(newData + 1);
  }

#if PMEM_LOG_LOWLEVEL
  if (gFile != NULL && isLogEnabled)
  {
#if PMEM_STACKTRACE
    LCHAR stackTrace[P_MAX_STACKTRACE];
    size_t len = P_MAX_STACKTRACE;
    ESR_ReturnCode rc;

    rc = getStackTrace(stackTrace, &len);
    if (rc != ESR_SUCCESS)
    {
      pfprintf(PSTDERR, "[%s:%d] getStackTrace failed with %s\n", __FILE__, __LINE__, ESR_rc2str(rc));
      goto CLEANUP;
    }
    pfprintf(gFile, L("pmem|%s|%d|realloc|%d|0x%x|%s|\n"), e->tag, oldSize, actualSize, ptr, stackTrace);
#else
    pfprintf(gFile, L("pmem|%s|%d|realloc|%d|0x%x|\n"), e->tag, oldSize, actualSize, ptr);
#endif /* PMEM_STACKTRACE */
  }
#endif /* PMEM_LOG_LOWLEVEL */

  unlockMutex(&memMutex);
  return result;
#if PMEM_STACKTRACE && PMEM_LOG_LOWLEVEL
CLEANUP:
  unlockMutex(&memMutex);
  return NULL;
#endif
}

#ifdef PMEM_MAP_TRACE
void pfree(void* ptr, const LCHAR* file, int line)
#else
void pfree(void* ptr)
#endif
{
  MemoryData* data;
#ifdef PMEM_MAP_TRACE
  MemMapEntry* e;
#endif
  if (ptr == NULL || gNbInit == 0)
    return;

  lockMutex(&memMutex);

  data = (MemoryData*)(((unsigned char*) ptr) - sizeof(MemoryData));
#ifdef PMEM_MAP_TRACE
  passert(data->index >= 0 && data->index <= MAX_MEM_TAG);
  e = gMemoryMap + data->index;
  if (isLogEnabled)
  {
    passert(e->curAlloc >= data->size);
    e->curAlloc -= data->size;

    passert(gCurAlloc >= data->size);
    gCurAlloc -= data->size;
  }
#if PMEM_STACKTRACE
  if (e->first != NULL && e->first == data)
    e->first = data->next;
  if (e->last != NULL && e->last == data)
    e->last = data->last;
  if (data->last != NULL)
    data->last->next = data->next;
  if (data->next != NULL)
  {
    data->next->last = data->last;
    data->next = NULL;
  }
  data->last = NULL;
#endif /* PMEM_STACKTRACE */
#if PMEM_LOG_LOWLEVEL
  if (gFile != NULL && isLogEnabled)
  {
#if PMEM_STACKTRACE
    LCHAR stackTrace[P_MAX_STACKTRACE];
    size_t len = P_MAX_STACKTRACE;
    ESR_ReturnCode rc;

    rc = getStackTrace(stackTrace, &len);
    if (rc != ESR_SUCCESS)
    {
      pfprintf(PSTDERR, "[%s:%d] getStackTrace failed with %s\n", __FILE__, __LINE__, ESR_rc2str(rc));
      goto CLEANUP;
    }
    pfprintf(gFile, L("pmem|free|%s|%s|%d|0x%x|%s|\n"), e->tag, data->stackTrace, data->size, ptr, stackTrace);
#else
    pfprintf(gFile, L("pmem|free|%s|%d|0x%x\n"), e->tag, data->size, ptr);
#endif /* PMEM_STACKTRACE */
  }
#endif /* PMEM_LOG_LOWLEVEL */
#if PMEM_STACKTRACE
  free((LCHAR*) data->stackTrace);
  data->stackTrace = NULL;
#endif /* PMEM_STACKTRACE */
#endif

  free(data);
  unlockMutex(&memMutex);
#if PMEM_STACKTRACE && PMEM_LOG_LOWLEVEL
CLEANUP:
  unlockMutex(&memMutex);
  return;
#endif

}

#endif
