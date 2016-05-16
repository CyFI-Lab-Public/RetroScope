dnl AX_MALLOC_ATTRIBUTE - see if gcc will take __attribute__((malloc))
AC_DEFUN([AX_MALLOC_ATTRIBUTE],
[
AC_MSG_CHECKING([whether malloc attribute is understood])
SAVE_CFLAGS=$CFLAGS
CFLAGS="-Werror $CFLAGS"
AC_TRY_COMPILE(,[
void monkey() __attribute__((malloc));
],AC_MSG_RESULT([yes]); AC_DEFINE(MALLOC_ATTRIBUTE_OK, 1, [whether malloc attribute is understood]), AC_MSG_RESULT([no]))
CFLAGS=$SAVE_CFLAGS 
]
)
