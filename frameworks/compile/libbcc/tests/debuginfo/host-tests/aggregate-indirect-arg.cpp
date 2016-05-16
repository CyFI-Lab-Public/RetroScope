// RUN: %clangxx %s -O0 -g -fexceptions %extra-clang-opts -o %t
// RUN: %Test_jit_debuginfo %s %t
// XFAIL: host-bcc
// DEBUGGER: set breakpoint pending on
// DEBUGGER: break aggregate-indirect-arg.cpp:22
// DEBUGGER: r
// DEBUGGER: p v
// CHECK: $1 = (SVal &)
// CHECK:  Data = 0x0,
// CHECK:  Kind = 2142

class SVal {
public:
  ~SVal() {}
  const void* Data;
  unsigned Kind;
};

void bar(SVal &v) {}
class A {
public:
  void foo(SVal v) { bar(v); }
};

int main() {
  SVal v;
  v.Data = 0;
  v.Kind = 2142;
  A a;
  a.foo(v);
  return 0;
}
