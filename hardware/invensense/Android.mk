# Can't have both 65xx and 60xx sensors.
ifneq ($(filter hammerhead, $(TARGET_DEVICE)),)
# hammerhead expects 65xx sensors.
include $(call all-named-subdir-makefiles,65xx)
else
# manta expects 60xx sensors.
include $(call all-named-subdir-makefiles,60xx)
endif
