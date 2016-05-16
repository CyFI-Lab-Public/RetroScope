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

package dasm;

import com.android.dx.dex.code.CatchBuilder;
import com.android.dx.dex.code.CatchHandlerList;
import com.android.dx.dex.code.CatchTable;
import com.android.dx.dex.code.CodeAddress;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Type;

import dasm.DAsm.LabelTableEntry;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Constructor of (@link CatchTable) instances from table of labels and list of
 * catch blocks defined in method.
 */
public class DasmCatchBuilder implements CatchBuilder {

    /**
     * Represents catch block that was not processed yet. Holds "from" and "to"
     * labels as well as list of exceptions to catch.
     */
    private class UnprocessedCatch {

        String from;
        String to;
        Hashtable<CstType, String> type_branch = 
                new Hashtable<CstType, String>();

        /**
         * Constructs an instance.
         * 
         * @param exception
         *            exception type
         * @param from
         *            "from" label
         * @param to
         *            "to" label
         * @param branch
         *            "with" label
         */
        UnprocessedCatch(String exception, String from, String to,
                String branch) {
            this.from = from;
            this.to = to;
            add(exception, branch);
        }

        /**
         * Adds new exception type and branch label to current "from-to" block
         * to allow to have code like try { // do something } catch(Exception1
         * e1) { } catch(Exception2 e2) { } or in Dasm: Label1: ; do something
         * Labe2: ; .... Label3: ; .... Label4: ; .... .catch
         * java/lang/Exception from Label1 to Label2 using Label3 .catch
         * java/lang/Throwable from Label1 to Label2 using Label4
         * 
         * @param exception
         *            exception type
         * @param branch
         *            "with" label
         */
        void add(String exception, String branch) {
            CstType type;
            if (exception.compareToIgnoreCase("all") == 0)
                type = CstType.OBJECT;
            else
                type = CstType.intern(Type.internClassName(exception));

            String s = type_branch.get(type);
            if (s != null && s.compareToIgnoreCase(branch) != 0)
                throw new RuntimeException(
                        "Bad .catch directive: same exception (" + exception
                                + ") but different branch addresses (" + s
                                + " and " + branch + ")");
            type_branch.put(type, branch);
        }
    }

    private Vector<UnprocessedCatch> unprocessed_catches = 
            new Vector<UnprocessedCatch>();
    
    private Hashtable<String, LabelTableEntry> labels_table;

    /**
     * Constructs an instance.
     * 
     * @param labels_table
     *            holds list of labels defined in method being processed
     */
    public DasmCatchBuilder(Hashtable<String, LabelTableEntry> labels_table) {
        this.labels_table = labels_table;
    }

    /**
     * Gets the set of catch types associated with this instance.
     */
    public HashSet<Type> getCatchTypes() {
        int sz = unprocessed_catches.size();
        HashSet<Type> result = new HashSet<Type>(sz);
        for (int i = 0; i < sz; i++) {
            Enumeration<CstType> keys = unprocessed_catches.elementAt(i)
                    .type_branch.keys();
            while (keys.hasMoreElements()) {
                result.add(keys.nextElement().getClassType());
            }
        }
        return result;
    }

    /**
     * Gets whether this instance has any catches at all.
     */
    public boolean hasAnyCatches() {
        return unprocessed_catches.size() != 0;
    }

    /**
     * Adds new exception handler
     * 
     * @param exception
     *            type of exception to catch
     * @param start
     *            "from" label
     * @param end
     *            "to" label
     * @param branch
     *            "with" label
     */
    public void add(String exception, String start, String end, String branch) {
        int sz = unprocessed_catches.size();
        for (int i = 0; i < sz; i++) {
            UnprocessedCatch uc = unprocessed_catches.elementAt(i);
            if (uc.from.compareToIgnoreCase(start) == 0) {
                if (uc.to.compareToIgnoreCase(end) != 0)
                    throw new RuntimeException(
                            "Bad .catch directive: two blocks have the same "
                                    + "start address ("
                                    + uc.from
                                    + ") and different end addresses (" + uc.to
                                    + " and " + end + ")");
                uc.add(exception, branch);
                return;
            }
        }

        unprocessed_catches.add(new UnprocessedCatch(exception, start, end,
                branch));
    }

    /**
     * Builds and returns the catch table for this instance.
     */
    public CatchTable build() {
        int sz = unprocessed_catches.size();
        CatchTable result = new CatchTable(sz);
        for (int i = 0; i < sz; i++) {
            UnprocessedCatch uc = unprocessed_catches.elementAt(i);
            LabelTableEntry lte = labels_table.get(uc.from);
            // get "from" address
            if (lte == null || lte.planted == false)
                throw new RuntimeException("Label " + uc.from + " not defined");
            CodeAddress from = lte.code_address;
            // get "to" address
            lte = labels_table.get(uc.to);
            if (lte == null || lte.planted == false)
                throw new RuntimeException("Label " + uc.to + " not defined");
            CodeAddress to = lte.code_address;

            // build handlers list
            CatchHandlerList chl = new CatchHandlerList(uc.type_branch.size());
            Enumeration<CstType> keys = uc.type_branch.keys();
            int j = 0;
            CatchHandlerList.Entry catch_all = null;
            while (keys.hasMoreElements()) {
                CstType type = keys.nextElement();
                String branch = uc.type_branch.get(type);
                lte = labels_table.get(branch);
                if (lte == null || lte.planted == false)
                    throw new RuntimeException("Label " + branch
                            + " not defined");
                CatchHandlerList.Entry chle = new CatchHandlerList.Entry(type,
                        lte.code_address.getAddress());
                // catch_all shall be the last handler in the list
                if (type.equals(CstType.OBJECT)) {
                    catch_all = chle;
                } else {
                    chl.set(j, chle);
                    j++;
                }
            }
            if (catch_all != null) chl.set(j, catch_all);
            chl.setImmutable();

            CatchTable.Entry entry = new CatchTable.Entry(from.getAddress(), to
                    .getAddress(), chl);
            result.set(i, entry);
        }
        return result;
    }
}
