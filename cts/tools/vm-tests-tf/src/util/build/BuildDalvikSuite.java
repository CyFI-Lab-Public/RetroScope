/*
 * Copyright (C) 2011 The Android Open Source Project
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

package util.build;

import com.android.dex.util.FileUtils;

import dot.junit.AllTests;

import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main class to generate data from the test suite to later run from a shell
 * script. the project's home folder.<br>
 * <project-home>/src must contain the java sources<br>
 * <project-home>/src/<for-each-package>/Main_testN1.java will be generated<br>
 * (one Main class for each test method in the Test_... class
 */
public class BuildDalvikSuite {

    public static boolean DEBUG = true;

    private static String JAVASRC_FOLDER = "";
    private static String MAIN_SRC_OUTPUT_FOLDER = "";

    // the folder for the generated junit-files for the cts host (which in turn
    // execute the real vm tests using adb push/shell etc)
    private static String HOSTJUNIT_SRC_OUTPUT_FOLDER = "";
    private static String OUTPUT_FOLDER = "";
    private static String COMPILED_CLASSES_FOLDER = "";

    private static String CLASSES_OUTPUT_FOLDER = "";
    private static String HOSTJUNIT_CLASSES_OUTPUT_FOLDER = "";

    private static String CLASS_PATH = "";

    private static String restrictTo = null; // e.g. restrict to "opcodes.add_double"

    private static final String TARGET_JAR_ROOT_PATH = "/data/local/tmp/vm-tests";

    private int testClassCnt = 0;
    private int testMethodsCnt = 0;

    /*
     * using a linked hashmap to keep the insertion order for iterators.
     * the junit suite/tests adding order is used to generate the order of the
     * report.
     * a map. key: fully qualified class name, value: a list of test methods for
     * the given class
     */
    private LinkedHashMap<String, List<String>> map = new LinkedHashMap<String,
    List<String>>();

    private class MethodData {
        String methodBody, constraint, title;
    }

    /**
     * @param args
     *            args 0 must be the project root folder (where src, lib etc.
     *            resides)
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        if (args.length > 5) {
            JAVASRC_FOLDER = args[0];
            OUTPUT_FOLDER = args[1];
            CLASS_PATH = args[2];
            MAIN_SRC_OUTPUT_FOLDER = args[3];
            CLASSES_OUTPUT_FOLDER = MAIN_SRC_OUTPUT_FOLDER + "/classes";

            COMPILED_CLASSES_FOLDER = args[4];

            HOSTJUNIT_SRC_OUTPUT_FOLDER = args[5];
            HOSTJUNIT_CLASSES_OUTPUT_FOLDER = HOSTJUNIT_SRC_OUTPUT_FOLDER + "/classes";

            if (args.length > 6) {
                // optional: restrict to e.g. "opcodes.add_double"
                restrictTo = args[6];
                System.out.println("restricting build to: " + restrictTo);
            }

        } else {
            System.out.println("usage: java-src-folder output-folder classpath " +
                    "generated-main-files compiled_output generated-main-files " +
            "[restrict-to-opcode]");
            System.exit(-1);
        }

        long start = System.currentTimeMillis();
        BuildDalvikSuite cat = new BuildDalvikSuite();
        cat.compose();
        long end = System.currentTimeMillis();

        System.out.println("elapsed seconds: " + (end - start) / 1000);
    }

    public void compose() throws IOException {
        System.out.println("Collecting all junit tests...");
        new TestRunner() {
            @Override
            protected TestResult createTestResult() {
                return new TestResult() {
                    @Override
                    protected void run(TestCase test) {
                        addToTests(test);
                    }

                };
            }
        }.doRun(AllTests.suite());

        // for each combination of TestClass and method, generate a Main_testN1
        // class in the respective package.
        // for the report make sure all N... tests are called first, then B,
        // then E, then VFE test methods.
        // e.g. dxc.junit.opcodes.aaload.Test_aaload - testN1() ->
        // File Main_testN1.java in package dxc.junit.opcodes.aaload.
        //
        handleTests();
    }

    private void addToTests(TestCase test) {

        String packageName = test.getClass().getPackage().getName();
        packageName = packageName.substring(packageName.lastIndexOf('.'));


        String method = test.getName(); // e.g. testVFE2
        String fqcn = test.getClass().getName(); // e.g.
        // dxc.junit.opcodes.iload_3.Test_iload_3

        // ignore all tests not belonging to the given restriction
        if (restrictTo != null && !fqcn.contains(restrictTo)) return;

        testMethodsCnt++;
        List<String> li = map.get(fqcn);
        if (li == null) {
            testClassCnt++;
            li = new ArrayList<String>();
            map.put(fqcn, li);
        }
        li.add(method);
    }
    private String curJunitFileName = null;
    private String curJunitFileData = "";

    private JavacBuildStep javacHostJunitBuildStep;

    private void flushHostJunitFile() {
        if (curJunitFileName != null) {
            File toWrite = new File(curJunitFileName);
            String absPath = toWrite.getAbsolutePath();
            // add to java source files for later compilation
            javacHostJunitBuildStep.addSourceFile(absPath);
            // write file
            curJunitFileData += "\n}\n";
            writeToFileMkdir(toWrite, curJunitFileData);
            curJunitFileName = null;
            curJunitFileData = "";
        }
    }

    private void openCTSHostFileFor(String pName, String classOnlyName) {
        // flush previous JunitFile
        flushHostJunitFile();
        String sourceName = "JUnit_" + classOnlyName;

        // prepare current testcase-file
        curJunitFileName = HOSTJUNIT_SRC_OUTPUT_FOLDER + "/" + pName.replaceAll("\\.","/") + "/" +
        sourceName + ".java";
        curJunitFileData = getWarningMessage() +
        "package " + pName + ";\n" +
        "import java.io.IOException;\n" +
        "import com.android.tradefed.testtype.DeviceTestCase;\n" +
        "\n" +
        "public class " + sourceName + " extends DeviceTestCase {\n";
    }

    private String getShellExecJavaLine(String classpath, String mainclass) {
      String cmd = String.format("ANDROID_DATA=%s dalvikvm -Xint:portable -Xmx512M -Xss32K " +
              "-Djava.io.tmpdir=%s -classpath %s %s", TARGET_JAR_ROOT_PATH, TARGET_JAR_ROOT_PATH,
              classpath, mainclass);
      return "String res = getDevice().executeShellCommand(\""+ cmd + "\");\n" +
             "// A sucessful adb shell command returns an empty string.\n" +
             "assertEquals(\"" + cmd + "\", \"\", res);";
    }

    private String getWarningMessage() {
        return "//Autogenerated code by " + this.getClass().getName() + "; do not edit.\n";
    }

    private void addCTSHostMethod(String pName, String method, MethodData md,
            Set<String> dependentTestClassNames) {
        curJunitFileData += "public void " + method + "() throws Exception {\n";
        final String targetCoreJarPath = String.format("%s/dot/junit/dexcore.jar",
                TARGET_JAR_ROOT_PATH);

        // push class with Main jar.
        String mjar = "Main_" + method + ".jar";
        String pPath = pName.replaceAll("\\.","/");
        String mainJar = String.format("%s/%s/%s", TARGET_JAR_ROOT_PATH, pPath, mjar);

        String cp = String.format("%s:%s", targetCoreJarPath, mainJar);
        for (String depFqcn : dependentTestClassNames) {
            int lastDotPos = depFqcn.lastIndexOf('.');
            String sourceName = depFqcn.replaceAll("\\.", "/") + ".jar";
            String targetName= String.format("%s/%s", TARGET_JAR_ROOT_PATH,
                    sourceName);
            cp += ":" + targetName;
            // dot.junit.opcodes.invoke_interface_range.ITest
            // -> dot/junit/opcodes/invoke_interface_range/ITest.jar
        }

        //"dot.junit.opcodes.add_double_2addr.Main_testN2";
        String mainclass = pName + ".Main_" + method;
        curJunitFileData += "    " + getShellExecJavaLine(cp, mainclass);
        curJunitFileData += "}\n\n";
    }

    private void handleTests() throws IOException {
        System.out.println("collected " + testMethodsCnt + " test methods in " +
                testClassCnt + " junit test classes");
        Set<BuildStep> targets = new TreeSet<BuildStep>();

        javacHostJunitBuildStep = new JavacBuildStep(HOSTJUNIT_CLASSES_OUTPUT_FOLDER, CLASS_PATH);


        JavacBuildStep javacBuildStep = new JavacBuildStep(
                CLASSES_OUTPUT_FOLDER, CLASS_PATH);

        for (Entry<String, List<String>> entry : map.entrySet()) {

            String fqcn = entry.getKey();
            int lastDotPos = fqcn.lastIndexOf('.');
            String pName = fqcn.substring(0, lastDotPos);
            String classOnlyName = fqcn.substring(lastDotPos + 1);
            String instPrefix = "new " + classOnlyName + "()";

            openCTSHostFileFor(pName, classOnlyName);

            List<String> methods = entry.getValue();
            Collections.sort(methods, new Comparator<String>() {
                public int compare(String s1, String s2) {
                    // TODO sort according: test ... N, B, E, VFE
                    return s1.compareTo(s2);
                }
            });
            for (String method : methods) {
                // e.g. testN1
                if (!method.startsWith("test")) {
                    throw new RuntimeException("no test method: " + method);
                }

                // generate the Main_xx java class

                // a Main_testXXX.java contains:
                // package <packagenamehere>;
                // public class Main_testxxx {
                // public static void main(String[] args) {
                // new dxc.junit.opcodes.aaload.Test_aaload().testN1();
                // }
                // }
                MethodData md = parseTestMethod(pName, classOnlyName, method);
                String methodContent = md.methodBody;

                Set<String> dependentTestClassNames = parseTestClassName(pName,
                        classOnlyName, methodContent);

                addCTSHostMethod(pName, method, md, dependentTestClassNames);


                if (dependentTestClassNames.isEmpty()) {
                    continue;
                }


                String content = getWarningMessage() +
                "package " + pName + ";\n" +
                "import " + pName + ".d.*;\n" +
                "import dot.junit.*;\n" +
                "public class Main_" + method + " extends DxAbstractMain {\n" +
                "    public static void main(String[] args) throws Exception {" +
                methodContent + "\n}\n";

                String fileName = getFileName(pName, method, ".java");
                File sourceFile = getFileFromPackage(pName, method);

                File classFile = new File(CLASSES_OUTPUT_FOLDER + "/" +
                        getFileName(pName, method, ".class"));
                // if (sourceFile.lastModified() > classFile.lastModified()) {
                writeToFile(sourceFile, content);
                javacBuildStep.addSourceFile(sourceFile.getAbsolutePath());

                BuildStep dexBuildStep = generateDexBuildStep(
                        CLASSES_OUTPUT_FOLDER, getFileName(pName, method, ""));
                targets.add(dexBuildStep);
                // }

                generateBuildStepFor(pName, method, dependentTestClassNames,
                        targets);
            }


        }

        // write latest HOSTJUNIT generated file.
        flushHostJunitFile();

        if (!javacHostJunitBuildStep.build()) {
            System.out.println("main javac cts-host-hostjunit-classes build step failed");
            System.exit(1);
        }

        if (javacBuildStep.build()) {
            for (BuildStep buildStep : targets) {
                if (!buildStep.build()) {
                    System.out.println("building failed. buildStep: " +
                            buildStep.getClass().getName() + ", " + buildStep);
                    System.exit(1);
                }
            }
        } else {
            System.out.println("main javac dalvik-cts-buildutil build step failed");
            System.exit(1);
        }
    }

    private void generateBuildStepFor(String pName, String method,
            Set<String> dependentTestClassNames, Set<BuildStep> targets) {


        for (String dependentTestClassName : dependentTestClassNames) {
            generateBuildStepForDependant(dependentTestClassName, targets);
        }
    }

    private void generateBuildStepForDependant(String dependentTestClassName,
            Set<BuildStep> targets) {

        File sourceFolder = new File(JAVASRC_FOLDER);
        String fileName = dependentTestClassName.replace('.', '/').trim();

        if (new File(sourceFolder, fileName + ".dfh").exists()) {

            BuildStep.BuildFile inputFile = new BuildStep.BuildFile(
                    JAVASRC_FOLDER, fileName + ".dfh");
            BuildStep.BuildFile dexFile = new BuildStep.BuildFile(
                    OUTPUT_FOLDER, fileName + ".dex");

            DFHBuildStep buildStep = new DFHBuildStep(inputFile, dexFile);

            BuildStep.BuildFile jarFile = new BuildStep.BuildFile(
                    OUTPUT_FOLDER, fileName + ".jar");
            JarBuildStep jarBuildStep = new JarBuildStep(dexFile,
                    "classes.dex", jarFile, true);
            jarBuildStep.addChild(buildStep);

            targets.add(jarBuildStep);
            return;
        }

        if (new File(sourceFolder, fileName + ".d").exists()) {

            BuildStep.BuildFile inputFile = new BuildStep.BuildFile(
                    JAVASRC_FOLDER, fileName + ".d");
            BuildStep.BuildFile dexFile = new BuildStep.BuildFile(
                    OUTPUT_FOLDER, fileName + ".dex");

            DasmBuildStep buildStep = new DasmBuildStep(inputFile, dexFile);

            BuildStep.BuildFile jarFile = new BuildStep.BuildFile(
                    OUTPUT_FOLDER, fileName + ".jar");

            JarBuildStep jarBuildStep = new JarBuildStep(dexFile,
                    "classes.dex", jarFile, true);
            jarBuildStep.addChild(buildStep);
            targets.add(jarBuildStep);
            return;
        }

        if (new File(sourceFolder, fileName + ".java").exists()) {

            BuildStep dexBuildStep = generateDexBuildStep(
                    COMPILED_CLASSES_FOLDER, fileName);
            targets.add(dexBuildStep);
            return;
        }

        try {
            if (Class.forName(dependentTestClassName) != null) {

                BuildStep dexBuildStep = generateDexBuildStep(
                        COMPILED_CLASSES_FOLDER, fileName);
                targets.add(dexBuildStep);
                return;
            }
        } catch (ClassNotFoundException e) {
            // do nothing
        }

        throw new RuntimeException("neither .dfh,.d,.java file of dependant test class found : " +
                dependentTestClassName + ";" + fileName);
    }

    private BuildStep generateDexBuildStep(String classFileFolder,
            String classFileName) {
        BuildStep.BuildFile classFile = new BuildStep.BuildFile(
                classFileFolder, classFileName + ".class");

        BuildStep.BuildFile tmpJarFile = new BuildStep.BuildFile(OUTPUT_FOLDER,
                classFileName + "_tmp.jar");

        JarBuildStep jarBuildStep = new JarBuildStep(classFile, classFileName +
                ".class", tmpJarFile, false);

        BuildStep.BuildFile outputFile = new BuildStep.BuildFile(OUTPUT_FOLDER,
                classFileName + ".jar");

        DexBuildStep dexBuildStep = new DexBuildStep(tmpJarFile, outputFile,
                true);

        dexBuildStep.addChild(jarBuildStep);
        return dexBuildStep;

    }

    /**
     * @param pName
     * @param classOnlyName
     * @param methodSource
     * @return testclass names
     */
    private Set<String> parseTestClassName(String pName, String classOnlyName,
            String methodSource) {
        Set<String> entries = new HashSet<String>();
        String opcodeName = classOnlyName.substring(5);

        Scanner scanner = new Scanner(methodSource);

        String[] patterns = new String[] {"new\\s(T_" + opcodeName + "\\w*)",
                "(T_" + opcodeName + "\\w*)", "new\\s(T\\w*)"};

        String token = null;
        for (String pattern : patterns) {
            token = scanner.findWithinHorizon(pattern, methodSource.length());
            if (token != null) {
                break;
            }
        }

        if (token == null) {
            System.err.println("warning: failed to find dependent test class name: " + pName +
                    ", " + classOnlyName + " in methodSource:\n" + methodSource);
            return entries;
        }

        MatchResult result = scanner.match();

        entries.add((pName + ".d." + result.group(1)).trim());

        // search additional @uses directives
        Pattern p = Pattern.compile("@uses\\s+(.*)\\s+", Pattern.MULTILINE);
        Matcher m = p.matcher(methodSource);
        while (m.find()) {
            String res = m.group(1);
            entries.add(res.trim());
        }

        // lines with the form @uses
        // dot.junit.opcodes.add_double.jm.T_add_double_2
        // one dependency per one @uses
        // TODO

        return entries;
    }

    private MethodData parseTestMethod(String pname, String classOnlyName,
            String method) {

        String path = pname.replaceAll("\\.", "/");
        String absPath = JAVASRC_FOLDER + "/" + path + "/" + classOnlyName + ".java";
        File f = new File(absPath);

        Scanner scanner;
        try {
            scanner = new Scanner(f);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("error while reading to file: " + e.getClass().getName() +
                    ", msg:" + e.getMessage());
        }

        String methodPattern = "public\\s+void\\s+" + method + "[^\\{]+\\{";

        String token = scanner.findWithinHorizon(methodPattern, (int) f.length());
        if (token == null) {
            throw new RuntimeException("cannot find method source of 'public void " + method +
                    "' in file '" + absPath + "'");
        }

        MatchResult result = scanner.match();
        result.start();
        result.end();

        StringBuilder builder = new StringBuilder();
        //builder.append(token);

        try {
            FileReader reader = new FileReader(f);
            reader.skip(result.end());

            char currentChar;
            int blocks = 1;
            while ((currentChar = (char) reader.read()) != -1 && blocks > 0) {
                switch (currentChar) {
                    case '}': {
                        blocks--;
                        builder.append(currentChar);
                        break;
                    }
                    case '{': {
                        blocks++;
                        builder.append(currentChar);
                        break;
                    }
                    default: {
                        builder.append(currentChar);
                        break;
                    }
                }
            }
            if (reader != null) {
                reader.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("failed to parse", e);
        }

        // find the @title/@constraint in javadoc comment for this method
        // using platform's default charset
        String all = new String(FileUtils.readFile(f));
        // System.out.println("grepping javadoc found for method " + method +
        // " in " + pname + "," + classOnlyName);
        String commentPattern = "/\\*\\*([^{]*)\\*/\\s*" + methodPattern;
        Pattern p = Pattern.compile(commentPattern, Pattern.DOTALL);
        Matcher m = p.matcher(all);
        String title = null, constraint = null;
        if (m.find()) {
            String res = m.group(1);
            // System.out.println("res: " + res);
            // now grep @title and @constraint
            Matcher titleM = Pattern.compile("@title (.*)", Pattern.DOTALL)
            .matcher(res);
            if (titleM.find()) {
                title = titleM.group(1).replaceAll("\\n     \\*", "");
                title = title.replaceAll("\\n", " ");
                title = title.trim();
                // System.out.println("title: " + title);
            } else {
                System.err.println("warning: no @title found for method " + method + " in " + pname +
                        "," + classOnlyName);
            }
            // constraint can be one line only
            Matcher constraintM = Pattern.compile("@constraint (.*)").matcher(
                    res);
            if (constraintM.find()) {
                constraint = constraintM.group(1);
                constraint = constraint.trim();
                // System.out.println("constraint: " + constraint);
            } else if (method.contains("VFE")) {
                System.err
                .println("warning: no @constraint for for a VFE method:" + method + " in " +
                        pname + "," + classOnlyName);
            }
        } else {
            System.err.println("warning: no javadoc found for method " + method + " in " + pname +
                    "," + classOnlyName);
        }
        MethodData md = new MethodData();
        md.methodBody = builder.toString();
        md.constraint = constraint;
        md.title = title;
        if (scanner != null) {
            scanner.close();
        }
        return md;
    }

    private void writeToFileMkdir(File file, String content) {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new RuntimeException("failed to create directory: " + parent.getAbsolutePath());
        }
        writeToFile(file, content);
    }

    private void writeToFile(File file, String content) {
        try {
            if (file.length() == content.length()) {
                FileReader reader = new FileReader(file);
                char[] charContents = new char[(int) file.length()];
                reader.read(charContents);
                reader.close();
                String contents = new String(charContents);
                if (contents.equals(content)) {
                    // System.out.println("skipping identical: "
                    // + file.getAbsolutePath());
                    return;
                }
            }

            //System.out.println("writing file " + file.getAbsolutePath());

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), "utf-8"));
            bw.write(content);
            bw.close();
        } catch (Exception e) {
            throw new RuntimeException("error while writing to file: " + e.getClass().getName() +
                    ", msg:" + e.getMessage());
        }
    }

    private File getFileFromPackage(String pname, String methodName)
    throws IOException {
        // e.g. dxc.junit.argsreturns.pargsreturn
        String path = getFileName(pname, methodName, ".java");
        String absPath = MAIN_SRC_OUTPUT_FOLDER + "/" + path;
        File dirPath = new File(absPath);
        File parent = dirPath.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("failed to create directory: " + absPath);
        }
        return dirPath;
    }

    private String getFileName(String pname, String methodName,
            String extension) {
        String path = pname.replaceAll("\\.", "/");
        return new File(path, "Main_" + methodName + extension).getPath();
    }
}
