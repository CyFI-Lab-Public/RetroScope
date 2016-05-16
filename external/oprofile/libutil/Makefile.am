SUBDIRS = . tests

AM_CPPFLAGS=-I ${top_srcdir}/libop
AM_CFLAGS = @OP_CFLAGS@

noinst_LIBRARIES = libutil.a
libutil_a_SOURCES = \
	op_deviceio.c \
	op_lockfile.c \
	op_file.c \
	op_fileio.c \
	op_get_time.c \
	op_libiberty.c \
	op_popt.c \
	op_string.c \
	op_cpufreq.c \
	op_deviceio.h \
	op_fileio.h \
	op_libiberty.h \
	op_lockfile.h \
	op_types.h \
	op_file.h \
	op_get_time.h \
	op_list.h \
	op_popt.h \
	op_string.h \
	op_cpufreq.h \
	op_version.c \
	op_version.h \
	op_growable_buffer.c \
	op_growable_buffer.h
