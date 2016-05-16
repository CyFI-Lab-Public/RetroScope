#include "hijack.h"

#include <cstddef>
#include <cstdlib>
#include <cstdio>
#include <cstring>
#include "../ds/list.h"
//extern "C"
//{
//  #include <asm/cacheflush.h>
//}

using namespace std;

#if defined(_CONFIG_X86_)
    #define HIJACK_SIZE 6
#elif defined(_CONFIG_X86_64_)
    #define HIJACK_SIZE 12
#else // ARM
    #define HIJACK_SIZE 12
#endif

struct sym_hook {
    void *addr;
    unsigned char o_code[HIJACK_SIZE];
    unsigned char n_code[HIJACK_SIZE];
};

BDS_List<sym_hook> hooked_syms;

#if defined(_CONFIG_X86_) || defined(_CONFIG_X86_64_)
inline unsigned long disable_wp ( void )
{
    unsigned long cr0;

    preempt_disable();
    barrier();

    cr0 = read_cr0();
    write_cr0(cr0 & ~X86_CR0_WP);
    return cr0;
}

inline void restore_wp ( unsigned long cr0 )
{
    write_cr0(cr0);

    barrier();
    preempt_enable();
}
#else // ARM
//void cacheflush ( void *begin, unsigned long size )
//{
//    flush_icache_range((unsigned long)begin, (unsigned long)begin + size);
//}

# if defined(CONFIG_STRICT_MEMORY_RWX)
inline void arm_write_hook ( void *target, char *code )
{
    unsigned long *target_arm = (unsigned long *)target;
    unsigned long *code_arm = (unsigned long *)code;

    // We should have something more generalized here, but we'll
    // get away with it since the ARM hook is always 12 bytes
    mem_text_write_kernel_word(target_arm, *code_arm);
    mem_text_write_kernel_word(target_arm + 1, *(code_arm + 1));
    mem_text_write_kernel_word(target_arm + 2, *(code_arm + 2));
}
# else
inline void arm_write_hook ( void *target, char *code )
{
    memcpy(target, code, HIJACK_SIZE);
//    cacheflush(target, HIJACK_SIZE);
}
# endif
#endif

void hijack_start ( void *target, void *hook )
{
    hijack_stop (target); // clear it if it is already hijacked

    sym_hook sa;
    sa.addr = target;

    #if defined(_CONFIG_X86_)
    unsigned long o_cr0;

    // push $addr; ret
    memcpy(sa.n_code, "\x68\x00\x00\x00\x00\xc3", HIJACK_SIZE);
    *(unsigned long *)&(sa.n_code[1]) = (unsigned long)hook;
    #elif defined(_CONFIG_X86_64_)
    unsigned long o_cr0;

    // mov rax, $addr; jmp rax
    memcpy(n_code, "\x48\xb8\x00\x00\x00\x00\x00\x00\x00\x00\xff\xe0", HIJACK_SIZE);
    *(unsigned long *)&(sa.n_code[2]) = (unsigned long)hook;
    #else // ARM
    if ( (unsigned long)target % 4 == 0 )
    {
        // ldr pc, [pc, #0]; .long addr; .long addr
        memcpy(sa.n_code, "\x00\xf0\x9f\xe5\x00\x00\x00\x00\x00\x00\x00\x00", HIJACK_SIZE);
        unsigned long * temp = (unsigned long *)(void *)(&(sa.n_code[4]));
        *temp = (unsigned long)hook;
        temp = (unsigned long *)(void *)(&(sa.n_code[8]));
        *temp = (unsigned long)hook;
    }
    else // Thumb
    {
        // add r0, pc, #4; ldr r0, [r0, #0]; mov pc, r0; mov pc, r0; .long addr
        memcpy(sa.n_code, "\x01\xa0\x00\x68\x87\x46\x87\x46\x00\x00\x00\x00", HIJACK_SIZE);
        *(unsigned long *)(void *)&(sa.n_code[8]) = (unsigned long)hook;
        target = (void *)((unsigned)target-4);
    }
    #endif

    memcpy(sa.o_code, target, HIJACK_SIZE);

    #if defined(_CONFIG_X86_) || defined(_CONFIG_X86_64_)
    o_cr0 = disable_wp();
    memcpy(target,sa. n_code, HIJACK_SIZE);
    restore_wp(o_cr0);
    #else // ARM
    arm_write_hook(target, (char *)(sa.n_code));
    #endif

    hooked_syms.push_back(sa);
}

void hijack_pause ( void *target )
{
    for(BDS_List<sym_hook>::iterator it = hooked_syms.begin();
        it != hooked_syms.end(); ++it)
    {
        if ( target == it->addr )
        {
            #if defined(_CONFIG_X86_) || defined(_CONFIG_X86_64_)
            unsigned long o_cr0 = disable_wp();
            memcpy(target, it->o_code, HIJACK_SIZE);
            restore_wp(o_cr0);
            #else // ARM
            arm_write_hook(target, (char *)(it->o_code));
            #endif
        }
    }
}

void hijack_resume ( void *target )
{
    for(BDS_List<sym_hook>::iterator it = hooked_syms.begin();
        it != hooked_syms.end(); ++it)
    {
        if ( target == it->addr )
        {
            #if defined(_CONFIG_X86_) || defined(_CONFIG_X86_64_)
            unsigned long o_cr0 = disable_wp();
            memcpy(target, it->n_code, HIJACK_SIZE);
            restore_wp(o_cr0);
            #else // ARM
            arm_write_hook(target, (char *)(it->n_code));
            #endif
        }
    }
}

void hijack_stop ( void *target )
{
    for(BDS_List<sym_hook>::iterator it = hooked_syms.begin();
        it != hooked_syms.end(); ++it)
    {
        if ( target == it->addr )
        {
            #if defined(_CONFIG_X86_) || defined(_CONFIG_X86_64_)
            unsigned long o_cr0 = disable_wp();
            memcpy(target, it->o_code, HIJACK_SIZE);
            restore_wp(o_cr0);
            #else // ARM
            arm_write_hook(target, (char *)(it->o_code));
            #endif
            
            hooked_syms.erase(it);
            break;
        }
    }
}
