// RUN: %clang %s -g -fexceptions %extra-clang-opts -o %t
// RUN: %Test_jit_debuginfo %s %t
// DEBUGGER: set breakpoint pending on
// DEBUGGER: break three
// DEBUGGER: run
// DEBUGGER: bt 4
// CHECK: #0
// CHECK:  three () at
// CHECK: #1
// CHECK:  in two
// CHECK: #2
// CHECK:  in one
// CHECK: #3
// CHECK:  in main

int three()
{
  return 0;
}

int two()
{
  return three();
}

int one()
{
  return two();
}

int main(int argc, char** argv)
{
  return one();
}
