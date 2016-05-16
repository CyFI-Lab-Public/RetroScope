/**
 * @file abi.cpp
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Graydon Hoare
 * @author John Levon
 * @author Philippe Elie
 */

#include "abi.h"
#include "op_abi.h"
#include "odb.h"
#include "op_sample_file.h"

#include <iostream>
#include <cassert>

using namespace std;

typedef map<string, int> abi_map;
typedef abi_map::const_iterator abi_iter;

abi_exception::abi_exception(string const d) : desc(d) {}


abi::abi()
{
	op_abi_entry const * entry = get_abi();
	for ( ; entry->name != 0; ++entry)
		slots[entry->name] = entry->offset;

	slots["little_endian"] = op_little_endian();
}


int abi::need(string const key) const throw (abi_exception)
{
	if (slots.find(key) != slots.end())
		return slots.find(key)->second;
	else
		throw abi_exception(string("missing ABI key: ") + key);
}


bool abi::operator==(abi const & other) const
{
	return slots == other.slots;
}


ostream & operator<<(ostream & o, abi const & abi)
{
	abi_iter i = abi.slots.begin();
	abi_iter e = abi.slots.end();

	for (; i != e; ++i)
		o << i->first << " " << i->second << endl;

	return o;
}


istream & operator>>(istream & i, abi & abi)
{
	string key;
	int val;
	abi.slots.clear();

	while(i >> key >> val)
		abi.slots[key] = val;

	return i;
}
