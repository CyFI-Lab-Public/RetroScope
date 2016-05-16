#ifndef MARISA_VECTOR_INLINE_H_
#define MARISA_VECTOR_INLINE_H_

namespace marisa {

template <typename T>
Vector<T>::Vector()
    : buf_(NULL), objs_(NULL), size_(0), capacity_(0), fixed_(false) {}

template <typename T>
Vector<T>::~Vector() {
  if (buf_ != NULL) {
    for (std::size_t i = 0; i < size_; ++i) {
      buf_[i].~T();
    }
    delete [] reinterpret_cast<char *>(buf_);
  }
}

template <typename T>
void Vector<T>::mmap(Mapper *mapper, const char *filename,
    long offset, int whence) {
  MARISA_THROW_IF(mapper == NULL, MARISA_PARAM_ERROR);
  Mapper temp_mapper;
  temp_mapper.open(filename, offset, whence);
  map(temp_mapper);
  temp_mapper.swap(mapper);
}

template <typename T>
void Vector<T>::map(const void *ptr, std::size_t size) {
  Mapper mapper(ptr, size);
  map(mapper);
}

template <typename T>
void Vector<T>::map(Mapper &mapper) {
  UInt32 size;
  mapper.map(&size);
  Vector temp;
  mapper.map(&temp.objs_, size);
  temp.size_ = size;
  temp.fix();
  temp.swap(this);
}

template <typename T>
void Vector<T>::load(const char *filename,
    long offset, int whence) {
  Reader reader;
  reader.open(filename, offset, whence);
  read(reader);
}

template <typename T>
void Vector<T>::fread(std::FILE *file) {
  Reader reader(file);
  read(reader);
}

template <typename T>
void Vector<T>::read(int fd) {
  Reader reader(fd);
  read(reader);
}

template <typename T>
void Vector<T>::read(std::istream &stream) {
  Reader reader(&stream);
  read(reader);
}

template <typename T>
void Vector<T>::read(Reader &reader) {
  UInt32 size;
  reader.read(&size);
  Vector temp;
  temp.resize(size);
  reader.read(temp.buf_, size);
  temp.swap(this);
}

template <typename T>
void Vector<T>::save(const char *filename, bool trunc_flag,
    long offset, int whence) const {
  Writer writer;
  writer.open(filename, trunc_flag, offset, whence);
  write(writer);
}

template <typename T>
void Vector<T>::fwrite(std::FILE *file) const {
  Writer writer(file);
  write(writer);
}

template <typename T>
void Vector<T>::write(int fd) const {
  Writer writer(fd);
  write(writer);
}

template <typename T>
void Vector<T>::write(std::ostream &stream) const {
  Writer writer(&stream);
  write(writer);
}

template <typename T>
void Vector<T>::write(Writer &writer) const {
  writer.write(size_);
  writer.write(objs_, size_);
}

template <typename T>
void Vector<T>::push_back(const T &x) {
  MARISA_THROW_IF(fixed_, MARISA_STATE_ERROR);
  MARISA_THROW_IF(size_ == max_size(), MARISA_SIZE_ERROR);
  reserve(size_ + 1);
  new (&buf_[size_++]) T(x);
}

template <typename T>
void Vector<T>::pop_back() {
  MARISA_THROW_IF(fixed_ || (size_ == 0), MARISA_STATE_ERROR);
  buf_[--size_].~T();
}

template <typename T>
void Vector<T>::resize(std::size_t size) {
  MARISA_THROW_IF(fixed_, MARISA_STATE_ERROR);
  reserve(size);
  for (std::size_t i = size_; i < size; ++i) {
    new (&buf_[i]) T;
  }
  for (std::size_t i = size; i < size_; ++i) {
    buf_[i].~T();
  }
  size_ = (UInt32)size;
}

template <typename T>
void Vector<T>::resize(std::size_t size, const T &x) {
  MARISA_THROW_IF(fixed_, MARISA_STATE_ERROR);
  reserve(size);
  for (std::size_t i = size_; i < size; ++i) {
    new (&buf_[i]) T(x);
  }
  for (std::size_t i = size; i < size_; ++i) {
    buf_[i].~T();
  }
  size_ = (UInt32)size;
}

template <typename T>
void Vector<T>::reserve(std::size_t capacity) {
  MARISA_THROW_IF(fixed_, MARISA_STATE_ERROR);
  MARISA_THROW_IF(capacity > max_size(), MARISA_SIZE_ERROR);
  if (capacity <= capacity_) {
    return;
  }
  std::size_t new_capacity = capacity;
  if (capacity_ > (capacity / 2)) {
    if (capacity_ > (max_size() / 2)) {
      new_capacity = max_size();
    } else {
      new_capacity = capacity_ * 2;
    }
  }
  realloc(new_capacity);
}

template <typename T>
void Vector<T>::shrink() {
  MARISA_THROW_IF(fixed_, MARISA_STATE_ERROR);
  if (size_ != capacity_) {
    realloc(size_);
  }
}

template <typename T>
void Vector<T>::fix() {
  MARISA_THROW_IF(fixed_, MARISA_STATE_ERROR);
  fixed_ = true;
}

template <typename T>
void Vector<T>::swap(Vector *rhs) {
  MARISA_THROW_IF(rhs == NULL, MARISA_PARAM_ERROR);
  Swap(&buf_, &rhs->buf_);
  Swap(&objs_, &rhs->objs_);
  Swap(&size_, &rhs->size_);
  Swap(&capacity_, &rhs->capacity_);
  Swap(&fixed_, &rhs->fixed_);
}

template <typename T>
void Vector<T>::realloc(std::size_t new_capacity) {
  MARISA_THROW_IF(new_capacity > (MARISA_SIZE_MAX / sizeof(T)),
      MARISA_SIZE_ERROR);
  T * const new_buf = reinterpret_cast<T *>(
      new (std::nothrow) char[sizeof(T) * new_capacity]);
  MARISA_THROW_IF(new_buf == NULL, MARISA_MEMORY_ERROR);
  for (std::size_t i = 0; i < size_; ++i) {
    new (&new_buf[i]) T(buf_[i]);
    buf_[i].~T();
  }
  delete [] reinterpret_cast<char *>(buf_);
  buf_ = new_buf;
  objs_ = new_buf;
  capacity_ = (UInt32)new_capacity;
}

}  // namespace marisa

#endif  // MARISA_VECTOR_INLINE_H_
