// RUN: %clangxx %s -g -fexceptions %extra-clang-opts -o %t
// RUN: %Test_jit_debuginfo %s %t
// Radar 9168773

// DEBUGGER: set breakpoint pending on
// DEBUGGER: b forward-declare-class.cpp:28
// DEBUGGER: r
// DEBUGGER: ptype A
// CHECK: type = class A {
// CHECK-NEXT: public:
// CHECK-NEXT: int MyData;
// CHECK-NEXT: }
class A;
class B {
public:
  void foo(const A *p);
};

B iEntry;

class A {
public:
  int MyData;
};

A irp;

int main() {
  return 0;
}
