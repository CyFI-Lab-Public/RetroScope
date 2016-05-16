# Copyright 2008 The Android Open Source Project

LOCAL_PATH := $(my-dir)


# the custom dex'ed emma library ready to put on a device.
# ============================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, core pregenerated)

LOCAL_MODULE := emma

LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_RESOURCE_DIRS := core/res pregenerated/res

LOCAL_SDK_VERSION := 8

include $(BUILD_STATIC_JAVA_LIBRARY)



# the custom emma library to add to an SDK project.
# ============================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, core pregenerated)

LOCAL_MODULE := emmalib

LOCAL_JAVA_RESOURCE_DIRS := core/res pregenerated/res

include $(BUILD_HOST_JAVA_LIBRARY)
