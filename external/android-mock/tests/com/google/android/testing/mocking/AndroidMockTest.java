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
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import junit.framework.TestCase;

import org.easymock.Capture;
import org.easymock.IAnswer;
import org.easymock.LogicalOperator;
import org.easymock.internal.matchers.Equals;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.Vector;


/**
 * Tests for the AndroidMock class.
 * 
 * @author swoodward@google.com (Stephen Woodward)
 */
public class AndroidMockTest extends TestCase {
  private List<String> notForwardedMethods =
      new ArrayList<String>(Arrays.asList(new String[] {
          "com.google.android.testing.mocking.AndroidMock.getInterfaceFor(java.lang.Class)",
          "com.google.android.testing.mocking.AndroidMock.getSubclassNameFor(java.lang.Class)",
          "com.google.android.testing.mocking.AndroidMock.getSubclassFor(java.lang.Class,"
              + "java.lang.Class,java.lang.Object)",
          "com.google.android.testing.mocking.AndroidMock.getInterfaceNameFor(java.lang.Class)",
          "com.google.android.testing.mocking.AndroidMock.createStrictMock("
              + "java.lang.Class,java.lang.Object[])",
          "com.google.android.testing.mocking.AndroidMock.createStrictMock("
              + "java.lang.String,java.lang.Class,java.lang.Object[])",
          "com.google.android.testing.mocking.AndroidMock.createMock("
              + "java.lang.Class,java.lang.Object[])",
          "com.google.android.testing.mocking.AndroidMock.createMock("
              + "java.lang.String,java.lang.Class,java.lang.Object[])",
          "com.google.android.testing.mocking.AndroidMock.createNiceMock("
              + "java.lang.Class,java.lang.Object[])",
          "com.google.android.testing.mocking.AndroidMock.createNiceMock("
              + "java.lang.String,java.lang.Class,java.lang.Object[])"}));

  private CtMethod[] getForwardedMethods() throws NotFoundException {
    List<CtMethod> methods =
        new ArrayList<CtMethod>(Arrays.asList(getAndroidMockCtClass().getDeclaredMethods()));
    // Get a copy for safe removal of elements during iteration.
    for (CtMethod method : Arrays.asList(methods.toArray(new CtMethod[0]))) {
      if (notForwardedMethods.contains(method.getLongName())
          || !Modifier.isPublic(method.getModifiers())) {
        methods.remove(method);
      }
    }
    return methods.toArray(new CtMethod[0]);
  }

  private CtClass getAndroidMockCtClass() throws NotFoundException {
    return ClassPool.getDefault().get("com.google.android.testing.mocking.AndroidMock");
  }

  private void compileClasses(List<GeneratedClassFile> mockClasses) throws NotFoundException {
    for (GeneratedClassFile clazz : mockClasses) {
      CtClass ctClass;
      ctClass = ClassPool.getDefault().get(clazz.getClassName());
      try {
        ctClass.toClass();
      } catch (CannotCompileException e) {
        // Just ignore -- this will happen for every class used in more than one test.
      }
    }
  }

  public void testIsUnboxableToPrimitiveAllPrimitives() {
    assertTrue(AndroidMock.isUnboxableToPrimitive(Integer.TYPE, new Integer(42), true));
    assertTrue(AndroidMock.isUnboxableToPrimitive(Long.TYPE, new Long(42L), true));
    assertTrue(AndroidMock.isUnboxableToPrimitive(Short.TYPE, new Short((short) 42), true));
    assertTrue(AndroidMock.isUnboxableToPrimitive(Byte.TYPE, new Byte((byte) 42), true));
    assertTrue(AndroidMock.isUnboxableToPrimitive(Boolean.TYPE, Boolean.TRUE, true));
    assertTrue(AndroidMock.isUnboxableToPrimitive(Float.TYPE, new Float(42.0f), true));
    assertTrue(AndroidMock.isUnboxableToPrimitive(Double.TYPE, new Double(42.0), true));
    assertTrue(AndroidMock.isUnboxableToPrimitive(Character.TYPE, new Character('a'), true));

    assertTrue(AndroidMock.isUnboxableToPrimitive(Integer.TYPE, 42, true));
    assertTrue(AndroidMock.isUnboxableToPrimitive(Long.TYPE, 42L, true));
    assertTrue(AndroidMock.isUnboxableToPrimitive(Short.TYPE, (short) 42, true));
    assertTrue(AndroidMock.isUnboxableToPrimitive(Byte.TYPE, (byte) 42, true));
    assertTrue(AndroidMock.isUnboxableToPrimitive(Boolean.TYPE, true, true));
    assertTrue(AndroidMock.isUnboxableToPrimitive(Float.TYPE, 42.0f, true));
    assertTrue(AndroidMock.isUnboxableToPrimitive(Double.TYPE, 42.0, true));
    assertTrue(AndroidMock.isUnboxableToPrimitive(Character.TYPE, 'a', true));
  }

  public void testIsUnboxableToPrimitiveIsObject() {
    assertFalse(AndroidMock.isUnboxableToPrimitive(Integer.TYPE, new Object(), false));
  }

  public void testIsUnboxableToPrimitiveAllWideningPrimitives() {
    Object[] testValues =
        new Object[] {new Byte((byte) 42), new Short((short) 42), new Integer(42), new Long(42L),
            new Float(42.0f), new Double(42.0), new Character('a'), Boolean.TRUE};
    boolean[] byteExpected = new boolean[] {true, false, false, false, false, false, false, false};
    boolean[] shortExpected = new boolean[] {true, true, false, false, false, false, true, false};
    boolean[] intExpected = new boolean[] {true, true, true, false, false, false, true, false};
    boolean[] longExpected = new boolean[] {true, true, true, true, false, false, true, false};
    boolean[] floatExpected = new boolean[] {true, true, true, false, true, false, true, false};
    boolean[] doubleExpected = new boolean[] {true, true, true, true, true, true, true, false};
    boolean[] charExpected = new boolean[] {true, true, true, false, false, false, true, false};
    boolean[] booleanExpected =
        new boolean[] {false, false, false, false, false, false, false, true};

    for (int i = 0; i < testValues.length; ++i) {
      assertEquals("Convert byte from " + testValues[i].getClass(), byteExpected[i], AndroidMock
          .isUnboxableToPrimitive(Byte.TYPE, testValues[i], false));
      assertEquals("Convert short from " + testValues[i].getClass(), shortExpected[i], AndroidMock
          .isUnboxableToPrimitive(Short.TYPE, testValues[i], false));
      assertEquals("Convert int from " + testValues[i].getClass(), intExpected[i], AndroidMock
          .isUnboxableToPrimitive(Integer.TYPE, testValues[i], false));
      assertEquals("Convert long from " + testValues[i].getClass(), longExpected[i], AndroidMock
          .isUnboxableToPrimitive(Long.TYPE, testValues[i], false));
      assertEquals("Convert float from " + testValues[i].getClass(), floatExpected[i], AndroidMock
          .isUnboxableToPrimitive(Float.TYPE, testValues[i], false));
      assertEquals("Convert double from " + testValues[i].getClass(), doubleExpected[i],
          AndroidMock.isUnboxableToPrimitive(Double.TYPE, testValues[i], false));
      assertEquals("Convert char from " + testValues[i].getClass(), charExpected[i], AndroidMock
          .isUnboxableToPrimitive(Character.TYPE, testValues[i], false));
      assertEquals("Convert boolean from " + testValues[i].getClass(), booleanExpected[i],
          AndroidMock.isUnboxableToPrimitive(Boolean.TYPE, testValues[i], false));
    }
  }


  public void testIsUnboxableToPrimitiveNotPrimitive() {
    try {
      AndroidMock.isUnboxableToPrimitive(Object.class, Object.class, false);
      fail("Exception should have been thrown");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void testCreateMock() throws ClassNotFoundException, IOException, CannotCompileException,
      NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Vector.class);
    compileClasses(mockClasses);
    Vector<String> mockVector = AndroidMock.createMock(Vector.class);
    AndroidMock.expect(mockVector.get(0)).andReturn("Hello World");
    AndroidMock.replay(mockVector);
    assertEquals("Hello World", mockVector.get(0).toString());
    AndroidMock.verify(mockVector);
  }

  public void testCreateMockUsingParameters() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(SimpleTimeZone.class);
    compileClasses(mockClasses);
    SimpleTimeZone mockTimeZone = AndroidMock.createMock(SimpleTimeZone.class, 0, "GMT");
    AndroidMock.expect(mockTimeZone.getRawOffset()).andReturn(42);
    AndroidMock.replay(mockTimeZone);
    assertEquals(42, mockTimeZone.getRawOffset());
    AndroidMock.verify(mockTimeZone);
  }

  public void testCreateMockUsingProtectedConstructors() throws ClassNotFoundException,
      IOException, CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Calendar.class);
    compileClasses(mockClasses);
    Calendar mockCalendar = AndroidMock.createMock(Calendar.class);
    AndroidMock.expect(mockCalendar.getGreatestMinimum(1)).andReturn(42);
    AndroidMock.replay(mockCalendar);
    assertEquals(42, mockCalendar.getGreatestMinimum(1));
    AndroidMock.verify(mockCalendar);

    // Just don't explode
    Calendar newMockCalendar =
        AndroidMock.createMock(Calendar.class, new SimpleTimeZone(1, "GMT"), Locale.UK);
  }

  public void testCreateMockUsingCastableParameters() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(SimpleTimeZone.class);
    compileClasses(mockClasses);
    SimpleTimeZone mockTimeZone = AndroidMock.createMock(SimpleTimeZone.class, 'a', "GMT");
    AndroidMock.expect(mockTimeZone.getRawOffset()).andReturn(42);
    AndroidMock.replay(mockTimeZone);
    assertEquals(42, mockTimeZone.getRawOffset());
    AndroidMock.verify(mockTimeZone);
  }

  public void testCreateMockUsingUnusableParameters() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(SimpleTimeZone.class);
    compileClasses(mockClasses);
    try {
      SimpleTimeZone mockTimeZone = AndroidMock.createMock(SimpleTimeZone.class, "GMT");
      fail("Excepted an IllegalArgumentException for incorrect number of constructor parameters");
    } catch (IllegalArgumentException e) {
      // Expected
    }
    try {
      SimpleTimeZone mockTimeZone = AndroidMock.createMock(SimpleTimeZone.class, 0, null);
      fail("Excepted an IllegalArgumentException for indeterminate null constructor parameters");
    } catch (IllegalArgumentException e) {
      // Expected
    }
    try {
      SimpleTimeZone mockTimeZone = AndroidMock.createMock(SimpleTimeZone.class, 0, new Object());
      fail("Excepted an IllegalArgumentException for incorrect constructor parameters");
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  public void testCreateMockUsingInterface() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Map.class);
    compileClasses(mockClasses);
    Map<String, String> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get("key")).andReturn("Hello World");
    AndroidMock.replay(mockMap);
    assertEquals("Hello World", mockMap.get("key"));
    AndroidMock.verify(mockMap);
  }

  public void testCreateMockUsingClass() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Vector.class);
    compileClasses(mockClasses);
    Vector<String> mockVector = AndroidMock.createMock(Vector.class);
    AndroidMock.expect(mockVector.get(0)).andReturn("Hello World");
    AndroidMock.replay(mockVector);
    assertEquals("Hello World", mockVector.get(0).toString());
    AndroidMock.verify(mockVector);
  }

  public void testCreateNiceMock() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Vector.class);
    compileClasses(mockClasses);
    Vector<String> mockVector = AndroidMock.createNiceMock(Vector.class);
    AndroidMock.expect(mockVector.get(0)).andReturn("Hello World");
    AndroidMock.replay(mockVector);
    assertEquals("Hello World", mockVector.get(0).toString());
    AndroidMock.verify(mockVector);
  }

  public void testCreateNiceMockUsingUnusableParameters() throws ClassNotFoundException,
      IOException, CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(SimpleTimeZone.class);
    compileClasses(mockClasses);
    try {
      SimpleTimeZone mockTimeZone = AndroidMock.createNiceMock(SimpleTimeZone.class, "GMT");
      fail("Excepted an IllegalArgumentException for incorrect number of constructor parameters");
    } catch (IllegalArgumentException e) {
      // Expected
    }
    try {
      SimpleTimeZone mockTimeZone = AndroidMock.createNiceMock(SimpleTimeZone.class, 0, null);
      fail("Excepted an IllegalArgumentException for indeterminate null constructor parameters");
    } catch (IllegalArgumentException e) {
      // Expected
    }
    try {
      SimpleTimeZone mockTimeZone =
          AndroidMock.createNiceMock(SimpleTimeZone.class, 0, new Object());
      fail("Excepted an IllegalArgumentException for incorrect constructor parameters");
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  public void testCreateNiceMockUsingParameters() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(SimpleTimeZone.class);

    compileClasses(mockClasses);
    SimpleTimeZone mockTimeZone = AndroidMock.createNiceMock(SimpleTimeZone.class, 0, "GMT");
    AndroidMock.expect(mockTimeZone.getRawOffset()).andReturn(42);
    AndroidMock.replay(mockTimeZone);
    assertEquals(42, mockTimeZone.getRawOffset());
    AndroidMock.verify(mockTimeZone);
  }

  public void testCreateNiceMockUsingCastableParameters() throws ClassNotFoundException,
      IOException, CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(SimpleTimeZone.class);

    compileClasses(mockClasses);
    SimpleTimeZone mockTimeZone = AndroidMock.createNiceMock(SimpleTimeZone.class, 'a', "GMT");
    AndroidMock.expect(mockTimeZone.getRawOffset()).andReturn(42);
    AndroidMock.replay(mockTimeZone);
    assertEquals(42, mockTimeZone.getRawOffset());
    AndroidMock.verify(mockTimeZone);
  }

  public void testCreateNiceMockUsingInterface() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Map.class);

    compileClasses(mockClasses);
    Map<String, String> mockMap = AndroidMock.createNiceMock(Map.class);
    AndroidMock.expect(mockMap.get("key")).andReturn("Hello World");
    AndroidMock.replay(mockMap);
    assertEquals("Hello World", mockMap.get("key"));
    AndroidMock.verify(mockMap);
  }

  public void testCreateNiceMockUsingClass() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Vector.class);

    compileClasses(mockClasses);
    Vector<String> mockVector = AndroidMock.createNiceMock(Vector.class);
    AndroidMock.expect(mockVector.get(0)).andReturn("Hello World");
    AndroidMock.replay(mockVector);
    assertEquals("Hello World", mockVector.get(0).toString());
    AndroidMock.verify(mockVector);
  }

  public void testCreateStrictMock() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Vector.class);

    compileClasses(mockClasses);
    Vector<String> mockVector = AndroidMock.createStrictMock(Vector.class);
    AndroidMock.expect(mockVector.get(0)).andReturn("Hello World");
    AndroidMock.replay(mockVector);
    assertEquals("Hello World", mockVector.get(0).toString());
    AndroidMock.verify(mockVector);
  }

  public void testCreateStrictMockUsingUnusableParameters() throws ClassNotFoundException,
      IOException, CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(SimpleTimeZone.class);

    compileClasses(mockClasses);
    try {
      SimpleTimeZone mockTimeZone = AndroidMock.createStrictMock(SimpleTimeZone.class, "GMT");
      fail("Excepted an IllegalArgumentException for incorrect number of constructor parameters");
    } catch (IllegalArgumentException e) {
      // Expected
    }
    try {
      SimpleTimeZone mockTimeZone = AndroidMock.createStrictMock(SimpleTimeZone.class, 0, null);
      fail("Excepted an IllegalArgumentException for indeterminate null constructor parameters");
    } catch (IllegalArgumentException e) {
      // Expected
    }
    try {
      SimpleTimeZone mockTimeZone =
          AndroidMock.createStrictMock(SimpleTimeZone.class, 0, new Object());
      fail("Excepted an IllegalArgumentException for incorrect constructor parameters");
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  public void testCreateStrictMockUsingParameters() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(SimpleTimeZone.class);

    compileClasses(mockClasses);
    SimpleTimeZone mockTimeZone = AndroidMock.createStrictMock(SimpleTimeZone.class, 0, "GMT");
    AndroidMock.expect(mockTimeZone.getRawOffset()).andReturn(42);
    AndroidMock.replay(mockTimeZone);
    assertEquals(42, mockTimeZone.getRawOffset());
    AndroidMock.verify(mockTimeZone);
  }

  public void testCreateStrictMockUsingCastableParameters() throws ClassNotFoundException,
      IOException, CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(SimpleTimeZone.class);

    compileClasses(mockClasses);
    SimpleTimeZone mockTimeZone = AndroidMock.createStrictMock(SimpleTimeZone.class, 'a', "GMT");
    AndroidMock.expect(mockTimeZone.getRawOffset()).andReturn(42);
    AndroidMock.replay(mockTimeZone);
    assertEquals(42, mockTimeZone.getRawOffset());
    AndroidMock.verify(mockTimeZone);
  }

  public void testCreateStrictMockUsingInterface() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Map.class);

    compileClasses(mockClasses);
    Map<String, String> mockMap = AndroidMock.createStrictMock(Map.class);
    AndroidMock.expect(mockMap.get("key")).andReturn("Hello World");
    AndroidMock.replay(mockMap);
    assertEquals("Hello World", mockMap.get("key"));
    AndroidMock.verify(mockMap);
  }

  public void testCreateStrictMockUsingClass() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Vector.class);
    compileClasses(mockClasses);
    Vector<String> mockVector = AndroidMock.createStrictMock(Vector.class);
    AndroidMock.expect(mockVector.get(0)).andReturn("Hello World");
    AndroidMock.replay(mockVector);
    assertEquals("Hello World", mockVector.get(0).toString());
    AndroidMock.verify(mockVector);
  }

  public void testCreateMockConstructorDoesWorkOnAllReturnTypes() throws ClassNotFoundException,
      IOException, CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(ClassDoesWorkInConstructor.class);
    compileClasses(mockClasses);
    ClassDoesWorkInConstructor mock = AndroidMock.createMock(ClassDoesWorkInConstructor.class);
  }

  public void testAllForwardedMethods() throws CannotCompileException, NotFoundException {
    for (CtMethod method : getForwardedMethods()) {
      MethodVerifier verifier = new MethodVerifier(method);
      // CtMethod.instrument Causes every instruction in the method to be
      // inspected, and passed to
      // the MethodVerifier callback (extends javassist.expr.ExprEditor). We
      // want to verify that
      // the expected EasyMock method is called at least once in each
      // AndroidMock method.
      method.instrument(verifier);
      assertTrue(method.getLongName() + " not called.", verifier.expectedMethodCalled());
    }
  }

  public void testCheckOrder() throws ClassNotFoundException, IOException, CannotCompileException,
      NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Vector.class);
    compileClasses(mockClasses);
    Vector<?> mockVector = AndroidMock.createMock(Vector.class);
    AndroidMock.checkOrder(mockVector, false);
    AndroidMock.checkOrder(AndroidMock.createMock(Map.class), false);
  }

  public void testVerify() throws ClassNotFoundException, IOException, CannotCompileException,
      NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Vector.class);
    compileClasses(mockClasses);
    Vector<?> mockVector = AndroidMock.createMock(Vector.class);
    AndroidMock.replay(mockVector);
    AndroidMock.verify(mockVector);
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.replay(mockMap);
    AndroidMock.verify(mockMap);
  }

  public void testResetToStrict() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Vector.class);
    compileClasses(mockClasses);
    Vector<?> mockVector = AndroidMock.createMock(Vector.class);
    AndroidMock.resetToStrict(mockVector);
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.resetToStrict(mockMap);
  }

  public void testResetToDefault() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Vector.class);
    compileClasses(mockClasses);
    Vector<?> mockVector = AndroidMock.createMock(Vector.class);
    AndroidMock.resetToDefault(mockVector);
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.resetToDefault(mockMap);
  }

  public void testResetToNice() throws ClassNotFoundException, IOException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Vector.class);
    compileClasses(mockClasses);
    Vector<?> mockVector = AndroidMock.createMock(Vector.class);
    AndroidMock.resetToNice(mockVector);
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.resetToNice(mockMap);
  }

  public void testReset() throws ClassNotFoundException, IOException, CannotCompileException,
      NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Vector.class);
    compileClasses(mockClasses);
    Vector<?> mockVector = AndroidMock.createMock(Vector.class);
    AndroidMock.reset(mockVector);
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.reset(mockMap);

  }

  public void testReplay() throws ClassNotFoundException, IOException, CannotCompileException,
      NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(Vector.class);
    compileClasses(mockClasses);
    Vector<?> mockVector = AndroidMock.createMock(Vector.class);
    AndroidMock.replay(mockVector);
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.replay(mockMap);
  }

  public void testExpect() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    mockMap.clear();
    AndroidMock.expect(null);
    AndroidMock.replay(mockMap);
  }

  public void testExpectLastCall() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    mockMap.clear();
    AndroidMock.expectLastCall();
    AndroidMock.replay(mockMap);
  }

  public void testAnyBoolean() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.anyBoolean())).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testAnyByte() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.anyByte())).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testAnyChar() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.anyChar())).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testAnyInt() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.anyInt())).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testAnyLong() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.anyLong())).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testAnyFloat() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.anyFloat())).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testAnyDouble() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.anyDouble())).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testAnyShort() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.anyShort())).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testAnyObject() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.anyObject())).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testGeq() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.geq((byte) 0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.geq((short) 0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.geq(0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.geq(0L))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.geq(0.0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.geq(0.0f))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.geq("Hi"))).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testLeq() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.leq((byte) 0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.leq((short) 0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.leq(0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.leq(0L))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.leq(0.0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.leq(0.0f))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.leq("Hi"))).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testGt() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.gt((byte) 0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.gt((short) 0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.gt(0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.gt(0L))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.gt(0.0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.gt(0.0f))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.gt("Hi"))).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testLt() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.lt((byte) 0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.lt((short) 0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.lt(0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.lt(0L))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.lt(0.0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.lt(0.0f))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.lt("Hi"))).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testIsA() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.isA(String.class))).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testContains() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.contains("hi"))).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testAnd() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.and(AndroidMock.eq(true), AndroidMock.eq(true))))
        .andReturn(null);
    AndroidMock.expect(
        mockMap.get(AndroidMock.and(AndroidMock.eq((byte) 0), AndroidMock.eq((byte) 0))))
        .andReturn(null);
    AndroidMock.expect(
        mockMap.get(AndroidMock.and(AndroidMock.eq((short) 0), AndroidMock.eq((short) 0))))
        .andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.and(AndroidMock.eq(0), AndroidMock.eq(0))))
        .andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.and(AndroidMock.eq(0L), AndroidMock.eq(0L))))
        .andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.and(AndroidMock.eq(0.0), AndroidMock.eq(0.0))))
        .andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.and(AndroidMock.eq(0.0f), AndroidMock.eq(0.0f))))
        .andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.and(AndroidMock.eq("hi"), AndroidMock.eq("hi"))))
        .andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.and(AndroidMock.eq('a'), AndroidMock.eq('a'))))
        .andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testOr() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.or(AndroidMock.eq(true), AndroidMock.eq(true))))
        .andReturn(null);
    AndroidMock.expect(
        mockMap.get(AndroidMock.or(AndroidMock.eq((byte) 0), AndroidMock.eq((byte) 0))))
        .andReturn(null);
    AndroidMock.expect(
        mockMap.get(AndroidMock.or(AndroidMock.eq((short) 0), AndroidMock.eq((short) 0))))
        .andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.or(AndroidMock.eq(0), AndroidMock.eq(0))))
        .andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.or(AndroidMock.eq(0L), AndroidMock.eq(0L))))
        .andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.or(AndroidMock.eq(0.0), AndroidMock.eq(0.0))))
        .andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.or(AndroidMock.eq(0.0f), AndroidMock.eq(0.0f))))
        .andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.or(AndroidMock.eq("hi"), AndroidMock.eq("hi"))))
        .andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.or(AndroidMock.eq('a'), AndroidMock.eq('a'))))
        .andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testNot() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.not(AndroidMock.eq(true)))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.not(AndroidMock.eq((byte) 0)))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.not(AndroidMock.eq((short) 0)))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.not(AndroidMock.eq(0)))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.not(AndroidMock.eq(0L)))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.not(AndroidMock.eq(0.0)))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.not(AndroidMock.eq(0.0f)))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.not(AndroidMock.eq("hi")))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.not(AndroidMock.eq('a')))).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testEq() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.eq(true))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.eq((byte) 0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.eq((short) 0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.eq(0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.eq(0L))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.eq(0.0))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.eq(0.0f))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.eq(0.0, 0.1))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.eq(0.0f, 0.1f))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.eq("hi"))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.eq('a'))).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testAryEq() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.aryEq(new boolean[] {true}))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.aryEq(new byte[] {(byte) 0}))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.aryEq(new short[] {(short) 0}))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.aryEq(new int[] {0}))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.aryEq(new long[] {0L}))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.aryEq(new double[] {0.0}))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.aryEq(new float[] {0.0f}))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.aryEq(new String[] {"hi"}))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.aryEq(new char[] {'a'}))).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testIsNull() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.isNull())).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testNotNull() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.notNull())).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testFind() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.find("hi"))).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testMatches() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.matches("hi"))).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testStartsWith() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.startsWith("hi"))).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testEndsWith() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.endsWith("hi"))).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testSame() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.same("hi"))).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testCmpEq() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.cmpEq("hi"))).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testCmp() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(
        mockMap.get(AndroidMock.cmp("hi", String.CASE_INSENSITIVE_ORDER, LogicalOperator.EQUAL)))
        .andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testCapture() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(AndroidMock.capture(new Capture<Byte>()))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.capture(new Capture<Character>()))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.capture(new Capture<Double>()))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.capture(new Capture<Float>()))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.capture(new Capture<Integer>()))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.capture(new Capture<Long>()))).andReturn(null);
    AndroidMock.expect(mockMap.get(AndroidMock.capture(new Capture<String>()))).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testReportMatcher() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.reportMatcher(new Equals(null));
    AndroidMock.expect(mockMap.get(null)).andReturn(null);
    AndroidMock.replay(mockMap);
  }

  public void testGetCurrentArguments() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.expect(mockMap.get(null)).andAnswer(new IAnswer() {
      @Override
      public Object answer() {
        AndroidMock.getCurrentArguments();
        return null;
      }
    });
    AndroidMock.replay(mockMap);
    mockMap.get(null);
  }

  public void testMakeThreadSafe() {
    Map<?, ?> mockMap = AndroidMock.createMock(Map.class);
    AndroidMock.makeThreadSafe(mockMap, false);
    AndroidMock.replay(mockMap);
  }

  public void testAndThrowsOnMockedInterface() throws IOException {
    ObjectInput mockInStream = AndroidMock.createMock(ObjectInput.class);
    AndroidMock.expect(mockInStream.read()).andThrow(new IOException("foo"));
    AndroidMock.replay(mockInStream);
    try {
      mockInStream.read();
      fail("IOException not thrown");
    } catch (IOException e) {
      assertEquals("foo", e.getMessage());
    }
    AndroidMock.verify(mockInStream);
  }

  public void testAndThrowsOnMockedClass() throws IOException, ClassNotFoundException,
      CannotCompileException, NotFoundException {
    List<GeneratedClassFile> mockClasses =
        new AndroidMockGenerator().createMocksForClass(InputStream.class);
    compileClasses(mockClasses);
    InputStream mockInStream = AndroidMock.createMock(InputStream.class);
    AndroidMock.expect(mockInStream.read()).andThrow(new IOException("foo"));
    AndroidMock.replay(mockInStream);
    try {
      mockInStream.read();
      fail("IOException not thrown");
    } catch (IOException e) {
      assertEquals("foo", e.getMessage());
    }
    AndroidMock.verify(mockInStream);
  }

  /**
   * Used for testing that a given method on Android Mock calls the equivalent
   * method on EasyMock, to ensure that the method-wiring of Android Mock is
   * correct.
   * 
   * @author swoodward@google.com (Stephen Woodward)
   */
  class MethodVerifier extends ExprEditor {
    private CtMethod expectedMethod;
    private boolean methodCalled;

    MethodVerifier(CtMethod expectedMethod) {
      this.expectedMethod = expectedMethod;
    }

    @Override
    public void edit(MethodCall calledMethod) {
      try {
        methodCalled = methodCalled || expectedMethod.equals(calledMethod.getMethod());
      } catch (NotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    public boolean expectedMethodCalled() {
      return methodCalled;
    }
  }
}
