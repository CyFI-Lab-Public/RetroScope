/**
 * @file filename_spec.h
 * Container holding a sample filename split into its components
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#ifndef FILENAME_SPEC_H
#define FILENAME_SPEC_H

#include <unistd.h>
#include <string>

#include "generic_spec.h"

class profile_spec;
class extra_images;

/**
 * A class to split and store components of a sample filename.
 * These derived values are then used to match against a
 * profile_spec as given by the user.
 */
class filename_spec
{
	friend class profile_spec;

public:
	/**
	 * @param filename  the samples filename
	 * @param extra  extra binary image location
	 *
	 * build a filename_spec from a samples filename
	 */
	filename_spec(std::string const & filename,
		      extra_images const & extra);

	filename_spec();

	/**
	 * @param filename  a sample filename
	 * @param extra  extra binary image location
	 *
	 * setup filename spec according to the samples filename. PP:3.19 to
	 * 3.25
	 */
	void set_sample_filename(std::string const & filename,
				 extra_images const & extra);

	/**
	 * @param rhs  right hand side of the match operator
	 * @param binary  if binary is non-empty, and matches
	 * the binary or lib name, use it rather than the
	 * one in rhs.
	 *
	 * return true if *this match rhs, matching if:
	 *  - image_name are identical
	 *  - lib_name are identical
	 *  - event_spec match
	 *
	 * This operation is not commutative. First part of PP:3.24
	 */
	bool match(filename_spec const & rhs,
	           std::string const & binary) const;

	bool is_dependent() const;

private:
	std::string image;
	std::string lib_image;
	std::string cg_image;
	std::string event;
	int count;
	unsigned int unitmask;
	generic_spec<pid_t> tgid;
	generic_spec<pid_t> tid;
	generic_spec<int> cpu;
};


#endif /* !FILENAME_SPEC_H */
