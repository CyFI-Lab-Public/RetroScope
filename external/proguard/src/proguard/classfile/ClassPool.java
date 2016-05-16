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
package proguard.classfile;

import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.*;

import java.util.*;

/**
 * This is a set of representations of classes. They      can be enumerated or
 * retrieved by name. They can also be accessed by means of class visitors.
 *
 * @author Eric Lafortune
 */
public class ClassPool
{
    private final Map classes = new HashMap();


    /**
     * Clears the class pool.
     */
    public void clear()
    {
        classes.clear();
    }


    /**
     * Adds the given Clazz to the class pool.
     */
    public void addClass(Clazz clazz)
    {
        classes.put(clazz.getName(), clazz);
    }


    /**
     * Removes the given Clazz from the class pool.
     */
    public void removeClass(Clazz clazz)
    {
        classes.remove(clazz.getName());
    }


    /**
     * Returns a Clazz from the class pool based on its name. Returns
     * <code>null</code> if the class with the given name is not in the class
     * pool. Returns the base class if the class name is an array type.
     */
    public Clazz getClass(String className)
    {
        return (Clazz)classes.get(ClassUtil.internalClassNameFromClassType(className));
    }


    /**
     * Returns an Iterator of all class names in the class pool.
     */
    public Iterator classNames()
    {
        return classes.keySet().iterator();
    }


    /**
     * Returns the number of classes in the class pool.
     */
    public int size()
    {
        return classes.size();
    }


    /**
     * Applies the given ClassPoolVisitor to the class pool.
     */
    public void accept(ClassPoolVisitor classPoolVisitor)
    {
        classPoolVisitor.visitClassPool(this);
    }


    /**
     * Applies the given ClassVisitor to all classes in the class pool,
     * in random order.
     */
    public void classesAccept(ClassVisitor classVisitor)
    {
        Iterator iterator = classes.values().iterator();
        while (iterator.hasNext())
        {
            Clazz clazz = (Clazz)iterator.next();
            clazz.accept(classVisitor);
        }
    }


    /**
     * Applies the given ClassVisitor to all classes in the class pool,
     * in sorted order.
     */
    public void classesAcceptAlphabetically(ClassVisitor classVisitor)
    {
        TreeMap sortedClasses = new TreeMap(classes);
        Iterator iterator = sortedClasses.values().iterator();
        while (iterator.hasNext())
        {
            Clazz clazz = (Clazz)iterator.next();
            clazz.accept(classVisitor);
        }
    }


    /**
     * Applies the given ClassVisitor to the class with the given name,
     * if it is present in the class pool.
     */
    public void classAccept(String className, ClassVisitor classVisitor)
    {
        Clazz clazz = getClass(className);
        if (clazz != null)
        {
            clazz.accept(classVisitor);
        }
    }
}
