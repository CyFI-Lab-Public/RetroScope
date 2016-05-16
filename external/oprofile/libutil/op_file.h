/**
 * @file op_file.h
 * Useful file management helpers
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OP_FILE_H
#define OP_FILE_H

#ifdef __cplusplus
extern "C" {
#endif

#include <sys/types.h>

/**
 * op_file_readable - is a file readable
 * @param file file name
 *
 * Return true if the given file is readable and regular.
 *
 * Beware of race conditions !
 */
int op_file_readable(char const * file);

/**
 * op_get_mtime - get mtime of file
 * @param file  file name
 *
 * Returns the mtime of the given file or 0 on failure
 */
time_t op_get_mtime(char const * file);

/**
 * create_dir - create a directory
 * @param dir  the directory name to create
 *
 * Returns 0 on success.
 */
int create_dir(char const * dir);


/**
 * create_path - create a path
 * @param path  the path to create
 *
 * create directory for each dir components in path
 * the last path component is not considered as a directory
 * but as a filename
 *
 * Returns 0 on success.
 */
int create_path(char const * path);

/**
 * Clients of get_matching_pathnames must provide their own implementation
 * of get_pathname_callback.
 */
typedef void (*get_pathname_callback)(char const * pathname, void * name_list);

/* This enum is intended solely for the use of get_matching_pathnames(),
 * bit 0 is reserved for internal use..*/
enum recursion_type {
	NO_RECURSION = 2,
	MATCH_ANY_ENTRY_RECURSION = 4,
	MATCH_DIR_ONLY_RECURSION = 8,
};
/**
 * @param name_list where to store result
 * @param get_pathname_callback client-provided callback function
 * @param base_dir directory from where lookup starts
 * @param filter a pathname filter
 * @param recursion recursion_type -- see above enum and following description:
 *	NO_RECURSION:  Find matching files from passed base_dir and call
 *          get_pathname_callback to add entry to name_list to be returned.
 *	MATCH_ANY_ENTRY_RECURSION: Starting at base_dir, for each entry in the
 *	   dir that matches the filter: if entry is of type 'dir', recurse;
 *         else call get_pathname_callback to add entry to name_list to be
 *         returned.
 *	MATCH_DIR_ONLY_RECURSION: Starting at base_dir, if an entry in the
 *         dir is of type 'dir' and its complete pathname contains a match to
 *         the filter, call get_pathname_callback to add entry to name_list to
 *         be returned; else recurse.
 *
 * Returns 0 on success.
 *
 * Return a list of pathnames under base_dir, filtered by filter and optionally
 * looking in sub-directory. See description above of the recursion_type 
 * parameter for more details.
 *    NOTE: For C clients: Your implementation of the get_pathname_callback
 *          function will probably dynamically allocate storage for elements
 *          added to name_list.  If so, remember to free that memory when it's
 *          no longer needed.
 */
int get_matching_pathnames(void * name_list, get_pathname_callback,
			   char const * base_dir, char const * filter,
			   enum recursion_type recursion);


#ifdef __cplusplus
}
#endif

#endif /* OP_FILE_H */
