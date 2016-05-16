/**
 * @file daemon/opd_printf.h
 * Output routines
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OPD_PRINTF_H
#define OPD_PRINTF_H

/// log all sample file name manipulation; sample files open, close,
/// sfile LRU etc. voluminous. FIXME need to be splitted (filename manip, files
/// handling) ?
extern int vsfile;
/// log samples, voluminous.
extern int vsamples;
/// log arc, very voluminous.
extern int varcs;
/// kernel module handling
extern int vmodule;
/// extended feature
extern int vext;
/// all others not fitting in above category, not voluminous.
extern int vmisc;

#define verbprintf(x, args...) \
	do { \
		/* look like fragile but we must catch verbrintf("%s", "") */ \
		if (x == 1) \
			printf(args); \
	} while (0)

#endif /* OPD_PRINTF_H */
