#pragma version(1)
#pragma rs java_package_name(foo)

double d;

struct s {
    int i;
    double d;
    char c;
    long l;
};

struct s myS;

void foo_d(double d) {
    double e;
    float f = 0.0;
    e = d;
}

void foo_l(long l) {
    long m;
    int i = 1l;
    m = l;
}

void foo_ll(long long l) {
    long long m;
    int i = 1ll;
    m = l;
}

void foo_ld(long double l) {
    long double m;
    float f = 0.0L;
    m = l;
}

