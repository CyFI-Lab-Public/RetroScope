// RUN: %clang %s -g -fexceptions %extra-clang-opts -o %t
// RUN: %Test_jit_debuginfo %s %t
// DEBUGGER: set breakpoint pending on
// DEBUGGER: break test_parameters
// DEBUGGER: run
// DEBUGGER: step
// DEBUGGER: print pf[0]
// CHECK: $1 = 0
// DEBUGGER: print ppd[1][1]
// CHECK: $2 = 3
// DEBUGGER: print s
// CHECK: $3 = (char_struct &)
// CHECK: {c = 97 'a', c2 = "01"}
// DEBUGGER: print ppn
// CHECK: $4 = (int **) 0x0
// DEBUGGER: print us
// CHECK: $5 = 10
// DEBUGGER: print l
// CHECK: $6 = 42
// DEBUGGER: continue

struct char_struct {
  char c;
  char c2[2];
} compound_char;


double test_parameters(float* pf, double ppd[][2], struct char_struct& s, int** ppn = 0, unsigned short us = 10u, const unsigned long l = 42)
{
  double result = pf[0] * ppd[1][1] * s.c * us * l;
  return result;
}

int main(int argc, char* argv[])
{
  struct char_struct s;
  float f = 0.f;
  double d[2][2] = {{0, 1}, {2, 3.0}};

  s.c = 'a';
  s.c2[0] = '0';
  s.c2[1] = '1';

  double result = test_parameters(&f, d, s);
  return(result == 0 ? 0 : -1);
}
