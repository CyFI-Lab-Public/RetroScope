/* Copyright (C) 2002, 2004, 2005 Red Hat, Inc.
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
#include ELFUTILS_HEADER(asm)
#include <libelf.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/wait.h>


static const char fname[] = "asm-tst4-out.o";


int
main (void)
{
  AsmCtx_t *ctx;
  int result = 0;
  size_t cnt;

  elf_version (EV_CURRENT);

  Ebl *ebl = ebl_openbackend_machine (EM_386);
  if (ebl == NULL)
    {
      puts ("cannot open backend library");
      return 1;
    }

  ctx = asm_begin (fname, ebl, false);
  if (ctx == NULL)
    {
      printf ("cannot create assembler context: %s\n", asm_errmsg (-1));
      return 1;
    }

  /* Create 66000 sections.  */
  for (cnt = 0; cnt < 66000; ++cnt)
    {
      char buf[20];
      AsmScn_t *scn;

      /* Create a unique name.  */
      snprintf (buf, sizeof (buf), ".data.%Zu", cnt);

      /* Create the section.  */
      scn = asm_newscn (ctx, buf, SHT_PROGBITS, SHF_ALLOC | SHF_WRITE);
      if (scn == NULL)
	{
	  printf ("cannot create section \"%s\" in output file: %s\n",
		  buf, asm_errmsg (-1));
	  asm_abort (ctx);
	  return 1;
	}

      /* Add some content.  */
      if (asm_adduint32 (scn, cnt) != 0)
	{
	  printf ("cannot create content of section \"%s\": %s\n",
		  buf, asm_errmsg (-1));
	  asm_abort (ctx);
	  return 1;
	}
    }

  /* Create the output file.  */
  if (asm_end (ctx) != 0)
    {
      printf ("cannot create output file: %s\n", asm_errmsg (-1));
      asm_abort (ctx);
      return 1;
    }

  if (result == 0)
    result = WEXITSTATUS (system ("\
env LD_LIBRARY_PATH=../libelf ../src/elflint -q asm-tst4-out.o"));

  /* We don't need the file anymore.  */
  unlink (fname);

  ebl_closebackend (ebl);

  return result;
}
