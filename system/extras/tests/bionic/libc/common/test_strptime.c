/*
 * Copyright (C) 2011 The Android Open Source Project
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

// Minimal test program for strptime

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

int main(int argc, char **argv)
{
    struct tm tm;
    char buf[255];

    // For now, only test a couple of formats that use recursion

    memset(&tm, 0, sizeof(tm));
    strptime("11:14", "%R", &tm);
    strftime(buf, sizeof(buf), "%H:%M", &tm);
    puts(buf);
    puts(!strcmp(buf, "11:14") ? "OK" : "FAILED");

    memset(&tm, 0, sizeof(tm));
    strptime("09:41:53", "%T", &tm);
    strftime(buf, sizeof(buf), "%H:%M:%S", &tm);
    puts(buf);
    puts(!strcmp(buf, "09:41:53") ? "OK" : "FAILED");

    return EXIT_SUCCESS;
}
