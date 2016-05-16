/**
 * @file cverb.cpp
 * verbose output stream
 *
 * @remark Copyright 2002, 2004  OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include <cstring>

#include <fstream>
#include <iostream>
#include <map>
#include <string>
#include <cstring>

#include "cverb.h"

using namespace std;

cverb_object cverb;
verbose vlevel1("level1");
verbose vdebug("debug");
verbose vstats("stats");
verbose vsfile("sfile");
verbose vxml("xml");

namespace {

// The right way is to use: ofstream fout; but cverb(fout.rdbuf()) receive
// a null pointer and stl shipped with 2.91 segfault.
ofstream fout("/dev/null");
ostream null_stream(fout.rdbuf());

// Used to setup the bad bit in our null stream so output will fail earlier
// and overhead will be smaller.
struct setup_stream {
	setup_stream();
};

setup_stream::setup_stream()
{
	null_stream.clear(ios::badbit);
}

setup_stream setup;

// We use a multimap because user can create multiple verbose object with
// the same name, these are synonymous, setting up one to true will setup
// all with the same name to true.
typedef multimap<string, verbose *> recorder_t;
// The recorder is lazilly created by verbose object ctor
static recorder_t * object_map;

} // anonymous namespace


verbose::verbose(char const * name)
	:
	set(false)
{
	// all params is treated a part, there is no need to create a
	// verbose all("all"); it's meaningless. "all" verbose named object is
	// reserved.
	if (strcmp(name, "all") == 0)
		return;
	if (!object_map)
		object_map = new recorder_t;
	object_map->insert(recorder_t::value_type(name, this));
}


verbose verbose::operator|(verbose const & rhs)
{
	verbose result(*this);
	result.set = result.set || rhs.set;
	return result;
}


verbose verbose::operator&(verbose const & rhs)
{
	verbose result(*this);
	result.set = result.set && rhs.set;
	return result;
}


bool verbose::setup(string const & name)
{
	if (name == "all") {
		null_stream.rdbuf(cout.rdbuf());
		null_stream.clear();
		return true;
	}
	if (!object_map)
		object_map = new recorder_t;
	pair<recorder_t::iterator, recorder_t::iterator> p_it =
		object_map->equal_range(name);
	if (p_it.first == p_it.second)
		return false;
	for (; p_it.first != p_it.second; ++p_it.first)
		p_it.first->second->set = true;
	return true;
}


bool verbose::setup(vector<string> const & names)
{
	for (size_t i = 0; i < names.size(); ++i)
		if (!setup(names[i]))
			return false;
	return true;
}


ostream& operator<<(cverb_object &, verbose const & v)
{
	return v.set ? cout : null_stream;
}
