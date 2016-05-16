AM_CPPFLAGS = \
	-I ${top_srcdir}/libutil \
	-I ${top_srcdir}/libop

AM_CFLAGS = @OP_CFLAGS@ -DOPROFILE_SRCDIR=\"@top_srcdir@\"

COMMON_LIBS = ../libop.a ../../libutil/libutil.a

LIBS = @LIBERTY_LIBS@

check_PROGRAMS = \
	cpu_type_tests \
	parse_event_tests \
	load_events_files_tests \
	alloc_counter_tests \
	mangle_tests

cpu_type_tests_SOURCES = cpu_type_tests.c
cpu_type_tests_LDADD = ${COMMON_LIBS}

parse_event_tests_SOURCES = parse_event_tests.c
parse_event_tests_LDADD = ${COMMON_LIBS}

alloc_counter_tests_SOURCES = alloc_counter_tests.c
alloc_counter_tests_LDADD = ${COMMON_LIBS}

load_events_files_tests_SOURCES = load_events_files_tests.c
load_events_files_tests_LDADD = ${COMMON_LIBS}

mangle_tests_SOURCES = mangle_tests.c
mangle_tests_LDADD = ${COMMON_LIBS}

TESTS = ${check_PROGRAMS}
