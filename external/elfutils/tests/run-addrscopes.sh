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

testfiles testfile22

testrun_compare ./addrscopes -e testfile22 0x8048353 <<\EOF
0x8048353:
    tests/foo.c (0x11): 0x8048348 (tests/foo.c:5) .. 0x804837e (tests/foo.c:16)
        global                        [    be]
        function (0x2e): 0x8048348 (tests/foo.c:5) .. 0x804835b (tests/foo.c:14)
            local                         [    8f]
EOF

test_cleanup

testfiles testfile24
testrun_compare ./addrscopes -e testfile24 0x804834e <<\EOF
0x804834e:
    inline-test.c (0x11): 0x8048348 (/home/roland/build/stock-elfutils/inline-test.c:7) .. 0x8048364 (/home/roland/build/stock-elfutils/inline-test.c:16)
        add (0x1d): 0x804834e (/home/roland/build/stock-elfutils/inline-test.c:3) .. 0x8048350 (/home/roland/build/stock-elfutils/inline-test.c:9)
            y                             [    9d]
            x                             [    a2]
            x (abstract)
            y (abstract)
EOF

exit 0
