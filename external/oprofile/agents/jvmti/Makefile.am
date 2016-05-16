AM_CFLAGS = @OP_CFLAGS@

pkglib_LTLIBRARIES = libjvmti_oprofile.la

libjvmti_oprofile_la_CFLAGS = $(AM_CFLAGS) -fPIC

libjvmti_oprofile_la_LIBADD = ../../libopagent/libopagent.la

libjvmti_oprofile_la_SOURCES = libjvmti_oprofile.c

AM_CPPFLAGS = \
       -I ${top_srcdir}/libopagent \
       -I @JAVA_HOMEDIR@/include \
       -I @JAVA_HOMEDIR@/include/linux
