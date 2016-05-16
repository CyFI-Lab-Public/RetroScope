LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= ping.c ping_common.c
LOCAL_MODULE := ping
LOCAL_CFLAGS := -DWITHOUT_IFADDRS -Wno-sign-compare
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_CFLAGS := -DWITHOUT_IFADDRS -Wno-sign-compare
LOCAL_SRC_FILES := ping6.c ping_common.c
LOCAL_MODULE := ping6
LOCAL_C_INCLUDES := external/openssl/include
LOCAL_SHARED_LIBRARIES := libcrypto
include $(BUILD_EXECUTABLE)
