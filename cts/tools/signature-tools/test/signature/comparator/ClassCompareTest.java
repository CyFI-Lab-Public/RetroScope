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

package signature.comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import signature.comparator.util.AbstractComparatorTest;
import signature.compare.model.IAnnotationDelta;
import signature.compare.model.IApiDelta;
import signature.compare.model.IClassDefinitionDelta;
import signature.compare.model.DeltaType;
import signature.compare.model.ITypeReferenceDelta;
import signature.converter.util.CompilationUnit;
import signature.model.IApi;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Set;

public abstract class ClassCompareTest extends AbstractComparatorTest {

    @Test
    public void compareEqualClasses() throws IOException{
         CompilationUnit A = new CompilationUnit("a.A", 
                    "package a; " +
                    "public class A {}");
         CompilationUnit B = new CompilationUnit("a.B", 
                    "package a; " +
                    "public class B {}");
          IApi fromApi = convert(A, B);
          IApi toApi = convert(A, B);
          assertNull(compare(fromApi, toApi));
    }

    @Test
    public void compareMissingClass() throws IOException{
         CompilationUnit A = new CompilationUnit("a.A", 
                    "package a; " +
                    "public class A {}");
         CompilationUnit B = new CompilationUnit("a.B", 
                    "package a; " +
                    "public class B {}");
          IApi fromApi = convert(A, B);
          IApi toApi = convert(A);
          
          IApiDelta delta = compare(fromApi, toApi);
          IClassDefinitionDelta classDelta = getSingleClassDelta(delta);
          assertSame(DeltaType.REMOVED, classDelta.getType());
    }
    
    @Test
    public void compareAddedClass() throws IOException{
         CompilationUnit A = new CompilationUnit("a.A", 
                    "package a; " +
                    "public class A {}");
         CompilationUnit B = new CompilationUnit("a.B", 
                    "package a; " +
                    "public class B {}");
          IApi fromApi = convert(A);
          IApi toApi = convert(A, B);
          
          IApiDelta delta = compare(fromApi, toApi);
          IClassDefinitionDelta classDelta = getSingleClassDelta(delta);
          assertSame(DeltaType.ADDED, classDelta.getType());
    }
    
    @Test
    public void compareAnnotationsOnClass() throws IOException{
         CompilationUnit A = new CompilationUnit("a.A", 
                    "package a; " +
                    "public class A {}");
         CompilationUnit AnnotA = new CompilationUnit("a.A", 
                    "package a; " +
                    "@Deprecated " +
                    "public class A {}");
          IApi fromApi = convert(A);
          IApi toApi = convert(AnnotA);
          
          IApiDelta delta = compare(fromApi, toApi);
          IClassDefinitionDelta classDelta = getSingleClassDelta(delta);
          
          System.out.println(classDelta);
          
          Set<IAnnotationDelta> annotationDeltas = classDelta.getAnnotationDeltas();
          assertEquals(1, annotationDeltas.size());
          
          IAnnotationDelta annotationDelta = annotationDeltas.iterator().next();
          assertSame(DeltaType.ADDED, annotationDelta.getType());
    }
    
    @Test
    public void compareDefaultedAnnotationElementOnClass() throws IOException{
        CompilationUnit annot = new CompilationUnit("a.A", 
                "package a; " +
                "public @interface A {" +
                "  String name() default \"NAME\" ;" + 
                "}");
         CompilationUnit AnnotBDefault = new CompilationUnit("a.B", 
                    "package a; " +
                    "@A " +
                    "public class B {}");
         CompilationUnit AnnotB = new CompilationUnit("a.B", 
                    "package a; " +
                    "@A(name=\"NAME\") " +
                    "public class B {}");
          IApi fromApi = convert(annot, AnnotBDefault);
          IApi toApi = convert(annot, AnnotB);
          assertNull(compare(fromApi, toApi));
    }
    
    @Test
    public void compareSameInterfaces() throws IOException{
         CompilationUnit A = new CompilationUnit("a.A", 
                    "package a; " +
                    "public class A implements Comparable<String>{ " +
                    "  public int compareTo(String another){return 0;}" +
                    "}");
          IApi fromApi = convert(A);
          IApi toApi = convert(A);
          assertNull(compare(fromApi, toApi));
    }
    
    @Test
    public void compareMissingInterface() throws IOException{
         CompilationUnit A0 = new CompilationUnit("a.A", 
                 "package a; " +
                 "public class A implements Cloneable{}");
         CompilationUnit A1 = new CompilationUnit("a.A", 
                 "package a; " +
                 "public class A {}");
          IApi fromApi = convert(A0);
          IApi toApi = convert(A1);
          IApiDelta apiDelta = compare(fromApi, toApi);
          IClassDefinitionDelta classDelta =  getSingleClassDelta(apiDelta);
          assertEquals(1, classDelta.getInterfaceDeltas().size());
          ITypeReferenceDelta<?> interfaceDelta = classDelta.getInterfaceDeltas().iterator().next();
          assertNotNull(interfaceDelta);
    }
    
    @Test
    public void compareMissingGenericInterface0() throws IOException{
        CompilationUnit I = new CompilationUnit("a.I", 
                "package a; " +
                "public interface I<T>{}");
        CompilationUnit B = new CompilationUnit("a.B", 
                "package a; " +
                "public class B implements I<String>{}");
         CompilationUnit A0 = new CompilationUnit("a.A", 
                "package a; " +
                "public class A extends B implements I<String>{}");
         CompilationUnit A1 = new CompilationUnit("a.A", 
                 "package a; " +
                 "public class A extends B {}");
          IApi fromApi = convert(I, B, A0);
          IApi toApi = convert(I, B, A1);
          IApiDelta apiDelta = compare(fromApi, toApi);
          assertNull(apiDelta);
    }
    
    @Test
    public void compareMissingGenericInterface1() throws IOException{
        CompilationUnit I = new CompilationUnit("a.I", 
                "package a; " +
                "public interface I<T>{}");
        CompilationUnit B = new CompilationUnit("a.B", 
                "package a; " +
                "public class B<T> implements I<T>{}");
         CompilationUnit A0 = new CompilationUnit("a.A", 
                "package a; " +
                "public class A<T> extends B<T> implements I<T>{}");
                 //generic declaration of 'T' in I<T> is A<T>
         CompilationUnit A1 = new CompilationUnit("a.A", 
                 "package a; " +
                 "public class A<T> extends B<T> {}");
                 //generic declaration of 'T' in I<T> is B<T>
          IApi fromApi = convert(I, B, A0);
          IApi toApi = convert(I, B, A1);
          IApiDelta apiDelta = compare(fromApi, toApi);
          assertNull(apiDelta);
    }
    
    static interface I<T>{}
    static class B<T> implements I<T>{}
    static class A0<E extends Number> extends B<E> implements I<E>{}
    static class A1<S extends Number> extends B<S>{}
    
    @Test
    public void compareMissingGenericInterfaceReflection() {
        ParameterizedType sC = (ParameterizedType)A0.class.getGenericSuperclass();
        Type[] bounds = ((TypeVariable<?>)sC.getActualTypeArguments()[0]).getBounds();
        Type[] a1Int = A1.class.getGenericInterfaces();
        assertEquals(0,a1Int.length);
    }
    
    @Test
    public void compareInterfaceClosure() throws IOException{
         CompilationUnit I0 = new CompilationUnit("a.I0", 
                 "package a; " +
                 "public interface I0{}");
         CompilationUnit I1 = new CompilationUnit("a.I1", 
                 "package a; " +
                 "public interface I1 extends I0{}");
         CompilationUnit C0 = new CompilationUnit("a.C0", 
                 "package a; " +
                 "public class C0 implements I1{}");
         CompilationUnit C0_I1 = new CompilationUnit("a.C0", 
                 "package a; " +
                 "public class C0 implements I1, I0{}");
          IApi fromApi = convert(I0, I1, C0);
          IApi toApi = convert(I0, I1, C0_I1);
          IApiDelta apiDelta = compare(fromApi, toApi);
          assertNull(apiDelta);
    }
    
    @Test
    public void compareUpperBounds0() throws IOException{
        CompilationUnit Number = new CompilationUnit("a.Number", 
                    "package a; " +
                    "public class Number implements java.io.Serializable{}");
         CompilationUnit I0 = new CompilationUnit("a.I", 
                    "package a; " +
                    "public interface I<T extends Number & java.io.Serializable>{}");
         CompilationUnit I1 = new CompilationUnit("a.I", 
                     "package a; " +
                     "public interface I<T extends Number>{}");
          IApi fromApi = convert(I0,Number);
          IApi toApi = convert(I1,Number);
          IApiDelta apiDelta = compare(fromApi, toApi);
          assertNull(apiDelta);
    }
    
    @Test
    public void compareUpperBounds1() throws IOException{
        CompilationUnit Number = new CompilationUnit("a.Number", 
                    "package a; " +
                    "public class Number {}");
         CompilationUnit I0 = new CompilationUnit("a.I", 
                    "package a; " +
                    "public interface I<T extends Number & java.io.Serializable>{}");
         CompilationUnit I1 = new CompilationUnit("a.I", 
                     "package a; " +
                     "public interface I<T extends Number>{}");
          IApi fromApi = convert(I0,Number);
          IApi toApi = convert(I1,Number);
          IApiDelta apiDelta = compare(fromApi, toApi);
          assertNotNull(apiDelta);
    }
    
    @Test
    public void compareTypeVariables0() throws IOException{
        CompilationUnit C0 = new CompilationUnit("a.C", 
                    "package a; " +
                    "public class C<T,S> {}");
        CompilationUnit C1 = new CompilationUnit("a.C", 
                    "package a; " +
                    "public class C<S,T> {}");
          IApi fromApi = convert(C0);
          IApi toApi = convert(C1);
          IApiDelta apiDelta = compare(fromApi, toApi);
          assertNull(apiDelta);
    }
    
    @Test
    public void compareTypeVariables1() throws IOException{
        CompilationUnit C0 = new CompilationUnit("a.C", 
                    "package a; " +
                    "public class C<T,S> {}");
        CompilationUnit C1 = new CompilationUnit("a.C", 
                    "package a; " +
                    "public class C<T,S,R> {}");
          IApi fromApi = convert(C0);
          IApi toApi = convert(C1);
          IApiDelta apiDelta = compare(fromApi, toApi);
          assertNotNull(apiDelta);
    }
    
    @Test
    public void compareTypeVariables2() throws IOException{
        CompilationUnit C0 = new CompilationUnit("a.C", 
                    "package a; " +
                    "public class C<T,S> {" +
                    "  public void m(T t, S s){} " +
                    "}");
        CompilationUnit C1 = new CompilationUnit("a.C", 
                    "package a; " +
                    "public class C<S,T> {" +
                    "  public void m(S s, T t){} " +
                    "}");
          IApi fromApi = convert(C0);
          IApi toApi = convert(C1);
          IApiDelta apiDelta = compare(fromApi, toApi);
          assertNull(apiDelta);
    }
    
    @Test
    public void compareTypeVariables3() throws IOException{
        CompilationUnit C0 = new CompilationUnit("a.C", 
                    "package a; " +
                    "public class C<T,S> {" +
                    "  public void m(T t, S s){} " +
                    "}");
        CompilationUnit C1 = new CompilationUnit("a.C", 
                    "package a; " +
                    "public class C<S,T> {" +
                    "  public void m(T t, S s){} " +
                    "}");
          IApi fromApi = convert(C0);
          IApi toApi = convert(C1);
          IApiDelta apiDelta = compare(fromApi, toApi);
          assertNotNull(apiDelta);
    }
    
    @Test
    public void compareTypeVariables4() throws IOException{
        CompilationUnit C0 = new CompilationUnit("a.C", 
                    "package a; " +
                    "public class C<T> {" +
                    "  public class I{" +
                    "    public void m(T t){}" +
                    "  } " +
                    "}");
        CompilationUnit C1 = new CompilationUnit("a.C", 
                    "package a; " +
                    "public class C {" +
                    "  public class I<T>{" +
                    "    public void m(T t){}" +
                    "  } " +
                    "}");
          IApi fromApi = convert(C0);
          IApi toApi = convert(C1);
          IApiDelta apiDelta = compare(fromApi, toApi);
          assertNotNull(apiDelta);
    }
    
    @Test
    public void interfaceClosureTest() throws IOException{
        CompilationUnit B = new CompilationUnit("a.B", 
                    "package a; " +
                    "public class  B<S> {}");
        CompilationUnit C = new CompilationUnit("a.C", 
                "package a; " +
                "public class C<R> extends B<R>  {}");
        CompilationUnit E = new CompilationUnit("a.E", 
                "package a; " +
                "public class E<Q> extends C<Q> {}");
        CompilationUnit F = new CompilationUnit("a.F", 
                "package a; " +
                "public class F<P> extends E<P> {}");
        CompilationUnit G = new CompilationUnit("a.G", 
                "package a; " +
                "public class G<O> extends F<O> {}");
        CompilationUnit H = new CompilationUnit("a.H", 
                "package a; " +
                "public class H<R> extends G<R> {}");
//        IApi fromApi = convert(B,C, E, F);
//        IApi toApi = convert(B,C,E, F);
          IApi fromApi = convert(B,C, E,F, G, H);
          IApi toApi = convert(B,C,E,F, G, H);
          
          
          long start = System.currentTimeMillis();
          IApiDelta apiDelta = compare(fromApi, toApi);
          System.out.println("compare took :" + (System.currentTimeMillis() -start) + "ms");
          assertNull(apiDelta);
    }
    
}