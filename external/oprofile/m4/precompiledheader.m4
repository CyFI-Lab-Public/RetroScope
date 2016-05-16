dnl AX_CXXFLAGS_OPTIONS(var-name, option)
dnl add option to var-name if $CXX support it.
AC_DEFUN([AX_CHECK_PRECOMPILED_HEADER], [
AC_MSG_CHECKING([whether ${CXX} support precompiled header])
AC_LANG_SAVE
AC_LANG_CPLUSPLUS
SAVE_CXXFLAGS=$CXXFLAGS
dnl we consider than if -Winvalid-pch is accepted pch will works ...
CXXFLAGS=-Winvalid-pch
dnl but we don't want -Winvalid-pch else compilation will fail due -Werror and
dnl the fact than some pch will be invalid for the given compilation option
AC_TRY_COMPILE(,[;],AC_MSG_RESULT([yes]); $1="${$1} -include bits/stdc++.h", AC_MSG_RESULT([no]))
CXXFLAGS=$SAVE_CXXFLAGS
AC_LANG_RESTORE
])
