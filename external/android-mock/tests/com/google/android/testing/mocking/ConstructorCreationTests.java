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

import junit.framework.TestCase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;


/**
 * Tests for mocked objects with non default constructors.
 * 
 * @author swoodward@google.com (Stephen Woodward)
 */
public class ConstructorCreationTests extends TestCase {
  public static class Foo {
    private int value;
    Foo(int value) { this.value = value; }
    int get() { return value; }
  }

  public static class Bar {
    private double value;
    Bar(double value) { this.value = value; }
    double get() { return value; }
  }

  public static class TestClass {
    public int v1;
    public double v2;
    public boolean usedFloatConstructor;
    
    public TestClass(Foo foo) {
      this(foo.get());
    }

    public TestClass(Foo foo, Bar bar) {
      this(foo.get(), bar.get());
    }

    public TestClass(int v1) {
      this(v1, 0);
    }

    public TestClass(int v1, float v2) {
      this.v1 = v1;
      this.v2 = v2;
      usedFloatConstructor = true;
    }

    public TestClass(int v1, double v2) {
      this.v1 = v1;
      this.v2 = (int) v2;
      usedFloatConstructor = false;
    }
  }

  private void hasConstructor(Object... args) {
    Constructor<TestClass> constructor =
        AndroidMock.getConstructorFor(TestClass.class, args);
    assertNotNull(constructor);
  }

  private void doesNotHaveConstructor(Object... args) {
    try {
      Constructor<TestClass> constructor =
          AndroidMock.getConstructorFor(TestClass.class, args);
      fail("A constructor was found: " + constructor);
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void testConstructors() {
    hasConstructor(new Foo(1));
    doesNotHaveConstructor(new Bar(2));
    hasConstructor(new Foo(1), new Bar(2));
    hasConstructor(1);
    hasConstructor(1, 2);
    doesNotHaveConstructor(new Foo(1), 2);
    hasConstructor(1, new Integer("2"));
    hasConstructor(1, 2.0);
    hasConstructor(1, 2.0f);
  }

  private void checkConstructor(Object[] args, Type[] expectedTypes) {
    Constructor<TestClass> constructor =
        AndroidMock.getConstructorFor(TestClass.class, args);
    assertNotNull(constructor);
    Type[] types = constructor.getGenericParameterTypes();
    assertEquals(expectedTypes.length, types.length);
    for (int i = 0; i < expectedTypes.length; ++i) {
      assertEquals(expectedTypes[i], types[i]);
    }
  }

  public void testCorrectConstructor() {
    checkConstructor(
            new Object[]{new Foo(1)},
            new Type[]{Foo.class});
    checkConstructor(
            new Object[]{new Foo(1), new Bar(2)},
            new Type[]{Foo.class, Bar.class});
    checkConstructor(
            new Object[]{1},
            new Type[]{Integer.TYPE});
    checkConstructor(
            new Object[]{1, new Float("2")},
            new Type[]{Integer.TYPE, Float.TYPE});
    checkConstructor(
            new Object[]{1, 2.0},
            new Type[]{Integer.TYPE, Double.TYPE});
    checkConstructor(
            new Object[]{1, 2.0f},
            new Type[]{Integer.TYPE, Float.TYPE});
  }
}