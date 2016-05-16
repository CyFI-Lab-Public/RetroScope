dnl AX_KERNEL_OPTION(option, action-if-found, action-if-not-found)
dnl see if autoconf.h defines the option
AC_DEFUN([AX_KERNEL_OPTION], [
SAVE_CFLAGS=$CFLAGS
CFLAGS="-I$KINC -O2 -D__KERNEL__"
AC_TRY_COMPILE( [#include <linux/autoconf.h>],
[
#ifndef $1
break_me_hard(\\\);
#endif
],[$2],[$3],)
CFLAGS=$SAVE_CFLAGS
])
