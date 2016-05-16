/**
 * @file load_events_files_tests.c
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <stdlib.h>
#include <stdio.h>

#include "op_events.h"
#include "op_cpu_type.h"

int main(void)
{
	op_cpu cpu_type;

	setenv("OPROFILE_EVENTS_DIR", OPROFILE_SRCDIR "/events", 1);

	for (cpu_type = CPU_NO_GOOD + 1; cpu_type < MAX_CPU_TYPE; ++cpu_type) {
		if (cpu_type != CPU_TIMER_INT) {
			op_events(cpu_type);
			op_free_events();
		}
	}

	return 0;
}
