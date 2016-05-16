dnl AX_CHECK_TYPEDEF(typedef_name, type, action-if-true, action-if-false)
dnl exec action-if-true if typedef_name is a typedef to type else exec 
dnl action-if-false
dnl currently work only with type typedef'ed in stddef.h
AC_DEFUN([AX_CHECK_TYPEDEF], [
dnl AC_LANG_PUSH(C) not in autoconf 2.13
AC_LANG_SAVE
AC_LANG_C
SAVE_CFLAGS=$CFLAGS
CFLAGS="-Werror $CFLAGS"

AC_TRY_COMPILE(
  [
  #include <stddef.h>
  ],
  [
  typedef void (*fct1)($1);
  typedef void (*fct2)($2);
  fct1 f1 = 0;
  fct2 f2 = 0;
  if (f1 == f2) {}
  ],
[$3],[$4])

CFLAGS=$SAVE_CFLAGS
AC_LANG_RESTORE
])


dnl AX_TYPEDEFED_NAME(typedef_name, candidate_list, var_name)
dnl set var_name to the typedef name of $1 which must be in canditate_list
dnl else produce a fatal error
AC_DEFUN([AX_TYPEDEFED_NAME], [
	AC_MSG_CHECKING([type of $1])
	for f in $2; do
		AX_CHECK_TYPEDEF($1, $f, $3="$f", $3="")
		if test -n "${$3}"; then
			break
		fi
	done
	if test -n "${$3}"; then
		AC_MSG_RESULT([${$3}])
	else
		AC_MSG_ERROR([not found])
	fi
])
