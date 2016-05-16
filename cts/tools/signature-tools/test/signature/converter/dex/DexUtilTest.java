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

package signature.converter.dex;


import dex.reader.DexTestsCommon;
import dex.reader.util.JavaSource;
import dex.reader.util.JavaSourceToDexUtil;
import dex.structure.DexClass;
import dex.structure.DexFile;

import org.junit.Before;
import org.junit.Test;

import signature.converter.dex.DexToSigConverter;
import signature.model.impl.SigPackage;
import static signature.converter.dex.DexUtil.*;

import java.io.IOException;

import static org.junit.Assert.*;

public class DexUtilTest extends DexTestsCommon {

    private JavaSourceToDexUtil dexUtil;
    private DexToSigConverter converter;
    
    @Before
    public void setupDexUtil(){
        dexUtil = new JavaSourceToDexUtil();
        converter = new DexToSigConverter();
    }
    
    @Test
    public void convertPackageTest(){
        SigPackage aPackage = converter.convertPackage("a");
        assertEquals("a", aPackage.getName());
        
        aPackage = converter.convertPackage("a.b.c");
        assertEquals("a.b.c", aPackage.getName());
    }
    
    @Test
    public void getPackageNameTest(){
        assertEquals("",getPackageName("LA;"));
        assertEquals("a",getPackageName("La/A;"));
        assertEquals("a.b.c",getPackageName("La/b/c/A;"));
    }
    
    @Test
    public void getClassNameTest(){
        assertEquals("A",getClassName("LA;"));
        assertEquals("A",getClassName("La/A;"));
        assertEquals("A",getClassName("La/b/c/A;"));
    }
    
    @Test
    public void hasGenericSignatureTest() throws IOException {
        DexFile dexFile = dexUtil.getFrom(new JavaSource("A", "public class A{}"));
        DexClass dexClass = getClass(dexFile, "LA;");
        assertFalse(hasGenericSignature(dexClass));

        dexFile = dexUtil.getFrom(new JavaSource("B", "public class B<T>{}"));
        dexClass = getClass(dexFile, "LB;");
        assertTrue(hasGenericSignature(dexClass));
    }
    
    @Test
    public void getGenericSignatureTest() throws IOException {
        DexFile dexFile =  dexUtil.getFrom(new JavaSource("A", "public class A{}"));
        DexClass dexClass = getClass(dexFile, "LA;");
        assertNull(getGenericSignature(dexClass));
        
        dexFile =  dexUtil.getFrom(new JavaSource("B", "public class B<T>{}"));
        dexClass = getClass(dexFile, "LB;");
        assertEquals("<T:Ljava/lang/Object;>Ljava/lang/Object;", getGenericSignature(dexClass));
    }
    
    
    
}
