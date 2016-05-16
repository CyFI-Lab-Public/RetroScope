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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import dex.reader.util.JavaSource;
import dex.structure.DexAnnotation;
import dex.structure.DexAnnotationAttribute;
import dex.structure.DexClass;
import dex.structure.DexEncodedValue;
import dex.structure.DexField;
import dex.structure.DexFile;
import dex.structure.DexMethod;
import dex.structure.DexParameter;


public class DexFileReaderTests extends DexTestsCommon {

    private static final String LDALVIK_ANNOTATION_SIGNATURE = "Ldalvik/annotation/Signature;";
    
    
    JavaSource A = new JavaSource("a.b.c.A", 
            "package a.b.c; public class A{ public void get() {}}"
    );
    
    @Test
    public void testA() throws IOException {
        DexFile dexFile = javaToDexUtil.getFrom(A);
        assertEquals(1, dexFile.getDefinedClasses().size());
        @SuppressWarnings("unused")
        DexClass class1 = getClass(dexFile, "La/b/c/A;");
        System.out.println(dexFile);
    }
    
    
    JavaSource T0 = new JavaSource("T0",
            "public class T0 {" + 
            "    public int publicIntField;" + 
            "    protected long protectedLongField;" + 
            "    short defaultShortField;" + 
            "    private double privateDoubleField;" + 
            "    " + 
            "    public String publicStringMethodInt(int a){ return \"bla\"; }" + 
            "    protected String protectedStringMethodInt(int a){ return \"bla\"; }" + 
            "    String defaultStringMethodInt(int a){ return \"bla\"; }" + 
            "    private String privateStringMethodInt(int a){ return \"bla\"; }" + 
            "}"     
    );
    
    /**
     * Tests parsing a simple class.
     */
    @Test 
    public void testT0() throws IOException {
        
        DexFile dexFile = javaToDexUtil.getFrom(T0);
        assertEquals(1, dexFile.getDefinedClasses().size());
        DexClass clazz = dexFile.getDefinedClasses().get(0);
        assertEquals("LT0;", clazz.getName());
        assertPublic(clazz);
        //fields
        assertEquals(4, clazz.getFields().size());
        DexField field = getField(clazz, "publicIntField");
        assertPublic(field);
        field = getField(clazz, "protectedLongField");
        assertProtected(field);
        field = getField(clazz, "defaultShortField");
        assertDefault(field);
        field = getField(clazz, "privateDoubleField");
        assertPrivate(field);
        //methods
        DexMethod method = getMethod(clazz, "publicStringMethodInt", "I");
        assertPublic(method);
        method = getMethod(clazz, "protectedStringMethodInt", "I");/** a.b.C */
        assertProtected(method);
        method = getMethod(clazz, "defaultStringMethodInt", "I");
        assertDefault(method);
        method = getMethod(clazz, "privateStringMethodInt", "I");
        assertPrivate(method);
    }
    
    JavaSource T1 = new JavaSource( "T1","public class T1 extends T0 {}" );

    
    private static Set<JavaSource> toSet(JavaSource...javaSources){
        return new HashSet<JavaSource>(Arrays.asList(javaSources));
    }
    
    private static Set<String> toStringSet(JavaSource... javaSources) {
        Set<String> names = new HashSet<String>();
        for (JavaSource javaSource : javaSources) {
            names.add(javaSource.getName());
        }
        
        return names;
    }
    
    private static Set<String> toStringSet(String... javaSourceName) {
        return new HashSet<String>(Arrays.asList(javaSourceName));
    }
    
    /**
     * Tests parsing a simple sub class.
     * @throws IOException
     */
    @Test 
    public void testT1() throws IOException {
        DexFile dexFile = javaToDexUtil.getFrom(toSet(T1, T0), toStringSet(T1));
        assertEquals(1, dexFile.getDefinedClasses().size());
        DexClass clazz = dexFile.getDefinedClasses().get(0);
        assertEquals("LT1;", clazz.getName());
        assertPublic(clazz);
        assertEquals("LT0;", clazz.getSuperClass());
    }
    
    /**
     * Tests parsing T0 and T1 from same dex file.
     * 
     * @throws IOException
     */
    @Test 
    public void testT0_T1() throws IOException {
        DexFile dexFile = javaToDexUtil.getFrom(T1, T0);
        assertEquals(2, dexFile.getDefinedClasses().size());
        
        DexClass T0 = getClass(dexFile, "LT0;");
        assertPublic(T0);
        
        DexClass T1 = getClass(dexFile, "LT1;");
        assertPublic(T1);
        
        assertEquals(T1.getSuperClass(), T0.getName());
    }
    
    static final JavaSource A0 = new JavaSource("A0", 
    "import java.lang.annotation.*;" + 
    "@Retention(RetentionPolicy.RUNTIME)" + 
    "@Target(ElementType.TYPE)" + 
    "public @interface A0 {}"
     );
    
    /**
     * Tests parsing Annotation Declaration.
     */
    @Test 
    public void testA0() throws IOException {
        DexFile dexFile = javaToDexUtil.getFrom(A0);
        assertEquals(1, dexFile.getDefinedClasses().size());
        
        DexClass A0 = getClass(dexFile, "LA0;");
        assertPublic(A0);
        assertEquals(2, A0.getAnnotations().size());
    }
    
    
    static final JavaSource T3 = new JavaSource("T3",
    "import java.io.*;" + 
    "@A0 " + 
    "public final class T3 {}"        
    );
    
    
    /**
     * Tests parsing Annotated Class.
     */
    @Test 
    public void testA0_T3() throws IOException {
        DexFile dexFile = javaToDexUtil.getFrom(T3, A0);
        assertEquals(2, dexFile.getDefinedClasses().size());
        
        DexClass T3 = getClass(dexFile, "LT3;");
        assertPublic(T3);
        assertEquals(1, T3.getAnnotations().size());
        
        DexAnnotation annotation = getAnnotation(T3, "LA0;");
        
        DexClass A0 = getClass(dexFile, "LA0;");
        
        assertEquals(A0.getName(), annotation.getTypeName());
    }
    
    
    static final JavaSource G0 = new JavaSource("G0","public class G0<T>{}");
    
    /**
     * Tests parsing Generic Type.
     */
    @Test 
    public void testG0() throws IOException {
        DexFile dexFile = javaToDexUtil.getFrom(G0);
        assertEquals(1, dexFile.getDefinedClasses().size());
        DexClass G0 = getClass(dexFile, "LG0;");
        assertPublic(G0);
        DexAnnotation sig = getAnnotation(G0, LDALVIK_ANNOTATION_SIGNATURE);
        assertEquals(1, sig.getAttributes().size());
        DexAnnotationAttribute dexAnnotationValue = sig.getAttributes().get(0);
        assertNotNull(dexAnnotationValue.getEncodedValue());
        Object value = dexAnnotationValue.getEncodedValue().getValue();
        assertTrue(value instanceof List);
        StringBuilder builder = new StringBuilder();
        for (Object o : (List<?>)value) {
            builder.append(((DexEncodedValue)o).getValue());
        }
        //FIXME verify
        assertEquals("<T:Ljava/lang/Object;>Ljava/lang/Object;", builder.toString());
    }
    
    static final JavaSource G1 = new JavaSource("G1","public class G1<T extends G1>{}");
    
    /**
     * Tests parsing Generic Type.
     */
    @Test 
    public void testG1() throws IOException {
        DexFile dexFile = javaToDexUtil.getFrom(G1);
        assertEquals(1, dexFile.getDefinedClasses().size());
        DexClass G1 = getClass(dexFile, "LG1;");
        assertPublic(G1);
        DexAnnotation sig = getAnnotation(G1, LDALVIK_ANNOTATION_SIGNATURE);
        assertEquals(1, sig.getAttributes().size());
        DexAnnotationAttribute dexAnnotationValue = sig.getAttributes().get(0);
        assertNotNull(dexAnnotationValue.getEncodedValue());
        Object value = dexAnnotationValue.getEncodedValue().getValue();
        assertTrue(value instanceof List);
        StringBuilder builder = new StringBuilder();
        for (Object o : (List<?>)value) {
            builder.append(((DexEncodedValue)o).getValue());
        }
        //FIXME verify
        assertEquals("<T:LG1;>Ljava/lang/Object;", builder.toString());
    }
    

    
    static final JavaSource I0 = new JavaSource("I0",
    "import java.io.Serializable;" + 
    "public interface I0 extends Serializable {}"
    );
    
    /**
     * Tests parsing Interface Type.
     */
    @Test  
    public void testI0() throws IOException {
        DexFile dexFile = javaToDexUtil.getFrom(I0);
        assertEquals(1, dexFile.getDefinedClasses().size());
        DexClass I0 = getClass(dexFile, "LI0;");
        assertPublic(I0);
        assertTrue(Modifier.isInterface(I0.getModifiers()));
        assertEquals(1,  I0.getInterfaces().size());
        assertEquals("Ljava/io/Serializable;",  I0.getInterfaces().get(0));
    }
    
    
    static final JavaSource Outer0 = new JavaSource("Outer0",
    "public class Outer0 {" + 
    "    static class StaticInner {}" + 
    "    class Inner{}" + 
    "}"
    );
    
    /**
     * Tests parsing Interface Type.
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Test  
    public void testOuter0() throws IOException {
        DexFile dexFile = javaToDexUtil.getFrom(toSet(Outer0), toStringSet("Outer0", "Outer0$Inner", "Outer0$StaticInner"));
        assertEquals(3, dexFile.getDefinedClasses().size());
        DexClass Outer0 = getClass(dexFile, "LOuter0;");
        DexAnnotation sig = getAnnotation(Outer0, "Ldalvik/annotation/MemberClasses;");
        assertEquals(1, sig.getAttributes().size());
        DexAnnotationAttribute dexAnnotationValue = sig.getAttributes().get(0);
        assertNotNull(dexAnnotationValue.getEncodedValue());
        List<DexEncodedValue> values = (List<DexEncodedValue>) dexAnnotationValue.getEncodedValue().getValue();
        Set<String> innerTypeNames = new HashSet<String>();
        for (DexEncodedValue value : values) {
            innerTypeNames.add((String) value.getValue());
        }
        DexClass inner = getClass(dexFile, "LOuter0$Inner;");
        DexClass staticInner = getClass(dexFile, "LOuter0$StaticInner;");
        assertTrue(innerTypeNames.contains(inner.getName()));
        assertTrue(innerTypeNames.contains(staticInner.getName()));
    }
    
    static final JavaSource parameterAnnotation = new JavaSource("A",
            "public class A {" + 
            "  void m(@Deprecated int a) {}" + 
            "}");

    /**
     * Tests parameter annotation.
     * 
     * @throws IOException
     */
    @Test
    public void testParameterAnnotation() throws IOException {
        DexFile dexFile = javaToDexUtil.getFrom(parameterAnnotation);
        assertEquals(1, dexFile.getDefinedClasses().size());
        DexClass A = getClass(dexFile, "LA;");

        DexMethod method = getMethod(A, "m", "I");
        assertEquals(1, method.getParameters().size());
        DexParameter dexParameter = method.getParameters().get(0);
        assertEquals("I", dexParameter.getTypeName());

        assertEquals(1, dexParameter.getAnnotations().size());
        DexAnnotation annotation = dexParameter.getAnnotations().iterator().next();
        assertEquals("Ljava/lang/Deprecated;", annotation.getTypeName());
    }
    
    @Test 
    public void testEnum() throws IOException {
        JavaSource source = new JavaSource("E", "public enum E { A,B; public static final E C = null; }");
        DexFile dexFile = javaToDexUtil.getFrom(source);
        assertEquals(1, dexFile.getDefinedClasses().size());
        DexClass E = getClass(dexFile, "LE;");
        System.out.println(E);
        System.out.println(E.getFields());
    }

    /**
     * Tests parsing of huge dex file. 
     * @throws IOException
     */
    @Test 
    public void testAllReader() throws IOException {
        FileWriter w = new FileWriter("dex/classes.out.dex");
        DexFileReader dexReader = new DexFileReader();
        DexFile dexFile = dexReader.read(new DexBuffer("dex/classes.dex"));
        TypeFormatter formatter = new TypeFormatter();
        w.append(formatter.formatDexFile(dexFile));
        w.flush();
        w.close();
        assertTrue(true);
    }
    
    /**
     * Tests parsing of huge dex file. 
     * @throws IOException
     */
    @Test 
    public void testAllReader0() throws IOException {
        FileWriter w = new FileWriter("dex/classes0.out.dex");
        DexFileReader dexReader = new DexFileReader();
        DexFile dexFile = dexReader.read(new DexBuffer("dex/classes0.dex"));
        TypeFormatter formatter = new TypeFormatter();
        w.append(formatter.formatDexFile(dexFile));
        w.flush();
        w.close();
        assertTrue(true);
    }
    
}
