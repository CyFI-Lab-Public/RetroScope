/**
 * @file op_fixmap.c
 * Horrible hacks for compatibility's sake.
 * Based in part on arch/i386/kernel/mpparse.c
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include <linux/mm.h>
#include <linux/init.h>
#include <linux/config.h>
#include <linux/pagemap.h>
#include <asm/io.h>

#include "oprofile.h"
#include "apic_compat.h"

#ifndef cpu_has_pge
#if V_BEFORE(2, 4, 0)
#define cpu_has_pge (test_bit(X86_FEATURE_PGE, &boot_cpu_data.x86_capability)) 
#else
#define cpu_has_pge (test_bit(X86_FEATURE_PGE, boot_cpu_data.x86_capability))
#endif
#endif

unsigned long virt_apic_base;

/* some static commented out to avoid warning, trying to figure out
 * in exactly which circumstances we need this function is too prone
 * error to be made w/o a full rebuild of supported kernel version */
/* how about __attribute__(__unused__) then ? */

/* FIXME is this comment right ? */
/* We don't take care about locking mm->page_table_lock because this is
 * only needed on SMP and on SMP we have already a sensible setup */

/*static*/ void set_pte_phys(ulong vaddr, ulong phys)
{
	pgprot_t prot;
	pgd_t * pgd;
	pmd_t * pmd;
	pte_t * pte;

	pgd = pgd_offset_k(vaddr);
	pmd = pmd_offset(pgd, vaddr);
	pte = pte_offset(pmd, vaddr);
	prot = PAGE_KERNEL;
	/* when !CONFIG_X86_LOCAL_APIC we can't rely on no cache flag set */
	pgprot_val(prot) |= _PAGE_PCD;
	if (cpu_has_pge)
		pgprot_val(prot) |= _PAGE_GLOBAL;
	set_pte(pte, mk_pte_phys(phys, prot));
	__flush_tlb_one(vaddr);
}

/*static*/ void alloc_fixmap(void)
{
	/* dirty hack :/ */
	virt_apic_base = (ulong)vmalloc(4096);
	set_pte_phys(virt_apic_base, APIC_DEFAULT_PHYS_BASE);
}

/*static*/ void free_fixmap(void)
{
	ulong vaddr;
	pgd_t * pgd;
	pmd_t * pmd;
	pte_t * pte;

	vaddr = virt_apic_base;
	if (!vaddr)
		return;

	pgd = pgd_offset_k(vaddr);
	if (!pgd)
		return;

	pmd = pmd_offset(pgd, vaddr);
	if (!pmd)
		return;

	pte = pte_offset(pmd, vaddr);
	if (!pte)
		return;

	/* FIXME: is this the right way */
	pte_clear(pte);
	__flush_tlb_one(vaddr);

	vfree((void*)virt_apic_base);
}

/*
 * Make sure we can access the APIC. Some kernel versions create
 * a meaningless zero-page mapping for the local APIC: we must
 * detect this case and reset it.
 *
 * Some kernel versions/configs won't map the APIC at all, in
 * which case we need to hack it ourselves.
 */
void fixmap_setup(void)
{
#if V_BEFORE(2, 4, 10)
#if defined(CONFIG_X86_LOCAL_APIC)
	static int find_intel_smp(void);

	if (!find_intel_smp()) {
		set_pte_phys(__fix_to_virt(FIX_APIC_BASE),
			APIC_DEFAULT_PHYS_BASE);
		printk(KERN_INFO "oprofile: remapping local APIC.\n");
	}
#else
	alloc_fixmap();
	printk(KERN_INFO "oprofile: mapping APIC.\n");
#endif /* CONFIG_X86_LOCAL_APIC */
#else
#if !defined(CONFIG_X86_LOCAL_APIC)
	alloc_fixmap();
	printk(KERN_INFO "oprofile: mapping APIC.\n");
#endif
#endif
}

void fixmap_restore(void)
{
#if V_BEFORE(2, 4, 10)
#if defined(CONFIG_X86_LOCAL_APIC)
	/* Nothing to do */
#else
	free_fixmap();
	printk(KERN_INFO "oprofile: freeing APIC mapping.\n");
#endif /* CONFIG_X86_LOCAL_APIC */
#else
#if !defined(CONFIG_X86_LOCAL_APIC)
	free_fixmap();
	printk(KERN_INFO "oprofile: freeing APIC mapping.\n");
#endif
#endif
}

/* ---------------- MP table code ------------------ */

#if V_BEFORE(2, 4, 10) && defined(CONFIG_X86_LOCAL_APIC)

static int __init mpf_checksum(unsigned char * mp, int len)
{
	int sum = 0;

	while (len--)
		sum += *mp++;

	return sum & 0xFF;
}

static int __init mpf_table_ok(struct intel_mp_floating * mpf, unsigned long * bp)
{
	if (*bp != SMP_MAGIC_IDENT)
		return 0;
	if (mpf->mpf_length != 1)
		return 0;
	if (mpf_checksum((unsigned char *)bp, 16))
		return 0;

	return (mpf->mpf_specification == 1 || mpf->mpf_specification == 4);
}

static int __init smp_scan_config (unsigned long base, unsigned long length)
{
	unsigned long * bp = phys_to_virt(base);
	struct intel_mp_floating * mpf;

	while (length > 0) {
		mpf = (struct intel_mp_floating *)bp;
		if (mpf_table_ok(mpf, bp))
			return 1;
		bp += 4;
		length -= 16;
	}
	return 0;
}

static int __init find_intel_smp(void)
{
	unsigned int address;

	if (smp_scan_config(0x0, 0x400) ||
		smp_scan_config(639*0x400, 0x400) ||
		smp_scan_config(0xF0000, 0x10000))
		return 1;

	address = *(unsigned short *)phys_to_virt(0x40E);
	address <<= 4;
	return smp_scan_config(address, 0x1000);
}

#endif /* V_BEFORE(2,4,10) && defined(CONFIG_X86_LOCAL_APIC) */
