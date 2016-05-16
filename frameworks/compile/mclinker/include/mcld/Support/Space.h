//===- Space.h ------------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_MEMORY_SPACE_H
#define MCLD_MEMORY_SPACE_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <llvm/Support/DataTypes.h>
#include <mcld/ADT/TypeTraits.h>

namespace mcld {

class FileHandle;
class MemoryRegion;

/** \class Space
 *  \brief Space contains a chunk of memory space that does not overlap with
 *  the other Space.
 *
 */
class Space
{
public:
  enum Type
  {
    ALLOCATED_ARRAY,
    MMAPED,
    EXTERNAL,
    UNALLOCATED
  };

  typedef NonConstTraits<uint8_t>::pointer Address;
  typedef ConstTraits<uint8_t>::pointer ConstAddress;

private:
  Space();

  ~Space();

  Space(Type pType, void* pMemBuffer, size_t pSize);

public:
  void setStart(size_t pOffset)
  { m_StartOffset = pOffset; }

  Address memory()
  { return m_Data; }

  ConstAddress memory() const
  { return m_Data; }

  size_t start() const
  { return m_StartOffset; }

  size_t size() const
  { return m_Size; }

  Type type() const
  { return m_Type; }

  void addRegion(MemoryRegion& pRegion)
  { ++m_RegionCount; }

  void removeRegion(MemoryRegion& pRegion)
  { --m_RegionCount; }

  size_t numOfRegions() const
  { return m_RegionCount; }

  /// Create - Create a Space from external memory
  static Space* Create(void* pMemBuffer, size_t pSize);

  /// Create - Create a Space from FileHandler
  static Space* Create(FileHandle& pHandler, size_t pOffset, size_t pSize);

  static void Destroy(Space*& pSpace);
  
  static void Release(Space* pSpace, FileHandle& pHandler);

  static void Sync(Space* pSpace, FileHandle& pHandler);

private:
  Address m_Data;
  uint32_t m_StartOffset;
  uint32_t m_Size;
  uint16_t m_RegionCount;
  Type m_Type : 2;
};

} // namespace of mcld

#endif

