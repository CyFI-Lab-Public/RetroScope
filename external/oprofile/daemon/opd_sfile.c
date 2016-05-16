/**
 * @file daemon/opd_sfile.c
 * Management of sample files
 *
 * @remark Copyright 2002, 2005 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include "opd_sfile.h"

#include "opd_trans.h"
#include "opd_kernel.h"
#include "opd_mangling.h"
#include "opd_anon.h"
#include "opd_printf.h"
#include "opd_stats.h"
#include "opd_extended.h"
#include "oprofiled.h"

#include "op_libiberty.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define HASH_SIZE 2048
#define HASH_BITS (HASH_SIZE - 1)

/** All sfiles are hashed into these lists */
static struct list_head hashes[HASH_SIZE];

/** All sfiles are on this list. */
static LIST_HEAD(lru_list);


/* FIXME: can undoubtedly improve this hashing */
/** Hash the transient parameters for lookup. */
static unsigned long
sfile_hash(struct transient const * trans, struct kernel_image * ki)
{
	unsigned long val = 0;
	
	if (separate_thread) {
		val ^= trans->tid << 2;
		val ^= trans->tgid << 2;
	}

	if (separate_kernel || ((trans->anon || separate_lib) && !ki))
		val ^= trans->app_cookie >> (DCOOKIE_SHIFT + 3);

	if (separate_cpu)
		val ^= trans->cpu;

	/* cookie meaningless for kernel, shouldn't hash */
	if (trans->in_kernel) {
		val ^= ki->start >> 14;
		val ^= ki->end >> 7;
		return val & HASH_BITS;
	}

	if (trans->cookie != NO_COOKIE) {
		val ^= trans->cookie >> DCOOKIE_SHIFT;
		return val & HASH_BITS;
	}

	if (!separate_thread)
		val ^= trans->tgid << 2;

	if (trans->anon) {
		val ^= trans->anon->start >> VMA_SHIFT;
		val ^= trans->anon->end >> (VMA_SHIFT + 1);
	}

	return val & HASH_BITS;
}


static int
do_match(struct sfile const * sf, cookie_t cookie, cookie_t app_cookie,
         struct kernel_image const * ki, struct anon_mapping const * anon,
         pid_t tgid, pid_t tid, unsigned int cpu)
{
	/* this is a simplified check for "is a kernel image" AND
	 * "is the right kernel image". Also handles no-vmlinux
	 * correctly.
	 */
	if (sf->kernel != ki)
		return 0;

	if (separate_thread) {
		if (sf->tid != tid || sf->tgid != tgid)
			return 0;
	}

	if (separate_cpu) {
		if (sf->cpu != cpu)
			return 0;
	}

	if (separate_kernel || ((anon || separate_lib) && !ki)) {
		if (sf->app_cookie != app_cookie)
			return 0;
	}

	/* ignore the cached trans->cookie for kernel images,
	 * it's meaningless and we checked all others already
	 */
	if (ki)
		return 1;

	if (sf->anon != anon)
		return 0;

	return sf->cookie == cookie;
}


static int
trans_match(struct transient const * trans, struct sfile const * sfile,
            struct kernel_image const * ki)
{
	return do_match(sfile, trans->cookie, trans->app_cookie, ki,
	                trans->anon, trans->tgid, trans->tid, trans->cpu);
}


int
sfile_equal(struct sfile const * sf, struct sfile const * sf2)
{
	return do_match(sf, sf2->cookie, sf2->app_cookie, sf2->kernel,
	                sf2->anon, sf2->tgid, sf2->tid, sf2->cpu);
}


static int
is_sf_ignored(struct sfile const * sf)
{
	if (sf->kernel) {
		if (!is_image_ignored(sf->kernel->name))
			return 0;

		/* Let a dependent kernel image redeem the sf if we're
		 * executing on behalf of an application.
		 */
		return is_cookie_ignored(sf->app_cookie);
	}

	/* Anon regions are always dependent on the application.
 	 * Otherwise, let a dependent image redeem the sf.
	 */
	if (sf->anon || is_cookie_ignored(sf->cookie))
		return is_cookie_ignored(sf->app_cookie);

	return 0;
}


/** create a new sfile matching the current transient parameters */
static struct sfile *
create_sfile(unsigned long hash, struct transient const * trans,
             struct kernel_image * ki)
{
	size_t i;
	struct sfile * sf;

	sf = xmalloc(sizeof(struct sfile));

	sf->hashval = hash;

	/* The logic here: if we're in the kernel, the cached cookie is
	 * meaningless (though not the app_cookie if separate_kernel)
	 */
	sf->cookie = trans->in_kernel ? INVALID_COOKIE : trans->cookie;
	sf->app_cookie = INVALID_COOKIE;
	sf->tid = (pid_t)-1;
	sf->tgid = (pid_t)-1;
	sf->cpu = 0;
	sf->kernel = ki;
	sf->anon = trans->anon;

	for (i = 0 ; i < op_nr_counters ; ++i)
		odb_init(&sf->files[i]);

	if (trans->ext)
		opd_ext_sfile_create(sf);
	else
		sf->ext_files = NULL;

	for (i = 0; i < CG_HASH_SIZE; ++i)
		list_init(&sf->cg_hash[i]);

	if (separate_thread)
		sf->tid = trans->tid;
	if (separate_thread || trans->cookie == NO_COOKIE)
		sf->tgid = trans->tgid;

	if (separate_cpu)
		sf->cpu = trans->cpu;

	if (separate_kernel || ((trans->anon || separate_lib) && !ki))
		sf->app_cookie = trans->app_cookie;

	sf->ignored = is_sf_ignored(sf);

	sf->embedded_offset = trans->embedded_offset;

	/* If embedded_offset is a valid value, it means we're
	 * processing a Cell BE SPU profile; in which case, we
	 * want sf->app_cookie to hold trans->app_cookie.
	 */
	if (trans->embedded_offset != UNUSED_EMBEDDED_OFFSET)
		sf->app_cookie = trans->app_cookie;
	return sf;
}


struct sfile * sfile_find(struct transient const * trans)
{
	struct sfile * sf;
	struct list_head * pos;
	struct kernel_image * ki = NULL;
	unsigned long hash;

	if (trans->tracing != TRACING_ON) {
		opd_stats[OPD_SAMPLES]++;
		opd_stats[trans->in_kernel == 1 ? OPD_KERNEL : OPD_PROCESS]++;
	}

	/* There is a small race where this *can* happen, see
	 * caller of cpu_buffer_reset() in the kernel
	 */
	if (trans->in_kernel == -1) {
		verbprintf(vsamples, "Losing sample at 0x%llx of unknown provenance.\n",
		           trans->pc);
		opd_stats[OPD_NO_CTX]++;
		return NULL;
	}

	/* we might need a kernel image start/end to hash on */
	if (trans->in_kernel) {
		ki = find_kernel_image(trans);
		if (!ki) {
			verbprintf(vsamples, "Lost kernel sample %llx\n", trans->pc);
			opd_stats[OPD_LOST_KERNEL]++;
			return NULL;
		}
	} else if (trans->cookie == NO_COOKIE && !trans->anon) {
		if (vsamples) {
			char const * app = verbose_cookie(trans->app_cookie);
			printf("No anon map for pc %llx, app %s.\n",
			       trans->pc, app);
		}
		opd_stats[OPD_LOST_NO_MAPPING]++;
		return NULL;
	}

	hash = sfile_hash(trans, ki);
	list_for_each(pos, &hashes[hash]) {
		sf = list_entry(pos, struct sfile, hash);
		if (trans_match(trans, sf, ki)) {
			sfile_get(sf);
			goto lru;
		}
	}

	sf = create_sfile(hash, trans, ki);
	list_add(&sf->hash, &hashes[hash]);

lru:
	sfile_put(sf);
	return sf;
}


void sfile_dup(struct sfile * to, struct sfile * from)
{
	size_t i;

	memcpy(to, from, sizeof (struct sfile));

	for (i = 0 ; i < op_nr_counters ; ++i)
		odb_init(&to->files[i]);

	opd_ext_sfile_dup(to, from);

	for (i = 0; i < CG_HASH_SIZE; ++i)
		list_init(&to->cg_hash[i]);

	list_init(&to->hash);
	list_init(&to->lru);
}


static odb_t * get_file(struct transient const * trans, int is_cg)
{
	struct sfile * sf = trans->current;
	struct sfile * last = trans->last;
	struct cg_entry * cg;
	struct list_head * pos;
	unsigned long hash;
	odb_t * file;

	if ((trans->ext) != NULL)
		return opd_ext_sfile_get(trans, is_cg);

	if (trans->event >= op_nr_counters) {
		fprintf(stderr, "%s: Invalid counter %lu\n", __FUNCTION__,
			trans->event);
		abort();
	}

	file = &sf->files[trans->event];

	if (!is_cg)
		goto open;

	hash = last->hashval & (CG_HASH_SIZE - 1);

	/* Need to look for the right 'to'. Since we're looking for
	 * 'last', we use its hash.
	 */
	list_for_each(pos, &sf->cg_hash[hash]) {
		cg = list_entry(pos, struct cg_entry, hash);
		if (sfile_equal(last, &cg->to)) {
			file = &cg->to.files[trans->event];
			goto open;
		}
	}

	cg = xmalloc(sizeof(struct cg_entry));
	sfile_dup(&cg->to, last);
	list_add(&cg->hash, &sf->cg_hash[hash]);
	file = &cg->to.files[trans->event];

open:
	if (!odb_open_count(file))
		opd_open_sample_file(file, last, sf, trans->event, is_cg);

	/* Error is logged by opd_open_sample_file */
	if (!odb_open_count(file))
		return NULL;

	return file;
}


static void verbose_print_sample(struct sfile * sf, vma_t pc, uint counter)
{
	char const * app = verbose_cookie(sf->app_cookie);
	printf("0x%llx(%u): ", pc, counter);
	if (sf->anon) {
		printf("anon (tgid %u, 0x%llx-0x%llx), ",
		       (unsigned int)sf->anon->tgid,
		       sf->anon->start, sf->anon->end);
	} else if (sf->kernel) {
		printf("kern (name %s, 0x%llx-0x%llx), ", sf->kernel->name,
		       sf->kernel->start, sf->kernel->end);
	} else {
		printf("%s(%llx), ", verbose_cookie(sf->cookie),  sf->cookie);
	}
	printf("app %s(%llx)", app, sf->app_cookie);
}


static void verbose_sample(struct transient const * trans, vma_t pc)
{
	printf("Sample ");
	verbose_print_sample(trans->current, pc, trans->event);
	printf("\n");
}


static void
verbose_arc(struct transient const * trans, vma_t from, vma_t to)
{
	printf("Arc ");
	verbose_print_sample(trans->current, from, trans->event);
	printf(" -> 0x%llx", to);
	printf("\n");
}


static void sfile_log_arc(struct transient const * trans)
{
	int err;
	vma_t from = trans->pc;
	vma_t to = trans->last_pc;
	uint64_t key;
	odb_t * file;

	file = get_file(trans, 1);

	/* absolute value -> offset */
	if (trans->current->kernel)
		from -= trans->current->kernel->start;

	if (trans->last->kernel)
		to -= trans->last->kernel->start;

	if (trans->current->anon)
		from -= trans->current->anon->start;

	if (trans->last->anon)
		to -= trans->last->anon->start;

	if (varcs)
		verbose_arc(trans, from, to);

	if (!file) {
		opd_stats[OPD_LOST_SAMPLEFILE]++;
		return;
	}

	/* Possible narrowings to 32-bit value only. */
	key = to & (0xffffffff);
	key |= ((uint64_t)from) << 32;

	err = odb_update_node(file, key);
	if (err) {
		fprintf(stderr, "%s: %s\n", __FUNCTION__, strerror(err));
		abort();
	}
}


void sfile_log_sample(struct transient const * trans)
{
	sfile_log_sample_count(trans, 1);
}


void sfile_log_sample_count(struct transient const * trans,
                            unsigned long int count)
{
	int err;
	vma_t pc = trans->pc;
	odb_t * file;

	if (trans->tracing == TRACING_ON) {
		/* can happen if kernel sample falls through the cracks,
		 * see opd_put_sample() */
		if (trans->last)
			sfile_log_arc(trans);
		return;
	}

	file = get_file(trans, 0);

	/* absolute value -> offset */
	if (trans->current->kernel)
		pc -= trans->current->kernel->start;

	if (trans->current->anon)
		pc -= trans->current->anon->start;

	if (vsamples)
		verbose_sample(trans, pc);

	if (!file) {
		opd_stats[OPD_LOST_SAMPLEFILE]++;
		return;
	}

	err = odb_update_node_with_offset(file,
					  (odb_key_t)pc,
					  count);
	if (err) {
		fprintf(stderr, "%s: %s\n", __FUNCTION__, strerror(err));
		abort();
	}
}


static int close_sfile(struct sfile * sf, void * data __attribute__((unused)))
{
	size_t i;

	/* it's OK to close a non-open odb file */
	for (i = 0; i < op_nr_counters; ++i)
		odb_close(&sf->files[i]);

	opd_ext_sfile_close(sf);

	return 0;
}


static void kill_sfile(struct sfile * sf)
{
	close_sfile(sf, NULL);
	list_del(&sf->hash);
	list_del(&sf->lru);
}


static int sync_sfile(struct sfile * sf, void * data __attribute__((unused)))
{
	size_t i;

	for (i = 0; i < op_nr_counters; ++i)
		odb_sync(&sf->files[i]);

	opd_ext_sfile_sync(sf);

	return 0;
}


static int is_sfile_kernel(struct sfile * sf, void * data __attribute__((unused)))
{
	return !!sf->kernel;
}


static int is_sfile_anon(struct sfile * sf, void * data)
{
	return sf->anon == data;
}


typedef int (*sfile_func)(struct sfile *, void *);

static void
for_one_sfile(struct sfile * sf, sfile_func func, void * data)
{
	size_t i;
	int free_sf = func(sf, data);

	for (i = 0; i < CG_HASH_SIZE; ++i) {
		struct list_head * pos;
		struct list_head * pos2;
		list_for_each_safe(pos, pos2, &sf->cg_hash[i]) {
			struct cg_entry * cg =
				list_entry(pos, struct cg_entry, hash);
			if (free_sf || func(&cg->to, data)) {
				kill_sfile(&cg->to);
				list_del(&cg->hash);
				free(cg);
			}
		}
	}

	if (free_sf) {
		kill_sfile(sf);
		free(sf);
	}
}


static void for_each_sfile(sfile_func func, void * data)
{
	struct list_head * pos;
	struct list_head * pos2;

	list_for_each_safe(pos, pos2, &lru_list) {
		struct sfile * sf = list_entry(pos, struct sfile, lru);
		for_one_sfile(sf, func, data);
	}
}


void sfile_clear_kernel(void)
{
	for_each_sfile(is_sfile_kernel, NULL);
}


void sfile_clear_anon(struct anon_mapping * anon)
{
	for_each_sfile(is_sfile_anon, anon);
}


void sfile_sync_files(void)
{
	for_each_sfile(sync_sfile, NULL);
}


void sfile_close_files(void)
{
	for_each_sfile(close_sfile, NULL);
}


static int always_true(void)
{
	return 1;
}


#define LRU_AMOUNT 256

/*
 * Clear out older sfiles. Note the current sfiles we're using
 * will not be present in this list, due to sfile_get/put() pairs
 * around the caller of this.
 */
int sfile_lru_clear(void)
{
	struct list_head * pos;
	struct list_head * pos2;
	int amount = LRU_AMOUNT;

	if (list_empty(&lru_list))
		return 1;

	list_for_each_safe(pos, pos2, &lru_list) {
		struct sfile * sf;
		if (!--amount)
			break;
		sf = list_entry(pos, struct sfile, lru);
		for_one_sfile(sf, (sfile_func)always_true, NULL);
	}

	return 0;
}


void sfile_get(struct sfile * sf)
{
	if (sf)
		list_del(&sf->lru);
}


void sfile_put(struct sfile * sf)
{
	if (sf)
		list_add_tail(&sf->lru, &lru_list);
}


void sfile_init(void)
{
	size_t i = 0;

	for (; i < HASH_SIZE; ++i)
		list_init(&hashes[i]);
}
