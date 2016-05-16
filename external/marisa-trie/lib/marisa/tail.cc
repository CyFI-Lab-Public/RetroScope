#include <algorithm>
#include <functional>
#include <utility>

#include "tail.h"

namespace marisa {

Tail::Tail() : buf_() {}

void Tail::build(const Vector<String> &keys,
    Vector<UInt32> *offsets, int mode) {
  switch (mode) {
    case MARISA_BINARY_TAIL: {
      build_binary_tail(keys, offsets);
      return;
    }
    case MARISA_TEXT_TAIL: {
      if (!build_text_tail(keys, offsets)) {
        build_binary_tail(keys, offsets);
      }
      return;
    }
    default: {
      MARISA_THROW(MARISA_PARAM_ERROR);
    }
  }
}

void Tail::mmap(Mapper *mapper, const char *filename,
    long offset, int whence) {
  if (mapper == NULL) {
    MARISA_THROW(MARISA_PARAM_ERROR);
  }
  Mapper temp_mapper;
  temp_mapper.open(filename, offset, whence);
  map(temp_mapper);
  temp_mapper.swap(mapper);
}

void Tail::map(const void *ptr, std::size_t size) {
  Mapper mapper(ptr, size);
  map(mapper);
}

void Tail::map(Mapper &mapper) {
  Tail temp;
  temp.buf_.map(mapper);
  temp.swap(this);
}

void Tail::load(const char *filename, long offset, int whence) {
  Reader reader;
  reader.open(filename, offset, whence);
  read(reader);
}

void Tail::fread(::FILE *file) {
  Reader reader(file);
  read(reader);
}

void Tail::read(int fd) {
  Reader reader(fd);
  read(reader);
}

void Tail::read(std::istream &stream) {
  Reader reader(&stream);
  read(reader);
}

void Tail::read(Reader &reader) {
  Tail temp;
  temp.buf_.read(reader);
  temp.swap(this);
}

void Tail::save(const char *filename, bool trunc_flag,
    long offset, int whence) const {
  Writer writer;
  writer.open(filename, trunc_flag, offset, whence);
  write(writer);
}

void Tail::fwrite(::FILE *file) const {
  Writer writer(file);
  write(writer);
}

void Tail::write(int fd) const {
  Writer writer(fd);
  write(writer);
}

void Tail::write(std::ostream &stream) const {
  Writer writer(&stream);
  write(writer);
}

void Tail::write(Writer &writer) const {
  buf_.write(writer);
}

void Tail::clear() {
  Tail().swap(this);
}

void Tail::swap(Tail *rhs) {
  buf_.swap(&rhs->buf_);
}

void Tail::build_binary_tail(const Vector<String> &keys,
    Vector<UInt32> *offsets) {
  if (keys.empty()) {
    build_empty_tail(offsets);
    return;
  }

  Vector<UInt8> buf;
  buf.push_back('\0');

  Vector<UInt32> temp_offsets;
  temp_offsets.resize(keys.size() + 1);

  for (std::size_t i = 0; i < keys.size(); ++i) {
    temp_offsets[i] = (UInt32)buf.size();
    for (std::size_t j = 0; j < keys[i].length(); ++j) {
      buf.push_back(keys[i][j]);
    }
  }
  temp_offsets.back() = (UInt32)buf.size();
  buf.shrink();

  if (offsets != NULL) {
    temp_offsets.swap(offsets);
  }
  buf_.swap(&buf);
}

bool Tail::build_text_tail(const Vector<String> &keys,
    Vector<UInt32> *offsets) {
  if (keys.empty()) {
    build_empty_tail(offsets);
    return true;
  }

  typedef std::pair<RString, UInt32> KeyIdPair;
  Vector<KeyIdPair> pairs;
  pairs.resize(keys.size());
  for (std::size_t i = 0; i < keys.size(); ++i) {
    for (std::size_t j = 0; j < keys[i].length(); ++j) {
      if (keys[i][j] == '\0') {
        return false;
      }
    }
    pairs[i].first = RString(keys[i]);
    pairs[i].second = (UInt32)i;
  }
  std::sort(pairs.begin(), pairs.end(), std::greater<KeyIdPair>());

  Vector<UInt8> buf;
  buf.push_back('T');

  Vector<UInt32> temp_offsets;
  temp_offsets.resize(pairs.size(), 1);

  const KeyIdPair dummy_key;
  const KeyIdPair *last = &dummy_key;
  for (std::size_t i = 0; i < pairs.size(); ++i) {
    const KeyIdPair &cur = pairs[i];
    std::size_t match = 0;
    while ((match < cur.first.length()) && (match < last->first.length()) &&
        last->first[match] == cur.first[match]) {
      ++match;
    }
    if ((match == cur.first.length()) && (last->first.length() != 0)) {
      temp_offsets[cur.second] = (UInt32)(temp_offsets[last->second]
          + (last->first.length() - match));
    } else {
      temp_offsets[cur.second] = (UInt32)buf.size();
      for (std::size_t j = 1; j <= cur.first.length(); ++j) {
        buf.push_back(cur.first[cur.first.length() - j]);
      }
      buf.push_back('\0');
    }
    last = &cur;
  }
  buf.shrink();

  if (offsets != NULL) {
    temp_offsets.swap(offsets);
  }
  buf_.swap(&buf);
  return true;
}

void Tail::build_empty_tail(Vector<UInt32> *offsets) {
  buf_.clear();
  if (offsets != NULL) {
    offsets->clear();
  }
}

}  // namespace marisa
