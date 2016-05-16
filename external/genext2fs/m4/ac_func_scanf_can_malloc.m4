dnl AC_FUNC_SCANF_CAN_MALLOC macro
dnl
dnl (c) Finn Thain 2006
dnl Copying and distribution of this file, with or without modification,
dnl are permitted in any medium without royalty provided the copyright
dnl notice and this notice are preserved.

# AC_FUNC_SCANF_CAN_MALLOC()
# --------------------------------------
AC_DEFUN([AC_FUNC_SCANF_CAN_MALLOC],
  [ AC_CHECK_HEADERS([stdlib.h])
    AC_CACHE_CHECK([whether scanf can malloc], [ac_scanf_can_malloc],
    [ AC_RUN_IFELSE(
      [ AC_LANG_PROGRAM(
        [
#include <stdio.h>
#if STDC_HEADERS || HAVE_STDLIB_H
#include <stdlib.h>
#endif
        ], [
  union { float f; char *p; } u;
  char *p;
  u.f = 0;
  char *scan_this = "56789";
  int matched = sscanf(scan_this, "%as", &u);
  if(matched < 1) return 1; /* shouldn't happens */
  if(u.f == (float)56789) return 2;

  p = u.p;
  while(*scan_this && *p == *scan_this) {
    ++p;
    ++scan_this;
  };
  free(u.p);
  if(*scan_this == 0) return 0;
  return 3;
        ])
      ],
      [ac_scanf_can_malloc=yes],
      [ac_scanf_can_malloc=no],
      [
case $host_alias in
  *-*-linux* ) ac_scanf_can_malloc=yes ;;
  *-*-solaris* ) ac_scanf_can_malloc=no ;;
  *-*-darwin* ) ac_scanf_can_malloc=no ;;
  * ) ac_scanf_can_malloc=no ;;
esac
      ])
    ])
if test x$ac_scanf_can_malloc = "xyes"; then
  AC_DEFINE([SCANF_CAN_MALLOC], 1, [Define to 1 if the scanf %a conversion format mallocs a buffer. Undefine if %a format denotes a float.])
fi
  ])
