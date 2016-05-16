/**
 * @file growable_vector.h
 * Auto-expanding vector type
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef GROWABLE_VECTOR_H
#define GROWABLE_VECTOR_H

#include <vector>
#include <algorithm>
#include <functional>

/**
 * A simple growable vector template.
 */
template <typename T> class growable_vector {
public:
	typedef std::vector<T> container_type;
	typedef typename container_type::size_type size_type;


	/**
	 * Index into the vector for a value. An out of
	 * bounds index will return a default-constructed value.
	 */
	T operator[](size_type index) const {
		if (index >= container.size())
			return T();
		return container[index];
	}


	/**
	 * Index into the vector for a value. If the index is larger than
	 * the current max index, the array is expanded, default-filling
	 * any intermediary gaps.
	 */
	T & operator[](size_type index) {
		if (index >= container.size())
			container.resize(index + 1);
		return container[index];
	}


	/**
	 * vectorized += operator
	 */
	growable_vector<T> & operator+=(growable_vector<T> const & rhs) {
		if (rhs.container.size() > container.size())
			container.resize(rhs.container.size());

		size_type min_size = min(container.size(), rhs.container.size());
		for (size_type i = 0 ; i < min_size; ++i)
			container[i] += rhs.container[i];

		return *this;
	}


	/**
	 * vectorized -= operator, overflow shouldn't occur during substraction
	 * (iow: for each components lhs[i] >= rhs[i]
	 */
	growable_vector<T> & operator-=(growable_vector<T> const & rhs) {
		if (rhs.container.size() > container.size())
			container.resize(rhs.container.size());

		size_type min_size = min(container.size(), rhs.container.size());
		for (size_type i = 0 ; i < min_size; ++i)
			container[i] -= rhs.container[i];

		return *this;
	}


	/// return current size of vector
	size_type size() const {
		return container.size();
	}


	/// fill container with given value
	void fill(size_type size, T const & value) {
		container.resize(size, value);
	}


	/// return true if all elements have the default constructed value
	bool zero() const {
		return std::find_if(container.begin(), container.end(),
	                            std::bind2nd(std::not_equal_to<T>(), T()))
					== container.end();
	}

private:
	container_type container;
};

#endif // GROWABLE_VECTOR_H
