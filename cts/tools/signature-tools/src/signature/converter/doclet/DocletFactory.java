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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.Set;

import signature.converter.Visibility;
import signature.io.IApiLoader;
import signature.model.IApi;

import com.sun.javadoc.RootDoc;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Options;
import com.sun.tools.javadoc.JavadocTool;
import com.sun.tools.javadoc.Messager;
import com.sun.tools.javadoc.ModifierFilter;
import com.sun.tools.javadoc.RootDocImpl;

public class DocletFactory implements IApiLoader {

    public IApi loadApi(String name, Visibility visibility,
            Set<String> fileNames, Set<String> packageNames) throws
            IOException {
        for (String packageName : packageNames) {
            if (packageName.length() == 0)
                throw new IllegalArgumentException(
                        "default package not supported by DocletFactory");
        }
        StringBuffer buf = new StringBuffer();
        for (String filename : fileNames) {
            buf.append(filename);
            buf.append(":");
        }
        String sourcepath = buf.substring(0, buf.length() - 1);
        RootDoc root = getRootDoc(visibility, sourcepath, packageNames);
        DocletToSigConverter converter = new DocletToSigConverter();
        IApi api = converter.convertDocletRoot(name, root, visibility,
                packageNames);
        return api;
    }

    private static RootDoc getRootDoc(Visibility visibility, String sourcepath,
            java.util.Set<String> packages) throws IOException {
        long accessModifier = 0;
        switch (visibility) {
        case PRIVATE:
            accessModifier |= com.sun.tools.javac.code.Flags.PRIVATE; // 0x2
        case PACKAGE:                                              // 0x80000000
            accessModifier |= com.sun.tools.javadoc.ModifierFilter.PACKAGE;
        case PROTECTED:
            accessModifier |= com.sun.tools.javac.code.Flags.PROTECTED; // 0x4
        case PUBLIC:
            accessModifier |= com.sun.tools.javac.code.Flags.PUBLIC; // 0x1
        }

        ModifierFilter showAccess = new ModifierFilter(accessModifier);
        boolean breakiterator = false;
        boolean quiet = false;
        boolean legacy = false;
        boolean docClasses = false;

        String docLocale = "";
        String encoding = null;
        ListBuffer<String> javaNames = new ListBuffer<String>();
        for (String p : packages)
            javaNames.append(p);

        ListBuffer<String[]> options = new ListBuffer<String[]>();

        options.append(new String[] {"-sourcepath", sourcepath});

        ListBuffer<String> subPackages = new ListBuffer<String>();
        ListBuffer<String> excludedPackages = new ListBuffer<String>();

        Context context = new Context();
        Options compOpts = Options.instance(context);
        compOpts.put("-sourcepath", sourcepath);

        Constructor<Messager> c;
        try {
            // c = Messager.class.getDeclaredConstructor(Context.class,
            // String.class);
            // c.setAccessible(true);
            // c.newInstance(context, "SigTest");
            c = Messager.class.getDeclaredConstructor(Context.class,
                    String.class, PrintWriter.class, PrintWriter.class,
                    PrintWriter.class);
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
