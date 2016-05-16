/* tools/editdisklbl/editdisklbl.c
 *
 * Copyright 2008, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define __USE_LARGEFILE64
#define __USE_FILE_OFFSET64
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>

#include "diskconfig/diskconfig.h"

/* give us some room */
#define EXTRA_LBAS      100

static struct pf_map {
    struct part_info *pinfo;
    const char *filename;
} part_file_map[MAX_NUM_PARTS] = { {0, 0} };

static int
usage(void)
{
    fprintf(stderr,
            "\nusage: editdisklbl <options> part1=file1 [part2=file2,...]\n"
            "Where options can be one of:\n"
            "\t\t-l <layout conf>  -- The image layout config file.\n"
            "\t\t-i <image file>   -- The image file to edit.\n"
            "\t\t-t                -- Test mode (optional)\n"
            "\t\t-v                -- Be verbose\n"
            "\t\t-h                -- This message (optional)\n"
            );
    return 1;
}

static int
parse_args(int argc, char *argv[], struct disk_info **dinfo, int *test,
           int *verbose)
{
    char *layout_conf = NULL;
    char *img_file = NULL;
    struct stat filestat;
    int x;
    int update_lba = 0;

    while ((x = getopt (argc, argv, "vthl:i:")) != EOF) {
        switch (x) {
            case 'h':
                return usage();
            case 'l':
                layout_conf = optarg;
                break;
            case 't':
                *test = 1;
                break;
            case 'i':
                img_file = optarg;
                break;
            case 'v':
                *verbose = 1;
                break;
            default:
                fprintf(stderr, "Unknown argument: %c\n", (char)optopt);
                return usage();
        }
    }

    if (!img_file || !layout_conf) {
        fprintf(stderr, "Image filename and configuration file are required\n");
        return usage();
    }

    /* we'll need to parse the command line later for partition-file
     * mappings, so make sure there's at least something there */
    if (optind >= argc) {
        fprintf(stderr, "Must provide partition -> file mappings\n");
        return usage();
    }

    if (stat(img_file, &filestat)) {
        perror("Cannot stat image file");
        return 1;
    }

    /* make sure we don't screw up and write to a block device on the host
     * and wedge things. I just don't trust myself. */
    if (!S_ISREG(filestat.st_mode)) {
        fprintf(stderr, "This program should only be used on regular files.");
        return 1;
    }

    /* load the disk layout file */
    if (!(*dinfo = load_diskconfig(layout_conf, img_file))) {
        fprintf(stderr, "Errors encountered while loading disk conf file %s",
                layout_conf);
        return 1;
    }

    if ((*dinfo)->num_lba == 0) {
        (*dinfo)->num_lba = (*dinfo)->skip_lba + EXTRA_LBAS;
        update_lba = 1;
    }

    /* parse the filename->partition mappings from the command line and patch
     * up a loaded config file's partition table entries to have
     * length == filesize */
    x = 0;
    while (optind < argc) {
        char *pair = argv[optind++];
        char *part_name;
        struct part_info *pinfo;
        struct stat tmp_stat;

        if (x >= MAX_NUM_PARTS) {
            fprintf(stderr, "Error: Too many partitions specified (%d)!\n", x);
            return 1;
        }

        if (!(part_name = strsep(&pair, "=")) || !pair || !(*pair)) {
            fprintf(stderr, "Error parsing partition mappings\n");
            return usage();
        }

        if (!(pinfo = find_part(*dinfo, part_name))) {
            fprintf(stderr, "Partition '%s' not found.\n", part_name);
            return 1;
        }

        /* here pair points to the filename (after the '=') */
        part_file_map[x].pinfo = pinfo;
        part_file_map[x++].filename = pair;

        if (stat(pair, &tmp_stat) < 0) {
            fprintf(stderr, "Could not stat file: %s\n", pair);
            return 1;
        }

        pinfo->len_kb = (uint32_t) ((tmp_stat.st_size + 1023) >> 10);
        if (update_lba)
            (*dinfo)->num_lba += 
                    ((uint64_t)pinfo->len_kb * 1024) / (*dinfo)->sect_size;
        printf("Updated %s length to be %uKB\n", pinfo->name, pinfo->len_kb);
    }

    return 0;
}

int
main(int argc, char *argv[])
{
    struct disk_info *dinfo = NULL;
    int test = 0;
    int verbose = 0;
    int cnt;

    if (parse_args(argc, argv, &dinfo, &test, &verbose))
        return 1;

    if (verbose)
        dump_disk_config(dinfo);

    if (test)
        printf("Test mode enabled. Actions will not be committed to disk!\n");

    if (apply_disk_config(dinfo, test)) {
        fprintf(stderr, "Could not apply disk configuration!\n");
        return 1;
    }

    printf("Copying images to specified partition offsets\n");
    /* now copy the images to their appropriate locations on disk */
    for (cnt = 0; cnt < MAX_NUM_PARTS && part_file_map[cnt].pinfo; ++cnt) {
        loff_t offs = part_file_map[cnt].pinfo->start_lba * dinfo->sect_size;
        const char *dest_fn = dinfo->device;
        if (write_raw_image(dest_fn, part_file_map[cnt].filename, offs, test)) {
            fprintf(stderr, "Could not write images after editing label.\n");
            return 1;
        }
    }
    printf("File edit complete. Wrote %d images.\n", cnt);

    return 0;
}
