#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string>

void usage(const char* me) {
  static const char* usage_s = "Usage:\n"
    "  %s /system/bin/app_process <args>\n"
    "or, better:\n"
    "  setprop wrap.<nicename> %s\n";
  fprintf(stderr, usage_s, me, me);
  exit(1);
}

void env_prepend(const char* name, const char* value, const char* delim) {
  const char* value_old = getenv(name);
  std::string value_new = value;
  if (value_old) {
    value_new += delim;
    value_new += value_old;
  }
  setenv(name, value_new.c_str(), 1);
}

int main(int argc, char** argv) {
  if (argc < 2) {
    usage(argv[0]);
  }
  char** args = new char*[argc];
  // If we are wrapping app_process, replace it with app_process_asan.
  // TODO(eugenis): rewrite to <dirname>/asan/<basename>, if exists?
  if (strcmp(argv[1], "/system/bin/app_process") == 0) {
    args[0] = (char*)"/system/bin/asan/app_process";
  } else {
    args[0] = argv[1];
  }

  for (int i = 1; i < argc - 1; ++i)
    args[i] = argv[i + 1];
  args[argc - 1] = 0;

  env_prepend("ASAN_OPTIONS", "debug=1,verbosity=1", ",");
  env_prepend("LD_LIBRARY_PATH", "/system/lib/asan", ":");
  env_prepend("LD_PRELOAD", "/system/lib/libasan_preload.so", ":");

  printf("ASAN_OPTIONS: %s\n", getenv("ASAN_OPTIONS"));
  printf("LD_LIBRARY_PATH: %s\n", getenv("LD_LIBRARY_PATH"));
  printf("LD_PRELOAD: %s\n", getenv("LD_PRELOAD"));

  execv(args[0], args);
}
