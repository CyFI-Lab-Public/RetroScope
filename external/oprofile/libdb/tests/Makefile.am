AM_CPPFLAGS = \
	-I ${top_srcdir}/libop \
	-I ${top_srcdir}/libutil \
	-I ${top_srcdir}/libdb

AM_CFLAGS = @OP_CFLAGS@

LIBS = @LIBERTY_LIBS@

check_PROGRAMS = db_test

db_test_SOURCES = db_test.c
db_test_LDADD = ../libodb.a ../../libutil/libutil.a

TESTS = ${check_PROGRAMS}
