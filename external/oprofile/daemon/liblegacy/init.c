/**
 * @file daemon/liblegacy/init.c
 * Daemon set up and main loop for 2.4
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include "config.h"
 
#include "opd_proc.h"
#include "opd_mapping.h"
#include "opd_24_stats.h"
#include "opd_sample_files.h"
#include "opd_image.h"
#include "opd_parse_proc.h"
#include "opd_kernel.h"
#include "opd_printf.h"
#include "oprofiled.h"

#include "op_sample_file.h"
#include "op_config_24.h"
#include "op_interface.h"
#include "op_libiberty.h"
#include "op_deviceio.h"
#include "op_events.h"
#include "op_get_time.h"
#include "op_fileio.h"

#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>

fd_t hashmapdevfd;

int cpu_number;

static fd_t devfd;
static fd_t notedevfd;
static struct op_buffer_head * sbuf;
static size_t s_buf_bytesize;
static struct op_note * nbuf;
static size_t n_buf_bytesize;

static void opd_sighup(void);
static void opd_alarm(void);
static void opd_sigterm(void);


/**
 * op_open_files - open necessary files
 *
 * Open the device files and the log file,
 * and mmap() the hash map.
 */
static void op_open_files(void)
{
	hashmapdevfd = op_open_device(op_hash_device);
	if (hashmapdevfd == -1) {
		perror("Failed to open hash map device");
		exit(EXIT_FAILURE);
	}

	notedevfd = op_open_device(op_note_device);
	if (notedevfd == -1) {
		if (errno == EINVAL)
			fprintf(stderr, "Failed to open note device. Possibly you have passed incorrect\n"
				"parameters. Check /var/log/messages.");
		else
			perror("Failed to open note device");
		exit(EXIT_FAILURE);
	}

	devfd = op_open_device(op_device);
	if (devfd == -1) {
		if (errno == EINVAL)
			fprintf(stderr, "Failed to open device. Possibly you have passed incorrect\n"
				"parameters. Check /var/log/messages.");
		else
			perror("Failed to open profile device");
		exit(EXIT_FAILURE);
	}

	opd_init_hash_map();

	/* give output before re-opening stdout as the logfile */
	printf("Using log file %s\n", op_log_file);

	/* set up logfile */
	close(0);
	close(1);

	if (open("/dev/null", O_RDONLY) == -1) {
		perror("oprofiled: couldn't re-open stdin as /dev/null: ");
		exit(EXIT_FAILURE);
	}

	opd_open_logfile();

	printf("oprofiled started %s", op_get_time());
	fflush(stdout);
}
 

static void opd_do_samples(struct op_buffer_head const * buf);
static void opd_do_notes(struct op_note const * opd_buf, size_t count);

/**
 * do_shutdown - shutdown cleanly, reading as much remaining data as possible.
 * @param buf  sample buffer area
 * @param size  size of sample buffer
 * @param nbuf  note buffer area
 * @param nsize  size of note buffer
 */
static void opd_shutdown(struct op_buffer_head * buf, size_t size, struct op_note * nbuf, size_t nsize)
{
	ssize_t count = -1;
	ssize_t ncount = -1;

	/* the dump may have added no samples, so we must set
	 * non-blocking */
	if (fcntl(devfd, F_SETFL, fcntl(devfd, F_GETFL) | O_NONBLOCK) < 0) {
		perror("Failed to set non-blocking read for device: ");
		exit(EXIT_FAILURE);
	}

	/* it's always OK to read the note device */
	while (ncount < 0)
		ncount = op_read_device(notedevfd, nbuf, nsize);

	if (ncount > 0)
		opd_do_notes(nbuf, ncount);

	/* read as much as we can until we have exhausted the data
	 * (EAGAIN is returned).
	 *
	 * This will not livelock as the profiler has been partially
	 * shut down by now.
	 */
	while (1) {
		count = op_read_device(devfd, buf, size);
		if (count < 0 && errno == EAGAIN)
			break;
		verbprintf(vmisc, "Shutting down, state %d\n", buf->state);
		opd_do_samples(buf);
	}
}
 

/**
 * opd_do_read - enter processing loop
 * @param buf  buffer to read into
 * @param size  size of buffer
 * @param nbuf  note buffer
 * @param nsize  size of note buffer
 *
 * Read some of a buffer from the device and process
 * the contents.
 */
static void opd_do_read(struct op_buffer_head * buf, size_t size, struct op_note * nbuf, size_t nsize)
{
	while (1) {
		ssize_t count = -1;
		ssize_t ncount = -1;

		/* loop to handle EINTR */
		while (count < 0)
			count = op_read_device(devfd, buf, size);

		while (ncount < 0)
			ncount = op_read_device(notedevfd, nbuf, nsize);

		opd_do_notes(nbuf, ncount);
		opd_do_samples(buf);

		// we can lost a signal alarm or a signal hup but we don't
		// take care.
		if (signal_alarm) {
			signal_alarm = 0;
			opd_alarm();
		}

		if (signal_hup) {
			signal_hup = 0;
			opd_sighup();
		}

		if (signal_term)
			opd_sigterm();
 
		/* request to stop arrived */
		if (buf->state == STOPPING) {
			verbprintf(vmisc, "Shutting down by request.\n");
			opd_shutdown(buf, size, nbuf, nsize);
			return;
		}
	}
}

/**
 * opd_do_notes - process a notes buffer
 * @param opd_buf  buffer to process
 * @param count  number of bytes in buffer
 *
 * Process a buffer of notes.
 */
static void opd_do_notes(struct op_note const * opd_buf, size_t count)
{
	uint i;
	struct op_note const * note;

	for (i = 0; i < count/sizeof(struct op_note); i++) {
		note = &opd_buf[i];

		opd_24_stats[OPD_NOTIFICATIONS]++;

		switch (note->type) {
			case OP_MAP:
			case OP_EXEC:
				if (note->type == OP_EXEC)
					opd_handle_exec(note->pid, note->tgid);
				opd_handle_mapping(note);
				break;

			case OP_FORK:
				opd_handle_fork(note);
				break;

			case OP_DROP_MODULES:
				opd_clear_module_info();
				break;

			case OP_EXIT:
				opd_handle_exit(note);
				break;

			default:
				fprintf(stderr, "Received unknown notification type %u\n", note->type);
				abort();
				break;
		}
	}
}

/**
 * opd_do_samples - process a sample buffer
 * @param opd_buf  buffer to process
 *
 * Process a buffer of samples.
 * The signals specified by the global variable maskset are
 * masked.
 *
 * If the sample could be processed correctly, it is written
 * to the relevant sample file. Additionally mapping and
 * process notifications are handled here.
 */
static void opd_do_samples(struct op_buffer_head const * opd_buf)
{
	uint i;
	struct op_sample const * buffer = opd_buf->buffer; 

	opd_24_stats[OPD_DUMP_COUNT]++;

	verbprintf(vmisc, "Read buffer of %d entries for cpu %d.\n",
		   (unsigned int)opd_buf->count, opd_buf->cpu_nr);
 
	if (separate_cpu)
		cpu_number = opd_buf->cpu_nr;
	for (i = 0; i < opd_buf->count; i++) {
		verbprintf(vsamples, "%.6u: EIP: 0x%.8lx pid: %.6d\n",
			i, buffer[i].eip, buffer[i].pid);
		opd_put_sample(&buffer[i]);
	}
}


/**
 * opd_alarm - clean up old procs, msync, and report stats
 */
static void opd_alarm(void)
{
	opd_sync_samples_files();

	opd_age_procs();

	opd_print_24_stats();

	alarm(60 * 10);
}
 

/* re-open logfile for logrotate */
static void opd_sighup(void)
{
	printf("Received SIGHUP.\n");
	close(1);
	close(2);
	opd_open_logfile();
	/* We just close them, and re-open them lazily as usual. */
	opd_for_each_image(opd_close_image_samples_files);
}


static void clean_exit(void)
{
	opd_cleanup_hash_name();
	op_free_events();
	unlink(op_lock_file);
}


static void opd_sigterm(void)
{
	opd_print_24_stats();
	printf("oprofiled stopped %s", op_get_time());
	exit(EXIT_FAILURE);
}
 

static void opd_24_init(void)
{
	size_t i;
	int opd_buf_size = OP_DEFAULT_BUF_SIZE;
	int opd_note_buf_size = OP_DEFAULT_NOTE_SIZE;

	if (!no_vmlinux)
		opd_parse_kernel_range(kernel_range);
	opd_buf_size = opd_read_fs_int(OP_MOUNT, "bufsize", 1);
	opd_note_buf_size = opd_read_fs_int(OP_MOUNT, "notesize", 1);

	s_buf_bytesize = sizeof(struct op_buffer_head) + opd_buf_size * sizeof(struct op_sample);

	sbuf = xmalloc(s_buf_bytesize);

	n_buf_bytesize = opd_note_buf_size * sizeof(struct op_note);
	nbuf = xmalloc(n_buf_bytesize);

	opd_init_images();
	opd_init_procs();
	opd_init_kernel_image();

	for (i = 0; i < OPD_MAX_STATS; i++)
		opd_24_stats[i] = 0;

	if (atexit(clean_exit)) {
		perror("oprofiled: couldn't set exit cleanup: ");
		exit(EXIT_FAILURE);
	}
}


static void opd_24_start(void)
{
	op_open_files();

	/* yes, this is racey. */
	opd_get_ascii_procs();

	/* simple sleep-then-process loop */
	opd_do_read(sbuf, s_buf_bytesize, nbuf, n_buf_bytesize);
}


static void opd_24_exit(void)
{
	opd_print_24_stats();
	printf("oprofiled stopped %s", op_get_time());

	free(sbuf);
	free(nbuf);
	opd_clear_module_info();
	opd_proc_cleanup();
	/* kernel/module image are not owned by a proc, we must cleanup them */
	opd_for_each_image(opd_delete_image);
}


struct oprofiled_ops opd_24_ops = {
	.init = opd_24_init,
	.start = opd_24_start,
	.exit = opd_24_exit
};
