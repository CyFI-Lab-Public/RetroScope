
extern const struct SLInterfaceID_ SL_IID_array[MPH_MAX];
extern const char * const interface_names[MPH_MAX];
extern void MPH_to_MPH_string(unsigned MPH, char buffer[40]);

int main(int argc, char **argv)
{
    int i;
    for (i = 0; i <= MAX_HASH_VALUE; ++i) {
        const char *x = wordlist[i];
        if (!x) {
            printf("        -1");
        } else {
            const struct SLInterfaceID_ *xx = SL_IID_array;
            unsigned MPH;
            for (MPH = 0; MPH < MPH_MAX; ++MPH, ++xx) {
                if (!memcmp(x, xx, 16)) {
                    char buffer[40];
                    buffer[39] = 'x';
                    MPH_to_MPH_string(MPH, buffer);
                    assert('x' == buffer[39]);
                    printf("        %s", buffer);
                    goto out;
                }
            }
            printf("        (-1)");
out:
            ;
        }
        if (i < MAX_HASH_VALUE)
            printf(",");
        printf("\n");
    }
    return EXIT_SUCCESS;
}
