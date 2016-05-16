/* Copyright (C) 2008 Red Hat, Inc.
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

#include <errno.h>
#include <error.h>
#include <fcntl.h>
#include <gelf.h>
#include <stdio.h>
#include <stdlib.h>

int
main (int argc, char *argv[])
{
  if (argc < 2)
    error (1, 0, "Usage: %s FILE OFFSET", argv[0]);

  /* Set the ELF version.  */
  elf_version (EV_CURRENT);

  /* Open the archive.  */
  int fd = open (argv[1], O_RDONLY);
  if (fd < 0)
    error (1, errno, "cannot open '%s'", argv[1]);

  Elf *elf = elf_begin (fd, ELF_C_READ, NULL);
  if (elf == NULL)
    error (2, 0, "elf_begin: %s", elf_errmsg (-1));

  Elf_Scn *scn = gelf_offscn (elf, strtoull (argv[2], NULL, 0));
  if (scn == NULL)
    error (3, 0, "gelf_offscn: %s", elf_errmsg (-1));

  elf_end (elf);
  return 0;
}
