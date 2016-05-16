/* Declarations for common convenience functions.
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

#ifndef LIB_SYSTEM_H
#define LIB_SYSTEM_H	1

#include <stddef.h>
#include <stdint.h>

extern void *xmalloc (size_t) __attribute__ ((__malloc__));
extern void *xcalloc (size_t, size_t) __attribute__ ((__malloc__));
extern void *xrealloc (void *, size_t) __attribute__ ((__malloc__));

extern char *xstrdup (const char *) __attribute__ ((__malloc__));
extern char *xstrndup (const char *, size_t) __attribute__ ((__malloc__));


extern uint32_t crc32 (uint32_t crc, unsigned char *buf, size_t len);
extern int crc32_file (int fd, uint32_t *resp);

/* A special gettext function we use if the strings are too short.  */
#define sgettext(Str) \
  ({ const char *__res = strrchr (gettext (Str), '|');			      \
     __res ? __res + 1 : Str; })

#define gettext_noop(Str) Str


#define pwrite_retry(fd, buf,  len, off) \
  TEMP_FAILURE_RETRY (pwrite (fd, buf, len, off))
#define write_retry(fd, buf, n) \
     TEMP_FAILURE_RETRY (write (fd, buf, n))
#define pread_retry(fd, buf,  len, off) \
  TEMP_FAILURE_RETRY (pread (fd, buf, len, off))

#endif /* system.h */
