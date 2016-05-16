/*
 * Copyright (C) 2008 The Android Open Source Project
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

package dxconvext;

import com.android.dx.cf.direct.ClassPathOpener;
import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.direct.StdAttributeFactory;
import com.android.dx.cf.iface.Member;
import com.android.dx.cf.iface.ParseObserver;
import com.android.dx.util.ByteArray;
import com.android.dx.util.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class ClassFileParser {

    private BufferedWriter bw; // the writer to write the result to.

    /**
     * Parses a .class file and outputs a .cfh (class file in hex format) file.
     * 
     * args[0] is the absolute path to the java src directory e.g.
     * /home/fjost/android/workspace/dxconverter/src
     * 
     * args[1] is the absolute path to the classes directory e.g.
     * /home/fjost/android/workspace/out/classes_javac this is the place where
     * 
     * args[2] is the absolute path to the java source file, e.g.
     * /home/fjost/android/workspace/dxconverter/src/test/MyTest.java
     * 
     * 
     * 
     * @param args
     */
    public static void main(String[] args) throws IOException {
        ClassFileParser cfp = new ClassFileParser();
        cfp.process(args[0], args[1], args[2]);
    }

    private void process(final String srcDir, final String classesDir,
            final String absSrcFilePath) throws IOException {
        ClassPathOpener opener;

        String fileName = absSrcFilePath;
        // e.g. test/p1/MyTest.java
        String pckPath = fileName.substring(srcDir.length() + 1);
        // e.g. test/p1
        String pck = pckPath.substring(0, pckPath.lastIndexOf("/"));
        // e.g. MyTest
        String cName = pckPath.substring(pck.length() + 1);
        cName = cName.substring(0, cName.lastIndexOf("."));
        String cfName = pck+"/"+cName+".class";
        // 2. calculate the target file name:
        // e.g. <out-path>/test/p1/MyTest.class
        String inFile = classesDir + "/" + pck + "/" + cName + ".class";
        if (!new File(inFile).exists()) {
            throw new RuntimeException("cannot read:" + inFile);
        }
        byte[] bytes = FileUtils.readFile(inFile);
        // write the outfile to the same directory as the corresponding .java
        // file
        String outFile = absSrcFilePath.substring(0, absSrcFilePath
                .lastIndexOf("/"))+ "/" + cName + ".cfh";
        Writer w;
        try {
            w = new OutputStreamWriter(new FileOutputStream(new File(outFile)));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("cannot write to file:"+outFile, e);
        }
        // Writer w = new OutputStreamWriter(System.out);
        ClassFileParser.this.processFileBytes(w, cfName, bytes);

    }

    /**
     * 
     * @param w the writer to write the generated .cfh file to
     * @param name the relative name of the java src file, e.g.
     *        dxc/util/Util.java
     * @param allbytes the bytes of this java src file
     * @return true if everthing went alright
     */
    void processFileBytes(Writer w, String name, final byte[] allbytes) throws IOException {
        String fixedPathName = fixPath(name);
        DirectClassFile cf = new DirectClassFile(allbytes, fixedPathName, true);
        bw = new BufferedWriter(w);
        String className = fixedPathName.substring(0, fixedPathName.lastIndexOf("."));
        out("//@class:" + className, 0);
        cf.setObserver(new ParseObserver() {
            private int cur_indent = 0;
            private int checkpos = 0;

            /**
             * Indicate that the level of indentation for a dump should increase
             * or decrease (positive or negative argument, respectively).
             * 
             * @param indentDelta the amount to change indentation
             */
            public void changeIndent(int indentDelta) {
                cur_indent += indentDelta;
            }

            /**
             * Indicate that a particular member is now being parsed.
             * 
             * @param bytes non-null; the source that is being parsed
             * @param offset offset into <code>bytes</code> for the start of
             *        the member
             * @param name non-null; name of the member
             * @param descriptor non-null; descriptor of the member
             */
            public void startParsingMember(ByteArray bytes, int offset,
                    String name, String descriptor) {
                // ByteArray ba = bytes.slice(offset, bytes.size());
                out("// ========== start-ParseMember:" + name + ", offset "
                        + offset + ", len:" + (bytes.size() - offset)
                        + ",desc: " + descriptor);
                // out("// "+dumpReadableString(ba));
                // out(" "+dumpBytes(ba));
            }

            /**
             * Indicate that a particular member is no longer being parsed.
             * 
             * @param bytes non-null; the source that was parsed
             * @param offset offset into <code>bytes</code> for the end of the
             *        member
             * @param name non-null; name of the member
             * @param descriptor non-null; descriptor of the member
             * @param member non-null; the actual member that was parsed
             */
            public void endParsingMember(ByteArray bytes, int offset,
                    String name, String descriptor, Member member) {
                ByteArray ba = bytes.slice(offset, bytes.size());
                out("// ========== end-ParseMember:" + name + ", desc: "
                        + descriptor);
                // out("// "+dumpReadableString(ba));
                // out(" "+dumpBytes(ba));
            }

            /**
             * Indicate that some parsing happened.
             * 
             * @param bytes non-null; the source that was parsed
             * @param offset offset into <code>bytes</code> for what was
             *        parsed
             * @param len number of bytes parsed
             * @param human non-null; human form for what was parsed
             */
            public void parsed(ByteArray bytes, int offset, int len,
                    String human) {
                human = human.replace('\n', ' ');
                out("// parsed:" + ", offset " + offset + ", len " + len
                        + ", h: " + human);
                if (len > 0) {
                    ByteArray ba = bytes.slice(offset, offset + len);
                    check(ba);
                    out("// " + dumpReadableString(ba));
                    out("   " + dumpBytes(ba));
                }
            }

            private void out(String msg) {
                ClassFileParser.this.out(msg, cur_indent);

            }

            private void check(ByteArray ba) {
                int len = ba.size();
                int offset = checkpos;
                for (int i = 0; i < len; i++) {
                    int b = ba.getByte(i);
                    byte b2 = allbytes[i + offset];
                    if (b != b2)
                        throw new RuntimeException("byte dump mismatch at pos "
                                + (i + offset));
                }
                checkpos += len;
            }



            private String dumpBytes(ByteArray ba) {
                String s = "";
                for (int i = 0; i < ba.size(); i++) {
                    int byt = ba.getUnsignedByte(i);
                    String hexVal = Integer.toHexString(byt);
                    if (hexVal.length() == 1) {
                        hexVal = "0" + hexVal;
                    }
                    s += hexVal + " ";
                }
                return s;
            }

            private String dumpReadableString(ByteArray ba) {
                String s = "";
                for (int i = 0; i < ba.size(); i++) {
                    int bb = ba.getUnsignedByte(i);
                    if (bb > 31 && bb < 127) {
                        s += (char) bb;
                    } else {
                        s += ".";
                    }
                    s += "  ";
                }
                return s;
            }


        });
        cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
        // what is needed to force parsing to the end?
        cf.getMagic();
        // cf.getFields();
        // cf.getAttributes();
        // cf.getMethods();        
        bw.close();
    }


    private String getIndent(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent * 4; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private void out(String msg, int cur_indent) {
        try {
            bw.write(getIndent(cur_indent) + msg);
            bw.newLine();
        } catch (IOException ioe) {
            throw new RuntimeException("error while writing to the writer", ioe);
        }
    }

    private static String fixPath(String path) {
        /*
         * If the path separator is \ (like on windows), we convert the path to
         * a standard '/' separated path.
         */
        if (File.separatorChar == '\\') {
            path = path.replace('\\', '/');
        }

        int index = path.lastIndexOf("/./");

        if (index != -1) {
            return path.substring(index + 3);
        }

        if (path.startsWith("./")) {
            return path.substring(2);
        }

        return path;
    }



}
