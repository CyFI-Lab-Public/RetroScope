/* External ELF types.
   Copyright (C) 1998, 1999, 2000, 2002, 2007 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Contributed by Ulrich Drepper <drepper@redhat.com>, 1998.

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

#ifndef _EXTTYPES_H
#define	_EXTTYPES_H 1

/* Integral types.  */
typedef char Elf32_Ext_Addr[ELF32_FSZ_ADDR];
typedef char Elf32_Ext_Off[ELF32_FSZ_OFF];
typedef char Elf32_Ext_Half[ELF32_FSZ_HALF];
typedef char Elf32_Ext_Sword[ELF32_FSZ_SWORD];
typedef char Elf32_Ext_Word[ELF32_FSZ_WORD];
typedef char Elf32_Ext_Sxword[ELF32_FSZ_SXWORD];
typedef char Elf32_Ext_Xword[ELF32_FSZ_XWORD];

typedef char Elf64_Ext_Addr[ELF64_FSZ_ADDR];
typedef char Elf64_Ext_Off[ELF64_FSZ_OFF];
typedef char Elf64_Ext_Half[ELF64_FSZ_HALF];
typedef char Elf64_Ext_Sword[ELF64_FSZ_SWORD];
typedef char Elf64_Ext_Word[ELF64_FSZ_WORD];
typedef char Elf64_Ext_Sxword[ELF64_FSZ_SXWORD];
typedef char Elf64_Ext_Xword[ELF64_FSZ_XWORD];


/* Define the composed types.  */
#define START(Bits, Name, EName) typedef struct {
#define END(Bits, Name) } ElfW2(Bits, Name)
#define TYPE_NAME(Type, Name) Type Name;
#define TYPE_EXTRA(Text) Text
#define TYPE_XLATE(Text)

/* Get the abstract definitions. */
#include "abstract.h"

/* And define the types.  */
Ehdr32 (Ext_);
Phdr32 (Ext_);
Shdr32 (Ext_);
Sym32 (Ext_);
Rel32 (Ext_);
Rela32 (Ext_);
Note32 (Ext_);
Dyn32 (Ext_);
Verdef32 (Ext_);
Verdaux32 (Ext_);
Verneed32 (Ext_);
Vernaux32 (Ext_);
Syminfo32 (Ext_);
Move32 (Ext_);
auxv_t32 (Ext_);

Ehdr64 (Ext_);
Phdr64 (Ext_);
Shdr64 (Ext_);
Sym64 (Ext_);
Rel64 (Ext_);
Rela64 (Ext_);
Note64 (Ext_);
Dyn64 (Ext_);
Verdef64 (Ext_);
Verdaux64 (Ext_);
Verneed64 (Ext_);
Vernaux64 (Ext_);
Syminfo64 (Ext_);
Move64 (Ext_);
auxv_t64 (Ext_);

#undef START
#undef END
#undef TYPE_NAME
#undef TYPE_EXTRA
#undef TYPE_XLATE

#endif	/* exttypes.h */
