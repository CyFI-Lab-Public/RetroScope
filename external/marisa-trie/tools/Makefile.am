AM_CFLAGS = -Wall -I../lib
AM_CXXFLAGS = -Wall -Weffc++ -I../lib

bin_PROGRAMS = \
  marisa-build \
  marisa-lookup \
  marisa-benchmark \
  marisa-find \
  marisa-predict

noinst_HEADERS = cmdopt.h

marisa_build_SOURCES = marisa-build.cc cmdopt.c
marisa_build_LDADD = ../lib/libmarisa.a

marisa_lookup_SOURCES = marisa-lookup.cc cmdopt.c
marisa_lookup_LDADD = ../lib/libmarisa.a

marisa_find_SOURCES = marisa-find.cc cmdopt.c
marisa_find_LDADD = ../lib/libmarisa.a

marisa_predict_SOURCES = marisa-predict.cc cmdopt.c
marisa_predict_LDADD = ../lib/libmarisa.a

marisa_benchmark_SOURCES = marisa-benchmark.cc cmdopt.c
marisa_benchmark_LDADD = ../lib/libmarisa.a
