// RUN: %Slang %s
// RUN: %rs-filecheck-wrapper %s
// CHECK-NOT: DW_TAG_subprogram

#pragma version(1)
#pragma rs java_package_name(foo)

void root(const int *ain, int *aout, const void *usrData,
          uint32_t x, uint32_t y) {
}
