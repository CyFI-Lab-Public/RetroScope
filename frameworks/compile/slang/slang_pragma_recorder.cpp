/*
 * Copyright 2010, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "slang_pragma_recorder.h"

#include <string>

#include "clang/Basic/TokenKinds.h"

#include "clang/Lex/Preprocessor.h"
#include "clang/Lex/Token.h"

namespace slang {

bool PragmaRecorder::GetPragmaNameFromToken(const clang::Token &Token,
                                            std::string &PragmaName) {
  if (Token.isLiteral())
    PragmaName.assign(Token.getLiteralData(), Token.getLength());
  else if (Token.is(clang::tok::identifier))
    PragmaName.assign(Token.getIdentifierInfo()->getNameStart(),
                      Token.getIdentifierInfo()->getLength());
  else
    return false;
  return true;
}

bool PragmaRecorder::GetPragmaValueFromToken(const clang::Token &Token,
                                             std::string &PragmaValue) {
  // Same as the GetPragmaName()
  if (Token.is(clang::tok::r_paren))
    PragmaValue.clear();
  else
    return GetPragmaNameFromToken(Token, PragmaValue);
  return true;
}

PragmaRecorder::PragmaRecorder(PragmaList *Pragmas)
    : PragmaHandler(),
      mPragmas(Pragmas) {
  return;
}

void PragmaRecorder::HandlePragma(clang::Preprocessor &PP,
                                  clang::PragmaIntroducerKind Introducer,
                                  clang::Token &FirstToken) {
  clang::Token &CurrentToken = FirstToken;
  std::string PragmaName, PragmaValue = "";
  // Pragma in ACC should be a name/value pair

  if (GetPragmaNameFromToken(FirstToken, PragmaName)) {
    // start parsing the value '(' PragmaValue ')', if we have one.
    const clang::Token* NextToken = &PP.LookAhead(0);

    if (NextToken->is(clang::tok::l_paren))
      PP.LexUnexpandedToken(CurrentToken);
    else
      goto end_parsing_pragma_value;

    NextToken = &PP.LookAhead(0);
    if (GetPragmaValueFromToken(*NextToken, PragmaValue)) {
      PP.Lex(CurrentToken);
    } else {
      PP.LexUnexpandedToken(CurrentToken);
      PP.Diag(NextToken->getLocation(),
              PP.getDiagnostics().getCustomDiagID(
                  clang::DiagnosticsEngine::Error,
                  "expected value after '#pragma %0('")) << PragmaName;
      return;
    }

    if (!NextToken->is(clang::tok::r_paren)) {
      NextToken = &PP.LookAhead(0);
      if (NextToken->is(clang::tok::r_paren)) {
        PP.LexUnexpandedToken(CurrentToken);
      } else {
        PP.LexUnexpandedToken(CurrentToken);
        PP.Diag(NextToken->getLocation(),
                PP.getDiagnostics().getCustomDiagID(
                    clang::DiagnosticsEngine::Error,
                    "missing ')' after '#pragma %0(%1'"))
            << PragmaName << PragmaValue;
        return;
      }
    }
  } else {
    PP.Diag(FirstToken.getLocation(),
            PP.getDiagnostics().getCustomDiagID(
                clang::DiagnosticsEngine::Error,
                "no pragma name or value"));
    return;
  }

 end_parsing_pragma_value:

  // PragmaValue may be an empty string.
  mPragmas->push_back(make_pair(PragmaName, PragmaValue));

  // Inform lex to eat the token
  PP.LexUnexpandedToken(CurrentToken);

  return;
}

}  // namespace slang
