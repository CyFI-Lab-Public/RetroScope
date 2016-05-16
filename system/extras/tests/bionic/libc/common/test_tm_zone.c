/*
 * Copyright (C) 2008 The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the 
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED 
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
/* a small program to test the tm_zone setting in Bionic */
#include <time.h>
#include <stdio.h>
#include <stdlib.h>

int  main( void )
{
#ifndef TM_ZONE
    fprintf(stderr, "TM_ZONE is not defined in <time.h> !!\n" );
    return 1;
#else
    const char*  tz = getenv("TZ");
    time_t       now = time(NULL);
    struct tm    tm0;
    struct tm*   tm;

    if (tz) {
        printf( "TZ set to '%s'\n", tz );
    } else
        printf( "TZ is not defined\n" );

    tm = localtime_r( &now, &tm0 );
    printf( "localtime_r() returns timezone abbreviation '%s'\n", tm->TM_ZONE ? tm->TM_ZONE : "<NULL POINTER>" );
    printf( "tzname[0] is '%s'\n", tzname[0] ? tzname[0] : "<NULL POINTER>" );
    printf( "tzname[1] is '%s'\n", tzname[1] ? tzname[1] : "<NULL POINTER>" );
#endif
    return 0;
}
