#include <stdlib.h>
#include <stdio.h>

#define test(x) \
 do { \
   if(!(x)) { \
     printf("Test FAILED: " #x "\n"); \
     exit(0); \
   } \
 } while(0)

#define test_f(F) void test_ ## F 

#define call_test(x) \
  do { \
    printf("Enter Test Func \t" #x "\n"); \
    test_ ## x ; \
    printf("Finish Test Func\t" #x "\n"); \
  } while (0)

