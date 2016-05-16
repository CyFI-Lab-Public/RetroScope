LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    libcap-ng-0.7/src/cap-ng.c \
    libcap-ng-0.7/src/lookup_table.c

LOCAL_C_INCLUDES += $(LOCAL_PATH)/libcap-ng-0.7/src
LOCAL_MODULE := libcap-ng
include $(BUILD_SHARED_LIBRARY)

######################

include $(CLEAR_VARS)
LOCAL_SRC_FILES := libcap-ng-0.7/utils/pscap.c
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libcap-ng-0.7/src
LOCAL_SHARED_LIBRARIES += libcap-ng
LOCAL_MODULE := pscap
include $(BUILD_EXECUTABLE)



