/**
 * @file daemon/opd_mangling.c
 * Mangling and opening of sample files
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <sys/types.h>
 
#include "opd_mangling.h"
#include "opd_kernel.h"
#include "opd_cookie.h"
#include "opd_sfile.h"
#include "opd_anon.h"
#include "opd_printf.h"
#include "opd_events.h"
#include "oprofiled.h"

#include "op_file.h"
#include "op_sample_file.h"
#include "op_config.h"
#include "op_mangle.h"
#include "op_events.h"
#include "op_libiberty.h"

#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>


static char const * get_dep_name(struct sfile const * sf)
{
	if (sf->anon)
		return find_cookie(sf->app_cookie);

	/* avoid to call find_cookie(), caller can recover using image_name */
	if (sf->cookie == sf->app_cookie)
		return NULL;

	if (!separate_kernel && !(separate_lib && !sf->kernel))
		return NULL;

	/* this will fail if e.g. kernel thread */
	if (sf->app_cookie == 0)
		return NULL;

	return find_cookie(sf->app_cookie);
}


static char * mangle_anon(struct anon_mapping const * anon)
{
	char * name = xmalloc(PATH_MAX);

	snprintf(name, 1024, "%u.0x%llx.0x%llx", (unsigned int)anon->tgid,
	       anon->start, anon->end);

	return name;
}


static char *
mangle_filename(struct sfile * last, struct sfile const * sf, int counter, int cg)
{
	char * mangled;
	struct mangle_values values;
	struct opd_event * event = find_counter_event(counter);

	values.flags = 0;

	if (sf->kernel) {
		values.image_name = sf->kernel->name;
		values.flags |= MANGLE_KERNEL;
	} else if (sf->anon) {
		values.flags |= MANGLE_ANON;
		values.image_name = mangle_anon(sf->anon);
		values.anon_name = sf->anon->name;
	} else {
		values.image_name = find_cookie(sf->cookie);
	}

	values.dep_name = get_dep_name(sf);
	if (!values.dep_name)
		values.dep_name = values.image_name;
 
	/* FIXME: log */
	if (!values.image_name || !values.dep_name)
		return NULL;

	if (separate_thread) {
		values.flags |= MANGLE_TGID | MANGLE_TID;
		values.tid = sf->tid;
		values.tgid = sf->tgid;
	}
 
	if (separate_cpu) {
		values.flags |= MANGLE_CPU;
		values.cpu = sf->cpu;
	}

	if (cg) {
		values.flags |= MANGLE_CALLGRAPH;
		if (last->kernel) {
			values.cg_image_name = last->kernel->name;
		} else if (last->anon) {
			values.flags |= MANGLE_CG_ANON;
			values.cg_image_name = mangle_anon(last->anon);
			values.anon_name = last->anon->name;
		} else {
			values.cg_image_name = find_cookie(last->cookie);
		}

		/* FIXME: log */
		if (!values.cg_image_name) {
			if (values.flags & MANGLE_ANON)
				free((char *)values.image_name);
			return NULL;
		}
	}

	values.event_name = event->name;
	values.count = event->count;
	values.unit_mask = event->um;

	mangled = op_mangle_filename(&values);

	if (values.flags & MANGLE_ANON)
		free((char *)values.image_name);
	if (values.flags & MANGLE_CG_ANON)
		free((char *)values.cg_image_name);
	return mangled;
}


int opd_open_sample_file(odb_t *file, struct sfile *last,
                         struct sfile * sf, int counter, int cg)
{
	char * mangled;
	char const * binary;
	int spu_profile = 0;
	vma_t last_start = 0;
	int err;

	mangled = mangle_filename(last, sf, counter, cg);

	if (!mangled)
		return EINVAL;

	verbprintf(vsfile, "Opening \"%s\"\n", mangled);

	create_path(mangled);

	/* locking sf will lock associated cg files too */
	sfile_get(sf);
	if (sf != last)
		sfile_get(last);

retry:
	err = odb_open(file, mangled, ODB_RDWR, sizeof(struct opd_header));

	/* This can naturally happen when racing against opcontrol --reset. */
	if (err) {
		if (err == EMFILE) {
			if (sfile_lru_clear()) {
				printf("LRU cleared but odb_open() fails for %s.\n", mangled);
				abort();
			}
			goto retry;
		}

		fprintf(stderr, "oprofiled: open of %s failed: %s\n",
		        mangled, strerror(err));
		goto out;
	}

	if (!sf->kernel)
		binary = find_cookie(sf->cookie);
	else
		binary = sf->kernel->name;

	if (last && last->anon)
		last_start = last->anon->start;

	if (sf->embedded_offset != UNUSED_EMBEDDED_OFFSET)
		spu_profile = 1;

	fill_header(odb_get_data(file), counter,
		    sf->anon ? sf->anon->start : 0, last_start,
		    !!sf->kernel, last ? !!last->kernel : 0,
		    spu_profile, sf->embedded_offset,
		    binary ? op_get_mtime(binary) : 0);

out:
	sfile_put(sf);
	if (sf != last)
		sfile_put(last);
	free(mangled);
	return err;
}
