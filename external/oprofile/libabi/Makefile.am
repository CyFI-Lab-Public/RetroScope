SUBDIRS=. tests

LIBS=@POPT_LIBS@ @LIBERTY_LIBS@

AM_CPPFLAGS = \
	-I ${top_srcdir}/libop \
	-I ${top_srcdir}/libutil \
	-I ${top_srcdir}/libdb \
	-I ${top_srcdir}/libopt++

AM_CXXFLAGS = @OP_CXXFLAGS@

noinst_LIBRARIES = libabi.a
libabi_a_SOURCES = abi.cpp abi.h op_abi.c op_abi.h

bin_PROGRAMS = opimport

opimport_SOURCES = opimport.cpp
opimport_LDADD = \
	libabi.a \
	../libdb/libodb.a \
	../libopt++/libopt++.a \
	../libutil++/libutil++.a \
	../libutil/libutil.a
