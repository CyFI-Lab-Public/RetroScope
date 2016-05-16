/**
 * @file daemon/opd_mangling.h
 * Mangling and opening of sample files
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OPD_MANGLING_H
#define OPD_MANGLING_H

#include "odb.h"

struct sfile;

/*
 * opd_open_sample_file - open a sample file
 * @param sf  sfile to open sample file for
 * @param counter  counter number
 * @param cg if this is a callgraph file
 *
 * Open image sample file for the sfile, counter
 * counter and set up memory mappings for it.
 *
 * Returns 0 on success.
 */
int opd_open_sample_file(odb_t *file, struct sfile *last,
                         struct sfile * sf, int counter, int cg);

#endif /* OPD_MANGLING_H */
