dnl AX_CELL_SPU - check for needed binutils stuff for Cell BE SPU
AC_DEFUN([AX_CELL_SPU],
[
# On Cell BE architecture, OProfile uses bfd_openr_iovec when processing some
# SPU profiles.  To parse embedded SPU ELF on Cell BE, OProfile requires a
# version of bfd_openr_iovec that supports the elf32-spu target.
# This version of the function also has a 7th parameter that's been added.
# First, we check for existence of the base bfd_openr_iovec.  If it exists,
# we then use a temporary test program below that passes 7 arguments to
# bfd_openr_iovec; if it compiles OK, we assume we have the right BFD
# library to support Cell BE SPU profiling.

AC_LANG_PUSH(C)

AC_CHECK_LIB(bfd, bfd_openr_iovec,
	[bfd_openr_iovec_exists="yes"],
	[bfd_openr_iovec_exists="no"]
)

if test "$bfd_openr_iovec_exists" = "yes"; then
	AC_MSG_CHECKING([whether bfd_openr_iovec has seven parameters])
	AC_COMPILE_IFELSE([AC_LANG_PROGRAM([#include <bfd.h>
	  #include <stdlib.h>
	],
	 [[struct bfd *nbfd = bfd_openr_iovec("some-file", "elf32-spu",
			NULL, NULL, NULL, NULL, NULL);
	  return 0;
	]])],
	[AC_DEFINE([HAVE_BFD_OPENR_IOVEC_WITH_7PARMS],
		[],
		[Defined if you have the version of bfd_openr_iovec with 7 parameters])
	bfd_open_iovec_7="yes"
	AC_MSG_RESULT([yes])],
	[AC_MSG_RESULT([no])]
	)
fi

AC_LANG_POP(C)

arch="unknown"
AC_ARG_WITH(target,
[  --with-target=cell-be   Check BFD support for Cell Broadband Engine SPU profiling], arch=$withval)

if test "$arch" = "cell-be"; then
        if test "$bfd_open_iovec_7" = "yes"; then
	        AC_MSG_NOTICE([BFD library has support for Cell Broadband Engine SPU profiling])
	else
		AC_ERROR([BFD library does not support elf32-spu target; SPU profiling is unsupported])
	fi
fi
]
)
