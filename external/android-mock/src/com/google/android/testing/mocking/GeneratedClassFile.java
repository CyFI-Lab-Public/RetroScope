/*
 * Copyright 2010 Google Inc.
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
package com.google.android.testing.mocking;

/**
 * Represents the contents of a Class file.
 * 
 * @author swoodward@google.com (Stephen Woodward)
 */
public class GeneratedClassFile {
  private final String className;
  private final byte[] contents;

  /**
   * @param name the fully qualified name of the class.
   * @param classFileContents the binary contents of the file.
   */
  public GeneratedClassFile(String name, byte[] classFileContents) {
    className = name;
    contents = classFileContents;
  }

  public String getClassName() {
    return className;
  }

  public byte[] getContents() {
    return contents;
  }

  @Override
  public int hashCode() {
    return (this.getClass().getName() + className).hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof GeneratedClassFile)
      && className.equals(((GeneratedClassFile) obj).getClassName());
  }
}
