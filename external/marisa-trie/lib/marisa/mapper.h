#ifndef MARISA_MAPPER_H_
#define MARISA_MAPPER_H_

#include <cstdio>

#include "base.h"

namespace marisa {

class Mapper {
 public:
  Mapper();
  Mapper(const void *ptr, std::size_t size);
  ~Mapper();

  void open(const char *filename, long offset = 0, int whence = SEEK_SET);

  template <typename T>
  void map(T *obj) {
    MARISA_THROW_IF(obj == NULL, MARISA_PARAM_ERROR);
    *obj = *static_cast<const T *>(map_data(sizeof(T)));
  }

  template <typename T>
  void map(const T **objs, std::size_t num_objs) {
    MARISA_THROW_IF((objs == NULL) && (num_objs != 0), MARISA_PARAM_ERROR);
    MARISA_THROW_IF(num_objs > (MARISA_UINT32_MAX / sizeof(T)),
        MARISA_SIZE_ERROR);
    *objs = static_cast<const T *>(map_data(sizeof(T) * num_objs));
  }

  bool is_open() const {
    return ptr_ != NULL;
  }

  void clear();
  void swap(Mapper *rhs);

 private:
  const void *ptr_;
  void *origin_;
  std::size_t avail_;
  std::size_t size_;
#if defined _WIN32 || defined _WIN64
  void *file_;
  void *map_;
#else  // defined _WIN32 || defined _WIN64
  int fd_;
#endif  // defined _WIN32 || defined _WIN64

  void seek(long offset, int whence);

  const void *map_data(std::size_t size);

  // Disallows copy and assignment.
  Mapper(const Mapper &);
  Mapper &operator=(const Mapper &);
};

}  // namespace marisa

#endif  // MARISA_MAPPER_H_
