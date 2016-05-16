LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    AudioBufferProviderSource.cpp   \
    AudioStreamOutSink.cpp          \
    AudioStreamInSource.cpp         \
    NBAIO.cpp                       \
    MonoPipe.cpp                    \
    MonoPipeReader.cpp              \
    Pipe.cpp                        \
    PipeReader.cpp                  \
    roundup.c                       \
    SourceAudioBufferProvider.cpp

LOCAL_SRC_FILES += NBLog.cpp

# libsndfile license is incompatible; uncomment to use for local debug only
#LOCAL_SRC_FILES += LibsndfileSink.cpp LibsndfileSource.cpp
#LOCAL_C_INCLUDES += path/to/libsndfile/src
#LOCAL_STATIC_LIBRARIES += libsndfile

# uncomment for systrace
# LOCAL_CFLAGS += -DATRACE_TAG=ATRACE_TAG_AUDIO

LOCAL_MODULE := libnbaio

LOCAL_SHARED_LIBRARIES := \
    libbinder \
    libcommon_time_client \
    libcutils \
    libutils \
    liblog \
    libmedia
# This dependency on libmedia is for SingleStateQueueInstantiations.
# Consider a separate a library for SingleStateQueueInstantiations.

include $(BUILD_SHARED_LIBRARY)
