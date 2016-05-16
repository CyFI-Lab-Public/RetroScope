// RUN: %clang %s -g -fexceptions %extra-clang-opts -o %t
// RUN: %Test_jit_debuginfo %s %t
// DEBUGGER: set breakpoint pending on
// DEBUGGER: break %s:42
// DEBUGGER: run
// DEBUGGER: print pf[0]
// CHECK: $1 = 0
// DEBUGGER: print d[0][0]
// CHECK: $2 = 0
// DEBUGGER: print us
// CHECK: $3 = 65535
// DEBUGGER: print l
// CHECK: $4 = 1
// DEBUGGER: print f
// CHECK: $5 = 10
// DEBUGGER: continue

struct double_struct {
  double d;
  double d2[2];
} compound_double;


float f = 0.f;
float *pf = &f;

const double d[2][2] = {{0, 1}, {2, 3.0}};
struct double_struct s;

unsigned short us = -1;
const unsigned long l = 1;

int main(int argc, char* argv[])
{
  int f = 10; // shadow

  s.d = 10e-4;
  s.d2[0] = 1e4;
  s.d2[1] = 100.5;

  double result = pf[0] * d[1][1] * s.d * us * l;
  return (result == 0 ? 0 : -1);
}
