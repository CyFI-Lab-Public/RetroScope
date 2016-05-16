/* Compute CRC32 checksum of file contents.
   Copyright (C) 2006 Red Hat, Inc.
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
   under an Open Source Initiative certified open source license
   (http://www.opensource.org/licenses/index.php) and to distribute linked
   combinations including the two.  Non-GPL Code permitted under this
   exception must only link to the code of Red Hat elfutils through those
   well defined interfaces identified in the file named EXCEPTION found in
   the source code files (the "Approved Interfaces").  The files of Non-GPL
   Code may instantiate templates or use macros or inline functions from
   the Approved Interfaces without causing the resulting work to be covered
   by the GNU General Public License.  Only Red Hat, Inc. may make changes
   or additions to the list of Approved Interfaces.  Red Hat's grant of
   this exception is conditioned upon your not adding any new exceptions.
   If you wish to add a new Approved Interface or exception, please contact
   Red Hat.  You must obey the GNU General Public License in all respects
   for all of the Red Hat elfutils code and other code used in conjunction
   with Red Hat elfutils except the Non-GPL Code covered by this exception.
   If you modify this file, you may extend this exception to your version
   of the file, but you are not obligated to do so.  If you do not wish to
   provide this exception without modification, you must delete this
   exception statement from your version and license this file solely under
   the GPL without exception.

   Red Hat elfutils is an included package of the Open Invention Network.
   An included package of the Open Invention Network is a package for which
   Open Invention Network licensees cross-license their patents.  No patent
   license is granted, either expressly or impliedly, by designation as an
   included package.  Should you wish to participate in the Open Invention
   Network licensing program, please visit www.openinventionnetwork.com
   <http://www.openinventionnetwork.com>.  */

#include "system.h"
#include <errno.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/mman.h>

int
crc32_file (int fd, uint32_t *resp)
{
  unsigned char buffer[1024 * 8];
  uint32_t crc = 0;
  off_t off = 0;
  ssize_t count;

  struct stat st;
  if (fstat (fd, &st) == 0)
    {
      /* Try mapping in the file data.  */
      size_t mapsize = st.st_size;
      void *mapped = mmap (NULL, mapsize, PROT_READ, MAP_PRIVATE, fd, 0);
      if (mapped == MAP_FAILED && errno == ENOMEM)
	{
	  const size_t pagesize = sysconf (_SC_PAGE_SIZE);
	  mapsize = ((mapsize / 2) + pagesize - 1) & -pagesize;
	  while (mapsize >= pagesize
		 && (mapped = mmap (NULL, mapsize, PROT_READ, MAP_PRIVATE,
				    fd, 0)) == MAP_FAILED && errno == ENOMEM)
	    mapsize /= 2;
	}
      if (mapped != MAP_FAILED)
	{
	  do
	    {
	      if (st.st_size <= (off_t) mapsize)
		{
		  *resp = crc32 (crc, mapped, st.st_size);
		  munmap (mapped, mapsize);
		  return 0;
		}
	      crc = crc32 (crc, mapped, mapsize);
	      off += mapsize;
	      st.st_size -= mapsize;
	    } while (mmap (mapped, mapsize, PROT_READ, MAP_FIXED|MAP_PRIVATE,
			   fd, off) == mapped);
	  munmap (mapped, mapsize);
	}
    }

  while ((count = TEMP_FAILURE_RETRY (pread (fd, buffer, sizeof buffer,
					     off))) > 0)
    {
      off += count;
      crc = crc32 (crc, buffer, count);
    }

  *resp = crc;

  return count == 0 ? 0 : -1;
}
