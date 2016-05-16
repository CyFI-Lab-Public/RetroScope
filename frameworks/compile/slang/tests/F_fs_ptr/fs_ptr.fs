#pragma version(1)
#pragma rs java_package_name(foo)

int *i;

struct f {
    int i;
    float *pf;
    char c;
    short *ps;
};

int ia[10];

int __attribute__((kernel)) root(uint32_t ain) {
  char *c;

  c = (char*) ain; // TODO(srhines): This is ok today.
  return 0;
}

void __attribute__((kernel)) in_only(uint32_t ain) {
}

int __attribute__((kernel)) out_only() {
  return 0;
}

int __attribute__((kernel)) everything(uint32_t ain, uint32_t x, uint32_t y) {
  return (int)&ain; // TODO(srhines): This is ok today.
}

void old_kernel(const uint32_t *ain, uint32_t x, uint32_t y) {
}

void test_call() {
    int i = root(ia[4]);
}

