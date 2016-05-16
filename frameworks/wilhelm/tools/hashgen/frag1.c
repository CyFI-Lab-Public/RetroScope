/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// This program prepares the input to gperf for hashing IIDs

#include <SLES/OpenSLES.h>
#include "MPH.h"
#include <stdio.h>
#include <stdlib.h>

extern const struct SLInterfaceID_ SL_IID_array[MPH_MAX];

int main(int argc, char **argv)
{
    int MPH;
    const struct SLInterfaceID_ *x = SL_IID_array;
    for (MPH = 0; MPH < MPH_MAX; ++MPH, ++x) {
        unsigned char *y = (unsigned char *) x;
        unsigned k;
        printf("\"");
        for (k = 0; k < sizeof(struct SLInterfaceID_); ++k)
            printf("\\x%02X", y[k]);
        printf("\"\n");
    }
    return EXIT_SUCCESS;
}
