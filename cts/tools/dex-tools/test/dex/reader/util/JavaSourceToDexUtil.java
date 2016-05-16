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

package dex.reader.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler.CompilationTask;

import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.file.ClassDefItem;
import com.android.dx.dex.file.DexFile;

import dex.reader.DexBuffer;
import dex.reader.DexFileReader;

public class JavaSourceToDexUtil {
    
    public dex.structure.DexFile getFrom(JavaSource source) throws IOException{
        return getAllFrom(Collections.singleton(source));
    }
    
    public dex.structure.DexFile getFrom(JavaSource... source) throws IOException{
        return getAllFrom(new HashSet<JavaSource>(Arrays.asList(source)));
    }
    
    public dex.structure.DexFile getAllFrom(Set<JavaSource> sources) throws IOException{
        return getFrom(sources, null);
    }

    /**
     * Converts java source code to a {@link dex.structure.DexFile} loaded by
     * {@link DexFileReader}. Converts only classes with the specified name in
     * classesToDex or all classes if classesToDex is null.
     * 
     * @throws IOException
     */
    public dex.structure.DexFile getFrom(Set<JavaSource> sources,
            Set<String> classesToDex) throws IOException {
        Set<MemoryByteCode> byteCodeInMemory = compileToByteCode(sources);

        byte[] dexCode = convertToDexCode(byteCodeInMemory, classesToDex);
        DexBuffer dexBuffer = new DexBuffer(dexCode);
        DexFileReader reader = new DexFileReader();
        return reader.read(dexBuffer);
    }
    

    private byte[] convertToDexCode(Set<MemoryByteCode> byteCodeInMemory, Set<String> classNamesToDex) throws IOException {
        CfOptions cfOptions = new CfOptions();
        DexFile dexFile = new DexFile();
        for (MemoryByteCode memoryByteCode : byteCodeInMemory) {
            if(classNamesToDex == null || classNamesToDex.contains(memoryByteCode.getName())) {
            ClassDefItem classDefItem = CfTranslator.translate(memoryByteCode.getName().replace('.', '/') +".class", memoryByteCode.getBytes(), cfOptions);
            dexFile.add(classDefItem);
            }
        }
        return dexFile.toDex(null, false);
    }


    public Set<MemoryByteCode> compileToByteCode(Set<JavaSource> source) {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diacol = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager sjfm = javac.getStandardFileManager(diacol,
                null, null);
        SpecialJavaFileManager xfm = new SpecialJavaFileManager(sjfm);
        
        CompilationTask compile = javac.getTask(null, xfm, diacol, Arrays
                .asList(new String[] {"-classpath", "."}), null, source);
        boolean success = compile.call();
        if(!success) {
        StringBuilder errorMessage = new StringBuilder();
            for (Diagnostic<? extends JavaFileObject> dia : diacol.getDiagnostics()) {
                errorMessage.append(dia);
            }
            throw new IllegalStateException(errorMessage.toString());
        }
        return xfm.getAllMemoryByteCodes();
    }
}






