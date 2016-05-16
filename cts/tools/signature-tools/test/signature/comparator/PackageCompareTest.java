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

import org.junit.Test;

import signature.comparator.util.AbstractComparatorTest;
import signature.compare.model.IApiDelta;
import signature.compare.model.DeltaType;
import signature.compare.model.IPackageDelta;
import signature.converter.util.CompilationUnit;
import signature.model.IApi;

import java.io.IOException;

public abstract class PackageCompareTest extends AbstractComparatorTest{

    @Test
    public void compareEqualPackageTest0() throws IOException{
         CompilationUnit from = new CompilationUnit("a.A", 
                    "package a; " +
                    "public class A {}");
          IApi fromApi = convert(from);
          IApi toApi = convert(from);
          IApiDelta delta = compare(fromApi, toApi);
          assertNull(delta);
    }

    @Test
    public void compareEqualPackageTest1() throws IOException{
         CompilationUnit from0 = new CompilationUnit("a.A", 
                    "package a; " +
                    "public class A {}");
         CompilationUnit from1 = new CompilationUnit("a.b.A", 
                    "package a.b; " +
                    "public class A {}");
          IApi fromApi = convert(from0, from1);
          IApi toApi = convert(from0, from1);
          assertNull(compare(fromApi, toApi));
    }

    @Test
    public void compareRemovedPackagePackageTest1() throws IOException{
         CompilationUnit packageA = new CompilationUnit("a.A", 
                    "package a; " +
                    "public class A {}");
         CompilationUnit packageB = new CompilationUnit("a.b.A", 
                    "package a.b; " +
                    "public class A {}");
          IApi fromApi = convert(packageA, packageB);
          IApi toApi = convert(packageA);
          IApiDelta apiDelta = compare(fromApi, toApi);
          assertNotNull(apiDelta);
          
          assertEquals(1, apiDelta.getPackageDeltas().size());
          IPackageDelta packageDelta = apiDelta.getPackageDeltas().iterator().next();
          assertEquals(DeltaType.REMOVED, packageDelta.getType());
    }

    @Test
    public void compareAddedPackagePackageTest1() throws IOException{
         CompilationUnit packageA = new CompilationUnit("a.A", 
                    "package a; " +
                    "public class A {}");
         CompilationUnit packageB = new CompilationUnit("a.b.A", 
                    "package a.b; " +
                    "public class A {}");
          IApi fromApi = convert(packageA);
          IApi toApi = convert(packageA, packageB);
          IApiDelta apiDelta = compare(fromApi, toApi);
          assertNotNull(apiDelta);
          
          assertEquals(1, apiDelta.getPackageDeltas().size());
          IPackageDelta packageDelta = apiDelta.getPackageDeltas().iterator().next();
          assertEquals(DeltaType.ADDED, packageDelta.getType());
    }
}
