//===- CommandLine.h ------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_COMMANDLINE_H
#define MCLD_COMMANDLINE_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <mcld/Support/FileSystem.h>
#include <mcld/MC/ZOption.h>

#include <llvm/ADT/StringRef.h>
#include <llvm/ADT/Triple.h>
#include <llvm/Support/CommandLine.h>

#include <string>

namespace llvm {
namespace cl {

//===----------------------------------------------------------------------===//
// SearchDirParser
//===----------------------------------------------------------------------===//
class SearchDirParser : public llvm::cl::basic_parser<std::string>
{
public:
  // parse - Return true on error.
  bool parse(Option &pOption,
             StringRef pArgName,
             StringRef pArg,
             std::string &pValue);

  const char *getValueName() const { return "searchdir"; }

  void printOptionDiff(const Option &pOption,
                       StringRef pValue,
                       OptVal pDefault,
                       size_t pGlobalWidth) const;

  void anchor();
};

//===----------------------------------------------------------------------===//
// FalseParser
//===----------------------------------------------------------------------===//
class FalseParser : public cl::parser<bool>
{
public:
  // parse - Return true on error.
  bool parse(cl::Option &O, StringRef ArgName, StringRef Arg, bool &Val) {
    if (cl::parser<bool>::parse(O, ArgName, Arg, Val))
      return false;
    Val = false;
    return false;
  }
};

//===----------------------------------------------------------------------===//
// parser<mcld::sys::fs::Path>
//===----------------------------------------------------------------------===//
template<>
class parser<mcld::sys::fs::Path> : public basic_parser<mcld::sys::fs::Path>
{
public:
  bool parse(Option &O,
             StringRef ArgName,
             StringRef Arg,
             mcld::sys::fs::Path &Val);

  virtual const char *getValueName() const { return "path"; }
  void printOptionDiff(const Option &O,
                       const mcld::sys::fs::Path &V,
                       OptVal Default,
                       size_t GlobalWidth) const;
  virtual void anchor();
};

//===----------------------------------------------------------------------===//
// parser<mcld::ZOption>
//===----------------------------------------------------------------------===//
template<>
class parser<mcld::ZOption> : public llvm::cl::basic_parser<mcld::ZOption>
{
public:
  bool parse(Option &O, StringRef ArgName, StringRef Arg, mcld::ZOption &Val);

  virtual const char *getValueName() const { return "z-option"; }
  void printOptionDiff(const Option &O,
                       const mcld::ZOption &V,
                       OptVal Default,
                       size_t GlobalWidth) const;
  virtual void anchor();
};

} // namespace of cl
} // namespace of llvm

#endif

