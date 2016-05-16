#ifndef MARISA_CONTAINER_H_
#define MARISA_CONTAINER_H_

#include <vector>

#include "base.h"

namespace marisa {

template <typename T>
class Container;

template <typename T>
class Container<std::vector<T> *> {
 public:
  Container(std::vector<T> *vec) : vec_(vec) {}
  Container(const Container &container) : vec_(container.vec_) {}

  void insert(std::size_t, const T &value) const {
    vec_->push_back(value);
  }

  bool is_valid() const {
    return vec_ != NULL;
  }

 private:
  std::vector<T> *vec_;

  // Disallows assignment.
  Container &operator=(const Container &query);
};

template <typename T>
class Container<T *> {
 public:
  explicit Container(T *ptr) : ptr_(ptr) {}
  Container(const Container &container) : ptr_(container.ptr_) {}

  void insert(std::size_t i, const T &value) {
    ptr_[i] = value;
  }

  bool is_valid() const {
    return ptr_ != NULL;
  }

 private:
  T *ptr_;

  // Disallows assignment.
  Container &operator=(const Container &);
};

template <typename T>
inline Container<T *> MakeContainer(T *ptr) {
  return Container<T *>(ptr);
}

}  // namespace marisa

#endif  // MARISA_CONTAINER_H_
