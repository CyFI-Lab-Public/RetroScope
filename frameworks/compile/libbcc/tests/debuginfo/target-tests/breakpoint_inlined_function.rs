// RUN: %build_test_apk --driver driver-simple-exit --out %t --testcase %s %build_test_apk_opts
// RUN: %Test_jit_debuginfo %s %t
// DEBUGGER: source android-commands.py
// DEBUGGER: load-android-app %t
// DEBUGGER: set breakpoint pending on
// DEBUGGER: b twenty
// DEBUGGER: run-android-app
// DEBUGGER: bt
// CHECK: twenty
// CHECK: some_function
// CHECK: foo
// CHECK: bar
// CHECK: entry
// CHECK: breakpoint_inlined_function.rs:

#pragma version(1)
#pragma rs java_package_name(%PACKAGE%)

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
