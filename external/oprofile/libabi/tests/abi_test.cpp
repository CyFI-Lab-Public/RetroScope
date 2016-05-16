/**
 * @file abi_test.cpp
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
#include "op_cpu_type.h"
#include "op_config.h"

#include <fstream>
#include <iostream>

#include <cstdlib>
#include <cstring>

using namespace std;

namespace {
	string db_filename;
	string abi_filename;
}


popt::option options_array[] = {
	popt::option(db_filename, "db", 'd', "output db to file", "filename"),
	popt::option(abi_filename, "abi", 'a', "output abi to file", "filename")
};


int main(int argc, char const ** argv)
{
	vector<string> rest;
	popt::parse_options(argc, argv, rest);

	if (abi_filename.empty() && db_filename.empty()) {
		cerr << "error: no file specified to work on" << endl;
		exit(1);
	}


	if (!abi_filename.empty()) {
		ofstream file(abi_filename.c_str());
		if (!file) {
			cerr << "error: cannot open " << abi_filename
			     << " for writing" << endl;
			exit(1);
		}
		file << abi();
	}

	if (!db_filename.empty()) {
		odb_t dest;
		int rc = odb_open(&dest, db_filename.c_str(), ODB_RDWR,
		                  sizeof(struct opd_header));

		if (rc) {
			cerr << "odb_open() fail:\n"
			     << strerror(rc) << endl;
			exit(EXIT_FAILURE);
		}

		struct opd_header * header;
		header = static_cast<struct opd_header *>(odb_get_data(&dest));
		memset(header, '\0', sizeof(struct opd_header));
		header->version = OPD_VERSION;
		memcpy(header->magic, OPD_MAGIC, sizeof(header->magic));
		header->is_kernel = 1;
		/* ICACHE_FETCHES */
		header->ctr_event = 0x80;
		header->ctr_um = 0x0;
		header->cpu_type = CPU_ATHLON;
		header->ctr_count = 0xdeadbeef;
		header->cpu_speed = 0;
		header->mtime = 1034790063;
		header->cg_to_is_kernel = 1;
		header->anon_start = 0;
		header->cg_to_anon_start = 0;
    
		for (int i = 0; i < 3793; ++i) {
			int rc = odb_add_node(&dest, i, i);
			if (rc != EXIT_SUCCESS) {
				cerr << strerror(rc) << endl;
				exit(EXIT_FAILURE);
			}
		}
		odb_close(&dest);
	}
}
