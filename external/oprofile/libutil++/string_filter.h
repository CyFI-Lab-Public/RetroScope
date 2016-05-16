/**
 * @file string_filter.h
 * Filter strings based on exclude/include list
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef STRING_FILTER_H
#define STRING_FILTER_H

#include <string>
#include <vector>

/**
 * string_filter - filtering of a string based on include/exclude list
 *
 * This class is an oracle on whether a particular string matches
 * the given list of included and excluded strings.
 *
 * This base class gives a default exact-match semantics.
 */
class string_filter {
public:
	string_filter() {}

	/**
	 * Initialise the filter with the include and exclude list,
	 * comma-separated.
	 */
	string_filter(std::string const & include_patterns,
	              std::string const & exclude_patterns);

	/**
	 * Initialise the filter with the include and exclude list.
	 */
	string_filter(std::vector<std::string> const & include_patterns,
	              std::vector<std::string> const & exclude_patterns);

	virtual ~string_filter() {}

	/// Returns true if the given string matches
	virtual bool match(std::string const & str) const;

protected:
	/// include patterns
	std::vector<std::string> include;
	/// exclude patterns
	std::vector<std::string> exclude;
};

#endif /* STRING_FILTER_H */
