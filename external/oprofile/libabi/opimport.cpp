/**
 * @file opimport.cpp
 * Import sample files from other ABI
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Graydon Hoare
 */

#include "abi.h"
#include "odb.h"
#include "popt_options.h"
#include "op_sample_file.h"

#include <fstream>
#include <iostream>
#include <vector>
#include <cassert>
#include <cstring>
#include <cstdlib>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <cstdlib>
#include <cstring>

using namespace std;

namespace {
	string output_filename;
	string abi_filename;
	bool verbose;
	bool force;
};


popt::option options_array[] = {
	popt::option(verbose, "verbose", 'V', "verbose output"),
	popt::option(output_filename, "output", 'o', "output to file", "filename"),
	popt::option(abi_filename, "abi", 'a', "abi description", "filename"),
	popt::option(force, "force", 'f', "force conversion, even if identical")
};


struct extractor {

	abi const & theabi;

	unsigned char const * begin;
	unsigned char const * end;
	bool little_endian;

	explicit
	extractor(abi const & a, unsigned char const * src, size_t len)
		: theabi(a), begin(src), end(src + len) {
		little_endian = theabi.need(string("little_endian")) == 1;
		if (verbose) {
			cerr << "source byte order is: "
			     << string(little_endian ? "little" : "big")
			     << " endian" << endl;
		}
	}

	template <typename T>
	void extract(T & targ, void const * src_,
	             char const * sz, char const * off);
};


template <typename T>
void extractor::extract(T & targ, void const * src_,
                        char const * sz, char const * off)
{
	unsigned char const * src = static_cast<unsigned char const *>(src_)
		+ theabi.need(off);
	size_t nbytes = theabi.need(sz);

	targ = 0;
	if (nbytes == 0)
		return;
	
	assert(nbytes <= sizeof(T));
	assert(src >= begin);
	assert(src + nbytes <= end);
	
	if (verbose)
		cerr << hex << "get " << sz << " = " << nbytes
		     << " bytes @ " << off << " = " << (src - begin)
		     << " : ";

	if (little_endian)
		while(nbytes--)
			targ = (targ << 8) | src[nbytes];
	else
		for(size_t i = 0; i < nbytes; ++i)
			targ = (targ << 8) | src[i];
	
	if (verbose)
		cerr << " = " << targ << endl;
}


void import_from_abi(abi const & abi, void const * srcv,
                     size_t len, odb_t * dest) throw (abi_exception)
{
	struct opd_header * head =
		static_cast<opd_header *>(odb_get_data(dest));
	unsigned char const * src = static_cast<unsigned char const *>(srcv);
	unsigned char const * const begin = src;
	extractor ext(abi, src, len);	

	memcpy(head->magic, src + abi.need("offsetof_header_magic"), 4);

	// begin extracting opd header
	ext.extract(head->version, src, "sizeof_u32", "offsetof_header_version");
	ext.extract(head->cpu_type, src, "sizeof_u32", "offsetof_header_cpu_type");
	ext.extract(head->ctr_event, src, "sizeof_u32", "offsetof_header_ctr_event");
	ext.extract(head->ctr_um, src, "sizeof_u32", "offsetof_header_ctr_um");
	ext.extract(head->ctr_count, src, "sizeof_u32", "offsetof_header_ctr_count");
	ext.extract(head->is_kernel, src, "sizeof_u32", "offsetof_header_is_kernel");
	// "double" extraction is unlikely to work
	head->cpu_speed = 0.0;
	ext.extract(head->mtime, src, "sizeof_time_t", "offsetof_header_mtime");
	ext.extract(head->cg_to_is_kernel, src, "sizeof_u32",
		"offsetof_header_cg_to_is_kernel");
	ext.extract(head->anon_start, src, "sizeof_u32",
		"offsetof_header_anon_start");
	ext.extract(head->cg_to_anon_start, src, "sizeof_u32",
		"offsetof_header_cg_to_anon_start");
	src += abi.need("sizeof_struct_opd_header");
	// done extracting opd header

	// begin extracting necessary parts of descr
	odb_node_nr_t node_nr;
	ext.extract(node_nr, src, "sizeof_odb_node_nr_t", "offsetof_descr_current_size");
	src += abi.need("sizeof_odb_descr_t");
	// done extracting descr

	// skip node zero, it is reserved and contains nothing usefull
	src += abi.need("sizeof_odb_node_t");

	// begin extracting nodes
	unsigned int step = abi.need("sizeof_odb_node_t");
	if (verbose)
		cerr << "extracting " << node_nr << " nodes of " << step << " bytes each " << endl;

	assert(src + (node_nr * step) <= begin + len);

	for (odb_node_nr_t i = 1 ; i < node_nr ; ++i, src += step) {
		odb_key_t key;
		odb_value_t val;
		ext.extract(key, src, "sizeof_odb_key_t", "offsetof_node_key");
		ext.extract(val, src, "sizeof_odb_value_t", "offsetof_node_value");
		int rc = odb_add_node(dest, key, val);
		if (rc != EXIT_SUCCESS) {
			cerr << strerror(rc) << endl;
			exit(EXIT_FAILURE);
		}
	}
	// done extracting nodes
}


int main(int argc, char const ** argv)
{

	vector<string> inputs;
	popt::parse_options(argc, argv, inputs);

	if (inputs.size() != 1) {
		cerr << "error: must specify exactly 1 input file" << endl;
		exit(1);
	}

	abi current_abi, input_abi;

	{
		ifstream abi_file(abi_filename.c_str());
		if (!abi_file) {
			cerr << "error: cannot open abi file "
			     << abi_filename << endl;
			exit(1);
		}
		abi_file >> input_abi;
	}

	if (!force && current_abi == input_abi) {
		cerr << "input abi is identical to native. "
		     << "no conversion necessary." << endl;
		exit(1);
	}

	int in_fd;
	struct stat statb;
	void * in;
	odb_t dest;
	int rc;

	in_fd = open(inputs[0].c_str(), O_RDONLY);
	assert(in_fd > 0);
	rc = fstat(in_fd, &statb);
	assert(rc == 0);
	in = mmap(0, statb.st_size, PROT_READ, MAP_PRIVATE, in_fd, 0);
	assert(in != (void *)-1);

	rc = odb_open(&dest, output_filename.c_str(), ODB_RDWR,
		      sizeof(struct opd_header));
	if (rc) {
		cerr << "odb_open() fail:\n"
		     << strerror(rc) << endl;
		exit(EXIT_FAILURE);
	}

	try {
		import_from_abi(input_abi, in, statb.st_size, &dest);
	} catch (abi_exception & e) {
		cerr << "caught abi exception: " << e.desc << endl;
	}

	odb_close(&dest);

	rc = munmap(in, statb.st_size);
	assert(rc == 0);
}
