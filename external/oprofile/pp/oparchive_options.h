/**
 * @file oparchive_options.h
 * Options for oparchive tool
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Will Cohen
 * @author Philippe Elie
 */

#ifndef OPARCHIVE_OPTIONS_H
#define OPARCHIVE_OPTIONS_H

#include "common_option.h"

class profile_classes;
class merge_option;

namespace options {
	extern bool exclude_dependent;
	extern merge_option merge_by;
	extern std::string outdirectory;
	extern bool list_files;
}

/// All the chosen sample files.
extern profile_classes classes;
extern std::list<std::string> sample_files;

/**
 * handle_options - process command line
 * @param spec  profile specification
 *
 * Process the spec, fatally complaining on error.
 */
void handle_options(options::spec const & spec);

#endif // OPARCHIVE_OPTIONS_H
