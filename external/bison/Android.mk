# Copyright 2006 The Android Open Source Project
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

LOCAL_PATH:= $(call my-dir)

###########################################
include $(CLEAR_VARS)

LOCAL_MODULE := libbison

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/$(BUILD_OS)-lib \
    $(LOCAL_PATH)/lib

LOCAL_SRC_FILES := \
    lib/abitset.c \
    lib/argmatch.c \
    lib/asnprintf.c \
    lib/basename-lgpl.c \
    lib/basename.c \
    lib/binary-io.c \
    lib/bitrotate.c \
    lib/bitset.c \
    lib/bitset_stats.c \
    lib/bitsetv-print.c \
    lib/bitsetv.c \
    lib/c-ctype.c \
    lib/c-strcasecmp.c \
    lib/c-strncasecmp.c \
    lib/cloexec.c \
    lib/close-stream.c \
    lib/closeout.c \
    lib/dirname-lgpl.c \
    lib/dirname.c \
    lib/dup-safer-flag.c \
    lib/dup-safer.c \
    lib/ebitset.c \
    lib/exitfail.c \
    lib/fatal-signal.c \
    lib/fd-hook.c \
    lib/fd-safer-flag.c \
    lib/fd-safer.c \
    lib/fopen-safer.c \
    lib/fprintf.c \
    lib/fseterr.c \
    lib/get-errno.c \
    lib/hash.c \
    lib/isnand.c \
    lib/isnanf.c \
    lib/isnanl.c \
    lib/lbitset.c \
    lib/localcharset.c \
    lib/math.c \
    lib/mbchar.c \
    lib/mbschr.c \
    lib/mbsrchr.c \
    lib/mbswidth.c \
    lib/mbuiter.c \
    lib/pipe-safer.c \
    lib/pipe2-safer.c \
    lib/pipe2.c \
    lib/printf-args.c \
    lib/printf-frexp.c \
    lib/printf-frexpl.c \
    lib/printf-parse.c \
    lib/printf.c \
    lib/progname.c \
    lib/quotearg.c \
    lib/sig-handler.c \
    lib/snprintf.c \
    lib/spawn-pipe.c \
    lib/sprintf.c \
    lib/stdio.c \
    lib/strerror_r.c \
    lib/stripslash.c \
    lib/strnlen1.c \
    lib/timevar.c \
    lib/unistd.c \
    lib/vasnprintf.c \
    lib/vbitset.c \
    lib/vfprintf.c \
    lib/vsnprintf.c \
    lib/vsprintf.c \
    lib/wait-process.c \
    lib/wctype-h.c \
    lib/xalloc-die.c \
    lib/xmalloc.c \
    lib/xmemdup0.c \
    lib/xsize.c \
    lib/xstrndup.c \
    lib/yyerror.c \
    lib/glthread/lock.c \
    lib/glthread/threadlib.c \
    lib/uniwidth/width.c

ifeq ($(BUILD_OS),darwin)
LOCAL_SRC_FILES += \
    lib/error.c \
    lib/fpending.c \
    lib/getdelim.c \
    lib/getline.c \
    lib/getopt.c \
    lib/getopt1.c \
    lib/obstack.c \
    lib/obstack_printf.c \
    lib/open.c \
    lib/perror.c \
    lib/rawmemchr.c \
    lib/stat.c \
    lib/strchrnul.c \
    lib/strerror-override.c \
    lib/strerror.c \
    lib/strndup.c \
    lib/strnlen.c \
    lib/strverscmp.c \
    lib/wcwidth.c
endif

ifeq ($(BUILD_OS),linux)
LOCAL_SRC_FILES += \
    lib/fcntl.c
endif

include $(BUILD_HOST_STATIC_LIBRARY)
###########################################

include $(CLEAR_VARS)

LOCAL_MODULE := bison

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/$(BUILD_OS)-lib \
    $(LOCAL_PATH)/lib

LOCAL_CFLAGS := -DPKGDATADIR=\"$(LOCAL_PATH)/data\"

LOCAL_STATIC_LIBRARIES := libbison

LOCAL_SRC_FILES := \
    src/AnnotationList.c \
    src/InadequacyList.c \
    src/LR0.c \
    src/Sbitset.c \
    src/assoc.c \
    src/closure.c \
    src/complain.c \
    src/conflicts.c \
    src/derives.c \
    src/files.c \
    src/getargs.c \
    src/gram.c \
    src/graphviz.c \
    src/ielr.c \
    src/lalr.c \
    src/location.c \
    src/main.c \
    src/muscle-tab.c \
    src/named-ref.c \
    src/nullable.c \
    src/output.c \
    src/parse-gram.c \
    src/print-xml.c \
    src/print.c \
    src/print_graph.c \
    src/reader.c \
    src/reduce.c \
    src/relation.c \
    src/scan-code-c.c \
    src/scan-gram-c.c \
    src/scan-skel-c.c \
    src/state.c \
    src/symlist.c \
    src/symtab.c \
    src/tables.c \
    src/uniqstr.c

include $(BUILD_HOST_EXECUTABLE)
