AM_CXXFLAGS = -Wall -Weffc++
#AM_CXXFLAGS = -Wall -Weffc++ -D_DEBUG

lib_LIBRARIES = libmarisa.a

libmarisa_a_SOURCES = \
  marisa/base.cc \
  marisa/progress.cc \
  marisa/mapper.cc \
  marisa/reader.cc \
  marisa/writer.cc \
  marisa/intvector.cc \
  marisa/bitvector.cc \
  marisa/tail.cc \
  marisa/trie.cc \
  marisa/trie-build.cc \
  marisa/trie-search.cc \
  marisa/trie-c.cc

include_HEADERS = marisa.h

libmarisa_includedir = ${includedir}/marisa
libmarisa_include_HEADERS = \
  marisa/base.h \
  marisa/popcount.h \
  marisa/rank.h \
  marisa/string.h \
  marisa/key.h \
  marisa/progress.h \
  marisa/range.h \
  marisa/query.h \
  marisa/container.h \
  marisa/cell.h \
  marisa/mapper.h \
  marisa/reader.h \
  marisa/writer.h \
  marisa/io.h \
  marisa/vector.h \
  marisa/vector-inline.h \
  marisa/intvector.h \
  marisa/bitvector.h \
  marisa/tail.h \
  marisa/trie.h \
  marisa/trie-inline.h
