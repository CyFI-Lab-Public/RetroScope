/**
 * @file string_manip.cpp
 * std::string helpers
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#include <sstream>
#include <iomanip>

#include <cstdlib>
#include <cmath>

#include "string_manip.h"

using namespace std;


string erase_to_last_of(string const & str, char ch)
{
	string result = str;
	string::size_type pos = result.find_last_of(ch);
	if (pos != string::npos)
		result.erase(0, pos + 1);

	return result;
}


string split(string & s, char c)
{
	string::size_type i = s.find_first_of(c);
	if (i == string::npos)
		return string();

	string const tail = s.substr(i + 1);
	s = s.substr(0, i);
	return tail;
}


bool is_prefix(string const & s, string const & prefix)
{
	// gcc 2.95 and below don't have this
	// return s.compare(0, prefix.length(), prefix) == 0;
	return s.find(prefix) == 0;
}


vector<string> separate_token(string const & str, char sep)
{
	vector<string> result;
	string next;

	for (size_t pos = 0 ; pos != str.length() ; ++pos) {
		char ch = str[pos];
		if (ch == '\\') {
			if (pos < str.length() - 1 && str[pos + 1] == sep) {
				++pos;
				next += sep;
			} else {
				next += '\\';
			}
		} else if (ch == sep) {
			result.push_back(next);
			// some stl lacks string::clear()
			next.erase(next.begin(), next.end());
		} else {
			next += ch;
		}
	}

	if (!next.empty())
		result.push_back(next);

	return result;
}


string ltrim(string const & str, string const & totrim)
{
	string result(str);

	return result.erase(0, result.find_first_not_of(totrim));
}


string rtrim(string const & str, string const & totrim)
{
	string result(str);

	return result.erase(result.find_last_not_of(totrim) + 1);
}


string trim(string const & str, string const & totrim)
{
	return rtrim(ltrim(str, totrim), totrim);
}


string const
format_percent(double value, size_t int_width, size_t fract_width, bool showpos)
{
	ostringstream os;

	if (value == 0.0)
		return string(int_width + fract_width, ' ') + "0";

	if (showpos)
		os.setf(ios::showpos);

	if (fabs(value) > .001) {
		// os << fixed << value unsupported by gcc 2.95
		os.setf(ios::fixed, ios::floatfield);
		os << setw(int_width + fract_width + 1)
		   << setprecision(fract_width) << value;
	} else {
		// os << scientific << value unsupported by gcc 2.95
		os.setf(ios::scientific, ios::floatfield);
		os << setw(int_width + fract_width + 1)
		   // - 3 to count exponent part
		   << setprecision(fract_width - 3) << value;
	}

	string formatted = os.str();
	if (is_prefix(formatted, "100."))
		formatted.erase(formatted.size() - 1);
	return formatted;
}


template <>
unsigned int op_lexical_cast<unsigned int, string>(string const & str)
{
	char* endptr;

	// 2.91.66 fix
	unsigned long ret = 0;
	ret = strtoul(str.c_str(), &endptr, 0);
	if (*endptr)
		throw invalid_argument("op_lexical_cast(\""+ str +"\")");
	return ret;
}
