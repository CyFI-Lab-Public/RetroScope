/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.testing.mocking;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.NotFoundException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * AndroidMockGenerator creates the subclass and interface required for mocking
 * a given Class.
 * 
 * The only public method of AndroidMockGenerator is createMocksForClass. See
 * the javadocs for this method for more information about AndroidMockGenerator.
 * 
 * @author swoodward@google.com (Stephen Woodward)
 */
class AndroidMockGenerator {
  public AndroidMockGenerator() {
    ClassPool.doPruning = false;
    ClassPool.getDefault().insertClassPath(new ClassClassPath(MockObject.class));
  }

  /**
   * Creates a List of javassist.CtClass objects representing all of the
   * interfaces and subclasses required to meet the Mocking requests of the
   * Class specified by {@code clazz}.
   * 
   * A test class can request that a Class be prepared for mocking by using the
   * {@link UsesMocks} annotation at either the Class or Method level. All
   * classes specified by these annotations will have exactly two CtClass
   * objects created, one for a generated interface, and one for a generated
   * subclass. The interface and subclass both define the same methods which
   * comprise all of the mockable methods of the provided class. At present, for
   * a method to be mockable, it must be non-final and non-static, although this
   * may expand in the future.
   * 
   * The class itself must be mockable, otherwise this method will ignore the
   * requested mock and print a warning. At present, a class is mockable if it
   * is a non-final publicly-instantiable Java class that is assignable from the
   * java.lang.Object class. See the javadocs for
   * {@link java.lang.Class#isAssignableFrom(Class)} for more information about
   * what "is assignable from the Object class" means. As a non-exhaustive
   * example, if a given Class represents an Enum, Annotation, Primitive or
   * Array, then it is not assignable from Object. Interfaces are also ignored
   * since these need no modifications in order to be mocked.
   * 
   * @param clazz the Class object to have all of its UsesMocks annotations
   *        processed and the corresponding Mock Classes created.
   * @return a List of CtClass objects representing the Classes and Interfaces
   *         required for mocking the classes requested by {@code clazz}
   * @throws ClassNotFoundException
   * @throws CannotCompileException
   * @throws IOException
   */
  public List<GeneratedClassFile> createMocksForClass(Class<?> clazz)
      throws ClassNotFoundException, IOException, CannotCompileException {
    return this.createMocksForClass(clazz, SdkVersion.UNKNOWN);
  }

  public List<GeneratedClassFile> createMocksForClass(Class<?> clazz, SdkVersion sdkVersion)
      throws ClassNotFoundException, IOException, CannotCompileException {
    if (!classIsSupportedType(clazz)) {
      reportReasonForUnsupportedType(clazz);
      return Arrays.asList(new GeneratedClassFile[0]);
    }
    CtClass newInterfaceCtClass = generateInterface(clazz, sdkVersion);
    GeneratedClassFile newInterface = new GeneratedClassFile(newInterfaceCtClass.getName(),
        newInterfaceCtClass.toBytecode());
    CtClass mockDelegateCtClass = generateSubClass(clazz, newInterfaceCtClass, sdkVersion);
    GeneratedClassFile mockDelegate = new GeneratedClassFile(mockDelegateCtClass.getName(),
        mockDelegateCtClass.toBytecode());
    return Arrays.asList(new GeneratedClassFile[] {newInterface, mockDelegate});
  }

  private void reportReasonForUnsupportedType(Class<?> clazz) {
    String reason = null;
    if (clazz.isInterface()) {
      // do nothing to make sure none of the other conditions apply.
    } else if (clazz.isEnum()) {
      reason = "Cannot mock an Enum";
    } else if (clazz.isAnnotation()) {
      reason = "Cannot mock an Annotation";
    } else if (clazz.isArray()) {
      reason = "Cannot mock an Array";
    } else if (Modifier.isFinal(clazz.getModifiers())) {
      reason = "Cannot mock a Final class";
    } else if (clazz.isPrimitive()) {
      reason = "Cannot mock primitives";
    } else if (!Object.class.isAssignableFrom(clazz)) {
      reason = "Cannot mock non-classes";
    } else if (!containsUsableConstructor(clazz)) {
      reason = "Cannot mock a class with no public constructors";
    } else {
      // Whatever the reason is, it's not one that we care about.
    }
    if (reason != null) {
      // Sometimes we want to be silent, so check 'reason' against null.
      System.err.println(reason + ": " + clazz.getName());
    }
  }
  
  private boolean containsUsableConstructor(Class<?> clazz) {
    Constructor<?>[] constructors = clazz.getDeclaredConstructors();
    for (Constructor<?> constructor : constructors) {
      if (Modifier.isPublic(constructor.getModifiers()) ||
          Modifier.isProtected(constructor.getModifiers())) {
        return true;
      }
    }
    return false;
  }

  boolean classIsSupportedType(Class<?> clazz) {
    return (containsUsableConstructor(clazz)) && Object.class.isAssignableFrom(clazz)
        && !clazz.isInterface() && !clazz.isEnum() && !clazz.isAnnotation() && !clazz.isArray()
        && !Modifier.isFinal(clazz.getModifiers());
  }

  void saveCtClass(CtClass clazz) throws ClassNotFoundException, IOException {
    try {
      clazz.writeFile();
    } catch (NotFoundException e) {
      throw new ClassNotFoundException("Error while saving modified class " + clazz.getName(), e);
    } catch (CannotCompileException e) {
      throw new RuntimeException("Internal Error: Attempt to save syntactically incorrect code "
          + "for class " + clazz.getName(), e);
    }
  }

  CtClass generateInterface(Class<?> originalClass, SdkVersion sdkVersion) {
    ClassPool classPool = getClassPool();
    try {
      return classPool.getCtClass(FileUtils.getInterfaceNameFor(originalClass, sdkVersion));
    } catch (NotFoundException e) {
      CtClass newInterface =
          classPool.makeInterface(FileUtils.getInterfaceNameFor(originalClass, sdkVersion));
      addInterfaceMethods(originalClass, newInterface);
      return newInterface;
    }
  }

  String getInterfaceMethodSource(Method method) throws UnsupportedOperationException {
    StringBuilder methodBody = getMethodSignature(method);
    methodBody.append(";");
    return methodBody.toString();
  }

  private StringBuilder getMethodSignature(Method method) {
    int modifiers = method.getModifiers();
    if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
      throw new UnsupportedOperationException(
          "Cannot specify final or static methods in an interface");
    }
    StringBuilder methodSignature = new StringBuilder("public ");
    methodSignature.append(getClassName(method.getReturnType()));
    methodSignature.append(" ");
    methodSignature.append(method.getName());
    methodSignature.append("(");
    int i = 0;
    for (Class<?> arg : method.getParameterTypes()) {
      methodSignature.append(getClassName(arg));
      methodSignature.append(" arg");
      methodSignature.append(i);
      if (i < method.getParameterTypes().length - 1) {
        methodSignature.append(",");
      }
      i++;
    }
    methodSignature.append(")");
    if (method.getExceptionTypes().length > 0) {
      methodSignature.append(" throws ");
    }
    i = 0;
    for (Class<?> exception : method.getExceptionTypes()) {
      methodSignature.append(getClassName(exception));
      if (i < method.getExceptionTypes().length - 1) {
        methodSignature.append(",");
      }
      i++;
    }
    return methodSignature;
  }

  private String getClassName(Class<?> clazz) {
    return clazz.getCanonicalName();
  }

  static ClassPool getClassPool() {
    return ClassPool.getDefault();
  }

  private boolean classExists(String name) {
    // The following line is the ideal, but doesn't work (bug in library).
    // return getClassPool().find(name) != null;
    try {
      getClassPool().get(name);
      return true;
    } catch (NotFoundException e) {
      return false;
    }
  }

  CtClass generateSubClass(Class<?> superClass, CtClass newInterface, SdkVersion sdkVersion)
      throws ClassNotFoundException {
    if (classExists(FileUtils.getSubclassNameFor(superClass, sdkVersion))) {
      try {
        return getClassPool().get(FileUtils.getSubclassNameFor(superClass, sdkVersion));
      } catch (NotFoundException e) {
        throw new ClassNotFoundException("This should be impossible, since we just checked for "
            + "the existence of the class being created", e);
      }
    }
    CtClass newClass = generateSkeletalClass(superClass, newInterface, sdkVersion);
    if (!newClass.isFrozen()) {
      newClass.addInterface(newInterface);
      try {
        newClass.addInterface(getClassPool().get(MockObject.class.getName()));
      } catch (NotFoundException e) {
        throw new ClassNotFoundException("Could not find " + MockObject.class.getName(), e);
      }
      addMethods(superClass, newClass);
      addGetDelegateMethod(newClass);
      addSetDelegateMethod(newClass, newInterface);
      addConstructors(newClass, superClass);
    }
    return newClass;
  }
  
  private void addConstructors(CtClass clazz, Class<?> superClass) throws ClassNotFoundException {
    CtClass superCtClass = getCtClassForClass(superClass);
    
    CtConstructor[] constructors = superCtClass.getDeclaredConstructors();
    for (CtConstructor constructor : constructors) {
      int modifiers = constructor.getModifiers();
      if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
         CtConstructor ctConstructor;
        try {
          ctConstructor = CtNewConstructor.make(constructor.getParameterTypes(),
               constructor.getExceptionTypes(), clazz);
          clazz.addConstructor(ctConstructor);
        } catch (CannotCompileException e) {
          throw new RuntimeException("Internal Error - Could not add constructors.", e);
        } catch (NotFoundException e) {
          throw new RuntimeException("Internal Error - Constructor suddenly could not be found", e);
        }
      }
    }
  }

  CtClass getCtClassForClass(Class<?> clazz) throws ClassNotFoundException {
    ClassPool classPool = getClassPool();
    try {
      return classPool.get(clazz.getName());
    } catch (NotFoundException e) {
      throw new ClassNotFoundException("Class not found when finding the class to be mocked: "
          + clazz.getName(), e);
    }
  }

  private void addSetDelegateMethod(CtClass clazz, CtClass newInterface) {
    try {
      clazz.addMethod(CtMethod.make(getSetDelegateMethodSource(newInterface), clazz));
    } catch (CannotCompileException e) {
      throw new RuntimeException("Internal error while creating the setDelegate() method", e);
    }
  }

  String getSetDelegateMethodSource(CtClass newInterface) {
    return "public void setDelegate___AndroidMock(" + newInterface.getName() + " obj) { this."
        + getDelegateFieldName() + " = obj;}";
  }

  private void addGetDelegateMethod(CtClass clazz) {
    try {
      CtMethod newMethod = CtMethod.make(getGetDelegateMethodSource(), clazz);
      try {
        CtMethod existingMethod = clazz.getMethod(newMethod.getName(), newMethod.getSignature());
        clazz.removeMethod(existingMethod);
      } catch (NotFoundException e) {
        // expected path... sigh.
      }
      clazz.addMethod(newMethod);
    } catch (CannotCompileException e) {
      throw new RuntimeException("Internal error while creating the getDelegate() method", e);
    }
  }

  private String getGetDelegateMethodSource() {
    return "public Object getDelegate___AndroidMock() { return this." + getDelegateFieldName()
        + "; }";
  }

  String getDelegateFieldName() {
    return "delegateMockObject";
  }

  void addInterfaceMethods(Class<?> originalClass, CtClass newInterface) {
    Method[] methods = getAllMethods(originalClass);
    for (Method method : methods) {
      try {
        if (isMockable(method)) {
          CtMethod newMethod = CtMethod.make(getInterfaceMethodSource(method), newInterface);
          newInterface.addMethod(newMethod);
        }
      } catch (UnsupportedOperationException e) {
        // Can't handle finals and statics.
      } catch (CannotCompileException e) {
        throw new RuntimeException(
            "Internal error while creating a new Interface method for class "
                + originalClass.getName() + ".  Method name: " + method.getName(), e);
      }
    }
  }

  void addMethods(Class<?> superClass, CtClass newClass) {
    Method[] methods = getAllMethods(superClass);
    if (newClass.isFrozen()) {
      newClass.defrost();
    }
    List<CtMethod> existingMethods = Arrays.asList(newClass.getDeclaredMethods());
    for (Method method : methods) {
      try {
        if (isMockable(method)) {
          CtMethod newMethod = CtMethod.make(getDelegateMethodSource(method), newClass);
          if (!existingMethods.contains(newMethod)) {
            newClass.addMethod(newMethod);
          }
        }
      } catch (UnsupportedOperationException e) {
        // Can't handle finals and statics.
      } catch (CannotCompileException e) {
        throw new RuntimeException("Internal Error while creating subclass methods for "
            + newClass.getName() + " method: " + method.getName(), e);
      }
    }
  }

  Method[] getAllMethods(Class<?> clazz) {
    Map<String, Method> methodMap = getAllMethodsMap(clazz);
    return methodMap.values().toArray(new Method[0]);
  }

  private Map<String, Method> getAllMethodsMap(Class<?> clazz) {
    Map<String, Method> methodMap = new HashMap<String, Method>();
    Class<?> superClass = clazz.getSuperclass();
    if (superClass != null) {
      methodMap.putAll(getAllMethodsMap(superClass));
    }
    List<Method> methods = new ArrayList<Method>(Arrays.asList(clazz.getDeclaredMethods()));
    for (Method method : methods) {
      String key = method.getName();
      for (Class<?> param : method.getParameterTypes()) {
        key += param.getCanonicalName();
      }
      methodMap.put(key, method);
    }
    return methodMap;
  }

  boolean isMockable(Method method) {
    if (isForbiddenMethod(method)) {
      return false;
    }
    int modifiers = method.getModifiers();
    return !Modifier.isFinal(modifiers) && !Modifier.isStatic(modifiers) && !method.isBridge()
        && (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers));
  }

  boolean isForbiddenMethod(Method method) {
    if (method.getName().equals("equals")) {
      return method.getParameterTypes().length == 1
          && method.getParameterTypes()[0].equals(Object.class);
    } else if (method.getName().equals("toString")) {
      return method.getParameterTypes().length == 0;
    } else if (method.getName().equals("hashCode")) {
      return method.getParameterTypes().length == 0;
    }
    return false;
  }

  private String getReturnDefault(Method method) {
    Class<?> returnType = method.getReturnType();
    if (!returnType.isPrimitive()) {
      return "null";
    } else if (returnType == Boolean.TYPE) {
      return "false";
    } else if (returnType == Void.TYPE) {
      return "";
    } else {
      return "(" + returnType.getName() + ")0";
    }
  }
  
  String getDelegateMethodSource(Method method) {
    StringBuilder methodBody = getMethodSignature(method);
    methodBody.append("{");
    methodBody.append("if(this.");
    methodBody.append(getDelegateFieldName());
    methodBody.append("==null){return ");
    methodBody.append(getReturnDefault(method));
    methodBody.append(";}");
    if (!method.getReturnType().equals(Void.TYPE)) {
      methodBody.append("return ");
    }
    methodBody.append("this.");
    methodBody.append(getDelegateFieldName());
    methodBody.append(".");
    methodBody.append(method.getName());
    methodBody.append("(");
    for (int i = 0; i < method.getParameterTypes().length; ++i) {
      methodBody.append("arg");
      methodBody.append(i);
      if (i < method.getParameterTypes().length - 1) {
        methodBody.append(",");
      }
    }
    methodBody.append(");}");
    return methodBody.toString();
  }

  CtClass generateSkeletalClass(Class<?> superClass, CtClass newInterface, SdkVersion sdkVersion)
      throws ClassNotFoundException {
    ClassPool classPool = getClassPool();
    CtClass superCtClass = getCtClassForClass(superClass);
    String subclassName = FileUtils.getSubclassNameFor(superClass, sdkVersion);

    CtClass newClass;
    try {
      newClass = classPool.makeClass(subclassName, superCtClass);
    } catch (RuntimeException e) {
      if (e.getMessage().contains("frozen class")) {
        try {
          return classPool.get(subclassName);
        } catch (NotFoundException ex) {
          throw new ClassNotFoundException("Internal Error: could not find class", ex);
        }
      }
      throw e;
    }

    try {
      newClass.addField(new CtField(newInterface, getDelegateFieldName(), newClass));
    } catch (CannotCompileException e) {
      throw new RuntimeException("Internal error adding the delegate field to "
          + newClass.getName(), e);
    }
    return newClass;
  }
}
