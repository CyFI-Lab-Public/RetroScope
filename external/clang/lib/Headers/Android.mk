LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

$(TARGET_OUT_HEADERS)/clang/arm_neon.h: TBLGEN_LOCAL_MODULE := arm_neon.h
$(TARGET_OUT_HEADERS)/clang/arm_neon.h: $(CLANG_ROOT_PATH)/include/clang/Basic/arm_neon.td \
    | $(CLANG_TBLGEN)
	$(call transform-host-clang-td-to-out,arm-neon)

# Make sure when clang is used, arm_neon.h does exist.
$(CLANG): | $(TARGET_OUT_HEADERS)/clang/arm_neon.h
