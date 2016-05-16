#ifndef MARISA_READER_H_
#define MARISA_READER_H_

#include <cstdio>
#include <iostream>

#include "base.h"

namespace marisa {

class Reader {
 public:
  Reader();
  explicit Reader(std::FILE *file);
  explicit Reader(int fd);
  explicit Reader(std::istream *stream);
  ~Reader();

  void open(const char *filename, long offset = 0, int whence = SEEK_SET);

  template <typename T>
  void read(T *obj) {
    MARISA_THROW_IF(obj == NULL, MARISA_PARAM_ERROR);
    read_data(obj, sizeof(T));
  }

  template <typename T>
  void read(T *objs, std::size_t num_objs) {
    MARISA_THROW_IF((objs == NULL) && (num_objs != 0), MARISA_PARAM_ERROR);
    MARISA_THROW_IF(num_objs > (MARISA_UINT32_MAX / sizeof(T)),
        MARISA_SIZE_ERROR);
    if (num_objs != 0) {
      read_data(objs, sizeof(T) * num_objs);
    }
  }

  bool is_open() const {
    return (file_ != NULL) || (fd_ != -1) || (stream_ != NULL);
  }

  void clear();
  void swap(Reader *rhs);

 private:
  std::FILE *file_;
  int fd_;
  std::istream *stream_;
  bool needs_fclose_;

  void read_data(void *buf, std::size_t size);

  // Disallows copy and assignment.
  Reader(const Reader &);
  Reader &operator=(const Reader &);
};

}  // namespace marisa

#endif  // MARISA_READER_H_
