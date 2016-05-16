/**
 * @file op_util.c
 * Various utility functions
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <linux/vmalloc.h>
#include <linux/wrapper.h>
#include <linux/pagemap.h>

#include "compat.h"

#include "op_util.h"
 
/* Given PGD from the address space's page table, return the kernel
 * virtual mapping of the physical memory mapped at ADR.
 */
static inline unsigned long uvirt_to_kva(pgd_t * pgd, unsigned long adr)
{
	unsigned long ret = 0UL;
	pmd_t * pmd;
	pte_t * ptep, pte;

	if (!pgd_none(*pgd)) {
		pmd = pmd_offset(pgd, adr);
		if (!pmd_none(*pmd)) {
			ptep = pte_offset(pmd, adr);
			pte = *ptep;
			if (pte_present(pte)) {
				ret = (unsigned long) pte_page_address(pte);
				ret |= adr & (PAGE_SIZE - 1);
			}
		}
	}
	return ret;
}

/* Here we want the physical address of the memory.
 * This is used when initializing the contents of the
 * area and marking the pages as reserved.
 */
unsigned long kvirt_to_pa(unsigned long adr)
{
	unsigned long va, kva, ret;

	va = VMALLOC_VMADDR(adr);
	kva = uvirt_to_kva(pgd_offset_k(va), va);
	ret = __pa(kva);
	return ret;
}

void * rvmalloc(signed long size)
{
	void * mem;
	unsigned long adr, page;

	mem = VMALLOC_32(size);
	if (!mem)
		return NULL;

	memset(mem, 0, size);

	adr=(unsigned long) mem;
	while (size > 0) {
		page = kvirt_to_pa(adr);
		mem_map_reserve(virt_to_page((unsigned long)__va(page)));
		adr += PAGE_SIZE;
		size -= PAGE_SIZE;
	}
	return mem;
}

void rvfree(void * mem, signed long size)
{
	unsigned long adr, page;

	if (!mem)
		return;

	adr=(unsigned long) mem;
	while (size > 0) {
		page = kvirt_to_pa(adr);
		mem_map_unreserve(virt_to_page((unsigned long)__va(page)));

		adr += PAGE_SIZE;
		size -= PAGE_SIZE;
	}
	vfree(mem);
}

int check_range(int val, int l, int h, char const * msg)
{
	if (val < l || val > h) {
		printk(msg, val, l, h);
		return -EINVAL;
	}
	return 0;
} 
