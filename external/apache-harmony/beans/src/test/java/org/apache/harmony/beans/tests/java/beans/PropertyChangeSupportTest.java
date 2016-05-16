/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.beans.tests.java.beans;

import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import junit.framework.TestCase;

import org.apache.harmony.beans.tests.support.NonSerializablePropertyChangeListener;
import org.apache.harmony.beans.tests.support.SerializablePropertyChangeListener;
import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

import tests.util.SerializationTester;

/**
 * Test class PropertyChangeSupport.
 */
public class PropertyChangeSupportTest extends TestCase {

    private File tempFile;
    
    @Override
    protected void setUp() throws Exception {
        tempFile = File.createTempFile("beans", ".ser");
    }
    
    @Override
    protected void tearDown() throws Exception {
        tempFile.delete();
        tempFile = null;
    }
    /*
     * Test the constructor with a normal parameter.
     */
    public void testConstructor_Normal() {
        Object src = new Object();
        new PropertyChangeSupport(src);
    }

    /*
     * Test the method addPropertyChangeListener(PropertyChangeListener) with a
     * normal listener parameter.
     */
    public void testAddPropertyChangeListener_PropertyChangeListener_Normal() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        PropertyChangeListener l1 = new MockPropertyChangeListener();
        PropertyChangeListener l2 = new MockPropertyChangeListener();
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        PropertyChangeListener l4 = new PropertyChangeListenerProxy("myProp",
                l3);

        sup.addPropertyChangeListener(l1);

        assertEquals(1, sup.getPropertyChangeListeners().length);
        assertSame(l1, sup.getPropertyChangeListeners()[0]);

        sup.removePropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        assertEquals(1, sup.getPropertyChangeListeners().length);
        assertSame(l2, ((PropertyChangeListenerProxy) sup
                .getPropertyChangeListeners()[0]).getListener());
        assertNotSame(l3, sup.getPropertyChangeListeners()[0]);

        sup.removePropertyChangeListener(sup.getPropertyChangeListeners()[0]);
        assertEquals(0, sup.getPropertyChangeListeners().length);
        sup.addPropertyChangeListener(l4);
        //RI asserts to true here, really strange behavior
        assertNotSame(l3, ((PropertyChangeListenerProxy) sup
                .getPropertyChangeListeners()[0]).getListener());
        assertNotSame(l4, sup.getPropertyChangeListeners()[0]);
        assertSame(
                l2,
                ((PropertyChangeListenerProxy) ((PropertyChangeListenerProxy) sup
                        .getPropertyChangeListeners()[0]).getListener())
                        .getListener());
    }

    /*
     * Test the method addPropertyChangeListener(PropertyChangeListener) with a
     * null listener parameter.
     */
    public void testAddPropertyChangeListener_PropertyChangeListener_Null() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);

        sup.addPropertyChangeListener(null);
        PropertyChangeListener[] listeners = sup.getPropertyChangeListeners();
        assertEquals(0, listeners.length);
    }

    /*
     * Test the method addPropertyChangeListener(PropertyChangeListener) with a
     * listener parameter that has already been registered.
     */
    public void testAddPropertyChangeListener_PropertyChangeListener_Duplicate() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        PropertyChangeListener l1 = new MockPropertyChangeListener();

        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l1);

        PropertyChangeListener[] listeners = sup.getPropertyChangeListeners();
        assertEquals(2, listeners.length);
        assertSame(l1, listeners[0]);
        assertSame(l1, listeners[1]);
    }

    /*
     * Test the method addPropertyChangeListener(PropertyChangeListener,
     * String) with a normal listener parameter and property name parameter.
     */
    public void testAddPropertyChangeListener_PropertyChangeListener_String_Normal() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        PropertyChangeListener l1 = new MockPropertyChangeListener();
        PropertyChangeListener l2 = new MockPropertyChangeListener();
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        PropertyChangeListener[] listeners;

        sup.addPropertyChangeListener("myProp2", l1);

        listeners = sup.getPropertyChangeListeners();
        assertEquals(1, listeners.length);
        assertSame(l1, ((PropertyChangeListenerProxy) listeners[0])
                .getListener());

        sup.removePropertyChangeListener(listeners[0]);
        sup.addPropertyChangeListener("myProp3", l3);
        listeners = sup.getPropertyChangeListeners();
        assertEquals(1, listeners.length);
        // pay attention to this recursive proxy
        assertNotSame(l3, ((PropertyChangeListenerProxy) listeners[0])
                .getListener());
        assertNotSame(l3, listeners[0]);
        assertSame(
                l2,
                ((PropertyChangeListenerProxy) ((PropertyChangeListenerProxy) listeners[0])
                        .getListener()).getListener());

        listeners = sup.getPropertyChangeListeners("myProp");
        assertEquals(0, listeners.length);

        listeners = sup.getPropertyChangeListeners("myProp3");
        assertEquals(1, listeners.length);
        // pay attention to this recursive proxy
        assertNotSame(l3, ((PropertyChangeListenerProxy) listeners[0])
                .getListener());
        assertNotSame(l3, listeners[0]);
        assertSame(l2, ((PropertyChangeListenerProxy) listeners[0])
                .getListener());

    }

    /*
     * Test the method addPropertyChangeListener(PropertyChangeListener,
     * String) with a null listener parameter and a normal property name
     * parameter.
     */
    public void testAddPropertyChangeListener_PropertyChangeListener_String_NullListener() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);

        sup.addPropertyChangeListener("myProp", null);

        PropertyChangeListener[] listeners = sup.getPropertyChangeListeners();
        assertEquals(0, listeners.length);

        new PropertyChangeListenerProxy("myProp", null);
        assertEquals(0, listeners.length);
    }

    /*
     * Test the method addPropertyChangeListener(PropertyChangeListener,
     * String) with a normal listener parameter and a null property name
     * parameter.
     */
    public void testAddPropertyChangeListener_PropertyChangeListener_String_NullProperty() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        PropertyChangeListener l1 = new MockPropertyChangeListener();
        PropertyChangeListener l2 = new MockPropertyChangeListener();
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);

        sup.addPropertyChangeListener(null, l1);
        sup.addPropertyChangeListener(null, l3);
        l3 = new PropertyChangeListenerProxy(null, l2);
        sup.addPropertyChangeListener(l3);
    }

    /*
     * Test the method addPropertyChangeListener(PropertyChangeListener,
     * String) with a listener parameter that has already been registered for
     * the named property.
     */
    public void testAddPropertyChangeListener_PropertyChangeListener_String_Duplicate() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        PropertyChangeListener l1 = new MockPropertyChangeListener();
        PropertyChangeListener l2 = new MockPropertyChangeListener();
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);

        sup.addPropertyChangeListener("myProp2", l1);
        sup.addPropertyChangeListener("myProp2", l1);

        PropertyChangeListener[] listeners = sup.getPropertyChangeListeners();
        assertEquals(2, listeners.length);
        assertSame(l1, ((PropertyChangeListenerProxy) listeners[0])
                .getListener());
        assertSame(l1, ((PropertyChangeListenerProxy) listeners[1])
                .getListener());

        sup.removePropertyChangeListener(listeners[0]);
        sup.removePropertyChangeListener(listeners[1]);
        sup.addPropertyChangeListener("myProp3", l3);
        sup.addPropertyChangeListener("myProp3", l3);
        listeners = sup.getPropertyChangeListeners();
        assertEquals(2, listeners.length);
        assertSame(
                l2,
                ((PropertyChangeListenerProxy) ((PropertyChangeListenerProxy) listeners[0])
                        .getListener()).getListener());
        assertSame(
                l2,
                ((PropertyChangeListenerProxy) ((PropertyChangeListenerProxy) listeners[1])
                        .getListener()).getListener());
    }

    /*
     * Test the method removePropertyChangeListener(PropertyChangeListener)
     * with a normal listener parameter.
     */
    public void testRemovePropertyChangeListener_PropertyChangeListener_Normal() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        PropertyChangeListener l1 = new MockPropertyChangeListener();
        PropertyChangeListener l2 = new MockPropertyChangeListener();
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);

        sup.addPropertyChangeListener(l1);

        PropertyChangeListener[] listeners = sup.getPropertyChangeListeners();
        assertEquals(1, listeners.length);
        sup.removePropertyChangeListener(l1);
        listeners = sup.getPropertyChangeListeners();
        assertEquals(0, listeners.length);
        sup.addPropertyChangeListener(l3);
        listeners = sup.getPropertyChangeListeners();
        assertEquals(1, listeners.length);
        sup.removePropertyChangeListener(l3);
        listeners = sup.getPropertyChangeListeners();
        assertEquals(0, listeners.length);
        sup.addPropertyChangeListener("myProp3", l2);
        listeners = sup.getPropertyChangeListeners();
        assertEquals(1, listeners.length);
        sup.removePropertyChangeListener(l2);
        listeners = sup.getPropertyChangeListeners();
        assertEquals(1, listeners.length);
        sup.removePropertyChangeListener(listeners[0]);
        listeners = sup.getPropertyChangeListeners();
        assertEquals(0, listeners.length);
    }

    /*
     * Test the method removePropertyChangeListener(PropertyChangeListener)
     * with a null listener parameter.
     */
    public void testRemovePropertyChangeListener_PropertyChangeListener_Null() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);

        sup.removePropertyChangeListener(null);
        assertEquals(0, sup.getPropertyChangeListeners().length);
        sup.addPropertyChangeListener(null);
        assertEquals(0, sup.getPropertyChangeListeners().length);
        sup.removePropertyChangeListener(null);
        assertEquals(0, sup.getPropertyChangeListeners().length);
    }

    /*
     * Test the method removePropertyChangeListener(PropertyChangeListener)
     * with a non-registered listener parameter.
     */
    public void testRemovePropertyChangeListener_PropertyChangeListener_NonRegistered() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        sup.removePropertyChangeListener(new MockPropertyChangeListener());
        assertEquals(0, sup.getPropertyChangeListeners().length);
    }

    /*
     * Test the method removePropertyChangeListener(PropertyChangeListener,
     * String) when a listener for all properties has been registered.
     */
    public void testRemovePropertyChangeListener_PropertyChangeListener_String_AllRegistered() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = new Object();
        Object oldValue = new Object();

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);

        sup.addPropertyChangeListener(l1);

        sup.removePropertyChangeListener("myProp", l1);
        assertEquals(1, sup.getPropertyChangeListeners().length);
        assertEquals(0, sup.getPropertyChangeListeners("myProp").length);
        sup.firePropertyChange("myProp", oldValue, newValue);
        l1.assertCalled();
    }

    /*
     * Test the method removePropertyChangeListener(PropertyChangeListener,
     * String) when a listener for the named property has been registered.
     */
    public void testRemovePropertyChangeListener_PropertyChangeListener_String_PropertyRegistered() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        PropertyChangeListener l1 = new MockPropertyChangeListener();
        sup.addPropertyChangeListener("myProp", l1);
        assertEquals(1, sup.getPropertyChangeListeners().length);

        sup.removePropertyChangeListener("myProp", l1);
        assertEquals(0, sup.getPropertyChangeListeners().length);
        assertEquals(0, sup.getPropertyChangeListeners("myProp").length);

        PropertyChangeListener l2 = new MockPropertyChangeListener();
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        sup.addPropertyChangeListener(l3);
        assertEquals(1, sup.getPropertyChangeListeners().length);
        sup.removePropertyChangeListener("myProp", l2);
        assertEquals(0, sup.getPropertyChangeListeners().length);
        assertEquals(0, sup.getPropertyChangeListeners("myProp").length);
    }

    /*
     * Test the method removePropertyChangeListener(PropertyChangeListener,
     * String) with a non-registered listener parameter.
     */
    public void testRemovePropertyChangeListener_PropertyChangeListener_String_NonRegistered() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        sup.removePropertyChangeListener("myProp",
                new MockPropertyChangeListener());
        assertEquals(0, sup.getPropertyChangeListeners().length);
    }

    /*
     * Test the method removePropertyChangeListener(PropertyChangeListener,
     * String) with a null listener parameter.
     */
    public void testRemovePropertyChangeListener_PropertyChangeListener_String_NullListener() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);

        sup.removePropertyChangeListener("myProp", null);
        assertEquals(0, sup.getPropertyChangeListeners().length);
        sup.addPropertyChangeListener("myProp", null);
        assertEquals(0, sup.getPropertyChangeListeners().length);
        sup.removePropertyChangeListener("myProp", null);
        assertEquals(0, sup.getPropertyChangeListeners().length);
    }

    /*
     * Test the method removePropertyChangeListener(PropertyChangeListener,
     * String) with a null property name parameter.
     */
    public void testRemovePropertyChangeListener_PropertyChangeListener_String_NullProperty() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);

        sup.removePropertyChangeListener(null,
                new MockPropertyChangeListener());
        sup.addPropertyChangeListener("myProp",
                new MockPropertyChangeListener());
        sup.removePropertyChangeListener(null,
                new MockPropertyChangeListener());
    }

    /*
     * Test the method getPropertyChangeListeners() when there is one listener
     * for all properties and one for a named property.
     */
    public void testGetPropertyChangeListener_Normal() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        PropertyChangeListener l1 = new MockPropertyChangeListener();
        PropertyChangeListener l2 = new MockPropertyChangeListener();
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        PropertyChangeListener l4 = new MockPropertyChangeListener();

        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp2", l4);

        assertEquals(3, sup.getPropertyChangeListeners().length);
    }

    /*
     * Test the method getPropertyChangeListeners() when there is no listeners.
     */
    public void testGetPropertyChangeListener_Empty() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        assertEquals(0, sup.getPropertyChangeListeners().length);
    }

    /*
     * Test the method getPropertyChangeListeners(String) when there is one
     * listener for all properties and one for the named property and a third
     * for another named property.
     */
    public void testGetPropertyChangeListener_String_Normal() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        PropertyChangeListener l1 = new MockPropertyChangeListener();
        PropertyChangeListener l2 = new MockPropertyChangeListener();
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        PropertyChangeListener l4 = new MockPropertyChangeListener();

        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp2", l4);

        assertEquals(1, sup.getPropertyChangeListeners("myProp").length);
        assertSame(l2, sup.getPropertyChangeListeners("myProp")[0]);
        sup.addPropertyChangeListener("myProp",
                new MockPropertyChangeListener());
        assertEquals(2, sup.getPropertyChangeListeners("myProp").length);
    }

    /*
     * Test the method getPropertyChangeListeners(String) when there is no
     * listener for the named property but there is one for another named
     * property.
     */
    public void testGetPropertyChangeListener_String_None() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        PropertyChangeListener l1 = new MockPropertyChangeListener();
        PropertyChangeListener l2 = new MockPropertyChangeListener();
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp2",
                l2);
        PropertyChangeListener l4 = new MockPropertyChangeListener();

        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp3", l4);

        assertEquals(0, sup.getPropertyChangeListeners("myProp").length);
    }

    /*
     * Test the method getPropertyChangeListeners(String) with a null parameter.
     */
    public void testGetPropertyChangeListener_String_Null() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);

        sup.getPropertyChangeListeners(null);
        sup.addPropertyChangeListener("myProp",
                new MockPropertyChangeListener());
        sup.getPropertyChangeListeners(null);
    }

    /*
     * Test the method hasListeners(String) when there is one listener for all
     * properties.
     */
    public void testHasListener_AllRegistered() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        PropertyChangeListener l1 = new MockPropertyChangeListener();
        PropertyChangeListener l2 = new MockPropertyChangeListener();
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);

        assertFalse(sup.hasListeners("myProp"));
        sup.addPropertyChangeListener(l1);
        assertTrue(sup.hasListeners("myProp"));
        sup.removePropertyChangeListener(l1);
        assertFalse(sup.hasListeners("myProp"));
        sup.addPropertyChangeListener(l3);
        assertTrue(sup.hasListeners("myProp"));
    }

    /*
     * Test the method hasListeners(String) when there is one listener for the
     * named property.
     */
    public void testHasListener_PropertyRegistered() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        PropertyChangeListener l1 = new MockPropertyChangeListener();
        PropertyChangeListener l2 = new MockPropertyChangeListener();
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);

        assertFalse(sup.hasListeners("myProp"));
        sup.addPropertyChangeListener("myProP", l1);
        assertFalse(sup.hasListeners("myProp"));
        sup.addPropertyChangeListener("myProp", l2);
        assertTrue(sup.hasListeners("myProp"));
        sup.removePropertyChangeListener("myProp", l2);
        assertFalse(sup.hasListeners("myProp"));
        // The following assertion fails on RI. See HARMONY-2526
        sup.addPropertyChangeListener("myProp", l3);
        assertTrue(sup.hasListeners("myProp"));
    }

    /*
     * Test the method hasListeners(String) when there is no listeners.
     */
    public void testHasListener_None() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        assertFalse(sup.hasListeners("myProp"));
    }

    /*
     * Test the method hasListeners(String) with a null parameter.
     */
    public void testHasListener_Null() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        PropertyChangeListener l1 = new MockPropertyChangeListener();

        assertFalse(sup.hasListeners(null));

        sup.addPropertyChangeListener("myProP", l1);
        sup.hasListeners(null);
    }

    public void testFireIndexedPropertyChange() {
        final Object src = new Object();
        PropertyChangeSupport pcs = new PropertyChangeSupport(src);
        pcs.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                assertEquals(src, evt.getSource());
                assertEquals(0, ((IndexedPropertyChangeEvent)evt).getIndex());
                assertEquals("one", evt.getOldValue());
                assertEquals("two", evt.getNewValue());
            }
        });

        pcs.fireIndexedPropertyChange("foo", 0, "one", "two");
    }

    /*
     * Test the method firePropertyChange(String, Object, Object) with normal
     * parameters, when there is no listeners.
     */
    public void testFirePropertyChange_Object_NoListeners() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        sup.firePropertyChange("myProp", new Object(), new Object());
    }

    /*
     * Test the method firePropertyChange(String, Object, Object) with normal
     * parameters, when there is a listener for all properties and another for
     * the named property.
     */
    public void testFirePropertyChange_Object_Normal() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = new Object();
        Object oldValue = new Object();

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange("myProp", oldValue, newValue);
        l1.assertCalled();
        l2.assertCalled();
        l4.assertCalled();
    }

    /*
     * Test the method firePropertyChange(String, Object, Object) with equal old
     * and new non-null values, when there is a listener for all properties and
     * another for the named property.
     */
    public void testFirePropertyChange_Object_EqualValues() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = new Object();
        Object oldValue = newValue;

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange("myProp", oldValue, newValue);
        l1.assertNotCalled();
        l2.assertNotCalled();
        l4.assertNotCalled();
    }

    /*
     * Test the method firePropertyChange(String, Object, Object) with null old
     * and new values, when there is a listener for all properties and another
     * for the named property.
     */
    public void testFirePropertyChange_Object_NullValues() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = null;
        Object oldValue = null;

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange("myProp", oldValue, newValue);
        l1.assertCalled();
        l2.assertCalled();
        l4.assertCalled();
    }

    /*
     * Test the method firePropertyChange(String, Object, Object) with a
     * non-null old value and a null new value, when there is a listener for all
     * properties and another for the named property.
     */
    public void testFirePropertyChange_Object_NullNewValue() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = null;
        Object oldValue = new Object();

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange("myProp", oldValue, newValue);
        l1.assertCalled();
        l2.assertCalled();
        l4.assertCalled();
    }

    /*
     * Test the method firePropertyChange(String, Object, Object) with a null
     * old value and a non-null new value, when there is a listener for all
     * properties and another for the named property.
     */
    public void testFirePropertyChange_Object_NullOldValue() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = new Object();
        Object oldValue = null;

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange("myProp", oldValue, newValue);
        l1.assertCalled();
        l2.assertCalled();
        l4.assertCalled();
    }

    /*
     * Test the method firePropertyChange(String, Object, Object) with a null
     * property name parameter.
     */
    public void testFirePropertyChange_Object_NullProperty() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = new Object();
        Object oldValue = new Object();

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src,
                null, oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src,
                null, oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src,
                null, oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange(null, oldValue, newValue);
        l1.assertCalled();
        l2.assertNotCalled();
        l4.assertNotCalled();
    }

    /*
     * Test the method firePropertyChange(String, Object, Object) when a null
     * listener has been registered.
     */
    public void testFirePropertyChange_Object_NullListener() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = new Object();
        Object oldValue = new Object();

        sup.addPropertyChangeListener(null);
        sup.firePropertyChange("myProp", oldValue, newValue);
    }

    /*
     * Test the method firePropertyChange(PropertyChangeEvent) with normal
     * parameters, when there is a listener for all properties and another for
     * the named property.
     */
    public void testFirePropertyChange_PropertyChangeEvent_Normal() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = new Object();
        Object oldValue = new Object();
        Object src2 = new Object();
        PropertyChangeEvent event = new PropertyChangeEvent(src2, "myProp",
                oldValue, newValue);

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src2,
                "myProp", oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src2,
                "myProp", oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src2,
                "myProp", oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange(event);
        l1.assertCalled();
        l2.assertCalled();
        l4.assertCalled();
    }

    /*
     * Test the method firePropertyChange(PropertyChangeEvent) with equal old
     * and new non-null values, when there is a listener for all properties and
     * another for the named property.
     */
    public void testFirePropertyChange_PropertyChangeEvent_EqualValues() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = new Object();
        Object oldValue = newValue;
        Object src2 = new Object();
        PropertyChangeEvent event = new PropertyChangeEvent(src2, "myProp",
                oldValue, newValue);

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src2,
                "myProp", oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src2,
                "myProp", oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src2,
                "myProp", oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange(event);
        l1.assertNotCalled();
        l2.assertNotCalled();
        l4.assertNotCalled();
    }

    /*
     * Test the method firePropertyChange(PropertyChangeEvent) with null old and
     * new values, when there is a listener for all properties and another for
     * the named property.
     */
    public void testFirePropertyChange_PropertyChangeEvent_NullValues() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = null;
        Object oldValue = null;
        Object src2 = new Object();
        PropertyChangeEvent event = new PropertyChangeEvent(src2, "myProp",
                oldValue, newValue);

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src2,
                "myProp", oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src2,
                "myProp", oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src2,
                "myProp", oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange(event);
        l1.assertCalled();
        l2.assertCalled();
        l4.assertCalled();
    }

    /*
     * Test the method firePropertyChange(PropertyChangeEvent) with a non-null
     * old value and a null new value, when there is a listener for all
     * properties and another for the named property.
     */
    public void testFirePropertyChange_PropertyChangeEvent_NullNewValue() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = null;
        Object oldValue = new Object();
        Object src2 = new Object();
        PropertyChangeEvent event = new PropertyChangeEvent(src2, "myProp",
                oldValue, newValue);

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src2,
                "myProp", oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src2,
                "myProp", oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src2,
                "myProp", oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange(event);
        l1.assertCalled();
        l2.assertCalled();
        l4.assertCalled();
    }

    /*
     * Test the method firePropertyChange(PropertyChangeEvent) with a null old
     * value and a non-null new value, when there is a listener for all
     * properties and another for the named property.
     */
    public void testFirePropertyChange_PropertyChangeEvent_NullOldValue() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = new Object();
        Object oldValue = null;
        Object src2 = new Object();
        PropertyChangeEvent event = new PropertyChangeEvent(src2, "myProp",
                oldValue, newValue);

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src2,
                "myProp", oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src2,
                "myProp", oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src2,
                "myProp", oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange(event);
        l1.assertCalled();
        l2.assertCalled();
        l4.assertCalled();
    }

    /*
     * Test the method firePropertyChange(PropertyChangeEvent) with a null
     * property name parameter.
     */
    public void testFirePropertyChange_PropertyChangeEvent_NullProperty() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = new Object();
        Object oldValue = new Object();
        Object src2 = new Object();
        PropertyChangeEvent event = new PropertyChangeEvent(src2, null,
                oldValue, newValue);

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src2,
                null, oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src2,
                null, oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src2,
                null, oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange(event);
        l1.assertCalled();
        l2.assertNotCalled();
        l4.assertNotCalled();
    }

    /*
     * Test the method firePropertyChange(PropertyChangeEvent) when null.
     */
    public void testFirePropertyChange_PropertyChangeEvent_Null() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);

        try {
            sup.firePropertyChange(null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    /*
     * Test the method firePropertyChange(PropertyChangeEvent) when a null
     * listener has been registered.
     */
    public void testFirePropertyChange_PropertyChangeEvent_NullListener() {
        PropertyChangeSupport sup = new PropertyChangeSupport(new Object());
        PropertyChangeEvent event = new PropertyChangeEvent(new Object(),
                "myProp", new Object(), new Object());

        sup.addPropertyChangeListener(null);
        sup.firePropertyChange(event);
    }

    /*
     * Test the method firePropertyChange(String, boolean, boolean) with normal
     * parameters, when there is a listener for all properties and another for
     * the named property.
     */
    public void testFirePropertyChange_Boolean_Normal() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = Boolean.TRUE;
        Object oldValue = Boolean.FALSE;

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange("myProp", false, true);
        l1.assertCalled();
        l2.assertCalled();
        l4.assertCalled();
    }

    /*
     * Test the method firePropertyChange(String, boolean, boolean) with equal
     * old and new non-null values, when there is a listener for all properties
     * and another for the named property.
     */
    public void testFirePropertyChange_Boolean_EqualValues() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = Boolean.TRUE;
        Object oldValue = newValue;

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange("myProp", true, true);
        l1.assertNotCalled();
        l2.assertNotCalled();
        l4.assertNotCalled();
    }

    /*
     * Test the method firePropertyChange(String, boolean, boolean) with a null
     * property name parameter.
     */
    public void testFirePropertyChange_Boolean_NullProperty() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Object newValue = Boolean.TRUE;
        Object oldValue = Boolean.FALSE;

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src,
                null, oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src,
                null, oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src,
                null, oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange(null, false, true);
        l1.assertCalled();
        l2.assertNotCalled();
        l4.assertNotCalled();
    }

    /*
     * Test the method firePropertyChange(String, boolean, boolean) when a null
     * listener has been registered.
     */
    public void testFirePropertyChange_Boolean_NullListener() {
        PropertyChangeSupport sup = new PropertyChangeSupport(new Object());

        sup.addPropertyChangeListener(null);
        sup.firePropertyChange("myProp", true, false);
    }

    /*
     * Test the method firePropertyChange(String, int, int) with normal
     * parameters, when there is a listener for all properties and another for
     * the named property.
     */
    public void testFirePropertyChange_Int_Normal() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        int newValue = 1;
        int oldValue = 2;

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange("myProp", oldValue, newValue);
        l1.assertCalled();
        l2.assertCalled();
        l4.assertCalled();
    }

    /*
     * Test the method firePropertyChange(String, int, int) with equal old and
     * new non-null values, when there is a listener for all properties and
     * another for the named property.
     */
    public void testFirePropertyChange_Int_EqualValues() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Integer newValue = new Integer(1);
        Integer oldValue = newValue;

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange("myProp", oldValue.intValue(), newValue
                .intValue());
        l1.assertNotCalled();
        l2.assertNotCalled();
        l4.assertNotCalled();
    }

    /*
     * Test the method firePropertyChange(String, int, int) with a null property
     * name parameter.
     */
    public void testFirePropertyChange_Int_NullProperty() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        Integer newValue = new Integer(1);
        Integer oldValue = new Integer(2);

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src,
                null, oldValue, newValue);
        MockPropertyChangeListener l2 = new MockPropertyChangeListener(src,
                null, oldValue, newValue);
        PropertyChangeListener l3 = new PropertyChangeListenerProxy("myProp",
                l2);
        MockPropertyChangeListener l4 = new MockPropertyChangeListener(src,
                null, oldValue, newValue);
        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener(l3);
        sup.addPropertyChangeListener("myProp", l4);

        sup.firePropertyChange(null, oldValue.intValue(), newValue.intValue());
        l1.assertCalled();
        l2.assertNotCalled();
        l4.assertNotCalled();
    }

    /*
     * Test the method firePropertyChange(String, int, int) when a null listener
     * has been registered.
     */
    public void testFirePropertyChange_Int_NullListener() {
        Object src = new Object();
        PropertyChangeSupport sup = new PropertyChangeSupport(src);

        sup.addPropertyChangeListener(null);
        sup.firePropertyChange("myProp", 1, 2);
    }

    /*
     * Test serialization/deserialization.
     */
    public void testSerialization() throws Exception {
        Object src = "PropertyChangeSupportSerializationTest";
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        PropertyChangeSupport sup2 = new PropertyChangeSupport(src);
        Integer newValue = new Integer(1);
        Integer oldValue = new Integer(2);

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        MockPropertyChangeListener2 l2 = new MockPropertyChangeListener2();

        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener("myProp", l1);
        sup.addPropertyChangeListener("myProp", l2);
        sup2.addPropertyChangeListener(l1);
        sup2.addPropertyChangeListener("myProp", l1);

        PropertyChangeSupport deSup = (PropertyChangeSupport) SerializationTester
                .getDeserilizedObject(sup);
        assertEquals(sup2.getPropertyChangeListeners()[0], deSup
                .getPropertyChangeListeners()[0]);
        assertEquals(((PropertyChangeListenerProxy) sup2
                .getPropertyChangeListeners()[1]).getListener(),
                ((PropertyChangeListenerProxy) deSup
                        .getPropertyChangeListeners()[1]).getListener());
    }

    /*
     * Test serialization/deserialization compatibility
     */

    public void testSerializationCompatibility() throws Exception {
        Object src = "PropertyChangeSupportSerializationTest";
        PropertyChangeSupport sup = new PropertyChangeSupport(src);
        PropertyChangeSupport sup2 = new PropertyChangeSupport(src);
        Integer newValue = new Integer(1);
        Integer oldValue = new Integer(2);

        MockPropertyChangeListener l1 = new MockPropertyChangeListener(src,
                "myProp", oldValue, newValue);
        MockPropertyChangeListener2 l2 = new MockPropertyChangeListener2();

        sup.addPropertyChangeListener(l1);
        sup.addPropertyChangeListener("myProp", l1);
        sup.addPropertyChangeListener("myProp", l2);
        sup2.addPropertyChangeListener(l1);
        sup2.addPropertyChangeListener("myProp", l1);
        SerializationTest.verifyGolden(this, sup2, new SerializableAssert() {
            public void assertDeserialized(Serializable initial,
                    Serializable deserialized) {
                PropertyChangeSupport sup2 = (PropertyChangeSupport) initial;
                PropertyChangeSupport deSup = (PropertyChangeSupport) deserialized;
                assertEquals(sup2.getPropertyChangeListeners()[0], deSup
                        .getPropertyChangeListeners()[0]);
                assertEquals(((PropertyChangeListenerProxy) sup2
                        .getPropertyChangeListeners()[1]).getListener(),
                        ((PropertyChangeListenerProxy) deSup
                                .getPropertyChangeListeners()[1]).getListener());
            }
        });
    }

    /*
     * Mock PropertyChangeListener.
     */
    static class MockPropertyChangeListener implements PropertyChangeListener,
            Serializable {

        private static final long serialVersionUID = 161877638385579731L;

        private transient Object expSrc;

        private String expPropName;

        private transient Object expOldValue;

        private transient Object expNewValue;

        private transient PropertyChangeEvent event;

        private final transient boolean called = false;

        public MockPropertyChangeListener() {
        }

        public MockPropertyChangeListener(Object src, String propName,
                Object oldValue, Object newValue) {
            this.expSrc = src;
            this.expPropName = propName;
            this.expOldValue = oldValue;
            this.expNewValue = newValue;
        }

        public void setAll(Object src, String propName, Object oldValue,
                Object newValue) {
            this.expSrc = src;
            this.expPropName = propName;
            this.expOldValue = oldValue;
            this.expNewValue = newValue;
        }

        public void propertyChange(PropertyChangeEvent event) {
            this.event = event;
        }

        public void assertCalled() {
            assertSame(expSrc, event.getSource());
            assertEquals(expPropName, event.getPropertyName());
            assertEquals(expOldValue, event.getOldValue());
            assertEquals(expNewValue, event.getNewValue());
            assertNull(event.getPropagationId());
        }

        public void assertNotCalled() {
            assertNull(event);
            assertFalse(called);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MockPropertyChangeListener) {
                MockPropertyChangeListener l = (MockPropertyChangeListener) obj;
                return null == this.expPropName ? null == l.expPropName
                        : this.expPropName.equals(l.expPropName);
            }
            return false;
        }
    }

    /*
     * Mock PropertyChangeListener which is not serializable.
     */
    static class MockPropertyChangeListener2 implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent event) {
        }
    }

    /*
     * Mock PropertyChangeListener that modifies the listener set on
     * notification.
     */
    static class MockPropertyChangeListener3 implements PropertyChangeListener {

        PropertyChangeSupport changeSupport;

        public MockPropertyChangeListener3(PropertyChangeSupport changeSupport) {
            super();
            this.changeSupport = changeSupport;
        }

        /* On property changed event modify the listener set */
        public void propertyChange(PropertyChangeEvent event) {
            changeSupport
                    .addPropertyChangeListener(new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent event) {
                            // Empty
                        }
                    });
        }
    }

    /**
     * Regression test for concurrent modification of listener set
     */
    @SuppressWarnings("unused")
    public void testConcurrentModification() {
        PropertyChangeSupport changeSupport = new PropertyChangeSupport("bogus");
        MockPropertyChangeListener3 changeListener = new MockPropertyChangeListener3(
                changeSupport);
        changeSupport.firePropertyChange("bogus property", "previous", "newer");
    }

    /**
     * @tests java.beans.PropertyChangeSupport#PropertyChangeSupport(
     *        java.lang.Object)
     */
    public void testConstructor_Null() {
        // Regression for HARMONY-227
        try {
            new PropertyChangeSupport(null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    /**
     * @tests java.beans.PropertyChangeSupport#addPropertyChangeSupport
     * 
     */
    public void test_addPropertyChangeListenerNullNull() throws Exception {
        // Regression for HARMONY-441
        new PropertyChangeSupport("bean1")
                .addPropertyChangeListener(null, null);
    }

    /**
     * @tests java.beans.PropertyChangeSupport#removePropertyChangeListener(
     *        java.lang.String, java.beans.PropertyChangeListener)
     */
    public void testRemovePropertyChangeListener() {
        // Regression for HARMONY-386
        PropertyChangeSupport prop = new PropertyChangeSupport(new Object());

        PropertyChangeListener lis1 = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {
            }
        };

        PropertyChangeListener lis2 = new PropertyChangeListenerProxy("name",
                lis1);

        assertEquals(0, prop.getPropertyChangeListeners().length);

        prop.addPropertyChangeListener(lis2);
        assertEquals(1, prop.getPropertyChangeListeners().length);

        prop.removePropertyChangeListener("name", lis1);
        assertEquals(0, prop.getPropertyChangeListeners().length);
    }

    /**
     * @tests java.beans.PropertyChangeSupport#removePropertyChangeListener(
     *        java.lang.String, java.beans.PropertyChangeListener)
     */
    public void testRemovePropertyChangeListener2() {
        // Regression for HARMONY-320
        PropertyChangeListener listener = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent arg0) {
            }
        };

        PropertyChangeSupport pcs = new PropertyChangeSupport(this);

        pcs.addPropertyChangeListener("property", listener);
        PropertyChangeListener[] listeners = pcs.getPropertyChangeListeners();
        assertEquals(1, listeners.length);

        pcs.removePropertyChangeListener(listeners[0]);
        listeners = pcs.getPropertyChangeListeners();
        assertEquals(0, listeners.length);
    }

    /**
     * The test checks the serialization for listeners supporting serialization
     */
    public void testSerializableListener() throws Exception {
        writePropertyChangeListeners(new PropertyChangeListener[] { new SerializablePropertyChangeListener() });
        readPropertyChangeListeners();
    }

    /**
     * The test checks the serialization for listeners not supporting
     * serialization
     */
    public void testNonSerializableListener() throws Exception {
        writePropertyChangeListeners(new PropertyChangeListener[] { new NonSerializablePropertyChangeListener() });
        readPropertyChangeListeners();
    }

    private void writePropertyChangeListeners(PropertyChangeListener[] array) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(tempFile));
            PropertyChangeSupport pcs = new PropertyChangeSupport("bean");
            if (array != null && array.length > 0) {
                for (PropertyChangeListener element : array) {
                    pcs.addPropertyChangeListener(element);
                }
            }
            oos.writeObject(pcs);
            oos.flush();
        } catch (Exception e) {
            System.out.println(e.getClass() + ": " + e.getMessage());
            fail("Exception is thrown in testNonSerializableListener");
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException ioe) {
                    fail("Exception while closing ObjectOutputStream");
                }
            }
        }
    }

    private PropertyChangeListener[] readPropertyChangeListeners()
            throws Exception {
        ObjectInputStream ois = null;
        PropertyChangeSupport pcs = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(tempFile));
            pcs = (PropertyChangeSupport) ois.readObject();
        } finally {
            if (ois != null) {
                ois.close();
            }
        }
        return pcs.getPropertyChangeListeners();
    }
}
