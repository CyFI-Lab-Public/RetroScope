//===- StubFactory.cpp ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/LD/StubFactory.h>
#include <mcld/IRBuilder.h>
#include <mcld/LD/BranchIslandFactory.h>
#include <mcld/LD/BranchIsland.h>
#include <mcld/LD/LDSymbol.h>
#include <mcld/LD/ResolveInfo.h>
#include <mcld/Fragment/Stub.h>
#include <mcld/Fragment/Relocation.h>
#include <mcld/Fragment/FragmentRef.h>

#include <string>

using namespace mcld;

//===----------------------------------------------------------------------===//
// StubFactory
//===----------------------------------------------------------------------===//
StubFactory::~StubFactory()
{
  for (StubPoolType::iterator it = m_StubPool.begin(), ie = m_StubPool.end();
       it != ie; ++it)
    delete(*it);
}

/// addPrototype - register a stub prototype
void StubFactory::addPrototype(Stub* pPrototype)
{
  m_StubPool.push_back(pPrototype);
}

/// create - create a stub if needed, otherwise return NULL
Stub* StubFactory::create(Relocation& pReloc,
                          uint64_t pTargetSymValue,
                          IRBuilder& pBuilder,
                          BranchIslandFactory& pBRIslandFactory)
{
  // find if there is a prototype stub for the input relocation
  Stub* prototype = findPrototype(pReloc,
                                  pReloc.place(),
                                  pTargetSymValue);
  if (NULL != prototype) {
    // find the island for the input relocation
    BranchIsland* island = pBRIslandFactory.find(*(pReloc.targetRef().frag()));
    if (NULL == island) {
      island = pBRIslandFactory.produce(*(pReloc.targetRef().frag()));
    }

    // find if there is such a stub in the island already
    assert(NULL != island);
    Stub* stub = island->findStub(prototype, pReloc);
    if (NULL != stub) {
      // reset the branch target to the stub instead!
      pReloc.setSymInfo(stub->symInfo());
    }
    else {
      // create a stub from the prototype
      stub = prototype->clone();

      // build a name for stub symbol
      std::string name("__");
      name.append(pReloc.symInfo()->name());
      name.append("_");
      name.append(stub->name());
      name.append("@");
      name.append(island->name());

      // create LDSymbol for the stub
      LDSymbol* symbol =
        pBuilder.AddSymbol<IRBuilder::Force, IRBuilder::Resolve>(
                               name,
                               ResolveInfo::Function,
                               ResolveInfo::Define,
                               ResolveInfo::Local,
                               stub->size(), // size
                               stub->initSymValue(), // value
                               FragmentRef::Create(*stub, stub->initSymValue()),
                               ResolveInfo::Default);
      stub->setSymInfo(symbol->resolveInfo());

      // add relocations of this stub (i.e., set the branch target of the stub)
      for (Stub::fixup_iterator it = stub->fixup_begin(),
             ie = stub->fixup_end(); it != ie; ++it) {

        Relocation* reloc = Relocation::Create((*it)->type(),
                                 *(FragmentRef::Create(*stub, (*it)->offset())),
                                 (*it)->addend());
        reloc->setSymInfo(pReloc.symInfo());
        island->addRelocation(*reloc);
      }

      // add stub to the branch island
      island->addStub(prototype, pReloc, *stub);

      // reset the branch target of the input reloc to this stub instead!
      pReloc.setSymInfo(stub->symInfo());
      return stub;
    }
  }
  return NULL;
}

/// findPrototype - find if there is a registered stub prototype for the given
/// relocation
Stub* StubFactory::findPrototype(const Relocation& pReloc,
                                 uint64_t pSource,
                                 uint64_t pTargetSymValue)
{
  for (StubPoolType::iterator it = m_StubPool.begin(), ie = m_StubPool.end();
       it != ie; ++it) {
    if ((*it)->isMyDuty(pReloc, pSource, pTargetSymValue))
      return (*it);
  }
  return NULL;
}

