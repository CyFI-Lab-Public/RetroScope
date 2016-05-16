LOCAL_PATH:= $(call my-dir)

ifneq ($(TARGET_BUILD_PDK), true)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:=       \
        DataUriSource.cpp \
        ChromiumHTTPDataSource.cpp \
        support.cpp \
        chromium_http_stub.cpp

LOCAL_C_INCLUDES:= \
        $(TOP)/frameworks/av/media/libstagefright \
        $(TOP)/frameworks/native/include/media/openmax \
        external/chromium \
        external/chromium/android

LOCAL_CFLAGS += -Wno-multichar

LOCAL_SHARED_LIBRARIES += \
        libstlport \
        libchromium_net \
        libutils \
        libcutils \
        liblog \
        libstagefright_foundation \
        libstagefright \
        libdrmframework

include external/stlport/libstlport.mk

LOCAL_MODULE:= libstagefright_chromium_http

LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)
endif
