/*
 * Copyright (C) 2013 The Android Open Source Project
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
package android.os.cts;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.os.WorkSource;
import android.test.AndroidTestCase;

public class WorkSourceTest extends AndroidTestCase {
    private Constructor<WorkSource> mConstructWS;
    private Object[] mConstructWSArgs = new Object[1];
    private Method mAddUid;
    private Object[] mAddUidArgs = new Object[1];
    private Method mAddUidName;
    private Object[] mAddUidNameArgs = new Object[2];
    private Method mAddReturningNewbs;
    private Object[] mAddReturningNewbsArgs = new Object[1];
    private Method mSetReturningDiffs;
    private Object[] mSetReturningDiffsArgs = new Object[1];
    private Method mStripNames;
    private Object[] mStripNamesArgs = new Object[0];

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mConstructWS = WorkSource.class.getConstructor(new Class[] { int.class });
        mAddUid = WorkSource.class.getMethod("add", new Class[] { int.class });
        mAddUidName = WorkSource.class.getMethod("add", new Class[] { int.class, String.class });
        mAddReturningNewbs = WorkSource.class.getMethod("addReturningNewbs", new Class[] { WorkSource.class });
        mSetReturningDiffs = WorkSource.class.getMethod("setReturningDiffs", new Class[] { WorkSource.class });
        mStripNames = WorkSource.class.getMethod("stripNames", new Class[] {  });
    }

    private WorkSource wsNew(int uid) throws IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        mConstructWSArgs[0] = uid;
        return mConstructWS.newInstance(mConstructWSArgs);
    }

    private WorkSource wsNew(int[] uids) throws IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        WorkSource ws = new WorkSource();
        for (int i=0; i<uids.length; i++) {
            wsAdd(ws, uids[i]);
        }
        checkWorkSource("Constructed", ws, uids);
        return ws;
    }

    private WorkSource wsNew(int[] uids, String[] names) throws IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        WorkSource ws = new WorkSource();
        for (int i=0; i<uids.length; i++) {
            wsAdd(ws, uids[i], names[i]);
        }
        checkWorkSource("Constructed", ws, uids, names);
        return ws;
    }

    private boolean wsAdd(WorkSource ws, int uid) throws IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        mAddUidArgs[0] = uid;
        return (Boolean)mAddUid.invoke(ws, mAddUidArgs);
    }

    private boolean wsAdd(WorkSource ws, int uid, String name) throws IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        mAddUidNameArgs[0] = uid;
        mAddUidNameArgs[1] = name;
        return (Boolean)mAddUidName.invoke(ws, mAddUidNameArgs);
    }

    private WorkSource wsAddReturningNewbs(WorkSource ws, WorkSource other) throws IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        mAddReturningNewbsArgs[0] = other;
        return (WorkSource)mAddReturningNewbs.invoke(ws, mAddReturningNewbsArgs);
    }

    private WorkSource[] wsSetReturningDiffs(WorkSource ws, WorkSource other) throws IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        mSetReturningDiffsArgs[0] = other;
        return (WorkSource[])mSetReturningDiffs.invoke(ws, mSetReturningDiffsArgs);
    }

    private WorkSource wsStripNames(WorkSource ws) throws IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        return (WorkSource)mStripNames.invoke(ws);
    }

    private void printArrays(StringBuilder sb, int[] uids, String[] names) {
        sb.append("{ ");
        for (int i=0; i<uids.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(uids[i]);
            if (names != null) {
                sb.append(" ");
                sb.append(names[i]);
            }
        }
        sb.append(" }");
    }

    private void failWorkSource(String op, WorkSource ws, int[] uids) {
        StringBuilder sb = new StringBuilder();
        sb.append(op);
        sb.append(": Expected: ");
        printArrays(sb, uids, null);
        sb.append(", got: ");
        sb.append(ws);
        fail(sb.toString());
    }

    private void failWorkSource(String op, WorkSource ws, int[] uids, String[] names) {
        StringBuilder sb = new StringBuilder();
        sb.append(op);
        sb.append(": Expected: ");
        printArrays(sb, uids, names);
        sb.append(", got: ");
        sb.append(ws);
        fail(sb.toString());
    }

    private void checkWorkSource(String op, WorkSource ws, int[] uids) {
        if (ws == null || uids == null) {
            if (ws != null) {
                fail(op + ": WorkSource is not null " + ws +", but expected null");
            }
            if (uids != null) {
                fail(op + "WorkSource is null, but expected non-null: " + uids);
            }
            return;
        }
        if (ws.size() != uids.length) {
            failWorkSource(op, ws, uids);
        }
        for (int i=0; i<uids.length; i++) {
            if (uids[i] != ws.get(i)) {
                failWorkSource(op, ws, uids);
            }
        }
    }

    private void checkWorkSource(String op, WorkSource ws, int[] uids, String[] names) {
        if (ws == null || uids == null) {
            if (ws != null) {
                fail(op + ": WorkSource is not null " + ws +", but expected null");
            }
            if (uids != null) {
                fail(op + "WorkSource is null, but expected non-null: " + uids);
            }
            return;
        }
        if (ws.size() != uids.length) {
            failWorkSource(op, ws, uids, names);
        }
        for (int i=0; i<uids.length; i++) {
            if (uids[i] != ws.get(i) || !names[i].equals(ws.getName(i))) {
                failWorkSource(op, ws, uids, names);
            }
        }
    }

    public void testConstructEmpty() {
        checkWorkSource("Empty", new WorkSource(), new int[] { });
    }

    public void testConstructSingle() throws Exception {
        checkWorkSource("Single 1", wsNew(1), new int[] { 1 });
    }

    public void testAddRawOrdered() throws Exception {
        WorkSource ws = wsNew(1);
        wsAdd(ws, 2);
        checkWorkSource("First", ws, new int[] { 1 , 2 });
        wsAdd(ws, 20);
        checkWorkSource("Second", ws, new int[] { 1 , 2, 20 });
        wsAdd(ws, 100);
        checkWorkSource("Third", ws, new int[] { 1, 2, 20, 100 });
    }

    public void testAddRawRevOrdered() throws Exception {
        WorkSource ws = wsNew(100);
        wsAdd(ws, 20);
        checkWorkSource("First", ws, new int[] { 20, 100 });
        wsAdd(ws, 2);
        checkWorkSource("Second", ws, new int[] { 2, 20, 100 });
        wsAdd(ws, 1);
        checkWorkSource("Third", ws, new int[] { 1, 2, 20, 100 });
    }

    public void testAddRawUnordered() throws Exception {
        WorkSource ws = wsNew(10);
        wsAdd(ws, 2);
        checkWorkSource("First", ws, new int[] { 2, 10 });
        wsAdd(ws, 5);
        checkWorkSource("Second", ws, new int[] { 2, 5, 10 });
        wsAdd(ws, 1);
        checkWorkSource("Third", ws, new int[] { 1, 2, 5, 10 });
        wsAdd(ws, 100);
        checkWorkSource("Fourth", ws, new int[] { 1, 2, 5, 10, 100 });
    }

    public void testAddWsOrdered() throws Exception {
        WorkSource ws = wsNew(1);
        ws.add(wsNew(2));
        checkWorkSource("First", ws, new int[] { 1 , 2 });
        ws.add(wsNew(20));
        checkWorkSource("Second", ws, new int[] { 1 , 2, 20 });
        ws.add(wsNew(100));
        checkWorkSource("Third", ws, new int[] { 1 , 2, 20, 100 });
    }

    public void testAddWsRevOrdered() throws Exception {
        WorkSource ws = wsNew(100);
        ws.add(wsNew(20));
        checkWorkSource("First", ws, new int[] { 20, 100 });
        ws.add(wsNew(2));
        checkWorkSource("Second", ws, new int[] { 2, 20, 100 });
        ws.add(wsNew(1));
        checkWorkSource("Third", ws, new int[] { 1, 2, 20, 100 });
    }

    public void testAddWsUnordered() throws Exception {
        WorkSource ws = wsNew(10);
        ws.add(wsNew(2));
        checkWorkSource("First", ws, new int[] { 2, 10 });
        ws.add(wsNew(5));
        checkWorkSource("Second", ws, new int[] { 2, 5, 10 });
        ws.add(wsNew(1));
        checkWorkSource("Third", ws, new int[] { 1, 2, 5, 10 });
        ws.add(wsNew(100));
        checkWorkSource("Fourth", ws, new int[] { 1, 2, 5, 10, 100 });
    }

    private void doTestCombineTwoUids(int[] lhs, int[] rhs, int[] expected, int[] newbs,
            int[] gones) throws Exception {
        WorkSource ws1 = wsNew(lhs);
        WorkSource ws2 = wsNew(rhs);
        ws1.add(ws2);
        checkWorkSource("Add result", ws1, expected);
        ws1 = wsNew(lhs);
        WorkSource wsNewbs = wsAddReturningNewbs(ws1, ws2);
        checkWorkSource("AddReturning result", ws1, expected);
        checkWorkSource("Newbs", wsNewbs, newbs);
        ws1 = wsNew(lhs);
        WorkSource[] res = wsSetReturningDiffs(ws1, ws2);
        checkWorkSource("SetReturning result", ws1, rhs);
        checkWorkSource("Newbs", res[0], newbs);
        checkWorkSource("Gones", res[1], gones);
    }

    private int[] makeRepeatingIntArray(String[] stringarray, int value) {
        if (stringarray == null) {
            return null;
        }
        int[] res = new int[stringarray.length];
        for (int i=0; i<stringarray.length; i++) {
            res[i] = value;
        }
        return res;
    }

    private void doTestCombineTwoNames(String[] lhsnames, String[] rhsnames,
            String[] expectednames, String[] newbnames,
            String[] gonenames) throws Exception {
        int[] lhs = makeRepeatingIntArray(lhsnames, 0);
        int[] rhs = makeRepeatingIntArray(rhsnames, 0);
        int[] expected = makeRepeatingIntArray(expectednames, 0);
        int[] newbs = makeRepeatingIntArray(newbnames, 0);
        int[] gones = makeRepeatingIntArray(gonenames, 0);
        doTestCombineTwoUidsNames(lhs, lhsnames, rhs, rhsnames, expected, expectednames,
                newbs, newbnames, gones, gonenames);
    }

    private void doTestCombineTwoUidsNames(int[] lhs, String[] lhsnames, int[] rhs, String[] rhsnames,
            int[] expected, String[] expectednames, int[] newbs, String[] newbnames,
            int[] gones, String[] gonenames) throws Exception {
        WorkSource ws1 = wsNew(lhs, lhsnames);
        WorkSource ws2 = wsNew(rhs, rhsnames);
        ws1.add(ws2);
        checkWorkSource("Add result", ws1, expected, expectednames);
        ws1 = wsNew(lhs, lhsnames);
        WorkSource wsNewbs = wsAddReturningNewbs(ws1, ws2);
        checkWorkSource("AddReturning result", ws1, expected, expectednames);
        checkWorkSource("Newbs", wsNewbs, newbs, newbnames);
        ws1 = wsNew(lhs, lhsnames);
        WorkSource[] res = wsSetReturningDiffs(ws1, ws2);
        checkWorkSource("SetReturning result", ws1, rhs, rhsnames);
        checkWorkSource("Newbs", res[0], newbs, newbnames);
        checkWorkSource("Gones", res[1], gones, gonenames);
    }

    private String[] makeRepeatingStringArray(int[] intarray, String value) {
        if (intarray == null) {
            return null;
        }
        String[] res = new String[intarray.length];
        for (int i=0; i<intarray.length; i++) {
            res[i] = value;
        }
        return res;
    }

    private String[] makeStringArray(int[] intarray) {
        if (intarray == null) {
            return null;
        }
        String[] res = new String[intarray.length];
        for (int i=0; i<intarray.length; i++) {
            res[i] = Character.toString((char)('A' + intarray[i]));
        }
        return res;
    }

    private void doTestCombineTwo(int[] lhs, int[] rhs, int[] expected, int[] newbs,
            int[] gones) throws Exception {
        doTestCombineTwoUids(lhs, rhs, expected, newbs, gones);
        doTestCombineTwoUidsNames(lhs, makeRepeatingStringArray(lhs, "A"),
                rhs, makeRepeatingStringArray(rhs, "A"),
                expected, makeRepeatingStringArray(expected, "A"),
                newbs, makeRepeatingStringArray(newbs, "A"),
                gones, makeRepeatingStringArray(gones, "A"));
        doTestCombineTwoNames(makeStringArray(lhs), makeStringArray(rhs),
                makeStringArray(expected), makeStringArray(newbs), makeStringArray(gones));
    }

    public void testCombineMultiFront() throws Exception {
        doTestCombineTwo(
                new int[] { 10, 20, 30, 40 },
                new int[] { 1, 2, 15, 16 },
                new int[] { 1, 2, 10, 15, 16, 20, 30, 40 },
                new int[] { 1, 2, 15, 16 },
                new int[] { 10, 20, 30, 40 });
    }

    public void testCombineMultiMiddle() throws Exception {
        doTestCombineTwo(
                new int[] { 10, 20, 30, 40 },
                new int[] { 12, 14, 22 },
                new int[] { 10, 12, 14, 20, 22, 30, 40 },
                new int[] { 12, 14, 22 },
                new int[] { 10, 20, 30, 40 });
    }

    public void testCombineMultiEnd() throws Exception {
        doTestCombineTwo(
                new int[] { 10, 20, 30, 40 },
                new int[] { 35, 45, 50 },
                new int[] { 10, 20, 30, 35, 40, 45, 50 },
                new int[] { 35, 45, 50 },
                new int[] { 10, 20, 30, 40 });
    }

    public void testCombineMultiFull() throws Exception {
        doTestCombineTwo(
                new int[] { 10, 20, 30, 40 },
                new int[] { 1, 2, 15, 35, 50 },
                new int[] { 1, 2, 10, 15, 20, 30, 35, 40, 50 },
                new int[] { 1, 2, 15, 35, 50 },
                new int[] { 10, 20, 30, 40 });
    }

    public void testCombineMultiSame() throws Exception {
        doTestCombineTwo(
                new int[] { 10, 20, 30, 40 },
                new int[] { 10, 20, 30 },
                new int[] { 10, 20, 30, 40 },
                null,
                new int[] { 40 });
    }

    public void testCombineMultiSomeSame() throws Exception {
        doTestCombineTwo(
                new int[] { 10, 20, 30, 40 },
                new int[] { 1, 30, 40 },
                new int[] { 1, 10, 20, 30, 40 },
                new int[] { 1 },
                new int[] { 10, 20 });
    }

    public void testCombineMultiSomeSameUidsNames() throws Exception {
        doTestCombineTwoUidsNames(
                new int[]    { 10,  10,  20,  30,  30,  30,  40 },
                new String[] { "A", "B", "A", "A", "B", "C", "A" },
                new int[]    { 1,   30,  40,  50 },
                new String[] { "A", "A", "B", "A" },
                new int[]    { 1,   10,  10,  20,  30,  30,  30,  40,  40,  50 },
                new String[] { "A", "A", "B", "A", "A", "B", "C", "A", "B", "A" },
                new int[]    { 1,   40,  50 },
                new String[] { "A", "B", "A" },
                new int[]    { 10,  10,  20,  30,  30,  40 },
                new String[] { "A", "B", "A", "B", "C", "A" });
    }

    private void doTestRemoveUids(int[] lhs, int[] rhs, int[] result, boolean diff) throws Exception {
        WorkSource ws1 = wsNew(lhs);
        WorkSource ws2 = wsNew(rhs);
        boolean diffres = ws1.remove(ws2);
        if (diffres != diff) {
            StringBuilder sb = new StringBuilder();
            sb.append("Expected diff ");
            sb.append(diff);
            sb.append(" but got ");
            sb.append(diffres);
            sb.append(" when removing ");
            sb.append(ws2);
            sb.append(" from ");
            sb.append(ws1);
            fail(sb.toString());
        }
        checkWorkSource("Remove", ws1, result);
    }

    private void doTestRemoveNames(String[] lhsnames, String[] rhsnames,
            String[] resultnames, boolean diff) throws Exception {
        int[] lhs = makeRepeatingIntArray(lhsnames, 0);
        int[] rhs = makeRepeatingIntArray(rhsnames, 0);
        int[] result = makeRepeatingIntArray(resultnames, 0);
        WorkSource ws1 = wsNew(lhs, lhsnames);
        WorkSource ws2 = wsNew(rhs, rhsnames);
        boolean diffres = ws1.remove(ws2);
        if (diffres != diff) {
            StringBuilder sb = new StringBuilder();
            sb.append("Expected diff ");
            sb.append(diff);
            sb.append(" but got ");
            sb.append(diffres);
            sb.append(" when removing ");
            sb.append(ws2);
            sb.append(" from ");
            sb.append(ws1);
            fail(sb.toString());
        }
        checkWorkSource("Remove", ws1, result, resultnames);
    }

    private void doTestRemoveUidsNames(int[] lhs, String[] lhsnames, int[] rhs, String[] rhsnames,
            int[] result, String[] resultnames, boolean diff) throws Exception {
        WorkSource ws1 = wsNew(lhs, lhsnames);
        WorkSource ws2 = wsNew(rhs, rhsnames);
        boolean diffres = ws1.remove(ws2);
        if (diffres != diff) {
            StringBuilder sb = new StringBuilder();
            sb.append("Expected diff ");
            sb.append(diff);
            sb.append(" but got ");
            sb.append(diffres);
            sb.append(" when removing ");
            sb.append(ws2);
            sb.append(" from ");
            sb.append(ws1);
            fail(sb.toString());
        }
        checkWorkSource("Remove", ws1, result, resultnames);
    }

    private void doTestRemove(int[] lhs, int[] rhs, int[] result, boolean diff) throws Exception {
        doTestRemoveUids(lhs, rhs, result, diff);
        doTestRemoveUidsNames(lhs, makeRepeatingStringArray(lhs, "A"),
                rhs, makeRepeatingStringArray(rhs, "A"),
                result, makeRepeatingStringArray(result, "A"),
                diff);
        doTestRemoveNames(makeStringArray(lhs), makeStringArray(rhs),
                makeStringArray(result), diff);
    }

    public void testRemoveNone() throws Exception {
        doTestRemove(
                new int[] { 10, 20, 30, 40 },
                new int[] { 1, 2, 35, 50 },
                new int[] { 10, 20, 30, 40 },
                false);
    }

    public void testRemoveMultiFront() throws Exception {
        doTestRemove(
                new int[] { 10, 20, 30, 40 },
                new int[] { 1, 2, 10, 30 },
                new int[] { 20, 40 },
                true);
    }

    public void testRemoveMultiMiddle() throws Exception {
        doTestRemove(
                new int[] { 10, 20, 30, 40 },
                new int[] { 20, 30 },
                new int[] { 10, 40 },
                true);
    }

    public void testRemoveMultiEnd() throws Exception {
        doTestRemove(
                new int[] { 10, 20, 30, 40 },
                new int[] { 30, 40, 50 },
                new int[] { 10, 20 },
                true);
    }

    public void testRemoveMultiFull() throws Exception {
        doTestRemove(
                new int[] { 10, 20, 30, 40 },
                new int[] { 1, 2, 20, 25, 35, 40 },
                new int[] { 10, 30 },
                true);
    }

    public void testRemoveMultiAll() throws Exception {
        doTestRemove(
                new int[] { 10, 20, 30, 40 },
                new int[] { 10, 20, 30, 40 },
                new int[] { },
                true);
    }

    private void doTestStripNames(int[] uids, String[] names, int[] expected) throws Exception {
        WorkSource ws1 = wsNew(uids, names);
        WorkSource res = wsStripNames(ws1);
        checkWorkSource("StripNames", res, expected);
    }

    public void testStripNamesSimple() throws Exception {
        doTestStripNames(
                new int[]    { 10,  20,  30,  40 },
                new String[] { "A", "A", "A", "A" },
                new int[]    { 10, 20, 30, 40 });
    }

    public void testStripNamesFull() throws Exception {
        doTestStripNames(
                new int[]    { 10,  10,  10,  10 },
                new String[] { "A", "B", "C", "D" },
                new int[]    { 10 });
    }

    public void testStripNamesComplex() throws Exception {
        doTestStripNames(
                new int[]    { 10,  20,  20,  30,  40,  40 },
                new String[] { "A", "A", "B", "A", "A", "B" },
                new int[]    { 10, 20, 30, 40 });
    }
}
