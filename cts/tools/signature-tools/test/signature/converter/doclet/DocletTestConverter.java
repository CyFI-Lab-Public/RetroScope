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

package signature.converter.doclet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

import signature.converter.Visibility;
import signature.converter.doclet.DocletToSigConverter;
import signature.converter.util.CompilationUnit;
import signature.model.IApi;
import signature.model.util.ModelUtil;

import com.sun.javadoc.RootDoc;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Options;
import com.sun.tools.javadoc.JavadocTool;
import com.sun.tools.javadoc.Messager;
import com.sun.tools.javadoc.ModifierFilter;
import com.sun.tools.javadoc.RootDocImpl;

public class DocletTestConverter extends signature.converter.util.AbstractTestSourceConverter {

    static String sourcepath;
    static String separator;
    
    static {
        separator = System.getProperty("file.separator");

        sourcepath = System.getProperty("java.io.tmpdir");
        sourcepath = sourcepath + separator + "cts" + separator;
        System.out.println(">> "+sourcepath);
    }

    public IApi convert(Visibility visibility, Set<CompilationUnit> units) throws IOException {
        try {
            Set<String> packages = new HashSet<String>();
            for(CompilationUnit u : units){
                putSource(u);
                String p = ModelUtil.getPackageName(u.getName());
                assert p.length() > 0 : "default package not supported by doclets";
                packages.add(p);
            }
            
            RootDoc root = getRootDoc(visibility, packages);

            DocletToSigConverter converter = new DocletToSigConverter();
            
            IApi api = converter.convertDocletRoot("Doclet Test", root, visibility, packages);
            return api;
        }
        finally {
            removeSources();
        }
    }
    
    public static void putSource(CompilationUnit source) throws IOException {
        String directory = sourcepath;
        String filename = source.getName();    // a.b.C
        int pos = filename.indexOf(".");
        while(pos > 0) {
            directory = directory + filename.substring(0, pos) + separator;
            filename = filename.substring(pos+1);
            pos = filename.indexOf(".");
        }
        
        File file = new File(directory, filename + ".java");
        File parent = file.getParentFile();
        parent.mkdirs();
        
        FileWriter wr = new FileWriter(file);
        wr.write(source.getSource());
        wr.close();
    }

    private static void removeSources() {
        File file = new File(sourcepath);
        removeFile(file);
    }
    
    private static void removeFile(File file){
        if(file.isDirectory()){
            for(File f : file.listFiles()) removeFile(f);
        }
        file.delete();
    }

    private static RootDoc getRootDoc(Visibility visibility, java.util.Set<String> packages) throws IOException  {
        long accessModifier = 0;
        if(visibility == Visibility.PUBLIC){
            accessModifier = 
                com.sun.tools.javac.code.Flags.PUBLIC;    // 0x1
        }
        if(visibility == Visibility.PROTECTED){
            accessModifier = 
                com.sun.tools.javac.code.Flags.PUBLIC    // 0x1
                | com.sun.tools.javac.code.Flags.PROTECTED;    // 0x4
        }
        if(visibility == Visibility.PACKAGE){
            accessModifier = 
                com.sun.tools.javac.code.Flags.PUBLIC    // 0x1
                | com.sun.tools.javac.code.Flags.PROTECTED    // 0x4
                | com.sun.tools.javadoc.ModifierFilter.PACKAGE; // 0x80000000
        }
        if(visibility == Visibility.PRIVATE){
            accessModifier = 
                com.sun.tools.javac.code.Flags.PUBLIC    // 0x1
                | com.sun.tools.javac.code.Flags.PROTECTED    // 0x4
                | com.sun.tools.javadoc.ModifierFilter.PACKAGE // 0x80000000
                | com.sun.tools.javac.code.Flags.PRIVATE;    // 0x2
        }

        ModifierFilter showAccess = new ModifierFilter(accessModifier);
        boolean breakiterator = false;
        boolean quiet = false;
        boolean legacy = false;
        boolean docClasses = false;
        
        String docLocale = "";
        String encoding = null;
        ListBuffer<String> javaNames = new ListBuffer<String>();
        for(String p : packages)
            javaNames.append(p);
        
        ListBuffer<String[]> options = new ListBuffer<String[]>();
    
        
//        String sourcepath = "//D:/Documents/Projects/08_CTS/signature-tools/signature-tools/test/";
        options.append(new String[]{"-sourcepath", sourcepath});
        
        ListBuffer<String> subPackages = new ListBuffer<String>();
        ListBuffer<String> excludedPackages = new ListBuffer<String>();

        Context context = new Context();
        Options compOpts = Options.instance(context);
        compOpts.put("-sourcepath", sourcepath);
        
        Constructor<Messager> c;
        try {
//            c = Messager.class.getDeclaredConstructor(Context.class, String.class);
//            c.setAccessible(true);
//            c.newInstance(context, "SigTest");
            c = Messager.class.getDeclaredConstructor(Context.class, String.class, PrintWriter.class, PrintWriter.class, PrintWriter.class);
            c.setAccessible(true);
            PrintWriter err = new PrintWriter(new StringWriter());
            PrintWriter warn = new PrintWriter(new StringWriter());
            PrintWriter notice = new PrintWriter(new StringWriter());
            c.newInstance(context, "SigTest", err, warn, notice);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        JavadocTool comp = JavadocTool.make0(context);
        RootDocImpl root = comp.getRootDocImpl(docLocale, encoding, showAccess,
                javaNames.toList(), options.toList(), breakiterator,
                subPackages.toList(), excludedPackages.toList(), docClasses,
                legacy, quiet);
        return root;
    }
    
}
