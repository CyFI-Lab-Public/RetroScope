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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import signature.converter.util.AbstractConvertTest;
import signature.converter.util.CompilationUnit;
import signature.model.IAnnotation;
import signature.model.IAnnotationField;
import signature.model.IApi;
import signature.model.IArrayType;
import signature.model.IClassDefinition;
import signature.model.IClassReference;
import signature.model.IEnumConstant;
import signature.model.IPackage;
import signature.model.IPrimitiveType;
import signature.model.ITypeReference;
import signature.model.Kind;
import signature.model.impl.SigClassReference;
import signature.model.impl.SigPrimitiveType;
import signature.model.util.ModelUtil;

public abstract class ConvertAnnotationTest extends AbstractConvertTest {

    // Tests whether an annotation declaration element may be annotated with 
    // the declared annotation.
    @Test
    public void convertAnnotationDefinition1() throws IOException {
        CompilationUnit classSrc = new CompilationUnit("a.ToDo", 
                "package a; " +
                "public @interface ToDo {" +
                "    @ToDo(name=\"ToDo\")" +
                "   String name() default \"nobody\";" +
                "}");
        IApi api = convert(classSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "ToDo");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        assertEquals(0, sigClass.getConstructors().size());
        assertEquals(0, sigClass.getMethods().size());
        assertEquals(0, sigClass.getFields().size());
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("name", field.getName());
        assertEquals("String", ((IClassReference)field.getType()).getClassDefinition().getName());
        assertEquals("nobody", field.getDefaultValue());
        Set<IAnnotation> annotations = field.getAnnotations();
        assertEquals(1, annotations.size());
        IAnnotation annotation = annotations.iterator().next();
        assertSame(sigClass, annotation.getType().getClassDefinition());
        // TODO TEST add additional assert for annotation.getElements()
    }

    // Tests whether an annotation declaration may be annotated with 
    // the declared annotation.
    @Test
    public void convertAnnotationDefinition2() throws IOException {
        CompilationUnit classSrc = new CompilationUnit("a.ToDo", 
                "package a; " +
                "@ToDo\n" +
                "public @interface ToDo {" +
                "   String name() default \"nobody\";" +
                "}");
        IApi api = convert(classSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "ToDo");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        assertEquals(0, sigClass.getConstructors().size());
        assertEquals(0, sigClass.getMethods().size());
        assertEquals(0, sigClass.getFields().size());
        Set<IAnnotation> annotations = sigClass.getAnnotations();
        assertEquals(1, annotations.size());
        IAnnotation annotation = annotations.iterator().next();
        assertSame(sigClass, ((SigClassReference)annotation.getType()).getClassDefinition());
        assertEquals(0, annotation.getElements().size());
    }

    @Test
    public void convertAnnotationDefinition3() throws IOException {
        CompilationUnit classSrc = new CompilationUnit("a.ToDo", 
                "package a; " +
                "public @interface ToDo {" +
                "   String name() default \"nobody\";" +
                "   int num() default 3;" +
                "}");
        IApi api = convert(classSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "ToDo");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        assertEquals(0, sigClass.getConstructors().size());
        assertEquals(0, sigClass.getMethods().size());
        assertEquals(0, sigClass.getFields().size());
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(2, annotationFields.size());
        IAnnotationField name = ModelUtil.getAnnotationField(sigClass, "name");
        IAnnotationField num = ModelUtil.getAnnotationField(sigClass, "num");
        
        assertEquals("name", name.getName());
        assertEquals("num", num.getName());
        
        assertEquals("nobody", name.getDefaultValue());
        assertEquals(3, num.getDefaultValue());
    }
    
    
    // tests whether default int value has the correct type and defaultValue
    @Test
    public void testAnnotationDefaultsInt1() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a; " +
                "public @interface A {" +
                "    int value() default 1;" +
                "}");
        IApi api = convert(src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField annotationField = annotationFields.iterator().next();
        assertEquals("value", annotationField.getName());
        assertTrue(annotationField.getType() instanceof IPrimitiveType);
        IPrimitiveType annotationFieldType = (IPrimitiveType)annotationField.getType();
        assertSame(SigPrimitiveType.INT_TYPE, annotationFieldType);
        
        assertTrue(annotationField.getDefaultValue() instanceof Integer);
        Integer defaultValue = (Integer)annotationField.getDefaultValue();
        assertEquals(1, defaultValue.intValue());
    }

    // tests whether default int[] value has the correct type and defaultValue
    @Test
    public void testAnnotationDefaultsInt2() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a; " +
                "public @interface A {" +
                "    int[] value() default {};" +
                "}");
        IApi api = convert(src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("value", field.getName());
        
        assertTrue(field.getType() instanceof IArrayType);
        assertTrue(field.getDefaultValue() instanceof Object[]);
        assertSame(field.getDefaultValue().getClass(), Object[].class);
        Object[] defaultValue = (Object[])field.getDefaultValue();
        assertEquals(0, defaultValue.length);
    }

    // tests whether default int[] value has the correct type and defaultValue
    @Test
    public void testAnnotationDefaultsInt3() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a; " +
                "public @interface A {" +
                "    int[] value() default {1,2};" +
                "}");
        IApi api = convert(src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("value", field.getName());
        
        assertTrue(field.getType() instanceof IArrayType);
        assertTrue(field.getDefaultValue() instanceof Object[]);
        assertSame(field.getDefaultValue().getClass(), Object[].class);
        Object[] defaultValue = (Object[])field.getDefaultValue();
        assertEquals(2, defaultValue.length);
        
        Object defaultValue0 = defaultValue[0];
        assertTrue(defaultValue0 instanceof Integer);
        Integer defaultValue0int = (Integer)defaultValue0;
        assertEquals(1, defaultValue0int.intValue());

        Object defaultValue1 = defaultValue[1];
        assertTrue(defaultValue1 instanceof Integer);
        Integer defaultValue1int = (Integer)defaultValue1;
        assertEquals(2, defaultValue1int.intValue());
    }


    // tests whether default double value has the correct type and defaultValue
    @Test
    public void testAnnotationDefaultsDouble1() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a; " +
                "public @interface A {" +
                "    double value() default 1;" +
                "}");
        IApi api = convert(src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField annotationField = annotationFields.iterator().next();
        assertEquals("value", annotationField.getName());
        assertTrue(annotationField.getType() instanceof IPrimitiveType);
        IPrimitiveType annotationFieldType = (IPrimitiveType)annotationField.getType();
        assertSame(SigPrimitiveType.DOUBLE_TYPE, annotationFieldType);
        
        
        assertTrue(annotationField.getDefaultValue() instanceof Double);
        Double defaultValue = (Double)annotationField.getDefaultValue();
        assertEquals(1.0, defaultValue.doubleValue());
    }

    // tests whether default int[] value has the correct type and defaultValue
    @Test
    public void testAnnotationDefaultsDouble2() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a; " +
                "public @interface A {" +
                "    double[] value() default {};" +
                "}");
        IApi api = convert(src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("value", field.getName());
        
        assertTrue(field.getType() instanceof IArrayType);
        assertTrue(field.getDefaultValue() instanceof Object[]);
        assertSame(field.getDefaultValue().getClass(), Object[].class);
        Object[] defaultValue = (Object[])field.getDefaultValue();
        assertEquals(0, defaultValue.length);
    }

    // tests whether default int[] value has the correct type and defaultValue
    @Test
    public void testAnnotationDefaultsDouble3() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a; " +
                "public @interface A {" +
                "    double[] value() default {1,2.5};" +
                "}");
        IApi api = convert(src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("value", field.getName());
        
        assertTrue(field.getType() instanceof IArrayType);
        assertTrue(field.getDefaultValue() instanceof Object[]);
        assertSame(field.getDefaultValue().getClass(), Object[].class);
        Object[] defaultValue = (Object[])field.getDefaultValue();
        assertEquals(2, defaultValue.length);
        
        Object defaultValue0 = defaultValue[0];
        assertTrue(defaultValue0 instanceof Double);
        Double defaultValue0int = (Double)defaultValue0;
        assertEquals(1, defaultValue0int.doubleValue());

        Object defaultValue1 = defaultValue[1];
        assertTrue(defaultValue1 instanceof Double);
        Double defaultValue1int = (Double)defaultValue1;
        assertEquals(2.5, defaultValue1int.doubleValue());
    }


    // tests whether default enum value has the correct type
    @Test
    public void testAnnotationDefaultsEnum1() throws IOException {
        CompilationUnit src1 = new CompilationUnit("a.A", 
                "package a; " +
                "public @interface A {" +
                "    Kind value() default Kind.TWO;" +
                "}");
        CompilationUnit src2 = new CompilationUnit("a.Kind", 
                "package a; " +
                "public enum Kind {" +
                "    ONE, TWO, THREE" +
                "}");
        IApi api = convert(src1, src2);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("value", field.getName());
        assertEquals("Kind", ((IClassReference)field.getType()).getClassDefinition().getName());
        assertTrue(field.getDefaultValue() instanceof IEnumConstant);
        IEnumConstant defaultValue = (IEnumConstant)field.getDefaultValue();
//        assertEquals(1, defaultValue.getOrdinal());
        assertEquals("TWO", defaultValue.getName());
        
        IClassDefinition enumClass = ModelUtil.getClass(sigPackage, "Kind");
        assertTrue(enumClass.getKind() == Kind.ENUM);
        IEnumConstant enumTWO = null;
        Set<IEnumConstant> enumConstants = enumClass.getEnumConstants();
        for(IEnumConstant c : enumConstants){
            if("TWO".equals(c.getName())) enumTWO = c;
        }
        assertSame(enumTWO, defaultValue);
    }

    // tests whether default enum value has the correct type
    @Test
    public void testAnnotationDefaultsEnum2() throws IOException {
        CompilationUnit src1 = new CompilationUnit("a.A", 
                "package a; " +
                "public @interface A {" +
                "    Kind[] value() default {};" +
                "}");
        CompilationUnit src2 = new CompilationUnit("a.Kind", 
                "package a; " +
                "public enum Kind {" +
                "    ONE, TWO, THREE" +
                "}");
        IApi api = convert(src1, src2);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("value", field.getName());
        assertTrue(field.getType() instanceof IArrayType);
        assertTrue(field.getDefaultValue() instanceof Object[]);
        assertSame(field.getDefaultValue().getClass(), Object[].class);
        Object[] defaultValue = (Object[])field.getDefaultValue();
        assertEquals(0, defaultValue.length);
    }

    // tests whether default enum value has the correct type
    @Test
    public void testAnnotationDefaultsEnum3() throws IOException {
        CompilationUnit src1 = new CompilationUnit("a.A", 
                "package a; " +
                "public @interface A {" +
                "    Kind[] value() default {Kind.ONE,Kind.TWO};" +
                "}");
        CompilationUnit src2 = new CompilationUnit("a.Kind", 
                "package a; " +
                "public enum Kind {" +
                "    ONE, TWO, THREE" +
                "}");
        IApi api = convert(src1, src2);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("value", field.getName());
        
        assertTrue(field.getType() instanceof IArrayType);
        assertTrue(field.getDefaultValue() instanceof Object[]);
        assertSame(field.getDefaultValue().getClass(), Object[].class);
        Object[] defaultValue = (Object[])field.getDefaultValue();
        assertEquals(2, defaultValue.length);
        
        IClassDefinition enumClass = ModelUtil.getClass(sigPackage, "Kind");
        assertTrue(enumClass.getKind() == Kind.ENUM);
        IEnumConstant enumONE = null;
        IEnumConstant enumTWO = null;
        Set<IEnumConstant> enumConstants = enumClass.getEnumConstants();
        for(IEnumConstant c : enumConstants){
            if("ONE".equals(c.getName())) enumONE = c;
            if("TWO".equals(c.getName())) enumTWO = c;
        }

        Object defaultValue0 = defaultValue[0];
        assertTrue(defaultValue0 instanceof IEnumConstant);
        IEnumConstant defaultValue0enum = (IEnumConstant)defaultValue0;
//        assertEquals(0, defaultValue0enum.getOrdinal());
        assertEquals("ONE", defaultValue0enum.getName());
        assertSame(enumONE, defaultValue0enum);

        Object defaultValue1 = defaultValue[1];
        assertTrue(defaultValue1 instanceof IEnumConstant);
        IEnumConstant defaultValue1enum = (IEnumConstant)defaultValue1;
//        assertEquals(1, defaultValue1enum.getOrdinal());
        assertEquals("TWO", defaultValue1enum.getName());
        assertSame(enumTWO, defaultValue1enum);
    }

    
    @Test
    public void testAnnotationDefaultsAnnotation1() throws IOException {
        CompilationUnit src1 = new CompilationUnit("a.A", 
                "package a; " +
                "public @interface A {" +
                "    AA aa() default @AA;" +
                "}");
        CompilationUnit src2 = new CompilationUnit("a.AA", 
                "package a; " +
                "public @interface AA {}");
        IApi api = convert(src1, src2);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("aa", field.getName());
        IClassReference fieldType = (IClassReference)field.getType();
        assertEquals("AA", fieldType.getClassDefinition().getName());
        
        IClassDefinition aaType = ModelUtil.getClass(sigPackage, "AA");
        assertTrue(aaType.getKind() == Kind.ANNOTATION);
        assertSame(aaType, fieldType.getClassDefinition());
        
        assertTrue(field.getDefaultValue() instanceof IAnnotation);
        IAnnotation defaultValue = (IAnnotation)field.getDefaultValue();
        assertEquals(0, defaultValue.getElements().size());
        assertSame(aaType, defaultValue.getType().getClassDefinition());
    }

    @Test
    public void testAnnotationDefaultsAnnotation2() throws IOException {
        CompilationUnit src1 = new CompilationUnit("a.A", 
                "package a; " +
                "public @interface A {" +
                "    AA[] aa() default {};" +
                "}");
        CompilationUnit src2 = new CompilationUnit("a.AA", 
                "package a; " +
                "public @interface AA {}");
        
        IApi api = convert(src1, src2);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("aa", field.getName());
        
        assertTrue(field.getType() instanceof IArrayType);
        assertTrue(field.getDefaultValue() instanceof Object[]);
        assertSame(field.getDefaultValue().getClass(), Object[].class);
        Object[] defaultValue = (Object[])field.getDefaultValue();
        assertEquals(0, defaultValue.length);
    }

    @Test
    public void testAnnotationDefaultsAnnotation3() throws IOException {
        CompilationUnit src1 = new CompilationUnit("a.A", 
                "package a; " +
                "public @interface A {" +
                "    AA[] aa() default {@AA,@AA};" +
                "}");
        CompilationUnit src2 = new CompilationUnit("a.AA", 
                "package a; " +
                "public @interface AA {}");
        
        IApi api = convert(src1, src2);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("aa", field.getName());
        
        assertTrue(field.getType() instanceof IArrayType);
        assertTrue(field.getDefaultValue() instanceof Object[]);
        assertSame(field.getDefaultValue().getClass(), Object[].class);
        Object[] defaultValue = (Object[])field.getDefaultValue();
        assertEquals(2, defaultValue.length);
        
        IClassDefinition aaType = ModelUtil.getClass(sigPackage, "AA");
        assertTrue(aaType.getKind() == Kind.ANNOTATION);
        
        Object defaultValue0 = defaultValue[0];
        assertTrue(defaultValue0 instanceof IAnnotation);
        IAnnotation defaultValue0ann = (IAnnotation)defaultValue0;
        assertSame(aaType, defaultValue0ann.getType().getClassDefinition());
        assertEquals(0, defaultValue0ann.getElements().size());
        
        Object defaultValue1 = defaultValue[1];
        assertTrue(defaultValue1 instanceof IAnnotation);
        IAnnotation defaultValue1ann = (IAnnotation)defaultValue1;
        assertSame(aaType, defaultValue1ann.getType().getClassDefinition());
        assertEquals(0, defaultValue1ann.getElements().size());  
    }



    @Test
    public void testAnnotationDefaultsString1() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a;" +
                "public @interface A {" +
                "    String str() default \"xxx\";" +
                "}");
        IApi api = convert(src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("str", field.getName());
        
        assertTrue(field.getType() instanceof IClassReference);
        assertEquals("String", ((IClassReference)field.getType()).getClassDefinition().getName());
        
        assertTrue(field.getDefaultValue() instanceof String);
        String defaultValue = (String)field.getDefaultValue();
        assertEquals("xxx", defaultValue);
    }

    @Test    
    public void testAnnotationDefaultsString2() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a;" +
                "public @interface A {" +
                "    String[] str() default {};" +
                "}");
        IApi api = convert(src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("str", field.getName());
        
        assertTrue(field.getType() instanceof IArrayType);
        Object defaultValue = field.getDefaultValue();
        assertTrue(defaultValue instanceof Object[]);
        assertSame(defaultValue.getClass(), Object[].class);
        assertEquals(0, ((Object[])defaultValue).length);
    }

    @Test    
    public void testAnnotationDefaultsString3() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a;" +
                "public @interface A {" +
                "    String[] str() default {\"ONE\", \"TWO\"};" +
                "}");
        IApi api = convert(src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        assertTrue(sigClass.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = sigClass.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("str", field.getName());
        
        assertTrue(field.getType() instanceof IArrayType);
        assertTrue(field.getDefaultValue() instanceof Object[]);
        assertSame(field.getDefaultValue().getClass(), Object[].class);
        Object[] defaultValue = (Object[])field.getDefaultValue();
        assertEquals(2, defaultValue.length);
        
        Object defaultValue0 = defaultValue[0];
        assertTrue(defaultValue0 instanceof String);
        String defaultValue0str = (String)defaultValue0;
        assertEquals("ONE", defaultValue0str);

        Object defaultValue1 = defaultValue[1];
        assertTrue(defaultValue1 instanceof String);
        String defaultValue1str = (String)defaultValue1;
        assertEquals("TWO", defaultValue1str);
    }

    @Test
    public void testAnnotationDefaultsClass1() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a;" +
                "public @interface A {" +
                "    Class cc() default A.class;" +
                "}");
        IApi api = convert(src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition aType = ModelUtil.getClass(sigPackage, "A");
        assertTrue(aType.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = aType.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("cc", field.getName());
        
        assertTrue(field.getType() instanceof IClassReference);
        assertEquals("Class", ((IClassReference)field.getType()).getClassDefinition().getName());
        Object defaultValue = field.getDefaultValue();
        assertSame(aType, ((IClassReference)defaultValue).getClassDefinition());
    }

    @Test
    public void testAnnotationDefaultsClass2() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a;" +
                "public @interface A {" +
                "    Class cc() default void.class;" +
                "}");
        IApi api = convert(src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition aType = ModelUtil.getClass(sigPackage, "A");
        assertTrue(aType.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = aType.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("cc", field.getName());
        
        assertTrue(field.getType() instanceof IClassReference);
        assertEquals("Class", ((IClassReference)field.getType()).getClassDefinition().getName());
        
        Object defaultValue = field.getDefaultValue();
        assertTrue(defaultValue instanceof IPrimitiveType);
        assertEquals("void", ((IPrimitiveType)defaultValue).getName());
        assertSame(SigPrimitiveType.VOID_TYPE, defaultValue);
    }

    @Test
    public void testAnnotationDefaultsClass3() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a;" +
                "public @interface A {" +
                "    Class[] cc() default {};" +
                "}");
        IApi api = convert(src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition aType = ModelUtil.getClass(sigPackage, "A");
        assertTrue(aType.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = aType.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("cc", field.getName());
        
        assertTrue(field.getDefaultValue() instanceof Object[]);
        assertSame(field.getDefaultValue().getClass(), Object[].class);
        Object[] defaultValue = (Object[])field.getDefaultValue();
        assertEquals(0, defaultValue.length);
    }

    @Test
    public void testAnnotationDefaultsClass4() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a;" +
                "public @interface A {" +
                "    Class[] cc() default {A.class, void.class};" +
                "}");
        IApi api = convert(src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition aType = ModelUtil.getClass(sigPackage, "A");
        assertTrue(aType.getKind() == Kind.ANNOTATION);
        Set<IAnnotationField> annotationFields = aType.getAnnotationFields();
        assertEquals(1, annotationFields.size());
        IAnnotationField field = annotationFields.iterator().next();
        assertEquals("cc", field.getName());
        
        assertTrue(field.getDefaultValue() instanceof Object[]);
        assertSame(field.getDefaultValue().getClass(), Object[].class);
        Object[] defaultValue = (Object[])field.getDefaultValue();
        assertEquals(2, defaultValue.length);
        
        Object defaultValue0 = defaultValue[0];
        assertTrue(defaultValue0 instanceof ITypeReference);
        ITypeReference defaultValue0type = (ITypeReference)defaultValue0;
        assertSame(aType, ((IClassReference)defaultValue0type).getClassDefinition());

        Object defaultValue1 = defaultValue[1];
        assertTrue(defaultValue1 instanceof ITypeReference);
        assertTrue(defaultValue1 instanceof IPrimitiveType);
        IPrimitiveType defaultValue1type = (IPrimitiveType)defaultValue1;
        assertEquals("void", defaultValue1type.getName());
        assertSame(SigPrimitiveType.VOID_TYPE, defaultValue1type);
    }
    
    @Test
    public void testAnnotationRetentionRuntime() throws IOException {
        CompilationUnit retention = new CompilationUnit("java.lang.annotation.Retention", 
                "package java.lang.annotation; " +
                "@Retention(RetentionPolicy.RUNTIME) " + 
                //"@Target(ElementType.ANNOTATION_TYPE) " + 
                "public @interface Retention { " +
                "   RetentionPolicy value() default RetentionPolicy.CLASS; " +
                "}");
        CompilationUnit retentionPolicy = new CompilationUnit("java.lang.annotation.RetentionPolicy", 
                "package java.lang.annotation; " +
                "public enum RetentionPolicy { " + 
                "    SOURCE," + 
                "    CLASS," + 
                "    RUNTIME" + 
                "}");
        CompilationUnit anno = new CompilationUnit("a.A", 
                "package a;" +
                "@java.lang.annotation.Retention(value=java.lang.annotation.RetentionPolicy.SOURCE)" +
                "public @interface A{}");
        CompilationUnit cla = new CompilationUnit("a.B",  
                "package a;" +
                "@A public class B{}");
        IApi api = convert(anno, cla, retention, retentionPolicy);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition aType = ModelUtil.getClass(sigPackage, "B");
        assertTrue(aType.getAnnotations().isEmpty());
    }
    
    @Test
    public void testAnnotationRetentionClass() throws IOException {
        CompilationUnit retention = new CompilationUnit("java.lang.annotation.Retention", 
                "package java.lang.annotation; " +
                "@Retention(RetentionPolicy.RUNTIME) " + 
                //"@Target(ElementType.ANNOTATION_TYPE) " + 
                "public @interface Retention { " +
                "   RetentionPolicy value() default RetentionPolicy.CLASS; " +
                "}");
        CompilationUnit retentionPolicy = new CompilationUnit("java.lang.annotation.RetentionPolicy", 
                "package java.lang.annotation; " +
                "public enum RetentionPolicy { " + 
                "    SOURCE," + 
                "    CLASS," + 
                "    RUNTIME" + 
                "}");
        CompilationUnit anno = new CompilationUnit("a.A", 
                "package a;" +
                "@java.lang.annotation.Retention(value=java.lang.annotation.RetentionPolicy.CLASS)" +
                "public @interface A{}");
        CompilationUnit cla = new CompilationUnit("a.B",  
                "package a;" +
                "@A public class B{}");
        IApi api = convert(anno, cla, retention, retentionPolicy);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition aType = ModelUtil.getClass(sigPackage, "B");
        assertEquals(1, aType.getAnnotations().size());
    }

}
