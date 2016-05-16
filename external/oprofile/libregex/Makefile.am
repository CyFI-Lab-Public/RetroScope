SUBDIRS = . tests

AM_CPPFLAGS = -I ${top_srcdir}/libutil++
AM_CXXFLAGS = @OP_CXXFLAGS@

noinst_LIBRARIES = libop_regex.a

libop_regex_a_SOURCES = \
	op_regex.cpp \
	op_regex.h \
	demangle_symbol.h \
	demangle_symbol.cpp \
	demangle_java_symbol.h \
	demangle_java_symbol.cpp

datadir = $(prefix)/share/oprofile
nodist_data_DATA = stl.pat
