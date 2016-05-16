//===- MemoryAreaFactory.cpp ----------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Support/MemoryAreaFactory.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/Support/SystemUtils.h>
#include <mcld/Support/Space.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// MemoryAreaFactory
//===----------------------------------------------------------------------===//
MemoryAreaFactory::MemoryAreaFactory(size_t pNum)
  : GCFactory<MemoryArea, 0>(pNum) {
}

MemoryAreaFactory::~MemoryAreaFactory()
{
  HandleToArea::iterator rec, rEnd = m_HandleToArea.end();
  for (rec = m_HandleToArea.begin(); rec != rEnd; ++rec) {
    if (rec->handle->isOpened()) {
      rec->handle->close();
    }
    delete rec->handle;
  }
}

MemoryArea*
MemoryAreaFactory::produce(const sys::fs::Path& pPath,
                           FileHandle::OpenMode pMode)
{
  HandleToArea::Result map_result = m_HandleToArea.findFirst(pPath);
  if (NULL == map_result.area) {
    // can not found
    FileHandle* handler = new FileHandle();
    if (!handler->open(pPath, pMode)) {
      error(diag::err_cannot_open_file) << pPath
                                        << sys::strerror(handler->error());
    }

    MemoryArea* result = allocate();
    new (result) MemoryArea(*handler);

    m_HandleToArea.push_back(handler, result);
    return result;
  }

  return map_result.area;
}

MemoryArea*
MemoryAreaFactory::produce(const sys::fs::Path& pPath,
                           FileHandle::OpenMode pMode,
                           FileHandle::Permission pPerm)
{
  HandleToArea::Result map_result = m_HandleToArea.findFirst(pPath);
  if (NULL == map_result.area) {
    // can not found
    FileHandle* handler = new FileHandle();
    if (!handler->open(pPath, pMode, pPerm)) {
      error(diag::err_cannot_open_file) << pPath
                                        << sys::strerror(handler->error());
    }

    MemoryArea* result = allocate();
    new (result) MemoryArea(*handler);

    m_HandleToArea.push_back(handler, result);
    return result;
  }

  return map_result.area;
}

MemoryArea* MemoryAreaFactory::produce(void* pMemBuffer, size_t pSize)
{
  Space* space = Space::Create(pMemBuffer, pSize);
  MemoryArea* result = allocate();
  new (result) MemoryArea(*space);
  return result;
}

MemoryArea*
MemoryAreaFactory::produce(int pFD, FileHandle::OpenMode pMode)
{
  FileHandle* handler = new FileHandle();
  handler->delegate(pFD, pMode);
  
  MemoryArea* result = allocate();
  new (result) MemoryArea(*handler);

  return result;
}

void MemoryAreaFactory::destruct(MemoryArea* pArea)
{
  m_HandleToArea.erase(pArea);
  pArea->clear();
  pArea->handler()->close();
  destroy(pArea);
  deallocate(pArea);
}

