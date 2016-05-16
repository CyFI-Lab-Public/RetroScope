#!/bin/bash
#
# Copyright 2011 Google Inc. All Rights Reserved.

function usage() {
  echo Usage: $0 "[PATH_TO_YOUR_LLVM_DIRECTORY]"
  echo This will generate a diff of both Clang and LLVM in the files
  echo diff_clang.txt
  echo diff_llvm.txt
}

BASE_LLVM_DIR_ONCE=0
BASE_LLVM_DIR=$LLVMDIR/llvm

ARGS=`getopt -o h --long help -- "$@"`
eval set -- "$ARGS"

while true; do
  case "$1" in
  -h|--help)
    usage
    exit 0
    ;;
  --)
    shift;
    break
    ;;
  *)
    echo "Internal error!"
    exit 1
    ;;
  esac
done

for ARG; do
  if [ $BASE_LLVM_DIR_ONCE -eq 1 ]; then
    usage
    exit 1
  fi
  BASE_LLVM_DIR_ONCE=1
  BASE_LLVM_DIR=$ARG
done

BASE_CLANG_DIR=$BASE_LLVM_DIR/tools/clang
echo "Using BASE_LLVM_DIR = $BASE_LLVM_DIR"
echo "Using BASE_CLANG_DIR = $BASE_CLANG_DIR"

ANDROID_LLVM_DIR=$PWD/../llvm
ANDROID_CLANG_DIR=$PWD
echo "Using ANDROID_LLVM_DIR = $ANDROID_LLVM_DIR"
echo "Using ANDROID_CLANG_DIR = $ANDROID_CLANG_DIR"

DIFF_FLAGS="-x .git -r"

diff $DIFF_FLAGS $BASE_CLANG_DIR $ANDROID_CLANG_DIR > diff_clang.txt
diff $DIFF_FLAGS $BASE_LLVM_DIR $ANDROID_LLVM_DIR > diff_llvm.txt

exit 0
