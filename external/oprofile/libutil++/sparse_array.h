/**
 * @file sparse_array.h
 * Auto-expanding sparse array type
 *
 * @remark Copyright 2007 OProfile authors
 * @remark Copyright (c) International Business Machines, 2007.
 * @remark Read the file COPYING
 *
 * @author Dave Nomura <dcnltc@us.ibm.com>
 */

#ifndef SPARSE_ARRAY_H
#define SPARSE_ARRAY_H

template <typename I, typename T> class sparse_array {
public:
	typedef std::map<I, T> container_type;
	typedef typename container_type::size_type size_type;

	/**
	 * Index into the map for a value.
	 * NOTE: since std::map does/can not have a const member function for
	 * operator[], this const member function simply returns 0 for 
	 * profile classes that aren't represented in the map.
	 * This member function will only be invoked for queries of the
	 * sparse array.
	 */
	T operator[](size_type index) const {
		typename container_type::const_iterator it = container.find(index);
		if (it != container.end())
			return it->second;
		else
			return 0;
	}


	/**
	 * Index into the vector for a value. If the index is larger than
	 * the current max index, a new array entry is created.
	 */
	T & operator[](size_type index) {
		return container[index];
	}


	/**
	 * vectorized += operator
	 */
	sparse_array & operator+=(sparse_array const & rhs) {
		typename container_type::const_iterator it = rhs.container.begin();
		typename container_type::const_iterator it_end = rhs.container.end();
		for ( ; it != it_end; it++)
			container[it->first] += it->second;

		return *this;
	}


	/**
	 * vectorized -= operator, overflow shouldn't occur during substraction
	 * (iow: for each components lhs[i] >= rhs[i]
	 */
	sparse_array & operator-=(sparse_array const & rhs) {
		typename container_type::const_iterator it = rhs.container.begin();
		typename container_type::const_iterator it_end = rhs.container.end();
		for ( ; it != it_end; it++)
			container[it->first] -= it->second;

		return *this;
	}


	/**
	 * return the maximum index of the array + 1 or 0 if the array
	 * is empty.
	 */
	size_type size() const {
		if (container.size() == 0)
			return 0;
		typename container_type::const_iterator last = container.end();
		--last;
		return last->first + 1;
	}


	/// return true if all elements have the default constructed value
	bool zero() const {
		typename container_type::const_iterator it = container.begin();
		typename container_type::const_iterator it_end = container.end();
		for ( ; it != it_end; it++)
			if (it->second != 0)
				return false;
		return true;
	}

private:
	container_type container;
};

#endif // SPARSE_ARRAY_H
