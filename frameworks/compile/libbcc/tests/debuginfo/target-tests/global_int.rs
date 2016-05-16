// RUN: %build_test_apk --driver driver-int-param --out %t --testcase %s %build_test_apk_opts
// RUN: %Test_jit_debuginfo %s %t
// DEBUGGER: source android-commands.py
// DEBUGGER: load-android-app %t
// DEBUGGER: set breakpoint pending on 
// DEBUGGER: b %s:46
// DEBUGGER: run-android-app
// DEBUGGER: p global_zero
// DEBUGGER: p global_value
// CHECK: $1 = 0
// CHECK: $2 = 11

#pragma version(1)
#pragma rs java_package_name(%PACKAGE%)

// a global value
int global_zero = 0;
int global_value = 1;

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

void entry(int parameter) {
  bar();
  if (parameter != 0) {
    global_value += 10;
  } else {
    global_zero += 1;
  }
  global_zero += global_value;
}
