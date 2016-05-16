/**
 * @file op_config.c
 * Oprofile configuration parameters.
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Nathan Tallent
 * @Modifications Daniel Hansel
 */

#include "op_config.h"
#include "op_config_24.h"

#include <limits.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

/* paths in op_config.h */
char op_session_dir[PATH_MAX];
char op_samples_dir[PATH_MAX];
char op_samples_current_dir[PATH_MAX];
char op_lock_file[PATH_MAX];
char op_log_file[PATH_MAX];
char op_pipe_file[PATH_MAX];
char op_dump_status[PATH_MAX];

/* paths in op_config_24.h */
char op_device[PATH_MAX];
char op_note_device[PATH_MAX];
char op_hash_device[PATH_MAX];

void
init_op_config_dirs(char const * session_dir)
{
	int session_dir_len;

	assert(session_dir);	
	session_dir_len = strlen(session_dir);

	if (session_dir_len + strlen("/samples/oprofiled.log") > PATH_MAX) {
		fprintf(stderr, "Session_dir string \"%s\" is too large.\n", 
			session_dir);
		exit(EXIT_FAILURE);
	}
	
	strcpy(op_session_dir, session_dir);
	
	strcpy(op_samples_dir, op_session_dir);
	strcat(op_samples_dir, "/samples/");
	
	strcpy(op_samples_current_dir, op_samples_dir);
	strcat(op_samples_current_dir, "/current/");

	strcpy(op_lock_file, op_session_dir);
	strcat(op_lock_file, "/lock");

	strcpy(op_pipe_file, op_session_dir);
	strcat(op_pipe_file, "/opd_pipe");

	strcpy(op_log_file, op_samples_dir);
	strcat(op_log_file, "oprofiled.log");

	strcpy(op_dump_status, op_session_dir);
	strcat(op_dump_status, "/complete_dump");

	strcpy(op_device, op_session_dir);
	strcat(op_device, "/opdev");

	strcpy(op_note_device, op_session_dir);
	strcat(op_note_device, "/opnotedev");

	strcpy(op_hash_device, op_session_dir);
	strcat(op_hash_device, "/ophashmapdev");
}
