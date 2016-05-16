#ifndef MARISA_ALPHA_VECTOR_H_
#define MARISA_ALPHA_VECTOR_H_

#include "io.h"

namespace marisa_alpha {

template <typename T>
class Vector {
 public:
  Vector();
  ~Vector();

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

  void save(const char *filename, bool trunc_flag = false,
      long offset = 0, int whence = SEEK_SET) const;
  void fwrite(std::FILE *file) const;
  void write(int fd) const;
  void write(std::ostream &stream) const;
  void write(Writer &writer) const;

  void push_back(const T &x);
  void pop_back();

  void resize(std::size_t size);
  void resize(std::size_t size, const T &x);
  void reserve(std::size_t capacity);
  void shrink();

  void fix();

  const T *begin() const {
    return objs_;
  }
  const T *end() const {
    return objs_ + size_;
  }
  const T &operator[](std::size_t i) const {
    MARISA_ALPHA_DEBUG_IF(i > size_, MARISA_ALPHA_PARAM_ERROR);
    return objs_[i];
  }
  const T &front() const {
    MARISA_ALPHA_DEBUG_IF(size_ == 0, MARISA_ALPHA_STATE_ERROR);
    return objs_[0];
  }
  const T &back() const {
    MARISA_ALPHA_DEBUG_IF(size_ == 0, MARISA_ALPHA_STATE_ERROR);
    return objs_[size_ - 1];
  }

  T *begin() {
    MARISA_ALPHA_DEBUG_IF(fixed_, MARISA_ALPHA_STATE_ERROR);
    return buf_;
  }
  T *end() {
    MARISA_ALPHA_DEBUG_IF(fixed_, MARISA_ALPHA_STATE_ERROR);
    return buf_ + size_;
  }
  T &operator[](std::size_t i) {
    MARISA_ALPHA_DEBUG_IF(fixed_, MARISA_ALPHA_STATE_ERROR);
    MARISA_ALPHA_DEBUG_IF(i > size_, MARISA_ALPHA_PARAM_ERROR);
    return buf_[i];
  }
  T &front() {
    MARISA_ALPHA_DEBUG_IF(fixed_ || (size_ == 0), MARISA_ALPHA_STATE_ERROR);
    return buf_[0];
  }
  T &back() {
    MARISA_ALPHA_DEBUG_IF(fixed_ || (size_ == 0), MARISA_ALPHA_STATE_ERROR);
    return buf_[size_ - 1];
  }

  bool empty() const {
    return size_ == 0;
  }
  std::size_t size() const {
    return size_;
  }
  std::size_t capacity() const {
    return capacity_;
  }
  bool fixed() const {
    return fixed_;
  }
  std::size_t total_size() const {
    return (sizeof(T) * size_) + sizeof(size_);
  }

  void clear() {
    Vector().swap(this);
  }
  void swap(Vector *rhs);

  static std::size_t max_size() {
    return MARISA_ALPHA_UINT32_MAX;
  }

 private:
  T *buf_;
  const T *objs_;
  UInt32 size_;
  UInt32 capacity_;
  bool fixed_;

  void realloc(std::size_t new_capacity);

  // Disallows copy and assignment.
  Vector(const Vector &);
  Vector &operator=(const Vector &);
};

}  // namespace marisa_alpha

#include "vector-inline.h"

#endif  // MARISA_ALPHA_VECTOR_H_
