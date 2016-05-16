#ifndef MARISA_TAIL_H_
#define MARISA_TAIL_H_

#include "marisa-string.h"
#include "vector.h"

namespace marisa {

class Tail {
 public:
  Tail();

  void build(const Vector<String> &keys,
      Vector<UInt32> *offsets, int mode);

  void mmap(Mapper *mapper, const char *filename,
      long offset = 0, int whence = SEEK_SET);
  void map(const void *ptr, std::size_t size);
  void map(Mapper &mapper);

  void load(const char *filename,
      long offset = 0, int whence = SEEK_SET);
  void fread(::FILE *file);
  void read(int fd);
  void read(std::istream &stream);
  void read(Reader &reader);

  void save(const char *filename, bool trunc_flag = true,
      long offset = 0, int whence = SEEK_SET) const;
  void fwrite(::FILE *file) const;
  void write(int fd) const;
  void write(std::ostream &stream) const;
  void write(Writer &writer) const;

  const UInt8 *operator[](std::size_t offset) const {
    MARISA_DEBUG_IF(offset >= buf_.size(), MARISA_PARAM_ERROR);
    return &buf_[offset];
  }

  int mode() const {
    return (buf_.front() == '\0') ? MARISA_BINARY_TAIL : MARISA_TEXT_TAIL;
  }
  bool empty() const {
    return buf_.empty();
  }
  std::size_t size() const {
    return buf_.size();
  }
  std::size_t total_size() const {
    return buf_.total_size();
  }

  void clear();
  void swap(Tail *rhs);

 private:
  Vector<UInt8> buf_;

  void build_binary_tail(const Vector<String> &keys,
      Vector<UInt32> *offsets);
  bool build_text_tail(const Vector<String> &keys,
      Vector<UInt32> *offsets);
  void build_empty_tail(Vector<UInt32> *offsets);

  // Disallows copy and assignment.
  Tail(const Tail &);
  Tail &operator=(const Tail &);
};

}  // namespace marisa

#endif  // MARISA_TAIL_H_
