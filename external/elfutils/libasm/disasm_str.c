/* Copyright (C) 2005, 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2007.

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

#include <string.h>

#include "libasmP.h"


struct buffer
{
  char *buf;
  size_t len;
};


static int
buffer_cb (char *str, size_t len, void *arg)
{
  struct buffer *buffer = (struct buffer *) arg;

  if (len > buffer->len)
    /* Return additional needed space.  */
    return len - buffer->len;

  buffer->buf = mempcpy (buffer->buf, str, len);
  buffer->len = len;

  return 0;
}


int
disasm_str (DisasmCtx_t *ctx, const uint8_t **startp, const uint8_t *end,
	    GElf_Addr addr, const char *fmt, char **bufp, size_t len,
	    void *symcbarg)
{
  struct buffer buffer = { .buf = *bufp, .len = len };

  int res = INTUSE(disasm_cb) (ctx, startp, end, addr, fmt, buffer_cb, &buffer,
			       symcbarg);
  *bufp = buffer.buf;
  return res;
}
