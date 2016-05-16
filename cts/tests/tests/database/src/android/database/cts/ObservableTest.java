/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.database.cts;

import android.database.Observable;
import android.test.AndroidTestCase;

public class ObservableTest extends AndroidTestCase {
    public void testRegisterUnRegisterObserver() {
        MockObservable observable = new MockObservable();

        // Test register a null observer object
        try {
            observable.registerObserver((null));
            fail("registerObserver should throw a IllegalArgumentException here.");
        } catch (IllegalArgumentException e) {
        }

        Object observer = new Object();
        // In the beginning, Observable has no observer object, nothing to be unregistered.
        try {
            observable.unregisterObserver(observer);
            fail("unregisterObserver should throw a IllegalStateException here.");
        } catch (IllegalStateException e) {
        }

        // Test unregister a null object, the Observable will unregister nothing.
        try {
            observable.unregisterObserver(null);
            fail("unregisterObserver should throw a IllegalArgumentException here.");
        } catch (IllegalArgumentException e) {
        }

        // Test register a observer object
        observable.registerObserver(observer);
        // If previous registerObserver was executed successfully, the object can't be registered
        // twice.
        try {
            observable.registerObserver(observer);
            fail("registerObserver should throw a IllegalStateException here.");
        } catch (IllegalStateException e) {
        }

        // Test unregister a observer object
        observable.unregisterObserver(observer);
        // If unregister function was executed successfully, the input observer object will be
        // removed, so it can not be unregistered anymore, and it can be registered again.
        try {
            observable.unregisterObserver(observer);
            fail("unregisterObserver should throw a IllegalStateException here.");
        } catch (IllegalStateException e) {
        }
        observable.registerObserver(observer);
    }

    public void testUnregisterAll() {
        MockObservable observable = new MockObservable();
        Object observer1 = new Object();
        Object observer2 = new Object();

        observable.registerObserver(observer1);
        observable.registerObserver(observer2);

        // If a observer was registered, it can't be registered again.
        try {
            observable.registerObserver(observer1);
            fail("registerObserver should throw a IllegalStateException here.");
        } catch (IllegalStateException e) {
        }
        try {
            observable.registerObserver(observer2);
            fail("registerObserver should throw a IllegalStateException here.");
        } catch (IllegalStateException e) {
        }

        // unregisterAll will unregister all the registered observers.
        observable.unregisterAll();
        // The Observable will be empty after unregisterAll, so it can register observers again.
        observable.registerObserver(observer1);
        observable.registerObserver(observer2);
    }

    private class MockObservable extends Observable<Object> {
    }
}
