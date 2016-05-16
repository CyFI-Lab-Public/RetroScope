/*---------------------------------------------------------------------------*
 *  srec_test_config.h  *
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

extern int	get_num_srec_test_continuous_loops ( unsigned int *continuous_loops );
extern int	get_num_srec_test_shutdown_times ( unsigned int *shutdown_times );
extern int	set_num_srec_test_continuous_loops ( unsigned int continuous_loops );
extern int	set_num_srec_test_shutdown_times ( unsigned int shutdown_times );

/*
 *	All functions return 0 on success, -1 on failure
 */


