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

package com.google.clearsilver.jsilver.functions.string;

import com.google.clearsilver.jsilver.functions.NonEscapingFunction;
import com.google.clearsilver.jsilver.values.Value;
import static com.google.clearsilver.jsilver.values.Value.literalConstant;

import java.io.UnsupportedEncodingException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Returns the CRC-32 of a string.
 */
public class CrcFunction extends NonEscapingFunction {

  /**
   * @param args 1 string expression
   * @return CRC-32 of string as number value
   */
  public Value execute(Value... args) {
    String string = args[0].asString();
    // This function produces a 'standard' CRC-32 (IV -1, reflected polynomial,
    // and final complement step). The CRC-32 of "123456789" is 0xCBF43926.
    Checksum crc = new CRC32();
    byte[] b;
    try {
      b = string.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError("UTF-8 must be supported");
    }
    crc.update(b, 0, b.length);
    // Note that we need to cast to signed int, but that's okay because the
    // CRC fits into 32 bits by definition.
    return literalConstant((int) crc.getValue(), args[0]);
  }
}
