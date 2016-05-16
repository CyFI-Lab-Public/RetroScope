/**
 * @file op_init.c
 * Initialisation stubs
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

// FIXME: any point in this file now we don't fiddle
// with -march options ??
 
#undef __NO_VERSION__
 
#include "oprofile.h"

EXPORT_NO_SYMBOLS;

int __init stub_init(void)
{
	sysctl.cpu_type = get_cpu_type();
	return oprof_init();
}

void __exit stub_exit(void)
{
	oprof_exit();
}

module_init(stub_init);
module_exit(stub_exit);
