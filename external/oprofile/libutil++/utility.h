/**
 * @file utility.h
 * General purpose C++ utility
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 * @author John Levon
 */

#ifndef UTILITY_H
#define UTILITY_H

#include <cstddef>

/** noncopyable : object of class derived from this class can't be copied
 * and isn't copy-constructible */
class noncopyable {
protected:
	noncopyable() {}
	~noncopyable() {}
private:
	noncopyable(noncopyable const &);
	noncopyable const & operator=(noncopyable const &);
};


template<typename T> class scoped_ptr {
public:
	explicit scoped_ptr(T * p = 0) : p_(p) {}
	~scoped_ptr() { delete p_; }

	void reset(T * p = 0) {
		if (p == p_)
			return;
		delete p_;
		p_ = p;
	}
 
	T & operator*() const { return *p_; }
	T * operator->() const { return p_; }
	T * get() const { return p_; }
 
	void swap(scoped_ptr & sp) {
		T * tmp = sp.p_;
		sp.p_ = p_;
		p_ = tmp;
	}
 
private:
	scoped_ptr & operator=(scoped_ptr const &);
	scoped_ptr(scoped_ptr const &);
	T * p_;
};

template<typename T> class scoped_array {
public:
	explicit scoped_array(T * p = 0) : p_(p) {}
	~scoped_array() { delete [] p_; }

	void reset(T * p = 0) {
		if (p == p_)
			return;
		delete [] p_;
		p_ = p;
	}
 
	T & operator[](std::ptrdiff_t i) const { return p_[i]; }
	T * get() const { return p_; }
 
	void swap(scoped_array & sp) {
		T * tmp = sp.p_;
		sp.p_ = p_;
		p_ = tmp;
	}
 
private:
	scoped_array & operator=(scoped_array const &);
	scoped_array(scoped_array const &);
	T * p_;
};

/**
 * @param count
 * @param total
 *
 * return total == 0 ? 1.0 : (count / total);
 */
inline double op_ratio(double count, double total)
{
	return total == 0 ? 0.0 : (count / total);
}
 
// Part copyright:
//  (C) Copyright boost.org 1999. Permission to copy, use, modify, sell
//  and distribute this software is granted provided this copyright
//  notice appears in all copies. This software is provided "as is" without
//  express or implied warranty, and with no claim as to its suitability for
//  any purpose.

#endif /* !UTILITY_H */
