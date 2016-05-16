#! /bin/sh
# Copyright (C) 1999, 2000, 2002, 2003, 2005, 2007, 2008 Red Hat, Inc.
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

original=${original:-testfile11}
stripped=${stripped:-testfile7}
debugout=${debugfile:+-f testfile.debug.temp -F $debugfile}

testfiles $original
test x$stripped = xtestfile.temp || testfiles $stripped $debugfile

tempfiles testfile.temp testfile.debug.temp testfile.unstrip

testrun ../src/strip -o testfile.temp $debugout $original

status=0

cmp $stripped testfile.temp || status=$?

# Check elflint and the expected result.
testrun ../src/elflint -q testfile.temp || status=$?

test -z "$debugfile" || {
cmp $debugfile testfile.debug.temp || status=$?

# Check elflint and the expected result.
testrun ../src/elflint -q -d testfile.debug.temp || status=$?

# Now test unstrip recombining those files.
testrun ../src/unstrip -o testfile.unstrip testfile.temp testfile.debug.temp

# Check that it came back whole.
testrun ../src/elfcmp --hash-inexact $original testfile.unstrip
}

tempfiles testfile.sections
testrun ../src/readelf -S testfile.temp > testfile.sections || status=$?
fgrep ' .debug_' testfile.sections && status=1

exit $status
