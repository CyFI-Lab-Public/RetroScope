AM_CFLAGS = -Wall -I../lib
AM_CXXFLAGS = -Wall -Weffc++ -I../lib

bin_PROGRAMS = \
  marisa_alpha-build \
  marisa_alpha-lookup \
  marisa_alpha-find \
  marisa_alpha-predict \
  marisa_alpha-benchmark

noinst_HEADERS = cmdopt.h

marisa_alpha_build_SOURCES = marisa_alpha-build.cc cmdopt.c
marisa_alpha_build_LDADD = ../lib/libmarisa_alpha.la

marisa_alpha_lookup_SOURCES = marisa_alpha-lookup.cc cmdopt.c
marisa_alpha_lookup_LDADD = ../lib/libmarisa_alpha.la

marisa_alpha_find_SOURCES = marisa_alpha-find.cc cmdopt.c
marisa_alpha_find_LDADD = ../lib/libmarisa_alpha.la

marisa_alpha_predict_SOURCES = marisa_alpha-predict.cc cmdopt.c
marisa_alpha_predict_LDADD = ../lib/libmarisa_alpha.la

marisa_alpha_benchmark_SOURCES = marisa_alpha-benchmark.cc cmdopt.c
marisa_alpha_benchmark_LDADD = ../lib/libmarisa_alpha.la
