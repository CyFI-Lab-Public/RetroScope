LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

cts_src_test_path := bionic/tests
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := bionic-unit-tests-cts

LOCAL_ADDITION_DEPENDENCIES := \
	$(LOCAL_PATH)/Android.mk \

LOCAL_SHARED_LIBRARIES += \
	libstlport \
	libdl \

LOCAL_WHOLE_STATIC_LIBRARIES += \
	libBionicTests \

LOCAL_STATIC_LIBRARIES += \
	libgtest \
	libgtest_main \

LOCAL_MODULE_PATH := $(TARGET_OUT_DATA)/nativetest
LOCAL_CTS_TEST_PACKAGE := android.bionic

include $(BUILD_CTS_EXECUTABLE)
