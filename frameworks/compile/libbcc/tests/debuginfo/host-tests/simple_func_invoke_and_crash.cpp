// RUN: %clangxx %s -g -fexceptions %extra-clang-opts -o %t
// RUN: %Test_jit_debuginfo %s %t
// DEBUGGER: run
// DEBUGGER: bt 2
// CHECK: function_with_a_segfault
// CHECK: main

static int function_with_a_segfault() {
  int* bla = 0;
  *bla = 5;
  return 0;
}

int main() {
  return function_with_a_segfault();
}
