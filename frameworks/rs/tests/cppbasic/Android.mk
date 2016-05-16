LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	mono.rs \
	compute.cpp

LOCAL_SHARED_LIBRARIES := \
	libz \
	libEGL \
	libGLESv1_CM \
	libGLESv2 \
	libui \
	libbcc \
	libbcinfo \
	libgui \
	libdl \
	libRScpp \
	libstlport


LOCAL_MODULE:= rstest-compute

LOCAL_MODULE_TAGS := tests

intermediates := $(call intermediates-dir-for,STATIC_LIBRARIES,libRS,TARGET,)

LOCAL_C_INCLUDES += external/stlport/stlport bionic/ bionic/libstdc++/include
LOCAL_C_INCLUDES += frameworks/rs/cpp
LOCAL_C_INCLUDES += frameworks/rs
LOCAL_C_INCLUDES += $(intermediates)


include $(BUILD_EXECUTABLE)

