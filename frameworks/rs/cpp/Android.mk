rs_cpp_SRC_FILES := \
	RenderScript.cpp \
	BaseObj.cpp \
	Element.cpp \
	Type.cpp \
	Allocation.cpp \
	Script.cpp \
	ScriptC.cpp \
	ScriptIntrinsics.cpp \
	Sampler.cpp

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

ifeq "REL" "$(PLATFORM_VERSION_CODENAME)"
  RS_VERSION := $(PLATFORM_SDK_VERSION)
else
  # Increment by 1 whenever this is not a final release build, since we want to
  # be able to see the RS version number change during development.
  # See build/core/version_defaults.mk for more information about this.
  RS_VERSION := "(1 + $(PLATFORM_SDK_VERSION))"
endif
local_cflags_for_rs_cpp += -DRS_VERSION=$(RS_VERSION)

LOCAL_SRC_FILES := $(rs_cpp_SRC_FILES)

LOCAL_CFLAGS += $(local_cflags_for_rs_cpp)

LOCAL_SHARED_LIBRARIES := \
	libz \
	libcutils \
	libutils \
	liblog \
	libdl \
	libstlport

LOCAL_MODULE:= libRScpp

LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES += frameworks/rs
LOCAL_C_INCLUDES += external/stlport/stlport bionic/ bionic/libstdc++/include
LOCAL_C_INCLUDES += $(intermediates)

include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)

LOCAL_CFLAGS += $(local_cflags_for_rs_cpp)

LOCAL_SRC_FILES := $(rs_cpp_SRC_FILES)

LOCAL_STATIC_LIBRARIES := \
	libz \
	libcutils \
	libutils \
	liblog \
	libstlport_static

LOCAL_SHARED_LIBRARIES := libdl

LOCAL_MODULE:= libRScpp_static

LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES += frameworks/rs
LOCAL_C_INCLUDES += external/stlport/stlport bionic/ bionic/libstdc++/include
LOCAL_C_INCLUDES += $(intermediates)

include $(BUILD_STATIC_LIBRARY)
