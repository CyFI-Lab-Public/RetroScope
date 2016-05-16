/**
 * @file cached_value.h
 * Hold a cached value.
 *
 * @remark Copyright 2005 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 */

#ifndef CACHED_VALUE_H
#define CACHED_VALUE_H

#include "op_exception.h"

/**
 * Hold a single value, returning a cached value if there is one.
 */
template <class T>
class cached_value
{
public:
	cached_value() : set(false) {}

	typedef T value_type;

	/// return the cached value
	value_type const get() const {
		if (!set)
			throw op_fatal_error("cached value not set");
		return value;
	}

	/// return true if a value is cached
	bool cached() const { return set; }

	/// set the contained value
	value_type const reset(value_type const & val) {
		value = val;
		set = true;
		return value;
	}

private:
	/// the cached value
	value_type value;
	/// is the value valid?
	bool set;
};


#endif /* !CACHED_VALUE_H */
