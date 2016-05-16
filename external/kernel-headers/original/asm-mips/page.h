#ifndef _ASM_PAGE_H
#define _ASM_PAGE_H

#ifndef PAGE_SHIFT
#define PAGE_SHIFT 12
#endif
#define PAGE_SIZE (1UL << PAGE_SHIFT)
#define PAGE_MASK (~((1 << PAGE_SHIFT) - 1))

#endif
