pkglib_LTLIBRARIES = libjvmpi_oprofile.la

libjvmpi_oprofile_la_CXXFLAGS = -W -Wall -fPIC

libjvmpi_oprofile_la_SOURCES = jvmpi_oprofile.cpp

libjvmpi_oprofile_la_LIBADD = ../../libopagent/libopagent.la

AM_CPPFLAGS = \
       -I ${top_srcdir}/libopagent \
       -I @JAVA_HOMEDIR@/include \
       -I @JAVA_HOMEDIR@/include/linux
