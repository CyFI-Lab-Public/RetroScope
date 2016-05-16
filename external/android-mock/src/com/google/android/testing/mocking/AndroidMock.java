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

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.easymock.IExpectationSetters;
import org.easymock.LogicalOperator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Android Mock is a wrapper for EasyMock (2.4) which allows for real Class mocking on
 * an Android (Dalvik) VM.
 * 
 * All methods on Android Mock are syntactically equivalent to EasyMock method
 * calls, and will delegate calls to EasyMock, while performing the required
 * transformations to avoid Dalvik VM troubles.
 * 
 * Calls directly to EasyMock will work correctly only if the Class being mocked
 * is in fact an Interface. Calls to Android Mock will work correctly for both
 * Interfaces and concrete Classes.
 * 
 * Android Mock requires that the code being mocked be instrumented prior to
 * loading to the Dalvik VM by having called the MockGenerator.jar file. Try
 * running {@code java -jar MockGenerator.jar --help} for more information.
 * 
 * An example usage pattern is:
 * 
 * {@code &#64;UsesMocks(MyClass.class) public void testFoo() &#123; MyClass
 * mockObject = AndroidMock.createMock(MyClass.class);
 * AndroidMock.expect(mockObject.foo(0)).andReturn(42);
 * AndroidMock.replay(mockObject); assertEquals(42, mockObject.foo(0));
 * AndroidMock.verify(mockObject); &#125; * }
 * 
 * 
 * <b>A note about parameter and return types for the <i>expects</i> style of methods.</b>
 * The various expectation methods such as {@link #eq(boolean)}, {@link #and(boolean, boolean)},
 * and {@link #leq(byte)} all have nonsense return values. Each of the expectation methods may only
 * be executed under strict conditions (in order to set expectations of incoming method parameters
 * during record mode) and thus their return types are in fact never used. The return types are set
 * only to satisfy the compile-time parameter requirements of the methods being mocked in order to
 * allow code such as: {@code mockObject.doFoo(anyInt());}. If {@link #anyInt()} did not return
 * {@code int} then the compiler would not accept the preceding code fragment.
 * 
 * Similarly, the complex expectation methods ({@code #and}, {@code #or}, and {@code not}) take
 * various parameter types, but will throw an {@link java.lang.IllegalStateException} if anything
 * other than an expectation method is provided.  E.g. {@code mockObject.doFoo(and(gt(5), lt(10));}
 * 
 * The benefit of this is to make it very easy to read the test code after it has been written.
 * Additionally, the test code is protected by type safety at compile time.
 * 
 * The downside of this is that when writing the test code in the record phase, how to use the
 * expectation APIs is not made clear by the method signatures of these expectation methods. In
 * particular, it's not at all clear that {@link #and(byte, byte)} takes as parameters other
 * expectation methods, and not just any random method that returns a {@literal byte} or even a
 * {@literal byte} literal.
 * 
 * @author swoodward@google.com (Stephen Woodward)
 */
public class AndroidMock {
  private AndroidMock() {
  }

  /**
   * Creates a mock object for the specified class, order checking
   * is enabled by default. The difference between a strict mock and a normal mock is that a strict
   * mock will not allow for invocations of the mock object to occur other than in the exact order
   * specified during record mode.
   * 
   * The parameter {@literal args} allows the caller to choose which constructor on the Class
   * specified by {@literal toMock} to be called when constructing the Mock object. If a constructor
   * takes primitive values, Java Auto-boxing/unboxing will take care of it automatically, allowing
   * the caller to make calls such as {@literal createStrictMock(MyObject.class, 42, "hello!")},
   * where {@literal MyObject} defines a constructor such as
   * {@literal public MyObject(int answer, String greeting)}.
   * 
   * @param <T> the class type to be mocked.
   * @param toMock the class of the object to be mocked.
   * @param args the arguments to pass to the constructor.
   * @return the mock object.
   */
  public static <T> T createStrictMock(Class<T> toMock, Object... args) {
    return createStrictMock(null, toMock, args);
  }

  /**
   * Creates a mock object for the specified class, order checking
   * is enabled by default. The difference between a strict mock and a normal mock is that a strict
   * mock will not allow for invocations of the mock object to occur other than in the exact order
   * specified during record mode.
   * 
   * The parameter {@literal args} allows the caller to choose which constructor on the Class
   * specified by {@literal toMock} to be called when constructing the Mock object. If a constructor
   * takes primitive values, Java Auto-boxing/unboxing will take care of it automatically, allowing
   * the caller to make calls such as
   * {@literal createStrictMock("NameMyMock", MyObject.class, 42, "hello!")},
   * where {@literal MyObject} defines a constructor such as
   * {@literal public MyObject(int answer, String greeting)}.
   * 
   * @param <T> the class type to be mocked.
   * @param name the name of the mock object. This must be a valid Java identifier. This value is
   * used as the return value from {@link #toString()} when invoked on the mock object.
   * @param toMock the class of the object to be mocked.
   * @param args the arguments to pass to the constructor.
   * @return the mock object.
   * @throws IllegalArgumentException if the name is not a valid Java identifier.
   */
  @SuppressWarnings("cast")
  public static <T> T createStrictMock(String name, Class<T> toMock, Object... args) {
    if (toMock.isInterface()) {
      return EasyMock.createStrictMock(name, toMock);
    }
    Object mockedInterface = EasyMock.createStrictMock(name, getInterfaceFor(toMock));
    return (T) getSubclassFor(toMock, getInterfaceFor(toMock), mockedInterface, args);
  }

  /**
   * Creates a mock object for the specified class, order checking
   * is disabled by default. A normal mock with order checking disabled will allow you to record
   * the method invocations during record mode in any order. If order is important, use
   * {@link #createStrictMock(Class, Object...)} instead.
   * 
   * The parameter {@literal args} allows the caller to choose which constructor on the Class
   * specified by {@literal toMock} to be called when constructing the Mock object. If a constructor
   * takes primitive values, Java Auto-boxing/unboxing will take care of it automatically, allowing
   * the caller to make calls such as
   * {@literal createMock(MyObject.class, 42, "hello!")},
   * where {@literal MyObject} defines a constructor such as
   * {@literal public MyObject(int answer, String greeting)}.
   * 
   * @param <T> the type of the class to be mocked.
   * @param toMock the class object representing the class to be mocked.
   * @param args the arguments to pass to the constructor.
   * @return the mock object.
   */
  public static <T> T createMock(Class<T> toMock, Object... args) {
    return createMock(null, toMock, args);
  }

  /**
   * Creates a mock object for the specified class, order checking
   * is disabled by default. A normal mock with order checking disabled will allow you to record
   * the method invocations during record mode in any order. If order is important, use
   * {@link #createStrictMock(Class, Object...)} instead.
   * 
   * The parameter {@literal args} allows the caller to choose which constructor on the Class
   * specified by {@literal toMock} to be called when constructing the Mock object. If a constructor
   * takes primitive values, Java Auto-boxing/unboxing will take care of it automatically, allowing
   * the caller to make calls such as
   * {@literal createMock("NameMyMock", MyObject.class, 42, "hello!")},
   * where {@literal MyObject} defines a constructor such as
   * {@literal public MyObject(int answer, String greeting)}.
   * 
   * @param <T> the type of the class to be mocked.
   * @param name the name of the mock object. This must be a valid Java identifier. This value is
   * used as the return value from {@link #toString()} when invoked on the mock object.
   * @param toMock the class object representing the class to be mocked.
   * @param args the arguments to pass to the constructor.
   * @return the mock object.
   * @throws IllegalArgumentException if the name is not a valid Java identifier.
   */
  @SuppressWarnings("cast")
  public static <T> T createMock(String name, Class<T> toMock, Object... args) {
    if (toMock.isInterface()) {
      return EasyMock.createMock(name, toMock);
    }
    Object mockedInterface = EasyMock.createMock(name, getInterfaceFor(toMock));
    return (T) getSubclassFor(toMock, getInterfaceFor(toMock), mockedInterface, args);
  }

  /**
   * Creates a mock object for the specified class, order checking
   * is disabled by default, and the mock object will return {@code 0},
   * {@code null} or {@code false} for unexpected invocations.
   * 
   * The parameter {@literal args} allows the caller to choose which constructor on the Class
   * specified by {@literal toMock} to be called when constructing the Mock object. If a constructor
   * takes primitive values, Java Auto-boxing/unboxing will take care of it automatically, allowing
   * the caller to make calls such as
   * {@literal createNiceMock(MyObject.class, 42, "hello!")},
   * where {@literal MyObject} defines a constructor such as
   * {@literal public MyObject(int answer, String greeting)}.
   * 
   * @param <T> the type of the class to be mocked.
   * @param toMock the class object representing the class to be mocked.
   * @param args the arguments to pass to the constructor.
   * @return the mock object.
   */
  public static <T> T createNiceMock(Class<T> toMock, Object... args) {
    return createNiceMock(null, toMock, args);
  }

  /**
   * Creates a mock object for the specified class, order checking
   * is disabled by default, and the mock object will return {@code 0},
   * {@code null} or {@code false} for unexpected invocations.
   * 
   * The parameter {@literal args} allows the caller to choose which constructor on the Class
   * specified by {@literal toMock} to be called when constructing the Mock object. If a constructor
   * takes primitive values, Java Auto-boxing/unboxing will take care of it automatically, allowing
   * the caller to make calls such as
   * {@literal createNiceMock("NameMyMock", MyObject.class, 42, "hello!")},
   * where {@literal MyObject} defines a constructor such as
   * {@literal public MyObject(int answer, String greeting)}.
   * 
   * @param <T> the type of the class to be mocked.
   * @param name the name of the mock object. This must be a valid Java identifier. This value is
   * used as the return value from {@link #toString()} when invoked on the mock object.
   * @param toMock the class object representing the class to be mocked.
   * @param args the arguments to pass to the constructor.
   * @throws IllegalArgumentException if the name is not a valid Java identifier.
   */
  @SuppressWarnings("cast")
  public static <T> T createNiceMock(String name, Class<T> toMock, Object... args) {
    if (toMock.isInterface()) {
      return EasyMock.createNiceMock(name, toMock);
    }
    Object mockedInterface = EasyMock.createNiceMock(name, getInterfaceFor(toMock));
    return (T) getSubclassFor(toMock, getInterfaceFor(toMock), mockedInterface, args);
  }

  /**
   * Returns the expectation setter for the last expected invocation in the current thread.
   * Expectation setters are used during the recording phase to specify what method calls
   * will be expected during the replay phase, and with which parameters. Parameters may be
   * specified as literal values (e.g. {@code expect(mock.foo(42));  expect(mock.foo("hello"));})
   * or according to parameter expectation criteria. Some examples of parameter expectation
   * criteria include {@link #anyObject()}, {@link #leq(int)}, {@link #contains(String)},
   * {@link #isA(Class)} and also the more complex {@link #and(char, char)},
   * {@link #or(boolean, boolean)}, and {@link #not(double)}.
   * 
   * An {@link org.easymock.IExpectationSetters} object has methods which allow you to define
   * the expected behaviour of the mocked method and the expected number of invocations,
   * e.g. {@link org.easymock.IExpectationSetters#andReturn(Object)},
   * {@link org.easymock.IExpectationSetters#andThrow(Throwable)}, and
   * {@link org.easymock.IExpectationSetters#atLeastOnce()}.
   * 
   * @param expectedValue the parameter is used to transport the type to the ExpectationSetter.
   * It allows writing the expected call as an argument,
   * e.g. {@code expect(mock.getName()).andReturn("John Doe")}.
   * @return the expectation setter.
   */
  public static <T> IExpectationSetters<T> expect(T expectedValue) {
    return EasyMock.expect(expectedValue);
  }

  /**
   * Returns the expectation setter for the last expected invocation in the
   * current thread. This method is used for expected invocations on void
   * methods. Use this for things such as
   * {@link org.easymock.IExpectationSetters#andThrow(Throwable)}
   * on void methods.
   * E.g.
   * {@code mock.doFoo();
   * AndroidMock.expectLastCall().andThrow(new IllegalStateException());}
   * 
   * @see #expect(Object) for more details about {@link org.easymock.IExpectationSetters}
   * @return the expectation setter.
   */
  public static <T> IExpectationSetters<T> expectLastCall() {
    return EasyMock.expectLastCall();
  }

  /**
   * Expects any {@code boolean} argument as a parameter to a mocked method.
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.anyBoolean())).andReturn("hello world");}
   * 
   * @return {@code false}. The return value is always ignored.
   */
  public static boolean anyBoolean() {
    return EasyMock.anyBoolean();
  }

  /**
   * Expects any {@code byte} argument as a parameter to a mocked method.
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.anyByte())).andReturn("hello world");}
   * 
   * @return {@code 0}. The return value is always ignored.
   */
  public static byte anyByte() {
    return EasyMock.anyByte();
  }

  /**
   * Expects any {@code char} argument as a parameter to a mocked method.
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.anyChar())).andReturn("hello world");}
   * 
   * @return {@code 0}. The return value is always ignored.
   */
  public static char anyChar() {
    return EasyMock.anyChar();
  }

  /**
   * Expects any {@code int} argument as a parameter to a mocked method.
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.anyInt())).andReturn("hello world");}
   * 
   * @return {@code 0}. The return value is always ignored.
   */
  public static int anyInt() {
    return EasyMock.anyInt();
  }

  /**
   * Expects any {@code long} argument as a parameter to a mocked method.
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.anyLong())).andReturn("hello world");}
   * 
   * @return {@code 0}. The return value is always ignored.
   */
  public static long anyLong() {
    return EasyMock.anyLong();
  }

  /**
   * Expects any {@code float} argument as a parameter to a mocked method.
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.anyFloat())).andReturn("hello world");}
   * 
   * @return {@code 0}. The return value is always ignored.
   */
  public static float anyFloat() {
    return EasyMock.anyFloat();
  }

  /**
   * Expects any {@code double} argument as a parameter to a mocked method.
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.anyDouble())).andReturn("hello world");}
   * 
   * @return {@code 0}. The return value is always ignored.   */
  public static double anyDouble() {
    return EasyMock.anyDouble();
  }

  /**
   * Expects any {@code short} argument as a parameter to a mocked method.
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.anyShort())).andReturn("hello world");}
   * 
   * @return {@code 0}. The return value is always ignored.   */
  public static short anyShort() {
    return EasyMock.anyShort();
  }

  /**
   * Expects any {@code java.lang.Object} (or subclass) argument as a parameter to a mocked method.
   * Note that this includes Arrays (since an array {@literal is an Object})
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.anyLong())).andReturn("hello world");}
   * 
   * @return {@code 0}. The return value is always ignored.
   */
  @SuppressWarnings("unchecked")
  public static <T> T anyObject() {
    return (T) EasyMock.anyObject();
  }

  /**
   * Expects a {@code Comparable} argument greater than or equal to the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.geq("hi"))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be greater than or equal.
   * @return {@code null}. The return value is always ignored.
   */
  public static <T extends Comparable<T>> T geq(Comparable<T> expectedValue) {
    return EasyMock.geq(expectedValue);
  }

  /**
   * Expects a {@code byte} argument greater than or equal to the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.geq((byte)42))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be greater than or equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static byte geq(byte expectedValue) {
    return EasyMock.geq(expectedValue);
  }

  /**
   * Expects a {@code double} argument greater than or equal to the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.geq(42.0))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be greater than or equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static double geq(double expectedValue) {
    return EasyMock.geq(expectedValue);
  }

  /**
   * Expects a {@code float} argument greater than or equal to the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.geq(42.0f))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be greater than or equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static float geq(float expectedValue) {
    return EasyMock.geq(expectedValue);
  }

  /**
   * Expects an {@code int} argument greater than or equal to the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.geq(42))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be greater than or equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static int geq(int expectedValue) {
    return EasyMock.geq(expectedValue);
  }

  /**
   * Expects a {@code long} argument greater than or equal to the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.geq(42l))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be greater than or equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static long geq(long expectedValue) {
    return EasyMock.geq(expectedValue);
  }

  /**
   * Expects a {@code short} argument greater than or equal to the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.geq((short)42))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be greater than or equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static short geq(short expectedValue) {
    return EasyMock.geq(expectedValue);
  }

  /**
   * Expects a {@code Comparable} argument less than or equal to the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.leq("hi"))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be less than or equal.
   * @return {@code null}. The return value is always ignored.
   */
  public static <T extends Comparable<T>> T leq(Comparable<T> expectedValue) {
    return EasyMock.leq(expectedValue);
  }

  /**
   * Expects a {@code byte} argument less than or equal to the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.leq((byte)42))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be less than or equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static byte leq(byte expectedValue) {
    return EasyMock.leq(expectedValue);
  }

  /**
   * Expects a {@code double} argument less than or equal to the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.leq(42.0))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be less than or equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static double leq(double expectedValue) {
    return EasyMock.leq(expectedValue);
  }

  /**
   * Expects a {@code float} argument less than or equal to the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.leq(42.0f))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be less than or equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static float leq(float expectedValue) {
    return EasyMock.leq(expectedValue);
  }

  /**
   * Expects an {@code int} argument less than or equal to the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.leq(42))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be less than or equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static int leq(int expectedValue) {
    return EasyMock.leq(expectedValue);
  }

  /**
   * Expects a {@code long} argument less than or equal to the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.leq(42l))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be less than or equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static long leq(long expectedValue) {
    return EasyMock.leq(expectedValue);
  }

  /**
   * Expects a {@code short} argument less than or equal to the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.leq((short)42))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be less than or equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static short leq(short expectedValue) {
    return EasyMock.leq(expectedValue);
  }

  /**
   * Expects a {@code Comparable} argument greater than the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.gt("hi"))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be greater than.
   * @return {@code null}. The return value is always ignored.
   */
  public static <T extends Comparable<T>> T gt(Comparable<T> expectedValue) {
    return EasyMock.gt(expectedValue);
  }

  /**
   * Expects a {@code byte} argument greater than the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.gt((byte)42))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be greater than.
   * @return {@code 0}. The return value is always ignored.
   */
  public static byte gt(byte expectedValue) {
    return EasyMock.gt(expectedValue);
  }

  /**
   * Expects a {@code double} argument greater than the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.gt(42.0))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be greater than.
   * @return {@code 0}. The return value is always ignored.
   */
  public static double gt(double expectedValue) {
    return EasyMock.gt(expectedValue);
  }

  /**
   * Expects a {@code float} argument greater than the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.gt(42.0f))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be greater than.
   * @return {@code 0}. The return value is always ignored.
   */
  public static float gt(float expectedValue) {
    return EasyMock.gt(expectedValue);
  }

  /**
   * Expects an {@code int} argument greater than the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.gt(42))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be greater than.
   * @return {@code 0}. The return value is always ignored.
   */
  public static int gt(int expectedValue) {
    return EasyMock.gt(expectedValue);
  }

  /**
   * Expects a {@code long} argument greater than the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.gt(42l))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be greater than.
   * @return {@code 0}. The return value is always ignored.
   */
  public static long gt(long expectedValue) {
    return EasyMock.gt(expectedValue);
  }

  /**
   * Expects a {@code short} argument greater than the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.gt((short)42))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be greater than.
   * @return {@code 0}. The return value is always ignored.
   */
  public static short gt(short expectedValue) {
    return EasyMock.gt(expectedValue);
  }

  /**
   * Expects a {@code Comparable} argument less than the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.lt("hi"))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be less than.
   * @return {@code null}. The return value is always ignored.
   */
  public static <T extends Comparable<T>> T lt(Comparable<T> expectedValue) {
    return EasyMock.lt(expectedValue);
  }

  /**
   * Expects a {@code byte} argument less than the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.lt((byte)42))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be less than.
   * @return {@code 0}. The return value is always ignored.
   */
  public static byte lt(byte expectedValue) {
    return EasyMock.lt(expectedValue);
  }

  /**
   * Expects a {@code double} argument less than the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.lt(42.0))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be less than.
   * @return {@code 0}. The return value is always ignored.
   */
  public static double lt(double expectedValue) {
    return EasyMock.lt(expectedValue);
  }

  /**
   * Expects a {@code float} argument less than the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.lt(42.0f))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be less than.
   * @return {@code 0}. The return value is always ignored.
   */
  public static float lt(float expectedValue) {
    return EasyMock.lt(expectedValue);
  }

  /**
   * Expects an {@code int} argument less than the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.lt(42))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be less than.
   * @return {@code 0}. The return value is always ignored.
   */
  public static int lt(int expectedValue) {
    return EasyMock.lt(expectedValue);
  }

  /**
   * Expects a {@code long} argument less than the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.lt(42l))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be less than.
   * @return {@code 0}. The return value is always ignored.
   */
  public static long lt(long expectedValue) {
    return EasyMock.lt(expectedValue);
  }

  /**
   * Expects a {@code short} argument less than the given value as a parameter
   * to a mocked method.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.lt((short)42))).andReturn("hello");}
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be less than.
   * @return {@code 0}. The return value is always ignored.
   */
  public static short lt(short expectedValue) {
    return EasyMock.lt(expectedValue);
  }

  /**
   * Expects an object implementing the given class as a parameter to a mocked method. During
   * replay mode, the mocked method call will accept any {@code Object} that is an instance of
   * the specified class or one of its subclasses. Specifically, any {@code non-null} parameter for
   * which the {@code java.lang.Class.isAssignableFrom(Class)} will return true will be accepted by
   * this matcher during the replay phase.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.isA(HashMap.class))).andReturn("hello");}
   * 
   * @param <T> the expected Class type.
   * @param clazz the class of the accepted type.
   * @return {@code null}. The return value is always ignored.
   */
  public static <T> T isA(Class<T> clazz) {
    return EasyMock.isA(clazz);
  }

  /**
   * Expects a string that contains the given substring as a parameter to a mocked method.
   * During replay mode, the mocked method will accept any {@code non-null String} which contains
   * the provided {@code substring}.
   * 
   * Use this to loosen the expectations of acceptable parameters for a mocked method call.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.substring("hi"))).andReturn("hello");}
   * 
   * @param substring the substring which any incoming parameter to the mocked method must contain.
   * @return {@code null}.
   */
  public static String contains(String substring) {
    return EasyMock.contains(substring);
  }

  /**
   * Expects a {@code boolean} parameter that matches both of the provided expectations. During
   * replay mode, the mocked method will accept any {@code boolean} that matches both of the
   * provided expectations. Possible expectations for {@code first} and {@code second} include (but
   * are not limited to) {@link #anyBoolean()} and {@link #eq(boolean)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.and(AndroidMock.anyBoolean(), AndroidMock.eq(true)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(and(anyBoolean(), eq(true)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code false}. The return value is always ignored.
   */
  public static boolean and(boolean first, boolean second) {
    return EasyMock.and(first, second);
  }

  /**
   * Expects a {@code byte} parameter that matches both of the provided expectations. During replay
   * mode, the mocked method will accept any {@code byte} that matches both of the provided
   * expectations. Possible expectations for {@code first} and {@code second} include (but are not
   * limited to) {@link #anyByte()}, {@link #leq(byte)} and {@link #eq(byte)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.and(
   *        AndroidMock.gt((byte)0), AndroidMock.lt((byte)42)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(and(gt((byte)0), lt((byte)42)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static byte and(byte first, byte second) {
    return EasyMock.and(first, second);
  }

  /**
   * Expects a {@code char} parameter that matches both of the provided expectations. During replay
   * mode, the mocked method will accept any {@code char} that matches both of the provided
   * expectations. Possible expectations for {@code first} and {@code second} include (but are not
   * limited to) {@link #anyChar()} and {@link #eq(char)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.and(AndroidMock.geq('a'), AndroidMock.lt('q')))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(and(eq('a'), anyChar()))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static char and(char first, char second) {
    return EasyMock.and(first, second);
  }

  /**
   * Expects a {@code double} parameter that matches both of the provided expectations. During
   * replay mode, the mocked method will accept any {@code double} that matches both of the provided
   * expectations. Possible expectations for {@code first} and {@code second} include (but are not
   * limited to) {@link #anyDouble()}, {@link #leq(double)} and {@link #eq(double)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.and(AndroidMock.gt(0.0), AndroidMock.lt(42.0)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(and(gt(0.0), lt(42.0)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static double and(double first, double second) {
    return EasyMock.and(first, second);
  }

  /**
   * Expects a {@code float} parameter that matches both of the provided expectations. During
   * replay mode, the mocked method will accept any {@code float} that matches both of the provided
   * expectations. Possible expectations for {@code first} and {@code second} include (but are not
   * limited to) {@link #anyFloat()}, {@link #leq(float)} and {@link #eq(float)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.and(AndroidMock.gt(0.0f), AndroidMock.lt(42.0f)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(and(gt(0.0f), lt(42.0f)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static float and(float first, float second) {
    return EasyMock.and(first, second);
  }

  /**
   * Expects an {@code int} parameter that matches both of the provided expectations. During
   * replay mode, the mocked method will accept any {@code int} that matches both of the provided
   * expectations. Possible expectations for {@code first} and {@code second} include (but are not
   * limited to) {@link #anyInt()}, {@link #leq(int)} and {@link #eq(int)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.and(AndroidMock.gt(0), AndroidMock.lt(42)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(and(gt(0), lt(42)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static int and(int first, int second) {
    return EasyMock.and(first, second);
  }

  /**
   * Expects a {@code long} parameter that matches both of the provided expectations. During
   * replay mode, the mocked method will accept any {@code long} that matches both of the provided
   * expectations. Possible expectations for {@code first} and {@code second} include (but are not
   * limited to) {@link #anyLong()}, {@link #leq(long)} and {@link #eq(long)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.and(AndroidMock.gt(0l), AndroidMock.lt(42l)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(and(gt(0l), lt(42l)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static long and(long first, long second) {
    return EasyMock.and(first, second);
  }

  /**
   * Expects a {@code short} parameter that matches both of the provided expectations. During
   * replay mode, the mocked method will accept any {@code short} that matches both of the provided
   * expectations. Possible expectations for {@code first} and {@code second} include (but are not
   * limited to) {@link #anyShort()}, {@link #leq(short)} and {@link #eq(short)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.and(
   *        AndroidMock.gt((short)0), AndroidMock.lt((short)42)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(and(gt((short)0), lt((short)42)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static short and(short first, short second) {
    return EasyMock.and(first, second);
  }

  /**
   * Expects an {@code Object} parameter that matches both of the provided expectations. During
   * replay mode, the mocked method will accept any {@code Object} that matches both of the provided
   * expectations. Possible expectations for {@code first} and {@code second} include (but are not
   * limited to) {@link #anyObject()}, {@link #isA(Class)} and {@link #contains(String)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.and(
   *            AndroidMock.contains("hi"), AndroidMock.contains("world")))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(and(contains("hi"), contains("world")))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static <T> T and(T first, T second) {
    return EasyMock.and(first, second);
  }

  /**
   * Expects a {@code boolean} parameter that matches one or both of the provided expectations.
   * During replay mode, the mocked method will accept any {@code boolean} that matches one of the
   * provided expectations, or both of them. Possible expectations for {@code first} and
   * {@code second} include (but are not limited to) {@link #anyBoolean()} and {@link #eq(boolean)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.or(AndroidMock.eq(true), AndroidMock.anyBoolean()))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(and(eq(true), anyBoolean()))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code false}. The return value is always ignored.
   */
  public static boolean or(boolean first, boolean second) {
    return EasyMock.or(first, second);
  }

  /**
   * Expects a {@code byte} parameter that matches one or both of the provided expectations.
   * During replay mode, the mocked method will accept any {@code byte} that matches one of the
   * provided expectations, or both of them. Possible expectations for {@code first} and
   * {@code second} include (but are not limited to) {@link #anyByte()}, {@link #eq(byte)},
   * and {@link #lt(byte)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.or(
   *        AndroidMock.geq((byte)0), AndroidMock.lt((byte)42)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(or(geq((byte)0), lt((byte)42)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static byte or(byte first, byte second) {
    return EasyMock.or(first, second);
  }

  /**
   * Expects a {@code char} parameter that matches one or both of the provided expectations.
   * During replay mode, the mocked method will accept any {@code char} that matches one of the
   * provided expectations, or both of them. Possible expectations for {@code first} and
   * {@code second} include (but are not limited to) {@link #anyChar()} and {@link #eq(char)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.or(
   *        AndroidMock.eq('a'), AndroidMock.eq('z')))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(or(eq('a'), eq('z')))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static char or(char first, char second) {
    return EasyMock.or(first, second);
  }

  /**
   * Expects a {@code double} parameter that matches one or both of the provided expectations.
   * During replay mode, the mocked method will accept any {@code double} that matches one of the
   * provided expectations, or both of them. Possible expectations for {@code first} and
   * {@code second} include (but are not limited to) {@link #anyDouble()}, {@link #eq(double)}
   * and {@link #lt(double)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.or(
   *        AndroidMock.eq(0.0), AndroidMock.geq(42.0)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(or(eq(0.0), geq(42.0)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static double or(double first, double second) {
    return EasyMock.or(first, second);
  }

  /**
   * Expects a {@code float} parameter that matches one or both of the provided expectations.
   * During replay mode, the mocked method will accept any {@code float} that matches one of the
   * provided expectations, or both of them. Possible expectations for {@code first} and
   * {@code second} include (but are not limited to) {@link #anyFloat()}, {@link #eq(float)}
   * and {@link #lt(float)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.or(
   *        AndroidMock.eq(0.0f), AndroidMock.geq(42.0f)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(or(eq(0.0f), geq(42.0f)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static float or(float first, float second) {
    return EasyMock.or(first, second);
  }

  /**
   * Expects an {@code int} parameter that matches one or both of the provided expectations.
   * During replay mode, the mocked method will accept any {@code int} that matches one of the
   * provided expectations, or both of them. Possible expectations for {@code first} and
   * {@code second} include (but are not limited to) {@link #anyInt()}, {@link #eq(int)}
   * and {@link #lt(int)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.or(
   *        AndroidMock.eq(0), AndroidMock.geq(42)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(or(eq(0), geq(42)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static int or(int first, int second) {
    return EasyMock.or(first, second);
  }

  /**
   * Expects a {@code long} parameter that matches one or both of the provided expectations.
   * During replay mode, the mocked method will accept any {@code long} that matches one of the
   * provided expectations, or both of them. Possible expectations for {@code first} and
   * {@code second} include (but are not limited to) {@link #anyLong()}, {@link #eq(long)}
   * and {@link #lt(long)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.or(
   *        AndroidMock.eq(0l), AndroidMock.geq(42l)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(or(eq(0l), geq(42l)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static long or(long first, long second) {
    return EasyMock.or(first, second);
  }

  /**
   * Expects a {@code short} parameter that matches one or both of the provided expectations.
   * During replay mode, the mocked method will accept any {@code short} that matches one of the
   * provided expectations, or both of them. Possible expectations for {@code first} and
   * {@code second} include (but are not limited to) {@link #anyShort()}, {@link #eq(short)}
   * and {@link #lt(short)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.or(
   *        AndroidMock.eq((short)0), AndroidMock.geq((short)42)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(or(eq((short)0), geq((short)42)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static short or(short first, short second) {
    return EasyMock.or(first, second);
  }

  /**
   * Expects an {@code Object} parameter that matches one or both of the provided expectations.
   * During replay mode, the mocked method will accept any {@code Object} that matches one of the
   * provided expectations, or both of them. Possible expectations for {@code first} and
   * {@code second} include (but are not limited to) {@link #anyObject()}, {@link #eq(Class)}
   * and {@link #lt(Comparable)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.or(
   *        AndroidMock.notNull(), AndroidMock.geq(fortyTwo)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(or(notNull(), geq(fortyTwo)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param first the first expectation to test.
   * @param second the second expectation to test.
   * @return {@code null}. The return value is always ignored.
   */
  public static <T> T or(T first, T second) {
    return EasyMock.or(first, second);
  }

  /**
   * Expects a {@code boolean} parameter that does not match the provided expectation.
   * During replay mode, the mocked method will accept any {@code boolean} that does not match
   * the provided expectation. Possible expectations for {@code expectation}
   * include (but are not limited to) {@link #anyBoolean()} and {@link #eq(boolean)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.not(AndroidMock.eq(true)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(not(eq(true)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectation the expectation to test.
   * @return {@code false}. The return value is always ignored.
   */
  public static boolean not(boolean expectation) {
    return EasyMock.not(expectation);
  }

  /**
   * Expects a {@code byte} parameter that does not match the provided expectation.
   * During replay mode, the mocked method will accept any {@code byte} that does not match
   * the provided expectation. Possible expectations for {@code expectation}
   * include (but are not limited to) {@link #anyByte()}, {@link #eq(byte)} and
   * {@link #lt(byte)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.not(AndroidMock.eq((byte)42)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(not(eq((byte)42)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectation the expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static byte not(byte expectation) {
    return EasyMock.not(expectation);
  }

  /**
   * Expects a {@code char} parameter that does not match the provided expectation.
   * During replay mode, the mocked method will accept any {@code char} that does not match
   * the provided expectation. Possible expectations for {@code expectation}
   * include (but are not limited to) {@link #anyChar()} and {@link #eq(char)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.not(AndroidMock.eq('a')))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(not(eq('a')))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectation the expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static char not(char expectation) {
    return EasyMock.not(expectation);
  }

  /**
   * Expects a {@code double} parameter that does not match the provided expectation.
   * During replay mode, the mocked method will accept any {@code double} that does not match
   * the provided expectation. Possible expectations for {@code expectation}
   * include (but are not limited to) {@link #anyDouble()}, {@link #eq(double)} and
   * {@link #lt(double)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.not(AndroidMock.eq(42.0)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(not(eq(42.0)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectation the expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static double not(double expectation) {
    return EasyMock.not(expectation);
  }

  /**
   * Expects a {@code float} parameter that does not match the provided expectation.
   * During replay mode, the mocked method will accept any {@code float} that does not match
   * the provided expectation. Possible expectations for {@code expectation}
   * include (but are not limited to) {@link #anyFloat()}, {@link #eq(float)} and
   * {@link #lt(float)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.not(AndroidMock.eq(42.0f)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(not(eq(42.0f)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectation the expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static float not(float expectation) {
    return EasyMock.not(expectation);
  }

  /**
   * Expects a {@code int} parameter that does not match the provided expectation.
   * During replay mode, the mocked method will accept any {@code int} that does not match
   * the provided expectation. Possible expectations for {@code expectation}
   * include (but are not limited to) {@link #anyInt()}, {@link #eq(int)} and
   * {@link #lt(int)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.not(AndroidMock.eq(42)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(not(eq(42)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectation the expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static int not(int expectation) {
    return EasyMock.not(expectation);
  }

  /**
   * Expects a {@code long} parameter that does not match the provided expectation.
   * During replay mode, the mocked method will accept any {@code long} that does not match
   * the provided expectation. Possible expectations for {@code expectation}
   * include (but are not limited to) {@link #anyLong()}, {@link #eq(long)} and
   * {@link #lt(long)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.not(AndroidMock.eq(42l)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(not(eq(42l)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectation the expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static long not(long expectation) {
    return EasyMock.not(expectation);
  }

  /**
   * Expects a {@code short} parameter that does not match the provided expectation.
   * During replay mode, the mocked method will accept any {@code short} that does not match
   * the provided expectation. Possible expectations for {@code expectation}
   * include (but are not limited to) {@link #anyShort()}, {@link #eq(short)} and
   * {@link #lt(short)}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.not(AndroidMock.eq((short)42)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(not(eq((short)42)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectation the expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static short not(short expectation) {
    return EasyMock.not(expectation);
  }

  /**
   * Expects an {@code Object} parameter that does not match the given expectation.
   * During replay mode, the mocked method will accept any {@code Object} that does not match
   * the provided expectation. Possible expectations for {@code expectation}
   * include (but are not limited to) {@link #anyObject()}, {@link #leq(Comparable)} and
   * {@link #isNull()}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(
   *        AndroidMock.not(AndroidMock.eq(fortyTwo)))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(not(eq(fortyTwo)))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectation the expectation to test.
   * @return {@code 0}. The return value is always ignored.
   */
  public static <T> T not(T expectation) {
    return EasyMock.not(expectation);
  }

  /**
   * Expects a {@code boolean} parameter that is equal to the provided {@code value}.
   * During replay mode, the mocked method will accept any {@code boolean} that matches the
   * value of {@code expectedValue}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq(true))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq(true))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be equal.
   * @return {@code false}. The return value is always ignored.
   */
  public static boolean eq(boolean expectedValue) {
    return EasyMock.eq(expectedValue);
  }

  /**
   * Expects a {@code byte} parameter that is equal to the provided {@code expectedValue}.
   * During replay mode, the mocked method will accept any {@code byte} that matches the
   * value of {@code expectedValue}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq((byte)0))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq((byte)0))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be equal.
   * @return {@code false}. The return value is always ignored.
   */
  public static byte eq(byte expectedValue) {
    return EasyMock.eq(expectedValue);
  }

  /**
   * Expects a {@code char} parameter that is equal to the provided {@code expectedValue}.
   * During replay mode, the mocked method will accept any {@code char} that matches the
   * value of {@code expectedValue}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq('a'))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq('a'))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static char eq(char expectedValue) {
    return EasyMock.eq(expectedValue);
  }

  /**
   * Expects a {@code double} parameter that is equal to the provided {@code expectedValue}.
   * During replay mode, the mocked method will accept any {@code double} that matches the
   * value of {@code expectedValue}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq(0.0))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq(0.0))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static double eq(double expectedValue) {
    return EasyMock.eq(expectedValue);
  }

  /**
   * Expects a {@code float} parameter that is equal to the provided {@code expectedValue}.
   * During replay mode, the mocked method will accept any {@code float} that matches the
   * value of {@code expectedValue}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq(0.0f))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq(0.0f))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static float eq(float expectedValue) {
    return EasyMock.eq(expectedValue);
  }

  /**
   * Expects an {@code int} parameter that is equal to the provided {@code expectedValue}.
   * During replay mode, the mocked method will accept any {@code int} that matches the
   * value of {@code expectedValue}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq(0))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq(0))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static int eq(int expectedValue) {
    return EasyMock.eq(expectedValue);
  }

  /**
   * Expects a {@code long} parameter that is equal to the provided {@code expectedValue}.
   * During replay mode, the mocked method will accept any {@code long} that matches the
   * value of {@code expectedValue}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq(0l))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq(0l))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static long eq(long expectedValue) {
    return EasyMock.eq(expectedValue);
  }

  /**
   * Expects a {@code short} parameter that is equal to the provided {@code expectedValue}.
   * During replay mode, the mocked method will accept any {@code short} that matches the
   * value of {@code expectedValue}.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq((short)0))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq((short)0))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static short eq(short expectedValue) {
    return EasyMock.eq(expectedValue);
  }

  /**
   * Expects an {@code Object} parameter that is equal to the provided {@code expectedValue}.
   * During replay mode, the mocked method will accept any {@code Object} that matches the
   * value of {@code expectedValue} according to its {@code equals(Object)} method.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq("hi"))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq("hi"))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the value to which the specified incoming parameter to the mocked method
   * must be equal.
   * @return {@code 0}. The return value is always ignored.
   */
  public static <T> T eq(T expectedValue) {
    return EasyMock.eq(expectedValue);
  }

  /**
   * Expects a {@code boolean} array parameter that is equal to the given array, i.e. it has to
   * have the same length, and each element has to be equal.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq(myBooleanArray))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq(myBooleanArray))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the array to which the specified incoming parameter to the mocked method
   * must have equal contents.
   * @return {@code null}. The return value is always ignored.
   */
  public static boolean[] aryEq(boolean[] expectedValue) {
    return EasyMock.aryEq(expectedValue);
  }

  /**
   * Expects a {@code byte} array parameter that is equal to the given array, i.e. it has to
   * have the same length, and each element has to be equal.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq(myByteArray))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq(myByteArray))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the array to which the specified incoming parameter to the mocked method
   * must have equal contents.
   * @return {@code null}. The return value is always ignored.
   */
  public static byte[] aryEq(byte[] expectedValue) {
    return EasyMock.aryEq(expectedValue);
  }

  /**
   * Expects a {@code char} array parameter that is equal to the given array, i.e. it has to
   * have the same length, and each element has to be equal.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq(myCharArray))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq(myCharArray))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the array to which the specified incoming parameter to the mocked method
   * must have equal contents.
   * @return {@code null}. The return value is always ignored.
   */
  public static char[] aryEq(char[] expectedValue) {
    return EasyMock.aryEq(expectedValue);
  }

  /**
   * Expects a {@code double} array parameter that is equal to the given array, i.e. it has to
   * have the same length, and each element has to be equal.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq(myDoubleArray))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq(myDoubleArray))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the array to which the specified incoming parameter to the mocked method
   * must have equal contents.
   * @return {@code null}. The return value is always ignored.
   */
  public static double[] aryEq(double[] expectedValue) {
    return EasyMock.aryEq(expectedValue);
  }

  /**
   * Expects a {@code float} array parameter that is equal to the given array, i.e. it has to
   * have the same length, and each element has to be equal.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq(myFloatrArray))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq(myFloatArray))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the array to which the specified incoming parameter to the mocked method
   * must have equal contents.
   * @return {@code null}. The return value is always ignored.
   */
  public static float[] aryEq(float[] expectedValue) {
    return EasyMock.aryEq(expectedValue);
  }

  /**
   * Expects an {@code int} array parameter that is equal to the given array, i.e. it has to
   * have the same length, and each element has to be equal.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq(myIntArray))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq(myIntArray))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the array to which the specified incoming parameter to the mocked method
   * must have equal contents.
   * @return {@code null}. The return value is always ignored.
   */
  public static int[] aryEq(int[] expectedValue) {
    return EasyMock.aryEq(expectedValue);
  }

  /**
   * Expects a {@code long} array parameter that is equal to the given array, i.e. it has to
   * have the same length, and each element has to be equal.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq(myLongArray))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq(myLongArray))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the array to which the specified incoming parameter to the mocked method
   * must have equal contents.
   * @return {@code null}. The return value is always ignored.
   */
  public static long[] aryEq(long[] expectedValue) {
    return EasyMock.aryEq(expectedValue);
  }

  /**
   * Expects a {@code short} array parameter that is equal to the given array, i.e. it has to
   * have the same length, and each element has to be equal.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq(myShortArray))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq(myShortArray))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the array to which the specified incoming parameter to the mocked method
   * must have equal contents.
   * @return {@code null}. The return value is always ignored.
   */
  public static short[] aryEq(short[] expectedValue) {
    return EasyMock.aryEq(expectedValue);
  }

  /**
   * Expects a {@code Object} array parameter that is equal to the given array, i.e. it has to
   * have the same length, and each element has to be equal.
   * 
   * E.g.
   * {@code AndroidMock.expect(mock.getString(AndroidMock.eq(myObjectArray))).andReturn("hello");}
   * 
   * Or, for illustration purposes (using static imports)
   * 
   * {@code expect(mock.getString(eq(myObjectArray))).andReturn("hello");}
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param <T> the type of the array, it is passed through to prevent casts.
   * @param expectedValue the array to which the specified incoming parameter to the mocked method
   * must have equal contents.
   * @return {@code null}. The return value is always ignored.
   */
  public static <T> T[] aryEq(T[] expectedValue) {
    return EasyMock.aryEq(expectedValue);
  }

  /**
   * Expects any {@code null} Object as a parameter.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @return {@code null}. The return value is always ignored.
   */
  @SuppressWarnings("unchecked")
  public static <T> T isNull() {
    return (T) EasyMock.isNull();
  }

  /**
   * Expects any {@code non-null} Object parameter.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @return {@code null}. The return value is always ignored.
   */
  @SuppressWarnings("unchecked")
  public static <T> T notNull() {
    return (T) EasyMock.notNull();
  }

  /**
   * Expects a {@code String} that contains a substring that matches the given regular
   * expression as a parameter to the mocked method.
   * 
   * See {@link java.util.regex.Matcher#find()} for more details.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param regex the regular expression which must match some substring of the incoming parameter
   * to the mocked method.
   * @return {@code null}. The return value is always ignored.
   */
  public static String find(String regex) {
    return EasyMock.find(regex);
  }

  /**
   * Expects a {@code String} as a parameter to the mocked method, the entire length of which must
   * match the given regular expression. This is not to be confused with {@link #find(String)} which
   * matches the regular expression against any substring of the incoming parameter to the mocked
   * method.
   * 
   * See {@link java.util.regex.Matcher#matches()} for more details.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param regex the regular expression against which the entire incoming parameter to the
   * mocked method must match.
   * @return {@code null}. The return value is always ignored.
   */
  public static String matches(String regex) {
    return EasyMock.matches(regex);
  }

  /**
   * Expects a {@code String} as a parameter to the mocked method that starts with the given prefix.
   * 
   * See {@link java.lang.String#startsWith(String)} for more details.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param prefix the string that is expected to match against the start of any incoming
   * parameter to the mocked method.
   * @return {@code null}. The return value is always ignored.
   */
  public static String startsWith(String prefix) {
    return EasyMock.startsWith(prefix);
  }

  /**
   * Expects a {@code String} as a parameter to the mocked method that ends with the given
   * {@code suffix}.
   * 
   * See {@link java.lang.String#startsWith(String)} for more details.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param suffix the string that is expected to match against the end of any incoming
   * parameter to the mocked method.
   * @return {@code null}. The return value is always ignored.
   */
  public static String endsWith(String suffix) {
    return EasyMock.endsWith(suffix);
  }

  /**
   * Expects a {@code double} as a parameter to the mocked method that has an absolute difference to
   * the given {@code expectedValue} that is less than the given {@code delta}.
   * 
   * The acceptable range of values is theoretically defined as any value {@code x} which satisfies
   * the following inequality: {@code expectedValue - delta &lt;= x &lt;= expectedValue + delta}.
   * 
   * In practice, this is only true when {@code expectedValue + delta} and
   * {@code expectedValue - delta} fall exactly on a precisely representable {@code double} value.
   * Normally, the acceptable range of values is defined as any value {@code x} which satisfies the
   * following inequality:
   * {@code expectedValue - delta &lt; x &lt; expectedValue + delta}.
   * 
   * E.g. {@code AndroidMock.expect(mockObject.getString(
   *    AndroidMock.eq(42.0, 0.1))).andReturn("hello world");}
   * 
   * The code snippet above will expect any {@code double} value greater than 41.9 and
   * less than 42.1.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the center value of the expected range of values.
   * @param delta the acceptable level of inaccuracy before this expectation fails.
   * @return {@code 0}. The return value is always ignored.
   */
  public static double eq(double expectedValue, double delta) {
    return EasyMock.eq(expectedValue, delta);
  }

  /**
   * Expects a {@code float} as a parameter to the mocked method that has an absolute difference to
   * the given {@code expectedValue} that is less than the given {@code delta}.
   * 
   * The acceptable range of values is theoretically defined as any value {@code x} which satisfies
   * the following inequality: {@code expectedValue - delta &lt;= x &lt;= expectedValue + delta}.
   * 
   * In practice, this is only true when {@code expectedValue + delta} and
   * {@code expectedValue - delta} fall exactly on a precisely representable {@code float} value.
   * Normally, the acceptable range of values is defined as any value {@code x} which satisfies the
   * following inequality:
   * {@code expectedValue - delta &lt; x &lt; expectedValue + delta}.
   * 
   * E.g. {@code AndroidMock.expect(mockObject.getString(
   *    AndroidMock.eq(42.0f, 0.1f))).andReturn("hello world");}
   * 
   * The code snippet above will expect any {@code float} value greater than 41.9 and
   * less than 42.1.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the center value of the expected range of values.
   * @param delta the acceptable level of inaccuracy before this expectation fails.
   * @return {@code 0}. The return value is always ignored.
   */
  public static float eq(float expectedValue, float delta) {
    return EasyMock.eq(expectedValue, delta);
  }

  /**
   * Expects an {@code Object} as a parameter to the mocked method that is the same as the given
   * value. This expectation will fail unless the incoming parameter is {@code ==} to the
   * {@code expectedValue} provided (i.e. the same {@code Object} reference).
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param <T> the type of the object, it is passed through to prevent casts.
   * @param expectedValue the exact object which is expected during replay.
   * @return {@code null}. The return value is always ignored.
   */
  public static <T> T same(T expectedValue) {
    return EasyMock.same(expectedValue);
  }

  /**
   * Expects a {@link java.lang.Comparable} argument equal to the given value according to
   * its {@link java.lang.Comparable#compareTo(Object)} method.
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the {@link java.lang.Comparable} value which is expected to be equal to
   * the incoming parameter to the mocked method according to the
   * {@link java.lang.Comparable#compareTo(Object)} method.
   * @return {@code null}. The return value is always ignored.
   */
  public static <T extends Comparable<T>> T cmpEq(Comparable<T> expectedValue) {
    return EasyMock.cmpEq(expectedValue);
  }

  /**
   * Expects an argument that will be compared using the provided {@link java.util.Comparator}, the
   * result of which will then be applied to the provided {@link org.easymock.LogicalOperator}
   * (e.g. {@link org.easymock.LogicalOperator#LESS_THAN},
   * {@link org.easymock.LogicalOperator#EQUAL},
   * {@link org.easymock.LogicalOperator#GREATER_OR_EQUAL}).
   * 
   * The following comparison will take place:
   * {@code comparator.compare(actual, expected) operator 0}
   * 
   * E.g.
   * For illustration purposes (using static imports):
   * 
   * {@code
   * expect(mockObject.getString(cmp("hi", CASE_INSENSITIVE_ORDER, LESS_THAN))).andReturn("hello");}
   *
   * {@code
   * AndroidMock.expect(mockObject.getString(AndroidMock.cmp("hi", String.CASE_INSENSITIVE_ORDER,
   *    LogicalOperator.LESS_THAN))).andReturn("hello");}
   * 
   * 
   * The above invocation indicates that the call to {@code mockObject.getString(String)} is
   * expecting any String which is lexically before "hi" (in a case insensitive ordering).
   * 
   * If this method is used for anything other than to set a parameter expectation as part of a
   * mock object's recording phase, then an {@code IllegalStateException} will be thrown.
   * 
   * @param expectedValue the expected value against which the incoming method parameter will be
   * compared.
   * @param comparator {@link java.util.Comparator} used to perform the comparison between the
   * expected value and the incoming parameter to the mocked method.
   * @param operator The comparison operator, usually one of
   * {@link org.easymock.LogicalOperator#LESS_THAN},
   * {@link org.easymock.LogicalOperator#LESS_OR_EQUAL},
   * {@link org.easymock.LogicalOperator#EQUAL}, {@link org.easymock.LogicalOperator#GREATER},
   * {@link org.easymock.LogicalOperator#GREATER_OR_EQUAL} 
   * @return {@code null}. The return value is always ignored.
   */
  public static <T> T cmp(T expectedValue, Comparator<? super T> comparator,
      LogicalOperator operator) {
    return EasyMock.cmp(expectedValue, comparator, operator);
  }

  /**
   * Expect any {@code Object} as a parameter to the mocked method, but capture it for later use.
   * 
   * {@link org.easymock.Capture} allows for capturing of the incoming value. Use
   * {@link org.easymock.Capture#getValue()} to retrieve the captured value.
   * 
   * @param <T> Type of the captured object
   * @param captured a container to hold the captured value, retrieved by
   * {@link org.easymock.Capture#getValue()}
   * @return {@code null}. The return value is always ignored.
   */
  public static <T> T capture(Capture<T> captured) {
    return EasyMock.capture(captured);
  }

  /**
   * Expect any {@code int/Integer} as a parameter to the mocked method, but capture it for later
   * use.
   * 
   * {@link org.easymock.Capture} allows for capturing of the incoming value. Use
   * {@link org.easymock.Capture#getValue()} to retrieve the captured value.
   * 
   * @param captured a container to hold the captured value, retrieved by
   * {@link org.easymock.Capture#getValue()}
   * @return {@code 0}. The return value is always ignored.
   */
  public static int capture(Capture<Integer> captured) {
    return EasyMock.capture(captured);
  }

  /**
   * Expect any {@code long/Long} as a parameter to the mocked method, but capture it for later
   * use.
   * 
   * {@link org.easymock.Capture} allows for capturing of the incoming value. Use
   * {@link org.easymock.Capture#getValue()} to retrieve the captured value.
   * 
   * @param captured a container to hold the captured value, retrieved by
   * {@link org.easymock.Capture#getValue()}
   * @return {@code 0}. The return value is always ignored.
   */
  public static long capture(Capture<Long> captured) {
    return EasyMock.capture(captured);
  }

  /**
   * Expect any {@code float/Float} as a parameter to the mocked method, but capture it for later
   * use.
   * 
   * {@link org.easymock.Capture} allows for capturing of the incoming value. Use
   * {@link org.easymock.Capture#getValue()} to retrieve the captured value.
   * 
   * @param captured a container to hold the captured value, retrieved by
   * {@link org.easymock.Capture#getValue()}
   * @return {@code 0}. The return value is always ignored.
   */
  public static float capture(Capture<Float> captured) {
    return EasyMock.capture(captured);
  }

  /**
   * Expect any {@code double/Double} as a parameter to the mocked method, but capture it for later
   * use.
   * 
   * {@link org.easymock.Capture} allows for capturing of the incoming value. Use
   * {@link org.easymock.Capture#getValue()} to retrieve the captured value.
   * 
   * @param captured a container to hold the captured value, retrieved by
   * {@link org.easymock.Capture#getValue()}
   * @return {@code 0}. The return value is always ignored.
   */
  public static double capture(Capture<Double> captured) {
    return EasyMock.capture(captured);
  }

  /**
   * Expect any {@code byte/Byte} as a parameter to the mocked method, but capture it for later
   * use.
   * 
   * {@link org.easymock.Capture} allows for capturing of the incoming value. Use
   * {@link org.easymock.Capture#getValue()} to retrieve the captured value.
   * 
   * @param captured a container to hold the captured value, retrieved by
   * {@link org.easymock.Capture#getValue()}
   * @return {@code 0}
   */
  public static byte capture(Capture<Byte> captured) {
    return EasyMock.capture(captured);
  }

  /**
   * Expect any {@code char/Character} as a parameter to the mocked method, but capture it for later
   * use.
   * 
   * {@link org.easymock.Capture} allows for capturing of the incoming value. Use
   * {@link org.easymock.Capture#getValue()} to retrieve the captured value.
   * 
   * @param captured a container to hold the captured value, retrieved by
   * {@link org.easymock.Capture#getValue()}
   * @return {@code 0}
   */
  public static char capture(Capture<Character> captured) {
    return EasyMock.capture(captured);
  }

  /**
   * Switches the given mock objects (more exactly: the controls of the mock
   * objects) to replay mode.
   * 
   * @param mocks the mock objects.
   */
  public static void replay(Object... mocks) {
    for (Object mockObject : mocks) {
      if (mockObject instanceof MockObject) {
        EasyMock.replay(((MockObject) mockObject).getDelegate___AndroidMock());
      } else {
        EasyMock.replay(mockObject);
      }
    }
  }

  /**
   * Resets the given mock objects (more exactly: the controls of the mock
   * objects) allowing the mock objects to be reused.
   * 
   * @param mocks the mock objects.
   */
  public static void reset(Object... mocks) {
    for (Object mockObject : mocks) {
      if (mockObject instanceof MockObject) {
        EasyMock.reset(((MockObject) mockObject).getDelegate___AndroidMock());
      } else {
        EasyMock.reset(mockObject);
      }
    }
  }

  /**
   * Resets the given mock objects (more exactly: the controls of the mock
   * objects) and change them in to mocks with nice behavior.
   * {@link #createNiceMock(Class, Object...)} has more details.
   * 
   * @param mocks the mock objects
   */
  public static void resetToNice(Object... mocks) {
    for (Object mockObject : mocks) {
      if (mockObject instanceof MockObject) {
        EasyMock.resetToNice(((MockObject) mockObject).getDelegate___AndroidMock());
      } else {
        EasyMock.resetToNice(mockObject);
      }
    }
  }

  /**
   * Resets the given mock objects (more exactly: the controls of the mock
   * objects) and turn them to a mock with default behavior. {@link #createMock(Class, Object...)}
   * has more details.
   * 
   * @param mocks the mock objects
   */
  public static void resetToDefault(Object... mocks) {
    for (Object mockObject : mocks) {
      if (mockObject instanceof MockObject) {
        EasyMock.resetToDefault(((MockObject) mockObject).getDelegate___AndroidMock());
      } else {
        EasyMock.resetToDefault(mockObject);
      }
    }
  }

  /**
   * Resets the given mock objects (more exactly: the controls of the mock
   * objects) and turn them to a mock with strict behavior.
   * {@link #createStrictMock(Class, Object...)} has more details.
   * 
   * @param mocks the mock objects
   */
  public static void resetToStrict(Object... mocks) {
    for (Object mockObject : mocks) {
      if (mockObject instanceof MockObject) {
        EasyMock.resetToStrict(((MockObject) mockObject).getDelegate___AndroidMock());
      } else {
        EasyMock.resetToStrict(mockObject);
      }
    }
  }

  /**
   * Verifies that all of the expected method calls for the given mock objects (more exactly: the
   * controls of the mock objects) have been executed.
   * 
   * The {@code verify} method captures the scenario where several methods were invoked correctly,
   * but some invocations did not occur. Typically, the {@code verify} method is the final thing
   * invoked in a test. 
   * 
   * @param mocks the mock objects.
   */
  public static void verify(Object... mocks) {
    for (Object mockObject : mocks) {
      if (mockObject instanceof MockObject) {
        EasyMock.verify(((MockObject) mockObject).getDelegate___AndroidMock());
      } else {
        EasyMock.verify(mockObject);
      }
    }
  }

  /**
   * Switches order checking of the given mock object (more exactly: the control
   * of the mock object) on or off. When order checking is on, the mock will expect the method
   * invokations to occur exactly in the order in which they appeared during the recording phase.
   * 
   * @param mock the mock object.
   * @param orderCheckingOn {@code true} to turn order checking on, {@code false} to turn it off.
   */
  public static void checkOrder(Object mock, boolean orderCheckingOn) {
    if (mock instanceof MockObject) {
      EasyMock.checkOrder(((MockObject) mock).getDelegate___AndroidMock(), orderCheckingOn);
    } else {
      EasyMock.checkOrder(mock, orderCheckingOn);
    }
  }

  /**
   * Reports an argument matcher. This method is needed to define custom argument
   * matchers.
   * 
   * For example:
   * 
   * {@code
   * AndroidMock.reportMatcher(new IntIsFortyTwo());
   * AndroidMock.expect(mockObject.getString(null)).andReturn("hello world");}
   * 
   * This example will expect a parameter for {@code mockObject.getString(int)} that matches the
   * conditions required by the {@code matches} method as defined by
   * {@link org.easymock.IArgumentMatcher#matches(Object)}.
   * 
   * @param matcher the matcher whose {@code matches} method will be applied to the incoming
   * parameter to the mocked method.
   */
  public static void reportMatcher(IArgumentMatcher matcher) {
    EasyMock.reportMatcher(matcher);
  }

  /**
   * Returns the arguments of the current mock method call, if inside an
   * {@code IAnswer} callback - be careful here, reordering parameters of a
   * method changes the semantics of your tests.
   * 
   * This method is only usable within an {@link org.easymock.IAnswer} instance. Attach an
   * {@link org.easymock.IAnswer} to an expectation by using the
   * {@link org.easymock.IExpectationSetters#andAnswer(org.easymock.IAnswer)} method.
   * 
   * E.g.
   * {@code AndroidMock.expect(mockObject.getString()).andAnswer(myAnswerCallback);}
   * 
   * @return the arguments of the current mock method call.
   * @throws IllegalStateException if called outside of {@code IAnswer}
   *         callbacks.
   */
  public static Object[] getCurrentArguments() {
    return EasyMock.getCurrentArguments();
  }

  /**
   * Makes the mock thread safe. The mock will be usable in a multithreaded
   * environment.
   * 
   * @param mock the mock to make thread safe.
   * @param threadSafe If the mock should be thread safe or not.
   */
  public static void makeThreadSafe(Object mock, boolean threadSafe) {
    if (mock instanceof MockObject) {
      EasyMock.makeThreadSafe(((MockObject) mock).getDelegate___AndroidMock(), threadSafe);
    } else {
      EasyMock.makeThreadSafe(mock, threadSafe);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T, S> T getSubclassFor(Class<? super T> clazz, Class<S> delegateInterface,
      Object realMock, Object... args) {
    Class<T> subclass;
    String className = null;
    try {
      if (isAndroidClass(clazz)) {
        className = FileUtils.getSubclassNameFor(clazz, SdkVersion.getCurrentVersion());
      } else {
        className = FileUtils.getSubclassNameFor(clazz, SdkVersion.UNKNOWN);
      }
      subclass = (Class<T>) Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Could not find class for " + className
          + " which likely means that the mock-instrumented jar has not been created or else"
          + " is not being used in the current runtime environment. Try running MockGeneratorMain"
          + " in MockGenerator_deploy.jar or using the output of that execution as the input to"
          + " the dex/apk generation.", e);
    }
    Constructor<T> constructor = getConstructorFor(subclass, args);
    T newObject;
    try {
      newObject = constructor.newInstance(args);
    } catch (InstantiationException e) {
      throw new RuntimeException("Internal error instantiating new mock subclass"
          + subclass.getName(), e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(
          "Internal error - the new mock subclass' constructor was inaccessible", e);
    } catch (InvocationTargetException e) {
      throw new ExceptionInInitializerError(e);
    }
    Method[] methods = subclass.getMethods();
    Method setMethod;
    try {
      setMethod = subclass.getMethod("setDelegate___AndroidMock", delegateInterface);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Internal error - No setDelegate method found for " + "class "
          + subclass.getName() + " and param " + delegateInterface.getName(), e);
    }
    try {
      setMethod.invoke(newObject, realMock);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Internal error setting the delegate, expected "
          + newObject.getClass() + " to be subclass of " + clazz.getName());
    } catch (InvocationTargetException e) {
      throw new RuntimeException("Severe internal error, setDelegate threw an exception", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Internal error, setDelegate method was inaccessible", e);
    }
    return newObject;
  }
  
  static boolean isUnboxableToPrimitive(Class<?> clazz, Object arg, boolean exactMatch) {
    if (!clazz.isPrimitive()) {
      throw new IllegalArgumentException(
          "Internal Error - The class to test against is not a primitive");
    }
    Class<?> unboxedType = null;
    if (arg.getClass().equals(Integer.class)) {
      unboxedType = Integer.TYPE;
    } else if (arg.getClass().equals(Long.class)) {
      unboxedType = Long.TYPE;
    } else if (arg.getClass().equals(Byte.class)) {
      unboxedType = Byte.TYPE;
    } else if (arg.getClass().equals(Short.class)) {
      unboxedType = Short.TYPE;
    } else if (arg.getClass().equals(Character.class)) {
      unboxedType = Character.TYPE;
    } else if (arg.getClass().equals(Float.class)) {
      unboxedType = Float.TYPE;
    } else if (arg.getClass().equals(Double.class)) {
      unboxedType = Double.TYPE;
    } else if (arg.getClass().equals(Boolean.class)) {
      unboxedType = Boolean.TYPE;
    } else {
      return false;
    }
    if (exactMatch) {
      return clazz == unboxedType;
    }
    return isAssignable(clazz, unboxedType);
  }
  
  private static boolean isAssignable(Class<?> to, Class<?> from) {
    if (to == Byte.TYPE) {
      return from == Byte.TYPE;
    } else if (to == Short.TYPE){
      return from == Byte.TYPE || from == Short.TYPE || from == Character.TYPE;
    } else if (to == Integer.TYPE || to == Character.TYPE) {
      return from == Byte.TYPE || from == Short.TYPE || from == Integer.TYPE
          || from == Character.TYPE;
    } else if (to == Long.TYPE) {
      return from == Byte.TYPE || from == Short.TYPE || from == Integer.TYPE || from == Long.TYPE
          || from == Character.TYPE;
    } else if (to == Float.TYPE) {
      return from == Byte.TYPE || from == Short.TYPE || from == Integer.TYPE
          || from == Character.TYPE || from == Float.TYPE;
    } else if (to == Double.TYPE) {
      return from == Byte.TYPE || from == Short.TYPE || from == Integer.TYPE || from == Long.TYPE
          || from == Character.TYPE || from == Float.TYPE || from == Double.TYPE;
    } else if (to == Boolean.TYPE) {
      return from == Boolean.TYPE;
    } else {
      return to.isAssignableFrom(from);
    }
  }
  
  @SuppressWarnings("unchecked")
  static <T> Constructor<T> getConstructorFor(Class<T> clazz, Object... args)
      throws SecurityException {
    Constructor<T>[] constructors = (Constructor<T>[]) clazz.getConstructors();
    Constructor<T> compatibleConstructor = null;
    for (Constructor<T> constructor : constructors) {
      Class<?>[] params = constructor.getParameterTypes();
      if (params.length == args.length) {
        boolean exactMatch = true;
        boolean compatibleMatch = true;
        for (int i = 0; i < params.length; ++i) {
          Object arg = args[i];
          if (arg == null) {
            arg = Void.TYPE;
          }
          if (!params[i].isAssignableFrom(arg.getClass())) {
            if (params[i].isPrimitive()) {
              exactMatch &= isUnboxableToPrimitive(params[i], arg, true);
              compatibleMatch &= isUnboxableToPrimitive(params[i], arg, false);
            } else {
              exactMatch = false;
              compatibleMatch = false;
            }
          }
        }
        if (exactMatch) {
          return constructor;
        } else if (compatibleMatch) {
          compatibleConstructor = constructor;
        }
      }
    }
    if (compatibleConstructor != null) {
      return compatibleConstructor;
    }
    List<String> argTypes = new ArrayList<String>(args.length);
    for (Object arg : args) {
      argTypes.add(arg == null ? "<null>" : arg.getClass().toString());
    }
    throw new IllegalArgumentException("Could not find the specified Constructor: "
        + clazz.getName() + "(" + argTypes + ")");
  }

  @SuppressWarnings("unchecked")
  private static <T> Class<T> getInterfaceFor(Class<T> clazz) {
    try {
      String className;
      if (isAndroidClass(clazz)) {
        className = FileUtils.getInterfaceNameFor(clazz, SdkVersion.getCurrentVersion());
      } else {
        className = FileUtils.getInterfaceNameFor(clazz, SdkVersion.UNKNOWN);
      }
      return (Class<T>) Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Could not find mock for " + clazz.getName()
          + "  -- Make sure to run the MockGenerator.jar on your test jar, and to "
          + "build the Android test APK using the modified jar created by MockGenerator", e);
    }
  }

  static boolean isAndroidClass(Class<?> clazz) {
    String packageName = clazz.getPackage().getName();
    return packageName.startsWith("android.") || packageName.startsWith("dalvik.")
        || packageName.startsWith("java.") || packageName.startsWith("javax.")
        || packageName.startsWith("org.xml.sax") || packageName.startsWith("org.xmlpull.v1")
        || packageName.startsWith("org.w3c.dom") || packageName.startsWith("org.apache.http")
        || packageName.startsWith("junit.");
  }
}
