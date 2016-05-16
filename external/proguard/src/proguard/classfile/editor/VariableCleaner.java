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
import proguard.optimize.info.VariableUsageMarker;

/**
 * This AttributeVisitor cleans up unused variables in all attributes that it
 * visits.
 *
 * @author Eric Lafortune
 */
public class VariableCleaner
extends      SimplifiedVisitor
implements   AttributeVisitor
{
    private final VariableUsageMarker variableUsageMarker = new VariableUsageMarker();


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // Figure out the local variables that are used by the code.
        variableUsageMarker.visitCodeAttribute(clazz, method, codeAttribute);

        // Clean up the variables of the attributes.
        codeAttribute.attributesAccept(clazz, method, this);
    }


    public void visitLocalVariableTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTableAttribute localVariableTableAttribute)
    {
        // Clean up local variables that aren't used.
        localVariableTableAttribute.u2localVariableTableLength =
            removeEmptyLocalVariables(localVariableTableAttribute.localVariableTable,
                                      localVariableTableAttribute.u2localVariableTableLength,
                                      codeAttribute.u2maxLocals);
    }


    public void visitLocalVariableTypeTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeTableAttribute localVariableTypeTableAttribute)
    {
        // Clean up local variables that aren't used.
        localVariableTypeTableAttribute.u2localVariableTypeTableLength =
            removeEmptyLocalVariableTypes(localVariableTypeTableAttribute.localVariableTypeTable,
                                          localVariableTypeTableAttribute.u2localVariableTypeTableLength,
                                          codeAttribute.u2maxLocals);
    }


    // Small utility methods.

    /**
     * Returns the given list of local variables, without the ones that aren't
     * used
     */
    private int removeEmptyLocalVariables(LocalVariableInfo[] localVariableInfos,
                                          int                 localVariableInfoCount,
                                          int                 maxLocals)
    {
        // Overwrite all empty local variable entries.
        int newIndex = 0;
        for (int index = 0; index < localVariableInfoCount && index < maxLocals; index++)
        {
            if (variableUsageMarker.isVariableUsed(index))
            {
                localVariableInfos[newIndex++] = localVariableInfos[index];
            }
        }

        // Clean up any remaining array elements.
        for (int index = newIndex; index < localVariableInfoCount; index++)
        {
            localVariableInfos[index] = null;
        }

        return newIndex;
    }


    /**
     * Returns the given list of local variable types, without the ones that
     * aren't used
     */
    private int removeEmptyLocalVariableTypes(LocalVariableTypeInfo[] localVariableTypeInfos,
                                              int                     localVariableTypeInfoCount,
                                              int                     maxLocals)
    {
        // Overwrite all empty local variable type entries.
        int newIndex = 0;
        for (int index = 0; index < localVariableTypeInfoCount && index < maxLocals; index++)
        {
            if (variableUsageMarker.isVariableUsed(index))
            {
                localVariableTypeInfos[newIndex++] = localVariableTypeInfos[index];
            }
        }

        // Clean up any remaining array elements.
        for (int index = newIndex; index < localVariableTypeInfoCount; index++)
        {
            localVariableTypeInfos[index] = null;
        }

        return newIndex;
    }
}