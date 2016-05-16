/* Copyright (C) 2008 Red Hat, Inc.
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

#include <stdio.h>
#include <string.h>

#include <sha1.h>


int
main (void)
{
  char buf[1000];
  int result = 0;

  struct sha1_ctx ctx;
  sha1_init_ctx (&ctx);
  sha1_process_bytes ("abc", 3, &ctx);
  sha1_finish_ctx (&ctx, buf);
  static const char expected1[SHA1_DIGEST_SIZE] =
    "\xa9\x99\x3e\x36\x47\x06\x81\x6a\xba\x3e"
    "\x25\x71\x78\x50\xc2\x6c\x9c\xd0\xd8\x9d";
  if (memcmp (buf, expected1, SHA1_DIGEST_SIZE) != 0)
    {
      puts ("test 1 failed");
      result = 1;
    }

  sha1_init_ctx (&ctx);
  sha1_process_bytes ("\
abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq", 56, &ctx);
  sha1_finish_ctx (&ctx, buf);
  static const char expected2[SHA1_DIGEST_SIZE] =
    "\x84\x98\x3e\x44\x1c\x3b\xd2\x6e\xba\xae"
    "\x4a\xa1\xf9\x51\x29\xe5\xe5\x46\x70\xf1";
  if (memcmp (buf, expected2, SHA1_DIGEST_SIZE) != 0)
    {
      puts ("test 2 failed");
      result = 1;
    }

  sha1_init_ctx (&ctx);
  memset (buf, 'a', sizeof (buf));
  for (int i = 0; i < 1000; ++i)
    sha1_process_bytes (buf, sizeof (buf), &ctx);
  sha1_finish_ctx (&ctx, buf);
  static const char expected3[SHA1_DIGEST_SIZE] =
    "\x34\xaa\x97\x3c\xd4\xc4\xda\xa4\xf6\x1e"
    "\xeb\x2b\xdb\xad\x27\x31\x65\x34\x01\x6f";
  if (memcmp (buf, expected3, SHA1_DIGEST_SIZE) != 0)
    {
      puts ("test 3 failed");
      result = 1;
    }

  return result;
}
