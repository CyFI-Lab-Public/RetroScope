#if defined _WIN32 || defined _WIN64
#include <sys/types.h>
#include <sys/stat.h>
#include <Windows.h>
#else  // defined _WIN32 || defined _WIN64
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>
#include <unistd.h>
#endif  // defined _WIN32 || defined _WIN64

#include "mapper.h"

namespace marisa_alpha {

#if defined _WIN32 || defined _WIN64
Mapper::Mapper()
    : ptr_(NULL), origin_(NULL), avail_(0), size_(0),
      file_(NULL), map_(NULL) {}

Mapper::Mapper(const void *ptr, std::size_t size)
    : ptr_(ptr), origin_(NULL), avail_(size), size_(0),
      file_(NULL), map_(NULL) {
  MARISA_ALPHA_THROW_IF((ptr == NULL) && (size != 0),
      MARISA_ALPHA_PARAM_ERROR);
}
#else  // defined _WIN32 || defined _WIN64
Mapper::Mapper()
    : ptr_(NULL), origin_(MAP_FAILED), avail_(0), size_(0), fd_(-1) {}

Mapper::Mapper(const void *ptr, std::size_t size)
    : ptr_(ptr), origin_(MAP_FAILED), avail_(size), size_(0), fd_(-1) {
  MARISA_ALPHA_THROW_IF((ptr == NULL) && (size != 0),
      MARISA_ALPHA_PARAM_ERROR);
}
#endif  // defined _WIN32 || defined _WIN64

#if defined _WIN32 || defined _WIN64
Mapper::~Mapper() {
  if (origin_ != NULL) {
    ::UnmapViewOfFile(origin_);
  }

  if (map_ != NULL) {
    ::CloseHandle(map_);
  }

  if (file_ != NULL) {
    ::CloseHandle(file_);
  }
}
#else  // defined _WIN32 || defined _WIN64
Mapper::~Mapper() {
  if (origin_ != MAP_FAILED) {
    ::munmap(origin_, size_);
  }

  if (fd_ != -1) {
    ::close(fd_);
  }
}
#endif  // defined _WIN32 || defined _WIN64

#if defined _WIN32 || defined _WIN64
void Mapper::open(const char *filename, long offset, int whence) {
  MARISA_ALPHA_THROW_IF(is_open(), MARISA_ALPHA_STATE_ERROR);
  MARISA_ALPHA_THROW_IF(filename == NULL, MARISA_ALPHA_PARAM_ERROR);

  struct __stat64 st;
  if (::_stat64(filename, &st) != 0) {
    MARISA_ALPHA_THROW(MARISA_ALPHA_IO_ERROR);
  }
  const UInt64 file_size = st.st_size;
  MARISA_ALPHA_THROW_IF(file_size > MARISA_ALPHA_UINT32_MAX,
      MARISA_ALPHA_SIZE_ERROR);

  Mapper temp;
  temp.size_ = (std::size_t)file_size;

  temp.file_ = ::CreateFileA(filename, GENERIC_READ, FILE_SHARE_READ,
    NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
  MARISA_ALPHA_THROW_IF(temp.file_ == NULL, MARISA_ALPHA_IO_ERROR);

  temp.map_ = ::CreateFileMapping(temp.file_, NULL, PAGE_READONLY, 0, 0, NULL);
  MARISA_ALPHA_THROW_IF(temp.map_ == NULL, MARISA_ALPHA_IO_ERROR);

  temp.origin_ = ::MapViewOfFile(temp.map_, FILE_MAP_READ, 0, 0, 0);
  MARISA_ALPHA_THROW_IF(temp.origin_ == NULL, MARISA_ALPHA_IO_ERROR);

  temp.seek(offset, whence);
  temp.swap(this);
}
#else  // defined _WIN32 || defined _WIN64
void Mapper::open(const char *filename, long offset, int whence) {
  MARISA_ALPHA_THROW_IF(is_open(), MARISA_ALPHA_STATE_ERROR);
  MARISA_ALPHA_THROW_IF(filename == NULL, MARISA_ALPHA_PARAM_ERROR);

  struct stat st;
  if (::stat(filename, &st) != 0) {
    MARISA_ALPHA_THROW(MARISA_ALPHA_IO_ERROR);
  }
  UInt64 file_size = st.st_size;
  MARISA_ALPHA_THROW_IF(file_size > MARISA_ALPHA_UINT32_MAX,
      MARISA_ALPHA_SIZE_ERROR);

  Mapper temp;
  temp.size_ = (std::size_t)file_size;

  temp.fd_ = ::open(filename, O_RDONLY);
  MARISA_ALPHA_THROW_IF(temp.fd_ == -1, MARISA_ALPHA_IO_ERROR);

  temp.origin_ = ::mmap(NULL, temp.size_, PROT_READ, MAP_SHARED, temp.fd_, 0);
  MARISA_ALPHA_THROW_IF(temp.origin_ == MAP_FAILED, MARISA_ALPHA_IO_ERROR);

  temp.seek(offset, whence);
  temp.swap(this);
}
#endif  // defined _WIN32 || defined _WIN64

void Mapper::clear() {
  Mapper().swap(this);
}

void Mapper::swap(Mapper *rhs) {
  MARISA_ALPHA_THROW_IF(rhs == NULL, MARISA_ALPHA_PARAM_ERROR);
  Swap(&ptr_, &rhs->ptr_);
  Swap(&avail_, &rhs->avail_);
  Swap(&origin_, &rhs->origin_);
  Swap(&size_, &rhs->size_);
#if defined _WIN32 || defined _WIN64
  Swap(&file_, &rhs->file_);
  Swap(&map_, &rhs->map_);
#else  // defined _WIN32 || defined _WIN64
  Swap(&fd_, &rhs->fd_);
#endif  // defined _WIN32 || defined _WIN64
}

void Mapper::seek(long offset, int whence) {
  switch (whence) {
    case SEEK_SET:
    case SEEK_CUR: {
      MARISA_ALPHA_THROW_IF((offset < 0) || ((unsigned long)offset > size_),
          MARISA_ALPHA_IO_ERROR);
      ptr_ = static_cast<const UInt8 *>(origin_) + offset;
      avail_ = (std::size_t)(size_ - offset);
      return;
    }
    case SEEK_END: {
      MARISA_ALPHA_THROW_IF((offset > 0) || ((unsigned long)-offset > size_),
          MARISA_ALPHA_IO_ERROR);
      ptr_ = static_cast<const UInt8 *>(origin_) + size_ + offset;
      avail_ = (std::size_t)-offset;
      return;
    }
    default: {
      MARISA_ALPHA_THROW(MARISA_ALPHA_PARAM_ERROR);
    }
  }
}

const void *Mapper::map_data(std::size_t size) {
  MARISA_ALPHA_THROW_IF(!is_open(), MARISA_ALPHA_STATE_ERROR);
  MARISA_ALPHA_THROW_IF(size > avail_, MARISA_ALPHA_IO_ERROR);
  ptr_ = static_cast<const UInt8 *>(ptr_) + size;
  avail_ -= size;
  return static_cast<const UInt8 *>(ptr_) - size;
}

}  // namespace marisa_alpha
