/* Copyright (C) 2002, 2005 Red Hat, Inc.
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


static int globcnt;

static int
callback (Dwarf *dbg, Dwarf_Global *gl, void *arg __attribute__ ((unused)))
{
  int result = DWARF_CB_OK;

  printf (" [%2d] \"%s\", die: %llu, cu: %llu\n",
	  globcnt++, gl->name, (unsigned long long int) gl->die_offset,
	  (unsigned long long int) gl->cu_offset);

  Dwarf_Die cu_die;
  const char *cuname;
  if (dwarf_offdie (dbg, gl->cu_offset, &cu_die) == NULL
      || (cuname = dwarf_diename (&cu_die)) == NULL)
    {
      puts ("failed to get CU die");
      result = DWARF_CB_ABORT;
    }
  else
    printf ("CU name: \"%s\"\n", cuname);

  const char *diename;
  Dwarf_Die die;
  if (dwarf_offdie (dbg, gl->die_offset, &die) == NULL
      || (diename = dwarf_diename (&die)) == NULL)
    {
      puts ("failed to get object die");
      result = DWARF_CB_ABORT;
    }
  else
    printf ("object name: \"%s\"\n", diename);

  return result;
}


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
	  printf ("%s not usable: %s\n", argv[cnt], dwarf_errmsg (-1));
	  result = 1;
	  close (fd);
	  continue;
	}

      globcnt = 0;

      if (dwarf_getpubnames (dbg, callback, NULL, 0) != 0)
	{
	  printf ("dwarf_get_pubnames didn't return zero: %s\n",
		  dwarf_errmsg (-1));
	  result = 1;
	}

      dwarf_end (dbg);
      close (fd);
    }

  return result;
}
