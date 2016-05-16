LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:=                 \
        TextDescriptions.cpp      \
        TimedTextDriver.cpp       \
        TimedText3GPPSource.cpp \
        TimedTextSource.cpp       \
        TimedTextSRTSource.cpp    \
        TimedTextPlayer.cpp

LOCAL_CFLAGS += -Wno-multichar
LOCAL_C_INCLUDES:= \
        $(TOP)/frameworks/av/include/media/stagefright/timedtext \
        $(TOP)/frameworks/av/media/libstagefright

LOCAL_MODULE:= libstagefright_timedtext

include $(BUILD_STATIC_LIBRARY)
