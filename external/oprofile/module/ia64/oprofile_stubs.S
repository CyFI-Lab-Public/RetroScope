/**
 * @file oprofile_stubs.S
 * Assembly language system call interceptor stubs 
 *
 * @remark Copyright 2001-2002 Hewlett-Packard Company
 * @remark Read the file COPYING
 *
 * @author Bob Montgomery <bob_montgomery@hp.com>
 */

/*
 * This interceptor for execve was stolen from ia64/kernel/entry.S
 *
 * Kernel entry points.
 *
 * Copyright (C) 1998-2001 Hewlett-Packard Co
 *	David Mosberger-Tang <davidm@hpl.hp.com>
 * Copyright (C) 1999 VA Linux Systems
 * Copyright (C) 1999 Walt Drummond <drummond@valinux.com>
 * Copyright (C) 1999 Asit Mallick <Asit.K.Mallick@intel.com>
 * Copyright (C) 1999 Don Dugger <Don.Dugger@intel.com>
 */
/*
 * ia64_switch_to now places correct virtual mapping in in TR2 for
 * kernel stack. This allows us to handle interrupts without changing
 * to physical mode.
 *
 * Jonathan Nicklin	<nicklin@missioncriticallinux.com>
 * Patrick O'Rourke	<orourke@missioncriticallinux.com>
 * 11/07/2000
 */
/*
 * Global (preserved) predicate usage on syscall entry/exit path:
 *
 *	pKern:		See entry.h.
 *	pUser:		See entry.h.
 *	pSys:		See entry.h.
 *	pNonSys:	!pSys
 */

#include <linux/config.h>

#include <asm/cache.h>
#include <asm/errno.h>
#include <asm/kregs.h>
#include <asm/offsets.h>
#include <asm/processor.h>
#include <asm/unistd.h>
#include <asm/asmmacro.h>
#include <asm/pgtable.h>

#include "IA64minstate.h"

	/*
	 * execve() is special because in case of success, we need to
	 * setup a null register window frame.
	 */
GLOBAL_ENTRY(my_ia64_execve)
	.prologue ASM_UNW_PRLG_RP|ASM_UNW_PRLG_PFS, ASM_UNW_PRLG_GRSAVE(3)
	alloc loc1=ar.pfs, 3, 3, 4, 0
	mov loc0=rp
	mov loc2=gp
	.body
	mov out0=in0			// filename
	;;				// stop bit between alloc and call
	mov out1=in1			// argv
	mov out2=in2			// envp
	add out3=16, sp			// regs
	/* 
	 * We are here with the kernel's gp register value but we need
	 * to find the module's gp value before we can call our own
	 * routine.  That's why we can't just use:
	 *	br.call.sptk.many rp=my_sys_execve
	 * Use ip-relative addressing to get to the fptr since I can't
	 * use gp-relative anything without the module's gp.
	 */
.L1_execve:
	mov r3 = ip
	;;
	addl r14 = .fptr_execve - .L1_execve, r3
	;;
	ld8 r14=[r14]
	;;
	ld8 r15=[r14], 8
	;;
	ld8 gp=[r14]
	;;
	mov b6=r15
	br.call.sptk.many b0=b6
	;;
.ret0:	cmp4.ge p6, p7=r8, r0
	mov ar.pfs=loc1			// restore ar.pfs
	sxt4 r8=r8			// return 64-bit result
	;;
	stf.spill [sp]=f0
(p6)	cmp.ne pKern, pUser=r0, r0	// a successful execve() lands us in user-mode...
	mov gp=loc2
	mov rp=loc0
(p6)	mov ar.pfs=r0			// clear ar.pfs on success
(p7)	br.ret.sptk.many rp

	/*
	 * In theory, we'd have to zap this state only to prevent leaking of
	 * security sensitive state (e.g., if current->mm->dumpable is zero).  However,
	 * this executes in less than 20 cycles even on Itanium, so it's not worth
	 * optimizing for...).
	 */
	mov r4=0;		mov f2=f0;		mov b1=r0
	mov r5=0;		mov f3=f0;		mov b2=r0
	mov r6=0;		mov f4=f0;		mov b3=r0
	mov r7=0;		mov f5=f0;		mov b4=r0
	mov ar.unat=0;		mov f10=f0;		mov b5=r0
	ldf.fill f11=[sp];	ldf.fill f12=[sp];	mov f13=f0
	ldf.fill f14=[sp];	ldf.fill f15=[sp];	mov f16=f0
	ldf.fill f17=[sp];	ldf.fill f18=[sp];	mov f19=f0
	ldf.fill f20=[sp];	ldf.fill f21=[sp];	mov f22=f0
	ldf.fill f23=[sp];	ldf.fill f24=[sp];	mov f25=f0
	ldf.fill f26=[sp];	ldf.fill f27=[sp];	mov f28=f0
	ldf.fill f29=[sp];	ldf.fill f30=[sp];	mov f31=f0
	mov ar.lc=0
	br.ret.sptk.many rp
	.align 16
.fptr_execve:
	data8 @fptr(my_sys_execve)
END(my_ia64_execve)

/* These interceptors are from IA64syscallstub.h macros */
#include "IA64syscallstub.h"

SYSCALLSTUB_POST(clone)

SYSCALLSTUB_POST(clone2)

SYSCALLSTUB_POST(mmap)

SYSCALLSTUB_POST(mmap2)

SYSCALLSTUB_POST(init_module)

SYSCALLSTUB_PRE(exit)
