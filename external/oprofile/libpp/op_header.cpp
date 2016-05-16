/**
 * @file op_header.cpp
 * various free function acting on a sample file header
 *
 * @remark Copyright 2004 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 * @Modifications Daniel Hansel
 */

#include <cstring>
#include <iostream>
#include <cstdlib>
#include <iomanip>
#include <set>
#include <sstream>
#include <cstring>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

#include "op_config.h"
#include "op_exception.h"
#include "odb.h"
#include "op_cpu_type.h"
#include "op_file.h"
#include "op_header.h"
#include "op_events.h"
#include "string_manip.h"
#include "format_output.h"
#include "xml_utils.h"
#include "cverb.h"

using namespace std;

extern verbose vbfd;

void op_check_header(opd_header const & h1, opd_header const & h2,
		     string const & filename)
{
	if (h1.mtime != h2.mtime) {
		ostringstream os;
		os << "header timestamps are different ("
		   << h1.mtime << ", " << h2.mtime << ") for "
		   << filename << "\n";
		throw op_fatal_error(os.str());
	}

	if (h1.is_kernel != h2.is_kernel) {
		ostringstream os;
		os << "header is_kernel flags are different for "
		   << filename << "\n";
		throw op_fatal_error(os.str());
	}
	
	// Note that in the generated ELF file for anonymous code the vma
	// of the symbol is exaclty the same vma as the code had during sampling.
	
	// Note that we don't check CPU speed since that can vary
	// freely on the same machine
}


namespace {

set<string> warned_files;

}

bool is_jit_sample(string const & filename)
{
	// suffix for JIT sample files (see FIXME in check_mtime() below)
	string suf = ".jo";
	
	string::size_type pos;
	pos = filename.rfind(suf);
	// for JIT sample files do not output the warning to stderr.
	if (pos != string::npos && pos == filename.size() - suf.size())
		return true;
	else
		return false;
}

void check_mtime(string const & file, opd_header const & header)
{
	time_t const newmtime = op_get_mtime(file.c_str());

	if (newmtime == header.mtime)
		return;

	if (warned_files.find(file) != warned_files.end())
		return;

	warned_files.insert(file);

	// Files we couldn't get mtime of have zero mtime
	if (!header.mtime) {
		// FIXME: header.mtime for JIT sample files is 0. The problem could be that
		//        in opd_mangling.c:opd_open_sample_file() the call of fill_header()
		//        think that the JIT sample file is not a binary file.
		if (is_jit_sample(file)) {
			cverb << vbfd << "warning: could not check that the binary file "
			      << file << " has not been modified since "
			      "the profile was taken. Results may be inaccurate.\n";
		} else {
			cerr << "warning: could not check that the binary file "
			     << file << " has not been modified since "
			     "the profile was taken. Results may be inaccurate.\n";
		}
	} else {
		static bool warned_already = false;

#ifdef ANDROID
		// Android symbol files may not have the same timestamp as the stripped
		// files deployed to the device.  Suppress spurious warnings.
		if (file.find("/symbols/") == string::npos) {
#endif

		cerr << "warning: the last modified time of the binary file "
		     "does not match that of the sample file for " << file
		     << "\n";

		if (!warned_already) {
			cerr << "Either this is the wrong binary or the binary "
			"has been modified since the sample file was created.\n";
			warned_already = true;
		}

#ifdef ANDROID
		}
#endif
	}
}


opd_header const read_header(string const & sample_filename)
{
	int fd = open(sample_filename.c_str(), O_RDONLY);
	if (fd < 0)
		throw op_fatal_error("Can't open sample file:" +
				     sample_filename);

	opd_header header;
	if (read(fd, &header, sizeof(header)) != sizeof(header)) {
		close(fd);
		throw op_fatal_error("Can't read sample file header:" +
				     sample_filename);
	}

	if (memcmp(header.magic, OPD_MAGIC, sizeof(header.magic))) {
		throw op_fatal_error("Invalid sample file, "
				     "bad magic number: " +
				     sample_filename);
		close(fd);
	}

	close(fd);

	return header;
}


namespace {

string const op_print_event(op_cpu cpu_type, u32 type, u32 um, u32 count)
{
	string str;

	if (cpu_type == CPU_TIMER_INT) {
		str += "Profiling through timer interrupt";
		return str;
	}

	struct op_event * event = op_find_event(cpu_type, type, um);

	if (!event) {
		event = op_find_event_any(cpu_type, type);
		if (!event) { 
			cerr << "Could not locate event " << int(type) << endl;
			str = "Unknown event";
			return str;
		}
	}

	char const * um_desc = 0;

	for (size_t i = 0; i < event->unit->num; ++i) {
		if (event->unit->um[i].value == um)
			um_desc = event->unit->um[i].desc;
	}

	str += string("Counted ") + event->name;
	str += string(" events (") + event->desc + ")";

	if (cpu_type != CPU_RTC) {
		str += " with a unit mask of 0x";

		ostringstream ss;
		ss << hex << setw(2) << setfill('0') << unsigned(um);
		str += ss.str();

		str += " (";
		str += um_desc ? um_desc : "multiple flags";
		str += ")";
	}

	str += " count " + op_lexical_cast<string>(count);
	return str;
}

string const op_xml_print_event(op_cpu cpu_type, u32 type, u32 um, u32 count)
{
	string unit_mask;

	if (cpu_type == CPU_TIMER_INT || cpu_type == CPU_RTC)
		return xml_utils::get_timer_setup((size_t)count);

	struct op_event * event = op_find_event(cpu_type, type, um);
	if (!event) {
		event = op_find_event_any(cpu_type, type);
		if (!event) { 
			cerr << "Could not locate event " << int(type) << endl;
			return "";
		}
	}

	if (cpu_type != CPU_RTC) {
		ostringstream str_out;
		str_out << um;
		unit_mask = str_out.str();
	}

	return xml_utils::get_event_setup(string(event->name),
		(size_t)count, unit_mask);
}

}

string const describe_header(opd_header const & header)
{
	op_cpu cpu = static_cast<op_cpu>(header.cpu_type);

	if (want_xml)
		return op_xml_print_event(cpu, header.ctr_event,
	                      header.ctr_um, header.ctr_count);
	else
		return op_print_event(cpu, header.ctr_event,
	                      header.ctr_um, header.ctr_count);
}


string const describe_cpu(opd_header const & header)
{
	op_cpu cpu = static_cast<op_cpu>(header.cpu_type);

	string str;
	if (want_xml) {
		string cpu_name = op_get_cpu_name(cpu);

		str = xml_utils::get_profile_header(cpu_name, header.cpu_speed);
	} else {
		str += string("CPU: ") + op_get_cpu_type_str(cpu);
		str += ", speed ";

		ostringstream ss;
		ss << header.cpu_speed;
		str += ss.str() + " MHz (estimated)";
	}
	return str;
}
