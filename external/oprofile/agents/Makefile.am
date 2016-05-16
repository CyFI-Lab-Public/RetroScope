
SUBDIRS =

if BUILD_JVMTI_AGENT
SUBDIRS += 	jvmti
endif

if BUILD_JVMPI_AGENT
SUBDIRS +=      jvmpi
endif

INSTALL_EXEC_HOOK =

if CHECK_ACCOUNT
INSTALL_EXEC_HOOK += __install-exec-hook
endif

__install-exec-hook:
	@getent passwd oprofile >/dev/null || ( \
	echo "****************************************************************" ; \
	echo "*    WARNING:" ; \
	echo "*      Create the 'oprofile' user account to enable profiling of JITed code." ; \
	echo "****************************************************************") ;
	@getent group oprofile >/dev/null ||  ( \
	echo "****************************************************************" ; \
	echo "*    WARNING:" ; \
	echo "*      Create the 'oprofile' group to enable profiling of JITed code." ; \
	echo "****************************************************************") ;
	@test -z `id -gn oprofile 2>/dev/null` || \
	test `id -gn oprofile 2>/dev/null` = "oprofile" || ( \
	echo "****************************************************************" ; \
	echo "*    WARNING:" ; \
	echo "*      The special user 'oprofile' must have the default group set to 'oprofile'." ; \
	echo "****************************************************************") ;

install-exec-hook: $(INSTALL_EXEC_HOOK)
