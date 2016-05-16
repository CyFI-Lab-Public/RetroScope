/*******************************************************************************
 * Copyright (c) 2011 Google, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Google, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.wb.internal.core.utils.reflect;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Helper for setting properties for {@link ClassLoader}.
 * <p>
 * http://java.dzone.com/articles/classloaderlocal-how-avoid
 * 
 * @author Jevgeni Kabanov
 * @author scheglov_ke
 * @coverage core.util
 */
@SuppressWarnings("unchecked")
public class ClassLoaderLocalMap implements Opcodes {
  private static final String NAME = "GEN$$ClassLoaderProperties";
  private static final Map<Object, Object> globalMap =
      Collections.synchronizedMap(new WeakHashMap<Object, Object>());
  private static Method defineMethod;
  private static Method findLoadedClass;
  static {
    try {
      defineMethod =
          ClassLoader.class.getDeclaredMethod("defineClass", new Class[]{
              String.class,
              byte[].class,
              int.class,
              int.class});
      defineMethod.setAccessible(true);
      findLoadedClass =
          ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[]{String.class});
      findLoadedClass.setAccessible(true);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Map
  //
  ////////////////////////////////////////////////////////////////////////////
  public static boolean containsKey(ClassLoader cl, Object key) {
    if (cl == null) {
      return globalMap.containsKey(key);
    }
    // synchronizing over ClassLoader is usually safest
    synchronized (cl) {
      if (!hasHolder(cl)) {
        return false;
      }
      return getLocalMap(cl).containsKey(key);
    }
  }

  public static void put(ClassLoader cl, Object key, Object value) {
    if (cl == null) {
      globalMap.put(key, value);
      return;
    }
    // synchronizing over ClassLoader is usually safest
    synchronized (cl) {
      getLocalMap(cl).put(key, value);
    }
  }

  public static Object get(ClassLoader cl, Object key) {
    if (cl == null) {
      return globalMap.get(key);
    }
    // synchronizing over ClassLoader is usually safest
    synchronized (cl) {
      return getLocalMap(cl).get(key);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Implementation
  //
  ////////////////////////////////////////////////////////////////////////////
  private static boolean hasHolder(ClassLoader cl) {
    String propertiesClassName = NAME;
    try {
      Class<?> clazz = (Class<?>) findLoadedClass.invoke(cl, new Object[]{propertiesClassName});
      if (clazz == null) {
        return false;
      }
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getTargetException());
    }
    return true;
  }

  private static Map<Object, Object> getLocalMap(ClassLoader cl) {
    String holderClassName = NAME;
    Class<?> holderClass;
    try {
      holderClass = (Class<?>) findLoadedClass.invoke(cl, new Object[]{holderClassName});
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getTargetException());
    }
    if (holderClass == null) {
      byte[] classBytes = buildHolderByteCode(holderClassName);
      try {
        holderClass =
            (Class<?>) defineMethod.invoke(
                cl,
                new Object[]{
                    holderClassName,
                    classBytes,
                    Integer.valueOf(0),
                    Integer.valueOf(classBytes.length)});
      } catch (InvocationTargetException e1) {
        throw new RuntimeException(e1.getTargetException());
      } catch (Throwable e1) {
        throw new RuntimeException(e1);
      }
    }
    try {
      return (Map<Object, Object>) holderClass.getDeclaredField("localMap").get(null);
    } catch (Throwable e1) {
      throw new RuntimeException(e1);
    }
  }

  private static byte[] buildHolderByteCode(String holderClassName) {
    ClassWriter cw = new ClassWriter(0);
    FieldVisitor fv;
    MethodVisitor mv;
    cw.visit(V1_2, ACC_PUBLIC + ACC_SUPER, holderClassName, null, "java/lang/Object", null);
    {
      fv =
          cw.visitField(
              ACC_PUBLIC + ACC_FINAL + ACC_STATIC,
              "localMap",
              "Ljava/util/Map;",
              null,
              null);
      fv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
      mv.visitCode();
      mv.visitTypeInsn(NEW, "java/util/WeakHashMap");
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, "java/util/WeakHashMap", "<init>", "()V");
      mv.visitFieldInsn(PUTSTATIC, holderClassName, "localMap", "Ljava/util/Map;");
      mv.visitInsn(RETURN);
      mv.visitMaxs(2, 0);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
      mv.visitInsn(RETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    cw.visitEnd();
    return cw.toByteArray();
  }
}
