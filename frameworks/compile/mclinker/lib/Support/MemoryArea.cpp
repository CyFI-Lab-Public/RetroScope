//===- MemoryArea.cpp -----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Support/MemoryArea.h>
#include <mcld/Support/Space.h>
#include <mcld/Support/MemoryRegion.h>
#include <mcld/Support/FileHandle.h>
#include <mcld/Support/MsgHandling.h>

using namespace mcld;

//===--------------------------------------------------------------------===//
// MemoryArea
//===--------------------------------------------------------------------===//
// MemoryArea - special constructor
// This constructor is used for *SPECIAL* situation. I'm sorry I can not
// reveal what is the special situation.
MemoryArea::MemoryArea(Space& pUniverse)
  : m_pFileHandle(NULL) {
  m_SpaceMap.insert(std::make_pair(Key(pUniverse.start(), pUniverse.size()),
                                   &pUniverse));
}

MemoryArea::MemoryArea(FileHandle& pFileHandle)
  : m_pFileHandle(&pFileHandle) {
}

MemoryArea::~MemoryArea()
{
}

// The layout of MemorySpace in the virtual memory space
//
// |  : page boundary
// [,]: MemoryRegion
// -  : fillment
// =  : data
//
// |---[=|====|====|==]--|
// ^   ^              ^  ^
// |   |              |  |
// | r_start      +r_len |
// space.data      +space.size
//
// space.file_offset is the offset of the mapped file segment from the start of
// the file. if the MemorySpace's type is ALLOCATED_ARRAY, the distances of
// (space.data, r_start) and (r_len, space.size) are zero.
//
MemoryRegion* MemoryArea::request(size_t pOffset, size_t pLength)
{
  Space* space = find(pOffset, pLength);
  if (NULL == space) {
    // not found
    if (NULL == m_pFileHandle) {
      // if m_pFileHandle is NULL, clients delegate us an universal Space and
      // we never remove it. In that way, space can not be NULL.
      unreachable(diag::err_out_of_range_region) << pOffset << pLength;
    }

    space = Space::Create(*m_pFileHandle, pOffset, pLength);
    m_SpaceMap.insert(std::make_pair(Key(space->start(), space->size()), space));
  }

  // adjust r_start
  off_t distance = pOffset - space->start();
  void* r_start = space->memory() + distance;

  // now, we have a legal space to hold the new MemoryRegion
  return MemoryRegion::Create(r_start, pLength, *space);
}

// release - release a MemoryRegion
void MemoryArea::release(MemoryRegion* pRegion)
{
  if (NULL == pRegion)
    return;

  Space *space = pRegion->parent();
  MemoryRegion::Destroy(pRegion);

  if (0 == space->numOfRegions()) {

    if (NULL != m_pFileHandle) {
      // if m_pFileHandle is NULL, clients delegate us an universal Space and
      // we never remove it. Otherwise, we have to synchronize and release
      // Space.
      if (m_pFileHandle->isWritable()) {
        // synchronize writable space before we release it.
        Space::Sync(space, *m_pFileHandle);
      }

      std::pair<SpaceMapType::iterator, SpaceMapType::iterator> range =
        m_SpaceMap.equal_range(Key(space->start(), space->size()));
      SpaceMapType::iterator it;
      for (it = range.first; it != range.second; ++it) {
        if (space == it->second)
          break;
      }
      m_SpaceMap.erase(it);

      Space::Release(space, *m_pFileHandle);
      assert(NULL != space);
      Space::Destroy(space);
    }
  }
}

// clear - release all MemoryRegions
void MemoryArea::clear()
{
  if (NULL == m_pFileHandle)
    return;

  SpaceMapType::iterator space, sEnd = m_SpaceMap.end();
  if (m_pFileHandle->isWritable()) {
    for (space = m_SpaceMap.begin(); space != sEnd; ++space) {
      Space::Sync(space->second, *m_pFileHandle);
      Space::Release(space->second, *m_pFileHandle);
      assert(NULL != space->second);
      Space::Destroy(space->second);
    }
  }
  else {
    for (space = m_SpaceMap.begin(); space != sEnd; ++space) {
      Space::Release(space->second, *m_pFileHandle);
      assert(NULL != space->second);
      Space::Destroy(space->second);
    }
  }

  m_SpaceMap.clear();
}

//===--------------------------------------------------------------------===//
// SpaceList methods
//===--------------------------------------------------------------------===//
Space* MemoryArea::find(size_t pOffset, size_t pLength)
{
  std::pair<SpaceMapType::iterator, SpaceMapType::iterator> range =
    m_SpaceMap.equal_range(Key(pOffset, pLength));
  SpaceMapType::iterator it;
  for (it = range.first; it != range.second; ++it) {
    if ((it->second->start() <= pOffset) &&
        ((pOffset + pLength) <= (it->second->start() + it->second->size())))
      return it->second;
  }
  return NULL;
}

const Space* MemoryArea::find(size_t pOffset, size_t pLength) const
{
  std::pair<SpaceMapType::const_iterator, SpaceMapType::const_iterator> range =
    m_SpaceMap.equal_range(Key(pOffset, pLength));
  SpaceMapType::const_iterator it;
  for (it = range.first; it != range.second; ++it) {
    if ((it->second->start() <= pOffset) &&
        ((pOffset + pLength) <= (it->second->start() + it->second->size())))
      return it->second;
  }
  return NULL;
}

