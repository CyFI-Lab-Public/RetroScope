/* Copyright (C) 2002, 2004, 2005, 2007 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2002.

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

#include <fcntl.h>
#include <libelf.h>
#include ELFUTILS_HEADER(dw)
#include <stdio.h>
#include <unistd.h>


int
main (int argc, char *argv[])
{
  int result = 0;
  int cnt;

  for (cnt = 1; cnt < argc; ++cnt)
    {
      int fd = open (argv[cnt], O_RDONLY);

      Dwarf *dbg = dwarf_begin (fd, DWARF_C_READ);
      if (dbg == NULL)
	{
	  printf ("%s not usable\n", argv[cnt]);
	  result = 1;
	  if (fd != -1)
	    close (fd);
	  continue;
	}

      Dwarf_Off o = 0;
      Dwarf_Off ncu;
      Dwarf_Off ao;
      size_t cuhl;
      uint8_t asz;
      uint8_t osz;
      while (dwarf_nextcu (dbg, o, &ncu, &cuhl, &ao, &asz, &osz) == 0)
	{
	  printf ("cuhl = %zu, o = %llu, asz = %hhu, osz = %hhu, ncu = %llu\n",
		  cuhl, (unsigned long long int) ao,
		  asz, osz, (unsigned long long int) ncu);

	  Dwarf_Die die_mem;
	  Dwarf_Die *die = dwarf_offdie (dbg, o + cuhl, &die_mem);
	  if (die == NULL)
	    {
	      printf ("%s: cannot get CU die\n", argv[cnt]);
	      result = 1;
	      break;
	    }

	  Dwarf_Files *files;
	  size_t nfiles;
	  if (dwarf_getsrcfiles (die, &files, &nfiles) != 0)
	    {
	      printf ("%s: cannot get files\n", argv[cnt]);
	      result = 1;
	      break;
	    }

	  const char *const *dirs;
	  size_t ndirs;
	  if (dwarf_getsrcdirs (files, &dirs, &ndirs) != 0)
	    {
	      printf ("%s: cannot get include directories\n", argv[cnt]);
	      result = 1;
	      break;
	    }

	  if (dirs[0] == NULL)
	    puts (" dirs[0] = (null)");
	  else
	    printf (" dirs[0] = \"%s\"\n", dirs[0]);
	  for (size_t i = 1; i < ndirs; ++i)
	    printf (" dirs[%zu] = \"%s\"\n", i, dirs[i]);

	  for (size_t i = 0; i < nfiles; ++i)
	    printf (" file[%zu] = \"%s\"\n", i,
		    dwarf_filesrc (files, i, NULL, NULL));

	  o = ncu;
	}

      dwarf_end (dbg);
      close (fd);
    }

  return result;
}
