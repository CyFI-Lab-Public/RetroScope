/* A Bison parser, made by GNU Bison 2.3.  */

/* Skeleton implementation for Bison's Yacc-like parsers in C

   Copyright (C) 1984, 1989, 1990, 2000, 2001, 2002, 2003, 2004, 2005, 2006
   Free Software Foundation, Inc.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2, or (at your option)
   any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor,
   Boston, MA 02110-1301, USA.  */

/* As a special exception, you may create a larger work that contains
   part or all of the Bison parser skeleton and distribute that work
   under terms of your choice, so long as that work isn't itself a
   parser generator using the skeleton or a modified version thereof
   as a parser skeleton.  Alternatively, if you modify or redistribute
   the parser skeleton itself, you may (at your option) remove this
   special exception, which will cause the skeleton and the resulting
   Bison output files to be licensed under the GNU General Public
   License without this special exception.

   This special exception was added by the Free Software Foundation in
   version 2.2 of Bison.  */

/* C LALR(1) parser skeleton written by Richard Stallman, by
   simplifying the original so-called "semantic" parser.  */

/* All symbols defined below should begin with yy or YY, to avoid
   infringing on user name space.  This should be done even for local
   variables, as they might otherwise be expanded by user macros.
   There are some unavoidable exceptions within include files to
   define necessary library symbols; they are noted "INFRINGES ON
   USER NAME SPACE" below.  */

/* Identify Bison output.  */
#define YYBISON 1

/* Bison version.  */
#define YYBISON_VERSION "2.3"

/* Skeleton name.  */
#define YYSKELETON_NAME "yacc.c"

/* Pure parsers.  */
#define YYPURE 0

/* Using locations.  */
#define YYLSP_NEEDED 0

/* Substitute the variable and function names.  */
#define yyparse ldparse
#define yylex   ldlex
#define yyerror lderror
#define yylval  ldlval
#define yychar  ldchar
#define yydebug lddebug
#define yynerrs ldnerrs


/* Tokens.  */
#ifndef YYTOKENTYPE
# define YYTOKENTYPE
   /* Put the tokens into the symbol table, so that GDB and other debuggers
      know about them.  */
   enum yytokentype {
     kADD_OP = 258,
     kALIGN = 259,
     kAS_NEEDED = 260,
     kENTRY = 261,
     kEXCLUDE_FILE = 262,
     kFILENAME = 263,
     kGLOBAL = 264,
     kGROUP = 265,
     kID = 266,
     kINPUT = 267,
     kINTERP = 268,
     kKEEP = 269,
     kLOCAL = 270,
     kMODE = 271,
     kMUL_OP = 272,
     kNUM = 273,
     kOUTPUT_FORMAT = 274,
     kPAGESIZE = 275,
     kPROVIDE = 276,
     kSEARCH_DIR = 277,
     kSEGMENT = 278,
     kSIZEOF_HEADERS = 279,
     kSORT = 280,
     kVERSION = 281,
     kVERSION_SCRIPT = 282,
     ADD_OP = 283,
     MUL_OP = 284
   };
#endif
/* Tokens.  */
#define kADD_OP 258
#define kALIGN 259
#define kAS_NEEDED 260
#define kENTRY 261
#define kEXCLUDE_FILE 262
#define kFILENAME 263
#define kGLOBAL 264
#define kGROUP 265
#define kID 266
#define kINPUT 267
#define kINTERP 268
#define kKEEP 269
#define kLOCAL 270
#define kMODE 271
#define kMUL_OP 272
#define kNUM 273
#define kOUTPUT_FORMAT 274
#define kPAGESIZE 275
#define kPROVIDE 276
#define kSEARCH_DIR 277
#define kSEGMENT 278
#define kSIZEOF_HEADERS 279
#define kSORT 280
#define kVERSION 281
#define kVERSION_SCRIPT 282
#define ADD_OP 283
#define MUL_OP 284




/* Copy the first part of user declarations.  */
#line 1 "ldscript.y"

/* Parser for linker scripts.
   Copyright (C) 2001, 2002, 2003, 2004, 2005, 2006 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2001.

   Red Hat elfutils is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by the
   Free Software Foundation; version 2 of the License.

   Red Hat elfutils is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with Red Hat elfutils; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301 USA.

   Red Hat elfutils is an included package of the Open Invention Network.
   An included package of the Open Invention Network is a package for which
   Open Invention Network licensees cross-license their patents.  No patent
   license is granted, either expressly or impliedly, by designation as an
   included package.  Should you wish to participate in the Open Invention
   Network licensing program, please visit www.openinventionnetwork.com
   <http://www.openinventionnetwork.com>.  */

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include <assert.h>
#include <error.h>
#include <libintl.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <system.h>
#include <ld.h>

/* The error handler.  */
static void yyerror (const char *s);

/* Some helper functions we need to construct the data structures
   describing information from the file.  */
static struct expression *new_expr (int tag);
static struct input_section_name *new_input_section_name (const char *name,
							  bool sort_flag);
static struct input_rule *new_input_rule (int tag);
static struct output_rule *new_output_rule (int tag);
static struct assignment *new_assignment (const char *variable,
					  struct expression *expression,
					  bool provide_flag);
static void new_segment (int mode, struct output_rule *output_rule);
static struct filename_list *new_filename_listelem (const char *string);
static void add_inputfiles (struct filename_list *fnames);
static struct id_list *new_id_listelem (const char *str);
 static struct filename_list *mark_as_needed (struct filename_list *listp);
static struct version *new_version (struct id_list *local,
				    struct id_list *global);
static struct version *merge_versions (struct version *one,
				       struct version *two);
static void add_versions (struct version *versions);

extern int yylex (void);


/* Enabling traces.  */
#ifndef YYDEBUG
# define YYDEBUG 0
#endif

/* Enabling verbose error messages.  */
#ifdef YYERROR_VERBOSE
# undef YYERROR_VERBOSE
# define YYERROR_VERBOSE 1
#else
# define YYERROR_VERBOSE 0
#endif

/* Enabling the token table.  */
#ifndef YYTOKEN_TABLE
# define YYTOKEN_TABLE 0
#endif

#if ! defined YYSTYPE && ! defined YYSTYPE_IS_DECLARED
typedef union YYSTYPE
#line 71 "ldscript.y"
{
  uintmax_t num;
  enum expression_tag op;
  char *str;
  struct expression *expr;
  struct input_section_name *sectionname;
  struct filemask_section_name *filemask_section_name;
  struct input_rule *input_rule;
  struct output_rule *output_rule;
  struct assignment *assignment;
  struct filename_list *filename_list;
  struct version *version;
  struct id_list *id_list;
}
/* Line 187 of yacc.c.  */
#line 247 "ldscript.c"
	YYSTYPE;
# define yystype YYSTYPE /* obsolescent; will be withdrawn */
# define YYSTYPE_IS_DECLARED 1
# define YYSTYPE_IS_TRIVIAL 1
#endif



/* Copy the second part of user declarations.  */


/* Line 216 of yacc.c.  */
#line 260 "ldscript.c"

#ifdef short
# undef short
#endif

#ifdef YYTYPE_UINT8
typedef YYTYPE_UINT8 yytype_uint8;
#else
typedef unsigned char yytype_uint8;
#endif

#ifdef YYTYPE_INT8
typedef YYTYPE_INT8 yytype_int8;
#elif (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
typedef signed char yytype_int8;
#else
typedef short int yytype_int8;
#endif

#ifdef YYTYPE_UINT16
typedef YYTYPE_UINT16 yytype_uint16;
#else
typedef unsigned short int yytype_uint16;
#endif

#ifdef YYTYPE_INT16
typedef YYTYPE_INT16 yytype_int16;
#else
typedef short int yytype_int16;
#endif

#ifndef YYSIZE_T
# ifdef __SIZE_TYPE__
#  define YYSIZE_T __SIZE_TYPE__
# elif defined size_t
#  define YYSIZE_T size_t
# elif ! defined YYSIZE_T && (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
#  include <stddef.h> /* INFRINGES ON USER NAME SPACE */
#  define YYSIZE_T size_t
# else
#  define YYSIZE_T unsigned int
# endif
#endif

#define YYSIZE_MAXIMUM ((YYSIZE_T) -1)

#ifndef YY_
# if YYENABLE_NLS
#  if ENABLE_NLS
#   include <libintl.h> /* INFRINGES ON USER NAME SPACE */
#   define YY_(msgid) dgettext ("bison-runtime", msgid)
#  endif
# endif
# ifndef YY_
#  define YY_(msgid) msgid
# endif
#endif

/* Suppress unused-variable warnings by "using" E.  */
#if ! defined lint || defined __GNUC__
# define YYUSE(e) ((void) (e))
#else
# define YYUSE(e) /* empty */
#endif

/* Identity function, used to suppress warnings about constant conditions.  */
#ifndef lint
# define YYID(n) (n)
#else
#if (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
static int
YYID (int i)
#else
static int
YYID (i)
    int i;
#endif
{
  return i;
}
#endif

#if ! defined yyoverflow || YYERROR_VERBOSE

/* The parser invokes alloca or malloc; define the necessary symbols.  */

# ifdef YYSTACK_USE_ALLOCA
#  if YYSTACK_USE_ALLOCA
#   ifdef __GNUC__
#    define YYSTACK_ALLOC __builtin_alloca
#   elif defined __BUILTIN_VA_ARG_INCR
#    include <alloca.h> /* INFRINGES ON USER NAME SPACE */
#   elif defined _AIX
#    define YYSTACK_ALLOC __alloca
#   elif defined _MSC_VER
#    include <malloc.h> /* INFRINGES ON USER NAME SPACE */
#    define alloca _alloca
#   else
#    define YYSTACK_ALLOC alloca
#    if ! defined _ALLOCA_H && ! defined _STDLIB_H && (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
#     include <stdlib.h> /* INFRINGES ON USER NAME SPACE */
#     ifndef _STDLIB_H
#      define _STDLIB_H 1
#     endif
#    endif
#   endif
#  endif
# endif

# ifdef YYSTACK_ALLOC
   /* Pacify GCC's `empty if-body' warning.  */
#  define YYSTACK_FREE(Ptr) do { /* empty */; } while (YYID (0))
#  ifndef YYSTACK_ALLOC_MAXIMUM
    /* The OS might guarantee only one guard page at the bottom of the stack,
       and a page size can be as small as 4096 bytes.  So we cannot safely
       invoke alloca (N) if N exceeds 4096.  Use a slightly smaller number
       to allow for a few compiler-allocated temporary stack slots.  */
#   define YYSTACK_ALLOC_MAXIMUM 4032 /* reasonable circa 2006 */
#  endif
# else
#  define YYSTACK_ALLOC YYMALLOC
#  define YYSTACK_FREE YYFREE
#  ifndef YYSTACK_ALLOC_MAXIMUM
#   define YYSTACK_ALLOC_MAXIMUM YYSIZE_MAXIMUM
#  endif
#  if (defined __cplusplus && ! defined _STDLIB_H \
       && ! ((defined YYMALLOC || defined malloc) \
	     && (defined YYFREE || defined free)))
#   include <stdlib.h> /* INFRINGES ON USER NAME SPACE */
#   ifndef _STDLIB_H
#    define _STDLIB_H 1
#   endif
#  endif
#  ifndef YYMALLOC
#   define YYMALLOC malloc
#   if ! defined malloc && ! defined _STDLIB_H && (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
void *malloc (YYSIZE_T); /* INFRINGES ON USER NAME SPACE */
#   endif
#  endif
#  ifndef YYFREE
#   define YYFREE free
#   if ! defined free && ! defined _STDLIB_H && (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
void free (void *); /* INFRINGES ON USER NAME SPACE */
#   endif
#  endif
# endif
#endif /* ! defined yyoverflow || YYERROR_VERBOSE */


#if (! defined yyoverflow \
     && (! defined __cplusplus \
	 || (defined YYSTYPE_IS_TRIVIAL && YYSTYPE_IS_TRIVIAL)))

/* A type that is properly aligned for any stack member.  */
union yyalloc
{
  yytype_int16 yyss;
  YYSTYPE yyvs;
  };

/* The size of the maximum gap between one aligned stack and the next.  */
# define YYSTACK_GAP_MAXIMUM (sizeof (union yyalloc) - 1)

/* The size of an array large to enough to hold all stacks, each with
   N elements.  */
# define YYSTACK_BYTES(N) \
     ((N) * (sizeof (yytype_int16) + sizeof (YYSTYPE)) \
      + YYSTACK_GAP_MAXIMUM)

/* Copy COUNT objects from FROM to TO.  The source and destination do
   not overlap.  */
# ifndef YYCOPY
#  if defined __GNUC__ && 1 < __GNUC__
#   define YYCOPY(To, From, Count) \
      __builtin_memcpy (To, From, (Count) * sizeof (*(From)))
#  else
#   define YYCOPY(To, From, Count)		\
      do					\
	{					\
	  YYSIZE_T yyi;				\
	  for (yyi = 0; yyi < (Count); yyi++)	\
	    (To)[yyi] = (From)[yyi];		\
	}					\
      while (YYID (0))
#  endif
# endif

/* Relocate STACK from its old location to the new one.  The
   local variables YYSIZE and YYSTACKSIZE give the old and new number of
   elements in the stack, and YYPTR gives the new location of the
   stack.  Advance YYPTR to a properly aligned location for the next
   stack.  */
# define YYSTACK_RELOCATE(Stack)					\
    do									\
      {									\
	YYSIZE_T yynewbytes;						\
	YYCOPY (&yyptr->Stack, Stack, yysize);				\
	Stack = &yyptr->Stack;						\
	yynewbytes = yystacksize * sizeof (*Stack) + YYSTACK_GAP_MAXIMUM; \
	yyptr += yynewbytes / sizeof (*yyptr);				\
      }									\
    while (YYID (0))

#endif

/* YYFINAL -- State number of the termination state.  */
#define YYFINAL  32
/* YYLAST -- Last index in YYTABLE.  */
#define YYLAST   226

/* YYNTOKENS -- Number of terminals.  */
#define YYNTOKENS  40
/* YYNNTS -- Number of nonterminals.  */
#define YYNNTS  23
/* YYNRULES -- Number of rules.  */
#define YYNRULES  66
/* YYNRULES -- Number of states.  */
#define YYNSTATES  159

/* YYTRANSLATE(YYLEX) -- Bison symbol number corresponding to YYLEX.  */
#define YYUNDEFTOK  2
#define YYMAXUTOK   284

#define YYTRANSLATE(YYX)						\
  ((unsigned int) (YYX) <= YYMAXUTOK ? yytranslate[YYX] : YYUNDEFTOK)

/* YYTRANSLATE[YYLEX] -- Bison symbol number corresponding to YYLEX.  */
static const yytype_uint8 yytranslate[] =
{
       0,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,    29,     2,
      33,    34,    31,     2,    39,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,    35,
       2,    38,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,    36,    28,    37,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     1,     2,     3,     4,
       5,     6,     7,     8,     9,    10,    11,    12,    13,    14,
      15,    16,    17,    18,    19,    20,    21,    22,    23,    24,
      25,    26,    27,    30,    32
};

#if YYDEBUG
/* YYPRHS[YYN] -- Index of the first RHS symbol of rule number YYN in
   YYRHS.  */
static const yytype_uint8 yyprhs[] =
{
       0,     0,     3,     5,     8,    11,    13,    19,    25,    31,
      37,    43,    49,    54,    59,    64,    69,    74,    77,    79,
      82,    87,    90,    94,   101,   104,   106,   108,   113,   116,
     122,   124,   129,   134,   135,   140,   144,   148,   152,   156,
     160,   164,   166,   168,   170,   172,   176,   178,   180,   181,
     186,   191,   193,   196,   198,   203,   209,   216,   219,   221,
     224,   227,   231,   234,   236,   238,   240
};

/* YYRHS -- A `-1'-separated list of the rules' RHS.  */
static const yytype_int8 yyrhs[] =
{
      41,     0,    -1,    42,    -1,    27,    56,    -1,    42,    43,
      -1,    43,    -1,     6,    33,    11,    34,    35,    -1,    22,
      33,    61,    34,    35,    -1,    20,    33,    18,    34,    35,
      -1,    13,    33,    61,    34,    35,    -1,    23,    16,    36,
      44,    37,    -1,    23,     1,    36,    44,    37,    -1,    10,
      33,    53,    34,    -1,    12,    33,    53,    34,    -1,     5,
      33,    53,    34,    -1,    26,    36,    56,    37,    -1,    19,
      33,    61,    34,    -1,    44,    45,    -1,    45,    -1,    46,
      35,    -1,    11,    36,    47,    37,    -1,    11,    35,    -1,
      11,    38,    52,    -1,    21,    33,    11,    38,    52,    34,
      -1,    47,    48,    -1,    48,    -1,    49,    -1,    14,    33,
      49,    34,    -1,    46,    35,    -1,    62,    33,    51,    50,
      34,    -1,    11,    -1,    25,    33,    11,    34,    -1,     7,
      33,    61,    34,    -1,    -1,     4,    33,    52,    34,    -1,
      33,    52,    34,    -1,    52,    31,    52,    -1,    52,    17,
      52,    -1,    52,     3,    52,    -1,    52,    29,    52,    -1,
      52,    28,    52,    -1,    18,    -1,    11,    -1,    24,    -1,
      20,    -1,    53,    54,    55,    -1,    55,    -1,    39,    -1,
      -1,    10,    33,    53,    34,    -1,     5,    33,    53,    34,
      -1,    61,    -1,    56,    57,    -1,    57,    -1,    36,    58,
      37,    35,    -1,    61,    36,    58,    37,    35,    -1,    61,
      36,    58,    37,    61,    35,    -1,    58,    59,    -1,    59,
      -1,     9,    60,    -1,    15,    60,    -1,    60,    62,    35,
      -1,    62,    35,    -1,     8,    -1,    11,    -1,    61,    -1,
      31,    -1
};

/* YYRLINE[YYN] -- source line where rule number YYN was defined.  */
static const yytype_uint16 yyrline[] =
{
       0,   143,   143,   144,   148,   149,   152,   157,   161,   166,
     172,   176,   182,   193,   195,   197,   199,   203,   208,   212,
     217,   229,   253,   255,   259,   264,   268,   273,   280,   287,
     298,   300,   304,   307,   310,   315,   317,   323,   329,   335,
     341,   347,   352,   357,   359,   363,   368,   372,   373,   376,
     387,   389,   394,   399,   403,   409,   415,   424,   426,   430,
     432,   437,   443,   447,   449,   453,   455
};
#endif

#if YYDEBUG || YYERROR_VERBOSE || YYTOKEN_TABLE
/* YYTNAME[SYMBOL-NUM] -- String name of the symbol SYMBOL-NUM.
   First, the terminals, then, starting at YYNTOKENS, nonterminals.  */
static const char *const yytname[] =
{
  "$end", "error", "$undefined", "kADD_OP", "kALIGN", "kAS_NEEDED",
  "kENTRY", "kEXCLUDE_FILE", "kFILENAME", "kGLOBAL", "kGROUP", "kID",
  "kINPUT", "kINTERP", "kKEEP", "kLOCAL", "kMODE", "kMUL_OP", "kNUM",
  "kOUTPUT_FORMAT", "kPAGESIZE", "kPROVIDE", "kSEARCH_DIR", "kSEGMENT",
  "kSIZEOF_HEADERS", "kSORT", "kVERSION", "kVERSION_SCRIPT", "'|'", "'&'",
  "ADD_OP", "'*'", "MUL_OP", "'('", "')'", "';'", "'{'", "'}'", "'='",
  "','", "$accept", "script_or_version", "file", "content",
  "outputsections", "outputsection", "assignment", "inputsections",
  "inputsection", "sectionname", "sort_opt_name", "exclude_opt", "expr",
  "filename_id_list", "comma_opt", "filename_id_listelem", "versionlist",
  "version", "version_stmt_list", "version_stmt", "filename_id_star_list",
  "filename_id", "filename_id_star", 0
};
#endif

# ifdef YYPRINT
/* YYTOKNUM[YYLEX-NUM] -- Internal token number corresponding to
   token YYLEX-NUM.  */
static const yytype_uint16 yytoknum[] =
{
       0,   256,   257,   258,   259,   260,   261,   262,   263,   264,
     265,   266,   267,   268,   269,   270,   271,   272,   273,   274,
     275,   276,   277,   278,   279,   280,   281,   282,   124,    38,
     283,    42,   284,    40,    41,    59,   123,   125,    61,    44
};
# endif

/* YYR1[YYN] -- Symbol number of symbol that rule YYN derives.  */
static const yytype_uint8 yyr1[] =
{
       0,    40,    41,    41,    42,    42,    43,    43,    43,    43,
      43,    43,    43,    43,    43,    43,    43,    44,    44,    45,
      45,    45,    46,    46,    47,    47,    48,    48,    48,    49,
      50,    50,    51,    51,    52,    52,    52,    52,    52,    52,
      52,    52,    52,    52,    52,    53,    53,    54,    54,    55,
      55,    55,    56,    56,    57,    57,    57,    58,    58,    59,
      59,    60,    60,    61,    61,    62,    62
};

/* YYR2[YYN] -- Number of symbols composing right hand side of rule YYN.  */
static const yytype_uint8 yyr2[] =
{
       0,     2,     1,     2,     2,     1,     5,     5,     5,     5,
       5,     5,     4,     4,     4,     4,     4,     2,     1,     2,
       4,     2,     3,     6,     2,     1,     1,     4,     2,     5,
       1,     4,     4,     0,     4,     3,     3,     3,     3,     3,
       3,     1,     1,     1,     1,     3,     1,     1,     0,     4,
       4,     1,     2,     1,     4,     5,     6,     2,     1,     2,
       2,     3,     2,     1,     1,     1,     1
};

/* YYDEFACT[STATE-NAME] -- Default rule to reduce with in state
   STATE-NUM when YYTABLE doesn't specify something else to do.  Zero
   means the default is an error.  */
static const yytype_uint8 yydefact[] =
{
       0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
       0,     0,     0,     2,     5,     0,     0,     0,     0,     0,
       0,     0,     0,     0,     0,     0,    63,    64,     0,     3,
      53,     0,     1,     4,     0,     0,    48,    46,    51,     0,
      48,    48,     0,     0,     0,     0,     0,     0,     0,     0,
       0,     0,    58,    52,     0,     0,     0,    14,    47,     0,
       0,    12,    13,     0,    16,     0,     0,     0,     0,     0,
      18,     0,     0,    15,    66,    59,    65,     0,    60,     0,
      57,     0,    48,    48,    45,     6,     9,     8,     7,    21,
       0,     0,     0,    11,    17,    19,    10,     0,    62,    54,
       0,    50,    49,    64,     0,     0,     0,    25,    26,     0,
       0,    42,    41,    44,    43,     0,    22,     0,    61,    55,
       0,     0,    28,    20,    24,    33,     0,     0,     0,     0,
       0,     0,     0,     0,    56,     0,     0,     0,     0,    35,
      38,    37,    40,    39,    36,     0,    27,     0,    30,     0,
       0,    34,    23,     0,     0,    29,    32,     0,    31
};

/* YYDEFGOTO[NTERM-NUM].  */
static const yytype_int16 yydefgoto[] =
{
      -1,    12,    13,    14,    69,    70,    71,   106,   107,   108,
     150,   137,   116,    36,    59,    37,    29,    30,    51,    52,
      75,    76,   109
};

/* YYPACT[STATE-NUM] -- Index in YYTABLE of the portion describing
   STATE-NUM.  */
#define YYPACT_NINF -86
static const yytype_int16 yypact[] =
{
     111,   -18,   -14,    23,    45,    70,    75,    85,    92,    97,
      91,    19,   128,   134,   -86,   162,    96,   162,   162,     5,
       5,   123,     5,    93,    99,    19,   -86,   -86,   117,    19,
     -86,   115,   -86,   -86,   125,   144,    71,   -86,   -86,   145,
     116,   135,   147,   148,   149,   150,   101,   101,    14,    83,
      83,    55,   -86,   -86,   117,   162,   162,   -86,   -86,   162,
     133,   -86,   -86,   143,   -86,   151,   152,   107,   155,    63,
     -86,   154,    74,   -86,   -86,    83,   -86,   156,    83,   157,
     -86,    56,   137,   141,   -86,   -86,   -86,   -86,   -86,   -86,
      88,    48,   174,   -86,   -86,   -86,   -86,   158,   -86,   -86,
      69,   -86,   -86,   159,   161,   160,    12,   -86,   -86,   163,
     165,   -86,   -86,   -86,   -86,    48,    59,   164,   -86,   -86,
     166,    83,   -86,   -86,   -86,   183,    48,     0,    48,    48,
      48,    48,    48,    48,   -86,   169,   167,    90,     7,   -86,
      59,    59,    44,    66,   103,    29,   -86,     5,   -86,   171,
     172,   -86,   -86,   173,   188,   -86,   -86,   175,   -86
};

/* YYPGOTO[NTERM-NUM].  */
static const yytype_int16 yypgoto[] =
{
     -86,   -86,   -86,   192,   168,    80,   -85,   -86,   102,    89,
     -86,   -86,    33,   -16,   -86,   153,   186,    38,   170,   -39,
     176,   -11,     4
};

/* YYTABLE[YYPACT[STATE-NUM]].  What to do in state STATE-NUM.  If
   positive, shift that token.  If negative, reduce the rule which
   number is the opposite.  If zero, do what YYDEFACT says.
   If YYTABLE_NINF, syntax error.  */
#define YYTABLE_NINF -1
static const yytype_uint8 yytable[] =
{
      31,    40,    41,   128,    38,   105,    38,    38,    42,    43,
     128,    45,    80,    26,    31,    15,    27,   129,    31,    16,
      26,   105,    26,   103,   129,    27,   104,    26,   130,   131,
      27,   132,   128,    68,   139,   130,   131,    31,   132,    82,
      83,   151,    80,    74,    38,    38,   129,   128,    38,   123,
      28,    73,   110,    77,    77,    28,    17,   130,   131,   111,
     132,   129,   128,   152,    49,    49,   112,    53,   113,   128,
      50,    50,   114,   131,    67,   132,   129,    26,    18,    97,
      27,   115,    97,   129,    68,    67,    53,   130,   131,   120,
     132,    26,    79,   100,    27,    68,    26,   132,    23,   103,
      93,   148,   104,    19,   119,    57,   128,    39,    20,    68,
      58,    96,    67,    24,    74,   149,     1,     2,    21,    74,
     129,     3,    68,     4,     5,    22,    49,    25,    32,    46,
       6,     7,    50,     8,     9,    47,   153,    10,    11,     1,
       2,    44,    89,    90,     3,    91,     4,     5,   127,    94,
      61,    54,    94,     6,     7,    58,     8,     9,    55,   138,
      10,   140,   141,   142,   143,   144,   145,    34,    85,    62,
      26,   101,    35,    27,    58,   102,    58,    56,    86,    60,
      58,    63,    64,    65,    66,   117,    87,    88,    92,    95,
     136,    98,    99,   118,   121,   122,   125,    91,   126,   157,
     147,   134,   133,   146,   154,    33,   155,   156,   124,   158,
     135,    48,    84,     0,     0,    72,     0,     0,     0,     0,
       0,     0,     0,     0,    81,     0,    78
};

static const yytype_int16 yycheck[] =
{
      11,    17,    18,     3,    15,    90,    17,    18,    19,    20,
       3,    22,    51,     8,    25,    33,    11,    17,    29,    33,
       8,   106,     8,    11,    17,    11,    14,     8,    28,    29,
      11,    31,     3,    21,    34,    28,    29,    48,    31,    55,
      56,    34,    81,    31,    55,    56,    17,     3,    59,    37,
      36,    37,     4,    49,    50,    36,    33,    28,    29,    11,
      31,    17,     3,    34,     9,     9,    18,    29,    20,     3,
      15,    15,    24,    29,    11,    31,    17,     8,    33,    75,
      11,    33,    78,    17,    21,    11,    48,    28,    29,   100,
      31,     8,    37,    37,    11,    21,     8,    31,     1,    11,
      37,    11,    14,    33,    35,    34,     3,    11,    33,    21,
      39,    37,    11,    16,    31,    25,     5,     6,    33,    31,
      17,    10,    21,    12,    13,    33,     9,    36,     0,    36,
      19,    20,    15,    22,    23,    36,   147,    26,    27,     5,
       6,    18,    35,    36,    10,    38,    12,    13,   115,    69,
      34,    36,    72,    19,    20,    39,    22,    23,    33,   126,
      26,   128,   129,   130,   131,   132,   133,     5,    35,    34,
       8,    34,    10,    11,    39,    34,    39,    33,    35,    34,
      39,    34,    34,    34,    34,    11,    35,    35,    33,    35,
       7,    35,    35,    35,    33,    35,    33,    38,    33,    11,
      33,    35,    38,    34,    33,    13,    34,    34,   106,    34,
     121,    25,    59,    -1,    -1,    47,    -1,    -1,    -1,    -1,
      -1,    -1,    -1,    -1,    54,    -1,    50
};

/* YYSTOS[STATE-NUM] -- The (internal number of the) accessing
   symbol of state STATE-NUM.  */
static const yytype_uint8 yystos[] =
{
       0,     5,     6,    10,    12,    13,    19,    20,    22,    23,
      26,    27,    41,    42,    43,    33,    33,    33,    33,    33,
      33,    33,    33,     1,    16,    36,     8,    11,    36,    56,
      57,    61,     0,    43,     5,    10,    53,    55,    61,    11,
      53,    53,    61,    61,    18,    61,    36,    36,    56,     9,
      15,    58,    59,    57,    36,    33,    33,    34,    39,    54,
      34,    34,    34,    34,    34,    34,    34,    11,    21,    44,
      45,    46,    44,    37,    31,    60,    61,    62,    60,    37,
      59,    58,    53,    53,    55,    35,    35,    35,    35,    35,
      36,    38,    33,    37,    45,    35,    37,    62,    35,    35,
      37,    34,    34,    11,    14,    46,    47,    48,    49,    62,
       4,    11,    18,    20,    24,    33,    52,    11,    35,    35,
      61,    33,    35,    37,    48,    33,    33,    52,     3,    17,
      28,    29,    31,    38,    35,    49,     7,    51,    52,    34,
      52,    52,    52,    52,    52,    52,    34,    33,    11,    25,
      50,    34,    34,    61,    33,    34,    34,    11,    34
};

#define yyerrok		(yyerrstatus = 0)
#define yyclearin	(yychar = YYEMPTY)
#define YYEMPTY		(-2)
#define YYEOF		0

#define YYACCEPT	goto yyacceptlab
#define YYABORT		goto yyabortlab
#define YYERROR		goto yyerrorlab


/* Like YYERROR except do call yyerror.  This remains here temporarily
   to ease the transition to the new meaning of YYERROR, for GCC.
   Once GCC version 2 has supplanted version 1, this can go.  */

#define YYFAIL		goto yyerrlab

#define YYRECOVERING()  (!!yyerrstatus)

#define YYBACKUP(Token, Value)					\
do								\
  if (yychar == YYEMPTY && yylen == 1)				\
    {								\
      yychar = (Token);						\
      yylval = (Value);						\
      yytoken = YYTRANSLATE (yychar);				\
      YYPOPSTACK (1);						\
      goto yybackup;						\
    }								\
  else								\
    {								\
      yyerror (YY_("syntax error: cannot back up")); \
      YYERROR;							\
    }								\
while (YYID (0))


#define YYTERROR	1
#define YYERRCODE	256


/* YYLLOC_DEFAULT -- Set CURRENT to span from RHS[1] to RHS[N].
   If N is 0, then set CURRENT to the empty location which ends
   the previous symbol: RHS[0] (always defined).  */

#define YYRHSLOC(Rhs, K) ((Rhs)[K])
#ifndef YYLLOC_DEFAULT
# define YYLLOC_DEFAULT(Current, Rhs, N)				\
    do									\
      if (YYID (N))                                                    \
	{								\
	  (Current).first_line   = YYRHSLOC (Rhs, 1).first_line;	\
	  (Current).first_column = YYRHSLOC (Rhs, 1).first_column;	\
	  (Current).last_line    = YYRHSLOC (Rhs, N).last_line;		\
	  (Current).last_column  = YYRHSLOC (Rhs, N).last_column;	\
	}								\
      else								\
	{								\
	  (Current).first_line   = (Current).last_line   =		\
	    YYRHSLOC (Rhs, 0).last_line;				\
	  (Current).first_column = (Current).last_column =		\
	    YYRHSLOC (Rhs, 0).last_column;				\
	}								\
    while (YYID (0))
#endif


/* YY_LOCATION_PRINT -- Print the location on the stream.
   This macro was not mandated originally: define only if we know
   we won't break user code: when these are the locations we know.  */

#ifndef YY_LOCATION_PRINT
# if YYLTYPE_IS_TRIVIAL
#  define YY_LOCATION_PRINT(File, Loc)			\
     fprintf (File, "%d.%d-%d.%d",			\
	      (Loc).first_line, (Loc).first_column,	\
	      (Loc).last_line,  (Loc).last_column)
# else
#  define YY_LOCATION_PRINT(File, Loc) ((void) 0)
# endif
#endif


/* YYLEX -- calling `yylex' with the right arguments.  */

#ifdef YYLEX_PARAM
# define YYLEX yylex (YYLEX_PARAM)
#else
# define YYLEX yylex ()
#endif

/* Enable debugging if requested.  */
#if YYDEBUG

# ifndef YYFPRINTF
#  include <stdio.h> /* INFRINGES ON USER NAME SPACE */
#  define YYFPRINTF fprintf
# endif

# define YYDPRINTF(Args)			\
do {						\
  if (yydebug)					\
    YYFPRINTF Args;				\
} while (YYID (0))

# define YY_SYMBOL_PRINT(Title, Type, Value, Location)			  \
do {									  \
  if (yydebug)								  \
    {									  \
      YYFPRINTF (stderr, "%s ", Title);					  \
      yy_symbol_print (stderr,						  \
		  Type, Value); \
      YYFPRINTF (stderr, "\n");						  \
    }									  \
} while (YYID (0))


/*--------------------------------.
| Print this symbol on YYOUTPUT.  |
`--------------------------------*/

/*ARGSUSED*/
#if (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
static void
yy_symbol_value_print (FILE *yyoutput, int yytype, YYSTYPE const * const yyvaluep)
#else
static void
yy_symbol_value_print (yyoutput, yytype, yyvaluep)
    FILE *yyoutput;
    int yytype;
    YYSTYPE const * const yyvaluep;
#endif
{
  if (!yyvaluep)
    return;
# ifdef YYPRINT
  if (yytype < YYNTOKENS)
    YYPRINT (yyoutput, yytoknum[yytype], *yyvaluep);
# else
  YYUSE (yyoutput);
# endif
  switch (yytype)
    {
      default:
	break;
    }
}


/*--------------------------------.
| Print this symbol on YYOUTPUT.  |
`--------------------------------*/

#if (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
static void
yy_symbol_print (FILE *yyoutput, int yytype, YYSTYPE const * const yyvaluep)
#else
static void
yy_symbol_print (yyoutput, yytype, yyvaluep)
    FILE *yyoutput;
    int yytype;
    YYSTYPE const * const yyvaluep;
#endif
{
  if (yytype < YYNTOKENS)
    YYFPRINTF (yyoutput, "token %s (", yytname[yytype]);
  else
    YYFPRINTF (yyoutput, "nterm %s (", yytname[yytype]);

  yy_symbol_value_print (yyoutput, yytype, yyvaluep);
  YYFPRINTF (yyoutput, ")");
}

/*------------------------------------------------------------------.
| yy_stack_print -- Print the state stack from its BOTTOM up to its |
| TOP (included).                                                   |
`------------------------------------------------------------------*/

#if (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
static void
yy_stack_print (yytype_int16 *bottom, yytype_int16 *top)
#else
static void
yy_stack_print (bottom, top)
    yytype_int16 *bottom;
    yytype_int16 *top;
#endif
{
  YYFPRINTF (stderr, "Stack now");
  for (; bottom <= top; ++bottom)
    YYFPRINTF (stderr, " %d", *bottom);
  YYFPRINTF (stderr, "\n");
}

# define YY_STACK_PRINT(Bottom, Top)				\
do {								\
  if (yydebug)							\
    yy_stack_print ((Bottom), (Top));				\
} while (YYID (0))


/*------------------------------------------------.
| Report that the YYRULE is going to be reduced.  |
`------------------------------------------------*/

#if (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
static void
yy_reduce_print (YYSTYPE *yyvsp, int yyrule)
#else
static void
yy_reduce_print (yyvsp, yyrule)
    YYSTYPE *yyvsp;
    int yyrule;
#endif
{
  int yynrhs = yyr2[yyrule];
  int yyi;
  unsigned long int yylno = yyrline[yyrule];
  YYFPRINTF (stderr, "Reducing stack by rule %d (line %lu):\n",
	     yyrule - 1, yylno);
  /* The symbols being reduced.  */
  for (yyi = 0; yyi < yynrhs; yyi++)
    {
      fprintf (stderr, "   $%d = ", yyi + 1);
      yy_symbol_print (stderr, yyrhs[yyprhs[yyrule] + yyi],
		       &(yyvsp[(yyi + 1) - (yynrhs)])
		       		       );
      fprintf (stderr, "\n");
    }
}

# define YY_REDUCE_PRINT(Rule)		\
do {					\
  if (yydebug)				\
    yy_reduce_print (yyvsp, Rule); \
} while (YYID (0))

/* Nonzero means print parse trace.  It is left uninitialized so that
   multiple parsers can coexist.  */
int yydebug;
#else /* !YYDEBUG */
# define YYDPRINTF(Args)
# define YY_SYMBOL_PRINT(Title, Type, Value, Location)
# define YY_STACK_PRINT(Bottom, Top)
# define YY_REDUCE_PRINT(Rule)
#endif /* !YYDEBUG */


/* YYINITDEPTH -- initial size of the parser's stacks.  */
#ifndef	YYINITDEPTH
# define YYINITDEPTH 200
#endif

/* YYMAXDEPTH -- maximum size the stacks can grow to (effective only
   if the built-in stack extension method is used).

   Do not make this value too large; the results are undefined if
   YYSTACK_ALLOC_MAXIMUM < YYSTACK_BYTES (YYMAXDEPTH)
   evaluated with infinite-precision integer arithmetic.  */

#ifndef YYMAXDEPTH
# define YYMAXDEPTH 10000
#endif



#if YYERROR_VERBOSE

# ifndef yystrlen
#  if defined __GLIBC__ && defined _STRING_H
#   define yystrlen strlen
#  else
/* Return the length of YYSTR.  */
#if (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
static YYSIZE_T
yystrlen (const char *yystr)
#else
static YYSIZE_T
yystrlen (yystr)
    const char *yystr;
#endif
{
  YYSIZE_T yylen;
  for (yylen = 0; yystr[yylen]; yylen++)
    continue;
  return yylen;
}
#  endif
# endif

# ifndef yystpcpy
#  if defined __GLIBC__ && defined _STRING_H && defined _GNU_SOURCE
#   define yystpcpy stpcpy
#  else
/* Copy YYSRC to YYDEST, returning the address of the terminating '\0' in
   YYDEST.  */
#if (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
static char *
yystpcpy (char *yydest, const char *yysrc)
#else
static char *
yystpcpy (yydest, yysrc)
    char *yydest;
    const char *yysrc;
#endif
{
  char *yyd = yydest;
  const char *yys = yysrc;

  while ((*yyd++ = *yys++) != '\0')
    continue;

  return yyd - 1;
}
#  endif
# endif

# ifndef yytnamerr
/* Copy to YYRES the contents of YYSTR after stripping away unnecessary
   quotes and backslashes, so that it's suitable for yyerror.  The
   heuristic is that double-quoting is unnecessary unless the string
   contains an apostrophe, a comma, or backslash (other than
   backslash-backslash).  YYSTR is taken from yytname.  If YYRES is
   null, do not copy; instead, return the length of what the result
   would have been.  */
static YYSIZE_T
yytnamerr (char *yyres, const char *yystr)
{
  if (*yystr == '"')
    {
      YYSIZE_T yyn = 0;
      char const *yyp = yystr;

      for (;;)
	switch (*++yyp)
	  {
	  case '\'':
	  case ',':
	    goto do_not_strip_quotes;

	  case '\\':
	    if (*++yyp != '\\')
	      goto do_not_strip_quotes;
	    /* Fall through.  */
	  default:
	    if (yyres)
	      yyres[yyn] = *yyp;
	    yyn++;
	    break;

	  case '"':
	    if (yyres)
	      yyres[yyn] = '\0';
	    return yyn;
	  }
    do_not_strip_quotes: ;
    }

  if (! yyres)
    return yystrlen (yystr);

  return yystpcpy (yyres, yystr) - yyres;
}
# endif

/* Copy into YYRESULT an error message about the unexpected token
   YYCHAR while in state YYSTATE.  Return the number of bytes copied,
   including the terminating null byte.  If YYRESULT is null, do not
   copy anything; just return the number of bytes that would be
   copied.  As a special case, return 0 if an ordinary "syntax error"
   message will do.  Return YYSIZE_MAXIMUM if overflow occurs during
   size calculation.  */
static YYSIZE_T
yysyntax_error (char *yyresult, int yystate, int yychar)
{
  int yyn = yypact[yystate];

  if (! (YYPACT_NINF < yyn && yyn <= YYLAST))
    return 0;
  else
    {
      int yytype = YYTRANSLATE (yychar);
      YYSIZE_T yysize0 = yytnamerr (0, yytname[yytype]);
      YYSIZE_T yysize = yysize0;
      YYSIZE_T yysize1;
      int yysize_overflow = 0;
      enum { YYERROR_VERBOSE_ARGS_MAXIMUM = 5 };
      char const *yyarg[YYERROR_VERBOSE_ARGS_MAXIMUM];
      int yyx;

# if 0
      /* This is so xgettext sees the translatable formats that are
	 constructed on the fly.  */
      YY_("syntax error, unexpected %s");
      YY_("syntax error, unexpected %s, expecting %s");
      YY_("syntax error, unexpected %s, expecting %s or %s");
      YY_("syntax error, unexpected %s, expecting %s or %s or %s");
      YY_("syntax error, unexpected %s, expecting %s or %s or %s or %s");
# endif
      char *yyfmt;
      char const *yyf;
      static char const yyunexpected[] = "syntax error, unexpected %s";
      static char const yyexpecting[] = ", expecting %s";
      static char const yyor[] = " or %s";
      char yyformat[sizeof yyunexpected
		    + sizeof yyexpecting - 1
		    + ((YYERROR_VERBOSE_ARGS_MAXIMUM - 2)
		       * (sizeof yyor - 1))];
      char const *yyprefix = yyexpecting;

      /* Start YYX at -YYN if negative to avoid negative indexes in
	 YYCHECK.  */
      int yyxbegin = yyn < 0 ? -yyn : 0;

      /* Stay within bounds of both yycheck and yytname.  */
      int yychecklim = YYLAST - yyn + 1;
      int yyxend = yychecklim < YYNTOKENS ? yychecklim : YYNTOKENS;
      int yycount = 1;

      yyarg[0] = yytname[yytype];
      yyfmt = yystpcpy (yyformat, yyunexpected);

      for (yyx = yyxbegin; yyx < yyxend; ++yyx)
	if (yycheck[yyx + yyn] == yyx && yyx != YYTERROR)
	  {
	    if (yycount == YYERROR_VERBOSE_ARGS_MAXIMUM)
	      {
		yycount = 1;
		yysize = yysize0;
		yyformat[sizeof yyunexpected - 1] = '\0';
		break;
	      }
	    yyarg[yycount++] = yytname[yyx];
	    yysize1 = yysize + yytnamerr (0, yytname[yyx]);
	    yysize_overflow |= (yysize1 < yysize);
	    yysize = yysize1;
	    yyfmt = yystpcpy (yyfmt, yyprefix);
	    yyprefix = yyor;
	  }

      yyf = YY_(yyformat);
      yysize1 = yysize + yystrlen (yyf);
      yysize_overflow |= (yysize1 < yysize);
      yysize = yysize1;

      if (yysize_overflow)
	return YYSIZE_MAXIMUM;

      if (yyresult)
	{
	  /* Avoid sprintf, as that infringes on the user's name space.
	     Don't have undefined behavior even if the translation
	     produced a string with the wrong number of "%s"s.  */
	  char *yyp = yyresult;
	  int yyi = 0;
	  while ((*yyp = *yyf) != '\0')
	    {
	      if (*yyp == '%' && yyf[1] == 's' && yyi < yycount)
		{
		  yyp += yytnamerr (yyp, yyarg[yyi++]);
		  yyf += 2;
		}
	      else
		{
		  yyp++;
		  yyf++;
		}
	    }
	}
      return yysize;
    }
}
#endif /* YYERROR_VERBOSE */


/*-----------------------------------------------.
| Release the memory associated to this symbol.  |
`-----------------------------------------------*/

/*ARGSUSED*/
#if (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
static void
yydestruct (const char *yymsg, int yytype, YYSTYPE *yyvaluep)
#else
static void
yydestruct (yymsg, yytype, yyvaluep)
    const char *yymsg;
    int yytype;
    YYSTYPE *yyvaluep;
#endif
{
  YYUSE (yyvaluep);

  if (!yymsg)
    yymsg = "Deleting";
  YY_SYMBOL_PRINT (yymsg, yytype, yyvaluep, yylocationp);

  switch (yytype)
    {

      default:
	break;
    }
}


/* Prevent warnings from -Wmissing-prototypes.  */

#ifdef YYPARSE_PARAM
#if defined __STDC__ || defined __cplusplus
int yyparse (void *YYPARSE_PARAM);
#else
int yyparse ();
#endif
#else /* ! YYPARSE_PARAM */
#if defined __STDC__ || defined __cplusplus
int yyparse (void);
#else
int yyparse ();
#endif
#endif /* ! YYPARSE_PARAM */



/* The look-ahead symbol.  */
int yychar;

/* The semantic value of the look-ahead symbol.  */
YYSTYPE yylval;

/* Number of syntax errors so far.  */
int yynerrs;



/*----------.
| yyparse.  |
`----------*/

#ifdef YYPARSE_PARAM
#if (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
int
yyparse (void *YYPARSE_PARAM)
#else
int
yyparse (YYPARSE_PARAM)
    void *YYPARSE_PARAM;
#endif
#else /* ! YYPARSE_PARAM */
#if (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
int
yyparse (void)
#else
int
yyparse ()

#endif
#endif
{
  
  int yystate;
  int yyn;
  int yyresult;
  /* Number of tokens to shift before error messages enabled.  */
  int yyerrstatus;
  /* Look-ahead token as an internal (translated) token number.  */
  int yytoken = 0;
#if YYERROR_VERBOSE
  /* Buffer for error messages, and its allocated size.  */
  char yymsgbuf[128];
  char *yymsg = yymsgbuf;
  YYSIZE_T yymsg_alloc = sizeof yymsgbuf;
#endif

  /* Three stacks and their tools:
     `yyss': related to states,
     `yyvs': related to semantic values,
     `yyls': related to locations.

     Refer to the stacks thru separate pointers, to allow yyoverflow
     to reallocate them elsewhere.  */

  /* The state stack.  */
  yytype_int16 yyssa[YYINITDEPTH];
  yytype_int16 *yyss = yyssa;
  yytype_int16 *yyssp;

  /* The semantic value stack.  */
  YYSTYPE yyvsa[YYINITDEPTH];
  YYSTYPE *yyvs = yyvsa;
  YYSTYPE *yyvsp;



#define YYPOPSTACK(N)   (yyvsp -= (N), yyssp -= (N))

  YYSIZE_T yystacksize = YYINITDEPTH;

  /* The variables used to return semantic value and location from the
     action routines.  */
  YYSTYPE yyval;


  /* The number of symbols on the RHS of the reduced rule.
     Keep to zero when no symbol should be popped.  */
  int yylen = 0;

  YYDPRINTF ((stderr, "Starting parse\n"));

  yystate = 0;
  yyerrstatus = 0;
  yynerrs = 0;
  yychar = YYEMPTY;		/* Cause a token to be read.  */

  /* Initialize stack pointers.
     Waste one element of value and location stack
     so that they stay on the same level as the state stack.
     The wasted elements are never initialized.  */

  yyssp = yyss;
  yyvsp = yyvs;

  goto yysetstate;

/*------------------------------------------------------------.
| yynewstate -- Push a new state, which is found in yystate.  |
`------------------------------------------------------------*/
 yynewstate:
  /* In all cases, when you get here, the value and location stacks
     have just been pushed.  So pushing a state here evens the stacks.  */
  yyssp++;

 yysetstate:
  *yyssp = yystate;

  if (yyss + yystacksize - 1 <= yyssp)
    {
      /* Get the current used size of the three stacks, in elements.  */
      YYSIZE_T yysize = yyssp - yyss + 1;

#ifdef yyoverflow
      {
	/* Give user a chance to reallocate the stack.  Use copies of
	   these so that the &'s don't force the real ones into
	   memory.  */
	YYSTYPE *yyvs1 = yyvs;
	yytype_int16 *yyss1 = yyss;


	/* Each stack pointer address is followed by the size of the
	   data in use in that stack, in bytes.  This used to be a
	   conditional around just the two extra args, but that might
	   be undefined if yyoverflow is a macro.  */
	yyoverflow (YY_("memory exhausted"),
		    &yyss1, yysize * sizeof (*yyssp),
		    &yyvs1, yysize * sizeof (*yyvsp),

		    &yystacksize);

	yyss = yyss1;
	yyvs = yyvs1;
      }
#else /* no yyoverflow */
# ifndef YYSTACK_RELOCATE
      goto yyexhaustedlab;
# else
      /* Extend the stack our own way.  */
      if (YYMAXDEPTH <= yystacksize)
	goto yyexhaustedlab;
      yystacksize *= 2;
      if (YYMAXDEPTH < yystacksize)
	yystacksize = YYMAXDEPTH;

      {
	yytype_int16 *yyss1 = yyss;
	union yyalloc *yyptr =
	  (union yyalloc *) YYSTACK_ALLOC (YYSTACK_BYTES (yystacksize));
	if (! yyptr)
	  goto yyexhaustedlab;
	YYSTACK_RELOCATE (yyss);
	YYSTACK_RELOCATE (yyvs);

#  undef YYSTACK_RELOCATE
	if (yyss1 != yyssa)
	  YYSTACK_FREE (yyss1);
      }
# endif
#endif /* no yyoverflow */

      yyssp = yyss + yysize - 1;
      yyvsp = yyvs + yysize - 1;


      YYDPRINTF ((stderr, "Stack size increased to %lu\n",
		  (unsigned long int) yystacksize));

      if (yyss + yystacksize - 1 <= yyssp)
	YYABORT;
    }

  YYDPRINTF ((stderr, "Entering state %d\n", yystate));

  goto yybackup;

/*-----------.
| yybackup.  |
`-----------*/
yybackup:

  /* Do appropriate processing given the current state.  Read a
     look-ahead token if we need one and don't already have one.  */

  /* First try to decide what to do without reference to look-ahead token.  */
  yyn = yypact[yystate];
  if (yyn == YYPACT_NINF)
    goto yydefault;

  /* Not known => get a look-ahead token if don't already have one.  */

  /* YYCHAR is either YYEMPTY or YYEOF or a valid look-ahead symbol.  */
  if (yychar == YYEMPTY)
    {
      YYDPRINTF ((stderr, "Reading a token: "));
      yychar = YYLEX;
    }

  if (yychar <= YYEOF)
    {
      yychar = yytoken = YYEOF;
      YYDPRINTF ((stderr, "Now at end of input.\n"));
    }
  else
    {
      yytoken = YYTRANSLATE (yychar);
      YY_SYMBOL_PRINT ("Next token is", yytoken, &yylval, &yylloc);
    }

  /* If the proper action on seeing token YYTOKEN is to reduce or to
     detect an error, take that action.  */
  yyn += yytoken;
  if (yyn < 0 || YYLAST < yyn || yycheck[yyn] != yytoken)
    goto yydefault;
  yyn = yytable[yyn];
  if (yyn <= 0)
    {
      if (yyn == 0 || yyn == YYTABLE_NINF)
	goto yyerrlab;
      yyn = -yyn;
      goto yyreduce;
    }

  if (yyn == YYFINAL)
    YYACCEPT;

  /* Count tokens shifted since error; after three, turn off error
     status.  */
  if (yyerrstatus)
    yyerrstatus--;

  /* Shift the look-ahead token.  */
  YY_SYMBOL_PRINT ("Shifting", yytoken, &yylval, &yylloc);

  /* Discard the shifted token unless it is eof.  */
  if (yychar != YYEOF)
    yychar = YYEMPTY;

  yystate = yyn;
  *++yyvsp = yylval;

  goto yynewstate;


/*-----------------------------------------------------------.
| yydefault -- do the default action for the current state.  |
`-----------------------------------------------------------*/
yydefault:
  yyn = yydefact[yystate];
  if (yyn == 0)
    goto yyerrlab;
  goto yyreduce;


/*-----------------------------.
| yyreduce -- Do a reduction.  |
`-----------------------------*/
yyreduce:
  /* yyn is the number of a rule to reduce with.  */
  yylen = yyr2[yyn];

  /* If YYLEN is nonzero, implement the default value of the action:
     `$$ = $1'.

     Otherwise, the following line sets YYVAL to garbage.
     This behavior is undocumented and Bison
     users should not rely upon it.  Assigning to YYVAL
     unconditionally makes the parser a bit smaller, and it avoids a
     GCC warning that YYVAL may be used uninitialized.  */
  yyval = yyvsp[1-yylen];


  YY_REDUCE_PRINT (yyn);
  switch (yyn)
    {
        case 3:
#line 145 "ldscript.y"
    { add_versions ((yyvsp[(2) - (2)].version)); }
    break;

  case 6:
#line 153 "ldscript.y"
    {
		      if (likely (ld_state.entry == NULL))
			ld_state.entry = (yyvsp[(3) - (5)].str);
		    }
    break;

  case 7:
#line 158 "ldscript.y"
    {
		      ld_new_searchdir ((yyvsp[(3) - (5)].str));
		    }
    break;

  case 8:
#line 162 "ldscript.y"
    {
		      if (likely (ld_state.pagesize == 0))
			ld_state.pagesize = (yyvsp[(3) - (5)].num);
		    }
    break;

  case 9:
#line 167 "ldscript.y"
    {
		      if (likely (ld_state.interp == NULL)
			  && ld_state.file_type != dso_file_type)
			ld_state.interp = (yyvsp[(3) - (5)].str);
		    }
    break;

  case 10:
#line 173 "ldscript.y"
    {
		      new_segment ((yyvsp[(2) - (5)].num), (yyvsp[(4) - (5)].output_rule));
		    }
    break;

  case 11:
#line 177 "ldscript.y"
    {
		      fputs_unlocked (gettext ("mode for segment invalid\n"),
				      stderr);
		      new_segment (0, (yyvsp[(4) - (5)].output_rule));
		    }
    break;

  case 12:
#line 183 "ldscript.y"
    {
		      /* First little optimization.  If there is only one
			 file in the group don't do anything.  */
		      if ((yyvsp[(3) - (4)].filename_list) != (yyvsp[(3) - (4)].filename_list)->next)
			{
			  (yyvsp[(3) - (4)].filename_list)->next->group_start = 1;
			  (yyvsp[(3) - (4)].filename_list)->group_end = 1;
			}
		      add_inputfiles ((yyvsp[(3) - (4)].filename_list));
		    }
    break;

  case 13:
#line 194 "ldscript.y"
    { add_inputfiles ((yyvsp[(3) - (4)].filename_list)); }
    break;

  case 14:
#line 196 "ldscript.y"
    { add_inputfiles (mark_as_needed ((yyvsp[(3) - (4)].filename_list))); }
    break;

  case 15:
#line 198 "ldscript.y"
    { add_versions ((yyvsp[(3) - (4)].version)); }
    break;

  case 16:
#line 200 "ldscript.y"
    { /* XXX TODO */ }
    break;

  case 17:
#line 204 "ldscript.y"
    {
		      (yyvsp[(2) - (2)].output_rule)->next = (yyvsp[(1) - (2)].output_rule)->next;
		      (yyval.output_rule) = (yyvsp[(1) - (2)].output_rule)->next = (yyvsp[(2) - (2)].output_rule);
		    }
    break;

  case 18:
#line 209 "ldscript.y"
    { (yyval.output_rule) = (yyvsp[(1) - (1)].output_rule); }
    break;

  case 19:
#line 213 "ldscript.y"
    {
		      (yyval.output_rule) = new_output_rule (output_assignment);
		      (yyval.output_rule)->val.assignment = (yyvsp[(1) - (2)].assignment);
		    }
    break;

  case 20:
#line 218 "ldscript.y"
    {
		      (yyval.output_rule) = new_output_rule (output_section);
		      (yyval.output_rule)->val.section.name = (yyvsp[(1) - (4)].str);
		      (yyval.output_rule)->val.section.input = (yyvsp[(3) - (4)].input_rule)->next;
		      if (ld_state.strip == strip_debug
			  && ebl_debugscn_p (ld_state.ebl, (yyvsp[(1) - (4)].str)))
			(yyval.output_rule)->val.section.ignored = true;
		      else
			(yyval.output_rule)->val.section.ignored = false;
		      (yyvsp[(3) - (4)].input_rule)->next = NULL;
		    }
    break;

  case 21:
#line 230 "ldscript.y"
    {
		      /* This is a short cut for "ID { *(ID) }".  */
		      (yyval.output_rule) = new_output_rule (output_section);
		      (yyval.output_rule)->val.section.name = (yyvsp[(1) - (2)].str);
		      (yyval.output_rule)->val.section.input = new_input_rule (input_section);
		      (yyval.output_rule)->val.section.input->next = NULL;
		      (yyval.output_rule)->val.section.input->val.section =
			(struct filemask_section_name *)
			  obstack_alloc (&ld_state.smem,
					 sizeof (struct filemask_section_name));
		      (yyval.output_rule)->val.section.input->val.section->filemask = NULL;
		      (yyval.output_rule)->val.section.input->val.section->excludemask = NULL;
		      (yyval.output_rule)->val.section.input->val.section->section_name =
			new_input_section_name ((yyvsp[(1) - (2)].str), false);
		      (yyval.output_rule)->val.section.input->val.section->keep_flag = false;
		      if (ld_state.strip == strip_debug
			  && ebl_debugscn_p (ld_state.ebl, (yyvsp[(1) - (2)].str)))
			(yyval.output_rule)->val.section.ignored = true;
		      else
			(yyval.output_rule)->val.section.ignored = false;
		    }
    break;

  case 22:
#line 254 "ldscript.y"
    { (yyval.assignment) = new_assignment ((yyvsp[(1) - (3)].str), (yyvsp[(3) - (3)].expr), false); }
    break;

  case 23:
#line 256 "ldscript.y"
    { (yyval.assignment) = new_assignment ((yyvsp[(3) - (6)].str), (yyvsp[(5) - (6)].expr), true); }
    break;

  case 24:
#line 260 "ldscript.y"
    {
		      (yyvsp[(2) - (2)].input_rule)->next = (yyvsp[(1) - (2)].input_rule)->next;
		      (yyval.input_rule) = (yyvsp[(1) - (2)].input_rule)->next = (yyvsp[(2) - (2)].input_rule);
		    }
    break;

  case 25:
#line 265 "ldscript.y"
    { (yyval.input_rule) = (yyvsp[(1) - (1)].input_rule); }
    break;

  case 26:
#line 269 "ldscript.y"
    {
		      (yyval.input_rule) = new_input_rule (input_section);
		      (yyval.input_rule)->val.section = (yyvsp[(1) - (1)].filemask_section_name);
		    }
    break;

  case 27:
#line 274 "ldscript.y"
    {
		      (yyvsp[(3) - (4)].filemask_section_name)->keep_flag = true;

		      (yyval.input_rule) = new_input_rule (input_section);
		      (yyval.input_rule)->val.section = (yyvsp[(3) - (4)].filemask_section_name);
		    }
    break;

  case 28:
#line 281 "ldscript.y"
    {
		      (yyval.input_rule) = new_input_rule (input_assignment);
		      (yyval.input_rule)->val.assignment = (yyvsp[(1) - (2)].assignment);
		    }
    break;

  case 29:
#line 288 "ldscript.y"
    {
		      (yyval.filemask_section_name) = (struct filemask_section_name *)
			obstack_alloc (&ld_state.smem, sizeof (*(yyval.filemask_section_name)));
		      (yyval.filemask_section_name)->filemask = (yyvsp[(1) - (5)].str);
		      (yyval.filemask_section_name)->excludemask = (yyvsp[(3) - (5)].str);
		      (yyval.filemask_section_name)->section_name = (yyvsp[(4) - (5)].sectionname);
		      (yyval.filemask_section_name)->keep_flag = false;
		    }
    break;

  case 30:
#line 299 "ldscript.y"
    { (yyval.sectionname) = new_input_section_name ((yyvsp[(1) - (1)].str), false); }
    break;

  case 31:
#line 301 "ldscript.y"
    { (yyval.sectionname) = new_input_section_name ((yyvsp[(3) - (4)].str), true); }
    break;

  case 32:
#line 305 "ldscript.y"
    { (yyval.str) = (yyvsp[(3) - (4)].str); }
    break;

  case 33:
#line 307 "ldscript.y"
    { (yyval.str) = NULL; }
    break;

  case 34:
#line 311 "ldscript.y"
    {
		      (yyval.expr) = new_expr (exp_align);
		      (yyval.expr)->val.child = (yyvsp[(3) - (4)].expr);
		    }
    break;

  case 35:
#line 316 "ldscript.y"
    { (yyval.expr) = (yyvsp[(2) - (3)].expr); }
    break;

  case 36:
#line 318 "ldscript.y"
    {
		      (yyval.expr) = new_expr (exp_mult);
		      (yyval.expr)->val.binary.left = (yyvsp[(1) - (3)].expr);
		      (yyval.expr)->val.binary.right = (yyvsp[(3) - (3)].expr);
		    }
    break;

  case 37:
#line 324 "ldscript.y"
    {
		      (yyval.expr) = new_expr ((yyvsp[(2) - (3)].op));
		      (yyval.expr)->val.binary.left = (yyvsp[(1) - (3)].expr);
		      (yyval.expr)->val.binary.right = (yyvsp[(3) - (3)].expr);
		    }
    break;

  case 38:
#line 330 "ldscript.y"
    {
		      (yyval.expr) = new_expr ((yyvsp[(2) - (3)].op));
		      (yyval.expr)->val.binary.left = (yyvsp[(1) - (3)].expr);
		      (yyval.expr)->val.binary.right = (yyvsp[(3) - (3)].expr);
		    }
    break;

  case 39:
#line 336 "ldscript.y"
    {
		      (yyval.expr) = new_expr (exp_and);
		      (yyval.expr)->val.binary.left = (yyvsp[(1) - (3)].expr);
		      (yyval.expr)->val.binary.right = (yyvsp[(3) - (3)].expr);
		    }
    break;

  case 40:
#line 342 "ldscript.y"
    {
		      (yyval.expr) = new_expr (exp_or);
		      (yyval.expr)->val.binary.left = (yyvsp[(1) - (3)].expr);
		      (yyval.expr)->val.binary.right = (yyvsp[(3) - (3)].expr);
		    }
    break;

  case 41:
#line 348 "ldscript.y"
    {
		      (yyval.expr) = new_expr (exp_num);
		      (yyval.expr)->val.num = (yyvsp[(1) - (1)].num);
		    }
    break;

  case 42:
#line 353 "ldscript.y"
    {
		      (yyval.expr) = new_expr (exp_id);
		      (yyval.expr)->val.str = (yyvsp[(1) - (1)].str);
		    }
    break;

  case 43:
#line 358 "ldscript.y"
    { (yyval.expr) = new_expr (exp_sizeof_headers); }
    break;

  case 44:
#line 360 "ldscript.y"
    { (yyval.expr) = new_expr (exp_pagesize); }
    break;

  case 45:
#line 364 "ldscript.y"
    {
		      (yyvsp[(3) - (3)].filename_list)->next = (yyvsp[(1) - (3)].filename_list)->next;
		      (yyval.filename_list) = (yyvsp[(1) - (3)].filename_list)->next = (yyvsp[(3) - (3)].filename_list);
		    }
    break;

  case 46:
#line 369 "ldscript.y"
    { (yyval.filename_list) = (yyvsp[(1) - (1)].filename_list); }
    break;

  case 49:
#line 377 "ldscript.y"
    {
		      /* First little optimization.  If there is only one
			 file in the group don't do anything.  */
		      if ((yyvsp[(3) - (4)].filename_list) != (yyvsp[(3) - (4)].filename_list)->next)
			{
			  (yyvsp[(3) - (4)].filename_list)->next->group_start = 1;
			  (yyvsp[(3) - (4)].filename_list)->group_end = 1;
			}
		      (yyval.filename_list) = (yyvsp[(3) - (4)].filename_list);
		    }
    break;

  case 50:
#line 388 "ldscript.y"
    { (yyval.filename_list) = mark_as_needed ((yyvsp[(3) - (4)].filename_list)); }
    break;

  case 51:
#line 390 "ldscript.y"
    { (yyval.filename_list) = new_filename_listelem ((yyvsp[(1) - (1)].str)); }
    break;

  case 52:
#line 395 "ldscript.y"
    {
		      (yyvsp[(2) - (2)].version)->next = (yyvsp[(1) - (2)].version)->next;
		      (yyval.version) = (yyvsp[(1) - (2)].version)->next = (yyvsp[(2) - (2)].version);
		    }
    break;

  case 53:
#line 400 "ldscript.y"
    { (yyval.version) = (yyvsp[(1) - (1)].version); }
    break;

  case 54:
#line 404 "ldscript.y"
    {
		      (yyvsp[(2) - (4)].version)->versionname = "";
		      (yyvsp[(2) - (4)].version)->parentname = NULL;
		      (yyval.version) = (yyvsp[(2) - (4)].version);
		    }
    break;

  case 55:
#line 410 "ldscript.y"
    {
		      (yyvsp[(3) - (5)].version)->versionname = (yyvsp[(1) - (5)].str);
		      (yyvsp[(3) - (5)].version)->parentname = NULL;
		      (yyval.version) = (yyvsp[(3) - (5)].version);
		    }
    break;

  case 56:
#line 416 "ldscript.y"
    {
		      (yyvsp[(3) - (6)].version)->versionname = (yyvsp[(1) - (6)].str);
		      (yyvsp[(3) - (6)].version)->parentname = (yyvsp[(5) - (6)].str);
		      (yyval.version) = (yyvsp[(3) - (6)].version);
		    }
    break;

  case 57:
#line 425 "ldscript.y"
    { (yyval.version) = merge_versions ((yyvsp[(1) - (2)].version), (yyvsp[(2) - (2)].version)); }
    break;

  case 58:
#line 427 "ldscript.y"
    { (yyval.version) = (yyvsp[(1) - (1)].version); }
    break;

  case 59:
#line 431 "ldscript.y"
    { (yyval.version) = new_version (NULL, (yyvsp[(2) - (2)].id_list)); }
    break;

  case 60:
#line 433 "ldscript.y"
    { (yyval.version) = new_version ((yyvsp[(2) - (2)].id_list), NULL); }
    break;

  case 61:
#line 438 "ldscript.y"
    {
		      struct id_list *newp = new_id_listelem ((yyvsp[(2) - (3)].str));
		      newp->next = (yyvsp[(1) - (3)].id_list)->next;
		      (yyval.id_list) = (yyvsp[(1) - (3)].id_list)->next = newp;
		    }
    break;

  case 62:
#line 444 "ldscript.y"
    { (yyval.id_list) = new_id_listelem ((yyvsp[(1) - (2)].str)); }
    break;

  case 63:
#line 448 "ldscript.y"
    { (yyval.str) = (yyvsp[(1) - (1)].str); }
    break;

  case 64:
#line 450 "ldscript.y"
    { (yyval.str) = (yyvsp[(1) - (1)].str); }
    break;

  case 65:
#line 454 "ldscript.y"
    { (yyval.str) = (yyvsp[(1) - (1)].str); }
    break;

  case 66:
#line 456 "ldscript.y"
    { (yyval.str) = NULL; }
    break;


/* Line 1267 of yacc.c.  */
#line 2040 "ldscript.c"
      default: break;
    }
  YY_SYMBOL_PRINT ("-> $$ =", yyr1[yyn], &yyval, &yyloc);

  YYPOPSTACK (yylen);
  yylen = 0;
  YY_STACK_PRINT (yyss, yyssp);

  *++yyvsp = yyval;


  /* Now `shift' the result of the reduction.  Determine what state
     that goes to, based on the state we popped back to and the rule
     number reduced by.  */

  yyn = yyr1[yyn];

  yystate = yypgoto[yyn - YYNTOKENS] + *yyssp;
  if (0 <= yystate && yystate <= YYLAST && yycheck[yystate] == *yyssp)
    yystate = yytable[yystate];
  else
    yystate = yydefgoto[yyn - YYNTOKENS];

  goto yynewstate;


/*------------------------------------.
| yyerrlab -- here on detecting error |
`------------------------------------*/
yyerrlab:
  /* If not already recovering from an error, report this error.  */
  if (!yyerrstatus)
    {
      ++yynerrs;
#if ! YYERROR_VERBOSE
      yyerror (YY_("syntax error"));
#else
      {
	YYSIZE_T yysize = yysyntax_error (0, yystate, yychar);
	if (yymsg_alloc < yysize && yymsg_alloc < YYSTACK_ALLOC_MAXIMUM)
	  {
	    YYSIZE_T yyalloc = 2 * yysize;
	    if (! (yysize <= yyalloc && yyalloc <= YYSTACK_ALLOC_MAXIMUM))
	      yyalloc = YYSTACK_ALLOC_MAXIMUM;
	    if (yymsg != yymsgbuf)
	      YYSTACK_FREE (yymsg);
	    yymsg = (char *) YYSTACK_ALLOC (yyalloc);
	    if (yymsg)
	      yymsg_alloc = yyalloc;
	    else
	      {
		yymsg = yymsgbuf;
		yymsg_alloc = sizeof yymsgbuf;
	      }
	  }

	if (0 < yysize && yysize <= yymsg_alloc)
	  {
	    (void) yysyntax_error (yymsg, yystate, yychar);
	    yyerror (yymsg);
	  }
	else
	  {
	    yyerror (YY_("syntax error"));
	    if (yysize != 0)
	      goto yyexhaustedlab;
	  }
      }
#endif
    }



  if (yyerrstatus == 3)
    {
      /* If just tried and failed to reuse look-ahead token after an
	 error, discard it.  */

      if (yychar <= YYEOF)
	{
	  /* Return failure if at end of input.  */
	  if (yychar == YYEOF)
	    YYABORT;
	}
      else
	{
	  yydestruct ("Error: discarding",
		      yytoken, &yylval);
	  yychar = YYEMPTY;
	}
    }

  /* Else will try to reuse look-ahead token after shifting the error
     token.  */
  goto yyerrlab1;


/*---------------------------------------------------.
| yyerrorlab -- error raised explicitly by YYERROR.  |
`---------------------------------------------------*/
yyerrorlab:

  /* Pacify compilers like GCC when the user code never invokes
     YYERROR and the label yyerrorlab therefore never appears in user
     code.  */
  if (/*CONSTCOND*/ 0)
     goto yyerrorlab;

  /* Do not reclaim the symbols of the rule which action triggered
     this YYERROR.  */
  YYPOPSTACK (yylen);
  yylen = 0;
  YY_STACK_PRINT (yyss, yyssp);
  yystate = *yyssp;
  goto yyerrlab1;


/*-------------------------------------------------------------.
| yyerrlab1 -- common code for both syntax error and YYERROR.  |
`-------------------------------------------------------------*/
yyerrlab1:
  yyerrstatus = 3;	/* Each real token shifted decrements this.  */

  for (;;)
    {
      yyn = yypact[yystate];
      if (yyn != YYPACT_NINF)
	{
	  yyn += YYTERROR;
	  if (0 <= yyn && yyn <= YYLAST && yycheck[yyn] == YYTERROR)
	    {
	      yyn = yytable[yyn];
	      if (0 < yyn)
		break;
	    }
	}

      /* Pop the current state because it cannot handle the error token.  */
      if (yyssp == yyss)
	YYABORT;


      yydestruct ("Error: popping",
		  yystos[yystate], yyvsp);
      YYPOPSTACK (1);
      yystate = *yyssp;
      YY_STACK_PRINT (yyss, yyssp);
    }

  if (yyn == YYFINAL)
    YYACCEPT;

  *++yyvsp = yylval;


  /* Shift the error token.  */
  YY_SYMBOL_PRINT ("Shifting", yystos[yyn], yyvsp, yylsp);

  yystate = yyn;
  goto yynewstate;


/*-------------------------------------.
| yyacceptlab -- YYACCEPT comes here.  |
`-------------------------------------*/
yyacceptlab:
  yyresult = 0;
  goto yyreturn;

/*-----------------------------------.
| yyabortlab -- YYABORT comes here.  |
`-----------------------------------*/
yyabortlab:
  yyresult = 1;
  goto yyreturn;

#ifndef yyoverflow
/*-------------------------------------------------.
| yyexhaustedlab -- memory exhaustion comes here.  |
`-------------------------------------------------*/
yyexhaustedlab:
  yyerror (YY_("memory exhausted"));
  yyresult = 2;
  /* Fall through.  */
#endif

yyreturn:
  if (yychar != YYEOF && yychar != YYEMPTY)
     yydestruct ("Cleanup: discarding lookahead",
		 yytoken, &yylval);
  /* Do not reclaim the symbols of the rule which action triggered
     this YYABORT or YYACCEPT.  */
  YYPOPSTACK (yylen);
  YY_STACK_PRINT (yyss, yyssp);
  while (yyssp != yyss)
    {
      yydestruct ("Cleanup: popping",
		  yystos[*yyssp], yyvsp);
      YYPOPSTACK (1);
    }
#ifndef yyoverflow
  if (yyss != yyssa)
    YYSTACK_FREE (yyss);
#endif
#if YYERROR_VERBOSE
  if (yymsg != yymsgbuf)
    YYSTACK_FREE (yymsg);
#endif
  /* Make sure YYID is used.  */
  return YYID (yyresult);
}


#line 459 "ldscript.y"


static void
yyerror (const char *s)
{
  error (0, 0, (ld_scan_version_script
		? gettext ("while reading version script '%s': %s at line %d")
		: gettext ("while reading linker script '%s': %s at line %d")),
	 ldin_fname, gettext (s), ldlineno);
}


static struct expression *
new_expr (int tag)
{
  struct expression *newp = (struct expression *)
    obstack_alloc (&ld_state.smem, sizeof (*newp));

  newp->tag = tag;
  return newp;
}


static struct input_section_name *
new_input_section_name (const char *name, bool sort_flag)
{
  struct input_section_name *newp = (struct input_section_name *)
    obstack_alloc (&ld_state.smem, sizeof (*newp));

  newp->name = name;
  newp->sort_flag = sort_flag;
  return newp;
}


static struct input_rule *
new_input_rule (int tag)
{
  struct input_rule *newp = (struct input_rule *)
    obstack_alloc (&ld_state.smem, sizeof (*newp));

  newp->tag = tag;
  newp->next = newp;
  return newp;
}


static struct output_rule *
new_output_rule (int tag)
{
  struct output_rule *newp = (struct output_rule *)
    memset (obstack_alloc (&ld_state.smem, sizeof (*newp)),
	    '\0', sizeof (*newp));

  newp->tag = tag;
  newp->next = newp;
  return newp;
}


static struct assignment *
new_assignment (const char *variable, struct expression *expression,
		bool provide_flag)
{
  struct assignment *newp = (struct assignment *)
    obstack_alloc (&ld_state.smem, sizeof (*newp));

  newp->variable = variable;
  newp->expression = expression;
  newp->sym = NULL;
  newp->provide_flag = provide_flag;

  /* Insert the symbol into a hash table.  We will later have to matc*/
  return newp;
}


static void
new_segment (int mode, struct output_rule *output_rule)
{
  struct output_segment *newp;

  newp
    = (struct output_segment *) obstack_alloc (&ld_state.smem, sizeof (*newp));
  newp->mode = mode;
  newp->next = newp;

  newp->output_rules = output_rule->next;
  output_rule->next = NULL;

  /* Enqueue the output segment description.  */
  if (ld_state.output_segments == NULL)
    ld_state.output_segments = newp;
  else
    {
      newp->next = ld_state.output_segments->next;
      ld_state.output_segments = ld_state.output_segments->next = newp;
    }

  /* If the output file should be stripped of all symbol set the flag
     in the structures of all output sections.  */
  if (mode == 0 && ld_state.strip == strip_all)
    {
      struct output_rule *runp;

      for (runp = newp->output_rules; runp != NULL; runp = runp->next)
	if (runp->tag == output_section)
	  runp->val.section.ignored = true;
    }
}


static struct filename_list *
new_filename_listelem (const char *string)
{
  struct filename_list *newp;

  /* We use calloc and not the obstack since this object can be freed soon.  */
  newp = (struct filename_list *) xcalloc (1, sizeof (*newp));
  newp->name = string;
  newp->next = newp;
  return newp;
}


static struct filename_list *
mark_as_needed (struct filename_list *listp)
{
  struct filename_list *runp = listp;
  do
    {
      runp->as_needed = true;
      runp = runp->next;
    }
  while (runp != listp);

  return listp;
}


static void
add_inputfiles (struct filename_list *fnames)
{
  assert (fnames != NULL);

  if (ld_state.srcfiles == NULL)
    ld_state.srcfiles = fnames;
  else
    {
      struct filename_list *first = ld_state.srcfiles->next;

      ld_state.srcfiles->next = fnames->next;
      fnames->next = first;
      ld_state.srcfiles->next = fnames;
    }
}


static _Bool
special_char_p (const char *str)
{
  while (*str != '\0')
    {
      if (__builtin_expect (*str == '*', 0)
	  || __builtin_expect (*str == '?', 0)
	  || __builtin_expect (*str == '[', 0))
	return true;

      ++str;
    }

  return false;
}


static struct id_list *
new_id_listelem (const char *str)
{
  struct id_list *newp;

  newp = (struct id_list *) obstack_alloc (&ld_state.smem, sizeof (*newp));
  if (str == NULL)
    newp->u.id_type = id_all;
  else if (__builtin_expect (special_char_p (str), false))
    newp->u.id_type = id_wild;
  else
    newp->u.id_type = id_str;
  newp->id = str;
  newp->next = newp;

  return newp;
}


static struct version *
new_version (struct id_list *local, struct id_list *global)
{
  struct version *newp;

  newp = (struct version *) obstack_alloc (&ld_state.smem, sizeof (*newp));
  newp->next = newp;
  newp->local_names = local;
  newp->global_names = global;
  newp->versionname = NULL;
  newp->parentname = NULL;

  return newp;
}


static struct version *
merge_versions (struct version *one, struct version *two)
{
  assert (two->local_names == NULL || two->global_names == NULL);

  if (two->local_names != NULL)
    {
      if (one->local_names == NULL)
	one->local_names = two->local_names;
      else
	{
	  two->local_names->next = one->local_names->next;
	  one->local_names = one->local_names->next = two->local_names;
	}
    }
  else
    {
      if (one->global_names == NULL)
	one->global_names = two->global_names;
      else
	{
	  two->global_names->next = one->global_names->next;
	  one->global_names = one->global_names->next = two->global_names;
	}
    }

  return one;
}


static void
add_id_list (const char *versionname, struct id_list *runp, _Bool local)
{
  struct id_list *lastp = runp;

  if (runp == NULL)
    /* Nothing to do.  */
    return;

  /* Convert into a simple single-linked list.  */
  runp = runp->next;
  assert (runp != NULL);
  lastp->next = NULL;

  do
    if (runp->u.id_type == id_str)
      {
	struct id_list *curp;
	struct id_list *defp;
	unsigned long int hval = elf_hash (runp->id);

	curp = runp;
	runp = runp->next;

	defp = ld_version_str_tab_find (&ld_state.version_str_tab, hval, curp);
	if (defp != NULL)
	  {
	    /* There is already a version definition for this symbol.  */
	    while (strcmp (defp->u.s.versionname, versionname) != 0)
	      {
		if (defp->next == NULL)
		  {
		    /* No version like this so far.  */
		    defp->next = curp;
		    curp->u.s.local = local;
		    curp->u.s.versionname = versionname;
		    curp->next = NULL;
		    defp = NULL;
		    break;
		  }

		defp = defp->next;
	      }

	    if (defp != NULL && defp->u.s.local != local)
	      error (EXIT_FAILURE, 0, versionname[0] == '\0'
		     ? gettext ("\
symbol '%s' in declared both local and global for unnamed version")
		     : gettext ("\
symbol '%s' in declared both local and global for version '%s'"),
		     runp->id, versionname);
	  }
	else
	  {
	    /* This is the first version definition for this symbol.  */
	    ld_version_str_tab_insert (&ld_state.version_str_tab, hval, curp);

	    curp->u.s.local = local;
	    curp->u.s.versionname = versionname;
	    curp->next = NULL;
	  }
      }
    else if (runp->u.id_type == id_all)
      {
	if (local)
	  {
	    if (ld_state.default_bind_global)
	      error (EXIT_FAILURE, 0,
		     gettext ("default visibility set as local and global"));
	    ld_state.default_bind_local = true;
	  }
	else
	  {
	    if (ld_state.default_bind_local)
	      error (EXIT_FAILURE, 0,
		     gettext ("default visibility set as local and global"));
	    ld_state.default_bind_global = true;
	  }

	runp = runp->next;
      }
    else
      {
	assert (runp->u.id_type == id_wild);
	/* XXX TBI */
	abort ();
      }
  while (runp != NULL);
}


static void
add_versions (struct version *versions)
{
  struct version *lastp = versions;

  if (versions == NULL)
    return;

  /* Convert into a simple single-linked list.  */
  versions = versions->next;
  assert (versions != NULL);
  lastp->next = NULL;

  do
    {
      struct version *oldp;

      add_id_list (versions->versionname, versions->local_names, true);
      add_id_list (versions->versionname, versions->global_names, false);

      oldp = versions;
      versions = versions->next;
    }
  while (versions != NULL);
}

