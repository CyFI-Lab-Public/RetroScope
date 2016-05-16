#! /bin/sh
# Copyright (C) 1999, 2000, 2002, 2006 Red Hat, Inc.
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

lib=../libelf/libelf.a
okfile=arsymtest.ok
tmpfile=arsymtest.tmp
testfile=arsymtest.test

tempfiles $okfile $tmpfile $testfile

result=77
if test -f $lib; then
    # Generate list using `nm' we check against.
    nm -s $lib |
    sed -e '1,/^Arch/d' -e '/^$/,$d' |
    sort > $okfile

    # Now run our program using libelf.
    testrun ./arsymtest $lib $tmpfile || exit 1
    sort $tmpfile > $testfile

    # Compare the outputs.
    if cmp $okfile $testfile; then
	result=0
    else
	result=1
    fi
fi

exit $result
