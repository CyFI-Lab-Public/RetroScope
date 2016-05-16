LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

# embUnit test framework source files
LOCAL_SRC_FILES :=      \
    src/AssertImpl.c    \
    src/RepeatedTest.c  \
    src/stdImpl.c       \
    src/TestCaller.c    \
    src/TestCase.c      \
    src/TestResult.c    \
    src/TestRunner.c    \
    src/TestSuite.c

# Header files path
LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/inc

LOCAL_MODULE_TAGS := tests

LOCAL_MODULE := libembunit

include $(BUILD_SHARED_LIBRARY)
