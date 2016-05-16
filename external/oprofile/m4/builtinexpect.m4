dnl builtin_expect is used in module we can't add that in config.h
AC_DEFUN([AX_BUILTIN_EXPECT],
[
AC_MSG_CHECKING([whether __builtin_expect is understood])
SAVE_CFLAGS=$CFLAGS
CFLAGS="-Werror $CFLAGS"
AC_TRY_LINK(,[
int i;
if (__builtin_expect(i, 0)) { }
],
AC_MSG_RESULT([yes]); EXTRA_CFLAGS_MODULE="$EXTRA_CFLAGS_MODULE -DEXPECT_OK",
AC_MSG_RESULT([no]);)
CFLAGS=$SAVE_CFLAGS 
]
) 
