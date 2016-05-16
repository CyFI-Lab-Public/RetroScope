/**
 * @file db_debug.c
 * Debug routines for libdb
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "odb.h"

static int check_circular_list(odb_data_t const * data)
{
	odb_node_nr_t pos;
	int do_abort = 0;
	unsigned char * bitmap = malloc(data->descr->current_size);
	memset(bitmap, '\0', data->descr->current_size);

	for (pos = 0 ; pos < data->descr->size * BUCKET_FACTOR ; ++pos) {

		odb_index_t index = data->hash_base[pos];
		if (index && !do_abort) {
			while (index) {
				if (bitmap[index])
					do_abort = 1;

				bitmap[index] = 1;
				index = data->node_base[index].next;
			}
		}

		if (do_abort) {
			printf("circular list detected size: %d\n",
			       data->descr->current_size);

			memset(bitmap, '\0', data->descr->current_size);

			index = data->hash_base[pos];
			while (index) {
				printf("%d ", index);
				if (bitmap[index])
					exit(1);

				bitmap[index] = 1;
				index = data->node_base[index].next;
			}
		}

		/* purely an optimization: intead of memset the map reset only
		 * the needed part: not my use to optimize test but here the
		 * test was so slow it was useless */
		index = data->hash_base[pos];
		while (index) {
			bitmap[index] = 1;
			index = data->node_base[index].next;
		}
	}

	free(bitmap);

	return do_abort;
}

static int check_redundant_key(odb_data_t const * data, odb_key_t max)
{
	odb_node_nr_t pos;

	unsigned char * bitmap = malloc(max + 1);
	memset(bitmap, '\0', max + 1);

	for (pos = 1 ; pos < data->descr->current_size ; ++pos) {
		if (bitmap[data->node_base[pos].key]) {
			printf("redundant key found %lld\n",
			       (unsigned long long)data->node_base[pos].key);
			return 1;
		}
		bitmap[data->node_base[pos].key] = 1;
	}
	free(bitmap);

	return 0;
}

int odb_check_hash(odb_t const * odb)
{
	odb_node_nr_t pos;
	odb_node_nr_t nr_node = 0;
	odb_node_nr_t nr_node_out_of_bound = 0;
	int ret = 0;
	odb_key_t max = 0;
	odb_data_t * data = odb->data;

	for (pos = 0 ; pos < data->descr->size * BUCKET_FACTOR ; ++pos) {
		odb_index_t index = data->hash_base[pos];
		while (index) {
			if (index >= data->descr->current_size) {
				nr_node_out_of_bound++;
				break;
			}
			++nr_node;

			if (data->node_base[index].key > max)
				max = data->node_base[index].key;

			index = data->node_base[index].next;
		}
	}

	if (nr_node != data->descr->current_size - 1) {
		printf("hash table walk found %d node expect %d node\n",
		       nr_node, data->descr->current_size - 1);
		ret = 1;
	}

	if (nr_node_out_of_bound) {
		printf("out of bound node index: %d\n", nr_node_out_of_bound);
		ret = 1;
	}

	if (ret == 0)
		ret = check_circular_list(data);

	if (ret == 0)
		ret = check_redundant_key(data, max);

	return ret;
}
