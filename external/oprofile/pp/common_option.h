/**
 * @file common_option.h
 * Declaration of entry point of pp tools, implementation file add common
 * options of pp tools and some miscelleaneous functions
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef COMMON_OPTION_H
#define COMMON_OPTION_H

#include <vector>
#include <list>

#include "arrange_profiles.h"
#include "demangle_symbol.h"

namespace options {
	extern bool verbose;
	extern double threshold;
	extern std::string threshold_opt;
	extern std::string command_options;
	extern std::vector<std::string> image_path;
	extern std::string root_path;

	struct spec {
		std::list<std::string> common;
		std::list<std::string> first;
		std::list<std::string> second;
	};
}

/**
 * prototype of a pp tool entry point. This entry point is called
 * by run_pp_tool
 */
typedef int (*pp_fct_run_t)(options::spec const & spec);

/**
 * @param argc  command line number of argument
 * @param argv  command line argument pointer array
 * @param fct  function to run to start this pp tool
 *
 * Provide a common entry to all pp tools, parsing all options, handling
 * common options and providing the necessary try catch clause
 */
int run_pp_tool(int argc, char const * argv[], pp_fct_run_t fct);

/**
 * @param option one of [smart,none,normal]
 *
 * return the demangle_type of option or throw an exception if option
 * is not valid.
 */
demangle_type handle_demangle_option(std::string const & option);

/**
 * @param mergespec  merge option
 * @param allow_lib  is merge)lib allowed in mergespec
 * @param exclude_dependent user specified --exclude-dependent
 *
 * parse merge option and return a merge_option filled from it.
 * 
 */
merge_option handle_merge_option(std::vector<std::string> const & mergespec,
       bool allow_lib, bool exclude_dependent);

#endif /* !COMMON_OPTION_H */
