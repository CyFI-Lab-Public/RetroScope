/**
 * @file opd_image.h
 * Management of binary images
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OPD_IMAGE_H
#define OPD_IMAGE_H

#include "op_list.h"
#include "op_config_24.h"
#include "op_types.h"

#include <time.h>

struct opd_24_sfile;

/**
 * A binary (library, application, kernel or module)
 * is represented by a struct opd_image.
 */
struct opd_image {
	/** used by container of opd_images */
	struct list_head hash_next;
	/** how many time this opd_image is referenced */
	int ref_count;
	/** all samples files belonging to this image */
	struct opd_24_sfile ** sfiles[NR_CPUS];
	/** name of this image */
	char * name;
	/** the application name where belongs this image, NULL if image has
	 * no owner (such as vmlinux or module) */
	char * app_name;
	/** thread id, on 2.2 kernel always == to tgid */
	pid_t tid;
	/** thread group id  */
	pid_t tgid;
	/** time of last modification */
	time_t mtime;
	/** kernel image or not */
	int kernel;
	/** non zero if this image must be profiled */
	int ignored;
};

/** callback function passed to opd_for_each_image() */
typedef void (*opd_image_cb)(struct opd_image *);

/**
 * @param imagecb callback to apply onto each existing image struct
 *
 * the callback receive a struct opd_image * (not a const struct) and is
 * allowed to freeze the image struct itself.
 */
void opd_for_each_image(opd_image_cb imagecb);

/**
 * initialize opd_image container
 */
void opd_init_images(void);

/**
 * @param image  the image pointer
 *
 * Decrement reference count of image, if reference count is zero flush and
 * close the samples files then freeze all memory belonging to this image.
 */
void opd_delete_image(struct opd_image * image);

/**
 * opd_get_kernel_image - get a kernel image
 * @param name of image
 * @param app_name application owner of this kernel image. non-null only
 *  when separate_kernel_sample != 0
 * @param tid  thread id
 * @param tgid  thread group id
 *
 * Create and initialise an image adding it to the image lists and to image
 * hash list. Note than at creation reference count is zero, it's caller
 * responsabilities to incr this count.
 */
struct opd_image * opd_get_kernel_image(char const * name,
     char const * app_name, pid_t tid, pid_t tgid);

/**
 * opd_get_image - get an image from the image structure
 * @param name  name of image
 * @param app_name  the application name where belongs this image
 * @param kernel  is the image a kernel/module image
 * @param tid  thread id
 * @param tgid  thread group id
 *
 * Get the image specified by the file name name from the
 * image structure. If it is not present, the image is
 * added to the structure. In either case, the image number
 * is returned.
 */
struct opd_image * opd_get_image(char const * name, char const * app_name,
                                 int kernel, pid_t tid, pid_t tgid);

/**
 * opd_get_nr_images - return number of images
 */
int opd_get_nr_images(void);

#endif /* OPD_IMAGE_H */
