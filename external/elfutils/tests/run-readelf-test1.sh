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

original=${original:-testfile11}
stripped=${stripped:-testfile7}
debugout=${debugfile:+-f testfile.debug.temp -F $debugfile}

testfiles testfile3

tempfiles testfile.temp

testrun ../src/readelf -r testfile3 > testfile.temp

diff -u - testfile.temp <<EOF

Relocation section [ 8] '.rel.got' for section [19] '.got' at offset 0x294 contains 1 entry:
  Offset      Type                 Value       Name
  0x08049544  386_GLOB_DAT         0000000000  __gmon_start__

Relocation section [ 9] '.rel.plt' for section [11] '.plt' at offset 0x29c contains 4 entries:
  Offset      Type                 Value       Name
  0x08049534  386_JMP_SLOT         0x080482e4  __register_frame_info
  0x08049538  386_JMP_SLOT         0x080482f4  __deregister_frame_info
  0x0804953c  386_JMP_SLOT         0x08048304  __libc_start_main
  0x08049540  386_JMP_SLOT         0x08048314  __cxa_finalize
EOF

exit 0
