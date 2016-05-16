#include <sstream>
#include <string>

#include <marisa/tail.h>

#include "assert.h"

namespace {

void TestBinaryTail() {
  TEST_START();

  marisa::Tail tail;

  ASSERT(tail.size() == 0);
  ASSERT(tail.empty());
  ASSERT(tail.total_size() == sizeof(marisa::UInt32));

  marisa::Vector<marisa::String> keys;
  tail.build(keys, NULL, MARISA_BINARY_TAIL);

  ASSERT(tail.size() == 0);
  ASSERT(tail.empty());
  ASSERT(tail.total_size() == sizeof(marisa::UInt32));

  keys.push_back(marisa::String(""));
  marisa::Vector<marisa::UInt32> offsets;
  tail.build(keys, &offsets, MARISA_BINARY_TAIL);

  ASSERT(tail.size() == 1);
  ASSERT(tail.mode() == MARISA_BINARY_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa::UInt32) + tail.size()));
  ASSERT(offsets.size() == keys.size() + 1);
  ASSERT(offsets[0] == 1);
  ASSERT(offsets[1] == tail.size());

  const char binary_key[] = { 'N', 'P', '\0', 'T', 'r', 'i', 'e' };
  keys[0] = marisa::String(binary_key, sizeof(binary_key));
  tail.build(keys, &offsets, MARISA_TEXT_TAIL);

  ASSERT(tail.size() == sizeof(binary_key) + 1);
  ASSERT(tail.mode() == MARISA_BINARY_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa::UInt32) + tail.size()));
  ASSERT(offsets.size() == keys.size() + 1);
  ASSERT(offsets[0] == 1);
  ASSERT(offsets[1] == tail.size());

  tail.build(keys, &offsets, MARISA_BINARY_TAIL);

  ASSERT(tail.size() == sizeof(binary_key) + 1);
  ASSERT(tail.mode() == MARISA_BINARY_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa::UInt32) + tail.size()));
  ASSERT(offsets.size() == keys.size() + 1);
  ASSERT(offsets[0] == 1);
  ASSERT(offsets[1] == tail.size());

  keys.clear();
  keys.push_back(marisa::String("abc"));
  keys.push_back(marisa::String("bc"));
  keys.push_back(marisa::String("abc"));
  keys.push_back(marisa::String("c"));
  keys.push_back(marisa::String("ABC"));
  keys.push_back(marisa::String("AB"));

  tail.build(keys, NULL, MARISA_BINARY_TAIL);

  ASSERT(tail.size() == 15);
  ASSERT(tail.mode() == MARISA_BINARY_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa::UInt32) + tail.size()));

  tail.build(keys, &offsets, MARISA_BINARY_TAIL);

  ASSERT(tail.size() == 15);
  ASSERT(tail.mode() == MARISA_BINARY_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa::UInt32) + tail.size()));
  ASSERT(offsets.size() == 7);
  for (marisa::UInt32 i = 0; i < keys.size(); ++i) {
    ASSERT(marisa::String(reinterpret_cast<const char *>(tail[offsets[i]]),
        offsets[i + 1] - offsets[i]) == keys[i]);
  }

  tail.save("tail-test.dat");
  tail.clear();

  ASSERT(tail.size() == 0);
  ASSERT(tail.empty());
  ASSERT(tail.total_size() == sizeof(marisa::UInt32));

  marisa::Mapper mapper;
  tail.mmap(&mapper, "tail-test.dat");

  ASSERT(tail.size() == 15);
  ASSERT(tail.mode() == MARISA_BINARY_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa::UInt32) + tail.size()));
  for (marisa::UInt32 i = 0; i < keys.size(); ++i) {
    ASSERT(marisa::String(reinterpret_cast<const char *>(tail[offsets[i]]),
        offsets[i + 1] - offsets[i]) == keys[i]);
  }

  tail.clear();
  tail.load("tail-test.dat");

  ASSERT(tail.size() == 15);
  ASSERT(tail.mode() == MARISA_BINARY_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa::UInt32) + tail.size()));
  for (marisa::UInt32 i = 0; i < keys.size(); ++i) {
    ASSERT(marisa::String(reinterpret_cast<const char *>(tail[offsets[i]]),
        offsets[i + 1] - offsets[i]) == keys[i]);
  }

  std::stringstream stream;
  tail.write(stream);

  tail.clear();
  tail.read(stream);

  ASSERT(tail.size() == 15);
  ASSERT(tail.mode() == MARISA_BINARY_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa::UInt32) + tail.size()));
  for (marisa::UInt32 i = 0; i < keys.size(); ++i) {
    ASSERT(marisa::String(reinterpret_cast<const char *>(tail[offsets[i]]),
        offsets[i + 1] - offsets[i]) == keys[i]);
  }

  TEST_END();
}

void TestTextTail() {
  TEST_START();

  marisa::Tail tail;
  marisa::Vector<marisa::String> keys;
  tail.build(keys, NULL, MARISA_TEXT_TAIL);

  ASSERT(tail.size() == 0);
  ASSERT(tail.empty());
  ASSERT(tail.total_size() == sizeof(marisa::UInt32));

  keys.push_back(marisa::String(""));
  marisa::Vector<marisa::UInt32> offsets;
  tail.build(keys, &offsets, MARISA_TEXT_TAIL);

  ASSERT(tail.size() == 2);
  ASSERT(tail.mode() == MARISA_TEXT_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa::UInt32) + tail.size()));
  ASSERT(offsets.size() == keys.size());
  ASSERT(offsets[0] == 1);
  ASSERT(*tail[offsets[0]] == '\0');

  keys.clear();
  keys.push_back(marisa::String("abc"));
  keys.push_back(marisa::String("bc"));
  keys.push_back(marisa::String("abc"));
  keys.push_back(marisa::String("c"));
  keys.push_back(marisa::String("ABC"));
  keys.push_back(marisa::String("AB"));

  tail.build(keys, NULL, MARISA_TEXT_TAIL);

  ASSERT(tail.size() == 12);
  ASSERT(tail.mode() == MARISA_TEXT_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa::UInt32) + tail.size()));

  tail.build(keys, &offsets, MARISA_TEXT_TAIL);

  ASSERT(tail.size() == 12);
  ASSERT(tail.mode() == MARISA_TEXT_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa::UInt32) + tail.size()));
  ASSERT(offsets.size() == keys.size());
  for (marisa::UInt32 i = 0; i < keys.size(); ++i) {
    ASSERT(marisa::String(reinterpret_cast<const char *>(
        tail[offsets[i]])) == keys[i]);
  }

  tail.save("tail-test.dat");
  tail.clear();

  ASSERT(tail.size() == 0);
  ASSERT(tail.empty());
  ASSERT(tail.total_size() == sizeof(marisa::UInt32));

  marisa::Mapper mapper;
  tail.mmap(&mapper, "tail-test.dat");

  ASSERT(tail.size() == 12);
  ASSERT(tail.mode() == MARISA_TEXT_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == sizeof(marisa::UInt32) + tail.size());
  for (marisa::UInt32 i = 0; i < keys.size(); ++i) {
    ASSERT(marisa::String(reinterpret_cast<const char *>(
        tail[offsets[i]])) == keys[i]);
  }

  tail.clear();
  tail.load("tail-test.dat");

  ASSERT(tail.size() == 12);
  ASSERT(tail.mode() == MARISA_TEXT_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == sizeof(marisa::UInt32) + tail.size());
  for (marisa::UInt32 i = 0; i < keys.size(); ++i) {
    ASSERT(marisa::String(reinterpret_cast<const char *>(
        tail[offsets[i]])) == keys[i]);
  }

  std::stringstream stream;
  tail.write(stream);

  tail.clear();
  tail.read(stream);

  ASSERT(tail.size() == 12);
  ASSERT(tail.mode() == MARISA_TEXT_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == sizeof(marisa::UInt32) + tail.size());
  for (marisa::UInt32 i = 0; i < keys.size(); ++i) {
    ASSERT(marisa::String(reinterpret_cast<const char *>(
        tail[offsets[i]])) == keys[i]);
  }

  TEST_END();
}

}  // namespace

int main() {
  TestBinaryTail();
  TestTextTail();

  return 0;
}
