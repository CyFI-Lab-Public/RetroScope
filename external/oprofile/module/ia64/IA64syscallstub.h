/**
 * @file IA64syscallstub.h
 * Assembly language file macros
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Bob Montgomery
 */

/* $Id: IA64syscallstub.h,v 1.3 2008/01/21 21:35:17 movement Exp $ */

#define SYSCALLSTUB_POST(name)		\
	.sbss;				\
	.align 8;			\
	.type .post_saverp_##name, @object;	\
	.size .post_saverp_##name, 8;	\
.post_saverp_##name:			\
	.skip 8;			\
	.text;				\
	.global post_stub_##name;	\
	.align 32;			\
	.proc post_stub_##name;		\
post_stub_##name:			\
.L1_##name:				\
	mov r3=ip;			\
	;;				\
	addl r16=.L2_##name - .L1_##name, r3;	\
	;;				\
	mov b6=r16;			\
	;;				\
	br.ret.sptk.many b6;		\
	;;				\
.L2_##name:				\
	mov r3=ip;			\
	mov r15=gp;	/* save kgp */	\
	mov r17=rp;			\
	;;				\
	addl r14=.post_fptr_##name - .L2_##name, r3;	\
	;;				\
	ld8 r14=[r14];			\
	;;				\
	adds r14=8, r14;		\
	;;				\
	ld8 gp=[r14];			\
	;;				\
	addl r14=@ltoff(old_sys_##name), gp;    \
	addl r16=@gprel(.post_saverp_##name), gp;	\
	;;				\
	ld8 r14=[r14];			\
	st8 [r16]=r17;	/* save krp */	\
	;;				\
	ld8 r14=[r14];			\
	mov gp=r15;	/* restore kgp */	\
	;;				\
	ld8 r14=[r14];			\
	;;				\
	mov b6 = r14;			\
	;;				\
	br.call.sptk.many b0=b6;	\
	;;				\
.L3_##name:				\
	mov r3=ip;			\
	mov r15=gp;	/* save kgp */	\
	;;				\
	addl r14=.post_fptr_##name - .L3_##name, r3;	\
	;;				\
	ld8 r14=[r14];			\
	;;				\
	adds r14=8, r14;		\
	;;				\
	ld8 gp=[r14];			\
	br.call.sptk.many b0=post_call_stub_##name;	\
	;;				\
	addl r16=@gprel(.post_saverp_##name), gp;	\
	;;				\
	ld8 r14=[r16];			\
	;;				\
	mov b0 = r14;			\
	mov gp = r15; /* preserved */	\
	br.cond.sptk.many b0;		\
	.align 16;			\
.post_fptr_##name:				\
	data8 @fptr(post_sys_##name);	\
	.endp post_stub_##name;		\
	.align 16;			\
	.global post_call_stub_##name;	\
	.proc post_call_stub_##name;	\
post_call_stub_##name:			\
	alloc loc1=ar.pfs, 8, 6, 8, 0;	\
	mov loc0=rp;			\
	mov loc2=r15; /* preserve it */	\
	mov loc3=r8;			\
	mov loc4=r10;			\
	mov loc5=gp;			\
	mov out0 = r8;	/* old rv */	\
	mov out1 = in0;			\
	mov out2 = in1;			\
	mov out3 = in2;			\
	mov out4 = in3;			\
	mov out5 = in4;			\
	mov out6 = in5;			\
	mov out7 = in6;			\
	;;				\
	br.call.sptk.many rp = post_sys_##name;	\
	;;				\
	mov ar.pfs = loc1;		\
	mov rp = loc0;			\
	mov r15=loc2;			\
	mov r8=loc3;			\
	mov r10=loc4;			\
	mov gp=loc5;			\
	br.ret.sptk.few	rp;		\
	.endp post_call_stub_##name;

#define SYSCALLSTUB_PRE(name)		\
	.text;				\
	.global pre_stub_##name;	\
	.align 32;			\
	.proc pre_stub_##name;		\
pre_stub_##name:			\
.L4_##name:				\
	mov r3=ip;			\
	;;				\
	addl r17=.L5_##name - .L4_##name, r3;	\
	;;				\
	mov b6=r17;			\
	;;				\
	br.ret.sptk.many b6;		\
	;;				\
.L5_##name:				\
	mov r3=ip;			\
	mov r15=gp;	/* save kgp */	\
	mov r16=rp;	/* save krp */	\
	;;				\
	addl r14=.pre_fptr_##name - .L5_##name, r3;	\
	;;				\
	ld8 r14=[r14];			\
	;;				\
	adds r14=8, r14;		\
	;;				\
	ld8 gp=[r14];			\
	;;				\
	br.call.sptk.many b0=pre_call_stub_##name;	\
	;;				\
	/* kernel gp still in r15 */	\
	/* kernel rp still in r16 */	\
	/* module gp in gp */		\
	;;				\
	addl r14=@ltoff(old_sys_##name), gp;    \
	;;				\
	ld8 r14=[r14];			\
	;;				\
	ld8 r14=[r14];			\
	mov gp=r15; /* restore kgp */	\
	;;				\
	ld8 r14=[r14];			\
	mov rp=r16; /* restore krp */	\
	;;				\
	mov b6 = r14;			\
	;;				\
	/* use the saved krp */		\
	br.call.sptk.many b6=b6;	\
	;;				\
	.align 16;			\
.pre_fptr_##name:				\
	data8 @fptr(pre_sys_##name);	\
	.endp pre_stub_##name;		\
	.align 16;			\
	.global pre_call_stub_##name;	\
	.proc pre_call_stub_##name;	\
pre_call_stub_##name:			\
	alloc loc1=ar.pfs, 8, 5, 8, 0;	\
	mov loc0=rp;			\
	mov loc2=r15; /* preserve it */	\
	mov loc3=r16; /* preserve it */	\
	mov loc4=gp;			\
	mov out0 = in0;			\
	mov out1 = in1;			\
	mov out2 = in2;			\
	mov out3 = in3;			\
	mov out4 = in4;			\
	mov out5 = in5;			\
	mov out6 = in6;			\
	mov out7 = in7;			\
	;;				\
	br.call.sptk.many rp = pre_sys_##name;	\
	;;				\
	mov ar.pfs = loc1;		\
	mov rp = loc0;			\
	mov r15=loc2;			\
	mov r16=loc3;			\
	mov gp=loc4;			\
	br.ret.sptk.few	rp;		\
	.endp pre_call_stub_##name;


