#
# Copyright (C) 2013 Google Inc.
#

# The version code scheme for the package apk is:
#      Mmbbbtad
# where
#    M - major version (one or more digits)
#    m - minor version (exactly 1 digit)
#  bbb - manually specified build number (exactly 3 digits)
#    t - build type (exactly 1 digit).  Current valid values are:
#           0 : eng build
#           1 : build server build
#    a - device architecture (exactly 1 digit).  Current valid values are:
#           0 : non-native
#           1 : armv5te
#           3 : armv7-a
#           5 : mips
#           7 : x86
#    d - asset density (exactly 1 digit).  Current valid values are:
#           0 : all densities
#           2 : mdpi
#           4 : hdpi
#           6 : xhdpi
# Mmbbb is specified manually.  tad is automatically set during the build.
#
# For the client jar, the version code is agnostic to the target architecture and density: Mmbbbt00
#
# NOTE: arch needs to be more significant than density because x86 devices support running ARM
# code in emulation mode, so all x86 versions must be higher than all ARM versions to ensure
# we deliver true x86 code to those devices.
#
# HISTORY:
# 2.0.001 - Factory ROM and 0-day OTA 4.4 (KK)
# 2.0.002 - 4.4 MR1 system image

# Specify the following manually.  Note that base_version_minor must be exactly 1 digit and
# base_version_build must be exactly 3 digits.
base_version_major := 2
base_version_minor := 0
base_version_build := 002

#####################################################
#####################################################
# Collect automatic version code parameters
ifneq "" "$(filter eng.%,$(BUILD_NUMBER))"
  # This is an eng build
  base_version_buildtype := 0
else
  # This is a build server build
  base_version_buildtype := 1
endif

ifeq "$(TARGET_ARCH)" "x86"
  base_version_arch := 7
else ifeq "$(TARGET_ARCH)" "mips"
  base_version_arch := 5
else ifeq "$(TARGET_ARCH)" "arm"
  ifeq ($(TARGET_ARCH_VARIANT),armv5te)
    base_version_arch := 1
  else
    base_version_arch := 3
  endif
else
  base_version_arch := 0
endif

ifeq "$(package_dpi)" "mdpi"
  base_version_density := 2
else ifeq "$(package_dpi)" "hdpi"
  base_version_density := 4
else ifeq "$(package_dpi)" "xhdpi"
  base_version_density := 6
else
  base_version_density := 0
endif

# Build the version code
version_code_package := $(base_version_major)$(base_version_minor)$(base_version_build)$(base_version_buildtype)$(base_version_arch)$(base_version_density)

# The version name scheme for the package apk is:
# - For eng build (t=0):     M.m.bbb eng.$(USER)-hh
# - For build server (t=1):  M.m.bbb (nnnnnn-hh)
#       where nnnnnn is the build number from the build server (no zero-padding)
# On eng builds, the BUILD_NUMBER has the user and timestamp inline
ifneq "" "$(filter eng.%,$(BUILD_NUMBER))"
  git_hash := $(shell git --git-dir $(LOCAL_PATH)/.git log -n 1 --pretty=format:%h)
  date_string := $(shell date +%m%d%y_%H%M%S)
  version_name_package := $(base_version_major).$(base_version_minor).$(base_version_build) (eng.$(USER).$(git_hash).$(date_string)-$(base_version_arch)$(base_version_density))
else
  version_name_package := $(base_version_major).$(base_version_minor).$(base_version_build) ($(BUILD_NUMBER)-$(base_version_arch)$(base_version_density))
endif

# Cleanup the locals
base_version_major :=
base_version_minor :=
base_version_build :=
base_version_buildtype :=
base_version_arch :=
base_version_density :=
git_hash :=
date_string :=
