/**
 * @file utility_tests.cpp
 * tests utility.h and op_exception.h
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <stdlib.h>
#include <new>
#include <iostream>

#include "utility.h"
#include "op_exception.h"

using namespace std;

static int nb_new;
static int nb_new_array;

void* operator new(size_t size) throw(bad_alloc)
{
	nb_new++;
	return malloc(size);
}

void* operator new[](size_t size) throw(bad_alloc)
{
	nb_new_array++;
	return malloc(size);
}

void operator delete(void * p) throw()
{
	nb_new--;
	if (p)
		free(p);
}

void operator delete[](void * p) throw()
{
	nb_new_array--;
	if (p)
		free(p);
}


void check_alloc()
{
	if (nb_new) {
		cerr << "new(size_t) leak\n";
		exit(EXIT_FAILURE);
	}

	if (nb_new_array) {
		cerr << "new[](size_t) leak\n";
		exit(EXIT_FAILURE);
	}
}


struct A {};

template <typename Throw, typename Catch>
void throw_tests()
{
	scoped_ptr<A> a(new A);
	try {
		scoped_ptr<A> a(new A);
		throw Throw("");
	}
	catch (Catch const &) {
	}
}


template <typename Throw, typename Catch>
void throw_tests(bool)
{
	scoped_array<A> b(new A[10]);
	try {
		scoped_array<A> a(new A[10]);
		throw Throw("");
	}
	catch (Catch const &) {
	}
}


void tests_new()
{
	throw_tests<op_fatal_error, op_fatal_error>();
	throw_tests<op_fatal_error, op_exception>();
	throw_tests<op_runtime_error, op_runtime_error>();
	throw_tests<op_runtime_error, runtime_error>();
	throw_tests<op_fatal_error, op_fatal_error>(true);
	throw_tests<op_fatal_error, op_exception>(true);
	throw_tests<op_runtime_error, op_runtime_error>(true);
	throw_tests<op_runtime_error, runtime_error>(true);
}


int main()
{
	try {
		tests_new();
		check_alloc();
	}
	catch (...) {
		cerr << "unknown exception\n";
		return EXIT_FAILURE;
	}

	return EXIT_SUCCESS;
}
