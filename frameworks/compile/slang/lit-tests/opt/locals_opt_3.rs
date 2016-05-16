// RUN: %Slang -O 3 %s
// RUN: %rs-filecheck-wrapper %s
// CHECK-NOT: define internal i32 @main(
// CHECK-NOT: %f = alloca float,
// CHECK-NOT: %pf = alloca float*,
// CHECK-NOT: %ppn = alloca i32**,

struct float_struct {
  float f;
  float f2[2];
} compound_float;

static
int main(int argc, char* argv[])
{
  float f = 0.f;
  float *pf = &f;

  double d[2][2] = {{0, 1}, {2, 3.0}};
  struct float_struct s;

  unsigned short us = -1;
  const unsigned long l = (unsigned long) -1.0e8f;

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

void the_main() {
  main(0, 0);
}

#pragma version(1)
#pragma rs java_package_name(foo)
