LOCAL_PATH:= $(call my-dir)

# We need to build this for both the device (as a shared library)
# and the host (as a static library for tools to use).

common_SRC_FILES := \
	hyphen.c \
	hnjalloc.c

common_C_INCLUDES += $(LOCAL_PATH)

# For the device
# =====================================================

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_C_INCLUDES += $(common_C_INCLUDES)
LOCAL_SHARED_LIBRARIES += $(common_SHARED_LIBRARIES)
LOCAL_CFLAGS += -fvisibility=hidden

LOCAL_MODULE:= libhyphenation

include $(BUILD_STATIC_LIBRARY)


# For the host
# ========================================================

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_C_INCLUDES += $(common_C_INCLUDES)
LOCAL_SHARED_LIBRARIES += $(common_SHARED_LIBRARIES)
LOCAL_MODULE:= libhyphenation
include $(BUILD_HOST_STATIC_LIBRARY)
