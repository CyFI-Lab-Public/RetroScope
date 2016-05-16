/*
 * Copyright (C) 2000 Google Inc.
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
package com.android.mail.lib.base;

import static com.android.mail.lib.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static utility methods and constants pertaining to {@code String} or {@code
 * CharSequence} instances.
 */
public final class StringUtil {
  private StringUtil() {} // COV_NF_LINE

  /**
   * A completely arbitrary selection of eight whitespace characters. See
   * <a href="http://go/white+space">this spreadsheet</a> for more details
   * about whitespace characters.
   *
   * @deprecated Rewrite your code to use {@link CharMatcher#WHITESPACE}, or
   *     consider the precise set of characters you want to match and construct
   *     the right explicit {@link CharMatcher} or {@link String} for your own
   *     purposes.
   */
  @Deprecated
  public static final String WHITE_SPACES = " \r\n\t\u3000\u00A0\u2007\u202F";

  /** A string containing the carriage return and linefeed characters. */
  public static final String LINE_BREAKS = "\r\n";

  /**
   * Old location of {@link Strings#isNullOrEmpty}; this method will be
   * deprecated soon.
   */
  public static boolean isEmpty(String string) {
    return Strings.isNullOrEmpty(string);
  }

  /**
   * Returns {@code true} if the given string is null, empty, or comprises only
   * whitespace characters, as defined by {@link CharMatcher#WHITESPACE}.
   *
   * <p><b>Warning:</b> there are many competing definitions of "whitespace";
   * please see <a href="http://go/white+space">this spreadsheet</a> for
   * details.
   *
   * @param string the string reference to check
   * @return {@code true} if {@code string} is null, empty, or consists of
   *     whitespace characters only
   */
  public static boolean isEmptyOrWhitespace(String string) {
    return string == null || CharMatcher.WHITESPACE.matchesAllOf(string);
  }

  /**
   * Old location of {@link Strings#nullToEmpty}; this method will be
   * deprecated soon.
   */
  public static String makeSafe(String string) {
    return Strings.nullToEmpty(string);
  }

  /**
   * Old location of {@link Strings#emptyToNull}; this method will be
   * deprecated soon.
   */
  public static String toNullIfEmpty(String string) {
    return Strings.emptyToNull(string);
  }

  /**
   * Returns the given string if it is nonempty and contains at least one
   * non-whitespace character; {@code null} otherwise. See comment in {@link
   * #isEmptyOrWhitespace} on the definition of whitespace.
   *
   * @param string the string to test and possibly return
   * @return {@code null} if {@code string} is null, empty, or contains only
   *     whitespace characters; {@code string} itself otherwise
   */
  public static String toNullIfEmptyOrWhitespace(
      String string) {
    return isEmptyOrWhitespace(string) ? null : string;
  }

  /**
   * Old location of {@link Strings#repeat}; this method will be deprecated
   * soon.
   */
  public static String repeat(String string, int count) {
    return Strings.repeat(string, count);
  }

  /**
   * Return the first index in the string of any of the specified characters,
   * starting at a given index, or {@code -1} if none of the characters is
   * present.
   *
   * @param string the non-null character sequence to look in
   * @param chars a non-null character sequence containing the set of characters
   *     to look for. If empty, this method will find no matches and return
   *     {@code -1}
   * @param fromIndex the index of the first character to examine in the input
   *     string. If negative, the entire string will be searched. If greater
   *     than or equal to the string length, no characters will be searched and
   *     {@code -1} will be returned.
   * @return the index of the first match, or {@code -1} if no match was found.
   *     Guaranteed to be either {@code -1} or a number greater than or equal to
   *     {@code fromIndex}
   * @throws NullPointerException if any argument is null
   */
  // author: pault
  public static int indexOfChars(
      CharSequence string, CharSequence chars, int fromIndex) {
    if (fromIndex >= string.length()) {
      return -1;
    }

    /*
     * Prepare lookup structures for the characters. TODO(pault): This loop
     * could be factored into another method to allow caching of the resulting
     * struct if a use-case of very large character sets exists.
     */
    Set<Character> charSet = Collections.emptySet();
    boolean[] charArray = new boolean[128];
    for (int i = 0; i < chars.length(); i++) {
      char c = chars.charAt(i);
      if (c < 128) {
        charArray[c] = true;
      } else {
        if (charSet.isEmpty()) {
          charSet = new HashSet<Character>();
        }
        charSet.add(c);
      }
    }

    // Scan the string for matches
    for (int i = Math.max(fromIndex, 0); i < string.length(); i++) {
      char c = string.charAt(i);
      if (c < 128) {
        if (charArray[c]) {
          return i;
        }
      } else if (charSet.contains(c)) {
        return i;
      }
    }
    return -1;
  }

/*
 * -------------------------------------------------------------------
 * This marks the end of the code that has been written or rewritten
 * in 2008 to the quality standards of the Java core libraries group.
 * Code below this point is still awaiting cleanup (you can help!).
 * See http://wiki/Nonconf/JavaCoreLibrariesStandards.
 * -------------------------------------------------------------------
 */


  /**
   * @param str the string to split.  Must not be null.
   * @param delims the delimiter characters. Each character in the
   *        string is individually treated as a delimiter.
   * @return an array of tokens. Will not return null. Individual tokens
   *        do not have leading/trailing whitespace removed.
   * @deprecated see the detailed instructions under
   *     {@link #split(String, String, boolean)}
   */
  @Deprecated
  public static String[] split(String str, String delims) {
    return split(str, delims, false);
  }

  /**
   * This method is deprecated because it is too inflexible, providing
   * only a very specific set of behaviors that almost never matches exactly
   * what you intend. Prefer using a {@link Splitter}, which is more flexible
   * and consistent in the way it handles trimming and empty tokens.
   *
   * <ul>
   * <li>Create a {@link Splitter} using {@link Splitter#on(CharMatcher)} such
   *     as {@code Splitter.on(CharMatcher.anyOf(delims))}.
   * <li><i>If</i> you need whitespace trimmed from the ends of each segment,
   *     adding {@code .trimResults()} to your splitter definition should work
   *     in most cases. To match the exact behavior of this method, use
   *     {@code .trimResults(CharMatcher.inRange('\0', ' '))}.
   * <li>This method silently ignores empty tokens in the input, but allows
   *     empty tokens to appear in the output if {@code trimTokens} is
   *     {@code true}. Adding {@code .omitEmptyStrings()} to your splitter
   *     definition will filter empty tokens out but will do so <i>after</i>
   *     having performed trimming. If you absolutely require this method's
   *     behavior in this respect, Splitter is not able to match it.
   * <li>If you need the result as an array, use {@link
   *     com.google.common.collect.Iterables#toArray(Iterable, Class)} on the
   *     {@code Iterable<String>} returned by {@link Splitter#split}.
   * </ul>
   *
   * @param str the string to split.  Must not be null.
   * @param delims the delimiter characters. Each character in the string
   *        is individually treated as a delimiter.
   * @param trimTokens if true, leading/trailing whitespace is removed
   *        from the tokens
   * @return an array of tokens. Will not return null.
   * @deprecated
   */
  @Deprecated
  public static String[] split(
      String str, String delims, boolean trimTokens) {
    StringTokenizer tokenizer = new StringTokenizer(str, delims);
    int n = tokenizer.countTokens();
    String[] list = new String[n];
    for (int i = 0; i < n; i++) {
      if (trimTokens) {
        list[i] = tokenizer.nextToken().trim();
      } else {
        list[i] = tokenizer.nextToken();
      }
    }
    return list;
  }

  /**
   * Trim characters from only the beginning of a string.
   * This is a convenience method, it simply calls trimStart(s, null).
   *
   * @param s String to be trimmed
   * @return String with whitespace characters removed from the beginning
   */
  public static String trimStart(String s) {
    return trimStart(s, null);
  }

  /**
   * Trim characters from only the beginning of a string.
   * This method will remove all whitespace characters
   * (defined by Character.isWhitespace(char), in addition to the characters
   * provided, from the end of the provided string.
   *
   * @param s String to be trimmed
   * @param extraChars Characters in addition to whitespace characters that
   *                   should be trimmed.  May be null.
   * @return String with whitespace and characters in extraChars removed
   *                   from the beginning
   */
  public static String trimStart(String s, String extraChars) {
    int trimCount = 0;
    while (trimCount < s.length()) {
      char ch = s.charAt(trimCount);
      if (Character.isWhitespace(ch)
        || (extraChars != null && extraChars.indexOf(ch) >= 0)) {
        trimCount++;
      } else {
        break;
      }
    }

    if (trimCount == 0) {
      return s;
    }
    return s.substring(trimCount);
  }

  /**
   * Trim characters from only the end of a string.
   * This is a convenience method, it simply calls trimEnd(s, null).
   *
   * @param s String to be trimmed
   * @return String with whitespace characters removed from the end
   */
  public static String trimEnd(String s) {
    return trimEnd(s, null);
  }

  /**
   * Trim characters from only the end of a string.
   * This method will remove all whitespace characters
   * (defined by Character.isWhitespace(char), in addition to the characters
   * provided, from the end of the provided string.
   *
   * @param s String to be trimmed
   * @param extraChars Characters in addition to whitespace characters that
   *                   should be trimmed.  May be null.
   * @return String with whitespace and characters in extraChars removed
   *                   from the end
   */
  public static String trimEnd(String s, String extraChars) {
    int trimCount = 0;
    while (trimCount < s.length()) {
      char ch = s.charAt(s.length() - trimCount - 1);
      if (Character.isWhitespace(ch)
        || (extraChars != null && extraChars.indexOf(ch) >= 0)) {
        trimCount++;
      } else {
        break;
      }
    }

    if (trimCount == 0) {
      return s;
    }
    return s.substring(0, s.length() - trimCount);
  }

  /**
   * @param str the string to split.  Must not be null.
   * @param delims the delimiter characters. Each character in the
   *        string is individually treated as a delimiter.
   * @return an array of tokens. Will not return null. Leading/trailing
   *        whitespace is removed from the tokens.
   * @deprecated see the detailed instructions under
   *     {@link #split(String, String, boolean)}
   */
  @Deprecated
  public static String[] splitAndTrim(String str, String delims) {
    return split(str, delims, true);
  }

  /** Parse comma-separated list of ints and return as array. */
  public static int[] splitInts(String str) throws IllegalArgumentException {
    StringTokenizer tokenizer = new StringTokenizer(str, ",");
    int n = tokenizer.countTokens();
    int[] list = new int[n];
    for (int i = 0; i < n; i++) {
      String token = tokenizer.nextToken();
      list[i] = Integer.parseInt(token);
    }
    return list;
  }

  /** Parse comma-separated list of longs and return as array. */
  public static long[] splitLongs(String str) throws IllegalArgumentException {
    StringTokenizer tokenizer = new StringTokenizer(str, ",");
    int n = tokenizer.countTokens();
    long[] list = new long[n];
    for (int i = 0; i < n; i++) {
      String token = tokenizer.nextToken();
      list[i] = Long.parseLong(token);
    }
    return list;
  }

  /** This replaces the occurrences of 'what' in 'str' with 'with'
   *
   * @param str the string to process
   * @param what to replace
   * @param with replace with this
   * @return String str where 'what' was replaced with 'with'
   *
   * @deprecated Please use {@link String#replace(CharSequence, CharSequence)}.
   */
  @Deprecated
  public static String replace(
      String str, CharSequence what, CharSequence with) {
    // Have to check this argument, for compatibility with the old impl.
    // For the record, String.replace() is capable of handling an empty target
    // string... but it does something kind of weird in that case.
    checkArgument(what.length() > 0);
    return str.replace(what, with);
  }

  private static final Splitter NEWLINE_SPLITTER =
      Splitter.on('\n').omitEmptyStrings();

  /**
   * Reformats the given string to a fixed width by inserting carriage returns
   * and trimming unnecessary whitespace. See
   * {@link #fixedWidth(String[], int)} for details. The {@code str} argument
   * to this method will be split on newline characters ({@code '\n'}) only
   * (regardless of platform).  An array of resulting non-empty strings is
   * then passed to {@link #fixedWidth(String[], int)} as the {@code lines}
   * parameter.
   *
   * @param str the string to format
   * @param width the fixed width (in characters)
   */
  public static String fixedWidth(String str, int width) {
    List<String> lines = new ArrayList<String>();

    for (String line : NEWLINE_SPLITTER.split(str)) {
      lines.add(line);
    }

    String[] lineArray = lines.toArray(new String[0]);
    return fixedWidth(lineArray, width);
  }

  /**
   * Reformats the given array of lines to a fixed width by inserting
   * newlines and trimming unnecessary whitespace.  This uses simple
   * whitespace-based splitting, not sophisticated internationalized
   * line breaking.  Newlines within a line are treated like any other
   * whitespace.  Lines which are already short enough will be passed
   * through unmodified.
   *
   * <p>Only breaking whitespace characters (those which match
   * {@link CharMatcher#BREAKING_WHITESPACE}) are treated as whitespace by
   * this method. Non-breaking whitespace characters will be considered as
   * ordinary characters which are connected to any other adjacent
   * non-whitespace characters, and will therefore appear in the returned
   * string in their original context.
   *
   * @param lines array of lines to format
   * @param width the fixed width (in characters)
   */
  public static String fixedWidth(String[] lines, int width) {
    List<String> formattedLines = new ArrayList<String>();

    for (String line : lines) {
      formattedLines.add(formatLineToFixedWidth(line, width));
    }

    return Joiner.on('\n').join(formattedLines);
  }

  private static final Splitter TO_WORDS =
      Splitter.on(CharMatcher.BREAKING_WHITESPACE).omitEmptyStrings();

  /**
   * Helper method for {@link #fixedWidth(String[], int)}
   */
  private static String formatLineToFixedWidth(String line, int width) {
    if (line.length() <= width) {
      return line;
    }

    StringBuilder builder = new StringBuilder();
    int col = 0;

    for (String word : TO_WORDS.split(line)) {
      if (col == 0) {
        col = word.length();
      } else {
        int newCol = col + word.length() + 1;  // +1 for the space

        if (newCol <= width) {
          builder.append(' ');
          col = newCol;
        } else {
          builder.append('\n');
          col = word.length();
        }
      }

      builder.append(word);
    }

    return builder.toString();
  }

  /**
   * Splits the argument original into a list of substrings.  All the
   * substrings in the returned list (except possibly the last) will
   * have length lineLen.
   *
   * @param lineLen  the length of the substrings to put in the list
   * @param original the original string
   *
   * @return a list of strings of length lineLen that together make up the
   *     original string
   * @deprecated use {@code Splitter.fixedLength(lineLen).split(original))}
   *     (note that it returns an {@code Iterable}, not a {@code List})
   */
  @Deprecated
  public static List<String> fixedSplit(String original, int lineLen) {
    List<String> output = new ArrayList<String>();
    for (String elem : Splitter.fixedLength(lineLen).split(original)) {
      output.add(elem);
    }
    return output;
  }

  /**
   * Indents the given String per line.
   * @param iString the string to indent
   * @param iIndentDepth the depth of the indentation
   * @return the indented string
   */
  public static String indent(String iString, int iIndentDepth) {
    StringBuilder spacer = new StringBuilder();
    spacer.append("\n");
    for (int i = 0; i < iIndentDepth; i++) {
      spacer.append("  ");
    }
    return iString.replace("\n", spacer.toString());
  }

  /**
   * This is a both way strip.
   *
   * @param str the string to strip
   * @param left strip from left
   * @param right strip from right
   * @param what character(s) to strip
   * @return the stripped string
   * @deprecated ensure the string is not null and use
   *  <ul>
   *    <li> {@code CharMatcher.anyOf(what).trimFrom(str)}
   *        if {@code left == true} and {@code right == true}
   *    <li> {@code CharMatcher.anyOf(what).trimLeadingFrom(str)}
   *        if {@code left == true} and {@code right == false}
   *    <li> {@code CharMatcher.anyOf(what).trimTrailingFrom(str)}
   *        if {@code left == false} and {@code right == true}
   *  </ul>
   */
  @Deprecated
  public static String megastrip(String str,
                                 boolean left, boolean right,
                                 String what) {
    if (str == null) {
      return null;
    }

    CharMatcher matcher = CharMatcher.anyOf(what);
    if (left) {
      if (right) {
        return matcher.trimFrom(str);
      }
      return matcher.trimLeadingFrom(str);
    }
    if (right) {
      return matcher.trimTrailingFrom(str);
    }
    return str;
  }

  /** strip - strips both ways
   *
   * @param str what to strip
   * @return String the striped string
   * @deprecated ensure the string is not null and use {@code
   *     CharMatcher.LEGACY_WHITESPACE.trimFrom(str)}; also consider whether you
   *     really want the legacy whitespace definition, or something more
   *     standard like {@link CharMatcher#WHITESPACE}.
   */
  @SuppressWarnings("deprecation") // this is deprecated itself
  @Deprecated public static String strip(String str) {
    return (str == null) ? null : CharMatcher.LEGACY_WHITESPACE.trimFrom(str);
  }

  /** Strip white spaces from both end, and collapse white spaces
   * in the middle.
   *
   * @param str what to strip
   * @return String the striped and collapsed string
   * @deprecated ensure the string is not null and use {@code
   *     CharMatcher.LEGACY_WHITESPACE.trimAndCollapseFrom(str, ' ')}; also
   *     consider whether you really want the legacy whitespace definition, or
   *     something more standard like {@link CharMatcher#WHITESPACE}.
   */
  @SuppressWarnings("deprecation") // this is deprecated itself
  @Deprecated public static String stripAndCollapse(String str) {
    return (str == null) ? null
        : CharMatcher.LEGACY_WHITESPACE.trimAndCollapseFrom(str, ' ');
  }

  /**
   * Give me a string and a potential prefix, and I return the string
   * following the prefix if the prefix matches, else null.
   * Analogous to the c++ functions strprefix and var_strprefix.
   *
   * @param str the string to strip
   * @param prefix the expected prefix
   * @return the stripped string or <code>null</code> if the string
   * does not start with the prefix
   */
  public static String stripPrefix(String str, String prefix) {
    return str.startsWith(prefix)
        ? str.substring(prefix.length())
        : null;
  }

  /**
   * Case insensitive version of stripPrefix. Strings are compared in
   * the same way as in {@link String#equalsIgnoreCase}.
   * Analogous to the c++ functions strcaseprefix and var_strcaseprefix.
   *
   * @param str the string to strip
   * @param prefix the expected prefix
   * @return the stripped string or <code>null</code> if the string
   * does not start with the prefix
   */
  public static String stripPrefixIgnoreCase(String str, String prefix) {
    return startsWithIgnoreCase(str, prefix)
        ? str.substring(prefix.length())
        : null;
  }

  /**
   * Give me a string and a potential suffix, and I return the string
   * before the suffix if the suffix matches, else null.
   * Analogous to the c++ function strsuffix.
   *
   * @param str the string to strip
   * @param suffix the expected suffix
   * @return the stripped string or <code>null</code> if the string
   * does not end with the suffix
   */
  public static String stripSuffix(String str, String suffix) {
    return str.endsWith(suffix)
        ? str.substring(0, str.length() - suffix.length())
        : null;
  }

  /**
   * Case insensitive version of stripSuffix. Strings are compared in
   * the same way as in {@link String#equalsIgnoreCase}.
   * Analogous to the c++ function strcasesuffix.
   *
   * @param str the string to strip
   * @param suffix the expected suffix
   * @return the stripped string or <code>null</code> if the string
   * does not end with the suffix
   */
  public static String stripSuffixIgnoreCase(
      String str, String suffix) {
    return endsWithIgnoreCase(str, suffix)
        ? str.substring(0, str.length() - suffix.length())
        : null;
  }

  /**
   * Strips all non-digit characters from a string.
   *
   * The resulting string will only contain characters for which isDigit()
   * returns true.
   *
   * @param str the string to strip
   * @return a string consisting of digits only, or an empty string
   * @deprecated use {@code CharMatcher.JAVA_DIGIT.retainFrom(str)} (also
   *     consider whether this is really the definition of "digit" you wish to
   *     use)
   */
  @Deprecated public static String stripNonDigits(String str) {
    return CharMatcher.JAVA_DIGIT.retainFrom(str);
  }

  /**
   * Finds the last index in str of a character not in the characters
   * in 'chars' (similar to ANSI string.find_last_not_of).
   *
   * Returns -1 if no such character can be found.
   *
   * <p><b>Note:</b> If {@code fromIndex} is zero, use {@link CharMatcher}
   * instead for this: {@code CharMatcher.noneOf(chars).lastIndexIn(str)}.
   */
  // TODO(kevinb): after adding fromIndex versions of (last)IndexOf to
  // CharMatcher, deprecate this
  public static int lastIndexNotOf(String str, String chars, int fromIndex) {
    fromIndex = Math.min(fromIndex, str.length() - 1);

    for (int pos = fromIndex; pos >= 0; pos--) {
      if (chars.indexOf(str.charAt(pos)) < 0) {
        return pos;
      }
    }

    return -1;
  }

  /**
   * Like String.replace() except that it accepts any number of old chars.
   * Replaces any occurrances of 'oldchars' in 'str' with 'newchar'.
   * Example: replaceChars("Hello, world!", "H,!", ' ') returns " ello  world "
   *
   * @deprecated use {@code CharMatcher#replaceFrom(String, char)}, for example
   *     {@code CharMatcher.anyOf(oldchars).replaceFrom(str, newchar)}
   */
  @Deprecated public static String replaceChars(
      String str, CharSequence oldchars, char newchar) {
    return CharMatcher.anyOf(oldchars).replaceFrom(str, newchar);
  }

  /**
   * Remove any occurrances of 'oldchars' in 'str'.
   * Example: removeChars("Hello, world!", ",!") returns "Hello world"
   *
   * @deprecated use {@link CharMatcher#removeFrom(CharSequence)}, for example
   *     {@code CharMatcher.anyOf(oldchars).removeFrom(str)}
   */
  @Deprecated public static String removeChars(
      String str, CharSequence oldchars) {
    return CharMatcher.anyOf(oldchars).removeFrom(str);
  }

  // See http://www.microsoft.com/typography/unicode/1252.htm
  private static final CharMatcher FANCY_SINGLE_QUOTE
      = CharMatcher.anyOf("\u0091\u0092\u2018\u2019");
  private static final CharMatcher FANCY_DOUBLE_QUOTE
      = CharMatcher.anyOf("\u0093\u0094\u201c\u201d");

  /**
   * Replaces microsoft "smart quotes" (curly " and ') with their
   * ascii counterparts.
   */
  public static String replaceSmartQuotes(String str) {
    String tmp = FANCY_SINGLE_QUOTE.replaceFrom(str, '\'');
    return FANCY_DOUBLE_QUOTE.replaceFrom(tmp, '"');
  }

  /**
   * Convert a string of hex digits to a byte array, with the first
   * byte in the array being the MSB. The string passed in should be
   * just the raw digits (upper or lower case), with no leading
   * or trailing characters (like '0x' or 'h').
   * An odd number of characters is supported.
   * If the string is empty, an empty array will be returned.
   *
   * This is significantly faster than using
   *   new BigInteger(str, 16).toByteArray();
   * especially with larger strings. Here are the results of some
   * microbenchmarks done on a P4 2.8GHz 2GB RAM running
   * linux 2.4.22-gg11 and JDK 1.5 with an optimized build:
   *
   * String length        hexToBytes (usec)   BigInteger
   * -----------------------------------------------------
   * 16                       0.570                 1.43
   * 256                      8.21                 44.4
   * 1024                    32.8                 526
   * 16384                  546                121000
   */
  public static byte[] hexToBytes(CharSequence str) {
    byte[] bytes = new byte[(str.length() + 1) / 2];
    if (str.length() == 0) {
      return bytes;
    }
    bytes[0] = 0;
    int nibbleIdx = (str.length() % 2);
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (!isHex(c)) {
        throw new IllegalArgumentException("string contains non-hex chars");
      }
      if ((nibbleIdx % 2) == 0) {
        bytes[nibbleIdx >> 1] = (byte) (hexValue(c) << 4);
      } else {
        bytes[nibbleIdx >> 1] += (byte) hexValue(c);
      }
      nibbleIdx++;
    }
    return bytes;
  }

  /**
   * Converts any instances of "\r" or "\r\n" style EOLs into "\n" (Line Feed).
   */
  public static String convertEOLToLF(String input) {
    StringBuilder res = new StringBuilder(input.length());
    char[] s = input.toCharArray();
    int from = 0;
    final int end = s.length;
    for (int i = 0; i < end; i++) {
      if (s[i] == '\r') {
        res.append(s, from, i - from);
        res.append('\n');
        if (i + 1 < end && s[i + 1] == '\n') {
          i++;
        }

        from = i + 1;
      }
    }

    if (from == 0) {   // no \r!
      return input;
    }

    res.append(s, from, end - from);
    return res.toString();
  }

  /**
   * Old location of {@link Strings#padStart}; this method will be deprecated
   * soon.
   */
  public static String padLeft(String s, int len, char padChar) {
    return Strings.padStart(s, len, padChar);
  }

  /**
   * Old location of {@link Strings#padEnd}; this method will be deprecated
   * soon.
   */
  public static String padRight(String s, int len, char padChar) {
    return Strings.padEnd(s, len, padChar);
  }

  /**
   * Returns a string consisting of "s", with each of the first "len" characters
   * replaced by "maskChar" character.
   */
  public static String maskLeft(String s, int len, char maskChar) {
    if (len <= 0) {
      return s;
    }
    len = Math.min(len, s.length());
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; i++) {
      sb.append(maskChar);
    }
    sb.append(s.substring(len));
    return sb.toString();
  }

  private static boolean isOctal(char c) {
    return (c >= '0') && (c <= '7');
  }

  private static boolean isHex(char c) {
    return ((c >= '0') && (c <= '9')) ||
           ((c >= 'a') && (c <= 'f')) ||
           ((c >= 'A') && (c <= 'F'));
  }

  private static int hexValue(char c) {
    if ((c >= '0') && (c <= '9')) {
      return (c - '0');
    } else if ((c >= 'a') && (c <= 'f')) {
      return (c - 'a') + 10;
    } else {
      return (c - 'A') + 10;
    }
  }

  /**
   * Unescape any C escape sequences (\n, \r, \\, \ooo, etc) and return the
   * resulting string.
   */
  public static String unescapeCString(String s) {
    if (s.indexOf('\\') < 0) {
      // Fast path: nothing to unescape
      return s;
    }

    StringBuilder sb = new StringBuilder();
    int len = s.length();
    for (int i = 0; i < len;) {
      char c = s.charAt(i++);
      if (c == '\\' && (i < len)) {
        c = s.charAt(i++);
        switch (c) {
          case 'a':  c = '\007';  break;
          case 'b':  c = '\b';    break;
          case 'f':  c = '\f';    break;
          case 'n':  c = '\n';    break;
          case 'r':  c = '\r';    break;
          case 't':  c = '\t';    break;
          case 'v':  c = '\013';  break;
          case '\\': c = '\\';    break;
          case '?':  c = '?';     break;
          case '\'': c = '\'';    break;
          case '"':  c = '\"';    break;

          default: {
            if ((c == 'x') && (i < len) && isHex(s.charAt(i))) {
              // "\xXX"
              int v = hexValue(s.charAt(i++));
              if ((i < len) && isHex(s.charAt(i))) {
                v = v * 16 + hexValue(s.charAt(i++));
              }
              c = (char) v;
            } else if (isOctal(c)) {
              // "\OOO"
              int v = (c - '0');
              if ((i < len) && isOctal(s.charAt(i))) {
                v = v * 8 + (s.charAt(i++) - '0');
              }
              if ((i < len) && isOctal(s.charAt(i))) {
                v = v * 8 + (s.charAt(i++) - '0');
              }
              c = (char) v;
            } else {
              // Propagate unknown escape sequences.
              sb.append('\\');
            }
            break;
          }
        }
      }
      sb.append(c);
    }
    return sb.toString();
  }

  /**
   * Unescape any MySQL escape sequences.
   * See MySQL language reference Chapter 6 at
   * <a href="http://www.mysql.com/doc/">http://www.mysql.com/doc/</a>.
   * This function will <strong>not</strong> work for other SQL-like
   * dialects.
   * @param s string to unescape, with the surrounding quotes.
   * @return unescaped string, without the surrounding quotes.
   * @exception IllegalArgumentException if s is not a valid MySQL string.
   */
  public static String unescapeMySQLString(String s)
      throws IllegalArgumentException {
    // note: the same buffer is used for both reading and writing
    // it works because the writer can never outrun the reader
    char chars[] = s.toCharArray();

    // the string must be quoted 'like this' or "like this"
    if (chars.length < 2 || chars[0] != chars[chars.length - 1] ||
        (chars[0] != '\'' && chars[0] != '"')) {
      throw new IllegalArgumentException("not a valid MySQL string: " + s);
    }

    // parse the string and decode the backslash sequences; in addition,
    // quotes can be escaped 'like this: ''', "like this: """, or 'like this: "'
    int j = 1;  // write position in the string (never exceeds read position)
    int f = 0;  // state: 0 (normal), 1 (backslash), 2 (quote)
    for (int i = 1; i < chars.length - 1; i++) {
      if (f == 0) {             // previous character was normal
        if (chars[i] == '\\') {
          f = 1;  // backslash
        } else if (chars[i] == chars[0]) {
          f = 2;  // quoting character
        } else {
          chars[j++] = chars[i];
        }
      } else if (f == 1) {      // previous character was a backslash
        switch (chars[i]) {
          case '0':   chars[j++] = '\0';   break;
          case '\'':  chars[j++] = '\'';   break;
          case '"':   chars[j++] = '"';    break;
          case 'b':   chars[j++] = '\b';   break;
          case 'n':   chars[j++] = '\n';   break;
          case 'r':   chars[j++] = '\r';   break;
          case 't':   chars[j++] = '\t';   break;
          case 'z':   chars[j++] = '\032'; break;
          case '\\':  chars[j++] = '\\';   break;
          default:
            // if the character is not special, backslash disappears
            chars[j++] = chars[i];
            break;
        }
        f = 0;
      } else {                  // previous character was a quote
        // quoting characters must be doubled inside a string
        if (chars[i] != chars[0]) {
          throw new IllegalArgumentException("not a valid MySQL string: " + s);
        }
        chars[j++] = chars[0];
        f = 0;
      }
    }
    // string contents cannot end with a special character
    if (f != 0) {
      throw new IllegalArgumentException("not a valid MySQL string: " + s);
    }

    // done
    return new String(chars, 1, j - 1);
  }

  // TODO(pbarry): move all HTML methods to common.html package

  static final Map<String, Character> ESCAPE_STRINGS;
  static final Set<Character> HEX_LETTERS;

  static {
    // HTML character entity references as defined in HTML 4
    // see http://www.w3.org/TR/REC-html40/sgml/entities.html
    ESCAPE_STRINGS = new HashMap<String, Character>(252);

    ESCAPE_STRINGS.put("&nbsp", '\u00A0');
    ESCAPE_STRINGS.put("&iexcl", '\u00A1');
    ESCAPE_STRINGS.put("&cent", '\u00A2');
    ESCAPE_STRINGS.put("&pound", '\u00A3');
    ESCAPE_STRINGS.put("&curren", '\u00A4');
    ESCAPE_STRINGS.put("&yen", '\u00A5');
    ESCAPE_STRINGS.put("&brvbar", '\u00A6');
    ESCAPE_STRINGS.put("&sect", '\u00A7');
    ESCAPE_STRINGS.put("&uml", '\u00A8');
    ESCAPE_STRINGS.put("&copy", '\u00A9');
    ESCAPE_STRINGS.put("&ordf", '\u00AA');
    ESCAPE_STRINGS.put("&laquo", '\u00AB');
    ESCAPE_STRINGS.put("&not", '\u00AC');
    ESCAPE_STRINGS.put("&shy", '\u00AD');
    ESCAPE_STRINGS.put("&reg", '\u00AE');
    ESCAPE_STRINGS.put("&macr", '\u00AF');
    ESCAPE_STRINGS.put("&deg", '\u00B0');
    ESCAPE_STRINGS.put("&plusmn", '\u00B1');
    ESCAPE_STRINGS.put("&sup2", '\u00B2');
    ESCAPE_STRINGS.put("&sup3", '\u00B3');
    ESCAPE_STRINGS.put("&acute", '\u00B4');
    ESCAPE_STRINGS.put("&micro", '\u00B5');
    ESCAPE_STRINGS.put("&para", '\u00B6');
    ESCAPE_STRINGS.put("&middot", '\u00B7');
    ESCAPE_STRINGS.put("&cedil", '\u00B8');
    ESCAPE_STRINGS.put("&sup1", '\u00B9');
    ESCAPE_STRINGS.put("&ordm", '\u00BA');
    ESCAPE_STRINGS.put("&raquo", '\u00BB');
    ESCAPE_STRINGS.put("&frac14", '\u00BC');
    ESCAPE_STRINGS.put("&frac12", '\u00BD');
    ESCAPE_STRINGS.put("&frac34", '\u00BE');
    ESCAPE_STRINGS.put("&iquest", '\u00BF');
    ESCAPE_STRINGS.put("&Agrave", '\u00C0');
    ESCAPE_STRINGS.put("&Aacute", '\u00C1');
    ESCAPE_STRINGS.put("&Acirc", '\u00C2');
    ESCAPE_STRINGS.put("&Atilde", '\u00C3');
    ESCAPE_STRINGS.put("&Auml", '\u00C4');
    ESCAPE_STRINGS.put("&Aring", '\u00C5');
    ESCAPE_STRINGS.put("&AElig", '\u00C6');
    ESCAPE_STRINGS.put("&Ccedil", '\u00C7');
    ESCAPE_STRINGS.put("&Egrave", '\u00C8');
    ESCAPE_STRINGS.put("&Eacute", '\u00C9');
    ESCAPE_STRINGS.put("&Ecirc", '\u00CA');
    ESCAPE_STRINGS.put("&Euml", '\u00CB');
    ESCAPE_STRINGS.put("&Igrave", '\u00CC');
    ESCAPE_STRINGS.put("&Iacute", '\u00CD');
    ESCAPE_STRINGS.put("&Icirc", '\u00CE');
    ESCAPE_STRINGS.put("&Iuml", '\u00CF');
    ESCAPE_STRINGS.put("&ETH", '\u00D0');
    ESCAPE_STRINGS.put("&Ntilde", '\u00D1');
    ESCAPE_STRINGS.put("&Ograve", '\u00D2');
    ESCAPE_STRINGS.put("&Oacute", '\u00D3');
    ESCAPE_STRINGS.put("&Ocirc", '\u00D4');
    ESCAPE_STRINGS.put("&Otilde", '\u00D5');
    ESCAPE_STRINGS.put("&Ouml", '\u00D6');
    ESCAPE_STRINGS.put("&times", '\u00D7');
    ESCAPE_STRINGS.put("&Oslash", '\u00D8');
    ESCAPE_STRINGS.put("&Ugrave", '\u00D9');
    ESCAPE_STRINGS.put("&Uacute", '\u00DA');
    ESCAPE_STRINGS.put("&Ucirc", '\u00DB');
    ESCAPE_STRINGS.put("&Uuml", '\u00DC');
    ESCAPE_STRINGS.put("&Yacute", '\u00DD');
    ESCAPE_STRINGS.put("&THORN", '\u00DE');
    ESCAPE_STRINGS.put("&szlig", '\u00DF');
    ESCAPE_STRINGS.put("&agrave", '\u00E0');
    ESCAPE_STRINGS.put("&aacute", '\u00E1');
    ESCAPE_STRINGS.put("&acirc", '\u00E2');
    ESCAPE_STRINGS.put("&atilde", '\u00E3');
    ESCAPE_STRINGS.put("&auml", '\u00E4');
    ESCAPE_STRINGS.put("&aring", '\u00E5');
    ESCAPE_STRINGS.put("&aelig", '\u00E6');
    ESCAPE_STRINGS.put("&ccedil", '\u00E7');
    ESCAPE_STRINGS.put("&egrave", '\u00E8');
    ESCAPE_STRINGS.put("&eacute", '\u00E9');
    ESCAPE_STRINGS.put("&ecirc", '\u00EA');
    ESCAPE_STRINGS.put("&euml", '\u00EB');
    ESCAPE_STRINGS.put("&igrave", '\u00EC');
    ESCAPE_STRINGS.put("&iacute", '\u00ED');
    ESCAPE_STRINGS.put("&icirc", '\u00EE');
    ESCAPE_STRINGS.put("&iuml", '\u00EF');
    ESCAPE_STRINGS.put("&eth", '\u00F0');
    ESCAPE_STRINGS.put("&ntilde", '\u00F1');
    ESCAPE_STRINGS.put("&ograve", '\u00F2');
    ESCAPE_STRINGS.put("&oacute", '\u00F3');
    ESCAPE_STRINGS.put("&ocirc", '\u00F4');
    ESCAPE_STRINGS.put("&otilde", '\u00F5');
    ESCAPE_STRINGS.put("&ouml", '\u00F6');
    ESCAPE_STRINGS.put("&divide", '\u00F7');
    ESCAPE_STRINGS.put("&oslash", '\u00F8');
    ESCAPE_STRINGS.put("&ugrave", '\u00F9');
    ESCAPE_STRINGS.put("&uacute", '\u00FA');
    ESCAPE_STRINGS.put("&ucirc", '\u00FB');
    ESCAPE_STRINGS.put("&uuml", '\u00FC');
    ESCAPE_STRINGS.put("&yacute", '\u00FD');
    ESCAPE_STRINGS.put("&thorn", '\u00FE');
    ESCAPE_STRINGS.put("&yuml", '\u00FF');
    ESCAPE_STRINGS.put("&fnof", '\u0192');
    ESCAPE_STRINGS.put("&Alpha", '\u0391');
    ESCAPE_STRINGS.put("&Beta", '\u0392');
    ESCAPE_STRINGS.put("&Gamma", '\u0393');
    ESCAPE_STRINGS.put("&Delta", '\u0394');
    ESCAPE_STRINGS.put("&Epsilon", '\u0395');
    ESCAPE_STRINGS.put("&Zeta", '\u0396');
    ESCAPE_STRINGS.put("&Eta", '\u0397');
    ESCAPE_STRINGS.put("&Theta", '\u0398');
    ESCAPE_STRINGS.put("&Iota", '\u0399');
    ESCAPE_STRINGS.put("&Kappa", '\u039A');
    ESCAPE_STRINGS.put("&Lambda", '\u039B');
    ESCAPE_STRINGS.put("&Mu", '\u039C');
    ESCAPE_STRINGS.put("&Nu", '\u039D');
    ESCAPE_STRINGS.put("&Xi", '\u039E');
    ESCAPE_STRINGS.put("&Omicron", '\u039F');
    ESCAPE_STRINGS.put("&Pi", '\u03A0');
    ESCAPE_STRINGS.put("&Rho", '\u03A1');
    ESCAPE_STRINGS.put("&Sigma", '\u03A3');
    ESCAPE_STRINGS.put("&Tau", '\u03A4');
    ESCAPE_STRINGS.put("&Upsilon", '\u03A5');
    ESCAPE_STRINGS.put("&Phi", '\u03A6');
    ESCAPE_STRINGS.put("&Chi", '\u03A7');
    ESCAPE_STRINGS.put("&Psi", '\u03A8');
    ESCAPE_STRINGS.put("&Omega", '\u03A9');
    ESCAPE_STRINGS.put("&alpha", '\u03B1');
    ESCAPE_STRINGS.put("&beta", '\u03B2');
    ESCAPE_STRINGS.put("&gamma", '\u03B3');
    ESCAPE_STRINGS.put("&delta", '\u03B4');
    ESCAPE_STRINGS.put("&epsilon", '\u03B5');
    ESCAPE_STRINGS.put("&zeta", '\u03B6');
    ESCAPE_STRINGS.put("&eta", '\u03B7');
    ESCAPE_STRINGS.put("&theta", '\u03B8');
    ESCAPE_STRINGS.put("&iota", '\u03B9');
    ESCAPE_STRINGS.put("&kappa", '\u03BA');
    ESCAPE_STRINGS.put("&lambda", '\u03BB');
    ESCAPE_STRINGS.put("&mu", '\u03BC');
    ESCAPE_STRINGS.put("&nu", '\u03BD');
    ESCAPE_STRINGS.put("&xi", '\u03BE');
    ESCAPE_STRINGS.put("&omicron", '\u03BF');
    ESCAPE_STRINGS.put("&pi", '\u03C0');
    ESCAPE_STRINGS.put("&rho", '\u03C1');
    ESCAPE_STRINGS.put("&sigmaf", '\u03C2');
    ESCAPE_STRINGS.put("&sigma", '\u03C3');
    ESCAPE_STRINGS.put("&tau", '\u03C4');
    ESCAPE_STRINGS.put("&upsilon", '\u03C5');
    ESCAPE_STRINGS.put("&phi", '\u03C6');
    ESCAPE_STRINGS.put("&chi", '\u03C7');
    ESCAPE_STRINGS.put("&psi", '\u03C8');
    ESCAPE_STRINGS.put("&omega", '\u03C9');
    ESCAPE_STRINGS.put("&thetasym", '\u03D1');
    ESCAPE_STRINGS.put("&upsih", '\u03D2');
    ESCAPE_STRINGS.put("&piv", '\u03D6');
    ESCAPE_STRINGS.put("&bull", '\u2022');
    ESCAPE_STRINGS.put("&hellip", '\u2026');
    ESCAPE_STRINGS.put("&prime", '\u2032');
    ESCAPE_STRINGS.put("&Prime", '\u2033');
    ESCAPE_STRINGS.put("&oline", '\u203E');
    ESCAPE_STRINGS.put("&frasl", '\u2044');
    ESCAPE_STRINGS.put("&weierp", '\u2118');
    ESCAPE_STRINGS.put("&image", '\u2111');
    ESCAPE_STRINGS.put("&real", '\u211C');
    ESCAPE_STRINGS.put("&trade", '\u2122');
    ESCAPE_STRINGS.put("&alefsym", '\u2135');
    ESCAPE_STRINGS.put("&larr", '\u2190');
    ESCAPE_STRINGS.put("&uarr", '\u2191');
    ESCAPE_STRINGS.put("&rarr", '\u2192');
    ESCAPE_STRINGS.put("&darr", '\u2193');
    ESCAPE_STRINGS.put("&harr", '\u2194');
    ESCAPE_STRINGS.put("&crarr", '\u21B5');
    ESCAPE_STRINGS.put("&lArr", '\u21D0');
    ESCAPE_STRINGS.put("&uArr", '\u21D1');
    ESCAPE_STRINGS.put("&rArr", '\u21D2');
    ESCAPE_STRINGS.put("&dArr", '\u21D3');
    ESCAPE_STRINGS.put("&hArr", '\u21D4');
    ESCAPE_STRINGS.put("&forall", '\u2200');
    ESCAPE_STRINGS.put("&part", '\u2202');
    ESCAPE_STRINGS.put("&exist", '\u2203');
    ESCAPE_STRINGS.put("&empty", '\u2205');
    ESCAPE_STRINGS.put("&nabla", '\u2207');
    ESCAPE_STRINGS.put("&isin", '\u2208');
    ESCAPE_STRINGS.put("&notin", '\u2209');
    ESCAPE_STRINGS.put("&ni", '\u220B');
    ESCAPE_STRINGS.put("&prod", '\u220F');
    ESCAPE_STRINGS.put("&sum", '\u2211');
    ESCAPE_STRINGS.put("&minus", '\u2212');
    ESCAPE_STRINGS.put("&lowast", '\u2217');
    ESCAPE_STRINGS.put("&radic", '\u221A');
    ESCAPE_STRINGS.put("&prop", '\u221D');
    ESCAPE_STRINGS.put("&infin", '\u221E');
    ESCAPE_STRINGS.put("&ang", '\u2220');
    ESCAPE_STRINGS.put("&and", '\u2227');
    ESCAPE_STRINGS.put("&or", '\u2228');
    ESCAPE_STRINGS.put("&cap", '\u2229');
    ESCAPE_STRINGS.put("&cup", '\u222A');
    ESCAPE_STRINGS.put("&int", '\u222B');
    ESCAPE_STRINGS.put("&there4", '\u2234');
    ESCAPE_STRINGS.put("&sim", '\u223C');
    ESCAPE_STRINGS.put("&cong", '\u2245');
    ESCAPE_STRINGS.put("&asymp", '\u2248');
    ESCAPE_STRINGS.put("&ne", '\u2260');
    ESCAPE_STRINGS.put("&equiv", '\u2261');
    ESCAPE_STRINGS.put("&le", '\u2264');
    ESCAPE_STRINGS.put("&ge", '\u2265');
    ESCAPE_STRINGS.put("&sub", '\u2282');
    ESCAPE_STRINGS.put("&sup", '\u2283');
    ESCAPE_STRINGS.put("&nsub", '\u2284');
    ESCAPE_STRINGS.put("&sube", '\u2286');
    ESCAPE_STRINGS.put("&supe", '\u2287');
    ESCAPE_STRINGS.put("&oplus", '\u2295');
    ESCAPE_STRINGS.put("&otimes", '\u2297');
    ESCAPE_STRINGS.put("&perp", '\u22A5');
    ESCAPE_STRINGS.put("&sdot", '\u22C5');
    ESCAPE_STRINGS.put("&lceil", '\u2308');
    ESCAPE_STRINGS.put("&rceil", '\u2309');
    ESCAPE_STRINGS.put("&lfloor", '\u230A');
    ESCAPE_STRINGS.put("&rfloor", '\u230B');
    ESCAPE_STRINGS.put("&lang", '\u2329');
    ESCAPE_STRINGS.put("&rang", '\u232A');
    ESCAPE_STRINGS.put("&loz", '\u25CA');
    ESCAPE_STRINGS.put("&spades", '\u2660');
    ESCAPE_STRINGS.put("&clubs", '\u2663');
    ESCAPE_STRINGS.put("&hearts", '\u2665');
    ESCAPE_STRINGS.put("&diams", '\u2666');
    ESCAPE_STRINGS.put("&quot", '\u0022');
    ESCAPE_STRINGS.put("&amp", '\u0026');
    ESCAPE_STRINGS.put("&lt", '\u003C');
    ESCAPE_STRINGS.put("&gt", '\u003E');
    ESCAPE_STRINGS.put("&OElig", '\u0152');
    ESCAPE_STRINGS.put("&oelig", '\u0153');
    ESCAPE_STRINGS.put("&Scaron", '\u0160');
    ESCAPE_STRINGS.put("&scaron", '\u0161');
    ESCAPE_STRINGS.put("&Yuml", '\u0178');
    ESCAPE_STRINGS.put("&circ", '\u02C6');
    ESCAPE_STRINGS.put("&tilde", '\u02DC');
    ESCAPE_STRINGS.put("&ensp", '\u2002');
    ESCAPE_STRINGS.put("&emsp", '\u2003');
    ESCAPE_STRINGS.put("&thinsp", '\u2009');
    ESCAPE_STRINGS.put("&zwnj", '\u200C');
    ESCAPE_STRINGS.put("&zwj", '\u200D');
    ESCAPE_STRINGS.put("&lrm", '\u200E');
    ESCAPE_STRINGS.put("&rlm", '\u200F');
    ESCAPE_STRINGS.put("&ndash", '\u2013');
    ESCAPE_STRINGS.put("&mdash", '\u2014');
    ESCAPE_STRINGS.put("&lsquo", '\u2018');
    ESCAPE_STRINGS.put("&rsquo", '\u2019');
    ESCAPE_STRINGS.put("&sbquo", '\u201A');
    ESCAPE_STRINGS.put("&ldquo", '\u201C');
    ESCAPE_STRINGS.put("&rdquo", '\u201D');
    ESCAPE_STRINGS.put("&bdquo", '\u201E');
    ESCAPE_STRINGS.put("&dagger", '\u2020');
    ESCAPE_STRINGS.put("&Dagger", '\u2021');
    ESCAPE_STRINGS.put("&permil", '\u2030');
    ESCAPE_STRINGS.put("&lsaquo", '\u2039');
    ESCAPE_STRINGS.put("&rsaquo", '\u203A');
    ESCAPE_STRINGS.put("&euro", '\u20AC');

    HEX_LETTERS = new HashSet<Character>(12);

    HEX_LETTERS.add('a');
    HEX_LETTERS.add('A');
    HEX_LETTERS.add('b');
    HEX_LETTERS.add('B');
    HEX_LETTERS.add('c');
    HEX_LETTERS.add('C');
    HEX_LETTERS.add('d');
    HEX_LETTERS.add('D');
    HEX_LETTERS.add('e');
    HEX_LETTERS.add('E');
    HEX_LETTERS.add('f');
    HEX_LETTERS.add('F');
  }

  /**
   * <p>
   * Replace all the occurences of HTML escape strings with the
   * respective characters.
   * </p>
   * <p>
   * The default mode is strict (requiring semicolons).
   * </p>
   *
   * @param s a <code>String</code> value
   * @return a <code>String</code> value
   * @throws NullPointerException if the input string is null.
   */
  public static final String unescapeHTML(String s) {
    return unescapeHTML(s, false);
  }

  /**
   * Replace all the occurences of HTML escape strings with the
   * respective characters.
   *
   * @param s a <code>String</code> value
   * @param emulateBrowsers a <code>Boolean</code> value that tells the method
   *     to allow entity refs not terminated with a semicolon to be unescaped.
   *     (a quirk of this feature, and some browsers, is that an explicit
   *     terminating character is needed - e.g., &lt$ would be unescaped, but
   *     not &ltab - see the tests for a more in-depth description of browsers)
   * @return a <code>String</code> value
   * @throws NullPointerException if the input string is null.
   */
  public static final String unescapeHTML(String s, boolean emulateBrowsers) {

    // See if there are any '&' in the string since that is what we look
    // for to escape. If there isn't, then we don't need to escape this string
    // Based on similar technique used in the escape function.
    int index = s.indexOf('&');
    if (index == -1) {
      // Nothing to escape. Return the original string.
      return s;
    }

    // We found an escaped character. Start slow escaping from there.
    char[] chars = s.toCharArray();
    char[] escaped = new char[chars.length];
    System.arraycopy(chars, 0, escaped, 0, index);

    // Note: escaped[pos] = end of the escaped char array.
    int pos = index;

    for (int i = index; i < chars.length;) {
      if (chars[i] != '&') {
        escaped[pos++] = chars[i++];
        continue;
      }

      // Allow e.g. &#123;
      int j = i + 1;
      boolean isNumericEntity = false;
      if (j < chars.length && chars[j] == '#') {
        j++;
        isNumericEntity = true;
      }

      // if it's numeric, also check for hex
      boolean isHexEntity = false;
      if (j < chars.length && (chars[j] == 'x' || chars[j] == 'X')) {
        j++;
        isHexEntity = true;
      }

      // Scan until we find a char that is not valid for this sequence.
      for (; j < chars.length; j++) {
        char ch = chars[j];
        boolean isDigit = Character.isDigit(ch);
        if (isNumericEntity) {
          // non-hex numeric sequence end condition
          if (!isHexEntity && !isDigit) {
            break;
          }
          // hex sequence end contition
          if (isHexEntity && !isDigit && !HEX_LETTERS.contains(ch)) {
            break;
          }
        }
        // anything other than a digit or letter is always an end condition
        if (!isDigit && !Character.isLetter(ch)) {
          break;
        }
      }

      boolean replaced = false;
      if ((j <= chars.length && emulateBrowsers) ||
          (j < chars.length && chars[j] == ';')) {
        // Check for &#D; and &#xD; pattern
        if (i + 2 < chars.length && s.charAt(i + 1) == '#') {
          try {
            long charcode = 0;
            char ch = s.charAt(i + 2);
            if (isHexEntity) {
              charcode = Long.parseLong(
                  new String(chars, i + 3, j - i - 3), 16);
            } else if (Character.isDigit(ch)) {
              charcode = Long.parseLong(
                  new String(chars, i + 2, j - i - 2));
            }
            if (charcode > 0 && charcode < 65536) {
              escaped[pos++] = (char) charcode;
              replaced = true;
            }
          } catch (NumberFormatException ex) {
            // Failed, not replaced.
          }
        } else {
          String key = new String(chars, i, j - i);
          Character repl = ESCAPE_STRINGS.get(key);
          if (repl != null) {
            escaped[pos++] = repl;
            replaced = true;
          }
        }
        // Skip over ';'
        if (j < chars.length && chars[j] == ';') {
          j++;
        }
      }

      if (!replaced) {
        // Not a recognized escape sequence, leave as-is
        System.arraycopy(chars, i, escaped, pos, j - i);
        pos += j - i;
      }
      i = j;
    }
    return new String(escaped, 0, pos);
  }

  // Escaper for < and > only.
  private static final CharEscaper LT_GT_ESCAPE =
      new CharEscaperBuilder()
        .addEscape('<', "&lt;")
        .addEscape('>', "&gt;")
        .toEscaper();

  private static final Pattern htmlTagPattern =
      Pattern.compile("</?[a-zA-Z][^>]*>");

  /**
   * Given a <code>String</code>, returns an equivalent <code>String</code> with
   * all HTML tags stripped. Note that HTML entities, such as "&amp;amp;" will
   * still be preserved.
   */
  public static String stripHtmlTags(String string) {
    if ((string == null) || "".equals(string)) {
      return string;
    }
    String stripped = htmlTagPattern.matcher(string).replaceAll("");
    /*
     * Certain inputs result in a well-formed HTML:
     * <<X>script>alert(0)<</X>/script> results in <script>alert(0)</script>
     * The following step ensures that no HTML can slip through by replacing all
     * < and > characters with &lt; and &gt; after HTML tags were stripped.
     */
    return LT_GT_ESCAPE.escape(stripped);
  }

  /**
   * We escape some characters in s to be able to insert strings into JavaScript
   * code. Also, make sure that we don't write out {@code -->} or
   * {@code </script>}, which may close a script tag, or any char in ["'>] which
   * might close a tag or attribute if seen inside an attribute.
   */
  public static String javaScriptEscape(CharSequence s) {
    return javaScriptEscapeHelper(s, false);
  }

  /**
   * We escape some characters in s to be able to insert strings into JavaScript
   * code. Also, make sure that we don't write out {@code -->} or
   * {@code </script>}, which may close a script tag, or any char in ["'>] which
   * might close a tag or attribute if seen inside an attribute.
   * Turns all non-ascii characters into ASCII javascript escape sequences
   * (eg \\uhhhh or \ooo).
   */
  public static String javaScriptEscapeToAscii(CharSequence s) {
    return javaScriptEscapeHelper(s, true);
  }

  /**
   * Represents the type of javascript escaping to perform.  Each enum below
   * determines whether to use octal escapes and how to handle quotes.
   */
  public static enum JsEscapingMode {
    /** No octal escapes, pass-through ', and escape " as \". */
    JSON,

    /** Octal escapes, escapes ' and " to \42 and \47, respectively. */
    EMBEDDABLE_JS,

    /** Octal escapes, escapes ' and " to \' and \". */
    MINIMAL_JS
  }

  /**
   * Helper for javaScriptEscape and javaScriptEscapeToAscii
   */
  private static String javaScriptEscapeHelper(CharSequence s,
                                               boolean escapeToAscii) {
    StringBuilder sb = new StringBuilder(s.length() * 9 / 8);
    try {
      escapeStringBody(s, escapeToAscii, JsEscapingMode.EMBEDDABLE_JS, sb);
    } catch (IOException ex) {
      // StringBuilder.append does not throw IOExceptions.
      throw new RuntimeException(ex);
    }
    return sb.toString();
  }

  /**
   * Appends the javascript string literal equivalent of plainText to the given
   * out buffer.
   * @param plainText the string to escape.
   * @param escapeToAscii true to encode all characters not in ascii [\x20-\x7e]
   *   <br>
   *   Full escaping of unicode entites isn't required but this makes
   *   sure that unicode strings will survive regardless of the
   *   content-encoding of the javascript file which is important when
   *   we use this function to autogenerated javascript source files.
   *   This is disabled by default because it makes non-latin strings very long.
   *   <br>
   *   If you seem to have trouble with character-encodings, maybe
   *   turn this on to see if the problem goes away.  If so, you need
   *   to specify a character encoding for your javascript somewhere.
   * @param jsEscapingMode determines the type of escaping to perform.
   * @param out the buffer to append output to.
   */
  /*
   * To avoid fallthrough, we would have to either use a hybrid switch-case/if
   * approach (which would obscure our special handling for ' and "), duplicate
   * the content of the default case, or pass a half-dozen parameters to a
   * helper method containing the code from the default case.
   */
  @SuppressWarnings("fallthrough")
  public static void escapeStringBody(
      CharSequence plainText, boolean escapeToAscii,
      JsEscapingMode jsEscapingMode, Appendable out)
      throws IOException {
    int pos = 0;  // Index just past the last char in plainText written to out.
    int len = plainText.length();
    for (int codePoint, charCount, i = 0; i < len; i += charCount) {
      codePoint = Character.codePointAt(plainText, i);
      charCount = Character.charCount(codePoint);

      if (!shouldEscapeChar(codePoint, escapeToAscii, jsEscapingMode)) {
        continue;
      }

      out.append(plainText, pos, i);
      pos = i + charCount;
      switch (codePoint) {
        case '\b': out.append("\\b"); break;
        case '\t': out.append("\\t"); break;
        case '\n': out.append("\\n"); break;
        case '\f': out.append("\\f"); break;
        case '\r': out.append("\\r"); break;
        case '\\': out.append("\\\\"); break;
        case '"': case '\'':
          if (jsEscapingMode == JsEscapingMode.JSON && '\'' == codePoint) {
            // JSON does not escape a single quote (and it should be surrounded
            // by double quotes).
            out.append((char) codePoint);
            break;
          } else if (jsEscapingMode != JsEscapingMode.EMBEDDABLE_JS) {
            out.append('\\').append((char) codePoint);
            break;
          }
          // fall through
        default:
          if (codePoint >= 0x100 || jsEscapingMode == JsEscapingMode.JSON) {
            appendHexJavaScriptRepresentation(codePoint, out);
          } else {
            // Output the minimal octal encoding.  We can't use an encoding
            // shorter than three digits if the next digit is a valid octal
            // digit.
            boolean pad = i + charCount >= len
                || isOctal(plainText.charAt(i + charCount));
            appendOctalJavaScriptRepresentation((char) codePoint, pad, out);
          }
          break;
      }
    }
    out.append(plainText, pos, len);
  }

  /**
   * Helper for escapeStringBody, which decides whether to escape a character.
   */
  private static boolean shouldEscapeChar(int codePoint,
      boolean escapeToAscii, JsEscapingMode jsEscapingMode) {
    // If non-ASCII chars should be escaped, identify non-ASCII code points.
    if (escapeToAscii && (codePoint < 0x20 || codePoint > 0x7e)) {
      return true;
    }

    // If in JSON escaping mode, check JSON *and* JS escaping rules. The JS
    // escaping rules will escape more characters than needed for JSON,
    // but it is safe to escape any character in JSON.
    // TODO(bbavar): Remove unnecessary escaping for JSON, as long as it can be
    //               shown that this change in legacy behavior is safe.
    if (jsEscapingMode == JsEscapingMode.JSON) {
      return mustEscapeCharInJsonString(codePoint)
          || mustEscapeCharInJsString(codePoint);
    }

    // Finally, just check the default JS escaping rules.
    return mustEscapeCharInJsString(codePoint);
  }

  /**
   * Returns a javascript representation of the character in a hex escaped
   * format.
   *
   * @param codePoint The codepoint to append.
   * @param out The buffer to which the hex representation should be appended.
   */
  private static void appendHexJavaScriptRepresentation(
      int codePoint, Appendable out)
      throws IOException {
    if (Character.isSupplementaryCodePoint(codePoint)) {
      // Handle supplementary unicode values which are not representable in
      // javascript.  We deal with these by escaping them as two 4B sequences
      // so that they will round-trip properly when sent from java to javascript
      // and back.
      char[] surrogates = Character.toChars(codePoint);
      appendHexJavaScriptRepresentation(surrogates[0], out);
      appendHexJavaScriptRepresentation(surrogates[1], out);
      return;
    }
    out.append("\\u")
        .append(HEX_CHARS[(codePoint >>> 12) & 0xf])
        .append(HEX_CHARS[(codePoint >>> 8) & 0xf])
        .append(HEX_CHARS[(codePoint >>> 4) & 0xf])
        .append(HEX_CHARS[codePoint & 0xf]);
  }

  /**
   * Returns a javascript representation of the character in a hex escaped
   * format. Although this is a rather specific method, it is made public
   * because it is also used by the JSCompiler.
   *
   * @param ch The character to append.
   * @param pad true to force use of the full 3 digit representation.
   * @param out The buffer to which the hex representation should be appended.
   */
  private static void appendOctalJavaScriptRepresentation(
      char ch, boolean pad, Appendable out) throws IOException {
    if (ch >= 0100
        // Be paranoid at the end of a string since someone might call
        // this method again with another string segment.
        || pad) {
      out.append('\\')
          .append(OCTAL_CHARS[(ch >>> 6) & 0x7])
          .append(OCTAL_CHARS[(ch >>> 3) & 0x7])
          .append(OCTAL_CHARS[ch & 0x7]);
    } else if (ch >= 010) {
      out.append('\\')
          .append(OCTAL_CHARS[(ch >>> 3) & 0x7])
          .append(OCTAL_CHARS[ch & 0x7]);
    } else {
      out.append('\\')
          .append(OCTAL_CHARS[ch & 0x7]);
    }
  }

  /**
   * Although this is a rather specific method, it is made public
   * because it is also used by the JSCompiler.
   *
   * @see #appendHexJavaScriptRepresentation(int, Appendable)
   */
  public static void appendHexJavaScriptRepresentation(StringBuilder sb,
                                                       char c) {
    try {
      appendHexJavaScriptRepresentation(c, sb);
    } catch (IOException ex) {
      // StringBuilder does not throw IOException.
      throw new RuntimeException(ex);
    }
  }

  /**
   * Undo escaping as performed in javaScriptEscape(.)
   * Throws an IllegalArgumentException if the string contains
   * bad escaping.
   */
  public static String javaScriptUnescape(String s) {
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); ) {
      char c = s.charAt(i);
      if (c == '\\') {
        i = javaScriptUnescapeHelper(s, i + 1, sb);
      } else {
        sb.append(c);
        i++;
      }
    }
    return sb.toString();
  }

  /**
   * Looks for an escape code starting at index i of s,
   * and appends it to sb.
   * @return the index of the first character in s
   * after the escape code.
   * @throws IllegalArgumentException if the escape code
   * is invalid
   */
  private static int javaScriptUnescapeHelper(String s, int i,
                                              StringBuilder sb) {
    if (i >= s.length()) {
      throw new IllegalArgumentException(
          "End-of-string after escape character in [" + s + "]");
    }

    char c = s.charAt(i++);
    switch (c) {
      case 'n': sb.append('\n'); break;
      case 'r': sb.append('\r'); break;
      case 't': sb.append('\t'); break;
      case 'b': sb.append('\b'); break;
      case 'f': sb.append('\f'); break;
      case '\\':
      case '\"':
      case '\'':
      case '>':
        sb.append(c);
        break;
      case '0': case '1': case '2': case '3':
      case '4': case '5': case '6': case '7':
        --i;  // backup to first octal digit
        int nOctalDigits = 1;
        int digitLimit = c < '4' ? 3 : 2;
        while (nOctalDigits < digitLimit && i + nOctalDigits < s.length()
               && isOctal(s.charAt(i + nOctalDigits))) {
          ++nOctalDigits;
        }
        sb.append(
            (char) Integer.parseInt(s.substring(i, i + nOctalDigits), 8));
        i += nOctalDigits;
        break;
      case 'x':
      case 'u':
        String hexCode;
        int nHexDigits = (c == 'u' ? 4 : 2);
        try {
          hexCode = s.substring(i, i + nHexDigits);
        } catch (IndexOutOfBoundsException ioobe) {
          throw new IllegalArgumentException(
              "Invalid unicode sequence [" + s.substring(i) + "] at index " + i
              + " in [" + s + "]");
        }
        int unicodeValue;
        try {
          unicodeValue = Integer.parseInt(hexCode, 16);
        } catch (NumberFormatException nfe) {
          throw new IllegalArgumentException(
              "Invalid unicode sequence [" + hexCode + "] at index " + i +
              " in [" + s + "]");
        }
        sb.append((char) unicodeValue);
        i += nHexDigits;
        break;
      default:
        throw new IllegalArgumentException(
            "Unknown escape code [" + c + "] at index " + i + " in [" + s + "]"
            );
    }

    return i;
  }

  // C0 control characters except \t, \n, and \r and 0xFFFE and 0xFFFF
  private static final CharMatcher CONTROL_MATCHER = CharMatcher.anyOf(
      "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007" +
      "\u0008\u000B\u000C\u000E\u000F" +
      "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017" +
      "\u0018\u0019\u001A\u001B\u001C\u001D\u001E\u001F" +
      "\uFFFE\uFFFF");

  /**
   * Escape a string that is meant to be embedded in a CDATA section.
   * The returned string is guaranteed to be valid CDATA content.
   * The syntax of CDATA sections is the following:
   * <blockquote>
   *   <code>&lt;[!CDATA[...]]&gt;</code>
   * </blockquote>
   * The only invalid character sequence in a CDATA tag is "]]&gt;".
   * If this sequence is present in the input string, we replace
   * it by closing the current CDATA field, then write ']]&amp;gt;',
   * then reopen a new CDATA section.
   */
  public static String xmlCDataEscape(String s) {
     // Make sure there are no illegal control characters.
     s = CONTROL_MATCHER.removeFrom(s);
    // Return the original reference if the string doesn't have a match.
    int found = s.indexOf("]]>");
    if (found == -1) {
      return s;
    }

    // For each occurrence of "]]>", append a string that adds "]]&gt;" after
    // the end of the CDATA which has just been closed, then opens a new CDATA.
    StringBuilder sb = new StringBuilder();
    int prev = 0;
    do {
      sb.append(s.substring(prev, found + 3));
      sb.append("]]&gt;<![CDATA[");
      prev = found + 3;
    } while ((found = s.indexOf("]]>", prev)) != -1);
    sb.append(s.substring(prev));
    return sb.toString();
  }

  /**
   * We escape some characters in s to be able to insert strings into Java code
   *
   * @deprecated Use {@link CharEscapers#asciiHtmlEscaper()} and {@link
   * CharEscapers#javaCharEscaper()} or {@link CharEscapers#javaStringEscaper()}
   * instead. This method combines two forms of escaping in a way that's rarely
   * desired.
   */
  @Deprecated
  public static String javaEscape(String s) {
    return JAVA_ESCAPE.escape(s);
  }

  // Java escaper.
  private static final CharEscaper JAVA_ESCAPE =
      new CharEscaperBuilder()
        .addEscape('\n', "\\n")
        .addEscape('\r', "\\r")
        .addEscape('\t', "\\t")
        .addEscape('\\', "\\\\")
        .addEscape('\"', "\\\"")
        .addEscape('&', "&amp;")
        .addEscape('<', "&lt;")
        .addEscape('>', "&gt;")
        .addEscape('\'', "\\\'")
        .toEscaper();

  /**
   * Escapes the special characters from a string so it can be used as part of
   * a regex pattern. This method is for use on gnu.regexp style regular
   * expressions.
   *
   * @deprecated Use {@link Pattern#quote(String)} instead. Note that it may not
   * be compatible with gnu.regexp style regular expressions.
   */
  @Deprecated
  public static String regexEscape(String s) {
    return REGEX_ESCAPE.escape(s);
  }

  // Regex escaper escapes all regex characters.
  private static final CharEscaper REGEX_ESCAPE =
      new CharEscaperBuilder()
        .addEscape('(', "\\(")
        .addEscape(')', "\\)")
        .addEscape('|', "\\|")
        .addEscape('*', "\\*")
        .addEscape('+', "\\+")
        .addEscape('?', "\\?")
        .addEscape('.', "\\.")
        .addEscape('{', "\\{")
        .addEscape('}', "\\}")
        .addEscape('[', "\\[")
        .addEscape(']', "\\]")
        .addEscape('$', "\\$")
        .addEscape('^', "\\^")
        .addEscape('\\', "\\\\")
        .toEscaper();

  /**
   *  If you want to preserve the exact
   * current (odd) behavior when {@code doStrip} is {@code true}, use
   * {@code .trimResults(CharMatcher.LEGACY_WHITESPACE).omitEmptyStrings()} on
   * the splitter.
   *
   * @param in what to process
   * @param delimiter the delimiting string
   * @return the tokens
   * @deprecated see the detailed instructions under
   *     {@link #split(String, String, boolean)}
   */
  @Deprecated
  public static LinkedList<String> string2List(
      String in, String delimiter, boolean doStrip) {
    if (in == null) {
      return null;
    }

    LinkedList<String> out = new LinkedList<String>();
    string2Collection(in, delimiter, doStrip, out);
    return out;
  }

  /**
   * See the detailed instructions under {@link
   * #split(String, String, boolean)}. Pass the resulting {@code Iterable} to
   * {@link com.google.common.collect.Sets#newHashSet(Iterable)}. If you want to
   * preserve the exact current (odd) behavior when {@code doStrip} is {@code
   * true}, use {@code
   * .trimResults(CharMatcher.LEGACY_WHITESPACE).omitEmptyStrings()} on the
   * splitter.
   *
   * @param in what to process
   * @param delimiter the delimiting string
   * @param doStrip to strip the substrings before adding to the list
   * @return the tokens
   * @deprecated see the detailed instructions under
   *     {@link #split(String, String, boolean)}
   */
  @Deprecated
  public static Set<String> string2Set(
       String in, String delimiter, boolean doStrip) {
    if (in == null) {
      return null;
    }

    HashSet<String> out = new HashSet<String>();
    string2Collection(in, delimiter, doStrip, out);
    return out;
  }

  /**
   * See the detailed instructions under {@link
   * #split(String, String, boolean)}. If you want to preserve the exact current
   * (odd) behavior when {@code doStrip} is {@code true}, use {@code
   * .trimResults(CharMatcher.LEGACY_WHITESPACE).omitEmptyStrings()} on the
   * splitter.
   *
   * @param in The delimited input string to process
   * @param delimiter The string delimiting entries in the input string.
   * @param doStrip whether to strip the substrings before adding to the
   *          collection
   * @param collection The collection to which the strings will be added. If
   *          <code>null</code>, a new <code>List</code> will be created.
   * @return The collection to which the substrings were added. This is
   *         syntactic sugar to allow call chaining.
   * @deprecated see the detailed instructions under
   *     {@link #split(String, String, boolean)}
   */
  @Deprecated
  public static Collection<String> string2Collection(
      String in,
      String delimiter,
      boolean doStrip,
      Collection<String> collection) {
    if (in == null) {
      return null;
    }
    if (collection == null) {
      collection = new ArrayList<String>();
    }
    if (delimiter == null || delimiter.length() == 0) {
      collection.add(in);
      return collection;
    }

    int fromIndex = 0;
    int pos;
    while ((pos = in.indexOf(delimiter, fromIndex)) >= 0) {
      String interim = in.substring(fromIndex, pos);
      if (doStrip) {
        interim = strip(interim);
      }
      if (!doStrip || interim.length() > 0) {
        collection.add(interim);
      }

      fromIndex = pos + delimiter.length();
    }

    String interim = in.substring(fromIndex);
    if (doStrip) {
      interim = strip(interim);
    }
    if (!doStrip || interim.length() > 0) {
      collection.add(interim);
    }

    return collection;
  }

  /**
   * This converts a string to a Map. It will first split the string into
   * entries using delimEntry. Then each entry is split into a key and a value
   * using delimKey. By default we strip the keys. Use doStripEntry to strip
   * also the entries.
   *
   * Note that this method returns a {@link HashMap}, which means that entries
   * will be in no particular order. See {@link #stringToOrderedMap}.
   *
   * @param in the string to be processed
   * @param delimEntry delimiter for the entries
   * @param delimKey delimiter between keys and values
   * @param doStripEntry strip entries before inserting in the map
   *
   * @return HashMap
   */
  public static HashMap<String, String> string2Map(
      String in, String delimEntry, String delimKey,
      boolean doStripEntry) {
    if (in == null) {
      return null;
    }

    return stringToMapImpl(new HashMap<String, String>(), in, delimEntry,
        delimKey, doStripEntry);
  }

  /**
   * This converts a string to a Map, with entries in the same order as the
   * key/value pairs in the input string. It will first split the string into
   * entries using delimEntry. Then each entry is split into a key and a value
   * using delimKey. By default we strip the keys. Use doStripEntry to strip
   * also the entries.
   *
   * @param in the string to be processed
   * @param delimEntry delimiter for the entries
   * @param delimKey delimiter between keys and values
   * @param doStripEntry strip entries before inserting in the map
   *
   * @return key/value pairs as a Map, in order
   */
  public static Map<String, String> stringToOrderedMap(
      String in, String delimEntry, String delimKey,
      boolean doStripEntry) {
    if (in == null) {
      return null;
    }

    return stringToMapImpl(new LinkedHashMap<String, String>(), in, delimEntry,
        delimKey, doStripEntry);
  }

  /**
   * This adds key/value pairs from the given string to the given Map.
   * It will first split the string into entries using delimEntry. Then each
   * entry is split into a key and a value using delimKey. By default we
   * strip the keys. Use doStripEntry to strip also the entries.
   *
   * @param out - Map to output into
   * @param in - the string to be processed
   * @param delimEntry - delimiter for the entries
   * @param delimKey - delimiter between keys and values
   * @param doStripEntry - strip entries before inserting in the map
   * @return out, for caller's convenience
   */
  private static <T extends Map<String, String>> T stringToMapImpl(T out,
      String in, String delimEntry, String delimKey, boolean doStripEntry) {

    if (isEmpty(delimEntry) || isEmpty(delimKey)) {
      out.put(strip(in), "");
      return out;
    }

    Iterator<String> it = string2List(in, delimEntry, false).iterator();
    int len = delimKey.length();
    while (it.hasNext()) {
      String entry = it.next();
      int pos = entry.indexOf(delimKey);
      if (pos > 0) {
        String value = entry.substring(pos + len);
        if (doStripEntry) {
          value = strip(value);
        }
        out.put(strip(entry.substring(0, pos)), value);
      } else {
        out.put(strip(entry), "");
      }
    }

    return out;
  }

  /**
   * This function concatenates the elements of a Map in a string with form
   *  "<key1><sepKey><value1><sepEntry>...<keyN><sepKey><valueN>"
   *
   * @param in - the map to be converted
   * @param sepKey - the separator to put between key and value
   * @param sepEntry - the separator to put between map entries
   * @return String
   * @deprecated create a {@link MapJoiner}, for example {@code
   *     Joiner.on(sepEntry).withKeyValueSeparator(sepKey)}. Ensure that your
   *     map is non-null and use this map joiner's {@link MapJoiner#join(Map)}
   *     method. To preserve behavior exactly, just in-line this method call.
   */
  @Deprecated public static <K, V> String map2String(
      Map<K, V> in, String sepKey, String sepEntry) {
    return (in == null) ? null : Joiner
        .on(sepEntry)
        .useForNull("null")
        .withKeyValueSeparator(sepKey)
        .join(in);
  }

  /**
   * Given a map, creates and returns a new map in which all keys are the
   * lower-cased version of each key.
   *
   * @param map A map containing String keys to be lowercased
   * @throws IllegalArgumentException if the map contains duplicate string keys
   *           after lower casing
   */
  public static <V> Map<String, V> lowercaseKeys(Map<String, V> map) {
    Map<String, V> result = new HashMap<String, V>(map.size());
    for (Map.Entry<String, V> entry : map.entrySet()) {
      String key = entry.getKey();
      if (result.containsKey(key.toLowerCase())) {
        throw new IllegalArgumentException(
            "Duplicate string key in map when lower casing");
      }
      result.put(key.toLowerCase(), entry.getValue());
    }
    return result;
  }

  /**
   * Replaces any string of adjacent whitespace characters with the whitespace
   * character " ".
   *
   * @param str the string you want to munge
   * @return String with no more excessive whitespace!
   * @deprecated ensure the string is not null and use {@code
   *     CharMatcher.LEGACY_WHITESPACE.collapseFrom(str, ' ')}; also consider
   *     whether you really want the legacy whitespace definition, or something
   *     more standard like {@link CharMatcher#WHITESPACE}.
   */
  @Deprecated public static String collapseWhitespace(String str) {
    return (str == null) ? null
        : CharMatcher.LEGACY_WHITESPACE.collapseFrom(str, ' ');
  }

  /**
   * Replaces any string of matched characters with the supplied string.<p>
   *
   * This is a more general version of collapseWhitespace.
   *
   * <pre>
   *   E.g. collapse("hello     world", " ", "::")
   *   will return the following string: "hello::world"
   * </pre>
   *
   * @param str the string you want to munge
   * @param chars all of the characters to be considered for munge
   * @param replacement the replacement string
   * @return munged and replaced string.
   * @deprecated if {@code replacement} is the empty string, use {@link
   *     CharMatcher#removeFrom(CharSequence)}; if it is a single character,
   *     use {@link CharMatcher#collapseFrom(CharSequence, char)}; for longer
   *     replacement strings use {@link String#replaceAll(String, String)} with
   *     a regular expression that matches one or more occurrences of {@code
   *     chars}. In all cases you must first ensure that {@code str} is not
   *     null.
   */
  @Deprecated public static String collapse(
      String str, String chars, String replacement) {
    if (str == null) {
      return null;
    }

    StringBuilder newStr = new StringBuilder();

    boolean prevCharMatched = false;
    char c;
    for (int i = 0; i < str.length(); i++) {
      c = str.charAt(i);
      if (chars.indexOf(c) != -1) {
        // this character is matched
        if (prevCharMatched) {
          // apparently a string of matched chars, so don't append anything
          // to the string
          continue;
        }
        prevCharMatched = true;
        newStr.append(replacement);
      } else {
        prevCharMatched = false;
        newStr.append(c);
      }
    }

    return newStr.toString();
  }

  /**
   * Returns a string with all sequences of ISO control chars (0x00 to 0x1F and
   * 0x7F to 0x9F) replaced by the supplied string.  ISO control characters are
   * identified via {@link Character#isISOControl(char)}.
   *
   * @param str the string you want to strip of ISO control chars
   * @param replacement the replacement string
   * @return a String with all control characters replaced by the replacement
   * string, or null if input is null.
   * @deprecated use {@link CharMatcher#JAVA_ISO_CONTROL}. If {@code
   *     replacement} is the empty string, use {@link
   *     CharMatcher#removeFrom(CharSequence)}; if it is a single character,
   *     use {@link CharMatcher#collapseFrom(CharSequence, char)}; for longer
   *     replacement strings use
   *     {@code str.replaceAll("\p{Cntrl}+", replacement)}.
   *     In all cases you must first ensure that {@code str} is not null.
   */
  @Deprecated public static String collapseControlChars(
      String str, String replacement) {
    /*
     * We re-implement the StringUtil.collapse() loop here rather than call
     * collapse() with an input String of control chars, because matching via
     * isISOControl() is about 10x faster.
     */
    if (str == null) {
      return null;
    }

    StringBuilder newStr = new StringBuilder();

    boolean prevCharMatched = false;
    char c;
    for (int i = 0; i < str.length(); i++) {
      c = str.charAt(i);
      if (Character.isISOControl(c)) {
        // this character is matched
        if (prevCharMatched) {
          // apparently a string of matched chars, so don't append anything
          // to the string
          continue;
        }
        prevCharMatched = true;
        newStr.append(replacement);
      } else {
        prevCharMatched = false;
        newStr.append(c);
      }
    }

    return newStr.toString();
  }

  /**
   * Read a String of up to maxLength bytes from an InputStream.
   *
   * <p>Note that this method uses the default platform encoding, and expects
   * that encoding to be single-byte, which is not always the case. Its use
   * is discouraged. For reading the entire stream (maxLength == -1) you can use:
   * <pre>
   *   CharStreams.toString(new InputStreamReader(is, Charsets.ISO_8859_1))
   * </pre>
   * {@code CharStreams} is in the {@code com.google.common.io} package.
   *
   * <p>For maxLength >= 0 a literal translation would be
   * <pre>
   *   CharStreams.toString(new InputStreamReader(
   *       new LimitInputStream(is, maxLength), Charsets.ISO_8859_1))
   * </pre>
   * For multi-byte encodings that is broken because the limit could end in
   * the middle of the character--it would be better to limit the reader than
   * the underlying stream.
   *
   * @param is input stream
   * @param maxLength max number of bytes to read from "is". If this is -1, we
   *          read everything.
   *
   * @return String up to maxLength bytes, read from "is"
   * @deprecated see the advice above
   */
  @Deprecated public static String stream2String(InputStream is, int maxLength)
      throws IOException {
    byte[] buffer = new byte[4096];
    StringWriter sw = new StringWriter();
    int totalRead = 0;
    int read = 0;

    do {
      sw.write(new String(buffer, 0, read));
      totalRead += read;
      read = is.read(buffer, 0, buffer.length);
    } while (((-1 == maxLength) || (totalRead < maxLength)) && (read != -1));

    return sw.toString();
  }

  /**
   * Parse a list of substrings separated by a given delimiter. The delimiter
   * can also appear in substrings (just double them):
   *
   * parseDelimitedString("this|is", '|') returns ["this","is"]
   * parseDelimitedString("this||is", '|') returns ["this|is"]
   *
   * @param list String containing delimited substrings
   * @param delimiter Delimiter (anything except ' ' is allowed)
   *
   * @return String[] A String array of parsed substrings
   */
  public static String[] parseDelimitedList(String list,
                                            char delimiter) {
    String delim = "" + delimiter;
    // Append a sentinel of delimiter + space
    // (see comments below for more info)
    StringTokenizer st = new StringTokenizer(list + delim + " ",
                                             delim,
                                             true);
    ArrayList<String> v = new ArrayList<String>();
    String lastToken = "";
    StringBuilder word = new StringBuilder();

    // We keep a sliding window of 2 tokens
    //
    // delimiter : delimiter -> append delimiter to current word
    //                          and clear most recent token
    //                          (so delim : delim : delim will not
    //                          be treated as two escaped delims.)
    //
    // tok : delimiter -> append tok to current word
    //
    // delimiter : tok -> add current word to list, and clear it.
    //                    (We append a sentinel that conforms to this
    //                    pattern to make sure we've pushed every parsed token)
    while (st.hasMoreTokens()) {
      String tok = st.nextToken();
      if (lastToken != null) {
        if (tok.equals(delim)) {
          word.append(lastToken);
          if (lastToken.equals(delim)) { tok = null; }
        } else {
          if (word.length() != 0) {
            v.add(word.toString());
          }
          word.setLength(0);
        }
      }
      lastToken = tok;
    }

    return v.toArray(new String[0]);
  }

  /**
   * Compares two strings, guarding against nulls.
   *
   * @param nullsAreGreater true if nulls should be greater than any string,
   *  false is less than.
   * @deprecated use {@link String#CASE_INSENSITIVE_ORDER}, together with
   *     {@link com.google.common.collect.Ordering#nullsFirst()} or
   *     {@link com.google.common.collect.Ordering#nullsLast()} if
   *     needed
   */
  @Deprecated public static int compareToIgnoreCase(String s1, String s2,
      boolean nullsAreGreater) {
    if (s1 == s2) {
      return 0; // Either both the same String, or both null
    }
    if (s1 == null) {
      return nullsAreGreater ? 1 : -1;
    }
    if (s2 == null) {
      return nullsAreGreater ? -1 : 1;
    }
    return s1.compareToIgnoreCase(s2);
  }

  /**
   * Splits s with delimiters in delimiter and returns the last token
   */
  public static String lastToken(String s, String delimiter) {
    return s.substring(CharMatcher.anyOf(delimiter).lastIndexIn(s) + 1);
  }

  private static final Pattern characterReferencePattern =
      Pattern.compile("&#?[a-zA-Z0-9]{1,8};");

  /**
   * Determines if a string contains what looks like an html character
   * reference. Useful for deciding whether unescaping is necessary.
   */
  public static boolean containsCharRef(String s) {
    return characterReferencePattern.matcher(s).find();
  }

  /**
   * Determines if a string is a Hebrew word. A string is considered to be
   * a Hebrew word if {@link #isHebrew(int)} is true for any of its characters.
   */
  public static boolean isHebrew(String s) {
    int len = s.length();
    for (int i = 0; i < len; ++i) {
      if (isHebrew(s.codePointAt(i))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determines if a character is a Hebrew character.
   */
  public static boolean isHebrew(int codePoint) {
    return Character.UnicodeBlock.HEBREW.equals(
               Character.UnicodeBlock.of(codePoint));
  }

  /**
   * Determines if a string is a CJK word. A string is considered to be CJK
   * if {@link #isCjk(char)} is true for any of its characters.
   */
  public static boolean isCjk(String s) {
    int len = s.length();
    for (int i = 0; i < len; ++i) {
      if (isCjk(s.codePointAt(i))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Unicode code blocks containing CJK characters.
   */
  private static final Set<Character.UnicodeBlock> CJK_BLOCKS;
  static {
    Set<Character.UnicodeBlock> set = new HashSet<Character.UnicodeBlock>();
    set.add(Character.UnicodeBlock.HANGUL_JAMO);
    set.add(Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT);
    set.add(Character.UnicodeBlock.KANGXI_RADICALS);
    set.add(Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION);
    set.add(Character.UnicodeBlock.HIRAGANA);
    set.add(Character.UnicodeBlock.KATAKANA);
    set.add(Character.UnicodeBlock.BOPOMOFO);
    set.add(Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO);
    set.add(Character.UnicodeBlock.KANBUN);
    set.add(Character.UnicodeBlock.BOPOMOFO_EXTENDED);
    set.add(Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS);
    set.add(Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS);
    set.add(Character.UnicodeBlock.CJK_COMPATIBILITY);
    set.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A);
    set.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
    set.add(Character.UnicodeBlock.HANGUL_SYLLABLES);
    set.add(Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS);
    set.add(Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS);
    set.add(Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS);
    set.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B);
    set.add(Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT);
    CJK_BLOCKS = Collections.unmodifiableSet(set);
  }

  /**
   * Determines if a character is a CJK ideograph or a character typically
   * used only in CJK text.
   *
   * Note: This function cannot handle supplementary characters. To handle all
   * Unicode characters, including supplementary characters, use the function
   * {@link #isCjk(int)}.
   */
  public static boolean isCjk(char ch) {
    return isCjk((int) ch);
  }

  /**
   * Determines if a character is a CJK ideograph or a character typically
   * used only in CJK text.
   */
  public static boolean isCjk(int codePoint) {
    // Time-saving early exit for all Latin-1 characters.
    if ((codePoint & 0xFFFFFF00) == 0) {
      return false;
    }

    return CJK_BLOCKS.contains(Character.UnicodeBlock.of(codePoint));
  }

  /**
   * Returns the approximate display width of the string, measured in units of
   * ascii characters.
   *
   * @see StringUtil#displayWidth(char)
   */
  public static int displayWidth(String s) {
    // TODO(kevinb): could reimplement this as
    // return s.length() * 2 - CharMatcher.SINGLE_WIDTH.countIn(s);
    int width = 0;
    int len = s.length();
    for (int i = 0; i < len; ++i) {
      width += displayWidth(s.charAt(i));
    }
    return width;
  }

  /**
   * Returns the approximate display width of the character, measured
   * in units of ascii characters.
   *
   * This method should err on the side of caution. By default, characters
   * are assumed to have width 2; this covers CJK ideographs, various
   * symbols and miscellaneous weird scripts. Given below are some Unicode
   * ranges for which it seems safe to assume that no character is
   * substantially wider than an ascii character:
   *   - Latin, extended Latin, even more extended Latin.
   *   - Greek, extended Greek, Cyrillic.
   *   - Some symbols (including currency symbols) and punctuation.
   *   - Half-width Katakana and Hangul.
   *   - Hebrew
   *   - Arabic
   *   - Thai
   * Characters in these ranges are given a width of 1.
   *
   * IMPORTANT: this function has analogs in C++ (encodingutils.cc,
   * named UnicodeCharWidth) and JavaScript
   * (java/com/google/ads/common/frontend/adwordsbase/resources/CreateAdUtil.js),
   * which need to be updated if you change the implementation here.
   */
  public static int displayWidth(char ch) {
    if (ch <= '\u04f9' ||   // CYRILLIC SMALL LETTER YERU WITH DIAERESIS
        ch == '\u05be' ||   // HEBREW PUNCTUATION MAQAF
        (ch >= '\u05d0' && ch <= '\u05ea') ||  // HEBREW LETTER ALEF ... TAV
        ch == '\u05F3' ||   // HEBREW PUNCTUATION GERESH
        ch == '\u05f4' ||   // HEBREW PUNCTUATION GERSHAYIM
        (ch >= '\u0600' && ch <= '\u06ff') || // Block=Arabic
        (ch >= '\u0750' && ch <= '\u077f') || // Block=Arabic_Supplement
        (ch >= '\ufb50' && ch <= '\ufdff') || // Block=Arabic_Presentation_Forms-A
        (ch >= '\ufe70' && ch <= '\ufeff') || // Block=Arabic_Presentation_Forms-B
        (ch >= '\u1e00' && ch <= '\u20af') || /* LATIN CAPITAL LETTER A WITH RING BELOW
                                                 ... DRACHMA SIGN */
        (ch >= '\u2100' && ch <= '\u213a') || // ACCOUNT OF ... ROTATED CAPITAL Q
        (ch >= '\u0e00' && ch <= '\u0e7f') || // Thai
        (ch >= '\uff61' && ch <= '\uffdc')) { /* HALFWIDTH IDEOGRAPHIC FULL STOP
                                                 ... HALFWIDTH HANGUL LETTER I */
      return 1;
    }
    return 2;
  }

  /**
   * @return a string representation of the given native array.
   */
  public static String toString(float[] iArray) {
    if (iArray == null) {
      return "NULL";
    }

    StringBuilder buffer = new StringBuilder();
    buffer.append("[");
    for (int i = 0; i < iArray.length; i++) {
      buffer.append(iArray[i]);
      if (i != (iArray.length - 1)) {
        buffer.append(", ");
      }
    }
    buffer.append("]");
    return buffer.toString();
  }

  /**
   * @return a string representation of the given native array.
   */
  public static String toString(long[] iArray) {
    if (iArray == null) {
      return "NULL";
    }

    StringBuilder buffer = new StringBuilder();
    buffer.append("[");
    for (int i = 0; i < iArray.length; i++) {
      buffer.append(iArray[i]);
      if (i != (iArray.length - 1)) {
        buffer.append(", ");
      }
    }
    buffer.append("]");
    return buffer.toString();
  }

  /**
   * @return a string representation of the given native array
   */
  public static String toString(int[] iArray) {
    if (iArray == null) {
      return "NULL";
    }

    StringBuilder buffer = new StringBuilder();
    buffer.append("[");
    for (int i = 0; i < iArray.length; i++) {
      buffer.append(iArray[i]);
      if (i != (iArray.length - 1)) {
        buffer.append(", ");
      }
    }
    buffer.append("]");
    return buffer.toString();
  }

  /**
   * @return a string representation of the given array.
   */
  public static String toString(String[] iArray) {
    if (iArray == null) { return "NULL"; }

    StringBuilder buffer = new StringBuilder();
    buffer.append("[");
    for (int i = 0; i < iArray.length; i++) {
      buffer.append("'").append(iArray[i]).append("'");
      if (i != iArray.length - 1) {
        buffer.append(", ");
      }
    }
    buffer.append("]");

    return buffer.toString();
  }

  /**
   * Returns the string, in single quotes, or "NULL". Intended only for
   * logging.
   *
   * @param s the string
   * @return the string, in single quotes, or the string "null" if it's null.
   */
  public static String toString(String s) {
    if (s == null) {
      return "NULL";
    } else {
      return new StringBuilder(s.length() + 2).append("'").append(s)
                                              .append("'").toString();
    }
  }

  /**
   * @return a string representation of the given native array
   */
  public static String toString(int[][] iArray) {
    if (iArray == null) {
      return "NULL";
    }

    StringBuilder buffer = new StringBuilder();
    buffer.append("[");
    for (int i = 0; i < iArray.length; i++) {
      buffer.append("[");
      for (int j = 0; j < iArray[i].length; j++) {
        buffer.append(iArray[i][j]);
        if (j != (iArray[i].length - 1)) {
          buffer.append(", ");
        }
      }
      buffer.append("]");
      if (i != iArray.length - 1) {
        buffer.append(" ");
      }
    }
    buffer.append("]");
    return buffer.toString();
  }

  /**
   * @return a string representation of the given native array.
   */
  public static String toString(long[][] iArray) {
    if (iArray == null) { return "NULL"; }

    StringBuilder buffer = new StringBuilder();
    buffer.append("[");
    for (int i = 0; i < iArray.length; i++) {
      buffer.append("[");
      for (int j = 0; j < iArray[i].length; j++) {
        buffer.append(iArray[i][j]);
        if (j != (iArray[i].length - 1)) {
          buffer.append(", ");
        }
      }
      buffer.append("]");
      if (i != iArray.length - 1) {
        buffer.append(" ");
      }
    }
    buffer.append("]");
    return buffer.toString();
  }

  /**
   * @return a String representation of the given object array.
   * The strings are obtained by calling toString() on the
   * underlying objects.
   */
  public static String toString(Object[] obj) {
    if (obj == null) { return "NULL"; }
    StringBuilder tmp = new StringBuilder();
    tmp.append("[");
    for (int i = 0; i < obj.length; i++) {
      tmp.append(obj[i].toString());
      if (i != obj.length - 1) {
        tmp.append(",");
      }
    }
    tmp.append("]");
    return tmp.toString();
  }

  private static final char[] HEX_CHARS
      = { '0', '1', '2', '3', '4', '5', '6', '7',
          '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
  private static final char[] OCTAL_CHARS = HEX_CHARS;  // ignore the last 8 :)

  /**
   * Convert a byte array to a hex-encoding string: "a33bff00..."
   *
   * @deprecated Use {@link ByteArrays#toHexString}.
   */
  @Deprecated public static String bytesToHexString(final byte[] bytes) {
    return ByteArrays.toHexString(bytes);
  }

  /**
   * Convert a byte array to a hex-encoding string with the specified
   * delimiter: "a3&lt;delimiter&gt;3b&lt;delimiter&gt;ff..."
   */
  public static String bytesToHexString(final byte[] bytes,
      Character delimiter) {
    StringBuilder hex =
      new StringBuilder(bytes.length * (delimiter == null ? 2 : 3));
    int nibble1, nibble2;
    for (int i = 0; i < bytes.length; i++) {
      nibble1 = (bytes[i] >>> 4) & 0xf;
      nibble2 = bytes[i] & 0xf;
      if (i > 0 && delimiter != null) { hex.append(delimiter.charValue()); }
      hex.append(HEX_CHARS[nibble1]);
      hex.append(HEX_CHARS[nibble2]);
    }
    return hex.toString();
  }

  /**
   * Safely convert the string to uppercase.
   * @return upper case representation of the String; or null if
   * the input string is null.
   */
  public static String toUpperCase(String src) {
    if (src == null) {
      return null;
    } else {
      return src.toUpperCase();
    }
  }

  /**
   * Safely convert the string to lowercase.
   * @return lower case representation of the String; or null if
   * the input string is null.
   */
  public static String toLowerCase(String src) {
    if (src == null) {
      return null;
    } else {
      return src.toLowerCase();
    }
  }

  private static final Pattern dbSpecPattern =
      Pattern.compile("(.*)\\{(\\d+),(\\d+)\\}(.*)");

  /**
   * @param dbSpecComponent a single component of a DBDescriptor spec
   * (e.g. the host or database component). The expected format of the string is:
   * <br>
   *             <center>(prefix){(digits),(digits)}(suffix)</center>
   * </br>
   * @return a shard expansion of the given String.
   * Note that unless the pattern is matched exactly, no expansion is
   * performed and the original string is returned unaltered.
   * For example, 'db{0,1}.adz' is expanded into 'db0.adz, db1.adz'.
   * Note that this method is added to StringUtil instead of
   * DBDescriptor to better encapsulate the choice of regexp implementation.
   * @throws IllegalArgumentException if the string does not parse.
   */
  public static String expandShardNames(String dbSpecComponent)
      throws IllegalArgumentException, IllegalStateException {

    Matcher matcher = dbSpecPattern.matcher(dbSpecComponent);
    if (matcher.find()) {
      try {
        String prefix = dbSpecComponent.substring(
          matcher.start(1), matcher.end(1));
        int minShard =
          Integer.parseInt(
            dbSpecComponent.substring(
              matcher.start(2), matcher.end(2)));
        int maxShard =
          Integer.parseInt(
            dbSpecComponent.substring(
              matcher.start(3), matcher.end(3)));
        String suffix = dbSpecComponent.substring(
          matcher.start(4), matcher.end(4));
        //Log2.logEvent(prefix + " " + minShard + " " + maxShard + " " + suffix);
        if (minShard > maxShard) {
          throw new IllegalArgumentException(
            "Maximum shard must be greater than or equal to " +
            "the minimum shard");
        }
        StringBuilder tmp = new StringBuilder();
        for (int shard = minShard; shard <= maxShard; shard++) {
          tmp.append(prefix).append(shard).append(suffix);
          if (shard != maxShard) {
            tmp.append(",");
          }
        }
        return tmp.toString();
      } catch (NumberFormatException nfex) {
        throw new IllegalArgumentException(
          "Malformed DB specification component: " + dbSpecComponent);
      }
    } else {
      return dbSpecComponent;
    }
  }


  /**
  * Returns a string that is equivalent to the specified string with its
  * first character converted to uppercase as by {@link String#toUpperCase()}.
  * The returned string will have the same value as the specified string if
  * its first character is non-alphabetic, if its first character is already
  * uppercase, or if the specified string is of length 0.
  *
  * <p>For example:
  * <pre>
  *    capitalize("foo bar").equals("Foo bar");
  *    capitalize("2b or not 2b").equals("2b or not 2b")
  *    capitalize("Foo bar").equals("Foo bar");
  *    capitalize("").equals("");
  * </pre>
  *
  * @param s the string whose first character is to be uppercased
  * @return a string equivalent to <tt>s</tt> with its first character
  *     converted to uppercase
  * @throws NullPointerException if <tt>s</tt> is null
  */
  public static String capitalize(String s) {
    if (s.length() == 0) {
      return s;
    }
    char first = s.charAt(0);
    char capitalized = Character.toUpperCase(first);
    return (first == capitalized)
        ? s
        : capitalized + s.substring(1);
  }

  /**
   * Examine a string to see if it starts with a given prefix (case
   * insensitive). Just like String.startsWith() except doesn't
   * respect case. Strings are compared in the same way as in
   * {@link String#equalsIgnoreCase}.
   *
   * @param str the string to examine
   * @param prefix the prefix to look for
   * @return a boolean indicating if str starts with prefix (case insensitive)
   */
  public static boolean startsWithIgnoreCase(String str, String prefix) {
    return str.regionMatches(true, 0, prefix, 0, prefix.length());
  }

  /**
   * Examine a string to see if it ends with a given suffix (case
   * insensitive). Just like String.endsWith() except doesn't respect
   * case. Strings are compared in the same way as in
   * {@link String#equalsIgnoreCase}.
   *
   * @param str the string to examine
   * @param suffix the suffix to look for
   * @return a boolean indicating if str ends with suffix (case insensitive)
   */
  public static boolean endsWithIgnoreCase(String str, String suffix) {
    int len = suffix.length();
    return str.regionMatches(true, str.length() - len, suffix, 0, len);
  }

  /**
   * @param c one codePoint
   * @return the number of bytes needed to encode this codePoint in UTF-8
   */
  private static int bytesUtf8(int c) {
    if (c < 0x80) {
      return 1;
    } else if (c < 0x00800) {
      return 2;
    } else if (c < 0x10000) {
      return 3;
    } else if (c < 0x200000) {
      return 4;

    // RFC 3629 forbids the use of UTF-8 for codePoint greater than 0x10FFFF,
    // so if the caller respects this RFC, this should not happen
    } else if (c < 0x4000000) {
      return 5;
    } else {
      return 6;
    }
  }

  /**
   * @param str a string
   * @return the number of bytes required to represent this string in UTF-8
   */
  public static int bytesStorage(String str) {
    // offsetByCodePoint has a bug if its argument is the result of a
    // call to substring. To avoid this, we create a new String
    // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6242664
    String s = new String(str);

    int len = 0;
    for (int i = 0; i < s.length(); i = s.offsetByCodePoints(i, 1)) {
      len += bytesUtf8(s.codePointAt(i));
    }
    return len;
  }

  /**
   * @param str a string
   * @param maxbytes
   * @return the beginning of the string, so that it uses less than
   *     maxbytes bytes in UTF-8
   * @throws IndexOutOfBoundsException if maxbytes is negative
   */
  public static String truncateStringForUtf8Storage(String str, int maxbytes) {
    if (maxbytes < 0) {
      throw new IndexOutOfBoundsException();
    }

    // offsetByCodePoint has a bug if its argument is the result of a
    // call to substring. To avoid this, we create a new String
    // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6242664
    // TODO(cquinn): should be fixed as of 1.5.0_01
    String s = new String(str);

    int codepoints = 0;
    int bytesUsed = 0;
    for (codepoints = 0; codepoints < s.length();
        codepoints = s.offsetByCodePoints(codepoints, 1)) {
      int glyphBytes = StringUtil.bytesUtf8(s.codePointAt(codepoints));
      if (bytesUsed + glyphBytes > maxbytes) {
        break;
      }
      bytesUsed += glyphBytes;
    }
    return s.substring(0, codepoints);
  }

  /**
   * If the given string is of length {@code maxLength} or less, then it is
   * returned as is.
   * If the string is longer than {@code maxLength}, the returned string is
   * truncated before the last space character on or before
   * {@code source.charAt(maxLength)}. If the string has no spaces, the
   * returned string is truncated to {@code maxLength}.
   *
   * @param source the string to truncate if necessary
   * @param maxLength
   * @return the original string if its length is less than or equal to
   *     maxLength, otherwise a truncated string as mentioned above
   */
  public static String truncateIfNecessary(String source, int maxLength) {
    if (source.length() <= maxLength) {
      return source;
    }
    String str = unicodePreservingSubstring(source, 0, maxLength);

    @SuppressWarnings("deprecation") // we'll make this go away before that does
    CharMatcher whitespaceMatcher = CharMatcher.LEGACY_WHITESPACE;
    String truncated = whitespaceMatcher.trimTrailingFrom(str);

    // We may have had multiple spaces at maxLength, which were stripped away
    if (truncated.length() < maxLength) {
      return truncated;
    }
    // We have a truncated string of length maxLength. If the next char was a
    // space, we truncated at a word boundary, so we can return immediately
    if (Character.isSpaceChar(source.charAt(maxLength))) {
      return truncated;
    }
    // We truncated in the middle of the word. Try to truncate before
    // the last space, if it exists. Otherwise, return the truncated string
    for (int i = truncated.length() - 1; i >= 0; --i) {
      if (Character.isSpaceChar(truncated.charAt(i))) {
        String substr = truncated.substring(0, i);
        return whitespaceMatcher.trimTrailingFrom(substr);
      }
    }
    return truncated;
  }

  /**
   * If this given string is of length {@code maxLength} or less, it will
   * be returned as-is.
   * Otherwise it will be trucated to {@code maxLength}, regardless of whether
   * there are any space characters in the String. If an ellipsis is requested
   * to be appended to the truncated String, the String will be truncated so
   * that the ellipsis will also fit within maxLength.
   * If no truncation was necessary, no ellipsis will be added.
   *
   * @param source the String to truncate if necessary
   * @param maxLength the maximum number of characters to keep
   * @param addEllipsis if true, and if the String had to be truncated,
   *     add "..." to the end of the String before returning. Additionally,
   *     the ellipsis will only be added if maxLength is greater than 3.
   * @return the original string if its length is less than or equal to
   *     maxLength, otherwise a truncated string as mentioned above
   */
  public static String truncateAtMaxLength(String source, int maxLength,
      boolean addEllipsis) {

    if (source.length() <= maxLength) {
      return source;
    }
    if (addEllipsis && maxLength > 3) {
      return unicodePreservingSubstring(source, 0, maxLength - 3) + "...";
    }
    return unicodePreservingSubstring(source, 0, maxLength);
  }

  /**
   * Normalizes {@code index} such that it respects Unicode character
   * boundaries in {@code str}.
   *
   * <p>If {@code index} is the low surrogate of a unicode character,
   * the method returns {@code index - 1}. Otherwise, {@code index} is
   * returned.
   *
   * <p>In the case in which {@code index} falls in an invalid surrogate pair
   * (e.g. consecutive low surrogates, consecutive high surrogates), or if
   * if it is not a valid index into {@code str}, the original value of
   * {@code index} is returned.
   *
   * @param str the String
   * @param index the index to be normalized
   * @return a normalized index that does not split a Unicode character
   */
  public static int unicodePreservingIndex(String str, int index) {
    if (index > 0 && index < str.length()) {
      if (Character.isHighSurrogate(str.charAt(index - 1)) &&
          Character.isLowSurrogate(str.charAt(index))) {
        return index - 1;
      }
    }
    return index;
  }

  /**
   * Returns a substring of {@code str} that respects Unicode character
   * boundaries.
   *
   * <p>The string will never be split between a [high, low] surrogate pair,
   * as defined by {@link Character#isHighSurrogate} and
   * {@link Character#isLowSurrogate}.
   *
   * <p>If {@code begin} or {@code end} are the low surrogate of a unicode
   * character, it will be offset by -1.
   *
   * <p>This behavior guarantees that
   * {@code str.equals(StringUtil.unicodePreservingSubstring(str, 0, n) +
   *     StringUtil.unicodePreservingSubstring(str, n, str.length())) } is
   * true for all {@code n}.
   * </pre>
   *
   * <p>This means that unlike {@link String#substring(int, int)}, the length of
   * the returned substring may not necessarily be equivalent to
   * {@code end - begin}.
   *
   * @param str the original String
   * @param begin the beginning index, inclusive
   * @param end the ending index, exclusive
   * @return the specified substring, possibly adjusted in order to not
   *   split unicode surrogate pairs
   * @throws IndexOutOfBoundsException if the {@code begin} is negative,
   *   or {@code end} is larger than the length of {@code str}, or
   *   {@code begin} is larger than {@code end}
   */
  public static String unicodePreservingSubstring(
      String str, int begin, int end) {
    return str.substring(unicodePreservingIndex(str, begin),
        unicodePreservingIndex(str, end));
  }

  /**
   * Equivalent to:
   *
   * <pre>
   * {@link #unicodePreservingSubstring(String, int, int)}(
   *     str, begin, str.length())
   * </pre>
   */
  public static String unicodePreservingSubstring(String str, int begin) {
    return unicodePreservingSubstring(str, begin, str.length());
  }

  /**
   * True iff the given character needs to be escaped in a javascript string
   * literal.
   * <p>
   * We need to escape the following characters in javascript string literals.
   * <dl>
   * <dt> \           <dd> the escape character
   * <dt> ', "        <dd> string delimiters.
   *                       TODO(msamuel): what about backticks (`) which are
   *                       non-standard but recognized as attribute delimiters.
   * <dt> &, <, >, =  <dd> so that a string literal can be embedded in XHTML
   *                       without further escaping.
   * </dl>
   * TODO(msamuel): If we're being paranoid, should we escape + to avoid UTF-7
   * attacks?
   * <p>
   * Unicode format control characters (category Cf) must be escaped since they
   * are removed by javascript parser in a pre-lex pass.
   * <br>According to EcmaScript 262 Section 7.1:
   * <blockquote>
   *     The format control characters can occur anywhere in the source text of
   *     an ECMAScript program. These characters are removed from the source
   *     text before applying the lexical grammar.
   * </blockquote>
   * <p>
   * Additionally, line terminators are not allowed to appear inside strings
   * and Section 7.3 says
   * <blockquote>
   *     The following characters are considered to be line terminators:<pre>
   *         Code Point Value   Name                  Formal Name
   *         \u000A             Line Feed             [LF]
   *         \u000D             Carriage Return       [CR]
   *         \u2028             Line separator        [LS]
   *         \u2029             Paragraph separator   [PS]
   * </pre></blockquote>
   *
   * @param codepoint a char instead of an int since the javascript language
   *    does not support extended unicode.
   */
  static boolean mustEscapeCharInJsString(int codepoint) {
    return JS_ESCAPE_CHARS.contains(codepoint);
  }

  /**
   * True iff the given character needs to be escaped in a JSON string literal.
   * <p>
   * We need to escape the following characters in JSON string literals.
   * <dl>
   * <dt> \           <dd> the escape character
   * <dt> "           <dd> string delimiter
   * <dt> 0x00 - 0x1F <dd> control characters
   * </dl>
   * <p>
   * See EcmaScript 262 Section 15.12.1 for the full JSON grammar.
   */
  static boolean mustEscapeCharInJsonString(int codepoint) {
    return JSON_ESCAPE_CHARS.contains(codepoint);
  }

  /**
   * Builds a small set of code points.
   * {@code com.google.common.base} cannot depend on ICU4J, thus avoiding ICU's
   * {@code UnicodeSet}.
   * For all other purposes, please use {@code com.ibm.icu.text.UnicodeSet}.
   */
  private static class UnicodeSetBuilder {
    Set<Integer> codePointSet = new HashSet<Integer>();

    UnicodeSetBuilder addCodePoint(int c) {
      codePointSet.add(c);
      return this;
    }

    UnicodeSetBuilder addRange(int from, int to) {
      for (int i = from; i <= to; i++) {
        codePointSet.add(i);
      }
      return this;
    }

    Set<Integer> create() {
      return codePointSet;
    }
  }

  private static final Set<Integer> JS_ESCAPE_CHARS = new UnicodeSetBuilder()
      // All characters in the class of format characters, [:Cf:].
      // Source: http://unicode.org/cldr/utility/list-unicodeset.jsp.
      .addCodePoint(0xAD)
      .addRange(0x600, 0x603)
      .addCodePoint(0x6DD)
      .addCodePoint(0x070F)
      .addRange(0x17B4, 0x17B5)
      .addRange(0x200B, 0x200F)
      .addRange(0x202A, 0x202E)
      .addRange(0x2060, 0x2064)
      .addRange(0x206A, 0x206F)
      .addCodePoint(0xFEFF)
      .addRange(0xFFF9, 0xFFFB)
      .addRange(0x0001D173, 0x0001D17A)
      .addCodePoint(0x000E0001)
      .addRange(0x000E0020, 0x000E007F)
      // Plus characters mentioned in the docs of mustEscapeCharInJsString().
      .addCodePoint(0x0000)
      .addCodePoint(0x000A)
      .addCodePoint(0x000D)
      .addRange(0x2028, 0x2029)
      .addCodePoint(0x0085)
      .addCodePoint(Character.codePointAt("'", 0))
      .addCodePoint(Character.codePointAt("\"", 0))
      .addCodePoint(Character.codePointAt("&", 0))
      .addCodePoint(Character.codePointAt("<", 0))
      .addCodePoint(Character.codePointAt(">", 0))
      .addCodePoint(Character.codePointAt("=", 0))
      .addCodePoint(Character.codePointAt("\\", 0))
      .create();

  private static final Set<Integer> JSON_ESCAPE_CHARS = new UnicodeSetBuilder()
      .addCodePoint(Character.codePointAt("\"", 0))
      .addCodePoint(Character.codePointAt("\\", 0))
      .addRange(0x0000, 0x001F)
      .create();

  /**
   * <b>To be deprecated:</b> use {@link CharEscapers#xmlEscaper()} instead.
   */
  public static String xmlEscape(String s) {
    return CharEscapers.xmlEscaper().escape(s);
  }

  /**
   * <b>To be deprecated:</b> use {@link CharEscapers#asciiHtmlEscaper()} instead.
   */
  public static String htmlEscape(String s) {
    return CharEscapers.asciiHtmlEscaper().escape(s);
  }
}