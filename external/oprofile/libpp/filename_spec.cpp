/**
 * @file filename_spec.cpp
 * Container holding a sample filename split into its components
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#include <string>

#include "filename_spec.h"
#include "parse_filename.h"
#include "generic_spec.h"
#include "locate_images.h"


using namespace std;


filename_spec::filename_spec(string const & filename,
			     extra_images const & extra)
{
	set_sample_filename(filename, extra);
}


filename_spec::filename_spec()
	: image("*"), lib_image("*")
{
}


bool filename_spec::match(filename_spec const & rhs,
                          string const & binary) const
{
	if (!tid.match(rhs.tid) || !cpu.match(rhs.cpu) ||
	    !tgid.match(rhs.tgid) || count != rhs.count ||
	    unitmask != rhs.unitmask || event != rhs.event) {
		return false;
	}

	if (binary.empty())
		return image == rhs.image && lib_image == rhs.lib_image;

	// PP:3.3 if binary is not empty we must match either the
	// lib_name if present or the image name
	if (!rhs.lib_image.empty()) {
		// FIXME: use fnmatch ?
		return rhs.lib_image == binary;
	}

	// FIXME: use fnmatch ?
	return rhs.image == binary;
}


void filename_spec::set_sample_filename(string const & filename,
	extra_images const & extra)
{
	parsed_filename parsed = parse_filename(filename, extra);

	image = parsed.image;
	lib_image = parsed.lib_image;
	cg_image = parsed.cg_image;
	event = parsed.event;
	count = op_lexical_cast<int>(parsed.count);
	unitmask = op_lexical_cast<unsigned int>(parsed.unitmask);
	tgid.set(parsed.tgid);
	tid.set(parsed.tid);
	cpu.set(parsed.cpu);
}


bool filename_spec::is_dependent() const
{
	if (cg_image.empty())
		return image != lib_image;
	return cg_image != image || cg_image != lib_image;
}
