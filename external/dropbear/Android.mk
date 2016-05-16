LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:=\
	dbutil.c buffer.c \
	dss.c bignum.c \
	signkey.c rsa.c random.c \
	queue.c \
	atomicio.c compat.c  fake-rfc2553.c
LOCAL_SRC_FILES+=\
	common-session.c packet.c common-algo.c common-kex.c \
	common-channel.c common-chansession.c termcodes.c \
	tcp-accept.c listener.c process-packet.c \
	common-runopts.c circbuffer.c
# loginrec.c 
LOCAL_SRC_FILES+=\
	cli-algo.c cli-main.c cli-auth.c cli-authpasswd.c cli-kex.c \
	cli-session.c cli-service.c cli-runopts.c cli-chansession.c \
	cli-authpubkey.c cli-tcpfwd.c cli-channel.c cli-authinteract.c
LOCAL_SRC_FILES+=netbsd_getpass.c

LOCAL_STATIC_LIBRARIES := libtommath libtomcrypt

LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := ssh
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libtommath 
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libtomcrypt/src/headers
LOCAL_CFLAGS += -DDROPBEAR_CLIENT

# we will build openssh version instead
# include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:=\
	scp.c progressmeter.c atomicio.c scpmisc.c

LOCAL_STATIC_LIBRARIES := libtommath libtomcrypt

LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)

LOCAL_MODULE_TAGS := debug

LOCAL_MODULE := scp
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libtommath 
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libtomcrypt/src/headers
LOCAL_CFLAGS += -DDROPBEAR_CLIENT -DPROGRESS_METER

# we will build openssh version instead
# include $(BUILD_EXECUTABLE)


include $(call all-makefiles-under,$(LOCAL_PATH))
