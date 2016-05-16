/**
 * @file oprof_start_config.cpp
 * GUI startup config management
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <stdio.h>

#include <sstream>
#include <fstream>
#include <iomanip>
#include <sys/utsname.h>

#include "string_manip.h"
#include "oprof_start_config.h"
#include "op_config.h"
#include "op_config_24.h"

using namespace std;

event_setting::event_setting()
	:
	count(0),
	umask(0),
	os_ring_count(0),
	user_ring_count(0)
{
}


config_setting::config_setting()
	:
	buffer_size(OP_DEFAULT_BUF_SIZE),
	note_table_size(OP_DEFAULT_NOTE_SIZE),
	no_kernel(false),
	verbose(false),
	separate_lib(false),
	separate_kernel(false),
	separate_cpu(false),
	separate_thread(false),
	callgraph_depth(0),
	buffer_watershed(0),
	cpu_buffer_size(0)
{
	struct utsname info;

	/* Guess path to vmlinux based on kernel currently running. */
	if (uname(&info)) {
		perror("oprof_start: Unable to determine OS release.");
	} else {
		string const version(info.release);
		string const vmlinux_path("/lib/modules/" + version
					 + "/build/vmlinux");
		kernel_filename = vmlinux_path;
	}
}


void config_setting::load(istream & in)
{
	buffer_size = OP_DEFAULT_BUF_SIZE;
	note_table_size = OP_DEFAULT_NOTE_SIZE;

	string str;

	while (getline(in, str)) {
		string val = split(str, '=');
		if (str == "BUF_SIZE") {
			buffer_size = op_lexical_cast<unsigned int>(val);
			if (buffer_size < OP_DEFAULT_BUF_SIZE)
				buffer_size = OP_DEFAULT_BUF_SIZE;
		} else if (str == "NOTE_SIZE") {
			note_table_size = op_lexical_cast<unsigned int>(val);
			if (note_table_size < OP_DEFAULT_NOTE_SIZE)
				note_table_size = OP_DEFAULT_NOTE_SIZE;
		} else if (str == "VMLINUX") {
			if (val == "none") {
				kernel_filename = "";
				no_kernel = true;
			} else if (!val.empty()) {
				no_kernel = false;
				kernel_filename = val;
			}
		} else if (str == "SEPARATE_LIB") {
			separate_lib = op_lexical_cast<bool>(val);
		} else if (str == "SEPARATE_KERNEL") {
			separate_kernel = op_lexical_cast<bool>(val);
		} else if (str == "SEPARATE_CPU") {
			separate_cpu = op_lexical_cast<bool>(val);
		} else if (str == "SEPARATE_THREAD") {
			separate_thread = op_lexical_cast<bool>(val);
		} else if (str == "CALLGRAPH") {
			callgraph_depth = op_lexical_cast<unsigned int>(val);
		} else if (str == "BUF_WATERSHED") {
			buffer_watershed = op_lexical_cast<unsigned int>(val);
		} else if (str == "CPU_BUF_SIZE") {
			cpu_buffer_size = op_lexical_cast<unsigned int>(val);
		}
	}
}


istream & operator>>(istream & in, config_setting & object)
{
	object.load(in);
	return in;
}
