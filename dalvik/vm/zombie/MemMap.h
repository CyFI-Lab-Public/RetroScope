#ifndef DALVIK_ZOMBIE_MEMMAP_H_
#define DALVIK_ZOMBIE_MEMMAP_H_

#include "Zombie.h"

#ifndef __cplusplus
# include <stdbool.h>
#endif

#include <cstring>
#include <cstdio>


struct MemSeg {
  uintptr_t start; // original start addr
  uintptr_t end;   // original end addr
  uintptr_t len;
  uintptr_t offset; // offset in the .mem file
  char name[500];
  char perm[5];
};

static inline MemSeg MemSegFromMapLine(char * map_line)
{
  MemSeg m;
  memset(&m, 0x0, sizeof(MemSeg));
  sscanf(map_line, "%x->%x->%x", &(m.start), &(m.end), &(m.offset));
  char * name = strrchr(map_line, ' ')+1;
  size_t ln;
  if(name[0] == '\n') ln = 0;
  else ln = strlen(name);
//  m.name = (char*)malloc(ln);
  memcpy(m.name, name, ln);
  m.name[ln] = '\0';
  m.len = m.end - m.start;
  return m;
}

//static inline bool isCopySeg(MemSeg* m)
//{
//  static const char* segs[] = {"dalvik-zygote", 
//                               "dalvik-heap",
//                               "[heap]",
//                               ".so",
//                               "/system/lib/libstdc++.so",
//                               "/system/lib/libc.so",
//                               "/system/lib/libcutils.so",
//                               "/system/lib/libhwui.so",
//                               "/system/lib/libskia.so",
//                               "/system/lib/libdvm.so",
//                               "/system/lib/libicui18n.so",
//                               ".dex",  // for zombie code
//                               ".odex", // for zombie code
//                               "apk", // for zombie code
//                               "dalvik-aux-structure", // for zombie code
//                               "LinearAlloc",
//                               "libandroid_runtime.so",
//                               "/dev/__properties__",
//                               ".ttf",
//                               ".jar",
//                               ".dat",
//                               "ashmem",
//                               "dmabuf",
//                               "pvrsrvkm" 
//                               "libandroidfw.so"
//                              };

  //static const char seg_bitmap[] = "dalvik-bitmap"; // for zombie code
  //static const char seg_functors[] = "libwebviewchromium_plat_support";

//#define n_segs (sizeof (segs) / sizeof (const char *))

//  if(m == NULL) return false;
//  return true;

//  if(strlen(m->name) == 0) return true;

//  for(unsigned i = 0; i < n_segs; ++i)
//  {
//    if(strstr(m->name, segs[i]) != NULL) return true;
//  }
//  return false;
//}

static inline bool zmbAddrInMemSeg(const void *addr, MemSeg *m)
{
  return (m->start <= (uintptr_t)addr && (uintptr_t)addr < m->end);
}

static inline void * zmbNoAddrTrans(const void *addr) { return (void*)addr; }

void * zmbTranslateAddr(const void *addr);
extern "C" bool zmbIsSafeAddr(const void *addr);
bool zmbIsSafeAddr(const void *addr, unsigned size);
extern "C" char * zmbGetNameForOldSymbolValue(void * val);

#ifndef ZMB_EXTERNAL

#include "zmb_elf.h"

void zmbMemMapInit(const char * map_file_name, const char * mem_file_name);
void zmbFillFromMemFile();

MemSeg * zmbMemSegForAddr(const void *addr);
void * zmbTranslateAddrInSeg(const void *addr, MemSeg *seg);
void zmbForEachMemSeg(int (*cb_func)(MemSeg*, void*), void* arg);

Elf_obj * zmbGetELFObjForSeg(const char * name);

#endif // !ZMB_EXTERNAL

#endif // DALVIK_ZOMBIE_MEMMAP_H_
