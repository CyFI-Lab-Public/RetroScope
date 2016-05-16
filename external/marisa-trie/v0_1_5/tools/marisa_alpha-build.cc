#include <cstdlib>
#include <fstream>
#include <iostream>
#include <limits>
#include <string>
#include <utility>
#include <vector>

#include <marisa_alpha.h>

#include "./cmdopt.h"

namespace {

typedef std::pair<std::string, double> Key;

int param_num_tries = MARISA_ALPHA_DEFAULT_NUM_TRIES;
int param_trie = MARISA_ALPHA_DEFAULT_TRIE;
int param_tail = MARISA_ALPHA_DEFAULT_TAIL;
int param_order = MARISA_ALPHA_DEFAULT_ORDER;
const char *output_filename = NULL;

void print_help(const char *cmd) {
  std::cerr << "Usage: " << cmd << " [OPTION]... [FILE]...\n\n"
      "Options:\n"
      "  -n, --num-tries=[N]  limits the number of tries to N"
      " (default: 3)\n"
      "  -P, --patricia-trie  build patricia tries (default)\n"
      "  -p, --prefix-trie    build prefix tries\n"
      "  -T, --text-tail      build a dictionary with text TAIL (default)\n"
      "  -b, --binary-tail    build a dictionary with binary TAIL\n"
      "  -t, --without-tail   build a dictionary without TAIL\n"
      "  -w, --weight-order   arranges siblings in weight order (default)\n"
      "  -l, --label-order    arranges siblings in label order\n"
      "  -o, --output=[FILE]  write tries to FILE (default: stdout)\n"
      "  -h, --help           print this help\n"
      << std::endl;
}

void read_keys(std::istream *input, std::vector<Key> *keys) {
  Key key;
  std::string line;
  while (std::getline(*input, line)) {
    const std::string::size_type delim_pos = line.find_last_of('\t');
    if (delim_pos != line.npos) {
      char *end_of_value;
      key.second = std::strtod(&line[delim_pos + 1], &end_of_value);
      if (*end_of_value == '\0') {
        line.resize(delim_pos);
      } else {
        key.second = 1.0;
      }
    } else {
      key.second = 1.0;
    }
    key.first = line;
    keys->push_back(key);
  }
}

int build(const char * const *args, std::size_t num_args) {
  std::vector<Key> keys;
  if (num_args == 0) {
    read_keys(&std::cin, &keys);
  }

  for (std::size_t i = 0; i < num_args; ++i) {
    std::ifstream input_file(args[i], std::ios::binary);
    if (!input_file) {
      std::cerr << "error: failed to open a keyset file: "
          << args[i] << std::endl;
      return 10;
    }
    read_keys(&input_file, &keys);
  }

  marisa_alpha::Trie trie;
  try {
    trie.build(keys, NULL, param_num_tries
        | param_trie | param_tail | param_order);
  } catch (const marisa_alpha::Exception &ex) {
    std::cerr << ex.filename() << ':' << ex.line() << ": " << ex.what()
        << ": failed to build a dictionary" << std::endl;
    return 20;
  }

  std::cerr << "#keys: " << trie.num_keys() << std::endl;
  std::cerr << "#tries: " << trie.num_tries() << std::endl;
  std::cerr << "#nodes: " << trie.num_nodes() << std::endl;
  std::cerr << "size: " << trie.total_size() << std::endl;

  if (output_filename != NULL) {
    try {
      trie.save(output_filename);
    } catch (const marisa_alpha::Exception &ex) {
      std::cerr << ex.filename() << ':' << ex.line() << ": " << ex.what()
          << ": failed to write a dictionary to file: "
          << output_filename << std::endl;
      return 30;
    }
  } else {
    try {
      trie.write(std::cout);
    } catch (const marisa_alpha::Exception &ex) {
      std::cerr << ex.filename() << ':' << ex.line() << ": " << ex.what()
          << ": failed to write a dictionary to standard output" << std::endl;
      return 31;
    }
  }
  return 0;
}

}  // namespace

int main(int argc, char *argv[]) {
  std::ios::sync_with_stdio(false);

  ::cmdopt_option long_options[] = {
    { "max-num-tries", 1, NULL, 'n' },
    { "patricia-trie", 0, NULL, 'P' },
    { "prefix-trie", 0, NULL, 'p' },
    { "text-tail", 0, NULL, 'T' },
    { "binary-tail", 0, NULL, 'b' },
    { "without-tail", 0, NULL, 't' },
    { "weight-order", 0, NULL, 'w' },
    { "label-order", 0, NULL, 'l' },
    { "output", 1, NULL, 'o' },
    { "help", 0, NULL, 'h' },
    { NULL, 0, NULL, 0 }
  };
  ::cmdopt_t cmdopt;
  ::cmdopt_init(&cmdopt, argc, argv, "n:PpTbtwlo:h", long_options);
  int label;
  while ((label = ::cmdopt_get(&cmdopt)) != -1) {
    switch (label) {
      case 'n': {
        char *end_of_value;
        const long value = std::strtol(cmdopt.optarg, &end_of_value, 10);
        if ((*end_of_value != '\0') || (value <= 0) ||
            (value > MARISA_ALPHA_MAX_NUM_TRIES)) {
          std::cerr << "error: option `-n' with an invalid argument: "
              << cmdopt.optarg << std::endl;
        }
        param_num_tries = (int)value;
        break;
      }
      case 'P': {
        param_trie = MARISA_ALPHA_PATRICIA_TRIE;
        break;
      }
      case 'p': {
        param_trie = MARISA_ALPHA_PREFIX_TRIE;
        break;
      }
      case 'T': {
        param_tail = MARISA_ALPHA_TEXT_TAIL;
        break;
      }
      case 'b': {
        param_tail = MARISA_ALPHA_BINARY_TAIL;
        break;
      }
      case 't': {
        param_tail = MARISA_ALPHA_WITHOUT_TAIL;
        break;
      }
      case 'w': {
        param_order = MARISA_ALPHA_WEIGHT_ORDER;
        break;
      }
      case 'l': {
        param_order = MARISA_ALPHA_LABEL_ORDER;
        break;
      }
      case 'o': {
        output_filename = cmdopt.optarg;
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
  return build(cmdopt.argv + cmdopt.optind, cmdopt.argc - cmdopt.optind);
}
