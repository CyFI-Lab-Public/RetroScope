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
package proguard.classfile.attribute;

import proguard.classfile.Clazz;
import proguard.classfile.visitor.ClassVisitor;

/**
 * Representation of an Local Variable table entry.
 *
 * @author Eric Lafortune
 */
public class LocalVariableTypeInfo
{
    public int u2startPC;
    public int u2length;
    public int u2nameIndex;
    public int u2signatureIndex;
    public int u2index;

    /**
     * An extra field pointing to the Clazz objects referenced in the
     * type string. This field is typically filled out by the <code>{@link
     * proguard.classfile.util.ClassReferenceInitializer
     * ClassReferenceInitializer}</code>.
     * References to primitive types are ignored.
     */
    public Clazz[] referencedClasses;


    /**
     * Creates an uninitialized LocalVariableTypeInfo.
     */
    public LocalVariableTypeInfo()
    {
    }


    /**
     * Creates an initialized LocalVariableTypeInfo.
     */
    public LocalVariableTypeInfo(int   u2startPC,
                                 int   u2length,
                                 int   u2nameIndex,
                                 int   u2signatureIndex,
                                 int   u2index)
    {
        this.u2startPC        = u2startPC;
        this.u2length         = u2length;
        this.u2nameIndex      = u2nameIndex;
        this.u2signatureIndex = u2signatureIndex;
        this.u2index          = u2index;
    }


    /**
     * Applies the given visitor to all referenced classes.
     */
    public void referencedClassesAccept(ClassVisitor classVisitor)
    {
        if (referencedClasses != null)
        {
            for (int index = 0; index < referencedClasses.length; index++)
            {
                Clazz referencedClass = referencedClasses[index];
                if (referencedClass != null)
                {
                    referencedClass.accept(classVisitor);
                }
            }
        }
    }
}
