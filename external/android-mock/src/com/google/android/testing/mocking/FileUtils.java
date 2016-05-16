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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author swoodward@google.com (Stephen Woodward)
 */
public class FileUtils {

  /**
   * @param clazz
   * @param sdkVersion
   * @return the appropriate interface name for the interface mock support file.
   */
  static String getInterfaceNameFor(Class<?> clazz, SdkVersion sdkVersion) {
    return sdkVersion.getPackagePrefix() + "genmocks." + clazz.getName() + "DelegateInterface";
  }
  /**
   * @param clazz
   * @param sdkVersion
   * @return the appropriate subclass name for the subclass mock support file.
   */
  static String getSubclassNameFor(Class<?> clazz, SdkVersion sdkVersion) {
    return sdkVersion.getPackagePrefix() + "genmocks." + clazz.getName() + "DelegateSubclass";
  }

  /**
   * Converts a class name into the a .class filename.
   * 
   * @param className
   * @return the file name for the specified class name.
   */
  static String getFilenameFor(String className) {
    return className.replace('.', File.separatorChar) + ".class";
  }

  /**
   * Converts a filename into a class name.
   * 
   * @param filename
   * @return the class name for the specified file name.
   */
  static String getClassNameFor(String filename) {
    if (!filename.endsWith(".class")) {
      throw new IllegalArgumentException("Argument provided is not a class filename: " + filename);
    }
    // On non-Linux, files use the native separator, but jar entries use /... sigh
    return filename.replace(File.separatorChar, '.').replace('/', '.')
        .substring(0, filename.length() - 6);
  }

  static void saveClassToFolder(GeneratedClassFile clazz, String outputFolderName)
      throws FileNotFoundException, IOException {
    File classFolder = new File(outputFolderName);
    File targetFile = new File(classFolder, getFilenameFor(clazz.getClassName()));
    targetFile.getParentFile().mkdirs();
    FileOutputStream outputStream = new FileOutputStream(targetFile);
    outputStream.write(clazz.getContents());
    outputStream.close();
  }
}
