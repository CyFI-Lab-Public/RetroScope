/*---------------------------------------------------------------------------*
 *  pmalloc.c  *
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




/* this source file is only used when PORTABLE_DINKUM_MEM_MGR is defined
 */
#ifdef PORTABLE_DINKUM_MEM_MGR

#include <stdlib.h>
#include <string.h> /* for memset */
#include "pmalloc.h"
#include "passert.h"
#include "ptypes.h"
#include "plog.h"

#undef malloc
#undef calloc
#undef realloc
#undef free

#ifdef __cplusplus
extern "C"
{
#endif

  /*
   * There are two controlling options within this scheme:
   *
   * STATIC_MEMORY_POOL: When defined, there is a static array from which memory is
   * allocated.  The size of this array is defined at compile time.  When undefined
   * (the default), the memory is allocated via malloc().  This code works under PSOS and
   * PSOSIM, but has not been tested anywhere else (4March02).
   * VOYAGER_KERNEL_MEMORY: When defined for the Voyager platform, it is similar to the
   * non-static memory pool, but the memory buffer is pre-defined, and is simply pointed
   * at by the pmalloc initializer.
   * RTXC_PARTITION_MEMORY: When defined for the RTXC operating system, uses a static kernel
   * partition resource for the memory chunk.
   * VOYAGER_KERNEL_MEMORY and RTXC_PARTITION_MEMORY are mutually exclusive and take precedence
   * over STATIC_MEMORY.
   *
  
   * the standard off-the-shelf Dinkumware software is pretty slow, primarily due to
   * scanning the free-memory linked list in PortFree(). If SPEEDUP is defined, then
   * split the memory pool into imaginary 'bins', and keep track of the first free list
   * entry in each bin. Then the linked list lookup can be MUCH faster, as you can
   * start very close to the final destination in the linked list.
   *
   * (If SPEEDUP_COMPARE is defined, then run BOTH the standard technique and the
   * speedup technique and compare the results.)
   */
  
  /* malloc function */
  _STD_BEGIN
  
  /* data *******************************************************************************/
  
#if defined(PORTABLE_DINKUM_MEM_MGR) || defined(PORTABLE_FIXED_SIZE_MEM_BLOCK_SCHEME)
  /* Verify that memory pool actually was created, because of the lack of structure, this is accessed externally */
  ESR_ReturnCode memory_pool_creation_status = ESR_FATAL_ERROR;
#endif
  
  /* static data */
  _Altab  _Aldata  = {0}; /* heap initially empty */
  psize_t  _Size_block = {SIZE_BLOCK}; /* preferred _Getmem chunk */
  
  /* Memory pool size */
#define MEM_SIZE_MB( mbytes )   ((mbytes) * 1024 * 1024 )
  
#ifndef MEM_SIZE
  /* If not defined on the command line, use default values. */
#define MEM_SIZE    MEM_SIZE_MB( 5 )
#endif
  
  /* Memory pool initialized */
  static int pmallocInitted = FALSE;  /* TRUE once initialized */
  
#ifdef STATIC_MEMORY_POOL
  /* The memory pool can either be statically allocated or require a one-time system malloc.
   * For VTB, the system was taking 2 seconds to zero the static memBuffer[] array at
   * boot time, since it's in the BSS segment. Therefore, for VTB, it is better to allocate
   * at run time.
   */
  static char memBuffer[MEM_SIZE];
#else
  static char *memBuffer;
#endif
  
  static psize_t memSize = MEM_SIZE;
  
  /* Memory pool free list */
  /* partition memory range into 'bins', and keep track of the first
   * free list entry in each bin. This is to speed up the linked-list search
   * that takes place when memory is freed.
   */
#define BIN_BITS         14   /* smaller number ==> more bins */
#define BIN_SIZE      16384   /* 2 ^ BIN_BITS */
  
#define __NUM_MEM_BINS(memPoolSize)  (((memPoolSize)/BIN_SIZE) + 5) /* 5 = extra for roundoff */
#define GET_MEM_BIN( _ptr_ )   (int)(((unsigned int)_ptr_ - (unsigned int)&memBuffer[0]) >> BIN_BITS)
  
#define NUM_MEM_BINS  __NUM_MEM_BINS(MEM_SIZE)
static _Cell				*binsFirstFreeCell[NUM_MEM_BINS+1] = {0};
  static psize_t    numMemBins;
  
  /* Memory Pool sbrk/getmem variables */
  
  static char *__heap_ptr = NULL;
  static char *__heap_end = NULL;
  static int  maxMemUsed = 0;
  
  /* Memory Pool initialization and _GetMem functions ************************************/
  
#if _USE_EXISTING_SYSTEM_NAMES
#define _Sbrk sbrk
#endif
  
  _STD_BEGIN
  
  void *_Sbrk(int incr)
  {
    char *ret;
    
    /* Subtract 1 from __heap_ptr so that the left hand side of the comparison evaluates to the address of the
       last address of the requested memory block */
    if ((__heap_ptr + incr - 1) > __heap_end) return(void *) - 1;
    
    ret = __heap_ptr;
    __heap_ptr += incr;
    maxMemUsed += incr;
    return (void *)ret;
  }
  
  void *_Getmem(psize_t size)
  { /* allocate raw storage */
    void *p;
    int isize = size;
    
    return (isize <= 0 || (p = _Sbrk(isize)) == (void *) - 1
            ? 0 : p);
  }
  _STD_END
  
  /* PortMallocInit() : initialize memory pool. There is no initialization needed for
   * a static memory pool. Otherwise, perform a one-time malloc from the OS.
   */
  void PortMallocInit(void)
  {
#if defined STATIC_MEMORY_POOL
    memSize = MEM_SIZE;
#else
    /* TODO: is malloc of binsFirstFreeCell safe under PSOS in all conditions ? */
    memBuffer    = (char *)malloc(memSize);
#if defined(PORTABLE_DINKUM_MEM_MGR) || defined(PORTABLE_FIXED_SIZE_MEM_BLOCK_SCHEME)
    if (memBuffer != NULL) /* For external access, check comment at top */
      memory_pool_creation_status = ESR_SUCCESS;
#endif
    numMemBins    = __NUM_MEM_BINS(memSize);
#endif /* #ifdef VOYAGER_KERNEL_MEMORY */
    
    passert(memBuffer != NULL);
    passert(binsFirstFreeCell != NULL);
    
    __heap_ptr = &memBuffer[0];
    __heap_end = &memBuffer[memSize-1];
    
    /* set initted flag so we only do this once */
    pmallocInitted = TRUE;
    maxMemUsed = 0;
    
    memset(&_Aldata, 0, sizeof(_Altab));
    
    memset(binsFirstFreeCell, 0, sizeof(_Cell*)*(NUM_MEM_BINS + 1));
  }
  
  void PortMallocTerm(void)
  {
#ifndef STATIC_MEMORY_POOL
    memSize = 0;
    free(memBuffer);
    memBuffer = NULL;
#if defined(PORTABLE_DINKUM_MEM_MGR) || defined(PORTABLE_FIXED_SIZE_MEM_BLOCK_SCHEME)
    memory_pool_creation_status = ESR_FATAL_ERROR; /* For external access, check comment at top */
#endif
#endif
    pmallocInitted = FALSE;
  }
  
  /* PortGetMaxMemUsed() : return the maximum real memory allocated.
   * There is another function of the same name in pmemory.cpp, for tracking
   * psos block memory. It uses #ifdef MEM_PSOS_BLOCK_SCHEME to enable.
   */
  int PortMallocGetMaxMemUsed(void)
  {
    return maxMemUsed;
  }
  
  /* PortMallocSetPoolSize( psize_t size ) : define size of memory pool.
   */
  void PortMallocSetPoolSize(psize_t size)
  {
#if !defined(STATIC_MEMORY_POOL) && !defined(VOYAGER_KERNEL_MEMORY) && !defined(RTXC_PARTITION_MEMORY)
    if (!pmallocInitted)
    {
      memSize = size;
    }
#else
    (void)size;
#endif
  }
  
  /* PortMallocGetPoolSize() : return size of memory pool.
   */
  psize_t PortMallocGetPoolSize(void)
  {
#if defined STATIC_MEMORY_POOL
    return MEM_SIZE;
#else
    return memSize;
#endif
  }
  
  /* debug *******************************************************************************/
  
  /* xalloc.h internal header - debugging components */
#if DEBUG
  
  int _OK_Cell(_Cell *p)
  {
    passert(SIZE_CELL <= p->_Size);
    return 1;
  }
  
  typedef struct _DB_Altab
  {
    psize_t total_heap;
    psize_t total_alloc;
    psize_t total_free;
  }
  _DB_Altab;
  
  _DB_Altab _DB_Altab_object = {0};
  
  void _UPD_Altab(psize_t d_heap, psize_t d_alloc, psize_t d_free)
  {
    _DB_Altab *pd = &_DB_Altab_object;
    pd->total_heap += d_heap;
    pd->total_alloc += d_alloc;
    pd->total_free += d_free;
  }
  
  int _OK_Altab(_Altab *p)
  {
    _DB_Altab *pd = &_DB_Altab_object;
    _Cell *q;
    psize_t total_free = 0;
    if (p->_Head == 0)
      return 1;
    for (q = p->_Head; q != 0; q = q->_Next)
    {
      total_free += q->_Size;
      _OK_Cell(q);
      if (q->_Next != 0)
      {
        passert(_PTR_NORM((char *)q + q->_Size) <=
                _PTR_NORM((char *)q->_Next));
        passert(_PTR_NORM(q) < _PTR_NORM(q->_Next));
      }
    }
    passert(pd->total_heap == pd->total_alloc + pd->total_free);
    passert(total_free == pd->total_free);
    return 1;
  }
#endif /* DEBUG */
  
  /* allocation functions ***************************************************************/
  
  static _Cell **findmem(psize_t size)
  { /* find storage */
    _Cell *q, **qb;
    
    for (; ;)
    { /* check freed space first */
      if ((qb = _Aldata._Plast) == 0)
      { /* take it from the top */
        for (qb = &_Aldata._Head; *qb != 0;
             qb = &(*qb)->_Next)
          if (size <= (*qb)->_Size)
            return (qb);
      }
      else
      { /* resume where we left off */
        for (; *qb != 0; qb = &(*qb)->_Next)
          if (size <= (*qb)->_Size)
            return (qb);
        q = *_Aldata._Plast;
        for (qb = &_Aldata._Head; *qb != q;
             qb = &(*qb)->_Next)
          if (size <= (*qb)->_Size)
            return (qb);
      }
      { /* try to buy more space */
        psize_t bs;
        
        for (bs = _Size_block; ; bs >>= 1)
        { /* try larger blocks first */
          if (bs < size)
            bs = size;
          if ((q = (_Cell *)_Getmem(bs)) != 0)
            break;
          else if (bs == size)
            return (0); /* no storage */
        }
        /* got storage: add to heap and retry */
        q->_Size = bs;
        _UPD_Altab(q->_Size, q->_Size, 0); /* heap=alloc+free */
        PortFree((char *)q + CELL_OFF);
      }
    }
  }
  
  
  void *(PortMalloc)(psize_t size_arg)
  { /* allocate a data object on the heap */
    _Cell *q, **qb;
#ifdef SPEEDUP
    int qbsBin;
    int qbNextBin;
#endif /* SPEEDUP */
    psize_t size;
    
    passert(pmallocInitted);
    
    size = (size_arg + (CELL_OFF + M_MASK)) & ~M_MASK;
    
    _OK_Altab(&_Aldata);
    if (size <= size_arg)
      return (0); /* size_arg too large */
    if (size < SIZE_CELL) /* round up size */
      size = SIZE_CELL;
    if ((qb = findmem(size)) == 0)
      return (0);
    q = *qb;
    if (q->_Size - SIZE_CELL < size)
    {
      /* use entire cell (there's not enough space to carve out a new cell from this one) */
#ifdef SPEEDUP
      /* remove *qb cell from free list.
       * careful : the Next pointer may be in a different bin.
       */
      qbsBin = GET_MEM_BIN(*qb);
      
      /* Check whether the cell is at the end of the 'free' linked-list */
      if (0 != ((*qb)->_Next))
      {
        /* The cell is not at the end of the free linked-list; find out which bin the next free cell is in */
        qbNextBin = GET_MEM_BIN((*qb)->_Next);
        
        if (qbsBin == qbNextBin)
        {
          if (binsFirstFreeCell[qbsBin] == *qb)
          {
            /* The allocated cell was the first free cell in the bin; update the first free cell
               pointer to point to the next free cell */
            binsFirstFreeCell[qbsBin] = (*qb)->_Next;
          }
        }
        else
        {
          if (binsFirstFreeCell[qbsBin] == *qb)
          {
            /* The allocated cell was the only free cell in the bin; update the first free cell
               pointer to point to NULL */
            
            binsFirstFreeCell[qbsBin] = 0;
          }
        }
      }
      else
      {
        /* Cell is at the end of the 'free' linked-list */
        if (binsFirstFreeCell[qbsBin] == *qb)
        {
          /* The allocated cell was the first free cell in the bin; there are no following free cells
             so set the first free cell pointer to NULL */
          binsFirstFreeCell[qbsBin] = 0;
        }
      }
#endif /* SPEEDUP */
      *qb = q->_Next;
    }
    else
    { /* peel off a residual cell */
      *qb = (_Cell *)((char *)q + size);
      (*qb)->_Next = q->_Next;
      (*qb)->_Size = q->_Size - size;
      q->_Size = size;
#ifdef SPEEDUP
      /* remove q from free list, and add *qb to free list.
       * Do this as two separate steps because they may be in 2 different bins.
       */
      /* remove q from free list */
      if (binsFirstFreeCell[GET_MEM_BIN(q)] == q)
        binsFirstFreeCell[GET_MEM_BIN(q)] = 0;
      /* now add *qb to its bin's free list if it's the first */
      qbsBin = GET_MEM_BIN(*qb);
      if ((binsFirstFreeCell[qbsBin] == 0) || (*qb < binsFirstFreeCell[qbsBin]))
        binsFirstFreeCell[qbsBin] = *qb;
#endif /* SPEEDUP */
    }
    _Aldata._Plast = qb; /* resume scan here */
    _UPD_Altab(0, q->_Size, -q->_Size); /* heap=alloc+free */
    _OK_Altab(&_Aldata);
    return ((char *)q + CELL_OFF);
  }
  _STD_END
  
  
  /* free function */
  _STD_BEGIN
  
  void(PortFree)(void *ptr)
  { /* free an allocated data object */
    register _Cell *q;
    register psize_t size;
#ifdef SPEEDUP
    int binNum;
    int binIndex;
    int qNextBin;
    int qNextNextBin;
#endif /* SPEEDUP */
    static int portFreeCount = 0;
    portFreeCount++;
    
    passert(pmallocInitted);
    
    _OK_Altab(&_Aldata);
    if (ptr == 0)
      return;
    q = (_Cell *)((char *)ptr - CELL_OFF);
    size = q->_Size;
#ifdef SPEEDUP
    binNum = GET_MEM_BIN(q);
#endif /* SPEEDUP */
    if (size < SIZE_CELL || (size & M_MASK) != 0)
      return; /* erroneous call, bad count */
    if (_Aldata._Head == 0
        || _PTR_NORM(q) < _PTR_NORM(_Aldata._Head))
    { /* insert at head of list */
      q->_Next = _Aldata._Head;
      _Aldata._Head = q;
#ifdef SPEEDUP
      /* always the start of a bin */
      binsFirstFreeCell[binNum] = q;
#endif /* SPEEDUP */
    }
    else
    { /* scan for insertion point */
      register _Cell *qp = _Aldata._Head;
      register char *qpp;
      register _Cell *nextCell;
#if !defined SPEEDUP || defined SPEEDUP_COMPARE
      _Cell *savedQp;
      
      /* this search loop is where all the time is spent */
      while ((nextCell = qp->_Next) != 0
             && _PTR_NORM(nextCell) < _PTR_NORM(q))
        qp = qp->_Next;
      savedQp = qp;
#endif /* SPEEDUP */
      
#ifdef SPEEDUP
      /* this is where the SPEEDUP code is sped up : start with the bin's first free cell */
      _Cell *firstFreeInBin = binsFirstFreeCell[binNum];
      if ((firstFreeInBin != 0) && (q > firstFreeInBin))
      {
        qp = firstFreeInBin;
        while ((nextCell = qp->_Next) != 0
               && _PTR_NORM(nextCell) < _PTR_NORM(q))
        {
          qp = qp->_Next;
        }
      }
      else
      {
        /* go back to the previous non-zero bin */
        qp = NULL;  /* for diagnostics */
        for (binIndex = binNum; binIndex >= 0; binIndex--)
        {
          if ((binsFirstFreeCell[binIndex] != 0) && (q > binsFirstFreeCell[binIndex]))
          {
            qp = binsFirstFreeCell[binIndex];
            break;
          }
        }
        /* this code for diagnostic purposes to see how often it happens. otherwise,
         * qp could have been set to _Aldata._Head prior to the binIndex loop above.
         */
        if (qp == NULL)
        {
          qp = _Aldata._Head;
        }
        
        /* find the free cell location */
        while ((nextCell = qp->_Next) != 0
               && _PTR_NORM(nextCell) < _PTR_NORM(q))
          qp = qp->_Next;
      }
      
#ifdef SPEEDUP_COMPARE
      if (qp != savedQp)
        printf("oops \n");
#endif /* SPEEDUP_COMPARE */
#endif /* SPEEDUP */
        
#if !defined SPEEDUP || defined SPEEDUP_COMPARE
      qp = savedQp;
#endif /* SPEEDUP */
      
      qpp = (char *)qp + qp->_Size;
      if (_PTR_NORM((char *)q) < _PTR_NORM(qpp))
        return; /* erroneous call, overlaps qp */
      else if (_PTR_NORM(qpp) == _PTR_NORM((char *)q))
      { /* merge qp and q (merge with prior cell) */
        /* nothing to do to bin's free list here */
        qp->_Size += q->_Size;
        q = qp;
      }
      else if (qp->_Next != 0 && _PTR_NORM((char *)qp->_Next)
               < _PTR_NORM((char *)q + q->_Size))
        return; /* erroneous call, overlaps qp->_Next */
      else
      { /* splice q after qp */
#ifdef SPEEDUP
        /* add 1 entry here - this could change first entry in q's bin */
        _Cell *firstFree = binsFirstFreeCell[binNum];
        if ((firstFree == 0) || (q < firstFree))
          binsFirstFreeCell[binNum] = q;
#endif /* SPEEDUP */
        q->_Next = qp->_Next;
        qp->_Next = q;
      }
    }
    if (q->_Next != 0 && _PTR_NORM((char *)q + q->_Size)
        == _PTR_NORM((char *)q->_Next))
    { /* merge q and q->_Next (merge with latter cell) */
#ifdef SPEEDUP
      /* lose 1 cell here - this could change first entry in bin.
       * if q->_Next was first in bin, now it's its Next.
       * careful : watch for next being in a different bin.
       */
      qNextBin = GET_MEM_BIN(q->_Next);
      
      if (binsFirstFreeCell[qNextBin] == q->_Next)
      {
        /* The q->_Next cell is the first free cell in its bin; set the first free cell
           pointer to NULL for now; if there is another free cell in the same bin then
           the first free cell pointer will be updated in next 'if' code block */
        binsFirstFreeCell[qNextBin] = 0;
        
        /* If there is another free cell after q->_Next and it's in the same bin then
           update the first free cell pointer if necessary */
        if (0 != (q->_Next->_Next))
        {
          qNextNextBin = GET_MEM_BIN(q->_Next->_Next);
          
          /* The first free cell pointer for q->_Next->_Next's bin can only be set to 0
             by the code above; if it is 0 then q->_Next->_Next must be the first free
             cell in the bin */
          if (0 == binsFirstFreeCell[qNextNextBin])
          {
            binsFirstFreeCell[qNextNextBin] = q->_Next->_Next;
          }
        }
      }
#endif /* SPEEDUP */
      _Aldata._Plast = 0; /* deoptimize for safety */
      q->_Size += q->_Next->_Size;
      q->_Next = q->_Next->_Next;
    }
    _UPD_Altab(0, -size, size); /* heap=alloc+free */
    _OK_Altab(&_Aldata);
    /* every successful free "falls off" here */
  }
  _STD_END
  
#ifdef __cplusplus
} /* end extern "C" */
#endif

#endif /* PORTABLE_DINKUM_MEM_MGR */


