#ifndef MARISA_BITVECTOR_H_
#define MARISA_BITVECTOR_H_

#include "rank.h"
#include "vector.h"

namespace marisa {

class BitVector {
 public:
  BitVector();

  void build();

  void clear_select0s() {
    select0s_.clear();
  }
  void clear_select1s() {
    select1s_.clear();
  }

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

  void push_back(bool bit) {
    MARISA_THROW_IF(size_ == max_size(), MARISA_SIZE_ERROR);
    if ((size_ % 32) == 0) {
      blocks_.push_back(0);
    }
    if (bit) {
      blocks_.back() |= 1U << (size_ % 32);
    }
    ++size_;
  }

  bool operator[](std::size_t i) const {
    MARISA_DEBUG_IF(i >= size_, MARISA_PARAM_ERROR);
    return (blocks_[i / 32] & (1U << (i % 32))) != 0;
  }

  UInt32 rank0(UInt32 i) const {
    MARISA_DEBUG_IF(i > size_, MARISA_PARAM_ERROR);
    return i - rank1(i);
  }
  UInt32 rank1(UInt32 i) const;

  UInt32 select0(UInt32 i) const;
  UInt32 select1(UInt32 i) const;

  std::size_t size() const {
    return size_;
  }
  bool empty() const {
    return blocks_.empty();
  }
  std::size_t max_size() const {
    return MARISA_UINT32_MAX;
  }
  std::size_t total_size() const {
    return blocks_.total_size() + sizeof(size_) + ranks_.total_size()
        + select0s_.total_size() + select1s_.total_size();
  }

  void clear();
  void swap(BitVector *rhs);

 private:
  Vector<UInt32> blocks_;
  UInt32 size_;
  Vector<Rank> ranks_;
  Vector<UInt32> select0s_;
  Vector<UInt32> select1s_;

  // Disallows copy and assignment.
  BitVector(const BitVector &);
  BitVector &operator=(const BitVector &);
};

}  // namespace marisa

#endif  // MARISA_BITVECTOR_H_
