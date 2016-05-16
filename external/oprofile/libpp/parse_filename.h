/**
 * @file parse_filename.h
 * Split a sample filename into its constituent parts
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#ifndef PARSE_FILENAME_H
#define PARSE_FILENAME_H

#include <string>

class extra_images;

/**
 * a convenience class to store result of parse_filename()
 */
struct parsed_filename
{
	std::string image;
	std::string lib_image;
	/// destination image for call graph file, empty if this sample
	/// file is not a callgraph file.
	std::string cg_image;
	std::string event;
	std::string count;
	std::string unitmask;
	std::string tgid;
	std::string tid;
	std::string cpu;

	/// return true if the profile specification are identical.
	bool profile_spec_equal(parsed_filename const & parsed);

	/**
	 * the original sample filename from which the
	 * above components are built
	 */
	std::string filename;
	bool jit_dumpfile_exists;
};


/// debugging helper
std::ostream & operator<<(std::ostream &, parsed_filename const &);


/**
 * parse a sample filename
 * @param filename in: a sample filename
 * @param extra_found_images binary image location
 *
 * filename is split into constituent parts, the lib_image is optional
 * and can be empty on successfull call. All other error are fatal.
 * Filenames are encoded as according to PP:3.19 to PP:3.25
 *
 * all errors throw an std::invalid_argument exception
 */
parsed_filename parse_filename(std::string const & filename,
			       extra_images const & extra_found_images);

#endif /* !PARSE_FILENAME_H */
