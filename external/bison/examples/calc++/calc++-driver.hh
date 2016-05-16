#line 10057 "../../doc/bison.texi"
#ifndef CALCXX_DRIVER_HH
# define CALCXX_DRIVER_HH
# include <string>
# include <map>
# include "calc++-parser.hh"
#line 10073 "../../doc/bison.texi"
// Tell Flex the lexer's prototype ...
# define YY_DECL                                        \
  yy::calcxx_parser::token_type                         \
  yylex (yy::calcxx_parser::semantic_type* yylval,      \
         yy::calcxx_parser::location_type* yylloc,      \
         calcxx_driver& driver)
// ... and declare it for the parser's sake.
YY_DECL;
#line 10089 "../../doc/bison.texi"
// Conducting the whole scanning and parsing of Calc++.
class calcxx_driver
{
public:
  calcxx_driver ();
  virtual ~calcxx_driver ();

  std::map<std::string, int> variables;

  int result;
#line 10107 "../../doc/bison.texi"
  // Handling the scanner.
  void scan_begin ();
  void scan_end ();
  bool trace_scanning;
#line 10118 "../../doc/bison.texi"
  // Run the parser.  Return 0 on success.
  int parse (const std::string& f);
  std::string file;
  bool trace_parsing;
#line 10132 "../../doc/bison.texi"
  // Error handling.
  void error (const yy::location& l, const std::string& m);
  void error (const std::string& m);
};
#endif // ! CALCXX_DRIVER_HH
