AM_CPPFLAGS=-I ${top_srcdir}/libop -I ${top_srcdir}/libutil
AM_CFLAGS = @OP_CFLAGS@

LIBS=@POPT_LIBS@ @LIBERTY_LIBS@

bin_PROGRAMS = ophelp
dist_bin_SCRIPTS = opcontrol

ophelp_SOURCES = ophelp.c
ophelp_LDADD = ../libop/libop.a ../libutil/libutil.a
