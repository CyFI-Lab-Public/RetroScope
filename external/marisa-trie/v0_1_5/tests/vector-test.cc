#include <cstdlib>
#include <ctime>
#include <sstream>
#include <string>
#include <vector>

#include <marisa_alpha/vector.h>
#include <marisa_alpha/intvector.h>
#include <marisa_alpha/bitvector.h>

#include "assert.h"

namespace {

void TestVector() {
  TEST_START();

  std::vector<int> values;
  for (std::size_t i = 0; i < 1000; ++i) {
    values.push_back(std::rand());
  }

  marisa_alpha::Vector<int> vec;

  ASSERT(vec.max_size() == MARISA_ALPHA_UINT32_MAX);
  ASSERT(vec.size() == 0);
  ASSERT(vec.capacity() == 0);
  ASSERT(!vec.fixed());
  ASSERT(vec.empty());
  ASSERT(vec.total_size() == sizeof(marisa_alpha::UInt32));

  for (std::size_t i = 0; i < values.size(); ++i) {
    vec.push_back(values[i]);
    ASSERT(vec[i] == values[i]);
    ASSERT(static_cast<const marisa_alpha::Vector<int> &>(vec)[i] ==
        values[i]);
  }

  ASSERT(vec.size() == values.size());
  ASSERT(vec.capacity() >= vec.size());
  ASSERT(!vec.empty());
  ASSERT(vec.total_size() == sizeof(marisa_alpha::UInt32)
      + ((sizeof(int) * values.size())));

  ASSERT(static_cast<const marisa_alpha::Vector<int> &>(vec).front()
      == values.front());
  ASSERT(static_cast<const marisa_alpha::Vector<int> &>(vec).back()
      == values.back());
  ASSERT(vec.front() == values.front());
  ASSERT(vec.back() == values.back());

  vec.shrink();

  ASSERT(vec.size() == values.size());
  ASSERT(vec.capacity() == vec.size());
  for (std::size_t i = 0; i < values.size(); ++i) {
    ASSERT(vec[i] == values[i]);
    ASSERT(static_cast<const marisa_alpha::Vector<int> &>(vec)[i] ==
        values[i]);
  }

  vec.save("vector-test.dat");
  vec.clear();

  ASSERT(vec.empty());
  ASSERT(vec.capacity() == 0);

  marisa_alpha::Mapper mapper;
  vec.mmap(&mapper, "vector-test.dat");

  ASSERT(mapper.is_open());
  ASSERT(vec.size() == values.size());
  ASSERT(vec.capacity() == 0);
  ASSERT(vec.fixed());
  ASSERT(!vec.empty());
  ASSERT(vec.total_size() == sizeof(marisa_alpha::UInt32)
      + ((sizeof(int) * values.size())));

  for (std::size_t i = 0; i < values.size(); ++i) {
    ASSERT(static_cast<const marisa_alpha::Vector<int> &>(vec)[i] ==
        values[i]);
  }

  vec.clear();
  vec.load("vector-test.dat");

  ASSERT(vec.size() == values.size());
  ASSERT(vec.capacity() == vec.size());
  ASSERT(!vec.fixed());
  ASSERT(!vec.empty());
  ASSERT(vec.total_size() == sizeof(marisa_alpha::UInt32)
      + ((sizeof(int) * values.size())));

  for (std::size_t i = 0; i < values.size(); ++i) {
    ASSERT(vec[i] == values[i]);
    ASSERT(static_cast<const marisa_alpha::Vector<int> &>(vec)[i] ==
        values[i]);
  }

  vec.clear();

  vec.push_back(0);
  ASSERT(vec.capacity() == 1);
  vec.push_back(1);
  ASSERT(vec.capacity() == 2);
  vec.push_back(2);
  ASSERT(vec.capacity() == 4);
  vec.resize(5);
  ASSERT(vec.capacity() == 8);
  vec.resize(100);
  ASSERT(vec.capacity() == 100);

  vec.fix();
  ASSERT(vec.fixed());
  EXCEPT(vec.fix(), MARISA_ALPHA_STATE_ERROR);
  EXCEPT(vec.push_back(0), MARISA_ALPHA_STATE_ERROR);
  EXCEPT(vec.resize(0), MARISA_ALPHA_STATE_ERROR);
  EXCEPT(vec.reserve(0), MARISA_ALPHA_STATE_ERROR);

  TEST_END();
}

void TestIntVector() {
  TEST_START();

  marisa_alpha::IntVector vec;

  ASSERT(vec.num_bits_per_int() == 0);
  ASSERT(vec.mask() == 0);
  ASSERT(vec.size() == 0);
  ASSERT(vec.empty());
  ASSERT(vec.total_size() == sizeof(marisa_alpha::UInt32) * 4);

  marisa_alpha::Vector<marisa_alpha::UInt32> values;
  vec.build(values);

  ASSERT(vec.num_bits_per_int() == 1);
  ASSERT(vec.mask() == 1);
  ASSERT(vec.size() == 0);
  ASSERT(vec.empty());
  ASSERT(vec.total_size() == sizeof(marisa_alpha::UInt32) * 4);

  values.push_back(0);
  vec.build(values);

  ASSERT(vec.num_bits_per_int() == 1);
  ASSERT(vec.mask() == 1);
  ASSERT(vec.size() == 1);
  ASSERT(!vec.empty());
  ASSERT(vec.total_size() == sizeof(marisa_alpha::UInt32) * 5);
  ASSERT(vec[0] == 0);

  values.push_back(255);
  vec.build(values);

  ASSERT(vec.num_bits_per_int() == 8);
  ASSERT(vec.mask() == 0xFF);
  ASSERT(vec.size() == 2);
  ASSERT(vec[0] == 0);
  ASSERT(vec[1] == 255);

  values.push_back(65536);
  vec.build(values);

  ASSERT(vec.num_bits_per_int() == 17);
  ASSERT(vec.mask() == 0x1FFFF);
  ASSERT(vec.size() == 3);
  ASSERT(vec[0] == 0);
  ASSERT(vec[1] == 255);
  ASSERT(vec[2] == 65536);

  vec.save("vector-test.dat");
  vec.clear();

  ASSERT(vec.num_bits_per_int() == 0);
  ASSERT(vec.mask() == 0);
  ASSERT(vec.size() == 0);

  marisa_alpha::Mapper mapper;
  vec.mmap(&mapper, "vector-test.dat");

  ASSERT(mapper.is_open());
  ASSERT(vec.num_bits_per_int() == 17);
  ASSERT(vec.mask() == 0x1FFFF);
  ASSERT(vec.size() == 3);
  ASSERT(vec[0] == 0);
  ASSERT(vec[1] == 255);
  ASSERT(vec[2] == 65536);

  vec.clear();
  vec.load("vector-test.dat");

  ASSERT(vec.num_bits_per_int() == 17);
  ASSERT(vec.mask() == 0x1FFFF);
  ASSERT(vec.size() == 3);
  ASSERT(vec[0] == 0);
  ASSERT(vec[1] == 255);
  ASSERT(vec[2] == 65536);

  values.clear();
  for (std::size_t i = 0; i < 500; ++i) {
    values.push_back(std::rand());
  }
  vec.build(values);

  ASSERT(vec.size() == values.size());
  for (std::size_t i = 0; i < vec.size(); ++i) {
    ASSERT(vec[i] == values[i]);
  }

  TEST_END();
}

void TestBitVector(marisa_alpha::UInt32 size) {
  marisa_alpha::BitVector bv;

  ASSERT(bv.size() == 0);
  ASSERT(bv.empty());
  ASSERT(bv.total_size() == sizeof(marisa_alpha::UInt32) * 5);

  std::vector<bool> bits(size);
  std::vector<marisa_alpha::UInt32> zeros, ones;
  for (marisa_alpha::UInt32 i = 0; i < size; ++i) {
    const bool bit = (std::rand() % 2) == 0;
    bits[i] = bit;
    bv.push_back(bit);
    (bit ? ones : zeros).push_back(i);
    ASSERT(bv[i] == bits[i]);
  }

  ASSERT(bv.size() == bits.size());
  ASSERT((size == 0) || !bv.empty());

  bv.build();

  marisa_alpha::UInt32 num_zeros = 0, num_ones = 0;
  for (marisa_alpha::UInt32 i = 0; i < bits.size(); ++i) {
    ASSERT(bv[i] == bits[i]);
    ASSERT(bv.rank0(i) == num_zeros);
    ASSERT(bv.rank1(i) == num_ones);
    ++(bv[i] ? num_ones : num_zeros);
  }
  for (marisa_alpha::UInt32 i = 0; i < zeros.size(); ++i) {
    ASSERT(bv.select0(i) == zeros[i]);
  }
  for (marisa_alpha::UInt32 i = 0; i < ones.size(); ++i) {
    ASSERT(bv.select1(i) == ones[i]);
  }

  std::stringstream stream;
  bv.write(stream);
  bv.clear();

  ASSERT(bv.size() == 0);
  ASSERT(bv.empty());
  ASSERT(bv.total_size() == sizeof(marisa_alpha::UInt32) * 5);

  bv.read(stream);

  ASSERT(bv.size() == bits.size());

  num_zeros = 0, num_ones = 0;
  for (marisa_alpha::UInt32 i = 0; i < bits.size(); ++i) {
    ASSERT(bv[i] == bits[i]);
    ASSERT(bv.rank0(i) == num_zeros);
    ASSERT(bv.rank1(i) == num_ones);
    ++(bv[i] ? num_ones : num_zeros);
  }
  for (marisa_alpha::UInt32 i = 0; i < zeros.size(); ++i) {
    ASSERT(bv.select0(i) == zeros[i]);
  }
  for (marisa_alpha::UInt32 i = 0; i < ones.size(); ++i) {
    ASSERT(bv.select1(i) == ones[i]);
  }
}

void TestBitVector() {
  TEST_START();

  TestBitVector(0);
  TestBitVector(1);
  TestBitVector(511);
  TestBitVector(512);
  TestBitVector(513);

  for (marisa_alpha::UInt32 i = 0; i < 100; ++i) {
    TestBitVector(std::rand() % 4096);
  }

  TEST_END();
}

}  // namespace

int main() {
  std::srand((unsigned int)time(NULL));

  TestVector();
  TestIntVector();
  TestBitVector();

  return 0;
}
