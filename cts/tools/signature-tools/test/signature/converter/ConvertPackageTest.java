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

import java.io.IOException;

import org.junit.Test;

import signature.converter.util.AbstractConvertTest;
import signature.converter.util.CompilationUnit;
import signature.model.IAnnotation;
import signature.model.IApi;
import signature.model.IPackage;
import signature.model.util.ModelUtil;

public abstract class ConvertPackageTest extends AbstractConvertTest {

    // tests whether package annotations are returned
    @Test
    public void testPackageAnnotation() throws IOException {
        CompilationUnit packageSrc = new CompilationUnit("a.package-info", "@Deprecated package a;");
        CompilationUnit classSrc = new CompilationUnit("a.X", "package a; public class X{}");
        IApi api = convert(classSrc, packageSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        assertEquals(1, sigPackage.getAnnotations().size());
        IAnnotation annotation = sigPackage.getAnnotations().iterator().next();        
        assertEquals("java.lang.Deprecated", annotation.getType().getClassDefinition().getQualifiedName());
    }
        
    // tests that package-info is not returned as class 
    @Test
    public void testNumberOfClasses1() throws IOException {
        CompilationUnit packageSrc = new CompilationUnit("a.package-info", "@Deprecated package a;");
        CompilationUnit classSrc = new CompilationUnit("a.X", "package a; public class X{}");
        IApi api = convert(classSrc, packageSrc);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        assertEquals(1, sigPackage.getClasses().size());
    }
        
    // tests that nested classes are returned as package classes 
    @Test
    public void testNumberOfClasses2() throws IOException {
        CompilationUnit src = new CompilationUnit("a.X",
                "package a;" +
                "public class X{" +
                "   public static class Y{}" +
                "}");
        IApi api = convert(src);
        IPackage sigPackage = ModelUtil.getPackage(api, "a");
        assertEquals(2, sigPackage.getClasses().size());
    }
        
}