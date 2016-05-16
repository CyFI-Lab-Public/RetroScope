/**
 * @file op_mangle.c
 * Mangling of sample file names
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#include "op_mangle.h"

#include <string.h>
#include <stdio.h>

#include "op_libiberty.h"

#include "op_sample_file.h"
#include "op_config.h"

static void append_image(char * dest, int flags, int anon, char const * name, char const * anon_name)
{
	if ((flags & MANGLE_KERNEL) && !strchr(name, '/')) {
		strcat(dest, "{kern}/");
	} else if (anon) {
		strcat(dest, "{anon:");
		strcat(dest, anon_name);
		strcat(dest,"}/");
	} else {
		strcat(dest, "{root}/");
	}

	strcat(dest, name);
	strcat(dest, "/");
}

char * op_mangle_filename(struct mangle_values const * values)
{
	char * mangled;
	size_t len;
	int anon = values->flags & MANGLE_ANON;
	int cg_anon = values->flags & MANGLE_CG_ANON;
	/* if dep_name != image_name we need to invert them (and so revert them
	 * unconditionally because if they are equal it doesn't hurt to invert
	 * them), see P:3, FIXME: this is a bit weirds, we prolly need to
	 * reword pp_interface */
	char const * image_name = values->dep_name;
	char const * anon_name = values->anon_name;
	char const * dep_name = values->image_name;
	char const * cg_image_name = values->cg_image_name;

	len = strlen(op_samples_current_dir) + strlen(dep_name) + 1
		+ strlen(values->event_name) + 1 + strlen(image_name) + 1;

	if (values->flags & MANGLE_CALLGRAPH)
		len += strlen(cg_image_name) + 1;

	if (anon || cg_anon)
		len += strlen(anon_name);

	/* provision for tgid, tid, unit_mask, cpu and some {root}, {dep},
	 * {kern}, {anon} and {cg} marker */
	/* FIXME: too ugly */
	len += 256;

	mangled = xmalloc(len);

	strcpy(mangled, op_samples_current_dir);
	append_image(mangled, values->flags, 0, image_name, anon_name);

	strcat(mangled, "{dep}" "/");
	append_image(mangled, values->flags, anon, dep_name, anon_name);

	if (values->flags & MANGLE_CALLGRAPH) {
		strcat(mangled, "{cg}" "/");
		append_image(mangled, values->flags, cg_anon,
		             cg_image_name, anon_name);
	}

	strcat(mangled, values->event_name);
	sprintf(mangled + strlen(mangled), ".%d.%d.",
	        values->count, values->unit_mask);

	if (values->flags & MANGLE_TGID) {
		sprintf(mangled + strlen(mangled), "%d.", values->tgid);
	} else {
		sprintf(mangled + strlen(mangled), "%s.", "all");
	}

	if (values->flags & MANGLE_TID) {
		sprintf(mangled + strlen(mangled), "%d.", values->tid);
	} else {
		sprintf(mangled + strlen(mangled), "%s.", "all");
	}

	if (values->flags & MANGLE_CPU) {
		sprintf(mangled + strlen(mangled), "%d", values->cpu);
	} else {
		sprintf(mangled + strlen(mangled), "%s", "all");
	}

	return mangled;
}
