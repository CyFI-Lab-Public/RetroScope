#pragma version(1)
#pragma rs java_package_name(foo)

static int gi;

static void not_a_kernel(int i) {
    static int j;
    int k;
    j = i;
}

int __attribute__((kernel)) root(uint32_t ain) {
  static const int ci;
  static int i;
  return 0;
}

static int __attribute__((kernel)) static_kernel() {
  return 0;
}
