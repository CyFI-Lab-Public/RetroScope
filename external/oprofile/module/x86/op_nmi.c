/**
 * @file op_nmi.c
 * Setup and handling of NMI PMC interrupts
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include "oprofile.h"
#include "op_msr.h"
#include "op_apic.h"
#include "op_util.h"
#include "op_x86_model.h"

static struct op_msrs cpu_msrs[NR_CPUS];
static struct op_x86_model_spec const * model = NULL;

static struct op_x86_model_spec const * get_model(void)
{
	if (!model) {	
		/* pick out our per-model function table */
		switch (sysctl.cpu_type) {
		case CPU_ATHLON:
		case CPU_HAMMER:
			model = &op_athlon_spec;
			break;
		case CPU_P4:
			model = &op_p4_spec;
			break;
#ifdef HT_SUPPORT
		case CPU_P4_HT2:
			model = &op_p4_ht2_spec;
			break;
#endif
		default:
			model = &op_ppro_spec;
			break;
		}
	}
	return model;
}

asmlinkage void op_do_nmi(struct pt_regs * regs)
{
	uint const cpu = op_cpu_id();
	struct op_msrs const * const msrs = &cpu_msrs[cpu];

	model->check_ctrs(cpu, msrs, regs);
}

/* ---------------- PMC setup ------------------ */

static void pmc_setup_ctr(void * dummy)
{
	uint const cpu = op_cpu_id();
	struct op_msrs const * const msrs = &cpu_msrs[cpu];
	get_model()->setup_ctrs(msrs);
}


static int pmc_setup_all(void)
{
	if (smp_call_function(pmc_setup_ctr, NULL, 0, 1))
		return -EFAULT;
	pmc_setup_ctr(NULL);
	return 0;
}


static void pmc_start(void * info)
{
	uint const cpu = op_cpu_id();
	struct op_msrs const * const msrs = &cpu_msrs[cpu];

	if (info && (*((uint *)info) != cpu))
		return;

	get_model()->start(msrs);
}


static void pmc_stop(void * info)
{
	uint const cpu = op_cpu_id();
	struct op_msrs const * const msrs = &cpu_msrs[cpu];

	if (info && (*((uint *)info) != cpu))
		return;

	get_model()->stop(msrs);
}


static void pmc_select_start(uint cpu)
{
	if (cpu == op_cpu_id())
		pmc_start(NULL);
	else
		smp_call_function(pmc_start, &cpu, 0, 1);
}


static void pmc_select_stop(uint cpu)
{
	if (cpu == op_cpu_id())
		pmc_stop(NULL);
	else
		smp_call_function(pmc_stop, &cpu, 0, 1);
}


static void pmc_start_all(void)
{
	int cpu, i;

	for (cpu = 0 ; cpu < smp_num_cpus; cpu++) {
		struct _oprof_data * data = &oprof_data[cpu];

		for (i = 0 ; i < get_model()->num_counters ; ++i) {
			if (sysctl.ctr[i].enabled)
				data->ctr_count[i] = sysctl.ctr[i].count;
			else
				data->ctr_count[i] = 0;
		}
	}

	install_nmi();
	smp_call_function(pmc_start, NULL, 0, 1);
	pmc_start(NULL);
}


static void pmc_stop_all(void)
{
	smp_call_function(pmc_stop, NULL, 0, 1);
	pmc_stop(NULL);
	restore_nmi();
}


static int pmc_check_params(void)
{
	int i;
	int enabled = 0;

	for (i = 0; i < get_model()->num_counters; i++) {
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


static void free_msr_group(struct op_msr_group * group)
{
	if (group->addrs)
		kfree(group->addrs);
	if (group->saved)
		kfree(group->saved);
	group->addrs = NULL;
	group->saved = NULL;
}
 

static void pmc_save_registers(void * dummy)
{
	uint i;
	uint const cpu = op_cpu_id();
	uint const nr_ctrs = get_model()->num_counters;
	uint const nr_ctrls = get_model()->num_controls;
	struct op_msr_group * counters = &cpu_msrs[cpu].counters;
	struct op_msr_group * controls = &cpu_msrs[cpu].controls;

	counters->addrs = NULL; 
	counters->saved = NULL;
	controls->addrs = NULL;
	controls->saved = NULL;

	counters->addrs = kmalloc(nr_ctrs * sizeof(uint), GFP_KERNEL);
	if (!counters->addrs)
		goto fault;

	counters->saved = kmalloc(
		nr_ctrs * sizeof(struct op_saved_msr), GFP_KERNEL);
	if (!counters->saved)
		goto fault;
 
	controls->addrs = kmalloc(nr_ctrls * sizeof(uint), GFP_KERNEL);
	if (!controls->addrs)
		goto fault;

	controls->saved = kmalloc(
		nr_ctrls * sizeof(struct op_saved_msr), GFP_KERNEL);
	if (!controls->saved)
		goto fault;
 
	model->fill_in_addresses(&cpu_msrs[cpu]);

	for (i = 0; i < nr_ctrs; ++i) {
		rdmsr(counters->addrs[i],
			counters->saved[i].low,
			counters->saved[i].high);
	}

	for (i = 0; i < nr_ctrls; ++i) {
		rdmsr(controls->addrs[i],
			controls->saved[i].low,
			controls->saved[i].high);
	}
	return;

fault:
	free_msr_group(counters);
	free_msr_group(controls);
}
 

static void pmc_restore_registers(void * dummy)
{
	uint i;
	uint const cpu = op_cpu_id();
	uint const nr_ctrs = get_model()->num_counters;
	uint const nr_ctrls = get_model()->num_controls;
	struct op_msr_group * counters = &cpu_msrs[cpu].counters;
	struct op_msr_group * controls = &cpu_msrs[cpu].controls;

	if (controls->addrs) {
		for (i = 0; i < nr_ctrls; ++i) {
			wrmsr(controls->addrs[i],
				controls->saved[i].low,
				controls->saved[i].high);
		}
	}

	if (counters->addrs) {
		for (i = 0; i < nr_ctrs; ++i) {
			wrmsr(counters->addrs[i],
				counters->saved[i].low,
				counters->saved[i].high);
		}
	}

	free_msr_group(counters);
	free_msr_group(controls);
}
 

static int pmc_init(void)
{
	int err = 0;
 
	if ((err = smp_call_function(pmc_save_registers, NULL, 0, 1)))
		goto out;

	pmc_save_registers(NULL);
 
	if ((err = apic_setup()))
		goto out_restore;

	if ((err = smp_call_function(lvtpc_apic_setup, NULL, 0, 1))) {
		lvtpc_apic_restore(NULL);
		goto out_restore;
	}

out:
	return err;
out_restore:
	smp_call_function(pmc_restore_registers, NULL, 0, 1);
	pmc_restore_registers(NULL);
	goto out;
}

 
static void pmc_deinit(void)
{
	smp_call_function(lvtpc_apic_restore, NULL, 0, 1);
	lvtpc_apic_restore(NULL);

	apic_restore();

	smp_call_function(pmc_restore_registers, NULL, 0, 1);
	pmc_restore_registers(NULL);
}
 

static char * names[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8"};

static int pmc_add_sysctls(ctl_table * next)
{
	ctl_table * start = next;
	ctl_table * tab;
	int i, j;

	/* now init the sysctls */
	for (i=0; i < get_model()->num_counters; i++) {
		next->ctl_name = 1;
		next->procname = names[i];
		next->mode = 0755;

		if (!(tab = kmalloc(sizeof(ctl_table)*7, GFP_KERNEL)))
			goto cleanup;

		next->child = tab;

		memset(tab, 0, sizeof(ctl_table)*7);
		tab[0] = ((ctl_table) { 1, "enabled", &sysctl_parms.ctr[i].enabled, sizeof(int), 0644, NULL, lproc_dointvec, NULL, });
		tab[1] = ((ctl_table) { 1, "event", &sysctl_parms.ctr[i].event, sizeof(int), 0644, NULL, lproc_dointvec, NULL,  });
		tab[2] = ((ctl_table) { 1, "count", &sysctl_parms.ctr[i].count, sizeof(int), 0644, NULL, lproc_dointvec, NULL, });
		tab[3] = ((ctl_table) { 1, "unit_mask", &sysctl_parms.ctr[i].unit_mask, sizeof(int), 0644, NULL, lproc_dointvec, NULL, });
		tab[4] = ((ctl_table) { 1, "kernel", &sysctl_parms.ctr[i].kernel, sizeof(int), 0644, NULL, lproc_dointvec, NULL, });
		tab[5] = ((ctl_table) { 1, "user", &sysctl_parms.ctr[i].user, sizeof(int), 0644, NULL, lproc_dointvec, NULL, });
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

 
static void pmc_remove_sysctls(ctl_table * next)
{
	int i;
	for (i=0; i < get_model()->num_counters; i++) {
		kfree(next->child);
		next++;
	}
}

 
static struct op_int_operations op_nmi_ops = {
	init: pmc_init,
	deinit: pmc_deinit,
	add_sysctls: pmc_add_sysctls,
	remove_sysctls: pmc_remove_sysctls,
	check_params: pmc_check_params,
	setup: pmc_setup_all,
	start: pmc_start_all,
	stop: pmc_stop_all,
	start_cpu: pmc_select_start,
	stop_cpu: pmc_select_stop,
};


struct op_int_operations const * op_int_interface()
{
	return &op_nmi_ops;
}
