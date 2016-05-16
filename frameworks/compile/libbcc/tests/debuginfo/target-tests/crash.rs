// RUN: %build_test_apk --driver driver-simple --out %t --testcase %s %build_test_apk_opts
// RUN: %Test_jit_debuginfo %s %t
// DEBUGGER: source android-commands.py
// DEBUGGER: load-android-app %t
// DEBUGGER: run-android-app
// DEBUGGER: bt
// CHECK: entry

#pragma version(1)
#pragma rs java_package_name(%PACKAGE%)

static int function_with_a_segfault() {
  int* bla = 0;
  *bla = 5;
  return 0;
}

static int some_function() {
  return function_with_a_segfault();
}

static int foo() {
  return some_function();
}

static int bar() {
  return foo();
}

int root() {
  return bar();
}

void entry() {
  bar();
}
