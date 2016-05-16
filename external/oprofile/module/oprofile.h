/**
 * @file oprofile.h
 * Main driver code
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OPROFILE_H
#define OPROFILE_H

#include <linux/version.h>
#include <linux/module.h>
#include <linux/config.h>
#include <linux/kernel.h>
#include <linux/init.h>
#include <linux/slab.h>
#include <linux/delay.h>
#include <linux/vmalloc.h>
#include <linux/sched.h>
#include <linux/sysctl.h>
#include <linux/smp_lock.h>

#include <asm/uaccess.h>

#include "compat.h"

#include "op_config_24.h"
#include "op_hw_config.h"
#include "op_interface.h"
#include "op_cpu_type.h"

#undef min
#undef max

#define streq(a, b) (!strcmp((a), (b)))

/* per-cpu dynamic data */
struct _oprof_data {
	/* eviction buffer */
	struct op_sample * buffer;
	/* nr. in buffer */
	uint buf_size;
	/* we try to wakeup when nextbuf >= buf_watermark */
	uint buf_watermark;
	/* next in buffer (atomic) */
	uint nextbuf;
	/* number of IRQs for this CPU */
	uint nr_irq;
	/* buffer overflow cumulated size */
	uint nr_buffer_overflow;
	/* reset counter values */
	uint ctr_count[OP_MAX_COUNTERS];
};

/* reflect /proc/sys/dev/oprofile/#counter files */
struct oprof_counter {
	int count;
	int enabled;
	int event;
	int kernel;
	int user;
	int unit_mask;
};

/* reflect /proc/sys/dev/oprofile files */
struct oprof_sysctl {
	/* nr. in eviction buffser */
	int buf_size;
	/* sysctl dump */
	int dump;
	/* dump and stop */
	int dump_stop;
	/* nr. in note buffer */
	int note_size;
	/* nr. interrupts occured */
	int nr_interrupts;
	/* the cpu core type: CPU_PPRO, CPU_PII ... */
	int cpu_type;
	/* nr note buffer overflow */
	int nr_note_buffer_overflow;
	/* nr buffer overflow */
	int nr_buffer_overflow;
	/* counter setup */
	struct oprof_counter ctr[OP_MAX_COUNTERS];
};

/**
 * A interrupt handler must implement these routines.
 * When an interrupt arrives, it must eventually call
 * op_do_profile().
 */
struct op_int_operations {
	/* initialise the interrupt handler on module load.
	 * On failure deinit handler is not called so all resources
	 * allocated by init() must be freed before returning an error code
	 * (or 0 on success)
	 */
	int (*init)(void);
	/* deinitialise on module unload */
	void (*deinit)(void);
	/* add any handler-specific sysctls at the position given by @next. Return 0 on success */
	int (*add_sysctls)(ctl_table * next);
	/* remove handler-specific sysctls */
	void (*remove_sysctls)(ctl_table * next);
	/* check given profiling parameters are correct. Return 0 on success */
	int (*check_params)(void);
	/* setup the handler from profiling parameters. Return 0 on success */
	int (*setup)(void);
	/* start profiling on all CPUs */
	void (*start)(void);
	/* stop profiling on all CPUs */
	void (*stop)(void);
	/* start profiling on the given CPU */
	void (*start_cpu)(uint);
	/* stop profiling on the given CPU */
	void (*stop_cpu)(uint);
};

/* maximum depth of dname trees - this is just a page */
#define DNAME_STACK_MAX 1024

/* oprof_start() copy here the sysctl settable parameters */
extern struct oprof_sysctl sysctl;

int oprof_init(void);
void oprof_exit(void);
unsigned long is_map_ready(void);
int oprof_hash_map_open(void);
int oprof_hash_map_release(void);
int oprof_hash_map_mmap(struct file * file, struct vm_area_struct * vma);
int oprof_map_open(void);
int oprof_map_release(void);
int oprof_init_hashmap(void);
void oprof_free_hashmap(void);

/* used by interrupt handlers if the underlined harware doesn't support
 * performance counter */
extern struct op_int_operations op_rtc_ops;

void op_do_profile(uint cpu, long eip, long irq_enabled, int ctr);
extern struct _oprof_data oprof_data[NR_CPUS];
extern struct oprof_sysctl sysctl_parms;
extern int lproc_dointvec(ctl_table * table, int write, struct file * filp, void * buffer, size_t * lenp);

/* functionality provided by the architecture dependent file */
/* must return OP_RTC if the hardware doesn't support something like
 * perf counter */
op_cpu get_cpu_type(void);
/* return an interface pointer, this function is called only if get_cpu_type
 * doesn't return OP_RTC */
struct op_int_operations const * op_int_interface(void);
/* intercept the needed syscall */
void op_intercept_syscalls(void);
void op_restore_syscalls(void);
void op_save_syscalls(void);

#endif /* OPROFILE_H */
