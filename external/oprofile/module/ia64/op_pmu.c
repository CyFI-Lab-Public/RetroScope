/**
 * @file op_pmu.c
 * Setup and handling of IA64 Performance Monitoring Unit (PMU)
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Bob Montgomery
 * @author Will Cohen
 * @author John Levon
 * @author Philippe Elie
 */


#include "oprofile.h"
#include "op_util.h"
#include <asm/perfmon.h>
#include "op_ia64_model.h"

/* number of counters physically present */
static uint op_nr_counters = 4;

/* performance counters are in pairs: pmcN and pmdN.  The pmc register acts
 * as the event selection; the pmd register is the counter. */
#define perf_reg(c)	((c)+4)

#define IA64_1_PMD_MASK_VAL	((1UL << 32) - 1)
#define IA64_2_PMD_MASK_VAL	((1UL << 47) - 1)

/* The appropriate value is selected in pmu_init() */
unsigned long pmd_mask = IA64_2_PMD_MASK_VAL;

#define pmd_overflowed(r, c) ((r) & (1 << perf_reg(c)))
#define set_pmd_neg(v, c) do { \
	ia64_set_pmd(perf_reg(c), -(ulong)(v) & pmd_mask); \
	ia64_srlz_d(); } while (0)
#define set_pmd(v, c) do { \
	ia64_set_pmd(perf_reg(c), (v) & pmd_mask); \
	ia64_srlz_d(); } while (0)
#define set_pmc(v, c) do { ia64_set_pmc(perf_reg(c), (v)); ia64_srlz_d(); } while (0)
#define get_pmd(c) ia64_get_pmd(perf_reg(c))
#define get_pmc(c) ia64_get_pmc(perf_reg(c))

/* ---------------- IRQ handler ------------------ */

/* The args match the args for pfm_overflow_handler in perfmon.c.
 * The task_struct is currently filled in with the perfmon "owner" of
 * the PMU.  This might change.  I'm not sure it makes sense in perfmon
 * either with system-wide profiling.
 * pmc0 is a bit mask for overflowed counters (bits 4-7)
 * This routine should return 0 to resume interrupts.
 */
inline static void
op_do_pmu_interrupt(u64 pmc0, struct pt_regs * regs)
{
	uint cpu = op_cpu_id();
	int ctr;

	for (ctr = 0 ; ctr < op_nr_counters ; ++ctr) {
		if (pmd_overflowed(pmc0, ctr)) {
			op_do_profile(cpu, regs->cr_iip, 1, ctr);
			set_pmd_neg(oprof_data[cpu].ctr_count[ctr], ctr);
		}
	}
	return;
}


static void
op_raw_pmu_interrupt(int irq, void * arg, struct pt_regs * regs)
{
	u64 pmc0;

	pmc0 = ia64_get_pmc(0);

	if ((pmc0 & ~0x1UL) != 0UL) {
		op_do_pmu_interrupt(pmc0, regs);
		ia64_set_pmc(0, 0);
		ia64_srlz_d();
	}
}


#define MY_OPROFILE_VECTOR (IA64_PERFMON_VECTOR - 2)

static void
op_set_pmv(void * dummy)
{
	ia64_set_pmv(MY_OPROFILE_VECTOR);
	ia64_srlz_d();
}


static void
op_restore_pmv(void* dummy)
{
	ia64_set_pmv(IA64_PERFMON_VECTOR);
	ia64_srlz_d();
}


static int
install_handler(void)
{
	int err = 0;

	/* Try it legally - confusion about vec vs irq */
	err = request_irq(MY_OPROFILE_VECTOR, op_raw_pmu_interrupt, 
			SA_INTERRUPT | SA_PERCPU_IRQ, "oprofile", NULL);

	if (err) {
		printk(KERN_ALERT "oprofile_IA64: request_irq fails, "
				"returns %d\n", err);
		return err;
	}

	if ((smp_call_function(op_set_pmv, NULL, 0, 1))) {
		printk(KERN_ALERT "oprofile_IA64: unexpected failure "
				"of smp_call_function(op_set_pmv)\n");
	}

	op_set_pmv(NULL);

	return err;
}


static int
restore_handler(void)
{
	int err = 0;

	if ((smp_call_function(op_restore_pmv, NULL, 0, 1))) {
		printk(KERN_ALERT "oprofile_IA64: unexpected failure "
				"of smp_call_function(op_restore_pmv)\n");
	}

	op_restore_pmv(NULL);

	free_irq(MY_OPROFILE_VECTOR, NULL);
	return err;
}


/* ---------------- PMU setup ------------------ */

/* This is kind of artificial.  The proc interface might really want to
 * accept register values directly.  There are other features not exposed 
 * by this limited interface.  Of course that might require all sorts of
 * validity checking??? */
static void
pmc_fill_in(ulong * val, u8 kernel, u8 user, u8 event, u8 um)
{
	/* enable interrupt generation */
	*val |= (1 << 5);

	/* setup as a privileged monitor */
	*val |= (1 << 6);

	/* McKinley requires pmc4 to have bit 23 set (enable PMU).
	 * It is supposedly ignored in other pmc registers.
	 * Try assuming it's ignored in Itanium, too, and just
	 * set it for everyone.
	 */

	*val |= (1 << 23);

	/* enable/disable chosen OS and USR counting */
	(user)   ? (*val |= (1 << 3))
		 : (*val &= ~(1 << 3));

	(kernel) ? (*val |= (1 << 0))
		 : (*val &= ~(1 << 0));

	/* what are we counting ? */
	*val &= ~(0xff << 8);
	*val |= ((event & 0xff) << 8);
	*val &= ~(0xf << 16);
	*val |= ((um & 0xf) << 16);
}


static void
pmu_setup(void * dummy)
{
	ulong pmc_val;
	int ii;

	/* setup each counter */
	for (ii = 0 ; ii < op_nr_counters ; ++ii) {
		if (sysctl.ctr[ii].enabled) {
			pmc_val = 0;

			set_pmd_neg(sysctl.ctr[ii].count, ii);
			pmc_fill_in(&pmc_val, sysctl.ctr[ii].kernel, 
				sysctl.ctr[ii].user, sysctl.ctr[ii].event, 
				sysctl.ctr[ii].unit_mask);

			set_pmc(pmc_val, ii);
		}
	}
}


void 
disable_psr(void * dummy)
{
	struct pt_regs * regs;
	/* disable profiling for my saved state */
	regs = (struct pt_regs *)((unsigned long) current + IA64_STK_OFFSET);
	regs--;
	ia64_psr(regs)->pp = 0;
	/* shouldn't need to */
	ia64_psr(regs)->up = 0;

	/* disable profiling for my current state */
	__asm__ __volatile__ ("rsm psr.pp;;"::: "memory");

#if defined(CONFIG_PERFMON) && defined(CONFIG_SMP)
#if V_AT_LEAST(2, 4, 21)
	local_cpu_data->pfm_syst_info |=  PFM_CPUINFO_SYST_WIDE;
	local_cpu_data->pfm_syst_info &= ~PFM_CPUINFO_DCR_PP;
	/* FIXME: what todo with the 3rd flags PFM_CPUINFO_EXCL_IDLE 0x4 */
#else
	/* disable profiling for everyone else */
	local_cpu_data->pfm_syst_wide = 1;
	local_cpu_data->pfm_dcr_pp = 0;
#endif
#endif
	ia64_set_pmc(0, 0);
	ia64_srlz_d();
}


static int
pmu_setup_all(void)
{

	/* This would be a great place to reserve all cpus with 
	 * some sort of call to perfmonctl (something like the
	 * CREATE_CONTEXT command).  The current interface to 
	 * perfmonctl wants to be called from a different task id
	 * for each CPU to be set up (and doesn't allow calls from
	 * modules.
	 */

	/* disable profiling with the psr.pp bit */
	if ((smp_call_function(disable_psr, NULL, 0, 1)))
		return -EFAULT;

	disable_psr(NULL);

	/* now I've reserved the PMUs and they should be quiet */

	if ((smp_call_function(pmu_setup, NULL, 0, 1)))
		return -EFAULT;

	pmu_setup(NULL);
	return 0;
}


#ifndef CONFIG_SMP
/* from linux/arch/ia64/kernel/perfmon.c */
/*
 * Originaly Written by Ganesh Venkitachalam, IBM Corp.
 * Copyright (C) 1999 Ganesh Venkitachalam <venkitac@us.ibm.com>
 *
 * Modifications by Stephane Eranian, Hewlett-Packard Co.
 * Modifications by David Mosberger-Tang, Hewlett-Packard Co.
 *
 * Copyright (C) 1999-2002  Hewlett Packard Co
 *               Stephane Eranian <eranian@hpl.hp.com>
 *               David Mosberger-Tang <davidm@hpl.hp.com>
 */

/*
 * On UP kernels, we do not need to constantly set the psr.pp bit
 * when a task is scheduled. The psr.pp bit can only be changed in
 * the kernel because of a user request. Given we are on a UP non preeemptive 
 * kernel we know that no other task is running, so we cna simply update their
 * psr.pp from their saved state. There is this no impact on the context switch
 * code compared to the SMP case.
 */
static void
op_tasklist_toggle_pp(unsigned int val)
{
	struct task_struct * p;
	struct pt_regs * regs;

	read_lock(&tasklist_lock);

	for_each_task(p) {
		regs = (struct pt_regs *)((unsigned long) p + IA64_STK_OFFSET);

		/*
		 * position on pt_regs saved on stack on 1st entry into the kernel
		 */
		regs--;

		/*
		 * update psr.pp
		 */
		ia64_psr(regs)->pp = val;
	}
	read_unlock(&tasklist_lock);
}
#endif


static void
pmu_start(void * info)
{
	struct pt_regs * regs;

	if (info && (*((uint *)info) != op_cpu_id()))
		return;

	/* printk(KERN_ALERT "oprofile_IA64: pmu_start on cpu %d\n", 
	  	op_cpu_id()); */
	/* The default control register pp value is copied into psr.pp
	 * on an interrupt.  This allows interrupt service routines to
	 * be monitored.
	 */
	ia64_set_dcr(ia64_get_dcr() | IA64_DCR_PP);

#ifdef CONFIG_PERFMON
#ifdef CONFIG_SMP
#if V_AT_LEAST(2, 4, 21)
	local_cpu_data->pfm_syst_info |= PFM_CPUINFO_SYST_WIDE;
	local_cpu_data->pfm_syst_info |= PFM_CPUINFO_DCR_PP;
	/* FIXME: what todo with the 3rd flags PFM_CPUINFO_EXCL_IDLE 0x4 */
#else
	local_cpu_data->pfm_syst_wide = 1;
	local_cpu_data->pfm_dcr_pp = 1;
#endif
#else
	op_tasklist_toggle_pp(1);
#endif
#endif
	/* set it in my saved state */
	regs = (struct pt_regs *)((unsigned long) current + IA64_STK_OFFSET);
	regs--;
	ia64_psr(regs)->pp = 1;

	/* set it in my current state */
	__asm__ __volatile__ ("ssm psr.pp;;"::: "memory");
	ia64_srlz_d();
}


static void
pmu_stop(void * info)
{
	struct pt_regs * regs;

	if (info && (*((uint *)info) != op_cpu_id()))
		return;

	/* stop in my current state */
	__asm__ __volatile__ ("rsm psr.pp;;"::: "memory");

	/* disable the dcr pp */
	ia64_set_dcr(ia64_get_dcr() & ~IA64_DCR_PP);

#ifdef CONFIG_PERFMON
#ifdef CONFIG_SMP
#if V_AT_LEAST(2, 4, 21)
	local_cpu_data->pfm_syst_info &= ~PFM_CPUINFO_SYST_WIDE;
	local_cpu_data->pfm_syst_info &= ~PFM_CPUINFO_DCR_PP;
	/* FIXME: what todo with the 3rd flags PFM_CPUINFO_EXCL_IDLE 0x4 */
#else
	local_cpu_data->pfm_syst_wide = 0;
	local_cpu_data->pfm_dcr_pp = 0;
#endif
#else
	pfm_tasklist_toggle_pp(0);
#endif
#endif

	/* disable in my saved state */
	regs = (struct pt_regs *)((unsigned long) current + IA64_STK_OFFSET);
	regs--;
	ia64_psr(regs)->pp = 0;
}


static void
pmu_select_start(uint cpu)
{
	if (cpu == op_cpu_id())
		pmu_start(NULL);
	else
		smp_call_function(pmu_start, &cpu, 0, 1);
}


static void
pmu_select_stop(uint cpu)
{
	if (cpu == op_cpu_id())
		pmu_stop(NULL);
	else
		smp_call_function(pmu_stop, &cpu, 0, 1);
}


static void
pmu_start_all(void)
{
	int cpu, i;
 
	for (cpu=0; cpu < smp_num_cpus; cpu++) {
		struct _oprof_data * data = &oprof_data[cpu];

		for (i = 0 ; i < op_nr_counters ; ++i) {
			if (sysctl.ctr[i].enabled) {
				data->ctr_count[i] = sysctl.ctr[i].count;
			} else {
				data->ctr_count[i] = 0;
			}
		}
	}
 
	if (!install_handler()) {
		smp_call_function(pmu_start, NULL, 0, 1);
		pmu_start(NULL);
	}
		/* FIXME need some way to fail here */;
}


static void
pmu_stop_all(void)
{
	smp_call_function(pmu_stop, NULL, 0, 1);
	pmu_stop(NULL);
	restore_handler();
}

 
static int
pmu_check_params(void)
{
	int i;
	int enabled = 0;

	for (i = 0; i < op_nr_counters ; i++) {
		if (!sysctl.ctr[i].enabled)
			continue;

		enabled = 1;

		if (!sysctl.ctr[i].user && !sysctl.ctr[i].kernel) {
			printk(KERN_ERR "oprofile: neither kernel nor user "
			       "set for counter %d\n", i);
			return -EINVAL;
		}

		if (check_range(sysctl.ctr[i].count, 1, OP_MAX_PERF_COUNT,
			"ctr count value %d not in range (%d %ld)\n"))
			return -EINVAL;
	}

	if (!enabled) {
		printk(KERN_ERR "oprofile: no counters have been enabled.\n");
		return -EINVAL;
	}

	return 0;
}


static struct op_msrs cpu_msrs[NR_CPUS];


static void free_msr_group(struct op_msr_group * group)
{
	if (group->addrs)
		kfree(group->addrs);
	if (group->saved)
		kfree(group->saved);
	group->addrs = NULL;
	group->saved = NULL;
}
 

static void pmu_save_registers(void * dummy)
{
	uint i;
	uint const cpu = op_cpu_id();
	struct op_msr_group * counters = &cpu_msrs[cpu].counters;
	struct op_msr_group * controls = &cpu_msrs[cpu].controls;

	counters->addrs = NULL; 
	counters->saved = NULL;
	controls->addrs = NULL;
	controls->saved = NULL;

	counters->saved = kmalloc(
		op_nr_counters * sizeof(struct op_saved_msr), GFP_KERNEL);
	if (!counters->saved)
		goto fault;
 
	controls->saved = kmalloc(
		op_nr_counters * sizeof(struct op_saved_msr), GFP_KERNEL);
	if (!controls->saved)
		goto fault;
 
	for (i = 0; i < op_nr_counters; ++i) {
		controls->saved[i].low = get_pmc(i);
		counters->saved[i].low = get_pmd(i);
	}
	return;

fault:
	free_msr_group(counters);
	free_msr_group(controls);
}
 

static void pmu_restore_registers(void * dummy)
{
	uint i;
	uint const cpu = op_cpu_id();
	struct op_msr_group * counters = &cpu_msrs[cpu].counters;
	struct op_msr_group * controls = &cpu_msrs[cpu].controls;

	for (i = 0; i < op_nr_counters; ++i) {
		set_pmc(controls->saved[i].low, i);
		set_pmd(counters->saved[i].low, i);
	}

	free_msr_group(counters);
	free_msr_group(controls);
}



static int
pmu_init(void)
{
	int err = 0; 

	/* figure out processor type configure number of bits in pmd
	   and number of counters */
	switch (get_cpu_type()) {
	case CPU_IA64_1:
		pmd_mask = IA64_1_PMD_MASK_VAL; break;
	case CPU_IA64_2:
	case CPU_IA64:
		pmd_mask = IA64_2_PMD_MASK_VAL; break;
	default:
		err = -EIO; break;
	}

	op_nr_counters = 4;

	if ((err = smp_call_function(pmu_save_registers, NULL, 0, 1)))
		goto out;

	pmu_save_registers(NULL);

out:
	return err;
}
 

static void
pmu_deinit(void)
{
	smp_call_function(pmu_restore_registers, NULL, 0, 1);
	pmu_restore_registers(NULL);
}
 

static char * names[] = { "0", "1", "2", "3", };


static int
pmu_add_sysctls(ctl_table * next)
{
	ctl_table * start = next; 
	ctl_table * tab; 
	int i, j;
 
	for (i=0; i < op_nr_counters; i++) {
		next->ctl_name = 1;
		next->procname = names[i];
		next->mode = 0700;

		if (!(tab = kmalloc(sizeof(ctl_table)*7, GFP_KERNEL)))
			goto cleanup;
 
		next->child = tab;

		memset(tab, 0, sizeof(ctl_table)*7);
		tab[0] = ((ctl_table) { 1, "enabled", &sysctl_parms.ctr[i].enabled, sizeof(int), 0600, NULL, lproc_dointvec, NULL, });
		tab[1] = ((ctl_table) { 1, "event", &sysctl_parms.ctr[i].event, sizeof(int), 0600, NULL, lproc_dointvec, NULL,  });
		tab[2] = ((ctl_table) { 1, "count", &sysctl_parms.ctr[i].count, sizeof(int), 0600, NULL, lproc_dointvec, NULL, });
		tab[3] = ((ctl_table) { 1, "unit_mask", &sysctl_parms.ctr[i].unit_mask, sizeof(int), 0600, NULL, lproc_dointvec, NULL, });
		tab[4] = ((ctl_table) { 1, "kernel", &sysctl_parms.ctr[i].kernel, sizeof(int), 0600, NULL, lproc_dointvec, NULL, });
		tab[5] = ((ctl_table) { 1, "user", &sysctl_parms.ctr[i].user, sizeof(int), 0600, NULL, lproc_dointvec, NULL, });
		next++;
	}

	return 0;

cleanup:
	next = start;
	for (j = 0; j < i; j++) {
		kfree(next->child);
		next++;
	}
	return -EFAULT;
}


static void pmu_remove_sysctls(ctl_table * next)
{
	int ii;

	for (ii=0; ii < op_nr_counters; ii++) {
		kfree(next->child);
		next++;
	}
}
 

struct op_int_operations op_nmi_ops = {
	init: pmu_init,
	deinit: pmu_deinit,
	add_sysctls: pmu_add_sysctls,
	remove_sysctls: pmu_remove_sysctls,
	check_params: pmu_check_params,
	setup: pmu_setup_all,
	start: pmu_start_all,
	stop: pmu_stop_all,
	start_cpu: pmu_select_start,
	stop_cpu: pmu_select_stop, 
};


struct op_int_operations const * op_int_interface()
{
	return &op_nmi_ops;
}

/* Need this dummy so module/oprofile.c links */
struct op_int_operations op_rtc_ops = {
	init: NULL,
	deinit: NULL,
	add_sysctls: NULL,
	remove_sysctls: NULL,
	check_params: NULL,
	setup: NULL,
	start: NULL,
	stop: NULL,
	start_cpu: NULL,
	stop_cpu: NULL,
};
