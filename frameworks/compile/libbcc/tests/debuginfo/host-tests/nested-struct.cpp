// RUN: %clang %s -g -fexceptions %extra-clang-opts -o %t
// RUN: %Test_jit_debuginfo %s %t

// If debug info for my_number() is emitted outside function foo's scope
// then a debugger may not be able to handle it. At least one version of
// gdb crashes in such cases.

// DEBUGGER: set breakpoint pending on
// DEBUGGER: b nested-struct.cpp:28
// DEBUGGER: run
// DEBUGGER: ptype foo
// CHECK: type = int (void)

int foo() {
  struct Local {
    static int my_number() {
      return 42;
    }
  };

  int i = 0;
  i = Local::my_number();
  return i + 1;
}

int main() {
  foo();
  return 0;
}
