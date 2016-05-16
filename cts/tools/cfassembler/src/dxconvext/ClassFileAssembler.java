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

import dxconvext.util.FileUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;

public class ClassFileAssembler {

    /**
     * @param args
     */
    public static void main(String[] args) {
        ClassFileAssembler cfa = new ClassFileAssembler();
        cfa.run(args);
    }

    private void run(String[] args) {
        // this class can be used to generate .class files that are somehow
        // damaged in order to test the dalvik vm verifier.
        // The input is a .cfh (class file hex) file.
        // The output is a java vm .class file.
        // The .cfh files can be generated as follows:
        // 1. create the initial .cfh file from an existing .class files by using
        //    the ClassFileParser
        // 2. modify some bytes to damage the structure of the .class file in a 
        //    way that would not be possible with e.g. jasmin (otherwise you are
        //    better off using jasmin).
        //    Uncomment the original bytes, and write "MOD:" meaning a modified 
        // entry (with the original commented out)
        //
        // Use the ClassFileAssembler to generate the .class file.
        // this class here simply takes all non-comment lines from the .cfh
        // file, parses them as hex values and writes the bytes to the class file
        File cfhF = new File(args[0]);
        if (!cfhF.getName().endsWith(".cfh") &&
            !cfhF.getName().endsWith(".dfh")) {
            System.out.println("file must be a .cfh or .dfh file, and its filename end with .cfh or .dfh");
            return;
        }
        
        String outBase = args[1];
        
        boolean isDex = cfhF.getName().endsWith(".dfh");
        
        byte[] cfhbytes = FileUtils.readFile(cfhF);
        ByteArrayInputStream bais = new ByteArrayInputStream(cfhbytes);
        // encoding should not matter, since we are skipping comment lines and parsing
        try {
            // get the package name
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(cfhF)));
            String firstLine = br.readLine();
            br.close();
            String classHdr = "//@class:";
            String dexHdr = "// Processing '";
            String hdr;
            if(isDex)
                hdr = dexHdr;
            else
                hdr = classHdr;
            
            if (!firstLine.startsWith(hdr)) throw new RuntimeException("wrong format:"+firstLine +" isDex=" + isDex);
            String tFile;
            if(isDex) {
                tFile = outBase + "/classes.dex";
            } else {
                String classO = firstLine.substring(hdr.length()).trim();
                tFile = outBase +"/"+classO+".class";
            }
            File outFile = new File(tFile);
            System.out.println("outfile:" + outFile);
            String mkdir = tFile.substring(0, tFile.lastIndexOf("/"));
            new File(mkdir).mkdirs();
            
            Reader r = new InputStreamReader(bais,"utf-8");
            OutputStream os = new FileOutputStream(outFile);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            writeClassFile(r, bos, isDex);
            bos.close();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("problem while parsing .dfh or .cfh file: "+cfhF.getAbsolutePath(), e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("problem while parsing .dfh or .cfh file: "+cfhF.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new RuntimeException("problem while parsing .dfh or .cfh file: "+cfhF.getAbsolutePath(), e);
        } 
    }
    
    /**
     * Calculates the signature for the <code>.dex</code> file in the
     * given array, and modify the array to contain it.
     * 
     * Originally from com.android.dx.dex.file.DexFile.
     * 
     * @param bytes non-null; the bytes of the file
     */
    private void calcSignature(byte[] bytes) {
        MessageDigest md;

        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }

        md.update(bytes, 32, bytes.length - 32);

        try {
            int amt = md.digest(bytes, 12, 20);
            if (amt != 20) {
                throw new RuntimeException("unexpected digest write: " + amt +
                                           " bytes");
            }
        } catch (DigestException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Calculates the checksum for the <code>.dex</code> file in the
     * given array, and modify the array to contain it.
     * 
     * Originally from com.android.dx.dex.file.DexFile.
     * 
     * @param bytes non-null; the bytes of the file
     */
    private void calcChecksum(byte[] bytes) {
        Adler32 a32 = new Adler32();

        a32.update(bytes, 12, bytes.length - 12);

        int sum = (int) a32.getValue();

        bytes[8]  = (byte) sum;
        bytes[9]  = (byte) (sum >> 8);
        bytes[10] = (byte) (sum >> 16);
        bytes[11] = (byte) (sum >> 24);
    }   

    public void writeClassFile(Reader r, OutputStream rOs, boolean isDex) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        BufferedReader br = new BufferedReader(r);
        String line;
        String secondLine = null;
        int lineCnt = 0;
        try {
            while ((line = br.readLine()) != null) {
                if (isDex && lineCnt++ == 1) {
                    secondLine = line;
                }
                // skip it if it is a comment
                if (!line.trim().startsWith("//")) {
                    // we have a row like "    ae 08 21 ff" etc.
                    String[] parts = line.split("\\s+");
                    for (int i = 0; i < parts.length; i++) {
                        String part = parts[i].trim();
                        if (!part.equals("")) {
                            int res = Integer.parseInt(part, 16);
                            baos.write(res);
                        }
                    }
                }
            }
            
            // now for dex, update the checksum and the signature.
            // special case:
            // for two tests (currently T_f1_9.dfh and T_f1_10.dfh), we need
            // to keep the checksum or the signature, respectively.
            byte[] outBytes = baos.toByteArray();
            if (isDex) {
                boolean leaveChecksum = secondLine.contains("//@leaveChecksum");
                boolean leaveSignature= secondLine.contains("//@leaveSignature");
                // update checksum and signature for dex file            
                if(!leaveSignature)
                    calcSignature(outBytes);
                if(!leaveChecksum)
                    calcChecksum(outBytes);
            }
            rOs.write(outBytes);
            rOs.close();
        } catch (IOException e) {
            throw new RuntimeException("problem while writing file",e);
        }
    }

}
