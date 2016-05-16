AM_CPPFLAGS = \
	-I ${top_srcdir}/libutil

AM_CFLAGS = @OP_CFLAGS@

LIBS = @LIBERTY_LIBS@

check_PROGRAMS = file_tests string_tests

file_tests_SOURCES = file_tests.c
file_tests_LDADD = ../libutil.a
string_tests_SOURCES = string_tests.c
string_tests_LDADD = ../libutil.a

TESTS = ${check_PROGRAMS}
