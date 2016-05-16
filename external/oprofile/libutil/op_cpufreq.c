/**
 * @file op_cpufreq.c
 * get cpu frequency definition
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <stdio.h>
#include <stdlib.h>

#include "op_fileio.h"

double op_cpu_frequency(void)
{
	double fval = 0.0;
	unsigned long uval;
	char * line = NULL;

	FILE * fp = op_try_open_file("/proc/cpuinfo", "r");
	if (!fp)
		return 0.0;

	while (1) {
		line = op_get_line(fp);

		if (!line)
			break;

		if (line[0] == '\0') {
			free(line);
			continue;
		}

		/* x86/parisc/ia64/x86_64 */
		if (sscanf(line, "cpu MHz : %lf", &fval) == 1)
			break;
		/* ppc/ppc64 */
		if (sscanf(line, "clock : %lfMHz", &fval) == 1)
			break;
		/* alpha */
		if (sscanf(line, "cycle frequency [Hz] : %lu", &uval) == 1) {
			fval = uval / 1E6;
			break;
		}
		/* sparc64 if CONFIG_SMP only */
		if (sscanf(line, "Cpu0ClkTck : %lx", &uval) == 1) {
			fval = uval / 1E6;
			break;
		}
		/* mips including loongson2 */
		if (sscanf(line, "BogoMIPS		: %lu", &uval) == 1) {
			fval = uval * 3 / 2;
			break;
		}
		/* s390 doesn't provide cpu freq, checked up to 2.6-test4 */

		free(line);
	}

	if (line)
		free(line);
	op_close_file(fp);

	return fval;
}
