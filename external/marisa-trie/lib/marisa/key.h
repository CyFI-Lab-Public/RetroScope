#ifndef MARISA_KEY_H_
#define MARISA_KEY_H_

namespace marisa {

template <typename T>
class Key {
 public:
  Key() : str_(), weight_(0.0), id_(0), terminal_(0) {}

  void set_str(const T &str) {
    str_ = str;
  }
  void set_weight(double weight) {
    weight_ = weight;
  }
  void set_id(UInt32 id) {
    id_ = id;
  }
  void set_terminal(UInt32 terminal) {
    terminal_ = terminal;
  }

  const T &str() const {
    return str_;
  }
  double weight() const {
    return weight_;
  }
  UInt32 id() const {
    return id_;
  }
  UInt32 terminal() const {
    return terminal_;
  }

 private:
  T str_;
  double weight_;
  UInt32 id_;
  UInt32 terminal_;
};

template <typename T>
inline bool operator<(const Key<T> &lhs, const T &rhs) {
  return lhs.str() < rhs;
}

template <typename T>
inline bool operator<(const T &lhs, const Key<T> &rhs) {
  return lhs < rhs.str();
}

template <typename T>
inline bool operator<(const Key<T> &lhs, const Key<T> &rhs) {
  return lhs.str() < rhs.str();
}

template <typename T>
inline bool operator==(const Key<T> &lhs, const Key<T> &rhs) {
  return lhs.str() == rhs.str();
}

}  // namespace marisa

#endif  // MARISA_KEY_H_
