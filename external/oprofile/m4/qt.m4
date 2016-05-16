dnl find a binary in the path
AC_DEFUN([QT_FIND_PATH],
[
	AC_MSG_CHECKING([for $1])
	AC_CACHE_VAL(qt_cv_path_$1,
	[
		qt_cv_path_$1="NONE"
		if test -n "$$2"; then
			qt_cv_path_$1="$$2";
		else
			dirs="$3"
			qt_save_IFS=$IFS
			IFS=':'
			for dir in $PATH; do
				dirs="$dirs $dir"
			done
			IFS=$qt_save_IFS
 
			for dir in $dirs; do
				if test -x "$dir/$1"; then
					if test -n "$5"; then
						evalstr="$dir/$1 $5 2>&1 "
						if eval $evalstr; then
							qt_cv_path_$1="$dir/$1"
							break
						fi
					else
						qt_cv_path_$1="$dir/$1"
						break
					fi
				fi
			done
		fi
	])
 
	if test -z "$qt_cv_path_$1" || test "$qt_cv_path_$1" = "NONE"; then
		AC_MSG_RESULT(not found)
		$4
	else
		AC_MSG_RESULT($qt_cv_path_$1)
		$2=$qt_cv_path_$1
	fi
])

dnl Find the uic compiler on the path or in qt_cv_dir
AC_DEFUN([QT_FIND_UIC],
[
	QT_FIND_PATH(uic, ac_uic, $qt_cv_dir/bin)
	if test -z "$ac_uic" -a "$FATAL" = 1; then
		AC_MSG_ERROR([uic binary not found in \$PATH or $qt_cv_dir/bin !])
	fi
])
 
dnl Find the right moc in path/qt_cv_dir
AC_DEFUN([QT_FIND_MOC],
[
	QT_FIND_PATH(moc2, ac_moc2, $qt_cv_dir/bin)
	QT_FIND_PATH(moc, ac_moc1, $qt_cv_dir/bin)

	if test -n "$ac_moc1" -a -n "$ac_moc2"; then
		dnl found both. Prefer Qt3's if it exists else moc2
		$ac_moc1 -v 2>&1 | grep "Qt 3" >/dev/null
		if test "$?" = 0; then
			ac_moc=$ac_moc1;
		else
			ac_moc=$ac_moc2;
		fi
	else
		if test -n "$ac_moc1"; then
			ac_moc=$ac_moc1;
		else
			ac_moc=$ac_moc2;
		fi
	fi

	if test -z "$ac_moc"  -a "$FATAL" = 1; then
		AC_MSG_ERROR([moc binary not found in \$PATH or $qt_cv_dir/bin !])
	fi
])

dnl check a particular libname
AC_DEFUN([QT_TRY_LINK],
[
	SAVE_LIBS="$LIBS"
	LIBS="$LIBS $1"
	AC_TRY_LINK([
	#include <qglobal.h>
	#include <qstring.h>
		],
	[
	QString s("mangle_failure");
	#if (QT_VERSION < 221)
	break_me_(\\\);
	#endif
	],
	qt_cv_libname=$1,
	)
	LIBS="$SAVE_LIBS"
])
 
dnl check we can do a compile
AC_DEFUN([QT_CHECK_COMPILE],
[
	AC_MSG_CHECKING([$1 for Qt library name])
 
	AC_CACHE_VAL(qt_cv_libname,
	[
		AC_LANG_CPLUSPLUS
		SAVE_CXXFLAGS=$CXXFLAGS
		CXXFLAGS="$CXXFLAGS $QT_INCLUDES $QT_LDFLAGS" 

		for libname in -lqt-mt -lqt3 -lqt2 -lqt;
		do
			QT_TRY_LINK($libname)
			if test -n "$qt_cv_libname"; then
				break;
			fi
		done

		CXXFLAGS=$SAVE_CXXFLAGS
	])

	if test -z "$qt_cv_libname"; then
		AC_MSG_RESULT([failed]) 
		if test "$FATAL" = 1 ; then
			AC_MSG_ERROR([Cannot compile a simple Qt executable. Check you have the right \$QTDIR !])
		fi
	else
		AC_MSG_RESULT([$qt_cv_libname])
	fi
])

dnl get Qt version we're using
AC_DEFUN([QT_GET_VERSION],
[
	AC_CACHE_CHECK([Qt version],lyx_cv_qtversion,
	[
		AC_LANG_CPLUSPLUS
		SAVE_CPPFLAGS=$CPPFLAGS
		CPPFLAGS="$CPPFLAGS $QT_INCLUDES"

		cat > conftest.$ac_ext <<EOF
#line __oline__ "configure"
#include "confdefs.h"
#include <qglobal.h>
"%%%"QT_VERSION_STR"%%%"
EOF
		lyx_cv_qtversion=`(eval "$ac_cpp conftest.$ac_ext") 2>&5 | \
			grep '^"%%%"'  2>/dev/null | \
			sed -e 's/"%%%"//g' -e 's/"//g'`
		rm -f conftest.$ac_ext
		CPPFLAGS=$SAVE_CPPFLAGS
	])
 
	QT_VERSION=$lyx_cv_qtversion
	AC_SUBST(QT_VERSION)
])
 
dnl start here 
AC_DEFUN([QT_DO_IT_ALL],
[
	dnl Please leave this alone. I use this file in
	dnl oprofile.
	FATAL=0

	AC_ARG_WITH(qt-dir, [  --with-qt-dir           where the root of Qt is installed ],
		[ qt_cv_dir=`eval echo "$withval"/` ])
	 
	AC_ARG_WITH(qt-includes, [  --with-qt-includes      where the Qt includes are. ],
		[ qt_cv_includes=`eval echo "$withval"` ])
 
	AC_ARG_WITH(qt-libraries, [  --with-qt-libraries     where the Qt library is installed.],
		[  qt_cv_libraries=`eval echo "$withval"` ])

	dnl pay attention to $QTDIR unless overridden
	if test -z "$qt_cv_dir"; then
		qt_cv_dir=$QTDIR
	fi
 
	dnl derive inc/lib if needed
	if test -n "$qt_cv_dir"; then
		if test -z "$qt_cv_includes"; then
			qt_cv_includes=$qt_cv_dir/include
		fi
		if test -z "$qt_cv_libraries"; then
			qt_cv_libraries=$qt_cv_dir/lib
		fi
	fi

	dnl flags for compilation
	QT_INCLUDES=
	QT_LDFLAGS=
	if test -n "$qt_cv_includes"; then
		QT_INCLUDES="-isystem $qt_cv_includes"
	fi
	if test -n "$qt_cv_libraries"; then
		QT_LDFLAGS="-L$qt_cv_libraries"
	fi
	AC_SUBST(QT_INCLUDES)
	AC_SUBST(QT_LDFLAGS)
 
	QT_FIND_MOC
	MOC=$ac_moc
	AC_SUBST(MOC)
	QT_FIND_UIC
	UIC=$ac_uic
	AC_SUBST(UIC)

	QT_CHECK_COMPILE(in lib)
	if test -z "$qt_cv_libname"; then
		if test -n "$qt_cv_dir"; then
		dnl Try again using lib64 vs lib
			qt_cv_libraries=$qt_cv_dir/lib64
			QT_LDFLAGS="-L$qt_cv_libraries"
			QT_CHECK_COMPILE(in lib64)
		fi
	fi

	QT_LIB=$qt_cv_libname;
	AC_SUBST(QT_LIB)

	if test -n "$qt_cv_libname"; then
		QT_GET_VERSION
	fi
])
