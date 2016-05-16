
dnl AX_MSG_RESULT_YN(a)
dnl results "yes" iff a==1, "no" else
AC_DEFUN([AX_MSG_RESULT_YN], [x=no
test "x$1" = "x1" && x=yes
AC_MSG_RESULT($x)])
