#include <limits.h>
#include <stdio.h>

#ifdef _MSC_VER
#include <io.h>
#else  // _MSC_VER
#include <unistd.h>
#endif  // _MSC_VER

#include "reader.h"

namespace marisa {

Reader::Reader()
    : file_(NULL), fd_(-1), stream_(NULL), needs_fclose_(false) {}

Reader::Reader(std::FILE *file)
    : file_(file), fd_(-1), stream_(NULL), needs_fclose_(false) {}

Reader::Reader(int fd)
    : file_(NULL), fd_(fd), stream_(NULL), needs_fclose_(false) {}

Reader::Reader(std::istream *stream)
    : file_(NULL), fd_(-1), stream_(stream), needs_fclose_(false) {}

Reader::~Reader() {
  if (needs_fclose_) {
    ::fclose(file_);
  }
}

void Reader::open(const char *filename, long offset, int whence) {
  MARISA_THROW_IF(is_open(), MARISA_STATE_ERROR);
  MARISA_THROW_IF(filename == NULL, MARISA_PARAM_ERROR);
#ifdef _MSC_VER
  std::FILE *file = NULL;
  if (::fopen_s(&file, filename, "rb") != 0) {
    MARISA_THROW(MARISA_IO_ERROR);
  }
#else  // _MSC_VER
  std::FILE * const file = ::fopen(filename, "rb");
  MARISA_THROW_IF(file == NULL, MARISA_IO_ERROR);
#endif  // _MSC_VER
  if (::fseek(file, offset, whence) != 0) {
    ::fclose(file);
    MARISA_THROW(MARISA_IO_ERROR);
  }
  file_ = file;
  needs_fclose_ = true;
}

void Reader::clear() {
  Reader().swap(this);
}

void Reader::swap(Reader *rhs) {
  MARISA_THROW_IF(rhs == NULL, MARISA_PARAM_ERROR);
  Swap(&file_, &rhs->file_);
  Swap(&fd_, &rhs->fd_);
  Swap(&stream_, &rhs->stream_);
  Swap(&needs_fclose_, &rhs->needs_fclose_);
}

void Reader::read_data(void *buf, std::size_t size) {
  if (fd_ != -1) {
    while (size != 0) {
#ifdef _MSC_VER
      const unsigned int count = (size < INT_MAX) ? size : INT_MAX;
      const int size_read = _read(fd_, buf, count);
#else  // _MSC_VER
      const ::size_t count = (size < SSIZE_MAX) ? size : SSIZE_MAX;
      const ::ssize_t size_read = ::read(fd_, buf, count);
#endif  // _MSC_VER
      MARISA_THROW_IF(size_read <= 0, MARISA_IO_ERROR);
      buf = static_cast<char *>(buf) + size_read;
      size -= size_read;
    }
  } else if (file_ != NULL) {
    if (::fread(buf, 1, size, file_) != size) {
      MARISA_THROW(MARISA_IO_ERROR);
    }
  } else if (stream_ != NULL) {
    if (!stream_->read(static_cast<char *>(buf), size)) {
      MARISA_THROW(MARISA_IO_ERROR);
    }
  } else {
    MARISA_THROW(MARISA_STATE_ERROR);
  }
}

}  // namespace marisa
