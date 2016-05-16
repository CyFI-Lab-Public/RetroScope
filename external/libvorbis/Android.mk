LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := libvorbis
OGG_DIR         := external/libogg
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include $(LOCAL_PATH)/lib $(OGG_DIR)/include

LOCAL_SRC_FILES := lib/analysis.c \
                   lib/barkmel.c \
                   lib/bitrate.c \
                   lib/block.c \
                   lib/codebook.c \
                   lib/envelope.c \
                   lib/floor0.c \
                   lib/floor1.c \
                   lib/info.c \
                   lib/lookup.c \
                   lib/lpc.c \
                   lib/lsp.c \
                   lib/mapping0.c \
                   lib/mdct.c \
                   lib/psy.c \
                   lib/registry.c \
                   lib/res0.c \
                   lib/sharedbook.c \
                   lib/smallft.c \
                   lib/synthesis.c \
                   lib/vorbisenc.c \
                   lib/vorbisfile.c \
                   lib/window.c

LOCAL_STATIC_LIBRARIES := libogg

LOCAL_SDK_VERSION := 14

include $(BUILD_STATIC_LIBRARY)
