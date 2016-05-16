/**
 * @file op_file.c
 * Useful file management helpers
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include <dirent.h>
#include <fnmatch.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <limits.h>

#include "op_file.h"
#include "op_libiberty.h"

int op_file_readable(char const * file)
{
	struct stat st;
	return !stat(file, &st) && S_ISREG(st.st_mode) && !access(file, R_OK);
}


time_t op_get_mtime(char const * file)
{
	struct stat st;

	if (stat(file, &st))
		return 0;

	return st.st_mtime;
}


int create_dir(char const * dir)
{
	if (mkdir(dir, 0755)) {
		/* FIXME: Does not verify existing is a dir */
		if (errno == EEXIST)
			return 0;
		return errno;
	}

	return 0;
}


int create_path(char const * path)
{
	int ret = 0;

	char * str = xstrdup(path);

	char * pos = str[0] == '/' ? str + 1 : str;

	for ( ; (pos = strchr(pos, '/')) != NULL; ++pos) {
		*pos = '\0';
		ret = create_dir(str);
		*pos = '/';
		if (ret)
			break;
	}

	free(str);
	return ret;
}


inline static int is_dot_or_dotdot(char const * name)
{
	return name[0] == '.' &&
		(name[1] == '\0' ||
		 (name[1] == '.' && name[2] == '\0'));
}


/* If non-null is returned, the caller is responsible for freeing
 * the memory allocated for the return value. */
static char * make_pathname_from_dirent(char const * basedir,
				      struct dirent * ent,
				      struct stat * st_buf)
{
	int name_len;
	char * name;
	name_len = strlen(basedir) + strlen("/") + strlen(ent->d_name) + 1;
	name = xmalloc(name_len);
	sprintf(name, "%s/%s", basedir,	ent->d_name);
	if (stat(name, st_buf) != 0)
	{
		struct stat lstat_buf;
		int err = errno;
		if (lstat(name, &lstat_buf) == 0 &&
			    S_ISLNK(lstat_buf.st_mode)) {
			// dangling symlink -- silently ignore
		} else {
			fprintf(stderr, "stat failed for %s (%s)\n",
			                name, strerror(err));
		}
		free(name);
		name = NULL;
	}
	return name;
}


int get_matching_pathnames(void * name_list, get_pathname_callback getpathname,
			   char const * base_dir, char const * filter,
			   enum recursion_type recursion)
{
/* The algorithm below depends on recursion type (of which there are 3)
 * and whether the current dirent matches the filter.  There are 6 possible
 * different behaviors, which is why we define 6 case below in the switch
 * statement of the algorithm.  Actually, when the recursion type is
 * MATCH_DIR_ONLY_RECURSION, the behavior is the same, whether or not the dir
 * entry matches the filter.  However, the behavior of the recursion types
 * NO_RECURSION and MATCH_ANY_ENTRY_RECURSION do depend on the dir entry
 * filter match, so for simplicity, we perform this match for all recursion
 * types and logically OR the match result with the  value of the passed
 * recursion_type.
 */
#define NO_MATCH 0
#define MATCH 1

	DIR * dir;
	struct dirent * ent;
	struct stat stat_buffer;
	int match;
	char * name = NULL;

	if (!(dir = opendir(base_dir)))
		return -1;
	while ((ent = readdir(dir)) != 0) {
		if (is_dot_or_dotdot(ent->d_name))
			continue;
		if (fnmatch(filter, ent->d_name, 0) == 0)
			match = 1;
		else
			match = 0;

		switch (recursion | match) {
		case NO_RECURSION + NO_MATCH:
		case MATCH_ANY_ENTRY_RECURSION + NO_MATCH:
			// nothing to do but continue the loop
			break;
		case NO_RECURSION + MATCH:
			getpathname(ent->d_name, name_list);
			break;
		case MATCH_ANY_ENTRY_RECURSION + MATCH:
			name = make_pathname_from_dirent(base_dir, ent,
						       &stat_buffer);
			if (name) {
				if (S_ISDIR(stat_buffer.st_mode)) {
					get_matching_pathnames(
						name_list, getpathname,
						name, filter, recursion);
				} else {
					getpathname(name, name_list);
				}
			}
			free(name);
			break;
		case MATCH_DIR_ONLY_RECURSION + NO_MATCH:
		case MATCH_DIR_ONLY_RECURSION + MATCH:
			name = make_pathname_from_dirent(base_dir, ent,
						       &stat_buffer);
			if (name && S_ISDIR(stat_buffer.st_mode)) {
				/* Check if full directory name contains
				 * match to the filter; if so, add it to
				 * name_list and quit; else, recurse.
				 */
				if (!fnmatch(filter, name, 0)) {
					getpathname(name, name_list);
				} else {
					get_matching_pathnames(
						name_list, getpathname,
						name, filter, recursion);
				}
			}
			free(name);
			break;
		}
	}
	closedir(dir);

	return 0;
}
