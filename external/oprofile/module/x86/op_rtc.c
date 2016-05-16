/**
 * @file op_rtc.c
 * Setup and handling of RTC interrupts
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Bob Montgomery
 * @author Philippe Elie
 * @author John Levon
 */

#include <linux/ioport.h>
#include <linux/mc146818rtc.h>
#include <asm/ptrace.h>

#include "oprofile.h"
#include "op_arch.h"
#include "op_util.h"

#define RTC_IO_PORTS 2

/* not in 2.2 */
#ifndef RTC_IRQ
#define RTC_IRQ 8
#endif

/* ---------------- RTC handler ------------------ */

static void do_rtc_interrupt(int irq, void * dev_id, struct pt_regs * regs)
{
	uint cpu = op_cpu_id();
	unsigned char intr_flags;
	unsigned long flags;

	int usermode = user_mode(regs);
	if ((sysctl.ctr[0].kernel && usermode)
		|| (sysctl.ctr[0].user && !usermode))
		return;

	lock_rtc(flags);

	/* read and ack the interrupt */
	intr_flags = CMOS_READ(RTC_INTR_FLAGS);
	/* Is this my type of interrupt? */
	if (intr_flags & RTC_PF) {
		op_do_profile(cpu, instruction_pointer(regs), IRQ_ENABLED(regs), 0);
	}

	unlock_rtc(flags);

	return;
}

static int rtc_setup(void)
{
	unsigned char tmp_control;
	unsigned long flags;
	unsigned char tmp_freq_select;
	unsigned long target;
	unsigned int exp, freq;

	lock_rtc(flags);

	/* disable periodic interrupts */
	tmp_control = CMOS_READ(RTC_CONTROL);
	tmp_control &= ~RTC_PIE;
	CMOS_WRITE(tmp_control, RTC_CONTROL);
	CMOS_READ(RTC_INTR_FLAGS);

	/* Set the frequency for periodic interrupts by finding the
	 * closest power of two within the allowed range.
	 */

	target = sysctl.ctr[0].count;

	exp = 0;
	while (target > (1 << exp) + ((1 << exp) >> 1))
		exp++;
	freq = 16 - exp;

	tmp_freq_select = CMOS_READ(RTC_FREQ_SELECT);
	tmp_freq_select = (tmp_freq_select & 0xf0) | freq;
	CMOS_WRITE(tmp_freq_select, RTC_FREQ_SELECT);

	/* Update /proc with the actual frequency. */
	sysctl_parms.ctr[0].count = sysctl.ctr[0].count = 1 << exp;

	unlock_rtc(flags);
	return 0;
}

static void rtc_start(void)
{
	unsigned char tmp_control;
	unsigned long flags;

	lock_rtc(flags);

	/* Enable periodic interrupts */
	tmp_control = CMOS_READ(RTC_CONTROL);
	tmp_control |= RTC_PIE;
	CMOS_WRITE(tmp_control, RTC_CONTROL);

	/* read the flags register to start interrupts */
	CMOS_READ(RTC_INTR_FLAGS);

	unlock_rtc(flags);
}

static void rtc_stop(void)
{
	unsigned char tmp_control;
	unsigned long flags;

	lock_rtc(flags);

	/* disable periodic interrupts */
	tmp_control = CMOS_READ(RTC_CONTROL);
	tmp_control &= ~RTC_PIE;
	CMOS_WRITE(tmp_control, RTC_CONTROL);
	CMOS_READ(RTC_INTR_FLAGS);

	unlock_rtc(flags);
}

static void rtc_start_cpu(uint cpu)
{
	rtc_start();
}

static void rtc_stop_cpu(uint cpu)
{
	rtc_stop();
}

static int rtc_check_params(void)
{
	int target = sysctl.ctr[0].count;

	if (check_range(target, OP_MIN_RTC_COUNT, OP_MAX_RTC_COUNT,
		"RTC value %d is out of range (%d-%d)\n"))
		return -EINVAL;

	return 0;
}

static int rtc_init(void)
{
	 /* request_region returns 0 on **failure** */
	if (!request_region_check(RTC_PORT(0), RTC_IO_PORTS, "oprofile")) {
		printk(KERN_ERR "oprofile: can't get RTC I/O Ports\n");
		return -EBUSY;
	}

	/* request_irq returns 0 on **success** */
	if (request_irq(RTC_IRQ, do_rtc_interrupt,
			SA_INTERRUPT, "oprofile", NULL)) {
		printk(KERN_ERR "oprofile: IRQ%d busy \n", RTC_IRQ);
		release_region(RTC_PORT(0), RTC_IO_PORTS);
		return -EBUSY;
	}
	return 0;
}

static void rtc_deinit(void)
{
	free_irq(RTC_IRQ, NULL);
	release_region(RTC_PORT(0), RTC_IO_PORTS);
}

static int rtc_add_sysctls(ctl_table * next)
{
	*next = ((ctl_table) { 1, "rtc_value", &sysctl_parms.ctr[0].count, sizeof(int), 0600, NULL, lproc_dointvec, NULL, });
	return 0;
}

static void rtc_remove_sysctls(ctl_table * next)
{
	/* nothing to do */
}

struct op_int_operations op_rtc_ops = {
	init: rtc_init,
	deinit: rtc_deinit,
	add_sysctls: rtc_add_sysctls,
	remove_sysctls: rtc_remove_sysctls,
	check_params: rtc_check_params,
	setup: rtc_setup,
	start: rtc_start,
	stop: rtc_stop,
	start_cpu: rtc_start_cpu,
	stop_cpu: rtc_stop_cpu,
};
