/* Support to debug branch prediction.
   Copyright (C) 2007 Red Hat, Inc.
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

#include <stdio.h>

#if DEBUGPRED
extern const unsigned long int __start_predict_data;
extern const unsigned long int __stop_predict_data;
extern const unsigned long int __start_predict_line;
extern const char *__start_predict_file;

static void
__attribute__ ((destructor))
predprint (void)
{
  const unsigned long int *s = &__start_predict_data;
  const unsigned long int *e = &__stop_predict_data;
  const unsigned long int *sl = &__start_predict_line;
  const char **sf = &__start_predict_file;
  while (s < e)
    {
      if (s[0] != 0 || s[1] != 0)
	printf ("%s:%lu: wrong=%lu, correct=%lu%s\n", *sf, *sl, s[0], s[1],
		s[0] > s[1] ? "   <==== WARNING" : "");
      ++sl;
      ++sf;
      s += 2;
    }
}
#endif
