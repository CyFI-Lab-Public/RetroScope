#include "intvector.h"

namespace marisa_alpha {

IntVector::IntVector()
    : units_(), num_bits_per_int_(0), mask_(0), size_(0) {}

void IntVector::build(const Vector<UInt32> &ints) {
  UInt32 max_int = 0;
  for (UInt32 i = 0; i < ints.size(); ++i) {
    if (ints[i] > max_int) {
      max_int = ints[i];
    }
  }
  build(max_int, ints.size());
  for (UInt32 i = 0; i < ints.size(); ++i) {
    set(i, ints[i]);
  }
}

void IntVector::build(UInt32 max_int, std::size_t size) {
  UInt32 num_bits_per_int = 0;
  do {
    ++num_bits_per_int;
    max_int >>= 1;
  } while (max_int != 0);

  const std::size_t new_size = (std::size_t)(
      (((UInt64)num_bits_per_int * size) + 31) / 32);

  Vector<UInt32> temp_units;
  temp_units.resize(new_size, 0);
  units_.swap(&temp_units);

  num_bits_per_int_ = num_bits_per_int;
  mask_ = ~0U;
  if (num_bits_per_int != 32) {
    mask_ = (1U << num_bits_per_int) - 1;
  }
  size_ = (UInt32)size;
}

void IntVector::mmap(Mapper *mapper, const char *filename,
    long offset, int whence) {
  MARISA_ALPHA_THROW_IF(mapper == NULL, MARISA_ALPHA_PARAM_ERROR);
  Mapper temp_mapper;
  temp_mapper.open(filename, offset, whence);
  map(temp_mapper);
  temp_mapper.swap(mapper);
}

void IntVector::map(const void *ptr, std::size_t size) {
  Mapper mapper(ptr, size);
  map(mapper);
}

void IntVector::map(Mapper &mapper) {
  IntVector temp;
  temp.units_.map(mapper);
  mapper.map(&temp.num_bits_per_int_);
  mapper.map(&temp.mask_);
  mapper.map(&temp.size_);
  temp.swap(this);
}

void IntVector::load(const char *filename,
    long offset, int whence) {
  Reader reader;
  reader.open(filename, offset, whence);
  read(reader);
}

void IntVector::fread(std::FILE *file) {
  Reader reader(file);
  read(reader);
}

void IntVector::read(int fd) {
  Reader reader(fd);
  read(reader);
}

void IntVector::read(std::istream &stream) {
  Reader reader(&stream);
  read(reader);
}

void IntVector::read(Reader &reader) {
  IntVector temp;
  temp.units_.read(reader);
  reader.read(&temp.num_bits_per_int_);
  reader.read(&temp.mask_);
  reader.read(&temp.size_);
  temp.swap(this);
}

void IntVector::save(const char *filename, bool trunc_flag,
    long offset, int whence) const {
  Writer writer;
  writer.open(filename, trunc_flag, offset, whence);
  write(writer);
}

void IntVector::fwrite(std::FILE *file) const {
  Writer writer(file);
  write(writer);
}

void IntVector::write(int fd) const {
  Writer writer(fd);
  write(writer);
}

void IntVector::write(std::ostream &stream) const {
  Writer writer(&stream);
  write(writer);
}

void IntVector::write(Writer &writer) const {
  units_.write(writer);
  writer.write(num_bits_per_int_);
  writer.write(mask_);
  writer.write(size_);
}

void IntVector::clear() {
  IntVector().swap(this);
}

void IntVector::swap(IntVector *rhs) {
  MARISA_ALPHA_THROW_IF(rhs == NULL, MARISA_ALPHA_PARAM_ERROR);
  units_.swap(&rhs->units_);
  Swap(&num_bits_per_int_, &rhs->num_bits_per_int_);
  Swap(&mask_, &rhs->mask_);
  Swap(&size_, &rhs->size_);
}

}  // namespace marisa_alpha
