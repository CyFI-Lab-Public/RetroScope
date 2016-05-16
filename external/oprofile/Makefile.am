#### ATTENTION ####
#    The agents directory must be kept as the last subdir
SUBDIRS = \
	. \
	m4 \
	libutil \
	libop \
	libopagent \
	libdb \
	libutil++ \
	libopt++ \
	libabi \
	daemon \
	utils \
	libregex \
	libpp \
	opjitconv \
	pp \
	events \
	doc \
	gui \
	agents
#### ATTENTION ####
#    The agents directory must be kept as the last subdir



ACLOCAL_AMFLAGS = -I m4

# The module will not build under distcheck
DISTCHECK_CONFIGURE_FLAGS = --with-kernel-support

if !kernel_support

.PHONY: module

all-local: module

# This must use a fully-qualified SUBDIRS path, so we can't use ${top_srcdir}
module:
	make -C libutil
	make -C libop
	(cd $(KSRC) && $(MAKE) SUBDIRS=$(OPROFILE_DIR)/module/@OPROFILE_MODULE_ARCH@ modules) || exit 1 ;
	(cd $(KSRC) && $(MAKE) SUBDIRS=$(OPROFILE_DIR)/module modules) || exit 1 ;

install-exec-local:
	-mkdir -p $(MODINSTALLDIR)/oprofile
	cp module/oprofile-module.o $(MODINSTALLDIR)/oprofile/oprofile.o
	if test $(KVERS) = "`uname -r`"; then depmod -a ; fi

clean-local:
	rm -f module/*.o module/@OPROFILE_MODULE_ARCH@/*.o \
	module/.*.o.cmd module/.*.o.flags \
	module/@OPROFILE_MODULE_ARCH@/.*.o.cmd module/@OPROFILE_MODULE_ARCH@/.*.o.flags

endif

EXTRA_DIST = \
	ChangeLog-2001 \
	ChangeLog-2002 \
	ChangeLog-2003 \
	ChangeLog-2004 \
	ChangeLog-2005 \
        ChangeLog-2006 \
	ChangeLog-2007 \
	ChangeLog-2008 \
	autogen.sh \
	README_PACKAGERS \
	include/sstream \
	module/Makefile.in \
	module/compat.c \
	module/op_dname.c \
	module/op_init.c \
	module/oprofile.c \
	module/op_util.c \
	module/compat22.h \
	module/compat24.h \
	module/compat.h \
	module/op_cache.h \
	module/op_dcache.h \
	module/oprofile.h \
	module/op_util.h \
	module/ia64/Makefile.in \
	module/ia64/oprofile_stubs.S \
	module/ia64/cpu_type.c \
	module/ia64/op_pmu.c \
	module/ia64/op_syscalls.c \
	module/ia64/IA64entry.h \
	module/ia64/IA64minstate.h \
	module/ia64/IA64syscallstub.h \
	module/ia64/op_ia64_model.h \
	module/x86/Makefile.in \
	module/x86/oprofile_nmi.S \
	module/x86/apic_compat.h \
	module/x86/op_apic.h \
	module/x86/op_arch.h \
	module/x86/op_x86_model.h \
	module/x86/op_msr.h \
	module/x86/cpu_type.c \
	module/x86/op_apic.c \
	module/x86/op_fixmap.c \
	module/x86/op_model_athlon.c \
	module/x86/op_model_p4.c \
	module/x86/op_model_ppro.c \
	module/x86/op_nmi.c \
	module/x86/op_rtc.c \
	module/x86/op_syscalls.c
