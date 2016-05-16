
REALPATH= readlink -f

SRCDIR := $(shell $(REALPATH) $(topdir)/libutil++/tests/ )

AM_CPPFLAGS = \
	-I ${top_srcdir}/libutil++ -D SRCDIR="\"$(SRCDIR)/\""

COMMON_LIBS = ../libutil++.a ../../libutil/libutil.a

LIBS = @LIBERTY_LIBS@

AM_CXXFLAGS = @OP_CXXFLAGS@

check_PROGRAMS = \
	string_manip_tests \
	string_filter_tests \
	comma_list_tests \
	file_manip_tests \
	glob_filter_tests \
	path_filter_tests \
	cached_value_tests \
	utility_tests

string_manip_tests_SOURCES = string_manip_tests.cpp
string_manip_tests_LDADD = ${COMMON_LIBS}

string_filter_tests_SOURCES = string_filter_tests.cpp
string_filter_tests_LDADD = ${COMMON_LIBS}

glob_filter_tests_SOURCES = glob_filter_tests.cpp
glob_filter_tests_LDADD = ${COMMON_LIBS}

path_filter_tests_SOURCES = path_filter_tests.cpp
path_filter_tests_LDADD = ${COMMON_LIBS}

comma_list_tests_SOURCES = comma_list_tests.cpp
comma_list_tests_LDADD = ${COMMON_LIBS}

file_manip_tests_SOURCES = file_manip_tests.cpp
file_manip_tests_LDADD = ${COMMON_LIBS}

cached_value_tests_SOURCES = cached_value_tests.cpp
cached_value_tests_LDADD = ${COMMON_LIBS}

utility_tests_SOURCES = utility_tests.cpp
utility_tests_LDADD = ${COMMON_LIBS}

TESTS = ${check_PROGRAMS}
