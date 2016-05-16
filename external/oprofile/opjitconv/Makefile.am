AM_CPPFLAGS = -I ${top_srcdir}/libopagent  -I ${top_srcdir}/libutil -I ${top_srcdir}/daemon
AM_CFLAGS = @OP_CFLAGS@

bin_PROGRAMS = opjitconv

LIBS = @BFD_LIBS@

needed_libs =  \
	../libutil/libutil.a

opjitconv_LDADD = $(needed_libs)

opjitconv_SOURCES = \
	opjitconv.c \
	opjitconv.h \
	conversion.c \
	parse_dump.c \
	jitsymbol.c \
	create_bfd.c \
	debug_line.c
