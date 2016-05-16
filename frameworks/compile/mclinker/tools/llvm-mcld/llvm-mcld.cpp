//===- llvm-mcld.cpp ------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Module.h>
#include <mcld/LinkerConfig.h>
#include <mcld/LinkerScript.h>
#include <mcld/Target/TargetMachine.h>
#include <mcld/Support/TargetSelect.h>
#include <mcld/Support/TargetRegistry.h>
#include <mcld/Support/CommandLine.h>
#include <mcld/Support/Path.h>
#include <mcld/Support/RealPath.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/Support/FileHandle.h>
#include <mcld/Support/FileSystem.h>
#include <mcld/Support/raw_ostream.h>
#include <mcld/Support/SystemUtils.h>
#include <mcld/Support/ToolOutputFile.h>
#include <mcld/LD/DiagnosticLineInfo.h>
#include <mcld/LD/TextDiagnosticPrinter.h>

#include <llvm/PassManager.h>
#include <llvm/Pass.h>
#include <llvm/IR/Module.h>
#include <llvm/IR/DataLayout.h>
#include <llvm/IR/LLVMContext.h>
#include <llvm/ADT/Triple.h>
#include <llvm/ADT/StringSwitch.h>
#include <llvm/MC/SubtargetFeature.h>
#include <llvm/Support/CommandLine.h>
#include <llvm/Support/Debug.h>
#include <llvm/Support/FormattedStream.h>
#include <llvm/Support/Host.h>
#include <llvm/Support/IRReader.h>
#include <llvm/Support/ManagedStatic.h>
#include <llvm/Support/Signals.h>
#include <llvm/Support/TargetRegistry.h>
#include <llvm/Support/TargetSelect.h>
#include <llvm/Support/Process.h>
#include <llvm/Target/TargetMachine.h>

#if defined(HAVE_UNISTD_H)
# include <unistd.h>
#endif

#if defined(_MSC_VER) || defined(__MINGW32__)
#include <io.h>
#ifndef STDIN_FILENO
# define STDIN_FILENO 0
#endif
#ifndef STDOUT_FILENO
# define STDOUT_FILENO 1
#endif
#ifndef STDERR_FILENO
# define STDERR_FILENO 2
#endif
#endif

using namespace llvm;

#ifdef ENABLE_UNITTEST
#include <gtest.h>

static cl::opt<bool>
UnitTest("unittest",  cl::desc("do unit test") );

int unit_test( int argc, char* argv[] )
{
  testing::InitGoogleTest( &argc, argv );
  return RUN_ALL_TESTS();
}

#endif

// General options for llc.  Other pass-specific options are specified
// within the corresponding llc passes, and target-specific options
// and back-end code generation options are specified with the target machine.
//
// Determine optimization level.
static cl::opt<char>
OptLevel("O",
         cl::desc("Optimization level. [-O0, -O1, -O2, or -O3] "
                  "(default = '-O2')"),
         cl::Prefix,
         cl::ZeroOrMore,
         cl::init(' '));

static cl::opt<std::string>
TargetTriple("mtriple", cl::desc("Override target triple for module"));

static cl::opt<std::string>
MArch("march", cl::desc("Architecture to generate code for (see --version)"));

static cl::opt<std::string>
MCPU("mcpu",
  cl::desc("Target a specific cpu type (-mcpu=help for details)"),
  cl::value_desc("cpu-name"),
  cl::init(""));

static cl::list<std::string>
MAttrs("mattr",
  cl::CommaSeparated,
  cl::desc("Target specific attributes (-mattr=help for details)"),
  cl::value_desc("a1,+a2,-a3,..."));

static cl::opt<llvm::CodeModel::Model>
CMModel("code-model",
        cl::desc("Choose code model"),
        cl::init(CodeModel::Default),
        cl::values(clEnumValN(CodeModel::Default, "default",
                              "Target default code model"),
                   clEnumValN(CodeModel::Small, "small",
                              "Small code model"),
                   clEnumValN(CodeModel::Kernel, "kernel",
                              "Kernel code model"),
                   clEnumValN(CodeModel::Medium, "medium",
                              "Medium code model"),
                   clEnumValN(CodeModel::Large, "large",
                              "Large code model"),
                   clEnumValEnd));

cl::opt<bool> NoVerify("disable-verify", cl::Hidden,
                       cl::desc("Do not verify input module"));

static cl::opt<bool>
EnableFPMAD("enable-fp-mad",
  cl::desc("Enable less precise MAD instructions to be generated"),
  cl::init(false));

static cl::opt<bool>
DisableFPElim("disable-fp-elim",
  cl::desc("Disable frame pointer elimination optimization"),
  cl::init(false));

static cl::opt<bool>
DisableFPElimNonLeaf("disable-non-leaf-fp-elim",
  cl::desc("Disable frame pointer elimination optimization for non-leaf funcs"),
  cl::init(false));

static cl::opt<llvm::FPOpFusion::FPOpFusionMode>
FuseFPOps("fuse-fp-ops",
  cl::desc("Enable aggresive formation of fused FP ops"),
  cl::init(FPOpFusion::Standard),
  cl::values(
    clEnumValN(FPOpFusion::Fast, "fast",
               "Fuse FP ops whenever profitable"),
    clEnumValN(FPOpFusion::Standard, "standard",
               "Only fuse 'blessed' FP ops."),
    clEnumValN(FPOpFusion::Strict, "strict",
               "Only fuse FP ops when the result won't be effected."),
    clEnumValEnd));

static cl::opt<bool>
EnableUnsafeFPMath("enable-unsafe-fp-math",
  cl::desc("Enable optimizations that may decrease FP precision"),
  cl::init(false));

static cl::opt<bool>
EnableNoInfsFPMath("enable-no-infs-fp-math",
  cl::desc("Enable FP math optimizations that assume no +-Infs"),
  cl::init(false));

static cl::opt<bool>
EnableNoNaNsFPMath("enable-no-nans-fp-math",
  cl::desc("Enable FP math optimizations that assume no NaNs"),
  cl::init(false));

static cl::opt<bool>
EnableHonorSignDependentRoundingFPMath("enable-sign-dependent-rounding-fp-math",
  cl::Hidden,
  cl::desc("Force codegen to assume rounding mode can change dynamically"),
  cl::init(false));

static cl::opt<bool>
GenerateSoftFloatCalls("soft-float",
  cl::desc("Generate software floating point library calls"),
  cl::init(false));

static cl::opt<llvm::FloatABI::ABIType>
FloatABIForCalls("float-abi",
  cl::desc("Choose float ABI type"),
  cl::init(FloatABI::Default),
  cl::values(
    clEnumValN(FloatABI::Default, "default",
               "Target default float ABI type"),
    clEnumValN(FloatABI::Soft, "soft",
               "Soft float ABI (implied by -soft-float)"),
    clEnumValN(FloatABI::Hard, "hard",
               "Hard float ABI (uses FP registers)"),
    clEnumValEnd));

static cl::opt<bool>
DontPlaceZerosInBSS("nozero-initialized-in-bss",
  cl::desc("Don't place zero-initialized symbols into bss section"),
  cl::init(false));

static cl::opt<bool>
EnableJITExceptionHandling("jit-enable-eh",
  cl::desc("Emit exception handling information"),
  cl::init(false));

// In debug builds, make this default to true.
#ifdef NDEBUG
#define EMIT_DEBUG false
#else
#define EMIT_DEBUG true
#endif
static cl::opt<bool>
EmitJitDebugInfo("jit-emit-debug",
  cl::desc("Emit debug information to debugger"),
  cl::init(EMIT_DEBUG));
#undef EMIT_DEBUG

static cl::opt<bool>
EmitJitDebugInfoToDisk("jit-emit-debug-to-disk",
  cl::Hidden,
  cl::desc("Emit debug info objfiles to disk"),
  cl::init(false));

static cl::opt<bool>
EnableGuaranteedTailCallOpt("tailcallopt",
  cl::desc("Turn fastcc calls into tail calls by (potentially) changing ABI."),
  cl::init(false));

static cl::opt<unsigned>
OverrideStackAlignment("stack-alignment",
  cl::desc("Override default stack alignment"),
  cl::init(0));

static cl::opt<bool>
EnableRealignStack("realign-stack",
  cl::desc("Realign stack if needed"),
  cl::init(true));

static cl::opt<std::string>
TrapFuncName("trap-func", cl::Hidden,
  cl::desc("Emit a call to trap function rather than a trap instruction"),
  cl::init(""));

static cl::opt<bool>
SegmentedStacks("segmented-stacks",
  cl::desc("Use segmented stacks if possible."),
  cl::init(false));

//===----------------------------------------------------------------------===//
// Command Line Options
// There are four kinds of command line options:
//   1. Bitcode option. Used to represent a bitcode.
//   2. Attribute options. Attributes describes the input file after them. For
//      example, --as-needed affects the input file after this option. Attribute
//      options are not attributes. Attribute options are the options that is
//      used to define a legal attribute.
//   3. Scripting options, Used to represent a subset of link scripting
//      language, such as --defsym.
//   4. General options. (the rest of options)
//===----------------------------------------------------------------------===//
// Bitcode Options
//===----------------------------------------------------------------------===//
static cl::opt<mcld::sys::fs::Path, false, llvm::cl::parser<mcld::sys::fs::Path> >
ArgBitcodeFilename("dB",
              cl::desc("set default bitcode"),
              cl::value_desc("bitcode"));

//===----------------------------------------------------------------------===//
// General Options
//===----------------------------------------------------------------------===//
static cl::opt<mcld::sys::fs::Path, false, llvm::cl::parser<mcld::sys::fs::Path> >
ArgOutputFilename("o",
               cl::desc("Output filename"),
               cl::value_desc("filename"));

static cl::alias
AliasOutputFilename("output",
                    cl::desc("alias for -o"),
                    cl::aliasopt(ArgOutputFilename));

static cl::opt<mcld::sys::fs::Path, false, llvm::cl::parser<mcld::sys::fs::Path> >
ArgSysRoot("sysroot",
           cl::desc("Use directory as the location of the sysroot, overriding the configure-time default."),
           cl::value_desc("directory"),
           cl::ValueRequired);

static cl::list<std::string, bool, llvm::cl::SearchDirParser>
ArgSearchDirList("L",
                 cl::ZeroOrMore,
                 cl::desc("Add path searchdir to the list of paths that ld will search for archive libraries and ld control scripts."),
                 cl::value_desc("searchdir"),
                 cl::Prefix);

static cl::alias
ArgSearchDirListAlias("library-path",
                      cl::desc("alias for -L"),
                      cl::aliasopt(ArgSearchDirList));

static cl::opt<bool>
ArgTrace("t",
         cl::desc("Print the names of the input files as ld processes them."));

static cl::alias
ArgTraceAlias("trace",
              cl::desc("alias for -t"),
              cl::aliasopt(ArgTrace));

static cl::opt<int>
ArgVerbose("verbose",
           cl::init(-1),
           cl::desc("Display the version number for ld and list the linker emulations supported."));

static cl::opt<bool>
ArgVersion("V",
           cl::init(false),
           cl::desc("Display the version number for MCLinker."));

static cl::opt<int>
ArgMaxErrorNum("error-limit",
               cl::init(-1),
               cl::desc("limits the maximum number of erros."));

static cl::opt<int>
ArgMaxWarnNum("warning-limit",
               cl::init(-1),
               cl::desc("limits the maximum number of warnings."));

static cl::opt<std::string>
ArgEntry("e",
         cl::desc("Use entry as the explicit symbol for beginning execution of your program."),
         cl::value_desc("entry"),
         cl::ValueRequired);

static cl::alias
ArgEntryAlias("entry",
              cl::desc("alias for -e"),
              cl::aliasopt(ArgEntry));

static cl::opt<bool>
ArgBsymbolic("Bsymbolic",
             cl::desc("Bind references within the shared library."),
             cl::init(false));

static cl::opt<bool>
ArgBgroup("Bgroup",
          cl::desc("Info the dynamic linker to perform lookups only inside the group."),
          cl::init(false));

static cl::opt<std::string>
ArgSOName("soname",
          cl::desc("Set internal name of shared library"),
          cl::value_desc("name"));

static cl::opt<bool>
ArgNoUndefined("no-undefined",
               cl::desc("Do not allow unresolved references"),
               cl::init(false));

static cl::opt<bool>
ArgAllowMulDefs("allow-multiple-definition",
                cl::desc("Allow multiple definition"),
                cl::init(false));

static cl::opt<bool>
ArgEhFrameHdr("eh-frame-hdr",
              cl::desc("Request creation of \".eh_frame_hdr\" section and ELF \"PT_GNU_EH_FRAME\" segment header."),
              cl::init(false));

static cl::list<mcld::ZOption, bool, llvm::cl::parser<mcld::ZOption> >
ArgZOptionList("z",
               cl::ZeroOrMore,
               cl::desc("The -z options for GNU ld compatibility."),
               cl::value_desc("keyword"),
               cl::Prefix);

cl::opt<mcld::CodeGenFileType>
ArgFileType("filetype", cl::init(mcld::CGFT_EXEFile),
  cl::desc("Choose a file type (not all types are supported by all targets):"),
  cl::values(
       clEnumValN(mcld::CGFT_ASMFile, "asm",
                  "Emit an assembly ('.s') file"),
       clEnumValN(mcld::CGFT_OBJFile, "obj",
                  "Emit a relocatable object ('.o') file"),
       clEnumValN(mcld::CGFT_DSOFile, "dso",
                  "Emit an dynamic shared object ('.so') file"),
       clEnumValN(mcld::CGFT_EXEFile, "exe",
                  "Emit a executable ('.exe') file"),
       clEnumValN(mcld::CGFT_NULLFile, "null",
                  "Emit nothing, for performance testing"),
       clEnumValEnd));

static cl::opt<bool>
ArgShared("shared",
          cl::desc("Create a shared library."),
          cl::init(false));

static cl::alias
ArgSharedAlias("Bshareable",
               cl::desc("alias for -shared"),
               cl::aliasopt(ArgShared));

static cl::opt<bool>
ArgPIE("pie",
       cl::desc("Emit a position-independent executable file"),
       cl::init(false));

static cl::opt<bool>
ArgRelocatable("relocatable",
               cl::desc("Generate relocatable output"),
               cl::init(false));

static cl::alias
ArgRelocatableAlias("r",
                    cl::desc("alias for --relocatable"),
                    cl::aliasopt(ArgRelocatable));

static cl::opt<Reloc::Model>
ArgRelocModel("relocation-model",
             cl::desc("Choose relocation model"),
             cl::init(Reloc::Default),
             cl::values(
               clEnumValN(Reloc::Default, "default",
                       "Target default relocation model"),
               clEnumValN(Reloc::Static, "static",
                       "Non-relocatable code"),
               clEnumValN(Reloc::PIC_, "pic",
                       "Fully relocatable, position independent code"),
               clEnumValN(Reloc::DynamicNoPIC, "dynamic-no-pic",
                       "Relocatable external references, non-relocatable code"),
               clEnumValEnd));

static cl::opt<bool>
ArgFPIC("fPIC",
        cl::desc("Set relocation model to pic. The same as -relocation-model=pic."),
        cl::init(false));

static cl::opt<std::string>
ArgDyld("dynamic-linker",
        cl::ZeroOrMore,
        cl::desc("Set the name of the dynamic linker."),
        cl::value_desc("Program"));

namespace color {
enum Color {
  Never,
  Always,
  Auto
};
} // namespace of color

static cl::opt<color::Color>
ArgColor("color",
  cl::value_desc("WHEN"),
  cl::desc("Surround the result strings with the marker"),
  cl::init(color::Auto),
  cl::values(
    clEnumValN(color::Never, "never",
      "do not surround result strings"),
    clEnumValN(color::Always, "always",
      "always surround result strings, even the output is a plain file"),
    clEnumValN(color::Auto, "auto",
      "surround result strings only if the output is a tty"),
    clEnumValEnd));

static cl::opt<bool>
ArgDiscardLocals("discard-locals",
                 cl::desc("Delete all temporary local symbols."),
                 cl::init(false));

static cl::alias
ArgDiscardLocalsAlias("X",
                      cl::desc("alias for --discard-locals"),
                      cl::aliasopt(ArgDiscardLocals));

static cl::opt<bool>
ArgDiscardAll("discard-all",
              cl::desc("Delete all local symbols."),
              cl::init(false));

static cl::alias
ArgDiscardAllAlias("x",
                   cl::desc("alias for --discard-all"),
                   cl::aliasopt(ArgDiscardAll));

static cl::opt<bool>
ArgStripDebug("strip-debug",
              cl::desc("Omit debugger symbol information from the output file."),
              cl::init(false));

static cl::alias
ArgStripDebugAlias("S",
                   cl::desc("alias for --strip-debug"),
                   cl::aliasopt(ArgStripDebug));

static cl::opt<bool>
ArgStripAll("strip-all",
            cl::desc("Omit all symbol information from the output file."),
            cl::init(false));

static cl::alias
ArgStripAllAlias("s",
                 cl::desc("alias for --strip-all"),
                 cl::aliasopt(ArgStripAll));

static cl::opt<bool>
ArgNMagic("nmagic",
          cl::desc("Do not page align data"),
          cl::init(false));

static cl::alias
ArgNMagicAlias("n",
               cl::desc("alias for --nmagic"),
               cl::aliasopt(ArgNMagic));

static cl::opt<bool>
ArgOMagic("omagic",
          cl::desc("Do not page align data, do not make text readonly"),
          cl::init(false));

static cl::alias
ArgOMagicAlias("N",
               cl::desc("alias for --omagic"),
               cl::aliasopt(ArgOMagic));


static cl::opt<int>
ArgGPSize("G",
          cl::desc("Set the maximum size of objects to be optimized using GP"),
          cl::init(8));

/// @{
/// @name FIXME: begin of unsupported options
/// @}
static cl::opt<bool>
ArgGCSections("gc-sections",
              cl::desc("Enable garbage collection of unused input sections."),
              cl::init(false));

static cl::opt<bool>
ArgNoGCSections("no-gc-sections",
              cl::desc("disable garbage collection of unused input sections."),
              cl::init(false));

namespace icf {
enum Mode {
  None,
  All,
  Safe
};
} // namespace of icf

static cl::opt<icf::Mode>
ArgICF("icf",
       cl::ZeroOrMore,
       cl::desc("Identical Code Folding"),
       cl::init(icf::None),
       cl::values(
         clEnumValN(icf::None, "none",
           "do not perform cold folding"),
         clEnumValN(icf::All, "all",
           "always preform cold folding"),
         clEnumValN(icf::Safe, "safe",
           "Folds ctors, dtors and functions whose pointers are definitely not taken."),
         clEnumValEnd));

// FIXME: add this to target options?
static cl::opt<bool>
ArgFIXCA8("fix-cortex-a8",
          cl::desc("Enable Cortex-A8 Thumb-2 branch erratum fix"),
          cl::init(false));

static cl::opt<bool>
ArgExportDynamic("export-dynamic",
                 cl::desc("Export all dynamic symbols"),
                 cl::init(false));

static cl::alias
ArgExportDynamicAlias("E",
                      cl::desc("alias for --export-dynamic"),
                      cl::aliasopt(ArgExportDynamic));

static cl::opt<std::string>
ArgEmulation("m",
             cl::ZeroOrMore,
             cl::desc("Set GNU linker emulation"),
             cl::value_desc("emulation"));

static cl::list<std::string, bool, llvm::cl::SearchDirParser>
ArgRuntimePathLink("rpath-link",
                   cl::ZeroOrMore,
                   cl::desc("Add a directory to the link time library search path"),
                   cl::value_desc("dir"));

static cl::list<std::string>
ArgExcludeLIBS("exclude-libs",
               cl::CommaSeparated,
               cl::desc("Exclude libraries from automatic export"),
               cl::value_desc("lib1,lib2,..."));

static cl::opt<std::string>
ArgBuildID("build-id",
           cl::desc("Request creation of \".note.gnu.build-id\" ELF note section."),
           cl::value_desc("style"),
           cl::ValueOptional);

static cl::opt<std::string>
ArgForceUndefined("u",
                  cl::desc("Force symbol to be undefined in the output file"),
                  cl::value_desc("symbol"));

static cl::alias
ArgForceUndefinedAlias("undefined",
                       cl::desc("alias for -u"),
                       cl::aliasopt(ArgForceUndefined));

static cl::opt<std::string>
ArgVersionScript("version-script",
                 cl::desc("Version script."),
                 cl::value_desc("Version script"));

static cl::opt<bool>
ArgWarnCommon("warn-common",
              cl::desc("warn common symbol"),
              cl::init(false));

static cl::opt<mcld::GeneralOptions::HashStyle>
ArgHashStyle("hash-style", cl::init(mcld::GeneralOptions::SystemV),
  cl::desc("Set the type of linker's hash table(s)."),
  cl::values(
       clEnumValN(mcld::GeneralOptions::SystemV, "sysv",
                 "classic ELF .hash section"),
       clEnumValN(mcld::GeneralOptions::GNU, "gnu",
                 "new style GNU .gnu.hash section"),
       clEnumValN(mcld::GeneralOptions::Both, "both",
                 "both the classic ELF and new style GNU hash tables"),
       clEnumValEnd));

static cl::opt<std::string>
ArgFilter("F",
          cl::desc("Filter for shared object symbol table"),
          cl::value_desc("name"));

static cl::alias
ArgFilterAlias("filter",
               cl::desc("alias for -F"),
               cl::aliasopt(ArgFilterAlias));

static cl::list<std::string>
ArgAuxiliary("f",
             cl::ZeroOrMore,
             cl::desc("Auxiliary filter for shared object symbol table"),
             cl::value_desc("name"));

static cl::alias
ArgAuxiliaryAlias("auxiliary",
                  cl::desc("alias for -f"),
                  cl::aliasopt(ArgAuxiliary));

static cl::opt<bool>
ArgUseGold("use-gold",
          cl::desc("GCC/collect2 compatibility: uses ld.gold.  Ignored"),
          cl::init(false));

static cl::opt<bool>
ArgUseMCLD("use-mcld",
          cl::desc("GCC/collect2 compatibility: uses ld.mcld.  Ignored"),
          cl::init(false));

static cl::opt<bool>
ArgUseLD("use-ld",
          cl::desc("GCC/collect2 compatibility: uses ld.bfd.  Ignored"),
          cl::init(false));

static cl::opt<bool>
ArgEB("EB",
      cl::desc("Link big-endian objects. This affects the default output format."),
      cl::init(false));

static cl::opt<bool>
ArgEL("EL",
      cl::desc("Link little-endian objects. This affects the default output format."),
      cl::init(false));

static cl::list<std::string>
ArgPlugin("plugin",
          cl::desc("Load a plugin library."),
          cl::value_desc("plugin"));

static cl::list<std::string>
ArgPluginOpt("plugin-opt",
             cl::desc("	Pass an option to the plugin."),
             cl::value_desc("option"));

static cl::opt<bool>
ArgSVR4Compatibility("Qy",
                    cl::desc("This option is ignored for SVR4 compatibility"),
                    cl::init(false));

static cl::list<std::string>
ArgY("Y",
     cl::desc("Add path to the default library search path"),
     cl::value_desc("default-search-path"));

/// @{
/// @name FIXME: end of unsupported options
/// @}

static cl::opt<bool>
ArgNoStdlib("nostdlib",
            cl::desc("Only search lib dirs explicitly specified on cmdline"),
            cl::init(false));

static cl::list<std::string, bool, llvm::cl::SearchDirParser>
ArgRuntimePath("rpath",
               cl::ZeroOrMore,
               cl::desc("Add a directory to the runtime library search path"),
               cl::value_desc("dir"));

static cl::alias
ArgRuntimePathAlias("R",
                    cl::desc("alias for --rpath"),
                    cl::aliasopt(ArgRuntimePath), cl::Prefix);

static cl::opt<bool>
ArgEnableNewDTags("enable-new-dtags",
                  cl::desc("Enable use of DT_RUNPATH and DT_FLAGS"),
                  cl::init(false));

static cl::opt<bool>
ArgPrintMap("M",
            cl::desc("Print a link map to the standard output."),
            cl::init(false));

static cl::alias
ArgPrintMapAlias("print-map",
                 cl::desc("alias for -M"),
                 cl::aliasopt(ArgPrintMap));

static bool ArgFatalWarnings;

static cl::opt<bool, true, cl::FalseParser>
ArgNoFatalWarnings("no-fatal-warnings",
              cl::location(ArgFatalWarnings),
              cl::desc("do not turn warnings into errors"),
              cl::init(false),
              cl::ValueDisallowed);

static cl::opt<bool, true>
ArgFatalWarningsFlag("fatal-warnings",
              cl::location(ArgFatalWarnings),
              cl::desc("turn all warnings into errors"),
              cl::init(false),
              cl::ValueDisallowed);

static cl::opt<bool>
ArgWarnSharedTextrel("warn-shared-textrel",
                     cl::desc("Warn if adding DT_TEXTREL in a shared object."),
                     cl::init(false));

namespace format {
enum Format {
  Binary,
  Unknown // decided by triple
};
} // namespace of format

static cl::opt<format::Format>
ArgFormat("b",
  cl::value_desc("Format"),
  cl::desc("set input format"),
  cl::init(format::Unknown),
  cl::values(
    clEnumValN(format::Binary, "binary",
      "read in binary machine code."),
    clEnumValEnd));

static cl::alias
ArgFormatAlias("format",
               cl::desc("alias for -b"),
               cl::aliasopt(ArgFormat));

static cl::opt<format::Format>
ArgOFormat("oformat",
  cl::value_desc("Format"),
  cl::desc("set output format"),
  cl::init(format::Unknown),
  cl::values(
    clEnumValN(format::Binary, "binary",
      "generate binary machine code."),
    clEnumValEnd));

static cl::opt<bool>
ArgDefineCommon("d",
                cl::ZeroOrMore,
                cl::desc("Define common symbol"),
                cl::init(false));

static cl::alias
ArgDefineCommonAlias1("dc",
                      cl::ZeroOrMore,
                      cl::desc("alias for -d"),
                      cl::aliasopt(ArgDefineCommon));

static cl::alias
ArgDefineCommonAlias2("dp",
                      cl::ZeroOrMore,
                      cl::desc("alias for -d"),
                      cl::aliasopt(ArgDefineCommon));

//===----------------------------------------------------------------------===//
// Scripting Options
//===----------------------------------------------------------------------===//
static cl::list<std::string>
ArgWrapList("wrap",
            cl::ZeroOrMore,
            cl::desc("Use a wrap function fo symbol."),
            cl::value_desc("symbol"));

static cl::list<std::string>
ArgPortList("portable",
            cl::ZeroOrMore,
            cl::desc("Use a portable function fo symbol."),
            cl::value_desc("symbol"));

static cl::list<std::string>
ArgAddressMapList("section-start",
                  cl::ZeroOrMore,
                  cl::desc("Locate a output section at the given absolute address"),
                  cl::value_desc("Set address of section"),
                  cl::Prefix);

static cl::list<std::string>
ArgDefSymList("defsym",
              cl::ZeroOrMore,
              cl::desc("Define a symbol"),
              cl::value_desc("symbol=expression"));

static cl::opt<unsigned long long>
ArgBssSegAddr("Tbss",
              cl::desc("Set the address of the bss segment"),
              cl::init(-1U));

static cl::opt<unsigned long long>
ArgDataSegAddr("Tdata",
               cl::desc("Set the address of the data segment"),
               cl::init(-1U));

static cl::opt<unsigned long long>
ArgTextSegAddr("Ttext",
               cl::desc("Set the address of the text segment"),
               cl::init(-1U));

//===----------------------------------------------------------------------===//
// non-member functions
//===----------------------------------------------------------------------===//
/// GetOutputStream - get the output stream.
static mcld::ToolOutputFile *GetOutputStream(const char* pTargetName,
                                             Triple::OSType pOSType,
                                             mcld::CodeGenFileType pFileType,
                                             const mcld::sys::fs::Path& pInputFilename,
                                             mcld::sys::fs::Path& pOutputFilename)
{
  if (pOutputFilename.empty()) {
    if (0 == pInputFilename.native().compare("-"))
      pOutputFilename.assign("-");
    else {
      switch(pFileType) {
      case mcld::CGFT_ASMFile: {
        if (0 == pInputFilename.native().compare("-"))
          pOutputFilename.assign("_out");
        else
          pOutputFilename.assign(pInputFilename.stem().native());

        if (0 == strcmp(pTargetName, "c"))
          pOutputFilename.native() += ".cbe.c";
        else if (0 == strcmp(pTargetName, "cpp"))
          pOutputFilename.native() += ".cpp";
        else
          pOutputFilename.native() += ".s";
      }
      break;

      case mcld::CGFT_OBJFile: {
        if (0 == pInputFilename.native().compare("-"))
          pOutputFilename.assign("_out");
        else
          pOutputFilename.assign(pInputFilename.stem().native());

        if (pOSType == Triple::Win32)
          pOutputFilename.native() += ".obj";
        else
          pOutputFilename.native() += ".o";
      }
      break;

      case mcld::CGFT_PARTIAL: {
        if (Triple::Win32 == pOSType) {
          if (0 == pInputFilename.native().compare("-"))
            pOutputFilename.assign("_out");
          else
            pOutputFilename.assign(pInputFilename.stem().native());
          pOutputFilename.native() += ".obj";
        }
        else
          pOutputFilename.assign("a.out");
      }
      break;

      case mcld::CGFT_DSOFile: {
        if (Triple::Win32 == pOSType) {
          if (0 == pInputFilename.native().compare("-"))
            pOutputFilename.assign("_out");
          else
            pOutputFilename.assign(pInputFilename.stem().native());
          pOutputFilename.native() += ".dll";
        }
        else
          pOutputFilename.assign("a.out");
      }
      break;

      case mcld::CGFT_EXEFile: {
        if (Triple::Win32 == pOSType) {
          if (0 == pInputFilename.native().compare("-"))
            pOutputFilename.assign("_out");
          else
            pOutputFilename.assign(pInputFilename.stem().native());
          pOutputFilename.native() += ".exe";
        }
        else
          pOutputFilename.assign("a.out");
      }
      break;

      case mcld::CGFT_NULLFile:
        break;
      default:
        llvm::report_fatal_error("Unknown output file type.\n");
      } // end of switch
    } // end of ! pInputFilename == "-"
  } // end of if empty pOutputFilename

  mcld::FileHandle::Permission permission;
  switch (pFileType) {
  default: assert(0 && "Unknown file type");
  case mcld::CGFT_ASMFile:
  case mcld::CGFT_OBJFile:
  case mcld::CGFT_PARTIAL:
    permission = mcld::FileHandle::Permission(0x644);
    break;
  case mcld::CGFT_DSOFile:
  case mcld::CGFT_EXEFile:
  case mcld::CGFT_BINARY:
  case mcld::CGFT_NULLFile:
    permission = mcld::FileHandle::Permission(0x755);
    break;
  }

  // Open the file.
  mcld::ToolOutputFile* result_output =
                      new mcld::ToolOutputFile(pOutputFilename,
                                                 mcld::FileHandle::ReadWrite |
                                                 mcld::FileHandle::Create |
                                                 mcld::FileHandle::Truncate,
                                               permission);

  return result_output;
}

/// ParseProgName - Parse program name
/// This function simplifies cross-compiling by reading triple from the program
/// name. For example, if the program name is `arm-linux-eabi-ld.mcld', we can
/// get the triple is arm-linux-eabi by the program name.
static std::string ParseProgName(const char *progname)
{
  static const char *suffixes[] = {
    "ld",
    "ld.mcld",
  };

  std::string ProgName(mcld::sys::fs::Path(progname).stem().native());

  for (size_t i = 0; i < sizeof(suffixes) / sizeof(suffixes[0]); ++i) {
    if (ProgName == suffixes[i])
      return std::string();
  }

  StringRef ProgNameRef(ProgName);
  StringRef Prefix;

  for (size_t i = 0; i < sizeof(suffixes) / sizeof(suffixes[0]); ++i) {
    if (!ProgNameRef.endswith(suffixes[i]))
      continue;

    StringRef::size_type LastComponent = ProgNameRef.rfind('-',
      ProgNameRef.size() - strlen(suffixes[i]));
    if (LastComponent == StringRef::npos)
      continue;
    StringRef Prefix = ProgNameRef.slice(0, LastComponent);
    std::string IgnoredError;
    if (!llvm::TargetRegistry::lookupTarget(Prefix, IgnoredError))
      continue;
    return Prefix.str();
  }
  return std::string();
}

static Triple ParseEmulation(const std::string& pEmulation)
{
  Triple result = StringSwitch<Triple>(pEmulation)
    .Case("armelf_linux_eabi", Triple("arm", "", "linux", "gnueabi"))
    .Case("elf_i386",          Triple("i386", "", "", "gnu"))
    .Case("elf_x86_64",        Triple("x86_64", "", "", "gnu"))
    .Case("elf32_x86_64",      Triple("x86_64", "", "", "gnux32"))
    .Case("elf_i386_fbsd",     Triple("i386", "", "freebsd", "gnu"))
    .Case("elf_x86_64_fbsd",   Triple("x86_64", "", "freebsd", "gnu"))
    .Case("elf32ltsmip",       Triple("mipsel", "", "", "gnu"))
    .Default(Triple());

  if (result.getArch()        == Triple::UnknownArch &&
      result.getOS()          == Triple::UnknownOS &&
      result.getEnvironment() == Triple::UnknownEnvironment)
    mcld::error(mcld::diag::err_invalid_emulation) << pEmulation << "\n";

  return result;
}

static bool ShouldColorize()
{
   const char* term = getenv("TERM");
   return term && (0 != strcmp(term, "dumb"));
}

static bool ProcessLinkerOptionsFromCommand(mcld::LinkerScript& pScript,
                                            mcld::LinkerConfig& pConfig)
{
  // -----  Set up General Options  ----- //
  // set up colorize
  switch (ArgColor) {
    case color::Never:
      pConfig.options().setColor(false);
    break;
    case color::Always:
      pConfig.options().setColor(true);
    break;
    case color::Auto:
      bool color_option = ShouldColorize() &&
                 llvm::sys::Process::FileDescriptorIsDisplayed(STDOUT_FILENO);
      pConfig.options().setColor(color_option);
    break;
  }

  mcld::outs().setColor(pConfig.options().color());
  mcld::errs().setColor(pConfig.options().color());

  // set up soname
  pConfig.options().setSOName(ArgSOName);

  // add all rpath entries
  cl::list<std::string>::iterator rp;
  cl::list<std::string>::iterator rpEnd = ArgRuntimePath.end();
  for (rp = ArgRuntimePath.begin(); rp != rpEnd; ++rp) {
    pConfig.options().getRpathList().push_back(*rp);
  }

  // --fatal-warnings
  // pConfig.options().setFatalWarnings(ArgFatalWarnings);

  // -shared or -pie
  if (true == ArgShared || true == ArgPIE) {
    ArgFileType = mcld::CGFT_DSOFile;
  }
  else if (true == ArgRelocatable) {
    ArgFileType = mcld::CGFT_PARTIAL;
  }
  else if (format::Binary == ArgOFormat) {
    ArgFileType = mcld::CGFT_BINARY;
  }

  // -b [input-format], --format=[input-format]
  if (format::Binary == ArgFormat)
    pConfig.options().setBinaryInput();

  // -V
  if (ArgVersion) {
    mcld::outs() << "MCLinker - "
                 << mcld::LinkerConfig::version()
                 << "\n";
  }

  // set up sysroot
  if (!ArgSysRoot.empty()) {
    if (exists(ArgSysRoot) && is_directory(ArgSysRoot))
      pScript.setSysroot(ArgSysRoot);
  }

  // add all search directories
  cl::list<std::string>::iterator sd;
  cl::list<std::string>::iterator sdEnd = ArgSearchDirList.end();
  for (sd=ArgSearchDirList.begin(); sd!=sdEnd; ++sd) {
    if (!pScript.directories().insert(*sd)) {
      // FIXME: need a warning function
      errs() << "WARNING: can not open search directory `-L"
             << *sd
             << "'.\n";
    }
  }

  pConfig.options().setPIE(ArgPIE);
  pConfig.options().setTrace(ArgTrace);
  pConfig.options().setVerbose(ArgVerbose);
  pConfig.options().setMaxErrorNum(ArgMaxErrorNum);
  pConfig.options().setMaxWarnNum(ArgMaxWarnNum);
  pConfig.options().setEntry(ArgEntry);
  pConfig.options().setBsymbolic(ArgBsymbolic);
  pConfig.options().setBgroup(ArgBgroup);
  pConfig.options().setDyld(ArgDyld);
  pConfig.options().setNoUndefined(ArgNoUndefined);
  pConfig.options().setMulDefs(ArgAllowMulDefs);
  pConfig.options().setEhFrameHdr(ArgEhFrameHdr);
  pConfig.options().setNMagic(ArgNMagic);
  pConfig.options().setOMagic(ArgOMagic);
  pConfig.options().setStripDebug(ArgStripDebug || ArgStripAll);
  pConfig.options().setExportDynamic(ArgExportDynamic);
  pConfig.options().setWarnSharedTextrel(ArgWarnSharedTextrel);
  pConfig.options().setDefineCommon(ArgDefineCommon);
  pConfig.options().setNewDTags(ArgEnableNewDTags);
  pConfig.options().setHashStyle(ArgHashStyle);
  pConfig.options().setNoStdlib(ArgNoStdlib);
  pConfig.options().setPrintMap(ArgPrintMap);
  pConfig.options().setGPSize(ArgGPSize);

  if (ArgStripAll)
    pConfig.options().setStripSymbols(mcld::GeneralOptions::StripAllSymbols);
  else if (ArgDiscardAll)
    pConfig.options().setStripSymbols(mcld::GeneralOptions::StripLocals);
  else if (ArgDiscardLocals)
    pConfig.options().setStripSymbols(mcld::GeneralOptions::StripTemporaries);
  else
    pConfig.options().setStripSymbols(mcld::GeneralOptions::KeepAllSymbols);

  // set up rename map, for --wrap
  cl::list<std::string>::iterator wname;
  cl::list<std::string>::iterator wnameEnd = ArgWrapList.end();
  for (wname = ArgWrapList.begin(); wname != wnameEnd; ++wname) {
    bool exist = false;

    // add wname -> __wrap_wname
    mcld::StringEntry<llvm::StringRef>* to_wrap =
                                     pScript.renameMap().insert(*wname, exist);

    std::string to_wrap_str = "__wrap_" + *wname;
    to_wrap->setValue(to_wrap_str);

    if (exist)
      mcld::warning(mcld::diag::rewrap) << *wname << to_wrap_str;

    // add __real_wname -> wname
    std::string from_real_str = "__real_" + *wname;
    mcld::StringEntry<llvm::StringRef>* from_real =
                              pScript.renameMap().insert(from_real_str, exist);
    from_real->setValue(*wname);
    if (exist)
      mcld::warning(mcld::diag::rewrap) << *wname << from_real_str;
  } // end of for

  // set up rename map, for --portable
  cl::list<std::string>::iterator pname;
  cl::list<std::string>::iterator pnameEnd = ArgPortList.end();
  for (pname = ArgPortList.begin(); pname != pnameEnd; ++pname) {
    bool exist = false;

    // add pname -> pname_portable
    mcld::StringEntry<llvm::StringRef>* to_port =
                                     pScript.renameMap().insert(*pname, exist);

    std::string to_port_str = *pname + "_portable";
    to_port->setValue(to_port_str);

    if (exist)
      mcld::warning(mcld::diag::rewrap) << *pname << to_port_str;

    // add __real_pname -> pname
    std::string from_real_str = "__real_" + *pname;
    mcld::StringEntry<llvm::StringRef>* from_real =
                              pScript.renameMap().insert(from_real_str, exist);

    from_real->setValue(*pname);
    if (exist)
      mcld::warning(mcld::diag::rewrap) << *pname << from_real_str;
  } // end of for

  // add -z options
  cl::list<mcld::ZOption>::iterator zOpt;
  cl::list<mcld::ZOption>::iterator zOptEnd = ArgZOptionList.end();
  for (zOpt = ArgZOptionList.begin(); zOpt != zOptEnd; ++zOpt) {
    pConfig.options().addZOption(*zOpt);
  }

  if (ArgGCSections) {
    mcld::warning(mcld::diag::warn_unsupported_option) << ArgGCSections.ArgStr;
  }

  // set up icf mode
  switch (ArgICF) {
    case icf::None:
      break;
    case icf::All:
    case icf::Safe:
    default:
      mcld::warning(mcld::diag::warn_unsupported_option) << ArgICF.ArgStr;
      break;
  }

  if (ArgFIXCA8) {
    mcld::warning(mcld::diag::warn_unsupported_option) << ArgFIXCA8.ArgStr;
  }

  // add address mappings
  // -Ttext
  if (-1U != ArgTextSegAddr) {
    bool exist = false;
    mcld::StringEntry<uint64_t>* text_mapping =
                                   pScript.addressMap().insert(".text", exist);
    text_mapping->setValue(ArgTextSegAddr);
  }
  // -Tdata
  if (-1U != ArgDataSegAddr) {
    bool exist = false;
    mcld::StringEntry<uint64_t>* data_mapping =
                                   pScript.addressMap().insert(".data", exist);
    data_mapping->setValue(ArgDataSegAddr);
  }
  // -Tbss
  if (-1U != ArgBssSegAddr) {
    bool exist = false;
    mcld::StringEntry<uint64_t>* bss_mapping =
                                    pScript.addressMap().insert(".bss", exist);
    bss_mapping->setValue(ArgBssSegAddr);
  }
  // --section-start SECTION=ADDRESS
  for (cl::list<std::string>::iterator
         it = ArgAddressMapList.begin(), ie = ArgAddressMapList.end();
       it != ie; ++it) {
    // FIXME: Add a cl::parser
    size_t pos = (*it).find_last_of('=');
    llvm::StringRef script(*it);
    uint64_t address = 0x0;
    script.substr(pos + 1).getAsInteger(0, address);
    bool exist = false;
    mcld::StringEntry<uint64_t>* addr_mapping =
                     pScript.addressMap().insert(script.substr(0, pos), exist);
    addr_mapping->setValue(address);
  }

  // --defsym symbols
  for (cl::list<std::string>::iterator
       it = ArgDefSymList.begin(), ie = ArgDefSymList.end();
       it != ie ; ++it) {
    llvm::StringRef expression(*it);
    size_t pos = expression.find_last_of('=');
    if (pos == expression.size() - 1) {
      errs() << "defsym option: expression must not end with '='\n";
      return false;
    }
    if (llvm::StringRef::npos == pos) {
      errs() << "syntax : --defsym symbol=expression\n";
      return false;
    }
    bool exist = false;
    // FIXME: This will not work with multiple destinations such as
    // --defsym abc=pqr=expression

    mcld::StringEntry<llvm::StringRef> *defsyms =
                    pScript.defSymMap().insert(expression.substr(0,pos),exist);
    defsyms->setValue(expression.substr(pos + 1));
  }

  // set up filter/aux filter for shared object
  pConfig.options().setFilter(ArgFilter);

  cl::list<std::string>::iterator aux;
  cl::list<std::string>::iterator auxEnd = ArgAuxiliary.end();
  for (aux = ArgAuxiliary.begin(); aux != auxEnd; ++aux)
    pConfig.options().getAuxiliaryList().push_back(*aux);

  return true;
}

int main(int argc, char* argv[])
{
  sys::PrintStackTraceOnErrorSignal();

  LLVMContext &Context = getGlobalContext();
  llvm_shutdown_obj Y;  // Call llvm_shutdown() on exit.

  // Initialize targets first, so that --version shows registered targets.
  InitializeAllTargets();
  InitializeAllAsmPrinters();
  InitializeAllAsmParsers();
  InitializeAllTargetMCs();
  mcld::InitializeAllTargets();
  mcld::InitializeAllLinkers();
  mcld::InitializeAllEmulations();
  mcld::InitializeAllDiagnostics();

  cl::ParseCommandLineOptions(argc, argv, "MCLinker\n");

#ifdef ENABLE_UNITTEST
  if (UnitTest) {
    return unit_test( argc, argv );
  }
#endif

  // Load the module to be compiled...
  std::auto_ptr<llvm::Module> M;

  // Load the module to be linked...
  mcld::LinkerScript LDScript;
  mcld::Module LDIRModule(LDScript);
  mcld::LinkerConfig LDConfig;

  // Process the linker input from the command line
  if (!ProcessLinkerOptionsFromCommand(LDScript, LDConfig)) {
    errs() << argv[0] << ": failed to process linker options from command line!\n";
    return 1;
  }

  if (ArgBitcodeFilename.empty() &&
      (mcld::CGFT_DSOFile != ArgFileType &&
       mcld::CGFT_EXEFile != ArgFileType &&
       mcld::CGFT_PARTIAL != ArgFileType &&
       mcld::CGFT_BINARY  != ArgFileType)) {
    // If the file is not given, forcefully read from stdin
    if (ArgVerbose >= 0) {
      errs() << "** The bitcode/llvm asm file is not given. Read from stdin.\n"
             << "** Specify input bitcode/llvm asm file by\n\n"
             << "          llvm-mcld -dB [the bitcode/llvm asm]\n\n";
    }

    ArgBitcodeFilename.assign("-");
  }

  if (!ArgBitcodeFilename.empty()) {
    SMDiagnostic Err;
    M.reset(ParseIRFile(ArgBitcodeFilename.native(), Err, Context));

    if (M.get() == 0) {
      Err.print(argv[0], errs());
      errs() << "** Failed to to the given bitcode/llvm asm file '"
             << ArgBitcodeFilename.native() << "'. **\n";
      return 1;
    }
  }
  else {
    // If here, output must be dynamic shared object (mcld::CGFT_DSOFile) and
    // executable file (mcld::CGFT_EXEFile).
    M.reset(new Module("Empty Module", Context));
  }
  Module &mod = *M.get();

  // If we are supposed to override the target triple, do so now.
  Triple TheTriple;
  if (!TargetTriple.empty()) {
    // 1. Use the triple from command.
    TheTriple.setTriple(TargetTriple);
    mod.setTargetTriple(TargetTriple);
  } else if (!mod.getTargetTriple().empty()) {
    // 2. Use the triple in the input Module.
    TheTriple.setTriple(mod.getTargetTriple());
  } else {
    std::string ProgNameTriple = ParseProgName(argv[0]);
    if (!ProgNameTriple.empty()) {
      // 3. Use the triple from the program name prefix.
      TheTriple.setTriple(ProgNameTriple);
      mod.setTargetTriple(ProgNameTriple);
    } else {
      // 4. Use the default target triple.
      TheTriple.setTriple(mcld::sys::getDefaultTargetTriple());
      if (!ArgEmulation.empty()) {
        // Process target emulation.
        Triple EmulationTriple = ParseEmulation(ArgEmulation);
        if (EmulationTriple.getArch() != Triple::UnknownArch)
          TheTriple.setArch(EmulationTriple.getArch());
        if (EmulationTriple.getOS() != Triple::UnknownOS)
          TheTriple.setOS(EmulationTriple.getOS());
        if (EmulationTriple.getEnvironment() != Triple::UnknownEnvironment)
          TheTriple.setEnvironment(EmulationTriple.getEnvironment());
      }
    }
  }

  // Allocate target machine.  First, check whether the user has explicitly
  // specified an architecture to compile for. If so we have to look it up by
  // name, because it might be a backend that has no mapping to a target triple.
  const mcld::Target *TheTarget = 0;
  if (!MArch.empty()) {
    for (mcld::TargetRegistry::iterator it = mcld::TargetRegistry::begin(),
           ie = mcld::TargetRegistry::end(); it != ie; ++it) {
      if (MArch == (*it)->get()->getName()) {
        TheTarget = *it;
        break;
      }
    }

    if (!TheTarget) {
      errs() << argv[0] << ": error: invalid target '" << MArch << "'.\n";
      return 1;
    }

    // Adjust the triple to match (if known), otherwise stick with the
    // module/host triple.
    Triple::ArchType Type = Triple::getArchTypeForLLVMName(MArch);
    if (Type != Triple::UnknownArch)
      TheTriple.setArch(Type);
  }
  else {
    std::string Err;
    TheTarget = mcld::TargetRegistry::lookupTarget(TheTriple.getTriple(), Err);
    if (TheTarget == 0) {
      errs() << "error: auto-selecting target `" << TheTriple.getTriple()
             << "'\n"
             << "Please use the -march option to explicitly select a target.\n"
             << "Example:\n"
             << "  $ " << argv[0] << " -march=arm\n";
      return 1;
    }
  }
  // Set up mcld::LinkerConfig
  LDConfig.targets().setTriple(TheTriple);

  // Package up features to be passed to target/subtarget
  std::string FeaturesStr;
  if (MAttrs.size()) {
    SubtargetFeatures Features;
    for (unsigned i = 0; i != MAttrs.size(); ++i)
      Features.AddFeature(MAttrs[i]);
    FeaturesStr = Features.getString();
  }

  CodeGenOpt::Level OLvl = CodeGenOpt::Default;
  switch (OptLevel) {
  default:
    errs() << argv[0] << ": invalid optimization level.\n";
    return 1;
  case ' ': break;
  case '0': OLvl = CodeGenOpt::None; break;
  case '1': OLvl = CodeGenOpt::Less; break;
  case '2': OLvl = CodeGenOpt::Default; break;
  case '3': OLvl = CodeGenOpt::Aggressive; break;
  }

  // set -fPIC
  if (ArgFPIC)
    ArgRelocModel = Reloc::PIC_;

  TargetOptions Options;
  Options.LessPreciseFPMADOption = EnableFPMAD;
  Options.NoFramePointerElim = DisableFPElim;
  Options.NoFramePointerElimNonLeaf = DisableFPElimNonLeaf;
  Options.AllowFPOpFusion = FuseFPOps;
  Options.UnsafeFPMath = EnableUnsafeFPMath;
  Options.NoInfsFPMath = EnableNoInfsFPMath;
  Options.NoNaNsFPMath = EnableNoNaNsFPMath;
  Options.HonorSignDependentRoundingFPMathOption =
      EnableHonorSignDependentRoundingFPMath;
  Options.UseSoftFloat = GenerateSoftFloatCalls;
  if (FloatABIForCalls != FloatABI::Default)
    Options.FloatABIType = FloatABIForCalls;
  Options.NoZerosInBSS = DontPlaceZerosInBSS;
  Options.JITExceptionHandling = EnableJITExceptionHandling;
  Options.JITEmitDebugInfo = EmitJitDebugInfo;
  Options.JITEmitDebugInfoToDisk = EmitJitDebugInfoToDisk;
  Options.GuaranteedTailCallOpt = EnableGuaranteedTailCallOpt;
  Options.StackAlignmentOverride = OverrideStackAlignment;
  Options.RealignStack = EnableRealignStack;
  Options.TrapFuncName = TrapFuncName;
  Options.EnableSegmentedStacks = SegmentedStacks;

  std::auto_ptr<mcld::MCLDTargetMachine> target_machine(
          TheTarget->createTargetMachine(TheTriple.getTriple(),
                                         MCPU, FeaturesStr, Options,
                                         ArgRelocModel, CMModel, OLvl));
  assert(target_machine.get() && "Could not allocate target machine!");
  mcld::MCLDTargetMachine &TheTargetMachine = *target_machine.get();

  LDConfig.targets().setTargetCPU(MCPU);
  LDConfig.targets().setTargetFeatureString(FeaturesStr);

  TheTargetMachine.getTM().setMCUseLoc(false);
  TheTargetMachine.getTM().setMCUseCFI(false);

  // FIXME: Move the initialization of LineInfo to mcld::Linker when we
  // finish LineInfo's implementation.
  OwningPtr<mcld::DiagnosticLineInfo>
    diag_line_info(TheTarget->createDiagnosticLineInfo(*TheTarget,
                                                       TheTriple.getTriple()));

  mcld::getDiagnosticEngine().setLineInfo(*diag_line_info.take());

  // Figure out where we are going to send the output...
  OwningPtr<mcld::ToolOutputFile>
  Out(GetOutputStream(TheTarget->get()->getName(),
                      TheTriple.getOS(),
                      ArgFileType,
                      ArgBitcodeFilename,
                      ArgOutputFilename));
  if (!Out) {
    // FIXME: show some error message pls.
    return 1;
  }

  // Build up all of the passes that we want to do to the module.
  PassManager PM;

  // Add the data layout from the target machine, if it exists, or the module.
  if (const DataLayout *DL = TheTargetMachine.getTM().getDataLayout())
    PM.add(new DataLayout(*DL));
   else
    PM.add(new DataLayout(&mod));

  // Override default to generate verbose assembly.
  TheTargetMachine.getTM().setAsmVerbosityDefault(true);

  {
    // Ask the target to add backend passes as necessary.
    if( TheTargetMachine.addPassesToEmitFile(PM,
                                             *Out,
                                             ArgFileType,
                                             OLvl,
                                             LDIRModule,
                                             LDConfig,
                                             NoVerify)) {
      errs() << argv[0] << ": target does not support generation of this"
             << " file type!\n";
      return 1;
    }

    // Before executing passes, print the final values of the LLVM options.
    cl::PrintOptionValues();

    PM.run(mod);
  }

  if (mcld::getDiagnosticEngine().getPrinter()->getNumErrors())
    return 1;

  // Declare success.
  Out->keep();
  return 0;
}
