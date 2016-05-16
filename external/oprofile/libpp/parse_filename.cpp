/**
 * @file parse_filename.cpp
 * Split a sample filename into its constituent parts
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#include <stdexcept>
#include <vector>
#include <string>
#include <iostream>
#include <sys/stat.h>

#include "parse_filename.h"
#include "file_manip.h"
#include "string_manip.h"
#include "locate_images.h"

using namespace std;

namespace {

// PP:3.19 event_name.count.unitmask.tgid.tid.cpu
parsed_filename parse_event_spec(string const & event_spec)
{
	typedef vector<string> parts_type;
	typedef parts_type::size_type size_type;

	size_type const nr_parts = 6;

	parts_type parts = separate_token(event_spec, '.');

	if (parts.size() != nr_parts) {
		throw invalid_argument("parse_event_spec(): bad event specification: " + event_spec);
	}

	for (size_type i = 0; i < nr_parts ; ++i) {
		if (parts[i].empty()) {
			throw invalid_argument("parse_event_spec(): bad event specification: " + event_spec);
		}
	}

	parsed_filename result;

	size_type i = 0;
	result.event = parts[i++];
	result.count = parts[i++];
	result.unitmask = parts[i++];
	result.tgid = parts[i++];
	result.tid = parts[i++];
	result.cpu = parts[i++];

	return result;
}


/**
 * @param component  path component
 *
 * remove from path_component all directory left to {root}, {kern} or {anon}
 */
void remove_base_dir(vector<string> & path)
{
	vector<string>::iterator it;
	for (it = path.begin(); it != path.end(); ++it) {
		if (*it == "{root}" || *it == "{kern}"  || *it == "{anon}")
			break;
	}

	path.erase(path.begin(), it);
}


/// Handle an anon region. Pretty print the details.
/// The second argument is the anon portion of the path which will
/// contain extra details such as the anon region name (unknown, vdso, heap etc.)
string const parse_anon(string const & str, string const & str2)
{
	string name = str2;
	// Get rid of "{anon:
	name.erase(0, 6);
	// Catch the case where we end up with an empty string.  This should
	// never happen, except where things have gone awfully bad with profile
	// data collection, resulting in one or more bogus sample files.
	if(0 == name.size())
		throw invalid_argument("parse_anon() invalid name: " + str2 + "\n"
			+ "This error indicates your sample data is suspect. It is "
			+ "recommended you do a --reset and collect new profile data.");
	// Get rid of the trailing '}'
	name.erase(name.size() - 1, 1);
	vector<string> parts = separate_token(str, '.');
	if (parts.size() != 3)
		throw invalid_argument("parse_anon() invalid name: " + str);

	string ret = name +" (tgid:";
	ret += parts[0] + " range:" + parts[1] + "-" + parts[2] + ")";
	return ret;
}


}  // anonymous namespace


/*
 *  valid filename are variations on:
 *
 * {kern}/name/event_spec
 * {root}/path/to/bin/{dep}/{root}/path/to/bin/event_spec
 * {root}/path/to/bin/{dep}/{anon:anon}/pid.start.end/event_spec
 * {root}/path/to/bin/{dep}/{anon:[vdso]}/pid.start.end/event_spec
 * {root}/path/to/bin/{dep}/{kern}/name/event_spec
 * {root}/path/to/bin/{dep}/{root}/path/to/bin/{cg}/{root}/path/to/bin/event_spec

 *
 * where /name/ denote a unique path component
 */
parsed_filename parse_filename(string const & filename,
			       extra_images const & extra_found_images)
{
	struct stat st;

	string::size_type pos = filename.find_last_of('/');
	if (pos == string::npos) {
		throw invalid_argument("parse_filename() invalid filename: " +
				       filename);
	}
	string event_spec = filename.substr(pos + 1);
	string filename_spec = filename.substr(0, pos);

	parsed_filename result = parse_event_spec(event_spec);

	result.filename = filename;

	vector<string> path = separate_token(filename_spec, '/');

	remove_base_dir(path);

	// pp_interface PP:3.19 to PP:3.23 path must start either with {root}
	// or {kern} and we must found at least 2 component, remove_base_dir()
	// return an empty path if {root} or {kern} are not found
	if (path.size() < 2) {
		throw invalid_argument("parse_filename() invalid filename: " +
				       filename);
	}

	size_t i;
	for (i = 1 ; i < path.size() ; ++i) {
		if (path[i] == "{dep}")
			break;

		result.image += "/" + path[i];
	}

	if (i == path.size()) {
		throw invalid_argument("parse_filename() invalid filename: " +
				       filename);
	}

	// skip "{dep}"
	++i;

	// PP:3.19 {dep}/ must be followed by {kern}/, {root}/ or {anon}/
	if (path[i] != "{kern}" && path[i] != "{root}" &&
	    path[i].find("{anon", 0) != 0) {
		throw invalid_argument("parse_filename() invalid filename: " +
				       filename);
	}

	bool anon = path[i].find("{anon:", 0) == 0;

	// skip "{root}", "{kern}" or "{anon:.*}"
	++i;

	for (; i < path.size(); ++i) {
		if (path[i] == "{cg}")
			break;

		if (anon) {
			pos = filename_spec.rfind('.');
			pos = filename_spec.rfind('.', pos-1);
			if (pos == string::npos) {
				throw invalid_argument("parse_filename() pid.addr.addr name expected: " +
						       filename_spec);
			}
			string jitdump = filename_spec.substr(0, pos) + ".jo";
			// if a jitdump file exists, we point to this file
			if (!stat(jitdump.c_str(), &st)) {
				// later code assumes an optional prefix path
				// is stripped from the lib_image.
				result.lib_image =
					extra_found_images.strip_path_prefix(jitdump);
				result.jit_dumpfile_exists = true;
			} else {
				result.lib_image =  parse_anon(path[i], path[i - 1]);
			}
			i++;
			break;
		} else {
			result.lib_image += "/" + path[i];
		}
	}

	if (i == path.size())
		return result;

	// skip "{cg}"
	++i;
	if (i == path.size() ||
	    (path[i] != "{kern}" && path[i] != "{root}" &&
	     path[i].find("{anon", 0) != 0)) {
		throw invalid_argument("parse_filename() invalid filename: "
		                       + filename);
	}

	// skip "{root}", "{kern}" or "{anon}"
	anon = (path[i].find("{anon", 0) == 0);
	++i;

	if (anon) {
		result.cg_image = parse_anon(path[i], path[i - 1]);
		i++;
	} else {
		for (; i < path.size(); ++i)
			result.cg_image += "/" + path[i];
	}

	return result;
}

bool parsed_filename::profile_spec_equal(parsed_filename const & parsed)
{
	return 	event == parsed.event &&
		count == parsed.count &&
		unitmask == parsed.unitmask &&
		tgid == parsed.tgid &&
		tid == parsed.tid &&
		cpu == parsed.tid;
}

ostream & operator<<(ostream & out, parsed_filename const & data)
{
	out << data.filename << endl;
	out << data.image << " " << data.lib_image << " "
	    << data.event << " " << data.count << " "
	    << data.unitmask << " " << data.tgid << " "
	    << data.tid << " " << data.cpu << endl;

	return out;
}
