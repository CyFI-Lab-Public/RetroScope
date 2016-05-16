#ifndef BARRIER_H
#define BARRIER_H

#if defined(__ia64__)
  #define store_barrier()         asm volatile ("mf" ::: "memory")
#elif defined(__x86_64__)
  #define store_barrier()         asm volatile("sfence" ::: "memory")
#elif defined(__i386__)
  #define store_barrier()         asm volatile ("": : :"memory")
#elif defined(__ppc__) || defined(__powerpc__)
  #define store_barrier()         asm volatile ("eieio" : : : "memory")
#elif defined(__s390__) || defined(__s390x__)
  #define store_barrier()         asm volatile ("bcr 15,0" : : : "memory")
#elif defined(__alpha__)
  #define store_barrier()         asm volatile("wmb": : :"memory")
#elif defined(__hppa__)
  #define store_barrier()         asm volatile("":::"memory")
#elif defined(__sparc__)
  #define store_barrier()         asm volatile("":::"memory")
#elif defined(__m68000__) || defined(__m68k__) || defined(mc68000) || defined(_M_M68K)
  #define store_barrier()         asm volatile("":::"memory")
#elif defined(__mips__)  /* also mipsel */
  #define store_barrier()         do { } while(0)
#elif defined(__arm__)
  /* taken from linux/arch/arm/kernel/entry-armv.S, thanks to pbrook! */
  typedef void (__kernel_dmb_t)(void);
  #define __kernel_dmb (*(__kernel_dmb_t *)0xffff0fa0)
  #define store_barrier()        __kernel_dmb()
#else
  #error Define store_barrier() for your CPU
#endif

#endif
