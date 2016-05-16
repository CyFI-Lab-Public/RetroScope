/**
 * @file opd_parse_proc.h
 * Parsing of /proc/#pid
 *
 * @remark Copyright 2002 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OPD_PARSE_PROC_H
#define OPD_PARSE_PROC_H

/**
 * opd_get_ascii_procs - read process and mapping information from /proc
 *
 * Read information on each process and its mappings from the /proc
 * filesystem.
 */
void opd_get_ascii_procs(void);

#endif /* OPD_PARSE_PROC_H */
