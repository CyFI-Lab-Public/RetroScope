/* Return the size of an object file type.
   Copyright (C) 1998, 1999, 2000, 2002, 2007 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 1998.

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

   In addition, as a special exception, Red Hat, Inc. gives You the
   additional right to link the code of Red Hat elfutils with code licensed
   under any Open Source Initiative certified open source license
   (http://www.opensource.org/licenses/index.php) which requires the
   distribution of source code with any binary distribution and to
   distribute linked combinations of the two.  Non-GPL Code permitted under
   this exception must only link to the code of Red Hat elfutils through
   those well defined interfaces identified in the file named EXCEPTION
   found in the source code files (the "Approved Interfaces").  The files
   of Non-GPL Code may instantiate templates or use macros or inline
   functions from the Approved Interfaces without causing the resulting
   work to be covered by the GNU General Public License.  Only Red Hat,
   Inc. may make changes or additions to the list of Approved Interfaces.
   Red Hat's grant of this exception is conditioned upon your not adding
   any new exceptions.  If you wish to add a new Approved Interface or
   exception, please contact Red Hat.  You must obey the GNU General Public
   License in all respects for all of the Red Hat elfutils code and other
   code used in conjunction with Red Hat elfutils except the Non-GPL Code
   covered by this exception.  If you modify this file, you may extend this
   exception to your version of the file, but you are not obligated to do
   so.  If you do not wish to provide this exception without modification,
   you must delete this exception statement from your version and license
   this file solely under the GPL without exception.

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

#include <gelf.h>
#include <stddef.h>

#include "libelfP.h"


/* These are the sizes for all the known types.  */
const size_t __libelf_type_sizes[EV_NUM - 1][ELFCLASSNUM - 1][ELF_T_NUM] =
{
  /* We have no entry for EV_NONE siince we have to set an error.  */
  [EV_CURRENT - 1] = {
    [ELFCLASS32 - 1] = {
#define TYPE_SIZES(LIBELFBITS) \
      [ELF_T_ADDR]	= ELFW2(LIBELFBITS, FSZ_ADDR),			      \
      [ELF_T_OFF]	= ELFW2(LIBELFBITS, FSZ_OFF),			      \
      [ELF_T_BYTE]	= 1,						      \
      [ELF_T_HALF]	= ELFW2(LIBELFBITS, FSZ_HALF),			      \
      [ELF_T_WORD]	= ELFW2(LIBELFBITS, FSZ_WORD),			      \
      [ELF_T_SWORD]	= ELFW2(LIBELFBITS, FSZ_SWORD),			      \
      [ELF_T_XWORD]	= ELFW2(LIBELFBITS, FSZ_XWORD),			      \
      [ELF_T_SXWORD]	= ELFW2(LIBELFBITS, FSZ_SXWORD),		      \
      [ELF_T_EHDR]	= sizeof (ElfW2(LIBELFBITS, Ext_Ehdr)),		      \
      [ELF_T_SHDR]	= sizeof (ElfW2(LIBELFBITS, Ext_Shdr)),		      \
      [ELF_T_SYM]	= sizeof (ElfW2(LIBELFBITS, Ext_Sym)),		      \
      [ELF_T_REL]	= sizeof (ElfW2(LIBELFBITS, Ext_Rel)),		      \
      [ELF_T_RELA]	= sizeof (ElfW2(LIBELFBITS, Ext_Rela)),		      \
      [ELF_T_PHDR]	= sizeof (ElfW2(LIBELFBITS, Ext_Phdr)),		      \
      [ELF_T_DYN]	= sizeof (ElfW2(LIBELFBITS, Ext_Dyn)),		      \
      [ELF_T_VDEF]	= sizeof (ElfW2(LIBELFBITS, Ext_Verdef)),	      \
      [ELF_T_VDAUX]	= sizeof (ElfW2(LIBELFBITS, Ext_Verdaux)),	      \
      [ELF_T_VNEED]	= sizeof (ElfW2(LIBELFBITS, Ext_Verneed)),	      \
      [ELF_T_VNAUX]	= sizeof (ElfW2(LIBELFBITS, Ext_Vernaux)),	      \
      [ELF_T_NHDR]	= sizeof (ElfW2(LIBELFBITS, Ext_Nhdr)),		      \
      [ELF_T_SYMINFO]	= sizeof (ElfW2(LIBELFBITS, Ext_Syminfo)),	      \
      [ELF_T_MOVE]	= sizeof (ElfW2(LIBELFBITS, Ext_Move)),		      \
      [ELF_T_AUXV]	= sizeof (ElfW2(LIBELFBITS, Ext_auxv_t))
      TYPE_SIZES (32)
    },
    [ELFCLASS64 - 1] = {
      TYPE_SIZES (64)
    }
  }
};


size_t
gelf_fsize (elf, type, count, version)
     Elf *elf;
     Elf_Type type;
     size_t count;
     unsigned int version;
{
  /* We do not have differences between file and memory sizes.  Better
     not since otherwise `mmap' would not work.  */
  if (elf == NULL)
    return 0;

  if (version == EV_NONE || version >= EV_NUM)
    {
      __libelf_seterrno (ELF_E_UNKNOWN_VERSION);
      return 0;
    }

  if (type >= ELF_T_NUM)
    {
      __libelf_seterrno (ELF_E_UNKNOWN_TYPE);
      return 0;
    }

#if EV_NUM != 2
  return count * __libelf_type_sizes[version - 1][elf->class - 1][type];
#else
  return count * __libelf_type_sizes[0][elf->class - 1][type];
#endif
}
INTDEF(gelf_fsize)
