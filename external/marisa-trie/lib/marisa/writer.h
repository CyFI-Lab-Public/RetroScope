#ifndef MARISA_WRITER_H_
#define MARISA_WRITER_H_

#include <cstdio>
#include <iostream>

#include "base.h"

namespace marisa {

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
    MARISA_THROW_IF((objs == NULL) && (num_objs != 0), MARISA_PARAM_ERROR);
    MARISA_THROW_IF(num_objs > (MARISA_UINT32_MAX / sizeof(T)),
        MARISA_SIZE_ERROR);
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

}  // namespace marisa

#endif  // MARISA_WRITER_H_
