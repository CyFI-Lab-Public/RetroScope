// RUN: %clangxx %s -g -fexceptions %extra-clang-opts -o %t
// RUN: %Test_jit_debuginfo %s %t
// DEBUGGER: set verbose on
// DEBUGGER: b __jit_debug_register_code
// DEBUGGER: run
// DEBUGGER: info sources
// CHECK: test_info_sources.cpp
// DEBUGGER: c


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

int main() {
  return bar();
}
