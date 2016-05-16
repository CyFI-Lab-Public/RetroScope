
Summary
=======
This directory contains integration tests for debug information in libbcc.

The tests come in two flavours: host and target. Host tests are run on the
build machine (currently, only Linux has been tested extensively) and target
tests run on a live Android system (emulator or device.)

Host tests use clang to build bytecode (bc) files, which are then executed
by the libbcc driver utility (bcc) on the host through GDB. The debugger
output is verified against expected output by the llvm tool FileCheck.
Both the debugger commands and the expected output are embedded in the
original sources as comments of the form "DEBUGGER: " and "CHECK: ".

Target tests are similar, but instead of using clang, they use ant and
llvm-rs-cc from the Android SDK to build a test binary package (apk)
that is uploaded to the device (or emulator) and run with GDB attached.
The output is verified in the same way as host side tests, and the format
of the tests is the same.

*** If you are running target-side tests, you must disable parallel
*** execution with the "-j1" flag to llvm-lit


Prerequisites
=============
To run the tests, you must have built the android source tree and have
the build environment variables set (i.e. ANDROID_BUILD_TOP)

You need the following tools (not built by the android build system) on
your PATH:
- gdb     (Tested with gdb 7.3 from Ubuntu 11.10)

In addition, you need a build of gdbserver available in the prebuilt directory.

Customizing
===========
By default, llvm-lit will use the clang and bcc driver built in the android
output directory. If you wish to use different versions of these tools,
set the following environment variables:
CLANG      - path to clang
BCC_DRIVER - path to bcc
FILECHECK  - path to FileCheck
GDB        - path to GDB

Further customization is possible by modifying the lit.cfg file.


Running
=======
To execute all the tests from this directory, use the llvm-lit tool:
$ ./llvm-lit host-tests
$ ./llvm-lit target-tests -j 1

The tool can be run from any directory.
-j controls the number of tests to run in parallel
-v enables additional verbosity (useful when examining unexpected failures)

Adding new tests
================
To add new tests, just add a .c, .cpp, or .rs file to a test directory with
similar RUN/DEBUGGER/CHECK directives in comments as the existing tests.
