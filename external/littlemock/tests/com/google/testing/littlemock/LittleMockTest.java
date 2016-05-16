/*
 * Copyright 2011 Google Inc.
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

package com.google.testing.littlemock;

import static com.google.testing.littlemock.LittleMock.anyBoolean;
import static com.google.testing.littlemock.LittleMock.anyByte;
import static com.google.testing.littlemock.LittleMock.anyChar;
import static com.google.testing.littlemock.LittleMock.anyDouble;
import static com.google.testing.littlemock.LittleMock.anyFloat;
import static com.google.testing.littlemock.LittleMock.anyInt;
import static com.google.testing.littlemock.LittleMock.anyLong;
import static com.google.testing.littlemock.LittleMock.anyObject;
import static com.google.testing.littlemock.LittleMock.anyShort;
import static com.google.testing.littlemock.LittleMock.anyString;
import static com.google.testing.littlemock.LittleMock.anyTimes;
import static com.google.testing.littlemock.LittleMock.atLeast;
import static com.google.testing.littlemock.LittleMock.atLeastOnce;
import static com.google.testing.littlemock.LittleMock.atMost;
import static com.google.testing.littlemock.LittleMock.between;
import static com.google.testing.littlemock.LittleMock.checkForProgrammingErrorsDuringTearDown;
import static com.google.testing.littlemock.LittleMock.doAnswer;
import static com.google.testing.littlemock.LittleMock.doNothing;
import static com.google.testing.littlemock.LittleMock.doReturn;
import static com.google.testing.littlemock.LittleMock.doThrow;
import static com.google.testing.littlemock.LittleMock.eq;
import static com.google.testing.littlemock.LittleMock.inOrder;
import static com.google.testing.littlemock.LittleMock.initMocks;
import static com.google.testing.littlemock.LittleMock.isA;
import static com.google.testing.littlemock.LittleMock.matches;
import static com.google.testing.littlemock.LittleMock.mock;
import static com.google.testing.littlemock.LittleMock.never;
import static com.google.testing.littlemock.LittleMock.reset;
import static com.google.testing.littlemock.LittleMock.timeout;
import static com.google.testing.littlemock.LittleMock.times;
import static com.google.testing.littlemock.LittleMock.verify;
import static com.google.testing.littlemock.LittleMock.verifyNoMoreInteractions;
import static com.google.testing.littlemock.LittleMock.verifyZeroInteractions;

import com.google.testing.littlemock.LittleMock.ArgumentMatcher;
import com.google.testing.littlemock.LittleMock.InOrder;

import junit.framework.TestCase;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Unit tests for the LittleMock class.
 *
 * @author hugohudson@gmail.com (Hugo Hudson)
 */
public class LittleMockTest extends TestCase {
  @Mock private Foo mFoo;
  @Mock private Bar mBar;
  @Mock private BarSubtype mBarSubtype;
  @Captor private ArgumentCaptor<String> mCaptureString;
  @Captor private ArgumentCaptor<String> mCaptureAnotherString;
  @Captor private ArgumentCaptor<Integer> mCaptureInteger;
  @Captor private ArgumentCaptor<Callback> mCaptureCallback;
  private ExecutorService mExecutorService;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LittleMock.initMocks(this);
    mExecutorService = Executors.newCachedThreadPool();
  }

  @Override
    protected void tearDown() throws Exception {
      mExecutorService.shutdown();
      super.tearDown();
    }

  /** Simple interface for testing against. */
  public interface Callback {
    public void callMeNow();
  }

  /** Simple interface for testing against. */
  public interface Foo {
    public int anInt();
    public boolean aBoolean();
    public byte aByte();
    public short aShort();
    public long aLong();
    public float aFloat();
    public double aDouble();
    public char aChar();
    public String aString();
    public Object anObject();
    public Foo anInterface();
    public void add(String input);
    public void clear();
    public String get(int index);
    public String lookup(String string);
    public void getResultLater(Callback callback);
    public String findByInt(int input);
    public String findByBoolean(boolean input);
    public String findByByte(byte input);
    public String findByShort(short input);
    public String findByLong(long input);
    public String findByFloat(float input);
    public String findByDouble(double input);
    public String findByChar(char input);
    public void takesObject(Object input);
    public void takesList(List<String> input);
    public void takesBar(Bar bar);
    public void exceptionThrower() throws Exception;
    public Bar aBar();
    public BarSubtype aBarSubtype();
  }

  /** Simple interface for testing against. */
  public interface Bar {
    public void doSomething();
    public String twoStrings(String first, String second);
    public void mixedArguments(int first, String second);
    public void getResultLater(Callback callback);
  }

  /** Subtype of Bar. */
  public interface BarSubtype extends Bar {
    public void doSomethingElse();
  }

  /** Another interface for testing with. */
  public interface OnClickListener {
    void onClick(Bar bar);
  }

  public void testDefaultReturnTypesForNewMocks() {
    assertEquals(0, mFoo.anInt());
    assertEquals(false, mFoo.aBoolean());
    assertEquals(0, mFoo.aByte());
    assertEquals(0, mFoo.aShort());
    assertEquals(0L, mFoo.aLong());
    assertEquals(0.0f, mFoo.aFloat());
    assertEquals(0.0d, mFoo.aDouble());
    assertEquals('\u0000', mFoo.aChar());
    assertEquals(null, mFoo.aString());
    assertEquals(null, mFoo.anObject());
    assertEquals(null, mFoo.anInterface());
  }

  public void testVerify_FailsIfNotDoneOnAProxy() {
    try {
      verify("hello").contains("something");
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testVerify_FailsIfNotCreatedByOurMockMethod() {
    try {
      verify(createNotARealMock()).add("something");
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testVerify_SuccessfulVerification() {
    mFoo.add("something");
    verify(mFoo).add("something");
  }

  public void testVerify_SuccessfulVerification_NormalOrder() {
    mFoo.add("something");
    mFoo.add("something else");
    verify(mFoo).add("something");
    verify(mFoo).add("something else");
  }

  public void testVerify_SuccessfulVerification_ReverseOrder() {
    mFoo.add("something");
    mFoo.add("something else");
    verify(mFoo).add("something else");
    verify(mFoo).add("something");
  }

  public void testVerify_MeansOnlyOnceSoShouldFailIfCalledTwice() {
    mFoo.add("something");
    mFoo.add("something");
    try {
      verify(mFoo).add("something");
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerify_FailedVerification_CalledWithWrongArgument() {
    mFoo.add("something else");
    try {
      verify(mFoo).add("something");
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerify_FailedVerification_WasNeverCalled() {
    try {
      verify(mFoo).add("something");
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerify_TimesTwice_Succeeds() {
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, LittleMock.times(2)).add("jim");
  }

  public void testVerify_TimesTwice_ButThreeTimesFails() {
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    try {
      verify(mFoo, LittleMock.times(2)).add("jim");
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerify_TimesTwice_ButOnceFails() {
    mFoo.add("jim");
    try {
      verify(mFoo, LittleMock.times(2)).add("jim");
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerify_TimesTwice_DifferentStringsFails() {
    mFoo.add("jim");
    mFoo.add("bob");
    try {
      verify(mFoo, LittleMock.times(2)).add("jim");
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerify_TimesTwice_WorksWithAnyString() {
    mFoo.add("jim");
    mFoo.add("bob");
    verify(mFoo, LittleMock.times(2)).add(anyString());
  }

  public void testVerify_TimesTwice_FailsIfJustOnceWithAnyString() {
    mFoo.add("bob");
    try {
      verify(mFoo, LittleMock.times(2)).add(anyString());
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerify_TimesTwice_FailsIfThreeTimesWithAnyString() {
    mFoo.add("bob");
    mFoo.add("jim");
    mFoo.add("james");
    try {
      verify(mFoo, LittleMock.times(2)).add(anyString());
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerify_Never_Succeeds() {
    verify(mFoo, never()).add("jim");
    verify(mFoo, never()).anInt();
  }

  public void testVerify_Never_FailsIfWasCalled() {
    mFoo.add("jim");
    try {
      verify(mFoo, never()).add("jim");
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerify_Never_PassesIfArgumentsDontMatch() {
    mFoo.add("bobby");
    verify(mFoo, never()).add("jim");
  }

  public void testVerify_AtLeastOnce_SuceedsForOneCall() {
    mFoo.add("jim");
    verify(mFoo, atLeastOnce()).add("jim");
  }

  public void testVerify_AtLeastOnce_SuceedsForThreeCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, atLeastOnce()).add("jim");
  }

  public void testVerify_AtLeastOnce_FailsForNoCalls() {
    try {
      verify(mFoo, atLeastOnce()).add("jim");
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerify_AtLeastThreeTimes_SuceedsForThreeCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, atLeast(3)).add("jim");
  }

  public void testVerify_AtLeastThreeTimes_SuceedsForFiveCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, atLeast(3)).add("jim");
  }

  public void testVerify_AtLeastThreeTimes_FailsForTwoCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    try {
      verify(mFoo, atLeast(3)).add("jim");
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerify_AtMostThreeTimes_SuceedsForThreeCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, atMost(3)).add("jim");
  }

  public void testVerify_AtMostThreeTimes_FailsForFiveCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    try {
      verify(mFoo, atMost(3)).add("jim");
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerify_AtMostThreeTimes_SucceedsForTwoCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, atMost(3)).add("jim");
  }

  public void testVerify_AtMostThreeTimes_SucceedsForNoCalls() {
    verify(mFoo, atMost(3)).add("jim");
  }

  public void testVerify_BetweenTwoAndFour_SucceedsForTwoCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, between(2, 4)).add("jim");
  }

  public void testVerify_BetweenTwoAndFour_SucceedsForFourCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    verify(mFoo, between(2, 4)).add("jim");
  }

  public void testVerify_BetweenTwoAndFour_FailsForOneCall() {
    mFoo.add("jim");
    try {
      verify(mFoo, between(2, 4)).add("jim");
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerify_BetweenTwoAndFour_FailsForFiveCalls() {
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    mFoo.add("jim");
    try {
      verify(mFoo, LittleMock.between(2, 4)).add("jim");
      fail();
    } catch (AssertionError expected) {}
  }

  public void testDoReturnWhen_SimpleReturn() {
    doReturn("first").when(mFoo).get(0);
    assertEquals("first", mFoo.get(0));
  }

  public void testDoReturnWhen_LastStubCallWins() {
    doReturn("first").when(mFoo).get(0);
    doReturn("second").when(mFoo).get(0);
    assertEquals("second", mFoo.get(0));
  }

  public void testDoReturnWhen_CorrectStubMethodIsChosen() {
    doReturn("one").when(mFoo).get(1);
    doReturn("two").when(mFoo).get(2);
    assertEquals("one", mFoo.get(1));
    assertEquals("two", mFoo.get(2));
  }

  public void testDoReturnWhen_UnstubbedMethodStillReturnsDefault() {
    doReturn("one").when(mFoo).get(1);
    assertEquals(null, mFoo.get(2));
  }

  public void testDoReturnWhen_CalledOnString() {
    try {
      doReturn("first").when("hello").contains("something");
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testDoReturnWhen_CalledOnNonMock() {
    try {
      doReturn("first").when(createNotARealMock()).get(0);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testDoReturnWhen_CanAlsoBeVerified() {
    // Mockito home page suggests that you don't verify stubbed calls.
    // I agree.  They support it anyway.  So will I.
    doReturn("one").when(mFoo).get(8);
    mFoo.get(8);
    verify(mFoo).get(8);
  }

  public void testDoReturn_CanPassIntForIntMethod() {
    doReturn(90).when(mFoo).anInt();
    assertEquals(90, mFoo.anInt());
  }

  // Interesting, you have to explicity convert the Integer class back into an int before it
  // is happy to accept this.
  public void testDoReturn_CanPassIntegerClassForIntMethod() {
    doReturn((int) Integer.valueOf(10)).when(mFoo).anInt();
    assertEquals(10, mFoo.anInt());
  }

  public void testDoReturn_PrimitiveLong() {
    doReturn((long) Long.valueOf(10L)).when(mFoo).aLong();
    assertEquals(10L, mFoo.aLong());
  }

  public void testDoReturn_PrimitiveTypes() {
    doReturn(5).when(mFoo).anInt();
    assertEquals(5, mFoo.anInt());
    doReturn((short) 5).when(mFoo).aShort();
    assertEquals(5, mFoo.aShort());
    doReturn(true).when(mFoo).aBoolean();
    assertEquals(true, mFoo.aBoolean());
    doReturn((byte) 3).when(mFoo).aByte();
    assertEquals(3, mFoo.aByte());
    doReturn(0.6f).when(mFoo).aFloat();
    assertEquals(0.6f, mFoo.aFloat());
    doReturn(0.7).when(mFoo).aDouble();
    assertEquals(0.7, mFoo.aDouble());
    doReturn('c').when(mFoo).aChar();
    assertEquals('c', mFoo.aChar());
    assertEquals(null, mFoo.anInterface());
  }

  public void testDoThrow_SimpleException() {
    doThrow(new RuntimeException()).when(mFoo).aDouble();
    try {
      mFoo.aDouble();
      fail();
    } catch (RuntimeException expected) {}
  }

  public void testDoThrow_IfItDoesntMatchItIsntThrown() {
    doThrow(new RuntimeException()).when(mFoo).aDouble();
    mFoo.aChar();
  }

  public void testDoThrow_KeepsThrowingForever() {
    doThrow(new RuntimeException()).when(mFoo).aDouble();
    try {
      mFoo.aDouble();
      fail();
    } catch (RuntimeException expected) {}
    // This second call should also throw a RuntimeException.
    try {
      mFoo.aDouble();
      fail();
    } catch (RuntimeException expected) {}
  }

  public void testDoNothing() {
    doNothing().when(mFoo).add("first");
    mFoo.add("first");
  }

  public void testVerifyZeroInteractions_PassesWhenNothingHasHappened() {
    verifyZeroInteractions(mFoo);
  }

  public void testVerifyZeroInteractions_FailsIfSomethingHasHappened() {
    mFoo.aBoolean();
    try {
      verifyZeroInteractions(mFoo);
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerifyZeroInteractions_HappyWithMultipleArguments() {
    verifyZeroInteractions(mFoo, mBar);
  }

  public void testVerifyZeroInteractions_ShouldFailEvenIfOtherInteractionsWereFirstVerified() {
    mFoo.add("test");
    verify(mFoo).add("test");
    try {
      verifyZeroInteractions(mFoo);
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerifyEq_Method() {
    mFoo.add("test");
    verify(mFoo).add(eq("test"));
  }

  public void testVerifyEq_MethodWithTwoSameTypeParameters() {
    mBar.twoStrings("first", "test");
    verify(mBar).twoStrings(eq("first"), eq("test"));
  }

  public void testVerifyEq_MethodWithTwoDifferentTypeParameters() {
    mBar.mixedArguments(8, "test");
    verify(mBar).mixedArguments(eq(8), eq("test"));
  }

  public void testVerifyEq_MethodFailsIfNotEqual() {
    mFoo.add("bob");
    try {
      verify(mFoo).add(eq("jim"));
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerifyEq_MethodFailsIfJustOneIsNotEqual() {
    mBar.twoStrings("first", "second");
    try {
      verify(mBar).twoStrings(eq("first"), eq("third"));
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerifyEq_MethodFailsIfSameParamsButInWrongOrder() {
    mBar.twoStrings("first", "second");
    try {
      verify(mBar).twoStrings(eq("second"), eq("first"));
      fail();
    } catch (AssertionError expected) {}
  }

  public void testCapture_SimpleCapture() {
    // We verify that there are zero matchers by using the check for programming errors method.
    checkForProgrammingErrorsDuringTearDown();
    mFoo.add("test");
    verify(mFoo).add(mCaptureString.capture());
    assertEquals("test", mCaptureString.getValue());
    checkForProgrammingErrorsDuringTearDown();
  }

  public void testCapture_DuringStubbing() {
    checkForProgrammingErrorsDuringTearDown();
    doReturn("hello").when(mFoo).lookup(mCaptureString.capture());

    assertEquals("hello", mFoo.lookup("what"));
    assertEquals("what", mCaptureString.getValue());
  }

  public void testCapture_TwoCallbacksDuringStubbing() {
    checkForProgrammingErrorsDuringTearDown();
    doNothing().when(mFoo).add(mCaptureString.capture());
    doNothing().when(mFoo).getResultLater(mCaptureCallback.capture());

    mFoo.add("hi");
    assertEquals("hi", mCaptureString.getValue());

    Callback callback = createNoOpCallback();
    mFoo.getResultLater(callback);
    assertEquals(callback, mCaptureCallback.getValue());
  }

  // TODO(hugohudson): 6. Is this impossible to fix?  You can't pass a
  // createCapture().capture() into a method expecting an int, because the capture
  // method returns null, and that gets auto-boxed to Integer on the way out of the
  // capture method, then auto-unboxed into an int when being passed to the underlying
  // method, which gives the NPE.  How best can we fix this?
  // It's not like you need to anyway - there's no point / need to capture a primitive,
  // just use eq(5) for example.
  public void testCapture_NPEWhenUnboxing() {
    try {
      mBar.mixedArguments(5, "ten");
      verify(mBar).mixedArguments(mCaptureInteger.capture(), mCaptureString.capture());
      // These lines are never reached, the previous line throws an NPE.
      fail("You shouldn't be able to reach here");
      assertEquals(Integer.valueOf(5), mCaptureInteger.getValue());
      assertEquals("ten", mCaptureString.getValue());
    } catch (NullPointerException e) {
      // Expected, unfortunately.
      // Now also we're in the situation where we have some captures hanging about in the static
      // variable, which will cause the tear down of this method to fail - we can clear them
      // as follows:
      try {
        checkForProgrammingErrorsDuringTearDown();
        fail("Expected an IllegalStateException");
      } catch (IllegalStateException e2) {
        // Expected.
      }
    }
  }

  public void testCapture_MultiCapture() {
    mFoo.lookup("james");
    mFoo.add("whinny");
    mFoo.add("jessica");
    verify(mFoo).lookup(mCaptureString.capture());
    verify(mFoo, atLeastOnce()).add(mCaptureAnotherString.capture());
    assertEquals("james", mCaptureString.getValue());
    assertEquals("jessica", mCaptureAnotherString.getValue());
    assertEquals(newList("whinny", "jessica"), mCaptureAnotherString.getAllValues());
  }

  public void testAnyString() {
    doReturn("jim").when(mFoo).lookup(anyString());
    assertEquals("jim", mFoo.lookup("barney"));
  }

  public void testAnyString_ObjectArgument() {
    // It can also be passed to a method that takes object.
    mFoo.takesObject("barney");
    verify(mFoo).takesObject(anyString());
  }

  public void testAnyString_ObjectValue() {
    mFoo.takesObject(new Object());
    try {
      verify(mFoo).takesObject(anyString());
      fail();
    } catch (AssertionError expected) {}
  }

  public void testAnyObject() {
    doReturn("jim").when(mFoo).lookup((String) anyObject());
    assertEquals("jim", mFoo.lookup("barney"));
  }

  public void testAnyPrimitives() {
    mFoo.findByBoolean(true);
    mFoo.findByInt(10000);
    mFoo.findByByte((byte) 250);
    mFoo.findByShort((short) 6666);
    mFoo.findByLong(13L);
    mFoo.findByFloat(8f);
    mFoo.findByDouble(1.1);
    mFoo.findByChar('h');
    verify(mFoo).findByBoolean(anyBoolean());
    verify(mFoo).findByInt(anyInt());
    verify(mFoo).findByByte(anyByte());
    verify(mFoo).findByShort(anyShort());
    verify(mFoo).findByLong(anyLong());
    verify(mFoo).findByFloat(anyFloat());
    verify(mFoo).findByDouble(anyDouble());
    verify(mFoo).findByChar(anyChar());
  }

  public void testAnyPrimitivesWhenMatching() {
    doReturn("a").when(mFoo).findByBoolean(anyBoolean());
    doReturn("b").when(mFoo).findByInt(anyInt());
    doReturn("c").when(mFoo).findByByte(anyByte());
    doReturn("d").when(mFoo).findByShort(anyShort());
    doReturn("e").when(mFoo).findByLong(anyLong());
    doReturn("f").when(mFoo).findByFloat(anyFloat());
    doReturn("g").when(mFoo).findByDouble(anyDouble());
    doReturn("h").when(mFoo).findByChar(anyChar());
    assertEquals("a", mFoo.findByBoolean(true));
    assertEquals("b", mFoo.findByInt(388));
    assertEquals("c", mFoo.findByByte((byte) 38));
    assertEquals("d", mFoo.findByShort((short) 16));
    assertEquals("e", mFoo.findByLong(1000000L));
    assertEquals("f", mFoo.findByFloat(15.3f));
    assertEquals("g", mFoo.findByDouble(13.3));
    assertEquals("h", mFoo.findByChar('i'));
  }

  public void testReset_NoInteractionsAfterReset() {
    mFoo.aByte();
    reset(mFoo);
    verifyZeroInteractions(mFoo);
  }

  public void testReset_VerifyFailsAfterReset() {
    mFoo.aByte();
    reset(mFoo);
    try {
      verify(mFoo).aByte();
      fail();
    } catch (AssertionError expected) {}
  }

  public void testCapture_BothBeforeAndAfter() {
    doNothing().when(mFoo).add(mCaptureString.capture());
    mFoo.add("first");
    verify(mFoo).add(mCaptureAnotherString.capture());
    assertEquals("first", mCaptureString.getValue());
    assertEquals("first", mCaptureAnotherString.getValue());
  }

  public void testDoAction_NormalOperation() {
    doAnswer(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return Boolean.TRUE;
      }
    }).when(mFoo).aBoolean();
    assertEquals(true, mFoo.aBoolean());
  }

  public void testComplexSituationWithCallback() {
    // I want to specify that when hasCallback(Callback) method is called, the framework
    // should immediately call on the captured callback.
    doAnswer(new CallCapturedCallbackCallable())
        .when(mBar).getResultLater(mCaptureCallback.capture());

    // The test.
    mBar.getResultLater(new Callback() {
      @Override
      public void callMeNow() {
        mFoo.add("yes");
      }
    });

    verify(mFoo).add("yes");
  }

  public void testDoAction_CanThrowDeclaredException() throws Exception {
    doAnswer(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        throw new IOException("Problem");
      }
    }).when(mFoo).exceptionThrower();
    try {
      mFoo.exceptionThrower();
      fail();
    } catch (IOException expected) {}
  }

  public void testDoAction_CanThrowUndelcaredException() {
    doAnswer(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        throw new RuntimeException("Problem");
      }
    }).when(mFoo).aBoolean();
    try {
      mFoo.aBoolean();
      fail();
    } catch (RuntimeException expected) {}
  }

  public void testRunThisIsAnAliasForDoAction() {
    doAnswer(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return Boolean.TRUE;
      }
    }).when(mFoo).aBoolean();
    assertEquals(true, mFoo.aBoolean());
  }

  public void testVerifyingTwice() {
    // Behaviour from Mockito docs online seems to be undefined for what should happen if you
    // try to verify the same behaviour twice.
    // I'm going to make a call on this one until I have more concrete information, and my
    // call is that it is okay to verify the same thing twice - a verify doesn't "consume"
    // the other verifications.
    // Thus this will pass:
    mFoo.aByte();
    verify(mFoo).aByte();
    verify(mFoo).aByte();
  }

  public void testVerifyNoMoreInteractions_SuccessWhenNoInteractions() {
    // Not absolutely certain how this is supposed to behave.
    // My guess is that every verify "tags" all the methods it verifies against.
    // Then verifyNoMoreInteractions() will pass only if there are no "untagged" method calls.
    // Thus, for a start, no interactions will pass.
    verifyNoMoreInteractions(mFoo, mBar);
  }

  public void testVerifyNoMoreInteractions_SuccessWhenOneActionWasVerified() {
    mFoo.aBoolean();
    verify(mFoo).aBoolean();
    verifyNoMoreInteractions(mFoo, mBar);
  }

  public void testVerifyNoMoreInteractions_FailsWhenOneActionWasNotVerified() {
    mFoo.aBoolean();
    try {
      verifyNoMoreInteractions(mFoo, mBar);
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerifyNoMoreInteractions_SucceedsWhenAllActionsWereVerified() {
    mFoo.get(3);
    mFoo.get(20);
    verify(mFoo, atLeastOnce()).get(anyInt());
    verifyNoMoreInteractions(mFoo);
  }

  public void testVerifyNoMoreInteractions_FailsWhenMostButNotAllActionsWereVerified() {
    mFoo.get(3);
    mFoo.get(20);
    mFoo.aByte();
    verify(mFoo, atLeastOnce()).get(anyInt());
    try {
      verifyNoMoreInteractions(mFoo);
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerifyNoMoreInteractions_ShouldIngoreStubbedCalls() {
    doReturn("hi").when(mFoo).get(8);
    mFoo.get(8);
    verifyNoMoreInteractions(mFoo);
  }

  public void testMatchers_WrongNumberOfMatchersOnStubbingCausesError() {
    try {
      doReturn("hi").when(mBar).twoStrings("jim", eq("bob"));
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testMatchers_WrongNumberOfMatchersOnVerifyCausesError() {
    try {
      verify(mBar).twoStrings("jim", eq("bob"));
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testCreateACaptureButDontUseItShouldFailAtNextVerify() {
    // If we create a capture illegally, outside of a method call, like so:
    mCaptureString.capture();
    // Then we will have illegally created an extra matcher object that we shouldn't have
    // created that is now sitting on the stack, and that will confuse the next true method
    // call on the mock object.
    // Thus we should check in the verify() method that there are *no matchers* on the static
    // list, as this would indicate a programming error such as the above.
    try {
      verify(mFoo, anyTimes()).aBoolean();
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testCreateACaptureButDontUseItShouldFailAtNextVerify_AlsoNoMoreInteractions() {
    // Same result desired as in previous test.
    mCaptureString.capture();
    try {
      verifyNoMoreInteractions(mFoo);
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testCreateACaptureButDontUseItShouldFailAtNextVerify_AlsoZeroInteraction() {
    mCaptureString.capture();
    try {
      verifyZeroInteractions(mFoo);
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testCheckStaticVariablesMethod() {
    // To help us avoid programming errors, I'm adding a method that you can call from tear down,
    // which will explode if there is anything still left in your static variables at the end
    // of the test (so that you know you did something wrong) and that also clears that static
    // variable (so that the next test won't fail).  It should fail if we create a matcher
    // be it a capture or anything else that is then not consumed.
    anyInt();
    try {
      checkForProgrammingErrorsDuringTearDown();
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testCantPassNullToVerifyCount() {
    try {
      verify(mFoo, null).aBoolean();
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testInjectionInNestedClasses() throws Exception {
    class Outer {
      @Mock protected Foo outerMock;
    }
    class Inner extends Outer {
      @Mock protected Foo innerMock;
    }
    Inner inner = new Inner();
    assertNull(inner.innerMock);
    assertNull(inner.outerMock);
    initMocks(inner);
    assertNotNull(inner.innerMock);
    assertNotNull(inner.outerMock);
  }

  public void testIsA_Succeeds() {
    mFoo.takesObject(new Object());
    verify(mFoo).takesObject(isA(Object.class));
  }

  public void testIsA_WithSubclass() {
    mFoo.takesObject("hello");
    verify(mFoo).takesObject(isA(Object.class));
    verify(mFoo).takesObject(isA(String.class));
  }

  public void testIsA_FailsWithSuperclass() {
    mFoo.takesObject(new Object());
    try {
      verify(mFoo).takesObject(isA(String.class));
      fail();
    } catch (AssertionError expected) {}
  }

  public void testIsA_WillAcceptNull() {
    mFoo.takesObject(null);
    verify(mFoo).takesObject(isA(Object.class));
    verify(mFoo).takesObject(isA(String.class));
  }

  public void testIsA_MatchSubtype() {
    mFoo.takesBar(mBarSubtype);
    verify(mFoo).takesBar(isA(BarSubtype.class));
  }

  public void testIsA_MatchSubtypeFailed() {
    mFoo.takesBar(mBar);
    try {
      verify(mFoo).takesBar(isA(BarSubtype.class));
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerifyEquals_ShouldFail() {
    mFoo.equals(null);
    try {
      verify(mFoo).equals(null);
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerifyHashCode_ShouldFail() {
    mFoo.hashCode();
    try {
      verify(mFoo).hashCode();
      fail();
    } catch (AssertionError expected) {}
  }

  public void testVerifyToString_ShouldFail() {
    mFoo.toString();
    try {
      verify(mFoo).toString();
      fail();
    } catch (AssertionError expected) {}
  }

  public void testStubEquals_ShouldFail() {
    try {
      doReturn(false).when(mFoo).equals(null);
      fail();
    } catch (AssertionError expected) {}
  }

  public void testStubHashCode_ShouldFail() {
    try {
      doReturn(0).when(mFoo).hashCode();
      fail();
    } catch (AssertionError expected) {}
  }

  public void testStubToString_ShouldFail() {
    try {
      doReturn("party").when(mFoo).toString();
      fail();
    } catch (AssertionError expected) {}
  }

  public void testEqualsMethod_DoesntCountAsInteraction() {
    mFoo.takesBar(mBar);
    verify(mFoo).takesBar(mBar);
    verifyNoMoreInteractions(mBar);
  }

  public void testHashCodeMethod_DoesntCountAsInteraction() {
    mFoo.hashCode();
    verifyNoMoreInteractions(mFoo);
  }

  public void testToStringMethod_DoesntCountAsInteraction() {
    mFoo.toString();
    verifyNoMoreInteractions(mFoo);
  }

  public void testEquals_OnMock() {
    assertTrue(mFoo.equals(mFoo));
  }

  public void testHashCode_OnMock() {
    // The hashCode() is checked against zero, the default int value, to make sure it is actually
    // being treated differently.
    // It is possible for a hashCode() to be zero, but very unlikely.
    assertNotSame(0, mFoo.hashCode());
  }

  public void testToString_OnMock() {
    assertTrue(mFoo.toString().contains(Foo.class.getName()));
  }

  public void testErrorMessages_NoArgMethodAndNoInteractions() {
    /* I would like the error message to look like this:
     * Expected exactly 2 calls to:
     *   mFoo.aBoolean()
     *   at the.line.where.the.verify.happened:xxx
     *
     * No method calls happened to this mock
     */
    int verifyLineNumber = 0;
    try {
      verifyLineNumber = getNextLineNumber();
      verify(mFoo, times(2)).aBoolean();
      fail("Should have thrown an assertion error");
    } catch (AssertionError e) {
      // Good, verify that the message is exactly as expected.
      String expectedMessage =
          "\nExpected exactly 2 calls to:\n"
          + "  mFoo.aBoolean()\n"
          + "  at " + singleLineStackTrace(verifyLineNumber) + "\n"
          + "\n"
          + "No method calls happened on this mock\n";
      assertEquals(expectedMessage, e.getMessage());
    }
  }

  public void testErrorMessages_SomeArgsMethodAndSomeInteractions() {
    /* I would like the error message to look like this:
     * Expected exactly 1 call to:
     *   mFoo.add(String)
     *   at the.line.where.the.verify.happened:xxx
     *
     * Method calls that did happen:
     *   mFoo.aByte()
     *   at the.line.where.the.byte.happened:xxx
     *   mFoo.findByBoolean(boolean)
     *   at the line.where.the.boolean.happened:xxx
     */
    int aByteLineNumber = 0;
    int findByBooleanLineNumber = 0;
    int verifyLineNumber = 0;
    try {
      aByteLineNumber = getNextLineNumber();
      mFoo.aByte();
      findByBooleanLineNumber = getNextLineNumber();
      mFoo.findByBoolean(true);
      verifyLineNumber = getNextLineNumber();
      verify(mFoo).add("jim");
      fail("Should have thrown an assertion error");
    } catch (AssertionError e) {
      // Good, verify that the message is exactly as expected.
      String expectedMessage =
          "\nExpected exactly 1 call to:\n"
          + "  mFoo.add(String)\n"
          + "  at " + singleLineStackTrace(verifyLineNumber) + "\n"
          + "\n"
          + "Method calls that did happen:\n"
          + "  mFoo.aByte()\n"
          + "  at " + singleLineStackTrace(aByteLineNumber) + "\n"
          + "  mFoo.findByBoolean(boolean)\n"
          + "  at " + singleLineStackTrace(findByBooleanLineNumber) + "\n";
      assertEquals(expectedMessage, e.getMessage());
    }
  }

  public void testErrorMessage_DoReturnExplainsWhatWentWrong() {
    /* I would like the error message to look like this:
     * Can't return Long from stub for:
     *   (int) mFoo.anInt()
     *   at the.line.where.the.assignment.happened:xxx
     */
    int lineNumber = 0;
    try {
      lineNumber = getNextLineNumber();
      doReturn(10L).when(mFoo).anInt();
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Good, expected, verify the message.
      String expectedMessage =
          "\nCan't return Long from stub for:\n"
          + "  (int) mFoo.anInt()\n"
          + "  at " + singleLineStackTrace(lineNumber) + "\n";
      assertEquals(expectedMessage, e.getMessage());
    }
  }

  public void testErrorMessage_DoReturnAlsoHasGoodErrorMessageForVoidMethods() {
    /* I would like the error message to look like this:
     * Can't return String from stub for:
     *   (void) mFoo.add(String)
     *   at the.line.where.the.assignment.happened:xxx
     */
    int lineNumber = 0;
    try {
      lineNumber = getNextLineNumber();
      doReturn("hello").when(mFoo).add("jim");
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Good, expected, verify the message.
      String expectedMessage =
          "\nCan't return String from stub for:\n"
          + "  (void) mFoo.add(String)\n"
          + "  at " + singleLineStackTrace(lineNumber) + "\n";
      assertEquals(expectedMessage, e.getMessage());
    }
  }

  public void testDoReturn_ThisShouldFailSinceDoubleIsNotAString() {
    try {
      doReturn("hello").when(mFoo).aDouble();
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testDoReturn_ThisShouldPassSinceStringCanBeReturnedFromObjectMethod() {
    doReturn("hello").when(mFoo).anObject();
  }

  public void testDoReturn_ThisShouldFailSinceObjectCantBeReturnedFromString() {
    try {
      doReturn(new Object()).when(mFoo).aString();
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testDoReturn_ThisShouldFailSinceBarIsNotSubtypeOfBarSubtype() {
    try {
      doReturn(mBar).when(mFoo).aBarSubtype();
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testDoReturn_ThisShouldPassSinceBarSubTypeIsABar() {
    doReturn(mBarSubtype).when(mFoo).aBar();
  }

  // TODO(hugohudson): 7. Should fix this.
//  @ShouldThrow(IllegalArgumentException.class)
  public void testDoReturn_ThisShouldFailBecauseNullIsNotAByte() {
    doReturn(null).when(mFoo).aByte();
  }

  // TODO(hugohudson): 7. Should fix this.
  // Once we fix the previous test we won't need this one.
  // I'm just demonstrating that currently this fails with NPE at use-time not at stub-time.
  public void testDoReturn_ThisShouldFailBecauseNullIsNotAByte2() {
    doReturn(null).when(mFoo).aByte();
    try {
      mFoo.aByte();
      fail();
    } catch (NullPointerException expected) {}
  }

  public void testDoReturn_ThisShouldPassSinceNullIsAnObject() {
    doReturn(null).when(mFoo).anObject();
  }

  // TODO(hugohudson): 7. Should fix this.
  // At present we aren't catching this, and would have difficulty doing so since we don't know
  // the type of the callable.
//  @ShouldThrow(IllegalArgumentException.class)
  public void testDoAnswer_ThisShouldFailSinceStringIsNotAByte() {
    doAnswer(new Callable<String>() {
      @Override public String call() throws Exception { return "hi"; }
    }).when(mFoo).aByte();
  }

  // TODO(hugohudson): 7. Should fix this to give proper message.
  // We could at least give a good message saying why you get failure - saying that your string
  // is not a byte, and pointing to where you stubbed it.
  public void testDoAnswer_ThisShouldFailSinceStringIsNotAByte2() {
    doAnswer(new Callable<String>() {
      @Override public String call() throws Exception { return "hi"; }
    }).when(mFoo).aByte();
    try {
      mFoo.aByte();
      fail();
    } catch (ClassCastException expected) {}
  }

  public void testDoAnswer_ShouldHaveSimpleNameOnReturnValue() {
    try {
      doReturn("hi").when(mFoo).aBar();
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testCantCreateMockOfNullType() {
    try {
      mock(null);
      fail();
    } catch (IllegalArgumentException expected) {}
  }

  public void testCreateMockWithNullFieldName() {
    OnClickListener mockClickListener = mock(OnClickListener.class);
    try {
      verify(mockClickListener).onClick(null);
      fail();
    } catch (AssertionError expected) {}
  }

  public void testDoubleVerifyNoProblems() {
    // Reusing a mock after a verify should be fine.
    // There was a bug with this, let's check it doesn't regress.
    mFoo.aBar();
    verify(mFoo).aBar();

    mFoo.aByte();
    verify(mFoo).aByte();
  }

  public void testUnfinishedVerifyCaughtInTearDown_Issue5() {
    verify(mFoo);
    try {
      checkForProgrammingErrorsDuringTearDown();
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testUnfinishedWhenCaughtInTearDown_Issue5() {
    doThrow(new RuntimeException()).when(mFoo);
    try {
      checkForProgrammingErrorsDuringTearDown();
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testUnfinishedVerifyCaughtByStartingWhen_Issue5() {
    verify(mFoo, never());
    try {
      doReturn(null).when(mFoo).clear();
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testUnfinishedWhenCaughtByStartingVerify_Issue5() {
    doThrow(new RuntimeException()).when(mFoo);
    try {
      verify(mFoo).clear();
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testSecondUnfinishedVerifyShouldFailImmediately() throws Exception {
    verify(mFoo);
    try {
      verify(mFoo);
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testSecondUnfinishedWhenShouldFailImmediately() throws Exception {
    doReturn(null).when(mFoo);
    try {
      doReturn(null).when(mFoo);
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testVerifyWithTimeout_SuccessCase() throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    invokeBarMethodAfterLatchAwait(countDownLatch);
    doReturn(null).when(mFoo).aBar();
    verify(mFoo, never()).aBar();
    countDownLatch.countDown();
    verify(mFoo, timeout(100)).aBar();
  }

  public void testVerifyWithTimeout_FailureCase() throws Exception {
    long start = System.currentTimeMillis();
    try {
      verify(mFoo, timeout(10)).aBar();
      fail();
    } catch (AssertionError expected) {}
    long duration = System.currentTimeMillis() - start;
    assertTrue(duration > 5);
  }

  public void testVerifyWithTimeoutMultipleInvocations_SuccessCase() throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    invokeBarMethodAfterLatchAwait(countDownLatch);
    invokeBarMethodAfterLatchAwait(countDownLatch);
    doReturn(null).when(mFoo).aBar();
    verify(mFoo, never()).aBar();
    countDownLatch.countDown();
    verify(mFoo, timeout(100).times(2)).aBar();
    verify(mFoo, timeout(100).atLeast(2)).aBar();
    verify(mFoo, timeout(100).between(2, 4)).aBar();
    verify(mFoo, timeout(100).atLeastOnce()).aBar();
  }

  public void testVerifyWithTimeoutMultipleInvocations_FailureCase() throws Exception {
    long start = System.currentTimeMillis();
    mFoo.aBar();
    try {
      verify(mFoo, timeout(10).between(2, 3)).aBar();
      fail();
    } catch (AssertionError expected) {}
    long duration = System.currentTimeMillis() - start;
    assertTrue(duration > 5);

  }

  public void testConcurrentModificationExceptions() throws Exception {
    // This test concurrently stubs, verifies, and uses a mock.
    // It used to totally reproducibly throw a ConcurrentModificationException.
    Future<?> task1 = mExecutorService.submit(new Runnable() {
      @Override
      public void run() {
        while (!Thread.currentThread().isInterrupted()) {
          mFoo.aBar();
          Thread.yield();
        }
      }
    });
    Future<?> task2 = mExecutorService.submit(new Runnable() {
        @Override
        public void run() {
          while (!Thread.currentThread().isInterrupted()) {
            verify(mFoo, anyTimes()).aBar();
            Thread.yield();
          }
        }
      });
    Thread.sleep(20);
    task1.cancel(true);
    task2.cancel(true);
    try {
      task1.get();
      fail();
    } catch (CancellationException expected) {}
    try {
      task2.get();
      fail();
    } catch (CancellationException expected) {}
  }

  public void testCannotVerifyFromSecondThreadAfterStubbingInFirst() throws Exception {
    doReturn(null).when(mFoo).aBar();
    Future<?> submit = mExecutorService.submit(new Runnable() {
      @Override
      public void run() {
        verify(mFoo, anyTimes()).aBar();
      }
    });
    try {
      submit.get();
      fail();
    } catch (ExecutionException expected) {
      assertTrue(expected.getCause() instanceof IllegalStateException);
    }
  }

  public void testCannotStubFromSecondThreadAfterVerifyingInFirst() throws Exception {
    mExecutorService.submit(new Runnable() {
      @Override
      public void run() {
        verify(mFoo, anyTimes()).aBar();
      }
    }).get();
    try {
      doReturn(null).when(mFoo).aBar();
      fail();
    } catch (IllegalStateException expected) {}
  }

  public void testCustomMatcher() {
    ArgumentMatcher argumentMatcher = new ArgumentMatcher() {
      @Override
      public boolean matches(Object value) {
        return ((String) value).contains("[]");
      }
    };
    mFoo.add("as[]df");
    mFoo.add("qwer[]asdf");
    mFoo.add("1234");
    verify(mFoo, times(3)).add(anyString());
    verify(mFoo, times(2)).add((String) matches(argumentMatcher));
  }

  public void testInorderExample_Success() {
    @SuppressWarnings("unchecked")
    List<String> firstMock = mock(List.class);
    @SuppressWarnings("unchecked")
    List<String> secondMock = mock(List.class);
    firstMock.add("was called first");
    secondMock.add("was called second");
    InOrder inOrder = inOrder(firstMock, secondMock);
    inOrder.verify(firstMock).add("was called first");
    inOrder.verify(secondMock).add("was called second");
  }

  public void testInorderExample_Failure() {
    @SuppressWarnings("unchecked")
    List<String> firstMock = mock(List.class);
    @SuppressWarnings("unchecked")
    List<String> secondMock = mock(List.class);
    firstMock.add("was called first");
    secondMock.add("was called second");
    InOrder inOrder = inOrder(firstMock, secondMock);
    inOrder.verify(secondMock).add("was called second");
    try {
      inOrder.verify(firstMock).add("was called first");
      throw new IllegalStateException();
    } catch (AssertionError expected) {}
  }

  public void testInorderInterleave() {
    @SuppressWarnings("unchecked")
    List<String> firstMock = mock(List.class);
    firstMock.add("a");
    firstMock.add("b");
    firstMock.add("a");

    // Should be fine to verify a then b, since they happened in that order.
    InOrder inOrder = inOrder(firstMock);
    inOrder.verify(firstMock).add("a");
    inOrder.verify(firstMock).add("b");

    // Should also be fine to inorder verify the other way around, they happened in that order too.
    inOrder = inOrder(firstMock);
    inOrder.verify(firstMock).add("b");
    inOrder.verify(firstMock).add("a");

    // Should be fine to verify "a, b, a" since that too happened.
    inOrder = inOrder(firstMock);
    inOrder.verify(firstMock).add("a");
    inOrder.verify(firstMock).add("b");
    inOrder.verify(firstMock).add("a");

    // "a, a, b" did not happen.
    inOrder = inOrder(firstMock);
    inOrder.verify(firstMock).add("a");
    inOrder.verify(firstMock).add("a");
    try {
      inOrder.verify(firstMock).add("b");
      throw new IllegalStateException();
    } catch (AssertionError expected) {}

    // "b, a, b" did not happen.
    inOrder = inOrder(firstMock);
    inOrder.verify(firstMock).add("b");
    inOrder.verify(firstMock).add("a");
    try {
      inOrder.verify(firstMock).add("b");
      throw new IllegalStateException();
    } catch (AssertionError expected) {}

    // "b" did not happen twice.
    inOrder = inOrder(firstMock);
    inOrder.verify(firstMock).add("b");
    try {
      inOrder.verify(firstMock).add("b");
      throw new IllegalStateException();
    } catch (AssertionError expected) {}
  }

  public void testInorderComplicatedExample() {
    // TODO: I'm currently totally ignoring the parameters passed to the inorder method.
    // I don't understand what the point of them is, anyway.
    @SuppressWarnings("unchecked")
    List<String> firstMock = mock(List.class);
    @SuppressWarnings("unchecked")
    List<String> secondMock = mock(List.class);

    firstMock.add("1");
    secondMock.add("2");
    firstMock.add("3");
    secondMock.add("4");

    InOrder allInOrder = inOrder(firstMock, secondMock);
    allInOrder.verify(firstMock).add("1");
    allInOrder.verify(secondMock).add("2");
    allInOrder.verify(firstMock).add("3");
    allInOrder.verify(secondMock).add("4");

    InOrder firstInOrder = inOrder(firstMock, secondMock);
    firstInOrder.verify(firstMock).add("1");
    firstInOrder.verify(firstMock).add("3");
    try {
      firstInOrder.verify(secondMock).add("2");
      throw new IllegalStateException();
    } catch (AssertionError expected) {}
    firstInOrder.verify(secondMock).add("4");

    InOrder secondInOrder = inOrder(firstMock, secondMock);
    secondInOrder.verify(secondMock).add("2");
    secondInOrder.verify(secondMock).add("4");
    try {
      secondInOrder.verify(firstMock).add("1");
      throw new IllegalStateException();
    } catch (AssertionError expected) {}
    try {
      secondInOrder.verify(firstMock).add("3");
      throw new IllegalStateException();
    } catch (AssertionError expected) {}
  }

  public static class Jim {
    public int bob() {
      fail();
      return 3;
    }
  }

  // Does not work on JVM, android only.
  public void testMockingConcreteClasses() throws Exception {
    Jim mock = mock(Jim.class);
    assertEquals(0, mock.bob());
    doReturn(8).when(mock).bob();
    assertEquals(8, mock.bob());
  }

  private Future<Void> invokeBarMethodAfterLatchAwait(final CountDownLatch countDownLatch) {
    return mExecutorService.submit(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        countDownLatch.await();
        mFoo.aBar();
        return null;
      }
    });
  }

  // TODO(hugohudson): 5. Every method that throws exceptions could be improved by adding
  // test for the content of the error message.

  // TODO(hugohudson): 5. Make the doReturn() method take variable arguments.
  // The syntax is:
  // doReturn(1, 2, 3).when(mFoo).anInt();
  // And of course means that the method returns 1 the first time, 2, the second and 3 the third.
  // Note that this doesn't imply verification, and I presume that the 3 is always returned for
  // the 4th and subsequent times.

  // TODO(hugohudson): 6. Could also offer a nicer syntax for multiple returns like this:
  // How about doReturn().thenThrow().thenReturn().when(mFoo).aDouble();

  // TODO(hugohudson): 5. Get around to implementing Mockito's when() syntax.
  // I don't really like it, because it means a lot more static nonsense, with yet more
  // opportunities to shoot oneself in the foot.
  // Plus, where's the upside in more than one way to do the same thing - it just gets confusing.
  // But, I imagine that plenty of people who are familiar with Mockito will want this, so I
  // probably should do it, plus there's a good argument that it allows typechecking of the
  // method calls, so I guess we probably should.  Looks like this:
  // when(mock.foo(0)).thenReturn(1);
  // when(mock.foo(1)).thenThrow(new RuntimeException)
  // when(mock.foo(anyInt())).thenReturn("bar")
  // when(mock.foo(argThat(isValid()))).thenReturn("element")
  // when(mock.someMethod("some arg")).thenThrow(new RuntimeException()).thenReturn("foo");
  // when(mock.someMethod("some arg")).thenReturn("one", "two", "three");
  // when(mock.someMethod(anyString())).thenAnswer(new Answer() {
  //   @Override
  //   Object answer(InvocationOnMock invocation) {
  //     Object[] args = invocation.getArguments();
  //     Object mock = invocation.getMock();
  //     return "called with arguments: " + args;
  //   }
  // }

  // TODO(hugohudson): 6. Again we can chain things like doNothing() then doThrow() I suppose.
  // doNothing().doThrow(new RuntimeException()).when(mock).someVoidMethod();

  // TODO(hugohudson): 6. I really like the idea of implementing the Spy, which is a wrapper on
  // a real object and delegates all calls to that real object, but allows you to intercept
  // the ones that you want to.
  // Sounds like it will be particularly good for testing legacy code.
  // But also wouldn't be so much use without us being able to mock concrete classes, which I
  // imagine is not on the cards for a while yet.

  // TODO(hugohudson): 6. Could possibly look into more aliases for the common methods, so that
  // you can do the 'given... when... assertThat...' pattern as follows:
  //  //given
  //  given(seller.askForBread()).willReturn(new Bread());
  //  //when
  //  Goods goods = shop.buyBread();
  //  //then
  //  assertThat(goods, containBread());

  // TODO: All unfinished verify and when statements should have sensible error messages telling
  // you where the unfinished statement comes from.

  /** Returns the line number of the line following the method call. */
  private int getNextLineNumber() {
    return new Exception().getStackTrace()[1].getLineNumber() + 1;
  }

  /** Returns a string like: "com.google.foo.TestFoo.testMethod(TestFoo:50)" */
  private String singleLineStackTrace(int lineNumber) {
    return getClass().getName() + "." + getName() + "(" + getClass().getSimpleName() +
        ".java:" + lineNumber + ")";
  }

  /** Simple callable that invokes the callback captured in the callback member variable. */
  private class CallCapturedCallbackCallable implements Callable<Object> {
    @Override
    public Object call() {
      mCaptureCallback.getValue().callMeNow();
      return null;
    }
  }

  private Foo createNotARealMock() {
    InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
      }
    };
    Foo notARealMock = (Foo) Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class<?>[]{ Foo.class }, handler);
    assertNotNull(notARealMock);
    return notARealMock;
  }

  private static Callback createNoOpCallback() {
    return new Callback() {
      @Override
      public void callMeNow() {
      }
    };
  }

  private static <T> List<T> newList(T... things) {
    ArrayList<T> list = new ArrayList<T>();
    for (T thing : things) {
      list.add(thing);
    }
    return list;
  }
}
