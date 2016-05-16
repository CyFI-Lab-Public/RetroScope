
#include "NewMemMap.h"
#include "ds/rb_tree.h"

class MemSegCompareStarts {
public:
  bool operator()(const MemSeg& a, const MemSeg& b)
  { return a.start < b.start; }
};

BDS_RB_Tree<MemSeg, MemSegCompareStarts> zmbNewMap;

static inline int dealocSeg(MemSeg* m, void* __unused) {
  free(m->name);
  return 0;
}

static inline void clearNewMap() {
  zmbForEachNewMemSeg(dealocSeg, NULL);
  zmbNewMap.clear();
}


void zmbNewMemMapInit()
{
  zmbNewMemMapRefresh();
}

void zmbNewMemMapRefresh()
{
  clearNewMap();
  FILE* map_file = fopen("/proc/self/maps", "r");
  if(!map_file)
  {
    ZMB_LOG("Cannot open map file: /proc/self/maps!");
    return;
  }

#define BUFFER_SZ (1024)
  char line[BUFFER_SZ];
  memset(line, 0, BUFFER_SZ);
  while(fgets(line, BUFFER_SZ, map_file) != NULL)
  {
    zmbNewMap.insert(MemSegFromProcLine(line));
  }
  fclose(map_file);
#undef BUFFER_SZ
}

MemSeg * zmbMemSegForNewAddr(const void *addr)
{
  MemSeg search;
  search.start = (uintptr_t)addr;

  BDS_RB_Tree<MemSeg, MemSegCompareStarts>::iterator found =
    --(zmbNewMap.upper_bound(search));
  if(found != zmbNewMap.end() && zmbAddrInMemSeg(addr, &(*found)))
    return &(*found);
  else
    return NULL;
}

void zmbForEachNewMemSeg(int (*cb_func)(MemSeg*, void*), void* arg)
{
  for(BDS_RB_Tree<MemSeg, MemSegCompareStarts>::iterator it = zmbNewMap.begin();
      it != zmbNewMap.end(); ++it)
  {
    if(cb_func(&(*it), arg) != 0) break;
  }
}

