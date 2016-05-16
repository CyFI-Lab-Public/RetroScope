/**
 * @file opgprof_options.cpp
 * Options for opgprof tool
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <cstdlib>

#include <vector>
#include <list>
#include <iterator>
#include <iostream>
#include <cstdlib>

#include "opgprof_options.h"
#include "popt_options.h"
#include "cverb.h"
#include "profile_spec.h"
#include "arrange_profiles.h"

using namespace std;

profile_classes classes;
inverted_profile image_profile;

namespace options {
	string gmon_filename = "gmon.out";

	// Ugly, for build only
	demangle_type demangle;
}


namespace {

popt::option options_array[] = {
	popt::option(options::gmon_filename, "output-filename", 'o',
	             "output filename, defaults to gmon.out if not specified",
	             "filename"),
	popt::option(options::threshold_opt, "threshold", 't',
		     "minimum percentage needed to produce output",
		     "percent"),
};


bool try_merge_profiles(profile_spec const & spec, bool exclude_dependent)
{
	list<string> sample_files = spec.generate_file_list(exclude_dependent, false);

	cverb << vsfile
	      << "Matched sample files: " << sample_files.size() << endl;
	copy(sample_files.begin(), sample_files.end(),
	     ostream_iterator<string>(cverb << vsfile, "\n"));

	// opgprof merge all by default
	merge_option merge_by;
	merge_by.cpu = true;
	merge_by.lib = true;
	merge_by.tid = true;
	merge_by.tgid = true;
	merge_by.unitmask = true;

	classes	= arrange_profiles(sample_files, merge_by,
				   spec.extra_found_images);

	cverb << vsfile << "profile_classes:\n" << classes << endl;

	size_t nr_classes = classes.v.size();

	list<inverted_profile> iprofiles = invert_profiles(classes);

	if (nr_classes == 1 && iprofiles.size() == 1) {
		image_profile = *(iprofiles.begin());
		return true;
	}

	// come round for another try
	if (exclude_dependent)
		return false;

	if (iprofiles.empty()) {
		cerr << "error: no sample files found: profile specification "
		     "too strict ?" << endl;
		exit(EXIT_FAILURE);
	}

	if (nr_classes > 1 || iprofiles.size() > 1) {
		cerr << "error: specify exactly one binary to process "
		     "and give an event: or count: specification if necessary"
		     << endl;
		exit(EXIT_FAILURE);
	}

	return false;
}

}  // anonymous namespace


void handle_options(options::spec const & spec)
{
	if (spec.first.size()) {
		cerr << "differential profiles not allowed" << endl;
		exit(EXIT_FAILURE);
	}

	profile_spec const pspec =
		profile_spec::create(spec.common, options::image_path,
				     options::root_path);

	cverb << vsfile << "output filename: " << options::gmon_filename
	      << endl;

	// we do a first try with exclude-dependent if it fails we include
	// dependent. First try should catch "opgrof /usr/bin/make" whilst
	// the second catch "opgprof /lib/libc-2.2.5.so"
	if (!try_merge_profiles(pspec, true))
		try_merge_profiles(pspec, false);
}
