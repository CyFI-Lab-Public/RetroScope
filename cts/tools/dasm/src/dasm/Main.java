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

package dasm;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * DAsm entry point
 */
public class Main {

    /**
     * The Dasm version
     */
    public static final String version = "v0.1";
    public static final boolean DEBUG = !false;

    /**
     * destination path to place .dex file(s)
     */
    private static String destPath = null;

    /**
     * generate human-readable files
     */
    private static boolean humanHeadable = false;

    /**
     * input files codepage
     */
    private static String encoding = null;

    /**
     * automatically generate line numbers
     */
    private static boolean generateLineNumbers = false;

    private static void incompleteOption(String opt) {
        System.err.println("Command line option " + opt
                + " required argument missed");
        System.exit(-1);
    }

    private static Reader createReader(String fname) throws IOException {
        FileInputStream fs = new FileInputStream(fname);
        InputStreamReader ir;
        if (encoding == null)
            ir = new InputStreamReader(fs);
        else
            ir = new InputStreamReader(fs, encoding);
        return new BufferedReader(ir);
    }

    /**
     * Called to assemble a single file.
     * 
     * @param fname
     *            is the name of the file containing the DAsm source code.
     */
    public static void assemble(String fname) {
        DAsm dAsm = new DAsm();

        // read and parse .d file
        Reader inp = null;
        try {
            inp = createReader(fname);
            dAsm.readD(inp, new File(fname).getName(), generateLineNumbers);
            close(inp);
        } catch (DasmError e) {
            if (DEBUG) e.printStackTrace();
            System.err.println("DASM Error: " + e.getMessage());
        } catch (Exception e) {
            if (DEBUG) e.printStackTrace();
            System.err.println("Exception <" + e.getClass().getName() + ">"
                    + e.getMessage() + " while reading and parsing " + fname);
            return;

        } finally {
            close(inp);
        }

        if (dAsm.errorCount() > 0) {
            System.err.println("Found " + dAsm.errorCount() + " errors "
                    + " while reading and parsing " + fname);
            return;
        }

        String class_path[] = Utils
                .getClassFieldFromString(dAsm.getClassName());
        String class_name = class_path[1];

        // determine where to place .dex file
        String dest_dir = destPath;
        if (class_path[0] != null) {
            String class_dir = class_path[0].replaceAll("/|\\.", Character
                    .toString(File.separatorChar));
            if (dest_dir != null) {
                dest_dir = dest_dir + File.separator + class_dir;
            } else {
                dest_dir = class_dir;
            }
        }

        File out_file = null;
        File hr_file = null;

        if (dest_dir == null) {
            out_file = new File(class_name + ".dex");
            hr_file = new File(class_name + ".dxt");
        } else {
            out_file = new File(dest_dir, class_name + ".dex");
            hr_file = new File(dest_dir, class_name + ".dxt");

            // check that dest_dir exists
            File dest = new File(dest_dir);
            if (!dest.exists()) {
                dest.mkdirs();
            }

            if (!dest.isDirectory()) {
                System.err.println("Cannot create directory " + dest_dir);
                return;
            }
        }

        // write output
        FileOutputStream outp = null;
        FileWriter hr_outp = null;

        try {
            outp = new FileOutputStream(out_file);
            if (humanHeadable) hr_outp = new FileWriter(hr_file);
            dAsm.write(outp, hr_outp);
        } catch (Exception e) {
            if (DEBUG) e.printStackTrace();
            System.err.println("Exception <" + e.getClass().getName() + ">"
                    + e.getMessage() + " while writing " + out_file.getPath());

            close(hr_outp);
            close(outp);

            hr_file.delete();
            out_file.delete();

            return;
        } finally {
            close(hr_outp);
            close(outp);
        }

        System.out.println("Generated: " + out_file.getPath());
    }

    private static void close(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException e) {

        }
    }

    public static void main(String args[]) {
        int i;

        String files[] = new String[args.length];
        int num_files = 0;

        if (args.length == 0) {
            printUsage();
            System.exit(-1);
        }

        for (i = 0; i < args.length; i++) {
            if (args[i].equals("-help") || args[i].equals("-?")) {
                printUsage();
                System.exit(0);
            }
            if (args[i].equals("-version")) {
                System.out.println("DAsm version: " + version);
                if (DEBUG) System.out.println("(compiled with DEBUG flag on)");
                System.exit(0);
            }
            if (args[i].equals("-g")) {
                generateLineNumbers = true;
            } else if (args[i].equals("-d")) {
                if (++i >= args.length)
                    incompleteOption("-d");
                else
                    destPath = args[i];
            } else if (args[i].equals("-h")) {
                humanHeadable = true;
            } else if (args[i].equals("-e")) {
                if (++i >= args.length)
                    incompleteOption("-e");
                else
                    encoding = args[i];
            } else {
                files[num_files++] = args[i];
            }
        }

        for (i = 0; i < num_files; i++) {
            assemble(files[i]);
        }
    }

    static void printUsage() {
        System.err
                .println("dasm [-d <outpath>] [-g] [-h] [-e <encoding>] <file>"
                        + "[<file> ...]\n\n"
                        + "  -g - autogenerate linenumbers\n"
                        + "  -e - codepage for inputfile encoding\n"
                        + "  -d - path for generated classfiles\n"
                        + "  -h - generate human-readable output\n"
                        + "  file  - sourcefile\n"
                        + "or: dasm -version\n"
                        + "or: dasm -help");
    }
};
