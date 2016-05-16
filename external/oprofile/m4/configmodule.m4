dnl Handle the 2.4 module inside module/
AC_DEFUN([AX_CONFIG_MODULE],
[
if test ! -f $KINC/linux/autoconf.h; then
	AC_MSG_ERROR([no suitably configured kernel include tree found])
fi

dnl  --- Get Linux kernel version and compile parameters ---

AC_SUBST(KVERS)
AC_MSG_CHECKING([for kernel version])
dnl it's like this to handle mandrake's fubar version.h - bug #471448
eval KVERS=`gcc -I$KINC -E -dM $KINC/linux/version.h | grep -w UTS_RELEASE | awk '{print $[]3}'`
AC_MSG_RESULT([$KVERS])
case "$KVERS" in
2.2.*|2.4.*) ;;
*) AC_MSG_ERROR([Unsupported kernel version])
esac

dnl Check for the minimal kernel version supported
AC_MSG_CHECKING([kernel version])
AX_KERNEL_VERSION(2, 2, 10, <=, AC_MSG_RESULT([ok]), AC_MSG_ERROR([check html documentation install section]))

dnl linux/spinlock.h added at some point in past
AC_MSG_CHECKING([for $KINC/linux/spinlock.h])
if test -f $KINC/linux/spinlock.h; then
	EXTRA_CFLAGS_MODULE="$EXTRA_CFLAGS_MODULE -DHAVE_LINUX_SPINLOCK_HEADER"
	AC_MSG_RESULT([yes])
else
	AC_MSG_RESULT([no])
fi

AC_MSG_CHECKING([for rtc_lock])
gcc -I$KINC -E $KINC/linux/mc146818rtc.h | grep rtc_lock >/dev/null
if test "$?" -eq 0; then
	EXTRA_CFLAGS_MODULE="$EXTRA_CFLAGS_MODULE -DRTC_LOCK"
	AC_MSG_RESULT([yes])
else
	AC_MSG_RESULT([no])
fi
	 
arch="unknown"
AC_MSG_CHECKING(for x86 architecture)
AX_KERNEL_OPTION(CONFIG_X86, x86=1, x86=0)
AX_KERNEL_OPTION(CONFIG_X86_WP_WORKS_OK, x86=1, x86=$x86)
AX_MSG_RESULT_YN($x86)
test "$x86" = 1 && arch="x86"
	
if test "$arch" = "unknown"; then
	AC_MSG_CHECKING(for ia64 architecture)
 	AX_KERNEL_OPTION(CONFIG_IA64, ia64=1, ia64=0)
 	AX_MSG_RESULT_YN($ia64)
 	test "$ia64" = 1 && arch="ia64"
fi

test "$arch" = "unknown" && AC_MSG_ERROR(Unsupported architecture)

dnl check to see if kernel verion appropriate for arch
AC_MSG_CHECKING(arch/kernel version combination)
case "$arch" in
ia64)
	AX_KERNEL_VERSION(2, 4, 18, <, AC_MSG_RESULT([ok]),
		AC_MSG_ERROR([unsupported arch/kernel])) ;;
*) AC_MSG_RESULT([ok])
esac

dnl for now we do not support PREEMPT patch
AC_MSG_CHECKING([for preempt patch])
AX_KERNEL_OPTION(CONFIG_PREEMPT,preempt=1,preempt=0)
AX_MSG_RESULT_YN([$preempt])
test "$preempt" = 0 || AC_MSG_ERROR([unsupported kernel configuration : CONFIG_PREEMPT])

AC_SUBST(KINC)

MODINSTALLDIR=/lib/modules/$KVERS
 
OPROFILE_MODULE_ARCH=$arch
AC_SUBST(OPROFILE_MODULE_ARCH)
]
)
