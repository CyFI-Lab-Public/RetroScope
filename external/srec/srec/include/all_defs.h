/*---------------------------------------------------------------------------*
 *  all_defs.h  *
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


#ifndef _h_all_defs_
#define _h_all_defs_

#include "ptypes.h"
#include "setting.h"

#define USE_CONFIDENCE_SCORER 1

/* constants */
/* Model constants */
#define MAX_LABEL       40      /* Maximum permitted label length */
#define MAX_STRING      1024    /* Maximum permitted string length */
#define MAX_FILE_NAME 256     /* Maximum permitted filename length */
#define ALLOC_BLOCK     100
#define MAX_STREAM      32      /* max no. of streams in prog, not file */
#define MAX_DIMEN 64
#define MAX_PELS        128

/* Likelihood calculations */
#define ADD_LOG_LIMIT    8
#define SCORE_ADJUST     4
#define SIGMA_BIAS       4
#define MUL_SCALE        6.4F
#define WT_SCALE         6.4F /* log weights */
#define WT_ADJUST  2 /* in bits, for exp table lookup */
#define WEIGHT_SHIFT  10
#define WEIGHT_SCALE  (0x01 << WEIGHT_SHIFT) /* absolute weights */
#define MAX_WTS   400 /* Needs to be tuned, dependent on some scales */
#define IMELDA_SCALE     14 /* Applicable only for grand variance */
#define EIGEN            1      /* for full covariance probability calc. */
#define EUCLID_SHIFT     14 /* Scaling for FIXED_POINT in shifts */

/* MLLR and Baum-Welch */
/*dahan20070525
#define MEAN_SCALE 20 // MLLR coefficients
#define MEAN_OFFSET 128 // MLLR coefficients 
#define MAX_MLLR_TRANSFORMS 50
#define EPSILON         0.001F
#define MAX_OCCUPANCY   1000
#define ITEM_WEIGHT     1       // item weighting for covariance calc. 
// otherwise gamma weighted 
#define GAMMA_SIGNI     0.0001  // minimum occupancy accumulates 
*/

/* state duration constants */
#define DUR_BIAS        0       /* duration penalty bias, changed elsewhere */
#define NEWTON_FACTOR   10.0F   /* for converting duration to penalties */
#define DEFAULT_UNIT_PER_FRAME 1
#define FRAME_RATE_IN_MS 10
#define UTB_MEAN        127.5F  /* mean of parameters in utb file */

/* utterence constants */
#define NORM_IN_IMELDA     0 /* Do channel normalization in IMELDA space */
#define MAX_CEP_DIM     12
#define MAX_CHAN_DIM     36
#define MAX_FILTER_NUM     32 /* spectrum filter read as frontend pars  (centre freq, spread)*/
#define NEW_UTB      1 /* support for latest (version 5) UTB files */
/* with this set to zero supports version 3 files */


/* phonemes and contexts */
#define MAX_PHONEMES     128
#define MAX_PHONE_STATES 6

#define True  ESR_TRUE /*  Boolean constants */
#define False  ESR_FALSE

#define LITTLE          0       /* endian types */
#define BIG             1
/*  Configuration options
    useful arithmetic functions and constants */
#ifndef MAX
#define MAX(X,Y)        ((X) > (Y) ? (X) : (Y))
#endif
#ifndef MIN
#define MIN(X,Y)        ((X) < (Y) ? (X) : (Y))
#endif
#ifndef RANGE
#define RANGE(X,Y,Z)        ((X) < (Y) ? (Y) : (X) > (Z) ? (Z) : (X))
#endif
#define SQR(X)          ((X) * (X))
#define MAKEBYTE(X)     ((X) > 255 ? 255: ((X) < 0 ? 0 : (X)))
#define ROUNDOFF(X) ((int)((X) >= 0 ? ((X) + 0.5) : ((X) - 0.5)))

#ifndef M_PI
#define M_PI            3.14159265358979323846  
#endif /* M_PI */

#define MAX_LOG  1000000                 /* Check this value against types */
#define FIXED_MAX 32767


#define D_SHORT         1L            /* Frame data types */
#define D_LONG          2L
#define D_FLOAT         3L
#define D_PTR           4L
#define D_CHAR          5L
#define D_UCHAR         6L
#define D_USHORT        7L
#define D_INT           8L
#define D_UINT          9L
#define D_ULONG        10L
#define D_DOUBLE       11L

#define USE_SYSTEM_MEMMOVE  1  /* memmove redefinitions, do not change */

#endif
