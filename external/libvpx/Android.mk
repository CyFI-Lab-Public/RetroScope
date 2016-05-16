LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# libvpx
# if ARMv7 + NEON etc blah blah
include external/libvpx/libvpx.mk

# libwebm
include external/libvpx/libwebm.mk
