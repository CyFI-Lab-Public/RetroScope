/**
 * @file opd_image.c
 * Management of binary images
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include "opd_image.h"
#include "opd_printf.h"
#include "opd_sample_files.h"
#include "opd_24_stats.h"
#include "oprofiled.h"

#include "op_file.h"
#include "op_config_24.h"
#include "op_libiberty.h"
#include "op_string.h"

#include <string.h>
#include <stdlib.h>
#include <stdio.h>

/* maintained for statistics purpose only */
static int nr_images;

/* list of images */
#define OPD_IMAGE_HASH_SIZE 2048
static struct list_head opd_images[OPD_IMAGE_HASH_SIZE];


void opd_init_images(void)
{
	int i;
	for (i = 0; i < OPD_IMAGE_HASH_SIZE; ++i)
		list_init(&opd_images[i]);
}


int opd_get_nr_images(void)
{
	return nr_images;
}


void opd_delete_image(struct opd_image * image)
{
	verbprintf(vmisc, "Deleting image: name %s app_name %s, kernel %d, "
	           "tid %d, tgid %d ref count %u\n",
	           image->name, image->app_name, image->kernel,
	           image->tid, image->tgid, (int)image->ref_count);

	if (image->ref_count <= 0) {
		printf("image->ref_count < 0 for image: name %s app_name %s, "
		       "kernel %d, tid %d, tgid %d ref count %u\n",
		       image->name, image->app_name, image->kernel,
		       image->tid, image->tgid, image->ref_count);
		abort();
	}

	if (--image->ref_count != 0)
		return;

	if (image->name)
		free(image->name);
	if (image->app_name)
		free(image->app_name);
	list_del(&image->hash_next);
	opd_close_image_samples_files(image);
	free(image);

	nr_images--;
}


void opd_for_each_image(opd_image_cb image_cb)
{
	struct list_head * pos;
	struct list_head * pos2;
	int i;

	for (i = 0; i < OPD_IMAGE_HASH_SIZE; ++i) {
		list_for_each_safe(pos, pos2, &opd_images[i]) {
			struct opd_image * image =
				list_entry(pos, struct opd_image, hash_next);
			image_cb(image);
		}
	}
}
 

/**
 * opd_hash_image - hash an image
 * @param hash  hash of image name
 * @param tid  thread id
 * @param tgid  thread group id
 *
 * return the hash code for the passed parameters
 */
static size_t opd_hash_image(char const * name, pid_t tid, pid_t tgid)
{
	size_t hash = op_hash_string(name);
	if (separate_thread)
		hash += tid + tgid;
	return  hash % OPD_IMAGE_HASH_SIZE;
}


/**
 * opd_new_image - create an image sample file
 * @param app_name  the application name where belongs this image
 * @param name  name of the image to add
 * @param kernel  is the image a kernel/module image
 * @param tid  thread id
 * @param tgid  thread group id
 *
 * image at funtion entry is uninitialised
 * name is copied i.e. should be GC'd separately from the
 * image structure if appropriate.
 *
 * Initialise an opd_image struct for the image image
 * without opening the associated samples files. At return
 * the image is fully initialized.
 */
static struct opd_image *
opd_new_image(char const * name, char const * app_name, int kernel,
              pid_t tid, pid_t tgid)
{
	size_t hash_image;
	struct opd_image * image;

	verbprintf(vmisc, "Creating image: %s %s, kernel %d, tid %d, "
	           "tgid %d\n", name, app_name, kernel, tid, tgid);

	image = xmalloc(sizeof(struct opd_image));

	list_init(&image->hash_next);
	image->name = xstrdup(name);
	image->kernel = kernel;
	image->tid = tid;
	image->tgid = tgid;
	image->ref_count = 0;
	image->app_name = app_name ? xstrdup(app_name) : NULL;
	image->mtime = op_get_mtime(image->name);

	image->ignored = 1;
	if (separate_lib && app_name)
		image->ignored = is_image_ignored(app_name);
	if (image->ignored)
		image->ignored = is_image_ignored(name);

	memset(image->sfiles, '\0', NR_CPUS * sizeof(struct opd_24_sfile **));

	hash_image = opd_hash_image(name, tid, tgid);
	list_add(&image->hash_next, &opd_images[hash_image]);

	nr_images++;

	return image;
}


/**
 * is_same_image - check for identical image
 * @param image  image to compare
 * @param name  name of image
 * @param app_name image must belong to this application name
 * @param tid  thread id
 * @param tgid  thread group id
 *
 * on entry caller have checked than strcmp(image->name, name) == 0
 * return 0 if the couple (name, app_name) refers to same image
 */
static int is_same_image(struct opd_image const * image, char const * app_name,
                         pid_t tid, pid_t tgid)
{
	/* correctness is really important here, if we fail to recognize
	 * identical image we will open/mmap multiple time the same samples
	 * files which is not supported by the kernel, strange assertion
	 * failure in libfd is a typical symptom of that */

	if (separate_thread) {
		if (image->tid != tid || image->tgid != tgid)
			return 1;
	}

	/* if !separate_lib, the comparison made by caller is enough */
	if (!separate_lib)
		return 0;

	if (image->app_name == NULL && app_name == NULL)
		return 0;

	if (image->app_name != NULL && app_name != NULL &&
	    !strcmp(image->app_name, app_name))
		return 0;

	/* /proc parsed image come with a non null app_name but notification
	 * for application itself come with a null app_name, in this case
	 * the test above fail so check for this case. */
	if (image->app_name && !app_name && !strcmp(image->app_name, image->name))
		return 0;

	return 1;
}


/**
 * opd_find_image - find an image
 * @param name  name of image to find
 * @param hash  hash of image to find
 * @param app_name  the application name where belongs this image
 * @param tid  thread id
 * @param tgid  thread group id
 *
 * Returns the image pointer for the file specified by name, or %NULL.
 */
static struct opd_image * opd_find_image(char const * name, 
                                char const * app_name, pid_t tid, pid_t tgid)
{
	/* suppress uninitialized use warning */
	struct opd_image * image = 0;
	struct list_head * pos;
	size_t bucket;

	opd_24_stats[OPD_IMAGE_HASH_ACCESS]++;
	bucket = opd_hash_image(name, tid, tgid);
	list_for_each(pos, &opd_images[bucket]) {
		opd_24_stats[OPD_IMAGE_HASH_DEPTH]++;
		image = list_entry(pos, struct opd_image, hash_next);

		if (!strcmp(image->name, name)) {
			if (!is_same_image(image, app_name, tid, tgid))
				break;
		}
	}

	if (pos == &opd_images[bucket])
		return NULL;

	/* The app_name field is always valid */
	return image;
}

 
struct opd_image * opd_get_image(char const * name, char const * app_name,
                                 int kernel, pid_t tid, pid_t tgid)
{
	struct opd_image * image;
	if ((image = opd_find_image(name, app_name, tid, tgid)) == NULL)
		image = opd_new_image(name, app_name, kernel, tid, tgid);

	return image;
}


struct opd_image * opd_get_kernel_image(char const * name,
                               char const * app_name, pid_t tid, pid_t tgid)
{
	return opd_get_image(name, app_name, 1, tid, tgid);
}
