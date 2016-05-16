/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2009 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.optimize.info;

import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.constant.RefConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.SimplifiedVisitor;
import proguard.classfile.visitor.MemberVisitor;

/**
 * This class can tell whether an instruction might throw exceptions.
 *
 * @author Eric Lafortune
 */
public class ExceptionInstructionChecker
extends      SimplifiedVisitor
implements   InstructionVisitor
//             ConstantVisitor,
//             MemberVisitor
{
    // A return value for the visitor methods.
    private boolean mayThrowExceptions;


    /**
     * Returns whether the given instruction may throw exceptions.
     */
    public boolean mayThrowExceptions(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction)
    {
        mayThrowExceptions = false;

        instruction.accept(clazz, method,  codeAttribute, offset, this);

        return mayThrowExceptions;
    }


    // Implementations for InstructionVisitor.

    public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction) {}


    public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction)
    {
        byte opcode = simpleInstruction.opcode;

        // Check for instructions that may throw exceptions.
        if (opcode == InstructionConstants.OP_IDIV         ||
            opcode == InstructionConstants.OP_LDIV         ||
            opcode == InstructionConstants.OP_IREM         ||
            opcode == InstructionConstants.OP_LREM         ||
            opcode == InstructionConstants.OP_IALOAD       ||
            opcode == InstructionConstants.OP_LALOAD       ||
            opcode == InstructionConstants.OP_FALOAD       ||
            opcode == InstructionConstants.OP_DALOAD       ||
            opcode == InstructionConstants.OP_AALOAD       ||
            opcode == InstructionConstants.OP_BALOAD       ||
            opcode == InstructionConstants.OP_CALOAD       ||
            opcode == InstructionConstants.OP_SALOAD       ||
            opcode == InstructionConstants.OP_IASTORE      ||
            opcode == InstructionConstants.OP_LASTORE      ||
            opcode == InstructionConstants.OP_FASTORE      ||
            opcode == InstructionConstants.OP_DASTORE      ||
            opcode == InstructionConstants.OP_AASTORE      ||
            opcode == InstructionConstants.OP_BASTORE      ||
            opcode == InstructionConstants.OP_CASTORE      ||
            opcode == InstructionConstants.OP_SASTORE      ||
            opcode == InstructionConstants.OP_NEWARRAY     ||
            opcode == InstructionConstants.OP_ARRAYLENGTH  ||
            opcode == InstructionConstants.OP_ATHROW       ||
            opcode == InstructionConstants.OP_MONITORENTER ||
            opcode == InstructionConstants.OP_MONITOREXIT)
        {
            // These instructions may throw exceptions.
            mayThrowExceptions = true;
        }

    }


    public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
    {
        byte opcode = constantInstruction.opcode;

        // Check for instructions that may throw exceptions.
        if (opcode == InstructionConstants.OP_GETSTATIC       ||
            opcode == InstructionConstants.OP_PUTSTATIC       ||
            opcode == InstructionConstants.OP_GETFIELD        ||
            opcode == InstructionConstants.OP_PUTFIELD        ||
            opcode == InstructionConstants.OP_INVOKEVIRTUAL   ||
            opcode == InstructionConstants.OP_INVOKESPECIAL   ||
            opcode == InstructionConstants.OP_INVOKESTATIC    ||
            opcode == InstructionConstants.OP_INVOKEINTERFACE ||
            opcode == InstructionConstants.OP_NEW             ||
            opcode == InstructionConstants.OP_ANEWARRAY       ||
            opcode == InstructionConstants.OP_CHECKCAST       ||
            opcode == InstructionConstants.OP_MULTIANEWARRAY)
        {
            // These instructions may throw exceptions.
            mayThrowExceptions = true;
        }
//        else
//        if (opcode == InstructionConstants.OP_INVOKEVIRTUAL   ||
//            opcode == InstructionConstants.OP_INVOKESPECIAL   ||
//            opcode == InstructionConstants.OP_INVOKESTATIC    ||
//            opcode == InstructionConstants.OP_INVOKEINTERFACE)
//        {
//            // Check if the invoking the method may throw an exception.
//            clazz.constantPoolEntryAccept(constantInstruction.constantIndex, this);
//        }
    }


//    // Implementations for ConstantVisitor.
//
//    public void visitAnyMethodrefConstant(Clazz clazz, RefConstant refConstant)
//    {
//        Member referencedMember = refConstant.referencedMember;
//
//        // Do we have a reference to the method?
//        if (referencedMember == null)
//        {
//            // We'll have to assume invoking the unknown method may throw an
//            // an exception.
//            mayThrowExceptions = true;
//        }
//        else
//        {
//            // First check the referenced method itself.
//            refConstant.referencedMemberAccept(this);
//
//            // If the result isn't conclusive, check down the hierarchy.
//            if (!mayThrowExceptions)
//            {
//                Clazz  referencedClass  = refConstant.referencedClass;
//                Method referencedMethod = (Method)referencedMember;
//
//                // Check all other implementations of the method in the class
//                // hierarchy.
//                referencedClass.methodImplementationsAccept(referencedMethod,
//                                                            false,
//                                                            false,
//                                                            true,
//                                                            true,
//                                                            this);
//            }
//        }
//    }
//
//
//    // Implementations for MemberVisitor.
//
//    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
//    {
//        mayThrowExceptions = mayThrowExceptions ||
//                             ExceptionMethodMarker.mayThrowExceptions(programMethod);
//    }
//
//
//    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
//    {
//        mayThrowExceptions = mayThrowExceptions ||
//                             !NoExceptionMethodMarker.doesntThrowExceptions(libraryMethod);
//    }
}
