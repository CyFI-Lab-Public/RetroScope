//===- ELFEmulation.cpp ---------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Target/ELFEmulation.h>
#include <mcld/LinkerScript.h>
#include <mcld/LinkerConfig.h>

#include <llvm/Support/Host.h>

using namespace mcld;

struct NameMap {
  const char* from; ///< the prefix of the input string. (match FROM*)
  const char* to;   ///< the output string.
};

static const NameMap map[] =
{
  {".text", ".text"},
  {".rodata", ".rodata"},
  {".data.rel.ro.local", ".data.rel.ro.local"},
  {".data.rel.ro", ".data.rel.ro"},
  {".data", ".data"},
  {".bss", ".bss"},
  {".tdata", ".tdata"},
  {".tbss", ".tbss"},
  {".init_array", ".init_array"},
  {".fini_array", ".fini_array"},
  // TODO: Support DT_INIT_ARRAY for all constructors?
  {".ctors", ".ctors"},
  {".dtors", ".dtors"},
  // FIXME: in GNU ld, if we are creating a shared object .sdata2 and .sbss2
  // sections would be handled differently.
  {".sdata2", ".sdata"},
  {".sbss2", ".sbss"},
  {".sdata", ".sdata"},
  {".sbss", ".sbss"},
  {".lrodata", ".lrodata"},
  {".ldata", ".ldata"},
  {".lbss", ".lbss"},
  {".gcc_except_table", ".gcc_except_table"},
  {".gnu.linkonce.d.rel.ro.local", ".data.rel.ro.local"},
  {".gnu.linkonce.d.rel.ro", ".data.rel.ro"},
  {".gnu.linkonce.r", ".rodata"},
  {".gnu.linkonce.d", ".data"},
  {".gnu.linkonce.b", ".bss"},
  {".gnu.linkonce.sb2", ".sbss"},
  {".gnu.linkonce.sb", ".sbss"},
  {".gnu.linkonce.s2", ".sdata"},
  {".gnu.linkonce.s", ".sdata"},
  {".gnu.linkonce.wi", ".debug_info"},
  {".gnu.linkonce.td", ".tdata"},
  {".gnu.linkonce.tb", ".tbss"},
  {".gnu.linkonce.t", ".text"},
  {".gnu.linkonce.lr", ".lrodata"},
  {".gnu.linkonce.lb", ".lbss"},
  {".gnu.linkonce.l", ".ldata"},
};

bool mcld::MCLDEmulateELF(LinkerScript& pScript, LinkerConfig& pConfig)
// FIXME: LinkerConfig& pConfig should be constant
{
  // set up section map
  if (pConfig.codeGenType() != LinkerConfig::Object) {
    const unsigned int map_size =  (sizeof(map) / sizeof(map[0]) );
    for (unsigned int i = 0; i < map_size; ++i) {
      bool exist = false;
      pScript.sectionMap().append(map[i].from, map[i].to, exist);
      if (exist)
        return false;
    }
  }

  if (!pConfig.options().nostdlib()) {
    // TODO: check if user sets the default search path instead via -Y option
    // set up default search path
    switch (pConfig.targets().triple().getOS()) {
      case llvm::Triple::NetBSD:
        pScript.directories().insert("=/usr/lib");
        break;
      case llvm::Triple::MinGW32:
        pScript.directories().insert("=/mingw/lib");
        break;
      default:
        pScript.directories().insert("=/lib");
        pScript.directories().insert("=/usr/lib");
        break;
    }
  }
  return true;
}

