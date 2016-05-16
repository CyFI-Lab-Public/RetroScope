## Makefile.am for libdwfl library subdirectory in elfutils.
##
## Process this file with automake to create Makefile.in
##
## Copyright (C) 2005, 2006, 2007, 2008 Red Hat, Inc.
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
DEFS = -D_GNU_SOURCE -DHAVE_CONFIG_H
if MUDFLAP
AM_CFLAGS = -fmudflap
else
AM_CFLAGS =
endif
AM_CFLAGS += -Wall -Werror -Wshadow -Wunused -Wformat=2 -Wextra -std=gnu99
INCLUDES = -I. -I$(srcdir) -I$(srcdir)/../libelf -I$(srcdir)/../libebl \
	   -I$(srcdir)/../libdw -I.. -I$(srcdir)/../lib
VERSION = 1

COMPILE.os = $(filter-out -fprofile-arcs, $(filter-out -ftest-coverage, \
						       $(COMPILE)))

noinst_LIBRARIES = libdwfl.a
if !MUDFLAP
noinst_LIBRARIES += libdwfl_pic.a
endif

pkginclude_HEADERS = libdwfl.h

libdwfl_a_SOURCES = dwfl_begin.c dwfl_end.c dwfl_error.c dwfl_version.c \
		    dwfl_module.c dwfl_report_elf.c relocate.c \
		    dwfl_module_build_id.c dwfl_module_report_build_id.c \
		    derelocate.c offline.c segment.c \
		    dwfl_module_info.c  dwfl_getmodules.c \
		    dwfl_module_getdwarf.c dwfl_getdwarf.c \
		    dwfl_validate_address.c \
		    argp-std.c find-debuginfo.c \
		    dwfl_build_id_find_elf.c \
		    dwfl_build_id_find_debuginfo.c \
		    linux-kernel-modules.c linux-proc-maps.c \
		    dwfl_addrmodule.c dwfl_addrdwarf.c \
		    cu.c dwfl_module_nextcu.c dwfl_nextcu.c dwfl_cumodule.c \
		    dwfl_module_addrdie.c dwfl_addrdie.c \
		    lines.c dwfl_lineinfo.c dwfl_line_comp_dir.c \
		    dwfl_linemodule.c dwfl_linecu.c \
		    dwfl_getsrclines.c dwfl_onesrcline.c \
		    dwfl_module_getsrc.c dwfl_getsrc.c \
		    dwfl_module_getsrc_file.c \
		    libdwfl_crc32.c libdwfl_crc32_file.c \
		    elf-from-memory.c \
		    dwfl_module_getsym.c \
		    dwfl_module_addrname.c dwfl_module_addrsym.c \
		    dwfl_module_return_value_location.c \
		    dwfl_module_register_names.c \
		    dwfl_segment_report_module.c \
		    link_map.c core-file.c


if MUDFLAP
libdwfl = libdwfl.a $(libdw) $(libebl) $(libelf) $(libeu)
libdw = ../libdw/libdw.a
libelf = ../libelf/libelf.a
libmudflap = -lmudflap
else
libdwfl = $(libdw)
libdw = ../libdw/libdw.so
libelf = ../libelf/libelf.so
endif
libebl = ../libebl/libebl.a
libeu = ../lib/libeu.a


if !MUDFLAP
libdwfl_pic_a_SOURCES =
am_libdwfl_pic_a_OBJECTS = $(libdwfl_a_SOURCES:.c=.os)

%.os: %.c %.o
	if $(COMPILE.os) -c -o $@ -fpic -DPIC -DSHARED -MT $@ -MD -MP \
	   -MF "$(DEPDIR)/$*.Tpo" `test -f '$<' || echo '$(srcdir)/'`$<; \
	then cat "$(DEPDIR)/$*.Tpo" >> "$(DEPDIR)/$*.Po"; \
	     rm -f "$(DEPDIR)/$*.Tpo"; \
	else rm -f "$(DEPDIR)/$*.Tpo"; exit 1; \
	fi
endif

noinst_HEADERS = libdwflP.h

CLEANFILES = $(am_libdwfl_pic_a_OBJECTS)
