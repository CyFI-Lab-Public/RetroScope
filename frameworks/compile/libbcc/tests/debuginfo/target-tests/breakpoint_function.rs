// RUN: %build_test_apk --driver driver-simple-exit --out %t --testcase %s %build_test_apk_opts
// RUN: %Test_jit_debuginfo %s %t
// DEBUGGER: source android-commands.py
// DEBUGGER: load-android-app %t
// DEBUGGER: set breakpoint pending on
// DEBUGGER: b entry
// DEBUGGER: run-android-app
// DEBUGGER: bt
// CHECK: entry
// CHECK: breakpoint_function.rs

#pragma version(1)
#pragma rs java_package_name(com.android.test.rsdebug.breakpoint_function)

static int twenty() {
  return 20;
}

static int some_function() {
  return twenty();
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
