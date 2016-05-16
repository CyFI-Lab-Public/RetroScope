/**
 * @file populate.cpp
 * Fill up a profile_container from inverted profiles
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 *
 * Modified by Maynard Johnson <maynardj@us.ibm.com>
 * (C) Copyright IBM Corporation 2007
 */

#include "profile.h"
#include "profile_container.h"
#include "arrange_profiles.h"
#include "op_bfd.h"
#include "op_header.h"
#include "populate.h"
#include "populate_for_spu.h"

#include "image_errors.h"

#include <iostream>

using namespace std;

namespace {

/// load merged files for one set of sample files
bool
populate_from_files(profile_t & profile, op_bfd const & abfd,
                    list<profile_sample_files> const & files)
{
	list<profile_sample_files>::const_iterator it = files.begin();
	list<profile_sample_files>::const_iterator const end = files.end();

	bool found = false;
	// we can't handle cg files here obviously
	for (; it != end; ++it) {
		// A bit ugly but we must accept silently empty sample filename
		// since we can create a profile_sample_files for cg file only
		// (i.e no sample to the binary)
		if (!it->sample_filename.empty()) {
			profile.add_sample_file(it->sample_filename);
			profile.set_offset(abfd);
			found = true;
		}
	}

	return found;
}

}  // anon namespace


void
populate_for_image(profile_container & samples, inverted_profile const & ip,
	string_filter const & symbol_filter, bool * has_debug_info)
{
	if (is_spu_profile(ip)) {
		populate_for_spu_image(samples, ip, symbol_filter,
				       has_debug_info);
		return;
	}

	bool ok = ip.error == image_ok;
	op_bfd abfd(ip.image, symbol_filter,
		    samples.extra_found_images, ok);
	if (!ok && ip.error == image_ok)
		ip.error = image_format_failure;

	if (ip.error == image_format_failure)
		report_image_error(ip, false, samples.extra_found_images);

	opd_header header;

	bool found = false;
	for (size_t i = 0; i < ip.groups.size(); ++i) {
		list<image_set>::const_iterator it
			= ip.groups[i].begin();
		list<image_set>::const_iterator const end
			= ip.groups[i].end();

		// we can only share a profile_t amongst each
		// image_set's files - this is because it->app_image
		// changes, and the .add() would mis-attribute
		// to the wrong app_image otherwise
		for (; it != end; ++it) {
			profile_t profile;
			if (populate_from_files(profile, abfd, it->files)) {
				header = profile.get_header();
				samples.add(profile, abfd, it->app_image, i);
				found = true;
			}
		}
	}

	if (found == true && ip.error == image_ok) {
		image_error error;
		string filename =
			samples.extra_found_images.find_image_path(
				ip.image, error, true);
		check_mtime(filename, header);
	}

	if (has_debug_info)
		*has_debug_info = abfd.has_debug_info();
}
