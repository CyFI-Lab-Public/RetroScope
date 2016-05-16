/**
 * @file apic_compat.h
 * Definitions and functions for APIC interaction
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef APIC_COMPAT_H
#define APIC_COMPAT_H

#if V_BEFORE(2, 4, 0)
/* even on SMP, some defines are missing in 2.2 */
#define APIC_LVR		0x30
#define APIC_LVTPC		0x340
#define APIC_LVTERR		0x370
#define GET_APIC_VERSION(x)	((x)&0xFF)
#define GET_APIC_MAXLVT(x)	(((x) >> 16)&0xFF)
#define APIC_INTEGRATED(x)	((x)&0xF0)
#else
#include <asm/apic.h>
#include <asm/apicdef.h>
#include <asm/mpspec.h>
#endif

#ifndef MSR_IA32_APICBASE
#define MSR_IA32_APICBASE 0x1B
#endif
 
#ifndef APIC_SPIV_APIC_ENABLED
#define APIC_SPIV_APIC_ENABLED (1 << 8)
#endif

#ifndef APIC_DEFAULT_PHYS_BASE
#define APIC_DEFAULT_PHYS_BASE 0xfee00000
#endif

#if !defined(CONFIG_X86_LOCAL_APIC)

#define APIC_DEFAULT_PHYS_BASE		0xfee00000
#define APIC_SPIV			0xF0
#define APIC_ESR			0x280
#define APIC_LVTT			0x320
#define APIC_LVT0			0x350
#define APIC_LVT_MASKED			(1 << 16)
#define APIC_LVT_LEVEL_TRIGGER		(1 << 15)
#define APIC_MODE_NMI			0x4
#define APIC_MODE_EXINT			0x7
#define GET_APIC_DELIVERY_MODE(x)	(((x) >> 8)&0x7)
#define SET_APIC_DELIVERY_MODE(x, y)	(((x)&~0x700)|((y) << 8))
#define APIC_LVT1			0x360
#define APIC_SEND_PENDING		(1 << 12)
#define APIC_TDCR			0x3E0
#define APIC_TDR_DIV_1			0xB

/* when !CONFIG_X86_LOCAL_APIC we need to provide a valid va to map the
 * the pa of APIC onto. This va must be un-cachable/un-swapable*/
/*#define APIC_BASE (fix_to_virt(FIX_APIC_BASE))*/
extern unsigned long virt_apic_base;
/* the other define above can be redefined until the ref is identical, this
 * allow a compile time checking but this need to be redefined differently */
#undef APIC_BASE
#define APIC_BASE	virt_apic_base

static __inline void apic_write(unsigned long reg, unsigned long v)
{
	*((volatile u32 *)(APIC_BASE+reg)) = v;
}

static __inline unsigned long apic_read(unsigned long reg)
{
	return *((volatile u32 *)(APIC_BASE+reg));
}

#endif /* !defined(CONFIG_X86_LOCAL_APIC) */

#endif /* APIC_COMPAT_H */
