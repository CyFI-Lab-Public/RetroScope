/**
 * @file profile_spec.cpp
 * Contains a PP profile specification
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#include <algorithm>
#include <set>
#include <sstream>
#include <iterator>
#include <iostream>
#include <dirent.h>

#include "file_manip.h"
#include "op_config.h"
#include "profile_spec.h"
#include "string_manip.h"
#include "glob_filter.h"
#include "locate_images.h"
#include "op_exception.h"
#include "op_header.h"
#include "op_fileio.h"

using namespace std;

namespace {

// PP:3.7, full path, or relative path. If we can't find it,
// we should maintain the original to maintain the wordexp etc.
string const fixup_image_spec(string const & str, extra_images const & extra)
{
	// On error find_image_path() return str, so if an occur we will
	// use the provided image_name not the fixed one.
	image_error error;
	return extra.find_image_path(str, error, true);
}

void fixup_image_spec(vector<string> & images, extra_images const & extra)
{
	vector<string>::iterator it = images.begin();
	vector<string>::iterator const end = images.end();

	for (; it != end; ++it)
		*it = fixup_image_spec(*it, extra);
}

}  // anon namespace


profile_spec::profile_spec()
	:
	extra_found_images()
{
	parse_table["archive"] = &profile_spec::parse_archive_path;
	parse_table["session"] = &profile_spec::parse_session;
	parse_table["session-exclude"] =
		&profile_spec::parse_session_exclude;
	parse_table["image"] = &profile_spec::parse_image;
	parse_table["image-exclude"] = &profile_spec::parse_image_exclude;
	parse_table["lib-image"] = &profile_spec::parse_lib_image;
	parse_table["event"] = &profile_spec::parse_event;
	parse_table["count"] = &profile_spec::parse_count;
	parse_table["unit-mask"] = &profile_spec::parse_unitmask;
	parse_table["tid"] = &profile_spec::parse_tid;
	parse_table["tgid"] = &profile_spec::parse_tgid;
	parse_table["cpu"] = &profile_spec::parse_cpu;
}


void profile_spec::parse(string const & tag_value)
{
	string value;
	action_t action = get_handler(tag_value, value);
	if (!action) {
		throw invalid_argument("profile_spec::parse(): not "
				       "a valid tag \"" + tag_value + "\"");
	}

	(this->*action)(value);
}


bool profile_spec::is_valid_tag(string const & tag_value)
{
	string value;
	return get_handler(tag_value, value);
}


void profile_spec::set_image_or_lib_name(string const & str)
{
	/* FIXME: what does spec say about this being allowed to be
	 * a comma list or not ? */
	image_or_lib_image.push_back(fixup_image_spec(str, extra_found_images));
}


void profile_spec::parse_archive_path(string const & str)
{
	archive_path = op_realpath(str);
}


string profile_spec::get_archive_path() const
{
	return archive_path;
}


void profile_spec::parse_session(string const & str)
{
	session = separate_token(str, ',');
}


void profile_spec::parse_session_exclude(string const & str)
{
	session_exclude = separate_token(str, ',');
}


void profile_spec::parse_image(string const & str)
{
	image = separate_token(str, ',');
	fixup_image_spec(image, extra_found_images);
}


void profile_spec::parse_image_exclude(string const & str)
{
	image_exclude = separate_token(str, ',');
	fixup_image_spec(image_exclude, extra_found_images);
}


void profile_spec::parse_lib_image(string const & str)
{
	lib_image = separate_token(str, ',');
	fixup_image_spec(lib_image, extra_found_images);
}


void profile_spec::parse_event(string const & str)
{
	event.set(str);
}


void profile_spec::parse_count(string const & str)
{
	count.set(str);
}


void profile_spec::parse_unitmask(string const & str)
{
	unitmask.set(str);
}


void profile_spec::parse_tid(string const & str)
{
	tid.set(str);
}


void profile_spec::parse_tgid(string const & str)
{
	tgid.set(str);
}


void profile_spec::parse_cpu(string const & str)
{
	cpu.set(str);
}


profile_spec::action_t
profile_spec::get_handler(string const & tag_value, string & value)
{
	string::size_type pos = tag_value.find_first_of(':');
	if (pos == string::npos)
		return 0;

	string tag(tag_value.substr(0, pos));
	value = tag_value.substr(pos + 1);

	parse_table_t::const_iterator it = parse_table.find(tag);
	if (it == parse_table.end())
		return 0;

	return it->second;
}


namespace {

/// return true if the value from the profile spec may match the comma
/// list
template<typename T>
bool comma_match(comma_list<T> const & cl, generic_spec<T> const & value)
{
	// if the profile spec is "all" we match the sample file
	if (!cl.is_set())
		return true;
	
	// an "all" sample file should never match specified profile
	// spec values
	if (!value.is_set())
		return false;

	// now match each profile spec value against the sample file
	return cl.match(value.value());
}

}


bool profile_spec::match(filename_spec const & spec) const
{
	bool matched_by_image_or_lib_image = false;

	// We need the true image name not the one based on the sample
	// filename for the benefit of module which have /oprofile in their
	// sample filename. This allow to specify profile spec based on the
	// real name of the image, e.g. 'binary:*oprofile.ko'
	string simage = fixup_image_spec(spec.image, extra_found_images);
	string slib_image = fixup_image_spec(spec.lib_image,
					     extra_found_images);

	// PP:3.19
	if (!image_or_lib_image.empty()) {
		glob_filter filter(image_or_lib_image, image_exclude);
		if (filter.match(simage) || filter.match(slib_image))
			matched_by_image_or_lib_image = true;
	}

	if (!matched_by_image_or_lib_image) {
		// PP:3.7 3.8
		if (!image.empty()) {
			glob_filter filter(image, image_exclude);
			if (!filter.match(simage))
				return false;
		} else if (!image_or_lib_image.empty()) {
			// image.empty() means match all except if user
			// specified image_or_lib_image
			return false;
		}

		// PP:3.9 3.10
		if (!lib_image.empty()) {
			glob_filter filter(lib_image, image_exclude);
			if (!filter.match(slib_image))
				return false;
		} else if (image.empty() && !image_or_lib_image.empty()) {
			// lib_image empty means match all except if user
			// specified image_or_lib_image *or* we already
			// matched this spec through image
			return false;
		}
	}

	if (!matched_by_image_or_lib_image) {
		// if we don't match by image_or_lib_image we must try to
		// exclude from spec, exclusion from image_or_lib_image has
		// been handled above
		vector<string> empty;
		glob_filter filter(empty, image_exclude);
		if (!filter.match(simage))
			return false;
		if (!spec.lib_image.empty() && !filter.match(slib_image))
			return false;
	}

	if (!event.match(spec.event))
		return false;

	if (!count.match(spec.count))
		return false;

	if (!unitmask.match(spec.unitmask))
		return false;

	if (!comma_match(cpu, spec.cpu))
		return false;

	if (!comma_match(tid, spec.tid))
		return false;

	if (!comma_match(tgid, spec.tgid))
		return false;

	return true;
}


profile_spec profile_spec::create(list<string> const & args,
                                  vector<string> const & image_path,
				  string const & root_path)
{
	profile_spec spec;
	set<string> tag_seen;
	vector<string> temp_image_or_lib;

	list<string>::const_iterator it = args.begin();
	list<string>::const_iterator end = args.end();

	for (; it != end; ++it) {
		if (spec.is_valid_tag(*it)) {
			if (tag_seen.find(*it) != tag_seen.end()) {
				throw op_runtime_error("tag specified "
				       "more than once: " + *it);
			}
			tag_seen.insert(*it);
			spec.parse(*it);
		} else {
			string const file = op_realpath(*it);
			temp_image_or_lib.push_back(file);
		}
	}

	// PP:3.5 no session given means use the current session.
	if (spec.session.empty())
		spec.session.push_back("current");

	bool ok = true;
	vector<string>::const_iterator ip_it = image_path.begin();
	for ( ; ip_it != image_path.end(); ++ip_it) {
		if (!is_directory(spec.get_archive_path() + "/" + *ip_it)) {
			cerr << spec.get_archive_path() + "/" + *ip_it << " isn't a valid directory\n";
			ok = false;
		}
	}
	if (!ok)
		throw op_runtime_error("invalid --image-path= options");

	spec.extra_found_images.populate(image_path, spec.get_archive_path(),
					 root_path);
	vector<string>::const_iterator im = temp_image_or_lib.begin();
	vector<string>::const_iterator last = temp_image_or_lib.end();
	for (; im != last; ++im)
		spec.set_image_or_lib_name(*im);

	return spec;
}

namespace {

vector<string> filter_session(vector<string> const & session,
			      vector<string> const & session_exclude)
{
	vector<string> result(session);

	if (result.empty())
		result.push_back("current");

	for (size_t i = 0 ; i < session_exclude.size() ; ++i) {
		// FIXME: would we use fnmatch on each item, are we allowed
		// to --session=current* ?
		vector<string>::iterator it =
			find(result.begin(), result.end(), session_exclude[i]);

		if (it != result.end())
			result.erase(it);
	}

	return result;
}

static bool invalid_sample_file;
bool valid_candidate(string const & base_dir, string const & filename,
                     profile_spec const & spec, bool exclude_dependent,
                     bool exclude_cg)
{
	if (exclude_cg && filename.find("{cg}") != string::npos)
		return false;

	// strip out non sample files
	string const & sub = filename.substr(base_dir.size(), string::npos);
	if (!is_prefix(sub, "/{root}/") && !is_prefix(sub, "/{kern}/"))
		return false;

	/* When overflows occur in the oprofile kernel driver's sample
	 * buffers (caused by too high of a sampling rate), it's possible
	 * for samples to be mis-attributed.  A common scenario is that,
	 * while profiling process 'abc' running binary 'xzy', the task
	 * switch for 'abc' gets dropped somehow.  Then, samples are taken
	 * for the 'xyz' binary.  In the attempt to attribute the samples to
	 * the associated binary, the oprofile kernel code examines the
	 * the memory mappings for the last process for which it recorded
	 * a task switch.  When profiling at a very high rate, the oprofile
	 * daemon is often the process that is mistakenly examined.  Then the
	 * sample from binary 'xyz' is matched to some file that's open in
	 * oprofiled's memory space.  Because oprofiled has many sample files
	 * open at any given time, there's a good chance the sample's VMA is
	 * contained within one of those sample files.  So, once finding this
	 * bogus match, the oprofile kernel records a cookie switch for the
	 * sample file.  This scenario is made even more likely if a high
	 * sampling rate (e.g., profiling on several events) is paired with
	 * callgraph data collection.
	 *
	 * When the daemon processes this sample data from the kernel, it
	 * creates a sample file for the sample file, resulting in something
	 * of the form:
	 *    <session-dir>/[blah]<session-dir>/[blah]
	 *
	 * When the sample data is post-processed, the sample file is parsed to
	 * try to determine the name of the binary, but it gets horribly confused.
	 * At best, the post-processing tool will spit out some warning messages,
	 * such as:
	 * warning:
	 * /lib64/libdl-2.9.so/CYCLES.10000.0.all.all.all/{dep}/{root}/var/lib/oprofile/samples/current/{root}/lib64/libdl-2.9.so/{dep}/{root}/lib64/libdl-2.9.so/PM_RUN_CYC_GRP12.10000.0.all.all.all
	 * could not be found.
	 *
	 * At worst, the parsing may result in an "invalid argument" runtime error
	 * because of the inability to parse a sample file whose name contains that
	 * of another sample file.  This typically seems to happen when callgraph
	 * data is being collected.
	 *
	 * The next several lines of code checks if the passed filename
	 * contains <session-dir>/samples; if so, we discard it as an
	 * invalid sample file.
	 */

	unsigned int j = base_dir.rfind('/');
	string session_samples_dir = base_dir.substr(0, j);
	if (sub.find(session_samples_dir) != string::npos) {
		invalid_sample_file = true;
		return false;
	}

	// strip out generated JIT object files for samples of anonymous regions
	if (is_jit_sample(sub))
		return false;

	filename_spec file_spec(filename, spec.extra_found_images);
	if (spec.match(file_spec)) {
		if (exclude_dependent && file_spec.is_dependent())
			return false;
		return true;
	}

	return false;
}


/**
 * Print a warning message if we detect any sample buffer overflows
 * occurred in the kernel driver. 
 */
void warn_if_kern_buffs_overflow(string const & session_samples_dir)
{
	DIR * dir;
	struct dirent * dirent;
	string stats_path;
	int ret = 0;
	
	stats_path = session_samples_dir + "stats/";
	ret = op_read_int_from_file((stats_path + "event_lost_overflow").
				    c_str(), 0);

	if (!(dir = opendir(stats_path.c_str()))) {
		ret = -1;
		goto done;
	}

	while ((dirent = readdir(dir)) && !ret) {
		int cpu_nr;
		string path;
		if (sscanf(dirent->d_name, "cpu%d", &cpu_nr) != 1)
			continue;
		path = stats_path + dirent->d_name + "/";
		ret = op_read_int_from_file((path + "sample_lost_overflow").
					    c_str(), 0);
	}
	closedir(dir);

 done:
	if (ret > 0) {
		cerr << "WARNING! The OProfile kernel driver reports sample "
		     << "buffer overflows." << endl;
		cerr << "Such overflows can result in incorrect sample attribution"
		     << ", invalid sample" << endl
		     <<	"files and other symptoms.  "
		     << "See the oprofiled.log for details." << endl;
		cerr << "You should adjust your sampling frequency to eliminate"
		     << " (or at least minimize)" << endl
		     <<	"these overflows." << endl;
	}
}


}  // anonymous namespace


list<string> profile_spec::generate_file_list(bool exclude_dependent,
  bool exclude_cg) const
{
	// FIXME: isn't remove_duplicates faster than doing this, then copy() ?
	set<string> unique_files;

	vector<string> sessions = filter_session(session, session_exclude);

	if (sessions.empty()) {
		ostringstream os;
		os << "No session given\n"
		   << "included session was:\n";
		copy(session.begin(), session.end(),
		     ostream_iterator<string>(os, "\n"));
		os << "excluded session was:\n";
		copy(session_exclude.begin(), session_exclude.end(),
		     ostream_iterator<string>(os, "\n"));
		throw invalid_argument(os.str());
	}

	bool found_file = false;

	vector<string>::const_iterator cit = sessions.begin();
	vector<string>::const_iterator end = sessions.end();

	for (; cit != end; ++cit) {
		if (cit->empty())
			continue;

		string base_dir;
		invalid_sample_file = false;
		if ((*cit)[0] != '.' && (*cit)[0] != '/')
			base_dir = archive_path + op_samples_dir;
		base_dir += *cit;

		base_dir = op_realpath(base_dir);

		list<string> files;
		create_file_list(files, base_dir, "*", true);

		if (!files.empty()) {
			found_file = true;
			warn_if_kern_buffs_overflow(base_dir + "/");
		}

		list<string>::const_iterator it = files.begin();
		list<string>::const_iterator fend = files.end();
		for (; it != fend; ++it) {
			if (valid_candidate(base_dir, *it, *this,
			    exclude_dependent, exclude_cg)) {
				unique_files.insert(*it);
			}
		}
		if (invalid_sample_file) {
			cerr << "Warning: Invalid sample files found in "
			     << base_dir << endl;
			cerr << "This problem can be caused by too high of a sampling rate."
			     << endl;
		}
	}

	if (!found_file) {
		ostringstream os;
		os  << "No sample file found: try running opcontrol --dump\n"
		    << "or specify a session containing sample files\n";
		throw op_fatal_error(os.str());
	}

	list<string> result;
	copy(unique_files.begin(), unique_files.end(), back_inserter(result));

	return result;
}
