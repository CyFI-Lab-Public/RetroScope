####################################
# Build modp_b64 as separate library

include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cc
LOCAL_MODULE:= modp_b64
LOCAL_MODULE_TAGS:= optional

LOCAL_SRC_FILES := \
    third_party/modp_b64/modp_b64.cc

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/third_party/modp_b64

# Including this will modify the include path
include external/stlport/libstlport.mk

LOCAL_CFLAGS := -DHAVE_CONFIG_H -DANDROID -fvisibility=hidden

include $(BUILD_STATIC_LIBRARY)

