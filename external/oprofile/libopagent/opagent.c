/**
 * @file opagent.c
 * Interface to report symbol names and dynamically generated code to Oprofile
 *
 * @remark Copyright 2007 OProfile authors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * @author Jens Wilke
 * @Modifications Daniel Hansel
 *
 * Copyright IBM Corporation 2007
 *
 */

/******************************************************************
 * ATTENTION: 
 *   When adding new functions to this interface, you MUST update
 *   opagent_symbols.ver.
 *
 *   If a change is made to an existing exported function, perform the
 *   the following steps.  As an example, assume op_open_agent()
 *   is being updated to include a 'dump_code' parameter. 
 *     1. Update the opagent.ver file with a new version node, and
 *        add the op_open_agent to it.  Note that op_open_agent
 *        is also still declared in the original version node.
 *     2. Add '__asm__(".symver <blah>") directives to this .c source file.
 *        For this example, the directives would be as follows:
 *            __asm__(".symver op_open_agent_1_0,op_open_agent@OPAGENT_1.0");
 *            __asm__(".symver op_open_agent_2_0,op_open_agent@@OPAGENT_2.0");
 *     3. Update the declaration of op_open_agent in the header file with
 *        the additional parameter.
 *     4. Change the name of the original op_open_agent to "op_open_agent_1_0"
 *        in this .c source file.
 *     5. Add the new op_open_agent_2_0(int dump_code) function in this
 *        .c source file.
 *            
 *   See libopagent/Makefile.am for more information.
 *******************************************************************/

#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <stdint.h>
#include <limits.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <time.h>
#include <bfd.h>

#include "opagent.h"
#include "op_config.h"
#include "jitdump.h"

// Declare BFD-related global variables.
static char * _bfd_target_name;
static int _bfd_arch;
static unsigned int _bfd_mach;

// Define BFD-related global variables.
static int define_bfd_vars(void)
{
	bfd * bfd;
	bfd_boolean r;
	int len;
#define MAX_PATHLENGTH 2048
	char mypath[MAX_PATHLENGTH];
     
	len = readlink("/proc/self/exe", mypath, sizeof(mypath));
     
	if (len < 0) {
		fprintf(stderr, "libopagent: readlink /proc/self/exe failed\n");
		return -1;
	}
	if (len >= MAX_PATHLENGTH) {
		fprintf(stderr, "libopagent: readlink /proc/self/exe returned"
			" path length longer than %d.\n", MAX_PATHLENGTH);

		return -1;
	}
	mypath[len] = '\0';

	bfd_init();
	bfd = bfd_openr(mypath, NULL);
	if (bfd == NULL) {
		bfd_perror("bfd_openr error. Cannot get required BFD info");
		return -1;
	}
	r = bfd_check_format(bfd, bfd_object);
	if (!r) {
		bfd_perror("bfd_get_arch error. Cannot get required BFD info");
		return -1;
	}
	_bfd_target_name =  bfd->xvec->name;
	_bfd_arch = bfd_get_arch(bfd);
	_bfd_mach = bfd_get_mach(bfd);

	return 0;
}
/**
 * Define the version of the opagent library.
 */
#define OP_MAJOR_VERSION 1
#define OP_MINOR_VERSION 0

#define AGENT_DIR OP_SESSION_DIR_DEFAULT "jitdump"

#define MSG_MAXLEN 20

op_agent_t op_open_agent(void)
{
	char pad_bytes[7] = {0, 0, 0, 0, 0, 0, 0};
	int pad_cnt;
	char dump_path[PATH_MAX];
	char err_msg[PATH_MAX + 16];
	struct stat dirstat;
	int rc;
	struct jitheader header;
	int fd;
	struct timeval tv;
	FILE * dumpfile = NULL;

	rc = stat(AGENT_DIR, &dirstat);
	if (rc || !S_ISDIR(dirstat.st_mode)) {
		if (!rc)
			errno = ENOTDIR;
		fprintf(stderr,"libopagent: Jitdump agent directory %s "
			"missing\n", AGENT_DIR);
		fprintf(stderr,"libopagent: do opcontrol --setup or "
			"opcontrol --reset, first\n");
		return NULL;
	}
	snprintf(dump_path, PATH_MAX, "%s/%i.dump", AGENT_DIR, getpid());
	snprintf(err_msg, PATH_MAX + 16, "Error opening %s\n", dump_path);
	// make the dump file only accessible for the user for security reason.
	fd = creat(dump_path, S_IRUSR|S_IWUSR);
	if (fd == -1) {
		fprintf(stderr, "%s\n", err_msg);
		return NULL;
	}
	dumpfile = fdopen(fd, "w");
	if (!dumpfile) {
		fprintf(stderr, "%s\n", err_msg);
		return NULL;
	}
	if (define_bfd_vars())
		return NULL;
	header.magic = JITHEADER_MAGIC;
	header.version = JITHEADER_VERSION;
	header.totalsize = sizeof(header) + strlen(_bfd_target_name) + 1;
	/* calculate amount of padding '\0' */
	pad_cnt = PADDING_8ALIGNED(header.totalsize);
	header.totalsize += pad_cnt;
	header.bfd_arch = _bfd_arch;
	header.bfd_mach = _bfd_mach;
	if (gettimeofday(&tv, NULL)) {
		fprintf(stderr, "gettimeofday failed\n");
		return NULL;
	}

	header.timestamp = tv.tv_sec;
	snprintf(err_msg, PATH_MAX + 16, "Error writing to %s", dump_path);
	if (!fwrite(&header, sizeof(header), 1, dumpfile)) {
		fprintf(stderr, "%s\n", err_msg);
		return NULL;
	}
	if (!fwrite(_bfd_target_name, strlen(_bfd_target_name) + 1, 1,
		    dumpfile)) {
		fprintf(stderr, "%s\n", err_msg);
		return NULL;
	}
	/* write padding '\0' if necessary */
	if (pad_cnt && !fwrite(pad_bytes, pad_cnt, 1, dumpfile)) {
		fprintf(stderr, "%s\n", err_msg);
		return NULL;
	}
	fflush(dumpfile);
	return (op_agent_t)dumpfile;
}


int op_close_agent(op_agent_t hdl)
{
	struct jr_code_close rec;
	struct timeval tv;
	FILE * dumpfile = (FILE *) hdl;
	if (!dumpfile) {
		errno = EINVAL;
		return -1;
	}
	rec.id = JIT_CODE_CLOSE;
	rec.total_size = sizeof(rec);
	if (gettimeofday(&tv, NULL)) {
		fprintf(stderr, "gettimeofday failed\n");
		return -1;
	}
	rec.timestamp = tv.tv_sec;

	if (!fwrite(&rec, sizeof(rec), 1, dumpfile))
		return -1;
	fclose(dumpfile);
	dumpfile = NULL;
	return 0;
}


int op_write_native_code(op_agent_t hdl, char const * symbol_name,
	uint64_t vma, void const * code, unsigned int const size)
{
	struct jr_code_load rec;
	struct timeval tv;
	size_t sz_symb_name;
	char pad_bytes[7] = { 0, 0, 0, 0, 0, 0, 0 };
	size_t padding_count;
	FILE * dumpfile = (FILE *) hdl;

	if (!dumpfile) {
		errno = EINVAL;
		fprintf(stderr, "Invalid hdl argument\n");
		return -1;
	}
	sz_symb_name = strlen(symbol_name) + 1;

	rec.id = JIT_CODE_LOAD;
	rec.code_size = size;
	rec.vma = vma;
	rec.code_addr = (u64) (uintptr_t) code;
	rec.total_size = code ? sizeof(rec) + sz_symb_name + size :
			sizeof(rec) + sz_symb_name;
	/* calculate amount of padding '\0' */
	padding_count = PADDING_8ALIGNED(rec.total_size);
	rec.total_size += padding_count;
	if (gettimeofday(&tv, NULL)) {
		fprintf(stderr, "gettimeofday failed\n");
		return -1;
	}

	rec.timestamp = tv.tv_sec;

	/* locking makes sure that we continuously write this record, if
	 * we are called within a multi-threaded context */
	flockfile(dumpfile);
	/* Write record, symbol name, code (optionally), and (if necessary)
	 * additonal padding \0 bytes.
	 */
	if (fwrite_unlocked(&rec, sizeof(rec), 1, dumpfile) &&
	    fwrite_unlocked(symbol_name, sz_symb_name, 1, dumpfile)) {
		if (code)
			fwrite_unlocked(code, size, 1, dumpfile);
		if (padding_count)
			fwrite_unlocked(pad_bytes, padding_count, 1, dumpfile);
		/* Always flush to ensure conversion code to elf will see
		 * data as soon as possible */
		fflush_unlocked(dumpfile);
		funlockfile(dumpfile);
		return 0;
	}
	fflush_unlocked(dumpfile);
	funlockfile(dumpfile);
	return -1;
}


int op_write_debug_line_info(op_agent_t hdl, void const * code,
			     size_t nr_entry,
			     struct debug_line_info const * compile_map)
{
	struct jr_code_debug_info rec;
	long cur_pos, last_pos;
	struct timeval tv;
	size_t i;
	size_t padding_count;
	char padd_bytes[7] = {0, 0, 0, 0, 0, 0, 0};
	int rc = -1;
	FILE * dumpfile = (FILE *) hdl;

	if (!dumpfile) {
		errno = EINVAL;
		fprintf(stderr, "Invalid hdl argument\n");
		return -1;
	}
	
	/* write nothing if no entries are provided */
	if (nr_entry == 0)
		return 0;

	rec.id = JIT_CODE_DEBUG_INFO;
	rec.code_addr = (uint64_t)(uintptr_t)code;
	/* will be fixed after writing debug line info */
	rec.total_size = 0;
	rec.nr_entry = nr_entry;
	if (gettimeofday(&tv, NULL)) {
		fprintf(stderr, "gettimeofday failed\n");
		return -1;
	}

	rec.timestamp = tv.tv_sec;

	flockfile(dumpfile);

	if ((cur_pos = ftell(dumpfile)) == -1l)
		goto error;
	if (!fwrite_unlocked(&rec, sizeof(rec), 1, dumpfile))
		goto error;
	for (i = 0; i < nr_entry; ++i) {
		if (!fwrite_unlocked(&compile_map[i].vma,
				     sizeof(compile_map[i].vma), 1,
				     dumpfile) ||
		    !fwrite_unlocked(&compile_map[i].lineno,
				     sizeof(compile_map[i].lineno), 1,
				     dumpfile) ||
		    !fwrite_unlocked(compile_map[i].filename,
				     strlen(compile_map[i].filename) + 1, 1,
				     dumpfile))
			goto error;
	}

	if ((last_pos = ftell(dumpfile)) == -1l)
		goto error;
	rec.total_size = last_pos - cur_pos;
	padding_count = PADDING_8ALIGNED(rec.total_size);
	rec.total_size += padding_count;
	if (padding_count && !fwrite(padd_bytes, padding_count, 1, dumpfile))
		goto error;
	if (fseek(dumpfile, cur_pos, SEEK_SET) == -1l)
		goto error;
	if (!fwrite_unlocked(&rec, sizeof(rec), 1, dumpfile))
		goto error;
	if (fseek(dumpfile, last_pos + padding_count, SEEK_SET) == -1)
		goto error;

	rc = 0;
error:
	fflush_unlocked(dumpfile);
	funlockfile(dumpfile);
	return rc;
}


int op_unload_native_code(op_agent_t hdl, uint64_t vma)
{
	struct jr_code_unload rec;
	struct timeval tv;
	FILE * dumpfile = (FILE *) hdl;

	if (!dumpfile) {
		errno = EINVAL;
		fprintf(stderr, "Invalid hdl argument\n");
		return -1;
	}

	rec.id = JIT_CODE_UNLOAD;
	rec.vma = vma;
	rec.total_size = sizeof(rec);
	if (gettimeofday(&tv, NULL)) {
		fprintf(stderr, "gettimeofday failed\n");
		return -1;
	}
	rec.timestamp = tv.tv_sec;

	if (!fwrite(&rec, sizeof(rec), 1, dumpfile))
		return -1;
	fflush(dumpfile);
	return 0;
}

int op_major_version(void)
{
	return OP_MAJOR_VERSION;
}

int op_minor_version(void)
{
	return OP_MINOR_VERSION;
}
