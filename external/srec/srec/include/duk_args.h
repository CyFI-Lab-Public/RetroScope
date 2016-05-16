/*---------------------------------------------------------------------------*
 *  duk_args.h  *
 *                                                                           *
 *  Copyright 2007, 2008 Nuance Communciations, Inc.                               *
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the 'License');          *
 *  you may not use this file except in compliance with the License.         *
 *                                                                           *
 *  You may obtain a copy of the License at                                  *
 *      http://www.apache.org/licenses/LICENSE-2.0                           *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an 'AS IS' BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * 
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *---------------------------------------------------------------------------*/



#ifndef _h_arg_defs_
#define _h_arg_defs_

#ifdef SET_RCSID
static const char duk_args_h[] = "$Id: duk_args.h,v 1.3.6.3 2007/08/31 17:44:52 dahan Exp $";
#endif

#include "all_defs.h"
#ifndef _RTT
#include "duk_io.h"
#endif


/* argument types */
#define A_BOO 1  /* A boolean, switch */
#define A_STR 2  /* A string */
#define A_INT 3  /* An integer */
#define A_FLT 4  /* A floating point number */
#define A_FIL 5  /* A file name */
#define A_LIS 6  /* A list of strings */

#define A_TYP_MASK 0x0f
#define A_TYP(X)        (A_TYP_MASK & (X))

/* Array flag */
#define A_ARRAY  0x10
#define A_IF_ARRAY(X) (A_ARRAY & (X))

/* Mandatory arguments info */
#define A_MAND  0x20
#define A_IF_MAND(X) (A_MAND & (X))

/* flagless arguments */
#define A_NOF  0x40
#define A_IF_NOF(X) (A_NOF & (X))

/* private info */
#define A_SET  0x80
#define A_IF_SET(X) (A_SET & (X))

/**
 * @todo document
 */
typedef union
{
  char   *a_string;
  int   *a_int;
  float  *a_float;
#ifndef _RTT
  PFile** a_file;
#endif
  char  **a_list;
}
arg_name;

typedef struct
{
  int   typ;
  char  *flag;
  int   max_args;
  arg_name  name;
  char  *def;
}
arg_info;

#define SET_ARG_ENTRY(A,W,X,Y,Z) ((A)->typ=(W), (A)->flag=(X), (A)->name.a_string=(Y), (A)->max_args=1, (A)->def=(Z))
#define SET_ARRAY_ARG_ENTRY(A,W,X,Y,N,Z) ((A)->typ=((W)|A_ARRAY), (A)->flag=(X), (A)->name.a_string=(Y), (A)->max_args=(N), (A)->def=(Z))

int  parse_single_argument(arg_info *avlist, int avc, char *key, char *value,
                           int count, int override, int *success);
int  get_single_argument(arg_info *avlist, int avc, char *key, void *value,
                         unsigned long *valueLen, int typ);
int get_string_argument(arg_info *avlist, int avc, char *key, char *value, int valueLen, int *bytes_required, int typ);
#ifdef __cplusplus
extern "C"
{
#endif
  int  com_arg_parse(int argc, char **argv, int avc, arg_info *avlist,
                     char *module);
#ifdef __cplusplus
}
#endif
int  get_arginfo_flags(arg_info *arglist, int argc, char buffer[], int buflen);
int  is_flag_assigned(char *pattern, int avc, arg_info *avlist);
void help_line(char *problem, int avc, arg_info *avlist);
void statement(char *descinfo);

#ifndef _RTT
int  resource_arg_parse(const char *resname, char *module, int avc,
                        arg_info *avlist);
void save_parsed_args(const char *resname, char *module, int avc,
                      arg_info *avlist);
#endif  /* _RTT */
                      
typedef struct
{
  int  value;
  char  *entry;
}
list_item;

int lookup_list(list_item *list_data, int nlist, char *key);

#endif /* _h_arg_defs_ */
