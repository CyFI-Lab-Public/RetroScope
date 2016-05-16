#include <cstdlib>
#include <ctime>
#include <fstream>
#include <iostream>
#include <limits>
#include <string>
#include <utility>
#include <vector>

#include <marisa.h>

#include "./cmdopt.h"

namespace {

typedef std::pair<std::string, double> Key;

int param_min_num_tries = 1;
int param_max_num_tries = 10;
int param_trie = MARISA_DEFAULT_TRIE;
int param_tail = MARISA_DEFAULT_TAIL;
int param_order = MARISA_DEFAULT_ORDER;
bool predict_strs_flag = false;
bool speed_flag = true;

class Clock {
 public:
  Clock() : cl_(std::clock()) {}

  void reset() {
    cl_ = std::clock();
  }

  double elasped() const {
    std::clock_t cur = std::clock();
    return (cur == cl_) ? 0.01 : (1.0 * (cur - cl_) / CLOCKS_PER_SEC);
  }

 private:
  std::clock_t cl_;
};

void print_help(const char *cmd) {
  std::cerr << "Usage: " << cmd << " [OPTION]... [FILE]...\n\n"
      "Options:\n"
      "  -N, --min-num-tries=[N]  limits the number of tries to N"
      " (default: 1)\n"
      "  -n, --max-num-tries=[N]  limits the number of tries to N"
      " (default: 10)\n"
      "  -P, --patricia-trie  build patricia tries (default)\n"
      "  -p, --prefix-trie    build prefix tries\n"
      "  -T, --text-tail      build a dictionary with text TAIL (default)\n"
      "  -b, --binary-tail    build a dictionary with binary TAIL\n"
      "  -t, --without-tail   build a dictionary without TAIL\n"
      "  -w, --weight-order   arrange siblings in weight order (default)\n"
      "  -l, --label-order    arrange siblings in label order\n"
      "  -I, --predict-ids    get key IDs in predictive searches (default)\n"
      "  -i, --predict-strs   restore key strings in predictive searches\n"
      "  -S, --print-speed    print speed [1000 keys/s] (default)\n"
      "  -s, --print-time     print time [us/key]\n"
      "  -h, --help           print this help\n"
      << std::endl;
}

void print_config() {
  std::cout << "#tries: " << param_min_num_tries
      << " - " << param_max_num_tries << std::endl;

  switch (param_trie) {
    case MARISA_PATRICIA_TRIE: {
      std::cout << "trie: patricia" << std::endl;
      break;
    }
    case MARISA_PREFIX_TRIE: {
      std::cout << "trie: prefix" << std::endl;
      break;
    }
  }

  switch (param_tail) {
    case MARISA_WITHOUT_TAIL: {
      std::cout << "tail: no" << std::endl;
      break;
    }
    case MARISA_BINARY_TAIL: {
      std::cout << "tail: binary" << std::endl;
      break;
    }
    case MARISA_TEXT_TAIL: {
      std::cout << "tail: text" << std::endl;
      break;
    }
  }

  switch (param_order) {
    case MARISA_LABEL_ORDER: {
      std::cout << "order: label" << std::endl;
      break;
    }
    case MARISA_WEIGHT_ORDER: {
      std::cout << "order: weight" << std::endl;
      break;
    }
  }

  if (predict_strs_flag) {
    std::cout << "predict: both IDs and strings" << std::endl;
  } else {
    std::cout << "predict: only IDs" << std::endl;
  }
}

void print_time_info(std::size_t num_keys, double elasped) {
  if (speed_flag) {
    if (elasped == 0.0) {
      std::printf(" %7s", "-");
    } else {
      std::printf(" %7.2f", num_keys / elasped / 1000.0);
    }
  } else {
    if (num_keys == 0) {
      std::printf(" %7s", "-");
    } else {
      std::printf(" %7.3f", 1000000.0 * elasped / num_keys);
    }
  }
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

int read_keys(const char * const *args, std::size_t num_args,
    std::vector<Key> *keys) {
  if (num_args == 0) {
    read_keys(&std::cin, keys);
  }
  for (std::size_t i = 0; i < num_args; ++i) {
    std::ifstream input_file(args[i], std::ios::binary);
    if (!input_file) {
      std::cerr << "error: failed to open a keyset file: "
          << args[i] << std::endl;
      return 10;
    }
    read_keys(&input_file, keys);
  }
  std::cout << "#keys: " << keys->size() << std::endl;
  std::size_t total_length = 0;
  for (std::size_t i = 0; i < keys->size(); ++i) {
    total_length += (*keys)[i].first.length();
  }
  std::cout << "total length: " << total_length << std::endl;
  return 0;
}

void benchmark_build(const std::vector<Key> &keys, int num_tries,
    marisa::Trie *trie, std::vector<marisa::UInt32> *key_ids) {
  Clock cl;
  trie->build(keys, key_ids, num_tries
      | param_trie | param_tail | param_order);
  std::printf(" %9lu", (unsigned long)trie->num_nodes());
  std::printf(" %9lu", (unsigned long)trie->total_size());
  print_time_info(keys.size(), cl.elasped());
}

void benchmark_restore(const marisa::Trie &trie,
    const std::vector<Key> &keys,
    const std::vector<marisa::UInt32> &key_ids) {
  Clock cl;
  std::string key;
  for (std::size_t i = 0; i < key_ids.size(); ++i) {
    key.clear();
    trie.restore(key_ids[i], &key);
    if (key != keys[i].first) {
      std::cerr << "error: restore() failed" << std::endl;
      return;
    }
  }
  print_time_info(key_ids.size(), cl.elasped());
}

void benchmark_lookup(const marisa::Trie &trie,
    const std::vector<Key> &keys,
    const std::vector<marisa::UInt32> &key_ids) {
  Clock cl;
  for (std::size_t i = 0; i < keys.size(); ++i) {
    const marisa::UInt32 key_id = trie.lookup(keys[i].first);
    if (key_id != key_ids[i]) {
      std::cerr << "error: lookup() failed" << std::endl;
      return;
    }
  }
  print_time_info(keys.size(), cl.elasped());
}

void benchmark_find(const marisa::Trie &trie,
    const std::vector<Key> &keys,
    const std::vector<marisa::UInt32> &key_ids) {
  Clock cl;
  std::vector<marisa::UInt32> found_key_ids;
  for (std::size_t i = 0; i < keys.size(); ++i) {
    found_key_ids.clear();
    const std::size_t num_keys = trie.find(keys[i].first, &found_key_ids);
    if ((num_keys == 0) || (found_key_ids.back() != key_ids[i])) {
      std::cerr << "error: find() failed" << std::endl;
      return;
    }
  }
  print_time_info(keys.size(), cl.elasped());
}

void benchmark_predict_breadth_first(const marisa::Trie &trie,
    const std::vector<Key> &keys,
    const std::vector<marisa::UInt32> &key_ids) {
  Clock cl;
  std::vector<marisa::UInt32> found_key_ids;
  std::vector<std::string> found_keys;
  std::vector<std::string> *found_keys_ref =
      predict_strs_flag ? &found_keys : NULL;
  for (std::size_t i = 0; i < keys.size(); ++i) {
    found_key_ids.clear();
    found_keys.clear();
    const std::size_t num_keys = trie.predict_breadth_first(
        keys[i].first, &found_key_ids, found_keys_ref);
    if ((num_keys == 0) || (found_key_ids.front() != key_ids[i])) {
      std::cerr << "error: predict() failed" << std::endl;
      return;
    }
  }
  print_time_info(keys.size(), cl.elasped());
}

void benchmark_predict_depth_first(const marisa::Trie &trie,
    const std::vector<Key> &keys,
    const std::vector<marisa::UInt32> &key_ids) {
  Clock cl;
  std::vector<marisa::UInt32> found_key_ids;
  std::vector<std::string> found_keys;
  std::vector<std::string> *found_keys_ref =
      predict_strs_flag ? &found_keys : NULL;
  for (std::size_t i = 0; i < keys.size(); ++i) {
    found_key_ids.clear();
    found_keys.clear();
    const std::size_t num_keys = trie.predict_depth_first(
        keys[i].first, &found_key_ids, found_keys_ref);
    if ((num_keys == 0) || (found_key_ids.front() != key_ids[i])) {
      std::cerr << "error: predict() failed" << std::endl;
      return;
    }
  }
  print_time_info(keys.size(), cl.elasped());
}

void benchmark(const std::vector<Key> &keys, int num_tries) {
  std::printf("%6d", num_tries);
  marisa::Trie trie;
  std::vector<marisa::UInt32> key_ids;
  benchmark_build(keys, num_tries, &trie, &key_ids);
  if (!trie.empty()) {
    benchmark_restore(trie, keys, key_ids);
    benchmark_lookup(trie, keys, key_ids);
    benchmark_find(trie, keys, key_ids);
    benchmark_predict_breadth_first(trie, keys, key_ids);
    benchmark_predict_depth_first(trie, keys, key_ids);
  }
  std::printf("\n");
}

int benchmark(const char * const *args, std::size_t num_args) try {
  std::vector<Key> keys;
  const int ret = read_keys(args, num_args, &keys);
  if (ret != 0) {
    return ret;
  }
  std::printf("------+---------+---------+-------+"
      "-------+-------+-------+-------+-------\n");
  std::printf("%6s %9s %9s %7s %7s %7s %7s %7s %7s\n",
      "#tries", "#nodes", "size",
      "build", "restore", "lookup", "find", "predict", "predict");
  std::printf("%6s %9s %9s %7s %7s %7s %7s %7s %7s\n",
      "", "", "", "", "", "", "", "breadth", "depth");
  if (speed_flag) {
    std::printf("%6s %9s %9s %7s %7s %7s %7s %7s %7s\n",
        "", "", "[bytes]",
        "[K/s]", "[K/s]", "[K/s]", "[K/s]", "[K/s]", "[K/s]");
  } else {
    std::printf("%6s %9s %9s %7s %7s %7s %7s %7s %7s\n",
        "", "", "[bytes]", "[us]", "[us]", "[us]", "[us]", "[us]", "[us]");
  }
  std::printf("------+---------+---------+-------+"
      "-------+-------+-------+-------+-------\n");
  for (int i = param_min_num_tries; i <= param_max_num_tries; ++i) {
    benchmark(keys, i);
  }
  std::printf("------+---------+---------+-------+"
      "-------+-------+-------+-------+-------\n");
  return 0;
} catch (const marisa::Exception &ex) {
  std::cerr << ex.filename() << ':' << ex.line()
      << ": " << ex.what() << std::endl;
  return -1;
}

}  // namespace

int main(int argc, char *argv[]) {
  std::ios::sync_with_stdio(false);

  ::cmdopt_option long_options[] = {
    { "min-num-tries", 1, NULL, 'N' },
    { "max-num-tries", 1, NULL, 'n' },
    { "patricia-trie", 0, NULL, 'P' },
    { "prefix-trie", 0, NULL, 'p' },
    { "text-tail", 0, NULL, 'T' },
    { "binary-tail", 0, NULL, 'b' },
    { "without-tail", 0, NULL, 't' },
    { "weight-order", 0, NULL, 'w' },
    { "label-order", 0, NULL, 'l' },
    { "predict-ids", 0, NULL, 'I' },
    { "predict-strs", 0, NULL, 'i' },
    { "print-speed", 0, NULL, 'S' },
    { "print-time", 0, NULL, 's' },
    { "help", 0, NULL, 'h' },
    { NULL, 0, NULL, 0 }
  };
  ::cmdopt_t cmdopt;
  ::cmdopt_init(&cmdopt, argc, argv, "N:n:PpTbtwlIiSsh", long_options);
  int label;
  while ((label = ::cmdopt_get(&cmdopt)) != -1) {
    switch (label) {
      case 'N': {
        char *end_of_value;
        const long value = std::strtol(cmdopt.optarg, &end_of_value, 10);
        if ((*end_of_value != '\0') || (value <= 0) ||
            (value > MARISA_MAX_NUM_TRIES)) {
          std::cerr << "error: option `-n' with an invalid argument: "
              << cmdopt.optarg << std::endl;
        }
        param_min_num_tries = (int)value;
        break;
      }
      case 'n': {
        char *end_of_value;
        const long value = std::strtol(cmdopt.optarg, &end_of_value, 10);
        if ((*end_of_value != '\0') || (value <= 0) ||
            (value > MARISA_MAX_NUM_TRIES)) {
          std::cerr << "error: option `-n' with an invalid argument: "
              << cmdopt.optarg << std::endl;
        }
        param_max_num_tries = (int)value;
        break;
      }
      case 'P': {
        param_trie = MARISA_PATRICIA_TRIE;
        break;
      }
      case 'p': {
        param_trie = MARISA_PREFIX_TRIE;
        break;
      }
      case 'T': {
        param_tail = MARISA_TEXT_TAIL;
        break;
      }
      case 'b': {
        param_tail = MARISA_BINARY_TAIL;
        break;
      }
      case 't': {
        param_tail = MARISA_WITHOUT_TAIL;
        break;
      }
      case 'w': {
        param_order = MARISA_WEIGHT_ORDER;
        break;
      }
      case 'l': {
        param_order = MARISA_LABEL_ORDER;
        break;
      }
      case 'I': {
        predict_strs_flag = false;
        break;
      }
      case 'i': {
        predict_strs_flag = true;
        break;
      }
      case 'S': {
        speed_flag = true;
        break;
      }
      case 's': {
        speed_flag = false;
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
  print_config();
  return benchmark(cmdopt.argv + cmdopt.optind, cmdopt.argc - cmdopt.optind);
}
