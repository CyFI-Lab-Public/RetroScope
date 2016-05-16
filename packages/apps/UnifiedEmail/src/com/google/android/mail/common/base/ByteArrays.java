/**
 * Copyright (c) 2008, Google Inc.
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

package com.google.android.mail.common.base;

/**
 * Static utility methods pertaining especially to byte arrays. Note that I/O-related functionality
 * belongs in the {@code com.google.common.io} package.
 *
 * @author Chris Nokleberg
 * @author Hiroshi Yamauchi
 */
public final class ByteArrays {
  private ByteArrays() {}

  private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

  /**
   * Returns the byte array formatted as a lowercase hexadecimal string. The string will be
   * {@code 2 * bytes.length} characters long.
   */
  public static String toHexString(byte[] bytes) {
    StringBuilder sb = new StringBuilder(2 * bytes.length);
    for (byte b : bytes) {
      sb.append(HEX_DIGITS[(b >> 4) & 0xf]).append(HEX_DIGITS[b & 0xf]);
    }
    return sb.toString();
  }
}