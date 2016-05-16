/**
 * @file opd_anon.c
 * Anonymous region handling.
 *
 * Our caching of maps has some problems: if we get tgid reuse,
 * and it's the same application, we might end up with wrong
 * maps. The same happens in an unmap-remap case. There's not much
 * we can do about this, we just hope it's not too common...
 *
 * What is relatively common is expanding anon maps, which leaves us
 * with lots of separate sample files.
 *
 * @remark Copyright 2005 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @Modifications Gisle Dankel
 */

#include "opd_anon.h"
#include "opd_trans.h"
#include "opd_sfile.h"
#include "opd_printf.h"
#include "op_libiberty.h"

#include <limits.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#define HASH_SIZE 1024
#define HASH_BITS (HASH_SIZE - 1)

/*
 * Note that this value is tempered by the fact that when we miss in the
 * anon cache, we'll tear down all the mappings for that tgid. Thus, LRU
 * of a mapping can potentially clear out a much larger number of
 * mappings.
 */
#define LRU_SIZE 8192
#define LRU_AMOUNT (LRU_SIZE/8)

static struct list_head hashes[HASH_SIZE];
static struct list_head lru;
static size_t nr_lru;

static void do_lru(struct transient * trans)
{
	size_t nr_to_kill = LRU_AMOUNT;
	struct list_head * pos;
	struct list_head * pos2;
	struct anon_mapping * entry;

	list_for_each_safe(pos, pos2, &lru) {
		entry = list_entry(pos, struct anon_mapping, lru_list);
		if (trans->anon == entry)
			clear_trans_current(trans);
		if (trans->last_anon == entry)
			clear_trans_last(trans);
		sfile_clear_anon(entry);
		list_del(&entry->list);
		list_del(&entry->lru_list);
		--nr_lru;
		free(entry);
		if (nr_to_kill-- == 0)
			break;
	}
}


static unsigned long hash_anon(pid_t tgid, cookie_t app)
{
	return ((app >> DCOOKIE_SHIFT) ^ (tgid >> 2)) & (HASH_SIZE - 1);
}
 

static void clear_anon_maps(struct transient * trans)
{
	unsigned long hash = hash_anon(trans->tgid, trans->app_cookie);
	pid_t tgid = trans->tgid;
	cookie_t app = trans->app_cookie;
	struct list_head * pos;
	struct list_head * pos2;
	struct anon_mapping * entry;

	clear_trans_current(trans);

	list_for_each_safe(pos, pos2, &hashes[hash]) {
		entry = list_entry(pos, struct anon_mapping, list);
		if (entry->tgid == tgid && entry->app_cookie == app) {
			if (trans->last_anon == entry)
				clear_trans_last(trans);
			sfile_clear_anon(entry);
			list_del(&entry->list);
			list_del(&entry->lru_list);
			--nr_lru;
			free(entry);
		}
	}

	if (vmisc) {
		char const * name = verbose_cookie(app);
		printf("Cleared anon maps for tgid %u (%s).\n", tgid, name);
	}
}


static void
add_anon_mapping(struct transient * trans, vma_t start, vma_t end, char * name)
{
	unsigned long hash = hash_anon(trans->tgid, trans->app_cookie);
	struct anon_mapping * m = xmalloc(sizeof(struct anon_mapping));
	m->tgid = trans->tgid;
	m->app_cookie = trans->app_cookie;
	m->start = start;
	m->end = end;
	strncpy(m->name, name, MAX_IMAGE_NAME_SIZE + 1);
	list_add_tail(&m->list, &hashes[hash]);
	list_add_tail(&m->lru_list, &lru);
	if (++nr_lru == LRU_SIZE)
		do_lru(trans);
	if (vmisc) {
		char const * name = verbose_cookie(m->app_cookie);
		printf("Added anon map 0x%llx-0x%llx for tgid %u (%s).\n",
		       start, end, m->tgid, name);
	}
}


/* 42000000-4212f000 r-xp 00000000 16:03 424334 /lib/tls/libc-2.3.2.so */
static void get_anon_maps(struct transient * trans)
{
	FILE * fp = NULL;
	char buf[PATH_MAX];
	vma_t start, end;
	int ret;

	snprintf(buf, PATH_MAX, "/proc/%d/maps", trans->tgid);
	fp = fopen(buf, "r");
	if (!fp)
		return;

	while (fgets(buf, PATH_MAX, fp) != NULL) {
		char tmp[MAX_IMAGE_NAME_SIZE + 1];
		char name[MAX_IMAGE_NAME_SIZE + 1];
		/* Some anon maps have labels like
		 * [heap], [stack], [vdso], [vsyscall] ...
		 * Keep track of these labels. If a map has no name, call it "anon".
		 * Ignore all mappings starting with "/" (file or shared memory object)
		 */
		strcpy(name, "anon");
		ret = sscanf(buf, "%llx-%llx %20s %20s %20s %20s %20s",
		             &start, &end, tmp, tmp, tmp, tmp, name);
		if (ret < 6 || name[0] == '/')
			continue;

		add_anon_mapping(trans, start, end, name);
	}

	fclose(fp);
}


static int
anon_match(struct transient const * trans, struct anon_mapping const * anon)
{
	if (!anon)
		return 0;
	if (trans->tgid != anon->tgid)
		return 0;
	if (trans->app_cookie != anon->app_cookie)
		return 0;
	if (trans->pc < anon->start)
		return 0;
	return (trans->pc < anon->end);
}


struct anon_mapping * find_anon_mapping(struct transient * trans)
{
	unsigned long hash = hash_anon(trans->tgid, trans->app_cookie);
	struct list_head * pos;
	struct anon_mapping * entry;
	int tried = 0;

	if (anon_match(trans, trans->anon))
		return (trans->anon);

retry:
	list_for_each(pos, &hashes[hash]) {
		entry = list_entry(pos, struct anon_mapping, list);
		if (anon_match(trans, entry))
			goto success;
	}

	if (!tried) {
		clear_anon_maps(trans);
		get_anon_maps(trans);
		tried = 1;
		goto retry;
	}

	return NULL;

success:
	/*
	 * Typically, there's one big mapping that matches. Let's go
	 * faster.
	 */
	list_del(&entry->list);
	list_add(&entry->list, &hashes[hash]);

	verbprintf(vmisc, "Found range 0x%llx-0x%llx for tgid %u, pc %llx.\n",
	           entry->start, entry->end, (unsigned int)entry->tgid,
		   trans->pc);
	return entry;
}


void anon_init(void)
{
	size_t i;

	for (i = 0; i < HASH_SIZE; ++i)
		list_init(&hashes[i]);

	list_init(&lru);
}
