/*
 * Copyright (C) 2010 Google Inc.
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

package com.google.doclava;

import com.google.doclava.apicheck.ApiParseException;
import com.google.clearsilver.jsilver.data.Data;
import java.util.Comparator;
import java.util.ArrayList;

public class FieldInfo extends MemberInfo {
  public static final Comparator<FieldInfo> comparator = new Comparator<FieldInfo>() {
    public int compare(FieldInfo a, FieldInfo b) {
      return a.name().compareTo(b.name());
    }
  };

  public FieldInfo(String name, ClassInfo containingClass, ClassInfo realContainingClass,
      boolean isPublic, boolean isProtected, boolean isPackagePrivate, boolean isPrivate,
      boolean isFinal, boolean isStatic, boolean isTransient, boolean isVolatile,
      boolean isSynthetic, TypeInfo type, String rawCommentText, Object constantValue,
      SourcePositionInfo position, ArrayList<AnnotationInstanceInfo> annotations) {
    super(rawCommentText, name, null, containingClass, realContainingClass, isPublic, isProtected,
          isPackagePrivate, isPrivate, isFinal, isStatic, isSynthetic, chooseKind(isFinal, isStatic, constantValue),
        position, annotations);
    mIsTransient = isTransient;
    mIsVolatile = isVolatile;
    mType = type;
    mConstantValue = constantValue;
  }

  public FieldInfo cloneForClass(ClassInfo newContainingClass) {
    return new FieldInfo(name(), newContainingClass, realContainingClass(), isPublic(),
        isProtected(), isPackagePrivate(), isPrivate(), isFinal(), isStatic(), isTransient(),
        isVolatile(), isSynthetic(), mType, getRawCommentText(), mConstantValue, position(),
        annotations());
  }

  static String chooseKind(boolean isFinal, boolean isStatic, Object constantValue)
  {
    return isConstant(isFinal, isStatic, constantValue) ? "constant" : "field";
  }
  
  public String qualifiedName() {
    String parentQName
        = (containingClass() != null) ? (containingClass().qualifiedName() + ".") : "";
    return parentQName + name();
  }

  public TypeInfo type() {
    return mType;
  }

  static boolean isConstant(boolean isFinal, boolean isStatic, Object constantValue)
  {
    /*
     * Note: There is an ambiguity in the doc API that prevents us
     * from distinguishing a constant-null from the lack of a
     * constant at all. We err on the side of treating all null
     * constantValues as meaning that the field is not a constant,
     * since having a static final field assigned to null is both
     * unusual and generally pretty useless.
     */
    return isFinal && isStatic && (constantValue != null);
  }

  public boolean isConstant() {
    return isConstant(isFinal(), isStatic(), mConstantValue);
  }

  public TagInfo[] firstSentenceTags() {
    return comment().briefTags();
  }

  public TagInfo[] inlineTags() {
    return comment().tags();
  }

  public Object constantValue() {
    return mConstantValue;
  }

  public String constantLiteralValue() {
    return constantLiteralValue(mConstantValue);
  }

  public void setDeprecated(boolean deprecated) {
    mDeprecatedKnown = true;
    mIsDeprecated = deprecated;
  }
  
  public boolean isDeprecated() {
    if (!mDeprecatedKnown) {
      boolean commentDeprecated = comment().isDeprecated();
      boolean annotationDeprecated = false;
      for (AnnotationInstanceInfo annotation : annotations()) {
        if (annotation.type().qualifiedName().equals("java.lang.Deprecated")) {
          annotationDeprecated = true;
          break;
        }
      }

      if (commentDeprecated != annotationDeprecated) {
        Errors.error(Errors.DEPRECATION_MISMATCH, position(), "Field "
            + mContainingClass.qualifiedName() + "." + name()
            + ": @Deprecated annotation and @deprecated comment do not match");
      }

      mIsDeprecated = commentDeprecated | annotationDeprecated;
      mDeprecatedKnown = true;
    }
    return mIsDeprecated;
  }

  public static String constantLiteralValue(Object val) {
    String str = null;
    if (val != null) {
      if (val instanceof Boolean || val instanceof Byte || val instanceof Short
          || val instanceof Integer) {
        str = val.toString();
      }
      // catch all special values
      else if (val instanceof Double) {
        str = canonicalizeFloatingPoint(val.toString(), "");
      } else if (val instanceof Float) {
        str = canonicalizeFloatingPoint(val.toString(), "f");
      } else if (val instanceof Long) {
        str = val.toString() + "L";
      } else if (val instanceof Character) {
        str = String.format("\'\\u%04x\'", val);
        System.out.println("str=" + str);
      } else if (val instanceof String) {
        str = "\"" + javaEscapeString((String) val) + "\"";
      } else {
        str = "<<<<" + val.toString() + ">>>>";
      }
    }
    if (str == null) {
      str = "null";
    }
    return str;
  }

  /**
   * Returns a canonical string representation of a floating point
   * number. The representation is suitable for use as Java source
   * code. This method also addresses bug #4428022 in the Sun JDK.
   */
  private static String canonicalizeFloatingPoint(String val, String suffix) {
    if (val.equals("Infinity")) {
      return "(1.0" + suffix + "/0.0" + suffix + ")";
    } else if (val.equals("-Infinity")) {
      return "(-1.0" + suffix + "/0.0" + suffix + ")";
    } else if (val.equals("NaN")) {
      return "(0.0" + suffix + "/0.0" + suffix + ")";
    }

    String str = val.toString();
    if (str.indexOf('E') != -1) {
      return str + suffix;
    }

    // 1.0 is the only case where a trailing "0" is allowed.
    // 1.00 is canonicalized as 1.0.
    int i = str.length() - 1;
    int d = str.indexOf('.');
    while (i >= d + 2 && str.charAt(i) == '0') {
      str = str.substring(0, i--);
    }
    return str + suffix;
  }

  public static String javaEscapeString(String str) {
    String result = "";
    final int N = str.length();
    for (int i = 0; i < N; i++) {
      char c = str.charAt(i);
      if (c == '\\') {
        result += "\\\\";
      } else if (c == '\t') {
        result += "\\t";
      } else if (c == '\b') {
        result += "\\b";
      } else if (c == '\r') {
        result += "\\r";
      } else if (c == '\n') {
        result += "\\n";
      } else if (c == '\f') {
        result += "\\f";
      } else if (c == '\'') {
        result += "\\'";
      } else if (c == '\"') {
        result += "\\\"";
      } else if (c >= ' ' && c <= '~') {
        result += c;
      } else {
        result += String.format("\\u%04x", new Integer((int) c));
      }
    }
    return result;
  }

  public static String javaUnescapeString(String str) throws ApiParseException {
    final int N = str.length();
    check: {
      for (int i=0; i<N; i++) {
        final char c = str.charAt(i);
        if (c == '\\') {
          break check;
        }
      }
      return str;
    }

    final StringBuilder buf = new StringBuilder(str.length());
    char escaped = 0;
    final int START = 0;
    final int CHAR1 = 1;
    final int CHAR2 = 2;
    final int CHAR3 = 3;
    final int CHAR4 = 4;
    final int ESCAPE = 5;
    int state = START;

    for (int i=0; i<N; i++) {
      final char c = str.charAt(i);
      switch (state) {
        case START:
          if (c == '\\') {
            state = ESCAPE;
          } else {
            buf.append(c);
          }
          break;
        case ESCAPE:
          switch (c) {
            case '\\':
              buf.append('\\');
              state = START;
              break;
            case 't':
              buf.append('\t');
              state = START;
              break;
            case 'b':
              buf.append('\b');
              state = START;
              break;
            case 'r':
              buf.append('\r');
              state = START;
              break;
            case 'n':
              buf.append('\n');
              state = START;
              break;
            case 'f':
              buf.append('\f');
              state = START;
              break;
            case '\'':
              buf.append('\'');
              state = START;
              break;
            case '\"':
              buf.append('\"');
              state = START;
              break;
            case 'u':
              state = CHAR1;
              escaped = 0;
              break;
          }
          break;
        case CHAR1:
        case CHAR2:
        case CHAR3:
        case CHAR4:
          escaped <<= 4;
          if (c >= '0' && c <= '9') {
            escaped |= c - '0';
          } else if (c >= 'a' && c <= 'f') {
            escaped |= 10 + (c - 'a');
          } else if (c >= 'A' && c <= 'F') {
            escaped |= 10 + (c - 'A');
          } else {
            throw new ApiParseException("bad escape sequence: '" + c + "' at pos " + i + " in: \""
                + str + "\"");
          }
          if (state == CHAR4) {
            buf.append(escaped);
            state = START;
          } else {
            state++;
          }
          break;
      }
    }
    if (state != START) {
      throw new ApiParseException("unfinished escape sequence: " + str);
    }
    return buf.toString();
  }

  public void makeHDF(Data data, String base) {
    data.setValue(base + ".kind", kind());
    type().makeHDF(data, base + ".type");
    data.setValue(base + ".name", name());
    data.setValue(base + ".href", htmlPage());
    data.setValue(base + ".anchor", anchor());
    TagInfo.makeHDF(data, base + ".shortDescr", firstSentenceTags());
    TagInfo.makeHDF(data, base + ".descr", inlineTags());
    TagInfo.makeHDF(data, base + ".deprecated", comment().deprecatedTags());
    TagInfo.makeHDF(data, base + ".seeAlso", comment().seeTags());
    data.setValue(base + ".since", getSince());
    if (isDeprecated()) {
      data.setValue(base + ".deprecatedsince", getDeprecatedSince());
    }
    data.setValue(base + ".final", isFinal() ? "final" : "");
    data.setValue(base + ".static", isStatic() ? "static" : "");
    if (isPublic()) {
      data.setValue(base + ".scope", "public");
    } else if (isProtected()) {
      data.setValue(base + ".scope", "protected");
    } else if (isPackagePrivate()) {
      data.setValue(base + ".scope", "");
    } else if (isPrivate()) {
      data.setValue(base + ".scope", "private");
    }
    Object val = mConstantValue;
    if (val != null) {
      String dec = null;
      String hex = null;
      String str = null;

      if (val instanceof Boolean) {
        str = ((Boolean) val).toString();
      } else if (val instanceof Byte) {
        dec = String.format("%d", val);
        hex = String.format("0x%02x", val);
      } else if (val instanceof Character) {
        dec = String.format("\'%c\'", val);
        hex = String.format("0x%04x", val);
      } else if (val instanceof Double) {
        str = ((Double) val).toString();
      } else if (val instanceof Float) {
        str = ((Float) val).toString();
      } else if (val instanceof Integer) {
        dec = String.format("%d", val);
        hex = String.format("0x%08x", val);
      } else if (val instanceof Long) {
        dec = String.format("%d", val);
        hex = String.format("0x%016x", val);
      } else if (val instanceof Short) {
        dec = String.format("%d", val);
        hex = String.format("0x%04x", val);
      } else if (val instanceof String) {
        str = "\"" + ((String) val) + "\"";
      } else {
        str = "";
      }

      if (dec != null && hex != null) {
        data.setValue(base + ".constantValue.dec", Doclava.escape(dec));
        data.setValue(base + ".constantValue.hex", Doclava.escape(hex));
      } else {
        data.setValue(base + ".constantValue.str", Doclava.escape(str));
        data.setValue(base + ".constantValue.isString", "1");
      }
    }
    
    setFederatedReferences(data, base);
  }

  @Override
  public boolean isExecutable() {
    return false;
  }

  public boolean isTransient() {
    return mIsTransient;
  }

  public boolean isVolatile() {
    return mIsVolatile;
  }
  
  // Check the declared value with a typed comparison, not a string comparison,
  // to accommodate toolchains with different fp -> string conversions.
  private boolean valueEquals(FieldInfo other) {
    if ((mConstantValue == null) != (other.mConstantValue == null)) {
      return false;
    }
    
    // Null values are considered equal
    if (mConstantValue == null) {
      return true;
    }

    return mType.equals(other.mType)
        && mConstantValue.equals(other.mConstantValue);
  }
  
  public boolean isConsistent(FieldInfo fInfo) {
    boolean consistent = true;
    if (!mType.equals(fInfo.mType)) {
      Errors.error(Errors.CHANGED_TYPE, fInfo.position(), "Field " + fInfo.qualifiedName()
          + " has changed type");
      consistent = false;
    } else if (!this.valueEquals(fInfo)) {
      Errors.error(Errors.CHANGED_VALUE, fInfo.position(), "Field " + fInfo.qualifiedName()
          + " has changed value from " + mConstantValue + " to " + fInfo.mConstantValue);
      consistent = false;
    }

    if (!scope().equals(fInfo.scope())) {
      Errors.error(Errors.CHANGED_SCOPE, fInfo.position(), "Method " + fInfo.qualifiedName()
          + " changed scope from " + this.scope() + " to " + fInfo.scope());
      consistent = false;
    }

    if (mIsStatic != fInfo.mIsStatic) {
      Errors.error(Errors.CHANGED_STATIC, fInfo.position(), "Field " + fInfo.qualifiedName()
          + " has changed 'static' qualifier");
      consistent = false;
    }

    if (!mIsFinal && fInfo.mIsFinal) {
      Errors.error(Errors.ADDED_FINAL, fInfo.position(), "Field " + fInfo.qualifiedName()
          + " has added 'final' qualifier");
      consistent = false;
    } else if (mIsFinal && !fInfo.mIsFinal) {
      Errors.error(Errors.REMOVED_FINAL, fInfo.position(), "Field " + fInfo.qualifiedName()
          + " has removed 'final' qualifier");
      consistent = false;
    }

    if (mIsTransient != fInfo.mIsTransient) {
      Errors.error(Errors.CHANGED_TRANSIENT, fInfo.position(), "Field " + fInfo.qualifiedName()
          + " has changed 'transient' qualifier");
      consistent = false;
    }

    if (mIsVolatile != fInfo.mIsVolatile) {
      Errors.error(Errors.CHANGED_VOLATILE, fInfo.position(), "Field " + fInfo.qualifiedName()
          + " has changed 'volatile' qualifier");
      consistent = false;
    }

    if (isDeprecated() != fInfo.isDeprecated()) {
      Errors.error(Errors.CHANGED_DEPRECATED, fInfo.position(), "Field " + fInfo.qualifiedName()
          + " has changed deprecation state");
      consistent = false;
    }

    return consistent;
  }

  public boolean hasValue() {
      return mHasValue;
  }

  public void setHasValue(boolean hasValue) {
      mHasValue = hasValue;
  }

  boolean mIsTransient;
  boolean mIsVolatile;
  boolean mDeprecatedKnown;
  boolean mIsDeprecated;
  boolean mHasValue;
  TypeInfo mType;
  Object mConstantValue;
}
