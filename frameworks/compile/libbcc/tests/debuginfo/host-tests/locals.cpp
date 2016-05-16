// RUN: %clangxx %s -g -fexceptions %extra-clang-opts -o %t
// RUN: %Test_jit_debuginfo %s %t
// DEBUGGER: set breakpoint pending on
// DEBUGGER: break %s:45
// DEBUGGER: run
// DEBUGGER: info locals
// CHECK: pf = 0x
// CHECK: s = {f = 0.00100000005, f2 = {10000, 100.5}}
// CHECK: us = 65535
// CHECK: f = 0
// CHECK: d = {{[{][{]}}0, 1}, {2, 3{{[}][}]}}
// CHECK: l = 0
// CHECK: result = 0
// DEBUGGER: continue

struct float_struct {
  float f;
  float f2[2];
} compound_float;


int main(int argc, char* argv[])
{
  float f = 0.f;
  float *pf = &f;

  double d[2][2] = {{0, 1}, {2, 3.0}};
  struct float_struct s;

  unsigned short us = -1;
  const unsigned long l = static_cast<unsigned long>(-1.0e8f);

  {
    int** ppn = 0;
    if (ppn) {
      return -1;
    }
  }

  s.f = 10e-4f;
  s.f2[0] = 1e4f;
  s.f2[1] = 100.5f;

  double result = pf[0] * d[1][1] * s.f * us * l;
  return (result == 0 ? 0 : -1);
}
