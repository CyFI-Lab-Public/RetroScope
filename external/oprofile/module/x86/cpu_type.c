/**
 * @file cpu_type.c
 * CPU determination
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include "oprofile.h"
#include "op_msr.h"

#include <linux/smp.h>

EXPORT_NO_SYMBOLS;

MODULE_PARM(force_rtc, "i");
MODULE_PARM_DESC(force_rtc, "force RTC mode.");
static int force_rtc;

#ifndef HT_SUPPORT
/**
 * p4_threads - determines the number of logical processor threads in a die
 * 
 * returns number of threads in p4 die (1 for non-HT processors)
 */
static int p4_threads(void)
{
	int processor_threads = 1;

#ifdef CONFIG_SMP
	if (test_bit(X86_FEATURE_HT, &current_cpu_data.x86_capability)) {
		/* This it a Pentium 4 with HT, find number of threads */
		int eax, ebx, ecx, edx;

		cpuid (1, &eax, &ebx, &ecx, &edx);
		processor_threads = (ebx >> 16) & 0xff;
	}
#endif /* CONFIG_SMP */

	return processor_threads;
}

#ifdef CONFIG_SMP
/**
 * do_cpu_id - Call the cpuid instruction and fill in data at passed address
 *
 * We expect that data is pointing to an array of unsigned chars as big as there
 * are cpus in an smp system.
 */
static void do_cpu_id(void * data)
{
	int eax, ebx, ecx, edx;
	unsigned char * ptr = (unsigned char *) data;

	cpuid(1, &eax, &ebx, &ecx, &edx);

	/* bits EBX[31:24] define APIC ID */
	ptr[smp_processor_id()] = (unsigned char) ((ebx & 0xff000000) >> 24);
}
#endif

/**
 * p4_ht_enabled - Determines if Hyper Threading is enabled or not.
 *
 * A P4 can be capable of HT, but not enabled via BIOS.  The check for 
 * this is unfortunately not simple and involves running cpuid on all 
 * logical processors and checking for bits in the APIC_ID fields. 
 * As per Intel docs.  Returns 1 if enabled, 0 otherwise.
 *
 */
static int p4_ht_enabled(void)
{
#ifndef CONFIG_SMP
	return 0;
#else
	int enabled, threads, i;
	unsigned char mask;
	unsigned char apic_id[smp_num_cpus];
	unsigned int cpu;

	/* First check for capability */
	threads = p4_threads();
	if (threads == 1) return 0;
	/* Create mask for low order bits */
	mask = 0xff;
	i = 1;
	while (i < threads) {
		i *= 2;
		mask <<= 1;
	}
	/* Get APIC_ID from all logial procs and self */
	smp_call_function(do_cpu_id, apic_id, 1, 1);
	do_cpu_id(apic_id);
	/* If the low order bits are on, then HT is enabled */
	enabled = 0;
	cpu = 0;
	do {
		if (apic_id[cpu] & ~mask) {
			enabled = 1;
			break;
		}
		cpu++;
	} while (cpu < smp_num_cpus);

	return enabled;
#endif /* CONFIG_SMP */
}
#endif /* !HT_SUPPORT */


op_cpu p4_cpu_type(void)
{
	__u8 model = current_cpu_data.x86_model;
	if (model <= 4) {
#ifdef HT_SUPPORT
		if (smp_num_siblings == 1) {
			return CPU_P4;
		} else if (smp_num_siblings == 2) {
			return CPU_P4_HT2;
		} else {
			printk(KERN_INFO 
			       "oprofile: P4 HT unsupported number of siblings"
			       "processor, reverting to RTC\n");
			return CPU_RTC;
		}
#else
		/* Cannot handle enabled HT P4 hardware */
		if ((p4_threads() > 1) && p4_ht_enabled()) {
			printk(KERN_INFO 
			       "oprofile: P4 HT enabled, reverting to RTC\n");
			return CPU_RTC;
		}
		else
			return CPU_P4;
#endif
	} else
		/* Do not know what it is */
		return CPU_RTC;
}

 
__init op_cpu get_cpu_type(void)
{
	__u8 vendor = current_cpu_data.x86_vendor;
	__u8 family = current_cpu_data.x86;
	__u8 model = current_cpu_data.x86_model;
	__u16 val;

	if (force_rtc)
		return CPU_RTC;

	switch (vendor) {
		case X86_VENDOR_AMD:
			if (family == 6) {
				/* certain models of K7 does not have apic.
				 * Check if apic is really present before enabling it.
				 * IA32 V3, 7.4.1 */
				val = cpuid_edx(1);
				if (!(val & (1 << 9)))
					return CPU_RTC;
				return CPU_ATHLON;
			}
			if (family == 15)
				return CPU_HAMMER;
			return CPU_RTC;

		case X86_VENDOR_INTEL:
			switch (family) {
			default:
				return CPU_RTC;
			case 6:
				/* A P6-class processor */
				if (model == 14)
					return CPU_CORE;
				if (model > 0xd)
					return CPU_RTC;
				if (model > 5)
					return CPU_PIII;
				else if (model > 2)
					return CPU_PII;
				return CPU_PPRO;
			case 0xf:
				return p4_cpu_type();
			}
			
		default:
			return CPU_RTC;
	}
}
