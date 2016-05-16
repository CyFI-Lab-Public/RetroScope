/*---------------------------------------------------------------------------*
 *  pmalloc.h  *
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



/* xalloc.h internal header */
#ifndef _PMALLOC
#define _PMALLOC

#ifdef __cplusplus
extern "C"
{
#endif

#ifndef _STD
#define _STD
#endif
#define _STD_BEGIN
#define _STD_END

  typedef unsigned int psize_t;
  
#ifndef DKoffsetof
#define DKoffsetof(T, member) ((_STD psize_t)&(((T *)0)->member))
#endif
  
  /* storage alignment properties */
#define DK_AUPBND 1U /* even-byte boundaries (2^^1) */
#define DK_ADNBND 1U
#define DK_MEMBND 2U /* cts : 4 byte malloc boundaries (3 ==> 8 byte) */
  
#ifndef NULL
#define NULL 0
#endif
  
  _STD_BEGIN
  /* macros */
#define M_MASK ((1 << DK_MEMBND) - 1) /* rounds all sizes */
#define CELL_OFF ((DKoffsetof(_Cell, _Next) + M_MASK) & ~M_MASK)
#define SIZE_BLOCK   256   /* minimum block size, power of 2 */
#define SIZE_CELL ((sizeof (_Cell) + M_MASK) & ~M_MASK)
  /* type definitions */
  typedef struct _Cell
  {
    psize_t _Size; /* CELL_OFF <= SIZE_CELL <= _Size */
    struct _Cell *_Next; /* reused if CELL_OFF < SIZE_CELL */
  }
  _Cell;
  typedef struct
  {
    _Cell **_Plast; /* null, or where to resume malloc scan */
    _Cell *_Head; /* null, or lowest addressed free cell */
  }
  _Altab;
  /* declarations */
  
  void *_Getmem(psize_t);
  extern _Altab _Aldata; /* free list initially empty */
  
#if _INTELx86
  /* #define _PTR_NORM(p) (void __huge *)(p) should have worked */
#define _PTR_NORM(p) \
  ( (((unsigned long)(p) & 0xFFFF0000L)>>12) \
    + ((unsigned long)(p) & 0xFFFFL) )
#else
#define _PTR_NORM(p) (p)
#endif
  
  
#if DEBUG
  int _OK_Cell(_Cell *p);
  int _OK_Altab(_Altab *p);
  void _UPD_Altab(psize_t d_heap, psize_t d_alloc, psize_t d_free);
#else
#define _OK_Cell(p) (void)0
#define _OK_Altab(p) (void)0
#define _UPD_Altab(d_heap, d_alloc, d_free) (void)0
#endif /*DEBUG*/
  _STD_END
  
  /* function prototypes */
  void    PortMallocSetPoolSize(psize_t size);
  psize_t PortMallocGetPoolSize(void);
  int     PortMallocGetMaxMemUsed(void);
  void    PortMallocInit(void);
  void    PortMallocTerm(void);
  void *(PortMalloc)(psize_t size_arg);
  void(PortFree)(void *ptr);
  
#ifdef __cplusplus
} /* end extern "C" */
#endif

#endif /* _PMALLOC */



