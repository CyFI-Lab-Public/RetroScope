/* commands/sysloader/installer/installer.c
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

#define LOG_TAG "installer"

#include <sys/types.h>
#include <errno.h>
#include <fcntl.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/mount.h>
#include <sys/stat.h>
#include <sys/wait.h>


#include <cutils/config_utils.h>
#include <cutils/log.h>

#include "diskconfig/diskconfig.h"
#include "installer.h"

#define MKE2FS_BIN     "/system/bin/mke2fs"
#define E2FSCK_BIN     "/system/bin/e2fsck"
#define TUNE2FS_BIN    "/system/bin/tune2fs"
#define RESIZE2FS_BIN  "/system/bin/resize2fs"

static int
usage(void)
{
    fprintf(stderr, "Usage: %s\n", LOG_TAG);
    fprintf(stderr, "\t-c <path> - Path to installer conf file "
                    "(/system/etc/installer.conf)\n");
    fprintf(stderr, "\t-l <path> - Path to device disk layout conf file "
                    "(/system/etc/disk_layout.conf)\n");
    fprintf(stderr, "\t-h        - This help message\n");
    fprintf(stderr, "\t-d        - Dump the compiled in partition info.\n");
    fprintf(stderr, "\t-p <path> - Path to device that should be mounted"
                    " to /data.\n");
    fprintf(stderr, "\t-t        - Test mode. Don't write anything to disk.\n");
    return 1;
}

static cnode *
read_conf_file(const char *fn)
{
    cnode *root = config_node("", "");
    config_load_file(root, fn);

    if (root->first_child == NULL) {
        ALOGE("Could not read config file %s", fn);
        return NULL;
    }

    return root;
}

static int
exec_cmd(const char *cmd, ...) /* const char *arg, ...) */
{
    va_list ap;
    int size = 0;
    char *str;
    char *outbuf;
    int rv;

    /* compute the size for the command buffer */
    size = strlen(cmd) + 1;
    va_start(ap, cmd);
    while ((str = va_arg(ap, char *))) {
        size += strlen(str) + 1;  /* need room for the space separator */
    }
    va_end(ap);

    if (!(outbuf = malloc(size + 1))) {
        ALOGE("Can't allocate memory to exec cmd");
        return -1;
    }

    /* this is a bit inefficient, but is trivial, and works */
    strcpy(outbuf, cmd);
    va_start(ap, cmd);
    while ((str = va_arg(ap, char *))) {
        strcat(outbuf, " ");
        strcat(outbuf, str);
    }
    va_end(ap);

    ALOGI("Executing: %s", outbuf);
    rv = system(outbuf);
    free(outbuf);
    if (rv < 0) {
        ALOGI("Error while trying to execute '%s'", cmd);
        return -1;
    }
    rv = WEXITSTATUS(rv);
    ALOGI("Done executing %s (%d)", outbuf, rv);
    return rv;
}


static int
do_fsck(const char *dst, int force)
{
    int rv;
    const char *opts = force ? "-fy" : "-y";


    ALOGI("Running e2fsck... (force=%d) This MAY take a while.", force);
    if ((rv = exec_cmd(E2FSCK_BIN, "-C 0", opts, dst, NULL)) < 0)
        return 1;
    if (rv >= 4) {
        ALOGE("Error while running e2fsck: %d", rv);
        return 1;
    }
    sync();
    ALOGI("e2fsck succeeded (exit code: %d)", rv);

    return 0;
}

static int
process_ext2_image(const char *dst, const char *src, uint32_t flags, int test)
{
    int rv;

    /* First, write the image to disk. */
    if (write_raw_image(dst, src, 0, test))
        return 1;

    if (test)
        return 0;

    /* Next, let's e2fsck the fs to make sure it got written ok, and
     * everything is peachy */
    if (do_fsck(dst, 1))
        return 1;

    /* set the mount count to 1 so that 1st mount on boot doesn't complain */
    if ((rv = exec_cmd(TUNE2FS_BIN, "-C", "1", dst, NULL)) < 0)
        return 1;
    if (rv) {
        ALOGE("Error while running tune2fs: %d", rv);
        return 1;
    }

    /* If the user requested that we resize, let's do it now */
    if (flags & INSTALL_FLAG_RESIZE) {
        if ((rv = exec_cmd(RESIZE2FS_BIN, "-F", dst, NULL)) < 0)
            return 1;
        if (rv) {
            ALOGE("Error while running resize2fs: %d", rv);
            return 1;
        }
        sync();
        if (do_fsck(dst, 0))
            return 1;
    }

    /* make this an ext3 fs? */
    if (flags & INSTALL_FLAG_ADDJOURNAL) {
        if ((rv = exec_cmd(TUNE2FS_BIN, "-j", dst, NULL)) < 0)
            return 1;
        if (rv) {
            ALOGE("Error while running tune2fs: %d", rv);
            return 1;
        }
        sync();
        if (do_fsck(dst, 0))
            return 1;
    }

    return 0;
}


/* TODO: PLEASE break up this function into several functions that just
 * do what they need with the image node. Many of them will end up
 * looking at same strings, but it will be sooo much cleaner */
static int
process_image_node(cnode *img, struct disk_info *dinfo, int test)
{
    struct part_info *pinfo = NULL;
    loff_t offset = (loff_t)-1;
    const char *filename = NULL;
    char *dest_part = NULL;
    const char *tmp;
    uint32_t flags = 0;
    uint8_t type = 0;
    int rv;
    int func_ret = 1;

    filename = config_str(img, "filename", NULL);

    /* process the 'offset' image parameter */
    if ((tmp = config_str(img, "offset", NULL)) != NULL)
        offset = strtoull(tmp, NULL, 0);

    /* process the 'partition' image parameter */
    if ((tmp = config_str(img, "partition", NULL)) != NULL) {
        if (offset != (loff_t)-1) {
            ALOGE("Cannot specify the partition name AND an offset for %s",
                 img->name);
            goto fail;
        }

        if (!(pinfo = find_part(dinfo, tmp))) {
            ALOGE("Cannot find partition %s while processing %s",
                 tmp, img->name);
            goto fail;
        }

        if (!(dest_part = find_part_device(dinfo, pinfo->name))) {
            ALOGE("Could not get the device name for partition %s while"
                 " processing image %s", pinfo->name, img->name);
            goto fail;
        }
        offset = pinfo->start_lba * dinfo->sect_size;
    }

    /* process the 'mkfs' parameter */
    if ((tmp = config_str(img, "mkfs", NULL)) != NULL) {
        char *journal_opts;
        char vol_lbl[16]; /* ext2/3 has a 16-char volume label */

        if (!pinfo) {
            ALOGE("Target partition required for mkfs for '%s'", img->name);
            goto fail;
        } else if (filename) {
            ALOGE("Providing filename and mkfs parameters is meaningless");
            goto fail;
        }

        if (!strcmp(tmp, "ext4"))
            journal_opts = "";
        else if (!strcmp(tmp, "ext2"))
            journal_opts = "";
        else if (!strcmp(tmp, "ext3"))
            journal_opts = "-j";
        else {
            ALOGE("Unknown filesystem type for mkfs: %s", tmp);
            goto fail;
        }

        /* put the partition name as the volume label */
        strncpy(vol_lbl, pinfo->name, sizeof(vol_lbl));

        /* since everything checked out, lets make the fs, and return since
         * we don't need to do anything else */
        rv = exec_cmd(MKE2FS_BIN, "-L", vol_lbl, journal_opts, dest_part, NULL);
        if (rv < 0)
            goto fail;
        else if (rv > 0) {
            ALOGE("Error while running mke2fs: %d", rv);
            goto fail;
        }
        sync();
        if (do_fsck(dest_part, 0))
            goto fail;
        goto done;
    }

    /* since we didn't mkfs above, all the rest of the options assume
     * there's a filename involved */
    if (!filename) {
        ALOGE("Filename is required for image %s", img->name);
        goto fail;
    }

    /* process the 'flags' image parameter */
    if ((tmp = config_str(img, "flags", NULL)) != NULL) {
        char *flagstr, *flagstr_orig;

        if (!(flagstr = flagstr_orig = strdup(tmp))) {
            ALOGE("Cannot allocate memory for dup'd flags string");
            goto fail;
        }
        while ((tmp = strsep(&flagstr, ","))) {
            if (!strcmp(tmp, "resize"))
                flags |= INSTALL_FLAG_RESIZE;
            else if (!strcmp(tmp, "addjournal"))
                flags |= INSTALL_FLAG_ADDJOURNAL;
            else {
                ALOGE("Unknown flag '%s' for image %s", tmp, img->name);
                free(flagstr_orig);
                goto fail;
            }
        }
        free(flagstr_orig);
    }

    /* process the 'type' image parameter */
    if (!(tmp = config_str(img, "type", NULL))) {
        ALOGE("Type is required for image %s", img->name);
        goto fail;
    } else if (!strcmp(tmp, "raw")) {
        type = INSTALL_IMAGE_RAW;
    } else if (!strcmp(tmp, "ext2")) {
        type = INSTALL_IMAGE_EXT2;
    } else if (!strcmp(tmp, "ext3")) {
        type = INSTALL_IMAGE_EXT3;
    } else if (!strcmp(tmp, "ext4")) {
        type = INSTALL_IMAGE_EXT4;
    } else {
        ALOGE("Unknown image type '%s' for image %s", tmp, img->name);
        goto fail;
    }

    /* at this point we MUST either have a partition in 'pinfo' or a raw
     * 'offset', otherwise quit */
    if (!pinfo && (offset == (loff_t)-1)) {
        ALOGE("Offset to write into the disk is unknown for %s", img->name);
        goto fail;
    }

    if (!pinfo && (type != INSTALL_IMAGE_RAW)) {
        ALOGE("Only raw images can specify direct offset on the disk. Please"
             " specify the target partition name instead. (%s)", img->name);
        goto fail;
    }

    switch(type) {
        case INSTALL_IMAGE_RAW:
            if (write_raw_image(dinfo->device, filename, offset, test))
                goto fail;
            break;

        case INSTALL_IMAGE_EXT3:
            /* makes the error checking in the imager function easier */
            if (flags & INSTALL_FLAG_ADDJOURNAL) {
                ALOGW("addjournal flag is meaningless for ext3 images");
                flags &= ~INSTALL_FLAG_ADDJOURNAL;
            }
            /* ...fall through... */

        case INSTALL_IMAGE_EXT4:
            /* fallthru */

        case INSTALL_IMAGE_EXT2:
            if (process_ext2_image(dest_part, filename, flags, test))
                goto fail;
            break;

        default:
            ALOGE("Unknown image type: %d", type);
            goto fail;
    }

done:
    func_ret = 0;

fail:
    if (dest_part)
        free(dest_part);
    return func_ret;
}

int
main(int argc, char *argv[])
{
    char *disk_conf_file = "/system/etc/disk_layout.conf";
    char *inst_conf_file = "/system/etc/installer.conf";
    char *inst_data_dir = "/data";
    char *inst_data_dev = NULL;
    char *data_fstype = "ext4";
    cnode *config;
    cnode *images;
    cnode *img;
    int cnt = 0;
    struct disk_info *device_disk_info;
    int dump = 0;
    int test = 0;
    int x;

    while ((x = getopt (argc, argv, "thdc:l:p:")) != EOF) {
        switch (x) {
            case 'h':
                return usage();
            case 'c':
                inst_conf_file = optarg;
                break;
            case 'l':
                disk_conf_file = optarg;
                break;
            case 't':
                test = 1;
                break;
            case 'p':
                inst_data_dev = optarg;
                break;
            case 'd':
                dump = 1;
                break;
            default:
                fprintf(stderr, "Unknown argument: %c\n", (char)optopt);
                return usage();
        }
    }

    /* If the user asked us to wait for data device, wait for it to appear,
     * and then mount it onto /data */
    if (inst_data_dev && !dump) {
        struct stat filestat;

        ALOGI("Waiting for device: %s", inst_data_dev);
        while (stat(inst_data_dev, &filestat))
            sleep(1);
        ALOGI("Device %s ready", inst_data_dev);
        if (mount(inst_data_dev, inst_data_dir, data_fstype, MS_RDONLY, NULL)) {
            ALOGE("Could not mount %s on %s as %s", inst_data_dev, inst_data_dir,
                 data_fstype);
            return 1;
        }
    }

    /* Read and process the disk configuration */
    if (!(device_disk_info = load_diskconfig(disk_conf_file, NULL))) {
        ALOGE("Errors encountered while loading disk conf file %s",
             disk_conf_file);
        return 1;
    }

    if (process_disk_config(device_disk_info)) {
        ALOGE("Errors encountered while processing disk config from %s",
             disk_conf_file);
        return 1;
    }

    /* Was all of this for educational purposes? If so, quit. */
    if (dump) {
        dump_disk_config(device_disk_info);
        return 0;
    }

    /* This doesnt do anything but load the config file */
    if (!(config = read_conf_file(inst_conf_file)))
        return 1;

    /* First, partition the drive */
    if (apply_disk_config(device_disk_info, test))
        return 1;

    /* Now process the installer config file and write the images to disk */
    if (!(images = config_find(config, "images"))) {
        ALOGE("Invalid configuration file %s. Missing 'images' section",
             inst_conf_file);
        return 1;
    }

    for (img = images->first_child; img; img = img->next) {
        if (process_image_node(img, device_disk_info, test)) {
            ALOGE("Unable to write data to partition. Try running 'installer' again.");
            return 1;
        }
        ++cnt;
    }

    /*
     * We have to do the apply() twice. We must do it once before the image
     * writes to layout the disk partitions so that we can write images to
     * them. We then do the apply() again in case one of the images
     * replaced the MBR with a new bootloader, and thus messed with
     * partition table.
     */
    if (apply_disk_config(device_disk_info, test))
        return 1;

    ALOGI("Done processing installer config. Configured %d images", cnt);
    ALOGI("Type 'reboot' or reset to run new image");
    return 0;
}
