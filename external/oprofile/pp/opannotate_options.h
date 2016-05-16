/**
 * @file opannotate_options.h
 * Options for opannotate tool
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OPANNOTATE_OPTIONS_H
#define OPANNOTATE_OPTIONS_H

#include <string>
#include <vector>

#include "common_option.h"
#include "path_filter.h"

class profile_classes;

namespace options {
	extern demangle_type demangle;
	extern bool source;
	extern bool assembly;
	extern string_filter symbol_filter;
	extern path_filter file_filter;
	extern std::string output_dir;
	extern std::vector<std::string> search_dirs;
	extern std::vector<std::string> base_dirs;
	extern std::vector<std::string> objdump_params;
	extern double threshold;
}

/// classes of sample filenames to handle
extern profile_classes classes;

/**
 * handle_options - process command line
 * @param spec  profile specification
 *
 * Process the spec, fatally complaining on error.
 */
void handle_options(options::spec const & spec);

#endif // OPANNOTATE_OPTIONS_H
