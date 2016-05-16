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

import com.google.common.collect.Maps;

import org.eclipse.wb.internal.core.utils.check.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Map;

/**
 * Contains different Java reflection utilities.
 *
 * @author scheglov_ke
 * @coverage core.util
 */
public class ReflectionUtils {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  private ReflectionUtils() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Signature
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @param runtime
   *          is <code>true</code> if we need name for class loading, <code>false</code> if we need
   *          name for source generation.
   *
   * @return the fully qualified name of given {@link Type}.
   */
  public static String getFullyQualifiedName(Type type, boolean runtime) {
    Assert.isNotNull(type);
    // Class
    if (type instanceof Class<?>) {
      Class<?> clazz = (Class<?>) type;
      // array
      if (clazz.isArray()) {
        return getFullyQualifiedName(clazz.getComponentType(), runtime) + "[]";
      }
      // object
      String name = clazz.getName();
      if (!runtime) {
        name = name.replace('$', '.');
      }
      return name;
    }
    // GenericArrayType
    if (type instanceof GenericArrayType) {
      GenericArrayType genericArrayType = (GenericArrayType) type;
      return getFullyQualifiedName(genericArrayType.getGenericComponentType(), runtime) + "[]";
    }
    // ParameterizedType
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      Type rawType = parameterizedType.getRawType();
      // raw type
      StringBuilder sb = new StringBuilder();
      sb.append(getFullyQualifiedName(rawType, runtime));
      // type arguments
      sb.append("<");
      boolean firstTypeArgument = true;
      for (Type typeArgument : parameterizedType.getActualTypeArguments()) {
        if (!firstTypeArgument) {
          sb.append(",");
        }
        firstTypeArgument = false;
        sb.append(getFullyQualifiedName(typeArgument, runtime));
      }
      sb.append(">");
      // done
      return sb.toString();
    }
    // WildcardType
    if (type instanceof WildcardType) {
      WildcardType wildcardType = (WildcardType) type;
      return "? extends " + getFullyQualifiedName(wildcardType.getUpperBounds()[0], runtime);
    }
    // TypeVariable
    TypeVariable<?> typeVariable = (TypeVariable<?>) type;
    return typeVariable.getName();
  }

  /**
   * Appends fully qualified names of given parameter types (appends also <code>"()"</code>).
   */
  private static void appendParameterTypes(StringBuilder buffer, Type[] parameterTypes) {
    buffer.append('(');
    boolean firstParameter = true;
    for (Type parameterType : parameterTypes) {
      if (firstParameter) {
        firstParameter = false;
      } else {
        buffer.append(',');
      }
      buffer.append(getFullyQualifiedName(parameterType, false));
    }
    buffer.append(')');
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Method
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return all declared {@link Method}'s, including protected and private.
   */
  public static Map<String, Method> getMethods(Class<?> clazz) {
    Map<String, Method> methods = Maps.newHashMap();
    // process classes
    for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
      for (Method method : c.getDeclaredMethods()) {
        String signature = getMethodSignature(method);
        if (!methods.containsKey(signature)) {
          method.setAccessible(true);
          methods.put(signature, method);
        }
      }
    }
    // process interfaces
    for (Class<?> interfaceClass : clazz.getInterfaces()) {
      for (Method method : interfaceClass.getDeclaredMethods()) {
        String signature = getMethodSignature(method);
        if (!methods.containsKey(signature)) {
          method.setAccessible(true);
          methods.put(signature, method);
        }
      }
    }
    // done
    return methods;
  }

  /**
   * @return signature for given {@link Method}. This signature is not same signature as in JVM or
   *         JDT, just some string that unique identifies method in its {@link Class}.
   */
  public static String getMethodSignature(Method method) {
    Assert.isNotNull(method);
    return getMethodSignature(method.getName(), method.getParameterTypes());
  }

  /**
   * Returns the signature of {@link Method} with given combination of name and parameter types.
   * This signature is not same signature as in JVM or JDT, just some string that unique identifies
   * method in its {@link Class}.
   *
   * @param name
   *          the name of {@link Method}.
   * @param parameterTypes
   *          the types of {@link Method} parameters.
   *
   * @return signature of {@link Method}.
   */
  public static String getMethodSignature(String name, Type... parameterTypes) {
    Assert.isNotNull(name);
    Assert.isNotNull(parameterTypes);
    //
    StringBuilder buffer = new StringBuilder();
    buffer.append(name);
    appendParameterTypes(buffer, parameterTypes);
    return buffer.toString();
  }

  private static final ClassMap<Map<String, Method>> m_getMethodBySignature = ClassMap.create();

  /**
   * Returns the {@link Method} defined in {@link Class}. This method can have any visibility, i.e.
   * we can find even protected/private methods. Can return <code>null</code> if no method with
   * given signature found.
   *
   * @param clazz
   *          the {@link Class} to get method from it, or its superclass.
   * @param signature
   *          the signature of method in same format as {@link #getMethodSignature(Method)}.
   *
   * @return the {@link Method} for given signature, or <code>null</code> if no such method found.
   */
  public static Method getMethodBySignature(Class<?> clazz, String signature) {
    Assert.isNotNull(clazz);
    Assert.isNotNull(signature);
    // prepare cache
    Map<String, Method> cache = m_getMethodBySignature.get(clazz);
    if (cache == null) {
      cache = getMethods(clazz);
      m_getMethodBySignature.put(clazz, cache);
    }
    // use cache
    return cache.get(signature);
  }

  /**
   * @return the {@link Object} result of invoking method with given signature.
   */
  public static Object invokeMethod(Object object, String signature, Object... arguments)
      throws Exception {
    Assert.isNotNull(object);
    Assert.isNotNull(arguments);
    // prepare class/object
    Class<?> refClass = getRefClass(object);
    Object refObject = getRefObject(object);
    // prepare method
    Method method = getMethodBySignature(refClass, signature);
    Assert.isNotNull(method, "Can not find method " + signature + " in " + refClass);
    // do invoke
    try {
      return method.invoke(refObject, arguments);
    } catch (InvocationTargetException e) {
      throw propagate(e.getCause());
    }
  }

  /**
   * Invokes method by name and parameter types.
   *
   * @param object
   *          the object to call, may be {@link Class} for invoking static method.
   * @param name
   *          the name of method.
   * @param parameterTypes
   *          the types of parameters.
   * @param arguments
   *          the values of argument for invocation.
   *
   * @return the {@link Object} result of invoking method.
   */
  public static Object invokeMethod2(Object object,
      String name,
      Class<?>[] parameterTypes,
      Object[] arguments) throws Exception {
    Assert.equals(parameterTypes.length, arguments.length);
    String signature = getMethodSignature(name, parameterTypes);
    return invokeMethod(object, signature, arguments);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Utils
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * @return the {@link Class} of given {@link Object} or casted object, if it is {@link Class}
   *         itself.
   */
  private static Class<?> getRefClass(Object object) {
    return object instanceof Class<?> ? (Class<?>) object : object.getClass();
  }

  /**
   * @return the {@link Object} that should be used as argument for {@link Field#get(Object)} and
   *         {@link Method#invoke(Object, Object[])}.
   */
  private static Object getRefObject(Object object) {
    return object instanceof Class<?> ? null : object;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Throwable propagation
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Helper class used in {@link #propagate(Throwable)}.
   */
  private static class ExceptionThrower {
    private static Throwable throwable;

    private ExceptionThrower() throws Throwable {
      if (System.getProperty("wbp.ReflectionUtils.propagate().InstantiationException") != null) {
        throw new InstantiationException();
      }
      if (System.getProperty("wbp.ReflectionUtils.propagate().IllegalAccessException") != null) {
        throw new IllegalAccessException();
      }
      throw throwable;
    }

    public static synchronized void spit(Throwable t) {
      if (System.getProperty("wbp.ReflectionUtils.propagate().dontThrow") == null) {
        ExceptionThrower.throwable = t;
        try {
          ExceptionThrower.class.newInstance();
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } finally {
          ExceptionThrower.throwable = null;
        }
      }
    }
  }

  /**
   * Propagates {@code throwable} as-is without any wrapping. This is trick.
   *
   * @return nothing will ever be returned; this return type is only for your convenience, to use
   *         this method in "throw" statement.
   */
  public static RuntimeException propagate(Throwable throwable) {
    if (System.getProperty("wbp.ReflectionUtils.propagate().forceReturn") == null) {
      ExceptionThrower.spit(throwable);
    }
    return null;
  }
}
