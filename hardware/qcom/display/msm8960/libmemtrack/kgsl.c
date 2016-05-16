/*
 * Copyright (C) 2013 The Android Open Source Project
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

#include <errno.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>

#include <hardware/memtrack.h>

#include "memtrack_msm.h"

#define ARRAY_SIZE(x) (sizeof(x)/sizeof(x[0]))
#define min(x, y) ((x) < (y) ? (x) : (y))

struct memtrack_record record_templates[] = {
    {
        .flags = MEMTRACK_FLAG_SMAPS_ACCOUNTED |
                 MEMTRACK_FLAG_PRIVATE |
                 MEMTRACK_FLAG_NONSECURE,
    },
    {
        .flags = MEMTRACK_FLAG_SMAPS_UNACCOUNTED |
                 MEMTRACK_FLAG_PRIVATE |
                 MEMTRACK_FLAG_NONSECURE,
    },
};

int kgsl_memtrack_get_memory(pid_t pid, enum memtrack_type type,
                             struct memtrack_record *records,
                             size_t *num_records)
{
    size_t allocated_records = min(*num_records, ARRAY_SIZE(record_templates));
    int i;
    FILE *fp;
    FILE *smaps_fp = NULL;
    char line[1024];
    char tmp[128];
    size_t accounted_size = 0;
    size_t unaccounted_size = 0;
    unsigned long smaps_addr = 0;

    *num_records = ARRAY_SIZE(record_templates);

    /* fastpath to return the necessary number of records */
    if (allocated_records == 0) {
        return 0;
    }

    memcpy(records, record_templates,
           sizeof(struct memtrack_record) * allocated_records);

    sprintf(tmp, "/d/kgsl/proc/%d/mem", pid);
    fp = fopen(tmp, "r");
    if (fp == NULL) {
        return -errno;
    }

    if (type == MEMTRACK_TYPE_GL) {
        sprintf(tmp, "/proc/%d/smaps", pid);
        smaps_fp = fopen(tmp, "r");
        if (smaps_fp == NULL) {
            fclose(fp);
            return -errno;
        }
    }

    while (1) {
        unsigned long uaddr;
        unsigned long size;
        char line_type[7];
        int ret;

        if (fgets(line, sizeof(line), fp) == NULL) {
            break;
        }

        /* Format:
         *  gpuaddr useraddr     size    id flags       type            usage sglen
         * 545ba000 545ba000     4096     1 ----p     gpumem      arraybuffer     1
         */
        ret = sscanf(line, "%*x %lx %lu %*d %*s %6s %*s %*d\n",
                     &uaddr, &size, line_type);
        if (ret != 3) {
            continue;
        }

        if (type == MEMTRACK_TYPE_GL && strcmp(line_type, "gpumem") == 0) {
            bool accounted = false;
            /*
             * We need to cross reference the user address against smaps,
             *  luckily both are sorted.
             */
            while (smaps_addr <= uaddr) {
                unsigned long start;
                unsigned long end;
                unsigned long smaps_size;

                if (fgets(line, sizeof(line), smaps_fp) == NULL) {
                    break;
                }

                if (sscanf(line, "%8lx-%8lx", &start, &end) == 2) {
                    smaps_addr = start;
                    continue;
                }

                if (smaps_addr != uaddr) {
                    continue;
                }

                if (sscanf(line, "Rss: %lu kB", &smaps_size) == 1) {
                    if (smaps_size) {
                        accounted = true;
                        accounted_size += size;
                        break;
                    }
                }
            }
            if (!accounted) {
                unaccounted_size += size;
            }
        } else if (type == MEMTRACK_TYPE_GRAPHICS && strcmp(line_type, "ion") == 0) {
            unaccounted_size += size;
        }
    }

    if (allocated_records > 0) {
        records[0].size_in_bytes = accounted_size;
    }
    if (allocated_records > 1) {
        records[1].size_in_bytes = unaccounted_size;
    }

    if (smaps_fp)
        fclose(smaps_fp);
    fclose(fp);

    return 0;
}
