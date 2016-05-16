#! /bin/sh
# Copyright (C) 2005, 2007 Red Hat, Inc.
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

status=0
runtest() {
# Uncomment for debuging
#  echo $1
  if [ -f $1 ]; then
    testrun ../src/elflint --quiet --gnu-ld $1 ||
    { echo "*** failure in $1"; status=1; }
  fi
}

runtest ../src/addr2line
runtest ../src/elfcmp
runtest ../src/elflint
runtest ../src/findtextrel
runtest ../src/ld
runtest ../src/nm
runtest ../src/objdump
runtest ../src/readelf
runtest ../src/size
runtest ../src/strip
runtest ../libelf/libelf.so
runtest ../libdw/libdw.so
runtest ../libasm/libasm.so
runtest ../libebl/libebl_alpha.so
runtest ../libebl/libebl_arm.so
runtest ../libebl/libebl_i386.so
runtest ../libebl/libebl_ia64.so
runtest ../libebl/libebl_ppc.so
runtest ../libebl/libebl_ppc64.so
runtest ../libebl/libebl_sh.so
runtest ../libebl/libebl_sparc.so
runtest ../libebl/libebl_x86_64.so

exit $status
