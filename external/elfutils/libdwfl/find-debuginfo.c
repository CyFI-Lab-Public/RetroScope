/* Standard find_debuginfo callback for libdwfl.
   Copyright (C) 2005, 2006, 2007, 2008 Red Hat, Inc.
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
#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include "system.h"

/* Try to open64 [DIR/][SUBDIR/]DEBUGLINK, return file descriptor or -1.
   On success, *DEBUGINFO_FILE_NAME has the malloc'd name of the open file.  */
static int
try_open (const char *dir, const char *subdir, const char *debuglink,
	  char **debuginfo_file_name)
{
  char *fname;
  if (dir == NULL && subdir == NULL)
    {
      fname = strdup (debuglink);
      if (fname == NULL)
	return -1;
    }
  else if ((subdir == NULL ? asprintf (&fname, "%s/%s", dir, debuglink)
	    : dir == NULL ? asprintf (&fname, "%s/%s", subdir, debuglink)
	    : asprintf (&fname, "%s/%s/%s", dir, subdir, debuglink)) < 0)
    return -1;

  int fd = TEMP_FAILURE_RETRY (open64 (fname, O_RDONLY));
  if (fd < 0)
    free (fname);
  else
    *debuginfo_file_name = fname;

  return fd;
}

/* Return true iff the FD's contents CRC matches DEBUGLINK_CRC.  */
static inline bool
check_crc (int fd, GElf_Word debuglink_crc)
{
  uint32_t file_crc;
  return (__libdwfl_crc32_file (fd, &file_crc) == 0
	  && file_crc == debuglink_crc);
}

static bool
validate (Dwfl_Module *mod, int fd, bool check, GElf_Word debuglink_crc)
{
  /* If we have a build ID, check only that.  */
  if (mod->build_id_len > 0)
    {
      /* We need to open an Elf handle on the file so we can check its
	 build ID note for validation.  Backdoor the handle into the
	 module data structure since we had to open it early anyway.  */
      mod->debug.elf = elf_begin (fd, ELF_C_READ_MMAP_PRIVATE, NULL);
      if (likely (__libdwfl_find_build_id (mod, false, mod->debug.elf) == 2))
	/* Also backdoor the gratuitous flag.  */
	mod->debug.valid = true;
      else
	{
	  /* A mismatch!  */
	  elf_end (mod->debug.elf);
	  mod->debug.elf = NULL;
	  mod->debug.valid = false;
	}

      return mod->debug.valid;
    }

  return !check || check_crc (fd, debuglink_crc);
}

static int
find_debuginfo_in_path (Dwfl_Module *mod, const char *file_name,
			const char *debuglink_file, GElf_Word debuglink_crc,
			char **debuginfo_file_name)
{
  bool cancheck = debuglink_crc != (GElf_Word) 0;

  const char *file_basename = file_name == NULL ? NULL : basename (file_name);
  if (debuglink_file == NULL)
    {
      if (file_basename == NULL)
	{
	  errno = 0;
	  return -1;
	}

      size_t len = strlen (file_basename);
      char *localname = alloca (len + sizeof ".debug");
      memcpy (localname, file_basename, len);
      memcpy (&localname[len], ".debug", sizeof ".debug");
      debuglink_file = localname;
      cancheck = false;
    }

  /* Look for a file named DEBUGLINK_FILE in the directories
     indicated by the debug directory path setting.  */

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

  /* A leading - or + in the whole path sets whether to check file CRCs.  */
  bool defcheck = true;
  if (path[0] == '-' || path[0] == '+')
    {
      defcheck = path[0] == '+';
      ++path;
    }

  /* ANDROID_CHANGE_BEGIN */
#if defined(__BIONIC__) || defined(__APPLE__)
  char *file_dirname = (file_basename == file_name ? NULL
			: strndup (file_name, file_basename - 1 - file_name));
#else
  char *file_dirname = (file_basename == file_name ? NULL
			: strndupa (file_name, file_basename - 1 - file_name));
#endif
  /* ANDROID_CHANGE_END */
  char *p;
  while ((p = strsep (&path, ":")) != NULL)
    {
      /* A leading - or + says whether to check file CRCs for this element.  */
      bool check = defcheck;
      if (*p == '+' || *p == '-')
	check = *p++ == '+';
      check = check && cancheck;

      const char *dir, *subdir;
      switch (p[0])
	{
	case '\0':
	  /* An empty entry says to try the main file's directory.  */
	  dir = file_dirname;
	  subdir = NULL;
	  break;
	case '/':
	  /* An absolute path says to look there for a subdirectory
	     named by the main file's absolute directory.
	     This cannot be applied to a relative file name.  */
	  if (file_dirname == NULL || file_dirname[0] != '/')
	    continue;
	  dir = p;
	  subdir = file_dirname + 1;
	  break;
	default:
	  /* A relative path says to try a subdirectory of that name
	     in the main file's directory.  */
	  dir = file_dirname;
	  subdir = p;
	  break;
	}

/* ANDROID_CHANGE_BEGIN */
      char *fname = NULL;
/* ANDROID_CHANGE_END */
      int fd = try_open (dir, subdir, debuglink_file, &fname);
      if (fd < 0)
	switch (errno)
	  {
	  case ENOENT:
	  case ENOTDIR:
	    continue;
	  default:
/* ANDROID_CHANGE_BEGIN */
#ifdef __BIONIC__
            free(path);
            free(file_dirname);
#endif
/* ANDROID_CHANGE_END */
	    return -1;
	  }
      if (validate (mod, fd, check, debuglink_crc))
	{
	  *debuginfo_file_name = fname;
/* ANDROID_CHANGE_BEGIN */
#ifdef __BIONIC__
          free(path);
          free(file_dirname);
#endif
/* ANDROID_CHANGE_END */
	  return fd;
	}
      free (fname);
      close (fd);
    }

/* ANDROID_CHANGE_BEGIN */
#ifdef __BIONIC__
  free(path);
  free(file_dirname);
#endif
/* ANDROID_CHANGE_END */

  /* No dice.  */
  errno = 0;
  return -1;
}

int
dwfl_standard_find_debuginfo (Dwfl_Module *mod,
			      void **userdata __attribute__ ((unused)),
			      const char *modname __attribute__ ((unused)),
			      GElf_Addr base __attribute__ ((unused)),
			      const char *file_name,
			      const char *debuglink_file,
			      GElf_Word debuglink_crc,
			      char **debuginfo_file_name)
{
  /* First try by build ID if we have one.  If that succeeds or fails
     other than just by finding nothing, that's all we do.  */
  const unsigned char *bits;
  GElf_Addr vaddr;
  if (INTUSE(dwfl_module_build_id) (mod, &bits, &vaddr) > 0)
    {
      int fd = INTUSE(dwfl_build_id_find_debuginfo) (mod,
						     NULL, NULL, 0,
						     NULL, NULL, 0,
						     debuginfo_file_name);
      if (fd >= 0 || errno != 0)
	return fd;
    }

  /* Failing that, search the path by name.  */
  int fd = find_debuginfo_in_path (mod, file_name,
				   debuglink_file, debuglink_crc,
				   debuginfo_file_name);

  if (fd < 0 && errno == 0)
    {
      /* If FILE_NAME is a symlink, the debug file might be associated
	 with the symlink target name instead.  */

      char *canon = canonicalize_file_name (file_name);
      if (canon != NULL && strcmp (file_name, canon))
	fd = find_debuginfo_in_path (mod, canon,
				     debuglink_file, debuglink_crc,
				     debuginfo_file_name);
      free (canon);
    }

  return fd;
}
INTDEF (dwfl_standard_find_debuginfo)
