
Summary
=======
This directory contains tests for Slang that use the 'llvm-lit' testing tool.
Each testcase is a separate .rs file, and comments inside the testcase are
used to verify certain strings are present in the output bitcode files.

Prerequisites
=============
To run the tests, you must have the android build environment variables
set (i.e. source build/envsetup.sh; lunch). You must also have on your path:
- Android version of llvm-lit (currently in libbcc/tests/debuginfo)
- FileCheck (utility from llvm)
- llvm-rs-cc (slang frontend compiler)

If you are unable to run the tests, try using the "--debug" option to llvm-lit.

When debugging failures, the "-v" option will print to stdout the details of
the failure. Note that tests marked as "Expected Fail" (XFAIL) will not print
failure information, even with -v.

Customizing
===========
The tools lit and FileCheck are fairly flexible, and could be used to validate
more than just emitted bitcode. For example, with some changes to the testcases
and the helper shell-script rs-filecheck-wrapper.sh, it should be possible to
write tests that verify the emitted Java code.

Running
=======
To execute all the tests from this directory, use the Android llvm-lit tool
from libbcc:
$ ../../libbcc/tests/debuginfo/llvm-lit .

The tool can be run from any directory.
-j controls the number of parallel test executions
-v enables additional verbosity (useful when examining unexpected failures)

Adding new tests
================
To add new tests, just add .rs files to a test directory with similar
RUN/CHECK directives in comments as the existing tests.
