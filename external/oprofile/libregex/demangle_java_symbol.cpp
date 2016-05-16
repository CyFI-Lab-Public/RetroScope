/**
 * @file demangle_java_symbol.cpp
 * Demangle a java symbol
 *
 * @remark Copyright 2007 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#include "demangle_java_symbol.h"

#include <algorithm>

using namespace std;

namespace {

/**
 * The grammar we implement:
 *
 * field_type:
 *    base_type | object_type | array_type
 * base_type:
 *    B | C | D | F | I | J | S | Z
 * object_type:
 *    L<classname>;
 * array_type:
 *    [field_type
 * method_descriptor:
 *    ( field_type* ) return_descriptor
 * return_descriptor:
 *    field_type | V
 * method_signature:
 *    object_type method_name method_descriptor
 *
 */

bool array_type(string & result,
	string::const_iterator & begin, string::const_iterator end);
bool object_type(string & result,
	string::const_iterator & begin, string::const_iterator end);


bool base_type(string & result,
	string::const_iterator & begin, string::const_iterator end)
{
	bool ret = true;

	if (begin == end)
		return false;

	switch (*begin) {
	case 'B': result += "byte";    break;
	case 'C': result += "char";    break;
	case 'D': result += "double";  break;
	case 'F': result += "float";   break;
	case 'I': result += "int";     break;
	case 'J': result += "long";    break;
	case 'S': result += "short";   break;
	case 'Z': result += "boolean"; break;
	default:  ret = false;         break;
	}

	if (ret)
		++begin;
	return ret;
}


bool field_type(string & result,
	string::const_iterator & begin, string::const_iterator end)
{
	if (base_type(result, begin, end))
		return true;

	if (object_type(result, begin, end))
		return true;

	if (array_type(result, begin, end))
		return true;

	return false;
}


bool array_type(string & result,
	string::const_iterator & begin, string::const_iterator end)
{
	if (begin == end || *begin != '[')
		return false;

	++begin;
	if (field_type(result, begin, end)) {
		result += "[]";
		return true;
	}

	return false;
}


bool list_of_field_type(string & result,
	string::const_iterator & begin, string::const_iterator end)
{
	bool first = false;
	while (begin != end) {
		if (first)
			result += ", ";

		if (!field_type(result, begin, end))
			return false;

		first = true;
	}

	return true;
}


bool return_descriptor(string & result,
	string::const_iterator & begin, string::const_iterator end)
{
	if (begin == end)
		return false;
	if (*begin == 'V') {
		++begin;
		result = "void " + result;
		return true;
	}

	string temp;
	if (!field_type(temp, begin, end))
		return false;
	result = temp + " " + result;

	return true;
}


bool method_descriptor(string & result,
	string::const_iterator & begin, string::const_iterator end)
{
	if (begin == end || *begin != '(')
		return false;
	++begin;
	string::const_iterator pos = find(begin, end, ')');
	if (pos == end)
		return false;

	result += "(";

	if (!list_of_field_type(result, begin, pos))
		return false;

	if (begin == end || *begin != ')')
		return false;

	++begin;

	if (!return_descriptor(result, begin, end))
		return false;

	result += ')';

	return true;
}


bool methode_name(string & result,
	string::const_iterator & begin, string::const_iterator end)
{
	if (begin == end)
		return false;

	string::const_iterator pos = find(begin, end, '(');
	if (pos == end)
		return false;

	result += '.' +  string(begin, pos);
	begin = pos;

	return true;
}


bool object_type(string & result,
	string::const_iterator & begin, string::const_iterator end)
{
	if (begin == end || *begin != 'L')
		return false;
	string::const_iterator pos = find(begin, end, ';');
	if (pos == end)
		return false;

	string temp = string(begin + 1, pos);
	replace(temp.begin(), temp.end(), '/', '.');
	result += temp;

	begin = pos + 1;

	return true;
}


string demangle_symbol(string::const_iterator begin,
		       string::const_iterator end)
{
	string result;

	if (!object_type(result, begin, end))
		return string();

	if (!methode_name(result, begin, end))
		return string();

	if (!method_descriptor(result, begin, end))
		return string();

	if (begin != end) {
		if (*begin == '~') {
			// special case for disambiguated symbol.
			result += string(begin, end);
		} else {
			return string();
		}
	}

	return result;
}

} // anonymous namespace


string const demangle_java_symbol(string const & name)
{
	return demangle_symbol(name.begin(), name.end());
}
