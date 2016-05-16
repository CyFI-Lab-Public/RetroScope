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

package dex.reader;

import static org.junit.Assert.assertEquals;
import dex.reader.util.JavaSource;
import dex.structure.DexClass;
import dex.structure.DexFile;

import org.junit.Test;

import java.io.IOException;

public class LargeDexTests extends DexTestsCommon{
   
    /**
     * Tests parsing a class with 10000 Fields.
     */
    @Test
    public void testManyFields() throws IOException{
        String CLASS_NAME = "L0";
        int NR_OF_FIELDS = 10000;
        StringBuilder b = new StringBuilder();
        b.append("public class ").append(CLASS_NAME).append("{\n");
        for (int i = 0; i < NR_OF_FIELDS; i++) {
            b.append("\tpublic int f").append(i).append(";\n");
        }
        b.append("}\n");
        
        JavaSource source = new JavaSource(CLASS_NAME, b.toString());
        DexFile dexFile = javaToDexUtil.getFrom(source);
        assertEquals(1, dexFile.getDefinedClasses().size());
        DexClass clazz = dexFile.getDefinedClasses().get(0);
        assertEquals("LL0;", clazz.getName());
        assertPublic(clazz);
        assertEquals(NR_OF_FIELDS, clazz.getFields().size());
    }
}
