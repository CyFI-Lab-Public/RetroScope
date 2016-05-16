//===- raw_mem_ostream.cpp ------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Support/raw_mem_ostream.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/Support/MemoryRegion.h>
#include <mcld/Support/MemoryArea.h>
#include <mcld/Support/FileHandle.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// raw_mem_ostream
//===----------------------------------------------------------------------===//
raw_mem_ostream::raw_mem_ostream(MemoryArea &pMemoryArea)
  : m_MemoryArea(pMemoryArea), m_Position(0) {
  if (NULL == m_MemoryArea.handler() ||
      !(m_MemoryArea.handler()->isGood() &&
        m_MemoryArea.handler()->isWritable())) {
    fatal(diag::fatal_unwritable_output) << m_MemoryArea.handler()->path();
  }
}

raw_mem_ostream::~raw_mem_ostream()
{
  flush();
  m_MemoryArea.clear();
}

void raw_mem_ostream::write_impl(const char *pPtr, size_t pSize)
{
  MemoryRegion* region = m_MemoryArea.request(m_Position, pSize);
  memcpy(region->start(), pPtr, pSize);
  m_Position += pSize;
}

uint64_t raw_mem_ostream::current_pos() const
{
  return m_Position;
}

