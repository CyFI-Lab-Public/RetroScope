/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
* @author Stepan M. Mishura
*/

package javax.security.auth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecurityPermission;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.harmony.auth.internal.SecurityTest;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Tests Subject and its inner classes implementation. 
 */

public class SubjectTest extends SecurityTest {

    private static final Principal principal = new Principal() {
        public String getName() {
            return "name";
        }
    };

    PrivilegedAction<Object> emptyPAction = new PrivilegedAction<Object>() {
        public Object run() {
            return null;
        }
    };

    PrivilegedExceptionAction<Object> emptyPEAction = new PrivilegedExceptionAction<Object>
    () {
        public Object run() {
            return null;
        }
    };

    PrivilegedAction<AccessControlContext> contextPAction = new PrivilegedAction<AccessControlContext>() {
        public AccessControlContext run() {
            return AccessController.getContext();
        }
    };

    PrivilegedExceptionAction<AccessControlContext> contextPEAction = new PrivilegedExceptionAction<AccessControlContext>() {
        public AccessControlContext run() {
            return AccessController.getContext();
        }
    };

    PrivilegedAction<Subject> subjectPAction = new PrivilegedAction<Subject>() {
        public Subject run() {
            return Subject.getSubject(AccessController.getContext());
        }
    };

    PrivilegedExceptionAction<Subject> subjectPEAction = new PrivilegedExceptionAction<Subject>() {
        public Subject run() {
            return Subject.getSubject(AccessController.getContext());
        }
    };

    private final HashSet<Principal> h1 = new HashSet<Principal>(); // principals

    private final HashSet<Object> h2 = new HashSet<Object>(); // public credentials

    private final HashSet<Object> h3 = new HashSet<Object>(); // private credentials

    public static Test suite() throws Exception {

        TestSuite setSuite = new TestSuite("SubjectSets");

        setSuite.addTest(new PrincipalTestSuite());
        setSuite.addTest(new PrivateCredentialTestSuite());
        setSuite.addTest(new PublicCredentialTestSuite());

        setSuite.addTest(new PrincipalClassTestSuite());
        setSuite.addTest(new PrivateCredentialClassTestSuite());
        setSuite.addTest(new PublicCredentialClassTestSuite());

        TestSuite suite = new TestSuite("Subject");

        suite.addTestSuite(javax.security.auth.SubjectTest.class);
        suite.addTest(setSuite);

        return suite;
    }

    public SubjectTest() {
        super();

        h1.add(principal);

        h2.add(new Object());
        h2.add(new Object());

        h3.add(new Object());
        h3.add(new Object());
        h3.add(new Object());
    }

    /**
     * Testing Subject() constructor
     */
    public final void testSubject() {
        Subject subject = new Subject();

        assertFalse("Read only state", subject.isReadOnly());
        assertEquals("Principals set", 0, subject.getPrincipals().size());

        assertEquals("Private credential set", 0, subject
                .getPrivateCredentials().size());

        assertEquals("Public credential set", 0, subject.getPublicCredentials()
                .size());
    }

    /**
     * Testing Subject(boolean,Set,Set,Set) constructor
     */
    public final void testSubject_3Set() {

        Subject subject = new Subject(false, h1, h2, h3);

        assertFalse("Read only state", subject.isReadOnly());
        assertEquals("Principals set", h1, subject.getPrincipals());

        assertEquals("Private credential set", h3, subject
                .getPrivateCredentials());

        assertEquals("Public credential set", h2, subject
                .getPublicCredentials());

        // the same but for read only subject        
        subject = new Subject(true, h1, h2, h3);

        assertTrue("Read only state", subject.isReadOnly());

        assertEquals("Principals set", 1, subject.getPrincipals().size());

        assertEquals("Private credential set", 3, subject
                .getPrivateCredentials().size());

        assertEquals("Public credential set", 2, subject.getPublicCredentials()
                .size());
    }

    /**
     * Testing Subject(boolean,Set,Set,Set) constructor
     * in restricted security context
     */
    public final void testSubject_3Set_NoPermissions() {

        // all sets modifications are denied
        denyPermission(new AuthPermission("*"));

        new Subject(true, h1, h2, h3);
    }

    /**
     * Testing Subject(boolean,Set,Set,Set) constructor
     * Checks NullPointerException if one of passed set is null
     */
    @SuppressWarnings("unchecked")
    public final void testSubject_3Set_NPE() {

        try {
            new Subject(false, null, new HashSet(), new HashSet());
            fail("No expected NullPointerException");
        } catch (NullPointerException e) {
        }

        try {
            new Subject(false, new HashSet(), null, new HashSet());
            fail("No expected NullPointerException");
        } catch (NullPointerException e) {
        }

        try {
            new Subject(false, new HashSet(), new HashSet(), null);
            fail("No expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Testing Subject(boolean,Set,Set,Set) constructor.
     * Parameter set contains an invalid element.
     */
    @SuppressWarnings("unchecked")
    public final void testSubject_3Set_InvalidSet() {
        HashSet hash = new HashSet();

        hash.add(null);

        try {
            new Subject(false, hash, new HashSet(), new HashSet());

            if (!testing) {
                // possible to add 'null' principal via constructor
                fail("No expected NullPointerException");
            }
        } catch (NullPointerException e) {
        }

        try {
            new Subject(false, new HashSet(), hash, new HashSet());

            if (!testing) {
                fail("No expected NullPointerException");
            }
        } catch (NullPointerException e) {
        }

        try {
            new Subject(false, new HashSet(), new HashSet(), hash);

            if (!testing) {
                fail("No expected NullPointerException");
            }
        } catch (NullPointerException e) {
        }

        hash.clear();
        hash.add(new Object());
        try {
            new Subject(false, hash, new HashSet(), new HashSet());

            if (!testing) {
                // possible to add 'null' principal via constructor
                fail("No expected IllegalArgumentException");
            }
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Tests SecurityException for Subject.doAs(Subject,PrivilegedAction)
     */
    public final void testACE_doAs_A() throws Exception {

        denyPermission(new AuthPermission("doAs"));
        try {
            Subject.doAs(new Subject(), emptyPAction);
            fail("No expected AccessControlException");
        } catch (AccessControlException e) {
            assertEquals(e, AuthPermission.class);
        }
    }

    /**
     * Tests SecurityException for Subject.doAs(Subject,PrivilegedExceptionAction)
     */
    public final void testACE_doAs_EA() throws Exception {

        denyPermission(new AuthPermission("doAs"));
        try {
            Subject.doAs(new Subject(), emptyPEAction);
            fail("No expected AccessControlException");
        } catch (AccessControlException e) {
            assertEquals(e, AuthPermission.class);
        } catch (PrivilegedActionException e) {
            fail("Unexpected PrivilegedActionException");
        }
    }

    /**
     * Tests SecurityException for Subject.doAsPrivileged(
     *     Subject,PrivilegedAction,AccessControlContext)
     */
    public final void testACE_doAsPrivileged_A() throws Exception {

        denyPermission(new AuthPermission("doAsPrivileged"));
        try {
            Subject.doAsPrivileged(new Subject(), emptyPAction, null);
            fail("No expected AccessControlException");
        } catch (AccessControlException e) {
            assertEquals(e, AuthPermission.class);
        }
    }

    /**
     * Tests SecurityException for Subject.doAsPrivileged(
     *     Subject,PrivilegedExceptionAction,AccessControlContext)
     */
    public final void testACE_doAsPrivileged_EA() throws Exception {

        denyPermission(new AuthPermission("doAsPrivileged"));
        try {
            Subject.doAsPrivileged(new Subject(), emptyPEAction, null);
            fail("No expected AccessControlException");
        } catch (AccessControlException e) {
            assertEquals(e, AuthPermission.class);
        }
    }

    /**
     * Tests SecurityException for Subject.getSubject()
     */
    public final void testACE_getSubject() {

        denyPermission(new AuthPermission("getSubject"));
        try {
            Subject.getSubject(AccessController.getContext());
            fail("No expected AccessControlException");
        } catch (AccessControlException e) {
            assertEquals(e, AuthPermission.class);
        }
    }

    /**
     * Tests SecurityException for Subject.setReadOnly()
     */
    public final void testACE_setReadOnly() {

        denyPermission(new AuthPermission("setReadOnly"));
        try {
            (new Subject()).setReadOnly();
            fail("No expected AccessControlException");
        } catch (AccessControlException e) {
            assertEquals(e, AuthPermission.class);
        }
    }

    /**
     * Tests Subject.doAs(Subject, PrivilegedAction)
     */
    public final void testDoAs() {

        Subject subject = new Subject();

        Subject contextSubject = (Subject) Subject
                .doAs(subject, subjectPAction);

        assertTrue("Returned subject", subject == contextSubject);

        // null subject
        contextSubject = (Subject) Subject.doAs(null, subjectPAction);

        assertNull("Subject is null", contextSubject);

        // null subject: check combiner (must be null)
        AccessControlContext context = (AccessControlContext) Subject.doAs(
                null, contextPAction);

        assertNull("Combiner for null subject", context.getDomainCombiner());
    }

    /**
     * Tests Subject.doAs(Subject, PrivilegedExceptionAction)
     */
    public final void testDoAs_PEA() throws Exception {

        Subject subject = new Subject();

        Subject contextSubject = (Subject) Subject.doAs(subject,
                subjectPEAction);

        assertTrue("Returned subject", subject == contextSubject);

        // null subject 
        contextSubject = (Subject) Subject.doAs(null, subjectPEAction);

        assertNull("Subject is null", contextSubject);

        // null subject: check combiner (must be null)
        AccessControlContext context = (AccessControlContext) Subject.doAs(
                null, contextPEAction);

        assertNull("Combiner for null subject", context.getDomainCombiner());

    }

    /**
     * Tests Subject.doAsPrivileged(Subject, PrivilegedAction, ACContext)
     */
    public final void testDoAsPrivileged() {

        Subject subject = new Subject();

        Subject contextSubject = (Subject) Subject.doAsPrivileged(subject,
                subjectPAction, null);

        assertTrue("Returned subject", subject == contextSubject);

        // null subject
        contextSubject = (Subject) Subject.doAsPrivileged(null, subjectPAction,
                null);

        assertNull("Subject is null", contextSubject);

        // null subject: check combiner (must be null)
        AccessControlContext context = (AccessControlContext) Subject
                .doAsPrivileged(null, contextPAction, null);

        assertNull("Combiner for null subject", context.getDomainCombiner());
    }

    /**
     * Tests Subject.doAsPrivileged(Subject, PEAction, ACContext)
     */
    public final void testDoAsPrivileged_PEA() throws Exception {

        Subject subject = new Subject();

        Subject contextSubject = (Subject) Subject.doAsPrivileged(subject,
                subjectPEAction, null);

        assertTrue("Returned subject", subject == contextSubject);

        // null subject
        contextSubject = (Subject) Subject.doAsPrivileged(null,
                subjectPEAction, null);

        assertNull("Subject is null", contextSubject);

        // null subject: check combiner (must be null)
        AccessControlContext context = (AccessControlContext) Subject
                .doAsPrivileged(null, contextPEAction, null);

        assertNull("Combiner for null subject", context.getDomainCombiner());
    }

    /**
     * Tests Subject.doAs* methods for creating new context
     * 
     * Expected: no SecurityException
     */
    public final void testDoAs_newACC() throws Exception {

        Subject subject = new Subject();

        Subject.doAs(subject, emptyPAction);
        Subject.doAs(subject, emptyPEAction);
        Subject.doAsPrivileged(subject, emptyPAction, null);
        Subject.doAsPrivileged(subject, emptyPEAction, null);

        // each doAs* creates new ACContext 
        denyPermission(new SecurityPermission("createAccessControlContext"));

        try {
            Subject.doAs(subject, emptyPAction);
            fail("No expected AccessControlException");
        } catch (AccessControlException e) {
        }

        try {
            Subject.doAs(subject, emptyPEAction);
            fail("No expected AccessControlException");
        } catch (AccessControlException e) {
        }

        try {
            Subject.doAsPrivileged(subject, emptyPAction, null);
            fail("No expected AccessControlException");
        } catch (AccessControlException e) {
        }

        try {
            Subject.doAsPrivileged(subject, emptyPEAction, null);
            fail("No expected AccessControlException");
        } catch (AccessControlException e) {
        }
    }

    /**
     * Tests Subject.equals() method
     */
    @SuppressWarnings("unchecked")
    public final void testEquals() {

        // empty sets
        Subject s1 = new Subject();
        Subject s2 = new Subject(false, new HashSet(), new HashSet(),
                new HashSet());
        Subject s3 = new Subject(true, new HashSet(), new HashSet(),
                new HashSet());

        equalsTest(s1, s2, s3);

        // non empty sets

        s1 = new Subject(false, h1, h2, h3);
        s3 = new Subject(true, h1, h2, h3);

        s2 = new Subject();
        s2.getPrincipals().addAll(h1);
        s2.getPublicCredentials().addAll(h2);
        s2.getPrivateCredentials().addAll(h3);

        equalsTest(s1, s2, s3);

        // not equal subjects
        s1 = new Subject();
        s2 = new Subject(true, h1, new HashSet(), h3);
        s3 = new Subject(true, h1, h2, h3);

        assertFalse(s1.equals(s2));
        assertFalse(s1.equals(s3));
        assertFalse(s2.equals(s3));
    }

    private void equalsTest(Object obj1, Object obj2, Object obj3) {

        // Check passed parameters.
        // Because we don't verify Object.equals() method
        if (obj1 == obj2 || obj1 == obj3 || obj2 == obj3) {
            throw new AssertionError("References MUST be different");
        }

        // reflexivity
        assertTrue(obj1.equals(obj1));

        // symmetry
        assertTrue(obj1.equals(obj2));
        assertTrue(obj2.equals(obj1));

        // transitivity
        assertTrue(obj1.equals(obj2));
        assertTrue(obj2.equals(obj3));
        assertTrue(obj1.equals(obj3));

        // consistency
        assertTrue(obj3.equals(obj1));
        assertTrue(obj3.equals(obj1));

        // null value
        assertFalse(obj1.equals(null));
    }

    /**
     * Verifies that Subject.equals() has defined comparison algorism.
     * 
     * The sequence of checks is following:
     * 1)principal set
     * 2)public credential set
     * 3)private credential set
     */
    @SuppressWarnings("unchecked")
    public final void testEquals_VerifyCheckSequence() {

        grantMode(); // no permissions

        HashSet hash = new HashSet();
        hash.add(principal);

        Subject subject1 = new Subject(false, new HashSet(), new HashSet(),
                hash);

        //doesn't verify private credential permissions on itself
        assertTrue(subject1.equals(subject1));

        // principals comparison goes before
        // no SecurityException expected 
        Subject subject2 = new Subject(false, hash, new HashSet(), hash);

        assertFalse(subject1.equals(subject2));

        // public credential comparison goes before
        // no SecurityException expected 
        subject2 = new Subject(false, new HashSet(), hash, hash);

        assertFalse(subject1.equals(subject2));

        // principal and public credentials sets are equal
        // Expected: SecurityException
        subject2 = new Subject(false, new HashSet(), new HashSet(), hash);
        try {
            subject1.equals(subject2);
            fail("No expected AccessControlException");
        } catch (AccessControlException e) {
            assertEquals(e, PrivateCredentialPermission.class);
        }
    }

    /**
     * Verifies no PrivateCredentialPermission
     * for 'this' subject and provided subject
     */
    public final void testEquals_NoPCP() {

        Subject subThis = new Subject();
        Subject subThat = new Subject();

        subThis.getPrivateCredentials().add(new MyClass1());
        subThat.getPrivateCredentials().add(new Object());

        grantMode(); // no permissions
        grantPermission(new PrivateCredentialPermission(
                "java.lang.Object * \"*\"", "read"));

        // verify permissions
        try {
            subThis.getPrivateCredentials().iterator().next();
            fail("No expected AccessControlException");
        } catch (AccessControlException e) {
            assertEquals(e, PrivateCredentialPermission.class);
        }
        subThat.getPrivateCredentials().iterator().next();

        // 'this' subject doesn't have permission
        try {
            subThis.equals(subThat);
            fail("No expected AccessControlException");
        } catch (AccessControlException e) {
            assertEquals(e, PrivateCredentialPermission.class);
        }

        // provided subject doesn't have permission
        try {
            subThat.equals(subThis);
            fail("No expected AccessControlException");
        } catch (AccessControlException e) {
            assertEquals(e, PrivateCredentialPermission.class);
        }
    }

    /**
     * Tests Subject.get<set>(Class) methods
     */
    @SuppressWarnings("unchecked")
    public final void testGetSetClass() {
        HashSet hash = new HashSet();

        MyClass1 p1 = new MyClass1();
        MyClass1 p2 = new MyClass1();

        hash.add(p1);
        hash.add(p2);

        HashSet h = new HashSet();

        h.add(principal);
        h.addAll(hash);
        h.add(new MyClass2());

        Subject subject = new Subject(true, h, h, h);

        assertEquals("Principal", hash, subject.getPrincipals(MyClass1.class));
        assertEquals("Private Credentials", hash, subject
                .getPrivateCredentials(MyClass1.class));
        assertEquals("Public Credentials", hash, subject
                .getPublicCredentials(MyClass1.class));
    }

    /**
     * Tests Subject.get<set>(Class) methods for null parameter
     */
    public final void testGetClass_NullParameter() {

        Subject subject = new Subject();

        try {
            subject.getPrincipals(null);
            fail("No expected NullPointerException");
        } catch (NullPointerException e) {
        }

        try {
            subject.getPrivateCredentials(null);
            fail("No expected NullPointerException");
        } catch (NullPointerException e) {
        }

        try {
            subject.getPublicCredentials(null);
            fail("No expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Tests Subject.getSubject() for null parameter
     */
    public final void test_getSubject_NPE() {
        try {
            Subject.getSubject(null);
            fail("No expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Tests Subject.getSubject() for current context
     */
    public final void test_getSubject() {
        assertNull("Current context", Subject.getSubject(AccessController
                .getContext()));

        try {
            Subject.getSubject(null);
            fail("No expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Tests Subject.getSubject() for associated context
     */
    public final void test_getSubject_SameSubject() {

        Subject subject = new Subject();

        Subject contextSubject = (Subject) Subject
                .doAs(subject, subjectPAction);

        assertTrue("Subject: ", subject == contextSubject);
    }

    /**
     * Tests Subject.getSubject() for associated context (2 subjects)
     */
    @SuppressWarnings("unchecked")
    public final void test_getSubject_NotSameSubject() {

        final HashSet hash = new HashSet();
        hash.add(new MyClass1());

        PrivilegedAction<Object> action = new PrivilegedAction<Object>() {
            public Object run() {

                return Subject.doAs(new Subject(false, hash, hash, hash),
                        subjectPAction);
            }
        };

        Subject subject = new Subject();

        Subject contextSubject = (Subject) Subject.doAs(subject, action);

        assertNotNull("Context subject: ", contextSubject);
        assertFalse("Subject: ", subject == contextSubject);
        assertTrue("Principals: ", hash.equals(contextSubject.getPrincipals()));
        assertTrue("Private Credentials: ", hash.equals(contextSubject
                .getPrivateCredentials()));
        assertTrue("Public Credentials: ", hash.equals(contextSubject
                .getPublicCredentials()));
    }

    /**
     * Tests Subject.getSubject() for privileged action in associated context
     */
    public final void test_getSubject_PrivilegedAction() {

        PrivilegedAction<Object> action = new PrivilegedAction<Object>() {
            public Object run() {
                return AccessController.doPrivileged(subjectPAction);
            }
        };

        Subject subject = new Subject();

        Subject contextSubject = (Subject) Subject.doAs(subject, action);

        assertNull("Context subject: ", contextSubject);
    }

    /**
     * Tests Subject.hashCode()
     */
    public final void testHashCode() {
        Subject subject1 = new Subject(false, h1, h2, h3);
        Subject subject2 = new Subject(true, h1, h2, h3);

        assertTrue(subject1.equals(subject2));
        assertTrue(subject1.hashCode() == subject2.hashCode());
    }

    /**
     * Tests Subject.hashCode() for SecurityException
     */
    public final void testHashCode_ACE() {

        grantMode();
        try {
            (new Subject(false, h1, h2, h3)).hashCode();

            if (!testing) {
                fail("No expected AccessControlException");
            }
        } catch (AccessControlException e) {
            assertEquals(e, PrivateCredentialPermission.class);
        }
    }

    /**
     * Tests Subject.isReadOnly() and Subject.setReadOnly()
     */
    public final void testSetReadOnly_isReadOnly() {
        Subject subject = new Subject();

        // check initialized value
        assertFalse("Read only state", subject.isReadOnly());

        // set the subject as read only
        subject.setReadOnly();
        assertTrue("Read only state", subject.isReadOnly());

        // anyway invoke it again to verify subject's state
        subject.setReadOnly();
        assertTrue("Read only state", subject.isReadOnly());
    }

    public final void testToString() {
        //FIXME        grantMode();
        //denyPermission(new PrivateCredentialPermission("* * \"*\"", "read"));
        //System.out.println((new Subject(false, h1, h2, h3)).toString());
    }

    public final void testSerialization() throws Exception {
        
        Subject subject = new Subject();

        subject.getPrincipals().add(new MyClass2());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream sOut = new ObjectOutputStream(out);

        try {
            sOut.writeObject(subject);
            fail("No expected NotSerializableException");
        } catch (NotSerializableException e) {
        } finally {
            sOut.close();
        }

        subject = new Subject();

        subject.getPrincipals().add(new MyClass1());
        subject.getPublicCredentials().add(new MyClass1());
        subject.getPrivateCredentials().add(new MyClass1());

        subject.setReadOnly();

        out = new ByteArrayOutputStream();
        sOut = new ObjectOutputStream(out);

        sOut.writeObject(subject);

        sOut.flush();
        sOut.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream sIn = new ObjectInputStream(in);

        Subject ss = (Subject) sIn.readObject();

        assertTrue(ss.isReadOnly());
        assertEquals(1, ss.getPrincipals().size());
        assertTrue(ss.getPrincipals().iterator().next() instanceof MyClass1);
        assertEquals(0, ss.getPublicCredentials().size());
        assertEquals(0, ss.getPrivateCredentials().size());

        try {
            ss.getPrincipals().add(new MyClass1());
            fail("No expected IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }

    /**
     * Test subject's deserialization in case of invalid('null') principals
     * 
     * Serialization byte array contains null element in principal set
     * The array is invalid because it is not possible to add null element
     * to principal set via public API methods.
     */
    public final void testSerialization_NullPrincipal() throws Exception {

        // The array was produced in the following way:
        // 1) A check that verifies a passed principal object for null
        //    value was disabled in Subject class.
        // 2) Subject object was created
        // 3) A null was added to subject's principal set by invoking
        //        getPrincipals().add(null);
        // 4) ByteArrayOutputStream class was used to write subject object
        //    and to get resulting array of bytes 
        byte[] nullPrincipal = new byte[] { (byte) 0xac, (byte) 0xed,
                (byte) 0x00, (byte) 0x05, (byte) 0x73, (byte) 0x72,
                (byte) 0x00, (byte) 0x1b, (byte) 0x6a, (byte) 0x61,
                (byte) 0x76, (byte) 0x61, (byte) 0x78, (byte) 0x2e,
                (byte) 0x73, (byte) 0x65, (byte) 0x63, (byte) 0x75,
                (byte) 0x72, (byte) 0x69, (byte) 0x74, (byte) 0x79,
                (byte) 0x2e, (byte) 0x61, (byte) 0x75, (byte) 0x74,
                (byte) 0x68, (byte) 0x2e, (byte) 0x53, (byte) 0x75,
                (byte) 0x62, (byte) 0x6a, (byte) 0x65, (byte) 0x63,
                (byte) 0x74, (byte) 0x8c, (byte) 0xb2, (byte) 0x32,
                (byte) 0x93, (byte) 0x00, (byte) 0x33, (byte) 0xfa,
                (byte) 0x68, (byte) 0x03, (byte) 0x00, (byte) 0x02,
                (byte) 0x5a, (byte) 0x00, (byte) 0x0a, (byte) 0x69,
                (byte) 0x73, (byte) 0x52, (byte) 0x65, (byte) 0x61,
                (byte) 0x64, (byte) 0x4f, (byte) 0x6e, (byte) 0x6c,
                (byte) 0x79, (byte) 0x4c, (byte) 0x00, (byte) 0x0a,
                (byte) 0x70, (byte) 0x72, (byte) 0x69, (byte) 0x6e,
                (byte) 0x63, (byte) 0x69, (byte) 0x70, (byte) 0x61,
                (byte) 0x6c, (byte) 0x73, (byte) 0x74, (byte) 0x00,
                (byte) 0x0f, (byte) 0x4c, (byte) 0x6a, (byte) 0x61,
                (byte) 0x76, (byte) 0x61, (byte) 0x2f, (byte) 0x75,
                (byte) 0x74, (byte) 0x69, (byte) 0x6c, (byte) 0x2f,
                (byte) 0x53, (byte) 0x65, (byte) 0x74, (byte) 0x3b,
                (byte) 0x78, (byte) 0x70, (byte) 0x00, (byte) 0x73,
                (byte) 0x72, (byte) 0x00, (byte) 0x25, (byte) 0x6a,
                (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x78,
                (byte) 0x2e, (byte) 0x73, (byte) 0x65, (byte) 0x63,
                (byte) 0x75, (byte) 0x72, (byte) 0x69, (byte) 0x74,
                (byte) 0x79, (byte) 0x2e, (byte) 0x61, (byte) 0x75,
                (byte) 0x74, (byte) 0x68, (byte) 0x2e, (byte) 0x53,
                (byte) 0x75, (byte) 0x62, (byte) 0x6a, (byte) 0x65,
                (byte) 0x63, (byte) 0x74, (byte) 0x24, (byte) 0x53,
                (byte) 0x65, (byte) 0x63, (byte) 0x75, (byte) 0x72,
                (byte) 0x65, (byte) 0x53, (byte) 0x65, (byte) 0x74,
                (byte) 0x6d, (byte) 0xcc, (byte) 0x32, (byte) 0x80,
                (byte) 0x17, (byte) 0x55, (byte) 0x7e, (byte) 0x27,
                (byte) 0x03, (byte) 0x00, (byte) 0x03, (byte) 0x49,
                (byte) 0x00, (byte) 0x07, (byte) 0x73, (byte) 0x65,
                (byte) 0x74, (byte) 0x54, (byte) 0x79, (byte) 0x70,
                (byte) 0x65, (byte) 0x4c, (byte) 0x00, (byte) 0x08,
                (byte) 0x65, (byte) 0x6c, (byte) 0x65, (byte) 0x6d,
                (byte) 0x65, (byte) 0x6e, (byte) 0x74, (byte) 0x73,
                (byte) 0x74, (byte) 0x00, (byte) 0x16, (byte) 0x4c,
                (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61,
                (byte) 0x2f, (byte) 0x75, (byte) 0x74, (byte) 0x69,
                (byte) 0x6c, (byte) 0x2f, (byte) 0x4c, (byte) 0x69,
                (byte) 0x6e, (byte) 0x6b, (byte) 0x65, (byte) 0x64,
                (byte) 0x4c, (byte) 0x69, (byte) 0x73, (byte) 0x74,
                (byte) 0x3b, (byte) 0x4c, (byte) 0x00, (byte) 0x06,
                (byte) 0x74, (byte) 0x68, (byte) 0x69, (byte) 0x73,
                (byte) 0x24, (byte) 0x30, (byte) 0x74, (byte) 0x00,
                (byte) 0x1d, (byte) 0x4c, (byte) 0x6a, (byte) 0x61,
                (byte) 0x76, (byte) 0x61, (byte) 0x78, (byte) 0x2f,
                (byte) 0x73, (byte) 0x65, (byte) 0x63, (byte) 0x75,
                (byte) 0x72, (byte) 0x69, (byte) 0x74, (byte) 0x79,
                (byte) 0x2f, (byte) 0x61, (byte) 0x75, (byte) 0x74,
                (byte) 0x68, (byte) 0x2f, (byte) 0x53, (byte) 0x75,
                (byte) 0x62, (byte) 0x6a, (byte) 0x65, (byte) 0x63,
                (byte) 0x74, (byte) 0x3b, (byte) 0x78, (byte) 0x70,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x73, (byte) 0x72, (byte) 0x00, (byte) 0x14,
                (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61,
                (byte) 0x2e, (byte) 0x75, (byte) 0x74, (byte) 0x69,
                (byte) 0x6c, (byte) 0x2e, (byte) 0x4c, (byte) 0x69,
                (byte) 0x6e, (byte) 0x6b, (byte) 0x65, (byte) 0x64,
                (byte) 0x4c, (byte) 0x69, (byte) 0x73, (byte) 0x74,
                (byte) 0x0c, (byte) 0x29, (byte) 0x53, (byte) 0x5d,
                (byte) 0x4a, (byte) 0x60, (byte) 0x88, (byte) 0x22,
                (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x78,
                (byte) 0x70, (byte) 0x77, (byte) 0x04, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x70,
                (byte) 0x78, (byte) 0x71, (byte) 0x00, (byte) 0x7e,
                (byte) 0x00, (byte) 0x02, (byte) 0x78, (byte) 0x78 };

        ByteArrayInputStream in = new ByteArrayInputStream(nullPrincipal);
        ObjectInputStream sIn = new ObjectInputStream(in);

        try {
            sIn.readObject();
            if (!testing) {
                fail("No expected NullPointerException");
            }
        } catch (NullPointerException e) {
        }
    }

    /**
     * Test subject's deserialization in case of invalid principals
     * Byte stream contains object in principal set that doesn't
     * implement Principal interface.
     * The array is invalid because it is not possible to add such object
     * to principal set via public API methods.
     */
    public final void testSerialization_IllegalPrincipal() throws Exception {

        // The array was produced in the following way:
        // 1) A check for verifying that passed principal object 
        //    implements Principal interface was disabled in Subject class.
        // 2) Subject object was created
        // 3) A serializable object was added to subject's principal
        //    set by invoking: getPrincipals().add(object);
        // 4) ByteArrayOutputStream class was used to write subject object
        //    and to get resulting array of bytes 
        byte[] objectPrincipal = new byte[] { (byte) 0xac, (byte) 0xed,
                (byte) 0x00, (byte) 0x05, (byte) 0x73, (byte) 0x72,
                (byte) 0x00, (byte) 0x1b, (byte) 0x6a, (byte) 0x61,
                (byte) 0x76, (byte) 0x61, (byte) 0x78, (byte) 0x2e,
                (byte) 0x73, (byte) 0x65, (byte) 0x63, (byte) 0x75,
                (byte) 0x72, (byte) 0x69, (byte) 0x74, (byte) 0x79,
                (byte) 0x2e, (byte) 0x61, (byte) 0x75, (byte) 0x74,
                (byte) 0x68, (byte) 0x2e, (byte) 0x53, (byte) 0x75,
                (byte) 0x62, (byte) 0x6a, (byte) 0x65, (byte) 0x63,
                (byte) 0x74, (byte) 0x8c, (byte) 0xb2, (byte) 0x32,
                (byte) 0x93, (byte) 0x00, (byte) 0x33, (byte) 0xfa,
                (byte) 0x68, (byte) 0x03, (byte) 0x00, (byte) 0x02,
                (byte) 0x5a, (byte) 0x00, (byte) 0x0a, (byte) 0x69,
                (byte) 0x73, (byte) 0x52, (byte) 0x65, (byte) 0x61,
                (byte) 0x64, (byte) 0x4f, (byte) 0x6e, (byte) 0x6c,
                (byte) 0x79, (byte) 0x4c, (byte) 0x00, (byte) 0x0a,
                (byte) 0x70, (byte) 0x72, (byte) 0x69, (byte) 0x6e,
                (byte) 0x63, (byte) 0x69, (byte) 0x70, (byte) 0x61,
                (byte) 0x6c, (byte) 0x73, (byte) 0x74, (byte) 0x00,
                (byte) 0x0f, (byte) 0x4c, (byte) 0x6a, (byte) 0x61,
                (byte) 0x76, (byte) 0x61, (byte) 0x2f, (byte) 0x75,
                (byte) 0x74, (byte) 0x69, (byte) 0x6c, (byte) 0x2f,
                (byte) 0x53, (byte) 0x65, (byte) 0x74, (byte) 0x3b,
                (byte) 0x78, (byte) 0x70, (byte) 0x00, (byte) 0x73,
                (byte) 0x72, (byte) 0x00, (byte) 0x25, (byte) 0x6a,
                (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x78,
                (byte) 0x2e, (byte) 0x73, (byte) 0x65, (byte) 0x63,
                (byte) 0x75, (byte) 0x72, (byte) 0x69, (byte) 0x74,
                (byte) 0x79, (byte) 0x2e, (byte) 0x61, (byte) 0x75,
                (byte) 0x74, (byte) 0x68, (byte) 0x2e, (byte) 0x53,
                (byte) 0x75, (byte) 0x62, (byte) 0x6a, (byte) 0x65,
                (byte) 0x63, (byte) 0x74, (byte) 0x24, (byte) 0x53,
                (byte) 0x65, (byte) 0x63, (byte) 0x75, (byte) 0x72,
                (byte) 0x65, (byte) 0x53, (byte) 0x65, (byte) 0x74,
                (byte) 0x6d, (byte) 0xcc, (byte) 0x32, (byte) 0x80,
                (byte) 0x17, (byte) 0x55, (byte) 0x7e, (byte) 0x27,
                (byte) 0x03, (byte) 0x00, (byte) 0x03, (byte) 0x49,
                (byte) 0x00, (byte) 0x07, (byte) 0x73, (byte) 0x65,
                (byte) 0x74, (byte) 0x54, (byte) 0x79, (byte) 0x70,
                (byte) 0x65, (byte) 0x4c, (byte) 0x00, (byte) 0x08,
                (byte) 0x65, (byte) 0x6c, (byte) 0x65, (byte) 0x6d,
                (byte) 0x65, (byte) 0x6e, (byte) 0x74, (byte) 0x73,
                (byte) 0x74, (byte) 0x00, (byte) 0x16, (byte) 0x4c,
                (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61,
                (byte) 0x2f, (byte) 0x75, (byte) 0x74, (byte) 0x69,
                (byte) 0x6c, (byte) 0x2f, (byte) 0x4c, (byte) 0x69,
                (byte) 0x6e, (byte) 0x6b, (byte) 0x65, (byte) 0x64,
                (byte) 0x4c, (byte) 0x69, (byte) 0x73, (byte) 0x74,
                (byte) 0x3b, (byte) 0x4c, (byte) 0x00, (byte) 0x06,
                (byte) 0x74, (byte) 0x68, (byte) 0x69, (byte) 0x73,
                (byte) 0x24, (byte) 0x30, (byte) 0x74, (byte) 0x00,
                (byte) 0x1d, (byte) 0x4c, (byte) 0x6a, (byte) 0x61,
                (byte) 0x76, (byte) 0x61, (byte) 0x78, (byte) 0x2f,
                (byte) 0x73, (byte) 0x65, (byte) 0x63, (byte) 0x75,
                (byte) 0x72, (byte) 0x69, (byte) 0x74, (byte) 0x79,
                (byte) 0x2f, (byte) 0x61, (byte) 0x75, (byte) 0x74,
                (byte) 0x68, (byte) 0x2f, (byte) 0x53, (byte) 0x75,
                (byte) 0x62, (byte) 0x6a, (byte) 0x65, (byte) 0x63,
                (byte) 0x74, (byte) 0x3b, (byte) 0x78, (byte) 0x70,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x73, (byte) 0x72, (byte) 0x00, (byte) 0x14,
                (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61,
                (byte) 0x2e, (byte) 0x75, (byte) 0x74, (byte) 0x69,
                (byte) 0x6c, (byte) 0x2e, (byte) 0x4c, (byte) 0x69,
                (byte) 0x6e, (byte) 0x6b, (byte) 0x65, (byte) 0x64,
                (byte) 0x4c, (byte) 0x69, (byte) 0x73, (byte) 0x74,
                (byte) 0x0c, (byte) 0x29, (byte) 0x53, (byte) 0x5d,
                (byte) 0x4a, (byte) 0x60, (byte) 0x88, (byte) 0x22,
                (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x78,
                (byte) 0x70, (byte) 0x77, (byte) 0x04, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x73,
                (byte) 0x72, (byte) 0x00, (byte) 0x28, (byte) 0x6a,
                (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x78,
                (byte) 0x2e, (byte) 0x73, (byte) 0x65, (byte) 0x63,
                (byte) 0x75, (byte) 0x72, (byte) 0x69, (byte) 0x74,
                (byte) 0x79, (byte) 0x2e, (byte) 0x61, (byte) 0x75,
                (byte) 0x74, (byte) 0x68, (byte) 0x2e, (byte) 0x53,
                (byte) 0x75, (byte) 0x62, (byte) 0x6a, (byte) 0x65,
                (byte) 0x63, (byte) 0x74, (byte) 0x54, (byte) 0x65,
                (byte) 0x73, (byte) 0x74, (byte) 0x24, (byte) 0x4d,
                (byte) 0x79, (byte) 0x4f, (byte) 0x62, (byte) 0x6a,
                (byte) 0x65, (byte) 0x63, (byte) 0x74, (byte) 0xf7,
                (byte) 0xbc, (byte) 0xdc, (byte) 0x95, (byte) 0xb2,
                (byte) 0x33, (byte) 0x3a, (byte) 0x0f, (byte) 0x02,
                (byte) 0x00, (byte) 0x00, (byte) 0x78, (byte) 0x70,
                (byte) 0x78, (byte) 0x71, (byte) 0x00, (byte) 0x7e,
                (byte) 0x00, (byte) 0x02, (byte) 0x78, (byte) 0x78 };

        ByteArrayInputStream in = new ByteArrayInputStream(objectPrincipal);
        ObjectInputStream sIn = new ObjectInputStream(in);

        try {
            sIn.readObject();
            if (!testing) {
                fail("No expected IllegalArgumentException");
            }
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Test subject's principal set deserialization in case
     * of invalid principal set's elements. Two cases are tested:
     * 1) null object
     * 2) an object in principal set that doesn't implement Principal interface.
     */
    public void test_PrincipalSetInvalidSerForm() throws Exception {

        // The array was produced in the following way:
        // 1) A check that verifies a passed principal object for null
        //    value was disabled in Subject class.
        // 2) Subject object was created
        // 3) A null was added to subject's principal set by invoking
        //        getPrincipals().add(null);
        // 4) ByteArrayOutputStream class was used to write
        //    subject's principal set object and to get resulting array of bytes 
        byte[] nullElement = new byte[] { (byte) 0xac, (byte) 0xed,
                (byte) 0x00, (byte) 0x05, (byte) 0x73, (byte) 0x72,
                (byte) 0x00, (byte) 0x25, (byte) 0x6a, (byte) 0x61,
                (byte) 0x76, (byte) 0x61, (byte) 0x78, (byte) 0x2e,
                (byte) 0x73, (byte) 0x65, (byte) 0x63, (byte) 0x75,
                (byte) 0x72, (byte) 0x69, (byte) 0x74, (byte) 0x79,
                (byte) 0x2e, (byte) 0x61, (byte) 0x75, (byte) 0x74,
                (byte) 0x68, (byte) 0x2e, (byte) 0x53, (byte) 0x75,
                (byte) 0x62, (byte) 0x6a, (byte) 0x65, (byte) 0x63,
                (byte) 0x74, (byte) 0x24, (byte) 0x53, (byte) 0x65,
                (byte) 0x63, (byte) 0x75, (byte) 0x72, (byte) 0x65,
                (byte) 0x53, (byte) 0x65, (byte) 0x74, (byte) 0x6d,
                (byte) 0xcc, (byte) 0x32, (byte) 0x80, (byte) 0x17,
                (byte) 0x55, (byte) 0x7e, (byte) 0x27, (byte) 0x03,
                (byte) 0x00, (byte) 0x02, (byte) 0x4c, (byte) 0x00,
                (byte) 0x08, (byte) 0x65, (byte) 0x6c, (byte) 0x65,
                (byte) 0x6d, (byte) 0x65, (byte) 0x6e, (byte) 0x74,
                (byte) 0x73, (byte) 0x74, (byte) 0x00, (byte) 0x16,
                (byte) 0x4c, (byte) 0x6a, (byte) 0x61, (byte) 0x76,
                (byte) 0x61, (byte) 0x2f, (byte) 0x75, (byte) 0x74,
                (byte) 0x69, (byte) 0x6c, (byte) 0x2f, (byte) 0x4c,
                (byte) 0x69, (byte) 0x6e, (byte) 0x6b, (byte) 0x65,
                (byte) 0x64, (byte) 0x4c, (byte) 0x69, (byte) 0x73,
                (byte) 0x74, (byte) 0x3b, (byte) 0x4c, (byte) 0x00,
                (byte) 0x06, (byte) 0x74, (byte) 0x68, (byte) 0x69,
                (byte) 0x73, (byte) 0x24, (byte) 0x30, (byte) 0x74,
                (byte) 0x00, (byte) 0x1d, (byte) 0x4c, (byte) 0x6a,
                (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x78,
                (byte) 0x2f, (byte) 0x73, (byte) 0x65, (byte) 0x63,
                (byte) 0x75, (byte) 0x72, (byte) 0x69, (byte) 0x74,
                (byte) 0x79, (byte) 0x2f, (byte) 0x61, (byte) 0x75,
                (byte) 0x74, (byte) 0x68, (byte) 0x2f, (byte) 0x53,
                (byte) 0x75, (byte) 0x62, (byte) 0x6a, (byte) 0x65,
                (byte) 0x63, (byte) 0x74, (byte) 0x3b, (byte) 0x78,
                (byte) 0x70, (byte) 0x73, (byte) 0x72, (byte) 0x00,
                (byte) 0x14, (byte) 0x6a, (byte) 0x61, (byte) 0x76,
                (byte) 0x61, (byte) 0x2e, (byte) 0x75, (byte) 0x74,
                (byte) 0x69, (byte) 0x6c, (byte) 0x2e, (byte) 0x4c,
                (byte) 0x69, (byte) 0x6e, (byte) 0x6b, (byte) 0x65,
                (byte) 0x64, (byte) 0x4c, (byte) 0x69, (byte) 0x73,
                (byte) 0x74, (byte) 0x0c, (byte) 0x29, (byte) 0x53,
                (byte) 0x5d, (byte) 0x4a, (byte) 0x60, (byte) 0x88,
                (byte) 0x22, (byte) 0x03, (byte) 0x00, (byte) 0x00,
                (byte) 0x78, (byte) 0x70, (byte) 0x77, (byte) 0x04,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
                (byte) 0x70, (byte) 0x78, (byte) 0x73, (byte) 0x72,
                (byte) 0x00, (byte) 0x1b, (byte) 0x6a, (byte) 0x61,
                (byte) 0x76, (byte) 0x61, (byte) 0x78, (byte) 0x2e,
                (byte) 0x73, (byte) 0x65, (byte) 0x63, (byte) 0x75,
                (byte) 0x72, (byte) 0x69, (byte) 0x74, (byte) 0x79,
                (byte) 0x2e, (byte) 0x61, (byte) 0x75, (byte) 0x74,
                (byte) 0x68, (byte) 0x2e, (byte) 0x53, (byte) 0x75,
                (byte) 0x62, (byte) 0x6a, (byte) 0x65, (byte) 0x63,
                (byte) 0x74, (byte) 0x8c, (byte) 0xb2, (byte) 0x32,
                (byte) 0x93, (byte) 0x00, (byte) 0x33, (byte) 0xfa,
                (byte) 0x68, (byte) 0x03, (byte) 0x00, (byte) 0x02,
                (byte) 0x5a, (byte) 0x00, (byte) 0x0a, (byte) 0x69,
                (byte) 0x73, (byte) 0x52, (byte) 0x65, (byte) 0x61,
                (byte) 0x64, (byte) 0x4f, (byte) 0x6e, (byte) 0x6c,
                (byte) 0x79, (byte) 0x4c, (byte) 0x00, (byte) 0x0a,
                (byte) 0x70, (byte) 0x72, (byte) 0x69, (byte) 0x6e,
                (byte) 0x63, (byte) 0x69, (byte) 0x70, (byte) 0x61,
                (byte) 0x6c, (byte) 0x73, (byte) 0x74, (byte) 0x00,
                (byte) 0x0f, (byte) 0x4c, (byte) 0x6a, (byte) 0x61,
                (byte) 0x76, (byte) 0x61, (byte) 0x2f, (byte) 0x75,
                (byte) 0x74, (byte) 0x69, (byte) 0x6c, (byte) 0x2f,
                (byte) 0x53, (byte) 0x65, (byte) 0x74, (byte) 0x3b,
                (byte) 0x78, (byte) 0x70, (byte) 0x00, (byte) 0x71,
                (byte) 0x00, (byte) 0x7e, (byte) 0x00, (byte) 0x03,
                (byte) 0x78, (byte) 0x78 };

        // The array was produced in the following way:
        // 1) A check for verifying that passed principal object 
        //    implements Principal interface was disabled in Subject class.
        // 2) Subject object was created
        // 3) A serializable object was added to subject's principal
        //    set by invoking: getPrincipals().add(object);
        // 4) ByteArrayOutputStream class was used to write
        //    subject's principal set object and to get resulting array of bytes 
        byte[] notPrincipalElement = new byte[] { (byte) 0xac, (byte) 0xed,
                (byte) 0x00, (byte) 0x05, (byte) 0x73, (byte) 0x72,
                (byte) 0x00, (byte) 0x25, (byte) 0x6a, (byte) 0x61,
                (byte) 0x76, (byte) 0x61, (byte) 0x78, (byte) 0x2e,
                (byte) 0x73, (byte) 0x65, (byte) 0x63, (byte) 0x75,
                (byte) 0x72, (byte) 0x69, (byte) 0x74, (byte) 0x79,
                (byte) 0x2e, (byte) 0x61, (byte) 0x75, (byte) 0x74,
                (byte) 0x68, (byte) 0x2e, (byte) 0x53, (byte) 0x75,
                (byte) 0x62, (byte) 0x6a, (byte) 0x65, (byte) 0x63,
                (byte) 0x74, (byte) 0x24, (byte) 0x53, (byte) 0x65,
                (byte) 0x63, (byte) 0x75, (byte) 0x72, (byte) 0x65,
                (byte) 0x53, (byte) 0x65, (byte) 0x74, (byte) 0x6d,
                (byte) 0xcc, (byte) 0x32, (byte) 0x80, (byte) 0x17,
                (byte) 0x55, (byte) 0x7e, (byte) 0x27, (byte) 0x03,
                (byte) 0x00, (byte) 0x02, (byte) 0x4c, (byte) 0x00,
                (byte) 0x08, (byte) 0x65, (byte) 0x6c, (byte) 0x65,
                (byte) 0x6d, (byte) 0x65, (byte) 0x6e, (byte) 0x74,
                (byte) 0x73, (byte) 0x74, (byte) 0x00, (byte) 0x16,
                (byte) 0x4c, (byte) 0x6a, (byte) 0x61, (byte) 0x76,
                (byte) 0x61, (byte) 0x2f, (byte) 0x75, (byte) 0x74,
                (byte) 0x69, (byte) 0x6c, (byte) 0x2f, (byte) 0x4c,
                (byte) 0x69, (byte) 0x6e, (byte) 0x6b, (byte) 0x65,
                (byte) 0x64, (byte) 0x4c, (byte) 0x69, (byte) 0x73,
                (byte) 0x74, (byte) 0x3b, (byte) 0x4c, (byte) 0x00,
                (byte) 0x06, (byte) 0x74, (byte) 0x68, (byte) 0x69,
                (byte) 0x73, (byte) 0x24, (byte) 0x30, (byte) 0x74,
                (byte) 0x00, (byte) 0x1d, (byte) 0x4c, (byte) 0x6a,
                (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x78,
                (byte) 0x2f, (byte) 0x73, (byte) 0x65, (byte) 0x63,
                (byte) 0x75, (byte) 0x72, (byte) 0x69, (byte) 0x74,
                (byte) 0x79, (byte) 0x2f, (byte) 0x61, (byte) 0x75,
                (byte) 0x74, (byte) 0x68, (byte) 0x2f, (byte) 0x53,
                (byte) 0x75, (byte) 0x62, (byte) 0x6a, (byte) 0x65,
                (byte) 0x63, (byte) 0x74, (byte) 0x3b, (byte) 0x78,
                (byte) 0x70, (byte) 0x73, (byte) 0x72, (byte) 0x00,
                (byte) 0x14, (byte) 0x6a, (byte) 0x61, (byte) 0x76,
                (byte) 0x61, (byte) 0x2e, (byte) 0x75, (byte) 0x74,
                (byte) 0x69, (byte) 0x6c, (byte) 0x2e, (byte) 0x4c,
                (byte) 0x69, (byte) 0x6e, (byte) 0x6b, (byte) 0x65,
                (byte) 0x64, (byte) 0x4c, (byte) 0x69, (byte) 0x73,
                (byte) 0x74, (byte) 0x0c, (byte) 0x29, (byte) 0x53,
                (byte) 0x5d, (byte) 0x4a, (byte) 0x60, (byte) 0x88,
                (byte) 0x22, (byte) 0x03, (byte) 0x00, (byte) 0x00,
                (byte) 0x78, (byte) 0x70, (byte) 0x77, (byte) 0x04,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
                (byte) 0x73, (byte) 0x72, (byte) 0x00, (byte) 0x28,
                (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61,
                (byte) 0x78, (byte) 0x2e, (byte) 0x73, (byte) 0x65,
                (byte) 0x63, (byte) 0x75, (byte) 0x72, (byte) 0x69,
                (byte) 0x74, (byte) 0x79, (byte) 0x2e, (byte) 0x61,
                (byte) 0x75, (byte) 0x74, (byte) 0x68, (byte) 0x2e,
                (byte) 0x53, (byte) 0x75, (byte) 0x62, (byte) 0x6a,
                (byte) 0x65, (byte) 0x63, (byte) 0x74, (byte) 0x54,
                (byte) 0x65, (byte) 0x73, (byte) 0x74, (byte) 0x24,
                (byte) 0x4d, (byte) 0x79, (byte) 0x4f, (byte) 0x62,
                (byte) 0x6a, (byte) 0x65, (byte) 0x63, (byte) 0x74,
                (byte) 0xf7, (byte) 0xbc, (byte) 0xdc, (byte) 0x95,
                (byte) 0xb2, (byte) 0x33, (byte) 0x3a, (byte) 0x0f,
                (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x78,
                (byte) 0x70, (byte) 0x78, (byte) 0x73, (byte) 0x72,
                (byte) 0x00, (byte) 0x1b, (byte) 0x6a, (byte) 0x61,
                (byte) 0x76, (byte) 0x61, (byte) 0x78, (byte) 0x2e,
                (byte) 0x73, (byte) 0x65, (byte) 0x63, (byte) 0x75,
                (byte) 0x72, (byte) 0x69, (byte) 0x74, (byte) 0x79,
                (byte) 0x2e, (byte) 0x61, (byte) 0x75, (byte) 0x74,
                (byte) 0x68, (byte) 0x2e, (byte) 0x53, (byte) 0x75,
                (byte) 0x62, (byte) 0x6a, (byte) 0x65, (byte) 0x63,
                (byte) 0x74, (byte) 0x8c, (byte) 0xb2, (byte) 0x32,
                (byte) 0x93, (byte) 0x00, (byte) 0x33, (byte) 0xfa,
                (byte) 0x68, (byte) 0x03, (byte) 0x00, (byte) 0x02,
                (byte) 0x5a, (byte) 0x00, (byte) 0x0a, (byte) 0x69,
                (byte) 0x73, (byte) 0x52, (byte) 0x65, (byte) 0x61,
                (byte) 0x64, (byte) 0x4f, (byte) 0x6e, (byte) 0x6c,
                (byte) 0x79, (byte) 0x4c, (byte) 0x00, (byte) 0x0a,
                (byte) 0x70, (byte) 0x72, (byte) 0x69, (byte) 0x6e,
                (byte) 0x63, (byte) 0x69, (byte) 0x70, (byte) 0x61,
                (byte) 0x6c, (byte) 0x73, (byte) 0x74, (byte) 0x00,
                (byte) 0x0f, (byte) 0x4c, (byte) 0x6a, (byte) 0x61,
                (byte) 0x76, (byte) 0x61, (byte) 0x2f, (byte) 0x75,
                (byte) 0x74, (byte) 0x69, (byte) 0x6c, (byte) 0x2f,
                (byte) 0x53, (byte) 0x65, (byte) 0x74, (byte) 0x3b,
                (byte) 0x78, (byte) 0x70, (byte) 0x00, (byte) 0x71,
                (byte) 0x00, (byte) 0x7e, (byte) 0x00, (byte) 0x03,
                (byte) 0x78, (byte) 0x78 };

        ByteArrayInputStream in = new ByteArrayInputStream(nullElement);
        ObjectInputStream sIn = new ObjectInputStream(in);

        try {
            sIn.readObject();
            if (!testing) {
                fail("No expected NullPointerException");
            }
        } catch (NullPointerException e) {
        } finally {
            sIn.close();
        }

        in = new ByteArrayInputStream(notPrincipalElement);
        sIn = new ObjectInputStream(in);

        try {
            sIn.readObject();
            if (!testing) {
                fail("No expected IllegalArgumentException");
            }
        } catch (IllegalArgumentException e) {
        } finally {
            sIn.close();
        }
    }

    /**
     * Test subject's private credential set deserialization in case
     * of invalid null element.
     */
    public void test_PrivateCredentialSetInvalidSerForm() throws Exception {

        // The array was produced in the following way:
        // 1) A check that verifies a passed private credential object for null
        //    value was disabled in Subject class.
        // 2) Subject object was created
        // 3) A null was added to subject's private credential set by invoking
        //        getPrivateCredentials().add(null);
        // 4) ByteArrayOutputStream class was used to write
        //    subject's private credential set object
        //    and to get resulting array of bytes 
        byte[] nullElement = new byte[] { (byte) 0xac, (byte) 0xed,
                (byte) 0x00, (byte) 0x05, (byte) 0x73, (byte) 0x72,
                (byte) 0x00, (byte) 0x25, (byte) 0x6a, (byte) 0x61,
                (byte) 0x76, (byte) 0x61, (byte) 0x78, (byte) 0x2e,
                (byte) 0x73, (byte) 0x65, (byte) 0x63, (byte) 0x75,
                (byte) 0x72, (byte) 0x69, (byte) 0x74, (byte) 0x79,
                (byte) 0x2e, (byte) 0x61, (byte) 0x75, (byte) 0x74,
                (byte) 0x68, (byte) 0x2e, (byte) 0x53, (byte) 0x75,
                (byte) 0x62, (byte) 0x6a, (byte) 0x65, (byte) 0x63,
                (byte) 0x74, (byte) 0x24, (byte) 0x53, (byte) 0x65,
                (byte) 0x63, (byte) 0x75, (byte) 0x72, (byte) 0x65,
                (byte) 0x53, (byte) 0x65, (byte) 0x74, (byte) 0x6d,
                (byte) 0xcc, (byte) 0x32, (byte) 0x80, (byte) 0x17,
                (byte) 0x55, (byte) 0x7e, (byte) 0x27, (byte) 0x03,
                (byte) 0x00, (byte) 0x03, (byte) 0x49, (byte) 0x00,
                (byte) 0x07, (byte) 0x73, (byte) 0x65, (byte) 0x74,
                (byte) 0x54, (byte) 0x79, (byte) 0x70, (byte) 0x65,
                (byte) 0x4c, (byte) 0x00, (byte) 0x08, (byte) 0x65,
                (byte) 0x6c, (byte) 0x65, (byte) 0x6d, (byte) 0x65,
                (byte) 0x6e, (byte) 0x74, (byte) 0x73, (byte) 0x74,
                (byte) 0x00, (byte) 0x16, (byte) 0x4c, (byte) 0x6a,
                (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2f,
                (byte) 0x75, (byte) 0x74, (byte) 0x69, (byte) 0x6c,
                (byte) 0x2f, (byte) 0x4c, (byte) 0x69, (byte) 0x6e,
                (byte) 0x6b, (byte) 0x65, (byte) 0x64, (byte) 0x4c,
                (byte) 0x69, (byte) 0x73, (byte) 0x74, (byte) 0x3b,
                (byte) 0x4c, (byte) 0x00, (byte) 0x06, (byte) 0x74,
                (byte) 0x68, (byte) 0x69, (byte) 0x73, (byte) 0x24,
                (byte) 0x30, (byte) 0x74, (byte) 0x00, (byte) 0x1d,
                (byte) 0x4c, (byte) 0x6a, (byte) 0x61, (byte) 0x76,
                (byte) 0x61, (byte) 0x78, (byte) 0x2f, (byte) 0x73,
                (byte) 0x65, (byte) 0x63, (byte) 0x75, (byte) 0x72,
                (byte) 0x69, (byte) 0x74, (byte) 0x79, (byte) 0x2f,
                (byte) 0x61, (byte) 0x75, (byte) 0x74, (byte) 0x68,
                (byte) 0x2f, (byte) 0x53, (byte) 0x75, (byte) 0x62,
                (byte) 0x6a, (byte) 0x65, (byte) 0x63, (byte) 0x74,
                (byte) 0x3b, (byte) 0x78, (byte) 0x70, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x73,
                (byte) 0x72, (byte) 0x00, (byte) 0x14, (byte) 0x6a,
                (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2e,
                (byte) 0x75, (byte) 0x74, (byte) 0x69, (byte) 0x6c,
                (byte) 0x2e, (byte) 0x4c, (byte) 0x69, (byte) 0x6e,
                (byte) 0x6b, (byte) 0x65, (byte) 0x64, (byte) 0x4c,
                (byte) 0x69, (byte) 0x73, (byte) 0x74, (byte) 0x0c,
                (byte) 0x29, (byte) 0x53, (byte) 0x5d, (byte) 0x4a,
                (byte) 0x60, (byte) 0x88, (byte) 0x22, (byte) 0x03,
                (byte) 0x00, (byte) 0x00, (byte) 0x78, (byte) 0x70,
                (byte) 0x77, (byte) 0x04, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x01, (byte) 0x70, (byte) 0x78,
                (byte) 0x73, (byte) 0x72, (byte) 0x00, (byte) 0x1b,
                (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61,
                (byte) 0x78, (byte) 0x2e, (byte) 0x73, (byte) 0x65,
                (byte) 0x63, (byte) 0x75, (byte) 0x72, (byte) 0x69,
                (byte) 0x74, (byte) 0x79, (byte) 0x2e, (byte) 0x61,
                (byte) 0x75, (byte) 0x74, (byte) 0x68, (byte) 0x2e,
                (byte) 0x53, (byte) 0x75, (byte) 0x62, (byte) 0x6a,
                (byte) 0x65, (byte) 0x63, (byte) 0x74, (byte) 0x8c,
                (byte) 0xb2, (byte) 0x32, (byte) 0x93, (byte) 0x00,
                (byte) 0x33, (byte) 0xfa, (byte) 0x68, (byte) 0x03,
                (byte) 0x00, (byte) 0x02, (byte) 0x5a, (byte) 0x00,
                (byte) 0x0a, (byte) 0x69, (byte) 0x73, (byte) 0x52,
                (byte) 0x65, (byte) 0x61, (byte) 0x64, (byte) 0x4f,
                (byte) 0x6e, (byte) 0x6c, (byte) 0x79, (byte) 0x4c,
                (byte) 0x00, (byte) 0x0a, (byte) 0x70, (byte) 0x72,
                (byte) 0x69, (byte) 0x6e, (byte) 0x63, (byte) 0x69,
                (byte) 0x70, (byte) 0x61, (byte) 0x6c, (byte) 0x73,
                (byte) 0x74, (byte) 0x00, (byte) 0x0f, (byte) 0x4c,
                (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61,
                (byte) 0x2f, (byte) 0x75, (byte) 0x74, (byte) 0x69,
                (byte) 0x6c, (byte) 0x2f, (byte) 0x53, (byte) 0x65,
                (byte) 0x74, (byte) 0x3b, (byte) 0x78, (byte) 0x70,
                (byte) 0x00, (byte) 0x73, (byte) 0x71, (byte) 0x00,
                (byte) 0x7e, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x73,
                (byte) 0x71, (byte) 0x00, (byte) 0x7e, (byte) 0x00,
                (byte) 0x04, (byte) 0x77, (byte) 0x04, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x78,
                (byte) 0x71, (byte) 0x00, (byte) 0x7e, (byte) 0x00,
                (byte) 0x08, (byte) 0x78, (byte) 0x78, (byte) 0x78 };

        ByteArrayInputStream in = new ByteArrayInputStream(nullElement);
        ObjectInputStream sIn = new ObjectInputStream(in);

        try {
            sIn.readObject();
            if (!testing) {
                fail("No expected NullPointerException");
            }
        } catch (NullPointerException e) {
        } finally {
            sIn.close();
        }
    }

    public static class PermissionTest extends SecurityTest {

        private final Subject subject = new Subject();

        /*
         * FIXME??? presence of unaccessible element
         * forbids all operations except adding new elements  
         */
        public void testForbiddenElement() {

            grantMode(); // no permissions
            grantPermission(new AuthPermission("modifyPrivateCredentials"));

            Principal privCr1 = new MyClass1();
            Object privCr2 = new Object();

            HashSet<Object> hash = new HashSet<Object>();
            hash.add(privCr1);
            hash.add(new Object());

            Set<Object> set = subject.getPrivateCredentials();

            // Adding is not prohibited
            set.add(privCr1);

            set.add(privCr2);

            try {
                set.clear();
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                // PrivateCredentialPermission check goes first
                assertEquals(e, PrivateCredentialPermission.class);
            }

            try {
                set.contains(privCr1);
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, PrivateCredentialPermission.class);
            }

            try {
                set.contains(new Object());
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, PrivateCredentialPermission.class);
            }

            assertTrue(set.equals(set));
            assertFalse(set.equals(new HashSet<Object>()));
            try {
                // set with equal size initiates iteration
                set.equals(hash);
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, PrivateCredentialPermission.class);
            }

            set.isEmpty();

            try {
                set.hashCode();
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, PrivateCredentialPermission.class);
            }

            try {
                set.remove(privCr1);
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                // PrivateCredentialPermission check goes first
                assertEquals(e, PrivateCredentialPermission.class);
            }

            try {
                set.remove(new Object());
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                // PrivateCredentialPermission check goes first
                assertEquals(e, PrivateCredentialPermission.class);
            }

            try {
                set.retainAll(new HashSet<Object>());
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                // PrivateCredentialPermission check goes first
                assertEquals(e, PrivateCredentialPermission.class);
            }

            try {
                set.toArray();
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, PrivateCredentialPermission.class);
            }

            try {
                set.toArray(new Object[5]);
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, PrivateCredentialPermission.class);
            }
        }

        public void testIteratorNext_EmptySet() {

            grantMode(); // no permissions
            try {
                (new Subject()).getPrivateCredentials().iterator().next();
                fail("No expected NoSuchElementException");
            } catch (NoSuchElementException e) {
            } catch (IndexOutOfBoundsException e) {
                if (!testing) {
                    throw e;
                }
            }
        }

        public void testIteratorNext() {

            subject.getPrincipals().add(new MyClass1());

            Set<Object> set = subject.getPrivateCredentials();

            Object obj1 = new Object();
            Object obj2 = new Object();
            Object obj3 = new Object();

            set.add(obj1);
            set.add(new HashSet<Object>());
            set.add(obj2);
            set.add(new HashSet<Object>());
            set.add(obj3);

            grantMode(); // no permissions

            HashSet<Object> hash = new HashSet<Object>();

            grantPermission(new PrivateCredentialPermission(
                    "java.lang.Object * \"*\"", "read"));

            Iterator<Object> it = set.iterator();
            while (it.hasNext()) {
                try {
                    hash.add(it.next());
                } catch (AccessControlException e) {
                    assertEquals(e, PrivateCredentialPermission.class);
                }
            }

            assertEquals("Size: ", 3, hash.size());
            assertTrue("1 element", hash.contains(obj1));
            assertTrue("2 element", hash.contains(obj2));
            assertTrue("3 element", hash.contains(obj3));
        }

        public void test_Remove_NotExistingElement_EmptySet() {

            denyPermission(new PrivateCredentialPermission("* * \"*\"", "read"));

            subject.getPrivateCredentials().remove(new Object());
        }

        public void test_PrivateCredentialPermission() {

            if (!testing) {
                class P implements Principal {
                    public String getName() {
                        return "name";
                    }
                }

                P p = new P();
                HashSet<Principal> hash = new HashSet<Principal>();
                hash.add(p);

                PrivateCredentialPermission p1 = new PrivateCredentialPermission(
                        "java.lang.Object", hash);

                PrivateCredentialPermission p2 = new PrivateCredentialPermission(
                        "java.lang.Object " + P.class.getName() + " \"name\"",
                        "read");

                assertTrue(p1.implies(p2));
                assertTrue(p2.implies(p1));
            }

            PrivateCredentialPermission p3 = new PrivateCredentialPermission(
                    "java.lang.Object * \"*\"", "read");
            PrivateCredentialPermission p4 = new PrivateCredentialPermission(
                    "java.lang.Object", new HashSet<Principal>());

            assertTrue(p3.implies(p4));
        }

        public void test_Principal() {

            Principal p1 = new MyClass1();
            Principal p2 = new MyClass2();

            HashSet<Principal> hash = new HashSet<Principal>();
            hash.add(p2);

            Set<Object> set = subject.getPrivateCredentials();

            set.add(new Object());

            grantMode(); // no permissions

            grantPermission(new AuthPermission("modifyPrincipals"));
            grantPermission(getPermission("java.lang.Object", hash));

            Iterator<Object> it = set.iterator();
            it.next();

            subject.getPrincipals().add(p1);
            it = set.iterator();
            try {
                it.next();
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, PrivateCredentialPermission.class);
            }

            subject.getPrincipals().add(p2);

            it = set.iterator();
            it.next();
        }

        public void test_Serialization() throws Exception {

            subject.getPrivateCredentials().add(new MyClass1());

            denyPermission(new PrivateCredentialPermission("* * \"*\"", "read"));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream sOut = new ObjectOutputStream(out);

            try {
                sOut.writeObject(subject.getPrivateCredentials());
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, PrivateCredentialPermission.class);
            } finally {
                sOut.close();
            }
        }

        @SuppressWarnings("unchecked")
        public void testGetClass() {

            HashSet hash = new HashSet();
            hash.add(new MyClass1());

            subject.getPrincipals().add(new MyClass1());

            subject.getPrivateCredentials().add(new MyClass1());
            subject.getPrivateCredentials().add(new MyClass2());

            grantMode(); // no permissions

            try {
                subject.getPrivateCredentials(MyClass1.class);
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, PrivateCredentialPermission.class);
            }

            try {
                subject.getPrivateCredentials(MyClass2.class);
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, PrivateCredentialPermission.class);
            }

            // subject hash partial permissions (only for MyClass1 class)
            grantPermission(getPermission(MyClass1.class.getName(), hash));

            // FIXME why security exception is thrown? 
            // the spec. require permissions for requested class only
            try {
                subject.getPrivateCredentials(MyClass1.class);
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, PrivateCredentialPermission.class);
            }

            try {
                subject.getPrivateCredentials(MyClass2.class);
                fail("No expected AccessControlException");
            } catch (AccessControlException e) {
                assertEquals(e, PrivateCredentialPermission.class);
            }

            // now subject has all permissions 
            grantPermission(getPermission(MyClass2.class.getName(), hash));

            subject.getPrivateCredentials(MyClass1.class);
            subject.getPrivateCredentials(MyClass2.class);
        }

        public PrivateCredentialPermission getPermission(String c, Set<? extends Principal> p) {
            StringBuffer buf = new StringBuffer(c);

            for (Iterator<? extends Principal> it = p.iterator(); it.hasNext();) {
                Object o = it.next();
                buf.append(" ");
                buf.append(o.getClass().getName());
                buf.append(" \"");
                buf.append(((Principal) o).getName());
                buf.append("\"");
            }
            return new PrivateCredentialPermission(buf.toString(), "read");
        }
    }

    /**
     * Test subject's public credential set deserialization in case
     * of invalid null element.
     */
    public void test_PublicCredentialInvalidSerForm() throws Exception {

        // The array was produced in the following way:
        // 1) A check that verifies a passed public credential object for null
        //    value was disabled in Subject class.
        // 2) Subject object was created
        // 3) A null was added to subject's public credential set by invoking
        //        getPublicCredentials().add(null);
        // 4) ByteArrayOutputStream class was used to write
        //    subject's public credential set object
        //    and to get resulting array of bytes 
        byte[] nullElement = new byte[] { (byte) 0xac, (byte) 0xed,
                (byte) 0x00, (byte) 0x05, (byte) 0x73, (byte) 0x72,
                (byte) 0x00, (byte) 0x25, (byte) 0x6a, (byte) 0x61,
                (byte) 0x76, (byte) 0x61, (byte) 0x78, (byte) 0x2e,
                (byte) 0x73, (byte) 0x65, (byte) 0x63, (byte) 0x75,
                (byte) 0x72, (byte) 0x69, (byte) 0x74, (byte) 0x79,
                (byte) 0x2e, (byte) 0x61, (byte) 0x75, (byte) 0x74,
                (byte) 0x68, (byte) 0x2e, (byte) 0x53, (byte) 0x75,
                (byte) 0x62, (byte) 0x6a, (byte) 0x65, (byte) 0x63,
                (byte) 0x74, (byte) 0x24, (byte) 0x53, (byte) 0x65,
                (byte) 0x63, (byte) 0x75, (byte) 0x72, (byte) 0x65,
                (byte) 0x53, (byte) 0x65, (byte) 0x74, (byte) 0x6d,
                (byte) 0xcc, (byte) 0x32, (byte) 0x80, (byte) 0x17,
                (byte) 0x55, (byte) 0x7e, (byte) 0x27, (byte) 0x03,
                (byte) 0x00, (byte) 0x03, (byte) 0x49, (byte) 0x00,
                (byte) 0x07, (byte) 0x73, (byte) 0x65, (byte) 0x74,
                (byte) 0x54, (byte) 0x79, (byte) 0x70, (byte) 0x65,
                (byte) 0x4c, (byte) 0x00, (byte) 0x08, (byte) 0x65,
                (byte) 0x6c, (byte) 0x65, (byte) 0x6d, (byte) 0x65,
                (byte) 0x6e, (byte) 0x74, (byte) 0x73, (byte) 0x74,
                (byte) 0x00, (byte) 0x16, (byte) 0x4c, (byte) 0x6a,
                (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2f,
                (byte) 0x75, (byte) 0x74, (byte) 0x69, (byte) 0x6c,
                (byte) 0x2f, (byte) 0x4c, (byte) 0x69, (byte) 0x6e,
                (byte) 0x6b, (byte) 0x65, (byte) 0x64, (byte) 0x4c,
                (byte) 0x69, (byte) 0x73, (byte) 0x74, (byte) 0x3b,
                (byte) 0x4c, (byte) 0x00, (byte) 0x06, (byte) 0x74,
                (byte) 0x68, (byte) 0x69, (byte) 0x73, (byte) 0x24,
                (byte) 0x30, (byte) 0x74, (byte) 0x00, (byte) 0x1d,
                (byte) 0x4c, (byte) 0x6a, (byte) 0x61, (byte) 0x76,
                (byte) 0x61, (byte) 0x78, (byte) 0x2f, (byte) 0x73,
                (byte) 0x65, (byte) 0x63, (byte) 0x75, (byte) 0x72,
                (byte) 0x69, (byte) 0x74, (byte) 0x79, (byte) 0x2f,
                (byte) 0x61, (byte) 0x75, (byte) 0x74, (byte) 0x68,
                (byte) 0x2f, (byte) 0x53, (byte) 0x75, (byte) 0x62,
                (byte) 0x6a, (byte) 0x65, (byte) 0x63, (byte) 0x74,
                (byte) 0x3b, (byte) 0x78, (byte) 0x70, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x73,
                (byte) 0x72, (byte) 0x00, (byte) 0x14, (byte) 0x6a,
                (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2e,
                (byte) 0x75, (byte) 0x74, (byte) 0x69, (byte) 0x6c,
                (byte) 0x2e, (byte) 0x4c, (byte) 0x69, (byte) 0x6e,
                (byte) 0x6b, (byte) 0x65, (byte) 0x64, (byte) 0x4c,
                (byte) 0x69, (byte) 0x73, (byte) 0x74, (byte) 0x0c,
                (byte) 0x29, (byte) 0x53, (byte) 0x5d, (byte) 0x4a,
                (byte) 0x60, (byte) 0x88, (byte) 0x22, (byte) 0x03,
                (byte) 0x00, (byte) 0x00, (byte) 0x78, (byte) 0x70,
                (byte) 0x77, (byte) 0x04, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x01, (byte) 0x70, (byte) 0x78,
                (byte) 0x73, (byte) 0x72, (byte) 0x00, (byte) 0x1b,
                (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61,
                (byte) 0x78, (byte) 0x2e, (byte) 0x73, (byte) 0x65,
                (byte) 0x63, (byte) 0x75, (byte) 0x72, (byte) 0x69,
                (byte) 0x74, (byte) 0x79, (byte) 0x2e, (byte) 0x61,
                (byte) 0x75, (byte) 0x74, (byte) 0x68, (byte) 0x2e,
                (byte) 0x53, (byte) 0x75, (byte) 0x62, (byte) 0x6a,
                (byte) 0x65, (byte) 0x63, (byte) 0x74, (byte) 0x8c,
                (byte) 0xb2, (byte) 0x32, (byte) 0x93, (byte) 0x00,
                (byte) 0x33, (byte) 0xfa, (byte) 0x68, (byte) 0x03,
                (byte) 0x00, (byte) 0x02, (byte) 0x5a, (byte) 0x00,
                (byte) 0x0a, (byte) 0x69, (byte) 0x73, (byte) 0x52,
                (byte) 0x65, (byte) 0x61, (byte) 0x64, (byte) 0x4f,
                (byte) 0x6e, (byte) 0x6c, (byte) 0x79, (byte) 0x4c,
                (byte) 0x00, (byte) 0x0a, (byte) 0x70, (byte) 0x72,
                (byte) 0x69, (byte) 0x6e, (byte) 0x63, (byte) 0x69,
                (byte) 0x70, (byte) 0x61, (byte) 0x6c, (byte) 0x73,
                (byte) 0x74, (byte) 0x00, (byte) 0x0f, (byte) 0x4c,
                (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61,
                (byte) 0x2f, (byte) 0x75, (byte) 0x74, (byte) 0x69,
                (byte) 0x6c, (byte) 0x2f, (byte) 0x53, (byte) 0x65,
                (byte) 0x74, (byte) 0x3b, (byte) 0x78, (byte) 0x70,
                (byte) 0x00, (byte) 0x73, (byte) 0x71, (byte) 0x00,
                (byte) 0x7e, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x73,
                (byte) 0x71, (byte) 0x00, (byte) 0x7e, (byte) 0x00,
                (byte) 0x04, (byte) 0x77, (byte) 0x04, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x78,
                (byte) 0x71, (byte) 0x00, (byte) 0x7e, (byte) 0x00,
                (byte) 0x08, (byte) 0x78, (byte) 0x78, (byte) 0x78 };

        ByteArrayInputStream in = new ByteArrayInputStream(nullElement);
        ObjectInputStream sIn = new ObjectInputStream(in);

        try {
            sIn.readObject();
            if (!testing) {
                fail("No expected NullPointerException");
            }
        } catch (NullPointerException e) {
        } finally {
            sIn.close();
        }
    }

    @SuppressWarnings("serial")
    public static class MyClass1 implements Principal, Serializable {
        public String getName() {
            return "MyClass1";
        }
    }

    public static class MyClass2 implements Principal {
        public String getName() {
            return "MyClass2";
        }
    }

    @SuppressWarnings("serial")
    public static class MyObject implements Serializable {
    }

    public static class PrincipalTestSuite extends TestSuite {

        public PrincipalTestSuite() {
            super("Principal");

            TestSuite iterator = new TestSuite("Iterator");

            iterator
                    .addTest(new TestSuite(IteratorInterface.class, "Interface"));
            iterator.addTest(new TestSuite(IteratorReadOnly.class, "ReadOnly"));
            iterator.addTest(new TestSuite(IteratorSecure.class, "Secure"));

            TestSuite set = new TestSuite("Set");
            set.addTest(new TestSuite(SetInterface.class, "Interface"));
            set
                    .addTest(new TestSuite(UnsupportedNull.class,
                            "UnsupportedNull"));
            set.addTest(new TestSuite(IneligibleElement.class,
                    "IneligibleElement"));
            set.addTest(new TestSuite(ReadOnlySet.class, "ReadOnly"));
            set.addTest(new TestSuite(SecureSet.class, "Secure"));

            TestSuite object = new TestSuite("Object");
            object.addTest(new TestSuite(SObjectTest.class, "Object"));

            addTest(iterator);
            addTest(set);
            addTest(object);
        }

        public static class IteratorInterface extends SecurityTest.IteratorTest {
            public IteratorInterface() {
                set = (new Subject()).getPrincipals();
                element = principal;
            }
        }

        public static class IteratorReadOnly extends
                SecurityTest.ReadOnlyIteratorTest {

            private final Subject subject = new Subject();

            public IteratorReadOnly() {
                set = subject.getPrincipals();
                element = principal;
            }

            @Override
            public void setReadOnly() {
                subject.setReadOnly();
            }
        }

        public static class IteratorSecure extends
                SecurityTest.SecureIteratorTest {

            public IteratorSecure() {
                set = (new Subject()).getPrincipals();
                element = principal;
            }

            @Override
            public void setSecure() {
                denyPermission(new AuthPermission("modifyPrincipals"));
            }
        }

        public static class SetInterface extends SecurityTest.SetTest {
            public SetInterface() {
                set = (new Subject()).getPrincipals();
                element = principal;
            }
        }

        public static class UnsupportedNull extends
                SecurityTest.UnsupportedNullTest {

            public UnsupportedNull() {
                set = (new Subject()).getPrincipals();
                element = principal;
            }
        }

        public static class IneligibleElement extends
                SecurityTest.IneligibleElementTest {

            public IneligibleElement() {
                set = (new Subject()).getPrincipals();
                element = principal;
                iElement = new Object();
            }
        }

        public static class ReadOnlySet extends SecurityTest.ReadOnlySetTest {
            private final Subject subject = new Subject();

            public ReadOnlySet() {
                set = subject.getPrincipals();
                element = principal;
            }

            @Override
            public void setReadOnly() {
                subject.setReadOnly();
            }
        }

        public static class SecureSet extends SecurityTest.SecureSetTest {

            public SecureSet() {
                set = (new Subject()).getPrincipals();
                element = principal;
            }

            @Override
            public void setSecure() {
                denyPermission(new AuthPermission("modifyPrincipals"));
            }
        }

        public static class SObjectTest extends
                SecurityTest.SubjectSetObjectTest {
            public SObjectTest() {
                obj1 = subject.getPrincipals();

                //intentionally another set
                obj2 = subject.getPrivateCredentials();
            }
        }
    }

    public static class PrivateCredentialTestSuite extends TestSuite {

        public PrivateCredentialTestSuite() {
            super("PrivateCredential");

            TestSuite iterator = new TestSuite("Iterator");

            iterator
                    .addTest(new TestSuite(IteratorInterface.class, "Interface"));
            iterator.addTest(new TestSuite(IteratorReadOnly.class, "ReadOnly"));
            iterator.addTest(new TestSuite(IteratorSecure.class, "Secure"));

            TestSuite set = new TestSuite("Set");
            set.addTest(new TestSuite(SetInterface.class, "Interface"));
            set
                    .addTest(new TestSuite(UnsupportedNull.class,
                            "UnsupportedNull"));
            set.addTest(new TestSuite(ReadOnlySet.class, "ReadOnly"));
            set.addTest(new TestSuite(SecureSet.class, "Secure"));
            set.addTest(new TestSuite(PermissionTest.class, "PermissionTest"));

            TestSuite object = new TestSuite("Object");
            object.addTest(new TestSuite(SObjectTest.class, "Object"));

            addTest(iterator);
            addTest(set);
            addTest(object);
        }

        public static class IteratorInterface extends SecurityTest.IteratorTest {
            public IteratorInterface() {
                set = (new Subject()).getPrivateCredentials();
                element = principal;
            }

            @Override
            public void testNext_EmptySet_NoSuchElementException() {

                if (testing) {
                    //Unexpected: IndexOutOfBoundsException
                    try {
                        super.testNext_EmptySet_NoSuchElementException();
                    } catch (IndexOutOfBoundsException e) {
                    }
                } else {
                    super.testNext_EmptySet_NoSuchElementException();
                }
            }

            @Override
            public void testNext_NoSuchElementException() {
                if (testing) {
                    //Unexpected: IndexOutOfBoundsException
                    try {
                        super.testNext_NoSuchElementException();
                    } catch (IndexOutOfBoundsException e) {
                    }
                } else {
                    super.testNext_NoSuchElementException();
                }
            }
        }

        public static class IteratorReadOnly extends
                SecurityTest.ReadOnlyIteratorTest {

            private final Subject subject = new Subject();

            public IteratorReadOnly() {
                set = subject.getPrivateCredentials();
                element = principal;
            }

            @Override
            public void setReadOnly() {
                subject.setReadOnly();
            }

            @Override
            public void testNext_EmptySet_NoSuchElementException() {

                if (testing) {
                    //Unexpected: IndexOutOfBoundsException
                    try {
                        super.testNext_EmptySet_NoSuchElementException();
                    } catch (IndexOutOfBoundsException e) {
                    }
                } else {
                    super.testNext_EmptySet_NoSuchElementException();
                }
            }

            @Override
            public void testNext_NoSuchElementException() {
                if (testing) {
                    //Unexpected: IndexOutOfBoundsException
                    try {
                        super.testNext_NoSuchElementException();
                    } catch (IndexOutOfBoundsException e) {
                    }
                } else {
                    super.testNext_NoSuchElementException();
                }
            }
        }

        public static class IteratorSecure extends
                SecurityTest.SecureIteratorTest {

            public IteratorSecure() {
                set = (new Subject()).getPrivateCredentials();
                element = principal;
            }

            @Override
            public void setSecure() {
                denyPermission(new AuthPermission("modifyPrivateCredentials"));
            }

            @Override
            public void testNext_EmptySet_NoSuchElementException() {

                if (testing) {
                    //Unexpected: IndexOutOfBoundsException
                    try {
                        super.testNext_EmptySet_NoSuchElementException();
                    } catch (IndexOutOfBoundsException e) {
                    }
                } else {
                    super.testNext_EmptySet_NoSuchElementException();
                }
            }

            @Override
            public void testNext_NoSuchElementException() {
                if (testing) {
                    //Unexpected: IndexOutOfBoundsException
                    try {
                        super.testNext_NoSuchElementException();
                    } catch (IndexOutOfBoundsException e) {
                    }
                } else {
                    super.testNext_NoSuchElementException();
                }
            }
        }

        public static class SetInterface extends SecurityTest.SetTest {
            public SetInterface() {
                set = (new Subject()).getPrivateCredentials();
                element = principal;
            }
        }

        public static class UnsupportedNull extends
                SecurityTest.UnsupportedNullTest {

            public UnsupportedNull() {
                set = (new Subject()).getPrivateCredentials();
                element = principal;
            }
        }

        public static class ReadOnlySet extends SecurityTest.ReadOnlySetTest {
            private final Subject subject = new Subject();

            public ReadOnlySet() {
                set = subject.getPrivateCredentials();
                element = principal;
            }

            @Override
            public void setReadOnly() {
                subject.setReadOnly();
            }
        }

        public static class SecureSet extends SecurityTest.SecureSetTest {

            public SecureSet() {
                set = (new Subject()).getPrivateCredentials();
                element = principal;
            }

            @Override
            public void setSecure() {
                denyPermission(new AuthPermission("modifyPrivateCredentials"));
            }
        }

        public static class SObjectTest extends
                SecurityTest.SubjectSetObjectTest {
            public SObjectTest() {
                obj1 = subject.getPrivateCredentials();

                //intentionally another set
                obj2 = subject.getPublicCredentials();
            }
        }
    }

    public static class PublicCredentialTestSuite extends TestSuite {

        public PublicCredentialTestSuite() {
            super("PublicCredential");

            TestSuite iterator = new TestSuite("Iterator");

            iterator
                    .addTest(new TestSuite(IteratorInterface.class, "Interface"));
            iterator.addTest(new TestSuite(IteratorReadOnly.class, "ReadOnly"));
            iterator.addTest(new TestSuite(IteratorSecure.class, "Secure"));

            TestSuite set = new TestSuite("Set");
            set.addTest(new TestSuite(SetInterface.class, "Interface"));
            set
                    .addTest(new TestSuite(UnsupportedNull.class,
                            "UnsupportedNull"));
            set.addTest(new TestSuite(ReadOnlySet.class, "ReadOnly"));
            set.addTest(new TestSuite(SecureSet.class, "Secure"));

            TestSuite object = new TestSuite("Object");
            object.addTest(new TestSuite(SObjectTest.class, "Object"));

            addTest(iterator);
            addTest(set);
            addTest(object);
        }

        public static class IteratorInterface extends SecurityTest.IteratorTest {
            public IteratorInterface() {
                set = (new Subject()).getPublicCredentials();
                element = principal;
            }
        }

        public static class IteratorReadOnly extends
                SecurityTest.ReadOnlyIteratorTest {

            private final Subject subject = new Subject();

            public IteratorReadOnly() {
                set = subject.getPublicCredentials();
                element = principal;
            }

            @Override
            public void setReadOnly() {
                subject.setReadOnly();
            }
        }

        public static class IteratorSecure extends
                SecurityTest.SecureIteratorTest {

            public IteratorSecure() {
                set = (new Subject()).getPublicCredentials();
                element = principal;
            }

            @Override
            public void setSecure() {
                denyPermission(new AuthPermission("modifyPublicCredentials"));
            }
        }

        public static class SetInterface extends SecurityTest.SetTest {
            public SetInterface() {
                set = (new Subject()).getPublicCredentials();
                element = principal;
            }
        }

        public static class UnsupportedNull extends
                SecurityTest.UnsupportedNullTest {

            public UnsupportedNull() {
                set = (new Subject()).getPublicCredentials();
                element = principal;
            }
        }

        public static class ReadOnlySet extends SecurityTest.ReadOnlySetTest {
            private final Subject subject = new Subject();

            public ReadOnlySet() {
                set = subject.getPublicCredentials();
                element = principal;
            }

            @Override
            public void setReadOnly() {
                subject.setReadOnly();
            }
        }

        public static class SecureSet extends SecurityTest.SecureSetTest {

            public SecureSet() {
                set = (new Subject()).getPublicCredentials();
                element = principal;
            }

            @Override
            public void setSecure() {
                denyPermission(new AuthPermission("modifyPublicCredentials"));
            }
        }

        public static class SObjectTest extends
                SecurityTest.SubjectSetObjectTest {
            public SObjectTest() {
                obj1 = subject.getPublicCredentials();

                //intentionally another set
                obj2 = subject.getPrincipals();
            }
        }
    }

    public static class PrincipalClassTestSuite extends TestSuite {

        public PrincipalClassTestSuite() {
            super("PrincipalClass");

            TestSuite set = new TestSuite("Set");
            set.addTest(new TestSuite(SetInterface.class, "Interface"));
            set
                    .addTest(new TestSuite(UnsupportedNull.class,
                            "UnsupportedNull"));
            set.addTest(new TestSuite(IneligibleElement.class,
                    "IneligibleElement"));

            TestSuite object = new TestSuite("Object");
            object.addTest(new TestSuite(SObjectTest.class, "Object"));

            addTest(set);
            addTest(object);
        }

        public static class SetInterface extends SecurityTest.SetTest {
            public SetInterface() {
                set = (new Subject()).getPrincipals(MyClass1.class);
                element = new MyClass1();
            }
        }

        public static class UnsupportedNull extends
                SecurityTest.UnsupportedNullTest {

            public UnsupportedNull() {
                set = (new Subject()).getPrincipals(MyClass1.class);
                element = new MyClass1();
            }
        }

        public static class IneligibleElement extends
                SecurityTest.IneligibleElementTest {

            public IneligibleElement() {
                set = (new Subject()).getPrincipals(MyClass1.class);
                element = new MyClass1();
                iElement = new MyClass2();
            }
        }

        public static class SObjectTest extends
                SecurityTest.SubjectSetObjectTest {
            public SObjectTest() {
                obj1 = subject.getPrincipals(MyClass1.class);

                //intentionally another set
                obj2 = subject.getPrivateCredentials(MyClass1.class);
            }
        }
    }

    public static class PrivateCredentialClassTestSuite extends TestSuite {

        public PrivateCredentialClassTestSuite() {
            super("PrivateCredentialClass");

            TestSuite set = new TestSuite("Set");
            set.addTest(new TestSuite(SetInterface.class, "Interface"));
            set
                    .addTest(new TestSuite(UnsupportedNull.class,
                            "UnsupportedNull"));
            set.addTest(new TestSuite(IneligibleElement.class,
                    "IneligibleElement"));

            TestSuite object = new TestSuite("Object");
            object.addTest(new TestSuite(SObjectTest.class, "Object"));

            addTest(set);
            addTest(object);
        }

        public static class SetInterface extends SecurityTest.SetTest {
            public SetInterface() {
                set = (new Subject()).getPrivateCredentials(MyClass1.class);
                element = new MyClass1();
            }
        }

        public static class UnsupportedNull extends
                SecurityTest.UnsupportedNullTest {

            public UnsupportedNull() {
                set = (new Subject()).getPrivateCredentials(MyClass1.class);
                element = new MyClass1();
            }
        }

        public static class IneligibleElement extends
                SecurityTest.IneligibleElementTest {

            public IneligibleElement() {
                set = (new Subject()).getPrivateCredentials(MyClass1.class);
                element = new MyClass1();
                iElement = new MyClass2();
            }
        }

        public static class SObjectTest extends
                SecurityTest.SubjectSetObjectTest {
            public SObjectTest() {
                obj1 = subject.getPrivateCredentials(MyClass1.class);

                //intentionally another set
                obj2 = subject.getPublicCredentials(MyClass1.class);
            }
        }
    }

    public static class PublicCredentialClassTestSuite extends TestSuite {

        public PublicCredentialClassTestSuite() {
            super("PublicCredentialClass");

            TestSuite set = new TestSuite("Set");
            set.addTest(new TestSuite(SetInterface.class, "Interface"));
            set
                    .addTest(new TestSuite(UnsupportedNull.class,
                            "UnsupportedNull"));
            set.addTest(new TestSuite(IneligibleElement.class,
                    "IneligibleElement"));

            TestSuite object = new TestSuite("Object");
            object.addTest(new TestSuite(SObjectTest.class, "Object"));

            addTest(set);
            addTest(object);
        }

        public static class SetInterface extends SecurityTest.SetTest {
            public SetInterface() {
                set = (new Subject()).getPublicCredentials(MyClass1.class);
                element = new MyClass1();
            }
        }

        public static class UnsupportedNull extends
                SecurityTest.UnsupportedNullTest {

            public UnsupportedNull() {
                set = (new Subject()).getPublicCredentials(MyClass1.class);
                element = new MyClass1();
            }
        }

        public static class IneligibleElement extends
                SecurityTest.IneligibleElementTest {

            public IneligibleElement() {
                set = (new Subject()).getPublicCredentials(MyClass1.class);
                element = new MyClass1();
                iElement = new MyClass2();
            }
        }

        public static class SObjectTest extends
                SecurityTest.SubjectSetObjectTest {
            public SObjectTest() {
                obj1 = subject.getPublicCredentials(MyClass1.class);

                //intentionally another set
                obj2 = subject.getPrincipals(MyClass1.class);
            }
        }
    }
}