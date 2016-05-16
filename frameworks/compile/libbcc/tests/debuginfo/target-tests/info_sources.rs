// RUN: %build_test_apk --driver driver-simple --out %t --testcase %s %build_test_apk_opts
// RUN: %Test_jit_debuginfo %s %t
// DEBUGGER: source android-commands.py
// DEBUGGER: load-android-app %t
// DEBUGGER: run-android-app
// DEBUGGER: info sources
// CHECK: info_sources/info_sources.rs

#pragma version(1)
#pragma rs java_package_name(%PACKAGE%)

static int function_with_a_segfault() {
  int* bla = 0;
  *bla = 5;
  return 0;
}

void entry() {
  function_with_a_segfault();
}
