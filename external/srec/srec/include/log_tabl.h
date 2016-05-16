/*---------------------------------------------------------------------------*
 *  log_tabl.h  *
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

#ifndef _h_log_tabl_
#define _h_log_tabl_

#define LOG_SCALE 1024  /* smallest scale is about 0.1 dB */
#define LOG_SCALE_SHIFT   10

/**
 * @todo document
 */
typedef struct
{
  int    size;
  int    shift;
  int    scale;
  unsigned long mask;
  const int *table;
}
log_table_info;

void create_lookup_log(log_table_info *logtab, int num_bits);
int log_lookup(log_table_info *logtab, int operand, int shift);
int inv_log_lookup(log_table_info *logtab, int operand);
void destroy_lookup_log(log_table_info *logtab);
int integer_square_root(int operand);



#endif
