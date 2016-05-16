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
package proguard.classfile.editor;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.SimplifiedVisitor;

/**
 * This AttributeVisitor remaps variable indexes in all attributes that it
 * visits, based on a given index map.
 *
 * @author Eric Lafortune
 */
public class VariableRemapper
extends      SimplifiedVisitor
implements   AttributeVisitor,
             InstructionVisitor,
             LocalVariableInfoVisitor,
             LocalVariableTypeInfoVisitor
{
    private final CodeAttributeEditor codeAttributeEditor = new CodeAttributeEditor();

    private int[] variableMap;


    /**
     * Sets the given mapping of old variable indexes to their new indexes.
     * Variables that should disappear can be mapped to -1.
     */
    public void setVariableMap(int[] variableMap)
    {
        this.variableMap = variableMap;
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // Initially, the code attribute editor doesn't contain any changes.
        codeAttributeEditor.reset(codeAttribute.u4codeLength);

        // Remap the variables of the instructions.
        codeAttribute.instructionsAccept(clazz, method, this);

        // Apply the code atribute editor.
        codeAttributeEditor.visitCodeAttribute(clazz, method, codeAttribute);

        // Remap the variables of the attributes.
        codeAttribute.attributesAccept(clazz, method, this);
    }


    public void visitLocalVariableTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTableAttribute localVariableTableAttribute)
    {
        // Remap the variable references of the local variables.
        localVariableTableAttribute.localVariablesAccept(clazz, method, codeAttribute, this);

        // Remove local variables that haven't been mapped.
        localVariableTableAttribute.u2localVariableTableLength =
            removeEmptyLocalVariables(localVariableTableAttribute.localVariableTable,
                                      localVariableTableAttribute.u2localVariableTableLength);
    }


    public void visitLocalVariableTypeTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeTableAttribute localVariableTypeTableAttribute)
    {
        // Remap the variable references of the local variables.
        localVariableTypeTableAttribute.localVariablesAccept(clazz, method, codeAttribute, this);

        // Remove local variables that haven't been mapped.
        localVariableTypeTableAttribute.u2localVariableTypeTableLength =
            removeEmptyLocalVariableTypes(localVariableTypeTableAttribute.localVariableTypeTable,
                                          localVariableTypeTableAttribute.u2localVariableTypeTableLength);
    }


    // Implementations for LocalVariableInfoVisitor.

    public void visitLocalVariableInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableInfo localVariableInfo)
    {
        localVariableInfo.u2index =
            remapVariable(localVariableInfo.u2index);
    }


    // Implementations for LocalVariableTypeInfoVisitor.

    public void visitLocalVariableTypeInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeInfo localVariableTypeInfo)
    {
        localVariableTypeInfo.u2index =
            remapVariable(localVariableTypeInfo.u2index);
    }


    // Implementations for InstructionVisitor.

    public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction) {}


    public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction)
    {
        // Is the new variable index different from the original one?
        int oldVariableIndex = variableInstruction.variableIndex;
        int newVariableIndex = remapVariable(oldVariableIndex);
        if (newVariableIndex != oldVariableIndex)
        {
            // Replace the instruction.
            Instruction replacementInstruction =
                new VariableInstruction(variableInstruction.opcode,
                                        newVariableIndex,
                                        variableInstruction.constant).shrink();

            codeAttributeEditor.replaceInstruction(offset, replacementInstruction);
        }
    }


    // Small utility methods.

    /**
     * Returns the new variable index of the given variable.
     */
    private int remapVariable(int variableIndex)
    {
        return variableMap[variableIndex];
    }


    /**
     * Returns the given list of local variables, without the ones that have
     * been removed.
     */
    private int removeEmptyLocalVariables(LocalVariableInfo[] localVariableInfos,
                                          int                 localVariableInfoCount)
    {
        // Overwrite all empty local variable entries.
        int newIndex = 0;
        for (int index = 0; index < localVariableInfoCount; index++)
        {
            LocalVariableInfo localVariableInfo = localVariableInfos[index];
            if (localVariableInfo.u2index >= 0)
            {
                localVariableInfos[newIndex++] = localVariableInfo;
            }
        }

        return newIndex;
    }


    /**
     * Returns the given list of local variable types, without the ones that
     * have been removed.
     */
    private int removeEmptyLocalVariableTypes(LocalVariableTypeInfo[] localVariableTypeInfos,
                                              int                     localVariableTypeInfoCount)
    {
        // Overwrite all empty local variable type entries.
        int newIndex = 0;
        for (int index = 0; index < localVariableTypeInfoCount; index++)
        {
            LocalVariableTypeInfo localVariableTypeInfo = localVariableTypeInfos[index];
            if (localVariableTypeInfo.u2index >= 0)
            {
                localVariableTypeInfos[newIndex++] = localVariableTypeInfo;
            }
        }

        return newIndex;
    }
}
