#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

#include "shared.rsh"

int memset_toValue = 0;

int compare_value = 0;
int compare_failure = 2;

// 0 = +, 1 = -, 2 = *, 3 = /
int arith_operation = 0;
int arith_use_rs_allocation = 0;
rs_allocation arith_rs_input;
int arith_value = 0;

void arith(const int *ain, int *aout, uint32_t x) {
    int value = arith_value;

    if (arith_use_rs_allocation)
        value = *(int*)(rsGetElementAt(arith_rs_input, x));

    if (arith_operation == 0) {
        *aout = *ain + value;
    } else if (arith_operation == 1) {
        *aout = *ain - value;
    } else if (arith_operation == 2) {
        *aout = *ain * value;
    } else if (arith_operation == 3) {
        *aout = *ain / value;
    }

}

void memset(int *aout) {
    *aout = memset_toValue;
    return;
}

void compare(const int *ain) {
    if (*ain != compare_value) {
        rsAtomicCas(&compare_failure, 2, -1);
    }
    return;
}

void getCompareResult(int* aout) {
    *aout = compare_failure;
}
