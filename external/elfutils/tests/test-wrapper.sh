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


# We don't compile in an rpath because we want "make installcheck" to
# use the installed libraries.  So for local test runs we need to point
# the library path at this build.

# This wrapper script is called by the makefile, in one of two ways:
#	$(srcdir)/test-wrapper.sh ../libelf:... run-test.sh ...
# or:
#	$(srcdir)/test-wrapper.sh installed s,^,eu-, run-test.sh ...

if [ "$1" = installed ]; then
  shift
  elfutils_tests_rpath=$1
  shift
  program_transform_name="$1"
  shift
  elfutils_testrun=installed
else
  built_library_path="$1"
  shift
  elfutils_testrun=built
fi

case "$1" in
*.sh)
  export built_library_path program_transform_name elfutils_testrun
  export elfutils_tests_rpath
  ;;
*)
  if [ $elfutils_testrun = built ]; then
    LD_LIBRARY_PATH="$built_library_path${LD_LIBRARY_PATH:+:}$LD_LIBRARY_PATH"
    export LD_LIBRARY_PATH
  elif [ $elfutils_tests_rpath = yes ]; then
    echo >&2 installcheck not possible with --enable-tests-rpath
    exit 77
  elif [ "x$libdir" != x/usr/lib ] && [ "x$libdir" != x/usr/lib64 ]; then
    LD_LIBRARY_PATH="$libdir${LD_LIBRARY_PATH:+:}$LD_LIBRARY_PATH"
    export LD_LIBRARY_PATH
  fi
  ;;
esac

exec "$@"
