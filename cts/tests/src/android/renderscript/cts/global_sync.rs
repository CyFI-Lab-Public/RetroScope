#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

int gInt;
static int sInt;

rs_allocation aFailed;

void test_read_global(int expected) {
    if (gInt != expected) {
        rsSetElementAt_uchar(aFailed, 1, 0);
    }
}

void test_read_static_global(int expected) {
    if (sInt != expected) {
        rsSetElementAt_uchar(aFailed, 1, 0);
    }
}

void test_write_global(int i) {
    gInt = i;
}

void test_write_static_global(int i) {
    sInt = i;
}

void __attribute__((kernel)) write_global(int ain, uint32_t x) {
    if (x == 0) {
        gInt = ain;
    }
}

void __attribute__((kernel)) write_static_global(int ain, uint32_t x) {
    if (x == 0) {
        sInt = ain;
    }
}

int __attribute__((kernel)) read_global(int ain, uint32_t x) {
    if (gInt != ain) {
        return 1;
    }
    return 0;
}

int __attribute__((kernel)) read_static_global(int ain, uint32_t x) {
    if (sInt != ain) {
        return 1;
    }
    return 0;
}
