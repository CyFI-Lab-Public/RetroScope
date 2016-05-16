SUBDIRS = liblegacy .

oprofiled_SOURCES = \
	init.c \
	oprofiled.c \
	oprofiled.h \
	opd_stats.c \
	opd_pipe.c \
	opd_pipe.h \
	opd_sfile.c \
	opd_sfile.h \
	opd_kernel.c \
	opd_kernel.h \
	opd_trans.c \
	opd_trans.h \
	opd_printf.h \
	opd_stats.h \
	opd_cookie.c \
	opd_cookie.h \
	opd_events.c \
	opd_events.h \
	opd_interface.h \
	opd_mangling.c \
	opd_mangling.h \
	opd_perfmon.h \
	opd_perfmon.c \
	opd_anon.h \
	opd_anon.c \
	opd_spu.c \
	opd_extended.h \
	opd_extended.c \
	opd_ibs.h \
	opd_ibs.c \
	opd_ibs_macro.h \
	opd_ibs_trans.h \
	opd_ibs_trans.c

LIBS=@POPT_LIBS@ @LIBERTY_LIBS@

AM_CPPFLAGS = \
	-I ${top_srcdir}/libabi \
	-I ${top_srcdir}/libutil \
	-I ${top_srcdir}/libop \
	-I ${top_srcdir}/libdb

# -fno-omit-frame-pointer needed for daemon build: see ChangeLog-2004 02-23
AM_CFLAGS = @OP_CFLAGS@ -fno-omit-frame-pointer

bin_PROGRAMS = oprofiled

oprofiled_LDADD = \
	liblegacy/liblegacy.a \
	../libabi/libabi.a \
	../libdb/libodb.a \
	../libop/libop.a \
	../libutil/libutil.a

oprofiled_LINK = $(CC) $(AM_CFLAGS) $(CFLAGS) $(AM_LDFLAGS) $(LDFLAGS) -o $@
