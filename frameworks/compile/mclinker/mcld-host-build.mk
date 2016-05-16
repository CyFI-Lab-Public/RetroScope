include $(LLVM_HOST_BUILD_MK)

LOCAL_CFLAGS := \
  -include $(MCLD_ROOT_PATH)/include/mcld/Config/Config.h \
  $(LOCAL_CFLAGS)

LOCAL_CPPFLAGS := \
  $(LOCAL_CPPFLAGS) \
  -Wall \
  -Wno-unused-parameter \
  -Werror

ifeq ($(MCLD_ENABLE_ASSERTION),true)
  LOCAL_CPPFLAGS += \
    -D_DEBUG \
    -UNDEBUG
endif

LOCAL_C_INCLUDES := \
  $(MCLD_ROOT_PATH)/include \
  $(LLVM_ROOT_PATH) \
  $(LLVM_ROOT_PATH)/include \
  $(LLVM_ROOT_PATH)/host/include \
  $(LOCAL_C_INCLUDES)

LOCAL_IS_HOST_MODULE := true
