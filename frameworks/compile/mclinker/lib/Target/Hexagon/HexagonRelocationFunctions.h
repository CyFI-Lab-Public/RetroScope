//===- HexagonRelocationFunction.h ----------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
typedef struct {
	const char *insnSyntax;
	uint32_t insnMask;
	uint32_t insnCmpMask;
	uint32_t insnBitMask;
        bool isDuplex;
} Instruction;

//===--------------------------------------------------------------------===//
// Relocation helper function
//===--------------------------------------------------------------------===//
template<typename T1, typename T2>
T1 ApplyMask(T2 pMask, T1 pData) {
  T1 result = 0;
  size_t off = 0;

  for (size_t bit = 0; bit != sizeof (T1) * 8; ++bit) {
    const bool valBit = (pData >> off) & 1;
    const bool maskBit = (pMask >> bit) & 1;
    if (maskBit) {
      result |= static_cast<T1>(valBit) << bit;
      ++off;
    }
  }
  return result;
}

#define DECL_HEXAGON_APPLY_RELOC_FUNC(Name) \
static HexagonRelocator::Result Name    (Relocation& pEntry, \
                                     HexagonRelocator& pParent);

#define DECL_HEXAGON_APPLY_RELOC_FUNCS \
DECL_HEXAGON_APPLY_RELOC_FUNC(none)             \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocB22PCREL)     \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocB15PCREL)     \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocB7PCREL)      \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocLO16)         \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocHI16)        \
DECL_HEXAGON_APPLY_RELOC_FUNC(reloc32)         \
DECL_HEXAGON_APPLY_RELOC_FUNC(reloc16)         \
DECL_HEXAGON_APPLY_RELOC_FUNC(reloc8)         \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocGPREL16_0)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocGPREL16_1)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocGPREL16_2)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocGPREL16_3)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocB13PCREL)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocB9PCREL)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocB32PCRELX)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(reloc32_6_X)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocB22PCRELX)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocB15PCRELX)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocB13PCRELX)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocB9PCRELX)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocB7PCRELX)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(reloc32PCREL)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocHexNX)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocHexGOTRELLO16)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocHexGOTRELHI16)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocHexGOTREL32)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocPLTB22PCREL)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocHex6PCRELX)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocHexGOT326X)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(relocHexGOT1611X)  \
DECL_HEXAGON_APPLY_RELOC_FUNC(unsupport)


#define DECL_HEXAGON_APPLY_RELOC_FUNC_PTRS \
  { &none,                0, "R_HEX_NONE"                        }, \
  { &relocB22PCREL,       1, "R_HEX_B22_PCREL"                   }, \
  { &relocB15PCREL,       2, "R_HEX_B15_PCREL"                   }, \
  { &relocB7PCREL,        3, "R_HEX_B7_PCREL"                    }, \
  { &relocLO16,           4, "R_HEX_LO16"                        }, \
  { &relocHI16,           5, "R_HEX_HI16"                        }, \
  { &reloc32,             6, "R_HEX_32"                          }, \
  { &reloc16,             7, "R_HEX_16"                          }, \
  { &reloc8,              8, "R_HEX_8"                           }, \
  { &relocGPREL16_0,      9, "R_HEX_GPREL16_0"                   }, \
  { &relocGPREL16_1,      10, "R_HEX_GPREL16_1"                  }, \
  { &relocGPREL16_2,      11, "R_HEX_GPREL16_2"                  }, \
  { &relocGPREL16_3,      12, "R_HEX_GPREL16_3"                  }, \
  { &unsupport,           13, "R_HEX_HL16"                       }, \
  { &relocB13PCREL,       14, "R_HEX_B13_PCREL"                  }, \
  { &relocB9PCREL,        15, "R_HEX_B9_PCREL"                   }, \
  { &relocB32PCRELX,      16, "R_HEX_B32_PCREL_X"                }, \
  { &reloc32_6_X,         17, "R_HEX_32_6_X"                     }, \
  { &relocB22PCRELX,      18, "R_HEX_B22_PCREL_X"                }, \
  { &relocB15PCRELX,      19, "R_HEX_B15_PCREL_X"                }, \
  { &relocB13PCRELX,      20, "R_HEX_B13_PCREL_X"                }, \
  { &relocB9PCRELX,       21, "R_HEX_B9_PCREL_X"                 }, \
  { &relocB7PCRELX,       22, "R_HEX_B7_PCREL_X"                 }, \
  { &relocHexNX,          23, "R_HEX_16_X"                       }, \
  { &relocHexNX,          24, "R_HEX_12_X"                       }, \
  { &relocHexNX,          25, "R_HEX_11_X"                       }, \
  { &relocHexNX,          26, "R_HEX_10_X"                       }, \
  { &relocHexNX,          27, "R_HEX_9_X"                        }, \
  { &relocHexNX,          28, "R_HEX_8_X"                        }, \
  { &relocHexNX,          29, "R_HEX_7_X"                        }, \
  { &relocHexNX,          30, "R_HEX_6_X"                        }, \
  { &reloc32PCREL,        31, "R_HEX_32_PCREL"                   }, \
  { &unsupport,           32, "R_HEX_COPY"                       }, \
  { &unsupport,           33, "R_HEX_GLOB_DAT"                   }, \
  { &unsupport,           34, "R_HEX_JMP_SLOT"                   }, \
  { &unsupport,           35, "R_HEX_RELATIVE"                   }, \
  { &relocPLTB22PCREL,    36, "R_HEX_PLT_B22_PCREL"              }, \
  { &relocHexGOTRELLO16,  37, "R_HEX_GOTREL_LO16"                }, \
  { &relocHexGOTRELHI16,  38, "R_HEX_GOTREL_HI16"                }, \
  { &relocHexGOTREL32,    39, "R_HEX_GOTREL_32"                  }, \
  { &unsupport,           40, "R_HEX_GOT_LO16"                   }, \
  { &unsupport,           41, "R_HEX_GOT_HI16"                   }, \
  { &unsupport,           42, "R_HEX_GOT_32"                     }, \
  { &unsupport,           43, "R_HEX_GOT_16"                     }, \
  { &unsupport,           44, "R_HEX_DTPMOD_32"                  }, \
  { &unsupport,           45, "R_HEX_DTPREL_LO16"                }, \
  { &unsupport,           46, "R_HEX_DTPREL_HI16"                }, \
  { &unsupport,           47, "R_HEX_DTPREL_32"                  }, \
  { &unsupport,           48, "R_HEX_DTPREL_16"                  }, \
  { &unsupport,           49, "R_HEX_GD_PLT_B22_PCREL"           }, \
  { &unsupport,           50, "R_HEX_GD_GOT_LO16"                }, \
  { &unsupport,           51, "R_HEX_GD_GOT_HI16"                }, \
  { &unsupport,           52, "R_HEX_GD_GOT_32"                  }, \
  { &unsupport,           53, "R_HEX_GD_GOT_16"                  }, \
  { &unsupport,           54, "R_HEX_IE_LO16"                    }, \
  { &unsupport,           55, "R_HEX_IE_HI16"                    }, \
  { &unsupport,           56, "R_HEX_IE_32"                      }, \
  { &unsupport,           57, "R_HEX_IE_GOT_LO16"                }, \
  { &unsupport,           58, "R_HEX_IE_GOT_HI16"                }, \
  { &unsupport,           59, "R_HEX_IE_GOT_32"                  }, \
  { &unsupport,           60, "R_HEX_IE_GOT_16"                  }, \
  { &unsupport,           61, "R_HEX_TPREL_LO16"                 }, \
  { &unsupport,           62, "R_HEX_TPREL_HI16"                 }, \
  { &unsupport,           63, "R_HEX_TPREL_32"                   }, \
  { &unsupport,           64, "R_HEX_TPREL_16"                   }, \
  { &relocHex6PCRELX,     65, "R_HEX_6_PCREL_X"                  }, \
  { &unsupport,           66, "R_HEX_GOTREL_32_6_X"              }, \
  { &unsupport,           67, "R_HEX_GOTREL_16_X"                }, \
  { &unsupport,           68, "R_HEX_GOTREL_11_X"                }, \
  { &relocHexGOT326X,     69, "R_HEX_GOT_32_6_X"                 }, \
  { &relocHexGOT1611X,    70, "R_HEX_GOT_16_X"                   }, \
  { &relocHexGOT1611X,    71, "R_HEX_GOT_11_X"                   }, \
  { &unsupport,           72, "R_HEX_DTPREL_32_6_X"              }, \
  { &unsupport,           73, "R_HEX_DTPREL_16_X"                }, \
  { &unsupport,           74, "R_HEX_DTPREL_11_X"                }, \
  { &unsupport,           75, "R_HEX_GD_GOT_32_6_X"              }, \
  { &unsupport,           76, "R_HEX_GD_GOT_16_X"                }, \
  { &unsupport,           77, "R_HEX_GD_GOT_11_X"                }, \
  { &unsupport,           78, "R_HEX_IE_32_6_X"                  }, \
  { &unsupport,           79, "R_HEX_IE_16_X"                    }, \
  { &unsupport,           80, "R_HEX_IE_GOT_32_6_X"              }, \
  { &unsupport,           81, "R_HEX_IE_GOT_16_X"                }, \
  { &unsupport,           82, "R_HEX_IE_GOT_11_X"                }, \
  { &unsupport,           83, "R_HEX_TPREL_32_6_X"               }, \
  { &unsupport,           84, "R_HEX_TPREL_16_X"                 }, \
  { &unsupport,           85, "R_HEX_TPREL_11_X"                 }
