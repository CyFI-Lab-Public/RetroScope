#ifndef MARISA_ALPHA_QUERY_H_
#define MARISA_ALPHA_QUERY_H_

#include <string>

#include "base.h"

namespace marisa_alpha {

class Query {
 public:
  Query(const char *ptr, std::size_t length) : ptr_(ptr), length_(length) {}
  Query(const Query &query) : ptr_(query.ptr_), length_(query.length_) {}

  void insert(std::string *str) const {
    str->insert(0, ptr_, length_);
  }

  UInt8 operator[](std::size_t i) const {
    MARISA_ALPHA_DEBUG_IF(i >= length_, MARISA_ALPHA_PARAM_ERROR);
    return ptr_[i];
  }
  bool ends_at(std::size_t i) const {
    MARISA_ALPHA_DEBUG_IF(i > length_, MARISA_ALPHA_PARAM_ERROR);
    return i == length_;
  }

 private:
  const char *ptr_;
  std::size_t length_;

  // Disallows assignment.
  Query &operator=(const Query &query);
};

class CQuery {
 public:
  explicit CQuery(const char *str) : str_(str) {}
  CQuery(const CQuery &query) : str_(query.str_) {}

  void insert(std::string *str) const {
    str->insert(0, str_);
  }

  UInt8 operator[](std::size_t i) const {
    return str_[i];
  }
  bool ends_at(std::size_t i) const {
    return str_[i] == '\0';
  }

 private:
  const char *str_;

  // Disallows assignment.
  CQuery &operator=(const CQuery &);
};

}  // namespace marisa_alpha

#endif  // MARISA_ALPHA_QUERY_H_
