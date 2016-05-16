/**
 * @file path_filter.cpp
 * Filter paths based on globbed exclude/include list
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include <fnmatch.h>

#include <algorithm>

#include "path_filter.h"
#include "string_manip.h"
#include "file_manip.h"

using namespace std;

bool path_filter::match(string const & str) const
{
	vector<string>::const_iterator cit;

	// first, if any component of the dir is listed in exclude -> no
	string comp = op_dirname(str);
	while (!comp.empty() && comp != "/") {
		cit = find_if(exclude.begin(), exclude.end(),
			fnmatcher(op_basename(comp)));
		if (cit != exclude.end())
			return false;

		// dirname("foo") == "foo"
		if (comp == op_dirname(comp))
			break;
		comp = op_dirname(comp);
	}

	string const base = op_basename(str);

	// now if the file name is specifically excluded -> no
	cit = find_if(exclude.begin(), exclude.end(), fnmatcher(base));
	if (cit != exclude.end())
		return false;

	// now if the file name is specifically included -> yes
	cit = find_if(include.begin(), include.end(), fnmatcher(base));
	if (cit != include.end())
		return true;

	// now if any component of the path is included -> yes
	// note that the include pattern defaults to '*'
	string compi = op_dirname(str);
	while (!compi.empty() && compi != "/") {
		cit = find_if(include.begin(), include.end(),
			fnmatcher(op_basename(compi)));
		if (cit != include.end())
			return true;

		// dirname("foo") == "foo"
		if (compi == op_dirname(compi))
			break;
		compi = op_dirname(compi);
	}

	return include.empty();
}
