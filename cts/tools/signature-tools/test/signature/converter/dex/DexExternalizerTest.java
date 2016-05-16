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

import org.junit.Test;

import signature.converter.Visibility;
import signature.io.IApiExternalizer;
import signature.io.impl.BinaryApi;
import signature.model.IApi;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

public class DexExternalizerTest {
    
    @Test
    public void testExternalizer() throws IOException{
        DexToSigConverter converter = new DexToSigConverter();
        IApi api = converter.convertApi("Dex Tests", DexUtil.getDexFiles(new HashSet<String>(Arrays.asList(new String[]{"resources/javaCore.dex"}))), Visibility.PRIVATE);
        System.setProperty("sun.io.serialization.extendedDebugInfo", "true");
        IApiExternalizer externalizer = new BinaryApi();
        externalizer.externalizeApi("dex-spec", api);
    }
}
