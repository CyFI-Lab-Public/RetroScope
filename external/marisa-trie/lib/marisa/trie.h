#ifndef MARISA_TRIE_H_
#define MARISA_TRIE_H_

#include "base.h"

#ifdef __cplusplus

#include <memory>
#include <vector>

#include "progress.h"
#include "key.h"
#include "query.h"
#include "container.h"
#include "intvector.h"
#include "bitvector.h"
#include "tail.h"

namespace marisa {

class Trie {
 public:
  Trie();

  void build(const char * const *keys, std::size_t num_keys,
      const std::size_t *key_lengths = NULL,
      const double *key_weights = NULL,
      UInt32 *key_ids = NULL, int flags = 0);

  void build(const std::vector<std::string> &keys,
      std::vector<UInt32> *key_ids = NULL, int flags = 0);
  void build(const std::vector<std::pair<std::string, double> > &keys,
      std::vector<UInt32> *key_ids = NULL, int flags = 0);

  void mmap(Mapper *mapper, const char *filename,
      long offset = 0, int whence = SEEK_SET);
  void map(const void *ptr, std::size_t size);
  void map(Mapper &mapper);

  void load(const char *filename,
      long offset = 0, int whence = SEEK_SET);
  void fread(std::FILE *file);
  void read(int fd);
  void read(std::istream &stream);
  void read(Reader &reader);

  void save(const char *filename, bool trunc_flag = true,
      long offset = 0, int whence = SEEK_SET) const;
  void fwrite(std::FILE *file) const;
  void write(int fd) const;
  void write(std::ostream &stream) const;
  void write(Writer &writer) const;

  std::string operator[](UInt32 key_id) const;

  UInt32 operator[](const char *str) const;
  UInt32 operator[](const std::string &str) const;

  std::string restore(UInt32 key_id) const;
  void restore(UInt32 key_id, std::string *key) const;
  std::size_t restore(UInt32 key_id, char *key_buf,
      std::size_t key_buf_size) const;

  UInt32 lookup(const char *str) const;
  UInt32 lookup(const char *ptr, std::size_t length) const;
  UInt32 lookup(const std::string &str) const;

  std::size_t find(const char *str,
      UInt32 *key_ids, std::size_t *key_lengths,
      std::size_t max_num_results) const;
  std::size_t find(const char *ptr, std::size_t length,
      UInt32 *key_ids, std::size_t *key_lengths,
      std::size_t max_num_results) const;
  std::size_t find(const std::string &str,
      UInt32 *key_ids, std::size_t *key_lengths,
      std::size_t max_num_results) const;

  std::size_t find(const char *str,
      std::vector<UInt32> *key_ids = NULL,
      std::vector<std::size_t> *key_lengths = NULL,
      std::size_t max_num_results = MARISA_MAX_NUM_KEYS) const;
  std::size_t find(const char *ptr, std::size_t length,
      std::vector<UInt32> *key_ids = NULL,
      std::vector<std::size_t> *key_lengths = NULL,
      std::size_t max_num_results = MARISA_MAX_NUM_KEYS) const;
  std::size_t find(const std::string &str,
      std::vector<UInt32> *key_ids = NULL,
      std::vector<std::size_t> *key_lengths = NULL,
      std::size_t max_num_results = MARISA_MAX_NUM_KEYS) const;

  UInt32 find_first(const char *str,
      std::size_t *key_length = NULL) const;
  UInt32 find_first(const char *ptr, std::size_t length,
      std::size_t *key_length = NULL) const;
  UInt32 find_first(const std::string &str,
      std::size_t *key_length = NULL) const;

  UInt32 find_last(const char *str,
      std::size_t *key_length = NULL) const;
  UInt32 find_last(const char *ptr, std::size_t length,
      std::size_t *key_length = NULL) const;
  UInt32 find_last(const std::string &str,
      std::size_t *key_length = NULL) const;

  // bool callback(UInt32 key_id, std::size_t key_length);
  template <typename T>
  std::size_t find_callback(const char *str, T callback) const;
  template <typename T>
  std::size_t find_callback(const char *ptr, std::size_t length,
      T callback) const;
  template <typename T>
  std::size_t find_callback(const std::string &str, T callback) const;

  std::size_t predict(const char *str,
      UInt32 *key_ids, std::string *keys, std::size_t max_num_results) const;
  std::size_t predict(const char *ptr, std::size_t length,
      UInt32 *key_ids, std::string *keys, std::size_t max_num_results) const;
  std::size_t predict(const std::string &str,
      UInt32 *key_ids, std::string *keys, std::size_t max_num_results) const;

  std::size_t predict(const char *str,
      std::vector<UInt32> *key_ids = NULL,
      std::vector<std::string> *keys = NULL,
      std::size_t max_num_results = MARISA_MAX_NUM_KEYS) const;
  std::size_t predict(const char *ptr, std::size_t length,
      std::vector<UInt32> *key_ids = NULL,
      std::vector<std::string> *keys = NULL,
      std::size_t max_num_results = MARISA_MAX_NUM_KEYS) const;
  std::size_t predict(const std::string &str,
      std::vector<UInt32> *key_ids = NULL,
      std::vector<std::string> *keys = NULL,
      std::size_t max_num_results = MARISA_MAX_NUM_KEYS) const;

  std::size_t predict_breadth_first(const char *str,
      UInt32 *key_ids, std::string *keys, std::size_t max_num_results) const;
  std::size_t predict_breadth_first(const char *ptr, std::size_t length,
      UInt32 *key_ids, std::string *keys, std::size_t max_num_results) const;
  std::size_t predict_breadth_first(const std::string &str,
      UInt32 *key_ids, std::string *keys, std::size_t max_num_results) const;

  std::size_t predict_breadth_first(const char *str,
      std::vector<UInt32> *key_ids = NULL,
      std::vector<std::string> *keys = NULL,
      std::size_t max_num_results = MARISA_MAX_NUM_KEYS) const;
  std::size_t predict_breadth_first(const char *ptr, std::size_t length,
      std::vector<UInt32> *key_ids = NULL,
      std::vector<std::string> *keys = NULL,
      std::size_t max_num_results = MARISA_MAX_NUM_KEYS) const;
  std::size_t predict_breadth_first(const std::string &str,
      std::vector<UInt32> *key_ids = NULL,
      std::vector<std::string> *keys = NULL,
      std::size_t max_num_results = MARISA_MAX_NUM_KEYS) const;

  std::size_t predict_depth_first(const char *str,
      UInt32 *key_ids, std::string *keys, std::size_t max_num_results) const;
  std::size_t predict_depth_first(const char *ptr, std::size_t length,
      UInt32 *key_ids, std::string *keys, std::size_t max_num_results) const;
  std::size_t predict_depth_first(const std::string &str,
      UInt32 *key_ids, std::string *keys, std::size_t max_num_results) const;

  std::size_t predict_depth_first(const char *str,
      std::vector<UInt32> *key_ids = NULL,
      std::vector<std::string> *keys = NULL,
      std::size_t max_num_results = MARISA_MAX_NUM_KEYS) const;
  std::size_t predict_depth_first(const char *ptr, std::size_t length,
      std::vector<UInt32> *key_ids = NULL,
      std::vector<std::string> *keys = NULL,
      std::size_t max_num_results = MARISA_MAX_NUM_KEYS) const;
  std::size_t predict_depth_first(const std::string &str,
      std::vector<UInt32> *key_ids = NULL,
      std::vector<std::string> *keys = NULL,
      std::size_t max_num_results = MARISA_MAX_NUM_KEYS) const;

  // bool callback(UInt32 key_id, const std::string &key);
  template <typename T>
  std::size_t predict_callback(const char *str, T callback) const;
  template <typename T>
  std::size_t predict_callback(const char *ptr, std::size_t length,
      T callback) const;
  template <typename T>
  std::size_t predict_callback(const std::string &str, T callback) const;

  bool empty() const;
  std::size_t num_tries() const;
  std::size_t num_keys() const;
  std::size_t num_nodes() const;
  std::size_t total_size() const;

  void clear();
  void swap(Trie *rhs);

  static UInt32 notfound();
  static std::size_t mismatch();

 private:
  BitVector louds_;
  Vector<UInt8> labels_;
  BitVector terminal_flags_;
  BitVector link_flags_;
  IntVector links_;
  std::auto_ptr<Trie> trie_;
  Tail tail_;
  UInt32 num_first_branches_;
  UInt32 num_keys_;

  void build_trie(Vector<Key<String> > &keys,
      std::vector<UInt32> *key_ids, int flags);
  void build_trie(Vector<Key<String> > &keys,
      UInt32 *key_ids, int flags);

  template <typename T>
  void build_trie(Vector<Key<T> > &keys,
      Vector<UInt32> *terminals, Progress &progress);

  template <typename T>
  void build_cur(Vector<Key<T> > &keys,
      Vector<UInt32> *terminals, Progress &progress);

  void build_next(Vector<Key<String> > &keys,
      Vector<UInt32> *terminals, Progress &progress);
  void build_next(Vector<Key<RString> > &rkeys,
      Vector<UInt32> *terminals, Progress &progress);

  template <typename T>
  UInt32 sort_keys(Vector<Key<T> > &keys) const;

  template <typename T>
  void build_terminals(const Vector<Key<T> > &keys,
      Vector<UInt32> *terminals) const;

  void restore_(UInt32 key_id, std::string *key) const;
  void trie_restore(UInt32 node, std::string *key) const;
  void tail_restore(UInt32 node, std::string *key) const;

  std::size_t restore_(UInt32 key_id, char *key_buf,
      std::size_t key_buf_size) const;
  void trie_restore(UInt32 node, char *key_buf,
      std::size_t key_buf_size, std::size_t &key_pos) const;
  void tail_restore(UInt32 node, char *key_buf,
      std::size_t key_buf_size, std::size_t &key_pos) const;

  template <typename T>
  UInt32 lookup_(T query) const;
  template <typename T>
  bool find_child(UInt32 &node, T query, std::size_t &pos) const;
  template <typename T>
  std::size_t trie_match(UInt32 node, T query, std::size_t pos) const;
  template <typename T>
  std::size_t tail_match(UInt32 node, UInt32 link_id,
      T query, std::size_t pos) const;

  template <typename T, typename U, typename V>
  std::size_t find_(T query, U key_ids, V key_lengths,
      std::size_t max_num_results) const;
  template <typename T>
  UInt32 find_first_(T query, std::size_t *key_length) const;
  template <typename T>
  UInt32 find_last_(T query, std::size_t *key_length) const;
  template <typename T, typename U>
  std::size_t find_callback_(T query, U callback) const;

  template <typename T, typename U, typename V>
  std::size_t predict_breadth_first_(T query, U key_ids, V keys,
      std::size_t max_num_results) const;
  template <typename T, typename U, typename V>
  std::size_t predict_depth_first_(T query, U key_ids, V keys,
      std::size_t max_num_results) const;
  template <typename T, typename U>
  std::size_t predict_callback_(T query, U callback) const;

  template <typename T>
  bool predict_child(UInt32 &node, T query, std::size_t &pos,
      std::string *key) const;
  template <typename T>
  std::size_t trie_prefix_match(UInt32 node, T query,
      std::size_t pos, std::string *key) const;
  template <typename T>
  std::size_t tail_prefix_match(UInt32 node, UInt32 link_id,
      T query, std::size_t pos, std::string *key) const;

  UInt32 key_id_to_node(UInt32 key_id) const;
  UInt32 node_to_key_id(UInt32 node) const;
  UInt32 louds_pos_to_node(UInt32 louds_pos, UInt32 parent_node) const;

  UInt32 get_child(UInt32 node) const;
  UInt32 get_parent(UInt32 node) const;

  bool has_link(UInt32 node) const;
  UInt32 get_link_id(UInt32 node) const;
  UInt32 get_link(UInt32 node) const;
  UInt32 get_link(UInt32 node, UInt32 link_id) const;

  bool has_link() const;
  bool has_trie() const;
  bool has_tail() const;

  // Disallows copy and assignment.
  Trie(const Trie &);
  Trie &operator=(const Trie &);
};

}  // namespace marisa

#include "trie-inline.h"

#else  // __cplusplus

#include <stdio.h>

#endif  // __cplusplus

#ifdef __cplusplus
extern "C" {
#endif  // __cplusplus

typedef struct marisa_trie_ marisa_trie;

marisa_status marisa_init(marisa_trie **h);
marisa_status marisa_end(marisa_trie *h);

marisa_status marisa_build(marisa_trie *h, const char * const *keys,
    size_t num_keys, const size_t *key_lengths, const double *key_weights,
    marisa_uint32 *key_ids, int flags);

marisa_status marisa_mmap(marisa_trie *h, const char *filename,
    long offset, int whence);
marisa_status marisa_map(marisa_trie *h, const void *ptr, size_t size);

marisa_status marisa_load(marisa_trie *h, const char *filename,
    long offset, int whence);
marisa_status marisa_fread(marisa_trie *h, FILE *file);
marisa_status marisa_read(marisa_trie *h, int fd);

marisa_status marisa_save(const marisa_trie *h, const char *filename,
    int trunc_flag, long offset, int whence);
marisa_status marisa_fwrite(const marisa_trie *h, FILE *file);
marisa_status marisa_write(const marisa_trie *h, int fd);

marisa_status marisa_restore(const marisa_trie *h, marisa_uint32 key_id,
    char *key_buf, size_t key_buf_size, size_t *key_length);

marisa_status marisa_lookup(const marisa_trie *h,
    const char *ptr, size_t length, marisa_uint32 *key_id);

marisa_status marisa_find(const marisa_trie *h,
    const char *ptr, size_t length,
    marisa_uint32 *key_ids, size_t *key_lengths,
    size_t max_num_results, size_t *num_results);
marisa_status marisa_find_first(const marisa_trie *h,
    const char *ptr, size_t length,
    marisa_uint32 *key_id, size_t *key_length);
marisa_status marisa_find_last(const marisa_trie *h,
    const char *ptr, size_t length,
    marisa_uint32 *key_id, size_t *key_length);
marisa_status marisa_find_callback(const marisa_trie *h,
    const char *ptr, size_t length,
    int (*callback)(void *, marisa_uint32, size_t),
    void *first_arg_to_callback);

marisa_status marisa_predict(const marisa_trie *h,
    const char *ptr, size_t length, marisa_uint32 *key_ids,
    size_t max_num_results, size_t *num_results);
marisa_status marisa_predict_breadth_first(const marisa_trie *h,
    const char *ptr, size_t length, marisa_uint32 *key_ids,
    size_t max_num_results, size_t *num_results);
marisa_status marisa_predict_depth_first(const marisa_trie *h,
    const char *ptr, size_t length, marisa_uint32 *key_ids,
    size_t max_num_results, size_t *num_results);
marisa_status marisa_predict_callback(const marisa_trie *h,
    const char *ptr, size_t length,
    int (*callback)(void *, marisa_uint32, const char *, size_t),
    void *first_arg_to_callback);

size_t marisa_get_num_tries(const marisa_trie *h);
size_t marisa_get_num_keys(const marisa_trie *h);
size_t marisa_get_num_nodes(const marisa_trie *h);
size_t marisa_get_total_size(const marisa_trie *h);

marisa_status marisa_clear(marisa_trie *h);

#ifdef __cplusplus
}  // extern "C"
#endif  // __cplusplus

#endif  // MARISA_TRIE_H_
