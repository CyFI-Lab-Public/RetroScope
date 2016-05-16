/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.android.testing.mocking;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Mock Generator for Android Framework classes.
 * 
 * <p>
 * This class will pull pre-defined mocks for Android framework classes. This is
 * needed because a project might build against version X and then run against
 * version Y, where the specification of the class being mocked will have
 * changed, rendering the mock invalid.
 * 
 * @author swoodward@google.com (Stephen Woodward)
 */
public class AndroidFrameworkMockGenerator {
  private static final String LIB_FOLDER = "lib";
  private static final String JAR_NAME = "android_";

  /**
   * Returns a set of mock support classes for the specified Class for all versions of
   * the Android SDK. If the requested class is not part of the Android framework, then the class
   * will not be found and an exception will be thrown.
   * 
   * @param clazz the class to mock.
   * @return All available mock support classes (for all known Android SDKs) for
   *         the requested Class.
   */
  public List<GeneratedClassFile> getMocksForClass(Class<?> clazz) throws ClassNotFoundException,
      IOException {
    List<Class<?>> prebuiltClasses = getPrebuiltClassesFor(clazz);
    List<GeneratedClassFile> classList = new ArrayList<GeneratedClassFile>();
    for (Class<?> prebuiltClass : prebuiltClasses) {
      try {
        CtClass ctClass = AndroidMockGenerator.getClassPool().get(prebuiltClass.getName());
        classList.add(new GeneratedClassFile(ctClass.getName(), ctClass.toBytecode()));
      } catch (NotFoundException e) {
        throw new ClassNotFoundException("Missing class while fetching prebuilt mocks: "
            + prebuiltClass.getName(), e);
      } catch (CannotCompileException e) {
        throw new RuntimeException("Internal Error copying a prebuilt mock: "
            + prebuiltClass.getName(), e);
      }
    }
    return classList;
  }

  private List<Class<?>> getPrebuiltClassesFor(Class<?> clazz) throws ClassNotFoundException {
    List<Class<?>> classes = new ArrayList<Class<?>>();
    SdkVersion[] versions = SdkVersion.getAllVersions();
    for (SdkVersion sdkVersion : versions) {
      classes.add(Class.forName(FileUtils.getSubclassNameFor(clazz, sdkVersion)));
      classes.add(Class.forName(FileUtils.getInterfaceNameFor(clazz, sdkVersion)));
    }
    return classes;
  }

  /**
   * @return a List of {@link GeneratedClassFile} objects representing the mocks for the specified
   *         class for a single version of the Android SDK.
   */
  public List<GeneratedClassFile> createMocksForClass(Class<?> clazz, SdkVersion version)
      throws ClassNotFoundException, IOException, CannotCompileException {
    AndroidMockGenerator mockGenerator = new AndroidMockGenerator();
    List<GeneratedClassFile> mocks = new ArrayList<GeneratedClassFile>();
    mocks.addAll(mockGenerator.createMocksForClass(clazz, version));
    return mocks;
  }

  /**
   * @return A list of all class files inside the provided jar file.
   */
  List<Class<?>> getClassList(JarFile jar) throws ClassNotFoundException {
    return getClassList(Collections.list(jar.entries()));
  }

  List<Class<?>> getClassList(Collection<JarEntry> jarEntries) throws ClassNotFoundException {
    List<Class<?>> classList = new ArrayList<Class<?>>();
    for (JarEntry jarEntry : jarEntries) {
      if (jarEntryIsClassFile(jarEntry)) {
        classList.add(Class.forName(FileUtils.getClassNameFor(jarEntry.getName())));
      }
    }
    return classList;
  }

  /**
   * @return true if the provided JarEntry represents a class file.
   */
  boolean jarEntryIsClassFile(JarEntry jarEntry) {
    return jarEntry.getName().endsWith(".class");
  }

  /**
   * @return the Android framework jar file for the specified {@link SdkVersion}.
   */
  static String getJarFileNameForVersion(SdkVersion version) {
    return new StringBuilder()
        .append(LIB_FOLDER)
        .append(File.separator)
        .append("android")
        .append(File.separator)
        .append(JAR_NAME)
        .append(version.getVersionName())
        .append(".jar").toString();
  }

  private static Set<GeneratedClassFile> generateMocks(
      SdkVersion version, JarFile jar)
      throws ClassNotFoundException, IOException, CannotCompileException {
    AndroidFrameworkMockGenerator mockGenerator = new AndroidFrameworkMockGenerator();
    List<Class<?>> classList = mockGenerator.getClassList(jar);
    Set<GeneratedClassFile> classes = new HashSet<GeneratedClassFile>();
    for (Class<?> clazz : classList) {
      classes.addAll(mockGenerator.createMocksForClass(clazz, version));
    }
    return classes;
  }

  private static JarFile getJarFile(SdkVersion version) throws IOException {
    File jarFile = new File(getJarFileNameForVersion(version)).getAbsoluteFile();
    System.out.println("Using Jar File: " + jarFile.getAbsolutePath());
    return new JarFile(jarFile);
  }

  public static void main(String[] args) {
    try {
      String outputFolderName = args[0];
      SdkVersion version = SdkVersion.getVersionFor(Integer.parseInt(args[1]));
      System.out.println("Generating files for " + version.getPackagePrefix());

      JarFile jar = getJarFile(version);

      Set<GeneratedClassFile> classes = generateMocks(version, jar);
      for (GeneratedClassFile clazz : classes) {
        FileUtils.saveClassToFolder(clazz, outputFolderName);
      }
    } catch (Exception e) {
      throw new RuntimeException("Internal error generating framework mocks", e);
    }
  }
}
