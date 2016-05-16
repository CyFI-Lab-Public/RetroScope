/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dex.reader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;

import dex.reader.util.JavaSourceToDexUtil;
import dex.structure.DexAnnotatedElement;
import dex.structure.DexAnnotation;
import dex.structure.DexClass;
import dex.structure.DexField;
import dex.structure.DexFile;
import dex.structure.DexMethod;
import dex.structure.DexParameter;
import dex.structure.WithModifiers;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class DexTestsCommon {
    
    protected JavaSourceToDexUtil javaToDexUtil;

    @Before
    public void initializeJavaToDexUtil() {
        javaToDexUtil = new JavaSourceToDexUtil();
    }
    
    @After
    public void shutdownJavaToDexUtil() {
        javaToDexUtil = null;
    }
    
    public static void assertPublic(WithModifiers withMod) {
        assertTrue(Modifier.isPublic(withMod.getModifiers()));
        assertFalse(Modifier.isPrivate(withMod.getModifiers())
                || Modifier.isProtected(withMod.getModifiers()));
    }
    
    public static void assertProtected(WithModifiers withMod) {
        assertTrue(Modifier.isProtected(withMod.getModifiers()));
        assertFalse(Modifier.isPrivate(withMod.getModifiers())
                || Modifier.isPublic(withMod.getModifiers()));
    }
    
    public static void assertPrivate(WithModifiers withMod) {
        assertTrue(Modifier.isPrivate(withMod.getModifiers()));
        assertFalse(Modifier.isProtected(withMod.getModifiers())
                || Modifier.isPublic(withMod.getModifiers()));
    }
    
    public static void assertDefault(WithModifiers withMod) {
        assertFalse(Modifier.isPrivate(withMod.getModifiers())
                || Modifier.isProtected(withMod.getModifiers())
                || Modifier.isPublic(withMod.getModifiers()));
    }
    
    /**
     * Creates and returns a dex file from the specified file.
     * 
     * @param fileName the name of the file to read
     * @return the dex file
     * @throws IOException if the file is not accessible
     */
    protected DexFile prepareDexFile(String fileName) throws IOException{
        DexFileReader dexReader = new DexFileReader();
        return  dexReader.read(new DexBuffer(fileName));
    }
    
    protected DexClass getClass(DexFile file, String className) {
        assertNotNull(file);
        assertNotNull(className);
        for (DexClass clazz : file.getDefinedClasses()) {
            if(className.equals(clazz.getName())){
                return clazz;
            }
        }
        fail("Class: " + className +" not present in file: " + file.getName());
        throw new IllegalArgumentException("Class: " + className +" not present in file: " + file.getName());
    }
    
    protected DexField getField(DexClass clazz, String fieldName) {
        assertNotNull(clazz);
        assertNotNull(fieldName);
        for (DexField field : clazz.getFields()) {
            if(fieldName.equals(field.getName())){
                return field;
            }
        }
        fail("Field: " + fieldName +" not present in class: " + clazz.getName());
        throw new IllegalArgumentException("Field: " + fieldName +" not present in class: " + clazz.getName());
    }
    
    protected DexAnnotation getAnnotation(DexAnnotatedElement element, String annotationType) {
        assertNotNull(element);
        assertNotNull(annotationType);
        for (DexAnnotation anno : element.getAnnotations()) {
            if(annotationType.equals(anno.getTypeName())){
                return anno;
            }
        }
        fail("Annotation: " + annotationType +" not present in Element.");
        throw new IllegalArgumentException("Annotation: " + annotationType +" not present in Element.");
    }
    
    protected DexMethod getMethod(DexClass clazz, String methodName, String... typeNames) {
        assertNotNull(clazz);
        assertNotNull(methodName);
        List<String> paramTypeNames = Arrays.asList(typeNames);
        for (DexMethod method : clazz.getMethods()) {
            List<String> methodsParamTypeNames = getParamTypeNames(method.getParameters());
            if(methodName.equals(method.getName()) && paramTypeNames.equals(methodsParamTypeNames)){
                return method;
            }
        }
        fail("Method: " + methodName +" not present in class: " + clazz.getName());
        throw new IllegalArgumentException("Method: " + methodName +" not present in class: " + clazz.getName());
    }
    
    private List<String> getParamTypeNames(List<DexParameter> parameters) {
        List<String> paramTypeNames = new LinkedList<String>();
        for (DexParameter parameter : parameters) {
            paramTypeNames.add(parameter.getTypeName());
        }
        return paramTypeNames;
    }

}
