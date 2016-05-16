#ifndef MARISA_ALPHA_INTVECTOR_H_
#define MARISA_ALPHA_INTVECTOR_H_

#include "vector.h"

namespace marisa_alpha {

class IntVector {
 public:
  IntVector();

  void build(const Vector<UInt32> &ints);
  void build(UInt32 max_int, std::size_t size);

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

  void set(std::size_t i, UInt32 value) {
    MARISA_ALPHA_DEBUG_IF(i >= size_, MARISA_ALPHA_PARAM_ERROR);
    std::size_t pos = i * num_bits_per_int_;
    std::size_t unit_id = pos / 32;
    std::size_t unit_offset = pos % 32;
    units_[unit_id] &= ~(mask_ << unit_offset);
    units_[unit_id] |= (value & mask_) << unit_offset;
    if ((unit_offset + num_bits_per_int_) > 32) {
      units_[unit_id + 1] &= ~(mask_ >> (32 - unit_offset));
      units_[unit_id + 1] |= (value & mask_) >> (32 - unit_offset);
    }
  }

  UInt32 get(std::size_t i) const {
    MARISA_ALPHA_DEBUG_IF(i >= size_, MARISA_ALPHA_PARAM_ERROR);
    std::size_t pos = i * num_bits_per_int_;
    std::size_t unit_id = pos / 32;
    std::size_t unit_offset = pos % 32;
    if ((unit_offset + num_bits_per_int_) <= 32) {
      return (units_[unit_id] >> unit_offset) & mask_;
    } else {
      return ((units_[unit_id] >> unit_offset)
          | (units_[unit_id + 1] << (32 - unit_offset))) & mask_;
    }
  }

  UInt32 operator[](std::size_t i) const {
    MARISA_ALPHA_DEBUG_IF(i >= size_, MARISA_ALPHA_PARAM_ERROR);
    return get(i);
  }

  std::size_t num_bits_per_int() const {
    return num_bits_per_int_;
  }
  UInt32 mask() const {
    return mask_;
  }
  std::size_t size() const {
    return size_;
  }
  bool empty() const {
    return size_ == 0;
  }
  std::size_t total_size() const {
    return units_.total_size() + sizeof(num_bits_per_int_)
        + sizeof(mask_) + sizeof(size_);
  }

  void clear();
  void swap(IntVector *rhs);

 private:
  Vector<UInt32> units_;
  UInt32 num_bits_per_int_;
  UInt32 mask_;
  UInt32 size_;

  // Disallows copy and assignment.
  IntVector(const IntVector &);
  IntVector &operator=(const IntVector &);
};

}  // namespace marisa_alpha

#endif  // MARISA_ALPHA_INTVECTOR_H_
