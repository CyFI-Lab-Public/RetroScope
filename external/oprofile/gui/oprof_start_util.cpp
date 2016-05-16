/**
 * @file oprof_start_util.cpp
 * Miscellaneous helpers for the GUI start
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include <dirent.h>
#include <unistd.h>
#include <glob.h>

#include <cerrno>
#include <vector>
#include <cmath>
#include <sstream>
#include <iostream>
#include <fstream>
#include <cstdlib>

#include <qfiledialog.h>
#include <qmessagebox.h>

#include "op_file.h"
#include "file_manip.h"
#include "child_reader.h"
#include "op_libiberty.h"

#include "oprof_start.h"
#include "oprof_start_util.h"

using namespace std;

namespace {

// return the ~ expansion suffixed with a '/'
string const get_config_dir()
{
	return "/root";
}

string daemon_pid;

} // namespace anon

daemon_status::daemon_status()
	: running(false),
	  nr_interrupts(0)
{
	int HZ;
	if (!daemon_pid.empty()) {
		string proc_filename = string("/proc/") + daemon_pid + "/exe";
		string const exec = op_realpath(proc_filename);
		if (exec == proc_filename)
			daemon_pid.erase();
		else
			running = true;
	}

	if (daemon_pid.empty()) {
		DIR * dir;
		struct dirent * dirent;

		if (!(dir = opendir("/proc"))) {
			perror("oprofiled: /proc directory could not be opened. ");
			exit(EXIT_FAILURE);
		}

		while ((dirent = readdir(dir))) {
			string const exec =
				op_realpath(string("/proc/")
				               + dirent->d_name + "/exe");
			string const name = op_basename(exec);
			if (name != "oprofiled")
				continue;

			daemon_pid = dirent->d_name;
			running = true;
		}

		closedir(dir);
	}

	HZ = sysconf(_SC_CLK_TCK);
	if (HZ == -1) {
		perror("oprofiled: Unable to determine clock ticks per second. ");
		exit(EXIT_FAILURE);
	}

	if (daemon_pid.empty())
		return;

	nr_interrupts = 0;

	switch (op_get_interface()) {
	case OP_INTERFACE_24:
		{
			ifstream ifs3("/proc/sys/dev/oprofile/nr_interrupts");
			if (ifs3)
				ifs3 >> nr_interrupts;
		}
		break;
	case OP_INTERFACE_26:
		{
			static unsigned int old_sum_interrupts;
			unsigned int sum_interrupts = 0;
			glob_t file_names;

			file_names.gl_offs = 0;
			glob("/dev/oprofile/stats/cpu*/sample_received",
			     GLOB_DOOFFS, NULL, &file_names);

			for (size_t i = 0; i < file_names.gl_pathc; ++i) {
				ifstream ifs3(file_names.gl_pathv[i]);
				if (ifs3) {
					unsigned int file_interrupts;
					ifs3 >> file_interrupts;
					sum_interrupts += file_interrupts;
				}
			}
			if (old_sum_interrupts > sum_interrupts)
				// occur if we stop/restart daemon.
				old_sum_interrupts = 0;
			nr_interrupts = sum_interrupts - old_sum_interrupts;
			old_sum_interrupts = sum_interrupts;
			globfree(&file_names);
		}
		break;
	default:
		break;
	}
}


/**
 * get_config_filename - get absolute filename of file in user $HOME
 * @param filename  the relative filename
 *
 * Get the absolute path of a file in a user's home directory.
 */
string const get_config_filename(string const & filename)
{
	return get_config_dir() + "/" + filename;
}


/**
 * check_and_create_config_dir - make sure config dir is accessible
 *
 * Returns %true if the dir is accessible.
 */
bool check_and_create_config_dir()
{
	string dir = get_config_filename(".oprofile");

	char * name = xstrdup(dir.c_str());

	if (create_dir(name)) {
		ostringstream out;
		out << "unable to create " << dir << " directory ";
		out << "cause: " << strerror(errno);
		QMessageBox::warning(0, 0, out.str().c_str());

		free(name);

		return false;
	}

	free(name);
	return true;
}


/**
 * format - re-format a string
 * @param orig  string to format
 * @param maxlen  width of line
 *
 * Re-formats a string to fit into a certain width,
 * breaking lines at spaces between words.
 *
 * Returns the formatted string
 */
string const format(string const & orig, uint const maxlen)
{
	string text(orig);

	istringstream ss(text);
	vector<string> lines;

	string oline;
	string line;

	while (getline(ss, oline)) {
		if (line.size() + oline.size() < maxlen) {
			lines.push_back(line + oline);
			line.erase();
		} else {
			lines.push_back(line);
			line.erase();
			string s;
			string word;
			istringstream oss(oline);
			while (oss >> word) {
				if (line.size() + word.size() > maxlen) {
					lines.push_back(line);
					line.erase();
				}
				line += word + " ";
			}
		}
	}

	if (line.size())
		lines.push_back(line);

	string ret;

	for(vector<string>::const_iterator it = lines.begin(); it != lines.end(); ++it)
		ret += *it + "\n";

	return ret;
}


/**
 * do_exec_command - execute a command
 * @param cmd  command name
 * @param args  arguments to command
 *
 * Execute a command synchronously. An error message is shown
 * if the command returns a non-zero status, which is also returned.
 *
 * The arguments are verified and will refuse to execute if they contain
 * shell metacharacters.
 */
int do_exec_command(string const & cmd, vector<string> const & args)
{
	ostringstream err;
	bool ok = true;

	// verify arguments
	for (vector<string>::const_iterator cit = args.begin();
		cit != args.end(); ++cit) {
		if (verify_argument(*cit))
			continue;

		QMessageBox::warning(0, 0,
			string(
			"Could not execute: Argument \"" + *cit +
			"\" contains shell metacharacters.\n").c_str());
		return EINVAL;
	}

	child_reader reader(cmd, args);
	if (reader.error())
		ok = false;

	if (ok)
		reader.get_data(cout, err);

	int ret = reader.terminate_process();
	if (ret) {
		string error = reader.error_str() + "\n";
		error += "Failed: \n" + err.str() + "\n";
		string cmdline = cmd;
		for (vector<string>::const_iterator cit = args.begin();
		     cit != args.end(); ++cit) {
			cmdline += " " + *cit + " ";
		}
		error += "\n\nCommand was :\n\n" + cmdline + "\n";

		QMessageBox::warning(0, 0, format(error, 50).c_str());
	}

	return ret;
}


/**
 * do_open_file_or_dir - open file/directory
 * @param base_dir  directory to start at
 * @param dir_only  directory or filename to select
 *
 * Select a file or directory. The selection is returned;
 * an empty string if the selection was cancelled.
 */
string const do_open_file_or_dir(string const & base_dir, bool dir_only)
{
	QString result;

	if (dir_only) {
		result = QFileDialog::getExistingDirectory(base_dir.c_str(), 0,
			"open_file_or_dir", "Get directory name", true);
	} else {
		result = QFileDialog::getOpenFileName(base_dir.c_str(), 0, 0,
			"open_file_or_dir", "Get filename");
	}

	if (result.isNull())
		return string();
	else
		return result.latin1();
}

/**
 * verify_argument - check string for potentially dangerous characters
 *
 * This function returns false if the string contains dangerous shell
 * metacharacters.
 *
 * WWW Security FAQ dangerous chars:
 *
 * & ; ` ' \ " | * ? ~ < > ^ ( ) [ ] { } $ \n \r
 *
 * David Wheeler: ! #
 *
 * We allow '-' because we disallow whitespace. We allow ':' and '='
 */
bool verify_argument(string const & str)
{
	if (str.find_first_not_of(
		"ABCDEFGHIJKLMNOPQRSTUVWXYZ"
		"abcdefghijklmnopqrstuvwxyz0123456789_:=-+%,./")
		!= string::npos)
		return false;
	return true;
}
