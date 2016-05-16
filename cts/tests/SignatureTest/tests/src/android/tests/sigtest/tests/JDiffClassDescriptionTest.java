/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.tests.sigtest.tests;

import android.test.InstrumentationTestCase;
import android.tests.sigtest.JDiffClassDescription;
import android.tests.sigtest.ResultObserver;
import android.tests.sigtest.SignatureTest.FAILURE_TYPE;

import java.lang.reflect.Modifier;

/**
 * Test class for JDiffClassDescription.
 */
public class JDiffClassDescriptionTest extends InstrumentationTestCase {
    private class NoFailures implements ResultObserver {
        public void notifyFailure(FAILURE_TYPE type, String name, String errmsg) {
            JDiffClassDescriptionTest.this.fail("Saw unexpected test failure: " + name + " failure type: " + type);
        }
    }

    private class ExpectFailure implements ResultObserver {
        private FAILURE_TYPE expectedType;
        private boolean failureSeen;

        public ExpectFailure(FAILURE_TYPE expectedType) {
            this.expectedType = expectedType;
        }

        public void notifyFailure(FAILURE_TYPE type, String name, String errMsg) {
            if (type == expectedType) {
                if (failureSeen) {
                    JDiffClassDescriptionTest.this.fail("Saw second test failure: " + name + " failure type: " + type);
                } else {
                    // We've seen the error, mark it and keep going
                    failureSeen = true;
                }
            } else {
                JDiffClassDescriptionTest.this.fail("Saw unexpected test failure: " + name + " failure type: " + type);
            }
        }
        
        public void validate() {
            JDiffClassDescriptionTest.this.assertTrue(failureSeen);
        }
    }

    /**
     * Create the JDiffClassDescription for "NormalClass".
     *
     * @return the new JDiffClassDescription
     */
    private JDiffClassDescription createNormalClass() {
        return createNormalClass(new NoFailures());
    }

    /**
     * Create the JDiffClassDescription for "NormalClass".
     *
     * @return the new JDiffClassDescription
     */
    private JDiffClassDescription createNormalClass(ResultObserver observer) {
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", "NormalClass", observer);
        clz.setType(JDiffClassDescription.JDiffType.CLASS);
        clz.setModifier(Modifier.PUBLIC);
        return clz;
    }    

    public void testNormalClassCompliance() {
        JDiffClassDescription clz = createNormalClass();
        clz.checkSignatureCompliance();
        assertEquals(clz.toSignatureString(),
        "public class NormalClass");
    }

    public void testMissingClass() {
        ExpectFailure observer = new ExpectFailure(FAILURE_TYPE.MISSING_CLASS);
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data",
                "NoSuchClass",
                observer);
        clz.setType(JDiffClassDescription.JDiffType.CLASS);
        clz.checkSignatureCompliance();
        observer.validate();
    }

    public void testSimpleConstructor() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffConstructor constructor = new JDiffClassDescription.JDiffConstructor("NormalClass", Modifier.PUBLIC);
        clz.addConstructor(constructor);
        clz.checkSignatureCompliance();
        assertEquals(constructor.toSignatureString(), "public NormalClass()");
    }
    public void testOneArgConstructor() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffConstructor constructor = new JDiffClassDescription.JDiffConstructor("NormalClass", Modifier.PRIVATE);
        constructor.addParam("java.lang.String");
        clz.addConstructor(constructor);
        clz.checkSignatureCompliance();
        assertEquals(constructor.toSignatureString(), "private NormalClass(java.lang.String)");
    }
    public void testConstructorThrowsException() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffConstructor constructor = new JDiffClassDescription.JDiffConstructor("NormalClass", Modifier.PROTECTED);
        constructor.addParam("java.lang.String");
        constructor.addParam("java.lang.String");
        constructor.addException("android.tests.sigtest.tests.data.NormalException");
        clz.addConstructor(constructor);
        clz.checkSignatureCompliance();
        assertEquals(constructor.toSignatureString(), "protected NormalClass(java.lang.String, " +
                "java.lang.String) throws android.tests.sigtest.tests.data.NormalException");
    }
    public void testPackageProtectedConstructor() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffConstructor constructor = new JDiffClassDescription.JDiffConstructor("NormalClass", 0);
        constructor.addParam("java.lang.String");
        constructor.addParam("java.lang.String");
        constructor.addParam("java.lang.String");
        clz.addConstructor(constructor);
        clz.checkSignatureCompliance();
        assertEquals(constructor.toSignatureString(), "NormalClass(java.lang.String, java.lang.String, java.lang.String)");
    }

    public void testStaticMethod() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffMethod method = new JDiffClassDescription.JDiffMethod("staticMethod", Modifier.STATIC | Modifier.PUBLIC, "void");
        clz.addMethod(method);
        clz.checkSignatureCompliance();
        assertEquals(method.toSignatureString(), "public static void staticMethod()");
    }
    public void testSyncMethod() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffMethod method = new JDiffClassDescription.JDiffMethod("syncMethod", Modifier.SYNCHRONIZED | Modifier.PUBLIC, "void");
        clz.addMethod(method);
        clz.checkSignatureCompliance();
        assertEquals(method.toSignatureString(), "public synchronized void syncMethod()");
    }
    public void testPackageProtectMethod() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffMethod method = new JDiffClassDescription.JDiffMethod("packageProtectedMethod", 0, "boolean");
        clz.addMethod(method);
        clz.checkSignatureCompliance();
        assertEquals(method.toSignatureString(), "boolean packageProtectedMethod()");
    }
    public void testPrivateMethod() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffMethod method = new JDiffClassDescription.JDiffMethod("privateMethod", Modifier.PRIVATE, "void");
        clz.addMethod(method);
        clz.checkSignatureCompliance();
        assertEquals(method.toSignatureString(), "private void privateMethod()");
    }
    public void testProtectedMethod() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffMethod method = new JDiffClassDescription.JDiffMethod("protectedMethod", Modifier.PROTECTED, "java.lang.String");
        clz.addMethod(method);
        clz.checkSignatureCompliance();
        assertEquals(method.toSignatureString(), "protected java.lang.String protectedMethod()");
    }
    public void testThrowsMethod() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffMethod method = new JDiffClassDescription.JDiffMethod("throwsMethod", Modifier.PUBLIC, "void");
        method.addException("android.tests.sigtest.tests.data.NormalException");
        clz.addMethod(method);
        clz.checkSignatureCompliance();
        assertEquals(method.toSignatureString(), "public void throwsMethod() throws" + 
                " android.tests.sigtest.tests.data.NormalException");
    }
    public void testNativeMethod() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffMethod method = new JDiffClassDescription.JDiffMethod("nativeMethod", Modifier.PUBLIC | Modifier.NATIVE, "void");
        clz.addMethod(method);
        clz.checkSignatureCompliance();
        assertEquals(method.toSignatureString(), "public native void nativeMethod()");
    }

    public void testFinalField() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffField field = new JDiffClassDescription.JDiffField("FINAL_FIELD", "java.lang.String", Modifier.PUBLIC | Modifier.FINAL);
        clz.addField(field);
        clz.checkSignatureCompliance();
        assertEquals(field.toSignatureString(), "public final java.lang.String FINAL_FIELD");
    }
    public void testStaticField() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffField field = new JDiffClassDescription.JDiffField("STATIC_FIELD", "java.lang.String", Modifier.PUBLIC | Modifier.STATIC);
        clz.addField(field);
        clz.checkSignatureCompliance();
        assertEquals(field.toSignatureString(), "public static java.lang.String STATIC_FIELD");
    }
    public void testVolatileFiled() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffField field = new JDiffClassDescription.JDiffField("VOLATILE_FIELD", "java.lang.String", Modifier.PUBLIC | Modifier.VOLATILE);
        clz.addField(field);
        clz.checkSignatureCompliance();
        assertEquals(field.toSignatureString(), "public volatile java.lang.String VOLATILE_FIELD");
    }
    public void testTransientField() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffField field = new JDiffClassDescription.JDiffField("TRANSIENT_FIELD", "java.lang.String", Modifier.PUBLIC | Modifier.TRANSIENT);
        clz.addField(field);
        clz.checkSignatureCompliance();
        assertEquals(field.toSignatureString(), "public transient java.lang.String TRANSIENT_FIELD");
    }
    public void testPacakgeField() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffField field = new JDiffClassDescription.JDiffField("PACAKGE_FIELD", "java.lang.String", 0);
        clz.addField(field);
        clz.checkSignatureCompliance();
        assertEquals(field.toSignatureString(), "java.lang.String PACAKGE_FIELD");
    }
    public void testPrivateField() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffField field = new JDiffClassDescription.JDiffField("PRIVATE_FIELD", "java.lang.String", Modifier.PRIVATE);
        clz.addField(field);
        clz.checkSignatureCompliance();
        assertEquals(field.toSignatureString(), "private java.lang.String PRIVATE_FIELD");
    }
    public void testProtectedField() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffField field = new JDiffClassDescription.JDiffField("PROTECTED_FIELD", "java.lang.String", Modifier.PROTECTED);
        clz.addField(field);
        clz.checkSignatureCompliance();
        assertEquals(field.toSignatureString(), "protected java.lang.String PROTECTED_FIELD");
    }

    public void testInnerClass() {
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", "NormalClass.InnerClass", new NoFailures());
        clz.setType(JDiffClassDescription.JDiffType.CLASS);
        clz.setModifier(Modifier.PUBLIC);
        JDiffClassDescription.JDiffField field = new JDiffClassDescription.JDiffField("innerClassData", "java.lang.String", Modifier.PRIVATE);
        clz.addField(field);
        clz.checkSignatureCompliance();
        assertEquals(clz.toSignatureString(), "public class NormalClass.InnerClass");
    }
    public void testInnerInnerClass() {
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", "NormalClass.InnerClass.InnerInnerClass", new NoFailures());
        clz.setType(JDiffClassDescription.JDiffType.CLASS);
        clz.setModifier(Modifier.PUBLIC);
        JDiffClassDescription.JDiffField field = new JDiffClassDescription.JDiffField("innerInnerClassData", "java.lang.String", Modifier.PRIVATE);
        clz.addField(field);
        clz.checkSignatureCompliance();
        assertEquals(clz.toSignatureString(), "public class NormalClass.InnerClass.InnerInnerClass");
    }
    public void testInnerInterface() {
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", "NormalClass.InnerInterface", new NoFailures());
        clz.setType(JDiffClassDescription.JDiffType.INTERFACE);
        clz.setModifier(Modifier.PUBLIC | Modifier.STATIC | Modifier.ABSTRACT);
        clz.addMethod(new JDiffClassDescription.JDiffMethod("doSomething", Modifier.PUBLIC, "void"));
        clz.checkSignatureCompliance();
        assertEquals(clz.toSignatureString(), "public interface NormalClass.InnerInterface");
    }
    public void testInterface() {
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", "NormalInterface", new NoFailures());
        clz.setType(JDiffClassDescription.JDiffType.INTERFACE);
        clz.setModifier(Modifier.PUBLIC | Modifier.ABSTRACT);
        clz.addMethod(new JDiffClassDescription.JDiffMethod("doSomething", Modifier.PUBLIC, "void"));
        clz.checkSignatureCompliance();
        assertEquals(clz.toSignatureString(), "public interface NormalInterface");
    }
    public void testFinalClass() {
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", "FinalClass", new NoFailures());
        clz.setType(JDiffClassDescription.JDiffType.CLASS);
        clz.setModifier(Modifier.PUBLIC | Modifier.FINAL);
        clz.checkSignatureCompliance();
        assertEquals(clz.toSignatureString(), "public final class FinalClass");
    }
    /** Test the case where the API declares the method not
     *  synchronized, but it actually is. */
    public void testAddingSync() {
        ExpectFailure observer = new ExpectFailure(FAILURE_TYPE.MISMATCH_METHOD);
        JDiffClassDescription clz = createNormalClass(observer);
        JDiffClassDescription.JDiffMethod method = new JDiffClassDescription.JDiffMethod("syncMethod", Modifier.PUBLIC, "void");
        clz.addMethod(method);
        clz.checkSignatureCompliance();
        observer.validate();
    }
    /** Test the case where the API declares the method is
     *  synchronized, but it actually is not. */
    public void testRemovingSync() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffMethod method = new JDiffClassDescription.JDiffMethod("notSyncMethod", Modifier.SYNCHRONIZED | Modifier.PUBLIC, "void");
        clz.addMethod(method);
        clz.checkSignatureCompliance();
    }
    /** API says method is not native, but it actually is.
     *  http://b/1839558
     */
    public void testAddingNative() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffMethod method = new JDiffClassDescription.JDiffMethod("nativeMethod", Modifier.PUBLIC, "void");
        clz.addMethod(method);
        clz.checkSignatureCompliance();
    }
    /** API says method is native, but actually isn't.
     *  http://b/1839558
     */
    public void testRemovingNative() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffMethod method = new JDiffClassDescription.JDiffMethod("notNativeMethod", Modifier.NATIVE | Modifier.PUBLIC, "void");
        clz.addMethod(method);
        clz.checkSignatureCompliance();
    }
    public void testAbstractClass() {
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", "AbstractClass", new NoFailures());
        clz.setType(JDiffClassDescription.JDiffType.CLASS);
        clz.setModifier(Modifier.PUBLIC | Modifier.ABSTRACT);
        clz.checkSignatureCompliance();
        assertEquals(clz.toSignatureString(), "public abstract class AbstractClass");
    }
    /** API lists class as abstract, reflection does not.
     * http://b/1839622
     */
    public void testRemovingAbstractFromAClass() {
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", "NormalClass", new NoFailures());
        clz.setType(JDiffClassDescription.JDiffType.CLASS);
        clz.setModifier(Modifier.PUBLIC | Modifier.ABSTRACT);
        clz.checkSignatureCompliance();
    }
    /** reflection lists class as abstract, api does not.
     * http://b/1839622
     */
    public void testAddingAbstractToAClass() {
        ExpectFailure observer = new ExpectFailure(FAILURE_TYPE.MISMATCH_CLASS);
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data",
                "AbstractClass", 
                observer);
        clz.setType(JDiffClassDescription.JDiffType.CLASS);
        clz.setModifier(Modifier.PUBLIC);
        clz.checkSignatureCompliance();
        observer.validate();
    }
    public void testFinalMethod() {
        JDiffClassDescription clz = createNormalClass();
        JDiffClassDescription.JDiffMethod method = new JDiffClassDescription.JDiffMethod("finalMethod", Modifier.PUBLIC | Modifier.FINAL, "void");
        clz.addMethod(method);
        clz.checkSignatureCompliance();
        assertEquals(method.toSignatureString(), "public final void finalMethod()");
    }
    /**
     * Final Class, API lists methods as non-final, reflection has it as final.
     * http://b/1839589
     */
    public void testAddingFinalToAMethodInAFinalClass() {
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", "FinalClass", new NoFailures());
        clz.setType(JDiffClassDescription.JDiffType.CLASS);
        clz.setModifier(Modifier.PUBLIC | Modifier.FINAL);
        JDiffClassDescription.JDiffMethod method = new JDiffClassDescription.JDiffMethod("finalMethod", Modifier.PUBLIC, "void");
        clz.addMethod(method);
        clz.checkSignatureCompliance();        
    }
    /**
     * Final Class, API lists methods as final, reflection has it as non-final.
     * http://b/1839589
     */
    public void testRemovingFinalToAMethodInAFinalClass() {
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", "FinalClass", new NoFailures());
        clz.setType(JDiffClassDescription.JDiffType.CLASS);
        clz.setModifier(Modifier.PUBLIC | Modifier.FINAL);
        JDiffClassDescription.JDiffMethod method = new JDiffClassDescription.JDiffMethod("nonFinalMethod", 
                Modifier.PUBLIC | Modifier.FINAL, 
                "void");
        clz.addMethod(method);
        clz.checkSignatureCompliance();        
    }
    /**
     * non-final Class, API lists methods as non-final, reflection has it as final.
     * http://b/1839589
     */  
    public void testAddingFinalToAMethodInANonFinalClass() {
        ExpectFailure observer = new ExpectFailure(FAILURE_TYPE.MISMATCH_METHOD);
        JDiffClassDescription clz = new JDiffClassDescription("android.tests.sigtest.tests.data", 
                "NormalClass", 
                observer);
        clz.setType(JDiffClassDescription.JDiffType.CLASS);
        clz.setModifier(Modifier.PUBLIC);
        JDiffClassDescription.JDiffMethod method = new JDiffClassDescription.JDiffMethod("finalMethod", Modifier.PUBLIC, "void");
        clz.addMethod(method);
        clz.checkSignatureCompliance();  
        observer.validate();
    }
}
