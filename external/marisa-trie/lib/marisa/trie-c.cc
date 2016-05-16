#include "trie.h"

extern "C" {

namespace {

class FindCallback {
 public:
  typedef int (*Func)(void *, marisa_uint32, size_t);

  FindCallback(Func func, void *first_arg)
      : func_(func), first_arg_(first_arg) {}
  FindCallback(const FindCallback &callback)
      : func_(callback.func_), first_arg_(callback.first_arg_) {}

  bool operator()(marisa::UInt32 key_id, std::size_t key_length) const {
    return func_(first_arg_, key_id, key_length) != 0;
  }

 private:
  Func func_;
  void *first_arg_;

  // Disallows assignment.
  FindCallback &operator=(const FindCallback &);
};

class PredictCallback {
 public:
  typedef int (*Func)(void *, marisa_uint32, const char *, size_t);

  PredictCallback(Func func, void *first_arg)
      : func_(func), first_arg_(first_arg) {}
  PredictCallback(const PredictCallback &callback)
      : func_(callback.func_), first_arg_(callback.first_arg_) {}

  bool operator()(marisa::UInt32 key_id, const std::string &key) const {
    return func_(first_arg_, key_id, key.c_str(), key.length()) != 0;
  }

 private:
  Func func_;
  void *first_arg_;

  // Disallows assignment.
  PredictCallback &operator=(const PredictCallback &);
};

}  // namespace

struct marisa_trie_ {
 public:
  marisa_trie_() : trie(), mapper() {}

  marisa::Trie trie;
  marisa::Mapper mapper;

 private:
  // Disallows copy and assignment.
  marisa_trie_(const marisa_trie_ &);
  marisa_trie_ &operator=(const marisa_trie_ &);
};

marisa_status marisa_init(marisa_trie **h) {
  if ((h == NULL) || (*h != NULL)) {
    return MARISA_HANDLE_ERROR;
  }
  *h = new (std::nothrow) marisa_trie_();
  return (*h != NULL) ? MARISA_OK : MARISA_MEMORY_ERROR;
}

marisa_status marisa_end(marisa_trie *h) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  }
  delete h;
  return MARISA_OK;
}

marisa_status marisa_build(marisa_trie *h, const char * const *keys,
    size_t num_keys, const size_t *key_lengths, const double *key_weights,
    marisa_uint32 *key_ids, int flags) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  }
  h->trie.build(keys, num_keys, key_lengths, key_weights, key_ids, flags);
  h->mapper.clear();
  return MARISA_OK;
}

marisa_status marisa_mmap(marisa_trie *h, const char *filename,
    long offset, int whence) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  }
  h->trie.mmap(&h->mapper, filename, offset, whence);
  return MARISA_OK;
}

marisa_status marisa_map(marisa_trie *h, const void *ptr, size_t size) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  }
  h->trie.map(ptr, size);
  h->mapper.clear();
  return MARISA_OK;
}

marisa_status marisa_load(marisa_trie *h, const char *filename,
    long offset, int whence) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  }
  h->trie.load(filename, offset, whence);
  h->mapper.clear();
  return MARISA_OK;
}

marisa_status marisa_fread(marisa_trie *h, FILE *file) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  }
  h->trie.fread(file);
  h->mapper.clear();
  return MARISA_OK;
}

marisa_status marisa_read(marisa_trie *h, int fd) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  }
  h->trie.read(fd);
  h->mapper.clear();
  return MARISA_OK;
}

marisa_status marisa_save(const marisa_trie *h, const char *filename,
    int trunc_flag, long offset, int whence) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  }
  h->trie.save(filename, trunc_flag != 0, offset, whence);
  return MARISA_OK;
}

marisa_status marisa_fwrite(const marisa_trie *h, FILE *file) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  }
  h->trie.fwrite(file);
  return MARISA_OK;
}

marisa_status marisa_write(const marisa_trie *h, int fd) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  }
  h->trie.write(fd);
  return MARISA_OK;
}

marisa_status marisa_restore(const marisa_trie *h, marisa_uint32 key_id,
    char *key_buf, size_t key_buf_size, size_t *key_length) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  } else if (key_length == NULL) {
    return MARISA_PARAM_ERROR;
  }
  *key_length = h->trie.restore(key_id, key_buf, key_buf_size);
  return MARISA_OK;
}

marisa_status marisa_lookup(const marisa_trie *h,
    const char *ptr, size_t length, marisa_uint32 *key_id) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  } else if (key_id == NULL) {
    return MARISA_PARAM_ERROR;
  }
  if (length == MARISA_ZERO_TERMINATED) {
    *key_id = h->trie.lookup(ptr);
  } else {
    *key_id = h->trie.lookup(ptr, length);
  }
  return MARISA_OK;
}

marisa_status marisa_find(const marisa_trie *h,
    const char *ptr, size_t length,
    marisa_uint32 *key_ids, size_t *key_lengths,
    size_t max_num_results, size_t *num_results) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  } else if (num_results == NULL) {
    return MARISA_PARAM_ERROR;
  }
  if (length == MARISA_ZERO_TERMINATED) {
    *num_results = h->trie.find(ptr, key_ids, key_lengths, max_num_results);
  } else {
    *num_results = h->trie.find(ptr, length,
        key_ids, key_lengths, max_num_results);
  }
  return MARISA_OK;
}

marisa_status marisa_find_first(const marisa_trie *h,
    const char *ptr, size_t length,
    marisa_uint32 *key_id, size_t *key_length) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  } else if (key_id == NULL) {
    return MARISA_PARAM_ERROR;
  }
  if (length == MARISA_ZERO_TERMINATED) {
    *key_id = h->trie.find_first(ptr, key_length);
  } else {
    *key_id = h->trie.find_first(ptr, length, key_length);
  }
  return MARISA_OK;
}

marisa_status marisa_find_last(const marisa_trie *h,
    const char *ptr, size_t length,
    marisa_uint32 *key_id, size_t *key_length) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  } else if (key_id == NULL) {
    return MARISA_PARAM_ERROR;
  }
  if (length == MARISA_ZERO_TERMINATED) {
    *key_id = h->trie.find_last(ptr, key_length);
  } else {
    *key_id = h->trie.find_last(ptr, length, key_length);
  }
  return MARISA_OK;
}

marisa_status marisa_find_callback(const marisa_trie *h,
    const char *ptr, size_t length,
    int (*callback)(void *, marisa_uint32, size_t),
    void *first_arg_to_callback) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  } else if (callback == NULL) {
    return MARISA_PARAM_ERROR;
  }
  if (length == MARISA_ZERO_TERMINATED) {
    h->trie.find_callback(ptr,
        ::FindCallback(callback, first_arg_to_callback));
  } else {
    h->trie.find_callback(ptr, length,
        ::FindCallback(callback, first_arg_to_callback));
  }
  return MARISA_OK;
}

marisa_status marisa_predict(const marisa_trie *h,
    const char *ptr, size_t length, marisa_uint32 *key_ids,
    size_t max_num_results, size_t *num_results) {
  return marisa_predict_breadth_first(h, ptr, length,
      key_ids, max_num_results, num_results);
}

marisa_status marisa_predict_breadth_first(const marisa_trie *h,
    const char *ptr, size_t length, marisa_uint32 *key_ids,
    size_t max_num_results, size_t *num_results) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  } else if (num_results == NULL) {
    return MARISA_PARAM_ERROR;
  }
  if (length == MARISA_ZERO_TERMINATED) {
    *num_results = h->trie.predict_breadth_first(
        ptr, key_ids, NULL, max_num_results);
  } else {
    *num_results = h->trie.predict_breadth_first(
        ptr, length, key_ids, NULL, max_num_results);
  }
  return MARISA_OK;
}

marisa_status marisa_predict_depth_first(const marisa_trie *h,
    const char *ptr, size_t length, marisa_uint32 *key_ids,
    size_t max_num_results, size_t *num_results) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  } else if (num_results == NULL) {
    return MARISA_PARAM_ERROR;
  }
  if (length == MARISA_ZERO_TERMINATED) {
    *num_results = h->trie.predict_depth_first(
        ptr, key_ids, NULL, max_num_results);
  } else {
    *num_results = h->trie.predict_depth_first(
        ptr, length, key_ids, NULL, max_num_results);
  }
  return MARISA_OK;
}

marisa_status marisa_predict_callback(const marisa_trie *h,
    const char *ptr, size_t length,
    int (*callback)(void *, marisa_uint32, const char *, size_t),
    void *first_arg_to_callback) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  } else if (callback == NULL) {
    return MARISA_PARAM_ERROR;
  }
  if (length == MARISA_ZERO_TERMINATED) {
    h->trie.predict_callback(ptr,
        ::PredictCallback(callback, first_arg_to_callback));
  } else {
    h->trie.predict_callback(ptr, length,
        ::PredictCallback(callback, first_arg_to_callback));
  }
  return MARISA_OK;
}

size_t marisa_get_num_tries(const marisa_trie *h) {
  return (h != NULL) ? h->trie.num_tries() : 0;
}

size_t marisa_get_num_keys(const marisa_trie *h) {
  return (h != NULL) ? h->trie.num_keys() : 0;
}

size_t marisa_get_num_nodes(const marisa_trie *h) {
  return (h != NULL) ? h->trie.num_nodes() : 0;
}

size_t marisa_get_total_size(const marisa_trie *h) {
  return (h != NULL) ? h->trie.total_size() : 0;
}

marisa_status marisa_clear(marisa_trie *h) {
  if (h == NULL) {
    return MARISA_HANDLE_ERROR;
  }
  h->trie.clear();
  h->mapper.clear();
  return MARISA_OK;
}

}  // extern "C"
