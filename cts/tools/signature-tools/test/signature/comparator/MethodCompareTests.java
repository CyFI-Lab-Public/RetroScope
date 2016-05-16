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

import java.io.IOException;

import org.junit.Test;

import signature.comparator.util.AbstractComparatorTest;
import signature.compare.model.IApiDelta;
import signature.compare.model.IClassDefinitionDelta;
import signature.compare.model.DeltaType;
import signature.converter.util.CompilationUnit;
import signature.model.IApi;

public abstract class MethodCompareTests extends AbstractComparatorTest {

    
    @Test
    public void compareEqualClasses() throws IOException{
        CompilationUnit A = new CompilationUnit("a.A", 
                      "package a; " +
                      "public class A {" +
                      "  public void m(){}" +
                      "}");
        IApi fromApi = convert(A);
        IApi toApi = convert(A);
        assertNull(compare(fromApi, toApi));
    }
      
    @Test
    public void compareMissingMethod() throws IOException{
        CompilationUnit A = new CompilationUnit("a.A", 
                      "package a; " +
                      "public class A {" +
                      "  public void m(){}" +
                      "}");
        CompilationUnit AMissing = new CompilationUnit("a.A", 
                      "package a; " +
                      "public class A {" +
                      "}");
        IApi fromApi = convert(A);
        IApi toApi = convert(AMissing);
        IApiDelta delta = compare(fromApi, toApi);
        assertNotNull(delta);
        IClassDefinitionDelta classDelta = delta.getPackageDeltas().iterator().next().getClassDeltas().iterator().next();
        assertEquals(1, classDelta.getMethodDeltas().size());
        assertEquals(DeltaType.REMOVED, classDelta.getMethodDeltas().iterator().next().getType());
    }
      
    @Test 
    public void compareAddedMethod() throws IOException{
        CompilationUnit A = new CompilationUnit("a.A", 
                  "package a; " +
                  "public class A {" +
                  "  public void m(){}" +
                  "}");
        CompilationUnit AMissing = new CompilationUnit("a.A", 
                  "package a; " +
                  "public class A {" +
                  "}");
        IApi fromApi = convert(AMissing);
        IApi toApi = convert(A);
        IApiDelta delta = compare(fromApi, toApi);
        assertNotNull(delta);
        IClassDefinitionDelta classDelta = delta.getPackageDeltas().iterator().next().getClassDeltas().iterator().next();
        assertEquals(1, classDelta.getMethodDeltas().size());
        assertEquals(DeltaType.ADDED, classDelta.getMethodDeltas().iterator().next().getType());
    }
      
    @Test
    public void compareChangedMethod() throws IOException{
        CompilationUnit A = new CompilationUnit("a.A", 
                  "package a; " +
                  "public class A {" +
                  "  public void m(){}" +
                  "}");
        CompilationUnit AMissing = new CompilationUnit("a.A", 
                  "package a; " +
                  "public class A {" +
                  "  public void m() throws Exception {}" +
                  "}");
        IApi fromApi = convert(AMissing);
        IApi toApi = convert(A);
        IApiDelta delta = compare(fromApi, toApi);
        assertNotNull(delta);
        IClassDefinitionDelta classDelta = delta.getPackageDeltas().iterator().next().getClassDeltas().iterator().next();
        assertEquals(1, classDelta.getMethodDeltas().size());
        assertEquals(DeltaType.CHANGED, classDelta.getMethodDeltas().iterator().next().getType());
    }
      
    @Test
    public void compareAddedParameterMethod() throws IOException{
        CompilationUnit A = new CompilationUnit("a.A", 
                  "package a; " +
                  "public class A {" +
                  "  public void m(){}" +
                  "}");
        CompilationUnit AMissing = new CompilationUnit("a.A", 
                  "package a; " +
                  "public class A {" +
                  "  public void m(int i) {}" +
                  "}");
        IApi fromApi = convert(AMissing);
        IApi toApi = convert(A);
        IApiDelta delta = compare(fromApi, toApi);
        assertNotNull(delta);
        IClassDefinitionDelta classDelta = delta.getPackageDeltas().iterator().next().getClassDeltas().iterator().next();
        assertEquals(2, classDelta.getMethodDeltas().size()); //one added , one removed
    }
      
    @Test
    public void compareExceptions0() throws IOException{
        CompilationUnit E0 = new CompilationUnit("a.E0", 
                "package a; " +
                "public class E0 extends Exception {}");
        CompilationUnit E1 = new CompilationUnit("a.E1", 
                "package a; " +
                "public class E1 extends E0 {}");
        CompilationUnit C0 = new CompilationUnit("a.C0", 
                 "package a; " +
                 "public class C0 {" +
                 "  public void m() throws E0 {}" +
                 "}");
        CompilationUnit C0_E1 = new CompilationUnit("a.C0", 
                 "package a; " +
                 "public class C0{" +
                 "  public void m() throws E0, E1 {}" +
                 "}");
        IApi fromApi = convert(E0, E1, C0);
        IApi toApi = convert(E0, E1, C0_E1);
        IApiDelta apiDelta = compare(fromApi, toApi);
        assertNull(apiDelta);
    }
    
    @Test
    public void compareExceptions1() throws IOException{
        CompilationUnit E0 = new CompilationUnit("a.E0", 
                "package a; " +
                "public class E0 extends Exception {}");
        CompilationUnit E1 = new CompilationUnit("a.E1", 
                "package a; " +
                "public class E1 extends Exception {}");
        CompilationUnit C0 = new CompilationUnit("a.C0", 
                 "package a; " +
                 "public class C0 {" +
                 "  public void m() throws E0 {}" +
                 "}");
        CompilationUnit C0_E1 = new CompilationUnit("a.C0", 
                 "package a; " +
                 "public class C0{" +
                 "  public void m() throws E0, E1 {}" +
                 "}");
        IApi fromApi = convert(E0, E1, C0);
        IApi toApi = convert(E0, E1, C0_E1);
        IApiDelta apiDelta = compare(fromApi, toApi);
        assertNotNull(apiDelta);
    }
    
    @Test
    public void compareRuntimeExceptions() throws IOException{
        CompilationUnit E0 = new CompilationUnit("a.E0", 
                 "package a; " +
                 "public class E0 extends RuntimeException {}");
        CompilationUnit C0 = new CompilationUnit("a.C0", 
                 "package a; " +
                 "public class C0{" +
                 "  public void m() {}" +
                 "}");
        CompilationUnit C0_E0 = new CompilationUnit("a.C0", 
                 "package a; " +
                 "public class C0 {" +
                 "  public void m() throws E0 {}" +
                 "}");
        IApi fromApi = convert(E0, C0);
        IApi toApi = convert(E0, C0_E0);
        IApiDelta apiDelta = compare(fromApi, toApi);
        assertNull(apiDelta);
    }
    
    @Test
    public void compareAnnotations() throws IOException{
        CompilationUnit C0 = new CompilationUnit("a.C0", 
                 "package a; " +
                 "public class C0{" +
                 "  public void m(int i) {}" +
                 "}");
        CompilationUnit C1 = new CompilationUnit("a.C0", 
                 "package a; " +
                 "public class C0 {" +
                 "  public void m(@Deprecated  int i) {}" +
                 "}");
        IApi fromApi = convert(C0);
        IApi toApi = convert(C1);
        IApiDelta apiDelta = compare(fromApi, toApi);
        assertNotNull(apiDelta);
    }
    
    @Test
    public void compareMissingDefaultConstructor() throws IOException{
        CompilationUnit C0 = new CompilationUnit("a.C0", 
                 "package a; " +
                 "public class C0{" +
                 "  public C0() {}" +
                 "}");
        CompilationUnit C1 = new CompilationUnit("a.C0", 
                 "package a; " +
                 "public class C0 {}");
        IApi fromApi = convert(C0);
        IApi toApi = convert(C1);
        IApiDelta apiDelta = compare(fromApi, toApi);
        assertNull(apiDelta);
    }
        
    @Test
    public void compareMissingAbstractMethod() throws IOException{
        CompilationUnit I = new CompilationUnit("a.I", 
                 "package a; " +
                 "public interface I{" +
                 "  void m();" + 
                   "}");
        CompilationUnit C0 = new CompilationUnit("a.C0", 
                 "package a; " +
                 "public abstract class C0 implements I{" +
                 "  public abstract void m(); " +
                 "}");
        CompilationUnit C1 = new CompilationUnit("a.C0", 
                 "package a; " +
                 "public abstract class C0 implements I{}");
        IApi fromApi = convert(C0, I);
        IApi toApi = convert(C1, I);
        IApiDelta apiDelta = compare(fromApi, toApi);
        assertNull(apiDelta);
    }
           
    @Test
    public void compareMissingInheritedMethod() throws IOException{
        CompilationUnit I = new CompilationUnit("a.I", 
                "package a; " +
                "public class I{" +
                "  public void m(){};" + 
                "}");
        CompilationUnit C0 = new CompilationUnit("a.C0", 
                "package a; " +
                "public class C0 extends I{" +
                "  public void m(){}; " +
                "}");
        CompilationUnit C1 = new CompilationUnit("a.C0", 
                "package a; " +
                "public class C0 extends I{}");
        IApi fromApi = convert(C0, I);
        IApi toApi = convert(C1, I);
        IApiDelta apiDelta = compare(fromApi, toApi);
        assertNull(apiDelta);
     }
       
    @Test
    public void compareMissingInheritedMethodGeneric0() throws IOException{
        CompilationUnit I = new CompilationUnit("a.I", 
                "package a; " +
                "public class I<T>{" +
                "  public void m(T t){};" + 
                "}");
        CompilationUnit C0 = new CompilationUnit("a.C0", 
                "package a; " +
                "public class C0<T> extends I<T>{" +
                "  public void m(T t){}; " +
                "}");
        CompilationUnit C1 = new CompilationUnit("a.C0", 
                "package a; " +
                "public class C0<T> extends I<T>{}");
        IApi fromApi = convert(C0, I);
        IApi toApi = convert(C1, I);
        IApiDelta apiDelta = compare(fromApi, toApi);
        assertNull(apiDelta);
    }
           
    @Test
    public void compareMissingInheritedMethodGeneric1() throws IOException{
        CompilationUnit I = new CompilationUnit("a.I", 
                "package a; " +
                "public class I<T,S>{" +
                "  public void m(S s){};" + 
                "}");
        CompilationUnit C0 = new CompilationUnit("a.C0", 
                "package a; " +
                "public class C0<Q, R> extends I<Q,R>{" +
                "  public void m(R t){}; " +
                "}");
        CompilationUnit C1 = new CompilationUnit("a.C0", 
                "package a; " +
                "public class C0<Y,Z> extends I<Y,Z>{}");
         IApi fromApi = convert(C0, I);
         IApi toApi = convert(C1, I);
         IApiDelta apiDelta = compare(fromApi, toApi);
         assertNull(apiDelta);
    }
           
    @Test
    public void compareMissingInheritedMethodGeneric2() throws IOException{
        CompilationUnit I = new CompilationUnit("a.I", 
                "package a; " +
                "public class I<T,S>{" +
                "  public void m(S s){};" + 
                "}");
        CompilationUnit J = new CompilationUnit("a.J", 
                "package a; " +
                "public class J<W> extends I<Number,W>{" +
                "  public void m(W w){};" + 
                "}");
        CompilationUnit C0 = new CompilationUnit("a.C0", 
                "package a; " +
                "public class C0<Q> extends J<Q>{" +
                "  public void m(Q t){}; " +
                "}");
        CompilationUnit C1 = new CompilationUnit("a.C0", 
                "package a; " +
                "public class C0<Y> extends J<Y>{}");
        IApi fromApi = convert(C0, I, J);
        IApi toApi = convert(C1, I, J);
        IApiDelta apiDelta = compare(fromApi, toApi);
        assertNull(apiDelta);
    }
    
    

    @Test
    public void compareMissingInheritedMethodGeneric3() throws IOException{
        CompilationUnit Q = new CompilationUnit("a.Q", 
                "package a; " +
                "public class Q<S,T>{ " +
                "  public void m(T s){} " +
                "}");
        
        CompilationUnit W = new CompilationUnit("a.W", 
                "package a; " +
                "public class W<A,B> extends Q<A,A>{}");
        CompilationUnit E0 = new CompilationUnit("a.E", 
                "package a; " +
                "public class E<C,D> extends W<C,C>{" +
                "  public void m(C s){}" +
                "}");
        CompilationUnit E1 = new CompilationUnit("a.E", 
                "package a; " +
                "public class E<C,D> extends W<C,C>{}");
        IApi fromApi = convert(E0, Q, W);
        IApi toApi = convert(E1, Q, W);
        IApiDelta apiDelta = compare(fromApi, toApi);
        assertNull(apiDelta);
    }
    
    @Test
    public void compareMissingInheritedMethodGeneric4() throws IOException{
        CompilationUnit Q = new CompilationUnit("a.Q", 
                "package a; " +
                "public class Q<S,T>{ " +
                "  public void m(T t, S s){} " +
                "}");
        
        CompilationUnit W = new CompilationUnit("a.W", 
                "package a; " +
                "public class W<A,B> extends Q<A,A>{}");
        CompilationUnit E0 = new CompilationUnit("a.E", 
                "package a; " +
                "public class E<C,D> extends W<C,C>{" +
                "  public void m(C s, C c){}" +
                "}");
        CompilationUnit E1 = new CompilationUnit("a.E", 
                "package a; " +
                "public class E<C,D> extends W<C,C>{}");
        IApi fromApi = convert(E0, Q, W);
        IApi toApi = convert(E1, Q, W);
        IApiDelta apiDelta = compare(fromApi, toApi);
        assertNull(apiDelta);
    }
    
    @Test
    public void compareMissingInheritedMethodGeneric5() throws IOException{
        CompilationUnit Q = new CompilationUnit("a.Q", 
                "package a; " +
                "public class Q{}");
        
        CompilationUnit I = new CompilationUnit("a.I", 
                "package a; " +
                "public class I<S,T>{" +
                "  public void m(T s){};" + 
                "}");
        CompilationUnit C0 = new CompilationUnit("a.C0", 
                "package a; " +
                "public class C0<P> extends I<P, Q>{" +
                "  public void m(Q t){}; " +
                "}");
        CompilationUnit C1 = new CompilationUnit("a.C0", 
                "package a; " +
                "public class C0<Y> extends I<Y, Q>{}");
        IApi fromApi = convert(C0, I, Q);
        IApi toApi = convert(C1, I, Q);
        IApiDelta apiDelta = compare(fromApi, toApi);
        assertNull(apiDelta);
    }
    
    
    @Test
    public void substitutionTest() throws IOException{
        CompilationUnit NUMBER = new CompilationUnit("a.Number", 
                "package a; " +
                "public class Number{}");
        CompilationUnit Q = new CompilationUnit("a.A", 
                "package a; " +
                "public class A<T>{ " +
                "  public void m(T t){} " +
                "}");
        CompilationUnit E0 = new CompilationUnit("a.E", 
                "package a; " +
                "public class E extends A<Number>{" +
                "  public void m(Number n){}" +
                "}");
        CompilationUnit E1 = new CompilationUnit("a.E", 
                "package a; " +
                "public class E extends A<Number>{}");
        IApi fromApi = convert(E0, Q, NUMBER);
        IApi toApi = convert(E1, Q, NUMBER);
        
        IApiDelta apiDelta = compare(fromApi, toApi);
        assertNull(apiDelta);
    }
    
    @Test
    public void substitutionArrayTest() throws IOException{
        CompilationUnit NUMBER = new CompilationUnit("a.Number", 
                "package a; " +
                "public class Number{}");
        CompilationUnit Q = new CompilationUnit("a.A", 
                "package a; " +
                "public class A<T>{ " +
                "  public void m(T[] t){} " +
                "}");
        CompilationUnit E0 = new CompilationUnit("a.E", 
                "package a; " +
                "public class E extends A<Number>{" +
                "  public void m(Number[] n){}" +
                "}");
        CompilationUnit E1 = new CompilationUnit("a.E", 
                "package a; " +
                "public class E extends A<Number>{}");
        IApi fromApi = convert(E0, Q, NUMBER);
        IApi toApi = convert(E1, Q, NUMBER);
        
        IApiDelta apiDelta = compare(fromApi, toApi);
        assertNull(apiDelta);
    }
    
    @Test
    public void substitutionNestedTypes() throws IOException{
        CompilationUnit NUMBER = new CompilationUnit("a.Number", 
                "package a; " +
                "public class Number{}");
        CompilationUnit Q = new CompilationUnit("a.A", 
                "package a; " +
                "public class A<T>{ " +
                "  public void m(A<A<T[]>> t){} " +
                "}");
        CompilationUnit E0 = new CompilationUnit("a.E", 
                "package a; " +
                "public class E extends A<Number>{" +
                "  public void m(A<A<Number[]>> n){}" +
                "}");
        CompilationUnit E1 = new CompilationUnit("a.E", 
                "package a; " +
                "public class E extends A<Number>{}");
        IApi fromApi = convert(E0, Q, NUMBER);
        IApi toApi = convert(E1, Q, NUMBER);
        
        IApiDelta apiDelta = compare(fromApi, toApi);
        assertNull(apiDelta);
    }
}
