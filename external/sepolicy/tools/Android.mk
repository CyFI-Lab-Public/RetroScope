LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := checkseapp
LOCAL_MODULE_TAGS := optional
LOCAL_C_INCLUDES := external/libsepol/include/
LOCAL_CFLAGS := -DLINK_SEPOL_STATIC
LOCAL_SRC_FILES := check_seapp.c
LOCAL_STATIC_LIBRARIES := libsepol

include $(BUILD_HOST_EXECUTABLE)

###################################
include $(CLEAR_VARS)

LOCAL_MODULE := checkfc
LOCAL_MODULE_TAGS := optional
LOCAL_C_INCLUDES := external/libsepol/include \
                    external/libselinux/include
LOCAL_SRC_FILES := checkfc.c
LOCAL_STATIC_LIBRARIES := libsepol libselinux

include $(BUILD_HOST_EXECUTABLE)

##################################
include $(CLEAR_VARS)

LOCAL_MODULE := insertkeys.py
LOCAL_SRC_FILES := insertkeys.py
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_TAGS := optional

include $(BUILD_PREBUILT)
