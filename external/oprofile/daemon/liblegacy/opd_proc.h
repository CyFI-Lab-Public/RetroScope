/**
 * @file opd_proc.h
 * Management of processes
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OPD_PROC_H
#define OPD_PROC_H

#include "op_types.h"
#include "op_list.h"

struct opd_map;
struct opd_image;
struct op_note;
struct op_sample;

/**
 * track process, created either by a fork or an exec notification.
 */
struct opd_proc {
	/** maps are always added to the end of head, so search will be done
	 * from the newest map to the oldest which mean we don't care about
	 * munmap. First added map must be the primary image */
	struct list_head maps;
	/** process name */
	char const * name;
	/** thread id for this process, always equal to tgid for 2.2 kernel */
	pid_t tid;
	/** thread group id for this process */
	pid_t tgid;
	/** non-zero if this process receive any samples, this field
	 * is used with dead field to defer opd_proc deletion */
	int accessed;
	/** Set to non-zero when an exit notification occur for this process */
	int dead;
	/** used by container of opd_proc */
	struct list_head next;
};

/**
 * initialize opd_proc container
 */
void opd_init_procs(void);

/**
 * opd_put_sample - process a sample
 * @param sample  sample to process
 *
 * Write out the sample to the appropriate sample file. This
 * routine handles kernel and module samples as well as ordinary ones.
 */
void opd_put_sample(struct op_sample const * sample);

/**
 * opd_put_image_sample - write sample to file
 * @param image  image for sample
 * @param offset  (file) offset to write to
 * @param counter  counter number
 *
 * Add to the count stored at position offset in the
 * image file. Overflow pins the count at the maximum
 * value.
 */
void opd_put_image_sample(struct opd_image * image, unsigned long offset, u32 counter);

/**
 * opd_handle_fork - deal with fork notification
 * @param note  note to handle
 *
 * Deal with a fork() notification by creating a new process
 * structure, and copying mapping information from the old process.
 *
 * sample->pid contains the process id of the old process.
 * sample->eip contains the process id of the new process.
 */
void opd_handle_fork(struct op_note const * note);

/**
 * opd_handle_exec - deal with notification of execve()
 * @param tid  tid for this process
 * @param tgid  tgid for this process
 *
 * Drop all mapping information for the process.
 */
void opd_handle_exec(pid_t tid, pid_t tgid);

/**
 * opd_handle_exit - deal with exit notification
 * @param note  note to handle
 *
 * Deal with an exit() notification by setting the flag "dead"
 * on a process. These will be later cleaned up by the %SIGALRM
 * handler.
 *
 * sample->pid contains the process id of the exited process.
 */
void opd_handle_exit(struct op_note const * note);

/**
 * opd_get_proc - get process from process list
 * @param tid  tid for this process
 * @param tgid  tgid for this process
 *
 * A process with pid tid is searched on the process list,
 * maintaining LRU order. If it is not found, %NULL is returned,
 * otherwise the process structure is returned.
 */
struct opd_proc * opd_get_proc(pid_t tid, pid_t tgid);

/**
 * opd_new_proc - create a new process structure
 * @param tid  tid for this process
 * @param tgid  tgid for this process
 *
 * Allocate and initialise a process structure and insert
 * it into the procs hash table.
 */
struct opd_proc * opd_new_proc(pid_t tid, pid_t tgid);

/**
 * opd_get_nr_procs - return number of processes tracked
 */
int opd_get_nr_procs(void);

/**
 * opd_age_procs - age all dead process preparing them for a deletion
 */
void opd_age_procs(void);

/**
 * freeze all resource used by opd_procs managment
 */
void opd_proc_cleanup(void);

/**
 * opd_clear_kernel_mapping - remove all kernel mapping for all opd_proc
 *
 * invalidate (by removing them) all kernel mapping. This function do nothing
 * when separate_kernel == 0 because we don't add mapping for kernel
 * sample in proc struct. As side effect decrease reference count of
 * associated with these mapping which eventually close this image
 */
void opd_clear_kernel_mapping(void);

#endif /* OPD_PROC_H */
