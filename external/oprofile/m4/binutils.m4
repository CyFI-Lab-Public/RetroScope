dnl AX_BINUTILS - check for needed binutils stuff

AC_DEFUN([AX_BINUTILS],
[
dnl some distro have a libiberty.a but does not have a libiberty.h
AC_CHECK_HEADERS(libiberty.h)
AC_CHECK_LIB(iberty, cplus_demangle,, AC_MSG_ERROR([liberty library not found]))
AC_CHECK_FUNCS(xcalloc)
AC_CHECK_FUNCS(xmemdup)
AC_CHECK_LIB(dl, dlopen, LIBS="$LIBS -ldl"; DL_LIB="-ldl", DL_LIB="")
AC_CHECK_LIB(intl, main, LIBS="$LIBS -lintl"; INTL_LIB="-lintl", INTL_LIB="")

AC_CHECK_LIB(bfd, bfd_openr, LIBS="-lbfd $LIBS"; Z_LIB="",
	[AC_CHECK_LIB(z, compress,
dnl Use a different bfd function here so as not to use cached result from above
		[AC_CHECK_LIB(bfd, bfd_fdopenr, LIBS="-lbfd -lz $LIBS"; Z_LIB="-lz",
			[AC_MSG_ERROR([bfd library not found])], -lz)
		],
		[AC_MSG_ERROR([libz library not found; required by libbfd])])
	]
)

AC_LANG_PUSH(C)
# Determine if bfd_get_synthetic_symtab macro is available
OS="`uname`"
if test "$OS" = "Linux"; then
	AC_MSG_CHECKING([whether bfd_get_synthetic_symtab() exists in BFD library])
	rm -f test-for-synth
	AC_LANG_CONFTEST(
		[AC_LANG_PROGRAM([[#include <bfd.h>]],
			[[asymbol * synthsyms;	bfd * ibfd = 0; 
			long synth_count = bfd_get_synthetic_symtab(ibfd, 0, 0, 0, 0, &synthsyms);
			extern const bfd_target bfd_elf64_powerpc_vec;
			extern const bfd_target bfd_elf64_powerpcle_vec;
			char * ppc_name = bfd_elf64_powerpc_vec.name;
			char * ppcle_name = bfd_elf64_powerpcle_vec.name;
			printf("%s %s\n", ppc_name, ppcle_name);]])
		])
	$CC conftest.$ac_ext $CFLAGS $LDFLAGS $LIBS -o  test-for-synth > /dev/null 2>&1
	if test -f test-for-synth; then
		echo "yes"
		SYNTHESIZE_SYMBOLS='1'
	else
		echo "no"
		SYNTHESIZE_SYMBOLS='0'
	fi
	AC_DEFINE_UNQUOTED(SYNTHESIZE_SYMBOLS, $SYNTHESIZE_SYMBOLS, [Synthesize special symbols when needed])
	rm -f test-for-synth*

fi
AC_LANG_POP(C)
]
)
