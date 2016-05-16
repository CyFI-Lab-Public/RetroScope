#pragma version(1)
#pragma rs java_package_name(foo)

union u {
    int i;
    float f;
} myUnion;

static union u2 {
    int i;
    rs_font f;
} myUnion2;


void foo() {
    union iu {
        rs_font f;
        int i;
    } v;
    (void) v;
}
