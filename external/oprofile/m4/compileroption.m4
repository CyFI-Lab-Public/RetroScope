dnl AX_CFLAGS_OPTIONS(var-name, option)
dnl add option to var-name if $CC support it.
AC_DEFUN([AX_CFLAGS_OPTION], [
AC_MSG_CHECKING([whether ${CC} $2 is understood])
AC_LANG_SAVE
AC_LANG_C
SAVE_CFLAGS=$CFLAGS
CFLAGS=$2
AC_TRY_COMPILE(,[;],AC_MSG_RESULT([yes]); $1="${$1} $2",AC_MSG_RESULT([no]))
CFLAGS=$SAVE_CFLAGS
AC_LANG_RESTORE
])


dnl AX_CXXFLAGS_OPTIONS(var-name, option)
dnl add option to var-name if $CXX support it.
AC_DEFUN([AX_CXXFLAGS_OPTION], [
AC_MSG_CHECKING([whether ${CXX} $2 is understood])
AC_LANG_SAVE
AC_LANG_CPLUSPLUS
SAVE_CXXFLAGS=$CXXFLAGS
CXXFLAGS=$2
AC_TRY_COMPILE(,[;],AC_MSG_RESULT([yes]); $1="${$1} $2",AC_MSG_RESULT([no]))
CXXFLAGS=$SAVE_CXXFLAGS
AC_LANG_RESTORE
])
