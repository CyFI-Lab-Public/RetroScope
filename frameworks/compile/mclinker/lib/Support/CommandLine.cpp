//===- CommandLine.cpp ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Support/CommandLine.h>

#include <llvm/ADT/StringRef.h>
#include <llvm/ADT/Twine.h>
#include <llvm/ADT/StringSwitch.h>
#include <llvm/Support/ErrorHandling.h>

using namespace llvm;
using namespace llvm::cl;

using namespace mcld;

static const size_t MaxOptWidth = 8;  // arbitrary spacing for printOptionDiff

//===----------------------------------------------------------------------===//
// SearchDirParser
//===----------------------------------------------------------------------===//
// parse - Return true on error.
bool SearchDirParser::parse(Option &pOption,
                            StringRef pArgName,
                            StringRef pArg,
                            std::string &pValue)
{
  char separator = *(pArgName.data() + 1);
  if ('=' == separator)
    pValue = '=';
  pValue += pArg.str();
  return false;
}

void SearchDirParser::printOptionDiff(const Option &pOption,
                                      StringRef pValue,
                                      OptVal pDefault,
                                      size_t pGlobalWidth) const
{
  printOptionName(pOption, pGlobalWidth);
  outs() << "= " << pValue;
  size_t NumSpaces = MaxOptWidth > pValue.size()?MaxOptWidth - pValue.size():0;
  outs().indent(NumSpaces) << " (default: ";
  if (pDefault.hasValue())
    outs() << pDefault.getValue();
  else
    outs() << "*no default*";
  outs() << ")\n";
}

void SearchDirParser::anchor()
{
  // do nothing
}

//===----------------------------------------------------------------------===//
// parser<mcld::sys::fs::Path>
//===----------------------------------------------------------------------===//
bool parser<mcld::sys::fs::Path>::parse(llvm::cl::Option &O,
                       llvm::StringRef ArgName,
                       llvm::StringRef Arg,
                       mcld::sys::fs::Path &Val)
{
  Val.assign<llvm::StringRef::const_iterator>(Arg.begin(), Arg.end());
  return false;
}

void parser<mcld::sys::fs::Path>::printOptionDiff(const llvm::cl::Option &O,
                                                  const mcld::sys::fs::Path &V,
                                                  parser<mcld::sys::fs::Path>::OptVal Default,
                                                  size_t GlobalWidth) const
{
  printOptionName(O, GlobalWidth);
  outs() << "= " << V;
  size_t VSize = V.native().size();
  size_t NumSpaces = MaxOptWidth > VSize ? MaxOptWidth - VSize : 0;
  outs().indent(NumSpaces) << " (default: ";
  if (Default.hasValue())
    outs() << Default.getValue().c_str();
  else
    outs() << "*no default*";
  outs() << ")\n";
}

void parser<mcld::sys::fs::Path>::anchor()
{
  // do nothing
}

//===----------------------------------------------------------------------===//
// parser<mcld::ZOption>
//===----------------------------------------------------------------------===//
bool parser<mcld::ZOption>::parse(llvm::cl::Option &O,
                                  llvm::StringRef ArgName,
                                  llvm::StringRef Arg,
                                  mcld::ZOption &Val)
{
  if (0 == Arg.compare("combreloc"))
    Val.setKind(ZOption::CombReloc);
  else if (0 == Arg.compare("nocombreloc"))
    Val.setKind(ZOption::NoCombReloc);
  else if (0 == Arg.compare("defs"))
    Val.setKind(ZOption::Defs);
  else if (0 == Arg.compare("execstack"))
    Val.setKind(ZOption::ExecStack);
  else if (0 == Arg.compare("noexecstack"))
    Val.setKind(ZOption::NoExecStack);
  else if (0 == Arg.compare("initfirst"))
    Val.setKind(ZOption::InitFirst);
  else if (0 == Arg.compare("interpose"))
    Val.setKind(ZOption::InterPose);
  else if (0 == Arg.compare("loadfltr"))
    Val.setKind(ZOption::LoadFltr);
  else if (0 == Arg.compare("muldefs"))
    Val.setKind(ZOption::MulDefs);
  else if (0 == Arg.compare("nocopyreloc"))
    Val.setKind(ZOption::NoCopyReloc);
  else if (0 == Arg.compare("nodefaultlib"))
    Val.setKind(ZOption::NoDefaultLib);
  else if (0 == Arg.compare("nodelete"))
    Val.setKind(ZOption::NoDelete);
  else if (0 == Arg.compare("nodlopen"))
    Val.setKind(ZOption::NoDLOpen);
  else if (0 == Arg.compare("nodump"))
    Val.setKind(ZOption::NoDump);
  else if (0 == Arg.compare("relro"))
    Val.setKind(ZOption::Relro);
  else if (0 == Arg.compare("norelro"))
    Val.setKind(ZOption::NoRelro);
  else if (0 == Arg.compare("lazy"))
    Val.setKind(ZOption::Lazy);
  else if (0 == Arg.compare("now"))
    Val.setKind(ZOption::Now);
  else if (0 == Arg.compare("origin"))
    Val.setKind(ZOption::Origin);
  else if (Arg.startswith("common-page-size=")) {
    Val.setKind(ZOption::CommPageSize);
    long long unsigned size = 0;
    Arg.drop_front(17).getAsInteger(0, size);
    Val.setPageSize(static_cast<uint64_t>(size));
  } else if (Arg.startswith("max-page-size=")) {
    Val.setKind(ZOption::MaxPageSize);
    long long unsigned size = 0;
    Arg.drop_front(14).getAsInteger(0, size);
    Val.setPageSize(static_cast<uint64_t>(size));
  }

  if (ZOption::Unknown == Val.kind())
    llvm::report_fatal_error(llvm::Twine("unknown -z option: `") +
                             Arg +
                             llvm::Twine("'\n"));
  return false;
}

void parser<mcld::ZOption>::printOptionDiff(const llvm::cl::Option &O,
                                            const mcld::ZOption &V,
                                            parser<mcld::ZOption>::OptVal Default,
                                            size_t GlobalWidth) const
{
  // TODO
}

void parser<mcld::ZOption>::anchor()
{
  // do nothing
}

