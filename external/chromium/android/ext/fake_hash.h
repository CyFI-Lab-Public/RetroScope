#ifndef FAKE_HASH
#define FAKE_HASH

#include <hash_map>
#include <hash_set>

namespace __gnu_cxx {
using std::hash_map;
using std::hash_set;

template <class _Key> struct hash { };

#define DEFINE_HASH(_type) \
    template<> \
    struct hash<_type> { \
        std::size_t operator()(_type val) const { \
            return std::hash<_type>(val); \
        } \
    }
#undef DEFINE_HASH

}

#endif
