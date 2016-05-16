/*---------------------------------------------------------------------------*
 *  fft.h  *
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




#ifndef _h_fft_
#define _h_fft_


#include "fronttyp.h"

/*  These are the data objects associated with the FFT and IFFT
**  which generate the modulated carriers
*/


#include "sp_fft.h"

int  fft_perform_and_magsq(fft_info *fft);
void do_magsq(fft_info *fft);

void configure_fft(fft_info *fft, int size);
int  place_sample_data(fft_info *fft, fftdata *seq, fftdata *smooth, int num);
void unconfigure_fft(fft_info *fft);


#endif /* _h_fft_ */
