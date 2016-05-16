/**
 * @file abi.h
 *
 * Contains internal ABI management class
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Graydon Hoare
 */

#ifndef OPROF_ABI_H
#define OPROF_ABI_H
 
#include <string>
#include <map>
#include <iosfwd>

struct abi_exception : std::exception {
	std::string const desc;
 
	explicit abi_exception(std::string const d);
 
	~abi_exception() throw() {}
};


class abi {
public:
	abi();

	int need(std::string const key) const throw (abi_exception);

	bool operator==(abi const & other) const;
	friend std::ostream & operator<<(std::ostream & o, abi const & abi);
	friend std::istream & operator>>(std::istream & i, abi & abi);

private:
	std::map<std::string, int> slots;
};

#endif // OPROF_ABI_H
