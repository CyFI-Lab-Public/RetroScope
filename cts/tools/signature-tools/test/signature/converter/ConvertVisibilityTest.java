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

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import signature.converter.Visibility;
import signature.converter.util.AbstractConvertTest;
import signature.converter.util.CompilationUnit;
import signature.model.IApi;
import signature.model.IClassDefinition;
import signature.model.IMethod;
import signature.model.IPackage;
import signature.model.util.ModelUtil;

public abstract class ConvertVisibilityTest extends AbstractConvertTest {

    @Test
    public void testVisibilityMethods1() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a;" +
                "public class A{" +
                "   public void foo1(){}" +
                "   protected void foo2(){}" +
                "   void foo3(){}" +
                "   private void foo4(){}" +
                "}");

        IApi api = convert(Visibility.PUBLIC, src);
        IPackage p = ModelUtil.getPackage(api, "a");
        IClassDefinition c = ModelUtil.getClass(p, "A");
        Set<IMethod> methods = c.getMethods();
        assertEquals(1, methods.size());
    }
    
    @Test
    public void testVisibilityMethods2() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a;" +
                "public class A{" +
                "   public void foo1(){}" +
                "   protected void foo2(){}" +
                "   void foo3(){}" +
                "   private void foo4(){}" +
                "}");

        IApi api = convert(Visibility.PROTECTED, src);
        IPackage p = ModelUtil.getPackage(api, "a");
        IClassDefinition c = ModelUtil.getClass(p, "A");
        Set<IMethod> methods = c.getMethods();
        assertEquals(2, methods.size());
    }
    
    @Test
    public void testVisibilityMethods3() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a;" +
                "public class A{" +
                "   public void foo1(){}" +
                "   protected void foo2(){}" +
                "   void foo3(){}" +
                "   private void foo4(){}" +
                "}");

        IApi api = convert(Visibility.PACKAGE, src);
        IPackage p = ModelUtil.getPackage(api, "a");
        IClassDefinition c = ModelUtil.getClass(p, "A");
        Set<IMethod> methods = c.getMethods();
        assertEquals(3, methods.size());
    }
    
    @Test
    public void testVisibilityMethods4() throws IOException {
        CompilationUnit src = new CompilationUnit("a.A", 
                "package a;" +
                "public class A{" +
                "   public void foo1(){}" +
                "   protected void foo2(){}" +
                "   void foo3(){}" +
                "   private void foo4(){}" +
                "}");

        IApi api = convert(Visibility.PRIVATE, src);
        IPackage p = ModelUtil.getPackage(api, "a");
        IClassDefinition c = ModelUtil.getClass(p, "A");
        Set<IMethod> methods = c.getMethods();
        assertEquals(4, methods.size());
    }
    
    @Test
    public void testVisibility1() throws IOException {
        CompilationUnit src = new CompilationUnit("a.X1", 
                "package a;" +
                "public class X1{" +
                "   static class X2{}" +
                "   protected static class X3{}" +
                "   private static class X4{}" +
                "}");

        IApi api = convert(Visibility.PUBLIC, src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        assertEquals(1, sigPackage.getClasses().size());
    }

    @Test
    public void testVisibility2() throws IOException {
        CompilationUnit src = new CompilationUnit("a.X1", 
                "package a;" +
                "public class X1{" +
                "   static class X2{}" +
                "   protected static class X3{}" +
                "   private static class X4{}" +
                "}");

        IApi api = convert(Visibility.PROTECTED, src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        assertEquals(2, sigPackage.getClasses().size());
        assertNotNull(ModelUtil.getClass(sigPackage, "X1.X3"));
    }

    @Test
    public void testVisibility3() throws IOException {
        CompilationUnit src = new CompilationUnit("a.X1", 
                "package a;" +
                "public class X1{" +
                "   static class X2{}" +
                "   protected static class X3{}" +
                "   private static class X4{}" +
                "}");

        IApi api = convert(Visibility.PACKAGE, src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        assertEquals(3, sigPackage.getClasses().size());
        assertNotNull(ModelUtil.getClass(sigPackage, "X1.X2"));
    }
    
    @Test
    public void testVisibility4() throws IOException {
        CompilationUnit src = new CompilationUnit("a.X1", 
                "package a;" +
                "public class X1{" +
                "   static class X2{}" +
                "   protected static class X3{}" +
                "   private static class X4{}" +
                "}");

        IApi api = convert(Visibility.PRIVATE, src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        assertEquals(4, sigPackage.getClasses().size());
        assertNotNull(ModelUtil.getClass(sigPackage, "X1.X4"));
    }

}