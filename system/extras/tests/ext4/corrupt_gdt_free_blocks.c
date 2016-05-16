#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>

#include "ext4.h"
#include "ext4_utils.h"

#define SB_OFFSET 1024

int main(int argc, char *argv[])
{
    char me[] = "corrupt_gdt_free_blocks";
    int fd;
    int block_size;
    int num_bgs;
    int i;
    struct ext4_super_block sb;
    struct ext2_group_desc gd;

    if (argc != 2) {
        fprintf(stderr, "%s: Usage: %s <ext4_block_device>\n", me, me);
        exit(1);
    }

    fd = open(argv[1], O_RDWR);

    if (fd < 0) {
        fprintf(stderr, "%s: Cannot open block device %s\n", me, argv[1]);
        exit(1);
    }

    if (lseek(fd, SB_OFFSET, SEEK_SET) == -1) {
        fprintf(stderr, "%s: Cannot lseek to superblock to read\n", me);
        exit(1);
    }

    if (read(fd, &sb, sizeof(sb)) != sizeof(sb)) {
        fprintf(stderr, "%s: Cannot read superblock\n", me);
        exit(1);
    }

    if (sb.s_magic != 0xEF53) {
        fprintf(stderr, "%s: invalid superblock magic\n", me);
        exit(1);
    }

    /* Make sure the block size is 2K or 4K */
    if ((sb.s_log_block_size != 1) && (sb.s_log_block_size != 2)) {
        fprintf(stderr, "%s: block size not 2K or 4K\n", me);
        exit(1);
    }

    block_size = 1 << (10 + sb.s_log_block_size);
    num_bgs = DIV_ROUND_UP(sb.s_blocks_count_lo, sb.s_blocks_per_group);

    if (sb.s_desc_size != sizeof(struct ext2_group_desc)) {
        fprintf(stderr, "%s: Can't handle block group descriptor size of %d\n",
                me, sb.s_desc_size);
        exit(1);
    }

    /* read first block group descriptor, decrement free block count, and
     * write it back out
     */
    if (lseek(fd, block_size, SEEK_SET) == -1) {
        fprintf(stderr, "%s: Cannot lseek to block group descriptor table to read\n", me);
        exit(1);
    }

    /* Read in block group descriptors till we read one that has at least one free block */

    for (i=0; i < num_bgs; i++) {
        if (read(fd, &gd, sizeof(gd)) != sizeof(gd)) {
            fprintf(stderr, "%s: Cannot read group descriptor %d\n", me, i);
            exit(1);
        }
        if (gd.bg_free_blocks_count) {
            break;
        }
    }

    gd.bg_free_blocks_count--;

    if (lseek(fd, -sizeof(gd), SEEK_CUR) == -1) {
        fprintf(stderr, "%s: Cannot lseek to block group descriptor table to write\n", me);
        exit(1);
    }

    if (write(fd, &gd, sizeof(gd)) != sizeof(gd)) {
        fprintf(stderr, "%s: Cannot write modified group descriptor\n", me);
        exit(1);
    }

    close(fd);

    return 0;
}

