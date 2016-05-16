####################################
# Build libevent as separate library

include $(CLEAR_VARS)

LOCAL_MODULE:= libevent
LOCAL_MODULE_TAGS:= optional

LOCAL_SRC_FILES := \
    third_party/libevent/event.c \
    third_party/libevent/evutil.c \
    third_party/libevent/epoll.c \
    third_party/libevent/log.c \
    third_party/libevent/poll.c \
    third_party/libevent/select.c \
    third_party/libevent/signal.c

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/third_party/libevent \
    $(LOCAL_PATH)/third_party/libevent/android

LOCAL_CFLAGS := -DHAVE_CONFIG_H -DANDROID -fvisibility=hidden

include $(BUILD_STATIC_LIBRARY)
