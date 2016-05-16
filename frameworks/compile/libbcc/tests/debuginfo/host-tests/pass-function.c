// RUN: %clangxx %s -g -fexceptions %extra-clang-opts -o %t
// RUN: %Test_jit_debuginfo %s %t
// DEBUGGER: set breakpoint pending on
// DEBUGGER: break function_test
// DEBUGGER: break %s:47
// DEBUGGER: break %s:55
// DEBUGGER: break %s:60
// DEBUGGER: break %s:66
// DEBUGGER: break %s:69
// DEBUGGER: run
// DEBUGGER: bt 2
// CHECK: #0
// CHECK:  function_test
// CHECK: #1
// CHECK:  main
// DEBUGGER: continue
// DEBUGGER: print j
// CHECK: $1 = 0
// DEBUGGER: step
// DEBUGGER: print j
// CHECK: $2 = 1
// DEBUGGER: continue
// DEBUGGER: print j
// CHECK: $3 = -1
// DEBUGGER: continue
// DEBUGGER: bt 3
// CHECK: #0
// CHECK:  inline_test
// CHECK: #1
// CHECK:  function_test
// CHECK: #2
// CHECK:  main
// DEBUGGER: continue
// DEBUGGER: print j
// CHECK: $4 = 2
// DEBUGGER: continue
// DEBUGGER: print j
// CHECK: $5 = 0
// DEBUGGER: continue

__attribute__((noinline)) static int function_test();
__attribute__((always_inline)) static int inline_test();

int inline_test()
{
  int i = 0;
  i++;
  return i;
}

int function_test(int c)
{
  int i, j = 0;
  for (i = 0; i < c; i++) {
    j++;
  }

  {
    int j = -1;
    j++;
  }

  j += inline_test();

  if (j > 0) {
    j = 0;
  }

  return j;
}

int main(int argc, char** argv)
{
  return function_test(1);
}
