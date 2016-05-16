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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import signature.converter.util.AbstractConvertTest;
import signature.converter.util.CompilationUnit;
import signature.model.IAnnotation;
import signature.model.IApi;
import signature.model.IArrayType;
import signature.model.IClassDefinition;
import signature.model.IClassReference;
import signature.model.IConstructor;
import signature.model.IField;
import signature.model.IMethod;
import signature.model.IPackage;
import signature.model.IParameter;
import signature.model.IParameterizedType;
import signature.model.ITypeReference;
import signature.model.ITypeVariableDefinition;
import signature.model.ITypeVariableReference;
import signature.model.Kind;
import signature.model.Modifier;
import signature.model.impl.SigPrimitiveType;
import signature.model.util.ModelUtil;

public abstract class ConvertClassTest extends AbstractConvertTest {
    
    @Test
    public void convertPackageClassTest() throws IOException {
        IApi api = convert(new CompilationUnit("a.b.A", "package a.b; public class A{}"));
        IPackage sigPackage = ModelUtil.getPackage(api, "a.b");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals("a.b.A", sigClass.getQualifiedName());
        assertEquals("A", sigClass.getName());
        assertEquals("a.b", sigPackage.getName());
        assertEquals(1, sigPackage.getClasses().size());
    }
    
    @Test
    public void convertClassClassTest() throws IOException {
        IApi api = convert(new CompilationUnit("a.A", "package a; public class A{}"));
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals("a.A", sigClass.getQualifiedName());
        assertEquals("A", sigClass.getName());
        assertEquals(Kind.CLASS, sigClass.getKind());
        assertTrue(sigClass.getModifiers().contains(Modifier.PUBLIC));
    }
    
    @Test
    public void convertClassInterfaceTest() throws IOException {
        IApi api = convert(new CompilationUnit("a.A", "package a; public interface A{}"));
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals("a.A", sigClass.getQualifiedName());
        assertEquals("A", sigClass.getName());
        assertEquals(Kind.INTERFACE, sigClass.getKind());
        assertTrue(sigClass.getModifiers().contains(Modifier.PUBLIC));
    }
    
    @Test
    public void convertClassEnumTest() throws IOException {
        IApi api = convert(new CompilationUnit("a.A", "package a; public enum A{}"));
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals("a.A", sigClass.getQualifiedName());
        assertEquals("A", sigClass.getName());
        assertEquals(Kind.ENUM, sigClass.getKind());
        assertTrue(sigClass.getModifiers().contains(Modifier.PUBLIC));
    }
    
    @Test
    public void convertClassAnnotationTest() throws IOException {
        IApi api = convert(new CompilationUnit("a.A", "package a; public @interface A{}"));
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals("a.A", sigClass.getQualifiedName());
        assertEquals("A", sigClass.getName());
        assertEquals(Kind.ANNOTATION, sigClass.getKind());
        assertTrue(sigClass.getModifiers().contains(Modifier.PUBLIC));
    }
    
    @Test
    public void convertAnnotationTest() throws IOException {
        IApi api = convert(new CompilationUnit("a.A", "package a; @Deprecated public class A{}"));
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals("a.A", sigClass.getQualifiedName());
        assertEquals("A", sigClass.getName());
        assertEquals(Kind.CLASS, sigClass.getKind());
        assertTrue(sigClass.getModifiers().contains(Modifier.PUBLIC));
        assertEquals(1, sigClass.getAnnotations().size());
        IAnnotation annotation = sigClass.getAnnotations().iterator().next();        
        assertEquals("java.lang.Deprecated", annotation.getType().getClassDefinition().getQualifiedName());
    }
    
    @Test
    public void convertAnnotationOnFieldTest() throws IOException {
        IApi api = convert(new CompilationUnit("a.A", 
                "package a; " +
                "public class A{" +
                "  @Deprecated" +
                "  public int f;" +
                "}"));
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals("a.A", sigClass.getQualifiedName());
        assertEquals("A", sigClass.getName());
        assertEquals(Kind.CLASS, sigClass.getKind());
        assertTrue(sigClass.getModifiers().contains(Modifier.PUBLIC));
        assertEquals(1, sigClass.getFields().size());
        IField field = sigClass.getFields().iterator().next(); 
        assertEquals("f", field.getName());
        assertEquals(1, field.getAnnotations().size());
        IAnnotation annotation = field.getAnnotations().iterator().next();        
        assertEquals("java.lang.Deprecated", annotation.getType().getClassDefinition().getQualifiedName());
    }
    
    @Test
    public void convertGenericClass0() throws IOException {
        IApi api = convert(new CompilationUnit("a.A", "package a; public class A<T> {}"));
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
       assertEquals("a.A", sigClass.getQualifiedName());
        assertEquals("A", sigClass.getName());
        assertEquals(Kind.CLASS, sigClass.getKind());
        assertTrue(sigClass.getModifiers().contains(Modifier.PUBLIC));
        //test type variable
        assertEquals(1, sigClass.getTypeParameters().size());
        ITypeVariableDefinition variable = sigClass.getTypeParameters().get(0);
        assertEquals("T", variable.getName());
        //test type variable bound 
        assertEquals(1, variable.getUpperBounds().size());
        IClassReference bound = (IClassReference) variable.getUpperBounds().get(0);
        assertEquals("java.lang.Object", bound.getClassDefinition().getQualifiedName());
    }
    
    @Test
    public void convertGenericClass00() throws IOException {
        IApi api = convert(new CompilationUnit("a.A", "package a; public class A<T extends Integer> {}"));
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals("a.A", sigClass.getQualifiedName());
        assertEquals("A", sigClass.getName());
        assertEquals(Kind.CLASS, sigClass.getKind());
        assertTrue(sigClass.getModifiers().contains(Modifier.PUBLIC));
        //test type variable
        assertEquals(1, sigClass.getTypeParameters().size());
        ITypeVariableDefinition variable = sigClass.getTypeParameters().get(0);
        assertEquals("T", variable.getName());
        //test type variable bound 
        assertEquals(1, variable.getUpperBounds().size());
        IClassReference bound = (IClassReference) variable.getUpperBounds().get(0);
        assertEquals("java.lang.Integer", bound.getClassDefinition().getQualifiedName());
    }
    
    @Test
    public void convertGenericClass1() throws IOException {
        IApi api = convert(new CompilationUnit("a.A", "package a; public class A<S,T> {}"));
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals("a.A", sigClass.getQualifiedName());
        assertEquals("A", sigClass.getName());
        assertEquals(Kind.CLASS, sigClass.getKind());
        assertTrue(sigClass.getModifiers().contains(Modifier.PUBLIC));
        //test type variable
        assertEquals(2, sigClass.getTypeParameters().size());
        ITypeVariableDefinition variableS = sigClass.getTypeParameters().get(0);
        assertEquals("S", variableS.getName());
        //test type variable bound 
        assertEquals(1, variableS.getUpperBounds().size());
        IClassReference boundS = (IClassReference) variableS.getUpperBounds().get(0);
        assertEquals("java.lang.Object", boundS.getClassDefinition().getQualifiedName());
        
        ITypeVariableDefinition variableT = sigClass.getTypeParameters().get(1);
        assertEquals("T", variableT.getName());
        //test type variable bound 
        assertEquals(1, variableT.getUpperBounds().size());
        IClassReference boundT = (IClassReference) variableT.getUpperBounds().get(0);
        assertEquals("java.lang.Object", boundT.getClassDefinition().getQualifiedName());
    }
    
    @Test
    public void convertGenericClass2() throws IOException {
        IApi api = convert(new CompilationUnit("a.A", "package a; public class A<S,T extends S> {}"));
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals("a.A", sigClass.getQualifiedName());
        assertEquals("A", sigClass.getName());
        assertEquals(Kind.CLASS, sigClass.getKind());
        assertTrue(sigClass.getModifiers().contains(Modifier.PUBLIC));
        //test type variable
        assertEquals(2, sigClass.getTypeParameters().size());
        ITypeVariableDefinition variableS = sigClass.getTypeParameters().get(0);
        assertEquals("S", variableS.getName());
        //test type variable bound 
        assertEquals(1, variableS.getUpperBounds().size());
        IClassReference boundS = (IClassReference) variableS.getUpperBounds().get(0);
        assertEquals("java.lang.Object", boundS.getClassDefinition().getQualifiedName());
        
        ITypeVariableDefinition variableT = sigClass.getTypeParameters().get(1);
        assertEquals("T", variableT.getName());
        //test type variable bound 
        assertEquals(1, variableT.getUpperBounds().size());
        ITypeVariableReference boundT = (ITypeVariableReference) variableT.getUpperBounds().get(0);
        assertSame(variableS, boundT.getTypeVariableDefinition());
    }
    
    @Test
    public void convertClassImplementsInterface0() throws IOException {
        IApi api = convert(new CompilationUnit("a.A", "package a; public class A implements Cloneable {}"));
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals("a.A", sigClass.getQualifiedName());
        assertEquals("A", sigClass.getName());
        assertEquals(Kind.CLASS, sigClass.getKind());
        assertTrue(sigClass.getModifiers().contains(Modifier.PUBLIC));
        
        assertEquals(1, sigClass.getInterfaces().size());
        IClassDefinition clonable = ((IClassReference) sigClass.getInterfaces().iterator().next()).getClassDefinition();
        assertEquals("java.lang.Cloneable", clonable.getQualifiedName());
        assertEquals(Kind.INTERFACE, clonable.getKind());
    }
    
    @Test
    public void convertClassImplementsInterface1() throws IOException {
        CompilationUnit classSrc = new CompilationUnit("a.A", "package a; public class A implements I {}");
        CompilationUnit interfaceSrc = new CompilationUnit("a.I", "package a; public interface I {}");
        IApi api = convert(classSrc, interfaceSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals("a.A", sigClass.getQualifiedName());
        assertEquals("A", sigClass.getName());
        assertEquals(Kind.CLASS, sigClass.getKind());
        assertTrue(sigClass.getModifiers().contains(Modifier.PUBLIC));
        
        IClassDefinition sigInterface = ModelUtil.getClass(sigPackage, "I");
        assertEquals("a.I", sigInterface.getQualifiedName());
        assertEquals("I", sigInterface.getName());
        assertEquals(Kind.INTERFACE, sigInterface.getKind());
        assertTrue(sigInterface.getModifiers().contains(Modifier.PUBLIC));
        
        assertEquals(1, sigClass.getInterfaces().size());
        IClassReference interfaze = (IClassReference) sigClass.getInterfaces().iterator().next();
        assertEquals(Kind.INTERFACE, interfaze.getClassDefinition().getKind());
        assertSame(sigInterface, interfaze.getClassDefinition());
    }
    
    @Test
    public void convertClassImplementsGenericInterface0() throws IOException {
        CompilationUnit classSrc = new CompilationUnit("a.A", "package a; public class A implements I<String> {}");
        CompilationUnit interfaceSrc = new CompilationUnit("a.I", "package a; public interface I<T> {}");
        IApi api = convert(classSrc, interfaceSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals("a.A", sigClass.getQualifiedName());
        assertEquals("A", sigClass.getName());
        assertEquals(Kind.CLASS, sigClass.getKind());
        assertTrue(sigClass.getModifiers().contains(Modifier.PUBLIC));
        
        IClassDefinition sigInterface = ModelUtil.getClass(sigPackage, "I");
        assertEquals("a.I", sigInterface.getQualifiedName());
        assertEquals("I", sigInterface.getName());
        assertEquals(Kind.INTERFACE, sigInterface.getKind());
        assertTrue(sigInterface.getModifiers().contains(Modifier.PUBLIC));
        
        assertEquals(1, sigClass.getInterfaces().size());
        IParameterizedType interfaze = (IParameterizedType) sigClass.getInterfaces().iterator().next();
        //compare raw type "I"
        assertSame(sigInterface, interfaze.getRawType().getClassDefinition());
        //test type argument string
        assertEquals(1,  interfaze.getTypeArguments().size());
        IClassReference string = (IClassReference) interfaze.getTypeArguments().get(0);
        assertEquals("java.lang.String", string.getClassDefinition().getQualifiedName());
    }
    
    @Test
    public void convertGenericClassImplementsGenericInterface() throws IOException {
        CompilationUnit classSrc = new CompilationUnit("a.A", "package a; public class A<T> implements I<T> {}");
        CompilationUnit interfaceSrc = new CompilationUnit("a.I", "package a; public interface I<T> {}");
        IApi api = convert(classSrc, interfaceSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals("a.A", sigClass.getQualifiedName());
        assertEquals("A", sigClass.getName());
        assertEquals(Kind.CLASS, sigClass.getKind());
        assertTrue(sigClass.getModifiers().contains(Modifier.PUBLIC));
        
        //get type variable from class
        assertEquals(1, sigClass.getTypeParameters().size());
        ITypeVariableDefinition classTypeVar = sigClass.getTypeParameters().get(0);
        assertEquals("T", classTypeVar.getName());
        assertSame(sigClass, classTypeVar.getGenericDeclaration());
        
        //inspect interface
        IClassDefinition sigInterface = ModelUtil.getClass(sigPackage, "I");
        assertEquals("a.I", sigInterface.getQualifiedName());
        assertEquals("I", sigInterface.getName());
        assertEquals(Kind.INTERFACE, sigInterface.getKind());
        assertTrue(sigInterface.getModifiers().contains(Modifier.PUBLIC));
        
        //get type variable from interface
        assertEquals(1, sigInterface.getTypeParameters().size());
        ITypeVariableDefinition interfaceTypeVar = sigInterface.getTypeParameters().get(0);
        
        //get implemented interface from A
        assertEquals(1, sigClass.getInterfaces().size());
        IParameterizedType interfaze = (IParameterizedType) sigClass.getInterfaces().iterator().next();
        
        //compare raw type "I"
        assertSame(sigInterface, interfaze.getRawType().getClassDefinition());
        
        //test if type variable of A is type argument of I
        assertEquals(1,  interfaze.getTypeArguments().size());
        ITypeVariableReference argument = (ITypeVariableReference) interfaze.getTypeArguments().get(0);
        assertSame(sigClass, argument.getTypeVariableDefinition().getGenericDeclaration());
        assertEquals(classTypeVar, argument.getTypeVariableDefinition());
        
        //test that the two type parameters are not equal
        assertNotSame(classTypeVar, interfaceTypeVar);
        assertEquals(classTypeVar.getName(), interfaceTypeVar.getName());
        assertNotSame(classTypeVar.getGenericDeclaration(), interfaceTypeVar.getGenericDeclaration());
    }
    
    @Test
    public void convertConstructor0() throws IOException {
        CompilationUnit classSrc = new CompilationUnit("a.A", "package a; public class A { public A(){} }");
        IApi api = convert(classSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals(1, sigClass.getConstructors().size());
        IConstructor constructor = sigClass.getConstructors().iterator().next();
        assertTrue(constructor.getModifiers().contains(Modifier.PUBLIC));
    }
    
    @Test
    public void convertConstructor1a() throws IOException {
        CompilationUnit classSrc = new CompilationUnit("a.A", "package a; public class A { public class B{ } }");
        IApi api = convert(classSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A.B");
        assertEquals(1, sigClass.getConstructors().size());
        IConstructor constructor = sigClass.getConstructors().iterator().next();
        assertTrue(constructor.getModifiers().contains(Modifier.PUBLIC));
    }
    
    @Test
    public void convertConstructor1b() throws IOException {
        CompilationUnit classSrc = new CompilationUnit("a.A", "package a; public class A { protected class B{ } }");
        IApi api = convert(classSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A.B");
        assertEquals(1, sigClass.getConstructors().size());
        IConstructor constructor = sigClass.getConstructors().iterator().next();
        assertTrue(constructor.getModifiers().contains(Modifier.PROTECTED));
    }
    
    @Test
    public void convertConstructor2() throws IOException {
        CompilationUnit classSrc = new CompilationUnit("a.A", "package a; public class A {}");
        IApi api = convert(classSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals(1, sigClass.getConstructors().size());
    }
    
    @Test
    public void convertConstructor3() throws IOException {
        CompilationUnit classSrc = new CompilationUnit("a.A", "package a; public class A { public class B { public B(){}} }");
        IApi api = convert(classSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A.B");
        assertEquals(1, sigClass.getConstructors().size());
    }
    
    @Test
    public void convertConstructorWithException() throws IOException {
        CompilationUnit classSrc = new CompilationUnit("a.A", "package a; public class A { public A() throws NullPointerException{} }");
        IApi api = convert(classSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals(1, sigClass.getConstructors().size());
        IConstructor constructor = sigClass.getConstructors().iterator().next();
        assertTrue(constructor.getModifiers().contains(Modifier.PUBLIC));
        assertEquals(1, constructor.getExceptions().size());
        IClassReference exception = (IClassReference) constructor.getExceptions().iterator().next();
        assertEquals("java.lang.NullPointerException", exception.getClassDefinition().getQualifiedName());
    }
    
    @Test
    public void convertConstructorWithParameter() throws IOException {
        CompilationUnit classSrc = new CompilationUnit("a.A", "package a; public class A { public A(String param){} }");
        IApi api = convert(classSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals(1, sigClass.getConstructors().size());
        IConstructor constructor = sigClass.getConstructors().iterator().next();
        assertTrue(constructor.getModifiers().contains(Modifier.PUBLIC));
        assertEquals(1, constructor.getParameters().size());
        IParameter param = constructor.getParameters().get(0);
        assertEquals("java.lang.String", ((IClassReference)param.getType()).getClassDefinition().getQualifiedName());
    }
    
    @Test
    public void convertConstructorWithGenericParameter() throws IOException {
        CompilationUnit classSrc = new CompilationUnit("a.A", "package a; public class A<T> { public A(T param){} }");
        IApi api = convert(classSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals(1, sigClass.getConstructors().size());
        IConstructor constructor = sigClass.getConstructors().iterator().next();
        assertTrue(constructor.getModifiers().contains(Modifier.PUBLIC));
        assertEquals(1, constructor.getParameters().size());
        IParameter param = constructor.getParameters().get(0);
        ITypeVariableDefinition paramType = ((ITypeVariableReference)param.getType()).getTypeVariableDefinition();
        assertEquals("T", paramType.getName());
        assertSame(sigClass, paramType.getGenericDeclaration());
    }
    
    @Test
    public void convertVoidMethod() throws IOException {
        CompilationUnit classSrc = new CompilationUnit("a.A", "package a; public class A { public void m(){} }");
        IApi api = convert(classSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals(1, sigClass.getMethods().size());
        IMethod method = sigClass.getMethods().iterator().next();
        assertTrue(method.getModifiers().contains(Modifier.PUBLIC));
        assertEquals("m", method.getName());
        assertSame(SigPrimitiveType.VOID_TYPE, method.getReturnType());
    }
    
    @Test
    public void convertArrayMethod() throws IOException {
        CompilationUnit classSrc = new CompilationUnit("a.A", "package a; public class A { public String[] m(){ return null; } }");
        IApi api = convert(classSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertEquals(1, sigClass.getMethods().size());
        IMethod method = sigClass.getMethods().iterator().next();
        assertTrue(method.getModifiers().contains(Modifier.PUBLIC));
        assertEquals("m", method.getName());
        IArrayType arrayType = (IArrayType) method.getReturnType();
        IClassDefinition clazz = ((IClassReference) arrayType.getComponentType()).getClassDefinition();
        assertEquals("java.lang.String", clazz.getQualifiedName());
    }
    
     @Test
    public void testParameterConformance1() throws IOException {
           IApi api = convert(new CompilationUnit("test.A", 
                "package test; " +
                   "import java.util.List;" +
                   "public class A<T> {" +
                   "  public String[] a;" +
                   "  public String[] b;" +
                   "  public List<String> c;" +
                   "  public List<String> d;" +
                   "  public List<T> e;" +
                   "  public List<T> f;" +
                   "}"
           ));
        IPackage p = ModelUtil.getPackage(api, "test");
        assertEquals("test", p.getName());
        IClassDefinition c = ModelUtil.getClass(p, "A");
        assertEquals("A", c.getName());
        assertEquals("test.A", c.getQualifiedName());
        
        Map<String, IField> fields = new HashMap<String, IField>();
        for(IField f : c.getFields()){
            fields.put(f.getName(), f);
        }
        assertSame(((IClassReference)((IArrayType)fields.get("a").getType()).getComponentType()).getClassDefinition(), ((IClassReference)((IArrayType)fields.get("b").getType()).getComponentType()).getClassDefinition());
        assertSame(((IParameterizedType)fields.get("c").getType()).getRawType().getClassDefinition(), ((IParameterizedType)fields.get("d").getType()).getRawType().getClassDefinition());
        assertSame(((IParameterizedType)fields.get("e").getType()).getRawType().getClassDefinition(), ((IParameterizedType)fields.get("f").getType()).getRawType().getClassDefinition());
        
        ITypeReference type = fields.get("f").getType();
        assertTrue(type instanceof IParameterizedType);
        ITypeReference typeArgument = ((IParameterizedType)type).getTypeArguments().get(0);
        ITypeVariableDefinition typeParameter = c.getTypeParameters().get(0);
        assertSame(((ITypeVariableReference)typeArgument).getTypeVariableDefinition(), typeParameter);
    }

    @Test
    public void testParameterConformance2() throws IOException {
           IApi api = convert(new CompilationUnit("test.A", 
                "package test; " +
                   "public class A<T> {" +
                   "  public <T> T foo(T t){return null;}" +
                   "}"
           ));
        IPackage p = ModelUtil.getPackage(api, "test");
        assertEquals("test", p.getName());

        IPackage sigPackage = ModelUtil.getPackage(api, "test");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        ITypeVariableDefinition t = sigClass.getTypeParameters().get(0);
        
        IMethod sigMethod = sigClass.getMethods().iterator().next();
        ITypeVariableDefinition t1 = sigMethod.getTypeParameters().get(0);
        IParameter param = sigMethod.getParameters().get(0);
        ITypeReference t2 = param.getType();
        ITypeReference t3 = sigMethod.getReturnType();

        assertSame(t1, ((ITypeVariableReference)t2).getTypeVariableDefinition());
        assertSame(((ITypeVariableReference)t2).getTypeVariableDefinition(), ((ITypeVariableReference)t3).getTypeVariableDefinition());
        
        assertNotSame(t, t1);
    }
    
    @Test
    public void testInnerReferenceToOuterTypeVariable() throws IOException {
           IApi api = convert(new CompilationUnit("test.A", 
                "package test; " +
                "public class A<T> {" +
                "  public class B {" +
                "    public T f;" +
                "  }  " +
                "}"
        ));
        IPackage p = ModelUtil.getPackage(api, "test");
        assertEquals("test", p.getName());

        IPackage sigPackage = ModelUtil.getPackage(api, "test");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        ITypeVariableDefinition t = sigClass.getTypeParameters().get(0);
        
        IClassDefinition innerClass = ModelUtil.getClass(sigPackage, "A.B");
        IField field = ModelUtil.getField(innerClass, "f");
        
        assertSame(t, ((ITypeVariableReference)field.getType()).getTypeVariableDefinition());
    }
    
    
    @Test
    public void convertTypeVariableMultipleUpperBound() throws IOException {
        String source = 
            "package a; " +
            "public class A<T extends Comparable<T> & java.io.Serializable> {}";
            IApi api = convert(new CompilationUnit("a.A", source));
            IPackage sigPackage = ModelUtil.getPackage(api, "a");
            IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
            
            List<ITypeVariableDefinition> typeParameters = sigClass.getTypeParameters();
            assertEquals(1, typeParameters.size());
            
            ITypeVariableDefinition typeVariable = typeParameters.get(0);
            List<ITypeReference> upperBounds = typeVariable.getUpperBounds();
            assertEquals(2, upperBounds.size());
            ITypeReference firstBound = upperBounds.get(0);
            ITypeReference secondBound = upperBounds.get(1);
            
            assertTrue(firstBound instanceof IParameterizedType);
            
            IParameterizedType parametrizedType = (IParameterizedType)firstBound;
            
            IClassReference rawType = parametrizedType.getRawType();
            assertEquals("Comparable", rawType.getClassDefinition().getName());
            assertEquals(1, parametrizedType.getTypeArguments().size());
            ITypeVariableReference variable = (ITypeVariableReference) parametrizedType.getTypeArguments().get(0);
            assertSame(typeVariable, variable.getTypeVariableDefinition());
            
            assertTrue(secondBound instanceof IClassReference);
            IClassReference secondBoundClass = (IClassReference) secondBound;
            assertEquals("Serializable", secondBoundClass.getClassDefinition().getName());
    }
    
    @Test
    public void convertPrivateStaticInnerClass0() throws IOException {
        String source = 
            "package a; " +
            "public class A { " +
            "  private static class I{}" +
            "  private transient Object i = new I(); " +
            "}";
            IApi api = convert(Visibility.PRIVATE, new CompilationUnit("a.A", source));
            IPackage sigPackage = ModelUtil.getPackage(api, "a");
            IClassDefinition innerClass = ModelUtil.getClass(sigPackage, "A.I");
            assertEquals("A.I", innerClass.getName());
            Set<IConstructor> constructors = innerClass.getConstructors();
            assertEquals(1, constructors.size());
    }
    
    @Test
    public void convertPrivateStaticInnerClass1() throws IOException {
        String source = 
            "package a; " +
            "public class A {" + 
            "  private static class B {" + 
            "    public static class C {}" + 
            "  }" + 
            "}";
            IApi api = convert(new CompilationUnit("a.A", source));
            IPackage sigPackage = ModelUtil.getPackage(api, "a");
            assertEquals(1, sigPackage.getClasses().size());
    }
    
    /**
     * Tests whether the first constructor argument is removed.
     */
    @Test
    public void convertNonStaticInnerClassConstructor0() throws IOException {
        String source = 
            "package a; " +
            "public class A {" + 
            "  public class B {" + 
            "      public B(){}" +
            "  }" + 
            "}";
            IApi api = convert(new CompilationUnit("a.A", source));
            IPackage sigPackage = ModelUtil.getPackage(api, "a");
            IClassDefinition innerClass = ModelUtil.getClass(sigPackage, "A.B");
            assertEquals(1, innerClass.getConstructors().size());
            Set<IConstructor> constructors = innerClass.getConstructors();
            IConstructor first = constructors.iterator().next();
            //implicit A.this reference must be removed
            assertEquals(0, first.getParameters().size());
    }
    
    /**
     * Tests whether the first constructor argument is removed.
     */
    @Test
    public void convertNonStaticInnerClassConstructor1() throws IOException {
        String source = 
            "package a; " +
            "public class A {" + 
            "  public class B {}" + 
            "}";
            IApi api = convert(new CompilationUnit("a.A", source));
            IPackage sigPackage = ModelUtil.getPackage(api, "a");
            IClassDefinition innerClass = ModelUtil.getClass(sigPackage, "A.B");
            assertEquals(1, innerClass.getConstructors().size());
            Set<IConstructor> constructors = innerClass.getConstructors();
            IConstructor first = constructors.iterator().next();
            //implicit A.this reference must be removed
            assertEquals(0, first.getParameters().size());
    }
    
    
   
}
