####################################
# Build dmg_fp as separate library

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cc

LOCAL_MODULE:= dmg_fp

LOCAL_SRC_FILES := \
    base/third_party/dmg_fp/dtoa.cc \
    base/third_party/dmg_fp/g_fmt.cc

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/base/third_party/dmg_fp

LOCAL_CFLAGS := -DHAVE_CONFIG_H -DANDROID

include $(BUILD_STATIC_LIBRARY)
