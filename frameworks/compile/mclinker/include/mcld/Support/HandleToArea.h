//===- HandleToArea.h -----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_FILE_HANDLE_TO_MEMORY_AREA_H
#define MCLD_FILE_HANDLE_TO_MEMORY_AREA_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <mcld/ADT/Uncopyable.h>
#include <mcld/ADT/TypeTraits.h>
#include <mcld/ADT/StringHash.h>
#include <mcld/Support/Path.h>
#include <mcld/Support/FileHandle.h>
#include <vector>

namespace mcld {

class MemoryArea;

/** \class HandleToArea
 *
 *  Special double-key associative container. Keys are Path and file handler,
 *  associative value is MemoryArea.
 *
 *  For high performance, HandleToArea is not designed to contain unique
 *  <key, value> pair. The key and value may be duplicated.
 *
 *  Like FileHandle, HandleToArea should neither throw exception nor call
 *  expressive diagnostic.
 */
class HandleToArea : private Uncopyable
{
private:
  struct Bucket {
    unsigned int hash_value;
    FileHandle* handle;
    MemoryArea* area;
  };

  // the best data structure is a binary search tree.
  // However, by the shrinking time-to-market constraint, I used
  // vector and sequential search here.
  typedef std::vector<Bucket> HandleToAreaMap;

  typedef hash::StringHash<hash::BKDR> HashFunction;

public:
  typedef HandleToAreaMap::iterator iterator;
  typedef HandleToAreaMap::const_iterator const_iterator;

public:
  struct Result {
  public:
    Result(FileHandle* pHandle, MemoryArea* pArea)
      : handle(pHandle), area(pArea) { }

  public:
    FileHandle* handle;
    MemoryArea* area;
  };

  struct ConstResult {
  public:
    ConstResult(const FileHandle* pHandle, const MemoryArea* pArea)
      : handle(pHandle), area(pArea) { }

  public:
    const FileHandle* handle;
    const MemoryArea* area;
  };

public:
  bool push_back(FileHandle* pHandle, MemoryArea* pArea);

  bool erase(MemoryArea* pArea);

  bool erase(const sys::fs::Path& pPath);

  Result findFirst(const sys::fs::Path& pPath);

  ConstResult findFirst(const sys::fs::Path& pPath) const;

  iterator begin()
  { return m_AreaMap.begin(); }

  iterator end()
  { return m_AreaMap.end(); }

  const_iterator begin() const
  { return m_AreaMap.begin(); }

  const_iterator end() const
  { return m_AreaMap.end(); }

  // -----  capacity  ----- //
  bool empty() const
  { return m_AreaMap.empty(); }

  size_t size() const
  { return m_AreaMap.size(); }

  HandleToArea() : m_AreaMap() { }

  ~HandleToArea() { }

private:
  HandleToAreaMap m_AreaMap;
};

} // namespace of mcld

#endif

