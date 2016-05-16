LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	camera_metadata.c

LOCAL_C_INCLUDES:= \
	system/media/camera/include

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	liblog

LOCAL_MODULE := libcamera_metadata
LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS += \
	-Wall \
	-fvisibility=hidden \
	-std=c99


include $(BUILD_SHARED_LIBRARY)
