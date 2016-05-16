#include <stdio.h>


extern int __atomic_dec(volatile int* addr);

int main(int argc, const char *argv[])
{
    int x = 5;

    while (x > -20) {
        printf("old_x=%d\n", __atomic_dec(&x));
        printf("x=%d\n", x);
    }

    printf ("OK\n");
    return 0;
}
