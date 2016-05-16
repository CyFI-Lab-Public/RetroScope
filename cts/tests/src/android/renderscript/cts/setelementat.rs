#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

#include "shared.rsh"

int memset_toValue = 0;

int compare_value = 0;
int compare_failure = 2;
int failure_value = 0;

uint32_t dimX = 0;
uint32_t dimY = 0;
rs_allocation array;

void memset(int *aout) {
    *aout = memset_toValue;
    return;
}

void compare(const int *ain) {
    if (*ain != compare_value) {
        rsAtomicCas(&compare_failure, 2, -1);
        failure_value = *ain;
    }
    return;
}

void getCompareResult(int* aout) {
    *aout = compare_failure;
}

void setLargeArray(const int *ain, uint32_t x) {
    int source = 10;
    if (x == 0) {
        for (uint32_t i = 0; i < dimX; i++) {
            rsSetElementAt(array, &source, i);
        }
    }
}

void setLargeArray2D(const int *ain, uint32_t x) {
    int source = 10;
    if (x == 0) {
        for (uint32_t y = 0; y < dimY; y++) {
            for (uint32_t xtemp = 0; xtemp < dimX; xtemp++) {
                rsSetElementAt(array, &source, xtemp, y);
            }
        }
    }
}
