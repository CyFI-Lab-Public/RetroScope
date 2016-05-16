/**
 * @file daemon/oprofiled.c
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

#include "config.h"
 
#include "oprofiled.h"
#include "opd_printf.h"
#include "opd_events.h"
#include "opd_extended.h"

#include "op_config.h"
#include "op_version.h"
#include "op_hw_config.h"
#include "op_libiberty.h"
#include "op_file.h"
#include "op_abi.h"
#include "op_string.h"
#include "op_cpu_type.h"
#include "op_popt.h"
#include "op_lockfile.h"
#include "op_list.h"
#include "op_fileio.h"

#include <sys/types.h>
#include <sys/resource.h>
#include <stdlib.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <assert.h>
#include <dirent.h>
#include <limits.h>

sig_atomic_t signal_alarm;
sig_atomic_t signal_hup;
sig_atomic_t signal_term;
sig_atomic_t signal_child;
sig_atomic_t signal_usr1;
sig_atomic_t signal_usr2;

uint op_nr_counters;
op_cpu cpu_type;
int no_event_ok;
int vsfile;
int vsamples;
int varcs;
int vmodule;
int vmisc;
int vext;
int separate_lib;
int separate_kernel;
int separate_thread;
int separate_cpu;
int no_vmlinux;
char * vmlinux;
char * kernel_range;
char * session_dir;
int no_xen;
char * xenimage;
char * xen_range;
static char * verbose;
static char * binary_name_filter;
static char * events;
static char * ext_feature;
static int showvers;
static struct oprofiled_ops * opd_ops;
extern struct oprofiled_ops opd_24_ops;
extern struct oprofiled_ops opd_26_ops;

#define OPD_IMAGE_FILTER_HASH_SIZE 32
static struct list_head images_filter[OPD_IMAGE_FILTER_HASH_SIZE];

static struct poptOption options[] = {
	{ "session-dir", 0, POPT_ARG_STRING, &session_dir, 0, "place sample database in dir instead of default location", "/var/lib/oprofile", },
	{ "kernel-range", 'r', POPT_ARG_STRING, &kernel_range, 0, "Kernel VMA range", "start-end", },
	{ "vmlinux", 'k', POPT_ARG_STRING, &vmlinux, 0, "vmlinux kernel image", "file", },
	{ "no-vmlinux", 0, POPT_ARG_NONE, &no_vmlinux, 0, "vmlinux kernel image file not available", NULL, },
	{ "xen-range", 0, POPT_ARG_STRING, &xen_range, 0, "Xen VMA range", "start-end", },
	{ "xen-image", 0, POPT_ARG_STRING, &xenimage, 0, "Xen image", "file", },
	{ "image", 0, POPT_ARG_STRING, &binary_name_filter, 0, "image name filter", "profile these comma separated image" },
	{ "separate-lib", 0, POPT_ARG_INT, &separate_lib, 0, "separate library samples for each distinct application", "[0|1]", },
	{ "separate-kernel", 0, POPT_ARG_INT, &separate_kernel, 0, "separate kernel samples for each distinct application", "[0|1]", },
	{ "separate-thread", 0, POPT_ARG_INT, &separate_thread, 0, "thread-profiling mode", "[0|1]" },
	{ "separate-cpu", 0, POPT_ARG_INT, &separate_cpu, 0, "separate samples for each CPU", "[0|1]" },
	{ "events", 'e', POPT_ARG_STRING, &events, 0, "events list", "[events]" },
	{ "version", 'v', POPT_ARG_NONE, &showvers, 0, "show version", NULL, },
	{ "verbose", 'V', POPT_ARG_STRING, &verbose, 0, "be verbose in log file", "all,sfile,arcs,samples,module,misc", },
	{ "ext-feature", 'x', POPT_ARG_STRING, &ext_feature, 1, "enable extended feature", "<extended-feature-name>:[args]", },
	POPT_AUTOHELP
	{ NULL, 0, 0, NULL, 0, NULL, NULL, },
};
 

void opd_open_logfile(void)
{
	if (open(op_log_file, O_WRONLY|O_CREAT|O_NOCTTY|O_APPEND, 0644) == -1) {
		perror("oprofiled: couldn't re-open stdout: ");
		exit(EXIT_FAILURE);
	}

	if (dup2(1, 2) == -1) {
		perror("oprofiled: couldn't dup stdout to stderr: ");
		exit(EXIT_FAILURE);
	}
}
 

/**
 * opd_fork - fork and return as child
 *
 * fork() and exit the parent with _exit().
 * Failure is fatal.
 */
static void opd_fork(void)
{
	switch (fork()) {
		case -1:
			perror("oprofiled: fork() failed: ");
			exit(EXIT_FAILURE);
			break;
		case 0:
			break;
		default:
			/* parent */
			_exit(EXIT_SUCCESS);
			break;
	}
}

 
static void opd_go_daemon(void)
{
	opd_fork();

	if (chdir(op_session_dir)) {
		fprintf(stderr, "oprofiled: opd_go_daemon: couldn't chdir to %s: %s",
			op_session_dir, strerror(errno));
		exit(EXIT_FAILURE);
	}

	if (setsid() < 0) {
		perror("oprofiled: opd_go_daemon: couldn't setsid: ");
		exit(EXIT_FAILURE);
	}

	opd_fork();
}


static void opd_write_abi(void)
{
	char * cbuf;
 
	cbuf = xmalloc(strlen(op_session_dir) + 5);
	strcpy(cbuf, op_session_dir);
	strcat(cbuf, "/abi");
	op_write_abi_to_file(cbuf);
	free(cbuf);
}


/**
 * opd_alarm - sync files and report stats
 */
static void opd_alarm(int val __attribute__((unused)))
{
	signal_alarm = 1;
}
 

/* re-open logfile for logrotate */
static void opd_sighup(int val __attribute__((unused)))
{
	signal_hup = 1;
}


static void opd_sigterm(int val __attribute__((unused)))
{
	signal_term = 1;
}

static void opd_sigchild(int val __attribute__((unused)))
{
	signal_child = 1;
}
 

static void opd_sigusr1(int val __attribute__((unused)))
{
	signal_usr1 = 1;
}

 
static void opd_sigusr2(int val __attribute__((unused)))
{
	signal_usr2 = 1;
}


static void opd_setup_signals(void)
{
	struct sigaction act;
 
	act.sa_handler = opd_alarm;
	act.sa_flags = 0;
	sigemptyset(&act.sa_mask);

	if (sigaction(SIGALRM, &act, NULL)) {
		perror("oprofiled: install of SIGALRM handler failed: ");
		exit(EXIT_FAILURE);
	}

	act.sa_handler = opd_sighup;
	act.sa_flags = 0;
	sigemptyset(&act.sa_mask);
	sigaddset(&act.sa_mask, SIGALRM);

	if (sigaction(SIGHUP, &act, NULL)) {
		perror("oprofiled: install of SIGHUP handler failed: ");
		exit(EXIT_FAILURE);
	}

	act.sa_handler = opd_sigterm;
	act.sa_flags = 0;
	sigemptyset(&act.sa_mask);
	sigaddset(&act.sa_mask, SIGTERM);

	if (sigaction(SIGTERM, &act, NULL)) {
		perror("oprofiled: install of SIGTERM handler failed: ");
		exit(EXIT_FAILURE);
	}

	act.sa_handler = opd_sigchild;
	act.sa_flags = 0;
	sigemptyset(&act.sa_mask);
	sigaddset(&act.sa_mask, SIGCHLD);

	if (sigaction(SIGCHLD, &act, NULL)) {
		perror("oprofiled: install of SIGCHLD handler failed: ");
		exit(EXIT_FAILURE);
	}

	act.sa_handler = opd_sigusr1;
	act.sa_flags = 0;
	sigemptyset(&act.sa_mask);
	sigaddset(&act.sa_mask, SIGTERM);

	if (sigaction(SIGUSR1, &act, NULL)) {
		perror("oprofiled: install of SIGUSR1 handler failed: ");
		exit(EXIT_FAILURE);
	}

	act.sa_handler = opd_sigusr2;
	act.sa_flags = 0;
	sigemptyset(&act.sa_mask);
	sigaddset(&act.sa_mask, SIGTERM);

	if (sigaction(SIGUSR2, &act, NULL)) {
		perror("oprofiled: install of SIGUSR2 handler failed: ");
		exit(EXIT_FAILURE);
	}
}


struct opd_hashed_name {
	char * name;
	struct list_head next;
};


static void add_image_filter(char const * name)
{
	size_t hash;
	struct opd_hashed_name * elt = xmalloc(sizeof(struct opd_hashed_name));
	elt->name = xmalloc(PATH_MAX);
	if (!realpath(name, elt->name)) {
		free(elt->name);
		free(elt);
		return;
	}
	hash = op_hash_string(elt->name);
	verbprintf(vmisc, "Adding to image filter: \"%s\"\n", elt->name);
	list_add(&elt->next, &images_filter[hash % OPD_IMAGE_FILTER_HASH_SIZE]);
}


static void opd_parse_image_filter(void)
{
	size_t i;
	char const * last = binary_name_filter;
	char const * cur = binary_name_filter;

	if (!binary_name_filter)
		return;

	for (i = 0; i < OPD_IMAGE_FILTER_HASH_SIZE; ++i)
		list_init(&images_filter[i]);

	while ((cur = strchr(last, ',')) != NULL) {
		char * tmp = op_xstrndup(last, cur - last);
		add_image_filter(tmp);
		free(tmp);
		last = cur + 1;
	}
	add_image_filter(last);
}


int is_image_ignored(char const * name)
{
	size_t hash;
	struct list_head * pos;

	if (!binary_name_filter)
		return 0;
	
	hash = op_hash_string(name);

	list_for_each(pos, &images_filter[hash % OPD_IMAGE_FILTER_HASH_SIZE]) {
		struct opd_hashed_name * hashed_name =
			list_entry(pos, struct opd_hashed_name, next);
		if (!strcmp(hashed_name->name, name))
			return 0;
	}

	return 1;
}


/** return the int in the given oprofilefs file */
int opd_read_fs_int(char const * path, char const * name, int fatal)
{
	char filename[PATH_MAX + 1];
	snprintf(filename, PATH_MAX, "%s/%s", path, name);
	return op_read_int_from_file(filename, fatal);
}


static void opd_handle_verbose_option(char const * name)
{
	if (!strcmp(name, "all")) {
		vsfile = 1;
		vsamples = 1;
		varcs = 1;
		vmodule = 1;
		vmisc = 1;
		vext= 1;
	} else if (!strcmp(name, "sfile")) {
		vsfile = 1;
	} else if (!strcmp(name, "arcs")) {
		varcs = 1;
	} else if (!strcmp(name, "samples")) {
		vsamples = 1;
	} else if (!strcmp(name, "module")) {
		vmodule = 1;
	} else if (!strcmp(name, "misc")) {
		vmisc = 1;
	} else if (!strcmp(name, "ext")) {
		vext= 1;
	} else {
		fprintf(stderr, "unknown verbose options\n");
		exit(EXIT_FAILURE);
	}
}

static void opd_parse_verbose(void)
{
	char const * last = verbose;
	char const * cur = verbose;

	if (!verbose)
		return;

	while ((cur = strchr(last, ',')) != NULL) {
		char * tmp = op_xstrndup(last, cur - last);
		opd_handle_verbose_option(tmp);
		free(tmp);
		last = cur + 1;
	}
	opd_handle_verbose_option(last);
}


static void opd_options(int argc, char const * argv[])
{
	poptContext optcon;
	char * tmp;

	optcon = op_poptGetContext(NULL, argc, argv, options, 0);

	if (showvers)
		show_version(argv[0]);

	opd_parse_verbose();

	if (separate_kernel)
		separate_lib = 1;

	cpu_type = op_get_cpu_type();
	op_nr_counters = op_get_nr_counters(cpu_type);

	if (!no_vmlinux) {
		if (!vmlinux || !strcmp("", vmlinux)) {
			fprintf(stderr, "oprofiled: no vmlinux specified.\n");
			poptPrintHelp(optcon, stderr, 0);
			exit(EXIT_FAILURE);
		}

		/* canonicalise vmlinux filename. fix #637805 */
		tmp = xmalloc(PATH_MAX);
		if (realpath(vmlinux, tmp))
			vmlinux = tmp;
		else
			free(tmp);

		if (!kernel_range || !strcmp("", kernel_range)) {
			fprintf(stderr, "oprofiled: no kernel VMA range specified.\n");
			poptPrintHelp(optcon, stderr, 0);
			exit(EXIT_FAILURE);
		}
	}

	if(opd_ext_initialize(ext_feature) != EXIT_SUCCESS)
		exit(EXIT_FAILURE);

	if (events == NULL && no_event_ok == 0) {
		fprintf(stderr, "oprofiled: no events specified.\n");
		poptPrintHelp(optcon, stderr, 0);
		exit(EXIT_FAILURE);
	}

	if (!xenimage || !strcmp("", xenimage)) {
		no_xen = 1;
	} else {
		no_xen = 0;

		/* canonicalise xen image filename. */
		tmp = xmalloc(PATH_MAX);
		if (realpath(xenimage, tmp))
			xenimage = tmp;
		else
			free(tmp);

		if (!xen_range || !strcmp("", xen_range)) {
			fprintf(stderr, "oprofiled: no Xen VMA range specified.\n");
			poptPrintHelp(optcon, stderr, 0);
			exit(EXIT_FAILURE);
		}
	}

	if (events != NULL)
		opd_parse_events(events);

	opd_parse_image_filter();

	poptFreeContext(optcon);
}


/* determine what kernel we're running and which daemon
 * to use
 */
static struct oprofiled_ops * get_ops(void)
{
	switch (op_get_interface()) {
#ifndef ANDROID
		case OP_INTERFACE_24:
			printf("Using 2.4 OProfile kernel interface.\n");
			return &opd_24_ops;
#endif
		case OP_INTERFACE_26:
			printf("Using 2.6+ OProfile kernel interface.\n");
			return &opd_26_ops;
		default:
			break;
	}

	fprintf(stderr, "Couldn't determine kernel version.\n");
	exit(EXIT_FAILURE);
	return NULL;
}


int main(int argc, char const * argv[])
{
	int err;
	struct rlimit rlim = { 2048, 2048 };

	opd_options(argc, argv);
	init_op_config_dirs(session_dir);

	opd_setup_signals();

	err = setrlimit(RLIMIT_NOFILE, &rlim);
	if (err)
		perror("warning: could not set RLIMIT_NOFILE to 2048: ");

	opd_write_abi();

	opd_ops = get_ops();

	opd_ops->init();

	opd_go_daemon();

	/* clean up every 10 minutes */
	alarm(60 * 10);

	if (op_write_lock_file(op_lock_file)) {
		fprintf(stderr, "oprofiled: could not create lock file %s\n",
			op_lock_file);
		exit(EXIT_FAILURE);
	}

	opd_ops->start();

	opd_ops->exit();

	return 0;
}
