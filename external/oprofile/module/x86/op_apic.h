/**
 * @file op_apic.h
 * x86 apic, nmi, perf counter declaration
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 * @author Dave Jones
 * @author Graydon Hoare
 */

#ifndef OP_APIC_H
#define OP_APIC_H

#include "apic_compat.h"

#define NMI_GATE_TYPE 14
#define NMI_VECTOR_NUM 2
#define NMI_DPL_LEVEL 0


/* copied from kernel 2.4.19 : arch/i386/traps.c */

struct gate_struct {
	u32 a;
	u32 b;
} __attribute__((packed));

#define _set_gate(gate_addr, type, dpl, addr) \
do { \
	int __d0, __d1; \
	__asm__ __volatile__ ("movw %%dx, %%ax\n\t" \
	"movw %4, %%dx\n\t" \
	"movl %%eax, %0\n\t" \
	"movl %%edx, %1" \
	:"=m" (*((long *) (gate_addr))), \
	 "=m" (*(1+(long *) (gate_addr))), "=&a" (__d0), "=&d" (__d1) \
	:"i" ((short) (0x8000+(dpl << 13)+(type << 8))), \
	 "3" ((char *) (addr)), "2" (__KERNEL_CS << 16)); \
} while (0)

#define SET_NMI_GATE	\
	_set_gate(&descr.base[NMI_VECTOR_NUM], NMI_GATE_TYPE, NMI_DPL_LEVEL, &op_nmi);

#define store_idt(addr) \
	do { \
		__asm__ __volatile__ ("sidt %0" \
			: "=m" (addr) \
			: : "memory"); \
	} while (0)
 
struct _descr { 
	u16 limit; 
	struct gate_struct * base; 
} __attribute__((__packed__));

void lvtpc_apic_setup(void * dummy);
void lvtpc_apic_restore(void * dummy);
int apic_setup(void);
void apic_restore(void);
void install_nmi(void);
void restore_nmi(void);

void fixmap_setup(void);
void fixmap_restore(void);

asmlinkage void op_nmi(void);

#endif /* OP_APIC_H */
