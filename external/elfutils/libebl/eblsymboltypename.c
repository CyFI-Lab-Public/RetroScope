/* Return symbol type name.
   Copyright (C) 2001, 2002 Red Hat, Inc.
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

#include <stdio.h>
#include <libeblP.h>


const char *
ebl_symbol_type_name (ebl, symbol, buf, len)
     Ebl *ebl;
     int symbol;
     char *buf;
     size_t len;
{
  const char *res;

  res = ebl != NULL ? ebl->symbol_type_name (symbol, buf, len) : NULL;
  if (res == NULL)
    {
      static const char *stt_names[STT_NUM] =
	{
	  [STT_NOTYPE] = "NOTYPE",
	  [STT_OBJECT] = "OBJECT",
	  [STT_FUNC] = "FUNC",
	  [STT_SECTION] = "SECTION",
	  [STT_FILE] = "FILE",
	  [STT_COMMON] = "COMMON",
	  [STT_TLS] = "TLS"
	};

      /* Standard type?  */
      if (symbol < STT_NUM)
	res = stt_names[symbol];
      else
	{
	  if (symbol >= STT_LOPROC && symbol <= STT_HIPROC)
	    snprintf (buf, len, "LOPROC+%d", symbol - STT_LOPROC);
	  else if (symbol >= STT_LOOS && symbol <= STT_HIOS)
	    snprintf (buf, len, "LOOS+%d", symbol - STT_LOOS);
	  else
	    snprintf (buf, len, gettext ("<unknown>: %d"), symbol);

	  res = buf;
	}
    }

  return res;
}
