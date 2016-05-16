/**
 * @file op_syscalls.c
 * Tracing of system calls
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Bob Montgomery
 * @author Will Cohen
 * @author John Levon
 * @author Philippe Elie
 */

#include <linux/sched.h>
#include <linux/unistd.h>
#include <linux/mman.h>
#include <linux/file.h>

#include "oprofile.h"
#include "op_dcache.h"
#include "op_util.h"

uint dname_top;
struct qstr **dname_stack;
char * pool_pos;
char * pool_start;
char * pool_end;
 
void oprof_put_note(struct op_note * samp);

/* ------------ system calls --------------- */

struct mmap_arg_struct {
	unsigned long addr;
        unsigned long len;
        unsigned long prot;
        unsigned long flags;
        unsigned long fd;
        unsigned long offset;
};

/* --------- IA64 versions of system calls ------ */
asmlinkage static int (*old_sys_clone)(long, long);
asmlinkage static int (*old_sys_clone2)(long, long, long);
asmlinkage static int (*old_sys_execve)(char *, char **, char **);
asmlinkage static unsigned long (*old_sys_mmap)(unsigned long,
				unsigned long, int, int, int, long);
asmlinkage static unsigned long (*old_sys_mmap2)(unsigned long,
				unsigned long, int, int, int, long);
asmlinkage static long (*old_sys_init_module)(char const *, struct module *);
asmlinkage static long (*old_sys_exit)(int);

/* --------- declarations of interception stubs for IA64  ------ */
asmlinkage long post_stub_clone(long, long);
asmlinkage long post_stub_clone2(long, long, long);
asmlinkage long my_ia64_execve(char *, char **, char **);
asmlinkage unsigned long post_stub_mmap(unsigned long,
					unsigned long, int, int, int, long);
asmlinkage unsigned long post_stub_mmap2(unsigned long,
					unsigned long, int, int, int, long);
asmlinkage long post_stub_init_module(char const *, struct module *);
asmlinkage long pre_stub_exit(int);

/* IA64 system call table doesn't use function pointers, it uses
 * pointers to code (not the same thing).  Basically it can violate the
 * procedure calling rules because these "procedure calls" are made by
 * the assembly language BREAK handler in ivt.S.
 */

struct fdesc {
	void * ip;
	void * gp;
};

struct fdesc fdesc_clone;
struct fdesc fdesc_clone2;
struct fdesc fdesc_execve;
struct fdesc fdesc_mmap;
struct fdesc fdesc_mmap2;
struct fdesc fdesc_init_module;
struct fdesc fdesc_exit;
/* ----------- End of IA64 weirdness for now -------------- */

spinlock_t map_lock = SPIN_LOCK_UNLOCKED;

/* called with map_lock held */
static void oprof_output_map(ulong addr, ulong len,
	ulong offset, struct file * file, int is_execve)
{
	struct op_note note;

	/* don't bother with /dev/zero mappings etc. */
	if (!len)
		return;

	note.pid = current->pid;
	note.tgid = op_get_tgid();
	note.addr = addr;
	note.len = len;
	note.offset = offset;
	note.type = is_execve ? OP_EXEC : OP_MAP;
	note.hash = hash_path(file);
	if (note.hash == -1)
		return;
	oprof_put_note(&note);
}

static int oprof_output_maps(struct task_struct * task)
{
	int size=0;
	struct mm_struct * mm;
	struct vm_area_struct * map;

	/* we don't need to worry about mm_users here, since there is at
	   least one user (current), and if there's other code using this
	   mm, then mm_users must be at least 2; we should never have to
	   mmput() here. */

	if (!(mm = task->mm))
		goto out;

	lock_mmap(mm);
	spin_lock(&map_lock);

	/* We need two pass, daemon assume than the first mmap notification
	 * is for the executable but some process doesn't follow this model.
	 */
	for (map = mm->mmap; map; map = map->vm_next) {
		if (!(map->vm_flags & VM_EXEC) || !map->vm_file)
			continue;
		if (!(map->vm_flags & VM_EXECUTABLE))
			continue;

		oprof_output_map(map->vm_start, map->vm_end-map->vm_start,
			GET_VM_OFFSET(map), map->vm_file, 1);
	}
	for (map = mm->mmap; map; map = map->vm_next) {
		if (!(map->vm_flags & VM_EXEC) || !map->vm_file)
			continue;
		if (map->vm_flags & VM_EXECUTABLE)
			continue;

		oprof_output_map(map->vm_start, map->vm_end-map->vm_start,
			GET_VM_OFFSET(map), map->vm_file, 0);
	}
	spin_unlock(&map_lock);
	unlock_mmap(mm);

out:
	return size;
}


/* execve is a special case on IA64.  The others get the result and
 * arguments after the system call has been made from the ASM stub. */

asmlinkage long
my_sys_execve (char * filename, char **argv, char **envp, struct pt_regs * regs)
{
	int error;

	MOD_INC_USE_COUNT;

	filename = getname(filename);
	error = PTR_ERR(filename);
	if (IS_ERR(filename))
		goto out;
	error = do_execve(filename, argv, envp, regs);

	if (!error) {
		PTRACE_OFF(current);
		oprof_output_maps(current);
	}
	putname(filename);
out:
	unlock_execve();
	MOD_DEC_USE_COUNT;
	return error;
}


static void out_mmap(ulong addr, ulong len, ulong prot, ulong flags,
	ulong fd, ulong offset)
{
	struct file * file;

	lock_out_mmap();
 
	file = fget(fd);
	if (!file)
		goto out;

	spin_lock(&map_lock);
	oprof_output_map(addr, len, offset, file, 0);
	spin_unlock(&map_lock);

	fput(file);

out:
	unlock_out_mmap();
}


/* 
 * IA64 mmap routines:
 * The post_sys_* routines are called after the syscall has been made.
 * The first argument is the return value from the system call.
 */
asmlinkage void post_sys_mmap2(ulong ret, ulong addr, ulong len,
	ulong prot, ulong flags, ulong fd, ulong pgoff)
{
	/* FIXME: This should be done in the ASM stub. */
	MOD_INC_USE_COUNT;

	if ((prot & PROT_EXEC) && ret >= 0)
		out_mmap(ret, len, prot, flags, fd, pgoff << PAGE_SHIFT);
	goto out;
out:
	MOD_DEC_USE_COUNT;
}

asmlinkage void post_sys_mmap(ulong ret, ulong addr, ulong len,
	ulong prot, ulong flags, ulong fd, ulong off)
{
	/* FIXME: This should be done in the ASM stub. */
	MOD_INC_USE_COUNT;

	if ((prot & PROT_EXEC) && ret >= 0)
		out_mmap(ret, len, prot, flags, fd, off);
	goto out;
out:
	MOD_DEC_USE_COUNT;
}


inline static void oprof_report_fork(u32 old_pid, u32 new_pid, u32 old_tgid, u32 new_tgid)
{
	struct op_note note;

	note.type = OP_FORK;
	note.pid = old_pid;
	note.tgid = old_tgid;
	note.addr = new_pid;
	note.len = new_tgid;
	oprof_put_note(&note);
}


asmlinkage void post_sys_clone(long ret, long arg0, long arg1)
{
	u32 pid = current->pid;
	u32 tgid = op_get_tgid();

	/* FIXME: This should be done in the ASM stub. */
	MOD_INC_USE_COUNT;

	if (ret)
		/* FIXME: my libc show clone() is not implemented in ia64 
		 * but used only by fork() with a SIGCHILD first parameter
		 * so we assume it's a fork */
		oprof_report_fork(pid, ret, pid, tgid);
	MOD_DEC_USE_COUNT;
}

asmlinkage void post_sys_clone2(long ret, long arg0, long arg1, long arg2)
{
	u32 pid = current->pid;
	u32 tgid = op_get_tgid();
	long clone_flags = arg0;

	/* FIXME: This should be done in the ASM stub. */
	MOD_INC_USE_COUNT;

	if (ret) {
		if (clone_flags & CLONE_THREAD)
			oprof_report_fork(pid, ret, tgid, tgid);
		else
			oprof_report_fork(pid, ret, tgid, ret);
	}
	MOD_DEC_USE_COUNT;
}

asmlinkage void
post_sys_init_module(long ret, char const * name_user,
                     struct module * mod_user)
{
	/* FIXME: This should be done in the ASM stub. */
	MOD_INC_USE_COUNT;

	if (ret >= 0) {
		struct op_note note;

		note.type = OP_DROP_MODULES;
		oprof_put_note(&note);
	}
	MOD_DEC_USE_COUNT;
}

/* Exit must use a pre-call intercept stub.  There is no post exit. */
asmlinkage void pre_sys_exit(int error_code)
{
	struct op_note note;

	MOD_INC_USE_COUNT;

	note.addr = current->times.tms_utime;
	note.len = current->times.tms_stime;
	note.offset = current->start_time;
	note.type = OP_EXIT;
	note.pid = current->pid;
	note.tgid = op_get_tgid();
	oprof_put_note(&note);

	/* this looks UP-dangerous, as the exit sleeps and we don't
	 * have a use count, but in fact its ok as sys_exit is noreturn,
	 * so we can never come back to this non-existent exec page
	 */
	MOD_DEC_USE_COUNT;
}

extern void * sys_call_table[];

/* FIXME:  Now that I'm never trying to do a C-level call through these
 * pointers, I should just save, intercept, and restore with void *
 * instead of the void * part of the function descriptor, I think.
 */

void op_save_syscalls(void)
{
	fdesc_clone.ip = sys_call_table[__NR_clone - __NR_ni_syscall];
	old_sys_clone = (void *)&fdesc_clone;
	fdesc_clone2.ip = sys_call_table[__NR_clone2 - __NR_ni_syscall];
	old_sys_clone2 = (void *)&fdesc_clone2;
	fdesc_execve.ip = sys_call_table[__NR_execve - __NR_ni_syscall];
	old_sys_execve = (void *)&fdesc_execve;
	fdesc_mmap.ip = sys_call_table[__NR_mmap - __NR_ni_syscall];
	old_sys_mmap = (void *)&fdesc_mmap;
	fdesc_mmap2.ip = sys_call_table[__NR_mmap2 - __NR_ni_syscall];
	old_sys_mmap2 = (void *)&fdesc_mmap2;
	fdesc_init_module.ip = sys_call_table[__NR_init_module - __NR_ni_syscall];
	old_sys_init_module = (void *)&fdesc_init_module;
	fdesc_exit.ip = sys_call_table[__NR_exit - __NR_ni_syscall];
	old_sys_exit = (void *)&fdesc_exit;
}

void op_intercept_syscalls(void)
{
	/* Must extract the function address from the stub function
	 * descriptors.
	 */
	sys_call_table[__NR_clone - __NR_ni_syscall] = 
		((struct fdesc *)post_stub_clone)->ip;
	sys_call_table[__NR_clone2 - __NR_ni_syscall] = 
		((struct fdesc *)post_stub_clone2)->ip;
	sys_call_table[__NR_execve - __NR_ni_syscall] = 
		((struct fdesc *)my_ia64_execve)->ip;
	sys_call_table[__NR_mmap - __NR_ni_syscall] = 
		((struct fdesc *)post_stub_mmap)->ip;
	sys_call_table[__NR_mmap2 - __NR_ni_syscall] = 
		((struct fdesc *)post_stub_mmap2)->ip;
	sys_call_table[__NR_init_module - __NR_ni_syscall] = 
		((struct fdesc *)post_stub_init_module)->ip;
	sys_call_table[__NR_exit - __NR_ni_syscall] = 
		((struct fdesc *)pre_stub_exit)->ip;
}

void op_restore_syscalls(void)
{
	sys_call_table[__NR_clone - __NR_ni_syscall] = 
		((struct fdesc *)old_sys_clone)->ip;
	sys_call_table[__NR_clone2 - __NR_ni_syscall] = 
		((struct fdesc *)old_sys_clone2)->ip;
	sys_call_table[__NR_execve - __NR_ni_syscall] = 
		((struct fdesc *)old_sys_execve)->ip;
	sys_call_table[__NR_mmap - __NR_ni_syscall] = 
		((struct fdesc *)old_sys_mmap)->ip;
	sys_call_table[__NR_mmap2 - __NR_ni_syscall] = 
		((struct fdesc *)old_sys_mmap2)->ip;
	sys_call_table[__NR_init_module - __NR_ni_syscall] = 
		((struct fdesc *)old_sys_init_module)->ip;
	sys_call_table[__NR_exit - __NR_ni_syscall] = 
		((struct fdesc *)old_sys_exit)->ip;
}
