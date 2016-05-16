LOCAL_PATH:= $(call my-dir)

# ================================================================
# Unit tests for libstagefright_timedtext
# See also /development/testrunner/test_defs.xml
# ================================================================

# ================================================================
# A test for TimedTextSRTSource
# ================================================================
include $(CLEAR_VARS)

LOCAL_MODULE := TimedTextSRTSource_test

LOCAL_MODULE_TAGS := eng tests

LOCAL_SRC_FILES := TimedTextSRTSource_test.cpp

LOCAL_C_INCLUDES := \
    $(TOP)/external/expat/lib \
    $(TOP)/frameworks/base/media/libstagefright/timedtext

LOCAL_SHARED_LIBRARIES := \
    libexpat \
    libstagefright

include $(BUILD_NATIVE_TEST)
