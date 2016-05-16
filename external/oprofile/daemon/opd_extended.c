/**
 * @file opd_extended.c
 * OProfile Extended Feature
 *
 * @remark Copyright 2007-2009 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Suravee Suthikulpanit <suravee.suthikulpanit@amd.com>
 * Copyright (c) 2009 Advanced Micro Devices, Inc.
 */

#include "opd_extended.h"
#include "op_string.h"

#include <string.h>
#include <stdio.h>

/* This global variable is >= 0
 * if extended feature is enabled */
static int opd_ext_feat_index;

extern struct opd_ext_handlers ibs_handlers;

/**
 * OProfile Extended Feature Table
 *
 * This table contains a list of extended features.
 */
static struct opd_ext_feature ext_feature_table[] = {
	{"ibs", &ibs_handlers },
	{ NULL, NULL }
};


static int get_index_for_feature(char const * name)
{
	int ret = -1;
	unsigned int i;

	if(!name)
		return ret;

	for (i = 0 ; ext_feature_table[i].feature != NULL ; i++ ) {
		if(!strncmp(name, ext_feature_table[i].feature,
			strlen(ext_feature_table[i].feature))) {
			ret = i;
			break;
		}
	}

	return ret;
}


static inline int is_ext_enabled()
{
	if (opd_ext_feat_index >= 0
	&& ext_feature_table[opd_ext_feat_index].handlers != NULL)
		return 1;
	else
		return 0;
}


static inline int is_ext_sfile_enabled()
{
	if (opd_ext_feat_index >= 0
	&& ext_feature_table[opd_ext_feat_index].handlers != NULL
	&& ext_feature_table[opd_ext_feat_index].handlers->ext_sfile != NULL)
		return 1;
	else
		return 0;
}


/**
 * Param "value" is the input from CML option with the format:
 *
 * <feature name>:<param1>:<param2>:<param3>:.....
 *
 * where param1,2.3,..n are optional.
 */
int opd_ext_initialize(char const * value)
{
	int ret = EXIT_FAILURE;
	char * tmp = NULL, * name = NULL, * args = NULL;

	if(!value) {
		opd_ext_feat_index = -1;
		return 0;
	}

	tmp = op_xstrndup(value, strlen(value));

	/* Parse feature name*/
	if((name = strtok_r(tmp, ":", &args)) == NULL)
		goto err_out;

	if((opd_ext_feat_index = get_index_for_feature(name)) < 0)
		goto err_out;

	ret = ext_feature_table[opd_ext_feat_index].handlers->ext_init(args);

	return ret;

err_out:
	fprintf(stderr,"opd_ext_initialize: Invalid extended feature option: %s\n", value);
	return ret;
}


int opd_ext_deinitialize()
{
	int ret = EXIT_FAILURE;

	if(opd_ext_feat_index == -1) {
		return 0;
	}

	ret = ext_feature_table[opd_ext_feat_index].handlers->ext_deinit();

	return ret;
}


void opd_ext_print_stats()
{
	if (is_ext_enabled()
	&& ext_feature_table[opd_ext_feat_index].handlers->ext_print_stats != NULL) {
		printf("\n-- OProfile Extended-Feature Statistics --\n");
		ext_feature_table[opd_ext_feat_index].handlers->ext_print_stats();
	}
}


/**
 * opd_sfile extended APIs
 */
void opd_ext_sfile_create(struct sfile * sf)
{
	/* Creating ext sfile only if extended feature is enable*/
	if (is_ext_sfile_enabled()
	&& ext_feature_table[opd_ext_feat_index].handlers->ext_sfile->create != NULL)
		ext_feature_table[opd_ext_feat_index].handlers->ext_sfile->create(sf);
}


void opd_ext_sfile_dup (struct sfile * to, struct sfile * from)
{
	/* Duplicate ext sfile only if extended feature is enable*/
	if (is_ext_sfile_enabled()
	&& ext_feature_table[opd_ext_feat_index].handlers->ext_sfile->dup != NULL)
		ext_feature_table[opd_ext_feat_index].handlers->ext_sfile->dup(to, from);
}


void opd_ext_sfile_close (struct sfile * sf)
{
	/* Close ext sfile only if extended feature is enable*/
	if (is_ext_sfile_enabled()
	&& ext_feature_table[opd_ext_feat_index].handlers->ext_sfile->close != NULL)
		ext_feature_table[opd_ext_feat_index].handlers->ext_sfile->close(sf);
}


void opd_ext_sfile_sync(struct sfile * sf)
{
	/* Sync ext sfile only if extended feature is enable*/
	if (is_ext_sfile_enabled()
	&& ext_feature_table[opd_ext_feat_index].handlers->ext_sfile->sync != NULL)
		ext_feature_table[opd_ext_feat_index].handlers->ext_sfile->sync(sf);
}


odb_t * opd_ext_sfile_get(struct transient const * trans, int is_cg)
{
	/* Get ext sfile only if extended feature is enable*/
	if (is_ext_sfile_enabled()
	&& ext_feature_table[opd_ext_feat_index].handlers->ext_sfile->get != NULL)
		return	ext_feature_table[opd_ext_feat_index].handlers->ext_sfile->get(trans, is_cg);

	return NULL;
}


struct opd_event * opd_ext_find_counter_event(unsigned long counter)
{
	/* Only if extended feature is enable*/
	if (is_ext_sfile_enabled()
	&& ext_feature_table[opd_ext_feat_index].handlers->ext_sfile->find_counter_event != NULL)
		return	ext_feature_table[opd_ext_feat_index].handlers->ext_sfile->find_counter_event(counter);

	return NULL;
}

