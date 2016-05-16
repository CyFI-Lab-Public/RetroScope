#! /bin/sh
# Copyright (C) 1999, 2000, 2002, 2005 Red Hat, Inc.
# This file is part of Red Hat elfutils.
# Written by Ulrich Drepper <drepper@redhat.com>, 1999.
#
# Red Hat elfutils is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by the
# Free Software Foundation; version 2 of the License.
#
# Red Hat elfutils is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# General Public License for more details.
#
# You should have received a copy of the GNU General Public License along
# with Red Hat elfutils; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301 USA.
#
# Red Hat elfutils is an included package of the Open Invention Network.
# An included package of the Open Invention Network is a package for which
# Open Invention Network licensees cross-license their patents.  No patent
# license is granted, either expressly or impliedly, by designation as an
# included package.  Should you wish to participate in the Open Invention
# Network licensing program, please visit www.openinventionnetwork.com
# <http://www.openinventionnetwork.com>.

. $srcdir/test-subr.sh

testfiles testfile testfile2

testrun_compare ./get-aranges testfile testfile2 <<\EOF
0x804842b: not in range
CU name: "m.c"
CU name: "m.c"
CU name: "m.c"
0x804845a: not in range
0x804845b: not in range
CU name: "b.c"
CU name: "b.c"
CU name: "b.c"
0x8048466: not in range
0x8048467: not in range
CU name: "f.c"
CU name: "f.c"
CU name: "f.c"
0x8048472: not in range
 [ 0] start: 0x804842c, length: 46, cu: 11
CU name: "m.c"
 [ 1] start: 0x804845c, length: 10, cu: 202
CU name: "b.c"
 [ 2] start: 0x8048468, length: 10, cu: 5628
CU name: "f.c"
0x804842b: not in range
0x804842c: not in range
0x804843c: not in range
0x8048459: not in range
0x804845a: not in range
0x804845b: not in range
0x804845c: not in range
0x8048460: not in range
0x8048465: not in range
0x8048466: not in range
0x8048467: not in range
0x8048468: not in range
0x8048470: not in range
0x8048471: not in range
0x8048472: not in range
 [ 0] start: 0x10000470, length: 32, cu: 11
CU name: "b.c"
 [ 1] start: 0x10000490, length: 32, cu: 2429
CU name: "f.c"
 [ 2] start: 0x100004b0, length: 100, cu: 2532
CU name: "m.c"
EOF

exit 0
