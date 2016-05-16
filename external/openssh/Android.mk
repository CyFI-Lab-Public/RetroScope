LOCAL_PATH:= $(call my-dir)

###################### libssh ######################
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
    acss.c authfd.c authfile.c bufaux.c bufbn.c buffer.c \
    canohost.c channels.c cipher.c cipher-acss.c cipher-aes.c \
    cipher-bf1.c cipher-ctr.c cipher-3des1.c cleanup.c \
    compat.c compress.c crc32.c deattack.c fatal.c hostfile.c \
    log.c match.c md-sha256.c moduli.c nchan.c packet.c \
    readpass.c rsa.c ttymodes.c xmalloc.c addrmatch.c \
    atomicio.c key.c dispatch.c kex.c mac.c uidswap.c uuencode.c misc.c \
    monitor_fdpass.c rijndael.c ssh-dss.c ssh-ecdsa.c ssh-rsa.c dh.c \
    kexdh.c kexgex.c kexdhc.c kexgexc.c bufec.c kexecdh.c kexecdhc.c \
    msg.c progressmeter.c dns.c entropy.c gss-genr.c umac.c jpake.c \
    schnorr.c ssh-pkcs11.c roaming_dummy.c \
    openbsd-compat/strtonum.c openbsd-compat/bsd-misc.c \
    openbsd-compat/timingsafe_bcmp.c openbsd-compat/bsd-getpeereid.c \
    openbsd-compat/readpassphrase.c openbsd-compat/vis.c \
    openbsd-compat/port-tun.c openbsd-compat/setproctitle.c \
    openbsd-compat/bsd-closefrom.c  openbsd-compat/getopt.c \
    openbsd-compat/rresvport.c openbsd-compat/bindresvport.c \
    openbsd-compat/bsd-statvfs.c openbsd-compat/xmmap.c \
    openbsd-compat/port-linux.c openbsd-compat/strmode.c \
    openbsd-compat/bsd-openpty.c \
    openbsd-compat/fmt_scaled.c \
    openbsd-compat/pwcache.c openbsd-compat/glob.c

#    openbsd-compat/getrrsetbyname.c
#    openbsd-compat/xcrypt.c 

LOCAL_C_INCLUDES := external/openssl/include external/zlib
PRIVATE_C_INCLUDES := external/openssl/openbsd-compat

LOCAL_SHARED_LIBRARIES += libssl libcrypto libdl libz

LOCAL_MODULE := libssh

LOCAL_CFLAGS+=-O3

include $(BUILD_SHARED_LIBRARY)

###################### ssh ######################

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
    ssh.c readconf.c clientloop.c sshtty.c \
    sshconnect.c sshconnect1.c sshconnect2.c mux.c \
    roaming_common.c roaming_client.c

LOCAL_MODULE := ssh

LOCAL_C_INCLUDES := external/openssl/include
PRIVATE_C_INCLUDES := external/openssl/openbsd-compat

LOCAL_SHARED_LIBRARIES += libssh libssl libcrypto libdl libz

include $(BUILD_EXECUTABLE)

###################### sftp ######################

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
    sftp.c sftp-client.c sftp-common.c sftp-glob.c progressmeter.c

LOCAL_MODULE := sftp

LOCAL_C_INCLUDES := external/openssl/include
PRIVATE_C_INCLUDES := external/openssl/openbsd-compat

LOCAL_SHARED_LIBRARIES += libssh libssl libcrypto libdl libz

include $(BUILD_EXECUTABLE)

###################### scp ######################

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
    scp.c progressmeter.c bufaux.c

LOCAL_MODULE := scp

LOCAL_C_INCLUDES := external/openssl/include
PRIVATE_C_INCLUDES := external/openssl/openbsd-compat

LOCAL_SHARED_LIBRARIES += libssh libssl libcrypto libdl libz

include $(BUILD_EXECUTABLE)

###################### sshd ######################

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
    sshd.c auth-rhosts.c auth-rsa.c auth-rh-rsa.c \
	audit.c audit-bsm.c audit-linux.c platform.c \
	sshpty.c sshlogin.c servconf.c serverloop.c \
	auth.c auth1.c auth2.c auth-options.c session.c \
	auth-chall.c auth2-chall.c groupaccess.c \
	auth-skey.c auth-bsdauth.c auth2-hostbased.c auth2-kbdint.c \
	auth2-none.c auth2-passwd.c auth2-pubkey.c auth2-jpake.c \
	monitor_mm.c monitor.c monitor_wrap.c kexdhs.c kexgexs.c kexecdhs.c \
	auth-krb5.c \
	auth2-gss.c gss-serv.c gss-serv-krb5.c \
	loginrec.c auth-pam.c auth-shadow.c auth-sia.c md5crypt.c \
	sftp-server.c sftp-common.c \
	roaming_common.c roaming_serv.c \
	sandbox-null.c sandbox-rlimit.c sandbox-systrace.c sandbox-darwin.o

# auth-passwd.c

LOCAL_MODULE := sshd

LOCAL_C_INCLUDES := external/openssl/include external/zlib
PRIVATE_C_INCLUDES := external/openssl/openbsd-compat

LOCAL_SHARED_LIBRARIES += libssh libssl libcrypto libdl libz libcutils

include $(BUILD_EXECUTABLE)

###################### ssh-keygen ######################

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
    ssh-keygen.c

LOCAL_MODULE := ssh-keygen

LOCAL_C_INCLUDES := external/openssl/include
PRIVATE_C_INCLUDES := external/openssl/openbsd-compat

LOCAL_SHARED_LIBRARIES += libssh libssl libcrypto libdl libz

include $(BUILD_EXECUTABLE)

###################### sshd_config ######################

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := sshd_config
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/ssh
LOCAL_SRC_FILES := sshd_config.android
include $(BUILD_PREBUILT)

###################### start-ssh ######################

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := start-ssh
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_SRC_FILES := start-ssh
include $(BUILD_PREBUILT)
