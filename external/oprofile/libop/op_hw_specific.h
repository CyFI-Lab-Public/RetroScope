/* 
 * @file architecture specific interfaces
 * @remark Copyright 2008 Intel Corporation
 * @remark Read the file COPYING
 * @author Andi Kleen
 */

#if (defined(__i386__) || defined(__x86_64__)) && !defined(ANDROID_HOST)

/* Assume we run on the same host as the profilee */

#define num_to_mask(x) ((1U << (x)) - 1)

static inline int cpuid_vendor(char *vnd)
{
	union {
		struct {
			unsigned b,d,c;
		};
		char v[12];
	} v;
	unsigned eax;
#ifdef __PIC__
        __asm__ __volatile__(
            "pushl %%ebx\n"      /* must be preserved due to PIC code */
            "cpuid\n"
            "mov %%ebx, 0(%%edi)\n"
            "mov %%ecx, 4(%%edi)\n"
            "mov %%edx, 8(%%edi)\n"
            "popl %%ebx\n"
            : "=a" (eax)
            : "a"(0), "D"(v.v)
            : "%ecx", "%edx"
        );
#else
	asm("cpuid" : "=a" (eax), "=b" (v.b), "=c" (v.c), "=d" (v.d) : "0" (0));
#endif
	return !strncmp(v.v, vnd, 12);
}

static inline unsigned arch_cpuid_1(int code)
{
    unsigned val;
#ifdef __PIC__
        __asm__ __volatile__ (
            "pushl %%ebx\n"
            "cpuid\n"
            "popl %%ebx\n"
            : "=a" (val)
            : "a" (code)
            : "ecx", "edx"
        );
#else
        asm("cpuid" : "=a" (v.eax) : "a" (code) : "ecx","ebx","edx");
#endif
        return val;
}

static inline unsigned int cpuid_signature()
{
	return arch_cpuid_1(1);
}

static inline unsigned int cpu_model(unsigned int eax)
{
	unsigned model = (eax & 0xf0) >> 4;
	unsigned ext_model = (eax & 0xf0000) >> 12;
	return  ext_model + model;
}

static inline unsigned int cpu_family(unsigned int eax)
{
	unsigned family =  (eax & 0xf00) >> 8;
	unsigned ext_family = (eax & 0xff00000) >> 20;
	return ext_family + family;
}

static inline unsigned int cpu_stepping(unsigned int eax)
{
	return (eax & 0xf);
}


/* Work around Nehalem spec update AAJ79: CPUID incorrectly indicates
   unhalted reference cycle architectural event is supported. We assume
   steppings after C0 report correct data in CPUID. */
static inline void workaround_nehalem_aaj79(unsigned *ebx)
{
	unsigned eax;

	if (!cpuid_vendor("GenuineIntel"))
		return;
	eax = cpuid_signature();
	if (cpu_family(eax) != 6 || cpu_model(eax) != 26
		|| cpu_stepping(eax) > 4)
		return;
	*ebx |= (1 << 2);	/* disable unsupported event */
}

static inline unsigned arch_get_filter(op_cpu cpu_type)
{
	if (op_cpu_base_type(cpu_type) == CPU_ARCH_PERFMON) { 
		unsigned ebx, eax;
#ifdef __PIC__
                __asm__ __volatile__ (
                    "pushl %%ebx\n"
                    "cpuid\n"
                    "mov %%ebx, %%ecx\n"
                    "popl %%ebx"
                    : "=a" (eax), "=c" (ebx)
                    : "a" (0xa)
                    : "edx"
                );
#else
		asm("cpuid" : "=a" (eax), "=b" (ebx) : "0" (0xa) : "ecx","edx");
#endif
		workaround_nehalem_aaj79(&ebx);
		return ebx & num_to_mask(eax >> 24);
	}
	return -1U;
}

static inline int arch_num_counters(op_cpu cpu_type) 
{
	if (op_cpu_base_type(cpu_type) == CPU_ARCH_PERFMON) {
		unsigned v = arch_cpuid_1(0xa);
		return (v >> 8) & 0xff;
	} 
	return -1;
}

static inline unsigned arch_get_counter_mask(void)
{
	unsigned v = arch_cpuid_1(0xa);
	return num_to_mask((v >> 8) & 0xff);
}

static inline op_cpu op_cpu_specific_type(op_cpu cpu_type)
{
	if (cpu_type == CPU_ARCH_PERFMON) {
		/* Already know is Intel family 6, so just check the model. */
		int model = cpu_model(cpuid_signature());
		switch(model) {
		case 0x0f:
		case 0x16:
		case 0x17:
		case 0x1d:
			return CPU_CORE_2;
		case 0x1a:
		case 0x1e:
		case 0x2e:
			return CPU_CORE_I7;
		case 0x1c:
			return CPU_ATOM;
		case 0x25:
			return CPU_WESTMERE;
		}
	}
	return cpu_type;
}

#else

static inline unsigned arch_get_filter(op_cpu cpu_type)
{
	/* Do something with passed arg to shut up the compiler warning */
	if (cpu_type != CPU_NO_GOOD)
		return 0;
	return 0;
}

static inline int arch_num_counters(op_cpu cpu_type) 
{
	/* Do something with passed arg to shut up the compiler warning */
	if (cpu_type != CPU_NO_GOOD)
		return -1;
	return -1;
}

static inline unsigned arch_get_counter_mask(void)
{
	return 0;
}

static inline op_cpu op_cpu_specific_type(op_cpu cpu_type)
{
	return cpu_type;
}
#endif
