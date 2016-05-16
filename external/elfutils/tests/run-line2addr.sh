#! /bin/sh
# Copyright (C) 2005 Red Hat, Inc.
# This file is part of Red Hat elfutils.
# Written by Ulrich Drepper <drepper@redhat.com>, 2005.
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

testfiles testfile testfile2 testfile8 testfile14 testfile23

testrun_compare ./line2addr -e testfile f.c:4 testfile f.c:8 <<\EOF
f.c:4 -> 0x804846b (/home/drepper/gnu/new-bu/build/ttt/f.c:4)
EOF

testrun_compare ./line2addr -e testfile2 m.c:6 b.c:1 <<\EOF
m.c:6 -> 0x100004cc (/shoggoth/drepper/m.c:6)
b.c:1 -> 0x10000470 (/shoggoth/drepper/b.c:4)
EOF

testrun_compare ./line2addr -e testfile8 strip.c:953 strip.c:365 <<\EOF
strip.c:953 -> (.text)+0x169f (/home/drepper/gnu/elfutils/build/src/../../src/strip.c:953)
strip.c:953 -> (.text)+0x16aa (/home/drepper/gnu/elfutils/build/src/../../src/strip.c:953)
strip.c:365 -> (.text)+0x278b (/home/drepper/gnu/elfutils/build/src/../../src/strip.c:365)
strip.c:365 -> (.text)+0x2797 (/home/drepper/gnu/elfutils/build/src/../../src/strip.c:365)
EOF

testrun_compare ./line2addr -e testfile14 v.c:6 <<\EOF
v.c:6 -> 0x400468 (/home/drepper/local/elfutils-build/20050425/v.c:6)
v.c:6 -> 0x400487 (/home/drepper/local/elfutils-build/20050425/v.c:6)
EOF

testrun_compare ./line2addr -e testfile23 foo.c:2 foo.c:6 <<\EOF
foo.c:2 -> (.init.text)+0xc (/home/roland/stock-elfutils-build/foo.c:2)
foo.c:6 -> (.text)+0xc (/home/roland/stock-elfutils-build/foo.c:6)
EOF

exit 0
