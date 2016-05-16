/**
 * @file op_apic.c
 *
 * APIC setup etc. routines
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 * @author Dave Jones
 * @author Graydon Hoare
 */

#include <linux/mm.h>
#include <linux/init.h>
#include <linux/config.h>
#include <asm/io.h>

#include "oprofile.h"
#include "op_msr.h"
#include "op_apic.h"

/* used to save/restore original kernel nmi */
static struct gate_struct kernel_nmi;
static ulong lvtpc_masked;

/* this masking code is unsafe and nasty but might deal with the small
 * race when installing the NMI entry into the IDT.
 */
static void mask_lvtpc(void * e)
{
	u32 v = apic_read(APIC_LVTPC);
	lvtpc_masked = v & APIC_LVT_MASKED;
	apic_write(APIC_LVTPC, v | APIC_LVT_MASKED);
}

static void unmask_lvtpc(void * e)
{
	if (!lvtpc_masked)
		apic_write(APIC_LVTPC, apic_read(APIC_LVTPC) & ~APIC_LVT_MASKED);
}


void install_nmi(void)
{
	struct _descr descr;

	/* NMI handler is at idt_table[IDT_VECTOR_NUMBER]            */
	/* see Intel Vol.3 Figure 5-2, interrupt gate                */

	smp_call_function(mask_lvtpc, NULL, 0, 1);
	mask_lvtpc(NULL);

	store_idt(descr);
	kernel_nmi = descr.base[NMI_VECTOR_NUM];
	SET_NMI_GATE;

	smp_call_function(unmask_lvtpc, NULL, 0, 1);
	unmask_lvtpc(NULL);
}

void restore_nmi(void)
{
	struct _descr descr;

	smp_call_function(mask_lvtpc, NULL, 0, 1);
	mask_lvtpc(NULL);

	store_idt(descr);
	descr.base[NMI_VECTOR_NUM] = kernel_nmi;

	smp_call_function(unmask_lvtpc, NULL, 0, 1);
	unmask_lvtpc(NULL);
}


/* ---------------- APIC setup ------------------ */
static uint saved_lvtpc[NR_CPUS];

void __init lvtpc_apic_setup(void * dummy)
{
	uint val;

	/* set up LVTPC as we need it */
	/* IA32 V3, Figure 7.8 */
	val = apic_read(APIC_LVTPC);
	saved_lvtpc[op_cpu_id()] = val;
	/* allow PC overflow interrupts */
	val &= ~APIC_LVT_MASKED;
	/* set delivery to NMI */
	val = SET_APIC_DELIVERY_MODE(val, APIC_MODE_NMI);
	apic_write(APIC_LVTPC, val);
}

/* not safe to mark as __exit since used from __init code */
void lvtpc_apic_restore(void * dummy)
{
	/* restoring APIC_LVTPC can trigger an apic error because the delivery
	 * mode and vector nr combination can be illegal. That's by design: on
	 * power on apic lvt contain a zero vector nr which are legal only for
	 * NMI delivrey mode. So inhibit apic err before restoring lvtpc
	 */
	uint v = apic_read(APIC_LVTERR);
	apic_write(APIC_LVTERR, v | APIC_LVT_MASKED);
	apic_write(APIC_LVTPC, saved_lvtpc[op_cpu_id()]);
	apic_write(APIC_LVTERR, v);
}

static int __init enable_apic(void)
{
	uint msr_low, msr_high;
	uint val;

	/* enable local APIC via MSR. Forgetting this is a fun way to
	 * lock the box. But we have to hope this is allowed if the APIC
	 * has already been enabled.
	 *
	 * IA32 V3, 7.4.2 */
	rdmsr(MSR_IA32_APICBASE, msr_low, msr_high);
	if ((msr_low & (1 << 11)) == 0)
		wrmsr(MSR_IA32_APICBASE, msr_low | (1 << 11), msr_high);

	/* even if the apic is up we must check for a good APIC */

	/* IA32 V3, 7.4.15 */
	val = apic_read(APIC_LVR);
	if (!APIC_INTEGRATED(GET_APIC_VERSION(val)))
		goto not_local_apic;

	/* LVT0,LVT1,LVTT,LVTPC */
	if (GET_APIC_MAXLVT(apic_read(APIC_LVR)) < 4)
		goto not_local_apic;

	/* IA32 V3, 7.4.14.1 */
	val = apic_read(APIC_SPIV);
	if (!(val & APIC_SPIV_APIC_ENABLED))
		apic_write(APIC_SPIV, val | APIC_SPIV_APIC_ENABLED);

	return !!(val & APIC_SPIV_APIC_ENABLED);

not_local_apic:
	/* disable the apic only if it was disabled */
	if ((msr_low & (1 << 11)) == 0)
		wrmsr(MSR_IA32_APICBASE, msr_low & ~(1 << 11), msr_high);

	printk(KERN_ERR "oprofile: no suitable local APIC. Falling back to RTC mode.\n");
	return -ENODEV;
}

static void __init do_apic_setup(void)
{
	uint val;

	local_irq_disable();

	val = APIC_LVT_LEVEL_TRIGGER;
	val = SET_APIC_DELIVERY_MODE(val, APIC_MODE_EXINT);
	apic_write(APIC_LVT0, val);

	/* edge triggered, IA 7.4.11 */
	val = SET_APIC_DELIVERY_MODE(0, APIC_MODE_NMI);
	apic_write(APIC_LVT1, val);

	/* clear error register */
	/* IA32 V3, 7.4.17 */
	/* PHE must be cleared after unmasking by a back-to-back write,
	 * but it is probably ok because we mask only, the ESR is not
	 * updated is this a real problem ? */
	apic_write(APIC_ESR, 0);

	/* mask error interrupt */
	/* IA32 V3, Figure 7.8 */
	val = apic_read(APIC_LVTERR);
	val |= APIC_LVT_MASKED;
	apic_write(APIC_LVTERR, val);

	/* setup timer vector */
	/* IA32 V3, 7.4.8 */
	apic_write(APIC_LVTT, APIC_SEND_PENDING | 0x31);

	/* Divide configuration register */
	/* PHE the apic clock is based on the FSB. This should only
	 * changed with a calibration method.  */
	val = APIC_TDR_DIV_1;
	apic_write(APIC_TDCR, val);

	local_irq_enable();
}

/* does the CPU have a local APIC ? */
static int __init check_cpu_ok(void)
{
	if (sysctl.cpu_type != CPU_PPRO &&
		sysctl.cpu_type != CPU_PII &&
		sysctl.cpu_type != CPU_PIII &&
		sysctl.cpu_type != CPU_ATHLON &&
		sysctl.cpu_type != CPU_HAMMER &&
		sysctl.cpu_type != CPU_P4 &&
		sysctl.cpu_type != CPU_P4_HT2)
		return 0;

	return 1;
}

int __init apic_setup(void)
{
	u32 val;

	if (!check_cpu_ok())
		goto nodev;

	fixmap_setup();

	switch (enable_apic()) {
		case 0:
			do_apic_setup();
			val = apic_read(APIC_ESR);
			printk(KERN_INFO "oprofile: enabled local APIC. Err code %.08x\n", val);
			break;
		case 1:
			printk(KERN_INFO "oprofile: APIC was already enabled\n");
			break;
		default:
			goto nodev;
	}

	lvtpc_apic_setup(NULL);
	return 0;
nodev:
	printk(KERN_WARNING "Your CPU does not have a local APIC, e.g. "
	       "mobile P6. Falling back to RTC mode.\n");
	return -ENODEV;
}

void apic_restore(void)
{
	fixmap_restore();
}
