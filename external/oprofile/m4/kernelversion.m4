dnl AX_KERNEL_VERSION(major, minor, level, comparison, action-if-true, action-if-false)
AC_DEFUN([AX_KERNEL_VERSION], [
SAVE_CFLAGS=$CFLAGS
CFLAGS="-I$KINC -D__KERNEL__ -Werror"
AC_TRY_COMPILE( 
  [
  #include <linux/version.h>
  ],
  [
  #if LINUX_VERSION_CODE $4 KERNEL_VERSION($1, $2, $3)
  break_me_hard(\\\);
  #endif
  ],
[$5],[$6],)
CFLAGS=$SAVE_CFLAGS
])
