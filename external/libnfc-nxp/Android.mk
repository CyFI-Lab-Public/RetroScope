LOCAL_PATH:= $(call my-dir)

#
# libnfc
#

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm

#phLibNfc
LOCAL_SRC_FILES:= \
	src/phLibNfc.c \
	src/phLibNfc_discovery.c \
	src/phLibNfc_initiator.c \
	src/phLibNfc_llcp.c \
	src/phLibNfc_Ioctl.c \
	src/phLibNfc_ndef_raw.c \
	src/phLibNfc_SE.c \
	src/phLibNfc_target.c

#phHalNfc
LOCAL_SRC_FILES += src/phHal4Nfc_ADD.c
LOCAL_SRC_FILES += src/phHal4Nfc.c
LOCAL_SRC_FILES += src/phHal4Nfc_Emulation.c
LOCAL_SRC_FILES += src/phHal4Nfc_P2P.c
LOCAL_SRC_FILES += src/phHal4Nfc_Reader.c

#phDnldNfc
LOCAL_SRC_FILES += src/phDnldNfc.c

#phHciNfc
LOCAL_SRC_FILES += src/phHciNfc_AdminMgmt.c
LOCAL_SRC_FILES += src/phHciNfc.c
LOCAL_SRC_FILES += src/phHciNfc_CE_A.c
LOCAL_SRC_FILES += src/phHciNfc_CE_B.c
LOCAL_SRC_FILES += src/phHciNfc_DevMgmt.c
LOCAL_SRC_FILES += src/phHciNfc_Emulation.c
LOCAL_SRC_FILES += src/phHciNfc_Felica.c
LOCAL_SRC_FILES += src/phHciNfc_Generic.c
LOCAL_SRC_FILES += src/phHciNfc_IDMgmt.c
LOCAL_SRC_FILES += src/phHciNfc_ISO15693.c
LOCAL_SRC_FILES += src/phHciNfc_Jewel.c
LOCAL_SRC_FILES += src/phHciNfc_LinkMgmt.c
LOCAL_SRC_FILES += src/phHciNfc_NfcIPMgmt.c
LOCAL_SRC_FILES += src/phHciNfc_Pipe.c
LOCAL_SRC_FILES += src/phHciNfc_PollingLoop.c
LOCAL_SRC_FILES += src/phHciNfc_RFReaderA.c
LOCAL_SRC_FILES += src/phHciNfc_RFReaderB.c
LOCAL_SRC_FILES += src/phHciNfc_RFReader.c
LOCAL_SRC_FILES += src/phHciNfc_Sequence.c
LOCAL_SRC_FILES += src/phHciNfc_SWP.c
LOCAL_SRC_FILES += src/phHciNfc_WI.c

#phLlcNfc
LOCAL_SRC_FILES += src/phLlcNfc.c
LOCAL_SRC_FILES += src/phLlcNfc_Frame.c
LOCAL_SRC_FILES += src/phLlcNfc_Interface.c
LOCAL_SRC_FILES += src/phLlcNfc_StateMachine.c
LOCAL_SRC_FILES += src/phLlcNfc_Timer.c

#phFricNfc_Llcp
LOCAL_SRC_FILES += src/phFriNfc_Llcp.c
LOCAL_SRC_FILES += src/phFriNfc_LlcpUtils.c
LOCAL_SRC_FILES += src/phFriNfc_LlcpTransport.c
LOCAL_SRC_FILES += src/phFriNfc_LlcpTransport_Connectionless.c
LOCAL_SRC_FILES += src/phFriNfc_LlcpTransport_Connection.c
LOCAL_SRC_FILES += src/phFriNfc_LlcpMac.c
LOCAL_SRC_FILES += src/phFriNfc_LlcpMacNfcip.c

#phFriNfc_NdefMap
LOCAL_SRC_FILES += src/phFriNfc_FelicaMap.c
LOCAL_SRC_FILES += src/phFriNfc_MifareStdMap.c
LOCAL_SRC_FILES += src/phFriNfc_MifareULMap.c
LOCAL_SRC_FILES += src/phFriNfc_MapTools.c
LOCAL_SRC_FILES += src/phFriNfc_TopazMap.c
LOCAL_SRC_FILES += src/phFriNfc_TopazDynamicMap.c
LOCAL_SRC_FILES += src/phFriNfc_DesfireMap.c
LOCAL_SRC_FILES += src/phFriNfc_ISO15693Map.c
LOCAL_SRC_FILES += src/phFriNfc_NdefMap.c
LOCAL_SRC_FILES += src/phFriNfc_IntNdefMap.c

#phFriNfc_NdefReg
LOCAL_SRC_FILES += src/phFriNfc_NdefReg.c

#phFriNfc_SmtCrdFmt
LOCAL_SRC_FILES += src/phFriNfc_DesfireFormat.c
LOCAL_SRC_FILES += src/phFriNfc_MifULFormat.c
LOCAL_SRC_FILES += src/phFriNfc_MifStdFormat.c
LOCAL_SRC_FILES += src/phFriNfc_SmtCrdFmt.c
LOCAL_SRC_FILES += src/phFriNfc_ISO15693Format.c

#phFriNfc_OvrHal
LOCAL_SRC_FILES += src/phFriNfc_OvrHal.c

#phOsalNfc
LOCAL_SRC_FILES += Linux_x86/phOsalNfc_Timer.c
LOCAL_SRC_FILES += Linux_x86/phOsalNfc.c
LOCAL_SRC_FILES += Linux_x86/phOsalNfc_Utils.c

#phDal4Nfc
LOCAL_SRC_FILES += Linux_x86/phDal4Nfc_uart.c
LOCAL_SRC_FILES += Linux_x86/phDal4Nfc.c
LOCAL_SRC_FILES += Linux_x86/phDal4Nfc_i2c.c
LOCAL_SRC_FILES += Linux_x86/phDal4Nfc_messageQueueLib.c

LOCAL_CFLAGS += -DNXP_MESSAGING -DANDROID -DNFC_TIMER_CONTEXT -fno-strict-aliasing

ifeq ($(TARGET_HAS_NFC_CUSTOM_CONFIG),true)
LOCAL_CFLAGS += -DNFC_CUSTOM_CONFIG_INCLUDE
LOCAL_CFLAGS += -I$(TARGET_OUT_HEADERS)/libnfc-nxp
endif

# Uncomment for Chipset command/responses
# Or use "setprop debug.nfc.LOW_LEVEL_TRACES" at run-time
# LOCAL_CFLAGS += -DLOW_LEVEL_TRACES

# Uncomment for DAL traces
# LOCAL_CFLAGS += -DDEBUG -DDAL_TRACE

# Uncomment for LLC traces
# LOCAL_CFLAGS += -DDEBUG -DLLC_TRACE

# Uncomment for LLCP traces
# LOCAL_CFLAGS += -DDEBUG -DLLCP_TRACE

# Uncomment for HCI traces
# LOCAL_CFLAGS += -DDEBUG -DHCI_TRACE

#includes
LOCAL_CFLAGS += -I$(LOCAL_PATH)/inc
LOCAL_CFLAGS += -I$(LOCAL_PATH)/Linux_x86
LOCAL_CFLAGS += -I$(LOCAL_PATH)/src

LOCAL_MODULE:= libnfc
LOCAL_MODULE_TAGS := optional
LOCAL_SHARED_LIBRARIES := libcutils libnfc_ndef libdl libhardware liblog

include $(BUILD_SHARED_LIBRARY)

#
# libnfc_ndef
#

include $(CLEAR_VARS)

LOCAL_SRC_FILES += src/phFriNfc_NdefRecord.c

LOCAL_CFLAGS += -I$(LOCAL_PATH)/inc
LOCAL_CFLAGS += -I$(LOCAL_PATH)/src

LOCAL_MODULE:= libnfc_ndef
LOCAL_MODULE_TAGS := optional
LOCAL_SHARED_LIBRARIES := libcutils liblog

include $(BUILD_SHARED_LIBRARY)
