/* this tests tries to call strftime() with a date > 2038
 * to see if it works correctly.
 */
#include <time.h>
#include <stdio.h>
#include <stdlib.h>

int  main(void)
{
    char        buff[256];
    time_t      now = time(NULL);
    struct tm   tm  = *localtime(&now);

    tm.tm_year = 2039 - 1900;

    /* "%s" is the number of seconds since the epoch */
    if (strftime(buff, sizeof buff, "%s", &tm) == 0) {
        fprintf(stderr, "strftime() returned 0\n");
        exit(EXIT_FAILURE);
    }
    printf("seconds since epoch: %s\n", buff);

    /* a 32-bit limited implementation will return a negative number */
    if (buff[0] == '-') {
        fprintf(stderr, "FAIL\n");
        exit(EXIT_FAILURE);
    }

    /* "%c" is the usual date string for the current locale */
    if (strftime(buff, sizeof buff, "%c", &tm) == 0) {
        fprintf(stderr, "strftime() returned 0\n");
        exit(EXIT_FAILURE);
    }
    printf("date string        : %s\n", buff);
    return 0;
}
