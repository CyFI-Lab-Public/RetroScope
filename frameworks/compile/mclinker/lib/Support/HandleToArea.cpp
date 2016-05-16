//===- HandleToArea.cpp ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Support/HandleToArea.h>
#include <mcld/Support/MemoryArea.h>
#include <llvm/ADT/StringRef.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// HandleToArea
//===----------------------------------------------------------------------===//
bool HandleToArea::push_back(FileHandle* pHandle, MemoryArea* pArea)
{
  if (NULL == pHandle || NULL == pArea)
    return false;

  Bucket bucket;
  bucket.hash_value = HashFunction()(
                              llvm::StringRef(pHandle->path().native().c_str(),
                                              pHandle->path().native().size()));

  bucket.handle = pHandle;
  bucket.area = pArea;
  m_AreaMap.push_back(bucket);
  return true;
}

bool HandleToArea::erase(MemoryArea* pArea)
{
  if (NULL == pArea || NULL == pArea->handler())
    return false;

  return erase(pArea->handler()->path());
}

bool HandleToArea::erase(const sys::fs::Path& pPath)
{
  unsigned int hash_value = HashFunction()(
                                  llvm::StringRef(pPath.native().c_str(),
                                                  pPath.native().size()));

  HandleToAreaMap::iterator bucket, bEnd = m_AreaMap.end();
  for (bucket = m_AreaMap.begin(); bucket != bEnd; ++bucket) {
    if (bucket->hash_value == hash_value && bucket->handle->path() == pPath) {
      // found
      m_AreaMap.erase(bucket);
      return true;
    }
  }

  return false;
}

HandleToArea::Result HandleToArea::findFirst(const sys::fs::Path& pPath)
{
  unsigned int hash_value = HashFunction()(llvm::StringRef(pPath.native().c_str(),
                                                         pPath.native().size()));

  HandleToAreaMap::iterator bucket, bEnd = m_AreaMap.end();

  for (bucket = m_AreaMap.begin(); bucket != bEnd; ++bucket) {
    if (bucket->hash_value == hash_value) {
      if (bucket->handle->path() == pPath) {
        return Result(bucket->handle, bucket->area);
      }
    }
  }

  return Result(NULL, NULL);
}

HandleToArea::ConstResult HandleToArea::findFirst(const sys::fs::Path& pPath) const
{
  unsigned int hash_value = HashFunction()(llvm::StringRef(pPath.native().c_str(),
                                                         pPath.native().size()));

  HandleToAreaMap::const_iterator bucket, bEnd = m_AreaMap.end();

  for (bucket = m_AreaMap.begin(); bucket != bEnd; ++bucket) {
    if (bucket->hash_value == hash_value) {
      if (bucket->handle->path() == pPath) {
        return ConstResult(bucket->handle, bucket->area);
      }
    }
  }

  return ConstResult(NULL, NULL);
}

