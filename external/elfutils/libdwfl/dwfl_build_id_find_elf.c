/* Find an ELF file for a module from its build ID.
   Copyright (C) 2007, 2008 Red Hat, Inc.
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

#include "libdwflP.h"
#include <inttypes.h>
#include <fcntl.h>
#include <unistd.h>

int
internal_function
__libdwfl_open_by_build_id (Dwfl_Module *mod, bool debug, char **file_name)
{
  /* If *FILE_NAME was primed into the module, leave it there
     as the fallback when we have nothing to offer.  */
  errno = 0;
  if (mod->build_id_len <= 0)
    return -1;

  const size_t id_len = mod->build_id_len;
  const uint8_t *id = mod->build_id_bits;

  /* Search debuginfo_path directories' .build-id/ subdirectories.  */

  char id_name[sizeof "/.build-id/" + 1 + id_len * 2 + sizeof ".debug" - 1];
  strcpy (id_name, "/.build-id/");
  int n = snprintf (&id_name[sizeof "/.build-id/" - 1],
		    4, "%02" PRIx8 "/", (uint8_t) id[0]);
  assert (n == 3);
  for (size_t i = 1; i < id_len; ++i)
    {
      n = snprintf (&id_name[sizeof "/.build-id/" - 1 + 3 + (i - 1) * 2],
		    3, "%02" PRIx8, (uint8_t) id[i]);
      assert (n == 2);
    }
  if (debug)
    strcpy (&id_name[sizeof "/.build-id/" - 1 + 3 + (id_len - 1) * 2],
	    ".debug");

  const Dwfl_Callbacks *const cb = mod->dwfl->callbacks;

/* ANDROID_CHANGE_BEGIN */
#if defined(__BIONIC__) || defined(__APPLE__)
  char *path = strdup ((cb->debuginfo_path ? *cb->debuginfo_path : NULL)
			?: DEFAULT_DEBUGINFO_PATH);
#else
  char *path = strdupa ((cb->debuginfo_path ? *cb->debuginfo_path : NULL)
			?: DEFAULT_DEBUGINFO_PATH);
#endif
/* ANDROID_CHANGE_END */

  int fd = -1;
  char *dir;
  while (fd < 0 && (dir = strsep (&path, ":")) != NULL)
    {
      if (dir[0] == '+' || dir[0] == '-')
	++dir;

      /* Only absolute directory names are useful to us.  */
      if (dir[0] != '/')
	continue;

      size_t dirlen = strlen (dir);
      char *name = malloc (dirlen + sizeof id_name);
      if (unlikely (name == NULL))
	break;
      memcpy (mempcpy (name, dir, dirlen), id_name, sizeof id_name);
      fd = TEMP_FAILURE_RETRY (open64 (name, O_RDONLY));
      if (fd >= 0)
	{
	  if (*file_name != NULL)
	    free (*file_name);
	  *file_name = canonicalize_file_name (name);
	  if (*file_name == NULL)
	    {
	      *file_name = name;
	      name = NULL;
	    }
	}
      free (name);
    }

/* ANDROID_CHANGE_BEGIN */
#ifdef __BIONIC__
  free(path);
#endif
/* ANDROID_CHANGE_END */

  /* If we simply found nothing, clear errno.  If we had some other error
     with the file, report that.  Possibly this should treat other errors
     like ENOENT too.  But ignoring all errors could mask some that should
     be reported.  */
  if (fd < 0 && errno == ENOENT)
    errno = 0;

  return fd;
}

int
dwfl_build_id_find_elf (Dwfl_Module *mod,
			void **userdata __attribute__ ((unused)),
			const char *modname __attribute__ ((unused)),
			Dwarf_Addr base __attribute__ ((unused)),
			char **file_name, Elf **elfp)
{
  *elfp = NULL;
  int fd = __libdwfl_open_by_build_id (mod, false, file_name);
  if (fd >= 0)
    {
      *elfp = elf_begin (fd, ELF_C_READ_MMAP_PRIVATE, NULL);
      if (__libdwfl_find_build_id (mod, false, *elfp) == 2)
	/* This is a backdoor signal to short-circuit the ID refresh.  */
	mod->main.valid = true;
      else
	{
	  /* This file does not contain the ID it should!  */
	  elf_end (*elfp);
	  *elfp = NULL;
	  close (fd);
	  fd = -1;
	  free (*file_name);
	  *file_name = NULL;
	}
    }
  return fd;
}
INTDEF (dwfl_build_id_find_elf)
