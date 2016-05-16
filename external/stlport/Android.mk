LOCAL_PATH := $(call my-dir)

libstlport_src_files := \
	src/dll_main.cpp \
	src/fstream.cpp \
	src/strstream.cpp \
	src/sstream.cpp \
	src/ios.cpp \
	src/stdio_streambuf.cpp \
	src/istream.cpp \
	src/ostream.cpp \
	src/iostream.cpp \
	src/codecvt.cpp \
	src/collate.cpp \
	src/ctype.cpp \
	src/monetary.cpp \
	src/num_get.cpp \
	src/num_put.cpp \
	src/num_get_float.cpp \
	src/num_put_float.cpp \
	src/numpunct.cpp \
	src/time_facets.cpp \
	src/messages.cpp \
	src/locale.cpp \
	src/locale_impl.cpp \
	src/locale_catalog.cpp \
	src/facets_byname.cpp \
	src/complex.cpp \
	src/complex_io.cpp \
	src/complex_trig.cpp \
	src/string.cpp \
	src/bitset.cpp \
	src/allocators.cpp \
	src/c_locale.c \
	src/cxa.c \

libstlport_c_includes := \
	external/stlport/stlport \
	bionic \
        bionic/libstdc++/include \

libstlport_cflags := -D_GNU_SOURCE
libstlport_cppflags := -fuse-cxa-atexit

##########################################
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(libstlport_src_files)

# This is needed to work around the problem that libm.so in NDK does not
# have the sincos*() functions.

LOCAL_SRC_FILES += src/sincos.c

LOCAL_MODULE := libstlport
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk

LOCAL_CFLAGS := $(libstlport_cflags)
LOCAL_CPPFLAGS := $(libstlport_cppflags)
LOCAL_C_INCLUDES := $(libstlport_c_includes)
include $(BUILD_SHARED_LIBRARY)

##########################################
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(libstlport_src_files)

LOCAL_MODULE := libstlport_static
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk

LOCAL_CFLAGS := $(libstlport_cflags)
LOCAL_CPPFLAGS := $(libstlport_cppflags)
LOCAL_C_INCLUDES := $(libstlport_c_includes)
include $(BUILD_STATIC_LIBRARY)
