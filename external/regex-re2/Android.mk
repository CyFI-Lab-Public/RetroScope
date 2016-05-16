#
# Copyright 2012 Google Inc. All Rights Reserved.
# Author: idh@google.com (Ian Hodson)
#
# Android makefile for the re2 regexp library.
#

LOCAL_PATH := $(call my-dir)

regexp_re2_files := \
	util/arena.cc \
	util/hash.cc \
	util/rune.cc \
	util/stringpiece.cc \
	util/stringprintf.cc \
	util/strutil.cc \
	util/valgrind.cc \
	re2/bitstate.cc \
	re2/compile.cc \
	re2/dfa.cc \
	re2/filtered_re2.cc \
	re2/mimics_pcre.cc \
	re2/nfa.cc \
	re2/onepass.cc \
	re2/parse.cc \
	re2/perl_groups.cc \
	re2/prefilter.cc \
	re2/prefilter_tree.cc \
	re2/prog.cc \
	re2/re2.cc \
	re2/regexp.cc \
	re2/set.cc \
	re2/simplify.cc \
	re2/tostring.cc \
	re2/unicode_casefold.cc \
	re2/unicode_groups.cc

# Stlport version
# =======================================================
include $(CLEAR_VARS)
LOCAL_MODULE := libregex-re2
LOCAL_MODULE_TAGS := optional
LOCAL_CPP_EXTENSION := .cc
LOCAL_C_INCLUDES += $(LOCAL_PATH)/re2 \
		    external/stlport
LOCAL_SRC_FILES := $(regexp_re2_files)
include external/stlport/libstlport.mk
include $(BUILD_STATIC_LIBRARY)

# Gnustl+rtti version
# =======================================================

include $(CLEAR_VARS)
LOCAL_MODULE := libregex-re2-gnustl-rtti
LOCAL_MODULE_TAGS := optional
LOCAL_CPP_EXTENSION := .cc
LOCAL_C_INCLUDES += $(LOCAL_PATH)/re2
LOCAL_SRC_FILES := $(regexp_re2_files)
LOCAL_CFLAGS += -frtti
LOCAL_NDK_STL_VARIANT := gnustl_static
LOCAL_SDK_VERSION := 14

include $(BUILD_STATIC_LIBRARY)
