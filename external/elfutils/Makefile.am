## Process this file with automake to create Makefile.in
## Configure input file for elfutils.
##
## Copyright (C) 1996-2002, 2003, 2004, 2005, 2006, 2008 Red Hat, Inc.
## This file is part of Red Hat elfutils.
##
## Red Hat elfutils is free software; you can redistribute it and/or modify
## it under the terms of the GNU General Public License as published by the
## Free Software Foundation; version 2 of the License.
##
## Red Hat elfutils is distributed in the hope that it will be useful, but
## WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
## General Public License for more details.
##
## You should have received a copy of the GNU General Public License along
## with Red Hat elfutils; if not, write to the Free Software Foundation,
## Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301 USA.
##
## Red Hat elfutils is an included package of the Open Invention Network.
## An included package of the Open Invention Network is a package for which
## Open Invention Network licensees cross-license their patents.  No patent
## license is granted, either expressly or impliedly, by designation as an
## included package.  Should you wish to participate in the Open Invention
## Network licensing program, please visit www.openinventionnetwork.com
## <http://www.openinventionnetwork.com>.
##
ACLOCAL_AMFLAGS = -I m4

pkginclude_HEADERS = version.h

# Add doc back when we have some real content.
SUBDIRS = config m4 lib libelf libebl libdwfl libdw libcpu libasm backends \
	  src po tests

EXTRA_DIST = elfutils.spec GPG-KEY NOTES EXCEPTION

# Make sure the test install uses lib64 when $LIB will yield lib64.
# Make sure the test build uses the same compiler, which on e.g. ppc64
# determines which platform we are actually testing.
DISTCHECK_CONFIGURE_FLAGS = \
	--libdir=`echo $(libdir) | sed "s,^$(exec_prefix),$$dc_install_base,"`\
	CC="$(CC)"

distcheck-hook:
	chmod -R u+w $(distdir)

rpm: dist
	rpmbuild -ts --sign elfutils-@PACKAGE_VERSION@.tar.gz

# Tell version 3.79 and up of GNU make to not build goals in this
# directory in parallel.
.NOTPARALLEL:
