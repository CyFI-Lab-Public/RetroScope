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

import com.android.dx.dex.DexOptions;
import com.android.dx.dex.code.ArrayData;
import com.android.dx.dex.code.CodeAddress;
import com.android.dx.dex.code.CstInsn;
import com.android.dx.dex.code.DalvCode;
import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.Dops;
import com.android.dx.dex.code.OddSpacer;
import com.android.dx.dex.code.OutputFinisher;
import com.android.dx.dex.code.PositionList;
import com.android.dx.dex.code.SimpleInsn;
import com.android.dx.dex.code.SwitchData;
import com.android.dx.dex.code.TargetInsn;
import com.android.dx.dex.code.form.Form51l;
import com.android.dx.dex.file.ClassDefItem;
import com.android.dx.dex.file.DexFile;
import com.android.dx.dex.file.EncodedField;
import com.android.dx.dex.file.EncodedMethod;
import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstBoolean;
import com.android.dx.rop.cst.CstByte;
import com.android.dx.rop.cst.CstChar;
import com.android.dx.rop.cst.CstDouble;
import com.android.dx.rop.cst.CstFieldRef;
import com.android.dx.rop.cst.CstFloat;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.rop.cst.CstLong;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstShort;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;
import com.android.dx.util.IntList;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

//TODO: copyright notice

/**
 * This class represents the public API for Dasm. It has two main methods (readD
 * and write) and few utility methods. To compile .d file: -create DAsm instance
 * -call readD() to read and parse content of .d file -call write() to write out
 * binary representation of .d file. .d file can contain several classes and/or
 * intefaces declarations.
 */

public class DAsm {
    private static final boolean PARSER_DEBUG = false;

    // number of errors reported in a file.
    int errors;

    // options for dex output
    DexOptions dexOptions = new DexOptions();

    // file being processed
    DexFile dexFile;
    int line_num;
    Scanner scanner;

    // state info for the class being built
    boolean class_header;
    String class_name;
    int class_acc;
    String superclass_name;
    String source_name;
    String filename;
    Vector<String> interfaces = new Vector<String>();
    ClassDefItem classDef;

    // method being built
    EncodedMethod enc_method;
    CstNat method_nat;
    int method_acc;
    int regs_count;
    OutputFinisher output_finisher;

    /**
     * list of exceptions that method can throw.
     */
    Vector<String> throw_list = new Vector<String>();

    /**
     * Constructor of CatchTable instances from method data.
     */
    DasmCatchBuilder catch_builder;

    /**
     * Holds CodeAddress associated with particular label and <i>planted</i>
     * attribute specifying that CodeAddress was added to instructions array.
     * <i>planted</i> is false if this label is a target of forward branch and
     * it was not added to instructions array yet.
     */
    class LabelTableEntry {
        LabelTableEntry(CodeAddress code_address, boolean planted) {
            this.code_address = code_address;
            this.planted = planted;
        }

        CodeAddress code_address;
        boolean planted;
    }

    /**
     * Hold a translation table "LabelX" -> CodeAddress, planted.
     */
    Hashtable<String, LabelTableEntry> labels_table;

    /**
     * used by relative forward jumps. When relative forward offset is found,
     * CodeAddress is placed into unprocessed_relative_goto_addr. When addInsn
     * method is called, it checks if there was a jump to current instruction
     * and moves CodeAddress from unprocessed_relative_goto_addr into
     * output_finisher.
     */
    int current_insn_number;
    Hashtable<Integer, CodeAddress> unprocessed_relative_goto_addr =
            new Hashtable<Integer, CodeAddress>();

    // fill-array-data data
    int fill_data_reg;
    String fill_array_data_type;
    Vector<Number> fill_array_data_values;

    // packed-switch and sparse-switch data
    int switch_reg;
    Vector<Object> switch_targets;
    IntList switch_keys;
    int packed_switch_first_key;
    int packed_switch_current_key;

    /**
     * holds sparse-switch, packed-switch and fill-array-data data blocks to be
     * added at the end of method
     */
    Vector<DalvInsn> data_blocks = new Vector<DalvInsn>();

    /**
     * Returns the number of warnings/errors encountered while parsing a file. 0
     * if everything went OK.
     */
    public int errorCount() {
        return errors;
    }

    void report_error(String msg) {
        errors++;
        System.out.println("Line " + line_num + ": " + msg);
    }

    void throwDasmError(String msg) throws DasmError {
        throw new DasmError("Line " + line_num + ": " + msg);
    }

    /**
     * used by .line directive
     */
    void addLineInfo(int line_num) throws DasmError {
        throw new IllegalStateException(".line not implemented");
    }

    void addLine(int line_num) throws DasmError {
        throw new IllegalStateException(".line not implemented");
    }

    /**
     * used by the .var directive
     */
    void addVar(String startLab, String endLab, String name, String desc,
            String sign, int var_num) throws DasmError {
        throw new IllegalStateException(".var is not implemented");
    }

    void addVar(int startOffset, int endOffset, String name, String desc,
            String sign, int var_num) throws DasmError {
        throw new IllegalStateException(".var is not implemented");
    }


    /**
     * Used by the parser to tell DAsm what the line number for the next
     * statement is. DAsm's autoNumber mechanism uses this info.
     */
    void setLine(int l) {
        if (PARSER_DEBUG) System.out.println("setLine(" + l + ")");
        line_num = l;
    }

    /**
     * called by the .inner directive
     */
    void addInner(short iacc, String name, String inner, String outer) {
        throw new IllegalStateException(".inner is not implemented");
    }

    /*
     * ========================================================================
     * === FILE HEADER
     * ========================================================================
     */

    /**
     * called by the .source directive
     */
    void setSource(String name) {
        if (PARSER_DEBUG) System.out.println("setSource(" + name + ")");
        source_name = name;
    }

    /**
     * called by the .bytecode directive
     */
    void setVersion(Number version) {
        throw new IllegalStateException(".bytecode is not implemented");
    }

    /**
     * called by the .class directive
     */
    void setClass(String name, int acc) {
        if (PARSER_DEBUG)
            System.out.println("setClass(" + name + ", " + acc + ")");
        class_name = name;
        class_acc = acc;
        class_header = true;
        interfaces.clear();
        superclass_name = null;
    }

    /**
     * Returns the name of the class in the file (i.e. the string given to the
     * .class parameter). If there're several classes in one file, returns name
     * of last class.
     */
    public String getClassName() {
        return class_name;
    }

    /**
     * called by the .super directive
     */
    void setSuperClass(String name) {
        if (PARSER_DEBUG) System.out.println("setSuperClass(" + name + ")");
        superclass_name = name;
    }

    /**
     * called by the .implements directive
     */
    void addInterface(String name) {
        if (PARSER_DEBUG) System.out.println("addInterface(" + name + ")");

        int sz = interfaces.size();
        boolean found = false;
        // search for duplicates
        for (int i = 0; i < sz; i++) {
            String s = interfaces.elementAt(i);
            if (s.compareTo(name) == 0) {
                found = true;
                break;
            }

        }
        if (found == false) interfaces.add(name);
    }

    /**
     * called by the .signature directive
     */
    void setSignature(String str) throws DasmError {
        throw new IllegalStateException(".signature is not implemented");
    }

    /**
     * called by the .enclosing directive
     */
    void setEnclosingMethod(String str) {
        throw new IllegalStateException(".enclosing is not implemented");
    }

    /**
     * called by the .attribute directive
     */
    void addGenericAttr(String name, String file) throws DasmError {
        throw new IllegalStateException(".attribute is not implemented");
    }

    /**
     * called at end of dasm-header (resolve class variables)
     */
    void endHeader() {

        TypeList tl = createTypeListFromStrings(interfaces);

        classDef = new ClassDefItem(CstType.intern(Type
                .internClassName(class_name)), class_acc,
                superclass_name != null ? CstType.intern(Type
                        .internClassName(superclass_name)) : null, tl,
                new CstString(source_name));
        dexFile.add(classDef);
        class_header = false;
    }

    /*
     * ========================================================================
     * === FIELDS
     * ========================================================================
     */

    /**
     * called by the .field directive to begin 'prompted' field
     */
    void beginField(short access, String name, String desc, Object value)
            throws DasmError {
        throw new IllegalStateException(
                "multiline fields are not implemented yet");
    }

    /**
     * called by the .end field directive to end 'prompted' field
     */
    void endField() throws DasmError {
        throw new IllegalStateException(
                "multiline fields are not implemented yet");
    }

    /**
     * called by the .field directive
     */
    void addField(short access, String name, String desc, String sig,
            Object value) throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("addField(" + name + ", " + desc + ", " + sig
                    + ", " + access + ", "
                    + (value == null ? "null" : value.toString()) + ")");

        CstNat nat = new CstNat(new CstString(name), new CstString(desc));
        CstFieldRef field = new CstFieldRef(classDef.getThisClass(), nat);
        EncodedField ef = new EncodedField(field, access);
        if ((access & AccessFlags.ACC_STATIC) != 0) {
            // TODO: value?
            if (value != null)
                throw new IllegalStateException(
                        "addField: field initialization not implemented yet");
            classDef.addStaticField(ef, null);
        } else
            classDef.addInstanceField(ef);
    }


    /*
     * ========================================================================
     * === METHODS
     * ========================================================================
     */

    /**
     * called by the .method directive to start the definition for a method
     */
    void newMethod(String name, String descriptor, int access) {
        if (PARSER_DEBUG)
            System.out.println("newMethod(" + name + ", " + descriptor + ", "
                    + access + ")");

        output_finisher = null;
        throw_list.clear();
        unprocessed_relative_goto_addr.clear();
        labels_table = new Hashtable<String, LabelTableEntry>();
        catch_builder = new DasmCatchBuilder(labels_table);
        current_insn_number = 0;
        regs_count = 1;

        method_nat = new CstNat(new CstString(name), new CstString(descriptor));
        if (method_nat.isClassInit()) access |= AccessFlags.ACC_STATIC;
        if (method_nat.isInstanceInit()) access |= AccessFlags.ACC_CONSTRUCTOR;

        method_acc = access;
    }

    /**
     * called by the .end method directive to end the definition for a method
     */
    void endMethod() throws DasmError {
        if (PARSER_DEBUG) System.out.println("endMethod()");

        // add packed-switch, sparse-switch, fill-array-data data blocks at the
        // end of method
        int sz = data_blocks.size();
        for (int i = 0; i < sz; i++) {
            addInsn(data_blocks.elementAt(i));
        }
        data_blocks.clear();

        // check jump targets
        if (unprocessed_relative_goto_addr.size() != 0) {
            report_error("Relative forward jump offset too big.");
        }
        Enumeration<String> e = labels_table.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            LabelTableEntry lte = labels_table.get(key);
            if (lte.planted == false) {
                report_error("Label " + key + " not found.");
            }
        }

        TypeList tl = createTypeListFromStrings(throw_list);

        CstMethodRef meth = new CstMethodRef(classDef.getThisClass(),
                method_nat);
        DalvCode code = null;
        // output_finisher may be null at this point if method is native
        if (output_finisher != null)
            code = new DalvCode(PositionList.NONE, output_finisher,
                    catch_builder);
        enc_method = new EncodedMethod(meth, method_acc, code, tl);

        if (meth.isInstanceInit() || meth.isClassInit()
                || (method_acc & AccessFlags.ACC_STATIC) != 0
                || (method_acc & AccessFlags.ACC_PRIVATE) != 0) {
            classDef.addDirectMethod(enc_method);
        } else {
            classDef.addVirtualMethod(enc_method);
        }
        catch_builder = null;
        labels_table = null;
    }

    /**
     * used by the .limit regs directive
     */
    void setRegsSize(int v) throws DasmError {
        if (PARSER_DEBUG) System.out.println("setRegsSize(" + v + ")");
        regs_count = v;
    }

    /**
     * used by the .throws directive
     */
    void addThrow(String name) throws DasmError {
        if (PARSER_DEBUG) System.out.println("addThrow(" + name + ")");
        throw_list.add(name);
    }

    /**
     * used by the .catch directive
     */
    void addCatch(String name, String start_lab, String end_lab,
            String branch_lab) throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("addCatch(" + name + ", " + start_lab + ", "
                    + end_lab + ", " + branch_lab + ")");
        catch_builder.add(name, start_lab, end_lab, branch_lab);
    }

    void addCatch(String name, int start_off, int end_off, int branch_off)
            throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("addCatch(" + name + ", " + start_off + ", "
                    + end_off + ", " + branch_off + ")");
        throw new IllegalStateException(
                "addCatch(String, int, int, int) is not implemented yet");
    }


    /**
     * defines a label
     */
    void plantLabel(String name) throws DasmError {
        if (PARSER_DEBUG) System.out.println("plantLabel(" + name + ")");
        createOutputFinisher();
        LabelTableEntry lte = labels_table.get(name);
        if (lte != null) {
            if (lte.planted == true)
                report_error("Label " + name + " already defined");
            else {
                lte.planted = true;
                addInsn(lte.code_address);
            }
        } else {
            CodeAddress code_address = new CodeAddress(createSourcePosition());
            addInsn(code_address);
            labels_table.put(name, new LabelTableEntry(code_address, true));
        }
    }


    /**
     * used for instructions that take no arguments Format: 10x
     */
    void addOpcode(String name) throws DasmError {
        if (PARSER_DEBUG) System.out.println("addOpcode(" + name + ")");
        createOutputFinisher();
        DopInfo insn = DopInfo.get(name);
        if (insn.args.equals("")) {
            DalvInsn dalvInsn = new SimpleInsn(insn.opcode,
                    createSourcePosition(), RegisterSpecList.EMPTY);
            addInsn(dalvInsn);
        } else {
            throwDasmError("Missing arguments for instruction " + name);
        }
    }

    /**
     * used for instructions that take a word as a parameter (register name is
     * treated as word) Format: 11x, 10t, 20t, 30t
     */
    void addOpcode(String name, String val) throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("addOpcode(" + name + ", " + val + ")");
        createOutputFinisher();
        DopInfo insn = DopInfo.get(name);
        if (insn.args.compareToIgnoreCase(DopInfo.ARG_REGISTER) == 0) {
            int reg_num = -1;

            try {
                reg_num = getRegNumberFromString(val);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + val + ")");
            }
            // TODO: is Type.INT suitable for any opcodes? Or it should be
            // Type.OBJECT for return-object, for example?
            RegisterSpec reg_spec = RegisterSpec.make(reg_num, Type.INT);
            DalvInsn dalvInsn = new SimpleInsn(insn.opcode,
                    createSourcePosition(), RegisterSpecList.make(reg_spec));
            addInsn(dalvInsn);
        } else if (insn.args.compareToIgnoreCase(DopInfo.ARG_ADDRESS) == 0) {
            LabelTableEntry lte = labels_table.get(val);
            if (lte == null) {
                CodeAddress code_address = new CodeAddress(
                        SourcePosition.NO_INFO);
                lte = new LabelTableEntry(code_address, false);
                labels_table.put(val, lte);
            }
            DalvInsn dalvInsn = new TargetInsn(insn.opcode,
                    createSourcePosition(), RegisterSpecList.EMPTY,
                    lte.code_address);
            addInsn(dalvInsn);
        } else {
            throwDasmError("Bad arguments for instruction " + name + "(" + val
                    + ")");
        }
    }

    /**
     * used for relative branch targets (ie $+5, $-12, ...) Format: relative
     * 10t, 20t, 30t
     */
    void addRelativeGoto(String name, int val) throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("addRelativeGoto(" + name + ", " + val + ")");
        createOutputFinisher();
        DopInfo insn = DopInfo.get(name);
        if (insn.args.compareToIgnoreCase(DopInfo.ARG_ADDRESS) == 0) {
            if (val == 0)
                throwDasmError("Bad arguments for instruction " + name + "("
                        + val + ")");

            CodeAddress code_address = new CodeAddress(SourcePosition.NO_INFO);
            if (val < 0) {
                output_finisher.insert(current_insn_number + val, code_address);
                current_insn_number++;
            } else {
                unprocessed_relative_goto_addr.put(current_insn_number + val,
                        code_address);
            }
            DalvInsn dalvInsn = new TargetInsn(insn.opcode,
                    createSourcePosition(), RegisterSpecList.EMPTY,
                    code_address);
            addInsn(dalvInsn);

        } else {
            throwDasmError("Bad arguments for instruction " + name + "(" + val
                    + ")");
        }
    }

    /**
     * used for instructions that take two word parameters (register name is
     * treated as word) Format: 12x, 22x, 32x, 21t, 21c (string@, type@), 31c,
     * 35c, 3rc
     */
    void addOpcode(String name, String v1, String v2) throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("addOpcode(" + name + ", " + v1 + ", " + v2
                    + ")");
        createOutputFinisher();
        DopInfo insn = DopInfo.get(name);

        if (insn.args.compareToIgnoreCase(DopInfo.ARG_REG_REG) == 0) {
            int reg1_num = -1, reg2_num = -1;

            try {
                reg1_num = getRegNumberFromString(v1);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");
            }

            try {
                reg2_num = getRegNumberFromString(v2);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v2 + ")");
            }
            // TODO: is Type.INT suitable for any opcodes?
            RegisterSpec reg1_spec = RegisterSpec.make(reg1_num, Type.INT);
            RegisterSpec reg2_spec = RegisterSpec.make(reg2_num, Type.INT);
            DalvInsn dalvInsn = new SimpleInsn(insn.opcode,
                    createSourcePosition(), RegisterSpecList.make(reg1_spec,
                            reg2_spec));
            addInsn(dalvInsn);
        } else if (insn.args.compareToIgnoreCase(
                DopInfo.ARG_REG_ADDRESS) == 0) {
            int reg1_num = -1;

            try {
                reg1_num = getRegNumberFromString(v1);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");
            }

            LabelTableEntry lte = labels_table.get(v2);
            if (lte == null) {
                CodeAddress code_address = new CodeAddress(
                        SourcePosition.NO_INFO);
                lte = new LabelTableEntry(code_address, false);
                labels_table.put(v2, lte);
            }

            RegisterSpec reg1_spec = RegisterSpec.make(reg1_num, Type.INT);
            DalvInsn dalvInsn = new TargetInsn(insn.opcode,
                    createSourcePosition(), RegisterSpecList.make(reg1_spec),
                    lte.code_address);
            addInsn(dalvInsn);
        } else if (insn.args.compareToIgnoreCase(DopInfo.ARG_REG_STRING) == 0) {
            int reg1_num = -1;

            try {
                reg1_num = getRegNumberFromString(v1);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");
            }
            RegisterSpec reg1_spec = RegisterSpec.make(reg1_num, Type.STRING);
            Constant constant = new CstString(v2);
            DalvInsn dalvInsn = new CstInsn(insn.opcode,
                    createSourcePosition(), RegisterSpecList.make(reg1_spec),
                    constant);
            addInsn(dalvInsn);
        } else if (insn.args.compareToIgnoreCase(DopInfo.ARG_REG_TYPE) == 0) {
            int reg1_num = -1;

            try {
                reg1_num = getRegNumberFromString(v1);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");
            }
            Type type;
            try {
                // try to intern it as primitive type first
                type = Type.intern(v2);
            } catch (IllegalArgumentException e) {
                type = Type.internClassName(v2);
            }

            RegisterSpec reg1_spec = RegisterSpec.make(reg1_num, type);
            Constant constant = CstType.intern(type);
            DalvInsn dalvInsn = new CstInsn(insn.opcode,
                    createSourcePosition(), RegisterSpecList.make(reg1_spec),
                    constant);
            addInsn(dalvInsn);
        } else if (insn.args.compareToIgnoreCase(DopInfo.ARG_REGLIST_TYPE) == 0
                || insn.args.compareToIgnoreCase(
                        DopInfo.ARG_REGLIST_METHOD) == 0
                || insn.args.compareToIgnoreCase(
                        DopInfo.ARG_REGLIST_INTFMETHOD) == 0) {
            RegisterSpecList reg_spec_list = RegisterSpecList.EMPTY;
            String regs[] = Utils.splitRegList(v1);
            if (regs != null) {
                int rn = regs.length;
                if (rn == 0 || rn > 5)
                    throwDasmError("Bad arguments for instruction " + name
                            + "(" + v1 + ")");
                int reg_num[] = new int[rn];

                reg_spec_list = new RegisterSpecList(rn);

                for (int i = 0; i < rn; i++) {
                    try {
                        reg_num[i] = getRegNumberFromString(regs[i]);
                    } catch (IllegalArgumentException e) {
                        throwDasmError("Bad arguments for instruction " + name
                                + "(" + v1 + ")");
                    }
                    reg_spec_list.set(i, RegisterSpec
                            .make(reg_num[i], Type.INT));
                }
            }
            Constant constant;
            if (insn.args.compareToIgnoreCase(DopInfo.ARG_REGLIST_TYPE) == 0) {
                // filled-new-array
                Type type;
                try {
                    type = Type.intern(v2);
                } catch (IllegalArgumentException e) {
                    // in case of exception, try to intern type as a class name
                    // (Lclass_name;)
                    type = Type.internClassName(v2);
                }
                constant = CstType.intern(type);
            } else {
                // invoke-kind
                String[] names = Utils.getClassMethodSignatureFromString(v2);
                CstNat method_nat = new CstNat(new CstString(names[1]),
                        new CstString(names[2]));

                /*
                 * if(insn.args.compareToIgnoreCase(
                 *          DopInfo.ARG_REGLIST_INTFMETHOD
                 * ) == 0) constant = new
                 * CstInterfaceMethodRef(CstType.intern(Type
                 * .internClassName(names[0])), method_nat); else
                 */
                constant = new CstMethodRef(CstType.intern(Type
                        .internClassName(names[0])), method_nat);
            }

            DalvInsn dalvInsn = new CstInsn(insn.opcode,
                    createSourcePosition(), reg_spec_list, constant);
            addInsn(dalvInsn);

        } else if (insn.args.compareToIgnoreCase(
                        DopInfo.ARG_REGRANGE_TYPE) == 0
                || insn.args.compareToIgnoreCase(
                        DopInfo.ARG_REGRANGE_METHOD) == 0
                || insn.args.compareToIgnoreCase(
                        DopInfo.ARG_REGRANGE_INTFMETHOD) == 0) {
            String regs[] = Utils.splitRegList(v1);
            RegisterSpecList reg_spec_list;
            if (regs != null && regs.length > 0) {
                int regC = -1, regN = -1;
                try {
                    regC = getRegNumberFromString(regs[0]);
                } catch (IllegalArgumentException e) {
                    throwDasmError("Bad arguments for instruction " + name
                            + "(" + v1 + ")");
                }

                if (regs.length > 1) {
                    try {
                        regN = getRegNumberFromString(regs[1]);
                    } catch (IllegalArgumentException e) {
                        throwDasmError("Bad arguments for instruction " + name
                                + "(" + v1 + ")");
                    }

                    if (regC >= regN)
                        throwDasmError("Bad arguments for instruction " + name
                                + "(" + v1 + ")");
                } else
                    regN = regC;


                int sz = regN - regC + 1;
                reg_spec_list = new RegisterSpecList(sz);

                for (int i = 0; i < sz; i++) {
                    reg_spec_list.set(i, RegisterSpec.make(regC + i, Type.INT));
                }
            } else
                reg_spec_list = RegisterSpecList.EMPTY;

            Constant constant;
            if (insn.args.compareToIgnoreCase(DopInfo.ARG_REGRANGE_TYPE) == 0) {
                // filled-new-array/range
                Type type;
                try {
                    type = Type.intern(v2);
                } catch (IllegalArgumentException e) {
                    // in case of exception, try to intern type as a class name
                    // (Lclass_name;)
                    type = Type.internClassName(v2);
                }
                constant = CstType.intern(type);
            } else {
                // invoke-kind/range
                String[] names = Utils.getClassMethodSignatureFromString(v2);
                CstNat method_nat = new CstNat(new CstString(names[1]),
                        new CstString(names[2]));

                /*
                 * if(insn.args.compareToIgnoreCase(
                 *         DopInfo.ARG_REGRANGE_INTFMETHOD
                 * ) == 0) constant = new
                 * CstInterfaceMethodRef(CstType.intern(Type
                 * .internClassName(names[0])), method_nat); else
                 */
                constant = new CstMethodRef(CstType.intern(Type
                        .internClassName(names[0])), method_nat);
            }

            DalvInsn dalvInsn = new CstInsn(insn.opcode,
                    createSourcePosition(), reg_spec_list, constant);
            addInsn(dalvInsn);

        } else {
            throwDasmError("Bad arguments for instruction " + name + "(" + v1
                    + ", " + v2 + ")");
        }
    }

    /**
     * used for relative branch targets (ie $+5, $-12, ...) Format: relative 21t
     */
    void addRelativeGoto(String name, String v1, int val) throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("addRelativeGoto(" + name + ", " + v1 + ", "
                    + val + ")");
        createOutputFinisher();
        DopInfo insn = DopInfo.get(name);
        if (insn.args.compareToIgnoreCase(DopInfo.ARG_REG_ADDRESS) == 0) {
            if (val == 0)
                throwDasmError("Bad arguments for instruction " + name + "("
                        + val + ")");

            int reg1_num = -1;
            try {
                reg1_num = getRegNumberFromString(v1);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");
            }

            RegisterSpec reg1_spec = RegisterSpec.make(reg1_num, Type.INT);
            RegisterSpecList rsl = RegisterSpecList.make(reg1_spec);
            CodeAddress code_address = new CodeAddress(SourcePosition.NO_INFO);
            if (val < 0) {
                output_finisher.insert(current_insn_number + val, code_address);
                current_insn_number++;
            } else {
                unprocessed_relative_goto_addr.put(current_insn_number + val,
                        code_address);
            }
            DalvInsn dalvInsn = new TargetInsn(insn.opcode,
                    createSourcePosition(), rsl, code_address);
            addInsn(dalvInsn);

        } else {
            throwDasmError("Bad arguments for instruction " + name + "(" + val
                    + ")");
        }
    }

    /**
     * used for instructions that take one word parameter (register name is
     * treated as word) and one literal Format: 11n, 21s, 31i, 21h, 51l
     */
    void addOpcode(String name, String v1, Number v2) throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("addOpcode(" + name + ", " + v1 + ", " + v2
                    + ")");
        createOutputFinisher();
        DopInfo insn = DopInfo.get(name);
        if (insn.args.compareToIgnoreCase(DopInfo.ARG_REG_LITERAL) == 0) {
            int reg1_num = -1;

            try {
                reg1_num = getRegNumberFromString(v1);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");
            }

            RegisterSpec reg1_spec;
            Constant constant;
            // create Constant of type suitable for value specified in
            // instruction
            if (v2 instanceof Long
                    || (v2 instanceof Integer &&
                            insn.opcode.getFormat() == Form51l.THE_ONE)) {
                reg1_spec = RegisterSpec.make(reg1_num, Type.LONG);
                constant = CstLong.make(v2.longValue());
            } else if (v2 instanceof Float
                    && insn.opcode.getFormat() != Form51l.THE_ONE) {
                reg1_spec = RegisterSpec.make(reg1_num, Type.FLOAT);
                constant = CstFloat.make(Float.floatToIntBits(v2.floatValue()));
            } else if (v2 instanceof Double
                    || (v2 instanceof Float &&
                            insn.opcode.getFormat() == Form51l.THE_ONE)) {
                reg1_spec = RegisterSpec.make(reg1_num, Type.DOUBLE);
                constant = CstDouble.make(Double.doubleToLongBits(v2
                        .doubleValue()));
            } else {
                reg1_spec = RegisterSpec.make(reg1_num, Type.INT);
                constant = CstInteger.make(v2.intValue());
            }

            DalvInsn dalvInsn = new CstInsn(insn.opcode,
                    createSourcePosition(), RegisterSpecList.make(reg1_spec),
                    constant);
            addInsn(dalvInsn);
        } else {
            throwDasmError("Bad arguments for instruction " + name + "(" + v1
                    + ", " + v2 + ")");
        }

    }

    /**
     * used for instructions that take three word parameters (register name is
     * treated as word) Format: 23x, 22t, 21c (field@), 22c (type@)
     */
    void addOpcode(String name, String v1, String v2, String v3)
            throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("addOpcode(" + name + ", " + v1 + ", " + v2
                    + ", " + v3 + ")");
        createOutputFinisher();
        DopInfo insn = DopInfo.get(name);

        if (insn.args.compareToIgnoreCase(DopInfo.ARG_REG_REG_REG) == 0) {
            int reg1_num = -1, reg2_num = -1, reg3_num = -1;

            try {
                reg1_num = getRegNumberFromString(v1);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");
            }

            try {
                reg2_num = getRegNumberFromString(v2);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v2 + ")");
            }

            try {
                reg3_num = getRegNumberFromString(v3);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v3 + ")");
            }
            // TODO: is Type.INT suitable for any opcodes?
            RegisterSpec reg1_spec = RegisterSpec.make(reg1_num, Type.INT);
            RegisterSpec reg2_spec = RegisterSpec.make(reg2_num, Type.INT);
            RegisterSpec reg3_spec = RegisterSpec.make(reg3_num, Type.INT);
            DalvInsn dalvInsn = new SimpleInsn(insn.opcode,
                    createSourcePosition(), RegisterSpecList.make(reg1_spec,
                            reg2_spec, reg3_spec));
            addInsn(dalvInsn);
        } else if (insn.args.compareToIgnoreCase(
                DopInfo.ARG_REG_REG_ADDRESS) == 0) {
            int reg1_num = -1, reg2_num = -1;

            try {
                reg1_num = getRegNumberFromString(v1);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");
            }

            try {
                reg2_num = getRegNumberFromString(v2);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v2 + ")");
            }

            LabelTableEntry lte = labels_table.get(v3);
            if (lte == null) {
                CodeAddress code_address = new CodeAddress(
                        SourcePosition.NO_INFO);
                lte = new LabelTableEntry(code_address, false);
                labels_table.put(v3, lte);
            }

            RegisterSpec reg1_spec = RegisterSpec.make(reg1_num, Type.INT);
            RegisterSpec reg2_spec = RegisterSpec.make(reg2_num, Type.INT);

            DalvInsn dalvInsn = new TargetInsn(insn.opcode,
                    createSourcePosition(), RegisterSpecList.make(reg1_spec,
                            reg2_spec), lte.code_address);
            addInsn(dalvInsn);
        } else if (insn.args.compareToIgnoreCase(DopInfo.ARG_REG_FIELD) == 0) {
            int reg1_num = -1;

            try {
                reg1_num = getRegNumberFromString(v1);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");
            }
            // TODO: is Type.INT suitable?
            RegisterSpec reg1_spec = RegisterSpec.make(reg1_num, Type.INT);

            String[] names = Utils.getClassFieldFromString(v2);

            CstNat field_nat = new CstNat(new CstString(names[1]),
                    new CstString(v3));

            Constant constant = new CstFieldRef(CstType.intern(Type
                    .internClassName(names[0])), field_nat);
            DalvInsn dalvInsn = new CstInsn(insn.opcode,
                    createSourcePosition(), RegisterSpecList.make(reg1_spec),
                    constant);
            addInsn(dalvInsn);
        } else if (insn.args.compareToIgnoreCase(
                DopInfo.ARG_REG_REG_TYPE) == 0) {
            int reg1_num = -1, reg2_num = -1;

            try {
                reg1_num = getRegNumberFromString(v1);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");
            }

            try {
                reg2_num = getRegNumberFromString(v2);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v2 + ")");
            }

            Type type = Type.internClassName(v3);
            RegisterSpec reg1_spec = RegisterSpec.make(reg1_num, type);
            RegisterSpec reg2_spec = RegisterSpec.make(reg2_num, type);
            Constant constant = CstType.intern(type);
            DalvInsn dalvInsn = new CstInsn(insn.opcode,
                    createSourcePosition(), RegisterSpecList.make(reg1_spec,
                            reg2_spec), constant);
            addInsn(dalvInsn);
        } else {
            throwDasmError("Bad arguments for instruction " + name + "(" + v1
                    + ", " + v2 + ", " + v3 + ")");
        }
    }

    /**
     * Format: 22c (field@)
     */
    void addOpcode(String name, String v1, String v2, String v3, String v4)
            throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("addOpcode(" + name + ", " + v1 + ", " + v2
                    + ", " + v3 + ", " + v4 + ")");
        createOutputFinisher();
        DopInfo insn = DopInfo.get(name);
        if (insn.args.compareToIgnoreCase(DopInfo.ARG_REG_REG_FIELD) == 0) {
            int reg1_num = -1, reg2_num = -1;

            try {
                reg1_num = getRegNumberFromString(v1);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");
            }
            try {
                reg2_num = getRegNumberFromString(v2);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v2 + ")");
            }
            // TODO: is Type.INT suitable?
            RegisterSpec reg1_spec = RegisterSpec.make(reg1_num, Type.INT);
            RegisterSpec reg2_spec = RegisterSpec.make(reg2_num, Type.INT);

            String[] names = Utils.getClassFieldFromString(v3);

            CstNat field_nat = new CstNat(new CstString(names[1]),
                    new CstString(v4));

            Constant constant = new CstFieldRef(CstType.intern(Type
                    .internClassName(names[0])), field_nat);
            DalvInsn dalvInsn = new CstInsn(insn.opcode,
                    createSourcePosition(), RegisterSpecList.make(reg1_spec,
                            reg2_spec), constant);
            addInsn(dalvInsn);
        } else if (insn.args.compareToIgnoreCase(
                DopInfo.ARG_REGRANGE_TYPE) == 0) {
            String regs[] = Utils.splitRegList(v1);
            if (regs.length != 2)
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");

            int regC = -1, regN = -1;
            try {
                regC = getRegNumberFromString(regs[0]);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");
            }
            try {
                regN = getRegNumberFromString(regs[1]);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");
            }

            if (regC >= regN)
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");

            int sz = regN - regC + 1;
            RegisterSpecList reg_spec_list = new RegisterSpecList(sz);

            for (int i = 0; i < sz; i++) {
                reg_spec_list.set(i, RegisterSpec.make(regC + i, Type.INT));
            }

            Type type;
            try {
                type = Type.intern(v2);
            } catch (IllegalArgumentException e) {
                // in case of exception, try to intern type as a class name
                // (Lclass_name;)
                type = Type.internClassName(v2);
            }
            Constant constant = CstType.intern(type);

            DalvInsn dalvInsn = new CstInsn(insn.opcode,
                    createSourcePosition(), reg_spec_list, constant);
            addInsn(dalvInsn);

        } else {
            throwDasmError("Bad arguments for instruction " + name + "(" + v1
                    + ", " + v2 + ", " + v3 + ", " + v4 + ")");
        }
    }

    /**
     * used for relative branch targets (ie $+5, $-12, ...) Format: relative 22t
     */
    void addRelativeGoto(String name, String v1, String v2, int val)
            throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("addRelativeGoto(" + name + ", " + v1 + ", "
                    + v2 + ", " + val + ")");
        createOutputFinisher();
        DopInfo insn = DopInfo.get(name);
        if (insn.args.compareToIgnoreCase(DopInfo.ARG_REG_REG_ADDRESS) == 0) {
            if (val == 0)
                throwDasmError("Bad arguments for instruction " + name + "("
                        + val + ")");

            int reg1_num = -1, reg2_num = -1;

            try {
                reg1_num = getRegNumberFromString(v1);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");
            }

            try {
                reg2_num = getRegNumberFromString(v2);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v2 + ")");
            }

            RegisterSpec reg1_spec = RegisterSpec.make(reg1_num, Type.INT);
            RegisterSpec reg2_spec = RegisterSpec.make(reg2_num, Type.INT);
            RegisterSpecList rsl = RegisterSpecList.make(reg1_spec, reg2_spec);
            CodeAddress code_address = new CodeAddress(SourcePosition.NO_INFO);
            if (val < 0) {
                output_finisher.insert(current_insn_number + val, code_address);
                current_insn_number++;
            } else {
                unprocessed_relative_goto_addr.put(current_insn_number + val,
                        code_address);
            }
            DalvInsn dalvInsn = new TargetInsn(insn.opcode,
                    createSourcePosition(), rsl, code_address);
            addInsn(dalvInsn);

        } else {
            throwDasmError("Bad arguments for instruction " + name + "(" + val
                    + ")");
        }
    }

    /**
     * used for instructions that take two word parameters (register name is
     * treated as word) and one literal Format: 22b, 22s
     */
    void addOpcode(String name, String v1, String v2, int v3) throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("addOpcode(" + name + ", " + v1 + ", " + v2
                    + ")");
        createOutputFinisher();
        DopInfo insn = DopInfo.get(name);

        if (insn.args.compareToIgnoreCase(DopInfo.ARG_REG_REG_LITERAL) == 0) {
            int reg1_num = -1, reg2_num = -1;

            try {
                reg1_num = getRegNumberFromString(v1);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v1 + ")");
            }

            try {
                reg2_num = getRegNumberFromString(v2);
            } catch (IllegalArgumentException e) {
                throwDasmError("Bad arguments for instruction " + name + "("
                        + v2 + ")");
            }
            // TODO: is Type.INT suitable for any opcodes?
            RegisterSpec reg1_spec = RegisterSpec.make(reg1_num, Type.INT);
            RegisterSpec reg2_spec = RegisterSpec.make(reg2_num, Type.INT);
            Constant constant = CstInteger.make(v3);
            DalvInsn dalvInsn = new CstInsn(insn.opcode,
                    createSourcePosition(), RegisterSpecList.make(reg1_spec,
                            reg2_spec), constant);
            addInsn(dalvInsn);
        } else {
            throwDasmError("Bad arguments for instruction " + name + "(" + v1
                    + ", " + v2 + ", " + v3 + ")");
        }
    }

    /**
     * used for fill-array-data instruction Format: 31t fill-array-data
     * instruction has the syntax: fill-array-data &lt;register&gt; &lt;type&gt;
     * &lt;value1&gt; &lt;value2&gt; .... fill-array-data-end For example:
     * fill-array-data v7 I 1 2 3 4 5 fill-array-data-end
     */
    void newFillArrayData(String reg, String type) throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("newFillArrayData(" + reg + ", " + type + ")");

        try {
            fill_data_reg = getRegNumberFromString(reg);
        } catch (IllegalArgumentException e) {
            throwDasmError("Bad arguments for fill-array-data (" + reg + ")");
        }

        fill_array_data_type = type;
        fill_array_data_values = new Vector<Number>();
    }

    /**
     * add new value to data block
     */
    void addFillArrayData(Number num) throws DasmError {
        if (PARSER_DEBUG) System.out.println("addFillArrayData(" + num + ")");
        fill_array_data_values.add(num);
    }

    /**
     * called by fill-array-data-end
     */
    void endFillArrayData() throws DasmError {
        if (PARSER_DEBUG) System.out.println("endFillArrayData");
        int sz = fill_array_data_values.size();
        ArrayList<Constant> values = new ArrayList<Constant>(sz);
        CstType arrayType = CstType.intern(Type.intern("["
                + fill_array_data_type));
        for (int i = 0; i < sz; i++) {
            Constant constant;
            Number num = fill_array_data_values.elementAt(i);
            if (arrayType == CstType.LONG_ARRAY) {
                constant = CstLong.make(num.longValue());
            } else if (arrayType == CstType.FLOAT_ARRAY) {
                constant = CstFloat
                        .make(Float.floatToIntBits(num.floatValue()));
            } else if (arrayType == CstType.DOUBLE_ARRAY) {
                constant = CstDouble.make(Double.doubleToLongBits(num
                        .doubleValue()));
            } else if (arrayType == CstType.BOOLEAN_ARRAY) {
                constant = CstBoolean.make(num.intValue());
            } else if (arrayType == CstType.BYTE_ARRAY) {
                constant = CstByte.make(num.intValue());
            } else if (arrayType == CstType.CHAR_ARRAY) {
                constant = CstChar.make(num.intValue());
            } else if (arrayType == CstType.SHORT_ARRAY) {
                constant = CstShort.make(num.intValue());
            } else {
                constant = CstInteger.make(num.intValue());
            }
            values.add(constant);
        }

        CodeAddress insn_addr = new CodeAddress(createSourcePosition());
        CodeAddress data_addr = new CodeAddress(SourcePosition.NO_INFO);
        DalvInsn dalvInsn = new TargetInsn(Dops.FILL_ARRAY_DATA,
                createSourcePosition(), RegisterSpecList
                        .make(RegisterSpec.make(fill_data_reg, Type
                                .intern(fill_array_data_type))), data_addr);
        OddSpacer spacer = new OddSpacer(SourcePosition.NO_INFO);
        ArrayData array_data = new ArrayData(SourcePosition.NO_INFO, insn_addr,
                values, arrayType);

        addInsn(insn_addr);
        addInsn(dalvInsn);
        data_blocks.add(spacer);
        data_blocks.add(data_addr);
        data_blocks.add(array_data);

        fill_array_data_values = null;
        fill_array_data_type = null;
    }

    /**
     * used for packed-switch instruction Format: 31t packed-switch instruction
     * has the syntax: packed-switch &lt;register&gt; &lt;lowest&gt;
     * &lt;label1&gt; &lt;label2&gt; .... packed-switch-end For example:
     * packed-switch v3, -1 Label9 Label6 Label6 Label12 Label12
     * packed-switch-end
     */
    void newPackedSwitch(String reg, int first_key) throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("newPackedSwitch(" + reg + ", " + first_key
                    + ")");

        try {
            switch_reg = getRegNumberFromString(reg);
        } catch (IllegalArgumentException e) {
            throwDasmError("Bad arguments for packed-switch (" + reg + ")");
        }

        packed_switch_first_key = first_key;
        packed_switch_current_key = 0;
        switch_targets = new Vector<Object>();
        switch_keys = new IntList();
    }

    /**
     * add new target to packed-switch
     */
    void addPackedSwitchData(String target) throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("addPackedSwitchData(" + target + ")");
        switch_targets.add(target);
        switch_keys.add(packed_switch_first_key + packed_switch_current_key);
        packed_switch_current_key++;
    }

    /**
     * add new target to packed-switch
     */
    void addPackedSwitchData(int target) throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("addPackedSwitchData(" + target + ")");
        switch_targets.add(new Integer(target));
        switch_keys.add(packed_switch_first_key + packed_switch_current_key);
        packed_switch_current_key++;
    }

    /**
     * used for sparse-switch instruction Format: 31t sparse-switch instruction
     * has the syntax: sparse-switch &lt;register&gt; &lt;lowest&gt;
     * &lt;int1&gt; : &lt;label1&gt; &lt;int2&gt; : &lt;label2&gt; ....
     * sparse-switch-end For example: sparse-switch v3 -1 : Label9 10 : Label12
     * 15 : Label12 sparse-switch-end
     */
    void newSparseSwitch(String reg) throws DasmError {
        if (PARSER_DEBUG) System.out.println("newSparseSwitch(" + reg + ")");

        try {
            switch_reg = getRegNumberFromString(reg);
        } catch (IllegalArgumentException e) {
            throwDasmError("Bad arguments for sparse-switch (" + reg + ")");
        }

        switch_targets = new Vector<Object>();
        switch_keys = new IntList();
    }

    /**
     * add new target to sparse-switch
     */
    void addSparseSwitchData(int key, String target) throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("addSparseSwitchData(" + key + ", " + target
                    + ")");
        switch_targets.add(target);
        switch_keys.add(key);
    }

    /**
     * add new target to sparse-switch
     */
    void addSparseSwitchData(int key, int target) throws DasmError {
        if (PARSER_DEBUG)
            System.out.println("addSparseSwitchData(" + key + ", " + target
                    + ")");
        switch_targets.add(new Integer(target));
        switch_keys.add(key);
    }

    /**
     * called by sparse-switch-end or packed-switch-end
     */
    void endSwitch() throws DasmError {
        if (PARSER_DEBUG) System.out.println("endSwitch");
        int sz = switch_targets.size();

        CodeAddress targets[] = new CodeAddress[sz];
        for (int i = 0; i < sz; i++) {
            Object o = switch_targets.elementAt(i);
            CodeAddress addr;
            if (o instanceof String) {
                String t = (String) o;
                LabelTableEntry lte = labels_table.get(t);
                if (lte == null) {
                    CodeAddress code_address = new CodeAddress(
                            SourcePosition.NO_INFO);
                    lte = new LabelTableEntry(code_address, false);
                    labels_table.put(t, lte);
                }
                addr = lte.code_address;
            } else {
                Integer t = (Integer) o;

                addr = new CodeAddress(SourcePosition.NO_INFO);
                if (t < 0) {
                    output_finisher.insert(current_insn_number + t, addr);
                    current_insn_number++;
                } else {
                    unprocessed_relative_goto_addr.put(current_insn_number + t,
                            addr);
                }
            }
            targets[i] = addr;
        }

        CodeAddress insn_addr = new CodeAddress(createSourcePosition());
        CodeAddress data_addr = new CodeAddress(SourcePosition.NO_INFO);
        OddSpacer spacer = new OddSpacer(SourcePosition.NO_INFO);
        SwitchData switch_data = new SwitchData(SourcePosition.NO_INFO,
                insn_addr, switch_keys, targets);
        DalvInsn dalvInsn = new TargetInsn(switch_data.isPacked()
                ? Dops.PACKED_SWITCH : Dops.SPARSE_SWITCH,
                createSourcePosition(), RegisterSpecList.make(RegisterSpec
                        .make(switch_reg, Type.INT)), data_addr);

        addInsn(insn_addr);
        addInsn(dalvInsn);
        data_blocks.add(spacer);
        data_blocks.add(data_addr);
        data_blocks.add(switch_data);

        switch_targets = null;
        switch_keys = null;
    }

    /*
     * ========================================================================
     * === UTILITY METHODS
     * ========================================================================
     */

    /**
     * Creates instance of SourcePosition for current line
     */
    protected SourcePosition createSourcePosition() {
        return new SourcePosition(new CstString(filename), -1, line_num);
    }

    /**
     * Creates TypeList from list of types
     */
    protected TypeList createTypeListFromStrings(Vector<String> strings) {
        StdTypeList tl;

        if (strings.size() == 0)
            tl = StdTypeList.EMPTY;
        else {
            int sz = strings.size();
            tl = new StdTypeList(sz);
            for (int i = 0; i < sz; i++) {
                tl.set(i, Type.internClassName(strings.elementAt(i)));
            }
        }
        return tl;
    }

    /**
     * Creates processor of instruction list.
     */
    private void createOutputFinisher() {
        if (output_finisher == null)
            output_finisher = new OutputFinisher(dexOptions, 5, regs_count);
    }

    /**
     * Returns register number from "vX" string.
     */
    private int getRegNumberFromString(String val)
            throws IllegalArgumentException {
        int reg_num;
        int l = RegisterSpec.PREFIX.length();
        if (val.length() <= l
                || val.substring(0, l).compareToIgnoreCase(
                        RegisterSpec.PREFIX) != 0)
            throw new IllegalArgumentException("Wrong register name prefix");

        try {
            reg_num = Integer.parseInt(val.substring(l));
        } catch (Exception e) {
            throw new IllegalArgumentException("Wrong register name");
        }
        return reg_num;
    }

    /**
     * Adds new instruction to instruction list.
     */
    private void addInsn(DalvInsn insn) {
        createOutputFinisher();
        CodeAddress code_address = unprocessed_relative_goto_addr
                .get(current_insn_number);
        if (code_address != null) {
            output_finisher.add(code_address);
            unprocessed_relative_goto_addr.remove(current_insn_number);
            current_insn_number++;
        }
        output_finisher.add(insn);
        current_insn_number++;
    }

    /*
     * ========================================================================
     * === READER and WRITER
     * ========================================================================
     */

    /**
     * Writes the binary data for the class represented by this ClassFile object
     * to the specified output stream, using the Java Class File format. Throws
     * either an IOException or a dasmError if something goes wrong.
     */
    public void write(OutputStream outp, FileWriter human_readable)
            throws IOException, DasmError {
        dexFile.writeTo(outp, human_readable, true);
    }

    /**
     * Parses a .d file, converting it internally into a binary representation.
     * If something goes wrong, this throws one of an IOException, or a
     * dasmError, or one of a few other exceptions.
     *
     * @param input
     *            is the stream containing the Dalvik assembly code for the
     *            class.
     * @param name
     *            is the name of the stream. This name will be concatenated to
     *            error messages printed to System.err.
     * @param numberLines
     *            true if you want DAsm to generate line numbers automatically,
     *            based on the assembly source, or false if you are using the
     *            ".line" directive and don't want DAsm to help out.
     */
    public void readD(Reader input, String name, boolean numberLines)
            throws IOException, Exception {

        // TODO: numberLines?
        errors = 0;
        filename = name;
        source_name = name;
        class_header = false;
        classDef = null;
        dexFile = new DexFile(dexOptions);

        scanner = new Scanner(input);
        parser parse_obj = new parser(this, scanner);


        if (PARSER_DEBUG) {
            // for debugging
            parse_obj.debug_parse();
        } else {
            parse_obj.parse();
        }

    }
}
