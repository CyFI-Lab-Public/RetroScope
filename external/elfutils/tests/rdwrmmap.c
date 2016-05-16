#include <errno.h>
#include <error.h>
#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <libelf.h>

int
main (int argc __attribute__ ((unused)), char *argv[])
{
  int fd = open (argv[1], O_RDWR);
  if (fd < 0)
    error (2, errno, "open: %s", argv[1]);

  if (elf_version (EV_CURRENT) == EV_NONE)
    error (1, 0, "libelf version mismatch");

  Elf *elf = elf_begin (fd, ELF_C_RDWR_MMAP, NULL);
  if (elf == NULL)
    error (1, 0, "elf_begin: %s", elf_errmsg (-1));

  if (elf_update (elf, ELF_C_WRITE) < 0)
    error (1, 0, "elf_update: %s", elf_errmsg (-1));

  elf_end (elf);
  close (fd);

  return 0;
}
