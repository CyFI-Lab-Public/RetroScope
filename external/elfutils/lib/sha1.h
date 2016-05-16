/* Declaration of functions and data types used for SHA1 sum computing
   library functions.
   Copyright (C) 2008 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2008.

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

#ifndef _SHA1_H
#define _SHA1_H 1

#include <limits.h>
#include <stdint.h>
#include <stdio.h>

#define SHA1_DIGEST_SIZE 20
#define SHA1_BLOCK_SIZE 64

typedef uint32_t sha1_uint32;
typedef uintptr_t sha1_uintptr;

/* Structure to save state of computation between the single steps.  */
struct sha1_ctx
{
  sha1_uint32 A;
  sha1_uint32 B;
  sha1_uint32 C;
  sha1_uint32 D;
  sha1_uint32 E;

  sha1_uint32 total[2];
  sha1_uint32 buflen;
  char buffer[128] __attribute__ ((__aligned__ (__alignof__ (sha1_uint32))));
};

/* Initialize structure containing state of computation.  */
extern void sha1_init_ctx (struct sha1_ctx *ctx);

/* Starting with the result of former calls of this function (or the
   initialization function update the context for the next LEN bytes
   starting at BUFFER.
   It is necessary that LEN is a multiple of 64!!! */
extern void sha1_process_block (const void *buffer, size_t len,
				struct sha1_ctx *ctx);

/* Starting with the result of former calls of this function (or the
   initialization function update the context for the next LEN bytes
   starting at BUFFER.
   It is NOT required that LEN is a multiple of 64.  */
extern void sha1_process_bytes (const void *buffer, size_t len,
				struct sha1_ctx *ctx);

/* Process the remaining bytes in the buffer and put result from CTX
   in first 20 bytes following RESBUF.  The result is always in little
   endian byte order, so that a byte-wise output yields to the wanted
   ASCII representation of the message digest.

   IMPORTANT: On some systems it is required that RESBUF is correctly
   aligned for a 32 bits value.  */
extern void *sha1_finish_ctx (struct sha1_ctx *ctx, void *resbuf);


/* Put result from CTX in first 20 bytes following RESBUF.  The result is
   always in little endian byte order, so that a byte-wise output yields
   to the wanted ASCII representation of the message digest.

   IMPORTANT: On some systems it is required that RESBUF is correctly
   aligned for a 32 bits value.  */
extern void *sha1_read_ctx (const struct sha1_ctx *ctx, void *resbuf);

#endif /* sha1.h */
