//===- MsgHandling.cpp ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/LD/DiagnosticEngine.h>
#include <mcld/LD/DiagnosticLineInfo.h>
#include <mcld/LD/DiagnosticPrinter.h>
#include <mcld/LD/TextDiagnosticPrinter.h>
#include <mcld/LD/MsgHandler.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/Support/raw_ostream.h>

#include <llvm/Support/ManagedStatic.h>
#include <llvm/Support/raw_ostream.h>
#include <llvm/Support/Signals.h>

#include <cstdlib>

using namespace mcld;

//===----------------------------------------------------------------------===//
// static variables
//===----------------------------------------------------------------------===//
static llvm::ManagedStatic<DiagnosticEngine> g_pEngine;

void
mcld::InitializeDiagnosticEngine(const mcld::LinkerConfig& pConfig,
                                 DiagnosticPrinter* pPrinter)
{
  g_pEngine->reset(pConfig);
  if (NULL != pPrinter)
    g_pEngine->setPrinter(*pPrinter, false);
  else {
    DiagnosticPrinter* printer = new TextDiagnosticPrinter(mcld::errs(), pConfig);
    g_pEngine->setPrinter(*printer, true);
  }
}

DiagnosticEngine& mcld::getDiagnosticEngine()
{
  return *g_pEngine;
}

bool mcld::Diagnose()
{
  if (g_pEngine->getPrinter()->getNumErrors() > 0) {
    // If we reached here, we are failing ungracefully. Run the interrupt handlers
    // to make sure any special cleanups get done, in particular that we remove
    // files registered with RemoveFileOnSignal.
    llvm::sys::RunInterruptHandlers();
    g_pEngine->getPrinter()->finish();
    return false;
  }
  return true;
}

void mcld::FinalizeDiagnosticEngine()
{
  g_pEngine->getPrinter()->finish();
}

