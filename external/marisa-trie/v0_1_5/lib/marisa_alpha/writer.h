#ifndef MARISA_ALPHA_WRITER_H_
#define MARISA_ALPHA_WRITER_H_

#include <cstdio>
#include <iostream>

#include "base.h"

namespace marisa_alpha {

class Writer {
 public:
  Writer();
  explicit Writer(std::FILE *file);
  explicit Writer(int fd);
  explicit Writer(std::ostream *stream);
  ~Writer();

  void open(const char *filename, bool trunc_flag = true,
      long offset = 0, int whence = SEEK_SET);

  template <typename T>
  void write(const T &obj) {
    write_data(&obj, sizeof(T));
  }

  template <typename T>
  void write(const T *objs, std::size_t num_objs) {
    MARISA_ALPHA_THROW_IF((objs == NULL) && (num_objs != 0),
        MARISA_ALPHA_PARAM_ERROR);
    MARISA_ALPHA_THROW_IF(num_objs > (MARISA_ALPHA_UINT32_MAX / sizeof(T)),
        MARISA_ALPHA_SIZE_ERROR);
    if (num_objs != 0) {
      write_data(objs, sizeof(T) * num_objs);
    }
  }

  bool is_open() const {
    return (file_ != NULL) || (fd_ != -1) || (stream_ != NULL);
  }

  void clear();
  void swap(Writer *rhs);

 private:
  std::FILE *file_;
  int fd_;
  std::ostream *stream_;
  bool needs_fclose_;

  void write_data(const void *data, std::size_t size);

  // Disallows copy and assignment.
  Writer(const Writer &);
  Writer &operator=(const Writer &);
};

}  // namespace marisa_alpha

#endif  // MARISA_ALPHA_WRITER_H_
