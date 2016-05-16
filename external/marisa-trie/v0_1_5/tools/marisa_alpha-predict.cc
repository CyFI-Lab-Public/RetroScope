#include <cstdlib>
#include <iostream>
#include <limits>
#include <string>
#include <vector>

#include <marisa_alpha.h>

#include "./cmdopt.h"

namespace {

std::size_t max_num_results = 10;
bool mmap_flag = true;
bool depth_first_flag = true;

void print_help(const char *cmd) {
  std::cerr << "Usage: " << cmd << " [OPTION]... DIC\n\n"
      "Options:\n"
      "  -n, --max-num-results=[N]  limits the number of outputs to N"
      " (default: 10)\n"
      "                             0: no limit\n"
      "  -d, --depth-first      predict keys in depth first order(default)\n"
      "  -b, --breadth-first    predict keys in breadth first order\n"
      "  -m, --mmap-dictionary  use memory-mapped I/O to load a dictionary"
      " (default)\n"
      "  -r, --read-dictionary  read an entire dictionary into memory\n"
      "  -h, --help             print this help\n"
      << std::endl;
}

int predict(const char * const *args, std::size_t num_args) {
  if (num_args == 0) {
    std::cerr << "error: a dictionary is not specified" << std::endl;
    return 10;
  } else if (num_args > 1) {
    std::cerr << "error: more than one dictionaries are specified"
        << std::endl;
    return 11;
  }

  marisa_alpha::Trie trie;
  marisa_alpha::Mapper mapper;
  if (mmap_flag) {
    try {
      trie.mmap(&mapper, args[0]);
    } catch (const marisa_alpha::Exception &ex) {
      std::cerr << ex.filename() << ':' << ex.line() << ": " << ex.what()
          << ": failed to mmap a dictionary file: " << args[0] << std::endl;
      return 20;
    }
  } else {
    try {
      trie.load(args[0]);
    } catch (const marisa_alpha::Exception &ex) {
      std::cerr << ex.filename() << ':' << ex.line() << ": " << ex.what()
          << ": failed to load a dictionary file: " << args[0] << std::endl;
      return 21;
    }
  }

  std::vector<marisa_alpha::UInt32> key_ids;
  std::vector<std::string> keys;
  std::string str;
  while (std::getline(std::cin, str)) {
    std::size_t num_keys = trie.predict(str);
    if (num_keys != 0) {
      std::cout << num_keys << " found" << std::endl;
      key_ids.clear();
      keys.clear();
      try {
        if (depth_first_flag) {
          num_keys = trie.predict_depth_first(
              str, &key_ids, &keys, max_num_results);
        } else {
          num_keys = trie.predict_breadth_first(
              str, &key_ids, &keys, max_num_results);
        }
      } catch (const marisa_alpha::Exception &ex) {
        std::cerr << ex.filename() << ':' << ex.line() << ": " << ex.what()
            << ": failed to predict keys from: " << str << std::endl;
        return 30;
      }
      for (std::size_t i = 0; i < num_keys; ++i) {
        std::cout << key_ids[i] << '\t' << keys[i] << '\t' << str << '\n';
      }
    } else {
      std::cout << "not found" << std::endl;
    }
    if (!std::cout) {
      std::cerr << "error: failed to write results to standard output"
          << std::endl;
      return 31;
    }
  }

  return 0;
}

}  // namespace

int main(int argc, char *argv[]) {
  std::ios::sync_with_stdio(false);

  ::cmdopt_option long_options[] = {
    { "max-num-results", 1, NULL, 'n' },
    { "depth-first", 0, NULL, 'd' },
    { "breadth-first", 0, NULL, 'b' },
    { "mmap-dictionary", 0, NULL, 'm' },
    { "read-dictionary", 0, NULL, 'r' },
    { "help", 0, NULL, 'h' },
    { NULL, 0, NULL, 0 }
  };
  ::cmdopt_t cmdopt;
  ::cmdopt_init(&cmdopt, argc, argv, "n:dbmrh", long_options);
  int label;
  while ((label = ::cmdopt_get(&cmdopt)) != -1) {
    switch (label) {
      case 'n': {
        char *end_of_value;
        const long value = std::strtol(cmdopt.optarg, &end_of_value, 10);
        if ((*end_of_value != '\0') || (value < 0)) {
          std::cerr << "error: option `-n' with an invalid argument: "
              << cmdopt.optarg << std::endl;
        }
        if ((value == 0) ||
            ((unsigned long)value > MARISA_ALPHA_MAX_NUM_KEYS)) {
          max_num_results = MARISA_ALPHA_MAX_NUM_KEYS;
        } else {
          max_num_results = (std::size_t)(value);
        }
        break;
      }
      case 'd': {
        depth_first_flag = true;
        break;
      }
      case 'b': {
        depth_first_flag = false;
        break;
      }
      case 'm': {
        mmap_flag = true;
        break;
      }
      case 'r': {
        mmap_flag = false;
        break;
      }
      case 'h': {
        print_help(argv[0]);
        return 0;
      }
      default: {
        return 1;
      }
    }
  }
  return predict(cmdopt.argv + cmdopt.optind, cmdopt.argc - cmdopt.optind);
}
