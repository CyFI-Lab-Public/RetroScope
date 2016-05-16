/**
 * @file op_cpufreq.h
 * get cpu frequency declaration
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OP_CPUFREQ_H
#define OP_CPUFREQ_H

#if defined(__cplusplus)
extern "C" {
#endif

/*
 * return the estimated cpu frequency in Mhz through
 * parsing /proc/cpuinfo, return 0 if this information
 * is not avalaible e.g. sparc64 with a non SMP kernel
 */
double op_cpu_frequency(void);

#if defined(__cplusplus)
}
#endif

#endif /* !OP_CPUFREQ_H */
