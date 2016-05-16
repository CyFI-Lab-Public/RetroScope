LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := args.c bt_timeline.c devmap.c devs.c dip_rb.c iostat.c \
                   latency.c misc.c output.c proc.c seek.c trace.c \
                   trace_complete.c trace_im.c trace_issue.c \
                   trace_queue.c trace_remap.c trace_requeue.c \
                   ../rbtree.c mmap.c trace_plug.c bno_dump.c \
                   unplug_hist.c q2d.c aqd.c plat.c

LOCAL_C_INCLUDES := external/blktrace external/blktrace/btt

LOCAL_CFLAGS := -O2 -g -W -Wall -D_LARGEFILE_SOURCE -D_FILE_OFFSET_BITS=64 \
                -D_ANDROID_

LOCAL_MODULE := btt
LOCAL_MODULE_TAGS :=
LOCAL_SYSTEM_SHARED_LIBRARIES := libc
include $(BUILD_EXECUTABLE)
