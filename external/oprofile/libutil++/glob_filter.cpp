/**
 * @file glob_filter.cpp
 * Filter strings based on globbed exclude/include list
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include <fnmatch.h>

#include <algorithm>

#include "glob_filter.h"
#include "string_manip.h"

using namespace std;

bool glob_filter::fnmatcher::operator()(string const & s)
{
	return fnmatch(s.c_str(), str_.c_str(), 0) != FNM_NOMATCH;
}


bool glob_filter::match(string const & str) const
{
	vector<string>::const_iterator cit;
	cit = find_if(exclude.begin(), exclude.end(), fnmatcher(str));
	if (cit != exclude.end())
		return false;

	cit = find_if(include.begin(), include.end(), fnmatcher(str));
	if (include.empty() || cit != include.end())
		return true;

	return false;
}
