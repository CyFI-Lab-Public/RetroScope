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

package com.google.doclava.apicheck;

import com.google.doclava.AnnotationInstanceInfo;
import com.google.doclava.ClassInfo;
import com.google.doclava.Converter;
import com.google.doclava.FieldInfo;
import com.google.doclava.MethodInfo;
import com.google.doclava.PackageInfo;
import com.google.doclava.ParameterInfo;
import com.google.doclava.SourcePositionInfo;
import com.google.doclava.TypeInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;

class ApiFile {

  public static ApiInfo parseApi(String filename, InputStream stream) throws ApiParseException {
    final int CHUNK = 1024*1024;
    int hint = 0;
    try {
      hint = stream.available() + CHUNK;
    } catch (IOException ex) {
    }
    if (hint < CHUNK) {
      hint = CHUNK;
    }
    byte[] buf = new byte[hint];
    int size = 0;

    try {
      while (true) {
        if (size == buf.length) {
          byte[] tmp = new byte[buf.length+CHUNK];
          System.arraycopy(buf, 0, tmp, 0, buf.length);
          buf = tmp;
        }
        int amt = stream.read(buf, size, (buf.length-size));
        if (amt < 0) {
          break;
        } else {
          size += amt;
        }
      }
    } catch (IOException ex) {
      throw new ApiParseException("Error reading API file", ex);
    }

    final Tokenizer tokenizer = new Tokenizer(filename, (new String(buf, 0, size)).toCharArray());
    final ApiInfo api = new ApiInfo();

    while (true) {
      String token = tokenizer.getToken();
      if (token == null) {
        break;
      }
      if ("package".equals(token)) {
        parsePackage(api, tokenizer);
      } else {
        throw new ApiParseException("expected package got " + token, tokenizer.getLine());
      }
    }

    api.resolveSuperclasses();
    api.resolveInterfaces();

    return api;
  }

  private static void parsePackage(ApiInfo api, Tokenizer tokenizer)
      throws ApiParseException {
    String token;
    String name;
    PackageInfo pkg;

    token = tokenizer.requireToken();
    assertIdent(tokenizer, token);
    name = token;
    pkg = new PackageInfo(name, tokenizer.pos());
    token = tokenizer.requireToken();
    if (!"{".equals(token)) {
      throw new ApiParseException("expected '{' got " + token, tokenizer.getLine());
    }
    while (true) {
      token = tokenizer.requireToken();
      if ("}".equals(token)) {
        break;
      } else {
        parseClass(api, pkg, tokenizer, token);
      }
    }
    api.addPackage(pkg);
  }

  private static void parseClass(ApiInfo api, PackageInfo pkg, Tokenizer tokenizer, String token)
      throws ApiParseException {
    boolean pub = false;
    boolean prot = false;
    boolean pkgpriv = false;
    boolean stat = false;
    boolean fin = false;
    boolean abs = false;
    boolean dep = false;
    boolean iface;
    String name;
    String qname;
    String ext = null;
    ClassInfo cl;

    if ("public".equals(token)) {
      pub = true;
      token = tokenizer.requireToken();
    } else if ("protected".equals(token)) {
      prot = true;
      token = tokenizer.requireToken();
    } else {
      pkgpriv = true;
    }
    if ("static".equals(token)) {
      stat = true;
      token = tokenizer.requireToken();
    }
    if ("final".equals(token)) {
      fin = true;
      token = tokenizer.requireToken();
    }
    if ("abstract".equals(token)) {
      abs = true;
      token = tokenizer.requireToken();
    }
    if ("deprecated".equals(token)) {
      dep = true;
      token = tokenizer.requireToken();
    }
    if ("class".equals(token)) {
      iface = false;
      token = tokenizer.requireToken();
    } else if ("interface".equals(token)) {
      iface = true;
      token = tokenizer.requireToken();
    } else {
      throw new ApiParseException("missing class or interface. got: " + token, tokenizer.getLine());
    }
    assertIdent(tokenizer, token);
    name = token;
    token = tokenizer.requireToken();
    qname = qualifiedName(pkg.name(), name, null);
    cl = new ClassInfo(null/*classDoc*/, ""/*rawCommentText*/, tokenizer.pos(), pub, prot, 
        pkgpriv, false/*isPrivate*/, stat, iface, abs, true/*isOrdinaryClass*/, 
        false/*isException*/, false/*isError*/, false/*isEnum*/, false/*isAnnotation*/,
        fin, false/*isIncluded*/, name, qname, null/*qualifiedTypeName*/, false/*isPrimitive*/);
    cl.setDeprecated(dep);
    if ("extends".equals(token)) {
      token = tokenizer.requireToken();
      assertIdent(tokenizer, token);
      ext = token;
      token = tokenizer.requireToken();
    }
    // Resolve superclass after done parsing
    api.mapClassToSuper(cl, ext);
    final TypeInfo typeInfo = Converter.obtainTypeFromString(qname) ;
    cl.setTypeInfo(typeInfo);
    cl.setAnnotations(new ArrayList<AnnotationInstanceInfo>());
    if ("implements".equals(token)) {
      while (true) {
        token = tokenizer.requireToken();
        if ("{".equals(token)) {
          break;
        } else {
          /// TODO
          if (!",".equals(token)) {
            api.mapClassToInterface(cl, token);
          }
        }
      }
    }
    if (!"{".equals(token)) {
      throw new ApiParseException("expected {", tokenizer.getLine());
    }
    token = tokenizer.requireToken();
    while (true) {
      if ("}".equals(token)) {
        break;
      } else if ("ctor".equals(token)) {
        token = tokenizer.requireToken();
        parseConstructor(tokenizer, cl, token);
      } else if ("method".equals(token)) {
        token = tokenizer.requireToken();
        parseMethod(tokenizer, cl, token);
      } else if ("field".equals(token)) {
        token = tokenizer.requireToken();
        parseField(tokenizer, cl, token, false);
      } else if ("enum_constant".equals(token)) {
        token = tokenizer.requireToken();
        parseField(tokenizer, cl, token, true);
      } else {
        throw new ApiParseException("expected ctor, enum_constant, field or method", tokenizer.getLine());
      }
      token = tokenizer.requireToken();
    }
    pkg.addClass(cl);
  }
  
  private static void parseConstructor(Tokenizer tokenizer, ClassInfo cl, String token)
      throws ApiParseException {
    boolean pub = false;
    boolean prot = false;
    boolean pkgpriv = false;
    boolean dep = false;
    String name;
    MethodInfo method;

    if ("public".equals(token)) {
      pub = true;
      token = tokenizer.requireToken();
    } else if ("protected".equals(token)) {
      prot = true;
      token = tokenizer.requireToken();
    } else {
      pkgpriv = true;
    }
    if ("deprecated".equals(token)) {
      dep = true;
      token = tokenizer.requireToken();
    }
    assertIdent(tokenizer, token);
    name = token;
    token = tokenizer.requireToken();
    if (!"(".equals(token)) {
      throw new ApiParseException("expected (", tokenizer.getLine());
    }
    //method = new MethodInfo(name, cl.qualifiedName(), false/*static*/, false/*final*/, dep,
    //    pub ? "public" : "protected", tokenizer.pos(), cl);
    method = new MethodInfo(""/*rawCommentText*/, new ArrayList<TypeInfo>()/*typeParameters*/,
        name, null/*signature*/, cl, cl, pub, prot, pkgpriv, false/*isPrivate*/, false/*isFinal*/,
        false/*isStatic*/, false/*isSynthetic*/, false/*isAbstract*/, false/*isSynthetic*/,
        false/*isNative*/,
        false /*isAnnotationElement*/, "constructor", null/*flatSignature*/,
        null/*overriddenMethod*/, cl.asTypeInfo(), new ArrayList<ParameterInfo>(),
        new ArrayList<ClassInfo>()/*thrownExceptions*/, tokenizer.pos(),
        new ArrayList<AnnotationInstanceInfo>()/*annotations*/);
    method.setDeprecated(dep);
    token = tokenizer.requireToken();
    parseParameterList(tokenizer, method, token);
    token = tokenizer.requireToken();
    if ("throws".equals(token)) {
      token = parseThrows(tokenizer, method);
    }
    if (!";".equals(token)) {
      throw new ApiParseException("expected ; found " + token, tokenizer.getLine());
    }
    cl.addConstructor(method);
  }

  private static void parseMethod(Tokenizer tokenizer, ClassInfo cl, String token)
      throws ApiParseException {
    boolean pub = false;
    boolean prot = false;
    boolean pkgpriv = false;
    boolean stat = false;
    boolean fin = false;
    boolean abs = false;
    boolean dep = false;
    boolean syn = false;
    String type;
    String name;
    String ext = null;
    MethodInfo method;

    if ("public".equals(token)) {
      pub = true;
      token = tokenizer.requireToken();
    } else if ("protected".equals(token)) {
      prot = true;
      token = tokenizer.requireToken();
    } else {
      pkgpriv = true;
    }
    if ("static".equals(token)) {
      stat = true;
      token = tokenizer.requireToken();
    }
    if ("final".equals(token)) {
      fin = true;
      token = tokenizer.requireToken();
    }
    if ("abstract".equals(token)) {
      abs = true;
      token = tokenizer.requireToken();
    }
    if ("deprecated".equals(token)) {
      dep = true;
      token = tokenizer.requireToken();
    }
    if ("synchronized".equals(token)) {
      syn = true;
      token = tokenizer.requireToken();
    }
    assertIdent(tokenizer, token);
    type = token;
    token = tokenizer.requireToken();
    assertIdent(tokenizer, token);
    name = token;
    method = new MethodInfo(""/*rawCommentText*/, new ArrayList<TypeInfo>()/*typeParameters*/,
        name, null/*signature*/, cl, cl, pub, prot, pkgpriv, false/*isPrivate*/, fin,
        stat, false/*isSynthetic*/, abs/*isAbstract*/, syn, false/*isNative*/,
        false /*isAnnotationElement*/, "method", null/*flatSignature*/, null/*overriddenMethod*/,
        Converter.obtainTypeFromString(type), new ArrayList<ParameterInfo>(),
        new ArrayList<ClassInfo>()/*thrownExceptions*/, tokenizer.pos(),
        new ArrayList<AnnotationInstanceInfo>()/*annotations*/);
    method.setDeprecated(dep);
    token = tokenizer.requireToken();
    if (!"(".equals(token)) {
      throw new ApiParseException("expected (", tokenizer.getLine());
    }
    token = tokenizer.requireToken();
    parseParameterList(tokenizer, method, token);
    token = tokenizer.requireToken();
    if ("throws".equals(token)) {
      token = parseThrows(tokenizer, method);
    }
    if (!";".equals(token)) {
      throw new ApiParseException("expected ; found " + token, tokenizer.getLine());
    }
    cl.addMethod(method);
  }

  private static void parseField(Tokenizer tokenizer, ClassInfo cl, String token, boolean isEnum)
      throws ApiParseException {
    boolean pub = false;
    boolean prot = false;
    boolean pkgpriv = false;
    boolean stat = false;
    boolean fin = false;
    boolean dep = false;
    boolean trans = false;
    boolean vol = false;
    String type;
    String name;
    String val = null;
    Object v;
    FieldInfo field;

    if ("public".equals(token)) {
      pub = true;
      token = tokenizer.requireToken();
    } else if ("protected".equals(token)) {
      prot = true;
      token = tokenizer.requireToken();
    } else {
      pkgpriv = true;
    }
    if ("static".equals(token)) {
      stat = true;
      token = tokenizer.requireToken();
    }
    if ("final".equals(token)) {
      fin = true;
      token = tokenizer.requireToken();
    }
    if ("deprecated".equals(token)) {
      dep = true;
      token = tokenizer.requireToken();
    }
    if ("transient".equals(token)) {
      trans = true;
      token = tokenizer.requireToken();
    }
    if ("volatile".equals(token)) {
      vol = true;
      token = tokenizer.requireToken();
    }
    assertIdent(tokenizer, token);
    type = token;
    token = tokenizer.requireToken();
    assertIdent(tokenizer, token);
    name = token;
    token = tokenizer.requireToken();
    if ("=".equals(token)) {
      token = tokenizer.requireToken(false);
      val = token;
      token = tokenizer.requireToken();
    }
    if (!";".equals(token)) {
      throw new ApiParseException("expected ; found " + token, tokenizer.getLine());
    }
    try {
      v = parseValue(type, val);
    } catch (ApiParseException ex) {
      ex.line = tokenizer.getLine();
      throw ex;
    }
    field = new FieldInfo(name, cl, cl, pub, prot, pkgpriv, false/*isPrivate*/, fin, stat,
        trans, vol, false, Converter.obtainTypeFromString(type), "", v, tokenizer.pos(),
        new ArrayList<AnnotationInstanceInfo>());
    field.setDeprecated(dep);
    if (isEnum) {
      cl.addEnumConstant(field);
    } else {
      cl.addField(field);
    }
  }

  public static Object parseValue(String type, String val) throws ApiParseException {
    if (val != null) {
      if ("boolean".equals(type)) {
        return "true".equals(val) ? Boolean.TRUE : Boolean.FALSE;
      } else if ("byte".equals(type)) {
        return Integer.valueOf(val);
      } else if ("short".equals(type)) {
        return Integer.valueOf(val);
      } else if ("int".equals(type)) {
        return Integer.valueOf(val);
      } else if ("long".equals(type)) {
        return Long.valueOf(val.substring(0, val.length()-1));
      } else if ("float".equals(type)) {
        if ("(1.0f/0.0f)".equals(val) || "(1.0f / 0.0f)".equals(val)) {
          return Float.POSITIVE_INFINITY;
        } else if ("(-1.0f/0.0f)".equals(val) || "(-1.0f / 0.0f)".equals(val)) {
          return Float.NEGATIVE_INFINITY;
        } else if ("(0.0f/0.0f)".equals(val) || "(0.0f / 0.0f)".equals(val)) {
          return Float.NaN;
        } else {
          return Float.valueOf(val);
        }
      } else if ("double".equals(type)) {
        if ("(1.0/0.0)".equals(val) || "(1.0 / 0.0)".equals(val)) {
          return Double.POSITIVE_INFINITY;
        } else if ("(-1.0/0.0)".equals(val) || "(-1.0 / 0.0)".equals(val)) {
          return Double.NEGATIVE_INFINITY;
        } else if ("(0.0/0.0)".equals(val) || "(0.0 / 0.0)".equals(val)) {
          return Double.NaN;
        } else {
          return Double.valueOf(val);
        }
      } else if ("char".equals(type)) {
        return new Integer((char)Integer.parseInt(val));
      } else if ("java.lang.String".equals(type)) {
        if ("null".equals(val)) {
          return null;
        } else {
          return FieldInfo.javaUnescapeString(val.substring(1, val.length()-1));
        }
      }
    }
    if ("null".equals(val)) {
      return null;
    } else {
      return val;
    }
  }

  private static void parseParameterList(Tokenizer tokenizer, AbstractMethodInfo method,
      String token) throws ApiParseException {
    while (true) {
      if (")".equals(token)) {
        return;
      }

      String type = token;
      String name = null;
      token = tokenizer.requireToken();
      if (isIdent(token)) {
        name = token;
        token = tokenizer.requireToken();
      }
      if (",".equals(token)) {
        token = tokenizer.requireToken();
      } else if (")".equals(token)) {
      } else {
        throw new ApiParseException("expected , found " + token, tokenizer.getLine());
      }
      method.addParameter(new ParameterInfo(name, type,
            Converter.obtainTypeFromString(type),
            type.endsWith("..."),
            tokenizer.pos()));
      if (type.endsWith("...")) {
        method.setVarargs(true);
      }
    }
  }

  private static String parseThrows(Tokenizer tokenizer, AbstractMethodInfo method)
      throws ApiParseException {
    String token = tokenizer.requireToken();
    boolean comma = true;
    while (true) {
      if (";".equals(token)) {
        return token;
      } else if (",".equals(token)) {
        if (comma) {
          throw new ApiParseException("Expected exception, got ','", tokenizer.getLine());
        }
        comma = true;
      } else {
        if (!comma) {
          throw new ApiParseException("Expected ',' or ';' got " + token, tokenizer.getLine());
        }
        comma = false;
        method.addException(token);
      }
      token = tokenizer.requireToken();
    }
  }

  private static String qualifiedName(String pkg, String className, ClassInfo parent) {
    String parentQName = (parent != null) ? (parent.qualifiedName() + ".") : "";
    return pkg + "." + parentQName + className;
  }

  public static boolean isIdent(String token) {
    return isident(token.charAt(0));
  }

  public static void assertIdent(Tokenizer tokenizer, String token) throws ApiParseException {
    if (!isident(token.charAt(0))) {
      throw new ApiParseException("Expected identifier: " + token, tokenizer.getLine());
    }
  }
  
  static class Tokenizer {
    char[] mBuf;
    String mFilename;
    int mPos;
    int mLine = 1;
    Tokenizer(String filename, char[] buf) {
      mFilename = filename;
      mBuf = buf;
    }

    public SourcePositionInfo pos() {
      return new SourcePositionInfo(mFilename, mLine, 0);
    }

    public int getLine() {
      return mLine;
    }

    boolean eatWhitespace() {
      boolean ate = false;
      while (mPos < mBuf.length && isspace(mBuf[mPos])) {
        if (mBuf[mPos] == '\n') {
          mLine++;
        }
        mPos++;
        ate = true;
      }
      return ate;
    }

    boolean eatComment() {
      if (mPos+1 < mBuf.length) {
        if (mBuf[mPos] == '/' && mBuf[mPos+1] == '/') {
          mPos += 2;
          while (mPos < mBuf.length && !isnewline(mBuf[mPos])) {
            mPos++;
          }
          return true;
        }
      }
      return false;
    }

    void eatWhitespaceAndComments() {
      while (eatWhitespace() || eatComment()) {
      }
    }

    public String requireToken() throws ApiParseException {
      return requireToken(true);
    }

    public String requireToken(boolean parenIsSep) throws ApiParseException {
      final String token = getToken(parenIsSep);
      if (token != null) {
        return token;
      } else {
        throw new ApiParseException("Unexpected end of file", mLine);
      }
    }

    public String getToken() throws ApiParseException {
      return getToken(true);
    }

    public String getToken(boolean parenIsSep) throws ApiParseException {
      eatWhitespaceAndComments();
      if (mPos >= mBuf.length) {
        return null;
      }
      final int line = mLine;
      final char c = mBuf[mPos];
      final int start = mPos;
      mPos++;
      if (c == '"') {
        final int STATE_BEGIN = 0;
        final int STATE_ESCAPE = 1;
        int state = STATE_BEGIN;
        while (true) {
          if (mPos >= mBuf.length) {
            throw new ApiParseException("Unexpected end of file for \" starting at " + line, mLine);
          }
          final char k = mBuf[mPos];
          if (k == '\n' || k == '\r') {
            throw new ApiParseException("Unexpected newline for \" starting at " + line, mLine);
          }
          mPos++;
          switch (state) {
            case STATE_BEGIN:
              switch (k) {
                case '\\':
                  state = STATE_ESCAPE;
                  mPos++;
                  break;
                case '"':
                  return new String(mBuf, start, mPos-start);
              }
            case STATE_ESCAPE:
              state = STATE_BEGIN;
              break;
          }
        }
      } else if (issep(c, parenIsSep)) {
        return "" + c;
      } else {
        int genericDepth = 0;
        do {
          while (mPos < mBuf.length && !isspace(mBuf[mPos]) && !issep(mBuf[mPos], parenIsSep)) {
            mPos++;
          }
          if (mPos < mBuf.length) {
            if (mBuf[mPos] == '<') {
              genericDepth++;
              mPos++;
            } else if (mBuf[mPos] == '>') {
              genericDepth--;
              mPos++;
            } else if (genericDepth != 0) {
              mPos++;
            }
          }
        } while (mPos < mBuf.length
            && ((!isspace(mBuf[mPos]) && !issep(mBuf[mPos], parenIsSep)) || genericDepth != 0));
        if (mPos >= mBuf.length) {
          throw new ApiParseException("Unexpected end of file for \" starting at " + line, mLine);
        }
        return new String(mBuf, start, mPos-start);
      }
    }
  }

  static boolean isspace(char c) {
    return c == ' ' || c == '\t' || c == '\n' || c == '\r';
  }

  static boolean isnewline(char c) {
    return c == '\n' || c == '\r';
  }

  static boolean issep(char c, boolean parenIsSep) {
    if (parenIsSep) {
      if (c == '(' || c == ')') {
        return true;
      }
    }
    return c == '{' || c == '}' || c == ',' || c == ';' || c == '<' || c == '>';
  }

  static boolean isident(char c) {
    if (c == '"' || issep(c, true)) {
      return false;
    }
    return true;
  }
}

