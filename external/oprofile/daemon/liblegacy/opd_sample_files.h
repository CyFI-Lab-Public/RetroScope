/**
 * @file opd_sample_files.h
 * Management of sample files
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OPD_SAMPLE_FILES_H
#define OPD_SAMPLE_FILES_H

#include "op_list.h"
#include "odb.h"

struct opd_image;

/** one samples file when profiling on a 2.2/2.4 kernel */
struct opd_24_sfile {
	/** lru list of sample file */
	struct list_head lru_next;
	/** the sample file itself */
	odb_t sample_file;
};

/**
 * sync all samples files
 */
void opd_sync_samples_files(void);

/**
 * @param image  the image pointer to work on
 *
 * close all samples files belonging to this image
 */
void opd_close_image_samples_files(struct opd_image * image);

/**
 * opd_open_24_sample_file - open an image sample file
 * @param image  image to open file for
 * @param counter  counter number
 * @param cpu_nr  cpu number
 *
 * Open image sample file for the image, counter
 * counter and set up memory mappings for it.
 * image->kernel and image->name must have meaningful
 * values.
 *
 * Returns 0 on success.
 */
int opd_open_24_sample_file(struct opd_image * image, int counter, int cpu_nr);

/**
 * @param sfile  sample file to act on
 *
 * put sfile at the head of samples files lru list
 */
void opd_24_sfile_lru(struct opd_24_sfile * sfile);


#endif /* OPD_SAMPLE_FILES_H */
