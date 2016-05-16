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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import signature.converter.doclet.DocletTestConverter;
import signature.converter.util.ITestSourceConverter;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    AllDocletTests.ClassTest.class,
    AllDocletTests.PackageTest.class,
    AllDocletTests.AnnotationTest.class,
    AllDocletTests.VisibilityTest.class,
    AllDocletTests.WildcardTest.class,
    AllDocletTests.EnumTest.class
})
public class AllDocletTests {
    private static ITestSourceConverter newConverter(){
        return new DocletTestConverter();
    }
    
    public static class ClassTest extends ConvertClassTest {
        @Override public ITestSourceConverter createConverter() {
            return newConverter();
        }
    }
    public static class AnnotationTest extends ConvertAnnotationTest {
        @Override public ITestSourceConverter createConverter() {
            return newConverter();
        }
    }
    public static class PackageTest extends ConvertPackageTest {
        @Override public ITestSourceConverter createConverter() {
            return newConverter();
        }
    }
    public static class VisibilityTest extends ConvertVisibilityTest {
        @Override public ITestSourceConverter createConverter() {
            return newConverter();
        }
    }
    public static class WildcardTest extends ConvertWildcardTest {
        @Override
        public ITestSourceConverter createConverter() {
            return newConverter();
        }
    }
    public static class EnumTest extends ConvertEnumTest {
        @Override public ITestSourceConverter createConverter() {
            return newConverter();
        }
    }
}

