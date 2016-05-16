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
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import junit.framework.TestCase;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Tests for the AndroidMockGenerator class.
 * 
 * @author swoodward@google.com (Stephen Woodward)
 */
public class AndroidMockGeneratorTest extends TestCase {
  private AndroidMockGenerator getAndroidMockGenerator() {
    return new AndroidMockGenerator();
  }

  private NoFileAndroidMockGenerator getNoFileMockGenerator() {
    return new NoFileAndroidMockGenerator();
  }

  private void cleanupGeneratedClasses(CtClass... classes) {
    for (CtClass clazz : classes) {
      clazz.detach();
    }
  }

  private <T> void assertUnorderedContentsSame(Iterable<T> expected, Iterable<T> actual) {
    List<T> missingItems = new ArrayList<T>();
    List<T> extraItems = new ArrayList<T>();
    for (T item : expected) {
      missingItems.add(item);
    }
    for (T item : actual) {
      missingItems.remove(item);
      extraItems.add(item);
    }
    for (T item : expected) {
      extraItems.remove(item);
    }
    if (missingItems.size() + extraItems.size() != 0) {
      String errorMessage =
          "Contents were different. Missing: " + Arrays.toString(missingItems.toArray())
              + " Extra: " + Arrays.toString(extraItems.toArray());
      fail(errorMessage);
    }
  }

  private List<String> getExpectedNamesForNumberClass() {
    return getExpectedNamesForNumberClass(false);
  }

  private List<String> getExpectedNamesForObjectClass() {
    List<String> expectedNames = new ArrayList<String>();
    expectedNames.addAll(Arrays.asList(new String[] {"clone", "finalize"}));
    return expectedNames;
  }

  private List<String> getExpectedNamesForNumberClass(boolean includeDelegateMethods) {
    List<String> expectedNames = getExpectedNamesForObjectClass();
    expectedNames.addAll(Arrays.asList(new String[] {"byteValue", "doubleValue", "floatValue",
        "intValue", "longValue", "shortValue"}));
    if (includeDelegateMethods) {
      expectedNames.addAll(Arrays.asList(new String[] {"getDelegate___AndroidMock",
          "setDelegate___AndroidMock"}));
    }
    return expectedNames;
  }

  private List<String> getExpectedNamesForBigIntegerClass() {
    List<String> expectedNames = getExpectedNamesForNumberClass();
    expectedNames.addAll(Arrays.asList(new String[] {"abs", "add", "and", "andNot", "bitCount",
        "bitLength", "clearBit", "compareTo", "divide", "divideAndRemainder", "flipBit", "gcd",
        "getLowestSetBit", "isProbablePrime", "max", "min", "mod", "modInverse", "modPow",
        "multiply", "negate", "nextProbablePrime", "not", "or", "pow", "remainder", "setBit",
        "shiftLeft", "shiftRight", "signum", "subtract", "testBit", "toByteArray", "toString",
        "xor"}));
    return expectedNames;
  }

  private List<String> getMethodNames(CtMethod[] methods) {
    List<String> methodNames = new ArrayList<String>();
    for (CtMethod method : methods) {
      methodNames.add(method.getName());
    }
    return methodNames;
  }

  private List<String> getClassNames(List<GeneratedClassFile> classes) {
    List<String> classNames = new ArrayList<String>();
    for (GeneratedClassFile clazz : classes) {
      classNames.add(clazz.getClassName());
    }
    return classNames;
  }

  private List<String> getExpectedSignaturesForBigIntegerClass() {
    List<String> expectedNames = new ArrayList<String>();
    expectedNames.addAll(Arrays.asList(new String[] {
        "public int java.math.BigInteger.getLowestSetBit()",
        "public java.math.BigInteger java.math.BigInteger.abs()",
        "protected void java.lang.Object.finalize() throws java.lang.Throwable",
        "public java.math.BigInteger java.math.BigInteger.modPow(java.math.BigInteger,"
            + "java.math.BigInteger)",
        "protected native java.lang.Object java.lang.Object.clone() throws "
            + "java.lang.CloneNotSupportedException",
        "public java.math.BigInteger java.math.BigInteger.setBit(int)",
        "public java.math.BigInteger java.math.BigInteger.shiftRight(int)",
        "public int java.math.BigInteger.bitLength()",
        "public java.math.BigInteger java.math.BigInteger.not()",
        "public java.math.BigInteger java.math.BigInteger.subtract(java.math.BigInteger)",
        "public java.math.BigInteger java.math.BigInteger.flipBit(int)",
        "public boolean java.math.BigInteger.isProbablePrime(int)",
        "public java.math.BigInteger java.math.BigInteger.add(java.math.BigInteger)",
        "public java.math.BigInteger java.math.BigInteger.modInverse(java.math.BigInteger)",
        "public java.math.BigInteger java.math.BigInteger.clearBit(int)",
        "public java.math.BigInteger java.math.BigInteger.multiply(java.math.BigInteger)",
        "public byte java.lang.Number.byteValue()",
        "public java.math.BigInteger java.math.BigInteger.gcd(java.math.BigInteger)",
        "public float java.math.BigInteger.floatValue()",
        "public java.lang.String java.math.BigInteger.toString(int)",
        "public java.math.BigInteger java.math.BigInteger.min(java.math.BigInteger)",
        "public int java.math.BigInteger.intValue()",
        "public java.math.BigInteger java.math.BigInteger.or(java.math.BigInteger)",
        "public java.math.BigInteger java.math.BigInteger.remainder(java.math.BigInteger)",
        "public java.math.BigInteger java.math.BigInteger.divide(java.math.BigInteger)",
        "public java.math.BigInteger java.math.BigInteger.xor(java.math.BigInteger)",
        "public java.math.BigInteger java.math.BigInteger.and(java.math.BigInteger)",
        "public int java.math.BigInteger.signum()",
        "public java.math.BigInteger[] java.math.BigInteger.divideAndRemainder("
            + "java.math.BigInteger)",
        "public java.math.BigInteger java.math.BigInteger.max(java.math.BigInteger)",
        "public java.math.BigInteger java.math.BigInteger.shiftLeft(int)",
        "public double java.math.BigInteger.doubleValue()",
        "public java.math.BigInteger java.math.BigInteger.pow(int)",
        "public short java.lang.Number.shortValue()",
        "public java.math.BigInteger java.math.BigInteger.andNot(java.math.BigInteger)",
        "public byte[] java.math.BigInteger.toByteArray()",
        "public java.math.BigInteger java.math.BigInteger.negate()",
        "public int java.math.BigInteger.compareTo(java.math.BigInteger)",
        "public boolean java.math.BigInteger.testBit(int)",
        "public int java.math.BigInteger.bitCount()",
        "public long java.math.BigInteger.longValue()",
        "public java.math.BigInteger java.math.BigInteger.mod(java.math.BigInteger)",
        "public java.math.BigInteger java.math.BigInteger.nextProbablePrime()",
        }));
    return expectedNames;
  }

  private List<String> getMethodSignatures(Method[] methods) {
    List<String> methodSignatures = new ArrayList<String>();
    for (Method method : methods) {
      if (getAndroidMockGenerator().isMockable(method)) {
        methodSignatures.add(method.toGenericString());
      }
    }
    return methodSignatures;
  }

  public void testIsSupportedType() {
    Class<?>[] unsupportedClasses =
        new Class[] {ClassIsAnnotation.class, ClassIsEnum.class, ClassIsFinal.class,
            ClassIsInterface.class};
    Class<?>[] supportedClasses = new Class[] {Object.class};

    for (Class<?> clazz : unsupportedClasses) {
      assertFalse(getAndroidMockGenerator().classIsSupportedType(clazz));
    }
    for (Class<?> clazz : supportedClasses) {
      assertTrue(getAndroidMockGenerator().classIsSupportedType(clazz));
    }
  }

  public void testGetDelegateFieldName() {
    assertEquals("delegateMockObject", getAndroidMockGenerator().getDelegateFieldName());
  }

  public void testGetInterfaceMethodSource() throws SecurityException, NoSuchMethodException {
    Method method = Object.class.getMethod("equals", Object.class);
    assertEquals("public boolean equals(java.lang.Object arg0);", getAndroidMockGenerator()
        .getInterfaceMethodSource(method));
  }

  public void testGetInterfaceMethodSourceMultipleExceptions() throws SecurityException,
      NoSuchMethodException {
    Method method = Class.class.getDeclaredMethod("newInstance");
    assertEquals("public java.lang.Object newInstance() throws java.lang.InstantiationException,"
        + "java.lang.IllegalAccessException;", getAndroidMockGenerator().getInterfaceMethodSource(
        method));
  }

  public void testGetInterfaceMethodSourceProtectedMethod() throws SecurityException,
      NoSuchMethodException {
    Method method = Object.class.getDeclaredMethod("finalize");
    assertEquals("public void finalize() throws java.lang.Throwable;", getAndroidMockGenerator()
        .getInterfaceMethodSource(method));
  }

  public void testGetInterfaceMethodSourceNoParams() throws SecurityException,
      NoSuchMethodException {
    Method method = Object.class.getMethod("toString");
    assertEquals("public java.lang.String toString();", getAndroidMockGenerator()
        .getInterfaceMethodSource(method));
  }

  public void testGetInterfaceMethodSourceVoidReturn() throws SecurityException,
      NoSuchMethodException {
    Method method = Thread.class.getMethod("run");
    assertEquals("public void run();", getAndroidMockGenerator().getInterfaceMethodSource(method));
  }

  public void testGetInterfaceMethodSourceFinal() throws SecurityException, NoSuchMethodException {
    Method method = Object.class.getMethod("notify");
    try {
      getAndroidMockGenerator().getInterfaceMethodSource(method);
      fail("Exception not thrown on a final method");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  public void testGetInterfaceMethodSourceStatic() throws SecurityException, NoSuchMethodException {
    Method method = Thread.class.getMethod("currentThread");
    try {
      getAndroidMockGenerator().getInterfaceMethodSource(method);
      fail("Exception not thrown on a static method");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  public void testGetInterfaceName() {
    AndroidMockGenerator r = getAndroidMockGenerator();
    assertEquals("genmocks.java.lang.ObjectDelegateInterface",
        FileUtils.getInterfaceNameFor(Object.class, SdkVersion.UNKNOWN));
  }

  public void testGetSubclassName() {
    AndroidMockGenerator r = getAndroidMockGenerator();
    assertEquals("genmocks.java.lang.ObjectDelegateSubclass",
        FileUtils.getSubclassNameFor(Object.class, SdkVersion.UNKNOWN));
  }

  public void testGetDelegateMethodSource() throws SecurityException, NoSuchMethodException {
    Method method = Object.class.getMethod("equals", Object.class);
    assertEquals("public boolean equals(java.lang.Object arg0){if(this.delegateMockObject==null){"
        + "return false;}return this.delegateMockObject.equals(arg0);}", getAndroidMockGenerator()
        .getDelegateMethodSource(method));
  }

  public void testGetDelegateMethodSourceAllTypes() throws SecurityException,
      NoSuchMethodException {
    String[] returnTypes =
        new String[] {"boolean", "byte", "short", "int", "long", "char", "float", "double"};
    String[] castTypes =
        new String[] {"false", "(byte)0", "(short)0", "(int)0", "(long)0", "(char)0", "(float)0",
            "(double)0"};
    for (int i = 0; i < returnTypes.length; ++i) {
      Method method = AllTypes.class.getMethod(returnTypes[i] + "Foo");
      assertEquals("public " + returnTypes[i] + " " + returnTypes[i]
          + "Foo(){if(this.delegateMockObject==null){return " + castTypes[i]
          + ";}return this.delegateMockObject." + returnTypes[i] + "Foo();}",
          getAndroidMockGenerator().getDelegateMethodSource(method));
    }
    Method method = AllTypes.class.getMethod("objectFoo");
    assertEquals("public java.lang.Object objectFoo(){if(this.delegateMockObject==null){return "
        + "null;}return this.delegateMockObject.objectFoo();}", getAndroidMockGenerator()
        .getDelegateMethodSource(method));
    method = AllTypes.class.getMethod("voidFoo");
    assertEquals("public void voidFoo(){if(this.delegateMockObject==null){return ;"
        + "}this.delegateMockObject.voidFoo();}", getAndroidMockGenerator()
        .getDelegateMethodSource(method));
  }

  private class AllTypes {
    @SuppressWarnings("unused")
    public void voidFoo() {
    }

    @SuppressWarnings("unused")
    public boolean booleanFoo() {
      return false;
    }

    @SuppressWarnings("unused")
    public byte byteFoo() {
      return 0;
    }

    @SuppressWarnings("unused")
    public short shortFoo() {
      return 0;
    }

    @SuppressWarnings("unused")
    public int intFoo() {
      return 0;
    }

    @SuppressWarnings("unused")
    public long longFoo() {
      return 0;
    }

    @SuppressWarnings("unused")
    public char charFoo() {
      return 0;
    }

    @SuppressWarnings("unused")
    public float floatFoo() {
      return 0;
    }

    @SuppressWarnings("unused")
    public double doubleFoo() {
      return 0;
    }

    @SuppressWarnings("unused")
    public Object objectFoo() {
      return null;
    }
  }

  public void testGetDelegateMethodSourceMultipleExceptions() throws SecurityException,
      NoSuchMethodException {
    Method method = Class.class.getDeclaredMethod("newInstance");
    assertEquals(
        "public java.lang.Object newInstance() throws java.lang.InstantiationException,"
            + "java.lang.IllegalAccessException{if(this.delegateMockObject==null){return null;}"
            + "return this.delegateMockObject.newInstance();}", getAndroidMockGenerator()
            .getDelegateMethodSource(method));
  }

  public void testGetDelegateMethodSourceProtectedMethod() throws SecurityException,
      NoSuchMethodException {
    Method method = Object.class.getDeclaredMethod("finalize");
    assertEquals("public void finalize() throws java.lang.Throwable{if(this.delegateMockObject=="
        + "null){return ;}this.delegateMockObject.finalize();}", getAndroidMockGenerator()
        .getDelegateMethodSource(method));
  }

  public void testGetDelegateMethodSourceMultiParams() throws SecurityException,
      NoSuchMethodException {
    Method method =
        String.class.getMethod("getChars", Integer.TYPE, Integer.TYPE, char[].class, Integer.TYPE);
    assertEquals(
        "public void getChars(int arg0,int arg1,char[] arg2,int arg3){if(this."
            + "delegateMockObject==null){return ;}this.delegateMockObject.getChars(arg0,arg1,arg2,"
            + "arg3);}", getAndroidMockGenerator().getDelegateMethodSource(method));
  }

  public void testGetDelegateMethodSourceNoParams() throws SecurityException,
      NoSuchMethodException {
    Method method = Object.class.getMethod("toString");
    assertEquals(
        "public java.lang.String toString(){if(this.delegateMockObject==null){return null;"
            + "}return this.delegateMockObject.toString();}", getAndroidMockGenerator()
            .getDelegateMethodSource(method));
  }

  public void testGetDelegateMethodSourceVoidReturn() throws SecurityException,
      NoSuchMethodException {
    Method method = Thread.class.getMethod("run");
    assertEquals("public void run(){if(this.delegateMockObject==null){return ;}this."
        + "delegateMockObject.run();}", getAndroidMockGenerator().getDelegateMethodSource(method));
  }

  public void testGetDelegateMethodSourceFinal() throws SecurityException, NoSuchMethodException {
    Method method = Object.class.getMethod("notify");
    try {
      getAndroidMockGenerator().getDelegateMethodSource(method);
      fail("Exception not thrown on a final method");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  public void testGetDelegateMethodSourceStatic() throws SecurityException, NoSuchMethodException {
    Method method = Thread.class.getMethod("currentThread");
    try {
      getAndroidMockGenerator().getDelegateMethodSource(method);
      fail("Exception not thrown on a static method");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  public void testGenerateEmptySubclass() throws ClassNotFoundException, NotFoundException {
    AndroidMockGenerator mockGenerator = getAndroidMockGenerator();
    CtClass generatedInterface = mockGenerator.generateInterface(String.class, SdkVersion.UNKNOWN);
    CtClass generatedClass = getAndroidMockGenerator().generateSkeletalClass(
        String.class, generatedInterface, SdkVersion.UNKNOWN);

    assertEquals("genmocks.java.lang", generatedClass.getPackageName());
    assertEquals("StringDelegateSubclass", generatedClass.getSimpleName());
    assertEquals("java.lang.String", generatedClass.getSuperclass().getName());
    cleanupGeneratedClasses(generatedInterface, generatedClass);
  }

  public void testAddMethods() throws ClassNotFoundException {
    AndroidMockGenerator mockGenerator = getAndroidMockGenerator();
    CtClass generatedInterface = mockGenerator.generateInterface(Number.class, SdkVersion.UNKNOWN);
    CtClass generatedClass =
        mockGenerator.generateSkeletalClass(Number.class, generatedInterface, SdkVersion.UNKNOWN);

    mockGenerator.addMethods(Number.class, generatedClass);

    List<String> expectedNames = getExpectedNamesForNumberClass();
    List<String> actualNames = getMethodNames(generatedClass.getDeclaredMethods());
    assertUnorderedContentsSame(expectedNames, actualNames);
    cleanupGeneratedClasses(generatedInterface, generatedClass);
  }

  public void testAddMethodsObjectClass() throws ClassNotFoundException {
    AndroidMockGenerator mockGenerator = getAndroidMockGenerator();
    CtClass generatedInterface = mockGenerator.generateInterface(Object.class, SdkVersion.UNKNOWN);
    CtClass generatedClass =
        mockGenerator.generateSkeletalClass(Object.class, generatedInterface, SdkVersion.UNKNOWN);

    mockGenerator.addMethods(Object.class, generatedClass);

    List<String> expectedNames = getExpectedNamesForObjectClass();
    List<String> actualNames = getMethodNames(generatedClass.getDeclaredMethods());
    assertUnorderedContentsSame(expectedNames, actualNames);
    cleanupGeneratedClasses(generatedInterface, generatedClass);
  }

  public void testAddMethodsUsesSuperclass() throws ClassNotFoundException {
    AndroidMockGenerator mockGenerator = getAndroidMockGenerator();
    CtClass generatedInterface = mockGenerator.generateInterface(
        BigInteger.class, SdkVersion.UNKNOWN);
    CtClass generatedClass = mockGenerator.generateSkeletalClass(
        BigInteger.class, generatedInterface, SdkVersion.UNKNOWN);

    mockGenerator.addMethods(BigInteger.class, generatedClass);

    List<String> expectedNames = getExpectedNamesForBigIntegerClass();
    List<String> actualNames = getMethodNames(generatedClass.getDeclaredMethods());
    assertUnorderedContentsSame(expectedNames, actualNames);
    cleanupGeneratedClasses(generatedInterface, generatedClass);
  }

  public void testGetAllMethods() throws ClassNotFoundException {
    AndroidMockGenerator mockGenerator = getAndroidMockGenerator();
    CtClass generatedInterface = mockGenerator.generateInterface(
        BigInteger.class, SdkVersion.UNKNOWN);
    CtClass generatedClass = mockGenerator.generateSkeletalClass(
        BigInteger.class, generatedInterface, SdkVersion.UNKNOWN);

    Method[] methods = mockGenerator.getAllMethods(BigInteger.class);

    List<String> expectedNames = getExpectedSignaturesForBigIntegerClass();
    List<String> actualNames = getMethodSignatures(methods);
    assertUnorderedContentsSame(expectedNames, actualNames);
    cleanupGeneratedClasses(generatedInterface, generatedClass);
  }

  public void testGenerateInterface() {
    AndroidMockGenerator mockGenerator = getAndroidMockGenerator();
    CtClass generatedInterface = mockGenerator.generateInterface(Number.class, SdkVersion.UNKNOWN);

    List<String> expectedNames = getExpectedNamesForNumberClass();
    List<String> actualNames = getMethodNames(generatedInterface.getDeclaredMethods());
    assertUnorderedContentsSame(expectedNames, actualNames);
    cleanupGeneratedClasses(generatedInterface);
  }

  public void testAddInterfaceMethods() {
    AndroidMockGenerator mockGenerator = getAndroidMockGenerator();
    CtClass generatedInterface = AndroidMockGenerator.getClassPool().makeInterface("testInterface");

    mockGenerator.addInterfaceMethods(Number.class, generatedInterface);

    List<String> expectedNames = getExpectedNamesForNumberClass();
    List<String> actualNames = getMethodNames(generatedInterface.getDeclaredMethods());
    assertUnorderedContentsSame(expectedNames, actualNames);
    cleanupGeneratedClasses(generatedInterface);
  }

  public void testGenerateSubclass() throws ClassNotFoundException {
    AndroidMockGenerator mockGenerator = getAndroidMockGenerator();
    CtClass generatedInterface = mockGenerator.generateInterface(Number.class, SdkVersion.UNKNOWN);

    CtClass generatedClass =
        mockGenerator.generateSubClass(Number.class, generatedInterface, SdkVersion.UNKNOWN);

    List<String> expectedNames = getExpectedNamesForNumberClass(true);
    List<String> actualNames = getMethodNames(generatedClass.getDeclaredMethods());
    assertUnorderedContentsSame(expectedNames, actualNames);
    cleanupGeneratedClasses(generatedInterface, generatedClass);
  }

  public void testCreateMockForClass() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    NoFileAndroidMockGenerator mockGenerator = getNoFileMockGenerator();
    List<GeneratedClassFile> classes = mockGenerator.createMocksForClass(Object.class);

    List<String> expectedNames = new ArrayList<String>();
    String subclassName = "genmocks.java.lang.ObjectDelegateSubclass";
    String interfaceName = "genmocks.java.lang.ObjectDelegateInterface";
    expectedNames.addAll(Arrays.asList(new String[] {subclassName,
        interfaceName}));
    List<String> actualNames = getClassNames(classes);
    assertUnorderedContentsSame(expectedNames, actualNames);
    cleanupGeneratedClasses(
        ClassPool.getDefault().get(subclassName),
        ClassPool.getDefault().get(interfaceName));
  }

  public void testGetSetDelegateMethodSource() {
    AndroidMockGenerator mockGenerator = getAndroidMockGenerator();
    CtClass generatedInterface = mockGenerator.generateInterface(Object.class, SdkVersion.UNKNOWN);
    String expectedSource =
        "public void setDelegate___AndroidMock(genmocks.java.lang.ObjectDelegateInterface obj) {"
            + " this.delegateMockObject = obj;}";

    assertEquals(expectedSource, mockGenerator.getSetDelegateMethodSource(generatedInterface));
  }

  public void testIsForbiddenMethod() throws SecurityException, NoSuchMethodException {
    Method[] forbiddenMethods =
        new Method[] {Object.class.getMethod("equals", Object.class),
            Object.class.getMethod("toString"), Object.class.getMethod("hashCode")};
    Method[] allowedMethods = new Method[] {BigInteger.class.getMethod("toString", Integer.TYPE)};
    for (Method method : forbiddenMethods) {
      assertTrue(getAndroidMockGenerator().isForbiddenMethod(method));
    }
    for (Method method : allowedMethods) {
      assertFalse(getAndroidMockGenerator().isForbiddenMethod(method));
    }
  }

  /**
   * Support test class for capturing the names of files that would have been
   * saved to a jar file.
   * 
   * @author swoodward@google.com (Stephen Woodward)
   */
  class NoFileAndroidMockGenerator extends AndroidMockGenerator {
    List<CtClass> savedClasses = new ArrayList<CtClass>();

    @Override
    void saveCtClass(CtClass clazz) {
      savedClasses.add(clazz);
    }
  }
}
