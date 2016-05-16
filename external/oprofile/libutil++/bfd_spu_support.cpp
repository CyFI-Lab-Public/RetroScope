/**
 * @file libutil++/bfd_spu_support.cpp
 * Special BFD functions for processing a Cell BE SPU profile
 *
 * @remark Copyright 2007 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Maynard Johnson
 * (C) Copyright IBM Corporation 2007
 */

#include "bfd_support.h"
#include "op_bfd.h"
#include "config.h"
#include "cverb.h"

#include <stdlib.h>
#include <stdio.h>
#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <cstring>
#include <sys/types.h>

struct spu_elf {
	FILE * stream;
	off_t spu_offset;
};

using namespace std;

extern verbose vbfd;

#ifdef HAVE_BFD_OPENR_IOVEC_WITH_7PARMS

namespace {

static void *
spu_bfd_iovec_open(bfd * nbfd, void * open_closure)
{
	/* Checking nbfd isn't really necessary, except to silence
	 * compile warning.  In fact, nbfd will always be non-NULL.
	 */
	if (nbfd)
		return open_closure;
	else
		return NULL;
}

static int
spu_bfd_iovec_close(bfd * nbfd, void * stream)
{
	spu_elf * my_stream = (spu_elf *) stream;

	fclose(my_stream->stream);
	free(my_stream);
	/* Checking nbfd isn't really necessary, except to silence
	 * compile warning.  In fact, nbfd will always be non-NULL.
	 */
	if (nbfd)
		return 1;
	else
		return 0;
}

static file_ptr
spu_bfd_iovec_pread(bfd * abfd, void * stream, void * buf,
		    file_ptr nbytes, file_ptr offset)
{
	spu_elf * my_stream = (spu_elf *) stream;
	fseek(my_stream->stream, my_stream->spu_offset + offset,
	      SEEK_SET);
	nbytes = fread(buf, sizeof(char), nbytes, my_stream->stream);
	/* Checking abfd isn't really necessary, except to silence
	 * compile warning.  In fact, abfd will always be non-NULL.
	 */
	if (abfd)
		return nbytes;
	else
		return 0;
}
} // namespace anon
#endif

bfd *
spu_open_bfd(string const name, int fd, uint64_t offset_to_spu_elf)
{

	bfd * nbfd = NULL;
	spu_elf * spu_elf_stream = (spu_elf *)malloc(sizeof(spu_elf));

	FILE * fp = fdopen(fd, "r");
	spu_elf_stream->stream = fp;
	spu_elf_stream->spu_offset = offset_to_spu_elf;
#ifdef HAVE_BFD_OPENR_IOVEC_WITH_7PARMS
	nbfd = bfd_openr_iovec(strdup(name.c_str()), "elf32-spu",
			       spu_bfd_iovec_open, spu_elf_stream,
			       spu_bfd_iovec_pread, spu_bfd_iovec_close, NULL);
#else
	ostringstream os;
	os << "Attempt to process a Cell Broadband Engine SPU profile without"
	   << "proper BFD support.\n"
	   << "Rebuild the opreport utility with the correct BFD library.\n"
	   << "See the OProfile user manual for more information.\n";
	throw op_runtime_error(os.str());
#endif
	if (!nbfd) {
		cverb << vbfd << "spu_open_bfd failed for " << name << endl;
		return NULL;
	}

	bfd_check_format(nbfd, bfd_object);

	return nbfd;
}
