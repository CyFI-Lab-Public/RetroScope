## Process this file with automake to create Makefile.in
##
## Copyright (C) 2000-2006, 2007, 2008 Red Hat, Inc.
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
INCLUDES = -I$(srcdir) -I$(top_srcdir)/libebl -I$(top_srcdir)/libasm \
	   -I$(top_srcdir)/libelf -I$(top_srcdir)/libdw \
	   -I$(top_srcdir)/lib -I..


modules = i386 sh x86_64 ia64 alpha arm sparc ppc ppc64 s390
libebl_pic = libebl_i386_pic.a libebl_sh_pic.a libebl_x86_64_pic.a \
	     libebl_ia64_pic.a libebl_alpha_pic.a libebl_arm_pic.a \
	     libebl_sparc_pic.a libebl_ppc_pic.a libebl_ppc64_pic.a \
	     libebl_s390_pic.a
noinst_LIBRARIES = $(libebl_pic)
noinst_DATA = $(libebl_pic:_pic.a=.so)


if MUDFLAP
libelf = ../libelf/libelf.a
libdw = ../libdw/libdw.a
libmudflap = -lmudflap
else
libelf = ../libelf/libelf.so
libdw = ../libdw/libdw.so
libmudflap =
endif


textrel_check = if readelf -d $@ | fgrep -q TEXTREL; then exit 1; fi


i386_SRCS = i386_init.c i386_symbol.c i386_corenote.c \
	    i386_retval.c i386_regs.c i386_auxv.c i386_syscall.c
cpu_i386 = ../libcpu/libcpu_i386.a
libebl_i386_pic_a_SOURCES = $(i386_SRCS)
am_libebl_i386_pic_a_OBJECTS = $(i386_SRCS:.c=.os)

sh_SRCS = sh_init.c sh_symbol.c
libebl_sh_pic_a_SOURCES = $(sh_SRCS)
am_libebl_sh_pic_a_OBJECTS = $(sh_SRCS:.c=.os)

x86_64_SRCS = x86_64_init.c x86_64_symbol.c x86_64_corenote.c \
	      x86_64_retval.c x86_64_regs.c i386_auxv.c x86_64_syscall.c
cpu_x86_64 = ../libcpu/libcpu_x86_64.a
libebl_x86_64_pic_a_SOURCES = $(x86_64_SRCS)
am_libebl_x86_64_pic_a_OBJECTS = $(x86_64_SRCS:.c=.os)

ia64_SRCS = ia64_init.c ia64_symbol.c ia64_regs.c ia64_retval.c
libebl_ia64_pic_a_SOURCES = $(ia64_SRCS)
am_libebl_ia64_pic_a_OBJECTS = $(ia64_SRCS:.c=.os)

alpha_SRCS = alpha_init.c alpha_symbol.c alpha_retval.c alpha_regs.c \
	     alpha_corenote.c alpha_auxv.c
libebl_alpha_pic_a_SOURCES = $(alpha_SRCS)
am_libebl_alpha_pic_a_OBJECTS = $(alpha_SRCS:.c=.os)

arm_SRCS = arm_init.c arm_symbol.c
libebl_arm_pic_a_SOURCES = $(arm_SRCS)
am_libebl_arm_pic_a_OBJECTS = $(arm_SRCS:.c=.os)

sparc_SRCS = sparc_init.c sparc_symbol.c sparc_regs.c sparc_retval.c \
	     sparc_corenote.c sparc64_corenote.c sparc_auxv.c
libebl_sparc_pic_a_SOURCES = $(sparc_SRCS)
am_libebl_sparc_pic_a_OBJECTS = $(sparc_SRCS:.c=.os)

ppc_SRCS = ppc_init.c ppc_symbol.c ppc_retval.c ppc_regs.c \
	   ppc_corenote.c ppc_auxv.c ppc_attrs.c ppc_syscall.c
libebl_ppc_pic_a_SOURCES = $(ppc_SRCS)
am_libebl_ppc_pic_a_OBJECTS = $(ppc_SRCS:.c=.os)

ppc64_SRCS = ppc64_init.c ppc64_symbol.c ppc64_retval.c \
	     ppc64_corenote.c ppc_regs.c ppc_auxv.c ppc_attrs.c ppc_syscall.c
libebl_ppc64_pic_a_SOURCES = $(ppc64_SRCS)
am_libebl_ppc64_pic_a_OBJECTS = $(ppc64_SRCS:.c=.os)

s390_SRCS = s390_init.c s390_symbol.c s390_regs.c s390_retval.c
libebl_s390_pic_a_SOURCES = $(s390_SRCS)
am_libebl_s390_pic_a_OBJECTS = $(s390_SRCS:.c=.os)


libebl_%.so: libebl_%_pic.a libebl_%.map $(libelf) $(libdw) \
	     $(cpu_$(@:libebl_%.so=%)
	$(LINK) -shared -o $@ -Wl,--whole-archive,$<\
		$(cpu_$(@:libebl_%.so=%)) -Wl,--no-whole-archive \
		-Wl,--version-script,$(word 2,$^) \
		-Wl,-z,defs -Wl,--as-needed $(libelf) $(libdw) $(libmudflap)
	$(textrel_check)

# XXX Should not be needed...
libebl_i386.so: $(cpu_i386)
libebl_x86_64.so: $(cpu_x86_64)

libebl_%.map: Makefile
	echo 'ELFUTILS_$(PACKAGE_VERSION) { global: $*_init; local: *; };' > $@

%.os: %.c
	if $(COMPILE) -c -o $@ -fpic -DPIC -DSHARED -MT $@ -MD -MP \
	  -MF "$(DEPDIR)/$*.Tpo" `test -f '$<' || echo '$(srcdir)/'`$<; \
	then mv -f "$(DEPDIR)/$*.Tpo" "$(DEPDIR)/$*.Po"; \
	else rm -f "$(DEPDIR)/$*.Tpo"; exit 1; \
	fi

install: install-am install-ebl-modules
install-ebl-modules:
	$(mkinstalldirs) $(DESTDIR)$(libdir)/$(LIBEBL_SUBDIR)
	for m in $(modules); do \
	  $(INSTALL_PROGRAM) libebl_$${m}.so $(DESTDIR)$(libdir)/$(LIBEBL_SUBDIR)/libebl_$${m}-$(PACKAGE_VERSION).so; \
	  ln -fs libebl_$${m}-$(PACKAGE_VERSION).so $(DESTDIR)$(libdir)/$(LIBEBL_SUBDIR)/libebl_$${m}.so; \
	done

uninstall: uninstall-am
	for m in $(modules); do \
	  rm -f $(DESTDIR)$(libdir)/$(LIBEBL_SUBDIR)/libebl_$${m}-$(PACKAGE_VERSION).so; \
	  rm -f $(DESTDIR)$(libdir)/$(LIBEBL_SUBDIR)/libebl_$${m}.so; \
	done
	rmdir --ignore-fail-on-non-empty $(DESTDIR)$(libdir)/$(LIBEBL_SUBDIR)

noinst_HEADERS = libebl_CPU.h common-reloc.c linux-core-note.c x86_corenote.c
EXTRA_DIST = $(foreach m,$(modules),$($(m)_SRCS)) $(modules:=_reloc.def)

CLEANFILES = *.gcno *.gcda \
	     $(foreach m,$(modules),\
		       libebl_$(m).so $(am_libebl_$(m)_pic_a_OBJECTS))
