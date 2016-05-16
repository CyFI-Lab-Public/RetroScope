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
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.constant.*;
import proguard.classfile.util.SimplifiedVisitor;

/**
 * This ConstantVisitor marks all classes that invoke package visible members
 * in other classes.
 *
 * @author Eric Lafortune
 */
public class PackageVisibleMemberInvokingClassMarker
extends      SimplifiedVisitor
implements   ConstantVisitor
{
    // Implementations for ConstantVisitor.

    public void visitAnyConstant(Clazz clazz, Constant constant) {}


    public void visitAnyRefConstant(Clazz clazz, RefConstant refConstant)
    {
        Clazz referencedClass = refConstant.referencedClass;
        if (referencedClass != null &&
            (referencedClass.getAccessFlags() &
             ClassConstants.INTERNAL_ACC_PUBLIC) == 0)
        {
            setInvokesPackageVisibleMembers(clazz);
        }

        Member referencedMember = refConstant.referencedMember;
        if (referencedMember != null &&
            (referencedMember.getAccessFlags() &
             (ClassConstants.INTERNAL_ACC_PUBLIC |
              ClassConstants.INTERNAL_ACC_PRIVATE)) == 0)
        {
            setInvokesPackageVisibleMembers(clazz);
        }
    }


    // Small utility methods.

    private static void setInvokesPackageVisibleMembers(Clazz clazz)
    {
        ClassOptimizationInfo info = ClassOptimizationInfo.getClassOptimizationInfo(clazz);
        if (info != null)
        {
            info.setInvokesPackageVisibleMembers();
        }
    }


    public static boolean invokesPackageVisibleMembers(Clazz clazz)
    {
        ClassOptimizationInfo info = ClassOptimizationInfo.getClassOptimizationInfo(clazz);
        return info == null || info.invokesPackageVisibleMembers();
    }
}