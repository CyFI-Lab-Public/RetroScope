#pragma version(1)
#pragma rs java_package_name(foo)

rs_font globalAlloc;
rs_font globalAlloc2;

struct hasMatrix {
    rs_matrix3x3 m;
} ghm;

static void foo() {

    rs_font fontUninit;
    rs_font fontArr[10];
    fontUninit = globalAlloc;
    for (int i = 0; i < 10; i++) {
        fontArr[i] = globalAlloc;
    }

    return;
}

void singleStmt() {
    struct hasMatrix h = ghm;
    ghm = h;
    globalAlloc = globalAlloc2;
}

int root(void) {
    foo();
    return 10;
}

