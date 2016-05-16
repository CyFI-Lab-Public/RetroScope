/**
 * @file opjitconv.c
 * Convert a jit dump file to an ELF file
 *
 * @remark Copyright 2007 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Jens Wilke
 * @Modifications Maynard Johnson
 * @Modifications Daniel Hansel
 * @Modifications Gisle Dankel
 *
 * Copyright IBM Corporation 2007
 *
 */

#include "opjitconv.h"
#include "opd_printf.h"
#include "op_file.h"
#include "op_libiberty.h"

#include <dirent.h>
#include <fnmatch.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <pwd.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <unistd.h>
#include <wait.h>

/*
 * list head.  The linked list is used during parsing (parse_all) to
 * hold all jitentry elements. After parsing, the program works on the
 * array structures (entries_symbols_ascending, entries_address_ascending)
 * and the linked list is not used any more.
 */
struct jitentry * jitentry_list = NULL;
struct jitentry_debug_line * jitentry_debug_line_list = NULL;

/* Global variable for asymbols so we can free the storage later. */
asymbol ** syms;

/* jit dump header information */
enum bfd_architecture dump_bfd_arch;
int dump_bfd_mach;
char const * dump_bfd_target_name;

/* user information for special user 'oprofile' */
struct passwd * pw_oprofile;

char sys_cmd_buffer[PATH_MAX + 1];

/* the bfd handle of the ELF file we write */
bfd * cur_bfd;

/* count of jitentries in the list */
u32 entry_count;
/* maximul space in the entry arrays, needed to add entries */
u32 max_entry_count;
/* array pointing to all jit entries, sorted by symbol names */
struct jitentry ** entries_symbols_ascending;
/* array pointing to all jit entries sorted by address */
struct jitentry ** entries_address_ascending;

/* debug flag, print some information */
int debug;

/*
 *  Front-end processing from this point to end of the source.
 *    From main(), the general flow is as follows:
 *      1. Find all anonymous samples directories
 *      2. Find all JIT dump files
 *      3. For each JIT dump file:
 *        3.1 Find matching anon samples dir (from list retrieved in step 1)
 *        3.2 mmap the JIT dump file
 *        3.3 Call op_jit_convert to create ELF file if necessary
 */

/* Callback function used for get_matching_pathnames() call to obtain
 * matching path names.
 */
static void get_pathname(char const * pathname, void * name_list)
{
	struct list_head * names = (struct list_head *) name_list;
	struct pathname * pn = xmalloc(sizeof(struct pathname));
	pn->name = xstrdup(pathname);
	list_add(&pn->neighbor, names);
}

static void delete_pathname(struct pathname * pname)
{
	free(pname->name);
	list_del(&pname->neighbor);
	free(pname);
}


static void delete_path_names_list(struct list_head * list)
{
	struct list_head * pos1, * pos2;
	list_for_each_safe(pos1, pos2, list) {
		struct pathname * pname = list_entry(pos1, struct pathname,
						     neighbor);
		delete_pathname(pname);
	}
}

static int mmap_jitdump(char const * dumpfile,
	struct op_jitdump_info * file_info)
{
	int rc = OP_JIT_CONV_OK;
	int dumpfd;

	dumpfd = open(dumpfile, O_RDONLY);
	if (dumpfd < 0) {
		if (errno == ENOENT)
			rc = OP_JIT_CONV_NO_DUMPFILE;
		else
			rc = OP_JIT_CONV_FAIL;
		goto out;
	}
	rc = fstat(dumpfd, &file_info->dmp_file_stat);
	if (rc < 0) {
		perror("opjitconv:fstat on dumpfile");
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}
	file_info->dmp_file = mmap(0, file_info->dmp_file_stat.st_size,
				   PROT_READ, MAP_PRIVATE, dumpfd, 0);
	if (file_info->dmp_file == MAP_FAILED) {
		perror("opjitconv:mmap\n");
		rc = OP_JIT_CONV_FAIL;
	}
out:
	return rc;
}

static char const * find_anon_dir_match(struct list_head * anon_dirs,
					char const * proc_id)
{
	struct list_head * pos;
	char match_filter[10];
	snprintf(match_filter, 10, "*/%s.*", proc_id);
	list_for_each(pos, anon_dirs) {
		struct pathname * anon_dir =
			list_entry(pos, struct pathname, neighbor);
		if (!fnmatch(match_filter, anon_dir->name, 0))
			return anon_dir->name;
	}
	return NULL;
}

int change_owner(char * path)
{
	int rc = OP_JIT_CONV_OK;
	int fd;
	
	fd = open(path, 0);
	if (fd < 0) {
		printf("opjitconv: File cannot be opened for changing ownership.\n");
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}
	if (fchown(fd, pw_oprofile->pw_uid, pw_oprofile->pw_gid) != 0) {
		printf("opjitconv: Changing ownership failed (%s).\n", strerror(errno));
		close(fd);
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}
	close(fd);

out:
	return rc;
}

/* Copies the given file to the temporary working directory and sets ownership
 * to 'oprofile:oprofile'.
 */
int copy_dumpfile(char const * dumpfile, char * tmp_dumpfile)
{
	int rc = OP_JIT_CONV_OK;

	sprintf(sys_cmd_buffer, "/bin/cp -p %s %s", dumpfile, tmp_dumpfile);

	if (system(sys_cmd_buffer) != 0) {
		printf("opjitconv: Calling system() to copy files failed.\n");
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}

	if (change_owner(tmp_dumpfile) != 0) {
		printf("opjitconv: Changing ownership of temporary dump file failed.\n");
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}
	
out:
	return rc;
}

/* Copies the created ELF file located in the temporary working directory to the
 * final destination (i.e. given ELF file name) and sets ownership to the
 * current user.
 */
int copy_elffile(char * elf_file, char * tmp_elffile)
{
	int rc = OP_JIT_CONV_OK;
	int fd;

	sprintf(sys_cmd_buffer, "/bin/cp -p %s %s", tmp_elffile, elf_file);
	if (system(sys_cmd_buffer) != 0) {
		printf("opjitconv: Calling system() to copy files failed.\n");
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}

	fd = open(elf_file, 0);
	if (fd < 0) {
		printf("opjitconv: File cannot be opened for changing ownership.\n");
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}
	if (fchown(fd, getuid(), getgid()) != 0) {
		printf("opjitconv: Changing ownership failed (%s).\n", strerror(errno));
		close(fd);
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}
	close(fd);
	
out:
	return rc;
}

/* Look for an anonymous samples directory that matches the process ID
 * given by the passed JIT dmp_pathname.  If none is found, it's an error
 * since by agreement, all JIT dump files should be removed every time
 * the user does --reset.  If we do find the matching samples directory,
 * we create an ELF file (<proc_id>.jo) and place it in that directory.
 */
static int process_jit_dumpfile(char const * dmp_pathname,
				struct list_head * anon_sample_dirs,
				unsigned long long start_time,
				unsigned long long end_time,
				char * tmp_conv_dir)
{
	int result_dir_length, proc_id_length;
	int rc = OP_JIT_CONV_OK;
	int jofd;
	struct stat file_stat;
	time_t dumpfile_modtime;
	struct op_jitdump_info dmp_info;
	char * elf_file = NULL;
	char * proc_id = NULL;
	char const * anon_dir;
	char const * dumpfilename = rindex(dmp_pathname, '/');
	/* temporary copy of dump file created for conversion step */
	char * tmp_dumpfile;
	/* temporary ELF file created during conversion step */
	char * tmp_elffile;
	
	verbprintf(debug, "Processing dumpfile %s\n", dmp_pathname);
	
	/* Check if the dump file is a symbolic link.
	 * We should not trust symbolic links because we only produce normal dump
	 * files (no links).
	 */
	if (lstat(dmp_pathname, &file_stat) == -1) {
		printf("opjitconv: lstat for dumpfile failed (%s).\n", strerror(errno));
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}
	if (S_ISLNK(file_stat.st_mode)) {
		printf("opjitconv: dumpfile path is corrupt (symbolic links not allowed).\n");
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}
	
	if (dumpfilename) {
		size_t tmp_conv_dir_length = strlen(tmp_conv_dir);
		char const * dot_dump = rindex(++dumpfilename, '.');
		if (!dot_dump)
			goto chk_proc_id;
		proc_id_length = dot_dump - dumpfilename;
		proc_id = xmalloc(proc_id_length + 1);
		memcpy(proc_id, dumpfilename, proc_id_length);
		proc_id[proc_id_length] = '\0';
		verbprintf(debug, "Found JIT dumpfile for process %s\n",
			   proc_id);

		tmp_dumpfile = xmalloc(tmp_conv_dir_length + 1 + strlen(dumpfilename) + 1);
		strncpy(tmp_dumpfile, tmp_conv_dir, tmp_conv_dir_length);
		tmp_dumpfile[tmp_conv_dir_length] = '\0';
		strcat(tmp_dumpfile, "/");
		strcat(tmp_dumpfile, dumpfilename);
	}
chk_proc_id:
	if (!proc_id) {
		printf("opjitconv: dumpfile path is corrupt.\n");
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}
	if (!(anon_dir = find_anon_dir_match(anon_sample_dirs, proc_id))) {
		printf("Possible error: No matching anon samples for %s\n",
		       dmp_pathname);
		rc = OP_JIT_CONV_NO_MATCHING_ANON_SAMPLES;
		goto free_res1;
	}
	
	if (copy_dumpfile(dmp_pathname, tmp_dumpfile) != OP_JIT_CONV_OK)
		goto free_res1;
	
	if ((rc = mmap_jitdump(tmp_dumpfile, &dmp_info)) == OP_JIT_CONV_OK) {
		char * anon_path_seg = rindex(anon_dir, '/');
		if (!anon_path_seg) {
			printf("opjitconv: Bad path for anon sample: %s\n",
			       anon_dir);
			rc = OP_JIT_CONV_FAIL;
			goto free_res2;
		}
		result_dir_length = ++anon_path_seg - anon_dir;
		/* create final ELF file name */
		elf_file = xmalloc(result_dir_length +
				   strlen(proc_id) + strlen(".jo") + 1);
		strncpy(elf_file, anon_dir, result_dir_length);
		elf_file[result_dir_length] = '\0';
		strcat(elf_file, proc_id);
		strcat(elf_file, ".jo");
		/* create temporary ELF file name */
		tmp_elffile = xmalloc(strlen(tmp_conv_dir) + 1 +
				   strlen(proc_id) + strlen(".jo") + 1);
		strncpy(tmp_elffile, tmp_conv_dir, strlen(tmp_conv_dir));
		tmp_elffile[strlen(tmp_conv_dir)] = '\0';
		strcat(tmp_elffile, "/");
		strcat(tmp_elffile, proc_id);
		strcat(tmp_elffile, ".jo");

		// Check if final ELF file exists already
		jofd = open(elf_file, O_RDONLY);
		if (jofd < 0)
			goto create_elf;
		rc = fstat(jofd, &file_stat);
		if (rc < 0) {
			perror("opjitconv:fstat on .jo file");
			rc = OP_JIT_CONV_FAIL;
			goto free_res3;
		}
		if (dmp_info.dmp_file_stat.st_mtime >
		    dmp_info.dmp_file_stat.st_ctime)
			dumpfile_modtime = dmp_info.dmp_file_stat.st_mtime;
		else
			dumpfile_modtime = dmp_info.dmp_file_stat.st_ctime;

		/* Final ELF file already exists, so if dumpfile has not been
		 * modified since the ELF file's mod time, we don't need to
		 * do ELF creation again.
		 */
		if (!(file_stat.st_ctime < dumpfile_modtime ||
		    file_stat.st_mtime < dumpfile_modtime)) {
			rc = OP_JIT_CONV_ALREADY_DONE;
			goto free_res3; 
		}

	create_elf:
		verbprintf(debug, "Converting %s to %s\n", dmp_pathname,
			   elf_file);
		/* Set eGID of the special user 'oprofile'. */
		if (setegid(pw_oprofile->pw_gid) != 0) {
			perror("opjitconv: setegid to special user failed");
			rc = OP_JIT_CONV_FAIL;
			goto free_res3;
		}
		/* Set eUID of the special user 'oprofile'. */
		if (seteuid(pw_oprofile->pw_uid) != 0) {
			perror("opjitconv: seteuid to special user failed");
			rc = OP_JIT_CONV_FAIL;
			goto free_res3;
		}
		/* Convert the dump file as the special user 'oprofile'. */
		rc = op_jit_convert(dmp_info, tmp_elffile, start_time, end_time);
		/* Set eUID back to the original user. */
		if (seteuid(getuid()) != 0) {
			perror("opjitconv: seteuid to original user failed");
			rc = OP_JIT_CONV_FAIL;
			goto free_res3;
		}
		/* Set eGID back to the original user. */
		if (setegid(getgid()) != 0) {
			perror("opjitconv: setegid to original user failed");
			rc = OP_JIT_CONV_FAIL;
			goto free_res3;
		}
		rc = copy_elffile(elf_file, tmp_elffile);
	free_res3:
		free(elf_file);
		free(tmp_elffile);
	free_res2:
		munmap(dmp_info.dmp_file, dmp_info.dmp_file_stat.st_size);
	}
free_res1:
	free(proc_id);
	free(tmp_dumpfile);
out:
	return rc;
}

/* If non-NULL value is returned, caller is responsible for freeing memory.*/
static char * get_procid_from_dirname(char * dirname)
{
	char * ret = NULL;
	if (dirname) {
		char * proc_id;
		int proc_id_length;
		char * fname = rindex(dirname, '/');
		char const * dot = index(++fname, '.');
		if (!dot)
			goto out;
		proc_id_length = dot - fname;
		proc_id = xmalloc(proc_id_length + 1);
		memcpy(proc_id, fname, proc_id_length);
		proc_id[proc_id_length] = '\0';
		ret = proc_id;
	}
out:
	return ret;
}
static void filter_anon_samples_list(struct list_head * anon_dirs)
{
	struct procid {
		struct procid * next;
		char * pid;
	};
	struct procid * pid_list = NULL;
	struct procid * id, * nxt;
	struct list_head * pos1, * pos2;
	list_for_each_safe(pos1, pos2, anon_dirs) {
		struct pathname * pname = list_entry(pos1, struct pathname,
						     neighbor);
		char * proc_id = get_procid_from_dirname(pname->name);
		if (proc_id) {
			int found = 0;
			for (id = pid_list; id != NULL; id = id->next) {
				if (!strcmp(id->pid, proc_id)) {
					/* Already have an entry for this 
					 * process ID, so delete this entry
					 * from anon_dirs.
					 */
					free(pname->name);
					list_del(&pname->neighbor);
					free(pname);
					found = 1;
				}
			}
			if (!found) {
				struct procid * this_proc = 
					xmalloc(sizeof(struct procid));
				this_proc->pid = proc_id;
				this_proc->next = pid_list;
				pid_list = this_proc;
			}
		} else {
			printf("Unexpected result in processing anon sample"
			       " directory\n");
		}
	}
	for (id = pid_list; id; id = nxt) {
		free(id->pid);
		nxt = id->next;
		free(id);
	}
}


static int op_process_jit_dumpfiles(char const * session_dir,
	unsigned long long start_time, unsigned long long end_time)
{
	struct list_head * pos1, * pos2;
	int rc = OP_JIT_CONV_OK;
	char jitdumpfile[PATH_MAX + 1];
	char oprofile_tmp_template[] = "/tmp/oprofile.XXXXXX";
	char const * jitdump_dir = "/var/lib/oprofile/jitdump/";
	LIST_HEAD(jd_fnames);
	char const * anon_dir_filter = "*/{dep}/{anon:anon}/[0-9]*.*";
	LIST_HEAD(anon_dnames);
	char const * samples_subdir = "/samples/current";
	int samples_dir_len = strlen(session_dir) + strlen(samples_subdir);
	char * samples_dir;
	/* temporary working directory for dump file conversion step */
	char * tmp_conv_dir;

	/* Create a temporary working directory used for the conversion step.
	 */
	tmp_conv_dir = mkdtemp(oprofile_tmp_template);
	if (tmp_conv_dir == NULL) {
		printf("opjitconv: Temporary working directory cannot be created.\n");
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}

	if ((rc = get_matching_pathnames(&jd_fnames, get_pathname,
		jitdump_dir, "*.dump", NO_RECURSION)) < 0
			|| list_empty(&jd_fnames))
		goto rm_tmp;

	/* Get user information (i.e. UID and GID) for special user 'oprofile'.
	 */
	pw_oprofile = getpwnam("oprofile");
	if (pw_oprofile == NULL) {
		printf("opjitconv: User information for special user oprofile cannot be found.\n");
		rc = OP_JIT_CONV_FAIL;
		goto rm_tmp;
	}

	/* Change ownership of the temporary working directory to prevent other users
	 * to attack conversion process.
	 */
	if (change_owner(tmp_conv_dir) != 0) {
		printf("opjitconv: Changing ownership of temporary directory failed.\n");
		rc = OP_JIT_CONV_FAIL;
		goto rm_tmp;
	}
	
	samples_dir = xmalloc(samples_dir_len + 1);
	sprintf(samples_dir, "%s%s", session_dir, samples_subdir);
	if (get_matching_pathnames(&anon_dnames, get_pathname,
				    samples_dir, anon_dir_filter,
				    MATCH_DIR_ONLY_RECURSION) < 0
	    || list_empty(&anon_dnames)) {
		rc = OP_JIT_CONV_NO_ANON_SAMPLES;
		goto rm_tmp;
	}
	/* When using get_matching_pathnames to find anon samples,
	 * the list that's returned may contain multiple entries for
	 * one or more processes; e.g.,
	 *    6868.0x100000.0x103000
	 *    6868.0xdfe77000.0xdec40000
	 *    7012.0x100000.0x103000
	 *    7012.0xdfe77000.0xdec40000
	 *
	 * So we must filter the list so there's only one entry per
	 * process.
	 */
	filter_anon_samples_list(&anon_dnames);

	/* get_matching_pathnames returns only filename segment when
	 * NO_RECURSION is passed, so below, we add back the JIT
	 * dump directory path to the name.
	 */
	list_for_each_safe(pos1, pos2, &jd_fnames) {
		struct pathname * dmpfile =
			list_entry(pos1, struct pathname, neighbor);
		strncpy(jitdumpfile, jitdump_dir, PATH_MAX);
		strncat(jitdumpfile, dmpfile->name, PATH_MAX);
		rc = process_jit_dumpfile(jitdumpfile, &anon_dnames,
					  start_time, end_time, tmp_conv_dir);
		if (rc == OP_JIT_CONV_FAIL) {
			verbprintf(debug, "JIT convert error %d\n", rc);
			goto rm_tmp;
		}
		delete_pathname(dmpfile);
	}
	delete_path_names_list(&anon_dnames);
	
rm_tmp:
	/* Delete temporary working directory with all its files
	 * (i.e. dump and ELF file).
	 */
	sprintf(sys_cmd_buffer, "/bin/rm -rf %s", tmp_conv_dir);
	if (system(sys_cmd_buffer) != 0) {
		printf("opjitconv: Removing temporary working directory failed.\n");
		rc = OP_JIT_CONV_TMPDIR_NOT_REMOVED;
	}
	
out:
	return rc;
}

int main(int argc, char ** argv)
{
	unsigned long long start_time, end_time;
	char const * session_dir;
	int rc = 0;

	debug = 0;
	if (argc > 1 && strcmp(argv[1], "-d") == 0) {
		debug = 1;
		argc--;
		argv++;
	}

	if (argc != 4) {
		printf("Usage: opjitconv [-d] <session_dir> <starttime>"
		       " <endtime>\n");
		fflush(stdout);
		rc = EXIT_FAILURE;
		goto out;
	}

	session_dir = argv[1];
	/*
	 * Check for a maximum of 4096 bytes (Linux path name length limit) decremented
	 * by 16 bytes (will be used later for appending samples sub directory).
	 * Integer overflows according to the session dir parameter (user controlled)
	 * are not possible anymore.
	 */
	if (strlen(session_dir) > PATH_MAX - 16) {
		printf("opjitconv: Path name length limit exceeded for session directory: %s\n", session_dir);
		rc = EXIT_FAILURE;
		goto out;
	}

	start_time = atol(argv[2]);
	end_time = atol(argv[3]);

	if (start_time > end_time) {
		rc = EXIT_FAILURE;
		goto out;
	}
	verbprintf(debug, "start time/end time is %llu/%llu\n",
		   start_time, end_time);
	rc = op_process_jit_dumpfiles(session_dir, start_time, end_time);
	if (rc > OP_JIT_CONV_OK) {
		verbprintf(debug, "opjitconv: Ending with rc = %d. This code"
			   " is usually OK, but can be useful for debugging"
			   " purposes.\n", rc);
		rc = OP_JIT_CONV_OK;
	}
	fflush(stdout);
	if (rc == OP_JIT_CONV_OK)
		rc = EXIT_SUCCESS;
	else
		rc = EXIT_FAILURE;
out:
	_exit(rc);
}
