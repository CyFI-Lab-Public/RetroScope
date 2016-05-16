//===- DefSymParser.cpp ---------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Support/DefSymParser.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/LD/LDSymbol.h>

using namespace llvm;
using namespace mcld;

DefSymParser::DefSymParser(const Module& pModule)
  : m_Module(pModule) {
}

// passing a valid operator will return a number whose quantity relative
// to other such obtained quantities will give the priority of the operator
static inline int precedence(const char* x)
{
  switch (*x) {
    case '-' :
    case '+' : return 0;
    case '/' :
    case '*' : return 1;
    default  : assert("Unsupported operator specified");
  }
  return 0;
}

bool DefSymParser::parse(StringRef pExpr, uint64_t& pSymVal)
{
  std::stack<const char*> operatorStack;
  std::stack<unsigned long> operandStack;
  unsigned long operand1 = 0,
                operand2 = 0,
                result = 0;
  std::string token;
  std::vector<std::string> postfixString;
  std::vector<std::string>::iterator it;
  llvm::StringRef::iterator si = pExpr.begin();

  // Implement a modified Shunting Yard algorithm to form a RPN of the
  // given expression
  while (si != pExpr.end()) {
    if (*si == '+' || *si == '-' || *si == '*' || *si == '/') {
      if (token.empty() && (*si == '+' || *si == '-'))
        // we have a case such as a++b or a+-b or a-+b
        // pushing 0 when a token begins with a + or - operator
        // solves unary operator problem
        token = "0";
      // An operator encountered means a token ended, push it to
      // postfix string queue.
      postfixString.push_back(token);
      token.clear();

      if (operatorStack.empty()) {
        operatorStack.push(si);
      }
      else {
        if (precedence(si) <= precedence(operatorStack.top())) {
          // if the precedence of incoming operator is less or equal to
          // top of stack, we clear stack till top is lower precedence
          // or its empty
          while (!operatorStack.empty()) {
            if (precedence(si) <= precedence(operatorStack.top())) {
            postfixString.push_back(std::string(operatorStack.top(),1));
            operatorStack.pop();
            }
            else {
              break;
            }
          }
        }
        operatorStack.push(si);
      }
      si++;
      continue;
    }
    // keep reading the token when there is no operator encountered
    token += *si;
    si++;
  }
  postfixString.push_back(token);
  // pop off any remaining operators from operator stack
  while (!operatorStack.empty()) {
    postfixString.push_back(std::string(operatorStack.top(),1));
    operatorStack.pop();
  }
  //evaluate the postfix expression written above

  for (it=postfixString.begin(); it != postfixString.end(); it++) {
    switch (*((*it).c_str())) {
      case '*':
      case '-':
      case '+':
      case '/':
        // when postfix string has an operator, pop first two operands from
        // operand stack, use them in evaluate expression and push result
        // back to stack
        assert(!operandStack.empty() && "Invalid expression: extra operand");
        operand2 = operandStack.top();
        operandStack.pop();
        operand1 = operandStack.top();
        operandStack.pop();
        if (*((*it).c_str()) == '*')
          result = operand1 * operand2;
        else if (*((*it).c_str()) == '/')
          result = operand1 / operand2;
        else if (*((*it).c_str()) == '-')
          result = operand1 - operand2;
        else
          result = operand1 + operand2;
        operandStack.push(result);
        break;
      default:
        // if the string encountered in postfix queue is a string
        // try converting it to integer.
        llvm::StringRef stringOperand(*it);
        if(stringOperand.getAsInteger(0,result)) {
          // the integer conversion failed means the token is a symbol
          // or its invalid if the NamePool has no such symbol;
          const LDSymbol* symbol =
                              m_Module.getNamePool().findSymbol(stringOperand);

          if (!symbol)
            fatal(diag::fail_sym_resolution)
                  << __FILE__ << __LINE__
                  << "mclinker@googlegroups.com" ;
          result = symbol->value();
        }
        operandStack.push(result);
    }
  }
  // once complete queue is processed, stack top is result
  pSymVal = operandStack.top();
  return true;
}
