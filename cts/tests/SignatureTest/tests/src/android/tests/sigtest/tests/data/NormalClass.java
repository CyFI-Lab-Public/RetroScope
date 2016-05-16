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

package android.tests.sigtest.tests.data;

/**
 * This class is used as reference data for the
 * JDiffClassDescriptionTest tests.  These classes will actually be
 * examined through reflection and Class.forName as part of testing
 * JDiffClassDescription.  That is why there is no implementation for
 * any of these methods.
 */
public class NormalClass {
    // Constructors to test.
    public NormalClass() { }
    private NormalClass(String arg1) { }
    protected NormalClass(String arg1, String arg2) throws NormalException { }
    NormalClass(String arg1, String arg2, String arg3) { }

    // Methods to test.
    public static void staticMethod() { }
    public synchronized void syncMethod() { }
    public void notSyncMethod() { }
    boolean packageProtectedMethod() { return false; }
    private void privateMethod() { }
    protected String protectedMethod() { return null; }
    public void throwsMethod() throws NormalException { }
    public native void nativeMethod();
    public void notNativeMethod() { }
    public final void finalMethod() { }

    // Fields to test.
    public final String FINAL_FIELD = "";
    public static String STATIC_FIELD;
    public volatile String VOLATILE_FIELD;
    public transient String TRANSIENT_FIELD;
    String PACAKGE_FIELD;
    private String PRIVATE_FIELD;
    protected String PROTECTED_FIELD;

    public class InnerClass {
        public class InnerInnerClass {
            private String innerInnerClassData;
        }
        private String innerClassData;
    }

    public interface InnerInterface {
        void doSomething();
    }
}
