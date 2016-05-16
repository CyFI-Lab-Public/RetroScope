noinst_LIBRARIES = liblegacy.a

# -fno-omit-frame-pointer needed for daemon build: see ChangeLog-2004 02-23
AM_CFLAGS = @OP_CFLAGS@ -fno-omit-frame-pointer

AM_CPPFLAGS = \
	-I ${top_srcdir}/libutil \
	-I ${top_srcdir}/libop \
	-I ${top_srcdir}/libdb \
	-I ${top_srcdir}/daemon

liblegacy_a_SOURCES = \
	opd_24_stats.c \
	opd_24_stats.h \
	opd_kernel.c \
	opd_proc.c \
	opd_image.c \
	opd_mapping.c \
	opd_parse_proc.c \
	opd_image.h \
	opd_mapping.h \
	p_module.h \
	opd_kernel.h \
	opd_parse_proc.h \
	opd_proc.h \
	opd_sample_files.c \
	opd_sample_files.h \
	init.c
