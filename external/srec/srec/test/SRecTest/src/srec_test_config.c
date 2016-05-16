/*---------------------------------------------------------------------------*
 *  srec_test_config.c  *
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

#include "srec_test_config.h"

static unsigned int	num_test_shutdown_times = 1;	/* Number of times to shutdown and restart during test */
static unsigned int	num_test_continuous_loops = 1;	/* Number of loops to run between start and shutdown */



int get_num_srec_test_shutdown_times ( unsigned int *shutdown_times )
    {
    int get_status;

    *shutdown_times = num_test_shutdown_times;
    get_status = 0;

    return ( get_status );
    }



int set_num_srec_test_shutdown_times ( unsigned int shutdown_times )
    {
    int set_status;

    num_test_shutdown_times = shutdown_times;
    set_status = 0;

    return ( set_status );
    }



int get_num_srec_test_continuous_loops ( unsigned int *continuous_loops )
    {
    int get_status;

    *continuous_loops = num_test_continuous_loops;
    get_status = 0;

    return ( get_status );
    }



int set_num_srec_test_continuous_loops ( unsigned int continuous_loops )
    {
    int set_status;

    num_test_continuous_loops = continuous_loops;
    set_status = 0;

    return ( set_status );
    }

