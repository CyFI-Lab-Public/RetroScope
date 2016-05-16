/**
 * @file file_manip.cpp
 * Useful file management helpers
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <utime.h>
#include <limits.h>
#include <stdlib.h>

#include <cstdio>
#include <cerrno>
#include <iostream>
#include <fstream>
#include <vector>

#include "op_file.h"

#include "file_manip.h"
#include "string_manip.h"

using namespace std;


bool copy_file(string const & source, string const & destination)
{
	int retval;
	struct stat buf;
	if (stat(source.c_str(), &buf))
		return false;

	if (!op_file_readable(source))
		return false;

	ifstream in(source.c_str());
	if (!in)
		return false;

	mode_t mode = buf.st_mode & ~S_IFMT;
	if (!(mode & S_IWUSR))
		mode |= S_IWUSR;

	int fd = open(destination.c_str(), O_RDWR|O_CREAT, mode);
	if (fd < 0)
		return false;
	close(fd);


	// ignore error here: a simple user can copy a root.root 744 file
	// but can't chown the copied file to root.
	retval = chown(destination.c_str(), buf.st_uid, buf.st_gid);

	// a scope to ensure out is closed before changing is mtime/atime
	{
	ofstream out(destination.c_str(), ios::trunc);
	if (!out)
		return false;
	out << in.rdbuf();
	}

	struct utimbuf utim;
	utim.actime = buf.st_atime;
	utim.modtime = buf.st_mtime;
	if (utime(destination.c_str(), &utim))
		return false;

	return true;
}


bool is_directory(string const & dirname)
{
	struct stat st;
	return !stat(dirname.c_str(), &st) && S_ISDIR(st.st_mode);
}


bool is_files_identical(string const & file1, string const & file2)
{
	struct stat st1;
	struct stat st2;

	if (stat(file1.c_str(), &st1) == 0 && stat(file2.c_str(), &st2) == 0) {
		if (st1.st_dev == st2.st_dev && st1.st_ino == st2.st_ino)
			return true;
	}

	return false;
}


string const op_realpath(string const & name)
{
	static char tmp[PATH_MAX];
	if (!realpath(name.c_str(), tmp))
		return name;
	return string(tmp);
}


bool op_file_readable(string const & file)
{
	return op_file_readable(file.c_str());
}

static void get_pathname(const char * pathname, void * name_list)
{
	list<string> * file_list = (list<string> *)name_list;
	file_list->push_back(pathname);
}

bool create_file_list(list<string> & file_list, string const & base_dir,
		      string const & filter, bool recursive)
{
	return !get_matching_pathnames(&file_list, get_pathname,
				       base_dir.c_str(), filter.c_str(),
				       recursive ? MATCH_ANY_ENTRY_RECURSION :
				       NO_RECURSION) ? true : false;
 
}


/**
 * @param path_name the path where we remove trailing '/'
 *
 * erase all trailing '/' in path_name except if the last '/' is at pos 0
 */
static string erase_trailing_path_separator(string const & path_name)
{
	string result(path_name);

	while (result.length() > 1) {
		if (result[result.length() - 1] != '/')
			break;
		result.erase(result.length() - 1, 1);
	}

	return result;
}

string op_dirname(string const & file_name)
{
	string result = erase_trailing_path_separator(file_name);
	if (result.find_first_of('/') == string::npos)
		return "."; 	 
  	 
	// catch result == "/" 	 
	if (result.length() == 1) 	 
		return result;

	size_t pos = result.find_last_of('/'); 	 

	// "/usr" must return "/" 	 
	if (pos == 0) 	 
		pos = 1; 	 

	result.erase(pos, result.length() - pos); 	 

	// "////usr" must return "/"
	return erase_trailing_path_separator(result);
}


string op_basename(string const & path_name)
{
	string result = erase_trailing_path_separator(path_name);

	// catch result == "/"
	if (result.length() == 1)
		return result;

	return erase_to_last_of(result, '/');
}
