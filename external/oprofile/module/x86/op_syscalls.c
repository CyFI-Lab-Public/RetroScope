/**
 * @file op_syscalls.c
 * Tracing of system calls
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <linux/sched.h>
#include <linux/unistd.h>
#include <linux/mman.h>
#include <linux/file.h>

#include "oprofile.h"

void oprof_put_note(struct op_note * samp);
void __oprof_put_note(struct op_note * samp);

extern spinlock_t note_lock;

/* ------------ system calls --------------- */

struct mmap_arg_struct {
	unsigned long addr;
        unsigned long len;
        unsigned long prot;
        unsigned long flags;
        unsigned long fd;
        unsigned long offset;
};

asmlinkage static int (*old_sys_fork)(struct pt_regs);
asmlinkage static int (*old_sys_vfork)(struct pt_regs);
asmlinkage static int (*old_sys_clone)(struct pt_regs);
asmlinkage static int (*old_sys_execve)(struct pt_regs);
asmlinkage static int (*old_old_mmap)(struct mmap_arg_struct *);
#ifdef HAVE_MMAP2
asmlinkage static long (*old_sys_mmap2)(ulong, ulong, ulong, ulong, ulong, ulong);
#endif
asmlinkage static long (*old_sys_init_module)(char const *, struct module *);
asmlinkage static long (*old_sys_exit)(int);

/* called with note_lock held */
static void oprof_output_map(ulong addr, ulong len, ulong offset,
			     struct file * file, int is_execve)
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
	/* holding note lock */
	__oprof_put_note(&note);
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
	spin_lock(&note_lock);

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

	spin_unlock(&note_lock);
	unlock_mmap(mm);

out:
	return size;
}

asmlinkage static int my_sys_execve(struct pt_regs regs)
{
	char * filename;
	int ret;

	MOD_INC_USE_COUNT;

	lock_execve();

	filename = getname((char *)regs.ebx);
	if (IS_ERR(filename)) {
		ret = PTR_ERR(filename);
		goto out;
	}
	ret = do_execve(filename, (char **)regs.ecx, (char **)regs.edx, &regs);

	if (!ret) {
		PTRACE_OFF(current);
		oprof_output_maps(current);
	}

	putname(filename);

out:
	unlock_execve();
	MOD_DEC_USE_COUNT;
        return ret;
}

static void out_mmap(ulong addr, ulong len, ulong prot, ulong flags, ulong fd,
		     ulong offset)
{
	struct file * file;

	lock_out_mmap();

	file = fget(fd);
	if (!file)
		goto out;

	spin_lock(&note_lock);
	oprof_output_map(addr, len, offset, file, 0);
	spin_unlock(&note_lock);

	fput(file);

out:
	unlock_out_mmap();
}

#ifdef HAVE_MMAP2
asmlinkage static int my_sys_mmap2(ulong addr, ulong len,
	ulong prot, ulong flags, ulong fd, ulong pgoff)
{
	int ret;

	MOD_INC_USE_COUNT;

	ret = old_sys_mmap2(addr, len, prot, flags, fd, pgoff);

	if ((prot & PROT_EXEC) && ret >= 0)
		out_mmap(ret, len, prot, flags, fd, pgoff << PAGE_SHIFT);

	MOD_DEC_USE_COUNT;
	return ret;
}
#endif

asmlinkage static int my_old_mmap(struct mmap_arg_struct * arg)
{
	int ret;

	MOD_INC_USE_COUNT;

	ret = old_old_mmap(arg);

	if (ret >= 0) {
		struct mmap_arg_struct a;

		if (copy_from_user(&a, arg, sizeof(a))) {
			ret = -EFAULT;
			goto out;
		}

		if (a.prot&PROT_EXEC)
			out_mmap(ret, a.len, a.prot, a.flags, a.fd, a.offset);
	}

out:
	MOD_DEC_USE_COUNT;
	return ret;
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

asmlinkage static int my_sys_fork(struct pt_regs regs)
{
	u32 pid = current->pid;
	u32 tgid = op_get_tgid();
	int ret;

	MOD_INC_USE_COUNT;

	ret = old_sys_fork(regs);
	if (ret)
		oprof_report_fork(pid, ret, tgid, ret);
	MOD_DEC_USE_COUNT;
	return ret;
}

asmlinkage static int my_sys_vfork(struct pt_regs regs)
{
	u32 pid = current->pid;
	u32 tgid = op_get_tgid();
	int ret;

	MOD_INC_USE_COUNT;
	ret = old_sys_vfork(regs);
	if (ret)
		oprof_report_fork(pid, ret, tgid, ret);
	MOD_DEC_USE_COUNT;
	return ret;
}

asmlinkage static int my_sys_clone(struct pt_regs regs)
{
	u32 pid = current->pid;
	u32 tgid = op_get_tgid();
#if V_AT_LEAST(2, 4, 0)
	u32 clone_flags = regs.ebx;
#endif
	int ret;

	MOD_INC_USE_COUNT;
	ret = old_sys_clone(regs);
	if (ret) {
#if V_AT_LEAST(2, 4, 0)
		if (clone_flags & CLONE_THREAD)
			oprof_report_fork(pid, ret, tgid, tgid);
		else
#endif
			oprof_report_fork(pid, ret, tgid, ret);
	}
	MOD_DEC_USE_COUNT;
	return ret;
}

asmlinkage static long my_sys_init_module(char const * name_user, struct module * mod_user)
{
	long ret;

	MOD_INC_USE_COUNT;

	ret = old_sys_init_module(name_user, mod_user);

	if (ret >= 0) {
		struct op_note note;

		note.type = OP_DROP_MODULES;
		oprof_put_note(&note);
	}
	MOD_DEC_USE_COUNT;
	return ret;
}

/* used from do_nmi */
asmlinkage long my_sys_exit(int error_code)
{
	struct op_note note;

	MOD_INC_USE_COUNT;

	note.type = OP_EXIT;
	note.pid = current->pid;
	note.tgid = op_get_tgid();
	oprof_put_note(&note);

	/* this looks UP-dangerous, as the exit sleeps and we don't
	 * have a use count, but in fact its ok as sys_exit is noreturn,
	 * so we can never come back to this non-existent exec page
	 */
	MOD_DEC_USE_COUNT;
	return old_sys_exit(error_code);
}

extern void * sys_call_table[];

void op_save_syscalls(void)
{
	old_sys_fork = sys_call_table[__NR_fork];
	old_sys_vfork = sys_call_table[__NR_vfork];
	old_sys_clone = sys_call_table[__NR_clone];
	old_sys_execve = sys_call_table[__NR_execve];
	old_old_mmap = sys_call_table[__NR_mmap];
#ifdef HAVE_MMAP2
	old_sys_mmap2 = sys_call_table[__NR_mmap2];
#endif
	old_sys_init_module = sys_call_table[__NR_init_module];
	old_sys_exit = sys_call_table[__NR_exit];
}

void op_intercept_syscalls(void)
{
	sys_call_table[__NR_fork] = my_sys_fork;
	sys_call_table[__NR_vfork] = my_sys_vfork;
	sys_call_table[__NR_clone] = my_sys_clone;
	sys_call_table[__NR_execve] = my_sys_execve;
	sys_call_table[__NR_mmap] = my_old_mmap;
#ifdef HAVE_MMAP2
	sys_call_table[__NR_mmap2] = my_sys_mmap2;
#endif
	sys_call_table[__NR_init_module] = my_sys_init_module;
	sys_call_table[__NR_exit] = my_sys_exit;
}

void op_restore_syscalls(void)
{
	sys_call_table[__NR_fork] = old_sys_fork;
	sys_call_table[__NR_vfork] = old_sys_vfork;
	sys_call_table[__NR_clone] = old_sys_clone;
	sys_call_table[__NR_execve] = old_sys_execve;
	sys_call_table[__NR_mmap] = old_old_mmap;
#ifdef HAVE_MMAP2
	sys_call_table[__NR_mmap2] = old_sys_mmap2;
#endif
	sys_call_table[__NR_init_module] = old_sys_init_module;
	sys_call_table[__NR_exit] = old_sys_exit;
}
