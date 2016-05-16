/**
 * @file db_test.c
 * Tests for DB hash
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Philippe Elie
 */

#include <sys/types.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <fcntl.h>

#include "op_sample_file.h"
#include "odb.h"

#define TEST_FILENAME "test-hash-db.dat"

static int nr_error;

static int verbose = 0;

#define verbprintf(args...) \
	do { \
		if (verbose) \
			printf(args); \
	} while (0)

static double used_time(void)
{
	struct rusage  usage;

	getrusage(RUSAGE_SELF, &usage);

	return (usage.ru_utime.tv_sec + usage.ru_stime.tv_sec) * 1E9 + 
		((usage.ru_utime.tv_usec + usage.ru_stime.tv_usec)) * 1000;
}


/* update nr item */
static void speed_test(int nr_item, char const * test_name)
{
	int i;
	double begin, end;
	odb_t hash;
	int rc;

	rc = odb_open(&hash, TEST_FILENAME, ODB_RDWR, sizeof(struct opd_header));
	if (rc) {
		fprintf(stderr, "%s", strerror(rc));
		exit(EXIT_FAILURE);
	}
	begin = used_time();
	for (i = 0 ; i < nr_item ; ++i) {
		rc = odb_update_node(&hash, i);
		if (rc != EXIT_SUCCESS) {
			fprintf(stderr, "%s", strerror(rc));
			exit(EXIT_FAILURE);
		}
	}

	end = used_time();
	odb_close(&hash);

	verbprintf("%s: nr item: %d, elapsed: %f ns\n",
		   test_name, nr_item, (end - begin) / nr_item);
}


static void do_speed_test(void)
{
	int i;

	for (i = 100000; i <= 10000000; i *= 10) {
		// first test count insertion, second fetch and incr count
		speed_test(i, "insert");
		speed_test(i, "update");
		remove(TEST_FILENAME);
	}
}


static int test(int nr_item, int nr_unique_item)
{
	int i;
	odb_t hash;
	int ret;
	int rc;

	rc = odb_open(&hash, TEST_FILENAME, ODB_RDWR, sizeof(struct opd_header));
	if (rc) {
		fprintf(stderr, "%s", strerror(rc));
		exit(EXIT_FAILURE);
	}


	for (i = 0 ; i < nr_item ; ++i) {
		odb_key_t key = (random() % nr_unique_item) + 1;
		rc = odb_update_node(&hash, key);
		if (rc != EXIT_SUCCESS) {
			fprintf(stderr, "%s", strerror(rc));
			exit(EXIT_FAILURE);
		}
	}

	ret = odb_check_hash(&hash);

	odb_close(&hash);

	remove(TEST_FILENAME);

	return ret;
}


static void do_test(void)
{
	int i, j;

	for (i = 1000; i <= 100000; i *= 10) {
		for (j = 100 ; j <= i / 10 ; j *= 10) {
			if (test(i, j)) {
				fprintf(stderr, "%s:%d failure for %d %d\n",
				       __FILE__, __LINE__, i, j);
				nr_error++;
			} else {
				verbprintf("test() ok %d %d\n", i, j);
			}
		}
	}
}


static void sanity_check(char const * filename)
{
	odb_t hash;
	int rc;

	rc = odb_open(&hash, filename, ODB_RDONLY, sizeof(struct opd_header));
	if (rc) {
		fprintf(stderr, "%s", strerror(rc));
	        exit(EXIT_FAILURE);
	}

	if (odb_check_hash(&hash)) {
		fprintf(stderr, "checking file %s FAIL\n", filename);
		++nr_error;
	} else if (verbose) {
		odb_hash_stat_t * stats;
		stats = odb_hash_stat(&hash);
		odb_hash_display_stat(stats);
		odb_hash_free_stat(stats);
	}

	odb_close(&hash);
}

int main(int argc, char * argv[1])
{
	/* if a filename is given take it as: "check this db" */
	if (argc > 1) {
		int i;
		verbose = 1;
		if (!strcmp(argv[1], "--speed"))
			goto speed_test;
		for (i = 1 ; i < argc ; ++i)
			sanity_check(argv[i]);
		return 0;
	}

speed_test:
	remove(TEST_FILENAME);

	do_test();

	do_speed_test();

	if (nr_error)
		printf("%d error occured\n", nr_error);

	return nr_error ? EXIT_FAILURE : EXIT_SUCCESS;
}
