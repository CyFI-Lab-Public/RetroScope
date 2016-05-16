#include "trie.h"

extern "C" {

namespace {

class FindCallback {
 public:
  typedef int (*Func)(void *, marisa_alpha_uint32, size_t);

  FindCallback(Func func, void *first_arg)
      : func_(func), first_arg_(first_arg) {}
  FindCallback(const FindCallback &callback)
      : func_(callback.func_), first_arg_(callback.first_arg_) {}

  bool operator()(marisa_alpha::UInt32 key_id, std::size_t key_length) const {
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
  typedef int (*Func)(void *, marisa_alpha_uint32, const char *, size_t);

  PredictCallback(Func func, void *first_arg)
      : func_(func), first_arg_(first_arg) {}
  PredictCallback(const PredictCallback &callback)
      : func_(callback.func_), first_arg_(callback.first_arg_) {}

  bool operator()(marisa_alpha::UInt32 key_id, const std::string &key) const {
    return func_(first_arg_, key_id, key.c_str(), key.length()) != 0;
  }

 private:
  Func func_;
  void *first_arg_;

  // Disallows assignment.
  PredictCallback &operator=(const PredictCallback &);
};

}  // namespace

struct marisa_alpha_trie_ {
 public:
  marisa_alpha_trie_() : trie(), mapper() {}

  marisa_alpha::Trie trie;
  marisa_alpha::Mapper mapper;

 private:
  // Disallows copy and assignment.
  marisa_alpha_trie_(const marisa_alpha_trie_ &);
  marisa_alpha_trie_ &operator=(const marisa_alpha_trie_ &);
};

marisa_alpha_status marisa_alpha_init(marisa_alpha_trie **h) {
  if ((h == NULL) || (*h != NULL)) {
    return MARISA_ALPHA_HANDLE_ERROR;
  }
  *h = new (std::nothrow) marisa_alpha_trie_();
  return (*h != NULL) ? MARISA_ALPHA_OK : MARISA_ALPHA_MEMORY_ERROR;
}

marisa_alpha_status marisa_alpha_end(marisa_alpha_trie *h) {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  }
  delete h;
  return MARISA_ALPHA_OK;
}

marisa_alpha_status marisa_alpha_build(marisa_alpha_trie *h,
    const char * const *keys, size_t num_keys, const size_t *key_lengths,
    const double *key_weights, marisa_alpha_uint32 *key_ids, int flags) try {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  }
  h->trie.build(keys, num_keys, key_lengths, key_weights, key_ids, flags);
  h->mapper.clear();
  return MARISA_ALPHA_OK;
} catch (const marisa_alpha::Exception &ex) {
  return ex.status();
}

marisa_alpha_status marisa_alpha_mmap(marisa_alpha_trie *h,
    const char *filename, long offset, int whence) try {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  }
  h->trie.mmap(&h->mapper, filename, offset, whence);
  return MARISA_ALPHA_OK;
} catch (const marisa_alpha::Exception &ex) {
  return ex.status();
}

marisa_alpha_status marisa_alpha_map(marisa_alpha_trie *h, const void *ptr,
    size_t size) try {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  }
  h->trie.map(ptr, size);
  h->mapper.clear();
  return MARISA_ALPHA_OK;
} catch (const marisa_alpha::Exception &ex) {
  return ex.status();
}

marisa_alpha_status marisa_alpha_load(marisa_alpha_trie *h,
    const char *filename, long offset, int whence) try {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  }
  h->trie.load(filename, offset, whence);
  h->mapper.clear();
  return MARISA_ALPHA_OK;
} catch (const marisa_alpha::Exception &ex) {
  return ex.status();
}

marisa_alpha_status marisa_alpha_fread(marisa_alpha_trie *h, FILE *file) try {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  }
  h->trie.fread(file);
  h->mapper.clear();
  return MARISA_ALPHA_OK;
} catch (const marisa_alpha::Exception &ex) {
  return ex.status();
}

marisa_alpha_status marisa_alpha_read(marisa_alpha_trie *h, int fd) try {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  }
  h->trie.read(fd);
  h->mapper.clear();
  return MARISA_ALPHA_OK;
} catch (const marisa_alpha::Exception &ex) {
  return ex.status();
}

marisa_alpha_status marisa_alpha_save(const marisa_alpha_trie *h,
    const char *filename, int trunc_flag, long offset, int whence) try {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  }
  h->trie.save(filename, trunc_flag != 0, offset, whence);
  return MARISA_ALPHA_OK;
} catch (const marisa_alpha::Exception &ex) {
  return ex.status();
}

marisa_alpha_status marisa_alpha_fwrite(const marisa_alpha_trie *h,
    FILE *file) try {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  }
  h->trie.fwrite(file);
  return MARISA_ALPHA_OK;
} catch (const marisa_alpha::Exception &ex) {
  return ex.status();
}

marisa_alpha_status marisa_alpha_write(const marisa_alpha_trie *h, int fd) try {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  }
  h->trie.write(fd);
  return MARISA_ALPHA_OK;
} catch (const marisa_alpha::Exception &ex) {
  return ex.status();
}

marisa_alpha_status marisa_alpha_restore(const marisa_alpha_trie *h,
    marisa_alpha_uint32 key_id, char *key_buf, size_t key_buf_size,
    size_t *key_length) try {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  } else if (key_length == NULL) {
    return MARISA_ALPHA_PARAM_ERROR;
  }
  *key_length = h->trie.restore(key_id, key_buf, key_buf_size);
  return MARISA_ALPHA_OK;
} catch (const marisa_alpha::Exception &ex) {
  return ex.status();
}

marisa_alpha_status marisa_alpha_lookup(const marisa_alpha_trie *h,
    const char *ptr, size_t length, marisa_alpha_uint32 *key_id) try {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  } else if (key_id == NULL) {
    return MARISA_ALPHA_PARAM_ERROR;
  }
  if (length == MARISA_ALPHA_ZERO_TERMINATED) {
    *key_id = h->trie.lookup(ptr);
  } else {
    *key_id = h->trie.lookup(ptr, length);
  }
  return MARISA_ALPHA_OK;
} catch (const marisa_alpha::Exception &ex) {
  return ex.status();
}

marisa_alpha_status marisa_alpha_find(const marisa_alpha_trie *h,
    const char *ptr, size_t length,
    marisa_alpha_uint32 *key_ids, size_t *key_lengths,
    size_t max_num_results, size_t *num_results) try {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  } else if (num_results == NULL) {
    return MARISA_ALPHA_PARAM_ERROR;
  }
  if (length == MARISA_ALPHA_ZERO_TERMINATED) {
    *num_results = h->trie.find(ptr, key_ids, key_lengths, max_num_results);
  } else {
    *num_results = h->trie.find(ptr, length,
        key_ids, key_lengths, max_num_results);
  }
  return MARISA_ALPHA_OK;
} catch (const marisa_alpha::Exception &ex) {
  return ex.status();
}

marisa_alpha_status marisa_alpha_find_first(const marisa_alpha_trie *h,
    const char *ptr, size_t length,
    marisa_alpha_uint32 *key_id, size_t *key_length) {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  } else if (key_id == NULL) {
    return MARISA_ALPHA_PARAM_ERROR;
  }
  if (length == MARISA_ALPHA_ZERO_TERMINATED) {
    *key_id = h->trie.find_first(ptr, key_length);
  } else {
    *key_id = h->trie.find_first(ptr, length, key_length);
  }
  return MARISA_ALPHA_OK;
}

marisa_alpha_status marisa_alpha_find_last(const marisa_alpha_trie *h,
    const char *ptr, size_t length,
    marisa_alpha_uint32 *key_id, size_t *key_length) {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  } else if (key_id == NULL) {
    return MARISA_ALPHA_PARAM_ERROR;
  }
  if (length == MARISA_ALPHA_ZERO_TERMINATED) {
    *key_id = h->trie.find_last(ptr, key_length);
  } else {
    *key_id = h->trie.find_last(ptr, length, key_length);
  }
  return MARISA_ALPHA_OK;
}

marisa_alpha_status marisa_alpha_find_callback(const marisa_alpha_trie *h,
    const char *ptr, size_t length,
    int (*callback)(void *, marisa_alpha_uint32, size_t),
    void *first_arg_to_callback) try {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  } else if (callback == NULL) {
    return MARISA_ALPHA_PARAM_ERROR;
  }
  if (length == MARISA_ALPHA_ZERO_TERMINATED) {
    h->trie.find_callback(ptr,
        ::FindCallback(callback, first_arg_to_callback));
  } else {
    h->trie.find_callback(ptr, length,
        ::FindCallback(callback, first_arg_to_callback));
  }
  return MARISA_ALPHA_OK;
} catch (const marisa_alpha::Exception &ex) {
  return ex.status();
}

marisa_alpha_status marisa_alpha_predict(const marisa_alpha_trie *h,
    const char *ptr, size_t length, marisa_alpha_uint32 *key_ids,
    size_t max_num_results, size_t *num_results) {
  return marisa_alpha_predict_breadth_first(h, ptr, length,
      key_ids, max_num_results, num_results);
}

marisa_alpha_status marisa_alpha_predict_breadth_first(
    const marisa_alpha_trie *h, const char *ptr, size_t length,
    marisa_alpha_uint32 *key_ids, size_t max_num_results,
    size_t *num_results) try {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  } else if (num_results == NULL) {
    return MARISA_ALPHA_PARAM_ERROR;
  }
  if (length == MARISA_ALPHA_ZERO_TERMINATED) {
    *num_results = h->trie.predict_breadth_first(
        ptr, key_ids, NULL, max_num_results);
  } else {
    *num_results = h->trie.predict_breadth_first(
        ptr, length, key_ids, NULL, max_num_results);
  }
  return MARISA_ALPHA_OK;
} catch (const marisa_alpha::Exception &ex) {
  return ex.status();
}

marisa_alpha_status marisa_alpha_predict_depth_first(
    const marisa_alpha_trie *h, const char *ptr, size_t length,
    marisa_alpha_uint32 *key_ids, size_t max_num_results,
    size_t *num_results) try {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  } else if (num_results == NULL) {
    return MARISA_ALPHA_PARAM_ERROR;
  }
  if (length == MARISA_ALPHA_ZERO_TERMINATED) {
    *num_results = h->trie.predict_depth_first(
        ptr, key_ids, NULL, max_num_results);
  } else {
    *num_results = h->trie.predict_depth_first(
        ptr, length, key_ids, NULL, max_num_results);
  }
  return MARISA_ALPHA_OK;
} catch (const marisa_alpha::Exception &ex) {
  return ex.status();
}

marisa_alpha_status marisa_alpha_predict_callback(const marisa_alpha_trie *h,
    const char *ptr, size_t length,
    int (*callback)(void *, marisa_alpha_uint32, const char *, size_t),
    void *first_arg_to_callback) try {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  } else if (callback == NULL) {
    return MARISA_ALPHA_PARAM_ERROR;
  }
  if (length == MARISA_ALPHA_ZERO_TERMINATED) {
    h->trie.predict_callback(ptr,
        ::PredictCallback(callback, first_arg_to_callback));
  } else {
    h->trie.predict_callback(ptr, length,
        ::PredictCallback(callback, first_arg_to_callback));
  }
  return MARISA_ALPHA_OK;
} catch (const marisa_alpha::Exception &ex) {
  return ex.status();
}

size_t marisa_alpha_get_num_tries(const marisa_alpha_trie *h) {
  return (h != NULL) ? h->trie.num_tries() : 0;
}

size_t marisa_alpha_get_num_keys(const marisa_alpha_trie *h) {
  return (h != NULL) ? h->trie.num_keys() : 0;
}

size_t marisa_alpha_get_num_nodes(const marisa_alpha_trie *h) {
  return (h != NULL) ? h->trie.num_nodes() : 0;
}

size_t marisa_alpha_get_total_size(const marisa_alpha_trie *h) {
  return (h != NULL) ? h->trie.total_size() : 0;
}

marisa_alpha_status marisa_alpha_clear(marisa_alpha_trie *h) {
  if (h == NULL) {
    return MARISA_ALPHA_HANDLE_ERROR;
  }
  h->trie.clear();
  h->mapper.clear();
  return MARISA_ALPHA_OK;
}

}  // extern "C"
