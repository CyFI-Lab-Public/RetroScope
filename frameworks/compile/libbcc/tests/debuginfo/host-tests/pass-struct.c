// RUN: %clangxx %s -O0 -g -fexceptions %extra-clang-opts -o %t
// RUN: %Test_jit_debuginfo %s %t
// XFAIL: host-bcc
// (This testcase is expected to fail because of bcc optimizations that
//  are enabled by default in the absence of metadata)

// DEBUGGER: set breakpoint pending on
// DEBUGGER: break test_struct
// DEBUGGER: run
// DEBUGGER: step
// DEBUGGER: print s
// CHECK: $1 = {n = 10, n2 = {20, 21}}
// DEBUGGER: continue

struct int_struct {
  int n;
  int n2[2];
} compound_int;


int test_struct(struct int_struct s)
{
  s.n2[1]++;
  return s.n > s.n2[0] ? s.n : s.n2[0];
}

int main(int argc, char* argv[])
{
  struct int_struct s;

  s.n = 10;
  s.n2[0] = 20;
  s.n2[1] = 21;

  int result = test_struct(s);
  return(result == 20 ? 0 : -1);
}
