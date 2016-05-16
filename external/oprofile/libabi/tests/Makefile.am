LIBS=@POPT_LIBS@ @LIBERTY_LIBS@

AM_CPPFLAGS = \
	-I ${top_srcdir}/libabi \
	-I ${top_srcdir}/libop \
	-I ${top_srcdir}/libdb \
	-I ${top_srcdir}/libopt++ \
	-I ${top_srcdir}/libutil

AM_CXXFLAGS = @OP_CXXFLAGS@

check_PROGRAMS = abi_test

abi_test_SOURCES = abi_test.cpp
abi_test_LDADD = \
	../libabi.a \
	../../libop/libop.a \
	../../libdb/libodb.a \
	../../libopt++/libopt++.a \
	../../libutil++/libutil++.a \
	../../libutil/libutil.a
