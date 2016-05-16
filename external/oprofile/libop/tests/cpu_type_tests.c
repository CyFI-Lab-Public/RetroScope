/**
 * @file cpu_type_tests.c
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "op_cpu_type.h"

static struct cpu_type {
	char const * name;
	op_cpu type;
} cpu_str[] = {
	{ "i386/ppro", CPU_PPRO },
	{ "i386/pii", CPU_PII },
	{ "i386/piii", CPU_PIII },
	{ "i386/athlon", CPU_ATHLON },
	{ "timer", CPU_TIMER_INT },
	{ "rtc", CPU_RTC },
	{ "i386/p4", CPU_P4 },
	{ "ia64/ia64", CPU_IA64 },
	{ "ia64/itanium", CPU_IA64_1 },
	{ "ia64/itanium2", CPU_IA64_2 },
	{ "x86-64/hammer", CPU_HAMMER },
	{ "i386/p4-ht", CPU_P4_HT2 },
	{ "alpha/ev4", CPU_AXP_EV4 },
	{ "alpha/ev5", CPU_AXP_EV5 },
	{ "alpha/pca56", CPU_AXP_PCA56 },
	{ "alpha/ev6", CPU_AXP_EV6 },
	{ "alpha/ev67", CPU_AXP_EV67 },
	{ "foo", CPU_NO_GOOD },
	{ "-3", CPU_NO_GOOD },
	{ "2927", CPU_NO_GOOD },
	{ "", CPU_NO_GOOD },
	{ NULL, CPU_NO_GOOD }
};


static void test(struct cpu_type const * cpu)
{
	char const * name;
	op_cpu type;

	name = op_get_cpu_name(cpu->type);
	if (cpu->type != CPU_NO_GOOD && strcmp(cpu->name, name)) {
		printf("for %d expect %s found %s\n", cpu->type, cpu->name,
		       name);
		exit(EXIT_FAILURE);
	}
	if (cpu->type == CPU_NO_GOOD && strcmp("invalid cpu type", name)) {
		printf("for %d expect %s found %s\n", cpu->type,
		       "invalid cpu type", name);
		exit(EXIT_FAILURE);
	}

	type = op_get_cpu_number(cpu->name);
	if (type != cpu->type) {
		printf("for %s expect %d found %d\n", cpu->name, cpu->type,
		       type);
		exit(EXIT_FAILURE);
	}
}


int main(void)
{
	struct cpu_type * cpu;
	for (cpu = cpu_str; cpu->name; ++cpu)
		test(cpu);
	return 0;
}
