#include "MemMap.h"
#include "NewMemMap.h"
#include <sys/mman.h>
#include <fcntl.h>
#include "ds/rb_tree.h"
#include "ds/list.h"
#include "zmb_elf.h"

/**
 * Compare two MemSegRBNodes. Essentially, if
 *  the segments start at the same addr
 *  then we assume they are equal.
 */
class MemSegCompareStarts {
public:
  bool operator()(const MemSeg& a, const MemSeg& b)
  { return a.start < b.start; }
};

struct MemMap {
  bool init;
  BDS_RB_Tree<MemSeg, MemSegCompareStarts> segs;
  typedef BDS_RB_Tree<MemSeg, MemSegCompareStarts>::iterator iterator; 
  int fd;
  char *mem;
  uintptr_t lowest_addr;
  uintptr_t memsz;
  
  MemMap() {
    init = false;
  }
};

struct MemMap zmbMemMap;

/**
 * zmbMemMapInit fills the zmbMemMap.segs tree with the segs
 * listed in map_file_name, but DOES NOT map anything into memory!
 *
 * Mapping is done by zmbReserveHeapAreas (no memory image file, just
 * grabs the *real* regions), zmbMapMemFile (maps the memory
 * image into random address), and zmbFillFromMemFile (fills the
 * real regions with data).
 *
 * ***** Be sure to call zmbNewMemMapInit after this!!!!!!!!!!!!!!
 */
void zmbMemMapInit(const char * map_file_name, const char * mem_file_name)
{
  if(zmbMemMap.init) return;
  FILE* map_file = fopen(map_file_name, "r");
  if(!map_file)
  {
    ZMB_LOG("Cannot open map file: %s : %s!", map_file_name, strerror(errno));
    return;
  }

  zmbMemMap.lowest_addr = 0;
  zmbMemMap.memsz = 0;

#define BUFFER_SZ (1024)
  char line[BUFFER_SZ];
  memset(line, 0, BUFFER_SZ);
  while(fgets(line, BUFFER_SZ, map_file) != NULL)
  {
    MemSeg seg = MemSegFromMapLine(line);
    if(zmbMemMap.lowest_addr == 0)
      zmbMemMap.lowest_addr = seg.start;
    zmbMemMap.memsz = seg.offset + seg.len;
    //ZMB_LOG("::%s:%x:%x", seg.name, seg.start, seg.end);
    zmbMemMap.segs.insert(seg);
  }
  fclose(map_file);
#undef BUFFER_SZ

  zmbMemMap.fd = open(mem_file_name, O_RDONLY);
  if(zmbMemMap.fd == -1)
  {
    ZMB_LOG("Error File %s",  strerror(errno));
    return;
  }

  ZMB_LOG("Imported (Nothing Mapped): %s", map_file_name);
  zmbMemMap.init = true;
}

/**
 * Here is where the magic happens! We merge the old dalvik heap (now
 * mapped into zmbMemMap.mem) into the current process's dalvik heap!
 */
struct mapSegIfGoodArgs {
  bool copy_data;
  bool kill_hard;
  bool failure;
};
static inline int mapSegIfGood(MemSeg* m, void* arg)
{
  mapSegIfGoodArgs* a = (mapSegIfGoodArgs*)arg;
//  ZMB_LOG("Map: %x : %x : %s", m->start, m->end, m->name);
  // print what we will kill
  for(u1* addr = (u1*)m->start; addr < (u1*)m->end; addr+=0x1000)
  {
    MemSeg *new_m = zmbMemSegForNewAddr(addr);
    if (new_m != NULL)
    {
//      ZMB_LOG("Kill: %x : %x : %s", new_m->start, new_m->end, new_m->name);
      if(a->kill_hard)
      {
        a->failure = true;
        return 1;
      }
      addr = (u1*)new_m->end - 0x1000;
    }
  }
  munmap((void*)m->start, m->len); // just remove whatever is there
  void * ret = NULL;
  if(!(a->copy_data)) {
    ret = mmap((void*)m->start, m->len, PROT_READ|PROT_WRITE|PROT_EXEC,
               MAP_PRIVATE|MAP_FIXED|MAP_ANONYMOUS, -1, 0);
  }
  else {
    ret = mmap((void*)m->start, m->len, PROT_READ|PROT_WRITE|PROT_EXEC,
               MAP_PRIVATE|MAP_FIXED, zmbMemMap.fd, m->offset);
  }
  if(ret != (void*)m->start) ZMB_LOG("MMAP! %s", strerror(errno));
  return 0; // keep looping!
}


/**
 * BEFORE calling zmbReserveHeapAreas, zmbMapMemFile, or zmbFillFromMemFile
 * you MUST call zmbMemMapInit to fill the zmbMemMap.segs tree!
 */


//#ifdef ZMB_ALL_PROCS_INIT
/**
 * Reserves the space for the memory image file at a random address
 */
//void zmbReserveMemImgSpace()
//{
//  zmbMemMap.mem = //(char*) mmap(NULL, zmbMemMap.memsz, PROT_READ|PROT_WRITE, MAP_PRIVATE, zmbMemMap.fd, 0); 
//    (char*) zmbMmapSafe(zmbMemMap.memsz, PROT_READ, MAP_PRIVATE|MAP_ANONYMOUS, -1, 0);
//  if(zmbMemMap.mem == (void*)-1)
//  {
//    ZMB_LOG("Error Map %s",  strerror(errno));
//    return;
//  }
//
//  ZMB_LOG("Reserved %p -> %p", zmbMemMap.mem, zmbMemMap.mem + zmbMemMap.memsz);
//}


/**
 * Just reserves the REAL memory segments with blank data
 */
//extern "C" int zmbReserveHeapAreas() {
//  zmbMemMapInit(map_file_name);
//  zmbNewMemMapInit();
//  mapSegIfGoodArgs a = {false, true, false};
//  zmbForEachMemSeg(mapSegIfGood, &a);
//  if(a.failure) return 1;
//  zmbReserveMemImgSpace();
//  ZMB_LOG("Real Mem Segments reserved (but still empty)");
//  return 0;
//}
//#endif // ZMB_ALL_PROCS_INIT


int findFullMemSeg(MemSeg * m, void * _unused)
{
  if (m->len == zmbMemMap.memsz)
  {
    munmap((void *)m->start, m->len);
    zmbMemMap.mem = (char*) mmap((void *)m->start, zmbMemMap.memsz, PROT_READ, MAP_PRIVATE|MAP_FIXED, zmbMemMap.fd, 0); 
    return 1;
  }
  return 0;
}

/**
 * Maps the memory image file into a random address
 */
// void zmbMapMemFile(const char * mem_file_name)
// {
//   zmbMemMap.fd = open(mem_file_name, O_RDONLY);
//   if(zmbMemMap.fd == -1)
//   {
//     ZMB_LOG("Error File %s",  strerror(errno));
//     return;
//   }
// 
// //  munmap(zmbMemMap.mem, zmbMemMap.memsz);
// //  zmbMemMap.mem = (char*) mmap(NULL, zmbMemMap.memsz, PROT_READ|PROT_WRITE, MAP_PRIVATE, zmbMemMap.fd, 0); 
// //    (char*) zmbMmapSafe(zmbMemMap.memsz, PROT_READ|PROT_WRITE, MAP_PRIVATE, zmbMemMap.fd, 0);
//   zmbMemMap.mem = (char*)-1;
//   zmbForEachNewMemSeg(findFullMemSeg, NULL);
// 
//   if(zmbMemMap.mem == (void*)-1)
//   {
//     ZMB_LOG("Error Map %s",  strerror(errno));
//     return;
//   }
// 
//   ZMB_LOG("Mapped %p -> %p", zmbMemMap.mem, zmbMemMap.mem + zmbMemMap.memsz);
//   RS_LOG("Mapped %p -> %p", zmbMemMap.mem, zmbMemMap.mem + zmbMemMap.memsz);
// }

/**
 * Maps the REAL memory segments and fills them with data from the memory image.
 * DOES NOT map the memory image file!!!! So you must call zmbMapMemFile first!!!
 */
void zmbFillFromMemFile()
{
  mapSegIfGoodArgs a = {true, false, false};
  zmbForEachMemSeg(mapSegIfGood, &a);
  ZMB_LOG("Real Mem Segments filled!");
  RS_LOG("Real Mem Segments filled!");
}

//////////////////////////////////////////////////////////


MemSeg * zmbMemSegForAddr(const void *addr)
{
  MemSeg search;
  search.start = (uintptr_t)addr;
  MemMap::iterator found = --(zmbMemMap.segs.upper_bound(search));
  if(found != zmbMemMap.segs.end() && zmbAddrInMemSeg(addr, &(*found)))
    return &(*found);
  else
    return NULL;
}

void * zmbTranslateAddrInSeg(const void *addr, MemSeg *seg)
{
  if (seg && zmbAddrInMemSeg(addr, seg)) return (void*)addr;
  return NULL;
//  uintptr_t offset = (uintptr_t)addr - seg->start;
//  uintptr_t addr_in_file = seg->offset + offset;
//  return (void *) &(zmbMemMap.mem[addr_in_file]);
}

void * zmbTranslateAddr(const void *addr)
{
  MemSeg* found = zmbMemSegForAddr(addr);
  if(found)
    return zmbTranslateAddrInSeg(addr, found);
  else
    return NULL;
}


void zmbForEachMemSeg(int (*cb_func)(MemSeg*, void*), void* arg)
{
  for(MemMap::iterator it = zmbMemMap.segs.begin();
      it != zmbMemMap.segs.end(); ++it)
  {
    if(cb_func(&(*it), arg) != 0) break;
  }
}


// struct findFullMemSegArgs {
//   MemSeg * ret;
//   const void * addr;
// };
// 
// int findMemSegForTransAddr(MemSeg * m, void * _args)
// {
//   findFullMemSegArgs * args = (findFullMemSegArgs *) _args;
//   uintptr_t offset = (uintptr_t)args->addr - (uintptr_t)zmbMemMap.mem;
//   if(m->offset <= offset && offset < m->offset + m->len)
//   {
//     args->ret = m;
//     return 1;
//   } 
//   return 0;
// }
// 
// MemSeg * memSegForTransAddr(const void * addr)
// {
//   findFullMemSegArgs a = {NULL, addr};
//   zmbForEachMemSeg(findMemSegForTransAddr, &a);  
//   return a.ret;
// }

//void * zmbUntranslateAddr(const void *addr)
//{
//  return (void*)addr;
//  MemSeg * seg = memSegForTransAddr(addr);
//  if (seg==NULL) return NULL;
//  uintptr_t offset = (uintptr_t)addr - (uintptr_t)zmbMemMap.mem - seg->offset;
//  return (void *)(seg->start + offset);
//}


//bool zmbAddrAlreadyTranslated(const void* addr)
//{
//  return zmbMemMap.mem <= addr && addr <= zmbMemMap.mem + zmbMemMap.memsz;
//}

extern "C" bool zmbIsSafeAddr(const void *addr)
{
  if(zmbIsZombieObject((const Object *)addr)) return true; 
  return zmbMemSegForAddr(addr)!=NULL;
}

bool zmbIsSafeAddr(const void *addr, unsigned size)
{
  if(zmbIsZombieObject((const Object *)addr)) return true; 
  uintptr_t start = (uintptr_t)addr;
  uintptr_t end = (((uintptr_t)addr) + size);
  do {
    MemSeg *m = zmbMemSegForAddr((void*)start);
    if(m == NULL) return false;
    start = m->end;
  } while(start < end);
  return true;
}


// mmap a memory region that will be safe once the old heap is pushed in
// void* zmbMmapSafe(size_t sz, int prot, int flags, int fd, off_t offset)
// {
//   //if(!zmbMemMap.init) zmbMemMapInit(map_file_name);
// 
//   BDS_List<u1*> kill;
//   int pgsize = getpagesize();
//   bool clear_area = false;
//   u1* new_area;
//   while(!clear_area) {
//     new_area = (u1*)mmap(NULL, sz, prot, flags, fd, offset);
//     if(new_area == (void*)-1) goto out;
//     clear_area = true;
//     for(u1* check = new_area; check < new_area+sz; check+=pgsize) {
//       MemSeg *m = zmbMemSegForAddr(check);
//       if(m != NULL && isCopySeg(m)) clear_area = false;
//     }
//     if(!clear_area) kill.push_front(new_area);
//   }
// out:
//   for(BDS_List<u1*>::iterator it = kill.begin(); it != kill.end(); ++it)
//   {
//     munmap((void*)*it, sz);
//     kill.erase(it);
//   }
//   return new_area;
// }


uintptr_t getEndAddrOfSeg(MemMap::iterator& it)
{
  MemSeg* m = &(*it);

  ++it;
  MemSeg* m2 = &(*it);

  // only the 2nd seg can have a blank name
  if(strlen(m2->name) != 0 && strcmp(m->name, m2->name) != 0)
  {
    --it; // start loop again for m2's segment
    return (uintptr_t)NULL;
  }

  ++it;
  MemSeg* m3 = &(*it);
  
  if(strcmp(m->name, m3->name) != 0)
  {
    --it; // start loop again from m3 because m2 is "" or name == m1
    return (uintptr_t)NULL;
  }

  if(strlen(m2->name) != 0)
    return m3->end; //if 2 was not blank then we are done.

  ++it;
  MemSeg* m4 = &(*it);
  
  if(strcmp(m->name, m4->name) != 0)
  {
    --it; // start loop from m4 because something funny happened with m1-m3
    return (uintptr_t)NULL;
  }

  return m4->end;
}

// it points to the first instance of a memory segment.
Elf_obj * getELFObjForSO(MemMap::iterator& it)
{
  MemSeg* m = &(*it);
  uintptr_t seg_start = m->start;
  uintptr_t seg_end   = getEndAddrOfSeg(it);
  void * mapped_addr = zmbTranslateAddrInSeg((void*)m->start, m);
  return elf_register(mapped_addr, seg_end - seg_start);
}

Elf_obj * zmbGetELFObjForSeg(const char * name)
{
  MemMap::iterator it = zmbMemMap.segs.begin();
  while(strstr(it->name, name)==NULL) { ++it; }
  return getELFObjForSO(it);
}


extern "C" char * zmbGetNameForOldSymbolValue(void * val)
{
  uintptr_t old_vtable_addr = (uintptr_t)zmbTranslateAddr(val)-8; // vtables are offset by 8

  for(MemMap::iterator it = zmbMemMap.segs.begin();
      it != zmbMemMap.segs.end(); ++it)
  {
    MemSeg& m = *it;
    static const char so[] = ".so";
    if(strstr(m.name, so) != NULL)
    {
      ZMB_LOG("Making ELF for : %s", m.name);
      Elf_obj * elf_obj = getELFObjForSO(it);
      if(elf_obj == NULL)
      {
        ZMB_LOG("Failed");
        continue;
      }
      for(Elf32_Sym * sym = elf_firstdsym(elf_obj);
          sym != NULL; sym = elf_nextdsym(elf_obj, sym))
      {
        if((uintptr_t)elf_obj->maddr + sym->st_value == old_vtable_addr)
        {
          char * ret = elf_dsymname(elf_obj, sym);
          elf_unregister(elf_obj);
          ZMB_LOG("Found %s", ret);
          return ret;
        }
      }
      elf_unregister(elf_obj);
    }
  }
  return NULL;
}


