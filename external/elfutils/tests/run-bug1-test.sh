#! /bin/sh
# Copyright (C) 2006 Red Hat, Inc.
# Written by Ulrich Drepper <drepper@redhat.com>, 2006.
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

testfiles testfile28 testfile28.rdwr

testrun ./rdwrmmap testfile28

cmp testfile28 testfile28.rdwr

test_cleanup

testfiles testfile29 testfile29.rdwr

testrun ./rdwrmmap testfile29

cmp testfile29 testfile29.rdwr

exit 0
