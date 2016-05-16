SUBDIRS = . tests

AM_CPPFLAGS= \
	-I ${top_srcdir}/libutil \
	-I ${top_srcdir}/libop \
	-I ${top_srcdir}/libpp

AM_CXXFLAGS = @OP_CXXFLAGS@

noinst_LIBRARIES = libutil++.a
libutil___a_SOURCES = \
	op_bfd.cpp \
	op_bfd.h \
	bfd_support.cpp \
	bfd_support.h \
	string_filter.cpp \
	string_filter.h \
	glob_filter.cpp \
	glob_filter.h \
	growable_vector.h \
	path_filter.cpp \
	path_filter.h \
	file_manip.cpp \
	file_manip.h \
	sparse_array.h \
	stream_util.cpp \
	stream_util.h \
	string_manip.cpp \
	string_manip.h \
	cverb.cpp \
	cverb.h \
	generic_spec.h \
	op_exception.cpp \
	op_exception.h \
	child_reader.cpp \
	child_reader.h \
	unique_storage.h \
	utility.h \
	cached_value.h \
	comma_list.h \
	xml_output.h \
	xml_output.cpp \
	bfd_spu_support.cpp \
	op_spu_bfd.cpp
