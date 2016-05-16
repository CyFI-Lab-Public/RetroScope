/**
 * @file compat24.h
 * Compatability functions for 2.4 kernels
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef COMPAT24_H
#define COMPAT24_H

#include <linux/version.h>

static inline pid_t op_get_tgid(void)
{
	return current->tgid;
}

#define pte_page_address(a) page_address(pte_page(a))
#define oprof_wake_up(w) wake_up(w)
#define lock_rtc(f) spin_lock_irqsave(&rtc_lock, f)
#define unlock_rtc(f) spin_unlock_irqrestore(&rtc_lock, f)
#define wind_dentries(d, v, r, m) wind_dentries_2_4(d, v, r, m)
extern uint do_path_hash_2_4(struct dentry * dentry, struct vfsmount * vfsmnt);
#define hash_path(f) do_path_hash_2_4((f)->f_dentry, (f)->f_vfsmnt)
#define request_region_check request_region
#define op_cpu_id() cpu_number_map(smp_processor_id())
#define GET_VM_OFFSET(v) ((v)->vm_pgoff << PAGE_SHIFT)
#define PTRACE_OFF(t) ((t)->ptrace &= ~PT_DTRACE)
#define lock_execve() do { } while (0)
#define unlock_execve() do { } while (0)
#define lock_out_mmap() do { } while (0)
#define unlock_out_mmap() do { } while (0)
#define HAVE_MMAP2
#define HAVE_FILE_OPERATIONS_OWNER

/* ->owner field in 2.4 */
#define INC_USE_COUNT_MAYBE
#define DEC_USE_COUNT_MAYBE

/* no global waitqueue spinlock in 2.4 */
#define wq_is_lockable() (1)

/* 2.4.3 introduced rw mmap semaphore  */
#if V_AT_LEAST(2, 4, 3)
	#define lock_mmap(mm) down_read(&mm->mmap_sem)
	#define unlock_mmap(mm) up_read(&mm->mmap_sem)
#else
	#define lock_mmap(mm) down(&mm->mmap_sem)
	#define unlock_mmap(mm) up(&mm->mmap_sem)
#endif

/* 2.4.26 exported the needed stuff for HT support */
#if V_AT_LEAST(2, 4, 26) && defined(CONFIG_SMP)
#define HT_SUPPORT
#endif

#endif /* COMPAT24_H */
