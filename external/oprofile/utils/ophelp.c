/**
 * @file ophelp.c
 * Print out PMC event information
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>

#include "op_version.h"
#include "op_events.h"
#include "op_popt.h"
#include "op_cpufreq.h"
#include "op_hw_config.h"
#include "op_string.h"
#include "op_alloc_counter.h"
#include "op_parse_event.h"
#include "op_libiberty.h"
#include "op_xml_events.h"

static char const ** chosen_events;
static int num_chosen_events;
struct parsed_event * parsed_events;
static op_cpu cpu_type = CPU_NO_GOOD;
static char * cpu_string;
static int callgraph_depth;
static int want_xml;

static poptContext optcon;


/// return the Hamming weight (number of set bits)
static size_t hweight(size_t mask)
{
	size_t count = 0;

	while (mask) {
		mask &= mask - 1;
		count++;
	}

	return count;
}

static void do_arch_specific_event_help(struct op_event * event)
{
	switch (cpu_type) {
	case CPU_PPC64_CELL:
		printf("Group %u :", event->val / 100);
		break;
	default:
		break;
	}
}

#define LINE_LEN 99

static void word_wrap(int indent, int *column, char *msg)
{
	while (*msg) {
		int wlen = strcspn(msg, " ");
		if (*column + wlen > LINE_LEN) {
			printf("\n%*s", indent, "");
			*column = indent;
		}
		printf("%.*s ", wlen, msg);
		*column += wlen + 1;
		msg += wlen;
		msg += strspn(msg, " ");
	}
}

/**
 * help_for_event - output event name and description
 * @param i  event number
 *
 * output an help string for the event @i
 */
static void help_for_event(struct op_event * event)
{
	int column;
	uint i, j;
	uint mask;
	size_t nr_counters;
	char buf[32];

	do_arch_specific_event_help(event);
	nr_counters = op_get_nr_counters(cpu_type);

	/* Sanity check */
	if (!event)
		return;

	printf("%s", event->name);

	if(event->counter_mask != 0) {
		printf(": (counter: ");

		mask = event->counter_mask;
		if (hweight(mask) == nr_counters) {
			printf("all");
		} else {
			for (i = 0; i < CHAR_BIT * sizeof(event->counter_mask); ++i) {
				if (mask & (1 << i)) {
					printf("%d", i);
					mask &= ~(1 << i);
					if (mask)
						printf(", ");
				}
			}
		}
	} else	if (event->ext != NULL) {
		/* Handling extended feature interface */
		printf(": (ext: %s", event->ext);
	} else {
		/* Handling arch_perfmon case */
		printf(": (counter: all");
	}   

	printf(")\n\t");
	column = 8;
	word_wrap(8, &column, event->desc);
	snprintf(buf, sizeof buf, "(min count: %d)", event->min_count);
	word_wrap(8, &column, buf);
	putchar('\n');

	if (strcmp(event->unit->name, "zero")) {

		printf("\tUnit masks (default 0x%x)\n",
		       event->unit->default_mask);
		printf("\t----------\n");

		for (j = 0; j < event->unit->num; j++) {
			printf("\t0x%.2x: ",
			       event->unit->um[j].value);
			column = 14;
			word_wrap(14, &column, event->unit->um[j].desc);
			putchar('\n');
		}
	}
}


static void check_event(struct parsed_event * pev,
			struct op_event const * event)
{
	int ret;
	int min_count;
	int const callgraph_min_count_scale = 15;

	if (!event) {
		event = find_event_by_name(pev->name, 0, 0);
		if (event)
			fprintf(stderr, "Invalid unit mask %x for event %s\n",
				pev->unit_mask, pev->name);
		else
			fprintf(stderr, "No event named %s is available.\n",
				pev->name);
		exit(EXIT_FAILURE);
	}

	ret = op_check_events(0, event->val, pev->unit_mask, cpu_type);

	if (ret & OP_INVALID_UM) {
		fprintf(stderr, "Invalid unit mask 0x%x for event %s\n",
		        pev->unit_mask, pev->name);
		exit(EXIT_FAILURE);
	}

	min_count = event->min_count;
	if (callgraph_depth)
		min_count *= callgraph_min_count_scale;
	if (pev->count < min_count) {
		fprintf(stderr, "Count %d for event %s is below the "
		        "minimum %d\n", pev->count, pev->name, min_count);
		exit(EXIT_FAILURE);
	}
}


static void resolve_events(void)
{
	size_t count, count_events;
	size_t i, j;
	size_t * counter_map;
	size_t nr_counters = op_get_nr_counters(cpu_type);
	struct op_event const * selected_events[num_chosen_events];

	count = parse_events(parsed_events, num_chosen_events, chosen_events);

	for (i = 0; i < count; ++i) {
		for (j = i + 1; j < count; ++j) {
			struct parsed_event * pev1 = &parsed_events[i];
			struct parsed_event * pev2 = &parsed_events[j];

			if (!strcmp(pev1->name, pev2->name) &&
			    pev1->count == pev2->count &&
			    pev1->unit_mask == pev2->unit_mask &&
			    pev1->kernel == pev2->kernel &&
			    pev1->user == pev2->user) {
				fprintf(stderr, "All events must be distinct.\n");
				exit(EXIT_FAILURE);
			}
		}
	}

	for (i = 0, count_events = 0; i < count; ++i) {
		struct parsed_event * pev = &parsed_events[i];

		/* For 0 unit mask always do wild card match */
		selected_events[i] = find_event_by_name(pev->name, pev->unit_mask,
					pev->unit_mask ? pev->unit_mask_valid : 0);
		check_event(pev, selected_events[i]);

		if (selected_events[i]->ext == NULL) {
			count_events++;
		}
	}
	if (count_events > nr_counters) {
		fprintf(stderr, "Not enough hardware counters. "
				"Need %lu counters but only has %lu.\n",
				(unsigned long) count_events,
				(unsigned long) nr_counters);
		exit(EXIT_FAILURE);
	}

	counter_map = map_event_to_counter(selected_events, count, cpu_type);

	if (!counter_map) {
		fprintf(stderr, "Couldn't allocate hardware counters for the selected events.\n");
		exit(EXIT_FAILURE);
	}

	for (i = 0; i < count; ++i)
		if(counter_map[i] == (size_t)-1)
			if (selected_events[i]->ext != NULL)
				printf("%s ", (char*) selected_events[i]->ext);
			else
				printf("N/A ");
		else
			printf("%d ", (unsigned int) counter_map[i]);
	printf("\n");

	free(counter_map);
}


static void show_unit_mask(void)
{
	struct op_event * event;
	size_t count;

	count = parse_events(parsed_events, num_chosen_events, chosen_events);
	if (count > 1) {
		fprintf(stderr, "More than one event specified.\n");
		exit(EXIT_FAILURE);
	}

	event = find_event_by_name(parsed_events[0].name, 0, 0);

	if (!event) {
		fprintf(stderr, "No such event found.\n");
		exit(EXIT_FAILURE);
	}

	printf("%d\n", event->unit->default_mask);
}


static void show_default_event(void)
{
	struct op_default_event_descr descr;

	op_default_event(cpu_type, &descr);

	if (descr.name[0] == '\0')
		return;

	printf("%s:%lu:%lu:1:1\n", descr.name, descr.count, descr.um);
}


static int show_vers;
static int get_cpu_type;
static int check_events;
static int unit_mask;
static int get_default_event;

static struct poptOption options[] = {
	{ "cpu-type", 'c', POPT_ARG_STRING, &cpu_string, 0,
	  "use the given CPU type", "cpu type", },
	{ "check-events", 'e', POPT_ARG_NONE, &check_events, 0,
	  "check the given event descriptions for validity", NULL, },
	{ "unit-mask", 'u', POPT_ARG_NONE, &unit_mask, 0,
	  "default unit mask for the given event", NULL, },
	{ "get-cpu-type", 'r', POPT_ARG_NONE, &get_cpu_type, 0,
	  "show the auto-detected CPU type", NULL, },
	{ "get-default-event", 'd', POPT_ARG_NONE, &get_default_event, 0,
	  "get the default event", NULL, },
	{ "callgraph", '\0', POPT_ARG_INT, &callgraph_depth, 0,
	  "use this callgraph depth", "callgraph depth", },
	{ "version", 'v', POPT_ARG_NONE, &show_vers, 0,
	   "show version", NULL, },
	{ "xml", 'X', POPT_ARG_NONE, &want_xml, 0,
	   "list events as XML", NULL, },
	POPT_AUTOHELP
	{ NULL, 0, 0, NULL, 0, NULL, NULL, },
};

/**
 * get_options - process command line
 * @param argc  program arg count
 * @param argv  program arg array
 *
 * Process the arguments, fatally complaining on error.
 */
static void get_options(int argc, char const * argv[])
{
	optcon = op_poptGetContext(NULL, argc, argv, options, 0);

	if (show_vers)
		show_version(argv[0]);

	/* non-option, must be a valid event name or event specs */
	chosen_events = poptGetArgs(optcon);

	if(chosen_events) {
		num_chosen_events = 0;
		while (chosen_events[num_chosen_events] != NULL)
			num_chosen_events++;
	}

	/* don't free the context now, we need chosen_events */
}


/** make valgrind happy */
static void cleanup(void)
{
	int i;
	if (parsed_events) {
		for (i = 0; i < num_chosen_events; ++i) {
			if (parsed_events[i].name)
				free(parsed_events[i].name);
		}
	}
	op_free_events();
	if (optcon)
		poptFreeContext(optcon);
	if (parsed_events)
		free(parsed_events);
}


#define MAX_LINE 256
int main(int argc, char const * argv[])
{
	struct list_head * events;
	struct list_head * pos;
	char const * pretty;
	char title[10 * MAX_LINE];
	char const * event_doc = "";

	atexit(cleanup);

	get_options(argc, argv);

	/* usefull for testing purpose to allow to force the cpu type
	 * with --cpu-type */
	if (cpu_string) {
		cpu_type = op_get_cpu_number(cpu_string);
	} else {
		cpu_type = op_get_cpu_type();
	}

	if (cpu_type == CPU_NO_GOOD) {
		fprintf(stderr, "cpu_type '%s' is not valid\n",
		        cpu_string ? cpu_string : "unset");
		fprintf(stderr, "you should upgrade oprofile or force the "
			"use of timer mode\n");
		exit(EXIT_FAILURE);
	}

	parsed_events = (struct parsed_event *)xcalloc(num_chosen_events,
		sizeof(struct parsed_event));

	pretty = op_get_cpu_type_str(cpu_type);

	if (get_cpu_type) {
		printf("%s\n", pretty);
		exit(EXIT_SUCCESS);
	}

	if (get_default_event) {
		show_default_event();
		exit(EXIT_SUCCESS);
	}

	if (cpu_type == CPU_TIMER_INT) {
		if (!check_events)
			printf("Using timer interrupt.\n");
		exit(EXIT_SUCCESS);
	}

	events = op_events(cpu_type);

	if (!chosen_events && (unit_mask || check_events)) {
		fprintf(stderr, "No events given.\n");
		exit(EXIT_FAILURE);
	}

	if (unit_mask) {
		show_unit_mask();
		exit(EXIT_SUCCESS);
	}

	if (check_events) {
		resolve_events();
		exit(EXIT_SUCCESS);
	}

	/* without --check-events, the only argument must be an event name */
	if (chosen_events && chosen_events[0]) {
		if (chosen_events[1]) {
			fprintf(stderr, "Too many arguments.\n");
			exit(EXIT_FAILURE);
		}

		list_for_each(pos, events) {
			struct op_event * event = list_entry(pos, struct op_event, event_next);

			if (strcmp(event->name, chosen_events[0]) == 0) {
				char const * map = find_mapping_for_event(event->val, cpu_type);
				if (map) {
					printf("%d %s\n", event->val, map);
				} else {
					printf("%d\n", event->val);
				}
				exit(EXIT_SUCCESS);
			}
		}
		fprintf(stderr, "No such event \"%s\"\n", chosen_events[0]);
		exit(EXIT_FAILURE);
	}

	/* default: list all events */

	switch (cpu_type) {
	case CPU_HAMMER:
		event_doc =
			"See BIOS and Kernel Developer's Guide for AMD Athlon and AMD Opteron Processors\n"
			"(26094.pdf), Section 10.2\n\n";
		break;
	case CPU_FAMILY10:
		event_doc =
			"See BIOS and Kernel Developer's Guide for AMD Family 10h Processors\n"
			"(31116.pdf), Section 3.14\n\n";
		break;
	case CPU_FAMILY11H:
		event_doc =
			"See BIOS and Kernel Developer's Guide for AMD Family 11h Processors\n"
			"(41256.pdf), Section 3.14\n\n";
		break;
	case CPU_FAMILY12H:
		event_doc =
			"See BIOS and Kernel Developer's Guide for AMD Family 12h Processors\n";
		break;
	case CPU_FAMILY14H:
		event_doc =
			"See BIOS and Kernel Developer's Guide for AMD Family 14h Processors\n";
		break;
	case CPU_FAMILY15H:
		event_doc =
			"See BIOS and Kernel Developer's Guide for AMD Family 15h Processors\n";
		break;
	case CPU_ATHLON:
		event_doc =
			"See AMD Athlon Processor x86 Code Optimization Guide\n"
			"(22007.pdf), Appendix D\n\n";
		break;
	case CPU_PPRO:
	case CPU_PII:
	case CPU_PIII:
	case CPU_P6_MOBILE:
	case CPU_P4:
	case CPU_P4_HT2:
	case CPU_CORE:
	case CPU_CORE_2:
	case CPU_CORE_I7:
	case CPU_NEHALEM:
	case CPU_WESTMERE:
	case CPU_ATOM:
		event_doc =
			"See Intel Architecture Developer's Manual Volume 3B, Appendix A and\n"
			"Intel Architecture Optimization Reference Manual (730795-001)\n\n";
		break;

	case CPU_ARCH_PERFMON:
		event_doc =
			"See Intel 64 and IA-32 Architectures Software Developer's Manual\n"
			"Volume 3B (Document 253669) Chapter 18 for architectural perfmon events\n"
			"This is a limited set of fallback events because oprofile doesn't know your CPU\n";
		break;
	
	case CPU_IA64:
	case CPU_IA64_1:
	case CPU_IA64_2:
		event_doc =
			"See Intel Itanium Processor Reference Manual\n"
			"for Software Development (Document 245320-003),\n"
			"Intel Itanium Processor Reference Manual\n"
			"for Software Optimization (Document 245473-003),\n"
			"Intel Itanium 2 Processor Reference Manual\n"
			"for Software Development and Optimization (Document 251110-001)\n\n";
		break;
	case CPU_AXP_EV4:
	case CPU_AXP_EV5:
	case CPU_AXP_PCA56:
	case CPU_AXP_EV6:
	case CPU_AXP_EV67:
		event_doc =
			"See Alpha Architecture Reference Manual\n"
			"http://download.majix.org/dec/alpha_arch_ref.pdf\n";
		break;
	case CPU_ARM_XSCALE1:
	case CPU_ARM_XSCALE2:
		event_doc =
			"See Intel XScale Core Developer's Manual\n"
			"Chapter 8 Performance Monitoring\n";
		break;
	case CPU_ARM_MPCORE:
		event_doc =
			"See ARM11 MPCore Processor Technical Reference Manual r1p0\n"
			"Page 3-70, performance counters\n";
		break;

	case CPU_ARM_V6:
		event_doc = "See ARM11 Technical Reference Manual\n";
  		break;

	case CPU_ARM_V7:
		event_doc =
			"See Cortex-A8 Technical Reference Manual\n"
			"Cortex A8 DDI (ARM DDI 0344B, revision r1p1)\n";
		break;

	case CPU_ARM_V7_CA9:
		event_doc =
			"See Cortex-A9 Technical Reference Manual\n"
			"Cortex A9 DDI (ARM DDI 0388E, revision r2p0)\n";
		break;

	case CPU_PPC64_PA6T:
		event_doc =
			"See PA6T Power Implementation Features Book IV\n"
			"Chapter 7 Performance Counters\n";
		break;

	case CPU_PPC64_POWER4:
	case CPU_PPC64_POWER5:
	case CPU_PPC64_POWER6:
	case CPU_PPC64_POWER5p:
	case CPU_PPC64_POWER5pp:
	case CPU_PPC64_970:
	case CPU_PPC64_970MP:
	case CPU_PPC64_POWER7:
	case CPU_PPC64_IBM_COMPAT_V1:
		event_doc =
			"Obtain PowerPC64 processor documentation at:\n"
			"http://www-306.ibm.com/chips/techlib/techlib.nsf/productfamilies/PowerPC\n";
		break;

	case CPU_PPC64_CELL:
		event_doc =
			"Obtain Cell Broadband Engine documentation at:\n"
			"http://www-306.ibm.com/chips/techlib/techlib.nsf/products/Cell_Broadband_Engine\n";
		break;

	case CPU_MIPS_20K:
		event_doc =
			"See Programming the MIPS64 20Kc Processor Core User's "
		"manual available from www.mips.com\n";
		break;
	case CPU_MIPS_24K:
		event_doc =
			"See Programming the MIPS32 24K Core "
			"available from www.mips.com\n";
		break;
	case CPU_MIPS_25K:
		event_doc =
			"See Programming the MIPS64 25Kf Processor Core User's "
			"manual available from www.mips.com\n";
		break;
	case CPU_MIPS_34K:
		event_doc =
			"See Programming the MIPS32 34K Core Family "
			"available from www.mips.com\n";
		break;
	case CPU_MIPS_74K:
		event_doc =
			"See Programming the MIPS32 74K Core Family "
			"available from www.mips.com\n";
		break;
	case CPU_MIPS_1004K:
		event_doc =
			"See Programming the MIPS32 1004K Core Family "
			"available from www.mips.com\n";
		break;
	case CPU_MIPS_5K:
		event_doc =
			"See Programming the MIPS64 5K Processor Core Family "
			"Software User's manual available from www.mips.com\n";
		break;
	case CPU_MIPS_R10000:
	case CPU_MIPS_R12000:
		event_doc =
			"See NEC R10000 / R12000 User's Manual\n"
			"http://www.necelam.com/docs/files/U10278EJ3V0UM00.pdf\n";
		break;
	case CPU_MIPS_RM7000:
		event_doc =
			"See RM7000 Family User Manual "
			"available from www.pmc-sierra.com\n";
		break;
	case CPU_MIPS_RM9000:
		event_doc =
			"See RM9000x2 Family User Manual "
			"available from www.pmc-sierra.com\n";
		break;
	case CPU_MIPS_SB1:
	case CPU_MIPS_VR5432:
		event_doc =
			"See NEC VR5443 User's Manual, Volume 1\n"
			"http://www.necelam.com/docs/files/1375_V1.pdf\n";
		break;
	case CPU_MIPS_VR5500:
		event_doc =
			"See NEC R10000 / R12000 User's Manual\n"
			"http://www.necel.com/nesdis/image/U16677EJ3V0UM00.pdf\n";
		break;

	case CPU_MIPS_LOONGSON2:
		event_doc = 
			"See loongson2 RISC Microprocessor Family Reference Manual\n";
		break;

	case CPU_PPC_E500:
	case CPU_PPC_E500_2:
		event_doc =
			"See PowerPC e500 Core Complex Reference Manual\n"
			"Chapter 7: Performance Monitor\n"
			"Downloadable from http://www.freescale.com\n";
		break;

	case CPU_PPC_E300:
		event_doc =
			"See PowerPC e300 Core Reference Manual\n"
			"Downloadable from http://www.freescale.com\n";
		break;

	case CPU_PPC_7450:
		event_doc =
			"See MPC7450 RISC Microprocessor Family Reference "
			"Manual\n"
			"Chapter 11: Performance Monitor\n"
			"Downloadable from http://www.freescale.com\n";
		break;

	case CPU_AVR32:
		event_doc =
			"See AVR32 Architecture Manual\n"
			"Chapter 6: Performance Counters\n"
			"http://www.atmel.com/dyn/resources/prod_documents/doc32000.pdf\n";

		case CPU_RTC:
			break;

		// don't use default, if someone add a cpu he wants a compiler warning
		// if he forgets to handle it here.
		case CPU_TIMER_INT:
		case CPU_NO_GOOD:
		case MAX_CPU_TYPE:
			printf("%d is not a valid processor type.\n", cpu_type);
			exit(EXIT_FAILURE);
	}

	sprintf(title, "oprofile: available events for CPU type \"%s\"\n\n", pretty);
	if (want_xml)
		open_xml_events(title, event_doc, cpu_type);
	else
		printf("%s%s", title, event_doc);

	list_for_each(pos, events) {
		struct op_event * event = list_entry(pos, struct op_event, event_next);
		if (want_xml) 
			xml_help_for_event(event);
		else
			help_for_event(event);
	}

	if (want_xml)
		close_xml_events();

	return EXIT_SUCCESS;
}
