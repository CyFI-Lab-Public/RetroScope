#ifndef MARISA_ALPHA_ASSERT_H_
#define MARISA_ALPHA_ASSERT_H_

#include <stdio.h>
#include <stdlib.h>

#define ASSERT(cond) (void)((!!(cond)) || \
  (printf("%d: Assertion `%s' failed.\n", __LINE__, #cond), exit(-1), 0))

#define EXCEPT(code, expected_status) try { \
  code; \
  printf("%d: Exception `%s' failed.\n", __LINE__, #code); \
  exit(-1); \
} catch (const marisa_alpha::Exception &ex) { \
  ASSERT(ex.status() == expected_status); \
}

#define TEST_START() \
  printf("%s:%d: %s(): ", __FILE__, __LINE__, __FUNCTION__)

#define TEST_END() \
  printf("ok\n")

#endif  // MARISA_ALPHA_ASSERT_H_
