/**
 * @file op_version.h
 * output version string
 *
 * @remark Copyright 2003 OProfile authors
 * @remark Read the file COPYING
 *
 * @author John Levon
 * @author Philippe Elie
 */

#ifndef OP_VERSION_H
#define OP_VERSION_H

#ifdef __cplusplus
extern "C" {
#endif

/** output the version string */
void show_version(char const * app_name);

#ifdef __cplusplus
}
#endif

#endif /* !OP_VERSION_H */
