AM_CXXFLAGS = -Wall -Weffc++
#AM_CXXFLAGS = -Wall -Weffc++ -D_DEBUG
ACLOCAL_AMFLAGS = -I m4

lib_LTLIBRARIES = libmarisa_alpha.la

libmarisa_alpha_la_SOURCES = \
  marisa_alpha/base.cc \
  marisa_alpha/progress.cc \
  marisa_alpha/mapper.cc \
  marisa_alpha/reader.cc \
  marisa_alpha/writer.cc \
  marisa_alpha/intvector.cc \
  marisa_alpha/bitvector.cc \
  marisa_alpha/tail.cc \
  marisa_alpha/trie.cc \
  marisa_alpha/trie-build.cc \
  marisa_alpha/trie-search.cc \
  marisa_alpha/trie-c.cc

include_HEADERS = marisa_alpha.h

libmarisa_alpha_includedir = ${includedir}/marisa_alpha
libmarisa_alpha_include_HEADERS = \
  marisa_alpha/base.h \
  marisa_alpha/popcount.h \
  marisa_alpha/rank.h \
  marisa_alpha/string.h \
  marisa_alpha/key.h \
  marisa_alpha/progress.h \
  marisa_alpha/range.h \
  marisa_alpha/query.h \
  marisa_alpha/container.h \
  marisa_alpha/cell.h \
  marisa_alpha/mapper.h \
  marisa_alpha/reader.h \
  marisa_alpha/writer.h \
  marisa_alpha/io.h \
  marisa_alpha/vector.h \
  marisa_alpha/vector-inline.h \
  marisa_alpha/intvector.h \
  marisa_alpha/bitvector.h \
  marisa_alpha/tail.h \
  marisa_alpha/trie.h \
  marisa_alpha/trie-inline.h
