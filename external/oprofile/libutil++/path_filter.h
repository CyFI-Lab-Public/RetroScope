/**
 * @file path_filter.h
 * Filter paths based on globbed exclude/include list
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef PATH_FILTER_H
#define PATH_FILTER_H

#include "glob_filter.h"

/**
 * path_filter - filtering of a string based on globbed include/exclude list
 *
 * This class is an oracle on whether a particular string matches
 * the given list of included and excluded strings.
 *
 * This class gives glob-based matches on each pattern, as with fnmatch(3),
 * where each component of the candidate path is considered separately.
 */
class path_filter : public glob_filter {
public:
	/**
	 * Initialise the filter with the include and exclude list,
	 * comma-separated.
	 */
	path_filter(std::string const & include_patterns = std::string(),
	            std::string const & exclude_patterns = std::string())
		: glob_filter(include_patterns, exclude_patterns) {}

	/**
	 * Initialise the filter with the include and exclude list.
	 */
	path_filter(std::vector<std::string> const & include_patterns,
	            std::vector<std::string> const & exclude_patterns)
		: glob_filter(include_patterns, exclude_patterns) {}


	/// Returns true if the given string matches
	virtual bool match(std::string const & str) const;
};

#endif /* PATH_FILTER_H */
