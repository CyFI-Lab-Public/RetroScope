#! /bin/sh
# Copyright (C) 2005, 2007, 2008 Red Hat, Inc.
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


# This file is sourced by ". $srcdir/test-subr.sh" at the start of
# each test script.  It defines some functions they use and sets up
# canonical sh state for test runs.

set -e

#LC_ALL=C
#export LC_ALL

remove_files=
trap 'rm -f $remove_files' 0

tempfiles()
{
  remove_files="$remove_files $*"
}

testfiles()
{
  for file; do
    bunzip2 -c $srcdir/${file}.bz2 > ${file} 2>/dev/null || exit 77
    remove_files="$remove_files $file"
  done
}

testrun_out()
{
  outfile="$1"
  shift
  remove_files="$remove_files $outfile"
  testrun "$@" > $outfile 2>&1 || :
}

testrun_compare()
{
  outfile="${1##*/}.out"
  testrun_out $outfile "$@"
  diff -u $outfile -
  # diff's exit status will kill the script.
}

test_cleanup()
{
  rm -f $remove_files
  remove_files=
}

# See test-wrapper.sh, which sets the environment for this.
testrun()
{
  ${elfutils_testrun}_testrun "$@"
}

built_testrun()
{
  LD_LIBRARY_PATH="${built_library_path}${LD_LIBRARY_PATH:+:}$LD_LIBRARY_PATH"\
  "$@"
}

installed_testrun()
{
  program="$1"
  shift
  case "$program" in
  ./*)
    if [ "x$elfutils_tests_rpath" != xno ]; then
      echo >&2 installcheck not possible with --enable-tests-rpath
      exit 77
    fi
    ;;
  ../*)
    program=${bindir}/`program_transform ${program##*/}`
    ;;
  esac
  if [ "${libdir}" != /usr/lib ] && [ "${libdir}" != /usr/lib64 ]; then
    LD_LIBRARY_PATH="${libdir}${LD_LIBRARY_PATH:+:}$LD_LIBRARY_PATH" \
    $program ${1+"$@"}
  else
    $program ${1+"$@"}
  fi
}

program_transform()
{
  echo "$*" | sed "${program_transform_name}"
}
