#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char *argv[]) {
    // Arguments
    if (argc != 4 || (argc == 2 && strcmp(argv[1], "--help") == 0)) {
        fprintf(stderr, "Usage: %s PREFIX OUTFILE INFILE\n", argv[0]);
        exit(EXIT_FAILURE);
    }

    char const *prefix = argv[1];
    char const *infile_name = argv[3];
    char const *outfile_name = argv[2];

    // Open Files
    FILE *infile = fopen(infile_name, "rb");

    if (!infile) {
        fprintf(stderr, "Unable to open input file: %s\n", infile_name);
        exit(EXIT_FAILURE);
    }

    FILE *outfile = fopen(outfile_name, "w");

    if (!outfile) {
        fprintf(stderr, "Uanble to open output file: %s\n", outfile_name);
        fclose(infile);
        exit(EXIT_FAILURE);
    }


    // Generate Header Guard Begin
    fprintf(outfile, "#ifndef %s_data_pack_h\n", prefix);
    fprintf(outfile, "#define %s_data_pack_h\n\n", prefix);


    // Generate Include Directive
    fprintf(outfile, "#include <stddef.h>\n\n");


    // Generate Encoded Data
    fprintf(outfile, "static const char %s_data[] =\n", prefix);

    size_t data_size = 0;
    for (;;) {
        unsigned char buf[256];
        unsigned char *ptr = buf;

        size_t nread = fread(buf, sizeof(char), sizeof(buf), infile);
        size_t line_count = nread / 16;
        size_t i;

        data_size += nread;

        for (i = 0; i < line_count; ++i, ptr += 16) {
            fprintf(outfile,
                    "\""
                    "\\x%02x\\x%02x\\x%02x\\x%02x"
                    "\\x%02x\\x%02x\\x%02x\\x%02x"
                    "\\x%02x\\x%02x\\x%02x\\x%02x"
                    "\\x%02x\\x%02x\\x%02x\\x%02x"
                    "\"\n",
                    ptr[0], ptr[1], ptr[2], ptr[3],
                    ptr[4], ptr[5], ptr[6], ptr[7],
                    ptr[8], ptr[9], ptr[10], ptr[11],
                    ptr[12], ptr[13], ptr[14], ptr[15]);
        }

        if (nread % 16 != 0) {
            fprintf(outfile, "\"");

            for (i = line_count * 16; i < nread; ++i) {
                fprintf(outfile, "\\x%02x", buf[i]);
            }

            fprintf(outfile, "\"\n");
        }

        if (nread != sizeof(buf)) {
            // End of file reached
            break;
        }
    }

    fprintf(outfile, ";\n\n");


    // Generate Data Size
    fprintf(outfile, "static const size_t %s_size = %lu;\n",
            prefix, (unsigned long)data_size);


    // Generate Header Guard End
    fprintf(outfile, "\n#endif\n");


    // Close Files
    fclose(infile);
    fclose(outfile);

    return EXIT_SUCCESS;
}
