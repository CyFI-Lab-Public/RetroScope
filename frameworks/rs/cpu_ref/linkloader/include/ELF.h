#ifndef ELF_H
#define ELF_H

#include <llvm/Support/ELF.h>
// FIXME: Can't using namespace in header file!
using namespace llvm::ELF;

// These definitions are not defined in include/llvm/Support/ELF.h.
// So we define them here.

#ifndef ET_LOOS
#define ET_LOOS 0xfe00
#endif

#ifndef ET_HIOS
#define ET_HIOS 0xfeff
#endif

#endif // ELF_H
