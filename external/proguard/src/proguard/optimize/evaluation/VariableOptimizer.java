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
package proguard.optimize.evaluation;

import proguard.classfile.*;
import proguard.classfile.visitor.MemberVisitor;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.editor.VariableRemapper;
import proguard.classfile.util.*;

/**
 * This AttributeVisitor optimizes variable allocation based on their the liveness,
 * in the code attributes that it visits.
 *
 * @author Eric Lafortune
 */
public class VariableOptimizer
extends      SimplifiedVisitor
implements   AttributeVisitor
{
    //*
    private static final boolean DEBUG = false;
    /*/
    private static       boolean DEBUG = true;
    //*/

    private static final int MAX_VARIABLES_SIZE = 64;


    private final boolean       reuseThis;
    private final MemberVisitor extraVariableMemberVisitor;

    private final LivenessAnalyzer livenessAnalyzer = new LivenessAnalyzer();
    private final VariableRemapper variableRemapper = new VariableRemapper();

    private int[] variableMap = new int[ClassConstants.TYPICAL_VARIABLES_SIZE];


    /**
     * Creates a new VariableOptimizer.
     * @param reuseThis specifies whether the 'this' variable can be reused.
     *                  Many JVMs for JME and IBM's JVMs for JSE can't handle
     *                  such reuse.
     */
    public VariableOptimizer(boolean reuseThis)
    {
        this(reuseThis, null);
    }


    /**
     * Creates a new VariableOptimizer with an extra visitor.
     * @param reuseThis                  specifies whether the 'this' variable
     *                                   can be reused. Many JVMs for JME and
     *                                   IBM's JVMs for JSE can't handle such
     *                                   reuse.
     * @param extraVariableMemberVisitor an optional extra visitor for all
     *                                   removed variables.
     */
    public VariableOptimizer(boolean       reuseThis,
                             MemberVisitor extraVariableMemberVisitor)
    {
        this.reuseThis                  = reuseThis;
        this.extraVariableMemberVisitor = extraVariableMemberVisitor;
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
//        DEBUG =
//            clazz.getName().equals("abc/Def") &&
//            method.getName(clazz).equals("abc");

        // Initialize the global arrays.
        initializeArrays(codeAttribute);

        // Analyze the liveness of the variables in the code.
        livenessAnalyzer.visitCodeAttribute(clazz, method, codeAttribute);

        int startIndex =
            (method.getAccessFlags() & ClassConstants.INTERNAL_ACC_STATIC) != 0 ||
            reuseThis ? 0 : 1;

        int parameterSize =
            ClassUtil.internalMethodParameterSize(method.getDescriptor(clazz),
                                                  method.getAccessFlags());

        int variableSize = codeAttribute.u2maxLocals;
        int codeLength   = codeAttribute.u4codeLength;

        boolean remapping = false;

        // Loop over all variables.
        for (int oldIndex = 0; oldIndex < variableSize; oldIndex++)
        {
            // By default, the variable will be mapped onto itself.
            variableMap[oldIndex] = oldIndex;

            // Only try remapping the variable if it's not a parameter.
            if (oldIndex >= parameterSize &&
                oldIndex < MAX_VARIABLES_SIZE)
            {
                // Try to remap the variable to a variable with a smaller index.
                for (int newIndex = startIndex; newIndex < oldIndex; newIndex++)
                {
                    if (areNonOverlapping(oldIndex, newIndex, codeLength))
                    {
                        variableMap[oldIndex] = newIndex;

                        updateLiveness(oldIndex, newIndex, codeLength);

                        remapping = true;

                        // This variable has been remapped. Go to the next one.
                        break;
                    }
                }
            }
        }

        // Remap the variables.
        if (remapping)
        {
            if (DEBUG)
            {
                System.out.println("Remapping variables:");
                System.out.println("  Class "+ ClassUtil.externalClassName(clazz.getName()));
                System.out.println("  Method "+ClassUtil.externalFullMethodDescription(clazz.getName(),
                                                                                       0,
                                                                                       method.getName(clazz),
                                                                                       method.getDescriptor(clazz)));
                for (int index = 0; index < variableSize; index++)
                {
                    System.out.println("  ["+index+"] -> ["+variableMap[index]+"]");
                }
            }

            variableRemapper.setVariableMap(variableMap);
            variableRemapper.visitCodeAttribute(clazz, method, codeAttribute);

            // Visit the method, if required.
            if (extraVariableMemberVisitor != null)
            {
                method.accept(clazz, extraVariableMemberVisitor);
            }
        }
    }


    // Small utility methods.

    /**
     * Initializes the global arrays.
     */
    private void initializeArrays(CodeAttribute codeAttribute)
    {
        int codeLength = codeAttribute.u4codeLength;

        // Create new arrays for storing information at each instruction offset.
        if (variableMap.length < codeLength)
        {
            variableMap = new int[codeLength];
        }
    }


    /**
     * Returns whether the given variables are never alive at the same time.
     */
    private boolean areNonOverlapping(int variableIndex1,
                                      int variableIndex2,
                                      int codeLength)
    {
        // Loop over all instructions.
        for (int offset = 0; offset < codeLength; offset++)
        {
            if ((livenessAnalyzer.isAliveBefore(offset, variableIndex1) &&
                 livenessAnalyzer.isAliveBefore(offset, variableIndex2)) ||

                (livenessAnalyzer.isAliveAfter(offset, variableIndex1) &&
                 livenessAnalyzer.isAliveAfter(offset, variableIndex2)) ||

                // For now, exclude Category 2 variables.
                livenessAnalyzer.isCategory2(offset, variableIndex1))
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Updates the liveness resulting from mapping the given old variable on
     * the given new variable.
     */
    private void updateLiveness(int oldVariableIndex,
                                int newVariableIndex,
                                int codeLength)
    {
        // Loop over all instructions.
        for (int offset = 0; offset < codeLength; offset++)
        {
            // Update the liveness before the instruction.
            if (livenessAnalyzer.isAliveBefore(offset, oldVariableIndex))
            {
                livenessAnalyzer.setAliveBefore(offset, oldVariableIndex, false);
                livenessAnalyzer.setAliveBefore(offset, newVariableIndex, true);
            }

            // Update the liveness after the instruction.
            if (livenessAnalyzer.isAliveAfter(offset, oldVariableIndex))
            {
                livenessAnalyzer.setAliveAfter(offset, oldVariableIndex, false);
                livenessAnalyzer.setAliveAfter(offset, newVariableIndex, true);
            }
        }
    }
}
