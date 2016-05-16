/**
 * @file file_manip.h
 * Useful file management helpers
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef FILE_MANIP_H
#define FILE_MANIP_H

#include <string>
#include <list>


/**
 * copy_file - copy a file.
 * @param source        filename to copy from
 * @param destination   filename to copy into
 *
 * the last modification time of the source file is preserved, file attribute
 * and owner are preserved if possible. Return true if copying successful.
 */
bool copy_file(std::string const & source, std::string const & destination);

/// return true if dir is an existing directory
bool is_directory(std::string const & dirname);

/**
 * is_file_identical - check for identical files
 * @param file1  first filename
 * @param file2  second filename
 *
 * return true if the two filenames belong to the same file
 */
bool is_files_identical(std::string const & file1, std::string const & file2);

/**
 * op_realpath - resolve symlinks etc.
 * Resolve a path as much as possible. Accounts for relative
 * paths (from cwd), ".." and ".". For success, the target
 * file must exist !
 *
 * Resolve a symbolic link as far as possible.
 * Returns the original string on failure.
 */
std::string const op_realpath(std::string const & name);

/// return true if the given file is readable
bool op_file_readable(std::string const & file);

/**
 * @param file_list where to store result
 * @param base_dir directory from where lookup start
 * @param filter a filename filter
 * @param recursive if true lookup in sub-directory
 *
 * create a filelist under base_dir, filtered by filter and optionally
 * looking in sub-directory. If we look in sub-directory only sub-directory
 * which match filter are traversed.
 */
bool create_file_list(std::list<std::string> & file_list,
		      std::string const & base_dir,
		      std::string const & filter = "*",
		      bool recursive = false);

/**
 * op_dirname - get the path component of a filename
 * @param file_name  filename
 *
 * Returns the path name of a filename with trailing '/' removed.
 */
std::string op_dirname(std::string const & file_name);

/**
 * op_basename - get the basename of a path
 * @param path_name  path
 *
 * Returns the basename of a path with trailing '/' removed.
 *
 * Always use this instead of the C basename() - header order
 * can affect behaviour  for basename("/")
 */
std::string op_basename(std::string const & path_name);

#endif /* !FILE_MANIP_H */
