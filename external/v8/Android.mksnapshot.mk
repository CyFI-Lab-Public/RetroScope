##
# mksnapshot
# ===================================================
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# Set up the target identity
LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE := mksnapshot.$(TARGET_ARCH)
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS = optional
intermediates := $(call local-intermediates-dir)

V8_LOCAL_SRC_FILES :=
V8_LOCAL_JS_LIBRARY_FILES :=
V8_LOCAL_JS_EXPERIMENTAL_LIBRARY_FILES :=
include $(LOCAL_PATH)/Android.v8common.mk

V8_LOCAL_SRC_FILES += \
  src/mksnapshot.cc \
  src/snapshot-empty.cc

ifeq ($(TARGET_ARCH),arm)
V8_LOCAL_SRC_FILES += src/arm/simulator-arm.cc
endif

ifeq ($(TARGET_ARCH),mips)
V8_LOCAL_SRC_FILES += src/mips/simulator-mips.cc

endif

ifeq ($(HOST_ARCH),x86)
V8_LOCAL_SRC_FILES += src/atomicops_internals_x86_gcc.cc
endif

ifeq ($(HOST_OS),linux)
V8_LOCAL_SRC_FILES += \
  src/platform-linux.cc \
  src/platform-posix.cc
endif
ifeq ($(HOST_OS),darwin)
V8_LOCAL_SRC_FILES += \
  src/platform-macos.cc \
  src/platform-posix.cc
endif

LOCAL_SRC_FILES := $(V8_LOCAL_SRC_FILES)

LOCAL_JS_LIBRARY_FILES := $(addprefix $(LOCAL_PATH)/, $(V8_LOCAL_JS_LIBRARY_FILES))
LOCAL_JS_EXPERIMENTAL_LIBRARY_FILES := $(addprefix $(LOCAL_PATH)/, $(V8_LOCAL_JS_EXPERIMENTAL_LIBRARY_FILES))

# Copy js2c.py to intermediates directory and invoke there to avoid generating
# jsmin.pyc in the source directory
JS2C_PY := $(intermediates)/js2c.py $(intermediates)/jsmin.py
$(JS2C_PY): $(intermediates)/%.py : $(LOCAL_PATH)/tools/%.py | $(ACP)
	@echo "Copying $@"
	$(copy-file-to-target)

# Generate libraries.cc
GEN3 := $(intermediates)/libraries.cc
$(GEN3): SCRIPT := $(intermediates)/js2c.py
$(GEN3): $(LOCAL_JS_LIBRARY_FILES) $(JS2C_PY)
	@echo "Generating libraries.cc"
	@mkdir -p $(dir $@)
	python $(SCRIPT) $(GEN3) CORE off $(LOCAL_JS_LIBRARY_FILES)
LOCAL_GENERATED_SOURCES := $(intermediates)/libraries.cc

# Generate experimental-libraries.cc
GEN4 := $(intermediates)/experimental-libraries.cc
$(GEN4): SCRIPT := $(intermediates)/js2c.py
$(GEN4): $(LOCAL_JS_EXPERIMENTAL_LIBRARY_FILES) $(JS2C_PY)
	@echo "Generating experimental-libraries.cc"
	@mkdir -p $(dir $@)
	python $(SCRIPT) $(GEN4) EXPERIMENTAL off $(LOCAL_JS_EXPERIMENTAL_LIBRARY_FILES)
LOCAL_GENERATED_SOURCES += $(intermediates)/experimental-libraries.cc

LOCAL_CFLAGS := \
	-Wno-endif-labels \
	-Wno-import \
	-Wno-format \
	-ansi \
	-fno-rtti \
	-DENABLE_DEBUGGER_SUPPORT \
	-DENABLE_LOGGING_AND_PROFILING \
	-DENABLE_VMSTATE_TRACKING \
	-DV8_NATIVE_REGEXP

ifeq ($(TARGET_ARCH),arm)
  LOCAL_CFLAGS += -DV8_TARGET_ARCH_ARM
endif

ifeq ($(TARGET_CPU_ABI),armeabi-v7a)
    ifeq ($(ARCH_ARM_HAVE_VFP),true)
        LOCAL_CFLAGS += -DCAN_USE_VFP_INSTRUCTIONS -DCAN_USE_ARMV7_INSTRUCTIONS
    endif
endif

ifeq ($(TARGET_ARCH),mips)
  LOCAL_CFLAGS += -DV8_TARGET_ARCH_MIPS
  LOCAL_CFLAGS += -DCAN_USE_FPU_INSTRUCTIONS
  LOCAL_CFLAGS += -Umips
  LOCAL_CFLAGS += -finline-limit=64
  LOCAL_CFLAGS += -fno-strict-aliasing
endif

ifeq ($(TARGET_ARCH),x86)
  LOCAL_CFLAGS += -DV8_TARGET_ARCH_IA32
endif

ifeq ($(DEBUG_V8),true)
	LOCAL_CFLAGS += -DDEBUG -UNDEBUG
endif

LOCAL_C_INCLUDES := $(LOCAL_PATH)/src

# This is on host.
LOCAL_LDLIBS := -lpthread

LOCAL_STATIC_LIBRARIES := liblog

include $(BUILD_HOST_EXECUTABLE)
