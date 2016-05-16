#include <limits.h>
#include <stdio.h>

#ifdef _MSC_VER
#include <io.h>
#else  // _MSC_VER
#include <unistd.h>
#endif  // _MSC_VER

#include "writer.h"

namespace marisa {

Writer::Writer()
    : file_(NULL), fd_(-1), stream_(NULL), needs_fclose_(false) {}

Writer::Writer(std::FILE *file)
    : file_(file), fd_(-1), stream_(NULL), needs_fclose_(false) {}

Writer::Writer(int fd)
    : file_(NULL), fd_(fd), stream_(NULL), needs_fclose_(false) {}

Writer::Writer(std::ostream *stream)
    : file_(NULL), fd_(-1), stream_(stream), needs_fclose_(false) {}

Writer::~Writer() {
  if (needs_fclose_) {
    ::fclose(file_);
  }
}

void Writer::open(const char *filename, bool trunc_flag,
    long offset, int whence) {
  MARISA_THROW_IF(is_open(), MARISA_STATE_ERROR);
  MARISA_THROW_IF(filename == NULL, MARISA_PARAM_ERROR);
#ifdef _MSC_VER
  std::FILE *file = NULL;
  if (!trunc_flag) {
    ::fopen_s(&file, filename, "rb+");
  }
  if (file == NULL) {
    if (::fopen_s(&file, filename, "wb") != 0) {
      MARISA_THROW(MARISA_IO_ERROR);
    }
  }
#else  // _MSC_VER
  std::FILE *file = NULL;
  if (!trunc_flag) {
    file = ::fopen(filename, "rb+");
  }
  if (file == NULL) {
    file = ::fopen(filename, "wb");
    MARISA_THROW_IF(file == NULL, MARISA_IO_ERROR);
  }
#endif  // _MSC_VER
  if (::fseek(file, offset, whence) != 0) {
    ::fclose(file);
    MARISA_THROW(MARISA_IO_ERROR);
  }
  file_ = file;
  needs_fclose_ = true;
}

void Writer::clear() {
  Writer().swap(this);
}

void Writer::swap(Writer *rhs) {
  MARISA_THROW_IF(rhs == NULL, MARISA_PARAM_ERROR);
  Swap(&file_, &rhs->file_);
  Swap(&fd_, &rhs->fd_);
  Swap(&stream_, &rhs->stream_);
  Swap(&needs_fclose_, &rhs->needs_fclose_);
}

void Writer::write_data(const void *data, std::size_t size) {
  if (fd_ != -1) {
    while (size != 0) {
#ifdef _MSC_VER
      const unsigned int count = (size < INT_MAX) ? size : INT_MAX;
      const int size_written = _write(fd_, data, count);
#else  // _MSC_VER
      const ::size_t count = (size < SSIZE_MAX) ? size : SSIZE_MAX;
      const ::ssize_t size_written = ::write(fd_, data, count);
#endif  // _MSC_VER
      MARISA_THROW_IF(size_written <= 0, MARISA_IO_ERROR);
      data = static_cast<const char *>(data) + size_written;
      size -= size_written;
    }
  } else if (file_ != NULL) {
    if ((::fwrite(data, 1, size, file_) != size) || (::fflush(file_) != 0)) {
      MARISA_THROW(MARISA_IO_ERROR);
    }
  } else if (stream_ != NULL) {
    if (!stream_->write(static_cast<const char *>(data), size)) {
      MARISA_THROW(MARISA_IO_ERROR);
    }
  } else {
    MARISA_THROW(MARISA_STATE_ERROR);
  }
}

}  // namespace marisa
