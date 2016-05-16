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

testfiles testfile testfile2 testfile8

testrun_compare ./allfcts testfile testfile2 testfile8 <<\EOF
/home/drepper/gnu/new-bu/build/ttt/m.c:5:main
/home/drepper/gnu/new-bu/build/ttt/b.c:4:bar
/home/drepper/gnu/new-bu/build/ttt/f.c:3:foo
/shoggoth/drepper/b.c:4:bar
/shoggoth/drepper/f.c:3:foo
/shoggoth/drepper/m.c:5:main
/home/drepper/gnu/elfutils/build/src/../../src/strip.c:107:main
/home/drepper/gnu/elfutils/build/src/../../src/strip.c:159:print_version
/home/drepper/gnu/elfutils/build/src/../../src/strip.c:173:parse_opt
/home/drepper/gnu/elfutils/build/src/../../src/strip.c:201:more_help
/home/drepper/gnu/elfutils/build/src/../../src/strip.c:217:process_file
/usr/include/sys/stat.h:375:stat64
/home/drepper/gnu/elfutils/build/src/../../src/strip.c:291:crc32_file
/home/drepper/gnu/elfutils/build/src/../../src/strip.c:313:handle_elf
EOF

exit 0
