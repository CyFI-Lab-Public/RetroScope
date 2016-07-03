#!/bin/sh
#
# Copyright (C) 2011 The Android Open Source Project
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
#

# This script is used to rebuild the Android goldfish arm-based kernel from
# sources.

###
### Utilities
###

# Implementation of 'dirname' that works safely when paths contains spaces
# $1: output variable name
# $2: path (may contain spaces)
# Usage: set_dirname FOO "/path/to/foo with spaces"
set_dirname ()
{
    eval $1="`dirname \"$2\"`"
}

# Same as set_dirname, but for the 'basename' command
set_basename ()
{
    eval $1="`basename \"$2\"`"
}

# Check the status of the previous command, and print a message and exit
# if it didn't succeed.
fail_panic ()
{
    if [ $? != 0 ]; then
        echo "ERROR: $@"
        exit 1
    fi
}

# Send a message to the log. Does nothing if --verbose is not used
log ()
{
    if [ "$VERBOSE" = "yes" ] ; then
        echo "$@"
    fi
}

# Run a comment. Print it to the output first if --verbose is used
run ()
{
    if [ "$VERBOSE" = "yes" ] ; then
        echo "### COMMAND: $@"
        $@
    else
        $@ >/dev/null 2>&1
    fi
}

fail_ifnotdir ()
{
    local _dir="$1"
    if [ ! -d "$_dir" ]; then
        shift
        echo "ERROR: $@ ($_dir)"
        exit 1
    fi
}

# Check wether program '$2' is in your path, and set variable $1 to its
# full path, or to empty otherwise.
# Example: find_program FOO foo
#
# $1: Variable name
# $2: Program name
find_program ()
{
    local FINDPROG_=`which $2 2>/dev/null`
    if [ -n "$FINDPROG_" ]; then
        eval $1="$FINDPROG_";
    else
        eval $1=
    fi
}

set_dirname  PROGDIR  "$0"
set_basename PROGNAME "$0"

###
### Get defaults
###

# Location of prebuilt kernel images relative to Android top-level
PREBUILT_KERNEL_DIR="prebuilts/qemu-kernel/arm"

# Look for top-level Android build source tree if not in environment

if [ -n "$ANDROID_BUILD_TOP" ] ; then
    OUT_DIR="$ANDROID_BUILD_TOP/$PREBUILT_KERNEL_DIR"
else
    OUT_DIR=
fi

TOOLCHAIN_NAME=arm-eabi-4.4.0
TOOLCHAIN_PREFIX=arm-eabi
GCC="$TOOLCHAIN_PREFIX-gcc"

JOBS="`cat /proc/cpuinfo | grep -e "^processor" | wc -l`"

###
### Parse options
###
OPTION_HELP=no
VERBOSE=no
VERBOSE2=no
OPTION_OUT_DIR=
OPTION_CC=

PARAMS=""

for opt do
    optarg=`expr "x$opt" : 'x[^=]*=\(.*\)'`
    case "$opt" in
        --help|-h|-\?) OPTION_HELP=yes
            ;;
        --verbose)
            if [ "$VERBOSE" = "yes" ] ; then
                VERBOSE2=yes
            else
                VERBOSE=yes
            fi
            ;;
        --out-dir=*)
            OPTION_OUT_DIR="$optarg"
            ;;
        --cc=*)
            OPTION_CC="$optarg"
            ;;
        --toolchain=*)
            TOOLCHAIN_NAME="$optarg"
            ;;
        --jobs=*)
            JOBS="$optarg"
            ;;
        -*)
            echo "unknown option '$opt', use --help"
            exit 1
            ;;
        *)
            if [ -z "$PARAMS" ] ; then
                PARAMS="$opt"
            else
                PARAMS="$PARAMS $opt"
            fi
            ;;
    esac
done

if [ "$OPTION_HELP" = "yes" ] ; then
    echo "Usage: $PROGNAME <options> /path/to/kernel"
    echo ""
    echo "Rebuild the prebuilt Android goldfish-specific kernel from sources."
    echo ""
    echo "Options (default are inside brackets):"
    echo ""
    echo "    --help                Print this message."
    echo "    --verbose             Enable verbose output."
    echo "    --android=<path>      Set Android top-level directory [$ANDROID_BUILD_TOP]"
    echo "    --out-dir=<path>      Set output directory [$OUT_DIR]"
    echo "    --toolchain=<name>    Toolchain name [$TOOLCHAIN_NAME]"
    echo "    --cc=<path>           Path to C compiler [$GCC]"
    echo "    --jobs=<count>        Perform <count> parallel builds [$JOBS]"
    echo ""
    echo "The --toolchain option is ignored if you use the --cc one."
    exit 0
fi

###
### Parse parameters
###

set_parameters ()
{
    if [ -z "$1" ] ; then
        echo "ERROR: Please specify path of kernel sources directory. See --help for details."
        exit 1
    fi
    if [ -n "$2" ]; then
        echo "ERROR: Too many arguments. See --help for details."
        exit 1
    fi
    KERNEL_DIR="$1"
    fail_ifnotdir "$KERNEL_DIR" "Missing kernel directory"
    fail_ifnotdir "$KERNEL_DIR/kernel" "Missing kernel-specific directory"
    fail_ifnotdir "$KERNEL_DIR/arch"   "Missing kernel-specific directory"
    fail_ifnotdir "$KERNEL_DIR/arch/arm/mach-goldfish" "Missing goldfish-specific directory"
}

set_parameters $PARAMS

###
### Determine build configuration
###

if [ -n "$OPTION_OUT_DIR" ] ; then
    OUT_DIR="$OPTION_OUT_DIR"
fi

if [ -z "$ANDROID_BUILD_TOP" ] ; then
    # Assume that we are under $ANDROID_BUILD_TOP/prebuilt/android-arm/kernel
    ANDROID_BUILD_TOP="`cd \"$PROGDIR\"/../../.. && pwd`"
    if [ -d "$ANDROID_BUILD_TOP" ]; then
        echo "Probed Android top-level directory: $ANDROID_BUILD_TOP"
    else
        echo "ERROR: Can't find Android top-leveld directory. Please define ANDROID_BUILD_TOP"
        exit 1
    fi
fi

if [ -z "$OUT_DIR" ] ; then
    OUT_DIR="$ANDROID_BUILD_TOP/$PREBUILT_KERNEL_DIR"
    if [ ! -d "$OUT_DIR" ]; then
        echo "ERROR: Missing default output dir: $OUT_DIR"
        echo "Please use --out-dir=<path> to specify alternative output directory."
        exit 1
    fi
fi

echo "Using output directory: $OUT_DIR"
mkdir -p "$OUT_DIR"
fail_panic "Could not create directory: $OUT_DIR"

# Find toolchain

if [ -n "$OPTION_CC" ]; then
    GCC="$OPTION_CC"
else
    find_program GCC $TOOLCHAIN_PREFIX-gcc
fi

if [ -z "$GCC" ]; then
    TOOLCHAIN_DIR="$ANDROID_BUILD_TOP/prebuilt/linux-x86/toolchain/$TOOLCHAIN_NAME/bin"
    if [ ! -d "$TOOLCHAIN_DIR" ] ; then
        echo "ERROR: Missing directory: $TOOLCHAIN_DIR"
        exit 1
    fi
    GCC="$TOOLCHAIN_DIR/$TOOLCHAIN_PREFIX-gcc"
    if [ ! -f "$GCC" ] ; then
        echo "ERROR: Missing program file: $GCC"
        exit 1
    fi
fi

CROSS_COMPILE="`echo $GCC | sed -e 's!gcc$!!g' `"
echo "Using cross-toolchain prefix: $CROSS_COMPILE"

# Do we have ccache in our path?
find_program CCACHE ccache
if [ -n "$CCACHE" ]; then
    echo "Using ccache program: $CCACHE"
    CROSS_COMPILE="ccache $CROSS_COMPILE"
fi

###
### Do the build
###

cd "$KERNEL_DIR"
ARCH=arm
SUBARCH=arm

export CROSS_COMPILE ARCH SUBARCH

# $1: defconfig name (e.g. goldfish or goldfish_armv7)
# $2: output suffix (e.g. "" or "-armv7")
do_build ()
{
    MAKE_FLAGS="-j$JOBS"
    # Use --verbose --verbose to see build commands
    if [ "$VERBOSE2" = "yes" ] ; then
        MAKE_FLAGS="$MAKE_FLAGS V=1"
    fi
    echo "Cleaning up source tree."
    git ls-files -o | xargs rm -f
    echo "Setting up $1_defconfig..."
    run make $1_defconfig
    fail_panic "Could not setup $1_defconfig build!"
    echo "Building $1_deconfig..."
    run make $MAKE_FLAGS
    fail_panic "Could not build $1_defconfig!"
    echo "Copying $1_defconfig binaries to $OUT_DIR."
    cp -f arch/arm/boot/zImage "$OUT_DIR/kernel-qemu$2"
    cp -f vmlinux "$OUT_DIR/vmlinux-qemu$2"
}

do_build goldfish
if [ -f arch/arm/configs/goldfish_armv7_defconfig ]; then
    do_build goldfish_armv7 -armv7
fi

echo "Done."
exit 0
