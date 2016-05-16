/*
 * filecap.c - A program that lists running processes with capabilities
 * Copyright (c) 2009-10 Red Hat Inc., Durham, North Carolina.
 * All Rights Reserved.
 *
 * This software may be freely redistributed and/or modified under the
 * terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING. If not, write to the
 * Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * Authors:
 *   Steve Grubb <sgrubb@redhat.com>
 */

#include "config.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include "cap-ng.h"
#define __USE_GNU 1
#include <fcntl.h>
#define __USE_XOPEN_EXTENDED 1
#include <ftw.h>

static int show_all = 0, header = 0, capabilities = 0, cremove = 0;

static void usage(void)
{
	fprintf(stderr,
	    "usage: filecap [-a | -d | /dir | /dir/file [cap1 cap2 ...] ]\n");
	exit(1);
}

static int check_file(const char *fpath,
		const struct stat *sb,
		int typeflag_unused __attribute__ ((unused)),
		struct FTW *s_unused __attribute__ ((unused)))
{
	if (S_ISREG(sb->st_mode) == 0)
		return FTW_CONTINUE;

	int fd = open(fpath, O_RDONLY|O_CLOEXEC);
	if (fd >= 0) {
		capng_results_t rc;

		capng_clear(CAPNG_SELECT_BOTH);
		capng_get_caps_fd(fd);
		rc = capng_have_capabilities(CAPNG_SELECT_CAPS);
		if (rc > CAPNG_NONE) {
			if (header == 0) {
				header = 1;
				printf("%-20s capabilities\n", "file");
			}
			printf("%s     ", fpath);
			if (rc == CAPNG_FULL)
				printf("full");
			else
				capng_print_caps_text(CAPNG_PRINT_STDOUT,
						CAPNG_PERMITTED);
			printf("\n");
		}
		close(fd);
	}
	return FTW_CONTINUE;
}


// Use cases:
//  filecap 
//  filecap -a
//  filecap /path/dir
//  filecap /path/file
//  filecap /path/file capability1 capability2 capability 3 ...
//
int main(int argc, char *argv[])
{
#if CAP_LAST_CAP < 31 || !defined (VFS_CAP_U32) || !defined (HAVE_ATTR_XATTR_H)
	printf("File based capabilities are not supported\n");
#else
	char *path_env, *path = NULL, *dir = NULL;
	struct stat sbuf;
	int nftw_flags = FTW_PHYS;
	int i;

	if (argc >1) {
		for (i=1; i<argc; i++) {	
			if (strcmp(argv[i], "-a") == 0) {
				show_all = 1;
				if (argc != 2)
					usage();
			} else if (strcmp(argv[i], "-d") == 0) {
				for (i=0; i<=CAP_LAST_CAP; i++) {
					const char *n =
						capng_capability_to_name(i);
					if (n == NULL)
						n = "unknown";
					printf("%s\n", n);
				}
				return 0;
			} else if (argv[i][0] == '/') {
				if (lstat(argv[i], &sbuf) != 0) {
					printf("Error checking path %s (%s)\n",
						argv[i], strerror(errno));
					exit(1);
				}
				// Clear all capabilities in case cap strings
				// follow. If we get a second file we err out
				// so this is safe
				if (S_ISREG(sbuf.st_mode) && path == NULL &&
								 dir == NULL) {
					path = argv[i];
					capng_clear(CAPNG_SELECT_BOTH);
				} else if (S_ISDIR(sbuf.st_mode) && path == NULL 
								&& dir == NULL)
					dir = argv[i];
				else {
					printf("Must be one regular file or "
						"directory\n");
					exit(1);
				}
			} else {
				int cap = capng_name_to_capability(argv[i]);
				if (cap >= 0) {
					if (path == NULL)
						usage();
					capng_update(CAPNG_ADD,
						CAPNG_PERMITTED|CAPNG_EFFECTIVE,
						cap);
					capabilities = 1;
				} else if (strcmp("none", argv[i]) == 0) {
					capng_clear(CAPNG_SELECT_BOTH);
					capabilities = 1;
					cremove = 1;
				} else {
					printf("Unrecognized capability.\n");
					usage();
				}
			}
		}
	}
	if (path == NULL && dir == NULL && show_all == 0) {
		path_env = getenv("PATH");
		if (path_env != NULL) {
			path = strdup(path_env);
			for (dir=strtok(path,":"); dir!=NULL;
						dir=strtok(NULL,":")) {
				nftw(dir, check_file, 1024, nftw_flags);
			}
			free(path);
		}
	} else if (path == NULL && dir == NULL && show_all == 1) {
		// Find files
		nftw("/", check_file, 1024, nftw_flags);
	} else if (dir) {
		// Print out the dir
		nftw(dir, check_file, 1024, nftw_flags);
	}else if (path && capabilities == 0) {
		// Print out specific file
		check_file(path, &sbuf, 0, NULL);
	} else if (path && capabilities == 1) {
		// Write capabilities to file
		int fd = open(path, O_WRONLY|O_NOFOLLOW|O_CLOEXEC);
		if (fd < 0) {
			printf("Could not open %s for writing (%s)\n", path,
				strerror(errno));
			return 1;
		}
		capng_apply_caps_fd(fd);
		close(fd);
	}
#endif
	return 0;
}

