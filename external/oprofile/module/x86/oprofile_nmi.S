#ifndef __ASSEMBLY__
#define __ASSEMBLY__  /* for some 2.2 kernel */
#endif
#include <linux/sys.h>
#include <linux/linkage.h>
#include <asm/segment.h>
#define ASSEMBLY
#include <asm/smp.h>

#define SAVE_ALL \
	cld; \
	pushl %es; \
	pushl %ds; \
	pushl %eax; \
	pushl %ebp; \
	pushl %edi; \
	pushl %esi; \
	pushl %edx; \
	pushl %ecx; \
	pushl %ebx; \
	movl $(__KERNEL_DS), %edx; \
	movl %edx, %ds; \
	movl %edx, %es;
 
#define RESTORE_ALL	\
	popl %ebx;	\
	popl %ecx;	\
	popl %edx;	\
	popl %esi;	\
	popl %edi;	\
	popl %ebp;	\
	popl %eax;	\
1:	popl %ds;	\
2:	popl %es;	\
	addl $4, %esp;	\
3:	iret;		\
.section .fixup,"ax";	\
4:	movl $0, (%esp);	\
	jmp 1b;		\
5:	movl $0, (%esp);	\
	jmp 2b;		\
6:	pushl %ss;	\
	popl %ds;	\
	pushl %ss;	\
	popl %es;	\
	pushl $11;	\
	call my_sys_exit;\
.previous;		\
.section __ex_table,"a";\
	.align 4;	\
	.long 1b, 4b;	\
	.long 2b, 5b;	\
	.long 3b, 6b;	\
.previous

ENTRY(op_nmi)
	pushl %eax
	SAVE_ALL
	movl %esp, %edx
	pushl %edx
	call op_do_nmi
	addl $4, %esp
	RESTORE_ALL
