## Process this file with automake to create Makefile.in
##
## Copyright (C) 2002, 2004, 2005, 2007, 2008 Red Hat, Inc.
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
AM_CFLAGS += -Wall -Wshadow -Wunused -Wextra -std=gnu99 -fpic \
	     $($(*F)_CFLAGS) \
	     $(if $($(*F)_no_Werror),,-Werror)
INCLUDES = -I$(srcdir) -I$(srcdir)/../lib -I$(srcdir)/../libelf \
	   -I$(srcdir)/../libebl -I$(srcdir)/../libdw -I$(srcdir)/../libasm
LEXCOMPILE = $(LEX) $(LFLAGS) $(AM_LFLAGS) -P$(<F:lex.l=)
LEX_OUTPUT_ROOT = lex.$(<F:lex.l=)
AM_YFLAGS = -p$(<F:parse.y=)

if MUDFLAP
libmudflap = -lmudflap
endif

noinst_LIBRARIES = libcpu_i386.a libcpu_x86_64.a
noinst_PROGRAMS = i386_gendis

libcpu_i386_a_SOURCES = i386_disasm.c
libcpu_x86_64_a_SOURCES = x86_64_disasm.c

i386_gendis_SOURCES = i386_gendis.c i386_lex.l i386_parse.y

i386_disasm.o: i386.mnemonics i386_dis.h
x86_64_disasm.o: x86_64.mnemonics x86_64_dis.h i386_disasm.c

i386_dis.h: i386_gendis $(srcdir)/defs/i386
	m4 -Di386 -DDISASSEMBLER $(srcdir)/defs/i386 | ./i386_gendis - > $@
x86_64_dis.h: i386_gendis $(srcdir)/defs/i386
	m4 -Dx86_64 -DDISASSEMBLER $(srcdir)/defs/i386 | ./i386_gendis - > $@

i386.mnemonics x86_64.mnemonics: %.mnemonics: $(srcdir)/defs/i386
	m4 -D$(@:.mnemonics=) -DDISASSEMBLER $^ \
	| sed '1,/^%%/d;/^#/d;/^[[:space:]]*$$/d;s/[^:]*:\([^[:space:]]*\).*/MNE(\1)/;s/{[^}]*}//g;/INVALID/d' \
	| sort -u > $@

i386_lex_no_Werror = yes

libeu = ../lib/libeu.a

i386_lex_CFLAGS = -Wno-unused-label -Wno-unused-function -Wno-sign-compare
i386_parse.o: i386_parse.c i386.mnemonics
i386_parse_CFLAGS = -DNMNES=$$(wc -l < i386.mnemonics)
i386_lex.o: i386_parse.h
i386_gendis_LDADD = $(libeu) -lm $(libmudflap)

i386_parse.h: i386_parse.c ;

noinst_HEADERS = memory-access.h i386_parse.h i386_data.h

EXTRA_DIST = defs/i386

CLEANFILES = i386.mnemonics i386_dis.h x86_64.mnemonics x86_64_dis.h
