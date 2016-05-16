# Copyright (C) 2012 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH := $(call my-dir)

ifeq ($(TARGET_PRODUCT),sdk)
supported_platforms := none
else
supported_platforms := linux-x86 darwin-x86
endif

cur_platform := $(filter $(HOST_OS)-$(HOST_ARCH),$(supported_platforms))

ifdef cur_platform

#
# host libperf
#

include $(CLEAR_VARS)

libperf_src_files := \
	util/added/rbtree.c \
	util/abspath.c \
	util/alias.c \
	util/annotate.c \
	util/bitmap.c \
	util/build-id.c \
	util/callchain.c \
	util/cgroup.c \
	util/color.c \
	util/config.c \
	util/cpumap.c \
	util/ctype.c \
	util/debug.c \
	util/debugfs.c \
	util/environment.c \
	util/event.c \
	util/evlist.c \
	util/evsel.c \
	util/exec_cmd.c \
	util/header.c \
	util/help.c \
	util/hist.c \
	util/hweight.c \
	util/levenshtein.c \
	util/map.c \
	util/pager.c \
	util/parse-events.c \
	util/parse-options.c \
	util/path.c \
	util/probe-event.c \
	util/probe-finder.c \
	util/pstack.c \
	util/quote.c \
	util/run-command.c \
	util/session.c \
	util/sigchain.c \
	util/sort.c \
	util/strbuf.c \
	util/strfilter.c \
	util/string.c \
	util/strlist.c \
	util/svghelper.c \
	util/symbol.c \
	util/thread.c \
	util/thread_map.c \
	util/top.c \
	util/trace-event-info.c \
	util/trace-event-parse.c \
	util/trace-event-read.c \
	util/trace-event-scripting.c \
	util/usage.c \
	util/util.c \
	util/values.c \
	util/wrapper.c \
	util/xyarray.c

LOCAL_SRC_FILES := $(libperf_src_files)

LOCAL_SRC_FILES += \
	arch/arm/util/dwarf-regs.c

LOCAL_CFLAGS := -DNO_NEWT_SUPPORT -DNO_LIBPERL -DNO_LIBPYTHON -DNO_STRLCPY -std=gnu99

LOCAL_CFLAGS += -DHAVE_ANDROID_DEMANGLE
LOCAL_CFLAGS += -DDWARF_SUPPORT

# various macros
LOCAL_CFLAGS += -DETC_PERFCONFIG='"etc/perfconfig"' \
                -DPREFIX='""' \
		-DPERF_EXEC_PATH='"libexec/perf-core"'

# in list.h: entry->next = LIST_POISON1;
LOCAL_CFLAGS += -Wno-pointer-arith

# for __used
LOCAL_CFLAGS += -include $(LOCAL_PATH)/util/include/linux/compiler.h

LOCAL_CFLAGS += \
	-include $(LOCAL_PATH)/host-$(HOST_OS)-fixup/AndroidFixup.h

LOCAL_C_INCLUDES := external/elfutils external/elfutils/libelf external/elfutils/libdw external/elfutils/libdwfl

LOCAL_C_INCLUDES += $(LOCAL_PATH)/host-$(HOST_OS)-fixup

LOCAL_MODULE := libperf

include $(BUILD_HOST_STATIC_LIBRARY)

#
# target libperf
#

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(libperf_src_files)

LOCAL_CFLAGS := -DNO_NEWT_SUPPORT -DNO_LIBPERL -DNO_LIBPYTHON -std=gnu99

LOCAL_CFLAGS += -DHAVE_ANDROID_DEMANGLE
LOCAL_CFLAGS += -DDWARF_SUPPORT

# various macros
LOCAL_CFLAGS += -DETC_PERFCONFIG='"etc/perfconfig"' \
                -DPREFIX='""' \
		-DPERF_EXEC_PATH='"libexec/perf-core"'

# in list.h: entry->next = LIST_POISON1;
LOCAL_CFLAGS += -Wno-pointer-arith

# for __used
LOCAL_CFLAGS += -include $(LOCAL_PATH)/util/include/linux/compiler.h

# for various GNU extensions
LOCAL_CFLAGS += -include external/elfutils/bionic-fixup/AndroidFixup.h

LOCAL_CFLAGS += -Wno-attributes -Werror

LOCAL_C_INCLUDES := external/elfutils external/elfutils/libelf external/elfutils/libdw external/elfutils/libdwfl

LOCAL_MODULE := libperf

include $(BUILD_STATIC_LIBRARY)

#
# host perf
#

include $(CLEAR_VARS)

LOCAL_MODULE := perfhost

perf_src_files := \
	builtin-annotate.c \
	builtin-buildid-cache.c \
	builtin-buildid-list.c \
	builtin-diff.c \
	builtin-evlist.c \
	builtin-help.c \
	builtin-inject.c \
	builtin-kmem.c \
	builtin-kvm.c \
	builtin-list.c \
	builtin-lock.c \
	builtin-probe.c \
	builtin-record.c \
	builtin-report.c \
	builtin-sched.c \
	builtin-script.c \
	builtin-stat.c \
	builtin-timechart.c \
	builtin-top.c \
	perf.c

LOCAL_SRC_FILES := $(perf_src_files)

LOCAL_STATIC_LIBRARIES := libperf libdwfl libdw libebl libelf libgccdemangle

LOCAL_LDLIBS := -lpthread -ldl

# for clock_gettime
ifeq ($(HOST_OS),linux)
LOCAL_LDLIBS += -lrt
endif

# common
LOCAL_CFLAGS := -DNO_NEWT_SUPPORT -DNO_LIBPERL -DNO_LIBPYTHON -DNO_STRLCPY -std=gnu99

LOCAL_CFLAGS += \
	-include $(LOCAL_PATH)/host-$(HOST_OS)-fixup/AndroidFixup.h

# in list.h: entry->next = LIST_POISON1;
LOCAL_CFLAGS += -Wno-pointer-arith

# for __used
LOCAL_CFLAGS += -include $(LOCAL_PATH)/util/include/linux/compiler.h

LOCAL_CFLAGS += -DHAVE_ANDROID_DEMANGLE

# unique
LOCAL_CFLAGS += -DPERF_HTML_PATH='""'
LOCAL_CFLAGS += -DPERF_MAN_PATH='""'
LOCAL_CFLAGS += -DPERF_INFO_PATH='""'
LOCAL_CFLAGS += -DPERF_VERSION='"perf.3.0.8_android"'

LOCAL_C_INCLUDES += $(LOCAL_PATH)/host-$(HOST_OS)-fixup

include $(BUILD_HOST_EXECUTABLE)

#
# target perf
#

include $(CLEAR_VARS)

LOCAL_MODULE := perf
LOCAL_MODULE_TAGS := eng

LOCAL_SRC_FILES := $(perf_src_files)

LOCAL_SRC_FILES += \
	builtin-test.c \
	bench/mem-memcpy.c \
	bench/sched-messaging.c \
	bench/sched-pipe.c \
	arch/arm/util/dwarf-regs.c

LOCAL_STATIC_LIBRARIES := libperf libdwfl libdw libebl libelf libgccdemangle

LOCAL_SHARED_LIBRARIES := libdl

# common
LOCAL_CFLAGS := -DNO_NEWT_SUPPORT -DNO_LIBPERL -DNO_LIBPYTHON -std=gnu99

# in list.h: entry->next = LIST_POISON1;
LOCAL_CFLAGS += -Wno-pointer-arith

# for __used
LOCAL_CFLAGS += -include $(LOCAL_PATH)/util/include/linux/compiler.h

# for various GNU extensions
LOCAL_CFLAGS += -include external/elfutils/bionic-fixup/AndroidFixup.h

LOCAL_CFLAGS += -DHAVE_ANDROID_DEMANGLE

# unique
LOCAL_CFLAGS += -DPERF_HTML_PATH='""'
LOCAL_CFLAGS += -DPERF_MAN_PATH='""'
LOCAL_CFLAGS += -DPERF_INFO_PATH='""'
LOCAL_CFLAGS += -DPERF_VERSION='"perf.3.0.8_android"'

LOCAL_CFLAGS += -Wno-attributes -Werror

include $(BUILD_EXECUTABLE)

endif #cur_platform
