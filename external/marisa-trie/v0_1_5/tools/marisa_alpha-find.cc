#include <cstdlib>
#include <iostream>
#include <limits>
#include <string>
#include <vector>

#include <marisa_alpha.h>

#include "./cmdopt.h"

namespace {

enum FindMode {
  FIND_ALL,
  FIND_FIRST,
  FIND_LAST
};

std::size_t max_num_results = 10;
FindMode find_mode = FIND_ALL;
bool mmap_flag = true;

void print_help(const char *cmd) {
  std::cerr << "Usage: " << cmd << " [OPTION]... DIC\n\n"
      "Options:\n"
      "  -n, --max-num-results=[N]  limits the number of results to N"
      " (default: 10)\n"
      "                             0: no limit\n"
      "  -a, --find-all         find all prefix keys (default)\n"
      "  -f, --find-first       find a shortest prefix key\n"
      "  -l, --find-last        find a longest prefix key\n"
      "  -m, --mmap-dictionary  use memory-mapped I/O to load a dictionary"
      " (default)\n"
      "  -r, --read-dictionary  read an entire dictionary into memory\n"
      "  -h, --help             print this help\n"
      << std::endl;
}

void find_all(const marisa_alpha::Trie &trie, const std::string &str) {
  static std::vector<marisa_alpha::UInt32> key_ids;
  static std::vector<std::size_t> lengths;
  const std::size_t num_keys = trie.find(str, &key_ids, &lengths);
  if (num_keys != 0) {
    std::cout << num_keys << " found" << std::endl;
    for (std::size_t i = 0; (i < num_keys) && (i < max_num_results); ++i) {
      std::cout << key_ids[i] << '\t';
      std::cout.write(str.c_str(), lengths[i]) << '\t' << str << '\n';
    }
  } else {
    std::cout << "not found" << std::endl;
  }
  key_ids.clear();
  lengths.clear();
}

void find_first(const marisa_alpha::Trie &trie, const std::string &str) {
  std::size_t length = 0;
  const marisa_alpha::UInt32 key_id = trie.find_first(str, &length);
  if (key_id != trie.notfound()) {
    std::cout << key_id << '\t';
    std::cout.write(str.c_str(), length) << '\t' << str << '\n';
  } else {
    std::cout << "-1\t" << str << '\n';
  }
}

void find_last(const marisa_alpha::Trie &trie, const std::string &str) {
  std::size_t length = 0;
  const marisa_alpha::UInt32 key_id = trie.find_last(str, &length);
  if (key_id != trie.notfound()) {
    std::cout << key_id << '\t';
    std::cout.write(str.c_str(), length) << '\t' << str << '\n';
  } else {
    std::cout << "-1\t" << str << '\n';
  }
}

int find(const char * const *args, std::size_t num_args) {
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

  std::string str;
  while (std::getline(std::cin, str)) {
    try {
      switch (find_mode) {
        case FIND_ALL: {
          find_all(trie, str);
          break;
        }
        case FIND_FIRST: {
          find_first(trie, str);
          break;
        }
        case FIND_LAST: {
          find_last(trie, str);
          break;
        }
      }
    } catch (const marisa_alpha::Exception &ex) {
      std::cerr << ex.filename() << ':' << ex.line() << ": " << ex.what()
          << ": failed to find keys in: " << str << std::endl;
      return 30;
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
    { "find-all", 0, NULL, 'a' },
    { "find-first", 0, NULL, 'f' },
    { "find-last", 0, NULL, 'l' },
    { "mmap-dictionary", 0, NULL, 'm' },
    { "read-dictionary", 0, NULL, 'r' },
    { "help", 0, NULL, 'h' },
    { NULL, 0, NULL, 0 }
  };
  ::cmdopt_t cmdopt;
  ::cmdopt_init(&cmdopt, argc, argv, "n:aflmrh", long_options);
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
      case 'a': {
        find_mode = FIND_ALL;
        break;
      }
      case 'f': {
        find_mode = FIND_FIRST;
        break;
      }
      case 'l': {
        find_mode = FIND_LAST;
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
  return find(cmdopt.argv + cmdopt.optind, cmdopt.argc - cmdopt.optind);
}
