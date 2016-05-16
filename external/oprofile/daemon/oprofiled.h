/**
 * @file daemon/oprofiled.h
 * Initialisation and setup
 *
 * @remark Copyright 2002, 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 * Modified by Aravind Menon for Xen
 * These modifications are:
 * Copyright (C) 2005 Hewlett-Packard Co.
 */

#ifndef OPROFILED_H

#include <signal.h>

struct oprofiled_ops {
	void (*init)(void);
	void (*start)(void);
	void (*exit)(void);
};


/**
 * opd_open_logfile - open the log file
 *
 * Open the logfile on stdout and stderr. This function
 * assumes that 1 and 2 are the lowest close()d file
 * descriptors. Failure to open on either descriptor is
 * a fatal error.
 */
void opd_open_logfile(void);

 
/**
 * is_image_ignored - check if we must ignore this image
 * @param name the name to check
 *
 * Return true if the image should not be profiled
 */
int is_image_ignored(char const * name);

/** return the int in the given oprofilefs file, error is fatal if !is_fatal */
int opd_read_fs_int(char const * path, char const * name, int is_fatal);


/** global variable positioned by signal handler */
extern sig_atomic_t signal_alarm;
extern sig_atomic_t signal_hup;
extern sig_atomic_t signal_term;
extern sig_atomic_t signal_child;
extern sig_atomic_t signal_usr1;
extern sig_atomic_t signal_usr2;

extern unsigned int op_nr_counters;
extern int separate_lib;
extern int separate_kernel;
extern int separate_thread;
extern int separate_cpu;
extern int no_vmlinux;
extern char * vmlinux;
extern char * kernel_range;
extern int no_xen;
extern char * xenimage;
extern char * xen_range;

#endif /* OPROFILED_H */
