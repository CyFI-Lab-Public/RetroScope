
#define _LARGEFILE64_SOURCE
 
#include <unistd.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/ptrace.h>
#include <sys/prctl.h>
#include <sys/wait.h>
#include <linux/user.h>
#include <dirent.h>
#include <string.h>
#include <signal.h>
#include <errno.h>
#include <stdio.h>
#include <fcntl.h>
#include <sys/stat.h>

#define DROID_VERSION
#ifdef DROID_VERSION
# include <android/log.h>
#endif

//#define LOG_ON
#undef LOG_ON

#ifdef DROID_VERSION
# define PMD_LOG(fmt, ...) __android_log_print(ANDROID_LOG_ERROR, "[pmd]", "%s: "fmt, __func__, ##__VA_ARGS__)
#else
# define PMD_LOG(fmt, ...) fprintf(stderr, "[ ] "__func__ fmt, ##__VA_ARGS__)
#endif
#define PMD_ERR(prefix, ...) PMD_LOG(prefix": %s\n", ##__VA_ARGS__, strerror(errno))




#define n_ARRAY(array) (sizeof(array)/sizeof(array[0]))

const char *skip_segments[] = {
 "/dev/kgsl-3d0"
};

const char *must_injects[] = {
 "/dev/ashmem"
};

static inline int 
is_in_string_list (const char* s,
                   const char* strings[],
                   unsigned n_strings)
{
  unsigned i;
  for(i = 0; i < n_strings; i++)
  {
    if(strcmp(strings[i], s) == 0) return 1;
  }
  return 0;
}

#define is_in_skip_segments(s) is_in_string_list(s, skip_segments, n_ARRAY(skip_segments))
#define is_in_must_inject(s)   is_in_string_list(s, must_injects, n_ARRAY(must_injects))


static inline size_t
get_tids(pid_t **const listptr, size_t *const sizeptr, const pid_t pid)
{
    char     dirname[64];
    DIR     *dir;
    pid_t   *list;
    size_t   size, used = 0;
    list = *listptr = NULL;
    size = *sizeptr = 0;

    sprintf(dirname, "/proc/%d/task/", (int)pid); 
    dir = opendir(dirname);
    if (!dir) {
        PMD_ERR("open task dir");
        return 0;
    }

    while (1) {
        struct dirent *ent;
        int            value;
        char           dummy;

        ent = readdir(dir);
        if (!ent)
            break;
        /* Parse TIDs. Ignore non-numeric entries. */
        if (sscanf(ent->d_name, "%d%c", &value, &dummy) != 1)
            continue;
        /* Ignore obviously invalid entries. */
        if (value < 1)
            continue;
        /* Make sure there is room for another TID. */
        if (used >= size) {
            size = (used | 127) + 128;
            list = realloc(list, size * sizeof list[0]);
            *listptr = list;
            *sizeptr = size;
        }
        /* Add to list. */
        list[used++] = (pid_t)value;
    }
    closedir(dir); 
    return used;
}


static inline void
check_new_tids(pid_t **const listptr, size_t *const usedptr, size_t *const sizeptr,
               const pid_t pid, const unsigned char let_run) 
{
  int used = *usedptr;
  int max = *sizeptr;
  pid_t *list = *listptr;

  pid_t *new_list;
  int new_max;
  int new_used = get_tids(&new_list , &new_max, pid);

  int i, j;
  for(i = 0; i < new_used; i++) {
    unsigned char found = 0;
    if(new_list[i] == pid) continue;
    for(j = 0; j < used; j++) {
      if(list[j] == new_list[i]) {
        found = 1;
        break;
      }
    }
    if(!found) {
      PMD_LOG("New Thread %d", new_list[i]);
      if(ptrace(PTRACE_ATTACH, new_list[i], NULL, NULL) == -1)
	  PMD_ERR("ptrace thread %d", new_list[i]);

      if(let_run)
        ptrace(PTRACE_CONT, new_list[i], NULL, NULL);
    }
  }
  
  free(list);
  *listptr = new_list;
  *usedptr = new_used;
  *sizeptr = new_max;
}

static inline pid_t wait_and_check_threads(pid_t pid, int* status, pid_t**const tids,
                                           size_t*const n_tids, size_t*const max_tids,
                                           unsigned char let_run)
{
//#define DEBUG_WAIT
#ifdef DEBUG_WAIT
  int cnt = 0;
#endif
  pid_t stopped = 0;
  int stat = 0;
  errno = 0;
  while(stopped == 0) {
    check_new_tids(tids, n_tids, max_tids, pid, let_run);
    stopped = waitpid((pid_t)-1, &stat, __WALL|WNOHANG);
#ifdef DEBUG_WAIT
    if(cnt++ == 10000) {
      PMD_ERR("In Wait %d %d", stopped, stat);
      cnt = 0;
    }
#endif
  }
#ifdef DEBUG_WAIT
  PMD_ERR("returning %d %d", stopped, stat);
#endif
  if(status != NULL)
    *status = stat;
  return stopped;
}



int getdata(pid_t child, long addr,
             char *str, int len)
{   char *laddr;
    int i, j;
    union u {
            long val;
            char chars[sizeof(long)];
    }data;
    i = 0;
    j = len / sizeof(long);
    laddr = str;
    while(i < j) {
        errno = 0;
        data.val = ptrace(PTRACE_PEEKDATA, child,
                          (void*)(addr + i * 4), NULL);
        if(errno != 0)
	{
          PMD_ERR("read 0x%lx", (addr + i * 4));
          return 1;
        }
        memcpy(laddr, data.chars, sizeof(long));
        ++i;
        laddr += sizeof(long);
    }
    j = len % sizeof(long);
    if(j != 0) {
        errno = 0;
        data.val = ptrace(PTRACE_PEEKDATA, child,
                          (void*)(addr + i * 4), NULL);
        if(errno != 0)
	{
          PMD_ERR("read 0x%lx", (addr + i * 4));
          return 1;
        }
        memcpy(laddr, data.chars, j);
    }
  return 0;
}
int putdata(pid_t child, long addr,
            const char *str, int len)
{   const char *laddr;
    int i, j;
    union u {
            long val;
            char chars[sizeof(long)];
    }data;
    i = 0;
    j = len / sizeof(long);
    laddr = str;
    while(i < j) {
        memcpy(data.chars, laddr, sizeof(long));
        if(ptrace(PTRACE_POKEDATA, child,
               (void*)(addr + i * 4), (void*)data.val) == -1)
          return 1;
        ++i;
        laddr += sizeof(long);
    }
    j = len % sizeof(long);
    if(j != 0) {
        memcpy(data.chars, laddr, j);
        if(ptrace(PTRACE_POKEDATA, child,
               (void*)(addr + i * 4), (void*)data.val) == -1)
          return 1;
    }
  return 0;
}




#define BUF_SZ 8192 // bytes
unsigned long buf[BUF_SZ/sizeof(unsigned long)];

static inline int dump_region_inject(int out_fd, pid_t pid, off64_t start, off64_t end,
                          pid_t**const tids, size_t*const n_tids, size_t*const max_tids)
{
  int ret = 0;
  struct pt_regs in_regs, out_regs, save_regs;
  ptrace(PTRACE_GETREGS, pid, NULL, &save_regs);
  ptrace(PTRACE_GETREGS, pid, NULL, &in_regs);

  while(start != end) {
      int read_sz = ((end - start) > BUF_SZ ? BUF_SZ : (end - start));
      int i;
      for(i = 0; i < read_sz / sizeof(unsigned long); i++) {
        in_regs.ARM_r3 = (unsigned long)start;
        ptrace(PTRACE_SETREGS, pid, NULL, &in_regs);
        ptrace(PTRACE_CONT, pid, NULL, NULL); // execute shell
        int status;
        pid_t p;
        do {
          p = wait(&status);
          //p = wait_and_check_threads(pid, &status, tids, n_tids, max_tids, 0);
        } while(p != pid);
        if(!(WIFSTOPPED(status)) || WSTOPSIG(status) != SIGTRAP) {
          PMD_LOG("Wait returned %d: status %s", p, strsignal(WSTOPSIG(status)));
          ret = 1;
          goto out;
        }
        ptrace(PTRACE_GETREGS, pid, NULL, &out_regs);
#ifdef LOG_ON
        if(out_regs.ARM_pc != in_regs.ARM_pc + 4)
        {
          PMD_LOG("PC Skip? %lx %lx", out_regs.ARM_pc, in_regs.ARM_pc);
          ret = 1;
          goto out;
        }
#endif
        buf[i] = out_regs.ARM_r2;
	start += sizeof(unsigned long);
      }
      if(write(out_fd, buf, read_sz) != read_sz)
      {
        PMD_ERR("write 0x%llx %d", lseek64(out_fd, 0, SEEK_CUR), read_sz);
        ret = 2;
        goto out;
      }
  }
out:
  ptrace(PTRACE_SETREGS, pid, NULL, &save_regs);
  return ret;
}

static inline int dump_region_ptrace(pid_t child, int out_fd, off64_t start, off64_t end)
{
    while(start != end) {
        register int read_sz = ((end - start) > BUF_SZ ? BUF_SZ : (end - start));
        if(getdata(child, start, (char*)buf, read_sz) != 0) 
        {
          PMD_ERR("getdata 0x%llx %d", start, read_sz);
          return 1;
        }
        if(write(out_fd, buf, read_sz) != read_sz)
        {
          PMD_ERR("write 0x%llx %d", lseek64(out_fd, 0, SEEK_CUR), read_sz);
          return 2;
        }
        start += read_sz;
    }
    return 0;
}

static inline int dump_region_fd(int out_fd, int in_fd, off64_t start, off64_t end)
{
    if(lseek64(in_fd, start, SEEK_SET) != start)
    {
      PMD_ERR("seek");
      return -1;
    }

    while(start != end) {
        register int read_sz = ((end - start) > BUF_SZ ? BUF_SZ : (end - start));
        if(read(in_fd, buf, read_sz) != read_sz) 
        {
          PMD_ERR("read 0x%llx %d", lseek64(in_fd, 0, SEEK_CUR), read_sz);
          return 1;
        }
        if(write(out_fd, buf, read_sz) != read_sz)
        {
          PMD_ERR("write 0x%llx %d", lseek64(out_fd, 0, SEEK_CUR), read_sz);
          return 2;
        }
        start += read_sz;
    }
#undef BUF_SZ
    return 0;
}

#define ARM_BP_STR   "\xf0\x01\xf0\xe7"
#define THUMB_BP_STR "\xbe\xbe\xbe\xbe"

static inline void dump_mem(pid_t pid, int mem, int mem_file, FILE* maps, FILE* map_file,
                            pid_t**const tids, size_t*const n_tids, size_t*const max_tids)
{
  char buf[BUFSIZ + 1];
  char name[BUFSIZ + 1];
  off64_t start, end, pos;
#ifdef LOG_ON
  int regions = 0;
#endif
  
#define ARM_LINUX_BP
  const char shell[] = "\x00\x20\x93\xe5"  // ldr r2, [r3]
#ifdef ARM_LINUX_BP
                       ARM_BP_STR; // ARM breakpoint
#endif
#ifdef THUMB_BP
                       THUMB_BP_STR; // Thumb breakpoint
#endif

  char save[sizeof(shell)];

  // insert the shell code
  struct pt_regs regs;
  ptrace(PTRACE_GETREGS, pid, NULL, &regs);
  // save old
  if(getdata(pid, regs.ARM_pc, save, sizeof(save)) != 0)
    return;
  // write shell
  if(putdata(pid, regs.ARM_pc, shell, sizeof(shell)) != 0)
    return;

  while(fgets(buf, BUFSIZ, maps))
  {
    name[0] = '\0';
    sscanf(buf, "%llx-%llx %*s %*x %*x:%*x %*u %s\n", &start, &end, name);

    if(is_in_skip_segments(name)) continue;

    pos = lseek64(mem_file, 0, SEEK_CUR);
    
    if(is_in_must_inject(name))
    {
      if(dump_region_ptrace(pid, mem_file, start, end))
      {
        PMD_ERR("dump ptrace: %llx->%llx %s", start, end, name);
        continue;
      }
    }
    // first try to read from proc/mem (WAY MUCH FASTER)
    else if(dump_region_fd(mem_file, mem, start, end))
    {
      PMD_LOG("injecting for: %llx->%llx %s", start, end, name);
      // if that doesn't work, then try inject
      if(dump_region_inject(mem_file, pid, start, end, tids, n_tids, max_tids))
      {
        PMD_ERR("dump: %llx->%llx %s", start, end, name);
        continue;
      }
    }

    fprintf(map_file, "%llx->%llx->%llx %s\n", start, end, pos, name);

#ifdef LOG_ON
    regions++;
    PMD_LOG("dump [%d] = %s = %llx", regions, buf, pos);
#endif
  }

  ptrace(PTRACE_SETREGS, pid, NULL, &regs);
  // write save
  if(putdata(pid, regs.ARM_pc, save, sizeof(save)) != 0)
    return;

#ifdef LOG_ON
  PMD_LOG("Done: dumped %d regions.\n", regions);
#else
  PMD_LOG("Done.\n");
#endif
}

uintptr_t find_start_of_map(FILE* maps, char* image)
{
  char buf[BUFSIZ + 1];
  char name[BUFSIZ + 1];
  char perms[5];
  uintptr_t start;
  while(fgets(buf, BUFSIZ, maps))
  {
    name[0] = '\0';
    sscanf(buf, "%x-%*x %s %*x %*x:%*x %*u %s\n", &start, perms, name);
    if(strstr(name, image) != NULL && strstr(perms, "r-x") == perms) return start;
  }
  return 0;
}


void hold_for_break(pid_t pid, pid_t**const tids, size_t*const n_tids, size_t*const max_tids,
                    FILE* maps, char* image, uintptr_t offset)
{
    uintptr_t addr = find_start_of_map(maps, image);
    rewind(maps);
    if(addr == 0)
    {
      PMD_LOG("ERROR! No matching map for %s", image);
      return;
    }
    addr+=offset;

    char bp[] =
#ifdef ARM_LINUX_BP
                       ARM_BP_STR; // ARM breakpoint
#endif
#ifdef THUMB_BP
                       THUMB_BP_STR; // Thumb breakpoint
#endif
    
    char save[sizeof(bp)];
    char check[sizeof(bp)];

    // insert the BP
    // save old
    if(getdata(pid, addr, save, 4) != 0)
      return;
    // write shell
    if(putdata(pid, addr, bp, 4) != 0)
      return;

    if(getdata(pid, addr, check, 4) != 0)
      return;

    if(memcmp(bp, check, 4) != 0) {
      PMD_ERR("NOT SETTING BP!!!!!!!!!");
      return;
    }

    PMD_LOG("Put BP on 0x%x", addr);
    PMD_LOG("Now holding for BP....");

    kill(pid, SIGCONT); 

    unsigned char hold = 1;
    pid_t stopped = 0;
    struct pt_regs regs;
    while(hold)
    {
      int status = 0;
      stopped = wait_and_check_threads(pid, &status, tids, n_tids, max_tids, 1); 
      if(stopped == -1) {
        PMD_ERR("wait?");
        return;
      }
      ptrace(PTRACE_GETREGS, stopped, NULL, &regs);
      if((WIFSTOPPED(status) && WSTOPSIG(status) == SIGTRAP) ||
         (addr - 32 <= regs.ARM_pc && regs.ARM_pc <= addr + 32))
      {
        hold = 0;
#ifdef LOG_ON
        PMD_LOG("Hit BP %d: pc 0x%lx status %s", stopped,
                  regs.ARM_pc, strsignal(WSTOPSIG(status)));
#else
        PMD_LOG("Hit our BP!");
#endif
      }
      else
      {
        int sig = 0;
        if (WIFEXITED(status)) {
#ifdef LOG_ON
          PMD_LOG("Wait Ret: Exited %d: pc 0x%lx status %d", stopped,
                  regs.ARM_pc, WEXITSTATUS(status));
#endif
        } else if (WIFSIGNALED(status)) {
#ifdef LOG_ON
          PMD_LOG("Wait Ret: Killed by signal %d: pc 0x%lx status %s", stopped,
                  regs.ARM_pc, strsignal(WTERMSIG(status)));
#endif
        } else if (WIFSTOPPED(status)) {
#ifdef LOG_ON
          PMD_LOG("Wait Ret: Stopped by signal %d: pc 0x%lx status %s", stopped,
                  regs.ARM_pc, strsignal(WSTOPSIG(status)));
#endif
        }
        ptrace(PTRACE_CONT, stopped, NULL, (void*)(sig));
      }
    }


    regs.ARM_pc = addr;
    ptrace(PTRACE_SETREGS, stopped, NULL, &regs);
    // write save
    putdata(pid, addr, save, 4);

    kill(pid, SIGSTOP); // stop other threads

    do {
      stopped = wait_and_check_threads(pid, NULL, tids, n_tids, max_tids, 0);
    } while (stopped != pid);
}


int main(int argc, char *argv[])
{
    FILE *maps = NULL, *map_file = NULL;
    int mem = -1, mem_file = -1;
    int error = EXIT_SUCCESS;
    pid_t pid;
    pid_t *tid = 0;
    size_t tids = 0;
    size_t tids_max = 0;
    int t;
    char path[BUFSIZ];

    if(argc < 3 || argc > 6) {
        PMD_LOG("usage: %s pid out_dir [-s <image> <offset>]\n", argv[0]);
        return EXIT_FAILURE;
    }
    
    pid = strtol(argv[1], NULL, 10);
 
    snprintf(path, sizeof(path), "%s/%d.map", argv[2], pid);
    map_file = fopen(path, "w");
    if(!map_file) {
      PMD_ERR("%s",path);
      error = EXIT_FAILURE;
      goto clean_up;
    }

    snprintf(path, sizeof(path), "%s/%d.mem", argv[2], pid);
    mem_file = open(path, O_WRONLY | O_CREAT, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
    if(mem_file < 0) {
      PMD_ERR("%s",path);
      error = EXIT_FAILURE;
      goto clean_up;
    }

    if(ptrace(PTRACE_ATTACH, pid, NULL, NULL) == -1) {
      PMD_ERR("ptrace");
      error = EXIT_FAILURE;
      goto clean_up;
    }
    check_new_tids(&tid, &tids, &tids_max, pid, 0);
 
    snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    maps = fopen(path, "r");
    if(!maps) {
      PMD_ERR("%s",path);
      error = EXIT_FAILURE;
      goto clean_up;
    }

    snprintf(path, sizeof(path), "/proc/%d/mem", pid);
    mem = open(path, O_RDONLY);
    if(mem < 0) {
      PMD_ERR("%s",path);
      error = EXIT_FAILURE;
      goto clean_up;
    }

    if(argc > 3) {
      hold_for_break(pid, &tid, &tids,&tids_max, maps, argv[4], strtoll(argv[5],NULL, 0));
    }

    dump_mem(pid, mem, mem_file, maps, map_file, &tid, &tids,&tids_max);
   
clean_up:     
    ptrace(PTRACE_DETACH, pid, NULL, NULL);
    for (t = 0; t < tids; t++) {
      if(tid[t] != pid)
        ptrace(PTRACE_DETACH, tid[t], NULL, NULL);
    }
    kill(pid, SIGCONT);

    if(mem >= 0)
        close(mem);
    if(mem_file >= 0)
        close(mem_file);
    if(maps)
        fclose(maps);
    if(map_file)
        fclose(map_file);

    return error;
}
