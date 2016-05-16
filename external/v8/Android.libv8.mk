##
LOCAL_PATH := $(call my-dir)
# libv8.so
# ===================================================
include $(CLEAR_VARS)

include external/stlport/libstlport.mk

ifeq ($(TARGET_ARCH),mips)
       LOCAL_MIPS_MODE=mips
endif

# Set up the target identity
LOCAL_MODULE := libv8
LOCAL_MODULE_CLASS := STATIC_LIBRARIES
intermediates := $(call local-intermediates-dir)

# Android.v8common.mk defines common V8_LOCAL_SRC_FILES
# and V8_LOCAL_JS_LIBRARY_FILES
V8_LOCAL_SRC_FILES :=
V8_LOCAL_JS_LIBRARY_FILES :=
V8_LOCAL_JS_EXPERIMENTAL_LIBRARY_FILES :=
include $(LOCAL_PATH)/Android.v8common.mk

# Target can only be linux
V8_LOCAL_SRC_FILES += \
  src/platform-linux.cc \
  src/platform-posix.cc

ifeq ($(TARGET_ARCH),x86)
V8_LOCAL_SRC_FILES += src/atomicops_internals_x86_gcc.cc
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
GEN1 := $(intermediates)/libraries.cc
$(GEN1): SCRIPT := $(intermediates)/js2c.py
$(GEN1): $(LOCAL_JS_LIBRARY_FILES) $(JS2C_PY)
	@echo "Generating libraries.cc"
	@mkdir -p $(dir $@)
	python $(SCRIPT) $(GEN1) CORE off $(LOCAL_JS_LIBRARY_FILES)
V8_GENERATED_LIBRARIES := $(intermediates)/libraries.cc

# Generate experimental-libraries.cc
GEN2 := $(intermediates)/experimental-libraries.cc
$(GEN2): SCRIPT := $(intermediates)/js2c.py
$(GEN2): $(LOCAL_JS_EXPERIMENTAL_LIBRARY_FILES) $(JS2C_PY)
	@echo "Generating experimental-libraries.cc"
	@mkdir -p $(dir $@)
	python $(SCRIPT) $(GEN2) EXPERIMENTAL off $(LOCAL_JS_EXPERIMENTAL_LIBRARY_FILES)
V8_GENERATED_LIBRARIES += $(intermediates)/experimental-libraries.cc

LOCAL_GENERATED_SOURCES += $(V8_GENERATED_LIBRARIES)

# Generate snapshot.cc
ifeq ($(ENABLE_V8_SNAPSHOT),true)
SNAP_GEN := $(intermediates)/snapshot.cc
MKSNAPSHOT := $(HOST_OUT_EXECUTABLES)/mksnapshot.$(TARGET_ARCH)
$(SNAP_GEN): PRIVATE_CUSTOM_TOOL = $(MKSNAPSHOT) --logfile $(intermediates)/v8.log $(SNAP_GEN)
$(SNAP_GEN): $(MKSNAPSHOT)
	$(transform-generated-source)
LOCAL_GENERATED_SOURCES += $(SNAP_GEN)
else
LOCAL_SRC_FILES += \
  src/snapshot-empty.cc
endif

# The -fvisibility=hidden option below prevents exporting of symbols from
# libv8.a in libwebcore.so.  That reduces size of libwebcore.so by 500k.
LOCAL_CFLAGS += \
	-Wno-endif-labels \
	-Wno-import \
	-Wno-format \
	-fno-exceptions \
	-fvisibility=hidden \
	-DENABLE_DEBUGGER_SUPPORT \
	-DENABLE_LOGGING_AND_PROFILING \
	-DENABLE_VMSTATE_TRACKING \
	-DV8_NATIVE_REGEXP

ifeq ($(TARGET_ARCH),arm)
	LOCAL_CFLAGS += -DARM -DV8_TARGET_ARCH_ARM
endif

ifeq ($(TARGET_ARCH),mips)
	LOCAL_CFLAGS += -DV8_TARGET_ARCH_MIPS
	LOCAL_CFLAGS += -Umips
	LOCAL_CFLAGS += -finline-limit=64
	LOCAL_CFLAGS += -fno-strict-aliasing
endif

ifeq ($(TARGET_ARCH),x86)
	LOCAL_CFLAGS += -DV8_TARGET_ARCH_IA32 -fno-pic
endif

ifeq ($(DEBUG_V8),true)
	LOCAL_CFLAGS += -DDEBUG -UNDEBUG
endif

LOCAL_C_INCLUDES += $(LOCAL_PATH)/src

include $(BUILD_STATIC_LIBRARY)
