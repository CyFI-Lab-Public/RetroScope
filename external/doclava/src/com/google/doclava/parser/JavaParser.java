/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.doclava.parser;

import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.BitSet;
import org.antlr.runtime.DFA;
import org.antlr.runtime.EarlyExitException;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.MismatchedSetException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.debug.DebugEventListener;
import org.antlr.runtime.debug.DebugEventSocketProxy;
import org.antlr.runtime.debug.DebugParser;

import java.io.IOException;
import java.util.HashMap;
/** A Java 1.5 grammar for ANTLR v3 derived from the spec
 *
 *  This is a very close representation of the spec; the changes
 *  are comestic (remove left recursion) and also fixes (the spec
 *  isn't exactly perfect).  I have run this on the 1.4.2 source
 *  and some nasty looking enums from 1.5, but have not really
 *  tested for 1.5 compatibility.
 *
 *  I built this with: java -Xmx100M org.antlr.Tool java.g
 *  and got two errors that are ok (for now):
 *  java.g:691:9: Decision can match input such as
 *    "'0'..'9'{'E', 'e'}{'+', '-'}'0'..'9'{'D', 'F', 'd', 'f'}"
 *    using multiple alternatives: 3, 4
 *  As a result, alternative(s) 4 were disabled for that input
 *  java.g:734:35: Decision can match input such as "{'$', 'A'..'Z',
 *    '_', 'a'..'z', '\u00C0'..'\u00D6', '\u00D8'..'\u00F6',
 *    '\u00F8'..'\u1FFF', '\u3040'..'\u318F', '\u3300'..'\u337F',
 *    '\u3400'..'\u3D2D', '\u4E00'..'\u9FFF', '\uF900'..'\uFAFF'}"
 *    using multiple alternatives: 1, 2
 *  As a result, alternative(s) 2 were disabled for that input
 *
 *  You can turn enum on/off as a keyword :)
 *
 *  Version 1.0 -- initial release July 5, 2006 (requires 3.0b2 or higher)
 *
 *  Primary author: Terence Parr, July 2006
 *
 *  Version 1.0.1 -- corrections by Koen Vanderkimpen & Marko van Dooren,
 *      October 25, 2006;
 *      fixed normalInterfaceDeclaration: now uses typeParameters instead
 *          of typeParameter (according to JLS, 3rd edition)
 *      fixed castExpression: no longer allows expression next to type
 *          (according to semantics in JLS, in contrast with syntax in JLS)
 *
 *  Version 1.0.2 -- Terence Parr, Nov 27, 2006
 *      java spec I built this from had some bizarre for-loop control.
 *          Looked weird and so I looked elsewhere...Yep, it's messed up.
 *          simplified.
 *
 *  Version 1.0.3 -- Chris Hogue, Feb 26, 2007
 *      Factored out an annotationName rule and used it in the annotation rule.
 *          Not sure why, but typeName wasn't recognizing references to inner
 *          annotations (e.g. @InterfaceName.InnerAnnotation())
 *      Factored out the elementValue section of an annotation reference.  Created
 *          elementValuePair and elementValuePairs rules, then used them in the
 *          annotation rule.  Allows it to recognize annotation references with
 *          multiple, comma separated attributes.
 *      Updated elementValueArrayInitializer so that it allows multiple elements.
 *          (It was only allowing 0 or 1 element).
 *      Updated localVariableDeclaration to allow annotations.  Interestingly the JLS
 *          doesn't appear to indicate this is legal, but it does work as of at least
 *          JDK 1.5.0_06.
 *      Moved the Identifier portion of annotationTypeElementRest to annotationMethodRest.
 *          Because annotationConstantRest already references variableDeclarator which
 *          has the Identifier portion in it, the parser would fail on constants in
 *          annotation definitions because it expected two identifiers.
 *      Added optional trailing ';' to the alternatives in annotationTypeElementRest.
 *          Wouldn't handle an inner interface that has a trailing ';'.
 *      Swapped the expression and type rule reference order in castExpression to
 *          make it check for genericized casts first.  It was failing to recognize a
 *          statement like  "Class<Byte> TYPE = (Class<Byte>)...;" because it was seeing
 *          'Class<Byte' in the cast expression as a less than expression, then failing
 *          on the '>'.
 *      Changed createdName to use typeArguments instead of nonWildcardTypeArguments.
 *
 *      Changed the 'this' alternative in primary to allow 'identifierSuffix' rather than
 *          just 'arguments'.  The case it couldn't handle was a call to an explicit
 *          generic method invocation (e.g. this.<E>doSomething()).  Using identifierSuffix
 *          may be overly aggressive--perhaps should create a more constrained thisSuffix rule?
 *
 *  Version 1.0.4 -- Hiroaki Nakamura, May 3, 2007
 *
 *  Fixed formalParameterDecls, localVariableDeclaration, forInit,
 *  and forVarControl to use variableModifier* not 'final'? (annotation)?
 *
 *  Version 1.0.5 -- Terence, June 21, 2007
 *  --a[i].foo didn't work. Fixed unaryExpression
 *
 *  Version 1.0.6 -- John Ridgway, March 17, 2008
 *      Made "assert" a switchable keyword like "enum".
 *      Fixed compilationUnit to disallow "annotation importDeclaration ...".
 *      Changed "Identifier ('.' Identifier)*" to "qualifiedName" in more
 *          places.
 *      Changed modifier* and/or variableModifier* to classOrInterfaceModifiers,
 *          modifiers or variableModifiers, as appropriate.
 *      Renamed "bound" to "typeBound" to better match language in the JLS.
 *      Added "memberDeclaration" which rewrites to methodDeclaration or
 *      fieldDeclaration and pulled type into memberDeclaration.  So we parse
 *          type and then move on to decide whether we're dealing with a field
 *          or a method.
 *      Modified "constructorDeclaration" to use "constructorBody" instead of
 *          "methodBody".  constructorBody starts with explicitConstructorInvocation,
 *          then goes on to blockStatement*.  Pulling explicitConstructorInvocation
 *          out of expressions allowed me to simplify "primary".
 *      Changed variableDeclarator to simplify it.
 *      Changed type to use classOrInterfaceType, thus simplifying it; of course
 *          I then had to add classOrInterfaceType, but it is used in several
 *          places.
 *      Fixed annotations, old version allowed "@X(y,z)", which is illegal.
 *      Added optional comma to end of "elementValueArrayInitializer"; as per JLS.
 *      Changed annotationTypeElementRest to use normalClassDeclaration and
 *          normalInterfaceDeclaration rather than classDeclaration and
 *          interfaceDeclaration, thus getting rid of a couple of grammar ambiguities.
 *      Split localVariableDeclaration into localVariableDeclarationStatement
 *          (includes the terminating semi-colon) and localVariableDeclaration.
 *          This allowed me to use localVariableDeclaration in "forInit" clauses,
 *           simplifying them.
 *      Changed switchBlockStatementGroup to use multiple labels.  This adds an
 *          ambiguity, but if one uses appropriately greedy parsing it yields the
 *           parse that is closest to the meaning of the switch statement.
 *      Renamed "forVarControl" to "enhancedForControl" -- JLS language.
 *      Added semantic predicates to test for shift operations rather than other
 *          things.  Thus, for instance, the string "< <" will never be treated
 *          as a left-shift operator.
 *      In "creator" we rule out "nonWildcardTypeArguments" on arrayCreation,
 *          which are illegal.
 *      Moved "nonWildcardTypeArguments into innerCreator.
 *      Removed 'super' superSuffix from explicitGenericInvocation, since that
 *          is only used in explicitConstructorInvocation at the beginning of a
 *           constructorBody.  (This is part of the simplification of expressions
 *           mentioned earlier.)
 *      Simplified primary (got rid of those things that are only used in
 *          explicitConstructorInvocation).
 *      Lexer -- removed "Exponent?" from FloatingPointLiteral choice 4, since it
 *          led to an ambiguity.
 *
 *      This grammar successfully parses every .java file in the JDK 1.5 source
 *          tree (excluding those whose file names include '-', which are not
 *          valid Java compilation units).
 *
 *  Known remaining problems:
 *      "Letter" and "JavaIDDigit" are wrong.  The actual specification of
 *      "Letter" should be "a character for which the method
 *      Character.isJavaIdentifierStart(int) returns true."  A "Java
 *      letter-or-digit is a character for which the method
 *      Character.isJavaIdentifierPart(int) returns true."
 */
public class JavaParser extends DebugParser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "IDENTIFIER", "INTLITERAL", "LONGLITERAL", "FLOATLITERAL", "DOUBLELITERAL", "CHARLITERAL", "STRINGLITERAL", "TRUE", "FALSE", "NULL", "IntegerNumber", "LongSuffix", "HexPrefix", "HexDigit", "Exponent", "NonIntegerNumber", "FloatSuffix", "DoubleSuffix", "EscapeSequence", "UNICODECHAR", "UNICODEPART", "WS", "COMMENT", "LINE_COMMENT", "ABSTRACT", "ASSERT", "BOOLEAN", "BREAK", "BYTE", "CASE", "CATCH", "CHAR", "CLASS", "CONST", "CONTINUE", "DEFAULT", "DO", "DOUBLE", "ELSE", "ENUM", "EXTENDS", "FINAL", "FINALLY", "FLOAT", "FOR", "GOTO", "IF", "IMPLEMENTS", "IMPORT", "INSTANCEOF", "INT", "INTERFACE", "LONG", "NATIVE", "NEW", "PACKAGE", "PRIVATE", "PROTECTED", "PUBLIC", "RETURN", "SHORT", "STATIC", "STRICTFP", "SUPER", "SWITCH", "SYNCHRONIZED", "THIS", "THROW", "THROWS", "TRANSIENT", "TRY", "VOID", "VOLATILE", "WHILE", "LPAREN", "RPAREN", "LBRACE", "RBRACE", "LBRACKET", "RBRACKET", "SEMI", "COMMA", "DOT", "ELLIPSIS", "EQ", "BANG", "TILDE", "QUES", "COLON", "EQEQ", "AMPAMP", "BARBAR", "PLUSPLUS", "SUBSUB", "PLUS", "SUB", "STAR", "SLASH", "AMP", "BAR", "CARET", "PERCENT", "PLUSEQ", "SUBEQ", "STAREQ", "SLASHEQ", "AMPEQ", "BAREQ", "CARETEQ", "PERCENTEQ", "MONKEYS_AT", "BANGEQ", "GT", "LT", "IdentifierStart", "IdentifierPart", "SurrogateIdentifer"
    };
    public static final int EOF=-1;
    public static final int IDENTIFIER=4;
    public static final int INTLITERAL=5;
    public static final int LONGLITERAL=6;
    public static final int FLOATLITERAL=7;
    public static final int DOUBLELITERAL=8;
    public static final int CHARLITERAL=9;
    public static final int STRINGLITERAL=10;
    public static final int TRUE=11;
    public static final int FALSE=12;
    public static final int NULL=13;
    public static final int IntegerNumber=14;
    public static final int LongSuffix=15;
    public static final int HexPrefix=16;
    public static final int HexDigit=17;
    public static final int Exponent=18;
    public static final int NonIntegerNumber=19;
    public static final int FloatSuffix=20;
    public static final int DoubleSuffix=21;
    public static final int EscapeSequence=22;
    public static final int UNICODECHAR=23;
    public static final int UNICODEPART=24;
    public static final int WS=25;
    public static final int COMMENT=26;
    public static final int LINE_COMMENT=27;
    public static final int ABSTRACT=28;
    public static final int ASSERT=29;
    public static final int BOOLEAN=30;
    public static final int BREAK=31;
    public static final int BYTE=32;
    public static final int CASE=33;
    public static final int CATCH=34;
    public static final int CHAR=35;
    public static final int CLASS=36;
    public static final int CONST=37;
    public static final int CONTINUE=38;
    public static final int DEFAULT=39;
    public static final int DO=40;
    public static final int DOUBLE=41;
    public static final int ELSE=42;
    public static final int ENUM=43;
    public static final int EXTENDS=44;
    public static final int FINAL=45;
    public static final int FINALLY=46;
    public static final int FLOAT=47;
    public static final int FOR=48;
    public static final int GOTO=49;
    public static final int IF=50;
    public static final int IMPLEMENTS=51;
    public static final int IMPORT=52;
    public static final int INSTANCEOF=53;
    public static final int INT=54;
    public static final int INTERFACE=55;
    public static final int LONG=56;
    public static final int NATIVE=57;
    public static final int NEW=58;
    public static final int PACKAGE=59;
    public static final int PRIVATE=60;
    public static final int PROTECTED=61;
    public static final int PUBLIC=62;
    public static final int RETURN=63;
    public static final int SHORT=64;
    public static final int STATIC=65;
    public static final int STRICTFP=66;
    public static final int SUPER=67;
    public static final int SWITCH=68;
    public static final int SYNCHRONIZED=69;
    public static final int THIS=70;
    public static final int THROW=71;
    public static final int THROWS=72;
    public static final int TRANSIENT=73;
    public static final int TRY=74;
    public static final int VOID=75;
    public static final int VOLATILE=76;
    public static final int WHILE=77;
    public static final int LPAREN=78;
    public static final int RPAREN=79;
    public static final int LBRACE=80;
    public static final int RBRACE=81;
    public static final int LBRACKET=82;
    public static final int RBRACKET=83;
    public static final int SEMI=84;
    public static final int COMMA=85;
    public static final int DOT=86;
    public static final int ELLIPSIS=87;
    public static final int EQ=88;
    public static final int BANG=89;
    public static final int TILDE=90;
    public static final int QUES=91;
    public static final int COLON=92;
    public static final int EQEQ=93;
    public static final int AMPAMP=94;
    public static final int BARBAR=95;
    public static final int PLUSPLUS=96;
    public static final int SUBSUB=97;
    public static final int PLUS=98;
    public static final int SUB=99;
    public static final int STAR=100;
    public static final int SLASH=101;
    public static final int AMP=102;
    public static final int BAR=103;
    public static final int CARET=104;
    public static final int PERCENT=105;
    public static final int PLUSEQ=106;
    public static final int SUBEQ=107;
    public static final int STAREQ=108;
    public static final int SLASHEQ=109;
    public static final int AMPEQ=110;
    public static final int BAREQ=111;
    public static final int CARETEQ=112;
    public static final int PERCENTEQ=113;
    public static final int MONKEYS_AT=114;
    public static final int BANGEQ=115;
    public static final int GT=116;
    public static final int LT=117;
    public static final int IdentifierStart=118;
    public static final int IdentifierPart=119;
    public static final int SurrogateIdentifer=120;

    // delegates
    // delegators

    public static final String[] ruleNames = new String[] {
        "invalidRule", "typeList", "synpred114_Java", "synpred175_Java",
        "synpred19_Java", "elementValuePairs", "identifierSuffix", "interfaceFieldDeclaration",
        "synpred69_Java", "synpred263_Java", "synpred231_Java", "synpred267_Java",
        "synpred111_Java", "block", "synpred261_Java", "elementValuePair",
        "typeArgument", "synpred264_Java", "synpred95_Java", "synpred93_Java",
        "synpred215_Java", "normalInterfaceDeclaration", "enumHeader", "synpred236_Java",
        "createdName", "synpred271_Java", "synpred230_Java", "synpred30_Java",
        "synpred212_Java", "synpred82_Java", "synpred128_Java", "synpred83_Java",
        "synpred255_Java", "synpred190_Java", "arrayInitializer", "interfaceDeclaration",
        "synpred92_Java", "localVariableHeader", "packageDeclaration", "formalParameter",
        "catchClause", "synpred27_Java", "synpred270_Java", "synpred46_Java",
        "synpred1_Java", "synpred4_Java", "synpred233_Java", "synpred120_Java",
        "superSuffix", "literal", "classDeclaration", "synpred72_Java",
        "synpred160_Java", "arguments", "synpred80_Java", "formalParameterDecls",
        "synpred113_Java", "inclusiveOrExpression", "synpred71_Java", "selector",
        "synpred194_Java", "synpred265_Java", "synpred173_Java", "synpred141_Java",
        "synpred187_Java", "trystatement", "synpred133_Java", "interfaceHeader",
        "synpred73_Java", "localVariableDeclarationStatement", "synpred102_Java",
        "synpred90_Java", "equalityExpression", "synpred177_Java", "synpred149_Java",
        "interfaceBodyDeclaration", "classCreatorRest", "synpred121_Java",
        "synpred105_Java", "typeArguments", "synpred60_Java", "synpred195_Java",
        "fieldDeclaration", "synpred269_Java", "synpred250_Java", "multiplicativeExpression",
        "qualifiedNameList", "synpred86_Java", "synpred148_Java", "synpred142_Java",
        "synpred65_Java", "synpred75_Java", "synpred235_Java", "synpred192_Java",
        "synpred144_Java", "castExpression", "enumBody", "synpred70_Java",
        "synpred33_Java", "synpred54_Java", "annotationTypeDeclaration",
        "annotationHeader", "synpred107_Java", "synpred35_Java", "creator",
        "nonWildcardTypeArguments", "variableInitializer", "enumConstants",
        "synpred34_Java", "interfaceMethodDeclaration", "type", "synpred135_Java",
        "synpred119_Java", "conditionalAndExpression", "synpred9_Java",
        "synpred125_Java", "synpred40_Java", "synpred257_Java", "enumConstant",
        "synpred143_Java", "synpred132_Java", "synpred146_Java", "synpred188_Java",
        "ellipsisParameterDecl", "synpred245_Java", "synpred167_Java", "compilationUnit",
        "synpred259_Java", "synpred64_Java", "synpred181_Java", "synpred23_Java",
        "synpred12_Java", "synpred74_Java", "explicitConstructorInvocation",
        "synpred266_Java", "synpred197_Java", "synpred147_Java", "synpred15_Java",
        "synpred178_Java", "synpred174_Java", "exclusiveOrExpression", "forstatement",
        "synpred7_Java", "synpred76_Java", "synpred224_Java", "parExpression",
        "synpred241_Java", "synpred159_Java", "synpred260_Java", "synpred50_Java",
        "synpred166_Java", "annotationMethodDeclaration", "synpred208_Java",
        "synpred106_Java", "classOrInterfaceType", "qualifiedImportName",
        "statement", "typeBound", "methodHeader", "synpred249_Java", "synpred55_Java",
        "synpred131_Java", "classBodyDeclaration", "synpred189_Java", "synpred51_Java",
        "synpred227_Java", "synpred220_Java", "synpred123_Java", "andExpression",
        "synpred200_Java", "synpred165_Java", "relationalExpression", "annotationTypeBody",
        "synpred210_Java", "synpred109_Java", "conditionalOrExpression",
        "synpred161_Java", "classOrInterfaceDeclaration", "synpred180_Java",
        "synpred154_Java", "elementValueArrayInitializer", "synpred14_Java",
        "innerCreator", "synpred26_Java", "synpred52_Java", "synpred198_Java",
        "synpred219_Java", "synpred126_Java", "synpred85_Java", "synpred88_Java",
        "synpred68_Java", "synpred3_Java", "synpred203_Java", "annotations",
        "elementValue", "synpred205_Java", "synpred6_Java", "synpred32_Java",
        "synpred209_Java", "assignmentOperator", "synpred262_Java", "synpred139_Java",
        "synpred29_Java", "synpred204_Java", "synpred118_Java", "synpred94_Java",
        "synpred84_Java", "synpred63_Java", "conditionalExpression", "synpred56_Java",
        "synpred162_Java", "primitiveType", "synpred240_Java", "synpred216_Java",
        "synpred79_Java", "synpred99_Java", "additiveExpression", "synpred78_Java",
        "modifiers", "synpred184_Java", "synpred168_Java", "synpred48_Java",
        "switchBlockStatementGroups", "blockStatement", "synpred193_Java",
        "classBody", "interfaceBody", "synpred67_Java", "synpred5_Java",
        "synpred58_Java", "synpred254_Java", "localVariableDeclaration",
        "annotationTypeElementDeclaration", "synpred251_Java", "arrayCreator",
        "synpred226_Java", "synpred239_Java", "synpred191_Java", "synpred24_Java",
        "normalClassDeclaration", "synpred98_Java", "synpred53_Java", "synpred145_Java",
        "synpred22_Java", "synpred150_Java", "synpred238_Java", "synpred207_Java",
        "variableModifiers", "typeParameters", "synpred38_Java", "synpred129_Java",
        "enumBodyDeclarations", "synpred172_Java", "synpred16_Java", "synpred100_Java",
        "fieldHeader", "synpred41_Java", "synpred248_Java", "synpred152_Java",
        "synpred214_Java", "switchBlockStatementGroup", "synpred199_Java",
        "switchLabel", "qualifiedName", "synpred137_Java", "synpred237_Java",
        "synpred223_Java", "synpred156_Java", "synpred243_Java", "synpred182_Java",
        "synpred138_Java", "synpred77_Java", "synpred127_Java", "synpred112_Java",
        "unaryExpressionNotPlusMinus", "synpred42_Java", "synpred89_Java",
        "formalParameters", "synpred225_Java", "synpred136_Java", "synpred186_Java",
        "synpred122_Java", "synpred87_Java", "synpred244_Java", "synpred97_Java",
        "synpred229_Java", "synpred170_Java", "shiftOp", "synpred134_Java",
        "synpred253_Java", "synpred44_Java", "memberDecl", "synpred157_Java",
        "synpred246_Java", "synpred49_Java", "synpred31_Java", "synpred256_Java",
        "unaryExpression", "synpred13_Java", "synpred213_Java", "synpred155_Java",
        "typeHeader", "synpred91_Java", "instanceOfExpression", "variableDeclarator",
        "synpred140_Java", "synpred25_Java", "synpred117_Java", "synpred2_Java",
        "synpred222_Java", "synpred10_Java", "synpred104_Java", "synpred115_Java",
        "synpred221_Java", "synpred45_Java", "synpred211_Java", "typeParameter",
        "synpred36_Java", "synpred103_Java", "synpred39_Java", "synpred201_Java",
        "methodDeclaration", "synpred62_Java", "synpred110_Java", "classHeader",
        "synpred101_Java", "synpred21_Java", "synpred196_Java", "synpred96_Java",
        "synpred61_Java", "synpred228_Java", "synpred28_Java", "synpred218_Java",
        "synpred179_Java", "normalParameterDecl", "enumDeclaration", "synpred17_Java",
        "synpred18_Java", "synpred108_Java", "synpred43_Java", "synpred206_Java",
        "synpred169_Java", "synpred130_Java", "synpred242_Java", "synpred252_Java",
        "synpred151_Java", "forInit", "shiftExpression", "synpred81_Java",
        "synpred247_Java", "synpred20_Java", "catches", "synpred202_Java",
        "synpred47_Java", "synpred185_Java", "synpred158_Java", "synpred66_Java",
        "synpred11_Java", "synpred8_Java", "synpred163_Java", "synpred217_Java",
        "primary", "synpred153_Java", "synpred57_Java", "synpred258_Java",
        "expressionList", "annotation", "expression", "synpred176_Java",
        "synpred171_Java", "synpred164_Java", "importDeclaration", "synpred124_Java",
        "synpred268_Java", "synpred234_Java", "relationalOp", "synpred59_Java",
        "synpred37_Java", "synpred183_Java", "synpred232_Java", "synpred116_Java",
        "typeDeclaration"
    };
    public static final boolean[] decisionCanBacktrack = new boolean[] {
        false, // invalid decision
        false, true, false, false, false, false, false, false, false, false,
            false, true, false, false, true, false, false, false, false,
            false, false, false, false, false, false, false, false, false,
            false, false, true, false, false, false, false, false, false,
            false, true, false, false, true, false, false, false, false,
            false, false, true, false, false, false, true, false, false,
            false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, true, true, false,
            false, false, true, false, false, false, false, false, false,
            false, false, false, false, true, false, false, true, false,
            false, false, true, false, false, false, true, false, false,
            false, true, false, false, false, false, false, true, true,
            false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false,
            false, false, true, true, true, true, true, true, false, false,
            false, false, false, false, true, false, false, false, true,
            false, true, false, true, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, true, false, false,
            false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false,
            false, false, false
    };


        public int ruleLevel = 0;
        public int getRuleLevel() { return ruleLevel; }
        public void incRuleLevel() { ruleLevel++; }
        public void decRuleLevel() { ruleLevel--; }
        public JavaParser(TokenStream input) {
            this(input, DebugEventSocketProxy.DEFAULT_DEBUGGER_PORT, new RecognizerSharedState());
        }
        public JavaParser(TokenStream input, int port, RecognizerSharedState state) {
            super(input, state);
            this.state.ruleMemo = new HashMap[381+1];

            DebugEventSocketProxy proxy =
                new DebugEventSocketProxy(this, port, null);
            setDebugListener(proxy);
            try {
                proxy.handshake();
            }
            catch (IOException ioe) {
                reportError(ioe);
            }
        }
    public JavaParser(TokenStream input, DebugEventListener dbg) {
        super(input, dbg, new RecognizerSharedState());
        this.state.ruleMemo = new HashMap[381+1];

    }
    protected boolean evalPredicate(boolean result, String predicate) {
        dbg.semanticPredicate(result, predicate);
        return result;
    }


    public String[] getTokenNames() { return JavaParser.tokenNames; }
    public String getGrammarFileName() { return "src/com/google/doclava/parser/Java.g"; }



    // $ANTLR start "compilationUnit"
    // src/com/google/doclava/parser/Java.g:293:1: compilationUnit : ( ( annotations )? packageDeclaration )? ( importDeclaration )* ( typeDeclaration )* ;
    public final void compilationUnit() throws RecognitionException {
        int compilationUnit_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "compilationUnit");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(293, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 1) ) { return ; }
            // src/com/google/doclava/parser/Java.g:298:5: ( ( ( annotations )? packageDeclaration )? ( importDeclaration )* ( typeDeclaration )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:298:9: ( ( annotations )? packageDeclaration )? ( importDeclaration )* ( typeDeclaration )*
            {
            dbg.location(298,9);
            // src/com/google/doclava/parser/Java.g:298:9: ( ( annotations )? packageDeclaration )?
            int alt2=2;
            try { dbg.enterSubRule(2);
            try { dbg.enterDecision(2, decisionCanBacktrack[2]);

            try {
                isCyclicDecision = true;
                alt2 = dfa2.predict(input);
            }
            catch (NoViableAltException nvae) {
                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(2);}

            switch (alt2) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:298:13: ( annotations )? packageDeclaration
                    {
                    dbg.location(298,13);
                    // src/com/google/doclava/parser/Java.g:298:13: ( annotations )?
                    int alt1=2;
                    try { dbg.enterSubRule(1);
                    try { dbg.enterDecision(1, decisionCanBacktrack[1]);

                    int LA1_0 = input.LA(1);

                    if ( (LA1_0==MONKEYS_AT) ) {
                        alt1=1;
                    }
                    } finally {dbg.exitDecision(1);}

                    switch (alt1) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:298:14: annotations
                            {
                            dbg.location(298,14);
                            pushFollow(FOLLOW_annotations_in_compilationUnit64);
                            annotations();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(1);}

                    dbg.location(300,13);
                    pushFollow(FOLLOW_packageDeclaration_in_compilationUnit93);
                    packageDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(2);}

            dbg.location(302,9);
            // src/com/google/doclava/parser/Java.g:302:9: ( importDeclaration )*
            try { dbg.enterSubRule(3);

            loop3:
            do {
                int alt3=2;
                try { dbg.enterDecision(3, decisionCanBacktrack[3]);

                int LA3_0 = input.LA(1);

                if ( (LA3_0==IMPORT) ) {
                    alt3=1;
                }


                } finally {dbg.exitDecision(3);}

                switch (alt3) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:302:10: importDeclaration
		    {
		    dbg.location(302,10);
		    pushFollow(FOLLOW_importDeclaration_in_compilationUnit115);
		    importDeclaration();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop3;
                }
            } while (true);
            } finally {dbg.exitSubRule(3);}

            dbg.location(304,9);
            // src/com/google/doclava/parser/Java.g:304:9: ( typeDeclaration )*
            try { dbg.enterSubRule(4);

            loop4:
            do {
                int alt4=2;
                try { dbg.enterDecision(4, decisionCanBacktrack[4]);

                int LA4_0 = input.LA(1);

                if ( (LA4_0==IDENTIFIER||LA4_0==ABSTRACT||LA4_0==BOOLEAN||LA4_0==BYTE||(LA4_0>=CHAR && LA4_0<=CLASS)||LA4_0==DOUBLE||LA4_0==ENUM||LA4_0==FINAL||LA4_0==FLOAT||(LA4_0>=INT && LA4_0<=NATIVE)||(LA4_0>=PRIVATE && LA4_0<=PUBLIC)||(LA4_0>=SHORT && LA4_0<=STRICTFP)||LA4_0==SYNCHRONIZED||LA4_0==TRANSIENT||(LA4_0>=VOID && LA4_0<=VOLATILE)||LA4_0==SEMI||LA4_0==MONKEYS_AT||LA4_0==LT) ) {
                    alt4=1;
                }


                } finally {dbg.exitDecision(4);}

                switch (alt4) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:304:10: typeDeclaration
		    {
		    dbg.location(304,10);
		    pushFollow(FOLLOW_typeDeclaration_in_compilationUnit137);
		    typeDeclaration();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop4;
                }
            } while (true);
            } finally {dbg.exitSubRule(4);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 1, compilationUnit_StartIndex); }
        }
        dbg.location(306, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "compilationUnit");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "compilationUnit"


    // $ANTLR start "packageDeclaration"
    // src/com/google/doclava/parser/Java.g:308:1: packageDeclaration : 'package' qualifiedName ';' ;
    public final void packageDeclaration() throws RecognitionException {
        int packageDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "packageDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(308, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 2) ) { return ; }
            // src/com/google/doclava/parser/Java.g:309:5: ( 'package' qualifiedName ';' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:309:9: 'package' qualifiedName ';'
            {
            dbg.location(309,9);
            match(input,PACKAGE,FOLLOW_PACKAGE_in_packageDeclaration167); if (state.failed) return ;
            dbg.location(309,19);
            pushFollow(FOLLOW_qualifiedName_in_packageDeclaration169);
            qualifiedName();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(310,9);
            match(input,SEMI,FOLLOW_SEMI_in_packageDeclaration179); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 2, packageDeclaration_StartIndex); }
        }
        dbg.location(311, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "packageDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "packageDeclaration"


    // $ANTLR start "importDeclaration"
    // src/com/google/doclava/parser/Java.g:313:1: importDeclaration : ( 'import' ( 'static' )? IDENTIFIER '.' '*' ';' | 'import' ( 'static' )? IDENTIFIER ( '.' IDENTIFIER )+ ( '.' '*' )? ';' );
    public final void importDeclaration() throws RecognitionException {
        int importDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "importDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(313, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 3) ) { return ; }
            // src/com/google/doclava/parser/Java.g:314:5: ( 'import' ( 'static' )? IDENTIFIER '.' '*' ';' | 'import' ( 'static' )? IDENTIFIER ( '.' IDENTIFIER )+ ( '.' '*' )? ';' )
            int alt9=2;
            try { dbg.enterDecision(9, decisionCanBacktrack[9]);

            int LA9_0 = input.LA(1);

            if ( (LA9_0==IMPORT) ) {
                int LA9_1 = input.LA(2);

                if ( (LA9_1==STATIC) ) {
                    int LA9_2 = input.LA(3);

                    if ( (LA9_2==IDENTIFIER) ) {
                        int LA9_3 = input.LA(4);

                        if ( (LA9_3==DOT) ) {
                            int LA9_4 = input.LA(5);

                            if ( (LA9_4==STAR) ) {
                                alt9=1;
                            }
                            else if ( (LA9_4==IDENTIFIER) ) {
                                alt9=2;
                            }
                            else {
                                if (state.backtracking>0) {state.failed=true; return ;}
                                NoViableAltException nvae =
                                    new NoViableAltException("", 9, 4, input);

                                dbg.recognitionException(nvae);
                                throw nvae;
                            }
                        }
                        else {
                            if (state.backtracking>0) {state.failed=true; return ;}
                            NoViableAltException nvae =
                                new NoViableAltException("", 9, 3, input);

                            dbg.recognitionException(nvae);
                            throw nvae;
                        }
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        NoViableAltException nvae =
                            new NoViableAltException("", 9, 2, input);

                        dbg.recognitionException(nvae);
                        throw nvae;
                    }
                }
                else if ( (LA9_1==IDENTIFIER) ) {
                    int LA9_3 = input.LA(3);

                    if ( (LA9_3==DOT) ) {
                        int LA9_4 = input.LA(4);

                        if ( (LA9_4==STAR) ) {
                            alt9=1;
                        }
                        else if ( (LA9_4==IDENTIFIER) ) {
                            alt9=2;
                        }
                        else {
                            if (state.backtracking>0) {state.failed=true; return ;}
                            NoViableAltException nvae =
                                new NoViableAltException("", 9, 4, input);

                            dbg.recognitionException(nvae);
                            throw nvae;
                        }
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        NoViableAltException nvae =
                            new NoViableAltException("", 9, 3, input);

                        dbg.recognitionException(nvae);
                        throw nvae;
                    }
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 9, 1, input);

                    dbg.recognitionException(nvae);
                    throw nvae;
                }
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 9, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(9);}

            switch (alt9) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:314:9: 'import' ( 'static' )? IDENTIFIER '.' '*' ';'
                    {
                    dbg.location(314,9);
                    match(input,IMPORT,FOLLOW_IMPORT_in_importDeclaration198); if (state.failed) return ;
                    dbg.location(315,9);
                    // src/com/google/doclava/parser/Java.g:315:9: ( 'static' )?
                    int alt5=2;
                    try { dbg.enterSubRule(5);
                    try { dbg.enterDecision(5, decisionCanBacktrack[5]);

                    int LA5_0 = input.LA(1);

                    if ( (LA5_0==STATIC) ) {
                        alt5=1;
                    }
                    } finally {dbg.exitDecision(5);}

                    switch (alt5) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:315:10: 'static'
                            {
                            dbg.location(315,10);
                            match(input,STATIC,FOLLOW_STATIC_in_importDeclaration209); if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(5);}

                    dbg.location(317,9);
                    match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_importDeclaration230); if (state.failed) return ;
                    dbg.location(317,20);
                    match(input,DOT,FOLLOW_DOT_in_importDeclaration232); if (state.failed) return ;
                    dbg.location(317,24);
                    match(input,STAR,FOLLOW_STAR_in_importDeclaration234); if (state.failed) return ;
                    dbg.location(318,9);
                    match(input,SEMI,FOLLOW_SEMI_in_importDeclaration244); if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:319:9: 'import' ( 'static' )? IDENTIFIER ( '.' IDENTIFIER )+ ( '.' '*' )? ';'
                    {
                    dbg.location(319,9);
                    match(input,IMPORT,FOLLOW_IMPORT_in_importDeclaration254); if (state.failed) return ;
                    dbg.location(320,9);
                    // src/com/google/doclava/parser/Java.g:320:9: ( 'static' )?
                    int alt6=2;
                    try { dbg.enterSubRule(6);
                    try { dbg.enterDecision(6, decisionCanBacktrack[6]);

                    int LA6_0 = input.LA(1);

                    if ( (LA6_0==STATIC) ) {
                        alt6=1;
                    }
                    } finally {dbg.exitDecision(6);}

                    switch (alt6) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:320:10: 'static'
                            {
                            dbg.location(320,10);
                            match(input,STATIC,FOLLOW_STATIC_in_importDeclaration265); if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(6);}

                    dbg.location(322,9);
                    match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_importDeclaration286); if (state.failed) return ;
                    dbg.location(323,9);
                    // src/com/google/doclava/parser/Java.g:323:9: ( '.' IDENTIFIER )+
                    int cnt7=0;
                    try { dbg.enterSubRule(7);

                    loop7:
                    do {
                        int alt7=2;
                        try { dbg.enterDecision(7, decisionCanBacktrack[7]);

                        int LA7_0 = input.LA(1);

                        if ( (LA7_0==DOT) ) {
                            int LA7_1 = input.LA(2);

                            if ( (LA7_1==IDENTIFIER) ) {
                                alt7=1;
                            }


                        }


                        } finally {dbg.exitDecision(7);}

                        switch (alt7) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:323:10: '.' IDENTIFIER
			    {
			    dbg.location(323,10);
			    match(input,DOT,FOLLOW_DOT_in_importDeclaration297); if (state.failed) return ;
			    dbg.location(323,14);
			    match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_importDeclaration299); if (state.failed) return ;

			    }
			    break;

			default :
			    if ( cnt7 >= 1 ) break loop7;
			    if (state.backtracking>0) {state.failed=true; return ;}
                                EarlyExitException eee =
                                    new EarlyExitException(7, input);
                                dbg.recognitionException(eee);

                                throw eee;
                        }
                        cnt7++;
                    } while (true);
                    } finally {dbg.exitSubRule(7);}

                    dbg.location(325,9);
                    // src/com/google/doclava/parser/Java.g:325:9: ( '.' '*' )?
                    int alt8=2;
                    try { dbg.enterSubRule(8);
                    try { dbg.enterDecision(8, decisionCanBacktrack[8]);

                    int LA8_0 = input.LA(1);

                    if ( (LA8_0==DOT) ) {
                        alt8=1;
                    }
                    } finally {dbg.exitDecision(8);}

                    switch (alt8) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:325:10: '.' '*'
                            {
                            dbg.location(325,10);
                            match(input,DOT,FOLLOW_DOT_in_importDeclaration321); if (state.failed) return ;
                            dbg.location(325,14);
                            match(input,STAR,FOLLOW_STAR_in_importDeclaration323); if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(8);}

                    dbg.location(327,9);
                    match(input,SEMI,FOLLOW_SEMI_in_importDeclaration344); if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 3, importDeclaration_StartIndex); }
        }
        dbg.location(328, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "importDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "importDeclaration"


    // $ANTLR start "qualifiedImportName"
    // src/com/google/doclava/parser/Java.g:330:1: qualifiedImportName : IDENTIFIER ( '.' IDENTIFIER )* ;
    public final void qualifiedImportName() throws RecognitionException {
        int qualifiedImportName_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "qualifiedImportName");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(330, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 4) ) { return ; }
            // src/com/google/doclava/parser/Java.g:331:5: ( IDENTIFIER ( '.' IDENTIFIER )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:331:9: IDENTIFIER ( '.' IDENTIFIER )*
            {
            dbg.location(331,9);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_qualifiedImportName363); if (state.failed) return ;
            dbg.location(332,9);
            // src/com/google/doclava/parser/Java.g:332:9: ( '.' IDENTIFIER )*
            try { dbg.enterSubRule(10);

            loop10:
            do {
                int alt10=2;
                try { dbg.enterDecision(10, decisionCanBacktrack[10]);

                int LA10_0 = input.LA(1);

                if ( (LA10_0==DOT) ) {
                    alt10=1;
                }


                } finally {dbg.exitDecision(10);}

                switch (alt10) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:332:10: '.' IDENTIFIER
		    {
		    dbg.location(332,10);
		    match(input,DOT,FOLLOW_DOT_in_qualifiedImportName374); if (state.failed) return ;
		    dbg.location(332,14);
		    match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_qualifiedImportName376); if (state.failed) return ;

		    }
		    break;

		default :
		    break loop10;
                }
            } while (true);
            } finally {dbg.exitSubRule(10);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 4, qualifiedImportName_StartIndex); }
        }
        dbg.location(334, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "qualifiedImportName");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "qualifiedImportName"


    // $ANTLR start "typeDeclaration"
    // src/com/google/doclava/parser/Java.g:336:1: typeDeclaration : ( classOrInterfaceDeclaration | ';' );
    public final void typeDeclaration() throws RecognitionException {
        int typeDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "typeDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(336, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 5) ) { return ; }
            // src/com/google/doclava/parser/Java.g:337:5: ( classOrInterfaceDeclaration | ';' )
            int alt11=2;
            try { dbg.enterDecision(11, decisionCanBacktrack[11]);

            int LA11_0 = input.LA(1);

            if ( (LA11_0==IDENTIFIER||LA11_0==ABSTRACT||LA11_0==BOOLEAN||LA11_0==BYTE||(LA11_0>=CHAR && LA11_0<=CLASS)||LA11_0==DOUBLE||LA11_0==ENUM||LA11_0==FINAL||LA11_0==FLOAT||(LA11_0>=INT && LA11_0<=NATIVE)||(LA11_0>=PRIVATE && LA11_0<=PUBLIC)||(LA11_0>=SHORT && LA11_0<=STRICTFP)||LA11_0==SYNCHRONIZED||LA11_0==TRANSIENT||(LA11_0>=VOID && LA11_0<=VOLATILE)||LA11_0==MONKEYS_AT||LA11_0==LT) ) {
                alt11=1;
            }
            else if ( (LA11_0==SEMI) ) {
                alt11=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 11, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(11);}

            switch (alt11) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:337:9: classOrInterfaceDeclaration
                    {
                    dbg.location(337,9);
                    pushFollow(FOLLOW_classOrInterfaceDeclaration_in_typeDeclaration406);
                    classOrInterfaceDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:338:9: ';'
                    {
                    dbg.location(338,9);
                    match(input,SEMI,FOLLOW_SEMI_in_typeDeclaration416); if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 5, typeDeclaration_StartIndex); }
        }
        dbg.location(339, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "typeDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "typeDeclaration"


    // $ANTLR start "classOrInterfaceDeclaration"
    // src/com/google/doclava/parser/Java.g:341:1: classOrInterfaceDeclaration : ( classDeclaration | interfaceDeclaration );
    public final void classOrInterfaceDeclaration() throws RecognitionException {
        int classOrInterfaceDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "classOrInterfaceDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(341, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 6) ) { return ; }
            // src/com/google/doclava/parser/Java.g:342:5: ( classDeclaration | interfaceDeclaration )
            int alt12=2;
            try { dbg.enterDecision(12, decisionCanBacktrack[12]);

            try {
                isCyclicDecision = true;
                alt12 = dfa12.predict(input);
            }
            catch (NoViableAltException nvae) {
                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(12);}

            switch (alt12) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:342:10: classDeclaration
                    {
                    dbg.location(342,10);
                    pushFollow(FOLLOW_classDeclaration_in_classOrInterfaceDeclaration436);
                    classDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:343:9: interfaceDeclaration
                    {
                    dbg.location(343,9);
                    pushFollow(FOLLOW_interfaceDeclaration_in_classOrInterfaceDeclaration446);
                    interfaceDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 6, classOrInterfaceDeclaration_StartIndex); }
        }
        dbg.location(344, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "classOrInterfaceDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "classOrInterfaceDeclaration"


    // $ANTLR start "modifiers"
    // src/com/google/doclava/parser/Java.g:347:1: modifiers : ( annotation | 'public' | 'protected' | 'private' | 'static' | 'abstract' | 'final' | 'native' | 'synchronized' | 'transient' | 'volatile' | 'strictfp' )* ;
    public final void modifiers() throws RecognitionException {
        int modifiers_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "modifiers");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(347, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 7) ) { return ; }
            // src/com/google/doclava/parser/Java.g:348:5: ( ( annotation | 'public' | 'protected' | 'private' | 'static' | 'abstract' | 'final' | 'native' | 'synchronized' | 'transient' | 'volatile' | 'strictfp' )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:349:5: ( annotation | 'public' | 'protected' | 'private' | 'static' | 'abstract' | 'final' | 'native' | 'synchronized' | 'transient' | 'volatile' | 'strictfp' )*
            {
            dbg.location(349,5);
            // src/com/google/doclava/parser/Java.g:349:5: ( annotation | 'public' | 'protected' | 'private' | 'static' | 'abstract' | 'final' | 'native' | 'synchronized' | 'transient' | 'volatile' | 'strictfp' )*
            try { dbg.enterSubRule(13);

            loop13:
            do {
                int alt13=13;
                try { dbg.enterDecision(13, decisionCanBacktrack[13]);

                try {
                    isCyclicDecision = true;
                    alt13 = dfa13.predict(input);
                }
                catch (NoViableAltException nvae) {
                    dbg.recognitionException(nvae);
                    throw nvae;
                }
                } finally {dbg.exitDecision(13);}

                switch (alt13) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:349:10: annotation
		    {
		    dbg.location(349,10);
		    pushFollow(FOLLOW_annotation_in_modifiers473);
		    annotation();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;
		case 2 :
		    dbg.enterAlt(2);

		    // src/com/google/doclava/parser/Java.g:350:9: 'public'
		    {
		    dbg.location(350,9);
		    match(input,PUBLIC,FOLLOW_PUBLIC_in_modifiers483); if (state.failed) return ;

		    }
		    break;
		case 3 :
		    dbg.enterAlt(3);

		    // src/com/google/doclava/parser/Java.g:351:9: 'protected'
		    {
		    dbg.location(351,9);
		    match(input,PROTECTED,FOLLOW_PROTECTED_in_modifiers493); if (state.failed) return ;

		    }
		    break;
		case 4 :
		    dbg.enterAlt(4);

		    // src/com/google/doclava/parser/Java.g:352:9: 'private'
		    {
		    dbg.location(352,9);
		    match(input,PRIVATE,FOLLOW_PRIVATE_in_modifiers503); if (state.failed) return ;

		    }
		    break;
		case 5 :
		    dbg.enterAlt(5);

		    // src/com/google/doclava/parser/Java.g:353:9: 'static'
		    {
		    dbg.location(353,9);
		    match(input,STATIC,FOLLOW_STATIC_in_modifiers513); if (state.failed) return ;

		    }
		    break;
		case 6 :
		    dbg.enterAlt(6);

		    // src/com/google/doclava/parser/Java.g:354:9: 'abstract'
		    {
		    dbg.location(354,9);
		    match(input,ABSTRACT,FOLLOW_ABSTRACT_in_modifiers523); if (state.failed) return ;

		    }
		    break;
		case 7 :
		    dbg.enterAlt(7);

		    // src/com/google/doclava/parser/Java.g:355:9: 'final'
		    {
		    dbg.location(355,9);
		    match(input,FINAL,FOLLOW_FINAL_in_modifiers533); if (state.failed) return ;

		    }
		    break;
		case 8 :
		    dbg.enterAlt(8);

		    // src/com/google/doclava/parser/Java.g:356:9: 'native'
		    {
		    dbg.location(356,9);
		    match(input,NATIVE,FOLLOW_NATIVE_in_modifiers543); if (state.failed) return ;

		    }
		    break;
		case 9 :
		    dbg.enterAlt(9);

		    // src/com/google/doclava/parser/Java.g:357:9: 'synchronized'
		    {
		    dbg.location(357,9);
		    match(input,SYNCHRONIZED,FOLLOW_SYNCHRONIZED_in_modifiers553); if (state.failed) return ;

		    }
		    break;
		case 10 :
		    dbg.enterAlt(10);

		    // src/com/google/doclava/parser/Java.g:358:9: 'transient'
		    {
		    dbg.location(358,9);
		    match(input,TRANSIENT,FOLLOW_TRANSIENT_in_modifiers563); if (state.failed) return ;

		    }
		    break;
		case 11 :
		    dbg.enterAlt(11);

		    // src/com/google/doclava/parser/Java.g:359:9: 'volatile'
		    {
		    dbg.location(359,9);
		    match(input,VOLATILE,FOLLOW_VOLATILE_in_modifiers573); if (state.failed) return ;

		    }
		    break;
		case 12 :
		    dbg.enterAlt(12);

		    // src/com/google/doclava/parser/Java.g:360:9: 'strictfp'
		    {
		    dbg.location(360,9);
		    match(input,STRICTFP,FOLLOW_STRICTFP_in_modifiers583); if (state.failed) return ;

		    }
		    break;

		default :
		    break loop13;
                }
            } while (true);
            } finally {dbg.exitSubRule(13);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 7, modifiers_StartIndex); }
        }
        dbg.location(362, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "modifiers");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "modifiers"


    // $ANTLR start "variableModifiers"
    // src/com/google/doclava/parser/Java.g:365:1: variableModifiers : ( 'final' | annotation )* ;
    public final void variableModifiers() throws RecognitionException {
        int variableModifiers_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "variableModifiers");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(365, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 8) ) { return ; }
            // src/com/google/doclava/parser/Java.g:366:5: ( ( 'final' | annotation )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:366:9: ( 'final' | annotation )*
            {
            dbg.location(366,9);
            // src/com/google/doclava/parser/Java.g:366:9: ( 'final' | annotation )*
            try { dbg.enterSubRule(14);

            loop14:
            do {
                int alt14=3;
                try { dbg.enterDecision(14, decisionCanBacktrack[14]);

                int LA14_0 = input.LA(1);

                if ( (LA14_0==FINAL) ) {
                    alt14=1;
                }
                else if ( (LA14_0==MONKEYS_AT) ) {
                    alt14=2;
                }


                } finally {dbg.exitDecision(14);}

                switch (alt14) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:366:13: 'final'
		    {
		    dbg.location(366,13);
		    match(input,FINAL,FOLLOW_FINAL_in_variableModifiers614); if (state.failed) return ;

		    }
		    break;
		case 2 :
		    dbg.enterAlt(2);

		    // src/com/google/doclava/parser/Java.g:367:13: annotation
		    {
		    dbg.location(367,13);
		    pushFollow(FOLLOW_annotation_in_variableModifiers628);
		    annotation();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop14;
                }
            } while (true);
            } finally {dbg.exitSubRule(14);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 8, variableModifiers_StartIndex); }
        }
        dbg.location(369, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "variableModifiers");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "variableModifiers"


    // $ANTLR start "classDeclaration"
    // src/com/google/doclava/parser/Java.g:372:1: classDeclaration : ( normalClassDeclaration | enumDeclaration );
    public final void classDeclaration() throws RecognitionException {
        int classDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "classDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(372, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 9) ) { return ; }
            // src/com/google/doclava/parser/Java.g:373:5: ( normalClassDeclaration | enumDeclaration )
            int alt15=2;
            try { dbg.enterDecision(15, decisionCanBacktrack[15]);

            try {
                isCyclicDecision = true;
                alt15 = dfa15.predict(input);
            }
            catch (NoViableAltException nvae) {
                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(15);}

            switch (alt15) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:373:9: normalClassDeclaration
                    {
                    dbg.location(373,9);
                    pushFollow(FOLLOW_normalClassDeclaration_in_classDeclaration659);
                    normalClassDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:374:9: enumDeclaration
                    {
                    dbg.location(374,9);
                    pushFollow(FOLLOW_enumDeclaration_in_classDeclaration669);
                    enumDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 9, classDeclaration_StartIndex); }
        }
        dbg.location(375, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "classDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "classDeclaration"


    // $ANTLR start "normalClassDeclaration"
    // src/com/google/doclava/parser/Java.g:377:1: normalClassDeclaration : modifiers 'class' IDENTIFIER ( typeParameters )? ( 'extends' type )? ( 'implements' typeList )? classBody ;
    public final void normalClassDeclaration() throws RecognitionException {
        int normalClassDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "normalClassDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(377, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 10) ) { return ; }
            // src/com/google/doclava/parser/Java.g:378:5: ( modifiers 'class' IDENTIFIER ( typeParameters )? ( 'extends' type )? ( 'implements' typeList )? classBody )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:378:9: modifiers 'class' IDENTIFIER ( typeParameters )? ( 'extends' type )? ( 'implements' typeList )? classBody
            {
            dbg.location(378,9);
            pushFollow(FOLLOW_modifiers_in_normalClassDeclaration688);
            modifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(378,20);
            match(input,CLASS,FOLLOW_CLASS_in_normalClassDeclaration691); if (state.failed) return ;
            dbg.location(378,28);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_normalClassDeclaration693); if (state.failed) return ;
            dbg.location(379,9);
            // src/com/google/doclava/parser/Java.g:379:9: ( typeParameters )?
            int alt16=2;
            try { dbg.enterSubRule(16);
            try { dbg.enterDecision(16, decisionCanBacktrack[16]);

            int LA16_0 = input.LA(1);

            if ( (LA16_0==LT) ) {
                alt16=1;
            }
            } finally {dbg.exitDecision(16);}

            switch (alt16) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:379:10: typeParameters
                    {
                    dbg.location(379,10);
                    pushFollow(FOLLOW_typeParameters_in_normalClassDeclaration704);
                    typeParameters();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(16);}

            dbg.location(381,9);
            // src/com/google/doclava/parser/Java.g:381:9: ( 'extends' type )?
            int alt17=2;
            try { dbg.enterSubRule(17);
            try { dbg.enterDecision(17, decisionCanBacktrack[17]);

            int LA17_0 = input.LA(1);

            if ( (LA17_0==EXTENDS) ) {
                alt17=1;
            }
            } finally {dbg.exitDecision(17);}

            switch (alt17) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:381:10: 'extends' type
                    {
                    dbg.location(381,10);
                    match(input,EXTENDS,FOLLOW_EXTENDS_in_normalClassDeclaration726); if (state.failed) return ;
                    dbg.location(381,20);
                    pushFollow(FOLLOW_type_in_normalClassDeclaration728);
                    type();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(17);}

            dbg.location(383,9);
            // src/com/google/doclava/parser/Java.g:383:9: ( 'implements' typeList )?
            int alt18=2;
            try { dbg.enterSubRule(18);
            try { dbg.enterDecision(18, decisionCanBacktrack[18]);

            int LA18_0 = input.LA(1);

            if ( (LA18_0==IMPLEMENTS) ) {
                alt18=1;
            }
            } finally {dbg.exitDecision(18);}

            switch (alt18) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:383:10: 'implements' typeList
                    {
                    dbg.location(383,10);
                    match(input,IMPLEMENTS,FOLLOW_IMPLEMENTS_in_normalClassDeclaration750); if (state.failed) return ;
                    dbg.location(383,23);
                    pushFollow(FOLLOW_typeList_in_normalClassDeclaration752);
                    typeList();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(18);}

            dbg.location(385,9);
            pushFollow(FOLLOW_classBody_in_normalClassDeclaration773);
            classBody();

            state._fsp--;
            if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 10, normalClassDeclaration_StartIndex); }
        }
        dbg.location(386, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "normalClassDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "normalClassDeclaration"


    // $ANTLR start "typeParameters"
    // src/com/google/doclava/parser/Java.g:389:1: typeParameters : '<' typeParameter ( ',' typeParameter )* '>' ;
    public final void typeParameters() throws RecognitionException {
        int typeParameters_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "typeParameters");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(389, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 11) ) { return ; }
            // src/com/google/doclava/parser/Java.g:390:5: ( '<' typeParameter ( ',' typeParameter )* '>' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:390:9: '<' typeParameter ( ',' typeParameter )* '>'
            {
            dbg.location(390,9);
            match(input,LT,FOLLOW_LT_in_typeParameters793); if (state.failed) return ;
            dbg.location(391,13);
            pushFollow(FOLLOW_typeParameter_in_typeParameters807);
            typeParameter();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(392,13);
            // src/com/google/doclava/parser/Java.g:392:13: ( ',' typeParameter )*
            try { dbg.enterSubRule(19);

            loop19:
            do {
                int alt19=2;
                try { dbg.enterDecision(19, decisionCanBacktrack[19]);

                int LA19_0 = input.LA(1);

                if ( (LA19_0==COMMA) ) {
                    alt19=1;
                }


                } finally {dbg.exitDecision(19);}

                switch (alt19) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:392:14: ',' typeParameter
		    {
		    dbg.location(392,14);
		    match(input,COMMA,FOLLOW_COMMA_in_typeParameters822); if (state.failed) return ;
		    dbg.location(392,18);
		    pushFollow(FOLLOW_typeParameter_in_typeParameters824);
		    typeParameter();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop19;
                }
            } while (true);
            } finally {dbg.exitSubRule(19);}

            dbg.location(394,9);
            match(input,GT,FOLLOW_GT_in_typeParameters849); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 11, typeParameters_StartIndex); }
        }
        dbg.location(395, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "typeParameters");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "typeParameters"


    // $ANTLR start "typeParameter"
    // src/com/google/doclava/parser/Java.g:397:1: typeParameter : IDENTIFIER ( 'extends' typeBound )? ;
    public final void typeParameter() throws RecognitionException {
        int typeParameter_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "typeParameter");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(397, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 12) ) { return ; }
            // src/com/google/doclava/parser/Java.g:398:5: ( IDENTIFIER ( 'extends' typeBound )? )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:398:9: IDENTIFIER ( 'extends' typeBound )?
            {
            dbg.location(398,9);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_typeParameter868); if (state.failed) return ;
            dbg.location(399,9);
            // src/com/google/doclava/parser/Java.g:399:9: ( 'extends' typeBound )?
            int alt20=2;
            try { dbg.enterSubRule(20);
            try { dbg.enterDecision(20, decisionCanBacktrack[20]);

            int LA20_0 = input.LA(1);

            if ( (LA20_0==EXTENDS) ) {
                alt20=1;
            }
            } finally {dbg.exitDecision(20);}

            switch (alt20) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:399:10: 'extends' typeBound
                    {
                    dbg.location(399,10);
                    match(input,EXTENDS,FOLLOW_EXTENDS_in_typeParameter879); if (state.failed) return ;
                    dbg.location(399,20);
                    pushFollow(FOLLOW_typeBound_in_typeParameter881);
                    typeBound();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(20);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 12, typeParameter_StartIndex); }
        }
        dbg.location(401, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "typeParameter");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "typeParameter"


    // $ANTLR start "typeBound"
    // src/com/google/doclava/parser/Java.g:404:1: typeBound : type ( '&' type )* ;
    public final void typeBound() throws RecognitionException {
        int typeBound_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "typeBound");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(404, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 13) ) { return ; }
            // src/com/google/doclava/parser/Java.g:405:5: ( type ( '&' type )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:405:9: type ( '&' type )*
            {
            dbg.location(405,9);
            pushFollow(FOLLOW_type_in_typeBound912);
            type();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(406,9);
            // src/com/google/doclava/parser/Java.g:406:9: ( '&' type )*
            try { dbg.enterSubRule(21);

            loop21:
            do {
                int alt21=2;
                try { dbg.enterDecision(21, decisionCanBacktrack[21]);

                int LA21_0 = input.LA(1);

                if ( (LA21_0==AMP) ) {
                    alt21=1;
                }


                } finally {dbg.exitDecision(21);}

                switch (alt21) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:406:10: '&' type
		    {
		    dbg.location(406,10);
		    match(input,AMP,FOLLOW_AMP_in_typeBound923); if (state.failed) return ;
		    dbg.location(406,14);
		    pushFollow(FOLLOW_type_in_typeBound925);
		    type();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop21;
                }
            } while (true);
            } finally {dbg.exitSubRule(21);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 13, typeBound_StartIndex); }
        }
        dbg.location(408, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "typeBound");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "typeBound"


    // $ANTLR start "enumDeclaration"
    // src/com/google/doclava/parser/Java.g:411:1: enumDeclaration : modifiers ( 'enum' ) IDENTIFIER ( 'implements' typeList )? enumBody ;
    public final void enumDeclaration() throws RecognitionException {
        int enumDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "enumDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(411, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 14) ) { return ; }
            // src/com/google/doclava/parser/Java.g:412:5: ( modifiers ( 'enum' ) IDENTIFIER ( 'implements' typeList )? enumBody )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:412:9: modifiers ( 'enum' ) IDENTIFIER ( 'implements' typeList )? enumBody
            {
            dbg.location(412,9);
            pushFollow(FOLLOW_modifiers_in_enumDeclaration956);
            modifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(413,9);
            // src/com/google/doclava/parser/Java.g:413:9: ( 'enum' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:413:10: 'enum'
            {
            dbg.location(413,10);
            match(input,ENUM,FOLLOW_ENUM_in_enumDeclaration967); if (state.failed) return ;

            }

            dbg.location(415,9);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_enumDeclaration987); if (state.failed) return ;
            dbg.location(416,9);
            // src/com/google/doclava/parser/Java.g:416:9: ( 'implements' typeList )?
            int alt22=2;
            try { dbg.enterSubRule(22);
            try { dbg.enterDecision(22, decisionCanBacktrack[22]);

            int LA22_0 = input.LA(1);

            if ( (LA22_0==IMPLEMENTS) ) {
                alt22=1;
            }
            } finally {dbg.exitDecision(22);}

            switch (alt22) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:416:10: 'implements' typeList
                    {
                    dbg.location(416,10);
                    match(input,IMPLEMENTS,FOLLOW_IMPLEMENTS_in_enumDeclaration998); if (state.failed) return ;
                    dbg.location(416,23);
                    pushFollow(FOLLOW_typeList_in_enumDeclaration1000);
                    typeList();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(22);}

            dbg.location(418,9);
            pushFollow(FOLLOW_enumBody_in_enumDeclaration1021);
            enumBody();

            state._fsp--;
            if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 14, enumDeclaration_StartIndex); }
        }
        dbg.location(419, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "enumDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "enumDeclaration"


    // $ANTLR start "enumBody"
    // src/com/google/doclava/parser/Java.g:422:1: enumBody : '{' ( enumConstants )? ( ',' )? ( enumBodyDeclarations )? '}' ;
    public final void enumBody() throws RecognitionException {
        int enumBody_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "enumBody");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(422, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 15) ) { return ; }
            // src/com/google/doclava/parser/Java.g:423:5: ( '{' ( enumConstants )? ( ',' )? ( enumBodyDeclarations )? '}' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:423:9: '{' ( enumConstants )? ( ',' )? ( enumBodyDeclarations )? '}'
            {
            dbg.location(423,9);
            match(input,LBRACE,FOLLOW_LBRACE_in_enumBody1041); if (state.failed) return ;
            dbg.location(424,9);
            // src/com/google/doclava/parser/Java.g:424:9: ( enumConstants )?
            int alt23=2;
            try { dbg.enterSubRule(23);
            try { dbg.enterDecision(23, decisionCanBacktrack[23]);

            int LA23_0 = input.LA(1);

            if ( (LA23_0==IDENTIFIER||LA23_0==MONKEYS_AT) ) {
                alt23=1;
            }
            } finally {dbg.exitDecision(23);}

            switch (alt23) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:424:10: enumConstants
                    {
                    dbg.location(424,10);
                    pushFollow(FOLLOW_enumConstants_in_enumBody1052);
                    enumConstants();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(23);}

            dbg.location(426,9);
            // src/com/google/doclava/parser/Java.g:426:9: ( ',' )?
            int alt24=2;
            try { dbg.enterSubRule(24);
            try { dbg.enterDecision(24, decisionCanBacktrack[24]);

            int LA24_0 = input.LA(1);

            if ( (LA24_0==COMMA) ) {
                alt24=1;
            }
            } finally {dbg.exitDecision(24);}

            switch (alt24) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:0:0: ','
                    {
                    dbg.location(426,9);
                    match(input,COMMA,FOLLOW_COMMA_in_enumBody1073); if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(24);}

            dbg.location(427,9);
            // src/com/google/doclava/parser/Java.g:427:9: ( enumBodyDeclarations )?
            int alt25=2;
            try { dbg.enterSubRule(25);
            try { dbg.enterDecision(25, decisionCanBacktrack[25]);

            int LA25_0 = input.LA(1);

            if ( (LA25_0==SEMI) ) {
                alt25=1;
            }
            } finally {dbg.exitDecision(25);}

            switch (alt25) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:427:10: enumBodyDeclarations
                    {
                    dbg.location(427,10);
                    pushFollow(FOLLOW_enumBodyDeclarations_in_enumBody1085);
                    enumBodyDeclarations();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(25);}

            dbg.location(429,9);
            match(input,RBRACE,FOLLOW_RBRACE_in_enumBody1106); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 15, enumBody_StartIndex); }
        }
        dbg.location(430, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "enumBody");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "enumBody"


    // $ANTLR start "enumConstants"
    // src/com/google/doclava/parser/Java.g:432:1: enumConstants : enumConstant ( ',' enumConstant )* ;
    public final void enumConstants() throws RecognitionException {
        int enumConstants_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "enumConstants");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(432, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 16) ) { return ; }
            // src/com/google/doclava/parser/Java.g:433:5: ( enumConstant ( ',' enumConstant )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:433:9: enumConstant ( ',' enumConstant )*
            {
            dbg.location(433,9);
            pushFollow(FOLLOW_enumConstant_in_enumConstants1125);
            enumConstant();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(434,9);
            // src/com/google/doclava/parser/Java.g:434:9: ( ',' enumConstant )*
            try { dbg.enterSubRule(26);

            loop26:
            do {
                int alt26=2;
                try { dbg.enterDecision(26, decisionCanBacktrack[26]);

                int LA26_0 = input.LA(1);

                if ( (LA26_0==COMMA) ) {
                    int LA26_1 = input.LA(2);

                    if ( (LA26_1==IDENTIFIER||LA26_1==MONKEYS_AT) ) {
                        alt26=1;
                    }


                }


                } finally {dbg.exitDecision(26);}

                switch (alt26) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:434:10: ',' enumConstant
		    {
		    dbg.location(434,10);
		    match(input,COMMA,FOLLOW_COMMA_in_enumConstants1136); if (state.failed) return ;
		    dbg.location(434,14);
		    pushFollow(FOLLOW_enumConstant_in_enumConstants1138);
		    enumConstant();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop26;
                }
            } while (true);
            } finally {dbg.exitSubRule(26);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 16, enumConstants_StartIndex); }
        }
        dbg.location(436, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "enumConstants");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "enumConstants"


    // $ANTLR start "enumConstant"
    // src/com/google/doclava/parser/Java.g:438:1: enumConstant : ( annotations )? IDENTIFIER ( arguments )? ( classBody )? ;
    public final void enumConstant() throws RecognitionException {
        int enumConstant_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "enumConstant");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(438, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 17) ) { return ; }
            // src/com/google/doclava/parser/Java.g:443:5: ( ( annotations )? IDENTIFIER ( arguments )? ( classBody )? )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:443:9: ( annotations )? IDENTIFIER ( arguments )? ( classBody )?
            {
            dbg.location(443,9);
            // src/com/google/doclava/parser/Java.g:443:9: ( annotations )?
            int alt27=2;
            try { dbg.enterSubRule(27);
            try { dbg.enterDecision(27, decisionCanBacktrack[27]);

            int LA27_0 = input.LA(1);

            if ( (LA27_0==MONKEYS_AT) ) {
                alt27=1;
            }
            } finally {dbg.exitDecision(27);}

            switch (alt27) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:443:10: annotations
                    {
                    dbg.location(443,10);
                    pushFollow(FOLLOW_annotations_in_enumConstant1171);
                    annotations();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(27);}

            dbg.location(445,9);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_enumConstant1192); if (state.failed) return ;
            dbg.location(446,9);
            // src/com/google/doclava/parser/Java.g:446:9: ( arguments )?
            int alt28=2;
            try { dbg.enterSubRule(28);
            try { dbg.enterDecision(28, decisionCanBacktrack[28]);

            int LA28_0 = input.LA(1);

            if ( (LA28_0==LPAREN) ) {
                alt28=1;
            }
            } finally {dbg.exitDecision(28);}

            switch (alt28) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:446:10: arguments
                    {
                    dbg.location(446,10);
                    pushFollow(FOLLOW_arguments_in_enumConstant1203);
                    arguments();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(28);}

            dbg.location(448,9);
            // src/com/google/doclava/parser/Java.g:448:9: ( classBody )?
            int alt29=2;
            try { dbg.enterSubRule(29);
            try { dbg.enterDecision(29, decisionCanBacktrack[29]);

            int LA29_0 = input.LA(1);

            if ( (LA29_0==LBRACE) ) {
                alt29=1;
            }
            } finally {dbg.exitDecision(29);}

            switch (alt29) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:448:10: classBody
                    {
                    dbg.location(448,10);
                    pushFollow(FOLLOW_classBody_in_enumConstant1225);
                    classBody();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(29);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 17, enumConstant_StartIndex); }
        }
        dbg.location(452, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "enumConstant");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "enumConstant"


    // $ANTLR start "enumBodyDeclarations"
    // src/com/google/doclava/parser/Java.g:454:1: enumBodyDeclarations : ';' ( classBodyDeclaration )* ;
    public final void enumBodyDeclarations() throws RecognitionException {
        int enumBodyDeclarations_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "enumBodyDeclarations");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(454, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 18) ) { return ; }
            // src/com/google/doclava/parser/Java.g:455:5: ( ';' ( classBodyDeclaration )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:455:9: ';' ( classBodyDeclaration )*
            {
            dbg.location(455,9);
            match(input,SEMI,FOLLOW_SEMI_in_enumBodyDeclarations1265); if (state.failed) return ;
            dbg.location(456,9);
            // src/com/google/doclava/parser/Java.g:456:9: ( classBodyDeclaration )*
            try { dbg.enterSubRule(30);

            loop30:
            do {
                int alt30=2;
                try { dbg.enterDecision(30, decisionCanBacktrack[30]);

                int LA30_0 = input.LA(1);

                if ( (LA30_0==IDENTIFIER||LA30_0==ABSTRACT||LA30_0==BOOLEAN||LA30_0==BYTE||(LA30_0>=CHAR && LA30_0<=CLASS)||LA30_0==DOUBLE||LA30_0==ENUM||LA30_0==FINAL||LA30_0==FLOAT||(LA30_0>=INT && LA30_0<=NATIVE)||(LA30_0>=PRIVATE && LA30_0<=PUBLIC)||(LA30_0>=SHORT && LA30_0<=STRICTFP)||LA30_0==SYNCHRONIZED||LA30_0==TRANSIENT||(LA30_0>=VOID && LA30_0<=VOLATILE)||LA30_0==LBRACE||LA30_0==SEMI||LA30_0==MONKEYS_AT||LA30_0==LT) ) {
                    alt30=1;
                }


                } finally {dbg.exitDecision(30);}

                switch (alt30) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:456:10: classBodyDeclaration
		    {
		    dbg.location(456,10);
		    pushFollow(FOLLOW_classBodyDeclaration_in_enumBodyDeclarations1276);
		    classBodyDeclaration();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop30;
                }
            } while (true);
            } finally {dbg.exitSubRule(30);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 18, enumBodyDeclarations_StartIndex); }
        }
        dbg.location(458, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "enumBodyDeclarations");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "enumBodyDeclarations"


    // $ANTLR start "interfaceDeclaration"
    // src/com/google/doclava/parser/Java.g:460:1: interfaceDeclaration : ( normalInterfaceDeclaration | annotationTypeDeclaration );
    public final void interfaceDeclaration() throws RecognitionException {
        int interfaceDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "interfaceDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(460, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 19) ) { return ; }
            // src/com/google/doclava/parser/Java.g:461:5: ( normalInterfaceDeclaration | annotationTypeDeclaration )
            int alt31=2;
            try { dbg.enterDecision(31, decisionCanBacktrack[31]);

            try {
                isCyclicDecision = true;
                alt31 = dfa31.predict(input);
            }
            catch (NoViableAltException nvae) {
                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(31);}

            switch (alt31) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:461:9: normalInterfaceDeclaration
                    {
                    dbg.location(461,9);
                    pushFollow(FOLLOW_normalInterfaceDeclaration_in_interfaceDeclaration1306);
                    normalInterfaceDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:462:9: annotationTypeDeclaration
                    {
                    dbg.location(462,9);
                    pushFollow(FOLLOW_annotationTypeDeclaration_in_interfaceDeclaration1316);
                    annotationTypeDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 19, interfaceDeclaration_StartIndex); }
        }
        dbg.location(463, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "interfaceDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "interfaceDeclaration"


    // $ANTLR start "normalInterfaceDeclaration"
    // src/com/google/doclava/parser/Java.g:465:1: normalInterfaceDeclaration : modifiers 'interface' IDENTIFIER ( typeParameters )? ( 'extends' typeList )? interfaceBody ;
    public final void normalInterfaceDeclaration() throws RecognitionException {
        int normalInterfaceDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "normalInterfaceDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(465, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 20) ) { return ; }
            // src/com/google/doclava/parser/Java.g:466:5: ( modifiers 'interface' IDENTIFIER ( typeParameters )? ( 'extends' typeList )? interfaceBody )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:466:9: modifiers 'interface' IDENTIFIER ( typeParameters )? ( 'extends' typeList )? interfaceBody
            {
            dbg.location(466,9);
            pushFollow(FOLLOW_modifiers_in_normalInterfaceDeclaration1335);
            modifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(466,19);
            match(input,INTERFACE,FOLLOW_INTERFACE_in_normalInterfaceDeclaration1337); if (state.failed) return ;
            dbg.location(466,31);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_normalInterfaceDeclaration1339); if (state.failed) return ;
            dbg.location(467,9);
            // src/com/google/doclava/parser/Java.g:467:9: ( typeParameters )?
            int alt32=2;
            try { dbg.enterSubRule(32);
            try { dbg.enterDecision(32, decisionCanBacktrack[32]);

            int LA32_0 = input.LA(1);

            if ( (LA32_0==LT) ) {
                alt32=1;
            }
            } finally {dbg.exitDecision(32);}

            switch (alt32) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:467:10: typeParameters
                    {
                    dbg.location(467,10);
                    pushFollow(FOLLOW_typeParameters_in_normalInterfaceDeclaration1350);
                    typeParameters();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(32);}

            dbg.location(469,9);
            // src/com/google/doclava/parser/Java.g:469:9: ( 'extends' typeList )?
            int alt33=2;
            try { dbg.enterSubRule(33);
            try { dbg.enterDecision(33, decisionCanBacktrack[33]);

            int LA33_0 = input.LA(1);

            if ( (LA33_0==EXTENDS) ) {
                alt33=1;
            }
            } finally {dbg.exitDecision(33);}

            switch (alt33) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:469:10: 'extends' typeList
                    {
                    dbg.location(469,10);
                    match(input,EXTENDS,FOLLOW_EXTENDS_in_normalInterfaceDeclaration1372); if (state.failed) return ;
                    dbg.location(469,20);
                    pushFollow(FOLLOW_typeList_in_normalInterfaceDeclaration1374);
                    typeList();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(33);}

            dbg.location(471,9);
            pushFollow(FOLLOW_interfaceBody_in_normalInterfaceDeclaration1395);
            interfaceBody();

            state._fsp--;
            if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 20, normalInterfaceDeclaration_StartIndex); }
        }
        dbg.location(472, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "normalInterfaceDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "normalInterfaceDeclaration"


    // $ANTLR start "typeList"
    // src/com/google/doclava/parser/Java.g:474:1: typeList : type ( ',' type )* ;
    public final void typeList() throws RecognitionException {
        int typeList_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "typeList");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(474, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 21) ) { return ; }
            // src/com/google/doclava/parser/Java.g:475:5: ( type ( ',' type )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:475:9: type ( ',' type )*
            {
            dbg.location(475,9);
            pushFollow(FOLLOW_type_in_typeList1414);
            type();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(476,9);
            // src/com/google/doclava/parser/Java.g:476:9: ( ',' type )*
            try { dbg.enterSubRule(34);

            loop34:
            do {
                int alt34=2;
                try { dbg.enterDecision(34, decisionCanBacktrack[34]);

                int LA34_0 = input.LA(1);

                if ( (LA34_0==COMMA) ) {
                    alt34=1;
                }


                } finally {dbg.exitDecision(34);}

                switch (alt34) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:476:10: ',' type
		    {
		    dbg.location(476,10);
		    match(input,COMMA,FOLLOW_COMMA_in_typeList1425); if (state.failed) return ;
		    dbg.location(476,14);
		    pushFollow(FOLLOW_type_in_typeList1427);
		    type();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop34;
                }
            } while (true);
            } finally {dbg.exitSubRule(34);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 21, typeList_StartIndex); }
        }
        dbg.location(478, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "typeList");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "typeList"


    // $ANTLR start "classBody"
    // src/com/google/doclava/parser/Java.g:480:1: classBody : '{' ( classBodyDeclaration )* '}' ;
    public final void classBody() throws RecognitionException {
        int classBody_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "classBody");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(480, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 22) ) { return ; }
            // src/com/google/doclava/parser/Java.g:481:5: ( '{' ( classBodyDeclaration )* '}' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:481:9: '{' ( classBodyDeclaration )* '}'
            {
            dbg.location(481,9);
            match(input,LBRACE,FOLLOW_LBRACE_in_classBody1457); if (state.failed) return ;
            dbg.location(482,9);
            // src/com/google/doclava/parser/Java.g:482:9: ( classBodyDeclaration )*
            try { dbg.enterSubRule(35);

            loop35:
            do {
                int alt35=2;
                try { dbg.enterDecision(35, decisionCanBacktrack[35]);

                int LA35_0 = input.LA(1);

                if ( (LA35_0==IDENTIFIER||LA35_0==ABSTRACT||LA35_0==BOOLEAN||LA35_0==BYTE||(LA35_0>=CHAR && LA35_0<=CLASS)||LA35_0==DOUBLE||LA35_0==ENUM||LA35_0==FINAL||LA35_0==FLOAT||(LA35_0>=INT && LA35_0<=NATIVE)||(LA35_0>=PRIVATE && LA35_0<=PUBLIC)||(LA35_0>=SHORT && LA35_0<=STRICTFP)||LA35_0==SYNCHRONIZED||LA35_0==TRANSIENT||(LA35_0>=VOID && LA35_0<=VOLATILE)||LA35_0==LBRACE||LA35_0==SEMI||LA35_0==MONKEYS_AT||LA35_0==LT) ) {
                    alt35=1;
                }


                } finally {dbg.exitDecision(35);}

                switch (alt35) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:482:10: classBodyDeclaration
		    {
		    dbg.location(482,10);
		    pushFollow(FOLLOW_classBodyDeclaration_in_classBody1468);
		    classBodyDeclaration();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop35;
                }
            } while (true);
            } finally {dbg.exitSubRule(35);}

            dbg.location(484,9);
            match(input,RBRACE,FOLLOW_RBRACE_in_classBody1489); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 22, classBody_StartIndex); }
        }
        dbg.location(485, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "classBody");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "classBody"


    // $ANTLR start "interfaceBody"
    // src/com/google/doclava/parser/Java.g:487:1: interfaceBody : '{' ( interfaceBodyDeclaration )* '}' ;
    public final void interfaceBody() throws RecognitionException {
        int interfaceBody_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "interfaceBody");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(487, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 23) ) { return ; }
            // src/com/google/doclava/parser/Java.g:488:5: ( '{' ( interfaceBodyDeclaration )* '}' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:488:9: '{' ( interfaceBodyDeclaration )* '}'
            {
            dbg.location(488,9);
            match(input,LBRACE,FOLLOW_LBRACE_in_interfaceBody1508); if (state.failed) return ;
            dbg.location(489,9);
            // src/com/google/doclava/parser/Java.g:489:9: ( interfaceBodyDeclaration )*
            try { dbg.enterSubRule(36);

            loop36:
            do {
                int alt36=2;
                try { dbg.enterDecision(36, decisionCanBacktrack[36]);

                int LA36_0 = input.LA(1);

                if ( (LA36_0==IDENTIFIER||LA36_0==ABSTRACT||LA36_0==BOOLEAN||LA36_0==BYTE||(LA36_0>=CHAR && LA36_0<=CLASS)||LA36_0==DOUBLE||LA36_0==ENUM||LA36_0==FINAL||LA36_0==FLOAT||(LA36_0>=INT && LA36_0<=NATIVE)||(LA36_0>=PRIVATE && LA36_0<=PUBLIC)||(LA36_0>=SHORT && LA36_0<=STRICTFP)||LA36_0==SYNCHRONIZED||LA36_0==TRANSIENT||(LA36_0>=VOID && LA36_0<=VOLATILE)||LA36_0==SEMI||LA36_0==MONKEYS_AT||LA36_0==LT) ) {
                    alt36=1;
                }


                } finally {dbg.exitDecision(36);}

                switch (alt36) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:489:10: interfaceBodyDeclaration
		    {
		    dbg.location(489,10);
		    pushFollow(FOLLOW_interfaceBodyDeclaration_in_interfaceBody1519);
		    interfaceBodyDeclaration();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop36;
                }
            } while (true);
            } finally {dbg.exitSubRule(36);}

            dbg.location(491,9);
            match(input,RBRACE,FOLLOW_RBRACE_in_interfaceBody1540); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 23, interfaceBody_StartIndex); }
        }
        dbg.location(492, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "interfaceBody");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "interfaceBody"


    // $ANTLR start "classBodyDeclaration"
    // src/com/google/doclava/parser/Java.g:494:1: classBodyDeclaration : ( ';' | ( 'static' )? block | memberDecl );
    public final void classBodyDeclaration() throws RecognitionException {
        int classBodyDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "classBodyDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(494, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 24) ) { return ; }
            // src/com/google/doclava/parser/Java.g:495:5: ( ';' | ( 'static' )? block | memberDecl )
            int alt38=3;
            try { dbg.enterDecision(38, decisionCanBacktrack[38]);

            switch ( input.LA(1) ) {
            case SEMI:
                {
                alt38=1;
                }
                break;
            case STATIC:
                {
                int LA38_2 = input.LA(2);

                if ( (LA38_2==LBRACE) ) {
                    alt38=2;
                }
                else if ( (LA38_2==IDENTIFIER||LA38_2==ABSTRACT||LA38_2==BOOLEAN||LA38_2==BYTE||(LA38_2>=CHAR && LA38_2<=CLASS)||LA38_2==DOUBLE||LA38_2==ENUM||LA38_2==FINAL||LA38_2==FLOAT||(LA38_2>=INT && LA38_2<=NATIVE)||(LA38_2>=PRIVATE && LA38_2<=PUBLIC)||(LA38_2>=SHORT && LA38_2<=STRICTFP)||LA38_2==SYNCHRONIZED||LA38_2==TRANSIENT||(LA38_2>=VOID && LA38_2<=VOLATILE)||LA38_2==MONKEYS_AT||LA38_2==LT) ) {
                    alt38=3;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 38, 2, input);

                    dbg.recognitionException(nvae);
                    throw nvae;
                }
                }
                break;
            case LBRACE:
                {
                alt38=2;
                }
                break;
            case IDENTIFIER:
            case ABSTRACT:
            case BOOLEAN:
            case BYTE:
            case CHAR:
            case CLASS:
            case DOUBLE:
            case ENUM:
            case FINAL:
            case FLOAT:
            case INT:
            case INTERFACE:
            case LONG:
            case NATIVE:
            case PRIVATE:
            case PROTECTED:
            case PUBLIC:
            case SHORT:
            case STRICTFP:
            case SYNCHRONIZED:
            case TRANSIENT:
            case VOID:
            case VOLATILE:
            case MONKEYS_AT:
            case LT:
                {
                alt38=3;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 38, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }

            } finally {dbg.exitDecision(38);}

            switch (alt38) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:495:9: ';'
                    {
                    dbg.location(495,9);
                    match(input,SEMI,FOLLOW_SEMI_in_classBodyDeclaration1559); if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:496:9: ( 'static' )? block
                    {
                    dbg.location(496,9);
                    // src/com/google/doclava/parser/Java.g:496:9: ( 'static' )?
                    int alt37=2;
                    try { dbg.enterSubRule(37);
                    try { dbg.enterDecision(37, decisionCanBacktrack[37]);

                    int LA37_0 = input.LA(1);

                    if ( (LA37_0==STATIC) ) {
                        alt37=1;
                    }
                    } finally {dbg.exitDecision(37);}

                    switch (alt37) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:496:10: 'static'
                            {
                            dbg.location(496,10);
                            match(input,STATIC,FOLLOW_STATIC_in_classBodyDeclaration1570); if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(37);}

                    dbg.location(498,9);
                    pushFollow(FOLLOW_block_in_classBodyDeclaration1591);
                    block();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:499:9: memberDecl
                    {
                    dbg.location(499,9);
                    pushFollow(FOLLOW_memberDecl_in_classBodyDeclaration1601);
                    memberDecl();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 24, classBodyDeclaration_StartIndex); }
        }
        dbg.location(500, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "classBodyDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "classBodyDeclaration"


    // $ANTLR start "memberDecl"
    // src/com/google/doclava/parser/Java.g:502:1: memberDecl : ( fieldDeclaration | methodDeclaration | classDeclaration | interfaceDeclaration );
    public final void memberDecl() throws RecognitionException {
        int memberDecl_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "memberDecl");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(502, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 25) ) { return ; }
            // src/com/google/doclava/parser/Java.g:503:5: ( fieldDeclaration | methodDeclaration | classDeclaration | interfaceDeclaration )
            int alt39=4;
            try { dbg.enterDecision(39, decisionCanBacktrack[39]);

            try {
                isCyclicDecision = true;
                alt39 = dfa39.predict(input);
            }
            catch (NoViableAltException nvae) {
                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(39);}

            switch (alt39) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:503:10: fieldDeclaration
                    {
                    dbg.location(503,10);
                    pushFollow(FOLLOW_fieldDeclaration_in_memberDecl1621);
                    fieldDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:504:10: methodDeclaration
                    {
                    dbg.location(504,10);
                    pushFollow(FOLLOW_methodDeclaration_in_memberDecl1632);
                    methodDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:505:10: classDeclaration
                    {
                    dbg.location(505,10);
                    pushFollow(FOLLOW_classDeclaration_in_memberDecl1643);
                    classDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 4 :
                    dbg.enterAlt(4);

                    // src/com/google/doclava/parser/Java.g:506:10: interfaceDeclaration
                    {
                    dbg.location(506,10);
                    pushFollow(FOLLOW_interfaceDeclaration_in_memberDecl1654);
                    interfaceDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 25, memberDecl_StartIndex); }
        }
        dbg.location(507, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "memberDecl");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "memberDecl"


    // $ANTLR start "methodDeclaration"
    // src/com/google/doclava/parser/Java.g:510:1: methodDeclaration : ( modifiers ( typeParameters )? IDENTIFIER formalParameters ( 'throws' qualifiedNameList )? '{' ( explicitConstructorInvocation )? ( blockStatement )* '}' | modifiers ( typeParameters )? ( type | 'void' ) IDENTIFIER formalParameters ( '[' ']' )* ( 'throws' qualifiedNameList )? ( block | ';' ) );
    public final void methodDeclaration() throws RecognitionException {
        int methodDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "methodDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(510, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 26) ) { return ; }
            // src/com/google/doclava/parser/Java.g:511:5: ( modifiers ( typeParameters )? IDENTIFIER formalParameters ( 'throws' qualifiedNameList )? '{' ( explicitConstructorInvocation )? ( blockStatement )* '}' | modifiers ( typeParameters )? ( type | 'void' ) IDENTIFIER formalParameters ( '[' ']' )* ( 'throws' qualifiedNameList )? ( block | ';' ) )
            int alt49=2;
            try { dbg.enterDecision(49, decisionCanBacktrack[49]);

            try {
                isCyclicDecision = true;
                alt49 = dfa49.predict(input);
            }
            catch (NoViableAltException nvae) {
                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(49);}

            switch (alt49) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:513:10: modifiers ( typeParameters )? IDENTIFIER formalParameters ( 'throws' qualifiedNameList )? '{' ( explicitConstructorInvocation )? ( blockStatement )* '}'
                    {
                    dbg.location(513,10);
                    pushFollow(FOLLOW_modifiers_in_methodDeclaration1691);
                    modifiers();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(514,9);
                    // src/com/google/doclava/parser/Java.g:514:9: ( typeParameters )?
                    int alt40=2;
                    try { dbg.enterSubRule(40);
                    try { dbg.enterDecision(40, decisionCanBacktrack[40]);

                    int LA40_0 = input.LA(1);

                    if ( (LA40_0==LT) ) {
                        alt40=1;
                    }
                    } finally {dbg.exitDecision(40);}

                    switch (alt40) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:514:10: typeParameters
                            {
                            dbg.location(514,10);
                            pushFollow(FOLLOW_typeParameters_in_methodDeclaration1702);
                            typeParameters();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(40);}

                    dbg.location(516,9);
                    match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_methodDeclaration1723); if (state.failed) return ;
                    dbg.location(517,9);
                    pushFollow(FOLLOW_formalParameters_in_methodDeclaration1733);
                    formalParameters();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(518,9);
                    // src/com/google/doclava/parser/Java.g:518:9: ( 'throws' qualifiedNameList )?
                    int alt41=2;
                    try { dbg.enterSubRule(41);
                    try { dbg.enterDecision(41, decisionCanBacktrack[41]);

                    int LA41_0 = input.LA(1);

                    if ( (LA41_0==THROWS) ) {
                        alt41=1;
                    }
                    } finally {dbg.exitDecision(41);}

                    switch (alt41) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:518:10: 'throws' qualifiedNameList
                            {
                            dbg.location(518,10);
                            match(input,THROWS,FOLLOW_THROWS_in_methodDeclaration1744); if (state.failed) return ;
                            dbg.location(518,19);
                            pushFollow(FOLLOW_qualifiedNameList_in_methodDeclaration1746);
                            qualifiedNameList();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(41);}

                    dbg.location(520,9);
                    match(input,LBRACE,FOLLOW_LBRACE_in_methodDeclaration1767); if (state.failed) return ;
                    dbg.location(521,9);
                    // src/com/google/doclava/parser/Java.g:521:9: ( explicitConstructorInvocation )?
                    int alt42=2;
                    try { dbg.enterSubRule(42);
                    try { dbg.enterDecision(42, decisionCanBacktrack[42]);

                    try {
                        isCyclicDecision = true;
                        alt42 = dfa42.predict(input);
                    }
                    catch (NoViableAltException nvae) {
                        dbg.recognitionException(nvae);
                        throw nvae;
                    }
                    } finally {dbg.exitDecision(42);}

                    switch (alt42) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:521:10: explicitConstructorInvocation
                            {
                            dbg.location(521,10);
                            pushFollow(FOLLOW_explicitConstructorInvocation_in_methodDeclaration1778);
                            explicitConstructorInvocation();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(42);}

                    dbg.location(523,9);
                    // src/com/google/doclava/parser/Java.g:523:9: ( blockStatement )*
                    try { dbg.enterSubRule(43);

                    loop43:
                    do {
                        int alt43=2;
                        try { dbg.enterDecision(43, decisionCanBacktrack[43]);

                        int LA43_0 = input.LA(1);

                        if ( ((LA43_0>=IDENTIFIER && LA43_0<=NULL)||(LA43_0>=ABSTRACT && LA43_0<=BYTE)||(LA43_0>=CHAR && LA43_0<=CLASS)||LA43_0==CONTINUE||(LA43_0>=DO && LA43_0<=DOUBLE)||LA43_0==ENUM||LA43_0==FINAL||(LA43_0>=FLOAT && LA43_0<=FOR)||LA43_0==IF||(LA43_0>=INT && LA43_0<=NEW)||(LA43_0>=PRIVATE && LA43_0<=THROW)||(LA43_0>=TRANSIENT && LA43_0<=LPAREN)||LA43_0==LBRACE||LA43_0==SEMI||(LA43_0>=BANG && LA43_0<=TILDE)||(LA43_0>=PLUSPLUS && LA43_0<=SUB)||LA43_0==MONKEYS_AT||LA43_0==LT) ) {
                            alt43=1;
                        }


                        } finally {dbg.exitDecision(43);}

                        switch (alt43) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:523:10: blockStatement
			    {
			    dbg.location(523,10);
			    pushFollow(FOLLOW_blockStatement_in_methodDeclaration1800);
			    blockStatement();

			    state._fsp--;
			    if (state.failed) return ;

			    }
			    break;

			default :
			    break loop43;
                        }
                    } while (true);
                    } finally {dbg.exitSubRule(43);}

                    dbg.location(525,9);
                    match(input,RBRACE,FOLLOW_RBRACE_in_methodDeclaration1821); if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:526:9: modifiers ( typeParameters )? ( type | 'void' ) IDENTIFIER formalParameters ( '[' ']' )* ( 'throws' qualifiedNameList )? ( block | ';' )
                    {
                    dbg.location(526,9);
                    pushFollow(FOLLOW_modifiers_in_methodDeclaration1831);
                    modifiers();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(527,9);
                    // src/com/google/doclava/parser/Java.g:527:9: ( typeParameters )?
                    int alt44=2;
                    try { dbg.enterSubRule(44);
                    try { dbg.enterDecision(44, decisionCanBacktrack[44]);

                    int LA44_0 = input.LA(1);

                    if ( (LA44_0==LT) ) {
                        alt44=1;
                    }
                    } finally {dbg.exitDecision(44);}

                    switch (alt44) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:527:10: typeParameters
                            {
                            dbg.location(527,10);
                            pushFollow(FOLLOW_typeParameters_in_methodDeclaration1842);
                            typeParameters();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(44);}

                    dbg.location(529,9);
                    // src/com/google/doclava/parser/Java.g:529:9: ( type | 'void' )
                    int alt45=2;
                    try { dbg.enterSubRule(45);
                    try { dbg.enterDecision(45, decisionCanBacktrack[45]);

                    int LA45_0 = input.LA(1);

                    if ( (LA45_0==IDENTIFIER||LA45_0==BOOLEAN||LA45_0==BYTE||LA45_0==CHAR||LA45_0==DOUBLE||LA45_0==FLOAT||LA45_0==INT||LA45_0==LONG||LA45_0==SHORT) ) {
                        alt45=1;
                    }
                    else if ( (LA45_0==VOID) ) {
                        alt45=2;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        NoViableAltException nvae =
                            new NoViableAltException("", 45, 0, input);

                        dbg.recognitionException(nvae);
                        throw nvae;
                    }
                    } finally {dbg.exitDecision(45);}

                    switch (alt45) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:529:10: type
                            {
                            dbg.location(529,10);
                            pushFollow(FOLLOW_type_in_methodDeclaration1864);
                            type();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;
                        case 2 :
                            dbg.enterAlt(2);

                            // src/com/google/doclava/parser/Java.g:530:13: 'void'
                            {
                            dbg.location(530,13);
                            match(input,VOID,FOLLOW_VOID_in_methodDeclaration1878); if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(45);}

                    dbg.location(532,9);
                    match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_methodDeclaration1898); if (state.failed) return ;
                    dbg.location(533,9);
                    pushFollow(FOLLOW_formalParameters_in_methodDeclaration1908);
                    formalParameters();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(534,9);
                    // src/com/google/doclava/parser/Java.g:534:9: ( '[' ']' )*
                    try { dbg.enterSubRule(46);

                    loop46:
                    do {
                        int alt46=2;
                        try { dbg.enterDecision(46, decisionCanBacktrack[46]);

                        int LA46_0 = input.LA(1);

                        if ( (LA46_0==LBRACKET) ) {
                            alt46=1;
                        }


                        } finally {dbg.exitDecision(46);}

                        switch (alt46) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:534:10: '[' ']'
			    {
			    dbg.location(534,10);
			    match(input,LBRACKET,FOLLOW_LBRACKET_in_methodDeclaration1919); if (state.failed) return ;
			    dbg.location(534,14);
			    match(input,RBRACKET,FOLLOW_RBRACKET_in_methodDeclaration1921); if (state.failed) return ;

			    }
			    break;

			default :
			    break loop46;
                        }
                    } while (true);
                    } finally {dbg.exitSubRule(46);}

                    dbg.location(536,9);
                    // src/com/google/doclava/parser/Java.g:536:9: ( 'throws' qualifiedNameList )?
                    int alt47=2;
                    try { dbg.enterSubRule(47);
                    try { dbg.enterDecision(47, decisionCanBacktrack[47]);

                    int LA47_0 = input.LA(1);

                    if ( (LA47_0==THROWS) ) {
                        alt47=1;
                    }
                    } finally {dbg.exitDecision(47);}

                    switch (alt47) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:536:10: 'throws' qualifiedNameList
                            {
                            dbg.location(536,10);
                            match(input,THROWS,FOLLOW_THROWS_in_methodDeclaration1943); if (state.failed) return ;
                            dbg.location(536,19);
                            pushFollow(FOLLOW_qualifiedNameList_in_methodDeclaration1945);
                            qualifiedNameList();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(47);}

                    dbg.location(538,9);
                    // src/com/google/doclava/parser/Java.g:538:9: ( block | ';' )
                    int alt48=2;
                    try { dbg.enterSubRule(48);
                    try { dbg.enterDecision(48, decisionCanBacktrack[48]);

                    int LA48_0 = input.LA(1);

                    if ( (LA48_0==LBRACE) ) {
                        alt48=1;
                    }
                    else if ( (LA48_0==SEMI) ) {
                        alt48=2;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        NoViableAltException nvae =
                            new NoViableAltException("", 48, 0, input);

                        dbg.recognitionException(nvae);
                        throw nvae;
                    }
                    } finally {dbg.exitDecision(48);}

                    switch (alt48) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:539:13: block
                            {
                            dbg.location(539,13);
                            pushFollow(FOLLOW_block_in_methodDeclaration1980);
                            block();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;
                        case 2 :
                            dbg.enterAlt(2);

                            // src/com/google/doclava/parser/Java.g:540:13: ';'
                            {
                            dbg.location(540,13);
                            match(input,SEMI,FOLLOW_SEMI_in_methodDeclaration1994); if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(48);}


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 26, methodDeclaration_StartIndex); }
        }
        dbg.location(542, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "methodDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "methodDeclaration"


    // $ANTLR start "fieldDeclaration"
    // src/com/google/doclava/parser/Java.g:545:1: fieldDeclaration : modifiers type variableDeclarator ( ',' variableDeclarator )* ';' ;
    public final void fieldDeclaration() throws RecognitionException {
        int fieldDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "fieldDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(545, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 27) ) { return ; }
            // src/com/google/doclava/parser/Java.g:546:5: ( modifiers type variableDeclarator ( ',' variableDeclarator )* ';' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:546:9: modifiers type variableDeclarator ( ',' variableDeclarator )* ';'
            {
            dbg.location(546,9);
            pushFollow(FOLLOW_modifiers_in_fieldDeclaration2024);
            modifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(547,9);
            pushFollow(FOLLOW_type_in_fieldDeclaration2034);
            type();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(548,9);
            pushFollow(FOLLOW_variableDeclarator_in_fieldDeclaration2044);
            variableDeclarator();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(549,9);
            // src/com/google/doclava/parser/Java.g:549:9: ( ',' variableDeclarator )*
            try { dbg.enterSubRule(50);

            loop50:
            do {
                int alt50=2;
                try { dbg.enterDecision(50, decisionCanBacktrack[50]);

                int LA50_0 = input.LA(1);

                if ( (LA50_0==COMMA) ) {
                    alt50=1;
                }


                } finally {dbg.exitDecision(50);}

                switch (alt50) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:549:10: ',' variableDeclarator
		    {
		    dbg.location(549,10);
		    match(input,COMMA,FOLLOW_COMMA_in_fieldDeclaration2055); if (state.failed) return ;
		    dbg.location(549,14);
		    pushFollow(FOLLOW_variableDeclarator_in_fieldDeclaration2057);
		    variableDeclarator();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop50;
                }
            } while (true);
            } finally {dbg.exitSubRule(50);}

            dbg.location(551,9);
            match(input,SEMI,FOLLOW_SEMI_in_fieldDeclaration2078); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 27, fieldDeclaration_StartIndex); }
        }
        dbg.location(552, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "fieldDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "fieldDeclaration"


    // $ANTLR start "variableDeclarator"
    // src/com/google/doclava/parser/Java.g:554:1: variableDeclarator : IDENTIFIER ( '[' ']' )* ( '=' variableInitializer )? ;
    public final void variableDeclarator() throws RecognitionException {
        int variableDeclarator_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "variableDeclarator");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(554, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 28) ) { return ; }
            // src/com/google/doclava/parser/Java.g:555:5: ( IDENTIFIER ( '[' ']' )* ( '=' variableInitializer )? )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:555:9: IDENTIFIER ( '[' ']' )* ( '=' variableInitializer )?
            {
            dbg.location(555,9);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_variableDeclarator2097); if (state.failed) return ;
            dbg.location(556,9);
            // src/com/google/doclava/parser/Java.g:556:9: ( '[' ']' )*
            try { dbg.enterSubRule(51);

            loop51:
            do {
                int alt51=2;
                try { dbg.enterDecision(51, decisionCanBacktrack[51]);

                int LA51_0 = input.LA(1);

                if ( (LA51_0==LBRACKET) ) {
                    alt51=1;
                }


                } finally {dbg.exitDecision(51);}

                switch (alt51) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:556:10: '[' ']'
		    {
		    dbg.location(556,10);
		    match(input,LBRACKET,FOLLOW_LBRACKET_in_variableDeclarator2108); if (state.failed) return ;
		    dbg.location(556,14);
		    match(input,RBRACKET,FOLLOW_RBRACKET_in_variableDeclarator2110); if (state.failed) return ;

		    }
		    break;

		default :
		    break loop51;
                }
            } while (true);
            } finally {dbg.exitSubRule(51);}

            dbg.location(558,9);
            // src/com/google/doclava/parser/Java.g:558:9: ( '=' variableInitializer )?
            int alt52=2;
            try { dbg.enterSubRule(52);
            try { dbg.enterDecision(52, decisionCanBacktrack[52]);

            int LA52_0 = input.LA(1);

            if ( (LA52_0==EQ) ) {
                alt52=1;
            }
            } finally {dbg.exitDecision(52);}

            switch (alt52) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:558:10: '=' variableInitializer
                    {
                    dbg.location(558,10);
                    match(input,EQ,FOLLOW_EQ_in_variableDeclarator2132); if (state.failed) return ;
                    dbg.location(558,14);
                    pushFollow(FOLLOW_variableInitializer_in_variableDeclarator2134);
                    variableInitializer();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(52);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 28, variableDeclarator_StartIndex); }
        }
        dbg.location(560, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "variableDeclarator");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "variableDeclarator"


    // $ANTLR start "interfaceBodyDeclaration"
    // src/com/google/doclava/parser/Java.g:562:1: interfaceBodyDeclaration : ( interfaceFieldDeclaration | interfaceMethodDeclaration | interfaceDeclaration | classDeclaration | ';' );
    public final void interfaceBodyDeclaration() throws RecognitionException {
        int interfaceBodyDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "interfaceBodyDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(562, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 29) ) { return ; }
            // src/com/google/doclava/parser/Java.g:566:5: ( interfaceFieldDeclaration | interfaceMethodDeclaration | interfaceDeclaration | classDeclaration | ';' )
            int alt53=5;
            try { dbg.enterDecision(53, decisionCanBacktrack[53]);

            try {
                isCyclicDecision = true;
                alt53 = dfa53.predict(input);
            }
            catch (NoViableAltException nvae) {
                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(53);}

            switch (alt53) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:567:9: interfaceFieldDeclaration
                    {
                    dbg.location(567,9);
                    pushFollow(FOLLOW_interfaceFieldDeclaration_in_interfaceBodyDeclaration2172);
                    interfaceFieldDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:568:9: interfaceMethodDeclaration
                    {
                    dbg.location(568,9);
                    pushFollow(FOLLOW_interfaceMethodDeclaration_in_interfaceBodyDeclaration2182);
                    interfaceMethodDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:569:9: interfaceDeclaration
                    {
                    dbg.location(569,9);
                    pushFollow(FOLLOW_interfaceDeclaration_in_interfaceBodyDeclaration2192);
                    interfaceDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 4 :
                    dbg.enterAlt(4);

                    // src/com/google/doclava/parser/Java.g:570:9: classDeclaration
                    {
                    dbg.location(570,9);
                    pushFollow(FOLLOW_classDeclaration_in_interfaceBodyDeclaration2202);
                    classDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 5 :
                    dbg.enterAlt(5);

                    // src/com/google/doclava/parser/Java.g:571:9: ';'
                    {
                    dbg.location(571,9);
                    match(input,SEMI,FOLLOW_SEMI_in_interfaceBodyDeclaration2212); if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 29, interfaceBodyDeclaration_StartIndex); }
        }
        dbg.location(572, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "interfaceBodyDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "interfaceBodyDeclaration"


    // $ANTLR start "interfaceMethodDeclaration"
    // src/com/google/doclava/parser/Java.g:574:1: interfaceMethodDeclaration : modifiers ( typeParameters )? ( type | 'void' ) IDENTIFIER formalParameters ( '[' ']' )* ( 'throws' qualifiedNameList )? ';' ;
    public final void interfaceMethodDeclaration() throws RecognitionException {
        int interfaceMethodDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "interfaceMethodDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(574, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 30) ) { return ; }
            // src/com/google/doclava/parser/Java.g:575:5: ( modifiers ( typeParameters )? ( type | 'void' ) IDENTIFIER formalParameters ( '[' ']' )* ( 'throws' qualifiedNameList )? ';' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:575:9: modifiers ( typeParameters )? ( type | 'void' ) IDENTIFIER formalParameters ( '[' ']' )* ( 'throws' qualifiedNameList )? ';'
            {
            dbg.location(575,9);
            pushFollow(FOLLOW_modifiers_in_interfaceMethodDeclaration2231);
            modifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(576,9);
            // src/com/google/doclava/parser/Java.g:576:9: ( typeParameters )?
            int alt54=2;
            try { dbg.enterSubRule(54);
            try { dbg.enterDecision(54, decisionCanBacktrack[54]);

            int LA54_0 = input.LA(1);

            if ( (LA54_0==LT) ) {
                alt54=1;
            }
            } finally {dbg.exitDecision(54);}

            switch (alt54) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:576:10: typeParameters
                    {
                    dbg.location(576,10);
                    pushFollow(FOLLOW_typeParameters_in_interfaceMethodDeclaration2242);
                    typeParameters();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(54);}

            dbg.location(578,9);
            // src/com/google/doclava/parser/Java.g:578:9: ( type | 'void' )
            int alt55=2;
            try { dbg.enterSubRule(55);
            try { dbg.enterDecision(55, decisionCanBacktrack[55]);

            int LA55_0 = input.LA(1);

            if ( (LA55_0==IDENTIFIER||LA55_0==BOOLEAN||LA55_0==BYTE||LA55_0==CHAR||LA55_0==DOUBLE||LA55_0==FLOAT||LA55_0==INT||LA55_0==LONG||LA55_0==SHORT) ) {
                alt55=1;
            }
            else if ( (LA55_0==VOID) ) {
                alt55=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 55, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(55);}

            switch (alt55) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:578:10: type
                    {
                    dbg.location(578,10);
                    pushFollow(FOLLOW_type_in_interfaceMethodDeclaration2264);
                    type();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:579:10: 'void'
                    {
                    dbg.location(579,10);
                    match(input,VOID,FOLLOW_VOID_in_interfaceMethodDeclaration2275); if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(55);}

            dbg.location(581,9);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_interfaceMethodDeclaration2295); if (state.failed) return ;
            dbg.location(582,9);
            pushFollow(FOLLOW_formalParameters_in_interfaceMethodDeclaration2305);
            formalParameters();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(583,9);
            // src/com/google/doclava/parser/Java.g:583:9: ( '[' ']' )*
            try { dbg.enterSubRule(56);

            loop56:
            do {
                int alt56=2;
                try { dbg.enterDecision(56, decisionCanBacktrack[56]);

                int LA56_0 = input.LA(1);

                if ( (LA56_0==LBRACKET) ) {
                    alt56=1;
                }


                } finally {dbg.exitDecision(56);}

                switch (alt56) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:583:10: '[' ']'
		    {
		    dbg.location(583,10);
		    match(input,LBRACKET,FOLLOW_LBRACKET_in_interfaceMethodDeclaration2316); if (state.failed) return ;
		    dbg.location(583,14);
		    match(input,RBRACKET,FOLLOW_RBRACKET_in_interfaceMethodDeclaration2318); if (state.failed) return ;

		    }
		    break;

		default :
		    break loop56;
                }
            } while (true);
            } finally {dbg.exitSubRule(56);}

            dbg.location(585,9);
            // src/com/google/doclava/parser/Java.g:585:9: ( 'throws' qualifiedNameList )?
            int alt57=2;
            try { dbg.enterSubRule(57);
            try { dbg.enterDecision(57, decisionCanBacktrack[57]);

            int LA57_0 = input.LA(1);

            if ( (LA57_0==THROWS) ) {
                alt57=1;
            }
            } finally {dbg.exitDecision(57);}

            switch (alt57) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:585:10: 'throws' qualifiedNameList
                    {
                    dbg.location(585,10);
                    match(input,THROWS,FOLLOW_THROWS_in_interfaceMethodDeclaration2340); if (state.failed) return ;
                    dbg.location(585,19);
                    pushFollow(FOLLOW_qualifiedNameList_in_interfaceMethodDeclaration2342);
                    qualifiedNameList();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(57);}

            dbg.location(586,12);
            match(input,SEMI,FOLLOW_SEMI_in_interfaceMethodDeclaration2355); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 30, interfaceMethodDeclaration_StartIndex); }
        }
        dbg.location(587, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "interfaceMethodDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "interfaceMethodDeclaration"


    // $ANTLR start "interfaceFieldDeclaration"
    // src/com/google/doclava/parser/Java.g:589:1: interfaceFieldDeclaration : modifiers type variableDeclarator ( ',' variableDeclarator )* ';' ;
    public final void interfaceFieldDeclaration() throws RecognitionException {
        int interfaceFieldDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "interfaceFieldDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(589, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 31) ) { return ; }
            // src/com/google/doclava/parser/Java.g:595:5: ( modifiers type variableDeclarator ( ',' variableDeclarator )* ';' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:595:9: modifiers type variableDeclarator ( ',' variableDeclarator )* ';'
            {
            dbg.location(595,9);
            pushFollow(FOLLOW_modifiers_in_interfaceFieldDeclaration2376);
            modifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(595,19);
            pushFollow(FOLLOW_type_in_interfaceFieldDeclaration2378);
            type();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(595,24);
            pushFollow(FOLLOW_variableDeclarator_in_interfaceFieldDeclaration2380);
            variableDeclarator();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(596,9);
            // src/com/google/doclava/parser/Java.g:596:9: ( ',' variableDeclarator )*
            try { dbg.enterSubRule(58);

            loop58:
            do {
                int alt58=2;
                try { dbg.enterDecision(58, decisionCanBacktrack[58]);

                int LA58_0 = input.LA(1);

                if ( (LA58_0==COMMA) ) {
                    alt58=1;
                }


                } finally {dbg.exitDecision(58);}

                switch (alt58) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:596:10: ',' variableDeclarator
		    {
		    dbg.location(596,10);
		    match(input,COMMA,FOLLOW_COMMA_in_interfaceFieldDeclaration2391); if (state.failed) return ;
		    dbg.location(596,14);
		    pushFollow(FOLLOW_variableDeclarator_in_interfaceFieldDeclaration2393);
		    variableDeclarator();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop58;
                }
            } while (true);
            } finally {dbg.exitSubRule(58);}

            dbg.location(598,9);
            match(input,SEMI,FOLLOW_SEMI_in_interfaceFieldDeclaration2414); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 31, interfaceFieldDeclaration_StartIndex); }
        }
        dbg.location(599, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "interfaceFieldDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "interfaceFieldDeclaration"


    // $ANTLR start "type"
    // src/com/google/doclava/parser/Java.g:602:1: type : ( classOrInterfaceType ( '[' ']' )* | primitiveType ( '[' ']' )* );
    public final void type() throws RecognitionException {
        int type_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "type");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(602, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 32) ) { return ; }
            // src/com/google/doclava/parser/Java.g:603:5: ( classOrInterfaceType ( '[' ']' )* | primitiveType ( '[' ']' )* )
            int alt61=2;
            try { dbg.enterDecision(61, decisionCanBacktrack[61]);

            int LA61_0 = input.LA(1);

            if ( (LA61_0==IDENTIFIER) ) {
                alt61=1;
            }
            else if ( (LA61_0==BOOLEAN||LA61_0==BYTE||LA61_0==CHAR||LA61_0==DOUBLE||LA61_0==FLOAT||LA61_0==INT||LA61_0==LONG||LA61_0==SHORT) ) {
                alt61=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 61, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(61);}

            switch (alt61) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:603:9: classOrInterfaceType ( '[' ']' )*
                    {
                    dbg.location(603,9);
                    pushFollow(FOLLOW_classOrInterfaceType_in_type2434);
                    classOrInterfaceType();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(604,9);
                    // src/com/google/doclava/parser/Java.g:604:9: ( '[' ']' )*
                    try { dbg.enterSubRule(59);

                    loop59:
                    do {
                        int alt59=2;
                        try { dbg.enterDecision(59, decisionCanBacktrack[59]);

                        int LA59_0 = input.LA(1);

                        if ( (LA59_0==LBRACKET) ) {
                            alt59=1;
                        }


                        } finally {dbg.exitDecision(59);}

                        switch (alt59) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:604:10: '[' ']'
			    {
			    dbg.location(604,10);
			    match(input,LBRACKET,FOLLOW_LBRACKET_in_type2445); if (state.failed) return ;
			    dbg.location(604,14);
			    match(input,RBRACKET,FOLLOW_RBRACKET_in_type2447); if (state.failed) return ;

			    }
			    break;

			default :
			    break loop59;
                        }
                    } while (true);
                    } finally {dbg.exitSubRule(59);}


                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:606:9: primitiveType ( '[' ']' )*
                    {
                    dbg.location(606,9);
                    pushFollow(FOLLOW_primitiveType_in_type2468);
                    primitiveType();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(607,9);
                    // src/com/google/doclava/parser/Java.g:607:9: ( '[' ']' )*
                    try { dbg.enterSubRule(60);

                    loop60:
                    do {
                        int alt60=2;
                        try { dbg.enterDecision(60, decisionCanBacktrack[60]);

                        int LA60_0 = input.LA(1);

                        if ( (LA60_0==LBRACKET) ) {
                            alt60=1;
                        }


                        } finally {dbg.exitDecision(60);}

                        switch (alt60) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:607:10: '[' ']'
			    {
			    dbg.location(607,10);
			    match(input,LBRACKET,FOLLOW_LBRACKET_in_type2479); if (state.failed) return ;
			    dbg.location(607,14);
			    match(input,RBRACKET,FOLLOW_RBRACKET_in_type2481); if (state.failed) return ;

			    }
			    break;

			default :
			    break loop60;
                        }
                    } while (true);
                    } finally {dbg.exitSubRule(60);}


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 32, type_StartIndex); }
        }
        dbg.location(609, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "type");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "type"


    // $ANTLR start "classOrInterfaceType"
    // src/com/google/doclava/parser/Java.g:612:1: classOrInterfaceType : IDENTIFIER ( typeArguments )? ( '.' IDENTIFIER ( typeArguments )? )* ;
    public final void classOrInterfaceType() throws RecognitionException {
        int classOrInterfaceType_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "classOrInterfaceType");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(612, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 33) ) { return ; }
            // src/com/google/doclava/parser/Java.g:613:5: ( IDENTIFIER ( typeArguments )? ( '.' IDENTIFIER ( typeArguments )? )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:613:9: IDENTIFIER ( typeArguments )? ( '.' IDENTIFIER ( typeArguments )? )*
            {
            dbg.location(613,9);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_classOrInterfaceType2512); if (state.failed) return ;
            dbg.location(614,9);
            // src/com/google/doclava/parser/Java.g:614:9: ( typeArguments )?
            int alt62=2;
            try { dbg.enterSubRule(62);
            try { dbg.enterDecision(62, decisionCanBacktrack[62]);

            int LA62_0 = input.LA(1);

            if ( (LA62_0==LT) ) {
                int LA62_1 = input.LA(2);

                if ( (LA62_1==IDENTIFIER||LA62_1==BOOLEAN||LA62_1==BYTE||LA62_1==CHAR||LA62_1==DOUBLE||LA62_1==FLOAT||LA62_1==INT||LA62_1==LONG||LA62_1==SHORT||LA62_1==QUES) ) {
                    alt62=1;
                }
            }
            } finally {dbg.exitDecision(62);}

            switch (alt62) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:614:10: typeArguments
                    {
                    dbg.location(614,10);
                    pushFollow(FOLLOW_typeArguments_in_classOrInterfaceType2523);
                    typeArguments();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(62);}

            dbg.location(616,9);
            // src/com/google/doclava/parser/Java.g:616:9: ( '.' IDENTIFIER ( typeArguments )? )*
            try { dbg.enterSubRule(64);

            loop64:
            do {
                int alt64=2;
                try { dbg.enterDecision(64, decisionCanBacktrack[64]);

                int LA64_0 = input.LA(1);

                if ( (LA64_0==DOT) ) {
                    alt64=1;
                }


                } finally {dbg.exitDecision(64);}

                switch (alt64) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:616:10: '.' IDENTIFIER ( typeArguments )?
		    {
		    dbg.location(616,10);
		    match(input,DOT,FOLLOW_DOT_in_classOrInterfaceType2545); if (state.failed) return ;
		    dbg.location(616,14);
		    match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_classOrInterfaceType2547); if (state.failed) return ;
		    dbg.location(617,13);
		    // src/com/google/doclava/parser/Java.g:617:13: ( typeArguments )?
		    int alt63=2;
		    try { dbg.enterSubRule(63);
		    try { dbg.enterDecision(63, decisionCanBacktrack[63]);

		    int LA63_0 = input.LA(1);

		    if ( (LA63_0==LT) ) {
		        int LA63_1 = input.LA(2);

		        if ( (LA63_1==IDENTIFIER||LA63_1==BOOLEAN||LA63_1==BYTE||LA63_1==CHAR||LA63_1==DOUBLE||LA63_1==FLOAT||LA63_1==INT||LA63_1==LONG||LA63_1==SHORT||LA63_1==QUES) ) {
		            alt63=1;
		        }
		    }
		    } finally {dbg.exitDecision(63);}

		    switch (alt63) {
		        case 1 :
		            dbg.enterAlt(1);

		            // src/com/google/doclava/parser/Java.g:617:14: typeArguments
		            {
		            dbg.location(617,14);
		            pushFollow(FOLLOW_typeArguments_in_classOrInterfaceType2562);
		            typeArguments();

		            state._fsp--;
		            if (state.failed) return ;

		            }
		            break;

		    }
		    } finally {dbg.exitSubRule(63);}


		    }
		    break;

		default :
		    break loop64;
                }
            } while (true);
            } finally {dbg.exitSubRule(64);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 33, classOrInterfaceType_StartIndex); }
        }
        dbg.location(620, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "classOrInterfaceType");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "classOrInterfaceType"


    // $ANTLR start "primitiveType"
    // src/com/google/doclava/parser/Java.g:622:1: primitiveType : ( 'boolean' | 'char' | 'byte' | 'short' | 'int' | 'long' | 'float' | 'double' );
    public final void primitiveType() throws RecognitionException {
        int primitiveType_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "primitiveType");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(622, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 34) ) { return ; }
            // src/com/google/doclava/parser/Java.g:623:5: ( 'boolean' | 'char' | 'byte' | 'short' | 'int' | 'long' | 'float' | 'double' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:
            {
            dbg.location(623,5);
            if ( input.LA(1)==BOOLEAN||input.LA(1)==BYTE||input.LA(1)==CHAR||input.LA(1)==DOUBLE||input.LA(1)==FLOAT||input.LA(1)==INT||input.LA(1)==LONG||input.LA(1)==SHORT ) {
                input.consume();
                state.errorRecovery=false;state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                dbg.recognitionException(mse);
                throw mse;
            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 34, primitiveType_StartIndex); }
        }
        dbg.location(631, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "primitiveType");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "primitiveType"


    // $ANTLR start "typeArguments"
    // src/com/google/doclava/parser/Java.g:633:1: typeArguments : '<' typeArgument ( ',' typeArgument )* '>' ;
    public final void typeArguments() throws RecognitionException {
        int typeArguments_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "typeArguments");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(633, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 35) ) { return ; }
            // src/com/google/doclava/parser/Java.g:634:5: ( '<' typeArgument ( ',' typeArgument )* '>' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:634:9: '<' typeArgument ( ',' typeArgument )* '>'
            {
            dbg.location(634,9);
            match(input,LT,FOLLOW_LT_in_typeArguments2696); if (state.failed) return ;
            dbg.location(634,13);
            pushFollow(FOLLOW_typeArgument_in_typeArguments2698);
            typeArgument();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(635,9);
            // src/com/google/doclava/parser/Java.g:635:9: ( ',' typeArgument )*
            try { dbg.enterSubRule(65);

            loop65:
            do {
                int alt65=2;
                try { dbg.enterDecision(65, decisionCanBacktrack[65]);

                int LA65_0 = input.LA(1);

                if ( (LA65_0==COMMA) ) {
                    alt65=1;
                }


                } finally {dbg.exitDecision(65);}

                switch (alt65) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:635:10: ',' typeArgument
		    {
		    dbg.location(635,10);
		    match(input,COMMA,FOLLOW_COMMA_in_typeArguments2709); if (state.failed) return ;
		    dbg.location(635,14);
		    pushFollow(FOLLOW_typeArgument_in_typeArguments2711);
		    typeArgument();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop65;
                }
            } while (true);
            } finally {dbg.exitSubRule(65);}

            dbg.location(637,9);
            match(input,GT,FOLLOW_GT_in_typeArguments2732); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 35, typeArguments_StartIndex); }
        }
        dbg.location(638, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "typeArguments");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "typeArguments"


    // $ANTLR start "typeArgument"
    // src/com/google/doclava/parser/Java.g:640:1: typeArgument : ( type | '?' ( ( 'extends' | 'super' ) type )? );
    public final void typeArgument() throws RecognitionException {
        int typeArgument_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "typeArgument");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(640, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 36) ) { return ; }
            // src/com/google/doclava/parser/Java.g:641:5: ( type | '?' ( ( 'extends' | 'super' ) type )? )
            int alt67=2;
            try { dbg.enterDecision(67, decisionCanBacktrack[67]);

            int LA67_0 = input.LA(1);

            if ( (LA67_0==IDENTIFIER||LA67_0==BOOLEAN||LA67_0==BYTE||LA67_0==CHAR||LA67_0==DOUBLE||LA67_0==FLOAT||LA67_0==INT||LA67_0==LONG||LA67_0==SHORT) ) {
                alt67=1;
            }
            else if ( (LA67_0==QUES) ) {
                alt67=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 67, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(67);}

            switch (alt67) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:641:9: type
                    {
                    dbg.location(641,9);
                    pushFollow(FOLLOW_type_in_typeArgument2751);
                    type();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:642:9: '?' ( ( 'extends' | 'super' ) type )?
                    {
                    dbg.location(642,9);
                    match(input,QUES,FOLLOW_QUES_in_typeArgument2761); if (state.failed) return ;
                    dbg.location(643,9);
                    // src/com/google/doclava/parser/Java.g:643:9: ( ( 'extends' | 'super' ) type )?
                    int alt66=2;
                    try { dbg.enterSubRule(66);
                    try { dbg.enterDecision(66, decisionCanBacktrack[66]);

                    int LA66_0 = input.LA(1);

                    if ( (LA66_0==EXTENDS||LA66_0==SUPER) ) {
                        alt66=1;
                    }
                    } finally {dbg.exitDecision(66);}

                    switch (alt66) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:644:13: ( 'extends' | 'super' ) type
                            {
                            dbg.location(644,13);
                            if ( input.LA(1)==EXTENDS||input.LA(1)==SUPER ) {
                                input.consume();
                                state.errorRecovery=false;state.failed=false;
                            }
                            else {
                                if (state.backtracking>0) {state.failed=true; return ;}
                                MismatchedSetException mse = new MismatchedSetException(null,input);
                                dbg.recognitionException(mse);
                                throw mse;
                            }

                            dbg.location(647,13);
                            pushFollow(FOLLOW_type_in_typeArgument2829);
                            type();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(66);}


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 36, typeArgument_StartIndex); }
        }
        dbg.location(649, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "typeArgument");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "typeArgument"


    // $ANTLR start "qualifiedNameList"
    // src/com/google/doclava/parser/Java.g:651:1: qualifiedNameList : qualifiedName ( ',' qualifiedName )* ;
    public final void qualifiedNameList() throws RecognitionException {
        int qualifiedNameList_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "qualifiedNameList");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(651, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 37) ) { return ; }
            // src/com/google/doclava/parser/Java.g:652:5: ( qualifiedName ( ',' qualifiedName )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:652:9: qualifiedName ( ',' qualifiedName )*
            {
            dbg.location(652,9);
            pushFollow(FOLLOW_qualifiedName_in_qualifiedNameList2859);
            qualifiedName();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(653,9);
            // src/com/google/doclava/parser/Java.g:653:9: ( ',' qualifiedName )*
            try { dbg.enterSubRule(68);

            loop68:
            do {
                int alt68=2;
                try { dbg.enterDecision(68, decisionCanBacktrack[68]);

                int LA68_0 = input.LA(1);

                if ( (LA68_0==COMMA) ) {
                    alt68=1;
                }


                } finally {dbg.exitDecision(68);}

                switch (alt68) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:653:10: ',' qualifiedName
		    {
		    dbg.location(653,10);
		    match(input,COMMA,FOLLOW_COMMA_in_qualifiedNameList2870); if (state.failed) return ;
		    dbg.location(653,14);
		    pushFollow(FOLLOW_qualifiedName_in_qualifiedNameList2872);
		    qualifiedName();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop68;
                }
            } while (true);
            } finally {dbg.exitSubRule(68);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 37, qualifiedNameList_StartIndex); }
        }
        dbg.location(655, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "qualifiedNameList");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "qualifiedNameList"


    // $ANTLR start "formalParameters"
    // src/com/google/doclava/parser/Java.g:657:1: formalParameters : '(' ( formalParameterDecls )? ')' ;
    public final void formalParameters() throws RecognitionException {
        int formalParameters_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "formalParameters");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(657, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 38) ) { return ; }
            // src/com/google/doclava/parser/Java.g:658:5: ( '(' ( formalParameterDecls )? ')' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:658:9: '(' ( formalParameterDecls )? ')'
            {
            dbg.location(658,9);
            match(input,LPAREN,FOLLOW_LPAREN_in_formalParameters2902); if (state.failed) return ;
            dbg.location(659,9);
            // src/com/google/doclava/parser/Java.g:659:9: ( formalParameterDecls )?
            int alt69=2;
            try { dbg.enterSubRule(69);
            try { dbg.enterDecision(69, decisionCanBacktrack[69]);

            int LA69_0 = input.LA(1);

            if ( (LA69_0==IDENTIFIER||LA69_0==BOOLEAN||LA69_0==BYTE||LA69_0==CHAR||LA69_0==DOUBLE||LA69_0==FINAL||LA69_0==FLOAT||LA69_0==INT||LA69_0==LONG||LA69_0==SHORT||LA69_0==MONKEYS_AT) ) {
                alt69=1;
            }
            } finally {dbg.exitDecision(69);}

            switch (alt69) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:659:10: formalParameterDecls
                    {
                    dbg.location(659,10);
                    pushFollow(FOLLOW_formalParameterDecls_in_formalParameters2913);
                    formalParameterDecls();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(69);}

            dbg.location(661,9);
            match(input,RPAREN,FOLLOW_RPAREN_in_formalParameters2934); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 38, formalParameters_StartIndex); }
        }
        dbg.location(662, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "formalParameters");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "formalParameters"


    // $ANTLR start "formalParameterDecls"
    // src/com/google/doclava/parser/Java.g:664:1: formalParameterDecls : ( ellipsisParameterDecl | normalParameterDecl ( ',' normalParameterDecl )* | ( normalParameterDecl ',' )+ ellipsisParameterDecl );
    public final void formalParameterDecls() throws RecognitionException {
        int formalParameterDecls_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "formalParameterDecls");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(664, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 39) ) { return ; }
            // src/com/google/doclava/parser/Java.g:665:5: ( ellipsisParameterDecl | normalParameterDecl ( ',' normalParameterDecl )* | ( normalParameterDecl ',' )+ ellipsisParameterDecl )
            int alt72=3;
            try { dbg.enterDecision(72, decisionCanBacktrack[72]);

            switch ( input.LA(1) ) {
            case FINAL:
                {
                int LA72_1 = input.LA(2);

                if ( (synpred96_Java()) ) {
                    alt72=1;
                }
                else if ( (synpred98_Java()) ) {
                    alt72=2;
                }
                else if ( (true) ) {
                    alt72=3;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 72, 1, input);

                    dbg.recognitionException(nvae);
                    throw nvae;
                }
                }
                break;
            case MONKEYS_AT:
                {
                int LA72_2 = input.LA(2);

                if ( (synpred96_Java()) ) {
                    alt72=1;
                }
                else if ( (synpred98_Java()) ) {
                    alt72=2;
                }
                else if ( (true) ) {
                    alt72=3;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 72, 2, input);

                    dbg.recognitionException(nvae);
                    throw nvae;
                }
                }
                break;
            case IDENTIFIER:
                {
                int LA72_3 = input.LA(2);

                if ( (synpred96_Java()) ) {
                    alt72=1;
                }
                else if ( (synpred98_Java()) ) {
                    alt72=2;
                }
                else if ( (true) ) {
                    alt72=3;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 72, 3, input);

                    dbg.recognitionException(nvae);
                    throw nvae;
                }
                }
                break;
            case BOOLEAN:
            case BYTE:
            case CHAR:
            case DOUBLE:
            case FLOAT:
            case INT:
            case LONG:
            case SHORT:
                {
                int LA72_4 = input.LA(2);

                if ( (synpred96_Java()) ) {
                    alt72=1;
                }
                else if ( (synpred98_Java()) ) {
                    alt72=2;
                }
                else if ( (true) ) {
                    alt72=3;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 72, 4, input);

                    dbg.recognitionException(nvae);
                    throw nvae;
                }
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 72, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }

            } finally {dbg.exitDecision(72);}

            switch (alt72) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:665:9: ellipsisParameterDecl
                    {
                    dbg.location(665,9);
                    pushFollow(FOLLOW_ellipsisParameterDecl_in_formalParameterDecls2953);
                    ellipsisParameterDecl();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:666:9: normalParameterDecl ( ',' normalParameterDecl )*
                    {
                    dbg.location(666,9);
                    pushFollow(FOLLOW_normalParameterDecl_in_formalParameterDecls2963);
                    normalParameterDecl();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(667,9);
                    // src/com/google/doclava/parser/Java.g:667:9: ( ',' normalParameterDecl )*
                    try { dbg.enterSubRule(70);

                    loop70:
                    do {
                        int alt70=2;
                        try { dbg.enterDecision(70, decisionCanBacktrack[70]);

                        int LA70_0 = input.LA(1);

                        if ( (LA70_0==COMMA) ) {
                            alt70=1;
                        }


                        } finally {dbg.exitDecision(70);}

                        switch (alt70) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:667:10: ',' normalParameterDecl
			    {
			    dbg.location(667,10);
			    match(input,COMMA,FOLLOW_COMMA_in_formalParameterDecls2974); if (state.failed) return ;
			    dbg.location(667,14);
			    pushFollow(FOLLOW_normalParameterDecl_in_formalParameterDecls2976);
			    normalParameterDecl();

			    state._fsp--;
			    if (state.failed) return ;

			    }
			    break;

			default :
			    break loop70;
                        }
                    } while (true);
                    } finally {dbg.exitSubRule(70);}


                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:669:9: ( normalParameterDecl ',' )+ ellipsisParameterDecl
                    {
                    dbg.location(669,9);
                    // src/com/google/doclava/parser/Java.g:669:9: ( normalParameterDecl ',' )+
                    int cnt71=0;
                    try { dbg.enterSubRule(71);

                    loop71:
                    do {
                        int alt71=2;
                        try { dbg.enterDecision(71, decisionCanBacktrack[71]);

                        switch ( input.LA(1) ) {
                        case FINAL:
                            {
                            int LA71_1 = input.LA(2);

                            if ( (synpred99_Java()) ) {
                                alt71=1;
                            }


                            }
                            break;
                        case MONKEYS_AT:
                            {
                            int LA71_2 = input.LA(2);

                            if ( (synpred99_Java()) ) {
                                alt71=1;
                            }


                            }
                            break;
                        case IDENTIFIER:
                            {
                            int LA71_3 = input.LA(2);

                            if ( (synpred99_Java()) ) {
                                alt71=1;
                            }


                            }
                            break;
                        case BOOLEAN:
                        case BYTE:
                        case CHAR:
                        case DOUBLE:
                        case FLOAT:
                        case INT:
                        case LONG:
                        case SHORT:
                            {
                            int LA71_4 = input.LA(2);

                            if ( (synpred99_Java()) ) {
                                alt71=1;
                            }


                            }
                            break;

                        }

                        } finally {dbg.exitDecision(71);}

                        switch (alt71) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:669:10: normalParameterDecl ','
			    {
			    dbg.location(669,10);
			    pushFollow(FOLLOW_normalParameterDecl_in_formalParameterDecls2998);
			    normalParameterDecl();

			    state._fsp--;
			    if (state.failed) return ;
			    dbg.location(670,9);
			    match(input,COMMA,FOLLOW_COMMA_in_formalParameterDecls3008); if (state.failed) return ;

			    }
			    break;

			default :
			    if ( cnt71 >= 1 ) break loop71;
			    if (state.backtracking>0) {state.failed=true; return ;}
                                EarlyExitException eee =
                                    new EarlyExitException(71, input);
                                dbg.recognitionException(eee);

                                throw eee;
                        }
                        cnt71++;
                    } while (true);
                    } finally {dbg.exitSubRule(71);}

                    dbg.location(672,9);
                    pushFollow(FOLLOW_ellipsisParameterDecl_in_formalParameterDecls3029);
                    ellipsisParameterDecl();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 39, formalParameterDecls_StartIndex); }
        }
        dbg.location(673, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "formalParameterDecls");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "formalParameterDecls"


    // $ANTLR start "normalParameterDecl"
    // src/com/google/doclava/parser/Java.g:675:1: normalParameterDecl : variableModifiers type IDENTIFIER ( '[' ']' )* ;
    public final void normalParameterDecl() throws RecognitionException {
        int normalParameterDecl_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "normalParameterDecl");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(675, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 40) ) { return ; }
            // src/com/google/doclava/parser/Java.g:676:5: ( variableModifiers type IDENTIFIER ( '[' ']' )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:676:9: variableModifiers type IDENTIFIER ( '[' ']' )*
            {
            dbg.location(676,9);
            pushFollow(FOLLOW_variableModifiers_in_normalParameterDecl3048);
            variableModifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(676,27);
            pushFollow(FOLLOW_type_in_normalParameterDecl3050);
            type();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(676,32);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_normalParameterDecl3052); if (state.failed) return ;
            dbg.location(677,9);
            // src/com/google/doclava/parser/Java.g:677:9: ( '[' ']' )*
            try { dbg.enterSubRule(73);

            loop73:
            do {
                int alt73=2;
                try { dbg.enterDecision(73, decisionCanBacktrack[73]);

                int LA73_0 = input.LA(1);

                if ( (LA73_0==LBRACKET) ) {
                    alt73=1;
                }


                } finally {dbg.exitDecision(73);}

                switch (alt73) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:677:10: '[' ']'
		    {
		    dbg.location(677,10);
		    match(input,LBRACKET,FOLLOW_LBRACKET_in_normalParameterDecl3063); if (state.failed) return ;
		    dbg.location(677,14);
		    match(input,RBRACKET,FOLLOW_RBRACKET_in_normalParameterDecl3065); if (state.failed) return ;

		    }
		    break;

		default :
		    break loop73;
                }
            } while (true);
            } finally {dbg.exitSubRule(73);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 40, normalParameterDecl_StartIndex); }
        }
        dbg.location(679, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "normalParameterDecl");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "normalParameterDecl"


    // $ANTLR start "ellipsisParameterDecl"
    // src/com/google/doclava/parser/Java.g:681:1: ellipsisParameterDecl : variableModifiers type '...' IDENTIFIER ;
    public final void ellipsisParameterDecl() throws RecognitionException {
        int ellipsisParameterDecl_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "ellipsisParameterDecl");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(681, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 41) ) { return ; }
            // src/com/google/doclava/parser/Java.g:682:5: ( variableModifiers type '...' IDENTIFIER )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:682:9: variableModifiers type '...' IDENTIFIER
            {
            dbg.location(682,9);
            pushFollow(FOLLOW_variableModifiers_in_ellipsisParameterDecl3095);
            variableModifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(683,9);
            pushFollow(FOLLOW_type_in_ellipsisParameterDecl3105);
            type();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(683,15);
            match(input,ELLIPSIS,FOLLOW_ELLIPSIS_in_ellipsisParameterDecl3108); if (state.failed) return ;
            dbg.location(684,9);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_ellipsisParameterDecl3118); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 41, ellipsisParameterDecl_StartIndex); }
        }
        dbg.location(685, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "ellipsisParameterDecl");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "ellipsisParameterDecl"


    // $ANTLR start "explicitConstructorInvocation"
    // src/com/google/doclava/parser/Java.g:688:1: explicitConstructorInvocation : ( ( nonWildcardTypeArguments )? ( 'this' | 'super' ) arguments ';' | primary '.' ( nonWildcardTypeArguments )? 'super' arguments ';' );
    public final void explicitConstructorInvocation() throws RecognitionException {
        int explicitConstructorInvocation_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "explicitConstructorInvocation");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(688, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 42) ) { return ; }
            // src/com/google/doclava/parser/Java.g:689:5: ( ( nonWildcardTypeArguments )? ( 'this' | 'super' ) arguments ';' | primary '.' ( nonWildcardTypeArguments )? 'super' arguments ';' )
            int alt76=2;
            try { dbg.enterDecision(76, decisionCanBacktrack[76]);

            try {
                isCyclicDecision = true;
                alt76 = dfa76.predict(input);
            }
            catch (NoViableAltException nvae) {
                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(76);}

            switch (alt76) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:689:9: ( nonWildcardTypeArguments )? ( 'this' | 'super' ) arguments ';'
                    {
                    dbg.location(689,9);
                    // src/com/google/doclava/parser/Java.g:689:9: ( nonWildcardTypeArguments )?
                    int alt74=2;
                    try { dbg.enterSubRule(74);
                    try { dbg.enterDecision(74, decisionCanBacktrack[74]);

                    int LA74_0 = input.LA(1);

                    if ( (LA74_0==LT) ) {
                        alt74=1;
                    }
                    } finally {dbg.exitDecision(74);}

                    switch (alt74) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:689:10: nonWildcardTypeArguments
                            {
                            dbg.location(689,10);
                            pushFollow(FOLLOW_nonWildcardTypeArguments_in_explicitConstructorInvocation3139);
                            nonWildcardTypeArguments();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(74);}

                    dbg.location(691,9);
                    if ( input.LA(1)==SUPER||input.LA(1)==THIS ) {
                        input.consume();
                        state.errorRecovery=false;state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        dbg.recognitionException(mse);
                        throw mse;
                    }

                    dbg.location(694,9);
                    pushFollow(FOLLOW_arguments_in_explicitConstructorInvocation3197);
                    arguments();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(694,19);
                    match(input,SEMI,FOLLOW_SEMI_in_explicitConstructorInvocation3199); if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:696:9: primary '.' ( nonWildcardTypeArguments )? 'super' arguments ';'
                    {
                    dbg.location(696,9);
                    pushFollow(FOLLOW_primary_in_explicitConstructorInvocation3210);
                    primary();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(697,9);
                    match(input,DOT,FOLLOW_DOT_in_explicitConstructorInvocation3220); if (state.failed) return ;
                    dbg.location(698,9);
                    // src/com/google/doclava/parser/Java.g:698:9: ( nonWildcardTypeArguments )?
                    int alt75=2;
                    try { dbg.enterSubRule(75);
                    try { dbg.enterDecision(75, decisionCanBacktrack[75]);

                    int LA75_0 = input.LA(1);

                    if ( (LA75_0==LT) ) {
                        alt75=1;
                    }
                    } finally {dbg.exitDecision(75);}

                    switch (alt75) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:698:10: nonWildcardTypeArguments
                            {
                            dbg.location(698,10);
                            pushFollow(FOLLOW_nonWildcardTypeArguments_in_explicitConstructorInvocation3231);
                            nonWildcardTypeArguments();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(75);}

                    dbg.location(700,9);
                    match(input,SUPER,FOLLOW_SUPER_in_explicitConstructorInvocation3252); if (state.failed) return ;
                    dbg.location(701,9);
                    pushFollow(FOLLOW_arguments_in_explicitConstructorInvocation3262);
                    arguments();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(701,19);
                    match(input,SEMI,FOLLOW_SEMI_in_explicitConstructorInvocation3264); if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 42, explicitConstructorInvocation_StartIndex); }
        }
        dbg.location(702, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "explicitConstructorInvocation");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "explicitConstructorInvocation"


    // $ANTLR start "qualifiedName"
    // src/com/google/doclava/parser/Java.g:704:1: qualifiedName : IDENTIFIER ( '.' IDENTIFIER )* ;
    public final void qualifiedName() throws RecognitionException {
        int qualifiedName_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "qualifiedName");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(704, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 43) ) { return ; }
            // src/com/google/doclava/parser/Java.g:705:5: ( IDENTIFIER ( '.' IDENTIFIER )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:705:9: IDENTIFIER ( '.' IDENTIFIER )*
            {
            dbg.location(705,9);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_qualifiedName3283); if (state.failed) return ;
            dbg.location(706,9);
            // src/com/google/doclava/parser/Java.g:706:9: ( '.' IDENTIFIER )*
            try { dbg.enterSubRule(77);

            loop77:
            do {
                int alt77=2;
                try { dbg.enterDecision(77, decisionCanBacktrack[77]);

                int LA77_0 = input.LA(1);

                if ( (LA77_0==DOT) ) {
                    alt77=1;
                }


                } finally {dbg.exitDecision(77);}

                switch (alt77) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:706:10: '.' IDENTIFIER
		    {
		    dbg.location(706,10);
		    match(input,DOT,FOLLOW_DOT_in_qualifiedName3294); if (state.failed) return ;
		    dbg.location(706,14);
		    match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_qualifiedName3296); if (state.failed) return ;

		    }
		    break;

		default :
		    break loop77;
                }
            } while (true);
            } finally {dbg.exitSubRule(77);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 43, qualifiedName_StartIndex); }
        }
        dbg.location(708, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "qualifiedName");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "qualifiedName"


    // $ANTLR start "annotations"
    // src/com/google/doclava/parser/Java.g:710:1: annotations : ( annotation )+ ;
    public final void annotations() throws RecognitionException {
        int annotations_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "annotations");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(710, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 44) ) { return ; }
            // src/com/google/doclava/parser/Java.g:711:5: ( ( annotation )+ )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:711:9: ( annotation )+
            {
            dbg.location(711,9);
            // src/com/google/doclava/parser/Java.g:711:9: ( annotation )+
            int cnt78=0;
            try { dbg.enterSubRule(78);

            loop78:
            do {
                int alt78=2;
                try { dbg.enterDecision(78, decisionCanBacktrack[78]);

                int LA78_0 = input.LA(1);

                if ( (LA78_0==MONKEYS_AT) ) {
                    alt78=1;
                }


                } finally {dbg.exitDecision(78);}

                switch (alt78) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:711:10: annotation
		    {
		    dbg.location(711,10);
		    pushFollow(FOLLOW_annotation_in_annotations3327);
		    annotation();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    if ( cnt78 >= 1 ) break loop78;
		    if (state.backtracking>0) {state.failed=true; return ;}
                        EarlyExitException eee =
                            new EarlyExitException(78, input);
                        dbg.recognitionException(eee);

                        throw eee;
                }
                cnt78++;
            } while (true);
            } finally {dbg.exitSubRule(78);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 44, annotations_StartIndex); }
        }
        dbg.location(713, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "annotations");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "annotations"


    // $ANTLR start "annotation"
    // src/com/google/doclava/parser/Java.g:715:1: annotation : '@' qualifiedName ( '(' ( elementValuePairs | elementValue )? ')' )? ;
    public final void annotation() throws RecognitionException {
        int annotation_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "annotation");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(715, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 45) ) { return ; }
            // src/com/google/doclava/parser/Java.g:720:5: ( '@' qualifiedName ( '(' ( elementValuePairs | elementValue )? ')' )? )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:720:9: '@' qualifiedName ( '(' ( elementValuePairs | elementValue )? ')' )?
            {
            dbg.location(720,9);
            match(input,MONKEYS_AT,FOLLOW_MONKEYS_AT_in_annotation3359); if (state.failed) return ;
            dbg.location(720,13);
            pushFollow(FOLLOW_qualifiedName_in_annotation3361);
            qualifiedName();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(721,9);
            // src/com/google/doclava/parser/Java.g:721:9: ( '(' ( elementValuePairs | elementValue )? ')' )?
            int alt80=2;
            try { dbg.enterSubRule(80);
            try { dbg.enterDecision(80, decisionCanBacktrack[80]);

            int LA80_0 = input.LA(1);

            if ( (LA80_0==LPAREN) ) {
                alt80=1;
            }
            } finally {dbg.exitDecision(80);}

            switch (alt80) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:721:13: '(' ( elementValuePairs | elementValue )? ')'
                    {
                    dbg.location(721,13);
                    match(input,LPAREN,FOLLOW_LPAREN_in_annotation3375); if (state.failed) return ;
                    dbg.location(722,19);
                    // src/com/google/doclava/parser/Java.g:722:19: ( elementValuePairs | elementValue )?
                    int alt79=3;
                    try { dbg.enterSubRule(79);
                    try { dbg.enterDecision(79, decisionCanBacktrack[79]);

                    int LA79_0 = input.LA(1);

                    if ( (LA79_0==IDENTIFIER) ) {
                        int LA79_1 = input.LA(2);

                        if ( (LA79_1==EQ) ) {
                            alt79=1;
                        }
                        else if ( (LA79_1==INSTANCEOF||(LA79_1>=LPAREN && LA79_1<=RPAREN)||LA79_1==LBRACKET||LA79_1==DOT||LA79_1==QUES||(LA79_1>=EQEQ && LA79_1<=PERCENT)||(LA79_1>=BANGEQ && LA79_1<=LT)) ) {
                            alt79=2;
                        }
                    }
                    else if ( ((LA79_0>=INTLITERAL && LA79_0<=NULL)||LA79_0==BOOLEAN||LA79_0==BYTE||LA79_0==CHAR||LA79_0==DOUBLE||LA79_0==FLOAT||LA79_0==INT||LA79_0==LONG||LA79_0==NEW||LA79_0==SHORT||LA79_0==SUPER||LA79_0==THIS||LA79_0==VOID||LA79_0==LPAREN||LA79_0==LBRACE||(LA79_0>=BANG && LA79_0<=TILDE)||(LA79_0>=PLUSPLUS && LA79_0<=SUB)||LA79_0==MONKEYS_AT) ) {
                        alt79=2;
                    }
                    } finally {dbg.exitDecision(79);}

                    switch (alt79) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:722:23: elementValuePairs
                            {
                            dbg.location(722,23);
                            pushFollow(FOLLOW_elementValuePairs_in_annotation3399);
                            elementValuePairs();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;
                        case 2 :
                            dbg.enterAlt(2);

                            // src/com/google/doclava/parser/Java.g:723:23: elementValue
                            {
                            dbg.location(723,23);
                            pushFollow(FOLLOW_elementValue_in_annotation3423);
                            elementValue();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(79);}

                    dbg.location(725,13);
                    match(input,RPAREN,FOLLOW_RPAREN_in_annotation3458); if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(80);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 45, annotation_StartIndex); }
        }
        dbg.location(727, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "annotation");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "annotation"


    // $ANTLR start "elementValuePairs"
    // src/com/google/doclava/parser/Java.g:729:1: elementValuePairs : elementValuePair ( ',' elementValuePair )* ;
    public final void elementValuePairs() throws RecognitionException {
        int elementValuePairs_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "elementValuePairs");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(729, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 46) ) { return ; }
            // src/com/google/doclava/parser/Java.g:730:5: ( elementValuePair ( ',' elementValuePair )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:730:9: elementValuePair ( ',' elementValuePair )*
            {
            dbg.location(730,9);
            pushFollow(FOLLOW_elementValuePair_in_elementValuePairs3488);
            elementValuePair();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(731,9);
            // src/com/google/doclava/parser/Java.g:731:9: ( ',' elementValuePair )*
            try { dbg.enterSubRule(81);

            loop81:
            do {
                int alt81=2;
                try { dbg.enterDecision(81, decisionCanBacktrack[81]);

                int LA81_0 = input.LA(1);

                if ( (LA81_0==COMMA) ) {
                    alt81=1;
                }


                } finally {dbg.exitDecision(81);}

                switch (alt81) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:731:10: ',' elementValuePair
		    {
		    dbg.location(731,10);
		    match(input,COMMA,FOLLOW_COMMA_in_elementValuePairs3499); if (state.failed) return ;
		    dbg.location(731,14);
		    pushFollow(FOLLOW_elementValuePair_in_elementValuePairs3501);
		    elementValuePair();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop81;
                }
            } while (true);
            } finally {dbg.exitSubRule(81);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 46, elementValuePairs_StartIndex); }
        }
        dbg.location(733, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "elementValuePairs");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "elementValuePairs"


    // $ANTLR start "elementValuePair"
    // src/com/google/doclava/parser/Java.g:735:1: elementValuePair : IDENTIFIER '=' elementValue ;
    public final void elementValuePair() throws RecognitionException {
        int elementValuePair_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "elementValuePair");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(735, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 47) ) { return ; }
            // src/com/google/doclava/parser/Java.g:736:5: ( IDENTIFIER '=' elementValue )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:736:9: IDENTIFIER '=' elementValue
            {
            dbg.location(736,9);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_elementValuePair3531); if (state.failed) return ;
            dbg.location(736,20);
            match(input,EQ,FOLLOW_EQ_in_elementValuePair3533); if (state.failed) return ;
            dbg.location(736,24);
            pushFollow(FOLLOW_elementValue_in_elementValuePair3535);
            elementValue();

            state._fsp--;
            if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 47, elementValuePair_StartIndex); }
        }
        dbg.location(737, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "elementValuePair");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "elementValuePair"


    // $ANTLR start "elementValue"
    // src/com/google/doclava/parser/Java.g:739:1: elementValue : ( conditionalExpression | annotation | elementValueArrayInitializer );
    public final void elementValue() throws RecognitionException {
        int elementValue_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "elementValue");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(739, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 48) ) { return ; }
            // src/com/google/doclava/parser/Java.g:740:5: ( conditionalExpression | annotation | elementValueArrayInitializer )
            int alt82=3;
            try { dbg.enterDecision(82, decisionCanBacktrack[82]);

            switch ( input.LA(1) ) {
            case IDENTIFIER:
            case INTLITERAL:
            case LONGLITERAL:
            case FLOATLITERAL:
            case DOUBLELITERAL:
            case CHARLITERAL:
            case STRINGLITERAL:
            case TRUE:
            case FALSE:
            case NULL:
            case BOOLEAN:
            case BYTE:
            case CHAR:
            case DOUBLE:
            case FLOAT:
            case INT:
            case LONG:
            case NEW:
            case SHORT:
            case SUPER:
            case THIS:
            case VOID:
            case LPAREN:
            case BANG:
            case TILDE:
            case PLUSPLUS:
            case SUBSUB:
            case PLUS:
            case SUB:
                {
                alt82=1;
                }
                break;
            case MONKEYS_AT:
                {
                alt82=2;
                }
                break;
            case LBRACE:
                {
                alt82=3;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 82, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }

            } finally {dbg.exitDecision(82);}

            switch (alt82) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:740:9: conditionalExpression
                    {
                    dbg.location(740,9);
                    pushFollow(FOLLOW_conditionalExpression_in_elementValue3554);
                    conditionalExpression();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:741:9: annotation
                    {
                    dbg.location(741,9);
                    pushFollow(FOLLOW_annotation_in_elementValue3564);
                    annotation();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:742:9: elementValueArrayInitializer
                    {
                    dbg.location(742,9);
                    pushFollow(FOLLOW_elementValueArrayInitializer_in_elementValue3574);
                    elementValueArrayInitializer();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 48, elementValue_StartIndex); }
        }
        dbg.location(743, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "elementValue");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "elementValue"


    // $ANTLR start "elementValueArrayInitializer"
    // src/com/google/doclava/parser/Java.g:745:1: elementValueArrayInitializer : '{' ( elementValue ( ',' elementValue )* )? ( ',' )? '}' ;
    public final void elementValueArrayInitializer() throws RecognitionException {
        int elementValueArrayInitializer_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "elementValueArrayInitializer");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(745, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 49) ) { return ; }
            // src/com/google/doclava/parser/Java.g:746:5: ( '{' ( elementValue ( ',' elementValue )* )? ( ',' )? '}' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:746:9: '{' ( elementValue ( ',' elementValue )* )? ( ',' )? '}'
            {
            dbg.location(746,9);
            match(input,LBRACE,FOLLOW_LBRACE_in_elementValueArrayInitializer3593); if (state.failed) return ;
            dbg.location(747,9);
            // src/com/google/doclava/parser/Java.g:747:9: ( elementValue ( ',' elementValue )* )?
            int alt84=2;
            try { dbg.enterSubRule(84);
            try { dbg.enterDecision(84, decisionCanBacktrack[84]);

            int LA84_0 = input.LA(1);

            if ( ((LA84_0>=IDENTIFIER && LA84_0<=NULL)||LA84_0==BOOLEAN||LA84_0==BYTE||LA84_0==CHAR||LA84_0==DOUBLE||LA84_0==FLOAT||LA84_0==INT||LA84_0==LONG||LA84_0==NEW||LA84_0==SHORT||LA84_0==SUPER||LA84_0==THIS||LA84_0==VOID||LA84_0==LPAREN||LA84_0==LBRACE||(LA84_0>=BANG && LA84_0<=TILDE)||(LA84_0>=PLUSPLUS && LA84_0<=SUB)||LA84_0==MONKEYS_AT) ) {
                alt84=1;
            }
            } finally {dbg.exitDecision(84);}

            switch (alt84) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:747:10: elementValue ( ',' elementValue )*
                    {
                    dbg.location(747,10);
                    pushFollow(FOLLOW_elementValue_in_elementValueArrayInitializer3604);
                    elementValue();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(748,13);
                    // src/com/google/doclava/parser/Java.g:748:13: ( ',' elementValue )*
                    try { dbg.enterSubRule(83);

                    loop83:
                    do {
                        int alt83=2;
                        try { dbg.enterDecision(83, decisionCanBacktrack[83]);

                        int LA83_0 = input.LA(1);

                        if ( (LA83_0==COMMA) ) {
                            int LA83_1 = input.LA(2);

                            if ( ((LA83_1>=IDENTIFIER && LA83_1<=NULL)||LA83_1==BOOLEAN||LA83_1==BYTE||LA83_1==CHAR||LA83_1==DOUBLE||LA83_1==FLOAT||LA83_1==INT||LA83_1==LONG||LA83_1==NEW||LA83_1==SHORT||LA83_1==SUPER||LA83_1==THIS||LA83_1==VOID||LA83_1==LPAREN||LA83_1==LBRACE||(LA83_1>=BANG && LA83_1<=TILDE)||(LA83_1>=PLUSPLUS && LA83_1<=SUB)||LA83_1==MONKEYS_AT) ) {
                                alt83=1;
                            }


                        }


                        } finally {dbg.exitDecision(83);}

                        switch (alt83) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:748:14: ',' elementValue
			    {
			    dbg.location(748,14);
			    match(input,COMMA,FOLLOW_COMMA_in_elementValueArrayInitializer3619); if (state.failed) return ;
			    dbg.location(748,18);
			    pushFollow(FOLLOW_elementValue_in_elementValueArrayInitializer3621);
			    elementValue();

			    state._fsp--;
			    if (state.failed) return ;

			    }
			    break;

			default :
			    break loop83;
                        }
                    } while (true);
                    } finally {dbg.exitSubRule(83);}


                    }
                    break;

            }
            } finally {dbg.exitSubRule(84);}

            dbg.location(750,12);
            // src/com/google/doclava/parser/Java.g:750:12: ( ',' )?
            int alt85=2;
            try { dbg.enterSubRule(85);
            try { dbg.enterDecision(85, decisionCanBacktrack[85]);

            int LA85_0 = input.LA(1);

            if ( (LA85_0==COMMA) ) {
                alt85=1;
            }
            } finally {dbg.exitDecision(85);}

            switch (alt85) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:750:13: ','
                    {
                    dbg.location(750,13);
                    match(input,COMMA,FOLLOW_COMMA_in_elementValueArrayInitializer3650); if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(85);}

            dbg.location(750,19);
            match(input,RBRACE,FOLLOW_RBRACE_in_elementValueArrayInitializer3654); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 49, elementValueArrayInitializer_StartIndex); }
        }
        dbg.location(751, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "elementValueArrayInitializer");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "elementValueArrayInitializer"


    // $ANTLR start "annotationTypeDeclaration"
    // src/com/google/doclava/parser/Java.g:754:1: annotationTypeDeclaration : modifiers '@' 'interface' IDENTIFIER annotationTypeBody ;
    public final void annotationTypeDeclaration() throws RecognitionException {
        int annotationTypeDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "annotationTypeDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(754, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 50) ) { return ; }
            // src/com/google/doclava/parser/Java.g:758:5: ( modifiers '@' 'interface' IDENTIFIER annotationTypeBody )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:758:9: modifiers '@' 'interface' IDENTIFIER annotationTypeBody
            {
            dbg.location(758,9);
            pushFollow(FOLLOW_modifiers_in_annotationTypeDeclaration3676);
            modifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(758,19);
            match(input,MONKEYS_AT,FOLLOW_MONKEYS_AT_in_annotationTypeDeclaration3678); if (state.failed) return ;
            dbg.location(759,9);
            match(input,INTERFACE,FOLLOW_INTERFACE_in_annotationTypeDeclaration3688); if (state.failed) return ;
            dbg.location(760,9);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_annotationTypeDeclaration3698); if (state.failed) return ;
            dbg.location(761,9);
            pushFollow(FOLLOW_annotationTypeBody_in_annotationTypeDeclaration3708);
            annotationTypeBody();

            state._fsp--;
            if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 50, annotationTypeDeclaration_StartIndex); }
        }
        dbg.location(762, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "annotationTypeDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "annotationTypeDeclaration"


    // $ANTLR start "annotationTypeBody"
    // src/com/google/doclava/parser/Java.g:765:1: annotationTypeBody : '{' ( annotationTypeElementDeclaration )* '}' ;
    public final void annotationTypeBody() throws RecognitionException {
        int annotationTypeBody_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "annotationTypeBody");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(765, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 51) ) { return ; }
            // src/com/google/doclava/parser/Java.g:766:5: ( '{' ( annotationTypeElementDeclaration )* '}' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:766:9: '{' ( annotationTypeElementDeclaration )* '}'
            {
            dbg.location(766,9);
            match(input,LBRACE,FOLLOW_LBRACE_in_annotationTypeBody3728); if (state.failed) return ;
            dbg.location(767,9);
            // src/com/google/doclava/parser/Java.g:767:9: ( annotationTypeElementDeclaration )*
            try { dbg.enterSubRule(86);

            loop86:
            do {
                int alt86=2;
                try { dbg.enterDecision(86, decisionCanBacktrack[86]);

                int LA86_0 = input.LA(1);

                if ( (LA86_0==IDENTIFIER||LA86_0==ABSTRACT||LA86_0==BOOLEAN||LA86_0==BYTE||(LA86_0>=CHAR && LA86_0<=CLASS)||LA86_0==DOUBLE||LA86_0==ENUM||LA86_0==FINAL||LA86_0==FLOAT||(LA86_0>=INT && LA86_0<=NATIVE)||(LA86_0>=PRIVATE && LA86_0<=PUBLIC)||(LA86_0>=SHORT && LA86_0<=STRICTFP)||LA86_0==SYNCHRONIZED||LA86_0==TRANSIENT||(LA86_0>=VOID && LA86_0<=VOLATILE)||LA86_0==SEMI||LA86_0==MONKEYS_AT||LA86_0==LT) ) {
                    alt86=1;
                }


                } finally {dbg.exitDecision(86);}

                switch (alt86) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:767:10: annotationTypeElementDeclaration
		    {
		    dbg.location(767,10);
		    pushFollow(FOLLOW_annotationTypeElementDeclaration_in_annotationTypeBody3739);
		    annotationTypeElementDeclaration();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop86;
                }
            } while (true);
            } finally {dbg.exitSubRule(86);}

            dbg.location(769,9);
            match(input,RBRACE,FOLLOW_RBRACE_in_annotationTypeBody3760); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 51, annotationTypeBody_StartIndex); }
        }
        dbg.location(770, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "annotationTypeBody");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "annotationTypeBody"


    // $ANTLR start "annotationTypeElementDeclaration"
    // src/com/google/doclava/parser/Java.g:772:1: annotationTypeElementDeclaration : ( annotationMethodDeclaration | interfaceFieldDeclaration | normalClassDeclaration | normalInterfaceDeclaration | enumDeclaration | annotationTypeDeclaration | ';' );
    public final void annotationTypeElementDeclaration() throws RecognitionException {
        int annotationTypeElementDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "annotationTypeElementDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(772, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 52) ) { return ; }
            // src/com/google/doclava/parser/Java.g:776:5: ( annotationMethodDeclaration | interfaceFieldDeclaration | normalClassDeclaration | normalInterfaceDeclaration | enumDeclaration | annotationTypeDeclaration | ';' )
            int alt87=7;
            try { dbg.enterDecision(87, decisionCanBacktrack[87]);

            try {
                isCyclicDecision = true;
                alt87 = dfa87.predict(input);
            }
            catch (NoViableAltException nvae) {
                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(87);}

            switch (alt87) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:776:9: annotationMethodDeclaration
                    {
                    dbg.location(776,9);
                    pushFollow(FOLLOW_annotationMethodDeclaration_in_annotationTypeElementDeclaration3781);
                    annotationMethodDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:777:9: interfaceFieldDeclaration
                    {
                    dbg.location(777,9);
                    pushFollow(FOLLOW_interfaceFieldDeclaration_in_annotationTypeElementDeclaration3791);
                    interfaceFieldDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:778:9: normalClassDeclaration
                    {
                    dbg.location(778,9);
                    pushFollow(FOLLOW_normalClassDeclaration_in_annotationTypeElementDeclaration3801);
                    normalClassDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 4 :
                    dbg.enterAlt(4);

                    // src/com/google/doclava/parser/Java.g:779:9: normalInterfaceDeclaration
                    {
                    dbg.location(779,9);
                    pushFollow(FOLLOW_normalInterfaceDeclaration_in_annotationTypeElementDeclaration3811);
                    normalInterfaceDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 5 :
                    dbg.enterAlt(5);

                    // src/com/google/doclava/parser/Java.g:780:9: enumDeclaration
                    {
                    dbg.location(780,9);
                    pushFollow(FOLLOW_enumDeclaration_in_annotationTypeElementDeclaration3821);
                    enumDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 6 :
                    dbg.enterAlt(6);

                    // src/com/google/doclava/parser/Java.g:781:9: annotationTypeDeclaration
                    {
                    dbg.location(781,9);
                    pushFollow(FOLLOW_annotationTypeDeclaration_in_annotationTypeElementDeclaration3831);
                    annotationTypeDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 7 :
                    dbg.enterAlt(7);

                    // src/com/google/doclava/parser/Java.g:782:9: ';'
                    {
                    dbg.location(782,9);
                    match(input,SEMI,FOLLOW_SEMI_in_annotationTypeElementDeclaration3841); if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 52, annotationTypeElementDeclaration_StartIndex); }
        }
        dbg.location(783, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "annotationTypeElementDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "annotationTypeElementDeclaration"


    // $ANTLR start "annotationMethodDeclaration"
    // src/com/google/doclava/parser/Java.g:785:1: annotationMethodDeclaration : modifiers type IDENTIFIER '(' ')' ( 'default' elementValue )? ';' ;
    public final void annotationMethodDeclaration() throws RecognitionException {
        int annotationMethodDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "annotationMethodDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(785, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 53) ) { return ; }
            // src/com/google/doclava/parser/Java.g:786:5: ( modifiers type IDENTIFIER '(' ')' ( 'default' elementValue )? ';' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:786:9: modifiers type IDENTIFIER '(' ')' ( 'default' elementValue )? ';'
            {
            dbg.location(786,9);
            pushFollow(FOLLOW_modifiers_in_annotationMethodDeclaration3860);
            modifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(786,19);
            pushFollow(FOLLOW_type_in_annotationMethodDeclaration3862);
            type();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(786,24);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_annotationMethodDeclaration3864); if (state.failed) return ;
            dbg.location(787,9);
            match(input,LPAREN,FOLLOW_LPAREN_in_annotationMethodDeclaration3874); if (state.failed) return ;
            dbg.location(787,13);
            match(input,RPAREN,FOLLOW_RPAREN_in_annotationMethodDeclaration3876); if (state.failed) return ;
            dbg.location(787,17);
            // src/com/google/doclava/parser/Java.g:787:17: ( 'default' elementValue )?
            int alt88=2;
            try { dbg.enterSubRule(88);
            try { dbg.enterDecision(88, decisionCanBacktrack[88]);

            int LA88_0 = input.LA(1);

            if ( (LA88_0==DEFAULT) ) {
                alt88=1;
            }
            } finally {dbg.exitDecision(88);}

            switch (alt88) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:787:18: 'default' elementValue
                    {
                    dbg.location(787,18);
                    match(input,DEFAULT,FOLLOW_DEFAULT_in_annotationMethodDeclaration3879); if (state.failed) return ;
                    dbg.location(787,28);
                    pushFollow(FOLLOW_elementValue_in_annotationMethodDeclaration3881);
                    elementValue();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(88);}

            dbg.location(789,9);
            match(input,SEMI,FOLLOW_SEMI_in_annotationMethodDeclaration3910); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 53, annotationMethodDeclaration_StartIndex); }
        }
        dbg.location(790, 9);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "annotationMethodDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "annotationMethodDeclaration"


    // $ANTLR start "block"
    // src/com/google/doclava/parser/Java.g:792:1: block : '{' ( blockStatement )* '}' ;
    public final void block() throws RecognitionException {
        int block_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "block");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(792, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 54) ) { return ; }
            // src/com/google/doclava/parser/Java.g:793:5: ( '{' ( blockStatement )* '}' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:793:9: '{' ( blockStatement )* '}'
            {
            dbg.location(793,9);
            match(input,LBRACE,FOLLOW_LBRACE_in_block3933); if (state.failed) return ;
            dbg.location(794,9);
            // src/com/google/doclava/parser/Java.g:794:9: ( blockStatement )*
            try { dbg.enterSubRule(89);

            loop89:
            do {
                int alt89=2;
                try { dbg.enterDecision(89, decisionCanBacktrack[89]);

                int LA89_0 = input.LA(1);

                if ( ((LA89_0>=IDENTIFIER && LA89_0<=NULL)||(LA89_0>=ABSTRACT && LA89_0<=BYTE)||(LA89_0>=CHAR && LA89_0<=CLASS)||LA89_0==CONTINUE||(LA89_0>=DO && LA89_0<=DOUBLE)||LA89_0==ENUM||LA89_0==FINAL||(LA89_0>=FLOAT && LA89_0<=FOR)||LA89_0==IF||(LA89_0>=INT && LA89_0<=NEW)||(LA89_0>=PRIVATE && LA89_0<=THROW)||(LA89_0>=TRANSIENT && LA89_0<=LPAREN)||LA89_0==LBRACE||LA89_0==SEMI||(LA89_0>=BANG && LA89_0<=TILDE)||(LA89_0>=PLUSPLUS && LA89_0<=SUB)||LA89_0==MONKEYS_AT||LA89_0==LT) ) {
                    alt89=1;
                }


                } finally {dbg.exitDecision(89);}

                switch (alt89) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:794:10: blockStatement
		    {
		    dbg.location(794,10);
		    pushFollow(FOLLOW_blockStatement_in_block3944);
		    blockStatement();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop89;
                }
            } while (true);
            } finally {dbg.exitSubRule(89);}

            dbg.location(796,9);
            match(input,RBRACE,FOLLOW_RBRACE_in_block3965); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 54, block_StartIndex); }
        }
        dbg.location(797, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "block");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "block"


    // $ANTLR start "blockStatement"
    // src/com/google/doclava/parser/Java.g:823:1: blockStatement : ( localVariableDeclarationStatement | classOrInterfaceDeclaration | statement );
    public final void blockStatement() throws RecognitionException {
        int blockStatement_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "blockStatement");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(823, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 55) ) { return ; }
            // src/com/google/doclava/parser/Java.g:824:5: ( localVariableDeclarationStatement | classOrInterfaceDeclaration | statement )
            int alt90=3;
            try { dbg.enterDecision(90, decisionCanBacktrack[90]);

            try {
                isCyclicDecision = true;
                alt90 = dfa90.predict(input);
            }
            catch (NoViableAltException nvae) {
                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(90);}

            switch (alt90) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:824:9: localVariableDeclarationStatement
                    {
                    dbg.location(824,9);
                    pushFollow(FOLLOW_localVariableDeclarationStatement_in_blockStatement3986);
                    localVariableDeclarationStatement();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:825:9: classOrInterfaceDeclaration
                    {
                    dbg.location(825,9);
                    pushFollow(FOLLOW_classOrInterfaceDeclaration_in_blockStatement3996);
                    classOrInterfaceDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:826:9: statement
                    {
                    dbg.location(826,9);
                    pushFollow(FOLLOW_statement_in_blockStatement4006);
                    statement();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 55, blockStatement_StartIndex); }
        }
        dbg.location(827, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "blockStatement");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "blockStatement"


    // $ANTLR start "localVariableDeclarationStatement"
    // src/com/google/doclava/parser/Java.g:830:1: localVariableDeclarationStatement : localVariableDeclaration ';' ;
    public final void localVariableDeclarationStatement() throws RecognitionException {
        int localVariableDeclarationStatement_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "localVariableDeclarationStatement");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(830, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 56) ) { return ; }
            // src/com/google/doclava/parser/Java.g:831:5: ( localVariableDeclaration ';' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:831:9: localVariableDeclaration ';'
            {
            dbg.location(831,9);
            pushFollow(FOLLOW_localVariableDeclaration_in_localVariableDeclarationStatement4026);
            localVariableDeclaration();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(832,9);
            match(input,SEMI,FOLLOW_SEMI_in_localVariableDeclarationStatement4036); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 56, localVariableDeclarationStatement_StartIndex); }
        }
        dbg.location(833, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "localVariableDeclarationStatement");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "localVariableDeclarationStatement"


    // $ANTLR start "localVariableDeclaration"
    // src/com/google/doclava/parser/Java.g:835:1: localVariableDeclaration : variableModifiers type variableDeclarator ( ',' variableDeclarator )* ;
    public final void localVariableDeclaration() throws RecognitionException {
        int localVariableDeclaration_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "localVariableDeclaration");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(835, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 57) ) { return ; }
            // src/com/google/doclava/parser/Java.g:836:5: ( variableModifiers type variableDeclarator ( ',' variableDeclarator )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:836:9: variableModifiers type variableDeclarator ( ',' variableDeclarator )*
            {
            dbg.location(836,9);
            pushFollow(FOLLOW_variableModifiers_in_localVariableDeclaration4055);
            variableModifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(836,27);
            pushFollow(FOLLOW_type_in_localVariableDeclaration4057);
            type();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(837,9);
            pushFollow(FOLLOW_variableDeclarator_in_localVariableDeclaration4067);
            variableDeclarator();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(838,9);
            // src/com/google/doclava/parser/Java.g:838:9: ( ',' variableDeclarator )*
            try { dbg.enterSubRule(91);

            loop91:
            do {
                int alt91=2;
                try { dbg.enterDecision(91, decisionCanBacktrack[91]);

                int LA91_0 = input.LA(1);

                if ( (LA91_0==COMMA) ) {
                    alt91=1;
                }


                } finally {dbg.exitDecision(91);}

                switch (alt91) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:838:10: ',' variableDeclarator
		    {
		    dbg.location(838,10);
		    match(input,COMMA,FOLLOW_COMMA_in_localVariableDeclaration4078); if (state.failed) return ;
		    dbg.location(838,14);
		    pushFollow(FOLLOW_variableDeclarator_in_localVariableDeclaration4080);
		    variableDeclarator();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop91;
                }
            } while (true);
            } finally {dbg.exitSubRule(91);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 57, localVariableDeclaration_StartIndex); }
        }
        dbg.location(840, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "localVariableDeclaration");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "localVariableDeclaration"


    // $ANTLR start "statement"
    // src/com/google/doclava/parser/Java.g:842:1: statement : ( block | ( 'assert' ) expression ( ':' expression )? ';' | 'assert' expression ( ':' expression )? ';' | 'if' parExpression statement ( 'else' statement )? | forstatement | 'while' parExpression statement | 'do' statement 'while' parExpression ';' | trystatement | 'switch' parExpression '{' switchBlockStatementGroups '}' | 'synchronized' parExpression block | 'return' ( expression )? ';' | 'throw' expression ';' | 'break' ( IDENTIFIER )? ';' | 'continue' ( IDENTIFIER )? ';' | expression ';' | IDENTIFIER ':' statement | ';' );
    public final void statement() throws RecognitionException {
        int statement_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "statement");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(842, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 58) ) { return ; }
            // src/com/google/doclava/parser/Java.g:843:5: ( block | ( 'assert' ) expression ( ':' expression )? ';' | 'assert' expression ( ':' expression )? ';' | 'if' parExpression statement ( 'else' statement )? | forstatement | 'while' parExpression statement | 'do' statement 'while' parExpression ';' | trystatement | 'switch' parExpression '{' switchBlockStatementGroups '}' | 'synchronized' parExpression block | 'return' ( expression )? ';' | 'throw' expression ';' | 'break' ( IDENTIFIER )? ';' | 'continue' ( IDENTIFIER )? ';' | expression ';' | IDENTIFIER ':' statement | ';' )
            int alt98=17;
            try { dbg.enterDecision(98, decisionCanBacktrack[98]);

            try {
                isCyclicDecision = true;
                alt98 = dfa98.predict(input);
            }
            catch (NoViableAltException nvae) {
                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(98);}

            switch (alt98) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:843:9: block
                    {
                    dbg.location(843,9);
                    pushFollow(FOLLOW_block_in_statement4110);
                    block();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:845:9: ( 'assert' ) expression ( ':' expression )? ';'
                    {
                    dbg.location(845,9);
                    // src/com/google/doclava/parser/Java.g:845:9: ( 'assert' )
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:845:10: 'assert'
                    {
                    dbg.location(845,10);
                    match(input,ASSERT,FOLLOW_ASSERT_in_statement4122); if (state.failed) return ;

                    }

                    dbg.location(847,9);
                    pushFollow(FOLLOW_expression_in_statement4142);
                    expression();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(847,20);
                    // src/com/google/doclava/parser/Java.g:847:20: ( ':' expression )?
                    int alt92=2;
                    try { dbg.enterSubRule(92);
                    try { dbg.enterDecision(92, decisionCanBacktrack[92]);

                    int LA92_0 = input.LA(1);

                    if ( (LA92_0==COLON) ) {
                        alt92=1;
                    }
                    } finally {dbg.exitDecision(92);}

                    switch (alt92) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:847:21: ':' expression
                            {
                            dbg.location(847,21);
                            match(input,COLON,FOLLOW_COLON_in_statement4145); if (state.failed) return ;
                            dbg.location(847,25);
                            pushFollow(FOLLOW_expression_in_statement4147);
                            expression();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(92);}

                    dbg.location(847,38);
                    match(input,SEMI,FOLLOW_SEMI_in_statement4151); if (state.failed) return ;

                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:848:9: 'assert' expression ( ':' expression )? ';'
                    {
                    dbg.location(848,9);
                    match(input,ASSERT,FOLLOW_ASSERT_in_statement4161); if (state.failed) return ;
                    dbg.location(848,19);
                    pushFollow(FOLLOW_expression_in_statement4164);
                    expression();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(848,30);
                    // src/com/google/doclava/parser/Java.g:848:30: ( ':' expression )?
                    int alt93=2;
                    try { dbg.enterSubRule(93);
                    try { dbg.enterDecision(93, decisionCanBacktrack[93]);

                    int LA93_0 = input.LA(1);

                    if ( (LA93_0==COLON) ) {
                        alt93=1;
                    }
                    } finally {dbg.exitDecision(93);}

                    switch (alt93) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:848:31: ':' expression
                            {
                            dbg.location(848,31);
                            match(input,COLON,FOLLOW_COLON_in_statement4167); if (state.failed) return ;
                            dbg.location(848,35);
                            pushFollow(FOLLOW_expression_in_statement4169);
                            expression();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(93);}

                    dbg.location(848,48);
                    match(input,SEMI,FOLLOW_SEMI_in_statement4173); if (state.failed) return ;

                    }
                    break;
                case 4 :
                    dbg.enterAlt(4);

                    // src/com/google/doclava/parser/Java.g:849:9: 'if' parExpression statement ( 'else' statement )?
                    {
                    dbg.location(849,9);
                    match(input,IF,FOLLOW_IF_in_statement4183); if (state.failed) return ;
                    dbg.location(849,14);
                    pushFollow(FOLLOW_parExpression_in_statement4185);
                    parExpression();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(849,28);
                    pushFollow(FOLLOW_statement_in_statement4187);
                    statement();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(849,38);
                    // src/com/google/doclava/parser/Java.g:849:38: ( 'else' statement )?
                    int alt94=2;
                    try { dbg.enterSubRule(94);
                    try { dbg.enterDecision(94, decisionCanBacktrack[94]);

                    int LA94_0 = input.LA(1);

                    if ( (LA94_0==ELSE) ) {
                        int LA94_1 = input.LA(2);

                        if ( (synpred133_Java()) ) {
                            alt94=1;
                        }
                    }
                    } finally {dbg.exitDecision(94);}

                    switch (alt94) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:849:39: 'else' statement
                            {
                            dbg.location(849,39);
                            match(input,ELSE,FOLLOW_ELSE_in_statement4190); if (state.failed) return ;
                            dbg.location(849,46);
                            pushFollow(FOLLOW_statement_in_statement4192);
                            statement();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(94);}


                    }
                    break;
                case 5 :
                    dbg.enterAlt(5);

                    // src/com/google/doclava/parser/Java.g:850:9: forstatement
                    {
                    dbg.location(850,9);
                    pushFollow(FOLLOW_forstatement_in_statement4204);
                    forstatement();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 6 :
                    dbg.enterAlt(6);

                    // src/com/google/doclava/parser/Java.g:851:9: 'while' parExpression statement
                    {
                    dbg.location(851,9);
                    match(input,WHILE,FOLLOW_WHILE_in_statement4214); if (state.failed) return ;
                    dbg.location(851,17);
                    pushFollow(FOLLOW_parExpression_in_statement4216);
                    parExpression();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(851,31);
                    pushFollow(FOLLOW_statement_in_statement4218);
                    statement();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 7 :
                    dbg.enterAlt(7);

                    // src/com/google/doclava/parser/Java.g:852:9: 'do' statement 'while' parExpression ';'
                    {
                    dbg.location(852,9);
                    match(input,DO,FOLLOW_DO_in_statement4228); if (state.failed) return ;
                    dbg.location(852,14);
                    pushFollow(FOLLOW_statement_in_statement4230);
                    statement();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(852,24);
                    match(input,WHILE,FOLLOW_WHILE_in_statement4232); if (state.failed) return ;
                    dbg.location(852,32);
                    pushFollow(FOLLOW_parExpression_in_statement4234);
                    parExpression();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(852,46);
                    match(input,SEMI,FOLLOW_SEMI_in_statement4236); if (state.failed) return ;

                    }
                    break;
                case 8 :
                    dbg.enterAlt(8);

                    // src/com/google/doclava/parser/Java.g:853:9: trystatement
                    {
                    dbg.location(853,9);
                    pushFollow(FOLLOW_trystatement_in_statement4246);
                    trystatement();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 9 :
                    dbg.enterAlt(9);

                    // src/com/google/doclava/parser/Java.g:854:9: 'switch' parExpression '{' switchBlockStatementGroups '}'
                    {
                    dbg.location(854,9);
                    match(input,SWITCH,FOLLOW_SWITCH_in_statement4256); if (state.failed) return ;
                    dbg.location(854,18);
                    pushFollow(FOLLOW_parExpression_in_statement4258);
                    parExpression();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(854,32);
                    match(input,LBRACE,FOLLOW_LBRACE_in_statement4260); if (state.failed) return ;
                    dbg.location(854,36);
                    pushFollow(FOLLOW_switchBlockStatementGroups_in_statement4262);
                    switchBlockStatementGroups();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(854,63);
                    match(input,RBRACE,FOLLOW_RBRACE_in_statement4264); if (state.failed) return ;

                    }
                    break;
                case 10 :
                    dbg.enterAlt(10);

                    // src/com/google/doclava/parser/Java.g:855:9: 'synchronized' parExpression block
                    {
                    dbg.location(855,9);
                    match(input,SYNCHRONIZED,FOLLOW_SYNCHRONIZED_in_statement4274); if (state.failed) return ;
                    dbg.location(855,24);
                    pushFollow(FOLLOW_parExpression_in_statement4276);
                    parExpression();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(855,38);
                    pushFollow(FOLLOW_block_in_statement4278);
                    block();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 11 :
                    dbg.enterAlt(11);

                    // src/com/google/doclava/parser/Java.g:856:9: 'return' ( expression )? ';'
                    {
                    dbg.location(856,9);
                    match(input,RETURN,FOLLOW_RETURN_in_statement4288); if (state.failed) return ;
                    dbg.location(856,18);
                    // src/com/google/doclava/parser/Java.g:856:18: ( expression )?
                    int alt95=2;
                    try { dbg.enterSubRule(95);
                    try { dbg.enterDecision(95, decisionCanBacktrack[95]);

                    int LA95_0 = input.LA(1);

                    if ( ((LA95_0>=IDENTIFIER && LA95_0<=NULL)||LA95_0==BOOLEAN||LA95_0==BYTE||LA95_0==CHAR||LA95_0==DOUBLE||LA95_0==FLOAT||LA95_0==INT||LA95_0==LONG||LA95_0==NEW||LA95_0==SHORT||LA95_0==SUPER||LA95_0==THIS||LA95_0==VOID||LA95_0==LPAREN||(LA95_0>=BANG && LA95_0<=TILDE)||(LA95_0>=PLUSPLUS && LA95_0<=SUB)) ) {
                        alt95=1;
                    }
                    } finally {dbg.exitDecision(95);}

                    switch (alt95) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:856:19: expression
                            {
                            dbg.location(856,19);
                            pushFollow(FOLLOW_expression_in_statement4291);
                            expression();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(95);}

                    dbg.location(856,33);
                    match(input,SEMI,FOLLOW_SEMI_in_statement4296); if (state.failed) return ;

                    }
                    break;
                case 12 :
                    dbg.enterAlt(12);

                    // src/com/google/doclava/parser/Java.g:857:9: 'throw' expression ';'
                    {
                    dbg.location(857,9);
                    match(input,THROW,FOLLOW_THROW_in_statement4306); if (state.failed) return ;
                    dbg.location(857,17);
                    pushFollow(FOLLOW_expression_in_statement4308);
                    expression();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(857,28);
                    match(input,SEMI,FOLLOW_SEMI_in_statement4310); if (state.failed) return ;

                    }
                    break;
                case 13 :
                    dbg.enterAlt(13);

                    // src/com/google/doclava/parser/Java.g:858:9: 'break' ( IDENTIFIER )? ';'
                    {
                    dbg.location(858,9);
                    match(input,BREAK,FOLLOW_BREAK_in_statement4320); if (state.failed) return ;
                    dbg.location(859,13);
                    // src/com/google/doclava/parser/Java.g:859:13: ( IDENTIFIER )?
                    int alt96=2;
                    try { dbg.enterSubRule(96);
                    try { dbg.enterDecision(96, decisionCanBacktrack[96]);

                    int LA96_0 = input.LA(1);

                    if ( (LA96_0==IDENTIFIER) ) {
                        alt96=1;
                    }
                    } finally {dbg.exitDecision(96);}

                    switch (alt96) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:859:14: IDENTIFIER
                            {
                            dbg.location(859,14);
                            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_statement4335); if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(96);}

                    dbg.location(860,16);
                    match(input,SEMI,FOLLOW_SEMI_in_statement4352); if (state.failed) return ;

                    }
                    break;
                case 14 :
                    dbg.enterAlt(14);

                    // src/com/google/doclava/parser/Java.g:861:9: 'continue' ( IDENTIFIER )? ';'
                    {
                    dbg.location(861,9);
                    match(input,CONTINUE,FOLLOW_CONTINUE_in_statement4362); if (state.failed) return ;
                    dbg.location(862,13);
                    // src/com/google/doclava/parser/Java.g:862:13: ( IDENTIFIER )?
                    int alt97=2;
                    try { dbg.enterSubRule(97);
                    try { dbg.enterDecision(97, decisionCanBacktrack[97]);

                    int LA97_0 = input.LA(1);

                    if ( (LA97_0==IDENTIFIER) ) {
                        alt97=1;
                    }
                    } finally {dbg.exitDecision(97);}

                    switch (alt97) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:862:14: IDENTIFIER
                            {
                            dbg.location(862,14);
                            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_statement4377); if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(97);}

                    dbg.location(863,16);
                    match(input,SEMI,FOLLOW_SEMI_in_statement4394); if (state.failed) return ;

                    }
                    break;
                case 15 :
                    dbg.enterAlt(15);

                    // src/com/google/doclava/parser/Java.g:864:9: expression ';'
                    {
                    dbg.location(864,9);
                    pushFollow(FOLLOW_expression_in_statement4404);
                    expression();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(864,21);
                    match(input,SEMI,FOLLOW_SEMI_in_statement4407); if (state.failed) return ;

                    }
                    break;
                case 16 :
                    dbg.enterAlt(16);

                    // src/com/google/doclava/parser/Java.g:865:9: IDENTIFIER ':' statement
                    {
                    dbg.location(865,9);
                    match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_statement4417); if (state.failed) return ;
                    dbg.location(865,20);
                    match(input,COLON,FOLLOW_COLON_in_statement4419); if (state.failed) return ;
                    dbg.location(865,24);
                    pushFollow(FOLLOW_statement_in_statement4421);
                    statement();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 17 :
                    dbg.enterAlt(17);

                    // src/com/google/doclava/parser/Java.g:866:9: ';'
                    {
                    dbg.location(866,9);
                    match(input,SEMI,FOLLOW_SEMI_in_statement4431); if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 58, statement_StartIndex); }
        }
        dbg.location(868, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "statement");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "statement"


    // $ANTLR start "switchBlockStatementGroups"
    // src/com/google/doclava/parser/Java.g:870:1: switchBlockStatementGroups : ( switchBlockStatementGroup )* ;
    public final void switchBlockStatementGroups() throws RecognitionException {
        int switchBlockStatementGroups_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "switchBlockStatementGroups");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(870, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 59) ) { return ; }
            // src/com/google/doclava/parser/Java.g:871:5: ( ( switchBlockStatementGroup )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:871:9: ( switchBlockStatementGroup )*
            {
            dbg.location(871,9);
            // src/com/google/doclava/parser/Java.g:871:9: ( switchBlockStatementGroup )*
            try { dbg.enterSubRule(99);

            loop99:
            do {
                int alt99=2;
                try { dbg.enterDecision(99, decisionCanBacktrack[99]);

                int LA99_0 = input.LA(1);

                if ( (LA99_0==CASE||LA99_0==DEFAULT) ) {
                    alt99=1;
                }


                } finally {dbg.exitDecision(99);}

                switch (alt99) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:871:10: switchBlockStatementGroup
		    {
		    dbg.location(871,10);
		    pushFollow(FOLLOW_switchBlockStatementGroup_in_switchBlockStatementGroups4452);
		    switchBlockStatementGroup();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop99;
                }
            } while (true);
            } finally {dbg.exitSubRule(99);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 59, switchBlockStatementGroups_StartIndex); }
        }
        dbg.location(872, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "switchBlockStatementGroups");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "switchBlockStatementGroups"


    // $ANTLR start "switchBlockStatementGroup"
    // src/com/google/doclava/parser/Java.g:874:1: switchBlockStatementGroup : switchLabel ( blockStatement )* ;
    public final void switchBlockStatementGroup() throws RecognitionException {
        int switchBlockStatementGroup_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "switchBlockStatementGroup");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(874, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 60) ) { return ; }
            // src/com/google/doclava/parser/Java.g:875:5: ( switchLabel ( blockStatement )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:876:9: switchLabel ( blockStatement )*
            {
            dbg.location(876,9);
            pushFollow(FOLLOW_switchLabel_in_switchBlockStatementGroup4480);
            switchLabel();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(877,9);
            // src/com/google/doclava/parser/Java.g:877:9: ( blockStatement )*
            try { dbg.enterSubRule(100);

            loop100:
            do {
                int alt100=2;
                try { dbg.enterDecision(100, decisionCanBacktrack[100]);

                int LA100_0 = input.LA(1);

                if ( ((LA100_0>=IDENTIFIER && LA100_0<=NULL)||(LA100_0>=ABSTRACT && LA100_0<=BYTE)||(LA100_0>=CHAR && LA100_0<=CLASS)||LA100_0==CONTINUE||(LA100_0>=DO && LA100_0<=DOUBLE)||LA100_0==ENUM||LA100_0==FINAL||(LA100_0>=FLOAT && LA100_0<=FOR)||LA100_0==IF||(LA100_0>=INT && LA100_0<=NEW)||(LA100_0>=PRIVATE && LA100_0<=THROW)||(LA100_0>=TRANSIENT && LA100_0<=LPAREN)||LA100_0==LBRACE||LA100_0==SEMI||(LA100_0>=BANG && LA100_0<=TILDE)||(LA100_0>=PLUSPLUS && LA100_0<=SUB)||LA100_0==MONKEYS_AT||LA100_0==LT) ) {
                    alt100=1;
                }


                } finally {dbg.exitDecision(100);}

                switch (alt100) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:877:10: blockStatement
		    {
		    dbg.location(877,10);
		    pushFollow(FOLLOW_blockStatement_in_switchBlockStatementGroup4491);
		    blockStatement();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop100;
                }
            } while (true);
            } finally {dbg.exitSubRule(100);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 60, switchBlockStatementGroup_StartIndex); }
        }
        dbg.location(879, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "switchBlockStatementGroup");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "switchBlockStatementGroup"


    // $ANTLR start "switchLabel"
    // src/com/google/doclava/parser/Java.g:881:1: switchLabel : ( 'case' expression ':' | 'default' ':' );
    public final void switchLabel() throws RecognitionException {
        int switchLabel_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "switchLabel");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(881, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 61) ) { return ; }
            // src/com/google/doclava/parser/Java.g:882:5: ( 'case' expression ':' | 'default' ':' )
            int alt101=2;
            try { dbg.enterDecision(101, decisionCanBacktrack[101]);

            int LA101_0 = input.LA(1);

            if ( (LA101_0==CASE) ) {
                alt101=1;
            }
            else if ( (LA101_0==DEFAULT) ) {
                alt101=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 101, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(101);}

            switch (alt101) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:882:9: 'case' expression ':'
                    {
                    dbg.location(882,9);
                    match(input,CASE,FOLLOW_CASE_in_switchLabel4521); if (state.failed) return ;
                    dbg.location(882,16);
                    pushFollow(FOLLOW_expression_in_switchLabel4523);
                    expression();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(882,27);
                    match(input,COLON,FOLLOW_COLON_in_switchLabel4525); if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:883:9: 'default' ':'
                    {
                    dbg.location(883,9);
                    match(input,DEFAULT,FOLLOW_DEFAULT_in_switchLabel4535); if (state.failed) return ;
                    dbg.location(883,19);
                    match(input,COLON,FOLLOW_COLON_in_switchLabel4537); if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 61, switchLabel_StartIndex); }
        }
        dbg.location(884, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "switchLabel");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "switchLabel"


    // $ANTLR start "trystatement"
    // src/com/google/doclava/parser/Java.g:887:1: trystatement : 'try' block ( catches 'finally' block | catches | 'finally' block ) ;
    public final void trystatement() throws RecognitionException {
        int trystatement_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "trystatement");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(887, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 62) ) { return ; }
            // src/com/google/doclava/parser/Java.g:888:5: ( 'try' block ( catches 'finally' block | catches | 'finally' block ) )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:888:9: 'try' block ( catches 'finally' block | catches | 'finally' block )
            {
            dbg.location(888,9);
            match(input,TRY,FOLLOW_TRY_in_trystatement4557); if (state.failed) return ;
            dbg.location(888,15);
            pushFollow(FOLLOW_block_in_trystatement4559);
            block();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(889,9);
            // src/com/google/doclava/parser/Java.g:889:9: ( catches 'finally' block | catches | 'finally' block )
            int alt102=3;
            try { dbg.enterSubRule(102);
            try { dbg.enterDecision(102, decisionCanBacktrack[102]);

            int LA102_0 = input.LA(1);

            if ( (LA102_0==CATCH) ) {
                int LA102_1 = input.LA(2);

                if ( (synpred153_Java()) ) {
                    alt102=1;
                }
                else if ( (synpred154_Java()) ) {
                    alt102=2;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 102, 1, input);

                    dbg.recognitionException(nvae);
                    throw nvae;
                }
            }
            else if ( (LA102_0==FINALLY) ) {
                alt102=3;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 102, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(102);}

            switch (alt102) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:889:13: catches 'finally' block
                    {
                    dbg.location(889,13);
                    pushFollow(FOLLOW_catches_in_trystatement4573);
                    catches();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(889,21);
                    match(input,FINALLY,FOLLOW_FINALLY_in_trystatement4575); if (state.failed) return ;
                    dbg.location(889,31);
                    pushFollow(FOLLOW_block_in_trystatement4577);
                    block();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:890:13: catches
                    {
                    dbg.location(890,13);
                    pushFollow(FOLLOW_catches_in_trystatement4591);
                    catches();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:891:13: 'finally' block
                    {
                    dbg.location(891,13);
                    match(input,FINALLY,FOLLOW_FINALLY_in_trystatement4605); if (state.failed) return ;
                    dbg.location(891,23);
                    pushFollow(FOLLOW_block_in_trystatement4607);
                    block();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(102);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 62, trystatement_StartIndex); }
        }
        dbg.location(893, 6);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "trystatement");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "trystatement"


    // $ANTLR start "catches"
    // src/com/google/doclava/parser/Java.g:895:1: catches : catchClause ( catchClause )* ;
    public final void catches() throws RecognitionException {
        int catches_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "catches");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(895, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 63) ) { return ; }
            // src/com/google/doclava/parser/Java.g:896:5: ( catchClause ( catchClause )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:896:9: catchClause ( catchClause )*
            {
            dbg.location(896,9);
            pushFollow(FOLLOW_catchClause_in_catches4637);
            catchClause();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(897,9);
            // src/com/google/doclava/parser/Java.g:897:9: ( catchClause )*
            try { dbg.enterSubRule(103);

            loop103:
            do {
                int alt103=2;
                try { dbg.enterDecision(103, decisionCanBacktrack[103]);

                int LA103_0 = input.LA(1);

                if ( (LA103_0==CATCH) ) {
                    alt103=1;
                }


                } finally {dbg.exitDecision(103);}

                switch (alt103) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:897:10: catchClause
		    {
		    dbg.location(897,10);
		    pushFollow(FOLLOW_catchClause_in_catches4648);
		    catchClause();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop103;
                }
            } while (true);
            } finally {dbg.exitSubRule(103);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 63, catches_StartIndex); }
        }
        dbg.location(899, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "catches");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "catches"


    // $ANTLR start "catchClause"
    // src/com/google/doclava/parser/Java.g:901:1: catchClause : 'catch' '(' formalParameter ')' block ;
    public final void catchClause() throws RecognitionException {
        int catchClause_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "catchClause");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(901, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 64) ) { return ; }
            // src/com/google/doclava/parser/Java.g:902:5: ( 'catch' '(' formalParameter ')' block )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:902:9: 'catch' '(' formalParameter ')' block
            {
            dbg.location(902,9);
            match(input,CATCH,FOLLOW_CATCH_in_catchClause4678); if (state.failed) return ;
            dbg.location(902,17);
            match(input,LPAREN,FOLLOW_LPAREN_in_catchClause4680); if (state.failed) return ;
            dbg.location(902,21);
            pushFollow(FOLLOW_formalParameter_in_catchClause4682);
            formalParameter();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(903,9);
            match(input,RPAREN,FOLLOW_RPAREN_in_catchClause4692); if (state.failed) return ;
            dbg.location(903,13);
            pushFollow(FOLLOW_block_in_catchClause4694);
            block();

            state._fsp--;
            if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 64, catchClause_StartIndex); }
        }
        dbg.location(904, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "catchClause");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "catchClause"


    // $ANTLR start "formalParameter"
    // src/com/google/doclava/parser/Java.g:906:1: formalParameter : variableModifiers type IDENTIFIER ( '[' ']' )* ;
    public final void formalParameter() throws RecognitionException {
        int formalParameter_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "formalParameter");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(906, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 65) ) { return ; }
            // src/com/google/doclava/parser/Java.g:907:5: ( variableModifiers type IDENTIFIER ( '[' ']' )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:907:9: variableModifiers type IDENTIFIER ( '[' ']' )*
            {
            dbg.location(907,9);
            pushFollow(FOLLOW_variableModifiers_in_formalParameter4713);
            variableModifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(907,27);
            pushFollow(FOLLOW_type_in_formalParameter4715);
            type();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(907,32);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_formalParameter4717); if (state.failed) return ;
            dbg.location(908,9);
            // src/com/google/doclava/parser/Java.g:908:9: ( '[' ']' )*
            try { dbg.enterSubRule(104);

            loop104:
            do {
                int alt104=2;
                try { dbg.enterDecision(104, decisionCanBacktrack[104]);

                int LA104_0 = input.LA(1);

                if ( (LA104_0==LBRACKET) ) {
                    alt104=1;
                }


                } finally {dbg.exitDecision(104);}

                switch (alt104) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:908:10: '[' ']'
		    {
		    dbg.location(908,10);
		    match(input,LBRACKET,FOLLOW_LBRACKET_in_formalParameter4728); if (state.failed) return ;
		    dbg.location(908,14);
		    match(input,RBRACKET,FOLLOW_RBRACKET_in_formalParameter4730); if (state.failed) return ;

		    }
		    break;

		default :
		    break loop104;
                }
            } while (true);
            } finally {dbg.exitSubRule(104);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 65, formalParameter_StartIndex); }
        }
        dbg.location(910, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "formalParameter");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "formalParameter"


    // $ANTLR start "forstatement"
    // src/com/google/doclava/parser/Java.g:912:1: forstatement : ( 'for' '(' variableModifiers type IDENTIFIER ':' expression ')' statement | 'for' '(' ( forInit )? ';' ( expression )? ';' ( expressionList )? ')' statement );
    public final void forstatement() throws RecognitionException {
        int forstatement_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "forstatement");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(912, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 66) ) { return ; }
            // src/com/google/doclava/parser/Java.g:913:5: ( 'for' '(' variableModifiers type IDENTIFIER ':' expression ')' statement | 'for' '(' ( forInit )? ';' ( expression )? ';' ( expressionList )? ')' statement )
            int alt108=2;
            try { dbg.enterDecision(108, decisionCanBacktrack[108]);

            int LA108_0 = input.LA(1);

            if ( (LA108_0==FOR) ) {
                int LA108_1 = input.LA(2);

                if ( (synpred157_Java()) ) {
                    alt108=1;
                }
                else if ( (true) ) {
                    alt108=2;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 108, 1, input);

                    dbg.recognitionException(nvae);
                    throw nvae;
                }
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 108, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(108);}

            switch (alt108) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:915:9: 'for' '(' variableModifiers type IDENTIFIER ':' expression ')' statement
                    {
                    dbg.location(915,9);
                    match(input,FOR,FOLLOW_FOR_in_forstatement4775); if (state.failed) return ;
                    dbg.location(915,15);
                    match(input,LPAREN,FOLLOW_LPAREN_in_forstatement4777); if (state.failed) return ;
                    dbg.location(915,19);
                    pushFollow(FOLLOW_variableModifiers_in_forstatement4779);
                    variableModifiers();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(915,37);
                    pushFollow(FOLLOW_type_in_forstatement4781);
                    type();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(915,42);
                    match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_forstatement4783); if (state.failed) return ;
                    dbg.location(915,53);
                    match(input,COLON,FOLLOW_COLON_in_forstatement4785); if (state.failed) return ;
                    dbg.location(916,9);
                    pushFollow(FOLLOW_expression_in_forstatement4795);
                    expression();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(916,20);
                    match(input,RPAREN,FOLLOW_RPAREN_in_forstatement4797); if (state.failed) return ;
                    dbg.location(916,24);
                    pushFollow(FOLLOW_statement_in_forstatement4799);
                    statement();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:919:9: 'for' '(' ( forInit )? ';' ( expression )? ';' ( expressionList )? ')' statement
                    {
                    dbg.location(919,9);
                    match(input,FOR,FOLLOW_FOR_in_forstatement4819); if (state.failed) return ;
                    dbg.location(919,15);
                    match(input,LPAREN,FOLLOW_LPAREN_in_forstatement4821); if (state.failed) return ;
                    dbg.location(920,17);
                    // src/com/google/doclava/parser/Java.g:920:17: ( forInit )?
                    int alt105=2;
                    try { dbg.enterSubRule(105);
                    try { dbg.enterDecision(105, decisionCanBacktrack[105]);

                    int LA105_0 = input.LA(1);

                    if ( ((LA105_0>=IDENTIFIER && LA105_0<=NULL)||LA105_0==BOOLEAN||LA105_0==BYTE||LA105_0==CHAR||LA105_0==DOUBLE||LA105_0==FINAL||LA105_0==FLOAT||LA105_0==INT||LA105_0==LONG||LA105_0==NEW||LA105_0==SHORT||LA105_0==SUPER||LA105_0==THIS||LA105_0==VOID||LA105_0==LPAREN||(LA105_0>=BANG && LA105_0<=TILDE)||(LA105_0>=PLUSPLUS && LA105_0<=SUB)||LA105_0==MONKEYS_AT) ) {
                        alt105=1;
                    }
                    } finally {dbg.exitDecision(105);}

                    switch (alt105) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:920:18: forInit
                            {
                            dbg.location(920,18);
                            pushFollow(FOLLOW_forInit_in_forstatement4840);
                            forInit();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(105);}

                    dbg.location(921,20);
                    match(input,SEMI,FOLLOW_SEMI_in_forstatement4861); if (state.failed) return ;
                    dbg.location(922,17);
                    // src/com/google/doclava/parser/Java.g:922:17: ( expression )?
                    int alt106=2;
                    try { dbg.enterSubRule(106);
                    try { dbg.enterDecision(106, decisionCanBacktrack[106]);

                    int LA106_0 = input.LA(1);

                    if ( ((LA106_0>=IDENTIFIER && LA106_0<=NULL)||LA106_0==BOOLEAN||LA106_0==BYTE||LA106_0==CHAR||LA106_0==DOUBLE||LA106_0==FLOAT||LA106_0==INT||LA106_0==LONG||LA106_0==NEW||LA106_0==SHORT||LA106_0==SUPER||LA106_0==THIS||LA106_0==VOID||LA106_0==LPAREN||(LA106_0>=BANG && LA106_0<=TILDE)||(LA106_0>=PLUSPLUS && LA106_0<=SUB)) ) {
                        alt106=1;
                    }
                    } finally {dbg.exitDecision(106);}

                    switch (alt106) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:922:18: expression
                            {
                            dbg.location(922,18);
                            pushFollow(FOLLOW_expression_in_forstatement4880);
                            expression();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(106);}

                    dbg.location(923,20);
                    match(input,SEMI,FOLLOW_SEMI_in_forstatement4901); if (state.failed) return ;
                    dbg.location(924,17);
                    // src/com/google/doclava/parser/Java.g:924:17: ( expressionList )?
                    int alt107=2;
                    try { dbg.enterSubRule(107);
                    try { dbg.enterDecision(107, decisionCanBacktrack[107]);

                    int LA107_0 = input.LA(1);

                    if ( ((LA107_0>=IDENTIFIER && LA107_0<=NULL)||LA107_0==BOOLEAN||LA107_0==BYTE||LA107_0==CHAR||LA107_0==DOUBLE||LA107_0==FLOAT||LA107_0==INT||LA107_0==LONG||LA107_0==NEW||LA107_0==SHORT||LA107_0==SUPER||LA107_0==THIS||LA107_0==VOID||LA107_0==LPAREN||(LA107_0>=BANG && LA107_0<=TILDE)||(LA107_0>=PLUSPLUS && LA107_0<=SUB)) ) {
                        alt107=1;
                    }
                    } finally {dbg.exitDecision(107);}

                    switch (alt107) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:924:18: expressionList
                            {
                            dbg.location(924,18);
                            pushFollow(FOLLOW_expressionList_in_forstatement4920);
                            expressionList();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(107);}

                    dbg.location(925,20);
                    match(input,RPAREN,FOLLOW_RPAREN_in_forstatement4941); if (state.failed) return ;
                    dbg.location(925,24);
                    pushFollow(FOLLOW_statement_in_forstatement4943);
                    statement();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 66, forstatement_StartIndex); }
        }
        dbg.location(926, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "forstatement");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "forstatement"


    // $ANTLR start "forInit"
    // src/com/google/doclava/parser/Java.g:928:1: forInit : ( localVariableDeclaration | expressionList );
    public final void forInit() throws RecognitionException {
        int forInit_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "forInit");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(928, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 67) ) { return ; }
            // src/com/google/doclava/parser/Java.g:929:5: ( localVariableDeclaration | expressionList )
            int alt109=2;
            try { dbg.enterDecision(109, decisionCanBacktrack[109]);

            try {
                isCyclicDecision = true;
                alt109 = dfa109.predict(input);
            }
            catch (NoViableAltException nvae) {
                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(109);}

            switch (alt109) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:929:9: localVariableDeclaration
                    {
                    dbg.location(929,9);
                    pushFollow(FOLLOW_localVariableDeclaration_in_forInit4962);
                    localVariableDeclaration();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:930:9: expressionList
                    {
                    dbg.location(930,9);
                    pushFollow(FOLLOW_expressionList_in_forInit4972);
                    expressionList();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 67, forInit_StartIndex); }
        }
        dbg.location(931, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "forInit");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "forInit"


    // $ANTLR start "parExpression"
    // src/com/google/doclava/parser/Java.g:933:1: parExpression : '(' expression ')' ;
    public final void parExpression() throws RecognitionException {
        int parExpression_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "parExpression");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(933, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 68) ) { return ; }
            // src/com/google/doclava/parser/Java.g:934:5: ( '(' expression ')' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:934:9: '(' expression ')'
            {
            dbg.location(934,9);
            match(input,LPAREN,FOLLOW_LPAREN_in_parExpression4991); if (state.failed) return ;
            dbg.location(934,13);
            pushFollow(FOLLOW_expression_in_parExpression4993);
            expression();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(934,24);
            match(input,RPAREN,FOLLOW_RPAREN_in_parExpression4995); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 68, parExpression_StartIndex); }
        }
        dbg.location(935, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "parExpression");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "parExpression"


    // $ANTLR start "expressionList"
    // src/com/google/doclava/parser/Java.g:937:1: expressionList : expression ( ',' expression )* ;
    public final void expressionList() throws RecognitionException {
        int expressionList_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "expressionList");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(937, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 69) ) { return ; }
            // src/com/google/doclava/parser/Java.g:938:5: ( expression ( ',' expression )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:938:9: expression ( ',' expression )*
            {
            dbg.location(938,9);
            pushFollow(FOLLOW_expression_in_expressionList5014);
            expression();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(939,9);
            // src/com/google/doclava/parser/Java.g:939:9: ( ',' expression )*
            try { dbg.enterSubRule(110);

            loop110:
            do {
                int alt110=2;
                try { dbg.enterDecision(110, decisionCanBacktrack[110]);

                int LA110_0 = input.LA(1);

                if ( (LA110_0==COMMA) ) {
                    alt110=1;
                }


                } finally {dbg.exitDecision(110);}

                switch (alt110) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:939:10: ',' expression
		    {
		    dbg.location(939,10);
		    match(input,COMMA,FOLLOW_COMMA_in_expressionList5025); if (state.failed) return ;
		    dbg.location(939,14);
		    pushFollow(FOLLOW_expression_in_expressionList5027);
		    expression();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop110;
                }
            } while (true);
            } finally {dbg.exitSubRule(110);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 69, expressionList_StartIndex); }
        }
        dbg.location(941, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "expressionList");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "expressionList"


    // $ANTLR start "expression"
    // src/com/google/doclava/parser/Java.g:944:1: expression : conditionalExpression ( assignmentOperator expression )? ;
    public final void expression() throws RecognitionException {
        int expression_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "expression");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(944, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 70) ) { return ; }
            // src/com/google/doclava/parser/Java.g:945:5: ( conditionalExpression ( assignmentOperator expression )? )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:945:9: conditionalExpression ( assignmentOperator expression )?
            {
            dbg.location(945,9);
            pushFollow(FOLLOW_conditionalExpression_in_expression5058);
            conditionalExpression();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(946,9);
            // src/com/google/doclava/parser/Java.g:946:9: ( assignmentOperator expression )?
            int alt111=2;
            try { dbg.enterSubRule(111);
            try { dbg.enterDecision(111, decisionCanBacktrack[111]);

            int LA111_0 = input.LA(1);

            if ( (LA111_0==EQ||(LA111_0>=PLUSEQ && LA111_0<=PERCENTEQ)||(LA111_0>=GT && LA111_0<=LT)) ) {
                alt111=1;
            }
            } finally {dbg.exitDecision(111);}

            switch (alt111) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:946:10: assignmentOperator expression
                    {
                    dbg.location(946,10);
                    pushFollow(FOLLOW_assignmentOperator_in_expression5069);
                    assignmentOperator();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(946,29);
                    pushFollow(FOLLOW_expression_in_expression5071);
                    expression();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(111);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 70, expression_StartIndex); }
        }
        dbg.location(948, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "expression");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "expression"


    // $ANTLR start "assignmentOperator"
    // src/com/google/doclava/parser/Java.g:951:1: assignmentOperator : ( '=' | '+=' | '-=' | '*=' | '/=' | '&=' | '|=' | '^=' | '%=' | '<' '<' '=' | '>' '>' '>' '=' | '>' '>' '=' );
    public final void assignmentOperator() throws RecognitionException {
        int assignmentOperator_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "assignmentOperator");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(951, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 71) ) { return ; }
            // src/com/google/doclava/parser/Java.g:952:5: ( '=' | '+=' | '-=' | '*=' | '/=' | '&=' | '|=' | '^=' | '%=' | '<' '<' '=' | '>' '>' '>' '=' | '>' '>' '=' )
            int alt112=12;
            try { dbg.enterDecision(112, decisionCanBacktrack[112]);

            try {
                isCyclicDecision = true;
                alt112 = dfa112.predict(input);
            }
            catch (NoViableAltException nvae) {
                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(112);}

            switch (alt112) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:952:9: '='
                    {
                    dbg.location(952,9);
                    match(input,EQ,FOLLOW_EQ_in_assignmentOperator5102); if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:953:9: '+='
                    {
                    dbg.location(953,9);
                    match(input,PLUSEQ,FOLLOW_PLUSEQ_in_assignmentOperator5112); if (state.failed) return ;

                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:954:9: '-='
                    {
                    dbg.location(954,9);
                    match(input,SUBEQ,FOLLOW_SUBEQ_in_assignmentOperator5122); if (state.failed) return ;

                    }
                    break;
                case 4 :
                    dbg.enterAlt(4);

                    // src/com/google/doclava/parser/Java.g:955:9: '*='
                    {
                    dbg.location(955,9);
                    match(input,STAREQ,FOLLOW_STAREQ_in_assignmentOperator5132); if (state.failed) return ;

                    }
                    break;
                case 5 :
                    dbg.enterAlt(5);

                    // src/com/google/doclava/parser/Java.g:956:9: '/='
                    {
                    dbg.location(956,9);
                    match(input,SLASHEQ,FOLLOW_SLASHEQ_in_assignmentOperator5142); if (state.failed) return ;

                    }
                    break;
                case 6 :
                    dbg.enterAlt(6);

                    // src/com/google/doclava/parser/Java.g:957:9: '&='
                    {
                    dbg.location(957,9);
                    match(input,AMPEQ,FOLLOW_AMPEQ_in_assignmentOperator5152); if (state.failed) return ;

                    }
                    break;
                case 7 :
                    dbg.enterAlt(7);

                    // src/com/google/doclava/parser/Java.g:958:9: '|='
                    {
                    dbg.location(958,9);
                    match(input,BAREQ,FOLLOW_BAREQ_in_assignmentOperator5162); if (state.failed) return ;

                    }
                    break;
                case 8 :
                    dbg.enterAlt(8);

                    // src/com/google/doclava/parser/Java.g:959:9: '^='
                    {
                    dbg.location(959,9);
                    match(input,CARETEQ,FOLLOW_CARETEQ_in_assignmentOperator5172); if (state.failed) return ;

                    }
                    break;
                case 9 :
                    dbg.enterAlt(9);

                    // src/com/google/doclava/parser/Java.g:960:9: '%='
                    {
                    dbg.location(960,9);
                    match(input,PERCENTEQ,FOLLOW_PERCENTEQ_in_assignmentOperator5182); if (state.failed) return ;

                    }
                    break;
                case 10 :
                    dbg.enterAlt(10);

                    // src/com/google/doclava/parser/Java.g:961:10: '<' '<' '='
                    {
                    dbg.location(961,10);
                    match(input,LT,FOLLOW_LT_in_assignmentOperator5193); if (state.failed) return ;
                    dbg.location(961,14);
                    match(input,LT,FOLLOW_LT_in_assignmentOperator5195); if (state.failed) return ;
                    dbg.location(961,18);
                    match(input,EQ,FOLLOW_EQ_in_assignmentOperator5197); if (state.failed) return ;

                    }
                    break;
                case 11 :
                    dbg.enterAlt(11);

                    // src/com/google/doclava/parser/Java.g:962:10: '>' '>' '>' '='
                    {
                    dbg.location(962,10);
                    match(input,GT,FOLLOW_GT_in_assignmentOperator5208); if (state.failed) return ;
                    dbg.location(962,14);
                    match(input,GT,FOLLOW_GT_in_assignmentOperator5210); if (state.failed) return ;
                    dbg.location(962,18);
                    match(input,GT,FOLLOW_GT_in_assignmentOperator5212); if (state.failed) return ;
                    dbg.location(962,22);
                    match(input,EQ,FOLLOW_EQ_in_assignmentOperator5214); if (state.failed) return ;

                    }
                    break;
                case 12 :
                    dbg.enterAlt(12);

                    // src/com/google/doclava/parser/Java.g:963:10: '>' '>' '='
                    {
                    dbg.location(963,10);
                    match(input,GT,FOLLOW_GT_in_assignmentOperator5225); if (state.failed) return ;
                    dbg.location(963,14);
                    match(input,GT,FOLLOW_GT_in_assignmentOperator5227); if (state.failed) return ;
                    dbg.location(963,18);
                    match(input,EQ,FOLLOW_EQ_in_assignmentOperator5229); if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 71, assignmentOperator_StartIndex); }
        }
        dbg.location(964, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "assignmentOperator");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "assignmentOperator"


    // $ANTLR start "conditionalExpression"
    // src/com/google/doclava/parser/Java.g:967:1: conditionalExpression : conditionalOrExpression ( '?' expression ':' conditionalExpression )? ;
    public final void conditionalExpression() throws RecognitionException {
        int conditionalExpression_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "conditionalExpression");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(967, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 72) ) { return ; }
            // src/com/google/doclava/parser/Java.g:968:5: ( conditionalOrExpression ( '?' expression ':' conditionalExpression )? )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:968:9: conditionalOrExpression ( '?' expression ':' conditionalExpression )?
            {
            dbg.location(968,9);
            pushFollow(FOLLOW_conditionalOrExpression_in_conditionalExpression5249);
            conditionalOrExpression();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(969,9);
            // src/com/google/doclava/parser/Java.g:969:9: ( '?' expression ':' conditionalExpression )?
            int alt113=2;
            try { dbg.enterSubRule(113);
            try { dbg.enterDecision(113, decisionCanBacktrack[113]);

            int LA113_0 = input.LA(1);

            if ( (LA113_0==QUES) ) {
                alt113=1;
            }
            } finally {dbg.exitDecision(113);}

            switch (alt113) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:969:10: '?' expression ':' conditionalExpression
                    {
                    dbg.location(969,10);
                    match(input,QUES,FOLLOW_QUES_in_conditionalExpression5260); if (state.failed) return ;
                    dbg.location(969,14);
                    pushFollow(FOLLOW_expression_in_conditionalExpression5262);
                    expression();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(969,25);
                    match(input,COLON,FOLLOW_COLON_in_conditionalExpression5264); if (state.failed) return ;
                    dbg.location(969,29);
                    pushFollow(FOLLOW_conditionalExpression_in_conditionalExpression5266);
                    conditionalExpression();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(113);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 72, conditionalExpression_StartIndex); }
        }
        dbg.location(971, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "conditionalExpression");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "conditionalExpression"


    // $ANTLR start "conditionalOrExpression"
    // src/com/google/doclava/parser/Java.g:973:1: conditionalOrExpression : conditionalAndExpression ( '||' conditionalAndExpression )* ;
    public final void conditionalOrExpression() throws RecognitionException {
        int conditionalOrExpression_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "conditionalOrExpression");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(973, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 73) ) { return ; }
            // src/com/google/doclava/parser/Java.g:974:5: ( conditionalAndExpression ( '||' conditionalAndExpression )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:974:9: conditionalAndExpression ( '||' conditionalAndExpression )*
            {
            dbg.location(974,9);
            pushFollow(FOLLOW_conditionalAndExpression_in_conditionalOrExpression5296);
            conditionalAndExpression();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(975,9);
            // src/com/google/doclava/parser/Java.g:975:9: ( '||' conditionalAndExpression )*
            try { dbg.enterSubRule(114);

            loop114:
            do {
                int alt114=2;
                try { dbg.enterDecision(114, decisionCanBacktrack[114]);

                int LA114_0 = input.LA(1);

                if ( (LA114_0==BARBAR) ) {
                    alt114=1;
                }


                } finally {dbg.exitDecision(114);}

                switch (alt114) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:975:10: '||' conditionalAndExpression
		    {
		    dbg.location(975,10);
		    match(input,BARBAR,FOLLOW_BARBAR_in_conditionalOrExpression5307); if (state.failed) return ;
		    dbg.location(975,15);
		    pushFollow(FOLLOW_conditionalAndExpression_in_conditionalOrExpression5309);
		    conditionalAndExpression();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop114;
                }
            } while (true);
            } finally {dbg.exitSubRule(114);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 73, conditionalOrExpression_StartIndex); }
        }
        dbg.location(977, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "conditionalOrExpression");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "conditionalOrExpression"


    // $ANTLR start "conditionalAndExpression"
    // src/com/google/doclava/parser/Java.g:979:1: conditionalAndExpression : inclusiveOrExpression ( '&&' inclusiveOrExpression )* ;
    public final void conditionalAndExpression() throws RecognitionException {
        int conditionalAndExpression_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "conditionalAndExpression");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(979, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 74) ) { return ; }
            // src/com/google/doclava/parser/Java.g:980:5: ( inclusiveOrExpression ( '&&' inclusiveOrExpression )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:980:9: inclusiveOrExpression ( '&&' inclusiveOrExpression )*
            {
            dbg.location(980,9);
            pushFollow(FOLLOW_inclusiveOrExpression_in_conditionalAndExpression5339);
            inclusiveOrExpression();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(981,9);
            // src/com/google/doclava/parser/Java.g:981:9: ( '&&' inclusiveOrExpression )*
            try { dbg.enterSubRule(115);

            loop115:
            do {
                int alt115=2;
                try { dbg.enterDecision(115, decisionCanBacktrack[115]);

                int LA115_0 = input.LA(1);

                if ( (LA115_0==AMPAMP) ) {
                    alt115=1;
                }


                } finally {dbg.exitDecision(115);}

                switch (alt115) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:981:10: '&&' inclusiveOrExpression
		    {
		    dbg.location(981,10);
		    match(input,AMPAMP,FOLLOW_AMPAMP_in_conditionalAndExpression5350); if (state.failed) return ;
		    dbg.location(981,15);
		    pushFollow(FOLLOW_inclusiveOrExpression_in_conditionalAndExpression5352);
		    inclusiveOrExpression();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop115;
                }
            } while (true);
            } finally {dbg.exitSubRule(115);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 74, conditionalAndExpression_StartIndex); }
        }
        dbg.location(983, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "conditionalAndExpression");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "conditionalAndExpression"


    // $ANTLR start "inclusiveOrExpression"
    // src/com/google/doclava/parser/Java.g:985:1: inclusiveOrExpression : exclusiveOrExpression ( '|' exclusiveOrExpression )* ;
    public final void inclusiveOrExpression() throws RecognitionException {
        int inclusiveOrExpression_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "inclusiveOrExpression");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(985, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 75) ) { return ; }
            // src/com/google/doclava/parser/Java.g:986:5: ( exclusiveOrExpression ( '|' exclusiveOrExpression )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:986:9: exclusiveOrExpression ( '|' exclusiveOrExpression )*
            {
            dbg.location(986,9);
            pushFollow(FOLLOW_exclusiveOrExpression_in_inclusiveOrExpression5382);
            exclusiveOrExpression();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(987,9);
            // src/com/google/doclava/parser/Java.g:987:9: ( '|' exclusiveOrExpression )*
            try { dbg.enterSubRule(116);

            loop116:
            do {
                int alt116=2;
                try { dbg.enterDecision(116, decisionCanBacktrack[116]);

                int LA116_0 = input.LA(1);

                if ( (LA116_0==BAR) ) {
                    alt116=1;
                }


                } finally {dbg.exitDecision(116);}

                switch (alt116) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:987:10: '|' exclusiveOrExpression
		    {
		    dbg.location(987,10);
		    match(input,BAR,FOLLOW_BAR_in_inclusiveOrExpression5393); if (state.failed) return ;
		    dbg.location(987,14);
		    pushFollow(FOLLOW_exclusiveOrExpression_in_inclusiveOrExpression5395);
		    exclusiveOrExpression();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop116;
                }
            } while (true);
            } finally {dbg.exitSubRule(116);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 75, inclusiveOrExpression_StartIndex); }
        }
        dbg.location(989, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "inclusiveOrExpression");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "inclusiveOrExpression"


    // $ANTLR start "exclusiveOrExpression"
    // src/com/google/doclava/parser/Java.g:991:1: exclusiveOrExpression : andExpression ( '^' andExpression )* ;
    public final void exclusiveOrExpression() throws RecognitionException {
        int exclusiveOrExpression_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "exclusiveOrExpression");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(991, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 76) ) { return ; }
            // src/com/google/doclava/parser/Java.g:992:5: ( andExpression ( '^' andExpression )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:992:9: andExpression ( '^' andExpression )*
            {
            dbg.location(992,9);
            pushFollow(FOLLOW_andExpression_in_exclusiveOrExpression5425);
            andExpression();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(993,9);
            // src/com/google/doclava/parser/Java.g:993:9: ( '^' andExpression )*
            try { dbg.enterSubRule(117);

            loop117:
            do {
                int alt117=2;
                try { dbg.enterDecision(117, decisionCanBacktrack[117]);

                int LA117_0 = input.LA(1);

                if ( (LA117_0==CARET) ) {
                    alt117=1;
                }


                } finally {dbg.exitDecision(117);}

                switch (alt117) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:993:10: '^' andExpression
		    {
		    dbg.location(993,10);
		    match(input,CARET,FOLLOW_CARET_in_exclusiveOrExpression5436); if (state.failed) return ;
		    dbg.location(993,14);
		    pushFollow(FOLLOW_andExpression_in_exclusiveOrExpression5438);
		    andExpression();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop117;
                }
            } while (true);
            } finally {dbg.exitSubRule(117);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 76, exclusiveOrExpression_StartIndex); }
        }
        dbg.location(995, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "exclusiveOrExpression");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "exclusiveOrExpression"


    // $ANTLR start "andExpression"
    // src/com/google/doclava/parser/Java.g:997:1: andExpression : equalityExpression ( '&' equalityExpression )* ;
    public final void andExpression() throws RecognitionException {
        int andExpression_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "andExpression");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(997, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 77) ) { return ; }
            // src/com/google/doclava/parser/Java.g:998:5: ( equalityExpression ( '&' equalityExpression )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:998:9: equalityExpression ( '&' equalityExpression )*
            {
            dbg.location(998,9);
            pushFollow(FOLLOW_equalityExpression_in_andExpression5468);
            equalityExpression();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(999,9);
            // src/com/google/doclava/parser/Java.g:999:9: ( '&' equalityExpression )*
            try { dbg.enterSubRule(118);

            loop118:
            do {
                int alt118=2;
                try { dbg.enterDecision(118, decisionCanBacktrack[118]);

                int LA118_0 = input.LA(1);

                if ( (LA118_0==AMP) ) {
                    alt118=1;
                }


                } finally {dbg.exitDecision(118);}

                switch (alt118) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:999:10: '&' equalityExpression
		    {
		    dbg.location(999,10);
		    match(input,AMP,FOLLOW_AMP_in_andExpression5479); if (state.failed) return ;
		    dbg.location(999,14);
		    pushFollow(FOLLOW_equalityExpression_in_andExpression5481);
		    equalityExpression();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop118;
                }
            } while (true);
            } finally {dbg.exitSubRule(118);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 77, andExpression_StartIndex); }
        }
        dbg.location(1001, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "andExpression");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "andExpression"


    // $ANTLR start "equalityExpression"
    // src/com/google/doclava/parser/Java.g:1003:1: equalityExpression : instanceOfExpression ( ( '==' | '!=' ) instanceOfExpression )* ;
    public final void equalityExpression() throws RecognitionException {
        int equalityExpression_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "equalityExpression");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1003, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 78) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1004:5: ( instanceOfExpression ( ( '==' | '!=' ) instanceOfExpression )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1004:9: instanceOfExpression ( ( '==' | '!=' ) instanceOfExpression )*
            {
            dbg.location(1004,9);
            pushFollow(FOLLOW_instanceOfExpression_in_equalityExpression5511);
            instanceOfExpression();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1005,9);
            // src/com/google/doclava/parser/Java.g:1005:9: ( ( '==' | '!=' ) instanceOfExpression )*
            try { dbg.enterSubRule(119);

            loop119:
            do {
                int alt119=2;
                try { dbg.enterDecision(119, decisionCanBacktrack[119]);

                int LA119_0 = input.LA(1);

                if ( (LA119_0==EQEQ||LA119_0==BANGEQ) ) {
                    alt119=1;
                }


                } finally {dbg.exitDecision(119);}

                switch (alt119) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:1006:13: ( '==' | '!=' ) instanceOfExpression
		    {
		    dbg.location(1006,13);
		    if ( input.LA(1)==EQEQ||input.LA(1)==BANGEQ ) {
		        input.consume();
		        state.errorRecovery=false;state.failed=false;
		    }
		    else {
		        if (state.backtracking>0) {state.failed=true; return ;}
		        MismatchedSetException mse = new MismatchedSetException(null,input);
		        dbg.recognitionException(mse);
		        throw mse;
		    }

		    dbg.location(1009,13);
		    pushFollow(FOLLOW_instanceOfExpression_in_equalityExpression5585);
		    instanceOfExpression();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop119;
                }
            } while (true);
            } finally {dbg.exitSubRule(119);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 78, equalityExpression_StartIndex); }
        }
        dbg.location(1011, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "equalityExpression");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "equalityExpression"


    // $ANTLR start "instanceOfExpression"
    // src/com/google/doclava/parser/Java.g:1013:1: instanceOfExpression : relationalExpression ( 'instanceof' type )? ;
    public final void instanceOfExpression() throws RecognitionException {
        int instanceOfExpression_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "instanceOfExpression");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1013, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 79) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1014:5: ( relationalExpression ( 'instanceof' type )? )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1014:9: relationalExpression ( 'instanceof' type )?
            {
            dbg.location(1014,9);
            pushFollow(FOLLOW_relationalExpression_in_instanceOfExpression5615);
            relationalExpression();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1015,9);
            // src/com/google/doclava/parser/Java.g:1015:9: ( 'instanceof' type )?
            int alt120=2;
            try { dbg.enterSubRule(120);
            try { dbg.enterDecision(120, decisionCanBacktrack[120]);

            int LA120_0 = input.LA(1);

            if ( (LA120_0==INSTANCEOF) ) {
                alt120=1;
            }
            } finally {dbg.exitDecision(120);}

            switch (alt120) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1015:10: 'instanceof' type
                    {
                    dbg.location(1015,10);
                    match(input,INSTANCEOF,FOLLOW_INSTANCEOF_in_instanceOfExpression5626); if (state.failed) return ;
                    dbg.location(1015,23);
                    pushFollow(FOLLOW_type_in_instanceOfExpression5628);
                    type();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(120);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 79, instanceOfExpression_StartIndex); }
        }
        dbg.location(1017, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "instanceOfExpression");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "instanceOfExpression"


    // $ANTLR start "relationalExpression"
    // src/com/google/doclava/parser/Java.g:1019:1: relationalExpression : shiftExpression ( relationalOp shiftExpression )* ;
    public final void relationalExpression() throws RecognitionException {
        int relationalExpression_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "relationalExpression");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1019, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 80) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1020:5: ( shiftExpression ( relationalOp shiftExpression )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1020:9: shiftExpression ( relationalOp shiftExpression )*
            {
            dbg.location(1020,9);
            pushFollow(FOLLOW_shiftExpression_in_relationalExpression5658);
            shiftExpression();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1021,9);
            // src/com/google/doclava/parser/Java.g:1021:9: ( relationalOp shiftExpression )*
            try { dbg.enterSubRule(121);

            loop121:
            do {
                int alt121=2;
                try { dbg.enterDecision(121, decisionCanBacktrack[121]);

                int LA121_0 = input.LA(1);

                if ( (LA121_0==LT) ) {
                    int LA121_2 = input.LA(2);

                    if ( ((LA121_2>=IDENTIFIER && LA121_2<=NULL)||LA121_2==BOOLEAN||LA121_2==BYTE||LA121_2==CHAR||LA121_2==DOUBLE||LA121_2==FLOAT||LA121_2==INT||LA121_2==LONG||LA121_2==NEW||LA121_2==SHORT||LA121_2==SUPER||LA121_2==THIS||LA121_2==VOID||LA121_2==LPAREN||(LA121_2>=EQ && LA121_2<=TILDE)||(LA121_2>=PLUSPLUS && LA121_2<=SUB)) ) {
                        alt121=1;
                    }


                }
                else if ( (LA121_0==GT) ) {
                    int LA121_3 = input.LA(2);

                    if ( ((LA121_3>=IDENTIFIER && LA121_3<=NULL)||LA121_3==BOOLEAN||LA121_3==BYTE||LA121_3==CHAR||LA121_3==DOUBLE||LA121_3==FLOAT||LA121_3==INT||LA121_3==LONG||LA121_3==NEW||LA121_3==SHORT||LA121_3==SUPER||LA121_3==THIS||LA121_3==VOID||LA121_3==LPAREN||(LA121_3>=EQ && LA121_3<=TILDE)||(LA121_3>=PLUSPLUS && LA121_3<=SUB)) ) {
                        alt121=1;
                    }


                }


                } finally {dbg.exitDecision(121);}

                switch (alt121) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:1021:10: relationalOp shiftExpression
		    {
		    dbg.location(1021,10);
		    pushFollow(FOLLOW_relationalOp_in_relationalExpression5669);
		    relationalOp();

		    state._fsp--;
		    if (state.failed) return ;
		    dbg.location(1021,23);
		    pushFollow(FOLLOW_shiftExpression_in_relationalExpression5671);
		    shiftExpression();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop121;
                }
            } while (true);
            } finally {dbg.exitSubRule(121);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 80, relationalExpression_StartIndex); }
        }
        dbg.location(1023, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "relationalExpression");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "relationalExpression"


    // $ANTLR start "relationalOp"
    // src/com/google/doclava/parser/Java.g:1025:1: relationalOp : ( '<' '=' | '>' '=' | '<' | '>' );
    public final void relationalOp() throws RecognitionException {
        int relationalOp_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "relationalOp");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1025, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 81) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1026:5: ( '<' '=' | '>' '=' | '<' | '>' )
            int alt122=4;
            try { dbg.enterDecision(122, decisionCanBacktrack[122]);

            int LA122_0 = input.LA(1);

            if ( (LA122_0==LT) ) {
                int LA122_1 = input.LA(2);

                if ( (LA122_1==EQ) ) {
                    alt122=1;
                }
                else if ( ((LA122_1>=IDENTIFIER && LA122_1<=NULL)||LA122_1==BOOLEAN||LA122_1==BYTE||LA122_1==CHAR||LA122_1==DOUBLE||LA122_1==FLOAT||LA122_1==INT||LA122_1==LONG||LA122_1==NEW||LA122_1==SHORT||LA122_1==SUPER||LA122_1==THIS||LA122_1==VOID||LA122_1==LPAREN||(LA122_1>=BANG && LA122_1<=TILDE)||(LA122_1>=PLUSPLUS && LA122_1<=SUB)) ) {
                    alt122=3;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 122, 1, input);

                    dbg.recognitionException(nvae);
                    throw nvae;
                }
            }
            else if ( (LA122_0==GT) ) {
                int LA122_2 = input.LA(2);

                if ( (LA122_2==EQ) ) {
                    alt122=2;
                }
                else if ( ((LA122_2>=IDENTIFIER && LA122_2<=NULL)||LA122_2==BOOLEAN||LA122_2==BYTE||LA122_2==CHAR||LA122_2==DOUBLE||LA122_2==FLOAT||LA122_2==INT||LA122_2==LONG||LA122_2==NEW||LA122_2==SHORT||LA122_2==SUPER||LA122_2==THIS||LA122_2==VOID||LA122_2==LPAREN||(LA122_2>=BANG && LA122_2<=TILDE)||(LA122_2>=PLUSPLUS && LA122_2<=SUB)) ) {
                    alt122=4;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 122, 2, input);

                    dbg.recognitionException(nvae);
                    throw nvae;
                }
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 122, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(122);}

            switch (alt122) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1026:10: '<' '='
                    {
                    dbg.location(1026,10);
                    match(input,LT,FOLLOW_LT_in_relationalOp5702); if (state.failed) return ;
                    dbg.location(1026,14);
                    match(input,EQ,FOLLOW_EQ_in_relationalOp5704); if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:1027:10: '>' '='
                    {
                    dbg.location(1027,10);
                    match(input,GT,FOLLOW_GT_in_relationalOp5715); if (state.failed) return ;
                    dbg.location(1027,14);
                    match(input,EQ,FOLLOW_EQ_in_relationalOp5717); if (state.failed) return ;

                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:1028:9: '<'
                    {
                    dbg.location(1028,9);
                    match(input,LT,FOLLOW_LT_in_relationalOp5727); if (state.failed) return ;

                    }
                    break;
                case 4 :
                    dbg.enterAlt(4);

                    // src/com/google/doclava/parser/Java.g:1029:9: '>'
                    {
                    dbg.location(1029,9);
                    match(input,GT,FOLLOW_GT_in_relationalOp5737); if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 81, relationalOp_StartIndex); }
        }
        dbg.location(1030, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "relationalOp");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "relationalOp"


    // $ANTLR start "shiftExpression"
    // src/com/google/doclava/parser/Java.g:1032:1: shiftExpression : additiveExpression ( shiftOp additiveExpression )* ;
    public final void shiftExpression() throws RecognitionException {
        int shiftExpression_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "shiftExpression");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1032, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 82) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1033:5: ( additiveExpression ( shiftOp additiveExpression )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1033:9: additiveExpression ( shiftOp additiveExpression )*
            {
            dbg.location(1033,9);
            pushFollow(FOLLOW_additiveExpression_in_shiftExpression5756);
            additiveExpression();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1034,9);
            // src/com/google/doclava/parser/Java.g:1034:9: ( shiftOp additiveExpression )*
            try { dbg.enterSubRule(123);

            loop123:
            do {
                int alt123=2;
                try { dbg.enterDecision(123, decisionCanBacktrack[123]);

                int LA123_0 = input.LA(1);

                if ( (LA123_0==LT) ) {
                    int LA123_1 = input.LA(2);

                    if ( (LA123_1==LT) ) {
                        int LA123_4 = input.LA(3);

                        if ( ((LA123_4>=IDENTIFIER && LA123_4<=NULL)||LA123_4==BOOLEAN||LA123_4==BYTE||LA123_4==CHAR||LA123_4==DOUBLE||LA123_4==FLOAT||LA123_4==INT||LA123_4==LONG||LA123_4==NEW||LA123_4==SHORT||LA123_4==SUPER||LA123_4==THIS||LA123_4==VOID||LA123_4==LPAREN||(LA123_4>=BANG && LA123_4<=TILDE)||(LA123_4>=PLUSPLUS && LA123_4<=SUB)) ) {
                            alt123=1;
                        }


                    }


                }
                else if ( (LA123_0==GT) ) {
                    int LA123_2 = input.LA(2);

                    if ( (LA123_2==GT) ) {
                        int LA123_5 = input.LA(3);

                        if ( (LA123_5==GT) ) {
                            int LA123_7 = input.LA(4);

                            if ( ((LA123_7>=IDENTIFIER && LA123_7<=NULL)||LA123_7==BOOLEAN||LA123_7==BYTE||LA123_7==CHAR||LA123_7==DOUBLE||LA123_7==FLOAT||LA123_7==INT||LA123_7==LONG||LA123_7==NEW||LA123_7==SHORT||LA123_7==SUPER||LA123_7==THIS||LA123_7==VOID||LA123_7==LPAREN||(LA123_7>=BANG && LA123_7<=TILDE)||(LA123_7>=PLUSPLUS && LA123_7<=SUB)) ) {
                                alt123=1;
                            }


                        }
                        else if ( ((LA123_5>=IDENTIFIER && LA123_5<=NULL)||LA123_5==BOOLEAN||LA123_5==BYTE||LA123_5==CHAR||LA123_5==DOUBLE||LA123_5==FLOAT||LA123_5==INT||LA123_5==LONG||LA123_5==NEW||LA123_5==SHORT||LA123_5==SUPER||LA123_5==THIS||LA123_5==VOID||LA123_5==LPAREN||(LA123_5>=BANG && LA123_5<=TILDE)||(LA123_5>=PLUSPLUS && LA123_5<=SUB)) ) {
                            alt123=1;
                        }


                    }


                }


                } finally {dbg.exitDecision(123);}

                switch (alt123) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:1034:10: shiftOp additiveExpression
		    {
		    dbg.location(1034,10);
		    pushFollow(FOLLOW_shiftOp_in_shiftExpression5767);
		    shiftOp();

		    state._fsp--;
		    if (state.failed) return ;
		    dbg.location(1034,18);
		    pushFollow(FOLLOW_additiveExpression_in_shiftExpression5769);
		    additiveExpression();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop123;
                }
            } while (true);
            } finally {dbg.exitSubRule(123);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 82, shiftExpression_StartIndex); }
        }
        dbg.location(1036, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "shiftExpression");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "shiftExpression"


    // $ANTLR start "shiftOp"
    // src/com/google/doclava/parser/Java.g:1039:1: shiftOp : ( '<' '<' | '>' '>' '>' | '>' '>' );
    public final void shiftOp() throws RecognitionException {
        int shiftOp_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "shiftOp");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1039, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 83) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1040:5: ( '<' '<' | '>' '>' '>' | '>' '>' )
            int alt124=3;
            try { dbg.enterDecision(124, decisionCanBacktrack[124]);

            int LA124_0 = input.LA(1);

            if ( (LA124_0==LT) ) {
                alt124=1;
            }
            else if ( (LA124_0==GT) ) {
                int LA124_2 = input.LA(2);

                if ( (LA124_2==GT) ) {
                    int LA124_3 = input.LA(3);

                    if ( (LA124_3==GT) ) {
                        alt124=2;
                    }
                    else if ( ((LA124_3>=IDENTIFIER && LA124_3<=NULL)||LA124_3==BOOLEAN||LA124_3==BYTE||LA124_3==CHAR||LA124_3==DOUBLE||LA124_3==FLOAT||LA124_3==INT||LA124_3==LONG||LA124_3==NEW||LA124_3==SHORT||LA124_3==SUPER||LA124_3==THIS||LA124_3==VOID||LA124_3==LPAREN||(LA124_3>=BANG && LA124_3<=TILDE)||(LA124_3>=PLUSPLUS && LA124_3<=SUB)) ) {
                        alt124=3;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return ;}
                        NoViableAltException nvae =
                            new NoViableAltException("", 124, 3, input);

                        dbg.recognitionException(nvae);
                        throw nvae;
                    }
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 124, 2, input);

                    dbg.recognitionException(nvae);
                    throw nvae;
                }
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 124, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(124);}

            switch (alt124) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1040:10: '<' '<'
                    {
                    dbg.location(1040,10);
                    match(input,LT,FOLLOW_LT_in_shiftOp5801); if (state.failed) return ;
                    dbg.location(1040,14);
                    match(input,LT,FOLLOW_LT_in_shiftOp5803); if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:1041:10: '>' '>' '>'
                    {
                    dbg.location(1041,10);
                    match(input,GT,FOLLOW_GT_in_shiftOp5814); if (state.failed) return ;
                    dbg.location(1041,14);
                    match(input,GT,FOLLOW_GT_in_shiftOp5816); if (state.failed) return ;
                    dbg.location(1041,18);
                    match(input,GT,FOLLOW_GT_in_shiftOp5818); if (state.failed) return ;

                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:1042:10: '>' '>'
                    {
                    dbg.location(1042,10);
                    match(input,GT,FOLLOW_GT_in_shiftOp5829); if (state.failed) return ;
                    dbg.location(1042,14);
                    match(input,GT,FOLLOW_GT_in_shiftOp5831); if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 83, shiftOp_StartIndex); }
        }
        dbg.location(1043, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "shiftOp");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "shiftOp"


    // $ANTLR start "additiveExpression"
    // src/com/google/doclava/parser/Java.g:1046:1: additiveExpression : multiplicativeExpression ( ( '+' | '-' ) multiplicativeExpression )* ;
    public final void additiveExpression() throws RecognitionException {
        int additiveExpression_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "additiveExpression");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1046, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 84) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1047:5: ( multiplicativeExpression ( ( '+' | '-' ) multiplicativeExpression )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1047:9: multiplicativeExpression ( ( '+' | '-' ) multiplicativeExpression )*
            {
            dbg.location(1047,9);
            pushFollow(FOLLOW_multiplicativeExpression_in_additiveExpression5851);
            multiplicativeExpression();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1048,9);
            // src/com/google/doclava/parser/Java.g:1048:9: ( ( '+' | '-' ) multiplicativeExpression )*
            try { dbg.enterSubRule(125);

            loop125:
            do {
                int alt125=2;
                try { dbg.enterDecision(125, decisionCanBacktrack[125]);

                int LA125_0 = input.LA(1);

                if ( ((LA125_0>=PLUS && LA125_0<=SUB)) ) {
                    alt125=1;
                }


                } finally {dbg.exitDecision(125);}

                switch (alt125) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:1049:13: ( '+' | '-' ) multiplicativeExpression
		    {
		    dbg.location(1049,13);
		    if ( (input.LA(1)>=PLUS && input.LA(1)<=SUB) ) {
		        input.consume();
		        state.errorRecovery=false;state.failed=false;
		    }
		    else {
		        if (state.backtracking>0) {state.failed=true; return ;}
		        MismatchedSetException mse = new MismatchedSetException(null,input);
		        dbg.recognitionException(mse);
		        throw mse;
		    }

		    dbg.location(1052,13);
		    pushFollow(FOLLOW_multiplicativeExpression_in_additiveExpression5925);
		    multiplicativeExpression();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop125;
                }
            } while (true);
            } finally {dbg.exitSubRule(125);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 84, additiveExpression_StartIndex); }
        }
        dbg.location(1054, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "additiveExpression");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "additiveExpression"


    // $ANTLR start "multiplicativeExpression"
    // src/com/google/doclava/parser/Java.g:1056:1: multiplicativeExpression : unaryExpression ( ( '*' | '/' | '%' ) unaryExpression )* ;
    public final void multiplicativeExpression() throws RecognitionException {
        int multiplicativeExpression_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "multiplicativeExpression");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1056, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 85) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1057:5: ( unaryExpression ( ( '*' | '/' | '%' ) unaryExpression )* )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1058:9: unaryExpression ( ( '*' | '/' | '%' ) unaryExpression )*
            {
            dbg.location(1058,9);
            pushFollow(FOLLOW_unaryExpression_in_multiplicativeExpression5962);
            unaryExpression();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1059,9);
            // src/com/google/doclava/parser/Java.g:1059:9: ( ( '*' | '/' | '%' ) unaryExpression )*
            try { dbg.enterSubRule(126);

            loop126:
            do {
                int alt126=2;
                try { dbg.enterDecision(126, decisionCanBacktrack[126]);

                int LA126_0 = input.LA(1);

                if ( ((LA126_0>=STAR && LA126_0<=SLASH)||LA126_0==PERCENT) ) {
                    alt126=1;
                }


                } finally {dbg.exitDecision(126);}

                switch (alt126) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:1060:13: ( '*' | '/' | '%' ) unaryExpression
		    {
		    dbg.location(1060,13);
		    if ( (input.LA(1)>=STAR && input.LA(1)<=SLASH)||input.LA(1)==PERCENT ) {
		        input.consume();
		        state.errorRecovery=false;state.failed=false;
		    }
		    else {
		        if (state.backtracking>0) {state.failed=true; return ;}
		        MismatchedSetException mse = new MismatchedSetException(null,input);
		        dbg.recognitionException(mse);
		        throw mse;
		    }

		    dbg.location(1064,13);
		    pushFollow(FOLLOW_unaryExpression_in_multiplicativeExpression6054);
		    unaryExpression();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop126;
                }
            } while (true);
            } finally {dbg.exitSubRule(126);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 85, multiplicativeExpression_StartIndex); }
        }
        dbg.location(1066, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "multiplicativeExpression");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "multiplicativeExpression"


    // $ANTLR start "unaryExpression"
    // src/com/google/doclava/parser/Java.g:1068:1: unaryExpression : ( '+' unaryExpression | '-' unaryExpression | '++' unaryExpression | '--' unaryExpression | unaryExpressionNotPlusMinus );
    public final void unaryExpression() throws RecognitionException {
        int unaryExpression_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "unaryExpression");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1068, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 86) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1073:5: ( '+' unaryExpression | '-' unaryExpression | '++' unaryExpression | '--' unaryExpression | unaryExpressionNotPlusMinus )
            int alt127=5;
            try { dbg.enterDecision(127, decisionCanBacktrack[127]);

            switch ( input.LA(1) ) {
            case PLUS:
                {
                alt127=1;
                }
                break;
            case SUB:
                {
                alt127=2;
                }
                break;
            case PLUSPLUS:
                {
                alt127=3;
                }
                break;
            case SUBSUB:
                {
                alt127=4;
                }
                break;
            case IDENTIFIER:
            case INTLITERAL:
            case LONGLITERAL:
            case FLOATLITERAL:
            case DOUBLELITERAL:
            case CHARLITERAL:
            case STRINGLITERAL:
            case TRUE:
            case FALSE:
            case NULL:
            case BOOLEAN:
            case BYTE:
            case CHAR:
            case DOUBLE:
            case FLOAT:
            case INT:
            case LONG:
            case NEW:
            case SHORT:
            case SUPER:
            case THIS:
            case VOID:
            case LPAREN:
            case BANG:
            case TILDE:
                {
                alt127=5;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 127, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }

            } finally {dbg.exitDecision(127);}

            switch (alt127) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1073:9: '+' unaryExpression
                    {
                    dbg.location(1073,9);
                    match(input,PLUS,FOLLOW_PLUS_in_unaryExpression6086); if (state.failed) return ;
                    dbg.location(1073,14);
                    pushFollow(FOLLOW_unaryExpression_in_unaryExpression6089);
                    unaryExpression();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:1074:9: '-' unaryExpression
                    {
                    dbg.location(1074,9);
                    match(input,SUB,FOLLOW_SUB_in_unaryExpression6099); if (state.failed) return ;
                    dbg.location(1074,13);
                    pushFollow(FOLLOW_unaryExpression_in_unaryExpression6101);
                    unaryExpression();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:1075:9: '++' unaryExpression
                    {
                    dbg.location(1075,9);
                    match(input,PLUSPLUS,FOLLOW_PLUSPLUS_in_unaryExpression6111); if (state.failed) return ;
                    dbg.location(1075,14);
                    pushFollow(FOLLOW_unaryExpression_in_unaryExpression6113);
                    unaryExpression();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 4 :
                    dbg.enterAlt(4);

                    // src/com/google/doclava/parser/Java.g:1076:9: '--' unaryExpression
                    {
                    dbg.location(1076,9);
                    match(input,SUBSUB,FOLLOW_SUBSUB_in_unaryExpression6123); if (state.failed) return ;
                    dbg.location(1076,14);
                    pushFollow(FOLLOW_unaryExpression_in_unaryExpression6125);
                    unaryExpression();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 5 :
                    dbg.enterAlt(5);

                    // src/com/google/doclava/parser/Java.g:1077:9: unaryExpressionNotPlusMinus
                    {
                    dbg.location(1077,9);
                    pushFollow(FOLLOW_unaryExpressionNotPlusMinus_in_unaryExpression6135);
                    unaryExpressionNotPlusMinus();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 86, unaryExpression_StartIndex); }
        }
        dbg.location(1078, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "unaryExpression");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "unaryExpression"


    // $ANTLR start "unaryExpressionNotPlusMinus"
    // src/com/google/doclava/parser/Java.g:1080:1: unaryExpressionNotPlusMinus : ( '~' unaryExpression | '!' unaryExpression | castExpression | primary ( selector )* ( '++' | '--' )? );
    public final void unaryExpressionNotPlusMinus() throws RecognitionException {
        int unaryExpressionNotPlusMinus_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "unaryExpressionNotPlusMinus");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1080, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 87) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1081:5: ( '~' unaryExpression | '!' unaryExpression | castExpression | primary ( selector )* ( '++' | '--' )? )
            int alt130=4;
            try { dbg.enterDecision(130, decisionCanBacktrack[130]);

            try {
                isCyclicDecision = true;
                alt130 = dfa130.predict(input);
            }
            catch (NoViableAltException nvae) {
                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(130);}

            switch (alt130) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1081:9: '~' unaryExpression
                    {
                    dbg.location(1081,9);
                    match(input,TILDE,FOLLOW_TILDE_in_unaryExpressionNotPlusMinus6154); if (state.failed) return ;
                    dbg.location(1081,13);
                    pushFollow(FOLLOW_unaryExpression_in_unaryExpressionNotPlusMinus6156);
                    unaryExpression();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:1082:9: '!' unaryExpression
                    {
                    dbg.location(1082,9);
                    match(input,BANG,FOLLOW_BANG_in_unaryExpressionNotPlusMinus6166); if (state.failed) return ;
                    dbg.location(1082,13);
                    pushFollow(FOLLOW_unaryExpression_in_unaryExpressionNotPlusMinus6168);
                    unaryExpression();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:1083:9: castExpression
                    {
                    dbg.location(1083,9);
                    pushFollow(FOLLOW_castExpression_in_unaryExpressionNotPlusMinus6178);
                    castExpression();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 4 :
                    dbg.enterAlt(4);

                    // src/com/google/doclava/parser/Java.g:1084:9: primary ( selector )* ( '++' | '--' )?
                    {
                    dbg.location(1084,9);
                    pushFollow(FOLLOW_primary_in_unaryExpressionNotPlusMinus6188);
                    primary();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(1085,9);
                    // src/com/google/doclava/parser/Java.g:1085:9: ( selector )*
                    try { dbg.enterSubRule(128);

                    loop128:
                    do {
                        int alt128=2;
                        try { dbg.enterDecision(128, decisionCanBacktrack[128]);

                        int LA128_0 = input.LA(1);

                        if ( (LA128_0==LBRACKET||LA128_0==DOT) ) {
                            alt128=1;
                        }


                        } finally {dbg.exitDecision(128);}

                        switch (alt128) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:1085:10: selector
			    {
			    dbg.location(1085,10);
			    pushFollow(FOLLOW_selector_in_unaryExpressionNotPlusMinus6199);
			    selector();

			    state._fsp--;
			    if (state.failed) return ;

			    }
			    break;

			default :
			    break loop128;
                        }
                    } while (true);
                    } finally {dbg.exitSubRule(128);}

                    dbg.location(1087,9);
                    // src/com/google/doclava/parser/Java.g:1087:9: ( '++' | '--' )?
                    int alt129=2;
                    try { dbg.enterSubRule(129);
                    try { dbg.enterDecision(129, decisionCanBacktrack[129]);

                    int LA129_0 = input.LA(1);

                    if ( ((LA129_0>=PLUSPLUS && LA129_0<=SUBSUB)) ) {
                        alt129=1;
                    }
                    } finally {dbg.exitDecision(129);}

                    switch (alt129) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:
                            {
                            dbg.location(1087,9);
                            if ( (input.LA(1)>=PLUSPLUS && input.LA(1)<=SUBSUB) ) {
                                input.consume();
                                state.errorRecovery=false;state.failed=false;
                            }
                            else {
                                if (state.backtracking>0) {state.failed=true; return ;}
                                MismatchedSetException mse = new MismatchedSetException(null,input);
                                dbg.recognitionException(mse);
                                throw mse;
                            }


                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(129);}


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 87, unaryExpressionNotPlusMinus_StartIndex); }
        }
        dbg.location(1090, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "unaryExpressionNotPlusMinus");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "unaryExpressionNotPlusMinus"


    // $ANTLR start "castExpression"
    // src/com/google/doclava/parser/Java.g:1092:1: castExpression : ( '(' primitiveType ')' unaryExpression | '(' type ')' unaryExpressionNotPlusMinus );
    public final void castExpression() throws RecognitionException {
        int castExpression_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "castExpression");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1092, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 88) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1093:5: ( '(' primitiveType ')' unaryExpression | '(' type ')' unaryExpressionNotPlusMinus )
            int alt131=2;
            try { dbg.enterDecision(131, decisionCanBacktrack[131]);

            int LA131_0 = input.LA(1);

            if ( (LA131_0==LPAREN) ) {
                int LA131_1 = input.LA(2);

                if ( (synpred206_Java()) ) {
                    alt131=1;
                }
                else if ( (true) ) {
                    alt131=2;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 131, 1, input);

                    dbg.recognitionException(nvae);
                    throw nvae;
                }
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 131, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(131);}

            switch (alt131) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1093:9: '(' primitiveType ')' unaryExpression
                    {
                    dbg.location(1093,9);
                    match(input,LPAREN,FOLLOW_LPAREN_in_castExpression6268); if (state.failed) return ;
                    dbg.location(1093,13);
                    pushFollow(FOLLOW_primitiveType_in_castExpression6270);
                    primitiveType();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(1093,27);
                    match(input,RPAREN,FOLLOW_RPAREN_in_castExpression6272); if (state.failed) return ;
                    dbg.location(1093,31);
                    pushFollow(FOLLOW_unaryExpression_in_castExpression6274);
                    unaryExpression();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:1094:9: '(' type ')' unaryExpressionNotPlusMinus
                    {
                    dbg.location(1094,9);
                    match(input,LPAREN,FOLLOW_LPAREN_in_castExpression6284); if (state.failed) return ;
                    dbg.location(1094,13);
                    pushFollow(FOLLOW_type_in_castExpression6286);
                    type();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(1094,18);
                    match(input,RPAREN,FOLLOW_RPAREN_in_castExpression6288); if (state.failed) return ;
                    dbg.location(1094,22);
                    pushFollow(FOLLOW_unaryExpressionNotPlusMinus_in_castExpression6290);
                    unaryExpressionNotPlusMinus();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 88, castExpression_StartIndex); }
        }
        dbg.location(1095, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "castExpression");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "castExpression"


    // $ANTLR start "primary"
    // src/com/google/doclava/parser/Java.g:1097:1: primary : ( parExpression | 'this' ( '.' IDENTIFIER )* ( identifierSuffix )? | IDENTIFIER ( '.' IDENTIFIER )* ( identifierSuffix )? | 'super' superSuffix | literal | creator | primitiveType ( '[' ']' )* '.' 'class' | 'void' '.' 'class' );
    public final void primary() throws RecognitionException {
        int primary_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "primary");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1097, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 89) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1101:5: ( parExpression | 'this' ( '.' IDENTIFIER )* ( identifierSuffix )? | IDENTIFIER ( '.' IDENTIFIER )* ( identifierSuffix )? | 'super' superSuffix | literal | creator | primitiveType ( '[' ']' )* '.' 'class' | 'void' '.' 'class' )
            int alt137=8;
            try { dbg.enterDecision(137, decisionCanBacktrack[137]);

            switch ( input.LA(1) ) {
            case LPAREN:
                {
                alt137=1;
                }
                break;
            case THIS:
                {
                alt137=2;
                }
                break;
            case IDENTIFIER:
                {
                alt137=3;
                }
                break;
            case SUPER:
                {
                alt137=4;
                }
                break;
            case INTLITERAL:
            case LONGLITERAL:
            case FLOATLITERAL:
            case DOUBLELITERAL:
            case CHARLITERAL:
            case STRINGLITERAL:
            case TRUE:
            case FALSE:
            case NULL:
                {
                alt137=5;
                }
                break;
            case NEW:
                {
                alt137=6;
                }
                break;
            case BOOLEAN:
            case BYTE:
            case CHAR:
            case DOUBLE:
            case FLOAT:
            case INT:
            case LONG:
            case SHORT:
                {
                alt137=7;
                }
                break;
            case VOID:
                {
                alt137=8;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 137, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }

            } finally {dbg.exitDecision(137);}

            switch (alt137) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1101:9: parExpression
                    {
                    dbg.location(1101,9);
                    pushFollow(FOLLOW_parExpression_in_primary6311);
                    parExpression();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:1102:9: 'this' ( '.' IDENTIFIER )* ( identifierSuffix )?
                    {
                    dbg.location(1102,9);
                    match(input,THIS,FOLLOW_THIS_in_primary6321); if (state.failed) return ;
                    dbg.location(1103,9);
                    // src/com/google/doclava/parser/Java.g:1103:9: ( '.' IDENTIFIER )*
                    try { dbg.enterSubRule(132);

                    loop132:
                    do {
                        int alt132=2;
                        try { dbg.enterDecision(132, decisionCanBacktrack[132]);

                        int LA132_0 = input.LA(1);

                        if ( (LA132_0==DOT) ) {
                            int LA132_2 = input.LA(2);

                            if ( (LA132_2==IDENTIFIER) ) {
                                int LA132_3 = input.LA(3);

                                if ( (synpred208_Java()) ) {
                                    alt132=1;
                                }


                            }


                        }


                        } finally {dbg.exitDecision(132);}

                        switch (alt132) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:1103:10: '.' IDENTIFIER
			    {
			    dbg.location(1103,10);
			    match(input,DOT,FOLLOW_DOT_in_primary6332); if (state.failed) return ;
			    dbg.location(1103,14);
			    match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_primary6334); if (state.failed) return ;

			    }
			    break;

			default :
			    break loop132;
                        }
                    } while (true);
                    } finally {dbg.exitSubRule(132);}

                    dbg.location(1105,9);
                    // src/com/google/doclava/parser/Java.g:1105:9: ( identifierSuffix )?
                    int alt133=2;
                    try { dbg.enterSubRule(133);
                    try { dbg.enterDecision(133, decisionCanBacktrack[133]);

                    try {
                        isCyclicDecision = true;
                        alt133 = dfa133.predict(input);
                    }
                    catch (NoViableAltException nvae) {
                        dbg.recognitionException(nvae);
                        throw nvae;
                    }
                    } finally {dbg.exitDecision(133);}

                    switch (alt133) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:1105:10: identifierSuffix
                            {
                            dbg.location(1105,10);
                            pushFollow(FOLLOW_identifierSuffix_in_primary6356);
                            identifierSuffix();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(133);}


                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:1107:9: IDENTIFIER ( '.' IDENTIFIER )* ( identifierSuffix )?
                    {
                    dbg.location(1107,9);
                    match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_primary6377); if (state.failed) return ;
                    dbg.location(1108,9);
                    // src/com/google/doclava/parser/Java.g:1108:9: ( '.' IDENTIFIER )*
                    try { dbg.enterSubRule(134);

                    loop134:
                    do {
                        int alt134=2;
                        try { dbg.enterDecision(134, decisionCanBacktrack[134]);

                        int LA134_0 = input.LA(1);

                        if ( (LA134_0==DOT) ) {
                            int LA134_2 = input.LA(2);

                            if ( (LA134_2==IDENTIFIER) ) {
                                int LA134_3 = input.LA(3);

                                if ( (synpred211_Java()) ) {
                                    alt134=1;
                                }


                            }


                        }


                        } finally {dbg.exitDecision(134);}

                        switch (alt134) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:1108:10: '.' IDENTIFIER
			    {
			    dbg.location(1108,10);
			    match(input,DOT,FOLLOW_DOT_in_primary6388); if (state.failed) return ;
			    dbg.location(1108,14);
			    match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_primary6390); if (state.failed) return ;

			    }
			    break;

			default :
			    break loop134;
                        }
                    } while (true);
                    } finally {dbg.exitSubRule(134);}

                    dbg.location(1110,9);
                    // src/com/google/doclava/parser/Java.g:1110:9: ( identifierSuffix )?
                    int alt135=2;
                    try { dbg.enterSubRule(135);
                    try { dbg.enterDecision(135, decisionCanBacktrack[135]);

                    try {
                        isCyclicDecision = true;
                        alt135 = dfa135.predict(input);
                    }
                    catch (NoViableAltException nvae) {
                        dbg.recognitionException(nvae);
                        throw nvae;
                    }
                    } finally {dbg.exitDecision(135);}

                    switch (alt135) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:1110:10: identifierSuffix
                            {
                            dbg.location(1110,10);
                            pushFollow(FOLLOW_identifierSuffix_in_primary6412);
                            identifierSuffix();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(135);}


                    }
                    break;
                case 4 :
                    dbg.enterAlt(4);

                    // src/com/google/doclava/parser/Java.g:1112:9: 'super' superSuffix
                    {
                    dbg.location(1112,9);
                    match(input,SUPER,FOLLOW_SUPER_in_primary6433); if (state.failed) return ;
                    dbg.location(1113,9);
                    pushFollow(FOLLOW_superSuffix_in_primary6443);
                    superSuffix();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 5 :
                    dbg.enterAlt(5);

                    // src/com/google/doclava/parser/Java.g:1114:9: literal
                    {
                    dbg.location(1114,9);
                    pushFollow(FOLLOW_literal_in_primary6453);
                    literal();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 6 :
                    dbg.enterAlt(6);

                    // src/com/google/doclava/parser/Java.g:1115:9: creator
                    {
                    dbg.location(1115,9);
                    pushFollow(FOLLOW_creator_in_primary6463);
                    creator();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 7 :
                    dbg.enterAlt(7);

                    // src/com/google/doclava/parser/Java.g:1116:9: primitiveType ( '[' ']' )* '.' 'class'
                    {
                    dbg.location(1116,9);
                    pushFollow(FOLLOW_primitiveType_in_primary6473);
                    primitiveType();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(1117,9);
                    // src/com/google/doclava/parser/Java.g:1117:9: ( '[' ']' )*
                    try { dbg.enterSubRule(136);

                    loop136:
                    do {
                        int alt136=2;
                        try { dbg.enterDecision(136, decisionCanBacktrack[136]);

                        int LA136_0 = input.LA(1);

                        if ( (LA136_0==LBRACKET) ) {
                            alt136=1;
                        }


                        } finally {dbg.exitDecision(136);}

                        switch (alt136) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:1117:10: '[' ']'
			    {
			    dbg.location(1117,10);
			    match(input,LBRACKET,FOLLOW_LBRACKET_in_primary6484); if (state.failed) return ;
			    dbg.location(1117,14);
			    match(input,RBRACKET,FOLLOW_RBRACKET_in_primary6486); if (state.failed) return ;

			    }
			    break;

			default :
			    break loop136;
                        }
                    } while (true);
                    } finally {dbg.exitSubRule(136);}

                    dbg.location(1119,9);
                    match(input,DOT,FOLLOW_DOT_in_primary6507); if (state.failed) return ;
                    dbg.location(1119,13);
                    match(input,CLASS,FOLLOW_CLASS_in_primary6509); if (state.failed) return ;

                    }
                    break;
                case 8 :
                    dbg.enterAlt(8);

                    // src/com/google/doclava/parser/Java.g:1120:9: 'void' '.' 'class'
                    {
                    dbg.location(1120,9);
                    match(input,VOID,FOLLOW_VOID_in_primary6519); if (state.failed) return ;
                    dbg.location(1120,16);
                    match(input,DOT,FOLLOW_DOT_in_primary6521); if (state.failed) return ;
                    dbg.location(1120,20);
                    match(input,CLASS,FOLLOW_CLASS_in_primary6523); if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 89, primary_StartIndex); }
        }
        dbg.location(1121, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "primary");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "primary"


    // $ANTLR start "superSuffix"
    // src/com/google/doclava/parser/Java.g:1124:1: superSuffix : ( arguments | '.' ( typeArguments )? IDENTIFIER ( arguments )? );
    public final void superSuffix() throws RecognitionException {
        int superSuffix_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "superSuffix");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1124, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 90) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1125:5: ( arguments | '.' ( typeArguments )? IDENTIFIER ( arguments )? )
            int alt140=2;
            try { dbg.enterDecision(140, decisionCanBacktrack[140]);

            int LA140_0 = input.LA(1);

            if ( (LA140_0==LPAREN) ) {
                alt140=1;
            }
            else if ( (LA140_0==DOT) ) {
                alt140=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 140, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(140);}

            switch (alt140) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1125:9: arguments
                    {
                    dbg.location(1125,9);
                    pushFollow(FOLLOW_arguments_in_superSuffix6543);
                    arguments();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:1126:9: '.' ( typeArguments )? IDENTIFIER ( arguments )?
                    {
                    dbg.location(1126,9);
                    match(input,DOT,FOLLOW_DOT_in_superSuffix6553); if (state.failed) return ;
                    dbg.location(1126,13);
                    // src/com/google/doclava/parser/Java.g:1126:13: ( typeArguments )?
                    int alt138=2;
                    try { dbg.enterSubRule(138);
                    try { dbg.enterDecision(138, decisionCanBacktrack[138]);

                    int LA138_0 = input.LA(1);

                    if ( (LA138_0==LT) ) {
                        alt138=1;
                    }
                    } finally {dbg.exitDecision(138);}

                    switch (alt138) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:1126:14: typeArguments
                            {
                            dbg.location(1126,14);
                            pushFollow(FOLLOW_typeArguments_in_superSuffix6556);
                            typeArguments();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(138);}

                    dbg.location(1128,9);
                    match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_superSuffix6577); if (state.failed) return ;
                    dbg.location(1129,9);
                    // src/com/google/doclava/parser/Java.g:1129:9: ( arguments )?
                    int alt139=2;
                    try { dbg.enterSubRule(139);
                    try { dbg.enterDecision(139, decisionCanBacktrack[139]);

                    int LA139_0 = input.LA(1);

                    if ( (LA139_0==LPAREN) ) {
                        alt139=1;
                    }
                    } finally {dbg.exitDecision(139);}

                    switch (alt139) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:1129:10: arguments
                            {
                            dbg.location(1129,10);
                            pushFollow(FOLLOW_arguments_in_superSuffix6588);
                            arguments();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(139);}


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 90, superSuffix_StartIndex); }
        }
        dbg.location(1131, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "superSuffix");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "superSuffix"


    // $ANTLR start "identifierSuffix"
    // src/com/google/doclava/parser/Java.g:1134:1: identifierSuffix : ( ( '[' ']' )+ '.' 'class' | ( '[' expression ']' )+ | arguments | '.' 'class' | '.' nonWildcardTypeArguments IDENTIFIER arguments | '.' 'this' | '.' 'super' arguments | innerCreator );
    public final void identifierSuffix() throws RecognitionException {
        int identifierSuffix_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "identifierSuffix");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1134, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 91) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1135:5: ( ( '[' ']' )+ '.' 'class' | ( '[' expression ']' )+ | arguments | '.' 'class' | '.' nonWildcardTypeArguments IDENTIFIER arguments | '.' 'this' | '.' 'super' arguments | innerCreator )
            int alt143=8;
            try { dbg.enterDecision(143, decisionCanBacktrack[143]);

            try {
                isCyclicDecision = true;
                alt143 = dfa143.predict(input);
            }
            catch (NoViableAltException nvae) {
                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(143);}

            switch (alt143) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1135:9: ( '[' ']' )+ '.' 'class'
                    {
                    dbg.location(1135,9);
                    // src/com/google/doclava/parser/Java.g:1135:9: ( '[' ']' )+
                    int cnt141=0;
                    try { dbg.enterSubRule(141);

                    loop141:
                    do {
                        int alt141=2;
                        try { dbg.enterDecision(141, decisionCanBacktrack[141]);

                        int LA141_0 = input.LA(1);

                        if ( (LA141_0==LBRACKET) ) {
                            alt141=1;
                        }


                        } finally {dbg.exitDecision(141);}

                        switch (alt141) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:1135:10: '[' ']'
			    {
			    dbg.location(1135,10);
			    match(input,LBRACKET,FOLLOW_LBRACKET_in_identifierSuffix6620); if (state.failed) return ;
			    dbg.location(1135,14);
			    match(input,RBRACKET,FOLLOW_RBRACKET_in_identifierSuffix6622); if (state.failed) return ;

			    }
			    break;

			default :
			    if ( cnt141 >= 1 ) break loop141;
			    if (state.backtracking>0) {state.failed=true; return ;}
                                EarlyExitException eee =
                                    new EarlyExitException(141, input);
                                dbg.recognitionException(eee);

                                throw eee;
                        }
                        cnt141++;
                    } while (true);
                    } finally {dbg.exitSubRule(141);}

                    dbg.location(1137,9);
                    match(input,DOT,FOLLOW_DOT_in_identifierSuffix6643); if (state.failed) return ;
                    dbg.location(1137,13);
                    match(input,CLASS,FOLLOW_CLASS_in_identifierSuffix6645); if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:1138:9: ( '[' expression ']' )+
                    {
                    dbg.location(1138,9);
                    // src/com/google/doclava/parser/Java.g:1138:9: ( '[' expression ']' )+
                    int cnt142=0;
                    try { dbg.enterSubRule(142);

                    loop142:
                    do {
                        int alt142=2;
                        try { dbg.enterDecision(142, decisionCanBacktrack[142]);

                        try {
                            isCyclicDecision = true;
                            alt142 = dfa142.predict(input);
                        }
                        catch (NoViableAltException nvae) {
                            dbg.recognitionException(nvae);
                            throw nvae;
                        }
                        } finally {dbg.exitDecision(142);}

                        switch (alt142) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:1138:10: '[' expression ']'
			    {
			    dbg.location(1138,10);
			    match(input,LBRACKET,FOLLOW_LBRACKET_in_identifierSuffix6656); if (state.failed) return ;
			    dbg.location(1138,14);
			    pushFollow(FOLLOW_expression_in_identifierSuffix6658);
			    expression();

			    state._fsp--;
			    if (state.failed) return ;
			    dbg.location(1138,25);
			    match(input,RBRACKET,FOLLOW_RBRACKET_in_identifierSuffix6660); if (state.failed) return ;

			    }
			    break;

			default :
			    if ( cnt142 >= 1 ) break loop142;
			    if (state.backtracking>0) {state.failed=true; return ;}
                                EarlyExitException eee =
                                    new EarlyExitException(142, input);
                                dbg.recognitionException(eee);

                                throw eee;
                        }
                        cnt142++;
                    } while (true);
                    } finally {dbg.exitSubRule(142);}


                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:1140:9: arguments
                    {
                    dbg.location(1140,9);
                    pushFollow(FOLLOW_arguments_in_identifierSuffix6681);
                    arguments();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 4 :
                    dbg.enterAlt(4);

                    // src/com/google/doclava/parser/Java.g:1141:9: '.' 'class'
                    {
                    dbg.location(1141,9);
                    match(input,DOT,FOLLOW_DOT_in_identifierSuffix6691); if (state.failed) return ;
                    dbg.location(1141,13);
                    match(input,CLASS,FOLLOW_CLASS_in_identifierSuffix6693); if (state.failed) return ;

                    }
                    break;
                case 5 :
                    dbg.enterAlt(5);

                    // src/com/google/doclava/parser/Java.g:1142:9: '.' nonWildcardTypeArguments IDENTIFIER arguments
                    {
                    dbg.location(1142,9);
                    match(input,DOT,FOLLOW_DOT_in_identifierSuffix6703); if (state.failed) return ;
                    dbg.location(1142,13);
                    pushFollow(FOLLOW_nonWildcardTypeArguments_in_identifierSuffix6705);
                    nonWildcardTypeArguments();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(1142,38);
                    match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_identifierSuffix6707); if (state.failed) return ;
                    dbg.location(1142,49);
                    pushFollow(FOLLOW_arguments_in_identifierSuffix6709);
                    arguments();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 6 :
                    dbg.enterAlt(6);

                    // src/com/google/doclava/parser/Java.g:1143:9: '.' 'this'
                    {
                    dbg.location(1143,9);
                    match(input,DOT,FOLLOW_DOT_in_identifierSuffix6719); if (state.failed) return ;
                    dbg.location(1143,13);
                    match(input,THIS,FOLLOW_THIS_in_identifierSuffix6721); if (state.failed) return ;

                    }
                    break;
                case 7 :
                    dbg.enterAlt(7);

                    // src/com/google/doclava/parser/Java.g:1144:9: '.' 'super' arguments
                    {
                    dbg.location(1144,9);
                    match(input,DOT,FOLLOW_DOT_in_identifierSuffix6731); if (state.failed) return ;
                    dbg.location(1144,13);
                    match(input,SUPER,FOLLOW_SUPER_in_identifierSuffix6733); if (state.failed) return ;
                    dbg.location(1144,21);
                    pushFollow(FOLLOW_arguments_in_identifierSuffix6735);
                    arguments();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 8 :
                    dbg.enterAlt(8);

                    // src/com/google/doclava/parser/Java.g:1145:9: innerCreator
                    {
                    dbg.location(1145,9);
                    pushFollow(FOLLOW_innerCreator_in_identifierSuffix6745);
                    innerCreator();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 91, identifierSuffix_StartIndex); }
        }
        dbg.location(1146, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "identifierSuffix");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "identifierSuffix"


    // $ANTLR start "selector"
    // src/com/google/doclava/parser/Java.g:1149:1: selector : ( '.' IDENTIFIER ( arguments )? | '.' 'this' | '.' 'super' superSuffix | innerCreator | '[' expression ']' );
    public final void selector() throws RecognitionException {
        int selector_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "selector");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1149, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 92) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1150:5: ( '.' IDENTIFIER ( arguments )? | '.' 'this' | '.' 'super' superSuffix | innerCreator | '[' expression ']' )
            int alt145=5;
            try { dbg.enterDecision(145, decisionCanBacktrack[145]);

            int LA145_0 = input.LA(1);

            if ( (LA145_0==DOT) ) {
                switch ( input.LA(2) ) {
                case IDENTIFIER:
                    {
                    alt145=1;
                    }
                    break;
                case THIS:
                    {
                    alt145=2;
                    }
                    break;
                case SUPER:
                    {
                    alt145=3;
                    }
                    break;
                case NEW:
                    {
                    alt145=4;
                    }
                    break;
                default:
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 145, 1, input);

                    dbg.recognitionException(nvae);
                    throw nvae;
                }

            }
            else if ( (LA145_0==LBRACKET) ) {
                alt145=5;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 145, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(145);}

            switch (alt145) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1150:9: '.' IDENTIFIER ( arguments )?
                    {
                    dbg.location(1150,9);
                    match(input,DOT,FOLLOW_DOT_in_selector6765); if (state.failed) return ;
                    dbg.location(1150,13);
                    match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_selector6767); if (state.failed) return ;
                    dbg.location(1151,9);
                    // src/com/google/doclava/parser/Java.g:1151:9: ( arguments )?
                    int alt144=2;
                    try { dbg.enterSubRule(144);
                    try { dbg.enterDecision(144, decisionCanBacktrack[144]);

                    int LA144_0 = input.LA(1);

                    if ( (LA144_0==LPAREN) ) {
                        alt144=1;
                    }
                    } finally {dbg.exitDecision(144);}

                    switch (alt144) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:1151:10: arguments
                            {
                            dbg.location(1151,10);
                            pushFollow(FOLLOW_arguments_in_selector6778);
                            arguments();

                            state._fsp--;
                            if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(144);}


                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:1153:9: '.' 'this'
                    {
                    dbg.location(1153,9);
                    match(input,DOT,FOLLOW_DOT_in_selector6799); if (state.failed) return ;
                    dbg.location(1153,13);
                    match(input,THIS,FOLLOW_THIS_in_selector6801); if (state.failed) return ;

                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:1154:9: '.' 'super' superSuffix
                    {
                    dbg.location(1154,9);
                    match(input,DOT,FOLLOW_DOT_in_selector6811); if (state.failed) return ;
                    dbg.location(1154,13);
                    match(input,SUPER,FOLLOW_SUPER_in_selector6813); if (state.failed) return ;
                    dbg.location(1155,9);
                    pushFollow(FOLLOW_superSuffix_in_selector6823);
                    superSuffix();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 4 :
                    dbg.enterAlt(4);

                    // src/com/google/doclava/parser/Java.g:1156:9: innerCreator
                    {
                    dbg.location(1156,9);
                    pushFollow(FOLLOW_innerCreator_in_selector6833);
                    innerCreator();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 5 :
                    dbg.enterAlt(5);

                    // src/com/google/doclava/parser/Java.g:1157:9: '[' expression ']'
                    {
                    dbg.location(1157,9);
                    match(input,LBRACKET,FOLLOW_LBRACKET_in_selector6843); if (state.failed) return ;
                    dbg.location(1157,13);
                    pushFollow(FOLLOW_expression_in_selector6845);
                    expression();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(1157,24);
                    match(input,RBRACKET,FOLLOW_RBRACKET_in_selector6847); if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 92, selector_StartIndex); }
        }
        dbg.location(1158, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "selector");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "selector"


    // $ANTLR start "creator"
    // src/com/google/doclava/parser/Java.g:1160:1: creator : ( 'new' nonWildcardTypeArguments classOrInterfaceType classCreatorRest | 'new' classOrInterfaceType classCreatorRest | arrayCreator );
    public final void creator() throws RecognitionException {
        int creator_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "creator");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1160, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 93) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1161:5: ( 'new' nonWildcardTypeArguments classOrInterfaceType classCreatorRest | 'new' classOrInterfaceType classCreatorRest | arrayCreator )
            int alt146=3;
            try { dbg.enterDecision(146, decisionCanBacktrack[146]);

            int LA146_0 = input.LA(1);

            if ( (LA146_0==NEW) ) {
                int LA146_1 = input.LA(2);

                if ( (synpred236_Java()) ) {
                    alt146=1;
                }
                else if ( (synpred237_Java()) ) {
                    alt146=2;
                }
                else if ( (true) ) {
                    alt146=3;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 146, 1, input);

                    dbg.recognitionException(nvae);
                    throw nvae;
                }
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 146, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(146);}

            switch (alt146) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1161:9: 'new' nonWildcardTypeArguments classOrInterfaceType classCreatorRest
                    {
                    dbg.location(1161,9);
                    match(input,NEW,FOLLOW_NEW_in_creator6866); if (state.failed) return ;
                    dbg.location(1161,15);
                    pushFollow(FOLLOW_nonWildcardTypeArguments_in_creator6868);
                    nonWildcardTypeArguments();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(1161,40);
                    pushFollow(FOLLOW_classOrInterfaceType_in_creator6870);
                    classOrInterfaceType();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(1161,61);
                    pushFollow(FOLLOW_classCreatorRest_in_creator6872);
                    classCreatorRest();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:1162:9: 'new' classOrInterfaceType classCreatorRest
                    {
                    dbg.location(1162,9);
                    match(input,NEW,FOLLOW_NEW_in_creator6882); if (state.failed) return ;
                    dbg.location(1162,15);
                    pushFollow(FOLLOW_classOrInterfaceType_in_creator6884);
                    classOrInterfaceType();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(1162,36);
                    pushFollow(FOLLOW_classCreatorRest_in_creator6886);
                    classCreatorRest();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:1163:9: arrayCreator
                    {
                    dbg.location(1163,9);
                    pushFollow(FOLLOW_arrayCreator_in_creator6896);
                    arrayCreator();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 93, creator_StartIndex); }
        }
        dbg.location(1164, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "creator");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "creator"


    // $ANTLR start "arrayCreator"
    // src/com/google/doclava/parser/Java.g:1166:1: arrayCreator : ( 'new' createdName '[' ']' ( '[' ']' )* arrayInitializer | 'new' createdName '[' expression ']' ( '[' expression ']' )* ( '[' ']' )* );
    public final void arrayCreator() throws RecognitionException {
        int arrayCreator_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "arrayCreator");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1166, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 94) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1167:5: ( 'new' createdName '[' ']' ( '[' ']' )* arrayInitializer | 'new' createdName '[' expression ']' ( '[' expression ']' )* ( '[' ']' )* )
            int alt150=2;
            try { dbg.enterDecision(150, decisionCanBacktrack[150]);

            int LA150_0 = input.LA(1);

            if ( (LA150_0==NEW) ) {
                int LA150_1 = input.LA(2);

                if ( (synpred239_Java()) ) {
                    alt150=1;
                }
                else if ( (true) ) {
                    alt150=2;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 150, 1, input);

                    dbg.recognitionException(nvae);
                    throw nvae;
                }
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 150, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(150);}

            switch (alt150) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1167:9: 'new' createdName '[' ']' ( '[' ']' )* arrayInitializer
                    {
                    dbg.location(1167,9);
                    match(input,NEW,FOLLOW_NEW_in_arrayCreator6915); if (state.failed) return ;
                    dbg.location(1167,15);
                    pushFollow(FOLLOW_createdName_in_arrayCreator6917);
                    createdName();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(1168,9);
                    match(input,LBRACKET,FOLLOW_LBRACKET_in_arrayCreator6927); if (state.failed) return ;
                    dbg.location(1168,13);
                    match(input,RBRACKET,FOLLOW_RBRACKET_in_arrayCreator6929); if (state.failed) return ;
                    dbg.location(1169,9);
                    // src/com/google/doclava/parser/Java.g:1169:9: ( '[' ']' )*
                    try { dbg.enterSubRule(147);

                    loop147:
                    do {
                        int alt147=2;
                        try { dbg.enterDecision(147, decisionCanBacktrack[147]);

                        int LA147_0 = input.LA(1);

                        if ( (LA147_0==LBRACKET) ) {
                            alt147=1;
                        }


                        } finally {dbg.exitDecision(147);}

                        switch (alt147) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:1169:10: '[' ']'
			    {
			    dbg.location(1169,10);
			    match(input,LBRACKET,FOLLOW_LBRACKET_in_arrayCreator6940); if (state.failed) return ;
			    dbg.location(1169,14);
			    match(input,RBRACKET,FOLLOW_RBRACKET_in_arrayCreator6942); if (state.failed) return ;

			    }
			    break;

			default :
			    break loop147;
                        }
                    } while (true);
                    } finally {dbg.exitSubRule(147);}

                    dbg.location(1171,9);
                    pushFollow(FOLLOW_arrayInitializer_in_arrayCreator6963);
                    arrayInitializer();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:1173:9: 'new' createdName '[' expression ']' ( '[' expression ']' )* ( '[' ']' )*
                    {
                    dbg.location(1173,9);
                    match(input,NEW,FOLLOW_NEW_in_arrayCreator6974); if (state.failed) return ;
                    dbg.location(1173,15);
                    pushFollow(FOLLOW_createdName_in_arrayCreator6976);
                    createdName();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(1174,9);
                    match(input,LBRACKET,FOLLOW_LBRACKET_in_arrayCreator6986); if (state.failed) return ;
                    dbg.location(1174,13);
                    pushFollow(FOLLOW_expression_in_arrayCreator6988);
                    expression();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(1175,9);
                    match(input,RBRACKET,FOLLOW_RBRACKET_in_arrayCreator6998); if (state.failed) return ;
                    dbg.location(1176,9);
                    // src/com/google/doclava/parser/Java.g:1176:9: ( '[' expression ']' )*
                    try { dbg.enterSubRule(148);

                    loop148:
                    do {
                        int alt148=2;
                        try { dbg.enterDecision(148, decisionCanBacktrack[148]);

                        try {
                            isCyclicDecision = true;
                            alt148 = dfa148.predict(input);
                        }
                        catch (NoViableAltException nvae) {
                            dbg.recognitionException(nvae);
                            throw nvae;
                        }
                        } finally {dbg.exitDecision(148);}

                        switch (alt148) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:1176:13: '[' expression ']'
			    {
			    dbg.location(1176,13);
			    match(input,LBRACKET,FOLLOW_LBRACKET_in_arrayCreator7012); if (state.failed) return ;
			    dbg.location(1176,17);
			    pushFollow(FOLLOW_expression_in_arrayCreator7014);
			    expression();

			    state._fsp--;
			    if (state.failed) return ;
			    dbg.location(1177,13);
			    match(input,RBRACKET,FOLLOW_RBRACKET_in_arrayCreator7028); if (state.failed) return ;

			    }
			    break;

			default :
			    break loop148;
                        }
                    } while (true);
                    } finally {dbg.exitSubRule(148);}

                    dbg.location(1179,9);
                    // src/com/google/doclava/parser/Java.g:1179:9: ( '[' ']' )*
                    try { dbg.enterSubRule(149);

                    loop149:
                    do {
                        int alt149=2;
                        try { dbg.enterDecision(149, decisionCanBacktrack[149]);

                        int LA149_0 = input.LA(1);

                        if ( (LA149_0==LBRACKET) ) {
                            int LA149_2 = input.LA(2);

                            if ( (LA149_2==RBRACKET) ) {
                                alt149=1;
                            }


                        }


                        } finally {dbg.exitDecision(149);}

                        switch (alt149) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:1179:10: '[' ']'
			    {
			    dbg.location(1179,10);
			    match(input,LBRACKET,FOLLOW_LBRACKET_in_arrayCreator7050); if (state.failed) return ;
			    dbg.location(1179,14);
			    match(input,RBRACKET,FOLLOW_RBRACKET_in_arrayCreator7052); if (state.failed) return ;

			    }
			    break;

			default :
			    break loop149;
                        }
                    } while (true);
                    } finally {dbg.exitSubRule(149);}


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 94, arrayCreator_StartIndex); }
        }
        dbg.location(1181, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "arrayCreator");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "arrayCreator"


    // $ANTLR start "variableInitializer"
    // src/com/google/doclava/parser/Java.g:1183:1: variableInitializer : ( arrayInitializer | expression );
    public final void variableInitializer() throws RecognitionException {
        int variableInitializer_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "variableInitializer");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1183, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 95) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1184:5: ( arrayInitializer | expression )
            int alt151=2;
            try { dbg.enterDecision(151, decisionCanBacktrack[151]);

            int LA151_0 = input.LA(1);

            if ( (LA151_0==LBRACE) ) {
                alt151=1;
            }
            else if ( ((LA151_0>=IDENTIFIER && LA151_0<=NULL)||LA151_0==BOOLEAN||LA151_0==BYTE||LA151_0==CHAR||LA151_0==DOUBLE||LA151_0==FLOAT||LA151_0==INT||LA151_0==LONG||LA151_0==NEW||LA151_0==SHORT||LA151_0==SUPER||LA151_0==THIS||LA151_0==VOID||LA151_0==LPAREN||(LA151_0>=BANG && LA151_0<=TILDE)||(LA151_0>=PLUSPLUS && LA151_0<=SUB)) ) {
                alt151=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 151, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(151);}

            switch (alt151) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1184:9: arrayInitializer
                    {
                    dbg.location(1184,9);
                    pushFollow(FOLLOW_arrayInitializer_in_variableInitializer7082);
                    arrayInitializer();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:1185:9: expression
                    {
                    dbg.location(1185,9);
                    pushFollow(FOLLOW_expression_in_variableInitializer7092);
                    expression();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 95, variableInitializer_StartIndex); }
        }
        dbg.location(1186, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "variableInitializer");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "variableInitializer"


    // $ANTLR start "arrayInitializer"
    // src/com/google/doclava/parser/Java.g:1188:1: arrayInitializer : '{' ( variableInitializer ( ',' variableInitializer )* )? ( ',' )? '}' ;
    public final void arrayInitializer() throws RecognitionException {
        int arrayInitializer_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "arrayInitializer");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1188, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 96) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1189:5: ( '{' ( variableInitializer ( ',' variableInitializer )* )? ( ',' )? '}' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1189:9: '{' ( variableInitializer ( ',' variableInitializer )* )? ( ',' )? '}'
            {
            dbg.location(1189,9);
            match(input,LBRACE,FOLLOW_LBRACE_in_arrayInitializer7111); if (state.failed) return ;
            dbg.location(1190,13);
            // src/com/google/doclava/parser/Java.g:1190:13: ( variableInitializer ( ',' variableInitializer )* )?
            int alt153=2;
            try { dbg.enterSubRule(153);
            try { dbg.enterDecision(153, decisionCanBacktrack[153]);

            int LA153_0 = input.LA(1);

            if ( ((LA153_0>=IDENTIFIER && LA153_0<=NULL)||LA153_0==BOOLEAN||LA153_0==BYTE||LA153_0==CHAR||LA153_0==DOUBLE||LA153_0==FLOAT||LA153_0==INT||LA153_0==LONG||LA153_0==NEW||LA153_0==SHORT||LA153_0==SUPER||LA153_0==THIS||LA153_0==VOID||LA153_0==LPAREN||LA153_0==LBRACE||(LA153_0>=BANG && LA153_0<=TILDE)||(LA153_0>=PLUSPLUS && LA153_0<=SUB)) ) {
                alt153=1;
            }
            } finally {dbg.exitDecision(153);}

            switch (alt153) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1190:14: variableInitializer ( ',' variableInitializer )*
                    {
                    dbg.location(1190,14);
                    pushFollow(FOLLOW_variableInitializer_in_arrayInitializer7126);
                    variableInitializer();

                    state._fsp--;
                    if (state.failed) return ;
                    dbg.location(1191,17);
                    // src/com/google/doclava/parser/Java.g:1191:17: ( ',' variableInitializer )*
                    try { dbg.enterSubRule(152);

                    loop152:
                    do {
                        int alt152=2;
                        try { dbg.enterDecision(152, decisionCanBacktrack[152]);

                        int LA152_0 = input.LA(1);

                        if ( (LA152_0==COMMA) ) {
                            int LA152_1 = input.LA(2);

                            if ( ((LA152_1>=IDENTIFIER && LA152_1<=NULL)||LA152_1==BOOLEAN||LA152_1==BYTE||LA152_1==CHAR||LA152_1==DOUBLE||LA152_1==FLOAT||LA152_1==INT||LA152_1==LONG||LA152_1==NEW||LA152_1==SHORT||LA152_1==SUPER||LA152_1==THIS||LA152_1==VOID||LA152_1==LPAREN||LA152_1==LBRACE||(LA152_1>=BANG && LA152_1<=TILDE)||(LA152_1>=PLUSPLUS && LA152_1<=SUB)) ) {
                                alt152=1;
                            }


                        }


                        } finally {dbg.exitDecision(152);}

                        switch (alt152) {
			case 1 :
			    dbg.enterAlt(1);

			    // src/com/google/doclava/parser/Java.g:1191:18: ',' variableInitializer
			    {
			    dbg.location(1191,18);
			    match(input,COMMA,FOLLOW_COMMA_in_arrayInitializer7145); if (state.failed) return ;
			    dbg.location(1191,22);
			    pushFollow(FOLLOW_variableInitializer_in_arrayInitializer7147);
			    variableInitializer();

			    state._fsp--;
			    if (state.failed) return ;

			    }
			    break;

			default :
			    break loop152;
                        }
                    } while (true);
                    } finally {dbg.exitSubRule(152);}


                    }
                    break;

            }
            } finally {dbg.exitSubRule(153);}

            dbg.location(1194,13);
            // src/com/google/doclava/parser/Java.g:1194:13: ( ',' )?
            int alt154=2;
            try { dbg.enterSubRule(154);
            try { dbg.enterDecision(154, decisionCanBacktrack[154]);

            int LA154_0 = input.LA(1);

            if ( (LA154_0==COMMA) ) {
                alt154=1;
            }
            } finally {dbg.exitDecision(154);}

            switch (alt154) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1194:14: ','
                    {
                    dbg.location(1194,14);
                    match(input,COMMA,FOLLOW_COMMA_in_arrayInitializer7196); if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(154);}

            dbg.location(1195,9);
            match(input,RBRACE,FOLLOW_RBRACE_in_arrayInitializer7208); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 96, arrayInitializer_StartIndex); }
        }
        dbg.location(1196, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "arrayInitializer");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "arrayInitializer"


    // $ANTLR start "createdName"
    // src/com/google/doclava/parser/Java.g:1199:1: createdName : ( classOrInterfaceType | primitiveType );
    public final void createdName() throws RecognitionException {
        int createdName_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "createdName");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1199, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 97) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1200:5: ( classOrInterfaceType | primitiveType )
            int alt155=2;
            try { dbg.enterDecision(155, decisionCanBacktrack[155]);

            int LA155_0 = input.LA(1);

            if ( (LA155_0==IDENTIFIER) ) {
                alt155=1;
            }
            else if ( (LA155_0==BOOLEAN||LA155_0==BYTE||LA155_0==CHAR||LA155_0==DOUBLE||LA155_0==FLOAT||LA155_0==INT||LA155_0==LONG||LA155_0==SHORT) ) {
                alt155=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 155, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }
            } finally {dbg.exitDecision(155);}

            switch (alt155) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1200:9: classOrInterfaceType
                    {
                    dbg.location(1200,9);
                    pushFollow(FOLLOW_classOrInterfaceType_in_createdName7241);
                    classOrInterfaceType();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:1201:9: primitiveType
                    {
                    dbg.location(1201,9);
                    pushFollow(FOLLOW_primitiveType_in_createdName7251);
                    primitiveType();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 97, createdName_StartIndex); }
        }
        dbg.location(1202, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "createdName");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "createdName"


    // $ANTLR start "innerCreator"
    // src/com/google/doclava/parser/Java.g:1204:1: innerCreator : '.' 'new' ( nonWildcardTypeArguments )? IDENTIFIER ( typeArguments )? classCreatorRest ;
    public final void innerCreator() throws RecognitionException {
        int innerCreator_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "innerCreator");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1204, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 98) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1205:5: ( '.' 'new' ( nonWildcardTypeArguments )? IDENTIFIER ( typeArguments )? classCreatorRest )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1205:9: '.' 'new' ( nonWildcardTypeArguments )? IDENTIFIER ( typeArguments )? classCreatorRest
            {
            dbg.location(1205,9);
            match(input,DOT,FOLLOW_DOT_in_innerCreator7270); if (state.failed) return ;
            dbg.location(1205,13);
            match(input,NEW,FOLLOW_NEW_in_innerCreator7272); if (state.failed) return ;
            dbg.location(1206,9);
            // src/com/google/doclava/parser/Java.g:1206:9: ( nonWildcardTypeArguments )?
            int alt156=2;
            try { dbg.enterSubRule(156);
            try { dbg.enterDecision(156, decisionCanBacktrack[156]);

            int LA156_0 = input.LA(1);

            if ( (LA156_0==LT) ) {
                alt156=1;
            }
            } finally {dbg.exitDecision(156);}

            switch (alt156) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1206:10: nonWildcardTypeArguments
                    {
                    dbg.location(1206,10);
                    pushFollow(FOLLOW_nonWildcardTypeArguments_in_innerCreator7283);
                    nonWildcardTypeArguments();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(156);}

            dbg.location(1208,9);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_innerCreator7304); if (state.failed) return ;
            dbg.location(1209,9);
            // src/com/google/doclava/parser/Java.g:1209:9: ( typeArguments )?
            int alt157=2;
            try { dbg.enterSubRule(157);
            try { dbg.enterDecision(157, decisionCanBacktrack[157]);

            int LA157_0 = input.LA(1);

            if ( (LA157_0==LT) ) {
                alt157=1;
            }
            } finally {dbg.exitDecision(157);}

            switch (alt157) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1209:10: typeArguments
                    {
                    dbg.location(1209,10);
                    pushFollow(FOLLOW_typeArguments_in_innerCreator7315);
                    typeArguments();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(157);}

            dbg.location(1211,9);
            pushFollow(FOLLOW_classCreatorRest_in_innerCreator7336);
            classCreatorRest();

            state._fsp--;
            if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 98, innerCreator_StartIndex); }
        }
        dbg.location(1212, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "innerCreator");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "innerCreator"


    // $ANTLR start "classCreatorRest"
    // src/com/google/doclava/parser/Java.g:1215:1: classCreatorRest : arguments ( classBody )? ;
    public final void classCreatorRest() throws RecognitionException {
        int classCreatorRest_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "classCreatorRest");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1215, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 99) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1216:5: ( arguments ( classBody )? )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1216:9: arguments ( classBody )?
            {
            dbg.location(1216,9);
            pushFollow(FOLLOW_arguments_in_classCreatorRest7356);
            arguments();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1217,9);
            // src/com/google/doclava/parser/Java.g:1217:9: ( classBody )?
            int alt158=2;
            try { dbg.enterSubRule(158);
            try { dbg.enterDecision(158, decisionCanBacktrack[158]);

            int LA158_0 = input.LA(1);

            if ( (LA158_0==LBRACE) ) {
                alt158=1;
            }
            } finally {dbg.exitDecision(158);}

            switch (alt158) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1217:10: classBody
                    {
                    dbg.location(1217,10);
                    pushFollow(FOLLOW_classBody_in_classCreatorRest7367);
                    classBody();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(158);}


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 99, classCreatorRest_StartIndex); }
        }
        dbg.location(1219, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "classCreatorRest");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "classCreatorRest"


    // $ANTLR start "nonWildcardTypeArguments"
    // src/com/google/doclava/parser/Java.g:1222:1: nonWildcardTypeArguments : '<' typeList '>' ;
    public final void nonWildcardTypeArguments() throws RecognitionException {
        int nonWildcardTypeArguments_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "nonWildcardTypeArguments");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1222, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 100) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1223:5: ( '<' typeList '>' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1223:9: '<' typeList '>'
            {
            dbg.location(1223,9);
            match(input,LT,FOLLOW_LT_in_nonWildcardTypeArguments7398); if (state.failed) return ;
            dbg.location(1223,13);
            pushFollow(FOLLOW_typeList_in_nonWildcardTypeArguments7400);
            typeList();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1224,9);
            match(input,GT,FOLLOW_GT_in_nonWildcardTypeArguments7410); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 100, nonWildcardTypeArguments_StartIndex); }
        }
        dbg.location(1225, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "nonWildcardTypeArguments");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "nonWildcardTypeArguments"


    // $ANTLR start "arguments"
    // src/com/google/doclava/parser/Java.g:1227:1: arguments : '(' ( expressionList )? ')' ;
    public final void arguments() throws RecognitionException {
        int arguments_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "arguments");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1227, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 101) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1228:5: ( '(' ( expressionList )? ')' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1228:9: '(' ( expressionList )? ')'
            {
            dbg.location(1228,9);
            match(input,LPAREN,FOLLOW_LPAREN_in_arguments7429); if (state.failed) return ;
            dbg.location(1228,13);
            // src/com/google/doclava/parser/Java.g:1228:13: ( expressionList )?
            int alt159=2;
            try { dbg.enterSubRule(159);
            try { dbg.enterDecision(159, decisionCanBacktrack[159]);

            int LA159_0 = input.LA(1);

            if ( ((LA159_0>=IDENTIFIER && LA159_0<=NULL)||LA159_0==BOOLEAN||LA159_0==BYTE||LA159_0==CHAR||LA159_0==DOUBLE||LA159_0==FLOAT||LA159_0==INT||LA159_0==LONG||LA159_0==NEW||LA159_0==SHORT||LA159_0==SUPER||LA159_0==THIS||LA159_0==VOID||LA159_0==LPAREN||(LA159_0>=BANG && LA159_0<=TILDE)||(LA159_0>=PLUSPLUS && LA159_0<=SUB)) ) {
                alt159=1;
            }
            } finally {dbg.exitDecision(159);}

            switch (alt159) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1228:14: expressionList
                    {
                    dbg.location(1228,14);
                    pushFollow(FOLLOW_expressionList_in_arguments7432);
                    expressionList();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(159);}

            dbg.location(1229,12);
            match(input,RPAREN,FOLLOW_RPAREN_in_arguments7445); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 101, arguments_StartIndex); }
        }
        dbg.location(1230, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "arguments");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "arguments"


    // $ANTLR start "literal"
    // src/com/google/doclava/parser/Java.g:1232:1: literal : ( INTLITERAL | LONGLITERAL | FLOATLITERAL | DOUBLELITERAL | CHARLITERAL | STRINGLITERAL | TRUE | FALSE | NULL );
    public final void literal() throws RecognitionException {
        int literal_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "literal");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1232, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 102) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1233:5: ( INTLITERAL | LONGLITERAL | FLOATLITERAL | DOUBLELITERAL | CHARLITERAL | STRINGLITERAL | TRUE | FALSE | NULL )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:
            {
            dbg.location(1233,5);
            if ( (input.LA(1)>=INTLITERAL && input.LA(1)<=NULL) ) {
                input.consume();
                state.errorRecovery=false;state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                dbg.recognitionException(mse);
                throw mse;
            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 102, literal_StartIndex); }
        }
        dbg.location(1242, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "literal");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "literal"


    // $ANTLR start "classHeader"
    // src/com/google/doclava/parser/Java.g:1244:1: classHeader : modifiers 'class' IDENTIFIER ;
    public final void classHeader() throws RecognitionException {
        int classHeader_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "classHeader");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1244, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 103) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1249:5: ( modifiers 'class' IDENTIFIER )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1249:9: modifiers 'class' IDENTIFIER
            {
            dbg.location(1249,9);
            pushFollow(FOLLOW_modifiers_in_classHeader7566);
            modifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1249,19);
            match(input,CLASS,FOLLOW_CLASS_in_classHeader7568); if (state.failed) return ;
            dbg.location(1249,27);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_classHeader7570); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 103, classHeader_StartIndex); }
        }
        dbg.location(1250, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "classHeader");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "classHeader"


    // $ANTLR start "enumHeader"
    // src/com/google/doclava/parser/Java.g:1252:1: enumHeader : modifiers ( 'enum' | IDENTIFIER ) IDENTIFIER ;
    public final void enumHeader() throws RecognitionException {
        int enumHeader_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "enumHeader");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1252, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 104) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1253:5: ( modifiers ( 'enum' | IDENTIFIER ) IDENTIFIER )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1253:9: modifiers ( 'enum' | IDENTIFIER ) IDENTIFIER
            {
            dbg.location(1253,9);
            pushFollow(FOLLOW_modifiers_in_enumHeader7589);
            modifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1253,19);
            if ( input.LA(1)==IDENTIFIER||input.LA(1)==ENUM ) {
                input.consume();
                state.errorRecovery=false;state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                dbg.recognitionException(mse);
                throw mse;
            }

            dbg.location(1253,39);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_enumHeader7597); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 104, enumHeader_StartIndex); }
        }
        dbg.location(1254, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "enumHeader");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "enumHeader"


    // $ANTLR start "interfaceHeader"
    // src/com/google/doclava/parser/Java.g:1256:1: interfaceHeader : modifiers 'interface' IDENTIFIER ;
    public final void interfaceHeader() throws RecognitionException {
        int interfaceHeader_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "interfaceHeader");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1256, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 105) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1257:5: ( modifiers 'interface' IDENTIFIER )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1257:9: modifiers 'interface' IDENTIFIER
            {
            dbg.location(1257,9);
            pushFollow(FOLLOW_modifiers_in_interfaceHeader7616);
            modifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1257,19);
            match(input,INTERFACE,FOLLOW_INTERFACE_in_interfaceHeader7618); if (state.failed) return ;
            dbg.location(1257,31);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_interfaceHeader7620); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 105, interfaceHeader_StartIndex); }
        }
        dbg.location(1258, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "interfaceHeader");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "interfaceHeader"


    // $ANTLR start "annotationHeader"
    // src/com/google/doclava/parser/Java.g:1260:1: annotationHeader : modifiers '@' 'interface' IDENTIFIER ;
    public final void annotationHeader() throws RecognitionException {
        int annotationHeader_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "annotationHeader");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1260, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 106) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1261:5: ( modifiers '@' 'interface' IDENTIFIER )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1261:9: modifiers '@' 'interface' IDENTIFIER
            {
            dbg.location(1261,9);
            pushFollow(FOLLOW_modifiers_in_annotationHeader7639);
            modifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1261,19);
            match(input,MONKEYS_AT,FOLLOW_MONKEYS_AT_in_annotationHeader7641); if (state.failed) return ;
            dbg.location(1261,23);
            match(input,INTERFACE,FOLLOW_INTERFACE_in_annotationHeader7643); if (state.failed) return ;
            dbg.location(1261,35);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_annotationHeader7645); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 106, annotationHeader_StartIndex); }
        }
        dbg.location(1262, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "annotationHeader");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "annotationHeader"


    // $ANTLR start "typeHeader"
    // src/com/google/doclava/parser/Java.g:1264:1: typeHeader : modifiers ( 'class' | 'enum' | ( ( '@' )? 'interface' ) ) IDENTIFIER ;
    public final void typeHeader() throws RecognitionException {
        int typeHeader_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "typeHeader");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1264, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 107) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1265:5: ( modifiers ( 'class' | 'enum' | ( ( '@' )? 'interface' ) ) IDENTIFIER )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1265:9: modifiers ( 'class' | 'enum' | ( ( '@' )? 'interface' ) ) IDENTIFIER
            {
            dbg.location(1265,9);
            pushFollow(FOLLOW_modifiers_in_typeHeader7664);
            modifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1265,19);
            // src/com/google/doclava/parser/Java.g:1265:19: ( 'class' | 'enum' | ( ( '@' )? 'interface' ) )
            int alt161=3;
            try { dbg.enterSubRule(161);
            try { dbg.enterDecision(161, decisionCanBacktrack[161]);

            switch ( input.LA(1) ) {
            case CLASS:
                {
                alt161=1;
                }
                break;
            case ENUM:
                {
                alt161=2;
                }
                break;
            case INTERFACE:
            case MONKEYS_AT:
                {
                alt161=3;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 161, 0, input);

                dbg.recognitionException(nvae);
                throw nvae;
            }

            } finally {dbg.exitDecision(161);}

            switch (alt161) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1265:20: 'class'
                    {
                    dbg.location(1265,20);
                    match(input,CLASS,FOLLOW_CLASS_in_typeHeader7667); if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:1265:28: 'enum'
                    {
                    dbg.location(1265,28);
                    match(input,ENUM,FOLLOW_ENUM_in_typeHeader7669); if (state.failed) return ;

                    }
                    break;
                case 3 :
                    dbg.enterAlt(3);

                    // src/com/google/doclava/parser/Java.g:1265:35: ( ( '@' )? 'interface' )
                    {
                    dbg.location(1265,35);
                    // src/com/google/doclava/parser/Java.g:1265:35: ( ( '@' )? 'interface' )
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1265:36: ( '@' )? 'interface'
                    {
                    dbg.location(1265,36);
                    // src/com/google/doclava/parser/Java.g:1265:36: ( '@' )?
                    int alt160=2;
                    try { dbg.enterSubRule(160);
                    try { dbg.enterDecision(160, decisionCanBacktrack[160]);

                    int LA160_0 = input.LA(1);

                    if ( (LA160_0==MONKEYS_AT) ) {
                        alt160=1;
                    }
                    } finally {dbg.exitDecision(160);}

                    switch (alt160) {
                        case 1 :
                            dbg.enterAlt(1);

                            // src/com/google/doclava/parser/Java.g:0:0: '@'
                            {
                            dbg.location(1265,36);
                            match(input,MONKEYS_AT,FOLLOW_MONKEYS_AT_in_typeHeader7672); if (state.failed) return ;

                            }
                            break;

                    }
                    } finally {dbg.exitSubRule(160);}

                    dbg.location(1265,42);
                    match(input,INTERFACE,FOLLOW_INTERFACE_in_typeHeader7676); if (state.failed) return ;

                    }


                    }
                    break;

            }
            } finally {dbg.exitSubRule(161);}

            dbg.location(1265,56);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_typeHeader7680); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 107, typeHeader_StartIndex); }
        }
        dbg.location(1266, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "typeHeader");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "typeHeader"


    // $ANTLR start "methodHeader"
    // src/com/google/doclava/parser/Java.g:1268:1: methodHeader : modifiers ( typeParameters )? ( type | 'void' )? IDENTIFIER '(' ;
    public final void methodHeader() throws RecognitionException {
        int methodHeader_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "methodHeader");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1268, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 108) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1269:5: ( modifiers ( typeParameters )? ( type | 'void' )? IDENTIFIER '(' )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1269:9: modifiers ( typeParameters )? ( type | 'void' )? IDENTIFIER '('
            {
            dbg.location(1269,9);
            pushFollow(FOLLOW_modifiers_in_methodHeader7699);
            modifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1269,19);
            // src/com/google/doclava/parser/Java.g:1269:19: ( typeParameters )?
            int alt162=2;
            try { dbg.enterSubRule(162);
            try { dbg.enterDecision(162, decisionCanBacktrack[162]);

            int LA162_0 = input.LA(1);

            if ( (LA162_0==LT) ) {
                alt162=1;
            }
            } finally {dbg.exitDecision(162);}

            switch (alt162) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:0:0: typeParameters
                    {
                    dbg.location(1269,19);
                    pushFollow(FOLLOW_typeParameters_in_methodHeader7701);
                    typeParameters();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(162);}

            dbg.location(1269,35);
            // src/com/google/doclava/parser/Java.g:1269:35: ( type | 'void' )?
            int alt163=3;
            try { dbg.enterSubRule(163);
            try { dbg.enterDecision(163, decisionCanBacktrack[163]);

            switch ( input.LA(1) ) {
                case IDENTIFIER:
                    {
                    int LA163_1 = input.LA(2);

                    if ( (LA163_1==IDENTIFIER||LA163_1==LBRACKET||LA163_1==DOT||LA163_1==LT) ) {
                        alt163=1;
                    }
                    }
                    break;
                case BOOLEAN:
                case BYTE:
                case CHAR:
                case DOUBLE:
                case FLOAT:
                case INT:
                case LONG:
                case SHORT:
                    {
                    alt163=1;
                    }
                    break;
                case VOID:
                    {
                    alt163=2;
                    }
                    break;
            }

            } finally {dbg.exitDecision(163);}

            switch (alt163) {
                case 1 :
                    dbg.enterAlt(1);

                    // src/com/google/doclava/parser/Java.g:1269:36: type
                    {
                    dbg.location(1269,36);
                    pushFollow(FOLLOW_type_in_methodHeader7705);
                    type();

                    state._fsp--;
                    if (state.failed) return ;

                    }
                    break;
                case 2 :
                    dbg.enterAlt(2);

                    // src/com/google/doclava/parser/Java.g:1269:41: 'void'
                    {
                    dbg.location(1269,41);
                    match(input,VOID,FOLLOW_VOID_in_methodHeader7707); if (state.failed) return ;

                    }
                    break;

            }
            } finally {dbg.exitSubRule(163);}

            dbg.location(1269,50);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_methodHeader7711); if (state.failed) return ;
            dbg.location(1269,61);
            match(input,LPAREN,FOLLOW_LPAREN_in_methodHeader7713); if (state.failed) return ;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 108, methodHeader_StartIndex); }
        }
        dbg.location(1270, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "methodHeader");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "methodHeader"


    // $ANTLR start "fieldHeader"
    // src/com/google/doclava/parser/Java.g:1272:1: fieldHeader : modifiers type IDENTIFIER ( '[' ']' )* ( '=' | ',' | ';' ) ;
    public final void fieldHeader() throws RecognitionException {
        int fieldHeader_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "fieldHeader");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1272, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 109) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1273:5: ( modifiers type IDENTIFIER ( '[' ']' )* ( '=' | ',' | ';' ) )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1273:9: modifiers type IDENTIFIER ( '[' ']' )* ( '=' | ',' | ';' )
            {
            dbg.location(1273,9);
            pushFollow(FOLLOW_modifiers_in_fieldHeader7732);
            modifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1273,19);
            pushFollow(FOLLOW_type_in_fieldHeader7734);
            type();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1273,24);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_fieldHeader7736); if (state.failed) return ;
            dbg.location(1273,35);
            // src/com/google/doclava/parser/Java.g:1273:35: ( '[' ']' )*
            try { dbg.enterSubRule(164);

            loop164:
            do {
                int alt164=2;
                try { dbg.enterDecision(164, decisionCanBacktrack[164]);

                int LA164_0 = input.LA(1);

                if ( (LA164_0==LBRACKET) ) {
                    alt164=1;
                }


                } finally {dbg.exitDecision(164);}

                switch (alt164) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:1273:36: '[' ']'
		    {
		    dbg.location(1273,36);
		    match(input,LBRACKET,FOLLOW_LBRACKET_in_fieldHeader7739); if (state.failed) return ;
		    dbg.location(1273,39);
		    match(input,RBRACKET,FOLLOW_RBRACKET_in_fieldHeader7740); if (state.failed) return ;

		    }
		    break;

		default :
		    break loop164;
                }
            } while (true);
            } finally {dbg.exitSubRule(164);}

            dbg.location(1273,45);
            if ( (input.LA(1)>=SEMI && input.LA(1)<=COMMA)||input.LA(1)==EQ ) {
                input.consume();
                state.errorRecovery=false;state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                dbg.recognitionException(mse);
                throw mse;
            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 109, fieldHeader_StartIndex); }
        }
        dbg.location(1274, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "fieldHeader");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "fieldHeader"


    // $ANTLR start "localVariableHeader"
    // src/com/google/doclava/parser/Java.g:1276:1: localVariableHeader : variableModifiers type IDENTIFIER ( '[' ']' )* ( '=' | ',' | ';' ) ;
    public final void localVariableHeader() throws RecognitionException {
        int localVariableHeader_StartIndex = input.index();
        try { dbg.enterRule(getGrammarFileName(), "localVariableHeader");
        if ( getRuleLevel()==0 ) {dbg.commence();}
        incRuleLevel();
        dbg.location(1276, 1);

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 110) ) { return ; }
            // src/com/google/doclava/parser/Java.g:1277:5: ( variableModifiers type IDENTIFIER ( '[' ']' )* ( '=' | ',' | ';' ) )
            dbg.enterAlt(1);

            // src/com/google/doclava/parser/Java.g:1277:9: variableModifiers type IDENTIFIER ( '[' ']' )* ( '=' | ',' | ';' )
            {
            dbg.location(1277,9);
            pushFollow(FOLLOW_variableModifiers_in_localVariableHeader7769);
            variableModifiers();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1277,27);
            pushFollow(FOLLOW_type_in_localVariableHeader7771);
            type();

            state._fsp--;
            if (state.failed) return ;
            dbg.location(1277,32);
            match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_localVariableHeader7773); if (state.failed) return ;
            dbg.location(1277,43);
            // src/com/google/doclava/parser/Java.g:1277:43: ( '[' ']' )*
            try { dbg.enterSubRule(165);

            loop165:
            do {
                int alt165=2;
                try { dbg.enterDecision(165, decisionCanBacktrack[165]);

                int LA165_0 = input.LA(1);

                if ( (LA165_0==LBRACKET) ) {
                    alt165=1;
                }


                } finally {dbg.exitDecision(165);}

                switch (alt165) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:1277:44: '[' ']'
		    {
		    dbg.location(1277,44);
		    match(input,LBRACKET,FOLLOW_LBRACKET_in_localVariableHeader7776); if (state.failed) return ;
		    dbg.location(1277,47);
		    match(input,RBRACKET,FOLLOW_RBRACKET_in_localVariableHeader7777); if (state.failed) return ;

		    }
		    break;

		default :
		    break loop165;
                }
            } while (true);
            } finally {dbg.exitSubRule(165);}

            dbg.location(1277,53);
            if ( (input.LA(1)>=SEMI && input.LA(1)<=COMMA)||input.LA(1)==EQ ) {
                input.consume();
                state.errorRecovery=false;state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                dbg.recognitionException(mse);
                throw mse;
            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 110, localVariableHeader_StartIndex); }
        }
        dbg.location(1278, 5);

        }
        finally {
            dbg.exitRule(getGrammarFileName(), "localVariableHeader");
            decRuleLevel();
            if ( getRuleLevel()==0 ) {dbg.terminate();}
        }

        return ;
    }
    // $ANTLR end "localVariableHeader"

    // $ANTLR start synpred2_Java
    public final void synpred2_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:298:13: ( ( annotations )? packageDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:298:13: ( annotations )? packageDeclaration
        {
        dbg.location(298,13);
        // src/com/google/doclava/parser/Java.g:298:13: ( annotations )?
        int alt166=2;
        try { dbg.enterSubRule(166);
        try { dbg.enterDecision(166, decisionCanBacktrack[166]);

        int LA166_0 = input.LA(1);

        if ( (LA166_0==MONKEYS_AT) ) {
            alt166=1;
        }
        } finally {dbg.exitDecision(166);}

        switch (alt166) {
            case 1 :
                dbg.enterAlt(1);

                // src/com/google/doclava/parser/Java.g:298:14: annotations
                {
                dbg.location(298,14);
                pushFollow(FOLLOW_annotations_in_synpred2_Java64);
                annotations();

                state._fsp--;
                if (state.failed) return ;

                }
                break;

        }
        } finally {dbg.exitSubRule(166);}

        dbg.location(300,13);
        pushFollow(FOLLOW_packageDeclaration_in_synpred2_Java93);
        packageDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred2_Java

    // $ANTLR start synpred12_Java
    public final void synpred12_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:342:10: ( classDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:342:10: classDeclaration
        {
        dbg.location(342,10);
        pushFollow(FOLLOW_classDeclaration_in_synpred12_Java436);
        classDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred12_Java

    // $ANTLR start synpred27_Java
    public final void synpred27_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:373:9: ( normalClassDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:373:9: normalClassDeclaration
        {
        dbg.location(373,9);
        pushFollow(FOLLOW_normalClassDeclaration_in_synpred27_Java659);
        normalClassDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred27_Java

    // $ANTLR start synpred43_Java
    public final void synpred43_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:461:9: ( normalInterfaceDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:461:9: normalInterfaceDeclaration
        {
        dbg.location(461,9);
        pushFollow(FOLLOW_normalInterfaceDeclaration_in_synpred43_Java1306);
        normalInterfaceDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred43_Java

    // $ANTLR start synpred52_Java
    public final void synpred52_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:503:10: ( fieldDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:503:10: fieldDeclaration
        {
        dbg.location(503,10);
        pushFollow(FOLLOW_fieldDeclaration_in_synpred52_Java1621);
        fieldDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred52_Java

    // $ANTLR start synpred53_Java
    public final void synpred53_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:504:10: ( methodDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:504:10: methodDeclaration
        {
        dbg.location(504,10);
        pushFollow(FOLLOW_methodDeclaration_in_synpred53_Java1632);
        methodDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred53_Java

    // $ANTLR start synpred54_Java
    public final void synpred54_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:505:10: ( classDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:505:10: classDeclaration
        {
        dbg.location(505,10);
        pushFollow(FOLLOW_classDeclaration_in_synpred54_Java1643);
        classDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred54_Java

    // $ANTLR start synpred57_Java
    public final void synpred57_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:521:10: ( explicitConstructorInvocation )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:521:10: explicitConstructorInvocation
        {
        dbg.location(521,10);
        pushFollow(FOLLOW_explicitConstructorInvocation_in_synpred57_Java1778);
        explicitConstructorInvocation();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred57_Java

    // $ANTLR start synpred59_Java
    public final void synpred59_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:513:10: ( modifiers ( typeParameters )? IDENTIFIER formalParameters ( 'throws' qualifiedNameList )? '{' ( explicitConstructorInvocation )? ( blockStatement )* '}' )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:513:10: modifiers ( typeParameters )? IDENTIFIER formalParameters ( 'throws' qualifiedNameList )? '{' ( explicitConstructorInvocation )? ( blockStatement )* '}'
        {
        dbg.location(513,10);
        pushFollow(FOLLOW_modifiers_in_synpred59_Java1691);
        modifiers();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(514,9);
        // src/com/google/doclava/parser/Java.g:514:9: ( typeParameters )?
        int alt169=2;
        try { dbg.enterSubRule(169);
        try { dbg.enterDecision(169, decisionCanBacktrack[169]);

        int LA169_0 = input.LA(1);

        if ( (LA169_0==LT) ) {
            alt169=1;
        }
        } finally {dbg.exitDecision(169);}

        switch (alt169) {
            case 1 :
                dbg.enterAlt(1);

                // src/com/google/doclava/parser/Java.g:514:10: typeParameters
                {
                dbg.location(514,10);
                pushFollow(FOLLOW_typeParameters_in_synpred59_Java1702);
                typeParameters();

                state._fsp--;
                if (state.failed) return ;

                }
                break;

        }
        } finally {dbg.exitSubRule(169);}

        dbg.location(516,9);
        match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_synpred59_Java1723); if (state.failed) return ;
        dbg.location(517,9);
        pushFollow(FOLLOW_formalParameters_in_synpred59_Java1733);
        formalParameters();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(518,9);
        // src/com/google/doclava/parser/Java.g:518:9: ( 'throws' qualifiedNameList )?
        int alt170=2;
        try { dbg.enterSubRule(170);
        try { dbg.enterDecision(170, decisionCanBacktrack[170]);

        int LA170_0 = input.LA(1);

        if ( (LA170_0==THROWS) ) {
            alt170=1;
        }
        } finally {dbg.exitDecision(170);}

        switch (alt170) {
            case 1 :
                dbg.enterAlt(1);

                // src/com/google/doclava/parser/Java.g:518:10: 'throws' qualifiedNameList
                {
                dbg.location(518,10);
                match(input,THROWS,FOLLOW_THROWS_in_synpred59_Java1744); if (state.failed) return ;
                dbg.location(518,19);
                pushFollow(FOLLOW_qualifiedNameList_in_synpred59_Java1746);
                qualifiedNameList();

                state._fsp--;
                if (state.failed) return ;

                }
                break;

        }
        } finally {dbg.exitSubRule(170);}

        dbg.location(520,9);
        match(input,LBRACE,FOLLOW_LBRACE_in_synpred59_Java1767); if (state.failed) return ;
        dbg.location(521,9);
        // src/com/google/doclava/parser/Java.g:521:9: ( explicitConstructorInvocation )?
        int alt171=2;
        try { dbg.enterSubRule(171);
        try { dbg.enterDecision(171, decisionCanBacktrack[171]);

        try {
            isCyclicDecision = true;
            alt171 = dfa171.predict(input);
        }
        catch (NoViableAltException nvae) {
            dbg.recognitionException(nvae);
            throw nvae;
        }
        } finally {dbg.exitDecision(171);}

        switch (alt171) {
            case 1 :
                dbg.enterAlt(1);

                // src/com/google/doclava/parser/Java.g:521:10: explicitConstructorInvocation
                {
                dbg.location(521,10);
                pushFollow(FOLLOW_explicitConstructorInvocation_in_synpred59_Java1778);
                explicitConstructorInvocation();

                state._fsp--;
                if (state.failed) return ;

                }
                break;

        }
        } finally {dbg.exitSubRule(171);}

        dbg.location(523,9);
        // src/com/google/doclava/parser/Java.g:523:9: ( blockStatement )*
        try { dbg.enterSubRule(172);

        loop172:
        do {
            int alt172=2;
            try { dbg.enterDecision(172, decisionCanBacktrack[172]);

            int LA172_0 = input.LA(1);

            if ( ((LA172_0>=IDENTIFIER && LA172_0<=NULL)||(LA172_0>=ABSTRACT && LA172_0<=BYTE)||(LA172_0>=CHAR && LA172_0<=CLASS)||LA172_0==CONTINUE||(LA172_0>=DO && LA172_0<=DOUBLE)||LA172_0==ENUM||LA172_0==FINAL||(LA172_0>=FLOAT && LA172_0<=FOR)||LA172_0==IF||(LA172_0>=INT && LA172_0<=NEW)||(LA172_0>=PRIVATE && LA172_0<=THROW)||(LA172_0>=TRANSIENT && LA172_0<=LPAREN)||LA172_0==LBRACE||LA172_0==SEMI||(LA172_0>=BANG && LA172_0<=TILDE)||(LA172_0>=PLUSPLUS && LA172_0<=SUB)||LA172_0==MONKEYS_AT||LA172_0==LT) ) {
                alt172=1;
            }


            } finally {dbg.exitDecision(172);}

            switch (alt172) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:523:10: blockStatement
		    {
		    dbg.location(523,10);
		    pushFollow(FOLLOW_blockStatement_in_synpred59_Java1800);
		    blockStatement();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop172;
            }
        } while (true);
        } finally {dbg.exitSubRule(172);}

        dbg.location(525,9);
        match(input,RBRACE,FOLLOW_RBRACE_in_synpred59_Java1821); if (state.failed) return ;

        }
    }
    // $ANTLR end synpred59_Java

    // $ANTLR start synpred68_Java
    public final void synpred68_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:567:9: ( interfaceFieldDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:567:9: interfaceFieldDeclaration
        {
        dbg.location(567,9);
        pushFollow(FOLLOW_interfaceFieldDeclaration_in_synpred68_Java2172);
        interfaceFieldDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred68_Java

    // $ANTLR start synpred69_Java
    public final void synpred69_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:568:9: ( interfaceMethodDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:568:9: interfaceMethodDeclaration
        {
        dbg.location(568,9);
        pushFollow(FOLLOW_interfaceMethodDeclaration_in_synpred69_Java2182);
        interfaceMethodDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred69_Java

    // $ANTLR start synpred70_Java
    public final void synpred70_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:569:9: ( interfaceDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:569:9: interfaceDeclaration
        {
        dbg.location(569,9);
        pushFollow(FOLLOW_interfaceDeclaration_in_synpred70_Java2192);
        interfaceDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred70_Java

    // $ANTLR start synpred71_Java
    public final void synpred71_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:570:9: ( classDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:570:9: classDeclaration
        {
        dbg.location(570,9);
        pushFollow(FOLLOW_classDeclaration_in_synpred71_Java2202);
        classDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred71_Java

    // $ANTLR start synpred96_Java
    public final void synpred96_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:665:9: ( ellipsisParameterDecl )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:665:9: ellipsisParameterDecl
        {
        dbg.location(665,9);
        pushFollow(FOLLOW_ellipsisParameterDecl_in_synpred96_Java2953);
        ellipsisParameterDecl();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred96_Java

    // $ANTLR start synpred98_Java
    public final void synpred98_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:666:9: ( normalParameterDecl ( ',' normalParameterDecl )* )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:666:9: normalParameterDecl ( ',' normalParameterDecl )*
        {
        dbg.location(666,9);
        pushFollow(FOLLOW_normalParameterDecl_in_synpred98_Java2963);
        normalParameterDecl();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(667,9);
        // src/com/google/doclava/parser/Java.g:667:9: ( ',' normalParameterDecl )*
        try { dbg.enterSubRule(175);

        loop175:
        do {
            int alt175=2;
            try { dbg.enterDecision(175, decisionCanBacktrack[175]);

            int LA175_0 = input.LA(1);

            if ( (LA175_0==COMMA) ) {
                alt175=1;
            }


            } finally {dbg.exitDecision(175);}

            switch (alt175) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:667:10: ',' normalParameterDecl
		    {
		    dbg.location(667,10);
		    match(input,COMMA,FOLLOW_COMMA_in_synpred98_Java2974); if (state.failed) return ;
		    dbg.location(667,14);
		    pushFollow(FOLLOW_normalParameterDecl_in_synpred98_Java2976);
		    normalParameterDecl();

		    state._fsp--;
		    if (state.failed) return ;

		    }
		    break;

		default :
		    break loop175;
            }
        } while (true);
        } finally {dbg.exitSubRule(175);}


        }
    }
    // $ANTLR end synpred98_Java

    // $ANTLR start synpred99_Java
    public final void synpred99_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:669:10: ( normalParameterDecl ',' )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:669:10: normalParameterDecl ','
        {
        dbg.location(669,10);
        pushFollow(FOLLOW_normalParameterDecl_in_synpred99_Java2998);
        normalParameterDecl();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(670,9);
        match(input,COMMA,FOLLOW_COMMA_in_synpred99_Java3008); if (state.failed) return ;

        }
    }
    // $ANTLR end synpred99_Java

    // $ANTLR start synpred103_Java
    public final void synpred103_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:689:9: ( ( nonWildcardTypeArguments )? ( 'this' | 'super' ) arguments ';' )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:689:9: ( nonWildcardTypeArguments )? ( 'this' | 'super' ) arguments ';'
        {
        dbg.location(689,9);
        // src/com/google/doclava/parser/Java.g:689:9: ( nonWildcardTypeArguments )?
        int alt176=2;
        try { dbg.enterSubRule(176);
        try { dbg.enterDecision(176, decisionCanBacktrack[176]);

        int LA176_0 = input.LA(1);

        if ( (LA176_0==LT) ) {
            alt176=1;
        }
        } finally {dbg.exitDecision(176);}

        switch (alt176) {
            case 1 :
                dbg.enterAlt(1);

                // src/com/google/doclava/parser/Java.g:689:10: nonWildcardTypeArguments
                {
                dbg.location(689,10);
                pushFollow(FOLLOW_nonWildcardTypeArguments_in_synpred103_Java3139);
                nonWildcardTypeArguments();

                state._fsp--;
                if (state.failed) return ;

                }
                break;

        }
        } finally {dbg.exitSubRule(176);}

        dbg.location(691,9);
        if ( input.LA(1)==SUPER||input.LA(1)==THIS ) {
            input.consume();
            state.errorRecovery=false;state.failed=false;
        }
        else {
            if (state.backtracking>0) {state.failed=true; return ;}
            MismatchedSetException mse = new MismatchedSetException(null,input);
            dbg.recognitionException(mse);
            throw mse;
        }

        dbg.location(694,9);
        pushFollow(FOLLOW_arguments_in_synpred103_Java3197);
        arguments();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(694,19);
        match(input,SEMI,FOLLOW_SEMI_in_synpred103_Java3199); if (state.failed) return ;

        }
    }
    // $ANTLR end synpred103_Java

    // $ANTLR start synpred117_Java
    public final void synpred117_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:776:9: ( annotationMethodDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:776:9: annotationMethodDeclaration
        {
        dbg.location(776,9);
        pushFollow(FOLLOW_annotationMethodDeclaration_in_synpred117_Java3781);
        annotationMethodDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred117_Java

    // $ANTLR start synpred118_Java
    public final void synpred118_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:777:9: ( interfaceFieldDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:777:9: interfaceFieldDeclaration
        {
        dbg.location(777,9);
        pushFollow(FOLLOW_interfaceFieldDeclaration_in_synpred118_Java3791);
        interfaceFieldDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred118_Java

    // $ANTLR start synpred119_Java
    public final void synpred119_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:778:9: ( normalClassDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:778:9: normalClassDeclaration
        {
        dbg.location(778,9);
        pushFollow(FOLLOW_normalClassDeclaration_in_synpred119_Java3801);
        normalClassDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred119_Java

    // $ANTLR start synpred120_Java
    public final void synpred120_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:779:9: ( normalInterfaceDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:779:9: normalInterfaceDeclaration
        {
        dbg.location(779,9);
        pushFollow(FOLLOW_normalInterfaceDeclaration_in_synpred120_Java3811);
        normalInterfaceDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred120_Java

    // $ANTLR start synpred121_Java
    public final void synpred121_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:780:9: ( enumDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:780:9: enumDeclaration
        {
        dbg.location(780,9);
        pushFollow(FOLLOW_enumDeclaration_in_synpred121_Java3821);
        enumDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred121_Java

    // $ANTLR start synpred122_Java
    public final void synpred122_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:781:9: ( annotationTypeDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:781:9: annotationTypeDeclaration
        {
        dbg.location(781,9);
        pushFollow(FOLLOW_annotationTypeDeclaration_in_synpred122_Java3831);
        annotationTypeDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred122_Java

    // $ANTLR start synpred125_Java
    public final void synpred125_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:824:9: ( localVariableDeclarationStatement )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:824:9: localVariableDeclarationStatement
        {
        dbg.location(824,9);
        pushFollow(FOLLOW_localVariableDeclarationStatement_in_synpred125_Java3986);
        localVariableDeclarationStatement();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred125_Java

    // $ANTLR start synpred126_Java
    public final void synpred126_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:825:9: ( classOrInterfaceDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:825:9: classOrInterfaceDeclaration
        {
        dbg.location(825,9);
        pushFollow(FOLLOW_classOrInterfaceDeclaration_in_synpred126_Java3996);
        classOrInterfaceDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred126_Java

    // $ANTLR start synpred130_Java
    public final void synpred130_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:845:9: ( ( 'assert' ) expression ( ':' expression )? ';' )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:845:9: ( 'assert' ) expression ( ':' expression )? ';'
        {
        dbg.location(845,9);
        // src/com/google/doclava/parser/Java.g:845:9: ( 'assert' )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:845:10: 'assert'
        {
        dbg.location(845,10);
        match(input,ASSERT,FOLLOW_ASSERT_in_synpred130_Java4122); if (state.failed) return ;

        }

        dbg.location(847,9);
        pushFollow(FOLLOW_expression_in_synpred130_Java4142);
        expression();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(847,20);
        // src/com/google/doclava/parser/Java.g:847:20: ( ':' expression )?
        int alt179=2;
        try { dbg.enterSubRule(179);
        try { dbg.enterDecision(179, decisionCanBacktrack[179]);

        int LA179_0 = input.LA(1);

        if ( (LA179_0==COLON) ) {
            alt179=1;
        }
        } finally {dbg.exitDecision(179);}

        switch (alt179) {
            case 1 :
                dbg.enterAlt(1);

                // src/com/google/doclava/parser/Java.g:847:21: ':' expression
                {
                dbg.location(847,21);
                match(input,COLON,FOLLOW_COLON_in_synpred130_Java4145); if (state.failed) return ;
                dbg.location(847,25);
                pushFollow(FOLLOW_expression_in_synpred130_Java4147);
                expression();

                state._fsp--;
                if (state.failed) return ;

                }
                break;

        }
        } finally {dbg.exitSubRule(179);}

        dbg.location(847,38);
        match(input,SEMI,FOLLOW_SEMI_in_synpred130_Java4151); if (state.failed) return ;

        }
    }
    // $ANTLR end synpred130_Java

    // $ANTLR start synpred132_Java
    public final void synpred132_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:848:9: ( 'assert' expression ( ':' expression )? ';' )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:848:9: 'assert' expression ( ':' expression )? ';'
        {
        dbg.location(848,9);
        match(input,ASSERT,FOLLOW_ASSERT_in_synpred132_Java4161); if (state.failed) return ;
        dbg.location(848,19);
        pushFollow(FOLLOW_expression_in_synpred132_Java4164);
        expression();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(848,30);
        // src/com/google/doclava/parser/Java.g:848:30: ( ':' expression )?
        int alt180=2;
        try { dbg.enterSubRule(180);
        try { dbg.enterDecision(180, decisionCanBacktrack[180]);

        int LA180_0 = input.LA(1);

        if ( (LA180_0==COLON) ) {
            alt180=1;
        }
        } finally {dbg.exitDecision(180);}

        switch (alt180) {
            case 1 :
                dbg.enterAlt(1);

                // src/com/google/doclava/parser/Java.g:848:31: ':' expression
                {
                dbg.location(848,31);
                match(input,COLON,FOLLOW_COLON_in_synpred132_Java4167); if (state.failed) return ;
                dbg.location(848,35);
                pushFollow(FOLLOW_expression_in_synpred132_Java4169);
                expression();

                state._fsp--;
                if (state.failed) return ;

                }
                break;

        }
        } finally {dbg.exitSubRule(180);}

        dbg.location(848,48);
        match(input,SEMI,FOLLOW_SEMI_in_synpred132_Java4173); if (state.failed) return ;

        }
    }
    // $ANTLR end synpred132_Java

    // $ANTLR start synpred133_Java
    public final void synpred133_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:849:39: ( 'else' statement )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:849:39: 'else' statement
        {
        dbg.location(849,39);
        match(input,ELSE,FOLLOW_ELSE_in_synpred133_Java4190); if (state.failed) return ;
        dbg.location(849,46);
        pushFollow(FOLLOW_statement_in_synpred133_Java4192);
        statement();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred133_Java

    // $ANTLR start synpred148_Java
    public final void synpred148_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:864:9: ( expression ';' )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:864:9: expression ';'
        {
        dbg.location(864,9);
        pushFollow(FOLLOW_expression_in_synpred148_Java4404);
        expression();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(864,21);
        match(input,SEMI,FOLLOW_SEMI_in_synpred148_Java4407); if (state.failed) return ;

        }
    }
    // $ANTLR end synpred148_Java

    // $ANTLR start synpred149_Java
    public final void synpred149_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:865:9: ( IDENTIFIER ':' statement )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:865:9: IDENTIFIER ':' statement
        {
        dbg.location(865,9);
        match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_synpred149_Java4417); if (state.failed) return ;
        dbg.location(865,20);
        match(input,COLON,FOLLOW_COLON_in_synpred149_Java4419); if (state.failed) return ;
        dbg.location(865,24);
        pushFollow(FOLLOW_statement_in_synpred149_Java4421);
        statement();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred149_Java

    // $ANTLR start synpred153_Java
    public final void synpred153_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:889:13: ( catches 'finally' block )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:889:13: catches 'finally' block
        {
        dbg.location(889,13);
        pushFollow(FOLLOW_catches_in_synpred153_Java4573);
        catches();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(889,21);
        match(input,FINALLY,FOLLOW_FINALLY_in_synpred153_Java4575); if (state.failed) return ;
        dbg.location(889,31);
        pushFollow(FOLLOW_block_in_synpred153_Java4577);
        block();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred153_Java

    // $ANTLR start synpred154_Java
    public final void synpred154_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:890:13: ( catches )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:890:13: catches
        {
        dbg.location(890,13);
        pushFollow(FOLLOW_catches_in_synpred154_Java4591);
        catches();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred154_Java

    // $ANTLR start synpred157_Java
    public final void synpred157_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:915:9: ( 'for' '(' variableModifiers type IDENTIFIER ':' expression ')' statement )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:915:9: 'for' '(' variableModifiers type IDENTIFIER ':' expression ')' statement
        {
        dbg.location(915,9);
        match(input,FOR,FOLLOW_FOR_in_synpred157_Java4775); if (state.failed) return ;
        dbg.location(915,15);
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred157_Java4777); if (state.failed) return ;
        dbg.location(915,19);
        pushFollow(FOLLOW_variableModifiers_in_synpred157_Java4779);
        variableModifiers();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(915,37);
        pushFollow(FOLLOW_type_in_synpred157_Java4781);
        type();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(915,42);
        match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_synpred157_Java4783); if (state.failed) return ;
        dbg.location(915,53);
        match(input,COLON,FOLLOW_COLON_in_synpred157_Java4785); if (state.failed) return ;
        dbg.location(916,9);
        pushFollow(FOLLOW_expression_in_synpred157_Java4795);
        expression();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(916,20);
        match(input,RPAREN,FOLLOW_RPAREN_in_synpred157_Java4797); if (state.failed) return ;
        dbg.location(916,24);
        pushFollow(FOLLOW_statement_in_synpred157_Java4799);
        statement();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred157_Java

    // $ANTLR start synpred161_Java
    public final void synpred161_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:929:9: ( localVariableDeclaration )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:929:9: localVariableDeclaration
        {
        dbg.location(929,9);
        pushFollow(FOLLOW_localVariableDeclaration_in_synpred161_Java4962);
        localVariableDeclaration();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred161_Java

    // $ANTLR start synpred202_Java
    public final void synpred202_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:1083:9: ( castExpression )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:1083:9: castExpression
        {
        dbg.location(1083,9);
        pushFollow(FOLLOW_castExpression_in_synpred202_Java6178);
        castExpression();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred202_Java

    // $ANTLR start synpred206_Java
    public final void synpred206_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:1093:9: ( '(' primitiveType ')' unaryExpression )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:1093:9: '(' primitiveType ')' unaryExpression
        {
        dbg.location(1093,9);
        match(input,LPAREN,FOLLOW_LPAREN_in_synpred206_Java6268); if (state.failed) return ;
        dbg.location(1093,13);
        pushFollow(FOLLOW_primitiveType_in_synpred206_Java6270);
        primitiveType();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(1093,27);
        match(input,RPAREN,FOLLOW_RPAREN_in_synpred206_Java6272); if (state.failed) return ;
        dbg.location(1093,31);
        pushFollow(FOLLOW_unaryExpression_in_synpred206_Java6274);
        unaryExpression();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred206_Java

    // $ANTLR start synpred208_Java
    public final void synpred208_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:1103:10: ( '.' IDENTIFIER )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:1103:10: '.' IDENTIFIER
        {
        dbg.location(1103,10);
        match(input,DOT,FOLLOW_DOT_in_synpred208_Java6332); if (state.failed) return ;
        dbg.location(1103,14);
        match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_synpred208_Java6334); if (state.failed) return ;

        }
    }
    // $ANTLR end synpred208_Java

    // $ANTLR start synpred209_Java
    public final void synpred209_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:1105:10: ( identifierSuffix )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:1105:10: identifierSuffix
        {
        dbg.location(1105,10);
        pushFollow(FOLLOW_identifierSuffix_in_synpred209_Java6356);
        identifierSuffix();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred209_Java

    // $ANTLR start synpred211_Java
    public final void synpred211_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:1108:10: ( '.' IDENTIFIER )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:1108:10: '.' IDENTIFIER
        {
        dbg.location(1108,10);
        match(input,DOT,FOLLOW_DOT_in_synpred211_Java6388); if (state.failed) return ;
        dbg.location(1108,14);
        match(input,IDENTIFIER,FOLLOW_IDENTIFIER_in_synpred211_Java6390); if (state.failed) return ;

        }
    }
    // $ANTLR end synpred211_Java

    // $ANTLR start synpred212_Java
    public final void synpred212_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:1110:10: ( identifierSuffix )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:1110:10: identifierSuffix
        {
        dbg.location(1110,10);
        pushFollow(FOLLOW_identifierSuffix_in_synpred212_Java6412);
        identifierSuffix();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred212_Java

    // $ANTLR start synpred224_Java
    public final void synpred224_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:1138:10: ( '[' expression ']' )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:1138:10: '[' expression ']'
        {
        dbg.location(1138,10);
        match(input,LBRACKET,FOLLOW_LBRACKET_in_synpred224_Java6656); if (state.failed) return ;
        dbg.location(1138,14);
        pushFollow(FOLLOW_expression_in_synpred224_Java6658);
        expression();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(1138,25);
        match(input,RBRACKET,FOLLOW_RBRACKET_in_synpred224_Java6660); if (state.failed) return ;

        }
    }
    // $ANTLR end synpred224_Java

    // $ANTLR start synpred236_Java
    public final void synpred236_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:1161:9: ( 'new' nonWildcardTypeArguments classOrInterfaceType classCreatorRest )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:1161:9: 'new' nonWildcardTypeArguments classOrInterfaceType classCreatorRest
        {
        dbg.location(1161,9);
        match(input,NEW,FOLLOW_NEW_in_synpred236_Java6866); if (state.failed) return ;
        dbg.location(1161,15);
        pushFollow(FOLLOW_nonWildcardTypeArguments_in_synpred236_Java6868);
        nonWildcardTypeArguments();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(1161,40);
        pushFollow(FOLLOW_classOrInterfaceType_in_synpred236_Java6870);
        classOrInterfaceType();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(1161,61);
        pushFollow(FOLLOW_classCreatorRest_in_synpred236_Java6872);
        classCreatorRest();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred236_Java

    // $ANTLR start synpred237_Java
    public final void synpred237_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:1162:9: ( 'new' classOrInterfaceType classCreatorRest )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:1162:9: 'new' classOrInterfaceType classCreatorRest
        {
        dbg.location(1162,9);
        match(input,NEW,FOLLOW_NEW_in_synpred237_Java6882); if (state.failed) return ;
        dbg.location(1162,15);
        pushFollow(FOLLOW_classOrInterfaceType_in_synpred237_Java6884);
        classOrInterfaceType();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(1162,36);
        pushFollow(FOLLOW_classCreatorRest_in_synpred237_Java6886);
        classCreatorRest();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred237_Java

    // $ANTLR start synpred239_Java
    public final void synpred239_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:1167:9: ( 'new' createdName '[' ']' ( '[' ']' )* arrayInitializer )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:1167:9: 'new' createdName '[' ']' ( '[' ']' )* arrayInitializer
        {
        dbg.location(1167,9);
        match(input,NEW,FOLLOW_NEW_in_synpred239_Java6915); if (state.failed) return ;
        dbg.location(1167,15);
        pushFollow(FOLLOW_createdName_in_synpred239_Java6917);
        createdName();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(1168,9);
        match(input,LBRACKET,FOLLOW_LBRACKET_in_synpred239_Java6927); if (state.failed) return ;
        dbg.location(1168,13);
        match(input,RBRACKET,FOLLOW_RBRACKET_in_synpred239_Java6929); if (state.failed) return ;
        dbg.location(1169,9);
        // src/com/google/doclava/parser/Java.g:1169:9: ( '[' ']' )*
        try { dbg.enterSubRule(193);

        loop193:
        do {
            int alt193=2;
            try { dbg.enterDecision(193, decisionCanBacktrack[193]);

            int LA193_0 = input.LA(1);

            if ( (LA193_0==LBRACKET) ) {
                alt193=1;
            }


            } finally {dbg.exitDecision(193);}

            switch (alt193) {
		case 1 :
		    dbg.enterAlt(1);

		    // src/com/google/doclava/parser/Java.g:1169:10: '[' ']'
		    {
		    dbg.location(1169,10);
		    match(input,LBRACKET,FOLLOW_LBRACKET_in_synpred239_Java6940); if (state.failed) return ;
		    dbg.location(1169,14);
		    match(input,RBRACKET,FOLLOW_RBRACKET_in_synpred239_Java6942); if (state.failed) return ;

		    }
		    break;

		default :
		    break loop193;
            }
        } while (true);
        } finally {dbg.exitSubRule(193);}

        dbg.location(1171,9);
        pushFollow(FOLLOW_arrayInitializer_in_synpred239_Java6963);
        arrayInitializer();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred239_Java

    // $ANTLR start synpred240_Java
    public final void synpred240_Java_fragment() throws RecognitionException {
        // src/com/google/doclava/parser/Java.g:1176:13: ( '[' expression ']' )
        dbg.enterAlt(1);

        // src/com/google/doclava/parser/Java.g:1176:13: '[' expression ']'
        {
        dbg.location(1176,13);
        match(input,LBRACKET,FOLLOW_LBRACKET_in_synpred240_Java7012); if (state.failed) return ;
        dbg.location(1176,17);
        pushFollow(FOLLOW_expression_in_synpred240_Java7014);
        expression();

        state._fsp--;
        if (state.failed) return ;
        dbg.location(1177,13);
        match(input,RBRACKET,FOLLOW_RBRACKET_in_synpred240_Java7028); if (state.failed) return ;

        }
    }
    // $ANTLR end synpred240_Java

    // Delegated rules

    public final boolean synpred43_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred43_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred98_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred98_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred157_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred157_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred224_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred224_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred211_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred211_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred121_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred121_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred239_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred239_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred69_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred69_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred202_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred202_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred154_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred154_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred71_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred71_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred133_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred133_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred125_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred125_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred132_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred132_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred119_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred119_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred54_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred54_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred148_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred148_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred117_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred117_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred2_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred2_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred130_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred130_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred126_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred126_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred59_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred59_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred212_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred212_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred161_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred161_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred57_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred57_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred209_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred209_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred68_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred68_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred53_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred53_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred52_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred52_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred236_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred236_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred12_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred12_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred149_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred149_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred120_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred120_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred122_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred122_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred240_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred240_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred206_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred206_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred70_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred70_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred27_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred27_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred96_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred96_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred153_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred153_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred99_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred99_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred103_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred103_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred237_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred237_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred118_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred118_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred208_Java() {
        state.backtracking++;
        dbg.beginBacktrack(state.backtracking);
        int start = input.mark();
        try {
            synpred208_Java_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        dbg.endBacktrack(state.backtracking, success);
        state.backtracking--;
        state.failed=false;
        return success;
    }


    protected DFA2 dfa2 = new DFA2(this);
    protected DFA12 dfa12 = new DFA12(this);
    protected DFA13 dfa13 = new DFA13(this);
    protected DFA15 dfa15 = new DFA15(this);
    protected DFA31 dfa31 = new DFA31(this);
    protected DFA39 dfa39 = new DFA39(this);
    protected DFA49 dfa49 = new DFA49(this);
    protected DFA42 dfa42 = new DFA42(this);
    protected DFA53 dfa53 = new DFA53(this);
    protected DFA76 dfa76 = new DFA76(this);
    protected DFA87 dfa87 = new DFA87(this);
    protected DFA90 dfa90 = new DFA90(this);
    protected DFA98 dfa98 = new DFA98(this);
    protected DFA109 dfa109 = new DFA109(this);
    protected DFA112 dfa112 = new DFA112(this);
    protected DFA130 dfa130 = new DFA130(this);
    protected DFA133 dfa133 = new DFA133(this);
    protected DFA135 dfa135 = new DFA135(this);
    protected DFA143 dfa143 = new DFA143(this);
    protected DFA142 dfa142 = new DFA142(this);
    protected DFA148 dfa148 = new DFA148(this);
    protected DFA171 dfa171 = new DFA171(this);
    static final String DFA2_eotS =
        "\24\uffff";
    static final String DFA2_eofS =
        "\1\3\23\uffff";
    static final String DFA2_minS =
        "\1\34\1\0\22\uffff";
    static final String DFA2_maxS =
        "\1\162\1\0\22\uffff";
    static final String DFA2_acceptS =
        "\2\uffff\1\1\1\2\20\uffff";
    static final String DFA2_specialS =
        "\1\uffff\1\0\22\uffff}>";
    static final String[] DFA2_transitionS = {
            "\1\3\7\uffff\1\3\6\uffff\1\3\1\uffff\1\3\6\uffff\1\3\2\uffff"+
            "\1\3\1\uffff\1\3\1\uffff\1\2\3\3\2\uffff\2\3\2\uffff\1\3\3\uffff"+
            "\1\3\2\uffff\1\3\7\uffff\1\3\35\uffff\1\1",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA2_eot = DFA.unpackEncodedString(DFA2_eotS);
    static final short[] DFA2_eof = DFA.unpackEncodedString(DFA2_eofS);
    static final char[] DFA2_min = DFA.unpackEncodedStringToUnsignedChars(DFA2_minS);
    static final char[] DFA2_max = DFA.unpackEncodedStringToUnsignedChars(DFA2_maxS);
    static final short[] DFA2_accept = DFA.unpackEncodedString(DFA2_acceptS);
    static final short[] DFA2_special = DFA.unpackEncodedString(DFA2_specialS);
    static final short[][] DFA2_transition;

    static {
        int numStates = DFA2_transitionS.length;
        DFA2_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA2_transition[i] = DFA.unpackEncodedString(DFA2_transitionS[i]);
        }
    }

    class DFA2 extends DFA {

        public DFA2(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 2;
            this.eot = DFA2_eot;
            this.eof = DFA2_eof;
            this.min = DFA2_min;
            this.max = DFA2_max;
            this.accept = DFA2_accept;
            this.special = DFA2_special;
            this.transition = DFA2_transition;
        }
        public String getDescription() {
            return "298:9: ( ( annotations )? packageDeclaration )?";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA2_1 = input.LA(1);


                        int index2_1 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred2_Java()) ) {s = 2;}

                        else if ( (true) ) {s = 3;}


                        input.seek(index2_1);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 2, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA12_eotS =
        "\20\uffff";
    static final String DFA12_eofS =
        "\20\uffff";
    static final String DFA12_minS =
        "\1\34\14\0\3\uffff";
    static final String DFA12_maxS =
        "\1\162\14\0\3\uffff";
    static final String DFA12_acceptS =
        "\15\uffff\1\1\1\uffff\1\2";
    static final String DFA12_specialS =
        "\1\uffff\1\0\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\3\uffff}>";
    static final String[] DFA12_transitionS = {
            "\1\6\7\uffff\1\15\6\uffff\1\15\1\uffff\1\7\11\uffff\1\17\1\uffff"+
            "\1\10\2\uffff\1\4\1\3\1\2\2\uffff\1\5\1\14\2\uffff\1\11\3\uffff"+
            "\1\12\2\uffff\1\13\45\uffff\1\1",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "",
            "",
            ""
    };

    static final short[] DFA12_eot = DFA.unpackEncodedString(DFA12_eotS);
    static final short[] DFA12_eof = DFA.unpackEncodedString(DFA12_eofS);
    static final char[] DFA12_min = DFA.unpackEncodedStringToUnsignedChars(DFA12_minS);
    static final char[] DFA12_max = DFA.unpackEncodedStringToUnsignedChars(DFA12_maxS);
    static final short[] DFA12_accept = DFA.unpackEncodedString(DFA12_acceptS);
    static final short[] DFA12_special = DFA.unpackEncodedString(DFA12_specialS);
    static final short[][] DFA12_transition;

    static {
        int numStates = DFA12_transitionS.length;
        DFA12_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA12_transition[i] = DFA.unpackEncodedString(DFA12_transitionS[i]);
        }
    }

    class DFA12 extends DFA {

        public DFA12(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 12;
            this.eot = DFA12_eot;
            this.eof = DFA12_eof;
            this.min = DFA12_min;
            this.max = DFA12_max;
            this.accept = DFA12_accept;
            this.special = DFA12_special;
            this.transition = DFA12_transition;
        }
        public String getDescription() {
            return "341:1: classOrInterfaceDeclaration : ( classDeclaration | interfaceDeclaration );";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA12_1 = input.LA(1);


                        int index12_1 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred12_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index12_1);
                        if ( s>=0 ) return s;
                        break;
                    case 1 :
                        int LA12_2 = input.LA(1);


                        int index12_2 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred12_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index12_2);
                        if ( s>=0 ) return s;
                        break;
                    case 2 :
                        int LA12_3 = input.LA(1);


                        int index12_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred12_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index12_3);
                        if ( s>=0 ) return s;
                        break;
                    case 3 :
                        int LA12_4 = input.LA(1);


                        int index12_4 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred12_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index12_4);
                        if ( s>=0 ) return s;
                        break;
                    case 4 :
                        int LA12_5 = input.LA(1);


                        int index12_5 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred12_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index12_5);
                        if ( s>=0 ) return s;
                        break;
                    case 5 :
                        int LA12_6 = input.LA(1);


                        int index12_6 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred12_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index12_6);
                        if ( s>=0 ) return s;
                        break;
                    case 6 :
                        int LA12_7 = input.LA(1);


                        int index12_7 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred12_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index12_7);
                        if ( s>=0 ) return s;
                        break;
                    case 7 :
                        int LA12_8 = input.LA(1);


                        int index12_8 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred12_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index12_8);
                        if ( s>=0 ) return s;
                        break;
                    case 8 :
                        int LA12_9 = input.LA(1);


                        int index12_9 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred12_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index12_9);
                        if ( s>=0 ) return s;
                        break;
                    case 9 :
                        int LA12_10 = input.LA(1);


                        int index12_10 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred12_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index12_10);
                        if ( s>=0 ) return s;
                        break;
                    case 10 :
                        int LA12_11 = input.LA(1);


                        int index12_11 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred12_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index12_11);
                        if ( s>=0 ) return s;
                        break;
                    case 11 :
                        int LA12_12 = input.LA(1);


                        int index12_12 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred12_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index12_12);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 12, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA13_eotS =
        "\17\uffff";
    static final String DFA13_eofS =
        "\17\uffff";
    static final String DFA13_minS =
        "\1\4\1\uffff\1\4\14\uffff";
    static final String DFA13_maxS =
        "\1\165\1\uffff\1\67\14\uffff";
    static final String DFA13_acceptS =
        "\1\uffff\1\15\1\uffff\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13"+
        "\1\14\1\1";
    static final String DFA13_specialS =
        "\17\uffff}>";
    static final String[] DFA13_transitionS = {
            "\1\1\27\uffff\1\7\1\uffff\1\1\1\uffff\1\1\2\uffff\2\1\4\uffff"+
            "\1\1\1\uffff\1\1\1\uffff\1\10\1\uffff\1\1\6\uffff\3\1\1\11\2"+
            "\uffff\1\5\1\4\1\3\1\uffff\1\1\1\6\1\15\2\uffff\1\12\3\uffff"+
            "\1\13\1\uffff\1\1\1\14\45\uffff\1\2\2\uffff\1\1",
            "",
            "\1\16\62\uffff\1\1",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA13_eot = DFA.unpackEncodedString(DFA13_eotS);
    static final short[] DFA13_eof = DFA.unpackEncodedString(DFA13_eofS);
    static final char[] DFA13_min = DFA.unpackEncodedStringToUnsignedChars(DFA13_minS);
    static final char[] DFA13_max = DFA.unpackEncodedStringToUnsignedChars(DFA13_maxS);
    static final short[] DFA13_accept = DFA.unpackEncodedString(DFA13_acceptS);
    static final short[] DFA13_special = DFA.unpackEncodedString(DFA13_specialS);
    static final short[][] DFA13_transition;

    static {
        int numStates = DFA13_transitionS.length;
        DFA13_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA13_transition[i] = DFA.unpackEncodedString(DFA13_transitionS[i]);
        }
    }

    class DFA13 extends DFA {

        public DFA13(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 13;
            this.eot = DFA13_eot;
            this.eof = DFA13_eof;
            this.min = DFA13_min;
            this.max = DFA13_max;
            this.accept = DFA13_accept;
            this.special = DFA13_special;
            this.transition = DFA13_transition;
        }
        public String getDescription() {
            return "()* loopback of 349:5: ( annotation | 'public' | 'protected' | 'private' | 'static' | 'abstract' | 'final' | 'native' | 'synchronized' | 'transient' | 'volatile' | 'strictfp' )*";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
    }
    static final String DFA15_eotS =
        "\17\uffff";
    static final String DFA15_eofS =
        "\17\uffff";
    static final String DFA15_minS =
        "\1\34\14\0\2\uffff";
    static final String DFA15_maxS =
        "\1\162\14\0\2\uffff";
    static final String DFA15_acceptS =
        "\15\uffff\1\1\1\2";
    static final String DFA15_specialS =
        "\1\uffff\1\0\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\2\uffff}>";
    static final String[] DFA15_transitionS = {
            "\1\6\7\uffff\1\15\6\uffff\1\16\1\uffff\1\7\13\uffff\1\10\2\uffff"+
            "\1\4\1\3\1\2\2\uffff\1\5\1\14\2\uffff\1\11\3\uffff\1\12\2\uffff"+
            "\1\13\45\uffff\1\1",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "",
            ""
    };

    static final short[] DFA15_eot = DFA.unpackEncodedString(DFA15_eotS);
    static final short[] DFA15_eof = DFA.unpackEncodedString(DFA15_eofS);
    static final char[] DFA15_min = DFA.unpackEncodedStringToUnsignedChars(DFA15_minS);
    static final char[] DFA15_max = DFA.unpackEncodedStringToUnsignedChars(DFA15_maxS);
    static final short[] DFA15_accept = DFA.unpackEncodedString(DFA15_acceptS);
    static final short[] DFA15_special = DFA.unpackEncodedString(DFA15_specialS);
    static final short[][] DFA15_transition;

    static {
        int numStates = DFA15_transitionS.length;
        DFA15_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA15_transition[i] = DFA.unpackEncodedString(DFA15_transitionS[i]);
        }
    }

    class DFA15 extends DFA {

        public DFA15(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 15;
            this.eot = DFA15_eot;
            this.eof = DFA15_eof;
            this.min = DFA15_min;
            this.max = DFA15_max;
            this.accept = DFA15_accept;
            this.special = DFA15_special;
            this.transition = DFA15_transition;
        }
        public String getDescription() {
            return "372:1: classDeclaration : ( normalClassDeclaration | enumDeclaration );";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA15_1 = input.LA(1);


                        int index15_1 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred27_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index15_1);
                        if ( s>=0 ) return s;
                        break;
                    case 1 :
                        int LA15_2 = input.LA(1);


                        int index15_2 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred27_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index15_2);
                        if ( s>=0 ) return s;
                        break;
                    case 2 :
                        int LA15_3 = input.LA(1);


                        int index15_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred27_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index15_3);
                        if ( s>=0 ) return s;
                        break;
                    case 3 :
                        int LA15_4 = input.LA(1);


                        int index15_4 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred27_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index15_4);
                        if ( s>=0 ) return s;
                        break;
                    case 4 :
                        int LA15_5 = input.LA(1);


                        int index15_5 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred27_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index15_5);
                        if ( s>=0 ) return s;
                        break;
                    case 5 :
                        int LA15_6 = input.LA(1);


                        int index15_6 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred27_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index15_6);
                        if ( s>=0 ) return s;
                        break;
                    case 6 :
                        int LA15_7 = input.LA(1);


                        int index15_7 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred27_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index15_7);
                        if ( s>=0 ) return s;
                        break;
                    case 7 :
                        int LA15_8 = input.LA(1);


                        int index15_8 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred27_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index15_8);
                        if ( s>=0 ) return s;
                        break;
                    case 8 :
                        int LA15_9 = input.LA(1);


                        int index15_9 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred27_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index15_9);
                        if ( s>=0 ) return s;
                        break;
                    case 9 :
                        int LA15_10 = input.LA(1);


                        int index15_10 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred27_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index15_10);
                        if ( s>=0 ) return s;
                        break;
                    case 10 :
                        int LA15_11 = input.LA(1);


                        int index15_11 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred27_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index15_11);
                        if ( s>=0 ) return s;
                        break;
                    case 11 :
                        int LA15_12 = input.LA(1);


                        int index15_12 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred27_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index15_12);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 15, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA31_eotS =
        "\17\uffff";
    static final String DFA31_eofS =
        "\17\uffff";
    static final String DFA31_minS =
        "\1\34\14\0\2\uffff";
    static final String DFA31_maxS =
        "\1\162\14\0\2\uffff";
    static final String DFA31_acceptS =
        "\15\uffff\1\1\1\2";
    static final String DFA31_specialS =
        "\1\uffff\1\0\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\2\uffff}>";
    static final String[] DFA31_transitionS = {
            "\1\6\20\uffff\1\7\11\uffff\1\15\1\uffff\1\10\2\uffff\1\4\1\3"+
            "\1\2\2\uffff\1\5\1\14\2\uffff\1\11\3\uffff\1\12\2\uffff\1\13"+
            "\45\uffff\1\1",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "",
            ""
    };

    static final short[] DFA31_eot = DFA.unpackEncodedString(DFA31_eotS);
    static final short[] DFA31_eof = DFA.unpackEncodedString(DFA31_eofS);
    static final char[] DFA31_min = DFA.unpackEncodedStringToUnsignedChars(DFA31_minS);
    static final char[] DFA31_max = DFA.unpackEncodedStringToUnsignedChars(DFA31_maxS);
    static final short[] DFA31_accept = DFA.unpackEncodedString(DFA31_acceptS);
    static final short[] DFA31_special = DFA.unpackEncodedString(DFA31_specialS);
    static final short[][] DFA31_transition;

    static {
        int numStates = DFA31_transitionS.length;
        DFA31_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA31_transition[i] = DFA.unpackEncodedString(DFA31_transitionS[i]);
        }
    }

    class DFA31 extends DFA {

        public DFA31(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 31;
            this.eot = DFA31_eot;
            this.eof = DFA31_eof;
            this.min = DFA31_min;
            this.max = DFA31_max;
            this.accept = DFA31_accept;
            this.special = DFA31_special;
            this.transition = DFA31_transition;
        }
        public String getDescription() {
            return "460:1: interfaceDeclaration : ( normalInterfaceDeclaration | annotationTypeDeclaration );";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA31_1 = input.LA(1);


                        int index31_1 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred43_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index31_1);
                        if ( s>=0 ) return s;
                        break;
                    case 1 :
                        int LA31_2 = input.LA(1);


                        int index31_2 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred43_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index31_2);
                        if ( s>=0 ) return s;
                        break;
                    case 2 :
                        int LA31_3 = input.LA(1);


                        int index31_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred43_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index31_3);
                        if ( s>=0 ) return s;
                        break;
                    case 3 :
                        int LA31_4 = input.LA(1);


                        int index31_4 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred43_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index31_4);
                        if ( s>=0 ) return s;
                        break;
                    case 4 :
                        int LA31_5 = input.LA(1);


                        int index31_5 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred43_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index31_5);
                        if ( s>=0 ) return s;
                        break;
                    case 5 :
                        int LA31_6 = input.LA(1);


                        int index31_6 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred43_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index31_6);
                        if ( s>=0 ) return s;
                        break;
                    case 6 :
                        int LA31_7 = input.LA(1);


                        int index31_7 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred43_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index31_7);
                        if ( s>=0 ) return s;
                        break;
                    case 7 :
                        int LA31_8 = input.LA(1);


                        int index31_8 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred43_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index31_8);
                        if ( s>=0 ) return s;
                        break;
                    case 8 :
                        int LA31_9 = input.LA(1);


                        int index31_9 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred43_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index31_9);
                        if ( s>=0 ) return s;
                        break;
                    case 9 :
                        int LA31_10 = input.LA(1);


                        int index31_10 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred43_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index31_10);
                        if ( s>=0 ) return s;
                        break;
                    case 10 :
                        int LA31_11 = input.LA(1);


                        int index31_11 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred43_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index31_11);
                        if ( s>=0 ) return s;
                        break;
                    case 11 :
                        int LA31_12 = input.LA(1);


                        int index31_12 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred43_Java()) ) {s = 13;}

                        else if ( (true) ) {s = 14;}


                        input.seek(index31_12);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 31, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA39_eotS =
        "\25\uffff";
    static final String DFA39_eofS =
        "\25\uffff";
    static final String DFA39_minS =
        "\1\4\16\0\6\uffff";
    static final String DFA39_maxS =
        "\1\165\16\0\6\uffff";
    static final String DFA39_acceptS =
        "\17\uffff\1\2\1\uffff\1\3\1\uffff\1\4\1\1";
    static final String DFA39_specialS =
        "\1\uffff\1\0\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14"+
        "\1\15\6\uffff}>";
    static final String[] DFA39_transitionS = {
            "\1\15\27\uffff\1\6\1\uffff\1\16\1\uffff\1\16\2\uffff\1\16\1"+
            "\21\4\uffff\1\16\1\uffff\1\21\1\uffff\1\7\1\uffff\1\16\6\uffff"+
            "\1\16\1\23\1\16\1\10\2\uffff\1\4\1\3\1\2\1\uffff\1\16\1\5\1"+
            "\14\2\uffff\1\11\3\uffff\1\12\1\uffff\1\17\1\13\45\uffff\1\1"+
            "\2\uffff\1\17",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA39_eot = DFA.unpackEncodedString(DFA39_eotS);
    static final short[] DFA39_eof = DFA.unpackEncodedString(DFA39_eofS);
    static final char[] DFA39_min = DFA.unpackEncodedStringToUnsignedChars(DFA39_minS);
    static final char[] DFA39_max = DFA.unpackEncodedStringToUnsignedChars(DFA39_maxS);
    static final short[] DFA39_accept = DFA.unpackEncodedString(DFA39_acceptS);
    static final short[] DFA39_special = DFA.unpackEncodedString(DFA39_specialS);
    static final short[][] DFA39_transition;

    static {
        int numStates = DFA39_transitionS.length;
        DFA39_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA39_transition[i] = DFA.unpackEncodedString(DFA39_transitionS[i]);
        }
    }

    class DFA39 extends DFA {

        public DFA39(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 39;
            this.eot = DFA39_eot;
            this.eof = DFA39_eof;
            this.min = DFA39_min;
            this.max = DFA39_max;
            this.accept = DFA39_accept;
            this.special = DFA39_special;
            this.transition = DFA39_transition;
        }
        public String getDescription() {
            return "502:1: memberDecl : ( fieldDeclaration | methodDeclaration | classDeclaration | interfaceDeclaration );";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA39_1 = input.LA(1);


                        int index39_1 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred52_Java()) ) {s = 20;}

                        else if ( (synpred53_Java()) ) {s = 15;}

                        else if ( (synpred54_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 19;}


                        input.seek(index39_1);
                        if ( s>=0 ) return s;
                        break;
                    case 1 :
                        int LA39_2 = input.LA(1);


                        int index39_2 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred52_Java()) ) {s = 20;}

                        else if ( (synpred53_Java()) ) {s = 15;}

                        else if ( (synpred54_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 19;}


                        input.seek(index39_2);
                        if ( s>=0 ) return s;
                        break;
                    case 2 :
                        int LA39_3 = input.LA(1);


                        int index39_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred52_Java()) ) {s = 20;}

                        else if ( (synpred53_Java()) ) {s = 15;}

                        else if ( (synpred54_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 19;}


                        input.seek(index39_3);
                        if ( s>=0 ) return s;
                        break;
                    case 3 :
                        int LA39_4 = input.LA(1);


                        int index39_4 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred52_Java()) ) {s = 20;}

                        else if ( (synpred53_Java()) ) {s = 15;}

                        else if ( (synpred54_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 19;}


                        input.seek(index39_4);
                        if ( s>=0 ) return s;
                        break;
                    case 4 :
                        int LA39_5 = input.LA(1);


                        int index39_5 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred52_Java()) ) {s = 20;}

                        else if ( (synpred53_Java()) ) {s = 15;}

                        else if ( (synpred54_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 19;}


                        input.seek(index39_5);
                        if ( s>=0 ) return s;
                        break;
                    case 5 :
                        int LA39_6 = input.LA(1);


                        int index39_6 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred52_Java()) ) {s = 20;}

                        else if ( (synpred53_Java()) ) {s = 15;}

                        else if ( (synpred54_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 19;}


                        input.seek(index39_6);
                        if ( s>=0 ) return s;
                        break;
                    case 6 :
                        int LA39_7 = input.LA(1);


                        int index39_7 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred52_Java()) ) {s = 20;}

                        else if ( (synpred53_Java()) ) {s = 15;}

                        else if ( (synpred54_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 19;}


                        input.seek(index39_7);
                        if ( s>=0 ) return s;
                        break;
                    case 7 :
                        int LA39_8 = input.LA(1);


                        int index39_8 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred52_Java()) ) {s = 20;}

                        else if ( (synpred53_Java()) ) {s = 15;}

                        else if ( (synpred54_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 19;}


                        input.seek(index39_8);
                        if ( s>=0 ) return s;
                        break;
                    case 8 :
                        int LA39_9 = input.LA(1);


                        int index39_9 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred52_Java()) ) {s = 20;}

                        else if ( (synpred53_Java()) ) {s = 15;}

                        else if ( (synpred54_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 19;}


                        input.seek(index39_9);
                        if ( s>=0 ) return s;
                        break;
                    case 9 :
                        int LA39_10 = input.LA(1);


                        int index39_10 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred52_Java()) ) {s = 20;}

                        else if ( (synpred53_Java()) ) {s = 15;}

                        else if ( (synpred54_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 19;}


                        input.seek(index39_10);
                        if ( s>=0 ) return s;
                        break;
                    case 10 :
                        int LA39_11 = input.LA(1);


                        int index39_11 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred52_Java()) ) {s = 20;}

                        else if ( (synpred53_Java()) ) {s = 15;}

                        else if ( (synpred54_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 19;}


                        input.seek(index39_11);
                        if ( s>=0 ) return s;
                        break;
                    case 11 :
                        int LA39_12 = input.LA(1);


                        int index39_12 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred52_Java()) ) {s = 20;}

                        else if ( (synpred53_Java()) ) {s = 15;}

                        else if ( (synpred54_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 19;}


                        input.seek(index39_12);
                        if ( s>=0 ) return s;
                        break;
                    case 12 :
                        int LA39_13 = input.LA(1);


                        int index39_13 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred52_Java()) ) {s = 20;}

                        else if ( (synpred53_Java()) ) {s = 15;}


                        input.seek(index39_13);
                        if ( s>=0 ) return s;
                        break;
                    case 13 :
                        int LA39_14 = input.LA(1);


                        int index39_14 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred52_Java()) ) {s = 20;}

                        else if ( (synpred53_Java()) ) {s = 15;}


                        input.seek(index39_14);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 39, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA49_eotS =
        "\22\uffff";
    static final String DFA49_eofS =
        "\22\uffff";
    static final String DFA49_minS =
        "\1\4\16\0\3\uffff";
    static final String DFA49_maxS =
        "\1\165\16\0\3\uffff";
    static final String DFA49_acceptS =
        "\17\uffff\1\2\1\uffff\1\1";
    static final String DFA49_specialS =
        "\1\uffff\1\0\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14"+
        "\1\15\3\uffff}>";
    static final String[] DFA49_transitionS = {
            "\1\16\27\uffff\1\6\1\uffff\1\17\1\uffff\1\17\2\uffff\1\17\5"+
            "\uffff\1\17\3\uffff\1\7\1\uffff\1\17\6\uffff\1\17\1\uffff\1"+
            "\17\1\10\2\uffff\1\4\1\3\1\2\1\uffff\1\17\1\5\1\14\2\uffff\1"+
            "\11\3\uffff\1\12\1\uffff\1\17\1\13\45\uffff\1\1\2\uffff\1\15",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "",
            "",
            ""
    };

    static final short[] DFA49_eot = DFA.unpackEncodedString(DFA49_eotS);
    static final short[] DFA49_eof = DFA.unpackEncodedString(DFA49_eofS);
    static final char[] DFA49_min = DFA.unpackEncodedStringToUnsignedChars(DFA49_minS);
    static final char[] DFA49_max = DFA.unpackEncodedStringToUnsignedChars(DFA49_maxS);
    static final short[] DFA49_accept = DFA.unpackEncodedString(DFA49_acceptS);
    static final short[] DFA49_special = DFA.unpackEncodedString(DFA49_specialS);
    static final short[][] DFA49_transition;

    static {
        int numStates = DFA49_transitionS.length;
        DFA49_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA49_transition[i] = DFA.unpackEncodedString(DFA49_transitionS[i]);
        }
    }

    class DFA49 extends DFA {

        public DFA49(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 49;
            this.eot = DFA49_eot;
            this.eof = DFA49_eof;
            this.min = DFA49_min;
            this.max = DFA49_max;
            this.accept = DFA49_accept;
            this.special = DFA49_special;
            this.transition = DFA49_transition;
        }
        public String getDescription() {
            return "510:1: methodDeclaration : ( modifiers ( typeParameters )? IDENTIFIER formalParameters ( 'throws' qualifiedNameList )? '{' ( explicitConstructorInvocation )? ( blockStatement )* '}' | modifiers ( typeParameters )? ( type | 'void' ) IDENTIFIER formalParameters ( '[' ']' )* ( 'throws' qualifiedNameList )? ( block | ';' ) );";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA49_1 = input.LA(1);


                        int index49_1 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred59_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index49_1);
                        if ( s>=0 ) return s;
                        break;
                    case 1 :
                        int LA49_2 = input.LA(1);


                        int index49_2 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred59_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index49_2);
                        if ( s>=0 ) return s;
                        break;
                    case 2 :
                        int LA49_3 = input.LA(1);


                        int index49_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred59_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index49_3);
                        if ( s>=0 ) return s;
                        break;
                    case 3 :
                        int LA49_4 = input.LA(1);


                        int index49_4 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred59_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index49_4);
                        if ( s>=0 ) return s;
                        break;
                    case 4 :
                        int LA49_5 = input.LA(1);


                        int index49_5 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred59_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index49_5);
                        if ( s>=0 ) return s;
                        break;
                    case 5 :
                        int LA49_6 = input.LA(1);


                        int index49_6 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred59_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index49_6);
                        if ( s>=0 ) return s;
                        break;
                    case 6 :
                        int LA49_7 = input.LA(1);


                        int index49_7 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred59_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index49_7);
                        if ( s>=0 ) return s;
                        break;
                    case 7 :
                        int LA49_8 = input.LA(1);


                        int index49_8 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred59_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index49_8);
                        if ( s>=0 ) return s;
                        break;
                    case 8 :
                        int LA49_9 = input.LA(1);


                        int index49_9 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred59_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index49_9);
                        if ( s>=0 ) return s;
                        break;
                    case 9 :
                        int LA49_10 = input.LA(1);


                        int index49_10 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred59_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index49_10);
                        if ( s>=0 ) return s;
                        break;
                    case 10 :
                        int LA49_11 = input.LA(1);


                        int index49_11 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred59_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index49_11);
                        if ( s>=0 ) return s;
                        break;
                    case 11 :
                        int LA49_12 = input.LA(1);


                        int index49_12 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred59_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index49_12);
                        if ( s>=0 ) return s;
                        break;
                    case 12 :
                        int LA49_13 = input.LA(1);


                        int index49_13 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred59_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index49_13);
                        if ( s>=0 ) return s;
                        break;
                    case 13 :
                        int LA49_14 = input.LA(1);


                        int index49_14 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred59_Java()) ) {s = 17;}

                        else if ( (true) ) {s = 15;}


                        input.seek(index49_14);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 49, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA42_eotS =
        "\55\uffff";
    static final String DFA42_eofS =
        "\55\uffff";
    static final String DFA42_minS =
        "\1\4\1\uffff\10\0\43\uffff";
    static final String DFA42_maxS =
        "\1\165\1\uffff\10\0\43\uffff";
    static final String DFA42_acceptS =
        "\1\uffff\1\1\10\uffff\1\2\42\uffff";
    static final String DFA42_specialS =
        "\2\uffff\1\0\1\1\1\2\1\3\1\4\1\5\1\6\1\7\43\uffff}>";
    static final String[] DFA42_transitionS = {
            "\1\5\11\6\16\uffff\2\12\1\10\1\12\1\10\2\uffff\1\10\1\12\1\uffff"+
            "\1\12\1\uffff\1\12\1\10\1\uffff\1\12\1\uffff\1\12\1\uffff\1"+
            "\10\1\12\1\uffff\1\12\3\uffff\1\10\1\12\1\10\1\12\1\7\1\uffff"+
            "\4\12\1\10\2\12\1\4\2\12\1\2\1\12\1\uffff\2\12\1\11\2\12\1\3"+
            "\1\uffff\2\12\2\uffff\1\12\4\uffff\2\12\5\uffff\4\12\16\uffff"+
            "\1\12\2\uffff\1\1",
            "",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA42_eot = DFA.unpackEncodedString(DFA42_eotS);
    static final short[] DFA42_eof = DFA.unpackEncodedString(DFA42_eofS);
    static final char[] DFA42_min = DFA.unpackEncodedStringToUnsignedChars(DFA42_minS);
    static final char[] DFA42_max = DFA.unpackEncodedStringToUnsignedChars(DFA42_maxS);
    static final short[] DFA42_accept = DFA.unpackEncodedString(DFA42_acceptS);
    static final short[] DFA42_special = DFA.unpackEncodedString(DFA42_specialS);
    static final short[][] DFA42_transition;

    static {
        int numStates = DFA42_transitionS.length;
        DFA42_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA42_transition[i] = DFA.unpackEncodedString(DFA42_transitionS[i]);
        }
    }

    class DFA42 extends DFA {

        public DFA42(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 42;
            this.eot = DFA42_eot;
            this.eof = DFA42_eof;
            this.min = DFA42_min;
            this.max = DFA42_max;
            this.accept = DFA42_accept;
            this.special = DFA42_special;
            this.transition = DFA42_transition;
        }
        public String getDescription() {
            return "521:9: ( explicitConstructorInvocation )?";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA42_2 = input.LA(1);


                        int index42_2 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred57_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 10;}


                        input.seek(index42_2);
                        if ( s>=0 ) return s;
                        break;
                    case 1 :
                        int LA42_3 = input.LA(1);


                        int index42_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred57_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 10;}


                        input.seek(index42_3);
                        if ( s>=0 ) return s;
                        break;
                    case 2 :
                        int LA42_4 = input.LA(1);


                        int index42_4 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred57_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 10;}


                        input.seek(index42_4);
                        if ( s>=0 ) return s;
                        break;
                    case 3 :
                        int LA42_5 = input.LA(1);


                        int index42_5 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred57_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 10;}


                        input.seek(index42_5);
                        if ( s>=0 ) return s;
                        break;
                    case 4 :
                        int LA42_6 = input.LA(1);


                        int index42_6 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred57_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 10;}


                        input.seek(index42_6);
                        if ( s>=0 ) return s;
                        break;
                    case 5 :
                        int LA42_7 = input.LA(1);


                        int index42_7 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred57_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 10;}


                        input.seek(index42_7);
                        if ( s>=0 ) return s;
                        break;
                    case 6 :
                        int LA42_8 = input.LA(1);


                        int index42_8 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred57_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 10;}


                        input.seek(index42_8);
                        if ( s>=0 ) return s;
                        break;
                    case 7 :
                        int LA42_9 = input.LA(1);


                        int index42_9 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred57_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 10;}


                        input.seek(index42_9);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 42, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA53_eotS =
        "\26\uffff";
    static final String DFA53_eofS =
        "\26\uffff";
    static final String DFA53_minS =
        "\1\4\16\0\7\uffff";
    static final String DFA53_maxS =
        "\1\165\16\0\7\uffff";
    static final String DFA53_acceptS =
        "\17\uffff\1\2\1\uffff\1\3\1\4\1\uffff\1\5\1\1";
    static final String DFA53_specialS =
        "\1\uffff\1\0\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14"+
        "\1\15\7\uffff}>";
    static final String[] DFA53_transitionS = {
            "\1\15\27\uffff\1\6\1\uffff\1\16\1\uffff\1\16\2\uffff\1\16\1"+
            "\22\4\uffff\1\16\1\uffff\1\22\1\uffff\1\7\1\uffff\1\16\6\uffff"+
            "\1\16\1\21\1\16\1\10\2\uffff\1\4\1\3\1\2\1\uffff\1\16\1\5\1"+
            "\14\2\uffff\1\11\3\uffff\1\12\1\uffff\1\17\1\13\7\uffff\1\24"+
            "\35\uffff\1\1\2\uffff\1\17",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA53_eot = DFA.unpackEncodedString(DFA53_eotS);
    static final short[] DFA53_eof = DFA.unpackEncodedString(DFA53_eofS);
    static final char[] DFA53_min = DFA.unpackEncodedStringToUnsignedChars(DFA53_minS);
    static final char[] DFA53_max = DFA.unpackEncodedStringToUnsignedChars(DFA53_maxS);
    static final short[] DFA53_accept = DFA.unpackEncodedString(DFA53_acceptS);
    static final short[] DFA53_special = DFA.unpackEncodedString(DFA53_specialS);
    static final short[][] DFA53_transition;

    static {
        int numStates = DFA53_transitionS.length;
        DFA53_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA53_transition[i] = DFA.unpackEncodedString(DFA53_transitionS[i]);
        }
    }

    class DFA53 extends DFA {

        public DFA53(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 53;
            this.eot = DFA53_eot;
            this.eof = DFA53_eof;
            this.min = DFA53_min;
            this.max = DFA53_max;
            this.accept = DFA53_accept;
            this.special = DFA53_special;
            this.transition = DFA53_transition;
        }
        public String getDescription() {
            return "562:1: interfaceBodyDeclaration : ( interfaceFieldDeclaration | interfaceMethodDeclaration | interfaceDeclaration | classDeclaration | ';' );";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA53_1 = input.LA(1);


                        int index53_1 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred68_Java()) ) {s = 21;}

                        else if ( (synpred69_Java()) ) {s = 15;}

                        else if ( (synpred70_Java()) ) {s = 17;}

                        else if ( (synpred71_Java()) ) {s = 18;}


                        input.seek(index53_1);
                        if ( s>=0 ) return s;
                        break;
                    case 1 :
                        int LA53_2 = input.LA(1);


                        int index53_2 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred68_Java()) ) {s = 21;}

                        else if ( (synpred69_Java()) ) {s = 15;}

                        else if ( (synpred70_Java()) ) {s = 17;}

                        else if ( (synpred71_Java()) ) {s = 18;}


                        input.seek(index53_2);
                        if ( s>=0 ) return s;
                        break;
                    case 2 :
                        int LA53_3 = input.LA(1);


                        int index53_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred68_Java()) ) {s = 21;}

                        else if ( (synpred69_Java()) ) {s = 15;}

                        else if ( (synpred70_Java()) ) {s = 17;}

                        else if ( (synpred71_Java()) ) {s = 18;}


                        input.seek(index53_3);
                        if ( s>=0 ) return s;
                        break;
                    case 3 :
                        int LA53_4 = input.LA(1);


                        int index53_4 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred68_Java()) ) {s = 21;}

                        else if ( (synpred69_Java()) ) {s = 15;}

                        else if ( (synpred70_Java()) ) {s = 17;}

                        else if ( (synpred71_Java()) ) {s = 18;}


                        input.seek(index53_4);
                        if ( s>=0 ) return s;
                        break;
                    case 4 :
                        int LA53_5 = input.LA(1);


                        int index53_5 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred68_Java()) ) {s = 21;}

                        else if ( (synpred69_Java()) ) {s = 15;}

                        else if ( (synpred70_Java()) ) {s = 17;}

                        else if ( (synpred71_Java()) ) {s = 18;}


                        input.seek(index53_5);
                        if ( s>=0 ) return s;
                        break;
                    case 5 :
                        int LA53_6 = input.LA(1);


                        int index53_6 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred68_Java()) ) {s = 21;}

                        else if ( (synpred69_Java()) ) {s = 15;}

                        else if ( (synpred70_Java()) ) {s = 17;}

                        else if ( (synpred71_Java()) ) {s = 18;}


                        input.seek(index53_6);
                        if ( s>=0 ) return s;
                        break;
                    case 6 :
                        int LA53_7 = input.LA(1);


                        int index53_7 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred68_Java()) ) {s = 21;}

                        else if ( (synpred69_Java()) ) {s = 15;}

                        else if ( (synpred70_Java()) ) {s = 17;}

                        else if ( (synpred71_Java()) ) {s = 18;}


                        input.seek(index53_7);
                        if ( s>=0 ) return s;
                        break;
                    case 7 :
                        int LA53_8 = input.LA(1);


                        int index53_8 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred68_Java()) ) {s = 21;}

                        else if ( (synpred69_Java()) ) {s = 15;}

                        else if ( (synpred70_Java()) ) {s = 17;}

                        else if ( (synpred71_Java()) ) {s = 18;}


                        input.seek(index53_8);
                        if ( s>=0 ) return s;
                        break;
                    case 8 :
                        int LA53_9 = input.LA(1);


                        int index53_9 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred68_Java()) ) {s = 21;}

                        else if ( (synpred69_Java()) ) {s = 15;}

                        else if ( (synpred70_Java()) ) {s = 17;}

                        else if ( (synpred71_Java()) ) {s = 18;}


                        input.seek(index53_9);
                        if ( s>=0 ) return s;
                        break;
                    case 9 :
                        int LA53_10 = input.LA(1);


                        int index53_10 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred68_Java()) ) {s = 21;}

                        else if ( (synpred69_Java()) ) {s = 15;}

                        else if ( (synpred70_Java()) ) {s = 17;}

                        else if ( (synpred71_Java()) ) {s = 18;}


                        input.seek(index53_10);
                        if ( s>=0 ) return s;
                        break;
                    case 10 :
                        int LA53_11 = input.LA(1);


                        int index53_11 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred68_Java()) ) {s = 21;}

                        else if ( (synpred69_Java()) ) {s = 15;}

                        else if ( (synpred70_Java()) ) {s = 17;}

                        else if ( (synpred71_Java()) ) {s = 18;}


                        input.seek(index53_11);
                        if ( s>=0 ) return s;
                        break;
                    case 11 :
                        int LA53_12 = input.LA(1);


                        int index53_12 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred68_Java()) ) {s = 21;}

                        else if ( (synpred69_Java()) ) {s = 15;}

                        else if ( (synpred70_Java()) ) {s = 17;}

                        else if ( (synpred71_Java()) ) {s = 18;}


                        input.seek(index53_12);
                        if ( s>=0 ) return s;
                        break;
                    case 12 :
                        int LA53_13 = input.LA(1);


                        int index53_13 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred68_Java()) ) {s = 21;}

                        else if ( (synpred69_Java()) ) {s = 15;}


                        input.seek(index53_13);
                        if ( s>=0 ) return s;
                        break;
                    case 13 :
                        int LA53_14 = input.LA(1);


                        int index53_14 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred68_Java()) ) {s = 21;}

                        else if ( (synpred69_Java()) ) {s = 15;}


                        input.seek(index53_14);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 53, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA76_eotS =
        "\12\uffff";
    static final String DFA76_eofS =
        "\12\uffff";
    static final String DFA76_minS =
        "\1\4\1\uffff\1\0\1\uffff\1\0\5\uffff";
    static final String DFA76_maxS =
        "\1\165\1\uffff\1\0\1\uffff\1\0\5\uffff";
    static final String DFA76_acceptS =
        "\1\uffff\1\1\1\uffff\1\2\6\uffff";
    static final String DFA76_specialS =
        "\2\uffff\1\0\1\uffff\1\1\5\uffff}>";
    static final String[] DFA76_transitionS = {
            "\12\3\20\uffff\1\3\1\uffff\1\3\2\uffff\1\3\5\uffff\1\3\5\uffff"+
            "\1\3\6\uffff\1\3\1\uffff\1\3\1\uffff\1\3\5\uffff\1\3\2\uffff"+
            "\1\4\2\uffff\1\2\4\uffff\1\3\2\uffff\1\3\46\uffff\1\1",
            "",
            "\1\uffff",
            "",
            "\1\uffff",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA76_eot = DFA.unpackEncodedString(DFA76_eotS);
    static final short[] DFA76_eof = DFA.unpackEncodedString(DFA76_eofS);
    static final char[] DFA76_min = DFA.unpackEncodedStringToUnsignedChars(DFA76_minS);
    static final char[] DFA76_max = DFA.unpackEncodedStringToUnsignedChars(DFA76_maxS);
    static final short[] DFA76_accept = DFA.unpackEncodedString(DFA76_acceptS);
    static final short[] DFA76_special = DFA.unpackEncodedString(DFA76_specialS);
    static final short[][] DFA76_transition;

    static {
        int numStates = DFA76_transitionS.length;
        DFA76_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA76_transition[i] = DFA.unpackEncodedString(DFA76_transitionS[i]);
        }
    }

    class DFA76 extends DFA {

        public DFA76(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 76;
            this.eot = DFA76_eot;
            this.eof = DFA76_eof;
            this.min = DFA76_min;
            this.max = DFA76_max;
            this.accept = DFA76_accept;
            this.special = DFA76_special;
            this.transition = DFA76_transition;
        }
        public String getDescription() {
            return "688:1: explicitConstructorInvocation : ( ( nonWildcardTypeArguments )? ( 'this' | 'super' ) arguments ';' | primary '.' ( nonWildcardTypeArguments )? 'super' arguments ';' );";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA76_2 = input.LA(1);


                        int index76_2 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred103_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 3;}


                        input.seek(index76_2);
                        if ( s>=0 ) return s;
                        break;
                    case 1 :
                        int LA76_4 = input.LA(1);


                        int index76_4 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred103_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 3;}


                        input.seek(index76_4);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 76, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA87_eotS =
        "\26\uffff";
    static final String DFA87_eofS =
        "\26\uffff";
    static final String DFA87_minS =
        "\1\4\16\0\7\uffff";
    static final String DFA87_maxS =
        "\1\162\16\0\7\uffff";
    static final String DFA87_acceptS =
        "\17\uffff\1\3\1\4\1\5\1\7\1\1\1\2\1\6";
    static final String DFA87_specialS =
        "\1\uffff\1\0\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14"+
        "\1\15\7\uffff}>";
    static final String[] DFA87_transitionS = {
            "\1\15\27\uffff\1\6\1\uffff\1\16\1\uffff\1\16\2\uffff\1\16\1"+
            "\17\4\uffff\1\16\1\uffff\1\21\1\uffff\1\7\1\uffff\1\16\6\uffff"+
            "\1\16\1\20\1\16\1\10\2\uffff\1\4\1\3\1\2\1\uffff\1\16\1\5\1"+
            "\14\2\uffff\1\11\3\uffff\1\12\2\uffff\1\13\7\uffff\1\22\35\uffff"+
            "\1\1",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA87_eot = DFA.unpackEncodedString(DFA87_eotS);
    static final short[] DFA87_eof = DFA.unpackEncodedString(DFA87_eofS);
    static final char[] DFA87_min = DFA.unpackEncodedStringToUnsignedChars(DFA87_minS);
    static final char[] DFA87_max = DFA.unpackEncodedStringToUnsignedChars(DFA87_maxS);
    static final short[] DFA87_accept = DFA.unpackEncodedString(DFA87_acceptS);
    static final short[] DFA87_special = DFA.unpackEncodedString(DFA87_specialS);
    static final short[][] DFA87_transition;

    static {
        int numStates = DFA87_transitionS.length;
        DFA87_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA87_transition[i] = DFA.unpackEncodedString(DFA87_transitionS[i]);
        }
    }

    class DFA87 extends DFA {

        public DFA87(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 87;
            this.eot = DFA87_eot;
            this.eof = DFA87_eof;
            this.min = DFA87_min;
            this.max = DFA87_max;
            this.accept = DFA87_accept;
            this.special = DFA87_special;
            this.transition = DFA87_transition;
        }
        public String getDescription() {
            return "772:1: annotationTypeElementDeclaration : ( annotationMethodDeclaration | interfaceFieldDeclaration | normalClassDeclaration | normalInterfaceDeclaration | enumDeclaration | annotationTypeDeclaration | ';' );";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA87_1 = input.LA(1);


                        int index87_1 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred117_Java()) ) {s = 19;}

                        else if ( (synpred118_Java()) ) {s = 20;}

                        else if ( (synpred119_Java()) ) {s = 15;}

                        else if ( (synpred120_Java()) ) {s = 16;}

                        else if ( (synpred121_Java()) ) {s = 17;}

                        else if ( (synpred122_Java()) ) {s = 21;}


                        input.seek(index87_1);
                        if ( s>=0 ) return s;
                        break;
                    case 1 :
                        int LA87_2 = input.LA(1);


                        int index87_2 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred117_Java()) ) {s = 19;}

                        else if ( (synpred118_Java()) ) {s = 20;}

                        else if ( (synpred119_Java()) ) {s = 15;}

                        else if ( (synpred120_Java()) ) {s = 16;}

                        else if ( (synpred121_Java()) ) {s = 17;}

                        else if ( (synpred122_Java()) ) {s = 21;}


                        input.seek(index87_2);
                        if ( s>=0 ) return s;
                        break;
                    case 2 :
                        int LA87_3 = input.LA(1);


                        int index87_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred117_Java()) ) {s = 19;}

                        else if ( (synpred118_Java()) ) {s = 20;}

                        else if ( (synpred119_Java()) ) {s = 15;}

                        else if ( (synpred120_Java()) ) {s = 16;}

                        else if ( (synpred121_Java()) ) {s = 17;}

                        else if ( (synpred122_Java()) ) {s = 21;}


                        input.seek(index87_3);
                        if ( s>=0 ) return s;
                        break;
                    case 3 :
                        int LA87_4 = input.LA(1);


                        int index87_4 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred117_Java()) ) {s = 19;}

                        else if ( (synpred118_Java()) ) {s = 20;}

                        else if ( (synpred119_Java()) ) {s = 15;}

                        else if ( (synpred120_Java()) ) {s = 16;}

                        else if ( (synpred121_Java()) ) {s = 17;}

                        else if ( (synpred122_Java()) ) {s = 21;}


                        input.seek(index87_4);
                        if ( s>=0 ) return s;
                        break;
                    case 4 :
                        int LA87_5 = input.LA(1);


                        int index87_5 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred117_Java()) ) {s = 19;}

                        else if ( (synpred118_Java()) ) {s = 20;}

                        else if ( (synpred119_Java()) ) {s = 15;}

                        else if ( (synpred120_Java()) ) {s = 16;}

                        else if ( (synpred121_Java()) ) {s = 17;}

                        else if ( (synpred122_Java()) ) {s = 21;}


                        input.seek(index87_5);
                        if ( s>=0 ) return s;
                        break;
                    case 5 :
                        int LA87_6 = input.LA(1);


                        int index87_6 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred117_Java()) ) {s = 19;}

                        else if ( (synpred118_Java()) ) {s = 20;}

                        else if ( (synpred119_Java()) ) {s = 15;}

                        else if ( (synpred120_Java()) ) {s = 16;}

                        else if ( (synpred121_Java()) ) {s = 17;}

                        else if ( (synpred122_Java()) ) {s = 21;}


                        input.seek(index87_6);
                        if ( s>=0 ) return s;
                        break;
                    case 6 :
                        int LA87_7 = input.LA(1);


                        int index87_7 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred117_Java()) ) {s = 19;}

                        else if ( (synpred118_Java()) ) {s = 20;}

                        else if ( (synpred119_Java()) ) {s = 15;}

                        else if ( (synpred120_Java()) ) {s = 16;}

                        else if ( (synpred121_Java()) ) {s = 17;}

                        else if ( (synpred122_Java()) ) {s = 21;}


                        input.seek(index87_7);
                        if ( s>=0 ) return s;
                        break;
                    case 7 :
                        int LA87_8 = input.LA(1);


                        int index87_8 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred117_Java()) ) {s = 19;}

                        else if ( (synpred118_Java()) ) {s = 20;}

                        else if ( (synpred119_Java()) ) {s = 15;}

                        else if ( (synpred120_Java()) ) {s = 16;}

                        else if ( (synpred121_Java()) ) {s = 17;}

                        else if ( (synpred122_Java()) ) {s = 21;}


                        input.seek(index87_8);
                        if ( s>=0 ) return s;
                        break;
                    case 8 :
                        int LA87_9 = input.LA(1);


                        int index87_9 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred117_Java()) ) {s = 19;}

                        else if ( (synpred118_Java()) ) {s = 20;}

                        else if ( (synpred119_Java()) ) {s = 15;}

                        else if ( (synpred120_Java()) ) {s = 16;}

                        else if ( (synpred121_Java()) ) {s = 17;}

                        else if ( (synpred122_Java()) ) {s = 21;}


                        input.seek(index87_9);
                        if ( s>=0 ) return s;
                        break;
                    case 9 :
                        int LA87_10 = input.LA(1);


                        int index87_10 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred117_Java()) ) {s = 19;}

                        else if ( (synpred118_Java()) ) {s = 20;}

                        else if ( (synpred119_Java()) ) {s = 15;}

                        else if ( (synpred120_Java()) ) {s = 16;}

                        else if ( (synpred121_Java()) ) {s = 17;}

                        else if ( (synpred122_Java()) ) {s = 21;}


                        input.seek(index87_10);
                        if ( s>=0 ) return s;
                        break;
                    case 10 :
                        int LA87_11 = input.LA(1);


                        int index87_11 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred117_Java()) ) {s = 19;}

                        else if ( (synpred118_Java()) ) {s = 20;}

                        else if ( (synpred119_Java()) ) {s = 15;}

                        else if ( (synpred120_Java()) ) {s = 16;}

                        else if ( (synpred121_Java()) ) {s = 17;}

                        else if ( (synpred122_Java()) ) {s = 21;}


                        input.seek(index87_11);
                        if ( s>=0 ) return s;
                        break;
                    case 11 :
                        int LA87_12 = input.LA(1);


                        int index87_12 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred117_Java()) ) {s = 19;}

                        else if ( (synpred118_Java()) ) {s = 20;}

                        else if ( (synpred119_Java()) ) {s = 15;}

                        else if ( (synpred120_Java()) ) {s = 16;}

                        else if ( (synpred121_Java()) ) {s = 17;}

                        else if ( (synpred122_Java()) ) {s = 21;}


                        input.seek(index87_12);
                        if ( s>=0 ) return s;
                        break;
                    case 12 :
                        int LA87_13 = input.LA(1);


                        int index87_13 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred117_Java()) ) {s = 19;}

                        else if ( (synpred118_Java()) ) {s = 20;}


                        input.seek(index87_13);
                        if ( s>=0 ) return s;
                        break;
                    case 13 :
                        int LA87_14 = input.LA(1);


                        int index87_14 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred117_Java()) ) {s = 19;}

                        else if ( (synpred118_Java()) ) {s = 20;}


                        input.seek(index87_14);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 87, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA90_eotS =
        "\54\uffff";
    static final String DFA90_eofS =
        "\54\uffff";
    static final String DFA90_minS =
        "\1\4\4\0\6\uffff\1\0\40\uffff";
    static final String DFA90_maxS =
        "\1\162\4\0\6\uffff\1\0\40\uffff";
    static final String DFA90_acceptS =
        "\5\uffff\1\2\14\uffff\1\3\30\uffff\1\1";
    static final String DFA90_specialS =
        "\1\uffff\1\0\1\1\1\2\1\3\6\uffff\1\4\40\uffff}>";
    static final String[] DFA90_transitionS = {
            "\1\3\11\22\16\uffff\1\5\1\22\1\4\1\22\1\4\2\uffff\1\4\1\5\1"+
            "\uffff\1\22\1\uffff\1\22\1\4\1\uffff\1\5\1\uffff\1\1\1\uffff"+
            "\1\4\1\22\1\uffff\1\22\3\uffff\1\4\1\5\1\4\1\5\1\22\1\uffff"+
            "\3\5\1\22\1\4\2\5\2\22\1\13\2\22\1\uffff\1\5\2\22\1\5\2\22\1"+
            "\uffff\1\22\3\uffff\1\22\4\uffff\2\22\5\uffff\4\22\16\uffff"+
            "\1\2",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA90_eot = DFA.unpackEncodedString(DFA90_eotS);
    static final short[] DFA90_eof = DFA.unpackEncodedString(DFA90_eofS);
    static final char[] DFA90_min = DFA.unpackEncodedStringToUnsignedChars(DFA90_minS);
    static final char[] DFA90_max = DFA.unpackEncodedStringToUnsignedChars(DFA90_maxS);
    static final short[] DFA90_accept = DFA.unpackEncodedString(DFA90_acceptS);
    static final short[] DFA90_special = DFA.unpackEncodedString(DFA90_specialS);
    static final short[][] DFA90_transition;

    static {
        int numStates = DFA90_transitionS.length;
        DFA90_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA90_transition[i] = DFA.unpackEncodedString(DFA90_transitionS[i]);
        }
    }

    class DFA90 extends DFA {

        public DFA90(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 90;
            this.eot = DFA90_eot;
            this.eof = DFA90_eof;
            this.min = DFA90_min;
            this.max = DFA90_max;
            this.accept = DFA90_accept;
            this.special = DFA90_special;
            this.transition = DFA90_transition;
        }
        public String getDescription() {
            return "823:1: blockStatement : ( localVariableDeclarationStatement | classOrInterfaceDeclaration | statement );";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA90_1 = input.LA(1);


                        int index90_1 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred125_Java()) ) {s = 43;}

                        else if ( (synpred126_Java()) ) {s = 5;}


                        input.seek(index90_1);
                        if ( s>=0 ) return s;
                        break;
                    case 1 :
                        int LA90_2 = input.LA(1);


                        int index90_2 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred125_Java()) ) {s = 43;}

                        else if ( (synpred126_Java()) ) {s = 5;}


                        input.seek(index90_2);
                        if ( s>=0 ) return s;
                        break;
                    case 2 :
                        int LA90_3 = input.LA(1);


                        int index90_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred125_Java()) ) {s = 43;}

                        else if ( (true) ) {s = 18;}


                        input.seek(index90_3);
                        if ( s>=0 ) return s;
                        break;
                    case 3 :
                        int LA90_4 = input.LA(1);


                        int index90_4 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred125_Java()) ) {s = 43;}

                        else if ( (true) ) {s = 18;}


                        input.seek(index90_4);
                        if ( s>=0 ) return s;
                        break;
                    case 4 :
                        int LA90_11 = input.LA(1);


                        int index90_11 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred126_Java()) ) {s = 5;}

                        else if ( (true) ) {s = 18;}


                        input.seek(index90_11);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 90, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA98_eotS =
        "\40\uffff";
    static final String DFA98_eofS =
        "\40\uffff";
    static final String DFA98_minS =
        "\1\4\1\uffff\1\0\23\uffff\1\0\11\uffff";
    static final String DFA98_maxS =
        "\1\143\1\uffff\1\0\23\uffff\1\0\11\uffff";
    static final String DFA98_acceptS =
        "\1\uffff\1\1\1\uffff\1\4\1\5\1\6\1\7\1\10\1\11\1\12\1\13\1\14\1"+
        "\15\1\16\1\17\15\uffff\1\21\1\2\1\3\1\20";
    static final String DFA98_specialS =
        "\2\uffff\1\0\23\uffff\1\1\11\uffff}>";
    static final String[] DFA98_transitionS = {
            "\1\26\11\16\17\uffff\1\2\1\16\1\14\1\16\2\uffff\1\16\2\uffff"+
            "\1\15\1\uffff\1\6\1\16\5\uffff\1\16\1\4\1\uffff\1\3\3\uffff"+
            "\1\16\1\uffff\1\16\1\uffff\1\16\4\uffff\1\12\1\16\2\uffff\1"+
            "\16\1\10\1\11\1\16\1\13\2\uffff\1\7\1\16\1\uffff\1\5\1\16\1"+
            "\uffff\1\1\3\uffff\1\34\4\uffff\2\16\5\uffff\4\16",
            "",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA98_eot = DFA.unpackEncodedString(DFA98_eotS);
    static final short[] DFA98_eof = DFA.unpackEncodedString(DFA98_eofS);
    static final char[] DFA98_min = DFA.unpackEncodedStringToUnsignedChars(DFA98_minS);
    static final char[] DFA98_max = DFA.unpackEncodedStringToUnsignedChars(DFA98_maxS);
    static final short[] DFA98_accept = DFA.unpackEncodedString(DFA98_acceptS);
    static final short[] DFA98_special = DFA.unpackEncodedString(DFA98_specialS);
    static final short[][] DFA98_transition;

    static {
        int numStates = DFA98_transitionS.length;
        DFA98_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA98_transition[i] = DFA.unpackEncodedString(DFA98_transitionS[i]);
        }
    }

    class DFA98 extends DFA {

        public DFA98(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 98;
            this.eot = DFA98_eot;
            this.eof = DFA98_eof;
            this.min = DFA98_min;
            this.max = DFA98_max;
            this.accept = DFA98_accept;
            this.special = DFA98_special;
            this.transition = DFA98_transition;
        }
        public String getDescription() {
            return "842:1: statement : ( block | ( 'assert' ) expression ( ':' expression )? ';' | 'assert' expression ( ':' expression )? ';' | 'if' parExpression statement ( 'else' statement )? | forstatement | 'while' parExpression statement | 'do' statement 'while' parExpression ';' | trystatement | 'switch' parExpression '{' switchBlockStatementGroups '}' | 'synchronized' parExpression block | 'return' ( expression )? ';' | 'throw' expression ';' | 'break' ( IDENTIFIER )? ';' | 'continue' ( IDENTIFIER )? ';' | expression ';' | IDENTIFIER ':' statement | ';' );";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA98_2 = input.LA(1);


                        int index98_2 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred130_Java()) ) {s = 29;}

                        else if ( (synpred132_Java()) ) {s = 30;}


                        input.seek(index98_2);
                        if ( s>=0 ) return s;
                        break;
                    case 1 :
                        int LA98_22 = input.LA(1);


                        int index98_22 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred148_Java()) ) {s = 14;}

                        else if ( (synpred149_Java()) ) {s = 31;}


                        input.seek(index98_22);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 98, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA109_eotS =
        "\21\uffff";
    static final String DFA109_eofS =
        "\21\uffff";
    static final String DFA109_minS =
        "\1\4\2\uffff\2\0\14\uffff";
    static final String DFA109_maxS =
        "\1\162\2\uffff\2\0\14\uffff";
    static final String DFA109_acceptS =
        "\1\uffff\1\1\3\uffff\1\2\13\uffff";
    static final String DFA109_specialS =
        "\3\uffff\1\0\1\1\14\uffff}>";
    static final String[] DFA109_transitionS = {
            "\1\3\11\5\20\uffff\1\4\1\uffff\1\4\2\uffff\1\4\5\uffff\1\4\3"+
            "\uffff\1\1\1\uffff\1\4\6\uffff\1\4\1\uffff\1\4\1\uffff\1\5\5"+
            "\uffff\1\4\2\uffff\1\5\2\uffff\1\5\4\uffff\1\5\2\uffff\1\5\12"+
            "\uffff\2\5\5\uffff\4\5\16\uffff\1\1",
            "",
            "",
            "\1\uffff",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA109_eot = DFA.unpackEncodedString(DFA109_eotS);
    static final short[] DFA109_eof = DFA.unpackEncodedString(DFA109_eofS);
    static final char[] DFA109_min = DFA.unpackEncodedStringToUnsignedChars(DFA109_minS);
    static final char[] DFA109_max = DFA.unpackEncodedStringToUnsignedChars(DFA109_maxS);
    static final short[] DFA109_accept = DFA.unpackEncodedString(DFA109_acceptS);
    static final short[] DFA109_special = DFA.unpackEncodedString(DFA109_specialS);
    static final short[][] DFA109_transition;

    static {
        int numStates = DFA109_transitionS.length;
        DFA109_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA109_transition[i] = DFA.unpackEncodedString(DFA109_transitionS[i]);
        }
    }

    class DFA109 extends DFA {

        public DFA109(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 109;
            this.eot = DFA109_eot;
            this.eof = DFA109_eof;
            this.min = DFA109_min;
            this.max = DFA109_max;
            this.accept = DFA109_accept;
            this.special = DFA109_special;
            this.transition = DFA109_transition;
        }
        public String getDescription() {
            return "928:1: forInit : ( localVariableDeclaration | expressionList );";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA109_3 = input.LA(1);


                        int index109_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred161_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 5;}


                        input.seek(index109_3);
                        if ( s>=0 ) return s;
                        break;
                    case 1 :
                        int LA109_4 = input.LA(1);


                        int index109_4 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred161_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 5;}


                        input.seek(index109_4);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 109, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA112_eotS =
        "\17\uffff";
    static final String DFA112_eofS =
        "\17\uffff";
    static final String DFA112_minS =
        "\1\130\12\uffff\1\164\1\130\2\uffff";
    static final String DFA112_maxS =
        "\1\165\12\uffff\2\164\2\uffff";
    static final String DFA112_acceptS =
        "\1\uffff\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\12\2\uffff\1\13"+
        "\1\14";
    static final String DFA112_specialS =
        "\17\uffff}>";
    static final String[] DFA112_transitionS = {
            "\1\1\21\uffff\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\2\uffff\1\13"+
            "\1\12",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\14",
            "\1\16\33\uffff\1\15",
            "",
            ""
    };

    static final short[] DFA112_eot = DFA.unpackEncodedString(DFA112_eotS);
    static final short[] DFA112_eof = DFA.unpackEncodedString(DFA112_eofS);
    static final char[] DFA112_min = DFA.unpackEncodedStringToUnsignedChars(DFA112_minS);
    static final char[] DFA112_max = DFA.unpackEncodedStringToUnsignedChars(DFA112_maxS);
    static final short[] DFA112_accept = DFA.unpackEncodedString(DFA112_acceptS);
    static final short[] DFA112_special = DFA.unpackEncodedString(DFA112_specialS);
    static final short[][] DFA112_transition;

    static {
        int numStates = DFA112_transitionS.length;
        DFA112_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA112_transition[i] = DFA.unpackEncodedString(DFA112_transitionS[i]);
        }
    }

    class DFA112 extends DFA {

        public DFA112(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 112;
            this.eot = DFA112_eot;
            this.eof = DFA112_eof;
            this.min = DFA112_min;
            this.max = DFA112_max;
            this.accept = DFA112_accept;
            this.special = DFA112_special;
            this.transition = DFA112_transition;
        }
        public String getDescription() {
            return "951:1: assignmentOperator : ( '=' | '+=' | '-=' | '*=' | '/=' | '&=' | '|=' | '^=' | '%=' | '<' '<' '=' | '>' '>' '>' '=' | '>' '>' '=' );";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
    }
    static final String DFA130_eotS =
        "\14\uffff";
    static final String DFA130_eofS =
        "\14\uffff";
    static final String DFA130_minS =
        "\1\4\2\uffff\1\0\10\uffff";
    static final String DFA130_maxS =
        "\1\132\2\uffff\1\0\10\uffff";
    static final String DFA130_acceptS =
        "\1\uffff\1\1\1\2\1\uffff\1\4\6\uffff\1\3";
    static final String DFA130_specialS =
        "\3\uffff\1\0\10\uffff}>";
    static final String[] DFA130_transitionS = {
            "\12\4\20\uffff\1\4\1\uffff\1\4\2\uffff\1\4\5\uffff\1\4\5\uffff"+
            "\1\4\6\uffff\1\4\1\uffff\1\4\1\uffff\1\4\5\uffff\1\4\2\uffff"+
            "\1\4\2\uffff\1\4\4\uffff\1\4\2\uffff\1\3\12\uffff\1\2\1\1",
            "",
            "",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA130_eot = DFA.unpackEncodedString(DFA130_eotS);
    static final short[] DFA130_eof = DFA.unpackEncodedString(DFA130_eofS);
    static final char[] DFA130_min = DFA.unpackEncodedStringToUnsignedChars(DFA130_minS);
    static final char[] DFA130_max = DFA.unpackEncodedStringToUnsignedChars(DFA130_maxS);
    static final short[] DFA130_accept = DFA.unpackEncodedString(DFA130_acceptS);
    static final short[] DFA130_special = DFA.unpackEncodedString(DFA130_specialS);
    static final short[][] DFA130_transition;

    static {
        int numStates = DFA130_transitionS.length;
        DFA130_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA130_transition[i] = DFA.unpackEncodedString(DFA130_transitionS[i]);
        }
    }

    class DFA130 extends DFA {

        public DFA130(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 130;
            this.eot = DFA130_eot;
            this.eof = DFA130_eof;
            this.min = DFA130_min;
            this.max = DFA130_max;
            this.accept = DFA130_accept;
            this.special = DFA130_special;
            this.transition = DFA130_transition;
        }
        public String getDescription() {
            return "1080:1: unaryExpressionNotPlusMinus : ( '~' unaryExpression | '!' unaryExpression | castExpression | primary ( selector )* ( '++' | '--' )? );";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA130_3 = input.LA(1);


                        int index130_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred202_Java()) ) {s = 11;}

                        else if ( (true) ) {s = 4;}


                        input.seek(index130_3);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 130, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA133_eotS =
        "\41\uffff";
    static final String DFA133_eofS =
        "\1\4\40\uffff";
    static final String DFA133_minS =
        "\1\65\1\0\1\uffff\1\0\35\uffff";
    static final String DFA133_maxS =
        "\1\165\1\0\1\uffff\1\0\35\uffff";
    static final String DFA133_acceptS =
        "\2\uffff\1\1\1\uffff\1\2\34\uffff";
    static final String DFA133_specialS =
        "\1\uffff\1\0\1\uffff\1\1\35\uffff}>";
    static final String[] DFA133_transitionS = {
            "\1\4\30\uffff\1\2\1\4\1\uffff\1\4\1\1\3\4\1\3\1\uffff\1\4\2"+
            "\uffff\27\4\1\uffff\3\4",
            "\1\uffff",
            "",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA133_eot = DFA.unpackEncodedString(DFA133_eotS);
    static final short[] DFA133_eof = DFA.unpackEncodedString(DFA133_eofS);
    static final char[] DFA133_min = DFA.unpackEncodedStringToUnsignedChars(DFA133_minS);
    static final char[] DFA133_max = DFA.unpackEncodedStringToUnsignedChars(DFA133_maxS);
    static final short[] DFA133_accept = DFA.unpackEncodedString(DFA133_acceptS);
    static final short[] DFA133_special = DFA.unpackEncodedString(DFA133_specialS);
    static final short[][] DFA133_transition;

    static {
        int numStates = DFA133_transitionS.length;
        DFA133_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA133_transition[i] = DFA.unpackEncodedString(DFA133_transitionS[i]);
        }
    }

    class DFA133 extends DFA {

        public DFA133(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 133;
            this.eot = DFA133_eot;
            this.eof = DFA133_eof;
            this.min = DFA133_min;
            this.max = DFA133_max;
            this.accept = DFA133_accept;
            this.special = DFA133_special;
            this.transition = DFA133_transition;
        }
        public String getDescription() {
            return "1105:9: ( identifierSuffix )?";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA133_1 = input.LA(1);


                        int index133_1 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred209_Java()) ) {s = 2;}

                        else if ( (true) ) {s = 4;}


                        input.seek(index133_1);
                        if ( s>=0 ) return s;
                        break;
                    case 1 :
                        int LA133_3 = input.LA(1);


                        int index133_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred209_Java()) ) {s = 2;}

                        else if ( (true) ) {s = 4;}


                        input.seek(index133_3);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 133, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA135_eotS =
        "\41\uffff";
    static final String DFA135_eofS =
        "\1\4\40\uffff";
    static final String DFA135_minS =
        "\1\65\1\0\1\uffff\1\0\35\uffff";
    static final String DFA135_maxS =
        "\1\165\1\0\1\uffff\1\0\35\uffff";
    static final String DFA135_acceptS =
        "\2\uffff\1\1\1\uffff\1\2\34\uffff";
    static final String DFA135_specialS =
        "\1\uffff\1\0\1\uffff\1\1\35\uffff}>";
    static final String[] DFA135_transitionS = {
            "\1\4\30\uffff\1\2\1\4\1\uffff\1\4\1\1\3\4\1\3\1\uffff\1\4\2"+
            "\uffff\27\4\1\uffff\3\4",
            "\1\uffff",
            "",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA135_eot = DFA.unpackEncodedString(DFA135_eotS);
    static final short[] DFA135_eof = DFA.unpackEncodedString(DFA135_eofS);
    static final char[] DFA135_min = DFA.unpackEncodedStringToUnsignedChars(DFA135_minS);
    static final char[] DFA135_max = DFA.unpackEncodedStringToUnsignedChars(DFA135_maxS);
    static final short[] DFA135_accept = DFA.unpackEncodedString(DFA135_acceptS);
    static final short[] DFA135_special = DFA.unpackEncodedString(DFA135_specialS);
    static final short[][] DFA135_transition;

    static {
        int numStates = DFA135_transitionS.length;
        DFA135_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA135_transition[i] = DFA.unpackEncodedString(DFA135_transitionS[i]);
        }
    }

    class DFA135 extends DFA {

        public DFA135(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 135;
            this.eot = DFA135_eot;
            this.eof = DFA135_eof;
            this.min = DFA135_min;
            this.max = DFA135_max;
            this.accept = DFA135_accept;
            this.special = DFA135_special;
            this.transition = DFA135_transition;
        }
        public String getDescription() {
            return "1110:9: ( identifierSuffix )?";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA135_1 = input.LA(1);


                        int index135_1 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred212_Java()) ) {s = 2;}

                        else if ( (true) ) {s = 4;}


                        input.seek(index135_1);
                        if ( s>=0 ) return s;
                        break;
                    case 1 :
                        int LA135_3 = input.LA(1);


                        int index135_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred212_Java()) ) {s = 2;}

                        else if ( (true) ) {s = 4;}


                        input.seek(index135_3);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 135, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA143_eotS =
        "\13\uffff";
    static final String DFA143_eofS =
        "\13\uffff";
    static final String DFA143_minS =
        "\1\116\1\4\1\uffff\1\44\7\uffff";
    static final String DFA143_maxS =
        "\1\126\1\143\1\uffff\1\165\7\uffff";
    static final String DFA143_acceptS =
        "\2\uffff\1\3\1\uffff\1\1\1\2\1\4\1\6\1\7\1\10\1\5";
    static final String DFA143_specialS =
        "\13\uffff}>";
    static final String[] DFA143_transitionS = {
            "\1\2\3\uffff\1\1\3\uffff\1\3",
            "\12\5\20\uffff\1\5\1\uffff\1\5\2\uffff\1\5\5\uffff\1\5\5\uffff"+
            "\1\5\6\uffff\1\5\1\uffff\1\5\1\uffff\1\5\5\uffff\1\5\2\uffff"+
            "\1\5\2\uffff\1\5\4\uffff\1\5\2\uffff\1\5\4\uffff\1\4\5\uffff"+
            "\2\5\5\uffff\4\5",
            "",
            "\1\6\25\uffff\1\11\10\uffff\1\10\2\uffff\1\7\56\uffff\1\12",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA143_eot = DFA.unpackEncodedString(DFA143_eotS);
    static final short[] DFA143_eof = DFA.unpackEncodedString(DFA143_eofS);
    static final char[] DFA143_min = DFA.unpackEncodedStringToUnsignedChars(DFA143_minS);
    static final char[] DFA143_max = DFA.unpackEncodedStringToUnsignedChars(DFA143_maxS);
    static final short[] DFA143_accept = DFA.unpackEncodedString(DFA143_acceptS);
    static final short[] DFA143_special = DFA.unpackEncodedString(DFA143_specialS);
    static final short[][] DFA143_transition;

    static {
        int numStates = DFA143_transitionS.length;
        DFA143_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA143_transition[i] = DFA.unpackEncodedString(DFA143_transitionS[i]);
        }
    }

    class DFA143 extends DFA {

        public DFA143(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 143;
            this.eot = DFA143_eot;
            this.eof = DFA143_eof;
            this.min = DFA143_min;
            this.max = DFA143_max;
            this.accept = DFA143_accept;
            this.special = DFA143_special;
            this.transition = DFA143_transition;
        }
        public String getDescription() {
            return "1134:1: identifierSuffix : ( ( '[' ']' )+ '.' 'class' | ( '[' expression ']' )+ | arguments | '.' 'class' | '.' nonWildcardTypeArguments IDENTIFIER arguments | '.' 'this' | '.' 'super' arguments | innerCreator );";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
    }
    static final String DFA142_eotS =
        "\41\uffff";
    static final String DFA142_eofS =
        "\1\1\40\uffff";
    static final String DFA142_minS =
        "\1\65\1\uffff\1\0\36\uffff";
    static final String DFA142_maxS =
        "\1\165\1\uffff\1\0\36\uffff";
    static final String DFA142_acceptS =
        "\1\uffff\1\2\36\uffff\1\1";
    static final String DFA142_specialS =
        "\2\uffff\1\0\36\uffff}>";
    static final String[] DFA142_transitionS = {
            "\1\1\31\uffff\1\1\1\uffff\1\1\1\2\4\1\1\uffff\1\1\2\uffff\27"+
            "\1\1\uffff\3\1",
            "",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA142_eot = DFA.unpackEncodedString(DFA142_eotS);
    static final short[] DFA142_eof = DFA.unpackEncodedString(DFA142_eofS);
    static final char[] DFA142_min = DFA.unpackEncodedStringToUnsignedChars(DFA142_minS);
    static final char[] DFA142_max = DFA.unpackEncodedStringToUnsignedChars(DFA142_maxS);
    static final short[] DFA142_accept = DFA.unpackEncodedString(DFA142_acceptS);
    static final short[] DFA142_special = DFA.unpackEncodedString(DFA142_specialS);
    static final short[][] DFA142_transition;

    static {
        int numStates = DFA142_transitionS.length;
        DFA142_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA142_transition[i] = DFA.unpackEncodedString(DFA142_transitionS[i]);
        }
    }

    class DFA142 extends DFA {

        public DFA142(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 142;
            this.eot = DFA142_eot;
            this.eof = DFA142_eof;
            this.min = DFA142_min;
            this.max = DFA142_max;
            this.accept = DFA142_accept;
            this.special = DFA142_special;
            this.transition = DFA142_transition;
        }
        public String getDescription() {
            return "()+ loopback of 1138:9: ( '[' expression ']' )+";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA142_2 = input.LA(1);


                        int index142_2 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred224_Java()) ) {s = 32;}

                        else if ( (true) ) {s = 1;}


                        input.seek(index142_2);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 142, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA148_eotS =
        "\41\uffff";
    static final String DFA148_eofS =
        "\1\2\40\uffff";
    static final String DFA148_minS =
        "\1\65\1\0\37\uffff";
    static final String DFA148_maxS =
        "\1\165\1\0\37\uffff";
    static final String DFA148_acceptS =
        "\2\uffff\1\2\35\uffff\1\1";
    static final String DFA148_specialS =
        "\1\uffff\1\0\37\uffff}>";
    static final String[] DFA148_transitionS = {
            "\1\2\31\uffff\1\2\1\uffff\1\2\1\1\4\2\1\uffff\1\2\2\uffff\27"+
            "\2\1\uffff\3\2",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA148_eot = DFA.unpackEncodedString(DFA148_eotS);
    static final short[] DFA148_eof = DFA.unpackEncodedString(DFA148_eofS);
    static final char[] DFA148_min = DFA.unpackEncodedStringToUnsignedChars(DFA148_minS);
    static final char[] DFA148_max = DFA.unpackEncodedStringToUnsignedChars(DFA148_maxS);
    static final short[] DFA148_accept = DFA.unpackEncodedString(DFA148_acceptS);
    static final short[] DFA148_special = DFA.unpackEncodedString(DFA148_specialS);
    static final short[][] DFA148_transition;

    static {
        int numStates = DFA148_transitionS.length;
        DFA148_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA148_transition[i] = DFA.unpackEncodedString(DFA148_transitionS[i]);
        }
    }

    class DFA148 extends DFA {

        public DFA148(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 148;
            this.eot = DFA148_eot;
            this.eof = DFA148_eof;
            this.min = DFA148_min;
            this.max = DFA148_max;
            this.accept = DFA148_accept;
            this.special = DFA148_special;
            this.transition = DFA148_transition;
        }
        public String getDescription() {
            return "()* loopback of 1176:9: ( '[' expression ']' )*";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA148_1 = input.LA(1);


                        int index148_1 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred240_Java()) ) {s = 32;}

                        else if ( (true) ) {s = 2;}


                        input.seek(index148_1);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 148, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA171_eotS =
        "\55\uffff";
    static final String DFA171_eofS =
        "\55\uffff";
    static final String DFA171_minS =
        "\1\4\1\uffff\10\0\43\uffff";
    static final String DFA171_maxS =
        "\1\165\1\uffff\10\0\43\uffff";
    static final String DFA171_acceptS =
        "\1\uffff\1\1\10\uffff\1\2\42\uffff";
    static final String DFA171_specialS =
        "\2\uffff\1\0\1\1\1\2\1\3\1\4\1\5\1\6\1\7\43\uffff}>";
    static final String[] DFA171_transitionS = {
            "\1\5\11\6\16\uffff\2\12\1\10\1\12\1\10\2\uffff\1\10\1\12\1\uffff"+
            "\1\12\1\uffff\1\12\1\10\1\uffff\1\12\1\uffff\1\12\1\uffff\1"+
            "\10\1\12\1\uffff\1\12\3\uffff\1\10\1\12\1\10\1\12\1\7\1\uffff"+
            "\4\12\1\10\2\12\1\4\2\12\1\2\1\12\1\uffff\2\12\1\11\2\12\1\3"+
            "\1\uffff\2\12\2\uffff\1\12\4\uffff\2\12\5\uffff\4\12\16\uffff"+
            "\1\12\2\uffff\1\1",
            "",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA171_eot = DFA.unpackEncodedString(DFA171_eotS);
    static final short[] DFA171_eof = DFA.unpackEncodedString(DFA171_eofS);
    static final char[] DFA171_min = DFA.unpackEncodedStringToUnsignedChars(DFA171_minS);
    static final char[] DFA171_max = DFA.unpackEncodedStringToUnsignedChars(DFA171_maxS);
    static final short[] DFA171_accept = DFA.unpackEncodedString(DFA171_acceptS);
    static final short[] DFA171_special = DFA.unpackEncodedString(DFA171_specialS);
    static final short[][] DFA171_transition;

    static {
        int numStates = DFA171_transitionS.length;
        DFA171_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA171_transition[i] = DFA.unpackEncodedString(DFA171_transitionS[i]);
        }
    }

    class DFA171 extends DFA {

        public DFA171(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 171;
            this.eot = DFA171_eot;
            this.eof = DFA171_eof;
            this.min = DFA171_min;
            this.max = DFA171_max;
            this.accept = DFA171_accept;
            this.special = DFA171_special;
            this.transition = DFA171_transition;
        }
        public String getDescription() {
            return "521:9: ( explicitConstructorInvocation )?";
        }
        public void error(NoViableAltException nvae) {
            dbg.recognitionException(nvae);
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
		int _s = s;
            switch ( s ) {
                    case 0 :
                        int LA171_2 = input.LA(1);


                        int index171_2 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred57_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 10;}


                        input.seek(index171_2);
                        if ( s>=0 ) return s;
                        break;
                    case 1 :
                        int LA171_3 = input.LA(1);


                        int index171_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred57_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 10;}


                        input.seek(index171_3);
                        if ( s>=0 ) return s;
                        break;
                    case 2 :
                        int LA171_4 = input.LA(1);


                        int index171_4 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred57_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 10;}


                        input.seek(index171_4);
                        if ( s>=0 ) return s;
                        break;
                    case 3 :
                        int LA171_5 = input.LA(1);


                        int index171_5 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred57_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 10;}


                        input.seek(index171_5);
                        if ( s>=0 ) return s;
                        break;
                    case 4 :
                        int LA171_6 = input.LA(1);


                        int index171_6 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred57_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 10;}


                        input.seek(index171_6);
                        if ( s>=0 ) return s;
                        break;
                    case 5 :
                        int LA171_7 = input.LA(1);


                        int index171_7 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred57_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 10;}


                        input.seek(index171_7);
                        if ( s>=0 ) return s;
                        break;
                    case 6 :
                        int LA171_8 = input.LA(1);


                        int index171_8 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred57_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 10;}


                        input.seek(index171_8);
                        if ( s>=0 ) return s;
                        break;
                    case 7 :
                        int LA171_9 = input.LA(1);


                        int index171_9 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred57_Java()) ) {s = 1;}

                        else if ( (true) ) {s = 10;}


                        input.seek(index171_9);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 171, _s, input);
            error(nvae);
            throw nvae;
        }
    }


    public static final BitSet FOLLOW_annotations_in_compilationUnit64 = new BitSet(new long[]{0x0800000000000000L});
    public static final BitSet FOLLOW_packageDeclaration_in_compilationUnit93 = new BitSet(new long[]{0x7290281010000002L,0x0004000000101226L});
    public static final BitSet FOLLOW_importDeclaration_in_compilationUnit115 = new BitSet(new long[]{0x7290281010000002L,0x0004000000101226L});
    public static final BitSet FOLLOW_typeDeclaration_in_compilationUnit137 = new BitSet(new long[]{0x7280281010000002L,0x0004000000101226L});
    public static final BitSet FOLLOW_PACKAGE_in_packageDeclaration167 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_qualifiedName_in_packageDeclaration169 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_packageDeclaration179 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IMPORT_in_importDeclaration198 = new BitSet(new long[]{0x0000000000000010L,0x0000000000000002L});
    public static final BitSet FOLLOW_STATIC_in_importDeclaration209 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_importDeclaration230 = new BitSet(new long[]{0x0000000000000000L,0x0000000000400000L});
    public static final BitSet FOLLOW_DOT_in_importDeclaration232 = new BitSet(new long[]{0x0000000000000000L,0x0000001000000000L});
    public static final BitSet FOLLOW_STAR_in_importDeclaration234 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_importDeclaration244 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IMPORT_in_importDeclaration254 = new BitSet(new long[]{0x0000000000000010L,0x0000000000000002L});
    public static final BitSet FOLLOW_STATIC_in_importDeclaration265 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_importDeclaration286 = new BitSet(new long[]{0x0000000000000000L,0x0000000000400000L});
    public static final BitSet FOLLOW_DOT_in_importDeclaration297 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_importDeclaration299 = new BitSet(new long[]{0x0000000000000000L,0x0000000000500000L});
    public static final BitSet FOLLOW_DOT_in_importDeclaration321 = new BitSet(new long[]{0x0000000000000000L,0x0000001000000000L});
    public static final BitSet FOLLOW_STAR_in_importDeclaration323 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_importDeclaration344 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IDENTIFIER_in_qualifiedImportName363 = new BitSet(new long[]{0x0000000000000002L,0x0000000000400000L});
    public static final BitSet FOLLOW_DOT_in_qualifiedImportName374 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_qualifiedImportName376 = new BitSet(new long[]{0x0000000000000002L,0x0000000000400000L});
    public static final BitSet FOLLOW_classOrInterfaceDeclaration_in_typeDeclaration406 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SEMI_in_typeDeclaration416 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_classDeclaration_in_classOrInterfaceDeclaration436 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_interfaceDeclaration_in_classOrInterfaceDeclaration446 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_annotation_in_modifiers473 = new BitSet(new long[]{0x7200200010000002L,0x0004000000001226L});
    public static final BitSet FOLLOW_PUBLIC_in_modifiers483 = new BitSet(new long[]{0x7200200010000002L,0x0004000000001226L});
    public static final BitSet FOLLOW_PROTECTED_in_modifiers493 = new BitSet(new long[]{0x7200200010000002L,0x0004000000001226L});
    public static final BitSet FOLLOW_PRIVATE_in_modifiers503 = new BitSet(new long[]{0x7200200010000002L,0x0004000000001226L});
    public static final BitSet FOLLOW_STATIC_in_modifiers513 = new BitSet(new long[]{0x7200200010000002L,0x0004000000001226L});
    public static final BitSet FOLLOW_ABSTRACT_in_modifiers523 = new BitSet(new long[]{0x7200200010000002L,0x0004000000001226L});
    public static final BitSet FOLLOW_FINAL_in_modifiers533 = new BitSet(new long[]{0x7200200010000002L,0x0004000000001226L});
    public static final BitSet FOLLOW_NATIVE_in_modifiers543 = new BitSet(new long[]{0x7200200010000002L,0x0004000000001226L});
    public static final BitSet FOLLOW_SYNCHRONIZED_in_modifiers553 = new BitSet(new long[]{0x7200200010000002L,0x0004000000001226L});
    public static final BitSet FOLLOW_TRANSIENT_in_modifiers563 = new BitSet(new long[]{0x7200200010000002L,0x0004000000001226L});
    public static final BitSet FOLLOW_VOLATILE_in_modifiers573 = new BitSet(new long[]{0x7200200010000002L,0x0004000000001226L});
    public static final BitSet FOLLOW_STRICTFP_in_modifiers583 = new BitSet(new long[]{0x7200200010000002L,0x0004000000001226L});
    public static final BitSet FOLLOW_FINAL_in_variableModifiers614 = new BitSet(new long[]{0x0000200000000002L,0x0004000000000000L});
    public static final BitSet FOLLOW_annotation_in_variableModifiers628 = new BitSet(new long[]{0x0000200000000002L,0x0004000000000000L});
    public static final BitSet FOLLOW_normalClassDeclaration_in_classDeclaration659 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_enumDeclaration_in_classDeclaration669 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_normalClassDeclaration688 = new BitSet(new long[]{0x0000001000000000L});
    public static final BitSet FOLLOW_CLASS_in_normalClassDeclaration691 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_normalClassDeclaration693 = new BitSet(new long[]{0x0008100000000000L,0x0020000000010000L});
    public static final BitSet FOLLOW_typeParameters_in_normalClassDeclaration704 = new BitSet(new long[]{0x0008100000000000L,0x0020000000010000L});
    public static final BitSet FOLLOW_EXTENDS_in_normalClassDeclaration726 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_normalClassDeclaration728 = new BitSet(new long[]{0x0008100000000000L,0x0020000000010000L});
    public static final BitSet FOLLOW_IMPLEMENTS_in_normalClassDeclaration750 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_typeList_in_normalClassDeclaration752 = new BitSet(new long[]{0x0008100000000000L,0x0020000000010000L});
    public static final BitSet FOLLOW_classBody_in_normalClassDeclaration773 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LT_in_typeParameters793 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_typeParameter_in_typeParameters807 = new BitSet(new long[]{0x0000000000000000L,0x0010000000200000L});
    public static final BitSet FOLLOW_COMMA_in_typeParameters822 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_typeParameter_in_typeParameters824 = new BitSet(new long[]{0x0000000000000000L,0x0010000000200000L});
    public static final BitSet FOLLOW_GT_in_typeParameters849 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IDENTIFIER_in_typeParameter868 = new BitSet(new long[]{0x0000100000000002L});
    public static final BitSet FOLLOW_EXTENDS_in_typeParameter879 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_typeBound_in_typeParameter881 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_type_in_typeBound912 = new BitSet(new long[]{0x0000000000000002L,0x0000004000000000L});
    public static final BitSet FOLLOW_AMP_in_typeBound923 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_typeBound925 = new BitSet(new long[]{0x0000000000000002L,0x0000004000000000L});
    public static final BitSet FOLLOW_modifiers_in_enumDeclaration956 = new BitSet(new long[]{0x0000080000000000L});
    public static final BitSet FOLLOW_ENUM_in_enumDeclaration967 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_enumDeclaration987 = new BitSet(new long[]{0x0008000000000000L,0x0000000000010000L});
    public static final BitSet FOLLOW_IMPLEMENTS_in_enumDeclaration998 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_typeList_in_enumDeclaration1000 = new BitSet(new long[]{0x0008000000000000L,0x0000000000010000L});
    public static final BitSet FOLLOW_enumBody_in_enumDeclaration1021 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACE_in_enumBody1041 = new BitSet(new long[]{0x0000000000000010L,0x0004000000320000L});
    public static final BitSet FOLLOW_enumConstants_in_enumBody1052 = new BitSet(new long[]{0x0000000000000000L,0x0000000000320000L});
    public static final BitSet FOLLOW_COMMA_in_enumBody1073 = new BitSet(new long[]{0x0000000000000000L,0x0000000000120000L});
    public static final BitSet FOLLOW_enumBodyDeclarations_in_enumBody1085 = new BitSet(new long[]{0x0000000000000000L,0x0000000000020000L});
    public static final BitSet FOLLOW_RBRACE_in_enumBody1106 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_enumConstant_in_enumConstants1125 = new BitSet(new long[]{0x0000000000000002L,0x0000000000200000L});
    public static final BitSet FOLLOW_COMMA_in_enumConstants1136 = new BitSet(new long[]{0x0000000000000010L,0x0004000000000000L});
    public static final BitSet FOLLOW_enumConstant_in_enumConstants1138 = new BitSet(new long[]{0x0000000000000002L,0x0000000000200000L});
    public static final BitSet FOLLOW_annotations_in_enumConstant1171 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_enumConstant1192 = new BitSet(new long[]{0x0008100000000002L,0x0020000000014000L});
    public static final BitSet FOLLOW_arguments_in_enumConstant1203 = new BitSet(new long[]{0x0008100000000002L,0x0020000000010000L});
    public static final BitSet FOLLOW_classBody_in_enumConstant1225 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SEMI_in_enumBodyDeclarations1265 = new BitSet(new long[]{0x73C0AA1950000012L,0x0024000000111A27L});
    public static final BitSet FOLLOW_classBodyDeclaration_in_enumBodyDeclarations1276 = new BitSet(new long[]{0x73C0AA1950000012L,0x0024000000111A27L});
    public static final BitSet FOLLOW_normalInterfaceDeclaration_in_interfaceDeclaration1306 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_annotationTypeDeclaration_in_interfaceDeclaration1316 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_normalInterfaceDeclaration1335 = new BitSet(new long[]{0x0080000000000000L});
    public static final BitSet FOLLOW_INTERFACE_in_normalInterfaceDeclaration1337 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_normalInterfaceDeclaration1339 = new BitSet(new long[]{0x0000100000000000L,0x0020000000010000L});
    public static final BitSet FOLLOW_typeParameters_in_normalInterfaceDeclaration1350 = new BitSet(new long[]{0x0000100000000000L,0x0020000000010000L});
    public static final BitSet FOLLOW_EXTENDS_in_normalInterfaceDeclaration1372 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_typeList_in_normalInterfaceDeclaration1374 = new BitSet(new long[]{0x0000100000000000L,0x0020000000010000L});
    public static final BitSet FOLLOW_interfaceBody_in_normalInterfaceDeclaration1395 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_type_in_typeList1414 = new BitSet(new long[]{0x0000000000000002L,0x0000000000200000L});
    public static final BitSet FOLLOW_COMMA_in_typeList1425 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_typeList1427 = new BitSet(new long[]{0x0000000000000002L,0x0000000000200000L});
    public static final BitSet FOLLOW_LBRACE_in_classBody1457 = new BitSet(new long[]{0x73C0AA1950000010L,0x0024000000131A27L});
    public static final BitSet FOLLOW_classBodyDeclaration_in_classBody1468 = new BitSet(new long[]{0x73C0AA1950000010L,0x0024000000131A27L});
    public static final BitSet FOLLOW_RBRACE_in_classBody1489 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACE_in_interfaceBody1508 = new BitSet(new long[]{0x73C0AA1950000010L,0x0024000000121A27L});
    public static final BitSet FOLLOW_interfaceBodyDeclaration_in_interfaceBody1519 = new BitSet(new long[]{0x73C0AA1950000010L,0x0024000000121A27L});
    public static final BitSet FOLLOW_RBRACE_in_interfaceBody1540 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SEMI_in_classBodyDeclaration1559 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STATIC_in_classBodyDeclaration1570 = new BitSet(new long[]{0x0000000000000000L,0x0000000000010002L});
    public static final BitSet FOLLOW_block_in_classBodyDeclaration1591 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_memberDecl_in_classBodyDeclaration1601 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_fieldDeclaration_in_memberDecl1621 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_methodDeclaration_in_memberDecl1632 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_classDeclaration_in_memberDecl1643 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_interfaceDeclaration_in_memberDecl1654 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_methodDeclaration1691 = new BitSet(new long[]{0x0000000000000010L,0x0020000000000000L});
    public static final BitSet FOLLOW_typeParameters_in_methodDeclaration1702 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_methodDeclaration1723 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_formalParameters_in_methodDeclaration1733 = new BitSet(new long[]{0x0000000000000000L,0x0000000000010100L});
    public static final BitSet FOLLOW_THROWS_in_methodDeclaration1744 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_qualifiedNameList_in_methodDeclaration1746 = new BitSet(new long[]{0x0000000000000000L,0x0000000000010000L});
    public static final BitSet FOLLOW_LBRACE_in_methodDeclaration1767 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06137EFFL});
    public static final BitSet FOLLOW_explicitConstructorInvocation_in_methodDeclaration1778 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06137EFFL});
    public static final BitSet FOLLOW_blockStatement_in_methodDeclaration1800 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06137EFFL});
    public static final BitSet FOLLOW_RBRACE_in_methodDeclaration1821 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_methodDeclaration1831 = new BitSet(new long[]{0x0140820940000010L,0x0020000000000801L});
    public static final BitSet FOLLOW_typeParameters_in_methodDeclaration1842 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000801L});
    public static final BitSet FOLLOW_type_in_methodDeclaration1864 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_VOID_in_methodDeclaration1878 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_methodDeclaration1898 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_formalParameters_in_methodDeclaration1908 = new BitSet(new long[]{0x0000000000000000L,0x0000000000150102L});
    public static final BitSet FOLLOW_LBRACKET_in_methodDeclaration1919 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_methodDeclaration1921 = new BitSet(new long[]{0x0000000000000000L,0x0000000000150102L});
    public static final BitSet FOLLOW_THROWS_in_methodDeclaration1943 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_qualifiedNameList_in_methodDeclaration1945 = new BitSet(new long[]{0x0000000000000000L,0x0000000000110002L});
    public static final BitSet FOLLOW_block_in_methodDeclaration1980 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SEMI_in_methodDeclaration1994 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_fieldDeclaration2024 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_fieldDeclaration2034 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_variableDeclarator_in_fieldDeclaration2044 = new BitSet(new long[]{0x0000000000000000L,0x0000000000300000L});
    public static final BitSet FOLLOW_COMMA_in_fieldDeclaration2055 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_variableDeclarator_in_fieldDeclaration2057 = new BitSet(new long[]{0x0000000000000000L,0x0000000000300000L});
    public static final BitSet FOLLOW_SEMI_in_fieldDeclaration2078 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IDENTIFIER_in_variableDeclarator2097 = new BitSet(new long[]{0x0000000000000002L,0x0000000001040000L});
    public static final BitSet FOLLOW_LBRACKET_in_variableDeclarator2108 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_variableDeclarator2110 = new BitSet(new long[]{0x0000000000000002L,0x0000000001040000L});
    public static final BitSet FOLLOW_EQ_in_variableDeclarator2132 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06014849L});
    public static final BitSet FOLLOW_variableInitializer_in_variableDeclarator2134 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_interfaceFieldDeclaration_in_interfaceBodyDeclaration2172 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_interfaceMethodDeclaration_in_interfaceBodyDeclaration2182 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_interfaceDeclaration_in_interfaceBodyDeclaration2192 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_classDeclaration_in_interfaceBodyDeclaration2202 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SEMI_in_interfaceBodyDeclaration2212 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_interfaceMethodDeclaration2231 = new BitSet(new long[]{0x0140820940000010L,0x0020000000000801L});
    public static final BitSet FOLLOW_typeParameters_in_interfaceMethodDeclaration2242 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000801L});
    public static final BitSet FOLLOW_type_in_interfaceMethodDeclaration2264 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_VOID_in_interfaceMethodDeclaration2275 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_interfaceMethodDeclaration2295 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_formalParameters_in_interfaceMethodDeclaration2305 = new BitSet(new long[]{0x0000000000000000L,0x0000000000140100L});
    public static final BitSet FOLLOW_LBRACKET_in_interfaceMethodDeclaration2316 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_interfaceMethodDeclaration2318 = new BitSet(new long[]{0x0000000000000000L,0x0000000000140100L});
    public static final BitSet FOLLOW_THROWS_in_interfaceMethodDeclaration2340 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_qualifiedNameList_in_interfaceMethodDeclaration2342 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_interfaceMethodDeclaration2355 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_interfaceFieldDeclaration2376 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_interfaceFieldDeclaration2378 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_variableDeclarator_in_interfaceFieldDeclaration2380 = new BitSet(new long[]{0x0000000000000000L,0x0000000000300000L});
    public static final BitSet FOLLOW_COMMA_in_interfaceFieldDeclaration2391 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_variableDeclarator_in_interfaceFieldDeclaration2393 = new BitSet(new long[]{0x0000000000000000L,0x0000000000300000L});
    public static final BitSet FOLLOW_SEMI_in_interfaceFieldDeclaration2414 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_classOrInterfaceType_in_type2434 = new BitSet(new long[]{0x0000000000000002L,0x0000000000040000L});
    public static final BitSet FOLLOW_LBRACKET_in_type2445 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_type2447 = new BitSet(new long[]{0x0000000000000002L,0x0000000000040000L});
    public static final BitSet FOLLOW_primitiveType_in_type2468 = new BitSet(new long[]{0x0000000000000002L,0x0000000000040000L});
    public static final BitSet FOLLOW_LBRACKET_in_type2479 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_type2481 = new BitSet(new long[]{0x0000000000000002L,0x0000000000040000L});
    public static final BitSet FOLLOW_IDENTIFIER_in_classOrInterfaceType2512 = new BitSet(new long[]{0x0000000000000002L,0x0020000000400000L});
    public static final BitSet FOLLOW_typeArguments_in_classOrInterfaceType2523 = new BitSet(new long[]{0x0000000000000002L,0x0000000000400000L});
    public static final BitSet FOLLOW_DOT_in_classOrInterfaceType2545 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_classOrInterfaceType2547 = new BitSet(new long[]{0x0000000000000002L,0x0020000000400000L});
    public static final BitSet FOLLOW_typeArguments_in_classOrInterfaceType2562 = new BitSet(new long[]{0x0000000000000002L,0x0000000000400000L});
    public static final BitSet FOLLOW_set_in_primitiveType0 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LT_in_typeArguments2696 = new BitSet(new long[]{0x0140820940000010L,0x0000000008000001L});
    public static final BitSet FOLLOW_typeArgument_in_typeArguments2698 = new BitSet(new long[]{0x0000000000000000L,0x0010000000200000L});
    public static final BitSet FOLLOW_COMMA_in_typeArguments2709 = new BitSet(new long[]{0x0140820940000010L,0x0000000008000001L});
    public static final BitSet FOLLOW_typeArgument_in_typeArguments2711 = new BitSet(new long[]{0x0000000000000000L,0x0010000000200000L});
    public static final BitSet FOLLOW_GT_in_typeArguments2732 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_type_in_typeArgument2751 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_QUES_in_typeArgument2761 = new BitSet(new long[]{0x0000100000000002L,0x0000000000000008L});
    public static final BitSet FOLLOW_set_in_typeArgument2785 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_typeArgument2829 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_qualifiedName_in_qualifiedNameList2859 = new BitSet(new long[]{0x0000000000000002L,0x0000000000200000L});
    public static final BitSet FOLLOW_COMMA_in_qualifiedNameList2870 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_qualifiedName_in_qualifiedNameList2872 = new BitSet(new long[]{0x0000000000000002L,0x0000000000200000L});
    public static final BitSet FOLLOW_LPAREN_in_formalParameters2902 = new BitSet(new long[]{0x0140A20940000010L,0x0004000000008001L});
    public static final BitSet FOLLOW_formalParameterDecls_in_formalParameters2913 = new BitSet(new long[]{0x0000000000000000L,0x0000000000008000L});
    public static final BitSet FOLLOW_RPAREN_in_formalParameters2934 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ellipsisParameterDecl_in_formalParameterDecls2953 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_normalParameterDecl_in_formalParameterDecls2963 = new BitSet(new long[]{0x0000000000000002L,0x0000000000200000L});
    public static final BitSet FOLLOW_COMMA_in_formalParameterDecls2974 = new BitSet(new long[]{0x0140A20940000010L,0x0004000000000001L});
    public static final BitSet FOLLOW_normalParameterDecl_in_formalParameterDecls2976 = new BitSet(new long[]{0x0000000000000002L,0x0000000000200000L});
    public static final BitSet FOLLOW_normalParameterDecl_in_formalParameterDecls2998 = new BitSet(new long[]{0x0000000000000000L,0x0000000000200000L});
    public static final BitSet FOLLOW_COMMA_in_formalParameterDecls3008 = new BitSet(new long[]{0x0140A20940000010L,0x0004000000000001L});
    public static final BitSet FOLLOW_ellipsisParameterDecl_in_formalParameterDecls3029 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_variableModifiers_in_normalParameterDecl3048 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_normalParameterDecl3050 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_normalParameterDecl3052 = new BitSet(new long[]{0x0000000000000002L,0x0000000000040000L});
    public static final BitSet FOLLOW_LBRACKET_in_normalParameterDecl3063 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_normalParameterDecl3065 = new BitSet(new long[]{0x0000000000000002L,0x0000000000040000L});
    public static final BitSet FOLLOW_variableModifiers_in_ellipsisParameterDecl3095 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_ellipsisParameterDecl3105 = new BitSet(new long[]{0x0000000000000000L,0x0000000000800000L});
    public static final BitSet FOLLOW_ELLIPSIS_in_ellipsisParameterDecl3108 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_ellipsisParameterDecl3118 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_nonWildcardTypeArguments_in_explicitConstructorInvocation3139 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000048L});
    public static final BitSet FOLLOW_set_in_explicitConstructorInvocation3165 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_arguments_in_explicitConstructorInvocation3197 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_explicitConstructorInvocation3199 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_primary_in_explicitConstructorInvocation3210 = new BitSet(new long[]{0x0000000000000000L,0x0000000000400000L});
    public static final BitSet FOLLOW_DOT_in_explicitConstructorInvocation3220 = new BitSet(new long[]{0x0000000000000000L,0x0020000000000008L});
    public static final BitSet FOLLOW_nonWildcardTypeArguments_in_explicitConstructorInvocation3231 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000008L});
    public static final BitSet FOLLOW_SUPER_in_explicitConstructorInvocation3252 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_arguments_in_explicitConstructorInvocation3262 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_explicitConstructorInvocation3264 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IDENTIFIER_in_qualifiedName3283 = new BitSet(new long[]{0x0000000000000002L,0x0000000000400000L});
    public static final BitSet FOLLOW_DOT_in_qualifiedName3294 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_qualifiedName3296 = new BitSet(new long[]{0x0000000000000002L,0x0000000000400000L});
    public static final BitSet FOLLOW_annotation_in_annotations3327 = new BitSet(new long[]{0x0000000000000002L,0x0004000000000000L});
    public static final BitSet FOLLOW_MONKEYS_AT_in_annotation3359 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_qualifiedName_in_annotation3361 = new BitSet(new long[]{0x0000000000000002L,0x0000000000004000L});
    public static final BitSet FOLLOW_LPAREN_in_annotation3375 = new BitSet(new long[]{0x0540820940003FF0L,0x0024000F0601C849L});
    public static final BitSet FOLLOW_elementValuePairs_in_annotation3399 = new BitSet(new long[]{0x0000000000000000L,0x0000000000008000L});
    public static final BitSet FOLLOW_elementValue_in_annotation3423 = new BitSet(new long[]{0x0000000000000000L,0x0000000000008000L});
    public static final BitSet FOLLOW_RPAREN_in_annotation3458 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_elementValuePair_in_elementValuePairs3488 = new BitSet(new long[]{0x0000000000000002L,0x0000000000200000L});
    public static final BitSet FOLLOW_COMMA_in_elementValuePairs3499 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_elementValuePair_in_elementValuePairs3501 = new BitSet(new long[]{0x0000000000000002L,0x0000000000200000L});
    public static final BitSet FOLLOW_IDENTIFIER_in_elementValuePair3531 = new BitSet(new long[]{0x0000000000000000L,0x0000000001000000L});
    public static final BitSet FOLLOW_EQ_in_elementValuePair3533 = new BitSet(new long[]{0x0540820940003FF0L,0x0024000F06014849L});
    public static final BitSet FOLLOW_elementValue_in_elementValuePair3535 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_conditionalExpression_in_elementValue3554 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_annotation_in_elementValue3564 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_elementValueArrayInitializer_in_elementValue3574 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACE_in_elementValueArrayInitializer3593 = new BitSet(new long[]{0x0540820940003FF0L,0x0024000F06234849L});
    public static final BitSet FOLLOW_elementValue_in_elementValueArrayInitializer3604 = new BitSet(new long[]{0x0000000000000000L,0x0000000000220000L});
    public static final BitSet FOLLOW_COMMA_in_elementValueArrayInitializer3619 = new BitSet(new long[]{0x0540820940003FF0L,0x0024000F06014849L});
    public static final BitSet FOLLOW_elementValue_in_elementValueArrayInitializer3621 = new BitSet(new long[]{0x0000000000000000L,0x0000000000220000L});
    public static final BitSet FOLLOW_COMMA_in_elementValueArrayInitializer3650 = new BitSet(new long[]{0x0000000000000000L,0x0000000000020000L});
    public static final BitSet FOLLOW_RBRACE_in_elementValueArrayInitializer3654 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_annotationTypeDeclaration3676 = new BitSet(new long[]{0x0000000000000000L,0x0004000000000000L});
    public static final BitSet FOLLOW_MONKEYS_AT_in_annotationTypeDeclaration3678 = new BitSet(new long[]{0x0080000000000000L});
    public static final BitSet FOLLOW_INTERFACE_in_annotationTypeDeclaration3688 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_annotationTypeDeclaration3698 = new BitSet(new long[]{0x0000000000000000L,0x0000000000010000L});
    public static final BitSet FOLLOW_annotationTypeBody_in_annotationTypeDeclaration3708 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACE_in_annotationTypeBody3728 = new BitSet(new long[]{0x73C0AA1950000010L,0x0004000000121227L});
    public static final BitSet FOLLOW_annotationTypeElementDeclaration_in_annotationTypeBody3739 = new BitSet(new long[]{0x73C0AA1950000010L,0x0004000000121227L});
    public static final BitSet FOLLOW_RBRACE_in_annotationTypeBody3760 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_annotationMethodDeclaration_in_annotationTypeElementDeclaration3781 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_interfaceFieldDeclaration_in_annotationTypeElementDeclaration3791 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_normalClassDeclaration_in_annotationTypeElementDeclaration3801 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_normalInterfaceDeclaration_in_annotationTypeElementDeclaration3811 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_enumDeclaration_in_annotationTypeElementDeclaration3821 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_annotationTypeDeclaration_in_annotationTypeElementDeclaration3831 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SEMI_in_annotationTypeElementDeclaration3841 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_annotationMethodDeclaration3860 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_annotationMethodDeclaration3862 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_annotationMethodDeclaration3864 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_LPAREN_in_annotationMethodDeclaration3874 = new BitSet(new long[]{0x0000000000000000L,0x0000000000008000L});
    public static final BitSet FOLLOW_RPAREN_in_annotationMethodDeclaration3876 = new BitSet(new long[]{0x0000008000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_DEFAULT_in_annotationMethodDeclaration3879 = new BitSet(new long[]{0x0540820940003FF0L,0x0024000F06014849L});
    public static final BitSet FOLLOW_elementValue_in_annotationMethodDeclaration3881 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_annotationMethodDeclaration3910 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACE_in_block3933 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06137EFFL});
    public static final BitSet FOLLOW_blockStatement_in_block3944 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06137EFFL});
    public static final BitSet FOLLOW_RBRACE_in_block3965 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_localVariableDeclarationStatement_in_blockStatement3986 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_classOrInterfaceDeclaration_in_blockStatement3996 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_statement_in_blockStatement4006 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_localVariableDeclaration_in_localVariableDeclarationStatement4026 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_localVariableDeclarationStatement4036 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_variableModifiers_in_localVariableDeclaration4055 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_localVariableDeclaration4057 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_variableDeclarator_in_localVariableDeclaration4067 = new BitSet(new long[]{0x0000000000000002L,0x0000000000200000L});
    public static final BitSet FOLLOW_COMMA_in_localVariableDeclaration4078 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_variableDeclarator_in_localVariableDeclaration4080 = new BitSet(new long[]{0x0000000000000002L,0x0000000000200000L});
    public static final BitSet FOLLOW_block_in_statement4110 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ASSERT_in_statement4122 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_statement4142 = new BitSet(new long[]{0x0000000000000000L,0x0000000010100000L});
    public static final BitSet FOLLOW_COLON_in_statement4145 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_statement4147 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_statement4151 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ASSERT_in_statement4161 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_statement4164 = new BitSet(new long[]{0x0000000000000000L,0x0000000010100000L});
    public static final BitSet FOLLOW_COLON_in_statement4167 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_statement4169 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_statement4173 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IF_in_statement4183 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_parExpression_in_statement4185 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06117EFFL});
    public static final BitSet FOLLOW_statement_in_statement4187 = new BitSet(new long[]{0x0000040000000002L});
    public static final BitSet FOLLOW_ELSE_in_statement4190 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06117EFFL});
    public static final BitSet FOLLOW_statement_in_statement4192 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_forstatement_in_statement4204 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_WHILE_in_statement4214 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_parExpression_in_statement4216 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06117EFFL});
    public static final BitSet FOLLOW_statement_in_statement4218 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DO_in_statement4228 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06117EFFL});
    public static final BitSet FOLLOW_statement_in_statement4230 = new BitSet(new long[]{0x0000000000000000L,0x0000000000002000L});
    public static final BitSet FOLLOW_WHILE_in_statement4232 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_parExpression_in_statement4234 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_statement4236 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_trystatement_in_statement4246 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SWITCH_in_statement4256 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_parExpression_in_statement4258 = new BitSet(new long[]{0x0000000000000000L,0x0000000000010000L});
    public static final BitSet FOLLOW_LBRACE_in_statement4260 = new BitSet(new long[]{0x0000008200000000L,0x0000000000020000L});
    public static final BitSet FOLLOW_switchBlockStatementGroups_in_statement4262 = new BitSet(new long[]{0x0000000000000000L,0x0000000000020000L});
    public static final BitSet FOLLOW_RBRACE_in_statement4264 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SYNCHRONIZED_in_statement4274 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_parExpression_in_statement4276 = new BitSet(new long[]{0x0000000000000000L,0x0000000000010002L});
    public static final BitSet FOLLOW_block_in_statement4278 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RETURN_in_statement4288 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06104849L});
    public static final BitSet FOLLOW_expression_in_statement4291 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_statement4296 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_THROW_in_statement4306 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_statement4308 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_statement4310 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_BREAK_in_statement4320 = new BitSet(new long[]{0x0000000000000010L,0x0000000000100000L});
    public static final BitSet FOLLOW_IDENTIFIER_in_statement4335 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_statement4352 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_CONTINUE_in_statement4362 = new BitSet(new long[]{0x0000000000000010L,0x0000000000100000L});
    public static final BitSet FOLLOW_IDENTIFIER_in_statement4377 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_statement4394 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_expression_in_statement4404 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_statement4407 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IDENTIFIER_in_statement4417 = new BitSet(new long[]{0x0000000000000000L,0x0000000010000000L});
    public static final BitSet FOLLOW_COLON_in_statement4419 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06117EFFL});
    public static final BitSet FOLLOW_statement_in_statement4421 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SEMI_in_statement4431 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_switchBlockStatementGroup_in_switchBlockStatementGroups4452 = new BitSet(new long[]{0x0000008200000002L});
    public static final BitSet FOLLOW_switchLabel_in_switchBlockStatementGroup4480 = new BitSet(new long[]{0xF7C5AB59F0003FF2L,0x0024000F06117EFFL});
    public static final BitSet FOLLOW_blockStatement_in_switchBlockStatementGroup4491 = new BitSet(new long[]{0xF7C5AB59F0003FF2L,0x0024000F06117EFFL});
    public static final BitSet FOLLOW_CASE_in_switchLabel4521 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_switchLabel4523 = new BitSet(new long[]{0x0000000000000000L,0x0000000010000000L});
    public static final BitSet FOLLOW_COLON_in_switchLabel4525 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DEFAULT_in_switchLabel4535 = new BitSet(new long[]{0x0000000000000000L,0x0000000010000000L});
    public static final BitSet FOLLOW_COLON_in_switchLabel4537 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TRY_in_trystatement4557 = new BitSet(new long[]{0x0000000000000000L,0x0000000000010002L});
    public static final BitSet FOLLOW_block_in_trystatement4559 = new BitSet(new long[]{0x0000400400000000L});
    public static final BitSet FOLLOW_catches_in_trystatement4573 = new BitSet(new long[]{0x0000400000000000L});
    public static final BitSet FOLLOW_FINALLY_in_trystatement4575 = new BitSet(new long[]{0x0000000000000000L,0x0000000000010002L});
    public static final BitSet FOLLOW_block_in_trystatement4577 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_catches_in_trystatement4591 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FINALLY_in_trystatement4605 = new BitSet(new long[]{0x0000000000000000L,0x0000000000010002L});
    public static final BitSet FOLLOW_block_in_trystatement4607 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_catchClause_in_catches4637 = new BitSet(new long[]{0x0000000400000002L});
    public static final BitSet FOLLOW_catchClause_in_catches4648 = new BitSet(new long[]{0x0000000400000002L});
    public static final BitSet FOLLOW_CATCH_in_catchClause4678 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_LPAREN_in_catchClause4680 = new BitSet(new long[]{0x0140A20940000010L,0x0004000000000001L});
    public static final BitSet FOLLOW_formalParameter_in_catchClause4682 = new BitSet(new long[]{0x0000000000000000L,0x0000000000008000L});
    public static final BitSet FOLLOW_RPAREN_in_catchClause4692 = new BitSet(new long[]{0x0000000000000000L,0x0000000000010002L});
    public static final BitSet FOLLOW_block_in_catchClause4694 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_variableModifiers_in_formalParameter4713 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_formalParameter4715 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_formalParameter4717 = new BitSet(new long[]{0x0000000000000002L,0x0000000000040000L});
    public static final BitSet FOLLOW_LBRACKET_in_formalParameter4728 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_formalParameter4730 = new BitSet(new long[]{0x0000000000000002L,0x0000000000040000L});
    public static final BitSet FOLLOW_FOR_in_forstatement4775 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_LPAREN_in_forstatement4777 = new BitSet(new long[]{0x0140A20940000010L,0x0004000000000001L});
    public static final BitSet FOLLOW_variableModifiers_in_forstatement4779 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_forstatement4781 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_forstatement4783 = new BitSet(new long[]{0x0000000000000000L,0x0000000010000000L});
    public static final BitSet FOLLOW_COLON_in_forstatement4785 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_forstatement4795 = new BitSet(new long[]{0x0000000000000000L,0x0000000000008000L});
    public static final BitSet FOLLOW_RPAREN_in_forstatement4797 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06117EFFL});
    public static final BitSet FOLLOW_statement_in_forstatement4799 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FOR_in_forstatement4819 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_LPAREN_in_forstatement4821 = new BitSet(new long[]{0x0540A20940003FF0L,0x0024000F06104849L});
    public static final BitSet FOLLOW_forInit_in_forstatement4840 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_forstatement4861 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06104849L});
    public static final BitSet FOLLOW_expression_in_forstatement4880 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_forstatement4901 = new BitSet(new long[]{0x0540A20940003FF0L,0x0024000F0600C849L});
    public static final BitSet FOLLOW_expressionList_in_forstatement4920 = new BitSet(new long[]{0x0000000000000000L,0x0000000000008000L});
    public static final BitSet FOLLOW_RPAREN_in_forstatement4941 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06117EFFL});
    public static final BitSet FOLLOW_statement_in_forstatement4943 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_localVariableDeclaration_in_forInit4962 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_expressionList_in_forInit4972 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_parExpression4991 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_parExpression4993 = new BitSet(new long[]{0x0000000000000000L,0x0000000000008000L});
    public static final BitSet FOLLOW_RPAREN_in_parExpression4995 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_expression_in_expressionList5014 = new BitSet(new long[]{0x0000000000000002L,0x0000000000200000L});
    public static final BitSet FOLLOW_COMMA_in_expressionList5025 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_expressionList5027 = new BitSet(new long[]{0x0000000000000002L,0x0000000000200000L});
    public static final BitSet FOLLOW_conditionalExpression_in_expression5058 = new BitSet(new long[]{0x0000000000000002L,0x0033FC0001000000L});
    public static final BitSet FOLLOW_assignmentOperator_in_expression5069 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_expression5071 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_EQ_in_assignmentOperator5102 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PLUSEQ_in_assignmentOperator5112 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SUBEQ_in_assignmentOperator5122 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STAREQ_in_assignmentOperator5132 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SLASHEQ_in_assignmentOperator5142 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_AMPEQ_in_assignmentOperator5152 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_BAREQ_in_assignmentOperator5162 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_CARETEQ_in_assignmentOperator5172 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PERCENTEQ_in_assignmentOperator5182 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LT_in_assignmentOperator5193 = new BitSet(new long[]{0x0000000000000000L,0x0020000000000000L});
    public static final BitSet FOLLOW_LT_in_assignmentOperator5195 = new BitSet(new long[]{0x0000000000000000L,0x0000000001000000L});
    public static final BitSet FOLLOW_EQ_in_assignmentOperator5197 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_GT_in_assignmentOperator5208 = new BitSet(new long[]{0x0000000000000000L,0x0010000000000000L});
    public static final BitSet FOLLOW_GT_in_assignmentOperator5210 = new BitSet(new long[]{0x0000000000000000L,0x0010000000000000L});
    public static final BitSet FOLLOW_GT_in_assignmentOperator5212 = new BitSet(new long[]{0x0000000000000000L,0x0000000001000000L});
    public static final BitSet FOLLOW_EQ_in_assignmentOperator5214 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_GT_in_assignmentOperator5225 = new BitSet(new long[]{0x0000000000000000L,0x0010000000000000L});
    public static final BitSet FOLLOW_GT_in_assignmentOperator5227 = new BitSet(new long[]{0x0000000000000000L,0x0000000001000000L});
    public static final BitSet FOLLOW_EQ_in_assignmentOperator5229 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_conditionalOrExpression_in_conditionalExpression5249 = new BitSet(new long[]{0x0000000000000002L,0x0000000008000000L});
    public static final BitSet FOLLOW_QUES_in_conditionalExpression5260 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_conditionalExpression5262 = new BitSet(new long[]{0x0000000000000000L,0x0000000010000000L});
    public static final BitSet FOLLOW_COLON_in_conditionalExpression5264 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_conditionalExpression_in_conditionalExpression5266 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_conditionalAndExpression_in_conditionalOrExpression5296 = new BitSet(new long[]{0x0000000000000002L,0x0000000080000000L});
    public static final BitSet FOLLOW_BARBAR_in_conditionalOrExpression5307 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_conditionalAndExpression_in_conditionalOrExpression5309 = new BitSet(new long[]{0x0000000000000002L,0x0000000080000000L});
    public static final BitSet FOLLOW_inclusiveOrExpression_in_conditionalAndExpression5339 = new BitSet(new long[]{0x0000000000000002L,0x0000000040000000L});
    public static final BitSet FOLLOW_AMPAMP_in_conditionalAndExpression5350 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_inclusiveOrExpression_in_conditionalAndExpression5352 = new BitSet(new long[]{0x0000000000000002L,0x0000000040000000L});
    public static final BitSet FOLLOW_exclusiveOrExpression_in_inclusiveOrExpression5382 = new BitSet(new long[]{0x0000000000000002L,0x0000008000000000L});
    public static final BitSet FOLLOW_BAR_in_inclusiveOrExpression5393 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_exclusiveOrExpression_in_inclusiveOrExpression5395 = new BitSet(new long[]{0x0000000000000002L,0x0000008000000000L});
    public static final BitSet FOLLOW_andExpression_in_exclusiveOrExpression5425 = new BitSet(new long[]{0x0000000000000002L,0x0000010000000000L});
    public static final BitSet FOLLOW_CARET_in_exclusiveOrExpression5436 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_andExpression_in_exclusiveOrExpression5438 = new BitSet(new long[]{0x0000000000000002L,0x0000010000000000L});
    public static final BitSet FOLLOW_equalityExpression_in_andExpression5468 = new BitSet(new long[]{0x0000000000000002L,0x0000004000000000L});
    public static final BitSet FOLLOW_AMP_in_andExpression5479 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_equalityExpression_in_andExpression5481 = new BitSet(new long[]{0x0000000000000002L,0x0000004000000000L});
    public static final BitSet FOLLOW_instanceOfExpression_in_equalityExpression5511 = new BitSet(new long[]{0x0000000000000002L,0x0008000020000000L});
    public static final BitSet FOLLOW_set_in_equalityExpression5535 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_instanceOfExpression_in_equalityExpression5585 = new BitSet(new long[]{0x0000000000000002L,0x0008000020000000L});
    public static final BitSet FOLLOW_relationalExpression_in_instanceOfExpression5615 = new BitSet(new long[]{0x0020000000000002L});
    public static final BitSet FOLLOW_INSTANCEOF_in_instanceOfExpression5626 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_instanceOfExpression5628 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_shiftExpression_in_relationalExpression5658 = new BitSet(new long[]{0x0000000000000002L,0x0030000000000000L});
    public static final BitSet FOLLOW_relationalOp_in_relationalExpression5669 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_shiftExpression_in_relationalExpression5671 = new BitSet(new long[]{0x0000000000000002L,0x0030000000000000L});
    public static final BitSet FOLLOW_LT_in_relationalOp5702 = new BitSet(new long[]{0x0000000000000000L,0x0000000001000000L});
    public static final BitSet FOLLOW_EQ_in_relationalOp5704 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_GT_in_relationalOp5715 = new BitSet(new long[]{0x0000000000000000L,0x0000000001000000L});
    public static final BitSet FOLLOW_EQ_in_relationalOp5717 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LT_in_relationalOp5727 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_GT_in_relationalOp5737 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_additiveExpression_in_shiftExpression5756 = new BitSet(new long[]{0x0000000000000002L,0x0030000000000000L});
    public static final BitSet FOLLOW_shiftOp_in_shiftExpression5767 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_additiveExpression_in_shiftExpression5769 = new BitSet(new long[]{0x0000000000000002L,0x0030000000000000L});
    public static final BitSet FOLLOW_LT_in_shiftOp5801 = new BitSet(new long[]{0x0000000000000000L,0x0020000000000000L});
    public static final BitSet FOLLOW_LT_in_shiftOp5803 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_GT_in_shiftOp5814 = new BitSet(new long[]{0x0000000000000000L,0x0010000000000000L});
    public static final BitSet FOLLOW_GT_in_shiftOp5816 = new BitSet(new long[]{0x0000000000000000L,0x0010000000000000L});
    public static final BitSet FOLLOW_GT_in_shiftOp5818 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_GT_in_shiftOp5829 = new BitSet(new long[]{0x0000000000000000L,0x0010000000000000L});
    public static final BitSet FOLLOW_GT_in_shiftOp5831 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_multiplicativeExpression_in_additiveExpression5851 = new BitSet(new long[]{0x0000000000000002L,0x0000000C00000000L});
    public static final BitSet FOLLOW_set_in_additiveExpression5875 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_multiplicativeExpression_in_additiveExpression5925 = new BitSet(new long[]{0x0000000000000002L,0x0000000C00000000L});
    public static final BitSet FOLLOW_unaryExpression_in_multiplicativeExpression5962 = new BitSet(new long[]{0x0000000000000002L,0x0000023000000000L});
    public static final BitSet FOLLOW_set_in_multiplicativeExpression5986 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_unaryExpression_in_multiplicativeExpression6054 = new BitSet(new long[]{0x0000000000000002L,0x0000023000000000L});
    public static final BitSet FOLLOW_PLUS_in_unaryExpression6086 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_unaryExpression_in_unaryExpression6089 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SUB_in_unaryExpression6099 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_unaryExpression_in_unaryExpression6101 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PLUSPLUS_in_unaryExpression6111 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_unaryExpression_in_unaryExpression6113 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SUBSUB_in_unaryExpression6123 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_unaryExpression_in_unaryExpression6125 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_unaryExpressionNotPlusMinus_in_unaryExpression6135 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TILDE_in_unaryExpressionNotPlusMinus6154 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_unaryExpression_in_unaryExpressionNotPlusMinus6156 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_BANG_in_unaryExpressionNotPlusMinus6166 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_unaryExpression_in_unaryExpressionNotPlusMinus6168 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_castExpression_in_unaryExpressionNotPlusMinus6178 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_primary_in_unaryExpressionNotPlusMinus6188 = new BitSet(new long[]{0x0000000000000002L,0x0000000300440000L});
    public static final BitSet FOLLOW_selector_in_unaryExpressionNotPlusMinus6199 = new BitSet(new long[]{0x0000000000000002L,0x0000000300440000L});
    public static final BitSet FOLLOW_set_in_unaryExpressionNotPlusMinus6220 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_castExpression6268 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_primitiveType_in_castExpression6270 = new BitSet(new long[]{0x0000000000000000L,0x0000000000008000L});
    public static final BitSet FOLLOW_RPAREN_in_castExpression6272 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_unaryExpression_in_castExpression6274 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_castExpression6284 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_castExpression6286 = new BitSet(new long[]{0x0000000000000000L,0x0000000000008000L});
    public static final BitSet FOLLOW_RPAREN_in_castExpression6288 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_unaryExpressionNotPlusMinus_in_castExpression6290 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_parExpression_in_primary6311 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_THIS_in_primary6321 = new BitSet(new long[]{0x0000000000000002L,0x0000000000444000L});
    public static final BitSet FOLLOW_DOT_in_primary6332 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_primary6334 = new BitSet(new long[]{0x0000000000000002L,0x0000000000444000L});
    public static final BitSet FOLLOW_identifierSuffix_in_primary6356 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IDENTIFIER_in_primary6377 = new BitSet(new long[]{0x0000000000000002L,0x0000000000444000L});
    public static final BitSet FOLLOW_DOT_in_primary6388 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_primary6390 = new BitSet(new long[]{0x0000000000000002L,0x0000000000444000L});
    public static final BitSet FOLLOW_identifierSuffix_in_primary6412 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SUPER_in_primary6433 = new BitSet(new long[]{0x0000000000000000L,0x0000000000404000L});
    public static final BitSet FOLLOW_superSuffix_in_primary6443 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_literal_in_primary6453 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_creator_in_primary6463 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_primitiveType_in_primary6473 = new BitSet(new long[]{0x0000000000000000L,0x0000000000440000L});
    public static final BitSet FOLLOW_LBRACKET_in_primary6484 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_primary6486 = new BitSet(new long[]{0x0000000000000000L,0x0000000000440000L});
    public static final BitSet FOLLOW_DOT_in_primary6507 = new BitSet(new long[]{0x0000001000000000L});
    public static final BitSet FOLLOW_CLASS_in_primary6509 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_VOID_in_primary6519 = new BitSet(new long[]{0x0000000000000000L,0x0000000000400000L});
    public static final BitSet FOLLOW_DOT_in_primary6521 = new BitSet(new long[]{0x0000001000000000L});
    public static final BitSet FOLLOW_CLASS_in_primary6523 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_arguments_in_superSuffix6543 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOT_in_superSuffix6553 = new BitSet(new long[]{0x0000000000000010L,0x0020000000000000L});
    public static final BitSet FOLLOW_typeArguments_in_superSuffix6556 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_superSuffix6577 = new BitSet(new long[]{0x0000000000000002L,0x0000000000004000L});
    public static final BitSet FOLLOW_arguments_in_superSuffix6588 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACKET_in_identifierSuffix6620 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_identifierSuffix6622 = new BitSet(new long[]{0x0000000000000000L,0x0000000000440000L});
    public static final BitSet FOLLOW_DOT_in_identifierSuffix6643 = new BitSet(new long[]{0x0000001000000000L});
    public static final BitSet FOLLOW_CLASS_in_identifierSuffix6645 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACKET_in_identifierSuffix6656 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_identifierSuffix6658 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_identifierSuffix6660 = new BitSet(new long[]{0x0000000000000002L,0x0000000000040000L});
    public static final BitSet FOLLOW_arguments_in_identifierSuffix6681 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOT_in_identifierSuffix6691 = new BitSet(new long[]{0x0000001000000000L});
    public static final BitSet FOLLOW_CLASS_in_identifierSuffix6693 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOT_in_identifierSuffix6703 = new BitSet(new long[]{0x0000000000000000L,0x0020000000000000L});
    public static final BitSet FOLLOW_nonWildcardTypeArguments_in_identifierSuffix6705 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_identifierSuffix6707 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_arguments_in_identifierSuffix6709 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOT_in_identifierSuffix6719 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000040L});
    public static final BitSet FOLLOW_THIS_in_identifierSuffix6721 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOT_in_identifierSuffix6731 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000008L});
    public static final BitSet FOLLOW_SUPER_in_identifierSuffix6733 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_arguments_in_identifierSuffix6735 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_innerCreator_in_identifierSuffix6745 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOT_in_selector6765 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_selector6767 = new BitSet(new long[]{0x0000000000000002L,0x0000000000004000L});
    public static final BitSet FOLLOW_arguments_in_selector6778 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOT_in_selector6799 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000040L});
    public static final BitSet FOLLOW_THIS_in_selector6801 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOT_in_selector6811 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000008L});
    public static final BitSet FOLLOW_SUPER_in_selector6813 = new BitSet(new long[]{0x0000000000000000L,0x0000000000404000L});
    public static final BitSet FOLLOW_superSuffix_in_selector6823 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_innerCreator_in_selector6833 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACKET_in_selector6843 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_selector6845 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_selector6847 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NEW_in_creator6866 = new BitSet(new long[]{0x0000000000000000L,0x0020000000000000L});
    public static final BitSet FOLLOW_nonWildcardTypeArguments_in_creator6868 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_classOrInterfaceType_in_creator6870 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_classCreatorRest_in_creator6872 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NEW_in_creator6882 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_classOrInterfaceType_in_creator6884 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_classCreatorRest_in_creator6886 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_arrayCreator_in_creator6896 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NEW_in_arrayCreator6915 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_createdName_in_arrayCreator6917 = new BitSet(new long[]{0x0000000000000000L,0x0000000000040000L});
    public static final BitSet FOLLOW_LBRACKET_in_arrayCreator6927 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_arrayCreator6929 = new BitSet(new long[]{0x0000000000000000L,0x0000000000050000L});
    public static final BitSet FOLLOW_LBRACKET_in_arrayCreator6940 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_arrayCreator6942 = new BitSet(new long[]{0x0000000000000000L,0x0000000000050000L});
    public static final BitSet FOLLOW_arrayInitializer_in_arrayCreator6963 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NEW_in_arrayCreator6974 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_createdName_in_arrayCreator6976 = new BitSet(new long[]{0x0000000000000000L,0x0000000000040000L});
    public static final BitSet FOLLOW_LBRACKET_in_arrayCreator6986 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_arrayCreator6988 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_arrayCreator6998 = new BitSet(new long[]{0x0000000000000002L,0x0000000000040000L});
    public static final BitSet FOLLOW_LBRACKET_in_arrayCreator7012 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_arrayCreator7014 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_arrayCreator7028 = new BitSet(new long[]{0x0000000000000002L,0x0000000000040000L});
    public static final BitSet FOLLOW_LBRACKET_in_arrayCreator7050 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_arrayCreator7052 = new BitSet(new long[]{0x0000000000000002L,0x0000000000040000L});
    public static final BitSet FOLLOW_arrayInitializer_in_variableInitializer7082 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_expression_in_variableInitializer7092 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACE_in_arrayInitializer7111 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06234849L});
    public static final BitSet FOLLOW_variableInitializer_in_arrayInitializer7126 = new BitSet(new long[]{0x0000000000000000L,0x0000000000220000L});
    public static final BitSet FOLLOW_COMMA_in_arrayInitializer7145 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06014849L});
    public static final BitSet FOLLOW_variableInitializer_in_arrayInitializer7147 = new BitSet(new long[]{0x0000000000000000L,0x0000000000220000L});
    public static final BitSet FOLLOW_COMMA_in_arrayInitializer7196 = new BitSet(new long[]{0x0000000000000000L,0x0000000000020000L});
    public static final BitSet FOLLOW_RBRACE_in_arrayInitializer7208 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_classOrInterfaceType_in_createdName7241 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_primitiveType_in_createdName7251 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOT_in_innerCreator7270 = new BitSet(new long[]{0x0400000000000000L});
    public static final BitSet FOLLOW_NEW_in_innerCreator7272 = new BitSet(new long[]{0x0000000000000010L,0x0020000000000000L});
    public static final BitSet FOLLOW_nonWildcardTypeArguments_in_innerCreator7283 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_innerCreator7304 = new BitSet(new long[]{0x0000000000000000L,0x0020000000004000L});
    public static final BitSet FOLLOW_typeArguments_in_innerCreator7315 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_classCreatorRest_in_innerCreator7336 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_arguments_in_classCreatorRest7356 = new BitSet(new long[]{0x0008100000000002L,0x0020000000010000L});
    public static final BitSet FOLLOW_classBody_in_classCreatorRest7367 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LT_in_nonWildcardTypeArguments7398 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_typeList_in_nonWildcardTypeArguments7400 = new BitSet(new long[]{0x0000000000000000L,0x0010000000000000L});
    public static final BitSet FOLLOW_GT_in_nonWildcardTypeArguments7410 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_arguments7429 = new BitSet(new long[]{0x0540A20940003FF0L,0x0024000F0600C849L});
    public static final BitSet FOLLOW_expressionList_in_arguments7432 = new BitSet(new long[]{0x0000000000000000L,0x0000000000008000L});
    public static final BitSet FOLLOW_RPAREN_in_arguments7445 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_literal0 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_classHeader7566 = new BitSet(new long[]{0x0000001000000000L});
    public static final BitSet FOLLOW_CLASS_in_classHeader7568 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_classHeader7570 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_enumHeader7589 = new BitSet(new long[]{0x0000080000000010L});
    public static final BitSet FOLLOW_set_in_enumHeader7591 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_enumHeader7597 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_interfaceHeader7616 = new BitSet(new long[]{0x0080000000000000L});
    public static final BitSet FOLLOW_INTERFACE_in_interfaceHeader7618 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_interfaceHeader7620 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_annotationHeader7639 = new BitSet(new long[]{0x0000000000000000L,0x0004000000000000L});
    public static final BitSet FOLLOW_MONKEYS_AT_in_annotationHeader7641 = new BitSet(new long[]{0x0080000000000000L});
    public static final BitSet FOLLOW_INTERFACE_in_annotationHeader7643 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_annotationHeader7645 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_typeHeader7664 = new BitSet(new long[]{0x0080081000000000L,0x0004000000000000L});
    public static final BitSet FOLLOW_CLASS_in_typeHeader7667 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_ENUM_in_typeHeader7669 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_MONKEYS_AT_in_typeHeader7672 = new BitSet(new long[]{0x0080000000000000L});
    public static final BitSet FOLLOW_INTERFACE_in_typeHeader7676 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_typeHeader7680 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_methodHeader7699 = new BitSet(new long[]{0x0140820940000010L,0x0020000000000801L});
    public static final BitSet FOLLOW_typeParameters_in_methodHeader7701 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000801L});
    public static final BitSet FOLLOW_type_in_methodHeader7705 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_VOID_in_methodHeader7707 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_methodHeader7711 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_LPAREN_in_methodHeader7713 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_fieldHeader7732 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_fieldHeader7734 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_fieldHeader7736 = new BitSet(new long[]{0x0000000000000000L,0x0000000001340000L});
    public static final BitSet FOLLOW_LBRACKET_in_fieldHeader7739 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_fieldHeader7740 = new BitSet(new long[]{0x0000000000000000L,0x0000000001340000L});
    public static final BitSet FOLLOW_set_in_fieldHeader7744 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_variableModifiers_in_localVariableHeader7769 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_localVariableHeader7771 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_localVariableHeader7773 = new BitSet(new long[]{0x0000000000000000L,0x0000000001340000L});
    public static final BitSet FOLLOW_LBRACKET_in_localVariableHeader7776 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_localVariableHeader7777 = new BitSet(new long[]{0x0000000000000000L,0x0000000001340000L});
    public static final BitSet FOLLOW_set_in_localVariableHeader7781 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_annotations_in_synpred2_Java64 = new BitSet(new long[]{0x0800000000000000L});
    public static final BitSet FOLLOW_packageDeclaration_in_synpred2_Java93 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_classDeclaration_in_synpred12_Java436 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_normalClassDeclaration_in_synpred27_Java659 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_normalInterfaceDeclaration_in_synpred43_Java1306 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_fieldDeclaration_in_synpred52_Java1621 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_methodDeclaration_in_synpred53_Java1632 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_classDeclaration_in_synpred54_Java1643 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_explicitConstructorInvocation_in_synpred57_Java1778 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_modifiers_in_synpred59_Java1691 = new BitSet(new long[]{0x0000000000000010L,0x0020000000000000L});
    public static final BitSet FOLLOW_typeParameters_in_synpred59_Java1702 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_synpred59_Java1723 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_formalParameters_in_synpred59_Java1733 = new BitSet(new long[]{0x0000000000000000L,0x0000000000010100L});
    public static final BitSet FOLLOW_THROWS_in_synpred59_Java1744 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_qualifiedNameList_in_synpred59_Java1746 = new BitSet(new long[]{0x0000000000000000L,0x0000000000010000L});
    public static final BitSet FOLLOW_LBRACE_in_synpred59_Java1767 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06137EFFL});
    public static final BitSet FOLLOW_explicitConstructorInvocation_in_synpred59_Java1778 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06137EFFL});
    public static final BitSet FOLLOW_blockStatement_in_synpred59_Java1800 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06137EFFL});
    public static final BitSet FOLLOW_RBRACE_in_synpred59_Java1821 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_interfaceFieldDeclaration_in_synpred68_Java2172 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_interfaceMethodDeclaration_in_synpred69_Java2182 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_interfaceDeclaration_in_synpred70_Java2192 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_classDeclaration_in_synpred71_Java2202 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ellipsisParameterDecl_in_synpred96_Java2953 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_normalParameterDecl_in_synpred98_Java2963 = new BitSet(new long[]{0x0000000000000002L,0x0000000000200000L});
    public static final BitSet FOLLOW_COMMA_in_synpred98_Java2974 = new BitSet(new long[]{0x0140A20940000010L,0x0004000000000001L});
    public static final BitSet FOLLOW_normalParameterDecl_in_synpred98_Java2976 = new BitSet(new long[]{0x0000000000000002L,0x0000000000200000L});
    public static final BitSet FOLLOW_normalParameterDecl_in_synpred99_Java2998 = new BitSet(new long[]{0x0000000000000000L,0x0000000000200000L});
    public static final BitSet FOLLOW_COMMA_in_synpred99_Java3008 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_nonWildcardTypeArguments_in_synpred103_Java3139 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000048L});
    public static final BitSet FOLLOW_set_in_synpred103_Java3165 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_arguments_in_synpred103_Java3197 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_synpred103_Java3199 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_annotationMethodDeclaration_in_synpred117_Java3781 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_interfaceFieldDeclaration_in_synpred118_Java3791 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_normalClassDeclaration_in_synpred119_Java3801 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_normalInterfaceDeclaration_in_synpred120_Java3811 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_enumDeclaration_in_synpred121_Java3821 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_annotationTypeDeclaration_in_synpred122_Java3831 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_localVariableDeclarationStatement_in_synpred125_Java3986 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_classOrInterfaceDeclaration_in_synpred126_Java3996 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ASSERT_in_synpred130_Java4122 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_synpred130_Java4142 = new BitSet(new long[]{0x0000000000000000L,0x0000000010100000L});
    public static final BitSet FOLLOW_COLON_in_synpred130_Java4145 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_synpred130_Java4147 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_synpred130_Java4151 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ASSERT_in_synpred132_Java4161 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_synpred132_Java4164 = new BitSet(new long[]{0x0000000000000000L,0x0000000010100000L});
    public static final BitSet FOLLOW_COLON_in_synpred132_Java4167 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_synpred132_Java4169 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_synpred132_Java4173 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ELSE_in_synpred133_Java4190 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06117EFFL});
    public static final BitSet FOLLOW_statement_in_synpred133_Java4192 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_expression_in_synpred148_Java4404 = new BitSet(new long[]{0x0000000000000000L,0x0000000000100000L});
    public static final BitSet FOLLOW_SEMI_in_synpred148_Java4407 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IDENTIFIER_in_synpred149_Java4417 = new BitSet(new long[]{0x0000000000000000L,0x0000000010000000L});
    public static final BitSet FOLLOW_COLON_in_synpred149_Java4419 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06117EFFL});
    public static final BitSet FOLLOW_statement_in_synpred149_Java4421 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_catches_in_synpred153_Java4573 = new BitSet(new long[]{0x0000400000000000L});
    public static final BitSet FOLLOW_FINALLY_in_synpred153_Java4575 = new BitSet(new long[]{0x0000000000000000L,0x0000000000010002L});
    public static final BitSet FOLLOW_block_in_synpred153_Java4577 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_catches_in_synpred154_Java4591 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FOR_in_synpred157_Java4775 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_LPAREN_in_synpred157_Java4777 = new BitSet(new long[]{0x0140A20940000010L,0x0004000000000001L});
    public static final BitSet FOLLOW_variableModifiers_in_synpred157_Java4779 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_type_in_synpred157_Java4781 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_synpred157_Java4783 = new BitSet(new long[]{0x0000000000000000L,0x0000000010000000L});
    public static final BitSet FOLLOW_COLON_in_synpred157_Java4785 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_synpred157_Java4795 = new BitSet(new long[]{0x0000000000000000L,0x0000000000008000L});
    public static final BitSet FOLLOW_RPAREN_in_synpred157_Java4797 = new BitSet(new long[]{0xF7C5AB59F0003FF0L,0x0024000F06117EFFL});
    public static final BitSet FOLLOW_statement_in_synpred157_Java4799 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_localVariableDeclaration_in_synpred161_Java4962 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_castExpression_in_synpred202_Java6178 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPAREN_in_synpred206_Java6268 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_primitiveType_in_synpred206_Java6270 = new BitSet(new long[]{0x0000000000000000L,0x0000000000008000L});
    public static final BitSet FOLLOW_RPAREN_in_synpred206_Java6272 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_unaryExpression_in_synpred206_Java6274 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOT_in_synpred208_Java6332 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_synpred208_Java6334 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_identifierSuffix_in_synpred209_Java6356 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOT_in_synpred211_Java6388 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_IDENTIFIER_in_synpred211_Java6390 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_identifierSuffix_in_synpred212_Java6412 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACKET_in_synpred224_Java6656 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_synpred224_Java6658 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_synpred224_Java6660 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NEW_in_synpred236_Java6866 = new BitSet(new long[]{0x0000000000000000L,0x0020000000000000L});
    public static final BitSet FOLLOW_nonWildcardTypeArguments_in_synpred236_Java6868 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_classOrInterfaceType_in_synpred236_Java6870 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_classCreatorRest_in_synpred236_Java6872 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NEW_in_synpred237_Java6882 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_classOrInterfaceType_in_synpred237_Java6884 = new BitSet(new long[]{0x0000000000000000L,0x0000000000004000L});
    public static final BitSet FOLLOW_classCreatorRest_in_synpred237_Java6886 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NEW_in_synpred239_Java6915 = new BitSet(new long[]{0x0140820940000010L,0x0000000000000001L});
    public static final BitSet FOLLOW_createdName_in_synpred239_Java6917 = new BitSet(new long[]{0x0000000000000000L,0x0000000000040000L});
    public static final BitSet FOLLOW_LBRACKET_in_synpred239_Java6927 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_synpred239_Java6929 = new BitSet(new long[]{0x0000000000000000L,0x0000000000050000L});
    public static final BitSet FOLLOW_LBRACKET_in_synpred239_Java6940 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_synpred239_Java6942 = new BitSet(new long[]{0x0000000000000000L,0x0000000000050000L});
    public static final BitSet FOLLOW_arrayInitializer_in_synpred239_Java6963 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACKET_in_synpred240_Java7012 = new BitSet(new long[]{0x0540820940003FF0L,0x0020000F06004849L});
    public static final BitSet FOLLOW_expression_in_synpred240_Java7014 = new BitSet(new long[]{0x0000000000000000L,0x0000000000080000L});
    public static final BitSet FOLLOW_RBRACKET_in_synpred240_Java7028 = new BitSet(new long[]{0x0000000000000002L});

}