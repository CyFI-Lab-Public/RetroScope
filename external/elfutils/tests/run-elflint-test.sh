#! /bin/sh
# Copyright (C) 2005, 2007, 2008 Red Hat, Inc.
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

testfiles testfile18

testrun_compare ../src/elflint --gnu-ld testfile18 <<\EOF
section [ 8] '.rela.dyn': relocation 1: copy relocation against symbol of type FUNC
EOF

testfiles testfile32
testrun ../src/elflint -q testfile32

testfiles testfile33
testrun ../src/elflint -q testfile33

testfiles testfile42
testrun ../src/elflint -q --gnu-ld testfile42

testfiles testfile46
testrun ../src/elflint -q testfile46

exit 0
