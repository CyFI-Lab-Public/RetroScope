# Build the unit tests.
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

test_src_files := \
    mimeUri_test.cpp \

shared_libraries := \
    libutils \
    libOpenSLES \
    libstlport

static_libraries := \
    libgtest \
    libgtest_main

c_includes := \
    bionic \
    bionic/libstdc++/include \
    external/gtest/include \
    $(call include-path-for, wilhelm) \
    external/stlport/stlport

module_tags := tests

$(foreach file,$(test_src_files), \
    $(eval include $(CLEAR_VARS)) \
    $(eval LOCAL_SHARED_LIBRARIES := $(shared_libraries)) \
    $(eval LOCAL_STATIC_LIBRARIES := $(static_libraries)) \
    $(eval LOCAL_C_INCLUDES := $(c_includes)) \
    $(eval LOCAL_SRC_FILES := $(file)) \
    $(eval LOCAL_MODULE := libopenslestests) \
    $(eval LOCAL_MODULE_PATH := $(TARGET_OUT_DATA)/nativetest) \
    $(eval LOCAL_MODULE_TAGS := $(module_tags)) \
    $(eval include $(BUILD_EXECUTABLE)) \
)

# Build the manual test programs.
include $(call all-makefiles-under,$(LOCAL_PATH))
