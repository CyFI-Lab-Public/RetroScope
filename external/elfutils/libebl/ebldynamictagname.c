/* Return dynamic tag name.
   Copyright (C) 2001, 2002, 2006, 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2001.

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

#include <inttypes.h>
#include <stdio.h>
#include <libeblP.h>


const char *
ebl_dynamic_tag_name (ebl, tag, buf, len)
     Ebl *ebl;
     int64_t tag;
     char *buf;
     size_t len;
{
  const char *res = ebl != NULL ? ebl->dynamic_tag_name (tag, buf, len) : NULL;

  if (res == NULL)
    {
      if (tag >= 0 && tag < DT_NUM)
	{
	  static const char *stdtags[] =
	    {
	      "NULL", "NEEDED", "PLTRELSZ", "PLTGOT", "HASH", "STRTAB",
	      "SYMTAB", "RELA", "RELASZ", "RELAENT", "STRSZ", "SYMENT",
	      "INIT", "FINI", "SONAME", "RPATH", "SYMBOLIC", "REL", "RELSZ",
	      "RELENT", "PLTREL", "DEBUG", "TEXTREL", "JMPREL", "BIND_NOW",
	      "INIT_ARRAY", "FINI_ARRAY", "INIT_ARRAYSZ", "FINI_ARRAYSZ",
	      "RUNPATH", "FLAGS", "ENCODING", "PREINIT_ARRAY",
	      "PREINIT_ARRAYSZ"
	    };

	  res = stdtags[tag];
	}
      else if (tag == DT_VERSYM)
	res = "VERSYM";
      else if (tag >= DT_GNU_PRELINKED && tag <= DT_SYMINENT)
	{
	  static const char *valrntags[] =
	    {
	      "GNU_PRELINKED", "GNU_CONFLICTSZ", "GNU_LIBLISTSZ",
	      "CHECKSUM", "PLTPADSZ", "MOVEENT", "MOVESZ", "FEATURE_1",
	      "POSFLAG_1", "SYMINSZ", "SYMINENT"
	    };

	  res = valrntags[tag - DT_GNU_PRELINKED];
	}
      else if (tag >= DT_GNU_HASH && tag <= DT_SYMINFO)
	{
	  static const char *addrrntags[] =
	    {
	      "GNU_HASH", "TLSDESC_PLT", "TLSDESC_DOT",
	      "GNU_CONFLICT", "GNU_LIBLIST", "CONFIG", "DEPAUDIT", "AUDIT",
	      "PLTPAD", "MOVETAB", "SYMINFO"
	    };

	  res = addrrntags[tag - DT_GNU_HASH];
	}
      else if (tag >= DT_RELACOUNT && tag <= DT_VERNEEDNUM)
	{
	  static const char *suntags[] =
	    {
	      "RELACOUNT", "RELCOUNT", "FLAGS_1", "VERDEF", "VERDEFNUM",
	      "VERNEED", "VERNEEDNUM"
	    };

	  res = suntags[tag - DT_RELACOUNT];
	}
      else if (tag == DT_AUXILIARY)
	res = "AUXILIARY";
      else if (tag == DT_FILTER)
	res = "FILTER";
      else
	{
	  snprintf (buf, len, gettext ("<unknown>: %#" PRIx64), tag);

	  res = buf;

	}
    }

  return res;
}
