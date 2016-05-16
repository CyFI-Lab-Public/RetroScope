/**
 * @file opreport_options.h
 * Options for opreport tool
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OPREPORT_OPTIONS_H
#define OPREPORT_OPTIONS_H

#include <string>
#include <vector>
#include <iosfwd>

#include "common_option.h"
#include "string_filter.h"
#include "symbol_sort.h"

class profile_classes;
class merge_option;

namespace options {
	extern demangle_type demangle;
	extern bool symbols;
	extern bool callgraph;
	extern bool debug_info;
	extern bool details;
	extern bool reverse_sort;
	extern bool exclude_dependent;
	extern sort_options sort_by;
	extern merge_option merge_by;
	extern bool global_percent;
	extern bool long_filenames;
	extern bool show_address;
	extern string_filter symbol_filter;
	extern bool show_header;
	extern bool accumulated;
	extern bool xml;
	extern std::string xml_options;
}

/// All the chosen sample files.
extern profile_classes classes;
extern profile_classes classes2;

/**
 * handle_options - process command line
 * @param spec  profile specification
 *
 * Process the spec, fatally complaining on error.
 */
void handle_options(options::spec const & spec);

#endif // OPREPORT_OPTIONS_H
