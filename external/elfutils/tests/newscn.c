/* Copyright (C) 1999, 2000, 2001, 2002, 2005 Red Hat, Inc.
   This file is part of Red Hat elfutils.

   Red Hat elfutils is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by the
   Free Software Foundation; version 2 of the License.

   Red Hat elfutils is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with Red Hat elfutils; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301 USA.

   Red Hat elfutils is an included package of the Open Invention Network.
   An included package of the Open Invention Network is a package for which
   Open Invention Network licensees cross-license their patents.  No patent
   license is granted, either expressly or impliedly, by designation as an
   included package.  Should you wish to participate in the Open Invention
   Network licensing program, please visit www.openinventionnetwork.com
   <http://www.openinventionnetwork.com>.  */

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include <assert.h>
#include <fcntl.h>
#include <libelf.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>


int
main (void)
{
  Elf *elf;
  int fd;
  Elf_Scn *section;

  if (elf_version (EV_CURRENT) == EV_NONE)
    {
      fprintf (stderr, "library fd of date\n");
      exit (1);
    }

  char name[] = "test.XXXXXX";
  fd = mkstemp (name);
  if (fd < 0)
    {
      fprintf (stderr, "Failed to open fdput file: %s\n", name);
      exit (1);
    }
  unlink (name);

  elf = elf_begin (fd, ELF_C_WRITE, NULL);
  if (elf == NULL)
    {
      fprintf (stderr, "Failed to elf_begin fdput file: %s\n", name);
      exit (1);
    }

  section = elf_newscn (elf);
  section = elf_nextscn (elf, section);
  assert (section == NULL);

  elf_end (elf);
  close (fd);

  return 0;
}
