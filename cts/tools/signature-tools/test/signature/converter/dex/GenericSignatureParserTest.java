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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import dex.reader.DexTestsCommon;
import dex.reader.util.JavaSource;
import dex.reader.util.JavaSourceToDexUtil;
import dex.structure.DexClass;
import dex.structure.DexFile;

import org.junit.Before;
import org.junit.Test;

import signature.converter.dex.DexToSigConverter;
import signature.converter.dex.DexUtil;
import signature.converter.dex.GenericSignatureParser;
import signature.model.impl.SigClassDefinition;
import signature.model.util.TypePool;

import java.io.IOException;

public class GenericSignatureParserTest extends DexTestsCommon{
    
    private DexToSigConverter converter;
    private JavaSourceToDexUtil dexUtil;
    private GenericSignatureParser parser;

    @Before
    public void setupConverter(){
        converter = new DexToSigConverter();
        dexUtil = new JavaSourceToDexUtil();
        parser = new GenericSignatureParser(new TypePool(), converter);
    }
    
    @Test
    public void getGenericSignatureTest() throws IOException {
        DexFile  dexFile =  dexUtil.getFrom(new JavaSource("B", "public class B<T>{}"));
        DexClass dexClass = getClass(dexFile, "LB;");
        assertEquals("<T:Ljava/lang/Object;>Ljava/lang/Object;", DexUtil.getGenericSignature(dexClass));
        SigClassDefinition sigClass = converter.convertClass(dexClass);
        
        parser.parseForClass(sigClass, DexUtil.getGenericSignature(dexClass));
        //type parameter name
        assertEquals(1, parser.formalTypeParameters.size());
        assertEquals("T", parser.formalTypeParameters.get(0).getName());
        //type parameter declaration
        assertSame(sigClass, parser.formalTypeParameters.get(0).getGenericDeclaration());
        //type parameter upper bounds
        assertEquals(1, parser.formalTypeParameters.get(0).getUpperBounds().size());
//        IType type = parser.formalTypeParameters.get(0).getUpperBounds().get(0);
        
    }
}
