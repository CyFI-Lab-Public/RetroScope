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

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import org.junit.Test;

import signature.converter.util.AbstractConvertTest;
import signature.converter.util.CompilationUnit;
import signature.model.IApi;
import signature.model.IClassDefinition;
import signature.model.IField;
import signature.model.IPackage;
import signature.model.IParameterizedType;
import signature.model.ITypeReference;
import signature.model.util.ModelUtil;

public abstract class ConvertParameterizedTypeTest extends AbstractConvertTest {
    /**
     * [L, a, /, A, $, B, <, L, j, a, v, a, /, l, a, n, g, /, I, n, t, e, g, e, r, ;, >, ;]
     * '$' - separated
     * @throws IOException
     */
    @Test
    public void convertParameterizedType() throws IOException {
        String source = 
        "package a; " +
        "public class A{" +
        "  public class B<T> {} " +
        "  public A.B<Integer> f; "+
        "}";
        IApi api = convert(new CompilationUnit("a.A", source));
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A");
        IField field = ModelUtil.getField(sigClass, "f");
        
        ITypeReference type = field.getType();
        assertTrue(type instanceof IParameterizedType);
        
        IParameterizedType parametrizedType = (IParameterizedType)type;
        ITypeReference ownerType = parametrizedType.getOwnerType();
        assertNotNull(ownerType);
    }
    
    @Test
    public void convertWildcardLowerBound() throws IOException {
        String clazz = 
        "package a; " +
        "public final class A<T> implements I<T>{ " +
        " abstract class Super{} " +
        " final class Sub extends Super implements I<T>{} " +
        "}";
        String interfaze = 
            "package a; " +
            "public interface I <T>{}";
        IApi api = convert(Visibility.PRIVATE, new CompilationUnit("a.A", clazz),new CompilationUnit("a.I", interfaze));
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        IClassDefinition sigClass = ModelUtil.getClass(sigPackage, "A.Sub");
        System.out.println(sigClass);
    }
    
    public class A{
      public class B<T> {}
      public A.B<Integer> f;
    }
   
    @Test
    public void reflectionTest0() throws SecurityException, NoSuchFieldException{
        Field field = A.class.getDeclaredField("f");
        ParameterizedType paramType = (ParameterizedType)field.getGenericType();
        assertNotNull(paramType.getOwnerType());
    }
    
    public static class C<T>{}
    
    ConvertParameterizedTypeTest.C<String> f;
     
      @Test
      public void reflectionTest1() throws SecurityException, NoSuchFieldException{
          Field field = ConvertParameterizedTypeTest.class.getDeclaredField("f");
          ParameterizedType paramType = (ParameterizedType)field.getGenericType();
          assertNotNull(paramType.getOwnerType());
      }
      
      public static class E<T>{
          static class F<Q>{}
          E.F<String> f;
      }
      
       
     @Test
    public void reflectionTest2() throws SecurityException, NoSuchFieldException {
        Field field = E.class.getDeclaredField("f");
        ParameterizedType paramType = (ParameterizedType) field.getGenericType();
        assertNotNull(paramType.getOwnerType());
    }
}
