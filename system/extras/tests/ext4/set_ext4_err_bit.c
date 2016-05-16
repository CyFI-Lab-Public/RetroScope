#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>

#define SB_OFFSET 1024
#define SB_SIZE 1024
#define EXT4_MAGIC_OFFSET 0x38
#define EXT4_STATE_OFFSET 0x3A

int main(int argc, char *argv[])
{
    int fd;
    char me[] = "set_ext4_err_bit";
    unsigned char sb[1024];

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

    if (read(fd, sb, SB_SIZE) != SB_SIZE) {
        fprintf(stderr, "%s: Cannot read superblock\n", me);
        exit(1);
    }

    if ((sb[EXT4_MAGIC_OFFSET] != 0x53) || (sb[EXT4_MAGIC_OFFSET+1] != 0xEF)) {
        fprintf(stderr, "%s: invalid superblock magic\n", me);
        exit(1);
    }

    /* Set the errors detected bit */
    sb[EXT4_STATE_OFFSET] |= 0x2;

    if (lseek(fd, SB_OFFSET, SEEK_SET) == -1) {
        fprintf(stderr, "%s: Cannot lseek to superblock to write\n", me);
        exit(1);
    }

    if (write(fd, sb, SB_SIZE) != SB_SIZE) {
        fprintf(stderr, "%s: Cannot write superblock\n", me);
        exit(1);
    }
  
    close(fd);

    return 0;
}

