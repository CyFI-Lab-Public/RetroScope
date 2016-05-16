## Process this file with automake to produce Makefile.in -*-Makefile-*-
## Configure input file for elfutils.
##
## Copyright (C) 2004, 2005, 2008 Red Hat, Inc.
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
## Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
##
## Red Hat elfutils is an included package of the Open Invention Network.
## An included package of the Open Invention Network is a package for which
## Open Invention Network licensees cross-license their patents.  No patent
## license is granted, either expressly or impliedly, by designation as an
## included package.  Should you wish to participate in the Open Invention
## Network licensing program, please visit www.openinventionnetwork.com
## <http://www.openinventionnetwork.com>.
##
EXTRA_DIST = elfutils.spec.in

if MAINTAINER_MODE
$(srcdir)/elfutils.spec.in: $(top_srcdir)/NEWS
	@tmpname=$$(mktemp $${TMPDIR:-/tmp}/elfutils.XXXXXX); \
	date +'* %a %b %e %Y' | tr '[\n]' '[ ]' > $$tmpname; \
	getent passwd "$$(whoami)" | \
	  awk 'BEGIN {FS=":"} { printf $$5; exit 0}' >> $$tmpname; \
	echo -n " <$$(whoami)@redhat.com> " >> $$tmpname; \
	awk '\
	  $$1 == "Version" && started { exit } \
	  $$1 == "Version" { started=1; line=""; sub(/:/,"",$$2); \
			     print $$2 "-1"; next } \
	  NF > 0 { line = (line != "") ? (line " " $$0) : ("- " $$0) } \
	  NF == 0 && line != "" { print line; line="" } \
	  END { if (line != "") print line; print "" }' $< \
	| fold -s -w 70 | sed '1!s/^[^-]/  &/' >> $$tmpname; \
	sed "/^%changelog/r $$tmpname" $@ > $@.new; \
	rm -f $$tmpname; \
	mv -f $@.new $@
endif
