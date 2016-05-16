#ifndef DALVIK_ZOMBIE_NEWMEMMAP_H_
#define DALVIK_ZOMBIE_NEWMEMMAP_H_

#include "MemMap.h"

static inline MemSeg MemSegFromProcLine(char* proc_line)
{
  MemSeg ret;
  memset(&ret, 0x0, sizeof(MemSeg));
  sscanf(proc_line,"%x-%x %4s", &ret.start, &ret.end, ret.perm);
  char * name = strrchr(proc_line, ' ')+1;
  size_t l;
  if(name[0] == '\n') l = 0;
  else l = strlen(name);
  memcpy(ret.name, name, l);
  ret.name[l] = '\0';
  ret.len = ret.end - ret.start;
  return ret;
}

#ifndef ZMB_EXTERNAL

void zmbNewMemMapInit();

void zmbNewMemMapRefresh();

MemSeg * zmbMemSegForNewAddr(const void *addr);

void zmbForEachNewMemSeg(int (*cb_func)(MemSeg*, void*), void* arg);

static inline bool zmbIsSafeNewAddr(const void *addr)
{
  MemSeg *m = zmbMemSegForNewAddr(addr);
  return m != NULL; 
}

#endif // ZMB_EXTERNAL

#endif // DALVIK_ZOMBIE_NEWMEMMAP_H_
