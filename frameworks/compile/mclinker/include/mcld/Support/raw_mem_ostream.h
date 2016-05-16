//===- raw_mem_ostream.h --------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_RAW_MEMORY_AREA_OSTREAM_H
#define MCLD_RAW_MEMORY_AREA_OSTREAM_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <string>

#include <llvm/Support/raw_ostream.h>

namespace mcld {

class MemoryArea;

class raw_mem_ostream : public llvm::raw_ostream
{
public:
  /// constructor - pMemoryArea must be writable.
  explicit raw_mem_ostream(MemoryArea &pMemoryArea);

  ~raw_mem_ostream();

  MemoryArea& getMemoryArea() {
    flush();
    return m_MemoryArea;
  }

private:
  /// write_impl - See raw_ostream::write_impl.
  virtual void write_impl(const char *pPtr, size_t pSize);

  /// current_pos - Return the current position within the stream, not
  /// counting the bytes currently in the buffer.
  virtual uint64_t current_pos() const;

private:
  MemoryArea& m_MemoryArea;
  uint64_t m_Position;
};

} // namespace of mcld

#endif

