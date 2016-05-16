/**
 * @file libpp/populate_for_spu.cpp
 * Fill up a profile_container from inverted profiles for
 * a Cell BE SPU profile
 *
 * @remark Copyright 2007 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Maynard Johnson
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

static int spu_profile = unknown_profile;

/*
 * On Cell Broadband Engine, an application executing on an SPE may
 * have been loaded from a separate SPU executable binary file or may
 * have been loaded from an embedded section of a PPE application or
 * shared library.  In the embedded case, the embedding file may actually
 * contain multiple SPU images, resulting in different SPU images being loaded
 * onto different SPUs.  Thus, the SPUs may be executing different code, even
 * though the application of the parent PPE process is the same.  Therefore,
 * we must be sure to create a separate op_bfd object for each SPU.  When doing
 * so below, we examine header.embedded_offset.  If embedded_offset is > 0, it's
 * interpreted as the offset of an SPU image embedded in the containing file,
 * so the filename to do the check_mtime on is the containing file, ip.image;
 * otherwise, the filename to do the check_mtime on is the separate backing
 * file of the SPU image, abfd->filename.
 */
void
populate_spu_profile_from_files(list<profile_sample_files> const & files,
				string const app_image,
				profile_container & samples,
				inverted_profile const & ip,
				string_filter const & symbol_filter,
				size_t ip_grp_num, bool * has_debug_info)
{
	string archive_path = samples.extra_found_images.get_archive_path();
	bool ok = ip.error == image_ok;
	op_bfd * abfd = NULL;
	string fname_to_check;
	list<profile_sample_files>::const_iterator it = files.begin();
	list<profile_sample_files>::const_iterator const end = files.end();
	for (; it != end; ++it) {
		profile_t profile;
		if (it->sample_filename.empty())
			continue;

		profile.add_sample_file(it->sample_filename);
		opd_header header = profile.get_header();
		if (header.embedded_offset) {
			abfd = new op_bfd(header.embedded_offset,
					  ip.image,
					  symbol_filter,
					  samples.extra_found_images,
					  ok);
			fname_to_check = ip.image;
		} else {
			abfd = new op_bfd(ip.image,
					  symbol_filter,
					  samples.extra_found_images,
					  ok);
			fname_to_check = abfd->get_filename();
		}
		profile.set_offset(*abfd);
		if (!ok && ip.error == image_ok)
			ip.error = image_format_failure;

		if (ip.error == image_format_failure)
			report_image_error(ip, false,
					   samples.extra_found_images);

		samples.add(profile, *abfd, app_image, ip_grp_num);
		if (ip.error == image_ok) {
			image_error error;
			string filename =
				samples.extra_found_images.find_image_path(
					fname_to_check, error, true);
			check_mtime(filename, profile.get_header());
		}

		if (has_debug_info && !*has_debug_info)
			*has_debug_info = abfd->has_debug_info();
		delete abfd;
	}
}
}  // anon namespace

void
populate_for_spu_image(profile_container & samples,
		       inverted_profile const & ip,
		       string_filter const & symbol_filter,
		       bool * has_debug_info)
{

	for (size_t i = 0; i < ip.groups.size(); ++i) {
		list < image_set >::const_iterator it=
			ip.groups[i].begin();
		list < image_set >::const_iterator const end
			= ip.groups[i].end();

		for (; it != end; ++it)
			populate_spu_profile_from_files(it->files,
				it->app_image, samples, ip,
				symbol_filter, i, has_debug_info);
	}
}

bool is_spu_profile(inverted_profile const & ip)
{
	bool retval = false;
	string sfname = "";
	if (spu_profile != unknown_profile)
		return spu_profile;

	if (!ip.groups.size())
		return false;

	for (size_t i = 0; i < ip.groups.size(); ++i) {
		list<image_set>::const_iterator grp_it
			= ip.groups[i].begin();
		list<image_set>::const_iterator const grp_end
			= ip.groups[i].end();

		for (; grp_it != grp_end; ++grp_it) {
			list<profile_sample_files>::const_iterator sfiles_it =
				grp_it->files.begin();
			list<profile_sample_files>::const_iterator sfiles_end =
				grp_it->files.end();
			for (; sfiles_it != sfiles_end; ++sfiles_it) {
				if (!sfiles_it->sample_filename.empty()) {
					sfname = sfiles_it->sample_filename;
					goto do_check;
				}
			}
		}
	}
	goto out;

do_check:
	spu_profile = profile_t::is_spu_sample_file(sfname);

	if (spu_profile == cell_spu_profile)
		retval = true;

out:
	return retval;
}


