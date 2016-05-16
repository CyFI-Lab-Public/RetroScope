#! /bin/sh
# Copyright (C) 2005 Red Hat, Inc.
# This file is part of Red Hat elfutils.
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

testfiles testfile25

testrun_compare ./funcscopes -e testfile25 incr <<\EOF
testfile25: 0x8048000 .. 0x8049528
    inline-test.c (0x11): 0x8048348 (/home/roland/build/stock-elfutils/inline-test.c:7) .. 0x804834f (/home/roland/build/stock-elfutils/inline-test.c:9)
        incr (0x2e): 0x8048348 (/home/roland/build/stock-elfutils/inline-test.c:7) .. 0x804834f (/home/roland/build/stock-elfutils/inline-test.c:9)
            x                             [    66]
EOF

exit 0
