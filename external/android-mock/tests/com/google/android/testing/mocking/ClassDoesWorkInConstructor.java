/*
 *  Copyright 2010 Google Inc.
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
package com.google.android.testing.mocking;


/**
 * Support class for testing.
 * 
 * @author swoodward@google.com (Stephen Woodward)
 */
public class ClassDoesWorkInConstructor {
  public ClassDoesWorkInConstructor() {
    this.fooInt(1);
    this.fooByte((byte) 1);
    this.fooShort((short) 1);
    this.fooChar('a');
    this.fooLong(1L);
    this.fooFloat(1.0f);
    this.fooDouble(1.0);
    this.fooBoolean(true);
    this.fooObject("hello");
    this.fooVoid();
  }

  public void fooVoid() {
    throw new IllegalStateException("I wasn't mocked!!");
  }

  public Object fooObject(String string) {
    throw new IllegalStateException("I wasn't mocked!!");
  }

  public boolean fooBoolean(boolean b) {
    throw new IllegalStateException("I wasn't mocked!!");
  }

  public double fooDouble(double d) {
    throw new IllegalStateException("I wasn't mocked!!");
  }

  public float fooFloat(float f) {
    throw new IllegalStateException("I wasn't mocked!!");
  }

  public long fooLong(long i) {
    throw new IllegalStateException("I wasn't mocked!!");
  }

  public char fooChar(char c) {
    throw new IllegalStateException("I wasn't mocked!!");
  }

  public short fooShort(short s) {
    throw new IllegalStateException("I wasn't mocked!!");
  }

  public byte fooByte(byte b) {
    throw new IllegalStateException("I wasn't mocked!!");
  }

  public int fooInt(int i) {
    throw new IllegalStateException("I wasn't mocked!!");
  }
}
