## Process this file with automake to create Makefile.in
##
## Copyright (C) 2002, 2003, 2004, 2005, 2006, 2007, 2008 Red Hat, Inc.
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
DEFS = -D_GNU_SOURCE -DHAVE_CONFIG_H -DIS_LIBDW
if MUDFLAP
AM_CFLAGS = -fmudflap
else
AM_CFLAGS =
endif
if BUILD_STATIC
AM_CFLAGS += -fpic
endif
AM_CFLAGS += -Wall -Werror -Wshadow -Wunused -Wformat=2 -Wextra -std=gnu99
INCLUDES = -I. -I$(srcdir) -I$(srcdir)/../libelf -I.. -I$(srcdir)/../lib
VERSION = 1

COMPILE.os = $(filter-out -fprofile-arcs, $(filter-out -ftest-coverage, \
						       $(COMPILE)))

lib_LIBRARIES = libdw.a
if !MUDFLAP
noinst_LIBRARIES = libdw_pic.a
noinst_PROGRAMS = $(noinst_LIBRARIES:_pic.a=.so)
endif

include_HEADERS = dwarf.h
pkginclude_HEADERS = libdw.h

libdw_a_SOURCES = dwarf_begin.c dwarf_begin_elf.c dwarf_end.c dwarf_getelf.c \
		  dwarf_getpubnames.c dwarf_getabbrev.c dwarf_tag.c \
		  dwarf_error.c dwarf_nextcu.c dwarf_diename.c dwarf_offdie.c \
		  dwarf_attr.c dwarf_formstring.c dwarf_abbrev_hash.c \
		  dwarf_attr_integrate.c dwarf_hasattr_integrate.c \
		  dwarf_child.c dwarf_haschildren.c dwarf_formaddr.c \
		  dwarf_formudata.c dwarf_formsdata.c dwarf_lowpc.c \
		  dwarf_entrypc.c dwarf_haspc.c dwarf_highpc.c dwarf_ranges.c \
		  dwarf_formref.c dwarf_formref_die.c dwarf_siblingof.c \
		  dwarf_dieoffset.c dwarf_cuoffset.c dwarf_diecu.c \
		  dwarf_hasattr.c dwarf_hasform.c \
		  dwarf_whatform.c dwarf_whatattr.c \
		  dwarf_bytesize.c dwarf_arrayorder.c dwarf_bitsize.c \
		  dwarf_bitoffset.c dwarf_srclang.c dwarf_getabbrevtag.c \
		  dwarf_getabbrevcode.c dwarf_abbrevhaschildren.c \
		  dwarf_getattrcnt.c dwarf_getabbrevattr.c \
		  dwarf_getsrclines.c dwarf_getsrc_die.c \
		  dwarf_getscopes.c dwarf_getscopes_die.c dwarf_getscopevar.c \
		  dwarf_linesrc.c dwarf_lineno.c dwarf_lineaddr.c \
		  dwarf_linecol.c dwarf_linebeginstatement.c \
		  dwarf_lineendsequence.c dwarf_lineblock.c \
		  dwarf_lineprologueend.c dwarf_lineepiloguebegin.c \
		  dwarf_onesrcline.c dwarf_formblock.c \
		  dwarf_getsrcfiles.c dwarf_filesrc.c dwarf_getsrcdirs.c \
		  dwarf_getlocation.c dwarf_getstring.c dwarf_offabbrev.c \
		  dwarf_getaranges.c dwarf_onearange.c dwarf_getarangeinfo.c \
		  dwarf_getarange_addr.c dwarf_getattrs.c dwarf_formflag.c \
		  dwarf_getmacros.c dwarf_macro_opcode.c dwarf_macro_param1.c \
		  dwarf_macro_param2.c dwarf_addrdie.c \
		  dwarf_getfuncs.c  \
		  dwarf_decl_file.c dwarf_decl_line.c dwarf_decl_column.c \
		  dwarf_func_inline.c dwarf_getsrc_file.c \
		  libdw_findcu.c libdw_form.c libdw_alloc.c memory-access.c \
		  libdw_visit_scopes.c \
		  dwarf_entry_breakpoints.c

if !MUDFLAP
libdw_pic_a_SOURCES =
am_libdw_pic_a_OBJECTS = $(libdw_a_SOURCES:.c=.os)

libdw_so_SOURCES =
libdw.so: $(srcdir)/libdw.map libdw_pic.a \
	  ../libdwfl/libdwfl_pic.a ../libebl/libebl.a \
	  ../libelf/libelf.so
# The rpath is necessary for libebl because its $ORIGIN use will
# not fly in a setuid executable that links in libdw.
	$(LINK) -shared -o $@ -Wl,--soname,$@.$(VERSION),-z,defs \
		-Wl,--enable-new-dtags,-rpath,$(pkglibdir) \
		-Wl,--version-script,$<,--no-undefined \
		-Wl,--whole-archive $(filter-out $<,$^) -Wl,--no-whole-archive\
		-ldl
	if readelf -d $@ | fgrep -q TEXTREL; then exit 1; fi
	ln -fs $@ $@.$(VERSION)

%.os: %.c %.o
	if $(COMPILE.os) -c -o $@ -fpic -DPIC -DSHARED -MT $@ -MD -MP \
	   -MF "$(DEPDIR)/$*.Tpo" `test -f '$<' || echo '$(srcdir)/'`$<; \
	then cat "$(DEPDIR)/$*.Tpo" >> "$(DEPDIR)/$*.Po"; \
	     rm -f "$(DEPDIR)/$*.Tpo"; \
	else rm -f "$(DEPDIR)/$*.Tpo"; exit 1; \
	fi

install: install-am libdw.so
	$(mkinstalldirs) $(DESTDIR)$(libdir)
	$(INSTALL_PROGRAM) libdw.so $(DESTDIR)$(libdir)/libdw-$(PACKAGE_VERSION).so
	ln -fs libdw-$(PACKAGE_VERSION).so $(DESTDIR)$(libdir)/libdw.so.$(VERSION)
	ln -fs libdw.so.$(VERSION) $(DESTDIR)$(libdir)/libdw.so

uninstall: uninstall-am
	rm -f $(DESTDIR)$(libdir)/libdw-$(PACKAGE_VERSION).so
	rm -f $(DESTDIR)$(libdir)/libdw.so.$(VERSION)
	rm -f $(DESTDIR)$(libdir)/libdw.so
	rmdir --ignore-fail-on-non-empty $(DESTDIR)$(includedir)/elfutils
endif

libdw_a_LIBADD = $(addprefix ../libdwfl/,$(shell $(AR) t ../libdwfl/libdwfl.a))

noinst_HEADERS = libdwP.h memory-access.h dwarf_abbrev_hash.h

EXTRA_DIST = libdw.map

CLEANFILES = $(am_libdw_pic_a_OBJECTS) *.gcno *.gcda libdw.so.$(VERSION)
