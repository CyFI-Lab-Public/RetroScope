/**
 * @file string_manip.h
 * std::string helpers
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef STRING_MANIP_H
#define STRING_MANIP_H

#include <string>
#include <vector>
#include <sstream>
#include <stdexcept>

/**
 * @param str string
 * @param ch the characterto search
 *
 * erase char from the begin of str to the last
 * occurence of ch from and return the string
 */
std::string erase_to_last_of(std::string const & str, char ch);

/// split string s by first occurence of char c, returning the second part.
/// s is set to the first part. Neither include the split character
std::string split(std::string & s, char c);

/// return true if "prefix" is a prefix of "s", behavior is undefined
/// if prefix is an empty string
bool is_prefix(std::string const & s, std::string const & prefix);

/**
 * @param str the string to tokenize
 * @param sep the separator_char
 *
 * separate fields in a string in a list of token; field are
 * separated by the sep character, sep char can be escaped
 * by '\\' to specify a sep char in a token, '\\' not followed
 * by a sep is taken as it e.g. "\,\a" --> ",\a"
 */
std::vector<std::string> separate_token(std::string const & str, char sep);

/// remove trim chars from start of input string return the new string
std::string ltrim(std::string const & str, std::string const & totrim = "\t ");
/// remove trim chars from end of input string return the new string
std::string rtrim(std::string const & str, std::string const & totrim = "\t ");
/// ltrim(rtrim(str))
std::string trim(std::string const & str, std::string const & totrim = "\t ");

/**
 * format_percent - smart format of double percentage value
 * @param value - the value
 * @param int_width - the maximum integer integer width default to 2
 * @param frac_width - the fractionnary width default to 4
 * @param showpos - show + sign for positive values
 *
 * This formats a percentage into exactly the given width and returns
 * it. If the integer part is larger than the given int_width, the
 * returned string will be wider. The returned string is never
 * shorter than (fract_with + int_width + 1)
 *
 */
std::string const
format_percent(double value, size_t int_width,
               size_t frac_width, bool showpos = false);

/// prefered width to format percentage
static unsigned int const percent_int_width = 2;
static unsigned int const percent_fract_width = 4;
static unsigned int const percent_width = percent_int_width + percent_fract_width + 1;


/**
 * @param src  input parameter
 * convert From src to a T through an istringstream.
 *
 * Throws invalid_argument if conversion fail.
 *
 * Note that this is not as foolproof as boost's lexical_cast
 */
template <typename To, typename From>
To op_lexical_cast(From const & src)
{
	std::ostringstream in;
	if (!(in << src))
		throw std::invalid_argument("op_lexical_cast<T>()");
	std::istringstream out(in.str());
	To value;
	if (!(out >> value)) {
		throw std::invalid_argument("op_lexical_cast<T>(\"" +
		    in.str() +"\")");
	}
	return value;
}

// specialization accepting hexadecimal and octal number in input. Note that
// op_lexical_cast<unsigned int>("0x23"); will fail because it call the
// non specialized template.
template <>
unsigned int op_lexical_cast<unsigned int>(std::string const & str);

#endif /* !STRING_MANIP_H */
