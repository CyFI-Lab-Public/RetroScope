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
import proguard.classfile.util.MethodLinker;
import proguard.evaluation.value.*;

/**
 * This class stores some optimization information that can be attached to
 * a field.
 *
 * @author Eric Lafortune
 */
public class FieldOptimizationInfo
{
    private static final SpecificValueFactory VALUE_FACTORY = new SpecificValueFactory();

    private boolean        isWritten;
    private boolean        isRead;
    private boolean        canBeMadePrivate = true;
    private ReferenceValue referencedClass;
    private Value          value;


    public FieldOptimizationInfo(Clazz clazz, Field field)
    {
        isWritten =
        isRead    = (field.getAccessFlags() & ClassConstants.INTERNAL_ACC_VOLATILE) != 0;
        value     = initialValue(field.getDescriptor(clazz));
    }


    public void setWritten()
    {
        isWritten = true;
    }


    public boolean isWritten()
    {
        return isWritten;
    }


    public void setRead()
    {
        isRead = true;
    }


    public boolean isRead()
    {
        return isRead;
    }


    public void setCanNotBeMadePrivate()
    {
        canBeMadePrivate = false;
    }


    public boolean canBeMadePrivate()
    {
        return canBeMadePrivate;
    }


    public void generalizeReferencedClass(ReferenceValue referencedClass)
    {
        this.referencedClass = this.referencedClass != null ?
            this.referencedClass.generalize(referencedClass) :
            referencedClass;
    }


    public ReferenceValue getReferencedClass()
    {
        return referencedClass;
    }


    public void generalizeValue(Value value)
    {
        this.value = this.value != null ?
            this.value.generalize(value) :
            value;
    }


    public Value getValue()
    {
        return value;
    }


    // Small utility methods.

    private Value initialValue(String type)
    {
        switch (type.charAt(0))
        {
            case ClassConstants.INTERNAL_TYPE_BOOLEAN:
            case ClassConstants.INTERNAL_TYPE_BYTE:
            case ClassConstants.INTERNAL_TYPE_CHAR:
            case ClassConstants.INTERNAL_TYPE_SHORT:
            case ClassConstants.INTERNAL_TYPE_INT:
                return VALUE_FACTORY.createIntegerValue(0);

            case ClassConstants.INTERNAL_TYPE_LONG:
                return VALUE_FACTORY.createLongValue(0L);

            case ClassConstants.INTERNAL_TYPE_FLOAT:
                return VALUE_FACTORY.createFloatValue(0.0f);

            case ClassConstants.INTERNAL_TYPE_DOUBLE:
                return VALUE_FACTORY.createDoubleValue(0.0);

            case ClassConstants.INTERNAL_TYPE_CLASS_START:
            case ClassConstants.INTERNAL_TYPE_ARRAY:
                return VALUE_FACTORY.createReferenceValueNull();

            default:
                throw new IllegalArgumentException("Invalid type ["+type+"]");
        }
    }


    public static void setFieldOptimizationInfo(Clazz clazz, Field field)
    {
        MethodLinker.lastMember(field).setVisitorInfo(new FieldOptimizationInfo(clazz, field));
    }


    public static FieldOptimizationInfo getFieldOptimizationInfo(Field field)
    {
        Object visitorInfo = MethodLinker.lastMember(field).getVisitorInfo();

        return visitorInfo instanceof FieldOptimizationInfo ?
            (FieldOptimizationInfo)visitorInfo :
            null;
    }
}
