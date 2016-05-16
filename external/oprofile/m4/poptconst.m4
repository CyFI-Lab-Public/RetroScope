dnl AX_POPT_CONST - check popt prototype
AC_DEFUN([AX_POPT_CONST],
[
AC_MSG_CHECKING([popt prototype])
SAVE_CXXFLAGS=$CXXFLAGS
CXXFLAGS="-Werror $CXXFLAGS"
AC_TRY_COMPILE([#include <popt.h>],
[
int c; char **v;
poptGetContext(0, c, v, 0, 0);
],
AC_MSG_RESULT([takes char **]);,
AC_MSG_RESULT([takes const char **]); AC_DEFINE(CONST_POPT, 1, [whether popt prototype takes a const char **]))
CXXFLAGS="$SAVE_CXXFLAGS"
]
)
