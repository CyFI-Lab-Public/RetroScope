LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

subdirs := $(addprefix $(LOCAL_PATH)/,$(addsuffix /Android.mk, \
	toolutil \
	ctestfw  \
	makeconv \
	genrb    \
	genbrk   \
	gencnval \
	gensprep \
	icuinfo  \
	genccode \
	gencmn   \
	icupkg   \
	pkgdata  \
	gentest  \
	gennorm2 \
	gencfu   \
	gendict  \
	))

include $(subdirs)
