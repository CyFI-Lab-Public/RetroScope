SUBDIRS=. tests

AM_CPPFLAGS = \
	-I ${top_srcdir}/libop \
	-I ${top_srcdir}/libutil

AM_CFLAGS = @OP_CFLAGS@

noinst_LIBRARIES = libodb.a

libodb_a_SOURCES = \
	db_manage.c \
	db_insert.c \
	db_travel.c \
	db_debug.c \
	db_stat.c \
	odb.h

