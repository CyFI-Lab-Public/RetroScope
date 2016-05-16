// RUN: %Slang %s
// RUN: %rs-filecheck-wrapper %s
// CHECK: define void @root(
// CHECK: define void @foo(
// CHECK: define void @.helper_bar(

#pragma version(1)
#pragma rs java_package_name(compute)

void root(const int *ain, int *aout, const void *usrData,
          uint32_t x, uint32_t y) {
}

void bar(int i, float f) {
}

void foo (int *p) {
}
