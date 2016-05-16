/**
 * @file glob_filter.h
 * Filter strings based on globbed exclude/include list
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef GLOB_FILTER_H
#define GLOB_FILTER_H

#include "string_filter.h"

/**
 * glob_filter - filtering of a string based on globbed include/exclude list
 *
 * This class is an oracle on whether a particular string matches
 * the given list of included and excluded strings.
 *
 * This class gives glob-based matches on each pattern, as with fnmatch(3)
 */
class glob_filter : public string_filter {
public:
	/**
	 * Initialise the filter with the include and exclude list, 
	 * comma-separated.
	 */
	glob_filter(std::string const & include_patterns,
	            std::string const & exclude_patterns)
		: string_filter(include_patterns, exclude_patterns) {}

	/**
	 * Initialise the filter with the include and exclude list.
	 */
	glob_filter(std::vector<std::string> const & include_patterns,
	            std::vector<std::string> const & exclude_patterns)
		: string_filter(include_patterns, exclude_patterns) {}

	/// Returns true if the given string matches
	virtual bool match(std::string const & str) const;

protected:

	/// function object for fnmatching
	struct fnmatcher {
		fnmatcher(std::string const & str) : str_(str) {}

		bool operator()(std::string const & s);

		std::string const & str_;
	};
};

#endif /* GLOB_FILTER_H */
