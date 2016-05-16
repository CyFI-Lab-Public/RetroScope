/**
 * @file jitsymbol.c
 * Handle symbols found in jitted code dump
 *
 * @remark Copyright 2007 OProfile authors
 * @remark Read the file COPYING
 *
 * @author Jens Wilke
 * @Modifications Maynard Johnson
 * @Modifications Philippe Elie
 * @Modifications Daniel Hansel
 *
 * Copyright IBM Corporation 2007
 *
 */

#include "opjitconv.h"
#include "opd_printf.h"
#include "op_libiberty.h"
#include "op_types.h"

#include <stddef.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <limits.h>

typedef int (*compare_symbol)(void const *, void const *);


/* count the entries in the jitentry_list */
static u32 count_entries(void)
{
	struct jitentry const * entry;
	u32 cnt = 0;
	for (entry = jitentry_list; entry; entry = entry->next)
		cnt++;
	return cnt;
}


static void fill_entry_array(struct jitentry * entries[])
{
	int i = 0;
	struct jitentry * entry;
	for (entry = jitentry_list; entry; entry = entry->next)
		entries[i++] = entry;
}


/* create an array pointing to the jitentry structures which is sorted
 * according to the comparator rule given by parameter compar
 */
static struct jitentry ** create_sorted_array(compare_symbol compare)
{
	struct jitentry ** array =
		xmalloc(sizeof(struct jitentry *) * entry_count);
	fill_entry_array(array);
	qsort(array, entry_count, sizeof(struct jitentry *), compare);
	return array;
}


/* comparator method for qsort which sorts jitentries by symbol_name */
static int cmp_symbolname(void const * a, void const * b)
{
	struct jitentry * a0 = *(struct jitentry **) a;
	struct jitentry * b0 = *(struct jitentry **) b;
	return strcmp(a0->symbol_name, b0->symbol_name);
}


/* comparator method for qsort which sorts jitentries by address */
static int cmp_address(void const * a, void const * b)
{
	struct jitentry * a0 = *(struct jitentry **) a;
	struct jitentry * b0 = *(struct jitentry **) b;
	if (a0->vma < b0->vma)
		return -1;
	if (a0->vma == b0->vma)
		return 0;
	return 1;
}


/* resort address_ascending array */
static void resort_address(void)
{
	u32 i;

	qsort(entries_address_ascending, entry_count,
	      sizeof(struct jitentry *), cmp_address);

	// lower entry_count if entries are invalidated
	for (i = 0; i < entry_count; ++i) {
		if (entries_address_ascending[i]->vma)
			break;
	}

	if (i) {
		entry_count -= i;
		memmove(&entries_address_ascending[0],
			&entries_address_ascending[i],
			sizeof(struct jitentry *) * entry_count);
	}
}


/* Copy address_ascending array to entries_symbols_ascending and resort it.  */
static void resort_symbol(void)
{
	memcpy(entries_symbols_ascending, entries_address_ascending,
	       sizeof(struct jitentry *) * entry_count);
	qsort(entries_symbols_ascending, entry_count,
	      sizeof(struct jitentry *), cmp_symbolname);
}

/* allocate, populate and sort the jitentry arrays */
void create_arrays(void)
{
	max_entry_count = entry_count = count_entries();
	entries_symbols_ascending = create_sorted_array(cmp_symbolname);
	entries_address_ascending = create_sorted_array(cmp_address);
}


/* add a new create jitentry to the array. mallocs new arrays if space is
 * needed */
static void insert_entry(struct jitentry * entry)
{
	if (entry_count == max_entry_count) {
		if (max_entry_count < UINT32_MAX - 18)
			max_entry_count += 18;
		else if (max_entry_count < UINT32_MAX)
			max_entry_count += 1;
		else {
			fprintf(stderr, "Amount of JIT dump file entries is too large.\n");
			exit(EXIT_FAILURE);
		}
		entries_symbols_ascending = (struct jitentry **)
			xrealloc(entries_symbols_ascending,
				 sizeof(struct jitentry *) * max_entry_count);
		entries_address_ascending = (struct jitentry **)
			xrealloc(entries_address_ascending,
				 sizeof(struct jitentry *) * max_entry_count);
	}
	entries_address_ascending[entry_count++] = entry;
}


/* add a suffix to the name to differenciate it */
static char * replacement_name(char * s, int i)
{
	int cnt = 1;
	int j = i;
	char * res;

	while ((j = j/10))
		cnt++;
	cnt += 2 + strlen(s);
	res = xmalloc(cnt);
	snprintf(res, cnt, "%s~%i", s, i);
	return res;
}


/*
 * Mark the entry so it is not included in the ELF file. We do this by
 * writing a 0 address as magic vma and sorting
 * it out later
 */
static void invalidate_entry(struct jitentry * e)
{
	verbprintf(debug, "invalidate_entry: addr=%llx, name=%s\n",
		   e->vma, e->symbol_name);
	e->vma = 0;
}


/*
 * Invalidate all symbols that are not alive at sampling start time.
 */
static void invalidate_earlybirds(unsigned long long start_time)
{
	u32 i;
	int flag;
	struct jitentry * a;

	flag = 0;
	for (i = 0; i < entry_count; i++) {
		a = entries_address_ascending[i];
		if (a->life_end < start_time) {
			invalidate_entry(a);
			flag = 1;
		}
	}
	if (flag) {
		resort_address();
		resort_symbol();
	}
}


/* select the symbol with the longest life time in the index range */
static int select_one(int start_idx, int end_idx)
{
	int i;
	int candidate = OP_JIT_CONV_FAIL;
	unsigned long long lifetime = 0;
	unsigned long long x;
	struct jitentry const * e;

	for (i = start_idx; i <= end_idx; i++) {
		e = entries_address_ascending[i];
		x = e->life_end - e->life_start;
		if (candidate == -1 || x > lifetime) {
			candidate = i;
			lifetime = x;
		}
	}
	return candidate;
}


/*
 * We decided to keep one entry in favor of the other. Instead of dropping
 * the overlapping entry we split or truncate it to not overlap any more.
 *
 * Looking on the address regions, we may have the following situation:
 *
 *  split:     |------------|
 *  keep:          |---|
 *
 * The split entry may be splitted in a left part and a right part. E.g.:
 *
 *  split0/1:  |--|     |---|
 *  keep:          |---|
 *
 * However, both parts may or may not exist.
 */
static void split_entry(struct jitentry * split, struct jitentry const * keep)
{
	unsigned long long start_addr_keep = keep->vma;
	unsigned long long end_addr_keep = keep->vma + keep->code_size;
	unsigned long long end_addr_split = split->vma + split->code_size;
	unsigned long long start_addr_split = split->vma;

	// do we need a right part?
	if (end_addr_split > end_addr_keep) {
		struct jitentry * new_entry =
			xcalloc(1, sizeof(struct jitentry));
		char * s = NULL;
		
		/* Check for max. length to avoid possible integer overflow. */
		if (strlen(split->symbol_name) > SIZE_MAX - 3) {
			fprintf(stderr, "Length of symbol name is too large.\n");
			exit(EXIT_FAILURE);
		} else {
			s = xmalloc(strlen(split->symbol_name) + 3);
			strcpy(s, split->symbol_name);
			strcat(s, "#1");
		}
		
		new_entry->vma = end_addr_keep;
		new_entry->code_size = end_addr_split - end_addr_keep;
		new_entry->symbol_name = s;
		new_entry->sym_name_malloced = 1;
		new_entry->life_start = split->life_start;
		new_entry->life_end = split->life_end;
		// the right part does not have an associated code, because we
		// don't know whether the split part begins at an opcode
		new_entry->code = NULL;
		verbprintf(debug, "split right (new) name=%s, start=%llx,"
			   " end=%llx\n", new_entry->symbol_name,
			   new_entry->vma,
			   new_entry->vma + new_entry->code_size);
		insert_entry(new_entry);
	}
	// do we need a left part?
	if (start_addr_split < start_addr_keep) {
		char * s = NULL;
		
		/* Check for max. length to avoid possible integer overflow. */
		if (strlen(split->symbol_name) > SIZE_MAX - 3) {
			fprintf(stderr, "Length of symbol name is too large.\n");
			exit(EXIT_FAILURE);
		} else {
			s = xmalloc(strlen(split->symbol_name) + 3);
			strcpy(s, split->symbol_name);
			strcat(s, "#0");
		}
		
		split->code_size = start_addr_keep - start_addr_split;
		if (split->sym_name_malloced)
			free(split->symbol_name);
		split->symbol_name = s;
		split->sym_name_malloced = 1;
		verbprintf(debug, "split left name=%s, start=%llx, end=%llx\n",
			   split->symbol_name, split->vma,
			   split->vma + split->code_size);
	} else {
		invalidate_entry(split);
	}
}


/*
 * Scans through the index range and splits/truncates entries that overlap
 * with the one indexed by keep_idx. Returns the total lifetime of the symbols
 * found to overlap.
 * Returns ULONG_MAX on error.
 */
static unsigned long long eliminate_overlaps(int start_idx, int end_idx,
					 int keep_idx)
{
	unsigned long long retval;
	struct jitentry const * keep = entries_address_ascending[keep_idx];
	struct jitentry * e;
	unsigned long long start_addr_keep = keep->vma;
	unsigned long long end_addr_keep = keep->vma + keep->code_size;
	unsigned long long start_addr_entry, end_addr_entry;
	int i;
	unsigned long long min_start = keep->life_start;
	unsigned long long max_end = keep->life_end;

	for (i = start_idx; i <= end_idx; i++) {
		if (i == keep_idx)
			continue;
		e = entries_address_ascending[i];
		start_addr_entry = e->vma;
		end_addr_entry = e->vma + e->code_size;
		if (debug) {
			if (!(start_addr_entry <= end_addr_entry)) {
				verbprintf(debug, "assert failed:"
					   " start_addr_entry <="
					   " end_addr_entry\n");
				retval = ULONG_MAX;
				goto out;
			}
			if (!(start_addr_keep <= end_addr_keep)) {
				verbprintf(debug, "assert failed: "
					   "start_addr_keep <="
					   " end_addr_keep\n");
				retval = ULONG_MAX;
				goto out;
			}
		}
		if (start_addr_entry < end_addr_keep &&
		    end_addr_entry > start_addr_keep) {
			if (e->life_start < min_start)
				min_start = e->life_start;
			if (e->life_end > max_end)
				max_end = e->life_end;
			split_entry(e, keep);
		}
	}
	retval = max_end - min_start;
out:
	return retval;
}


/*
 * Within the index range (into the array entries_address_ascending), find the
 * symbol with the maximal lifetime and split/truncate all symbols that overlap
 * with it (i.e. that there won't be any overlaps anymore).
 */
static int handle_overlap_region(int start_idx, int end_idx)
{
	int rc = OP_JIT_CONV_OK;
	int idx;
	struct jitentry * e;
	int cnt;
	char * name;
	int i, j;
	unsigned long long totaltime;

	if (debug) {
		for (i = start_idx; i <= end_idx; i++) {
			e = entries_address_ascending[i];
			verbprintf(debug, "overlap idx=%i, name=%s, "
				   "start=%llx, end=%llx, life_start=%lli, "
				   "life_end=%lli, lifetime=%lli\n",
				   i, e->symbol_name, e->vma,
				   e->vma + e->code_size, e->life_start,
				   e->life_end, e->life_end - e->life_start);
		}
	}
	idx = select_one(start_idx, end_idx);
	totaltime = eliminate_overlaps(start_idx, end_idx, idx);
	if (totaltime == ULONG_MAX) {
		rc = OP_JIT_CONV_FAIL;
		goto out;
	}
	e = entries_address_ascending[idx];

	cnt = 1;
	j = (e->life_end - e->life_start) * 100 / totaltime;
	while ((j = j/10))
		cnt++;

	// Mark symbol name with a %% to indicate the overlap.
	cnt += strlen(e->symbol_name) + 2 + 1;
	name = xmalloc(cnt);
	snprintf(name, cnt, "%s%%%llu", e->symbol_name,
		 (e->life_end - e->life_start) * 100 / totaltime);
	if (e->sym_name_malloced)
		free(e->symbol_name);
	e->symbol_name = name;
	e->sym_name_malloced = 1;
	verbprintf(debug, "selected idx=%i, name=%s\n", idx, e->symbol_name);
out:
	return rc;
}


/*
 * one scan through the symbols to find overlaps.
 * return 1 if an overlap is found. this is repeated until no more overlap 
 * is there.
 * Process: There may be more than two overlapping symbols with each other.
 * The index range of symbols found to overlap are passed to
 * handle_overlap_region.
 */
static int scan_overlaps(void)
{
	int i, j;
	unsigned long long end_addr, end_addr2;
	struct jitentry const * a;
	int flag = 0;
	// entry_count can be incremented by split_entry() during the loop,
	// save the inital value as loop count
	int loop_count = entry_count;

	verbprintf(debug,"count=%i, scan overlaps...\n", entry_count);
	i = 0;
	end_addr = 0;
	for (j = 1; j < loop_count; j++) {
		/**
		 * Take a symbol [i] and look for the next symbol(s) [j] that are overlapping
		 * symbol [i]. If a symbol [j] is found that is not overlapping symbol [i] the
		 * symbols [i]..[j-1] are handled to be not overlapping anymore.
		 * See descriptions of handle_overlap_region() and eliminate_overlaps() for
		 * further details of this handling.
		 * E.g. possible cases to be handled could be:
		 *
		 *   sym1 |-----------|
		 *   sym2     |---|
		 *
		 *   sym1 |--------|
		 *   sym2     |--------|
		 *
		 *   sym1 |--------|
		 *   sym2     |-------|
		 *   sym3        |---------|
		 *
		 * But in the following case
		 *
		 *   sym1 |--------|
		 *   sym2      |-------------|
		 *   sym3              |----------|
		 *
		 * sym3 would not overlap with sym1. Therefore handle_overlap_regio() would
		 * only be called for sym1 up to sym2.
		 */
		a = entries_address_ascending[j - 1];
		end_addr2 = a->vma + a->code_size;
		if (end_addr2 > end_addr)
			end_addr = end_addr2;
		a = entries_address_ascending[j];
		if (end_addr <= a->vma) {
			if (i != j - 1) {
				if (handle_overlap_region(i, j - 1) ==
				    OP_JIT_CONV_FAIL) {
					flag = OP_JIT_CONV_FAIL;
					goto out;
				}
				flag = 1;
			}
			i = j;
		}
	}
	if (i != j - 1) {
		if (handle_overlap_region(i, j - 1) == OP_JIT_CONV_FAIL)
			flag = OP_JIT_CONV_FAIL;
		else
			flag = 1;
	}
out:
	return flag;
}


/* search for symbols that have overlapping address ranges and decide for
 * one */
int resolve_overlaps(unsigned long long start_time)
{
	int rc = OP_JIT_CONV_OK;
	int cnt = 0;

	invalidate_earlybirds(start_time);
	while ((rc = scan_overlaps()) && rc != OP_JIT_CONV_FAIL) {
		resort_address();
		if (cnt == 0) {
			verbprintf(debug, "WARNING: overlaps detected. "
				   "Removing overlapping JIT methods\n");
		}
		cnt++;
	}
	if (cnt > 0 && rc != OP_JIT_CONV_FAIL)
		resort_symbol();
	return rc;
}


/*
 * scan through the sorted array and replace identical symbol names by unique
 * ones by adding a counter value.
 */
void disambiguate_symbol_names(void)
{
	u32 j;
	int cnt, rep_cnt;
	struct jitentry * a;
	struct jitentry * b;

	rep_cnt = 0;
	for (j = 1; j < entry_count; j++) {
		a = entries_symbols_ascending[j - 1];
		cnt = 1;
		do {
			b = entries_symbols_ascending[j];
			if (strcmp(a->symbol_name, b->symbol_name) == 0) {
				if (b->sym_name_malloced)
					free(b->symbol_name);
				b->symbol_name =
					replacement_name(a->symbol_name, cnt);
				b->sym_name_malloced = 1;
				j++;
				cnt++;
				rep_cnt++;
			} else {
				break;
			}
		} while (j < entry_count);
	}
	/* recurse to avoid that the added suffix also creates a collision */
	if (rep_cnt) {
		qsort(entries_symbols_ascending, entry_count,
		      sizeof(struct jitentry *), cmp_symbolname);
		disambiguate_symbol_names();
	}
}
