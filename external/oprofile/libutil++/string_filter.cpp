/**
 * @file string_filter.cpp
 * Filter strings based on exclude/include list
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include <algorithm>

#include "string_filter.h"
#include "string_manip.h"

using namespace std;


string_filter::string_filter(string const & include_patterns,
                             string const & exclude_patterns)
	: include(separate_token(include_patterns, ',')),
	  exclude(separate_token(exclude_patterns, ','))
{
}


string_filter::string_filter(vector<string> const & include_patterns,
                             vector<string> const & exclude_patterns)
	: include(include_patterns),
	exclude(exclude_patterns)
{
}


// FIXME: PP reference
bool string_filter::match(string const & str) const
{
	vector<string>::const_iterator cit;
	cit = find(exclude.begin(), exclude.end(), str);
	if (cit != exclude.end())
		return false;

	cit = find(include.begin(), include.end(), str);
	if (include.empty() || cit != include.end())
		return true;

	return false;
}
