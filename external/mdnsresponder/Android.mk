LOCAL_PATH := $(call my-dir)

#########################

include $(CLEAR_VARS)
LOCAL_SRC_FILES :=  mDNSPosix/PosixDaemon.c    \
                    mDNSPosix/mDNSPosix.c      \
                    mDNSPosix/mDNSUNP.c        \
                    mDNSCore/mDNS.c            \
                    mDNSCore/DNSDigest.c       \
                    mDNSCore/uDNS.c            \
                    mDNSCore/DNSCommon.c       \
                    mDNSShared/uds_daemon.c    \
                    mDNSShared/mDNSDebug.c     \
                    mDNSShared/dnssd_ipc.c     \
                    mDNSShared/GenLinkedList.c \
                    mDNSShared/PlatformCommon.c

LOCAL_MODULE := mdnsd
LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES := external/mdnsresponder/mDNSPosix \
                    external/mdnsresponder/mDNSCore  \
                    external/mdnsresponder/mDNSShared

LOCAL_CFLAGS := -O2 -g -W -Wall -D__ANDROID__ -D_GNU_SOURCE -DHAVE_IPV6 -DNOT_HAVE_SA_LEN -DUSES_NETLINK -DTARGET_OS_LINUX -fno-strict-aliasing -DHAVE_LINUX -DMDNS_DEBUGMSGS=0 -DMDNS_UDS_SERVERPATH=\"/dev/socket/mdnsd\" -DMDNS_USERNAME=\"mdnsr\" -DPLATFORM_NO_RLIMIT
LOCAL_SYSTEM_SHARED_LIBRARIES := libc
LOCAL_SHARED_LIBRARIES := libcutils liblog

include $(BUILD_EXECUTABLE)

##########################

include $(CLEAR_VARS)
LOCAL_SRC_FILES :=  mDNSShared/dnssd_clientlib.c  \
                    mDNSShared/dnssd_clientstub.c \
                    mDNSShared/dnssd_ipc.c

LOCAL_MODULE := libmdnssd
LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS := -O2 -g -W -Wall -D__ANDROID__ -D_GNU_SOURCE -DHAVE_IPV6 -DNOT_HAVE_SA_LEN -DUSES_NETLINK -DTARGET_OS_LINUX -fno-strict-aliasing -DHAVE_LINUX -DMDNS_UDS_SERVERPATH=\"/dev/socket/mdnsd\" -DMDNS_DEBUGMSGS=0
LOCAL_SYSTEM_SHARED_LIBRARIES := libc
LOCAL_SHARED_LIBRARIES := libcutils liblog

include $(BUILD_SHARED_LIBRARY)

############################

include $(CLEAR_VARS)
LOCAL_SRC_FILES :=  Clients/dns-sd.c \
                    Clients/ClientCommon.c

LOCAL_MODULE := dnssd
LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES := external/mdnsresponder/mDNSShared

LOCAL_CFLAGS := -O2 -g -W -Wall -D__ANDROID__ -D_GNU_SOURCE -DHAVE_IPV6 -DNOT_HAVE_SA_LEN -DUSES_NETLINK -DTARGET_OS_LINUX -fno-strict-aliasing -DHAVE_LINUX -DMDNS_UDS_SERVERPATH=\"/dev/socket/mdnsd\" -DMDNS_DEBUGMSGS=0

LOCAL_SYSTEM_SHARED_LIBRARIES := libc
LOCAL_SHARED_LIBRARIES := libmdnssd libcutils liblog

include $(BUILD_EXECUTABLE)
