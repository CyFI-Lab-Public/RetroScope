## Process this file with automake to create Makefile.in
##
## Copyright (C) 2000,2001,2002,2003,2004,2005,2006,2007,2008 Red Hat, Inc.
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
DEFS = -D_GNU_SOURCE -DHAVE_CONFIG_H -DOBJDIR=\"$(shell pwd)\"
if MUDFLAP
AM_CFLAGS = -fmudflap
else
AM_CFLAGS =
endif
AM_CFLAGS += -fpic -Wall -Wshadow -Werror -Wunused -Wextra -Wformat=2 \
	     -std=gnu99

INCLUDES = -I$(srcdir) -I$(top_srcdir)/libelf -I$(top_srcdir)/libdw \
	   -I$(top_srcdir)/lib -I.. -I$(srcdir)/../libasm
VERSION = 1
PACKAGE_VERSION = @PACKAGE_VERSION@
LIBEBL_SUBDIR = @LIBEBL_SUBDIR@

lib_LIBRARIES = libebl.a

pkginclude_HEADERS = libebl.h

gen_SOURCES = eblopenbackend.c eblclosebackend.c eblstrtab.c \
	      eblreloctypename.c eblsegmenttypename.c \
	      eblsectiontypename.c eblmachineflagname.c \
	      eblsymboltypename.c ebldynamictagname.c eblsectionname.c \
	      eblobjecttypename.c eblsymbolbindingname.c \
	      eblbackendname.c eblshflagscombine.c eblwstrtab.c \
	      eblgstrtab.c eblosabiname.c \
	      eblmachineflagcheck.c eblmachinesectionflagcheck.c \
	      eblreloctypecheck.c eblrelocvaliduse.c eblrelocsimpletype.c \
	      ebldynamictagcheck.c eblcorenotetypename.c eblobjnotetypename.c \
	      eblcorenote.c eblobjnote.c ebldebugscnp.c \
	      eblgotpcreloccheck.c eblcopyrelocp.c eblsectionstripp.c \
	      eblelfclass.c eblelfdata.c eblelfmachine.c \
	      ebl_check_special_symbol.c eblbsspltp.c eblretval.c \
	      eblreginfo.c eblnonerelocp.c eblrelativerelocp.c \
	      eblsysvhashentrysize.c eblauxvinfo.c eblcheckobjattr.c \
	      ebl_check_special_section.c ebl_syscall_abi.c

libebl_a_SOURCES = $(gen_SOURCES)


%.os: %.c %.o
	if $(COMPILE) -c -o $@ -fpic -DPIC -DSHARED -MT $@ -MD -MP \
	  -MF "$(DEPDIR)/$*.Tpo" `test -f '$<' || echo '$(srcdir)/'`$<; \
	then cat "$(DEPDIR)/$*.Tpo" >> "$(DEPDIR)/$*.Po"; \
	     rm -f "$(DEPDIR)/$*.Tpo"; \
	else rm -f "$(DEPDIR)/$*.Tpo"; exit 1; \
	fi

noinst_HEADERS = libeblP.h ebl-hooks.h

CLEANFILES = $(am_libebl_pic_a_OBJECTS) *.gcno *.gcda
