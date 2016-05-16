#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>
#include <sepol/sepol.h>
#include <selinux/selinux.h>
#include <selinux/label.h>

static int nerr;

static int validate(char **contextp)
{
  char *context = *contextp;
  if (sepol_check_context(context) < 0) {
    nerr++;
    return -1;
  }
  return 0;
}

static void usage(char *name) {
    fprintf(stderr, "usage:  %s [OPTIONS] sepolicy context_file\n\n", name);
    fprintf(stderr, "Parses a context file and checks for syntax errors.\n");
    fprintf(stderr, "The context_file is assumed to be a file_contexts file\n");
    fprintf(stderr, "unless explicitly switched by an option.\n\n");
    fprintf(stderr, "    OPTIONS:\n");
    fprintf(stderr, "     -p : context file represents a property_context file.\n");
    fprintf(stderr, "\n");
    exit(1);
}

int main(int argc, char **argv)
{
  struct selinux_opt opts[] = {
    { SELABEL_OPT_VALIDATE, (void*)1 },
    { SELABEL_OPT_PATH, NULL }
  };

  // Default backend unless changed by input argument.
  unsigned int backend = SELABEL_CTX_FILE;

  FILE *fp;
  struct selabel_handle *sehnd;
  char c;

  while ((c = getopt(argc, argv, "ph")) != -1) {
    switch (c) {
      case 'p':
        backend = SELABEL_CTX_ANDROID_PROP;
        break;
      case 'h':
      default:
        usage(argv[0]);
        break;
    }
  }

  int index = optind;
  if (argc - optind != 2) {
    fprintf(stderr, "Expected sepolicy file and context file as arguments.\n");
    usage(argv[0]);
  }

  // remaining args are sepolicy file and context file
  char *sepolicyFile = argv[index];
  char *contextFile = argv[index + 1];

  fp = fopen(sepolicyFile, "r");
  if (!fp) {
    perror(sepolicyFile);
    exit(2);
  }
  if (sepol_set_policydb_from_file(fp) < 0) {
    fprintf(stderr, "Error loading policy from %s\n", sepolicyFile);
    exit(3);
  }

  selinux_set_callback(SELINUX_CB_VALIDATE,
                       (union selinux_callback)&validate);

  opts[1].value = contextFile;

  sehnd = selabel_open(backend, opts, 2);
  if (!sehnd) {
    fprintf(stderr, "Error loading context file from %s\n", contextFile);
    exit(4);
  }
  if (nerr) {
    fprintf(stderr, "Invalid context file found in %s\n", contextFile);
    exit(5);
  }

  exit(0);
}
