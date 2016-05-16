/**
 * @file daemon/liblegacy/opd_kernel.h
 * Dealing with the kernel and kernel module samples
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OPD_KERNEL_H
#define OPD_KERNEL_H

#include "op_types.h"

struct opd_proc;

/**
 * opd_init_kernel_image - initialise the kernel image
 */
void opd_init_kernel_image(void);

/**
 * opd_parse_kernel_range - parse the kernel range values
 */
void opd_parse_kernel_range(char const * arg);

/**
 * opd_clear_module_info - clear kernel module information
 *
 * Clear and free all kernel module information and reset
 * values.
 */
void opd_clear_module_info(void);

/**
 * opd_handle_kernel_sample - process a kernel sample
 * @param eip  EIP value of sample
 * @param counter  counter number
 *
 * Handle a sample in kernel address space or in a module. The sample is
 * output to the relevant image file.
 */
void opd_handle_kernel_sample(unsigned long eip, u32 counter);

/**
 * opd_eip_is_kernel - is the sample from kernel/module space
 * @param eip  EIP value
 *
 * Returns %1 if eip is in the address space starting at
 * kernel_start, %0 otherwise.
 */
int opd_eip_is_kernel(unsigned long eip);

/**
 * opd_add_kernel_map - add a module or kernel maps to a proc struct
 *
 * @param proc owning proc of the new mapping
 * @param eip eip inside the new mapping
 *
 * We assume than eip >= kernel_start
 *
 */
void opd_add_kernel_map(struct opd_proc * proc, unsigned long eip);

#endif /* OPD_KERNEL_H */
