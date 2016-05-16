/**
 * @file op_arch.h
 * defines registers for x86
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Will Cohen
 */

#ifndef OP_ARCH_H
#define OP_ARCH_H

#include <asm/ptrace.h>

/* How to access the processor's instruction pointer */
#ifndef instruction_pointer
#define instruction_pointer(regs) ((regs)->eip)
#endif

/* Bit in processor's status register for interrupt masking */
#define IRQ_ENABLED(regs)	((regs)->eflags & IF_MASK)

#endif /* OP_ARCH_H */
