#!/bin/bash
#
# Assists with importing a new version of Strace for Android.
# Removes source files from the Strace original distribution that are not
# needed on Android.
#
# Run this script after extracting an Strace distribution into external/strace.
#

UNNEEDED_SOURCES="\
  Makefile.am \
  Makefile.in \
  README-freebsd \
  README-sunos4 \
  README-svr4 \
  acinclude.m4 \
  aclocal.m4 \
  config.guess \
  config.h.in \
  config.log \
  config.status \
  config.sub \
  configure \
  configure.ac \
  debian \
  depcomp \
  errnoent.sh \
  freebsd \
  install-sh \
  linux/alpha \
  linux/arm/ioctlent.h.in \
  linux/avr32 \
  linux/bfin \
  linux/hppa \
  linux/i386/ioctlent.h.in \
  linux/ia64 \
  linux/ioctlent.h.in \
  linux/m68k \
  linux/microblaze \
  linux/mips/ioctlent.h.in \
  linux/powerpc \
  linux/s390 \
  linux/s390x \
  linux/sh/ioctlent.h.in \
  linux/sh64 \
  linux/sparc \
  linux/sparc64 \
  linux/tile \
  linux/x86_64 \
  m4 \
  missing \
  mkinstalldirs \
  signalent.sh \
  sunos4 \
  svr4 \
  syscallent.sh \
  test \
  tests \
  xlate.el \
"

rm -rvf $UNNEEDED_SOURCES

