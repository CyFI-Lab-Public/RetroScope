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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import signature.comparator.util.AbstractComparatorTest;
import signature.converter.util.CompilationUnit;
import signature.model.IApi;

import java.io.IOException;

public abstract class AnnotationCompareTest extends AbstractComparatorTest {

    @Test
    public void testAnnotationValue() throws IOException{
        CompilationUnit A0 = new CompilationUnit("a.A0", 
                "package a; " +
                "public @interface A0 {" +
                "  A1 value() default @A1;" + 
                "}");
        CompilationUnit A1 = new CompilationUnit("a.A1", 
                "package a; " +
                "public @interface A1 {" +
                "}");
         CompilationUnit AnnotBDefault = new CompilationUnit("a.B", 
                    "package a; " +
                    "@A0 " +
                    "public class B {}");
         CompilationUnit AnnotB = new CompilationUnit("a.B", 
                    "package a; " +
                    "@A0 " +
                    "public class B {}");
          IApi fromApi = convert(A0, A1, AnnotBDefault);
          IApi toApi = convert(A0, A1, AnnotB);
          assertNull(compare(fromApi, toApi));
    }
    
       @Test
        public void testDefaultAnnotationValue() throws IOException{
             CompilationUnit A0 = new CompilationUnit("a.A0", 
                    "package a; " +
                    "public @interface A0 {" +
                    "  String value() default \"bla\";" +
                    "}");
             CompilationUnit A1 = new CompilationUnit("a.A0", 
                    "package a; " +
                    "public @interface A0 {" +
                    "  String value();" + 
                    "}");
              IApi fromApi = convert(A0);
              IApi toApi = convert(A1);
              assertNotNull(compare(fromApi, toApi));
        }
}
