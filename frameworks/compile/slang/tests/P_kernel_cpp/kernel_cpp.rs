// -reflect-c++
#pragma version(1)
#pragma rs java_package_name(foo)

//int2 i2;

int i1 = 5;
bool bt = true;
bool bf = false;
int2 i2 = 2;
int3 i3 = {1, 2, 3};

int __attribute__((kernel)) root(uint32_t ain) {
  return 0;
}

void __attribute__((kernel)) in_only(uint32_t ain) {
}

int __attribute__((kernel)) out_only() {
  return 0;
}

int __attribute__((kernel)) everything(uint32_t ain, uint32_t x, uint32_t y) {
  return 0;
}

