/* Copyright (C) 2000, 2002, 2005 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2000.

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

#include <nlist.h>
#include <stdio.h>
#include <stdlib.h>


int var = 1;

int bss;


int
foo (int a)
{
  return a;
}

int
main (int argc, char *argv[] __attribute__ ((unused)))
{
  struct nlist nl[6] =
  {
    [0] = { .n_name = "var" },
    [1] = { .n_name = "bss" },
    [2] = { .n_name = "main" },
    [3] = { .n_name = "foo" },
    [4] = { .n_name = "not-there" },
    [5] = { .n_name = NULL },
  };
  int cnt;
  int result = 0;

  if (nlist (".libs/test-nlist", nl) != 0
      && nlist ("./test-nlist", nl) != 0)
    {
      puts ("nlist failed");
      exit (1);
    }

  for (cnt = 0; nl[cnt].n_name != NULL; ++cnt)
    {
      if (argc > 1)
	/* For debugging.  */
	printf ("nl[%d].n_name = \"%s\"\n"
		"nl[%d].n_value = %ld\n"
		"nl[%d].n_scnum = %d\n"
		"nl[%d].n_type = %u\n"
		"nl[%d].n_sclass = %d\n"
		"nl[%d].n_numaux = %d\n\n",
		cnt, nl[cnt].n_name,
		cnt, nl[cnt].n_value,
		cnt, nl[cnt].n_scnum,
		cnt, nl[cnt].n_type,
		cnt, nl[cnt].n_sclass,
		cnt, nl[cnt].n_numaux);

      if ((cnt != 4 && nl[cnt].n_value == 0 && nl[cnt].n_scnum == 0
	   && nl[cnt].n_type == 0 && nl[cnt].n_sclass == 0
	   && nl[cnt].n_numaux == 0)
	  || (cnt == 4 && (nl[cnt].n_value != 0 || nl[cnt].n_scnum != 0
			   || nl[cnt].n_type != 0 || nl[cnt].n_sclass != 0
			   || nl[cnt].n_numaux != 0)))
	result = 1;
    }

  return foo (result);
}
