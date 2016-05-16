#include <stdio.h>
#include <stdlib.h>

double drand48(void);

static int fails = 0;

static int
double_eq(double a, double b)
{
    /* Compare two double values, and return 1 if they are "close" enough */
    double diff = a -b;
    if (diff < 0) diff = -diff;
    if (a < 0) {
        if (b >= 0)
            return 0;
        a = -a;
        b = -b;
    } else if (b < 0) {
        return 0;
    }
    if (a >= b)
        a = b;

    return diff < a*1e-8;
}

#define EXPECT_LONG(value,expected) \
    do { \
        long _val = (value); \
        long _expected = (expected); \
        printf( "%s: ", #value); \
        if (_val != _expected) { \
            printf("KO: %ld (%ld expected)\n", _val, _expected); \
            fails += 1; \
        } else { \
            printf("%ld (ok)\n", _expected); \
        } \
    } while (0)

#define EXPECT_DOUBLE(value,expected) \
    do { \
        double _val = (value); \
        double _expected = (expected); \
        printf( "%s: ", #value); \
        if (!double_eq(_val,_expected)) { \
            printf("KO: %.12g (%.12g expected)\n", _val, _expected); \
            fails += 1; \
        } else { \
            printf("%.12g (ok)\n", _expected); \
        } \
    } while (0)

int
main(void)
{
    long int l = -345678L;
    float  f = 123.456e14;
    double d = -87.65432e45;

    // Verify display of hard-coded float and double values.
    // This is done to confirm the correct printf format specifiers
    // are being used.
    puts("Hard-coded values");
    printf("  l: %li\n", l);
    printf("  f: %g\n", (double) f);
    printf("  d: %g\n", d);

    // lrand48
    puts("lrand48");
    puts("  srand48(100)");
    srand48(100);
    EXPECT_LONG(lrand48(),539144888);
    EXPECT_LONG(lrand48(),448713282);
    EXPECT_LONG(lrand48(),2020627300);

    // Try again, with same seed.  Should get the same values
    puts("  srand48(100)");
    srand48(100);
    EXPECT_LONG(lrand48(),539144888);
    EXPECT_LONG(lrand48(),448713282);
    EXPECT_LONG(lrand48(),2020627300);

    // Try again, but with a different seed
    puts("  srand48(101)");
    srand48(101);
    EXPECT_LONG(lrand48(),261694958);
    EXPECT_LONG(lrand48(),1961809783);
    EXPECT_LONG(lrand48(),1458943423);

    // drand48
    puts("drand48");
    puts("  srand48(100)");
    srand48(100);
    EXPECT_DOUBLE(drand48(),0.251058902665);
    EXPECT_DOUBLE(drand48(),0.208948404851);
    EXPECT_DOUBLE(drand48(),0.940927909958);

    // Try again, with same seed.  Should get the same values
    puts("  srand48(100)");
    srand48(100);
    EXPECT_DOUBLE(drand48(),0.251058902665);
    EXPECT_DOUBLE(drand48(),0.208948404851);
    EXPECT_DOUBLE(drand48(),0.940927909958);

    // Try again, but with a different seed
    puts("  srand48(101)");
    srand48(101);
    EXPECT_DOUBLE(drand48(),0.121861211331);
    EXPECT_DOUBLE(drand48(),0.913538869095);
    EXPECT_DOUBLE(drand48(),0.679373472502);

    return (fails > 0) ? 1 : 0;
}
