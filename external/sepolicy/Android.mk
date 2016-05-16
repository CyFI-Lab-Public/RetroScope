LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

# SELinux policy version.
# Must be <= /selinux/policyvers reported by the Android kernel.
# Must be within the compatibility range reported by checkpolicy -V.
POLICYVERS ?= 26

MLS_SENS=1
MLS_CATS=1024

ifeq ($(TARGET_BUILD_VARIANT),user)
	BOARD_SEPOLICY_IGNORE+=external/sepolicy/su.te
else
	BOARD_SEPOLICY_IGNORE+=external/sepolicy/su_user.te
endif

# Quick edge case error detection for BOARD_SEPOLICY_REPLACE.
# Builds the singular path for each replace file.
sepolicy_replace_paths :=
$(foreach pf, $(BOARD_SEPOLICY_REPLACE), \
  $(if $(filter $(pf), $(BOARD_SEPOLICY_UNION)), \
    $(error Ambiguous request for sepolicy $(pf). Appears in both \
      BOARD_SEPOLICY_REPLACE and BOARD_SEPOLICY_UNION), \
  ) \
  $(eval _paths := $(filter-out $(BOARD_SEPOLICY_IGNORE), \
  $(wildcard $(addsuffix /$(pf), $(BOARD_SEPOLICY_DIRS))))) \
  $(eval _occurrences := $(words $(_paths))) \
  $(if $(filter 0,$(_occurrences)), \
    $(error No sepolicy file found for $(pf) in $(BOARD_SEPOLICY_DIRS)), \
  ) \
  $(if $(filter 1, $(_occurrences)), \
    $(eval sepolicy_replace_paths += $(_paths)), \
    $(error Multiple occurrences of replace file $(pf) in $(_paths)) \
  ) \
  $(if $(filter 0, $(words $(wildcard $(addsuffix /$(pf), $(LOCAL_PATH))))), \
    $(error Specified the sepolicy file $(pf) in BOARD_SEPOLICY_REPLACE, \
      but none found in $(LOCAL_PATH)), \
  ) \
)

# Builds paths for all requested policy files w.r.t
# both BOARD_SEPOLICY_REPLACE and BOARD_SEPOLICY_UNION
# product variables.
# $(1): the set of policy name paths to build
build_policy = $(foreach type, $(1), \
  $(filter-out $(BOARD_SEPOLICY_IGNORE), \
    $(foreach expanded_type, $(notdir $(wildcard $(addsuffix /$(type), $(LOCAL_PATH)))), \
      $(if $(filter $(expanded_type), $(BOARD_SEPOLICY_REPLACE)), \
        $(wildcard $(addsuffix $(expanded_type), $(sort $(dir $(sepolicy_replace_paths))))), \
        $(LOCAL_PATH)/$(expanded_type) \
      ) \
    ) \
    $(foreach union_policy, $(wildcard $(addsuffix /$(type), $(BOARD_SEPOLICY_DIRS))), \
      $(if $(filter $(notdir $(union_policy)), $(BOARD_SEPOLICY_UNION)), \
        $(union_policy), \
      ) \
    ) \
  ) \
)

##################################
include $(CLEAR_VARS)

LOCAL_MODULE := sepolicy
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT)

include $(BUILD_SYSTEM)/base_rules.mk

sepolicy_policy.conf := $(intermediates)/policy.conf
$(sepolicy_policy.conf): PRIVATE_MLS_SENS := $(MLS_SENS)
$(sepolicy_policy.conf): PRIVATE_MLS_CATS := $(MLS_CATS)
$(sepolicy_policy.conf) : $(call build_policy, security_classes initial_sids access_vectors global_macros mls_macros mls policy_capabilities te_macros attributes *.te roles users initial_sid_contexts fs_use genfs_contexts port_contexts)
	@mkdir -p $(dir $@)
	$(hide) m4 -D mls_num_sens=$(PRIVATE_MLS_SENS) -D mls_num_cats=$(PRIVATE_MLS_CATS) -s $^ > $@
	$(hide) sed '/dontaudit/d' $@ > $@.dontaudit

$(LOCAL_BUILT_MODULE) : $(sepolicy_policy.conf) $(HOST_OUT_EXECUTABLES)/checkpolicy
	@mkdir -p $(dir $@)
	$(hide) $(HOST_OUT_EXECUTABLES)/checkpolicy -M -c $(POLICYVERS) -o $@ $<
	$(hide) $(HOST_OUT_EXECUTABLES)/checkpolicy -M -c $(POLICYVERS) -o $(dir $<)/$(notdir $@).dontaudit $<.dontaudit

built_sepolicy := $(LOCAL_BUILT_MODULE)
sepolicy_policy.conf :=

###################################
include $(CLEAR_VARS)

LOCAL_MODULE := file_contexts
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT)

include $(BUILD_SYSTEM)/base_rules.mk

ALL_FC_FILES := $(call build_policy, file_contexts)

$(LOCAL_BUILT_MODULE): PRIVATE_SEPOLICY := $(built_sepolicy)
$(LOCAL_BUILT_MODULE):  $(ALL_FC_FILES) $(built_sepolicy) $(HOST_OUT_EXECUTABLES)/checkfc
	@mkdir -p $(dir $@)
	$(hide) m4 -s $(ALL_FC_FILES) > $@
	$(hide) $(HOST_OUT_EXECUTABLES)/checkfc $(PRIVATE_SEPOLICY) $@

file_contexts :=

##################################
include $(CLEAR_VARS)
LOCAL_MODULE := seapp_contexts
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT)

include $(BUILD_SYSTEM)/base_rules.mk

seapp_contexts.tmp := $(intermediates)/seapp_contexts.tmp
$(seapp_contexts.tmp): $(call build_policy, seapp_contexts)
	@mkdir -p $(dir $@)
	$(hide) m4 -s $^ > $@

$(LOCAL_BUILT_MODULE): PRIVATE_SEPOLICY := $(built_sepolicy)
$(LOCAL_BUILT_MODULE) : $(seapp_contexts.tmp) $(built_sepolicy) $(HOST_OUT_EXECUTABLES)/checkseapp
	@mkdir -p $(dir $@)
	$(HOST_OUT_EXECUTABLES)/checkseapp -p $(PRIVATE_SEPOLICY) -o $@ $<

seapp_contexts.tmp :=
##################################
include $(CLEAR_VARS)

LOCAL_MODULE := property_contexts
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT)

include $(BUILD_SYSTEM)/base_rules.mk

ALL_PC_FILES := $(call build_policy, property_contexts)

$(LOCAL_BUILT_MODULE): PRIVATE_SEPOLICY := $(built_sepolicy)
$(LOCAL_BUILT_MODULE):  $(ALL_PC_FILES) $(built_sepolicy) $(HOST_OUT_EXECUTABLES)/checkfc
	@mkdir -p $(dir $@)
	$(hide) m4 -s $(ALL_PC_FILES) > $@
	$(hide) $(HOST_OUT_EXECUTABLES)/checkfc -p $(PRIVATE_SEPOLICY) $@

property_contexts :=
built_sepolicy :=
##################################

##################################
include $(CLEAR_VARS)

LOCAL_MODULE := selinux-network.sh
LOCAL_SRC_FILES := $(LOCAL_MODULE)
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_EXECUTABLES)

include $(BUILD_PREBUILT)

##################################
include $(CLEAR_VARS)

LOCAL_MODULE := mac_permissions.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/security

include $(BUILD_SYSTEM)/base_rules.mk

# Build keys.conf
mac_perms_keys.tmp := $(intermediates)/keys.tmp
$(mac_perms_keys.tmp) : $(call build_policy, keys.conf)
	@mkdir -p $(dir $@)
	$(hide) m4 -s $^ > $@

ALL_MAC_PERMS_FILES := $(call build_policy, $(LOCAL_MODULE))

$(LOCAL_BUILT_MODULE) : $(mac_perms_keys.tmp) $(HOST_OUT_EXECUTABLES)/insertkeys.py $(ALL_MAC_PERMS_FILES)
	@mkdir -p $(dir $@)
	$(hide) $(HOST_OUT_EXECUTABLES)/insertkeys.py -t $(TARGET_BUILD_VARIANT) -d $(dir $(DEFAULT_SYSTEM_DEV_CERTIFICATE)) -c $(TOP) $< -o $@ $(ALL_MAC_PERMS_FILES)

mac_perms_keys.tmp :=
##################################

build_policy :=
sepolicy_replace_paths :=

include $(call all-makefiles-under,$(LOCAL_PATH))
