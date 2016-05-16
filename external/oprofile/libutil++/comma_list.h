/**
 * @file comma_list.h
 * Container holding items from a list of comma separated items
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#ifndef COMMA_LIST_H
#define COMMA_LIST_H

#include <string>
#include <vector>

#include "string_manip.h"

/**
 * hold a list of item of type T, tracking also if item has been set.
 */
template <class T>
class comma_list
{
public:
	comma_list();

	/**
	 * @param str  list of comma separated item
	 *
	 * setup items array according to str parameters. Implement PP:3.17
	 * w/o restriction on charset and with the special string all which
	 * match anything.
	 */
	void set(std::string const & str);

	/// return true if a specific value is held by this container
	bool is_set() const {
		return !is_all;
	}

	/**
	 * @param value  the value to test
	 *
	 * return true if value match one the stored value in items
	 */
	bool match(T const & value) const;

private:
	typedef T value_type;
	typedef std::vector<value_type> container_type;
	typedef typename container_type::const_iterator const_iterator;
	bool is_all;
	container_type items;
};


template <class T>
comma_list<T>::comma_list()
	: is_all(true)
{
}


template <class T>
void comma_list<T>::set(std::string const & str)
{
	items.clear();

	is_all = false;

	std::vector<std::string> result = separate_token(str, ',');
	for (size_t i = 0 ; i < result.size() ; ++i) {
		if (result[i] == "all") {
			is_all = true;
			items.clear();
			break;
		}
		items.push_back(op_lexical_cast<T>(result[i]));
	}
}


template <class T>
bool comma_list<T>::match(T const & value) const
{
	if (is_all)
		return true;

	const_iterator cit = items.begin();
	const_iterator const end = items.end();

	for (; cit != end; ++cit) {
		if (value == *cit)
			return true;
	}

	return false;
}


#endif /* !COMMA_LIST_H */
