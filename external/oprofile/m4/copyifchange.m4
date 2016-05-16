dnl AX_COPY_IF_CHANGE(source, dest)
dnl copy source to dest if they don't compare equally or if dest doesn't exist
AC_DEFUN([AX_COPY_IF_CHANGE], [
if test -r $2; then
	if cmp $1 $2 > /dev/null; then
		echo $2 is unchanged
	else
		cp -f $1 $2
	fi
else
	cp -f $1 $2
fi
])
