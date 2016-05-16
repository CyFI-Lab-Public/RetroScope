#include <sstream>
#include <string>

#include <marisa_alpha/tail.h>

#include "assert.h"

namespace {

void TestBinaryTail() {
  TEST_START();

  marisa_alpha::Tail tail;

  ASSERT(tail.size() == 0);
  ASSERT(tail.empty());
  ASSERT(tail.total_size() == sizeof(marisa_alpha::UInt32));

  marisa_alpha::Vector<marisa_alpha::String> keys;
  tail.build(keys, NULL, MARISA_ALPHA_BINARY_TAIL);

  ASSERT(tail.size() == 0);
  ASSERT(tail.empty());
  ASSERT(tail.total_size() == sizeof(marisa_alpha::UInt32));

  keys.push_back(marisa_alpha::String(""));
  marisa_alpha::Vector<marisa_alpha::UInt32> offsets;
  tail.build(keys, &offsets, MARISA_ALPHA_BINARY_TAIL);

  ASSERT(tail.size() == 1);
  ASSERT(tail.mode() == MARISA_ALPHA_BINARY_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa_alpha::UInt32) + tail.size()));
  ASSERT(offsets.size() == keys.size() + 1);
  ASSERT(offsets[0] == 1);
  ASSERT(offsets[1] == tail.size());

  const char binary_key[] = { 'N', 'P', '\0', 'T', 'r', 'i', 'e' };
  keys[0] = marisa_alpha::String(binary_key, sizeof(binary_key));
  tail.build(keys, &offsets, MARISA_ALPHA_TEXT_TAIL);

  ASSERT(tail.size() == sizeof(binary_key) + 1);
  ASSERT(tail.mode() == MARISA_ALPHA_BINARY_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa_alpha::UInt32) + tail.size()));
  ASSERT(offsets.size() == keys.size() + 1);
  ASSERT(offsets[0] == 1);
  ASSERT(offsets[1] == tail.size());

  tail.build(keys, &offsets, MARISA_ALPHA_BINARY_TAIL);

  ASSERT(tail.size() == sizeof(binary_key) + 1);
  ASSERT(tail.mode() == MARISA_ALPHA_BINARY_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa_alpha::UInt32) + tail.size()));
  ASSERT(offsets.size() == keys.size() + 1);
  ASSERT(offsets[0] == 1);
  ASSERT(offsets[1] == tail.size());

  keys.clear();
  keys.push_back(marisa_alpha::String("abc"));
  keys.push_back(marisa_alpha::String("bc"));
  keys.push_back(marisa_alpha::String("abc"));
  keys.push_back(marisa_alpha::String("c"));
  keys.push_back(marisa_alpha::String("ABC"));
  keys.push_back(marisa_alpha::String("AB"));

  tail.build(keys, NULL, MARISA_ALPHA_BINARY_TAIL);

  ASSERT(tail.size() == 15);
  ASSERT(tail.mode() == MARISA_ALPHA_BINARY_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa_alpha::UInt32) + tail.size()));

  tail.build(keys, &offsets, MARISA_ALPHA_BINARY_TAIL);

  ASSERT(tail.size() == 15);
  ASSERT(tail.mode() == MARISA_ALPHA_BINARY_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa_alpha::UInt32) + tail.size()));
  ASSERT(offsets.size() == 7);
  for (marisa_alpha::UInt32 i = 0; i < keys.size(); ++i) {
    ASSERT(marisa_alpha::String(
        reinterpret_cast<const char *>(tail[offsets[i]]),
        offsets[i + 1] - offsets[i]) == keys[i]);
  }

  tail.save("tail-test.dat");
  tail.clear();

  ASSERT(tail.size() == 0);
  ASSERT(tail.empty());
  ASSERT(tail.total_size() == sizeof(marisa_alpha::UInt32));

  marisa_alpha::Mapper mapper;
  tail.mmap(&mapper, "tail-test.dat");

  ASSERT(tail.size() == 15);
  ASSERT(tail.mode() == MARISA_ALPHA_BINARY_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa_alpha::UInt32) + tail.size()));
  for (marisa_alpha::UInt32 i = 0; i < keys.size(); ++i) {
    ASSERT(marisa_alpha::String(
        reinterpret_cast<const char *>(tail[offsets[i]]),
        offsets[i + 1] - offsets[i]) == keys[i]);
  }

  tail.clear();
  tail.load("tail-test.dat");

  ASSERT(tail.size() == 15);
  ASSERT(tail.mode() == MARISA_ALPHA_BINARY_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa_alpha::UInt32) + tail.size()));
  for (marisa_alpha::UInt32 i = 0; i < keys.size(); ++i) {
    ASSERT(marisa_alpha::String(
        reinterpret_cast<const char *>(tail[offsets[i]]),
        offsets[i + 1] - offsets[i]) == keys[i]);
  }

  std::stringstream stream;
  tail.write(stream);

  tail.clear();
  tail.read(stream);

  ASSERT(tail.size() == 15);
  ASSERT(tail.mode() == MARISA_ALPHA_BINARY_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa_alpha::UInt32) + tail.size()));
  for (marisa_alpha::UInt32 i = 0; i < keys.size(); ++i) {
    ASSERT(marisa_alpha::String(
        reinterpret_cast<const char *>(tail[offsets[i]]),
        offsets[i + 1] - offsets[i]) == keys[i]);
  }

  TEST_END();
}

void TestTextTail() {
  TEST_START();

  marisa_alpha::Tail tail;
  marisa_alpha::Vector<marisa_alpha::String> keys;
  tail.build(keys, NULL, MARISA_ALPHA_TEXT_TAIL);

  ASSERT(tail.size() == 0);
  ASSERT(tail.empty());
  ASSERT(tail.total_size() == sizeof(marisa_alpha::UInt32));

  keys.push_back(marisa_alpha::String(""));
  marisa_alpha::Vector<marisa_alpha::UInt32> offsets;
  tail.build(keys, &offsets, MARISA_ALPHA_TEXT_TAIL);

  ASSERT(tail.size() == 2);
  ASSERT(tail.mode() == MARISA_ALPHA_TEXT_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa_alpha::UInt32) + tail.size()));
  ASSERT(offsets.size() == keys.size());
  ASSERT(offsets[0] == 1);
  ASSERT(*tail[offsets[0]] == '\0');

  keys.clear();
  keys.push_back(marisa_alpha::String("abc"));
  keys.push_back(marisa_alpha::String("bc"));
  keys.push_back(marisa_alpha::String("abc"));
  keys.push_back(marisa_alpha::String("c"));
  keys.push_back(marisa_alpha::String("ABC"));
  keys.push_back(marisa_alpha::String("AB"));

  tail.build(keys, NULL, MARISA_ALPHA_TEXT_TAIL);

  ASSERT(tail.size() == 12);
  ASSERT(tail.mode() == MARISA_ALPHA_TEXT_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa_alpha::UInt32) + tail.size()));

  tail.build(keys, &offsets, MARISA_ALPHA_TEXT_TAIL);

  ASSERT(tail.size() == 12);
  ASSERT(tail.mode() == MARISA_ALPHA_TEXT_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == (sizeof(marisa_alpha::UInt32) + tail.size()));
  ASSERT(offsets.size() == keys.size());
  for (marisa_alpha::UInt32 i = 0; i < keys.size(); ++i) {
    ASSERT(marisa_alpha::String(reinterpret_cast<const char *>(
        tail[offsets[i]])) == keys[i]);
  }

  tail.save("tail-test.dat");
  tail.clear();

  ASSERT(tail.size() == 0);
  ASSERT(tail.empty());
  ASSERT(tail.total_size() == sizeof(marisa_alpha::UInt32));

  marisa_alpha::Mapper mapper;
  tail.mmap(&mapper, "tail-test.dat");

  ASSERT(tail.size() == 12);
  ASSERT(tail.mode() == MARISA_ALPHA_TEXT_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == sizeof(marisa_alpha::UInt32) + tail.size());
  for (marisa_alpha::UInt32 i = 0; i < keys.size(); ++i) {
    ASSERT(marisa_alpha::String(reinterpret_cast<const char *>(
        tail[offsets[i]])) == keys[i]);
  }

  tail.clear();
  tail.load("tail-test.dat");

  ASSERT(tail.size() == 12);
  ASSERT(tail.mode() == MARISA_ALPHA_TEXT_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == sizeof(marisa_alpha::UInt32) + tail.size());
  for (marisa_alpha::UInt32 i = 0; i < keys.size(); ++i) {
    ASSERT(marisa_alpha::String(reinterpret_cast<const char *>(
        tail[offsets[i]])) == keys[i]);
  }

  std::stringstream stream;
  tail.write(stream);

  tail.clear();
  tail.read(stream);

  ASSERT(tail.size() == 12);
  ASSERT(tail.mode() == MARISA_ALPHA_TEXT_TAIL);
  ASSERT(!tail.empty());
  ASSERT(tail.total_size() == sizeof(marisa_alpha::UInt32) + tail.size());
  for (marisa_alpha::UInt32 i = 0; i < keys.size(); ++i) {
    ASSERT(marisa_alpha::String(reinterpret_cast<const char *>(
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
