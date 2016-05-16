#include <stdlib.h>
#include <stdio.h>
#include <sys/wait.h>
#include <errno.h>

int
main(int argc, char *argv[])
{
    int rv;

    if (argc < 2)
        return -1;

    rv = system(argv[1]);
    if (rv < 0) {
        fprintf(stderr, "Error calling system(): %d\n", errno);
        return 1;
    }

    printf("Done!\n");

    if (WEXITSTATUS(rv) != 0) {
        fprintf(stderr, "Command returned non-zero exit code: %d\n",
                WEXITSTATUS(rv));
        return 1;
    }
    return 0;
}
