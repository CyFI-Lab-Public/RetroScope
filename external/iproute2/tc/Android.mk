LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES :=  tc.c tc_qdisc.c q_cbq.c tc_util.c tc_class.c tc_core.c m_action.c \
                    m_estimator.c tc_filter.c tc_monitor.c tc_stab.c tc_cbq.c \
                    tc_estimator.c f_u32.c m_police.c q_ingress.c m_mirred.c q_htb.c

LOCAL_MODULE := tc

LOCAL_SYSTEM_SHARED_LIBRARIES := \
	libc libm libdl

LOCAL_SHARED_LIBRARIES += libiprouteutil libnetlink

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../include

LOCAL_CFLAGS := -O2 -g -W -Wall

include $(BUILD_EXECUTABLE)

