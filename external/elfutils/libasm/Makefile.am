## Process this file with automake to create Makefile.in
##
## Copyright (C) 2002, 2004, 2005, 2006, 2008 Red Hat, Inc.
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
AM_CFLAGS += -std=gnu99 -Wall -Wshadow -Werror -Wunused -Wextra -Wformat=2
INCLUDES = -I. -I$(srcdir) -I.. \
	   -I$(top_srcdir)/libelf -I$(top_srcdir)/libebl -I$(top_srcdir)/libdw\
	   -I$(top_srcdir)/lib
GCC_INCLUDE = -I$(shell $(CC) -print-file-name=include)
VERSION = 1

COMPILE.os = $(filter-out -fprofile-arcs, $(filter-out -ftest-coverage, \
						       $(COMPILE)))

lib_LIBRARIES = libasm.a
if !MUDFLAP
noinst_LIBRARIES = libasm_pic.a
noinst_PROGRAMS = $(noinst_LIBRARIES:_pic.a=.so)
endif
pkginclude_HEADERS = libasm.h

libasm_a_SOURCES = asm_begin.c asm_abort.c asm_end.c asm_error.c \
		   asm_getelf.c asm_newscn.c asm_newscn_ingrp.c \
		   asm_newsubscn.c asm_newsym.c asm_newcomsym.c \
		   asm_newabssym.c \
		   asm_newscngrp.c asm_scngrp_newsignature.c \
		   asm_fill.c asm_align.c asm_addstrz.c \
		   asm_addint8.c asm_adduint8.c \
		   asm_addint16.c asm_adduint16.c \
		   asm_addint32.c asm_adduint32.c \
		   asm_addint64.c asm_adduint64.c \
		   asm_adduleb128.c asm_addsleb128.c \
		   disasm_begin.c disasm_cb.c disasm_end.c disasm_str.c \
		   symbolhash.c

if !MUDFLAP
libasm_pic_a_SOURCES =
am_libasm_pic_a_OBJECTS = $(libasm_a_SOURCES:.c=.os)

libasm_so_LDLIBS =
if USE_TLS
libasm_so_LDLIBS += -lpthread
endif

libasm_so_SOURCES =
libasm.so: libasm_pic.a libasm.map
	$(LINK) -shared -o $@ -Wl,--whole-archive,$<,--no-whole-archive \
		-Wl,--version-script,$(srcdir)/libasm.map,--no-undefined \
		-Wl,--soname,$@.$(VERSION) \
		../libebl/libebl.a ../libelf/libelf.so  $(libasm_so_LDLIBS)
	if readelf -d $@ | fgrep -q TEXTREL; then exit 1; fi
	ln -fs $@ $@.$(VERSION)


%.os: %.c %.o
	if $(COMPILE.os) -c -o $@ -fpic -DPIC -DSHARED -MT $@ -MD -MP \
	  -MF "$(DEPDIR)/$*.Tpo" `test -f '$<' || echo '$(srcdir)/'`$<; \
	then cat "$(DEPDIR)/$*.Tpo" >> "$(DEPDIR)/$*.Po"; \
	     rm -f "$(DEPDIR)/$*.Tpo"; \
	else rm -f "$(DEPDIR)/$*.Tpo"; exit 1; \
	fi

install: install-am libasm.so
	$(mkinstalldirs) $(DESTDIR)$(libdir)
	$(INSTALL_PROGRAM) libasm.so $(DESTDIR)$(libdir)/libasm-$(PACKAGE_VERSION).so
	ln -fs libasm-$(PACKAGE_VERSION).so $(DESTDIR)$(libdir)/libasm.so.$(VERSION)
	ln -fs libasm.so.$(VERSION) $(DESTDIR)$(libdir)/libasm.so

uninstall: uninstall-am
	rm -f $(DESTDIR)$(libdir)/libasm-$(PACKAGE_VERSION).so
	rm -f $(DESTDIR)$(libdir)/libasm.so.$(VERSION)
	rm -f $(DESTDIR)$(libdir)/libasm.so
	rmdir --ignore-fail-on-non-empty $(DESTDIR)$(includedir)/elfutils
endif

noinst_HEADERS = libasmP.h symbolhash.h
EXTRA_DIST = libasm.map

CLEANFILES = $(am_libasm_pic_a_OBJECTS) *.gcno *.gcda libasm.so.$(VERSION)
