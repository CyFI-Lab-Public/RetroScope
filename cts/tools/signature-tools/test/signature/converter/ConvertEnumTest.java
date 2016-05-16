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

package signature.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import signature.converter.util.AbstractConvertTest;
import signature.converter.util.CompilationUnit;
import signature.model.IApi;
import signature.model.IArrayType;
import signature.model.IClassDefinition;
import signature.model.IClassReference;
import signature.model.IConstructor;
import signature.model.IEnumConstant;
import signature.model.IField;
import signature.model.IMethod;
import signature.model.IPackage;
import signature.model.IParameter;
import signature.model.Kind;
import signature.model.Modifier;
import signature.model.util.ModelUtil;

public abstract class ConvertEnumTest extends AbstractConvertTest {
    
    @Test
    public void testEnum1() throws IOException {
        IApi api = convert(new CompilationUnit("test.A", 
                "package test; " +
                "public enum A {" +
                "  ONE, TWO, THREE" +
                "}"));
        IPackage sigPackage = ModelUtil.getPackage(api, "test");
        IClassDefinition c = ModelUtil.getClass(sigPackage, "A");
        assertNotNull(c);
        assertTrue(c.getKind() == Kind.ENUM);

        Set<IEnumConstant> constants = c.getEnumConstants();
        assertEquals(3, constants.size());
        
        Set<String> constantNames = new HashSet<String>();
        for (IEnumConstant constant : constants) {
            constantNames.add(constant.getName());
        }
        
        assertTrue(constantNames.contains("ONE"));
        assertTrue(constantNames.contains("TWO"));
        assertTrue(constantNames.contains("THREE"));
        
//        IEnumConstant[] enumConstants = new IEnumConstant[3];
//        for (IEnumConstant enumConstant : constants) {
//            enumConstants[enumConstant.getOrdinal()] = enumConstant;
//            assertEquals(0, enumConstant.getAnnotations().size());
//            assertSame(c, enumConstant.getType());
//        }
//        
//        assertEquals("ONE", enumConstants[0].getName());
//        assertEquals("TWO", enumConstants[1].getName());
//        assertEquals("THREE", enumConstants[2].getName());
    }

    @Test
    public void testEnum2() throws IOException {
        IApi api = convert(new CompilationUnit("test.A", 
                "package test; " +
                "public enum A {" +
                "  ONE, TWO, THREE;" +
                "  public static A FOUR = ONE;" +
                "}"));
        IPackage sigPackage = ModelUtil.getPackage(api, "test");
        IClassDefinition c = ModelUtil.getClass(sigPackage, "A");
        assertNotNull(c);
        assertTrue(c.getKind() == Kind.ENUM);

        Set<IEnumConstant> constants = c.getEnumConstants();
        assertEquals(3, constants.size());
        
        Set<IField> fields = c.getFields();
        assertEquals(1, fields.size());
        IField field = c.getFields().iterator().next();
        
        assertEquals("FOUR", field.getName());
        assertSame(c, ((IClassReference)field.getType()).getClassDefinition());
    }


   @Test
    public void testEnum3() throws IOException {
        IApi api = convert(new CompilationUnit("test.A", 
                "package test; " +
                "public enum A {" +
                "  ONE(1), TWO(2), THREE(3);" +
                "  A(int value){}" +
                "}"));
        IPackage sigPackage = ModelUtil.getPackage(api, "test");
        IClassDefinition c = ModelUtil.getClass(sigPackage, "A");
        assertNotNull(c);
        assertTrue(c.getKind() == Kind.ENUM);

        Set<IEnumConstant> constants = c.getEnumConstants();
        assertEquals(3, constants.size());
        
        Set<IConstructor> ctors = c.getConstructors();
        assertEquals(0, ctors.size());

        Set<IMethod> methods = c.getMethods();
        assertEquals(2, methods.size());
        Map<String, IMethod> map = new HashMap<String, IMethod>();
        for(IMethod m : methods){
            map.put(m.getName(), m);
        }
        
        IMethod values = map.get("values");
        assertNotNull(values);
        assertEquals(0, values.getParameters().size());
        assertTrue(values.getReturnType() instanceof IArrayType);
        assertSame(c, ((IClassReference)((IArrayType)values.getReturnType()).getComponentType()).getClassDefinition());
        assertTrue(c.getModifiers().contains(Modifier.PUBLIC));
        assertFalse(c.getModifiers().contains(Modifier.STATIC));
        assertTrue(c.getModifiers().contains(Modifier.FINAL));
        
        IMethod valueOf = map.get("valueOf");
        assertNotNull(valueOf);
        assertEquals(1, valueOf.getParameters().size());
        IParameter param = valueOf.getParameters().iterator().next();
        assertEquals("java.lang.String", ((IClassReference)param.getType()).getClassDefinition().getQualifiedName());
        assertSame(c, ((IClassReference)valueOf.getReturnType()).getClassDefinition());
    }
   
   @Test
   public void testEnum4() throws IOException {
       IApi api = convert(new CompilationUnit("test.A", 
               "package test; " +
               "public enum A {" +
               "  ONE { void m(){} }, TWO{ void m(){} };" +
               "  abstract void m();" +
               "}"));
       IPackage sigPackage = ModelUtil.getPackage(api, "test");
       IClassDefinition c = ModelUtil.getClass(sigPackage, "A");
       assertNotNull(c);
       assertTrue(c.getKind() == Kind.ENUM);

       Set<IEnumConstant> constants = c.getEnumConstants();
       assertEquals(2, constants.size());
       
       Set<IConstructor> ctors = c.getConstructors();
       assertEquals(0, ctors.size());

       Set<IMethod> methods = c.getMethods();
       assertEquals(2, methods.size());
       Map<String, IMethod> map = new HashMap<String, IMethod>();
       for(IMethod m : methods){
           map.put(m.getName(), m);
       }
       
       IMethod values = map.get("values");
       assertNotNull(values);
       assertEquals(0, values.getParameters().size());
       assertTrue(values.getReturnType() instanceof IArrayType);
       assertSame(c, ((IClassReference)((IArrayType)values.getReturnType()).getComponentType()).getClassDefinition());
       assertTrue(c.getModifiers().contains(Modifier.PUBLIC));
       assertFalse(c.getModifiers().contains(Modifier.STATIC));
       assertFalse(c.getModifiers().contains(Modifier.FINAL));
   }

}
