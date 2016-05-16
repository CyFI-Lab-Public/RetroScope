#ifndef ZOMBIE_APPMAININIT_H_
#define ZOMBIE_APPMAININIT_H_

#define ZMB_EXTERNAL
#include "MemMap.h"
#include "NewMemMap.h"
#include <sys/mman.h>
#include <fcntl.h>
#undef ZMB_EXTERNAL


static inline int fill_me(char * buf, size_t sz, int fd)
{
  size_t i = 0;
  char c;
  while(read(fd, &c, 1)) {
    if(c == '\n')
      break;
    buf[i++] = c;
    if(i == sz-1)
      break;
  }
  buf[i] = '\0';
  return i;
}

static inline void zmbAppMainBlindInit()
{
  int new_file = open("/proc/self/maps", O_RDONLY);
  if(new_file == -1)
  {
    ZMB_LOG("Cannot open map file: /proc/self/maps!");
    return;
  }
  int map_file = open(map_file_name, O_RDONLY);
  if(map_file == -1)
  {
    ZMB_LOG("Cannot open map file: %s : %s!", map_file_name, strerror(errno));
    return;
  }

#define BUFFER_SZ (1024)
#define ZMB_PRINT_ONCE
#ifdef ZMB_PRINT_ONCE
  {
    char line[BUFFER_SZ];
    while(fill_me(line, BUFFER_SZ, new_file) > 0)
    {
      MemSeg new_seg = MemSegFromProcLine(line);
      ZMB_LOG("%x : %x : %s", new_seg.start, new_seg.end, new_seg.name);
    }
  }
#endif

  unsigned memFileSize=0;
  char map_line[BUFFER_SZ];
  memset(map_line, 0, BUFFER_SZ);
  while(fill_me(map_line, BUFFER_SZ, map_file) > 0)
  {
    MemSeg map_seg = MemSegFromMapLine(map_line);
    // print what we will kill
    char line[BUFFER_SZ];
    memset(line, 0, BUFFER_SZ);
    while(fill_me(line, BUFFER_SZ, new_file) > 0)
    {
      MemSeg new_seg = MemSegFromProcLine(line);
      if(new_seg.start < map_seg.end && map_seg.start < new_seg.end) // overlap
      {
        ZMB_LOG("Kill: %x : %x : %s", new_seg.start, new_seg.end, new_seg.name);
        munmap((void*)new_seg.start, new_seg.len); // just remove whatever is there
      }
      memset(line, 0, BUFFER_SZ);
    }
    lseek(new_file, 0, SEEK_SET);
    void * ret = mmap((void*)map_seg.start, map_seg.len, PROT_READ|PROT_WRITE,
                        MAP_PRIVATE|MAP_FIXED|MAP_ANONYMOUS, -1, 0);
    if(ret != (void*)map_seg.start)
      ZMB_LOG("MMAP ERROR: %x : %x : %s", map_seg.start, map_seg.end, map_seg.name);
    memFileSize += map_seg.len;
    memset(map_line, 0, BUFFER_SZ);
  }

  close(new_file);
  close(map_file);
#undef BUFFER_SZ
  ZMB_LOG("Mapped %s", map_file_name);
}

#endif // ZOMBIE_APPMAININIT_H_
