AM_CFLAGS = -Wall -D_DEBUG -I../lib
AM_CXXFLAGS = -Wall -Weffc++ -D_DEBUG -I../lib

TESTS = \
  unit-test \
  io-test \
  vector-test \
  tail-test \
  trie-test \
  c-test

check_PROGRAMS = $(TESTS)

noinst_HEADERS = assert.h

unit_test_SOURCES = unit-test.cc
unit_test_LDADD = ../lib/libmarisa.a

io_test_SOURCES = io-test.cc
io_test_LDADD = ../lib/libmarisa.a

vector_test_SOURCES = vector-test.cc
vector_test_LDADD = ../lib/libmarisa.a

tail_test_SOURCES = tail-test.cc
tail_test_LDADD = ../lib/libmarisa.a

trie_test_SOURCES = trie-test.cc
trie_test_LDADD = ../lib/libmarisa.a

c_test_SOURCES = c-test.c
c_test_LDADD = ../lib/libmarisa.a -lstdc++
