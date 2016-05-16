dnl AX_CHECK_SSTREAM - check if local sstream is needed to compile OK
AC_DEFUN([AX_CHECK_SSTREAM],
[
AC_MSG_CHECKING([whether to use included sstream])
AC_TRY_COMPILE([#include <sstream>], [], 
AC_MSG_RESULT([no]);,
AC_MSG_RESULT([yes]); OP_CXXFLAGS="$OP_CXXFLAGS -I\${top_srcdir}/include")
]
)
